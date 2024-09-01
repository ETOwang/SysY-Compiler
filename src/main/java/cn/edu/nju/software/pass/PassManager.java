package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;

import java.util.ArrayList;
import java.util.List;

public class PassManager {
   private final ModuleRef module;
   List<Pass> allPasses;
    public PassManager(ModuleRef module) {
        this.module = module;
        allPasses=new ArrayList<>();
        register();
    }

    public boolean runPass() {
        boolean changed = false;
        for (Pass pass : allPasses) {
            if(pass instanceof ModulePass modulePass){
               changed|= modulePass.runOnModule(module);
            }else if(pass instanceof FunctionPass functionPass){
                for (FunctionValue functionValue: module.getFunctions()){
                    changed|= functionPass.runOnFunction(functionValue);
                }
            }else if(pass instanceof BasicBlockPass basicBlockPass){
                for (FunctionValue functionValue: module.getFunctions()){
                    for (BasicBlockRef basicBlockRef:functionValue.getBasicBlockRefs()){
                        changed|= basicBlockPass.runOnBasicBlock(basicBlockRef);
                    }
                }
            }
        }

        EliminateConstExp eliminateConstExp = EliminateConstExp.getInstance();
        EliminateDeadCode eliminateDeadCode = EliminateDeadCode.getInstance();
        do {
            eliminateConstExp.runOnModule(module);
        } while (eliminateDeadCode.runOnModule(module));

        ModifyUserPass modifyUserPass = ModifyUserPass.getInstance();
        IdentifyTmpPass identifyTmpPass = IdentifyTmpPass.getInstance();
        ValueAnalyzePass valueAnalyzePass = ValueAnalyzePass.getInstance();
        modifyUserPass.runOnModule(module);
        identifyTmpPass.runOnModule(module);
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (!fv.isLib()) {
                valueAnalyzePass.runOnFunction(fv);
            }
        }
        return changed;
    }

    //TODO:add pass here
    private void register(){
        allPasses.add(CFGBuildPass.getInstance());
        allPasses.add(LoopBuildPass.getInstance());
        allPasses.add(GlobalToLocalPass.getInstance());
        allPasses.add(GEPReductionPass.getInstance());
        allPasses.add(new FunctionInlinePass());
        allPasses.add(new RedundantBlockEliminationPass());
        allPasses.add(MemToReg.getInstance());
        allPasses.add(MergeBlockPass.getInstance());
        allPasses.add(MergeRepeatedArithmeticPass.getInstance());
        allPasses.add(StrengthReductionPass.getInstance());
        allPasses.add(EliminateConstExp.getInstance());
        allPasses.add(RegToMem.getInstance());
        allPasses.add(new CommonSubexpressionElimination());
        allPasses.add(MergeRepeatedArithmeticPass.getInstance());
        allPasses.add(new AddToMul());
        allPasses.add(new OptOptimize());
        allPasses.add(EliminateLoadStorePass.getInstance());
        allPasses.add(EliminateDeadCode.getInstance());
        allPasses.add(EliminateConstExp.getInstance());
        allPasses.add(EliminateDeadCode.getInstance());
        allPasses.add(MergeBlockPass.getInstance());
//        allPasses.add(IdentifyTmpPass.getInstance()); //must be commented
//        allPasses.add(ValueAnalyzePass.getInstance());
    }

    public void setDbgFlag(){
        for(Pass pass:allPasses){
            pass.setDbgFlag();
        }
    }

}
