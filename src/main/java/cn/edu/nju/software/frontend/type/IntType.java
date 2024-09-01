package cn.edu.nju.software.frontend.type;

public class IntType extends NumberType {
    private int value;

    public IntType() {
        setConst(false);
    }
    public IntType(boolean c) {
        setConst(c);
    }
    public int getValue() {
        return value;
    }
    public void updateValue(int value) {
        this.value = value;
        setConst(false);
    }
    public String toString() {
        return "Type.IntType";
    }
}
