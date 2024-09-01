package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.value.ValueRef;

public class FloatToInt extends Instruction {
    public FloatToInt(ValueRef lVal, ValueRef floatVal) {
        this.lVal = lVal;
        operator = "fptosi";
        operands = new ValueRef[]{floatVal};
        floatVal.addUser(this);
    }

    @Override
    public String toString() {
        return lVal + " = fptosi float " + operands[0] + " to i32";
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if(!(rhs instanceof FloatToInt floatToInt)){
            return false;
        }
        ValueRef[] operands=floatToInt.getOperands();
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;
    }
}
