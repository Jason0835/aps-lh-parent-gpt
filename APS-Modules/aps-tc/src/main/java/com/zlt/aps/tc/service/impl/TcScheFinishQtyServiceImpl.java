package com.zlt.aps.tc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.mapper.TcScheFinishQtyMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.ITcScheFinishQtyService;
import com.zlt.aps.tc.service.mes.TcShiftBusinessDateResolver;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧班次完成量MES快照服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TcScheFinishQtyServiceImpl extends AbstractDocService<TcScheFinishQty>
        implements ITcScheFinishQtyService {

    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcScheFinishQtyMapper scheFinishQtyMapper;

    /**
     * 获取内部单据类型编码。
     *
     * @return 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "TC_SCHE_FINISH";
    }

    /**
     * 失效旧快照并批量保存MES最新完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param insertList 完成量列表
     * @throws ServiceException 参数无效或持久化失败时抛出
     */
    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy,
                                        List<TcScheFinishQty> insertList) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.mes.finishArgumentsInvalid"));
        }
        int deleteCount = this.scheFinishQtyMapper.update(null, new LambdaUpdateWrapper<TcScheFinishQty>()
                .eq(TcScheFinishQty::getFactoryCode, factoryCode)
                .eq(TcScheFinishQty::getScheduleDate, scheduleDate)
                .set(TcScheFinishQty::getIsDelete, 1)
                .set(TcScheFinishQty::getUpdateBy, updateBy)
                .set(TcScheFinishQty::getUpdateTime, new Date()));
        if (CollectionUtils.isNotEmpty(insertList)) {
            insertList.stream().forEach(item -> {
                item.setFactoryCode(factoryCode);
                item.setScheduleDate(scheduleDate);
                item.setCreateBy(updateBy);
            });
            baseDao.saveBatch(insertList);
        }
        log.info("胎侧班次完成量同步完成, factoryCode={}, scheduleDate={}, invalidated={}, inserted={}",
                factoryCode, scheduleDate, deleteCount, CollectionUtils.size(insertList));
    }

    /**
     * 将MES三班完成量回写胎侧六班排程结果。
     *
     * <p>回写仅更新 `classNFinishQty`，不会修改任务版本和发布状态。六班映射由
     * {@link TcShiftBusinessDateResolver} 统一解析。</p>
     *
     * @param finishQtyList MES完成量列表
     * @return 回写数量及未匹配数量摘要
     * @throws ServiceException 完成量为负数时抛出
     */
    @Override
    public AjaxResult writeBackScheduleResultFinishQty(List<TcScheFinishQty> finishQtyList) {
        if (CollectionUtils.isEmpty(finishQtyList)) {
            return AjaxResult.success(this.buildWriteBackSummary(0, 0, 0));
        }
        finishQtyList.stream().forEach(this::validateFinishQty);
        Map<String, TcScheFinishQty> summaryMap = this.summarizeFinishQty(finishQtyList);
        Set<String> orderNoSet = summaryMap.values().stream().map(TcScheFinishQty::getOrderNo)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        if (orderNoSet.isEmpty()) {
            return AjaxResult.success(this.buildWriteBackSummary(summaryMap.size(), 0, summaryMap.size()));
        }

        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(
                new LambdaQueryWrapper<TcScheduleResult>().in(TcScheduleResult::getOrderNo, orderNoSet));
        Map<String, List<TcScheduleResult>> resultGroupMap = CollectionUtils.emptyIfNull(resultList).stream()
                .collect(Collectors.groupingBy(this::buildResultGroupKey, LinkedHashMap::new, Collectors.toList()));
        Map<Long, TcScheduleResult> updatePatchMap = new LinkedHashMap<>();
        int unmatchedCount = 0;

        for (TcScheFinishQty summary : summaryMap.values()) {
            List<TcScheduleResult> matchedResultList = resultGroupMap.get(this.buildSummaryGroupKey(summary));
            if (CollectionUtils.isEmpty(matchedResultList)) {
                unmatchedCount++;
                log.warn("[TC_MES_SYNC] 未匹配胎侧排程结果, factoryCode={}, sidewallCode={}, orderNo={}, mesDate={}",
                        summary.getFactoryCode(), summary.getSidewallCode(), summary.getOrderNo(),
                        DateUtil.formatDate(summary.getScheduleDate()));
                continue;
            }
            for (TcScheduleResult result : matchedResultList) {
                this.applyMesShiftFinishQty(updatePatchMap, result, summary, "NIGHT",
                        BigDecimalUtils.valueOf(summary.getNightFinishQty()));
                this.applyMesShiftFinishQty(updatePatchMap, result, summary, "DAY",
                        BigDecimalUtils.valueOf(summary.getDayFinishQty()));
                this.applyMesShiftFinishQty(updatePatchMap, result, summary, "MID",
                        BigDecimalUtils.valueOf(summary.getMidFinishQty()));
            }
        }

        int updateCount = updatePatchMap.values().stream()
                .mapToInt(this.scheduleResultMapper::updateById).sum();
        log.info("[TC_MES_SYNC] 胎侧完成量回写完成, source={}, updated={}, unmatched={}",
                summaryMap.size(), updateCount, unmatchedCount);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.finishWriteBackSuccess"),
                this.buildWriteBackSummary(summaryMap.size(), updateCount, unmatchedCount));
    }

    /**
     * 将一个MES班别完成量写入对应结果补丁。
     *
     * @param updatePatchMap 按结果ID归集的补丁
     * @param result 排程结果
     * @param summary MES汇总完成量
     * @param mesShiftCode MES班别
     * @param finishQty 完成量
     */
    private void applyMesShiftFinishQty(Map<Long, TcScheduleResult> updatePatchMap, TcScheduleResult result,
                                        TcScheFinishQty summary, String mesShiftCode, BigDecimal finishQty) {
        Integer shiftOrder = TcShiftBusinessDateResolver.resolveShiftOrder(result.getScheduleDate(),
                summary.getScheduleDate(), mesShiftCode);
        if (shiftOrder == null) {
            return;
        }
        TcScheduleResult updatePatch = updatePatchMap.computeIfAbsent(result.getId(), resultId -> {
            TcScheduleResult patch = new TcScheduleResult();
            patch.setId(resultId);
            return patch;
        });
        updatePatch.setFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder), finishQty);
    }

    /**
     * 按工厂、胎侧、工单和MES业务日期汇总完成量。
     *
     * @param finishQtyList 原始完成量
     * @return 汇总结果
     */
    private Map<String, TcScheFinishQty> summarizeFinishQty(List<TcScheFinishQty> finishQtyList) {
        Map<String, TcScheFinishQty> summaryMap = new LinkedHashMap<>();
        finishQtyList.stream().filter(Objects::nonNull).forEach(item -> {
            String summaryKey = this.buildSummaryKey(item);
            TcScheFinishQty existing = summaryMap.get(summaryKey);
            if (existing == null) {
                summaryMap.put(summaryKey, item);
                return;
            }
            existing.setNightFinishQty(BigDecimalUtils.valueOf(existing.getNightFinishQty())
                    .add(BigDecimalUtils.valueOf(item.getNightFinishQty())));
            existing.setDayFinishQty(BigDecimalUtils.valueOf(existing.getDayFinishQty())
                    .add(BigDecimalUtils.valueOf(item.getDayFinishQty())));
            existing.setMidFinishQty(BigDecimalUtils.valueOf(existing.getMidFinishQty())
                    .add(BigDecimalUtils.valueOf(item.getMidFinishQty())));
        });
        return summaryMap;
    }

    /**
     * 校验MES完成量不为负数。
     *
     * @param finishQty 完成量记录
     * @throws ServiceException 任一班别为负数时抛出
     */
    private void validateFinishQty(TcScheFinishQty finishQty) {
        if (finishQty == null || BigDecimalUtils.valueOf(finishQty.getNightFinishQty()).signum() < 0
                || BigDecimalUtils.valueOf(finishQty.getDayFinishQty()).signum() < 0
                || BigDecimalUtils.valueOf(finishQty.getMidFinishQty()).signum() < 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.mes.finishQtyNegative"));
        }
    }

    /**
     * 构造完成量汇总唯一键。
     *
     * @param item 完成量记录
     * @return 唯一键
     */
    private String buildSummaryKey(TcScheFinishQty item) {
        return this.buildSummaryGroupKey(item) + "|" + DateUtil.formatDate(item.getScheduleDate());
    }

    /**
     * 构造完成量与结果关联键。
     *
     * @param item 完成量记录
     * @return 关联键
     */
    private String buildSummaryGroupKey(TcScheFinishQty item) {
        return StrUtil.blankToDefault(item.getFactoryCode(), "") + "|"
                + StrUtil.blankToDefault(item.getSidewallCode(), "") + "|"
                + StrUtil.blankToDefault(item.getOrderNo(), "");
    }

    /**
     * 构造排程结果关联键。
     *
     * @param result 排程结果
     * @return 关联键
     */
    private String buildResultGroupKey(TcScheduleResult result) {
        return StrUtil.blankToDefault(result.getFactoryCode(), "") + "|"
                + StrUtil.blankToDefault(result.getSidewallCode(), "") + "|"
                + StrUtil.blankToDefault(result.getOrderNo(), "");
    }

    /**
     * 构造完成量回写摘要。
     *
     * @param sourceCount 汇总来源数量
     * @param updateCount 更新结果数量
     * @param unmatchedCount 未匹配数量
     * @return 摘要
     */
    private Map<String, Object> buildWriteBackSummary(int sourceCount, int updateCount, int unmatchedCount) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schemaVersion", 1);
        summary.put("sourceCount", sourceCount);
        summary.put("updateCount", updateCount);
        summary.put("unmatchedCount", unmatchedCount);
        return summary;
    }
}
