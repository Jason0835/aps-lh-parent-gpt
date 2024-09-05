package com.zlt.aps.nc.engine.service.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.nc.engine.mapper.NcEngineStockMapper;
import com.zlt.aps.nc.engine.service.NcEngineStockService;
import com.zlt.aps.nc.engine.vo.NcStockConsumeVo;
import com.zlt.aps.nc.engine.vo.NcStockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 库存service
 */
@Slf4j
@Service
public class NcEngineStockServiceImpl implements NcEngineStockService {

    @Resource
    private NcEngineStockMapper ncEngineStockMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /**
     * 计算胎面16点预计库存
     *
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率（%）
     *
     * @return
     */
    public Map<String, Double> getPlanStockMap(String batchNo, String scheduleDate, Double stockLossRate) {
        Map<String, Double> stockMap = new HashMap<>();
        List<NcStockVo> list = ncEngineStockMapper.listNcStock(scheduleDate);  //询指定日期的内衬库存量
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "①16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗", "11点半部件库存量：" + toJSONString(list));
        if (list.isEmpty()) {
            log.error("内衬库存查询为空");
            return stockMap;
        }

        //开始计算内衬16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗
        Map<String, Double> consumMap = this.getNcConsume(batchNo, scheduleDate); // 11点到16点半部件计划消耗量
        for (NcStockVo stockVo : list) {
            double consum = consumMap.getOrDefault(stockVo.getLiningCode(), 0D);  //消耗量
            consum = (consum < 0 ? 0 : consum);

            //计算11点实际半部件库存=11点取到的库存*（100-库存损耗率值）%
            double stockNum = stockVo.getStockNum();
            double rate = BigDecimalUtil.div(BigDecimalUtil.sub(100, stockLossRate), 100);  //计算库存率rate = （100 - stockLossRate）/100
            stockNum = BigDecimalUtil.mul(stockNum, rate);  //计算实际的11点半部件库存

            double planStock = BigDecimalUtil.sub(stockNum, consum); //16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗
            planStock = (planStock <= 0 ? 0D : BigDecimalUtil.roundDown(planStock,0)); //预计库存需要向下取整
            stockMap.put(stockVo.getLiningCode(), planStock);
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "④16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "16点预计库存集合：" + toJSONString(stockMap));  //添加日志
        return stockMap;
    }

    /**
     * 计算11点到16点半部件计划消耗量 = (8点到16点成型计划 - 8点到12点成型生产量）
     *
     * @param scheduleDate 排程日期
     * @return
     */
    private Map<String, Double> getNcConsume(String batchNo, String scheduleDate) {
        Map<String, Double> result = new HashMap<>();
        List<NcStockConsumeVo> list = ncEngineStockMapper.listCxPlanAndConsume(scheduleDate);  //查询内衬胶对应的成型三班计划消耗量，以及8-12点实际消耗量
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "②16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "半制品对应成型(8点-16点)计划量的消耗量cxClass3PlanConsume，以及制品对应成型(8点-12点)完成量的消耗量cxFinishConsume：" + toJSONString(list));  //添加日志
        if (list.isEmpty()) {
            log.error("计算12点到16点内衬计划消耗量为空");
            return result;
        }
        //开始计算11点到16点半部件消耗量
        for (NcStockConsumeVo consumeVo : list) {
            double planConsume = BigDecimalUtil.sub(consumeVo.getCxClass3PlanConsume(), consumeVo.getCxFinishConsume());  //11点到16点半部件计划消耗量
            result.put(consumeVo.getLiningCode(), planConsume);
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "③16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "计算11点到16点半部件消耗量 = 半制品对应成型(8点-16点)计划量的消耗量cxClass3PlanConsume - 制品对应成型(8点-12点)完成量的消耗量cxFinishConsume：" + toJSONString(result));  //添加日志
        return result;
    }
}
