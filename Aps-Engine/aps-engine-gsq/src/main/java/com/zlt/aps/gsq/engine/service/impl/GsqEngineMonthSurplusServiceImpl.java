package com.zlt.aps.gsq.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMonthSurplusMapper;
import com.zlt.aps.gsq.engine.service.GsqEngineMonthSurplusService;
import com.zlt.aps.gsq.engine.vo.GsqMonthSurplusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈月度汇总service
 */
@Slf4j
@Service
public class GsqEngineMonthSurplusServiceImpl implements GsqEngineMonthSurplusService {

    @Resource
    private GsqEngineMonthSurplusMapper GsqEngineMonthSurplusMapper;

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    public Map<String, GsqMonthSurplusVo> getMonthSurplus(String scheduleDate) {
        Map<String, GsqMonthSurplusVo> monthSurplusMap = new HashMap<>();
        String[] dateArray = scheduleDate.split("-");
        List<GsqMonthSurplusVo> list = GsqEngineMonthSurplusMapper.listGsqMonthPlanSurplus(dateArray[0], dateArray[1]);
        if(StringUtils.isEmpty(list)) {
            return monthSurplusMap;
        }
        for(GsqMonthSurplusVo monthSurplusVo : list) {
            monthSurplusMap.put(monthSurplusVo.getMaterialCode(), monthSurplusVo);
        }
        return monthSurplusMap;
    }
}
