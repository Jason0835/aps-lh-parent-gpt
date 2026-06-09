package com.zlt.aps.dj.engine.mapper;


import java.util.List;

import com.zlt.aps.dj.engine.vo.DjGlueOrderVo;

/**
 * 胶料顺序mapper
 */
public interface DjEngineGlueMapper {

    /**
     * 查询胶料顺序序号列表
     * @return
     */
    List<DjGlueOrderVo> listGlueSeq();
}
