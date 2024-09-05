package com.zlt.aps.mps.service;

import com.zlt.aps.mps.domain.TServiceSyncLog;

import java.util.List;

/**
 * @author Gim
 */
public interface MesSyncLogService {

    void addLog(TServiceSyncLog entity);

    void mergeSql(List<TServiceSyncLog> list);
}
