package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.MdmCustomerInfoEntityMapper;
import com.zlt.aps.maindata.service.IMdmCustomerInfoService;
import com.zlt.aps.monthplan.api.domain.entity.MdmCustomerInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmCustomerInfoServiceImpl.java
 * 描    述：MdmCustomerInfoServiceImpl客户信息业务层处理
 *@author zlt
 *@date 2025-03-04
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
public class MdmCustomerInfoServiceImpl extends AbstractDocService<MdmCustomerInfo>  implements IMdmCustomerInfoService {

    @Autowired
    private MdmCustomerInfoEntityMapper entityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0142";
    }

    /**
     * 根据指定的查询条件，查询符合条件的客户信息列表。
     *
     * @param wrapper 查询条件封装对象，用于构建查询条件。该对象包含查询字段、排序规则、分页信息等。
     * @return 返回符合条件的客户信息列表。如果未找到符合条件的记录，则返回空列表。
     */
    @Override
    public List<MdmCustomerInfo> selectList(QueryWrapper<MdmCustomerInfo> wrapper) {
        if (wrapper != null) {
            return entityMapper.selectList(wrapper);
        }
        return Collections.emptyList();
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0142");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmCustomerInfo docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.column.mdmCustomerInfo.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "customCode");
    }

}
