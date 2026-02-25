package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.MdmHoliday;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmHolidayMapper.java
 * 描    述：0150基础数据_节假日配置Mapper接口
 *@author zlt
 *@date 2026-01-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MdmHolidayEntityMapper extends CommBaseMapper<MdmHoliday> {

    /**
     * 根据年查询
     *
     * @param year 年
     * @return 结果
     */
    List<MdmHoliday> selectByYear(@Param("year") Integer year);
}
