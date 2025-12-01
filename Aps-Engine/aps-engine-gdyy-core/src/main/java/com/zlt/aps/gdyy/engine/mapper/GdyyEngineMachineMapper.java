package com.zlt.aps.gdyy.engine.mapper;

import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import com.zlt.aps.gdyy.engine.vo.GdyyMachineRollMappingVo;

import java.util.List;

/**
 * 钢带压延机台数据Mapper
 *
 * @Author steve
 * @Date 2025-2-17 20:39:15
 * @Version 1.0
 */
public interface GdyyEngineMachineMapper {

    /**
     * 加载钢压大卷与机台的映射表
     * @return 结果
     */
    List<GdyyMachineRollMappingVo> selectGdyyMachineRollMappingList();

    List<GdyyMachineInfo> listGdyyMachine();
}
