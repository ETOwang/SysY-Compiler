package cn.edu.nju.software.frontend.util;



import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.value.FunctionValue;

import java.util.HashSet;
import java.util.Set;

public class Loop {

    private final BasicBlockRef root;

    private final Set<Loop> subLoops;

    private final FunctionValue functionValue;
    private final Set<BasicBlockRef> allBasicBlocks;
    private final Loop parentLoop;
    public Loop(BasicBlockRef root,Loop parentLoop,FunctionValue functionValue) {
        this.root = root;
        this.parentLoop = parentLoop;
        this.subLoops = new HashSet<>();
        this.allBasicBlocks = new HashSet<>();
        this.functionValue=functionValue;
    }

    public BasicBlockRef getRoot() {
        return root;
    }

    public Set<Loop> getSubLoops() {
        return subLoops;
    }
    public void addBasicBlock(BasicBlockRef bb) {
        allBasicBlocks.add(bb);
    }
    public void addSubLoop(Loop loop) {
        subLoops.add(loop);
    }
    public Set<BasicBlockRef> getAllBasicBlocks() {
        return allBasicBlocks;
    }

    public Loop getParentLoop() {
        return parentLoop;
    }
    public boolean contains(BasicBlockRef bb) {
        return allBasicBlocks.contains(bb);
    }

    public void createLoopGraph(String fileName){
       //todo
    }
}
