package cn.edu.nju.software.backendarm.regalloc;

import cn.edu.nju.software.backendarm.ArmInstrGenerator;
import cn.edu.nju.software.backendarm.ArmSpecifications;
import cn.edu.nju.software.backendarm.arminstruction.ArmMov;
import cn.edu.nju.software.backendarm.arminstruction.ArmVmov_f32;
import cn.edu.nju.software.backendarm.arminstruction.operand.ArmRegister;
import cn.edu.nju.software.backendarm.arminstruction.util.ArmComment;
import cn.edu.nju.software.ir.type.IntType;
import cn.edu.nju.software.ir.type.TypeRef;
import cn.edu.nju.software.ir.value.LocalVar;
import cn.edu.nju.software.ir.value.ValueRef;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArmTempVarLiveTable {
    /* RegName : VarName */
    private final HashMap<String, LocalVar> tempVar2Reg = new HashMap<>();

    private final ArmInstrGenerator generator;

    private final ArmAllocator allocator;

    public ArmTempVarLiveTable(ArmInstrGenerator generator, ArmAllocator allocator) {
        this.generator = generator;
        this.allocator = allocator;
        tempVar2Reg.putAll(Arrays.stream(ArmSpecifications.getTempVarRegs())
                .collect(Collectors.toMap(key -> key, value -> new LocalVar(new IntType(),"")))); //初始化状态所有的寄存器对应的变量名为空
    }

    public boolean isRecorded(ValueRef tempVar) {
        String regName = getReg(tempVar);
        return regName != null;
    }

    public boolean isUsed(String regName) {
        if(!tempVar2Reg.containsKey(regName)){
            return false;
        }
        return !tempVar2Reg.get(regName).getName().isEmpty();
    }

    /**
     * 依据要暂存的变量的类型，为其分配一个寄存器，如果没有没有这一类型的寄存器空闲，则spill一个寄存器并且返回,
     * 默认变量一开始存储在t0（ft0）中
     * @param tempVar
     * @return 记录该变量的寄存器
     */
    public void record(LocalVar tempVar) {
        TypeRef type = tempVar.getType();
        String regForRecord = getAnEmptyReg(type);
        String regToStage;
        if(regForRecord != null){
            tempVar2Reg.put(regForRecord, tempVar);
            regToStage = regForRecord;
        } else {
            String regToSpill = spillFor(tempVar);
            tempVar2Reg.put(regToSpill, tempVar);
            regToStage = regToSpill;
        }
        stage(tempVar, regToStage);
    }

    /**
     * 将tempVar的值（对应reg中）mv到暂存用的寄存器中
     * @param tempVar
     * @param regToStage
     */
    public void stage(ValueRef tempVar, String regToStage) {
        if(ArmSpecifications.isGeneralType(tempVar.getType())){
            generator.addInstruction(new ArmMov(new ArmRegister(regToStage), new ArmRegister("r0")));
        } else if(ArmSpecifications.isFloatType(tempVar.getType())){
            generator.addInstruction(new ArmVmov_f32(new ArmRegister(regToStage), new ArmRegister("s0"))); //使用t1// 防止破坏t0
        } else {
            assert false;
        }
    }


    /**
     * 为tempVar spill一个寄存器
     * @param tempVar
     * @return
     */
    private String spillFor(LocalVar tempVar){
        generator.addInstruction(new ArmComment("spill for " + tempVar.getName()));
        TypeRef type = tempVar.getType();
        String regToSpill = getAUsedReg(type);
        if(regToSpill != null){
            allocator.storeLocalVarIntoMemory(tempVar2Reg.get(regToSpill) , regToSpill);//将寄存器中的变量spill到内存中
            tempVar2Reg.put(regToSpill, new LocalVar(new IntType(),"")); //更新tempVar2Reg表
            return regToSpill; //如果有空闲寄存器，返回寄存
        }
        assert false;
        return null;
    }

    /**
     * 从tempVar2Reg中获取tempVar对应的寄存器
     * 对于temp变量，只会被定义一次，使用n次，fetch以后此表无需再记录此变量
     * @param tempVar
     * @return
     */
    public String fetch(ValueRef tempVar) {
        String regName = getReg(tempVar);
        if(regName != null){
            tempVar2Reg.put(regName, new LocalVar(new IntType(),""));
            return regName;
        }
        return null;
    }

    /**
     * 释放tempVar对应的寄存器
     * release once
     * @param tempVar
     */
    public void release(ValueRef tempVar){
        String regName = getReg(tempVar);
        if(regName == null){
            return;
        }
        clear(regName);

    }

    /**
     * 从tempVar2Reg中获取tempVar对应的寄存器
     * @param variable
     * @return
     */
    private String getReg(ValueRef variable) {
        Optional<String> regForRecord = tempVar2Reg.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(variable.getName()))
                .map(Map.Entry::getKey)
                .findFirst();
        return regForRecord.orElse(null);
    }

    /**
     * 依据类型获取一个空闲的寄存器（分为通用和浮点）
     * @param type
     * @return
     */
    private String getAnEmptyReg(TypeRef type) {
        Optional<String> regName = tempVar2Reg.entrySet().stream()
                .filter(entry -> {
                    if(ArmSpecifications.isGeneralType(type)){
                        return ArmSpecifications.isGeneralReg(entry.getKey());
                    } else if(ArmSpecifications.isFloatType(type)) {
                        return ArmSpecifications.isFloatReg(entry.getKey());
                    }
                    assert false;
                    return false;})
                .filter(entry -> entry.getValue().getName().isEmpty())
                .map(Map.Entry::getKey) //获取所有对应此tempVar的非空闲寄存器
                .findFirst();
        return regName.orElse(null);
    }

    /**
     * 依据类型获取一个正在使用的寄存器（分为通用和浮点）
     * @param type
     * @return
     */
    private String getAUsedReg(TypeRef type) {
        Optional<String> regName = tempVar2Reg.entrySet().stream()
                .filter(entry -> {
                    if(ArmSpecifications.isGeneralType(type)){
                        return ArmSpecifications.isGeneralReg(entry.getKey());
                    } else if(ArmSpecifications.isFloatType(type)) {
                        return ArmSpecifications.isFloatReg(entry.getKey());
                    }
                    assert false;
                    return false;})
                .filter(entry -> !entry.getValue().getName().isEmpty())
                .map(Map.Entry::getKey) //获取所有对应此tempVar的非空闲寄存器
                .findFirst();
        return regName.orElse(null);
    }

    /**
     * 清空寄存器,这个寄存器对应的变量不再被记录，可以被任意破坏，相当于被释放了
     * @param regName
     */
    private void clear(String regName){
        tempVar2Reg.put(regName, new LocalVar(new IntType(),""));
    }
}
