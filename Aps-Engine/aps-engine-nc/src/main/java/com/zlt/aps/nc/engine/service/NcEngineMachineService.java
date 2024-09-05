package com.zlt.aps.nc.engine.service;

import java.util.Map;

public interface NcEngineMachineService {

    /**
     * 获得内衬代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);
}
