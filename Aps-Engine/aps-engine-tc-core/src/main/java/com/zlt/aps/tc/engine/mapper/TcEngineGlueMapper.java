package com.zlt.aps.tc.engine.mapper;


import com.zlt.aps.tc.engine.vo.TcGlueOrderVo;

import java.util.List;

/**
 * 胶料顺序mapper
 */
public interface TcEngineGlueMapper {

    /**
     * 查询胶料顺序序号列表
     * @return
     */
    List<TcGlueOrderVo> listGlueSeq();
}
