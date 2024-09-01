package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBl extends ArmDefaultInstruction {

    public ArmBl(ArmOperand... armOperands) {
        super(ArmOpcode.BL, armOperands);
    }
}
