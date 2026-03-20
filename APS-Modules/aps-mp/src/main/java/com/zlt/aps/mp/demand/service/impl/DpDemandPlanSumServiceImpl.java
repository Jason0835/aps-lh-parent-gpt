package com.zlt.aps.mp.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.enums.ProductionPlanType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.mp.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanSumEntityMapper;
import com.zlt.aps.mp.demand.service.IDpDemandPlanSumService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpDemandPlanSumServiceImpl.java
 * 描    述：DpDemandPlanSumServiceImpl需求计划汇总业务层处理
 *@author yelq
 *@date 2026-01-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DpDemandPlanSumServiceImpl extends AbstractDocService<DpDemandPlanSum>  implements IDpDemandPlanSumService {
    private final DpDemandPlanEntityMapper demandPlanEntityMapper;
    private final DpDemandPlanSumEntityMapper dpDemandPlanSumEntityMapper;
    private final ISysDictDataCacheService sysDictDataCacheService;

    @Override
    protected String getDocTypeCode() {
        return "2026012216";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("2026012216");
        return sysDocType;
    }

    @Override
    public String checkUnique(DpDemandPlanSum docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.dpDemandPlanSum.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public void batchUpdateForDemand(DpDemandPlanSum billVO) {
        DpDemandPlanSum existObj =  dpDemandPlanSumEntityMapper.selectById(billVO.getId());
        updateDpDemandPlanSum(billVO);
        List<DpDemandPlan> list = this.findDemandPlan(existObj);
        if(CollectionUtils.isEmpty(list)) {
            return;
        }
        if(StringUtils.isNotBlank(billVO.getScmPriority())) {
            list.forEach(dpDemandPlan -> dpDemandPlan.setScmPriority(billVO.getScmPriority()));
            this.baseDao.updateBatch(list);
            return;
        }
        list.forEach(dpDemandPlan -> dpDemandPlan.setIsProduction(billVO.getIsProduction()));
        this.baseDao.updateBatch(list);
    }

    @Override
    public List<String> findMonthPlanVersion(DpDemandPlanSum queryCondition) {
        return dpDemandPlanSumEntityMapper.selectDistinctMonthPlanVersion(
            queryCondition.getFactoryCode(),
            queryCondition.getYear(),
            queryCondition.getMonth(),
            ProductionPlanType.NORMAL.getPlanType(),
            YesOrNoEnum.NO.getValue()
        );
    }

    @Override
    public DpDemandPlanSum getDpDemandPlanSumByParam(FactoryProductionPlanVo selectedRequireVersion) {
        LambdaQueryWrapper<DpDemandPlanSum> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DpDemandPlanSum::getFactoryCode, selectedRequireVersion.getFactoryCode());
        wrapper.eq(DpDemandPlanSum::getYear, selectedRequireVersion.getYear());
        wrapper.eq(DpDemandPlanSum::getMonth, selectedRequireVersion.getMonth());
        wrapper.eq(DpDemandPlanSum::getMonthPlanVersion, selectedRequireVersion.getMonthPlanVersion());
        wrapper.eq(DpDemandPlanSum::getIsDelete, YesOrNoEnum.NO.getValue());
        List<DpDemandPlanSum>  list = dpDemandPlanSumEntityMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public byte[] exportDemandPlanSum(DpDemandPlanSum queryVO, List<DpDemandPlanSum> list) {
        // 1. 加载模板流（使用try-with-resources确保关闭）
        byte[] result;
        try (InputStream inputStream = getClass().getClassLoader()
            .getResourceAsStream("excelModel/demandPlanSumExportTemplate.xlsx")) {
            if (inputStream == null) {
                throw new BusinessException("Excel模板文件不存在: excelModel/demandPlanSumExportTemplate.xlsx");
            }

            // 2. 预加载所有字典映射（一次获取，多次使用）
            Map<String, String> factoryMap = loadDictMap("biz_factory_name");
            Map<String, String> productTypeMap = loadDictMap("biz_product_type");
            Map<String, String> brandMap = loadDictMap("biz_brand_type");
            Map<String, String> locationTypeMap = loadDictMap("biz_stor_type");
            Map<String, String> productionTypeMap = loadDictMap("biz_schedule_type");
            Map<String, String> yesNoMap = loadDictMap("biz_yes_no");

            // 3. 构建表头信息（使用预定义列顺序，便于维护）
            Map<String, Object> tableMap = buildTableHeader(queryVO);

            // 4. 遍历数据，同时计算合计并构建行数据
            List<Map<String, Object>> rows = new ArrayList<>();
            List<CellStyle> cellStyleList = new ArrayList<>();
            // 自定义累加器，避免多个long变量
            SumAccumulator sums = new SumAccumulator();
            if (!CollectionUtils.isEmpty(list)) {
                // 根据模板结构：第1行标题，第2行表头，第3行合计，第4行开始数据
                // Excel行号从0开始，所以数据从第3行开始
                // 对应Excel第4行（数据开始行）
                int dataStartRowIndex = 3;
                String factoryName = factoryMap.getOrDefault(queryVO.getFactoryCode(), "");
                DpDemandPlanSum item;
                for (int i = 0; i < list.size(); i++) {
                    item = list.get(i);
                    // 累加各项数值
                    sums.add(item);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("factoryName", factoryName);
                    row.put("productTypeName", productTypeMap.getOrDefault(item.getProductTypeCode(), ""));
                    row.put("brand", brandMap.getOrDefault(item.getBrand(), ""));
                    row.put("locationType", locationTypeMap.getOrDefault(item.getLocationType(), ""));
                    row.put("materialCode", nullToEmpty(item.getMaterialCode()));
                    row.put("materialDesc", nullToEmpty(item.getMaterialDesc()));
                    row.put("structureName", nullToEmpty(item.getStructureName()));
                    row.put("mainPattern", nullToEmpty(item.getMainPattern()));
                    row.put("productionType", productionTypeMap.getOrDefault(item.getProductionType(), ""));
                    row.put("scmPriority", yesNoMap.getOrDefault(item.getScmPriority(), ""));
                    row.put("orderQty", item.getOrderQty());
                    row.put("stockQty", item.getStockQty());
                    row.put("sub2YearStockQty", item.getSub2YearStockQty());
                    row.put("sub1YearStockQty", item.getSub1YearStockQty());
                    row.put("currentYearStockQty", item.getCurrentYearStockQty());
                    row.put("plannedSurplus", item.getPlannedSurplus());
                    row.put("netQty", item.getNetQty());
                    row.put("postponeNetQty", item.getPostponeNetQty());
                    row.put("unPostponeNetQty", item.getUnPostponeNetQty());
                    row.put("heightQty", item.getHeightQty());
                    row.put("midQty", item.getMidQty());
                    row.put("postponeQty", item.getPostponeQty());
                    row.put("cycleReserveQty", item.getCycleReserveQty());
                    row.put("conventionReserveQty", item.getConventionReserveQty());
                    row.put("isProduction", yesNoMap.getOrDefault(item.getIsProduction(), ""));
                    row.put("minProductionQty", item.getMinProductionQty());
                    row.put("isReachMinProductionQty", yesNoMap.getOrDefault(item.getIsReachMinProductionQty(), ""));
                    row.put("updateDate", item.getUpdateDate());
                    // 计算行号（Excel行号从0开始）
                    // 数据行号 = 数据开始行号 + 数据索引
                    int rowNum = dataStartRowIndex + i;
                    // 添加样式（确保样式作用在实际的数据行上）
                    // 使用白色背景色（#FFFFFF）代替null，避免Color.decode抛出异常
                    // 等线字体，9号大小，黑色，不加粗，带边框
                    cellStyleList.add(new CellStyle(rowNum, rowNum, 0, 18, "#FFFFFF", true, false, "等线"));
                    // 构建行数据
                    rows.add(row);
                }
            }

            // 5. 将合计值放入tableMap
            sums.putToMap(tableMap);

            // 6. 组装最终数据结构
            List<List<Map<String, Object>>> excelDataList = Collections.singletonList(rows);
            // 将单元格样式放入context
            if (PubUtil.isNotEmpty(cellStyleList)) {
                tableMap.put("CELL_STYLE", cellStyleList);
            }
            // 7. 写入Excel并返回字节数组
            result = ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
        } catch (Exception e) {
            log.error("导出需求计划汇总异常", e);
            throw new BusinessException("导出失败", e);
        }
        return result;
    }

    /**
     * 加载字典并转换为Map<value, label>
     */
    private Map<String, String> loadDictMap(String dictType) {
        List<SysDictData> dictList = sysDictDataCacheService.getType(dictType);
        if (CollectionUtils.isEmpty(dictList)) {
            return Collections.emptyMap();
        }
        return dictList.stream()
            .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
    }

    /**
     * 构建表头信息
     */
    private Map<String, Object> buildTableHeader(DpDemandPlanSum queryVO) {
        // 保持顺序
        Map<String, Object> header = new LinkedHashMap<>();
        String titleFormat = I18nUtil.getMessage("ui.data.column.demandPlanSum.exportTitle");
        header.put("title", String.format(titleFormat, queryVO.getYear(), queryVO.getMonth()));
        header.put("monthPlanVersion", queryVO.getMonthPlanVersion());
        // 表头字段定义（保持与Excel模板的变量名一致）
        List<String> headerFields = Arrays.asList(
            "factoryCode", "productTypeCode", "locationType", "brand",
            "materialCode", "materialDesc", "structureName", "mainPattern",
            "productionType", "scmPriority", "orderQty", "stockQty",
            "sub2YearStockQty", "sub1YearStockQty", "currentYearStockQty",
            "plannedSurplus", "netQty", "postponeNetQty", "unPostponeNetQty",
            "heightQty", "midQty", "postponeQty", "cycleReserveQty",
            "conventionReserveQty", "isReachMinProductionQty", "minProductionQty",
            "isProduction", "updateTime"
        );
        for (String field : headerFields) {
            header.put(field, I18nUtil.getMessage("ui.data.column.demandPlanSum." + field));
        }
        return header;
    }

    /**
     * 空字符串处理
     */
    private String nullToEmpty(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /**
     * 内部累加器，避免多个零散long变量
     */
    private static class SumAccumulator {
        private long orderQty, stockQty, sub2YearStockQty, sub1YearStockQty, currentYearStockQty;
        private long plannedSurplus, netQty, postponeNetQty, unPostponeNetQty;
        private long heightQty, midQty, postponeQty, cycleReserveQty, conventionReserveQty;

        public void add(DpDemandPlanSum item) {
            orderQty += item.getOrderQty();
            stockQty += item.getStockQty();
            sub2YearStockQty += item.getSub2YearStockQty();
            sub1YearStockQty += item.getSub1YearStockQty();
            currentYearStockQty += item.getCurrentYearStockQty();
            plannedSurplus += item.getPlannedSurplus();
            netQty += item.getNetQty();
            postponeNetQty += item.getPostponeNetQty();
            unPostponeNetQty += item.getUnPostponeNetQty();
            heightQty += item.getHeightQty();
            midQty += item.getMidQty();
            postponeQty += item.getPostponeQty();
            cycleReserveQty += item.getCycleReserveQty();
            conventionReserveQty += item.getConventionReserveQty();
        }

        public void putToMap(Map<String, Object> map) {
            map.put("sumOrderQty", orderQty);
            map.put("sumStockQty", stockQty);
            map.put("sumSub2YearStockQty", sub2YearStockQty);
            map.put("sumSub1YearStockQty", sub1YearStockQty);
            map.put("sumCurrentYearStockQty", currentYearStockQty);
            map.put("sumPlannedSurplus", plannedSurplus);
            map.put("sumNetQty", netQty);
            map.put("sumPostponeNetQty", postponeNetQty);
            map.put("sumUnPostponeNetQty", unPostponeNetQty);
            map.put("sumHeightQty", heightQty);
            map.put("sumMidQty", midQty);
            map.put("sumPostponeQty", postponeQty);
            map.put("sumCycleReserveQty", cycleReserveQty);
            map.put("sumConventionReserveQty", conventionReserveQty);
        }
    }

    private void updateDpDemandPlanSum(DpDemandPlanSum billVO) {
        if(StringUtils.isNotBlank(billVO.getScmPriority())) {
            billVO.setScmPriority(billVO.getScmPriority());
            this.dpDemandPlanSumEntityMapper.updateById(billVO);
            return;
        }
        billVO.setIsProduction(billVO.getIsProduction());
        this.dpDemandPlanSumEntityMapper.updateById(billVO);

    }

    private List<DpDemandPlan> findDemandPlan(DpDemandPlanSum existObj) {
        LambdaQueryWrapper<DpDemandPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(DpDemandPlan::getFactoryCode, existObj.getFactoryCode());
        wrapper.eq(DpDemandPlan::getYear, existObj.getYear());
        wrapper.eq(DpDemandPlan::getMonth, existObj.getMonth());
        wrapper.eq(DpDemandPlan::getProductTypeCode, existObj.getProductTypeCode());
        wrapper.eq(DpDemandPlan::getMonthPlanVersion, existObj.getMonthPlanVersion());
        wrapper.eq(DpDemandPlan::getMaterialDesc, existObj.getMaterialDesc());
        wrapper.eq(DpDemandPlan::getIsDelete, YesOrNoEnum.NO.getValue());
        return demandPlanEntityMapper.selectList(wrapper);

    }
}
