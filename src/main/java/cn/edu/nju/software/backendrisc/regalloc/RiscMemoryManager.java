package cn.edu.nju.software.backendrisc.regalloc;

import cn.edu.nju.software.ir.value.ValueRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RiscMemoryManager {

    private int size = 0;
    private final Map<String, Integer> memoryAddr = new HashMap<>();
    private final HashSet<String> hasAllocatedPtr = new HashSet<>();
    private static final RiscMemoryManager riscMemoryManager = new RiscMemoryManager();

    private RiscMemoryManager() {}

    public static RiscMemoryManager get() {
        return riscMemoryManager;
    }

    public void clear() {
        memoryAddr.clear();
        size = 0;
    }

    /**
     * which checks if var is in stack memory
     */
    public boolean checkInMem(ValueRef variable) {
        return memoryAddr.containsKey(variable.getName());
    }

    public int getOffset(ValueRef variable) {
        if (checkInMem(variable)) {
            return size - memoryAddr.get(variable.getName());
        }
        else {
            assert false;
            return -1;
        }
    }

    public void allocateInStack(ValueRef variable, int width) {
        if (checkInMem(variable)) {
            return;
        }
        size += width;
        memoryAddr.put(variable.getName(), size);
    }

    public void allocateInStack(int width) {
        size += width;
    }

    public int getSize() {
        return size;
    }

    public void align8byte() {
        size = (size + 7) / 8 * 8;
    }

    public void align16byte() {
        size = (size + 15) / 16 * 16;
    }

    public void addHasAllocatedPtr(String name) {
        hasAllocatedPtr.add(name);
    }

    public boolean checkPtrHasAllocated(String name) {
        return hasAllocatedPtr.contains(name);
    }
}
