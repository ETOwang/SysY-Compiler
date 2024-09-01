package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmStr extends ArmDefaultInstruction {

        public ArmStr(ArmOperand... armOperands) {
            super(ArmOpcode.STR, armOperands);
        }
}
