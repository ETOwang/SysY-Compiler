package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBle extends ArmDefaultInstruction{
    public ArmBle(ArmOperand... armOperands) {
          super(ArmOpcode.BLE,armOperands);
    }
}
