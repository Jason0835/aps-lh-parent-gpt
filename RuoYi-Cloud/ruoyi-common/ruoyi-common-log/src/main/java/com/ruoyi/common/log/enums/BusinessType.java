package com.ruoyi.common.log.enums;

/**
 * 业务操作类型
 * 
 * @author ruoyi
 */
public enum BusinessType
{
    /**
     * 其它
     */
    OTHER,

    /**
     * 新增
     */
    INSERT,

    /**
     * 修改
     */
    UPDATE,

    /**
     * 删除
     */
    DELETE,

    /**
     * 授权
     */
    GRANT,

    /**
     * 导出
     */
    EXPORT,

    /**
     * 导入
     */
    IMPORT,

    /**
     * 强退
     */
    FORCE,

    /**
     * 生成代码
     */
    GENCODE,

    /**
     * 清空数据
     */
    CLEAN,

    /**
     * 新增或编辑(根据是否有id来判断是新增还是编辑)
     */
    INSERT_OR_UPDATE,

    /**
     * 发布排程结果
     */
    PUBLISH,

    /**
     * 自动排程
     */
    AUTOPLAN,

    /**
     * 转机台
     */
    CHANGE_MACHINE,

    /**
     * 调量
     */
    CHANGE_QTY,

    /**
     * 模具变动单生成
     */
    auto_mold_change,

    /**
     * 排程收尾操作
     */
    MANUAL_CLOSE,

    /**
     * 修改-使用模数
     */
    UPDATE_MOLDS,

    /**
     * 选机台
     */
    CHOOSE_MACHINE,

    /**
     * 在产下发MPS
     */
    PRODUCING_ISSUE,

    /**
     * 均衡
     */
    BALANCE,

    /**
     * 生成前日增补计划
     */
    GENERATE_SUPPLEMENT_PLAN,

    /**
     * 确认前日增补计划
     */
    CONFIRM_SUPPLEMENT_PLAN,

    /**
     * 同胶料归并生产
     */
    MERGE_PRODUCT,

    /**
     * 合并
     */
    CONSOLIDATION,
}
