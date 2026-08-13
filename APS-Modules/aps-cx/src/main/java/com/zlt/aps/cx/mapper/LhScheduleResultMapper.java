package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 硫化排程结果Mapper接口
 *
 * @author APS Team
 * @since 2.0.0
 */
@Mapper
public interface LhScheduleResultMapper extends BaseMapper<LhScheduleResult> {

    /**
     * 按排程日期查询
     */
    @Select("SELECT * FROM t_lh_schedule_result WHERE SCHEDULE_DATE = #{scheduleDate}  AND IS_DELETE = '0'")
    List<LhScheduleResult> selectByDate(@Param("scheduleDate") LocalDate scheduleDate);

    /**
     * 查询所有未完成的排程
     */
    @Select("SELECT * FROM t_lh_schedule_result WHERE  IS_DELETE = '0' ORDER BY SCHEDULE_DATE, MACHINE_ORDER")
    List<LhScheduleResult> selectAll();

    /**
     * 按主键ID批量查询硫化排程结果。
     *
     * @param ids 硫化排程结果ID集合
     * @return 硫化排程结果列表
     */
    @Select("<script>SELECT * FROM t_lh_schedule_result WHERE IS_DELETE = '0' AND ID IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<LhScheduleResult> selectByIds(@Param("ids") Collection<Long> ids);

    /**
     * 查询指定物料在给定日期之前最近的一条硫化排程记录（仅限当月范围内）
     * 用于补充延误物料的历史任务信息
     *
     * @param materialCode 物料编码
     * @param beforeDate   排程日期（查询此日期之前的记录）
     * @param firstDayOfMonth 当月1号（查询下限，不早于此日期）
     * @return 最近一条硫化排程记录，不存在则返回null
     */
    @Select("SELECT * FROM t_lh_schedule_result WHERE MATERIAL_CODE = #{materialCode} AND SCHEDULE_DATE >= #{firstDayOfMonth} AND SCHEDULE_DATE < #{beforeDate} AND IS_DELETE = '0' ORDER BY SCHEDULE_DATE DESC LIMIT 1")
    LhScheduleResult selectLatestBeforeDate(@Param("materialCode") String materialCode, @Param("beforeDate") LocalDate beforeDate, @Param("firstDayOfMonth") LocalDate firstDayOfMonth);
}
