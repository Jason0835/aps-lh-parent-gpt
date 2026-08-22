package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 硫化结构转产配置 Mapper（MyBatis-Plus）。
 *
 * <p>使用硫化模块前缀区分月计划引擎同名 Mapper，避免统一日模具计算引入
 * {@code aps-engine-mp-core} 后两个接口注册成同一 Bean 名。</p>
 * <p>本 Mapper 供硫化排程基础数据阶段按工厂、年月、排产版本、计划类型和结构名称读取
 * {@code T_MP_STRUCTURE_ALLOCATION}，不承载结构收尾对齐业务判断。</p>
 *
 * @author APS
 */
@Mapper
public interface LhMpStructureAllocationMapper extends BaseMapper<MpStructureAllocation> {
}
