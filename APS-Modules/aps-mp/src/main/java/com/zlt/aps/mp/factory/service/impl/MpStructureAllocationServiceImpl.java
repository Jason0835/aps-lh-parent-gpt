package com.zlt.aps.mp.factory.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmStructureLhRatioEntityMapper;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.mp.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.enums.StructureAllocationExportDataTypeEnum;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportChangeCountVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportStatisticsVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.autoconfigure.metrics.startup.StartupTimeMetricsListenerAutoConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
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
    private final RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;
    private final MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;
    private final MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;
    private final MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;
    private final MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;
    private final MpMonthPlanStatisticsEntityMapper mpMonthPlanStatisticsEntityMapper;
    private final ISysDictDataCacheService sysDictDataCacheService;
    /**
     * 月份天数上限
     */
    private final static int MAX_MONTH_DAY = 31;
    /**
     * 日计划字段名称
     */
    private final static String DAY_FIELD_NAME_FORMAT = "day%s";


    @Override
    public List<MpStructureAllocation> getDataList(MpStructureAllocation param) {
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, param);
        queryWrapper.orderByAsc("CX_MACHINE_CODE");
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
        return Arrays.asList("factoryCode","year","month","structureName", "productionVersion", "cxMachineCode");
    }


    @Override
    public int save(MpStructureAllocation mpStructureAllocation) {
        // 查询月度生产计划
        List<FactoryMonthPlanProductionFinalResult> monthPlanProductionFinalResultList = queryMonthPlanFinalResult(mpStructureAllocation);
        if (PubUtil.isEmpty(monthPlanProductionFinalResultList)) {
            String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpStructureAllocation.notFinalMonthPlan"),
                    String.format("%d%02d", mpStructureAllocation.getYear(), mpStructureAllocation.getMonth()));
            throw new BusinessException(errorMsg);
        }
        // 设置需求计划版本、排产版本号
        if (PubUtil.isNotEmpty(monthPlanProductionFinalResultList)) {
            FactoryMonthPlanProductionFinalResult monthPlanProductionFinalResult = monthPlanProductionFinalResultList.get(0);
            mpStructureAllocation.setMonthPlanVersion(monthPlanProductionFinalResult.getMonthPlanVersion());
            mpStructureAllocation.setProductionVersion(monthPlanProductionFinalResult.getProductionVersion());
        }

        // 唯一性校验
        String unique = super.checkUnique(mpStructureAllocation);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            String errorMsg = StrUtil.format(I18nUtil.getMessage("ui.data.alert.mpStructureAllocation.uniqueCheck"),
                    mpStructureAllocation.getCxMachineCode(), mpStructureAllocation.getStructureName());
            throw new ServiceException(errorMsg);
        }

        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 创建查询数据的异步任务
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
        // 查询BOM物料消耗明细
        CompletableFuture<List<MdmMaterialConsumeDetail>> materialConsumeDetailFuture = CompletableFuture.supplyAsync(
                () -> queryMaterialConsumeDetailList(mpStructureAllocation)
        );
        // 查询特殊材料记录
        CompletableFuture<List<RawSpecialMaterialRecord>> rawSpecialMaterialRecordFuture = CompletableFuture.supplyAsync(
                () -> querySpecialMaterialRecordList(mpStructureAllocation)
        );
        // 查询sku与结构关系
        CompletableFuture<List<MdmSkuStructureRef>> skuStructureRefFuture = CompletableFuture.supplyAsync(
                () -> querySkuStructureRef(mpStructureAllocation)
        );

        // 查询SKU与施工（示方书）关系
        CompletableFuture<List<MdmSkuConstructionRef>> skuConstructionRefFuture = CompletableFuture.supplyAsync(
                () -> querySkuConstructionRef(mpStructureAllocation)
        );
        // 查询工作日历
        CompletableFuture<List<MdmWorkCalendar>> workCalendarFuture = CompletableFuture.supplyAsync(
                () -> queryMdmWorkCalendar(mpStructureAllocation)
        );


        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    structureAllocationFuture,
                    structureLhRatioFuture,
                    monCycleSchStruConfFuture,
                    cycleSchStruConfFuture,
                    factoryParamFuture,
                    materialConsumeDetailFuture,
                    rawSpecialMaterialRecordFuture,
                    skuStructureRefFuture,
                    skuConstructionRefFuture,
                    workCalendarFuture
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

        List<MpStructureAllocation> structureAllocationList = structureAllocationFuture.join();
        List<MdmStructureLhRatio> structureLhRatioList = structureLhRatioFuture.join();
        List<MdmMonCycleSchStruConf> monCycleSchStruConfList = monCycleSchStruConfFuture.join();
        List<MdmCycleSchStruConf> cycleSchStruConfList = cycleSchStruConfFuture.join();
        List<FactoryParam> factoryParamList = factoryParamFuture.join();
        List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList = materialConsumeDetailFuture.join();
        List<RawSpecialMaterialRecord> specialMaterialList = rawSpecialMaterialRecordFuture.join();
        List<MdmSkuStructureRef> skuStructureRefList = skuStructureRefFuture.join();
        List<MdmSkuConstructionRef> skuConstructionRefList= skuConstructionRefFuture.join();
        List<MdmWorkCalendar> workCalendarList = workCalendarFuture.join();


        // 判断时间是否有交叉，若有则抛出异常
        List<String> dateCrossedErrorMsgList = getDateCrossedErrorMsgList(mpStructureAllocation, structureAllocationList);
        if (PubUtil.isNotEmpty(dateCrossedErrorMsgList)) {
            throw new BusinessException(String.join("</br>", dateCrossedErrorMsgList));
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
        // 筛选工作日历数据(停产)
        List<MdmWorkCalendar> workCalendarResultList = filterWorkCalendar(workCalendarList, beginDay, endDay);
        // 分配天数 = 结束日期减去开始日期加1减去停产的天数
        mpStructureAllocation.setAllotDays(endDay - beginDay + 1 - workCalendarResultList.size());
        // 排产净需求
        mpStructureAllocation.setNetQty(0);
        // 排产净需求(含损耗)
        mpStructureAllocation.setLossQty(0);
        // 数据来源
        mpStructureAllocation.setDataSource(DataSourceEnum.HAND.getCode());
        // 设置是否含有特殊材料
        String materialCode = skuStructureRefList.stream()
                .filter(vo -> vo.getStructureName().equals(mpStructureAllocation.getStructureName()))
                .findFirst()
                .map(MdmSkuStructureRef::getMaterialCode)
                .orElse(null);
        String embryoCode = skuConstructionRefList.stream()
                .filter(vo -> vo.getMaterialCode().equals(materialCode))
                .findFirst()
                .map(MdmSkuConstructionRef::getEmbryoCode)
                .orElse(null);
        boolean isHasSpecialMaterial = hasSpecialMaterial(embryoCode, mdmMaterialConsumeDetailList, specialMaterialList);
        mpStructureAllocation.setIsHasSpecialMaterial(isHasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);
        return baseDao.save(mpStructureAllocation);
    }


    /**
     * 筛选工作日历数据(停产)
     *
     * @param workCalendarList 原始日历列表
     * @param beginDay         起始天（包含）
     * @param endDay           结束天（包含）
     * @return 符合条件的日历列表
     */
    public List<MdmWorkCalendar> filterWorkCalendar(List<MdmWorkCalendar> workCalendarList, Integer beginDay, Integer endDay) {
        if (PubUtil.isEmpty(workCalendarList)) {
            return Collections.emptyList();
        }
        if (beginDay == null || endDay == null || beginDay > endDay) {
            return Collections.emptyList();
        }
        return workCalendarList.stream()
                .filter(calendar -> {
                    Integer day = calendar.getDay();
                    return day != null && day >= beginDay && day <= endDay;
                })
                .filter(calendar -> ApsConstant.FALSE.equals(calendar.getDayFlag()))
                .collect(Collectors.toList());
    }

    /**
     * 查询SKU与施工（示方书）关系
     *
     * @param mpStructureAllocation
     */
    private List<MdmSkuConstructionRef> querySkuConstructionRef(MpStructureAllocation mpStructureAllocation) {
        MdmSkuConstructionRef queryVO = new MdmSkuConstructionRef();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());

        LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper = new LambdaQueryWrapper<>();
        buildSkuConstructionRefCondition(queryWrapper, queryVO);
        return mdmSkuConstructionRefEntityMapper.selectList(queryWrapper);
    }

    /**
     * 构建SKU与施工（示方书）关系条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSkuConstructionRefCondition(LambdaQueryWrapper<MdmSkuConstructionRef> queryWrapper, MdmSkuConstructionRef queryVO) {
        queryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuConstructionRef::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 判断是否特殊材料
     *
     * @param targetEmbryoCode             目标胚胎编码
     * @param mdmMaterialConsumeDetailList BOM物料消耗明细列表
     * @param specialMaterialList          特殊材料清单列表
     * @return
     */
    protected boolean hasSpecialMaterial(String targetEmbryoCode, List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList,
                                         List<RawSpecialMaterialRecord> specialMaterialList) {

        if (StringUtils.isEmpty(targetEmbryoCode) || PubUtil.isEmpty(mdmMaterialConsumeDetailList)
                || PubUtil.isEmpty(specialMaterialList)) {
            return Boolean.FALSE;
        }

        // 从BOM物料消耗明细列表中通过胎胚代码筛选出匹配的所有数据
        Set<String> childMaterialCodes = mdmMaterialConsumeDetailList.stream()
                .filter(detail -> StringUtils.equals(targetEmbryoCode, detail.getEmbryoCode()))
                .map(MdmMaterialConsumeDetail::getChildMaterialCode)
                .collect(Collectors.toSet());

        // 如果没有匹配到直接返回false
        if (PubUtil.isEmpty(childMaterialCodes)) {
            return Boolean.FALSE;
        }

        // 检查特殊材料清单列表中是否存在匹配的数据
        return specialMaterialList.stream()
                .map(RawSpecialMaterialRecord::getMaterialCode)
                .anyMatch(childMaterialCodes::contains);
    }

    /**
     * 查询sku与结构关系
     *
     * @param mpStructureAllocation
     */
    private List<MdmSkuStructureRef> querySkuStructureRef(MpStructureAllocation mpStructureAllocation) {
        MdmSkuStructureRef queryVO = new MdmSkuStructureRef();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());

        LambdaQueryWrapper<MdmSkuStructureRef> queryWrapper = new LambdaQueryWrapper<>();
        buildSkuStructureRefCondition(queryWrapper, queryVO);
        return mdmSkuStructureRefEntityMapper.selectList(queryWrapper);
    }

    /**
     * 构建sku与结构关系条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSkuStructureRefCondition(LambdaQueryWrapper<MdmSkuStructureRef> queryWrapper, MdmSkuStructureRef queryVO) {
        queryWrapper.eq(MdmSkuStructureRef::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmSkuStructureRef::getIsDelete, YesOrNoEnum.NO.getValue());
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
     * 查询工作日历
     *
     * @param mpStructureAllocation
     */
    private List<MdmWorkCalendar> queryMdmWorkCalendar(MpStructureAllocation mpStructureAllocation) {
        MdmWorkCalendar queryVO = new MdmWorkCalendar();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());
        queryVO.setYear(mpStructureAllocation.getYear());
        queryVO.setMonth(mpStructureAllocation.getMonth());

        LambdaQueryWrapper<MdmWorkCalendar> queryWrapper = new LambdaQueryWrapper<>();
        buildMdmWorkCalendarCondition(queryWrapper, queryVO);
        return mdmWorkCalendarEntityMapper.selectList(queryWrapper);
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
     * 构建工作日历条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMdmWorkCalendarCondition(LambdaQueryWrapper<MdmWorkCalendar> queryWrapper, MdmWorkCalendar queryVO) {
        queryWrapper.eq(MdmWorkCalendar::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmWorkCalendar::getYear, queryVO.getYear());
        queryWrapper.eq(MdmWorkCalendar::getMonth, queryVO.getMonth());
        queryWrapper.eq(MdmWorkCalendar::getIsDelete, YesOrNoEnum.NO.getValue());
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
     * 查询特殊材料记录
     *
     * @param mpStructureAllocation
     */
    private List<RawSpecialMaterialRecord> querySpecialMaterialRecordList(MpStructureAllocation mpStructureAllocation) {
        RawSpecialMaterialRecord queryVO = new RawSpecialMaterialRecord();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());

        LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper = new LambdaQueryWrapper<>();
        buildSpecialMaterialCondition(queryWrapper, queryVO);
        return rawSpecialMaterialRecordMapper.selectList(queryWrapper);
    }

    /**
     * 构建特殊材料条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildSpecialMaterialCondition(LambdaQueryWrapper<RawSpecialMaterialRecord> queryWrapper, RawSpecialMaterialRecord queryVO) {
        queryWrapper.eq(RawSpecialMaterialRecord::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(RawSpecialMaterialRecord::getIsDelete, YesOrNoEnum.NO.getValue());
    }

    /**
     * 查询BOM物料消耗明细
     *
     * @param mpStructureAllocation
     */
    private List<MdmMaterialConsumeDetail> queryMaterialConsumeDetailList(MpStructureAllocation mpStructureAllocation) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(mpStructureAllocation.getFactoryCode());

        LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new LambdaQueryWrapper<>();
        buildMaterialConsumeDetailCondition(queryWrapper, queryVO);
        return mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
    }

    /**
     * 构建BOM物料消耗明细条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void buildMaterialConsumeDetailCondition(LambdaQueryWrapper<MdmMaterialConsumeDetail> queryWrapper, MdmMaterialConsumeDetail queryVO) {
        queryWrapper.eq(MdmMaterialConsumeDetail::getFactoryCode, queryVO.getFactoryCode());
        queryWrapper.eq(MdmMaterialConsumeDetail::getIsDelete, YesOrNoEnum.NO.getValue());
    }


    /**
     * 判断开始时间和结束时间是否有交叉
     *
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


    /**
     * 获取日期最接近的上一个结构
     * @param param 目标结构参数（包含工厂、年月、成型机编码、目标开始/结束日等）
     * @return 最接近的上一个结构
     */
    @Override
    public MpStructureAllocation getPreviousStructure(MpStructureAllocation param) {
        // 通过工厂、年月、成型机编码获取结构排产列表
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        param.setStructureName(null);
        builderCondition(queryWrapper, param);
        List<MpStructureAllocation> structureAllocationList = entityMapper.selectList(queryWrapper);

        // 目标开始日期
        Integer targetBeginDay = param.getBeginDay();
        // 目标结束日期
        Integer targetEndDay = param.getEndDay();
        // 调整开始日期（为空则使用目标开始日）
        Integer adjustStartDay = param.getAdjustStartDay() == null ? targetBeginDay : param.getAdjustStartDay();

        // 排序（按开始日期升序，开始日期相同则按结束日期升序排序）
        structureAllocationList.sort(Comparator.comparing(MpStructureAllocation::getBeginDay,
                Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MpStructureAllocation::getEndDay,
                        Comparator.nullsLast(Integer::compareTo)));

        // 从集合中找出日期最接近目标开始日期和结束日期的上一个结构
        MpStructureAllocation structureAllocation = getClosestPreviousStructureAllocation(
                structureAllocationList, param.getId(), targetBeginDay, targetEndDay);

        if (structureAllocation != null) {
            // 获取当月总天数
            int monthTotalDay = DateUtil.lengthOfMonth(param.getMonth(), DateUtil.isLeapYear(param.getYear()));
            structureAllocation.setAdjustStartDay((Math.min(structureAllocation.getEndDay() + 1, monthTotalDay)));
        }
        return structureAllocation;
    }

    @Override
    public Set<String> findStructureNames(DpDemandPlan createCondition) {
        if(StringUtils.isNotBlank(createCondition.getStructureName())) {
           return Sets.newHashSet(createCondition.getStructureName());
        }
        LambdaQueryWrapper<MpStructureAllocation>  queryWrapper = Wrappers.<MpStructureAllocation>lambdaQuery()
            .eq(MpStructureAllocation::getFactoryCode, createCondition.getFactoryCode())
            .eq(MpStructureAllocation::getYear, createCondition.getYear())
            .eq(MpStructureAllocation::getMonth, createCondition.getMonth())
            .eq(MpStructureAllocation::getMonthPlanVersion, createCondition.getMonthPlanVersion())
            .eq(MpStructureAllocation::getProductionVersion, createCondition.getProductionVersion())
            .eq(MpStructureAllocation::getIsDelete,YesOrNoEnum.NO.getCode());
        List<MpStructureAllocation> list = this.entityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(list)) {
            return Collections.emptySet();
        }
        return  list.stream().map(MpStructureAllocation::getStructureName).collect(Collectors.toSet());
    }
    

    /**
     * 获取结构转产表导出数据
     * @param param
     * @return
     */
    @Override
    public MpStructureAllocationExportStatisticsVo getExportVo(MpStructureAllocation param) {
        // 1、加载构建导出列表的各项数据
        // 1.1、加载月计划模具排产明细
        List<MpStructureAllocationExportVo> recordList = entityMapper.getExportList(param);
        // 1.2、加载本次版本已生成的统计记录
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, param.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, param.getProductionVersion());
        Map<String, MpMonthPlanStatistics> statisticsMap = mpMonthPlanStatisticsEntityMapper.selectList(queryWrapper)
                .stream().collect(
                        Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));
        Map<String, Map<Integer, Integer>> lhMachineStatisticsMap = new HashMap<>();
        for (Entry<String, MpMonthPlanStatistics> entry: statisticsMap.entrySet()) {
            Map<Integer, Integer> dayLhMachinesMap = new HashMap<>();
            MpMonthPlanStatistics statistics = entry.getValue();
            for (int day = 1; day <= MAX_MONTH_DAY; day ++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                String dayStatisticsStr = (String)statistics.getFieldValueByFieldName(dayFieldName);
                if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                    MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
                    dayLhMachinesMap.put(day, dayStatistics.getLhMachines());
                }
            }
            lhMachineStatisticsMap.put(entry.getKey(), dayLhMachinesMap);
        }
        // 获取最大硫化机数
        Integer lhmachineCount = 176; // TODO 确认从什么地方获取后再改

        // 2、构建导出数据
        MpStructureAllocationExportStatisticsVo exportVo = new MpStructureAllocationExportStatisticsVo();
        // 2、构建报表头
        exportVo.setFactoryCode(param.getFactoryCode());
        exportVo.setYear(param.getYear());
        exportVo.setMonth(param.getMonth());
        exportVo.setMonthPlanVersion(param.getMonthPlanVersion());
        exportVo.setProductionVersion(param.getProductionVersion());
        exportVo.setStructureChangeCount(0);
        exportVo.setProSizeChangeCount(0);
        if (PubUtil.isNotEmpty(recordList)) {
            MpStructureAllocationExportVo firstRecotd = recordList.get(0);
            exportVo.setProductTypeCode(firstRecotd.getProductTypeCode());
        }
        
        // 3、构建导出总表
        List<MpStructureAllocationExportVo> totalRecordList = new LinkedList<>(); // 导出数据总表
        // 3、构建统计行
        // 3.1、排产合计
        MpStructureAllocationExportVo totalRecord = new MpStructureAllocationExportVo();
        totalRecord.setStructureName(I18nUtil.getMessage(StructureAllocationExportDataTypeEnum.TOTAL.getName()));
        totalRecord.setDataType(StructureAllocationExportDataTypeEnum.TOTAL.getCode());
        // 3.2、最大产能
        MpStructureAllocationExportVo maxProductQtyRecord = new MpStructureAllocationExportVo();
        maxProductQtyRecord.setStructureName(I18nUtil.getMessage(StructureAllocationExportDataTypeEnum.MAX_PRODUCT_QTY.getName()));
        maxProductQtyRecord.setDataType(StructureAllocationExportDataTypeEnum.MAX_PRODUCT_QTY.getCode());
        // 3.3、可用台数
        MpStructureAllocationExportVo enableCountRecord = new MpStructureAllocationExportVo();
        enableCountRecord.setStructureName(I18nUtil.getMessage(StructureAllocationExportDataTypeEnum.ENABLE_COUNT.getName()));
        enableCountRecord.setDataType(StructureAllocationExportDataTypeEnum.ENABLE_COUNT.getCode());
        
        // 3.4、构建主题表格
        String cxMachineCode = null; // 当前结构名称
        List<MpStructureAllocationExportVo> machineStructureList = new ArrayList<>(); // 机台排产记录列表
        Map<Integer, Integer> totalMap = new HashMap<>(); // 汇总map，用于记录每天的机台合计值
        for (Integer i = 0, size = recordList.size(); i < size; i ++) {
            // 3.4.1、把同结构的排产记录添加到列表中，全部添加完后开始处理这一批数据
            MpStructureAllocationExportVo record = recordList.get(i);
            machineStructureList.add(record); // 先添加到列表
            cxMachineCode = record.getStructureName(); // 更新结构
            // 3.4.2、下一笔结构没有变化，且还不是最后一笔记录，继续遍历下一笔数据
            if (i < size - 1) { // 还不是最后一行，则校验下一行是否同一个结构
                MpStructureAllocationExportVo nextRecord = recordList.get(i + 1);
                if (cxMachineCode.equals(nextRecord.getCxMachineCode())) { // 结构没有变化，则添继续往下
                    continue;
                }
            }
            Integer changeRank = 1;
            for (MpStructureAllocationExportVo machineRecord: machineStructureList) {
                Map<Integer, Integer> dayLhMachinesMap = lhMachineStatisticsMap.get(machineRecord.getStructureName());
                if (dayLhMachinesMap == null) {
                    continue;
                }
                for (int day = 1; day <= MAX_MONTH_DAY; day ++) {
                    Integer lhMachines = dayLhMachinesMap.getOrDefault(day, 0);
                    if (lhMachines!= null && lhMachines > 0) {
                        Integer realLhMachines = Math.min(machineRecord.getMaxLhMachineCount(), lhMachines);
                        String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                        dayLhMachinesMap.put(day, lhMachines - realLhMachines);
                        this.updateExportDayField(machineRecord, dayFieldName, realLhMachines); // 更新明细
                        totalMap.put(day, totalMap.getOrDefault(day, 0) + realLhMachines); // 更新汇总map
                    }
                }
                machineRecord.setChangeRank(changeRank ++); // 设置序号
            }
            totalRecordList.addAll(machineStructureList);
            machineStructureList.clear();
        }
        // 3.5、更新统计行数值
        for (Entry<Integer, Integer> entry: totalMap.entrySet()) {
            Integer day = entry.getKey();
            Integer realLhMachines = entry.getValue();
            String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
            this.updateExportDayField(totalRecord, dayFieldName, realLhMachines); // 累加记录
            this.updateExportDayField(maxProductQtyRecord, dayFieldName, lhmachineCount); // 填充最大产能数值 = 硫化机总数
            this.updateExportDayField(enableCountRecord, dayFieldName, lhmachineCount - realLhMachines); // 可用机台数 = 排产合计 - 最大产能
        }
        totalRecordList.add(totalRecord);
        totalRecordList.add(maxProductQtyRecord);
        totalRecordList.add(enableCountRecord);
        exportVo.setRecordList(totalRecordList);

        // 4、构建切换数子表
        Map<String, List<MpStructureAllocationExportVo>> cxMachineExportMap = recordList.stream()
                .collect(Collectors.groupingBy(MpStructureAllocationExportVo::getCxMachineCode)); // 按机台分好组
        Map<Integer, Integer> changeStructureCountMap = new HashMap<>(); // 记录统计的规格切换次数，key切换次数，value该切换次数的机台数
        // 4.1、遍历每个机台的结构排产记录，统计相关数据
        for (List<MpStructureAllocationExportVo> cxMachineExportList: cxMachineExportMap.values()) {
            // 4.1.1、统计结构切换次数
            Long changeStructureCount = cxMachineExportList.stream()
                    .map(MpStructureAllocationExportVo::getStructureName).distinct().count() - 1;
            if (changeStructureCount > 0) {
                Integer oldCount = changeStructureCountMap.getOrDefault(changeStructureCount, 0);
                changeStructureCountMap.put(changeStructureCount.intValue(), oldCount + 1);
            }
            // 4.1.2、统计英寸交替次数
            Long changeProSize = cxMachineExportList.stream().map(MpStructureAllocationExportVo::getProSize).distinct()
                    .count() - 1;
            if (changeProSize > 0) {
                Integer oldValue = Optional.ofNullable(exportVo.getProSizeChangeCount()).orElse(0);
                exportVo.setProSizeChangeCount(oldValue + changeProSize.intValue());
            }
        }
        // 4.2、统计的规格切换次数转换成表格
        List<MpStructureAllocationExportChangeCountVo> changeCountList = new LinkedList<>();
        changeStructureCountMap.keySet().stream().sorted(Integer::compareTo).forEach(changeCount -> {
            Integer machineCount = changeStructureCountMap.get(changeCount);
            MpStructureAllocationExportChangeCountVo changeCountRecord = new MpStructureAllocationExportChangeCountVo(
                    changeCount, machineCount, StructureAllocationExportDataTypeEnum.RECORD.getCode());
            changeCountList.add(changeCountRecord);
        });
        // 4.3、构建切换次数统计行
        Integer totalChangeCount = changeCountList.stream().mapToInt(r -> r.getChangeCount() * r.getMachineCount()).sum();
        MpStructureAllocationExportChangeCountVo totalChangeCountRecord = new MpStructureAllocationExportChangeCountVo(
                0, totalChangeCount, StructureAllocationExportDataTypeEnum.TOTAL_CHANGE_COUNT.getCode());
        changeCountList.add(totalChangeCountRecord);
        exportVo.setChangeCountList(changeCountList);
        exportVo.setStructureChangeCount(totalChangeCount); // 更新结构切换
        
        return exportVo;
    }
    
    /**
     * 更新导出数据的日数据
     * 
     * @param exportVo     导出记录
     * @param dayFieldName 日数据字段
     * @param value        更新值
     */
    private void updateExportDayField(MpStructureAllocationExportVo exportVo, String dayFieldName, Integer value) {
        if (value == null || value <= 0) {
            return;
        }
        Integer newValue = value;
        exportVo.setFieldValueByFieldName(dayFieldName, newValue);
    }


    /**
     * 从集合中找出日期最接近目标开始/结束日的上一个结构
     *
     * @param list           结构排产列表
     * @param excludeId      排除的目标结构ID
     * @param targetBeginDay 目标开始日
     * @param targetEndDay   目标结束日
     * @return 最接近的上一个结构
     */
    private MpStructureAllocation getClosestPreviousStructureAllocation(List<MpStructureAllocation> list,
                                                                        Long excludeId, Integer targetBeginDay,
                                                                        Integer targetEndDay) {
        if (PubUtil.isEmpty(list) || targetBeginDay == null || targetEndDay == null || excludeId == null) {
            return null;
        }

        Optional<MpStructureAllocation> result = list.stream()
                // 排除目标结构自身
                .filter(e -> !excludeId.equals(e.getId()))
                // 筛选出上一个结构（日期小于目标日期）
                .filter(e -> isPreviousElement(e, targetBeginDay, targetEndDay))
                // 按日期接近度排序（差值绝对值之和越小越接近）
                .min((a1, a2) -> {
                    int distance1 = calculateDistance(a1.getBeginDay(), a1.getEndDay(), targetBeginDay, targetEndDay);
                    int distance2 = calculateDistance(a2.getBeginDay(), a2.getEndDay(), targetBeginDay, targetEndDay);
                    return Integer.compare(distance1, distance2);
                });

        log.info("获取日期最接近的上一个结构 ==> 目标id[{}] 目标开始时间[{}] 目标结束时间[{}] 结构[{}]",
                excludeId, targetBeginDay, targetEndDay, JSONObject.toJSONString(result.orElse(null)));
        return result.orElse(null);
    }


    /**
     * 判断当前数据是否是目标日期的上一个（日期小于目标日期）
     */
    private boolean isPreviousElement(MpStructureAllocation allocation,
                                      Integer targetBegin, Integer targetEnd) {
        Integer begin = allocation.getBeginDay();
        Integer end = allocation.getEndDay();

        // 日期为空的结构直接排除
        if (begin == null || end == null) {
            return Boolean.FALSE;
        }

        // 完全相等的情况（业务上视为上一个）
        if (begin.equals(targetBegin) && end.equals(targetEnd)) {
            return Boolean.TRUE;
        }
        // 开始日小于目标开始日 → 是上一个
        else if (begin < targetBegin) {
            return Boolean.TRUE;
        }
        // 开始日相等，结束日小于目标结束日 → 是上一个
        else if (begin.equals(targetBegin)) {
            return end < targetEnd;
        }
        // 其他情况（开始日更大）→ 不是上一个
        else {
            return Boolean.FALSE;
        }
    }

    /**
     * 导出结构转产表数据
     *
     * @param list
     * @return
     */
    @Override
    public byte[] getMpStructureAllocationExportByte(MpStructureAllocationExportStatisticsVo statisticsVo) {


        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/mpStructureAllocationExportTemp.xlsx");

        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);

        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<CellStyle> cellStyleList = new ArrayList<>();

        // 加载字典数据
        // 工厂名称字典
        List<SysDictData> factoryDatas = sysDictDataCacheService.getType("biz_factory_name");
        Map<String, String> factoryMap = factoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 计划类型字典
        List<SysDictData> planTypeDatas = sysDictDataCacheService.getType("biz_plan_type");
        Map<String, String> planTypeMap = planTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 结构类型字典
        List<SysDictData> structureTypeDatas = sysDictDataCacheService.getType("structure_type");
        Map<String, String> structureTypeMap = structureTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 设备类型字典
        List<SysDictData> machineBrandDatas = sysDictDataCacheService.getType("biz_machine_brand");
        Map<String, String> machineBrandMap = machineBrandDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));


        String factoryName = factoryMap.getOrDefault(statisticsVo.getFactoryCode(), "");
        String titleFormat = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.exportTitle");
        tableMap.put("factoryName", String.format(titleFormat, factoryName, statisticsVo.getYear().toString(), statisticsVo.getMonth().toString(),""));
        tableMap.put("monthPlanVersion", statisticsVo.getMonthPlanVersion());
        tableMap.put("productionVersion", statisticsVo.getProductionVersion());
        List<Map<String, Object>> listData = new ArrayList<>();


        //主明细
        List<MpStructureAllocationExportVo> mpStructureAllocationExportVoList = statisticsVo.getRecordList();

        // 查询数据
        if (PubUtil.isNotEmpty(mpStructureAllocationExportVoList)) {
            int beginIndex = 3;
            // 记录上一个成型机编码，用于区分颜色
            String prevCxMachineCode = null;
            // 交替颜色标记，true使用颜色1，false使用颜色2
            boolean toggleColor = false;
            // 10种逐步加深的颜色，从 #fce4d6 开始逐步加深
            String[] gradientColors = {
                    "#fce4d6", "#f9d8c4", "#f6ccb2", "#f3c0a0", "#f0b48e",
                    "#eda87c", "#ea9c6a", "#e79058", "#e48446", "#e17834"
            };
            for (int i = 0; i < mpStructureAllocationExportVoList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(32);
                MpStructureAllocationExportVo exportVo = mpStructureAllocationExportVoList.get(i);
                String currentCxMachineCode = exportVo.getCxMachineCode();
                listDataMap.put("changeRank", exportVo.getChangeRank());
                listDataMap.put("factoryCode", factoryMap.getOrDefault(exportVo.getFactoryCode(), exportVo.getFactoryCode()));
                listDataMap.put("year", exportVo.getYear());
                listDataMap.put("month", exportVo.getMonth());
                listDataMap.put("structureName", exportVo.getStructureName());
                listDataMap.put("structureType", structureTypeMap.getOrDefault(exportVo.getStructureType(), exportVo.getStructureType()));
                listDataMap.put("cxMachineCode", exportVo.getCxMachineCode());
                listDataMap.put("cxMachineTypeCode", machineBrandMap.getOrDefault(exportVo.getCxMachineTypeCode(), exportVo.getCxMachineTypeCode()));
                listDataMap.put("maxEmbryoCodeCount", exportVo.getMaxEmbryoCodeCount());
                listDataMap.put("maxLhMachineCount", exportVo.getMaxLhMachineCount());
                listDataMap.put("minLhMachineCount", exportVo.getMinLhMachineCount());
                listDataMap.put("planType", planTypeMap.getOrDefault(exportVo.getPlanType(), exportVo.getPlanType()));
                listDataMap.put("netQty", exportVo.getNetQty());
                listDataMap.put("lossQty", exportVo.getLossQty());
                listDataMap.put("beginDay", exportVo.getBeginDay());
                listDataMap.put("endDay", exportVo.getEndDay());
                listDataMap.put("allotDays", exportVo.getAllotDays());
                listDataMap.put("isHasSpecialMaterial", exportVo.getIsHasSpecialMaterial());
                listDataMap.put("remark", exportVo.getRemark());
                listDataMap.put("totalQty", exportVo.getTotalQty());
                listDataMap.put("differenceQty", exportVo.getDifferenceQty());
                listDataMap.put("day1", exportVo.getDay1());
                listDataMap.put("day2", exportVo.getDay2());
                listDataMap.put("day3", exportVo.getDay3());
                listDataMap.put("day4", exportVo.getDay4());
                listDataMap.put("day5", exportVo.getDay5());
                listDataMap.put("day6", exportVo.getDay6());
                listDataMap.put("day7", exportVo.getDay7());
                listDataMap.put("day8", exportVo.getDay8());
                listDataMap.put("day9", exportVo.getDay9());
                listDataMap.put("day10", exportVo.getDay10());
                listDataMap.put("day11", exportVo.getDay11());
                listDataMap.put("day12", exportVo.getDay12());
                listDataMap.put("day13", exportVo.getDay13());
                listDataMap.put("day14", exportVo.getDay14());
                listDataMap.put("day15", exportVo.getDay15());
                listDataMap.put("day16", exportVo.getDay16());
                listDataMap.put("day17", exportVo.getDay17());
                listDataMap.put("day18", exportVo.getDay18());
                listDataMap.put("day19", exportVo.getDay19());
                listDataMap.put("day20", exportVo.getDay20());
                listDataMap.put("day21", exportVo.getDay21());
                listDataMap.put("day22", exportVo.getDay22());
                listDataMap.put("day23", exportVo.getDay23());
                listDataMap.put("day24", exportVo.getDay24());
                listDataMap.put("day25", exportVo.getDay25());
                listDataMap.put("day26", exportVo.getDay26());
                listDataMap.put("day27", exportVo.getDay27());
                listDataMap.put("day28", exportVo.getDay28());
                listDataMap.put("day29", exportVo.getDay29());
                listDataMap.put("day30", exportVo.getDay30());
                listDataMap.put("day31", exportVo.getDay31());

                // 计算day1到day31的数量合计
                Integer totalAll = 0;
                totalAll += exportVo.getDay1() != null ? exportVo.getDay1() : 0;
                totalAll += exportVo.getDay2() != null ? exportVo.getDay2() : 0;
                totalAll += exportVo.getDay3() != null ? exportVo.getDay3() : 0;
                totalAll += exportVo.getDay4() != null ? exportVo.getDay4() : 0;
                totalAll += exportVo.getDay5() != null ? exportVo.getDay5() : 0;
                totalAll += exportVo.getDay6() != null ? exportVo.getDay6() : 0;
                totalAll += exportVo.getDay7() != null ? exportVo.getDay7() : 0;
                totalAll += exportVo.getDay8() != null ? exportVo.getDay8() : 0;
                totalAll += exportVo.getDay9() != null ? exportVo.getDay9() : 0;
                totalAll += exportVo.getDay10() != null ? exportVo.getDay10() : 0;
                totalAll += exportVo.getDay11() != null ? exportVo.getDay11() : 0;
                totalAll += exportVo.getDay12() != null ? exportVo.getDay12() : 0;
                totalAll += exportVo.getDay13() != null ? exportVo.getDay13() : 0;
                totalAll += exportVo.getDay14() != null ? exportVo.getDay14() : 0;
                totalAll += exportVo.getDay15() != null ? exportVo.getDay15() : 0;
                totalAll += exportVo.getDay16() != null ? exportVo.getDay16() : 0;
                totalAll += exportVo.getDay17() != null ? exportVo.getDay17() : 0;
                totalAll += exportVo.getDay18() != null ? exportVo.getDay18() : 0;
                totalAll += exportVo.getDay19() != null ? exportVo.getDay19() : 0;
                totalAll += exportVo.getDay20() != null ? exportVo.getDay20() : 0;
                totalAll += exportVo.getDay21() != null ? exportVo.getDay21() : 0;
                totalAll += exportVo.getDay22() != null ? exportVo.getDay22() : 0;
                totalAll += exportVo.getDay23() != null ? exportVo.getDay23() : 0;
                totalAll += exportVo.getDay24() != null ? exportVo.getDay24() : 0;
                totalAll += exportVo.getDay25() != null ? exportVo.getDay25() : 0;
                totalAll += exportVo.getDay26() != null ? exportVo.getDay26() : 0;
                totalAll += exportVo.getDay27() != null ? exportVo.getDay27() : 0;
                totalAll += exportVo.getDay28() != null ? exportVo.getDay28() : 0;
                totalAll += exportVo.getDay29() != null ? exportVo.getDay29() : 0;
                totalAll += exportVo.getDay30() != null ? exportVo.getDay30() : 0;
                totalAll += exportVo.getDay31() != null ? exportVo.getDay31() : 0;
                // 处理底色：只有成型机不一样时，切换颜色区分
                // Excel行号从2开始（第1行是表头）
                int rowNum = beginIndex + i;
                if ("1".equals(exportVo.getDataType())) {
                    // 如果成型机改变，切换颜色
                    if (prevCxMachineCode == null || !prevCxMachineCode.equals(currentCxMachineCode)) {
                        toggleColor = !toggleColor;
                        prevCxMachineCode = currentCxMachineCode;
                    }
                    // 交替使用两种颜色
                    String color = toggleColor ? "#e2efda" : "#d9d9d9";

                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, 8, color, true, false, ""));

                    // 根据changeRank设置渐变颜色
                    Integer changeRank = exportVo.getChangeRank();
                    if (changeRank != null && changeRank >= 1) {
                        // 找到第一个和最后一个有值的day列
                        int firstDayWithValue = -1;
                        int lastDayWithValue = -1;
                        Integer[] days = {
                                exportVo.getDay1(), exportVo.getDay2(), exportVo.getDay3(), exportVo.getDay4(),
                                exportVo.getDay5(), exportVo.getDay6(), exportVo.getDay7(), exportVo.getDay8(),
                                exportVo.getDay9(), exportVo.getDay10(), exportVo.getDay11(), exportVo.getDay12(),
                                exportVo.getDay13(), exportVo.getDay14(), exportVo.getDay15(), exportVo.getDay16(),
                                exportVo.getDay17(), exportVo.getDay18(), exportVo.getDay19(), exportVo.getDay20(),
                                exportVo.getDay21(), exportVo.getDay22(), exportVo.getDay23(), exportVo.getDay24(),
                                exportVo.getDay25(), exportVo.getDay26(), exportVo.getDay27(), exportVo.getDay28(),
                                exportVo.getDay29(), exportVo.getDay30(), exportVo.getDay31()
                        };
                        // 查找第一个和最后一个有值的day
                        for (int d = 0; d < days.length; d++) {
                            if (days[d] != null && days[d] > 0) {
                                if (firstDayWithValue == -1) {
                                    firstDayWithValue = d;
                                }
                                lastDayWithValue = d;
                            }
                        }
                        // 如果找到有值的范围并且changeRank >= 2
                        if (firstDayWithValue != -1 && lastDayWithValue != -1 && changeRank >= 2) {
                            int colorIndex = Math.min(changeRank - 2, gradientColors.length - 1);
                            String colorSelect = gradientColors[colorIndex];
                            // 列从day1开始是第8列（索引从0开始：0~6是前面固定列，day1从第9列开始
                            int startCol = 9 + firstDayWithValue;
                            int endCol = 9 + lastDayWithValue;
                            cellStyleList.add(new CellStyle(rowNum, rowNum, startCol, endCol, colorSelect, true, false, ""));

                        }else {
                            cellStyleList.add(new CellStyle(rowNum, rowNum, 0, 39, "#e2efda", true, false, ""));
                        }

                    }else {
                        cellStyleList.add(new CellStyle(rowNum, rowNum, 0, 39, "#e2efda", true, false, ""));
                    }
                }
                if ("2".equals(exportVo.getDataType()) || "3".equals(exportVo.getDataType())|| "4".equals(exportVo.getDataType())) {
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, 39, "#e2efda", true, true, ""));
                }

                listDataMap.put("totalAll", totalAll);

                listData.add(listDataMap);
            }
            // 将处理好的数据添加到excelDataList
            excelDataList.add(listData);
        }
        //切换次数
