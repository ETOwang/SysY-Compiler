package cn.edu.nju.software.backendrisc;

import cn.edu.nju.software.backendrisc.regalloc.RiscAllocator;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Allocate;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.type.FunctionType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.LinkedList;
import java.util.List;

public class RiscFunction {

    private final FunctionValue functionValue;
    private final List<RiscBasicBlock> riscBasicBlocks = new LinkedList<>();
    private final RiscAllocator allocator = RiscAllocator.get();

    public RiscFunction(FunctionValue functionValue) {
        this.functionValue = functionValue;
    }

    public void codeGen() {
        allocator.initialize();
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
            allocator.allocate(new LocalVar(functionType.getFParameter(i), i + ""), allocator.getSizeOfType(functionType.getFParameter(i)));
        }
    }

    /**
     * [为函数内部的局部变量预留内存空间]
     * <p>
     * [所有出现的变量全部都要预留内存空间]
     * </p>
     */

    private void reserveSpaceForLocalVariables() {
        for(int i = 0; i < functionValue.getBasicBlockRefs().size(); i++){
            BasicBlockRef bb = functionValue.getBasicBlockRefs().get(i);
            if(i == 0){
                bb.setIsEntryBlock(true);
            }
            for(int j = 0; j < bb.getIrs().size(); j++){
                Instruction ir = bb.getIrs().get(j);
                if(ir.getLVal() != null){
                    if(! (ir instanceof Allocate)){
                        reserveMemoryForType(ir.getLVal(), ir.getLVal().getType());
                    } else {
                        reserveMemoryForType(ir.getLVal(), ((Pointer)ir.getLVal().getType()).getBase());
                        allocator.addHasAllocatedPtr(ir.getLVal().getName());
                    }
                }
            }
        }
    }

    private void alignStack8byte(){
        allocator.alignStack8byte();
    }

    private void alignStack16byte(){
        allocator.alignStack16byte();
    }

    private void reserveMemoryForType(ValueRef variable, TypeRef type) {
        alignStack8byte();
        allocator.allocate(variable, allocator.getSizeOfType(type));
    }

    private void genRiscBasicBlocks() {
        for (BasicBlockRef bb : functionValue.getBasicBlockRefs()) {
            RiscBasicBlock riscBasicBlock = new RiscBasicBlock(bb, functionValue);
            riscBasicBlock.codeGen();
            riscBasicBlocks.add(riscBasicBlock);
        }
    }

    public void dumpToConsole() {
        System.out.println(".text");
        System.out.println(".align 1");
        System.out.println(".type " + functionValue.getName() + ", @function");
        System.out.println(".globl " + functionValue.getName());
        System.out.println(functionValue.getName() + ":");
        riscBasicBlocks.forEach(RiscBasicBlock::dumpToConsole);
    }
}
