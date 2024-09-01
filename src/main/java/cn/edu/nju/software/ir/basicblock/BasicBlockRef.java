package cn.edu.nju.software.ir.basicblock;

import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.type.FloatType;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;
import cn.edu.nju.software.pass.Util;

import java.util.ArrayList;
import java.util.List;

public class BasicBlockRef extends ValueRef {
    private final static ArrayList<String> usedNameList = new ArrayList<String>(){{add("");}};
    private final static ArrayList<Integer> usedFreqList = new ArrayList<Integer>(){{add(0);}};
    private final String name;
    private ArrayList<Instruction> irs;
    private int irNum;
    /**
     * the function it belongs to
     */
    private final FunctionValue function;
    private final ArrayList<BasicBlockRef> pred;
    private boolean reachable = true;
    private boolean isEntryBlock = false;
    public BasicBlockRef(FunctionValue fv, String name) {
        this.function = fv;
        if (usedNameList.contains(name)) {
            int index = usedNameList.indexOf(name);
            this.name = name + usedFreqList.get(index);
            usedFreqList.set(index, usedFreqList.get(index) + 1);
        } else {
            this.name = name;
            usedFreqList.add(1);
            usedNameList.add(name);
        }
        irs = new ArrayList<>();
        irNum = 0;
        pred = new ArrayList<>();
    }

    public void setIsEntryBlock(boolean isEntryBlock) {
        this.isEntryBlock = isEntryBlock;
    }

    public void addPred(BasicBlockRef block) {
        pred.add(block);
    }

    public boolean hasPred() {
        return !pred.isEmpty();
    }

    public int getPredNum() {
        return pred.size();
    }

    public boolean isEntryBlock() {
        return isEntryBlock;
    }

    public BasicBlockRef getPred(int index) {
        return pred.get(index);
    }

    public void dropPred(BasicBlockRef p) {
        int index = pred.indexOf(p);
        if (index != -1) {
            pred.remove(index);
        }
    }
    public void mergeWith(BasicBlockRef block) {
        for (int i = 0; i < block.getIrNum(); i++) {
            irs.add(block.getIr(i));
        }
    }
    public void clearPred(){
        pred.clear();
    }

    public FunctionValue getFunction() {
        return function;
    }

    public void put(Instruction ir) {
        if (ir instanceof Allocate allocate) {
            function.emitAlloc((Allocate) ir);
            Pointer ptrType = (Pointer) allocate.getLVal().getType();
            if (!(ptrType.getBase() instanceof FloatType) && !(ptrType.getBase() instanceof IntType)) {
                function.emitAllocEntry(allocate);
            } else { // base type no need entry
                irs.add(0, ir);
                irNum++;
            }
        } else {
            irNum++;
            irs.add(ir);
        }
        ir.setBlock(this);
    }
    public void put(int index, Instruction ir) {
        irs.add(index, ir);
        ir.setBlock(this);
        irNum++;
    }

    public int getAllocSize() {
        int sz = 0;
        for (Instruction ir : irs) {
            if (ir instanceof Allocate) {
                sz++;
            } else {
                break;
            }
        }
        return sz;
    }

    public void renewIr(int index, Instruction ir) {
        irs.set(index, ir);
        ir.setBlock(this);
    }

    public ArrayList<Allocate> getAllocates() {
        return function.getAllocates();
    }

    public void putAllocAtEntry(Allocate allocate) {
        function.emitAllocEntry(allocate);
    }

    public String getName() {
        return name;
    }

    public int getIrNum() {
        return irs.size();
    }

    public Instruction getIr(int index) {
        return irs.get(index);
    }

    public void dropIr(Instruction ir) {
        irs.remove(ir);
        irNum--;
        if (ir instanceof Allocate) {
            function.dropAlloc((Allocate) ir);
        }
        if (ir instanceof Call call) {
            for (ValueRef vr : call.getRealParams()) {
                vr.getUser().remove(ir);
            }
        } else {
            for (int i = 0; i < ir.getNumberOfOperands(); i++) {
                ir.getOperand(i).getUser().remove(ir);
            }
        }
    }

    /***
     * delete all inst after br
     */
    public void modify() {
        int end = -1;
        for (int i = 0; i < irs.size(); i++) {
            Instruction ir = irs.get(i);
            if (ir instanceof Br || ir instanceof CondBr) {
                end = i;
                break;
            }
        }
        if (end != -1) {
            for (int i = end + 1; i < irNum; i++) {
                Instruction ir = irs.get(i);
                if (ir instanceof Br) {
                    BasicBlockRef bb = ((Br) ir).getTarget();
                    bb.dropPred(this);
                }
                if (ir instanceof CondBr) {
                    BasicBlockRef tar = ((CondBr) ir).getTrueBlock();
                    tar.dropPred(this);
                    tar = ((CondBr) ir).getFalseBlock();
                    tar.dropPred(this);
                }
            }
            irNum = end + 1;
            irs = new ArrayList<>(irs.subList(0, irNum));
        }
    }

    public List<Instruction> getIrs() {
        return irs;
    }

    public LocalVar createLocalVar(TypeRef type, String name) {
        return function.createLocalVar(type, name);
    }

    public void drop() {
        function.dropBlock(this);
    }

    public void dropDeadPred() {
        pred.removeIf(bb -> !bb.isReachable());
    }

    public void replaceIr(Instruction old, Instruction newIr) {
        int index = irs.indexOf(old);
        if (index != -1) {
            irs.set(index, newIr);
            newIr.setBlock(this);
        }
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public void addPhi(Phi phi) {
        irNum++;
        irs.add(0, phi);
        phi.setBlock(this);
    }

    public boolean contains(Instruction instruction) {
        return irs.contains(instruction);
    }

    public int getDirectSuccessorNum() {
        // if this is a pred block, its last inst must be a br/condBr
        int index = Util.findLastInstruction(this);
        Instruction inst = irs.get(index);
        if (inst instanceof CondBr) {
            return 2;
        } else {
            return 1;
        }
    }
}
