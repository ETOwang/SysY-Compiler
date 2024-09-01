package cn.edu.nju.software.frontend.util;

/**
 * T: self value
 * T2: key
 * */
public class Symbol<T> {
    private final String name;
    private final T value;

    private boolean valid;

    public Symbol(String name, T ctx) {
        this.name = name;
        this.value = ctx;
        valid = true;
    }

    public T getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean v) {
        valid = v;
    }
}
