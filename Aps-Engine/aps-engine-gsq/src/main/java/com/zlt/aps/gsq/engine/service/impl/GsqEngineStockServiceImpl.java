package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.gsq.engine.mapper.GsqEngineStockMapper;
import com.zlt.aps.gsq.engine.service.GsqEngineStockService;
import com.zlt.aps.gsq.engine.vo.GsqStockConsumeVo;
import com.zlt.aps.gsq.engine.vo.GsqStockVo;
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
public class GsqEngineStockServiceImpl implements GsqEngineStockService {

    @Resource
    private GsqEngineStockMapper gsqEngineStockMapper;
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
        List<GsqStockVo> list = gsqEngineStockMapper.listGsqStock(scheduleDate);  //询指定日期的钢丝圈库存量
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "①16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗", "11点半部件库存量：" + toJSONString(list));
        if (list.isEmpty()) {
            log.error("钢丝圈库存查询为空");
            return stockMap;
        }

        //开始计算钢丝圈16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗
        Map<String, Double> consumMap = this.getGsqConsume(batchNo, scheduleDate); // 11点到16点半部件计划消耗量
        for (GsqStockVo stockVo : list) {
            double consum = consumMap.getOrDefault(stockVo.getSteelRingCode(), 0D);  //消耗量
            consum = (consum < 0 ? 0 : consum);

            //计算11点实际半部件库存=11点取到的库存*（100-库存损耗率值）%
            double stockNum = stockVo.getStockNum();
            double rate = BigDecimalUtil.div(BigDecimalUtil.sub(100, stockLossRate), 100);  //计算库存率rate = （100 - stockLossRate）/100
            stockNum = BigDecimalUtil.mul(stockNum, rate);  //计算实际的11点半部件库存

            double planStock = BigDecimalUtil.sub(stockNum, consum); //16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗
            planStock = (planStock <= 0 ? 0D : planStock);
            stockMap.put(stockVo.getSteelRingCode(), planStock);
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "④16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "16点预计库存集合：" + toJSONString(stockMap));  //添加日志
        return stockMap;
    }

    /**
     * 计算11点到16点半部件计划消耗量 = (8点到16点成型计划 - 8点到12点成型生产量）
     *
     * @param batchNo 批次号
     * @param scheduleDate 排程日期
     * @return
     */
    private Map<String, Double> getGsqConsume(String batchNo, String scheduleDate) {
        Map<String, Double> result = new HashMap<>();
        List<GsqStockConsumeVo> list = gsqEngineStockMapper.listCxPlanAndConsume(scheduleDate);  //查询钢丝圈胶对应的成型三班计划消耗量，以及8-12点实际消耗量
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "②16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "半制品对应成型(8点-16点)计划量的消耗量cxClass3PlanConsume，以及制品对应成型(8点-12点)完成量的消耗量cxFinishConsume：" + toJSONString(list));  //添加日志
        if (list.isEmpty()) {
            log.error("计算12点到16点钢丝圈计划消耗量为空");
            return result;
        }
        //开始计算11点到16点半部件消耗量
        for (GsqStockConsumeVo consumeVo : list) {
            double planConsume = BigDecimalUtil.sub(consumeVo.getCxClass3PlanConsume(), consumeVo.getCxFinishConsume());  //11点到16点半部件计划消耗量
            result.put(consumeVo.getSteelRingCode(), planConsume);
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "③16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "计算11点到16点半部件消耗量 = 半制品对应成型(8点-16点)计划量的消耗量cxClass3PlanConsume - 制品对应成型(8点-12点)完成量的消耗量cxFinishConsume：" + toJSONString(result));  //添加日志
        return result;
    }
}
