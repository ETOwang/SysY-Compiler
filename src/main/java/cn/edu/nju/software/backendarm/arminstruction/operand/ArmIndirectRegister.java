package cn.edu.nju.software.backendarm.arminstruction.operand;

public class ArmIndirectRegister implements ArmOperand {
    private String regName;
    private int offset;
    private String opsh;
    private int opOffset;
    private String rm;
    public ArmIndirectRegister(String regName, int offset) {
        this.regName = regName;
        this.offset = offset;
    }

    public ArmIndirectRegister(String regName, String opsh, int opOffset, String rm) {
        this.regName = regName;
        this.opsh = opsh;
        this.opOffset = opOffset;
        this.rm = rm;
    }

    public String getRegName() {
        return regName;
    }

    public int getOffset() {
        return offset;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setRegName(String regName) {
        this.regName = regName;
    }
    public void addOffset(int offset) {
        this.offset += offset;
    }

    public String getOpsh() {
        return opsh;
    }
    @Override
    public String toString() {
        if(rm!=null){
            return "[" + regName + ", " + rm+", " +opsh+" #"+opOffset+"]";
        }
        return "[" + regName + ", " + "#" + offset + "]";
    }
}
