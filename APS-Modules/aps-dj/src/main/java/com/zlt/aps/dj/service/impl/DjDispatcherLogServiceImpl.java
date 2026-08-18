package com.zlt.aps.dj.service.impl;

import org.springframework.stereotype.Service;

import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.service.DjDispatcherLogService;
import com.zlt.bill.common.service.AbstractDocService;

/**
 * 垫胶调度员排程操作日志Service业务层处理
 * 
 * @author zlt
 * @date 2026-06-25
 */
@Service
public class DjDispatcherLogServiceImpl extends AbstractDocService<DjDispatcherLog> implements DjDispatcherLogService {

    @Override
    protected String getDocTypeCode() {
        return "";
    }

}
