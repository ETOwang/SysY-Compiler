package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Call;
import cn.edu.nju.software.ir.instruction.GEP;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashMap;

public class GEPReductionPass implements ModulePass {
    private static GEPReductionPass instance = null;
    public static GEPReductionPass getInstance() {
        if (instance == null) {
            instance = new GEPReductionPass();
        }
        return instance;
    }

    private GEPReductionPass() {}

    private ModuleRef module;

    private HashMap<Util.GEPPair, ValueRef> gep2Pointer = new HashMap<>();
    @Override
    public boolean runOnModule(ModuleRef module) {
        this.module = module;
        procOnModule();
        return false;
    }

    private void procOnModule() {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            procOnFunction(fv);
        }
    }

    private void procOnFunction(FunctionValue fv) {
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            gep2Pointer = new HashMap<>();
            procOnBB(bb);
        }
    }

    private void procOnBB(BasicBlockRef bb) {
        for (int i = 0; i < bb.getIrNum(); i++) {
            Instruction inst = bb.getIr(i);
            if (inst instanceof GEP gep) {
                Util.GEPPair gepPair;
                if (gep.getNumberOfOperands() != 3) {
                    gepPair = new Util.GEPPair(gep.getOperand(0), gep.getOperand(1), new ConstValue(new IntType(), 0));
                } else {
                    gepPair = new Util.GEPPair(gep.getOperand(0), gep.getOperand(1), gep.getOperand(2));
                }
                Util.GEPPair tmp = containsKey(gep2Pointer, gepPair);
                if (tmp != null) {
                    ValueRef pointer = gep2Pointer.get(tmp);
                    ValueRef old = gep.getLVal();
                    for (Instruction user : old.getUser()) {
                        if (bb.equals(user.getBlock())){
                            // only eliminate for same block
                            if (user instanceof Call call) {
                                call.replaceRealParams(old, pointer);
                            } else {
                                user.replace(old, pointer);
                            }
                        }
                    }
                    bb.dropIr(inst);
                    i--;
                } else {
                    gep2Pointer.put(gepPair, gep.getLVal());
                }
            }
        }
    }

    private Util.GEPPair containsKey(HashMap<Util.GEPPair, ValueRef> map, Util.GEPPair gepPair) {
        for (Util.GEPPair gepPair1 : map.keySet()) {
            if (gepPair1.equals(gepPair)) {
                return gepPair1;
            }
        }
        return null;
    }
    @Override
    public String getName() {
        return "GepPass";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
