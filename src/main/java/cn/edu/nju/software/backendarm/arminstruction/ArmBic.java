package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBic extends ArmDefaultInstruction {

    public ArmBic(ArmOperand... armOperands) {
        super(ArmOpcode.BIC, armOperands);
    }
}
