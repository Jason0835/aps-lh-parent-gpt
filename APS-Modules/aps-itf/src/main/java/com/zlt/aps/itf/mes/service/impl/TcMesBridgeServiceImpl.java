package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.mes.mapper.TcMesSourceMapper;
import com.zlt.aps.itf.mes.service.ITcMesBridgeService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesShiftStockSyncRequest;
import com.zlt.aps.tc.api.domain.entity.*;
import com.zlt.aps.tc.api.service.ITcMesSyncRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胎侧MES库存、班次完成量和日完成量同步桥接实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcMesBridgeServiceImpl implements ITcMesBridgeService {

    private final TcMesSourceMapper sourceMapper;
    private final ITcMesSyncRemoteService remoteService;

    /**
     * 从MES读取胎侧库存并按工厂日期替换APS快照。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    @Override
    public AjaxResult syncStock(AuxReqSyncDataLogs request) {
        AuxReqSyncDataLogs normalizedRequest = this.normalizeRequest(request);
        if (normalizedRequest.getQueryParams() == null
                || normalizedRequest.getQueryParams().get("stockDate") == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        Date stockDate;
        try {
            Object stockDateValue = normalizedRequest.getQueryParams().get("stockDate");
            stockDate = stockDateValue instanceof Date ? (Date) stockDateValue
                    : DateUtil.parseDate(String.valueOf(stockDateValue));
        } catch (Exception exception) {
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        if (stockDate == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        Date normalizedStockDate = DateUtil.beginOfDay(stockDate);
        normalizedRequest.getQueryParams().put("stockDate", normalizedStockDate);
        List<TcMesStock> sourceList = this.sourceMapper.selectStockList(normalizedRequest);
        Map<String, BigDecimal> stockQtyMap = CollectionUtils.emptyIfNull(sourceList).stream()
                .filter(source -> StrUtil.isNotBlank(source.getSidewallCode()))
                .collect(Collectors.toMap(source -> StrUtil.trim(source.getSidewallCode()),
                        source -> BigDecimalUtils.valueOf(source.getStockQty()), BigDecimal::add,
                        LinkedHashMap::new));
        List<TcStock> stockList = stockQtyMap.entrySet().stream().map(entry -> {
            TcStock target = new TcStock();
            target.setSidewallCode(entry.getKey());
            target.setStockQty(entry.getValue());
            target.setBadQty(BigDecimal.ZERO);
            target.setAdjustQty(BigDecimal.ZERO);
            return target;
        }).collect(Collectors.toList());
        AjaxResult result = FeignTokenHelper.callWithToken(() -> this.remoteService.logicDeleteAndSaveStock(
                normalizedRequest.getFactoryCode(), DateUtil.formatDate(normalizedStockDate), "MES", stockList));
        this.assertRemoteSuccess(result);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.stockSyncSuccess"), stockList.size());
    }

    /**
     * 从MES读取指定物理日的胎侧库存，并替换自动滚动班次快照。
     *
     * <p>MES无数据时仍调用TC清空对应快照，防止自动滚动继续使用旧库存。</p>
     *
     * @param request 工厂、物理库存日和班序
     * @return 同步数量
     * @throws ServiceException 参数非法或远程保存失败时抛出
     */
    @Override
    public AjaxResult syncShiftStock(MesShiftStockSyncRequest request) {
        MesShiftStockSyncRequest normalizedRequest = this.normalizeShiftStockRequest(request);
        List<TcMesStock> sourceList = this.sourceMapper.selectShiftStockList(normalizedRequest);
        Map<String, BigDecimal> stockQtyMap = CollectionUtils.emptyIfNull(sourceList).stream()
                .filter(source -> StrUtil.isNotBlank(source.getSidewallCode()))
                .collect(Collectors.toMap(source -> StrUtil.trim(source.getSidewallCode()),
                        source -> BigDecimalUtils.valueOf(source.getStockQty()), BigDecimal::add,
                        LinkedHashMap::new));
        List<TcShiftStock> stockList = stockQtyMap.entrySet().stream().map(entry -> {
            TcShiftStock target = new TcShiftStock();
            target.setFactoryCode(normalizedRequest.getFactoryCode());
            target.setStockDate(normalizedRequest.getStockDate());
            target.setShiftOrder(normalizedRequest.getShiftOrder());
            target.setSidewallCode(entry.getKey());
            target.setStockQty(entry.getValue());
            target.setBadQty(BigDecimal.ZERO);
            target.setAdjustQty(BigDecimal.ZERO);
            return target;
        }).collect(Collectors.toList());
        AjaxResult result = FeignTokenHelper.callWithToken(() -> this.remoteService.replaceShiftStock(
                normalizedRequest.getFactoryCode(), DateUtil.formatDate(normalizedRequest.getStockDate()),
                normalizedRequest.getShiftOrder(), "MES", stockList));
        this.assertRemoteSuccess(result);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.stockSyncSuccess"), stockList.size());
    }

    /**
     * 从MES读取三班完成量，保存快照后回写胎侧六班结果。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    @Override
    public AjaxResult syncShiftFinishQty(AuxReqSyncDataLogs request) {
        AuxReqSyncDataLogs normalizedRequest = this.normalizeRequest(request);
        List<TcScheFinishQty> sourceList = this.sourceMapper.selectShiftFinishQtyList(normalizedRequest);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.noSourceData"));
        }
        sourceList.stream().forEach(this::fillMesAuditFields);
        Map<String, List<TcScheFinishQty>> groupMap = sourceList.stream().collect(Collectors.groupingBy(
                item -> this.buildDateGroupKey(item.getFactoryCode(), item.getScheduleDate()),
                LinkedHashMap::new, Collectors.toList()));
        groupMap.values().stream().forEach(group -> {
            TcScheFinishQty firstItem = group.get(0);
            AjaxResult saveResult = FeignTokenHelper.callWithToken(
                    () -> this.remoteService.logicDeleteAndSaveScheFinishQty(firstItem.getFactoryCode(),
                            DateUtil.formatDate(firstItem.getScheduleDate()), "MES", group));
            this.assertRemoteSuccess(saveResult);
        });
        AjaxResult writeBackResult = FeignTokenHelper.callWithToken(
                () -> this.remoteService.writeBackScheduleResultFinishQty(sourceList));
        this.assertRemoteSuccess(writeBackResult);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.finishSyncSuccess"), sourceList.size());
    }

    /**
     * 从MES读取前一日胎侧日完成量并替换APS快照。
     *
     * @param request 同步请求
     * @return 同步结果
     */
    @Override
    public AjaxResult syncDayFinishQty(AuxReqSyncDataLogs request) {
        AuxReqSyncDataLogs normalizedRequest = this.normalizeRequest(request);
        if (normalizedRequest.getQueryParams() == null) {
            normalizedRequest.setQueryParams(new HashMap<>());
        }
        normalizedRequest.getQueryParams().putIfAbsent("scheduleDate",
                DateUtils.addDays(DateUtils.truncate(DateUtils.getNowDate(), Calendar.DATE), -1));
        List<TcDayFinishQty> sourceList = this.sourceMapper.selectDayFinishQtyList(normalizedRequest);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.noSourceData"));
        }
        Map<String, TcDayFinishQty> uniqueMap = sourceList.stream().collect(Collectors.toMap(
                item -> this.buildDateGroupKey(item.getFactoryCode(), item.getScheduleDate()) + "|"
                        + StrUtil.blankToDefault(item.getSidewallCode(), ""),
                Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        List<TcDayFinishQty> dayFinishQtyList = new ArrayList<>(uniqueMap.values());
        dayFinishQtyList.stream().forEach(this::fillMesAuditFields);
        Map<String, List<TcDayFinishQty>> groupMap = dayFinishQtyList.stream().collect(Collectors.groupingBy(
                item -> this.buildDateGroupKey(item.getFactoryCode(), item.getScheduleDate()),
                LinkedHashMap::new, Collectors.toList()));
        groupMap.values().stream().forEach(group -> {
            TcDayFinishQty firstItem = group.get(0);
            AjaxResult result = FeignTokenHelper.callWithToken(
                    () -> this.remoteService.logicDeleteAndSaveDayFinishQty(firstItem.getFactoryCode(),
                            DateUtil.formatDate(firstItem.getScheduleDate()), "MES", group));
            this.assertRemoteSuccess(result);
        });
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.dayFinishSyncSuccess"),
                dayFinishQtyList.size());
    }

    /**
     * 补齐缺省工厂和公司编码。
     *
     * @param request 原请求
     * @return 规范化请求
     */
    private AuxReqSyncDataLogs normalizeRequest(AuxReqSyncDataLogs request) {
        AuxReqSyncDataLogs normalizedRequest = request == null ? new AuxReqSyncDataLogs() : request;
        normalizedRequest.setFactoryCode(StrUtil.blankToDefault(normalizedRequest.getFactoryCode(),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        normalizedRequest.setCompanyCode(StrUtil.blankToDefault(normalizedRequest.getCompanyCode(),
                normalizedRequest.getFactoryCode()));
        return normalizedRequest;
    }

    /**
     * 校验并补齐自动滚动班次库存同步请求。
     *
     * @param request 原请求
     * @return 规范化请求
     * @throws ServiceException 日期或班序缺失时抛出
     */
    private MesShiftStockSyncRequest normalizeShiftStockRequest(MesShiftStockSyncRequest request) {
        if (request == null || request.getStockDate() == null || request.getShiftOrder() == null
                || request.getShiftOrder() < 1 || request.getShiftOrder() > 6) {
            throw new ServiceException(I18nUtil.getMessage("ui.itf.mes.shiftStockArgumentsInvalid"));
        }
        request.setFactoryCode(StrUtil.blankToDefault(request.getFactoryCode(),
                FactoryConstant.DEFAULT_FACTORY_CODE));
        request.setCompanyCode(StrUtil.blankToDefault(request.getCompanyCode(), request.getFactoryCode()));
        request.setStockDate(DateUtil.beginOfDay(request.getStockDate()));
        return request;
    }

    /**
     * 补齐MES同步记录审计字段。
     *
     * @param entity 同步实体
     */
    private void fillMesAuditFields(com.ruoyi.common.core.web.domain.BaseEntity entity) {
        Date now = DateUtils.getNowDate();
        entity.setCreateBy("MES");
        entity.setUpdateBy("MES");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setIsDelete(0);
    }

    /**
     * 构造工厂日期分组键。
     *
     * @param factoryCode 工厂编码
     * @param businessDate 业务日期
     * @return 分组键
     */
    private String buildDateGroupKey(String factoryCode, Date businessDate) {
        return StrUtil.blankToDefault(factoryCode, "") + "|" + DateUtil.formatDate(businessDate);
    }

    /**
     * 校验TC远程调用返回成功。
     *
     * @param result 远程调用结果
     * @throws ServiceException 远程服务返回失败时抛出
     */
    private void assertRemoteSuccess(AjaxResult result) {
        if (result == null || !java.util.Objects.equals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG))) {
            String message = result == null || result.get(AjaxResult.MSG_TAG) == null
                    ? I18nUtil.getMessage("ui.tc.schedule.mes.remoteFailed")
                    : String.valueOf(result.get(AjaxResult.MSG_TAG));
            throw new ServiceException(message);
        }
    }
}
