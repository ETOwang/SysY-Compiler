package cn.edu.nju.software.backendrisc;

import cn.edu.nju.software.backendrisc.regalloc.RiscAllocator;
import cn.edu.nju.software.backendrisc.riscinstruction.*;
import cn.edu.nju.software.backendrisc.riscinstruction.floatextension.*;
import cn.edu.nju.software.backendrisc.riscinstruction.multiplyextension.RiscMul;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscImmediateValue;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscIndirectRegister;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscOperand;
import cn.edu.nju.software.backendrisc.riscinstruction.operand.RiscRegister;
import cn.edu.nju.software.backendrisc.riscinstruction.pseudo.*;
import cn.edu.nju.software.backendrisc.riscinstruction.util.RiscComment;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.generator.InstructionVisitor;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.instruction.arithmetic.*;
import cn.edu.nju.software.ir.instruction.logic.Ashr;
import cn.edu.nju.software.ir.instruction.logic.Logic;
import cn.edu.nju.software.ir.instruction.logic.Lshr;
import cn.edu.nju.software.ir.instruction.logic.Shl;
import cn.edu.nju.software.ir.type.*;
import cn.edu.nju.software.ir.value.ArrayValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.LinkedList;
import java.util.List;

public class RiscInstrGenerator implements InstructionVisitor {

    private final FunctionValue llvmFunctionValue;
    private final List<Instruction> instructions;
    private final List<RiscInstruction> riscInstructions = new LinkedList<>();
    private final RiscAllocator riscAllocator = RiscAllocator.get();

    RiscInstrGenerator(List<Instruction> instructions, FunctionValue llvmFunctionValue) {
        this.instructions = instructions;
        this.llvmFunctionValue = llvmFunctionValue;
    }

    public List<RiscInstruction> genRiscInstructions() {
        for(Instruction instruction : instructions){
            instruction.accept(this);
        }
        return riscInstructions;
    }

    private List<String> beforeABinaryInstr(Instruction instr){
        return riscAllocator.prepareOperands(instr.getOperand(0), instr.getOperand(1));
    }

    private List<String> beforeAUnaryInstr(Instruction instr){
        return riscAllocator.prepareOperands(instr.getOperand(0));
    }

    private void afterAnInstr(Instruction instr){
        riscAllocator.setLastLVal(instr.getLVal());
        LocalVar lVal = (LocalVar) instr.getLVal();
        if(lVal.isTmpVar()) {
            riscAllocator.recordTempVar(lVal);
        } else {
            saveLVal(instr.getLVal());
        }
    }

    private void saveLVal(ValueRef lVal){
        String regName = RiscSpecifications.isGeneralType(lVal.getType()) ? "t0" : "ft0";
        riscAllocator.storeLocalVarIntoMemory(lVal, regName);
    }


    /**
     * t0 t1 t2 的被修改，t0的值为ptr的值，t1的值为basePtr的值， t2的值的elment的length大小
     * @param gep
     */
    @Override
    public void visit(GEP gep) {
        ValueRef lVal = gep.getLVal();
        ValueRef basePtr = gep.getOperand(0);
        ValueRef index = gep.getNumberOfOperands() == 3 ? gep.getOperand(2) : gep.getOperand(1);
        riscInstructions.add(new RiscComment("gep " + lVal.getName() + " " +  index.getName()));
        List<String> regs = riscAllocator.prepareOperands(basePtr, index);
        int length = ArrayType.getTotalSize(((ArrayType) gep.getArrayTypePtr().getBase()).getElementType());
        riscInstructions.add(new RiscLi(new RiscRegister("t4"), new RiscImmediateValue(length)));
        riscInstructions.add(new RiscMul(new RiscRegister("t4"), new RiscRegister(regs.get(1)), new RiscRegister("t4")));
        riscInstructions.add(new RiscAdd(new RiscRegister("t0"), new RiscRegister("t4"), new RiscRegister(regs.get(0))));
        afterAnInstr(gep);
    }

    @Override
    public void visit(Store store) {
        ValueRef src = store.getOperand(0);
        ValueRef dest = store.getOperand(1);
        TypeRef destType = ((Pointer) dest.getType()).getBase();
        insertComment("store " + dest.getName() + " " + src.getName());
        if(destType instanceof IntType || destType instanceof FloatType || destType instanceof Pointer){
            storeIntoNotArray(dest, src);
        } else if(destType instanceof ArrayType){
            storeIntoArray(dest, src);
        } else {
            assert false;
        }
        riscAllocator.resetLastLVal();
    }

