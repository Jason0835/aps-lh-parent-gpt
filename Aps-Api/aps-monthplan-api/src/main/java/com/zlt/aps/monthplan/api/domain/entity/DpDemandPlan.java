package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlan.java
 * 描    述：需求计划对象 t_dp_demand_plan
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2025-12-25
 */

@Data
@TableName(value = "T_DP_DEMAND_PLAN")
@ApiModel(value = "需求计划对象", description = "需求计划对象 ")
public class DpDemandPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂
     */
    @Excel(name = "ui.data.column.demandPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.demandPlan.year", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "年", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.demandPlan.month", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月", name = "month")
    @TableField(value = "MONTH")
    private Integer month;


    /**
     * 优先级(订单类型)
     */
    @ApiModelProperty(value = "优先级(订单类型)", name = "orderPriority")
    @TableField(value = "ORDER_PRIORITY")
    private String orderPriority;



    /**
     * 是否替换料
     */
    @ApiModelProperty(value = "是否替换料", name = "isAlternateMaterial")
    @TableField(value = "IS_ALTERNATE_MATERIAL")
    private String isAlternateMaterial;

    /**
     * 产品品类
     */
    @Excel(name = "ui.data.column.demandPlan.productTypeCode", dictType = "biz_product_type")
    @ApiModelProperty(value = "产品品类", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 类型
     */
    @Excel(name = "ui.data.column.demandPlan.locationType", dictType = "biz_stor_type")
    @ApiModelProperty(value = "类型", name = "locationType")
    @TableField(value = "LOCATION_TYPE")
    private String locationType;
    /**
     * 需求计划版本号
     */
    @Excel(name = "ui.data.column.demandPlan.monthPlanVersion")
    @ApiModelProperty(value = "需求计划版本号", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;
    /**
     * 品牌
     */
    @Excel(name = "ui.data.column.demandPlan.brand")
    @ApiModelProperty(value = "品牌", name = "brand")
    @TableField(value = "BRAND")
    private String brand;

    /**
     * 供应链优先级
     */
    @Excel(name = "ui.data.column.demandPlan.scmPriority", dictType = "biz_yes_no")
    @ApiModelProperty(value = "供应链优先级", name = "scmPriority")
    @TableField(value = "SCM_PRIORITY")
    private String scmPriority;

    /**
     * 产品结构
     */
    @Excel(name = "ui.data.column.demandPlan.structureName")
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 主花纹
     */
    @Excel(name = "ui.data.column.demandPlan.mainPattern")
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /**
     * 物料编码
     */
    @Excel(name = "ui.data.column.demandPlan.materialCode")
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @Excel(name = "ui.data.column.demandPlan.materialDesc")
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 排产分类
     */
    @Excel(name = "ui.data.column.demandPlan.productionType", dictType = "biz_schedule_type")
    @ApiModelProperty(value = "排产分类", name = "productionType")
    @TableField(value = "PRODUCTION_TYPE")
    private String productionType;

    /**
     * 年周号
     */
    @Excel(name = "ui.data.column.demandPlan.yearWeek")
    @ApiModelProperty(value = "年周号", name = "yearWeek")
    @TableField(value = "YEAR_WEEK")
    private String yearWeek;
    /**
     * 均匀性
     */
    @Excel(name = "ui.data.column.demandPlan.isUniformity", dictType = "biz_yes_no")
    @ApiModelProperty(value = "均匀性", name = "isUniformity")
    @TableField(value = "IS_UNIFORMITY")
    private String isUniformity;
    /**
     * 动平衡
     */
    @Excel(name = "ui.data.column.demandPlan.isDynamicBalance", dictType = "biz_yes_no")
    @ApiModelProperty(value = "动平衡", name = "isDynamicBalance")
    @TableField(value = "IS_DYNAMIC_BALANCE")
    private String isDynamicBalance;
    /**
     * 订单量
     */
    @Excel(name = "ui.data.column.demandPlan.orderQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "订单量", name = "orderQty")
    @TableField(value = "ORDER_QTY")
    private Integer orderQty;

    /**
     * 库存
     */
    @Excel(name = "ui.data.column.demandPlan.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "库存", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Integer stockQty;

    /**
     * 月结库存余量
     */
    @ApiModelProperty(value = "月结库存余量", name = "remainingQty")
    @TableField(value = "REMAINING_QTY")
    private Integer remainingQty;

    /**
     * 月均销量
     */
    @ApiModelProperty(value = "月均销量", name = "averageSaleQty")
    @TableField(value = "AVERAGE_SALE_QTY")
    private Integer averageSaleQty;

    /**
     * 月底余量
     */
    @Excel(name = "ui.data.column.demandPlan.plannedSurplus", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "月底余量", name = "plannedSurplus")
    @TableField(value = "PLANNED_SURPLUS")
    private Integer plannedSurplus;

    /**
     * 排产净需求
     */
    @Excel(name = "ui.data.column.demandPlan.netQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "排产净需求", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;

    /**
     * 是否排产
     */
    @Excel(name = "ui.data.column.demandPlan.isProduction", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否排产", name = "isProduction")
    @TableField(value = "IS_PRODUCTION")
    private String isProduction;

    /**
     * 净需求(含暂缓)
     */
    @Excel(name = "ui.data.column.demandPlan.postponeNetQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "净需求(含暂缓)", name = "postponeNetQty")
    @TableField(value = "POSTPONE_NET_QTY")
    private Integer postponeNetQty;

    /**
     * 净需求(不含暂缓)
     */
    @Excel(name = "ui.data.column.demandPlan.unPostponeNetQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "净需求(不含暂缓)", name = "unPostponeNetQty")
    @TableField(value = "UN_POSTPONE_NET_QTY")
    private Integer unPostponeNetQty;

    /**
     * 高优先级
     */
    @Excel(name = "ui.data.column.demandPlan.heightQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "高优先级", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /**
     * 中优先级
     */
    @Excel(name = "ui.data.column.demandPlan.midQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "中优先级", name = "midQty")
    @TableField(value = "MID_QTY")
    private Integer midQty;

    /**
     * 暂缓订单
     */
    @Excel(name = "ui.data.column.demandPlan.postponeQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "暂缓订单", name = "postponeQty")
    @TableField(value = "POSTPONE_QTY")
    private Integer postponeQty;

    /**
     * 周期排产储备
     */
    @Excel(name = "ui.data.column.demandPlan.cycleReserveQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "周期排产储备", name = "cycleReserveQty")
    @TableField(value = "CYCLE_RESERVE_QTY")
    private Integer cycleReserveQty;

    /**
     * 常规储备
     */
    @Excel(name = "ui.data.column.demandPlan.conventionReserveQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "常规排产储备", name = "conventionReserveQty")
    @TableField(value = "CONVENTION_RESERVE_QTY")
    private Integer conventionReserveQty;

    /**
     * 是否满足最小投产量
     */
    @Excel(name = "ui.data.column.demandPlan.isReachMinProductionQty", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否满足最小投产量", name = "isReachMinProductionQty")
    @TableField(value = "IS_REACH_MIN_PRODUCTION_QTY")
    private String isReachMinProductionQty;

    /**
     * 最小投产量值
     */
    @Excel(name = "ui.data.column.demandPlan.minProductionQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "最小投产量值", name = "minProductionQty")
    @TableField(value = "MIN_PRODUCTION_QTY")
    private Integer minProductionQty;

    @Excel(name = "ui.data.column.demandPlan.remark")
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @Excel(name = "ui.data.column.demandPlan.updateTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(
        value = "UPDATE_TIME",
        fill = FieldFill.INSERT_UPDATE,
        jdbcType = JdbcType.TIMESTAMP
    )
    private Date updateTime;

    /**
     * 计划类型
     */
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * MES物料编号
     */
    @ApiModelProperty(value = "MES物料编号", name = "mesMaterialCode")
    @TableField(value = "MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    @TableField(value = "CHANNEL")
    private String channel;

    /**
     * 英寸
     */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private String hierarchy;

    /**
     * 速级
     */
    @ApiModelProperty(value = "速级", name = "speed")
    @TableField(value = "SPEED")
    private String speed;

    /**
     * 是否重要客户
     */
    @ApiModelProperty(value = "是否重要客户", name = "isImportantCustom")
    @TableField(value = "IS_IMPORTANT_CUSTOM")
    private String isImportantCustom;

    /**
     * 是否必保计划
     */
    @ApiModelProperty(value = "是否必保计划", name = "isEnsurePlan")
    @TableField(value = "IS_ENSURE_PLAN")
    private String isEnsurePlan;

    /**
     * 是否紧急订单
     */
    @ApiModelProperty(value = "是否紧急订单", name = "isEmergency")
    @TableField(value = "IS_EMERGENCY")
    private String isEmergency;

    /**
     * 是否欠产
     */
    @ApiModelProperty(value = "是否欠产", name = "isDebitPlan")
    @TableField(value = "IS_DEBIT_PLAN")
    private String isDebitPlan;

    /**
     * 期望交期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "期望交期", name = "deliveryDateDue")
    @TableField(value = "DELIVERY_DATE_DUE")
    private Date deliveryDateDue;

    /**
     * 是否EXCEL导入
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    @TableField(value = "IS_IMPORT")
    private String isImport;
    /**
     * 暂缓订单是否参与冲减
     */
    @ApiModelProperty(value = "暂缓订单是否参与冲减", name = "isAllocationByPostponeOrder")
    @TableField(exist = false)
    private boolean includePostpone;
    /**
     * 需求版本号前缀
     */
    @ApiModelProperty(value = "需求版本号前缀", name = "prefix")
    @TableField(exist = false)
    private String prefix;

    @ApiModelProperty(value = "Y", name = "currentYearStockQty")
    @TableField(exist = false)
    private Integer currentYearStockQty;
    @ApiModelProperty(value = "Y-1", name = "sub1YearStockQty")
    @TableField(exist = false)
    private Integer sub1YearStockQty;
    @ApiModelProperty(value = "Y-2+", name = "sub2YearStockQty")
    @TableField(exist = false)
    private Integer sub2YearStockQty;




    /**
     * 按SKU、动平衡、均匀性、年周号为维度分组合并
     */
    public String getGroupKey() {
        String keyFormat = "%s|*|%s|*|%s|*|%s";
        return String.format(keyFormat, materialCode, isDynamicBalance, isUniformity, yearWeek);
    }

    public String getGroupFactoryAndMaterialKey() {
        String keyFormat = "%s|*|%s";
        return String.format(keyFormat, factoryCode, materialDesc);
    }

    /**
     * 获取分厂销售需求版本计划分组Key
     *
     * @return
     */
    public String getMonthPlanVersionKey() {
        String keyFormat = "%d|*|%d|*|%s|*|%s|*|%s|*|%s";
        return String.format(keyFormat, this.year, this.month, this.factoryCode, this.productTypeCode, this.monthPlanVersion,this.materialDesc);
    }
}