package com.zlt.aps.tc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.domain.entity.TcShiftStock;
import com.zlt.aps.tc.api.domain.vo.TcRollingCheckRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcRollingTaskVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.domain.vo.TcRollingWindow;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcShiftStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 胎侧自动滚动窗口识别、班次库存同步和任务提交应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcAutoRollingApplicationService {

    private final TcRollingWindowService rollingWindowService;
    private final TcParamsMapper paramsMapper;
    private final TcShiftStockMapper shiftStockMapper;
    private final TcAutoRollingTaskCreationService taskCreationService;
    private final TcAutoRollingAsyncExecutor asyncExecutor;
    private final IMesItfService mesItfService;
    private final RedissonClient redissonClient;

    /**
     * 检查严格命中的班前半小时窗口，同步班次库存后创建唯一异步任务。
     *
     * @param request 检查请求
     * @return 已创建或复用的任务
     * @throws ServiceException MES同步失败或班次库存为空时抛出
     */
    public List<TcRollingTaskVo> checkAndSubmit(TcRollingCheckRequestVo request) {
        Date triggerTime = request == null || request.getTriggerTime() == null
                ? new Date() : request.getTriggerTime();
        String factoryCode = request == null ? null : request.getFactoryCode();
        List<TcRollingTaskVo> taskList = new ArrayList<>();
        this.rollingWindowService.resolveDueWindows(factoryCode, triggerTime).stream()
                .filter(window -> this.isRollingEnabled(window.getFactoryCode()))
                .forEach(window -> {
                    TcAutoScheduleTask task = this.createWindowTask(window);
                    if (task == null) {
                        return;
                    }
                    taskList.add(this.toRollingTaskVo(task));
                    if (TcAutoScheduleTaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
                        this.asyncExecutor.execute(task.getTaskId());
                    }
                });
        return taskList;
    }

    /**
     * 在窗口分布式锁内同步、校验并创建胎侧自动滚动任务。
     *
     * @param window 命中的班次窗口
     * @return 创建或复用的任务；锁已被占用时返回null
     */
    private TcAutoScheduleTask createWindowTask(TcRollingWindow window) {
        String lockKey = TcScheduleConstants.ROLLING_LOCK_KEY_PREFIX + window.getFactoryCode() + ":"
                + DateUtil.formatDate(window.getScheduleDate()) + ":" + window.getTargetShiftOrder();
        RLock rollingLock = this.redissonClient.getLock(lockKey);
        if (!rollingLock.tryLock()) {
            log.info("胎侧自动滚动窗口已由其他实例处理，factoryCode={}，scheduleDate={}，shiftOrder={}",
                    window.getFactoryCode(), DateUtil.formatDate(window.getScheduleDate()),
                    window.getTargetShiftOrder());
            return null;
        }
        try {
            this.syncShiftStock(window);
            this.ensureShiftStockExists(window);
            return this.taskCreationService.createTask(window);
        } finally {
            if (rollingLock.isHeldByCurrentThread()) {
                rollingLock.unlock();
            }
        }
    }

    /**
     * 调用ITF同步胎侧自动滚动班次库存。
     *
     * @param window 班次窗口
     * @throws ServiceException ITF返回失败时抛出
     */
    private void syncShiftStock(TcRollingWindow window) {
        MesShiftStockSyncRequest syncRequest = new MesShiftStockSyncRequest();
        syncRequest.setFactoryCode(window.getFactoryCode());
        syncRequest.setCompanyCode(window.getFactoryCode());
        syncRequest.setStockDate(window.getStockDate());
        syncRequest.setShiftOrder(window.getTargetShiftOrder());
        AjaxResult result = FeignTokenHelper.callWithToken(
                () -> this.mesItfService.syncSidewallShiftStock(syncRequest));
        if (result == null || !Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG))) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.rolling.stockSyncFailed"));
        }
    }

    /**
     * 校验当前窗口至少存在一条有效胎侧库存。
     *
     * @param window 班次窗口
     * @throws ServiceException MES无库存时抛出并阻断任务创建
     */
    private void ensureShiftStockExists(TcRollingWindow window) {
        Long stockCount = this.shiftStockMapper.selectCount(new LambdaQueryWrapper<TcShiftStock>()
                .eq(TcShiftStock::getFactoryCode, window.getFactoryCode())
                .eq(TcShiftStock::getStockDate, DateUtil.beginOfDay(window.getStockDate()))
                .eq(TcShiftStock::getShiftOrder, window.getTargetShiftOrder()));
        if (stockCount == null || stockCount <= 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.rolling.shiftStockMissing"));
        }
    }

    /**
     * 判断工厂自动滚动参数是否开启。
     *
     * @param factoryCode 工厂编码
     * @return 开启返回true
     */
    private boolean isRollingEnabled(String factoryCode) {
        TcParams params = this.paramsMapper.selectOne(new LambdaQueryWrapper<TcParams>()
                .eq(TcParams::getFactoryCode, factoryCode)
                .eq(TcParams::getParamCode, TcScheduleConstants.PARAM_AUTO_ROLLING_ENABLED)
                .eq(TcParams::getEnableStatus, "1")
                .last("limit 1"));
        String value = params == null ? TcScheduleConstants.DEFAULT_AUTO_ROLLING_ENABLED
                : StrUtil.blankToDefault(params.getParamValue(),
                StrUtil.blankToDefault(params.getDefaultValue(),
                        TcScheduleConstants.DEFAULT_AUTO_ROLLING_ENABLED));
        return "1".equals(value) || "true".equalsIgnoreCase(value);
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
