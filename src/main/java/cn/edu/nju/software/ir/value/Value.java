package cn.edu.nju.software.ir.value;

/**
 * for constant propagation, representing a const value or NAC or Undef
 */
public class Value {
    private Kind kind;
    private int value;

    private Value(Kind kind) {
        this.kind = kind;
        this.value = 0;
    }

    private Value(int value) {
        this.kind = Kind.CONSTANT;
        this.value = value;
    }

    public static Value getNAC() {
        return new Value(Kind.NAC);
    }

    public static Value getUndef() {
        return new Value(Kind.UNDEF);
    }

    public static Value makeConstant(int val) {
        return new Value(val);
    }

    public boolean isConstant() {
        return kind == Kind.CONSTANT;
    }

    public boolean isNAC() {
        return kind == Kind.NAC;
    }

    public boolean isUndef() {
        return kind == Kind.UNDEF;
    }

    public int getValue() {
        if (kind == Kind.CONSTANT) {
            return value;
        } else {
            throw new RuntimeException(this + " is not constant!");
        }
    }

    public void merge(Value val) {
        if (kind == Kind.UNDEF) {
            this.value = val.value;
            this.kind = val.kind;
        } else if (kind == Kind.CONSTANT) {
            if (val.kind == Kind.NAC) {
                this.kind = Kind.NAC;
                this.value = 0;
            } else if (val.kind == Kind.CONSTANT) {
                if (this.value != val.value) {
                    this.kind = Kind.NAC;
                    this.value = 0;
                }
            }
        }
    }

    enum Kind {
      NAC,
      CONSTANT,
      UNDEF,
    };
}