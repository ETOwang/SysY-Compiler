package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmCmp extends ArmDefaultInstruction {

        public ArmCmp(ArmOperand... armOperands) {
            super(ArmOpcode.CMP, armOperands);
        }
}
