package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MdmMouldCleanPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * APS模具清洗预警计划Mapper
 *
 * @author zlt
 * @since 2025/12/25
 */
@Mapper
public interface MdmMouldCleanPlanEntityMapper extends BaseMapper<MdmMouldCleanPlan> {

    /**
     * 根据唯一键查询已存在的数据
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<MdmMouldCleanPlan> selectByUniqueKeyList(@Param("list") List<MdmMouldCleanPlan> list);

}
