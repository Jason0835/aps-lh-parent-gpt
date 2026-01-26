package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuConstructionRefMapper.java
 * 描    述：SKU与施工（示方书）关系Mapper接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MdmSkuConstructionRefEntityMapper extends CommBaseMapper<MdmSkuConstructionRef> {

    /**
     * 更新主物料(胎胚号)到物料信息表
     *
     * @return 结果
     */
    int updateMainMaterialDescToMaterialInfo(MdmSkuConstructionRef queryVO);
}
