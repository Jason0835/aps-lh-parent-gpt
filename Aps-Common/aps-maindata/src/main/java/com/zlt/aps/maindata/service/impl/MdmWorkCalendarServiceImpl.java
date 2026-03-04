package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.maindata.service.IMdmWorkCalendarService;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmWorkCalendarServiceImpl.java
 * 描    述：MdmWorkCalendarServiceImpl工作日历业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmWorkCalendarServiceImpl extends AbstractDocService<MdmWorkCalendar> implements IMdmWorkCalendarService {

    /**
     * 全年月份
     */
    private static final List<Integer> MONTH_CALENDAR = new ArrayList<>(Arrays.asList(
            Calendar.JANUARY,
            Calendar.FEBRUARY,
            Calendar.MARCH,
            Calendar.APRIL,
            Calendar.MAY,
            Calendar.JUNE,
            Calendar.JULY,
            Calendar.AUGUST,
            Calendar.SEPTEMBER,
            Calendar.OCTOBER,
            Calendar.NOVEMBER,
            Calendar.DECEMBER
    ));

    @Autowired
    private MdmWorkCalendarEntityMapper entityMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0104";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0104");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmWorkCalendar docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmWorkCalendar.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "procCode", "year", "month", "day"));
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(MdmWorkCalendar importDocEntity, List<ImportErrorLog> importErrorLogs, Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        Date productionDate = importDocEntity.getProductionDate();
        int year = DateUtils.getYear(productionDate);
        int month = DateUtils.getMonth(productionDate);
        int day = DateUtils.getDay(productionDate);
        importDocEntity.setYear(year);
        importDocEntity.setMonth(month);
        importDocEntity.setDay(day);
        // 停产比例改成0
        if (YesOrNoEnum.NO.getCode().equals(importDocEntity.getDayFlag())) {
            importDocEntity.setRate(0);
        }
        // 比例如果是0，赋值成停产
        Integer rate = importDocEntity.getRate();
        if (rate == 0) {
            importDocEntity.setDayFlag(YesOrNoEnum.NO.getCode());
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    /**
     * 根据用户名称过滤出可查看的工序列表
     *
     * @param userName 用户名称
     * @return 结果
     */
    @Override
    public List<SysDictData> selectProcCodeList(String userName) {
        List<SysDictData> dictDataList = iSysDictDataCacheService.getType("work_calendar_proc");
        if (StringUtils.isBlank(userName)) {
            return dictDataList;
        }
        // TODO 根据当前用户对应的权限，过滤出可查看的工序列表
        return dictDataList;
    }

    /**
     * 生成全年工作日历
     *
     * @param entity 条件
     * @return 结果
     */
    @Override
    public AjaxResult genAnnualPlan(MdmWorkCalendar entity) {
        String factoryCode = StringUtils.defaultIfBlank(entity.getFactoryCode(), FactoryConstant.DEFAULT_FACTORY_CODE);
        Integer year = entity.getYear();
        String procCode = entity.getProcCode();
        LambdaQueryWrapper<MdmWorkCalendar> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmWorkCalendar::getYear, year);
        wrapper.eq(MdmWorkCalendar::getFactoryCode, factoryCode);
        wrapper.eq(StringUtils.isNotBlank(procCode), MdmWorkCalendar::getProcCode, procCode);
        List<MdmWorkCalendar> mdmWorkCalendarList = entityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(mdmWorkCalendarList)) {
            throw new RuntimeException("已经生成过对应年份的工作日历");
        }
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR, year);
        List<MdmWorkCalendar> saveList = new ArrayList<>();
        List<String> procCodeList = new ArrayList<>();
        if (StringUtils.isBlank(procCode)) {
            List<SysDictData> procCodeDictDataList = this.selectProcCodeList("");
            List<String> dictValueList = procCodeDictDataList.stream().map(SysDictData::getDictValue).collect(Collectors.toList());
            procCodeList.addAll(dictValueList);
        } else {
            procCodeList.add(procCode);
        }
        for (Integer month : MONTH_CALENDAR) {
            instance.set(Calendar.MONTH, month);
            instance.set(Calendar.DAY_OF_MONTH, 1);
            int lastDay = instance.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= lastDay; i++) {
                for (String code : procCodeList) {
                    MdmWorkCalendar mdmWorkCalendar = new MdmWorkCalendar();
                    mdmWorkCalendar.setFactoryCode(factoryCode);
                    mdmWorkCalendar.setProcCode(code);
                    mdmWorkCalendar.setYear(year);
                    mdmWorkCalendar.setMonth(month + 1);
                    mdmWorkCalendar.setDay(i);
                    instance.set(Calendar.DAY_OF_MONTH, i);
                    mdmWorkCalendar.setProductionDate(instance.getTime());
                    mdmWorkCalendar.setOneShiftFlag(ApsConstant.TRUE);
                    mdmWorkCalendar.setTwoShiftFlag(ApsConstant.TRUE);
                    mdmWorkCalendar.setThreeShiftFlag(ApsConstant.TRUE);
                    mdmWorkCalendar.setDayFlag(ApsConstant.TRUE);
                    mdmWorkCalendar.setRate(100);
                    saveList.add(mdmWorkCalendar);
                }
            }
        }
        this.save(saveList);
        return AjaxResult.success();
    }
}
