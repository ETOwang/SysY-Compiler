package cn.edu.nju.software.ir.instruction.arithmetic;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.type.FloatType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class FDiv extends Arithmetic {

    public FDiv(ValueRef lVal, OpEnum op, ValueRef operand1, ValueRef operand2) {
        super(lVal, op, operand1, operand2);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ConstValue calculate() {
        if (operands[0] instanceof ConstValue && operands[1] instanceof ConstValue) {
            float op1 = (float) ((ConstValue) operands[0]).getValue(), op2 = (float) ((ConstValue) operands[1]).getValue();
            return new ConstValue(new FloatType(), op1 / op2);
        }
        return null;
    }

    @Override
    public boolean typeEquals(Instruction inst) {
        return inst instanceof FDiv;
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if (!(rhs instanceof FDiv fDiv)) {
            return false;
        }
        ValueRef[] operands=fDiv.getOperands();
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;
    }
}
