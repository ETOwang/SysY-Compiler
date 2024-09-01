package cn.edu.nju.software.ir.generator;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.builder.BuilderRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.instruction.arithmetic.*;
import cn.edu.nju.software.ir.instruction.logic.*;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.type.*;
import cn.edu.nju.software.ir.value.*;

import java.util.ArrayList;

import static cn.edu.nju.software.ir.instruction.OpEnum.*;
import static cn.edu.nju.software.ir.type.ArrayType.UNKNOWN;

public class Generator implements IrGenerator {
    // singleton mode
    private static Generator gen = null;
    private Generator() {}

    public static Generator getInstance() {
        if (gen == null) {
            gen = new Generator();
        }
        return gen;
    }

    public final ConstValue zero = ConstInt(i32Type, 0);

    private TypeRef typeTransfer(TypeRef ty1, TypeRef ty2) {
        if (ty1 instanceof FloatType || ty2 instanceof FloatType) {
            return new FloatType();
        }
        return new IntType();
    }

    //这里改成各个指令对应一条
//    private LocalVar buildArithmeticIr(BuilderRef builder, OpEnum op, ValueRef operand1, ValueRef operand2, String lValName) {
//        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
//        Instruction ir = new Arithmetic(lVal, op, operand1, operand2);
//        builder.put(ir);
//        return lVal;
//    }

//    private LocalVar buildLogicalIr(BuilderRef builder, OpEnum op, ValueRef operand1, ValueRef operand2, String lValName) {
//        LocalVar lVal = builder.createLocalVar(new BoolType(), lValName);
//        Instruction ir = new Logic(lVal, op, operand1, operand2);
//        builder.put(ir);
//        return lVal;
//    }

//    @Override
    public GlobalVar addGlobal(ModuleRef module, TypeRef type, String name) {
        Pointer typePtr = new Pointer(type);
        GlobalVar globalVar = new GlobalVar(typePtr, name);
        module.addGlobalVar(globalVar);
        return globalVar;
    }
    @Override
    public GlobalVar setInitValue(GlobalVar globalVar, ValueRef initValue) {
        globalVar.initialize(initValue);
        return globalVar;
    }

    @Override
    public FunctionValue addFunction(ModuleRef moduleRef, FunctionType ft, String funcName, boolean lib) {
        FunctionValue fv = addFunction(moduleRef, ft, funcName);
        fv.setLib(lib);
        return fv;
    }

    @Override
    public FunctionValue addFunction(ModuleRef module, FunctionType ft, String funcName) {
        FunctionValue fv = new FunctionValue(ft, funcName);
        module.addFunction(fv);
        return fv;
    }

