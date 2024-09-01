package cn.edu.nju.software.pass;

import cn.edu.nju.software.frontend.util.CFG;
import cn.edu.nju.software.frontend.util.CG;
import cn.edu.nju.software.ir.basicblock.BasicBlockRef;
import cn.edu.nju.software.ir.instruction.Call;
import cn.edu.nju.software.ir.instruction.Instruction;
import cn.edu.nju.software.ir.module.ModuleRef;
import cn.edu.nju.software.ir.value.FunctionValue;

import java.util.HashMap;
import java.util.Map;

public class CFGBuildPass implements ModulePass{
    private static CFGBuildPass cfgBuildPass;
    private boolean dbgFlag=false;
    private final Map<FunctionValue, CFG> basicBlockCFG;
    private CG functionCG;
    private CFGBuildPass(){
        basicBlockCFG=new HashMap<>();
    }

    public static CFGBuildPass getInstance(){
        if(cfgBuildPass==null){
            cfgBuildPass=new CFGBuildPass();
        }
        return cfgBuildPass;
    }
    @Override
    public boolean runOnModule(ModuleRef module) {
        functionCG=buildFunctionCG(module);
        for (FunctionValue functionValue:module.getFunctions()) {
             basicBlockCFG.put(functionValue, buildBasicBlockCFG(functionValue));
        }
        if(dbgFlag){
            printDbgInfo();
        }
        return false;
    }

    @Override
    public void setDbgFlag() {
        dbgFlag=true;
    }

    @Override
    public void printDbgInfo() {
         dumpCG();
         for(FunctionValue functionValue:basicBlockCFG.keySet()){
             dumpCFG(functionValue);
         }
    }

    public void update(FunctionValue functionValue){
        basicBlockCFG.put(functionValue, buildBasicBlockCFG(functionValue));
        if(dbgFlag){
            printDbgInfo();
        }
    }
    @Override
    public String getName() {
        return "CFG Build Pass";
    }

    public CG getFunctionCG() {
        return functionCG;
    }
    public CFG getBasicBlockCFG(FunctionValue functionValue) {
        assert basicBlockCFG.containsKey(functionValue);
        return basicBlockCFG.get(functionValue);
    }
    private CFG buildBasicBlockCFG(FunctionValue functionValue) {
        CFG cfg=new CFG();
        for (BasicBlockRef basicBlockRef:functionValue.getBasicBlockRefs()){
            cfg.addPoint(basicBlockRef);
            for (int i = 0; i < basicBlockRef.getPredNum(); i++) {
                cfg.addEdge(basicBlockRef.getPred(i),basicBlockRef);
            }
        }
        return cfg;
    }
    private CG buildFunctionCG(ModuleRef moduleRef) {
        CG cg=new CG();
        for (FunctionValue functionValue:moduleRef.getFunctions()) {
            cg.addPoint(functionValue);
            for (BasicBlockRef basicBlockRef:functionValue.getBasicBlockRefs()){
                for (Instruction instruction:basicBlockRef.getIrs()){
                    if(instruction instanceof Call){
                        cg.addEdge(functionValue,((Call) instruction).getFunction());
                    }
                }
            }
        }
        return cg;
    }
    private void dumpCFG(FunctionValue functionValue) {
        CFG cfg=basicBlockCFG.get(functionValue);
        if(!cfg.isEmpty()){
            cfg.dumpWholeGraph(functionValue.getName());
        }
    }

    private void dumpCG() {
        if(!functionCG.isEmpty()){
            functionCG.dumpWholeGraph("functionCG");
        }
    }
}
