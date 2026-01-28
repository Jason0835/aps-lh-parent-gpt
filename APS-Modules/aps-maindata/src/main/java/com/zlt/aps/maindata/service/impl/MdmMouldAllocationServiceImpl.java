package com.zlt.aps.maindata.service.impl;

import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.service.IMdmMouldAllocationService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldAllocation;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMouldAllocationServiceImpl.java
 * 描    述：MdmMouldAllocationServiceImpl模具分配比例(同结构/不同结构)业务层处理
 *@author zlt
 *@date 2025-12-14
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
public class MdmMouldAllocationServiceImpl extends AbstractDocService<MdmMouldAllocation>  implements IMdmMouldAllocationService {

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;

    @Override
    protected String getDocTypeCode() {
        return "MDM0118";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0118");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMouldAllocation docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            List<SysDictData> dictDataList = sysDictDataCacheService.getType("biz_factory_name");
            List<SysDictData> dictData = dictDataList.stream().filter(item -> item.getDictValue().equals(docEntityVO.getFactoryCode())).collect(Collectors.toList());
            String dictLabel = docEntityVO.getFactoryCode();
            if (CollectionUtils.isNotEmpty(dictData)) {
                dictLabel = dictData.get(0).getDictLabel();
            }
            String message = StringUtils.format(I18nUtil.getMessage("ui.data.alert.mdmMouldAllocation.notUnique"),
                    docEntityVO.getSpecifications(), docEntityVO.getMainPattern(),
                    docEntityVO.getStructureName(), dictLabel, docEntityVO.getYear(), docEntityVO.getMonth());
            throw new ServiceException(message);
        }
        return unique;
    }


    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "structureName", "specifications", "mainPattern"));
    }


}
