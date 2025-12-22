package com.zlt.sync.handle.dockSys;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;

import com.zlt.sync.aspectj.AopSyncData;
import com.zlt.sync.domain.AuxReqSyncDataLogs;
import com.zlt.sync.handle.DockSysHandle;
import com.zlt.sync.povo.SyncParamsVO;

/**
 * Mes 处理类
 */
@Component
public class MesHandle implements DockSysHandle {

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Override
    public void handle(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs) {
    	getHandle().commonSync(paramsVO, dataLogs);
    }

    /**
     * 需要对接口进行增强设置，内部都用 getHandle().xxxXxxx 进行调用
     *  接口方法，都需要 public 进行修饰
     * @return Mes 处理类
     */
    private MesHandle getHandle() {
        return (null != AopContext.currentProxy()) ? (MesHandle)AopContext.currentProxy() : this;
    }

    /**
     *  通用同步接口
     * @param paramsVO 同步请求或通知参数
     * @param dataLogs 同步请求状态日志
     */
    @AopSyncData
    public void commonSync(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs) {
    }
}
