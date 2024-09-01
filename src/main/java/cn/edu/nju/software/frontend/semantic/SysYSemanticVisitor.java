package cn.edu.nju.software.frontend.semantic;

import cn.edu.nju.software.frontend.parser.SysYParser;
import cn.edu.nju.software.frontend.parser.SysYParserBaseVisitor;
import cn.edu.nju.software.frontend.type.*;
import cn.edu.nju.software.frontend.util.ErrorType;
import cn.edu.nju.software.frontend.util.JudgeList;
import cn.edu.nju.software.frontend.util.Symbol;
import cn.edu.nju.software.frontend.util.SymbolTable;

import java.util.ArrayList;
import java.util.Stack;

public class SysYSemanticVisitor extends SysYParserBaseVisitor<Type> {
    private static final Type DEFAULT_RETTYPE = new NullType();
    private final Stack<SymbolTable<Type>> scope = new Stack<>(); // current scope is always at the top
    private SymbolTable<Type> curScope = new SymbolTable<>(); // copy of current scope
    private boolean error = false;
    private ArrayList<Type> paramsFTpList;
    private Type retType = DEFAULT_RETTYPE;

    // tag whether the scope(block) is started by function, if true, the block won't start a new scope, only work once
    private boolean funcScope = false;

    private final JudgeList judgeList = new JudgeList();

    public boolean noSemanticError() {
        return !error;
    }

