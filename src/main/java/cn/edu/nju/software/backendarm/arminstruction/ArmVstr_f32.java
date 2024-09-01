package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVstr_f32 extends ArmDefaultInstruction {

    public ArmVstr_f32(ArmOperand... armOperands) {
        super(ArmOpcode.VSTR_F32, armOperands);
    }
}
