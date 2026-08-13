package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.api.service.ICd90MesSyncRemoteService;
import com.zlt.aps.cd90.api.service.ICd90StockRemoteService;
import com.zlt.aps.cd90.api.service.ICd90StorageLaneLimitRemoteService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.mapper.Cd90MesItfMapper;
import com.zlt.aps.itf.mes.mapper.Cd90ShiftQueryMapper;
import com.zlt.aps.itf.mes.service.ICd90MesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.mp.api.domain.entity.Cd90MesStock;
import com.zlt.aps.utils.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 直裁MES接口服务实现。
 */
@Slf4j
@Service("cd90MesItfService")
@RequiredArgsConstructor
public class Cd90MesItfServiceImpl implements ICd90MesItfService {

    private final Cd90MesItfMapper cd90MesItfMapper;
    private final Cd90ShiftQueryMapper cd90ShiftQueryMapper;
    private final ICd90MesSyncRemoteService cd90MesSyncRemoteService;
    private final ICd90StockRemoteService cd90StockRemoteService;
    private final ICd90StorageLaneLimitRemoteService cd90StorageLaneLimitRemoteService;

    /**
     * 同步直裁库存。
     * 从MES中间表读取库存，按库存日期分别覆盖APS中同工厂、MES来源和班次的数据。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @Override
    public AjaxResult syncStock(AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        syncDataLogs.setFactoryCode(factoryCode);

        List<Cd90MesStock> syncList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            syncList = this.cd90MesItfMapper.selectStockList(syncDataLogs);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("直裁库存同步：MES中间表查询结果为空，factoryCode={}", factoryCode);
            return AjaxResult.success(I18nUtil.getMessage("ui.cd90.stock.syncNoData"));
        }

        Map<String, Cd90MesStock> stockMap = syncList.stream()
                .collect(Collectors.toMap(
                        item -> DateUtil.formatDate(item.getStockDate()) + "|" + item.getMaterialCode(),
                        Function.identity(),
                        (first, second) -> {
                            BigDecimal merged = (first.getAvailableStock() == null
                                    ? BigDecimal.ZERO : first.getAvailableStock())
                                    .add(second.getAvailableStock() == null
                                            ? BigDecimal.ZERO : second.getAvailableStock());
                            first.setAvailableStock(merged);
                            return first;
                        }
                ));
        syncList = new ArrayList<>(stockMap.values());

        String shiftCode = this.resolveShiftCode(syncDataLogs, factoryCode);
        if (StringUtils.isBlank(shiftCode)) {
            log.error("直裁库存同步：无法推断当前班次，factoryCode={}，请检查t_cd90_shift_config启用配置", factoryCode);
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.stock.shiftNotFound"));
        }

        Date now = DateUtils.getNowDate();
        List<Cd90Stock> insertList = syncList.stream().map(source -> {
            Cd90Stock stock = new Cd90Stock();
            stock.setFactoryCode(factoryCode);
            stock.setStockDate(source.getStockDate());
            stock.setShiftCode(shiftCode);
            stock.setSnapshotTime(now);
            stock.setMaterialCode(source.getMaterialCode());
            stock.setStockNum(source.getAvailableStock() != null ? source.getAvailableStock().doubleValue() : 0d);
            stock.setDataSource(ApsConstant.DATA_SOURCE_MES);
            stock.setCreateBy("MES");
            stock.setUpdateBy("MES");
            stock.setCreateTime(now);
            stock.setUpdateTime(now);
            return stock;
        }).collect(Collectors.toList());

        Map<String, List<Cd90Stock>> insertListByStockDate = insertList.stream()
                .collect(Collectors.groupingBy(
                        stock -> DateUtil.formatDate(stock.getStockDate()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        try {
            log.info("直裁库存同步：开始同步，factoryCode={}，shiftCode={}，库存日期数={}，待插入总数={}",
                    factoryCode, shiftCode, insertListByStockDate.size(), insertList.size());
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() -> {
                for (Map.Entry<String, List<Cd90Stock>> entry : insertListByStockDate.entrySet()) {
                    String stockDateText = entry.getKey();
                    List<Cd90Stock> stockDateList = entry.getValue();
                    log.info("直裁库存同步：同步库存日期，factoryCode={}，shiftCode={}，stockDate={}，插入数量={}",
                            factoryCode, shiftCode, stockDateText, stockDateList.size());
                    AjaxResult stockDateResult = this.cd90StockRemoteService.logicDeleteAndSaveCd90StockByDataSource(
                            factoryCode, ApsConstant.DATA_SOURCE_MES, stockDateText, shiftCode, "MES", stockDateList);
                    if (stockDateResult == null
                            || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, stockDateResult.get(AjaxResult.CODE_TAG))) {
                        return stockDateResult;
                    }
                }
                return AjaxResult.success();
            });
            if (saveResult == null
                    || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, saveResult.get(AjaxResult.CODE_TAG))) {
                Object resultMessage = saveResult == null ? "" : saveResult.get(AjaxResult.MSG_TAG);
                Object resultCode = saveResult == null ? null : saveResult.get(AjaxResult.CODE_TAG);
                log.error("直裁库存同步：同步失败，factoryCode={}，shiftCode={}，返回消息={}，"
                                + "code原始值={}，code类型={}，saveResult完整内容={}",
                        factoryCode, shiftCode, resultMessage, resultCode,
                        resultCode == null ? "null" : resultCode.getClass().getName(),
                        saveResult == null ? "null" : saveResult);
                return AjaxResult.error(MessageFormat.format(
                        I18nUtil.getMessage("ui.cd90.stock.syncFailed"), resultMessage));
            }
        } catch (Exception exception) {
            log.error("直裁库存同步：Feign调用异常，factoryCode={}，shiftCode={}，待插入数量={}",
                    factoryCode, shiftCode, insertList.size(), exception);
            return AjaxResult.error(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.stock.syncFailed"), exception.getMessage()));
        }
        log.info("直裁库存同步：同步完成，factoryCode={}，shiftCode={}，库存日期数={}，插入总数={}",
                factoryCode, shiftCode, insertListByStockDate.size(), insertList.size());
        return AjaxResult.success();
    }

    /**
     * 同步直裁自动滚动目标班次库存。
     * MES指定物理日的完整库存作为滚动基线，空结果会清空目标范围，防止继续使用旧库存。
     *
     * @param request 目标库存日期、班次和开始时间
     * @return 同步结果
     */
    @Override
    public AjaxResult syncShiftStock(MesShiftStockSyncRequest request) {
        if (request == null || request.getStockDate() == null
                || StringUtils.isBlank(request.getShiftCode()) || request.getShiftStartTime() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.shiftStock.syncArgumentsInvalid"));
        }
        request.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        request.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
        request.setShiftCode(request.getShiftCode().trim());

        List<Cd90MesStock> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.cd90MesItfMapper.selectShiftStockList(request);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }

