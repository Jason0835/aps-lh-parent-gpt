package com.zlt.aps.tm.engine.mapper;


import com.zlt.aps.tm.engine.vo.TmLossVo;

import java.util.List;

/**
 * 损耗率mapper
 */
public interface TmEngineLossMapper {

    /**
     * 获得损耗率类别
     * @return
     */
    List<TmLossVo> listLossRate();
}
