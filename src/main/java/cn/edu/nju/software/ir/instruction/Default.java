package cn.edu.nju.software.ir.instruction;
import cn.edu.nju.software.ir.generator.InstructionVisitor;


public class Default extends Instruction{

    @Override
    public String toString() {
        return "";
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equivalent(Instruction rhs) {
        return super.equivalent(rhs);
    }
}
