package com.zlt.aps.gsq.engine.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class GsqScheduleResultVo extends ApsBaseDto {

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_GSQ_SCHEDULE")
    private Long id;

    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的胎圈批次号")
    private String tqBatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    private String orderNo;

    @ApiModelProperty(value = "钢丝类型")
    private String steelType;

    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @ApiModelProperty(value = "排列")
    private String rank;

    @ApiModelProperty(value = "机台id")
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

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "是否发布")
    private String isRelease;

    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    /**
     * 对应成型一班的钢丝圈计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的钢丝圈计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的钢丝圈计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的钢丝圈计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的钢丝圈计划量
     */
    private Double cxClass5Plan;

    /**
     * 发布成功计数器，每点击一次发布并成功的话，计数器累加
     */
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    private Date newestPublishTime;

    /**
     * 不需要参与生产顺序排程tag。值不为空，表示不需要参与排序
     */
    private Integer notOrderTag;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;
}
