package com.zlt.aps.cd15.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.service.ICd15ScheduleLaneAllocationService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CD15斜裁排程库排分配明细ServiceImpl。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ScheduleLaneAllocationServiceImpl extends AbstractDocService<Cd15ScheduleLaneAllocation> implements ICd15ScheduleLaneAllocationService {

    @Override
    protected String getDocTypeCode() {
        return "CD15_SCHEDULE_LANE_ALLOCATION";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_SCHEDULE_LANE_ALLOCATION");
        return sysDocType;
    }
}