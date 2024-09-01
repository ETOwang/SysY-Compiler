package cn.edu.nju.software.pass;

import cn.edu.nju.software.frontend.util.CFG;
import cn.edu.nju.software.frontend.util.Loop;
import cn.edu.nju.software.frontend.util.LoopSet;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.GlobalVar;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.*;

public class LoopInvariantCodeMotionPass implements FunctionPass {
    //TODO:注意这个pass一定要在冗余块消除之前做
    //变量与其所处块的对照关系表
    private final Map<ValueRef, BasicBlockRef> valueTable = new HashMap<>();
    //该函数涉及的cfg
    private CFG cfg;
    private boolean dbgFlag = false;
    @Override
    public boolean runOnFunction(FunctionValue function) {
        //首先判断是否有机会做循环不变式外提并构建变量的对照表
        if (!judgeCanDoPassAndBuildTable(function)) {
            return false;
        }
        //获得相应cfg
        cfg = CFGBuildPass.getInstance().getBasicBlockCFG(function);
        LoopBuildPass loopBuildPass = LoopBuildPass.getInstance();
        //获得相应的循环
        LoopSet loopSet = loopBuildPass.getLoopSet(function);
        boolean flag = false;
        for (Loop loop : loopSet.getLoops()) {
            //寻找循环中的条件判断块
            List<BasicBlockRef> judgeBlocks=findJudgeBlock(loop);
            //只对判断条件外提
            List<Instruction> instructions = identifyInvariants(loop,findEntry(loop),judgeBlocks);
            if(!instructions.isEmpty()){
                flag = true;
            }
            //不变式外提
            doPass(loop, instructions);
            //删除原来块中的冗余指令
            deleteRedundantInstructions(instructions,loop);
        }
        if(flag){
            printDbgInfo();
        }
        //注意一定要clear！
        valueTable.clear();
        return flag;
    }

    @Override
    public void printDbgInfo() {
        if (dbgFlag) {
            System.out.println(this.getName());
        }
    }

    @Override
    public void setDbgFlag() {
         dbgFlag = true;
    }

    @Override
    public String getName() {
        return "LoopInvariantCodeMotionPass";
    }

    private void doPass(Loop loop, List<Instruction> instructions) {
        if (instructions.isEmpty()) {
            return;
        }
        //获得入口节点，将可以外提的指令加入入口节点的倒数第二条指令
        //由于有空指令，最后一条指令的位置需要手动寻找
        BasicBlockRef entry = findEntry(loop);
        int pos=findLastInstr(entry);
        //逆序加入
        for (int i = instructions.size()-1; i >=0 ; i--) {
                entry.put(pos,instructions.get(i));
        }
        deleteRedundantInstructions(instructions,loop);
        //对子循环做同样的操作
        for (Loop subLoop: loop.getSubLoops()) {
            List<BasicBlockRef> judgeBlocks=findJudgeBlock(subLoop);
            //只对判断条件外提
            List<Instruction> subLoopInstructions = identifyInvariants(subLoop,findEntry(subLoop),judgeBlocks);
            doPass(subLoop, subLoopInstructions);
            deleteRedundantInstructions(subLoopInstructions,subLoop);
        }
    }

    private boolean judgeCanDoPassAndBuildTable(FunctionValue function) {
        //仅仅判断函数中是否存在循环并且构建变量与其所处块的表
        LoopBuildPass loopBuildPass = LoopBuildPass.getInstance();
        LoopSet loopSet = loopBuildPass.getLoopSet(function);
        if (loopSet == null) {
            return false;
        }
        for (BasicBlockRef basicBlockRef : function.getBasicBlockRefs()) {
            for (Instruction instruction : basicBlockRef.getIrs()) {
                ValueRef lVal = instruction.getLVal();
                valueTable.put(lVal, basicBlockRef);
            }
        }
        return true;
    }

