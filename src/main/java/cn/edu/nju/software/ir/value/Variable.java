package cn.edu.nju.software.ir.value;

/**
 * This interface is designed for Constant Propagation
 */
public interface Variable {
   boolean isNAC();

   boolean isConstant();

   boolean isUndef();

   int getValue();

   void mergeValue(Value value);
}