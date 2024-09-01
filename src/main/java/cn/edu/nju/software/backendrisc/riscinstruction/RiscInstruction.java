package cn.edu.nju.software.backendrisc.riscinstruction;

import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscOpcode;

import java.util.ArrayList;

public interface RiscInstruction {

    RiscOpcode getOpCode() ;

    ArrayList<RiscOperand> getOperands();

    String emitCode();
}
