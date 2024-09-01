package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVcvt_s32_f32 extends ArmDefaultInstruction {

    public ArmVcvt_s32_f32(ArmOperand... armOperands) {
        super(ArmOpcode.VCVT_S32_F32, armOperands);
    }
}
