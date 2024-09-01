package cn.edu.nju.software.frontend.util;

import java.util.ArrayList;

/**
 * T & T2 same as Symbol
 * T: self value
 * T2: key
 * */
public class SymbolTable<T> {
    private final ArrayList<Symbol<T>> table;

    public SymbolTable() {
        table = new ArrayList<>();
    }

    public void put(Symbol<T> symbol) {
        table.add(symbol);
    }

    public T find(String name) {
        for (Symbol<T> symbol : table) {
            if (symbol.isValid() && name.equals(symbol.getName())) {
                return symbol.getValue();
            }
        }
        return null;
    }

    public void setInvalid(String name) {
        for (Symbol<T> symbol : table) {
            if (name.equals(symbol.getName())) {
                symbol.setValid(false);
            }
        }
    }
}
