package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.mapper.VulcanizingMachineMapper;
import com.zlt.aps.maindata.service.IVulcanizingMachineService;
import com.zlt.aps.mp.api.domain.entity.VulcanizingMachine;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：VulcanizingMachineServiceImpl.java
 * 描    述：VulcanizingMachineServiceImpl基础数据-硫化机档案业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Service
public class VulcanizingMachineServiceImpl extends AbstractDocService<VulcanizingMachine> implements IVulcanizingMachineService {

    private final VulcanizingMachineMapper vulcanizingMachineMapper;

    public VulcanizingMachineServiceImpl(VulcanizingMachineMapper vulcanizingMachineMapper) {
        this.vulcanizingMachineMapper = vulcanizingMachineMapper;
    }

    @Override
    protected String getDocTypeCode() {
        return "0122";
    }
    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0122");
        return sysDocType;
    }

    @Override
    public String checkUnique(VulcanizingMachine docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.vulcanizingMachine.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "lineCode", "vulcanizingMachineCode", "productTypeCode");
    }

    /**
     * 根据字段精确查询
     */
    @Override
    public List<VulcanizingMachine> selectListByVulcanizingMachine(VulcanizingMachine vulcanizingMachine) {
        LambdaQueryWrapper<VulcanizingMachine> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.isNotBlank(vulcanizingMachine.getFactoryCode()), VulcanizingMachine::getFactoryCode, vulcanizingMachine.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(vulcanizingMachine.getVulcanizingMachineCode()), VulcanizingMachine::getVulcanizingMachineCode, vulcanizingMachine.getVulcanizingMachineCode());
        wrapper.eq(StringUtils.isNotBlank(vulcanizingMachine.getProductTypeCode()), VulcanizingMachine::getProductTypeCode, vulcanizingMachine.getProductTypeCode());
        return vulcanizingMachineMapper.selectList(wrapper);
    }


}
