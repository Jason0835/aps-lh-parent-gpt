package com.zlt.aps.gdyy.engine.mapper;

import com.zlt.aps.gdyy.engine.vo.GdyySpecifyMachineVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 钢带压延定点机台数据Mapper
 *
 * @Author steve
 * @Date 2025-2-17 20:39:15
 * @Version 1.0
 */
public interface GdyyEngineSpecifyMachineMapper {


    /**
     * 钢带压延定点机台表
     *
     * @return
     */
    List<GdyySpecifyMachineVo> selectGdyySpecifyMachineList(@Param("jobType") String jobType);
}
