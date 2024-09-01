package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmBne extends ArmDefaultInstruction{
 public ArmBne(ArmOperand... armOperands){
     super(ArmOpcode.BNE,armOperands);
 }
}
