package com.zlt.aps.cd90.api.domain.vo;

import lombok.Data;

import java.util.Date;

/** CD90定时滚动排程检查请求。 */
@Data
public class Cd90RollingCheckRequest {

    /** 本次Job检查时间，为空时使用服务端当前时间。 */
    private Date triggerTime;

    /** 指定工厂编码，为空时检查所有启用班次的工厂。 */
    private String factoryCode;
}
