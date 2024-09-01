package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.arithmetic.Add;
import cn.edu.nju.software.ir.instruction.arithmetic.FAdd;
import cn.edu.nju.software.ir.type.FloatType;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

import static cn.edu.nju.software.ir.instruction.OpEnum.ADD;

/***
 * move inst, in fact is Add/FAdd 0 inst
 */
public class Move extends Instruction {
    private cn.edu.nju.software.ir.instruction.arithmetic.Add add = null;
    private cn.edu.nju.software.ir.instruction.arithmetic.FAdd fAdd = null;

    private Allocate memory;
    public Move(ValueRef target, ValueRef src) {
        if (target.getType() instanceof IntType) {
            ConstValue zero = new ConstValue(new IntType(), 0);
            add = new Add(target, ADD, src, zero);
        } else if (target.getType() instanceof FloatType) {
            ConstValue fZero = new ConstValue(new FloatType(), 0f);
            fAdd = new FAdd(target, ADD, src, fZero);
        }
        src.addUser(this);
    }

    public void setMemory(Allocate memory) {
        this.memory = memory;
    }

    public Allocate getMemory() {
        return memory;
    }

    @Override
    public ValueRef getLVal() {
        if (add != null) {
            return add.getLVal();
        } else {
            return fAdd.getLVal();
        }
    }

    @Override
    public ValueRef getOperand(int i) {
        if (add != null) {
            return add.getOperand(i);
        } else {
            return fAdd.getOperand(i);
        }
    }

    public ValueRef getSrc() {
        return getOperand(0);
    }

    @Override
    public void replace(ValueRef oldValue, ValueRef newValue) {
        if (add != null) {
            add.replace(oldValue, newValue);
        } else if (fAdd != null) {
            fAdd.replace(oldValue, newValue);
        }
    }

    @Override
    public String toString() {
        if (add != null) {
            return add.toString();
        } else {
            return fAdd.toString();
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
