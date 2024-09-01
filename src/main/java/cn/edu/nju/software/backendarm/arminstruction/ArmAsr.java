package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmAsr extends ArmDefaultInstruction {

    public ArmAsr(ArmOperand... armOperands) {
        super(ArmOpcode.ASR, armOperands);
    }
}
