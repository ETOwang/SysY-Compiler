package cn.edu.nju.software.ir.type;

public class TypeRef {
    protected int width;

    /**
     * get type string: i1, i32 ...
     * */
    public String getText() {
        return this.toString();
    }
    public int getWidth() {
        return width;
    }

    public boolean equals(TypeRef other) {
        return this.getClass().equals(other.getClass());
    }
}
