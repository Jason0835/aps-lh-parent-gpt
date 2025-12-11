package com.zlt.aps.monthplan.demand.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;

import lombok.extern.slf4j.Slf4j;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPoolServiceImpl.java
 * 描    述：SalesOrderPoolServiceImpl销售订单池业务层处理
 *@author zlt
 *@date 2025-12-04
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
public class SalesOrderPoolServiceImpl extends AbstractDocService<SalesOrderPool>  implements ISalesOrderPoolService {
	@Autowired
	private SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;
	
    @Override
    protected String getDocTypeCode() {
        return "MP099";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP099");
        return sysDocType;
    }

    @Override
    public String checkUnique(SalesOrderPool docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.salesOrderPool.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }
    
	/**
	 * 批量修改同PO号的销售优先级
	 * @param salesOrderPool
	 * @return
	 */
    @Override
    public AjaxResult editBySalCodePo(SalesOrderPool salesOrderPool) {
    	LambdaUpdateWrapper<SalesOrderPool> updateWrapper = new LambdaUpdateWrapper<>();
    	updateWrapper.eq(SalesOrderPool::getSalCodePo, salesOrderPool.getSalCodePo());
    	updateWrapper.set(SalesOrderPool::getScmPriority, salesOrderPool.getScmPriority());
    	salesOrderPoolEntityMapper.update(new SalesOrderPool(), updateWrapper);
        return AjaxResult.success();
	}
    
	/**
	 * 抓取SCM已计划未发货数据
	 * @param salesOrderPool
	 * @return
	 */
    @Override
    public AjaxResult getSCMData(SalesOrderPool salesOrderPool) {
    	// TODO 对接抓取接口
    	return null;
    }
}
