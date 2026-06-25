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

    /**
     * 批量更新完成状态（仅更新finish_status、update_by、update_time）
     *
     * @param list 需要更新的数据列表（必须包含id和finishStatus）
     * @return 更新记录数
     */
    int batchUpdateFinishStatus(@Param("list") List<LhMoldAlterPlanFinish> list);

}
