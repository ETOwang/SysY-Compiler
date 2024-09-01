package cn.edu.nju.software.backendarm;

import cn.edu.nju.software.backendarm.arminstruction.*;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmImmediateValue;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmIndirectRegister;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmRegister;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmComment;
import cn.edu.nju.software.backendarm.regalloc.ArmAllocator;
import cn.edu.nju.software.backendarm.regalloc.ArmLValLiveTable;
import cn.edu.nju.software.backendarm.regalloc.ArmTempVarLiveTable;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.type.*;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.LocalVar;

import java.util.ArrayList;
import java.util.List;

public class ArmBasicBlock {

    private final BasicBlockRef basicBlockRef;
    private final ArmAllocator allocator ;
    private final FunctionValue llvmFunctionValue;
    private final List<ArmInstruction> armInstructions = new ArrayList<>() ;
    private final ArmInstrGenerator generator;
    private final ArmTempVarLiveTable armTempVarLiveTable;
    private final ArmLValLiveTable armLValLiveTable;

    public ArmBasicBlock(BasicBlockRef basicBlockRef, FunctionValue functionValue) {
        this.basicBlockRef = basicBlockRef;
        this.llvmFunctionValue = functionValue;
        this.generator = new ArmInstrGenerator(basicBlockRef.getIrs(), llvmFunctionValue);
        this.allocator=ArmAllocator.get(llvmFunctionValue);
        this.armTempVarLiveTable = new ArmTempVarLiveTable(generator, allocator);
        this.armLValLiveTable = new ArmLValLiveTable();
    }

    public void codeGen() {
        allocator.setLValLiveTable(armLValLiveTable);
        allocator.setTempVarLiveTable(armTempVarLiveTable);
        allocator.setInstrGenerator(generator);
        if (basicBlockRef.isEntryBlock()) {
            functionInit();
        }
        armInstructions.addAll(generator.genArmInstructions());
    }

    private void functionInit() {
        generator.insertComment("reserve space for all local variables in function");
        int stackSize = allocator.getStackSize();
        if (stackSize > 0) {
            if(stackSize <= 256){
                generator.addInstruction(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(-stackSize)));
            } else {
                allocator.loadImmediate("r4", stackSize);
                generator.addInstruction(new ArmSub(new ArmRegister("sp"), new ArmRegister("sp"), new ArmRegister("r4")));
            }
        }
        if(!llvmFunctionValue.getName().equals("main")){ //main没有调用函数，也没有参数
            saveCalleeSavedRegs();
            saveParams();
        }
    }

    public ArmInstrGenerator getGenerator() {
        return generator;
    }

    /**
     * 保存参数
     * 依次检查参数的类系，如果是General，就从a0-a7中取出,如果是Float就从fa0-fa7中取出
     * 如果参数个数超过8个，依次从栈中取出
     * 对于每一函数的参数，都为其分配了一个LocalVar（%0,%1,...）后续对于传入的参数的值的使用都是通过这个LocalVar来引用的
     * 因此需要将参数寄存器中的的值保存到这些LocalVar中
     */
    private void saveParams() {
        generator.insertComment("save the parameters value in the regs");
        int preLen = getPredLen();
        FunctionType functionType = (FunctionType) llvmFunctionValue.getType();
        String[] fArgs = ArmSpecifications.getFArgRegs();
        String[] args = ArmSpecifications.getArgRegs();
        int fptr = 0;
        int ptr = 0;
        int order = 0; //order 相对于prelen的偏移量，分别对应第一个被存入栈中的参数，第二个被存入栈中的参数...
        for (int i = 0; i < functionType.getFParametersCount(); i++) {
            if (functionType.getFParameter(i) instanceof FloatType) {
                if(fptr >= ArmSpecifications.getFArgRegs().length){
                    fetchFromStack(functionType.getFParameter(i), i, preLen, order++);
                } else {
                    allocator.storeLocalVarIntoMemory(new LocalVar(functionType.getFParameter(i), i +""), fArgs[fptr++]);
                }
            } else if (functionType.getFParameter(i) instanceof IntType || functionType.getFParameter(i) instanceof Pointer){
                if(ptr >= ArmSpecifications.getArgRegs().length){
                    fetchFromStack(functionType.getFParameter(i), i, preLen, order++);
                } else {
                    allocator.storeLocalVarIntoMemory(new LocalVar(functionType.getFParameter(i), i +""), args[ptr++]);
                }
            } else {assert false;}
        }
    }

    /**
     * 获取第一个参数相对于栈底的偏移量
     */
    public int getPredLen(){
        FunctionType functionType = (FunctionType) llvmFunctionValue.getType();
        int intTypeCount = functionType.getFParameters().stream()
                .filter(IntType.class::isInstance)
                .mapToInt(typeRef -> 1)
                .sum();
        int floatTypeCount = functionType.getFParameters().stream()
                .filter(FloatType.class::isInstance)
                .mapToInt(typeRef -> 1)
                .sum();
        int pointerTypeCount = functionType.getFParameters().stream()
                .filter(Pointer.class::isInstance)
                .mapToInt(typeRef -> 1)
                .sum();
        int intAndPointerCount = intTypeCount + pointerTypeCount;
        return (
                ((intAndPointerCount > ArmSpecifications.getArgRegs().length) ? (intAndPointerCount - ArmSpecifications.getArgRegs().length) : 0) +
                        ((floatTypeCount > ArmSpecifications.getFArgRegs().length) ? (floatTypeCount - ArmSpecifications.getFArgRegs().length) : 0)
        ) * 8 + (ArmSpecifications.getCallerSavedRegs().length - 1) * 8;
    }

    private void fetchFromStack(TypeRef type, int i, int preLen, int order) {
        String destReg = "r0";
        if (type instanceof IntType) {
            generator.addInstruction(new ArmLdr(new ArmRegister("r0"), allocator.getRegWithOffset(allocator.getStackSize() + preLen - order * 8, "sp", "r4")));
            destReg = "r0";
        } else if (type instanceof FloatType) {
            generator.addInstruction(new ArmVldr_f32(new ArmRegister("s0"), allocator.getRegWithOffset(allocator.getStackSize() + preLen - order * 8, "sp", "r4")));
            destReg = "s0";
        } else if(type instanceof Pointer){
            generator.addInstruction(new ArmLdr(new ArmRegister("r0"), allocator.getRegWithOffset(allocator.getStackSize() + preLen - order * 8, "sp", "r4")));
            destReg = "r0";
        } else {assert false;}
        allocator.storeLocalVarIntoMemory(new LocalVar(type, i + ""), destReg);
    }

    private void saveCalleeSavedRegs() {
        generator.insertComment("save CallerSavedRegs");
        String[] calleeSavedRegs = ArmSpecifications.getCalleeSavedRegs();
        generator.addInstruction(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(-8 * calleeSavedRegs.length)));
        for (int i = 0; i < calleeSavedRegs.length; i++) {
            generator.addInstruction(new ArmStr(new ArmRegister(calleeSavedRegs[i]), new ArmIndirectRegister("sp", i * 8)));
        }
    }

    public void dumpToConsole() {
        System.out.println(basicBlockRef.getName() + ":");
        for(ArmInstruction armInstruction : armInstructions){
            if(armInstruction instanceof ArmComment){
                if(ArmSpecifications.getIsDebug()){
                    System.out.println(armInstruction.emitCode());
                }
            } else {
                System.out.println(armInstruction.emitCode());
            }
        }
    }

    public List<ArmInstruction> getArmInstructions() {
        return armInstructions;
    }
}
