package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.BoolType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.Objects;

import static cn.edu.nju.software.ir.instruction.Operator.cmpType;

public class Cmp extends Binary {
    private final String type;
    public Cmp(ValueRef lVal, OpEnum op, int type, ValueRef operand1, ValueRef operand2) {
        super(lVal, op, operand1, operand2);
        this.type = cmpType[type];
    }

    public Cmp(ValueRef lVal, OpEnum op, String type, ValueRef operand1, ValueRef operand2) {
        super(lVal, op, operand1, operand2);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean isCmp() {
        return true;
    }

    @Override
    public String toString() {
        String instr = lVal + " = " + operator + " " + type + " " + opType + " ";
        instr += operands[0] + ", ";
        instr += operands[1];
        return instr;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ConstValue calculate() {
        BoolType i1 = new BoolType();
        ConstValue cv = null;
        if (operands[0] instanceof ConstValue && operands[1] instanceof ConstValue) {
            switch (type) {
                case "ne":
                case "one":
                    cv = new ConstValue(i1, !operands[0].equals(operands[1]));
                    break;
                case "eq":
                case "oeq":
                    cv = new ConstValue(i1, operands[0].equals(operands[1]));
                    break;
                case "sgt":
                case "ogt":
                    cv = new ConstValue(i1, ((ConstValue) operands[0]).greaterThan((ConstValue) operands[1]));
                    break;
                case "slt":
                case "olt":
                    cv = new ConstValue(i1, !((ConstValue) operands[0]).greaterThan((ConstValue) operands[1]) && !operands[0].equals(operands[1]));
                    break;
                case "sle":
                case "ole":
                    cv = new ConstValue(i1,  !((ConstValue) operands[0]).greaterThan((ConstValue) operands[1]));
                    break;
                case "sge":
                case "oge":
                    cv = new ConstValue(i1, operands[0].equals(operands[1]) || ((ConstValue) operands[0]).greaterThan((ConstValue) operands[1]));
                    break;
                default:
                    break;
            }
            return cv;
        }
        return null;
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if(!(rhs instanceof Cmp cmp)){
            return false;
        }
        if(!Objects.equals(cmp.getType(), this.getType())){
            return false;
        }
        ValueRef[] operands=cmp.getOperands();
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;
    }
}
