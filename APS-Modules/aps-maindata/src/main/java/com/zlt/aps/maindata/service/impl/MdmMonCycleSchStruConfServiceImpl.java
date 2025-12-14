package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.monthplan.api.domain.entity.LhMachineInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMonCycleSchStruConfServiceImpl.java
 * 描    述：MdmMonCycleSchStruConfServiceImpl月周期排产结构配置业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class MdmMonCycleSchStruConfServiceImpl extends AbstractDocService<MdmMonCycleSchStruConf> implements IMdmMonCycleSchStruConfService {
    private  final MdmMonCycleSchStruConfEntityMapper mdmMonCycleSchStruConfEntityMapper;
    @Override
    protected String getDocTypeCode() {
        return "MDM0143";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0143");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmMonCycleSchStruConf docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmMonCycleSchStruConf.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return new ArrayList<>(Arrays.asList("factoryCode", "year", "month", "structureName"));
    }

    @Override
    public List<MdmMonCycleSchStruConf> findCurrentCycleSchStruConf() {
        LambdaQueryWrapper<MdmMonCycleSchStruConf> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMonCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
        return mdmMonCycleSchStruConfEntityMapper.selectList(wrapper);
    }
}