    private void storeIntoNotArray(ValueRef dest, ValueRef src){
        TypeRef destType = ((Pointer) dest.getType()).getBase();
        String srcReg = riscAllocator.prepareOperands(src).get(0);
        RiscOperand destRiscOperand = riscAllocator.getAddrOfVarPtrPointsToWithOffset(dest,0);
        if(destType instanceof IntType){
            riscInstructions.add(new RiscSw(new RiscRegister(srcReg), destRiscOperand));
        } else if(destType instanceof FloatType){
            riscInstructions.add(new RiscFsw(new RiscRegister(srcReg), destRiscOperand));
        } else if(destType instanceof Pointer){
            riscInstructions.add(new RiscSd(new RiscRegister(srcReg), destRiscOperand));
        } else {assert false;}
    }

    private void storeIntoArray(ValueRef dest, ValueRef src) {
        assert src instanceof ArrayValue;
        List<ValueRef> linerList = ((ArrayValue) src).getLinerList();
        for (int i = 0; i < linerList.size(); i++) {
            TypeRef type = linerList.get(i).getType();
            String srcReg = riscAllocator.prepareOperands(linerList.get(i)).get(0);
            if (type instanceof IntType) {
                riscInstructions.add(new RiscSw(new RiscRegister(srcReg), riscAllocator.getAddrOfVarPtrPointsToWithOffset(dest, i * RiscSpecifications.getIntSize())));
            } else if (type instanceof FloatType) {
                riscInstructions.add(new RiscFsw(new RiscRegister(srcReg), riscAllocator.getAddrOfVarPtrPointsToWithOffset(dest, i * RiscSpecifications.getFloatSize())));
            } else {assert false;}
        }
    }

    @Override
    public void visit(Allocate allocate) {
        riscInstructions.add(new RiscComment("allocate " + allocate.getLVal().getName()));
        riscAllocator.resetLastLVal();//所有通过allocate分配出来的指针都已经被记录了
    }

    @Override
    public void visit(Load load) {
        ValueRef src = load.getOperand(0);
        LocalVar lVal = (LocalVar) load.getLVal();
        insertComment("load " + lVal.getName() + " " + src.getName());
        RiscOperand srcRiscOperand = riscAllocator.getAddrOfVarPtrPointsToWithOffset(src, 0);
        TypeRef type = ((Pointer) src.getType()).getBase();
        if (type instanceof IntType) {
            riscInstructions.add(new RiscLw(new RiscRegister("t0"), srcRiscOperand));
        } else if (type instanceof FloatType) {
            riscInstructions.add(new RiscFlw(new RiscRegister("ft0"), srcRiscOperand));
        } else if (type instanceof Pointer){
            riscInstructions.add(new RiscLd(new RiscRegister("t0"), srcRiscOperand));
        } else {
            assert false;
        }
        afterAnInstr(load);
    }

