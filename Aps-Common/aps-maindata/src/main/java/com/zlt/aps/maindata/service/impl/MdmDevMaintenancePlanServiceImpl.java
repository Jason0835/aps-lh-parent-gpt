package com.zlt.aps.maindata.service.impl;

import com.zlt.aps.maindata.mapper.MdmDevMaintenancePlanEntityMapper;
import com.zlt.aps.maindata.service.IMdmDevMaintenancePlanService;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * APS设备保养计划Service实现类
 */
@Service
public class MdmDevMaintenancePlanServiceImpl extends AbstractDocService<MdmDevMaintenancePlan> implements IMdmDevMaintenancePlanService {

    @Resource
    private MdmDevMaintenancePlanEntityMapper mdmDevMaintenancePlanEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

}
