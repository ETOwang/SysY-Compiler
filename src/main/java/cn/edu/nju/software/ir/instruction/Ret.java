package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.value.ValueRef;

import static cn.edu.nju.software.ir.instruction.OpEnum.RETURN;
import static cn.edu.nju.software.ir.instruction.Operator.getOperator;

public class Ret extends Instruction {
    public Ret() {
        operator = getOperator(RETURN);
    }

    public Ret(ValueRef retVal) {
        operator = getOperator(RETURN);
        operands = new ValueRef[]{retVal};
        retVal.addUser(this);
    }

    @Override
    public boolean isReturn() {
        return true;
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
