package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResultLog;
import com.zlt.aps.cd90.service.ICd90ScheduleResultLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheduleResultLogServiceImpl extends AbstractDocService<Cd90ScheduleResultLog> implements ICd90ScheduleResultLogService {
    @Override protected String getDocTypeCode() { return "CD90_SCHEDULE_RESULT_LOG"; }
    @Override protected SysDocType getSysDocType() { SysDocType t = new SysDocType(); t.setDocTypeCode("CD90_SCHEDULE_RESULT_LOG"); return t; }
}