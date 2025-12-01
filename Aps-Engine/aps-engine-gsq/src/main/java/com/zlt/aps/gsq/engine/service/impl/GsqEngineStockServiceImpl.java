package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqReserveStockDto;
import com.zlt.aps.gsq.engine.mapper.GsqEngineStockMapper;
import com.zlt.aps.gsq.engine.service.GsqEngineStockService;
import com.zlt.aps.gsq.engine.vo.GsqStockConsumeVo;
import com.zlt.aps.gsq.engine.vo.GsqStockVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "①预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)", "7点半部件库存量：" + toJSONString(list));
        if (list.isEmpty()) {
            log.error("钢丝圈库存查询为空");
            return stockMap;
        }

        //开始计算钢丝圈预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)
        Map<String, Double> consumMap = this.getGsqConsume(batchNo, scheduleDate); // 今天成型早班(成型一班)-昨日半制品早班计划量
        for (GsqStockVo stockVo : list) {
            double consum = consumMap.getOrDefault(stockVo.getSteelRingCode(), 0D);  //消耗量
//            consum = (consum < 0 ? 0 : consum);

            //计算7点实际半部件库存=7点取到的库存*（100-库存损耗率值）%
            double stockNum = stockVo.getStockNum();
            double rate = BigDecimalUtil.div(BigDecimalUtil.sub(100, stockLossRate), 100);  //计算库存率rate = （100 - stockLossRate）/100
            stockNum = BigDecimalUtil.mul(stockNum, rate);  //计算实际的7点半部件库存

            double planStock = BigDecimalUtil.sub(stockNum, consum); //预计库存 = 7点半部件库存 - (今天成型早班(成型一班) - 昨日半制品早班计划量)
            //预计库存需要向下取整
            planStock = (planStock <= 0 ? 0D : BigDecimalUtil.roundDown(planStock, 0));
            stockMap.put(stockVo.getSteelRingCode(), planStock);
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
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "④16点预计库存 = 11点半部件库存 - 11点到16点半部件消耗",
                "预计库存集合：" + toJSONString(stockMap));  //添加日志
        return stockMap;
    }

    /**
     * 计算成型消耗量=今天成型早班(成型一班)-昨日半制品早班计划量
     *
     * @param batchNo 批次号
     * @param scheduleDate 排程日期
     * @return
     */
    private Map<String, Double> getGsqConsume(String batchNo, String scheduleDate) {
        Map<String, Double> result = new HashMap<>();
        List<GsqStockConsumeVo> list = gsqEngineStockMapper.listCxPlanAndConsume(scheduleDate);  //查询钢丝圈胶对应的成型三班计划消耗量，以及8-12点实际消耗量
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "①预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "成型一班计划量对应半制品计划消耗量cxClass1PlanConsume：" + toJSONString(list));  //添加日志
        List<GsqStockConsumeVo> consumeVos = gsqEngineStockMapper.listLastDayMidPlan(scheduleDate);
        autoScheduleLogService.insertTmScheduleLog(batchNo, "", "②预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "昨日早班半制品生产量consume：" + toJSONString(consumeVos));  //添加日志
        if (list.isEmpty()) {
            log.error("计算12点到16点钢丝圈计划消耗量为空");
            return result;
        }
        //开始计算11点到16点半部件消耗量
        for (GsqStockConsumeVo consumeVo : list) {
            Double cxClass3PlanConsume = consumeVo.getCxClass3PlanConsume();
            if (Objects.isNull(cxClass3PlanConsume)) {
                cxClass3PlanConsume = 0D;
            }
            result.put(consumeVo.getSteelRingCode(), cxClass3PlanConsume);
        }
        for (GsqStockConsumeVo consumeVo : consumeVos) {
            String treadCode = consumeVo.getSteelRingCode();
            Double consume = consumeVo.getConsume();
            if (result.containsKey(treadCode)) {
                Double cxClass3PlanConsume = result.get(treadCode);
                if (Objects.isNull(consume)) {
                    consume = 0D;
                }
                double planConsume = BigDecimalUtil.sub(cxClass3PlanConsume, consume);
                result.put(consumeVo.getSteelRingCode(), planConsume);
            } else {
                double planConsume = BigDecimalUtil.sub(0, consume);
                result.put(consumeVo.getSteelRingCode(), planConsume);
            }
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "③预计库存 = 7点半部件库存 + 昨日半制品早班计划量 - 今天成型早班(成型一班)",
                "计算成型一班计划量对应半制品计划消耗量cxClass1PlanConsume-昨日早班半制品生产量consume：" + toJSONString(result));  //添加日志
        return result;
    }

    /**
     * 取预生产库存倍数Map
     * @param codeList 要查询的steelRingCode列表
     * @param reserveStockRate 预生产库存倍数
     * @return 结果
     */
    @Override
    public Map<String, BigDecimal> getReserveStockMap(List<String> codeList, Double reserveStockRate) {
        List<GsqReserveStockDto> reserveStockList = new ArrayList<>();
        List<List<String>> splitList = CollectionUtil.splitList(codeList, 500);
        for (List<String> list : splitList) {
            reserveStockList.addAll(gsqEngineStockMapper.listReserveStock(list));
        }
        if (CollectionUtils.isEmpty(reserveStockList)) {
            return Collections.emptyMap();
        }
        return reserveStockList.stream().collect(Collectors.toMap(GsqReserveStockDto::getSteelRingCode, GsqReserveStockDto::getReserveStockRate, (v1, v2) -> v1));
    }
}
