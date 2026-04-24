package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;

import java.util.List;

public interface TqSpecifyMachineMapper extends BaseMapper<TqSpecifyMachine> {

    List<TqSpecifyMachine> listSpecifyMachine(TqSpecifyMachine entity);

    void mergeSql(List<TqSpecifyMachine> list);

    void deleteAllSpecifyMachine();
}
