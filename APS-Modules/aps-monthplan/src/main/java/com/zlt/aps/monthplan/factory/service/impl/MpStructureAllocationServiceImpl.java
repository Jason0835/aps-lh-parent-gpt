package com.zlt.aps.monthplan.factory.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpStructureAllocationServiceImpl.java
 * 描    述：MpStructureAllocationServiceImpl排产过程_结构排产业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MpStructureAllocationServiceImpl extends AbstractDocService<MpStructureAllocation> implements IMpStructureAllocationService {

    private final MpStructureAllocationEntityMapper entityMapper;
    private final FactoryMonthPlanProductionFinalResultServiceImpl monthPlanProductionFinalResultService;

    @Override
    public List<MpStructureAllocation> getDataList(MpStructureAllocation param) {
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, param);
        queryWrapper.orderByAsc("STRUCTURE_NAME", "CX_MACHINE_CODE");
        return this.entityMapper.selectList(queryWrapper);
    }


    /**
     * 条件拼接
     *
     * @param queryWrapper 查询条件构建器
     * @param param        查询条件值对象
     */
    private void builderCondition(QueryWrapper<MpStructureAllocation> queryWrapper, MpStructureAllocation param) {
        queryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        queryWrapper.eq("YEAR", param.getYear());
        queryWrapper.eq("MONTH", param.getMonth());
        queryWrapper.eq(PubUtil.isNotEmpty(param.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(param.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", param.getProductionVersion());

        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.like(PubUtil.isNotEmpty(param.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", param.getFieldValueByFieldName("structureName"));
        queryWrapper.like(PubUtil.isNotEmpty(param.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", param.getFieldValueByFieldName("cxMachineCode"));
    }

    @Override
    protected String getDocTypeCode() {
        return "MDM0408";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0408");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpStructureAllocation docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpStructureAllocation.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Arrays.asList("factoryCode","year","month","structureName", "productionVersion");
    }


    @Override
    public int save(MpStructureAllocation mpStructureAllocation) {
        // 工厂
        String factoryCode = mpStructureAllocation.getFactoryCode();
        // 年
        Integer year = mpStructureAllocation.getYear();
        // 月
        Integer month = mpStructureAllocation.getMonth();
        // 获取定稿的月度计划
        FactoryMonthPlanProductionFinalResult param = new FactoryMonthPlanProductionFinalResult();
        param.setFactoryCode(factoryCode);
        param.setYear(year);
        param.setMonth(month);

        List<FactoryMonthPlanProductionFinalResult>  monthPlanProductionFinalResultList = monthPlanProductionFinalResultService.listMonthProdFinalPlans(param);
        if (PubUtil.isNotEmpty(monthPlanProductionFinalResultList)) {
            FactoryMonthPlanProductionFinalResult monthPlanProductionFinalResult = monthPlanProductionFinalResultList.get(0);
            mpStructureAllocation.setMonthPlanVersion(monthPlanProductionFinalResult.getMonthPlanVersion());
            mpStructureAllocation.setProductionVersion(monthPlanProductionFinalResult.getProductionVersion());
        }
        mpStructureAllocation.setBaseVale(null);
        this.checkUnique(mpStructureAllocation);
        return baseDao.save(mpStructureAllocation);
    }




    /**
     * 获取日期最接近的下一个结构
     * @param param
     * @return
     */
    @Override
    public MpStructureAllocation getNextStructure(MpStructureAllocation param) {
        // 通过工厂、年月、成型机编码获取结构排产列表
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, param);
        List<MpStructureAllocation> structureAllocationList = entityMapper.selectList(queryWrapper);
        // 开始日期
        Integer beginDay = param.getBeginDay();
        // 结束日期
        Integer endDay = param.getEndDay();
        // 调整结束日期
        Integer adjustEndDay = param.getAdjustEndDay() == null ? endDay : param.getAdjustEndDay();
        // 排序（按开始日期升序，开始日期相同则按结束日期升序排序）
        structureAllocationList.sort(Comparator.comparing(MpStructureAllocation::getBeginDay,
                Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MpStructureAllocation::getEndDay,
                        Comparator.nullsLast(Integer::compareTo)));
        // 从集合中找出日期最接近目标开始日期和结束日期的数据
        MpStructureAllocation structureAllocation = getClosestStructureAllocation(structureAllocationList, param.getId(), beginDay, endDay);
        return structureAllocation;
    }


    /**
     * 从集合中找出日期最接近目标开始日期和结束日期的数据
     * @param list
     * @param excludeId
     * @param targetBeginDay
     * @param targetEndDay
     * @return
     */
    private MpStructureAllocation getClosestStructureAllocation(List<MpStructureAllocation> list,
                                                                         Long excludeId, Integer targetBeginDay,
                                                                         Integer targetEndDay) {
        if (PubUtil.isEmpty(list) || targetBeginDay == null || targetEndDay == null || excludeId == null) {
            return null;
        }
        Optional<MpStructureAllocation> result = list.stream()
                .filter(e -> !excludeId.equals(e.getId()))
                .filter(e -> isNextElement(e, targetBeginDay, targetEndDay))
                // 按日期接近度排序（差值绝对值之和越小越接近）
                .min((a1, a2) -> {
                    int distance1 = calculateDistance(a1.getBeginDay(), a1.getEndDay(),
                            targetBeginDay, targetEndDay);
                    int distance2 = calculateDistance(a2.getBeginDay(), a2.getEndDay(),
                            targetBeginDay, targetEndDay);
                    return Integer.compare(distance1, distance2);
                });
        log.info("获取日期最接近的下一个结构 ==> 目标id[{}] 目标开始时间[{}] 目标结束时间[{}] 结构[{}]", excludeId, targetBeginDay, targetEndDay,
                JSONObject.toJSONString(result.orElse(null)));
        return result.orElse(null);
    }

    /**
     * 计算beginDay差值绝对值 + endDay差值绝对值
     * 距离越小，说明和目标日期越接近
     */
    private int calculateDistance(Integer begin, Integer end, Integer targetBegin, Integer targetEnd) {
        int beginDiff = begin == null ? Math.abs(targetBegin) : Math.abs(begin - targetBegin);
        int endDiff = end == null ? Math.abs(targetEnd) : Math.abs(end - targetEnd);
        return beginDiff + endDiff;
    }

    /**
     * 判断当前数据是否比目标日期大
     */
    private boolean isNextElement(MpStructureAllocation allocation,
                                         Integer targetBegin, Integer targetEnd) {
        Integer begin = allocation.getBeginDay();
        Integer end = allocation.getEndDay();
        if (begin == null || end == null) {
            return Boolean.FALSE;
        }
        if (begin.equals(targetBegin) && end.equals(targetEnd)) {
            return Boolean.TRUE;
        } else if (begin > targetBegin) {
            return Boolean.TRUE;
        } else if (begin.equals(targetBegin)) {
            return end > targetEnd;
        } else {
            return  Boolean.FALSE;
        }
    }






}
