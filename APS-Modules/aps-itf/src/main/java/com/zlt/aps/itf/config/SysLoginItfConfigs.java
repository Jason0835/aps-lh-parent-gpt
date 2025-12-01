package com.zlt.aps.itf.config;

import com.zlt.aps.itf.vo.SysLoginItfVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 接口定义
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "syslogin")
@Data
public class SysLoginItfConfigs {

    private List<SysLoginItfVo> itf;

    /**
     * 根据系统Key,获取系统登录定义
     * @param sysKey
     * @return
     */
    public SysLoginItfVo getBySysKey(String sysKey) {

        for (SysLoginItfVo dataVO : itf) {
            if (sysKey.equals(dataVO.getSysKey())) {
                return dataVO;
            }
        }
        return null;
    }

    /**
     * 根据Token获取系统登录定义
     * @param token
     * @return
     */
    public SysLoginItfVo getBytoken(String token) {

        for (SysLoginItfVo dataVO : itf) {
            if (token.equals(dataVO.getToken())) {
                return dataVO;
            }
        }
        return null;
    }
}
