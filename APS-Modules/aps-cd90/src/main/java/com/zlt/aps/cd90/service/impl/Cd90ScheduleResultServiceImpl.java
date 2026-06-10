package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheduleResultServiceImpl extends AbstractDocService<Cd90ScheduleResult> implements ICd90ScheduleResultService {

    @Override
    protected String getDocTypeCode() { return "CD90_SCHEDULE_RESULT"; }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_SCHEDULE_RESULT");
        return sysDocType;
    }
}