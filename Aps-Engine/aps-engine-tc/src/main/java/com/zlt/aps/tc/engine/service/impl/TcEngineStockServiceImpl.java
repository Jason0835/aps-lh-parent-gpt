package com.zlt.aps.tc.engine.service.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tc.engine.mapper.TcEngineStockMapper;
import com.zlt.aps.tc.engine.service.TcEngineStockService;
import com.zlt.aps.tc.engine.vo.TcStockConsumeVo;
import com.zlt.aps.tc.engine.vo.TcStockVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.alibaba.fastjson.JSON.toJSONString;

/**
 * 库存service
 */
@Slf4j
@Service
public class TcEngineStockServiceImpl implements TcEngineStockService {

    @Resource
    private TcEngineStockMapper tcEngineStockMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /**
     * 计算胎侧预计库存
     *
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率（%）
     *
     * @return
     */
    public Map<String, Double> getPlanStockMap(String batchNo, String scheduleDate, Double stockLossRate) {
        Map<String, Double> stockMap = new HashMap<>();
        List<TcStockVo> list = tcEngineStockMapper.listTcStock(scheduleDate);  //询指定日期的胎侧库存量
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "①预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)", "7点半部件库存量：" + toJSONString(list));
        if (list.isEmpty()) {
            log.error("胎侧库存查询为空");
            return stockMap;
        }

        //开始计算胎面预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)
        Map<String, Double> consumMap = this.getTcConsume(batchNo, scheduleDate); // 今天成型早班(成型一班)-昨日半制品早班计划量
        for (TcStockVo stockVo : list) {
            double consum = consumMap.getOrDefault(stockVo.getSidewallCode(), 0D);  //消耗量
//            consum = (consum < 0 ? 0 : consum);

            //计算7点实际半部件库存=7点取到的库存*（100-库存损耗率值）%
            double stockNum = stockVo.getStockNum();
            double rate = BigDecimalUtil.div(BigDecimalUtil.sub(100, stockLossRate), 100);  //计算库存率rate = （100 - stockLossRate）/100
            stockNum = BigDecimalUtil.mul(stockNum, rate);  //计算实际的7点半部件库存

            double planStock = BigDecimalUtil.sub(stockNum, consum); //预计库存 = 7点半部件库存 - (今天成型早班(成型一班) - 昨日半制品早班计划量)
            planStock = (planStock <= 0 ? 0D : BigDecimalUtil.roundDown(planStock,0)); //预计库存需要向下取整
            stockMap.put(stockVo.getSidewallCode(), planStock);
        }
        // 解决物料无库存的情况，库存当成0，遍历consumMap的key和stockMap的key进行比较，如果key不存在，则添加key和value
        for (String key : consumMap.keySet()) {
            if (!stockMap.containsKey(key)) {
                Double consume = consumMap.get(key);
                if (consume < 0) {
                    stockMap.put(key, BigDecimalUtil.roundDown(0 - consume, 0));
                }
            }
        }
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "⑤预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "预计库存集合：" + toJSONString(stockMap));  //添加日志
        return stockMap;
    }

    /**
     * 计算11点到16点半部件计划消耗量 = (8点到16点成型计划 - 8点到12点成型生产量）
     *
     * @param batchNo 批次号
     * @param scheduleDate 排程日期
     * @return
     */
    private Map<String, Double> getTcConsume(String batchNo, String scheduleDate) {
        Map<String, Double> result = new HashMap<>();
        List<TcStockConsumeVo> list = tcEngineStockMapper.listCxPlanAndConsume(scheduleDate);  //查询胎侧胶对应的成型一班计划消耗量，以及8-12点实际消耗量
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "②预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "成型一班计划量对应半制品计划消耗量cxClass1PlanConsume：" + toJSONString(list));  //添加日志
        List<TcStockConsumeVo> consumeVos = tcEngineStockMapper.listLastDayMidPlan(scheduleDate);
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "③预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "昨日早班半制品生产量consume：" + toJSONString(consumeVos));  //添加日志
        if (list.isEmpty() && consumeVos.isEmpty()) {
            log.error("成型一班计划量、半部件昨日早班计划量也为空");
            return result;
        }
        //开始计算半部件消耗量
        for (TcStockConsumeVo consumeVo : list) {
            Double cxClass1PlanConsume = consumeVo.getCxClass1PlanConsume();
            if (Objects.isNull(cxClass1PlanConsume)) {
                cxClass1PlanConsume = 0D;
            }
            result.put(consumeVo.getSidewallCode(), cxClass1PlanConsume);
        }
        for (TcStockConsumeVo consumeVo : consumeVos) {
            String treadCode = consumeVo.getSidewallCode();
            Double consume = consumeVo.getConsume();
            if (result.containsKey(treadCode)) {
                Double cxClass1PlanConsume = result.get(treadCode);
                if (Objects.isNull(consume)) {
                    consume = 0D;
                }
                double planConsume = BigDecimalUtil.sub(cxClass1PlanConsume, consume);
                result.put(consumeVo.getSidewallCode(), planConsume);
            } else {
                double planConsume = BigDecimalUtil.sub(0, consume);
                result.put(consumeVo.getSidewallCode(), planConsume);
            }
        }
        autoScheduleLogService.insertTcScheduleLog(batchNo, "", "④预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "计算成型一班计划量对应半制品计划消耗量cxClass1PlanConsume-昨日早班半制品生产量consume：" + toJSONString(result));  //添加日志
        return result;
    }
}
