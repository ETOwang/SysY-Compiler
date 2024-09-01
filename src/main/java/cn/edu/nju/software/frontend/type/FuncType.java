package cn.edu.nju.software.frontend.type;

import java.util.ArrayList;

public class FuncType extends Type {
    private Type retType;

    private ArrayList<Type> paramsType;

    public FuncType() {}
    public FuncType(Type retType, ArrayList<Type> params) {
        this.retType = retType;
        paramsType = params;
    }
    public void update(Type retType, ArrayList<Type> params) {
        this.retType = retType;
        paramsType = params;
    }

    public Type getRetType() {
        return retType;
    }
    public ArrayList<Type> getParamsType() {
        return paramsType;
    }
    public String toString() {
        return "Type.FuncType";
    }
}
