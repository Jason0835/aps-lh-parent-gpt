package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRatioEntityMapper;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.aps.maindata.service.IRawSpecialMaterialRatioService;
import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRatio;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawSpecialMaterialRatioServiceImpl.java
 * 描    述：RawSpecialMaterialRatioServiceImpl特殊材料批次比例业务层处理
 *@author zlt
 *@date 2025-12-08
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
public class RawSpecialMaterialRatioServiceImpl extends AbstractDocService<RawSpecialMaterialRatio>  implements IRawSpecialMaterialRatioService {

    @Autowired
    private RawSpecialMaterialRatioEntityMapper entityMapper;

    @Override
    protected String getDocTypeCode() {
        return "RAW9002";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("RAW9002");
        return sysDocType;
    }

    @Override
    public String checkUnique(RawSpecialMaterialRatio docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawSpecialMaterialRatio.notUnique"));
        }

        // 检查同一工厂、材料代码下的比例合计
        checkRatioSum(docEntityVO);

        return unique;
    }

    /**
     * 检查同一工厂、材料代码下的比例合计是否超过100
     * @param docEntityVO 当前记录
     */
    private void checkRatioSum(RawSpecialMaterialRatio docEntityVO) {
        // 查询同一工厂、材料代码下的所有记录（如果是更新操作，排除当前记录）
        QueryWrapper<RawSpecialMaterialRatio> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("factory_code", docEntityVO.getFactoryCode())
                .eq("material_code", docEntityVO.getMaterialCode());

        // 如果是更新操作，排除当前记录自身
        if (docEntityVO.getId() != null) {
            queryWrapper.ne("id", docEntityVO.getId());
        }

        List<RawSpecialMaterialRatio> existingRecords = entityMapper.selectList(queryWrapper);

        // 计算已有记录的比例总和
        BigDecimal totalRatio = existingRecords.stream()
                .map(RawSpecialMaterialRatio::getRatio)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 加上当前记录的比例
        if (docEntityVO.getRatio() != null) {
            totalRatio = totalRatio.add(docEntityVO.getRatio());
        }

        // 检查总和是否超过100
        if (totalRatio.compareTo(new BigDecimal("100")) > 0) {
            throw new ServiceException(
                    String.format(
                            I18nUtil.getMessage("ui.data.alert.rawSpecialMaterialRatio.ratioExceed"),
                            docEntityVO.getFactoryCode(),
                            docEntityVO.getMaterialCode(),
                            totalRatio
                    )
            );
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "materialCode", "standardLength"));
    }
}
