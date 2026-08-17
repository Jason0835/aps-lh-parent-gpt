package com.zlt.aps.mp.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直裁排程结果下发 MES 实体。
 * 每条直裁排程结果按启用的班次配置展开成多条 issue，一条 issue 对应一个班次一个日期。
 * 由 APS 侧 Cd90ScheduleResultIssueAssembler 装配后通过 IMesItfService.issueCd90ScheduleResult 下发到 MES 中间表。
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
public class Cd90ScheduleResultIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 直裁批次号 */
    private String cd90BatchNo;

    /** 对应成型批次号 */
    private String cxBatchNo;

    /** 工单号 */
    private String orderNo;

    /** 该条对应的班次排班日期（由 t_cd90_shift_config.SCHEDULE_DAY 推导） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date scheduleDate;

    /** 直裁机台编码 */
    private String machineCode;

    /** 帘布代号 */
    private String clothCode;

    /** 产出物类别，直裁固定为纤维 */
    private String outputType;

    /** 产出物代码 */
    private String outputCode;

    /** 产出物料号 */
    private String outputMaterialCode;

    /** 成型胎胚物料描述，多个规格使用斜杠拼接 */
    private String embryoSpecDesc;

    /** 帘布大卷编号 */
    private String bigRollCode;

    /** 库排号（多库排逗号拼接） */
    private String storageLaneCode;

    /** 班次名称（夜/早/中） */
    private String shiftName;

    /** 对应 CLASS_FIELD（class1~class8），用于 MES 侧识别班次槽位 */
    private String classField;

    /** 排程天数（1/2/3，来自 t_cd90_shift_config.SCHEDULE_DAY） */
    private Integer scheduleDay;

    /** 当天班次序号（来自 t_cd90_shift_config.DAY_SHIFT_ORDER） */
    private Integer dayShiftOrder;

    /** 该班次直裁计划量 */
    private Double planQty;

    /** 该班次对应成型计划量 */
    private Double cxPlanQty;

    /** 该班次完成量 */
    private Double finishQty;

    /** 该班次生产顺序 */
    private Integer produceOrder;

    /** 该班次完成率 */
    private Double finishRate;

    /** 该班次系统原因分析 */
    private String analysis;

    /** 该班次手工原因分析 */
    private String analysisInput;

    /** 单耗（成型一个胎耗多少米） */
    private Double unitConsume;

    /** 库存数量 */
    private Double stockQty;

    /** 库存供应成型时长，单位小时 */
    private Double supplyTime;

    /** 当前班次示方类型 */
    private String exampleType;

    /** 当前班次示方号 */
    private String exampleNo;

    /** 当前班次备注 */
    private String shiftRemark;

    /** 对应成型一班计划量 */
    private Double cxClass1Plan;

    /** 对应成型二班计划量 */
    private Double cxClass2Plan;

    /** 对应成型三班计划量 */
    private Double cxClass3Plan;

    /** 对应成型四班计划量 */
    private Double cxClass4Plan;

    /** 排程备注 */
    private String remark;

    /** 数据版本号 */
    private String dataVersion;

    /** 公司代码 */
    private String companyCode;

    /** 工厂代码 */
    private String factoryCode;

    /** 发布追踪 ID（APS 生成，贯穿日志与 MES 调用，便于关联排查） */
    private String publishTraceId;
}
