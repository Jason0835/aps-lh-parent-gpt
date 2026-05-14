package com.zlt.aps.cx.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxScheFinishQtyMapper;
import com.zlt.aps.cx.service.ICxScheFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 成型排程完成量回报Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CxScheFinishQtyServiceImpl extends AbstractDocService<CxScheFinishQty> implements ICxScheFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Autowired
    private CxScheFinishQtyMapper cxScheFinishQtyMapper;

    @Override
    protected String getDocTypeCode() {
        return "CX_SCHE_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<CxScheFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }

    /**
     * 成型排程完成量回写成型排程结果表各班次完成量
     * <p>
     * 业务规则（同硫化排程完成量回写逻辑）：
     * 1. 完成量回报表(T_CX_SCHE_FINISH_QTY)按机台+物料+排程日期汇总夜早中完成量
     * 2. 排程结果表(T_CX_SCHEDULE_RESULT)的8班对应3天窗口：
     *    1~2班对应D-2的早中，3~5班对应D-1的夜早中，6~8班对应D的夜早中
     * 3. 回报日期D的完成量需要更新排程日期为D、D+1、D+2的排程结果：
     *    - 排程日期D：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
     *    - 排程日期D+1：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
     *    - 排程日期D+2：1班(早)=MES2班，2班(中)=MES3班
     * 4. 排程结果维度：工厂+机台编码+物料编码+排程日期
     * </p>
     *
     * @param finishQtyList 完成量回报数据列表
     * @return 回写结果
     */
    @Override
    public AjaxResult writeBackScheduleResultFinishQty(List<CxScheFinishQty> finishQtyList) {
        if (CollectionUtils.isEmpty(finishQtyList)) {
            log.info("【成型完成量回写】完成量回报数据为空，跳过回写");
            return AjaxResult.success();
        }

        log.info("【成型完成量回写】开始回写成型排程结果表完成量，回报数据条数：{}", finishQtyList.size());

        Map<String, CxScheFinishQty> summaryMap = summarizeFinishQty(finishQtyList);
        log.info("【成型完成量回写】汇总后数据条数：{}", summaryMap.size());

        int totalUpdateCount = 0;

        for (Map.Entry<String, CxScheFinishQty> entry : summaryMap.entrySet()) {
            CxScheFinishQty summary = entry.getValue();
            Date scheduleDate = summary.getScheduleDate();
            if (scheduleDate == null) {
                log.warn("【成型完成量回写】排程日期为空，跳过，机台：{}，物料：{}", summary.getCxMachineCode(), summary.getMaterialCode());
                continue;
            }

            BigDecimal nightQty = summary.getClass1FinishQty() != null ? summary.getClass1FinishQty() : BigDecimal.ZERO;
            BigDecimal morningQty = summary.getClass2FinishQty() != null ? summary.getClass2FinishQty() : BigDecimal.ZERO;
            BigDecimal middleQty = summary.getClass3FinishQty() != null ? summary.getClass3FinishQty() : BigDecimal.ZERO;

            Date dateD = DateUtil.beginOfDay(scheduleDate);
            Date dateD1 = DateUtil.offsetDay(dateD, 1);
            Date dateD2 = DateUtil.offsetDay(dateD, 2);

            LambdaQueryWrapper<CxScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
            queryWrapper.eq(CxScheduleResult::getFactoryCode, summary.getFactoryCode());
            queryWrapper.eq(CxScheduleResult::getCxMachineCode, summary.getCxMachineCode());
            queryWrapper.eq(CxScheduleResult::getMaterialCode, summary.getMaterialCode());
            queryWrapper.in(CxScheduleResult::getScheduleDate, Arrays.asList(dateD, dateD1, dateD2));
            List<CxScheduleResult> resultList = cxScheduleResultMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(resultList)) {
                log.info("【成型完成量回写】未找到排程结果数据，工厂：{}，机台：{}，物料：{}，日期范围：{}~{}",
                        summary.getFactoryCode(), summary.getCxMachineCode(), summary.getMaterialCode(),
                        DateUtil.formatDate(dateD), DateUtil.formatDate(dateD2));
                continue;
            }

            for (CxScheduleResult result : resultList) {
                Date resultScheduleDate = result.getScheduleDate();
                int dayOffset = daysBetween(dateD, resultScheduleDate);

                int updateCount = 0;
                if (dayOffset == 0) {
                    updateCount = updateDay0FinishQty(result, nightQty, morningQty, middleQty);
                } else if (dayOffset == 1) {
                    updateCount = updateDay1FinishQty(result, nightQty, morningQty, middleQty);
                } else if (dayOffset == 2) {
                    updateCount = updateDay2FinishQty(result, morningQty, middleQty);
                } else {
                    log.warn("【成型完成量回写】排程日期偏移量不在预期范围内，偏移：{}天，排程日期：{}", dayOffset, DateUtil.formatDate(resultScheduleDate));
                    continue;
                }

                totalUpdateCount += updateCount;
            }
        }

        log.info("【成型完成量回写】回写完成，累计更新记录数：{}", totalUpdateCount);
        return AjaxResult.success();
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy, List<CxScheFinishQty> insertList) {
        log.info("成型排程完成量同步-事务开始：逻辑删除分厂{}排程日期为{}的旧数据，待插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
        cxScheFinishQtyMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate, updateBy, new Date());
        log.info("成型排程完成量同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<CxScheFinishQty> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("成型排程完成量同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("成型排程完成量同步-事务完成：分厂{}，排程日期={}，插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
    }

    /**
     * 按工厂+机台+物料+排程日期汇总完成量
     * 将同一维度下的多条夜早中记录合并成一条
     *
     * @param finishQtyList 完成量原始列表
     * @return 汇总后的Map，key=工厂|机台|物料|排程日期
     */
    private Map<String, CxScheFinishQty> summarizeFinishQty(List<CxScheFinishQty> finishQtyList) {
        Map<String, CxScheFinishQty> summaryMap = new LinkedHashMap<>();

        for (CxScheFinishQty item : finishQtyList) {
            String key = buildSummaryKey(item);
            CxScheFinishQty existing = summaryMap.get(key);
            if (existing == null) {
                summaryMap.put(key, item);
            } else {
                existing.setClass1FinishQty(addBigDecimal(existing.getClass1FinishQty(), item.getClass1FinishQty()));
                existing.setClass2FinishQty(addBigDecimal(existing.getClass2FinishQty(), item.getClass2FinishQty()));
                existing.setClass3FinishQty(addBigDecimal(existing.getClass3FinishQty(), item.getClass3FinishQty()));
            }
        }
        return summaryMap;
    }

    /**
     * 构建汇总Key：工厂|机台|物料|排程日期
     */
    private String buildSummaryKey(CxScheFinishQty item) {
        return item.getFactoryCode() + "|" + item.getCxMachineCode() + "|" + item.getMaterialCode() + "|" + DateUtil.formatDate(item.getScheduleDate());
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
     * 更新排程日期D（当天）的完成量
     * 6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
     */
    private int updateDay0FinishQty(CxScheduleResult result, BigDecimal nightQty, BigDecimal morningQty, BigDecimal middleQty) {
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getId, result.getId());
        updateWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        updateWrapper.set(CxScheduleResult::getClass6FinishQty, nightQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass7FinishQty, morningQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass8FinishQty, middleQty.intValue());
        int count = cxScheduleResultMapper.update(null, updateWrapper);
        log.info("【成型完成量回写】排程日期D更新，ID：{}，6班(夜)={}，7班(早)={}，8班(中)={}，更新行数：{}",
                result.getId(), nightQty, morningQty, middleQty, count);
        return count;
    }

    /**
     * 更新排程日期D+1（次日）的完成量
     * 3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
     */
    private int updateDay1FinishQty(CxScheduleResult result, BigDecimal nightQty, BigDecimal morningQty, BigDecimal middleQty) {
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getId, result.getId());
        updateWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        updateWrapper.set(CxScheduleResult::getClass3FinishQty, nightQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass4FinishQty, morningQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass5FinishQty, middleQty.intValue());
        int count = cxScheduleResultMapper.update(null, updateWrapper);
        log.info("【成型完成量回写】排程日期D+1更新，ID：{}，3班(夜)={}，4班(早)={}，5班(中)={}，更新行数：{}",
                result.getId(), nightQty, morningQty, middleQty, count);
        return count;
    }

    /**
     * 更新排程日期D+2（后天）的完成量
     * 1班(早)=MES2班，2班(中)=MES3班
     */
    private int updateDay2FinishQty(CxScheduleResult result, BigDecimal morningQty, BigDecimal middleQty) {
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getId, result.getId());
        updateWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        updateWrapper.set(CxScheduleResult::getClass1FinishQty, morningQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass2FinishQty, middleQty.intValue());
        int count = cxScheduleResultMapper.update(null, updateWrapper);
        log.info("【成型完成量回写】排程日期D+2更新，ID：{}，1班(早)={}，2班(中)={}，更新行数：{}",
                result.getId(), morningQty, middleQty, count);
        return count;
    }
}
