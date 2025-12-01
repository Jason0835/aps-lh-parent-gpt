package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 引擎物料跨区模块相关mapper
 */
public interface MaterialSpanEngineMapper {

    /**
     * 查询出需要委托其他密炼区生产的硫磺辅料信息
     * @param mixArea  密炼区
     * @param scheduleDate  排程日期
     * @return
     */
    List<MaterialSpanSend> listAutoLhflSpanSetting(@Param("mixArea") String mixArea, @Param("scheduleDate") Date scheduleDate);
}
