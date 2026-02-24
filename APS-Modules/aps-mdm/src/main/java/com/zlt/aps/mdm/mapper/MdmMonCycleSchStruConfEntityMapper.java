package com.zlt.aps.mdm.mapper;

import com.zlt.aps.mdm.api.domain.entity.MdmCycleSchStruConf;
import com.zlt.aps.mdm.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMonCycleSchStruConfMapper.java
 * 描    述：月周期排产结构配置Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
@Mapper
public interface MdmMonCycleSchStruConfEntityMapper extends CommBaseMapper<MdmMonCycleSchStruConf> {

    /**
     * 查询月周期排产结构配置
     *
     * @param mdmCycleSchStruConf 查询参数
     * @return 结果
     */
    List<MdmMonCycleSchStruConf> selectMonthCycleSchStruConf(MdmCycleSchStruConf mdmCycleSchStruConf);
}
