package com.zlt.aps.maindata.mapper;


import com.zlt.aps.mp.api.domain.entity.RawMaterialRequirePlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawMaterialRequirePlanMapper.java
 * 描    述：原材料需求计划Mapper接口
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface RawMaterialRequirePlanEntityMapper extends CommBaseMapper<RawMaterialRequirePlan> {

    /**
     * 批量插入原材料需求计划
     * @param list 需求计划列表
     * @return 插入条数
     */
    int batchInsert(@Param("list") List<RawMaterialRequirePlan> list);

}
