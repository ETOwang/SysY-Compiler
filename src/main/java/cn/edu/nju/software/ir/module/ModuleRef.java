package cn.edu.nju.software.ir.module;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.type.ArrayType;
import cn.edu.nju.software.ir.type.FunctionType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.GlobalVar;
import cn.edu.nju.software.ir.value.LocalVar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ModuleRef {
    private final String moduleId;
    private final ArrayList<GlobalVar> globalVars;
    private final ArrayList<FunctionValue> functions;
    private int globalVarNum;
    private final ArrayList<String> libNameList = new ArrayList<>();

    private final static String TAB = "  ";

    public ModuleRef(String moduleName) {
        this.moduleId = moduleName;
        globalVars = new ArrayList<>();
        functions = new ArrayList<>();
        FunctionValue.clearDeclNames();
        GlobalVar.clearNames();
        globalVarNum = 0;
        libNameList.add("getint");
        libNameList.add("getch");
        libNameList.add("getfloat");
        libNameList.add("getarray");
        libNameList.add("getfarray");
        libNameList.add("putint");
        libNameList.add("putch");
        libNameList.add("putfloat");
        libNameList.add("putarray");
        libNameList.add("putfarray");
        libNameList.add("putf");
        libNameList.add("starttime");
        libNameList.add("stoptime");
    }

    public void addFunction(FunctionValue function) {
        functions.add(function);
    }

    public FunctionValue getFunction(int index) {
        return functions.get(index);
    }

    //todo() add getFunctions()
    public List<FunctionValue> getFunctions() {
        return functions;
    }
    public void dropFunction(FunctionValue functionValue) {
        functions.remove(functionValue);
    }
    public int getFunctionNum() {
        return functions.size();
    }

    public List<GlobalVar> getGlobalVars() {
        return Collections.unmodifiableList(globalVars);
    }

    public void addGlobalVar(GlobalVar globalVar) {
        globalVarNum++;
        globalVars.add(globalVar);
    }

    public int getGlobalVarNum() {
        return globalVars.size();
    }

    public GlobalVar getGlobalVar(int index) {
        return globalVars.get(index);
    }

    public void dropGlobalVar(GlobalVar globalVar) {
        globalVarNum--;
        globalVars.remove(globalVar);
    }

    private String generateGlobalVarIr(GlobalVar gv) {
        String ir = "@" + gv.getName() + " = global ";
        Pointer tyPtr = (Pointer) gv.getType();
        if (!(tyPtr.getBase() instanceof ArrayType)) {
            ir += tyPtr.getBase().toString() + " ";
            if (gv.getInitVal() instanceof ConstValue) {
                ir += gv.getInitVal() + ", ";
            } else {
                System.err.println("Global variable has not been initialized.");
                return null;
            }
        } else {
//            System.err.println(tyPtr.getBase());
            if (gv.getInitVal() instanceof ConstValue && ((ConstValue) gv.getInitVal()).getValue().equals(0)) {
                ir += tyPtr.getBase().toString() + " ";
                ir += "zeroinitializer, ";
            } else {
                ir += gv.getInitVal().toString() + ", ";
            }
        }
        ir += "align " + gv.getType().getWidth();
        return ir;
    }

    public void dumpToFile(String fileName) {
        // TODO
        if (fileName == null) {
            System.err.println("File name is null.");
            return;
        }
        // if file do not exist, create it else clear it's content
        if (!new java.io.File(fileName).exists()) {
            try {
                new java.io.File(fileName).createNewFile();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }

        PrintStream consoleStream = System.out;
        try (PrintStream ps = new PrintStream(new FileOutputStream(fileName))) {
            System.setOut(ps);
            dumpToConsole();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            System.setOut(consoleStream);
        }

    }

    public void dumpToConsole() {
        System.out.println("; ModuleId = '" + moduleId + "'");
        System.out.println("source_filename = \"" + moduleId + "\"");
        System.out.println(); // an empty line
        // declare lib functions
        dumpDeclares();
        // declare global var
        for (GlobalVar gv : globalVars) {
            String ir = generateGlobalVarIr(gv);
            if (ir != null) {
                System.out.println(ir);
            } else {
                return;
            }
        }

        if (globalVarNum > 0) {
            System.out.println(); // an empty line after declare all global variables
        }

        for (FunctionValue fv : functions) {
            int blockNameAreaLength = fv.getLengthOfLongestBlockName();
            if (libNameList.contains(fv.getName())) {
                continue;
            }
            FunctionType ft = ((FunctionType) fv.getType());

            System.out.print("define " + ft.getReturnType() + " @" + fv.getName());
            System.out.print("(");
            for (int i = 0; i < ft.getFParametersCount(); i++) {
                LocalVar fParam = fv.getParam(i);
                System.out.print(fParam.getType() + " ");
                System.out.print(fParam);
                if (i < ft.getFParametersCount() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.print(")");

            System.out.println(" {"); // start a function block
            for (int i = 0; i < fv.getBlockNum(); i++) {
                // output each basic block
                BasicBlockRef block = fv.getBasicBlockRef(i);
                System.out.print(block.getName() + ":");
                if (block.hasPred()) {
                    for (int k = 0; k < blockNameAreaLength - block.getName().length() + 40; k++) {
                        System.out.print(" ");
                    }
                    System.out.print("; pred = ");
                    for (int k = 0; k < block.getPredNum(); k++) {
                        System.out.print("%" + block.getPred(k).getName());
                        if (k < block.getPredNum() - 1) {
                            System.out.print(", ");
                        }
                    }
                }
                System.out.println();
                for (int j = 0; j < block.getIrNum(); j++) {
                    // output each ir in the basic block
                    //空指令不打印换行
                    if(!block.getIr(j).toString().isEmpty()){
                        System.out.println(TAB + block.getIr(j));
                    }

                }
                // when output a whole block, start a new line
                if (i < fv.getBlockNum() - 1) {
                    System.out.println();
                }
            }
            // when all basic blocks in the function finish output, a function ends
            System.out.println("}");
            System.out.println();
        }
    }

    private void  dumpDeclares() {
        Stream.of("declare i32 @getint()",
                "declare i32 @getch()",
                "declare float @getfloat()",
                "declare i32 @getarray(i32*)",
                "declare i32 @getfarray(float*)",
                "declare void @putint(i32)",
                "declare void @putch(i32)",
                "declare void @putfloat(float)",
                "declare void @putarray(i32, i32*)",
                "declare void @putfarray(i32, float*)",
                "declare void @_sysy_starttime(i32)",
                "declare void @_sysy_stoptime(i32)",
                "declare void @memset(i32*, i32, i32)"  // system func, just declare and then u can use it.
        ).forEach(System.out::println);
        System.out.printf("%n%n");
    }
}
