package com.zlt.aps.mdm.enums;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.enums.BillTypeCodeEnums;
import lombok.Getter;

/**
 * 工作日历权限对应字典编码枚举
 * @author Chen
 * @since 2026/3/12
 */
@Getter
public enum WorkCalendarPermiEnum {

    /**
     * 月计划
     */
    MONTH("maindata:mdmWorkCalendar:monthplan","01"),

    /**
     * 硫化
     */
    LH("maindata:mdmWorkCalendar:lh","02"),

    /**
     * 成型
     */
    CX("maindata:mdmWorkCalendar:cx","03"),

    /**
     * 胎面
     */
    TM("maindata:mdmWorkCalendar:tm","04"),

    /**
     * 胎侧
     */
    TC("maindata:mdmWorkCalendar:tc","05"),

    /**
     * 内衬
     */
    NC("maindata:mdmWorkCalendar:nc","06"),

    /**
     * 垫胶
     */
    DJ("maindata:mdmWorkCalendar:dj","07"),

    /**
     * 胎圈
     */
    TQ("maindata:mdmWorkCalendar:tq","08"),

    /**
     * 钢丝圈
     */
    GSQ("maindata:mdmWorkCalendar:gsq","09"),

    /**
     * 斜裁
     */
    XC("maindata:mdmWorkCalendar:xc","10"),

    /**
     * 直裁
     */
    ZC("maindata:mdmWorkCalendar:zc","11"),

    /**
     * 压延
     */
    YY("maindata:mdmWorkCalendar:yy","12"),

    /**
     * 零度
     */
    ZERO("maindata:mdmWorkCalendar:zero","15"),

    /**
     * 密炼
     */
    MIX("maindata:mdmWorkCalendar:mix","16"),

    ;

    WorkCalendarPermiEnum(String perms, String dictValue) {
        this.perms = perms;
        this.dictValue = dictValue;
    }

    private final String dictValue;

    private final String perms;

    /**
     * 根据权限字符获取对应的字典值
     *
     * @param billTypeCode 单据编号
     * @return 结果
     */
    public static BillTypeCodeEnums getByPerms(String billTypeCode) {
        if (StringUtils.isEmpty(billTypeCode)) {
            return null;
        }
        for (BillTypeCodeEnums enums : BillTypeCodeEnums.values()) {
            if (enums.getBillTypeCode().equals(billTypeCode)) {
                return enums;
            }
        }
        return null;
    }
}
