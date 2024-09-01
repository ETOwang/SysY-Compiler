package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBgt extends ArmDefaultInstruction{
    public ArmBgt(ArmOperand... armOperands){
            super(ArmOpcode.BGT,armOperands);
    }
}
