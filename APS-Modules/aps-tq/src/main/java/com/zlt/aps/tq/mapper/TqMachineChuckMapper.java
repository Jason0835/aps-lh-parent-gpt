package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;

import java.util.List;

public interface TqMachineChuckMapper extends BaseMapper<TqMachineChuck> {

    List<TqMachineChuck> listMachineChuck(TqMachineChuck entity);

    int checkUnique(TqMachineChuck machineChuck);

    void mergeSql(List<TqMachineChuck> list);

    void deleteAllMachineChuck();
}
