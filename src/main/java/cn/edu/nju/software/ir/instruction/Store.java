package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.ArrayType;
import cn.edu.nju.software.ir.value.ValueRef;

public class Store extends Instruction {
    // please first implement pointer type (allocate), then use this class
    public Store(ValueRef value, ValueRef pointer) {
        operator = "store";
        operands = new ValueRef[]{value, pointer};
        value.addUser(this);
        pointer.addUser(this);
    }

    @Override
    public boolean isStore() {
        return true;
    }
    @Override
    public String toString() {
        if (operands[0].getType() instanceof ArrayType) { // array value itself has Type Declaration
            return "store " + operands[0] + ", "
                    + operands[1].getType() + " " + operands[1] + ", align " + operands[0].getType().getWidth();
        } else {
            return "store " + operands[0].getType() + " " + operands[0] + ", "
                    + operands[1].getType() + " " + operands[1] + ", align " + operands[0].getType().getWidth();
        }
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
