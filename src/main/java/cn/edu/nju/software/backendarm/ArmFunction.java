package cn.edu.nju.software.backendarm;
import cn.edu.nju.software.backendarm.regalloc.ArmAllocator;
import cn.edu.nju.software.backendarm.regalloc.ArmRegisterManager;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Allocate;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.Load;
import cn.edu.nju.software.ir.type.FunctionType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;
import cn.edu.nju.software.pass.ValueAnalyzePass;

import java.util.LinkedList;
import java.util.List;

public class ArmFunction {

    private final FunctionValue functionValue;
    private final List<ArmBasicBlock> armBasicBlocks = new LinkedList<>();
    private final ArmAllocator armAllocator;
    private final ArmRegisterManager registerManager ;
    public ArmFunction(FunctionValue functionValue) {
        this.functionValue = functionValue;
        this.registerManager=ArmRegisterManager.get(functionValue);
        this.armAllocator=ArmAllocator.get(functionValue);

    }

    public void codeGen() {
        armAllocator.initialize();
        allocateSpace();
        genRiscBasicBlocks();
    }

    private void allocateSpace() {
        reserveSpaceForFParams();
        reserveSpaceForLocalVariables();
        alignStack16byte();
    }

    /**
     * [为函数参数预留内存空间]
     * <p>
     * [在llvm中，函数参数是直接通过%0,%1...来引用的，这些形式参数也要预留内存空间]
     * </p>
     */
    private void reserveSpaceForFParams() {
        FunctionType functionType = (FunctionType) functionValue.getType();
        for(int i = 0; i < functionType.getFParametersCount(); i++){
            armAllocator.allocate(new LocalVar(functionType.getFParameter(i), i + ""), armAllocator.getSizeOfType(functionType.getFParameter(i)));
        }
    }

    /**
     * [为函数内部的局部变量预留内存空间]
     * <p>
     * [所有出现的变量全部都要预留内存空间]
     * </p>
     */

//    private void reserveSpaceForLocalVariables() {
//        for(int i = 0; i < functionValue.getBasicBlockRefs().size(); i++){
//            BasicBlockRef bb = functionValue.getBasicBlockRefs().get(i);
//            if(i == 0){
//                bb.setIsEntryBlock(true);
//            }
//            for(int j = 0; j < bb.getIrs().size(); j++){
//                Instruction ir = bb.getIrs().get(j);
//                if(ir.getLVal() != null){
//                    if(! (ir instanceof Allocate)){
//                        reserveMemoryForType(ir.getLVal(), ir.getLVal().getType());
//                    } else {
//                        reserveMemoryForType(ir.getLVal(), ((Pointer)ir.getLVal().getType()).getBase());
//                        armAllocator.addHasAllocatedPtr(ir.getLVal().getName());
//                    }
//                }
//            }
//        }
//    }
    private void reserveSpaceForLocalVariables() {
        List<ValueRef> sortedValues= ValueAnalyzePass.getInstance().getSortedValue(functionValue);
        if(sortedValues!=null){
            preProcessValue(sortedValues);
        }
        for(int i = 0; i < functionValue.getBasicBlockRefs().size(); i++){
            BasicBlockRef bb = functionValue.getBasicBlockRefs().get(i);
            if(i == 0){
                bb.setIsEntryBlock(true);
            }
            for(int j = 0; j < bb.getIrs().size(); j++){
                Instruction ir = bb.getIrs().get(j);
                if(ir.getLVal() != null){
                    if(! (ir instanceof Allocate)){
                        if(registerManager.contains(ir.getLVal())){
                            continue;
                        }
                        reserveMemoryForType(ir.getLVal(), ir.getLVal().getType());
                    } else {
                        if(registerManager.contains(ir.getLVal())){
                            continue;
                        }
                        reserveMemoryForType(ir.getLVal(), ((Pointer)ir.getLVal().getType()).getBase());
                        armAllocator.addHasAllocatedPtr(ir.getLVal().getName());
                    }
                }
            }
        }
    }


    private void alignStack8byte(){
        armAllocator.alignStack8byte();
    }

    private void alignStack16byte(){
        armAllocator.alignStack16byte();
    }

    private void reserveMemoryForType(ValueRef variable, TypeRef type) {
        alignStack8byte();
        armAllocator.allocate(variable, armAllocator.getSizeOfType(type));
    }

    private void genRiscBasicBlocks() {
        for (BasicBlockRef bb : functionValue.getBasicBlockRefs()) {
            ArmBasicBlock armBasicBlock = new ArmBasicBlock(bb, functionValue);
            armBasicBlock.codeGen();
            armBasicBlocks.add(armBasicBlock);
        }
    }

    public void dumpToConsole() {
        System.out.println(".text");
        System.out.println(".align 1");
        System.out.println(".type " + functionValue.getName() + ", %function");
        System.out.println(".globl " + functionValue.getName());
        System.out.println(functionValue.getName() + ":");
        armBasicBlocks.forEach(ArmBasicBlock::dumpToConsole);
    }

    public List<ArmBasicBlock> getArmBasicBlocks() {
        return armBasicBlocks;
    }

    private void preProcessValue(List<ValueRef> sortedValues) {
        String[] localVarRegs= ArmSpecifications.getLocalVarRegs();
        int index=0;
        for(ValueRef vr : sortedValues){
            if(index<localVarRegs.length-1){
                registerManager.add(vr,localVarRegs[index++]);
            }
        }
        for (BasicBlockRef bb : functionValue.getBasicBlockRefs()){
            for(Instruction ir : bb.getIrs()){
                if(ir instanceof Load load){
                    if(registerManager.contains(load.getOperand(0))){
                        registerManager.add(ir.getLVal(),registerManager.get(load.getOperand(0)));
                    }
                }
            }
        }
    }

}