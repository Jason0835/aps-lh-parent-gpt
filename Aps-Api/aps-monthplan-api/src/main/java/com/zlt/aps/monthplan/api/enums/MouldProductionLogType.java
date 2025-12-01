package com.zlt.aps.monthplan.api.enums;

import java.util.Arrays;

/**
 * 分厂模具排程日志类型枚举定义类
 *
 * @author ZLT
 * 20250317
 */
public enum MouldProductionLogType {
    /**
     * 1 初始化
     */
    INIT_LOG(1, "初始化"),
    /**
     * 2 分组排序
     */
    GROUP_SORT_LOG(2, "分组排序"),
    /**
     * 3 单计划排产
     */
    SINGLE_PLAN_PRODUCTION_LOG(3, "单计划排产"),
    /**
     * 4 单计划有交期排产
     */
    SINGLE_PLAN_DELIVERY_LOG(4, "单计划有交期排产"),
    /**
     * 5 单计划通用排产
     */
    SINGLE_PLAN_GENERAL_LOG(5, "单计划通用排产"),
    /**
     * 6 同模具有交期排产
     */
    SAME_MOULD_DELIVERY_LOG(6, "同模具有交期排产"),
    /**
     * 7 同模具通用排产
     */
    SAME_MOULD_GENERAL_LOG(7, "同模具通用排产"),
    /**
     * 8 同规格排产
     */
    SAME_PRODUCT_LOG(8, "同规格排产"),
    /**
     * 9 模具排产开始
     */
    MOULD_INIT(9, "模具排产开始"),
    /**
     * 10 一键排产开始
     */
    WHOLE_PRODUCTION(10, "一键排产开始"),
    /**
     * 11 分组排产
     */
    GROUP_PRODUCTION_LOG(11, "分组排产"),
    /**
     * 12 计划调整
     */
    PLAN_ADJUST_LOG(12, "计划调整"),
    /**
     * 13 续作规格使用续作模具排产
     */
    CONTINUE_MOULD_GENERAL_LOG(13, "续作模具通用排产"),
    /**
     * 14 共用生胎排产
     */
    SAME_CONSTRUCTION_LOG(14, "共用生胎排产"),
    /**
     * 15 拼模排产
     */
    ASSEMBLING_MOULD_LOG(15, "拼模排产"),
    /**
     * 16 同寸口排产
     */
    SAME_PRO_SIZE_LOG(16, "同寸口排产"),
    /**
     * 17 跨组同寸口排产
     */
    CROSS_SAME_PRO_SIZE_LOG(17, "跨组同寸口排产"),
    /**
     * 18 一键排产开始
     */
    DAY_LEFT_OVER_LOG(18, "日剩余产能计算"),
    /**
     * 19 查找排产衔接分组
     */
    FIND_PRODUCTION_GROUP_LOG(19, "查找排产衔接分组"),
    /**
     * 20 创建续作排产分组
     */
    BUILD_CONTINUE_PRODUCTION_GROUP_LOG(20, "创建续作排产分组"),
    /**
     * 21 排产分组时间不一致处理
     */
    DIFF_DATE_PRODUCTION_GROUP_LOG(21, "排产分组时间不一致处理"),
    /**
     * 22 搭配排产模式
     */
    MATCHING_PRODUCTION_GROUP_LOG(22, "搭配排产模式"),
    /**
     * 23 规格已经超成型产能不继续排产
     */
    SKIP_PRODUCTION_PLAN_LOG(23, "规格已经超成型产能不继续排产"),
    /**
     * 24 产能预占消耗
     */
    PLAN_PREEMPTION_QTY_LOG(24, "产能预占消耗"),
    /**
     * 25 按寸口由大到小排产
     */
    PRO_SIZE_MODEL_PRODUCTION_LOG(25, "按寸口由大到小排产"),
    /**
     * 26 排产模具数限制
     */
    PRODUCTION_MOULD_QTY_LIMIT_LOG(26, "排产模具数限制"),
    /**
     * 99 一键排产结束
     */
    WHOLE_PRODUCTION_END(99, "一键排产结束"),
    /**
     * 0 unknown
     */
    UNKNOWN_LOG(0, "unknown");

    private Integer typeValue;

    private String desc;

    MouldProductionLogType(Integer typeValue, String desc) {
        this.typeValue = typeValue;
        this.desc = desc;
    }

    /**
     * 得到排产阶段枚举类
     *
     * @param logType
     * @return
     */
    public static MouldProductionLogType getInstance(Integer logType) {
        if (null == logType) {
            return UNKNOWN_LOG;
        }
        return Arrays.stream(values()).filter(production -> production.getTypeValue().equals(logType)).findFirst().orElse(UNKNOWN_LOG);
    }

    public Integer getTypeValue() {
        return typeValue;
    }

    public String getDesc() {
        return desc;
    }
}
