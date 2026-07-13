package com.zlt.aps.cd15.api.domain.vo;

import lombok.Data;

import java.util.Date;

/** CD15定时滚动排程检查请求。 */
@Data
public class Cd15RollingCheckRequest {

    /** 本次Job检查时间，为空时使用服务端当前时间。 */
    private Date triggerTime;

    /** 指定工厂编码，为空时检查有滚动参数或近期排程结果的工厂。 */
    private String factoryCode;
}