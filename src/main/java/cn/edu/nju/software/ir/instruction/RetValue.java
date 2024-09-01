package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.value.ValueRef;

public class RetValue extends Ret {
    public RetValue(ValueRef retVal) {
        super(retVal);
    }

    @Override
    public String toString() {
        return "ret " + operands[0].getType() + " " + operands[0];
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
