package com.zlt.aps.dj.engine.mapper;


import java.util.List;

import com.zlt.aps.dj.engine.vo.DjLossVo;

/**
 * 损耗率mapper
 */
public interface DjEngineLossMapper {

    /**
     * 获得损耗率类别
     * @return
     */
    List<DjLossVo> listLossRate();
}
