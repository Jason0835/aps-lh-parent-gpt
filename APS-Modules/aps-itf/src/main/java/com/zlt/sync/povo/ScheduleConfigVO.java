package com.zlt.sync.povo;

import lombok.Data;

import java.util.List;

/**
 * 定时调度，视图对象
 *  需要特殊定义的情况
 */
@Data
public class ScheduleConfigVO {
    private String name; //定时调度定义名称
    private String cron; //定时调度间隔配置
    private String desc; //集群定时执行描述
    private List<String> syncKeys; //本配置涉及的接口syncKey
}
