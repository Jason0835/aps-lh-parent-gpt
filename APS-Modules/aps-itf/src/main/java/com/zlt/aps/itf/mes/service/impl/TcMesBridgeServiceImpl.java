package com.zlt.aps.itf.mes.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.itf.mes.mapper.TcMesSourceMapper;
import com.zlt.aps.itf.mes.service.ITcMesBridgeService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcMesStock;
import com.zlt.aps.tc.api.domain.entity.TcScheFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.service.ITcMesSyncRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
        List<TcMesStock> sourceList = this.sourceMapper.selectStockList(normalizedRequest);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.noSourceData"));
        }
        List<TcStock> stockList = sourceList.stream().map(source -> {
            TcStock target = new TcStock();
            BeanUtils.copyProperties(source, target);
            target.setCreateBy("MES");
            target.setUpdateBy("MES");
            return target;
        }).collect(Collectors.toList());
        Map<String, List<TcStock>> groupMap = stockList.stream().collect(Collectors.groupingBy(
                item -> this.buildDateGroupKey(item.getFactoryCode(), item.getStockDate()),
                LinkedHashMap::new, Collectors.toList()));
        groupMap.values().stream().forEach(group -> {
            TcStock firstItem = group.get(0);
            AjaxResult result = FeignTokenHelper.callWithToken(() -> this.remoteService.logicDeleteAndSaveStock(
                    firstItem.getFactoryCode(), DateUtil.formatDate(firstItem.getStockDate()), "MES", group));
            this.assertRemoteSuccess(result);
        });
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
