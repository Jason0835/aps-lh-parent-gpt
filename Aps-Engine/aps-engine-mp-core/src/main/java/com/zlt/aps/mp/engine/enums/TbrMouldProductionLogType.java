package com.zlt.aps.mp.engine.enums;

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
     * 10-02-01 初始化读取业务参数为空
     */
    INIT_GET_PARAM_DATA("10-02-01", "初始化读取业务参数为空"),
    /**
     * 10-02-02 初始化计算物料损耗量
     */
    INIT_LOSS_QTY("10-02-02", "初始化计算物料损耗量"),
    /**
     * 10-02-03 初始化读取物料基础数据为空
     */
    INIT_MATERIAL_DATA("10-02-03", "初始化读取物料基础数据为空"),
    /**
     * 10-02-04 初始化没有找到物料基础信息
     */
    INIT_SINGLE_MATERIAL_DATA("10-02-04", "初始化没有找到物料基础信息"),
    /**
     * 10-02-05 初始化读取Sku施工信息数据为空
     */
    INIT_CONSTRUCTION_DATA("10-02-05", "初始化读取Sku施工信息数据为空"),
    /**
     * 10-02-06 初始化没有找到Sku施工关系
     */
    INIT_SINGLE_CONSTRUCTION_DATA("10-02-06", "初始化没有找到Sku施工关系"),
    /**
     * 10-02-07 初始化读取Sku日硫化量信息数据为空
     */
    INIT_DAY_LH_CAPACITY_DATA("10-02-07", "初始化读取Sku日硫化量信息数据为空"),
    /**
     * 10-02-08 初始化没有找到Sku日硫化量信息
     */
    INIT_SINGLE_DAY_LH_CAPACITY_DATA("10-02-08", "初始化没有找到Sku日硫化量信息"),
    /**
     * 10-02-09 初始化读取模具关系配置为空
     */
    INIT_MOULD_RELATION_INFO_EMPTY("10-02-09", "初始化读取模具关系配置为空"),
    /**
     * 10-02-10 初始化新模具到货配置为空
     */
    INIT_MOULD_DELIVERY_INFO_EMPTY("10-02-10", "初始化新模具到货配置为空"),
    /**
     * 10-02-11 初始化没有找到模具配置
     */
    INIT_SINGLE_MOULD_INFO_EMPTY("10-02-11", "初始化没有找到模具配置"),
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
     * 20-03 排产前计划初始数据设置
     */
    PLAN_INIT_DATA("20-03", "排产前计划初始数据设置"),
    /**
     * 20-04 开始排产前数据加载
     */
    START_BEFORE_PRODUCTION_DATA("20-04", "开始排产前数据加载"),
    /**
     * 20-05 排产前基础配置数据加载
     */
    BEFORE_PRODUCTION_DATA_LOADING("20-05", "排产前基础配置数据加载"),
    /**
     * 20-06-00 开始分组粗算成型机台数
     */
    START_GROUP_CAPACITY_CALCULATE("20-06-00", "开始分组粗算成型机台数"),
    /**
     * 20-06-01 分组主花纹产能预算
     */
    GROUP_MAIN_PATTERN_CAPACITY_INFO("20-06-01", "分组主花纹产能预算"),
    /**
     * 20-06-02 分组(TBR结构)总产能预算
     */
    GROUP_SUM_CAPACITY_INFO("20-06-02", "分组(TBR结构)总产能预算"),
    /**
     * 20-06-03 分组(TBR结构)预算机台数
     */
    GROUP_SUM_CAPACITY_CX_MACHINE_INFO("20-06-03", "分组(TBR结构)预算机台数"),
    /**
     * 20-06-04 汇总
     */
    SUMMARY_INFO_SUM("20-06-04", "汇总"),
    /**
     * 20-06-05 在机分组在产机台汇总
     */
    CONTINUE_GROUP_CONTINUE_CX_MACHINE_SUMMARY_INFO_SUM("20-06-05", "在机分组在产机台汇总"),
    /**
     * 20-07 在机分组(TBR结构)在产机台情况
     */
    CONTINUE_GROUP_CONTINUE_CX_MACHINE("20-07", "在机分组(TBR结构)在产机台情况"),
    /**
     * 20-08 为非在机分组(TBR结构)
     */
    GROUP_NO_CONTINUE_GROUP_INFO("20-08", "非在机分组(TBR结构)情况"),
    /**
     * 20-14-01 分组计划为在机分组(TBR结构)数据设置
     */
    ON_LINE_GROUP_SET_UP_DATA_INFO("20-14-01", "分组计划为在机分组(TBR结构)数据设置"),
    /**
     * 20-14-01-01 在机分组计划没有在产机台
     */
    CONTINUE_GROUP_NO_ON_LINE_MACHINE_EMPTY("20-14-01-01", "在机分组计划没有在产机台"),
    /**
     * 20-14-01-02 在机分组使用在产机台没有计划
     */
    CONTINUE_GROUP_ON_LINE_MACHINE_PLAN_EMPTY("20-14-01-02", "在机分组使用在产机台没有计划"),
    /**
     * 20-14-01-03 在机分组使用在产机台没有待排产计划
     */
    CONTINUE_GROUP_ON_LINE_MACHINE_PRODUCTION_PLAN_EMPTY("20-14-01-03", "在机分组使用在产机台没有待排产计划"),
    /**
     * 20-14-01-04 在机分组使用在产机台没有待排硫化组
     */
    CONTINUE_GROUP_ON_LINE_MACHINE_NO_LH_GROUP("20-14-01-04", "在机分组使用在产机台没有待排硫化组"),
    /**
     * 20-44 特殊原材料结构排产
     */
    SPECIAL_MATERIAL_GROUP_PRODUCTION("20-44","特殊原材料结构排产"),
    /**
     * 20-25 结构提前收尾业务
     */
    GROUP_BEFORE_CONCLUSION("20-25", "结构提前收尾业务"),
    /**
     * 20-26 日排产限制业务控制
     */
    DAY_LIMIT_CONTROL("20-26", "日排产限制业务控制"),
    /**
     * 20-14-01-05 在机分组使用在产机台排产硫化组
     */
    CONTINUE_GROUP_ON_LINE_MACHINE_LH_GROUP_RANGE("20-14-01-05", "在机分组使用在产机台排产硫化组"),
    /**
     * 20-14-99 分组查找硫化组
     */
    GROUP_FIND_LH_MACHINE_RANGE("20-14-01-05", "分组查找硫化组"),
    /**
     * 20-14-01-06 在机结构模具排产硫化组找到排产Sku
     */
    CONTINUE_GROUP_MOULD_FIND_SKU_LH_GROUP("20-14-01-06", "在机结构模具排产硫化组找到排产Sku"),
    /**
     * 30-14-01-07 在机结构硫化组排产Sku没有合适的排产模具
     */
    CONTINUE_GROUP_MOULD_SKU_NO_FIND_MOULD_LH_GROUP("30-14-01-07", "在机结构硫化组排产Sku没有合适的排产模具"),
    /**
     * 30-14-01-08 硫化组排产Sku达到限制
     */
    MOULD_SKU_LIMIT_LH_GROUP("30-14-01-08", "硫化组排产Sku达到限制"),
    /**
     * 20-14-00-01 在机分组(TBR结构)没有排产计划
     */
    CONTINUE_GROUP_NO_PRODUCTION_PLAN_INFO("20-14-00-01", "在机分组(TBR结构)没有排产计划"),
    /**
     * 20-14-00-02 在机分组(TBR结构)没有续作Sku信息
     */
    CONTINUE_GROUP_NO_CONTINUE_SKU_INFO("20-14-00-02", "在机分组(TBR结构)没有续作Sku信息"),
    /**
     * 20-14-00-03 在机分组(TBR结构)续作没有排产计划
     */
    CONTINUE_GROUP_CONTINUE_SKU_EMPTY_INFO("20-14-00-03", "在机分组(TBR结构)续作没有排产计划"),
    /**
     * 20-14-00-04 在机分组(TBR结构)续作Sku没有排产计划
     */
    CONTINUE_GROUP_CONTINUE_SKU_NO_PLAN_INFO("20-14-00-04", "在机分组(TBR结构)续作Sku没有排产计划"),
    /**
     * 20-14-02 在机分组计划没有数据，故而在机分组环节无需排产
     */
    CONTINUE_GROUP_NO_CONTINUE_PRODUCTION("20-14-02", "在机分组计划没有数据，故而在机分组环节无需排产"),
    /**
     * 20-14-03 在产分组机台反向匹配分组计划没有收尾的机台
     */
    CONTINUE_MACHINE_NO_CLOSING_MACHINE("20-14-03", "在产分组机台反向匹配分组计划没有收尾的机台"),
    /**
     * 20-14-04 收尾机台在基础信息中没有找到
     */
    REVERSE_MACHINE_NO_FIND_MACHINE("20-14-04", "收尾机台在基础信息中没有找到"),
    /**
     * 20-14-99 在机结构模拟排产后收尾机台排产
     */
    REVERSE_MACHINE_PRODUCTION_INFO("20-14-99", "在机结构模拟排产后收尾机台排产"),
    /**
     * 20-14-05 收尾机台没有排产计划
     */
    REVERSE_MACHINE_NO_FIND_GROUP_PLAN("20-14-05", "收尾机台没有排产计划"),
    /**
     * 20-14-06 收尾机台没有剩余产能
     */
    REVERSE_MACHINE_NO_REMAINING_CAPACITY("20-14-06", "收尾机台没有剩余产能"),
    /**
     * 20-14-07 收尾机台没有找到产能可覆盖的计划
     */
    REVERSE_MACHINE_CAPACITY_NO_COVER_PLAN("20-14-07", "收尾机台没有找到产能可覆盖的计划"),
    /**
     * 20-14-08 收尾机台没有找到产能可覆盖机台又匹配的计划
     */
    REVERSE_MACHINE_CAPACITY_COVER_NO_MATCH_PLAN("20-14-08", "收尾机台没有找到产能可覆盖机台又匹配的计划"),
    /**
     * 20-14 机台-分组计划匹配
     */
    MACHINE_MATCH_PLAN("20-14", "机台-分组计划匹配"),
    /**
     * 20-14-09 收尾机台反向匹配到计划分组
     */
    REVERSE_MACHINE_SELECTED_GROUP_PLAN("20-14-09", "收尾机台反向匹配到计划分组"),
    /**
     * 20-14-10 收尾机台还有剩余产能反向匹配下一组计划分组
     */
    REVERSE_MACHINE_SELECTED_NEXT_GROUP_PLAN("20-14-10", "收尾机台还有剩余产能反向匹配下一组计划分组"),
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
     * 20-16-02 分组计划与成型机台条件匹配
     */
    GROUP_CX_MACHINE_BASE_MACHE("20-16-02", "分组计划与成型机台条件匹配"),
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
     * 20-16-29 机台挑选计划-最终被选中
     */
    CX_MACHINE_SELECTED_FINAL_GROUP("20-16-29", "机台挑选分组计划-最终被选中"),
    /**
     * 20-16 没有获取到下一组优先级高的分组计划
     */
    GROUP_NO_SELECTED_CX_MACHINE("20-16", "分组计划没有找到合适的机台"),
    /**
     * 20-16-01 分组计划找机台
     */
    GROUP_SELECTED_CX_MACHINE("20-16-01", "分组计划找机台"),
    /**
     * 20-16-02 机台找分组计划
     */
    CX_MACHINE_SELECTED_GROUP("20-16-02", "机台找分组计划"),
    /**
     * 20-90 成型补量分配
     */
    SUPPLEMENT_CX_MACHINE_DISTRIBUTION("20-90","成型补量分配"),
    /**
     * 30-01-00 开始分组计划模具排产
     */
    START_CX_MACHINE_GROUP_MOULD_PRODUCTION("30-01-00", "开始分组计划模具排产"),
    /**
     * 30-39 补量排产
     */
    BOOST_QTY_PRODUCTION("30-39", "补量排产"),
    /**
     * 30-01 分组计划模拟模具排产
     */
    SIMULATE_MOULD_PRODUCTION("30-01", "分组计划模拟模具排产"),
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
     * 30-02-05 非在机结构使用模具排产硫化组
     */
    GROUP_MOULD_SKU_USED_FIND_MOULD_PRODUCTION("30-02-05", "非在机结构使用模具排产硫化组"),
    /**
     * 30-03-01 在机结构续作Sku开始模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_START_MOULD_PRODUCTION("30-03-01", "在机结构续作Sku开始模具排产"),
    /**
     * 30-03-00 在机结构续作Sku使用模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_FOR_MOULD_PRODUCTION("30-03-00", "在机结构续作Sku使用模具排产"),
    /**
     * 30-03-00-01 在机结构续作Sku没有模具
     */
    CONTINUE_GROUP_CONTINUE_SKU_NO_MOULD("30-03-01-01", "在机结构续作Sku没有模具"),
    /**
     * 30-09-01 计划模具排产
     */
    MOULD_PRODUCTION_PLAN("30-09-01", "计划模具排产"),
    /**
     * 30-03-01-01 在机结构续作Sku开始模具排产当前阶段没有排产量
     */
    CONTINUE_GROUP_CONTINUE_SKU_MOULD_PRODUCTION_NO_QTY("30-03-01-01", "在机结构续作Sku开始模具排产当前阶段没有排产量"),
    /**
     * 30-03-01-02 在机结构续作Sku降膜排产没有结果
     */
    CONTINUE_GROUP_CONTINUE_SKU_MOULD_PRODUCTION_NO_RESULT("30-03-01-02", "在机结构续作Sku降膜排产没有结果"),
    /**
     * 30-03-02 在机结构续作Sku-开始同规格同花纹-模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_START_SAME_SPEC_MOULD_PRODUCTION("30-03-02", "在机结构续作Sku-开始同规格同花纹-模具排产"),
    /**
     * 30-03-03 在机结构续作Sku-开始同生胎同模具-模具排产
     */
    CONTINUE_GROUP_CONTINUE_SKU_START_SAME_EMBRYO_MOULD_PRODUCTION("30-03-03", "在机结构续作Sku-开始同生胎同模具-模具排产"),
    /**
     * 40-01 分组计划正式开始模具排产
     */
    FORMAL_MOULD_START("40-01", "分组计划正式开始模具排产"),
    /**
     * 40-02 分组计划正式开始模具排产数据重置完成
     */
    FORMAL_MOULD_RESET_DATA_FINISH("40-02", "分组计划正式开始模具排产数据重置完成"),
    /**
     * 40-03 分组计划正式开始模具排产数据为空
     */
    FORMAL_MOULD_DATA_EMPTY("40-03", "分组计划正式开始模具排产数据为空"),
    /**
     * 40-04 分组计划正式开始模具排产-排产在机结构
     */
    FORMAL_MOULD_CONTINUE_GROUP_PRODUCTION("40-04", "分组计划正式开始模具排产-排产在机结构"),
    /**
     * 40-04-01 在机结构正式开始续作排产
     */
    FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP("40-04-01", "在机结构正式开始续作排产"),
    /**
     * 40-04-02 在机结构正式排产没有计划
     */
    FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP_NO_PLAN("40-04-02", "在机结构正式排产没有计划"),
    /**
     * 40-04-02 在机结构正式排产没有续作Sku
     */
    FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP_NO_CONTINUE_SKU("40-04-03", "在机结构正式排产没有续作Sku"),
    /**
     * 40-04-02 在机结构正式开始新增Sku排产
     */
    FORMAL_MOULD_CONTINUE_GROUP_SINGLE_ADD_GROUP("40-04-02", "在机结构正式开始新增Sku排产"),
    /**
     * 40-05 新增结构开始正式排产
     */
    FORMAL_MOULD_ADD_GROUP_SINGLE_GROUP("40-05", "新增结构开始正式排产"),
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
