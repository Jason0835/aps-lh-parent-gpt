package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.monthplan.api.domain.entity.MdmPersonLevel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmPersonLevelMapper.java
 * 描    述：成型机人员档配置Mapper接口
 *@author hsc
 *@date 2025-02-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hsc
 *     修改内容：...
 */
@Mapper
public interface MdmPersonLevelMapper extends BaseMapper<MdmPersonLevel> {

    /**
     * 查询成型机人员档配置列表
     *
     * @param mdmPersonLevel 成型机人员档配置
     * @return 成型机人员档配置集合
     */
    public List<MdmPersonLevel> selectMdmPersonLevelList(MdmPersonLevel mdmPersonLevel);
}
