package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMonCycleSchStruConfEntityMapper;
import com.zlt.aps.maindata.service.IMdmMonCycleSchStruConfService;
import com.zlt.aps.mp.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.SupplyOrderPool;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final MdmCycleSchStruConfEntityMapper mdmCycleSchStruConfEntityMapper;
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
    public List<MdmMonCycleSchStruConf> findCurrentCycleSchStruConf(SupplyOrderPool supplyOrderPool) {
        LambdaQueryWrapper<MdmMonCycleSchStruConf> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMonCycleSchStruConf::getFactoryCode, supplyOrderPool.getFactoryCode());
        wrapper.eq(MdmMonCycleSchStruConf::getYear, supplyOrderPool.getYear());
        wrapper.eq(MdmMonCycleSchStruConf::getMonth, supplyOrderPool.getMonth());
        wrapper.eq(MdmMonCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
        return mdmMonCycleSchStruConfEntityMapper.selectList(wrapper);
    }

    @Override
    public List<MdmMonCycleSchStruConf> queryAddStructList(MdmCycleSchStruConf queryVO) {
        LambdaQueryWrapper<MdmCycleSchStruConf> cycleWrapper = Wrappers.lambdaQuery();
        cycleWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), MdmCycleSchStruConf::getFactoryCode, queryVO.getFactoryCode());
        cycleWrapper.like(PubUtil.isNotEmpty(queryVO.getStructureName()), MdmCycleSchStruConf::getStructureName, queryVO.getStructureName());
        cycleWrapper.eq(MdmCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmCycleSchStruConf> cycleSchStruConfs = mdmCycleSchStruConfEntityMapper.selectList(cycleWrapper);
        if (PubUtil.isEmpty(cycleSchStruConfs)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<MdmMonCycleSchStruConf> monWrapper = Wrappers.lambdaQuery();
        monWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), MdmMonCycleSchStruConf::getFactoryCode, queryVO.getFactoryCode());
        monWrapper.eq(PubUtil.isNotEmpty(queryVO.getYear()), MdmMonCycleSchStruConf::getYear, queryVO.getYear());
        monWrapper.eq(PubUtil.isNotEmpty(queryVO.getMonth()), MdmMonCycleSchStruConf::getMonth, queryVO.getMonth());
        monWrapper.eq(MdmMonCycleSchStruConf::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MdmMonCycleSchStruConf> monthCycleSchStruConfs = mdmMonCycleSchStruConfEntityMapper.selectList(monWrapper);
        Set<String> existStructSet = new HashSet<>();
        if (PubUtil.isNotEmpty(monthCycleSchStruConfs)) {
            existStructSet = monthCycleSchStruConfs.stream()
                .map(item -> buildStructKey(item.getFactoryCode(), item.getStructureName()))
                .collect(Collectors.toSet());
        }

        Set<String> finalExistStructSet = existStructSet;
        return cycleSchStruConfs.stream()
            .filter(item -> !finalExistStructSet.contains(buildStructKey(item.getFactoryCode(), item.getStructureName())))
            .map(item -> {
                MdmMonCycleSchStruConf conf = new MdmMonCycleSchStruConf();
                conf.setFactoryCode(item.getFactoryCode());
                conf.setYear(queryVO.getYear());
                conf.setMonth(queryVO.getMonth());
                conf.setStructureName(item.getStructureName());
                conf.setTurnoverMonth(item.getTurnoverMonth());
                conf.setMinVulcanizingMachine(item.getMinVulcanizingMachine());
                return conf;
            })
            .collect(Collectors.toList());
    }

    @Override
    public AjaxResult addSave(MdmMonCycleSchStruConf mdmMonCycleSchStruConf) {
        if (UserConstants.NOT_UNIQUE.equals(this.checkUnique(mdmMonCycleSchStruConf))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmMonCycleSchStruConf.notUnique"));
        }
        int result = baseDao.save(mdmMonCycleSchStruConf);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    private String buildStructKey(String factoryCode, String structureName) {
        return String.format("%s_%s", factoryCode, structureName);
    }
}
