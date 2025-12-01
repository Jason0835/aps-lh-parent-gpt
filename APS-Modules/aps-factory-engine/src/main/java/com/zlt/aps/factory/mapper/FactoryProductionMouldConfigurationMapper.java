package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.dto.VulcanizingProductInfoDto;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMouldConfigurationMapper.java
 * 描    述：模具正在生产的品种Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@Mapper
public interface FactoryProductionMouldConfigurationMapper extends CommBaseMapper<ProductionMouldConfiguration> {
    /**
     * 根据硫化排程日获取分厂的硫化品种
     *
     * @param factoryCode     分厂
     * @param vulcanizingDate 硫化排程日
     * @return
     */
    List<VulcanizingProductInfoDto> getVulcanizingProduct(@Param("factoryCode") String factoryCode, @Param("vulcanizingDate") Date vulcanizingDate);
}
