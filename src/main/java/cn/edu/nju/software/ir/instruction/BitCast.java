package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.ArrayType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.value.ValueRef;

public class BitCast extends Instruction {
    /**
     * srcArr: generally the variable been allocated
     * */
    public BitCast(ValueRef lVal, ValueRef srcArr) {
        this.lVal = lVal;
        operator = "bitcast";
        operands = new ValueRef[]{srcArr};
        srcArr.addUser(this);
    }

    @Override
    public String toString() {
        return lVal + " = bitcast " + operands[0].getType() + " " + operands[0] +
                " to " + new Pointer(((ArrayType)((Pointer)operands[0].getType()).getBase()).getBaseType());
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if (!(rhs instanceof BitCast bitCast)) {
            return false;
        }
        ValueRef[] operands=bitCast.getOperands();
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;
    }
}
