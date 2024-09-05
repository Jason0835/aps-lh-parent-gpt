package com.zlt.aps.gsq.engine.mapper;


import com.zlt.aps.gsq.engine.vo.GsqLossVo;

import java.util.List;

/**
 * 损耗率mapper
 */
public interface GsqEngineLossMapper {

    /**
     * 获得损耗率类别
     * @return
     */
    List<GsqLossVo> listLossRate();
}
