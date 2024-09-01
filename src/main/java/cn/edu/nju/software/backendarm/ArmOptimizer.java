package cn.edu.nju.software.backendarm;

import cn.edu.nju.software.backendarm.arminstruction.*;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmImmediateValue;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmIndirectRegister;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmOperand;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmRegister;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmComment;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ArmOptimizer {
    private final ArmModule armModule;

    public ArmOptimizer(ArmModule armModule) {
        this.armModule = armModule;
    }

    public void optimize() {
        clearComment();
        for (ArmFunction armFunction : armModule.getArmFunctions()) {
            for (ArmBasicBlock armBasicBlock : armFunction.getArmBasicBlocks()) {
                boolean changed;
                do {
                    changed = blockOptimize(armBasicBlock);
                } while (changed);
            }
        }
    }

    private void clearComment() {
        for (ArmFunction armFunction : armModule.getArmFunctions()) {
            for (ArmBasicBlock armBasicBlock : armFunction.getArmBasicBlocks()) {
                armBasicBlock.getArmInstructions().removeIf(armInstruction -> armInstruction instanceof ArmComment);
            }
        }
    }

    private boolean blockOptimize(ArmBasicBlock armBasicBlock) {
        ListIterator<ArmInstruction> iterator = armBasicBlock.getArmInstructions().listIterator();
        boolean changed = false;
        ArmInstruction pre;
        while (iterator.hasNext()) {
            pre = iterator.next();
            changed |= matchBinary(pre, iterator);
            changed |= matchMov(pre, iterator);
            changed |= matchRedundantMov(pre, iterator);
            changed |= matchImmediate(pre, iterator);
            changed |= matchMla(pre, iterator);
            changed |= matchIndirectStoreAndLoad(pre, iterator);
            changed |= matchDisplacementAddressing(pre, iterator);
            changed |= matchBranch(pre, iterator);
        }
        return changed;
    }

    /**
     * change instructions like
     * movw r0 ,#100
     * movt r0 ,#0
     * to
     * movw r0,#100
     */
    private boolean matchMov(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        ArmInstruction cur;
        if (iterator.hasNext()) {
            cur = iterator.next();
        } else {
            return false;
        }
        if (pre instanceof ArmMovw armMovw && cur instanceof ArmMovt armMovt) {
            ArrayList<ArmOperand> firstOperands = armMovw.getOperands();
            ArrayList<ArmOperand> secondOperands = armMovt.getOperands();
            if (!firstOperands.get(0).equals(secondOperands.get(0))) {
                iterator.previous();
                return false;
            }
            if (firstOperands.get(1) instanceof ArmImmediateValue) {
                if (secondOperands.get(1) instanceof ArmImmediateValue immediateValue) {
                    long val = immediateValue.getValue();
                    if (val == 0) {
                        iterator.remove();
                        return true;
                    }
                }
            }
        }
        iterator.previous();
        return false;
    }

    /**
     * remove binary instructions like
     * add r0 ,r0,#0
     */
    private boolean matchBinary(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        if (pre instanceof ArmAdd || pre instanceof ArmSub) {
            ArrayList<ArmOperand> operands = pre.getOperands();
            if (operands.get(0).equals(operands.get(1)) && operands.get(2) instanceof ArmImmediateValue armImmediateValue) {
                if (armImmediateValue.getValue() == 0) {
                    iterator.remove();
                    return true;
                }
            }
        } else if (pre instanceof ArmMul || pre instanceof ArmSdiv) {
            ArrayList<ArmOperand> operands = pre.getOperands();
            if (operands.get(0).equals(operands.get(1)) && operands.get(2) instanceof ArmImmediateValue armImmediateValue) {
                if (armImmediateValue.getValue() == 1) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * change instructions like
     * movw r0 ,#100
     * add r0 ,sp ,r0
     * to
     * add r0 ,sp ,#100
     */
    private boolean matchImmediate(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        ArmInstruction cur;
        if (iterator.hasNext()) {
            cur = iterator.next();
        } else {
            return false;
        }
        if (pre instanceof ArmMovw && (cur instanceof ArmAdd || cur instanceof ArmSub)) {
            ArrayList<ArmOperand> firstOperands = pre.getOperands();
            ArrayList<ArmOperand> secondOperands = cur.getOperands();
            if (!ArmSpecifications.isCalculatedReg(firstOperands.get(0).toString())) {
                iterator.previous();
                return false;
            }
            if (firstOperands.get(1) instanceof ArmImmediateValue immediateValue) {
                if (secondOperands.get(2).equals(firstOperands.get(0))) {
                    long val = immediateValue.getValue();
                    //todo arm immediate value range
                    if (canBeInmmediateValue((int)val) && (secondOperands.get(0).equals(firstOperands.get(0))
                            || secondOperands.get(0).equals(new ArmRegister("sp")))) {
                        secondOperands.remove(2);
                        secondOperands.add(immediateValue);
                        removeElement(1, iterator);
                        return true;
                    }
                }
            }
        }
        iterator.previous();
        return false;
    }

    private boolean canBeInmmediateValue(int val) {
        if(val < 0) {
            return false;
        }
        for(int i = 0; i < 16; i++) {
            if((val & ~0xFF) == 0) {
                return true;
            }
            val = (val << 2) | ( val >>> 30 );
        }
        return false;
    }

    /**
     * remove instructions like
     * mov r0 ,r0
     * and
     * mov r0,s0
     * mov r0,s0
     */
    private boolean matchRedundantMov(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        ArmInstruction cur;
        if (iterator.hasNext()) {
            cur = iterator.next();
        } else {
            return false;
        }
        if (pre instanceof ArmMov) {
            ArrayList<ArmOperand> operands = pre.getOperands();
            if (operands.get(0).equals(operands.get(1))) {
                removeElement(1, iterator);
                return true;
            }
        }
        if (pre instanceof ArmMov && cur instanceof ArmMov) {
            ArrayList<ArmOperand> firstOperands = pre.getOperands();
            ArrayList<ArmOperand> secondOperands = cur.getOperands();
            for (int i = 0; i < firstOperands.size(); i++) {
                if (!firstOperands.get(i).equals(secondOperands.get(i))) {
                    iterator.previous();
                    return false;
                }
            }
            iterator.remove();
            return true;
        }
        iterator.previous();
        return false;
    }

    /**
     * change instructions like
     * mul r0 ,r1,r2
     * add r3 ,r0,r4
     * to
     * mla r3 ,r1 ,r2,r4
     */
    private boolean matchMla(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        ArmInstruction cur;
        if (iterator.hasNext()) {
            cur = iterator.next();
        } else {
            return false;
        }
        if (pre instanceof ArmMul && cur instanceof ArmAdd) {
            ArrayList<ArmOperand> firstOperands = pre.getOperands();
            ArrayList<ArmOperand> secondOperands = cur.getOperands();
            if (firstOperands.get(0).equals(secondOperands.get(1))) {
                ArmMla armMla = new ArmMla(secondOperands.get(0), firstOperands.get(1), firstOperands.get(2), secondOperands.get(2));
                removeElement(1, iterator);
                iterator.next();
                iterator.set(armMla);
                return true;
            }
        }
        iterator.previous();
        return false;
    }

    /**
     * change instructions like
     * add r8, sp, #3520
     * str r4, [r8, #0] or ldr r4, [r8, #0]
     * to
     * str r4, [sp, #3520] or ldr r4, [sp, #3520]
     */
    private boolean matchIndirectStoreAndLoad(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        ArmInstruction cur;
        if (iterator.hasNext()) {
            cur = iterator.next();
        } else {
            return false;
        }
        if (pre instanceof ArmAdd && (cur instanceof ArmStr || cur instanceof ArmLdr)) {
            ArrayList<ArmOperand> firstOperands = pre.getOperands();
            ArrayList<ArmOperand> secondOperands = cur.getOperands();
            if (ArmSpecifications.isCalculatedReg(firstOperands.get(0).toString()) && firstOperands.get(1).equals(new ArmRegister("sp")) && firstOperands.get(2) instanceof ArmImmediateValue immediateValue) {
                ArmIndirectRegister indirectRegister = (ArmIndirectRegister) secondOperands.get(1);
                if (indirectRegister.getOpsh() != null) {
                    iterator.previous();
                    return false;
                }
                String reg = indirectRegister.getRegName();
                int offset = indirectRegister.getOffset();
                long firstOffset = immediateValue.getValue();
                long totalOffset = firstOffset + offset;
                if (reg.equals(firstOperands.get(0).toString()) && totalOffset >= -4095 && totalOffset <= 4095) {
                    indirectRegister.setOffset((int) totalOffset);
                    indirectRegister.setRegName("sp");
                    removeElement(1, iterator);
                    return true;
                }
            }
        }
        iterator.previous();
        return false;
    }

    /**
     * change instructions like
     * movw r8, #4
     * mla r4, r6, r8, r5
     * mov r11, r4
     * ldr r4, [r11, #0]
     * mov r11, r4
     * to
     * ldr r4, [r5,r6,lsl #2]
     * mov r11, r4
     */
    private boolean matchDisplacementAddressing(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        List<ArmInstruction> cur = new ArrayList<>();
        while (iterator.hasNext() && cur.size() < 4) {
            cur.add(iterator.next());
        }
        if (cur.size() < 4) {
            for (int i = 0; i < cur.size(); i++) {
                iterator.previous();
            }
            return false;
        }
        if (pre instanceof ArmMovw movw && cur.get(0) instanceof ArmMla mla && cur.get(1) instanceof ArmMov firstMov &&
                cur.get(2) instanceof ArmLdr ldr && cur.get(3) instanceof ArmMov secondMov) {
            if (!(movw.getOperands().get(1) instanceof ArmImmediateValue armImmediateValue) || armImmediateValue.getValue() != 4) {
                for (int i = 0; i < cur.size(); i++) {
                    iterator.previous();
                }
                return false;
            }
            ArrayList<ArmOperand> firstMovOperands = firstMov.getOperands();
            ArrayList<ArmOperand> secondMovOperands = secondMov.getOperands();
            ArrayList<ArmOperand> mlaOperands = mla.getOperands();
            ArrayList<ArmOperand> ldrOperands = ldr.getOperands();
            if (!firstMovOperands.get(0).equals(secondMovOperands.get(0)) || !firstMovOperands.get(1).equals(secondMovOperands.get(1))) {
                for (int i = 0; i < cur.size(); i++) {
                    iterator.previous();
                }
                return false;
            }
            if (!mlaOperands.get(2).equals(pre.getOperands().get(0)) || !mlaOperands.get(0).equals(firstMovOperands.get(1)) ||
                    !mlaOperands.get(0).equals(ldrOperands.get(0))) {
                for (int i = 0; i < cur.size(); i++) {
                    iterator.previous();
                }
                return false;
            }
            ArmIndirectRegister indirectRegister = (ArmIndirectRegister) ldrOperands.get(1);
            if (!indirectRegister.getRegName().equals(firstMovOperands.get(0).toString()) || indirectRegister.getOffset() != 0) {
                for (int i = 0; i < cur.size(); i++) {
                    iterator.previous();
                }
                return false;
            }
            removeElement(2, iterator);
            iterator.previous();
            iterator.remove();
            iterator.previous();
            iterator.remove();
            ArmIndirectRegister newRegister = new ArmIndirectRegister(mlaOperands.get(3).toString(), "lsl", 2, mlaOperands.get(1).toString());
            ldrOperands.remove(1);
            ldrOperands.add(newRegister);
            return true;
        }
        for (int i = 0; i < cur.size(); i++) {
            iterator.previous();
        }
        return false;
    }

    /**
     * change instructions like
     * cmp r5, r6
     * movlt r4, #1
     * movge r4, #0
     * mov r10, r4
     * cmp r4, #0
     * beq ifFalse_1
     * b ifTrue_1
     * to
     * cmp r5, r6
     * movlt r4, #1
     * movge r4, #0
     * mov r10, r4
     * cmp r4, #0
     * beq ifFalse_1
     * b ifTrue_1
     */
    private boolean matchBranch(ArmInstruction pre, ListIterator<ArmInstruction> iterator) {
        List<ArmInstruction> cur = new ArrayList<>();
        while (iterator.hasNext() && cur.size() < 4) {
            cur.add(iterator.next());
        }
        if (cur.size() < 4) {
            for (int i = 0; i < cur.size(); i++) {
                iterator.previous();
            }
            return false;
        }
        if (pre instanceof ArmCmp && (cur.get(0) instanceof ArmMovlt || cur.get(0) instanceof ArmMovge|| cur.get(0) instanceof ArmMoveq || cur.get(0) instanceof ArmMovgt||cur.get(0) instanceof ArmMovne||cur.get(0) instanceof ArmMovle)  &&
                cur.get(2) instanceof ArmMov && cur.get(3) instanceof ArmCmp ) {
            removeElement(0, iterator);
            iterator.previous();
            iterator.remove();
            iterator.previous();
            iterator.remove();
            iterator.previous();
            iterator.remove();
            ArmInstruction b = iterator.next();
            iterator.previous();
            ArmInstruction newInstr;
            if (cur.get(0) instanceof ArmMovlt) {
                newInstr = new ArmBge(b.getOperands().get(0));
            }else if(cur.get(0) instanceof ArmMoveq){
                newInstr = new ArmBne(b.getOperands().get(0));
            } else if (cur.get(0) instanceof ArmMovgt) {
                newInstr = new ArmBle(b.getOperands().get(0));
            } else if (cur.get(0) instanceof ArmMovne) {
                newInstr = new ArmBeq(b.getOperands().get(0));
            } else if (cur.get(0) instanceof ArmMovle) {
                newInstr = new ArmBgt(b.getOperands().get(0));
            } else {
                newInstr = new ArmBlt(b.getOperands().get(0));
            }
            iterator.set(newInstr);
            return true;
        }
        for (int i = 0; i < cur.size(); i++) {
            iterator.previous();
        }
        return false;
    }

    private void removeElement(int index, ListIterator<ArmInstruction> iterator) {
        for (int i = 0; i < index + 1; i++) {
            iterator.previous();
        }
        iterator.remove();
    }
}
