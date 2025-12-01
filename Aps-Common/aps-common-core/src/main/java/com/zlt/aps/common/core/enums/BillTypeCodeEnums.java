package com.zlt.aps.common.core.enums;

import com.ruoyi.common.utils.StringUtils;

/**
 * Copyright (c) 2024, All rights reserved。
 * 文件名称：BillTypeCodeEnums.java
 * 描    述：
 *
 * @author cxy
 * @version 1.0
 * @date 2024/11/21
 */
public enum BillTypeCodeEnums {


    /**
     * 胎面排程结果
     */
    TM_SCHEDULE_RESULT("TM001","胎面排程结果"),

    /**
     * 胎侧排程结果
     */
    TC_SCHEDULE_RESULT("TC001","胎侧排程结果"),

    /**
     * 内衬排程结果
     */
    NC_SCHEDULE_RESULT("NC001","内衬排程结果"),

    /**
     * 胎圈排程结果
     */
    TQ_SCHEDULE_RESULT("TQ001","胎圈排程结果"),

    /**
     * 钢丝圈排程结果
     */
    GSQ_SCHEDULE_RESULT("GSQ001","钢丝圈排程结果"),

    /**
     * 纤维压延排程结果
     */
    XWYY_SCHEDULE_RESULT("XWYY001","纤维压延排程结果"),

    /**
     * 钢带压延排程结果
     */
    GDYY_SCHEDULE_RESULT("GDYY001","钢带压延排程结果"),

    /**
     * 15度裁断排程结果
     */
    CD15_SCHEDULE_RESULT("CD15001","15度裁断排程结果"),

    /**
     * 90度裁断排程结果
     */
    CD90_SCHEDULE_RESULT("CD90001","90度裁断排程结果"),





    ;


    /**
     * 单据类型编码
     */
    private final String billTypeCode;

    /**
     * 单据类型名称
     */
    private final String billTypeName;

    private BillTypeCodeEnums(String billTypeCode, String billTypeName) {
        this.billTypeCode = billTypeCode;
        this.billTypeName = billTypeName;
    }

    public String getBillTypeCode() {
        return billTypeCode;
    }

    public String getBillTypeName() {
        return billTypeName;
    }

    /**
     * 根据单据编号获取对应的单据枚举
     *
     * @param billTypeCode 单据编号
     * @return 结果
     */
    public static BillTypeCodeEnums getBillTypeCodeByCode(String billTypeCode) {
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
