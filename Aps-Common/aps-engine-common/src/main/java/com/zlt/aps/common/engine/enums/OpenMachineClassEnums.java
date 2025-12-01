package com.zlt.aps.common.engine.enums;


import lombok.Getter;

/**
 * 机台开机班次
 */
@Getter
public enum OpenMachineClassEnums {

    CLASS_ONE(1,"前日早班"),CLASS_TWO(2,"夜班"),CLASS_THREE(3,"早班"),CLASS_FOUR(4,"次日夜班"),CLASS_FIVE(5,"次日早班");

    /**
     * 班次下标
     */
    private int classIndex;
    /**
     * 班次名称
     */
    private String className;

    private OpenMachineClassEnums(int classIndex,String className){
        this.classIndex=classIndex;
        this.className=className;
    }

    /**
     * 根据下标获取
     * @param classIndex
     * @return
     */
    public static OpenMachineClassEnums getClassEnums(int classIndex) {
        for (OpenMachineClassEnums cls : OpenMachineClassEnums.values()) {
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
    public OpenMachineClassEnums getPreviousClass() {
        OpenMachineClassEnums previousClass = this;
        if (this != OpenMachineClassEnums.CLASS_ONE) { // 取出上一班的班次
            previousClass = OpenMachineClassEnums.getClassEnums(this.classIndex - 1);
        }
        return previousClass;
    }

    /**
     * 获取下一个班次
     * @return
     */
    public OpenMachineClassEnums getNextClass() {
        return OpenMachineClassEnums.getClassEnums(this.classIndex + 1);
    }
}
