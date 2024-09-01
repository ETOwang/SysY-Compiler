package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmUdiv extends ArmDefaultInstruction {

    public ArmUdiv(ArmOperand... armOperands) {
        super(ArmOpcode.UDIV, armOperands);
    }
}
