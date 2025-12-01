package com.zlt.aps.gdyy.engine.service;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineMachineMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineOriginlLineSpecMapper;
import com.zlt.aps.gdyy.engine.mapper.GdyyEngineSpecifyMachineMapper;
import com.zlt.aps.gdyy.engine.vo.GdyyMachineRollMappingVo;
import com.zlt.aps.gdyy.engine.vo.GdyyOriginalLineSpecVo;
import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;
import com.zlt.aps.gdyy.engine.vo.GdyySpecifyMachineVo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

@Service
@Slf4j
public abstract class BaseGdyyEngineMachineService {

    @Resource
    private AutoScheduleLogService autoScheduleLogService;

    @Autowired
    private GdyyEngineMachineMapper gdyyEngineMachineMapper;

    @Autowired
    private GdyyEngineSpecifyMachineMapper gdyyEngineSpecifyMachineMapper;
    
    @Autowired
    private GdyyEngineOriginlLineSpecMapper gdyyEngineOriginlLineSpecMapper;

    /**
     * 日志分割符
     */
    private String division = "\r\n---------------------------------------------------\r\n";

    /**
     * 选择机台
     * @param scheduleList 排程结果
     */
    public void scheduleMachine(List<GdyyScheduleResultVo> scheduleList) {
        // 批次号
        String batchNo = CollectionUtil.firstElement(scheduleList).getBatchNo();
        autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "2.1、生产线安排",
                "优先查看是否有帘线大卷与定点机台的配置，有则安排该生产线；无则查看是否有帘线大卷与机台的映射配置，有则查看该配置是否有在定点机台中配置为不生产，没有则安排该生产线");
        // 抓取钢压大卷与机台的对照关系
        // 限制作业
        Map<String, String> canWorkMap = this.getGdyySpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
        // 不可作业
        Map<String, String> notWorkMap = this.getGdyySpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);

        // 抓取大卷与机台的对照关系
        List<GdyyMachineRollMappingVo> machineRollList = gdyyEngineMachineMapper
                .selectGdyyMachineRollMappingList();
        Map<String, String> machineRollMap = machineRollList.stream().collect(
                Collectors.toMap(GdyyMachineRollMappingVo::getBigRollCode, GdyyMachineRollMappingVo::getMachineId));
        // 记录日志
        String logDetail = logSplit("大卷与定点机台配置：" + toJSONString(canWorkMap), "大卷不可作业的机台配置：" + toJSONString(notWorkMap),
                "大卷与机台的映射关系：" + toJSONString(machineRollMap));
        autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "2.2、生产线安排基础数据日志", logDetail);

        for (GdyyScheduleResultVo scheduleResult : scheduleList) {
            if (StringUtils.isNotBlank(scheduleResult.getMachineCode())) {
                // 判断如果已经有机台了，则说明是从上一次排程复制下来的，可以直接跳过
                continue;
            }

            // 1、先看大卷与定点机台是否有匹配的关系
            String bigRollCode = scheduleResult.getBigRollCode();
            String machineId = null;
            if (canWorkMap.containsKey(bigRollCode)) {
                machineId = canWorkMap.get(bigRollCode);
            }
            // 2、再看映射表有没有匹配，只有定点机台没有匹配上才需要看
            if (machineId == null && machineRollMap.containsKey(bigRollCode)) {
                machineId = machineRollMap.get(bigRollCode);
                // 除了大卷匹配上，还要求机台沒有被设置为“不可作业”
                machineId = this.removeNotWorkMachineId(notWorkMap, bigRollCode, machineId);
            }
            scheduleResult.setMachineCode(machineId);
        }
        // 记录日志
        autoScheduleLogService.insertGdyyScheduleLog(batchNo, "", "2.3、生产线安排完成", "安排生产线后排程记录：" + toJSONString(scheduleList));
    }

    /**
     * 根据产能选机台
     *
     * @param scheduleList         排程数据
     */
    public void chooseMachineByCapacity(List<GdyyScheduleResultVo> scheduleList) {
        // 根据机台产能选机台
        List<GdyyMachineInfo> allMachineList = this.listGdyyMachine();
        Map<String, String> specifyCanMachineMap = this.getGdyySpecifyMachineMap(EngineConstants.JOB_TYPE_CAN);
        Map<String, String> specifyNotMachineMap = this.getGdyySpecifyMachineMap(EngineConstants.JOB_TYPE_NOT);

        // 机台夜班已占用产能
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(16);
        // 机台白班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(16);

        // 先对排产计划
        List<GdyyScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted((o1, o2) -> {
            Integer flag1 = specifyCanMachineMap.containsKey(o1.getBigRollCode()) ? 1 : 2;
            Integer flag2 = specifyCanMachineMap.containsKey(o2.getBigRollCode()) ? 1 : 2;
            if (flag1.compareTo(flag2) != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                return flag1.compareTo(flag2);
            }
            // 如果定点机台设置一样，则按计划量从大到小
            BigDecimal planQty1 = BigDecimalUtils.add(o1.getClass1Plan(), o1.getClass2Plan());
            BigDecimal planQty2 = BigDecimalUtils.add(o2.getClass1Plan(), o2.getClass2Plan());
            return planQty2.compareTo(planQty1);
        }).collect(Collectors.toList());

        // 根据夜班计划分配机台
        for (GdyyScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getClass1Plan();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()); // 夜班
            List<GdyyMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            GdyyMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineCode(String.valueOf(machineId));
            //检查机台，如果早班不作业，则把计划量都转移到夜班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))) {
                scheduleVo.setClass1Plan(BigDecimalUtil.add(midPlanQty, scheduleVo.getClass2Plan()));
                scheduleVo.setClass2Plan(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getClass1Plan())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getClass2Plan())));
            // 添加日志
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap);
        }

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (GdyyScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineCode())) {
                continue;
            }
            // 早班
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex());
            List<GdyyMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    // 检索当班可选机台
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap);
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                scheduleVo.setMachineCode(String.valueOf(CollectionUtil.firstElement(allMachineList).getId()));// 匹配不到机台，直接随机分配
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            GdyyMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineCode(String.valueOf(machineId));
            //检查机台，如果夜班不作业，则把计划量都转移到早班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))) {
                scheduleVo.setClass2Plan(BigDecimalUtil.add(scheduleVo.getClass1Plan(), scheduleVo.getClass2Plan()));
                scheduleVo.setClass1Plan(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getClass1Plan())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getClass2Plan())));
            // 添加日志
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap);
        }
        this.caculateMachineQuata(chooseMachineScheduleList, allMachineList); // 根据机台定额重算计划量
        
        // 重算个数与总量
        for (GdyyScheduleResultVo resultVo: chooseMachineScheduleList) {
            BigDecimal standardSize = (BigDecimal) resultVo.getParams().get(EngineConstants.STANDARD_SIZE);
            double class1Plan = resultVo.getClass1Plan();
            double class2Plan = resultVo.getClass2Plan();
            resultVo.setClass1PlanNum(BigDecimalUtil.div(class1Plan, standardSize.doubleValue(), 1));
            resultVo.setClass2PlanNum(BigDecimalUtil.div(class2Plan, standardSize.doubleValue(), 1));
        }
    }
    

    /**
     * 根据机台定额重算计划量
     * @param scheduleList      排产计划
     * @param allMachineList    机台
     */
    private void caculateMachineQuata(List<GdyyScheduleResultVo> scheduleList, List<GdyyMachineInfo> allMachineList) {
        // 加载钢丝卷长
        Map<String, BigDecimal> wireCoilLengthMap = gdyyEngineOriginlLineSpecMapper.listGdyyOriginalLineSpec().stream()
                .collect(Collectors.toMap(GdyyOriginalLineSpecVo::getOriginalLineCode,
                        GdyyOriginalLineSpecVo::getOriginalLineLength));
        // 计算机台总产能
        for (GdyyMachineInfo machine: allMachineList) {
            // 查找安排在此机台上的规格
            List<GdyyScheduleResultVo> matchScheduleList = scheduleList.stream()
                    .filter(s -> StringUtils.isNotEmpty(s.getMachineCode()) 
                            && !s.getMachineCode().contains(",")
                            && machine.getId().equals(new Long(s.getMachineCode()))) // 根据机台ID过滤
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(matchScheduleList)) {
                continue;
            }
            // 根据原丝分组
            Map<String, List<GdyyScheduleResultVo>> wireGrouppingMap = matchScheduleList.stream()
                    .filter(r -> StringUtils.isNotEmpty(r.getSteelLineCode()))
                    .collect(Collectors.groupingBy(GdyyScheduleResultVo::getSteelLineCode));
            
            // 获取机台定额
            BigDecimal quata = machine.getQuata();
            String openMachineClass = machine.getOpenMachineClass();
            int openClass = 0; // 开机班数
            boolean isNightClass = true;
            if (StringUtils.isNotEmpty(openMachineClass)) {
                String[] classArr = openMachineClass.split(",");
                openClass = classArr.length;
                isNightClass = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()).equals(classArr[0]);
            }
            Double totalQuata = BigDecimalUtils.multiply(quata, new BigDecimal(openClass)).doubleValue(); // 总定额
            
            // 总计划量要受制于定额，少了的差值在原丝长度最大的原线组处理，差值也要修正成整卷，每次处理一卷
            Double totalPlan = matchScheduleList.stream().map(GdyyScheduleResultVo::getTotalPlan).reduce(0D, (a, b) -> BigDecimalUtil.add(a, b));
            if (totalPlan.doubleValue() >= totalQuata.doubleValue()) {
                // 超过班产的，强制将钢带长度最长的规格转移到下一个班
                String steelLineCode = wireCoilLengthMap.entrySet().stream().max(Comparator.comparing(Entry::getValue)).get().getKey();
                List<GdyyScheduleResultVo> wireScheduleList = wireGrouppingMap.get(steelLineCode);
                for (GdyyScheduleResultVo scheduleVo: wireScheduleList) {
                    Double class1Plan = scheduleVo.getClass1Plan();
                    Double class2Plan = scheduleVo.getClass2Plan();
                    if (class1Plan == 0 && class2Plan > 0) {
                        class1Plan = class2Plan;
                        class2Plan = 0D;
                    } else if (class1Plan > 0 && class2Plan == 0) {
                        class2Plan = class1Plan;
                        class1Plan = 0D;
                    }
                }
                continue;
            }
            BigDecimal diffPlan = BigDecimalUtils.sub(totalQuata, totalPlan); // 差值
            boolean isCapacityPass = diffPlan.compareTo(BigDecimal.ZERO) < 0; // 是否超量

            // 取出供需比最小的规格
            String steelLineCode = scheduleList.stream()
                    .filter(r -> { // 只处理计划量不足一个原丝长度的规格
                        BigDecimal originalLineLength = (BigDecimal)r.getParams().get(EngineConstants.ORIGINAL_LINE_LENGTH);
                        BigDecimal planQty = BigDecimalUtils.valueOf(r.getTotalPlanQty());
                        return planQty.compareTo(BigDecimal.ZERO) > 0 && planQty.compareTo(originalLineLength) < 0; // 只处理有需求量的
                    })
                    .sorted((r1, r2) -> this.compareResultPlan(r1, r2, isCapacityPass))
                    .map(GdyyScheduleResultVo::getSteelLineCode).findFirst().orElse(null);
            if (StringUtils.isEmpty(steelLineCode)) {
                continue;
            }
            List<GdyyScheduleResultVo> wireScheduleList = wireGrouppingMap.get(steelLineCode);
            Double wireTotalPlanQty = wireScheduleList.stream().mapToDouble(GdyyScheduleResultVo::getTotalPlanQty).sum();
            BigDecimal originalLineLength = (BigDecimal)CollectionUtil.firstElement(wireScheduleList).getParams().get(EngineConstants.ORIGINAL_LINE_LENGTH);
            if (wireTotalPlanQty <= originalLineLength.doubleValue()) {
                diffPlan = BigDecimalUtils.sub(originalLineLength, wireTotalPlanQty); // 差值
            } else {
                diffPlan = BigDecimalUtils.sub(BigDecimalUtils.ceil(wireTotalPlanQty, originalLineLength), wireTotalPlanQty); // 取整后的差值
            }
            while (diffPlan.compareTo(BigDecimal.ZERO) != 0) {
                GdyyScheduleResultVo scheduleVo = wireScheduleList.stream()
                        .sorted((r1, r2) -> this.compareResultPlan(r1, r2, isCapacityPass)).findFirst().get();
                if (scheduleVo == null) {
                    break;
                }
                BigDecimal newStandardSize = (BigDecimal)scheduleVo.getParams().get(EngineConstants.STANDARD_SIZE); // 原丝长度
                BigDecimal addPlan = BigDecimalUtils.least(diffPlan.abs(), newStandardSize); // 取原丝长度、差值作为本次处理的值
                BigDecimal class1Plan = BigDecimalUtils.valueOf(scheduleVo.getClass1Plan());
                BigDecimal class2Plan = BigDecimalUtils.valueOf(scheduleVo.getClass2Plan());
                if (isCapacityPass) { // 超量需要扣减
                    BigDecimal class1SubPlan = BigDecimalUtils.least(class1Plan, addPlan);
                    class1Plan = class1Plan.subtract(class1SubPlan);
                    class2Plan = class2Plan.subtract(addPlan.subtract(class1SubPlan));
                } else { // 缺量需要补值
                    class1Plan = isNightClass? class1Plan.add(addPlan): class1Plan;
                    class2Plan = !isNightClass? class2Plan.add(addPlan): class2Plan;
                }
                scheduleVo.setClass1Plan(class1Plan.doubleValue());
                scheduleVo.setClass2Plan(class2Plan.doubleValue());
                diffPlan = isCapacityPass? diffPlan.add(addPlan): diffPlan.subtract(addPlan);
                if (diffPlan.compareTo(BigDecimal.ZERO) < 0 ^ isCapacityPass) {
                    break;
                }
            }
        }
    }

    /**
     * 比对排产计划，按计划量》需求量排
     * @param scheduleVo1
     * @param scheduleVo2
     * @param isPass    是否超产能
     * @return
     */
    private int compareResultPlan(GdyyScheduleResultVo scheduleVo1, GdyyScheduleResultVo scheduleVo2, boolean isPass) {
        // 一天消耗量
        Double cxPlanQty1 = BigDecimalUtil.add(scheduleVo1.getCxClass3Plan(), scheduleVo1.getCxClass4Plan());
        Double cxPlanQty2 = BigDecimalUtil.add(scheduleVo2.getCxClass3Plan(), scheduleVo2.getCxClass4Plan());
        // 一天生产量+库存
        Double planQty1 = BigDecimalUtil.add(scheduleVo1.getStockQty(), scheduleVo1.getTotalPlanQty());
        Double planQty2 = BigDecimalUtil.add(scheduleVo2.getStockQty(), scheduleVo2.getTotalPlanQty());

        // 供需比
        BigDecimal stockRate1 = BigDecimalUtils.div(planQty1, cxPlanQty1, 2); 
        BigDecimal stockRate2 = BigDecimalUtils.div(planQty2, cxPlanQty2, 2); 
        if (isPass) {
            return stockRate2.compareTo(stockRate1); // 超产能需要扣减，因此先处理比率高的，即库存多或消耗少的的（倒序）
        } else {
            return stockRate1.compareTo(stockRate2);
        }
    }
    
    /**
     * 选择排程对应机台列表
     *
     * @param scheduleVo           排程
     * @param classCode            班制
     * @param capacityMap          机台产能map
     * @param allMachineList       所有机台
     * @param specifyCanMachineMap 定点机台
     * @param specifyNotMachineMap 不可作业机台
     * @return 机台列表
     */
    private List<GdyyMachineInfo> searchOptionalMachineList(GdyyScheduleResultVo scheduleVo, String classCode, Map<Long, BigDecimal> capacityMap, List<GdyyMachineInfo> allMachineList, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
        String beadCode = scheduleVo.getBigRollCode(); // 大卷代码
        // 定点机台ID列表
        String specifyMachineIds = specifyCanMachineMap.get(beadCode);
        List<String> machineIds;
        // 如果有设置定点机台，需要把非定点全部过滤掉
        if (StringUtils.isNotEmpty(specifyMachineIds)) {
            machineIds = Arrays.asList(specifyMachineIds.split(","));
        } else {
            machineIds = new ArrayList<>(0);
        }
        // 可选机台
        List<GdyyMachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {
                    // 排除定点不可生产机台
                    String machineId = String.valueOf(m.getId());
                    String notMachine = specifyNotMachineMap.get(beadCode);
                    if (StringUtils.isEmpty(notMachine)) {
                        return true;
                    }
                    String[] notMachineIds = notMachine.split(",");
                    for (String notMachineId : notMachineIds) {
                        return Objects.equals(machineId, notMachineId);
                    }
                    return true;
                }).filter(m -> {
                    // 如果有设置定点机台，则仅选中定点机台
                    if (CollectionUtils.isNotEmpty(machineIds)) {
                        return machineIds.contains(String.valueOf(m.getId()));
                    }
                    return true;
                }).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode))
                // 对应班次可用
                .sorted(new Comparator<GdyyMachineInfo>() {
                    // 按剩余产能升序排序
                    @Override
                    public int compare(GdyyMachineInfo m1, GdyyMachineInfo m2) {
                        return capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO)
                                .compareTo(capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO));
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
    }

    /**
     * 设置生产线日志
     *
     * @param scheduleVo           排程数据
     * @param specifyCanMachineMap 定点机台限制作业
     * @param specifyNotMachineMap 定点机台不可作业
     */
    private void chooseMachineLog(GdyyScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap) {
        StringBuffer logDetail = new StringBuffer();
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertGdyyScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * 查询所有机台信息
     *
     * @return 机台信息
     */
    public List<GdyyMachineInfo> listGdyyMachine() {
        return gdyyEngineMachineMapper.listGdyyMachine();
    }

    /**
     * 获取指定作业类型的定点机映射关系
     *
     * @Author steve
     * @date 2025-2-17 20:49:48
     * @param jobType 作业类型
     * @return 机台Map
     */
    public Map<String, String> getGdyySpecifyMachineMap(String jobType) {
        List<GdyySpecifyMachineVo> gdyySpecifyMachineList = gdyyEngineSpecifyMachineMapper
                .selectGdyySpecifyMachineList(jobType);
        Map<String, String> gdyySpecifyMachineMap = gdyySpecifyMachineList.stream()
                .collect(Collectors.toMap(GdyySpecifyMachineVo::getBigRollCode, GdyySpecifyMachineVo::getMachineId));
        return gdyySpecifyMachineMap;
    }

    /**
     * 移除原机台ID中已配置为不生产该钢带的机台ID
     *
     * @Author steve
     * @Description
     * @Date 2025-2-17 21:00:33
     * @param notWorkMap  不生产机台映射表
     * @param bigRollCode 需生产的大卷编号
     * @param machineId   原机台ID
     * @return
     */
    private String removeNotWorkMachineId(Map<String, String> notWorkMap, String bigRollCode, String machineId) {
        String newMachineId = machineId;
        // 判断定点机台中是否有配置不生产该大卷的机台
        if (notWorkMap.containsKey(bigRollCode)) {
            // 取出不生产该大卷的机台ID
            String notMachineId = notWorkMap.get(bigRollCode);

            // 通过匹配机台ID，确认映射关系中是否存在已配置为不生产的机台ID
            String[] machineIdArr = machineId.split(",");
            String[] notMachineIdArr = notMachineId.split(",");
            // 过滤掉不生产的机台ID
            List<String> newMachineIdList = Arrays.stream(machineIdArr)
                    .filter(n -> Arrays.stream(notMachineIdArr).noneMatch(n::equals))
                    .collect(Collectors.toList());
            if (!newMachineIdList.isEmpty()) {
                // 如果有保留的机台ID，重新合并赋值
                newMachineId = StringUtils.join(newMachineIdList, ',');
            } else {
                // 没有保留的机台ID，则机台放空
                newMachineId = null;
            }
        }
        return newMachineId;
    }

}
