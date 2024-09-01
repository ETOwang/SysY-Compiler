package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovw extends ArmDefaultInstruction {

        public ArmMovw(ArmOperand... armOperands) {
            super(ArmOpcode.MOVW, armOperands);
        }

        @Override
        public String emitCode() {
            return "\tmovw" + " " + getOperands().get(0) + ", " + getOperands().get(1);
        }
}
