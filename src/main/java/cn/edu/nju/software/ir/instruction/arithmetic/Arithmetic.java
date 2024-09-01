package cn.edu.nju.software.ir.instruction.arithmetic;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.Binary;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.value.ValueRef;

public class Arithmetic extends Binary {
    public Arithmetic(ValueRef lVal, OpEnum op, ValueRef operand1, ValueRef operand2) {
        super(lVal, op, operand1, operand2);
    }

    @Override
    public boolean isArithmetic() {
        return true;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
