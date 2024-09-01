package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

import java.util.ArrayList;

public interface ArmInstruction {

    ArmOpcode getOpCode();

    ArrayList<ArmOperand> getOperands();

    String emitCode();
}
