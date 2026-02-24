package com.zlt.aps.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.mdm.mapper.MdmPersonLevelMapper;
import com.zlt.aps.mdm.service.IMdmPersonLevelService;
import com.zlt.aps.mdm.api.domain.entity.MdmPersonLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmPersonLevelServiceImpl.java
 * 描    述：MdmPersonLevelServiceImpl成型机人员档配置业务层处理
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Service
public class MdmPersonLevelServiceImpl extends ServiceImpl<MdmPersonLevelMapper, MdmPersonLevel> implements IMdmPersonLevelService {


    @Override
    public MdmPersonLevel selectMdmPersonLevelById(Long id) {
        return getBaseMapper().selectById(id);
    }

    @Override
    public List<MdmPersonLevel> selectMdmPersonLevelList(MdmPersonLevel mdmPersonLevel) {
        return getBaseMapper().selectMdmPersonLevelList(mdmPersonLevel);
    }

    @Override
    public String checkUnique(MdmPersonLevel mdmPersonLevel) {
        LambdaQueryWrapper<MdmPersonLevel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(MdmPersonLevel::getIsDelete, ApsConstant.DEL_FLAG_NORMAL);
        lambdaQueryWrapper.ne(mdmPersonLevel.getId() != null, MdmPersonLevel::getId, mdmPersonLevel.getId());
        lambdaQueryWrapper.eq(MdmPersonLevel::getYear, mdmPersonLevel.getYear());
        lambdaQueryWrapper.eq(MdmPersonLevel::getMonth, mdmPersonLevel.getMonth());
        lambdaQueryWrapper.eq(MdmPersonLevel::getFactoryCode, mdmPersonLevel.getFactoryCode());
        lambdaQueryWrapper.eq(MdmPersonLevel::getLevelCode, mdmPersonLevel.getLevelCode());
        lambdaQueryWrapper.eq(MdmPersonLevel::getMethodType, mdmPersonLevel.getMethodType());
        Long count = getBaseMapper().selectCount(lambdaQueryWrapper);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
