package com.zlt.aps.mp.adjust.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureOutEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureOutService;
import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.zlt.bill.common.service.AbstractDocService;
import com.ruoyi.common.exception.ServiceException;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustStructureOutServiceImpl.java
 * 描    述：MpAdjustStructureOutServiceImpl调整-结构调整记录业务层处理
 *@author zlt
 *@date 2025-12-19
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
public class MpAdjustStructureOutServiceImpl extends AbstractDocService<MpAdjustStructureOut>  implements IMpAdjustStructureOutService {

    @Autowired
    private MpAdjustStructureOutEntityMapper structureOutEntityMapper;


    @Override
    public List<MpAdjustStructureOut> selectMpAdjustStructureOutList(MpRollAdjustContextDTO contextDTO) {
        QueryWrapper<MpAdjustStructureOut> structureOutQueryWrapper = new QueryWrapper<>();
        structureOutQueryWrapper.eq("FACTORY_CODE", contextDTO.getFactoryCode());
        structureOutQueryWrapper.eq("YEAR", contextDTO.getMpYear());
        structureOutQueryWrapper.eq("MONTH", contextDTO.getMpMonth());
        structureOutQueryWrapper.eq("VERSION", contextDTO.getVersion());
        structureOutQueryWrapper.eq("STRUCTURE_NAME", contextDTO.getStructureName());
        return structureOutEntityMapper.selectList(structureOutQueryWrapper);
    }

    @Override
    protected String getDocTypeCode() {
        return "MP0806";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP0806");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpAdjustStructureOut docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpAdjustStructureOut.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
}
