package com.zlt.aps.cx.engine.enums;


import lombok.Getter;

/**
 * 班次枚举类
 */
@Getter
public enum ClassEnums {

    CLASS_ONE(1,"中班"),CLASS_TWO(2,"夜班"),CLASS_THREE(3,"白班"),CLASS_FOUR(4,"次日一班"),CLASS_FIVE(5,"次日二班");

    /**
     * 班次下标
     */
    private int classIndex;
    /**
     * 班次名称
     */
    private String className;

    private ClassEnums(int classIndex,String className){
        this.classIndex=classIndex;
        this.className=className;
    }

    /**
     * 根据下标获取
     * @param classIndex
     * @return
     */
    public static ClassEnums getClassEnums(int classIndex) {
        for (ClassEnums cls : ClassEnums.values()) {
            if (cls.getClassIndex() == classIndex) {
                return cls;
            }
        }
        return null;
    }
}
