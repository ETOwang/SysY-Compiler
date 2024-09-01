package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmMoveq extends ArmDefaultInstruction {
    public ArmMoveq(ArmOperand... ins) {
        super(ArmOpcode.MOVEQ, ins);
    }
}
