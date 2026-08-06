package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tq.api.domain.entity.TqParams;
import com.zlt.aps.tq.mapper.TqParamsMapper;
import com.zlt.aps.tq.service.TqParamsService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TqParamsServiceImpl.java
 * 描    述：TqParamsServiceImpl胎圈排程参数配置业务层处理（对齐胎面 TmParamsServiceImpl）
 *
 * @author zlt
 * @version 1.0
 * @date 2025-12-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TqParamsServiceImpl extends AbstractDocService<TqParams> implements TqParamsService {

    @Resource
    private TqParamsMapper tqParamsMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ0801";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TQ0801");
        return sysDocType;
    }

    @Override
    public String checkUnique(TqParams query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.params.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段：工厂编码+参数编码
        return new ArrayList<>(Arrays.asList("factoryCode", "paramCode"));
    }

    @Override
    public TqParams selectOneByParamCode(String paramCode, String factoryCode) {
        if (StringUtils.isBlank(paramCode)) {
            return null;
        }
        LambdaQueryWrapper<TqParams> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TqParams::getParamCode, paramCode);
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(TqParams::getFactoryCode, factoryCode);
        }
        return tqParamsMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, String> listTqParams(String factoryCode) {
        Map<String, String> params = new HashMap<>();
        QueryWrapper<TqParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        List<TqParams> paramsList = tqParamsMapper.selectList(queryWrapper);
        if (PubUtil.isNotEmpty(paramsList)) {
            for (TqParams tqParam : paramsList) {
                params.put(tqParam.getParamCode(), tqParam.getParamValue());
            }
        }
        return params;
    }
}