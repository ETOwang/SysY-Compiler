package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBeq extends ArmDefaultInstruction {

    public ArmBeq(ArmOperand... armOperands) {
        super(ArmOpcode.BEQ, armOperands);
    }
}