    /**
     *
     * @param loop:需要进行不变式外提的循环
     * @param entry:循环的入口块（可以理解为不变式外提的目标块）
     * @param judgeBlocks:循环的判断块
     * @return:循环的判断块中可外提的指令列表
     */
    private List<Instruction> identifyInvariants(Loop loop,BasicBlockRef entry,List<BasicBlockRef> judgeBlocks) {
        //对指针更新的处理
        for (BasicBlockRef basicBlockRef : loop.getAllBasicBlocks()) {
            for (Instruction instruction : basicBlockRef.getIrs()) {
                if(instruction instanceof Store){
                    ValueRef dest=instruction.getOperand(1);
                    //仅仅为了表示这个变量在循环中被更新，防止下次load的时候误认为这个变量没有被更新
                    valueTable.put(dest,loop.getRoot());
                }
                //数组变量一律不外提
                if(instruction instanceof GEP gep){
                    ValueRef lVal=gep.getLVal();
                    //仅仅为了表示这个变量在循环中被更新，防止下次load的时候误认为这个变量没有被更新
                    valueTable.put(lVal,loop.getRoot());
                }
            }
        }
        List<Instruction> result = new ArrayList<>();
        for (BasicBlockRef basicBlockRef : judgeBlocks) {
            for (Instruction instruction : basicBlockRef.getIrs()) {
                if (instruction instanceof Br||instruction instanceof CondBr||instruction instanceof Call||instruction instanceof GEP) {
                    continue;
                }
                //通过判断与当前指令关联的值是否在循环内被更新来判断当前指令的左值是否被更新
                int opNum = instruction.getNumberOfOperands();
                boolean flag = true;
                for (int i = 0; i < opNum; i++) {
                    ValueRef operand = instruction.getOperand(i);
                    if(operand instanceof GlobalVar){
                        flag=false;
                        break;
                    }
                    if (operand instanceof ConstValue) {
                        continue;
                    }
                    if (valueTable.get(operand) == null) {
                        continue;
                    }
                    BasicBlockRef basicBlockRef1 = valueTable.get(operand);
                    if (loop.contains(basicBlockRef1)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    result.add(instruction);
                    //仅仅是为了表示这条指令已不在循环内，不代表这条指令真的在entry里
                    valueTable.put(instruction.getLVal(),entry);
                }
            }
        }
        return result;
    }


    private List<BasicBlockRef> findJudgeBlock(Loop loop) {
        //寻找判断块，基本思路是如果当前块有可能跳出循环，则其是判断块之一
        //TODO:无限循环
        List<BasicBlockRef> result = new ArrayList<>();
        BasicBlockRef root = loop.getRoot();
        Stack<BasicBlockRef> help = new Stack<>();
        help.add(root);
        result.add(root);
        //记录访问过的节点
        HashSet<BasicBlockRef> vis=new HashSet<>();
        while (!help.isEmpty()) {
            BasicBlockRef cur = help.pop();
            if(vis.contains(cur)){
                continue;
            }
            vis.add(cur);
            boolean flag = false;
            for (BasicBlockRef next : cfg.getSuccessors(cur)) {
                if (loop.contains(next)) {
                    help.add(next);
                } else {
                    if(cur!=root){
                        result.add(cur);
                    }
                    flag = true;
                }
            }
            if (!flag) {
                break;
            }
        }
        return result;
    }

    private BasicBlockRef findEntry(Loop loop) {
        //寻找root的前缀，不在循环中的即为入口节点
        BasicBlockRef root = loop.getRoot();
        for (int i = 0; i < root.getPredNum(); i++) {
            if (!loop.contains(root.getPred(i))) {
                return root.getPred(i);
            }
        }
        return null;
    }

    /**
     * @param instructions: 需要去除的冗余指令
     * @param loop: 需要去除的冗余指令所在的循环
     * */
    private void deleteRedundantInstructions(List<Instruction> instructions,Loop loop) {
        //对于冗余指令（即已经被外提的指令），替换为空指令
        for (Instruction instruction : instructions) {
            for (BasicBlockRef basicBlockRef : loop.getAllBasicBlocks()) {
                for (int i=0;i<basicBlockRef.getIrNum();i++) {
                    if (basicBlockRef.getIr(i).equals(instruction)) {
                        basicBlockRef.renewIr(i,new Default());
                    }
                }
            }
        }
    }

    private int findLastInstr(BasicBlockRef bb){
        return Util.findLastInstruction(bb);
    }
}
