package com.zlt.aps.mp.factory.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.DailyMouldAvailabilityResult;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.mapper.MpStructureAllocationMapper;
import com.zlt.aps.mp.engine.utils.DateUtils;
import com.zlt.aps.mp.enums.MonthPlanExportDataTypeEnum;
import com.zlt.aps.mp.factory.dto.FactoryMonthPlanMouldDayResultExportVo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanMouldDayResultEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanMouldDayResultService;
import com.zlt.sysdef.domain.SysDocType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanMouldDayResultServiceImpl.java
 * 描    述：FactoryMonthPlanMouldDayResultServiceImplS2-0604.排产结果-生产计划排产结果业务层处理
 *@author zlt
 *@date 2025-12-31
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
public class FactoryMonthPlanMouldDayResultServiceImpl extends AbstractDocService<FactoryMonthPlanMouldDayResult>  implements IFactoryMonthPlanMouldDayResultService {
    @Autowired
    private FactoryMonthPlanMouldDayResultEntityMapper factoryMonthPlanMouldDayResultEntityMapper;
    @Autowired
    private MpMonthPlanStatisticsEntityMapper mpMonthPlanStatisticsEntityMapper;
    @Autowired
    private MpStructureAllocationMapper mpStructureAllocationMapper;
    @Autowired
    private MoldCavityInsertMaxValueCalculatorImpl moldCavityInsertMaxValueCalculator;

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;
    /**
     * 日计划字段名称
     */
    private final static String DAY_FIELD_NAME_FORMAT = "day%s";
    /**
     * 月底计划字段名称
     */
    private final static String LAST_FIELD_NAME_FORMAT = "last%s";
    /**
     * 上月的首日，为月底前一天，由于只有2月存在月底28号，因此开始日期从27号开始
     */
    private final static Integer LAST_MONTH_FIRST_DAY = 27;

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
                                                                      boolean isAllMaterial) {
        // 1、加载构建导出列表的各项数据
        // 1.1、加载月计划表头信息
        this.loadExportTableData(params);
        // 1.2、加载月计划模具排产明细
        List<FactoryMonthPlanMouldDayResultExportVo> recordList = factoryMonthPlanMouldDayResultEntityMapper
                .getExportList(params, isAllMaterial);
        if (CollectionUtils.isEmpty(recordList)) {
            return recordList;
        }
        // 1.3、加载本次版本已生成的统计记录
        String productionVersion = params.getProductionVersion();
        Map<String, MpMonthPlanStatistics> statisticsMap = this.loadMpMonthPlanStatistics(params,
                productionVersion);
        // 1.4、加载上个月的统计记录
        String lastProductionVersion = recordList.stream().map(FactoryMonthPlanMouldDayResultExportVo::getLastProductionVersion).filter(Objects::nonNull).findAny().orElse(null);
        Map<String, MpMonthPlanStatistics> lastStatisticsMap = this.loadMpMonthPlanStatistics(params,
                lastProductionVersion);
        // 1.5、加载结构排产数据
        LambdaQueryWrapper<MpStructureAllocation> structureQueryWrapper = new LambdaQueryWrapper<>();
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, params.getFactoryCode());
        structureQueryWrapper.eq(MpStructureAllocation::getProductionVersion, params.getProductionVersion());
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, params.getFactoryCode());
        Map<String, MpStructureAllocation> structureAllocationMap = mpStructureAllocationMapper
                .selectList(structureQueryWrapper).stream().collect(
                        Collectors.toMap(MpStructureAllocation::getStructureName, Function.identity(), (s1, s2) -> s1));
        // 1.6、加载型腔数活块数
