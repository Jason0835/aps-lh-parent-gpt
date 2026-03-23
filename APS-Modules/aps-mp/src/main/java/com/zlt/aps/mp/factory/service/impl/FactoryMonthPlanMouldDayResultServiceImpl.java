package com.zlt.aps.mp.factory.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.mapper.MpStructureAllocationMapper;
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
    private ISysDictDataCacheService sysDictDataCacheService;
    /**
     * 月份天数上限
     */
    private final static int MAX_MONTH_DAY = 31;
    /**
     * 日计划字段名称
     */
    private final static String DAY_FIELD_NAME_FORMAT = "day%s";

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
    public List<FactoryMonthPlanMouldDayResultExportVo> getExportList(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                                      boolean isAllMaterial) {
        // 1、加载构建导出列表的各项数据
        // 1.1、加载月计划模具排产明细
        List<FactoryMonthPlanMouldDayResultExportVo> recordList = factoryMonthPlanMouldDayResultEntityMapper
                .getExportList(factoryMonthPlanMouldDayResult);
        if (CollectionUtils.isEmpty(recordList)) {
            return recordList;
        }
        // 1.2、加载本次版本已生成的统计记录
        LambdaQueryWrapper<MpMonthPlanStatistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MpMonthPlanStatistics::getFactoryCode, factoryMonthPlanMouldDayResult.getFactoryCode());
        queryWrapper.eq(MpMonthPlanStatistics::getIsDelete, YesOrNoEnum.NO.getValue());
        queryWrapper.eq(MpMonthPlanStatistics::getProductionVersion,
                factoryMonthPlanMouldDayResult.getProductionVersion());
        Map<String, MpMonthPlanStatistics> statisticsMap = mpMonthPlanStatisticsEntityMapper.selectList(queryWrapper)
                .stream().collect(
                        Collectors.toMap(MpMonthPlanStatistics::getStructureName, Function.identity(), (s1, s2) -> s1));
        // 1.3、加载结构排产数据
        LambdaQueryWrapper<MpStructureAllocation> structureQueryWrapper = new LambdaQueryWrapper<>();
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, factoryMonthPlanMouldDayResult.getFactoryCode());
        structureQueryWrapper.eq(MpStructureAllocation::getProductionVersion, factoryMonthPlanMouldDayResult.getProductionVersion());
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode, factoryMonthPlanMouldDayResult.getFactoryCode());
        Map<String, MpStructureAllocation> structureAllocationMap = mpStructureAllocationMapper
                .selectList(structureQueryWrapper).stream().collect(
                        Collectors.toMap(MpStructureAllocation::getStructureName, Function.identity(), (s1, s2) -> s1));
        
        // 2、构建导出总表
        List<FactoryMonthPlanMouldDayResultExportVo> totalRecordList = new LinkedList<>(); // 导出数据总表
        List<FactoryMonthPlanMouldDayResultExportVo> subtotalList = new ArrayList<>(); // 小计列表
        // 2.1、按结构遍历每一组排产明细记录，并构建该结构的明细数据 + 胎胚总类汇总 + 小计数据
        String structureName = null; // 当前结构名称
        List<FactoryMonthPlanMouldDayResultExportVo> structureList = new ArrayList<>(); // 同结构排产记录列表
        Map<Integer, Integer> changeMouldMap = new HashMap<>();
        for (int day = 1; day <= MAX_MONTH_DAY; day ++) {  // 初始化汇总map
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
            for (FactoryMonthPlanMouldDayResultExportVo result: structureList) { // 部分数据额外处理
                if (result.getDayVulcanizationQty() != null) {
                    result.setDayVulcanizationQty(result.getDayVulcanizationQty() * ProductionConstant.DOUBLE_MOULD_PRODUCTION); // 日硫化量调整为双模
                    Integer differenceQty = Optional.ofNullable(result.getDifferenceQty()).orElse(0); // 未排量负数处理
                    result.setDifferenceQty(differenceQty >= 0? differenceQty: 0);
                }
            }
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
            MpMonthPlanStatistics statistics = statisticsMap.get(structureName);
            if (statistics != null) {
                for (int day = 1; day <= MAX_MONTH_DAY; day ++) {
                    String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                    String dayStatisticsStr = (String)statistics.getFieldValueByFieldName(dayFieldName);
                    if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                        MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
                        embryoCountStatisticsRecord.setFieldValueByFieldName(dayFieldName, dayStatistics.getEmbryoCount());
                        lhMachinesStatisticsRecord.setFieldValueByFieldName(dayFieldName, dayStatistics.getLhMachines());
                        Integer changeMould = Optional.ofNullable(dayStatistics.getChangeMould()).orElse(0);
                        if (changeMould > 0) {
                            changeMouldMap.put(day, changeMouldMap.getOrDefault(day, 0) + Optional.ofNullable(dayStatistics.getChangeMould()).orElse(0));
                        }
                    }
                }
            }
            totalRecordList.add(embryoCountStatisticsRecord);
            totalRecordList.add(lhMachinesStatisticsRecord);

            // 2.1.4、构建小计行
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
        for (Entry<Integer, Integer> entry: changeMouldMap.entrySet()) {
            Integer day = entry.getKey();
            Integer mould = entry.getValue();
            changeMouldStatisticsRecord.setFieldValueByFieldName(String.format(DAY_FIELD_NAME_FORMAT, day), mould);
        }
        totalRecordList.add(changeMouldStatisticsRecord);
        return totalRecordList;
    }

    /**
     * 导出数据
     *
     * @param list
     * @return
     */
    @Override
    public byte[] getFactoryMonthPlanMouldDayResultExportByte(List<FactoryMonthPlanMouldDayResultExportVo> list) {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/factoryMonthPlanMouldDayResultExportTemp.xlsx");

        // 加载字典数据
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
        
        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        List<CellStyle> cellStyleList = new ArrayList<>();
        int beginIndex = 2; // 起始行
        //需要增加的第二部分表头统计
        Map<String, Object> totalMap = new HashMap<>();
        if (PubUtil.isNotEmpty(list)) {
            List<Map<String, Object>> listData = new ArrayList<>();
            list.stream()
            .filter(r -> MonthPlanExportDataTypeEnum.CHANGE_MOULDS.getCode().equals(r.getDataType())
                    || MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(r.getDataType()))
            .forEach(exportVo -> {
                Map<String, Object> listDataMap = this.buildListDataMap(exportVo, storTypeMap, productCategoryMap,
                        productStatusMap, constructionStageMap, brandMap, structureTypeMap, "A");
                listData.add(listDataMap);
                if (MonthPlanExportDataTypeEnum.TOTAL.getCode().equals(exportVo.getDataType())) {
                    totalMap.putAll(listDataMap);
                }
            });
            beginIndex += listData.size();
            // 将处理好的数据添加到excelDataList
            excelDataList.add(listData);
        }

        // 查询数据
        if (PubUtil.isNotEmpty(list)) {
            FactoryMonthPlanMouldDayResultExportVo firstItem = list.get(0);
            String factoryName = factoryMap.getOrDefault(firstItem.getFactoryCode(), "");
            String productTypeName = productTypeMap.getOrDefault(firstItem.getProductTypeCode(), firstItem.getProductTypeCode());
            String titleFormat = I18nUtil.getMessage("ui.data.column.factoryMonthPlanMouldDayResult.exportTitle");
            tableMap.put("factoryName",String.format(titleFormat, factoryName, firstItem.getYear(), firstItem.getMonth(), productTypeName));
            tableMap.put("monthPlanVersion", firstItem.getMonthPlanVersion());
            tableMap.put("productionVersion", firstItem.getProductionVersion());
            List<Map<String, Object>> listData = new ArrayList<>();
            
            for (int i = 0; i < list.size(); i++) {
                FactoryMonthPlanMouldDayResultExportVo exportVo = list.get(i);
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
        if(PubUtil.isNotEmpty(cellStyleList)){
            tableMap.put("CELL_STYLE", cellStyleList);
        }
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream
                , 0, tableMap, excelDataList);
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
        for (int day = 1; day <= MAX_MONTH_DAY; day ++) {
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
            averageQty = BigDecimalUtils.div(totalQty, workDays);
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

        // 计算day1到day31的数量合计
        Integer totolAll = 0;
        totolAll += exportVo.getDay1() != null ? exportVo.getDay1() : 0;
        totolAll += exportVo.getDay2() != null ? exportVo.getDay2() : 0;
        totolAll += exportVo.getDay3() != null ? exportVo.getDay3() : 0;
        totolAll += exportVo.getDay4() != null ? exportVo.getDay4() : 0;
        totolAll += exportVo.getDay5() != null ? exportVo.getDay5() : 0;
        totolAll += exportVo.getDay6() != null ? exportVo.getDay6() : 0;
        totolAll += exportVo.getDay7() != null ? exportVo.getDay7() : 0;
        totolAll += exportVo.getDay8() != null ? exportVo.getDay8() : 0;
        totolAll += exportVo.getDay9() != null ? exportVo.getDay9() : 0;
        totolAll += exportVo.getDay10() != null ? exportVo.getDay10() : 0;
        totolAll += exportVo.getDay11() != null ? exportVo.getDay11() : 0;
        totolAll += exportVo.getDay12() != null ? exportVo.getDay12() : 0;
        totolAll += exportVo.getDay13() != null ? exportVo.getDay13() : 0;
        totolAll += exportVo.getDay14() != null ? exportVo.getDay14() : 0;
        totolAll += exportVo.getDay15() != null ? exportVo.getDay15() : 0;
        totolAll += exportVo.getDay16() != null ? exportVo.getDay16() : 0;
        totolAll += exportVo.getDay17() != null ? exportVo.getDay17() : 0;
        totolAll += exportVo.getDay18() != null ? exportVo.getDay18() : 0;
        totolAll += exportVo.getDay19() != null ? exportVo.getDay19() : 0;
        totolAll += exportVo.getDay20() != null ? exportVo.getDay20() : 0;
        totolAll += exportVo.getDay21() != null ? exportVo.getDay21() : 0;
        totolAll += exportVo.getDay22() != null ? exportVo.getDay22() : 0;
        totolAll += exportVo.getDay23() != null ? exportVo.getDay23() : 0;
        totolAll += exportVo.getDay24() != null ? exportVo.getDay24() : 0;
        totolAll += exportVo.getDay25() != null ? exportVo.getDay25() : 0;
        totolAll += exportVo.getDay26() != null ? exportVo.getDay26() : 0;
        totolAll += exportVo.getDay27() != null ? exportVo.getDay27() : 0;
        totolAll += exportVo.getDay28() != null ? exportVo.getDay28() : 0;
        totolAll += exportVo.getDay29() != null ? exportVo.getDay29() : 0;
        totolAll += exportVo.getDay30() != null ? exportVo.getDay30() : 0;
        totolAll += exportVo.getDay31() != null ? exportVo.getDay31() : 0;
        listDataMap.put(this.getRealFieldName("totalAll", suffix), totolAll);
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
            subtotal.setDifferenceQty(this.safeAdd(subtotal.getDifferenceQty(), result.getDifferenceQty()));
            for (int day = 1; day <= MAX_MONTH_DAY; day ++) {
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
