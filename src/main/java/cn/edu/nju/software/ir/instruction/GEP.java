package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.value.ValueRef;

public class GEP extends Instruction {
    /**
     * a pointer point to the array type
     * */
    private final Pointer arrayTypePtr;
    public GEP(ValueRef lVal, Pointer arrayTypePtr, ValueRef[] operands) {
        this.lVal = lVal;
        operator = "getelementptr";
        this.arrayTypePtr = arrayTypePtr;
        this.operands = operands;
        for (ValueRef operand : operands) {
            operand.addUser(this);
        }
    }

    public Pointer getArrayTypePtr() {
        return arrayTypePtr;
    }
    @Override
    public String toString() {
        StringBuilder instr = new StringBuilder(lVal + " = ");
        instr.append(operator).append(" ").append(arrayTypePtr.getBase()).append(", ").append(arrayTypePtr).append(" ").append(operands[0]);
        for (int i = 1; i < operands.length; i++) {
            instr.append(", ").append(operands[i].getType()).append(" ").append(operands[i]);
        }
        return instr.toString();
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        if(!(rhs instanceof GEP gep)) {
            return false;
        }
        if(!gep.getArrayTypePtr().equals(arrayTypePtr)){
            return false;
        }
        ValueRef[] operands=gep.getOperands();
        if(operands.length!= this.operands.length){
            return false;
        }
        for (int i=0;i<this.operands.length;i++){
            if(!this.operands[i].equals(operands[i])){
                return false;
            }
        }
        return true;

    }
}
