package com.zlt.aps.tc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.domain.vo.TcRollingCheckRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcRollingTaskVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcBackgroundTaskTypeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.vo.TcRollingWindow;
import com.zlt.aps.tc.mapper.*;
import com.zlt.aps.tc.service.mes.TcShiftBusinessDateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 胎侧自动滚动窗口识别、库存同步和任务防重应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcAutoRollingApplicationService {

    private final TcShiftConfigMapper shiftConfigMapper;
    private final TcParamsMapper paramsMapper;
    private final TcStockMapper stockMapper;
    private final TcScheduleResultMapper scheduleResultMapper;
    private final TcAutoScheduleTaskMapper taskMapper;
    private final TcBackgroundTaskService backgroundTaskService;
    private final TcAutoRollingAsyncExecutor asyncExecutor;
    private final IMesItfService mesItfService;

    /**
     * 检查全部命中窗口，先同步库存，再为稳定输入创建唯一异步任务。
     *
     * @param request 检查请求
     * @return 已创建或复用的任务
     */
    public List<TcRollingTaskVo> checkAndSubmit(TcRollingCheckRequestVo request) {
        Date triggerTime = request == null || request.getTriggerTime() == null
                ? new Date() : request.getTriggerTime();
        String factoryCode = request == null ? null : request.getFactoryCode();
        List<TcRollingWindow> windowList = this.resolveRollingWindows(factoryCode, triggerTime);
        List<TcRollingTaskVo> taskList = new ArrayList<>();
        for (TcRollingWindow window : windowList) {
            if (!this.isRollingEnabled(window.getFactoryCode(), window.getScheduleDate())) {
                continue;
            }
            this.syncStock(window.getFactoryCode());
            TcAutoScheduleTask task = this.createTask(window, triggerTime);
            if (task != null) {
                taskList.add(this.toRollingTaskVo(task));
                if (TcAutoScheduleTaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
                    this.asyncExecutor.execute(task.getTaskId());
                }
            }
        }
        return taskList;
    }

    /**
     * 按工厂、MES物理日期偏移和配置时刻识别当前触发窗口。
     *
     * @param factoryCode 可选工厂
     * @param triggerTime 触发时间
     * @return 命中窗口
     */
    List<TcRollingWindow> resolveRollingWindows(String factoryCode, Date triggerTime) {
        Date startScheduleDate = DateUtil.beginOfDay(DateUtil.offsetDay(triggerTime, -1));
        Date endScheduleDate = DateUtil.endOfDay(DateUtil.offsetDay(triggerTime, 1));
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<TcShiftConfig>()
                .between(TcShiftConfig::getScheduleDate, startScheduleDate, endScheduleDate)
                .eq(TcShiftConfig::getOpenFlag, "1")
                .orderByAsc(TcShiftConfig::getFactoryCode)
                .orderByAsc(TcShiftConfig::getScheduleDate)
                .orderByAsc(TcShiftConfig::getShiftOrder);
        wrapper.eq(StrUtil.isNotBlank(factoryCode), TcShiftConfig::getFactoryCode, factoryCode);
        List<TcShiftConfig> configList = this.shiftConfigMapper.selectList(wrapper);
        Map<String, TcRollingWindow> closestWindowMap = new LinkedHashMap<>();
        CollectionUtils.emptyIfNull(configList).stream()
                .filter(config -> config.getShiftOrder() != null && config.getShiftOrder() >= 1
                        && config.getShiftOrder() <= TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .forEach(config -> {
                    int earlyMinutes = this.readIntegerParam(config.getFactoryCode(), config.getScheduleDate(),
                            TcScheduleConstants.PARAM_ROLLING_EARLY_MINUTES,
                            TcScheduleConstants.DEFAULT_ROLLING_EARLY_MINUTES);
                    int lateMinutes = this.readIntegerParam(config.getFactoryCode(), config.getScheduleDate(),
                            TcScheduleConstants.PARAM_ROLLING_LATE_MINUTES,
                            TcScheduleConstants.DEFAULT_ROLLING_LATE_MINUTES);
                    Date shiftStartTime = this.resolveShiftStartTime(config);
                    if (shiftStartTime == null || triggerTime.before(DateUtil.offsetMinute(shiftStartTime, -earlyMinutes))
                            || triggerTime.after(DateUtil.offsetMinute(shiftStartTime, lateMinutes))) {
                        return;
                    }
                    TcRollingWindow candidate = new TcRollingWindow();
                    candidate.setFactoryCode(config.getFactoryCode());
                    candidate.setScheduleDate(config.getScheduleDate());
                    candidate.setTargetShiftOrder(config.getShiftOrder());
                    candidate.setShiftStartTime(shiftStartTime);
                    String mapKey = config.getFactoryCode() + "|" + DateUtil.formatDate(config.getScheduleDate());
                    TcRollingWindow existing = closestWindowMap.get(mapKey);
                    if (existing == null || Math.abs(shiftStartTime.getTime() - triggerTime.getTime())
                            < Math.abs(existing.getShiftStartTime().getTime() - triggerTime.getTime())) {
                        closestWindowMap.put(mapKey, candidate);
                    }
                });
        return new ArrayList<>(closestWindowMap.values());
    }

    /**
     * 在事务内按稳定输入指纹创建或复用自动滚动任务。
     *
     * @param window 滚动窗口
     * @param triggerTime 触发时间
     * @return 任务；无当前排程结果时返回null
     */
    @Transactional(rollbackFor = Exception.class)
    public TcAutoScheduleTask createTask(TcRollingWindow window, Date triggerTime) {
        TcAutoScheduleTask activeTask = this.backgroundTaskService.findActive(
                window.getFactoryCode(), window.getScheduleDate());
        if (activeTask != null) {
            return TcBackgroundTaskTypeEnum.AUTO_ROLLING.getCode().equals(activeTask.getTaskType())
                    ? activeTask : null;
        }
        List<TcScheduleResult> resultList = this.loadCurrentBatchResults(window);
        if (resultList.isEmpty()) {
            return null;
        }
        if (!this.isStockInputStable(window, triggerTime)) {
            log.info("胎侧自动滚动输入尚未达到稳定时间，factoryCode={}, scheduleDate={}, shiftOrder={}",
                    window.getFactoryCode(), DateUtil.formatDate(window.getScheduleDate()),
                    window.getTargetShiftOrder());
            return null;
        }
        String inputVersion = this.buildInputVersion(window, resultList);
        TcAutoScheduleTask duplicateTask = this.taskMapper.selectOne(
                new LambdaQueryWrapper<TcAutoScheduleTask>()
                        .eq(TcAutoScheduleTask::getFactoryCode, window.getFactoryCode())
                        .eq(TcAutoScheduleTask::getScheduleDate, window.getScheduleDate())
                        .eq(TcAutoScheduleTask::getTaskType, TcBackgroundTaskTypeEnum.AUTO_ROLLING.getCode())
                        .eq(TcAutoScheduleTask::getTargetShiftOrder, window.getTargetShiftOrder())
                        .eq(TcAutoScheduleTask::getInputVersion, inputVersion)
                        .in(TcAutoScheduleTask::getTaskStatus, Arrays.asList(
                                TcAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                TcAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                                TcAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                        .orderByDesc(TcAutoScheduleTask::getCreateTime)
                        .last("limit 1"));
        if (duplicateTask != null) {
            return duplicateTask;
        }
        TcAutoScheduleTask task = new TcAutoScheduleTask();
        task.setTaskId(TcScheduleConstants.ROLLING_TASK_ID_PREFIX
                + IdUtil.fastSimpleUUID().toUpperCase());
        task.setTaskType(TcBackgroundTaskTypeEnum.AUTO_ROLLING.getCode());
        task.setFactoryCode(window.getFactoryCode());
        task.setScheduleDate(window.getScheduleDate());
        task.setBatchNo(resultList.get(0).getBatchNo());
        task.setTraceId(IdUtil.fastSimpleUUID().toUpperCase());
        task.setTargetShiftOrder(window.getTargetShiftOrder());
        task.setInputVersion(inputVersion);
        task.setIdempotencyKey(inputVersion);
        task.setTaskStatus(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(TcAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.tc.schedule.rolling.pending"));
        task.setRequestSnapshot(JSON.toJSONString(window));
        task.setSummaryJson(JSON.toJSONString(Collections.singletonMap("schemaVersion", 1)));
        task.setCreateBy("AUTO_ROLLING");
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.rolling.createFailed"));
        }
        return task;
    }

    /**
     * 加载当前批次结果。
     *
     * @param window 滚动窗口
     * @return 当前批次结果
     */
    private List<TcScheduleResult> loadCurrentBatchResults(TcRollingWindow window) {
        TcScheduleResult latestResult = this.scheduleResultMapper.selectOne(
                new LambdaQueryWrapper<TcScheduleResult>()
                        .eq(TcScheduleResult::getFactoryCode, window.getFactoryCode())
                        .eq(TcScheduleResult::getScheduleDate, window.getScheduleDate())
                        .orderByDesc(TcScheduleResult::getCreateTime)
                        .last("limit 1"));
        if (latestResult == null || StrUtil.isBlank(latestResult.getBatchNo())) {
            return Collections.emptyList();
        }
        return this.scheduleResultMapper.selectList(new LambdaQueryWrapper<TcScheduleResult>()
                .eq(TcScheduleResult::getFactoryCode, window.getFactoryCode())
                .eq(TcScheduleResult::getScheduleDate, window.getScheduleDate())
                .eq(TcScheduleResult::getBatchNo, latestResult.getBatchNo())
                .orderByAsc(TcScheduleResult::getId));
    }

    /**
     * 同步目标工厂最新胎侧库存，失败时阻断滚动任务创建。
     *
     * @param factoryCode 工厂编码
     */
    private void syncStock(String factoryCode) {
        AuxReqSyncDataLogs request = new AuxReqSyncDataLogs();
        request.setFactoryCode(factoryCode);
        request.setCompanyCode(factoryCode);
        AjaxResult result = FeignTokenHelper.callWithToken(() -> this.mesItfService.syncSidewallStock(request));
        if (result == null || !Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG))) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.rolling.stockSyncFailed"));
        }
    }

    /**
     * 按库存版本、结果任务版本和目标班次构造稳定指纹。
     *
     * @param window 滚动窗口
     * @param resultList 当前结果
     * @return SHA-256指纹
     */
    private String buildInputVersion(TcRollingWindow window, List<TcScheduleResult> resultList) {
        List<TcStock> stockList = this.stockMapper.selectList(new LambdaQueryWrapper<TcStock>()
                .eq(TcStock::getFactoryCode, window.getFactoryCode())
                .eq(TcStock::getStockDate, window.getScheduleDate())
                .orderByAsc(TcStock::getSidewallCode));
        List<String> partList = new ArrayList<>();
        partList.add(window.getFactoryCode());
        partList.add(DateUtil.formatDate(window.getScheduleDate()));
        partList.add(String.valueOf(window.getTargetShiftOrder()));
        CollectionUtils.emptyIfNull(stockList).stream().map(stock -> StrUtil.blankToDefault(
                stock.getSidewallCode(), "") + ":" + StrUtil.blankToDefault(stock.getDataVersion(), ""))
                .forEach(partList::add);
        resultList.stream().map(result -> result.getId() + ":"
                + (result.getTaskVersion() == null ? 0L : result.getTaskVersion()))
                .forEach(partList::add);
        return DigestUtils.sha256Hex(String.join("|", partList));
    }

    /**
     * 判断当前库存版本是否已保持到配置的稳定分钟数。
     *
     * <p>库存同步服务会跳过相同MES版本的重复快照写入，因此新版本首次落地后，
     * 后续定时检查能够以创建时间判断版本是否稳定，避免同一窗口内连续版本抖动触发滚动。</p>
     *
     * @param window 滚动窗口
     * @param triggerTime 触发时间
     * @return 已稳定或没有库存快照时返回true
     */
    private boolean isStockInputStable(TcRollingWindow window, Date triggerTime) {
        int stableMinutes = this.readIntegerParam(window.getFactoryCode(), window.getScheduleDate(),
                TcScheduleConstants.PARAM_ROLLING_STABLE_MINUTES,
                TcScheduleConstants.DEFAULT_ROLLING_STABLE_MINUTES);
        if (stableMinutes <= 0) {
            return true;
        }
        List<TcStock> stockList = this.stockMapper.selectList(new LambdaQueryWrapper<TcStock>()
                .eq(TcStock::getFactoryCode, window.getFactoryCode())
                .eq(TcStock::getStockDate, window.getScheduleDate()));
        Date newestCreateTime = CollectionUtils.emptyIfNull(stockList).stream()
                .map(TcStock::getCreateTime).filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        return newestCreateTime == null
                || !newestCreateTime.after(DateUtil.offsetMinute(triggerTime, -stableMinutes));
    }

    /**
     * 判断工厂日期自动滚动参数是否开启。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 开启返回true
     */
    private boolean isRollingEnabled(String factoryCode, Date scheduleDate) {
        String value = this.readParam(factoryCode, scheduleDate,
                TcScheduleConstants.PARAM_AUTO_ROLLING_ENABLED,
                TcScheduleConstants.DEFAULT_AUTO_ROLLING_ENABLED);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    /**
     * 读取整数参数。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 整数参数
     */
    private int readIntegerParam(String factoryCode, Date scheduleDate, String paramCode, String defaultValue) {
        try {
            return Integer.parseInt(this.readParam(factoryCode, scheduleDate, paramCode, defaultValue));
        } catch (NumberFormatException exception) {
            return Integer.parseInt(defaultValue);
        }
    }

    /**
     * 读取指定日期生效参数。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param paramCode 参数编码
     * @param defaultValue 默认值
     * @return 参数值
     */
    private String readParam(String factoryCode, Date scheduleDate, String paramCode, String defaultValue) {
        LambdaQueryWrapper<TcParams> wrapper = new LambdaQueryWrapper<TcParams>()
                .eq(TcParams::getFactoryCode, factoryCode)
                .eq(TcParams::getParamCode, paramCode)
                .eq(TcParams::getEnableStatus, "1")
                .and(condition -> condition.isNull(TcParams::getEffectiveStartTime)
                        .or().le(TcParams::getEffectiveStartTime, scheduleDate))
                .and(condition -> condition.isNull(TcParams::getEffectiveEndTime)
                        .or().ge(TcParams::getEffectiveEndTime, scheduleDate))
                .orderByDesc(TcParams::getEffectiveStartTime)
                .last("limit 1");
        TcParams params = this.paramsMapper.selectOne(wrapper);
        return params == null ? defaultValue
                : StrUtil.blankToDefault(params.getParamValue(),
                StrUtil.blankToDefault(params.getDefaultValue(), defaultValue));
    }

    /**
     * 根据六班MES日期偏移和配置时刻解析实际开始时间。
     *
     * @param config 班次配置
     * @return 实际开始时间，配置无效时返回null
     */
    private Date resolveShiftStartTime(TcShiftConfig config) {
        if (config.getScheduleDate() == null || config.getShiftOrder() == null
                || StrUtil.isBlank(config.getPlanStartTime())) {
            return null;
        }
        try {
            Date businessDate = TcShiftBusinessDateResolver.resolveMesBusinessDate(
                    config.getScheduleDate(), config.getShiftOrder());
            String clockTime = config.getPlanStartTime().trim();
            if (clockTime.length() == 5) {
                clockTime += ":00";
            }
            return DateUtil.parseDateTime(DateUtil.formatDate(businessDate) + " " + clockTime);
        } catch (RuntimeException exception) {
            log.warn("胎侧自动滚动班次时间配置无效, factoryCode={}, scheduleDate={}, shiftOrder={}",
                    config.getFactoryCode(), config.getScheduleDate(), config.getShiftOrder());
            return null;
        }
    }

    /**
     * 转换自动滚动任务响应。
     *
     * @param task 任务
     * @return 任务响应
     */
    public TcRollingTaskVo toRollingTaskVo(TcAutoScheduleTask task) {
        TcRollingTaskVo response = StrUtil.isBlank(task.getResultJson()) ? new TcRollingTaskVo()
                : JSON.parseObject(task.getResultJson(), TcRollingTaskVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setFactoryCode(task.getFactoryCode());
        response.setTargetShiftOrder(task.getTargetShiftOrder());
        response.setInputVersion(task.getInputVersion());
        if (StrUtil.isNotBlank(task.getSummaryJson())) {
            response.setSummary(JSON.parseObject(task.getSummaryJson(), LinkedHashMap.class));
        }
        return response;
    }
}
