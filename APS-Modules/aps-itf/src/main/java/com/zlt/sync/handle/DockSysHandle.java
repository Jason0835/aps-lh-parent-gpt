package com.zlt.sync.handle;

import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.povo.SyncParamsVO;

public interface DockSysHandle {

    void handle(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs);
}
