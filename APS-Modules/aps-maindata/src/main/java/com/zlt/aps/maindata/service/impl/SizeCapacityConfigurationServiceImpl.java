package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.BeanCopyUtils;
import com.zlt.aps.maindata.mapper.SizeCapacityConfigurationMapper;
import com.zlt.aps.maindata.service.ISizeCapacityConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.SizeCapacityConfigurationVo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
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
 * 文件名称：SizeCapacityConfigurationServiceImpl.java
 * 描    述：SizeCapacityConfigurationServiceImpl寸口产能配置业务层处理
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
public class SizeCapacityConfigurationServiceImpl extends AbstractDocService<SizeCapacityConfiguration> implements ISizeCapacityConfigurationService {

    private final SizeCapacityConfigurationMapper sizeCapacityConfigurationMapper;

    @Override
    protected String getDocTypeCode() {
        return "MDM0203";
    }

    @Override
    public String checkUnique(SizeCapacityConfiguration docEntityVO) {
        if (null == docEntityVO) {
            return UserConstants.NOT_UNIQUE;
        }
        QueryWrapper<SizeCapacityConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode());
        queryWrapper.eq("YEAR", docEntityVO.getYear());
        queryWrapper.eq("MONTH", docEntityVO.getMonth());
        queryWrapper.eq("PRO_SIZE", docEntityVO.getProSize());
        queryWrapper.eq("MOULD_METHOD", docEntityVO.getMouldMethod());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        if (null != docEntityVO.getId()) {
            queryWrapper.ne("ID", docEntityVO.getId());
        }
        List<SizeCapacityConfiguration> queryResultList = sizeCapacityConfigurationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(queryResultList)) {
            return UserConstants.UNIQUE;
        }
        return queryResultList.size() == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    @Override
    public List<SizeCapacityConfiguration> getConfigurationList(SizeCapacityConfiguration queryCondition) {
        if (null == queryCondition) {
            return Collections.emptyList();
        }
        String factoryCode = queryCondition.getFactoryCode();
        Integer year = queryCondition.getYear();
        Integer month = queryCondition.getMonth();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return Collections.emptyList();
        }
        QueryWrapper<SizeCapacityConfiguration> queryWrapper = new QueryWrapper<>();
        builderCondition(queryWrapper, queryCondition);
        return sizeCapacityConfigurationMapper.selectList(queryWrapper);
//        return sizeCapacityConfigurationMapper.getConfigurationList(queryCondition);
    }

    @Override
    public SizeCapacityConfigurationVo getDemandInfo(SizeCapacityConfiguration condition) {
        if (null == condition) {
            return new SizeCapacityConfigurationVo();
        }
        String factoryCode = condition.getFactoryCode();
        String monthPlanVersion = condition.getMonthPlanVersion();
        Integer year = condition.getYear();
        Integer month = condition.getMonth();
        BigDecimal proSize = condition.getProSize();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(monthPlanVersion) || null == year || null == month || null == proSize) {
            return new SizeCapacityConfigurationVo();
        }
        return sizeCapacityConfigurationMapper.getDemandInfo(condition);
    }

    @Override
    public SizeCapacityConfigurationVo getConfigurationById(Long id) {
        if (null == id) {
            return new SizeCapacityConfigurationVo();
        }
        SizeCapacityConfiguration configuration = sizeCapacityConfigurationMapper.selectById(id);
        if (null == configuration) {
            return new SizeCapacityConfigurationVo();
        }
        SizeCapacityConfigurationVo configurationInfo = BeanCopyUtils.copyBean(configuration, SizeCapacityConfigurationVo.class);
        SizeCapacityConfigurationVo demandInfo = sizeCapacityConfigurationMapper.getDemandInfo(configurationInfo);
        if (null != demandInfo) {
            configurationInfo.setDemandQty(demandInfo.getDemandQty());
            configurationInfo.setNetDemandQty(demandInfo.getNetDemandQty());
            configurationInfo.setStockUpDemandQty(demandInfo.getStockUpDemandQty());
        }
        return configurationInfo;
    }

    @Override
    public List<SizeCapacityConfiguration> getConfigurationByFactoryYearAndMonth(String factoryCode, Integer year, Integer month) {
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return Collections.emptyList();
        }
        QueryWrapper<SizeCapacityConfiguration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return sizeCapacityConfigurationMapper.selectList(queryWrapper);
    }

    /**
     * 构建查询条件
     *
     * @param queryWrapper   查询器
     * @param queryCondition 查询条件对象
     */
    public void builderCondition(QueryWrapper<?> queryWrapper, SizeCapacityConfiguration queryCondition) {
        queryWrapper.eq("FACTORY_CODE", queryCondition.getFactoryCode());
        queryWrapper.eq("YEAR", queryCondition.getYear());
        queryWrapper.eq("MONTH", queryCondition.getMonth());
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getProSize()), "PRO_SIZE", queryCondition.getProSize());
        queryWrapper.eq(PubUtil.isNotEmpty(queryCondition.getMouldMethod()), "MOULD_METHOD", queryCondition.getMouldMethod());
    }

}
