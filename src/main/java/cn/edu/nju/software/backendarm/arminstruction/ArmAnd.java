package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmAnd extends ArmDefaultInstruction {

    public ArmAnd(ArmOperand... armOperands) {
        super(ArmOpcode.AND, armOperands);
    }
}
