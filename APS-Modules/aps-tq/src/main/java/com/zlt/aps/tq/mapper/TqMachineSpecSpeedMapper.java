package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;

import java.util.List;

public interface TqMachineSpecSpeedMapper extends BaseMapper<TqMachineSpecSpeed> {

    List<TqMachineSpecSpeed> listMachineSpecSpeed(TqMachineSpecSpeed machineSpecSpeed);

    int checkUnique(TqMachineSpecSpeed machineSpecSpeed);

    void mergeSql(List<TqMachineSpecSpeed> list);

    void deleteAll();
}
