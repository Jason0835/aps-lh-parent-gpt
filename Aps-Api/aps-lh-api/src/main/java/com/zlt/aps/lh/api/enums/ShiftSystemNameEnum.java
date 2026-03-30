package com.zlt.aps.lh.api.enums;

import lombok.Getter;

/**
 * @author xh
 * @version 1.0
 * @Description 班次名称
 * @date 2025/2/20
 */
@Getter
public enum ShiftSystemNameEnum implements PublicEnum{

    //一班
    SHIFT_SYSTEM_CLASS_1("一班", "1"),
    //二班
    SHIFT_SYSTEM_CLASS_2("二班", "2"),
    //三班
    SHIFT_SYSTEM_CLASS_3("三班", "3"),
    //次日一班
    SHIFT_SYSTEM_CLASS_4("次日一班", "4"),
    //次日二班
    SHIFT_SYSTEM_CLASS_5("次日二班", "5"),
    //次日三班
    SHIFT_SYSTEM_CLASS_6("次日三班", "6"),
    ;
    private String code;

    private String name;

    ShiftSystemNameEnum(String name, String code) {
        this.code = code;
        this.name = name;
    }
}
