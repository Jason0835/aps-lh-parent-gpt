package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 结构转产配置 Mapper（MyBatis-Plus）。
 *
 * <p>供硫化排程基础数据阶段按工厂、年月、排产版本、计划类型和结构名称读取
 * {@code T_MP_STRUCTURE_ALLOCATION}，不承载结构收尾对齐业务判断。</p>
 *
 * @author APS
 */
@Mapper
public interface MpStructureAllocationMapper extends BaseMapper<MpStructureAllocation> {
}