    @Override
    public void visit(Add add) {
        insertComment("add " + add.getLVal().getName() + " " + add.getOperand(0).getName() + " " + add.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(add);
        riscInstructions.add(new RiscAddw(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(add);
    }

    @Override
    public void visit(FAdd fAdd) {
        insertComment("fadd " + fAdd.getLVal().getName() + " " + fAdd.getOperand(0).getName() + " " + fAdd.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fAdd);
        riscInstructions.add(new RiscFadds(new RiscRegister("ft0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(fAdd);
    }

    @Override
    public void visit(Sub sub) {
        insertComment("sub " + sub.getLVal().getName() + " " + sub.getOperand(0).getName() + " " + sub.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(sub);
        riscInstructions.add(new RiscSubw(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(sub);
    }

    @Override
    public void visit(FSub fSub) {
        insertComment("fsub " + fSub.getLVal().getName() + " " + fSub.getOperand(0).getName() + " " + fSub.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fSub);
        riscInstructions.add(new RiscFsubs(new RiscRegister("ft0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(fSub);
    }

    @Override
    public void visit(Mul mul) {
        insertComment("mul " + mul.getLVal().getName() + " " + mul.getOperand(0).getName() + " " + mul.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(mul);
        riscInstructions.add(new RiscMulw(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(mul);
    }

    @Override
    public void visit(FMul fmul) {
        insertComment("fmul " + fmul.getLVal().getName() + " " + fmul.getOperand(0).getName() + " " + fmul.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fmul);
        riscInstructions.add(new RiscFmuls(new RiscRegister("ft0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(fmul);
    }

    @Override
    public void visit(Mod mod) {
        insertComment("mod " + mod.getLVal().getName() + " " + mod.getOperand(0).getName() + " " + mod.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(mod);
        riscInstructions.add(new RiscRemw(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(mod);
    }

    @Override
    public void visit(Div div) {
        insertComment("div " + div.getLVal().getName() + " " + div.getOperand(0).getName() + " " + div.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(div);
        riscInstructions.add(new RiscDivw(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(div);
    }

    @Override
    public void visit(Ashr ashr) {
        insertComment("ashr " + ashr.getLVal().getName() + " " + ashr.getOperand(0).getName() + " " + ashr.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(ashr);
        riscInstructions.add(new RiscSra(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(ashr);
    }

    @Override
    public void visit(Shl shl){
        insertComment("shl " + shl.getLVal().getName() + " " + shl.getOperand(0).getName() + " " + shl.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(shl);
        riscInstructions.add(new RiscSll(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(shl);
    }

    @Override
    public void visit(Lshr lshr) {
        insertComment("lshr " + lshr.getLVal().getName() + " " + lshr.getOperand(0).getName() + " " + lshr.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(lshr);
        riscInstructions.add(new RiscSrl(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(lshr);
    }


    @Override
    public void visit(FDiv fdiv) {
        insertComment("fdiv " + fdiv.getLVal().getName() + " " + fdiv.getOperand(0).getName() + " " + fdiv.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fdiv);
        riscInstructions.add(new RiscFdivs(new RiscRegister("ft0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
        afterAnInstr(fdiv);
    }

    @Override
    public void visit(IntToFloat intToFloat) {
        insertComment("intToFloat " + intToFloat.getLVal().getName());
        List<String> regs = beforeAUnaryInstr(intToFloat);
        riscInstructions.add(new RiscFcvtsw(new RiscRegister("ft0"), new RiscRegister(regs.get(0))));
        afterAnInstr(intToFloat);
    }

    @Override
    public void visit(FloatToInt floatToInt) {
        insertComment("floatToInt " + floatToInt.getLVal().getName());
        List<String> regs = beforeAUnaryInstr(floatToInt);
        riscInstructions.add(new RiscFcvtws(new RiscRegister("t0"), new RiscRegister(regs.get(0))));
        afterAnInstr(floatToInt);
    }

    @Override
    public void visit(Br br) {
        insertComment("br " + br.getTarget().getName());
        /*
        j label
        ->
        la t1 label
        jr t1
         */
        riscInstructions.add(new RiscJ(br.getTarget().getName()));
        //todo() wait to reduce the allocate instr
//        riscInstructions.add(new RiscLa(new Register("t1"), new RiscLabelAddress(new RiscLabel(br.getTarget().getName()))));
//        riscInstructions.add(new RiscJr(new Register("t1")));
//        allocator.resetLastLVal();
    }

    @Override
    public void visit(CondBr condBr) {
        ValueRef cond = condBr.getOperand(0);
        BasicBlockRef ifTrue = condBr.getTrueBlock();
        BasicBlockRef ifFalse = condBr.getFalseBlock();
        insertComment("condBr " + cond.getName() + " " + ifTrue.getName() + " " + ifFalse.getName());
        List<String> regs = riscAllocator.prepareOperands(cond);
        /*
        beqz t1, ifFalse
        j ifTrue

        ->
            beqz t1, .temp_block1
            j temp_block2
        temp_block1:
            la t1 ifFalse
            jr t1;
        temp_block2:
            la t1 ifTrue
            jr t1;
         */
        riscInstructions.add(new RiscBeqz(new RiscRegister(regs.get(0)), ifFalse.getName()));
        riscInstructions.add(new RiscJ(ifTrue.getName()));
        //todo() wait to reduce the allocate instr
//        String tempBlock1 = RiscModule.createTempBlock();
//        String tempBlock2 = RiscModule.createTempBlock();
//        riscInstructions.add(new RiscBeqz(new Register(regs.get(0)), tempBlock1));
//        riscInstructions.add(new RiscJ(tempBlock2));
//        riscInstructions.add(new RiscLabel(tempBlock1));
//        riscInstructions.add(new RiscLa(new Register("t1"), new RiscLabelAddress(new RiscLabel(ifFalse.getName()))));
//        riscInstructions.add(new RiscJr(new Register("t1")));
//        riscInstructions.add(new RiscLabel(tempBlock2));
//        riscInstructions.add(new RiscLa(new Register("t1"), new RiscLabelAddress(new RiscLabel(ifTrue.getName()))));
//        riscInstructions.add(new RiscJr(new Register("t1")));
//        allocator.resetLastLVal();
    }

    @Override
    public void visit(Cmp cmp) {
        insertComment("cmp " + cmp.getLVal().getName() + " " + cmp.getOperand(0).getName() + " " + cmp.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(cmp);
        String cmpType = cmp.getType();
        switch (cmpType) {
            case "ne":
                riscInstructions.add(new RiscXor(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "eq":
                riscInstructions.add(new RiscXor(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "sgt":
                riscInstructions.add(new RiscSub(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSgtz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "slt":
                riscInstructions.add(new RiscSlt(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            case "sge":
                riscInstructions.add(new RiscSlt(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "sle":
                riscInstructions.add(new RiscSub(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSgtz(new RiscRegister("t0"), new RiscRegister("t0")));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "one":
                riscInstructions.add(new RiscFeqs(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "oeq":
                riscInstructions.add(new RiscFeqs(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            case "ogt":
                riscInstructions.add(new RiscFles(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "olt":
                riscInstructions.add(new RiscFlts(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            case "oge":
                riscInstructions.add(new RiscFlts(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                riscInstructions.add(new RiscSeqz(new RiscRegister("t0"), new RiscRegister("t0")));
                break;
            case "ole":
                riscInstructions.add(new RiscFles(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            default:
                assert false;
                break;
        }
        afterAnInstr(cmp);
    }

    @Override
    public void visit(Logic logic) {
        insertComment("logic " + logic.getLVal().getName() + " " + logic.getOperand(0).getName() + " " + logic.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(logic);
        OpEnum op = logic.getOp();
        switch (op) {
            case AND:
                riscInstructions.add(new RiscAnd(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            case OR:
                riscInstructions.add(new RiscOr(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            case XOR:
                riscInstructions.add(new RiscXor(new RiscRegister("t0"), new RiscRegister(regs.get(0)), new RiscRegister(regs.get(1))));
                break;
            default:
                assert false;
                break;
        }
        afterAnInstr(logic);
    }

    @Override
    public void visit(ZExt zExt) {
        insertComment("zext " + zExt.getLVal().getName());
        List<String> regs =beforeAUnaryInstr(zExt);
        riscInstructions.add(new RiscMv(new RiscRegister("t0"), new RiscRegister(regs.get(0))));
        afterAnInstr(zExt);
    }


    @Override
    public void visit(RetValue retValue) {
        ValueRef retVal = retValue.getOperand(0);
        insertComment("ret " + retVal.getName());
        List<String> regs = riscAllocator.prepareOperands(retVal);
        if(retVal.getType() instanceof IntType){
            riscInstructions.add(new RiscMv(new RiscRegister("a0"), new RiscRegister(regs.get(0))));
        } else if(retVal.getType() instanceof FloatType){
            riscInstructions.add(new RiscFmvxw(new RiscRegister("t0"), new RiscRegister(regs.get(0))));
            riscInstructions.add(new RiscFmvwx(new RiscRegister("fa0"), new RiscRegister("t0")));
        } else {assert false;}
        int stackSize = riscAllocator.getStackSize();
        if (stackSize > 0) {
            if(stackSize < 2048){
                riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(stackSize)));
            } else {
                riscInstructions.add(new RiscLi(new RiscRegister("t0"), new RiscImmediateValue(stackSize)));
                riscInstructions.add(new RiscAdd(new RiscRegister("sp"), new RiscRegister("sp"), new RiscRegister("t0")));
            }
        }
        if (!llvmFunctionValue.getName().equals("main")) {
            restoreCalleeSavedRegs();
        }
        riscInstructions.add(new RiscRet());
        riscAllocator.resetLastLVal();
    }


    @Override
    public void visit(RetVoid retVoid) {
        insertComment("ret void");
        int stackSize = riscAllocator.getStackSize();
        if (stackSize > 0) {
            if(stackSize < 2048){
                riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(stackSize)));
            } else {
                riscInstructions.add(new RiscLi(new RiscRegister("t0"), new RiscImmediateValue(stackSize)));
                riscInstructions.add(new RiscAdd(new RiscRegister("sp"), new RiscRegister("sp"), new RiscRegister("t0")));
            }
        }
        if (!llvmFunctionValue.getName().equals("main")) {
            restoreCalleeSavedRegs();
        }
        riscInstructions.add(new RiscRet());
        riscAllocator.resetLastLVal();
    }

    @Override
    public void visit(BitCast bitCast) {
        insertComment("bitcast " + bitCast.getLVal().getName() + " " + bitCast.getOperand(0).getName());
        beforeAUnaryInstr(bitCast);
        riscInstructions.add(new RiscMv(new RiscRegister("t0"), new RiscRegister("t1")));
        afterAnInstr(bitCast);
    }

    @Override
    public void visit(Call call) {
        riscAllocator.resetLastLVal();
        prepareParams(call);
        saveCallerSavedRegs();
        String funcName = call.getFunction().getName();
        if (funcName.equals("starttime") || funcName.equals("stoptime")) {
            funcName = "_sysy_" + funcName;
            riscInstructions.add(new RiscLi(new RiscRegister("a0"), new RiscImmediateValue(call.getLineNo())));
        }
        riscInstructions.add(new RiscComment("call " + funcName));
        riscInstructions.add(new RiscCall(funcName));
        restoreCallerSavedRegs();
        releaseParams(call);
        saveReturnValue(call);
    }


    private void saveReturnValue(Call call) {
        if (call.getLVal() != null) {
            TypeRef type = call.getLVal().getType();
            if (type instanceof IntType) {
                riscInstructions.add(new RiscSw(new RiscRegister("a0"), riscAllocator.getAddrOfLocalVar(call.getLVal())));
            } else if (type instanceof FloatType) {
                riscInstructions.add(new RiscFsw(new RiscRegister("fa0"), riscAllocator.getAddrOfLocalVar(call.getLVal())));
            } else {assert false;}
        }
    }

    private void prepareParams(Call call) {
        riscInstructions.add(new RiscComment("prepare params int regs"));
        String[] argRegs = RiscSpecifications.getArgRegs();
        String[] fArgRegs = RiscSpecifications.getFArgRegs();
        List<ValueRef> realParams = call.getRealParams();
        int ptr = 0;
        int fptr = 0;
        int order = 0;
        for (ValueRef realParam : realParams) {
            TypeRef type = realParam.getType();
            if (type instanceof FloatType) {
                if(fptr >= fArgRegs.length){
                    String srcReg = riscAllocator.prepareOperands(realParam).get(0);
                    pushIntoStack(srcReg, realParam, ++order);
                } else {
                    String srcReg = riscAllocator.prepareOperands(realParam).get(0);
                    riscInstructions.add(new RiscFmvxw(new RiscRegister("t0"), new RiscRegister(srcReg)));
                    riscInstructions.add(new RiscFmvwx(new RiscRegister(fArgRegs[fptr++]), new RiscRegister("t0")));
                }
            } else if (type instanceof IntType || type instanceof Pointer){
                if(ptr >= argRegs.length){
                    String srcReg = riscAllocator.prepareOperands(realParam).get(0);
                    pushIntoStack(srcReg ,realParam, ++order);
                } else {
                    String srcReg = riscAllocator.prepareOperands(realParam).get(0);
                    riscInstructions.add(new RiscMv(new RiscRegister(argRegs[ptr++]), new RiscRegister(srcReg)));
                }
            } else {assert false;}
        }
        if (order > 0) {
            if(order * 8 < 2048){
                riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(-8L * order)));
            } else {
                riscInstructions.add(new RiscLi(new RiscRegister("t0"), new RiscImmediateValue(-8L * order)));
                riscInstructions.add(new RiscAdd(new RiscRegister("sp"), new RiscRegister("sp"), new RiscRegister("t0")));
            }
        }
    }

    private void pushIntoStack(String srcReg,ValueRef realParam, int order){
        insertComment("push " + realParam.getName());
        if(realParam.getType() instanceof IntType){
            riscInstructions.add(new RiscSw(new RiscRegister(srcReg), riscAllocator.getRegWithOffset(-order * 8, "sp", "t4")));
        } else if(realParam.getType() instanceof FloatType){
            riscInstructions.add(new RiscFsw(new RiscRegister(srcReg), riscAllocator.getRegWithOffset(-order * 8, "sp", "t4")));
        } else if(realParam.getType() instanceof Pointer){
            riscInstructions.add(new RiscSd(new RiscRegister(srcReg), riscAllocator.getRegWithOffset(-order * 8, "sp", "t4")));
        } else {assert false;}
    }

    /**
     * 调用时候保存在栈里面用于传参的参数，弹栈释放
     * @param call
     */
    private void releaseParams(Call call){
        riscInstructions.add(new RiscComment("release params"));
        int intAndPointerParamNum = call.getRealParams().stream().filter(x -> x.getType() instanceof IntType || x.getType() instanceof Pointer).toArray().length;
        int floatParamNum = call.getRealParams().stream().filter(x -> x.getType() instanceof FloatType).toArray().length;
        int finalToRelease =Math.max(intAndPointerParamNum - RiscSpecifications.getArgRegs().length, 0) + Math.max(floatParamNum - RiscSpecifications.getFArgRegs().length, 0);
        if (finalToRelease > 0) {
            if(finalToRelease * 8 < 2048){
                riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(8L * finalToRelease)));
            } else {
                riscInstructions.add(new RiscLi(new RiscRegister("t0"), new RiscImmediateValue(8L * finalToRelease)));
                riscInstructions.add(new RiscAdd(new RiscRegister("sp"), new RiscRegister("sp"), new RiscRegister("t0")));
            }
        }
    }

    /**
     * 保存caller saved regs,但是只有记录其中保存着tempvar的值的才会被save
     */
    private void saveCallerSavedRegs() {
        riscInstructions.add(new RiscComment("save caller saved regs"));
        String[] callerSavedRegs = RiscSpecifications.getCallerSavedRegs();
        riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(-8L * callerSavedRegs.length)));
        for (int i = 0; i < callerSavedRegs.length; i++) {
            if(!riscAllocator.isUsedReg(callerSavedRegs[i]) && !callerSavedRegs[i].equals("ra")){ //ra不会被record但是仍然需要保存
                continue;
            }
            if(RiscSpecifications.isFloatReg(callerSavedRegs[i])){
                riscInstructions.add(new RiscFsd(new RiscRegister(callerSavedRegs[i]), new RiscIndirectRegister("sp", i * 8)));
            } else {
                riscInstructions.add(new RiscSd(new RiscRegister(callerSavedRegs[i]), new RiscIndirectRegister("sp", i * 8)));
            }
        }
    }

    private void restoreCallerSavedRegs() {
        riscInstructions.add(new RiscComment("restore caller saved regs"));
        String[] registers = RiscSpecifications.getCallerSavedRegs();
        for (int i = 0; i < registers.length; i++) {
            if(!riscAllocator.isUsedReg(registers[i]) && !registers[i].equals("ra")){
                continue;
            }
            if(RiscSpecifications.isFloatReg(registers[i])){
                riscInstructions.add(new RiscFld(new RiscRegister(registers[i]), new RiscIndirectRegister("sp", i * 8)));
            } else {
                riscInstructions.add(new RiscLd(new RiscRegister(registers[i]), new RiscIndirectRegister("sp", i * 8)));
            }
        }
        riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(8L * registers.length)));
    }

    private void restoreCalleeSavedRegs() {
        riscInstructions.add(new RiscComment("restore callee saved regs"));
        String[] calleeSavedRegs = RiscSpecifications.getCalleeSavedRegs();
        for (int i = 0; i < calleeSavedRegs.length; i++) {
            riscInstructions.add(new RiscLd(new RiscRegister(calleeSavedRegs[i]), new RiscIndirectRegister("sp", i * 8)));
        }
        riscInstructions.add(new RiscAddi(new RiscRegister("sp"), new RiscRegister("sp"), new RiscImmediateValue(8L * calleeSavedRegs.length)));
    }

    public void addInstruction(RiscInstruction instruction) {
        riscInstructions.add(instruction);
    }

    public void insertComment(String comment){
        riscInstructions.add(new RiscComment(comment));
    }
}
