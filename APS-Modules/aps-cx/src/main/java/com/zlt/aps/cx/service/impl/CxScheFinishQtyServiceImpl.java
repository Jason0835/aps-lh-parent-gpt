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
import org.apache.commons.lang3.StringUtils;
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
     * 业务规则（T+1排程窗口）：
     * 1. 完成量回报表(T_CX_SCHE_FINISH_QTY)按工厂+机台+胎胚+排程日期汇总夜早中完成量
     * 2. 排程结果表(T_CX_SCHEDULE_RESULT)的8班对应3天窗口（排程日期=T+1日）：
     *    1~2班对应T-1日的早中，3~5班对应T日的夜早中，6~8班对应T+1日的夜早中
     * 3. 回报日期D的完成量需要更新排程日期为D-1、D、D+1的排程结果：
     *    - 排程日期D-1：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
     *    - 排程日期D：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
     *    - 排程日期D+1：1班(早)=MES2班，2班(中)=MES3班
     * 4. 排程结果维度：工厂+机台编码+胎胚编码+排程日期
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
                log.warn("【成型完成量回写】排程日期为空，跳过，机台：{}，胎胚：{}", summary.getCxMachineCode(), summary.getEmbryoCode());
                continue;
            }

            BigDecimal nightQty = summary.getClass1FinishQty() != null ? summary.getClass1FinishQty() : BigDecimal.ZERO;
            BigDecimal morningQty = summary.getClass2FinishQty() != null ? summary.getClass2FinishQty() : BigDecimal.ZERO;
            BigDecimal middleQty = summary.getClass3FinishQty() != null ? summary.getClass3FinishQty() : BigDecimal.ZERO;

            Date dateD = DateUtil.beginOfDay(scheduleDate);
            Date dateDMinus1 = DateUtil.offsetDay(dateD, -1);
            Date dateDPlus1 = DateUtil.offsetDay(dateD, 1);

            LambdaQueryWrapper<CxScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
            queryWrapper.eq(CxScheduleResult::getFactoryCode, summary.getFactoryCode());
            queryWrapper.eq(CxScheduleResult::getCxMachineCode, summary.getCxMachineCode());
            queryWrapper.eq(CxScheduleResult::getEmbryoCode, summary.getEmbryoCode());
            queryWrapper.in(CxScheduleResult::getScheduleDate, Arrays.asList(dateDMinus1, dateD, dateDPlus1));
            List<CxScheduleResult> resultList = cxScheduleResultMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(resultList)) {
                log.info("【成型完成量回写】未找到排程结果数据，工厂：{}，机台：{}，胎胚：{}，日期范围：{}~{}",
                        summary.getFactoryCode(), summary.getCxMachineCode(), summary.getEmbryoCode(),
                        DateUtil.formatDate(dateDMinus1), DateUtil.formatDate(dateDPlus1));
                continue;
            }

            for (CxScheduleResult result : resultList) {
                Date resultScheduleDate = result.getScheduleDate();
                int dayOffset = daysBetween(dateD, resultScheduleDate);

                // 回填前校验：通过胎胚+各班示方号去SKU与示方关系表校验示方类型是否一致
                if (!this.validateCxTypeConsistency(result, dayOffset, summary)) {
                    log.warn("【成型完成量回写】示方类型校验不通过，跳过回填。工厂={}，机台={}，胎胚={}，排程日期={}",
                            summary.getFactoryCode(), summary.getCxMachineCode(), summary.getEmbryoCode(),
                            DateUtil.formatDate(resultScheduleDate));
                    continue;
                }

                int updateCount = 0;
                if (dayOffset == -1) {
                    // 排程日期D-1：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
                    updateCount = this.updateDayMinus1FinishQty(result, nightQty, morningQty, middleQty, summary);
                } else if (dayOffset == 0) {
                    // 排程日期D：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
                    updateCount = this.updateDay0FinishQty(result, nightQty, morningQty, middleQty, summary);
                } else if (dayOffset == 1) {
                    // 排程日期D+1：1班(早)=MES2班，2班(中)=MES3班
                    updateCount = this.updateDay1FinishQty(result, morningQty, middleQty, summary);
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
     * 按工厂+机台+胎胚+排程日期汇总完成量
     * 将同一维度下的多条夜早中记录合并成一条
     *
     * @param finishQtyList 完成量原始列表
     * @return 汇总后的Map，key=工厂|机台|胎胚|排程日期
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
     * 构建汇总Key：工厂|机台|胎胚|排程日期|示方类型
     * <p>示方类型维度：不同示方类型的示方号可能相同，因此用示方类型而非示方号作为维度，
     * 避免不同示方类型的数据被错误合并</p>
     */
    private String buildSummaryKey(CxScheFinishQty item) {
        return item.getFactoryCode() + "|" + item.getCxMachineCode() + "|" + item.getEmbryoCode() + "|"
                + DateUtil.formatDate(item.getScheduleDate()) + "|"
                + item.getClass1CxType() + "|" + item.getClass2CxType() + "|" + item.getClass3CxType();
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
     * 校验排程结果记录与完成量回报的示方类型是否一致
     * <p>
     * 校验规则：
     * 直接比对排程结果中对应班次的示方类型与MES回报中的示方类型，
     * 不一致则不回填。由于不同示方类型的示方号可能相同，不再通过示方号反查关系表校验。
     * </p>
     *
     * @param result    排程结果记录
     * @param dayOffset 日期偏移量(-1/0/1)
     * @param summary   完成量回报汇总数据
     * @return true-校验通过可回填；false-校验不通过跳过
     */
    private boolean validateCxTypeConsistency(CxScheduleResult result, int dayOffset, CxScheFinishQty summary) {
        List<String> mismatchShifts = new ArrayList<>();

        if (dayOffset == -1) {
            // 排程日期D-1：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
            if (!this.checkSingleShiftCxType(summary.getClass1CxType(), result.getClass6RecipeType(), "6班(MES1班)")) {
                mismatchShifts.add("6班");
            }
            if (!this.checkSingleShiftCxType(summary.getClass2CxType(), result.getClass7RecipeType(), "7班(MES2班)")) {
                mismatchShifts.add("7班");
            }
            if (!this.checkSingleShiftCxType(summary.getClass3CxType(), result.getClass8RecipeType(), "8班(MES3班)")) {
                mismatchShifts.add("8班");
            }
        } else if (dayOffset == 0) {
            // 排程日期D：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
            if (!this.checkSingleShiftCxType(summary.getClass1CxType(), result.getClass3RecipeType(), "3班(MES1班)")) {
                mismatchShifts.add("3班");
            }
            if (!this.checkSingleShiftCxType(summary.getClass2CxType(), result.getClass4RecipeType(), "4班(MES2班)")) {
                mismatchShifts.add("4班");
            }
            if (!this.checkSingleShiftCxType(summary.getClass3CxType(), result.getClass5RecipeType(), "5班(MES3班)")) {
                mismatchShifts.add("5班");
            }
        } else if (dayOffset == 1) {
            // 排程日期D+1：1班(早)=MES2班，2班(中)=MES3班
            if (!this.checkSingleShiftCxType(summary.getClass2CxType(), result.getClass1RecipeType(), "1班(MES2班)")) {
                mismatchShifts.add("1班");
            }
            if (!this.checkSingleShiftCxType(summary.getClass3CxType(), result.getClass2RecipeType(), "2班(MES3班)")) {
                mismatchShifts.add("2班");
            }
        }

        if (!mismatchShifts.isEmpty()) {
            log.warn("【成型示方类型校验】以下班次校验不通过，胎胚={}，不一致班次：{}",
                    summary.getEmbryoCode(), String.join(",", mismatchShifts));
            return false;
        }
        return true;
    }

    /**
     * 单班次示方类型校验
     * 直接比对MES回报的示方类型与排程结果中对应班次的示方类型
     * <p>
     * 由于不同示方类型的示方号可能相同，不再通过示方号反查关系表，
     * 而是直接比对示方类型值
     * </p>
     *
     * @param mesCxType        MES回报的示方类型
     * @param resultRecipeType 排程结果中对应班次的示方书类型
     * @param shiftDesc        班次描述（用于日志）
     * @return true-一致或无需校验；false-不一致
     */
    private boolean checkSingleShiftCxType(String mesCxType, String resultRecipeType, String shiftDesc) {
        // 如果MES回报的示方类型为空，跳过该校验（兼容旧数据）
        if (StringUtils.isEmpty(mesCxType)) {
            return true;
        }

        // 如果排程结果中对应班次的示方类型为空，跳过校验（兼容旧数据）
        if (StringUtils.isEmpty(resultRecipeType)) {
            return true;
        }

        if (!mesCxType.equals(resultRecipeType)) {
            log.warn("【成型示方类型校验】类型不一致！班次={}，MES类型={}，排程结果类型={}",
                    shiftDesc, mesCxType, resultRecipeType);
            return false;
        }

        return true;
    }

    /**
     * 更新排程日期D-1（前一天）的完成量
     * 6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
     *
     * @param result     排程结果
     * @param nightQty   夜班完成量
     * @param morningQty 早班完成量
     * @param middleQty  中班完成量
     * @param summary    完成量回报汇总数据（用于示方类型匹配）
     * @return 更新记录数
     */
    private int updateDayMinus1FinishQty(CxScheduleResult result, BigDecimal nightQty, BigDecimal morningQty, BigDecimal middleQty, CxScheFinishQty summary) {
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getId, result.getId());
        updateWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        // 示方类型匹配条件：用示方类型而非示方号匹配，避免不同示方类型示方号相同时回填错误
        if (StringUtils.isNotEmpty(summary.getClass1CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass6RecipeType, summary.getClass1CxType());  // 6班=MES1班
        }
        if (StringUtils.isNotEmpty(summary.getClass2CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass7RecipeType, summary.getClass2CxType());  // 7班=MES2班
        }
        if (StringUtils.isNotEmpty(summary.getClass3CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass8RecipeType, summary.getClass3CxType());  // 8班=MES3班
        }
        updateWrapper.set(CxScheduleResult::getClass6FinishQty, nightQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass7FinishQty, morningQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass8FinishQty, middleQty.intValue());
        int count = cxScheduleResultMapper.update(null, updateWrapper);
        log.info("【成型完成量回写】排程日期D-1更新，ID：{}，6班(夜)={}，7班(早)={}，8班(中)={}，更新行数：{}",
                result.getId(), nightQty, morningQty, middleQty, count);
        return count;
    }

    /**
     * 更新排程日期D（当天）的完成量
     * 3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
     *
     * @param result     排程结果
     * @param nightQty   夜班完成量
     * @param morningQty 早班完成量
     * @param middleQty  中班完成量
     * @param summary    完成量回报汇总数据（用于示方类型匹配）
     * @return 更新记录数
     */
    private int updateDay0FinishQty(CxScheduleResult result, BigDecimal nightQty, BigDecimal morningQty, BigDecimal middleQty, CxScheFinishQty summary) {
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getId, result.getId());
        updateWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        // 示方类型匹配条件：用示方类型而非示方号匹配，避免不同示方类型示方号相同时回填错误
        if (StringUtils.isNotEmpty(summary.getClass1CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass3RecipeType, summary.getClass1CxType());  // 3班=MES1班
        }
        if (StringUtils.isNotEmpty(summary.getClass2CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass4RecipeType, summary.getClass2CxType());  // 4班=MES2班
        }
        if (StringUtils.isNotEmpty(summary.getClass3CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass5RecipeType, summary.getClass3CxType());  // 5班=MES3班
        }
        updateWrapper.set(CxScheduleResult::getClass3FinishQty, nightQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass4FinishQty, morningQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass5FinishQty, middleQty.intValue());
        int count = cxScheduleResultMapper.update(null, updateWrapper);
        log.info("【成型完成量回写】排程日期D更新，ID：{}，3班(夜)={}，4班(早)={}，5班(中)={}，更新行数：{}",
                result.getId(), nightQty, morningQty, middleQty, count);
        return count;
    }

    /**
     * 更新排程日期D+1（次日）的完成量
     * 1班(早)=MES2班，2班(中)=MES3班
     * 注意：D+1没有夜班（D的夜班不在D+1的窗口内）
     *
     * @param result     排程结果
     * @param morningQty 早班完成量
     * @param middleQty  中班完成量
     * @param summary    完成量回报汇总数据（用于示方类型匹配）
     * @return 更新记录数
     */
    private int updateDay1FinishQty(CxScheduleResult result, BigDecimal morningQty, BigDecimal middleQty, CxScheFinishQty summary) {
        LambdaUpdateWrapper<CxScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CxScheduleResult::getId, result.getId());
        updateWrapper.eq(CxScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        // 示方类型匹配条件：用示方类型而非示方号匹配，避免不同示方类型示方号相同时回填错误
        if (StringUtils.isNotEmpty(summary.getClass2CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass1RecipeType, summary.getClass2CxType());  // 1班=MES2班
        }
        if (StringUtils.isNotEmpty(summary.getClass3CxType())) {
            updateWrapper.eq(CxScheduleResult::getClass2RecipeType, summary.getClass3CxType());  // 2班=MES3班
        }
        updateWrapper.set(CxScheduleResult::getClass1FinishQty, morningQty.intValue());
        updateWrapper.set(CxScheduleResult::getClass2FinishQty, middleQty.intValue());
        int count = cxScheduleResultMapper.update(null, updateWrapper);
        log.info("【成型完成量回写】排程日期D+1更新，ID：{}，1班(早)={}，2班(中)={}，更新行数：{}",
                result.getId(), morningQty, middleQty, count);
        return count;
    }
}