    @Override
    public LocalVar buildAllocate(BuilderRef builder, TypeRef type, String name) {
        Pointer typePtr = new Pointer(type);
        LocalVar localVar = builder.createLocalVar(typePtr, name);
        Instruction ir = new Allocate(localVar);
        builder.put(ir);
        return localVar;
    }
    @Override
    public ValueRef buildStore(BuilderRef builder, ValueRef value, ValueRef lVal) {
        if (!value.getType().equals(((Pointer)lVal.getType()).getBase())) {
            if (((Pointer)lVal.getType()).getBase().equals(i32Type)) {
                if (value instanceof ConstValue) {
                    value = gen.ConstInt(i32Type, ((ConstValue) value).castToInt());
                } else {
                    value = gen.buildFloatToInt(builder, value, "f2i_");
                }
            } else {
                if (value instanceof ConstValue) {
                    value = gen.ConstFloat(floatType, (float) (int)((ConstValue) value).getValue());
                } else {
                    value = gen.buildIntToFloat(builder, value, "i2f_");
                }
            }
        }
        Instruction ir = new Store(value, lVal);
        builder.put(ir);
        return lVal;
    }
    @Override
    public LocalVar buildLoad(BuilderRef builder, ValueRef memory, String lValName) {
        LocalVar lVal;
//        Pointer memoryPtr;
        if (!(((Pointer)memory.getType()).getBase() instanceof Pointer)){
            lVal = builder.createLocalVar(((Pointer)memory.getType()).getBase(), lValName);
        } else {
            ArrayType arrayType = new ArrayType(((Pointer) ((Pointer) memory.getType()).getBase()).getBase(), UNKNOWN);
            lVal = builder.createLocalVar(new Pointer(arrayType), lValName);
//            memoryPtr = (Pointer) memory.getType();
        }
//        memoryPtr = new Pointer(memory); // though memory maybe array, still translated to pointer positively
        Instruction ir = new Load(lVal, memory);
        builder.put(ir);
        return lVal;
    }
    @Override
    public ValueRef buildGEP(BuilderRef builder, ValueRef array,
            /** the indexes of visiting array
             *  arr[1][2]; {1, 2}
             */ ValueRef[] indices, int dims, String name) {
        // TODO maybe finished?
        ValueRef index;
        for (int i = 0; i < dims; i++) {
            Pointer arrayTyPtr = (Pointer) array.getType();
            ArrayType arrayTy = (ArrayType) arrayTyPtr.getBase();
            index = indices[i];
            LocalVar tmpLocal;
            if (i < dims - 1){
                tmpLocal = builder.createLocalVar(new Pointer(arrayTy.getElementType()), "ptr_");
            } else {
                tmpLocal = builder.createLocalVar(new Pointer(arrayTy.getElementType()), name);
            }
            tmpLocal.isArrPtr = true;
            ValueRef[] operands;
            if (arrayTy.getElementSize() != UNKNOWN) {
                operands = new ValueRef[]{array, zero, index};
            } else {
                operands = new ValueRef[]{array, index};
            }
            Instruction ir = new GEP(tmpLocal, arrayTyPtr, operands);
            builder.put(ir);
            if (i == dims - 1) {
                return tmpLocal; // last time tmpLocal is a pointer pointing to base type
            }
            array = tmpLocal;
        }
        return null;
    }
    @Override
    public LocalVar buildCmp(BuilderRef builder, int kind, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(new BoolType(), lValName);
        TypeRef type = typeTransfer(operand1.getType(), operand2.getType());
        if (type.equals(floatType)) {
            if (operand1.getType().equals(i32Type) && !(operand1 instanceof ConstValue)) {
                operand1 = buildIntToFloat(builder, operand1, "i2f_");
            }
            if (operand2.getType().equals(i32Type) && !(operand2 instanceof ConstValue)) {
                operand2 = buildIntToFloat(builder, operand2, "i2f_");
            }
        }
        Instruction ir = new Cmp(lVal, type.equals(floatType) ? FCMP : ICMP,
                type.equals(floatType) ? kind + 6 : kind, operand1, operand2);
        builder.put(ir);
        return lVal;
    }
    @Override
    public LocalVar buildXor(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(new BoolType(), lValName);
        Instruction ir = new Xor(lVal, XOR, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildLogicalIr(builder, XOR, operand1, operand2, lValName);
    }
    @Override
    public ValueRef buildAnd(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(new BoolType(), lValName);
        Instruction ir = new And(lVal, AND, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildLogicalIr(builder, AND, operand1, operand2, lValName);
    }
    @Override
    public ValueRef buildOr(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(new BoolType(), lValName);
        Instruction ir = new Or(lVal, OR, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildLogicalIr(builder, OR, operand1, operand2, lValName);
    }
    @Override
    public ValueRef buildZExtend(BuilderRef builder, ValueRef operand, TypeRef type, String lValName) {
        LocalVar lVal = builder.createLocalVar(type, lValName);
        if (operand instanceof ConstValue) {
            System.err.println("ZExtend target should be a variable.");
        }
        Instruction ir = new ZExt(lVal, operand, type);
        builder.put(ir);
        return lVal;
    }

    @Override
    public ValueRef buildBitCast(BuilderRef builder, ValueRef operand, String name) {
         TypeRef base = ((ArrayType)((Pointer)operand.getType()).getBase()).getBaseType();
         LocalVar lVal = builder.createLocalVar(new Pointer(base), name);
         Instruction ir = new BitCast(lVal, operand);
         builder.put(ir);
         return lVal;
    }
    @Override
    public ValueRef buildCall(BuilderRef builder, FunctionValue function, ArrayList<ValueRef> arguments
            , int argCount, String retValName, int lineNo) {
        FunctionType ft = ((FunctionType) function.getType());
        TypeRef retTy = ft.getReturnType();

        // implicit type conversion:
        for (int i = 0; i < arguments.size(); i++) {
            ValueRef argument = arguments.get(i);
            if (!ft.getFParameter(i).equals(argument.getType())) {
                if (ft.getFParameter(i) instanceof IntType && argument.getType() instanceof FloatType) {
                    argument= gen.buildFloatToInt(builder, argument, "f2i_");
                } else if (ft.getFParameter(i) instanceof FloatType && argument.getType() instanceof IntType) {
                    argument= gen.buildIntToFloat(builder, argument, "i2f_");
                } else {
                    throw new RuntimeException(String.format("Can't cast %s to %s!", argument.getType(), ft.getFParameter(i)));
                }
                arguments.set(i, argument);
            }
        }

        Instruction ir;
        LocalVar retVal = null;
        if (!(retTy instanceof VoidType)) {
            retVal = builder.createLocalVar(retTy, retValName);
            ir = new Call(retVal, function, arguments, lineNo);
        } else {
            ir = new Call(function, arguments, lineNo);
        }
        builder.put(ir);
        return retVal;
    }

    @Override
    public ValueRef buildReturnVoid(BuilderRef builder) {
        Instruction ir = new RetVoid();
        builder.put(ir);
        return null;
    }
    @Override
    public ValueRef buildReturn(BuilderRef builder, ValueRef retValue) {
        Instruction ir = new RetValue(retValue);
        builder.put(ir);
        return null;
    }

    //todo() here modify all related to arithmetic operations
    @Override
    public LocalVar buildAdd(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new Add(lVal, ADD, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, ADD, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildFAdd(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new FAdd(lVal, FADD, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, FADD, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildSub(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new Sub(lVal, SUB, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, SUB, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildFSub(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new FSub(lVal, FSUB, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, FSUB, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildMul(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new Mul(lVal, MUL, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, MUL, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildFMul(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new FMul(lVal, FMUL, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, FMUL, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildDiv(BuilderRef builder, ValueRef dividend, ValueRef divisor, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(dividend.getType(), divisor.getType()), lValName);
        Instruction ir = new Div(lVal, DIV, dividend, divisor);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, DIV, dividend, divisor, lValName);
    }

    @Override
    public LocalVar buildFDiv(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new FDiv(lVal, FDIV, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, FDIV, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildMod(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new Mod(lVal, MOD, operand1, operand2);
        builder.put(ir);
        return lVal;
//        return buildArithmeticIr(builder, MOD, operand1, operand2, lValName);
    }

    @Override
    public LocalVar buildAshr(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new Ashr(lVal, operand1, operand2);
        builder.put(ir);
        return lVal;
    }

    @Override
    public LocalVar buildShl(BuilderRef builder, ValueRef operand1, ValueRef operand2, String lValName) {
        LocalVar lVal = builder.createLocalVar(typeTransfer(operand1.getType(), operand2.getType()), lValName);
        Instruction ir = new Shl(lVal, operand1, operand2);
        builder.put(ir);
        return lVal;
    }


    @Override
    public ValueRef buildBranch(BuilderRef builder, BasicBlockRef targetBlock) {
        Instruction ir = new Br(targetBlock);
        builder.put(ir);
        builder.addPredForTargetBlock(targetBlock);
        return null;
    }
    @Override
    public ValueRef buildCondBranch(BuilderRef builder, ValueRef cond, BasicBlockRef ifTrue, BasicBlockRef ifFalse) {
        TypeRef condTy = cond.getType();
        if (!(condTy instanceof BoolType)) {
            System.err.println("Type of cond must be BoolType.");
        }
        Instruction ir = new CondBr(cond, ifTrue, ifFalse);
        builder.put(ir);
        builder.addPredForTargetBlock(ifTrue);
        builder.addPredForTargetBlock(ifFalse);
        return null;
    }
    @Override
    public ValueRef positionBuilderAtEnd(BuilderRef builder, BasicBlockRef block) {
        builder.positionAtEnd(block);
        return null;
    }

    @Override
    public ConstValue ConstInt(IntType type, int value) {
        return new ConstValue(type, value);
    }
    @Override
    public ConstValue ConstBool(BoolType type, boolean value) {
        return new ConstValue(type, value);
    }
    @Override
    public ConstValue ConstFloat(FloatType type, float value) {
        return new ConstValue(type, value);
    }

    @Override
    public ConstValue ConstInt(IntType type, int value, String name) {
        return new ConstValue(type, value, name);
    }
    @Override
    public ConstValue ConstBool(BoolType type, boolean value, String name) {
        return new ConstValue(type, value, name);
    }
    @Override
    public ConstValue ConstFloat(FloatType type, float value, String name) {
        return new ConstValue(type, value, name);
    }

    @Override
    public BasicBlockRef appendBasicBlock(FunctionValue function, String blockName) {
        BasicBlockRef block = new BasicBlockRef(function, blockName);
        function.appendBasicBlock(block);
        return block;
    }
    public BasicBlockRef appendEntryBasicBlock(FunctionValue function, String blockName) {
        BasicBlockRef block = new BasicBlockRef(function, blockName);
        function.appendEntryBasicBlock(block);
        return block;
    }
    @Override
    public ValueRef buildFloatToInt(BuilderRef builder, ValueRef floatVal, String name) {
        if (floatVal instanceof ConstValue) {
            return ConstInt(i32Type, ((ConstValue) floatVal).castToInt());
        }
        LocalVar localVar = builder.createLocalVar(i32Type, name);
        Instruction ir = new FloatToInt(localVar, floatVal);
        builder.put(ir);
        return localVar;
    }
    @Override
    public ValueRef buildIntToFloat(BuilderRef builder, ValueRef intVal, String name) {
        if (intVal instanceof ConstValue) {
            return ConstFloat(floatType, (float) (int)((ConstValue) intVal).getValue());
        }
        LocalVar localVar = builder.createLocalVar(floatType, name);
        Instruction ir = new IntToFloat(localVar, intVal);
        builder.put(ir);
        return localVar;
    }
    @Override
    public ValueRef dropBlock(BuilderRef builder, BasicBlockRef block) {
        builder.dropBlock(block);
        return block;
    }

    @Override
    public Phi buildEmptyPhiAfterInst(BasicBlockRef block, Allocate memory, String name) {
        LocalVar localVar = block.createLocalVar(((Pointer)memory.getLVal().getType()).getBase(), name);
        Phi emptyPhi = new Phi(localVar, block, memory);
        block.addPhi(emptyPhi);
        return emptyPhi;
    }
}
