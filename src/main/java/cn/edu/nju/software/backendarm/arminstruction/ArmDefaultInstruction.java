package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

import java.util.ArrayList;
import java.util.List;

public class ArmDefaultInstruction implements ArmInstruction {
    ArmOpcode op;

    ArrayList<ArmOperand> armOperands;

    public ArmDefaultInstruction(ArmOpcode op, ArmOperand... armOperands) {
        this.op = op;
        this.armOperands = new ArrayList<>(List.of(armOperands));
    }

    public ArmOpcode getOpCode() {
        return op;
    }

    public ArrayList<ArmOperand> getOperands() {
        return armOperands;
    }

    public String emitCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        sb.append(op.toString().toLowerCase().replace("_", "."));
        sb.append(" ");
        sb.append(String.join(", ", armOperands.stream().map(ArmOperand::toString).toList()));
        return sb.toString();
    }
}
