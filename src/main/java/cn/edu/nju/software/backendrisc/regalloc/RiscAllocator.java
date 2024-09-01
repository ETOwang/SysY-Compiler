package cn.edu.nju.software.backendrisc.regalloc;

import cn.edu.nju.software.backendrisc.RiscInstrGenerator;
import cn.edu.nju.software.backendrisc.riscinstruction.*;
import cn.edu.nju.software.backendrisc.riscinstruction.floatextension.RiscFlw;
import cn.edu.nju.software.backendrisc.riscinstruction.floatextension.RiscFmvwx;
import cn.edu.nju.software.backendrisc.riscinstruction.floatextension.RiscFsw;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.*;
import cn.edu.nju.software.backendrisc.riscinstruction.pseudo.RiscLi;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscComment;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscLabel;
import cn.edu.nju.software.ir.type.*;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.GlobalVar;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.ArrayList;
import java.util.List;

public class RiscAllocator {

    private RiscInstrGenerator generator;
    private final RiscMemoryManager riscMemoryManager = RiscMemoryManager.get();
    private static final RiscAllocator allocator = new RiscAllocator();
    private RiscTempVarLiveTable riscTempVarLiveTable;
    private RiscLValLiveTable riscLValLiveTable;
    private RiscAllocator() {}

    public static RiscAllocator get() {
        return allocator;
    }

    public void setLValLiveTable(RiscLValLiveTable riscLValLiveTable) {
        this.riscLValLiveTable = riscLValLiveTable;
    }

    public void setTempVarLiveTable(RiscTempVarLiveTable riscTempVarLiveTable) {
        this.riscTempVarLiveTable = riscTempVarLiveTable;
    }

    public void setInstrGenerator(RiscInstrGenerator generator) {
        this.generator = generator;
    }

    public void initialize() {
        riscMemoryManager.clear(); // reset for memory allocation in current func
    }

