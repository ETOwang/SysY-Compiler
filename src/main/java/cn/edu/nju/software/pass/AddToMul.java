package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.instruction.arithmetic.Add;
import cn.edu.nju.software.ir.instruction.arithmetic.Mul;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashSet;
import java.util.Set;


public class AddToMul implements BasicBlockPass{
    @Override
    public boolean runOnBasicBlock(BasicBlockRef basicBlock) {
        doPass(basicBlock);
        return false;
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void setDbgFlag() {

    }

    private void doPass(BasicBlockRef basicBlock) {
        Set<Instruction> toBeRemoved = new HashSet<>();
        for (int i=0;i<basicBlock.getIrNum();i++){
            Instruction ir=basicBlock.getIr(i);
            if (ir instanceof Add add&&add.getOperand(0).equals(add.getOperand(1))) {
                ValueRef operand=add.getOperand(0);
                int count=0;
                Instruction temp=null;
                for (int j = i+1; j < basicBlock.getIrNum(); j++) {
                    temp=basicBlock.getIr(j);
                    if (temp instanceof Add&&
                            temp.getOperand(0).equals(basicBlock.getIr(j-1).getLVal())&&
                            temp.getOperand(1).equals(operand)) {
                        toBeRemoved.add(basicBlock.getIr(j));
                        count++;
                    }else {
                        temp=basicBlock.getIr(j-1);
                        i=j-1;
                        break;
                    }
                }
                if (count!=0) {
                    Instruction newInstr=new Mul(temp.getLVal(), OpEnum.MUL,operand,new ConstValue(new IntType(),count+2));
                    basicBlock.replaceIr(temp,newInstr);
                    toBeRemoved.add(ir);
                    break;
                }
            }
        }
        for (Instruction instr:toBeRemoved){
            basicBlock.dropIr(instr);
        }
    }

    private int  contains(ValueRef operand,Instruction ir){
        for (int i = 0; i < ir.getOperands().length; i++) {
            if(operand.equals(ir.getOperand(i))){
                return i;
            }
        }
        return -1;
    }
}
