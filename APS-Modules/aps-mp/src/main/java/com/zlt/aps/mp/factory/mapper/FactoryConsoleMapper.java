package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanVersionVo;
import com.zlt.aps.mp.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.mp.factory.dto.FactoryProductionPlanVersionDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 分厂排产控制台业务SQL接口定义类
 *
 * @author ZLT
 * @date 20251201
 */
@Mapper
public interface FactoryConsoleMapper {
    /**
     * 根据查询条件，获取分厂对应的生产计划版本记录列表
     *
     * @param queryCondition 查询条件信息
     * @return
     */
    List<FactoryProductionPlanVersionDto> getProductionVersionList(FactoryProductionPlanVo queryCondition);

    /**
     * 根据查询条件，获取分厂还未选择的需求计划版本列表
     *
     * @param queryCondition 查询条件信息
     * @return
     */
    List<FactoryMonthPlanVersionVo> getNoSelectedVersionList(FactoryProductionPlanVo queryCondition);

}
