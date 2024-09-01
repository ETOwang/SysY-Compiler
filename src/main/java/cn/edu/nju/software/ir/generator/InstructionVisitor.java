package cn.edu.nju.software.ir.generator;

import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.instruction.arithmetic.*;
import cn.edu.nju.software.ir.instruction.logic.Ashr;
import cn.edu.nju.software.ir.instruction.logic.Logic;
import cn.edu.nju.software.ir.instruction.logic.Lshr;
import cn.edu.nju.software.ir.instruction.logic.Shl;

public interface InstructionVisitor {

    default void visit(Allocate allocate) {}

    // 这里已经被分解成了各个子类所以暂时用不到了
    @Deprecated
    default void visit(Arithmetic arithmetic) {}

    default void visit(Binary binary) {}

    default void visit(FSub fSub) {}

    default void visit(Mul mul) {}

    default void visit(FMul fmul) {}

    default void visit(Mod mod) {}

    default void visit(Div div) {}

    void visit(Ashr ashr);

    void visit(Shl shl);

    void visit(Lshr lshr);

    default void visit(FDiv fdiv) {}

    default void visit(Br br) {}

    default void visit(Call call) {}

    default void visit(Cmp cmp) {}

    default void visit(CondBr condBr) {}

    default void visit(FloatToInt floatToInt) {}

    default void visit(GEP getElementPtr) {}

    default void visit(IntToFloat intToFloat) {}

    default void visit(FAdd fAdd) {}

    default void visit(Sub sub) {}

    default void visit(Load load) {}

    default void visit(Logic logic){}

    default void visit(RetValue retValue) {}

    default void visit(RetVoid retVoid) {}

    default void visit(Store store) {}

    default void visit(ZExt zExt) {}

    default void visit(BitCast bitCast) {}

    default void visit(Ret ret) {}

    default void visit(Add add){}

    // new added TODO(): implement
    default void visit(Phi phi) {}

    default void visit(Default aDefault){};
    // new TODO(): implement
    default void visit(Move move) {}

}
