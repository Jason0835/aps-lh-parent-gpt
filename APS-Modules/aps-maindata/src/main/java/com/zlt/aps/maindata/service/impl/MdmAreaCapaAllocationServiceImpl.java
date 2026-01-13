package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.DpAreaEntityMapper;
import com.zlt.aps.maindata.mapper.MdmAreaCapaAllocationEntityMapper;
import com.zlt.aps.maindata.service.IMdmAreaCapaAllocationService;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.monthplan.api.domain.entity.DpArea;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.monthplan.api.service.IRemoteImportLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmAreaCapaAllocationServiceImpl.java
 * 描    述：MdmAreaCapaAllocationServiceImpl区域产能分配业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmAreaCapaAllocationServiceImpl extends AbstractDocService<MdmAreaCapaAllocation> implements IMdmAreaCapaAllocationService {

    @Autowired
    private MdmAreaCapaAllocationEntityMapper mdmAreaCapaAllocationEntityMapper;

    @Autowired
    private DpAreaEntityMapper dpAreaEntityMapper;

    @Autowired
    private IRemoteImportLogService iRemoteImportLogService;

    @Autowired
    private IRemoteImportErrorLogService iRemoteImportErrorLogService;

    @Autowired
    private FactoryParamServiceImpl factoryParamService;

    @Override
    protected String getDocTypeCode() {
        return "MDM0141";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0141");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmAreaCapaAllocation docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "areaCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<MdmAreaCapaAllocation> list, List<MdmAreaCapaAllocation> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        String areaCodeNotExistsMessage = I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.areaCodeNotExists");
        serviceCheckParams.put("areaCodeNotExistsMessage", areaCodeNotExistsMessage);
        // 查询区域数据，转义区域
        List<DpArea> dpAreaList = dpAreaEntityMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, String> areaMap;
        if (CollectionUtils.isNotEmpty(dpAreaList)) {
            JsonI18nConvertUtils.conventJsonI18n(dpAreaList, DpArea.class);
            areaMap = dpAreaList.stream().collect(Collectors.toMap(DpArea::getAreaNameI18n, DpArea::getAreaCode, (old, newValue) -> newValue));
            serviceCheckParams.put("areaMap", areaMap);
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmAreaCapaAllocation importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (serviceCheckParams.containsKey("areaMap")) {
            Map<String, String> areaMap = (Map<String, String>) serviceCheckParams.get("areaMap");
            String areaCodeNameI18n = importDocEntity.getAreaCodeNameI18n();
            if (areaMap.containsKey(areaCodeNameI18n)) {
                String areaCode = areaMap.get(areaCodeNameI18n);
                importDocEntity.setAreaCode(areaCode);
            } else {
                String areaCodeNotExistsMessage = serviceCheckParams.get("areaCodeNotExistsMessage").toString();
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorRowNum, areaCodeNotExistsMessage, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 异步导入
     */
    @Async
    @Override
    public void importDataAsync(List<MdmAreaCapaAllocation> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.importData(list, updateSupport, importLogId);
            Date endTime = DateUtils.getNowDate();
            importLog.setRowCount(list.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            RemoteImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, iRemoteImportLogService);
            RemoteImportExcelUtils.saveImportErrorLogs(result, iRemoteImportErrorLogService);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public AjaxResult copy(MdmAreaCapaAllocation entity) {
        String targetFactoryCode = entity.getTargetFactoryCode();
        Integer targetYear = entity.getTargetYear();
        Integer targetMonth = entity.getTargetMonth();
        LambdaUpdateWrapper<MdmAreaCapaAllocation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MdmAreaCapaAllocation::getFactoryCode, targetFactoryCode)
                .eq(MdmAreaCapaAllocation::getYear, targetYear)
                .eq(MdmAreaCapaAllocation::getMonth, targetMonth)
                .set(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_DEL);
        mdmAreaCapaAllocationEntityMapper.update(null, updateWrapper);
        entity.setBaseVale(null);
        mdmAreaCapaAllocationEntityMapper.copy(entity);
        return AjaxResult.success();
    }

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public AjaxResult checkBeforeCopy(MdmAreaCapaAllocation entity) {
        String sourceFactoryCode = entity.getSourceFactoryCode();
        Integer sourceYear = entity.getSourceYear();
        Integer sourceMonth = entity.getSourceMonth();
        String targetFactoryCode = entity.getTargetFactoryCode();
        Integer targetYear = entity.getTargetYear();
        Integer targetMonth = entity.getTargetMonth();
        if (sourceFactoryCode.equals(targetFactoryCode) && sourceYear.equals(targetYear) && sourceMonth.equals(targetMonth)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.sourceAndTargetEqual"), ApsConstant.APS_YES_NO_0);
        }
        List<MdmAreaCapaAllocation> sourceList = selectByFactoryAndYearMonth(sourceFactoryCode, sourceYear, sourceMonth);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.error(String.format(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.sourceNotExist"), sourceYear, sourceMonth), ApsConstant.APS_YES_NO_0);
        }
        List<MdmAreaCapaAllocation> targetList = selectByFactoryAndYearMonth(targetFactoryCode, targetYear, targetMonth);
        if (CollectionUtils.isNotEmpty(targetList)) {
            return AjaxResult.success(String.format(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.targetExists"), targetYear, targetMonth), ApsConstant.APS_YES_NO_1);
        }
        return AjaxResult.success(ApsConstant.APS_YES_NO_1);
    }

    @Override
    public List<MdmAreaCapaAllocation> findAreaCapaAllocation(int year, int month, String factoryCode, String sysDocTypeCode) {
        LambdaQueryWrapper<MdmAreaCapaAllocation> sourceWrapper = new LambdaQueryWrapper<>();
        sourceWrapper
            .eq(MdmAreaCapaAllocation::getYear, year)
            .eq(MdmAreaCapaAllocation::getMonth,month)
            .eq(MdmAreaCapaAllocation::getFactoryCode, factoryCode)
            .eq(MdmAreaCapaAllocation::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmAreaCapaAllocation> list = mdmAreaCapaAllocationEntityMapper.selectList(sourceWrapper);

        if (StringUtils.isNotEmpty(sysDocTypeCode)) {
            // desc  区域产能数据查询出来后要重新调整, 防止出现区域产能分配不均的情况 2026-1-13
            // 1.获取当前年月的总天数
            int days = getDaysOfMonth(year, month);
            // 2.从参数表获取当前配置的日产
            FactoryParam search = new FactoryParam();
            search.setFactoryCode(factoryCode);
            search.setBusinessGroup(sysDocTypeCode);
            search.setParamCode(MonthPlanEnums.NET_REQUIREMENT_DAY_CAPACITY.getCode());
            search.setParamCode(ProductTypeEnum.WHOLE_STEEL.getValue());
            FactoryParam factoryParam = factoryParamService.getFacParamSingle(search);

            // 3.从参数配置获取日产乘以天数
            Integer dayCapacity = null;
            if (factoryParam == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmAreaCapaAllocation.factoryParamNotExists"));
            } else {
                dayCapacity = factoryParam.getParamValue() == null ? Integer.parseInt(factoryParam.getDefauleValue() == null ? "0" : factoryParam.getDefauleValue()) : Integer.parseInt(factoryParam.getParamValue());
            }
            int totalCapacity = dayCapacity * days;

            // 4.计算区域产能分配比例
            BigDecimal totalAreaCapacity = list.stream().map(MdmAreaCapaAllocation::getCapacityAllocation).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalAreaCapacityRate = (new BigDecimal(totalCapacity).divide(totalAreaCapacity, 2, RoundingMode.HALF_UP));

            // 5.按照比例重新调整区域产能,最后一笔倒扣
            for (int i = 0; i < list.size(); i++) {
                MdmAreaCapaAllocation allocation = list.get(i);
                BigDecimal areaCapacity = allocation.getCapacityAllocation().multiply(totalAreaCapacityRate);
                if (i == list.size() - 1) {
                    areaCapacity = new BigDecimal(totalCapacity).subtract(totalAreaCapacity);
                }
                allocation.setCapacityAllocation(areaCapacity);
            }
        }
        return list;
    }

    /**
     * 获取指定年份和月份的总天数
     *
     * @param year  年份
     * @param month 月份（1-12）
     * @return 该月的总天数
     */
    public static int getDaysOfMonth(int year, int month) {
        // 使用 YearMonth 类计算指定月份的天数
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }

    private List<MdmAreaCapaAllocation> selectByFactoryAndYearMonth(String factoryCode, Integer year, Integer month) {
        LambdaQueryWrapper<MdmAreaCapaAllocation> sourceWrapper = new LambdaQueryWrapper<>();
        sourceWrapper.eq(MdmAreaCapaAllocation::getFactoryCode, factoryCode)
                .eq(MdmAreaCapaAllocation::getYear, year)
                .eq(MdmAreaCapaAllocation::getMonth, month);
        return mdmAreaCapaAllocationEntityMapper.selectList(sourceWrapper);
    }
}
