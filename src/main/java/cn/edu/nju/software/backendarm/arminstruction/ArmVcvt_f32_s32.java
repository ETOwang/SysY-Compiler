package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVcvt_f32_s32 extends ArmDefaultInstruction {

    public ArmVcvt_f32_s32(ArmOperand... armOperands) {
        super(ArmOpcode.VCVT_F32_S32, armOperands);
    }
}
