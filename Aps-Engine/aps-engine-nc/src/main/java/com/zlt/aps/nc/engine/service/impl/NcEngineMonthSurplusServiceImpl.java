package com.zlt.aps.nc.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.engine.mapper.NcEngineMonthSurplusMapper;
import com.zlt.aps.nc.engine.service.NcEngineMonthSurplusService;
import com.zlt.aps.nc.engine.vo.NcMonthSurplusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内衬月度汇总service
 */
@Slf4j
@Service
public class NcEngineMonthSurplusServiceImpl implements NcEngineMonthSurplusService {

    @Resource
    private NcEngineMonthSurplusMapper NcEngineMonthSurplusMapper;

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    public Map<String, NcMonthSurplusVo> getMonthSurplus(String scheduleDate) {
        Map<String, NcMonthSurplusVo> monthSurplusMap = new HashMap<>();
        String[] dateArray = scheduleDate.split("-");
        List<NcMonthSurplusVo> list = NcEngineMonthSurplusMapper.listNcMonthPlanSurplus(dateArray[0], dateArray[1]);
        if(StringUtils.isEmpty(list)) {
            return monthSurplusMap;
        }
        for(NcMonthSurplusVo monthSurplusVo : list) {
            monthSurplusMap.put(monthSurplusVo.getMaterialCode(), monthSurplusVo);
        }
        return monthSurplusMap;
    }
}
