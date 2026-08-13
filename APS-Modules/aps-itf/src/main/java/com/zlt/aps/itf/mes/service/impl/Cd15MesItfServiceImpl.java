package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftStock;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.api.service.ICd15MesSyncRemoteService;
import com.zlt.aps.cd15.api.service.ICd15StockRemoteService;
import com.zlt.aps.cd15.api.service.ICd15StorageLaneLimitRemoteService;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.domain.Cd15MesStock;
import com.zlt.aps.itf.mes.mapper.Cd15MesItfMapper;
import com.zlt.aps.itf.mes.service.ICd15MesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
 * 斜裁MES接口服务实现。
 */
@Slf4j
@Service("cd15MesItfService")
@RequiredArgsConstructor
public class Cd15MesItfServiceImpl implements ICd15MesItfService {

    private final Cd15MesItfMapper cd15MesItfMapper;
    private final ICd15MesSyncRemoteService cd15MesSyncRemoteService;
    private final ICd15StockRemoteService cd15StockRemoteService;
    private final ICd15StorageLaneLimitRemoteService cd15StorageLaneLimitRemoteService;

    @Override
    public AjaxResult syncStock(AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = FactoryConstant.DEFAULT_FACTORY_CODE;
        syncDataLogs.setFactoryCode(factoryCode);
        List<Cd15MesStock> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.cd15MesItfMapper.selectStockList(syncDataLogs);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.cd15.stock.syncNoData"));
        }
        Map<String, Cd15MesStock> stockMap = sourceList.stream().collect(Collectors.toMap(
                item -> DateUtil.formatDate(item.getStockDate()) + "|" + item.getMaterialCode(),
                Function.identity(),
                (first, second) -> {
                    BigDecimal firstValue = first.getAvailableStock() == null
                            ? BigDecimal.ZERO : first.getAvailableStock();
                    BigDecimal secondValue = second.getAvailableStock() == null
                            ? BigDecimal.ZERO : second.getAvailableStock();
                    first.setAvailableStock(firstValue.add(secondValue));
                    return first;
                }));
        Date now = DateUtils.getNowDate();
        List<Cd15Stock> stockList = stockMap.values().stream().map(source -> {
            Cd15Stock target = new Cd15Stock();
            target.setFactoryCode(factoryCode);
            target.setStockDate(source.getStockDate());
            target.setMaterialCode(source.getMaterialCode());
            target.setStockNum(source.getAvailableStock().doubleValue());
            target.setModifyNum(0D);
            target.setBadNum(0D);
            target.setCreateBy("MES");
            target.setUpdateBy("MES");
            target.setCreateTime(now);
            target.setUpdateTime(now);
            target.setIsDelete(0);
            return target;
        }).collect(Collectors.toList());
        Map<String, List<Cd15Stock>> listByDate = stockList.stream().collect(Collectors.groupingBy(
                item -> DateUtil.formatDate(item.getStockDate()), LinkedHashMap::new, Collectors.toList()));
        try {
            AjaxResult result = FeignTokenHelper.callWithToken(() -> {
                for (Map.Entry<String, List<Cd15Stock>> entry : listByDate.entrySet()) {
                    AjaxResult scopeResult = this.cd15StockRemoteService.logicDeleteAndSaveMesBatch(
                            factoryCode, entry.getKey(), "MES", entry.getValue());
                    if (scopeResult == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                            scopeResult.get(AjaxResult.CODE_TAG))) {
                        return scopeResult;
                    }
                }
                return AjaxResult.success();
            });
            if (result == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                    result.get(AjaxResult.CODE_TAG))) {
                Object message = result == null ? "" : result.get(AjaxResult.MSG_TAG);
                return AjaxResult.error(MessageFormat.format(
                        I18nUtil.getMessage("ui.cd15.stock.syncFailed"), message));
            }
        } catch (Exception exception) {
            log.error("斜裁库存同步调用异常：factoryCode={}，数量={}", factoryCode, stockList.size(), exception);
            return AjaxResult.error(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd15.stock.syncFailed"), exception.getMessage()));
        }
        return AjaxResult.success();
    }

    @Override
    public AjaxResult syncShiftStock(MesShiftStockSyncRequest request) {
        if (request == null || request.getStockDate() == null
                || StringUtils.isBlank(request.getShiftCode()) || request.getShiftStartTime() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.shiftStock.syncArgumentsInvalid"));
        }
        request.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        request.setCompanyCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
        request.setShiftCode(request.getShiftCode().trim());
        List<Cd15MesStock> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.cd15MesItfMapper.selectShiftStockList(request);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        List<Cd15MesStock> safeSourceList = sourceList == null ? Collections.emptyList() : sourceList;
        Set<String> materialCodes = new HashSet<>();
        if (safeSourceList.stream().anyMatch(source -> source == null
                || StringUtils.isBlank(source.getMaterialCode())
                || source.getAvailableStock() == null
                || !materialCodes.add(source.getMaterialCode().trim()))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.shiftStock.syncSourceInvalid"));
        }
        Date now = DateUtils.getNowDate();
        List<Cd15ShiftStock> stockList = safeSourceList.stream().map(source -> {
            Cd15ShiftStock target = new Cd15ShiftStock();
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
            AjaxResult result = FeignTokenHelper.callWithToken(() ->
                    this.cd15MesSyncRemoteService.replaceShiftStock(request.getFactoryCode(),
                            DateUtil.formatDate(request.getStockDate()), request.getShiftCode(),
                            DateUtil.formatDateTime(request.getShiftStartTime()), "MES", stockList));
            if (result == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                    result.get(AjaxResult.CODE_TAG))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.shiftStockRemoteFailed"));
            }
        } catch (Exception exception) {
            log.error("斜裁班次库存同步调用异常：factoryCode={}，shiftCode={}，shiftStartTime={}",
                    request.getFactoryCode(), request.getShiftCode(), request.getShiftStartTime(), exception);
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.shiftStockRemoteFailed"));
        }
        return AjaxResult.success(stockList.size());
    }

    @Override
    public AjaxResult syncStorageLaneLimit(AuxReqSyncDataLogs syncDataLogs) {
        String factoryCode = StringUtils.defaultIfBlank(syncDataLogs.getFactoryCode(),
                FactoryConstant.DEFAULT_FACTORY_CODE);
        syncDataLogs.setFactoryCode(factoryCode);
        String targetLaneDate = syncDataLogs.getQueryParams() == null ? null
                : Objects.toString(syncDataLogs.getQueryParams().get("laneDate"), null);
        String targetShiftCode = syncDataLogs.getQueryParams() == null ? null
                : Objects.toString(syncDataLogs.getQueryParams().get("shiftCode"), null);
        List<Cd15StorageLaneLimit> sourceList;
        DynamicDataSourceContextHolder.push(DataSource.MES);
        try {
            sourceList = this.cd15MesItfMapper.selectStorageLaneLimitList(syncDataLogs);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
        if (CollectionUtils.isEmpty(sourceList)) {
            if (StringUtils.isNotBlank(targetLaneDate) && StringUtils.isNotBlank(targetShiftCode)) {
                try {
                    AjaxResult result = FeignTokenHelper.callWithToken(() ->
                            this.cd15StorageLaneLimitRemoteService.logicDeleteAndSaveMesBatch(
                                    factoryCode, targetLaneDate, targetShiftCode, "MES", Collections.emptyList()));
                    if (result == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                            result.get(AjaxResult.CODE_TAG))) {
                        return AjaxResult.error(I18nUtil.getMessage("ui.cd15.storageLaneLimit.syncFailed"));
                    }
                } catch (Exception exception) {
                    log.error("斜裁库排空快照清理失败：factoryCode={}，laneDate={}，shiftCode={}",
                            factoryCode, targetLaneDate, targetShiftCode, exception);
                    return AjaxResult.error(I18nUtil.getMessage("ui.cd15.storageLaneLimit.syncFailed"));
                }
            }
            return AjaxResult.success(I18nUtil.getMessage("ui.cd15.storageLaneLimit.syncNoData"));
        }
        Cd15StorageLaneLimit invalidSource = sourceList.stream().filter(source -> source.getLaneDate() == null
                || StringUtils.isBlank(source.getShiftCode()) || StringUtils.isBlank(source.getStorageLaneCode())
                || source.getCarNum() == null || source.getMaxCarNum() == null
                || source.getMaxCarNum() <= 0 || source.getCarNum() < 0
                || source.getCarNum() > source.getMaxCarNum()).findFirst().orElse(null);
        if (invalidSource != null) {
            return AjaxResult.error(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd15.storageLaneLimit.syncInvalid"),
                    StringUtils.defaultString(invalidSource.getStorageLaneCode())));
        }
        Date now = DateUtils.getNowDate();
        List<Cd15StorageLaneLimit> insertList = sourceList.stream().map(source -> {
            Cd15StorageLaneLimit target = new Cd15StorageLaneLimit();
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
        Map<String, List<Cd15StorageLaneLimit>> listByScope = insertList.stream()
                .collect(Collectors.groupingBy(item -> DateUtil.formatDate(item.getLaneDate())
                                + "|" + item.getShiftCode(), LinkedHashMap::new, Collectors.toList()));
        try {
            AjaxResult result = FeignTokenHelper.callWithToken(() -> {
                for (List<Cd15StorageLaneLimit> scopeList : listByScope.values()) {
                    Cd15StorageLaneLimit scope = scopeList.get(0);
                    AjaxResult scopeResult = this.cd15StorageLaneLimitRemoteService.logicDeleteAndSaveMesBatch(
                            factoryCode, DateUtil.formatDate(scope.getLaneDate()),
                            scope.getShiftCode(), "MES", scopeList);
                    if (scopeResult == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                            scopeResult.get(AjaxResult.CODE_TAG))) {
                        return scopeResult;
                    }
                }
                return AjaxResult.success();
            });
            if (result == null || !Objects.equals(AppUtils.AJAX_RESULT_SUCCESS,
                    result.get(AjaxResult.CODE_TAG))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.cd15.storageLaneLimit.syncFailed"));
            }
        } catch (Exception exception) {
            log.error("斜裁库排同步调用异常：factoryCode={}，数量={}", factoryCode, insertList.size(), exception);
            return AjaxResult.error(I18nUtil.getMessage("ui.cd15.storageLaneLimit.syncFailed"));
        }
        return AjaxResult.success();
    }
}
