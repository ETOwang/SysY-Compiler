package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

import java.util.ArrayList;
import java.util.List;

public abstract class RiscDefaultInstruction implements RiscInstruction  {
    RiscOpcode op;
    ArrayList<RiscOperand> riscOperands;

    public RiscDefaultInstruction(RiscOpcode op, RiscOperand... riscOperands) {
        this.op = op;
        this.riscOperands = new ArrayList<>(List.of(riscOperands));
    }

    public RiscOpcode getOpCode() {
        return op;
    }

    public ArrayList<RiscOperand> getOperands() {
        return riscOperands;
    }

    public String emitCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        sb.append(op.toString().toLowerCase().replace("_", "."));
        sb.append(" ");
        sb.append(String.join(", ", riscOperands.stream().map(RiscOperand::toString).toList()));
        return sb.toString();
    }


}
