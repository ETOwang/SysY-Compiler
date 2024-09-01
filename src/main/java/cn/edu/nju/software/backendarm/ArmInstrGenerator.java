package cn.edu.nju.software.backendarm;

import cn.edu.nju.software.backendarm.arminstruction.*;
import cn.edu.nju.software.backendarm.arminstruction.operand.*;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmComment;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmLabel;
import cn.edu.nju.software.backendarm.regalloc.ArmAllocator;
import cn.edu.nju.software.backendarm.regalloc.ArmRegisterManager;
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

public class ArmInstrGenerator implements InstructionVisitor {

    private final FunctionValue llvmFunctionValue;
    private final List<Instruction> instructions;
    private final List<ArmInstruction> armInstructions = new LinkedList<>();
    private final ArmAllocator armAllocator;
    private final ArmRegisterManager registerManager;
    ArmInstrGenerator(List<Instruction> instructions, FunctionValue llvmFunctionValue) {
        this.instructions = instructions;
        this.llvmFunctionValue = llvmFunctionValue;
        this.armAllocator=ArmAllocator.get(llvmFunctionValue);
        this.registerManager=ArmRegisterManager.get(llvmFunctionValue);

    }

    public List<ArmInstruction> genArmInstructions() {
        for(Instruction instruction : instructions){
            instruction.accept(this);
        }
        return armInstructions;
    }

    private List<String> beforeABinaryInstr(Instruction instr){
        return armAllocator.prepareOperands(instr.getOperand(0), instr.getOperand(1));
    }

    private List<String> beforeAUnaryInstr(Instruction instr){
        return armAllocator.prepareOperands(instr.getOperand(0));
    }

    private void afterAnInstr(Instruction instr){
        armAllocator.setLastLVal(instr.getLVal());
        LocalVar lVal = (LocalVar) instr.getLVal();
        if(lVal.isTmpVar()) {
            armAllocator.recordTempVar(lVal);
        } else {
            saveLVal(instr.getLVal());
        }
    }

    private void saveLVal(ValueRef lVal){
        String regName = ArmSpecifications.isGeneralType(lVal.getType()) ? "r0" : "s0";
        armAllocator.storeLocalVarIntoMemory(lVal, regName);
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
        armInstructions.add(new ArmComment("gep " + lVal.getName() + " " +  index.getName()));
        List<String> regs = armAllocator.prepareOperands(basePtr, index);
        int length = ArrayType.getTotalSize(((ArrayType) gep.getArrayTypePtr().getBase()).getElementType());
        armAllocator.loadImmediate("r4", length);
        armInstructions.add(new ArmMul(new ArmRegister("r4"), new ArmRegister(regs.get(1)), new ArmRegister("r4")));
        armInstructions.add(new ArmAdd(new ArmRegister("r0"), new ArmRegister("r4"), new ArmRegister(regs.get(0))));
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
        armAllocator.resetLastLVal();
    }

    private void storeIntoNotArray(ValueRef dest, ValueRef src){
        TypeRef destType = ((Pointer) dest.getType()).getBase();
        String srcReg = armAllocator.prepareOperands(src).get(0);
        if(registerManager.contains(dest)){
            if(srcReg.startsWith("s")){
                armInstructions.add(new ArmVmov_f32(new ArmRegister(registerManager.get(dest)),new ArmRegister(srcReg)));
            }else {
               armInstructions.add(new ArmVmov_f32_s32(new ArmRegister(registerManager.get(dest)),new ArmRegister(srcReg)));
            }
            return;
        }
        ArmOperand destArmOperand = armAllocator.getAddrOfVarPtrPointsToWithOffset(dest,0);
        if(destType instanceof IntType){
            armInstructions.add(new ArmStr(new ArmRegister(srcReg), destArmOperand));
        } else if(destType instanceof FloatType){
            armInstructions.add(new ArmVstr_f32(new ArmRegister(srcReg), destArmOperand));
        } else if(destType instanceof Pointer){
            armInstructions.add(new ArmStr(new ArmRegister(srcReg), destArmOperand));
        } else {assert false;}
    }

