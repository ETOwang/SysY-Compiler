package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBge extends ArmDefaultInstruction{
    public ArmBge(ArmOperand... armOperands){
            super(ArmOpcode.BGE,armOperands);
    }
}
