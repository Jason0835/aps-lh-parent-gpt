package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 胎圈排程结果Mapper接口
 *
 * <p>对齐胎面 TmScheduleResultMapper，提供行锁查询方法支撑自动滚动事务内调量。</p>
 *
 * @author APS
 */
@Mapper
public interface TqScheduleResultMapper extends BaseMapper<TqScheduleResult> {

    /**
     * 按主键集合加行锁查询（SELECT ... FOR UPDATE）。
     *
     * <p>对齐胎面 selectBatchIdsForUpdate，在自动滚动事务内调用，
     * 保证调量期间其他事务无法修改这些行。</p>
     *
     * @param ids 主键集合
     * @return 加锁后的排程结果列表
     */
    @Select({"<script>",
            "SELECT * FROM T_TQ_SCHEDULE_RESULT WHERE ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "FOR UPDATE",
            "</script>"})
    List<TqScheduleResult> selectBatchIdsForUpdate(@Param("ids") List<Long> ids);

    /**
     * 查询指定ID集合中处于"发布中(2)/已发布(3)/已下推(4)"状态的记录数。
     *
     * <p>对齐胎面 TmScheduleResultMapper.isReleasingOrTimeoutByIds，
     * 用于人工操作前校验目标记录是否处于不可编辑状态（发布中或已发布/已下推）。</p>
     *
     * @param ids 排程结果ID数组
     * @return 符合不可编辑状态的记录数；大于0表示存在不可人工操作的记录
     */
    @Select({"<script>",
            "SELECT COUNT(1) FROM T_TQ_SCHEDULE_RESULT",
            "WHERE IS_DELETE = 0 AND RELEASE_STATUS IN ('2','3','4') AND ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"})
    int isReleasingOrTimeoutByIds(@Param("ids") Long[] ids);

    /**
     * 查询某工厂在指定排程日期下的最大批次号。
     *
     * <p>直接从排程结果表 {@code T_TQ_SCHEDULE_RESULT} 取 MAX(BATCH_NO)，
     * 与导出/导入操作的数据同源同工厂，保证批次号与真实数据一致。
     * 当 {@code factoryCode} 为空时忽略工厂条件（返回该日期全厂最大批次）。</p>
     *
     * @param factoryCode  分厂编码（可为空）
     * @param scheduleDate 排程日期（yyyy-MM-dd）
     * @return 最大批次号；结果表无数据时返回 null
     */
    @Select("SELECT MAX(BATCH_NO) FROM T_TQ_SCHEDULE_RESULT " +
            "WHERE IS_DELETE = 0 AND SCHEDULE_DATE = STR_TO_DATE(#{scheduleDate}, '%Y-%m-%d') " +
            "AND (#{factoryCode} IS NULL OR FACTORY_CODE = #{factoryCode})")
    String getMaxBatchNo(@Param("factoryCode") String factoryCode, @Param("scheduleDate") String scheduleDate);
}
