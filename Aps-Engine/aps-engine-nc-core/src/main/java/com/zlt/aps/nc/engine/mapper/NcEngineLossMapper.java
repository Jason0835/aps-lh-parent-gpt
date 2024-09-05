package com.zlt.aps.nc.engine.mapper;


import com.zlt.aps.nc.engine.vo.NcLossVo;

import java.util.List;

/**
 * 损耗率mapper
 */
public interface NcEngineLossMapper {

    /**
     * 获得损耗率类别
     * @return
     */
    List<NcLossVo> listLossRate();
}
