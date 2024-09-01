package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.Phi;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;

public class EliminateRedundantPhi implements ModulePass{
    private static EliminateRedundantPhi instance;
    public static EliminateRedundantPhi getInstance() {
        if (instance == null) {
            instance = new EliminateRedundantPhi();
        }
        return instance;
    }
    private boolean changed = false;

    private void modifyPhi(FunctionValue fv) {
        // rm redundant phi
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef block = fv.getBlock(i);
            for (int j = 0; j < block.getIrNum(); j++) {
                Instruction inst = block.getIr(j);
                if (inst instanceof Phi phi) {
                    if (phi.isRedundant()) { // only 2 operands: [value, block]
                        phi.modify();
                        block.dropIr(inst);
                        j--;
                        changed = true;
                    }
                }
            }
        }
    }

    private EliminateRedundantPhi() {}
    @Override
    public boolean runOnModule(ModuleRef module) {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            do {
                changed = false;
                modifyPhi(fv);
            } while (changed);
        }
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
