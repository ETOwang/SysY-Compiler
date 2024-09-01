package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVldr_f32 extends ArmDefaultInstruction {

    public ArmVldr_f32(ArmOperand... armOperands) {
        super(ArmOpcode.VLDR_F32, armOperands);
    }
}
