package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMov extends ArmDefaultInstruction {

    public ArmMov(ArmOperand... armOperands) {
        super(ArmOpcode.MOV, armOperands);
    }
}
