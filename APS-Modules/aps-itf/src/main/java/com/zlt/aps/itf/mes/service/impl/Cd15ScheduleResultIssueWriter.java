package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.itf.mes.domain.MesCd15ScheduleResult;
import com.zlt.aps.itf.mes.mapper.Cd15ScheduleResultIssueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 斜裁排程结果 MES 中间表事务写入器。
 *
 * <p>APS 发布载荷按自然班次展开，本写入器先按业务键合并为
 * MES_CD15_SCHEDULE_RESULT 夜班、早班、中班宽表，再按业务键覆盖写入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleResultIssueWriter {

    /** SQL Server 单次最多 2100 个参数，宽表按每批 20 条控制。 */
    private static final int BATCH_SIZE = 20;

    private final Cd15ScheduleResultIssueMapper issueMapper;

    /**
     * 聚合并覆盖 MES 中间表数据。
     *
     * @param issueList 班次级下发记录
     * @param dataVersion 数据版本
     * @param companyCode 公司编码
     * @param factoryCode 工厂编码
     * @return 实际写入宽表数量
     */
    @DSTransactional
    public int replace(
            List<Cd15ScheduleResultIssue> issueList,
            String dataVersion,
            String companyCode,
            String factoryCode) {
        if (CollectionUtils.isEmpty(issueList)) {
            return 0;
        }
        List<MesCd15ScheduleResult> rows = this.toWideRows(
                issueList, dataVersion, companyCode, factoryCode);
        this.validateRequiredFields(rows);
        int insertedCount = 0;
        for (int index = 0; index < rows.size(); index += BATCH_SIZE) {
            List<MesCd15ScheduleResult> batch = rows.subList(index,
                    Math.min(index + BATCH_SIZE, rows.size()));
            this.issueMapper.batchDeleteByBusinessKey(batch, factoryCode);
            int batchInsertedCount = this.issueMapper.batchInsert(batch);
            if (batchInsertedCount != batch.size()) {
                throw new IllegalStateException(
                        "MES_CD15_SCHEDULE_RESULT写入数量不一致");
            }
            insertedCount += batchInsertedCount;
        }
        log.info("斜裁排程结果已写入MES中间表, dataVersion={}, rowCount={}",
                dataVersion, insertedCount);
        return insertedCount;
    }

    /**
     * 将班次级下发记录聚合为自然日宽表。
     *
     * @param issueList 班次级下发记录
     * @param dataVersion 数据版本
     * @param companyCode 公司编码
     * @param factoryCode 工厂编码
     * @return MES 宽表记录
     */
    List<MesCd15ScheduleResult> toWideRows(
            List<Cd15ScheduleResultIssue> issueList,
            String dataVersion,
            String companyCode,
            String factoryCode) {
        Map<String, MesCd15ScheduleResult> rowByBusinessKey =
                new LinkedHashMap<>();
        issueList.forEach(issue -> {
            String businessKey = this.businessKey(issue, factoryCode);
            MesCd15ScheduleResult row = rowByBusinessKey.computeIfAbsent(
                    businessKey,
                    key -> this.newBaseRow(issue, dataVersion,
                            companyCode, factoryCode));
            this.applyShift(row, issue);
        });
        return new ArrayList<>(rowByBusinessKey.values());
    }

    /** 构造不含班次列的基础宽表记录。 */
    private MesCd15ScheduleResult newBaseRow(
            Cd15ScheduleResultIssue issue,
            String dataVersion,
            String companyCode,
            String factoryCode) {
        Date now = new Date();
        MesCd15ScheduleResult row = new MesCd15ScheduleResult();
        row.setScheduleDate(issue.getScheduleDate());
        row.setSplitBatchNo("SPLIT".equalsIgnoreCase(issue.getCutMode())
                ? issue.getGroupNo() : null);
        row.setBatchNo(issue.getCd15BatchNo());
        row.setOrderNo(issue.getOrderNo());
        row.setBigRollCode(issue.getBigRollCode());
        row.setMachineCode(issue.getMachineCode());
        row.setStockCode(issue.getStorageLaneCode());
        row.setEmbryoSpecDesc(issue.getEmbryoSpecDesc());
        row.setSteelStripCode1(issue.getSteelStripCode());
        row.setMaterialCode1(issue.getMaterialCode());
        row.setUnitConsume1(BigDecimalUtils.valueOf(
                issue.getUnitConsume()));
        row.setStock1Qty1(BigDecimalUtils.valueOf(issue.getStockQty()));
        row.setCuttingAngle(BigDecimalUtils.valueOf(
                issue.getCuttingAngle()));
        row.setSupplyTime1(BigDecimalUtils.valueOf(issue.getSupplyTime()));
        row.setCxClass1Plan(BigDecimalUtils.valueOf(
                issue.getCxClass1Plan()));
        row.setCxClass2Plan(BigDecimalUtils.valueOf(
                issue.getCxClass2Plan()));
        row.setCxClass3Plan(BigDecimalUtils.valueOf(
                issue.getCxClass3Plan()));
        row.setCxClass5Plan(BigDecimalUtils.valueOf(
                issue.getCxClass4Plan()));
        row.setMarkCloseOutTip(issue.getMarkCloseOutTip());
        row.setRemark(issue.getRemark());
        row.setDelFlag("0");
        row.setCreateBy("APS");
        row.setCreateTime(now);
        row.setUpdateBy("APS");
        row.setUpdateTime(now);
        row.setDataVersion(dataVersion);
        row.setCompanyCode(companyCode);
        row.setFactoryCode(factoryCode);
        return row;
    }

    /** 按班次名称写入夜、早、中对应列。 */
    private void applyShift(
            MesCd15ScheduleResult row,
            Cd15ScheduleResultIssue issue) {
        String shiftName = StringUtils.trimToEmpty(issue.getShiftName());
        if (shiftName.contains("夜")
                || "NIGHT".equalsIgnoreCase(shiftName)) {
            row.setNightPlanQty1(BigDecimalUtils.valueOf(issue.getPlanQty()));
            row.setNightProduceOrder1(issue.getProduceOrder());
            row.setNightSysAnalysis1(issue.getAnalysis());
            row.setNightHandAnalysis1(issue.getAnalysisInput());
            row.setNightExampleType(issue.getExampleType());
            row.setNightExampleNo(issue.getExampleNo());
            row.setNightRemark(issue.getShiftRemark());
            return;
        }
        if (shiftName.contains("早")
                || "DAY".equalsIgnoreCase(shiftName)) {
            row.setDayPlanQty1(BigDecimalUtils.valueOf(issue.getPlanQty()));
            row.setDayProduceOrder1(issue.getProduceOrder());
            row.setDaySysAnalysis1(issue.getAnalysis());
            row.setDayHandAnalysis1(issue.getAnalysisInput());
            row.setDayExampleType(issue.getExampleType());
            row.setDayExampleNo(issue.getExampleNo());
            row.setDayRemark(issue.getShiftRemark());
            return;
        }
        if (shiftName.contains("中")
                || "MID".equalsIgnoreCase(shiftName)) {
            row.setMidPlanQty1(BigDecimalUtils.valueOf(issue.getPlanQty()));
            row.setMidProduceOrder1(issue.getProduceOrder());
            row.setMidSysAnalysis1(issue.getAnalysis());
            row.setMidHandAnalysis1(issue.getAnalysisInput());
            row.setMidExampleType(issue.getExampleType());
            row.setMidExampleNo(issue.getExampleNo());
            row.setMidRemark(issue.getShiftRemark());
            return;
        }
        throw new IllegalArgumentException(
                "无法识别斜裁发布班次: " + shiftName);
    }

    /** 生成不含数据版本号的覆盖业务键。 */
    private String businessKey(
            Cd15ScheduleResultIssue issue, String factoryCode) {
        String splitBatchNo = "SPLIT".equalsIgnoreCase(issue.getCutMode())
                ? issue.getGroupNo() : null;
        return String.join("|",
                StringUtils.defaultString(factoryCode),
                issue.getScheduleDate() == null ? ""
                        : String.valueOf(issue.getScheduleDate().getTime()),
                StringUtils.defaultString(splitBatchNo),
                StringUtils.defaultString(issue.getCd15BatchNo()),
                StringUtils.defaultString(issue.getOrderNo()),
                StringUtils.defaultString(issue.getMachineCode()),
                StringUtils.defaultString(issue.getBigRollCode()),
                StringUtils.defaultString(issue.getSteelStripCode()),
                StringUtils.defaultString(issue.getCuttingAngle()));
    }

    /** 校验 MES 契约中的必填字段，避免写入残缺数据。 */
    private void validateRequiredFields(List<MesCd15ScheduleResult> rows) {
        MesCd15ScheduleResult invalid = rows.stream()
                .filter(row -> row.getScheduleDate() == null
                        || StringUtils.isAnyBlank(row.getBatchNo(),
                        row.getOrderNo(), row.getBigRollCode(),
                        row.getMachineCode(), row.getStockCode(),
                        row.getEmbryoSpecDesc(), row.getSteelStripCode1(),
                        row.getMaterialCode1(), row.getDataVersion(),
                        row.getCompanyCode(), row.getFactoryCode()))
                .findFirst()
                .orElse(null);
        if (invalid != null) {
            throw new IllegalArgumentException(
                    "斜裁排程结果缺少MES必填字段, orderNo="
                            + invalid.getOrderNo());
        }
    }
}
