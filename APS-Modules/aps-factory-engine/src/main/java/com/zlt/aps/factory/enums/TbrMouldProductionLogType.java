package com.zlt.aps.factory.enums;

import java.util.Arrays;

/**
 * 工厂TBR模具排产日志环节枚举定义
 *
 * @author ZLT
 * 20251210
 */
public enum TbrMouldProductionLogType {
    /**
     * 10-01 开始初始化
     */
    START_INIT("10-01", "开始初始化"),
    /**
     * 10-04 初始化结束
     */
    INIT_COMPLETE("10-04", "初始化结束"),
    /**
     * 10-09 初始化存储结束
     */
    SAVE_INIT("10-09", "初始化存储结束"),
    /**
     * 20-01 开始分组(结构)排产
     */
    START_GROUP("20-01", "开始分组(结构)排产"),
    /**
     * 20-02 获取排产版本计划数据
     */
    END_GET_VERSION_DATA("20-02", "获取排产版本计划数据"),
    /**
     * 20-03 读取排产参数配置
     */
    END_READER_PARAM_DATA("20-03", "读取排产参数配置"),
    /**
     * 20-04 特殊原材料配置为空
     */
    SPECIAL_MATERIAL_EMPTY("20-04", "特殊原材料配置为空"),
    /**
     * 20-05 特殊原材料库存为空
     */
    SPECIAL_MATERIAL_STOCK_EMPTY("20-05", "特殊原材料库存为空"),
    /**
     * 20-06 年月生产日历为空
     */
    PRODUCTION_CALENDAR_EMPTY("20-06", "年月生产日历为空"),
    /**
     * 20-07 年月没有停工日
     */
    STOP_DAY_EMPTY("20-07", "年月没有停工日"),
    /**
     * 20-08 成型机基础信息为空
     */
    CX_MACHINE_BASE_EMPTY("20-08", "成型机基础信息为空"),
    /**
     * 20-09 成型机维修信息为空
     */
    CX_MACHINE_MAINTENANCE_EMPTY("20-09", "成型机维修信息为空"),
    /**
     * 20-10 模具关系配置为空
     */
    MOULD_RELATION_INFO_EMPTY("20-10", "模具关系配置为空"),
    /**
     * 20-11 新模具到货配置为空
     */
    MOULD_DELIVERY_INFO_EMPTY("20-11", "新模具到货配置为空"),
    /**
     * 20-12 成型硫化配比配置为空
     */
    CX_GROUP_LH_RATIO_EMPTY("20-12", "成型硫化配比配置为空"),
    /**
     * 20-13 分组没有成型硫化配比配置
     */
    SINGLE_GROUP_LH_RATIO_EMPTY("20-13", "分组没有成型硫化配比配置"),
    /**
     * 20-14-01 没有获取到续作Sku信息
     */
    CONTINUE_SKU_DATA_EMPTY("20-14-01", "没有获取到续作Sku信息"),
    /**
     * 20-14-02 在机分组计划没有数据，故而在机分组环节无需排产
     */
    CONTINUE_GROUP_NO_CONTINUE_PRODUCTION("20-14-01", "在机分组计划没有数据，故而在机分组环节无需排产"),
    /**
     * 20-14-03 在产分组机台反向匹配分组计划没有收尾的机台
     */
    CONTINUE_MACHINE_NO_CLOSING_MACHINE("20-14-03", "在产分组机台反向匹配分组计划没有收尾的机台"),
    /**
     * 20-15 没有获取到下一组优先级高的分组计划
     */
    NO_NEXT_ADD_GROUP_PLAN("20-15", "没有获取到下一组优先级高的分组计划"),
    /**
     * 20-16-01 机台没有剩余产能
     */
    GROUP_NO_LEFT_OVER_CAPACITY_MACHINE("20-16-01", "机台没有剩余产能"),
    /**
     * 20-16-02 零度供料架不匹配
     */
    GROUP_NO_SELECTED_ZERO_MATCH_CX_MACHINE("20-16-02", "零度供料架不匹配"),
    /**
     * 20-16-03 机台限制生产
     */
    GROUP_NO_SELECTED_LIMIT_CX_MACHINE("20-16-03", "机台限制生产"),
    /**
     * 20-16-04 没有成型硫化配比
     */
    GROUP_NO_SELECTED_RATIO_CX_MACHINE("20-16-04", "没有成型硫化配比"),
    /**
     * 20-16-05 分组计划没有待排产计划
     */
    GROUP_NO_SELECTED_GROUP_NO_PRODUCTION_CX_MACHINE("20-16-05", "分组计划没有待排产计划"),
    /**
     * 20-16-06 分组计划没有物料描述信息
     */
    GROUP_NO_SELECTED_GROUP_MATERIAL_EXCEPTION_CX_MACHINE("20-16-06", "分组计划没有物料描述信息"),
    /**
     * 20-16-97 分组计划挑选机台-初步被选中
     */
    GROUP_SELECTED_FIRST_CX_MACHINE("20-16-97", "分组计划挑选机台-初步被选中"),
    /**
     * 20-16-98 分组计划挑选机台-固定优先被选中
     */
    GROUP_SELECTED_FIXED_FINAL_CX_MACHINE("20-16-98", "分组计划挑选机台-固定优先被选中"),
    /**
     * 20-16-99 分组计划挑选机台-最终被选中
     */
    GROUP_SELECTED_FINAL_CX_MACHINE("20-16-99", "分组计划挑选机台-最终被选中"),
    /**
     * 20-16 没有获取到下一组优先级高的分组计划
     */
    GROUP_NO_SELECTED_CX_MACHINE("20-16", "分组计划没有找到合适的机台"),

