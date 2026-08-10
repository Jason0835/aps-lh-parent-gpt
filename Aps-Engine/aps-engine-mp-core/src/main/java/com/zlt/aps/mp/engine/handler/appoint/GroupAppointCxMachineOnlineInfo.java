package com.zlt.aps.mp.engine.handler.appoint;

import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.GroupAppointProductionInfoVo;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 机台指定后结构最早上机日
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260807
 */
@Getter
public class GroupAppointCxMachineOnlineInfo implements Serializable {
    /**
     * 成型机台信息
     */
    private CxMachineBaseInfoVo cxMachineInfo;
    /**
     * 最早切换结构日：即最早上机的后结构日
     */
    private Integer earliestChangeGroupDay;
    /**
     * 所有指定配置
     */
    private List<GroupAppointProductionInfoVo> allAppointConfiguration;

    /**
     * 构造函数
     *
     * @param cxMachineInfo           成型机台
     * @param earliestChangeGroupDay  后结构最早上机日
     * @param allAppointConfiguration 所有该机台指定配置
     */
    public GroupAppointCxMachineOnlineInfo(CxMachineBaseInfoVo cxMachineInfo, Integer earliestChangeGroupDay, List<GroupAppointProductionInfoVo> allAppointConfiguration) {
        this.cxMachineInfo = cxMachineInfo;
        this.earliestChangeGroupDay = earliestChangeGroupDay;
        this.allAppointConfiguration = allAppointConfiguration;
    }
}
