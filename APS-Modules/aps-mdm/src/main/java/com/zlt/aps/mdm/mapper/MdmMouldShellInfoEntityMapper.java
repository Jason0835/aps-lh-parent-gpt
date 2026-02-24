package com.zlt.aps.mdm.mapper;

import com.zlt.aps.mdm.api.domain.entity.MdmMouldShellInfo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMouldShellInfoMapper.java
 * 描    述：模壳台账Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@Mapper
public interface MdmMouldShellInfoEntityMapper extends CommBaseMapper<MdmMouldShellInfo> {

    /**
     * 根据唯一键查询
     *
     * @param uniqueKeyList 唯一键列表
     * @return 列表
     */
    List<MdmMouldShellInfo> selectByUniqueKeyList(@Param("list") List<MdmMouldShellInfo> uniqueKeyList);
}
