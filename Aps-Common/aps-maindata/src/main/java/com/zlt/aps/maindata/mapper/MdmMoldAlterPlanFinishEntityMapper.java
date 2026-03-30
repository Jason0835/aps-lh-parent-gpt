package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmMoldAlterPlanFinish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具交替计划完成回报Mapper
 *
 * @author APS Team
 * @since 2026/03/29
 */
@Mapper
public interface MdmMoldAlterPlanFinishEntityMapper extends BaseMapper<MdmMoldAlterPlanFinish> {

    /**
     * 根据唯一键查询已存在的数据
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<MdmMoldAlterPlanFinish> selectByUniqueKeyList(@Param("list") List<MdmMoldAlterPlanFinish> list);

}
