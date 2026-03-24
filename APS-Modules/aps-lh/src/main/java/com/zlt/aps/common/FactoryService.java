package com.zlt.aps.common;

import com.ruoyi.api.gateway.system.domain.SysConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.sync.mapper.SyncDataLogsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Chen
 * @date 2025/4/27
 */
@Service
@Slf4j
public class FactoryService {
    /**
     * 厂别系统配置key
     */
    private final static String SYS_FACTORY_CODE = "sys.factory.code";
    /**
     * 公司编号系统配置key
     */
    private final static String SYS_BRANCH_OFFICE = "sys.branch.office";
    @Autowired
    private SyncDataLogsMapper syncDataLogsMapper;
    @Autowired
    private RedisService redisService;

    /**
     * 获取当前所属厂别
     *
     * @return
     */
    public String getFactoryCode() {
        return this.selectConfigByKey(SYS_FACTORY_CODE);
    }

    /**
     * 获取当前所属分公司代号
     *
     * @return
     */
    public String getCompanyCode() {
        return this.selectConfigByKey(SYS_BRANCH_OFFICE);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    public String selectConfigByKey(String configKey) {
        String configValue = Convert.toStr(redisService.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue)) {
            return configValue;
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        SysConfig retConfig = syncDataLogsMapper.selectConfig(config);
        if (StringUtils.isNotNull(retConfig)) {
            redisService.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey) {
        return Constants.SYS_CONFIG_KEY + configKey;
    }
}
