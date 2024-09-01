package cn.edu.nju.software.backendarm;

import cn.edu.nju.software.ir.module.ModuleRef;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class ArmModule {

    private final ModuleRef llvmModule;
    private final List<ArmFunction> armFunctions = new ArrayList<>();
    private final List<ArmGlobalVar> armGlobalVars = new ArrayList<>();
    private static final HashSet<String> libFuncs = new HashSet<>();
    private static long tempBlockCount = 0;/* 用于记录一个Moudle中后端额外生成的临时Block的数量 */

    static {
        Stream<String> funcs = Stream.of(
                "getint", "putint", "getfloat", "putfloat", "getarray", "putarray"
                , "getfarray", "putfarray", "getch", "putch", "starttime", "stoptime", "memset");
        funcs.forEach(libFuncs::add);
    }

    public ArmModule(ModuleRef llvmModule) {
        this.llvmModule = llvmModule;
        tempBlockCount = 0;
    }

    public void codeGen() {
        llvmModule.getGlobalVars()
                .forEach(globalVar -> armGlobalVars.add(new ArmGlobalVar(globalVar)));
        llvmModule.getFunctions().stream()
                .filter(function -> !libFuncs.contains(function.getName()))
                .toList()
                .forEach(function -> {
                    ArmFunction armFunction = new ArmFunction(function);
                    armFunctions.add(armFunction);
                    armFunction.codeGen();
                });
    }

    public static String createTempBlock() {
        return "tempBlock" + tempBlockCount++;
    }

    public void dumpToConsole() {
        /*
        .arch armv7ve
        .fpu vfpv3-d16
         */
        System.out.println(".arch armv7ve");
        System.out.println(".fpu vfpv3-d16");
        dumpGlobal(); // global variables (var, array)
        armFunctions.forEach(ArmFunction::dumpToConsole);
        appendArmFunctions();
    }

    private void dumpGlobal() {
        List<ArmGlobalVar> initializedVars = armGlobalVars.stream().filter(var -> !var.isUninitialized()).toList();
        List<ArmGlobalVar> uninitializedVars = armGlobalVars.stream().filter(ArmGlobalVar::isUninitialized).toList();
        System.out.println(".data" + System.lineSeparator() + ".align 4");
        if (!initializedVars.isEmpty()) {
            initializedVars.forEach(ArmGlobalVar::dumpToConsole);
        }
        System.out.println(".bss" + System.lineSeparator() + ".align 4");
        if (!uninitializedVars.isEmpty()) {
            uninitializedVars.forEach(ArmGlobalVar::dumpToConsole);
        }
    }

    /**
     * 在文件末尾添加自定义arm库函数
     */
    private void appendArmFunctions() {
        System.out.println(System.lineSeparator() + """
             memset:\s
                 cmp     r2, #0\s
                 ble     .LBB0_3\s
                 add     r2, r2, r0\s
             .LBB0_2:\s
                 str     r1, [r0]\s
                 add     r0, r0, #4\s
                 cmp     r0, r2\s
                 blt     .LBB0_2\s
             .LBB0_3:\s
                 bx      lr\s""");
    }

    public void dumpToFile(String path) {
        if (!new java.io.File(path).exists()) {
            try {
                new java.io.File(path).createNewFile();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        PrintStream consoleStream = System.out;
        try (PrintStream ps = new PrintStream(new FileOutputStream(path))) {
            System.setOut(ps);
            dumpToConsole();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            System.setOut(consoleStream);
        }
    }

    public List<ArmFunction> getArmFunctions() {
        return armFunctions;
    }
}
