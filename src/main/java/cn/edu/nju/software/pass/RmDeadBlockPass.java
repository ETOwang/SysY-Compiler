package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.module.ModuleRef;

// TODO implement *Pass
public class RmDeadBlockPass {
    private final ModuleRef module;
    private final EliminateConstExp eliminateConstExp;

    public RmDeadBlockPass(ModuleRef module) {
        this.module = module;
        eliminateConstExp = new EliminateConstExp();
    }

    private void rmDeadBlocks() {
        // TODO
    }

    public boolean runOnModule() {
        rmDeadBlocks();
        eliminateConstExp.runOnModule(module);
        return false; // TODO
    }
}
