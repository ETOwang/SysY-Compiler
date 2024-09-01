package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmAdd extends ArmDefaultInstruction {

    public ArmAdd(ArmOperand... armOperands) {
        super(ArmOpcode.ADD, armOperands);
    }
}
