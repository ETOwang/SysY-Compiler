package cn.edu.nju.software.backendarm.regalloc;

import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashMap;
import java.util.Map;

public class ArmRegisterManager {

    private final Map<ValueRef,String> registerMap=new HashMap<>();

    private static final Map<FunctionValue,ArmRegisterManager> registerManager=new HashMap<>();

    public static ArmRegisterManager get(FunctionValue function) {
        if (!registerManager.containsKey(function)) {
            registerManager.put(function,new ArmRegisterManager());
        }
        return registerManager.get(function);
    }
    public void add(ValueRef ref,String value){
        if(!registerMap.containsKey(ref)){
            registerMap.put(ref,value);
        }
    }

    public String get(ValueRef ref){
        return registerMap.get(ref);
    }

    public boolean contains(ValueRef ref){
        return registerMap.containsKey(ref);
    }

    public boolean isUsed(String reg){
        return registerMap.containsValue(reg);
    }
}
