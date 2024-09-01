package cn.edu.nju.software.ir.generator;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.instruction.arithmetic.*;
import cn.edu.nju.software.ir.instruction.logic.Ashr;
import cn.edu.nju.software.ir.instruction.logic.Logic;
import cn.edu.nju.software.ir.instruction.logic.Lshr;
import cn.edu.nju.software.ir.instruction.logic.Shl;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.ArrayList;
import java.util.List;


public class IrCloneVisitor implements InstructionVisitor {
    private Instruction curInstruction;
    public Instruction genClonedInstruction(Instruction instruction) {
        instruction.accept(this);
        return curInstruction;
    }

    @Override
    public void visit(GEP gep) {
        ValueRef lVal = gep.getLVal().copy();
        ValueRef[] operands = new ValueRef[gep.getNumberOfOperands()];
        for (int i = 0; i < operands.length; i++) {
            operands[i] = gep.getOperand(i).copy();
        }
        curInstruction = new GEP(lVal, gep.getArrayTypePtr(), operands);
    }

    @Override
    public void visit(Store store) {
        ValueRef src = store.getOperand(0).copy();
        ValueRef dest = store.getOperand(1).copy();
        curInstruction = new Store(src, dest);
    }

    @Override
    public void visit(Allocate allocate) {
        ValueRef lVal = allocate.getLVal().copy();
        curInstruction = new Allocate(lVal);
    }

    @Override
    public void visit(Load load) {
        ValueRef src =load.getOperand(0).copy();
        ValueRef lVal = load.getLVal().copy();
        curInstruction = new Load(lVal, src);
    }

    @Override
    public void visit(Add add) {
        ValueRef lVal = add.getLVal().copy();
        ValueRef operand1 = add.getOperand(0).copy();
        ValueRef operand2 = add.getOperand(1).copy();
        curInstruction = new Add(lVal, add.getOp(), operand1, operand2);
    }

    @Override
    public void visit(FAdd fAdd) {
        ValueRef lVal =fAdd.getLVal().copy();
        ValueRef operand1 = fAdd.getOperand(0).copy();
        ValueRef operand2 =fAdd.getOperand(1).copy();
        curInstruction = new FAdd(lVal, fAdd.getOp(), operand1, operand2);
    }


    @Override
    public void visit(Sub sub) {
        ValueRef lVal = sub.getLVal().copy();
        ValueRef operand1 = sub.getOperand(0).copy();
        ValueRef operand2 = sub.getOperand(1).copy();
        curInstruction = new Sub(lVal, sub.getOp(), operand1, operand2);
    }

    @Override
    public void visit(FSub fSub) {
        ValueRef lVal = fSub.getLVal().copy();
        ValueRef operand1 = fSub.getOperand(0).copy();
        ValueRef operand2 = fSub.getOperand(1).copy();
        curInstruction = new FSub(lVal, fSub.getOp(), operand1, operand2);
    }

    @Override
    public void visit(Mul mul) {
        ValueRef lVal =mul.getLVal().copy();
        ValueRef operand1 = mul.getOperand(0).copy();
        ValueRef operand2 = mul.getOperand(1).copy();
        curInstruction = new Mul(lVal, mul.getOp(), operand1, operand2);
    }

    @Override
    public void visit(FMul fmul) {
        ValueRef lVal = fmul.getLVal().copy();
        ValueRef operand1 = fmul.getOperand(0).copy();
        ValueRef operand2 = fmul.getOperand(1).copy();
        curInstruction = new FMul(lVal, fmul.getOp(), operand1, operand2);
    }

    @Override
    public void visit(Mod mod) {
        ValueRef lVal = mod.getLVal().copy();
        ValueRef operand1 = mod.getOperand(0).copy();
        ValueRef operand2 = mod.getOperand(1).copy();
        curInstruction = new Mod(lVal, mod.getOp(), operand1, operand2);
    }

    @Override
    public void visit(Div div) {
        ValueRef lVal =div.getLVal().copy();
        ValueRef operand1 = div.getOperand(0).copy();
        ValueRef operand2 = div.getOperand(1).copy();
        curInstruction = new Div(lVal, div.getOp(), operand1, operand2);
    }

