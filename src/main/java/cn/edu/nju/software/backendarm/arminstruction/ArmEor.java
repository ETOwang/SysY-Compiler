package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmEor extends ArmDefaultInstruction {

    public ArmEor(ArmOperand... armOperands) {
        super(ArmOpcode.EOR, armOperands);
    }
}
