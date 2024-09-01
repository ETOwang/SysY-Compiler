package cn.edu.nju.software.ir.instruction.logic;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class And extends Logic {

    public And(ValueRef lVal, OpEnum op, ValueRef operand1, ValueRef operand2) {
        super(lVal, op, operand1, operand2);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if (!(rhs instanceof And and)) {
            return false;
        }
        Set<ValueRef> self=new HashSet<>(List.of(operands));
        Set<ValueRef> other=new HashSet<>(List.of(and.operands));
        return self.equals(other);
    }
}
