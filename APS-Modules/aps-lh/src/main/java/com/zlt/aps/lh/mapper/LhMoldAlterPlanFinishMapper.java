package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具交替计划完成回报Mapper
 *
 * @author APS Team
 * @since 2026/04/09
 */
@Mapper
public interface LhMoldAlterPlanFinishMapper extends CommBaseMapper<LhMoldAlterPlanFinish> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<LhMoldAlterPlanFinish> selectByUniqueKeyList(@Param("list") List<LhMoldAlterPlanFinish> list);

}
