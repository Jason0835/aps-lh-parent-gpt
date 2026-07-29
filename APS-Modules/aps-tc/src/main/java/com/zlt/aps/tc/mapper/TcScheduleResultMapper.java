package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎侧排程结果表 Mapper接口
 */
@Mapper
public interface TcScheduleResultMapper extends CommBaseMapper<TcScheduleResult> {

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

    /**
     * 锁定指定工厂、日期和批次的当前排程结果。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 当前批次
     * @return 加行锁后的排程结果
     */
    List<TcScheduleResult> selectScopeForUpdate(@Param("factoryCode") String factoryCode,
                                                @Param("scheduleDate") Date scheduleDate,
                                                @Param("batchNo") String batchNo);

    /**
     * 按主键锁定排程结果，用于管理员修改发布状态时防止并发覆盖。
     *
     * @param ids 已排序的排程结果 ID
     * @return 加行锁后的排程结果
     */
    List<TcScheduleResult> selectBatchIdsForUpdate(@Param("ids") List<Long> ids);
}
