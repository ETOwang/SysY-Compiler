package cn.edu.nju.software.ir.type;

import cn.edu.nju.software.ir.value.ValueRef;

public class Pointer extends TypeRef {
    private final TypeRef base;
    public Pointer(ValueRef value) {
        this(value.getType());
    }

    public Pointer(TypeRef base) {
        this.base = base;
        this.width = 4; // todo: not sure
    }

    public String toString() {
        return base.toString() + "*";
    }
    public TypeRef getBase() {
        return base;
    }
}
