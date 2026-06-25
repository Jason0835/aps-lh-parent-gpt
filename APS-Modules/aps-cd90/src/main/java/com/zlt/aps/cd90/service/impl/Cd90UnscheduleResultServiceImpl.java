package com.zlt.aps.cd90.service.impl;

import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.service.ICd90UnscheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 直裁未排结果ServiceImpl。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90UnscheduleResultServiceImpl extends AbstractDocService<Cd90UnscheduleResult> implements ICd90UnscheduleResultService {
    @Override
    protected String getDocTypeCode() {
        return "CD90_UNSCHEDULE_RESULT";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType t = new SysDocType();
        t.setDocTypeCode("CD90_UNSCHEDULE_RESULT");
        return t;
    }
}
