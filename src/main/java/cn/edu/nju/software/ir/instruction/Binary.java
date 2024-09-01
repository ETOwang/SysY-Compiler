package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

import static cn.edu.nju.software.ir.instruction.Operator.getOperator;

public class Binary extends Instruction {
    protected final TypeRef opType;
    public Binary(ValueRef lVal, OpEnum op, ValueRef operand1, ValueRef operand2) {
        this.lVal = lVal;
        opType = operand1.getType();
        operator = getOperator(op);
        operands = new ValueRef[]{operand1, operand2};
        operand1.addUser(this);
        operand2.addUser(this);
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    public TypeRef getOpType() {
        return opType;
    }
    @Override
    public String toString() {
        return lVal + " = " + operator + " " + opType + " " + operands[0] + ", " + operands[1];
    }

    public boolean typeEquals(Instruction instruction) {
        return false;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    /***
     * if 2 const or 0, return -1
     * @return index of the only const
     */
    public int getOnlyConst() {
        if (operands[0] instanceof ConstValue && operands[1] instanceof ConstValue) {
            return -1;
        }
        if (!(operands[0] instanceof ConstValue) && !(operands[1] instanceof ConstValue)) {
            return -1;
        }
        if (operands[0] instanceof ConstValue) {
            return 0;
        }
        return 1;
    }

    /***
     * been called if is cons exp
     * @return exp value
     */
    public ConstValue calculate() {
        return null;
    }
    // TODO tobe implemented in arithmetic inst
}
