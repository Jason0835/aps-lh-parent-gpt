package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

/**
 * 月度计划-日排产信息对象
 *
 * @author ZLT
 * @data 20250519
 */
@Data
public class FactoryMonthPlanDayProductionInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 排产制造单号
     */
    @ApiModelProperty(value = "排产制造单号", name = "productionNo")
    private String productionNo;

    /**
     * 生产分厂编号
     */
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 年月:YYYYMM
     */
    @ApiModelProperty(value = "年月:YYYYMM", name = "yearMonth")
    private Integer yearMonth;

    /**
     * 销售生产需求计划版本
     */
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    private String productionVersion;

    /**
     * 生产物料编号
     */
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;

    /**
     * 生产规格描述
     */
    @ApiModelProperty(value = "生产规格描述", name = "productDesc")
    private String productDesc;

    /**
     * 施工阶段
     */
    @ApiModelProperty(value = "施工阶段", name = "constructionStage")
    private Integer constructionStage;

    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    private String mouldMethod;

    /**
     * 全规格代号信息 包含规格代号及对应的成型法
     */
    @ApiModelProperty(value = "全规格代号信息", name = "specCodeInfo")
    private String specCodeInfo;

    /**
     * 库位类别
     */
    @ApiModelProperty(value = "库位类别", name = "locationType")
    private String locationType;

    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", name = "channel")
    private String channel;

    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌", name = "brand")
    private String brand;

    /**
     * 寸口
     */
    @ApiModelProperty(value = "寸口", name = "proSize")
    private BigDecimal proSize;

    /**
     * 规格
     */
    @ApiModelProperty(value = "规格", name = "specifications")
    private String specifications;

    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /**
     * 层级
     */
    @ApiModelProperty(value = "层级", name = "hierarchy")
    private String hierarchy;

    /**
     * 品名编码
     */
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    private String productTypeCode;

    /**
     * 品名
     */
    @ApiModelProperty(value = "品名", name = "productTypeName")
    private String productTypeName;

    /**
     * 等级码
     */
    @ApiModelProperty(value = "等级码", name = "levelCode", hidden = true)
    private String levelCode;

    /**
     * 等级名称
     */
    @ApiModelProperty(value = "等级名称", name = "levelName", hidden = true)
    private String levelName;

    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;

    /**
     * 生胎代码
     */
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;

    /**
     * 模具
     */
    @ApiModelProperty(value = "模具", name = "mouldNo")
    private String mouldNo;

    /**
     * 模数
     */
    @ApiModelProperty(value = "模数", name = "mouldQty")
    private Integer mouldQty;

    /**
     * 模具编码集合，多个以,分隔
     */
    @ApiModelProperty(value = "模具编码集合，多个以,分隔", name = "mouldInfo", hidden = true)
    private String mouldInfo;

    /**
     * 合并信息json串
     */
    @ApiModelProperty(value = "合并信息json串", name = "mergeInfo", hidden = true)
    private String mergeInfo;

    /**
     * 是否有交期（0：默认没有，1：有）
     */
    @ApiModelProperty(value = "是否有交期", name = "isDeliveryDate")
    private Integer isDeliveryDate;

    /**
     * 是否EXCEL导入（0：默认不是，1：是）
     */
    @ApiModelProperty(value = "是否EXCEL导入", name = "isImport")
    private Integer isImport;

    /**
     * 单条硫化时间(包含增加间隔)-调整时使用
     */
    @ApiModelProperty(value = "单条硫化时间(包含增加间隔)-到秒", name = "curingTime")
    private BigDecimal curingTime;

    /**
     * 生产需求计划
     */
    @ApiModelProperty(value = "生产需求计划", name = "prodReqPlan")
    private Long prodReqPlan;

    /**
     * 实际生产需求(含损耗)
     */
    @ApiModelProperty(value = "实际生产需求(含损耗)", name = "factProdReqQty")
    private Long factProdReqQty;

    /**
     * 生产实际排产量
     */
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    private Long totalQty;

    /**
     * 差异量(未排产数量)
     */
    @ApiModelProperty(value = "差异量(未排产数量)", name = "differenceQty")
    private Long differenceQty;

    /**
     * 未排产原因
     */
    @ApiModelProperty(value = "未排产原因", name = "reason")
    private String reason;

    /**
     * 开始日期
     */
    @ApiModelProperty(value = "开始日期", name = "beginDate")
    private Integer beginDate;

    /**
     * 结束日期
     */
    @ApiModelProperty(value = "结束日期", name = "endDay")
    private Integer endDay;
    /**
     * 日排产量
     */
    @ApiModelProperty(value = "日排产量", name = "dayQty")
    private Long dayQty;

    /**
     * 硫化总工时
     */
    @ApiModelProperty(value = "硫化总工时", name = "totalVulcanizationMinutes")
    private BigDecimal totalVulcanizationMinutes;

    /**
     * 合模压力
     */
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    private BigDecimal mouldClampingPressure;

    /**
     * 模具型腔
     */
    @ApiModelProperty(value = "模具型腔", name = "moldCavity")
    private String moldCavity;

    /**
     * 未排产原因国际化
     */
    @ApiModelProperty(value = "未排产原因国际化", name = "reasonI18n")
    private String reasonI18n;
    /**
     * 每天单模最大硫化时间 --单位到秒
     */
    private BigDecimal dayMaxCuringTime;
    /**
     * 月份最大天数
     */
    private Integer maxDays;
    /**
     * 最大模具
     */
    private Set<String> maxMouldSet;
    /**
     * 换规格损耗时间
     */
    private BigDecimal changeProductConsumeTime;
    /**
     * 排产顺序
     */
    @ApiModelProperty(value = "排产顺序", name = "productionSequence")
    private Long productionSequence;
}