package com.zlt.aps.itf.mes.mapper;

import com.zlt.aps.itf.vo.LhDayPlanQtyVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 硫化排程结果查询Mapper（APS数据源）
 * <p>用于查询APS库的T_LH_SCHEDULE_RESULT，不使用类级@DS注解，
 * 数据源由调用方通过DynamicDataSourceContextHolder.push(DataSource.APS)切换。</p>
 *
 * @author zlt
 */
@Mapper
public interface LhScheduleResultQueryMapper {

    /**
     * 按工厂+完成日期+物料编码列表+示方类型，汇总硫化排程结果当日计划量。
     * <p>当日计划量 = 班次3计划量 + 班次4计划量 + 班次5计划量（夜/早/中三班）。</p>
     * <p>匹配维度：物料编码 + 示方类型（按CLASS3_LH_TYPE分组，同一物料同一施工阶段三班示方类型一致）。</p>
     * <p>查不到的物料+示方类型组合，由调用方按日计划量=0处理。</p>
     *
     * @param factoryCode 工厂编码
     * @param finishDate 完成日期（对应排程结果表的SCHEDULE_DATE）
     * @param materialCodes 物料编码列表
     * @return 日计划量列表
     */
    List<LhDayPlanQtyVo> sumDayPlanQtyByFinishDate(@Param("factoryCode") String factoryCode,
                                                   @Param("finishDate") Date finishDate,
                                                   @Param("materialCodes") List<String> materialCodes);
}
