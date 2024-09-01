package cn.edu.nju.software.pass;

import cn.edu.nju.software.frontend.util.Loop;
import cn.edu.nju.software.frontend.util.LoopSet;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Allocate;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.Load;
import cn.edu.nju.software.ir.instruction.Store;
import cn.edu.nju.software.ir.type.ArrayType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.*;

public class ValueAnalyzePass implements FunctionPass{
    private static ValueAnalyzePass valueAnalyzePass;
    private static class Node implements Comparable<Node>{
        int loopDepth;
        int visCount;
        ValueRef valueRef;

        @Override
        public int compareTo(Node node) {
            if (node.loopDepth==this.loopDepth) {
                return node.visCount-this.visCount;
            }
            return node.loopDepth-this.loopDepth;
        }

        public Node(ValueRef valueRef){
            this.valueRef = valueRef;
            this.loopDepth = 0;
            this.visCount = 0;
        }
    }
    private final Map<ValueRef,Node> nodeTable=new HashMap<>();
    public static ValueAnalyzePass getInstance(){
        if(valueAnalyzePass==null){
            valueAnalyzePass=new ValueAnalyzePass();
        }
        return valueAnalyzePass;
    }
    private final Map<FunctionValue,List<ValueRef>> sortTable=new HashMap<>();
    private  List<ValueRef> sortedValue=new ArrayList<>();
    @Override
    public boolean runOnFunction(FunctionValue function) {
        if(function.isLib()){
            return false;
        }
        buildTable(function);
        sortValues();
        sortTable.put(function,sortedValue);
        sortedValue=new ArrayList<>();
        nodeTable.clear();
        return false;
    }

    @Override
    public void setDbgFlag() {

    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public String getName() {
        return "";
    }

    private void buildTable(FunctionValue function){
        LoopBuildPass.getInstance().update(function);
        LoopSet loopSet=LoopBuildPass.getInstance().getLoopSet(function);
        for (BasicBlockRef basicBlockRef:function.getBasicBlockRefs()){
            for (Instruction ir:basicBlockRef.getIrs()){
                if(ir instanceof Allocate allocate){
                    ValueRef lVal=allocate.getLVal();
                    TypeRef type=lVal.getType();
                    if(!(((Pointer) type).getBase() instanceof ArrayType)) {
                        nodeTable.put(lVal, new Node(lVal));
                    }
                }else if(ir instanceof Store store){
                    ValueRef pointer=store.getOperand(1);
                    if(nodeTable.containsKey(pointer)){
                        Node cur=nodeTable.get(pointer);;
                        cur.visCount++;
                    }
                } else if (ir instanceof Load load) {
                    ValueRef pointer=load.getOperand(0);
                    if(nodeTable.containsKey(pointer)){
                        Node cur=nodeTable.get(pointer);;
                        cur.visCount++;
                    }
                }
            }
        }
        if(loopSet!=null){
            for (Loop loop:loopSet.getLoops()){
                buildLoopDepth(loop,1);
            }
        }
    }

    private void buildLoopDepth(Loop loop,int depth){
        Set<BasicBlockRef> cur=loop.getAllBasicBlocks();
        for(Loop subLoop:loop.getSubLoops()){
            cur.retainAll(subLoop.getAllBasicBlocks());
        }
        for (BasicBlockRef basicBlockRef:cur){
            for(Instruction ir:basicBlockRef.getIrs()){
                if(ir instanceof Load load){
                    ValueRef pointer=load.getOperand(0);
                    if(nodeTable.containsKey(pointer)){
                        Node temp=nodeTable.get(pointer);;
                        temp.loopDepth+=depth;
                    }
                }else if(ir instanceof Store store){
                    ValueRef pointer=store.getOperand(1);
                    if(nodeTable.containsKey(pointer)){
                        Node temp=nodeTable.get(pointer);
                        temp.loopDepth+=depth;
                    }
                }
            }
        }
        for(Loop subLoop:loop.getSubLoops()){
            buildLoopDepth(subLoop,depth+1);
        }
    }

    private void sortValues(){
        List<Node> list = new ArrayList<>(nodeTable.values());
        Collections.sort(list);
        for (Node node:list){
            sortedValue.add(node.valueRef);
        }
    }

    public List<ValueRef> getSortedValue(FunctionValue function){
        return sortTable.get(function);
    }
}
