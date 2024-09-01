//package cn.edu.nju.software.ir.opt;
//
//import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
//import cn.edu.nju.software.ir.module.ModuleRef;
//import cn.edu.nju.software.ir.value.FunctionValue;
//
//public class Optimizer {
//    // mainly eliminate useless br label
//    private final ModuleRef module;
//    public Optimizer(ModuleRef module) {
//        this.module = module;
//    }
//
//    private void removeUselessBlock() {
//        int ftNum = module.getFunctionNum();
//        for (int i = 0; i < ftNum; i++) {
//            FunctionValue fv = module.getFunction(i);
//            for (int j = 0; j < fv.getBlockNum(); j++) {
//                BasicBlockRef block = fv.getBlock(j); // function's j-th block
//                if (IsAUselessBlock(block)) { // if it is a useless block
//                    if (!block.hasPred()) {
//                        // block is inaccessible, delete it directly
//                        fv.dropBlock(block);
//                        j--;
//                        continue;
//                    }
//                    for (int k = 0; k < block.getPredNum(); k++){
//                        BasicBlockRef pred = block.getPred(k); // get its pred block
//                        // prepare to replace the jump target label in its pred block
//                        int index = indexOfIrWithLabel(pred, block.getName());
//                        if (index == -1) {
//                            continue; // reasonably impossible to be -1, but for safety I judge it
//                        }
//                        String newLabel = block.getIr(0).substring(10); // the index just after %
//                        String oldLabel = block.getName();
//                        replaceBrLabel(pred, index, oldLabel, newLabel);
//                        simplifyBrIrs(pred, index, pred.getIr(index)); // do possible simplification
//                    }
//                    fv.dropBlock(block);
//                    j--;
//                }
//            }
//        }
//    }
//
//    private void simplifyBrIrs(BasicBlockRef block, int index, String brIr) {
//        // optimize ir like "br i1 %cond, label %next, label %next" to "br label %next"
//        if (numberOfLabels(brIr) == 2) {
//            String sameLabel = irHasSameLabel(brIr);
//            if (sameLabel != null) {
//                brIr = "br label " + sameLabel;
//                block.renewIr(index, brIr);
//            }
//        }
//    }
//
//    public void optimize() {
//        removeUselessBlock();
//    }
//
//    private boolean IsAUselessBlock(BasicBlockRef block) {
//        return (block.getIrNum() == 0 && !block.hasPred()) || // an empty block and inaccessible
//                (block.getIr(0).startsWith("br") && numberOfLabels(block.getIr(0)) == 1); // a block with only an unconditional jump
//    }
//
//    /**
//     * find the position of ir with certain label in a basic block
//     * */
//    private int indexOfIrWithLabel(BasicBlockRef block, String label) {
//        for (int i = 0; i < block.getIrNum(); i++) {
//            String ir = block.getIr(i);
//            if (!ir.startsWith("br")) {
//                continue;
//            }
//            if (ir.contains(label)) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    private void replaceBrLabel(BasicBlockRef block, int index, String oldLabel, String newLabel) {
//        String ir = block.getIr(index);
//        ir = ir.replace(oldLabel, newLabel);
//        block.renewIr(index, ir);
//    }
//
//    private int numberOfLabels(String brIr) {
//        int cnt = 0;
//        int index = brIr.indexOf("label");
//        while (index != -1) {
//            cnt++;
//            index = brIr.indexOf("label", index + 1);
//        }
//        return cnt;
//    }
//
//    private String irHasSameLabel(String brIr) {
//        // if same, return the same label, else return null
//        // tip: here consider the label starts with the '%'
//        if (numberOfLabels(brIr) != 2) {
//            return null;
//        }
//        int indexOfLocalTag = brIr.indexOf("%"), indexOfDelimiter = brIr.indexOf(","); // two index change synchronously to locate label
//        indexOfLocalTag = brIr.indexOf("%", indexOfLocalTag + 1);
//        indexOfDelimiter = brIr.indexOf(",", indexOfDelimiter + 1); // position of first label's '%' and ','
//        String firstLabel = brIr.substring(indexOfLocalTag, indexOfDelimiter);
//        indexOfLocalTag = brIr.indexOf("%", indexOfLocalTag + 1); // position of second label's '%'
//        String secondLabel = brIr.substring(indexOfLocalTag);
//        if (firstLabel.equals(secondLabel)) {
//            return firstLabel;
//        }
//        return null;
//    }
//}
package cn.edu.nju.software.ir.opt;

import cn.edu.nju.software.ir.module.ModuleRef;

public class Optimizer {
    private final ModuleRef module;
    public Optimizer(ModuleRef module) {
        this.module = module;
    }

    public void rmKillExp() {

    }
}