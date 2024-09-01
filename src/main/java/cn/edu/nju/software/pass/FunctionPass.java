package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.value.FunctionValue;

public interface FunctionPass extends Pass {
    boolean runOnFunction(FunctionValue function);
}
