package com.zlt.aps.gsq.engine.service.impl;

import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.mapper.GsqEngineLossMapper;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMachineMapper;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.mapper.GsqEngineStockMapper;
import com.zlt.aps.gsq.engine.service.IGsqDataLoadService;
import com.zlt.aps.gsq.engine.vo.GsqLossVo;
import com.zlt.aps.gsq.engine.vo.GsqScheduleParams;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.engine.vo.GsqParamsVo;
import com.zlt.aps.gsq.engine.vo.GsqSpecifyMachineVo;
import com.zlt.aps.gsq.engine.vo.GsqStockVo;
import com.zlt.aps.gsq.engine.vo.GsqTwiningDiscMachineVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Resource
    private GsqEngineStockMapper gsqEngineStockMapper;

    @Resource
    private GsqEngineMachineMapper gsqEngineMachineMapper;

    @Resource
    private GsqEngineLossMapper gsqEngineLossMapper;

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

        // 7. 加载机台检修计划（对齐胎圈TQ：key=日期|班次编码，value=检修机台列表）
        List<Map<String, Object>> maintenanceList = gsqEngineMapper.listMachineMaintenancePlan(scheduleDate);
        Map<String, List<String>> maintenanceMap = convertToMaintenanceMap(maintenanceList);
        context.setMaintenanceMachineMap(maintenanceMap);

        // 8. 加载机台列表（仅启用机台，SQL已按 STATUS='1' 过滤）
        List<GsqMachineInfo> machineList = gsqEngineMachineMapper.listGsqMachine();
        if (machineList == null) {
            machineList = new ArrayList<>();
        }
        context.setAllMachineList(machineList);
        log.info("[数据加载] 机台加载完成, 机台数: {}", machineList.size());

        // 9. 加载定点机台映射（0-限制作业 → specifyCanMachineMap，1-不可作业 → specifyNotMachineMap）
        context.setSpecifyCanMachineMap(convertToSpecifyMachineMap(gsqEngineMachineMapper.listGsqSpecifyMachine("0")));
        context.setSpecifyNotMachineMap(convertToSpecifyMachineMap(gsqEngineMachineMapper.listGsqSpecifyMachine("1")));

        // 10. 加载钢丝圈-缠绕盘代码映射（多对多：一个钢丝圈可挂多个缠绕盘）
        //     用于排程结果 twiningDiscCode 回填及 S3 机台分配缠绕盘连续优先
        Map<String, Set<String>> twiningDiscCodeMap = new HashMap<>();
        List<Map<String, Object>> twiningDiscCodeList = gsqEngineMachineMapper.listGsqTwiningDiscCode();
        if (twiningDiscCodeList != null) {
            for (Map<String, Object> row : twiningDiscCodeList) {
                Object steelRingCode = row.get("steelRingCode");
                Object twiningDiscCode = row.get("twiningDiscCode");
                if (steelRingCode != null && twiningDiscCode != null) {
                    twiningDiscCodeMap
                            .computeIfAbsent(steelRingCode.toString(), k -> new HashSet<>())
                            .add(twiningDiscCode.toString());
                }
            }
        }
        context.setTwiningDiscCodeMap(twiningDiscCodeMap);

        // 10.1 加载缠绕盘-机台映射（多对多：一个缠绕盘可绑多个机台）
        //      用于 S3 DiscMachineFilter 过滤：仅保留规格可用盘绑定机台并集内的机台
        Map<String, Set<String>> discMachineMap = new HashMap<>();
        List<GsqTwiningDiscMachineVo> discMachineList = gsqEngineMachineMapper.listGsqTwiningDiscMachine();
        if (discMachineList != null) {
            for (GsqTwiningDiscMachineVo vo : discMachineList) {
                if (StringUtils.isEmpty(vo.getTwiningDiscCode()) || StringUtils.isEmpty(vo.getMachineIds())) {
                    continue;
                }
                Set<String> machineSet = discMachineMap
                        .computeIfAbsent(vo.getTwiningDiscCode(), k -> new HashSet<>());
                for (String machineCode : vo.getMachineIds().split(",")) {
                    if (StringUtils.isNotEmpty(machineCode.trim())) {
                        machineSet.add(machineCode.trim());
                    }
                }
            }
        }
        context.setDiscMachineMap(discMachineMap);
        log.info("[数据加载] 缠绕盘映射加载完成, 规格-盘映射数: {}, 盘-机台映射数: {}",
                twiningDiscCodeMap.size(), discMachineMap.size());

        // 8. 加载钢丝圈库存（排程日期前一天库存，listGsqStock 内部已按 STOCK_DATE=排程日期-1 查询）
        List<GsqStockVo> stockList = gsqEngineStockMapper.listGsqStock(scheduleDate);
        Map<String, Double> stockMap = new HashMap<>();
        if (stockList != null) {
            for (GsqStockVo stock : stockList) {
                if (stock.getSteelRingCode() != null) {
                    stockMap.put(stock.getSteelRingCode(), stock.getStockNum() == null ? 0D : stock.getStockNum());
                }
            }
        }
        context.setStockMap(stockMap);
        log.info("[数据加载] 库存加载完成, 规格数: {}", stockMap.size());

        // 11. 加载损耗率（对齐胎圈TQ：从 T_GSQ_LOSS_SETTING 表读取并按钢丝圈代码聚合，供 S2.3 计算计划量使用）
        List<GsqLossVo> lossVoList = gsqEngineLossMapper.listLossRate();
        context.setLossRateMap(buildSteelRingLossRateMap(lossVoList));
        // 同时构建机台维度损耗率映射（key=机台代码#钢丝圈代码），供 S3 机台分配后按实际机台精确取损耗率
        context.setMachineLossRateMap(buildMachineLossRateMap(lossVoList));
        log.info("[数据加载] 损耗率加载完成, 规格数: {}, 机台维度条目数: {}",
                context.getLossRateMap().size(), context.getMachineLossRateMap().size());

        log.info("[数据加载] 完成, 排程记录数: {}, 施工信息数: {}, 胎圈6班次记录数: {}",
                scheduleList.size(), constructionInfoList.size(), tq6ShiftResultMap.size());
    }

    /**
     * 按钢丝圈代码聚合损耗率映射（对齐胎圈 TqDataLoadServiceImpl.buildBeadLossRateMap）。
     *
     * <p>lossRateMap 的 key 格式为 "机台id#钢丝圈代码"（如 "20#HT3568-377P"），S2阶段机台尚未分配无法精确查询，
     * 本方法遍历并按钢丝圈代码聚合，得到 steelRingCode → LOSS_RATE 的映射。</p>
     *
     * <p>聚合规则：同一钢丝圈在多机台损耗率一致时直接取该值；不一致时取平均值。
     * 返回的损耗率为 LOSS_RATE 字段小数原值（如 0.02 表示2%），调用方使用时按 1 + lossRate 计算乘数。</p>
     *
     * @param lossVoList 原始损耗率列表（key=机台#钢丝圈代码，value=LOSS_RATE 字段原值）
     * @return 按钢丝圈代码聚合的损耗率映射（key=钢丝圈代码，value=LOSS_RATE 字段小数原值）
     */
    private Map<String, Double> buildSteelRingLossRateMap(List<GsqLossVo> lossVoList) {
        Map<String, Double> result = new HashMap<>();
        if (lossVoList == null || lossVoList.isEmpty()) {
            return result;
        }
        // 先按钢丝圈代码分组收集所有机台的损耗率
        Map<String, List<Double>> lossRateListMap = new HashMap<>();
        for (GsqLossVo lossVo : lossVoList) {
            String lossKey = lossVo.getLossKey();
            if (lossKey == null) {
                continue;
            }
            // key 格式：机台id#钢丝圈代码 或 #钢丝圈代码（无机台）
            int separatorIdx = lossKey.indexOf('#');
            if (separatorIdx < 0 || separatorIdx >= lossKey.length() - 1) {
                continue;
            }
            String steelRingCode = lossKey.substring(separatorIdx + 1);
            if (steelRingCode.trim().isEmpty()) {
                continue;
            }
            Double lossRate = lossVo.getLossRate();
            if (lossRate == null) {
                continue;
            }
            lossRateListMap.computeIfAbsent(steelRingCode, k -> new ArrayList<>()).add(lossRate);
        }
        // 聚合：一致则直接取，不一致则取平均值
        for (Map.Entry<String, List<Double>> entry : lossRateListMap.entrySet()) {
            List<Double> rates = entry.getValue();
            if (rates.isEmpty()) {
                continue;
            }
            double firstRate = rates.get(0);
            boolean allSame = true;
            for (Double rate : rates) {
                if (Double.compare(rate, firstRate) != 0) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                result.put(entry.getKey(), firstRate);
            } else {
                double sum = 0D;
                for (Double rate : rates) {
                    sum += rate;
                }
                result.put(entry.getKey(), BigDecimalUtil.div(sum, rates.size(), 4));
            }
        }
        return result;
    }

    /**
     * 构建机台维度损耗率映射（key=机台代码#钢丝圈代码，value=LOSS_RATE 字段小数原值）。
     *
     * <p>供 S3 机台分配确定后按实际机台精确取损耗率，解决按钢丝圈聚合取平均导致的失真问题
     * （如 023 在 GSQM03 配置0.02、其它机台配置0.0826，聚合平均会得到0.0513）。</p>
     *
     * @param lossVoList 原始损耗率列表（key=机台#钢丝圈代码，value=LOSS_RATE 字段原值）
     * @return 机台维度损耗率映射（key=机台代码#钢丝圈代码，value=LOSS_RATE 字段小数原值）
     */
    private Map<String, Double> buildMachineLossRateMap(List<GsqLossVo> lossVoList) {
        Map<String, Double> result = new HashMap<>();
        if (lossVoList == null || lossVoList.isEmpty()) {
            return result;
        }
        for (GsqLossVo lossVo : lossVoList) {
            String lossKey = lossVo.getLossKey();
            if (lossKey == null || lossKey.trim().isEmpty() || lossVo.getLossRate() == null) {
                continue;
            }
            // 仅保留有机台维度的 key（格式：机台代码#钢丝圈代码），纯 "#钢丝圈代码" 无机台条目略过
            if (lossKey.startsWith("#")) {
                continue;
            }
            result.put(lossKey, lossVo.getLossRate());
        }
        return result;
    }

    /**
     * 加载排程参数。
     *
     * <p>从 T_GSQ_PARAMS 表按分厂读取启用参数，覆盖默认值；不存在或解析失败时使用默认值。</p>
     */
    private GsqScheduleParams loadScheduleParams(String factoryCode) {
        GsqScheduleParams params = new GsqScheduleParams();
        // 默认值兜底
        params.setFreshPeriodHours(72D);
        params.setWireSwitchTime(1D);
        params.setLastShiftEstimateEnabled("1");
        params.setLastShiftEstimateClassCount(3);
        params.setProductionStage("0");
        params.setToolCapacity(120D);
        params.setClassHours(8D);
        params.setDemandCoefficient(1D);
        params.setBackupTriggerThresholdClass(0.7D);
        params.setBackupMultiSpecThreshold(1000D);
        params.setRoundingMergeThreshold(0D);
        params.setMachineOverAssignTolerance(0D);

        // 从 T_GSQ_PARAMS 读取启用的排程参数并覆盖默认值
        List<GsqParamsVo> paramList = gsqEngineMapper.listGsqParams(factoryCode);
        if (paramList == null || paramList.isEmpty()) {
            return params;
        }
        Map<String, String> paramMap = new HashMap<>();
        for (GsqParamsVo p : paramList) {
            if (p.getParamCode() != null && p.getParamValue() != null) {
                paramMap.put(p.getParamCode(), p.getParamValue());
            }
        }
        // 备库班数（SYS1601003）
        params.setStockShiftCount(parseParamDouble(paramMap.get("SYS1601003"), params.getStockShiftCount()));
        // 备库班次单班排产阈值（SYS1603004）
        params.setBackupShiftThreshold(parseParamDouble(paramMap.get("SYS1603004"), params.getBackupShiftThreshold()));
        // 备库触发阈值，单位班（SYS1601006）
        params.setBackupTriggerThresholdClass(parseParamDouble(paramMap.get("SYS1601006"), params.getBackupTriggerThresholdClass()));
        // 备库规格班次最大班产阈值（SYS1603005，多规格机台上备库规格当班初始排产上限）
        params.setBackupMultiSpecThreshold(parseParamDouble(paramMap.get("SYS1603005"), params.getBackupMultiSpecThreshold()));
        // 取整合并阈值（SYS1603006，备库分摊时剩余量≤此值合并到当前班次，默认0不启用）
        params.setRoundingMergeThreshold(parseParamDouble(paramMap.get("SYS1603006"), params.getRoundingMergeThreshold()));
        // 机台定额超排容忍阈值（SYS1603007，超出定额≤此值允许当班超排，默认0不启用）
        params.setMachineOverAssignTolerance(parseParamDouble(paramMap.get("SYS1603007"), params.getMachineOverAssignTolerance()));
        return params;
    }

    /**
     * 解析参数为Double，参数为空或解析失败时返回默认值。
     *
     * @param value        参数值
     * @param defaultValue 默认值
     * @return 解析后的参数值
     */
    private Double parseParamDouble(String value, Double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("[数据加载] 钢丝圈参数解析失败，value={}，使用默认值{}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 将定点机台List转为Map。
     *
     * <p>Map key=钢丝圈代码(steelRingCode)，value=机台编号列表(逗号分隔)。</p>
     */
    private Map<String, String> convertToSpecifyMachineMap(List<GsqSpecifyMachineVo> list) {
        Map<String, String> result = new HashMap<>();
        if (list == null) {
            return result;
        }
        for (GsqSpecifyMachineVo vo : list) {
            if (vo.getSteelRingCode() != null && vo.getMachineIds() != null) {
                result.put(vo.getSteelRingCode(), vo.getMachineIds());
            }
        }
        return result;
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
     * 将检修计划List转为Map（对齐胎圈TQ）。
     *
     * <p>Map key格式：日期|班次编码，value=该班次检修中的机台编号列表</p>
     */
    private Map<String, List<String>> convertToMaintenanceMap(List<Map<String, Object>> list) {
        Map<String, List<String>> result = new HashMap<>();
        if (list == null) {
            return result;
        }
        for (Map<String, Object> row : list) {
            Object machineCode = row.get("machineCode");
            if (machineCode == null) {
                continue;
            }
            String downtimeDate = String.valueOf(row.get("downtimeDate"));
            // 截取日期部分（格式可能为 yyyy-MM-dd HH:mm:ss）
            if (downtimeDate.length() > 10) {
                downtimeDate = downtimeDate.substring(0, 10);
            }
            Object shiftCode = row.get("downtimeShift");
            if (shiftCode != null) {
                String key = downtimeDate + "|" + shiftCode.toString();
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(machineCode.toString());
            }
        }
        return result;
    }
}
