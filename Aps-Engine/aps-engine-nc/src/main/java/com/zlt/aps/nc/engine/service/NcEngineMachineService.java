package com.zlt.aps.nc.engine.service;

import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;

import java.util.List;
import java.util.Map;

public interface NcEngineMachineService {

    /**
     * 获得内衬代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获得内衬机台列表
     *
     * @return 结果
     */
    List<NcMachineInfo> listNcMachine();
}
