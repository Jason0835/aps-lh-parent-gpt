package com.zlt.aps.cd15.engine.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;

import java.util.List;

/**
 * 15°裁断机台信息Mapper接口
 *
 * @author zlt
 * @date 2025-2-13
 */
public interface Cd15EngineMachineMapper {

    /**
     * 查询15°裁断机台信息列表
     *
     * @param machineInfo 15°裁断机台信息
     * @return 15°裁断机台信息集合
     */
    public List<Cd15MachineInfo> selectNotOutTwoMachineList(Cd15MachineInfo machineInfo);
}
