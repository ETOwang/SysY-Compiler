package cn.edu.nju.software.ir.value;

import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.type.*;

import java.util.ArrayList;
import java.util.stream.Stream;

public class GlobalVar extends ValueRef implements Variable {
    private final static ArrayList<String> usedNameList = new ArrayList<String>(){{add("");}};
    private final static ArrayList<Integer> usedFreqList = new ArrayList<Integer>(){{add(0);}};
    private ValueRef initVal;
    private boolean constant;
    private boolean isUninitialized = false;
    /**
     * constant: if the variable is defined by const
     * */
    public boolean isConst() {
        return constant;
    }
    public void setConst(boolean constant) {
        this.constant = constant;
    }
    /**
     * value is for constant propagation
     */
    private final Value value = Value.getUndef();
    public GlobalVar(TypeRef type, String name/*, boolean constant*/) {
        if (usedNameList.contains(name)) {
            int index = usedNameList.indexOf(name);
            this.name = name + usedFreqList.get(index);
            usedFreqList.set(index, usedFreqList.get(index) + 1);
        } else {
            this.name = name;
            usedFreqList.add(1);
            usedNameList.add(name);
        }
        this.type = type;
//        this.constant = constant;
    }


    public GlobalVar copy() {
        //全局唯一，无需拷贝
        return this;
    }

    public boolean isUninitialized() {
        return isUninitialized;
    }

    public void setUninitialized(boolean uninitialized) {
        isUninitialized = uninitialized;
    }

    public boolean isZeroInitializer() {
        return ((Pointer)type).getBase() instanceof ArrayType && !(initVal.getType() instanceof ArrayType); // initVal = zero
    }

    public void initialize(ValueRef value) {
        this.initVal = value;
    }

    public ValueRef getInitVal() {
        return initVal;
    }

    public static void clearNames() {
        Stream.of(usedNameList, usedFreqList)
                .forEach(ArrayList::clear);
    }

    @Override
    public String toString() {
        return "@" + name;
    }

    /**
     * the following methods are for constant propagation:
     */
    @Override
    public boolean isNAC() {
        return value.isNAC();
    }

    @Override
    public boolean isConstant() {
        return value.isConstant();
    }

    @Override
    public boolean isUndef() {
        return value.isUndef();
    }

    @Override
    public int getValue() {
        return value.getValue();
    }

    @Override
    public void mergeValue(Value value) {
        this.value.merge(value);
    }

    public int usedFreqInDifferentFunc() {
        ArrayList<FunctionValue> usedInFunc = new ArrayList<>();
        for (Instruction instruction : user) {
            if (!usedInFunc.contains(instruction.getBlock().getFunction())) {
                usedInFunc.add(instruction.getBlock().getFunction());
            }
        }
        return usedInFunc.size();
    }

    public boolean isRedundant() {
        return usedFreqInDifferentFunc() == 0;
    }

    public boolean isLocalizable() {
        return usedFreqInDifferentFunc() == 1 && (((Pointer)type).getBase() instanceof IntType || ((Pointer)type).getBase() instanceof FloatType)
                && getUsageFunction().getName().equals("main");
    }

    /***
     * called when isLocalizable
     */
    public FunctionValue getUsageFunction() {
        return user.get(0).getBlock().getFunction();
    }
}
