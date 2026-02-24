package com.zlt.aps.mp.factory.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmSkuStructureRefEntityMapper;
import com.zlt.aps.maindata.mapper.MdmStructureLhRatioEntityMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.FactoryParam;
import com.zlt.aps.monthplan.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuStructureRef;
import com.zlt.aps.monthplan.api.domain.entity.MdmStructureLhRatio;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;
    private final MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;
    private final MdmSkuStructureRefEntityMapper mdmSkuStructureRefEntityMapper;
    private final MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;

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
                    skuConstructionRefFuture
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
        // 分配天数 TODO 后续再扣除停工的天数
        mpStructureAllocation.setAllotDays(endDay - beginDay + 1);
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
     * @param targetEmbryoCode 目标胚胎编码
     * @param mdmMaterialConsumeDetailList BOM物料消耗明细列表
     * @param specialMaterialList 特殊材料清单列表
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
     * 从集合中找出日期最接近目标开始/结束日的上一个结构
     * @param list 结构排产列表
     * @param excludeId 排除的目标结构ID
     * @param targetBeginDay 目标开始日
     * @param targetEndDay 目标结束日
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
}
