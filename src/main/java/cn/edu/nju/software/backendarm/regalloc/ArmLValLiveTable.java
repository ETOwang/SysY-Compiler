package cn.edu.nju.software.backendarm.regalloc;

import cn.edu.nju.software.ir.value.ValueRef;

public class ArmLValLiveTable {

    ValueRef lastLVal;

    public ArmLValLiveTable() {
        this.lastLVal = null;
    }

    public void setLastLVal(ValueRef lastLVal) {
        this.lastLVal = lastLVal;
    }

    public void resetLastLVal() {
        this.lastLVal = null;
    }

    public boolean isLastLVal(ValueRef lval) {
        if(this.lastLVal == null) return false;
        return this.lastLVal.getName().equals(lval.getName());
    }
}
