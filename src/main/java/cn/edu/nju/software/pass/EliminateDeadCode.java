package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.type.BoolType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class EliminateDeadCode implements ModulePass {

    private static final EliminateDeadCode eliminateDeadCode = new EliminateDeadCode();

    private ModuleRef module;

    private EliminateDeadCode() {}

    private boolean change = false;
    private boolean tot = false;

    private final ConstValue trueValue = new ConstValue(new BoolType(), true);
    private final ConstValue falseValue = new ConstValue(new BoolType(), false);

    public static EliminateDeadCode getInstance() {
        return eliminateDeadCode;
    }

    @Override
    public boolean runOnModule(ModuleRef module) {
        this.module = module;
        tot = false;
        eliminateOnModule();
        while (change) {
            change = false;
            eliminateOnModule();
        }
        for (FunctionValue functionValue: module.getFunctions()) {
            CFGBuildPass.getInstance().update(functionValue);
        }
        return tot;
    }

    private void eliminateOnModule() {
        EliminateRedundantPhi eliminateRedundantPhi = EliminateRedundantPhi.getInstance();
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            eliminateOnFunction(fv);
            eliminateUnreachableBlock(fv);
            eliminateRedundantBlock(fv); // difficult bug to solve: different incoming value from same pred of phi
            eliminatePhiDeadBlock(fv);
        }
        eliminateRedundantPhi.runOnModule(module);
    }

    public void eliminateOnFunction(FunctionValue fv) {
        // e.g. br i1 true,label %1, label %2
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            int lastIndex = Util.findLastInstruction(bb);
            Instruction inst = bb.getIr(lastIndex);
            if (inst instanceof CondBr br) {
                ValueRef cond = br.getOperand(0);
                if (cond instanceof ConstValue) {
                    BasicBlockRef tar, drop;
                    if (cond.equals(trueValue)) {
                        tar = br.getTrueBlock();
                        drop = br.getFalseBlock();
                    } else {
                        tar = br.getFalseBlock();
                        drop = br.getTrueBlock();
                    }
                    change = true;
                    tot = true;
                    Br replace = new Br(tar);
                    drop.dropPred(bb);
                    // modify drop's phi
                    for (int j = 0; j < drop.getIrNum(); j++) {
                        Instruction instruction = drop.getIr(j);
                        if (instruction instanceof Phi phi) {
                            phi.dropBlock(bb);
                        }
                    }
                    bb.replaceIr(br, replace);
                }
            }
        }
    }

    public void eliminateUnreachableBlock(FunctionValue function) {
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            if (bb.getPredNum() == 0 && !bb.getName().contains("Entry")) {
                change = true;
                tot = true;
                // useless block, delete it
                Instruction last = bb.getIr(Util.findLastInstruction(bb));
                if (last instanceof CondBr condBr) {
                    BasicBlockRef t = condBr.getTrueBlock(), f = condBr.getFalseBlock();
                    t.dropPred(bb);
                    f.dropPred(bb);
                } else if (last instanceof Br br) {
                    BasicBlockRef tar = br.getTarget();
                    tar.dropPred(bb);
                }
                function.dropBlock(bb);
                i--;
            }
        }
    }

    public void eliminateRedundantBlock(FunctionValue function) {
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            if (bb.getName().contains("Entry")) {
                continue;
            }
            int d = 0;
            Instruction first = bb.getIr(d);
            d++;
            while (first instanceof Default) {
                first = bb.getIr(d);
                d++;
            }
            if (first instanceof Br br) {// bb is redundant
                change = true;
                tot = true;
                BasicBlockRef tar = br.getTarget();
                tar.dropPred(bb);
                // modify for bb pres
                for (int j = 0; j < bb.getPredNum(); j++) {
                    BasicBlockRef pred = bb.getPred(j);
                    Instruction last = pred.getIr(Util.findLastInstruction(pred));
                    if (last instanceof CondBr || last instanceof Br) {
                        last.replace(bb, tar); // renew target
                        tar.addPred(pred);
                    }
                    // modify for tar's phi
                    for (int k = 0; k < tar.getIrNum(); k++) {
                        Instruction inst = tar.getIr(k);
                        if (inst instanceof Phi phi) {
                            phi.replace(bb, pred); // replace pred block
                        } else {
                            break;
                        }
                    }
                }
                function.dropBlock(bb);
//                System.err.println(bb);
                i--;
            }
        }
    }

    private void eliminatePhiDeadBlock(FunctionValue function) {
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction inst = bb.getIr(j);
                if (inst instanceof Phi phi) {
                    for (int k = 0; k < phi.getPredSize(); k++) { // check pred block if existing
                        BasicBlockRef pred = phi.getPredBlock(k);
                        if (!function.containsBlock(pred) || !phi.containsBlock(pred)) {
                            change = true;
                            tot = true;
                            phi.dropBlock(pred);
                        }
                    }
                    // the phi inst may be redundant
                } else {
                    break;
                }
            }
        }
    }



    @Override
    public String getName() {
        return "Eliminate Dead Branch";
    }

    @Override
    public void printDbgInfo() {}

    @Override
    public void setDbgFlag() {}
}
