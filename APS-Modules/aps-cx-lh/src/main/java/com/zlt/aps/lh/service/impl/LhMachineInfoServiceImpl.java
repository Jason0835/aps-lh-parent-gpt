package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.annotation.DataImportCheck;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMachineInfoServiceImpl.java
 * 描    述：LhMachineInfoServiceImpl硫化机台信息业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMachineInfoServiceImpl extends AbstractDocService<LhMachineInfo> implements ILhMachineInfoService {


    @Resource
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0114";
    }

    /**
     * 查询硫化机台List
     * @param lhMachineInfo
     * @return
     */
    @Override
    public List<LhMachineInfo> selectList(LhMachineInfo  lhMachineInfo){
        LambdaQueryWrapper<LhMachineInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotBlank(lhMachineInfo.getFactoryCode()), LhMachineInfo::getFactoryCode, lhMachineInfo.getFactoryCode());
        queryWrapper.eq(StringUtils.isNotBlank(lhMachineInfo.getStatus()), LhMachineInfo::getStatus, lhMachineInfo.getStatus());
        queryWrapper.eq(StringUtils.isNotBlank(lhMachineInfo.getMachineCode()), LhMachineInfo::getMachineCode, lhMachineInfo.getMachineCode());
        List<LhMachineInfo> lhMachineInfoList = lhMachineInfoEntityMapper.selectList(queryWrapper);
        return lhMachineInfoList;
    }

    /**
     * 根据条件式查询
     * @param queryWrapper
     * @return
     */
    @Override
    public List<LhMachineInfo> selectListExportData(QueryWrapper<LhMachineInfo> queryWrapper){
        return lhMachineInfoEntityMapper.selectList(queryWrapper);
    }

    /**
     * 查询机台信息
     * @param factoryCode
     * @param machineCode
     * @return
     */
    @Override
    public LhMachineInfo selectOneByMachineCode(String factoryCode,String machineCode){
        LambdaQueryWrapper<LhMachineInfo> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotBlank(factoryCode), LhMachineInfo::getFactoryCode,factoryCode);
        queryWrapper.eq(StringUtils.isNotBlank(machineCode), LhMachineInfo::getMachineCode, machineCode);
        LhMachineInfo machineInfo = lhMachineInfoEntityMapper.selectOne(queryWrapper);
        return machineInfo;
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0114");
        return sysDocType;
    }

    @Override
    public String checkUnique(LhMachineInfo lhMachineInfo) {
        LambdaQueryWrapper<LhMachineInfo> lqw = Wrappers.lambdaQuery();
        lqw.ne(lhMachineInfo.getId() != null, LhMachineInfo::getId, lhMachineInfo.getId());
        lqw.eq(lhMachineInfo.getMachineCode() != null, LhMachineInfo::getMachineCode, lhMachineInfo.getMachineCode());
        lqw.eq(lhMachineInfo.getFactoryCode() != null, LhMachineInfo::getFactoryCode, lhMachineInfo.getFactoryCode());
        if (lhMachineInfoEntityMapper.selectCount(lqw) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "machineCode"));
    }


    @DataImportCheck(
            maxCount = 2000,
            messageKey = "ui.data.import.count.exceed",
            params = {"#list.size()", "2000"}
    )
    @Override
    public AjaxResult importData(List<LhMachineInfo> list, boolean updateSupport, Long importLogId) {
        return super.importData(list, updateSupport, importLogId);
    }


}
