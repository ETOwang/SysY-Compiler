package cn.edu.nju.software.backendarm.regalloc;

import cn.edu.nju.software.backendarm.ArmInstrGenerator;
import cn.edu.nju.software.backendarm.arminstruction.*;
import cn.edu.nju.software.backendarm.arminstruction.operand.*;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmComment;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmLabel;
import cn.edu.nju.software.ir.type.*;
import cn.edu.nju.software.ir.value.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmAllocator {

    private final FunctionValue function;
    private ArmInstrGenerator generator;
    private final ArmMemoryManager armMemoryManager = ArmMemoryManager.get();
    private ArmTempVarLiveTable armTempVarLiveTable;
    private ArmLValLiveTable armLValLiveTable;
    private static final Map<FunctionValue,ArmAllocator> allocators = new HashMap<>();
    private ArmAllocator(FunctionValue function) {
        this.function = function;
    }


    public void setLValLiveTable(ArmLValLiveTable armLValLiveTable) {
        this.armLValLiveTable = armLValLiveTable;
    }

    public void setTempVarLiveTable(ArmTempVarLiveTable armTempVarLiveTable) {
        this.armTempVarLiveTable = armTempVarLiveTable;
    }

    public void setInstrGenerator(ArmInstrGenerator generator) {
        this.generator = generator;
    }

    public void initialize() {
        armMemoryManager.clear(); // reset for memory allocation in current func
    }

    public static ArmAllocator get(FunctionValue function) {
        if (!allocators.containsKey(function)) {
            allocators.put(function, new ArmAllocator(function));
        }
        return allocators.get(function);
    }

    public List<String> prepareOperands(ValueRef... values) {
        List<String> regNames = new ArrayList<>();
        generator.addInstruction(new ArmComment("fetch variables"));
        int i = 1;
        for (ValueRef value : values) {
            if (value instanceof ConstValue constValue) {
                regNames.add(prepareAConst(constValue, i));
            } else if(value instanceof LocalVar localVar){
                regNames.add(prepareALocal(localVar, i));
            } else if(value instanceof GlobalVar globalVar){
                regNames.add(prepareAGlobal(globalVar, i));
            } else {
                assert false;
            }
            i++;
        }
        return regNames;
    }

    public List<String> prepareParamOperands(int startRegIndex, ValueRef... values) {
        List<String> regNames = new ArrayList<>();
        generator.addInstruction(new ArmComment("fetch variables"));
        int i = startRegIndex;
        for (ValueRef value : values) {
            if (value instanceof ConstValue constValue) {
                regNames.add(prepareAConst(constValue, i));
            } else if(value instanceof LocalVar localVar){
                regNames.add(prepareALocal(localVar, i));
            } else if(value instanceof GlobalVar globalVar){
                regNames.add(prepareAGlobal(globalVar, i));
            } else {
                assert false;
            }
            i++;
        }
        return regNames;
    }

    private String prepareAGlobal(GlobalVar globalVar, int i){
        loadLabel("r" + i, globalVar.getName());
        return "r" + i;
    }

    private String prepareALocal(LocalVar localVar, int i){
        ArmRegisterManager registerManager=ArmRegisterManager.get(function);
        if(registerManager.contains(localVar)){
            if(localVar.getType() instanceof FloatType){
                return registerManager.get(localVar);
            }else {
                generator.addInstruction(new ArmVmov_s32_f32(new ArmRegister("r"+i),new ArmRegister(registerManager.get(localVar))));
                return "r"+i;
            }
        }
        if (localVar.getType() instanceof FloatType) {
            if(isLastLVal(localVar)){
                armTempVarLiveTable.release(localVar);
                return "s0";
            }
            if(checkTempVarIsRecorded(localVar)){ //here is all localvar is temp
                return fetchTempVar(localVar);
            }
            generator.addInstruction(new ArmVldr_f32(new ArmRegister("s" + i), getAddrOfLocalVar(localVar)));
            return "s" + i;
        } else if (localVar.getType() instanceof IntType || localVar.getType() instanceof BoolType) {
            if(isLastLVal(localVar)){
                armTempVarLiveTable.release(localVar);
                return "r0";
            }
            if(checkTempVarIsRecorded(localVar)){ //here is all localvar is temp
                return fetchTempVar(localVar);
            }
            generator.addInstruction(new ArmLdr(new ArmRegister("r" + i), getAddrOfLocalVar(localVar)));
            return "r" + i;
        } else if(localVar.getType() instanceof Pointer){
            if(isLastLVal(localVar)){
                armTempVarLiveTable.release(localVar);
                return "r0";
            }
            if(checkTempVarIsRecorded(localVar)){ //here is all localvar is temp
                return fetchTempVar(localVar);
            }
            if(checkPtrHasAllocated(localVar.getName())){
                if(armMemoryManager.getOffset(localVar) >= 256 || armMemoryManager.getOffset(localVar) <= -256){
                    loadImmediate("r4", armMemoryManager.getOffset(localVar));
                    generator.addInstruction(new ArmAdd(new ArmRegister("r" + i), new ArmRegister("sp"), new ArmRegister("r4")));
                } else {
                    generator.addInstruction(new ArmAdd(new ArmRegister("r" + i), new ArmRegister("sp"), new ArmImmediateValue(armMemoryManager.getOffset(localVar))));
                }
                return "r" + i;
            }
            generator.addInstruction(new ArmLdr(new ArmRegister("r" + i), getAddrOfLocalVar(localVar)));
            return "r" + i;
        } else {assert false;}
        return null;
    }

    private String prepareAConst(ConstValue constValue, int i){
        if (constValue.getType() instanceof FloatType) {
            loadImmediate("r" + i, Float.floatToRawIntBits(Float.parseFloat(constValue.getValue().toString())));
            generator.addInstruction(new ArmVmov_f32_s32(new ArmRegister("s" + i), new ArmRegister("r" + i)));
            return "s" + i;
        } else if (constValue.getType() instanceof IntType) {
            loadImmediate("r" + i, Integer.parseInt(constValue.getValue().toString()));
            return "r" + i;
        } else if (constValue.getType() instanceof BoolType) {
            loadImmediate("r" + i, Boolean.TRUE.equals(constValue.getValue()) ? 1 : 0);
            return "r" + i;
        } else {assert false;}
        return null;
    }

    /**
     * t3用于实现大立即数的转化
     * 返回这个变量的地址，（对于指针，就是这个指针本身的地址 而不是指针的值）
     * @param variable
     * @return
     */
    public ArmOperand getAddrOfLocalVar(ValueRef variable) {
        generator.insertComment("get address of local var:" + variable.getName());
        if (variable instanceof LocalVar) {
            if(checkPtrHasAllocated(variable.getName())){
                assert false;
            }
            return getRegWithOffset(armMemoryManager.getOffset(variable), "sp", "r4");
        } else {
            assert false;
            return null;
        }
    }

    /**
     * 获取指针指向的变量的地址 + offset
     * 使用0（t3）,作为返回的地址
     * 用于处理数组元素的访问
     * eg: a is a pointer points to b. a 's value is 0x1000, b 's address is 0x1000, this function return b's address + offset (a's value)
     * @param ptr
     * @param offset
     * @return
     */
    public ArmOperand getAddrOfVarPtrPointsToWithOffset(ValueRef ptr, int offset) {
        generator.insertComment("get address of " + ptr.getName() + " points to");
        if(!(ptr.getType() instanceof Pointer)){
            assert false;
            return null;
        }
        if(checkTempVarIsRecorded(ptr)){
            return getRegWithOffset(offset, fetchTempVar(ptr), "r3");
        }
        if(checkPtrHasAllocated(ptr.getName())){
            return getRegWithOffset(armMemoryManager.getOffset(ptr) + offset, "sp", "r2");
        }
        if (ptr instanceof GlobalVar) {
            loadLabel("r3", ptr.getName());
            return getRegWithOffset(offset, "r3", "r4");
        } else if (ptr instanceof LocalVar) {
            generator.addInstruction(new ArmLdr(new ArmRegister("r3"), getRegWithOffset(armMemoryManager.getOffset(ptr), "sp", "r4")));
            return getRegWithOffset(offset, "r3", "r4");
        } else {assert false;return null;}
    }

    /**
     * 使用t4作为返回的寄存器
     * offset可能很大，无法作为offset(reg)的立即数，
     * 如果offset大于1024, destreg = baseReg + immediate 返回0(reg);
     * 否则直接返回offset(baseReg) t4用于offset过大的时候的返回的寄存器
     * 之所以要单独使用一个destReg是因为regToAdd可能是sp这种值不能随意更改的寄存器
     * @param immediate
     * @param baseReg
     * @param destReg
     * @return
     */
    public ArmOperand getRegWithOffset(int immediate, String baseReg, String destReg) {
        if(immediate >= 256 || immediate < -256){
            loadImmediate(destReg, immediate);
            generator.addInstruction(new ArmAdd(new ArmRegister(destReg), new ArmRegister(baseReg), new ArmRegister(destReg)));
            return new ArmIndirectRegister(destReg,0);
        }else {
            return new ArmIndirectRegister(baseReg, immediate);
        }
    }

    public int getOffset(ValueRef var) {
        return armMemoryManager.getOffset(var);
    }

    public void allocate(ValueRef var, int width) {
        armMemoryManager.allocateInStack(var, Math.max(width, 4));
    }

    public void allocate(int width) {
        armMemoryManager.allocateInStack(Math.max(width, 4));
    }

    public int getStackSize() {
        return armMemoryManager.getSize();
    }

    public int getSizeOfType(TypeRef type) {
        if (type instanceof ArrayType) {
            return ArrayType.getTotalSize(type);
        } else if (type instanceof FloatType || type instanceof IntType || type instanceof BoolType) {
            return 4;
        } else if (type instanceof Pointer) {
            return 8;
        } else {
            assert false;
            return 0;
        }
    }

    public void alignStack8byte() {
        armMemoryManager.align8byte();
    }

    public void alignStack16byte() {
        armMemoryManager.align16byte();
    }

    public void addHasAllocatedPtr(String name) {
        armMemoryManager.addHasAllocatedPtr(name);
    }

    public boolean checkPtrHasAllocated(String name) {
        return armMemoryManager.checkPtrHasAllocated(name);
    }

    public void loadImmediate(String reName, int immediate) {
        generator.addInstruction(new ArmMovw(new ArmRegister(reName), new ArmImmediateValue(immediate & 0x0000FFFF))); // 加载低 16 位
        generator.addInstruction(new ArmMovt(new ArmRegister(reName), new ArmImmediateValue((immediate & 0xFFFF0000 ) >>> 16)));
    }

    public void loadImmediate(String reName, float immediate) {
        generator.addInstruction(new ArmMovw(new ArmRegister(reName), new ArmImmediateValue(Float.floatToRawIntBits(immediate) & 0x0000FFFF))); // 加载低 16 位
        generator.addInstruction(new ArmMovt(new ArmRegister(reName), new ArmImmediateValue((Float.floatToRawIntBits(immediate) & 0xFFFF0000) >>> 16)));
    }

    public void loadLabel(String reName, String label) {
        generator.addInstruction(new ArmMovw(new ArmRegister(reName), new ArmLabelAddress(new ArmLabel(":lower16:" + label))));
        generator.addInstruction(new ArmMovt(new ArmRegister(reName), new ArmLabelAddress(new ArmLabel(":upper16:" + label))));
    }

    public boolean isLastLVal(ValueRef variable){
        return armLValLiveTable.isLastLVal(variable);
    }

    public void resetLastLVal(){
        armLValLiveTable.resetLastLVal();
    }

    public void setLastLVal(ValueRef variable){
        armLValLiveTable.setLastLVal(variable);
    }

    public void recordTempVar(LocalVar localVar){
        armTempVarLiveTable.record(localVar);
    }

    public boolean isUsedReg(String regName){
        return armTempVarLiveTable.isUsed(regName);
    }

    public String fetchTempVar(ValueRef variable){
        return armTempVarLiveTable.fetch(variable);
    }

    public boolean checkTempVarIsRecorded(ValueRef variable){
        return armTempVarLiveTable.isRecorded(variable);
    }


    /**
     * 将局部变量（当前存储在reg中）保存进入内存(需要保存的变量只可能是localVar)
     * @param variable 要保存的变量
     * @param regName 当前对应的值所在的寄存器
     */
    public void storeLocalVarIntoMemory(ValueRef variable, String regName){
        TypeRef type = variable.getType();
        if(type instanceof FloatType){
            generator.addInstruction(new ArmVstr_f32(new ArmRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof IntType || type instanceof BoolType){
            generator.addInstruction(new ArmStr(new ArmRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof Pointer){
            generator.addInstruction(new ArmStr(new ArmRegister(regName), getAddrOfLocalVar(variable)));
        } else {
            assert false;
        }
    }

    /**
     * 将内存中的局部变量加载到寄存器中（只可能是localVar）
     * @param variable 要加载的变量
     * @param regName 加载到的寄存器
     */
    public void loadLocalVarFromMemory(ValueRef variable, String regName){
        TypeRef type = variable.getType();
        if(type instanceof FloatType){
            generator.addInstruction(new ArmVldr_f32(new ArmRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof IntType || type instanceof BoolType){
            generator.addInstruction(new ArmLdr(new ArmRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof Pointer){
            generator.addInstruction(new ArmLdr(new ArmRegister(regName), getAddrOfLocalVar(variable)));
        } else {
            assert false;
        }
    }

    /**
     * 将内存中的localVar中的值拷贝到另一个localVar对应的内存中
     * @param src
     * @param dest
     * @param regName 用于中间存储值的寄存器
     */
    public void cpLocalVarBetweenMemory(ValueRef src, ValueRef dest, String regName){
        loadLocalVarFromMemory(src, regName);
        storeLocalVarIntoMemory(dest, regName);
    }
}
