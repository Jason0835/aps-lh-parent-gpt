package com.zlt.aps.mp.factory.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
    public List<FactoryMonthPlanMouldDayResultExportVo> getExportList(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult) {
        // 1、加载构建导出列表的各项数据
        // 1.1、加载月计划模具排产明细
        List<FactoryMonthPlanMouldDayResultExportVo> recordList = factoryMonthPlanMouldDayResultEntityMapper
                .getExportList(factoryMonthPlanMouldDayResult);
        if (CollectionUtils.isEmpty(recordList)) {
            return recordList;
        }
        Map<String, List<FactoryMonthPlanMouldDayResultExportVo>> structureMap = recordList.stream()
                .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResultExportVo::getStructureName)); // 排产明细按结构分组
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
        LambdaQueryWrapper<MpStructureAllocation> structureQueryWrapper = new LambdaQueryWrapper<MpStructureAllocation>();
        structureQueryWrapper.eq(MpStructureAllocation::getProductionVersion,
                factoryMonthPlanMouldDayResult.getProductionVersion());
        structureQueryWrapper.eq(MpStructureAllocation::getFactoryCode,
                factoryMonthPlanMouldDayResult.getFactoryCode());
        Map<String, MpStructureAllocation> structureAllocationMap = mpStructureAllocationMapper
                .selectList(structureQueryWrapper).stream().collect(
                        Collectors.toMap(MpStructureAllocation::getStructureName, Function.identity(), (s1, s2) -> s1));

        // 2、构建导出总表
        List<FactoryMonthPlanMouldDayResultExportVo> totalRecordList = new LinkedList<>(); // 导出数据总表
        List<FactoryMonthPlanMouldDayResultExportVo> subtotalList = new ArrayList<>(); // 小计列表
        // 2.1、按结构遍历每一组排产明细记录，并构建该结构的明细数据 + 胎胚总类汇总 + 小计数据
        for (Entry<String, List<FactoryMonthPlanMouldDayResultExportVo>> entry : structureMap.entrySet()) {
            String structureName = entry.getKey();
            // 2.1.1、把明细记录添加到总表
            List<FactoryMonthPlanMouldDayResultExportVo> structureList = entry.getValue();
            totalRecordList.addAll(structureList);
            // 2.1.2、添加胎胚总类汇总行
            FactoryMonthPlanMouldDayResultExportVo statisticsRecord = new FactoryMonthPlanMouldDayResultExportVo();
            statisticsRecord.setStructureName(structureName);
            statisticsRecord.setDataType(MonthPlanExportDataTypeEnum.EMBRYO_TYPE_COUNT.getCode());
            MpMonthPlanStatistics statistics = statisticsMap.get(structureName);
            if (statistics != null) {
                for (int day = 1; day <= MAX_MONTH_DAY; day ++) {
                    String dayFieldName = String.format(DAY_FIELD_NAME_FORMAT, day);
                    String dayStatisticsStr = (String)statistics.getFieldValueByFieldName(dayFieldName);
                    if (StringUtils.isNotEmpty(dayStatisticsStr) && JSONValidator.from(dayStatisticsStr).validate()) {
                        MpDayProductionStatisticsDetailVo dayStatistics = JSONObject.parseObject(dayStatisticsStr, MpDayProductionStatisticsDetailVo.class);
                        statisticsRecord.setFieldValueByFieldName(dayFieldName, dayStatistics.getEmbryoCount());
                    }
                }
            }
            totalRecordList.add(statisticsRecord);
            // 2.1.3、构建小计行
            FactoryMonthPlanMouldDayResultExportVo subtotalRecord = this.buildSubtotalRecord(structureName,
                    structureList, structureAllocationMap, MonthPlanExportDataTypeEnum.SUBTOTAL);
            subtotalList.add(subtotalRecord); // 添加至小计表，最后需要将小计汇总成总计
            totalRecordList.add(subtotalRecord); // 添加至总表
        }
        
        // 3、构建总计行
        FactoryMonthPlanMouldDayResultExportVo totalRecord = this.buildSubtotalRecord("", subtotalList, null,
                MonthPlanExportDataTypeEnum.TOTAL); // 将小计列表汇总为总计
        totalRecordList.add(totalRecord); // 添加至总表

        return totalRecordList;
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
            subtotal.setAverageSaleQty(Optional.ofNullable(subtotal.getAverageSaleQty()).orElse(0) + result.getAverageSaleQty());
            subtotal.setProdReqPlan(Optional.ofNullable(subtotal.getProdReqPlan()).orElse(0) + result.getProdReqPlan());
            subtotal.setHeightQty(Optional.ofNullable(subtotal.getHeightQty()).orElse(0) + result.getHeightQty());
            subtotal.setMidQty(Optional.ofNullable(subtotal.getMidQty()).orElse(0) + result.getMidQty());
            subtotal.setCycleReserveQty(Optional.ofNullable(subtotal.getCycleReserveQty()).orElse(0) + result.getCycleReserveQty());
            subtotal.setFactProdReqQty(Optional.ofNullable(subtotal.getFactProdReqQty()).orElse(0) + result.getFactProdReqQty());
            subtotal.setTotalQty(Optional.ofNullable(subtotal.getTotalQty()).orElse(0) + result.getTotalQty());
            subtotal.setHeightProductionQty(Optional.ofNullable(subtotal.getHeightProductionQty()).orElse(0) + result.getHeightProductionQty());
            subtotal.setMidProductionQty(Optional.ofNullable(subtotal.getMidProductionQty()).orElse(0) + result.getMidProductionQty());
            subtotal.setCycleProductionQty(Optional.ofNullable(subtotal.getCycleProductionQty()).orElse(0) + result.getCycleProductionQty());
            subtotal.setConventionProductionQty(Optional.ofNullable(subtotal.getConventionProductionQty()).orElse(0) + result.getConventionProductionQty());
            subtotal.setPostponeProductionQty(Optional.ofNullable(subtotal.getPostponeProductionQty()).orElse(0) + result.getPostponeProductionQty());
            subtotal.setDifferenceQty(Optional.ofNullable(subtotal.getDifferenceQty()).orElse(0) + result.getDifferenceQty());
            subtotal.setDifferenceQty(Optional.ofNullable(subtotal.getDifferenceQty()).orElse(0) + result.getDifferenceQty());
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
}
