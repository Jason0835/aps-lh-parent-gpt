package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.service.IMdmWorkCalendarService;
import com.zlt.aps.monthplan.api.domain.entity.MdmWorkCalendar;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
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
        Integer year = entity.getYear();
        String procCode = entity.getProcCode();
        String factoryCode = entity.getFactoryCode();
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
            int lastDay = instance.getActualMaximum(Calendar.DATE);
            for (int i = 1; i <= lastDay; i++) {
                for (String code : procCodeList) {
                    MdmWorkCalendar mdmWorkCalendar = new MdmWorkCalendar();
                    mdmWorkCalendar.setFactoryCode(factoryCode);
                    mdmWorkCalendar.setProcCode(code);
                    mdmWorkCalendar.setYear(year);
                    mdmWorkCalendar.setMonth(month + 1);
                    mdmWorkCalendar.setDay(i);
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
