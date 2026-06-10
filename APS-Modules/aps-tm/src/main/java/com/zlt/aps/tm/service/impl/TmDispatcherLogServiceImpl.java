package com.zlt.aps.tm.service.impl;

import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.mapper.TmDispatcherLogMapper;
import com.zlt.aps.tm.service.ITmDispatcherLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 胎面调度员排程操作日志 服务实现
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmDispatcherLogServiceImpl extends AbstractDocService<TmDispatcherLog> implements ITmDispatcherLogService {

    @Resource
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0814";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0814");
        return sysDocType;
    }
}
