package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.Arrays;

import static cn.edu.nju.software.ir.instruction.OpEnum.PHI;
import static cn.edu.nju.software.ir.instruction.Operator.getOperator;

public class Phi extends Instruction {
    // private final BasicBlockRef block; // in which lock
    private final Allocate memory; // merge for which memory
    /***
     * phi: first value, second block
     * @param lVal: create value
     * @param block: in which block
     */
    public Phi(ValueRef lVal, BasicBlockRef block, Allocate memory) {
        this.lVal = lVal;
        this.block = block;
        operator = getOperator(PHI);
        operands = new ValueRef[0];
        this.memory = memory;
    }

    public void add(ValueRef value, BasicBlockRef block) {
        operands = Arrays.copyOf(operands, operands.length + 2);
        operands[operands.length - 2] = value;
        operands[operands.length - 1] = block;
//        System.err.println(value + ": " + this);
        value.addUser(this);
    }

    public ValueRef getValueByBlock(BasicBlockRef block) {
        for (int i = 1; i < operands.length; i += 2) {
            if (operands[i].equals(block)) {
                return operands[i - 1];
            }
        }
        return null;
    }

    public Allocate getMemory() {
        return memory;
    }

    /***
     * judge if this phi inst is necessary <br>
     * 2 condition is redundant: <br>
     * each value op is same; only 2 or less op
     * @return
     */
    public boolean isRedundant() {
        boolean flag = true;
        ValueRef vr = operands[0];
        for (int i = 2; i < operands.length; i += 2) {
            if (!vr.equals(operands[i])) {
                flag = false;
                break;
            }
            vr = operands[i];
        }
        return operands.length == 2 || flag;
    }

    public int indexOfBlock(BasicBlockRef block) {
        for (int i = 1; i < operands.length; i += 2) {
            if (operands[i].equals(block)) {
                return i;
            }
        }
        return -1;
    }

    /***
     * if contains this block's value
     * @param block: block in phi
     * @return
     */
    public boolean containsBlock(BasicBlockRef block) {
        return indexOfBlock(block) != -1;
    }

    /***
     * drop a pred value source
     * @param block: pred block
     */
    public void dropBlock(BasicBlockRef block) {
        int index = indexOfBlock(block);
        if (index == -1) {
            return;
        }
        ValueRef[] tmp = new ValueRef[operands.length - 2];
        int l = 0;
        for (int i = 0; i < operands.length; i++) {
            if (i == index - 1 || i == index) {
                continue;
            }
            tmp[l++] = operands[i];
        }
        operands = tmp;
    }

    public int getPredSize() {
        return operands.length / 2;
    }

    public BasicBlockRef getPredBlock(int index) {
        return (BasicBlockRef) operands[index * 2 + 1];
    }

    /***
     * if self is redundant, call this func <br>
     * modify self's influence
     */
    public void modify() {
        if (operands.length == 0) {
            return;
        }
        ValueRef phiVal = this.getLVal();
        ValueRef replace = this.getOperand(0); // value
        // replace old(phiVal) with new value(replace); after replacing. old will be deleting
        for (Instruction instruction : phiVal.getUser()) {
            if (instruction instanceof Call call) {
                call.replaceRealParams(phiVal, replace);
            } else {
                instruction.replace(phiVal, replace);
            }
        }
    }

    /***
     * e.g. %0 = phi i32 [%1, %10], [%0, %11]
     */
    public boolean specialModify() {
        if (operands.length != 4) {
            return false;
        }
        ValueRef lVal = this.getLVal();
        ValueRef replace = null;
        if (operands[0].equals(lVal)) {
            replace = operands[2];
        } else if (operands[2].equals(lVal)) {
            replace = operands[0];
        }
        if (replace != null) {
            for (Instruction instruction : lVal.getUser()) {
                if (instruction.equals(this)) {
                    continue;
                }
                if (instruction instanceof Call call) {
                    call.replaceRealParams(lVal, replace);
                } else {
                    instruction.replace(lVal, replace);
                }
            }
            return true;
        }
        return false;
    }
    @Override
    public String toString() {
        String s =  lVal.getText() +
                " = phi " +
                lVal.getType().getText() + " ";
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < operands.length; i += 2) {
            sb.append("[").append(operands[i]).append(", ").append(operands[i + 1]).append("]");
            if (i + 2 < operands.length) {
                sb.append(", ");
            }
        }
        return sb.toString();
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
