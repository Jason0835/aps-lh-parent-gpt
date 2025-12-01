package com.zlt.aps.nc.engine.mapper;


import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.engine.vo.NcSpecifyMachineVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NcEngineMachineMapper {

    /**
     * 查询内衬定点机台信息
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    List<NcSpecifyMachineVo> listNcSpecifyMachine(@Param("jobType") String jobType);

    /**
     * 查询内衬机台信息
     *
     * @return 结果
     */
    List<NcMachineInfo> listNcMachine();
}
