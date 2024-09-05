package com.zlt.aps.tc.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.engine.mapper.TcEngineMonthSurplusMapper;
import com.zlt.aps.tc.engine.service.TcEngineMonthSurplusService;
import com.zlt.aps.tc.engine.vo.TcMonthSurplusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧月度汇总service
 */
@Slf4j
@Service
public class TcEngineMonthSurplusServiceImpl implements TcEngineMonthSurplusService {

    @Resource
    private TcEngineMonthSurplusMapper tcEngineMonthSurplusMapper;

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    public Map<String, TcMonthSurplusVo> getMonthSurplus(String scheduleDate) {
        Map<String, TcMonthSurplusVo> monthSurplusMap = new HashMap<>();
        String[] dateArray = scheduleDate.split("-");
        List<TcMonthSurplusVo> list = tcEngineMonthSurplusMapper.listTcMonthPlanSurplus(dateArray[0], dateArray[1]);
        if(StringUtils.isEmpty(list)) {
            return monthSurplusMap;
        }
        for(TcMonthSurplusVo monthSurplusVo : list) {
            monthSurplusMap.put(monthSurplusVo.getMaterialCode(), monthSurplusVo);
        }
        return monthSurplusMap;
    }
}
