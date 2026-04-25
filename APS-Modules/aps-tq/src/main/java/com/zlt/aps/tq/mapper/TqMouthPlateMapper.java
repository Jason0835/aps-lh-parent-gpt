package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqMouthPlate;

import java.util.List;

public interface TqMouthPlateMapper extends BaseMapper<TqMouthPlate> {

    List<TqMouthPlate> selectMouthPlateWithMachineInfo(TqMouthPlate mouthPlate);

    int checkUnique(TqMouthPlate mouthPlate);

    void mergeSql(List<TqMouthPlate> list);

    void deleteAll();
}