    /**
     * 30-01-01 非在机结构模具排产没有排产计划数据
     */
    GROUP_MOULD_NO_PLAN_DATA_CX_MACHINE("30-01-01", "非在机结构模具排产没有排产计划数据"),
    /**
     * 30-01-02 非在机结构模具排产没有找到机台信息
     */
    GROUP_MOULD_NO_FIND_CX_MACHINE("30-01-02", "非在机结构模具排产没有找到机台信息"),
    /**
     * 30-01-03 非在机结构模具排产结构分组没有找到硫化配比信息
     */
    GROUP_MOULD_GROUP_NO_FIND_RATIO_CX_MACHINE("30-01-03", "非在机结构模具排产结构分组没有找到硫化配比信息"),
    /**
     * 30-01-04 非在机结构模具排产结构分组没有找到机型硫化配比信息
     */
    GROUP_MOULD_GROUP_NO_FIND_BRAND_RATIO_CX_MACHINE("30-01-04", "非在机结构模具排产结构分组没有找到机型硫化配比信息"),
    /**
     * 30-02-01 非在机结构模具排产硫化组起始排产日超出收尾日
     */
    GROUP_MOULD_START_LIMIT_END_LH_GROUP("30-02-01", "非在机结构模具排产硫化组起始排产日超出收尾日"),
    /**
     * 30-02-02 非在机结构模具排产硫化组没有找到可排产Sku
     */
    GROUP_MOULD_NO_FIND_SKU_LH_GROUP("30-02-02", "非在机结构模具排产硫化组没有找到可排产Sku"),
    /**
     * 30-02-03 非在机结构模具排产硫化组排产Sku没有可排产量
     */
    GROUP_MOULD_SKU_NO_PRODUCTION_QTY_LH_GROUP("30-02-03", "非在机结构模具排产硫化组排产Sku没有可排产量"),
    /**
     * 30-02-04 非在机结构模具排产硫化组排产Sku没有合适的排产模具
     */
    GROUP_MOULD_SKU_NO_FIND_MOULD_LH_GROUP("30-02-04", "非在机结构模具排产硫化组排产Sku没有合适的排产模具"),
    /**
     * 30-03-01 在机结构续作Sku开始模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_START_MOULD_PRODUCTION("30-03-01", "在机结构续作Sku开始模具排产"),
    /**
     * 30-03-02 在机结构续作Sku-开始同规格同花纹-模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_START_SAME_SPEC_MOULD_PRODUCTION("30-03-02", "在机结构续作Sku-开始同规格同花纹-模具排产"),
    /**
     * 30-03-03 在机结构续作Sku-开始同生胎同模具-模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_START_SAME_EMBRYO_MOULD_PRODUCTION("30-03-03", "在机结构续作Sku-开始同生胎同模具-模具排产"),
    /**
     * 99 一键排产结束
     */
    WHOLE_PRODUCTION_END("99", "一键排产结束"),
    /**
     * 00 unknown
     */
    UNKNOWN_LOG("00", "unknown");

    private String typeValue;

    private String desc;

    TbrMouldProductionLogType(String typeValue, String desc) {
        this.typeValue = typeValue;
        this.desc = desc;
    }

    /**
     * 得到排产阶段枚举类
     *
     * @param logType
     * @return
     */
    public static TbrMouldProductionLogType getInstance(String logType) {
        if (null == logType) {
            return UNKNOWN_LOG;
        }
        return Arrays.stream(values()).filter(production -> production.getTypeValue().equals(logType)).findFirst().orElse(UNKNOWN_LOG);
    }

    public String getTypeValue() {
        return typeValue;
    }

    public String getDesc() {
        return desc;
    }
}
