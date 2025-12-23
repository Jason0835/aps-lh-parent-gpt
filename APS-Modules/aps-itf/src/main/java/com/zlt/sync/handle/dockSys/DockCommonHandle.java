package com.zlt.sync.handle.dockSys;

import com.zlt.sync.aspectj.AopSyncData;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.handle.DockSysHandle;
import com.zlt.sync.povo.SyncParamsVO;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

/**
 * 对接系统公共处理类
 */
@Component
public class DockCommonHandle implements DockSysHandle {

    @Override
    public void handle(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs) {
        getHandle().reSendRequest(paramsVO, dataLogs);
    }

    /**
     * 需要对接口进行增强设置，内部都用 getHandle().xxxXxxx 进行调用
     *  接口方法，都需要 public 进行修饰
     * @return
     */
    private DockCommonHandle getHandle() {
        return (null != AopContext.currentProxy()) ? (DockCommonHandle)AopContext.currentProxy() : this;
    }

    /**
     * 重新发送请求
     * @param paramsVO
     * @param dataLogs
     */
    @AopSyncData
    public void reSendRequest(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs) {
    }


}
