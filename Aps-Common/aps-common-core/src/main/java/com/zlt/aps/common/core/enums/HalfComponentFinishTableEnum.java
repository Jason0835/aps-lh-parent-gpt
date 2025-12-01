package com.zlt.aps.common.core.enums;

import lombok.Getter;

/**
 * 半部件完成量表名枚举
 * @author Chen
 */
@Getter
public enum HalfComponentFinishTableEnum {

    /**
     * 胎面完成量表名
     */
    TM("T_TM_DAY_FINISH_QTY",
            "T_TM_DAY_FINISH_TOTAL",
            "TREAD_CODE", null,
            "DAY_FINISH_QTY", "NIGHT_FINISH_QTY",
            1),

    /**
     * 胎侧完成量表名
     */
    TC("T_TC_DAY_FINISH_QTY",
            "T_TC_DAY_FINISH_TOTAL",
            "SIDEWALL_CODE", null,
            "DAY_FINISH_QTY", "NIGHT_FINISH_QTY",
            2),

    /**
     * 内衬完成量表名
     */
    NC("T_NC_DAY_FINISH_QTY",
            "T_NC_DAY_FINISH_TOTAL",
            "LINING_CODE", null,
            "DAY_FINISH_QTY", "NIGHT_FINISH_QTY",
            3),

    /**
     * 钢丝圈完成量表名
     */
    GSQ("T_GSQ_DAY_FINISH_QTY",
            "T_GSQ_DAY_FINISH_TOTAL",
            "STEEL_RING_CODE", null,
            "MID_FINISH_QTY", "NIGHT_FINISH_QTY",
            4),

    /**
     * 胎圈完成量表名
     */
    TQ("T_TQ_DAY_FINISH_QTY",
            "T_TQ_DAY_FINISH_TOTAL",
            "BEAD_CODE", null,
            "MID_FINISH_QTY", "NIGHT_FINISH_QTY",
            5),

    /**
     * 胎圈完成量表名
     */
    CD15("T_CD15_DAY_FINISH_QTY",
            "T_CD15_DAY_FINISH_TOTAL",
            "STEEL_STRIP_CODE", null,
            "DAY_FINISH_QTY", "NIGHT_FINISH_QTY",
            6),

    /**
     * 胎圈完成量表名
     */
    CD90("T_CD90_DAY_FINISH_QTY",
            "T_CD90_DAY_FINISH_TOTAL",
            "CLOTH_CODE", null,
            "DAY_FINISH_QTY", "NIGHT_FINISH_QTY",
            7),

    /**
     * 钢带压延完成量表名
     */
    GDYY("T_GDYY_DAY_FINISH_QTY",
            "T_GDYY_DAY_FINISH_TOTAL",
            "BIG_ROLL_CODE", null,
            "CLASS1_FINISH_QTY", "CLASS2_FINISH_QTY",
            8),

    /**
     * 纤维压延完成量表名
     */
    XWYY("T_XWYY_DAY_FINISH_QTY",
            "T_XWYY_DAY_FINISH_TOTAL",
            "BIG_ROLL_CODE", null,
            "DAY_FINISH_QTY", "NIGHT_FINISH_QTY",
            9),
    ;

    /**
     * 日完成量表名
     */
    private final String finishQtyTableName;

    /**
     * 总完成量表名
     */
    private final String finishTotalTableName;

    /**
     * 半部件编号
     */
    private final Integer halfComponentCode;

    /**
     * 编号字段名
     */
    private final String codeColumnName;

    /**
     * 编号字段名1，没有就是空
     */
    private final String codeColumnName1;

    /**
     * 夜班计划完成量字段名
     */
    private final String class1PlanQtyColumnName;

    /**
     * 早班计划完成量字段名
     */
    private final String class2PlanQtyColumnName;

    HalfComponentFinishTableEnum(String finishQtyTableName, String finishTotalTableName,
                                 String codeColumnName, String codeColumnName1,
                                 String class1PlanQtyColumnName, String class2PlanQtyColumnName,
                                 Integer halfComponentCode) {
        this.finishQtyTableName = finishQtyTableName;
        this.finishTotalTableName = finishTotalTableName;
        this.codeColumnName = codeColumnName;
        this.codeColumnName1 = codeColumnName1;
        this.class1PlanQtyColumnName = class1PlanQtyColumnName;
        this.class2PlanQtyColumnName = class2PlanQtyColumnName;
        this.halfComponentCode = halfComponentCode;
    }

    /**
     * 根据半部件编号获取对应的表名枚举
     *
     * @param halfComponentCode 半部件编号
     * @return 结果
     */
    public static HalfComponentFinishTableEnum getEnumByHalfComponentCode(Integer halfComponentCode) {
        if (halfComponentCode == null) {
            return null;
        }
        for (HalfComponentFinishTableEnum enums : HalfComponentFinishTableEnum.values()) {
            if (enums.getHalfComponentCode().equals(halfComponentCode)) {
                return enums;
            }
        }
        return null;
    }
}
