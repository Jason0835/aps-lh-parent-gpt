package com.zlt.aps.mdm.mapper;

import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmModelInfoMapper.java
 * 描    述：模具信息Mapper接口
 *@author zlt
 *@date 2025-02-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MdmModelInfoEntityMapper extends CommBaseMapper<MdmModelInfo> {

    /**
     * 根据模具编号查询
     *
     * @param saveList 模具编号
     * @return 结果
     */
    List<MdmModelInfo> selectByUniqueKeyList(@Param("list") List<MdmModelInfo> saveList);
}
