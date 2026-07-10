package com.zlt.aps.tc.service.impl;

import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import com.zlt.aps.tc.mapper.TcDispatcherLogMapper;
import com.zlt.aps.tc.service.ITcDispatcherLogService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 胎侧调度员排程操作日志 服务实现
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcDispatcherLogServiceImpl extends AbstractDocService<TcDispatcherLog> implements ITcDispatcherLogService {

    @Resource
    private TcDispatcherLogMapper tcDispatcherLogMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0914";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0914");
        return sysDocType;
    }
}