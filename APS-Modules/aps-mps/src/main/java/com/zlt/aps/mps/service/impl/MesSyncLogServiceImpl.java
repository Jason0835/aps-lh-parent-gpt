package com.zlt.aps.mps.service.impl;

import com.zlt.aps.mps.domain.TServiceSyncLog;
import com.zlt.aps.mps.mapper.TServiceSyncLogMapper;
import com.zlt.aps.mps.service.MesSyncLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Gim
 */
@Service
public class MesSyncLogServiceImpl implements MesSyncLogService {

    @Resource
    private TServiceSyncLogMapper mapper;

    @Override
    public void addLog(TServiceSyncLog entity) {
        mapper.insert(entity);
    }

    @Override
    public void mergeSql(List<TServiceSyncLog> list) {
        mapper.mergeSql(list);
    }
}
