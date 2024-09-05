package com.zlt.aps.tm.engine.mapper;


import java.util.List;

import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;

/**
 * 胎面卷曲设置mapper
 */
public interface TmEngineCurlRollMapper {

    /**
     * 查询胎面卷曲设置列表：胎面
     * @return
     */
    List<TmCurlRoll> getTmCurlRollList();
}
