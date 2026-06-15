package com.zlt.aps.common.engine.enums;


import lombok.Getter;

/**
 * 班次
 */
@Getter
public enum WorkClassEnums {
    // 夜班
    CLASS_NIGHT(1,"ui.data.column.scheduleResult.class.night"),
    // 早班
    CLASS_MORNING(2,"ui.data.column.scheduleResult.class.morning"),
    // 中班
    CLASS_DAY(3,"ui.data.column.scheduleResult.class.day");
    /**
     * 班次下标
     */
    private int classIndex;
    /**
     * 班次名称，国际化标志
     */
    private String className;

    private WorkClassEnums(int classIndex,String className){
        this.classIndex=classIndex;
        this.className=className;
    }

    /**
     * 根据下标获取
     * @param classIndex
     * @return
     */
    public static WorkClassEnums getClassEnums(int classIndex) {
        for (WorkClassEnums cls : WorkClassEnums.values()) {
            if (cls.getClassIndex() == classIndex) {
                return cls;
            }
        }
        return null;
    }
    
    /**
     * 获取上一个班次
     * @return
     */
    public WorkClassEnums getPreviousClass() {
        if (this.classIndex == 1) { // 如果是第一个班，则直接返回最后一个班
            return WorkClassEnums.getClassEnums(WorkClassEnums.values().length);
        }
        // 否则返回上一个下标的班次
        return WorkClassEnums.getClassEnums(this.classIndex - 1);
    }

    /**
     * 获取下一个班次
     * @return
     */
    public WorkClassEnums getNextClass() {
        // 如果是最后一个班，则直接返回第一个班
        if (this.classIndex == WorkClassEnums.values().length) {
            return WorkClassEnums.getClassEnums(1);
        }
        // 否则返回下一个下标的班次
        return WorkClassEnums.getClassEnums(this.classIndex + 1);
    }
}
