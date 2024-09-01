package cn.edu.nju.software.frontend.util;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;

import java.util.HashSet;
import java.util.Set;
public class CFG {
    private final Graph<BasicBlockRef> graph;

    public CFG(){
        graph=new Graph<>();
    }
    public void addPoint(BasicBlockRef bb){
        graph.addNode(bb);
    }

    public void addEdge(BasicBlockRef from,BasicBlockRef to){
        graph.addEdge(from,to);
    }

    public void removeEdge(BasicBlockRef from,BasicBlockRef to) {
        graph.removeEdge(from, to);
    }

    public Set<Edge<BasicBlockRef>> getAllEdges(){
        return graph.getAllEdges();
    }
    public Set<BasicBlockRef> getSuccessors(BasicBlockRef bb){
        return graph.getNeighbors(bb);
    }

    public Set<BasicBlockRef> getPreds(BasicBlockRef bb){
        Set<BasicBlockRef> preds = new HashSet<>();
        for (BasicBlockRef pred : getAllBasicBlock()) {
            if (getSuccessors(pred).contains(bb)) preds.add(pred);
        }
        return preds;
    }

    public Set<BasicBlockRef> getAllBasicBlock(){
        return graph.getAllNodes();
    }
    public void dumpWholeGraph(String fileName){
       //todo:
    }

    public boolean isEmpty() {
        return graph.isEmpty();
    }
}

