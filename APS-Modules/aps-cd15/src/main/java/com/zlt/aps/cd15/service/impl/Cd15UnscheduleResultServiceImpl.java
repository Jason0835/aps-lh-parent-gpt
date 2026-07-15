package com.zlt.aps.cd15.service.impl;

import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import com.zlt.aps.cd15.service.ICd15UnscheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CD15斜裁未排结果ServiceImpl。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15UnscheduleResultServiceImpl extends AbstractDocService<Cd15UnscheduleResult> implements ICd15UnscheduleResultService {

    @Override
    protected String getDocTypeCode() {
        return "CD15_UNSCHEDULE_RESULT";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_UNSCHEDULE_RESULT");
        return sysDocType;
    }
}