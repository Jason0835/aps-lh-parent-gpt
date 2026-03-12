package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmWorkCalendarMapper.java
 * 描    述：工作日历Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
@Mapper
public interface MdmWorkCalendarEntityMapper extends CommBaseMapper<MdmWorkCalendar> {

    /**
     * 复制，将源分厂年月、工序复制到目标分厂年月、工序
     *
     * @param entity 源参数
     * @return 源参数
     */
    int copy(MdmWorkCalendar entity);

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    public List<String> selectMenuBtPermsByUserId(Long userId);
}
