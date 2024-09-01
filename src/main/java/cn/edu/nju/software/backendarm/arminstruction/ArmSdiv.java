package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmSdiv extends ArmDefaultInstruction {

    public ArmSdiv(ArmOperand... armOperands) {
        super(ArmOpcode.SDIV, armOperands);
    }
}
