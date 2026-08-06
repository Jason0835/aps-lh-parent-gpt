package com.zlt.aps.cd90.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.mapper.Cd90ScheFinishQtyMapper;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.mapper.Cd90ShiftConfigMapper;
import com.zlt.aps.cd90.service.ICd90ScheFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 直裁排程每日完成量服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheFinishQtyServiceImpl extends AbstractDocService<Cd90ScheFinishQty>
        implements ICd90ScheFinishQtyService {

    private static final String SHIFT_CODE_NIGHT = "01";
    private static final String SHIFT_CODE_EARLY = "02";
    private static final String SHIFT_CODE_MIDDLE = "03";
    private static final int RESULT_SCHEDULE_DAY = 2;
    private static final int BATCH_SIZE = 1000;

    private final BaseDao baseDao;
    private final Cd90ScheFinishQtyMapper cd90ScheFinishQtyMapper;
    private final Cd90ScheduleResultMapper cd90ScheduleResultMapper;
    private final Cd90ShiftConfigMapper cd90ShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD90_SCHE_FINISH_QTY";
    }

    /**
     * 按工厂和MES归属日期替换每日完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES完成量归属日期
     * @param updateBy 更新人
     * @param finishQtyList 每日完成量列表
     */
    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy,
                                        List<Cd90ScheFinishQty> finishQtyList) {
        if (!hasText(factoryCode) || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.cd90.scheFinishQty.required"));
        }
        List<Cd90ScheFinishQty> safeList = finishQtyList == null ? new ArrayList<>() : finishQtyList;
        this.validateBatch(factoryCode, scheduleDate, safeList);

        Date now = new Date();
        this.cd90ScheFinishQtyMapper.logicDeleteByFactoryCodeAndScheduleDate(factoryCode, scheduleDate,
                updateBy, now);
        if (CollectionUtils.isEmpty(safeList)) {
            log.info("直裁每日完成量同步完成：工厂={}，归属日期={}，新数据为空，仅清理旧数据",
                    factoryCode, DateUtil.formatDate(scheduleDate));
            return;
        }

        safeList.forEach(item -> {
            item.setIsDelete(0);
            item.setCreateBy(hasText(item.getCreateBy()) ? item.getCreateBy() : updateBy);
            item.setUpdateBy(updateBy);
            item.setCreateTime(item.getCreateTime() == null ? now : item.getCreateTime());
            item.setUpdateTime(now);
        });
        for (int beginIndex = 0; beginIndex < safeList.size(); beginIndex += BATCH_SIZE) {
            int endIndex = Math.min(beginIndex + BATCH_SIZE, safeList.size());
            this.baseDao.saveBatch(safeList.subList(beginIndex, endIndex));
        }
        log.info("直裁每日完成量同步完成：工厂={}，归属日期={}，插入数量={}",
                factoryCode, DateUtil.formatDate(scheduleDate), safeList.size());
    }

    /**
     * 按启用班次配置将每日夜、早、中三班回写到排程结果的相对班次字段。
     *
     * @param finishQtyList 每日完成量列表
     * @return 回写结果
     */
    @Override
    public AjaxResult writeBackScheduleResultFinishQty(List<Cd90ScheFinishQty> finishQtyList) {
        if (CollectionUtils.isEmpty(finishQtyList)) {
            return AjaxResult.success();
        }
        this.validateBatch(null, null, finishQtyList);

        Set<String> orderNoSet = finishQtyList.stream()
                .map(Cd90ScheFinishQty::getOrderNo)
                .collect(Collectors.toSet());
        Set<String> factoryCodeSet = finishQtyList.stream()
                .map(Cd90ScheFinishQty::getFactoryCode)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<Cd90ScheduleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.in(Cd90ScheduleResult::getOrderNo, orderNoSet);
        resultWrapper.in(Cd90ScheduleResult::getFactoryCode, factoryCodeSet);
        List<Cd90ScheduleResult> scheduleResults = this.cd90ScheduleResultMapper.selectList(resultWrapper);
        Map<String, List<Cd90ScheduleResult>> resultMap = scheduleResults.stream()
                .collect(Collectors.groupingBy(this::buildResultKey, LinkedHashMap::new, Collectors.toList()));

        LambdaQueryWrapper<Cd90ShiftConfig> shiftWrapper = new LambdaQueryWrapper<>();
        shiftWrapper.in(Cd90ShiftConfig::getFactoryCode, factoryCodeSet);
        shiftWrapper.eq(Cd90ShiftConfig::getIsActive, 1);
        shiftWrapper.orderByAsc(Cd90ShiftConfig::getScheduleDay)
                .orderByAsc(Cd90ShiftConfig::getShiftOrder);
        Map<String, List<Cd90ShiftConfig>> shiftConfigMap = this.cd90ShiftConfigMapper.selectList(shiftWrapper)
                .stream().collect(Collectors.groupingBy(Cd90ShiftConfig::getFactoryCode));

        int updateCount = 0;
        for (Cd90ScheFinishQty finishQty : finishQtyList) {
            List<Cd90ScheduleResult> matchedResults = resultMap.get(this.buildFeedbackResultKey(finishQty));
            if (CollectionUtils.isEmpty(matchedResults)) {
                log.warn("直裁每日完成量未找到排程结果：工厂={}，工单号={}",
                        finishQty.getFactoryCode(), finishQty.getOrderNo());
                continue;
            }
            List<Cd90ShiftConfig> shiftConfigs = shiftConfigMap.get(finishQty.getFactoryCode());
            if (CollectionUtils.isEmpty(shiftConfigs)) {
                log.warn("直裁每日完成量未找到启用班次配置：工厂={}", finishQty.getFactoryCode());
                continue;
            }
            Map<String, ShiftFeedback> feedbackMap = this.buildShiftFeedbackMap(finishQty);
            for (Cd90ScheduleResult scheduleResult : matchedResults) {
                if (!this.matchesScheduleResult(finishQty, scheduleResult)) {
                    continue;
                }
                Cd90ScheduleResult updateEntity = new Cd90ScheduleResult();
                updateEntity.setId(scheduleResult.getId());
                if (this.applyFinishQty(updateEntity, scheduleResult.getScheduleDate(),
                        finishQty.getScheduleDate(), shiftConfigs, feedbackMap)) {
                    updateEntity.setUpdateBy("MES");
                    updateEntity.setUpdateTime(new Date());
                    updateCount += this.cd90ScheduleResultMapper.updateById(updateEntity);
                }
            }
        }
        log.info("直裁每日完成量回写完成：回报数量={}，更新排程结果数量={}", finishQtyList.size(), updateCount);
        return AjaxResult.success();
    }

    /**
     * 校验批量同步范围、每日业务键和完成量。
     */
    private void validateBatch(String factoryCode, Date scheduleDate, List<Cd90ScheFinishQty> finishQtyList) {
        Set<String> dailyKeySet = new HashSet<>();
        for (Cd90ScheFinishQty finishQty : finishQtyList) {
            this.validateFinishQty(finishQty);
            if (hasText(factoryCode) && !Objects.equals(factoryCode, finishQty.getFactoryCode())) {
                // 直裁完成量回报与本次同步工厂或日期不一致
                throw new ServiceException(I18nUtil.getMessage("ui.cd90.scheFinishQty.scopeMismatch"));
            }
            if (scheduleDate != null && !DateUtil.isSameDay(scheduleDate, finishQty.getScheduleDate())) {
                // 直裁完成量回报与本次同步工厂或日期不一致
                throw new ServiceException(I18nUtil.getMessage("ui.cd90.scheFinishQty.scopeMismatch"));
            }
            if (!dailyKeySet.add(this.buildDailyKey(finishQty))) {
                // 直裁完成量回报存在重复的每日记录
                throw new ServiceException(I18nUtil.getMessage("ui.cd90.scheFinishQty.duplicate"));
            }
        }
    }

    /**
     * 校验单条每日完成量必填字段和非负数量。
     */
    private void validateFinishQty(Cd90ScheFinishQty finishQty) {
        if (finishQty == null || !hasText(finishQty.getFactoryCode()) || !hasText(finishQty.getOrderNo())
                || finishQty.getScheduleDate() == null || !hasText(finishQty.getMachineCode())) {
            // 直裁完成量回报的工厂、工单号、归属日期和机台不能为空
            throw new ServiceException(I18nUtil.getMessage("ui.cd90.scheFinishQty.required"));
        }
        List<BigDecimal> quantities = Arrays.asList(finishQty.getClass1FinishQty(),
                finishQty.getClass2FinishQty(), finishQty.getClass3FinishQty());
        if (quantities.stream().filter(Objects::nonNull).anyMatch(quantity -> quantity.signum() < 0)) {
            // 直裁完成量不能为负数
            throw new ServiceException(I18nUtil.getMessage("ui.cd90.scheFinishQty.negative"));
        }
    }

    /**
     * 构建每日记录业务键。
     */
    private String buildDailyKey(Cd90ScheFinishQty finishQty) {
        return finishQty.getFactoryCode() + "|" + finishQty.getOrderNo() + "|"
                + DateUtil.formatDate(finishQty.getScheduleDate()) + "|" + finishQty.getMachineCode();
    }

    /**
     * 构建每日回报与排程结果的分组键。
     */
    private String buildFeedbackResultKey(Cd90ScheFinishQty finishQty) {
        return finishQty.getFactoryCode() + "|" + finishQty.getOrderNo();
    }

    /**
     * 构建排程结果分组键。
     */
    private String buildResultKey(Cd90ScheduleResult scheduleResult) {
        return scheduleResult.getFactoryCode() + "|" + scheduleResult.getOrderNo();
    }

    /**
     * 校验机台及可用物料追溯字段与排程结果一致。
     */
    private boolean matchesScheduleResult(Cd90ScheFinishQty finishQty, Cd90ScheduleResult scheduleResult) {
        boolean matched = Objects.equals(finishQty.getMachineCode(), scheduleResult.getMachineCode())
                && matchesOptional(finishQty.getClothCode(), scheduleResult.getClothCode())
                && matchesOptional(finishQty.getBigRollCode(), scheduleResult.getBigRollCode());
        if (!matched) {
            log.warn("直裁每日完成量与排程结果不一致，跳过回写：工厂={}，工单号={}，回报机台={}，排程机台={}",
                    finishQty.getFactoryCode(), finishQty.getOrderNo(), finishQty.getMachineCode(),
                    scheduleResult.getMachineCode());
        }
        return matched;
    }

    /**
     * 将同一天同一物理班次的完成量写入对应CLASS字段。
     */
    private boolean applyFinishQty(Cd90ScheduleResult updateEntity, Date resultScheduleDate, Date feedbackDate,
                                   List<Cd90ShiftConfig> shiftConfigs,
                                   Map<String, ShiftFeedback> feedbackMap) {
        if (resultScheduleDate == null || feedbackDate == null) {
            return false;
        }
        LocalDate resultDate = DateUtil.toLocalDateTime(resultScheduleDate).toLocalDate();
        LocalDate actualFeedbackDate = DateUtil.toLocalDateTime(feedbackDate).toLocalDate();
        boolean changed = false;
        for (Cd90ShiftConfig shiftConfig : shiftConfigs) {
            if (!isValidClassField(shiftConfig.getClassField())) {
                continue;
            }
            LocalDate shiftDate = this.resolveShiftReportDate(resultDate, shiftConfig);
            ShiftFeedback shiftFeedback = feedbackMap.get(shiftConfig.getShiftCode());
            if (!Objects.equals(actualFeedbackDate, shiftDate) || shiftFeedback == null
                    || (shiftFeedback.finishQty == null && !hasText(shiftFeedback.unfinishedReason))) {
                continue;
            }
            String classIndex = shiftConfig.getClassField().substring("CLASS".length());
            if (shiftFeedback.finishQty != null) {
                String finishField = String.format("class%sFinishQty", classIndex);
                updateEntity.setFieldValueByFieldName(finishField, shiftFeedback.finishQty.doubleValue());
            }
            if (hasText(shiftFeedback.unfinishedReason)) {
                String analysisField = String.format("class%sAnalysisInput", classIndex);
                updateEntity.setFieldValueByFieldName(analysisField, shiftFeedback.unfinishedReason);
            }
            changed = true;
        }
        return changed;
    }

    /**
     * 按排程日序和跨天标识计算MES完成量归属日期。
     */
    private LocalDate resolveShiftReportDate(LocalDate resultDate, Cd90ShiftConfig shiftConfig) {
        if (shiftConfig.getScheduleDay() == null) {
            return null;
        }
        int dayOffset = shiftConfig.getScheduleDay() - RESULT_SCHEDULE_DAY;
        if (Integer.valueOf(1).equals(shiftConfig.getIsCrossDay())) {
            dayOffset++;
        }
        return resultDate.plusDays(dayOffset);
    }

    /**
     * 将MES三班字段按物理班次编码组织，避免把每日CLASS与排程CLASS混用。
     */
    private Map<String, ShiftFeedback> buildShiftFeedbackMap(Cd90ScheFinishQty finishQty) {
        Map<String, ShiftFeedback> feedbackMap = new HashMap<>();
        feedbackMap.put(SHIFT_CODE_NIGHT,
                new ShiftFeedback(finishQty.getClass1FinishQty(), finishQty.getClass1UnReason()));
        feedbackMap.put(SHIFT_CODE_EARLY,
                new ShiftFeedback(finishQty.getClass2FinishQty(), finishQty.getClass2UnReason()));
        feedbackMap.put(SHIFT_CODE_MIDDLE,
                new ShiftFeedback(finishQty.getClass3FinishQty(), finishQty.getClass3UnReason()));
        return feedbackMap;
    }

    private static boolean isValidClassField(String classField) {
        return hasText(classField) && classField.matches("CLASS[1-8]");
    }

    private static boolean matchesOptional(String feedbackValue, String resultValue) {
        return !hasText(feedbackValue) || Objects.equals(feedbackValue, resultValue);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 单个物理班次的MES回报值。
     */
    private static final class ShiftFeedback {
        private final BigDecimal finishQty;
        private final String unfinishedReason;

        private ShiftFeedback(BigDecimal finishQty, String unfinishedReason) {
            this.finishQty = finishQty;
            this.unfinishedReason = unfinishedReason;
        }
    }
}
