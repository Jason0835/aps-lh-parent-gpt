package com.zlt.aps.mp.factory.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.api.enums.AlternativeTypeEnum;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.mp.enums.StructureAllocationExportDataTypeEnum;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportChangeCountVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportStatisticsVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

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
    private final FactoryMouldingDayResultMapper factoryMouldingDayResultMapper;
    private final LhMachineInfoEntityMapper lhMachineInfoEntityMapper;
    private final DpDemandPlanEntityMapper dpDemandPlanEntityMapper;
    private final FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProductionFinalResultEntityMapper;
    private final ISysDictDataCacheService sysDictDataCacheService;
    private final Map<Long, Map<String, String>> importMachineMapCache = new ConcurrentHashMap<>();
    /**
     * 日计划字段名称
     */
    private final static String DAY_FIELD_NAME_FORMAT = "day%s";

    /**
     * 最新需求计划版本为周程调整类版本时的前缀（与业务约定一致）
     */
    private static final String LAST_MONTH_PLAN_VERSION_ADJ_PREFIX = "ADJ";

    private void cacheImportMachineMap(Long importLogId, Map<String, String> machineMap) {
        if (importLogId == null || CollUtil.isEmpty(machineMap)) {
            return;
        }
        importMachineMapCache.put(importLogId, new HashMap<>(machineMap));
    }

    private Map<String, String> getImportMachineMap(Long importLogId) {
        if (importLogId == null) {
            return Collections.emptyMap();
        }
        return importMachineMapCache.getOrDefault(importLogId, Collections.emptyMap());
    }

    private void clearImportMachineMap(Long importLogId) {
        if (importLogId == null) {
            return;
        }
        importMachineMapCache.remove(importLogId);
    }


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

    /**
     * 批量删除结构排产；对手工新增且满足定稿月计划 ADJ 版本条件时，级联逻辑删除月计划统计与定稿月计划（is_delete=1）。
     *
     * @param ids 主键列表
     * @return 删除条数（与父类语义一致）
     */
    @Override
    public int removeByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return super.removeByIds(ids);
        }
        List<MpStructureAllocation> snapshots = entityMapper.selectBatchIds(ids);
        assertHandStructureDeletionAllowed(snapshots, ids);
        int removed = super.removeByIds(ids);
        cascadeDeleteRelatedAfterRemoveHandStructure(snapshots);
        return removed;
    }

    /**
     * 删除前校验：同工厂/年/月/排产版本下，除本次待删 ID 外若仍存在非手工（dataSource 非 01）的结构排产则禁止删除。
     *
     * @param snapshots 待删除记录的删除前快照
     * @param deleteIds 本次待删除的主键列表
     */
    private void assertHandStructureDeletionAllowed(List<MpStructureAllocation> snapshots, List<Long> deleteIds) {
        if (CollectionUtils.isEmpty(snapshots) || CollectionUtils.isEmpty(deleteIds)) {
            return;
        }
        Map<String, MpStructureAllocation> versionGroupMap = new LinkedHashMap<>(snapshots.size());
        for (MpStructureAllocation item : snapshots) {
            if (item == null) {
                continue;
            }
            String key = buildVersionScopeKey(item);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            versionGroupMap.putIfAbsent(key, item);
        }
        for (MpStructureAllocation alloc : versionGroupMap.values()) {
            assertNoOtherNonHandStructureInSameVersion(alloc.getFactoryCode(), alloc.getYear(), alloc.getMonth(),
                    alloc.getProductionVersion(), deleteIds);
        }
    }

    /**
     * 构建排产版本范围键：工厂 + 年 + 月 + 排产版本（不含产品结构）。
     *
     * @param item 结构排产实体
     * @return 分组键；必填维度缺失时返回 null
     */
    private String buildVersionScopeKey(MpStructureAllocation item) {
        if (item == null) {
            return null;
        }
        if (StringUtils.isBlank(item.getFactoryCode()) || item.getYear() == null || item.getMonth() == null
                || StringUtils.isBlank(item.getProductionVersion())) {
            return null;
        }
        return item.getFactoryCode() + ApsConstant.SPLIT_CHAR + item.getYear() + ApsConstant.SPLIT_CHAR
                + item.getMonth() + ApsConstant.SPLIT_CHAR + item.getProductionVersion();
    }

    /**
     * 校验除本次待删 ID 外，同排产版本维度是否仍存在非手工结构排产。
     *
     * @param factoryCode      工厂编码
     * @param year             年
     * @param month            月
     * @param productionVersion 排产版本
     * @param deleteIds        本次待删除的主键列表
     */
    private void assertNoOtherNonHandStructureInSameVersion(String factoryCode, Integer year, Integer month,
                                                            String productionVersion, List<Long> deleteIds) {
        LambdaQueryWrapper<MpStructureAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpStructureAllocation::getFactoryCode, factoryCode)
                .eq(MpStructureAllocation::getYear, year)
                .eq(MpStructureAllocation::getMonth, month)
                .eq(MpStructureAllocation::getProductionVersion, productionVersion)
                .eq(MpStructureAllocation::getIsDelete, YesOrNoEnum.NO.getValue())
                .and(innerWrapper -> innerWrapper.ne(MpStructureAllocation::getDataSource, DataSourceEnum.HAND.getCode())
                        .or()
                        .isNull(MpStructureAllocation::getDataSource))
                .notIn(MpStructureAllocation::getId, deleteIds);
        Long count = entityMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpStructureAllocation.cannotDeleteWhenNonHandExists"));
        }
    }

    /**
     * 按工厂、年、月、排产版本、产品结构查询未逻辑删除的定稿月计划行。
     *
     * @param alloc 含五元组的结构排产
     * @return 定稿月计划列表，无则空列表
     */
    private List<FactoryMonthPlanProductionFinalResult> listProdFinalNotDeletedForStructure(MpStructureAllocation alloc) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, alloc.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, alloc.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, alloc.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, alloc.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getStructureName, alloc.getStructureName())
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, YesOrNoEnum.NO.getValue());
        return factoryMonthPlanProductionFinalResultEntityMapper.selectList(wrapper);
    }

    /**
     * 删除手工结构排产后，按工厂/年月/排产版本/结构去重并尝试级联逻辑删除关联表数据。
     *
     * @param snapshots 删除前快照的结构排产列表
     */
    private void cascadeDeleteRelatedAfterRemoveHandStructure(List<MpStructureAllocation> snapshots) {
        if (CollectionUtils.isEmpty(snapshots)) {
            return;
        }
        Map<String, MpStructureAllocation> groupMap = new LinkedHashMap<>(snapshots.size());
        for (MpStructureAllocation item : snapshots) {
            if (item == null || !DataSourceEnum.HAND.getCode().equals(item.getDataSource())) {
                continue;
            }
            String key = buildStructureCascadeGroupKey(item);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            groupMap.putIfAbsent(key, item);
        }
        for (MpStructureAllocation alloc : groupMap.values()) {
            deleteRelatedMonthPlanIfAdjVersion(alloc);
        }
    }

    /**
     * 构建级联删除分组键：工厂 + 年 + 月 + 排产版本 + 产品结构。
     *
     * @param item 结构排产实体
     * @return 分组键；必填维度缺失时返回 null
     */
    private String buildStructureCascadeGroupKey(MpStructureAllocation item) {
        if (item == null) {
            return null;
        }
        if (StringUtils.isBlank(item.getFactoryCode()) || item.getYear() == null || item.getMonth() == null
                || StringUtils.isBlank(item.getProductionVersion()) || StringUtils.isBlank(item.getStructureName())) {
            return null;
        }
        return item.getFactoryCode() + ApsConstant.SPLIT_CHAR + item.getYear() + ApsConstant.SPLIT_CHAR
                + item.getMonth() + ApsConstant.SPLIT_CHAR + item.getProductionVersion() + ApsConstant.SPLIT_CHAR
                + item.getStructureName();
    }

    /**
     * 若定稿月计划存在且 LAST_MONTH_PLAN_VERSION 以 ADJ 开头，则逻辑删除月计划统计及定稿月计划对应行（is_delete=1）。
     *
     * @param alloc 分组代表的结构排产（含工厂、年月、排产版本、结构）
     */
    private void deleteRelatedMonthPlanIfAdjVersion(MpStructureAllocation alloc) {
        List<FactoryMonthPlanProductionFinalResult> finalList = listProdFinalNotDeletedForStructure(alloc);
        if (CollectionUtils.isEmpty(finalList)) {
            return;
        }
        boolean needCascade = finalList.stream()
                .anyMatch(r -> StringUtils.isNotBlank(r.getLastMonthPlanVersion())
                        && r.getLastMonthPlanVersion().startsWith(LAST_MONTH_PLAN_VERSION_ADJ_PREFIX));
        if (!needCascade) {
            return;
        }
        // 月计划统计：逻辑删除
        LambdaUpdateWrapper<MpMonthPlanStatistics> statUpdate = new LambdaUpdateWrapper<>();
        statUpdate.eq(MpMonthPlanStatistics::getFactoryCode, alloc.getFactoryCode())
                .eq(MpMonthPlanStatistics::getYear, alloc.getYear())
                .eq(MpMonthPlanStatistics::getMonth, alloc.getMonth())
                .eq(MpMonthPlanStatistics::getProductionVersion, alloc.getProductionVersion())
                .eq(MpMonthPlanStatistics::getStructureName, alloc.getStructureName())
                .eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue())
                .set(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.YES.getValue());
        int statRows = mpMonthPlanStatisticsEntityMapper.update(null, statUpdate);
        // 定稿月计划：逻辑删除（与查询条件一致的全部未删除行）
        LambdaUpdateWrapper<FactoryMonthPlanProductionFinalResult> finalUpdate = new LambdaUpdateWrapper<>();
        finalUpdate.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, alloc.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, alloc.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, alloc.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, alloc.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getStructureName, alloc.getStructureName())
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, YesOrNoEnum.NO.getValue())
                .set(FactoryMonthPlanProductionFinalResult::getIsDelete, YesOrNoEnum.YES.getValue());
        int finalRows = factoryMonthPlanProductionFinalResultEntityMapper.update(null, finalUpdate);
        log.info("删除结构排产级联逻辑删除完成，factoryCode={}, year={}, month={}, productionVersion={}, structureName={}, 统计表行数={}, 定稿表行数={}",
                alloc.getFactoryCode(), alloc.getYear(), alloc.getMonth(), alloc.getProductionVersion(),
                alloc.getStructureName(), statRows, finalRows);
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
                .filter(vo -> StringUtils.equals(vo.getStructureName(), mpStructureAllocation.getStructureName()))
                .findFirst()
                .map(MdmSkuStructureRef::getMaterialCode)
                .orElse(null);
        String embryoCode = skuConstructionRefList.stream()
                .filter(vo -> StringUtils.equals(vo.getMaterialCode(), materialCode))
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
        // 1.1、加载硫化机总数
        LambdaQueryWrapper<LhMachineInfo> lhMachineQueryWrapper = new LambdaQueryWrapper<>();
        lhMachineQueryWrapper.eq(LhMachineInfo::getFactoryCode, param.getFactoryCode());
        Integer lhmachineCount = lhMachineInfoEntityMapper.selectCount(lhMachineQueryWrapper).intValue();
        // 1.2、加载月计划模具排产明细
        List<MpStructureAllocationExportVo> recordList = entityMapper.getExportList(param);
        // 1.3、加载本次版本已生成的统计记录
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, param.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, param.getProductionVersion());
        Map<String, MpMonthPlanStatistics> statisticsMap = mpMonthPlanStatisticsEntityMapper.selectList(queryWrapper)
                .stream().collect(
                        Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));
        // 1.3.1、从日历获取月底日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(param.getYear(), param.getMonth() - 1, FactoryConstant.MONTH_START_DAY);
        Integer monthMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        // 1.3.2、按结构 + 日期 统计硫化机台数
        Map<String, Map<Integer, Integer>> lhMachineStatisticsMap = new HashMap<>();
        for (Entry<String, MpMonthPlanStatistics> entry: statisticsMap.entrySet()) {
            Map<Integer, Integer> dayLhMachinesMap = new HashMap<>();
            MpMonthPlanStatistics statistics = entry.getValue();
            for (int day = 1; day <= monthMaxDay; day ++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                String dayStatisticsStr = (String)statistics.getFieldValueByFieldName(dayFieldName);
                if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                    MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
                    dayLhMachinesMap.put(day, dayStatistics.getLhMachines());
                }
            }
            lhMachineStatisticsMap.put(entry.getKey(), dayLhMachinesMap);
        }
        // 1.4、加载月计划排产明细
        LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> resultQueryWrapper = new LambdaQueryWrapper<>();
        resultQueryWrapper.eq(FactoryMonthPlanMouldDayResult::getFactoryCode, param.getFactoryCode());
        resultQueryWrapper.eq(FactoryMonthPlanMouldDayResult::getProductionVersion, param.getProductionVersion());
        Map<String, List<FactoryMonthPlanMouldDayResult>> mouldingDayResultMap = factoryMouldingDayResultMapper
                .selectList(resultQueryWrapper).stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName)); // 按结构对排产结果分组
        // 1.4.1、根据结构将月计划明细汇总
        String productTypeCode = null;
        Map<String, FactoryMonthPlanMouldDayResult> structureDayResultMap = new HashMap<>();
        for (Entry<String, List<FactoryMonthPlanMouldDayResult>> entry: mouldingDayResultMap.entrySet()) {
            String structureName = entry.getKey();
            FactoryMonthPlanMouldDayResult mouldingDayResultAggregated = null;
            for (FactoryMonthPlanMouldDayResult result: entry.getValue()) {
                if (productTypeCode == null) {
                    productTypeCode = result.getProductTypeCode();
                }
                if (mouldingDayResultAggregated == null) {
                    mouldingDayResultAggregated = result;
                    continue;
                }
                // 1.4.1.1、统计结构每日排产量
                for (int day = 1; day <= monthMaxDay; day ++) {
                    String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                    Integer sumValue = Optional.ofNullable((Integer)mouldingDayResultAggregated.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    Integer value = Optional.ofNullable((Integer)result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    mouldingDayResultAggregated.setFieldValueByFieldName(dayFieldName, sumValue + value);
                }
                // 1.4.1.2、统计结构总排产量
                Integer sumTotalQty = Optional.ofNullable(mouldingDayResultAggregated.getTotalQty()).orElse(0);
                Integer totalQty = Optional.ofNullable(result.getTotalQty()).orElse(0);
                mouldingDayResultAggregated.setTotalQty(sumTotalQty + totalQty);
            }
            structureDayResultMap.put(structureName, mouldingDayResultAggregated);
        }
        // 1.5、加载需求计划
        QueryWrapper<DpDemandPlan> dpDemandPlanQueryWrapper = new QueryWrapper<>();
        dpDemandPlanQueryWrapper.select("STRUCTURE_NAME", "SUM(UN_POSTPONE_NET_QTY) UN_POSTPONE_NET_QTY");
        dpDemandPlanQueryWrapper.groupBy("STRUCTURE_NAME");
        dpDemandPlanQueryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        dpDemandPlanQueryWrapper.eq("MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        Map<String, Integer> unPostponeNetQtyMap = dpDemandPlanEntityMapper.selectList(dpDemandPlanQueryWrapper)
                .stream().filter(p -> p.getUnPostponeNetQty() != null)
                .collect(Collectors.toMap(DpDemandPlan::getStructureName, DpDemandPlan::getUnPostponeNetQty));

        // 2、构建报表头
        MpStructureAllocationExportStatisticsVo exportVo = new MpStructureAllocationExportStatisticsVo();
        exportVo.setFactoryCode(param.getFactoryCode());
        exportVo.setYear(param.getYear());
        exportVo.setMonth(param.getMonth());
        exportVo.setMonthPlanVersion(param.getMonthPlanVersion());
        exportVo.setProductionVersion(param.getProductionVersion());
        exportVo.setProductTypeCode(productTypeCode);
        exportVo.setStructureChangeCount(0);
        exportVo.setProSizeChangeCount(0);

        // 3、构建导出总表
        List<MpStructureAllocationExportVo> totalRecordList = new LinkedList<>(); // 导出数据总表
        // 3.1、构建统计行
        // 3.1.1、排产合计
        MpStructureAllocationExportVo totalRecord = this.createExportRecord(StructureAllocationExportDataTypeEnum.TOTAL);
        // 3.1.2、最大产能
        MpStructureAllocationExportVo maxProductQtyRecord = this.createExportRecord(StructureAllocationExportDataTypeEnum.MAX_PRODUCT_QTY);
        // 3.1.3、可用台数
        MpStructureAllocationExportVo enableCountRecord = this.createExportRecord(StructureAllocationExportDataTypeEnum.ENABLE_COUNT);

        // 3.2、构建主体表格
        String cxMachineCode = null; // 当前机台
        List<MpStructureAllocationExportVo> machineStructureList = new ArrayList<>(); // 机台排产记录列表
        Map<Integer, Integer> totalMap = new HashMap<>(); // 汇总map，用于记录每天的机台合计值
        for (int day = 1; day <= monthMaxDay; day ++) {  // 初始化汇总map
            totalMap.put(day, 0);
        }
        for (Integer i = 0, size = recordList.size(); i < size; i ++) {
            // 3.2.1、把同结构的排产记录添加到列表中，全部添加完后开始处理这一批数据
            MpStructureAllocationExportVo record = recordList.get(i);
            machineStructureList.add(record); // 先添加到列表
            cxMachineCode = record.getCxMachineCode(); // 更新机台
            // 3.2.2、下一笔结构没有变化，且还不是最后一笔记录，继续遍历下一笔数据
            if (i < size - 1) { // 还不是最后一行，则校验下一行是否同一个机台
                MpStructureAllocationExportVo nextRecord = recordList.get(i + 1);
                if (cxMachineCode.equals(nextRecord.getCxMachineCode())) { // 机台没有变化，则添继续往下
                    continue;
                }
            }
            // 3.2.3、处理列表明细的数据
            Integer changeRank = 1; // 切换序号，用于导出的切换颜色渲染
            for (MpStructureAllocationExportVo machineRecord: machineStructureList) {
                Map<Integer, Integer> dayLhMachinesMap = lhMachineStatisticsMap.get(machineRecord.getStructureName());
                if (dayLhMachinesMap == null) {
                    continue;
                }
                Integer beginDay = null;
                Integer endDay = null;
                // 3.2.3.1、处理在机天数区间内的硫化机数
                for (int day = machineRecord.getBeginDay(); day <= machineRecord.getEndDay(); day ++) {
                    Integer lhMachines = dayLhMachinesMap.getOrDefault(day, 0);
                    if (lhMachines!= null && lhMachines > 0) {
                        Integer realLhMachines = Math.min(machineRecord.getMaxLhMachineCount(), lhMachines);
                        String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                        dayLhMachinesMap.put(day, lhMachines - realLhMachines);
                        this.updateExportDayField(machineRecord, dayFieldName, realLhMachines); // 更新明细
                        totalMap.put(day, totalMap.getOrDefault(day, 0) + realLhMachines); // 更新汇总map
                        // 更新开始结束时间
                        if (beginDay == null) {
                            beginDay = day;
                        }
                        endDay = day;
                    }
                }
                // 3.2.3.2、统计结构排产汇总数据
                FactoryMonthPlanMouldDayResult mouldingDayResultAggregated = structureDayResultMap.get(machineRecord.getStructureName());
                if (mouldingDayResultAggregated != null) {
                    machineRecord.setStructureType(mouldingDayResultAggregated.getStructureType()); // 结构类型
                    machineRecord.setTotalQty(mouldingDayResultAggregated.getTotalQty()); // 结构总排产量
                    Integer netQty = Optional.ofNullable(machineRecord.getNetQty()).orElse(0); // 净需求
                    Integer differenceQty = Optional.ofNullable(mouldingDayResultAggregated.getTotalQty()).orElse(0); // 未排量 = 净需求 - 总排产量
                    machineRecord.setDifferenceQty(netQty > differenceQty? netQty - differenceQty: 0); // 未排量小于0的按0算
                    machineRecord.setProductTypeCode(mouldingDayResultAggregated.getProductTypeCode());
                    machineRecord.setProSize(mouldingDayResultAggregated.getProSize());
                }
                machineRecord.setChangeRank(changeRank ++); // 设置序号
                machineRecord.setBeginDay(beginDay);
                machineRecord.setEndDay(endDay);
                machineRecord.setUnPostponeNetQty(unPostponeNetQtyMap.getOrDefault(machineRecord.getStructureName(), 0));
                if (beginDay != null && endDay != null) {
                    machineRecord.setAllotDays(endDay - beginDay + 1);
                }
            }
            totalRecordList.addAll(machineStructureList);
            machineStructureList.clear();
        }
        // 3.3、更新统计行数值
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
            // 统计结构切换次数
            Integer changeStructureCount = (int)cxMachineExportList.stream()
                    .filter(sa -> !AlternativeTypeEnum.CONTINUE.getCode().equals(sa.getAlternatingType())).count();
            if (changeStructureCount > 0) {
                Integer oldCount = changeStructureCountMap.getOrDefault(changeStructureCount, 0);
                changeStructureCountMap.put(changeStructureCount, oldCount + 1);
            }
        }
        // 4.2、统计英寸交替次数
        Integer proSizeChangeCount = (int) recordList.stream()
                .filter(sa -> AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode().equals(sa.getAlternatingType()))
                .count();
        exportVo.setProSizeChangeCount(proSizeChangeCount);
        // 4.3、统计规格交替次数
        Integer structureChangeCount = (int) recordList.stream()
                .filter(sa -> AlternativeTypeEnum.STRUCT_ALTERNATIVE.getCode().equals(sa.getAlternatingType()))
                .count();
        // 4.4、构建切换次数子表
        List<MpStructureAllocationExportChangeCountVo> changeCountList = new LinkedList<>();
        // 4.4.1、统计的规格切换次数表格
        changeStructureCountMap.keySet().stream().sorted(Integer::compareTo).forEach(changeCount -> {
            Integer machineCount = changeStructureCountMap.get(changeCount);
            MpStructureAllocationExportChangeCountVo changeCountRecord = new MpStructureAllocationExportChangeCountVo(
                    changeCount, machineCount, StructureAllocationExportDataTypeEnum.RECORD.getCode());
            changeCountList.add(changeCountRecord);
        });
        exportVo.setStructureChangeCount(structureChangeCount); // 更新结构切换 = 总结构切换数 - 换英寸数
        // 4.4.2、构建切换次数统计行
        Integer totalChangeCount = proSizeChangeCount + structureChangeCount;
        MpStructureAllocationExportChangeCountVo totalChangeCountRecord = new MpStructureAllocationExportChangeCountVo(
                0, totalChangeCount, StructureAllocationExportDataTypeEnum.TOTAL_CHANGE_COUNT.getCode());
        changeCountList.add(totalChangeCountRecord);
        exportVo.setChangeCountList(changeCountList);

        // 5、构建头部合计行
        MpStructureAllocationExportVo totalProductRecord = this.createExportRecord(StructureAllocationExportDataTypeEnum.TOTAL_PRODUCT_QTY);
        for (FactoryMonthPlanMouldDayResult result: structureDayResultMap.values()) {
            for (int day = 1; day <= monthMaxDay; day ++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                Integer value = Optional.ofNullable((Integer)result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                if (value > 0) {
                    Integer sumValue = Optional.ofNullable((Integer)totalProductRecord.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    totalProductRecord.setFieldValueByFieldName(dayFieldName, sumValue + value);
                }
            }
        }
        exportVo.setHeadList(Collections.singletonList(totalProductRecord));
        return exportVo;
    }

    /**
     * 构建指定类型的导出数据行
     * @param dataType
     * @return
     */
    private MpStructureAllocationExportVo createExportRecord(StructureAllocationExportDataTypeEnum dataType) {
        MpStructureAllocationExportVo totalRecord = new MpStructureAllocationExportVo();
        totalRecord.setStructureName(I18nUtil.getMessage(dataType.getName()));
        totalRecord.setDataType(dataType.getCode());
        return totalRecord;
    }

    /**
     * 更新导出数据的日数据
     *
     * @param exportVo     导出记录
     * @param dayFieldName 日数据字段
     * @param value        更新值
     */
    private void updateExportDayField(MpStructureAllocationExportVo exportVo, String dayFieldName, Integer value) {
        if (value == null) {
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
        // 结构类型字典
        List<SysDictData> structureTypeDatas = sysDictDataCacheService.getType("structure_type");
        Map<String, String> structureTypeMap = structureTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 设备类型字典
        List<SysDictData> machineBrandDatas = sysDictDataCacheService.getType("biz_machine_brand");
        Map<String, String> machineBrandMap = machineBrandDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 产品品类字典
        List<SysDictData> productTypeDatas = sysDictDataCacheService.getType("biz_product_type");
        Map<String, String> productTypeMap = productTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));

        String factoryName = factoryMap.getOrDefault(statisticsVo.getFactoryCode(), "");
        String titleFormat = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.exportTitle");
        String productType = productTypeMap.getOrDefault(statisticsVo.getProductTypeCode(), statisticsVo.getProductTypeCode());
        tableMap.put("title", String.format(titleFormat, statisticsVo.getYear(), statisticsVo.getMonth(), factoryName, productType));

        String monthPlanVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.monthPlanVersion");
        String productionVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.productionVersion");
        tableMap.put("monthPlanVersion", monthPlanVersionLabel + ": " + statisticsVo.getMonthPlanVersion());
        tableMap.put("productionVersion", productionVersionLabel + ": " + statisticsVo.getProductionVersion());

        List<Map<String, Object>> listData = new ArrayList<>();
        // 设置表头名称
        Map<String, Object> headMap = new HashMap<>();
        headMap.put("cxMachineCode", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.cxMachineCode"));
        headMap.put("cxMachineTypeCode", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.cxMachineTypeCode"));
        headMap.put("structureName", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.structureName"));
        headMap.put("structureType", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.structureType"));
        headMap.put("lossQty", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.lossQty"));
        headMap.put("unPostponeNetQty", I18nUtil.getMessage("ui.data.column.demandPlanSum.unPostponeNetQty"));
        headMap.put("totalQty", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.totalQty"));
        headMap.put("differenceQty", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.differenceQty"));
        headMap.put("allotDays", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.allotDays"));
        headMap.put("maxLhMachineCount", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.maxLhMachineCount"));
        headMap.put("dailyproductionQty", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.dailyproductionQty"));
        tableMap.putAll(headMap);

        // 构建表头汇总行
        if (!CollectionUtils.isEmpty(statisticsVo.getHeadList())) {
            List<Map<String, Object>> totalList = new ArrayList<>();
            statisticsVo.getHeadList().stream()
                    .forEach(exportVo -> {
                        Map<String, Object> listDataMap = this.buildListDataMap(exportVo, factoryMap,
                                structureTypeMap, machineBrandMap, "A"); // 生成一份后缀带A的版本
                        totalList.add(listDataMap);
                    });
            excelDataList.add(totalList);
        }
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
            beginIndex += !CollectionUtils.isEmpty(statisticsVo.getHeadList())? statisticsVo.getHeadList().size(): 0; // 如果表头有复制统计行，起始行要往下顺延

            for (int i = 0; i < mpStructureAllocationExportVoList.size(); i++) {
                MpStructureAllocationExportVo exportVo = mpStructureAllocationExportVoList.get(i);
                String currentCxMachineCode = exportVo.getCxMachineCode();
                Map<String, Object> listDataMap = this.buildListDataMap(exportVo, factoryMap, structureTypeMap,
                        machineBrandMap, null);
                // 处理底色：只有成型机不一样时，切换颜色区分
                // Excel行号从2开始（第1行是表头）
                int rowNum = beginIndex + i;
                if (StructureAllocationExportDataTypeEnum.RECORD.getCode().equals(exportVo.getDataType())) {
                    // 如果成型机改变，切换颜色
                    if (prevCxMachineCode == null || !prevCxMachineCode.equals(currentCxMachineCode)) {
                        toggleColor = !toggleColor;
                        prevCxMachineCode = currentCxMachineCode;
                    }
                    // 交替使用两种颜色
                    String color = toggleColor ? "#e2efda" : "#d9d9d9";

                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, headMap.size() - 2, color, true, false, ""));

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
                            int startCol = headMap.size() - 1 + firstDayWithValue;
                            int endCol = headMap.size() - 1 + lastDayWithValue;
                            cellStyleList.add(new CellStyle(rowNum, rowNum, startCol, endCol, colorSelect, true, false, ""));

                        }
                    }
                }
                if (StructureAllocationExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())
                        || StructureAllocationExportDataTypeEnum.MAX_PRODUCT_QTY.getCode().equals(exportVo.getDataType())
                        || StructureAllocationExportDataTypeEnum.ENABLE_COUNT.getCode().equals(exportVo.getDataType())) {
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, headMap.size() + 29, "#DAEEF3", true, true, ""));
                }

                listData.add(listDataMap);
            }
            // 将处理好的数据添加到excelDataList
            excelDataList.add(listData);
        }
        //切换次数
        // 构建切换次数表头
        tableMap.put("changeSingleCount", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.changeSingleCount"));
        tableMap.put("changeMachineCount", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.changeMachineCount"));
        tableMap.put("totalChangeCount", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.totalChangeCount"));
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

        // 构建切换类型子表
        tableMap.put("changeType", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.changeType"));
        tableMap.put("changeCountLabel", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.changeCountLabel"));
        tableMap.put("proSizeChangeCountLabel", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.proSizeChangeCount"));
        tableMap.put("structureChangeCountLabel", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.structureChangeCount"));
        // 填充数据
        tableMap.put("proSizeChangeCount", statisticsVo.getProSizeChangeCount()); //英寸交替
        tableMap.put("structureChangeCount", statisticsVo.getStructureChangeCount()); //结构切换

        // 将单元格样式放入context
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
    }

    /**
     * 构建导出行
     * @param exportVo
     * @param factoryMap
     * @param structureTypeMap
     * @param machineBrandMap
     * @param suffix    后缀，用于复制合计行
     * @return
     */
    private Map<String, Object> buildListDataMap(MpStructureAllocationExportVo exportVo, Map<String, String> factoryMap,
                                                 Map<String, String> structureTypeMap,
                                                 Map<String, String> machineBrandMap, String suffix) {
        Map<String, Object> listDataMap = new HashMap<>(32);
        listDataMap.put(this.getRealFieldName("cxMachineCode", suffix), exportVo.getCxMachineCode());
        listDataMap.put(this.getRealFieldName("cxMachineTypeCode", suffix), machineBrandMap.getOrDefault(exportVo.getCxMachineTypeCode(), exportVo.getCxMachineTypeCode()));
        listDataMap.put(this.getRealFieldName("structureName", suffix), exportVo.getStructureName());
        listDataMap.put(this.getRealFieldName("structureType", suffix), structureTypeMap.getOrDefault(exportVo.getStructureType(), exportVo.getStructureType()));
        listDataMap.put(this.getRealFieldName("lossQty", suffix), exportVo.getLossQty());
        listDataMap.put(this.getRealFieldName("unPostponeNetQty", suffix), exportVo.getUnPostponeNetQty());
        listDataMap.put(this.getRealFieldName("totalQty", suffix), exportVo.getTotalQty());
        listDataMap.put(this.getRealFieldName("differenceQty", suffix), exportVo.getDifferenceQty());
        listDataMap.put(this.getRealFieldName("allotDays", suffix), exportVo.getAllotDays());
        listDataMap.put(this.getRealFieldName("maxLhMachineCount", suffix), exportVo.getMaxLhMachineCount());
        listDataMap.put(this.getRealFieldName("day1", suffix), exportVo.getDay1());
        listDataMap.put(this.getRealFieldName("day2", suffix), exportVo.getDay2());
        listDataMap.put(this.getRealFieldName("day3", suffix), exportVo.getDay3());
        listDataMap.put(this.getRealFieldName("day4", suffix), exportVo.getDay4());
        listDataMap.put(this.getRealFieldName("day5", suffix), exportVo.getDay5());
        listDataMap.put(this.getRealFieldName("day6", suffix), exportVo.getDay6());
        listDataMap.put(this.getRealFieldName("day7", suffix), exportVo.getDay7());
        listDataMap.put(this.getRealFieldName("day8", suffix), exportVo.getDay8());
        listDataMap.put(this.getRealFieldName("day9", suffix), exportVo.getDay9());
        listDataMap.put(this.getRealFieldName("day10", suffix), exportVo.getDay10());
        listDataMap.put(this.getRealFieldName("day11", suffix), exportVo.getDay11());
        listDataMap.put(this.getRealFieldName("day12", suffix), exportVo.getDay12());
        listDataMap.put(this.getRealFieldName("day13", suffix), exportVo.getDay13());
        listDataMap.put(this.getRealFieldName("day14", suffix), exportVo.getDay14());
        listDataMap.put(this.getRealFieldName("day15", suffix), exportVo.getDay15());
        listDataMap.put(this.getRealFieldName("day16", suffix), exportVo.getDay16());
        listDataMap.put(this.getRealFieldName("day17", suffix), exportVo.getDay17());
        listDataMap.put(this.getRealFieldName("day18", suffix), exportVo.getDay18());
        listDataMap.put(this.getRealFieldName("day19", suffix), exportVo.getDay19());
        listDataMap.put(this.getRealFieldName("day20", suffix), exportVo.getDay20());
        listDataMap.put(this.getRealFieldName("day21", suffix), exportVo.getDay21());
        listDataMap.put(this.getRealFieldName("day22", suffix), exportVo.getDay22());
        listDataMap.put(this.getRealFieldName("day23", suffix), exportVo.getDay23());
        listDataMap.put(this.getRealFieldName("day24", suffix), exportVo.getDay24());
        listDataMap.put(this.getRealFieldName("day25", suffix), exportVo.getDay25());
        listDataMap.put(this.getRealFieldName("day26", suffix), exportVo.getDay26());
        listDataMap.put(this.getRealFieldName("day27", suffix), exportVo.getDay27());
        listDataMap.put(this.getRealFieldName("day28", suffix), exportVo.getDay28());
        listDataMap.put(this.getRealFieldName("day29", suffix), exportVo.getDay29());
        listDataMap.put(this.getRealFieldName("day30", suffix), exportVo.getDay30());
        listDataMap.put(this.getRealFieldName("day31", suffix), exportVo.getDay31());
        return listDataMap;
    }

    /**
     * 获取实际字段名，后缀有值需要拼接上后缀
     * @param fieldName
     * @param suffix
     * @return
     */
    private String getRealFieldName(String fieldName, String suffix) {
        if (StringUtils.isNotEmpty(suffix)) {
            return fieldName + suffix;
        }
        return fieldName;
    }

    /**
     * 导入
     *
     * @param list             列表数据
     * @param updateSupport    覆盖
     * @param importLogId      导入日志ID
     * @param params           表头参数
     * @param monthPlanVersion 月计划版本
     * @param productVersion   生产版本
     * @return 结果
     */
    @Override
    public AjaxResult importDataStructureAllocation(List<MpStructureAllocationExportVo> list, boolean updateSupport, Long importLogId, String[] params, String monthPlanVersion, String productVersion,
                                                    Map<String, String> factoryMap, Map<String, String> productTypeMap) {
        // 1.初始化
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<MpStructureAllocation> insertList = new ArrayList<>();
        // 解析的excel表头参数
        String year = params[0];
        String month = params[1];
        String factoryName = params[2];

        //2.国际化初始化
        String noFactoryStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.noFactoryStr");
        String yearErrorStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.yearErrorStr");
        String monthErrorStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.monthErrorStr");

        // 过滤合计等数据
        list = list.stream().filter(item -> StringUtils.isNotBlank(item.getCxMachineCode())).collect(Collectors.toList());

        Map<String, String> machineMap = new HashMap<>();
        Map<String, MpStructureAllocation> machineLastValidRecordMap = new HashMap<>();
        Map<String, FactoryMonthPlanProductionFinalResult> lastMonthMachineFinalMap = Collections.emptyMap();
        Integer importYear = Convert.toInt(year, null);
        Integer importMonth = Convert.toInt(month, null);
        String importFactoryCode = factoryMap.get(factoryName);
        if (importYear != null && importMonth != null && StringUtils.isNotBlank(importFactoryCode)) {
            lastMonthMachineFinalMap = getLastMonthMachineFinalMap(importFactoryCode, importYear, importMonth);
        }

        //3.公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpStructureAllocation item = list.get(i);

            item.setDataSource(DataSourceEnum.IMPORT.getCode());
            item.setMonthPlanVersion(monthPlanVersion);
            item.setProductionVersion(productVersion);
            item.setPlanType("01");

            try {
                item.setYear(Integer.parseInt(year));
            } catch (NumberFormatException e) {
                item.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, yearErrorStr, importErrorLogs);
                continue;
            }
            try {
                item.setMonth(Integer.parseInt(month));
            } catch (NumberFormatException e) {
                item.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, monthErrorStr, importErrorLogs);
                continue;
            }
            if (factoryMap.containsKey(factoryName)) {
                String factoryCode = factoryMap.get(factoryName);
                item.setFactoryCode(factoryCode);
            } else {
                item.setId(-999L);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, String.format(noFactoryStr, factoryName), importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
            if (CollUtil.isNotEmpty(validated)) {
                item.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }
            // 赋值开始结束日期
            setBeginDayAndEndDay(item);
            // 赋值交替类型（仅对校验通过的有效记录）
            genAlternatingType(item, machineLastValidRecordMap, lastMonthMachineFinalMap);
//            item.setIsHasSpecialMaterial();
            insertList.add(item);

            machineMap.put(item.getStructureName(), item.getCxMachineCode());
        }

        // 过滤id不等于空的数据
        insertList = insertList.stream().filter(v -> v.getId() == null).collect(Collectors.toList());

        try {
            successNum = insertList.size();
            if(CollUtil.isNotEmpty(insertList)){
                // 插入新记录
                baseDao.insertBatch(insertList);
            }
        } catch (Exception e) {
            log.error("导入失败", e);
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        //返回提示信息及错误集合
        cacheImportMachineMap(importLogId, machineMap);
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    private void setBeginDayAndEndDay(MpStructureAllocation item) {
        for (int i = 1; i <= 31; i++) {
            Object fieldValue = ReflectUtils.getFieldValue(item, "day" + i);
            if (ObjUtil.isNotNull(fieldValue)) {
                if (item.getBeginDay() == null) {
                    item.setBeginDay(i);
                }
                item.setEndDay(i);
            }
        }
    }

    private void genAlternatingType(MpStructureAllocation item,
                                    Map<String, MpStructureAllocation> machineLastValidRecordMap,
                                    Map<String, FactoryMonthPlanProductionFinalResult> lastMonthMachineFinalMap) {
        if (item == null || StringUtils.isBlank(item.getCxMachineCode())) {
            return;
        }
        String machineCode = item.getCxMachineCode();
        MpStructureAllocation previousRecord = machineLastValidRecordMap.get(machineCode);
        if (previousRecord != null) {
            if (StringUtils.equals(previousRecord.tbrProSize(), item.tbrProSize())) {
                item.setAlternatingType(AlternativeTypeEnum.STRUCT_ALTERNATIVE.getCode());
            } else {
                item.setAlternatingType(AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode());
            }
            machineLastValidRecordMap.put(machineCode, item);
            return;
        }

        FactoryMonthPlanProductionFinalResult lastMonthRecord = lastMonthMachineFinalMap.get(machineCode);
        if (lastMonthRecord != null) {
            if (StringUtils.equals(lastMonthRecord.getStructureName(), item.getStructureName())) {
                item.setAlternatingType(AlternativeTypeEnum.CONTINUE.getCode());
            } else if (StringUtils.equals(lastMonthRecord.getProSize(), item.tbrProSize())) {
                item.setAlternatingType(AlternativeTypeEnum.STRUCT_ALTERNATIVE.getCode());
            } else {
                item.setAlternatingType(AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode());
            }
        } else {
            item.setAlternatingType(AlternativeTypeEnum.CONTINUE.getCode());
        }
        machineLastValidRecordMap.put(machineCode, item);
    }

    private Map<String, FactoryMonthPlanProductionFinalResult> getLastMonthMachineFinalMap(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || year == null || month == null) {
            return Collections.emptyMap();
        }
        java.time.YearMonth currentYearMonth = java.time.YearMonth.of(year, month);
        java.time.YearMonth previousYearMonth = currentYearMonth.minusMonths(1);

        FactoryMonthPlanProductionFinalResult queryParam = new FactoryMonthPlanProductionFinalResult();
        queryParam.setFactoryCode(factoryCode);
        queryParam.setYear(previousYearMonth.getYear());
        queryParam.setMonth(previousYearMonth.getMonthValue());

        List<FactoryMonthPlanProductionFinalResult> lastMonthFinalList = monthPlanProductionFinalResultService.listMonthProdFinalPlans(queryParam);
        if (CollUtil.isEmpty(lastMonthFinalList)) {
            return Collections.emptyMap();
        }

        Map<String, FactoryMonthPlanProductionFinalResult> machineRecordMap = new HashMap<>();
        Map<String, Integer> machineLastScheduleDayMap = new HashMap<>();
        for (FactoryMonthPlanProductionFinalResult record : lastMonthFinalList) {
            String machineCode = record.getCxMachineCode();
            if (StringUtils.isBlank(machineCode)) {
                continue;
            }
            int lastScheduleDay = getLastScheduleDay(record);
            if (lastScheduleDay <= 0) {
                continue;
            }
            Integer currentLastScheduleDay = machineLastScheduleDayMap.get(machineCode);
            if (currentLastScheduleDay == null || lastScheduleDay >= currentLastScheduleDay) {
                machineLastScheduleDayMap.put(machineCode, lastScheduleDay);
                machineRecordMap.put(machineCode, record);
            }
        }
        return machineRecordMap;
    }

    private int getLastScheduleDay(FactoryMonthPlanProductionFinalResult record) {
        for (int day = 31; day >= 1; day--) {
            Object value = record.getFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, day));
            Integer dayQty = Convert.toInt(value, 0);
            if (dayQty != null && dayQty > 0) {
                return day;
            }
        }
        return 0;
    }

    /**
     * 导入
     *
     * @param list             列表数据
     * @param updateSupport    覆盖
     * @param importLogId      导入日志ID
     * @param params           表头参数
     * @param monthPlanVersion 月计划版本
     * @param productVersion   生产版本
     * @return 结果
     */
    @Override
    public AjaxResult importDataDayResult(List<FactoryMonthPlanMouldDayResult> list, boolean updateSupport, Long importLogId, String[] params, String monthPlanVersion, String productVersion,
                                          Map<String, String> factoryMap, Map<String, String> productTypeMap) {
        Map<String, String> machineMap = getImportMachineMap(importLogId);
        try {
            //1.初始化
            int successNum = 0;
            int failureNum = 0;
            List<ImportErrorLog> importErrorLogs = new ArrayList<>();
            List<FactoryMonthPlanMouldDayResult> insertList = new ArrayList<>();
            // 解析的excel表头参数
            String year = params[1];
            String month = params[2];
            String factoryName = params[0];

            //2.国际化初始化
            String noFactoryStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.noFactoryStr");
            String yearErrorStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.yearErrorStr");
            String monthErrorStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.monthErrorStr");

            // 过滤合计等数据
            list = list.stream().filter(item -> StringUtils.isNotBlank(item.getMaterialCode())).collect(Collectors.toList());

            //3.公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                int errorNum = i + 2;
                FactoryMonthPlanMouldDayResult item = list.get(i);

                item.setIsImport(YesOrNoEnum.YES.getCode());
                item.setMonthPlanVersion(monthPlanVersion);
                item.setProductionVersion(productVersion);
                item.setPlanType("01");

                // 赋值开始结束日期
                setBeginDayAndEndDay(item);

                if (productTypeMap.containsKey(params[3])) {
                    item.setProductTypeCode(productTypeMap.get(params[3]));
                }
                if (StringUtils.isBlank(item.getCxMachineCode()) && StringUtils.isNotBlank(item.getStructureName())) {
                    String machineCode = machineMap.get(item.getStructureName());
                    if (StringUtils.isNotBlank(machineCode)) {
                        item.setCxMachineCode(machineCode);
                    }
                }

                try {
                    item.setYear(Integer.parseInt(year));
                } catch (NumberFormatException e) {
                    item.setId(-999L);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, yearErrorStr, importErrorLogs);
                    continue;
                }
                try {
                    item.setMonth(Integer.parseInt(month));
                } catch (NumberFormatException e) {
                    item.setId(-999L);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, monthErrorStr, importErrorLogs);
                    continue;
                }
                if (factoryMap.containsKey(factoryName)) {
                    String factoryCode = factoryMap.get(factoryName);
                    item.setFactoryCode(factoryCode);
                } else {
                    item.setId(-999L);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, String.format(noFactoryStr, factoryName), importErrorLogs);
                    continue;
                }

                List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
                if (CollUtil.isNotEmpty(validated)) {
                    item.setId(-999L);
                    failureNum++;
                    importErrorLogs.addAll(validated);
                    continue;
                }
                insertList.add(item);
            }

            // 过滤id不等于空的数据
            insertList = insertList.stream().filter(v -> v.getId() == null).collect(Collectors.toList());

            try {
                successNum = insertList.size();
                if(CollUtil.isNotEmpty(insertList)) {
                    // 插入新记录
                    baseDao.insertBatch(insertList);
                }
            } catch (Exception e) {
                log.error("导入失败", e);
                successNum = 0;
                failureNum = list.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
            //返回提示信息及错误集合
            if (failureNum > 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
            } else {
                return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
            }
        } finally {
            clearImportMachineMap(importLogId);
        }
    }

    private void setBeginDayAndEndDay(FactoryMonthPlanMouldDayResult item) {
        for (int i = 1; i <= 31; i++) {
            Object fieldValue = ReflectUtils.getFieldValue(item, "day" + i);
            if (ObjUtil.isNotNull(fieldValue)) {
                if (item.getBeginDay() == null) {
                    item.setBeginDay(i);
                }
                item.setEndDay(i);
            }
        }
    }
}
