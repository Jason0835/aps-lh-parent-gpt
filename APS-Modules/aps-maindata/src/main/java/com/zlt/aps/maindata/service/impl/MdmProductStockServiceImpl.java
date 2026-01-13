package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmProductStockEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductStockServiceImpl.java
 * 描    述：MdmProductStockServiceImpl成品库存业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmProductStockServiceImpl extends AbstractDocService<MdmProductStock> implements IMdmProductStockService {

    @Autowired
    private MdmProductStockEntityMapper mdmProductStockEntityMapper;

    @Autowired
    private IMesItfService mesItfService;

    @Override
    protected String getDocTypeCode() {
        return "MDM0216";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0216");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmProductStock docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmProductStock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<MdmProductStock> findCurrentFinishStock() {
        LambdaQueryWrapper<MdmProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmProductStock::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectList(wrapper);
    }

    @Override
    public List<MdmProductStock> getMpFinishedProductStockByMaterialCode(String materialCode) {
        LambdaQueryWrapper<MdmProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmProductStock::getMaterialCode, materialCode);
        wrapper.eq(MdmProductStock::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Map<String, Integer>> calculateStockQty() {
        List<MdmProductStock> list = this.findCurrentFinishStock();
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyMap();
        }
        YearMonth now = YearMonth.now();
        YearMonth lastOneYear = now.minusYears(BigDecimal.ONE.intValue());
        YearMonth lastTwoYear = now.minusYears(BigDecimal.ONE.intValue() + BigDecimal.ONE.intValue());
        Map<String, Map<String, Integer>> result = new HashMap<>();
        Map<String,List<MdmProductStock>> stockMap =   list.stream().collect(Collectors.groupingBy(MdmProductStock::getGroupKey));
        stockMap.forEach((key,value)->{
            Map<String, Integer> map = Maps.newHashMap();
            int totalStockQty = value.stream().filter(item -> null != item.getStockQty()).mapToInt(MdmProductStock::getStockQty).sum();
            int currentStockQty = value.stream().filter(item -> filter(item,now)).mapToInt(MdmProductStock::getStockQty).sum();
            int lastOneYearStockQty = value.stream().filter(item -> filter(item,lastOneYear)).mapToInt(MdmProductStock::getStockQty).sum();
            int lastTwoYearStockQty = value.stream().filter(item -> filter(item,lastTwoYear)).mapToInt(MdmProductStock::getStockQty).sum();
            map.put(StringConstant.ZERO,totalStockQty);
            map.put(StringConstant.ONE,currentStockQty);
            map.put(StringConstant.TWO,lastOneYearStockQty);
            map.put(StringConstant.THREE,lastTwoYearStockQty);
            result.put(key, map);
        });
        return result;
    }

    private boolean filter(MdmProductStock item, YearMonth yearMonth) {
        if(StringUtils.isBlank(item.getWeekYear()) || null == item.getStockQty()){
            return false;
        }
        if(yearMonth.equals(YearMonth.now())){
            String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(yearMonth.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
            String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
            int yearWeek = Integer.parseInt(transformed);
            return yearWeek >= Integer.parseInt(currentYearMonthStr);
        }
        if(yearMonth.equals(YearMonth.now().minusYears(BigDecimal.ONE.intValue()))){
            String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(yearMonth.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
            String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
            int yearWeek = Integer.parseInt(transformed);
            return yearWeek >= Integer.parseInt(currentYearMonthStr);
        }
        YearMonth lastOneYearWeek = YearMonth.now().minusYears(BigDecimal.ONE.intValue());
        String currentYearMonthStr = String.format("%s%02d", StringUtils.substring(String.valueOf(lastOneYearWeek.getYear()),2,4) ,Integer.valueOf(StringConstant.ONE));
        String transformed = item.getWeekYear().substring(2) + item.getWeekYear().substring(0,2);
        int yearWeek = Integer.parseInt(transformed);
        return yearWeek < Integer.parseInt(currentYearMonthStr);
    }
}
