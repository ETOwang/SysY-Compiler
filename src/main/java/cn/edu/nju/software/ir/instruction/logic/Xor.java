package cn.edu.nju.software.ir.instruction.logic;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.type.BoolType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Xor extends Logic {

    public Xor(ValueRef lVal, OpEnum op, ValueRef operand1, ValueRef operand2) {
        super(lVal, op, operand1, operand2);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    /***
     * XOR second operand is always one
     * @return result const
     */
    @Override
    public ConstValue calculate() {
        if (operands[0] instanceof ConstValue && operands[1] instanceof ConstValue) {
            if (operands[0].equals(new ConstValue(new BoolType(), true))) {
                return new ConstValue(new BoolType(), false);
            } else {
                return new ConstValue(new BoolType(), true);
            }
        }
        return null;
    }

    @Override
    public boolean typeEquals(Instruction inst) {
        return inst instanceof Xor;
    }
    @Override
    public boolean equivalent(Instruction rhs) {
        if (!(rhs instanceof Xor xor)) {
            return false;
        }
        Set<ValueRef> self=new HashSet<>(List.of(operands));
        Set<ValueRef> other=new HashSet<>(List.of(xor.operands));
        return self.equals(other);
    }
}

