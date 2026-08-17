package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.itf.mes.domain.MesCd90ScheduleResult;
import com.zlt.aps.itf.mes.mapper.Cd90ScheduleResultIssueMapper;
import com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue;
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
 * 直裁排程结果 MES 中间表事务写入器。
 *
 * <p>APS 发布载荷按自然班次展开，本写入器先按业务键合并为
 * MES_CD90_SCHEDULE_RESULT 夜班、早班、中班宽表，再按业务键覆盖写入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90ScheduleResultIssueWriter {

    /** SQL Server 单次最多 2100 个参数，47列宽表按每批20条控制。 */
    private static final int BATCH_SIZE = 20;

    private final Cd90ScheduleResultIssueMapper issueMapper;

    /**
     * 聚合并覆盖 MES 中间表数据。
     *
     * @param issueList 班次级下发记录
     * @param dataVersion 数据版本号
     * @param companyCode 分公司编码
     * @param factoryCode 工厂编码
     * @return 实际写入宽表数量
     */
    @DSTransactional
    public int replace(List<Cd90ScheduleResultIssue> issueList,
                       String dataVersion,
                       String companyCode,
                       String factoryCode) {
        if (CollectionUtils.isEmpty(issueList)) {
            return 0;
        }
        List<MesCd90ScheduleResult> rows = this.toWideRows(issueList,
                dataVersion, companyCode, factoryCode);
        this.validateRequiredFields(rows);
        int insertedCount = 0;
        for (int index = 0; index < rows.size(); index += BATCH_SIZE) {
            List<MesCd90ScheduleResult> batch = rows.subList(index,
                    Math.min(index + BATCH_SIZE, rows.size()));
            this.issueMapper.batchDeleteByBusinessKey(batch, factoryCode);
            int batchInsertedCount = this.issueMapper.batchInsert(batch);
            if (batchInsertedCount != batch.size()) {
                throw new IllegalStateException(
                        "MES_CD90_SCHEDULE_RESULT写入数量不一致");
            }
            insertedCount += batchInsertedCount;
        }
        log.info("直裁排程结果已写入MES中间表, dataVersion={}, rowCount={}",
                dataVersion, insertedCount);
        return insertedCount;
    }

    /**
     * 将班次级下发记录聚合为自然日宽表。
     *
     * @param issueList 班次级下发记录
     * @param dataVersion 数据版本号
     * @param companyCode 分公司编码
     * @param factoryCode 工厂编码
     * @return MES宽表记录
     */
    List<MesCd90ScheduleResult> toWideRows(
            List<Cd90ScheduleResultIssue> issueList,
            String dataVersion,
            String companyCode,
            String factoryCode) {
        Map<String, MesCd90ScheduleResult> rowByBusinessKey =
                new LinkedHashMap<>();
        issueList.forEach(issue -> {
            String businessKey = this.businessKey(issue, factoryCode);
            MesCd90ScheduleResult row = rowByBusinessKey.computeIfAbsent(
                    businessKey, key -> this.newBaseRow(issue, dataVersion,
                            companyCode, factoryCode));
            this.applyShift(row, issue);
        });
        return new ArrayList<>(rowByBusinessKey.values());
    }

    /** 构造不含班次列的基础宽表记录。 */
    private MesCd90ScheduleResult newBaseRow(
            Cd90ScheduleResultIssue issue,
            String dataVersion,
            String companyCode,
            String factoryCode) {
        Date now = new Date();
        MesCd90ScheduleResult row = new MesCd90ScheduleResult();
        row.setScheduleDate(issue.getScheduleDate());
        row.setCxBatchNo(issue.getCxBatchNo());
        row.setBatchNo(issue.getCd90BatchNo());
        row.setOrderNo(issue.getOrderNo());
        row.setBigRollCode(issue.getBigRollCode());
        row.setOutputType(issue.getOutputType());
        row.setOutputCode(issue.getOutputCode());
        row.setOutputMaterialCode(issue.getOutputMaterialCode());
        row.setEmbryoSpecDesc(issue.getEmbryoSpecDesc());
        row.setUnitConsume(BigDecimalUtils.valueOf(issue.getUnitConsume()));
        row.setMachineCode(issue.getMachineCode());
        row.setStockCode(issue.getStorageLaneCode());
        row.setStockQty(BigDecimalUtils.valueOf(issue.getStockQty()));
        row.setSupplyTime(BigDecimalUtils.valueOf(issue.getSupplyTime()));
        row.setCxClass1Plan(BigDecimalUtils.valueOf(
                issue.getCxClass1Plan()));
        row.setCxClass2Plan(BigDecimalUtils.valueOf(
                issue.getCxClass2Plan()));
        row.setCxClass3Plan(BigDecimalUtils.valueOf(
                issue.getCxClass3Plan()));
        row.setCxClass4Plan(BigDecimalUtils.valueOf(
                issue.getCxClass4Plan()));
        row.setRemark(issue.getRemark());
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
    private void applyShift(MesCd90ScheduleResult row,
                            Cd90ScheduleResultIssue issue) {
        String shiftName = StringUtils.trimToEmpty(issue.getShiftName());
        if (shiftName.contains("夜")
                || "NIGHT".equalsIgnoreCase(shiftName)) {
            row.setNightPlanQty(BigDecimalUtils.valueOf(issue.getPlanQty()));
            row.setNightProduceOrder(issue.getProduceOrder());
            row.setNightSysAnalysis(issue.getAnalysis());
            row.setNightHandAnalysis(issue.getAnalysisInput());
            row.setNightExampleType(issue.getExampleType());
            row.setNightExampleNo(issue.getExampleNo());
            row.setNightRemark(issue.getShiftRemark());
            return;
        }
        if (shiftName.contains("早")
                || "DAY".equalsIgnoreCase(shiftName)) {
            row.setDayPlanQty(BigDecimalUtils.valueOf(issue.getPlanQty()));
            row.setDayProduceOrder(issue.getProduceOrder());
            row.setDaySysAnalysis(issue.getAnalysis());
            row.setDayHandAnalysis(issue.getAnalysisInput());
            row.setDayExampleType(issue.getExampleType());
            row.setDayExampleNo(issue.getExampleNo());
            row.setDayRemark(issue.getShiftRemark());
            return;
        }
        if (shiftName.contains("中")
                || "MID".equalsIgnoreCase(shiftName)) {
            row.setMidPlanQty(BigDecimalUtils.valueOf(issue.getPlanQty()));
            row.setMidProduceOrder(issue.getProduceOrder());
            row.setMidSysAnalysis(issue.getAnalysis());
            row.setMidHandAnalysis(issue.getAnalysisInput());
            row.setMidExampleType(issue.getExampleType());
            row.setMidExampleNo(issue.getExampleNo());
            row.setMidRemark(issue.getShiftRemark());
            return;
        }
        throw new IllegalArgumentException("无法识别直裁发布班次: " + shiftName);
    }

    /** 生成不含版本号的覆盖业务键。 */
    private String businessKey(Cd90ScheduleResultIssue issue,
                               String factoryCode) {
        return String.join("|",
                StringUtils.defaultString(factoryCode),
                issue.getScheduleDate() == null ? ""
                        : String.valueOf(issue.getScheduleDate().getTime()),
                StringUtils.defaultString(issue.getCd90BatchNo()),
                StringUtils.defaultString(issue.getOrderNo()),
                StringUtils.defaultString(issue.getMachineCode()),
                StringUtils.defaultString(issue.getBigRollCode()),
                StringUtils.defaultString(issue.getOutputCode()));
    }

    /** 校验 MES 契约中的必填字段，避免把残缺数据写入中间库。 */
    private void validateRequiredFields(List<MesCd90ScheduleResult> rows) {
        MesCd90ScheduleResult invalid = rows.stream()
                .filter(row -> row.getScheduleDate() == null
                        || StringUtils.isAnyBlank(row.getBatchNo(),
                        row.getOrderNo(), row.getBigRollCode(),
                        row.getOutputType(), row.getOutputCode(),
                        row.getOutputMaterialCode(), row.getEmbryoSpecDesc(),
                        row.getMachineCode(), row.getStockCode(),
                        row.getDataVersion(), row.getCompanyCode(),
                        row.getFactoryCode()))
                .findFirst()
                .orElse(null);
        if (invalid != null) {
            throw new IllegalArgumentException(
                    "直裁排程结果缺少MES必填字段, orderNo="
                            + invalid.getOrderNo());
        }
    }
}
