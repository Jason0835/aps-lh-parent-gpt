package com.zlt.aps.tq.service;

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
import com.zlt.aps.tq.api.constant.TqScheduleConstants;
import com.zlt.aps.tq.api.domain.dto.TqRollingCheckRequestDTO;
import com.zlt.aps.tq.api.domain.dto.TqRollingRecalcRequestDTO;
import com.zlt.aps.tq.api.domain.entity.TqParams;
import com.zlt.aps.tq.api.domain.entity.TqShiftStock;
import com.zlt.aps.tq.api.domain.vo.TqRollingRecalcResponseVO;
import com.zlt.aps.tq.domain.vo.TqRollingWindow;
import com.zlt.aps.tq.mapper.TqParamsMapper;
import com.zlt.aps.tq.mapper.TqShiftStockMapper;
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
 * 胎圈自动滚动窗口检查、班次库存同步和滚动执行应用服务。
 *
 * <p>对齐胎面 TmAutoRollingApplicationService，按以下顺序执行：
 * <ol>
 *   <li>TqRollingWindowService.resolveDueWindows 识别命中的班次窗口（班前 30 分钟）</li>
 *   <li>isRollingEnabled 校验工厂启用开关（参数 TQ_ROLLING_ENABLED）</li>
 *   <li>RLock 加锁（factory+date+shiftOrder）保证同窗口互斥</li>
 *   <li>syncShiftStock 调用 IMesItfService.syncBeadShiftStock 同步班次库存</li>
 *   <li>ensureShiftStockExists 校验班次库存存在，缺失时阻断滚动</li>
 *   <li>调用 ITqRollingUpdateService.rollingRecalcAutomatically 执行核心调量</li>
 * </ol>
 * </p>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TqAutoRollingApplicationService {

    private final TqRollingWindowService rollingWindowService;
    private final TqParamsMapper tqParamsMapper;
    private final TqShiftStockMapper shiftStockMapper;
    private final IMesItfService mesItfService;
    private final ITqRollingUpdateService rollingUpdateService;
    private final RedissonClient redissonClient;

    /**
     * 检查当前分钟命中的班次，并在同一分布式锁内完成库存同步和滚动。
     *
     * @param request job检查请求
     * @return 本次实际执行或复用的滚动结果
     * @throws ServiceException MES同步失败或班次库存为空时抛出
     */
    public List<TqRollingRecalcResponseVO> checkAndExecute(TqRollingCheckRequestDTO request) {
        Date triggerTime = request == null || request.getTriggerTime() == null
                ? new Date() : request.getTriggerTime();
        String factoryCode = request == null ? null : request.getFactoryCode();
        List<TqRollingRecalcResponseVO> responseList = new ArrayList<>();
        this.rollingWindowService.resolveDueWindows(factoryCode, triggerTime).stream()
                .filter(window -> this.isRollingEnabled(window.getFactoryCode()))
                .forEach(window -> {
                    TqRollingRecalcResponseVO response = this.executeWindow(window);
                    if (response != null) {
                        responseList.add(response);
                    }
                });
        return responseList;
    }

    /**
     * 在窗口锁内同步并校验班次库存，然后调用胎圈滚动核心服务。
     *
     * @param window 命中的班次窗口
     * @return 滚动结果；锁已被占用时返回null
     */
    private TqRollingRecalcResponseVO executeWindow(TqRollingWindow window) {
        String lockKey = TqScheduleConstants.ROLLING_LOCK_KEY_PREFIX + window.getFactoryCode() + ":"
                + DateUtil.formatDate(window.getScheduleDate()) + ":" + window.getTargetShiftOrder();
        RLock rollingLock = this.redissonClient.getLock(lockKey);
        if (!rollingLock.tryLock()) {
            log.info("胎圈自动滚动窗口已由其他实例处理，factoryCode={}，scheduleDate={}，shiftOrder={}",
                    window.getFactoryCode(), DateUtil.formatDate(window.getScheduleDate()),
                    window.getTargetShiftOrder());
            return null;
        }
        try {
            this.syncShiftStock(window);
            this.ensureShiftStockExists(window);
            TqRollingRecalcRequestDTO rollingRequest = new TqRollingRecalcRequestDTO();
            rollingRequest.setFactoryCode(window.getFactoryCode());
            rollingRequest.setScheduleDate(window.getScheduleDate());
            rollingRequest.setStockDate(window.getStockDate());
            rollingRequest.setTargetShiftOrder(window.getTargetShiftOrder());
            rollingRequest.setOperator(TqScheduleConstants.ROLLING_OPERATOR_AUTO);
            return this.rollingUpdateService.rollingRecalcAutomatically(rollingRequest);
        } finally {
            if (rollingLock.isHeldByCurrentThread()) {
                rollingLock.unlock();
            }
        }
    }

    /**
     * 调用ITF同步胎圈自动滚动班次库存。
     *
     * @param window 班次窗口
     * @throws ServiceException ITF返回失败时抛出
     */
    private void syncShiftStock(TqRollingWindow window) {
        MesShiftStockSyncRequest syncRequest = new MesShiftStockSyncRequest();
        syncRequest.setFactoryCode(window.getFactoryCode());
        syncRequest.setCompanyCode(window.getFactoryCode());
        syncRequest.setStockDate(window.getStockDate());
        syncRequest.setShiftOrder(window.getTargetShiftOrder());
        AjaxResult result = FeignTokenHelper.callWithToken(
                () -> this.mesItfService.syncBeadShiftStock(syncRequest));
        if (result == null || !Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG))) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.shiftStockSyncFailed"));
        }
    }

    /**
     * 校验当前窗口至少存在一条有效胎圈库存。
     *
     * @param window 班次窗口
     * @throws ServiceException MES无库存时抛出并阻断滚动
     */
    private void ensureShiftStockExists(TqRollingWindow window) {
        Long stockCount = this.shiftStockMapper.selectCount(new LambdaQueryWrapper<TqShiftStock>()
                .eq(TqShiftStock::getFactoryCode, window.getFactoryCode())
                .eq(TqShiftStock::getStockDate, DateUtil.beginOfDay(window.getStockDate()))
                .eq(TqShiftStock::getShiftOrder, window.getTargetShiftOrder()));
        if (stockCount == null || stockCount <= 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tq.rolling.shiftStockMissing"));
        }
    }

    /**
     * 判断工厂是否明确启用胎圈自动滚动。
     *
     * @param factoryCode 工厂编码
     * @return 开启返回true
     */
    private boolean isRollingEnabled(String factoryCode) {
        TqParams params = this.tqParamsMapper.selectOne(new LambdaQueryWrapper<TqParams>()
                .eq(TqParams::getFactoryCode, factoryCode)
                .eq(TqParams::getParamCode, TqScheduleConstants.PARAM_ROLLING_ENABLED)
                .eq(TqParams::getEnableStatus, "1")
                .orderByDesc(TqParams::getId)
                .last("limit 1"));
        return params != null && ("1".equals(StrUtil.trim(params.getParamValue()))
                || "true".equalsIgnoreCase(StrUtil.trim(params.getParamValue())));
    }
}
