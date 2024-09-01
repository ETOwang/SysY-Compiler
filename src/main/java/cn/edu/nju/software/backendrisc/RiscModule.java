package cn.edu.nju.software.backendrisc;

import cn.edu.nju.software.ir.module.ModuleRef;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class RiscModule {

    private final ModuleRef llvmModule;
    private final List<RiscFunction> riscFunctions = new ArrayList<>();
    private final List<RiscGlobalVar> riscGlobalVars = new ArrayList<>();
    private static final HashSet<String> libFuncs = new HashSet<>();
    private static long tempBlockCount = 0;/* 用于记录一个Moudle中后端额外生成的临时Block的数量 */

    static {
        Stream<String> funcs = Stream.of(
                "getint", "putint", "getfloat", "putfloat", "getarray", "putarray"
                , "getfarray", "putfarray", "getch", "putch", "starttime", "stoptime", "memset");
        funcs.forEach(libFuncs::add);
    }

    public RiscModule(ModuleRef llvmModule) {
        this.llvmModule = llvmModule;
        tempBlockCount = 0;
    }

    public void codeGen() {
        llvmModule.getGlobalVars()
                .forEach(globalVar -> riscGlobalVars.add(new RiscGlobalVar(globalVar)));
        llvmModule.getFunctions().stream()
                .filter(function -> !libFuncs.contains(function.getName()))
                .toList()
                .forEach(function -> {
                    RiscFunction riscFunction = new RiscFunction(function);
                    riscFunctions.add(riscFunction);
                    riscFunction.codeGen();
                });
    }

    public static String createTempBlock() {
        return "tempBlock" + tempBlockCount++;
    }

    public void dumpToConsole() {
        System.out.println(".data" + System.lineSeparator() + ".align 4");
        riscGlobalVars.forEach(RiscGlobalVar::dumpToConsole);
        riscFunctions.forEach(RiscFunction::dumpToConsole);
        appendRiscFunctions();
    }

    /**
     * 在文件末尾添加自定义risc库函数
     */
    private void appendRiscFunctions() {
        System.out.println(System.lineSeparator() + """
            memset:\s
                blez    a2, .LBB0_3\s
                add     a2, a2, a0\s
            .LBB0_2:\s
                sw      a1, 0(a0)\s
                addi    a0, a0, 4\s
                bltu    a0, a2, .LBB0_2\s
            .LBB0_3:\s
                ret\s""");
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
}
