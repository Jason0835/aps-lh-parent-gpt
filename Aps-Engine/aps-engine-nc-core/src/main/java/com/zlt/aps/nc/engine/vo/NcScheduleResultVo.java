package com.zlt.aps.nc.engine.vo;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import lombok.Data;

import java.util.Date;

@Data
public class NcScheduleResultVo extends ApsBaseDto {

    private Long id;

    /**
     * 排程日期
     */
    private Date scheduleDate;

    /**
     * 对应的成型批次号
     */
    private String cxBatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    private String orderNo;

    /**
     * 内衬代码
     */
    private String liningCode;

    /**
     * 胶料代码
     */
    private String glueCode;

    /**
     * 胶料代码(完整没有截取的)
     */
    private String wholeGlueCode;

    /**
     * 胶料序号
     */
    private String glueSeq;

    /**
     * 口型板代码
     */
    private String mouthPlateCode;

    /**
     * 单耗（毫米）
     */
    private Double unitConsume;

    /**
     * 机台ID
     */
    private String machineId;

    /**
     * 库存数量
     */
    private Double stockQty;

    /**
     * 库存供应成型时长，单位：小时
     */
    private Double supplyTime;

    /**
     * 中班(12点-24点)计划量
     */
    private Double dayPlanQty;

    /**
     * 中班(12点-24点)完成量
     */
    private Double dayFinishQty;

    /**
     * 中班(12点-24点)生产顺序
     */
    private Integer dayProduceOrder;

    /**
     * 中班(12点-24点)完成率
     */
    private Double dayFinishRate;

    /**
     * 中班(12点-24点)系统原因分析
     */
    private String daySysAnalysis;

    /**
     * 中班(12点-24点)手动输入原因分析
     */
    private String dayHandAnalysis;

    /**
     * 夜班(0点-12点)计划量
     */
    private Double nightPlanQty;

    /**
     * 夜班(0点-12点)完成量
     */
    private Double nightFinishQty;

    /**
     * 夜班(0点-12点)生产顺序
     */
    private Integer nightProduceOrder;

    /**
     * 夜班(0点-12点)完成率
     */
    private Double nightFinishRate;

    /**
     * 夜班(0点-12点)系统原因分析
     */
    private String nightSysAnalysis;

    /**
     * 夜班(0点-12点)手动输入原因分析
     */
    private String nightHandAnalysis;

    /**
     * 预计划
     */
    private Double prePlanQty;

    /**
     * 对应成型一班的内衬胶计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的内衬胶计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的内衬胶计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的内衬胶计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的内衬胶计划量
     */
    private Double cxClass5Plan;
    
    /**
     * 剩余量
     */
    private double surplusQty;

    /**
     * 发布成功计数器，每点击一次发布并成功的话，计数器累加
     */
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    private Date newestPublishTime;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    private String isRelease;

//    /**
//     * 成型一班计划数
//     */
//    private Integer class1PlanQty;
//
//    /**
//     * 成型二班计划数
//     */
//    private Integer class2PlanQty;
//
//    /**
//     * 成型三班计划数
//     */
//    private Integer class3PlanQty;
//
//    /**
//     * 成型次日一班计划数
//     */
//    private Integer class4PlanQty;
//
//    /**
//     * 成型次日二班计划数
//     */
//    private Integer class5PlanQty;

    /**
     * 机台code$胎胚代码，多个逗号分割， 用来计算成型平均定额使用
     */
    private String quotaKeys;

    /**
     * 收尾提示标识(0:提示收尾；1:不需要提示)
     */
    private String markCloseOutTip;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    private String productionStatus;

    private String remark;

    //数据来源：0>自动排程；1>APS插单；2>导入；
    private String dataSource;
    
    /**
     * 收尾规格标记，0：收尾1：非收尾
     */
    private String closeOutSpecFlag;

    /**
     * 预计库存，晚班（19点）的剩余库存，仅用于计算可供时长
     */
    private Double planStockQty;

    /**
     * 上一天早班计划
     */
    private Double lastMidPlanQty;

    /**
     * 夜班与早班的交接班库存
     */
    private double classStock;

    /**
     * 库存供需比例，交接班库存/成型一天需求量
     */
    private double supplyDemandRatio;

    /**
     * 明日早班计划
     */
    private double nextDayPlanQty;
    
    /**
     * 是否均分
     */
    private Boolean isEqualShare;
}
