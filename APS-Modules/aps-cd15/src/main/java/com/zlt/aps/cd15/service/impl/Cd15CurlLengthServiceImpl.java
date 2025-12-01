package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.entity.Cd15Params;
import com.zlt.aps.cd15.mapper.Cd15CurlLengthEntityMapper;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.service.ICd15CurlLengthService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：Cd15CurlLengthServiceImpl.java
 * 描    述：Cd15CurlLengthServiceImpl钢丝斜裁卷曲长度业务层处理
 *@author zlt
 *@date 2025-03-11
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
public class Cd15CurlLengthServiceImpl extends AbstractDocService<Cd15CurlLength>  implements ICd15CurlLengthService {
    @Override
    protected String getDocTypeCode() {
        return "CD1501200";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD1501200");
        return sysDocType;
    }

    @Override
    public String checkUnique(Cd15CurlLength docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.cd15CurlLength.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Collections.singletonList("steelStripCode");
    }

    private static final Integer DEFAULT_CURL_LENGTH = 190;
    @Autowired
    private Cd15CurlLengthEntityMapper cd15CurlLengthMapper;
    @Autowired
    private Cd15ParamsMapper paramsMapper;

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlLength 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult selectCurlLengthByCode(Cd15CurlLength curlLength) {
        LambdaQueryWrapper<Cd15CurlLength> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15CurlLength::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(Cd15CurlLength::getSteelStripCode, curlLength.getQueryCode());
        Cd15CurlLength data = cd15CurlLengthMapper.selectOne(queryWrapper);
        LambdaQueryWrapper<Cd15Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApsBaseEntity::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(Cd15Params::getParamCode, "CRIMP_LENGTH");
        Cd15Params params = paramsMapper.selectOne(wrapper);
        if (data == null) {
            data = new Cd15CurlLength();
            data.setSteelStripCode(curlLength.getQueryCode());
            data.setCurlLength(params == null ? new BigDecimal(DEFAULT_CURL_LENGTH) : new BigDecimal(params.getParamValue()));
        }
        return AjaxResult.success(data);
    }
}
