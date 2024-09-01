package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVdiv_f32 extends ArmDefaultInstruction {

    public ArmVdiv_f32(ArmOperand... armOperands) {
        super(ArmOpcode.VDIV_F32, armOperands);
    }
}
