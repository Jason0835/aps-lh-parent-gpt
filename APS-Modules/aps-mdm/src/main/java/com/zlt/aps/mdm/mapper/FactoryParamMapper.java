package com.zlt.aps.mdm.mapper;

import com.zlt.aps.mdm.api.domain.dto.ProductBrandDto;
import com.zlt.aps.mdm.api.domain.entity.FactoryParam;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryParamMapper.java
 * 描    述：系统参数（排产设定）Mapper接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Mapper
public interface FactoryParamMapper extends CommBaseMapper<FactoryParam> {

    /**
     * 获取分厂排产设定数据
     *
     * @param entity
     * @return
     */
    List<FactoryParam> getFacParamList(FactoryParam entity);

    /**
     * 获取品牌信息
     *
     * @param brandNameList 品牌名称
     * @return
     */
    List<ProductBrandDto> getParamByBrandList(@Param("brandNameList") List<String> brandNameList);
}
