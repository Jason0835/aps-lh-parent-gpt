package com.zlt.aps.tc.engine.mapper;


import com.zlt.aps.tc.engine.vo.TcLossVo;

import java.util.List;

/**
 * 损耗率mapper
 */
public interface TcEngineLossMapper {

    /**
     * 获得损耗率类别
     * @return
     */
    List<TcLossVo> listLossRate();
}