//        LocalDate monthStart = LocalDate.of(params.getYear(), params.getMonth(), ProductionConstant.MONTH_START_DAY);
//        DateUtils.getDate(monthStart.with(TemporalAdjusters.lastDayOfMonth()))
        Map<String, Integer> cavityResults = new HashMap<>(0); // 型腔可用量（按结构+主花纹分组）
        Map<String, Integer> insertResults = new HashMap<>(0); // 活块可用量（按物料描述分组）
        if (isAllMaterial) {
            List<DailyMouldAvailabilityResult> moldResult = moldCavityInsertMaxValueCalculator
                    .moldCavityInsertMaxValueCalculator(params.getYear(), params.getMonth(), params.getFactoryCode(),
                            null, null, true);
            if (CollectionUtils.isNotEmpty(moldResult)) {
                cavityResults = moldResult.get(0).getCavityResults();
                insertResults = moldResult.get(0).getInsertResults();
            }
        }

        // 2、构建导出总表
        List<FactoryMonthPlanMouldDayResultExportVo> totalRecordList = new LinkedList<>(); // 导出数据总表
        List<FactoryMonthPlanMouldDayResultExportVo> subtotalList = new ArrayList<>(); // 小计列表
        // 2.1、按结构遍历每一组排产明细记录，并构建该结构的明细数据 + 胎胚总类汇总 + 小计数据
        String structureName = null; // 当前结构名称
        List<FactoryMonthPlanMouldDayResultExportVo> structureList = new ArrayList<>(); // 同结构排产记录列表
        Map<Integer, Integer> changeMouldMap = new HashMap<>();
        Map<Integer, Integer> lastChangeMouldMap = new HashMap<>();
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day ++) {  // 初始化汇总map
            changeMouldMap.put(day, 0);
        }
        for (Integer i = 0, size = recordList.size(); i < size; i ++) {
            // 2.1.1、把同结构的排产记录添加到列表中，全部添加完后开始处理这一批数据
            FactoryMonthPlanMouldDayResultExportVo record = recordList.get(i);
            structureList.add(record); // 先添加到列表
            structureName = record.getStructureName(); // 更新结构
            if (i < size - 1) { // 还不是最后一行，则校验下一行是否同一个结构
                // 下一笔结构没有变化，且还不是最后一笔记录，继续遍历下一笔数据
                FactoryMonthPlanMouldDayResultExportVo nextRecord = recordList.get(i + 1);
                if (structureName.equals(nextRecord.getStructureName())) { // 结构没有变化，则添继续往下
                    continue;
                }
            }

            // 2.1.2、把明细记录添加到总表
            Map<String, Integer> maxMouldCavityQtyMap = new HashMap<>(); // 主花纹的最大型腔数
            Map<String, Integer> maxTypeBlockQtyMap = new HashMap<>(); // 物料的最大活块数
            for (FactoryMonthPlanMouldDayResultExportVo result: structureList) { // 部分数据额外处理
                // 2.1.2.1、日硫化量调整为双模
                if (result.getDayVulcanizationQty() != null) {
                    result.setDayVulcanizationQty(result.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION);
                }
                // 2.1.2.2、未排量负数处理
                Integer differenceQty = Optional.ofNullable(result.getDifferenceQty()).orElse(0); 
                result.setDifferenceQty(differenceQty >= 0? differenceQty: 0);
                // 2.1.2.3、实单未排产 = 高优先级 + 中优先级 - 实际排产，如果为负数则设为0
                Integer heightQty = Optional.ofNullable(result.getHeightQty()).orElse(0);
                Integer midQty = Optional.ofNullable(result.getMidQty()).orElse(0);
                Integer totalQty = Optional.ofNullable(result.getTotalQty()).orElse(0);
                Integer actualOrderUnproduced = heightQty + midQty - totalQty;
                result.setActualOrderUnproduced(actualOrderUnproduced > 0? actualOrderUnproduced: 0);
                // 2.1.2.4、补充型腔数
                if (result.getMouldCavityQty() == null) {
                    result.setMouldCavityQty(cavityResults.getOrDefault(result.getStructureName() + result.getMainPattern(), 0));
                }
                // 2.1.2.5、记录主花纹的最大型腔数
                Integer maxMouldCavityQty = maxMouldCavityQtyMap.getOrDefault(result.getMainPattern(), 0);
                maxMouldCavityQtyMap.put(result.getMainPattern(), Math.max(maxMouldCavityQty, result.getMouldCavityQty()));
                // 2.1.2.5 补充活块数
                if (result.getTypeBlockQty() == null) {
                    result.setTypeBlockQty(insertResults.getOrDefault(result.getMaterialDesc(), 0));
                }
                // 2.1.2.6、记录物料的最大活块数
                Integer maxTypeBlockQty = maxTypeBlockQtyMap.getOrDefault(result.getMaterialDesc(), 0);
                maxTypeBlockQtyMap.put(result.getMaterialDesc(), Math.max(maxTypeBlockQty, result.getTypeBlockQty()));
            }
            // 2.1.2.6、重新对结构内的数据排序：主花纹分组，按型腔数倒序、主花纹、最大活块数倒序，主花纹组内按型腔数倒序、活块数倒序排序
            structureList.stream().forEach(s -> { // 设置对应的最大型腔数和最大活块数
                s.setMaxMouldCavityQty(maxMouldCavityQtyMap.getOrDefault(s.getMainPattern(), 0));
                s.setMaxTypeBlockQty(maxTypeBlockQtyMap.getOrDefault(s.getMaterialDesc(), 0));
            });
            structureList.sort(Comparator.comparing(FactoryMonthPlanMouldDayResultExportVo::getMaxMouldCavityQty, Comparator.reverseOrder()) // 最大型腔数倒序
                    .thenComparing(Comparator.comparing(FactoryMonthPlanMouldDayResultExportVo::getMainPattern, Comparator.nullsLast(String::compareTo))) // 主花纹
                    .thenComparing(Comparator.comparing(FactoryMonthPlanMouldDayResultExportVo::getMaxTypeBlockQty, Comparator.reverseOrder())) // 最大活块数倒序
                    .thenComparing(Comparator.comparing(FactoryMonthPlanMouldDayResultExportVo::getMaterialDesc, Comparator.nullsLast(String::compareTo))) // 物料描述
                    );// 排序
            totalRecordList.addAll(structureList);

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
                    changeMouldMap);
            // 2.1.5、上个月统计信息汇总
            MpMonthPlanStatistics lastStatistics = lastStatisticsMap.get(structureName);
            this.buildStatisticsRecord(embryoCountStatisticsRecord, lhMachinesStatisticsRecord, LAST_MONTH_FIRST_DAY, lastStatistics,
                    lastChangeMouldMap);
            
            totalRecordList.add(embryoCountStatisticsRecord);
            totalRecordList.add(lhMachinesStatisticsRecord);

            // 2.1.6、构建小计行
            FactoryMonthPlanMouldDayResultExportVo subtotalRecord = this.buildSubtotalRecord(structureName,
                    structureList, structureAllocationMap, MonthPlanExportDataTypeEnum.SUBTOTAL);
            subtotalList.add(subtotalRecord); // 添加至小计表，最后需要将小计汇总成总计
            totalRecordList.add(subtotalRecord); // 添加至总表

            // 2.1.5、结束本结束数据构建，清空数据
            structureList.clear();
        }

        // 3、构建总计行
        FactoryMonthPlanMouldDayResultExportVo totalRecord = this.buildSubtotalRecord("", subtotalList, null,
                MonthPlanExportDataTypeEnum.TOTAL); // 将小计列表汇总为总计
        totalRecordList.add(totalRecord); // 添加至总表
        
        // 4、构建换模次数列
        FactoryMonthPlanMouldDayResultExportVo changeMouldStatisticsRecord = new FactoryMonthPlanMouldDayResultExportVo();
        changeMouldStatisticsRecord.setDataType(MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getCode());
        changeMouldStatisticsRecord.setProductCategory(I18nUtil.getMessage(MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getName()));
        // 4.1、本月换模统计添加到统计行中
        for (Entry<Integer, Integer> entry: changeMouldMap.entrySet()) {
            Integer day = entry.getKey();
            Integer mould = entry.getValue();
            changeMouldStatisticsRecord.setFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, day), mould);
        }
        // 4.2、上月换模统计添加到统计行中
        for (Entry<Integer, Integer> entry: lastChangeMouldMap.entrySet()) {
            Integer day = entry.getKey();
            Integer mould = entry.getValue();
            changeMouldStatisticsRecord.setFieldValueByFieldName(String.format(LAST_FIELD_NAME_FORMAT, day), mould);
        }
        totalRecordList.add(changeMouldStatisticsRecord);
        return totalRecordList;
    }

    /**
     * 加载月计划表头信息
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
                                       Map<Integer, Integer> changeMouldMap) {
        if (statistics == null) {
            return;
        }
        for (int day = startDay; day <= FactoryConstant.MONTH_MAX_DAY; day++) {
            String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
            String dayStatisticsStr = (String) statistics.getFieldValueByFieldName(dayFieldName);
            if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr,
                        MpDayProductionStatisticsDetailVo.class);
                embryoCountStatisticsRecord.setFieldValueByFieldName(dayFieldName, dayStatistics.getEmbryoCount());
                lhMachinesStatisticsRecord.setFieldValueByFieldName(dayFieldName, dayStatistics.getLhMachines());
                Integer changeMould = Optional.ofNullable(dayStatistics.getChangeMould()).orElse(0);
                if (changeMould > 0) {
                    changeMouldMap.put(day, changeMouldMap.getOrDefault(day, 0) + Optional.ofNullable(dayStatistics.getChangeMould()).orElse(0));
                }
            }
        }
    }

    /**
     * 加载月计划排产统计记录
     * @param factoryMonthPlanMouldDayResult    
     * @param productionVersion
     * @return
     */
    private Map<String, MpMonthPlanStatistics> loadMpMonthPlanStatistics(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                                         String productionVersion) {
        if (StringUtils.isEmpty(productionVersion)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, factoryMonthPlanMouldDayResult.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion, productionVersion);
        Map<String, MpMonthPlanStatistics> statisticsMap = mpMonthPlanStatisticsEntityMapper.selectList(queryWrapper)
                .stream().collect(
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
                                                              List<FactoryMonthPlanMouldDayResultExportVo> list) {
        // 1、获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/factoryMonthPlanMouldDayResultExportTemp.xlsx");

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
        
        // 3、构建表格数据
        Map<String, Object> tableMap = new HashMap<>(16); // 总表
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>(); // 列表数据
        List<CellStyle> cellStyleList = new ArrayList<>(); // 样式列表
        int beginIndex = 2; // 起始行
        Map<String, Object> totalMap = new HashMap<>();
        if (PubUtil.isNotEmpty(list)) {
            // 3.1、根据上个月所在年份月计算最后两天日期
            String dayFieldName1 = null;
            String dayFieldName2 = null;
            if (queryResult.getYear() != null && queryResult.getMonth() != null) {
                Integer lastDay1 = null; // 月末第1天
                Integer lastDay2 = null; // 月末第2天
                Calendar calendar = Calendar.getInstance();
                calendar.set(queryResult.getYear(), queryResult.getMonth() - 1, 1); // 通过日历获取上本月一号的日历
                calendar.add(Calendar.MONTH, -1); // 切换到上个月
                Integer lastMonth = calendar.get(Calendar.MONTH) + 1;
                lastDay1 = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // 最后一天
                lastDay2 = lastDay1 - 1; // 倒数第二天
                dayFieldName1 = String.format(LAST_FIELD_NAME_FORMAT, lastDay1);
                dayFieldName2 = String.format(LAST_FIELD_NAME_FORMAT, lastDay2);
                String lastDayFormat = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.lastMonth");
                tableMap.put("lastDay1", String.format(lastDayFormat, lastMonth, lastDay1));
                tableMap.put("lastDay2", String.format(lastDayFormat, lastMonth, lastDay2));
            }
            
            // 3.2、加载表头国际化标签
            this.loadExportI18nTableName(tableMap);
            
            // 3.3、需要增加的第二部分表头统计
            List<Map<String, Object>> headSummaryData = new ArrayList<>();
            List<FactoryMonthPlanMouldDayResultExportVo> headList = list.stream()
                    .filter(r -> MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getCode().equals(r.getDataType()) // 换模统计
                            || MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(r.getDataType())) // 计划量统计
                    .collect(Collectors.toList());
            for (FactoryMonthPlanMouldDayResultExportVo exportVo: headList) {
                this.setLastDayValue(exportVo, dayFieldName1, dayFieldName2); // 设置上月最后两天的值
                Map<String, Object> listDataMap = this.buildListDataMap(exportVo, storTypeMap, productCategoryMap,
                        productStatusMap, constructionStageMap, brandMap, structureTypeMap, "A");
                headSummaryData.add(listDataMap);
                if (MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())) {
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
            tableMap.put("factoryName",String.format(titleFormat, factoryName, queryResult.getYear(), queryResult.getMonth(), productTypeName));
            tableMap.put("monthPlanVersion", monthPlanVersionFormat + ":" + queryResult.getMonthPlanVersion());
            tableMap.put("productionVersion", productionVersionFormat + ":" + queryResult.getProductionVersion());
            List<Map<String, Object>> listData = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                FactoryMonthPlanMouldDayResultExportVo exportVo = list.get(i);
                this.setLastDayValue(exportVo, dayFieldName1, dayFieldName2); // 设置上月最后两天的值
                Map<String, Object> listDataMap = this.buildListDataMap(exportVo, storTypeMap, productCategoryMap,
                        productStatusMap, constructionStageMap, brandMap, structureTypeMap, null);
                // Excel行号从2开始（第1行是表头）
                int rowNum = beginIndex + i;
                if(!MonthPlanExportDataTypeEnum.RECORD.getCode().equals(exportVo.getDataType())){
                    String color = "#DAEEF3";
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, listDataMap.size() - 1, color, true, true, ""));
                    if (MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())) {
                        tableMap.put("summary", this.buildSummary(list, exportVo)); // 如果是汇总行，需要构建汇总消息
                    }
                }
                listData.add(listDataMap);
            }
            // 将处理好的数据添加到excelDataList
            excelDataList.add(listData);
        }

        // 将单元格样式放入context
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }
        // 3.5、写到文件
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }

    /**
     * 加载表头国际化标签 
     * @param tableMap
     */
    private void loadExportI18nTableName(Map<String, Object> tableMap) {
        tableMap.put("locationType", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.locationType"));
        tableMap.put("materialCode", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.materialCode"));
        tableMap.put("mesMaterialCode", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.mesMaterialCode"));
        tableMap.put("materialDesc", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.materialDesc"));
        tableMap.put("structureName", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.structureName"));
        tableMap.put("productStatus", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.productStatus"));
        tableMap.put("embryoCode", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.embryoCode"));
        tableMap.put("structureType", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.structureType"));
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
        tableMap.put("prodReqPlan", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.prodReqPlan"));
        tableMap.put("heightQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.heightQty"));
        tableMap.put("midQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.midQty"));
        tableMap.put("cycleReserveQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.cycleReserveQty"));
        tableMap.put("totalQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.totalQty"));
        tableMap.put("heightProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.heightProductionQty"));
        tableMap.put("midProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.midProductionQty"));
        tableMap.put("cycleProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.cycleProductionQty"));
        tableMap.put("conventionProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.conventionProductionQty"));
        tableMap.put("postponeProductionQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.postponeProductionQty"));
        tableMap.put("actualOrderUnproduced", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.actualOrderUnproduced"));
        tableMap.put("differenceQty", I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.differenceQty"));
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
     * 设置上月最后两天的值
     * 
     * @param exportVo      待处理数据
     * @param dayFieldName1 最后一天的取值来源
     * @param dayFieldName2 倒数第二天的取值来源
     */
    private void setLastDayValue(FactoryMonthPlanMouldDayResultExportVo exportVo, String dayFieldName1,
                                 String dayFieldName2) {
        if (dayFieldName1 != null) {
            Integer lastDay1Value = (Integer) exportVo.getFieldValueByFieldName(dayFieldName1);
            if (lastDay1Value != null && lastDay1Value > 0) {
                exportVo.setLastDay1(lastDay1Value);
            }
        }
        if (dayFieldName2 != null) {
            Integer lastDay2Value = (Integer) exportVo.getFieldValueByFieldName(dayFieldName2);
            if (lastDay2Value != null && lastDay2Value > 0) {
                exportVo.setLastDay2(lastDay2Value);
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
        for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day ++) {
            String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
            Integer dayQty = Optional.ofNullable((Integer) totalRecord.getFieldValueByFieldName(dayFieldName)).orElse(0);
            if (dayQty > 0) {
                workDays ++;
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
     * @param exportVo
     * @param storTypeMap
     * @param productCategoryMap
     * @param productStatusMap
     * @param constructionStageMap
     * @param brandMap
     * @param structureTypeMap
     * @param suffix    后缀，用于复制合计行
     * @return
     */
    private Map<String, Object> buildListDataMap(FactoryMonthPlanMouldDayResultExportVo exportVo,
                                                 Map<String, String> storTypeMap,
                                                 Map<String, String> productCategoryMap,
                                                 Map<String, String> productStatusMap,
                                                 Map<String, String> constructionStageMap, Map<String, String> brandMap,
                                                 Map<String, String> structureTypeMap, String suffix) {
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
        listDataMap.put(this.getRealFieldName("heightQty", suffix), exportVo.getHeightQty());
        listDataMap.put(this.getRealFieldName("midQty", suffix), exportVo.getMidQty());
        listDataMap.put(this.getRealFieldName("cycleReserveQty", suffix), exportVo.getCycleReserveQty());
        listDataMap.put(this.getRealFieldName("totalQty", suffix), exportVo.getTotalQty());
        listDataMap.put(this.getRealFieldName("heightProductionQty", suffix), exportVo.getHeightProductionQty());
        listDataMap.put(this.getRealFieldName("midProductionQty", suffix), exportVo.getMidProductionQty());
        listDataMap.put(this.getRealFieldName("cycleProductionQty", suffix), exportVo.getCycleProductionQty());
        listDataMap.put(this.getRealFieldName("conventionProductionQty", suffix), exportVo.getConventionProductionQty());
        listDataMap.put(this.getRealFieldName("postponeProductionQty", suffix), exportVo.getPostponeProductionQty());
        listDataMap.put(this.getRealFieldName("actualOrderUnproduced", suffix), exportVo.getActualOrderUnproduced());
        listDataMap.put(this.getRealFieldName("differenceQty", suffix), exportVo.getDifferenceQty());
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
        
        listDataMap.put(this.getRealFieldName("totalAll", suffix), exportVo.getTotalQty());
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
        for (FactoryMonthPlanMouldDayResultExportVo result: recordList) {
            subtotal.setAverageSaleQty(this.safeAdd(subtotal.getAverageSaleQty(), result.getAverageSaleQty()));
            subtotal.setProdReqPlan(this.safeAdd(subtotal.getProdReqPlan(), result.getProdReqPlan()));
            subtotal.setHeightQty(this.safeAdd(subtotal.getHeightQty(), result.getHeightQty()));
            subtotal.setMidQty(this.safeAdd(subtotal.getMidQty(), result.getMidQty()));
            subtotal.setCycleReserveQty(this.safeAdd(subtotal.getCycleReserveQty(), result.getCycleReserveQty()));
            subtotal.setFactProdReqQty(this.safeAdd(subtotal.getFactProdReqQty(), result.getFactProdReqQty()));
            subtotal.setTotalQty(this.safeAdd(subtotal.getTotalQty(), result.getTotalQty()));
            subtotal.setHeightProductionQty(this.safeAdd(subtotal.getHeightProductionQty(), result.getHeightProductionQty()));
            subtotal.setMidProductionQty(this.safeAdd(subtotal.getMidProductionQty(), result.getMidProductionQty()));
            subtotal.setCycleProductionQty(this.safeAdd(subtotal.getCycleProductionQty(), result.getCycleProductionQty()));
            subtotal.setConventionProductionQty(this.safeAdd(subtotal.getConventionProductionQty(), result.getConventionProductionQty()));
            subtotal.setPostponeProductionQty(this.safeAdd(subtotal.getPostponeProductionQty(), result.getPostponeProductionQty()));
            subtotal.setActualOrderUnproduced(this.safeAdd(subtotal.getActualOrderUnproduced(), result.getActualOrderUnproduced()));
            subtotal.setDifferenceQty(this.safeAdd(subtotal.getDifferenceQty(), result.getDifferenceQty()));
            subtotal.setLast27(this.safeAdd(subtotal.getLast27(), result.getLast27()));
            subtotal.setLast28(this.safeAdd(subtotal.getLast28(), result.getLast28()));
            subtotal.setLast29(this.safeAdd(subtotal.getLast29(), result.getLast29()));
            subtotal.setLast30(this.safeAdd(subtotal.getLast30(), result.getLast30()));
            subtotal.setLast31(this.safeAdd(subtotal.getLast31(), result.getLast31()));
            
            for (int day = FactoryConstant.MONTH_START_DAY; day <= FactoryConstant.MONTH_MAX_DAY; day ++) {
                String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                Integer dayPlanQty = Optional.ofNullable((Integer)result.getFieldValueByFieldName(dayFieldName)).orElse(0);
                if (dayPlanQty > 0) {
                    Integer subDayPlanQty = Optional.ofNullable((Integer)subtotal.getFieldValueByFieldName(dayFieldName)).orElse(0);
                    subtotal.setFieldValueByFieldName(dayFieldName, subDayPlanQty + dayPlanQty);
                }
            }
        }
        return subtotal;
    }

    private Integer safeAdd(Integer value1, Integer value2) {
        return Optional.ofNullable(value1).orElse(0) + Optional.ofNullable(value2).orElse(0);
    }
}
