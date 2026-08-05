package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 钢丝圈排程结果Mapper接口
 *
 * <p>常规CRUD由MyBatisPlus提供，本接口仅声明特殊SQL。</p>
 *
 * <p>对齐胎圈 {@code TqScheduleResultMapper}，提供行锁查询和不可编辑状态校验方法，
 * 支撑人工操作门面与自动滚动事务内的并发控制。</p>
 *
 * @author APS
 */
@Mapper
public interface GsqScheduleResultMapper extends BaseMapper<GsqScheduleResult> {

    /**
     * 按主键集合加行锁查询（SELECT ... FOR UPDATE）。
     *
     * <p>对齐胎圈 selectBatchIdsForUpdate，在人工操作门面短事务和自动滚动事务内调用，
     * 保证调量/插单/转机台/删除期间其他事务无法修改这些行。</p>
     *
     * @param ids 主键集合
     * @return 加锁后的排程结果列表
     */
    @Select({"<script>",
            "SELECT * FROM T_GSQ_SCHEDULE_RESULT WHERE ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "FOR UPDATE",
            "</script>"})
    List<GsqScheduleResult> selectBatchIdsForUpdate(@Param("ids") List<Long> ids);

    /**
     * 查询指定ID集合中处于"已发布(1)/发布中(2)"状态的记录数。
     *
     * <p>对齐胎圈 {@code TqScheduleResultMapper.isReleasingOrTimeoutByIds}，
     * 用于人工操作前校验目标记录是否处于不可编辑状态（已发布或发布中）。</p>
     *
     * <p>钢丝圈 IS_RELEASE 取值：0-未发布 1-已发布 2-发布中 3-超时失败；
     * 其中 0/3 可人工编辑，1/2 不可编辑。</p>
     *
     * @param ids 排程结果ID数组
     * @return 符合不可编辑状态的记录数；大于0表示存在不可人工操作的记录
     */
    @Select({"<script>",
            "SELECT COUNT(1) FROM T_GSQ_SCHEDULE_RESULT",
            "WHERE IS_DELETE = 0 AND IS_RELEASE IN ('1','2') AND ID IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"})
    int isReleasingOrTimeoutByIds(@Param("ids") Long[] ids);

    /**
     * 查询某工厂在指定排程日期下的最大批次号。
     *
     * <p>直接从排程结果表 {@code T_GSQ_SCHEDULE_RESULT} 取 MAX(BATCH_NO)，
     * 与导出/导入操作的数据同源同工厂，保证批次号与真实数据一致。
     * 当 {@code factoryCode} 为空时忽略工厂条件（返回该日期全厂最大批次）。</p>
     *
     * @param factoryCode  分厂编码（可为空）
     * @param scheduleDate 排程日期（yyyy-MM-dd）
     * @return 最大批次号；结果表无数据时返回 null
     */
    @Select("SELECT MAX(BATCH_NO) FROM T_GSQ_SCHEDULE_RESULT " +
            "WHERE IS_DELETE = 0 AND SCHEDULE_DATE = STR_TO_DATE(#{scheduleDate}, '%Y-%m-%d') " +
            "AND (#{factoryCode} IS NULL OR FACTORY_CODE = #{factoryCode})")
    String getMaxBatchNo(@Param("factoryCode") String factoryCode, @Param("scheduleDate") String scheduleDate);
}