    @Override
    public void visit(FDiv fdiv) {
        ValueRef lVal = fdiv.getLVal().copy();
        ValueRef operand1 = fdiv.getOperand(0).copy();
        ValueRef operand2 = fdiv.getOperand(1).copy();
        curInstruction = new FDiv(lVal, fdiv.getOp(), operand1, operand2);
    }

    @Override
    public void visit(IntToFloat intToFloat) {
        ValueRef lVal = intToFloat.getLVal().copy();
        ValueRef operand1 = intToFloat.getOperand(0).copy();
        curInstruction = new IntToFloat(lVal, operand1);
    }

    @Override
    public void visit(FloatToInt floatToInt) {
        ValueRef lVal = floatToInt.getLVal().copy();
        ValueRef operand1 = floatToInt.getOperand(0).copy();
        curInstruction = new FloatToInt(lVal, operand1);
    }

    @Override
    public void visit(Br br) {
        curInstruction = new Br(br.getTarget());
    }

    @Override
    public void visit(CondBr condBr) {
        ValueRef cond = condBr.getOperand(0).copy();
        BasicBlockRef ifTrue = condBr.getTrueBlock();
        BasicBlockRef ifFalse = condBr.getFalseBlock();
        curInstruction = new CondBr(cond, ifTrue, ifFalse);
    }

    @Override
    public void visit(Cmp cmp) {
        ValueRef lVal = cmp.getLVal().copy();
        ValueRef operand1 =cmp.getOperand(0).copy();
        ValueRef operand2 =cmp.getOperand(1).copy();
        curInstruction = new Cmp(lVal, cmp.getOp(), cmp.getType(), operand1, operand2);
    }

    @Override
    public void visit(Logic logic)  {
        ValueRef lVal = logic.getLVal().copy();
        ValueRef operand1 = logic.getOperand(0).copy();
        ValueRef operand2=logic.getOperand(1).copy();
        curInstruction = new Logic(lVal, logic.getOp(), operand1, operand2);
    }

    @Override
    public void visit(ZExt zExt) {
        ValueRef lVal = zExt.getLVal().copy();
        ValueRef operand = zExt.getOperand(0).copy();
        curInstruction=new ZExt(lVal,operand,zExt.getTarget());
    }


    @Override
    public void visit(RetValue retValue) {
        ValueRef retVal = retValue.getOperand(0).copy();
        curInstruction = new RetValue(retVal);
    }

    @Override
    public void visit(RetVoid retVoid) {
        curInstruction = new RetVoid();
    }

    @Override
    public void visit(Ret ret) {
       curInstruction=new Ret();
    }

    @Override
    public void visit(BitCast bitCast) {
        ValueRef lVal = bitCast.getLVal().copy();
        ValueRef operand = bitCast.getOperand(0).copy();
        curInstruction = new BitCast(lVal, operand);
    }

    @Override
    public void visit(Call call) {
        if(call.getLVal()==null){
            FunctionValue functionValue=call.getFunction();
            List<ValueRef> params=new ArrayList<>();
            for (ValueRef valueRef:call.getRealParams()){
                params.add(valueRef.copy());
            }
            curInstruction=new Call(functionValue,params);
        }else {
            ValueRef lVal=call.getLVal().copy();
            FunctionValue functionValue=call.getFunction();
            List<ValueRef> params=new ArrayList<>();
            for (ValueRef valueRef:call.getRealParams()){
                params.add(valueRef.copy());
            }
            curInstruction=new Call(lVal,functionValue,params);
        }
    }

    @Override
    public void visit(Phi phi) {
        InstructionVisitor.super.visit(phi);
    }

    @Override
    public void visit(Ashr ashr) {
        ValueRef lVal = ashr.getLVal().copy();
        ValueRef operand1 =ashr.getOperand(0).copy();
        ValueRef operand2 =ashr.getOperand(1).copy();
        curInstruction = new Ashr(lVal, operand1, operand2);
    }

    @Override
    public void visit(Shl shl) {

    }

    @Override
    public void visit(Lshr Lshr) {
    }

    public void visit(Default defaultVal) {
            curInstruction=new Default();
    }
}
