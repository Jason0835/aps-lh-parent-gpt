package com.zlt.aps.itf.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

@Data
public class SysLoginItfVo extends BaseEntity {

    /**
     * 系统名称key
     */
    private String sysKey;

    /**
     * 系统路径
     */
    private String path;

    /**
     * 系统token
     */
    private String token;

    /**
     * 安全IP
     */
    private String ip;

    /**
     * 是否需要安全IP检查
     */
    private boolean needIpCheck;
}
