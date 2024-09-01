package cn.edu.nju.software.frontend.llvm;

import cn.edu.nju.software.frontend.util.SymbolTable;
import cn.edu.nju.software.ir.value.ValueRef;

import java.util.ArrayList;

public class LLVMStack {
    private final ArrayList<SymbolTable<ValueRef>> stack = new ArrayList<>();
    public int size() {
        return stack.size();
    }
    public void push(SymbolTable<ValueRef> scope) {
        stack.add(scope);
    }
    public SymbolTable<ValueRef> peek() {
        return stack.get(size() - 1);
    }
    public SymbolTable<ValueRef> pop() {
        SymbolTable<ValueRef> ret = peek();
        stack.remove(size() - 1);
        return ret;
    }
    public ValueRef find(String name) {
        for (int i = size() - 1; i >= 0; i--) {
            SymbolTable<ValueRef> cur = stack.get(i);
            if (cur.find(name) != null) {
                return cur.find(name);
            }
        }
        return null;
    }
}
