package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.generator.InstructionVisitor;

import static cn.edu.nju.software.ir.instruction.OpEnum.BR;
import static cn.edu.nju.software.ir.instruction.Operator.getOperator;

public class Br extends Instruction {
    public Br(BasicBlockRef target) {
        operator = getOperator(BR);
        operands = new BasicBlockRef[]{target};
    }

    public BasicBlockRef getTarget() {
        return (BasicBlockRef) operands[0];
    }

    public void setTarget(BasicBlockRef bb) {
        operands[0] = bb;
    }

    @Override
    public boolean isBr() {
        return true;
    }
    @Override
    public String toString() {
        return "br label " + operands[0];
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        return super.equivalent(rhs);
    }
}
