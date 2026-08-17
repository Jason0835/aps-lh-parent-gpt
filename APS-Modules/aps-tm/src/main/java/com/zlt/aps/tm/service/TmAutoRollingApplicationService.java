package com.zlt.aps.tm.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.dto.TmRollingCheckRequestDTO;
import com.zlt.aps.tm.api.domain.dto.TmRollingRecalcRequestDTO;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.domain.entity.TmShiftStock;
import com.zlt.aps.tm.api.domain.vo.TmRollingRecalcResponseVO;
import com.zlt.aps.tm.domain.vo.TmRollingWindow;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.mapper.TmShiftStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 胎面自动滚动窗口检查、班次库存同步和滚动执行应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmAutoRollingApplicationService {

    private final TmRollingWindowService rollingWindowService;
    private final TmParamsMapper paramsMapper;
    private final TmShiftStockMapper shiftStockMapper;
    private final IMesItfService mesItfService;
    private final ITmRollingUpdateService rollingUpdateService;
    private final RedissonClient redissonClient;

    /**
     * 检查当前分钟命中的班次，并在同一分布式锁内完成库存同步和滚动。
     *
     * @param request job检查请求
     * @return 本次实际执行或复用的滚动结果
     * @throws ServiceException MES同步失败或班次库存为空时抛出
     */
    public List<TmRollingRecalcResponseVO> checkAndExecute(TmRollingCheckRequestDTO request) {
        Date triggerTime = request == null || request.getTriggerTime() == null
                ? new Date() : request.getTriggerTime();
        String factoryCode = request == null ? null : request.getFactoryCode();
        List<TmRollingRecalcResponseVO> responseList = new ArrayList<>();
        this.rollingWindowService.resolveDueWindows(factoryCode, triggerTime).stream()
                .filter(window -> this.isRollingEnabled(window.getFactoryCode()))
                .forEach(window -> {
                    TmRollingRecalcResponseVO response = this.executeWindow(window);
                    if (response != null) {
                        responseList.add(response);
                    }
                });
        return responseList;
    }

    /**
     * 在窗口锁内同步并校验班次库存，然后调用胎面滚动核心服务。
     *
     * @param window 命中的班次窗口
     * @return 滚动结果；锁已被占用时返回null
     */
    private TmRollingRecalcResponseVO executeWindow(TmRollingWindow window) {
        String lockKey = TmScheduleConstants.ROLLING_LOCK_KEY_PREFIX + window.getFactoryCode() + ":"
                + DateUtil.formatDate(window.getScheduleDate()) + ":" + window.getTargetShiftOrder();
        RLock rollingLock = this.redissonClient.getLock(lockKey);
        if (!rollingLock.tryLock()) {
            log.info("胎面自动滚动窗口已由其他实例处理，factoryCode={}，scheduleDate={}，shiftOrder={}",
                    window.getFactoryCode(), DateUtil.formatDate(window.getScheduleDate()),
                    window.getTargetShiftOrder());
            return null;
        }
        try {
            this.syncShiftStock(window);
            this.ensureShiftStockExists(window);
            TmRollingRecalcRequestDTO rollingRequest = new TmRollingRecalcRequestDTO();
            rollingRequest.setFactoryCode(window.getFactoryCode());
            rollingRequest.setScheduleDate(window.getScheduleDate());
            rollingRequest.setStockDate(window.getStockDate());
            rollingRequest.setTargetShiftOrder(window.getTargetShiftOrder());
            rollingRequest.setOperator("TM_ROLLING_JOB");
            return this.rollingUpdateService.rollingRecalcAutomatically(rollingRequest);
        } finally {
            if (rollingLock.isHeldByCurrentThread()) {
                rollingLock.unlock();
            }
        }
    }

    /**
     * 调用ITF同步胎面自动滚动班次库存。
     *
     * @param window 班次窗口
     * @throws ServiceException ITF返回失败时抛出
     */
    private void syncShiftStock(TmRollingWindow window) {
        MesShiftStockSyncRequest syncRequest = new MesShiftStockSyncRequest();
        syncRequest.setFactoryCode(window.getFactoryCode());
        syncRequest.setCompanyCode(window.getFactoryCode());
        syncRequest.setStockDate(window.getStockDate());
        syncRequest.setShiftOrder(window.getTargetShiftOrder());
        AjaxResult result = FeignTokenHelper.callWithToken(
                () -> this.mesItfService.syncTmShiftStock(syncRequest));
        if (result == null || !Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG))) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.shiftStockSyncFailed"));
        }
    }

    /**
     * 校验当前窗口至少存在一条有效胎面库存。
     *
     * @param window 班次窗口
     * @throws ServiceException MES无库存时抛出并阻断滚动
     */
    private void ensureShiftStockExists(TmRollingWindow window) {
        Long stockCount = this.shiftStockMapper.selectCount(new LambdaQueryWrapper<TmShiftStock>()
                .eq(TmShiftStock::getFactoryCode, window.getFactoryCode())
                .eq(TmShiftStock::getStockDate, DateUtil.beginOfDay(window.getStockDate()))
                .eq(TmShiftStock::getShiftOrder, window.getTargetShiftOrder()));
        if (stockCount == null || stockCount <= 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.shiftStockMissing"));
        }
    }

    /**
     * 判断工厂是否明确启用胎面自动滚动。
     *
     * @param factoryCode 工厂编码
     * @return 开启返回true
     */
    private boolean isRollingEnabled(String factoryCode) {
        TmParams params = this.paramsMapper.selectOne(new LambdaQueryWrapper<TmParams>()
                .eq(TmParams::getFactoryCode, factoryCode)
                .eq(TmParams::getParamCode, TmScheduleConstants.PARAM_ROLLING_ENABLED)
                .eq(TmParams::getEnableStatus, "1")
                .orderByDesc(TmParams::getId)
                .last("limit 1"));
        return params != null && ("1".equals(StrUtil.trim(params.getParamValue()))
                || "true".equalsIgnoreCase(StrUtil.trim(params.getParamValue())));
    }
}
