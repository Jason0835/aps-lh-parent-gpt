package com.zlt.aps.common.engine.enums;


import com.google.common.base.Objects;

import lombok.Getter;

/**
 * 三班制班次枚举
 */
@Getter
public enum ClassNumThreePlanEnums {
    // 夜班
    CLASS_NIGHT("01","ui.data.column.scheduleResult.class.night", "02"),
    // 早班
    CLASS_MORNING("02","ui.data.column.scheduleResult.class.morning", "03"),
    // 中班
    CLASS_DAY("03","ui.data.column.scheduleResult.class.day", "01");
    
    /**
     * 班次编码
     */
    private String classIndex;
    /**
     * 班次名称，国际化标志
     */
    private String className;
    /**
     * 下一个班次编码
     */
    private String nextClass;

    private ClassNumThreePlanEnums(String classIndex,String className, String nextClass){
        this.classIndex=classIndex;
        this.className=className;
        this.nextClass = nextClass;
    }

    /**
     * 根据下标获取
     * @param classIndex
     * @return
     */
    public static ClassNumThreePlanEnums getClassEnums(String classIndex) {
        for (ClassNumThreePlanEnums cls : ClassNumThreePlanEnums.values()) {
            if (Objects.equal(cls.getClassIndex(), classIndex)) {
                return cls;
            }
        }
        return null;
    }
    
    /**
     * 获取上一个班次
     * @return
     */
    public ClassNumThreePlanEnums getPreviousClass() {
        for (ClassNumThreePlanEnums cls : ClassNumThreePlanEnums.values()) {
            if (Objects.equal(cls.getNextClass(), classIndex)) {
                return cls;
            }
        }
        // 否则返回本班次
        return this;
    }

    /**
     * 获取下一个班次
     * @return
     */
    public ClassNumThreePlanEnums getNextClass() {
        // 直接返回一个下一个班次
        return ClassNumThreePlanEnums.getClassEnums(this.nextClass);
    }
}
