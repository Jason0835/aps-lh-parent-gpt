package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.LocationChannelConfiguration;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LocationChannelConfigurationMapper.java
 * 描    述：库位类别渠道品牌配置Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
@Mapper
public interface LocationChannelConfigurationMapper extends CommBaseMapper<LocationChannelConfiguration> {

    /**
     * 删除对应分厂、业务类型、层级的数据
     *
     * @param factoryCodeList  分厂列表
     * @param businessTypeList 业务类型列表
     * @param hierarchyList    层级列表
     * @return 数量
     */
    int deleteByParamList(@Param("factoryCodeList") List<String> factoryCodeList,
                          @Param("businessTypeList") List<String> businessTypeList,
                          @Param("hierarchyList") List<Integer> hierarchyList);
}
