package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.maindata.mapper.TireCapacityConfigurationMapper;
import com.zlt.aps.maindata.service.ITireCapacityConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.TireCapacityConfigurationVo;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TireCapacityConfigurationServiceImpl.java
 * 描    述：TireCapacityConfigurationServiceImpl轮胎类型产能配置(特殊情况下配置)业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TireCapacityConfigurationServiceImpl extends AbstractDocService<TireCapacityConfiguration> implements ITireCapacityConfigurationService {

    private final TireCapacityConfigurationMapper tireCapacityConfigurationMapper;

    @Override
    protected String getDocTypeCode() {
        return "0204";
    }

    @Override
    public String checkUnique(TireCapacityConfiguration docEntityVO) {
        if (null == docEntityVO) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<TireCapacityConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode());
        queryWrapper.eq("YEAR", docEntityVO.getYear());
        queryWrapper.eq("MONTH", docEntityVO.getMonth());
        queryWrapper.eq("PRO_SIZE", docEntityVO.getProSize());
        queryWrapper.eq("TIRE_TYPE", docEntityVO.getTireType());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        if (null != docEntityVO.getId()) {
            queryWrapper.ne("ID", docEntityVO.getId());
        }
        List<TireCapacityConfiguration> queryResultList = tireCapacityConfigurationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(queryResultList)) {
            return UserConstants.UNIQUE;
        }
        return queryResultList.size() == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    @Override
    public List<TireCapacityConfigurationVo> getConfigurationList(TireCapacityConfiguration condition) {
        if (null == condition) {
            return Collections.emptyList();
        }
        String factoryCode = condition.getFactoryCode();
        String monthPlanVersion = condition.getMonthPlanVersion();
        Integer year = condition.getYear();
        Integer month = condition.getMonth();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(monthPlanVersion) || null == year || null == month) {
            return Collections.emptyList();
        }
        return tireCapacityConfigurationMapper.getConfigurationList(condition);
    }

    @Override
    public TireCapacityConfigurationVo getDemandInfo(TireCapacityConfiguration condition) {
        if (null == condition) {
            return new TireCapacityConfigurationVo();
        }
        String factoryCode = condition.getFactoryCode();
        String monthPlanVersion = condition.getMonthPlanVersion();
        Integer year = condition.getYear();
        Integer month = condition.getMonth();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(monthPlanVersion) || null == year || null == month) {
            return new TireCapacityConfigurationVo();
        }
        String tireType = condition.getTireType();
        BigDecimal proSize = condition.getProSize();
        if (StringUtils.isBlank(tireType) || null == proSize) {
            return new TireCapacityConfigurationVo();
        }
        return tireCapacityConfigurationMapper.getDemandInfo(condition);
    }

    @Override
    public TireCapacityConfigurationVo getConfigurationById(Long id) {
        if (null == id) {
            return new TireCapacityConfigurationVo();
        }
        TireCapacityConfiguration configuration = tireCapacityConfigurationMapper.selectById(id);
        if (null == configuration) {
            return new TireCapacityConfigurationVo();
        }
        TireCapacityConfigurationVo configurationInfo = BeanCopyUtils.copyBean(configuration, TireCapacityConfigurationVo.class);
        TireCapacityConfigurationVo demandInfo = tireCapacityConfigurationMapper.getDemandInfo(configurationInfo);
        if (null != demandInfo) {
            configurationInfo.setDemandQty(demandInfo.getDemandQty());
            configurationInfo.setNetDemandQty(demandInfo.getNetDemandQty());
            configurationInfo.setStockUpDemandQty(demandInfo.getStockUpDemandQty());
        }
        return configurationInfo;
    }

    @Override
    public List<TireCapacityConfiguration> getConfigurationByFactoryYearAndMonth(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return Collections.emptyList();
        }
        QueryWrapper<TireCapacityConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return tireCapacityConfigurationMapper.selectList(queryWrapper);
    }
}
