package cn.edu.nju.software.backendarm.arminstruction;

import cn.edu.nju.software.backendarm.arminstruction.util.ArmOpcode;

public class ArmVmrs extends ArmDefaultInstruction {

    public ArmVmrs(){
        super(ArmOpcode.VMRS);
    }

    @Override
    public String emitCode() {
        return "vmrs APSR_nzcv, FPSCR";
    }
}
