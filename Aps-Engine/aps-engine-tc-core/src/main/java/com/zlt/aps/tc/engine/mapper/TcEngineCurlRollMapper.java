package com.zlt.aps.tc.engine.mapper;


import java.util.List;

import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;

/**
 * 胎侧卷曲设置mapper
 */
public interface TcEngineCurlRollMapper {

    /**
     * 查询胎侧卷曲设置列表：胎面
     * @return
     */
    List<TcCurlRoll> getTcCurlRollList();
}
