package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.instruction.arithmetic.Add;
import cn.edu.nju.software.ir.instruction.arithmetic.Sub;
import cn.edu.nju.software.ir.instruction.logic.Ashr;
import cn.edu.nju.software.ir.instruction.logic.Lshr;
import cn.edu.nju.software.ir.instruction.logic.Shl;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.LocalVar;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class StrengthReductionPass implements BasicBlockPass {

    private BasicBlockRef bb;
    private boolean dbgFlag = false;
    private static StrengthReductionPass strengthReductionPass;

    @Override
    public boolean runOnBasicBlock(BasicBlockRef basicBlock) {
        bb = basicBlock;
        ListIterator<Instruction> iterator = bb.getIrs().listIterator();
        while (iterator.hasNext()) {
            Instruction ir = iterator.next();
            if (isMulInstruction(ir)) {
                List<Instruction> newInstructions = replaceMul(ir);
                iterator.remove();
                for (Instruction newIr : newInstructions) {
                    iterator.add(newIr);
                    newIr.setBlock(bb);
                }
            } else if (isDivInstruction(ir)) {
                List<Instruction> newInstructions = replaceDiv(ir);
                iterator.remove();
                for (Instruction newIr : newInstructions) {
                    iterator.add(newIr);
                    newIr.setBlock(bb);
                }
            }
        }
        if(dbgFlag){
            printDbgInfo();
        }
        return false;
    }

    @Override
    public void setDbgFlag() {
        dbgFlag = true;
    }

    @Override
    public void printDbgInfo() {
        /* todo() waiting to add dbg info*/
    }

    @Override
    public String getName() {
        return "Strength Reduction Pass";
    }

    public static StrengthReductionPass getInstance() {
        if (strengthReductionPass == null) {
            strengthReductionPass = new StrengthReductionPass();
        }
        return strengthReductionPass;
    }

    private boolean isMulInstruction(Instruction ir) {
        return ir.getOp() == OpEnum.MUL;
    }

    private boolean isDivInstruction(Instruction ir) {
        return ir.getOp() == OpEnum.DIV;
    }

    private List<Instruction> replaceMul(Instruction ir) {
        if(!(ir.getOperand(0) instanceof ConstValue) && !(ir.getOperand(1) instanceof ConstValue)){
            List<Instruction> newInstructions = new ArrayList<>();
            newInstructions.add(ir);
            ir.setBlock(bb);
            return newInstructions;
        }
        for(int i = 0; i < 2; i++){
            if(ir.getOperand(i) instanceof ConstValue constValue && (int) constValue.getValue() == 1){
                return reduceForMul1(ir, i);
            }
            if(ir.getOperand(i) instanceof ConstValue constValue && (int) constValue.getValue() == -1){
                return reduceForMulNeg1(ir, i);
            }
            if(ir.getOperand(i) instanceof ConstValue constValue && (int) constValue.getValue() == 0){
                return reduceForMul0(ir,i);
            }
            if(ir.getOperand(i) instanceof ConstValue constValue && isPowerOfTwo((int) constValue.getValue())){
                return reduceForMulPowerOf2(ir, i);
            }
            if(ir.getOperand(i) instanceof ConstValue constValue && isPowerOfTwo(-(int) constValue.getValue())){
                return reduceForMulNegPowerOf2(ir, i);
            }

        }
        List<Instruction> newInstructions = new ArrayList<>();
        newInstructions.add(ir);
        ir.setBlock(bb);
        return newInstructions;
    }

    private List<Instruction> reduceForMulPowerOf2(Instruction ir, int index){
        List<Instruction> newInstructions = new ArrayList<>();
        ConstValue constValue = (ConstValue) ir.getOperand(index);
        Integer bits = Integer.numberOfTrailingZeros(Math.abs((int) constValue.getValue()));
        newInstructions.add(new Shl(
                ir.getLVal(),
                ir.getOperand(index == 0 ? 1 : 0),
                new ConstValue(new IntType(), bits)));
        return newInstructions;
    }

    private List<Instruction> reduceForMulNegPowerOf2(Instruction ir, int index){
        List<Instruction> newInstructions = new ArrayList<>();
        ConstValue constValue = (ConstValue) ir.getOperand(index);
        Integer bits = Integer.numberOfTrailingZeros(Math.abs((int) constValue.getValue()));
        LocalVar temp = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Shl(
                temp,
                ir.getOperand(index == 0 ? 1 : 0),
                new ConstValue(new IntType(), bits)));
        newInstructions.add(new Sub(
                ir.getLVal(),
                OpEnum.SUB,
                new ConstValue(new IntType(), 0),
                temp));
        return newInstructions;
    }

    private List<Instruction> reduceForMul1(Instruction ir, int index){
        List<Instruction> newInstructions = new ArrayList<>();
        newInstructions.add(new Add(
                ir.getLVal(),
                OpEnum.ADD,
                ir.getOperand(index == 0 ? 1 : 0),
                new ConstValue(new IntType(), 0)));
        return newInstructions;
    }

    private List<Instruction> reduceForMulNeg1(Instruction ir, int index){
        List<Instruction> newInstructions = new ArrayList<>();
        newInstructions.add(new Sub(
                ir.getLVal(),
                OpEnum.SUB,
                new ConstValue(new IntType(), 0),
                ir.getOperand(index == 0 ? 1 : 0)));
        return newInstructions;
    }

    private List<Instruction> reduceForMul0(Instruction ir, int index){
        List<Instruction> newInstructions = new ArrayList<>();
        newInstructions.add(new Add(
                ir.getLVal(),
                OpEnum.ADD,
                new ConstValue(new IntType(), 0),
                new ConstValue(new IntType(), 0)));
        return newInstructions;
    }

    private List<Instruction> replaceDiv(Instruction ir){
        List<Instruction> newInstructions = new ArrayList<>();
        if(!(ir.getOperand(1) instanceof ConstValue constValue)){
            newInstructions.add(ir);
            ir.setBlock(bb);
            return newInstructions;
        }
        int value = (int) constValue.getValue();
        if (value == 1) {
            return reduceForDiv1(ir);
        } else if (value == -1) {
            return reduceForDivNeg1(ir);
        } else if (isPowerOfTwo(value)){
            return reduceForDivPowerOf2(ir);
        } else if (isPowerOfTwo(-value)) {
            return reduceForDivNegPowerOf2(ir);
        }
        newInstructions.add(ir);
        ir.setBlock(bb);
        return newInstructions;
    }

    private List<Instruction> reduceForDivPowerOf2(Instruction ir){
        List<Instruction> newInstructions = new ArrayList<>();
        ConstValue constValue = (ConstValue) ir.getOperand(1);
        Integer bits = Integer.numberOfTrailingZeros(Math.abs((int) constValue.getValue()));
        LocalVar temp1 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Ashr(
                temp1,
                ir.getOperand(0),
                new ConstValue(new IntType(), bits - 1)));
        LocalVar temp2 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Lshr(
                temp2,
                temp1,
                new ConstValue(new IntType(), 32 - bits)));
        LocalVar temp3 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Add(
                temp3,
                OpEnum.ADD,
                ir.getOperand(0),
                temp2));
        newInstructions.add(new Ashr(
                ir.getLVal(),
                temp3,
                new ConstValue(new IntType(), bits)));
        ir.getLVal().setTmp(false);
        return newInstructions;
    }

    private List<Instruction> reduceForDivNegPowerOf2(Instruction ir){
        List<Instruction> newInstructions = new ArrayList<>();
        ConstValue constValue = (ConstValue) ir.getOperand(1);
        Integer bits = Integer.numberOfTrailingZeros(Math.abs((int) constValue.getValue()));
        LocalVar temp1 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Ashr(
                temp1,
                ir.getOperand(0),
                new ConstValue(new IntType(), bits - 1)));
        LocalVar temp2 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Lshr(
                temp2,
                temp1,
                new ConstValue(new IntType(), 32 - bits)));
        LocalVar temp3 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Add(
                temp3,
                OpEnum.ADD,
                ir.getOperand(0),
                temp2));
        LocalVar temp4 = bb.createLocalVar(new IntType(), "temp");
        newInstructions.add(new Ashr(
                temp4,
                temp3,
                new ConstValue(new IntType(), bits)));
        newInstructions.add(new Sub(
                ir.getLVal(),
                OpEnum.SUB,
                new ConstValue(new IntType(), 0),
                temp4));
        ir.getLVal().setTmp(false);
        return newInstructions;
    }

    private List<Instruction> reduceForDiv1(Instruction ir){
        List<Instruction> newInstructions = new ArrayList<>();
        newInstructions.add(new Add(
                ir.getLVal(),
                OpEnum.ADD,
                ir.getOperand(0),
                new ConstValue(new IntType(), 0)));
        return newInstructions;
    }

    private List<Instruction> reduceForDivNeg1(Instruction ir){
        List<Instruction> newInstructions = new ArrayList<>();
        newInstructions.add(new Sub(
                ir.getLVal(),
                OpEnum.SUB,
                new ConstValue(new IntType(), 0),
                ir.getOperand(0)));
        return newInstructions;
    }

    private boolean isPowerOfTwo(int n) {
        return (n & (n - 1)) == 0;
    }
}