package com.zlt.aps.dj.engine.service;

import java.util.List;
import java.util.Map;

import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;

public interface DjEngineMachineService {

    /**
     * 获得垫胶代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获得垫胶机台列表
     *
     * @return 结果
     */
    List<DjMachineInfo> listDjMachine();
}
