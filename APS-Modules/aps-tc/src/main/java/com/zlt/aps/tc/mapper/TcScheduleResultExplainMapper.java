package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎侧排程结果解释表 Mapper接口
 */
@Mapper
public interface TcScheduleResultExplainMapper extends CommBaseMapper<TcScheduleResultExplain> {

    /**
     * 按工厂和排程日期逻辑删除结果解释
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     * @return 影响行数
     */
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate);

    /**
     * 按工厂和旧批次号逻辑删除解释，覆盖未排任务没有结果 ID 的解释行。
     *
     * @param factoryCode 工厂编号
     * @param batchNoList 旧批次号
     * @return 影响行数
     */
    int logicDeleteByFactoryCodeAndBatchNos(@Param("factoryCode") String factoryCode,
                                             @Param("batchNoList") List<String> batchNoList);
}
