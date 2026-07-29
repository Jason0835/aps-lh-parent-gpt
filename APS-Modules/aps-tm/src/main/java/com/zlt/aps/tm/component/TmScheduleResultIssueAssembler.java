package com.zlt.aps.tm.component;

import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultIssue;
import com.zlt.aps.tm.service.mes.TmShiftBusinessDateResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 胎面六班排程结果MES下发组装器（对齐胎圈，4班issue，六班拆3天）。
 *
 * <p>一条六班 TmScheduleResult 拆为 3 条 TmScheduleResultIssue，分别对应 D日/D+1日/D+2日业务日期，
 * 班次映射见 {@link TmShiftBusinessDateResolver} 与 tm 详设 §18.1：</p>
 * <ul>
 *   <li>D日(排程日期-1)：1班->中班(mid)</li>
 *   <li>D+1日(排程日期)：2班->夜班(night)、3班->早班(day)、4班->中班(mid)</li>
 *   <li>D+2日(排程日期+1)：5班->夜班(night)、6班->早班(day)</li>
 * </ul>
 */
@Component
public class TmScheduleResultIssueAssembler {

    /**
     * BigDecimal安全转Double。
     *
     * @param value 数值
     * @return Double值，null返回null
     */
    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * 将六班结果列表拆分为MES三班业务日期下发记录。
     *
     * @param resultList 待发布结果
     * @param dataVersion 发布数据版本
     * @return MES下发记录
     */
    public List<TmScheduleResultIssue> assemble(List<TmScheduleResult> resultList, String dataVersion) {
        List<TmScheduleResultIssue> issueList = new ArrayList<>();
        if (resultList == null || resultList.isEmpty()) {
            return issueList;
        }
        for (TmScheduleResult result : resultList) {
            if (result == null || result.getId() == null || result.getScheduleDate() == null) {
                continue;
            }
            Date scheduleDate = result.getScheduleDate();
            Date dDay = TmShiftBusinessDateResolver.resolveMesBusinessDate(scheduleDate, 1);
            Date dPlus1Day = TmShiftBusinessDateResolver.resolveMesBusinessDate(scheduleDate, 2);
            Date dPlus2Day = TmShiftBusinessDateResolver.resolveMesBusinessDate(scheduleDate, 5);
            issueList.add(this.buildDay1Issue(result, dDay, dataVersion));
            issueList.add(this.buildDay2Issue(result, dPlus1Day, dataVersion));
            issueList.add(this.buildDay3Issue(result, dPlus2Day, dataVersion));
        }
        return issueList;
    }

    /**
     * D日：胎面1班->MES中班，夜班/早班/次日中班已过不下发。
     *
     * @param source 排程结果
     * @param dDay D日
     * @param dataVersion 数据版本
     * @return D日下发记录
     */
    private TmScheduleResultIssue buildDay1Issue(TmScheduleResult source, Date dDay, String dataVersion) {
        TmScheduleResultIssue issue = this.buildBaseIssue(source, dDay, dataVersion);
        issue.setMidPlanQty(toDouble(source.getClass1PlanQty()));
        issue.setMidProduceOrder(source.getClass1Sequence());
        return issue;
    }

    /**
     * D+1日：胎面2班->MES夜班、3班->早班、4班->中班，次日中班不下发。
     *
     * @param source 排程结果
     * @param dPlus1Day D+1日
     * @param dataVersion 数据版本
     * @return D+1日下发记录
     */
    private TmScheduleResultIssue buildDay2Issue(TmScheduleResult source, Date dPlus1Day, String dataVersion) {
        TmScheduleResultIssue issue = this.buildBaseIssue(source, dPlus1Day, dataVersion);
        issue.setNightPlanQty(toDouble(source.getClass2PlanQty()));
        issue.setNightProduceOrder(source.getClass2Sequence());
        issue.setDayPlanQty(toDouble(source.getClass3PlanQty()));
        issue.setDayProduceOrder(source.getClass3Sequence());
        issue.setMidPlanQty(toDouble(source.getClass4PlanQty()));
        issue.setMidProduceOrder(source.getClass4Sequence());
        return issue;
    }

    /**
     * D+2日：胎面5班->MES夜班、6班->早班，中班尚未排产不下发。
     *
     * @param source 排程结果
     * @param dPlus2Day D+2日
     * @param dataVersion 数据版本
     * @return D+2日下发记录
     */
    private TmScheduleResultIssue buildDay3Issue(TmScheduleResult source, Date dPlus2Day, String dataVersion) {
        TmScheduleResultIssue issue = this.buildBaseIssue(source, dPlus2Day, dataVersion);
        issue.setNightPlanQty(toDouble(source.getClass5PlanQty()));
        issue.setNightProduceOrder(source.getClass5Sequence());
        issue.setDayPlanQty(toDouble(source.getClass6PlanQty()));
        issue.setDayProduceOrder(source.getClass6Sequence());
        return issue;
    }

    /**
     * 创建MES业务日期下发记录并复制工艺快照。
     *
     * @param source 排程结果
     * @param scheduleDate MES业务日期
     * @param dataVersion 数据版本
     * @return MES下发记录
     */
    private TmScheduleResultIssue buildBaseIssue(TmScheduleResult source, Date scheduleDate, String dataVersion) {
        TmScheduleResultIssue issue = new TmScheduleResultIssue();
        issue.setScheduleDate(scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        issue.setBatchNo(source.getBatchNo());
        issue.setOrderNo(source.getOrderNo());
        issue.setTreadCode(source.getTreadCode());
        // SAP物料编码取结果表 MATERIAL_CODE，待核对是否需走主数据映射
        issue.setSapMaterialCode(source.getMaterialCode());
        issue.setGlueCode(source.getGlueCode());
        issue.setBaseGlueCode(source.getBaseGlueCode());
        issue.setWholeGlueCode(source.getWholeGlueCode());
        issue.setGlueSeq(source.getGlueSeq());
        issue.setMouthPlateCode(source.getMouthPlateCode());
        // specSize/unitConsume/supplyTime/markCloseOutTip/productionStatus 在 TmScheduleResult 无对应字段，待核对主数据来源后补齐
        issue.setMachineCode(source.getMachineCode());
        issue.setStockQty(toDouble(source.getSixClockStockQty()));
        // 生产状态固定未投产（对齐胎圈 TqScheduleResultServiceImpl.buildBaseIssue）
        issue.setProductionStatus(com.zlt.aps.common.core.constant.ApsConstant.NO_PRODUNTION);
        issue.setTailFlag(source.getTailFlag());
        issue.setFactoryCode(source.getFactoryCode());
        // 胎面结果表不冗余公司编码，MES契约按单工厂口径用工厂编码补齐公司编码
        issue.setCompanyCode(source.getFactoryCode());
        issue.setDataVersion(dataVersion);
        issue.setRemark(source.getRemark());
        return issue;
    }

    /**
     * 构造结果行发布幂等键。
     *
     * @param result 排程结果
     * @return 幂等键
     */
    public String buildIdempotencyKey(TmScheduleResult result) {
        return StrUtil.blankToDefault(result.getBatchNo(), "") + "|"
                + StrUtil.blankToDefault(result.getOrderNo(), "") + "|0";
    }
}