//        List<MpStructureAllocationExportChangeCountVo> changeCountList = statisticsVo.getChangeCountList()
//                .stream()
//                .filter(x->x.getChangeCount()!=null)
//                .sorted(Comparator.comparingInt(MpStructureAllocationExportChangeCountVo::getChangeCount))
//                .collect(Collectors.toList());
        // 获取次数
        int changeAllCount = 0;
        if (PubUtil.isNotEmpty(statisticsVo.getChangeCountList())) {
            changeAllCount = statisticsVo.getChangeCountList().stream()
                    .filter(s -> StructureAllocationExportDataTypeEnum.TOTAL_CHANGE_COUNT.getCode()
                            .equals(s.getDataType()))
                    .map(MpStructureAllocationExportChangeCountVo::getMachineCount).findFirst().orElse(0);
            List<Map<String, Object>> listChangeCountData = new ArrayList<>();
            String changeMsg = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.changeCount");
            for (MpStructureAllocationExportChangeCountVo countVo : statisticsVo.getChangeCountList()) {
                if (!StructureAllocationExportDataTypeEnum.RECORD.getCode().equals(countVo.getDataType())) {
                    continue; // 只取明细数据
                }
                Map<String, Object> changeMap = new HashMap<>();
                changeMap.put("changeCount", String.format(changeMsg, countVo.getChangeCount().toString()));
                changeMap.put("machineCount", countVo.getMachineCount());
                listChangeCountData.add(changeMap);
            }
            excelDataList.add(listChangeCountData);
        }
        tableMap.put("changeAllCount", changeAllCount);


        //英寸交替
        //结构切换
        tableMap.put("proSizeChangeCount", statisticsVo.getProSizeChangeCount());
        tableMap.put("structureChangeCount", statisticsVo.getStructureChangeCount());

        // 将单元格样式放入context
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }
}
