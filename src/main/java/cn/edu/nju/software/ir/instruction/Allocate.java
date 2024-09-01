package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.ValueRef;

import static cn.edu.nju.software.ir.instruction.Operator.getOperator;

public class Allocate extends Instruction {
    // please make sure that pointer(lVal) is a pointer type variable
    public Allocate(ValueRef pointer) {
        operator = getOperator(OpEnum.ALLOC);
        lVal = pointer;
    }

    @Override
    public boolean isAlloc() {
        return true;
    }
    @Override
    public String toString() {
        TypeRef base = ((Pointer)lVal.getType()).getBase();
        return lVal + " = alloca " + base + ", align " + base.getWidth();
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
