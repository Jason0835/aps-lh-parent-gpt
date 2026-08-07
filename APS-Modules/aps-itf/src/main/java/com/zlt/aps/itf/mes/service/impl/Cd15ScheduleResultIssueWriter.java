package com.zlt.aps.itf.mes.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.itf.mes.mapper.Cd15ScheduleResultIssueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 斜裁排程结果 MES 中间表事务写入器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15ScheduleResultIssueWriter {

    /** SQL Server 单次最多 2100 个参数，按每批 40 条留出安全余量。 */
    private static final int BATCH_SIZE = 40;

    private final Cd15ScheduleResultIssueMapper issueMapper;

    /**
     * 按业务键覆盖中间表数据，保证落库事务在发送 MES 通知前完成。
     *
     * @param issueList 下发记录
     * @param dataVersion 数据版本
     * @param companyCode 公司编码
     * @param factoryCode 工厂编码
     * @return 实际写入数量
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
        int insertedCount = 0;
        List<List<Cd15ScheduleResultIssue>> batchList =
                this.partition(issueList);
        for (List<Cd15ScheduleResultIssue> batch : batchList) {
            this.issueMapper.batchDeleteByBusinessKey(batch, factoryCode);
            int batchInsertedCount = this.issueMapper.batchInsert(
                    batch, dataVersion, companyCode, factoryCode);
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
     * 将下发记录分批，避免超过 SQL Server 参数上限。
     *
     * @param sourceList 原始记录
     * @return 分批结果
     */
    private List<List<Cd15ScheduleResultIssue>> partition(
            List<Cd15ScheduleResultIssue> sourceList) {
        List<List<Cd15ScheduleResultIssue>> batchList = new ArrayList<>();
        for (int index = 0; index < sourceList.size(); index += BATCH_SIZE) {
            batchList.add(sourceList.subList(index,
                    Math.min(index + BATCH_SIZE, sourceList.size())));
        }
        return batchList;
    }
}
