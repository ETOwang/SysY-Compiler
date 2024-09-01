package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

public class RedundantBlockEliminationPass implements ModulePass {
    @Override
    public boolean runOnModule(ModuleRef moduleRef) {
        for (FunctionValue function : moduleRef.getFunctions()) {
            if (function.getBlockNum() == 0) {
                continue;
            }
            //首先简单删除不可达块
            ListIterator<BasicBlockRef> basicBlockRefListIterator = function.getBasicBlockRefs().listIterator();
            while (basicBlockRefListIterator.hasNext()) {
                BasicBlockRef basicBlockRef = basicBlockRefListIterator.next();
                if (basicBlockRef.getPredNum() == 0 && !basicBlockRef.equals(function.getEntryBlock())) {
                    basicBlockRefListIterator.remove();
                    Set<BasicBlockRef> successors = findSuccessors(basicBlockRef);
                    for (BasicBlockRef successor : successors) {
                        successor.dropPred(basicBlockRef);
                    }
                }
            }
            ListIterator<BasicBlockRef> listIterator = function.getBasicBlockRefs().listIterator();
            while (listIterator.hasNext()) {
                BasicBlockRef basicBlockRef = listIterator.next();
                for (int i = 0; i < basicBlockRef.getPredNum(); i++) {
                    if (judgeIsRedundant(basicBlockRef, basicBlockRef.getPred(i))) {
                        mergeBlock(basicBlockRef, basicBlockRef.getPred(i));
                        listIterator.remove();
                        break;
                    }
                }
            }

            Util.adjustPred(function);
            CFGBuildPass.getInstance().update(function);
            LoopBuildPass.getInstance().update(function);
        }
        return true;
    }

    /**
     * @param bb:要判断是否冗余的块,若是,下一步将其合并入前继块
     * @param pred:目标块的前继
     * @return 目标块是否是冗余块
     */
    private boolean judgeIsRedundant(BasicBlockRef bb, BasicBlockRef pred) {
        Set<BasicBlockRef> predSuc = findSuccessors(pred);
        Set<BasicBlockRef> bbSuc = findSuccessors(bb);
        for (BasicBlockRef suc : bbSuc) {
            Set<BasicBlockRef> sucSuc = findSuccessors(suc);
            for (BasicBlockRef suc2 : sucSuc) {
                if (suc2.equals(bb)) {
                    //bb是循环的头节点，不能合并
                    return false;
                }
            }
        }
        if (bb.getPredNum() > 1) {
            if (Util.calculateEffectiveInstr(bb) == 1) {
                int index = Util.findLastInstruction(bb);
                Instruction instruction = bb.getIr(index);
                //如果这条指令是ret或condbr，也不能合并
                return !(instruction instanceof Ret) && !(instruction instanceof CondBr);
            }
            //有多个前继且不止一条指令也无法合并
            return false;
        }
        //前继有多个后继节点，不可合并
        return predSuc.size() <= 1;
    }

    /**
     * @param bb:将要被合并的基本块
     * @param target:要合并到的基本块
     */
    private void mergeBlock(BasicBlockRef bb, BasicBlockRef target) {
        int last = Util.findLastInstruction(bb);
        Instruction instr = bb.getIr(last);
        //加一个判断条件，防止误合并
        if (Util.calculateEffectiveInstr(bb) == 1 && !(instr instanceof Ret)) {
            BasicBlockRef successor = null;
            //判断是否还有后继
            if (findSuccessors(bb).iterator().hasNext()) {
                successor = findSuccessors(bb).iterator().next();
            }

            for (int i = 0; i < bb.getPredNum(); i++) {
                BasicBlockRef pred = bb.getPred(i);
                int index = Util.findLastInstruction(pred);
                ValueRef[] operands = pred.getIr(index).getOperands();
                for (int j = 0; j < operands.length; j++) {
                    if (operands[j].equals(bb)) {
                        pred.getIr(index).setOperand(j, successor);
                    }
                }
            }
            //更改bb后继的前继
            if (successor != null) {
                successor.dropPred(bb);
                for (int i = 0; i < bb.getPredNum(); i++) {
                    BasicBlockRef pred = bb.getPred(i);
                    successor.addPred(pred);
                }
            }

            return;
        }
        int lastInstrPos = Util.findLastInstruction(target);
        //将最后一句Br删去
        target.renewIr(lastInstrPos, new Default());
        for (int i = bb.getIrNum() - 1; i >= 0; i--) {
            Instruction instruction = bb.getIr(i);
            if (!(instruction instanceof Default)) {
                target.put(lastInstrPos, instruction);
            }
        }
        Set<BasicBlockRef> suc = findSuccessors(bb);
        for (BasicBlockRef suc2 : suc) {
            suc2.dropPred(bb);
            suc2.addPred(target);
        }
    }

    @Override
    public void setDbgFlag() {

    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public String getName() {
        return "Redundant Block Elimination Pass";
    }

    //cfg无法动态更新，因此另写一个方法获取后继块
    private Set<BasicBlockRef> findSuccessors(BasicBlockRef bb) {
        Set<BasicBlockRef> res = new HashSet<>();
        int index = Util.findLastInstruction(bb);
        Instruction instruction = bb.getIr(index);
        if (instruction instanceof Br br) {
            res.add(br.getTarget());
        } else if (instruction instanceof CondBr condBr) {
            res.add(condBr.getTrueBlock());
            res.add(condBr.getFalseBlock());
        }
        return res;
    }


}
