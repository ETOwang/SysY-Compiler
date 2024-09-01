package cn.edu.nju.software.pass;

import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Default;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.instruction.Phi;
import cn.edu.nju.software.ir.value.FunctionValue;

/***
 * when deleting dead blocks, remove phi inst pred block by this pass <br>
 * function pass
 */
public class PhiModify implements FunctionPass {
    private static PhiModify instance = null;
    private PhiModify(){}

    private FunctionValue function;
    private boolean changed = false;

    public static PhiModify getInstance(){
        if (instance == null) {
            instance = new PhiModify();
        }
        return instance;
    }

    @Override
    public boolean runOnFunction(FunctionValue function) {
        this.function = function;
//        procOnFunction();
        reducePhiNumber();
        while (changed) {
            changed = false;
            reducePhiNumber();
        }
        return false;
    }

    /***
     * for %0 = phi [%0, %10], [%1, %11]
     */
    private void reducePhiNumber() {
        for (int i = 0; i < function.getBlockNum(); i++) {
            BasicBlockRef bb = function.getBlock(i);
            for (int j = 0; j < bb.getIrNum(); j++) {
                Instruction inst = bb.getIr(j);
                if (inst instanceof Default) {
                    continue;
                }
                if (inst instanceof Phi phi) {
                    if (phi.specialModify()) {
                        changed = true;
                        bb.dropIr(inst);
                        j--;
                    }
                } else {
                    break;
                }
            }
        }
    }

//    private void procOnFunction() {
//        for (int i = 0; i < function.getBlockNum(); i++) {
//            BasicBlockRef bb = function.getBlock(i);
//            for (int j = 0; j < bb.getIrNum(); j++) {
//                Instruction inst = bb.getIr(j);
//                if (inst instanceof Phi phi) {
//                    for (int k = 0; k < phi.getPredSize(); k++) { // check pred block if existing
//                        BasicBlockRef pred = phi.getPredBlock(k);
//                        if (!function.containsBlock(pred)) {
//                            phi.dropBlock(pred);
//                        }
//                    }
//                    // TODO: the phi inst may be redundant
//                } else {
//                    break;
//                }
//            }
//        }
//    }

    @Override
    public String getName() {
        return "PhiModify";
    }

    @Override
    public void printDbgInfo() {

    }

    @Override
    public void setDbgFlag() {

    }
}
