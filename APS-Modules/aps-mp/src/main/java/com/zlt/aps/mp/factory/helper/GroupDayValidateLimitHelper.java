package com.zlt.aps.mp.factory.helper;

import com.zlt.aps.mp.engine.handler.embryobalance.GroupCxMachineConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * 分组验证-限制信息-辅助类
 *
 * @author zlt
 * @date 2026-08-19
 */
@Slf4j
@Data
public class GroupDayValidateLimitHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer day;
    /**
     * 总的硫化机台数
     */
    private Integer sumUsedLhMachines;
    /**
     * 额外处理机台信息
     */
    private Integer extraLhMachines;
    /**
     * 分配的机台信息
     */
    private List<GroupCxMachineConfiguration> cxMachineConfigurationList;
}
