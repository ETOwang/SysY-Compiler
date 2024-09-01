package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Cmp;
import cn.edu.nju.software.ir.instruction.CondBr;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.ZExt;

public class OptOptimize implements BasicBlockPass{
    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }

    @Override
    public boolean runOnBasicBlock(BasicBlockRef basicBlock) {
        doPass(basicBlock);
        return true;
    }

    @Override
    public String getName() {
        return "";
    }

    private void doPass(BasicBlockRef block){
        Instruction last=block.getIr(block.getIrNum()-1);
        if (last instanceof CondBr) {
            if(block.getIrNum()-4<0){
                return;
            }
            Instruction first=block.getIr(block.getIrNum()-4);
            Instruction second=block.getIr(block.getIrNum()-3);
            Instruction third=block.getIr(block.getIrNum()-2);
            if(first instanceof Cmp&&second instanceof ZExt &&third instanceof Cmp){
                last.setOperand(0,first.getLVal());
                block.dropIr(second);
                block.dropIr(third);
            }
        }
    }
}
