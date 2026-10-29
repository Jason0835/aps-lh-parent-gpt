package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.domain.entity.LhEndingDiscard;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EndingDiscardCandidate;
import com.zlt.aps.lh.mapper.LhEndingDiscardMapper;
import com.zlt.aps.lh.service.ILhEndingDiscardService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 硫化收尾舍弃计划统一记录服务，消费最终未排与冻结候选并参与主结果事务。
 *
 * @author APS
 * @date 2026/10/28
 */
@Slf4j
@Service
public class LhEndingDiscardServiceImpl extends AbstractDocService<LhEndingDiscard>
        implements ILhEndingDiscardService {

    /**
     * 舍弃记录基础 Mapper，清理条件只使用业务键。
     */
    @Resource
    private LhEndingDiscardMapper endingDiscardMapper;
    /**
     * 项目统一批量保存入口。
     */
    @Resource
    private BaseDao baseDao;

    /**
     * 获取舍弃计划记录单据类型。
     *
     * @return 舍弃计划记录类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "LH_ENDING_DISCARD";
    }

    /**
     * 只读取最终未排和规则快照，不重新判断规则或修改任何排产账本。
     *
     * @param context 已完成主排程最终未排收口的上下文
     * @return 按 SKU 与原因去重的本批舍弃记录
     */
    @Override
    public List<LhEndingDiscard> buildRecords(LhScheduleContext context) {
        Map<String, LhEndingDiscard> records = new LinkedHashMap<>(16);
        for (LhUnscheduledResult result : context.getUnscheduledResultList()) {
            if (Objects.isNull(result) || Objects.isNull(result.getUnscheduledQty())
                    || result.getUnscheduledQty() <= 0) {
                continue;
            }
            String key = EndingDiscardCandidate.buildSkuReasonKey(result);
            List<EndingDiscardCandidate> candidates = context.getUnscheduledResultRuntime()
                    .getEndingDiscardCandidateMap().getOrDefault(key, Collections.emptyList());
            // 只取仍有效且属于该原始需求的首次确认，禁止累加重复来源或使用最终归并量兜底。
            EndingDiscardCandidate candidate = candidates.stream()
                    .filter(value -> value.matchesFinalResult(result)).findFirst().orElse(null);
            if (Objects.nonNull(candidate)) {
                records.computeIfAbsent(key, ignored -> this.buildRecord(context, candidate));
            }
        }
        return new ArrayList<>(records.values());
    }

    /**
     * 与主结果共同替换旧记录，任何写入异常均由主事务回滚。
     *
     * @param context 已完成主排程最终收口的上下文
     * @throws org.springframework.transaction.IllegalTransactionStateException 调用时没有主排程事务
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void replaceForSchedule(LhScheduleContext context) {
        List<LhEndingDiscard> records = this.buildRecords(context);
        Date targetDate = LhScheduleTimeUtil.clearTime(context.getScheduleTargetDate());
        int deletedCount = endingDiscardMapper.delete(new LambdaQueryWrapper<LhEndingDiscard>()
                .eq(LhEndingDiscard::getFactoryCode, context.getFactoryCode())
                .eq(LhEndingDiscard::getScheduleDate, targetDate));
        if (!records.isEmpty()) {
            baseDao.saveBatch(records);
        }
        log.info("硫化收尾舍弃计划替换完成, factoryCode: {}, scheduleDate: {}, batchNo: {}, deleted: {}, saved: {}",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(targetDate),
                context.getBatchNo(), deletedCount, records.size());
    }

    /**
     * 使用冻结的实际余量填充现有字段，原因复用公共备注字段。
     *
     * @param context 当前排程身份及操作人
     * @param candidate 最终未排确认仍有效的规则命中快照
     * @return 待保存舍弃记录
     */
    private LhEndingDiscard buildRecord(LhScheduleContext context, EndingDiscardCandidate candidate) {
        LhEndingDiscard record = new LhEndingDiscard();
        record.setFactoryCode(context.getFactoryCode());
        record.setScheduleDate(LhScheduleTimeUtil.clearTime(context.getScheduleTargetDate()));
        record.setBatchNo(context.getBatchNo());
        record.setMaterialCode(candidate.getMaterialCode());
        record.setProductStatus(candidate.getProductStatus());
        record.setDiscardPlanQty(candidate.getDiscardPlanQty());
        record.setRemark(candidate.getReasonCode());
        String operator = StringUtils.defaultIfBlank(context.getOperator(), "system");
        Date now = new Date();
        record.setCreateBy(operator);
        record.setUpdateBy(operator);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(0);
        return record;
    }
}
