package com.zlt.aps.gsq.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingCheckRequestVo;
import com.zlt.aps.gsq.api.domain.vo.GsqRollingTaskVo;
import com.zlt.aps.gsq.constant.GsqScheduleConstants;
import com.zlt.aps.gsq.domain.GsqAutoScheduleTask;
import com.zlt.aps.gsq.domain.vo.GsqRollingWindow;
import com.zlt.aps.gsq.entity.GsqParams;
import com.zlt.aps.gsq.entity.GsqShiftConfig;
import com.zlt.aps.gsq.enums.GsqAutoScheduleTaskStatusEnum;
import com.zlt.aps.gsq.enums.GsqBackgroundTaskTypeEnum;
import com.zlt.aps.gsq.mapper.GsqAutoScheduleTaskMapper;
import com.zlt.aps.gsq.mapper.GsqParamsMapper;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.mapper.GsqShiftConfigMapper;
import com.zlt.aps.gsq.mapper.GsqStockMapper;
import com.zlt.aps.gsq.service.mes.GsqShiftBusinessDateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 钢丝圈自动滚动窗口识别、库存同步和任务防重应用服务。
 *
 * <p>对齐 {@code TcAutoRollingApplicationService} 的分层模式：</p>
 * <ol>
 *   <li>根据 {@link GsqShiftConfig} 与触发时间识别命中的滚动窗口；</li>
 *   <li>同步本地库存快照（暂使用本地 {@code GsqStock} 表，未来对接 MES 接口）；</li>
 *   <li>基于库存快照与排程结果构造输入指纹，避免同窗口重复派发；</li>
 *   <li>持久化 {@link GsqAutoScheduleTask} 后由 {@link GsqAutoRollingAsyncExecutor} 异步执行。</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GsqAutoRollingApplicationService {

    private final GsqShiftConfigMapper shiftConfigMapper;
    private final GsqParamsMapper paramsMapper;
    private final GsqStockMapper stockMapper;
    private final GsqScheduleResultMapper scheduleResultMapper;
    private final GsqAutoScheduleTaskMapper taskMapper;
    private final GsqBackgroundTaskService backgroundTaskService;
    private final GsqAutoRollingAsyncExecutor asyncExecutor;

    /**
     * 检查全部命中窗口，先同步库存，再为稳定输入创建唯一异步任务。
     *
     * @param request 检查请求
     * @return 已创建或复用的任务
     */
    public List<GsqRollingTaskVo> checkAndSubmit(GsqRollingCheckRequestVo request) {
        Date triggerTime = request == null || request.getTriggerTime() == null
                ? new Date() : request.getTriggerTime();
        String factoryCode = request == null ? null : request.getFactoryCode();
        List<GsqRollingWindow> windowList = this.resolveRollingWindows(factoryCode, triggerTime);
        List<GsqRollingTaskVo> taskList = new ArrayList<>();
        for (GsqRollingWindow window : windowList) {
            if (!this.isRollingEnabled(window.getFactoryCode())) {
                continue;
            }
            this.syncStock(window.getFactoryCode());
            GsqAutoScheduleTask task = this.createTask(window, triggerTime);
            if (task != null) {
                taskList.add(this.toRollingTaskVo(task));
                if (GsqAutoScheduleTaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
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
    List<GsqRollingWindow> resolveRollingWindows(String factoryCode, Date triggerTime) {
        Date startScheduleDate = DateUtil.beginOfDay(DateUtil.offsetDay(triggerTime, -1));
        Date endScheduleDate = DateUtil.endOfDay(DateUtil.offsetDay(triggerTime, 1));
        LambdaQueryWrapper<GsqShiftConfig> wrapper = new LambdaQueryWrapper<GsqShiftConfig>()
                .eq(GsqShiftConfig::getOpenFlag, "1")
                .orderByAsc(GsqShiftConfig::getFactoryCode)
                .orderByAsc(GsqShiftConfig::getShiftOrder);
        wrapper.eq(StrUtil.isNotBlank(factoryCode), GsqShiftConfig::getFactoryCode, factoryCode);
        List<GsqShiftConfig> configList = this.shiftConfigMapper.selectList(wrapper);
        Map<String, GsqRollingWindow> closestWindowMap = new LinkedHashMap<>();
        CollectionUtils.emptyIfNull(configList).stream()
                .filter(config -> config.getShiftOrder() != null && config.getShiftOrder() >= 1
                        && config.getShiftOrder() <= GsqScheduleConstants.GSQ_MAX_SHIFT_ORDER)
                .forEach(config -> this.listScheduleDates(startScheduleDate, endScheduleDate).forEach(scheduleDate -> {
                    int earlyMinutes = this.readIntegerParam(config.getFactoryCode(),
                            GsqParams.PARAM_CODE_ROLLING_EARLY_MINUTES,
                            GsqParams.DEFAULT_ROLLING_EARLY_MINUTES);
                    int lateMinutes = this.readIntegerParam(config.getFactoryCode(),
                            GsqParams.PARAM_CODE_ROLLING_LATE_MINUTES,
                            GsqParams.DEFAULT_ROLLING_LATE_MINUTES);
                    Date shiftStartTime = this.resolveShiftStartTime(config, scheduleDate);
                    if (shiftStartTime == null || triggerTime.before(DateUtil.offsetMinute(shiftStartTime, -earlyMinutes))
                            || triggerTime.after(DateUtil.offsetMinute(shiftStartTime, lateMinutes))) {
                        return;
                    }
                    GsqRollingWindow candidate = new GsqRollingWindow();
                    candidate.setFactoryCode(config.getFactoryCode());
                    candidate.setScheduleDate(scheduleDate);
                    candidate.setTargetShiftOrder(config.getShiftOrder());
                    candidate.setShiftStartTime(shiftStartTime);
                    String mapKey = config.getFactoryCode() + "|" + DateUtil.formatDate(scheduleDate);
                    GsqRollingWindow existing = closestWindowMap.get(mapKey);
                    if (existing == null || Math.abs(shiftStartTime.getTime() - triggerTime.getTime())
                            < Math.abs(existing.getShiftStartTime().getTime() - triggerTime.getTime())) {
                        closestWindowMap.put(mapKey, candidate);
                    }
                }));
        return new ArrayList<>(closestWindowMap.values());
    }

    /**
     * 在事务内按稳定输入指纹创建或复用自动滚动任务。
     *
     * @param window      滚动窗口
     * @param triggerTime 触发时间
     * @return 任务；无当前排程结果时返回null
     */
    @Transactional(rollbackFor = Exception.class)
    public GsqAutoScheduleTask createTask(GsqRollingWindow window, Date triggerTime) {
        GsqAutoScheduleTask activeTask = this.backgroundTaskService.findActive(
                window.getFactoryCode(), window.getScheduleDate());
        if (activeTask != null) {
            return GsqBackgroundTaskTypeEnum.AUTO_ROLLING.getCode().equals(activeTask.getTaskType())
                    ? activeTask : null;
        }
        List<GsqScheduleResult> resultList = this.loadCurrentBatchResults(window);
        if (resultList.isEmpty()) {
            return null;
        }
        if (!this.isStockInputStable(window, triggerTime)) {
            log.info("钢丝圈自动滚动输入尚未达到稳定时间，factoryCode={}, scheduleDate={}, shiftOrder={}",
                    window.getFactoryCode(), DateUtil.formatDate(window.getScheduleDate()),
                    window.getTargetShiftOrder());
            return null;
        }
        String inputVersion = this.buildInputVersion(window, resultList);
        GsqAutoScheduleTask duplicateTask = this.taskMapper.selectOne(
                new LambdaQueryWrapper<GsqAutoScheduleTask>()
                        .eq(GsqAutoScheduleTask::getFactoryCode, window.getFactoryCode())
                        .eq(GsqAutoScheduleTask::getScheduleDate, window.getScheduleDate())
                        .eq(GsqAutoScheduleTask::getTaskType, GsqBackgroundTaskTypeEnum.AUTO_ROLLING.getCode())
                        .eq(GsqAutoScheduleTask::getTargetShiftOrder, window.getTargetShiftOrder())
                        .eq(GsqAutoScheduleTask::getInputVersion, inputVersion)
                        .in(GsqAutoScheduleTask::getTaskStatus, Arrays.asList(
                                GsqAutoScheduleTaskStatusEnum.PENDING.getCode(),
                                GsqAutoScheduleTaskStatusEnum.RUNNING.getCode(),
                                GsqAutoScheduleTaskStatusEnum.SUCCESS.getCode()))
                        .orderByDesc(GsqAutoScheduleTask::getCreateTime)
                        .last("limit 1"));
        if (duplicateTask != null) {
            return duplicateTask;
        }
        GsqAutoScheduleTask task = new GsqAutoScheduleTask();
        task.setTaskId(GsqScheduleConstants.ROLLING_TASK_ID_PREFIX
                + IdUtil.fastSimpleUUID().toUpperCase());
        task.setTaskType(GsqBackgroundTaskTypeEnum.AUTO_ROLLING.getCode());
        task.setFactoryCode(window.getFactoryCode());
        task.setScheduleDate(window.getScheduleDate());
        task.setBatchNo(resultList.get(0).getBatchNo());
        task.setTraceId(IdUtil.fastSimpleUUID().toUpperCase());
        task.setTargetShiftOrder(window.getTargetShiftOrder());
        task.setInputVersion(inputVersion);
        task.setIdempotencyKey(inputVersion);
        task.setTaskStatus(GsqAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setProgress(0);
        task.setCurrentStage(GsqAutoScheduleTaskStatusEnum.PENDING.getCode());
        task.setCurrentStageName(I18nUtil.getMessage("ui.gsq.schedule.rolling.pending"));
        task.setRequestSnapshot(JSON.toJSONString(window));
        task.setSummaryJson(JSON.toJSONString(Collections.singletonMap("schemaVersion", 1)));
        task.setCreateBy(GsqScheduleConstants.AUTO_ROLLING_OPERATOR);
        if (this.taskMapper.insert(task) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.gsq.schedule.rolling.createFailed"));
        }
        return task;
    }

    /**
     * 加载当前批次结果。
     *
     * @param window 滚动窗口
     * @return 当前批次结果
     */
    private List<GsqScheduleResult> loadCurrentBatchResults(GsqRollingWindow window) {
        GsqScheduleResult latestResult = this.scheduleResultMapper.selectOne(
                new LambdaQueryWrapper<GsqScheduleResult>()
                        .eq(GsqScheduleResult::getFactoryCode, window.getFactoryCode())
                        .eq(GsqScheduleResult::getScheduleDate, window.getScheduleDate())
                        .orderByDesc(GsqScheduleResult::getCreateTime)
                        .last("limit 1"));
        if (latestResult == null || StrUtil.isBlank(latestResult.getBatchNo())) {
            return Collections.emptyList();
        }
        return this.scheduleResultMapper.selectList(new LambdaQueryWrapper<GsqScheduleResult>()
                .eq(GsqScheduleResult::getFactoryCode, window.getFactoryCode())
                .eq(GsqScheduleResult::getScheduleDate, window.getScheduleDate())
                .eq(GsqScheduleResult::getBatchNo, latestResult.getBatchNo())
                .orderByAsc(GsqScheduleResult::getId));
    }

    /**
     * 同步目标工厂最新钢丝圈库存。
     *
     * <p>GSQ 暂未提供 MES 库存同步接口，此处调用本地库存校验入口；
     * 后续若提供 MES 接口，应替换为 FeignTokenHelper.runWithToken 调用。</p>
     *
     * @param factoryCode 工厂编码
     */
    private void syncStock(String factoryCode) {
        // 当前以"本地库存存在"作为前置条件，未来对接 MES 同步接口后替换为远程调用
        List<GsqStock> stockList = this.stockMapper.selectList(new LambdaQueryWrapper<GsqStock>()
                .eq(GsqStock::getStockDate, DateUtil.beginOfDay(new Date())));
        if (stockList.isEmpty()) {
            log.warn("钢丝圈自动滚动库存同步：当日无本地库存数据，factoryCode={}", factoryCode);
        }
    }

    /**
     * 按库存数量、结果创建时间和目标班次构造稳定指纹。
     *
     * @param window      滚动窗口
     * @param resultList  当前结果
     * @return SHA-256指纹
     */
    private String buildInputVersion(GsqRollingWindow window, List<GsqScheduleResult> resultList) {
        List<GsqStock> stockList = this.stockMapper.selectList(new LambdaQueryWrapper<GsqStock>()
                .eq(GsqStock::getStockDate, window.getScheduleDate())
                .orderByAsc(GsqStock::getSteelRingCode));
        List<String> partList = new ArrayList<>();
        partList.add(window.getFactoryCode());
        partList.add(DateUtil.formatDate(window.getScheduleDate()));
        partList.add(String.valueOf(window.getTargetShiftOrder()));
        CollectionUtils.emptyIfNull(stockList).stream().map(stock -> StrUtil.blankToDefault(
                stock.getSteelRingCode(), "") + ":" + Objects.toString(stock.getStockNum(), "")
                + ":" + Objects.toString(stock.getBadNum(), "")
                + ":" + Objects.toString(stock.getModifyNum(), ""))
                .forEach(partList::add);
        resultList.stream().map(result -> result.getId() + ":"
                + DateUtil.formatDateTime(result.getUpdateTime()))
                .forEach(partList::add);
        return DigestUtils.sha256Hex(String.join("|", partList));
    }

    /**
     * 判断当前库存版本是否已保持到配置的稳定分钟数。
     *
     * @param window      滚动窗口
     * @param triggerTime 触发时间
     * @return 已稳定或没有库存快照时返回true
     */
    private boolean isStockInputStable(GsqRollingWindow window, Date triggerTime) {
        int stableMinutes = this.readIntegerParam(window.getFactoryCode(),
                GsqParams.PARAM_CODE_ROLLING_STABLE_MINUTES,
                GsqParams.DEFAULT_ROLLING_STABLE_MINUTES);
        if (stableMinutes <= 0) {
            return true;
        }
        List<GsqStock> stockList = this.stockMapper.selectList(new LambdaQueryWrapper<GsqStock>()
                .eq(GsqStock::getStockDate, window.getScheduleDate()));
        Date newestCreateTime = CollectionUtils.emptyIfNull(stockList).stream()
                .map(GsqStock::getCreateTime).filter(Objects::nonNull)
                .max(Date::compareTo).orElse(null);
        return newestCreateTime == null
                || !newestCreateTime.after(DateUtil.offsetMinute(triggerTime, -stableMinutes));
    }

    /**
     * 判断工厂自动滚动参数是否开启。
     *
     * @param factoryCode 工厂编码
     * @return 开启返回true
     */
    private boolean isRollingEnabled(String factoryCode) {
        String value = this.readParam(factoryCode, GsqParams.PARAM_CODE_AUTO_ROLLING_ENABLED,
                GsqParams.DEFAULT_AUTO_ROLLING_ENABLED);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    /**
     * 读取整数参数。
     *
     * @param factoryCode  工厂编码
     * @param paramCode    参数编码
     * @param defaultValue 默认值
     * @return 整数参数
     */
    private int readIntegerParam(String factoryCode, String paramCode, String defaultValue) {
        try {
            return Integer.parseInt(this.readParam(factoryCode, paramCode, defaultValue));
        } catch (NumberFormatException exception) {
            return Integer.parseInt(defaultValue);
        }
    }

    /**
     * 读取工厂启用参数。
     *
     * @param factoryCode  工厂编码
     * @param paramCode    参数编码
     * @param defaultValue 默认值
     * @return 参数值
     */
    private String readParam(String factoryCode, String paramCode, String defaultValue) {
        LambdaQueryWrapper<GsqParams> wrapper = new LambdaQueryWrapper<GsqParams>()
                .eq(GsqParams::getFactoryCode, factoryCode)
                .eq(GsqParams::getParamCode, paramCode)
                .eq(GsqParams::getEnableStatus, "1")
                .last("limit 1");
        GsqParams params = this.paramsMapper.selectOne(wrapper);
        return params == null ? defaultValue
                : StrUtil.blankToDefault(params.getParamValue(),
                StrUtil.blankToDefault(params.getDefaultValue(), defaultValue));
    }

    /**
     * 根据六班MES日期偏移和配置时刻解析实际开始时间。
     *
     * @param config       班次配置
     * @param scheduleDate 排程日期
     * @return 实际开始时间，配置无效时返回null
     */
    private Date resolveShiftStartTime(GsqShiftConfig config, Date scheduleDate) {
        if (scheduleDate == null || config.getShiftOrder() == null
                || StrUtil.isBlank(config.getPlanStartTime())) {
            return null;
        }
        try {
            Date businessDate = GsqShiftBusinessDateResolver.resolveMesBusinessDate(
                    scheduleDate, config.getShiftOrder());
            String clockTime = config.getPlanStartTime().trim();
            if (clockTime.length() == 5) {
                clockTime += ":00";
            }
            return DateUtil.parseDateTime(DateUtil.formatDate(businessDate) + " " + clockTime);
        } catch (RuntimeException exception) {
            log.warn("钢丝圈自动滚动班次时间配置无效, factoryCode={}, scheduleDate={}, shiftOrder={}",
                    config.getFactoryCode(), scheduleDate, config.getShiftOrder());
            return null;
        }
    }

    /**
     * 构建自动滚动候选排程日期。
     *
     * @param startScheduleDate 起始日期
     * @param endScheduleDate   结束日期
     * @return 按日期升序排列的候选日期
     */
    private List<Date> listScheduleDates(Date startScheduleDate, Date endScheduleDate) {
        List<Date> scheduleDateList = new ArrayList<>();
        Date currentDate = DateUtil.beginOfDay(startScheduleDate);
        Date lastDate = DateUtil.beginOfDay(endScheduleDate);
        while (!currentDate.after(lastDate)) {
            scheduleDateList.add(currentDate);
            currentDate = DateUtil.offsetDay(currentDate, 1);
        }
        return scheduleDateList;
    }

    /**
     * 转换自动滚动任务响应。
     *
     * @param task 任务
     * @return 任务响应
     */
    public GsqRollingTaskVo toRollingTaskVo(GsqAutoScheduleTask task) {
        GsqRollingTaskVo response = StrUtil.isBlank(task.getResultJson()) ? new GsqRollingTaskVo()
                : JSON.parseObject(task.getResultJson(), GsqRollingTaskVo.class);
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getTaskStatus());
        response.setProgress(task.getProgress());
        response.setCurrentStage(task.getCurrentStage());
        response.setFactoryCode(task.getFactoryCode());
        response.setTargetShiftOrder(task.getTargetShiftOrder());
        response.setBatchNo(task.getBatchNo());
        response.setInputVersion(task.getInputVersion());
        if (StrUtil.isNotBlank(task.getSummaryJson())) {
            response.setSummary(JSON.parseObject(task.getSummaryJson(), LinkedHashMap.class));
        }
        return response;
    }

    /**
     * 提供给 GsqAutoRollingAsyncExecutorImpl 使用的工具方法，避免循环依赖。
     *
     * <p>同步本地库存按当日日期做幂等校验，对外暴露以便异步执行器在派发前再次确认。</p>
     *
     * @param factoryCode 工厂编码
     * @return {@link AjaxResult} 同步结果，成功时 code = {@link HttpStatus#SUCCESS}
     */
    public AjaxResult syncStockPublic(String factoryCode) {
        try {
            this.syncStock(factoryCode);
            return AjaxResult.success();
        } catch (Exception exception) {
            log.error("钢丝圈自动滚动库存同步失败, factoryCode={}", factoryCode, exception);
            return AjaxResult.error(I18nUtil.getMessage("ui.gsq.schedule.rolling.stockSyncFailed"));
        }
    }
}
