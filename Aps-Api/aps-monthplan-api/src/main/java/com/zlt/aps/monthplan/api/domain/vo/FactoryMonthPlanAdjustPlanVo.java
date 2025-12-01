package com.zlt.aps.monthplan.api.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.monthplan.api.enums.MouldProductionLogType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 月份计划计划调整参数对象
 *
 * @author ZLT
 * @date 20250331
 */
@Data
@ApiModel(value = "月份计划-计划调整参数对象", description = "月份计划-计划调整参数对象")
public class FactoryMonthPlanAdjustPlanVo implements Serializable {
    /**
     * 分厂编码
     */
    @ApiModelProperty(value = "分厂编码", name = "factoryCode")
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
     * 排产制造单号
     */
    @ApiModelProperty(value = "排产制造单号", name = "productionNo")
    private String productionNo;

    /**
     * 生产物料编号
     */
    @ApiModelProperty(value = "生产物料编号", name = "productCode")
    private String productCode;
    /**
     * 模具
     */
    @ApiModelProperty(value = "模具", name = "mouldNo")
    private String mouldNo;
    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;
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
     * 起始的调整日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "起始的调整日期", name = "startDate")
    private Date startDate;
    /**
     * 调整的数量 正数为调增，负数为调减
     */
    @ApiModelProperty(value = "调整的数量 正数为调增，负数为调减", name = "adjustNumber")
    private Integer adjustNumber;

    /**
     * 单条硫化时间 --加入间隔时间
     */
    @ApiModelProperty(hidden = true)
    private BigDecimal curingTime;
    /**
     * 单天单模最大硫化时间 --单位到秒
     */
    @ApiModelProperty(hidden = true)
    private BigDecimal dayMaxCuringTime;

    /**
     * 推荐需要调减的排产制造计划
     */
    @ApiModelProperty(value = "推荐需要调减的排产制造计划--用户前端进行调减确认", name = "planSubtractList")
    private List<FactoryMonthPlanProdFinalVo> planSubtractList;
    /**
     * 需要其他计划减量时的最大可增量数量
     */
    @ApiModelProperty(value = "需要其他计划减量时的最大可增量数量", name = "maxAddQty")
    private Long maxAddQty;
    /**
     * 确认需要调减的排产制造计划
     */
    @ApiModelProperty(value = "确认需要调减的排产制造计划", name = "confirmSubtractList")
    private List<FactoryMonthPlanAdjustPlanVo> confirmSubtractList;

    /**
     * 日志存储器
     */
    @ApiModelProperty(hidden = true)
    private StringBuilder logBuilder;
    /**
     * 版本信息
     */
    @ApiModelProperty(hidden = true)
    private FactoryMonthPlanFinalVersionInfoVo finalVersionInfo;

    /**
     * 施工代号
     */
    @ApiModelProperty(hidden = true)
    private String constructionCode;

    /**
     * 硫化规格信息
     */
    @ApiModelProperty(hidden = true)
    private String specCodeInfo;

    /**
     * 保存模具排程排产流程日志
     *
     * @param productionNo   调整计划编号
     * @param logContentInfo 日志内容
     */
    public final void addAdjustProductionLog(String productionNo, String logContentInfo) {
        if (StringUtils.isBlank(logContentInfo)) {
            return;
        }
        MouldProductionLogType logType = MouldProductionLogType.PLAN_ADJUST_LOG;
        String logContent = String.format("调整版本号：%s 排产计划编号: %s -阶段：%s 内容： %s", finalVersionInfo.getProductionVersion(), productionNo, logType.getDesc(), logContentInfo);
        logBuilder.append(logContent).append(System.lineSeparator()).append("===================").append(System.lineSeparator());
    }
}
