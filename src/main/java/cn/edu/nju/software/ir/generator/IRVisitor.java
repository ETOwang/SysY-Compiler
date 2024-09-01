package cn.edu.nju.software.ir.generator;

import cn.edu.nju.software.frontend.llvm.LLVMStack;
import cn.edu.nju.software.frontend.parser.SysYParser;
import cn.edu.nju.software.frontend.parser.SysYParserBaseVisitor;
import cn.edu.nju.software.frontend.util.Symbol;
import cn.edu.nju.software.frontend.util.SymbolTable;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.builder.BuilderRef;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.type.*;
import cn.edu.nju.software.ir.value.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import static cn.edu.nju.software.ir.instruction.Operator.*;

public class IRVisitor extends SysYParserBaseVisitor<ValueRef> {

    public IRVisitor() {
        module = new ModuleRef("module");
    }

    private final Generator gen = Generator.getInstance();

    //创建module
    private final ModuleRef module;

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    private final BuilderRef builder = new BuilderRef();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    private final IntType i32Type = new IntType();
    private final BoolType i1Type = new BoolType();
    private final VoidType voidType = new VoidType();
    private final FloatType floatType = new FloatType();

    private FunctionValue memset;

    private final ValueRef zero = gen.ConstInt(i32Type, 0);
    private final ValueRef fZero = gen.ConstFloat(floatType, 0.0f);

    private final ValueRef one = gen.ConstInt(i32Type, 1);

    private BasicBlockRef True;

    private BasicBlockRef False;

    private FunctionValue currentFunction;
    // scope!
    private final LLVMStack scope = new LLVMStack();

    private final Stack<BasicBlockRef> loopStack = new Stack<>();

    private boolean functionDef;
    private final Stack<Boolean> mustHaveReturn = new Stack<>(){{push(true);}}; // figure out whether always returns in `if () else if () else`
    private SymbolTable<ValueRef> curScope;

    private boolean global() {
        return scope.size() == 1;
    }

    public ModuleRef getModule() {
        return module;
    }

    public void dumpModuleToConsole() {
        module.dumpToConsole();
    }
    public void dumpModuleToFile(String path) {
        module.dumpToFile(path);
    }
    private int string2Int(String number) {
        if (number.equals("0")) return 0;
        if (number.startsWith("0x") || number.startsWith("0X")) {
            return Integer.parseInt(number.substring(2), 16);
        } else if (number.startsWith("0")) {
            return Integer.parseInt(number.substring(1), 8);
        } else {
            return Integer.parseInt(number);
        }
    }
    private float string2Float(String number) {
        return Float.parseFloat(number);
    }
    private ValueRef normalizeCond(ValueRef cond) {
        TypeRef type = cond.getType();
        if (type instanceof FloatType) {
            return gen.buildCmp(builder, CmpNE, cond, fZero, "cond_normalize_");
        } else if (type instanceof IntType) {
            return gen.buildCmp(builder, CmpNE, cond, zero, "cond_normalize_");
        }
        return cond;
    }
    private SysYParser.CondContext deleteParen(SysYParser.CondContext ctx) {
        while (ctx.L_PAREN() != null) {
            ctx = ctx.cond(0);
        }
        return ctx;
    }
    private void initGlobal() {
        // runtime library
        // make sure curScope point to the global
        if (global()) {
            FunctionType ft = new FunctionType(i32Type, new ArrayList<>(), 0);
            curScope.put(new Symbol<>("getint", gen.addFunction(module, ft, "getint", true)));
            curScope.put(new Symbol<>("getch", gen.addFunction(module, ft, "getch", true)));

            ft = new FunctionType(floatType, new ArrayList<>(), 0);
            curScope.put(new Symbol<>("getfloat", gen.addFunction(module, ft, "getfloat", true)));

//            ft = new FunctionType(i32Type, new ArrayList<TypeRef>(){{}}, 1); // TODO array type

            ft = new FunctionType(voidType, new ArrayList<TypeRef>(){{add(i32Type);}}, 1);
            curScope.put(new Symbol<>("putint", gen.addFunction(module, ft, "putint", true)));
            curScope.put(new Symbol<>("putch", gen.addFunction(module, ft, "putch", true)));

            ft = new FunctionType(voidType, new ArrayList<TypeRef>(){{add(floatType);}}, 1);
            curScope.put(new Symbol<>("putfloat", gen.addFunction(module, ft, "putfloat", true)));

            ft = new FunctionType(voidType, new ArrayList<>(), 0);
            curScope.put(new Symbol<>("starttime", gen.addFunction(module, ft, "starttime", true)));
            curScope.put(new Symbol<>("stoptime", gen.addFunction(module, ft, "stoptime", true)));

            ft = new FunctionType(i32Type, new ArrayList<>(){{add(new Pointer(i32Type));}}, 1);
            curScope.put(new Symbol<>("getarray", gen.addFunction(module, ft, "getarray", true)));

            ft = new FunctionType(i32Type, new ArrayList<>(){{add(new Pointer(floatType));}}, 1);
            curScope.put(new Symbol<>("getfarray", gen.addFunction(module, ft, "getfarray", true)));

            ft = new FunctionType(voidType, new ArrayList<>(){{add(i32Type); add(new Pointer(i32Type));}}, 2);
            curScope.put(new Symbol<>("putarray", gen.addFunction(module, ft, "putarray", true)));

            ft = new FunctionType(voidType, new ArrayList<>(){{add(i32Type); add(new Pointer(floatType));}}, 2);
            curScope.put(new Symbol<>("putfarray", gen.addFunction(module, ft, "putfarray", true)));

            addMemSet();
        }
    }

