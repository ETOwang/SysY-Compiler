package cn.edu.nju.software.ir.value;

import cn.edu.nju.software.ir.type.BoolType;
import cn.edu.nju.software.ir.type.FloatType;
import cn.edu.nju.software.ir.type.IntType;

public class ConstValue extends ValueRef {
    private final Object value;

    public ConstValue(IntType type, Integer value) {
        this.type = type;
        this.value = value;
    }

    public ConstValue(FloatType type, Float value) {
        this.type = type;
        this.value = value;
    }

    public ConstValue(BoolType type, Boolean value) {
        this.type = type;
        this.value = value;
    }

    public ConstValue(IntType type, Integer value, String name) {
        this.type = type;
        this.value = value;
        this.name = name;
    }

    public ConstValue(FloatType type, Float value, String name) {
        this.type = type;
        this.value = value;
        this.name = name;
    }

    public ConstValue(BoolType type, Boolean value, String name) {
        this.type = type;
        this.value = value;
        this.name = name;
    }



    public ConstValue copy() {
        if(type instanceof IntType){
            return new ConstValue(new IntType(),(Integer) value);
        }else if(type instanceof FloatType){
            return new ConstValue(new FloatType(),(Float) value);
        }else {
            return new ConstValue(new BoolType(),(Boolean) value);
        }
    }

    public Object getValue() {
        return value;
    }

    public int castToInt() {
        if (value instanceof Integer integer) {
            return integer;
        } else if (value instanceof Float floatVal) {
            return (int)(float)floatVal;
        } else if (value instanceof Boolean booleanVal) {
            return booleanVal ? 1 : 0;
        } else {
            throw new RuntimeException("castToInt error");
        }
    }

    public String toRiscvString() {
        if (type instanceof IntType){
            if (value instanceof Integer) {
                return value.toString();
            } else {
                // Float
                return (int)((Float) value).floatValue() + "";
            }
        } else if (type instanceof BoolType) {
            return value.toString();
        } else if (type instanceof FloatType) {
            int floatBits = Float.floatToRawIntBits(Float.parseFloat(value.toString()));
            String hex = Long.toHexString(floatBits);
            return "0x" + hex;
        } else {
            throw new RuntimeException("Unexpected type");
        }
    }

    public String toArmString(){
        //same format with riscv
        return toRiscvString();
    }


    public String toString() {
        if (type instanceof IntType){
            if (value instanceof Integer) {
                return value.toString();
            } else {
                // Float
                return (int)((Float) value).floatValue() + "";
            }
        } else if (type instanceof BoolType) {
            return value.toString();
        } else if (type instanceof FloatType) {
            long floatBits = Double.doubleToRawLongBits(Float.parseFloat(value.toString()));
            String hex = Long.toHexString(floatBits);
            return "0x" + hex;
        } else {
            throw new RuntimeException("Unexpected type");
        }
    }

    @Override
    public boolean equals(ValueRef other) {
        if (other instanceof ConstValue) {
            if (type.equals(((ConstValue) other).type)) {
                if (type instanceof IntType) {
                    int t1 = (int) getValue(), t2 = (int) ((ConstValue) other).getValue();
                    return t1 == t2;
                } else if (type instanceof FloatType) {
                    float t1 = (float) getValue(), t2 = (float) ((ConstValue) other).getValue();
                    return t1 == t2;
                } else {
                    boolean t1 = (boolean) getValue(), t2 = (boolean) ((ConstValue) other).getValue();
                    return t1 == t2;
                }
            }
            return false;
        }
        return false;
    }

    public boolean greaterThan(ConstValue other) {
        if (type instanceof IntType) {
            int t1 = (int) getValue(), t2 = (int) ((ConstValue) other).getValue();
            return t1 > t2;
        } else if (type instanceof FloatType) {
            float t1 = (float) getValue(), t2 = (float) ((ConstValue) other).getValue();
            return t1 > t2;
        }
        return false;
    }

    public ConstValue add(ConstValue other) {
        if (type instanceof IntType) {
            int t1 = (int) getValue(), t2 = (int) ((ConstValue) other).getValue();
            return new ConstValue(new IntType(), t1 + t2);
        } else if (type instanceof FloatType) {
            float t1 = (float) getValue(), t2 = (float) ((ConstValue) other).getValue();
            return new ConstValue(new FloatType(), t1 + t2);
        }
        return null;
    }

    public ConstValue sub(ConstValue other) {
        if (type instanceof IntType) {
            int t1 = (int) getValue(), t2 = (int) ((ConstValue) other).getValue();
            return new ConstValue(new IntType(), t1 - t2);
        } else if (type instanceof FloatType) {
            float t1 = (float) getValue(), t2 = (float) ((ConstValue) other).getValue();
            return new ConstValue(new FloatType(), t1 - t2);
        }
        return null;
    }

    public ConstValue mul(ConstValue other) {
        if (type instanceof IntType) {
            int t1 = (int) getValue(), t2 = (int) ((ConstValue) other).getValue();
            return new ConstValue(new IntType(), t1 * t2);
        } else if (type instanceof FloatType) {
            float t1 = (float) getValue(), t2 = (float) ((ConstValue) other).getValue();
            return new ConstValue(new FloatType(), t1 * t2);
        }
        return null;
    }

    public ConstValue div(ConstValue other) {
        if (type instanceof IntType) {
            int t1 = (int) getValue(), t2 = (int) ((ConstValue) other).getValue();
            return new ConstValue(new IntType(), t1 / t2);
        } else if (type instanceof FloatType) {
            float t1 = (float) getValue(), t2 = (float) ((ConstValue) other).getValue();
            return new ConstValue(new FloatType(), t1 / t2);
        }
        return null;
    }
}
