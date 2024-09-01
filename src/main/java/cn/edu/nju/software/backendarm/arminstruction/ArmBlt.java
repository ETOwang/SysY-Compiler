package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBlt extends ArmDefaultInstruction {
    public ArmBlt(ArmOperand... armOperands) {
        super(ArmOpcode.BLT,armOperands);
    }
}
