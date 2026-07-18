package com.zlt.aps.tm.task;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.mapper.TmShiftConfigMapper;
import com.zlt.aps.tm.service.ITmRollingUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面自动滚动分钟级定时任务。
 *
 * <p>仅扫描显式配置 {@code TM_ROLLING_ENABLED=1} 的工厂。在下一逻辑班次开始前达到提前量时，
 * 调用与手动入口相同的分布式锁和幂等服务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmRollingRecalcScheduler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private final TmParamsMapper tmParamsMapper;

    private final TmShiftConfigMapper tmShiftConfigMapper;

    private final ITmRollingUpdateService tmRollingUpdateService;

    /**
     * 每分钟扫描一次已启用工厂的下一逻辑班次。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanRollingWindows() {
        this.loadEnabledFactories().forEach(factoryCode -> {
            try {
                this.triggerFactoryIfDue(factoryCode, LocalDateTime.now());
            } catch (RuntimeException ex) {
                log.error("[TM_ROLLING_JOB] factoryCode={} 自动滚动触发失败", factoryCode, ex);
            }
        });
    }

    /**
     * 加载明确开启自动滚动的工厂编码。
     *
     * @return 去重工厂编码
     */
    private Set<String> loadEnabledFactories() {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getParamCode, TmScheduleConstants.PARAM_ROLLING_ENABLED);
        wrapper.eq(TmParams::getParamValue, "1");
        wrapper.eq(TmParams::getEnableStatus, "1");
        List<TmParams> paramsList = tmParamsMapper.selectList(wrapper);
        return (paramsList == null ? Collections.<TmParams>emptyList() : paramsList).stream()
                .map(TmParams::getFactoryCode).filter(StrUtil::isNotBlank).map(StrUtil::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断工厂当前分钟是否达到提前触发窗口。
     *
     * @param factoryCode 工厂编号
     * @param now 当前时间
     */
    void triggerFactoryIfDue(String factoryCode, LocalDateTime now) {
        int leadMinutes = this.loadLeadMinutes(factoryCode);
        LocalDateTime triggerMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(leadMinutes);
        List<TmShiftConfig> shiftConfigList = this.loadOpenShiftConfigs(factoryCode);
        for (int baseOffset = -1; baseOffset <= 1; baseOffset++) {
            LocalDate scheduleDate = now.toLocalDate().plusDays(baseOffset);
            for (TmShiftConfig shiftConfig : shiftConfigList) {
                LocalDateTime shiftStart = this.resolveShiftStart(scheduleDate, shiftConfig);
                if (shiftStart == null || !shiftStart.truncatedTo(ChronoUnit.MINUTES).equals(triggerMinute)) {
                    continue;
                }
                TmRollingRecalcRequestDTO request = new TmRollingRecalcRequestDTO();
                request.setFactoryCode(factoryCode);
                request.setScheduleDate(Date.from(scheduleDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                request.setTargetShiftOrder(shiftConfig.getShiftOrder());
                request.setOperator("TM_ROLLING_JOB");
                tmRollingUpdateService.rollingRecalcAutomatically(request);
                return;
            }
        }
    }

    /**
     * 加载工厂已开班的六班配置。
     *
     * @param factoryCode 工厂编号
     * @return 班次配置
     */
    private List<TmShiftConfig> loadOpenShiftConfigs(String factoryCode) {
        LambdaQueryWrapper<TmShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmShiftConfig::getFactoryCode, factoryCode);
        wrapper.eq(TmShiftConfig::getOpenFlag, "1");
        wrapper.between(TmShiftConfig::getShiftOrder, 1, TmScheduleConstants.TM_MAX_SHIFT_ORDER);
        wrapper.orderByAsc(TmShiftConfig::getShiftOrder);
        List<TmShiftConfig> shiftConfigList = tmShiftConfigMapper.selectList(wrapper);
        return shiftConfigList == null ? Collections.emptyList() : shiftConfigList;
    }

    /**
     * 将排程日期、班次序号和计划开始时间解析为实际开始时间。
     *
     * @param scheduleDate 排程日期
     * @param shiftConfig 班次配置
     * @return 实际开始时间；配置无效时返回 null
     */
    LocalDateTime resolveShiftStart(LocalDate scheduleDate, TmShiftConfig shiftConfig) {
        if (scheduleDate == null || shiftConfig == null || shiftConfig.getShiftOrder() == null
                || StrUtil.isBlank(shiftConfig.getPlanStartTime())) {
            return null;
        }
        try {
            LocalTime startTime = LocalTime.parse(shiftConfig.getPlanStartTime().trim(), TIME_FORMATTER);
            int dayOffset = (shiftConfig.getShiftOrder() - 1) / 3;
            return scheduleDate.plusDays(dayOffset).atTime(startTime);
        } catch (DateTimeParseException ex) {
            log.warn("[TM_ROLLING_JOB] 班次开始时间无效，shiftOrder={}，planStartTime={}",
                    shiftConfig.getShiftOrder(), shiftConfig.getPlanStartTime());
            return null;
        }
    }

    /**
     * 加载提前触发分钟数，非法值使用默认三十分钟。
     *
     * @param factoryCode 工厂编号
     * @return 非负提前分钟数
     */
    private int loadLeadMinutes(String factoryCode) {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, factoryCode);
        wrapper.eq(TmParams::getParamCode, TmScheduleConstants.PARAM_ROLLING_LEAD_MINUTES);
        wrapper.eq(TmParams::getEnableStatus, "1");
        wrapper.orderByDesc(TmParams::getId);
        List<TmParams> paramsList = tmParamsMapper.selectList(wrapper);
        if (paramsList == null || paramsList.isEmpty()) {
            return TmScheduleConstants.DEFAULT_ROLLING_LEAD_MINUTES;
        }
        try {
            return Math.max(Integer.parseInt(StrUtil.blankToDefault(paramsList.get(0).getParamValue(),
                    String.valueOf(TmScheduleConstants.DEFAULT_ROLLING_LEAD_MINUTES))), 0);
        } catch (NumberFormatException ex) {
            return TmScheduleConstants.DEFAULT_ROLLING_LEAD_MINUTES;
        }
    }
}
