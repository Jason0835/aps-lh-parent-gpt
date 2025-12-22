package com.zlt.sync.povo;

import lombok.Data;

/**
 * 队列配置信息
 */
@Data
public class QueueDataVO {
    private String queue;
    private String exchange;
    private String key;
}
