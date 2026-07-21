package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.service.IGsqDataLoadService;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈排程数据加载Service实现。
 *
 * <p>负责将S1阶段需要的全部基础数据加载到Context中。
 * 当前为初版实现，部分数据源仍待补充完整（已在方法注释中标注TODO）。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class GsqDataLoadServiceImpl implements IGsqDataLoadService {

    @Resource
    private GsqEngineMapper gsqEngineMapper;

    @Override
    public void loadAllData(GsqScheduleContext context) {
        String scheduleDate = context.getScheduleDate();
        String factoryCode = context.getFactoryCode();

        log.info("[数据加载] 开始加载, scheduleDate={}, factoryCode={}", scheduleDate, factoryCode);

        // 1. 加载排程参数
        GsqScheduleParams params = loadScheduleParams(factoryCode);
        context.setParams(params);

        // 2. 加载胎圈6班次排程结果，并按BOM分解计算钢丝圈6班次计划量
        List<GsqScheduleResultVo> scheduleList = gsqEngineMapper.statGsqScheduleBase(scheduleDate, params.getProductionStage());
        if (scheduleList == null) {
            scheduleList = new ArrayList<>();
        }
        context.setScheduleList(scheduleList);

        // 3. 加载施工信息（S1校验用）
        List<EngineConstructionInfo> constructionInfoList = gsqEngineMapper.listGsqNeedConstruction(scheduleDate, params.getProductionStage());
        if (constructionInfoList == null) {
            constructionInfoList = new ArrayList<>();
        }
        context.setConstructionInfoList(constructionInfoList);

        // 4. 加载胎圈6班次排程结果（用于S2阶段BOM分解对比）
        List<Map<String, Object>> tq6ClassList = gsqEngineMapper.listTqScheduleResult6Class(scheduleDate);
        Map<String, Map<Integer, Double>> tq6ShiftResultMap = new HashMap<>();
        if (tq6ClassList != null) {
            for (Map<String, Object> row : tq6ClassList) {
                String beadCode = (String) row.get("beadCode");
                if (beadCode == null) {
                    continue;
                }
                Map<Integer, Double> shiftMap = new HashMap<>();
                for (int i = 1; i <= 6; i++) {
                    Object val = row.get("class" + i + "PlanQty");
                    if (val != null) {
                        shiftMap.put(i, ((Number) val).doubleValue());
                    }
                }
                tq6ShiftResultMap.put(beadCode, shiftMap);
            }
        }
        context.setTq6ShiftResultMap(tq6ShiftResultMap);

        // 5. 加载胎圈停产班次配置
        List<Map<String, Object>> tqStopList = gsqEngineMapper.listTqStopShiftConfig(scheduleDate);
        Map<String, Boolean> tqStopShiftMap = convertToStopShiftMap(tqStopList);
        context.setTqStopShiftMap(tqStopShiftMap);

        // 6. 加载钢丝圈停产班次配置
        List<Map<String, Object>> gsqStopList = gsqEngineMapper.listGsqStopShiftConfig(scheduleDate);
        Map<String, Boolean> gsqStopShiftMap = convertToStopShiftMap(gsqStopList);
        context.setGsqStopShiftMap(gsqStopShiftMap);

        // 7. 加载机台检修计划
        List<Map<String, Object>> maintenanceList = gsqEngineMapper.listMachineMaintenancePlan(scheduleDate, scheduleDate);
        Map<String, List<String>> maintenanceMap = convertToMaintenanceMap(maintenanceList);
        context.setMaintenanceMachineMap(maintenanceMap);

        // 8. TODO: 加载6点MES库存、机台寸口/钢丝直径/产线规则、限定/不可作业机台、损耗率等
        // 这些数据源需要扩展mapper方法，当前先用空Map占位，避免NPE
        log.info("[数据加载] 完成, 排程记录数: {}, 施工信息数: {}, 胎圈6班次记录数: {}",
                scheduleList.size(), constructionInfoList.size(), tq6ShiftResultMap.size());
    }

    /**
     * 加载排程参数。
     *
     * <p>TODO: 实际应从 T_GSQ_PARAMS 表读取，当前返回默认值。</p>
     */
    private GsqScheduleParams loadScheduleParams(String factoryCode) {
        GsqScheduleParams params = new GsqScheduleParams();
        params.setFreshPeriodHours(72D);
        params.setWireSwitchTime(1D);
        params.setLastShiftEstimateEnabled("1");
        params.setLastShiftEstimateClassCount(3);
        params.setProductionStage("0");
        params.setToolCapacity(120D);
        params.setClassHours(8D);
        params.setDemandCoefficient(1D);
        return params;
    }

    /**
     * 将停产班次配置List转为Map。
     *
     * <p>Map key格式：日期|班次编码（如"2025-01-01|03"），value=true表示停产</p>
     */
    private Map<String, Boolean> convertToStopShiftMap(List<Map<String, Object>> list) {
        Map<String, Boolean> result = new HashMap<>();
        if (list == null) {
            return result;
        }
        for (Map<String, Object> row : list) {
            Object shiftDate = row.get("shiftDate");
            Object shiftCode = row.get("shiftCode");
            if (shiftDate != null && shiftCode != null) {
                String key = shiftDate.toString() + "|" + shiftCode.toString();
                result.put(key, Boolean.TRUE);
            }
        }
        return result;
    }

    /**
     * 将检修计划List转为Map。
     *
     * <p>Map key格式：日期|班次编码，value=该班次检修中的机台编号列表</p>
     */
    private Map<String, List<String>> convertToMaintenanceMap(List<Map<String, Object>> list) {
        Map<String, List<String>> result = new HashMap<>();
        if (list == null) {
            return result;
        }
        for (Map<String, Object> row : list) {
            Object shiftDate = row.get("shiftDate");
            Object shiftCode = row.get("shiftCode");
            Object machineCode = row.get("machineCode");
            if (shiftDate != null && shiftCode != null && machineCode != null) {
                String key = shiftDate.toString() + "|" + shiftCode.toString();
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(machineCode.toString());
            }
        }
        return result;
    }
}
