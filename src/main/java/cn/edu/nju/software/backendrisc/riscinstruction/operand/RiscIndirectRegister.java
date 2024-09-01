package cn.edu.nju.software.backendrisc.riscinstruction.operand;

/*
 * [sp] (sp = 0x1000000) -> [0x1000000]
 */
public class RiscIndirectRegister implements RiscOperand {
    private String regName;
    private int offset;

    public RiscIndirectRegister(String regName, int offset) {
        this.regName = regName;
        this.offset = offset;
    }

    public void addOffset(int offset) {
        this.offset += offset;
    }

    @Override
    public String toString() {
        return offset + "(" + regName +")";
    }
}
