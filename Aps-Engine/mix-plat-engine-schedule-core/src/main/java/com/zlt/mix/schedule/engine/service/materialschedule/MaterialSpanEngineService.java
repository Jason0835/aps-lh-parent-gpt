package com.zlt.mix.schedule.engine.service.materialschedule;

import com.zlt.mix.schedule.api.domain.entity.MaterialScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;

import java.util.Date;
import java.util.List;

/**
 * 硫磺辅料跨区发送接受引擎接口
 */
public interface MaterialSpanEngineService {

    /**
     * 查询出需要委托其他密炼区生产的硫磺辅料信息
     * @param mixArea 密炼区
     * @param scheduleDate  排程日期
     * @return
     */
    List<MaterialSpanSend> listAutoLhflSpanSetting(String mixArea, Date scheduleDate);

}
