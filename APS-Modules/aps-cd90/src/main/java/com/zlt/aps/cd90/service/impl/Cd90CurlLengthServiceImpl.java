package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.entity.Cd90Params;
import com.zlt.aps.cd90.mapper.Cd90CurlLengthEntityMapper;
import com.zlt.aps.cd90.mapper.Cd90ParamsMapper;
import com.zlt.aps.cd90.service.ICd90CurlLengthService;
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
 * 文件名称：Cd90CurlLengthServiceImpl.java
 * 描    述：Cd90CurlLengthServiceImpl纤维直裁卷曲长度业务层处理
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
public class Cd90CurlLengthServiceImpl extends AbstractDocService<Cd90CurlLength>  implements ICd90CurlLengthService {
    @Override
    protected String getDocTypeCode() {
        return "CD9001200";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD9001200");
        return sysDocType;
    }

    @Override
    public String checkUnique(Cd90CurlLength docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.cd90CurlLength.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Collections.singletonList("clothCode");
    }

    private static final Integer DEFAULT_CURL_LENGTH = 87;
    @Autowired
    private Cd90CurlLengthEntityMapper cd90CurlLengthMapper;
    @Autowired
    private Cd90ParamsMapper paramsMapper;

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlLength 查询条件
     * @return 结果
     */
    @Override
    public AjaxResult selectCurlLengthByCode(Cd90CurlLength curlLength) {
        LambdaQueryWrapper<Cd90CurlLength> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd90CurlLength::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(Cd90CurlLength::getClothCode, curlLength.getQueryCode());
        Cd90CurlLength data = cd90CurlLengthMapper.selectOne(queryWrapper);
        LambdaQueryWrapper<Cd90Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApsBaseEntity::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(Cd90Params::getParamCode, "CRIMP_LENGTH");
        Cd90Params params = paramsMapper.selectOne(wrapper);
        if (data == null) {
            data = new Cd90CurlLength();
            data.setClothCode(curlLength.getQueryCode());
            data.setCurlLength(params == null ? new BigDecimal(DEFAULT_CURL_LENGTH) : new BigDecimal(params.getParamValue()));
        }
        return AjaxResult.success(data);
    }
}
