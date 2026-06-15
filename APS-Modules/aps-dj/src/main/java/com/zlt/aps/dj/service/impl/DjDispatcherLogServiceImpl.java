package com.zlt.aps.dj.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.service.DjDispatcherLogService;
import com.zlt.bill.common.service.AbstractBillService;

/**
 * 垫胶调度员排程操作日志Service业务层处理
 * 
 * @author zlt
 * @date 2022-02-25
 */
@Service
public class DjDispatcherLogServiceImpl extends AbstractBillService<DjDispatcherLog>
        implements DjDispatcherLogService {
    @Override
    public int importData(List<DjDispatcherLog> list, boolean updateSupport, long importLogId) {
        return 0;
    }

    @Override
    protected String getBillTypeCode() {
        return "";
    }
    
}
