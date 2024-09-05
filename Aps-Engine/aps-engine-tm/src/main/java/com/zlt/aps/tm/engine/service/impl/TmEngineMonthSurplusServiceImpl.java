package com.zlt.aps.tm.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.tm.engine.mapper.TmEngineMonthSurplusMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import com.zlt.aps.tm.engine.service.TmEngineMonthSurplusService;
import com.zlt.aps.tm.engine.service.TmEngineStockService;
import com.zlt.aps.tm.engine.vo.TmMonthSurplusVo;
import com.zlt.aps.tm.engine.vo.TmStockConsumeVo;
import com.zlt.aps.tm.engine.vo.TmStockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎面月度汇总service
 */
@Slf4j
@Service
public class TmEngineMonthSurplusServiceImpl implements TmEngineMonthSurplusService {

    @Resource
    private TmEngineMonthSurplusMapper tmEngineMonthSurplusMapper;

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    public Map<String, TmMonthSurplusVo> getMonthSurplus(String scheduleDate) {
        Map<String, TmMonthSurplusVo> monthSurplusMap = new HashMap<>();
        String[] dateArray = scheduleDate.split("-");
        List<TmMonthSurplusVo> list = tmEngineMonthSurplusMapper.listTmMonthPlanSurplus(dateArray[0], dateArray[1]);
        if(StringUtils.isEmpty(list)) {
            return monthSurplusMap;
        }
        for(TmMonthSurplusVo monthSurplusVo : list) {
            monthSurplusMap.put(monthSurplusVo.getMaterialCode(), monthSurplusVo);
        }
        return monthSurplusMap;
    }
}