        List<Cd90MesStock> safeSourceList = sourceList == null ? Collections.emptyList() : sourceList;
        Set<String> materialCodes = new HashSet<>();
        Cd90MesStock invalidSource = safeSourceList.stream()
                .filter(source -> source == null || StringUtils.isBlank(source.getMaterialCode())
                        || source.getAvailableStock() == null
                        || !materialCodes.add(source.getMaterialCode().trim()))
                .findFirst()
                .orElse(null);
        if (invalidSource != null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd90.shiftStock.syncSourceInvalid"));
        }

        Date now = DateUtils.getNowDate();
        List<Cd90ShiftStock> stockList = safeSourceList.stream().map(source -> {
            Cd90ShiftStock target = new Cd90ShiftStock();
            target.setFactoryCode(request.getFactoryCode());
            target.setStockDate(request.getStockDate());
            target.setShiftCode(request.getShiftCode());
            target.setShiftStartTime(request.getShiftStartTime());
            target.setMaterialCode(source.getMaterialCode().trim());
            target.setStockNum(source.getAvailableStock().doubleValue());
            target.setModifyNum(0D);
            target.setBadNum(0D);
            target.setSnapshotTime(now);
            target.setCreateBy("MES");
            target.setUpdateBy("MES");
            target.setCreateTime(now);
            target.setUpdateTime(now);
            target.setIsDelete(0);
            return target;
        }).collect(Collectors.toList());

