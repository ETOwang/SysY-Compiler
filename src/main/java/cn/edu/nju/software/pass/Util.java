package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Br;
import cn.edu.nju.software.ir.instruction.CondBr;
import cn.edu.nju.software.ir.instruction.Default;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class Util {
    public static int findLastInstruction(BasicBlockRef bb) {
        for (int i = bb.getIrNum() - 1; i >= 0; i--) {
            if (!(bb.getIr(i) instanceof Default)) {
                return i;
            }
        }
        return -1;
    }

    public static int findFirstInstruction(BasicBlockRef bb) {
        for (int i = 0; i < bb.getIrNum(); i++) {
            if (!(bb.getIr(i) instanceof Default)) {
                return i;
            }
        }
        return -1;
    }

    public static void adjustPred(FunctionValue functionValue){
        for (BasicBlockRef bb: functionValue.getBasicBlockRefs()) {
            bb.clearPred();
        }
        for (BasicBlockRef bb:functionValue.getBasicBlockRefs()){
            int lastInstr=findLastInstruction(bb);
            Instruction instruction=bb.getIr(lastInstr);
            if(instruction instanceof Br br){
                BasicBlockRef target=br.getTarget();
                target.addPred(bb);
            }else if(instruction instanceof CondBr condBr){
                BasicBlockRef ifTrue=condBr.getTrueBlock();
                BasicBlockRef ifFalse=condBr.getFalseBlock();
                ifTrue.addPred(bb);
                ifFalse.addPred(bb);
            }
        }
    }
    public static int calculateEffectiveInstr(BasicBlockRef bb){
        int res=0;
        for (Instruction instruction:bb.getIrs()){
            if(!(instruction instanceof Default)){
                res++;
            }
        }
        return res;
    }
    public static class GEPPair {
        private final ValueRef pointer;
        private final ValueRef start;
        private final ValueRef end;

        public GEPPair(ValueRef pointer, ValueRef start, ValueRef end) {
            this.pointer = pointer;
            this.start = start;
            this.end = end;
        }

        public boolean equals(GEPPair other) {
            return pointer.equals(other.pointer) && start.equals(other.start) && end.equals(other.end);
        }
    }
}
