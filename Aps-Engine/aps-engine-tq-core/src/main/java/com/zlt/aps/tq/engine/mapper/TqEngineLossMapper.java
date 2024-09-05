package com.zlt.aps.tq.engine.mapper;


import com.zlt.aps.tq.engine.vo.TqLossVo;

import java.util.List;

/**
 * 损耗率mapper
 */
public interface TqEngineLossMapper {

    /**
     * 获得损耗率类别
     * @return
     */
    List<TqLossVo> listLossRate();
}
