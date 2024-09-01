package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;

/***
 * merge blocks which are clean without phi and with direct br
 * e.g.
 * %0:
 * ... br label %1
 *
 * %1:
 * ... not phi
 * merge %0 & %1
 */
public class MergeBlockPass implements ModulePass {
    private static MergeBlockPass instance;
    public static MergeBlockPass getInstance() {
        if (instance == null) {
            instance = new MergeBlockPass();
        }
        return instance;
    }
    private MergeBlockPass() {}
    private ModuleRef module;
    @Override
    public boolean runOnModule(ModuleRef module) {
        this.module = module;
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            mergeForFunction(fv);
        }
        return false;
    }

    private void mergeForFunction(FunctionValue fv) {
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            Instruction last = bb.getIr(Util.findLastInstruction(bb));
            if (last instanceof Br br) {
                BasicBlockRef target = br.getTarget();
                if (target.getPredNum() != 1) {
                    continue;
                }
                int f = 0;
                Instruction first = target.getIr(f++);
                while (first instanceof Default) {
                    first = target.getIr(f++);
                }
                if (first instanceof Phi) {
                    continue;
                }
                // merge
                bb.dropIr(last);
                bb.mergeWith(target); // bb append target -> bb-target
                // modify target's target pred
                last = target.getIr(Util.findLastInstruction(target));
                if (last instanceof Br br1) {
                    BasicBlockRef tar = br1.getTarget();
                    tar.dropPred(target);
                    tar.addPred(bb);
                    // tar's phi
                    replacePhiBlock(tar, target, bb);
                } else if (last instanceof CondBr condBr) {
                    BasicBlockRef t = condBr.getTrueBlock(), fa = condBr.getFalseBlock();
                    t.dropPred(target);
                    fa.dropPred(target);
                    t.addPred(bb);
                    fa.addPred(bb);

                    replacePhiBlock(t, target, bb);
                    replacePhiBlock(fa, target, bb);
                }
                fv.dropBlock(target); // delete target
                i--;
            }
        }
    }

    private void replacePhiBlock(BasicBlockRef tar, BasicBlockRef old, BasicBlockRef newB) {
        for (int j = Util.findFirstInstruction(tar); j < tar.getIrNum(); j++) {
            Instruction inst = tar.getIr(j);
            if (inst instanceof Phi phi) {
                phi.replace(old, newB);
            } else {
                break;
            }
        }
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
