package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.service.ITmParamsService;
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
 * 文件名称：TmParamsServiceImpl.java
 * 描    述：TmParamsServiceImpl胎面排程参数配置业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-12
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmParamsServiceImpl extends AbstractDocService<TmParams> implements ITmParamsService {

    @Resource
    private TmParamsMapper tmParamsMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0801";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0801");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmParams query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.Params.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "paramCode"));
    }

    @Override
    public TmParams selectOneByParamCode(String paramCode, String factoryCode) {
        if (StringUtils.isBlank(paramCode)) {
            return null;
        }
        LambdaQueryWrapper<TmParams> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmParams::getParamCode, paramCode);
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(TmParams::getFactoryCode, factoryCode);
        }
        return tmParamsMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, String> listTmParams(String factoryCode) {
        Map<String, String> params = new HashMap<>();
        QueryWrapper<TmParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        List<TmParams> paramsList = tmParamsMapper.selectList(queryWrapper);
        if (PubUtil.isNotEmpty(paramsList)) {
            for (TmParams tmParam : paramsList) {
                params.put(tmParam.getParamCode(), tmParam.getParamValue());
            }
        }
        return params;
    }
}
