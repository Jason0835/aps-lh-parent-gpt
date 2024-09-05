package com.zlt.aps.tm.engine.mapper;


import com.zlt.aps.tm.engine.vo.TmGlueOrderVo;

import java.util.List;

/**
 * 胶料顺序mapper
 */
public interface TmEngineGlueMapper {

    /**
     * 查询胶料顺序序号列表
     * @return
     */
    List<TmGlueOrderVo> listGlueSeq();
}
