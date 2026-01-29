package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.maindata.mapper.MdmProductStockEntityMapper;
import com.zlt.aps.maindata.service.IMdmProductStockService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductStockServiceImpl.java
 * 描    述：MdmProductStockServiceImpl成品库存业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmProductStockServiceImpl extends AbstractDocService<MdmProductStock> implements IMdmProductStockService {

    @Autowired
    private MdmProductStockEntityMapper mdmProductStockEntityMapper;

    @Autowired
    private IMesItfService mesItfService;

    @Override
    protected String getDocTypeCode() {
        return "MDM0216";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MDM0216");
        return sysDocType;
    }

    @Override
    public String checkUnique(MdmProductStock docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mdmProductStock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    @Override
    public List<MdmProductStock> findCurrentFinishStock() {
        LambdaQueryWrapper<MdmProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmProductStock::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectList(wrapper);
    }

    @Override
    public List<MdmProductStock> getMpFinishedProductStockByMaterialCode(String materialCode) {
        LambdaQueryWrapper<MdmProductStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmProductStock::getMaterialCode, materialCode);
        wrapper.eq(MdmProductStock::getIsDelete, YesOrNoEnum.NO.getValue());
        return this.mdmProductStockEntityMapper.selectList(wrapper);
    }

    @Override
    public List<MdmProductStock> findCurrentFinishStock(String factoryCode, Set<String> skus) {
        List<MdmProductStock> result = Lists.newArrayList();
        final int batchSize = 1000;
        List<String> skuList = new ArrayList<>(skus);
        for (int i = 0; i < skus.size(); i += batchSize) {
            int end = Math.min(i + batchSize, skus.size());
            List<String> batchSkus = skuList.subList(i, end);
            LambdaQueryWrapper<MdmProductStock> wrapper =
                Wrappers.lambdaQuery(MdmProductStock.class)
                    .eq(MdmProductStock::getFactoryCode, factoryCode)
                    .in(MdmProductStock::getMaterialCode, batchSkus)
                    .eq(MdmProductStock::getIsDelete, ApsConstant.APS_YES_NO_0);
            result.addAll(mdmProductStockEntityMapper.selectList(wrapper));
        }
        return result;
    }
}
