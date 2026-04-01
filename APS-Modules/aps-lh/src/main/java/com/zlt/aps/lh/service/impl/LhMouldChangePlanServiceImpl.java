package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.service.ILhMouldChangePlanService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 模具交替计划Service实现
 *
 * @author APS Team
 * @since 2026/04/01
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMouldChangePlanServiceImpl extends AbstractDocService<LhMouldChangePlan> implements ILhMouldChangePlanService {

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;


    @Override
    public String[] getQueryFormulas() {
        return new String[0];
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
