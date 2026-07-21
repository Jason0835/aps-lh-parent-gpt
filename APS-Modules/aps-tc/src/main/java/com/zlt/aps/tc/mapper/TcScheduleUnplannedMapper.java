package com.zlt.aps.tc.mapper;

import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 胎侧排程未排列表 Mapper接口
 */
@Mapper
public interface TcScheduleUnplannedMapper extends CommBaseMapper<TcScheduleUnplanned> {

    /**
     * 按工厂和排程日期逻辑删除未排列表
     * @param factoryCode 工厂编号
     * @param scheduleDate 排程日期
     * @return 影响行数
     */
    int logicDeleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode, @Param("scheduleDate") Date scheduleDate);
}
