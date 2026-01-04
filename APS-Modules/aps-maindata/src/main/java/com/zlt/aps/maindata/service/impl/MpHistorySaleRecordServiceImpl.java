package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Sets;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MpHistorySaleRecordEntityMapper;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleRecordServiceImpl.java
 * 描    述：MpHistorySaleRecordServiceImpl历史销售记录业务层处理
 *@author zlt
 *@date 2025-12-11
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
@RequiredArgsConstructor
public class MpHistorySaleRecordServiceImpl extends AbstractDocService<MpHistorySaleRecord> implements IMpHistorySaleRecordService {
    private static final int MIN_MONTH_THRESHOLD = 8;

    private final MpHistorySaleRecordEntityMapper mpHistorySaleRecordEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "MP1215";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP1215");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpHistorySaleRecord docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpHistorySaleRecord.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("year", "month", "areaCode", "materialCode"));
    }

    @Override
    public Set<String> findSkuInLastTwelveMonth() {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth startYearMonth = currentYearMonth.minusMonths(12);
        String yearMonth = String.format("%s%02d", startYearMonth.getYear(), startYearMonth.getMonthValue());
        LambdaQueryWrapper<MpHistorySaleRecord> queryWrapper = Wrappers.lambdaQuery(MpHistorySaleRecord.class)
            .ge(MpHistorySaleRecord::getYearMonth, Integer.valueOf(yearMonth))
            .ge(MpHistorySaleRecord::getSaleQty, BigDecimal.ZERO.intValue())
            .eq(MpHistorySaleRecord::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<MpHistorySaleRecord>  historySaleRecords = this.mpHistorySaleRecordEntityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(historySaleRecords)){
            return Sets.newHashSet();
        }
        return getMaterialCodesWithBitSet(historySaleRecords);
    }

    @Override
    public Map<String, Integer> calculateMonthSaleQty(int months) {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        YearMonth lastYearMonth =  currentYearMonth.minusMonths(months);
        String yearMonth = String.format("%s%02d", lastYearMonth.getYear(), lastYearMonth.getMonthValue());
        LambdaQueryWrapper<MpHistorySaleRecord> queryWrapper = Wrappers.lambdaQuery(MpHistorySaleRecord.class)
            .ge(MpHistorySaleRecord::getYearMonth, Integer.valueOf(yearMonth))
            .ge(MpHistorySaleRecord::getSaleQty, BigDecimal.ZERO.intValue())
            .eq(MpHistorySaleRecord::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<MpHistorySaleRecord>  historySaleRecords = this.mpHistorySaleRecordEntityMapper.selectList(queryWrapper);
        if(CollectionUtils.isEmpty(historySaleRecords)){
            return Collections.emptyMap();
        }
        return historySaleRecords.stream()
            .filter(Objects::nonNull)
            .filter(record -> StringUtils.isNotBlank(record.getMaterialCode())
                && record.getSaleQty() != null)
            .collect(Collectors.groupingBy(
                MpHistorySaleRecord::getMaterialCode,
                Collectors.summingLong(MpHistorySaleRecord::getSaleQty)
            ))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> BigDecimal.valueOf(entry.getValue())
                    .divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP).intValue()
            ));
    }

    private Set<String> getMaterialCodesWithBitSet(
        List<MpHistorySaleRecord> historySaleRecords) {

        if (CollectionUtils.isEmpty(historySaleRecords)) {
            return Collections.emptySet();
        }

        // 第一步：收集所有可能的年月
        Set<Integer> allYearMonths = historySaleRecords.stream()
            .map(MpHistorySaleRecord::getYearMonth)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 创建年月到索引的映射
        List<Integer> sortedMonths = allYearMonths.stream()
            .sorted()
            .collect(Collectors.toList());

        Map<Integer, Integer> monthToIndex = new HashMap<>();
        for (int i = 0; i < sortedMonths.size(); i++) {
            monthToIndex.put(sortedMonths.get(i), i);
        }

        // 第二步：使用BitSet跟踪每个物料的月份
        Map<String, BitSet> materialBitSets = new HashMap<>();
        for (MpHistorySaleRecord record : historySaleRecords) {
            if (record == null || record.getMaterialCode() == null
                || record.getYearMonth() == null) {
                continue;
            }
            String materialCode = record.getMaterialCode();
            Integer yearMonth = record.getYearMonth();
            Integer monthIndex = monthToIndex.get(yearMonth);

            if (monthIndex != null) {
                materialBitSets.computeIfAbsent(materialCode, k -> new BitSet())
                    .set(monthIndex);
            }
        }
        // 第三步：筛选结果
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, BitSet> entry : materialBitSets.entrySet()) {
            if (entry.getValue().cardinality() > MIN_MONTH_THRESHOLD) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
