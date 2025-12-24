package com.zlt.sync.handle;

import com.ruoyi.common.core.utils.SpringUtils;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.sync.handle.dockSys.MesHandle;
import com.zlt.sync.povo.SyncParamsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对接系统处理转向
 */
public class DockSysFactory {

    private static final Logger logger = LoggerFactory.getLogger(DockSysFactory.class);
    private static final Map<String, DockSysHandle> dockSysMap = new ConcurrentHashMap<>();

    private static Object lockObj = new Object();

    /**
     * 请求反馈统一方法
     * @param paramsVO
     * @param dataLogs
     */
    public static void handle(SyncParamsVO paramsVO, AuxReqSyncDataLogs dataLogs) {

        /**
         * 日志及MQ发送通过 aop 来触发
         */
        logger.info("DockSysFactory-handle 请求通知数据，转到具体类处理");
        if (dockSysMap.containsKey(paramsVO.getDockSys())) {
            dockSysMap.get(paramsVO.getDockSys()).handle(paramsVO, dataLogs);
        } else {
            synchronized (lockObj) {
                if (dockSysMap.size() == 0) {
                    initHandles();
                }
            }

            if (dockSysMap.containsKey(paramsVO.getDockSys())) {
                dockSysMap.get(paramsVO.getDockSys()).handle(paramsVO, dataLogs);
            } else {
                logger.error("DockSysFactory-handle-001 请求通知数据，转到具体类处理, 系统不存在: " + paramsVO.getDockSys());
            }
        }
    }

    /**
     * 处理类初始化
     */
    private static void initHandles() {
        MesHandle mesHandle = SpringUtils.getBean(MesHandle.class);
        dockSysMap.putIfAbsent("MES", mesHandle);
    }
}
