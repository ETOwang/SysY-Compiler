package cn.edu.nju.software.frontend.type;
public class FloatType extends NumberType{
    public FloatType() {
        setConst(false);
    }

    public FloatType(boolean c) {
        setConst(c);
    }

    @Override
    public String toString() {
        return "Type.FloatType";
    }
}
