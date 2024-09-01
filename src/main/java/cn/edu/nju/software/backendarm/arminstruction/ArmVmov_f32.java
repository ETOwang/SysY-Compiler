package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVmov_f32 extends ArmDefaultInstruction {

    public ArmVmov_f32(ArmOperand... armOperands) {
        super(ArmOpcode.VMOV_F32, armOperands);
    }
}
