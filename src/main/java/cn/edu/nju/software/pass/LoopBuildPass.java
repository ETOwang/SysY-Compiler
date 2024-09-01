package cn.edu.nju.software.pass;

import cn.edu.nju.software.frontend.util.CFG;
import cn.edu.nju.software.frontend.util.Loop;
import cn.edu.nju.software.frontend.util.LoopSet;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;

import java.util.*;

public class LoopBuildPass implements ModulePass {
    private final Map<FunctionValue, LoopSet> forestTable;
    private final CFGBuildPass cfgBuildPass;
    private static LoopBuildPass loopBuildPass;
    private boolean dbgFlag = false;
    private Integer tot=0;
    private Integer cnt=-1;
    @Override
    public boolean runOnModule(ModuleRef module) {
        for (FunctionValue functionValue : module.getFunctions()) {
            LoopSet loopSet =findLoops(functionValue);
            if(!loopSet.isEmpty()){
                forestTable.put(functionValue, loopSet);
            }
        }
        if (dbgFlag) {
            printDbgInfo();
        }
        return false;
    }

    @Override
    public void setDbgFlag() {
        dbgFlag = true;
    }

    @Override
    public void printDbgInfo() {
        for (FunctionValue functionValue : forestTable.keySet()) {
            createLoopForestGraph(functionValue);
        }
    }
    public void update(FunctionValue functionValue){
        LoopSet loopSet =findLoops(functionValue);
        if(!loopSet.isEmpty()){
            forestTable.put(functionValue, loopSet);
        }
    }
    @Override
    public String getName() {
        return "Loop Build Pass";
    }

    public static LoopBuildPass getInstance() {
        if (loopBuildPass == null) {
            loopBuildPass = new LoopBuildPass();
        }
        return loopBuildPass;
    }

    public LoopSet getLoopSet(FunctionValue functionValue) {
        return forestTable.get(functionValue);
    }

    private LoopBuildPass() {
        this.forestTable = new HashMap<>();
        this.cfgBuildPass = CFGBuildPass.getInstance();
    }


    private LoopSet findLoops(FunctionValue functionValue) {
        Map<BasicBlockRef, Integer> scc = new HashMap<>();
        Map<BasicBlockRef, Integer> size = new HashMap<>();
        Stack<BasicBlockRef> stack = new Stack<>();
        Map<BasicBlockRef, Boolean> inStk = new HashMap<>();
        Map<BasicBlockRef, Integer> dfn = new HashMap<>();
        Map<BasicBlockRef, Integer> low = new HashMap<>();
        ArrayList<BasicBlockRef> roots = new ArrayList<>();
        Set<BasicBlockRef> set = new HashSet<>(functionValue.getBasicBlockRefs());
        tot=0;
        cnt=-1;
        for (BasicBlockRef bb : functionValue.getBasicBlockRefs()) {
            if (!dfn.containsKey(bb)) {
                tarjan(set, bb, functionValue, scc, size, stack, inStk, dfn, low, roots);
            }
        }
        return buildForest(functionValue, size, scc, roots);
    }

    private LoopSet buildForest(FunctionValue functionValue, Map<BasicBlockRef, Integer> size, Map<BasicBlockRef, Integer> scc, ArrayList<BasicBlockRef> roots) {
        LoopSet forest = new LoopSet();
        CFG cfg = cfgBuildPass.getBasicBlockCFG(functionValue);
        for (BasicBlockRef root : roots) {
            if (size.get(root) == 1) {
                continue;
            }
            Loop loop = new Loop(root, null, functionValue);
            buildLoopFromRoot(scc, cfg, root, loop);
            addSubLoops(loop,functionValue);
            forest.addLoop(loop);
        }
        return forest;
    }


    private void tarjan(Set<BasicBlockRef> blockSet, BasicBlockRef x, FunctionValue functionValue, Map<BasicBlockRef, Integer> scc, Map<BasicBlockRef, Integer> size, Stack<BasicBlockRef> stack, Map<BasicBlockRef, Boolean> inStk
            , Map<BasicBlockRef, Integer> dfn, Map<BasicBlockRef, Integer> low, ArrayList<BasicBlockRef> roots) {
        tot++;
        dfn.put(x, tot);
        low.put(x, tot);
        stack.push(x);
        inStk.put(x, true);
        CFG cfg = cfgBuildPass.getBasicBlockCFG(functionValue);
        for (BasicBlockRef y : cfg.getSuccessors(x)) {
            if (y == x) {
                continue;
            }
            if (!blockSet.contains(y)) {
                continue;
            }
            if (!dfn.containsKey(y)) {
                tarjan(blockSet, y, functionValue, scc, size, stack, inStk, dfn, low, roots);
                low.put(x, Math.min(low.get(x), low.get(y)));
            } else if (inStk.get(y)) {
                low.put(x, Math.min(low.get(x), dfn.get(y)));
            }
        }
        if (Objects.equals(dfn.get(x), low.get(x))) {
            BasicBlockRef y;
            ++cnt;
            do {
                y = stack.pop();
                inStk.put(y, false);
                scc.put(y, cnt);
                if (size.containsKey(x)) {
                    size.put(x, size.get(x) + 1);
                } else {
                    size.put(x, 1);
                }
            } while (y != x);
            roots.add(x);
        }
    }


    private void addSubLoops(Loop loop, FunctionValue functionValue) {
        Map<BasicBlockRef, Integer> scc = new HashMap<>();
        Map<BasicBlockRef, Integer> size = new HashMap<>();
        Map<BasicBlockRef, Integer> dfn;
        dfn = new HashMap<>();
        Map<BasicBlockRef, Integer> low = new HashMap<>();
        Stack<BasicBlockRef> stack = new Stack<>();
        Map<BasicBlockRef, Boolean> inStk = new HashMap<>();
        ArrayList<BasicBlockRef> roots = new ArrayList<>();
        Set<BasicBlockRef> blockSet = new HashSet<>();
        for (BasicBlockRef block : loop.getAllBasicBlocks()) {
            if (block != loop.getRoot()) {
                blockSet.add(block);
            }
        }
        cnt=-1;
        tot=0;
        CFG cfg = cfgBuildPass.getBasicBlockCFG(functionValue);
        tarjan(blockSet, loop.getRoot(), functionValue, scc, size, stack, inStk, dfn, low, roots);
        for (BasicBlockRef root : roots) {
            if (size.get(root) == 1) {
                continue;
            }
            Loop subLoop = new Loop(root, loop, functionValue);
            buildLoopFromRoot(scc, cfg, root, subLoop);
            addSubLoops(subLoop, functionValue);
            loop.addSubLoop(subLoop);
        }

    }

    private void buildLoopFromRoot(Map<BasicBlockRef, Integer> scc, CFG cfg, BasicBlockRef root, Loop subLoop) {
        Queue<BasicBlockRef> help = new LinkedList<>();
        Set<BasicBlockRef> record = new HashSet<>();
        help.add(root);
        while (!help.isEmpty()) {
            BasicBlockRef cur = help.poll();
            if (record.contains(cur)) {
                continue;
            }
            record.add(cur);
            for (BasicBlockRef bb : cfg.getSuccessors(cur)) {
                if (Objects.equals(scc.get(bb), scc.get(cur))) {
                    subLoop.addBasicBlock(bb);
                    help.add(bb);
                }
            }
        }
    }

    private void createLoopForestGraph(FunctionValue functionValue) {
        LoopSet loopSet = getLoopSet(functionValue);
        loopSet.createLoopForestGraph(functionValue.getName() + "loopSet");
    }
}
