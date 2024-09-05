package com.ruoyi.system.api.domain;

import lombok.Getter;
import lombok.Setter;

/***
 * session 会话的有效期
 */
@Getter
@Setter
public class SessionBody {
    private String sessionId;
    private long expireTime;
    private String accessToken;
}
