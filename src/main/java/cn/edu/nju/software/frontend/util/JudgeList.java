package cn.edu.nju.software.frontend.util;

import java.util.ArrayList;

public class JudgeList {
    ArrayList<LineAndType> lineAndType = new ArrayList<>();
    public void put(int lineNo, int type) {
        lineAndType.add(new LineAndType(lineNo, type));
    }
    public LineAndType find(int lineNO) {
        for (LineAndType item : lineAndType) {
            if (item.lineNo == lineNO)
                return item;
        }
        return null;
    }
}
