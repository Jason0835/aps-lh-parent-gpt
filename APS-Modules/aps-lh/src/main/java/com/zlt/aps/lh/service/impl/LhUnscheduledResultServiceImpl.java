package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.mapper.LhUnscheduledResultEntityMapper;
import com.zlt.aps.lh.service.ILhUnscheduledResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhUnscheduledResultServiceImpl.java
 * 描    述：LhUnscheduledResultServiceImpl硫化未排结果业务层处理
 *@author zlt
 *@date 2026-04-30
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
public class LhUnscheduledResultServiceImpl extends AbstractDocService<LhUnscheduledResult>  implements ILhUnscheduledResultService {

    @Autowired
    private LhUnscheduledResultEntityMapper entityMapper;

    @Override
    protected String getDocTypeCode() {
        return "LH1010";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("LH1010");
        return sysDocType;
    }

    @Override
    public String checkUnique(LhUnscheduledResult docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.lhUnscheduledResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>();
    }

    /**
     * 根据排程日期和工厂删除未排产结果
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编号
     * @return 删除记录数
     */
    @Override
    public int deleteByDateAndFactory(Date scheduleDate, String factoryCode) {
        LambdaUpdateWrapper<LhUnscheduledResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(LhUnscheduledResult::getUpdateTime, new Date())
                .set(BaseEntity::getUpdateBy, SecurityUtils.getUsername())
                .set(BaseEntity::getIsDelete, YesOrNoEnum.YES.getValue())
                .eq(LhUnscheduledResult::getScheduleDate, scheduleDate)
                .eq(LhUnscheduledResult::getFactoryCode, factoryCode);
        return entityMapper.update(null, updateWrapper);
    }

}
