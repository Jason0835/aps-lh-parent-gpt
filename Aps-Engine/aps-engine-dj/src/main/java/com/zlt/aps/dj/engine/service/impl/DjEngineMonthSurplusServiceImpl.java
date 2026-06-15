package com.zlt.aps.dj.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.dj.engine.mapper.DjEngineMonthSurplusMapper;
import com.zlt.aps.dj.engine.service.DjEngineMonthSurplusService;
import com.zlt.aps.dj.engine.vo.DjMonthSurplusVo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 垫胶月度汇总service
 */
@Slf4j
@Service
public class DjEngineMonthSurplusServiceImpl implements DjEngineMonthSurplusService {

    @Resource
    private DjEngineMonthSurplusMapper djEngineMonthSurplusMapper;

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    public Map<String, DjMonthSurplusVo> getMonthSurplus(String scheduleDate) {
        Map<String, DjMonthSurplusVo> monthSurplusMap = new HashMap<>();
        String[] dateArray = scheduleDate.split("-");
        List<DjMonthSurplusVo> list = djEngineMonthSurplusMapper.listDjMonthPlanSurplus(dateArray[0], dateArray[1]);
        if(StringUtils.isEmpty(list)) {
            return monthSurplusMap;
        }
        for(DjMonthSurplusVo monthSurplusVo : list) {
            monthSurplusMap.put(monthSurplusVo.getMaterialCode(), monthSurplusVo);
        }
        return monthSurplusMap;
    }
}