    private void storeIntoArray(ValueRef dest, ValueRef src) {
        assert src instanceof ArrayValue;
        List<ValueRef> linerList = ((ArrayValue) src).getLinerList();
        for (int i = 0; i < linerList.size(); i++) {
            TypeRef type = linerList.get(i).getType();
            String srcReg = armAllocator.prepareOperands(linerList.get(i)).get(0);
            if (type instanceof IntType) {
                armInstructions.add(new ArmStr(new ArmRegister(srcReg), armAllocator.getAddrOfVarPtrPointsToWithOffset(dest, i * ArmSpecifications.getIntSize())));
            } else if (type instanceof FloatType) {
                armInstructions.add(new ArmVstr_f32(new ArmRegister(srcReg), armAllocator.getAddrOfVarPtrPointsToWithOffset(dest, i * ArmSpecifications.getFloatSize())));
            } else {assert false;}
        }
    }

    @Override
    public void visit(Allocate allocate) {
        armInstructions.add(new ArmComment("allocate " + allocate.getLVal().getName()));
        armAllocator.resetLastLVal();//所有通过allocate分配出来的指针都已经被记录了
    }

    @Override
    public void visit(Load load) {
        ValueRef src = load.getOperand(0);
        TypeRef type = ((Pointer) src.getType()).getBase();
        if(registerManager.contains(src)){
            return;
        }
        LocalVar lVal = (LocalVar) load.getLVal();
        insertComment("load " + lVal.getName() + " " + src.getName());
        ArmOperand srcArmOperand = armAllocator.getAddrOfVarPtrPointsToWithOffset(src, 0);
        if (type instanceof IntType) {
            armInstructions.add(new ArmLdr(new ArmRegister("r0"), srcArmOperand));
        } else if (type instanceof FloatType) {
            armInstructions.add(new ArmVldr_f32(new ArmRegister("s0"), srcArmOperand));
        } else if (type instanceof Pointer){
            armInstructions.add(new ArmLdr(new ArmRegister("r0"), srcArmOperand));
        } else {
            assert false;
        }
        afterAnInstr(load);
    }

