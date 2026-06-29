package com.zlt.aps.mp.factory.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialStockEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.MouldAllocationInfoVo;
import com.zlt.aps.mp.engine.domain.vo.SkuMoldCapacityInfoVo;
import com.zlt.aps.mp.engine.handler.MoldCapacityLimitAllocateHandler;
import com.zlt.aps.mp.engine.mapper.FactoryMonthPlanProductMouldMapper;
import com.zlt.aps.mp.engine.mapper.MpStructureAllocationMapper;
import com.zlt.aps.mp.enums.MonthPlanExportDataTypeEnum;
import com.zlt.aps.mp.factory.dto.FactoryMonthPlanMouldDayResultExportVo;
import com.zlt.aps.mp.factory.dto.MpMonthPlanExportWarningConfigVo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanMouldDayResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.SpecialMaterialResultEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanMouldDayResultService;
import com.zlt.aps.mp.factory.service.IMpMonthPlanStatisticsService;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ApsNumberUtils.intValue;
import static com.zlt.aps.common.core.utils.ApsNumberUtils.safeAdd;
import static com.zlt.aps.common.core.utils.ApsNumberUtils.safeAddDefaultNull;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanMouldDayResultServiceImpl.java
 * 描    述：FactoryMonthPlanMouldDayResultServiceImplS2-0604.排产结果-生产计划排产结果业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class FactoryMonthPlanMouldDayResultServiceImpl extends AbstractDocService<FactoryMonthPlanMouldDayResult> implements IFactoryMonthPlanMouldDayResultService {
    @Autowired
    private FactoryMonthPlanMouldDayResultEntityMapper factoryMonthPlanMouldDayResultEntityMapper;

    private final IMpMonthPlanStatisticsService monthPlanStatisticsService;
    @Autowired
    private MpStructureAllocationMapper mpStructureAllocationMapper;
    @Autowired
    private MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;
    @Autowired
    private SpecialMaterialResultEntityMapper specialMaterialResultEntityMapper;
    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper factoryMonthPlanProductionFinalResultEntityMapper;
    @Autowired
    protected RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;
    @Autowired
    private RawSpecialMaterialStockEntityMapper rawSpecialMaterialStockEntityMapper;
    @Autowired
    private MdmWorkCalendarEntityMapper mdmWorkCalendarEntityMapper;
    @Autowired
    private FactoryMonthPlanProductMouldMapper factoryMonthPlanProductMouldMapper;
    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;
    @Autowired
    private IFactoryParamService factoryParamService;
    @Autowired
    private MoldCapacityLimitAllocateHandler moldCapacityLimitAllocateHandler;

    /**
     * 日计划字段名称
     */
    private final static String DAY_FIELD_NAME_FORMAT = "day%s";
    /**
     * 日统计量预警行小标
     */
    private final static int DAY_TOTAL_WARNING_ROW_INDEX = 1;
    /**
     * 月底计划字段名称
     */
    private final static String LAST_FIELD_NAME_FORMAT = "lastDay%s";
    /**
     * 上月需加载的天数
     */
    private final static Integer LAST_MONTH_DAY = 10;

    @Override
    protected String getDocTypeCode() {
        return "11";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("11");
        return sysDocType;
    }

    @Override
    public String checkUnique(FactoryMonthPlanMouldDayResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.factoryMonthPlanMouldDayResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    /**
     * 获取导出数据
     */
    @Override
    public List<FactoryMonthPlanMouldDayResultExportVo> getExportList(FactoryMonthPlanMouldDayResult params,
                                                                      boolean isAllMaterial, boolean isFinal) {
        // 1、加载构建导出列表的各项数据
        // 1.1、加载月计划表头信息
        if (isFinal) {
            // 按调整版本取对应表格的数据
            this.loadFinalExportTableData(params);
        } else {
            this.loadExportTableData(params);
        }
        // 1.2、加载月计划模具排产明细
        List<FactoryMonthPlanMouldDayResultExportVo> recordList = factoryMonthPlanMouldDayResultEntityMapper
                .getExportList(params, isAllMaterial, isFinal);
        if (CollectionUtils.isEmpty(recordList)) {
            return recordList;
        }
        // 1.3.1、填充上个月的定稿记录信息，同时获取上个月的最后一天日期
        Integer lastDayOfMonth = this.fillLastFinalResultList(params, recordList);
        // 1.3.2、加载本次版本已生成的统计记录
        String productionVersion = params.getProductionVersion();
        Map<String, MpMonthPlanStatistics> statisticsMap = this.loadMpMonthPlanStatistics(params,
                productionVersion, isFinal);
        // 1.4、加载上个月的统计记录
        String lastProductionVersion = recordList.stream().map(FactoryMonthPlanMouldDayResultExportVo::getLastProductionVersion).filter(Objects::nonNull).findAny().orElse(null);
        Map<String, MpMonthPlanStatistics> lastStatisticsMap = this.loadMpMonthPlanStatistics(params,
                lastProductionVersion, isFinal);
        // 1.5、加载结构排产数据
        LambdaQueryWrapper<MpStructureAllocation> structureQueryWrapper = new LambdaQueryWrapper<>();
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, params.getFactoryCode());
        structureQueryWrapper.eq(MpStructureAllocation::getProductionVersion, params.getProductionVersion());
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, params.getFactoryCode());
        structureQueryWrapper.eq(StringUtils.isNotEmpty(params.getStructureName()), MpStructureAllocation::getStructureName, params.getStructureName());
        Map<String, MpStructureAllocation> structureAllocationMap = mpStructureAllocationMapper
                .selectList(structureQueryWrapper).stream().collect(
                        Collectors.toMap(MpStructureAllocation::getStructureName, Function.identity(), (s1, s2) -> s1));
        // 1.6、加载型腔数活块数 - 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> cavityResults = new HashMap<>(0);
        // 活块可用量（按物料描述分组）
        Map<String, Integer> insertResults = new HashMap<>(0);
        List<DailyMouldAvailabilityResult> moldResult = moldCavityInsertMaxValueCalculator
                .moldCavityInsertMaxValueCalculator(params.getYear(), params.getMonth(), params.getFactoryCode(),
                        null, null, true);
        if (CollectionUtils.isNotEmpty(moldResult)) {
            cavityResults = moldResult.get(0).getCavityResults();
            insertResults = moldResult.get(0).getInsertResults();
        }
        // 1.7、加载本月工作日历
        LambdaQueryWrapper<MdmWorkCalendar> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmWorkCalendar::getFactoryCode, params.getFactoryCode());
        queryWrapper.eq(MdmWorkCalendar::getYear, params.getYear());
        queryWrapper.eq(MdmWorkCalendar::getMonth, params.getMonth());
        queryWrapper.eq(MdmWorkCalendar::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MdmWorkCalendar::getProcCode, "01");
        Set<Integer> workDaySet = mdmWorkCalendarEntityMapper.selectList(queryWrapper).stream()
                .filter(wc -> ApsConstant.TRUE.equals(wc.getDayFlag())).map(MdmWorkCalendar::getDay)
                .distinct().collect(Collectors.toSet());

        // 1.8、加载结构模具分配比例
        List<MouldAllocationInfoVo> mouldAllocationInfoList = factoryMonthPlanProductMouldMapper
                .getMouldAllocationInfo(params.getFactoryCode(), params.getYear(), params.getMonth());

        // 2、构建导出总表-导出数据总表
        List<FactoryMonthPlanMouldDayResultExportVo> totalRecordList = new LinkedList<>();
        // 小计列表
        List<FactoryMonthPlanMouldDayResultExportVo> subtotalList = new ArrayList<>();
        // 2.1、按结构遍历每一组排产明细记录，并构建该结构的明细数据 + 胎胚总类汇总 + 小计数据- 当前结构名称
        String structureName;
        // 同结构排产记录列表
        List<FactoryMonthPlanMouldDayResultExportVo> structureList = new ArrayList<>();
        Map<Integer, Integer> changeMouldMap = new HashMap<>();
        Map<Integer, Integer> lastChangeMouldMap = new HashMap<>();
        // 初始化汇总map
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            changeMouldMap.put(day, 0);
        }
        for (Integer i = 0, size = recordList.size(); i < size; i++) {
            // 2.1.1、把同结构的排产记录添加到列表中，全部添加完后开始处理这一批数据
            FactoryMonthPlanMouldDayResultExportVo record = recordList.get(i);
            //20260608+ 型腔数、活字块取最新
            record.setMouldCavityQty(cavityResults.getOrDefault(record.getStructureName() + record.getMainPattern(), 0));
            record.setTypeBlockQty(insertResults.getOrDefault(record.getMaterialDesc(), 0));
            // 先添加到列表
            structureList.add(record);
            // 更新结构
            structureName = record.getStructureName();
            // 还不是最后一行，则校验下一行是否同一个结构
            if (i < size - 1) {
                // 下一笔结构没有变化，且还不是最后一笔记录，继续遍历下一笔数据
                FactoryMonthPlanMouldDayResultExportVo nextRecord = recordList.get(i + 1);
                // 结构没有变化，则添继续往下
                if (structureName.equals(nextRecord.getStructureName())) {
                    continue;
                }
            }

            // 2.1.2、把明细记录添加到总表- 主花纹的最大型腔数
            Map<String, Integer> maxMouldCavityQtyMap = new HashMap<>();
            // 物料的最大活块数
            Map<String, Integer> maxTypeBlockQtyMap = new HashMap<>();
            // 部分数据额外处理
            for (FactoryMonthPlanMouldDayResultExportVo result : structureList) {
                // 2.1.2.1、日硫化量调整为双模
                if (result.getDayVulcanizationQty() != null) {
                    result.setDayVulcanizationQty(result.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION);
                }
                // 2.1.2.2、未排量负数处理
                Integer prodReqPlan;
                if (isFinal) {
                    // 定稿只看高中
                    prodReqPlan = intValue(result.getHeightQty()) + intValue(result.getMidQty());
                } else {
                    prodReqPlan = intValue(result.getHeightQty()) + intValue(result.getMidQty())
                            + intValue(result.getCycleReserveQty()) + intValue(result.getConventionReserveQty());
                }
                Integer differenceQty = prodReqPlan - intValue(result.getTotalQty());
                result.setDifferenceQty(differenceQty >= 0 ? differenceQty : 0);
                result.setProdReqPlan(prodReqPlan);

                // 2.1.2.3、实单未排产 = 高优先级 + 中优先级 - 高优先级实际 - 中优先级实际，如果为负数则设为0
                Integer heightQty = intValue(result.getHeightQty());
                Integer midQty = intValue(result.getMidQty());
                // 重新分配高、中、周期、暂缓、储备的生产量
                result.setHeightLossQty(heightQty);
                result.setMidLossQty(midQty);
                result.setCycleReserveLossQty(intValue(result.getCycleReserveQty()));
                result.allocateProductionByPriority();
                Integer heightProductionQty = Optional.ofNullable(result.getHeightProductionQty()).orElse(0);
                Integer midProductionQty = Optional.ofNullable(result.getMidProductionQty()).orElse(0);
                Integer actualOrderUnproduced = heightQty + midQty - heightProductionQty - midProductionQty;
                result.setActualOrderUnproduced(actualOrderUnproduced > 0 ? actualOrderUnproduced : 0);
                // 2.1.2.4、补充型腔数
                if (result.getMouldCavityQty() == null || result.getMouldCavityQty() == 0) {
                    result.setMouldCavityQty(cavityResults.getOrDefault(result.getStructureName() + result.getMainPattern(), 0));
                }
//                // 2.1.2.5、记录胎胚的最大型腔数
//                Integer maxMouldCavityQty = maxMouldCavityQtyMap.getOrDefault(result.getMainMaterialDesc(), 0);
//                maxMouldCavityQtyMap.put(result.getMainMaterialDesc(), Math.max(maxMouldCavityQty, result.getMouldCavityQty()));
                // 2.1.2.5 补充活块数
                if (result.getTypeBlockQty() == null || result.getTypeBlockQty() == 0) {
                    result.setTypeBlockQty(insertResults.getOrDefault(result.getMaterialDesc(), 0));
                }
//                // 2.1.2.6、记录胎胚的最大活块数
//                Integer maxTypeBlockQty = maxTypeBlockQtyMap.getOrDefault(result.getMainMaterialDesc(), 0);
//                maxTypeBlockQtyMap.put(result.getMainMaterialDesc(), Math.max(maxTypeBlockQty, result.getTypeBlockQty()));
                // 2.1.2.7、计算模具产能受限
                Integer unRestrictedNetQty = result.getUnRestrictedNetQty();
                if (unRestrictedNetQty != null) {
                    Integer restrictedNetQty = intValue(result.getProdReqPlan()) - unRestrictedNetQty;
                    restrictedNetQty = restrictedNetQty < 0 ? 0 : restrictedNetQty;
                    result.setRestrictedNetQty(restrictedNetQty);
                }
                //2.1.2.8、处理定稿表特有的字段
                if (isFinal) {
                    // 计算每一周的调整排产量，第一周调整排产量=原始排产量 + 第一周调整量
                    Integer adjustProductQty1 = intValue(result.getOriginalTotalQty()) + intValue(result.getAdjustQty1());
                    result.setAdjustProductQty1(adjustProductQty1);
                    // 第二周调整排产量=第一周调整排产量 + 第二周调整量，第三周以此类推……
                    Integer adjustProductQty2 = adjustProductQty1 + intValue(result.getAdjustQty2());
                    result.setAdjustProductQty2(adjustProductQty2);
                    Integer adjustProductQty3 = adjustProductQty2 + intValue(result.getAdjustQty3());
                    result.setAdjustProductQty3(adjustProductQty3);
                    Integer adjustProductQty4 = adjustProductQty3 + intValue(result.getAdjustQty4());
                    result.setAdjustProductQty4(adjustProductQty4);
                    // 待调整量 = 净需求量 - 本月剩余量 - 上月剩余量
                    result.setPendingQty(intValue(result.getProdReqPlan()) - intValue(result.getProductSurplus()) - intValue(result.getLastMonthRemainQty()));
                }
            }
            // 2.1.2.6、设置对应的最大型腔数和最大活块数:重新对结构内的数据排序：主花纹分组，按型胎胚描述，最大腔数倒序、主花纹、最大活块数倒序，主花纹组内按型腔数倒序、活块数倒序排序
            structureList.stream().forEach(s -> {
                s.setMaxMouldCavityQty(maxMouldCavityQtyMap.getOrDefault(s.getMainMaterialDesc(), 0));
                s.setMaxTypeBlockQty(maxTypeBlockQtyMap.getOrDefault(s.getMainMaterialDesc(), 0));
            });
            //排序:最大型腔数倒序->胎胚描述->型腔数倒序->主花纹->活块数倒序->花纹->物料描述
            structureList.sort(Comparator.comparing(FactoryMonthPlanMouldDayResultExportVo::getMouldCavityQty, Comparator.reverseOrder())
                    .thenComparing(FactoryMonthPlanMouldDayResultExportVo::getMainMaterialDesc)
//                    .thenComparing(FactoryMonthPlanMouldDayResultExportVo::getMouldCavityQty, Comparator.reverseOrder())
                    .thenComparing(FactoryMonthPlanMouldDayResultExportVo::getMainPattern, Comparator.nullsLast(String::compareTo))
                    .thenComparing(FactoryMonthPlanMouldDayResultExportVo::getTypeBlockQty, Comparator.reverseOrder())
                    .thenComparing(FactoryMonthPlanMouldDayResultExportVo::getPattern, Comparator.nullsLast(String::compareTo))
                    .thenComparing(FactoryMonthPlanMouldDayResultExportVo::getMaterialDesc, Comparator.nullsLast(String::compareTo))
            );
            totalRecordList.addAll(structureList);

            // 2.1.2.7、如果是导出定稿版本，需要计算模具产能受限情况
            if (isFinal) {
                this.moldCapacityAllocate(structureName, structureList, workDaySet, mouldAllocationInfoList);
            }

            // 2.1.3、添加结构排产信息汇总行（胎胚种类数、硫化机台数）
            FactoryMonthPlanMouldDayResultExportVo embryoCountStatisticsRecord = new FactoryMonthPlanMouldDayResultExportVo();
            embryoCountStatisticsRecord.setStructureName(structureName);
            embryoCountStatisticsRecord.setDataType(MonthPlanExportDataTypeEnum.EMBRYO_TYPE_COUNT.getCode());
            embryoCountStatisticsRecord.setProductCategory(I18nUtil.getMessage(MonthPlanExportDataTypeEnum.EMBRYO_TYPE_COUNT.getName()));
            FactoryMonthPlanMouldDayResultExportVo lhMachinesStatisticsRecord = new FactoryMonthPlanMouldDayResultExportVo();
            lhMachinesStatisticsRecord.setStructureName(structureName);
            lhMachinesStatisticsRecord.setDataType(MonthPlanExportDataTypeEnum.LH_MACHINES.getCode());
            lhMachinesStatisticsRecord.setProductCategory(I18nUtil.getMessage(MonthPlanExportDataTypeEnum.LH_MACHINES.getName()));
            // 2.1.4、本月统计信息汇总
            MpMonthPlanStatistics statistics = statisticsMap.get(structureName);
            this.buildStatisticsRecord(embryoCountStatisticsRecord, lhMachinesStatisticsRecord, FactoryConstant.MONTH_START_DAY, statistics,
                    changeMouldMap, DAY_FIELD_NAME_FORMAT);
            // 2.1.5、上个月统计信息汇总，只取上个月最后10天的数据
            MpMonthPlanStatistics lastStatistics = lastStatisticsMap.get(structureName);
            this.buildStatisticsRecord(embryoCountStatisticsRecord, lhMachinesStatisticsRecord, lastDayOfMonth - LAST_MONTH_DAY + 1, lastStatistics,
                    lastChangeMouldMap, LAST_FIELD_NAME_FORMAT);

            totalRecordList.add(embryoCountStatisticsRecord);
            totalRecordList.add(lhMachinesStatisticsRecord);

            // 2.1.6、构建小计行
            FactoryMonthPlanMouldDayResultExportVo subtotalRecord = this.buildSubtotalRecord(structureName,
                    structureList, structureAllocationMap, MonthPlanExportDataTypeEnum.SUBTOTAL);
            // 添加至小计表，最后需要将小计汇总成总计
            subtotalList.add(subtotalRecord);
            // 添加至总表
            totalRecordList.add(subtotalRecord);

            // 2.1.5、结束本结束数据构建，清空数据
            structureList.clear();
        }

        // 3、构建总计行 将小计列表汇总为总计
        FactoryMonthPlanMouldDayResultExportVo totalRecord = this.buildSubtotalRecord("", subtotalList, null,
                MonthPlanExportDataTypeEnum.TOTAL);
        // 添加至总表
        totalRecordList.add(totalRecord);

        // 4、构建换模次数列
        FactoryMonthPlanMouldDayResultExportVo changeMouldStatisticsRecord = new FactoryMonthPlanMouldDayResultExportVo();
        changeMouldStatisticsRecord.setDataType(MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getCode());
        changeMouldStatisticsRecord.setProductCategory(I18nUtil.getMessage(MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getName()));
        // 4.1、本月换模统计添加到统计行中
        for (Entry<Integer, Integer> entry : changeMouldMap.entrySet()) {
            Integer day = entry.getKey();
            Integer mould = entry.getValue();
            changeMouldStatisticsRecord.setFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, day), mould);
        }
        // 4.2、上月换模统计添加到统计行中
        for (Entry<Integer, Integer> entry : lastChangeMouldMap.entrySet()) {
            Integer day = entry.getKey();
            Integer mould = entry.getValue();
            Integer realDay = lastDayOfMonth - day + 1; // 日期映射为倒数第n天
            if (realDay <= 0 || realDay > LAST_MONTH_DAY) {
                continue;
            }
            changeMouldStatisticsRecord.setFieldValueByFieldName(String.format(LAST_FIELD_NAME_FORMAT, realDay), mould);
        }
        totalRecordList.add(changeMouldStatisticsRecord);
        return totalRecordList;
    }

    /**
     * 填充上个月的定稿记录信息，并返回上个月最后一天的日期
     * @param params
     * @param recordList
     */
    private Integer fillLastFinalResultList(FactoryMonthPlanMouldDayResult params,
            List<FactoryMonthPlanMouldDayResultExportVo> recordList) {
        // 加载上个月的定稿记录
        Calendar calendar = Calendar.getInstance();
        calendar.set(params.getYear(), params.getMonth() - 1, 1); // 通过日历获取上本月一号的日历
        calendar.add(Calendar.DAY_OF_MONTH, -1); // 切换到上个月最后一天
        Integer lastYear = calendar.get(Calendar.YEAR);
        Integer lastMonth = calendar.get(Calendar.MONTH) + 1;
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> resultQueryWrapper = new LambdaQueryWrapper<>();
        resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, params.getFactoryCode());
        resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getYear, lastYear);
        resultQueryWrapper.eq(FactoryMonthPlanProductionFinalResult::getMonth, lastMonth);
        resultQueryWrapper.eq(StringUtils.isNotEmpty(params.getStructureName()), FactoryMonthPlanProductionFinalResult::getStructureName, params.getStructureName());
        List<FactoryMonthPlanProductionFinalResult> lastFinalResultList = factoryMonthPlanProductionFinalResultEntityMapper.selectList(resultQueryWrapper);
        Integer lastDayOfMonth = FactoryConstant.MONTH_MAX_DAY;
        // 将上月计划填装到排产明细表中
        if (CollectionUtils.isNotEmpty(lastFinalResultList) && CollectionUtils.isNotEmpty(recordList)) {
            // 计算上个月的天数
            Calendar cal = Calendar.getInstance();
            cal.set(lastYear, lastMonth - 1, 1);
            lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            // 按物料号+施工阶段构建映射
            Map<String, FactoryMonthPlanProductionFinalResult> lastFinalResultMap = new HashMap<>();
            for (FactoryMonthPlanProductionFinalResult finalResult : lastFinalResultList) {
                String key = GenerageMapKeyUtils.createMapKey(finalResult.getMaterialCode(), finalResult.getConstructionStage());
                lastFinalResultMap.put(key, finalResult);
            }
            // 已匹配的 key 集合
            Set<String> matchedKeys = new HashSet<>();

            // 遍历 recordList，匹配上则赋值 lastDay1~lastDay10
            for (FactoryMonthPlanMouldDayResultExportVo record : recordList) {
                String key = GenerageMapKeyUtils.createMapKey(record.getMaterialCode(), record.getConstructionStage());
                FactoryMonthPlanProductionFinalResult matchedFinal = lastFinalResultMap.get(key);
                if (matchedFinal != null) {
                    matchedKeys.add(key);
                    for (int i = 1; i <= LAST_MONTH_DAY; i++) {
                        int dayIndex = lastDayOfMonth - i + 1; // dayIndex = (lastDay - 10 + i)，如31天月时：22,23,...,31
                        Integer dayValue = (Integer) matchedFinal.getFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, dayIndex));
                        record.setFieldValueByFieldName(String.format(LAST_FIELD_NAME_FORMAT, i), dayValue);
                    }
                }
            }
            // 加载上月有排产本月没有排产的物料
            Set<String> noMatchMaterialCodeSet = lastFinalResultList.stream()
                    .filter(r -> !matchedKeys
                            .contains(GenerageMapKeyUtils.createMapKey(r.getMaterialCode(), r.getConstructionStage())))
                    .map(FactoryMonthPlanProductionFinalResult::getMaterialCode).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(noMatchMaterialCodeSet)) { // 如果没有匹配不上的情况，直接返回
                return lastDayOfMonth;
            }

            LambdaQueryWrapper<MdmMaterialInfo> mdmMaterialInfoQueryWrapper = new LambdaQueryWrapper<>();
            mdmMaterialInfoQueryWrapper.eq(MdmMaterialInfo::getFactoryCode, params.getFactoryCode());
            mdmMaterialInfoQueryWrapper.in(MdmMaterialInfo::getMaterialCode, noMatchMaterialCodeSet);
            Map<String, MdmMaterialInfo> materialInfoMap = mdmMaterialInfoEntityMapper
                    .selectList(mdmMaterialInfoQueryWrapper).stream()
                    .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (m1, m2) -> m1));

            // 未匹配的 lastFinalResult 新增到 recordList
            for (FactoryMonthPlanProductionFinalResult finalResult : lastFinalResultList) {
                String key = GenerageMapKeyUtils.createMapKey(finalResult.getMaterialCode(), finalResult.getConstructionStage());
                if (matchedKeys.contains(key)) {
                    continue;
                }

                // 创建新记录，复制基础字段
                FactoryMonthPlanMouldDayResultExportVo newRecord = new FactoryMonthPlanMouldDayResultExportVo();
                newRecord.setLocationType("2"); // TODO 暂时固定外销
                newRecord.setMaterialCode(finalResult.getMaterialCode());
                newRecord.setMaterialDesc(finalResult.getMaterialDesc());
                newRecord.setMesMaterialCode(finalResult.getMesMaterialCode());
                newRecord.setStructureName(finalResult.getStructureName());
                newRecord.setConstructionStage(finalResult.getConstructionStage());
                newRecord.setMainMaterialDesc(finalResult.getMainMaterialDesc());
                newRecord.setMainPattern(finalResult.getMainPattern());
                newRecord.setPattern(finalResult.getPattern());
                newRecord.setSpecifications(finalResult.getSpecifications());
                newRecord.setProSize(finalResult.getProSize());
                newRecord.setBrand(finalResult.getBrand());
                newRecord.setEmbryoCode(finalResult.getEmbryoCode());
                newRecord.setProductCategory(finalResult.getProductCategory());
                newRecord.setProductStatus(finalResult.getProductStatus());
                newRecord.setProductTypeCode(finalResult.getProductTypeCode());
                newRecord.setStructureType(finalResult.getStructureType());
                newRecord.setProductionType(finalResult.getProductionType());
                newRecord.setHeightQty(0);
                newRecord.setMidQty(0);
                newRecord.setMidQty(0);
                newRecord.setCycleReserveQty(0);
                newRecord.setPostponeQty(0);
                newRecord.setConventionReserveQty(0);
                newRecord.setTotalQty(0);
                newRecord.setLastProductionVersion(finalResult.getProductionVersion());
                newRecord.setDataType(MonthPlanExportDataTypeEnum.RECORD.getCode());
                newRecord.setDayVulcanizationQty(finalResult.getDayVulcanizationQty());
                newRecord.setAverageSaleQty(finalResult.getAverageSaleQty());
                newRecord.setInventorySalesRatio(finalResult.getInventorySalesRatio());
                MdmMaterialInfo material = materialInfoMap.get(key);
                if (material != null) {
                    newRecord.setSingleTireWeight(material.getSingleTireWeight());
                }
                // 赋值 lastDay1~lastDay10
                for (int i = 1; i <= LAST_MONTH_DAY; i++) {
                    int dayIndex = lastDayOfMonth - i + 1;
                    Integer dayValue = (Integer) finalResult.getFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, dayIndex));
                    newRecord.setFieldValueByFieldName(String.format(LAST_FIELD_NAME_FORMAT, i), dayValue);
                }

                // 查找插入位置：相同 StructureName 的最后一个元素之后
                int insertIndex = -1;
                for (int i = recordList.size() - 1; i >= 0; i--) {
                    if (finalResult.getStructureName() != null
                            && finalResult.getStructureName().equals(recordList.get(i).getStructureName())) {
                        insertIndex = i + 1;
                        break;
                    }
                }
                if (insertIndex == -1) {
                    // 没有相同 StructureName 的记录，插到末尾
                    recordList.add(newRecord);
                } else {
                    recordList.add(insertIndex, newRecord);
                }
            }
        }
        return lastDayOfMonth;
    }

    /**
     * 计算模具产能受限情况
     *
     * @param structureName
     * @param structureList
     * @param workDaySet
     */
    private void moldCapacityAllocate(String structureName, List<FactoryMonthPlanMouldDayResultExportVo> structureList,
                                      Set<Integer> workDaySet, List<MouldAllocationInfoVo> mouldAllocationInfoList) {
        //20260609+ 按结构+主花纹
        Map<String, List<FactoryMonthPlanMouldDayResultExportVo>> groupPlanMap = structureList.stream().collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResultExportVo::getGroupAndMainPattern));
        //最大可生产天数
        Integer maxProductionDays = workDaySet.size();
        //模具分配比例
        Map<String, Integer> mouldAllocationMap = mouldAllocationInfoList.stream().collect(Collectors.toMap(
                MouldAllocationInfoVo::getDuplicateKey, MouldAllocationInfoVo::getAllocationQty, (q1, q2) -> q1));
        // 计算最大可用模具产能 结构+主花纹
        Map<String, Integer> maxEnableMouldCapacityMap = getMoldMaxCapacity(groupPlanMap, maxProductionDays, mouldAllocationMap);
        /**
         * 20260609+ 按结构+主花纹分组 计算模具受限产能(模具最大产能、总净需求、总高优先级需求量)
         */
        List<SkuMoldCapacityInfoVo> groupResultList = Lists.newArrayList();
        // 计算产能受限
        groupPlanMap.forEach((structureAndMainPattern, groupList) -> {
            if (CollectionUtils.isEmpty(groupList)) {
                return;
            }
            //总净需求
            Integer sumNetQty = groupList.stream().mapToInt(FactoryMonthPlanMouldDayResultExportVo::getProdReqPlan).sum();
            //总高优先级量需求
            Integer sumHeightQty = groupList.stream().filter(result -> result.getHeightQty() != null)
                    .mapToInt(FactoryMonthPlanMouldDayResultExportVo::getHeightQty).sum();
            //最大模具产能
            Integer maxMoldCapacityQty = maxEnableMouldCapacityMap.getOrDefault(structureAndMainPattern, BigDecimal.ZERO.intValue());
            List<SkuMoldCapacityInfoVo> singleMainPatternSkuList = Lists.newArrayList();
            groupList.forEach(singleSku -> {
                SkuMoldCapacityInfoVo capacityInfo = buildSkuMoldCapacityInfo(structureName, singleSku, maxMoldCapacityQty, sumNetQty, sumHeightQty);
                singleMainPatternSkuList.add(capacityInfo);
            });
            if (CollectionUtils.isEmpty(singleMainPatternSkuList)) {
                return;
            }
            List<SkuMoldCapacityInfoVo> singleMainPatternList = moldCapacityLimitAllocateHandler.moldCapacityAllocateHandler(singleMainPatternSkuList);
            if (CollectionUtils.isEmpty(singleMainPatternList)) {
                return;
            }
            groupResultList.addAll(singleMainPatternList);
        });
        Map<String, Integer> moldCapacityAllocateMap = groupResultList.stream()
                .map(skuCapacityInfo -> skuCapacityInfo.buildLog()).collect(Collectors.toMap(
                        MpSkuMoldCapacityAllocateLog::getMaterialDesc, MpSkuMoldCapacityAllocateLog::getNetQty, (s1, s2) -> s1));
        // 重算根据模具产能受限重算净计算相关栏位
        structureList.forEach(result -> {
            Integer unRestrictedNetQty = moldCapacityAllocateMap.getOrDefault(result.getMaterialDesc(), BigDecimal.ZERO.intValue());
            Integer prodReqPlan = intValue(result.getProdReqPlan());
            result.setUnRestrictedNetQty(unRestrictedNetQty);
            // 不含模具受限
            if (!Objects.equals(prodReqPlan, unRestrictedNetQty)) {
                Integer restrictedNetQty = prodReqPlan - unRestrictedNetQty;
                restrictedNetQty = restrictedNetQty < 0 ? 0 : restrictedNetQty;
                result.setRestrictedNetQty(restrictedNetQty);
            } else {
                result.setRestrictedNetQty(null);
            }
        });
    }

    /**
     * 加载月计划表头信息
     *
     * @param params
     */
    private void loadExportTableData(FactoryMonthPlanMouldDayResult params) {
        QueryWrapper<FactoryMonthPlanMouldDayResult> resultQueryWrapper = new QueryWrapper<>();
        resultQueryWrapper.select("YEAR", "MONTH", "`YEAR_MONTH`", "MONTH_PLAN_VERSION", "PRODUCT_TYPE_CODE");
        resultQueryWrapper.groupBy("YEAR", "MONTH", "`YEAR_MONTH`", "MONTH_PLAN_VERSION", "PRODUCT_TYPE_CODE");
        resultQueryWrapper.eq("FACTORY_CODE", params.getFactoryCode());
        resultQueryWrapper.eq("PRODUCTION_VERSION", params.getProductionVersion());
        List<FactoryMonthPlanMouldDayResult> headList = factoryMonthPlanMouldDayResultEntityMapper.selectList(resultQueryWrapper);
        if (!CollectionUtils.isEmpty(headList)) {
            FactoryMonthPlanMouldDayResult head = headList.get(0);
            params.setYear(head.getYear());
            params.setMonth(head.getMonth());
            params.setYearMonth(head.getYearMonth());
            params.setProductTypeCode(head.getProductTypeCode());
            params.setMonthPlanVersion(head.getMonthPlanVersion());
        }
    }

    /**
     * 加载月计划表头信息
     *
     * @param params
     */
    private void loadFinalExportTableData(FactoryMonthPlanMouldDayResult params) {
        QueryWrapper<FactoryMonthPlanProductionFinalResult> resultQueryWrapper = new QueryWrapper<>();
        resultQueryWrapper.select("LAST_MONTH_PLAN_VERSION", "PRODUCT_TYPE_CODE", "PRODUCTION_VERSION");
        resultQueryWrapper.groupBy("LAST_MONTH_PLAN_VERSION", "PRODUCT_TYPE_CODE", "PRODUCTION_VERSION");
        resultQueryWrapper.eq("YEAR", params.getYear());
        resultQueryWrapper.eq("MONTH", params.getMonth());
        List<FactoryMonthPlanProductionFinalResult> headList = factoryMonthPlanProductionFinalResultEntityMapper.selectList(resultQueryWrapper);
        if (!CollectionUtils.isEmpty(headList)) {
            FactoryMonthPlanProductionFinalResult head = headList.get(0);
            params.setProductTypeCode(head.getProductTypeCode());
            params.setMonthPlanVersion(params.getProductionVersion());
            params.setProductionVersion(head.getProductionVersion());
        }
    }

    /**
     * 构建小计行
     *
     * @param embryoCountStatisticsRecord 胎胚统计
     * @param lhMachinesStatisticsRecord  硫化机统计
     * @param startDay                    统计开始日，上月统计和本月统计的开始日不一样
     * @param statistics                  统计数据
     * @param changeMouldMap              换模统计列表
     */
    private void buildStatisticsRecord(FactoryMonthPlanMouldDayResultExportVo embryoCountStatisticsRecord,
                                       FactoryMonthPlanMouldDayResultExportVo lhMachinesStatisticsRecord,
                                       Integer startDay, MpMonthPlanStatistics statistics,
                                       Map<Integer, Integer> changeMouldMap, String preFix) {
        if (statistics == null) {
            return;
        }
        for (int day = startDay; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String dayFieldGetterName = String.format(DAY_FIELD_NAME_FORMAT, day);
            String dayFieldSetterName;
            if (LAST_FIELD_NAME_FORMAT.equals(preFix)) {
                // 上个月统计数据：将日序号映射为月末倒数第n天
                Integer realDay = day - startDay + 1;
                if (realDay <= 0 || realDay > LAST_MONTH_DAY) {
                    continue;
                }
                dayFieldSetterName = String.format(LAST_FIELD_NAME_FORMAT, realDay);
            } else {
                dayFieldSetterName = String.format(preFix, day);
            }
            String dayStatisticsStr = (String) statistics.getFieldValueByFieldName(dayFieldGetterName);
            if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr,
                        MpDayProductionStatisticsDetailVo.class);
                embryoCountStatisticsRecord.setFieldValueByFieldName(dayFieldSetterName, dayStatistics.getEmbryoCount());
                lhMachinesStatisticsRecord.setFieldValueByFieldName(dayFieldSetterName, dayStatistics.getLhMachines());
                Integer changeMould = Optional.ofNullable(dayStatistics.getChangeMould()).orElse(0);
                if (changeMould > 0) {
                    changeMouldMap.put(day, changeMouldMap.getOrDefault(day, 0) + Optional.ofNullable(dayStatistics.getChangeMould()).orElse(0));
                }
            }
        }
    }

    /**
     * 加载月计划排产统计记录
     *
     * @param factoryMonthPlanMouldDayResult 查询条件
     * @param productionVersion              排产版本
     * @param isFinal                        月计划调整导出
     * @return
     */
    private Map<String, MpMonthPlanStatistics> loadMpMonthPlanStatistics(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                                         String productionVersion,
                                                                         boolean isFinal) {
        if (StringUtils.isEmpty(productionVersion)) {
            return new HashMap<>();
        }
        List<MpMonthPlanStatistics> statisticsList = monthPlanStatisticsService.getStatisticsInfo(factoryMonthPlanMouldDayResult, productionVersion, isFinal);
        if (CollectionUtils.isEmpty(statisticsList)) {
            return new HashMap<>();
        }

        Map<String, MpMonthPlanStatistics> statisticsMap = statisticsList.stream().collect(
                Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));
        return statisticsMap;
    }

    /**
     * 导出数据
     *
     * @param list
     * @return
     */
    @Override
    public byte[] getFactoryMonthPlanMouldDayResultExportByte(FactoryMonthPlanMouldDayResult queryResult,
                                                              List<FactoryMonthPlanMouldDayResultExportVo> list,
                                                              boolean isFinal) {
        // 1、获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        //20260604+ 日排产量预警处理
        int warningHeaderRowIndex = DAY_TOTAL_WARNING_ROW_INDEX;
        InputStream inputStream;
        if (isFinal) {
            inputStream = classLoader.getResourceAsStream("excelModel/factoryMonthPlanMouldFinalResultExportTemp.xlsx");
        } else {
            inputStream = classLoader.getResourceAsStream("excelModel/factoryMonthPlanMouldDayResultExportTemp.xlsx");
        }

        // 2、加载字典数据
        // 工厂名称字典
        List<SysDictData> factoryDatas = sysDictDataCacheService.getType("biz_factory_name");
        Map<String, String> factoryMap = factoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 内外销类型字典
        List<SysDictData> storTypeDatas = sysDictDataCacheService.getType("biz_stor_type");
        Map<String, String> storTypeMap = storTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 产品品类字典
        List<SysDictData> productTypeDatas = sysDictDataCacheService.getType("biz_product_type");
        Map<String, String> productTypeMap = productTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 产品分类字典
        List<SysDictData> productCategoryDatas = sysDictDataCacheService.getType("product_category");
        Map<String, String> productCategoryMap = productCategoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 产品状态字典
        List<SysDictData> productStatusDatas = sysDictDataCacheService.getType("trial_status");
        Map<String, String> productStatusMap = productStatusDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 施工阶段字典
        List<SysDictData> constructionStageDatas = sysDictDataCacheService.getType("biz_construction_stage");
        Map<String, String> constructionStageMap = constructionStageDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 品牌类型字典
        List<SysDictData> brandDatas = sysDictDataCacheService.getType("biz_brand_type");
        Map<String, String> brandMap = brandDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 结构类型字典
        List<SysDictData> structureTypeDatas = sysDictDataCacheService.getType("structure_type");
        Map<String, String> structureTypeMap = structureTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        // 是否字典
        List<SysDictData> yesNoDatas = sysDictDataCacheService.getType("biz_yes_no");
        Map<String, String> yesNoMap = yesNoDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        
        //20260604+ 预警配置
        MpMonthPlanExportWarningConfigVo warningConfiguration = getWarningConfiguration(queryResult);

        // 3、构建表格数据
        Map<String, Object> tableMap = new HashMap<>(16); // 总表
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>(); // 列表数据
        List<CellStyle> cellStyleList = new ArrayList<>(); // 样式列表
        int beginIndex = 2; // 起始行
        Map<String, Object> totalMap = new HashMap<>();
        if (PubUtil.isNotEmpty(list)) {
            // 3.1、根据上个月所在年份月计算最后十天日期
            String[] dayFieldNames = new String[LAST_MONTH_DAY];
            if (queryResult.getYear() != null && queryResult.getMonth() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(queryResult.getYear(), queryResult.getMonth() - 1, 1); // 通过日历获取本月一号的日历
                calendar.add(Calendar.MONTH, -1); // 切换到上个月
                Integer lastMonth = calendar.get(Calendar.MONTH) + 1;
                int lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // 上个月最后一天
                String lastDayFormat = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.lastMonth");
                for (int i = 1; i <= LAST_MONTH_DAY; i++) {
                    int day = lastDayOfMonth - i + 1; // 计算倒数第i天对应的是几号
                    dayFieldNames[i - 1] = String.format(LAST_FIELD_NAME_FORMAT, i);
                    tableMap.put(String.format(LAST_FIELD_NAME_FORMAT, i), String.format(lastDayFormat, lastMonth, day));
                }
            }

            // 3.2、加载表头国际化标签
            this.loadExportI18nTableName(tableMap, isFinal);

            // 3.3、需要增加的第二部分表头统计
            List<Map<String, Object>> headSummaryData = new ArrayList<>();
            List<FactoryMonthPlanMouldDayResultExportVo> headList = list.stream()
                    .filter(r -> MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getCode().equals(r.getDataType()) // 换模统计
                            || MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(r.getDataType())) // 计划量统计
                    .collect(Collectors.toList());
            int warningHeaderStartColumnIndex = tableMap.size() - 32;
            for (FactoryMonthPlanMouldDayResultExportVo exportVo : headList) {
                this.setLastDayValue(exportVo, dayFieldNames); // 设置上月最后十天的值
                Map<String, Object> listDataMap = this.buildListDataMap(exportVo, storTypeMap, productCategoryMap,
                        productStatusMap, constructionStageMap, brandMap, structureTypeMap, yesNoMap, "A", isFinal);
                headSummaryData.add(listDataMap);
                if (MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())) {
                    //20260604+ 日排产量预警处理
                    List<CellStyle> headerWaringStyleList = buildWarningStyleByHeader(warningConfiguration, exportVo, warningHeaderRowIndex, warningHeaderStartColumnIndex);
                    if (CollectionUtils.isNotEmpty(headerWaringStyleList)) {
                        cellStyleList.addAll(headerWaringStyleList);
                    }
                    totalMap.putAll(listDataMap);
                }
            }
            beginIndex += headSummaryData.size();
            // 将处理好的数据添加到excelDataList
            excelDataList.add(headSummaryData);

            // 3.4、构建主题导出表数据
            String factoryName = factoryMap.getOrDefault(queryResult.getFactoryCode(), "");
            String productTypeName = productTypeMap.getOrDefault(queryResult.getProductTypeCode(), queryResult.getProductTypeCode());
            String titleFormat = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.exportTitle");
            String monthPlanVersionFormat = I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.monthPlanVersion");
            String productionVersionFormat = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.productionVersion");
            tableMap.put("factoryName", String.format(titleFormat, factoryName, queryResult.getYear(), queryResult.getMonth(), productTypeName));
            tableMap.put("monthPlanVersion", monthPlanVersionFormat + ":" + queryResult.getMonthPlanVersion());
            tableMap.put("productionVersion", productionVersionFormat + ":" + queryResult.getProductionVersion());
            List<Map<String, Object>> listData = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                FactoryMonthPlanMouldDayResultExportVo exportVo = list.get(i);
                this.setLastDayValue(exportVo, dayFieldNames); // 设置上月最后十天的值
                Map<String, Object> listDataMap = this.buildListDataMap(exportVo, storTypeMap, productCategoryMap,
                        productStatusMap, constructionStageMap, brandMap, structureTypeMap, yesNoMap, null, isFinal);
                // Excel行号从2开始（第1行是表头）
                int rowNum = beginIndex + i;
                if (!MonthPlanExportDataTypeEnum.RECORD.getCode().equals(exportVo.getDataType())) {
                    String color = "#DAEEF3";
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, listDataMap.size() - 1, color, true, true, ""));
                    if (MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())) {
                        tableMap.put("summary", this.buildSummary(list, exportVo)); // 如果是汇总行，需要构建汇总消息
                    }
                }
                //20260604+ 排产断开预警设置
                if (MonthPlanExportDataTypeEnum.RECORD.getCode().equals(exportVo.getDataType()) && YesOrNoEnum.YES.getValue().equals(exportVo.getIsDisturb())) {
                    String color = "#FFFF00";
                    int endColumnIndex = listDataMap.size() - BigDecimal.ONE.intValue() - BigDecimal.ONE.intValue();
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, endColumnIndex, color, true, false, ""));
                }

                listData.add(listDataMap);
            }
            // 将处理好的数据添加到excelDataList
            excelDataList.add(listData);
        }

        // 构建特殊材料排产结果
        this.buildSpecialMaterialInfo(queryResult, tableMap, excelDataList, isFinal);

        // 将单元格样式放入context
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }
        // 3.5、写到文件
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }
    
    /**
     * 获取导出模板行数
     * 
     * @param isFinal 是否定稿版本导出的模板
     * @return
     */
    @Override
    public int getExportTemplateColumnCount(boolean isFinal) {
        Map<String, Object> tableMap = new HashMap<>();
        // 补上十个特殊列
        tableMap.put("lastDay1", "");
        tableMap.put("lastDay2", "");
        tableMap.put("lastDay3", "");
        tableMap.put("lastDay4", "");
        tableMap.put("lastDay5", "");
        tableMap.put("lastDay6", "");
        tableMap.put("lastDay7", "");
        tableMap.put("lastDay8", "");
        tableMap.put("lastDay9", "");
        tableMap.put("lastDay10", "");
        this.loadExportI18nTableName(tableMap, isFinal);
        return tableMap.size();
    }

    /**
     * 构建特殊材料排产结果
     *
     * @param queryResult
     * @param tableMap
     * @param excelDataList
     */
    private void buildSpecialMaterialInfo(FactoryMonthPlanMouldDayResult queryResult, Map<String, Object> tableMap,
                                          List<List<Map<String, Object>>> excelDataList, boolean isFinal) {
        if (isFinal) {
            Map<String, Object> listDataMap = new HashMap<>();
            listDataMap.put("specialMaterialResult", "");
            List<Map<String, Object>> listData = new ArrayList<>();
            listData.add(listDataMap);
            excelDataList.add(listData);
            return;
        }
        // 1、加载特殊材料列表
        List<RawSpecialMaterialRecord> recordList = null;
        // 1.1、仅加载参数有配置的特殊材料清单
        List<String> paramCodeList = Collections.singletonList(MonthPlanEnums.SPECIAL_MATERIAL_CODE.getCode());
        List<FactoryParam> specialMaterialParam = factoryParamService.getFactoryParamByCondition(queryResult.getFactoryCode(),
                queryResult.getProductTypeCode(), paramCodeList);
        if (CollectionUtils.isNotEmpty(specialMaterialParam)) {
            String paramValue = specialMaterialParam.get(0).getParamValue();
            if (StringUtils.isEmpty(paramValue)) {
                paramValue = specialMaterialParam.get(0).getDefauleValue();
            }
            if (StringUtils.isNotEmpty(paramValue)) {
                // 1.2、加载出来的特殊材料过滤掉未在参数配置的特殊材料
                String specialMaterialCodes = paramValue;
                LambdaQueryWrapper<RawSpecialMaterialRecord> recordQueryWrapper = new LambdaQueryWrapper<>();
                recordQueryWrapper.eq(RawSpecialMaterialRecord::getFactoryCode, queryResult.getFactoryCode());
                recordQueryWrapper.eq(RawSpecialMaterialRecord::getMaterialType,
                        ApsConstant.BIZ_RAWMATERIAL_TYPE_SPECIAL);
                recordList = rawSpecialMaterialRecordMapper.selectList(recordQueryWrapper).stream()
                        .filter(r -> specialMaterialCodes.contains(r.getMaterialCode())).collect(Collectors.toList());
            }
        }

        // 2、加载特殊材料库存记录
        // 2.1、需要取排产年月上一个月份库存
        Date yearMonth = DateUtils.parseDate(queryResult.getYear() + "-" + queryResult.getMonth() + "-" + 1);
        Date lastYearMonth = DateUtils.addMonths(yearMonth, -1);
        Integer queryYear = DateUtils.getYear(lastYearMonth);
        Integer queryMonth = DateUtils.getMonth(lastYearMonth);
        LambdaQueryWrapper<RawSpecialMaterialStock> stockQueryWrapper = new LambdaQueryWrapper<>();
        stockQueryWrapper.eq(RawSpecialMaterialStock::getFactoryCode, queryResult.getFactoryCode());
        stockQueryWrapper.eq(RawSpecialMaterialStock::getYear, queryYear);
        stockQueryWrapper.eq(RawSpecialMaterialStock::getMonth, queryMonth);
        Map<String, List<RawSpecialMaterialStock>> stockMap = rawSpecialMaterialStockEntityMapper
                .selectList(stockQueryWrapper).stream()
                .collect(Collectors.groupingBy(RawSpecialMaterialStock::getMaterialCode));

        // 3、加载特殊材料排产结果
        LambdaQueryWrapper<SpecialMaterialResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpecialMaterialResult::getFactoryCode, queryResult.getFactoryCode());
        queryWrapper.eq(SpecialMaterialResult::getProductionVersion, queryResult.getProductionVersion());
        List<SpecialMaterialResult> specialMaterialResultList = specialMaterialResultEntityMapper.selectList(queryWrapper);
        Map<String, List<SpecialMaterialResult>> specialMaterialResultMap = specialMaterialResultList.stream().collect(Collectors.groupingBy(SpecialMaterialResult::getMaterialDesc));
        List<Map<String, Object>> listData = new ArrayList<>();
        int seq = 1;

        // 4、构建特殊材料使用列表
        for (RawSpecialMaterialRecord record : recordList) {
            String materiDesc = record.getMaterialDesc();
            List<SpecialMaterialResult> specialMaterialResult = specialMaterialResultMap.get(materiDesc);
            Map<String, Object> listDataMap = new HashMap<>(1);
            String format = "%s%s: %s";
            String itemFormat = I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.export.special.item");
            String seqStr = String.valueOf((char) (seq + 9311)); // 序号转换成带圈的样式
            StringBuilder builder = new StringBuilder();
            // 4.1、如果有特殊材料的排产记录，以排产记录为准
            if (CollectionUtils.isNotEmpty(specialMaterialResult)) {
                for (SpecialMaterialResult result : specialMaterialResult) {
                    Long totalQty = result.getTotalQty();
                    Long standardlenLong = result.getStandardLength();
                    Long oriStandardlenLong = result.getOriStandardLength();
                    Integer batchNum = BigDecimalUtils.div(totalQty, standardlenLong, 2).setScale(0, RoundingMode.UP).intValue();
                    String itemStr = String.format(itemFormat, batchNum, oriStandardlenLong);
                    if (builder.length() > 0) {
                        builder.append(" + ");
                    }
                    builder.append(itemStr);
                }
            } else {
                // 4.2、如果没有特殊材料的排产记录，则根据库存记录构建
                List<RawSpecialMaterialStock> stockList = stockMap.get(record.getMaterialCode());
                if (CollectionUtils.isNotEmpty(stockList)) {
                    for (RawSpecialMaterialStock stock : stockList) {
                        Integer oriStandardlenLong = stock.getStandardLength();
                        if (oriStandardlenLong == null) {
                            continue;
                        }
                        Integer batchNum = 0;
                        String itemStr = String.format(itemFormat, batchNum, oriStandardlenLong);
                        if (builder.length() > 0) {
                            builder.append(" + ");
                        }
                        builder.append(itemStr);
                    }
                }
            }
            String resultStr = String.format(format, seqStr, materiDesc, builder); // 拼接展示文本：①ABS391：3批1000米 + 2批2000米
            listDataMap.put("specialMaterialResult", resultStr);
            listData.add(listDataMap);
            seq++;
        }
        excelDataList.add(listData);
        tableMap.put("specialMaterialResult", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.export.special.title"));
    }

    /**
     * 加载表头国际化标签
     *
     * @param tableMap
     */
    private void loadExportI18nTableName(Map<String, Object> tableMap, boolean isFinal) {
        tableMap.put("locationType", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.locationType"));
        tableMap.put("materialCode", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.materialCode"));
        tableMap.put("mesMaterialCode", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.mesMaterialCode"));
        tableMap.put("materialDesc", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.materialDesc"));
        tableMap.put("structureName", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.structureName"));
        tableMap.put("structureType", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.structureType"));
        tableMap.put("productStatus", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.productStatus"));
        tableMap.put("embryoCode", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.embryoCode"));
        tableMap.put("mainMaterialDesc", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.mainMaterialDesc"));
        tableMap.put("constructionStage", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.schedulingType"));
        tableMap.put("brand", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.brand"));
        tableMap.put("specifications", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.specifications"));
        tableMap.put("mainPattern", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.mainPattern"));
        tableMap.put("pattern", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.pattern"));
        tableMap.put("proSize", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.proSize"));
        tableMap.put("singleTireWeight", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.singleTireWeight"));
        tableMap.put("productCategory", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.productCategory"));
        tableMap.put("mouldCavityQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.mouldCavityQty"));
        tableMap.put("typeBlockQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.typeBlockQty"));
        tableMap.put("averageSaleQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.averageSaleQty"));
        tableMap.put("inventorySalesRatio", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.inventorySalesRatio"));
        tableMap.put("dayVulcanizationQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.dayVulcanizationQty"));
        tableMap.put("restrictedNetQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.restrictedNetQty"));
        if (isFinal) {
            tableMap.put("prodReqPlan", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.realProdReqPlan"));
            tableMap.put("unRestrictedNetQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.realUnRestrictedNetQty"));
            tableMap.put("lastMonthRemainQty", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.lastMonthRemainQty"));
            tableMap.put("productSurplus", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.productSurplus"));
            tableMap.put("pendingQty", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.pendingQty"));
            tableMap.put("lastMonthOverdueQty", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.lastMonthOverdueQty"));
            tableMap.put("lastMonthValidFlag", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.lastMonthValidFlag"));
            tableMap.put("originalTotalQty", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.originalTotalQty"));
            tableMap.put("adjustQty1", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustQty1"));
            tableMap.put("adjustQty2", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustQty2"));
            tableMap.put("adjustQty3", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustQty3"));
            tableMap.put("adjustQty4", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustQty4"));
            tableMap.put("adjustProductQty1", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustProductQty1"));
            tableMap.put("adjustProductQty2", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustProductQty2"));
            tableMap.put("adjustProductQty3", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustProductQty3"));
            tableMap.put("adjustProductQty4", I18nUtil.getMessage("ui.data.column.FactoryMonthPlanFinalResult.adjustProductQty4"));
            
        } else {
            tableMap.put("prodReqPlan", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.prodReqPlan"));
            tableMap.put("unRestrictedNetQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.unRestrictedNetQty"));
            tableMap.put("heightQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.heightQty"));
            tableMap.put("midQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.midQty"));
            tableMap.put("cycleReserveQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.cycleReserveQty"));
            tableMap.put("conventionReserveQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.conventionReserveQty"));
            tableMap.put("totalQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.totalQty"));
            tableMap.put("heightProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.heightProductionQty"));
            tableMap.put("midProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.midProductionQty"));
            tableMap.put("cycleProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.cycleProductionQty"));
            tableMap.put("conventionProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.conventionProductionQty"));
            tableMap.put("postponeProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.postponeProductionQty"));
            tableMap.put("actualOrderUnproduced", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.actualOrderUnproduced"));
            tableMap.put("differenceQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.differenceQty"));
        }
        tableMap.put("beginDay", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.beginDay"));
        tableMap.put("endDay", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.endDay"));
        tableMap.put("day1", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day1"));
        tableMap.put("day2", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day2"));
        tableMap.put("day3", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day3"));
        tableMap.put("day4", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day4"));
        tableMap.put("day5", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day5"));
        tableMap.put("day6", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day6"));
        tableMap.put("day7", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day7"));
        tableMap.put("day8", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day8"));
        tableMap.put("day9", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day9"));
        tableMap.put("day10", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day10"));
        tableMap.put("day11", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day11"));
        tableMap.put("day12", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day12"));
        tableMap.put("day13", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day13"));
        tableMap.put("day14", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day14"));
        tableMap.put("day15", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day15"));
        tableMap.put("day16", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day16"));
        tableMap.put("day17", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day17"));
        tableMap.put("day18", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day18"));
        tableMap.put("day19", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day19"));
        tableMap.put("day20", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day20"));
        tableMap.put("day21", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day21"));
        tableMap.put("day22", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day22"));
        tableMap.put("day23", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day23"));
        tableMap.put("day24", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day24"));
        tableMap.put("day25", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day25"));
        tableMap.put("day26", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day26"));
        tableMap.put("day27", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day27"));
        tableMap.put("day28", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day28"));
        tableMap.put("day29", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day29"));
        tableMap.put("day30", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day30"));
        tableMap.put("day31", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.day31"));
        tableMap.put("totalAll", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.totalAll"));
    }

    /**
     * 设置上月最后十天的值
     *
     * @param exportVo      待处理数据
     * @param dayFieldNames 最后十天对应的字段名数组
     */
    private void setLastDayValue(FactoryMonthPlanMouldDayResultExportVo exportVo, String[] dayFieldNames) {
        if (dayFieldNames == null) {
            return;
        }
        for (int i = 0; i < dayFieldNames.length; i++) {
            if (dayFieldNames[i] != null) {
                Integer dayValue = (Integer) exportVo.getFieldValueByFieldName(dayFieldNames[i]);
                if (dayValue != null && dayValue > 0) {
                    exportVo.setFieldValueByFieldName(String.format(LAST_FIELD_NAME_FORMAT, (i + 1)), dayValue);
                }
            }
        }
    }

    /**
     * 构建统计信息
     *
     * @param list        导出列表
     * @param totalRecord 合计行记录
     * @return
     */
    private String buildSummary(List<FactoryMonthPlanMouldDayResultExportVo> list,
                                FactoryMonthPlanMouldDayResultExportVo totalRecord) {
        String summaryFormat = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.exportSummary");
        // 1、产量合计
        Integer totalQty = 0;
        // 2、开工天数
        int workDays = 0;
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
            Integer dayQty = Optional.ofNullable((Integer) totalRecord.getFieldValueByFieldName(dayFieldName)).orElse(0);
            if (dayQty > 0) {
                workDays++;
                totalQty += dayQty;
            }
        }
        // 3、平均日产
        BigDecimal averageQty = BigDecimal.ZERO;
        if (workDays > 0) {
            averageQty = BigDecimalUtils.div(totalQty, workDays).setScale(0, RoundingMode.DOWN);
        }
        // 4、平均单胎重
        BigDecimal totalWeight = list.stream().filter(r -> MonthPlanExportDataTypeEnum.RECORD.getCode().equals(r.getDataType()))
                .map(r -> BigDecimalUtils.multiply(r.getSingleTireWeight(), r.getTotalQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageWeight = BigDecimalUtils.div(totalWeight, totalQty);
        return String.format(summaryFormat, totalQty, workDays, averageQty, averageWeight);
    }

    /**
     * 构建导出行
     *
     * @param exportVo
     * @param storTypeMap
     * @param productCategoryMap
     * @param productStatusMap
     * @param constructionStageMap
     * @param brandMap
     * @param structureTypeMap
     * @param yesNoMap
     * @param suffix               后缀，用于复制合计行
     * @return
     */
    private Map<String, Object> buildListDataMap(FactoryMonthPlanMouldDayResultExportVo exportVo,
                                                 Map<String, String> storTypeMap,
                                                 Map<String, String> productCategoryMap,
                                                 Map<String, String> productStatusMap,
                                                 Map<String, String> constructionStageMap, Map<String, String> brandMap,
                                                 Map<String, String> structureTypeMap, Map<String, String> yesNoMap, String suffix,
                                                 boolean isFinal) {
        Map<String, Object> listDataMap = new HashMap<>(16);
        listDataMap.put(this.getRealFieldName("locationType", suffix), storTypeMap.getOrDefault(exportVo.getLocationType(), exportVo.getLocationType()));
        listDataMap.put(this.getRealFieldName("materialCode", suffix), exportVo.getMaterialCode());
        listDataMap.put(this.getRealFieldName("mesMaterialCode", suffix), exportVo.getMesMaterialCode());
        listDataMap.put(this.getRealFieldName("materialDesc", suffix), exportVo.getMaterialDesc());
        listDataMap.put(this.getRealFieldName("structureName", suffix), exportVo.getStructureName());
        listDataMap.put(this.getRealFieldName("productStatus", suffix), productStatusMap.getOrDefault(exportVo.getProductStatus(), exportVo.getProductStatus()));
        listDataMap.put(this.getRealFieldName("embryoCode", suffix), exportVo.getEmbryoCode());
        listDataMap.put(this.getRealFieldName("structureType", suffix), structureTypeMap.getOrDefault(exportVo.getStructureType(), exportVo.getStructureType()));
        listDataMap.put(this.getRealFieldName("mainMaterialDesc", suffix), exportVo.getMainMaterialDesc());
        listDataMap.put(this.getRealFieldName("constructionStage", suffix), constructionStageMap.getOrDefault(exportVo.getConstructionStage(), exportVo.getConstructionStage()));
        listDataMap.put(this.getRealFieldName("brand", suffix), brandMap.getOrDefault(exportVo.getBrand(), exportVo.getBrand()));
        listDataMap.put(this.getRealFieldName("specifications", suffix), exportVo.getSpecifications());
        listDataMap.put(this.getRealFieldName("mainPattern", suffix), exportVo.getMainPattern());
        listDataMap.put(this.getRealFieldName("pattern", suffix), exportVo.getPattern());
        listDataMap.put(this.getRealFieldName("proSize", suffix), exportVo.getProSize());
        listDataMap.put(this.getRealFieldName("singleTireWeight", suffix), exportVo.getSingleTireWeight());
        listDataMap.put(this.getRealFieldName("productCategory", suffix), productCategoryMap.getOrDefault(exportVo.getProductCategory(), exportVo.getProductCategory()));
        listDataMap.put(this.getRealFieldName("mouldCavityQty", suffix), exportVo.getMouldCavityQty());
        listDataMap.put(this.getRealFieldName("typeBlockQty", suffix), exportVo.getTypeBlockQty());
        listDataMap.put(this.getRealFieldName("averageSaleQty", suffix), exportVo.getAverageSaleQty());
        listDataMap.put(this.getRealFieldName("inventorySalesRatio", suffix), exportVo.getInventorySalesRatio());
        listDataMap.put(this.getRealFieldName("dayVulcanizationQty", suffix), exportVo.getDayVulcanizationQty());
        listDataMap.put(this.getRealFieldName("prodReqPlan", suffix), exportVo.getProdReqPlan());
        listDataMap.put(this.getRealFieldName("unRestrictedNetQty", suffix), exportVo.getUnRestrictedNetQty());
        listDataMap.put(this.getRealFieldName("restrictedNetQty", suffix), exportVo.getRestrictedNetQty());
        if (isFinal) {
            listDataMap.put(this.getRealFieldName("originalTotalQty", suffix), exportVo.getOriginalTotalQty());
            listDataMap.put(this.getRealFieldName("lastMonthRemainQty", suffix), exportVo.getLastMonthRemainQty());
            listDataMap.put(this.getRealFieldName("productSurplus", suffix), exportVo.getProductSurplus());
            listDataMap.put(this.getRealFieldName("pendingQty", suffix), exportVo.getPendingQty());
            listDataMap.put(this.getRealFieldName("adjustQty1", suffix), exportVo.getAdjustQty1());
            listDataMap.put(this.getRealFieldName("adjustQty2", suffix), exportVo.getAdjustQty2());
            listDataMap.put(this.getRealFieldName("adjustQty3", suffix), exportVo.getAdjustQty3());
            listDataMap.put(this.getRealFieldName("adjustQty4", suffix), exportVo.getAdjustQty4());
            listDataMap.put(this.getRealFieldName("adjustProductQty1", suffix), exportVo.getAdjustProductQty1());
            listDataMap.put(this.getRealFieldName("adjustProductQty2", suffix), exportVo.getAdjustProductQty2());
            listDataMap.put(this.getRealFieldName("adjustProductQty3", suffix), exportVo.getAdjustProductQty3());
            listDataMap.put(this.getRealFieldName("adjustProductQty4", suffix), exportVo.getAdjustProductQty4());
            listDataMap.put(this.getRealFieldName("lastMonthOverdueQty", suffix), exportVo.getLastMonthOverdueQty());
            listDataMap.put(this.getRealFieldName("lastMonthValidFlag", suffix), yesNoMap.getOrDefault(exportVo.getLastMonthValidFlag(), exportVo.getLastMonthValidFlag()));
        } else {
            listDataMap.put(this.getRealFieldName("heightQty", suffix), exportVo.getHeightQty());
            listDataMap.put(this.getRealFieldName("midQty", suffix), exportVo.getMidQty());
            listDataMap.put(this.getRealFieldName("cycleReserveQty", suffix), exportVo.getCycleReserveQty());
            listDataMap.put(this.getRealFieldName("conventionReserveQty", suffix), exportVo.getConventionReserveQty());
            listDataMap.put(this.getRealFieldName("totalQty", suffix), exportVo.getTotalQty());
            listDataMap.put(this.getRealFieldName("heightProductionQty", suffix), exportVo.getHeightProductionQty());
            listDataMap.put(this.getRealFieldName("midProductionQty", suffix), exportVo.getMidProductionQty());
            listDataMap.put(this.getRealFieldName("cycleProductionQty", suffix), exportVo.getCycleProductionQty());
            listDataMap.put(this.getRealFieldName("conventionProductionQty", suffix), exportVo.getConventionProductionQty());
            listDataMap.put(this.getRealFieldName("postponeProductionQty", suffix), exportVo.getPostponeProductionQty());
            listDataMap.put(this.getRealFieldName("actualOrderUnproduced", suffix), exportVo.getActualOrderUnproduced());
            listDataMap.put(this.getRealFieldName("differenceQty", suffix), exportVo.getDifferenceQty());
        }
        listDataMap.put(this.getRealFieldName("beginDay", suffix), exportVo.getBeginDay());
        listDataMap.put(this.getRealFieldName("endDay", suffix), exportVo.getEndDay());
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
        listDataMap.put(this.getRealFieldName("lastDay1", suffix), exportVo.getLastDay1());
        listDataMap.put(this.getRealFieldName("lastDay2", suffix), exportVo.getLastDay2());
        listDataMap.put(this.getRealFieldName("lastDay3", suffix), exportVo.getLastDay3());
        listDataMap.put(this.getRealFieldName("lastDay4", suffix), exportVo.getLastDay4());
        listDataMap.put(this.getRealFieldName("lastDay5", suffix), exportVo.getLastDay5());
        listDataMap.put(this.getRealFieldName("lastDay6", suffix), exportVo.getLastDay6());
        listDataMap.put(this.getRealFieldName("lastDay7", suffix), exportVo.getLastDay7());
        listDataMap.put(this.getRealFieldName("lastDay8", suffix), exportVo.getLastDay8());
        listDataMap.put(this.getRealFieldName("lastDay9", suffix), exportVo.getLastDay9());
        listDataMap.put(this.getRealFieldName("lastDay10", suffix), exportVo.getLastDay10());

        listDataMap.put(this.getRealFieldName("totalAll", suffix), exportVo.getTotalQty());
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
     * 构建小计行数据
     *
     * @param structureName          结构名称
     * @param recordList             明细记录
     * @param structureAllocationMap 结构上机记录，用于取结构上机、下机时间，可以为空
     * @param dataType               构建的数据类型
     * @return
     */
    private FactoryMonthPlanMouldDayResultExportVo buildSubtotalRecord(String structureName,
                                                                       List<FactoryMonthPlanMouldDayResultExportVo> recordList,
                                                                       Map<String, MpStructureAllocation> structureAllocationMap,
                                                                       MonthPlanExportDataTypeEnum dataType) {
        // 统计行
        FactoryMonthPlanMouldDayResultExportVo subtotal = new FactoryMonthPlanMouldDayResultExportVo();
        subtotal.setStructureName(structureName);
        subtotal.setProductCategory(I18nUtil.getMessage(dataType.getName()));
        subtotal.setDataType(dataType.getCode());
        // 结构上机时间
        if (structureAllocationMap != null) {
            MpStructureAllocation structureAllocation = structureAllocationMap.get(structureName);
            if (structureAllocation != null) {
                subtotal.setBeginDay(structureAllocation.getBeginDay());
                subtotal.setEndDay(structureAllocation.getEndDay());
            }
        }
        for (FactoryMonthPlanMouldDayResultExportVo result : recordList) {
            subtotal.setAverageSaleQty(safeAdd(subtotal.getAverageSaleQty(), result.getAverageSaleQty()));
            subtotal.setProdReqPlan(safeAdd(subtotal.getProdReqPlan(), result.getProdReqPlan()));
            subtotal.setHeightQty(safeAdd(subtotal.getHeightQty(), result.getHeightQty()));
            subtotal.setMidQty(safeAdd(subtotal.getMidQty(), result.getMidQty()));
            subtotal.setCycleReserveQty(safeAdd(subtotal.getCycleReserveQty(), result.getCycleReserveQty()));
            subtotal.setConventionReserveQty(safeAdd(subtotal.getConventionReserveQty(), result.getConventionReserveQty()));
            subtotal.setFactProdReqQty(safeAdd(subtotal.getFactProdReqQty(), result.getFactProdReqQty()));
            subtotal.setTotalQty(safeAdd(subtotal.getTotalQty(), result.getTotalQty()));
            subtotal.setHeightProductionQty(safeAdd(subtotal.getHeightProductionQty(), result.getHeightProductionQty()));
            subtotal.setMidProductionQty(safeAdd(subtotal.getMidProductionQty(), result.getMidProductionQty()));
            subtotal.setCycleProductionQty(safeAdd(subtotal.getCycleProductionQty(), result.getCycleProductionQty()));
            subtotal.setConventionProductionQty(safeAdd(subtotal.getConventionProductionQty(), result.getConventionProductionQty()));
            subtotal.setPostponeProductionQty(safeAdd(subtotal.getPostponeProductionQty(), result.getPostponeProductionQty()));
            subtotal.setActualOrderUnproduced(safeAdd(subtotal.getActualOrderUnproduced(), result.getActualOrderUnproduced()));
            subtotal.setDifferenceQty(safeAdd(subtotal.getDifferenceQty(), result.getDifferenceQty()));
            subtotal.setLastDay1(safeAddDefaultNull(subtotal.getLastDay1(), result.getLastDay1()));
            subtotal.setLastDay2(safeAddDefaultNull(subtotal.getLastDay2(), result.getLastDay2()));
            subtotal.setLastDay3(safeAddDefaultNull(subtotal.getLastDay3(), result.getLastDay3()));
            subtotal.setLastDay4(safeAddDefaultNull(subtotal.getLastDay4(), result.getLastDay4()));
            subtotal.setLastDay5(safeAddDefaultNull(subtotal.getLastDay5(), result.getLastDay5()));
            subtotal.setLastDay6(safeAddDefaultNull(subtotal.getLastDay6(), result.getLastDay6()));
            subtotal.setLastDay7(safeAddDefaultNull(subtotal.getLastDay7(), result.getLastDay7()));
            subtotal.setLastDay8(safeAddDefaultNull(subtotal.getLastDay8(), result.getLastDay8()));
            subtotal.setLastDay9(safeAddDefaultNull(subtotal.getLastDay9(), result.getLastDay9()));
            subtotal.setLastDay10(safeAddDefaultNull(subtotal.getLastDay10(), result.getLastDay10()));
            subtotal.setUnRestrictedNetQty(safeAdd(subtotal.getUnRestrictedNetQty(), result.getUnRestrictedNetQty()));
            subtotal.setRestrictedNetQty(safeAdd(subtotal.getRestrictedNetQty(), result.getRestrictedNetQty()));
            subtotal.setAdjustQty1(safeAdd(subtotal.getAdjustQty1(), result.getAdjustQty1()));
            subtotal.setAdjustQty2(safeAdd(subtotal.getAdjustQty2(), result.getAdjustQty2()));
            subtotal.setAdjustQty3(safeAdd(subtotal.getAdjustQty3(), result.getAdjustQty3()));
            subtotal.setAdjustQty4(safeAdd(subtotal.getAdjustQty4(), result.getAdjustQty4()));
            subtotal.setAdjustProductQty1(safeAdd(subtotal.getAdjustProductQty1(), result.getAdjustProductQty1()));
            subtotal.setAdjustProductQty2(safeAdd(subtotal.getAdjustProductQty2(), result.getAdjustProductQty2()));
            subtotal.setAdjustProductQty3(safeAdd(subtotal.getAdjustProductQty3(), result.getAdjustProductQty3()));
            subtotal.setAdjustProductQty4(safeAdd(subtotal.getAdjustProductQty4(), result.getAdjustProductQty4()));
            subtotal.setOriginalTotalQty(safeAdd(subtotal.getOriginalTotalQty(), result.getOriginalTotalQty()));
            subtotal.setProductSurplus(safeAdd(subtotal.getProductSurplus(), result.getProductSurplus()));
            subtotal.setLastMonthRemainQty(safeAdd(subtotal.getLastMonthRemainQty(), result.getLastMonthRemainQty()));
            subtotal.setPendingQty(safeAdd(subtotal.getPendingQty(), result.getPendingQty()));

            for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                Integer dayPlanQty = Optional.ofNullable((Integer) result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                if (dayPlanQty > 0) {
                    Integer subDayPlanQty = Optional.ofNullable((Integer) subtotal.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    subtotal.setFieldValueByFieldName(dayFieldName, subDayPlanQty + dayPlanQty);
                }
            }
        }
        return subtotal;
    }


    /**
     * 获取预警配置值
     *
     * @param queryResult
     * @return
     */
    private MpMonthPlanExportWarningConfigVo getWarningConfiguration(FactoryMonthPlanMouldDayResult queryResult) {
        MpMonthPlanExportWarningConfigVo empty = new MpMonthPlanExportWarningConfigVo();
        if (null == queryResult) {
            return empty;
        }
        String factoryCode = queryResult.getFactoryCode();
        String productType = queryResult.getProductTypeCode();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(productType)) {
            return empty;
        }
        List<String> paramCodeList = new ArrayList<>(64);
        //日排产相关
        paramCodeList.add(MonthPlanEnums.DAY_MIN_ALARM_LIMIT.getCode());
        paramCodeList.add(MonthPlanEnums.DAY_MAX_ALARM_LIMIT.getCode());
        List<FactoryParam> paramConfigurationList = factoryParamService.getFactoryParamByCondition(factoryCode, productType, paramCodeList);
        if (CollectionUtils.isEmpty(paramConfigurationList)) {
            return empty;
        }
        Map<String, FactoryParam> paramConfigurationMap = paramConfigurationList.stream().collect(Collectors.toMap(FactoryParam::getParamCode, Function.identity()));
        Map<String, Object> paramValueMap = new HashMap<>(paramConfigurationMap.size());
        //数据类型转换
        paramConfigurationMap.forEach((key, paramConfiguration) -> {
            if (null == paramConfiguration) {
                paramValueMap.put(key, null);
                return;
            }
            paramValueMap.put(key, FactoryParamUtils.getParamValue(paramConfiguration));
        });
        Object dayMinWarningQty = paramValueMap.get(MonthPlanEnums.DAY_MIN_ALARM_LIMIT.getCode());
        if (null != dayMinWarningQty) {
            empty.setDayMinTotalQty((Integer) dayMinWarningQty);
        }
        Object dayMaxWarningQty = paramValueMap.get(MonthPlanEnums.DAY_MAX_ALARM_LIMIT.getCode());
        if (null != dayMaxWarningQty) {
            empty.setDayMaxTotalQty((Integer) dayMaxWarningQty);
        }
        return empty;
    }

    /**
     * 构建日统计量预警数据样式集合
     *
     * @param warningConfiguration 预警配置
     * @param warningData          预警数据
     * @param rowIndex             行下标
     * @param startColumnIndex     起始单元格小标
     * @return
     */
    private List<CellStyle> buildWarningStyleByHeader(MpMonthPlanExportWarningConfigVo warningConfiguration, FactoryMonthPlanMouldDayResultExportVo warningData, int rowIndex, int startColumnIndex) {
        if (startColumnIndex < BigDecimal.ZERO.intValue()) {
            return Collections.emptyList();
        }
        if (null == warningConfiguration || (null == warningConfiguration.getDayMaxTotalQty() && null == warningConfiguration.getDayMinTotalQty())) {
            return Collections.emptyList();
        }
        if (null == warningData) {
            return Collections.emptyList();
        }
        String color = "#FF0000";
        List<CellStyle> warningStyleList = Lists.newArrayList();
        for (Integer index = ProductionConstant.MONTH_START_DAY; index <= ProductionConstant.MONTH_MAX_DAY; index++) {
            String fieldName = String.format(DAY_FIELD_NAME_FORMAT, index);
            boolean isWarning = isWarningFlag(fieldName, warningData, warningConfiguration);
            if (!isWarning) {
                continue;
            }
            int startCellNumber = startColumnIndex + index;
            CellStyle singleStyle = new CellStyle(rowIndex, rowIndex, startCellNumber, startCellNumber, color, false, true, "");
            warningStyleList.add(singleStyle);
        }
        if (CollectionUtils.isEmpty(warningStyleList)) {
            return Collections.emptyList();
        }
        return warningStyleList;
    }

    /**
     * 是否需要预警
     *
     * @param fieldName            字段名
     * @param warningData          数据行
     * @param warningConfiguration 预警配置
     * @return
     */
    private boolean isWarningFlag(String fieldName, FactoryMonthPlanMouldDayResultExportVo warningData, MpMonthPlanExportWarningConfigVo warningConfiguration) {
        if (null == warningData || StringUtils.isBlank(fieldName) || null == warningConfiguration) {
            return false;
        }
        Object value = warningData.getFieldValueByFieldName(fieldName);
        if (null == value) {
            return false;
        }
        Integer dayTotalQty = (Integer) value;
        Integer dayMinValue = warningConfiguration.getDayMinTotalQty();
        Integer dayMaxValue = warningConfiguration.getDayMaxTotalQty();
        if (null != dayMinValue && dayTotalQty < dayMinValue) {
            return true;
        }
        if (null != dayMaxValue && dayTotalQty > dayMaxValue) {
            return true;
        }
        return false;
    }

    /**
     * 获取结构+主花纹最大模具产能
     *
     * @param groupPlanMap       结构+主花纹-分组计划
     * @param maxProductionDays  最大可排产天数
     * @param mouldAllocationMap 模具分配比例
     * @return
     */
    private Map<String, Integer> getMoldMaxCapacity(Map<String, List<FactoryMonthPlanMouldDayResultExportVo>> groupPlanMap,
                                                    Integer maxProductionDays,
                                                    Map<String, Integer> mouldAllocationMap) {
        Map<String, Integer> groupMoldMaxCapacityMap = Maps.newHashMap();
        groupPlanMap.forEach((key, groupList) -> {
            //获取min日产
            Integer dayCapacityQty = BigDecimal.ZERO.intValue();
            Integer mouldNumber = BigDecimal.ZERO.intValue();
            if (CollectionUtils.isNotEmpty(groupList)) {
                dayCapacityQty = groupList.stream()
                        .map(FactoryMonthPlanMouldDayResultExportVo::getDayVulcanizationQty)
                        .filter(Objects::nonNull)
                        .min(Integer::compareTo).orElse(BigDecimal.ZERO.intValue());
                mouldNumber = groupList.stream().map(FactoryMonthPlanMouldDayResultExportVo::getMouldCavityQty)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo).orElse(BigDecimal.ZERO.intValue());
            }
            // 如果有配置模具分配，则同时要比较模具分配数的最小值
            if (mouldAllocationMap.containsKey(key)) {
                Integer mouldAllocation = mouldAllocationMap.getOrDefault(key, BigDecimal.ZERO.intValue());
                mouldNumber = Math.min(mouldNumber, mouldAllocation);
            }
            Integer lhMachineCount = mouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            // 最大产能 = 日产 * 模具数 * 本月生产天数
            Integer maxCapacity = dayCapacityQty * lhMachineCount * maxProductionDays;
            groupMoldMaxCapacityMap.put(key, maxCapacity);
        });
        return groupMoldMaxCapacityMap;
    }

    /**
     * 构建模具产能受限分配对象
     * 结构+主花纹下分组
     *
     * @param structureName      结构名
     * @param skuPlan            Sku计划
     * @param maxMoldCapacityQty 结构+主花纹下最大模具产能
     * @param sumNetQty          结构+主花纹下总净需求
     * @param sumHeightQty       结构+主花纹下总高优先级量
     * @return
     */
    private SkuMoldCapacityInfoVo buildSkuMoldCapacityInfo(String structureName, FactoryMonthPlanMouldDayResultExportVo skuPlan, Integer maxMoldCapacityQty, Integer sumNetQty, Integer sumHeightQty) {
        SkuMoldCapacityInfoVo skuCapacityInfo = new SkuMoldCapacityInfoVo();
        skuCapacityInfo.setGroupName(structureName);
        skuCapacityInfo.setMaterialCode(skuPlan.getMaterialCode());
        skuCapacityInfo.setMaterialDesc(skuPlan.getMaterialDesc());
        skuCapacityInfo.setMainPattern(skuPlan.getMainPattern());
        skuCapacityInfo.setDayVulcanizationQty(intValue(skuPlan.getDayVulcanizationQty()));
        skuCapacityInfo.setMaxMoldCapacity(intValue(maxMoldCapacityQty));
        skuCapacityInfo.setSumProductionQty(intValue(sumNetQty));
        skuCapacityInfo.setSumHeightProductionQty(intValue(sumHeightQty));
        skuCapacityInfo.setProductionQty(intValue(skuPlan.getProdReqPlan()));
        skuCapacityInfo.setHeightProductionQty(intValue(skuPlan.getHeightQty()));
        return skuCapacityInfo;
    }

}