    public List<String> prepareOperands(ValueRef... values) {
        List<String> regNames = new ArrayList<>();
        generator.addInstruction(new RiscComment("fetch variables"));
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

    private String prepareAGlobal(GlobalVar globalVar, int i){
        generator.addInstruction(new RiscLa(new RiscRegister("r" + i), new RiscLabelAddress(new RiscLabel(globalVar.getName()))));
        return "t" + i;
    }

    private String prepareALocal(LocalVar localVar, int i){
        if (localVar.getType() instanceof FloatType) {
            if(isLastLVal(localVar)){
                riscTempVarLiveTable.release(localVar);
                return "ft0";
            }
            if(checkTempVarIsRecorded(localVar)){ //here is all localvar is temp
                return fetchTempVar(localVar);
            }
            generator.addInstruction(new RiscFlw(new RiscRegister("ft" + i), getAddrOfLocalVar(localVar)));
            return "ft" + i;
        } else if (localVar.getType() instanceof IntType || localVar.getType() instanceof BoolType) {
            if(isLastLVal(localVar)){
                riscTempVarLiveTable.release(localVar);
                return "t0";
            }
            if(checkTempVarIsRecorded(localVar)){ //here is all localvar is temp
                return fetchTempVar(localVar);
            }
            generator.addInstruction(new RiscLw(new RiscRegister("t" + i), getAddrOfLocalVar(localVar)));
            return "t" + i;
        } else if(localVar.getType() instanceof Pointer){
            if(isLastLVal(localVar)){
                riscTempVarLiveTable.release(localVar);
                return "t0";
            }
            if(checkTempVarIsRecorded(localVar)){ //here is all localvar is temp
                return fetchTempVar(localVar);
            }
            if(checkPtrHasAllocated(localVar.getName())){
                if(riscMemoryManager.getOffset(localVar) >= 2048 || riscMemoryManager.getOffset(localVar) <= -2048){
                    loadImmediate("t4", riscMemoryManager.getOffset(localVar));
                    generator.addInstruction(new RiscAdd(new RiscRegister("t" + i), new RiscRegister("sp"), new RiscRegister("t4")));
                } else {
                    generator.addInstruction(new RiscAddi(new RiscRegister("t" + i), new RiscRegister("sp"), new RiscImmediateValue(riscMemoryManager.getOffset(localVar))));
                }
                return "t" + i;
            }
            generator.addInstruction(new RiscLd(new RiscRegister("t" + i), getAddrOfLocalVar(localVar)));
            return "t" + i;
        } else {assert false;}
        return null;
    }

    private String prepareAConst(ConstValue constValue, int i){
        if (constValue.getType() instanceof FloatType) {
            generator.addInstruction(new RiscLi(new RiscRegister("t" + i), new RiscImmediateValue(Float.parseFloat(constValue.getValue().toString()))));
            generator.addInstruction(new RiscFmvwx(new RiscRegister("ft" + i), new RiscRegister("t" + i)));
            return "ft" + i;
        } else if (constValue.getType() instanceof IntType) {
            loadImmediate("t" + i, Integer.parseInt(constValue.getValue().toString()));
            return "t" + i;
        } else if (constValue.getType() instanceof BoolType) {
            loadImmediate("t" + i, Boolean.TRUE.equals(constValue.getValue()) ? 1 : 0);
            return "t" + i;
        } else {assert false;}
        return null;
    }

    /**
     * t3用于实现大立即数的转化
     * 返回这个变量的地址，（对于指针，就是这个指针本身的地址 而不是指针的值）
     * @param variable
     * @return
     */
    public RiscOperand getAddrOfLocalVar(ValueRef variable) {
        generator.insertComment("get address of local var:" + variable.getName());
        if (variable instanceof LocalVar) {
            if(checkPtrHasAllocated(variable.getName())){
                assert false;
            }
            return getRegWithOffset(riscMemoryManager.getOffset(variable), "sp", "t4");
        } else {
            assert false;
            return null;
        }
    }

    private RiscOperand getValueOfGlobalVar(GlobalVar globalVar){
        // GlobalVar 只可能是指针类型，所以获取value实际上就是指针中的地址（即label）
        generator.insertComment("get value of global var:" + globalVar.getName());
        generator.addInstruction(new RiscLa(new RiscRegister("t3"), new RiscLabelAddress(new RiscLabel(globalVar.getName()))));
        return new RiscRegister("t3");
    }

    private RiscOperand getValueOfConstVar(ConstValue constVar){
        generator.insertComment("get value of const var:" + constVar.getName());
        if(constVar.getType() instanceof FloatType){
            generator.addInstruction(new RiscLi(new RiscRegister("t1"), new RiscImmediateValue(Float.parseFloat(constVar.getValue().toString()))));
            generator.addInstruction(new RiscFmvwx(new RiscRegister("ft1"), new RiscRegister("t3")));
            return new RiscRegister("ft1");
        }
        else if(constVar.getType() instanceof IntType){
            loadImmediate("t1", Integer.parseInt(constVar.getValue().toString()));
            return new RiscRegister("t1");
        }
        else if(constVar.getType() instanceof BoolType){
            loadImmediate("t1", Boolean.TRUE.equals(constVar.getValue()) ? 1 : 0);
            return new RiscRegister("t1");
        }
        else {
            assert false;
            return null;
        }
    }

    /**
     * 获取局部变量的值，返回一个寄存器储存其值
     * @param localVar
     * @return
     */
    private RiscOperand getValueOfLocalVar(LocalVar localVar){
        generator.insertComment("get value of local var:" + localVar.getName());
        if(localVar.getType() instanceof FloatType) {
            generator.addInstruction(new RiscFlw(new RiscRegister("ft1"), getAddrOfLocalVar(localVar)));
            return new RiscRegister("ft1");
        } else if(localVar.getType() instanceof IntType || localVar.getType() instanceof BoolType){
            generator.addInstruction(new RiscLw(new RiscRegister("t1"), getAddrOfLocalVar(localVar)));
            return new RiscRegister("t1");
        } else if(localVar.getType() instanceof Pointer){
            if(checkPtrHasAllocated(localVar.getName())){
                loadImmediate("t1", riscMemoryManager.getOffset(localVar));
                generator.addInstruction(new RiscAdd(new RiscRegister("t1"), new RiscRegister("sp"), new RiscRegister("t1")));
                return new RiscRegister("t1");
            }
            generator.addInstruction(new RiscLd(new RiscRegister("t1"), getAddrOfLocalVar(localVar)));
            return new RiscRegister("t1");
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
    public RiscOperand getAddrOfVarPtrPointsToWithOffset(ValueRef ptr, int offset) {
        generator.insertComment("get address of " + ptr.getName() + " points to");
        if(!(ptr.getType() instanceof Pointer)){
            assert false;
            return null;
        }
        if(checkTempVarIsRecorded(ptr)){
            return getRegWithOffset(offset, fetchTempVar(ptr), "t3");
        }
        if(checkPtrHasAllocated(ptr.getName())){
            return getRegWithOffset(riscMemoryManager.getOffset(ptr) + offset, "sp", "t2");
        }
        if (ptr instanceof GlobalVar) {
            generator.addInstruction(new RiscLa(new RiscRegister("t3"), new RiscLabelAddress(new RiscLabel(ptr.getName()))));
            return getRegWithOffset(offset, "t3", "t4");
        } else if (ptr instanceof LocalVar) {
            generator.addInstruction(new RiscLd(new RiscRegister("t3"), getRegWithOffset(riscMemoryManager.getOffset(ptr), "sp", "t4")));
            return getRegWithOffset(offset, "t3", "t4");
        } else {
            assert false;
            return null;
        }
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
    public RiscOperand getRegWithOffset(int immediate, String baseReg, String destReg) {
        if(immediate >= 2048 || immediate < -2048){
            generator.addInstruction(new RiscLi(new RiscRegister(destReg), new RiscImmediateValue(immediate)));
            generator.addInstruction(new RiscAdd(new RiscRegister(destReg), new RiscRegister(baseReg), new RiscRegister(destReg)));
            return new RiscIndirectRegister(destReg,0);
        }else {
            return new RiscIndirectRegister(baseReg, immediate);
        }
    }

    public int getOffset(ValueRef var) {
        return riscMemoryManager.getOffset(var);
    }

    public void allocate(ValueRef var, int width) {
        riscMemoryManager.allocateInStack(var, Math.max(width, 4));
    }

    public void allocate(int width) {
        riscMemoryManager.allocateInStack(Math.max(width, 4));
    }

    public int getStackSize() {
        return riscMemoryManager.getSize();
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
        riscMemoryManager.align8byte();
    }

    public void alignStack16byte() {
        riscMemoryManager.align16byte();
    }

    public void addHasAllocatedPtr(String name) {
        riscMemoryManager.addHasAllocatedPtr(name);
    }

    public boolean checkPtrHasAllocated(String name) {
        return riscMemoryManager.checkPtrHasAllocated(name);
    }

    public void loadImmediate(String reName, int immediate) {
        if(immediate >= 2048 || immediate < -2048) {
            generator.addInstruction(new RiscLi(new RiscRegister(reName), new RiscImmediateValue(immediate)));
        }
        else {
            generator.addInstruction(new RiscAddi(new RiscRegister(reName), new RiscRegister("zero"),new RiscImmediateValue(immediate)));
        }
    }

    public boolean isLastLVal(ValueRef variable){
        return riscLValLiveTable.isLastLVal(variable);
    }

    public void resetLastLVal(){
        riscLValLiveTable.resetLastLVal();
    }

    public void setLastLVal(ValueRef variable){
       riscLValLiveTable.setLastLVal(variable);
    }

    public void recordTempVar(LocalVar localVar){
        riscTempVarLiveTable.record(localVar);
    }

    public boolean isUsedReg(String regName){
        return riscTempVarLiveTable.isUsed(regName);
    }

    public String fetchTempVar(ValueRef variable){
        return riscTempVarLiveTable.fetch(variable);
    }

    public boolean checkTempVarIsRecorded(ValueRef variable){
        return riscTempVarLiveTable.isRecorded(variable);
    }


    /**
     * 将局部变量（当前存储在reg中）保存进入内存(需要保存的变量只可能是localVar)
     * @param variable 要保存的变量
     * @param regName 当前对应的值所在的寄存器
     */
    public void storeLocalVarIntoMemory(ValueRef variable, String regName){
        TypeRef type = variable.getType();
        if(type instanceof FloatType){
            generator.addInstruction(new RiscFsw(new RiscRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof IntType || type instanceof BoolType){
            generator.addInstruction(new RiscSw(new RiscRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof Pointer){
            generator.addInstruction(new RiscSd(new RiscRegister(regName), getAddrOfLocalVar(variable)));
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
            generator.addInstruction(new RiscFlw(new RiscRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof IntType || type instanceof BoolType){
            generator.addInstruction(new RiscLw(new RiscRegister(regName), getAddrOfLocalVar(variable)));
        } else if(type instanceof Pointer){
            generator.addInstruction(new RiscLd(new RiscRegister(regName), getAddrOfLocalVar(variable)));
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
