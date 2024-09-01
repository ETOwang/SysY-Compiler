package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.ArrayList;
import java.util.HashMap;

import static cn.edu.nju.software.pass.MemToReg.UNDEF;

/***
 * eliminate phi inst
 */
public class RegToMem implements ModulePass {
    private ModuleRef module;

    private static RegToMem regToMemPass = null;
    /***
     * record each phi's merging value in each pred block
     */
    private final HashMap<Phi, HashMap<BasicBlockRef, ValueRef>> phi2EachPredBlockValue;

    private RegToMem() {
        phi2EachPredBlockValue = new HashMap<>();
    }

    public static RegToMem getInstance() {
        if (regToMemPass == null) {
            regToMemPass = new RegToMem();
        }
        return regToMemPass;
    }

    private void eliminatePhi() {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            eliminatePhiForFunction(fv);
            transferMove2AllocStoreLoad(fv);
        }
    }

    private void eliminatePhiForFunction(FunctionValue function) {
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            eliminatePhiForBlock(bb);
        }
    }

    private void eliminatePhiForBlock(BasicBlockRef block) {
        HashMap<BasicBlockRef, BasicBlockRef> pred2Middle = new HashMap<>(); // record pair(pred, cur) to mid
        // block pass identify all phis and initialize
        ArrayList<BasicBlockRef> pres = new ArrayList<>(); // buffer for this block's pred
        // because inserting mid_block will change pred, but we need the old pred
        for (int j = 0; j < block.getPredNum(); j++) {
            pres.add(block.getPred(j));
        }
        for (int i = 0; i < block.getIrNum(); i++) {
            Instruction inst = block.getIr(i);
            if (inst instanceof Phi phi) { // this phi inst is in BBlock block, so its src in block's pred
                phi2EachPredBlockValue.put(phi, new HashMap<>());
                HashMap<BasicBlockRef, ValueRef> tmp = phi2EachPredBlockValue.get(phi);
                // block's all pred also stay in phi inst
                for (BasicBlockRef pred : pres) {
                    ValueRef vr = phi.getValueByBlock(pred);
                    if (vr.equals(UNDEF)) {
                        continue; // undef drop it
                    }
                    if (pred.getDirectSuccessorNum() == 1) {
                        tmp.put(pred, vr); // phi inst in this pred block has value phi.get...
                    } else {
                        BasicBlockRef midBlock;
                        if (pred2Middle.containsKey(pred)) {
                            midBlock = pred2Middle.get(pred);
                        } else {
                            midBlock = insertNewBasicBlockBetween(pred, block);
                            pred2Middle.put(pred, midBlock);
                        }
                        tmp.put(midBlock, vr);
                    }
                }
                emitMoveForPhi(phi);
                block.dropIr(inst);
                i--;
            } else {
                break;
            }
        }
    }

    private BasicBlockRef insertNewBasicBlockBetween(BasicBlockRef start, BasicBlockRef end) {
        FunctionValue fv = start.getFunction();
        BasicBlockRef mid = new BasicBlockRef(fv, "mid_");
        fv.appendBasicBlock(mid);

        // modify start, mid and end
        int finalIndex = Util.findLastInstruction(start);
        Instruction inst = start.getIr(finalIndex); // last inst
        inst.replace(end, mid); // replace start's target, start jumps to mid
        mid.addPred(start);
        Br br = new Br(end); // br %end
        mid.put(br); // mid jumps to end
        // replace end's pred with mid
        end.dropPred(start);
        end.addPred(mid);

        return mid;
    }

    /***
     * emit move for phi
     * @param phi: as above saying
     */
    private void emitMoveForPhi(Phi phi) {
        HashMap<BasicBlockRef, ValueRef> tmp = phi2EachPredBlockValue.get(phi);
        if (tmp == null) {
            System.err.println("Error in RegToMemPass.");
            return;
        }
        for (BasicBlockRef bb : tmp.keySet()) {
            ValueRef src = tmp.get(bb);
            ValueRef target = phi.getLVal();
            Move move = new Move(target, src);
            move.setMemory(phi.getMemory());
            int finalIndex = Util.findLastInstruction(bb);
            bb.put(finalIndex, move); // insert into the one before last
        }
    }

    /***
     * transform emitted move inst to alloc, load, store <br>
     * llvm has no move
     * @param function: pass on function
     */
    private void transferMove2AllocStoreLoad(FunctionValue function) {
        ArrayList<Allocate> allocMemory = new ArrayList<>(); // move's lVal to alloc memory
        HashMap<ValueRef, ValueRef> movTar2Memory = new HashMap<>(); // record move lVal to memory
//        ArrayList<ValueRef> initList = new ArrayList<>(); // record memory's init value, do not need a load when using
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                // first block pass replace move
                int sz = bb.getIrNum();
                Instruction inst = bb.getIr(j);
                if (inst instanceof Move move) {
                    Allocate allocate = move.getMemory();
                    if (!allocMemory.contains(allocate)) {
                        allocMemory.add(allocate);
                        bb.putAllocAtEntry(allocate);
//                        if (sz != bb.getIrNum()) {
//                            j += bb.getIrNum() - sz;
//                        }
                    }
                    ValueRef src = move.getSrc(); // move value to memory
                    ValueRef target = move.getLVal();
                    movTar2Memory.put(target, allocate.getLVal());
                    // create store
                    Store store = new Store(src, allocate.getLVal());
                    bb.replaceIr(inst, store);
                }
            }
        }
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            // second block pass insert load inst
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction inst = bb.getIr(j);
                if (inst instanceof Call call) {
                    for (ValueRef op : call.getRealParams()) {
                        if (op instanceof ConstValue) { // constant do not replace
                            continue;
                        }
                        if (movTar2Memory.containsKey(op)) {
                            LocalVar ldVal = bb.createLocalVar(op.getType(), "ld_phi");
                            Load load = new Load(ldVal, movTar2Memory.get(op));
                            call.replaceRealParams(op, ldVal);
                            // insert load
                            bb.put(j, load);
                            j++;
                        }
                    }
                } else {
                    for (int k = 0; k < inst.getNumberOfOperands(); k++) {
                        ValueRef op = inst.getOperand(k);
                        if (op instanceof ConstValue) {
                            continue;
                        }
                        if (movTar2Memory.containsKey(op)) {
                            LocalVar ldVal = bb.createLocalVar(op.getType(), "ld_phi");
                            Load load = new Load(ldVal, movTar2Memory.get(op));
                            inst.replace(op, ldVal);
                            // insert load
                            bb.put(j, load);
                            j++;
                        }
                    }
                }
            }
        }
    }
    @Override
    public boolean runOnModule(ModuleRef module) {
        this.module = module;
        eliminatePhi();
        return false;
    }

    @Override
    public String getName() {
        return "RegToMemPass";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
