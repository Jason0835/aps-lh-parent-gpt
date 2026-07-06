package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.domain.entity.TmScheFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.mapper.TmScheFinishQtyMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.ITmScheFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面排程完成量回报Service实现
 *
 * @author APS Team
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmScheFinishQtyServiceImpl extends AbstractDocService<TmScheFinishQty> implements ITmScheFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Autowired
    private TmScheFinishQtyMapper tmScheFinishQtyMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM_SCHE_FINISH";
    }

    /**
     * 胎面排程完成量回写胎面排程结果表各班次完成量
     * <p>
     * 业务规则（6班制，3天排程窗口，与胎圈一致）：
     * 1. 完成量回报表(T_TM_SCHE_FINISH_QTY)按工厂+胎面代码+工单号+排程日期汇总夜早中完成量
     * 2. 排程结果表(T_TM_SCHEDULE_RESULT)的6班对应3天窗口（排程日期=胎面生产第一天=D+1日）：
     *    - D日(排程日期-1)：1班=中班(14-22)
     *    - D+1日(排程日期)：2班=夜班(22-6)，3班=早班(6-14)，4班=中班(14-22)
     *    - D+2日(排程日期+1)：5班=夜班(22-6)，6班=早班(6-14)
     * 3. 回报日期与排程日期的天数差决定更新哪个班次：
     *    - 天数差=-1（MES日期=排程日期-1）：中班→1班完成量
     *    - 天数差=0（MES日期=排程日期）：夜班→2班，早班→3班，中班→4班完成量
     *    - 天数差=+1（MES日期=排程日期+1）：夜班→5班，早班→6班完成量
     * 4. 排程结果维度：胎面代码+工单号+排程日期
     * 5. TmScheduleResult.classNFinishQty 为 BigDecimal，回写直接赋值（不做intValue）
     * </p>
     *
     * @param finishQtyList 完成量回报数据列表
     * @return 回写结果
     */
    @Override
    public AjaxResult writeBackScheduleResultFinishQty(List<TmScheFinishQty> finishQtyList) {
        if (CollectionUtils.isEmpty(finishQtyList)) {
            log.info("【胎面完成量回写】完成量回报数据为空，跳过回写");
            return AjaxResult.success();
        }

        log.info("【胎面完成量回写】开始回写胎面排程结果表完成量，回报数据条数：{}", finishQtyList.size());

        Map<String, TmScheFinishQty> summaryMap = this.summarizeFinishQty(finishQtyList);
        log.info("【胎面完成量回写】汇总后数据条数：{}", summaryMap.size());

        // 收集所有工单号，批量查询排程结果（6班制）
        Set<String> orderNos = summaryMap.values().stream()
                .map(TmScheFinishQty::getOrderNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(orderNos)) {
            log.warn("【胎面完成量回写】汇总数据中无有效工单号，跳过回写");
            return AjaxResult.success();
        }

        LambdaQueryWrapper<TmScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TmScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        queryWrapper.in(TmScheduleResult::getOrderNo, orderNos);
        List<TmScheduleResult> resultList = tmScheduleResultMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(resultList)) {
            log.warn("【胎面完成量回写】未找到排程结果数据，工单号集合：{}", orderNos);
            return AjaxResult.success();
        }

        // 按 胎面代码|工单号 分组排程结果
        Map<String, List<TmScheduleResult>> resultMap = resultList.stream()
                .collect(Collectors.groupingBy(this::buildResultGroupKey));

        int totalUpdateCount = 0;

        for (TmScheFinishQty summary : summaryMap.values()) {
            Date scheduleDate = summary.getScheduleDate();
            if (scheduleDate == null) {
                log.warn("【胎面完成量回写】排程日期为空，跳过，胎面代码：{}，工单号：{}", summary.getTreadCode(), summary.getOrderNo());
                continue;
            }

            String groupKey = this.buildSummaryGroupKey(summary);
            List<TmScheduleResult> results = resultMap.get(groupKey);
            if (CollectionUtils.isEmpty(results)) {
                log.info("【胎面完成量回写】未找到排程结果数据，工厂：{}，胎面代码：{}，工单号：{}",
                        summary.getFactoryCode(), summary.getTreadCode(), summary.getOrderNo());
                continue;
            }

            BigDecimal nightQty = summary.getNightFinishQty() != null ? summary.getNightFinishQty() : BigDecimal.ZERO;
            BigDecimal dayQty = summary.getDayFinishQty() != null ? summary.getDayFinishQty() : BigDecimal.ZERO;
            BigDecimal midQty = summary.getMidFinishQty() != null ? summary.getMidFinishQty() : BigDecimal.ZERO;
            Date mesDate = DateUtil.beginOfDay(scheduleDate);

            for (TmScheduleResult result : results) {
                Date resultScheduleDate = result.getScheduleDate();
                if (resultScheduleDate == null) {
                    continue;
                }
                // 天数差 = 排程日期 - MES日期
                int dayOffset = this.daysBetween(mesDate, resultScheduleDate);
                int updateCount = this.updateFinishQtyByDayOffset(result, dayOffset, nightQty, dayQty, midQty);
                totalUpdateCount += updateCount;
            }
        }

        log.info("【胎面完成量回写】回写完成，累计更新记录数：{}", totalUpdateCount);
        return AjaxResult.success();
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy, List<TmScheFinishQty> insertList) {
        log.info("胎面排程完成量同步-事务开始：逻辑删除分厂{}排程日期为{}的旧数据，待插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
        tmScheFinishQtyMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate, updateBy, new Date());
        log.info("胎面排程完成量同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<TmScheFinishQty> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("胎面排程完成量同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("胎面排程完成量同步-事务完成：分厂{}，排程日期={}，插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
    }

    /**
     * 按工厂+胎面代码+工单号+排程日期汇总完成量
     * 将同一维度下的多条夜早中记录合并成一条
     *
     * @param finishQtyList 完成量原始列表
     * @return 汇总后的Map，key=工厂|胎面代码|工单号|排程日期
     */
    private Map<String, TmScheFinishQty> summarizeFinishQty(List<TmScheFinishQty> finishQtyList) {
        Map<String, TmScheFinishQty> summaryMap = new LinkedHashMap<>();

        for (TmScheFinishQty item : finishQtyList) {
            String key = this.buildSummaryKey(item);
            TmScheFinishQty existing = summaryMap.get(key);
            if (existing == null) {
                summaryMap.put(key, item);
            } else {
                existing.setNightFinishQty(this.addBigDecimal(existing.getNightFinishQty(), item.getNightFinishQty()));
                existing.setDayFinishQty(this.addBigDecimal(existing.getDayFinishQty(), item.getDayFinishQty()));
                existing.setMidFinishQty(this.addBigDecimal(existing.getMidFinishQty(), item.getMidFinishQty()));
            }
        }
        return summaryMap;
    }

    /**
     * 构建汇总Key：工厂|胎面代码|工单号|排程日期
     */
    private String buildSummaryKey(TmScheFinishQty item) {
        return item.getFactoryCode() + "|" + item.getTreadCode() + "|" + item.getOrderNo() + "|" + DateUtil.formatDate(item.getScheduleDate());
    }

    /**
     * 构建汇总分组Key（不含排程日期，用于关联排程结果）：胎面代码|工单号
     */
    private String buildSummaryGroupKey(TmScheFinishQty item) {
        return item.getTreadCode() + "|" + item.getOrderNo();
    }

    /**
     * 构建排程结果分组Key：胎面代码|工单号
     */
    private String buildResultGroupKey(TmScheduleResult result) {
        return result.getTreadCode() + "|" + result.getOrderNo();
    }

    /**
     * BigDecimal安全累加，null视为0
     */
    private BigDecimal addBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return BigDecimal.ZERO;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.add(b);
    }

    /**
     * 计算两个日期之间的天数差（date2 - date1）
     * 仅比较日期部分，忽略时分秒
     */
    private int daysBetween(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        long diffMillis = cal2.getTimeInMillis() - cal1.getTimeInMillis();
        return (int) (diffMillis / (24 * 60 * 60 * 1000));
    }

    /**
     * 根据MES回报日期与排程日期的天数差，更新对应班次完成量
     * <p>
     * 6班制3天窗口映射（排程日期=胎面生产第一天=D+1日）：
     * - 天数差=-1（MES日期=排程日期-1=D日）：中班→1班完成量
     * - 天数差=0（MES日期=排程日期=D+1日）：夜班→2班，早班→3班，中班→4班完成量
     * - 天数差=+1（MES日期=排程日期+1=D+2日）：夜班→5班，早班→6班完成量
     * TmScheduleResult.classNFinishQty 为 BigDecimal，直接赋值
     * </p>
     *
     * @param result    排程结果
     * @param dayOffset 天数差（排程日期 - MES日期）
     * @param nightQty  夜班完成量
     * @param dayQty    早班完成量
     * @param midQty    中班完成量
     * @return 更新行数
     */
    private int updateFinishQtyByDayOffset(TmScheduleResult result, int dayOffset,
                                           BigDecimal nightQty, BigDecimal dayQty, BigDecimal midQty) {
        LambdaUpdateWrapper<TmScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TmScheduleResult::getId, result.getId());
        updateWrapper.eq(TmScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);

        String shiftDesc;
        switch (dayOffset) {
            case -1:
                // MES日期=排程日期-1=D日：中班→1班完成量
                updateWrapper.set(TmScheduleResult::getClass1FinishQty, midQty);
                shiftDesc = "1班(中班)";
                break;
            case 0:
                // MES日期=排程日期=D+1日：夜班→2班，早班→3班，中班→4班完成量
                updateWrapper.set(TmScheduleResult::getClass2FinishQty, nightQty);
                updateWrapper.set(TmScheduleResult::getClass3FinishQty, dayQty);
                updateWrapper.set(TmScheduleResult::getClass4FinishQty, midQty);
                shiftDesc = "2班(夜班)/3班(早班)/4班(中班)";
                break;
            case 1:
                // MES日期=排程日期+1=D+2日：夜班→5班，早班→6班完成量
                updateWrapper.set(TmScheduleResult::getClass5FinishQty, nightQty);
                updateWrapper.set(TmScheduleResult::getClass6FinishQty, dayQty);
                shiftDesc = "5班(夜班)/6班(早班)";
                break;
            default:
                log.warn("【胎面完成量回写】排程日期偏移量不在预期范围内，偏移：{}天，排程日期：{}",
                        dayOffset, DateUtil.formatDate(result.getScheduleDate()));
                return 0;
        }

        int count = tmScheduleResultMapper.update(null, updateWrapper);
        log.info("【胎面完成量回写】排程日期偏移{}天更新，ID：{}，班次：{}，夜班={}，早班={}，中班={}，更新行数：{}",
                dayOffset, result.getId(), shiftDesc, nightQty, dayQty, midQty, count);
        return count;
    }
}