    private void initGlobal() {
        // implement runtime library
        IntType intType = new IntType();
        FloatType floatType = new FloatType();
        VoidType voidType = new VoidType();
        ArrayType intArray = new ArrayType(intType), floatArray = new ArrayType(floatType);
        FuncType funcType = new FuncType(intType, new ArrayList<>());

        updateCurScope(funcType, "getint");

        updateCurScope(funcType, "getch");

        funcType = new FuncType(floatType, new ArrayList<>());
        updateCurScope(funcType, "getfloat");

        funcType = new FuncType(intType, new ArrayList<Type>(){{add(intArray);}});
        updateCurScope(funcType, "getarray");

        funcType = new FuncType(intType, new ArrayList<Type>(){{add(floatArray);}});
        updateCurScope(funcType, "getfarray");

        funcType = new FuncType(voidType, new ArrayList<Type>(){{add(intType);}});
        updateCurScope(funcType, "putint");

        updateCurScope(funcType, "putch");

        funcType = new FuncType(voidType, new ArrayList<Type>(){{add(floatType);}});
        updateCurScope(funcType, "putfloat");

        funcType = new FuncType(voidType, new ArrayList<Type>(){{add(intType);add(intArray);}});
        updateCurScope(funcType, "putarray");

        funcType = new FuncType(voidType, new ArrayList<Type>(){{add(intType);add(floatArray);}});
        updateCurScope(funcType, "putfarray");

//        funcType = new FuncType(voidType, new ArrayList<>()); // TODO putf(<format str>, ...) ?

        funcType = new FuncType(voidType, new ArrayList<>());
        updateCurScope(funcType, "starttime");

        updateCurScope(funcType, "stoptime");
    }
    private void updateCurScope() {
        // update current scope to the top one
        if (!scope.empty())
            curScope = scope.peek();
    }
    private void updateCurScope(Type type, String text) {
        // put a new var into current scope
        if (!scope.empty()){
            updateCurScope();
            curScope.put(new Symbol<>(text, type));
        } else {
            // why empty ????
            scope.push(new SymbolTable<>());
            updateCurScope(type, text);
        }
    }
    private void printError(int type, int lineNo, String msg) {
        error = true;
        if (judgeList.find(lineNo) == null) {
            System.err.println("Error type " + type + " at Line " + lineNo + ": " + msg);
            judgeList.put(lineNo, type);
        } else if (judgeList.find(lineNo).type == type) {
            System.err.println("Error type " + type + " at Line " + lineNo + ": " + msg);
        }
    }
    private Type findIdentByCheckGOT(String ident) {
        Type type = null;
        Stack<SymbolTable<Type>> tmp = new Stack<>();
        while (!scope.empty()) {
            curScope = scope.pop();// top scope is cur scope
            tmp.push(curScope);
            type = curScope.find(ident);
            if (type != null) {
                break;
            }
        }
        while (!tmp.empty()) {
            scope.push(tmp.pop());
        }
        updateCurScope();
        return type;
    }
    @Override
    public Type visitProgram(SysYParser.ProgramContext ctx) {
        SymbolTable<Type> global = new SymbolTable<>();
        scope.push(global);
        updateCurScope();
        initGlobal();
        visitChildren(ctx);
        if (!scope.empty()){
            scope.pop();
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitBlock(SysYParser.BlockContext ctx) {
        if (!funcScope) {
            scope.push(new SymbolTable<>());
        } else {
            funcScope = false;
        }
        updateCurScope();
        Type tmp = visitChildren(ctx);
        // a block ends, its scope dies
        if (!scope.empty()){
            scope.pop();
        }
        updateCurScope();
        return tmp;
    }
    @Override
    public Type visitVarDecl(SysYParser.VarDeclContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        for (int i = 0; i < ctx.varDef().size(); i++) {
            visitVarDef(ctx.varDef(i));
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitConstDecl(SysYParser.ConstDeclContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        for (int i = 0; i < ctx.constDef().size(); i++) {
            visitConstDef(ctx.constDef(i));
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitVarDef(SysYParser.VarDefContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        String varName = ctx.IDENT().getText();
        if (curScope.find(varName) != null) {
            printError(ErrorType.VAR_RE_DEF, ctx.getStart().getLine(), "redefined variable.");
            return DEFAULT_RETTYPE;
        }
        Type identTy;
        if (ctx.constExp() == null || ctx.constExp().isEmpty()) {
            //not array
            SysYParser.VarDeclContext parent = (SysYParser.VarDeclContext) ctx.getParent(); // VarDef的父节点只能是VarDecl
            if (parent.bType().INT() != null) {
                identTy = new IntType();
            } else {
                identTy = new FloatType();
            }
            updateCurScope(identTy, varName);
        } else {
            //array
            int size = ctx.constExp().size();
            identTy = new ArrayType(new IntType());
            for (int level = size - 2; level >= 0; level--) {
                identTy = new ArrayType(identTy);
            }
            updateCurScope(identTy, varName);
        }
        if (ctx.ASSIGN() != null) { // assign stmt
            Type initTy = visitInitVal(ctx.initVal());
            if (initTy == null || (identTy instanceof ArrayType && !(initTy instanceof ArrayType)) ||
                    (identTy instanceof NumberType && !(initTy instanceof NumberType))) {
                printError(ErrorType.ASSIGN_NOT_MATCH, ctx.getStart().getLine(), "assign not matched.");
            }
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitInitVal(SysYParser.InitValContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        if (ctx.exp() != null) {
            return visitExp(ctx.exp());
        }else {
            // todo 初始化数组的维数尚未计算
            for (SysYParser.InitValContext c : ctx.initVal()) {
                visitInitVal(c);
            }
            return new ArrayType(null);
        }
    }
    @Override
    public Type visitConstDef(SysYParser.ConstDefContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        String varName = ctx.IDENT().getText();
        if (curScope.find(varName) != null) {
            printError(ErrorType.VAR_RE_DEF, ctx.getStart().getLine(), "variable redefined.");
            return DEFAULT_RETTYPE;
        }
        Type identTy;
        if (!ctx.constExp().isEmpty()) {
            // array
            int size = ctx.constExp().size(); // at least 1
            identTy = new ArrayType(new IntType(true), true);
            for (int level = size - 2; level >= 0; level--) {
                identTy = new ArrayType(identTy, true);
            }
            updateCurScope(identTy, varName);
        } else {
            // number: int or float
            SysYParser.ConstDeclContext parent = (SysYParser.ConstDeclContext) ctx.getParent(); // 父节点只可能是ConstDecl
            if (parent.bType().INT() != null) {
                identTy = new IntType(true);
            } else {
                identTy = new FloatType(true);
            }
            updateCurScope(identTy, varName);
        }
        if (ctx.ASSIGN() != null) {
            // assign stmt
            Type initTy = visitConstInitVal(ctx.constInitVal());
            if (initTy == null || (identTy instanceof ArrayType && !(initTy instanceof ArrayType)) ||
                    (identTy instanceof NumberType && !(initTy instanceof NumberType))) {
                printError(ErrorType.ASSIGN_NOT_MATCH, ctx.getStart().getLine(), "assign not matched.");
            }
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        if (ctx.constExp() != null) {
            return visitConstExp(ctx.constExp());
        } else {
            // todo 初始化数组的维数尚未计算
            for (SysYParser.ConstInitValContext c : ctx.constInitVal()) {
                visitConstInitVal(c);
            }
            return new ArrayType(null);
        }
    }
    @Override
    public Type visitConstExp(SysYParser.ConstExpContext ctx) {
        return ctx == null ? DEFAULT_RETTYPE : visitExp(ctx.exp());
    }
    @Override
    public Type visitFuncDef(SysYParser.FuncDefContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        String funcName = ctx.funcName().getText();
        if (curScope.find(funcName) != null) {
            printError(ErrorType.FUNC_RE_DEF, ctx.getStart().getLine(), "redefined function name.");
            return DEFAULT_RETTYPE; // jump the function
        }
        Type retType = new VoidType();
        if (ctx.funcType().INT() != null) {
            retType = new IntType();
        } else if (ctx.funcType().FLOAT() != null) {
            retType = new FloatType();
        }
        this.retType = retType;
        FuncType funcType = new FuncType();
        updateCurScope(funcType, funcName);
        // start a new scope
        funcScope = true;
        scope.push(new SymbolTable<>());
        updateCurScope();

        paramsFTpList = new ArrayList<>();
        if (ctx.funcFParams() != null) {
            visitFuncFParams(ctx.funcFParams());
        }

        funcType.update(retType, paramsFTpList);
        visitBlock(ctx.block());
        return retType;
    }

    @Override
    public Type visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        if (ctx == null || ctx.funcFParam() == null)
            return DEFAULT_RETTYPE;
        for (int i = 0; i < ctx.funcFParam().size(); i++) {
            visitFuncFParam(ctx.funcFParam(i));
        }
        return DEFAULT_RETTYPE;
    }

    @Override
    public Type visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        String name = ctx.IDENT().getText();
        if (curScope.find(name) != null) {
            printError(ErrorType.VAR_RE_DEF, ctx.getStart().getLine(), "variable redefined.");
            return DEFAULT_RETTYPE;
        }
        if (ctx.L_BRACKT() != null && !ctx.L_BRACKT().isEmpty()) {
            // array
            int size = ctx.L_BRACKT().size(); // at least 1(and only)
            ArrayType arrayType = new ArrayType(new IntType());
            // bug fix: foo(int a[][5])
            for (int level = size - 1; level >= 1; level--) {
                arrayType = new ArrayType(arrayType);
            }
            paramsFTpList.add(arrayType);
            updateCurScope(arrayType, ctx.IDENT().getText());
        } else {
            // number type
            NumberType paramTy;
            if (ctx.bType().INT() != null) {
                paramTy = new IntType();
            } else {
                paramTy = new FloatType();
            }
            paramsFTpList.add(paramTy);
            updateCurScope(paramTy, ctx.IDENT().getText());
        }
        return DEFAULT_RETTYPE;
    }
    // above almost declare, below call
    @Override
    public Type visitCond(SysYParser.CondContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        if (ctx.L_PAREN() != null) {
            return visitCond(ctx.cond(0));
        }
        if (ctx.exp() != null) {
            Type expTy = visitExp(ctx.exp());
            if (expTy != null && !(expTy instanceof NumberType) && !(expTy instanceof NullType)) {
                printError(ErrorType.OP_NOT_MATCH, ctx.getStart().getLine(), "condExp should be int value.");
            }
        } else if (ctx.LE() != null || ctx.LT() != null || ctx.GT() != null || ctx.GE() != null ||
                ctx.OR() != null || ctx.AND() != null || ctx.EQ() != null || ctx.NEQ() != null){
            visitCond(ctx.cond(0));
            visitCond(ctx.cond(1));
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitNumber(SysYParser.NumberContext ctx) {
        if (ctx.FLOAT_CONST() != null) {
            return new FloatType();
        } else {
            return new IntType();
        }
    }
    @Override
    public Type visitExp(SysYParser.ExpContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        if (ctx.L_PAREN() != null) {
            return visitExp(ctx.exp(0));
        } else if (ctx.lVal() != null) {
            return visitLVal(ctx.lVal());
        } else if (ctx.number() != null) {
            return visitNumber(ctx.number());
        } else if (ctx.funcUse() != null) {
            return visitFuncUse(ctx.funcUse());
        } else if (ctx.unaryOp() != null) {
            return visitExp(ctx.exp(0));
        } else if (ctx.DIV() != null || ctx.MUL() != null || ctx.PLUS() != null || ctx.MINUS() != null || ctx.MOD() != null) {
            Type expTy1 = visitExp(ctx.exp(0));
            Type expTy2 = visitExp(ctx.exp(1));
            if ((!(expTy1 instanceof NullType) && !(expTy1 instanceof NumberType)) ||
                    (!(expTy2 instanceof NullType) && !(expTy2 instanceof NumberType))) {
                printError(ErrorType.OP_NOT_MATCH, ctx.getStart().getLine(), "variable should be number.");
            } else {
                return expTy1;
            }
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitLVal(SysYParser.LValContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        String ident = ctx.IDENT().getText();
        Type match = findIdentByCheckGOT(ident);
        if (match == null) {
            printError(ErrorType.VAR_NOT_DECL, ctx.getStart().getLine(), "variable not declared.");
        } else {
            if (ctx.exp() != null && !ctx.exp().isEmpty()){
                //array call
                int callDepth = ctx.exp().size();
                int i = 0;
                while (callDepth-- > 0) {
                    if (!(match instanceof ArrayType)) {
                        printError(ErrorType.NOT_ARRAY, ctx.getStart().getLine(), "non-array couldn't be visited by '['");
                        return DEFAULT_RETTYPE;
                    } else {
                        Type expTy = visitExp(ctx.exp(i++));
                        if (!(expTy instanceof NullType) && !(expTy instanceof IntType)) {
                            printError(ErrorType.OP_NOT_MATCH, ctx.getStart().getLine(), "'[' needs an int type.");
                            return DEFAULT_RETTYPE;
                        }
                        match = ((ArrayType) match).getElementType();
                    }
                }
            }
            return match;
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitFuncUse(SysYParser.FuncUseContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        String funcName = ctx.funcName().getText();
        Type match = findIdentByCheckGOT(funcName);
        if (match == null) {
            printError(ErrorType.FUNC_NOT_DECL, ctx.getStart().getLine(), "function not declared");
        } else {
            if (!(match instanceof FuncType)) {
                printError(ErrorType.CALL_OF_VAR, ctx.getStart().getLine(), "non-function couldn't be called by '('");
            } else {
                ArrayList<Type> paramsRTpList = ((FuncType) match).getParamsType();
                if (ctx.funcRParams() == null) {
                    if (paramsRTpList == null || paramsRTpList.isEmpty()) {
                        return ((FuncType) match).getRetType();
                    }
                    printError(ErrorType.FUNC_PARAMS_INVALID, ctx.getStart().getLine(), "function params invalid.");
                } else {
                    // here funcRParams isn't null, then at least 1
                    if (paramsRTpList != null && ctx.funcRParams().param().size() == paramsRTpList.size()) {
                        for (int i = 0; i < paramsRTpList.size(); i++) {
                            Type paramTy = visitParam(ctx.funcRParams().param(i));
                            if (paramTy == null || paramsRTpList.get(i) == null ||
                                    paramTy.getType() != paramsRTpList.get(i).getType()) {
                                printError(ErrorType.FUNC_PARAMS_INVALID, ctx.getStart().getLine(), "function params invalid.");
                            }
                        }
                    } else {
                        printError(ErrorType.FUNC_PARAMS_INVALID, ctx.getStart().getLine(), "function params invalid.");
                    }
                }
                return ((FuncType) match).getRetType();
            }
        }
        return DEFAULT_RETTYPE;
    }
    @Override
    public Type visitParam(SysYParser.ParamContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        return visitExp(ctx.exp());
    }
    @Override
    public Type visitStmt(SysYParser.StmtContext ctx) {
        if (ctx == null)
            return DEFAULT_RETTYPE;
        if (ctx.ASSIGN() != null) {
            //assign stmt
            Type lValTy = visitLVal(ctx.lVal());
            Type expTy = visitExp(ctx.exp());
            if (expTy != null && lValTy != null) {
                if (lValTy instanceof FuncType) {
                    printError(ErrorType.ASSIGN_FOR_FUNC, ctx.getStart().getLine(), "function couldn't be assigned.");
                } else {
                    if (lValTy.getType() != expTy.getType()) {
                        printError(ErrorType.ASSIGN_NOT_MATCH, ctx.getStart().getLine(), "assign type not matched.");
                    } else if (lValTy instanceof ArrayType && expTy instanceof ArrayType) {
                        if (((ArrayType) lValTy).level() != ((ArrayType) expTy).level()) {
                            printError(ErrorType.ASSIGN_NOT_MATCH, ctx.getStart().getLine(), "array level not matched.");
                        }
                    }
                }
            }
            return DEFAULT_RETTYPE;
        } else if(ctx.RETURN() != null) {
            Type retType;
            if (ctx.exp() == null) {
                //void return
                retType = new VoidType();
            } else {
                retType = visitExp(ctx.exp());
            }
            if (this.retType == null || retType == null ||
                    (!(retType instanceof NullType) && this.retType.getType() != retType.getType())) {
                printError(ErrorType.RET_NOT_MATCH, ctx.getStart().getLine(), "return type not matched.");
            }
            return retType;
        } else {
            return visitChildren(ctx);
        }
    }
}