    @Override
    public void visit(Add add) {
        insertComment("add " + add.getLVal().getName() + " " + add.getOperand(0).getName() + " " + add.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(add);
        armInstructions.add(new ArmAdd(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(add);
    }

    @Override
    public void visit(FAdd fAdd) {
        insertComment("fadd " + fAdd.getLVal().getName() + " " + fAdd.getOperand(0).getName() + " " + fAdd.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fAdd);
        armInstructions.add(new ArmVadd_f32(new ArmRegister("s0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(fAdd);
    }

    @Override
    public void visit(Sub sub) {
        insertComment("sub " + sub.getLVal().getName() + " " + sub.getOperand(0).getName() + " " + sub.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(sub);
        armInstructions.add(new ArmSub(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(sub);
    }

    @Override
    public void visit(FSub fSub) {
        insertComment("fsub " + fSub.getLVal().getName() + " " + fSub.getOperand(0).getName() + " " + fSub.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fSub);
        armInstructions.add(new ArmVsub_f32(new ArmRegister("s0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(fSub);
    }

    @Override
    public void visit(Mul mul) {
        insertComment("mul " + mul.getLVal().getName() + " " + mul.getOperand(0).getName() + " " + mul.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(mul);
        armInstructions.add(new ArmMul(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(mul);
    }

    @Override
    public void visit(FMul fmul) {
        insertComment("fmul " + fmul.getLVal().getName() + " " + fmul.getOperand(0).getName() + " " + fmul.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fmul);
        armInstructions.add(new ArmVmul_f32(new ArmRegister("s0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(fmul);
    }

    @Override
    public void visit(Mod mod) {
        insertComment("mod " + mod.getLVal().getName() + " " + mod.getOperand(0).getName() + " " + mod.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(mod);
        armInstructions.add(new ArmSdiv(new ArmRegister("r3"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        armInstructions.add(new ArmMul(new ArmRegister("r3"), new ArmRegister("r3"), new ArmRegister(regs.get(1))));
        armInstructions.add(new ArmSub(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister("r3")));
        afterAnInstr(mod);
    }

    @Override
    public void visit(Div div) {
        insertComment("div " + div.getLVal().getName() + " " + div.getOperand(0).getName() + " " + div.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(div);
        armInstructions.add(new ArmSdiv(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(div);
    }

    @Override
    public void visit(Ashr ashr) {
        insertComment("ashr " + ashr.getLVal().getName() + " " + ashr.getOperand(0).getName() + " " + ashr.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(ashr);
        armInstructions.add(new ArmAsr(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(ashr);
    }

    @Override
    public void visit(Shl shl){
        insertComment("shl " + shl.getLVal().getName() + " " + shl.getOperand(0).getName() + " " + shl.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(shl);
        armInstructions.add(new ArmLsl(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(shl);
    }

    @Override
    public void visit(Lshr lshr) {
        insertComment("lshr " + lshr.getLVal().getName() + " " + lshr.getOperand(0).getName() + " " + lshr.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(lshr);
        armInstructions.add(new ArmLsr(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(lshr);
    }


    @Override
    public void visit(FDiv fdiv) {
        insertComment("fdiv " + fdiv.getLVal().getName() + " " + fdiv.getOperand(0).getName() + " " + fdiv.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(fdiv);
        armInstructions.add(new ArmVdiv_f32(new ArmRegister("s0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
        afterAnInstr(fdiv);
    }

    @Override
    public void visit(IntToFloat intToFloat) {
        //todo() arm do not have intToFloat instruction like a / b
        insertComment("intToFloat " + intToFloat.getLVal().getName());
        List<String> regs = beforeAUnaryInstr(intToFloat);
        armInstructions.add(new ArmVmov_f32_s32(new ArmRegister("s2"), new ArmRegister(regs.get(0))));
        armInstructions.add(new ArmVcvt_f32_s32(new ArmRegister("s0"), new ArmRegister("s2")));
        afterAnInstr(intToFloat);
    }

    @Override
    public void visit(FloatToInt floatToInt) {
        insertComment("floatToInt " + floatToInt.getLVal().getName());
        List<String> regs = beforeAUnaryInstr(floatToInt);
        armInstructions.add(new ArmVcvt_s32_f32(new ArmRegister("s1"), new ArmRegister(regs.get(0))));
        armInstructions.add(new ArmVmov_s32_f32(new ArmRegister("r0"), new ArmRegister("s1")));
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
        armInstructions.add(new ArmB(new ArmLabelAddress( new ArmLabel(br.getTarget().getName()))));
        //todo() wait to reduce the allocate instr
//        ArmInstructions.add(new ArmLa(new Register("t1"), new ArmLabelAddress(new ArmLabel(br.getTarget().getName()))));
//        ArmInstructions.add(new ArmJr(new Register("t1")));
//        allocator.resetLastLVal();
    }

    @Override
    public void visit(CondBr condBr) {
        ValueRef cond = condBr.getOperand(0);
        BasicBlockRef ifTrue = condBr.getTrueBlock();
        BasicBlockRef ifFalse = condBr.getFalseBlock();
        insertComment("condBr " + cond.getName() + " " + ifTrue.getName() + " " + ifFalse.getName());
        List<String> regs = armAllocator.prepareOperands(cond);
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
        armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmImmediateValue(0)));
        armInstructions.add(new ArmBeq(new ArmLabelAddress(new ArmLabel(ifFalse.getName()))));
        armInstructions.add(new ArmB(new ArmLabelAddress(new ArmLabel(ifTrue.getName()))));
        //todo() wait to reduce the allocate instr
//        String tempBlock1 = ArmModule.createTempBlock();
//        String tempBlock2 = ArmModule.createTempBlock();
//        ArmInstructions.add(new ArmBeqz(new Register(regs.get(0)), tempBlock1));
//        ArmInstructions.add(new ArmJ(tempBlock2));
//        ArmInstructions.add(new ArmLabel(tempBlock1));
//        ArmInstructions.add(new ArmLa(new Register("t1"), new ArmLabelAddress(new ArmLabel(ifFalse.getName()))));
//        ArmInstructions.add(new ArmJr(new Register("t1")));
//        ArmInstructions.add(new ArmLabel(tempBlock2));
//        ArmInstructions.add(new ArmLa(new Register("t1"), new ArmLabelAddress(new ArmLabel(ifTrue.getName()))));
//        ArmInstructions.add(new ArmJr(new Register("t1")));
//        allocator.resetLastLVal();
    }

    @Override
    public void visit(Cmp cmp) {
        insertComment("cmp " + cmp.getLVal().getName() + " " + cmp.getOperand(0).getName() + " " + cmp.getOperand(1).getName());
        List<String> regs = beforeABinaryInstr(cmp);
        String cmpType = cmp.getType();
        switch (cmpType) {
            case "ne":
                armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmMovne(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMoveq(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "eq":
                armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmMoveq(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovne(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "sgt":
                armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmMovgt(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovle(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "slt":
                armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmMovlt(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovge(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "sge":
                armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmMovge(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovlt(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "sle":
                armInstructions.add(new ArmCmp(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmMovle(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovgt(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "one":
                armInstructions.add(new ArmVcmp_f32(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmVmrs());
                armInstructions.add(new ArmMovne(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMoveq(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "oeq":
                armInstructions.add(new ArmVcmp_f32(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmVmrs());
                armInstructions.add(new ArmMoveq(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovne(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "ogt":
                armInstructions.add(new ArmVcmp_f32(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmVmrs());
                armInstructions.add(new ArmMovgt(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovle(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "olt":
                armInstructions.add(new ArmVcmp_f32(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmVmrs());
                armInstructions.add(new ArmMovlt(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovge(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "oge":
                armInstructions.add(new ArmVcmp_f32(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmVmrs());
                armInstructions.add(new ArmMovge(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovlt(new ArmRegister("r0"), new ArmImmediateValue(0)));
                break;
            case "ole":
                armInstructions.add(new ArmVcmp_f32(new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                armInstructions.add(new ArmVmrs());
                armInstructions.add(new ArmMovle(new ArmRegister("r0"), new ArmImmediateValue(1)));
                armInstructions.add(new ArmMovgt(new ArmRegister("r0"), new ArmImmediateValue(0)));
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
                armInstructions.add(new ArmAnd(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                break;
            case OR:
                armInstructions.add(new ArmOr(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
                break;
            case XOR:
                armInstructions.add(new ArmEor(new ArmRegister("r0"), new ArmRegister(regs.get(0)), new ArmRegister(regs.get(1))));
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
        armInstructions.add(new ArmMov(new ArmRegister("r0"), new ArmRegister(regs.get(0))));
        afterAnInstr(zExt);
    }


    @Override
    public void visit(RetValue retValue) {
        ValueRef retVal = retValue.getOperand(0);
        armAllocator.resetLastLVal();
        insertComment("ret " + retVal.getName());
        List<String> regs = armAllocator.prepareOperands(retVal);
        int stackSize = armAllocator.getStackSize();
        if (stackSize > 0) {
            if(stackSize < 256){
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(stackSize)));
            } else {
                armAllocator.loadImmediate("r0",stackSize);
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmRegister("r0")));
            }
        }
        if (!llvmFunctionValue.getName().equals("main")) {
            restoreCalleeSavedRegs();
        }
        if(retVal.getType() instanceof IntType){
            armInstructions.add(new ArmMov(new ArmRegister("r0"), new ArmRegister(regs.get(0))));
        } else if(retVal.getType() instanceof FloatType){
            armInstructions.add(new ArmVmov_f32(new ArmRegister("s0"), new ArmRegister(regs.get(0))));
        } else {assert false;}
        armInstructions.add(new ArmBx(new ArmRegister("lr")));
        armAllocator.resetLastLVal();
    }


    @Override
    public void visit(RetVoid retVoid) {
        insertComment("ret void");
        int stackSize = armAllocator.getStackSize();
        if (stackSize > 0) {
            if(stackSize < 256){
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(stackSize)));
            } else {
                armAllocator.loadImmediate("r0",stackSize);
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmRegister("r0")));
            }
        }
        if (!llvmFunctionValue.getName().equals("main")) {
            restoreCalleeSavedRegs();
        }
        armInstructions.add(new ArmBx(new ArmRegister("lr")));
        armAllocator.resetLastLVal();
    }

    @Override
    public void visit(BitCast bitCast) {
        insertComment("bitcast " + bitCast.getLVal().getName() + " " + bitCast.getOperand(0).getName());
        beforeAUnaryInstr(bitCast);
        armInstructions.add(new ArmMov(new ArmRegister("r0"), new ArmRegister("r1")));
        afterAnInstr(bitCast);
    }

    @Override
    public void visit(Call call) {
        armAllocator.resetLastLVal();
        prepareParams(call);
        if(call.getFunction().isLib()||call.getFunction().getName().equals("memset")){
            saveLibRegs();
        }else {
            saveCallerSavedRegs();
        }
        String funcName = call.getFunction().getName();
        if (funcName.equals("starttime") || funcName.equals("stoptime")) {
            funcName = "_sysy_" + funcName;
            armAllocator.loadImmediate("r0", call.getLineNo());
        }
//        else if (funcName.equals("putfloat") ) {
//           armInstructions.add(new ArmVmov_s32_f32(new ArmRegister("r0"), new ArmRegister("s0")));
//        }
        armInstructions.add(new ArmComment("call " + funcName));
        armInstructions.add(new ArmBl(new ArmLabelAddress(new ArmLabel(funcName))));
        if(call.getFunction().isLib()||call.getFunction().getName().equals("memset")){
            restoreLibRegs();
        }else {
            restoreCallerSavedRegs();
        }

        releaseParams(call);
        saveReturnValue(call);
    }

    private void saveReturnValue(Call call) {
        if (call.getLVal() != null) {
            TypeRef type = call.getLVal().getType();
            if (type instanceof IntType) {
                armInstructions.add(new ArmStr(new ArmRegister("r0"), armAllocator.getAddrOfLocalVar(call.getLVal())));
            } else if (type instanceof FloatType) {
                armInstructions.add(new ArmVstr_f32(new ArmRegister("s0"), armAllocator.getAddrOfLocalVar(call.getLVal())));
            } else {assert false;}
        }
    }

    private void prepareParams(Call call) {
        armInstructions.add(new ArmComment("prepare params int regs"));
        String[] argRegs = ArmSpecifications.getArgRegs();
        String[] fArgRegs = ArmSpecifications.getFArgRegs();
        List<ValueRef> realParams = call.getRealParams();
        int ptr = 0;
        int fptr = 0;
        int order = 0;
        for (ValueRef realParam : realParams) {
            TypeRef type = realParam.getType();
            if (type instanceof FloatType) {
                if(fptr >= fArgRegs.length){
                    String srcReg = armAllocator.prepareParamOperands(5, realParam).get(0);
                    pushIntoStack(srcReg, realParam, ++order);
                } else {
                    String srcReg = armAllocator.prepareParamOperands(5, realParam).get(0);
                    armInstructions.add(new ArmVmov_f32(new ArmRegister(fArgRegs[fptr++]), new ArmRegister(srcReg)));
                }
            } else if (type instanceof IntType || type instanceof Pointer){
                if(ptr >= argRegs.length){
                    String srcReg = armAllocator.prepareParamOperands(5, realParam).get(0);
                    pushIntoStack(srcReg ,realParam, ++order);
                } else {
                    String srcReg = armAllocator.prepareParamOperands(5, realParam).get(0);
                    armInstructions.add(new ArmMov(new ArmRegister(argRegs[ptr++]), new ArmRegister(srcReg)));
                }
            } else {assert false;}
        }
        if (order > 0) {
            if(order * 8 < 256){
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(-8 * order)));
            } else {
                armAllocator.loadImmediate("r4", -8 * order);
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmRegister("r4")));
            }
        }
    }

    private void pushIntoStack(String srcReg,ValueRef realParam, int order){
        insertComment("push " + realParam.getName());
        if(realParam.getType() instanceof IntType){
            armInstructions.add(new ArmStr(new ArmRegister(srcReg), armAllocator.getRegWithOffset(-order * 8, "sp", "r4")));
        } else if(realParam.getType() instanceof FloatType){
            armInstructions.add(new ArmVstr_f32(new ArmRegister(srcReg), armAllocator.getRegWithOffset(-order * 8, "sp", "r4")));
        } else if(realParam.getType() instanceof Pointer){
            armInstructions.add(new ArmStr(new ArmRegister(srcReg), armAllocator.getRegWithOffset(-order * 8, "sp", "r4")));
        } else {assert false;}
    }

    /**
     * 调用时候保存在栈里面用于传参的参数，弹栈释放
     * @param call
     */
    private void releaseParams(Call call){
        armInstructions.add(new ArmComment("release params"));
        int intAndPointerParamNum = call.getRealParams().stream().filter(x -> x.getType() instanceof IntType || x.getType() instanceof Pointer).toArray().length;
        int floatParamNum = call.getRealParams().stream().filter(x -> x.getType() instanceof FloatType).toArray().length;
        int finalToRelease =Math.max(intAndPointerParamNum - ArmSpecifications.getArgRegs().length, 0) + Math.max(floatParamNum - ArmSpecifications.getFArgRegs().length, 0);
        if (finalToRelease > 0) {
            if(finalToRelease * 8 < 256){
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(8 * finalToRelease)));
            } else {
                armAllocator.loadImmediate("r2", 8 * finalToRelease);
                armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmRegister("r2")));
            }
        }
    }

    /**
     * 保存caller saved regs,但是只有记录其中保存着tempvar的值的才会被save
     */
    private void saveCallerSavedRegs() {
        armInstructions.add(new ArmComment("save caller saved regs"));
        String[] callerSavedRegs = ArmSpecifications.getCallerSavedRegs();
        armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(-8 * callerSavedRegs.length)));
        for (int i = 0; i < callerSavedRegs.length; i++) {
            if(!armAllocator.isUsedReg(callerSavedRegs[i]) &&
                    !callerSavedRegs[i].equals("lr")&&
                    !ArmSpecifications.isLocalReg(callerSavedRegs[i])){ //ra不会被record但是仍然需要保存
                continue;
            }
            if(ArmSpecifications.isLocalReg(callerSavedRegs[i])&&!registerManager.isUsed(callerSavedRegs[i])){
                 continue;
            }
            if(ArmSpecifications.isFloatReg(callerSavedRegs[i])){
                armInstructions.add(new ArmVstr_f32(new ArmRegister(callerSavedRegs[i]), new ArmIndirectRegister("sp", i * 8)));
            } else {
                armInstructions.add(new ArmStr(new ArmRegister(callerSavedRegs[i]), new ArmIndirectRegister("sp", i * 8)));
            }
        }
    }

    private void restoreCallerSavedRegs() {
        armInstructions.add(new ArmComment("restore caller saved regs"));
        String[] registers = ArmSpecifications.getCallerSavedRegs();
        for (int i = 0; i < registers.length; i++) {
            if(!armAllocator.isUsedReg(registers[i]) && !registers[i].equals("lr")&&!ArmSpecifications.isLocalReg(registers[i])){
                continue;
            }
            if(ArmSpecifications.isLocalReg(registers[i])&&!registerManager.isUsed(registers[i])){
                continue;
            }
            if(ArmSpecifications.isFloatReg(registers[i])){
                armInstructions.add(new ArmVldr_f32(new ArmRegister(registers[i]), new ArmIndirectRegister("sp", i * 8)));
            } else {
                armInstructions.add(new ArmLdr(new ArmRegister(registers[i]), new ArmIndirectRegister("sp", i * 8)));
            }
        }
        armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(8 * registers.length)));
    }

    private void restoreCalleeSavedRegs() {
        armInstructions.add(new ArmComment("restore callee saved regs"));
        String[] calleeSavedRegs = ArmSpecifications.getCalleeSavedRegs();
        for (int i = 0; i < calleeSavedRegs.length; i++) {
            if(!armAllocator.isUsedReg(calleeSavedRegs[i]) && !calleeSavedRegs[i].equals("lr")&&!ArmSpecifications.isLocalReg(calleeSavedRegs[i])){
                continue;
            }
            armInstructions.add(new ArmLdr(new ArmRegister(calleeSavedRegs[i]), new ArmIndirectRegister("sp", i * 8)));
        }
        armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(8 * calleeSavedRegs.length)));
    }

    public void addInstruction(ArmInstruction instruction) {
        armInstructions.add(instruction);
    }

    public void insertComment(String comment){
        armInstructions.add(new ArmComment(comment));
    }
    private void saveLibRegs(){
        String[] callerSavedRegs = ArmSpecifications.getCallerSavedRegs();
        armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(-8 * callerSavedRegs.length)));
        for (int i = 0; i < callerSavedRegs.length; i++) {
            if(!armAllocator.isUsedReg(callerSavedRegs[i]) &&
                    !callerSavedRegs[i].equals("lr")){ //ra不会被record但是仍然需要保存
                continue;
            }
            if(ArmSpecifications.isFloatReg(callerSavedRegs[i])){
                armInstructions.add(new ArmVstr_f32(new ArmRegister(callerSavedRegs[i]), new ArmIndirectRegister("sp", i * 8)));
            } else {
                armInstructions.add(new ArmStr(new ArmRegister(callerSavedRegs[i]), new ArmIndirectRegister("sp", i * 8)));
            }
        }
    }

    private void  restoreLibRegs(){
        String[] registers = ArmSpecifications.getCallerSavedRegs();
        for (int i = 0; i < registers.length; i++) {
            if(!armAllocator.isUsedReg(registers[i]) && !registers[i].equals("lr")){
                continue;
            }
            if(ArmSpecifications.isFloatReg(registers[i])){
                armInstructions.add(new ArmVldr_f32(new ArmRegister(registers[i]), new ArmIndirectRegister("sp", i * 8)));
            } else {
                armInstructions.add(new ArmLdr(new ArmRegister(registers[i]), new ArmIndirectRegister("sp", i * 8)));
            }
        }
        armInstructions.add(new ArmAdd(new ArmRegister("sp"), new ArmRegister("sp"), new ArmImmediateValue(8L * registers.length)));
    }
}
