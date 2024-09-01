package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMla extends ArmDefaultInstruction {
    public ArmMla(ArmOperand... armOperands) {
        super(ArmOpcode.MLA, armOperands);
    }
}
