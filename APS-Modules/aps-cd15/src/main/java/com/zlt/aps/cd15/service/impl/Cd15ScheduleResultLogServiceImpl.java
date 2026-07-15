package com.zlt.aps.cd15.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.service.ICd15ScheduleResultLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CD15斜裁排程结果日志ServiceImpl。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ScheduleResultLogServiceImpl extends AbstractDocService<Cd15ScheduleResultLog> implements ICd15ScheduleResultLogService {

    @Override
    protected String getDocTypeCode() {
        return "CD15_SCHEDULE_RESULT_LOG";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_SCHEDULE_RESULT_LOG");
        return sysDocType;
    }
}