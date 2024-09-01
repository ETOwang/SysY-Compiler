package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.ValueRef;

public class ZExt extends Instruction {
    private final TypeRef target;
    public ZExt(ValueRef lVal, ValueRef operand, TypeRef target) {
        this.lVal = lVal;
        operator = "zext";
        operands = new ValueRef[]{operand};
        this.target = target;
        operand.addUser(this);
    }

    @Override
    public boolean isZExt() {
        return true;
    }

    @Override
    public String toString() {
        return lVal + " = zext " + operands[0].getType() + " " + operands[0] + " to " + target;
    }
    public TypeRef getTarget(){
        return target;
    }
    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if(!(rhs instanceof ZExt zExt)){
            return false;
        }
        if(!zExt.getTarget().equals(this.target)){
            return false;
        }
        ValueRef[] operands=zExt.getOperands();
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;
    }
}
