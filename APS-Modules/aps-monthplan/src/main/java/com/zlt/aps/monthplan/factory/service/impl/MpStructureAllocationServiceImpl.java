package com.zlt.aps.monthplan.factory.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.SpringContextSupplierUtil;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmStructureLhRatioEntityMapper;
import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

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
    private final MdmStructureLhRatioEntityMapper mdmStructureLhRatioEntityMapper;
    private final MdmMonCycleSchStruConfEntityMapper mdmMonCycleSchStruConfEntityMapper;
    private final MdmCycleSchStruConfEntityMapper mdmCycleSchStruConfEntityMapper;
    private final FactoryParamMapper factoryParamMapper;

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
        // 唯一性校验
        this.checkUnique(mpStructureAllocation);

        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 创建查询数据的异步任务
        // 查询月度生产计划
        CompletableFuture<List<FactoryMonthPlanProductionFinalResult>> monthPlanFinalResultFuture = CompletableFuture.supplyAsync(
                // 解决父子上下文传递问题
                SpringContextSupplierUtil.wrap(() -> queryMonthPlanFinalResult(mpStructureAllocation))
        );
        // 查询排产结构
        CompletableFuture<List<MpStructureAllocation>> structureAllocationFuture = CompletableFuture.supplyAsync(
                () -> queryMpStructureAllocation(mpStructureAllocation)
        );
        // 查询成型硫化结构配比
        CompletableFuture<List<MdmStructureLhRatio>> structureLhRatioFuture = CompletableFuture.supplyAsync(
                () -> queryMdmStructureLhRatio(mpStructureAllocation)
        );
        // 查询月周期排产结构配置
        CompletableFuture<List<MdmMonCycleSchStruConf>> monCycleSchStruConfFuture = CompletableFuture.supplyAsync(
                () -> queryMdmMonCycleSchStruConf(mpStructureAllocation)
        );
        // 查询周期排产结构配置
        CompletableFuture<List<MdmCycleSchStruConf>> cycleSchStruConfFuture = CompletableFuture.supplyAsync(
                () -> queryMdmCycleSchStruConf(mpStructureAllocation)
        );
        // 查询工厂排产设定
        CompletableFuture<List<FactoryParam>> factoryParamFuture = CompletableFuture.supplyAsync(
                () -> queryFactoryParam(mpStructureAllocation)
        );

        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    monthPlanFinalResultFuture,
                    structureAllocationFuture,
                    structureLhRatioFuture,
                    monCycleSchStruConfFuture,
                    cycleSchStruConfFuture,
                    factoryParamFuture
            ).join();

            log.info("并行查询数据执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("查询数据失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
        }

        log.info("初始化任务执行完成 ==> 耗时:{} ms", watch.getLastTaskTimeMillis());

        List<FactoryMonthPlanProductionFinalResult> monthPlanProductionFinalResultList = monthPlanFinalResultFuture.join();
        List<MpStructureAllocation> structureAllocationList = structureAllocationFuture.join();
        List<MdmStructureLhRatio> structureLhRatioList = structureLhRatioFuture.join();
        List<MdmMonCycleSchStruConf> monCycleSchStruConfList = monCycleSchStruConfFuture.join();
        List<MdmCycleSchStruConf> cycleSchStruConfList = cycleSchStruConfFuture.join();
        List<FactoryParam> factoryParamList = factoryParamFuture.join();

        // 判断时间是否有交叉，若有则抛出异常
        List<String> dateCrossedErrorMsgList = getDateCrossedErrorMsgList(mpStructureAllocation, structureAllocationList);
        if (PubUtil.isNotEmpty(dateCrossedErrorMsgList)) {
            throw new BusinessException(String.join("</br>", dateCrossedErrorMsgList));
        }

        // 设置需求计划版本、排产版本号
        if (PubUtil.isNotEmpty(monthPlanProductionFinalResultList)) {
            FactoryMonthPlanProductionFinalResult monthPlanProductionFinalResult = monthPlanProductionFinalResultList.get(0);
            mpStructureAllocation.setMonthPlanVersion(monthPlanProductionFinalResult.getMonthPlanVersion());
            mpStructureAllocation.setProductionVersion(monthPlanProductionFinalResult.getProductionVersion());
        }

        // 设置最大胎胚种类数、最大硫化机台数
        if (PubUtil.isNotEmpty(structureLhRatioList)) {
            MdmStructureLhRatio mdmStructureLhRatio = structureLhRatioList.get(0);
            mpStructureAllocation.setMaxEmbryoCodeCount(mdmStructureLhRatio.getMaxEmbryoQty());
            mpStructureAllocation.setMaxLhMachineCount(mdmStructureLhRatio.getLhMachineMaxQty());
        }

        // 产品结构
        String structureName = mpStructureAllocation.getStructureName();
        // 开始日期
        Integer beginDay = mpStructureAllocation.getBeginDay();
        // 结束日期
        Integer endDay = mpStructureAllocation.getEndDay();

        // 设置实单最低硫化机台数
        Integer minLhMachineCount = 0;
        MdmMonCycleSchStruConf monCycleSchStruConf = monCycleSchStruConfList.stream()
                .filter(v -> StringUtils.equals(structureName, v.getStructureName()))
                .findFirst()
                .orElse(new MdmMonCycleSchStruConf());
        minLhMachineCount = monCycleSchStruConf.getMinVulcanizingMachine();

        if (minLhMachineCount == null) {
            MdmCycleSchStruConf cycleSchStruConf = cycleSchStruConfList.stream()
                    .filter(v -> StringUtils.equals(structureName, v.getStructureName()))
                    .findFirst()
                    .orElse(new MdmCycleSchStruConf());
            minLhMachineCount = cycleSchStruConf.getMinVulcanizingMachine();
        }

        if (minLhMachineCount == null) {
            FactoryParam factoryParam = factoryParamList.stream()
                    .filter(v -> StringUtils.equals(MonthPlanEnums.NO_CYCLE_PRODUCTION_MIN_LH_MACHINE_NUMBER.getCode(), v.getParamCode()))
                    .findFirst()
                    .orElse(new FactoryParam());
            minLhMachineCount = Convert.toInt(factoryParam.getParamValue(), 0);
        }
        // 实单最低硫化机台数
        mpStructureAllocation.setMinLhMachineCount(minLhMachineCount);
        mpStructureAllocation.setBaseVale(null);
        // 计划类型
        mpStructureAllocation.setPlanType("01");
        // 分配天数 TODO 后续再扣除停工的天数
        mpStructureAllocation.setAllotDays(endDay - beginDay + 1);
        // 排产净需求
        mpStructureAllocation.setNetQty(0);
        // 排产净需求(含损耗)
        mpStructureAllocation.setLossQty(0);
        // 数据来源
        mpStructureAllocation.setDataSource(DataSourceEnum.HAND.getCode());
        return baseDao.save(mpStructureAllocation);
    }



    /**
     * 查询月度生产计划
     *
     * @param mpStructureAllocation
     */
    private List<FactoryMonthPlanProductionFinalResult> queryMonthPlanFinalResult(MpStructureAllocation mpStructureAllocation) {
        FactoryMonthPlanProductionFinalResult param = new FactoryMonthPlanProductionFinalResult();
        param.setFactoryCode(mpStructureAllocation.getFactoryCode());
        param.setYear(mpStructureAllocation.getYear());
        param.setMonth(mpStructureAllocation.getMonth());
        return monthPlanProductionFinalResultService.listMonthProdFinalPlans(param);
    }


    /**
     * 查询排产结构
     *
     * @param mpStructureAllocation
     */
    private List<MpStructureAllocation> queryMpStructureAllocation(MpStructureAllocation mpStructureAllocation) {
        MpStructureAllocation queryParam = new MpStructureAllocation();
        queryParam.setFactoryCode(mpStructureAllocation.getFactoryCode());
        queryParam.setYear(mpStructureAllocation.getYear());
        queryParam.setMonth(mpStructureAllocation.getMonth());
        queryParam.setProductionVersion(mpStructureAllocation.getProductionVersion());
        return getDataList(queryParam);
    }

    /**
     * 查询成型硫化结构配比
     *
     * @param mpStructureAllocation
     */
    private List<MdmStructureLhRatio> queryMdmStructureLhRatio(MpStructureAllocation mpStructureAllocation) {
        LambdaQueryWrapper<MdmStructureLhRatio> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmStructureLhRatio::getFactoryCode, mpStructureAllocation.getFactoryCode())
                .eq(MdmStructureLhRatio::getStructureName, mpStructureAllocation.getStructureName());
        return mdmStructureLhRatioEntityMapper.selectList(queryWrapper);
    }


    /**
     * 查询月周期排产结构配置
     *
     * @param mpStructureAllocation
     */
    private List<MdmMonCycleSchStruConf> queryMdmMonCycleSchStruConf(MpStructureAllocation mpStructureAllocation) {
        MdmMonCycleSchStruConf queryVO = new MdmMonCycleSchStruConf();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());
        queryVO.setYear(mpStructureAllocation.getYear());
        queryVO.setMonth(mpStructureAllocation.getMonth());

        LambdaQueryWrapper<MdmMonCycleSchStruConf> queryWrapper = new LambdaQueryWrapper<>();
        buildMdmMonCycleSchStruConfCondition(queryWrapper, queryVO);
        return mdmMonCycleSchStruConfEntityMapper.selectList(queryWrapper);
    }

    /**
     * 构建月周期排产结构配置条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMdmMonCycleSchStruConfCondition(LambdaQueryWrapper<MdmMonCycleSchStruConf> queryWrapper, MdmMonCycleSchStruConf queryVO) {
        queryWrapper.eq(MdmMonCycleSchStruConf::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMonCycleSchStruConf::getYear, queryVO.getYear());
        queryWrapper.eq(MdmMonCycleSchStruConf::getMonth, queryVO.getMonth());
        queryWrapper.eq(MdmMonCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 查询周期排产结构配置
     *
     * @param mpStructureAllocation
     */
    private List<MdmCycleSchStruConf> queryMdmCycleSchStruConf(MpStructureAllocation mpStructureAllocation) {
        MdmCycleSchStruConf queryVO = new MdmCycleSchStruConf();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());
        queryVO.setYear(mpStructureAllocation.getYear());
        queryVO.setMonth(mpStructureAllocation.getMonth());

        LambdaQueryWrapper<MdmCycleSchStruConf> queryWrapper = new LambdaQueryWrapper<>();
        buildMdmCycleSchStruConfCondition(queryWrapper, queryVO);
        return mdmCycleSchStruConfEntityMapper.selectList(queryWrapper);
    }

    /**
     * 构建周期排产结构配置条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMdmCycleSchStruConfCondition(LambdaQueryWrapper<MdmCycleSchStruConf> queryWrapper, MdmCycleSchStruConf queryVO) {
        queryWrapper.eq(MdmCycleSchStruConf::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmCycleSchStruConf::getYear, queryVO.getYear());
        queryWrapper.eq(MdmCycleSchStruConf::getMonth, queryVO.getMonth());
        queryWrapper.eq(MdmCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 查询工厂排产设定
     *
     * @param mpStructureAllocation
     */
    private List<FactoryParam> queryFactoryParam(MpStructureAllocation mpStructureAllocation) {
        FactoryParam queryVO = new FactoryParam();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());

        LambdaQueryWrapper<FactoryParam> queryWrapper = new LambdaQueryWrapper<>();
        buildFactoryParamCondition(queryWrapper, queryVO);
        return factoryParamMapper.selectList(queryWrapper);
    }

    /**
     * 构建工厂排产设定条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildFactoryParamCondition(LambdaQueryWrapper<FactoryParam> queryWrapper, FactoryParam queryVO) {
        queryWrapper.eq(FactoryParam::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(FactoryParam::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 判断开始时间和结束时间是否有交叉
     * @param targetAlloc
     * @param structureAllocationList
     * @return
     */
    private List<String> getDateCrossedErrorMsgList(MpStructureAllocation targetAlloc,
                                                        List<MpStructureAllocation> structureAllocationList) {
        if (PubUtil.isEmpty(structureAllocationList)) {
            return Collections.emptyList();
        }
        if (Objects.isNull(targetAlloc.getBeginDay()) || Objects.isNull(targetAlloc.getEndDay())
                || Objects.isNull(targetAlloc.getStructureName())
                || targetAlloc.getBeginDay() > targetAlloc.getEndDay()) {
            return Collections.emptyList();
        }
        String targetMachineCode = targetAlloc.getCxMachineCode();
        // 错误信息
        List<String> errorList = new ArrayList<>();
        // 过滤同机台结构
        List<MpStructureAllocation> sameMachineList = structureAllocationList.stream()
                .filter(alloc ->
                        targetMachineCode.equals(alloc.getCxMachineCode())
                        && Objects.nonNull(alloc.getStructureName())
                        && Objects.nonNull(alloc.getBeginDay())
                        && Objects.nonNull(alloc.getEndDay())
                        && alloc.getBeginDay() <= alloc.getEndDay())
                .collect(Collectors.toList());

        // 遍历集合，判断交叉
        for (MpStructureAllocation alloc : sameMachineList) {
            if (targetAlloc.getStructureName().equals(alloc.getStructureName())) {
                continue;
            }
            if (isTimeCrossed(targetAlloc, alloc)) {
                String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpStructureAllocation.dateCrossed"),
                        targetAlloc.getStructureName(), alloc.getStructureName());
                errorList.add(errorMsg);
            }
        }
        return errorList;
    }

    /**
     * 判断时间是否有交叉
     * @param
     * @param
     * @return
     */
    private boolean isTimeCrossed(MpStructureAllocation a, MpStructureAllocation b) {
        int aBegin = a.getBeginDay();
        int aEnd = a.getEndDay();
        int bBegin = b.getBeginDay();
        int bEnd = b.getEndDay();
        return !(aEnd < bBegin || bEnd < aBegin);
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
        if (structureAllocation != null) {
            int day = DateUtil.lengthOfMonth(param.getMonth(), DateUtil.isLeapYear(param.getYear()));
            structureAllocation.setAdjustStartDay(Math.min(adjustEndDay + 1, day));
        }
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
