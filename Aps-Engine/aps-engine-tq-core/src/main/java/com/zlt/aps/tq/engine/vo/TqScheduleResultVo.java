package com.zlt.aps.tq.engine.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class TqScheduleResultVo extends ApsBaseDto {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的成型批次号")
    private String cxBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    private String orderNo;

    @ApiModelProperty(value = "胎圈代码")
    private String beadCode;

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "三角胶代码")
    private String triangleGlueCode;

    @ApiModelProperty(value = "胶料代码")
    private String glueCode;

    @ApiModelProperty(value = "口型板代码")
    private String mouthPlateCode;

    @ApiModelProperty(value = "尺寸")
    private String specSize;

    @ApiModelProperty(value = "机台ID，多个逗号分割")
    private String machineId;

    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    @ApiModelProperty(value = "库存数量")
    private Double stockQty;

    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    private Double supplyTime;

    @ApiModelProperty(value = "中班(16点-24点)计划量(条)")
    private Double midPlanQty;

    @ApiModelProperty(value = "中班(16点-24点)生产顺序")
    private Integer midProduceOrder;

    @ApiModelProperty(value = "中班(16点-24点)系统原因分析")
    private String midSysAnalysis;

    @ApiModelProperty(value = "中班(16点-24点)手动输入原因分析")
    private String midHandAnalysis;

    @ApiModelProperty(value = "夜班(0点-8点)计划量(条)")
    private Double nightPlanQty;

    @ApiModelProperty(value = "夜班(0点-8点)生产顺序")
    private Integer nightProduceOrder;

    @ApiModelProperty(value = "夜班(0点-8点)系统原因分析")
    private String nightSysAnalysis;

    @ApiModelProperty(value = "夜班(0点-8点)手动输入原因分析")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "白班(8点-16点)计划量(条)")
    private Double dayPlanQty;

    @ApiModelProperty(value = "白班(8点-16点)生产顺序")
    private Integer dayProduceOrder;

    @ApiModelProperty(value = "白班(8点-16点)系统原因分析")
    private String daySysAnalysis;

    @ApiModelProperty(value = "白班(8点-16点)手动输入原因分析")
    private String dayHandAnalysis;

    @ApiModelProperty(value = "次日中班(16点-24点)计划量(条)")
    private Double nextMidPlanQty;

    @ApiModelProperty(value = "次日中班(16点-24点)生产顺序")
    private Integer nextMidProduceOrder;

    @ApiModelProperty(value = "次日中班(16点-24点)系统原因分析")
    private String nextMidSysAnalysis;

    @ApiModelProperty(value = "次日中班(16点-24点)手动输入原因分析")
    private String nextMidHandAnalysis;

    @ApiModelProperty(value = "对应成型一班的计划量")
    private Double cxClass1Plan;

    @ApiModelProperty(value = "对应成型二班的计划量")
    private Double cxClass2Plan;

    @ApiModelProperty(value = "对应成型三班的计划量")
    private Double cxClass3Plan;

    @ApiModelProperty(value = "对应成型次一班的计划量")
    private Double cxClass4Plan;

    @ApiModelProperty(value = "对应成型次二班的计划量")
    private Double cxClass5Plan;

    /**
     * 发布成功计数器，每点击一次发布并成功的话，计数器累加
     */
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    private Date newestPublishTime;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "生产状态")
    private String productionStatus;
    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

}
