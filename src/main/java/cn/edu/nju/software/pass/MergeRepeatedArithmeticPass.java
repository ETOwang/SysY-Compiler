package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Binary;
import cn.edu.nju.software.ir.instruction.Cmp;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.arithmetic.Add;
import cn.edu.nju.software.ir.instruction.arithmetic.FAdd;
import cn.edu.nju.software.ir.instruction.arithmetic.FMul;
import cn.edu.nju.software.ir.instruction.arithmetic.Mul;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.ConstValue;
import cn.edu.nju.software.ir.value.FunctionValue;
import cn.edu.nju.software.ir.value.ValueRef;

public class MergeRepeatedArithmeticPass implements ModulePass {
    private static MergeRepeatedArithmeticPass instance = null;

    private MergeRepeatedArithmeticPass() {}
    public static MergeRepeatedArithmeticPass getInstance() {
        if (instance == null) {
            instance = new MergeRepeatedArithmeticPass();
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
            for (int j = 0; j < fv.getBlockNum(); j++) {
                BasicBlockRef bb = fv.getBlock(j);
                for (int k = 0; k < bb.getIrNum(); k++) {
                    Instruction inst = bb.getIr(k);
                    if (inst instanceof Binary binary) {
                        if (!(binary instanceof Cmp)) {
                            int index = binary.getOnlyConst();
                            if (index == -1) {
                                continue;
                            }
                            ValueRef lVal = binary.getLVal();
                            if (lVal.getUser().size() == 1) {
                                Instruction user = lVal.getUser().get(0);
                                if (binary.typeEquals(user)) {
                                    int index2 = ((Binary) user).getOnlyConst();
                                    if (index2 != -1) {
                                        ConstValue cv1 = (ConstValue) binary.getOperand(index), cv2 = (ConstValue) user.getOperand(index2);
                                        int srcIndex = 1 - index;
                                        ValueRef src = binary.getOperand(srcIndex);
                                        ConstValue cv = null;
                                        if (binary instanceof Add || binary instanceof FAdd) {
                                            cv = cv1.add(cv2);
                                        } else if (binary instanceof Mul || binary instanceof FMul) {
                                            cv = cv1.mul(cv2);
                                        }
                                        user.replace(cv2, cv);
                                        user.replace(lVal, src);
                                        bb.dropIr(binary);
                                        k--;
                                    }
                                }
                            } else if (lVal.getUser().isEmpty()){
                                bb.dropIr(inst);
                                k--;
                            }
                        }
                    }
                }
            }
        }
        return false;
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
