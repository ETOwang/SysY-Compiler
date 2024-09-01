package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.generator.Generator;
import cn.edu.nju.software.ir.instruction.*;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.type.FloatType;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.type.Pointer;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.ArrayList;
import java.util.HashMap;

public class MemToReg implements ModulePass {
    private final EliminateConstExp eliminateConstExp;

    private static MemToReg memToRegPass = null;

    private ModuleRef module;

    private boolean changed = false;

    public final static ValueRef UNDEF = new ValueRef(new TypeRef(), "undef");

    Generator gen = Generator.getInstance();

    public static MemToReg getInstance() {
        if (memToRegPass == null) {
            memToRegPass = new MemToReg();
        }
        return memToRegPass;
    }

    /***
     * record each replaceable allocate inst in each block's latest definition(if using)
     */
    private HashMap<Allocate, HashMap<BasicBlockRef, ValueRef>> defineInBlock = new HashMap<>();
    /***
     * memory to alloc inst
     */
    private HashMap<ValueRef, Allocate> mem2Alloc = new HashMap<>();
    /***
     * phis tobe filled
     */
    private ArrayList<Phi> emptyPhis = new ArrayList<>();

    private MemToReg() {
        eliminateConstExp = new EliminateConstExp();
    }

    private void memToRegProc() {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            defineInBlock = new HashMap<>();
            mem2Alloc = new HashMap<>();
            emptyPhis = new ArrayList<>();
//            CFG cfg = cfgBuildPass.getBasicBlockCFG(fv);
            BasicBlockRef entry = fv.getEntryBlock();
            // init replaceable alloc inst
            getReplaceableAlloc(entry);

            // identify store and load
            replaceLoadStoreWthPhi(fv);

            fillEmptyPhis();

            rmRedundantAllocStoreLoadAndPhiInFunction(fv);
        }
    }

    /***
     * identify load and store, init to insert phi
     * @param fv: function
     */
    private void replaceLoadStoreWthPhi(FunctionValue fv) {
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction inst = bb.getIr(j);
                if (inst instanceof Store store) {
                    ValueRef storeVal = store.getOperand(0);
                    ValueRef mem = store.getOperand(1);
                    if (mem2Alloc.containsKey(mem)) { // memory is in replaceable alloc inst
                        HashMap<BasicBlockRef, ValueRef> tmp = defineInBlock.get(mem2Alloc.get(mem));
//                        System.err.println(mem + ": " +bb + " -- " + storeVal);
                        tmp.put(bb, storeVal); // renew the memory's value in specific block
                    }
                }
                if (inst instanceof Load load) {
//                    System.err.println("load: " + inst);
                    ValueRef mem = inst.getOperand(0);
                    if (mem2Alloc.containsKey(mem)) {
//                        System.err.println("tag::" + mem + " -- " + bb);
                        ValueRef latestVal = getLatestDefineForMem(bb, mem2Alloc.get(mem));
//                        if (latestVal == null) {
//                            latestVal = UNDEF;
//                        }
                        ValueRef old = load.getLVal();
                        for (Instruction user : old.getUser()) { // replace all old load usage with the new value
//                            System.err.println(user);
                            if (user instanceof Call call) {
                                call.replaceRealParams(old, latestVal);
                            } else {
                                user.replace(old, latestVal);
                            }
                        }
                    }
                }
//                if (sz != bb.getIrNum()) {
//                    j += bb.getIrNum() - sz;
//                }
            }
        }
    }

    /***
     * get the latest value in memory in specific block
     * @param block: the specific block
     * @param allocate: memory
     * @return
     */
    private ValueRef getLatestDefineForMem(BasicBlockRef block, Allocate allocate) {
        HashMap<BasicBlockRef, ValueRef> tmp = defineInBlock.get(allocate);
        if (tmp == null) {
            System.err.println("Caller error.");
            return null;
        }
        if (tmp.containsKey(block)) { // define in this lock
            return tmp.get(block);
        }
        if (block.contains(allocate)) { // declare in this block, but use before defining it, undef
            return null; // undef
        }
        // need an empty phi inst
        Phi emptyPhi = gen.buildEmptyPhiAfterInst(block, allocate, "phi");
        tmp.put(block, emptyPhi.getLVal());
        emptyPhis.add(emptyPhi);
        return emptyPhi.getLVal();
    }
    /**
     * param entry: entry basic block
     * */
    private void getReplaceableAlloc(BasicBlockRef entry) {
        for (Allocate allocate : entry.getAllocates()) {
            if (((Pointer)allocate.getLVal().getType()).getBase() instanceof IntType ||
                    ((Pointer)allocate.getLVal().getType()).getBase() instanceof FloatType) {
                defineInBlock.put(allocate, new HashMap<>());
                mem2Alloc.put(allocate.getLVal(), allocate);
            }
        }
    }

    private void fillEmptyPhis() {
        while (!emptyPhis.isEmpty()) {
            Phi phi = emptyPhis.remove(0);
            BasicBlockRef phiBlock = phi.getBlock();
            for (int i = 0; i < phiBlock.getPredNum(); i++) {
                BasicBlockRef pred = phiBlock.getPred(i);
                ValueRef val = getLatestDefineForMem(pred, phi.getMemory());
                if (val == null) {
                    // undef
                    val = UNDEF;
                }
                phi.add(val, pred);
            }
        }
    }

    private void rmRedundantAllocStoreLoadAndPhiInFunction(FunctionValue fv) {
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction inst = bb.getIr(j);
                if (inst instanceof Load load) {
                    if (mem2Alloc.containsKey(load.getOperand(0))) {
                        // replaceable
                        bb.dropIr(inst); // remember after dropping, j--
                        j--;
                    }
                }
                if (inst instanceof Store store) {
                    if (mem2Alloc.containsKey(store.getOperand(1))) {
                        bb.dropIr(inst);
                        j--;
                    }
                }
                if (inst instanceof Allocate allocate) {
                    if (defineInBlock.containsKey(allocate)) {
                        bb.dropIr(inst);
                        j--;
                    }
                }
            }
        }

    }

    private void modifyPhiOnModule() {
        EliminateRedundantPhi eliminateRedundantPhi = EliminateRedundantPhi.getInstance();
        eliminateRedundantPhi.runOnModule(module);
    }


    @Override
    public boolean runOnModule(ModuleRef module) {
        this.module = module;
        memToRegProc();
        changed = true;
        while (changed) {
            changed = false;
            modifyPhiOnModule();
            changed |= eliminateConstExp.runOnModule(this.module);
        }
        // reduce phi and drop phi's dead pred
        runPhiModifyPass();
        modifyPhiOnModule();
        eliminateConstExp.runOnModule(this.module);
        return false;
    }

    private void runPhiModifyPass() {
        PhiModify phiModify = PhiModify.getInstance();
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            phiModify.runOnFunction(fv);
        }
    }

    @Override
    public String getName() {
        return "MemToRegPass";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
