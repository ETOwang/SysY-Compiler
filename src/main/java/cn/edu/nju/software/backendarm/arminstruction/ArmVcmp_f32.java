package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVcmp_f32 extends ArmDefaultInstruction {

        public ArmVcmp_f32(ArmOperand... armOperands) {
            super(ArmOpcode.VCMP_F32, armOperands);
        }
}
