package com.zlt.aps.lh.api.enums;

import lombok.Getter;

/**
 * @author xh
 * @version 1.0
 * @Description 班制
 * @date 2025/2/20
 */
@Getter
public enum ShiftSystemEnum{

    //2班制 12小时
    SHIFT_SYSTEM_2("2班制", 2, new String[]{"class1PlanQty", "class2PlanQty", "class4PlanQty", "class5PlanQty"},
            new String[]{"class1StartTime", "class2StartTime", "class4StartTime", "class5StartTime"},
            new String[]{"class1EndTime", "class2EndTime", "class4EndTime", "class5EndTime"},
            new String[]{"class1Analysis", "class2Analysis", "class4Analysis", "class5Analysis"},
            new String[]{"class1Quota", "class2Quota", "class1Quota", "class2Quota"},
            new String[]{"class1MaintainTime", "class2MaintainTime", "class1MaintainTime", "class2MaintainTime"}),
    //3班制 8小时
    SHIFT_SYSTEM_3("3班制", 3, new String[]{"class1PlanQty", "class2PlanQty", "class3PlanQty", "class4PlanQty", "class5PlanQty", "class6PlanQty"},
            new String[]{"class1StartTime", "class2StartTime", "class3EndTime","class4StartTime", "class5StartTime", "class6StartTime"},
            new String[]{"class1EndTime", "class2EndTime", "class3EndTime", "class4EndTime", "class5EndTime", "class6EndTime"},
            new String[]{"class1Analysis", "class2Analysis","class3Analysis", "class4Analysis", "class5Analysis","class6Analysis"},
            new String[]{"class1Quota", "class2Quota","class3Quota", "class1Quota", "class2Quota","class3Quota"},
            new String[]{"class1MaintainTime", "class2MaintainTime", "class3MaintainTime","class1MaintainTime", "class2MaintainTime","class3MaintainTime",}),
    ;
    private Integer code;

    private String name;

    /**
     * 班次数量字段名称
     */
    private String[] classQtyFieldNames;

    /**
     * 班次开始时间字段名称
     */
    private String[] classStartTimeFieldNames;

    /**
     * 班次结束时间字段名称
     */
    private String[] classEndTimeFieldNames;

    /**
     * 班次定额字段名称
     */
    private String[] classQuotaFieldNames;

    /**
     * 班次维修字段名称
     */
    private String[] classMaintainFieldNames;

    /**
     * 班次原因分析字段名称
     */
    private String[] classAnalysisFieldNames;

    ShiftSystemEnum(String name, Integer code,String[] classQtyFieldNames,
                    String[] classStartTimeFieldNames,
                    String[] classEndTimeFieldNames,
                    String[] classAnalysisFieldNames,
                    String[] classQuotaFieldNames,
                    String[] classMaintainFieldNames) {
        this.code = code;
        this.name = name;
        this.classQtyFieldNames = classQtyFieldNames;
        this.classStartTimeFieldNames = classStartTimeFieldNames;
        this.classEndTimeFieldNames = classEndTimeFieldNames;
        this.classAnalysisFieldNames = classAnalysisFieldNames;
        this.classQuotaFieldNames = classQuotaFieldNames;
        this.classMaintainFieldNames = classMaintainFieldNames;
    }

    public static ShiftSystemEnum getByCode(Integer code){
        for (ShiftSystemEnum enums:values()){
            if (enums.getCode().equals(code)){
                return enums;
            }
        }
        return null;
    }
}
