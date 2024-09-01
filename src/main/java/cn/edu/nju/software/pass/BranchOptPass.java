package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Br;
import cn.edu.nju.software.ir.instruction.CondBr;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.Phi;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.type.BoolType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;

/***
 * modify condBr -> Br <br>
 * e.g. br i1 true, %0, %1 -> br %0
 */
public class BranchOptPass {
    private final ModuleRef module;

    private final BoolType i1 = new BoolType();

    private final ConstValue TRUE = new ConstValue(i1, true);
    private final ConstValue FALSE = new ConstValue(i1, false);

    private final EliminateConstExp eliminateConstExp;

    public BranchOptPass(ModuleRef module) {
        this.module = module;
        eliminateConstExp = new EliminateConstExp();
    }

    private void rmRedundantCondBr() {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            for (int j = 0; j < fv.getBlockNum(); j++) {
                BasicBlockRef bb = fv.getBlock(j);
                for (int k = 0; k < bb.getIrNum(); k++) {
                    Instruction inst = bb.getIr(k);
                    if (inst instanceof CondBr condBr) { // this condBr jumb from bb
                        if (condBr.isRedundant()) {
                            ConstValue cond = (ConstValue) condBr.getOperand(0);
                            BasicBlockRef target, waste;
                            if (cond.equals(TRUE)) {
                                target = condBr.getTrueBlock();
                                waste = condBr.getFalseBlock();
                            } else {
                                target = condBr.getFalseBlock();
                                waste = condBr.getTrueBlock();
                            }
                            waste.dropPred(bb); // since unreachable from bb, so drop waste's pred --> bb
                            // modify waste block's phi node
                            modifyPhiInBlock(waste, bb);
                            Br br = new Br(target);
                            bb.replaceIr(condBr, br); // or bb.renewIr(k, br);
                        }
                    }
                }
            }
        }
    }

    /***
     * rm phi inst operand
     * @param customer: phi in which block
     * @param dropPred: customer's pred which is deleted
     */
    private void modifyPhiInBlock(BasicBlockRef customer, BasicBlockRef dropPred) {
        for (int i = 0; i < customer.getIrNum(); i++) {
            Instruction inst = customer.getIr(i);
            if (inst instanceof Phi phi) {
                if (phi.containsBlock(dropPred)) {
                    phi.dropBlock(dropPred);
                    if (phi.isRedundant()) {
                        phi.modify();
                        customer.dropIr(inst);
                        i--;
                    }

                }
            } else {
                break;
            }
        }
    }

    public void runOnModule() {
        rmRedundantCondBr();
        eliminateConstExp.runOnModule(module);
    }
}
