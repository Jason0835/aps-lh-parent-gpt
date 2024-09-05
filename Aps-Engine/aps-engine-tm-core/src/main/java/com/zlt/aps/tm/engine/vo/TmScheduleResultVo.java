package com.zlt.aps.tm.engine.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class TmScheduleResultVo extends ApsBaseDto {

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
     * 规格描述信息
     */
    private String specDesc;

    /**
     * 施工代码，即胎胚代码
     */
    private String workCode;

    /**
     * 胎面代码
     */
    private String treadCode;

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
     * 补强/封口胶
     */
    private String reinforceSealGlue;

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
     * 对应成型一班的胎面胶计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的胎面胶计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的胎面胶计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的胎面胶计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的胎面胶计划量
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

    /**
     * 数据来源：0>自动排程；1>APS插单；2>导入；
     */
    private String dataSource;
    
    /**
     * 收尾规格标记，0：收尾1：非收尾
     */
    private String closeOutSpecFlag;
}
