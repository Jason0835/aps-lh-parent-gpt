package com.zlt.aps.lh.engine.enums;

import lombok.Getter;

/**
 * 硫化各个班次枚举
 */
@Getter
public enum LhClassShiftEnum {
    ONE_CLASS_SHIFT(1,"中班"),TWO_CLASS_SHIFT(2,"夜班"),THREE_CLASS_SHIFT(3,"白班");
    private Integer classIndex;
    private String className;

    private LhClassShiftEnum(Integer classIndex, String className){
        this.classIndex=classIndex;
        this.className=className;
    }

    /**
     * 根据下标获取
     * @param classIndex
     * @return
     */
    public static LhClassShiftEnum getClassShiftByClassIndex(Integer classIndex) {

        if(classIndex==null){
            return null;
        }
        for (LhClassShiftEnum classShift : LhClassShiftEnum.values()) {
            if (classShift.getClassIndex().equals(classIndex)) {
                return classShift;
            }
        }
        return null;
    }
}
