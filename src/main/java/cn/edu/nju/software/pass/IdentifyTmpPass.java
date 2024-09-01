package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Call;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class IdentifyTmpPass implements ModulePass {
    private IdentifyTmpPass() {}
    private static IdentifyTmpPass instance;
    private boolean change = false;

    private int noUse = 0;
    private int tmp = 0;
    public static IdentifyTmpPass getInstance() {
        if (instance == null) {
            instance = new IdentifyTmpPass();
        }
        return instance;
    }

    @Override
    public boolean runOnModule(ModuleRef module) {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            for (int j = 0; j < fv.getBlockNum(); j++) {
                BasicBlockRef bb = fv.getBasicBlockRef(j);
                for (int k = 0; k < bb.getIrNum(); k++) {
                    Instruction instruction = bb.getIr(k);
                    ValueRef lVal = instruction.getLVal(); // define in current block
                    if (lVal != null) {
                        if (lVal.getUser().size() > 1) {
                            continue;
                        }

                        if (lVal.getUser().isEmpty()) { // no use variable
                            if (instruction instanceof Call) {
                                continue; // call may bring side effect, do not delete
                            }
                            bb.dropIr(instruction);
                            k--;
                            noUse++;
                            change = true;
                            continue;
                        }
                        Instruction user = lVal.getUser().get(0);
                        if (user.getBlock().equals(bb)) { // use once and only in current block
                            lVal.setTmp(true);
                            tmp++;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            change = true;
            while (change) {
                change = false;
                for (int j = 0; j < fv.getBlockNum(); j++) {
                    BasicBlockRef bb = fv.getBasicBlockRef(j);
                    for (int k = 0; k < bb.getIrNum(); k++) {
                        Instruction instruction = bb.getIr(k);
                        ValueRef lVal = instruction.getLVal(); // define in current block
                        if (lVal != null) {
                            if (lVal.getUser().isEmpty()) {
                                if (instruction instanceof Call) {
                                    continue; // call may bring side effect, do not delete
                                }
                                bb.dropIr(instruction);
                                k--;
                                noUse++;
                                change = true;
                            }
                        }
                    }
                }
            }
        }
//        System.err.println("no use: " + noUse);
//        System.err.println("tmp: " + tmp);
//        System.err.flush();
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
