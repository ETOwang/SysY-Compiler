package cn.edu.nju.software.backendrisc.riscinstruction.operand;

/**
 * sp (sp = 0x100000) -> 0x100000
 */
public class RiscRegister implements RiscOperand {
    String regName;

    public RiscRegister(String name) {
        this.regName = name;
    }

    public void setReg(String regName) {
        this.regName = regName;
    }

    @Override
    public String toString() {
        return regName;
    }
}
