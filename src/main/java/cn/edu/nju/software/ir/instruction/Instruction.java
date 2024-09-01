package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.value.ValueRef;

public abstract class Instruction {
    protected ValueRef[] operands = new ValueRef[0];
    protected String operator;
    protected ValueRef lVal;
    protected BasicBlockRef block;
    protected boolean dbg = false;
//    protected final static String DELIMITER = ", ";

    public ValueRef getOperand(int index) {
        return operands[index];
    }

    public void setDbg(boolean dbg) {
        this.dbg = dbg;
    }

    public boolean isDbg() {
        return dbg;
    }
    /***
     * inst block
     * @param block
     */
    public void setBlock(BasicBlockRef block) {
        this.block = block;
    }

    public BasicBlockRef getBlock() {
        return block;
    }

    /***
     * replace old with new; after replacing, old will be deleting, so no need to modify old
     * @param index: replace position
     * @param valueRef: new operand
     */
    public void replace(int index, ValueRef valueRef) {
//        operands[index].dropUser(this); // old drop one user -- no need
        operands[index] = valueRef;
        valueRef.addUser(this); // new add user
    }

    public void replace(ValueRef old, ValueRef nw) {
        for (int i = 0; i < operands.length; i++) {
            if (operands[i].equals(old)) {
//                old.dropUser(this);
                operands[i] = nw;
                nw.addUser(this);
            }
        }
    }

    public ValueRef[] getOperands() {
        return operands;
    }

    public void setOperand(int index,ValueRef operand) {
        operands[index] = operand;
        operand.addUser(this);
    }
    public int getNumberOfOperands() {
        return operands.length;
    }

    public OpEnum getOp() {
        return Operator.getOp(operator);
    }

    public boolean equivalent(Instruction rhs){return false;};
    public ValueRef getLVal() {
        return lVal;
    }

    public boolean isArithmetic() {
        return false;
    }

    public boolean isLogic() {
        return false;
    }

    public boolean isCmp() {
        return false;
    }

    public boolean isCall() {
        return false;
    }

    public boolean isReturn() {
        return false;
    }

    public boolean isZExt() {
        return false;
    }

    public boolean isAlloc() {
        return false;
    }

    public boolean isStore() {
        return false;
    }

    public boolean isLoad() {
        return false;
    }

    public boolean isGEP() {
        return false;
    }

    public boolean isBr() {
        return false;
    }

    public boolean isBinary() {
        return false;
    }

    public abstract void accept(InstructionVisitor visitor);
}
