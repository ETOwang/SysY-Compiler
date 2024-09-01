package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmLsr extends ArmDefaultInstruction {

    public ArmLsr(ArmOperand... armOperands) {
        super(ArmOpcode.LSR, armOperands);
    }
}