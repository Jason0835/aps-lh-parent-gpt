package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.mapper.entity.CxPersionTrainSettingEntityMapper;
import com.zlt.aps.cx.service.ICxPersionTrainSettingService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxPersionTrainSetting;
import com.zlt.aps.maindata.mapper.MdmMoldingMachineEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxPersionTrainSettingServiceImpl.java
 * 描    述：CxPersionTrainSettingServiceImpl成型工序开机档数业务层处理
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class CxPersionTrainSettingServiceImpl extends AbstractDocService<CxPersionTrainSetting>  implements ICxPersionTrainSettingService {

    private final CxPersionTrainSettingEntityMapper mapper;
    private final MdmMoldingMachineEntityMapper moldingMachineEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "9004CX";
    }

    @Override
    public int save(CxPersionTrainSetting docEntityVO) {
        // 校验成型机的信息
        checkMachineQuota(docEntityVO.getQuotaClass1());
        checkMachineQuota(docEntityVO.getQuotaClass2());
        checkMachineQuota(docEntityVO.getQuotaClass3());
        
        return docEntityVO.getId() != null ? mapper.updateById(docEntityVO) : mapper.insert(docEntityVO);
    }

    /**
     * 根据日期、成型法和班次，获取未指定机台的培训额度列表
     * @param scheduleDate 排程日期
     * @param mouldMethod 成型法
     * @param shift 班次（1/2/3）
     * @return 未指定机台的定额列表（如 [100, 300, 400]）
     */
    public static List<Integer> getUnspecifiedMachineQuotas(
            List<CxPersionTrainSetting> settings,
            Date scheduleDate,
            Integer mouldMethod,
            int shift
    ) {
        return settings.stream()
                .filter(s ->  s != null
                        && s.getScheduleDate() != null
                        && s.getMouldMethod() != null
                        && s.getScheduleDate().equals(scheduleDate)
                        && s.getMouldMethod().equals(mouldMethod))
                .findFirst()
                .map(setting -> {
                    String quotaClass = getQuotaClassByShift(setting, shift);
                    if (quotaClass != null) {
                        return Arrays.stream(quotaClass.split("/"))
                                .filter(entry -> !entry.contains("-")) // 过滤未指定机台的定额
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                    }else {
                        return null;
                    }
                })
                .orElse(Collections.emptyList());
    }


    /**
     * 根据日期、成型法和班次，获取指定机台的培训额度映射表
     * @param scheduleDate 排程日期
     * @param mouldMethod 成型法
     * @param shift 班次（1/2/3）
     * @return 机台与定额的映射表（如 {"L01": 100}）
     */
    public static Map<String, Integer> getSpecifiedMachineQuotas(
            List<CxPersionTrainSetting> settings,
            Date scheduleDate,
            Integer mouldMethod,
            int shift
    ) {
        return settings.stream()
                .filter(s -> s != null
                        && s.getScheduleDate() != null
                        && s.getMouldMethod() != null
                        && s.getScheduleDate().equals(scheduleDate)
                        && s.getMouldMethod().equals(mouldMethod))
                .findFirst()
                .map(setting -> {
                    String quotaClass = getQuotaClassByShift(setting, shift);
                    if (quotaClass != null) {
                        return Arrays.stream(quotaClass.split("/"))
                                .filter(entry -> entry.contains("-")) // 过滤指定机台的定额
                                .map(entry -> entry.split("-"))
                                .collect(Collectors.toMap(
                                        parts -> parts[0], // 机台编号
                                        parts -> Integer.parseInt(parts[1]) // 定额
                                ));
                    }else {
                        return null;
                    }
                })
                .orElse(Collections.emptyMap());
    }


    private static String getQuotaClassByShift(CxPersionTrainSetting setting, int shift) {
        switch (shift) {
            case 1: return setting.getQuotaClass1();
            case 2: return setting.getQuotaClass2();
            case 3: return setting.getQuotaClass3();
            default: throw new IllegalArgumentException("无效班次: " + shift);
        }
    }



    /**
     * 获取所有班次的机台培训数据（合并1/2/3班）
     * @param scheduleDate 排程日期
     * @param mouldMethod 成型法
     * @return 合并后的机台与定额映射表
     */
    public static Map<String, Integer> getAllMachineQuotas(
            List<CxPersionTrainSetting> settings,
            Date scheduleDate,
            Integer mouldMethod
    ) {
        Map<String, Integer> mergedMap = new HashMap<>();
        for (int shift = 1; shift <= 3; shift++) {
            Map<String, Integer> shiftQuotas =
                    getSpecifiedMachineQuotas(settings, scheduleDate, mouldMethod, shift);
            mergedMap.putAll(shiftQuotas);
        }
        return mergedMap;
    }



    /**
     * 定额格式按照 机台-定额分组，多个使用 / 分割
     * 校验格式是否正确，机台是否存在
     */
    private void checkMachineQuota(String machineQuota) {
        if (StringUtils.isBlank(machineQuota)) {
            return;
        }

        String[] quotaArray = machineQuota.split("/");

        // 记录对应机台编号，校验机台编号存在
        Set<String> machineSet = new HashSet<>();
        // 校验对应定额，是否为不小于0的数值
        String regex = "^\\d+(\\.\\d+)?$";

        for (String item : quotaArray) {
            if (StringUtils.isBlank(item)) {
                throw new RuntimeException(I18nUtil.getMessage("ui.data.column.cxPersionTrainSetting.checkMachineQuota"));
            }
            int index = item.lastIndexOf("-");
            String quota = item;
            if (index >= 0) {
                // 如果有机台，拆分校验
                String machine = item.substring(0, index);
                machineSet.add(machine);
                quota = item.substring(index + 1);
            }

            if (!Pattern.matches(regex, quota)) {
                throw new RuntimeException(StringUtils.format(I18nUtil.getMessage("ui.data.column.cxPersionTrainSetting.checkQuota"), quota));
            }
        }

        // 校验机台是否存在
        if (!machineSet.isEmpty()) {
            LambdaQueryWrapper<MdmMoldingMachine> machineWrapper = Wrappers.lambdaQuery(MdmMoldingMachine.class);
            machineWrapper.in(MdmMoldingMachine::getCxMachineCode, machineSet);
            List<MdmMoldingMachine> moldingMachineList = moldingMachineEntityMapper.selectList(machineWrapper);
            Set<String> existMachine = moldingMachineList.stream().map(MdmMoldingMachine::getCxMachineCode).collect(Collectors.toSet());
            for (String item : machineSet) {
                if (!existMachine.contains(item)) {
                    throw new RuntimeException(StringUtils.format(I18nUtil.getMessage("ui.data.column.cxPersionTrainSetting.checkMachine"), item));
                }
            }
        }
    }

    @Override
    public int removeByIds(List<Long> ids) {
        return mapper.deleteBatchIds(ids);
    }

    @Override
    public String checkUnique(CxPersionTrainSetting queryVO) {
        LambdaQueryWrapper<CxPersionTrainSetting> lqw = Wrappers.lambdaQuery();
        lqw.ne(queryVO.getId() != null, CxPersionTrainSetting::getId, queryVO.getId());
        lqw.eq(queryVO.getScheduleDate() != null, CxPersionTrainSetting::getScheduleDate, queryVO.getScheduleDate());
        lqw.eq(queryVO.getMouldMethod() != null, CxPersionTrainSetting::getMouldMethod, queryVO.getMouldMethod());
        if (mapper.selectCount(lqw) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询列表
     */
    @Override
    public List<CxPersionTrainSetting> selectList(CxPersionTrainSetting queryVO) {
        LambdaQueryWrapper<CxPersionTrainSetting> lqw = Wrappers.lambdaQuery();
        lqw.ge(queryVO.getBeginDate() != null, CxPersionTrainSetting::getScheduleDate, queryVO.getBeginDate());
        lqw.le(queryVO.getEndDate() != null, CxPersionTrainSetting::getScheduleDate, queryVO.getEndDate());
        lqw.eq(queryVO.getScheduleDate() != null, CxPersionTrainSetting::getScheduleDate, queryVO.getScheduleDate());
        lqw.eq(queryVO.getMouldMethod() != null, CxPersionTrainSetting::getMouldMethod, queryVO.getMouldMethod());
        lqw.orderByAsc(CxPersionTrainSetting::getScheduleDate);
        return mapper.selectList(lqw);
    }

    /**
     * 详情
     */
    @Override
    public CxPersionTrainSetting getInfo(Long billId) {
        return mapper.selectById(billId);
    }

    /**
     * 列表校验唯一并保存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveList(List<CxPersionTrainSetting> list) {
        // 过滤成型法数据为空的记录
        list = list.stream().filter(v -> v.getMouldMethod() != null).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error();
        }

        // 校验唯一性，保证当前保存记录+历史记录 对应 排产日期+成型法保证只有一条
        List<Date> scheduleDateList = list.stream().map(CxPersionTrainSetting::getScheduleDate).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Set<Long> idSet = list.stream().map(CxPersionTrainSetting::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<CxPersionTrainSetting> checkRepeatList = new ArrayList<>(list);
        if (CollectionUtils.isNotEmpty(scheduleDateList)) {
            List<CxPersionTrainSetting> historyList = mapper.selectList(
                            Wrappers.lambdaQuery(CxPersionTrainSetting.class).in(CxPersionTrainSetting::getScheduleDate, scheduleDateList))
                    .stream().filter(v -> !idSet.contains(v.getId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(historyList)) {
                checkRepeatList.addAll(historyList);
            }
        }
        Map<String, Long> repeatMap = checkRepeatList.stream().collect(Collectors.groupingBy(v -> GenerageMapKeyUtils.createMapKey(v.getScheduleDate(), v.getMouldMethod()), Collectors.counting()));
        long repeatCount = repeatMap.values().stream().filter(v -> v > 1).count();
        if (repeatCount > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.cxPersionTrainSetting.checkUnique"));
        }

        List<CxPersionTrainSetting> updateList = list.stream().filter(v -> v.getId() != null).collect(Collectors.toList());
        List<CxPersionTrainSetting> insertList = list.stream().filter(v -> v.getId() == null).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(updateList)) {
            this.baseDao.updateBatch(updateList);
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            this.baseDao.insertBatch(insertList);
        }

        return AjaxResult.success();
    }
}



