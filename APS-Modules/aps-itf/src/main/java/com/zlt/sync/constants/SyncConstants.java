package com.zlt.sync.constants;

import com.ruoyi.common.exception.CustomException;
import com.zlt.sync.povo.SyncDataVO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * APP定义标志
 * 配置文件: sync-data-module-${spring.profiles.active}.yml
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "syncdata")
public class SyncConstants {

    private List<SyncDataVO> configs = new ArrayList<>();

    public List<SyncDataVO> getConfigs() {
        return configs;
    }

    public void setConfigs(List<SyncDataVO> configs) {
        this.configs = configs;
    }

    /**
     * syncKey 根据业务模块获取同步块信息
     * @param syncKey
     * @return
     */
    public SyncDataVO getSyncDataByKey(String syncKey) {
        if (syncKey == null) {
            throw new CustomException("同步数据标志key不能为空: syncKey");
        }
        for (SyncDataVO dataVO : configs) {
            if (syncKey.equals(dataVO.getSyncKey())) {
                return dataVO;
            }
        }

        return null;
    }
}
