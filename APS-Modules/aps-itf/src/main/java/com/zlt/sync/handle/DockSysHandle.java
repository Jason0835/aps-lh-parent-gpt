package com.zlt.sync.handle;

import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.sync.povo.SyncParamsVO;

public interface DockSysHandle {

    void handle(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs);
}
