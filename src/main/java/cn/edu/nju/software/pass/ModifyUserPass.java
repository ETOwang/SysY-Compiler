package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Call;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.arithmetic.Mod;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class ModifyUserPass implements ModulePass {
    private static ModifyUserPass instance;
    private ModifyUserPass() {}

    public static ModifyUserPass getInstance() {
        if (instance == null) {
            instance = new ModifyUserPass();
        }
        return instance;
    }
    @Override
    public boolean runOnModule(ModuleRef module) {
        for (int i = 0; i < module.getFunctionNum(); i++) {
            FunctionValue fv = module.getFunction(i);
            if (fv.isLib()) {
                continue;
            }
            clearUser(fv);
            addUser(fv);
        }
        return false;
    }

    private void clearUser(FunctionValue fv) {
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction instruction = bb.getIr(j);
                if (instruction instanceof Call call) {
                    for (ValueRef vr : call.getRealParams()) {
                        vr.clearUsers();
                    }
                } else {
                    int opNum = instruction.getNumberOfOperands();
                    for (int k = 0; k < opNum; k++) {
                        ValueRef vr = instruction.getOperand(k);
                        vr.clearUsers();
                    }
                }
            }
        }
    }

    private void addUser(FunctionValue fv) {
        for (int i = 0; i < fv.getBlockNum(); i++) {
            BasicBlockRef bb = fv.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction instruction = bb.getIr(j);
                if (instruction instanceof Call call) {
                    for (ValueRef vr : call.getRealParams()) {
                        vr.addUser(instruction);
                    }
                } else {
                    int opNum = instruction.getNumberOfOperands();
                    for (int k = 0; k < opNum; k++) {
                        ValueRef vr = instruction.getOperand(k);
                        vr.addUser(instruction);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
