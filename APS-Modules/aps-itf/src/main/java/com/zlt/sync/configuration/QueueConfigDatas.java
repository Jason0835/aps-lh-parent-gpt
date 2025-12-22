package com.zlt.sync.configuration;

import com.zlt.sync.constants.ParamConstants;
import com.zlt.sync.povo.QueueDataVO;
import com.zlt.sync.povo.ScheduleConfigVO;
import com.zlt.sync.povo.SyncDataVO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "syncdata")
public class QueueConfigDatas {

    private String receiveQueue; //接收侦听队列
    private String dataSys; //需要系统排程 APS, 主计划 MPS
    private Integer asyncResult; //是否需要同步结果返回处理, 为1则需要处理, 如果有多个模块置1, syncDataHandle.asyncResult逻辑要一致
    private String asyncSuccCron; //asyncResult 为1时要配置
    private Map<String, QueueDataVO> sendQueues; //发送队列 (产销发送给玲珑对接系统)

    // 默认为 2 小时
    private Integer readTimeoutHours;
    /**
     * 是否设置default关联的syncKeys
     */
    private boolean isSetDefaultKeys = false;

    //定时调度配置
    private Map<String, ScheduleConfigVO> schedules;

    private List<SyncDataVO> configs = new ArrayList<>();

    /////////////////////////////////////////////////////////////

    /**
     * 设置default关联的syncKeys
     * @param _scheduleKey
     * @return
     */
    public synchronized List<String> getScheduleSyncKeys(String _scheduleKey) {

        if (isSetDefaultKeys) {
            return schedules.get(_scheduleKey).getSyncKeys();
        }

        Map<String, ScheduleConfigVO> schedules = this.getSchedules();
        List<SyncDataVO> syncDataVOS = this.getConfigs();

        if (schedules != null && schedules.size() > 0) {
            List<String> noDefaultSyncKeys = new ArrayList<>();

            // 获取 非default定时配置的 syncKeys
            for (String scheduleKey : schedules.keySet()) {
                if (ParamConstants.DEFAULTS.equals(scheduleKey)) {
                    continue;
                }

                ScheduleConfigVO configVO = schedules.get(scheduleKey);
                if (!CollectionUtils.isEmpty(configVO.getSyncKeys())) {
                    noDefaultSyncKeys.addAll(configVO.getSyncKeys());
                }
            }

            // 将 非default的syncKey存入 map
            Map<String, Object> noDefaultMap = new HashMap<>();

            for (String syncKey : noDefaultSyncKeys) {
                noDefaultMap.put(syncKey, new Object());
            }

            // 获取 default定时的 sycnKeys 列表;
            List<String> defaultSyncKeys = new ArrayList<>();
            if (!CollectionUtils.isEmpty(syncDataVOS)) {
                for (SyncDataVO dataVO : syncDataVOS) {
                    if (dataVO.getSyncKey() != null && !noDefaultMap.containsKey(dataVO.getSyncKey())
                            && !Integer.valueOf(1).equals(dataVO.getBackIssue())) {
                        defaultSyncKeys.add(dataVO.getSyncKey());
                    }
                }
            }
            schedules.get(ParamConstants.DEFAULTS).setSyncKeys(defaultSyncKeys);
        }
        isSetDefaultKeys = true;
        return schedules.get(_scheduleKey).getSyncKeys();
    }
    ////////////////////////////////////////////////////////////

    public String getReceiveQueue() {
        return receiveQueue;
    }

    public void setReceiveQueue(String receiveQueue) {
        this.receiveQueue = receiveQueue;
    }

    public String getDataSys() {
        return dataSys;
    }

    public void setDataSys(String dataSys) {
        this.dataSys = dataSys;
    }

    public Integer getAsyncResult() {
        return asyncResult;
    }

    public void setAsyncResult(Integer asyncResult) {
        this.asyncResult = asyncResult;
    }

    public String getAsyncSuccCron() {
        return asyncSuccCron;
    }

    public void setAsyncSuccCron(String asyncSuccCron) {
        this.asyncSuccCron = asyncSuccCron;
    }

    public Map<String, QueueDataVO> getSendQueues() {
        return sendQueues;
    }

    public void setSendQueues(Map<String, QueueDataVO> sendQueues) {
        this.sendQueues = sendQueues;
    }

    public Map<String, ScheduleConfigVO> getSchedules() {
        return schedules;
    }

    public void setSchedules(Map<String, ScheduleConfigVO> schedules) {
        this.schedules = schedules;
    }

    public List<SyncDataVO> getConfigs() {
        return configs;
    }

    public void setConfigs(List<SyncDataVO> configs) {
        this.configs = configs;
    }

    public Integer getReadTimeoutHours() {
        return readTimeoutHours;
    }

    public void setReadTimeoutHours(Integer readTimeoutHours) {
        this.readTimeoutHours = readTimeoutHours;
    }
}
