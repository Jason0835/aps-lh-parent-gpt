package com.zlt.aps.lh.mapper;

import java.util.Date;

import com.zlt.aps.lh.api.domain.entity.LhTestScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;


/**
 * Description: 硫化TEST排程结果Mapper
 * @author
 */
@Mapper
public interface LhTestScheduleResultEntityMapper extends CommBaseMapper<LhTestScheduleResult> {


    /**
     * 根据分厂编码和排程时间删除记录
     *
     * @param factoryCode 分厂编码
     * @param scheduleDate 排程时间
     * @return 受影响的行数
     */
    int deleteByFactoryCodeAndScheduleDate(@Param("factoryCode") String factoryCode,
                                           @Param("scheduleDate") Date scheduleDate);
}
