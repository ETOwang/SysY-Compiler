package cn.edu.nju.software.frontend.type;

public abstract class Type {
    private boolean isConst;
    public boolean isConst() {
        return isConst;
    }
    public void setConst(boolean c) {
        isConst = c;
    }
    public Class<? extends Type> getType() {
        return this.getClass();
    }


}
