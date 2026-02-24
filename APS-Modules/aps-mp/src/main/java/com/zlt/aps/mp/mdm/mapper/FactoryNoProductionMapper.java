package com.zlt.aps.mp.mdm.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryNoProductionMapper.java
 * 描    述：基础数据-分厂不排产Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-21
 */
@Mapper
public interface FactoryNoProductionMapper extends CommBaseMapper<FactoryNoProduction> {

    /**
     * 查询分厂不排产设定
     *
     * @param factoryNoProduction
     * @return
     */
    List<FactoryNoProduction> selectFactoryNoProductionList(FactoryNoProduction factoryNoProduction);
}
