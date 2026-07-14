package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果表 Mapper接口
 */
@Mapper
public interface TmScheduleResultMapper extends CommBaseMapper<TmScheduleResult> {

    /**
     * 按主键集合加行锁读取排程结果。
     *
     * <p>人工滚动与发布状态修改共用该数据库行锁，避免状态校验后被并发修改。</p>
     *
     * @param ids 排程结果主键集合
     * @return 已加行锁的排程结果
     */
    List<TmScheduleResult> selectBatchIdsForUpdate(@Param("ids") List<Long> ids);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录数
     * @param ids id数组
     * @return 符合条件的记录数
     */
    int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 按工厂和排程日期逻辑删除排程结果
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     * @return 影响行数
     */
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate);
}