        try {
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    this.cd90MesSyncRemoteService.replaceShiftStock(request.getFactoryCode(),
                            DateUtil.formatDate(request.getStockDate()), request.getShiftCode(),
                            DateUtil.formatDateTime(request.getShiftStartTime()), "MES", stockList));
            if (saveResult == null
                    || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, saveResult.get(AjaxResult.CODE_TAG))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.shiftStockRemoteFailed"));
            }
        } catch (Exception exception) {
            log.error("直裁班次库存同步调用异常：factoryCode={}，shiftCode={}，shiftStartTime={}",
                    request.getFactoryCode(), request.getShiftCode(), request.getShiftStartTime(), exception);
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.shiftStockRemoteFailed"));
        }
        log.info("直裁班次库存同步完成：factoryCode={}，shiftCode={}，shiftStartTime={}，数量={}",
                request.getFactoryCode(), request.getShiftCode(), request.getShiftStartTime(), stockList.size());
        return AjaxResult.success(stockList.size());
    }

    /**
     * 同步直裁库排状态。
     * MES每个版本是完整快照，APS按工厂、日期和班次逐组全量覆盖。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @Override
    public AjaxResult syncStorageLaneLimit(AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = StringUtils.isBlank(syncDataLogs.getFactoryCode())
                ? FactoryConstant.DEFAULT_FACTORY_CODE : syncDataLogs.getFactoryCode();
        syncDataLogs.setFactoryCode(factoryCode);
        String targetLaneDate = syncDataLogs.getQueryParams() == null ? null
                : Objects.toString(syncDataLogs.getQueryParams().get("laneDate"), null);
        String targetShiftCode = syncDataLogs.getQueryParams() == null ? null
                : Objects.toString(syncDataLogs.getQueryParams().get("shiftCode"), null);

        List<Cd90StorageLaneLimit> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.cd90MesItfMapper.selectStorageLaneLimitList(syncDataLogs);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        if (CollectionUtils.isEmpty(sourceList)) {
            log.warn("直裁库排同步：MES中间表查询结果为空，factoryCode={}", factoryCode);
            if (StringUtils.isNotBlank(targetLaneDate) && StringUtils.isNotBlank(targetShiftCode)) {
                try {
                    AjaxResult clearResult = FeignTokenHelper.callWithToken(() ->
                            this.cd90StorageLaneLimitRemoteService.logicDeleteAndSaveMesBatch(
                                    factoryCode, targetLaneDate, targetShiftCode, "MES",
                                    Collections.emptyList()));
                    if (clearResult == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                            clearResult.get(AjaxResult.CODE_TAG))) {
                        Object resultMessage = clearResult == null ? "" : clearResult.get(AjaxResult.MSG_TAG);
                        return AjaxResult.error(MessageFormat.format(
                                I18nUtil.getMessage("ui.cd90.storageLaneLimit.syncFailed"), resultMessage));
                    }
                } catch (Exception exception) {
                    log.error("直裁库排空快照清理失败：factoryCode={}，laneDate={}，shiftCode={}",
                            factoryCode, targetLaneDate, targetShiftCode, exception);
                    return AjaxResult.error(MessageFormat.format(
                            I18nUtil.getMessage("ui.cd90.storageLaneLimit.syncFailed"),
                            exception.getMessage()));
                }
            }
            return AjaxResult.success(I18nUtil.getMessage("ui.cd90.storageLaneLimit.syncNoData"));
        }

        Cd90StorageLaneLimit invalidSource = sourceList.stream()
                .filter(source -> source.getLaneDate() == null
                        || StringUtils.isBlank(source.getShiftCode())
                        || StringUtils.isBlank(source.getStorageLaneCode())
                        || source.getCarNum() == null
                        || source.getMaxCarNum() == null
                        || source.getMaxCarNum() <= 0
                        || source.getCarNum() < 0
                        || source.getCarNum() > source.getMaxCarNum())
                .findFirst()
                .orElse(null);
        if (invalidSource != null) {
            String laneCode = StringUtils.defaultString(invalidSource.getStorageLaneCode());
            log.error("直裁库排同步：MES数据非法，factoryCode={}，storageLaneCode={}", factoryCode, laneCode);
            return AjaxResult.error(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.storageLaneLimit.syncInvalid"), laneCode));
        }

        Date now = DateUtils.getNowDate();
        List<Cd90StorageLaneLimit> insertList = sourceList.stream().map(source -> {
            Cd90StorageLaneLimit target = new Cd90StorageLaneLimit();
            target.setFactoryCode(factoryCode);
            target.setLaneDate(source.getLaneDate());
            target.setShiftCode(source.getShiftCode());
            target.setStorageLaneCode(source.getStorageLaneCode());
            target.setMaterialCode(source.getMaterialCode());
            target.setCarNum(source.getCarNum());
            target.setMaxCarNum(source.getMaxCarNum());
            target.setAvailableCarNum(source.getAvailableCarNum() == null
                    ? source.getMaxCarNum() - source.getCarNum() : source.getAvailableCarNum());
            target.setDataSource("MES");
            target.setMesSyncTime(now);
            target.setCreateBy("MES");
            target.setUpdateBy("MES");
            target.setCreateTime(now);
            target.setUpdateTime(now);
            target.setIsDelete(0);
            return target;
        }).collect(Collectors.toList());

        Map<String, List<Cd90StorageLaneLimit>> listByScope = insertList.stream()
                .collect(Collectors.groupingBy(
                        item -> DateUtil.formatDate(item.getLaneDate()) + "|" + item.getShiftCode(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        try {
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() -> {
                for (List<Cd90StorageLaneLimit> scopeList : listByScope.values()) {
                    Cd90StorageLaneLimit scope = scopeList.get(0);
                    String laneDateText = DateUtil.formatDate(scope.getLaneDate());
                    AjaxResult scopeResult = this.cd90StorageLaneLimitRemoteService.logicDeleteAndSaveMesBatch(
                            factoryCode, laneDateText, scope.getShiftCode(), "MES", scopeList);
                    if (scopeResult == null
                            || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, scopeResult.get(AjaxResult.CODE_TAG))) {
                        return scopeResult;
                    }
                }
                return AjaxResult.success();
            });
            if (saveResult == null
                    || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, saveResult.get(AjaxResult.CODE_TAG))) {
                Object resultMessage = saveResult == null ? "" : saveResult.get(AjaxResult.MSG_TAG);
                return AjaxResult.error(MessageFormat.format(
                        I18nUtil.getMessage("ui.cd90.storageLaneLimit.syncFailed"), resultMessage));
            }
        } catch (Exception exception) {
            log.error("直裁库排同步调用异常：factoryCode={}，数量={}", factoryCode, insertList.size(), exception);
            return AjaxResult.error(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.storageLaneLimit.syncFailed"), exception.getMessage()));
        }
        log.info("直裁库排同步完成：factoryCode={}，快照范围数={}，数量={}",
                factoryCode, listByScope.size(), insertList.size());
        return AjaxResult.success();
    }

    /**
     * 推断直裁当前班次。
     *
     * @param syncDataLogs 同步参数
     * @param factoryCode 工厂编码
     * @return 班次编码，推断不出返回空
     */
    private String resolveShiftCode(AuxReqSyncDataLogs syncDataLogs, String factoryCode) {
        Map<String, Object> queryParams = syncDataLogs.getQueryParams();
        if (queryParams != null && queryParams.get("shiftCode") != null
                && StringUtils.isNotBlank(String.valueOf(queryParams.get("shiftCode")))) {
            return String.valueOf(queryParams.get("shiftCode"));
        }

        List<Cd90ShiftConfig> activeShifts;
        DynamicDataSourceContextHolder.push(DataSource.APS);
        try {
            activeShifts = this.cd90ShiftQueryMapper.listActiveShiftConfigs(factoryCode);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        if (CollectionUtils.isEmpty(activeShifts)) {
            return null;
        }
        String nowTime = DateUtil.format(new Date(), "HH:mm:ss");
        return activeShifts.stream()
                .filter(config -> this.matchShiftByTime(config, nowTime))
                .map(Cd90ShiftConfig::getShiftCode)
                .findFirst()
                .orElse(null);
    }

    /**
     * 按当前时间匹配班次配置。
     *
     * @param config 班次配置
     * @param nowTime 当前时间，格式HH:mm:ss
     * @return 是否命中
     */
    private boolean matchShiftByTime(Cd90ShiftConfig config, String nowTime) {
        String startTime = config.getStartTime();
        String endTime = config.getEndTime();
        if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            return false;
        }
        boolean crossDay = Objects.equals(config.getIsCrossDay(), 1);
        if (crossDay) {
            return nowTime.compareTo(startTime) >= 0 || nowTime.compareTo(endTime) < 0;
        }
        return nowTime.compareTo(startTime) >= 0 && nowTime.compareTo(endTime) < 0;
    }

    /**
     * 同步直裁每日三班完成量。
     * 从MES中间表读取业务键最新版本，替换APS当日回报并按班次配置回写排程结果。
     *
     * @param syncDataLogs 同步参数
     * @return 同步结果
     */
    @Override
    public AjaxResult syncClassShiftFinishQty(AuxReqSyncDataLogs syncDataLogs) {
        if (StringUtils.isBlank(syncDataLogs.getFactoryCode())) {
            syncDataLogs.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        List<Cd90ScheFinishQty> syncList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            syncList = this.cd90MesItfMapper.selectClassShiftFinishQtyList(syncDataLogs);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }

        if (CollectionUtils.isEmpty(syncList)) {
            log.warn("直裁每日完成量同步：MES中间表查询结果为空，factoryCode={}", syncDataLogs.getFactoryCode());
            return AjaxResult.success(I18nUtil.getMessage("ui.cd90.scheFinishQty.syncNoData"));
        }

        Date now = DateUtils.getNowDate();
        List<Cd90ScheFinishQty> insertList = syncList.stream().map(source -> {
            Cd90ScheFinishQty target = new Cd90ScheFinishQty();
            BeanUtils.copyProperties(source, target);
            target.setCreateBy("MES");
            target.setUpdateBy("MES");
            target.setCreateTime(now);
            target.setUpdateTime(now);
            target.setIsDelete(0);
            return target;
        }).collect(Collectors.toList());

        String factoryCode = syncDataLogs.getFactoryCode();
        Date scheduleDate = insertList.stream().map(Cd90ScheFinishQty::getScheduleDate)
                .filter(Objects::nonNull).findFirst().orElse(now);
        String scheduleDateText = DateUtil.formatDate(scheduleDate);
        try {
            AjaxResult saveResult = FeignTokenHelper.callWithToken(() ->
                    this.cd90MesSyncRemoteService.logicDeleteAndSaveScheFinishQty(factoryCode,
                            scheduleDateText, "MES", insertList));
            if (saveResult == null
                    || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, saveResult.get(AjaxResult.CODE_TAG))) {
                String message = MessageFormat.format(
                        I18nUtil.getMessage("ui.cd90.scheFinishQty.syncFailed"),
                        saveResult == null ? "" : saveResult.get(AjaxResult.MSG_TAG));
                log.error("直裁每日完成量同步保存失败：factoryCode={}，message={}", factoryCode, message);
                return AjaxResult.error(message);
            }

            AjaxResult writeBackResult = FeignTokenHelper.callWithToken(() ->
                    this.cd90MesSyncRemoteService.writeBackScheduleResultFinishQty(insertList));
            if (writeBackResult == null
                    || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS, writeBackResult.get(AjaxResult.CODE_TAG))) {
                String message = MessageFormat.format(
                        I18nUtil.getMessage("ui.cd90.scheFinishQty.writeBackFailed"),
                        writeBackResult == null ? "" : writeBackResult.get(AjaxResult.MSG_TAG));
                log.error("直裁每日完成量回写失败：factoryCode={}，message={}", factoryCode, message);
                return AjaxResult.error(message);
            }
        } catch (Exception exception) {
            String message = MessageFormat.format(I18nUtil.getMessage("ui.cd90.scheFinishQty.syncFailed"),
                    exception.getMessage());
            log.error("直裁每日完成量同步调用异常：factoryCode={}，数量={}",
                    factoryCode, insertList.size(), exception);
            return AjaxResult.error(message);
        }
        log.info("直裁每日完成量同步完成：factoryCode={}，scheduleDate={}，数量={}",
                factoryCode, scheduleDateText, insertList.size());
        return AjaxResult.success();
    }
}
