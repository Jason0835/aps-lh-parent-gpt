package com.zlt.aps.mp.factory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import java.text.MessageFormat;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.common.core.enums.DataSourceEnum;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.ProductionGroupTypeEnum;
import com.zlt.aps.enums.ProductionProcessesTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IMdmSkuScheduleCategoryService;
import com.zlt.aps.maindata.service.IRawSpecialMaterialRecordService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpWeekAdjustService;
import com.zlt.aps.mp.adjust.service.impl.MpAdjustStructureInStrategy;
import com.zlt.aps.mp.adjust.service.impl.MpMonthPlanStaticService;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.*;
import com.zlt.aps.mp.api.enums.AlternativeTypeEnum;
import com.zlt.aps.mp.api.enums.WeekAdjustTypeEnum;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine;
import com.zlt.aps.mp.engine.capacity.MpAdjustDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.mp.engine.enums.DayVulcanizationModeEnum;
import com.zlt.aps.mp.engine.handler.GroupProductionConversionHandler;
import com.zlt.aps.mp.engine.handler.LhMachineInfoCalculateHelper;
import com.zlt.aps.mp.engine.mapper.FactoryMonthPlanProductMouldMapper;
import com.zlt.aps.mp.engine.mapper.FactoryMouldingDayResultMapper;
import com.zlt.aps.mp.engine.service.ProductionMdmDataService;
import com.zlt.aps.mp.engine.utils.DateUtils;
import com.zlt.aps.mp.enums.StructureAllocationExportDataTypeEnum;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportChangeCountVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportStatisticsVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationExportVo;
import com.zlt.aps.mp.factory.dto.MpStructureAllocationImportHelper;
import com.zlt.aps.mp.factory.mapper.*;
import com.zlt.aps.mp.factory.service.IMpMonthPlanStatisticsService;
import com.zlt.aps.mp.factory.service.IMpStructureAllocationService;
import com.zlt.aps.mp.factory.service.ISpecialMaterialResultService;
import com.zlt.aps.mp.mdm.dto.DataDTO;
import com.zlt.aps.mp.mdm.handler.DataManager;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.StringUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsNumberUtils.intValue;
import static com.zlt.aps.common.core.utils.ApsNumberUtils.safeAdd;
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

    private final MpFinalStructureAllocationLogEntityMapper finalStructureAllocationLogEntityMapper;

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

    private final IMpMonthPlanStatisticsService monthPlanStatisticsService;

    private final FactoryMouldingDayResultMapper factoryMouldingDayResultMapper;
    private final LhMachineInfoEntityMapper lhMachineInfoEntityMapper;
    private final MdmMoldingMachineEntityMapper moldingMachineEntityMapper;
    private final DpDemandPlanEntityMapper dpDemandPlanEntityMapper;
    private final FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProductionFinalResultEntityMapper;
    private final ISysDictDataCacheService sysDictDataCacheService;
    private final MdmSkuLhCapacityEntityMapper mdmSkuLhCapacityEntityMapper;
    private final MpFactoryProductionVersionMapper mpFactoryProductionVersionMapper;
    private final ProductionMdmDataService productionMdmDataService;
    private final MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;
    private final MpTrialPlanEntityMapper mpTrialPlanEntityMapper;
    private final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    private final MpAdjustResultEntityMapper mpAdjustResultEntityMapper;
    private final FactoryMonthPlanProductMouldMapper factoryMonthPlanProductMouldMapper;
    private final DataManager dataManager;
    private final IRawSpecialMaterialRecordService rawSpecialMaterialRecordService;
    private final IMdmSkuScheduleCategoryService mdmSkuScheduleCategoryService;
    private final ISpecialMaterialResultService iSpecialMaterialResultService;
    private final Map<Long, Map<String, String>> importMachineMapCache = new ConcurrentHashMap<>();
    @Autowired
    @Lazy
    private MpMonthPlanStaticService mpMonthPlanStaticService;
    @Autowired
    @Lazy
    private MpAdjustStructureInStrategy mpAdjustStructureInStrategy;

    @Autowired
    private RedisService redisService;
    /**
     * 日计划字段名称
     */
    private final static String DAY_FIELD_NAME_FORMAT = "day%s";
    /**
     * 上月定稿日计划字段名称
     */
    private final static String LAST_DAY_FIELD_NAME_FORMAT = "lastDay%s";

    /**
     * 最新需求计划版本为周程调整类版本时的前缀（与业务约定一致）
     */
    private static final String LAST_MONTH_PLAN_VERSION_ADJ_PREFIX = "ADJ";
    /**
     * 上月需加载的天数
     */
    private final static Integer LAST_MONTH_DAY = 10;
    /**
     * 导入模板信息，仅加载一次
     */
    private static String sheetName = null;
    private static String sheetName4DayResult = null;
    private static int columnCount = 0;
    private static int columnCount4DayResult = 0;

    /**
     * 导入错误记录的缺省ID
     */
    private static final Long errorImportId = -999L;

    private void cacheImportMachineMap(Long importLogId, Map<String, String> machineMap) {
        if (importLogId == null || CollUtil.isEmpty(machineMap)) {
            return;
        }
        importMachineMapCache.put(importLogId, new HashMap<>(machineMap));
    }

    private Map<String, String> getImportMachineMap(Long importLogId) {
        if (importLogId == null) {
            return new HashMap<>();
        }
        return importMachineMapCache.getOrDefault(importLogId, new HashMap<>());
    }

    private void clearImportMachineMap(Long importLogId) {
        if (importLogId == null) {
            return;
        }
        importMachineMapCache.remove(importLogId);
    }


    @Override
    public List<MpStructureAllocation> getDataList(MpStructureAllocation param, boolean isFinalAdjust) {
        return getStructureAllocationData(param, isFinalAdjust);
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
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq(PubUtil.isNotEmpty(param.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        queryWrapper.eq(PubUtil.isNotEmpty(param.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", param.getProductionVersion());

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
        return Arrays.asList("factoryCode", "year", "month", "structureName", "productionVersion", "cxMachineCode");
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
     * 删除前校验：同工厂/年/月/排产版本/产品结构下，除本次待删 ID 外若仍存在非手工（dataSource 非 01）的结构排产则禁止删除。
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
            String key = buildVersionAndStructureScopeKey(item);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            versionGroupMap.putIfAbsent(key, item);
        }
        for (MpStructureAllocation alloc : versionGroupMap.values()) {
            assertNoOtherNonHandStructureInSameVersionAndStructure(alloc.getFactoryCode(), alloc.getYear(), alloc.getMonth(),
                    alloc.getProductionVersion(), alloc.getStructureName(), deleteIds);
        }
    }

    /**
     * 构建排产版本与产品结构范围键：工厂 + 年 + 月 + 排产版本 + 产品结构。
     *
     * @param item 结构排产实体
     * @return 分组键；必填维度缺失时返回 null
     */
    private String buildVersionAndStructureScopeKey(MpStructureAllocation item) {
        if (item == null) {
            return null;
        }
        if (StringUtils.isBlank(item.getFactoryCode()) || item.getYear() == null || item.getMonth() == null
                || StringUtils.isBlank(item.getProductionVersion())) {
            return null;
        }
        // 产品结构为空时仍参与分组，与同结构 NULL/空串 的库内数据一致
        String structurePart = StringUtils.isEmpty(item.getStructureName()) ? "" : item.getStructureName();
        return item.getFactoryCode() + ApsConstant.SPLIT_CHAR + item.getYear() + ApsConstant.SPLIT_CHAR
                + item.getMonth() + ApsConstant.SPLIT_CHAR + item.getProductionVersion() + ApsConstant.SPLIT_CHAR
                + structurePart;
    }

    /**
     * 校验除本次待删 ID 外，同排产版本且同产品结构维度是否仍存在非手工结构排产。
     *
     * @param factoryCode       工厂编码
     * @param year              年
     * @param month             月
     * @param productionVersion 排产版本
     * @param structureName     产品结构
     * @param deleteIds         本次待删除的主键列表
     */
    private void assertNoOtherNonHandStructureInSameVersionAndStructure(String factoryCode, Integer year, Integer month,
                                                                        String productionVersion, String structureName, List<Long> deleteIds) {
        LambdaQueryWrapper<MpStructureAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpStructureAllocation::getFactoryCode, factoryCode)
                .eq(MpStructureAllocation::getYear, year)
                .eq(MpStructureAllocation::getMonth, month)
                .eq(MpStructureAllocation::getProductionVersion, productionVersion);
        wrapper.eq(MpStructureAllocation::getStructureName, structureName);
        wrapper.eq(MpStructureAllocation::getIsDelete, YesOrNoEnum.NO.getValue())
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
        List<MdmSkuConstructionRef> skuConstructionRefList = skuConstructionRefFuture.join();
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

        //处理当前机台，当前结构及前后结构的交替类型
        //获取上个月最后机台结构信息
        Map<String, MpStructureAllocation> lastMachineStructureMap = getLastMachineStructureMap(mpStructureAllocation.getFactoryCode(), mpStructureAllocation.getYear(), mpStructureAllocation.getMonth());
        Map<String, String> continueStructureMap = new HashMap<>();
        if (PubUtil.isNotEmpty(lastMachineStructureMap)) {
            MpStructureAllocation lastMachineStructure = lastMachineStructureMap.get(mpStructureAllocation.getCxMachineCode());
            if (lastMachineStructure != null) {
                continueStructureMap.put(lastMachineStructure.getCxMachineCode(), lastMachineStructure.getStructureName());
            }
        }
        List<MpStructureAllocation> machineStructureList = structureAllocationList.stream().filter(x -> x.getCxMachineCode().equals(mpStructureAllocation.getCxMachineCode())).collect(Collectors.toList());
        if (PubUtil.isEmpty(machineStructureList)) {
            machineStructureList = new ArrayList<>();
        }
        machineStructureList.add(mpStructureAllocation);
        GroupProductionConversionHandler.setAlternatingType(machineStructureList, continueStructureMap);
        baseDao.updateBatch(machineStructureList);
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
     * 查询成型机台类型
     *
     * @param mpStructureAllocation
     */
    private Map<String, String> queryMoldingMachineTypeCode(MpStructureAllocation mpStructureAllocation) {
        LambdaQueryWrapper<MdmMoldingMachine> moldingMachineQueryWrapper = new LambdaQueryWrapper<>();
        moldingMachineQueryWrapper.eq(MdmMoldingMachine::getFactoryCode, mpStructureAllocation.getFactoryCode());
        return moldingMachineEntityMapper.selectList(moldingMachineQueryWrapper).stream().collect(Collectors
                .toMap(MdmMoldingMachine::getCxMachineCode, MdmMoldingMachine::getCxMachineTypeCode, (m1, m2) -> m1));
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
        return getDataList(queryParam, true);
    }

    /**
     * 查询成型硫化结构配比
     *
     * @param mpStructureAllocation
     */
    @Override
    public List<MdmStructureLhRatio> queryMdmStructureLhRatio(MpStructureAllocation mpStructureAllocation) {
        LambdaQueryWrapper<MdmStructureLhRatio> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmStructureLhRatio::getFactoryCode, mpStructureAllocation.getFactoryCode());
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
        queryWrapper.eq(MdmWorkCalendar::getProcCode, ProductionProcessesTypeEnum.MONTH_PLAN.getProcCode());
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
     *
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
     *
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
     *
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
            return Boolean.FALSE;
        }
    }

    @Override
    public AdjustsCxMachineVo getAdjustsCxMachineFromRedis() {
        return redisService.getCacheObject(ApsConstant.ADJUSTS_CX_MACHINE_KEY);
    }

    @Override
    public void setAdjustsCxMachineFromRedis(AdjustsCxMachineVo adjustsCxMachineVo) {
        redisService.setCacheObject(ApsConstant.ADJUSTS_CX_MACHINE_KEY, adjustsCxMachineVo);
    }

    /**
     * 获取日期最接近的上一个结构
     *
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
        if (StringUtils.isNotBlank(createCondition.getStructureName())) {
            return Sets.newHashSet(createCondition.getStructureName());
        }
        LambdaQueryWrapper<MpStructureAllocation> queryWrapper = Wrappers.<MpStructureAllocation>lambdaQuery()
                .eq(MpStructureAllocation::getFactoryCode, createCondition.getFactoryCode())
                .eq(MpStructureAllocation::getYear, createCondition.getYear())
                .eq(MpStructureAllocation::getMonth, createCondition.getMonth())
                .eq(MpStructureAllocation::getMonthPlanVersion, createCondition.getMonthPlanVersion())
                .eq(MpStructureAllocation::getProductionVersion, createCondition.getProductionVersion())
                .eq(MpStructureAllocation::getIsDelete, YesOrNoEnum.NO.getCode());
        List<MpStructureAllocation> list = this.entityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptySet();
        }
        return list.stream().map(MpStructureAllocation::getStructureName).collect(Collectors.toSet());
    }


    /**
     * 获取结构转产表导出数据
     *
     * @param param
     * @param isFinal
     * @return
     */
    @Override
    public MpStructureAllocationExportStatisticsVo getExportVo(MpStructureAllocation param, boolean isFinal) {
        // 0、各项参数初始化
        Integer changeStructDecLhMachines = (Integer)this.getFactorParam(param.getFactoryCode(), MonthPlanEnums.CHANGE_STRUCT_DEC_LH_MACHINES); // 成型机在结构切换时，首日应减少硫化机台数
        // 1、加载构建导出列表的各项数据
        // 1.1、加载硫化机总数
        LambdaQueryWrapper<LhMachineInfo> lhMachineQueryWrapper = new LambdaQueryWrapper<>();
        lhMachineQueryWrapper.eq(LhMachineInfo::getFactoryCode, param.getFactoryCode());
        Integer lhmachineCount = LhMachineInfoCalculateHelper
                .getLhMachineCount(lhMachineInfoEntityMapper.selectList(lhMachineQueryWrapper));
        // 1.2、加载月计划结构转产表明细
        List<MpStructureAllocationExportVo> recordList = getStructureAllocationInfo(param, isFinal);
        // 1.3、加载本次版本已生成的统计记录 20260608+ 统计数据取值修改
        Map<String, MpMonthPlanStatistics> statisticsMap = monthPlanStatisticsService.getStatisticsInfo(param.getFactoryCode(), param.getProductionVersion(), isFinal);
        
        // 1.3.1、从日历获取上个月月底日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(param.getYear(), param.getMonth() - 1, FactoryConstant.MONTH_START_DAY);
        Integer monthMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_MONTH, -1); // 切换到上个月最后一天
        Integer lastMonthMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        Integer lastYear = calendar.get(Calendar.YEAR);
        Integer lastMonth = calendar.get(Calendar.MONTH) + 1;
        // 1.3.1.1、获取上个月的工作日历
        Set<Integer> lastWorkCalendar = this.getOpenWorkCalendar(param.getFactoryCode(), lastYear, lastMonth);
        // 1.3.1.2、获取本月的工作日历
        Set<Integer> workCalendar = this.getOpenWorkCalendar(param.getFactoryCode(), param.getYear(), param.getMonth());
        // 1.3.2、按结构 + 日期 统计硫化机台数
        Map<String, Map<Integer, Integer>> lhMachineStatisticsMap = this.buildLhMachineStatiseicsMap(statisticsMap,
                monthMaxDay);
        // 1.3.3、加载周期结构
        LambdaQueryWrapper<MdmCycleSchStruConf> mdmCycleSchStruConfQueryWrapper = new LambdaQueryWrapper<>();
        mdmCycleSchStruConfQueryWrapper.eq(MdmCycleSchStruConf::getFactoryCode, param.getFactoryCode());
        Set<String> cycleSchStruSet = mdmCycleSchStruConfEntityMapper.selectList(mdmCycleSchStruConfQueryWrapper).stream().map(MdmCycleSchStruConf::getStructureName).distinct().collect(Collectors.toSet());
        
        // 1.4 补充日计划信息与上月最后10天的定稿信息
        // 1.4.1、加载月计划排产明细，根据参数决定加载月计划还是定稿版本，一个结构一份
        Map<String, FactoryMonthPlanMouldDayResult> structureDayResultMap = this.loadStructureDayResultMap(param, isFinal, monthMaxDay);
        // 1.4.2、加载上个月定稿信息，一个结构一份
        Map<String, FactoryMonthPlanProductionFinalResult> lastStructureDayResultMap = this.loadLastStructureDayResultMap(param, lastYear, lastMonth);
        // 1.4.3、将上月定稿信息填充到列表中，包括将上月定稿有本但月没有的结构也添加到结构列表中
        Map<Integer, Integer> lastTotalMap = this.fillLastFinalResultList(param, recordList, lastStructureDayResultMap, lastMonthMaxDay);
        
        // 1.5、加载需求计划
        Map<String, DpDemandPlan> dpDemandPlanMap = this.loadDemandPlanMap(param);

        // 2、构建报表头
        MpStructureAllocationExportStatisticsVo exportVo = new MpStructureAllocationExportStatisticsVo();
        exportVo.setFactoryCode(param.getFactoryCode());
        exportVo.setYear(param.getYear());
        exportVo.setMonth(param.getMonth());
        exportVo.setMonthPlanVersion(param.getMonthPlanVersion());
        exportVo.setProductionVersion(param.getProductionVersion());
        if (!CollectionUtils.isEmpty(structureDayResultMap)) {
            String productTypeCode = structureDayResultMap.values().iterator().next().getProductTypeCode(); // 取产品类型
            exportVo.setProductTypeCode(productTypeCode);
        }
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
        for (int day = 1; day <= monthMaxDay; day++) {  // 初始化汇总map
            totalMap.put(day, 0);
        }
        // 遍历
        for (int i = 0, size = recordList.size(); i < size; i++) {
            // 3.2.1、把同机台的排产记录添加到列表中，全部添加完后开始处理这一批数据
            MpStructureAllocationExportVo record = recordList.get(i);
            machineStructureList.add(record); // 先添加到列表
            cxMachineCode = record.getCxMachineCode(); // 更新机台
            // 3.2.2、下一笔机台没有变化，且还不是最后一笔记录，继续遍历下一笔数据
            if (i < size - 1) { // 还不是最后一行，则校验下一行是否同一个机台
                MpStructureAllocationExportVo nextRecord = recordList.get(i + 1);
                if (cxMachineCode.equals(nextRecord.getCxMachineCode())) { // 机台没有变化，则添继续往下
                    continue;
                }
            }
            // 3.2.3、处理列表明细的数据
            Integer changeRank = 1; // 切换序号，用于导出的切换颜色渲染
            for (MpStructureAllocationExportVo machineRecord : machineStructureList) {
                if (machineRecord.getIsOnlyLast()) { // 仅上月定稿数据的记录跳过不处理
                    machineRecord.setChangeRank(changeRank++); // 设置序号
                    continue;
                }
                String structureName = machineRecord.getStructureName();
                Map<Integer, Integer> dayLhMachinesMap = lhMachineStatisticsMap.get(structureName);
                if (dayLhMachinesMap == null) {
                    continue;
                }
                Integer beginDay = null;
                Integer endDay = null;
                // 3.2.3.1、处理在机天数区间内的硫化机数。另外如果是新上机结构，则最多只能分配上限-扣减机台数
                for (int day = machineRecord.getBeginDay(); day <= machineRecord.getEndDay(); day++) {
                    Integer lhMachines = dayLhMachinesMap.getOrDefault(day, 0);
                    if (lhMachines != null && lhMachines > 0) {
                        Integer realLhMachines = this.calculateRealLhMachines(
                                machineRecord, lhMachines, day, workCalendar, lastWorkCalendar,
                                lastMonthMaxDay, changeStructDecLhMachines);

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
                FactoryMonthPlanMouldDayResult mouldingDayResultAggregated = structureDayResultMap.get(structureName);
                if (mouldingDayResultAggregated != null) {
                    machineRecord.setTotalQty(mouldingDayResultAggregated.getTotalQty()); // 结构总排产量
                    Integer netQty = Optional.ofNullable(machineRecord.getNetQty()).orElse(0); // 净需求
                    Integer differenceQty = Optional.ofNullable(mouldingDayResultAggregated.getTotalQty()).orElse(0); // 未排量 = 净需求 - 总排产量
                    machineRecord.setDifferenceQty(netQty > differenceQty ? netQty - differenceQty : 0); // 未排量小于0的按0算
                    machineRecord.setProductTypeCode(mouldingDayResultAggregated.getProductTypeCode());
                    machineRecord.setProSize(mouldingDayResultAggregated.getProSize());
                }
                
                // 3.2.3.3、处理结构类型
                String structureType;
                if (!CollectionUtils.isEmpty(cycleSchStruSet) && cycleSchStruSet.contains(structureName)) {
                    structureType = ProductionGroupTypeEnum.CYCLE.getGroupType();
                } else {
                    structureType = ProductionGroupTypeEnum.CONVENTION.getGroupType();
                }
                machineRecord.setStructureType(structureType); // 结构类型
                machineRecord.setChangeRank(changeRank++); // 设置序号
                machineRecord.setBeginDay(beginDay);
                machineRecord.setEndDay(endDay);
                DpDemandPlan dpDemandPlan = Optional.ofNullable(dpDemandPlanMap.get(structureName)).orElse(null);
                if (dpDemandPlan != null) {
                    Integer heightQty = Convert.toInt(dpDemandPlan.getOriHeightQty(), Convert.toInt(dpDemandPlan.getHeightQty(), 0));// 高优先级需求量
                    Integer midQty = Convert.toInt(dpDemandPlan.getOriMidQty(), Convert.toInt(dpDemandPlan.getMidQty(), 0));// 中优先级需求量
                    Integer unPostponeNetQty = safeAdd(heightQty, midQty, dpDemandPlan.getCycleReserveQty(), dpDemandPlan.getConventionReserveQty());// 净需求不含暂缓 = 高优先级需求量 + 中优先级需求量 + 周期 + 常规储备
                    Integer lossQty = safeAdd(unPostponeNetQty, dpDemandPlan.getPostponeQty());// 净需求含暂缓 = 净需求不含暂缓 + 暂缓订单数量
                    machineRecord.setLossQty(lossQty);
                    machineRecord.setUnPostponeNetQty(unPostponeNetQty);
                    machineRecord.setHeightQty(heightQty);
                }
                if (beginDay != null && endDay != null) {
                    machineRecord.setAllotDays(endDay - beginDay + 1);
                }
            }
            totalRecordList.addAll(machineStructureList);
            machineStructureList.clear(); // 处理完一个机台后清空列表
        }
        // 3.3、部分特殊规格总硫化机数会超过成型机 * 最大硫化机数，因此会有剩余，剩余的部分需要重新分配到各机台当天有排产的记录中
        this.handleOverLimitMachine(lhMachineStatisticsMap, totalRecordList, totalMap, DAY_FIELD_NAME_FORMAT, FactoryConstant.MONTH_START_DAY);
        // 3.4.1、更新统计行数值
        for (Entry<Integer, Integer> entry : totalMap.entrySet()) {
            Integer day = entry.getKey();
            Integer realLhMachines = entry.getValue();
            String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
            this.updateExportDayField(totalRecord, dayFieldName, realLhMachines); // 累加记录
            this.updateExportDayField(maxProductQtyRecord, dayFieldName, lhmachineCount); // 填充最大产能数值 = 硫化机总数
            this.updateExportDayField(enableCountRecord, dayFieldName, lhmachineCount - realLhMachines); // 可用机台数 = 排产合计 - 最大产能
        }
        // 3.4.2、更新上月定稿统计数值
        for (Entry<Integer, Integer> entry : lastTotalMap.entrySet()) {
            Integer day = entry.getKey();
            Integer realLhMachines = entry.getValue();
            String dayFieldName = String.format(LAST_DAY_FIELD_NAME_FORMAT, day);
            this.updateExportDayField(totalRecord, dayFieldName, realLhMachines); // 累加记录
            this.updateExportDayField(maxProductQtyRecord, dayFieldName, lhmachineCount); // 填充最大产能数值 = 硫化机总数
            this.updateExportDayField(enableCountRecord, dayFieldName, lhmachineCount - realLhMachines); // 可用机台数 = 排产合计 - 最大产能
        }
        
        // 3.5、先顺序反转，再添加合计行
//        Collections.reverse(totalRecordList);
        totalRecordList.add(totalRecord);
        totalRecordList.add(maxProductQtyRecord);
        totalRecordList.add(enableCountRecord);
        exportVo.setRecordList(totalRecordList);
        // 4、构建切换数统计信息
        this.buildChangeStructureStatistics(recordList, exportVo);

        // 5、构建头部合计行
        this.buildHeadStatistics(monthMaxDay, structureDayResultMap, lastStructureDayResultMap, exportVo);
        
        return exportVo;
    }

    /**
     * 
     * 获取月计划排产参数
     * @param factoryCode
     * @param paramCode
     * @return
     */
    private Object getFactorParam(String factoryCode, ProductTypeEnum productType, MonthPlanEnums paramCode) {
        List<String> paramCodeList = Collections.singletonList(paramCode.getCode());
        Context context = new Context();
        context.setFactoryCode(factoryCode);
        context.setProductType(productType);
        return productionMdmDataService.getFactoryParamByCondition(context, paramCodeList).get(paramCode.getCode());
    }

    /**
     * 
     * 获取月计划排产参数
     * @param factoryCode
     * @param paramCode
     * @return
     */
    private Object getFactorParam(String factoryCode, MonthPlanEnums paramCode) {
        return this.getFactorParam(factoryCode, ProductTypeEnum.WHOLE_STEEL, paramCode);
    }

    /**
     * 部分特殊规格总硫化机数会超过成型机 * 最大硫化机数，因此会有剩余，剩余的部分需要重新分配到最后一行当天有排产的记录中
     * @param lhMachineStatisticsMap   硫化机统计总列表 
     * @param recordList   明细列表
     * @param totalMap  汇总列表
     * @param fieldName 更新字段名
     * @param startDay  开始处理日期
     */
    private void handleOverLimitMachine(Map<String, Map<Integer, Integer>> lhMachineStatisticsMap,
            List<MpStructureAllocationExportVo> recordList, Map<Integer, Integer> totalMap, String fieldName,
            Integer startDay) {
        for (Entry<String, Map<Integer, Integer>> statisticsEntry : lhMachineStatisticsMap.entrySet()) {
            String structureName = statisticsEntry.getKey();
            
            // 需要从上到下逐台成型机分配，每次分配一台，全部成型机分配一轮后如果还有剩余，需要再执行一轮，直到将所有机台分配完成
            Map<Integer, Integer> lhMachineDayMap = statisticsEntry.getValue();
            for (Entry<Integer, Integer> lhMachineDayEntry : lhMachineDayMap.entrySet()) {
                Integer day = lhMachineDayEntry.getKey();
                Integer remainLhMachine = Convert.toInt(lhMachineDayEntry.getValue(), 0);
                if (day < startDay) {
                    continue;
                }
                while (remainLhMachine > 0) {
                    String dayFieldName = String.format(fieldName, day);
                    boolean isAdd = false;
                    for (MpStructureAllocationExportVo record: recordList) {
                        if (!Objects.equals(structureName, record.getStructureName())) {
                            continue;
                        }
                        // 检查当天是否有排产，有则加上1，没有则看下一笔记录
                        int lhMachineCount = Convert.toInt(record.getFieldValueByFieldName(dayFieldName), 0);
                        if (lhMachineCount > 0) {
                            record.setFieldValueByFieldName(dayFieldName, lhMachineCount + 1);
                            totalMap.put(day, totalMap.getOrDefault(day, 0) + 1);
                            remainLhMachine--;
                            isAdd = true;
                            break;
                        }
                    }
                    // 一次循环如果一个都没加上，说明已经无法继续添加，直接跳出，防止死循环
                    if (!isAdd) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 构建导出模板的头部合计行
     * @param monthMaxDay
     * @param structureDayResultMap
     * @param lastStructureDayResultMap
     * @param exportVo
     */
    private void buildHeadStatistics(Integer monthMaxDay,
            Map<String, FactoryMonthPlanMouldDayResult> structureDayResultMap,
            Map<String, FactoryMonthPlanProductionFinalResult> lastStructureDayResultMap,
            MpStructureAllocationExportStatisticsVo exportVo) {
        // 5.1、本月排产字段汇总
        MpStructureAllocationExportVo totalProductRecord = this.createExportRecord(StructureAllocationExportDataTypeEnum.TOTAL_PRODUCT_QTY);
        for (FactoryMonthPlanMouldDayResult result : structureDayResultMap.values()) {
            for (int day = 1; day <= monthMaxDay; day++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                Integer value = Optional.ofNullable((Integer) result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                if (value > 0) {
                    Integer sumValue = Optional.ofNullable((Integer) totalProductRecord.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    totalProductRecord.setFieldValueByFieldName(dayFieldName, sumValue + value);
                }
            }
        }
        // 5.2、上月月定稿字段汇总
        for (FactoryMonthPlanProductionFinalResult result : lastStructureDayResultMap.values()) {
            for (int day = FactoryConstant.MONTH_MAX_DAY - LAST_MONTH_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                String lastDayFieldName = String.format(LAST_DAY_FIELD_NAME_FORMAT, day);
                Integer value = Optional.ofNullable((Integer) result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                if (value > 0) {
                    Integer sumValue = Optional.ofNullable((Integer) totalProductRecord.getFieldValueByFieldName(lastDayFieldName)).orElse(0);
                    totalProductRecord.setFieldValueByFieldName(lastDayFieldName, sumValue + value);
                }
            }
        }
        exportVo.setHeadList(Collections.singletonList(totalProductRecord));
    }

    /**
     * 构建导出模板的切换数子表
     * @param recordList
     * @param exportVo
     */
    private void buildChangeStructureStatistics(List<MpStructureAllocationExportVo> recordList,
            MpStructureAllocationExportStatisticsVo exportVo) {
        Map<String, List<MpStructureAllocationExportVo>> cxMachineExportMap = recordList.stream()
                .collect(Collectors.groupingBy(MpStructureAllocationExportVo::getCxMachineCode)); // 按机台分好组
        Map<Integer, Integer> changeStructureCountMap = new HashMap<>(); // 记录统计的规格切换次数，key切换次数，value该切换次数的机台数
        
        // 4.1、遍历每个机台的结构排产记录，统计相关数据
        for (Entry<String, List<MpStructureAllocationExportVo>> entry : cxMachineExportMap.entrySet()) {
//            String machineCode = entry.getKey();
            List<MpStructureAllocationExportVo> cxMachineExportList = entry.getValue();
            // 统计结构切换次数
            Integer changeStructureCount = (int) cxMachineExportList.stream()
                    .filter(sa -> !sa.getIsOnlyLast() // 排除仅掉上月定稿存在的结构
                            && !AlternativeTypeEnum.CONTINUE.getCode().equals(sa.getAlternatingType())) // 排除掉续作结构
                    .count();
            if (changeStructureCount > 0) {
                Integer oldCount = changeStructureCountMap.getOrDefault(changeStructureCount, 0);
                changeStructureCountMap.put(changeStructureCount, oldCount + 1);
            }
        }
        // 4.2、统计英寸交替次数
        Integer proSizeChangeCount = (int) recordList.stream()
                .filter(sa -> !sa.getIsOnlyLast() && AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode().equals(sa.getAlternatingType()))
                .count();
        exportVo.setProSizeChangeCount(proSizeChangeCount);
        // 4.3、统计规格交替次数
        Integer structureChangeCount = (int) recordList.stream()
                .filter(sa -> !sa.getIsOnlyLast() && AlternativeTypeEnum.STRUCT_ALTERNATIVE.getCode().equals(sa.getAlternatingType()))
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
    }

    /**
     * 加载需求计划
     * @param param
     * @return
     */
    private Map<String, DpDemandPlan> loadDemandPlanMap(MpStructureAllocation param) {
        QueryWrapper<DpDemandPlan> dpDemandPlanQueryWrapper = new QueryWrapper<>();
        dpDemandPlanQueryWrapper.eq("FACTORY_CODE", param.getFactoryCode());
        dpDemandPlanQueryWrapper.eq("MONTH_PLAN_VERSION", param.getMonthPlanVersion());
        List<DpDemandPlan> dpDemandPlanList = dpDemandPlanEntityMapper.selectList(dpDemandPlanQueryWrapper);
        // 按结构累计需求量
        Map<String, DpDemandPlan> dpDemandPlanMap = dpDemandPlanList.stream().map(dpDemandPlan -> {
            if (!Objects.equals(YesOrNoEnum.YES.getCode(), dpDemandPlan.getIsSchedule())) {
                dpDemandPlan.setConventionReserveQty(0); // 不参与排产，搭配需要清0
            }
            return dpDemandPlan;
        }).collect(Collectors.groupingBy(DpDemandPlan::getStructureName,
                Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().reduce((p1, p2) -> {
                    p1.setHeightQty(safeAdd(p1.getHeightQty(), p2.getHeightQty())); // 高优先级数量
                    p1.setOriHeightQty(safeAdd(p1.getOriHeightQty(), p2.getOriHeightQty())); // 高优先级数量原始值
                    p1.setMidQty(safeAdd(p1.getHeightQty(), p2.getMidQty())); // 中优先级数量
                    p1.setOriMidQty(safeAdd(p1.getOriMidQty(), p2.getOriMidQty())); // 中优先级数量原始值
                    p1.setCycleReserveQty(safeAdd(p1.getCycleReserveQty(), p2.getCycleReserveQty())); // 周期储备量
                    p1.setConventionReserveQty(safeAdd(p1.getConventionReserveQty(), p2.getConventionReserveQty())); // 常规储备量
                    p1.setPostponeQty(safeAdd(p1.getPostponeQty(), p2.getPostponeQty())); // 展缓订单量
                    return p1;
                }).orElse(null))));
        return dpDemandPlanMap;
    }

    /**
     * 按结构 + 日期 统计硫化机台数
     * @param statisticsMap
     * @param monthMaxDay
     * @return
     */
    private Map<String, Map<Integer, Integer>> buildLhMachineStatiseicsMap(
            Map<String, MpMonthPlanStatistics> statisticsMap, Integer monthMaxDay) {
        Map<String, Map<Integer, Integer>> lhMachineStatisticsMap = new HashMap<>();
        for (Entry<String, MpMonthPlanStatistics> entry : statisticsMap.entrySet()) {
            Map<Integer, Integer> dayLhMachinesMap = new HashMap<>();
            MpMonthPlanStatistics statistics = entry.getValue();
            for (int day = 1; day <= monthMaxDay; day++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                String dayStatisticsStr = (String) statistics.getFieldValueByFieldName(dayFieldName);
                if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                    MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
                    dayLhMachinesMap.put(day, dayStatistics.getLhMachines());
                }
            }
            lhMachineStatisticsMap.put(entry.getKey(), dayLhMachinesMap);
        }
        return lhMachineStatisticsMap;
    }

    /**
     * 加载各
     * @param param
     * @param isFinal
     * @param monthMaxDay
     * @return
     */
    private Map<String, FactoryMonthPlanMouldDayResult> loadStructureDayResultMap(MpStructureAllocation param,
            boolean isFinal, Integer monthMaxDay) {
        Map<String, List<FactoryMonthPlanMouldDayResult>> mouldingDayResultMap;
        if (isFinal) { // 定稿
            LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> resultQueryWrapper = new LambdaQueryWrapper<>();
            resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode());
            resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion());
            List<FactoryMonthPlanProductionFinalResult> finalResultList = factoryMonthPlanProductionFinalResultEntityMapper.selectList(resultQueryWrapper);
            mouldingDayResultMap = finalResultList.stream().map(finalResult -> {
                FactoryMonthPlanMouldDayResult result = new FactoryMonthPlanMouldDayResult();
                BeanUtil.copyProperties(finalResult, result);
                return result;
            }).collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName));
        } else {
            LambdaQueryWrapper<FactoryMonthPlanMouldDayResult> resultQueryWrapper = new LambdaQueryWrapper<>();
            resultQueryWrapper.eq(FactoryMonthPlanMouldDayResult::getFactoryCode, param.getFactoryCode());
            resultQueryWrapper.eq(FactoryMonthPlanMouldDayResult::getProductionVersion, param.getProductionVersion());
            mouldingDayResultMap = factoryMouldingDayResultMapper
                    .selectList(resultQueryWrapper).stream()
                    .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getStructureName)); // 按结构对排产结果分组
        }
        
        // 1.4.1、根据结构将月计划明细汇总
        Map<String, FactoryMonthPlanMouldDayResult> structureDayResultMap = new HashMap<>();
        for (Entry<String, List<FactoryMonthPlanMouldDayResult>> entry : mouldingDayResultMap.entrySet()) {
            String structureName = entry.getKey();
            FactoryMonthPlanMouldDayResult mouldingDayResultAggregated = null;
            for (FactoryMonthPlanMouldDayResult result : entry.getValue()) {
                if (mouldingDayResultAggregated == null) {
                    mouldingDayResultAggregated = result;
                    continue;
                }
                // 1.4.1.1、统计结构每日排产量
                for (int day = 1; day <= monthMaxDay; day++) {
                    String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                    Integer sumValue = Optional.ofNullable((Integer) mouldingDayResultAggregated.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    Integer value = Optional.ofNullable((Integer) result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    mouldingDayResultAggregated.setFieldValueByFieldName(dayFieldName, sumValue + value);
                }
                // 1.4.1.2、统计结构总排产量
                Integer sumTotalQty = Optional.ofNullable(mouldingDayResultAggregated.getTotalQty()).orElse(0);
                Integer totalQty = Optional.ofNullable(result.getTotalQty()).orElse(0);
                mouldingDayResultAggregated.setTotalQty(sumTotalQty + totalQty);
            }
            structureDayResultMap.put(structureName, mouldingDayResultAggregated);
        }
        return structureDayResultMap;
    }
    

    /**
     * 加载上个月的定稿记录信息
     * 
     * @param param
     * @param lastYear
     * @param lastMonth
     */
    private Map<String, FactoryMonthPlanProductionFinalResult> loadLastStructureDayResultMap(
            MpStructureAllocation param, Integer lastYear, Integer lastMonth) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> resultQueryWrapper = new LambdaQueryWrapper<>();
        resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode());
        resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getYear, lastYear);
        resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getMonth, lastMonth);
        resultQueryWrapper.eq(StringUtils.isNotEmpty(param.getStructureName()), FactoryMonthPlanProductionFinalResult::getStructureName, param.getStructureName());
        Map<String, List<FactoryMonthPlanProductionFinalResult>> mouldingDayResultMap = factoryMonthPlanProductionFinalResultEntityMapper
                .selectList(resultQueryWrapper).stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getStructureName)); // 按结构对排产结果分组

        // 1.4.1、根据结构将月计划明细汇总
        Map<String, FactoryMonthPlanProductionFinalResult> structureDayResultMap = new HashMap<>();
        for (Entry<String, List<FactoryMonthPlanProductionFinalResult>> entry : mouldingDayResultMap.entrySet()) {
            String structureName = entry.getKey();
            FactoryMonthPlanProductionFinalResult mouldingDayResultAggregated = null;
            for (FactoryMonthPlanProductionFinalResult result : entry.getValue()) {
                if (mouldingDayResultAggregated == null) {
                    mouldingDayResultAggregated = result;
                    continue;
                }
                // 1.4.1.1、统计结构每日排产量
                for (int day = FactoryConstant.MONTH_MAX_DAY - LAST_MONTH_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
                    String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                    Integer sumValue = Optional.ofNullable((Integer) mouldingDayResultAggregated.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    Integer value = Optional.ofNullable((Integer) result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    mouldingDayResultAggregated.setFieldValueByFieldName(dayFieldName, sumValue + value);
                }
                // 1.4.1.2、统计结构总排产量
                Integer sumTotalQty = Optional.ofNullable(mouldingDayResultAggregated.getTotalQty()).orElse(0);
                Integer totalQty = Optional.ofNullable(result.getTotalQty()).orElse(0);
                mouldingDayResultAggregated.setTotalQty(sumTotalQty + totalQty);
            }
            structureDayResultMap.put(structureName, mouldingDayResultAggregated);
        }
        return structureDayResultMap;
    }
    
    /**
     * 填充上个月的定稿记录信息，并返回上个月最后一天的日期
     * @param param
     * @param recordList
     * @param lastStructureDayResultMap
     * @param lastMonthMaxDay
     * @return
     */
    private Map<Integer, Integer> fillLastFinalResultList(MpStructureAllocation param, List<MpStructureAllocationExportVo> recordList,
            Map<String, FactoryMonthPlanProductionFinalResult> lastStructureDayResultMap, Integer lastMonthMaxDay) {
        Map<Integer, Integer> totalMap = new HashMap<>(); // 汇总map，用于记录每天的机台合计值
        Integer realStartDay = FactoryConstant.MONTH_MAX_DAY - LAST_MONTH_DAY; // 实际开始日期
        for (int day = realStartDay; day <= lastMonthMaxDay; day++) {  // 初始化汇总map
            totalMap.put(day, 0);
        }
        if (CollectionUtils.isEmpty(lastStructureDayResultMap)) {
            return totalMap;
        }
        // 加载上个月定稿版本对应的结构转产表
        String prodductionVersion = lastStructureDayResultMap.values().iterator().next().getProductionVersion();
        String factoryCode = param.getFactoryCode();
        LambdaQueryWrapper<MpStructureAllocation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpStructureAllocation::getFactoryCode, factoryCode);
        queryWrapper.eq(MpStructureAllocation::getProductionVersion, prodductionVersion);
        queryWrapper.orderBy(true, false, MpStructureAllocation::getBeginDay); // 倒序排序
        List<MpStructureAllocation> lastStructureList = entityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(lastStructureList)) {
            return totalMap;
        }
        // 按结构+机台构建映射对本月结构转产表愤分组
        Map<String, MpStructureAllocationExportVo> recordMap = recordList.stream()
                .collect(Collectors.toMap(
                        r -> GenerageMapKeyUtils.createMapKey(r.getStructureName(), r.getCxMachineCode()),
                        Function.identity(), (r1, r2) -> r1));
        
        // 加载版上个月定稿版本统计记录、
        Map<String, MpMonthPlanStatistics> statisticsMap = monthPlanStatisticsService.getStatisticsInfo(factoryCode, prodductionVersion, true);
        // 按结构 + 日期 统计硫化机台数
        Map<String, Map<Integer, Integer>> lhMachineStatisticsMap = this.buildLhMachineStatiseicsMap(statisticsMap,
                FactoryConstant.MONTH_MAX_DAY);
        // 加载周期结构
        LambdaQueryWrapper<MdmCycleSchStruConf> mdmCycleSchStruConfQueryWrapper = new LambdaQueryWrapper<>();
        mdmCycleSchStruConfQueryWrapper.eq(MdmCycleSchStruConf::getFactoryCode, factoryCode);
        Set<String> cycleSchStruSet = mdmCycleSchStruConfEntityMapper.selectList(mdmCycleSchStruConfQueryWrapper).stream().map(MdmCycleSchStruConf::getStructureName).distinct().collect(Collectors.toSet());
        // 加载成型机
        LambdaQueryWrapper<MdmMoldingMachine> moldingMachineQueryWrapper = new LambdaQueryWrapper<>();
        moldingMachineQueryWrapper.eq(MdmMoldingMachine::getFactoryCode, factoryCode);
        Map<String, String> cxMachineTypeCodeMap = moldingMachineEntityMapper.selectList(moldingMachineQueryWrapper)
                .stream().collect(Collectors.toMap(MdmMoldingMachine::getCxMachineCode,
                        MdmMoldingMachine::getCxMachineTypeCode, (m1, m2) -> m1));

        // 未匹配的 lastFinalResult 新增到 recordList
        for (MpStructureAllocation structureAllocation : lastStructureList) {
            String key = GenerageMapKeyUtils.createMapKey(structureAllocation.getStructureName(), structureAllocation.getCxMachineCode());
            MpStructureAllocationExportVo record = recordMap.get(key);
            
            boolean isNewRecord = record == null;
            if (isNewRecord) {
                // 创建新记录，复制基础字段
                record = new MpStructureAllocationExportVo();
                BeanUtil.copyProperties(structureAllocation, record);
                // 结构类型
                String structureType;
                if (!CollectionUtils.isEmpty(cycleSchStruSet) && cycleSchStruSet.contains(record.getStructureName())) {
                    structureType = ProductionGroupTypeEnum.CYCLE.getGroupType();
                } else {
                    structureType = ProductionGroupTypeEnum.CONVENTION.getGroupType();
                }
                record.setBeginDay(null);
                record.setEndDay(null);
                record.setAllotDays(null);
                record.setStructureType(structureType);
                record.setDataType(StructureAllocationExportDataTypeEnum.RECORD.getCode());
                record.setCxMachineTypeCode(cxMachineTypeCodeMap.get(record.getCxMachineCode()));
                record.setIsOnlyLast(true);
            }
            Integer beginDady = intValue(structureAllocation.getBeginDay());
            Integer endDady = intValue(structureAllocation.getEndDay());
            String structureName = structureAllocation.getStructureName();
            Map<Integer, Integer> dayLhMachinesMap = lhMachineStatisticsMap.get(structureName);
            if (dayLhMachinesMap == null) {
                continue;
            }
            // 处理在机天数区间内的硫化机数
            Integer totalQty = 0;
            for (Integer day: totalMap.keySet()) {
                // 非分配日的跳过
                if (day < beginDady || endDady < day) {
                    continue;
                }
                record.setLastBeginDay(Math.max(day, beginDady));
                Integer lhMachines = dayLhMachinesMap.getOrDefault(day, 0);
                if (lhMachines != null && lhMachines > 0) {
                    Integer realLhMachines = Math.min(record.getMaxLhMachineCount(), lhMachines);
                    String dayFieldName = String.format(LAST_DAY_FIELD_NAME_FORMAT, day);
                    totalQty += realLhMachines;
                    dayLhMachinesMap.put(day, lhMachines - realLhMachines);
                    this.updateExportDayField(record, dayFieldName, realLhMachines); // 更新明细
                    totalMap.put(day, totalMap.getOrDefault(day, 0) + realLhMachines); // 更新汇总map
                }
            }
            
            // 最后10天有排产的才添加到列表中
            if (totalQty > 0) {
                if (isNewRecord) { // 上月有生产本月无生产的规格
                    // 查找插入位置：相同 机台 的第一个元素之前
                    int insertIndex = -1;
                    for (int i = 0, size = recordList.size(); i < size; i++) {
                        if (Objects.equals(structureAllocation.getCxMachineCode(), recordList.get(i).getCxMachineCode())) {
                            insertIndex = i;
                            break;
                        }
                    }
                    if (insertIndex == -1) {
                        // 没有相同 机台 的记录，插到末尾
                        recordList.add(record);
                    } else {
                        recordList.add(insertIndex, record);
                    }
                } else { // 上月本月都有生产的规格
                    // 优先按上月的顺序重新调整顺序：相同 机台，上月开始时间比当前规格小的最后一个规格之后
                    for (int i = 0, size = recordList.size(); i < size; i++) {
                        MpStructureAllocationExportVo oldRecord = recordList.get(i);
                        if (!Objects.equals(structureAllocation.getCxMachineCode(), oldRecord.getCxMachineCode())) {
                            continue;
                        }
                        // 由于调整顺序只存在提前的场景，因此若没有找到可整位置就遍历到自己，直接结束
                        if (record == oldRecord) {
                            break;
                        }
                        // 如果遍历结构上月没有生产，或者遍历结构的上机日晚于当前结构，则记录遍历结构的下标，然后继续往后遍历，找到自己后调整位置
                        if (oldRecord.getLastBeginDay() == null
                                || oldRecord.getLastBeginDay() > record.getLastBeginDay()) {
                            for (int j = i + 1; j < size; j++) {
                                MpStructureAllocationExportVo tempRecord = recordList.get(j);
                                if (record == tempRecord) {
                                    recordList.add(i, recordList.remove(j)); // 找到当前结构后直接调整位置
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        // 部分特殊规格总硫化机数会超过成型机 * 最大硫化机数，因此会有剩余，剩余的部分需要重新分配到第一行当天有排产的记录中
        this.handleOverLimitMachine(lhMachineStatisticsMap, recordList, totalMap, LAST_DAY_FIELD_NAME_FORMAT, realStartDay);
        return totalMap;
    }

    /**
     * 获取结构排产分配信息
     * 如果isFinalAdjust = true，直接从t_mp_structure_allocation表获取
     * 否则，先从
     *
     * @param param         查询条件
     * @param isFinalAdjust 是否月计划调整入口
     * @return
     */
    private List<MpStructureAllocation> getStructureAllocationData(MpStructureAllocation param, boolean isFinalAdjust) {
        String productionVersion = param.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return Collections.emptyList();
        }
        if (isFinalAdjust) {
            QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
            builderCondition(queryWrapper, param);
            queryWrapper.orderByAsc("BEGIN_DAY");
            return entityMapper.selectList(queryWrapper);
        }
        QueryWrapper<MpFinalStructureAllocationLog> logQuery = new QueryWrapper<>();
        logQuery.eq("FACTORY_CODE", param.getFactoryCode());
        logQuery.eq("YEAR", param.getYear());
        logQuery.eq("MONTH", param.getMonth());
        logQuery.eq("PRODUCTION_VERSION", productionVersion);
        logQuery.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        String monthPlanVersion = param.getMonthPlanVersion();
        logQuery.eq(StringUtils.isNotBlank(monthPlanVersion), "MONTH_PLAN_VERSION", monthPlanVersion);
        List<MpFinalStructureAllocationLog> backUpList = finalStructureAllocationLogEntityMapper.selectList(logQuery);
        if (!CollectionUtils.isEmpty(backUpList)) {
            String structureName = param.getStructureName();
            String cxMachineCode = param.getCxMachineCode();
            if (StringUtils.isBlank(structureName) && StringUtils.isBlank(cxMachineCode)) {
                return BeanCopyUtils.copyBeanList(backUpList, MpStructureAllocation.class);
            }
            logQuery.like(StringUtils.isNotBlank(structureName), "STRUCTURE_NAME", structureName);
            logQuery.like(StringUtils.isNotBlank(cxMachineCode), "CX_MACHINE_CODE", cxMachineCode);
            List<MpFinalStructureAllocationLog> realDataList = finalStructureAllocationLogEntityMapper.selectList(logQuery);
            if (CollectionUtils.isEmpty(realDataList)) {
                return Collections.emptyList();
            }
            return BeanCopyUtils.copyBeanList(realDataList, MpStructureAllocation.class);
        }
        QueryWrapper<MpStructureAllocation> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, param);
        queryWrapper.orderByAsc("BEGIN_DAY");
        return entityMapper.selectList(queryWrapper);
    }

    /**
     * 获取结构排产分配信息
     * 如果isFinalAdjust = true，直接从t_mp_structure_allocation表获取
     * 否则，先从
     *
     * @param param         查询条件
     * @param isFinalAdjust 是否月计划调整入口
     * @return
     */
    private List<MpStructureAllocationExportVo> getStructureAllocationInfo(MpStructureAllocation param, boolean isFinalAdjust) {
        if (isFinalAdjust) {
            return entityMapper.getExportList(param);
        }

        List<MpStructureAllocationExportVo> recordList = finalStructureAllocationLogEntityMapper.getExportList(param);
        if (!CollectionUtils.isEmpty(recordList)) {
            return recordList;
        }
        return entityMapper.getExportList(param);
    }

    /**
     * 构建指定类型的导出数据行
     *
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
     * 计算当天该结构的实际硫化机分配数
     * <p>
     * 基础值为 min(maxLhMachineCount, lhMachines)；
     * 若上一个工作日该结构无计划（day字段为0或null），则上限扣减 changeStructDecLhMachines。
     * </p>
     *
     * @param machineRecord           结构排产记录
     * @param lhMachines              该结构当天可用的硫化机台数
     * @param day                     当天日期（本月第几天）
     * @param workCalendar            本月工作日历
     * @param lastWorkCalendar        上月工作日历
     * @param lastMonthMaxDay         上月最大天数
     * @param changeStructDecLhMachines 结构切换时扣减机台数
     * @return 实际分配的硫化机台数
     */
    private Integer calculateRealLhMachines(MpStructureAllocationExportVo machineRecord,
            Integer lhMachines, int day, Set<Integer> workCalendar,
            Set<Integer> lastWorkCalendar, Integer lastMonthMaxDay,
            Integer changeStructDecLhMachines) {
        Integer maxLhMachineCount = machineRecord.getMaxLhMachineCount();
        Integer realLhMachines = Math.min(maxLhMachineCount, lhMachines);

        // 上一天没有计划时，当天的硫化机数不能超过上限-扣减机台数
        Integer prevWorkDay = this.findPreviousWorkDay(day, workCalendar, lastWorkCalendar, lastMonthMaxDay);
        if (prevWorkDay != null) {
            boolean prevDayHasPlan;
            if (prevWorkDay > 0) {
                // 当前月：检查 day{prevWorkDay} 字段
                Object prevDayValue = machineRecord.getFieldValueByFieldName(
                        String.format(DAY_FIELD_NAME_FORMAT, prevWorkDay));
                prevDayHasPlan = prevDayValue != null && ((Integer) prevDayValue) > 0;
            } else {
                // 上个月：检查 lastDay{prevMonthDay} 字段
                int prevMonthDay = -prevWorkDay;
                Object prevDayValue = machineRecord.getFieldValueByFieldName(
                        String.format(LAST_DAY_FIELD_NAME_FORMAT, prevMonthDay));
                prevDayHasPlan = prevDayValue != null && ((Integer) prevDayValue) > 0;
            }
            if (!prevDayHasPlan) {
                Integer reducedCap = Math.max(0, maxLhMachineCount - changeStructDecLhMachines);
                realLhMachines = Math.min(realLhMachines, reducedCap);
            }
        }
        return realLhMachines;
    }

    /**
     * 查找指定天的上一个工作日
     * <p>
     * 优先在当前月工作日历中向前查找；若本月前几天都停产，则在上个月工作日历中查找（只看一个月）。
     * </p>
     *
     * @param currentDay      当前天
     * @param workCalendar    本月工作日历集合
     * @param lastWorkCalendar 上月工作日历集合
     * @param lastMonthMaxDay 上个月最大天数
     * @return 上一个工作日（正数为本月天数，负数为上月天数），找不到返回null
     */
    private Integer findPreviousWorkDay(int currentDay, Set<Integer> workCalendar,
            Set<Integer> lastWorkCalendar, Integer lastMonthMaxDay) {
        // 1. 在当前月向前查找
        for (int d = currentDay - 1; d >= 1; d--) {
            if (workCalendar.contains(d)) {
                return d; // 正数表示本月
            }
        }
        // 2. 当前月未找到，在上个月向前查找
        for (int d = lastMonthMaxDay; d >= 1; d--) {
            if (lastWorkCalendar.contains(d)) {
                return -d; // 负数表示上月
            }
        }
        return null;
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
     * @param statisticsVo
     * @return
     */
    @Override
    public byte[] getMpStructureAllocationExportByte(MpStructureAllocationExportStatisticsVo statisticsVo) {


        // 获取模板（立即读取为字节数组，避免 Spring Boot 嵌套 JAR 的 ZipInflaterInputStream 延迟读取报错）
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream;
        try {
            try (InputStream templateIs = classLoader.getResourceAsStream("excelModel/mpStructureAllocationExportTemp.xlsx");
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = templateIs.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                inputStream = new ByteArrayInputStream(baos.toByteArray());
            }
        } catch (IOException e) {
            throw new RuntimeException("读取Excel模板失败", e);
        }

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
        // 上个月月份
        Integer month = statisticsVo.getMonth();
        Calendar calendar = Calendar.getInstance();
        calendar.set(statisticsVo.getYear(), month - 1, 1); // 通过日历获取本月一号的日历
        calendar.add(Calendar.MONTH, -1); // 切换到上个月
        Integer lastMonth = calendar.get(Calendar.MONTH) + 1;

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
        headMap.put("heightQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.heightQty"));
        headMap.put("totalQty", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.totalQty"));
        headMap.put("differenceQty", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.differenceQty"));
        headMap.put("beginDay", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.beginDay"));
        headMap.put("endDay", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.endDay"));
        headMap.put("allotDays", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.allotDays"));
        headMap.put("maxLhMachineCount", I18nUtil.getMessage("ui.data.column.mpStructureAllocation.maxLhMachineCount"));
        int fixColumnCount = headMap.size(); // 固定列列数
        headMap.put("dailyproductionQty", MessageFormat.format(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.dailyproductionQty"), month));
        headMap.put("lastDailyproductionQty", MessageFormat.format(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.dailyproductionQty"), lastMonth));
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
            beginIndex += !CollectionUtils.isEmpty(statisticsVo.getHeadList()) ? statisticsVo.getHeadList().size() : 0; // 如果表头有复制统计行，起始行要往下顺延

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

                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, fixColumnCount - 1, color, true, false, ""));

                    // 根据changeRank设置渐变颜色
                    Integer changeRank = exportVo.getChangeRank();
                    if (changeRank != null && changeRank >= 1) {
                        // 找到第一个和最后一个有值的day列
                        int firstDayWithValue = -1;
                        int lastDayWithValue = -1;
                        Integer[] days = {
                                exportVo.getLastDay21(), exportVo.getLastDay22(), exportVo.getLastDay23(),
                                exportVo.getLastDay24(), exportVo.getLastDay25(), exportVo.getLastDay26(),
                                exportVo.getLastDay27(), exportVo.getLastDay28(), exportVo.getLastDay29(),
                                exportVo.getLastDay30(), exportVo.getLastDay31(),
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
                            // 列从day21开始是第13列（索引从0开始：0~12是前面固定列，day21从第13列开始）
                            int startCol = fixColumnCount + firstDayWithValue;
                            int endCol = fixColumnCount + lastDayWithValue;
                            cellStyleList.add(new CellStyle(rowNum, rowNum, startCol, endCol, colorSelect, true, false, ""));

                        }
                    }
                }
                if (StructureAllocationExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())
                        || StructureAllocationExportDataTypeEnum.MAX_PRODUCT_QTY.getCode().equals(exportVo.getDataType())
                        || StructureAllocationExportDataTypeEnum.ENABLE_COUNT.getCode().equals(exportVo.getDataType())) {
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, headMap.size() + 39, "#DAEEF3", true, true, ""));
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
     *
     * @param exportVo
     * @param factoryMap
     * @param structureTypeMap
     * @param machineBrandMap
     * @param suffix           后缀，用于复制合计行
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
        listDataMap.put(this.getRealFieldName("heightQty", suffix), exportVo.getHeightQty());
        listDataMap.put(this.getRealFieldName("totalQty", suffix), exportVo.getTotalQty());
        listDataMap.put(this.getRealFieldName("differenceQty", suffix), exportVo.getDifferenceQty());
        listDataMap.put(this.getRealFieldName("beginDay", suffix), exportVo.getBeginDay());
        listDataMap.put(this.getRealFieldName("endDay", suffix), exportVo.getEndDay());
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
        listDataMap.put(this.getRealFieldName("lastDay21", suffix), exportVo.getLastDay21());
        listDataMap.put(this.getRealFieldName("lastDay22", suffix), exportVo.getLastDay22());
        listDataMap.put(this.getRealFieldName("lastDay23", suffix), exportVo.getLastDay23());
        listDataMap.put(this.getRealFieldName("lastDay24", suffix), exportVo.getLastDay24());
        listDataMap.put(this.getRealFieldName("lastDay25", suffix), exportVo.getLastDay25());
        listDataMap.put(this.getRealFieldName("lastDay26", suffix), exportVo.getLastDay26());
        listDataMap.put(this.getRealFieldName("lastDay27", suffix), exportVo.getLastDay27());
        listDataMap.put(this.getRealFieldName("lastDay28", suffix), exportVo.getLastDay28());
        listDataMap.put(this.getRealFieldName("lastDay29", suffix), exportVo.getLastDay29());
        listDataMap.put(this.getRealFieldName("lastDay30", suffix), exportVo.getLastDay30());
        listDataMap.put(this.getRealFieldName("lastDay31", suffix), exportVo.getLastDay31());
        return listDataMap;
    }

    /**
     * 获取实际字段名，后缀有值需要拼接上后缀
     *
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
     * 数据导入
     *
     * @param fileBytes
     * @param importLog
     * @return
     */
    @Override
    public AjaxResult importData(byte[] fileBytes, ImportLog importLog) {
        ExcelUtil<MpStructureAllocationExportVo> util = new ExcelUtil<>(MpStructureAllocationExportVo.class);
        // 工厂名称字典
        List<SysDictData> factoryDatas = Optional.ofNullable(sysDictDataCacheService.getType("biz_factory_name")).orElse(Collections.emptyList());
        Map<String, String> factoryMap = factoryDatas.stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getDictLabel() != null && v.getDictValue() != null)
                .collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue, (a, b) -> a));

        List<SysDictData> productTypeList = Optional.ofNullable(sysDictDataCacheService.getType("biz_product_type")).orElse(Collections.emptyList());
        Map<String, String> productTypeMap = productTypeList.stream()
                .filter(Objects::nonNull)
                .filter(v -> v.getDictLabel() != null && v.getDictValue() != null)
                .collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue, (a, b) -> a));

        // 解析Excel文件
        MpStructureAllocationImportHelper helper = this.parseExcel(fileBytes);
        if (AjaxResultUtils.checkAjaxError(helper.getAjaxResult())) {
            return helper.getAjaxResult();
        }
        Map<String, String> structureMachineMap = new HashMap<>();
        String[] params = helper.getParams();
        String[] params4DayResult = helper.getParams4DayResult();
        String monthPlanVersion = helper.getMonthPlanVersion();
        String productVersion = "I" + com.ruoyi.common.core.utils.DateUtils.dateTimeNow();
        int year = helper.getYear();
        int month = helper.getMonth();
        ExcelUtil<FactoryMonthPlanMouldDayResult> util4DayResult = new ExcelUtil<>(FactoryMonthPlanMouldDayResult.class);
        List<FactoryMonthPlanMouldDayResult> list4DayResult;
        List<MpStructureAllocationExportVo> list;
        try {
            list4DayResult = util4DayResult.importExcel(sheetName4DayResult, new ByteArrayInputStream(fileBytes), 3, 1, -1); // 解析月计划数据
            list = util.importExcel(sheetName, new ByteArrayInputStream(fileBytes), 2, 2, 13); // 解析结构转产表数据
        } catch (Exception e) {
            log.warn("importDataStructureAllocation workbook parse failed", e);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateError"));
        }

        // 结构转产导入
        AjaxResult ajaxResult = this.importDataStructureAllocation(list, list4DayResult, false, importLog.getId(), params, monthPlanVersion, productVersion, factoryMap, productTypeMap, structureMachineMap);
        boolean isStrcutreImport = AjaxResultUtils.checkAjaxSuccess(ajaxResult);
        AjaxResult ajaxResult4DayResult = null;
        // 月计划排产导入
        boolean isDayDataImport = isStrcutreImport;
        if (isStrcutreImport) {
            ajaxResult4DayResult = this.importDataDayResult(list, list4DayResult, false, importLog.getId(), params4DayResult, monthPlanVersion, productVersion, factoryMap, productTypeMap, structureMachineMap, false);
            isDayDataImport = AjaxResultUtils.checkAjaxSuccess(ajaxResult4DayResult);
        }
        // 月计划或者结构转产任意一个导入成功都要生成版本
        if (isStrcutreImport || isDayDataImport) {
            // 版本关系存到版本表
            MpFactoryProductionVersion version = new MpFactoryProductionVersion();
            if (factoryMap.containsKey(params[2])) {
                version.setFactoryCode(factoryMap.get(params[2]));
            }
            if (productTypeMap.containsKey(params[3])) {
                version.setProductTypeCode(productTypeMap.get(params[3]));
            }
            version.setYear(year);
            version.setMonth(month);
            version.setMonthPlanVersion(monthPlanVersion);
            version.setProductionVersion(productVersion);
            version.setPlanType("01");
            version.setIsSelectedDemand(YesOrNoEnum.YES.getCode());
            version.setProductionInitVersion(productVersion);
            version.setProductionStVersion(productVersion);
//            version.setIsNaturalMonth("04");
            YearMonth yearMonth = YearMonth.of(year, month);
            version.setProductionStartDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(yearMonth.atDay(FactoryConstant.MONTH_START_DAY)));
            version.setProductionEndDate(com.zlt.aps.mp.engine.utils.DateUtils.getDate(yearMonth.atEndOfMonth()));
            version.setIsFinal(YesOrNoEnum.NO.getCode());
            baseDao.save(version);
            // 21349 把相同需求版本号数据的isSelectedDemand都更新成1
            LambdaUpdateWrapper<MpFactoryProductionVersion> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MpFactoryProductionVersion::getYear, year)
                    .eq(MpFactoryProductionVersion::getMonth, month)
                    .eq(MpFactoryProductionVersion::getFactoryCode, version.getFactoryCode())
                    .eq(MpFactoryProductionVersion::getProductTypeCode, version.getProductTypeCode())
                    .eq(MpFactoryProductionVersion::getMonthPlanVersion, version.getMonthPlanVersion())
                    .set(MpFactoryProductionVersion::getIsSelectedDemand, YesOrNoEnum.YES.getCode());
            mpFactoryProductionVersionMapper.update(null, updateWrapper);
        }

        int errorNum = 0;
        int successNum = 0;
        List<Object> importErrorLogs = new ArrayList<>();
        int[] resultParam = parseImportMsg(ajaxResult);
        if (resultParam[2] > 0) {
            List<ImportErrorLog> importErrorLogList = com.ruoyi.common.utils.StringUtils.cast(ajaxResult.get(AjaxResult.DATA_TAG));
            if (!CollectionUtils.isEmpty(importErrorLogList)) {
                String listTxt = JSONArray.toJSONString(importErrorLogList);
                importErrorLogs.addAll(JSONArray.parseArray(listTxt, ImportErrorLog.class));
            }
        }
        if (ajaxResult4DayResult != null) {
            int[] resultParam4DayResult = parseImportMsg(ajaxResult4DayResult);
            successNum += resultParam4DayResult[0];
            if (resultParam4DayResult[2] > 0) {
                errorNum += resultParam4DayResult[1];

                List<ImportErrorLog> importErrorLogList = com.ruoyi.common.utils.StringUtils.cast(ajaxResult4DayResult.get(AjaxResult.DATA_TAG));
                if (!CollectionUtils.isEmpty(importErrorLogList)) {
                    String listTxt = JSONArray.toJSONString(importErrorLogList);
                    importErrorLogs.addAll(JSONArray.parseArray(listTxt, ImportErrorLog.class));
                }
            }
        } else {
            errorNum = list.size();
        }

        // 构建返回数据
        Integer rowCount = list4DayResult.size();
        Map<String, Object> returnData = new HashMap<>();
        returnData.put("rowCount", rowCount);
        returnData.put("errorNum", errorNum);
        returnData.put("successNum", successNum);
        returnData.put("importErrorLogs", importErrorLogs);
        return AjaxResult.success(returnData);
    }


    /**
     * 解析导入Excel文件
     *
     * @param fileBytes 导入文件字节数组
     * @return
     */
    private MpStructureAllocationImportHelper parseExcel(byte[] fileBytes) {
        MpStructureAllocationImportHelper helper = new MpStructureAllocationImportHelper();
        helper.setAjaxResult(AjaxResult.success());
        String templateErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateError");
        String templateTitleErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.templateTitleError");
        String monthPlanVersionNotMatchErrorStr = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.import.monthPlanVersionNotMatch");
        DataFormatter dataFormatter = new DataFormatter();

        // 初始化月计划调整与结构转产表导出模板信息
        if (!this.initExcelData()) {
            helper.setAjaxResult(AjaxResult.error(templateErrorStr));
            return helper;
        }
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null || sheet.getRow(0) == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            Cell titleCell = sheet.getRow(0).getCell(0);
            if (titleCell == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            // 解析结构转产表页签
            // 解析标题
            String titleFormat = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.exportTitle");
            helper.setParams(parseFormat(titleFormat, dataFormatter.formatCellValue(titleCell)));
            if (helper.getParams() == null || helper.getParams().length < 4) {
                helper.setAjaxResult(AjaxResult.error(templateTitleErrorStr));
                return helper;
            }
            // 解析需求计划版本
            String monthPlanVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.monthPlanVersion") + ":";
            Cell monthPlanVersionCell = sheet.getRow(0).getCell(columnCount - 17);
            if (monthPlanVersionCell == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            helper.setMonthPlanVersion(dataFormatter.formatCellValue(monthPlanVersionCell).replace(monthPlanVersionLabel, "").trim());
            // 解析生产版本
            Cell productVersionCell = sheet.getRow(0).getCell(columnCount - 9);
            if (productVersionCell == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String productionVersionLabel = I18nUtil.getMessage("ui.data.column.mpStructureAllocation.productionVersion") + ":";
            helper.setProductVersion(dataFormatter.formatCellValue(productVersionCell).replace(productionVersionLabel, "").trim());

            // 解析月计划页签
            // 解析标题
            Sheet sheet4DayResult = wb.getSheet(sheetName4DayResult);
            if (sheet4DayResult == null || sheet4DayResult.getRow(0) == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            Cell titleCell4DayResult = sheet4DayResult.getRow(0).getCell(0);
            if (titleCell4DayResult == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String titleFormat4DayResult = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.exportTitle");
            helper.setParams4DayResult(parseFormat(titleFormat4DayResult, dataFormatter.formatCellValue(titleCell4DayResult)));
            if (helper.getParams4DayResult() == null || helper.getParams4DayResult().length < 3) {
                helper.setAjaxResult(AjaxResult.error(templateTitleErrorStr));
                return helper;
            }
            // 解析需求计划版本
            Cell monthPlanVersionCell4DayResult = sheet4DayResult.getRow(0).getCell(columnCount4DayResult - 9);
            if (monthPlanVersionCell4DayResult == null) {
                helper.setAjaxResult(AjaxResult.error(templateErrorStr));
                return helper;
            }
            String monthPlanVersion4Day = dataFormatter.formatCellValue(monthPlanVersionCell4DayResult).replace(monthPlanVersionLabel, "").trim();
            // 校验如果两个页签的需求版本号不一致，报失败
            if (!Objects.equals(monthPlanVersion4Day, helper.getMonthPlanVersion())) {
                helper.setAjaxResult(AjaxResult.error(monthPlanVersionNotMatchErrorStr));
                return helper;
            }
            try {
                helper.setYear(Integer.parseInt(helper.getParams()[0]));
                helper.setMonth(Integer.parseInt(helper.getParams()[1]));
            } catch (NumberFormatException e) {
                helper.setAjaxResult(AjaxResult.error("导入模板标题中的年月格式不正确"));
                return helper;
            }
            if (helper.getMonth() < 1 || helper.getMonth() > 12) {
                helper.setAjaxResult(AjaxResult.error("导入模板标题中的月份范围不正确"));
                return helper;
            }
        } catch (Exception e) {
            log.warn("importDataStructureAllocation workbook parse failed", e);
            helper.setAjaxResult(AjaxResult.error(templateErrorStr));
        }
        return helper;
    }

    /**
     * 初始化月计划调整与结构转产表导出模板信息
     */
    private boolean initExcelData() {
        if (StringUtils.isNotEmpty(sheetName) && StringUtils.isNotEmpty(sheetName4DayResult)) {
            return true;
        }
        ClassLoader classLoader = this.getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("excelModel/mpStructureAllocationExportTemp.xlsx");
                InputStream dayInputStream = classLoader.getResourceAsStream("excelModel/factoryMonthPlanMouldDayResultExportTemp.xlsx");
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                XSSFWorkbook dayWorkbook = new XSSFWorkbook(dayInputStream);) {
            // 结构转产表页签
            XSSFSheet sheet = workbook.getSheetAt(0);
            columnCount = sheet.getRow(1).getLastCellNum();
            sheetName = sheet.getSheetName();
            // 月计划页签
            XSSFSheet daySheet = dayWorkbook.getSheetAt(0);
            columnCount4DayResult = daySheet.getRow(1).getLastCellNum();
            sheetName4DayResult = daySheet.getSheetName();
        } catch (Exception e) {
            log.error("importDataStructureAllocation workbook parse failed", e);
            return false;
        }
        return true;
    }

    private int[] parseImportMsg(AjaxResult ajaxResult) {
        int[] result = new int[]{0, 0, 0};
        if (ajaxResult == null) {
            return result;
        }
        Object msgObj = ajaxResult.get(AjaxResult.MSG_TAG);
        if (msgObj == null) {
            log.warn("import result msg is null");
            return result;
        }
        String[] msgArr = msgObj.toString().split(",");
        if (msgArr.length < 2) {
            log.warn("import result msg format invalid: {}", msgObj);
            return result;
        }
        result[0] = Convert.toInt(msgArr[1], 0);
        if (msgArr.length > 2) {
            result[1] = Convert.toInt(msgArr[2], 0);
            result[2] = 1;
        }
        return result;
    }

    /**
     * 从格式化后的字符串中，反向解析出原始参数
     *
     * @param format       String.format 使用的模板（如 "年份:%d 月份:%d 工厂:%s 产品:%s"）
     * @param formattedStr 格式化后的最终字符串
     * @return 解析出的参数数组，null=解析失败
     */
    private String[] parseFormat(String format, String formattedStr) {
        if (format == null || formattedStr == null) {
            return null;
        }

        // 1. 把 format 模板 转成 正则表达式（核心步骤）
        // 转义正则特殊字符 . * + ? | ( ) [ ] { } \ ^ $
        String regex = format.replaceAll("([.*+?|()\\[\\]{}^$\\\\])", "\\\\$1");

        // 2. 替换所有占位符为 正则捕获组
        // 支持：%d %s %f %tY 等所有常用占位符
        regex = regex.replaceAll("%(?:\\d+\\$)?[+-]?(?:\\d+)?(?:\\.\\d+)?[a-zA-Z]", "(.*?)");

        // 3. 首尾加锚定，确保完全匹配整个字符串
        regex = "^" + regex + "$";

        // 4. 匹配
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(formattedStr);

        if (!matcher.matches()) {
            // 不匹配，解析失败
            return null;
        }

        // 5. 提取所有捕获组（group 0 是整个字符串，从 1 开始）
        String[] params = new String[matcher.groupCount()];
        for (int i = 0; i < params.length; i++) {
            params[i] = matcher.group(i + 1);
        }

        return params;
    }

    /**
     * 导入结构转产表
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
    public AjaxResult importDataStructureAllocation(List<MpStructureAllocationExportVo> list,
                                                    List<FactoryMonthPlanMouldDayResult> list4DayResult, boolean updateSupport, Long importLogId,
                                                    String[] params, String monthPlanVersion, String productVersion, Map<String, String> factoryMap,
                                                    Map<String, String> productTypeMap, Map<String, String> structureMachineMap) {
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

        // 构建每个成型机的当前排产结构
        Map<String, MpStructureAllocation> machineLastValidRecordMap = new HashMap<>();
        Integer importYear = Convert.toInt(year, null);
        Integer importMonth = Convert.toInt(month, null);
        String importFactoryCode = factoryMap.get(factoryName);
        if (importYear != null && importMonth != null && StringUtils.isNotBlank(importFactoryCode)) {
            // 上个月的定稿数据作为初始在产结构数据
            machineLastValidRecordMap = this.getLastMachineStructureMap(importFactoryCode, importYear, importMonth);
        }

        //3.公共校验（非空校验、长度校验等）
        StringBuilder sbError = new StringBuilder(); // 记录校验异常的信息
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 5;
            MpStructureAllocation item = list.get(i);

            item.setDataSource(DataSourceEnum.IMPORT.getCode());
            item.setMonthPlanVersion(monthPlanVersion);
            item.setProductionVersion(productVersion);
            item.setPlanType("01");

            try {
                item.setYear(Integer.parseInt(year));
            } catch (NumberFormatException e) {
                item.setId(errorImportId);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, yearErrorStr, importErrorLogs);
                continue;
            }
            try {
                item.setMonth(Integer.parseInt(month));
            } catch (NumberFormatException e) {
                item.setId(errorImportId);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, monthErrorStr, importErrorLogs);
                continue;
            }
            if (factoryMap.containsKey(factoryName)) {
                String factoryCode = factoryMap.get(factoryName);
                item.setFactoryCode(factoryCode);
            } else {
                item.setId(errorImportId);
                failureNum++;
                addImportErrorLog(importLogId, errorNum, String.format(noFactoryStr, factoryName), importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
            if (CollUtil.isNotEmpty(validated)) {
                item.setId(errorImportId);
//                failureNum++;
//                importErrorLogs.addAll(validated);
                String errorMsg = validated.get(0).getErrorDetail();
                this.appendErrorMessage(sbError, errorMsg);
                continue;
            }
            // 赋值开始结束日期
            if (item.getBeginDay() == null || item.getEndDay() == null) { // 没有排产的结构过滤掉，不提示直接过滤
                continue;
            } else {
                item.setAllotDays(item.getEndDay() - item.getBeginDay() + 1);
            }

            // 赋值交替类型（仅对校验通过的有效记录）
            this.genAlternatingType(item, machineLastValidRecordMap);
            insertList.add(item);

            structureMachineMap.put(item.getStructureName(), item.getCxMachineCode());
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())) {
            throw new BusinessException(sbError.toString());
        }

        // 过滤id不等于空的数据
        insertList = insertList.stream().filter(v -> v.getId() == null).collect(Collectors.toList());

        try {
            successNum = insertList.size();
            if (CollUtil.isNotEmpty(insertList)) {
                // 填充关联栏位
                this.fillStructureAllocation(insertList);

                if (updateSupport) { // 如果是覆盖，需要根据成型机 + 结构进行覆盖
                    LambdaQueryWrapper<MpStructureAllocation> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(MpStructureAllocation::getFactoryCode, importFactoryCode);
                    queryWrapper.eq(MpStructureAllocation::getProductionVersion, productVersion);
                    Map<String, MpStructureAllocation> oldAllocationMap = entityMapper.selectList(queryWrapper).stream()
                            .collect(Collectors.toMap(this::getStructureAllocationKey, Function.identity(),
                                    (p1, p2) -> p1));

                    // 取定稿版本对应的原始月计划需求核算版本
                    LambdaQueryWrapper<MpFactoryProductionVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
                    versionQueryWrapper.eq(MpFactoryProductionVersion::getFactoryCode, importFactoryCode);
                    versionQueryWrapper.eq(MpFactoryProductionVersion::getProductionVersion, productVersion);
                    versionQueryWrapper.eq(MpFactoryProductionVersion::getYear, year);
                    versionQueryWrapper.eq(MpFactoryProductionVersion::getMonth, month);
                    MpFactoryProductionVersion version = mpFactoryProductionVersionMapper
                            .selectOne(versionQueryWrapper);
                    String oriMonthPlanVersion = version != null ? version.getMonthPlanVersion() : monthPlanVersion;

                    for (MpStructureAllocation item : insertList) {
                        item.setMonthPlanVersion(oriMonthPlanVersion);
                        MpStructureAllocation oldAllocation = oldAllocationMap.get(this.getStructureAllocationKey(item));
                        if (oldAllocation != null) {
                            item.setId(oldAllocation.getId());
                            item.setCreateBy(oldAllocation.getCreateBy());
                            item.setCreateTime(oldAllocation.getCreateTime());
                            item.setBaseVale(item.getId());
                        }
                    }
                }
                // 插入新记录
                baseDao.saveBatch(insertList);
            }
        } catch (Exception e) {
            log.error("导入失败", e);
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        //返回提示信息及错误集合
//        cacheImportMachineMap(importLogId, machineMap);
        if (successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else if (failureNum > 0) {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 获取结构转产表的业务主键：机台 + 结构
     *
     * @param allocation
     * @return
     */
    private String getStructureAllocationKey(MpStructureAllocation allocation) {
        return GenerageMapKeyUtils.createMapKey(allocation.getCxMachineCode(), allocation.getStructureName());
    }

    /**
     * 填充导入结构转产表数据的关联栏位
     *
     * @param insertList
     */
    private void fillStructureAllocation(List<MpStructureAllocation> insertList) {
        // 填充关联字段
        MpStructureAllocation mpStructureAllocation = CollectionUtils.firstElement(insertList);
        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 创建查询数据的异步任务
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
        // 查询成型机台类型
        CompletableFuture<Map<String, String>> moldingMachineTypeCodeFuture = CompletableFuture.supplyAsync(
                () -> queryMoldingMachineTypeCode(mpStructureAllocation)
        );
        
        
        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    structureLhRatioFuture,
                    monCycleSchStruConfFuture,
                    cycleSchStruConfFuture,
                    factoryParamFuture,
                    materialConsumeDetailFuture,
                    rawSpecialMaterialRecordFuture,
                    skuStructureRefFuture,
                    skuConstructionRefFuture,
                    moldingMachineTypeCodeFuture
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

        List<MdmMonCycleSchStruConf> monCycleSchStruConfList = monCycleSchStruConfFuture.join();
        List<MdmStructureLhRatio> structureLhRatioList = structureLhRatioFuture.join();
        List<MdmCycleSchStruConf> cycleSchStruConfList = cycleSchStruConfFuture.join();
        List<FactoryParam> factoryParamList = factoryParamFuture.join();
        List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList = materialConsumeDetailFuture.join();
        List<RawSpecialMaterialRecord> specialMaterialList = rawSpecialMaterialRecordFuture.join();
        List<MdmSkuStructureRef> skuStructureRefList = skuStructureRefFuture.join();
        List<MdmSkuConstructionRef> skuConstructionRefList = skuConstructionRefFuture.join();
        Map<String, String> cxMachineTypeCodeMap = moldingMachineTypeCodeFuture.join();
        Map<String, MdmStructureLhRatio> structureLhRatioMap = this.buildStructureLhRatioMap(structureLhRatioList);

        for (MpStructureAllocation structure : insertList) {
            String structureName = structure.getStructureName();
            // 设置是否含有特殊材料
            String materialCode = skuStructureRefList.stream()
                    .filter(vo -> StringUtils.equals(vo.getStructureName(), structure.getStructureName()))
                    .findFirst()
                    .map(MdmSkuStructureRef::getMaterialCode)
                    .orElse(null);
            String embryoCode = skuConstructionRefList.stream()
                    .filter(vo -> StringUtils.equals(vo.getMaterialCode(), materialCode))
                    .findFirst()
                    .map(MdmSkuConstructionRef::getEmbryoCode)
                    .orElse(null);
            boolean isHasSpecialMaterial = hasSpecialMaterial(embryoCode, mdmMaterialConsumeDetailList, specialMaterialList);
            structure.setIsHasSpecialMaterial(isHasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);


            // 设置最大胎胚种类数、最大硫化机台数
            MdmStructureLhRatio mdmStructureLhRatio = this.getStructureLhRatio(structure, cxMachineTypeCodeMap,
                    structureLhRatioMap);
            if (mdmStructureLhRatio != null) {
                structure.setMaxEmbryoCodeCount(mdmStructureLhRatio.getMaxEmbryoQty());
                structure.setMaxLhMachineCount(mdmStructureLhRatio.getLhMachineMaxQty());
            } else {
                structure.setMaxEmbryoCodeCount(0);
                structure.setMaxLhMachineCount(0);
            }

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
        }
    }

    /**
     * 构建结构成型硫化配比Map,key=结构+成型机类型
     * @param structureLhRatioList
     * @return
     */
    private Map<String, MdmStructureLhRatio> buildStructureLhRatioMap(List<MdmStructureLhRatio> structureLhRatioList) {
        Map<String, MdmStructureLhRatio> structureLhRatioMap;
        if (PubUtil.isNotEmpty(structureLhRatioList)) {
            structureLhRatioMap = structureLhRatioList.stream().collect(Collectors.toMap(
                    ratio -> GenerageMapKeyUtils.createMapKey(ratio.getStructureName(), ratio.getCxMachineTypeCode()),
                    Function.identity(), (v1, v2) -> v1));
        } else {
            structureLhRatioMap = new HashMap<>();
        }
        return structureLhRatioMap;
    }

    /**
     * 获取结构对应的成型硫化配比
     * @param structure
     * @param cxMachineTypeCodeMap
     * @param structureLhRatioMap
     * @return
     */
    private MdmStructureLhRatio getStructureLhRatio(MpStructureAllocation structure,
            Map<String, String> cxMachineTypeCodeMap, Map<String, MdmStructureLhRatio> structureLhRatioMap) {
        String cxMachineTypeCode = cxMachineTypeCodeMap.get(structure.getCxMachineCode());
        if (StringUtils.isEmpty(cxMachineTypeCode)) {
            return null;
        }
        String key = GenerageMapKeyUtils.createMapKey(structure.getStructureName(), cxMachineTypeCode);
        return structureLhRatioMap.get(key);
    }
    
    /**
     * 处理结构交替类型
     *
     * @param item
     * @param machineLastValidRecordMap // 每个机台的当前结构
     */
    private void genAlternatingType(MpStructureAllocation item,
                                    Map<String, MpStructureAllocation> machineLastValidRecordMap) {
        if (item == null || StringUtils.isBlank(item.getCxMachineCode())) {
            return;
        }
        String machineCode = item.getCxMachineCode();
        MpStructureAllocation previousRecord = machineLastValidRecordMap.get(machineCode);
        if (previousRecord != null) {
            if (StringUtils.equals(previousRecord.getStructureName(), item.getStructureName())) {
                item.setAlternatingType(AlternativeTypeEnum.CONTINUE.getCode());
            } else if (StringUtils.equals(previousRecord.tbrProSize(), item.tbrProSize())) {
                item.setAlternatingType(AlternativeTypeEnum.STRUCT_ALTERNATIVE.getCode());
            } else {
                item.setAlternatingType(AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode());
            }
        } else {
            item.setAlternatingType(AlternativeTypeEnum.CONTINUE.getCode());
        }
        machineLastValidRecordMap.put(machineCode, item);// 本结构替换到机台上
    }

    /**
     * 获取上个月最后一天各机台排产的结构
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    private Map<String, MpStructureAllocation> getLastMachineStructureMap(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || year == null || month == null) {
            return new HashMap<>();
        }
        // 1、获取上个月日历
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1); // 通过日历获取本月一号的日历
        calendar.add(Calendar.MONTH, -1); // 切换到上个月
        Integer lastYear = calendar.get(Calendar.YEAR);
        Integer lastMonth = calendar.get(Calendar.MONTH) + 1;
        // 1.1、加载上个月的工作日历
        Set<Integer> workCalendar = this.getOpenWorkCalendar(factoryCode, lastYear, lastMonth);
        // 1.2、取出最后一个工作日
        Integer lastDay = workCalendar.stream().max(Integer::compareTo).orElse(null);
        if (lastDay == null) {
            return new HashMap<>();
        }

        // 2、加载上个月定稿版本的相关数据
        // 2.1、获取上个月定稿版本
        LambdaQueryWrapper<MpFactoryProductionVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
        versionQueryWrapper.eq(MpFactoryProductionVersion::getFactoryCode, factoryCode);
        versionQueryWrapper.eq(MpFactoryProductionVersion::getYear, lastYear);
        versionQueryWrapper.eq(MpFactoryProductionVersion::getMonth, lastMonth);
        versionQueryWrapper.eq(MpFactoryProductionVersion::getIsFinal, YesOrNoEnum.YES.getCode());
        MpFactoryProductionVersion procVersion = CollectionUtils.firstElement(mpFactoryProductionVersionMapper.selectList(versionQueryWrapper));
        if (procVersion == null || StringUtils.isEmpty(procVersion.getProductionVersion())) {
            return new HashMap<>();
        }
        // 2.2、获取上个月定稿版本对应的结构转产表
        String productionVersion = procVersion.getProductionVersion();
        LambdaQueryWrapper<MpStructureAllocation> structureQueryWrapper = new LambdaQueryWrapper<>();
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, factoryCode);
        structureQueryWrapper.eq(MpStructureAllocation::getProductionVersion, productionVersion);
        List<MpStructureAllocation> lastMonthFinalList = entityMapper.selectList(structureQueryWrapper);
        if (CollUtil.isEmpty(lastMonthFinalList)) {
            return new HashMap<>();
        }
        // 2.3、加载上个月定稿版本的统计记录
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, factoryCode);
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, productionVersion);
        Map<String, MpMonthPlanStatistics> statisticsMap = mpMonthPlanStatisticsEntityMapper.selectList(queryWrapper)
                .stream().collect(
                        Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));

        // 3、根据上个月定岗数据构建最后一天的结构排产情况
        Map<String, MpStructureAllocation> machineStructureMap = new HashMap<>();
        for (MpStructureAllocation record : lastMonthFinalList) {
            String machineCode = record.getCxMachineCode();
            String structureName = record.getStructureName();
            if (StringUtils.isBlank(structureName) || StringUtils.isBlank(machineCode)) {
                continue;
            }
            // 3.1、获取结构上个月最后一天的排产统计信息
            MpMonthPlanStatistics statistics = statisticsMap.get(structureName);
            if (statistics == null) {
                continue;
            }
            String dayStatisticsStr = (String) statistics.getFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, lastDay));
            if (StringUtils.isEmpty(dayStatisticsStr) || !JSONValidator.from(dayStatisticsStr).validate()) {
                continue;
            }
            // 3.2、检查最后一天的排产，如果硫化机数大于0说明有排产，则添加到列表中
            MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
            if (dayStatistics.getLhMachines() == null || dayStatistics.getLhMachines() <= 0) {
                continue;
            }
            machineStructureMap.put(machineCode, record);
        }
        return machineStructureMap;
    }

    /**
     * 获取指定月份的开班工作日历
     * 
     * @param factoryCode
     * @param lastYear
     * @param lastMonth
     * @return
     */
    private Set<Integer> getOpenWorkCalendar(String factoryCode, Integer lastYear, Integer lastMonth) {
        MpStructureAllocation queryParams = new MpStructureAllocation();
        queryParams.setFactoryCode(factoryCode);
        queryParams.setYear(lastYear);
        queryParams.setMonth(lastMonth);
        return this.queryMdmWorkCalendar(queryParams).stream()
                .filter(item -> Objects.equals(item.getDayFlag(), YesOrNoEnum.YES.getCode()))
                .map(MdmWorkCalendar::getDay).distinct().collect(Collectors.toSet());
    }

    /**
     * 导入月计划
     *
     * @param list             结构转产表列表数据
     * @param list4DayResult   月计划列表数据
     * @param updateSupport    覆盖
     * @param importLogId      导入日志ID
     * @param params           表头参数
     * @param monthPlanVersion 月计划版本
     * @param productVersion   生产版本
     * @param isAdjust         是否调整
     * @return 结果
     */
    @Override
    public AjaxResult importDataDayResult(List<MpStructureAllocationExportVo> list, List<FactoryMonthPlanMouldDayResult> list4DayResult, boolean updateSupport, Long importLogId, String[] params, String monthPlanVersion, String productVersion,
                                          Map<String, String> factoryMap, Map<String, String> productTypeMap, Map<String, String> structureMachineMap, boolean isAdjust) {
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

            String factoryCode = factoryMap.get(factoryName);
            Integer importYear = Convert.toInt(year, null);
            Integer importMonth = Convert.toInt(month, null);

            //2.国际化初始化
            String noFactoryStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.noFactoryStr");
            String yearErrorStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.yearErrorStr");
            String monthErrorStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.monthErrorStr");
            String noStructureNameStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.noStructureNameStr");
            String outOfRangeStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.outOfRange");
            String noStructureDateStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.noStructureDateStr");

            // 查询结构转产表
            Map<String, List<MpStructureAllocation>> allStructureNameMap = this.getAllStructureNameMap(factoryCode, productVersion, list);
            // 按结构分组汇总结构转产表的最早上机日期和最晚下机日期
            Map<String, MpStructureAllocation> structureDayMap = this.getStructureDayMap(allStructureNameMap);
            // 过滤合计等数据
            list4DayResult = list4DayResult.stream().collect(Collectors.toList());

            //3.公共校验（非空校验、长度校验等）
            StringBuilder sbError = new StringBuilder(); // 记录校验异常的信息
            for (int i = 0; i < list4DayResult.size(); i++) {
                int errorNum = i + 5;
                FactoryMonthPlanMouldDayResult item = list4DayResult.get(i);
                if (StringUtils.isBlank(item.getMaterialCode())) { // 没有物料编码的是合计行，直接跳过，不需要记录错误
                    continue;
                }
                item.setIsImport(YesOrNoEnum.YES.getCode());
                item.setMonthPlanVersion(monthPlanVersion);
                item.setProductionVersion(productVersion);
                item.setPlanType("01");

                // 校验是否有排产
                item.statisticsTotalQty(); // 统计总排产量
                if (item.getTotalQty() <= 0) {
                    item.setId(errorImportId);
                    continue;
                }

                if (productTypeMap.containsKey(params[3])) {
                    item.setProductTypeCode(productTypeMap.get(params[3]));
                }
                if (StringUtils.isBlank(item.getCxMachineCode()) && StringUtils.isNotBlank(item.getStructureName())) {
                    String machineCode = structureMachineMap.get(item.getStructureName());
                    if (StringUtils.isNotBlank(machineCode)) {
                        item.setCxMachineCode(machineCode);
                    }
                }

                try {
                    item.setYear(Integer.parseInt(year));
                } catch (NumberFormatException e) {
                    item.setId(errorImportId);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, yearErrorStr, importErrorLogs);
                    continue;
                }
                try {
                    item.setMonth(Integer.parseInt(month));
                } catch (NumberFormatException e) {
                    item.setId(errorImportId);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, monthErrorStr, importErrorLogs);
                    continue;
                }
                if (StringUtils.isNoneEmpty(factoryCode)) {
                    item.setFactoryCode(factoryCode);
                } else {
                    item.setId(errorImportId);
                    failureNum++;
                    addImportErrorLog(importLogId, errorNum, String.format(noFactoryStr, factoryName), importErrorLogs);
                    continue;
                }

                List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, item);
                if (CollUtil.isNotEmpty(validated)) {
                    item.setId(errorImportId);
//                    failureNum++;
//                    importErrorLogs.addAll(validated);
                    String errorMsg = validated.get(0).getErrorDetail();
                    this.appendErrorMessage(sbError, errorMsg);
                    continue;
                }
                // 结构转产表校验
                if (!allStructureNameMap.containsKey(item.getStructureName())) {
                    item.setId(errorImportId);
//                    failureNum++;
//                    addImportErrorLog(importLogId, errorNum, String.format(noStructureNameStr, item.getStructureName()), importErrorLogs);
                    String errorMsg = String.format(noStructureNameStr, item.getStructureName());
                    this.appendErrorMessage(sbError, errorMsg);
                    continue;
                }
                // 给开始日期、结束日期赋值
                this.setBeginDayAndEndDay(item);
                Integer resultBeginDay = item.getBeginDay();
                Integer resultEndDay = item.getEndDay();
                if (resultBeginDay == null || resultEndDay == 0) { // 没有排产日的记录忽略，无需记录错误记录
                    continue;
                }
                // 校验月计划的排产日期是否在结构转产表的范围内
                MpStructureAllocation mpStructureAllocation = structureDayMap.get(item.getStructureName());
                if (mpStructureAllocation != null) {
                    Integer structureBeginDay = mpStructureAllocation.getBeginDay();
                    Integer structureEndDay = mpStructureAllocation.getEndDay();
                    if (resultBeginDay < structureBeginDay || resultEndDay > structureEndDay) { // 超范围则记录错误信息
                        item.setId(errorImportId);
                        String errorMsg = String.format(outOfRangeStr, item.getMaterialDesc(), structureBeginDay, structureEndDay);
//                        failureNum++;
//                        addImportErrorLog(importLogId, errorNum, errorMsg, importErrorLogs);
                        this.appendErrorMessage(sbError, errorMsg);
                        continue;
                    }
                } else { // 结构没有开始结束时间
//                    failureNum++;
                    String errorMsg = String.format(noStructureDateStr, item.getStructureName());
                    this.appendErrorMessage(sbError, errorMsg);
                    continue;
                }

                insertList.add(item);
            }
            if (!StringUtil.isEmptyWithTrim(sbError.toString())) { // 任意一个强控项没有验证通过,都直接终止
                throw new BusinessException(sbError.toString());
            }

            // 填充字段信息
            List<FactoryMonthPlanMouldDayResult> finalImportList = this.fillMonthPlanMouldResult(insertList, isAdjust,
                    importLogId, importErrorLogs);
            failureNum += (insertList.size() - finalImportList.size());

            // 过滤id不等于空的数据
            finalImportList = finalImportList.stream().filter(v -> v.getId() == null).collect(Collectors.toList());

            try {
                successNum = finalImportList.size();
                if (CollUtil.isNotEmpty(finalImportList)) {
                    // 如果是调整，需要先删除原记录再插入
                    if (isAdjust) {
                        // 删除本次导入结构的调整记录
                        Set<String> structureNameSet = list4DayResult.stream()
                                .map(FactoryMonthPlanMouldDayResult::getStructureName).distinct()
                                .collect(Collectors.toSet());
                        LambdaQueryWrapper<MpAdjustResult> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(MpAdjustResult::getFactoryCode, factoryCode);
                        queryWrapper.eq(MpAdjustResult::getYear, importYear);
                        queryWrapper.eq(MpAdjustResult::getMonth, importMonth);
                        queryWrapper.eq(MpAdjustResult::getVersion, monthPlanVersion);
                        queryWrapper.in(MpAdjustResult::getStructureName, structureNameSet);
                        mpAdjustResultEntityMapper.delete(queryWrapper);
                        // 取定稿版本对应的原始月计划需求核算版本
                        LambdaQueryWrapper<MpFactoryProductionVersion> versionQueryWrapper = new LambdaQueryWrapper<>();
                        versionQueryWrapper.eq(MpFactoryProductionVersion::getFactoryCode, factoryCode);
                        versionQueryWrapper.eq(MpFactoryProductionVersion::getProductionVersion, productVersion);
                        versionQueryWrapper.eq(MpFactoryProductionVersion::getYear, year);
                        versionQueryWrapper.eq(MpFactoryProductionVersion::getMonth, month);
                        MpFactoryProductionVersion version = mpFactoryProductionVersionMapper
                                .selectOne(versionQueryWrapper);
                        String oriMonthPlanVersion = version != null ? version.getMonthPlanVersion() : monthPlanVersion;

                        // 构建调整记录
                        List<MpAdjustResult> saveList = finalImportList.stream().map(r -> {
                            MpAdjustResult adjustResult = new MpAdjustResult();
                            BeanUtil.copyProperties(r, adjustResult);
                            adjustResult.setVersion(monthPlanVersion);
                            adjustResult.setMonthPlanVersion(oriMonthPlanVersion);
                            adjustResult.setLastMonthPlanVersion(monthPlanVersion);
                            adjustResult.setAdjustType(WeekAdjustTypeEnum.STRUCTURE_IN.getCode());
                            adjustResult.setTotalPlanQty(adjustResult.getTotalQty());
                            // 从结构转产表获取成型机并添加到调整记录表中
                            List<MpStructureAllocation> allStructureNameList = allStructureNameMap.get(adjustResult.getStructureName());
                            if (allStructureNameList != null) {
                                adjustResult.setCxMachineCode(allStructureNameList.stream()
                                        .map(MpStructureAllocation::getCxMachineCode).filter(StringUtils::isNotEmpty)
                                        .sorted().collect(Collectors.joining(",")));
                            }
                            return adjustResult;
                        }).collect(Collectors.toList());
                        // 处理特殊材料标记
                        this.setSpecialMaterial(factoryCode, saveList);

                        // 类型转成
                        baseDao.insertBatch(saveList);
                    } else {
                        // 插入新记录
                        baseDao.insertBatch(finalImportList);
                    }
                }
            } catch (Exception e) {
                log.error("导入失败", e);
                successNum = 0;
                failureNum = list4DayResult.size();
                importErrorLogs.clear();
                addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            }
            //返回提示信息及错误集合
            if (successNum == 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
            } else if (failureNum > 0) {
                return AjaxResult.success(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
            } else {
                return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
            }
        } finally {
            clearImportMachineMap(importLogId);
        }
    }

    /**
     * 添加错误信息
     * @param sbError
     * @param errorMsg
     */
    private void appendErrorMessage(StringBuilder sbError, String errorMsg) {
        if (sbError.indexOf(errorMsg) < 0) { // 错误信息去重
            sbError.append(errorMsg).append(BusiConstant.WeekRollAdjust.SPLIT_FRONT_NEW_LINE);// 强控，任意一个有错都不允许导入
        }
    }

    /**
     * 查询结构转产表，合并到导入excel导入的转产列表中，最终还需要排除掉导入失败的记录
     *
     * @param factoryCode    工厂
     * @param productVersion 生产版本
     * @param imoprtList     导入的结构转产表记录
     * @return
     */
    private Map<String, List<MpStructureAllocation>> getAllStructureNameMap(String factoryCode, String productVersion,
                                                                            List<MpStructureAllocationExportVo> imoprtList) {
        Map<String, List<MpStructureAllocation>> allStructureNameMap = imoprtList.stream().collect(Collectors.groupingBy(MpStructureAllocation::getStructureName)); // 按结构名称分组
        QueryWrapper<MpStructureAllocation> structureAllocationQueryWrapper = new QueryWrapper<>();
        structureAllocationQueryWrapper.eq("FACTORY_CODE", factoryCode);
        structureAllocationQueryWrapper.eq("PRODUCTION_VERSION", productVersion);
        List<MpStructureAllocation> structureList = entityMapper.selectList(structureAllocationQueryWrapper);
        if (structureList != null) { // 根据结构 + 成型机添加excel中没有的结构转产数据
            Set<String> importStructureSet = imoprtList.stream().map(this::getStructureAllocationKey).collect(Collectors.toSet());
            for (MpStructureAllocation structure : structureList) {
                if (!importStructureSet.contains(this.getStructureAllocationKey(structure))) {
                    allStructureNameMap.computeIfAbsent(structure.getStructureName(), key -> new ArrayList<>()).add(structure);
                }
            }
        }
        // 将导入失败的结构剔除掉
        Set<String> removeKeySet = new HashSet<>();
        for (Entry<String, List<MpStructureAllocation>> entry : allStructureNameMap.entrySet()) {
            String key = entry.getKey();
            List<MpStructureAllocation> allStructureNameList = entry.getValue();
            for (int i = allStructureNameList.size() - 1; i >= 0; i--) {
                if (Objects.equals(allStructureNameList.get(i).getId(), errorImportId)) {
                    allStructureNameList.remove(i);
                }
            }
            if (CollectionUtils.isEmpty(allStructureNameList)) {
                removeKeySet.add(key);
            }
        }
        for (String key : removeKeySet) {
            allStructureNameMap.remove(key);
        }
        return allStructureNameMap;
    }

    /**
     * 按结构分组汇总结构转产表的最早上机日期和最晚下机日期
     *
     * @param allStructureNameMap
     * @return
     */
    private Map<String, MpStructureAllocation> getStructureDayMap(
            Map<String, List<MpStructureAllocation>> allStructureNameMap) {
        Map<String, MpStructureAllocation> structureDayMap = new HashMap<>();
        for (Entry<String, List<MpStructureAllocation>> entry : allStructureNameMap.entrySet()) {
            String structureName = entry.getKey();
            List<MpStructureAllocation> structureNameList = entry.getValue();
            // 取结构结构的上机时间和下机时间
            Integer itemBegingDay = structureNameList.stream().map(MpStructureAllocation::getBeginDay)
                    .filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
            Integer itemEndDay = structureNameList.stream().map(MpStructureAllocation::getEndDay)
                    .filter(Objects::nonNull).max(Integer::compareTo).orElse(null);
            if (itemBegingDay == null || itemEndDay == null) {
                continue;
            }
            MpStructureAllocation newVo = new MpStructureAllocation();
            newVo.setBeginDay(itemBegingDay);
            newVo.setEndDay(itemEndDay);
            structureDayMap.put(structureName, newVo);
        }
        return structureDayMap;
    }

    /**
     * 填充导入月计划数据的关联栏位
     *
     * @param isAdjust        是否导入调整版本
     * @param insertList      导入数据
     * @param importLogId
     * @param importErrorLogs
     * @return
     */
    private List<FactoryMonthPlanMouldDayResult> fillMonthPlanMouldResult(List<FactoryMonthPlanMouldDayResult> insertList,
                                                                          boolean isAdjust,
                                                                          Long importLogId,
                                                                          List<ImportErrorLog> importErrorLogs) {
        if (CollectionUtils.isEmpty(insertList)) {
            return new ArrayList<>(0);
        }
        // 计划类型、产品品类、MES物料编码、产品分类、排产分类、规格、花纹、品牌、SUM(高优先级数量)、月均销量、库销比、SUM(生产需求计划)、SUM(实际生产需求（含损耗）)、结构类型 --- 数据源：需求计划
        FactoryMonthPlanMouldDayResult firstResult = CollectionUtils.firstElement(insertList);
        String monthPlanVersion = firstResult.getMonthPlanVersion();
        String factoryCode = firstResult.getFactoryCode();
        String productTypeCode = firstResult.getProductTypeCode();
        Integer year = firstResult.getYear();
        Integer month = firstResult.getMonth();
        Integer yearMonth = Convert.toInt(String.format("%s%02d", year, month));
        MpWeekRollAdjustEngine weekRollAdjustEngine = new MpWeekRollAdjustEngine();
        MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj = new MpAdjustDailyCapacityLimit();

        // 错误提醒
        String notDayVulcanizationQtyStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.dayVulcanizationQty"); // 没有维护日硫化量
        String notMaterialStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.notMaterial"); // 物料不存在
        String notSkuMoldRelStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.notSkuMoldRel"); // SKU与模具关系不存在
        String notConstructStr = I18nUtil.getMessage("ui.data.alert.MpStructureAllocation.notConstructStr"); // 示方书不存在

        // 1、加载必要的数据=======【start】=======
        // 1.1、加载需求计划
        QueryWrapper<DpDemandPlan> dpDemandPlanQueryWrapper = new QueryWrapper<>();
        dpDemandPlanQueryWrapper.select("STRUCTURE_NAME", "MATERIAL_CODE", "MES_MATERIAL_CODE", "PLAN_TYPE", "PRODUCT_TYPE_CODE",
                "PRODUCTION_TYPE", "SPECIFICATIONS", "PATTERN", "BRAND", "SUM(HEIGHT_QTY) HEIGHT_QTY",
                "AVERAGE_SALE_QTY", "STOCK_QTY", "SUM(NET_QTY) NET_QTY", "SUM(POSTPONE_NET_QTY) POSTPONE_NET_QTY",
                "STRUCTURE_TYPE", "SUM(MID_QTY) MID_QTY", "SUM(CYCLE_RESERVE_QTY) CYCLE_RESERVE_QTY",
                "SUM(CONVENTION_RESERVE_QTY) CONVENTION_RESERVE_QTY", "SUM(POSTPONE_QTY) POSTPONE_QTY", "MAIN_PATTERN");
        dpDemandPlanQueryWrapper.groupBy("STRUCTURE_NAME", "MATERIAL_CODE", "MES_MATERIAL_CODE", "PLAN_TYPE", "PRODUCT_TYPE_CODE",
                "PRODUCTION_TYPE", "SPECIFICATIONS", "PATTERN", "BRAND", "AVERAGE_SALE_QTY", "STOCK_QTY",
                "STRUCTURE_TYPE", "MAIN_PATTERN");
        dpDemandPlanQueryWrapper.eq("FACTORY_CODE", factoryCode);
        dpDemandPlanQueryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        Map<String, DpDemandPlan> dpDemandPlanMap = dpDemandPlanEntityMapper.selectList(dpDemandPlanQueryWrapper)
                .stream().collect(Collectors.toMap(DpDemandPlan::getMaterialCode, Function.identity()));

        // 1.2、加载sku与施工关系
        LambdaQueryWrapper<MdmSkuConstructionRef> skuConstructionRefQueryWrapper = new LambdaQueryWrapper<>();
        skuConstructionRefQueryWrapper.eq(MdmSkuConstructionRef::getFactoryCode, factoryCode);
        Map<String, Map<String, List<MdmSkuConstructionRef>>> constructionInfoMap = mdmSkuConstructionRefEntityMapper
                .selectList(skuConstructionRefQueryWrapper).stream()
                .collect(Collectors.groupingBy(MdmSkuConstructionRef::getMaterialCode,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().collect(
                                Collectors.groupingBy(i -> this.transferTrialStatusToStage(i.getTrialStatus()))))));
        // 1.3、日硫化量获取
        DayVulcanizationModeEnum mode = null;
        String dayVulcanizationCode = (String)getFactorParam(factoryCode, ProductTypeEnum.getEnumByValue(productTypeCode), MonthPlanEnums.DAY_VULCANIZATION_MODE);
        if (dayVulcanizationCode != null) {
            mode = DayVulcanizationModeEnum.getInstance(dayVulcanizationCode);
        } else {
            mode = DayVulcanizationModeEnum.STANDARD_CAPACITY;
        }
        LambdaQueryWrapper<MdmSkuLhCapacity> skuLhCapacityQueryWrapper = new LambdaQueryWrapper<>();
        skuLhCapacityQueryWrapper.eq(MdmSkuLhCapacity::getFactoryCode, factoryCode);
        Map<String, MdmSkuLhCapacity> productLhCapacityMap = mdmSkuLhCapacityEntityMapper
                .selectList(skuLhCapacityQueryWrapper).stream().collect(Collectors
                        .toMap(MdmSkuLhCapacity::getMaterialCode, Function.identity(), (m1, m2) -> m1));
        // 1.4、加载型腔活块数
        Map<String, Integer> cavityResults = new HashMap<>(0); // 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> insertResults = new HashMap<>(0); // 活块可用量（按物料描述分组）
        List<DailyMouldAvailabilityResult> moldResult = moldCavityInsertMaxValueCalculator
                .moldCavityInsertMaxValueCalculator(year, month, factoryCode,
                        null, null, true,true);
        if (!CollectionUtils.isEmpty(moldResult)) {
            cavityResults = moldResult.get(0).getCavityResults();
            insertResults = moldResult.get(0).getInsertResults();
        }
        // 1.5、加载试产试制规格
        LambdaQueryWrapper<MpTrialPlan> mpTrialPlanQueryWrapper = new LambdaQueryWrapper<>();
        mpTrialPlanQueryWrapper.eq(MpTrialPlan::getFactoryCode, factoryCode);
        mpTrialPlanQueryWrapper.eq(MpTrialPlan::getYear, year);
        mpTrialPlanQueryWrapper.eq(MpTrialPlan::getMonth, month);
        mpTrialPlanQueryWrapper.isNull(MpTrialPlan::getProductionDate);
        Map<String, MpTrialPlan> trialPlanMap = mpTrialPlanEntityMapper.selectList(mpTrialPlanQueryWrapper).stream()
                .collect(Collectors.toMap(MpTrialPlan::getMaterialCode, Function.identity(), (p1, p2) -> p2));
        // 1.6、加载周期结构
        LambdaQueryWrapper<MdmCycleSchStruConf> mdmCycleSchStruConfQueryWrapper = new LambdaQueryWrapper<>();
        mdmCycleSchStruConfQueryWrapper.eq(MdmCycleSchStruConf::getFactoryCode, factoryCode);
        Set<String> cycleSchStruSet = mdmCycleSchStruConfEntityMapper.selectList(mdmCycleSchStruConfQueryWrapper).stream().map(MdmCycleSchStruConf::getStructureName).distinct().collect(Collectors.toSet());
        // 1.7、加载排产参数
        LambdaQueryWrapper<FactoryParam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FactoryParam::getFactoryCode, factoryCode);
        queryWrapper.eq(FactoryParam::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.in(FactoryParam::getParamCode, MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode(), MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode());
        Map<String, Object> paramMap = factoryParamMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(FactoryParam::getParamCode, FactoryParamUtils::getParamValue));
        // 1.8、加载物料表
        LambdaQueryWrapper<MdmMaterialInfo> mdmMaterialInfoQueryWrapper = new LambdaQueryWrapper<>();
        mdmMaterialInfoQueryWrapper.eq(MdmMaterialInfo::getFactoryCode, factoryCode);
        Map<String, MdmMaterialInfo> materialInfoMap = mdmMaterialInfoEntityMapper
                .selectList(mdmMaterialInfoQueryWrapper).stream()
                .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (m1, m2) -> m1));
        // 1.9、加载硫化日产
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap = this.loadDailyCapacityMap(factoryCode, year, month);
        // 1.10、加载SKU与模具关系
        List<MoldCavityInsertMaxValueCalculatorVo> moldList = factoryMonthPlanProductMouldMapper
                .getEnableProductionMouldInfoByNetDemand(factoryCode, year, yearMonth, monthPlanVersion, true);
        Map<String, String> mdmMouldInfoMap = convertToMouldInfoMap(moldList);
        // 1.10.1、加载模具到货计划
        LocalDate monthStart = LocalDate.of(year, month, ProductionConstant.MONTH_START_DAY);
        Date productionStartDate = DateUtils.getDate(LocalDate.of(year, month, ProductionConstant.MONTH_START_DAY));
        Date productionEndDate = DateUtils.getDate(monthStart.with(TemporalAdjusters.lastDayOfMonth()));
        List<MoldCavityInsertMaxValueCalculatorVo> mouldDeliveryList = factoryMonthPlanProductMouldMapper
                .getEnableMouldDeliveryInfoByNetDemand(factoryCode, year, month, monthPlanVersion, productionStartDate,
                        productionEndDate);
        // 1.10.2、合并SKU与模具关系以及模具到货计划
        Set<String> materialHasMoldSet = moldList.stream().map(MoldCavityInsertMaxValueCalculatorVo::getMaterialDesc).distinct().collect(Collectors.toSet()); // 有模具的sku列表
        materialHasMoldSet.addAll(mouldDeliveryList.stream().map(MoldCavityInsertMaxValueCalculatorVo::getMaterialDesc).distinct().collect(Collectors.toSet())); // 模具到货计划合并到sku列表中
        // 1.11、初始化SKU排产分类，Map<物料编码, 分类>
        Map<String, String> productionTypeMap = mdmSkuScheduleCategoryService.skuToProductionType(factoryCode);
        if (PubUtil.isEmpty(productionTypeMap)) {
            productionTypeMap = new HashMap<>();
        }
        // 1、加载必要的数据=======【end】=======

        // 2、遍历导入数据，填充各必要栏位数值
        List<FactoryMonthPlanMouldDayResult> finalImportList = new ArrayList<>();
        List<FactoryMonthPlanFinalAdjustVo> finalAdjustList = new ArrayList<>();
        StringBuilder sbError = new StringBuilder(); // 记录校验异常的信息
        for (FactoryMonthPlanMouldDayResult insertItem : insertList) {
            String structureName = insertItem.getStructureName();
            String materialDesc = insertItem.getMaterialDesc();
            String materialCode = insertItem.getMaterialCode();
            Integer rowNum = insertItem.getImportRowNum();
            DpDemandPlan demandPlan = dpDemandPlanMap.get(materialCode);
            // 2.1、物料校验
            MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
            if (materialInfo == null) {
                insertItem.setId(errorImportId);
                String errorMsg = String.format(notMaterialStr, materialDesc + materialCode);
                addImportErrorLog(importLogId, rowNum, errorMsg, importErrorLogs);
                continue;
            }
            // 2.2、模具校验
            if (!materialHasMoldSet.contains(materialDesc)) {
                insertItem.setId(errorImportId);
                String errorMsg = String.format(notSkuMoldRelStr, materialDesc + materialCode);
                addImportErrorLog(importLogId, rowNum, errorMsg, importErrorLogs);
                continue;
            }
            // 2.3、从需求计划或者物料表中关联必要信息
            if (demandPlan != null) {
                structureName = demandPlan.getStructureName();
                insertItem.setStructureName(structureName);
                insertItem.setMesMaterialCode(demandPlan.getMesMaterialCode());
                insertItem.setPlanType(demandPlan.getPlanType());
                insertItem.setProductTypeCode(demandPlan.getProductTypeCode());
                insertItem.setSpecifications(demandPlan.getSpecifications());
                insertItem.setPattern(demandPlan.getPattern());
                insertItem.setBrand(demandPlan.getBrand());
                insertItem.setHeightQty(demandPlan.getHeightQty());
                insertItem.setHeightLossQty(demandPlan.getHeightQty());
                insertItem.setMidLossQty(demandPlan.getMidQty());
                insertItem.setCycleReserveLossQty(demandPlan.getCycleReserveQty());
                insertItem.setConventionReserveQty(demandPlan.getConventionReserveQty());
                insertItem.setPostponeQty(demandPlan.getPostponeQty());
                insertItem.setAverageSaleQty(demandPlan.getAverageSaleQty());
                insertItem.setProdReqPlan(demandPlan.getNetQty());
                insertItem.setStructureType(demandPlan.getStructureType());
                insertItem.setMainPattern(demandPlan.getMainPattern());
                // 总需求+(奇数+3/偶数+2)
                Integer factProdReqQty = safeAdd(demandPlan.getHeightQty(), demandPlan.getMidQty(), demandPlan.getPostponeQty()); // 总需求
                if (factProdReqQty > 0) {
                    Integer lossQty = (factProdReqQty & 1) == 0?ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER : ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
                    factProdReqQty = factProdReqQty + lossQty;
                }
                insertItem.setFactProdReqQty(factProdReqQty);
                // 计算库销比
                insertItem.setInventorySalesRatio(BigDecimalUtils.div(demandPlan.getStockQty(), demandPlan.getAverageSaleQty(), 1));
            } else { // 试产试制规格从物料信息表加载信息
                structureName = materialInfo.getStructureName();
                insertItem.setStructureName(structureName);
                insertItem.setMesMaterialCode(materialInfo.getMesMaterialCode());
//                insertItem.setPlanType(materialInfo.getPlanType());
                insertItem.setProductTypeCode(materialInfo.getProductTypeCode());
                insertItem.setSpecifications(materialInfo.getSpecifications());
                insertItem.setPattern(materialInfo.getPattern());
                insertItem.setBrand(materialInfo.getBrand());
                insertItem.setHeightLossQty(0);
                insertItem.setMidLossQty(0);
                insertItem.setCycleReserveLossQty(0);
                insertItem.setConventionReserveQty(0);
                insertItem.setPostponeQty(0);
                insertItem.setAverageSaleQty(0);
                insertItem.setProdReqPlan(0);
                insertItem.setFactProdReqQty(0);
                insertItem.setMainPattern(materialInfo.getMainPattern());
                // 结构类型
                String structureType;
                if (!CollectionUtils.isEmpty(cycleSchStruSet) && cycleSchStruSet.contains(structureName)) {
                    structureType = ProductionGroupTypeEnum.CYCLE.getGroupType();
                } else {
                    structureType = ProductionGroupTypeEnum.CONVENTION.getGroupType();
                }
                insertItem.setStructureType(structureType);
                // 获取产品阶段
                MpTrialPlan trialPlan = trialPlanMap.get(materialCode);
                if (trialPlan != null) {
                    insertItem.setConstructionStage(transferTrialStatusToStage(trialPlan.getTrialStatus()));
                }
            }
            // 2.4、胎胚号、施工阶段、是否零度材料、制造示方书号、文字示方书号、硫化示方书号---数据源：SKU与示方书关系，关联：SKU+胎胚描述
            Map<String, List<MdmSkuConstructionRef>> constructionStatusGroup = constructionInfoMap.get(materialCode);
            if (constructionStatusGroup != null) {
                String constructionStage = insertItem.getConstructionStage();
                List<MdmSkuConstructionRef> constructionConfigurationList = constructionStatusGroup.get(constructionStage);
                if (CollectionUtils.isEmpty(constructionConfigurationList)) {
                    Entry<String, List<MdmSkuConstructionRef>> entry = constructionStatusGroup.entrySet().stream()
                            .max(Comparator.comparing(Entry::getKey)).get();
                    constructionConfigurationList = entry.getValue();
                    constructionStage = entry.getKey();
                }
                MdmSkuConstructionRef constructionInfo = constructionConfigurationList.get(0);
                insertItem.setEmbryoCode(constructionInfo.getEmbryoCode());
                insertItem.setConstructionStage(constructionStage);
                insertItem.setIsZeroRack(constructionInfo.getIsZeroRack());
                insertItem.setEmbryoNo(constructionInfo.getEmbryoNo());
                insertItem.setTextNo(constructionInfo.getTextNo());
                insertItem.setLhNo(constructionInfo.getLhNo());
                insertItem.setProductStatus(constructionInfo.getTrialStatus());
                insertItem.setMainMaterialDesc(constructionInfo.getMainMaterialDesc());
            }
            if (StringUtils.isEmpty(insertItem.getLhNo())) {
                String errorMsg = String.format(notConstructStr, materialDesc + materialCode);
                this.appendErrorMessage(sbError, errorMsg);
                continue;
            }
            if (insertItem.getConstructionStage() == null) {
                insertItem.setConstructionStage(ConstructionStageEnum.NO_CONSTRUCTION.getStage());
            }

            // 2.5、日硫化量（单模），单条硫化时间---数据源：SKU双模日硫化量， 日标准产量/2，硫化总时间(s)
            MdmSkuLhCapacity mdmSkuLhCapacity = productLhCapacityMap.get(materialCode);
            if (mdmSkuLhCapacity != null) {
                MonthPlanProductLhCapacityVo capacityVo = new MonthPlanProductLhCapacityVo();
                capacityVo.setMesCapacity(mdmSkuLhCapacity.getMesCapacity());
                capacityVo.setStandardCapacity(mdmSkuLhCapacity.getStandardCapacity());
                capacityVo.setApsCapacity(mdmSkuLhCapacity.getApsCapacity());
                capacityVo.calculateDayVulcanizationQty(mode);
                if (capacityVo.getDayVulcanizationQty() != null) {
                    insertItem.setDayVulcanizationQty(capacityVo.getDayVulcanizationQty() / 2);
                }
            }
            if (insertItem.getDayVulcanizationQty() == null) {
                insertItem.setId(errorImportId);
                String errorMsg = String.format(notDayVulcanizationQtyStr, structureName, materialDesc);
                this.appendErrorMessage(sbError, errorMsg);
                continue;
            }

            // 2.6、英寸---根据结构名称解析
            if (!StringUtil.isEmptyWithTrim(structureName)) {
                // 正则：R后面跟数字（可能带小数点）
                Pattern pattern = Pattern.compile("R\\d+(?:\\.\\d+)?");
                Matcher matcher = pattern.matcher(structureName);
                String proSize = matcher.find() ? matcher.group() : "";
                insertItem.setProSize(proSize);
            }

            // 2.7、型腔数量---同结构主花纹最大的型腔数量
            insertItem.setMouldCavityQty(cavityResults.getOrDefault(insertItem.getStructureName() + insertItem.getMainPattern(), 0));
            insertItem.setTypeBlockQty(insertResults.getOrDefault(materialDesc, 0));

            // 2.8、各排产量倒推，高优先级排产数量 = min(高优先级，剩余排产量) ->中优先级排产数量 = min(中优先级，剩余排产量) ->周期排产储备排产 = min(周期储备量，剩余排产量) -> 常规储备排产 = 剩余排产量；
            insertItem.allocateProductionByPriority();

            // 填充模壳标准，用于 统计表可以存储
            String key = getSpecAndMainPatternKey(insertItem.getSpecifications(),insertItem.getMainPattern());
            if (StringUtils.isNotBlank(mdmMouldInfoMap.get(key))){
                insertItem.setMouldShell(mdmMouldInfoMap.get(key));
            }

            // 2.9、构建调整对象
            FactoryMonthPlanFinalAdjustVo mpFinalVo = this.castToAdjustVo(insertItem);
            // 2.9.2、生成模具变化信息
            weekRollAdjustEngine.setMouldChangeInfo(adjustDailyCapacityLimitObj, paramMap, mpFinalVo.getBeginDay(), mpFinalVo, dailyCapacityMap);
            insertItem.setMouldChangeInfo(mpFinalVo.getMouldChangeInfo());
            
            insertItem.setYearMonth(yearMonth);
            insertItem.setProductionType(productionTypeMap.get(materialCode));
            finalImportList.add(insertItem);
            finalAdjustList.add(mpFinalVo);
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())) { // 任意一个强控项没有验证通过,都直接终止
            throw new BusinessException(sbError.toString());
        }
        MpRollAdjustContextDTO contextDTO = this.initAdjustContext(firstResult);
        contextDTO.setFactoryMonthPlanProdFinalList(finalAdjustList);
        // 3、生成统计信息（handleMonthPlanStatistics）
        mpMonthPlanStaticService.handleMonthPlanStatistics(contextDTO, finalImportList, isAdjust);
        // 4、校验导入数据中的各项限制
        this.checkAdjustLimit(contextDTO, dailyCapacityMap, weekRollAdjustEngine, adjustDailyCapacityLimitObj,
                importLogId, importErrorLogs);
        // 5、生成特殊材料排产记录
        iSpecialMaterialResultService.buildSecialMaterialResult(finalImportList);
        return finalImportList;
    }

    /**
     * 校验导入数据中的各项限制
     * 
     * @param contextDTO
     * @param dailyCapacityMap
     * @param weekRollAdjustEngine
     * @param adjustDailyCapacityLimitObj
     * @param importLogId
     * @param importErrorLogs
     */
    private void checkAdjustLimit(MpRollAdjustContextDTO contextDTO,
            Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap, MpWeekRollAdjustEngine weekRollAdjustEngine,
            MpAdjustDailyCapacityLimit adjustDailyCapacityLimitObj, Long importLogId,
            List<ImportErrorLog> importErrorLogs) {
        // 初始化校验相关逻辑的上下文
        List<FactoryMonthPlanFinalAdjustVo> finalAdjustList = contextDTO.getFactoryMonthPlanProdFinalList();
        if (CollectionUtils.isEmpty(finalAdjustList)) {
            return;
        }
        // 初始化计划的模壳
        for (FactoryMonthPlanFinalAdjustVo adjustVo : finalAdjustList) {
            String key = getSpecAndMainPatternKey(adjustVo.getSpecifications(), adjustVo.getMainPattern());
            String mouldShell = contextDTO.getMdmMouldInfoMap().get(key);
            if (StringUtils.isNotBlank(mouldShell)) {
                adjustVo.setMouldShell(mouldShell);
            }
        }
        mpAdjustStructureInStrategy.handleMonthPlanStatistics(contextDTO, null);
        // 1、校验模壳数
        mpAdjustStructureInStrategy.checkMouldShellLimit(contextDTO);
        // 2、校验卡盘数
        mpAdjustStructureInStrategy.checkCapsuleChuckLimit(contextDTO);
        // 3、校验活块数
        String blockNumLimitStr = I18nUtil.getMessage("alg.data.mp.weekRollAdjust.confirm.blockNumLimit"); // 超活块数校验
        StringBuilder sbError = new StringBuilder(); // 记录校验异常的信息
        for (int i = 0, size = finalAdjustList.size(); i < size; i++) {
            FactoryMonthPlanFinalAdjustVo mpFinalVo = finalAdjustList.get(i);
            String structureName = mpFinalVo.getStructureName();
            String materialCode = mpFinalVo.getMaterialCode();
            Map<String, Object> adjustParam = contextDTO.getParamMap();
            for (int day = mpFinalVo.getBeginDay(); day <= mpFinalVo.getEndDay(); day++) {
                if (intValue(mpFinalVo.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + day)) <= 0) {
                    continue;
                }
                MpDailyCapacityLimitVo dailyCapacity = dailyCapacityMap.get(day);
                int blockQty = mpFinalVo.getTypeBlockQty();
                int moulds = weekRollAdjustEngine.getMouldByDay(adjustDailyCapacityLimitObj, adjustParam, day,
                        mpFinalVo, dailyCapacity);
                if (moulds > blockQty) {
                    String errorMsg = String.format(blockNumLimitStr, structureName, materialCode, day, moulds,
                            blockQty);
                    this.appendErrorMessage(sbError, errorMsg);
                    continue;
                }
            }
        }
        if (!StringUtil.isEmptyWithTrim(sbError.toString())) {
            throw new BusinessException(sbError.toString());
        }
    }

    /**
     * 初始化调整上下文
     * @param importResult
     * @return
     */
    private MpRollAdjustContextDTO initAdjustContext(FactoryMonthPlanMouldDayResult importResult) {
        String factoryCode = importResult.getFactoryCode();
        String productTypeCode = importResult.getProductTypeCode();
        String productionVersion = importResult.getProductionVersion();
        Integer year = importResult.getYear();
        Integer month = importResult.getMonth();
        MpRollAdjustContextDTO contextDTO = mpAdjustStructureInStrategy.initContextDTO(factoryCode, productTypeCode);
        contextDTO.setMpYear(year);
        contextDTO.setMpMonth(month);
        contextDTO.setProductionVersion(productionVersion);
        return contextDTO;
    }

    /**
     * 构建调整对象
     * @param insertItem
     * @return
     */
    private FactoryMonthPlanFinalAdjustVo castToAdjustVo(FactoryMonthPlanMouldDayResult insertItem) {
        FactoryMonthPlanFinalAdjustVo mpFinalVo = new FactoryMonthPlanFinalAdjustVo();
        mpFinalVo.setMaterialCode(insertItem.getMaterialDesc());
        mpFinalVo.setMaterialDesc(insertItem.getMaterialCode());
        mpFinalVo.setSpecifications(insertItem.getSpecifications());
        mpFinalVo.setMainPattern(insertItem.getMainPattern());
        mpFinalVo.setDayVulcanizationQty(insertItem.getDayVulcanizationQty());
        mpFinalVo.setTypeBlockQty(insertItem.getTypeBlockQty());
        mpFinalVo.setStructureName(insertItem.getStructureName());
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String dayField = FactoryConstant.DAY_FIELD + day;
            int planQty = intValue(insertItem.getFieldValueByFieldName(dayField));
            mpFinalVo.setFieldValueByFieldName(dayField, planQty);
            if (planQty > 0) {
                if (intValue(mpFinalVo.getBeginDay()) == 0) {
                    mpFinalVo.setBeginDay(day);
                }
                mpFinalVo.setEndDay(day);
            }
        }
        return mpFinalVo;
    }
    
    /**
     * 加载硫化日产
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    private Map<Integer, MpDailyCapacityLimitVo> loadDailyCapacityMap(String factoryCode, Integer year, Integer month) {
        // 加载排产日历
        MpStructureAllocation mpStructureAllocation = new MpStructureAllocation();
        mpStructureAllocation.setFactoryCode(factoryCode);
        mpStructureAllocation.setYear(year);
        mpStructureAllocation.setMonth(month);
        List<MdmWorkCalendar> calendarList = this.queryMdmWorkCalendar(mpStructureAllocation);
        Map<Integer, MpDailyCapacityLimitVo> dailyCapacityMap = new HashMap<>();
        Set<Integer> stopDaySet = new HashSet<>();
        for (MdmWorkCalendar workCalendar : calendarList) {
            MpDailyCapacityLimitVo limitVo = new MpDailyCapacityLimitVo();
            Integer day = workCalendar.getDay();
            Integer lastDay = day - 1;
            boolean isOpenProductionFirstDay = false;
            if (Objects.equals(workCalendar.getDayFlag(), YesOrNoEnum.YES.getCode())) { // 在产
                if (stopDaySet.contains(lastDay)) { // 检查上一天是否停产
                    isOpenProductionFirstDay = true;
                }
            } else { // 停产
                stopDaySet.add(day);
            }
            limitVo.setDayProductionRate(workCalendar.getRate());
            limitVo.setOpenProductionFirstDay(isOpenProductionFirstDay);
            dailyCapacityMap.put(day, limitVo);
        }
        return dailyCapacityMap;
    }

    /**
     * MdmModelInfo转Map
     */
    private Map<String, String> convertToMouldInfoMap(List<MoldCavityInsertMaxValueCalculatorVo> mouldInfoList) {
        if (PubUtil.isEmpty(mouldInfoList)) {
            return Collections.emptyMap();
        }

        return mouldInfoList.stream()
                .filter(info -> info != null
                        && info.getSpecifications() != null
                        && info.getMainPattern() != null
                        && info.getShellStandard() != null)
                .collect(Collectors.groupingBy(
                        info -> getSpecAndMainPatternKey(info.getSpecifications(),info.getMainPattern()),
                        Collectors.mapping(
                                MoldCavityInsertMaxValueCalculatorVo::getShellStandard,
                                Collectors.collectingAndThen(
                                        Collectors.toSet(),
                                        set -> String.join(BusiConstant.WeekRollAdjust.SPLIT_COMMA, set)
                                )
                        )
                ));
    }

    /**
     * 获取规格+主花纹Key
     * @param spec
     * @param mainPattern
     * @return
     */
    private String getSpecAndMainPatternKey(String spec, String mainPattern){
        return spec + BusiConstant.WeekRollAdjust.SPLIT_GROUP_KEY + mainPattern;
    }

    /**
     * 产品状态与施工类型映射
     *
     * @param trialStatus
     * @return
     */
    protected String transferTrialStatusToStage(String trialStatus) {
        String constructionStage;
        if (ConstructionStageEnum.TRIAL_FLAG.equals(trialStatus)) {
            constructionStage = com.zlt.aps.lh.api.enums.ConstructionStageEnum.MASS_TRIAL.getCode();
        } else if (ConstructionStageEnum.MEASUREMENT_FLAG.equals(trialStatus)) {
            constructionStage = com.zlt.aps.lh.api.enums.ConstructionStageEnum.TRIAL.getCode();
        } else if (ConstructionStageEnum.FORMAL_FLAG.equals(trialStatus)) {
            constructionStage = com.zlt.aps.lh.api.enums.ConstructionStageEnum.FORMAL.getCode();
        } else {
            constructionStage = com.zlt.aps.lh.api.enums.ConstructionStageEnum.NO_PROCESS.getCode();
        }
        return constructionStage;
    }

    /**
     * 给结构转产表开始日期、结束日期赋值
     *
     * @param item
     */
    private void setBeginDayAndEndDay(MpStructureAllocation item) {
        if (item.getBeginDay() != null && item.getEndDay() != null) {
            return;
        }
        for (int i = FactoryConstant.MONTH_START_DAY; i <= FactoryConstant.MONTH_MAX_DAY; i++) {
            Object fieldValue = item.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + i);
            if (ObjUtil.isNotNull(fieldValue)) {
                if (item.getBeginDay() == null) {
                    item.setBeginDay(i);
                }
                item.setEndDay(i);
            }
        }
    }

    /**
     * 给月计划表开始日期、结束日期赋值
     *
     * @param item
     */
    private void setBeginDayAndEndDay(FactoryMonthPlanMouldDayResult item) {
        //20260624+ 先清除开始日期，再重新赋值
        item.setBeginDay(null);
        for (int i = FactoryConstant.MONTH_START_DAY; i <= FactoryConstant.MONTH_MAX_DAY; i++) {
            Object fieldValue = item.getFieldValueByFieldName(FactoryConstant.DAY_FIELD + i);
            if (ObjUtil.isNotNull(fieldValue) && Integer.parseInt(fieldValue.toString()) > 0) {
                if (item.getBeginDay() == null) {
                    item.setBeginDay(i);
                }
                item.setEndDay(i);
            }
        }
    }


    /**
     * 设置是否特殊材料
     *
     * @param factoryCode        分厂编号
     * @param mpAdjustResultList 调整列表
     */
    private void setSpecialMaterial(String factoryCode, List<MpAdjustResult> mpAdjustResultList) {
        if (com.zlt.aps.mp.common.utils.PubUtil.isEmpty(mpAdjustResultList)) {
            return;
        }

        // 创建计时器
        StopWatch watch = new StopWatch();
        watch.start();

        // 查询BOM物料消耗明细
        CompletableFuture<List<MdmMaterialConsumeDetail>> materialConsumeDetailFuture = CompletableFuture.supplyAsync(
                () -> queryMaterialConsumeDetailList(factoryCode)
        );
        // 查询特殊材料记录
        CompletableFuture<List<RawSpecialMaterialRecord>> rawSpecialMaterialRecordFuture = CompletableFuture.supplyAsync(
                () -> querySpecialMaterialRecordList(factoryCode)
        );

        try {
            // 等待所有异步任务执行完成
            CompletableFuture.allOf(
                    materialConsumeDetailFuture,
                    rawSpecialMaterialRecordFuture
            ).join();

            log.info("设置是否特殊材料 ==> 并行查询数据执行完成");

        } catch (CompletionException e) {
            // 异常处理
            Throwable throwable = e.getCause();
            log.error("查询数据失败! 失败原因:{}", throwable.getMessage(), throwable);
            throw new BusinessException(I18nUtil.getMessage("ui.data.alert.mpWeekRollAdjust.initDataFailure"), throwable);
        } finally {
            watch.stop();
        }

        List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList = materialConsumeDetailFuture.join();
        List<RawSpecialMaterialRecord> specialMaterialList = rawSpecialMaterialRecordFuture.join();

        for (MpAdjustResult adjustResult : mpAdjustResultList) {
            // 设置是否含有特殊材料
            boolean isHasSpecialMaterial = rawSpecialMaterialRecordService.hasSpecialMaterial(adjustResult.getEmbryoCode(), mdmMaterialConsumeDetailList, specialMaterialList);
            adjustResult.setHasSpecialMaterial(isHasSpecialMaterial ? ApsConstant.TRUE : ApsConstant.FALSE);
        }
    }

    /**
     * 查询BOM物料消耗明细
     *
     * @param factoryCode 分厂编号
     */
    @SuppressWarnings("unchecked")
    private List<MdmMaterialConsumeDetail> queryMaterialConsumeDetailList(String factoryCode) {
        MdmMaterialConsumeDetail queryVO = new MdmMaterialConsumeDetail();
        queryVO.setFactoryCode(factoryCode);

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        return dataManager.listMaterialConsumeDetails(dataDTO);
    }

    /**
     * 查询特殊材料记录
     *
     * @param factoryCode 分厂编号
     */
    @SuppressWarnings("unchecked")
    private List<RawSpecialMaterialRecord> querySpecialMaterialRecordList(String factoryCode) {
        RawSpecialMaterialRecord queryVO = new RawSpecialMaterialRecord();
        queryVO.setFactoryCode(factoryCode);

        String cacheKey = dataManager.generateCacheKey(queryVO.getFactoryCode());
        DataDTO dataDTO = dataManager.buildDataDTO(queryVO, cacheKey, Boolean.TRUE);
        return dataManager.listSpecialMaterials(dataDTO);
    }
}
