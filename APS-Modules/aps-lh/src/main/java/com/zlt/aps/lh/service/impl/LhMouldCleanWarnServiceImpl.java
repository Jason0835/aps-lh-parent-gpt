package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.mapper.LhMouldCleanWarnMapper;
import com.zlt.aps.lh.service.ILhMouldCleanWarnService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模具清洗预警业务层处理
 *
 * @author APS Team
 * @since 2026/04/10
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMouldCleanWarnServiceImpl extends AbstractDocService<LhMouldCleanWarn> implements ILhMouldCleanWarnService {

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0");
        return sysDocType;
    }
}
