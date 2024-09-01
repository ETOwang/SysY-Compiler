package cn.edu.nju.software.frontend.type;

public class ArrayType extends Type {
    private final Type elementType;

    public ArrayType(Type type) {
        elementType = type;
        setConst(false);
    }
    public ArrayType(Type type, boolean c) {
        elementType = type;
        setConst(c);
    }
    public Type getElementType() {
        return elementType;
    }
    public int level() {
        Type type = elementType;
        int level = 1;
        while (type instanceof ArrayType) {
            level++;
            type = ((ArrayType) type).getElementType();
        }
        return level;
    }
    public String toString() {
        return "Type.ArrayType <level " + level() + ">" ;
    }
}
