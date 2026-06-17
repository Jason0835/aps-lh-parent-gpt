package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 胎面排程结果解释表 Mapper接口
 */
@Mapper
public interface TmScheduleResultExplainMapper extends CommBaseMapper<TmScheduleResultExplain> {

    /**
     * 按工厂和排程日期逻辑删除结果解释
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     * @return 影响行数
     */
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate);
}
