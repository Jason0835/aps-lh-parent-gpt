package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhScheFinishQtyMapper;
import com.zlt.aps.lh.service.ILhScheFinishQtyService;
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
 * 硫化排程完成量回报Service实现
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhScheFinishQtyServiceImpl extends AbstractDocService<LhScheFinishQty> implements ILhScheFinishQtyService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private LhScheFinishQtyMapper lhScheFinishQtyMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH_SCHE_FINISH";
    }

    @Override
    public int saveOrUpdateBatch(List<LhScheFinishQty> list) {
        baseDao.saveBatch(list);
        return list.size();
    }

    /**
     * 硫化排程完成量回写硫化排程结果表各班次完成量
     * <p>
     * 业务规则（T+1排程窗口）：
     * 1. 完成量回报表(T_LH_SCHE_FINISH_QTY)按机台+物料+排程日期汇总夜早中完成量
     * 2. 排程结果表(T_LH_SCHEDULE_RESULT)的8班对应3天窗口（排程日期=T+1日）：
     *    1~2班对应T-1日的早中，3~5班对应T日的夜早中，6~8班对应T+1日的夜早中
     * 3. 回报日期D的完成量需要更新排程日期为D-1、D、D+1的排程结果：
     *    - 排程日期D-1：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
     *    - 排程日期D：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
     *    - 排程日期D+1：1班(早)=MES2班，2班(中)=MES3班
     * 4. 排程结果维度：工厂+机台编码+物料编码+左右模+排程日期
     * </p>
     *
     * @param finishQtyList 完成量回报数据列表
     * @return 回写结果
     */
    @Override
    public AjaxResult writeBackScheduleResultFinishQty(List<LhScheFinishQty> finishQtyList) {
        if (CollectionUtils.isEmpty(finishQtyList)) {
            log.info("【完成量回写】完成量回报数据为空，跳过回写");
            return AjaxResult.success();
        }

        log.info("【完成量回写】开始回写硫化排程结果表完成量，回报数据条数：{}", finishQtyList.size());

        // Step1：按工厂+机台+物料+排程日期汇总，将夜早中合并成一条
        Map<String, LhScheFinishQty> summaryMap = summarizeFinishQty(finishQtyList);
        log.info("【完成量回写】汇总后数据条数：{}", summaryMap.size());

        int totalUpdateCount = 0;
        // 诊断统计：分类记录跳过原因，便于定位回填失败根因
        int scheduleDateNullCount = 0;       // 排程日期为空跳过
        int noResultCount = 0;               // 查询排程结果为空跳过
        int validateFailCount = 0;           // 示方类型/产品状态校验不通过跳过
        int invalidOffsetCount = 0;          // 日期偏移量异常跳过
        int updateZeroCount = 0;             // update返回0行（命中记录但未实际更新）
        int successUpdateCount = 0;          // 成功更新记录数（update>0）

        for (Map.Entry<String, LhScheFinishQty> entry : summaryMap.entrySet()) {
            LhScheFinishQty summary = entry.getValue();
            Date scheduleDate = summary.getScheduleDate();
            if (scheduleDate == null) {
                scheduleDateNullCount++;
                log.warn("【完成量回写】跳过[排程日期为空]：机台={}，物料={}", summary.getLhMachineCode(), summary.getMaterialCode());
                continue;
            }

            BigDecimal nightQty = summary.getClass1FinishQty() != null ? summary.getClass1FinishQty() : BigDecimal.ZERO;
            BigDecimal morningQty = summary.getClass2FinishQty() != null ? summary.getClass2FinishQty() : BigDecimal.ZERO;
            BigDecimal middleQty = summary.getClass3FinishQty() != null ? summary.getClass3FinishQty() : BigDecimal.ZERO;

            // Step2：计算需要查询的排程结果日期：D-1、D、D+1（标准化为当天零点）
            // T+1排程窗口下，回报日期D的夜早中班次分布在排程日期D-1的6/7/8班、D的3/4/5班、D+1的1/2班
            Date dateD = DateUtil.beginOfDay(scheduleDate);
            Date dateDMinus1 = DateUtil.offsetDay(dateD, -1);
            Date dateDPlus1 = DateUtil.offsetDay(dateD, 1);

            // Step3：查询排程结果表
            LambdaQueryWrapper<LhScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LhScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
            queryWrapper.eq(LhScheduleResult::getFactoryCode, summary.getFactoryCode());
            queryWrapper.eq(LhScheduleResult::getLhMachineCode, summary.getLhMachineCode());
            queryWrapper.eq(LhScheduleResult::getMaterialCode, summary.getMaterialCode());
            queryWrapper.in(LhScheduleResult::getScheduleDate, Arrays.asList(dateDMinus1, dateD, dateDPlus1));
            // 按产品状态过滤：避免同一物料+机台+日期下不同产品状态（正规/量试）的排程记录被错误回填
            String mesProductStatus = summary.getLhType();
            if (StringUtils.isNotEmpty(mesProductStatus)) {
                queryWrapper.eq(LhScheduleResult::getProductStatus, mesProductStatus);
            }
            List<LhScheduleResult> resultList = lhScheduleResultMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(resultList)) {
                noResultCount++;
                log.warn("【完成量回写】跳过[未找到排程结果]：工厂={}，机台={}，物料={}，MES物料={}，回报日期={}，查询日期范围={}~{}，MES产品状态={}，夜班示方类型={}，早班示方类型={}，中班示方类型={}",
                        summary.getFactoryCode(), summary.getLhMachineCode(), summary.getMaterialCode(),
                        summary.getMesMaterialCode(), DateUtil.formatDate(scheduleDate),
                        DateUtil.formatDate(dateDMinus1), DateUtil.formatDate(dateDPlus1),
                        mesProductStatus, summary.getClass1LhType(), summary.getClass2LhType(), summary.getClass3LhType());
                continue;
            }

            // Step4：按排程日期分组，分别回写
            for (LhScheduleResult result : resultList) {
                Date resultScheduleDate = result.getScheduleDate();
                int dayOffset = daysBetween(dateD, resultScheduleDate);

                // 回填前校验：通过物料编码+各班示方号去SKU与示方关系表校验示方类型是否一致
                if (!validateLhTypeConsistency(result, dayOffset, summary)) {
                    validateFailCount++;
                    log.warn("【完成量回写】跳过[示方类型/产品状态校验不通过]：工厂={}，机台={}，物料={}，排程日期={}，偏移={}天，MES产品状态={}，排程结果产品状态={}，排程结果ID={}",
                            summary.getFactoryCode(), summary.getLhMachineCode(), summary.getMaterialCode(),
                            DateUtil.formatDate(resultScheduleDate), dayOffset, mesProductStatus,
                            result.getProductStatus(), result.getId());
                    continue;
                }

                int updateCount = 0;
                if (dayOffset == -1) {
                    // 排程日期D-1：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
                    updateCount = updateDayMinus1FinishQty(result, nightQty, morningQty, middleQty, summary);
                } else if (dayOffset == 0) {
                    // 排程日期D：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
                    updateCount = updateDay0FinishQty(result, nightQty, morningQty, middleQty, summary);
                } else if (dayOffset == 1) {
                    // 排程日期D+1：1班(早)=MES2班，2班(中)=MES3班
                    updateCount = updateDay1FinishQty(result, morningQty, middleQty, summary);
                } else {
                    invalidOffsetCount++;
                    log.warn("【完成量回写】跳过[日期偏移量异常]：偏移={}天，排程日期={}，回报日期={}，排程结果ID={}",
                            dayOffset, DateUtil.formatDate(resultScheduleDate), DateUtil.formatDate(scheduleDate), result.getId());
                    continue;
                }

                if (updateCount == 0) {
                    updateZeroCount++;
                    log.warn("【完成量回写】update返回0行：排程结果ID={}，机台={}，物料={}，排程日期={}，偏移={}天，可能因update的where条件中示方类型不匹配（排程结果班次示方类型与MES回报不一致）",
                            result.getId(), summary.getLhMachineCode(), summary.getMaterialCode(),
                            DateUtil.formatDate(resultScheduleDate), dayOffset);
                } else {
                    successUpdateCount++;
                }
                totalUpdateCount += updateCount;
            }
        }

        // 诊断汇总：一次性输出全部统计，便于快速定位回填失败根因
        log.info("【完成量回写】回写完成汇总：汇总条数={}，成功更新记录={}，累计更新行数={}，跳过[排程日期为空]={}，跳过[未找到排程结果]={}，跳过[校验不通过]={}，跳过[日期偏移异常]={}，update返回0行={}",
                summaryMap.size(), successUpdateCount, totalUpdateCount,
                scheduleDateNullCount, noResultCount, validateFailCount, invalidOffsetCount, updateZeroCount);

        // 当回填全部失败时返回error，便于上层接口感知；update部分不抛异常，已更新行保留（下次回报按日期逻辑删除+插入覆盖）
        if (successUpdateCount == 0 && summaryMap.size() > 0) {
            String msg = String.format("回写0条成功（汇总%d条），跳过明细：未找到排程结果=%d，校验不通过=%d，update返回0行=%d，排程日期为空=%d，日期偏移异常=%d",
                    summaryMap.size(), noResultCount, validateFailCount, updateZeroCount, scheduleDateNullCount, invalidOffsetCount);
            log.error("【完成量回写】回写全部失败！{}", msg);
            return AjaxResult.error(msg);
        }
        return AjaxResult.success();
    }

    /**
     * 按工厂+机台+物料+排程日期汇总完成量
     * 将同一维度下的多条夜早中记录合并成一条
     *
     * @param finishQtyList 完成量原始列表
     * @return 汇总后的Map，key=工厂|机台|物料|排程日期
     */
    private Map<String, LhScheFinishQty> summarizeFinishQty(List<LhScheFinishQty> finishQtyList) {
        Map<String, LhScheFinishQty> summaryMap = new LinkedHashMap<>();

        for (LhScheFinishQty item : finishQtyList) {
            String key = buildSummaryKey(item);
            LhScheFinishQty existing = summaryMap.get(key);
            if (existing == null) {
                summaryMap.put(key, item);
            } else {
                // 汇总夜早中完成量（取非空值累加）
                existing.setClass1FinishQty(addBigDecimal(existing.getClass1FinishQty(), item.getClass1FinishQty()));
                existing.setClass2FinishQty(addBigDecimal(existing.getClass2FinishQty(), item.getClass2FinishQty()));
                existing.setClass3FinishQty(addBigDecimal(existing.getClass3FinishQty(), item.getClass3FinishQty()));
            }
        }
        return summaryMap;
    }

    /**
     * 构建汇总Key：工厂|机台|物料|排程日期|示方类型
     * <p>示方类型维度：不同示方类型的示方号可能相同，因此用示方类型而非示方号作为维度，
     * 避免不同示方类型的数据被错误合并</p>
     */
    private String buildSummaryKey(LhScheFinishQty item) {
        return item.getFactoryCode() + "|" + item.getLhMachineCode() + "|" + item.getMaterialCode() + "|"
                + DateUtil.formatDate(item.getScheduleDate()) + "|"
                + item.getClass1LhType() + "|" + item.getClass2LhType() + "|" + item.getClass3LhType();
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
    private boolean validateLhTypeConsistency(LhScheduleResult result, int dayOffset, LhScheFinishQty summary) {
        // 产品状态前置校验：MES回报的产品状态与排程结果的产品状态必须一致，
        // 避免班次示方类型为空时跳过校验导致正规完成量回填到量试记录（或反之）
        String mesProductStatus = summary.getLhType();
        if (StringUtils.isNotEmpty(mesProductStatus)
                && StringUtils.isNotEmpty(result.getProductStatus())
                && !mesProductStatus.equals(result.getProductStatus())) {
            log.warn("【产品状态校验】不一致！物料={}，MES产品状态={}，排程结果产品状态={}",
                    summary.getMaterialCode(), mesProductStatus, result.getProductStatus());
            return false;
        }

        List<String> mismatchShifts = new ArrayList<>();

        if (dayOffset == -1) {
            // 排程日期D-1：6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
            if (!checkSingleShiftLhType(summary.getClass1LhType(), result.getClass6LhType(), "6班(MES1班)")) {
                mismatchShifts.add("6班");
            }
            if (!checkSingleShiftLhType(summary.getClass2LhType(), result.getClass7LhType(), "7班(MES2班)")) {
                mismatchShifts.add("7班");
            }
            if (!checkSingleShiftLhType(summary.getClass3LhType(), result.getClass8LhType(), "8班(MES3班)")) {
                mismatchShifts.add("8班");
            }
        } else if (dayOffset == 0) {
            // 排程日期D：3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
            if (!checkSingleShiftLhType(summary.getClass1LhType(), result.getClass3LhType(), "3班(MES1班)")) {
                mismatchShifts.add("3班");
            }
            if (!checkSingleShiftLhType(summary.getClass2LhType(), result.getClass4LhType(), "4班(MES2班)")) {
                mismatchShifts.add("4班");
            }
            if (!checkSingleShiftLhType(summary.getClass3LhType(), result.getClass5LhType(), "5班(MES3班)")) {
                mismatchShifts.add("5班");
            }
        } else if (dayOffset == 1) {
            // 排程日期D+1：1班(早)=MES2班，2班(中)=MES3班
            if (!checkSingleShiftLhType(summary.getClass2LhType(), result.getClass1LhType(), "1班(MES2班)")) {
                mismatchShifts.add("1班");
            }
            if (!checkSingleShiftLhType(summary.getClass3LhType(), result.getClass2LhType(), "2班(MES3班)")) {
                mismatchShifts.add("2班");
            }
        }

        if (!mismatchShifts.isEmpty()) {
            log.warn("【示方类型校验】以下班次校验不通过，物料={}，不一致班次：{}",
                    summary.getMaterialCode(), String.join(",", mismatchShifts));
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
     * @param mesLhType       MES回报的示方类型
     * @param resultLhType    排程结果中对应班次的示方类型
     * @param shiftDesc       班次描述（用于日志）
     * @return true-一致或无需校验；false-不一致
     */
    private boolean checkSingleShiftLhType(String mesLhType, String resultLhType, String shiftDesc) {
        // 如果MES回报的示方类型为空，跳过该校验（兼容旧数据）
        if (StringUtils.isEmpty(mesLhType)) {
            return true;
        }

        // 如果排程结果中对应班次的示方类型为空，跳过校验（兼容旧数据）
        if (StringUtils.isEmpty(resultLhType)) {
            return true;
        }

        if (!mesLhType.equals(resultLhType)) {
            log.warn("【示方类型校验】类型不一致！班次={}，MES类型={}，排程结果类型={}",
                    shiftDesc, mesLhType, resultLhType);
            return false;
        }

        return true;
    }

    /**
     * 更新排程日期D-1（前一天）的完成量
     * 6班(夜)=MES1班，7班(早)=MES2班，8班(中)=MES3班
     *
     * @param result    排程结果
     * @param nightQty  夜班完成量
     * @param morningQty 早班完成量
     * @param middleQty 中班完成量
     * @param summary   完成量回报汇总数据（用于示方号匹配）
     * @return 更新记录数
     */
    private int updateDayMinus1FinishQty(LhScheduleResult result, BigDecimal nightQty, BigDecimal morningQty, BigDecimal middleQty, LhScheFinishQty summary) {
        LambdaUpdateWrapper<LhScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LhScheduleResult::getId, result.getId());
        updateWrapper.eq(LhScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        // 产品状态匹配条件：确保只更新对应产品状态的记录，避免正规完成量回填到量试记录（或反之）
        String productStatus = summary.getLhType();
        if (StringUtils.isNotEmpty(productStatus)) {
            updateWrapper.eq(LhScheduleResult::getProductStatus, productStatus);
        }
        // 示方类型匹配条件：用示方类型而非示方号匹配，避免不同示方类型示方号相同时回填错误
        if (StringUtils.isNotEmpty(summary.getClass1LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass6LhType, summary.getClass1LhType());  // 6班=MES1班
        }
        if (StringUtils.isNotEmpty(summary.getClass2LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass7LhType, summary.getClass2LhType());  // 7班=MES2班
        }
        if (StringUtils.isNotEmpty(summary.getClass3LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass8LhType, summary.getClass3LhType());  // 8班=MES3班
        }
        updateWrapper.set(LhScheduleResult::getClass6FinishQty, nightQty.intValue());
        updateWrapper.set(LhScheduleResult::getClass7FinishQty, morningQty.intValue());
        updateWrapper.set(LhScheduleResult::getClass8FinishQty, middleQty.intValue());
        int count = lhScheduleResultMapper.update(null, updateWrapper);
        log.info("【完成量回写】排程日期D-1更新，ID：{}，6班(夜)={}，7班(早)={}，8班(中)={}，更新行数：{}",
                result.getId(), nightQty, morningQty, middleQty, count);
        return count;
    }

    /**
     * 更新排程日期D（当天）的完成量
     * 3班(夜)=MES1班，4班(早)=MES2班，5班(中)=MES3班
     *
     * @param result    排程结果
     * @param nightQty  夜班完成量
     * @param morningQty 早班完成量
     * @param middleQty 中班完成量
     * @param summary   完成量回报汇总数据（用于示方号匹配）
     * @return 更新记录数
     */
    private int updateDay0FinishQty(LhScheduleResult result, BigDecimal nightQty, BigDecimal morningQty, BigDecimal middleQty, LhScheFinishQty summary) {
        LambdaUpdateWrapper<LhScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LhScheduleResult::getId, result.getId());
        updateWrapper.eq(LhScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        // 产品状态匹配条件：确保只更新对应产品状态的记录，避免正规完成量回填到量试记录（或反之）
        String productStatus = summary.getLhType();
        if (StringUtils.isNotEmpty(productStatus)) {
            updateWrapper.eq(LhScheduleResult::getProductStatus, productStatus);
        }
        // 示方类型匹配条件：用示方类型而非示方号匹配，避免不同示方类型示方号相同时回填错误
        if (StringUtils.isNotEmpty(summary.getClass1LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass3LhType, summary.getClass1LhType());  // 3班=MES1班
        }
        if (StringUtils.isNotEmpty(summary.getClass2LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass4LhType, summary.getClass2LhType());  // 4班=MES2班
        }
        if (StringUtils.isNotEmpty(summary.getClass3LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass5LhType, summary.getClass3LhType());  // 5班=MES3班
        }
        updateWrapper.set(LhScheduleResult::getClass3FinishQty, nightQty.intValue());
        updateWrapper.set(LhScheduleResult::getClass4FinishQty, morningQty.intValue());
        updateWrapper.set(LhScheduleResult::getClass5FinishQty, middleQty.intValue());
        int count = lhScheduleResultMapper.update(null, updateWrapper);
        log.info("【完成量回写】排程日期D更新，ID：{}，3班(夜)={}，4班(早)={}，5班(中)={}，更新行数：{}",
                result.getId(), nightQty, morningQty, middleQty, count);
        return count;
    }

    /**
     * 更新排程日期D+1（次日）的完成量
     * 1班(早)=MES2班，2班(中)=MES3班
     * 注意：D+1没有夜班（D的夜班不在D+1的窗口内）
     *
     * @param result    排程结果
     * @param morningQty 早班完成量
     * @param middleQty 中班完成量
     * @param summary   完成量回报汇总数据（用于示方号匹配）
     * @return 更新记录数
     */
    private int updateDay1FinishQty(LhScheduleResult result, BigDecimal morningQty, BigDecimal middleQty, LhScheFinishQty summary) {
        LambdaUpdateWrapper<LhScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LhScheduleResult::getId, result.getId());
        updateWrapper.eq(LhScheduleResult::getIsDelete, ApsConstant.APS_YES_NO_0);
        // 产品状态匹配条件：确保只更新对应产品状态的记录，避免正规完成量回填到量试记录（或反之）
        String productStatus = summary.getLhType();
        if (StringUtils.isNotEmpty(productStatus)) {
            updateWrapper.eq(LhScheduleResult::getProductStatus, productStatus);
        }
        // 示方类型匹配条件：用示方类型而非示方号匹配，避免不同示方类型示方号相同时回填错误
        if (StringUtils.isNotEmpty(summary.getClass2LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass1LhType, summary.getClass2LhType());  // 1班=MES2班
        }
        if (StringUtils.isNotEmpty(summary.getClass3LhType())) {
            updateWrapper.eq(LhScheduleResult::getClass2LhType, summary.getClass3LhType());  // 2班=MES3班
        }
        updateWrapper.set(LhScheduleResult::getClass1FinishQty, morningQty.intValue());
        updateWrapper.set(LhScheduleResult::getClass2FinishQty, middleQty.intValue());
        int count = lhScheduleResultMapper.update(null, updateWrapper);
        log.info("【完成量回写】排程日期D+1更新，ID：{}，1班(早)={}，2班(中)={}，更新行数：{}",
                result.getId(), morningQty, middleQty, count);
        return count;
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy, List<LhScheFinishQty> insertList) {
        log.info("硫化排程完成量同步-事务开始：逻辑删除分厂{}排程日期为{}的旧数据，待插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
        lhScheFinishQtyMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate, updateBy, new Date());
        log.info("硫化排程完成量同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<LhScheFinishQty> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("硫化排程完成量同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("硫化排程完成量同步-事务完成：分厂{}，排程日期={}，插入数量={}", factoryCode, scheduleDate, CollectionUtils.size(insertList));
    }
}
