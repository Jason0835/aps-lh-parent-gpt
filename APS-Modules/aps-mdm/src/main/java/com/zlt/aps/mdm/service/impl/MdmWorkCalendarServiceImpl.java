package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.api.gateway.system.service.ISysMenuService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mdm.enums.MsgTemplateEnums;
import com.zlt.aps.mdm.enums.WorkCalendarPermiEnum;
import com.zlt.aps.mdm.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.mdm.service.IMdmWorkCalendarService;
import com.zlt.aps.mdm.utils.MessageServiceUtils;
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

    @Autowired
    private ISysMenuService iSysMenuService;

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
        Long userId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin(userId)) {
            return dictDataList;
        }
        List<String> permList = entityMapper.selectMenuBtPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : permList) {
            if (com.ruoyi.common.utils.StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        List<String> dictValueList = new ArrayList<>();
        WorkCalendarPermiEnum[] values = WorkCalendarPermiEnum.values();
        for (WorkCalendarPermiEnum value : values) {
            String perms = value.getPerms();
            if (permsSet.contains(perms)) {
                String dictValue = value.getDictValue();
                dictValueList.add(dictValue);
            }
        }
        dictDataList = dictDataList.stream().filter(item -> dictValueList.contains(item.getDictValue())).collect(Collectors.toList());
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

    /**
     * 复制工作日历
     *
     * @param entity 条件
     * @return 结果
     */
    @Override
    public AjaxResult copyWorkCalendar(MdmWorkCalendar entity) {
        String targetFactoryCode = entity.getTargetFactoryCode();
        Integer targetYear = entity.getTargetYear();
        Integer targetMonth = entity.getTargetMonth();
        String targetProcCode = entity.getTargetProcCode();
        LambdaUpdateWrapper<MdmWorkCalendar> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MdmWorkCalendar::getFactoryCode, targetFactoryCode)
                .eq(MdmWorkCalendar::getYear, targetYear)
                .eq(MdmWorkCalendar::getMonth, targetMonth)
                .eq(MdmWorkCalendar::getProcCode, targetProcCode)
                .set(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_DEL);
        entityMapper.update(null, updateWrapper);
        entity.setBaseVale(null);
        entityMapper.copy(entity);
        return AjaxResult.success();
    }

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @Override
    public AjaxResult checkBeforeCopy(MdmWorkCalendar entity) {
        List<SysDictData> dictDataList = iSysDictDataCacheService.getType("work_calendar_proc");
        Map<String, String> dictMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dictDataList)) {
            dictMap = dictDataList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        }
        String sourceFactoryCode = entity.getSourceFactoryCode();
        Integer sourceYear = entity.getSourceYear();
        Integer sourceMonth = entity.getSourceMonth();
        String sourceProcCode = entity.getSourceProcCode();
        String targetFactoryCode = entity.getTargetFactoryCode();
        Integer targetYear = entity.getTargetYear();
        Integer targetMonth = entity.getTargetMonth();
        String targetProcCode = entity.getTargetProcCode();
        if (sourceFactoryCode.equals(targetFactoryCode) && sourceYear.equals(targetYear) && sourceMonth.equals(targetMonth) && sourceProcCode.equals(targetProcCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmWorkCalendar.sourceAndTargetEqual"), ApsConstant.APS_YES_NO_0);
        }
        List<MdmWorkCalendar> sourceList = selectByFactoryAndYearMonth(sourceFactoryCode, sourceYear, sourceMonth, sourceProcCode);
        if (CollectionUtils.isEmpty(sourceList)) {
            return AjaxResult.error(String.format(I18nUtil.getMessage("ui.data.alert.mdmWorkCalendar.sourceNotExist"), dictMap.get(sourceProcCode), sourceYear, sourceMonth), ApsConstant.APS_YES_NO_0);
        }
        List<MdmWorkCalendar> targetList = selectByFactoryAndYearMonth(targetFactoryCode, targetYear, targetMonth, targetProcCode);
        if (CollectionUtils.isNotEmpty(targetList)) {
            return AjaxResult.success(String.format(I18nUtil.getMessage("ui.data.alert.mdmWorkCalendar.targetExists"), dictMap.get(targetProcCode), targetYear, targetMonth), ApsConstant.APS_YES_NO_1);
        }
        return AjaxResult.success(ApsConstant.APS_YES_NO_1);
    }

    private List<MdmWorkCalendar> selectByFactoryAndYearMonth(String factoryCode, Integer year, Integer month, String procCode) {
        LambdaQueryWrapper<MdmWorkCalendar> sourceWrapper = new LambdaQueryWrapper<>();
        sourceWrapper.eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                .eq(MdmWorkCalendar::getYear, year)
                .eq(MdmWorkCalendar::getMonth, month)
                .eq(MdmWorkCalendar::getProcCode, procCode);
        return entityMapper.selectList(sourceWrapper);
    }

    @Autowired
    private MessageServiceUtils messageService;

    /**
     * 发送通知计划员维护日历
     */
    @Override
    public void workCalendarNotice() {
        // 接收人自行维护
        messageService.sendNotice(MsgTemplateEnums.WORK_CALENDAR_NOTICE.getCode(), "");
    }
}
