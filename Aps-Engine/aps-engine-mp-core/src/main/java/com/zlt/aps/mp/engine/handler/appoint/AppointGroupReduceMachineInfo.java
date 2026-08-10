package com.zlt.aps.mp.engine.handler.appoint;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在机结构减机台信息对象
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260807
 */
@Getter
public class AppointGroupReduceMachineInfo implements Serializable {
    /**
     * 在机分组-TBR 结构
     */
    private String groupName;
    /**
     * 强行下机的机台信息
     */
    private List<OnlineGroupAppointCxMachineInfo> forceOffMachineInfo;
    /**
     * 在机分组-在产机台分配信息
     */
    private List<CxMachineAllocationPlanHelper> onlineCxMachineInfo;

    /**
     * 构造函数
     *
     * @param groupName
     * @param forceOffMachineInfo
     */
    public AppointGroupReduceMachineInfo(String groupName, List<OnlineGroupAppointCxMachineInfo> forceOffMachineInfo, List<CxMachineAllocationPlanHelper> onlineCxMachineInfo) {
        this.groupName = groupName;
        this.forceOffMachineInfo = forceOffMachineInfo;
        this.onlineCxMachineInfo = onlineCxMachineInfo;
    }

    /**
     * 获取在机分组(TBR-结构)，在产机台中途需要强制下机的在产机台信息
     *
     * @return
     */
    public List<OnlineGroupAppointCxMachineInfo> getForceOfflineCxMachine(Context context) {
        if (CollectionUtils.isEmpty(forceOffMachineInfo)) {
            return Collections.emptyList();
        }
        List<OnlineGroupAppointCxMachineInfo> forceOffCxMachineList = forceOffMachineInfo.stream().filter(single -> single.isForceOffLine(context)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(forceOffCxMachineList)) {
            return Collections.emptyList();
        }
        return forceOffCxMachineList;
    }


}
