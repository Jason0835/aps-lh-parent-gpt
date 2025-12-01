package com.zlt.aps.gdyy.engine.mapper;


import java.util.List;

import com.zlt.aps.gdyy.engine.vo.GdyyOriginalLineSpecVo;

public interface GdyyEngineOriginlLineSpecMapper {

    /**
     * 查询钢丝压延原丝规格
     * @param scheduleDate
     * @return
     */
    List<GdyyOriginalLineSpecVo> listGdyyOriginalLineSpec();

}
