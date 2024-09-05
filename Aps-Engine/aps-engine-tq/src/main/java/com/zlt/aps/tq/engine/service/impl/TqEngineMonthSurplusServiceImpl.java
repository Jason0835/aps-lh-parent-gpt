package com.zlt.aps.tq.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.engine.mapper.TqEngineMonthSurplusMapper;
import com.zlt.aps.tq.engine.service.TqEngineMonthSurplusService;
import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈月度汇总service
 */
@Slf4j
@Service
public class TqEngineMonthSurplusServiceImpl implements TqEngineMonthSurplusService {

    @Resource
    private TqEngineMonthSurplusMapper tqEngineMonthSurplusMapper;

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    public Map<String, TqMonthSurplusVo> getMonthSurplus(String scheduleDate) {
        Map<String, TqMonthSurplusVo> monthSurplusMap = new HashMap<>();
        String[] dateArray = scheduleDate.split("-");
        List<TqMonthSurplusVo> list = tqEngineMonthSurplusMapper.listTqMonthPlanSurplus(dateArray[0], dateArray[1]);
        if(StringUtils.isEmpty(list)) {
            return monthSurplusMap;
        }
        for(TqMonthSurplusVo monthSurplusVo : list) {
            monthSurplusMap.put(monthSurplusVo.getMaterialCode(), monthSurplusVo);
        }
        return monthSurplusMap;
    }
}
