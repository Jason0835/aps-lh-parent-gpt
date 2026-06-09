package com.zlt.aps.dj.engine.mapper;


import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.engine.vo.DjSpecifyMachineVo;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DjEngineMachineMapper {

    /**
     * 查询垫胶定点机台信息
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    List<DjSpecifyMachineVo> listNcSpecifyMachine(@Param("jobType") String jobType);

    /**
     * 查询垫胶机台信息
     *
     * @return 结果
     */
    List<DjMachineInfo> listNcMachine();
}
