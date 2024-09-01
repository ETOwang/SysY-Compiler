package cn.edu.nju.software.backendarm.arminstruction.operand;

public class ArmImmediateValue implements ArmOperand {

    long value;
    float fvalue;
    boolean isdouble = false;

    public ArmImmediateValue(long value) {
        this.value = value;
    }

    public ArmImmediateValue(float fvalue) {
        this.isdouble = true;
        this.fvalue = fvalue;
    }

    public long getValue() {
        return value;
    }

    public double getFValue() {
        return fvalue;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setFValue(  float fvalue) {
        this.fvalue = fvalue;
    }

    @Override
    public String toString() {
        if(isdouble) {
            int bits = Float.floatToRawIntBits(fvalue);
            return "0x"+Integer.toHexString(bits);
        }
        else {
            return "#" + value;
        }
    }
}
