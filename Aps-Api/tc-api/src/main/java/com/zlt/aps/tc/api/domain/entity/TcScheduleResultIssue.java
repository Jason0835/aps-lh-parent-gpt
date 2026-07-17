package com.zlt.aps.tc.api.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 胎侧排程结果下发MES契约对象。
 *
 * <p>该对象仅用于APS到MES的数据传输，不是MyBatis数据库实体。六班结果按MES业务日期
 * 拆成前一日、排程日和后一日最多三条记录。</p>
 */
@Data
@ApiModel(value = "胎侧排程结果下发对象", description = "胎侧六班排程拆分后的MES接口记录")
public class TcScheduleResultIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /** MES目标业务日期。 */
    @ApiModelProperty(value = "MES目标业务日期", name = "scheduleDate")
    private LocalDate scheduleDate;

    /** 排程批次号。 */
    @ApiModelProperty(value = "排程批次号", name = "batchNo")
    private String batchNo;

    /** 胎侧工单号。 */
    @ApiModelProperty(value = "胎侧工单号", name = "orderNo")
    private String orderNo;

    /** 胎侧编码。 */
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    private String sidewallCode;

    /** 机台编码。 */
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    private String machineCode;

    /** 主胶料编码。 */
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    private String glueCode;

    /** 基部胶编码。 */
    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    private String baseGlueCode;

    /** 整条胶料组合编码。 */
    @ApiModelProperty(value = "整条胶料组合编码", name = "wholeGlueCode")
    private String wholeGlueCode;

    /** 胶料顺序。 */
    @ApiModelProperty(value = "胶料顺序", name = "glueSeq")
    private String glueSeq;

    /** 口型板编码。 */
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    private String mouthPlateCode;

    /** 施工版本。 */
    @ApiModelProperty(value = "施工版本", name = "constructionVersion")
    private String constructionVersion;

    /** 胎侧工艺。 */
    @ApiModelProperty(value = "胎侧工艺", name = "sidewallCraft")
    private String sidewallCraft;

    /** 中班计划量。 */
    @ApiModelProperty(value = "中班计划量", name = "midPlanQty")
    private BigDecimal midPlanQty;

    /** 中班生产顺序。 */
    @ApiModelProperty(value = "中班生产顺序", name = "midProduceOrder")
    private Integer midProduceOrder;

    /** 中班原因分析。 */
    @ApiModelProperty(value = "中班原因分析", name = "midAnalysis")
    private String midAnalysis;

    /** 夜班计划量。 */
    @ApiModelProperty(value = "夜班计划量", name = "nightPlanQty")
    private BigDecimal nightPlanQty;

    /** 夜班生产顺序。 */
    @ApiModelProperty(value = "夜班生产顺序", name = "nightProduceOrder")
    private Integer nightProduceOrder;

    /** 夜班原因分析。 */
    @ApiModelProperty(value = "夜班原因分析", name = "nightAnalysis")
    private String nightAnalysis;

    /** 早班计划量。 */
    @ApiModelProperty(value = "早班计划量", name = "dayPlanQty")
    private BigDecimal dayPlanQty;

    /** 早班生产顺序。 */
    @ApiModelProperty(value = "早班生产顺序", name = "dayProduceOrder")
    private Integer dayProduceOrder;

    /** 早班原因分析。 */
    @ApiModelProperty(value = "早班原因分析", name = "dayAnalysis")
    private String dayAnalysis;

    /** 是否收尾任务。 */
    @ApiModelProperty(value = "是否收尾任务", name = "tailFlag")
    private String tailFlag;

    /** 发布任务数据版本。 */
    @ApiModelProperty(value = "发布任务数据版本", name = "dataVersion")
    private String dataVersion;

    /** 结果行任务版本。 */
    @ApiModelProperty(value = "结果行任务版本", name = "taskVersion")
    private Long taskVersion;

    /** MES幂等键。 */
    @ApiModelProperty(value = "MES幂等键", name = "idempotencyKey")
    private String idempotencyKey;

    /** 公司编码。 */
    @ApiModelProperty(value = "公司编码", name = "companyCode")
    private String companyCode;

    /** 工厂编码。 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;
}
