package com.zlt.aps.tc.engine.service;

import java.util.Map;

public interface TcEngineMachineService {


    /**
     * 获得胎侧代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    Map<String, String> getMouthPlateMachineMap();
}
