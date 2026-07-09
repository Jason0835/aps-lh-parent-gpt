package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.service.ITcParamsService;
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
 * 文件名称：TcParamsServiceImpl.java
 * 描    述：TcParamsServiceImpl胎侧排程参数配置业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-07-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcParamsServiceImpl extends AbstractDocService<TcParams> implements ITcParamsService {

    @Resource
    private TcParamsMapper tcParamsMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0901";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0901");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcParams query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.params.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "paramCode"));
    }

    @Override
    public TcParams selectOneByParamCode(String paramCode, String factoryCode) {
        if (StringUtils.isBlank(paramCode)) {
            return null;
        }
        LambdaQueryWrapper<TcParams> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TcParams::getParamCode, paramCode);
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(TcParams::getFactoryCode, factoryCode);
        }
        return tcParamsMapper.selectOne(wrapper);
    }

    @Override
    public Map<String, String> listTcParams(String factoryCode) {
        Map<String, String> params = new HashMap<>();
        QueryWrapper<TcParams> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        List<TcParams> paramsList = tcParamsMapper.selectList(queryWrapper);
        if (PubUtil.isNotEmpty(paramsList)) {
            for (TcParams tcParam : paramsList) {
                params.put(tcParam.getParamCode(), tcParam.getParamValue());
            }
        }
        return params;
    }
}
