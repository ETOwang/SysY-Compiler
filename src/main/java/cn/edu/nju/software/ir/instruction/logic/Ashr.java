package cn.edu.nju.software.ir.instruction.logic;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.Binary;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.OpEnum;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class Ashr extends Binary {

    public Ashr(ValueRef lVal, ValueRef operand1, ValueRef operand2) {
        super(lVal, OpEnum.ASHR, operand1, operand2);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean typeEquals(Instruction inst) {
        return inst instanceof Ashr;
    }

    @Override
    public ConstValue calculate() {
        if (operands[0] instanceof ConstValue && operands[1] instanceof ConstValue) {
            int bit = (int) ((ConstValue) operands[1]).getValue();
            int src = (int) ((ConstValue) operands[0]).getValue();
            return new ConstValue(new IntType(), src >> bit);
        }
        return null;
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if (!(rhs instanceof Ashr ashr)) {
            return false;
        }
        ValueRef[] operands=ashr.getOperands();
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;
    }
}
