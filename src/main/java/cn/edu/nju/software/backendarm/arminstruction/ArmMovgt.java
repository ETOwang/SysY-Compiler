package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovgt extends ArmDefaultInstruction {

    public ArmMovgt(ArmOperand... armOperands) {
        super(ArmOpcode.MOVGT, armOperands);
    }
}
