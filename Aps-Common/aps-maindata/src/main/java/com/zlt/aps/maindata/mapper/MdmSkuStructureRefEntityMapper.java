package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmSkuStructureRefMapper.java
 * 描    述：SKU与结构关系Mapper接口
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
public interface MdmSkuStructureRefEntityMapper extends CommBaseMapper<MdmSkuStructureRef> {

    /**
     * 查询结构选择列表
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MdmSkuStructureRef> getStructureSelectList(MdmSkuStructureRef queryVO);

    /**
     * 查询SKU与结构关系列表
     * @param queryWrapper 查询对象
     * @return 结果
     */
    List<MdmSkuStructureRef> getMdmSkuStructureRefList(@Param("ew") QueryWrapper<MdmSkuStructureRef> queryWrapper);

    /**
     * 更新结构到物料
     * @param queryVO 查询条件
     * @return 结果
     */
    int updateStructureToMaterial(MdmSkuStructureRef queryVO);

    /**
     * 根据SKU与示方关系新增不存在的胎胚描述
     * @return 结果
     */
    int insertMainMaterialDesc4SkuConstructionRef(MdmSkuConstructionRef queryVO);
}
