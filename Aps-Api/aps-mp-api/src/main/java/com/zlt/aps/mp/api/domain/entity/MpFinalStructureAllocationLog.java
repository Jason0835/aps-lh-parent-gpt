package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import com.zlt.common.utils.StringUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpFinalStructureAllocationLog.java
 * 描    述：排产过程_定稿版本结构排产备份对象 t_mp_final_structure_allocation_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-05-18
 */

@Data
@TableName(value = "T_MP_FINAL_STRUCTURE_ALLOCATION_LOG")
@ApiModel(value = "排产过程_定稿版本结构排产备份对象", description = "排产过程_定稿版本结构排产备份对象")
public class MpFinalStructureAllocationLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 需求计划版本
     */
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 排产版本号
     */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟
     */
    @ApiModelProperty(value = "计划类型：biz_plan_type 01 正常 02 订单预测 03 实单模拟", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 数据来源 01-手工新增 02-自动生成 03-导入 04-接口同步
     */
    @ApiModelProperty(value = "数据来源", name = "dataSource")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 是否含有特殊材料
     */
    @ApiModelProperty(value = "是否含有特殊材料", name = "isHasSpecialMaterial")
    @TableField(value = "IS_HAS_SPECIAL_MATERIAL")
    private String isHasSpecialMaterial;

    /**
     * 成型机编码
     */
    @ApiModelProperty(value = "成型机编码", name = "cxMachineCode")
    @TableField(value = "CX_MACHINE_CODE")
    private String cxMachineCode;

    /**
     * 最大胎胚种类数
     */
    @ApiModelProperty(value = "最大胎胚种类数", name = "maxEmbryoCodeCount")
    @TableField(value = "MAX_EMBRYO_CODE_COUNT")
    private Integer maxEmbryoCodeCount;

    /**
     * 最大硫化机台数
     */
    @ApiModelProperty(value = "最大硫化机台数", name = "maxLhMachineCount")
    @TableField(value = "MAX_LH_MACHINE_COUNT")
    private Integer maxLhMachineCount;

    /**
     * 实单最低硫化机台数
     */
    @ApiModelProperty(value = "实单最低硫化机台数", name = "minLhMachineCount")
    @TableField(value = "MIN_LH_MACHINE_COUNT")
    private Integer minLhMachineCount;

    /**
     * 排产净需求
     */
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /**
     * 排产净需求(含损耗)
     */
    @ApiModelProperty(value = "排产净需求(含损耗)", name = "lossQty")
    @TableField(value = "LOSS_QTY")
    private Integer lossQty;

    /**
     * 开始日期
     */
    @ApiModelProperty(value = "开始日期", name = "beginDay")
    @TableField(value = "BEGIN_DAY")
    private Integer beginDay;

    /**
     * 结束日期
     */
    @ApiModelProperty(value = "结束日期", name = "endDay")
    @TableField(value = "END_DAY")
    private Integer endDay;

    /**
     * 分配天数
     */
    @ApiModelProperty(value = "分配天数", name = "allotDays")
    @TableField(value = "ALLOT_DAYS")
    private Integer allotDays;

    /**
     * 交替类型
     */
    @ApiModelProperty(value = "交替类型", name = "alternatingType")
    @TableField(value = "ALTERNATING_TYPE")
    private String alternatingType;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    /**
     * 从结构信息中解析出英寸
     * @return 英寸
     */
    public String tbrProSize(){
        if (StringUtil.isEmptyWithTrim(this.structureName)){
            return "";
        }
        // 正则：R后面跟数字（可能带小数点）
        Pattern pattern = Pattern.compile("R\\d+(?:\\.\\d+)?");
        Matcher matcher = pattern.matcher(this.structureName);
        String proSize = matcher.find() ? matcher.group() : "";
        return proSize;
    }
}