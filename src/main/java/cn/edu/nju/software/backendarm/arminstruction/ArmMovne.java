package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMovne extends ArmDefaultInstruction {
    public ArmMovne(ArmOperand... ins) {
        super(ArmOpcode.MOVNE, ins);
    }
}