    private void addMemSet() {
        ArrayList<TypeRef> argsTy = new ArrayList<>();
        argsTy.add(new Pointer(i32Type));
        argsTy.add(i32Type);
        argsTy.add(i32Type);
        FunctionType ft = new FunctionType(voidType, argsTy, 3);
        memset = new FunctionValue(ft, "memset");
    }

    private static boolean isZeroInit(String initStr) {
        return initStr.replaceAll("[{0,}]", "").isEmpty();
    }

    @Override
    public ValueRef visitProgram(SysYParser.ProgramContext ctx) {
        functionDef = false;
        scope.push(new SymbolTable<>());
        curScope = scope.peek();
        initGlobal();
        return visitChildren(ctx);
    }
    @Override
    public ValueRef visitBlock(SysYParser.BlockContext ctx) {
        ValueRef ret;
        if (!functionDef){
            scope.push(new SymbolTable<>());
            curScope = scope.peek();
            ret = visitChildren(ctx);
            scope.pop();
            curScope = scope.peek();
        } else {
            functionDef = false;
            ret = visitChildren(ctx);
        }
        return ret;
    }
    @Override
    public ValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        TypeRef retType;
        if (ctx.funcType().INT() != null) {
            retType = i32Type;
        } else if (ctx.funcType().FLOAT() != null) {
            retType = floatType;
        } else {
            retType = voidType;
        }
        ArrayList<TypeRef> argumentTypes;
        int args = 0;
        if (ctx.funcFParams() == null) {
            argumentTypes = new ArrayList<>(0);
        } else {
            args = ctx.funcFParams().funcFParam().size();
            argumentTypes = new ArrayList<>(args);
            for (int i = 0; i < args; i++) {
                TypeRef type;
                if (ctx.funcFParams().funcFParam(i).bType().INT() != null) {
                    type = i32Type;
                } else {
                    type = floatType;
                }
                if (ctx.funcFParams().funcFParam(i).L_BRACKT() != null && !ctx.funcFParams().funcFParam(i).L_BRACKT().isEmpty()){
                    int ptrDim = ctx.funcFParams().funcFParam(i).exp().size();
                    TypeRef baseType = type;
                    for (int j = ptrDim - 1; j >= 0; j--) {
                        ValueRef vr = visitExp(ctx.funcFParams().funcFParam(i).exp(j));
                        // if a variable as array's dim-size
                        if (!(vr instanceof ConstValue)) {
                            type = baseType;
                            break;
                        }
                        int size = ((ConstValue) vr).castToInt();
                        type = new ArrayType(type, size);
                    }
                    type = new Pointer(type);
                }
                argumentTypes.add(type);
            }
        }
        FunctionType ft = new FunctionType(retType, argumentTypes, args);
        // add function to module
        FunctionValue function = gen.addFunction(module, ft, ctx.funcName().getText());
        currentFunction = function;
        // add function to LLVM.GOT
        curScope.put(new Symbol<>(ctx.funcName().getText(), function));
        // add basic block to function
        BasicBlockRef block = gen.appendEntryBasicBlock(function, ctx.funcName().getText() + "Entry");
        gen.positionBuilderAtEnd(builder, block);
        functionDef = true;
        scope.push(new SymbolTable<>());
        curScope = scope.peek();
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < args; i++) {
                String fParam = ctx.funcFParams().funcFParam(i).IDENT().getText();
                LocalVar param = function.getParam(i);
                LocalVar pointer = gen.buildAllocate(builder, param.getType(), "lv");
                gen.buildStore(builder, param, pointer);
                curScope.put(new Symbol<>(fParam, pointer));
            }
        }

        // initialize
        mustHaveReturn.push(false);

        ValueRef ret = visitBlock(ctx.block());
        if (!mustHaveReturn.pop() && retType instanceof VoidType) {
//             void function and haven't return
            gen.buildReturnVoid(builder);
        }
        functionDef = false;
        scope.pop();
        curScope = scope.peek();

        /* eliminate dead blocks: */
        function.clearDeadBlocks();
        function.modifyBlocks();

        return ret;
    }
    @Override
    public ValueRef visitFuncUse(SysYParser.FuncUseContext ctx) {
        int lineNo = ctx.funcName().IDENT().getSymbol().getLine();
        String funcName = ctx.funcName().getText();
        FunctionValue callFunc = (FunctionValue) scope.find(funcName);
        TypeRef retType = ((FunctionType) callFunc.getType()).getReturnType();
        if (ctx.funcRParams() == null) {
            if (retType.equals(voidType)) {
                funcName = "";
            }
            return gen.buildCall(builder, callFunc, new ArrayList<>(), 0, funcName, lineNo);
        } else {
            int args = ctx.funcRParams().param().size();
            ValueRef[] argv = new ValueRef[args];
            for (int i = 0; i < args; i++) {
                argv[i] = visitParam(ctx.funcRParams().param(i));
//                if (argv[i].getType() instanceof ArrayType) {
//                    argv[i] = gen.buildGEP(builder, argv[i], new ValueRef[]{gen.ConstInt(i32Type, 0)}, 1, "");
//                }
            }
            if (retType.equals(voidType)) {
                funcName = "";
            }
            return gen.buildCall(builder, callFunc, new ArrayList<>(Arrays.asList(argv)), args, funcName, lineNo);
        }
    }
    @Override
    public ValueRef visitParam(SysYParser.ParamContext ctx) {
        return visitExp(ctx.exp());
    }
    @Override
    public ValueRef visitNumber(SysYParser.NumberContext ctx) {
        if (ctx.FLOAT_CONST() != null) {
            return gen.ConstFloat(floatType, string2Float(ctx.FLOAT_CONST().getText()));
        } else {
            return gen.ConstInt(i32Type, string2Int(ctx.INTEGER_CONST().getText()));
        }
    }
    @Override
    public ValueRef visitExp(SysYParser.ExpContext ctx) {
        if (ctx.L_PAREN() != null) {
            return visitExp(ctx.exp(0));
        } else if (ctx.number() != null) {
            // Integer const
            return visitNumber(ctx.number());
        } else if (ctx.unaryOp() != null) {
            // !, - , +
            String op = ctx.unaryOp().getText();
            if (op.equals("-") && ctx.exp(0).number() != null) {
                SysYParser.NumberContext numberCtx = ctx.exp(0).number();
                if (numberCtx.INTEGER_CONST() != null) {
                    return gen.ConstInt(i32Type, string2Int("-" + numberCtx.INTEGER_CONST().getText()));
                }
            }
            ValueRef val = visitExp(ctx.exp(0));
            if (val instanceof ConstValue) {
                Object value = ((ConstValue) val).getValue();
                if (op.equals("!")) {
                    if (value.equals(0) || value.equals(0.0f)) return one;
                    else return zero;
                } else if (op.equals("+")) {
                    return val;
                } else {
                    // '-'
                    return val.getType().equals(i32Type) ? gen.ConstInt(i32Type, -(Integer) value) :
                            gen.ConstFloat(floatType, -(Float) value);
                }
            } else {
                // not a constant
                if (op.equals("!")) {
                    if (val.getType().equals(i32Type)){
                        val = gen.buildCmp(builder, CmpNE, zero, val, "tmp_");
                    } else {
                        val = gen.buildCmp(builder, CmpNE, fZero, val, "tmp_");
                    }
                    val = gen.buildXor(builder, val, one, "tmp_");
                    val = gen.buildZExtend(builder, val, i32Type, "tmp_");
                    return val;
                } else if (op.equals("+")) {
                    return val;
                } else {
                    if (val.getType().equals(i32Type)){
                        return gen.buildSub(builder, zero, val, "tmp_"); // 0-val => -val
                    } else {
                        return gen.buildFSub(builder, fZero, val, "tmp_");
                    }
                }
            }
        } else if (ctx.lVal() != null) {
            ValueRef lVal = visitLVal(ctx.lVal());
            if (lVal instanceof ConstValue) {
                return lVal;
            }
            if(lVal instanceof GlobalVar&&global()){
                return ((GlobalVar) lVal).getInitVal();
            }
            if (!(lVal.getType() instanceof Pointer)) {
                System.err.println("variable should be a pointer.");
            }
            if (!(((Pointer)lVal.getType()).getBase() instanceof ArrayType)){
                return gen.buildLoad(builder, lVal, ctx.lVal().IDENT().getText());
            } else {
                return gen.buildGEP(builder, lVal, new ValueRef[]{gen.ConstInt(i32Type, 0)}, 1,
                        ctx.lVal().IDENT().getText());
            }
        } else if (ctx.funcUse() != null) {
            return visitFuncUse(ctx.funcUse());
        } else {
            // +-*/%
//            System.err.println(ctx.exp(0).getText());
//            System.err.println(ctx.exp(1).getText());
            ValueRef val1 = visitExp(ctx.exp(0)), val2 = visitExp(ctx.exp(1));
            boolean tmpFlag = ctx.parent instanceof SysYParser.ExpContext;
            LocalVar lv;
            if (val1.getType().equals(i32Type) && val2.getType().equals(i32Type)) {
                if (ctx.PLUS() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        int v1 = (Integer) ((ConstValue) val1).getValue(), v2 = (Integer) ((ConstValue) val2).getValue();
                        return gen.ConstInt(i32Type, v1 + v2);
                    } else {
                        lv = gen.buildAdd(builder, val1, val2, "result_");
                    }
                } else if (ctx.MINUS() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        int v1 = (Integer) ((ConstValue) val1).getValue(), v2 = (Integer) ((ConstValue) val2).getValue();
                        return gen.ConstInt(i32Type, v1 - v2);
                    } else {
                        lv = gen.buildSub(builder, val1, val2, "result_");
                    }
                } else if (ctx.MUL() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        int v1 = (Integer) ((ConstValue) val1).getValue(), v2 = (Integer) ((ConstValue) val2).getValue();
                        return gen.ConstInt(i32Type, v1 * v2);
                    } else {
                        lv = gen.buildMul(builder, val1, val2, "result_");
                    }
                } else if (ctx.DIV() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        int v1 = (Integer) ((ConstValue) val1).getValue(), v2 = (Integer) ((ConstValue) val2).getValue();
                        return gen.ConstInt(i32Type, v1 / v2);
                    } else {
                        lv = gen.buildDiv(builder, val1, val2, "result_");
                    }
                } else {
                    // mod
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        int v1 = (Integer) ((ConstValue) val1).getValue(), v2 = (Integer) ((ConstValue) val2).getValue();
                        return gen.ConstInt(i32Type, v1 % v2);
                    } else {
                        lv = gen.buildMod(builder, val1, val2, "result_");
                    }
                }
            } else {
                if (val1.getType().equals(i32Type)) {
                    if (val1 instanceof ConstValue) {
                        val1 = gen.ConstFloat(floatType, (float)(int) ((ConstValue) val1).getValue());
                    } else {
                        val1 = gen.buildIntToFloat(builder, val1, "i2f_");
                    }
                }
                if (val2.getType().equals(i32Type)) {
                    if (val2 instanceof ConstValue) {
                        val2 = gen.ConstFloat(floatType, (float)(int) ((ConstValue) val2).getValue());
                    } else {
                        val2 = gen.buildIntToFloat(builder, val2, "i2f_");
                    }
                }
                if (ctx.PLUS() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        float v1 = (Float) ((ConstValue) val1).getValue(), v2 = (Float) ((ConstValue) val2).getValue();
                        return gen.ConstFloat(floatType, v1 + v2);
                    } else {
                        lv = gen.buildFAdd(builder, val1, val2, "result_");
                    }
                } else if (ctx.MINUS() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        float v1 = (Float) ((ConstValue) val1).getValue(), v2 = (Float) ((ConstValue) val2).getValue();
                        return gen.ConstFloat(floatType, v1 - v2);
                    } else {
                        lv = gen.buildFSub(builder, val1, val2, "result_");
                    }
                } else if (ctx.MUL() != null) {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        float v1 = (Float) ((ConstValue) val1).getValue(), v2 = (Float) ((ConstValue) val2).getValue();
                        return gen.ConstFloat(floatType, v1 * v2);
                    } else {
                        lv = gen.buildFMul(builder, val1, val2, "result_");
                    }
                } else {
                    if (val1 instanceof ConstValue && val2 instanceof ConstValue) {
                        float v1 = (Float) ((ConstValue) val1).getValue(), v2 = (Float) ((ConstValue) val2).getValue();
                        return gen.ConstFloat(floatType, v1 / v2);
                    } else {
                        lv = gen.buildFDiv(builder, val1, val2, "result_");
                    }
                }
            }
            //todo() 这里后端强度消解会破坏这个temp和父节点的关系，所以这里不设置tmpFlag,已经有一个pass专门处理这个问题
            lv.setTmp(false);
            return lv;
        }
    }
    @Override
    public ValueRef visitLVal(SysYParser.LValContext ctx) {
        ValueRef lVal = scope.find(ctx.IDENT().getText()); // lVal is a pointer type in fact
        if (ctx.exp() == null || ctx.exp().isEmpty()) {
            return lVal;
        } else {
            int dim = ctx.exp().size();
            ValueRef[] indices = new ValueRef[dim];
            for (int i = 0; i < dim; i++) {
                indices[i] = visitExp(ctx.exp(i));
            }
            if (lVal instanceof  GlobalVar && ((GlobalVar) lVal).isConst()) {
                ArrayValue av = (ArrayValue) ((GlobalVar) lVal).getInitVal();
                ValueRef res = null;
                for (ValueRef vr : indices) {
                    if (!(vr instanceof ConstValue)) {
                        res = null;
                        break;
                    }
                    int index = ((ConstValue)vr).castToInt();
                    if (av.getValue(index) instanceof ArrayValue) {
                        av = (ArrayValue) av.getValue(index);
                    } else {
                        res = av.getValue(index);
                    }
                }
                if (res != null) return res;
            }
//            System.err.println(dim);
            if (((Pointer)lVal.getType()).getBase() instanceof Pointer) {
                lVal = gen.buildLoad(builder, lVal, "arr_");
            }
            return gen.buildGEP(builder, lVal, indices, dim, ctx.IDENT().getText());
        }
    }
    @Override
    public ValueRef visitStmt(SysYParser.StmtContext ctx) {
//        System.err.println(ctx.getText());
        if (ctx.RETURN() != null) {
            // return stmt
            mustHaveReturn.pop();
            mustHaveReturn.push(true);
            if (ctx.exp() != null) {
                ValueRef retVal = visitExp(ctx.exp());
                TypeRef retTy = ((FunctionType)currentFunction.getType()).getReturnType();
                if (retTy.equals(i32Type) && retVal.getType().equals(floatType)) {
                    retVal = gen.buildFloatToInt(builder, retVal, "retVal_");
                } else if (retTy.equals(floatType) && retVal.getType().equals(i32Type)) {
                    retVal = gen.buildIntToFloat(builder, retVal, "retVal_");
                }
                gen.buildReturn(builder, retVal);
                return retVal;
            }
            return gen.buildReturnVoid(builder); // return; (no exp)
        } else if (ctx.ASSIGN() != null) {
            ValueRef lVal = visitLVal(ctx.lVal());
            ValueRef exp = visitExp(ctx.exp());
            return gen.buildStore(builder, exp, lVal); // assign: lVal = exp;
        } else if (ctx.WHILE() != null) {
            // loop
            // declare possible basic blocks
            BasicBlockRef whileCond = gen.appendBasicBlock(currentFunction, "whileCond_");
            BasicBlockRef whileBody = gen.appendBasicBlock(currentFunction, "whileBody_");
            BasicBlockRef next = gen.appendBasicBlock(currentFunction, "next_");
            True = whileBody;
            False = next;
            // while's exit push in stack, for break
            loopStack.push(next);
            // a loop push in stack
            loopStack.push(whileCond);
            // from current basic block jump into while condition
            gen.buildBranch(builder, whileCond);
            // following irs append on whileCond block
            gen.positionBuilderAtEnd(builder, whileCond);
            SysYParser.CondContext tmp = deleteParen(ctx.cond());
            if (tmp.AND() == null && tmp.OR() == null) {
                boolean alwaysTrue = false;
                ValueRef cond = visitCond(tmp);
                if (cond instanceof ConstValue constValue) {
                    alwaysTrue = constValue.castToInt() != 0;
                }
                // according to the condition decide jumping to where
                cond = normalizeCond(cond);
                if (alwaysTrue) {
                    // e.g: while(1)
                    gen.buildBranch(builder, whileBody);
                    next.setReachable(false);
                } else {
                    gen.buildCondBranch(builder, cond, whileBody, next);
                }
            } else {
                visitCond(tmp);
            }
            // while body part appends on whileBody block
            gen.positionBuilderAtEnd(builder, whileBody);
            ValueRef ret = visitStmt(ctx.whileStmt().stmt());
            // while body ends, jmp whileCond
            gen.buildBranch(builder, whileCond);
            // while part finished, other irs append on next block
            gen.positionBuilderAtEnd(builder, next);
            // loop ends, stack pops
            loopStack.pop();
            loopStack.pop();
            return ret;
        } else if (ctx.CONTINUE() != null) {
            return gen.buildBranch(builder, loopStack.peek());
        } else if (ctx.BREAK() != null) {
            BasicBlockRef tmp = loopStack.pop();
            BasicBlockRef br = loopStack.peek();
            loopStack.push(tmp);
            br.setReachable(true);
            return gen.buildBranch(builder, br);
        } else if (ctx.IF() != null) {
            // create possible basic blocks
            BasicBlockRef ifTrue = gen.appendBasicBlock(currentFunction, "ifTrue_");
            BasicBlockRef ifFalse = null;
            True = ifTrue;
            if (ctx.ELSE() != null){
                ifFalse = gen.appendBasicBlock(currentFunction, "ifFalse_");
                False = ifFalse;
            }
            BasicBlockRef next = gen.appendBasicBlock(currentFunction, "next_");
            if (ctx.ELSE() == null) {
                False = next;
            }
            // visit cond to compute the condition
            SysYParser.CondContext tmp = deleteParen(ctx.cond());
            if (tmp.AND() == null && tmp.OR() == null) {
                ValueRef cond = visitCond(tmp);
                // jump into different block according to the condition
                cond = normalizeCond(cond);
                if (ctx.ELSE() != null) {
                    gen.buildCondBranch(builder, cond, ifTrue, ifFalse);
                } else {
                    gen.buildCondBranch(builder, cond, ifTrue, next);
                }
            } else {
                visitCond(tmp);
            }
            // the ifTrue block runs the ifStmt irs
            gen.positionBuilderAtEnd(builder, ifTrue);

            boolean ifHaveRet = false, elseHaveRet = false;
            mustHaveReturn.push(false);

            ValueRef ret = visitStmt(ctx.ifStmt().stmt());

            ifHaveRet = mustHaveReturn.pop();
            mustHaveReturn.pop();
            mustHaveReturn.push(ifHaveRet);
            if (!ifHaveRet) {
                gen.buildBranch(builder, next);
            }
            // discuss the "else", exist or not
            if (ctx.ELSE() != null) {
                gen.positionBuilderAtEnd(builder, ifFalse);
                mustHaveReturn.push(false);
                ret = visitStmt(ctx.elseStmt().stmt());
                elseHaveRet = mustHaveReturn.pop();
                if (!elseHaveRet){
                    gen.buildBranch(builder, next);
                }
            }

            // update
            mustHaveReturn.pop();
            mustHaveReturn.push(ifHaveRet && elseHaveRet);

            if (!ifHaveRet || !elseHaveRet) {
                gen.positionBuilderAtEnd(builder, next);
            } else {
                next.setReachable(false);
            }
            return ret;
        } else {
            return visitChildren(ctx);
        }
    }
    @Override
    public ValueRef visitCond(SysYParser.CondContext ctx) {
        if (ctx.L_PAREN() != null) {
            return visitCond(ctx.cond(0));
        } else if (ctx.exp() != null) {
            return visitExp(ctx.exp());
        } else {
            if (ctx.AND() == null && ctx.OR() == null) {
                ValueRef c1 = visitCond(ctx.cond(0)), c2 = visitCond(ctx.cond(1));
                if (c1.getType() instanceof BoolType) {
                    if (c1 instanceof ConstValue) {
                        c1 = ((ConstValue) c1).getValue().equals(true) ? one : zero;
                    } else {
                        c1 = gen.buildZExtend(builder, c1, i32Type, "cond_tmp_");
                    }
                }
                if (c2.getType() instanceof BoolType){
                    if (c2 instanceof ConstValue) {
                        c2 = ((ConstValue) c2).getValue().equals(true) ? one : zero;
                    } else {
                        c2 = gen.buildZExtend(builder, c2, i32Type, "cond_tmp_");
                    }
                }
                if (c1.getType().equals(floatType) || c2.getType().equals(floatType)) {
                    if (c1.getType().equals(i32Type) && c1 instanceof ConstValue) {
                        c1 = gen.ConstFloat(floatType, (float) (int) ((ConstValue) c1).getValue());
                    }
                    if (c2.getType().equals(i32Type) && c2 instanceof ConstValue) {
                        c2 = gen.ConstFloat(floatType, (float) (int) ((ConstValue) c2).getValue());
                    }
                }
                ValueRef tmp;
                if (ctx.EQ() != null) {
                    // TODO int and float? e.g. 1 == 1.0
                    if (c1 instanceof ConstValue && c2 instanceof ConstValue) {
                        return gen.ConstBool(i1Type, ((ConstValue) c1).getValue().equals(((ConstValue) c2).getValue()));
                    }
                    tmp = gen.buildCmp(builder, CmpEQ, c1, c2, "cond_eq_tmp_");
                } else if (ctx.NEQ() != null) {
                    // TODO
                    if (c1 instanceof ConstValue && c2 instanceof ConstValue) {
                        return gen.ConstBool(i1Type, !((ConstValue) c1).getValue().equals(((ConstValue) c2).getValue()));
                    }
                    tmp = gen.buildCmp(builder, CmpNE, c1, c2, "cond_neq_tmp_");
                } else if (ctx.GT() != null) {
                    // TODO
                    tmp = gen.buildCmp(builder, CmpSGT, c1, c2, "cond_gt_tmp_");
                } else if (ctx.LT() != null) {
                    // TODO
                    tmp = gen.buildCmp(builder, CmpSLT, c1, c2, "cond_lt_tmp_");
                } else if (ctx.GE() != null) {
                    // TODO
                    tmp = gen.buildCmp(builder, CmpSGE, c1, c2, "cond_ge_tmp_");
                } else {
                    // TODO
                    // LE() != null
                    tmp = gen.buildCmp(builder, CmpSLE, c1, c2, "cond_le_tmp_");
                }
                tmp = gen.buildZExtend(builder, tmp, i32Type, "cond_tmp_");
                return gen.buildCmp(builder, CmpNE, tmp, zero, "cond_");
            } else {
                // && || need translating to br, to implement circuit computing
                SysYParser.CondContext tmp = deleteParen(ctx.cond(0));
                BasicBlockRef secondCondBlock = gen.appendBasicBlock(currentFunction, "secondCond_");
                BasicBlockRef tmpT = True, tmpF = False;
                if (tmp.AND() != null || tmp.OR() != null) {
                    if (ctx.AND() != null) {
                        True = secondCondBlock;
                    } else {
                        False = secondCondBlock;
                    }
                }
                ValueRef firstCond = visitCond(tmp);
                True = tmpT;
                False = tmpF;
                if (tmp.AND() == null && tmp.OR() == null) {
                    firstCond = normalizeCond(firstCond);
                    if (ctx.AND() != null) {
                        gen.buildCondBranch(builder, firstCond, secondCondBlock, False);
                    } else {
                        gen.buildCondBranch(builder, firstCond, True, secondCondBlock);
                    }
                }
                gen.positionBuilderAtEnd(builder, secondCondBlock);
                // if curScope second, then true is true
                ValueRef secondCond = visitCond(deleteParen(ctx.cond(1)));
                secondCond = normalizeCond(secondCond);
                gen.buildCondBranch(builder, secondCond, True, False);
                return zero; // return value is useless
            }
        }
    }
    @Override
    public ValueRef visitVarDef(SysYParser.VarDefContext ctx) {
        SysYParser.VarDeclContext parent = (SysYParser.VarDeclContext) ctx.getParent();
        GlobalVar globalVar = null;
        LocalVar localVar = null;
        TypeRef type;
        int arrSz = 0;
        if (parent.bType().INT() != null){
            type = i32Type;
        } else {
            type = floatType;
        }
        if (ctx.constExp() == null || ctx.constExp().isEmpty()) {
            // base var
            if (global()) {
                globalVar = gen.addGlobal(module, type, "gv");
            } else {
                localVar = gen.buildAllocate(builder, type, "lv");
            }
        } else {
            // array
            arrayInit = new ArrayList<>();
            curDim = 0;
            int dim = ctx.constExp().size();
            ConstValue cv = (ConstValue) visitConstExp(ctx.constExp(dim - 1));
            int size = cv.castToInt();
            arrSz = size;
            ArrayType arrayType = new ArrayType(type, size);
            for (int i = dim - 2; i >= 0; i--) {
                cv = (ConstValue) visitConstExp(ctx.constExp(i));
                size = cv.castToInt();
                arrSz *= size;
                arrayType = new ArrayType(arrayType, size);
            }
            elementDim = new ArrayList<>();
            for (TypeRef tmp = arrayType; tmp instanceof ArrayType; tmp = ((ArrayType) tmp).getElementType()) {
                elementDim.add(((ArrayType) tmp).getElementSize());
            }
            if (global()) {
                globalVar = gen.addGlobal(module, arrayType, "gv");
            } else {
                localVar = gen.buildAllocate(builder, arrayType, "lv");
            }
        }
        // init
        if (ctx.initVal() != null) {
            // need to initialize
            if (global()) {
                // global variable
                if (globalVar != null && !(((Pointer)(globalVar.getType())).getBase() instanceof ArrayType)) {
                    // base type
                    ValueRef init = visitInitVal(ctx.initVal());
                    if (!type.equals(init.getType())) {
                        // global variable's init must be compiling constant, so can directly use gen.build...
                        if (type instanceof IntType) {
                            init = gen.buildFloatToInt(builder, init, "f2i");
                        } else {
                            init = gen.buildIntToFloat(builder, init, "i2f");
                        }
                    }
                    gen.setInitValue(globalVar, init);
                } else if (globalVar != null) {
                    // array
                    arrayInit = new ArrayList<>();

                    // 特判{}
                    if (ctx.initVal().getText().equals("{}")) {
                        // {}
                        gen.setInitValue(globalVar, zero);
                    } else {
                        // {...}
                        visitInitVal(ctx.initVal());
                        ArrayType arrayType = (ArrayType) (((Pointer) globalVar.getType()).getBase());

                        ptr = 0;
                        // 确保数组基类型正确
                        ArrayValue av = getArrayValue(arrayType, type);
                        gen.setInitValue(globalVar, av);
                    }
                }
            } else {
                // local variable
                if (localVar != null && !(((Pointer)(localVar.getType())).getBase() instanceof ArrayType)) {
                    // base type
                    ValueRef init = visitInitVal(ctx.initVal());
                    gen.buildStore(builder, init, localVar);
                } else if (localVar != null) {
                    // array
                    arrayInit = new ArrayList<>();
                    visitInitVal(ctx.initVal());
                    boolean f = ifNeedMemSet();
                    // special {}
                    if (isZeroInit(ctx.initVal().getText()) || f) {
                        ValueRef bitCastPtr = gen.buildBitCast(builder, localVar, "ptr");
                        ArrayList<ValueRef> args = new ArrayList<>();
                        args.add(bitCastPtr);
                        args.add(zero);
                        ConstValue totSz = gen.ConstInt(i32Type, arrSz * 4); // need size of Bytes here!
                        args.add(totSz);
                        gen.buildCall(builder, memset, args, 3, "ret",
                                ctx.IDENT().getSymbol().getLine());
                    }
                    if (!isZeroInit(ctx.initVal().getText())) {

                        /* initialize array for local variable
                         * consider use GEP to get store target %p
                         * store %init %p
                         */
                        int dims = elementDim.size();
                        ValueRef[] indices = new ValueRef[dims];
                        for (int i = 0; i < arrayInit.size(); i++) {
                            ValueRef storeVal = arrayInit.get(i);
                            if (f && storeVal.equals(zero)) {
                                continue;
                            }
                            int tmp = i;
                            for (int j = dims - 1; j >= 0; j--) {
                                indices[j] = gen.ConstInt(i32Type, tmp % elementDim.get(j));
                                tmp /= elementDim.get(j);
                            }
                            ValueRef ptr = gen.buildGEP(builder, localVar, indices, dims, "inp");
                            gen.buildStore(builder, storeVal, ptr);
                        }
                    }
                }
            }
        } else {
            if (global() && globalVar != null){
                globalVar.setUninitialized(true);
                gen.setInitValue(globalVar, zero);
            }
        }
        if (global()) {
            curScope.put(new Symbol<>(ctx.IDENT().getText(), globalVar));
        } else {
            curScope.put(new Symbol<>(ctx.IDENT().getText(), localVar));
        }
        return null;
    }

    private boolean ifNeedMemSet() {
        int sz = arrayInit.size();
        int notZeroSz = 0;
        for (ValueRef vr : arrayInit) {
            if (vr.equals(zero)) {
                continue;
            }
            notZeroSz++;
        }
        double take = (double) notZeroSz / sz;
        return take < 0.2;
    }

    private int ptr = 0;
    private ArrayValue getArrayValue(ArrayType arrayType, TypeRef baseType) {
        int size = arrayType.getElementSize();
        ArrayList<ValueRef> tmp = new ArrayList<>();
        if (!(arrayType.getElementType() instanceof ArrayType)) {
            for (int i = ptr; ptr < i + size; ptr++) {
                ValueRef t = arrayInit.get(ptr);
                // 确保数组基类型正确
                t.updateType(baseType);
                tmp.add(t);
            }
        } else {
            for (int i = 0; i < size; i++) {
                tmp.add(getArrayValue((ArrayType) arrayType.getElementType(), baseType));
            }
        }
        return new ArrayValue(arrayType, tmp);
    }

    private int curDim = 0;
    private ArrayList<ValueRef> arrayInit = new ArrayList<>();
    private ArrayList<Integer> elementDim = new ArrayList<>();
    @Override
    public ValueRef visitInitVal(SysYParser.InitValContext ctx) {
        if (ctx.exp() != null) {
            return visitExp(ctx.exp());
        } else {
            // array initial
            int bottom = curDim; // no less than current dim
            boolean begin = false;
            int curCnt = 0, curMaxCnt = 0;
            for (SysYParser.InitValContext initValCtx : ctx.initVal()) {
                if (initValCtx.exp() != null) {
                    if (!begin) {
                        begin = true;
                        // as long as no {}, the current dim is the lowest dim
                        curDim = elementDim.size() - 1;
                        curMaxCnt = elementDim.get(curDim);
                        curCnt = 0;
                    }
                    arrayInit.add(visitInitVal(initValCtx));
                } else {
                    int tmp = curDim;
                    curDim++;
                    visitInitVal(initValCtx);
                    curDim = tmp;
                }
                curCnt++;
                if (curCnt >= curMaxCnt) {
                    // a dim is full now, reset and proc
                    begin = false; // reset
                    curDim--;
                    if (curDim <= bottom) {
                        curDim = bottom;
                    }
                    curMaxCnt = elementDim.get(curDim); // same as 'curMaxCnt = ...' above
                }
            }

            int tot = 1;
            for (int i = bottom; i < elementDim.size(); i++) {
                tot *= elementDim.get(i);
            }
            if (arrayInit.size() % tot != 0 || ctx.initVal().isEmpty()) {
                for (int i = arrayInit.size() % tot; i < tot; i++) {
                    arrayInit.add(zero); // fill the non-init location
                }
            }
            return null;
        }
    }
    @Override
    public ValueRef visitConstDef(SysYParser.ConstDefContext ctx) {
        // almost same as varDef
        SysYParser.ConstDeclContext parent = (SysYParser.ConstDeclContext) ctx.getParent();
        TypeRef type;
        if (parent.bType().INT() != null){
            type = i32Type;
        } else {
            type = floatType;
        }
        GlobalVar globalVar = null;
        LocalVar localVar = null;
        if (ctx.constExp() == null || ctx.constExp().isEmpty()) {
            // base
            String varName = ctx.IDENT().getText();
            ConstValue cv = (ConstValue) visitConstInitVal(ctx.constInitVal());
            // implicit type conversion
            if (!cv.getType().equals(type)) {
                if (cv.getType() instanceof FloatType && type.equals(i32Type)) {
                    cv = new ConstValue(i32Type, cv.castToInt(), cv.getName());
                } else if (cv.getType() instanceof IntType && type.equals(floatType)) {
                    cv = new ConstValue(floatType, (float) (int)cv.getValue(), cv.getName());
                } else {
                    throw new RuntimeException(String.format("Can't cast %s to %s!", cv.getValue(), type));
                }
            }
            curScope.put(new Symbol<>(varName, cv));
        } else {
            // array
            curDim = 0;
            arrayInit = new ArrayList<>();
            int dim = ctx.constExp().size();
            ConstValue cv = (ConstValue) visitConstExp(ctx.constExp(dim - 1));
            int size = cv.castToInt();
            ArrayType arrayType = new ArrayType(type, size);
            for (int i = dim - 2; i >= 0; i--) {
                cv = (ConstValue) visitConstExp(ctx.constExp(i));
                size = cv.castToInt();
                arrayType = new ArrayType(arrayType, size);
            }
            elementDim = new ArrayList<>();
            for (TypeRef tmp = arrayType; tmp instanceof ArrayType; tmp = ((ArrayType) tmp).getElementType()) {
                elementDim.add(((ArrayType) tmp).getElementSize());
            }
            if (global()) {
                globalVar = gen.addGlobal(module, arrayType, "gv");
                globalVar.setConst(true);
            } else {
                localVar = gen.buildAllocate(builder, arrayType, "lv");
            }
//            if (!global()) {
//                localVar = gen.buildAllocate(builder, arrayType, "lv");
//            }
        }
        // initialize
        if (global()) {
            if (globalVar != null && ((Pointer)(globalVar.getType())).getBase() instanceof ArrayType) {
                visitConstInitVal(ctx.constInitVal());
                ArrayType arrayType = (ArrayType) (((Pointer) globalVar.getType()).getBase());

                ptr = 0;
                ArrayValue av = getArrayValue(arrayType, type);
                gen.setInitValue(globalVar, av);
                curScope.put(new Symbol<>(ctx.IDENT().getText(), globalVar));
            }
        } else {
            // local variable keeps old version
            // difficult point: index may be variable TODO
            if (localVar != null && ((Pointer)(localVar.getType())).getBase() instanceof ArrayType) {
                visitConstInitVal(ctx.constInitVal());

                int dims = elementDim.size();
                ValueRef[] indices = new ValueRef[dims];
                for (int i = 0; i < arrayInit.size(); i++) {
                    ValueRef storeVal = arrayInit.get(i);
                    int tmp = i;
                    for (int j = dims - 1; j >= 0; j--) {
                        indices[j] = gen.ConstInt(i32Type, tmp % elementDim.get(j));
                        tmp /= elementDim.get(j);
                    }
                    ValueRef ptr = gen.buildGEP(builder, localVar, indices, dims, "inp");
                    gen.buildStore(builder, storeVal, ptr);
                }
                curScope.put(new Symbol<>(ctx.IDENT().getText(), localVar));
            }
        }
        return null;
    }
    @Override
    public ValueRef visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        if (ctx.constExp() != null) {
            return visitConstExp(ctx.constExp());
        } else {
            // array initial
            int bottom = curDim; // no less than current dim
            boolean begin = false;
            int curCnt = 0, curMaxCnt = 0;
            for (SysYParser.ConstInitValContext constInitValCtx : ctx.constInitVal()) {
                if (constInitValCtx.constExp() != null) {
                    if (!begin) {
                        begin = true;
                        curMaxCnt = elementDim.get(curDim);
                        curCnt = 0;
                        // as long as no {}, the current dim is the lowest dim
                        curDim = elementDim.size() - 1;
                    }
                    arrayInit.add(visitConstInitVal(constInitValCtx));
                } else {
                    int tmp = curDim;
                    curDim++;
                    visitConstInitVal(constInitValCtx);
                    curDim = tmp;
                }
                curCnt++;
                if (curCnt >= curMaxCnt) {
                    // a dim is full now, reset and proc
                    begin = false; // reset
                    curDim--;
                    if (curDim <= bottom) {
                        curDim = bottom;
                    }
                    curMaxCnt = elementDim.get(curDim); // same as 'curMaxCnt = ...' above
                }
            }

            int tot = 1;
            for (int i = bottom; i < elementDim.size(); i++) {
                tot *= elementDim.get(i);
            }
            if (arrayInit.size() % tot != 0 || ctx.constInitVal().isEmpty()) {
                for (int i = arrayInit.size() % tot; i < tot; i++) {
                    arrayInit.add(zero); // fill the non-init location
                }
            }
            return null;
        }
    }
    @Override
    public ValueRef visitConstExp(SysYParser.ConstExpContext ctx) {
        return visitExp(ctx.exp());
    }
}