package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmOr extends ArmDefaultInstruction {

        public ArmOr(ArmOperand... armOperands) {
            super(ArmOpcode.OR, armOperands);
        }
}
