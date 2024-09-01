package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Allocate;
import cn.edu.nju.software.ir.instruction.Call;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.Store;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.*;

public class GlobalToLocalPass implements ModulePass {
    private static GlobalToLocalPass glob2LocalPass = null;

    private final EliminateConstExp eliminateConstExp;
    private ModuleRef module;

    private GlobalToLocalPass() {
        eliminateConstExp = EliminateConstExp.getInstance();
    }

    public static GlobalToLocalPass getInstance() {
        if (glob2LocalPass == null) {
            glob2LocalPass = new GlobalToLocalPass();
        }
        return glob2LocalPass;
    }

    @Override
    public boolean runOnModule(ModuleRef module) {
        this.module = module;
        doGlobalToLocal();
        eliminateConstExp.runOnModule(this.module);
        return false;
    }

    private void doGlobalToLocal() {
        for (int i = 0; i < module.getGlobalVarNum(); i++) {
            GlobalVar gv = module.getGlobalVar(i);
            if (gv.isRedundant()) {
                module.dropGlobalVar(gv);
                i--;
            } else if (gv.isLocalizable()) { // only base type localize now
                FunctionValue fv = gv.getUsageFunction();
                BasicBlockRef entry = fv.getEntryBlock();
                LocalVar pointer = entry.createLocalVar(gv.getType(), "gv_to_lv");
                Allocate allocate = new Allocate(pointer);
                ConstValue init = (ConstValue) gv.getInitVal();
                Store store = new Store(init, pointer);
                // gv -> lv
                entry.put(allocate);
                entry.put(entry.getAllocSize(), store);
                module.dropGlobalVar(gv);
                i--; // remember i-- because drop global var
                for (Instruction user : gv.getUser()) {
                    // because of base type, its memory pointer won't be used in Call inst
                    int opNum = user.getNumberOfOperands();
                    for (int j = 0; j < opNum; j++) {
                        if (user.getOperand(j).equals(gv)) {
                            user.replace(gv, pointer);
                        }
                    }
                    if (user instanceof Call call) {
                        for (ValueRef vr : call.getRealParams()) {
                            if (vr.equals(gv)) {
                                call.replaceRealParams(gv, pointer);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "GlobalTooLocalPass";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
