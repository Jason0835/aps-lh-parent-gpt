package com.zlt.aps.nc.engine.mapper;


import com.zlt.aps.nc.engine.vo.NcGlueOrderVo;

import java.util.List;

/**
 * 胶料顺序mapper
 */
public interface NcEngineGlueMapper {

    /**
     * 查询胶料顺序序号列表
     * @return
     */
    List<NcGlueOrderVo> listGlueSeq();
}
