package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具交替计划Mapper
 *
 * @author APS Team
 * @since 2026/04/01
 */
@Mapper
public interface LhMouldChangePlanEntityMapper extends CommBaseMapper<LhMouldChangePlan> {

    /**
     * 批量插入模具交替计划
     *
     * @param list 模具交替计划列表
     * @return 插入记录数
     */
    int insertBatch(@Param("list") List<LhMouldChangePlan> list);

}
