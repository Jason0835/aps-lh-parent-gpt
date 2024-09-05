package com.zlt.framework;

import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.constant.CacheConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@Getter
public class GlobalSetting {

    @Autowired
    ISysConfigService iSysConfigService;

    @Autowired
    private ISysUserService iSysUserService;

    private ConcurrentHashMap<String, Object> options = new ConcurrentHashMap<>();

    /**
     * 单例模式取系统代码，重启生效
     *
     * @return
     */
    public String getSysCode() {

        String sysCode = (String) options.get(CacheConstants.SYSTEM_CODE_KEY_PREFIX);

        if (StringUtils.isEmpty(sysCode)) {
            sysCode = iSysConfigService.selectConfigByKey("sys.system.code");

            if (StringUtils.isEmpty(sysCode)) {
                log.error(I18nUtil.getMessage("ui.system.alter.noSetConfig.sysCode"));
                return sysCode;
            }

            options.put(CacheConstants.SYSTEM_CODE_KEY_PREFIX, sysCode);
        }

        return sysCode;

    }

    /**
     * 除了常量，尽量不要用这个方法，这个方法没有清理内存
     * !!!!使用CACHEUTILS
     * @param key
     * @return
     */
    public Object getKey(String key){
        return options.get(key);
    }

    /**
     * 除了常量，尽量不要用这个方法，这个方法没有清理内存
     * !!!!使用CACHEUTILS
     * @param key
     * @return
     */
    public Object setKey(String key, Object object){
        return  options.put(key, object);
    }

    /**
     * 移除缓存key
     * @param key
     */
    public void removeKey(String key){
        options.remove(key);
    }
}
