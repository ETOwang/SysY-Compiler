package cn.edu.nju.software.ir.type;

import java.util.ArrayList;

public class FunctionType extends TypeRef {
    private final TypeRef returnType;
    private final ArrayList<TypeRef> fParameters;
    private final int fParametersCount; // form params cnt

    public FunctionType(TypeRef returnType, ArrayList<TypeRef> parameters, int parameterCount) {
        this.returnType = returnType;
        this.fParameters = parameters;
        this.fParametersCount = parameterCount;
    }

    public TypeRef getReturnType() {
        return returnType;
    }

    public int getFParametersCount() {
        return fParametersCount;
    }

    public ArrayList<TypeRef> getFParameters() {
        return fParameters;
    }

    public TypeRef getFParameter(int index) {
        if (index < 0 || index >= fParametersCount) {
            return null;
        }
        return fParameters.get(index);
    }
}
