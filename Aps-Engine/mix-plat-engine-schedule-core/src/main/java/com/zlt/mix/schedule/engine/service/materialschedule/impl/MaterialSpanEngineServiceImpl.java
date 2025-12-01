package com.zlt.mix.schedule.engine.service.materialschedule.impl;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanSend;
import com.zlt.mix.schedule.engine.mapper.MaterialSpanEngineMapper;
import com.zlt.mix.schedule.engine.service.materialschedule.MaterialSpanEngineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 硫磺辅料跨区发送接受引擎Service业务层处理
 */
@Slf4j
@Service
public class MaterialSpanEngineServiceImpl implements MaterialSpanEngineService {

    @Resource
    private MaterialSpanEngineMapper materialSpanEngineMapper;

    /**
     * 查询出需要委托其他密炼区生产的硫磺辅料信息
     * @param mixArea
     * @param scheduleDate
     * @return
     */
    public List<MaterialSpanSend> listAutoLhflSpanSetting(String mixArea, Date scheduleDate) {
        return materialSpanEngineMapper.listAutoLhflSpanSetting(mixArea, scheduleDate);
    }
}
