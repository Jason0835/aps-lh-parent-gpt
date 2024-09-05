package com.ruoyi.common.constant;

/**
 * 服务名称
 * 
 * @author ruoyi
 */
public class ServiceNameConstants
{
    /**
     * 认证服务的serviceid
     */
    @Deprecated()
    public static final String AUTH_SERVICE = "ruoyi-auth";

    /**
     * 系统模块的serviceid
     */
    @Deprecated
    public static final String SYSTEM_SERVICE = "ruoyi-system";

    /**
     * 需要经过网关的服务都用这个serviceid
     */
    public static final String GATEWAY_SERVICE = "ruoyi-gateway";

    /**
     * 生产排程同步数据服务serviceid
     */
    public static final String APS_MPS_SERVICE = "aps-mps";

    /**
     * 文件服务的serviceid
     */
    @Deprecated
    public static final String FILE_SERVICE = "ruoyi-file";
}
