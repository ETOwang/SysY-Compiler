package cn.edu.nju.software.ir.instruction;

import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.type.FunctionType;
import cn.edu.nju.software.ir.type.VoidType;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.Collections;
import java.util.List;

import static cn.edu.nju.software.ir.instruction.OpEnum.CALL;
import static cn.edu.nju.software.ir.instruction.Operator.getOperator;

public class Call extends Instruction {
    private final List<ValueRef> realParams;
    private final FunctionValue function;
    private int lineNo = -1;

    public Call(FunctionValue function, List<ValueRef> realParams) {
        operator = getOperator(CALL);
        this.function = function;
        this.realParams = realParams;
        for (ValueRef ref : this.realParams) {
            ref.addUser(this);
        }
    }

    public Call(ValueRef lVal, FunctionValue function, List<ValueRef> realParams) {
        this.lVal = lVal;
        operator = getOperator(CALL);
        this.function = function;
        this.realParams = realParams;
        for (ValueRef r : this.realParams) {
            r.addUser(this);
        }
    }

    public Call(FunctionValue function, List<ValueRef> realParams, int lineNo) {
        operator = getOperator(CALL);
        this.function = function;
        this.realParams = realParams;
        this.lineNo = lineNo;
        for (ValueRef r : this.realParams) {
            r.addUser(this);
        }
    }

    public Call(ValueRef lVal, FunctionValue function, List<ValueRef> realParams, int lineNo) {
        this.lVal = lVal;
        operator = getOperator(CALL);
        this.function = function;
        this.realParams = realParams;
        this.lineNo = lineNo;
        for (ValueRef r : this.realParams) {
            r.addUser(this);
        }
    }

    public List<ValueRef> getRealParams() {
        return Collections.unmodifiableList(realParams);
    }

    public FunctionValue getFunction() {
        return function;
    }

    public void setParam(int index,ValueRef param){
        realParams.set(index, param);
    }
    public int getLineNo() {
        return lineNo;
    }
    public int getParamsNum() {
        return realParams.size();
    }

    public ValueRef getRealParam(int index) {
        return realParams.get(index);
    }

    /***
     * special for Call(because different op struct) <br>
     * replace old with new; after replacing, old will be deleting, so no need to modify old
     * @param index: position
     * @param newVal: replace value
     */
    public void replaceRealParam(int index, ValueRef newVal) {
//        realParams.get(index).dropUser(this);
        realParams.set(index, newVal);
        newVal.addUser(this);
    }

    public void replaceRealParams(ValueRef old, ValueRef newVal) {
        int index = realParams.indexOf(old);
        if (index != -1) {
//            old.dropUser(this);
            realParams.set(index, newVal);
            newVal.addUser(this);
        }
    }

    @Override
    public boolean isCall() {
        return true;
    }
    @Override
    public String toString() {
        FunctionType ft = (FunctionType) function.getType();
        StringBuilder instr = new StringBuilder();
        if (!(ft.getReturnType() instanceof VoidType)) {
            instr.append(lVal).append(" = ");
        }
        String funcStr = function.toString();

        // macro substitution:
        if (funcStr.equals("@starttime") || funcStr.equals("@stoptime")) {
            funcStr = funcStr.replace("@", "@_sysy_");
            instr.append("call ").append(ft.getReturnType()).append(" ").append(funcStr).append("(");
            instr.append("i32 ").append(lineNo).append(")");
            return instr.toString();
        }

        instr.append("call ").append(ft.getReturnType()).append(" ").append(funcStr).append("(");
        for (int i = 0; i < realParams.size(); i++) {
            ValueRef param = realParams.get(i);
//            if (ft.getFParameter(i) instanceof Pointer) {
//                Pointer paramPtr = new Pointer(param);
//                instr.append(paramPtr).append(" ");
//            } else {
//                instr.append(param.getType()).append(" ");
//            }
            instr.append(ft.getFParameter(i)).append(" ");
            instr.append(param);
            if (i < realParams.size() - 1) {
                instr.append(", ");
            }
        }
        return instr.append(")").toString();
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        return super.equivalent(rhs);
    }
}
