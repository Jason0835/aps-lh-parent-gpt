package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 胎面排程未排列表 Mapper接口
 */
@Mapper
public interface TmScheduleUnplannedMapper extends CommBaseMapper<TmScheduleUnplanned> {

    /**
     * 按工厂和排程日期逻辑删除未排列表
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     * @return 影响行数
     */
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate);
}
