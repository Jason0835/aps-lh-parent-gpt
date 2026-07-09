package com.zlt.aps.nc.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import com.zlt.aps.nc.service.NcDispatcherLogService;
import com.zlt.bill.common.service.AbstractBillService;

/**
 * 内衬调度员排程操作日志Service业务层处理
 * 
 * @author zlt
 * @date 2026-06-25
 */
@Service
public class NcDispatcherLogServiceImpl extends AbstractBillService<NcDispatcherLog> implements NcDispatcherLogService {
    @Override
    public int importData(List<NcDispatcherLog> list, boolean updateSupport, long importLogId) {
        return 0;
    }

    @Override
    protected String getBillTypeCode() {
        return "";
    }
}
