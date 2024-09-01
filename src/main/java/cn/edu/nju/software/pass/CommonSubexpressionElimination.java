package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.generator.IrCloneVisitor;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.*;

public class CommonSubexpressionElimination implements BasicBlockPass {
    private final Map<ValueRef, BasicBlockRef> posTable = new HashMap<>();
    private static int count = 0;
    private BasicBlockRef curBlock;
    @Override
    public boolean runOnBasicBlock(BasicBlockRef basicBlock) {
        curBlock=basicBlock;
        doPass(basicBlock);
        return true;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void setDbgFlag() {

    }

    @Override
    public void printDbgInfo() {

    }

    private void doPass(BasicBlockRef basicBlockRef) {
        Set<Set<Instruction>> commonSubexpression = findCommonSubexpression(basicBlockRef);
        for (Set<Instruction> subexpression : commonSubexpression) {
            if(subexpression.iterator().next() instanceof Load){
                if(!judgeLoad(basicBlockRef,subexpression)){
                    continue;
                }
            }
            ValueRef newValue = addMidInstruction(subexpression);
            assert newValue != null;
            //对用到的指令做替换
            for (Instruction ir : subexpression) {
                ValueRef lVal = ir.getLVal();
                for (Instruction user : lVal.getUser()) {
                    //特判call指令
                    if (user instanceof Call call) {
                        List<ValueRef> params = call.getRealParams();
                        for (int i = 0; i < params.size(); i++) {
                            if (params.get(i).equals(lVal)) {
                                newValue.addUser(user);
                                call.setParam(i, newValue);
                            }
                        }
                    }
                    ValueRef[] operands = user.getOperands();
                    for (int i = 0; i < operands.length; i++) {
                        if (operands[i].equals(lVal)) {
                            newValue.addUser(user);
                            user.setOperand(i, newValue);
                        }
                    }

                }
                ir.getBlock().dropIr(ir);
            }
        }
    }

    private Set<Set<Instruction>> findCommonSubexpression(BasicBlockRef basicBlockRef) {
        Set<Set<Instruction>> commonSubexpression = new HashSet<>();
        Map<OpEnum, Map<Instruction, Set<Instruction>>> record = new HashMap<>();
        for (Instruction ir : basicBlockRef.getIrs()) {
            OpEnum op = ir.getOp();
            if (ir.getLVal() != null) {
                posTable.put(ir.getLVal(), basicBlockRef);
            }
            if (record.containsKey(op)) {
                Map<Instruction, Set<Instruction>> content = record.get(op);
                boolean flag = false;
                for (Instruction instruction : content.keySet()) {
                    if (ir.equivalent(instruction)) {
                        content.get(instruction).add(ir);
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    content.put(ir, new HashSet<>());
                }
            } else {
                Map<Instruction, Set<Instruction>> content = new HashMap<>();
                content.put(ir, new HashSet<>());
                record.put(ir.getOp(), content);
            }
        }

        for (Map<Instruction, Set<Instruction>> content : record.values()) {
            for (Instruction instruction : content.keySet()) {
                if (!content.get(instruction).isEmpty()) {
                    Set<Instruction> temp = content.get(instruction);
                    temp.add(instruction);
                    commonSubexpression.add(temp);
                }
            }
        }
        return commonSubexpression;
    }

    private ValueRef addMidInstruction(Set<Instruction> subexpression) {
        Instruction temp = subexpression.iterator().next();
        IrCloneVisitor irCloneVisitor = new IrCloneVisitor();
        for (int i = 0; i < curBlock.getIrNum(); i++) {
            Instruction ir=curBlock.getIr(i);
            if(subexpression.contains(ir)){
                Instruction newInstr = irCloneVisitor.genClonedInstruction(temp);
                newInstr.getLVal().setName("m" + count++);
                if(newInstr instanceof Call call){
                    for (int j=0;j<call.getRealParams().size();j++){
                        ((Call)ir).getRealParam(j).addUser(newInstr);
                        call.setParam(j,((Call)ir).getRealParam(j));
                    }
                }else {
                    for (int j = 0; j < temp.getOperands().length; j++) {
                        temp.getOperand(j).addUser(newInstr);
                        newInstr.setOperand(j, temp.getOperand(j));
                    }
                }
                curBlock.put(i, newInstr);
                return newInstr.getLVal();
            }
        }
        return null;
    }


    private boolean judgeLoad(BasicBlockRef blockRef,Set<Instruction> instructions){
        int visNum=0;
        ValueRef pointer=instructions.iterator().next().getOperand(0);
        for (Instruction ir: blockRef.getIrs()) {
            if (ir instanceof Store store) {
                if(store.getOperand(1).equals(pointer)){
                    if(visNum!=instructions.size()&&visNum!=0){
                        return false;
                    }
                }
            } else if (instructions.contains(ir)) {
                visNum++;
            }
        }
        return true;
    }
}
