package com.zlt.aps.tq.engine.service.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.tq.engine.mapper.TqEngineStockMapper;
import com.zlt.aps.tq.engine.service.TqEngineStockService;
import com.zlt.aps.tq.engine.vo.TqStockConsumeVo;
import com.zlt.aps.tq.engine.vo.TqStockVo;
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
public class TqEngineStockServiceImpl implements TqEngineStockService {

    @Resource
    private TqEngineStockMapper tqEngineStockMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    /**
     * 计算胎圈隔天7点预计库存
     *
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率（%）
     *
     * @return
     */
    public Map<String, Double> getPlanStockMap(String batchNo, String scheduleDate, Double stockLossRate) {
        return this.getStockMap(batchNo, scheduleDate, stockLossRate, true);
    }
    
    /**
     * 计算胎圈19点预计库存
     *
     * @param batchNo 排程批次号
     * @param scheduleDate 排程日期
     * @param stockLossRate 库存损耗率（%）
     *
     * @return
     */
    @Override
    public Map<String, Double> getNightStockMap(String batchNo, String scheduleDate, Double stockLossRate) {
        return this.getStockMap(batchNo, scheduleDate, stockLossRate, false);
    }
        

    private Map<String, Double> getStockMap(String batchNo, String scheduleDate, Double stockLossRate, boolean isGetPlanStock) {
        Map<String, Double> stockMap = new HashMap<>();
        List<TqStockVo> list = tqEngineStockMapper.listTqStock(scheduleDate);  //查询指定日期的胎圈库存量
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "①预计库存 = 7点胎圈库存 × (1-损耗率) + 当天早班计划量 - 成型一班消耗", "7点胎圈库存量：" + toJSONString(list));
        if (list.isEmpty()) {
            log.error("胎圈库存查询为空");
            return stockMap;
        }

        // 计算胎圈预计库存 = 7点胎圈库存 × (1-损耗率) + 当天早班计划量 - 成型一班消耗
        Map<String, Double> consumMap = this.getTqConsume(batchNo, scheduleDate, isGetPlanStock); // 成型一班消耗 - 当天早班计划量
        for (TqStockVo stockVo : list) {
            double consum = consumMap.getOrDefault(stockVo.getBeadCode(), 0D);  //净消耗量

            //计算7点实际胎圈库存=7点取到的库存*（100-库存损耗率值）%
            double stockNum = stockVo.getStockNum();
            double rate = BigDecimalUtil.div(BigDecimalUtil.sub(100, stockLossRate), 100);  //计算库存率rate = （100 - stockLossRate）/100
            stockNum = BigDecimalUtil.mul(stockNum, rate);  //计算实际的7点胎圈库存

            double planStock = BigDecimalUtil.sub(stockNum, consum); //预计库存 = 7点胎圈库存 - (成型一班消耗 - 当天早班计划量)
            //预计库存需要向下取整
            planStock = BigDecimalUtil.roundDown(planStock, 0); // 调整为预计库存负数的情况，要补量
            stockMap.put(stockVo.getBeadCode(), planStock);
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
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "④预计库存 = 7点胎圈库存 × (1-损耗率) + 当天早班计划量 - 成型一班消耗",
                "预计库存集合：" + toJSONString(stockMap));  //添加日志
        return stockMap;
    }

    /**
     * 计算胎圈净消耗量 = 成型一班消耗 - 当天早班(D日早班)计划量
     *
     * @param batchNo 批次号
     * @param scheduleDate 排程日期
     * @param isGetPlanStock true取7点预计库存(成型一班消耗)，false取19点预计库存(成型二班消耗)
     * @return 净消耗量Map，key=胎圈代码，value=净消耗量（负数表示当天早班产出大于成型消耗）
     */
    private Map<String, Double> getTqConsume(String batchNo, String scheduleDate, boolean isGetPlanStock) {
        Map<String, Double> result = new HashMap<>();
        List<TqStockConsumeVo> list = tqEngineStockMapper.listCxPlanAndConsume(scheduleDate);  //查询胎圈对应的成型一班/二班计划消耗量
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "②预计库存 = 7点胎圈库存 × (1-损耗率) + 当天早班计划量 - 成型一班消耗",
                "成型一班/二班计划量对应胎圈计划消耗量：" + toJSONString(list));  //添加日志
        List<TqStockConsumeVo> consumeVos = tqEngineStockMapper.listTodayMorningPlan(scheduleDate);  //查询当天早班(D日早班)计划量
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "②预计库存 = 7点胎圈库存 × (1-损耗率) + 当天早班计划量 - 成型一班消耗",
                "当天早班(D日早班)胎圈计划量：" + toJSONString(consumeVos));  //添加日志
        if (list.isEmpty() && consumeVos.isEmpty()) {
            log.error("成型一班计划量、当天早班胎圈计划量也为空");
            return result;
        }
        //开始计算胎圈净消耗量
        for (TqStockConsumeVo consumeVo : list) {
            // 获取预计库存，取class1(7点)，否则取class2(19点)的值
            Double cxClass1PlanConsume = isGetPlanStock? consumeVo.getCxClass1PlanConsume(): consumeVo.getCxClass2PlanConsume();
            if (Objects.isNull(cxClass1PlanConsume)) {
                cxClass1PlanConsume = 0D;
            }
            result.put(consumeVo.getBeadCode(), cxClass1PlanConsume);
        }
        for (TqStockConsumeVo consumeVo : consumeVos) {
            String beadCode = consumeVo.getBeadCode();
            Double consume = consumeVo.getConsume();
            if (result.containsKey(beadCode)) {
                Double cxClass1PlanConsume = result.get(beadCode);
                if (Objects.isNull(consume)) {
                    consume = 0D;
                }
                double planConsume = BigDecimalUtil.sub(cxClass1PlanConsume, consume);
                result.put(beadCode, planConsume);
            } else {
                double planConsume = BigDecimalUtil.sub(0, consume);
                result.put(beadCode, planConsume);
            }
        }
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "②预计库存 = 7点胎圈库存 × (1-损耗率) + 当天早班计划量 - 成型一班消耗",
                "计算净消耗量 = 成型一班消耗 - 当天早班计划量：" + toJSONString(result));  //添加日志
        return result;
    }
}
