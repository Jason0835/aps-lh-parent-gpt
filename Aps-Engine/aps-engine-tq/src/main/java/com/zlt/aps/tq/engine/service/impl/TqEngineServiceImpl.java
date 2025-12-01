package com.zlt.aps.tq.engine.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.enums.OpenMachineClassEnums;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.mapper.TqEngineStockMapper;
import com.zlt.aps.tq.engine.service.*;
import com.zlt.aps.tq.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.*;

@Slf4j
@Service
public class TqEngineServiceImpl implements TqEngineService {

    @Resource
    private TqEngineMapper tqEngineMapper;
    @Resource
    private TqEngineStockService tqEngineStockService;
    @Resource
    private TqEngineMachineService tqEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private TqEngineLossService tqEngineLossService;
    @Resource
    private TqEngineMonthSurplusService tqEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private TqEngineStockMapper tqEngineStockMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    private final static BigDecimal HOUR24 = new BigDecimal("24"); // 24小时
    private final static String DEFAULT_TOOL_CAPACITY = "110"; // 默认值：工装容量默认值
    private final static String DEFAULT_PRODUCT_STOCK_HOUR = "12"; // 默认值：保库存供应时长
    private final static String DEFAULT_LARGE_DEMAND = "1500"; // 默认值：需求量超过该值的算大需求量规格，库存应该需要控制，且超过该值早夜班对半分
    private final static String DEFAULT_BIG_SIZE_SPEC = "35"; // 默认值：大尺寸阈值，超过该尺寸的规格比较难做，不要集中在一个班做
    private final static String DEFAULT_MIN_PLAN_QTY = "12750"; // 默认值：夜个班最少排产量
    private final static String DEFAULT_EQUAL_SHARE_THRESHOLD = "300"; // 需求量超过该值早夜班对半分
    private final static String DEFAULT_CLASS_STOCK_REFERENCE = "22500"; // 交接班库存平衡基准值
    private final static String DEFAULT_ONE_ROLL_NUM = "220"; // 最低排产量默认值

    /**
     * 胎圈胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoTqSchedule(String scheduleDate) {
        String username = SecurityUtils.getUsername(); //用户账号
        String cxBatchNo = "";  //成型批次号
        String batchNo = this.createBatchNo(scheduleDate);  //胎圈排程批次号
        TqScheduleParams params = this.loadParams();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = params.getProductionStage();  //仅投产阶段规格排产标识
        List<TqScheduleResultVo> scheduleList = tqEngineMapper.statTqScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 胎圈胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据成型排程记录 统计出 胎圈胶排程记录基础数据 为空");
            autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
        }
        //过滤掉成型5个班的计划量都为0的数据
        scheduleList = scheduleList.stream().filter(s -> (s.getCxClass2Plan()+s.getCxClass3Plan()+s.getCxClass4Plan()+s.getCxClass5Plan())>0).collect(Collectors.toList());
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "根据成'型排程记录'统计出胎圈胶排程记录基础数据",  toJSONString(scheduleList));
        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> mouthPlateMachineMap = tqEngineMachineService.getMouthPlateMachineMap(); //获得口型板代码map
        Map<String, String> specifyCanMachineMap = tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得胎圈代码和定点机台的限制作业map
        Map<String, String> specifyNotMachineMap = tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得胎圈代码和定点机台的不可作业map
        Map<String, Double> planStockMap = tqEngineStockService.getPlanStockMap(batchNo, scheduleDate, params.getStockLossRate());  //计算胎圈隔天7点预计库存
        Map<String, Double> stockMap = this.loadTqStock(scheduleDate); // 加载库存
        Map<String, Double> lastDayMidPlanMap = this.loadLastDayMidPlan(scheduleDate); // 加载昨日早班计划
        Map<String, Double> lossRateMap = tqEngineLossService.getLossRateMap();   //损耗率map
        Map<String, TqMonthSurplusVo> monthSurplus = tqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  // 获得月度计划剩余量、完成量
        List<TqMachineInfo> allMachineList = tqEngineMachineService.listTqMachine();
        this.baseDataLog(batchNo, mouthPlateMachineMap, specifyCanMachineMap, specifyNotMachineMap, planStockMap, lossRateMap, monthSurplus, params); //把基础数据假如到日志中
        TqTotalPlanQtyVo totalPlanQtyVo = new TqTotalPlanQtyVo();  //胎圈中班和夜班总计划量Vo
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            cxBatchNo = scheduleVo.getCxBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);
            scheduleVo.setStockQty(stockMap.getOrDefault(scheduleVo.getBeadCode(), 0D));  // 库存
            scheduleVo.setSurplusQty(Optional.ofNullable(monthSurplus.get(scheduleVo.getBeadCode())).map(TqMonthSurplusVo::getMonthRemainQty).orElse(0D)); // 剩余量
//            scheduleVo.setPlanStockQty(planStockMap.getOrDefault(scheduleVo.getBeadCode(), 0D));
            scheduleVo.setLastMidPlanQty(lastDayMidPlanMap.getOrDefault(scheduleVo.getBeadCode(), 0D)); // 上一天早班库存
            scheduleVo.setPlanStockQty(BigDecimalUtils.qtySub(BigDecimalUtil.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty()), scheduleVo.getCxClass1Plan())); // 计算19点预计库存
            scheduleVo.getParams().put(EngineConstants.BIG_SIZE_SPEC, params.getBigSizeSpec());
            autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getPlanStockQty());  //库存供应时长，用预计库存计算
            this.computeTqPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, params);  //计算胎圈中班和夜班计划量
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getBeadCode()), params.getCloseOutNum());  //设置收尾提示标识 和 生产状态字段
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(username);
        }
        this.equilibriumDay1(scheduleList, totalPlanQtyVo, params);  // 均衡第一天计划
        this.equilibriumDay2(scheduleList, totalPlanQtyVo, params); // 均衡第二天计划
//        this.equilibrium(batchNo, scheduleList, paramsMap, totalPlanQtyVo, toolCapacity.doubleValue());  //中班和夜班计排程计划量均衡处理（非特殊规格）
        this.chooseMachine(scheduleList, allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);  //选择生产线
        this.setProduceOrder(scheduleList);  //设置白班和夜班的生产顺序

        List<TqScheduleResultVo> existScheduleList = this.tqEngineMapper.listTqEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncTqScheduleToLog(scheduleDate);  //把排程数据同步到log表，删除历史外协排程数据
        this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);  //创建自动排程记录
        List<TqScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getBeadCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getBeadCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            tqEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            tqEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    /**
     * 加载当天库存
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadTqStock(String scheduleDate) {
        return tqEngineStockMapper.listTqStock(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeadCode()))
                .collect(Collectors.toMap(TqStockVo::getBeadCode, TqStockVo::getStockNum));
    }

    /**
     * 加载上一天的早班计划
     *
     * @param scheduleDate
     * @return
     */
    private Map<String, Double> loadLastDayMidPlan(String scheduleDate) {
        return tqEngineStockMapper.listLastDayMidPlan(scheduleDate).stream()
                .filter(v -> StringUtils.isNotEmpty(v.getBeadCode()))
                .collect(Collectors.toMap(TqStockConsumeVo::getBeadCode, TqStockConsumeVo::getConsume));
    }

    /**
     * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     * @param scheduleDate 排程日志
     * @param batchNo 批次号
     * @param productionStage 仅投产阶段规格排产标识
     */
    private void ValidatedConstruction(String scheduleDate, String batchNo, String productionStage, Map<String, String> mapAssistSpec) {
        List<EngineConstructionInfo> list = tqEngineMapper.listTqNeedConstruction(scheduleDate, productionStage);
        list = list.stream().filter(r -> !mapAssistSpec.containsKey(r.getTireRingCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
        for(EngineConstructionInfo construction : list) {
            List<String> errorColumns = new ArrayList<>();
            String embryoCode = construction.getEmbryoCode().split(",")[0];  //成型排程结果对应的胎胚代码
            String[] versionArray = construction.getBomDataVersion().split(",");
            String embryoVersion = versionArray.length > 0 ? versionArray[0] : "";  //施工版本
            if(construction.getEmbryoCode().split(",").length < 2) {
                //施工表胎胚代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoCode") + "\"");
            }
            if(versionArray.length < 2) {
                //施工表版本为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.embryoVersion") + "\"");
            }
            if(StringUtils.isBlank(construction.getTireRingCode())) {
                //施工表胎圈代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.tireRingCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getBeadCode())) {
                //施工表钢丝圈代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getApexCode())) {
                //施工表三角胶为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.apexCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getHexagonRubberCode())) {
                //施工表胎圈胶料为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.hexagonRubberCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getHexagonMouthPlate())) {
                //施工表胎圈口型板为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.hexagonMouthPlate") + "\"");
            }
            if(StringUtils.isBlank(construction.getHexagonRubberDimension())) {
                //施工表胎圈尺寸为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.hexagonRubberDimension") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
                throw new RuntimeException(tip);
            }
        }
    }

    /**
     * 把外协规格列表转成Map
     * @return
     */
    private Map<String, String> mapAssistSpec() {
        Map<String, String> map = new HashMap<>();
        List<String> listAssistSpec = this.tqEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 胎圈插单
     * @param scheduleVo
     */
    public int inertTqOrder(TqScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<TqScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveTqSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveTqSchedule(String scheduleDate, List<TqScheduleResultVo> scheduleList) {
        return this.batchSaveTqSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveTqSchedule(String scheduleDate, List<TqScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = tqEngineMapper.getTqCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“，那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //胎圈排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncTqScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        List<String> beadCodes = scheduleList.stream().map(TqScheduleResultVo::getBeadCode).collect(Collectors.toList());
        TqScheduleParams params = this.loadParams();  // 获取工序参数map
        String productionStage = params.getProductionStage();  //仅投产阶段规格排产标识
        Map<String, TqScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, beadCodes, productionStage);  //根据胎圈代码查询对应的胎圈基础信息
        Map<String, Double> planStockMap = tqEngineStockService.getPlanStockMap(batchNo, scheduleDate, params.getStockLossRate());  //计算胎圈16点预计库存
        Map<String, TqMonthSurplusVo> monthSurplus = tqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map：" + JSONObject.toJSONString(params)));  //添加日志

        for(TqScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            TqScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getBeadCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }

            Double midPlanQty = schedule.getMidPlanQty();  //中班计划量
            schedule.setMidPlanQty(midPlanQty == null ? 0D : midPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);
            Double dayPlanQty = schedule.getDayPlanQty();  //白班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);
            Double nextMidPlanQty = schedule.getNextMidPlanQty();  //次日中班计划量
            schedule.setNextMidPlanQty(nextMidPlanQty == null ? 0D : nextMidPlanQty);

            schedule.setStockQty(planStockMap.getOrDefault(schedule.getBeadCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getBeadCode()), params.getCloseOutNum());  //设置收尾提示标识 和 生产状态字段
            schedule.setUnitConsume(1D);  //单耗：1条胎对应2个钢丝圈
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
        }
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return tqEngineMapper.mergeTqScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据胎圈代码查询对应的胎圈基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, TqScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> beadCodes, String productionStage) {
        Map<String, TqScheduleBaseInfoVo> map = new HashMap<>();
        List<TqScheduleBaseInfoVo> list = tqEngineMapper.listTqScheduleBaseInfo(beadCodes, ""); //查询出胎面在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(TqScheduleBaseInfoVo::getBeadCode, baseInfoVo->baseInfoVo));
        }

        Map<String, TqScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<TqScheduleResultVo> hasCxlist = tqEngineMapper.statTqScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(TqScheduleResultVo info : hasCxlist) {
            TqScheduleBaseInfoVo baseInfoVo = new TqScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getBeadCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

    /**

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    public void changeTqMachine(String oldMachineIds, TqScheduleResultVo scheduleResult) {
//        String batchNo = scheduleResult.getBatchNo();  //批次号
//        String orderNo = scheduleResult.getOrderNo();  //工单号
//        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
//        Map<String, Double> lossRateMap = tqEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = tqEngineLossService.getLossRate(scheduleResult.getBeadCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = tqEngineLossService.getLossRate(scheduleResult.getBeadCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
//        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
//                logSplit("重新计算计划量规则：先要根据之前机台的耗损率推算出之前在没有加上耗损率之前的计划量A，然后再用计划量A * 当前机台对应的耗损率，计算出最终的计划量", "转机台前的耗损率：" + oldLossRate + "转机台后的耗损率：" + lossRate));  //添加日志
//
//        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
//        if(dayPlanQty != null) {
//            dayPlanQty = BigDecimalUtil.div(dayPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
//            scheduleResult.setDayPlanQty(dayPlanQty);
//        }
//        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
//        if(nightPlanQty != null) {
//            nightPlanQty = BigDecimalUtil.div(nightPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
//            scheduleResult.setNightPlanQty(nightPlanQty);
//        }
//        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmTqMachine(TqScheduleResultDto scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = tqEngineLossService.getLossRateMap();   //损耗率map
        TqScheduleParams params = this.loadParams();  // 获取工序参数map
        double paramLossRate = params.getLossRate();

        //耗损率
        double lossRate = tqEngineLossService.getLossRate(scheduleResult.getBeadCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

        Double midPlanQty = scheduleResult.getMidPlanQty();  //中班计划量
        if(midPlanQty != null) {
            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
            scheduleResult.setMidPlanQty(BigDecimalUtil.roundUp(midPlanQty,0));
        }
        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
        if(nightPlanQty != null) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty,0));
        }
        Double dayPlanQty = scheduleResult.getDayPlanQty();  //白班计划量
        if(dayPlanQty != null) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty,0));
        }
        Double nextMidPlanQty = scheduleResult.getNextMidPlanQty();  //次日中班计划量
        if(nextMidPlanQty != null) {
            nextMidPlanQty = BigDecimalUtil.add(nextMidPlanQty, BigDecimalUtil.mul(nextMidPlanQty, lossRate));
            scheduleResult.setNextMidPlanQty(BigDecimalUtil.roundUp(nextMidPlanQty,0));
        }
        autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 均衡第一天夜班与第二天的计划
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎圈中班和夜班总计划量Vo
     */
    private void equilibriumDay1(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleParams params) {
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());
        double totalMidPlanQty = totalPlanQtyVo.getTotalMidPlanQty(); // 夜班总计划量
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划量
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 次日夜班总计划量
//        BigDecimal oneProductQty = params.getOneRollNum(); // 最低排产量
//        double midPlanQtyReference = Arrays.asList(totalMidPlanQty, totalNightPlanQty, totalDayPlanQty).stream().mapToDouble(Double::doubleValue).average().getAsDouble(); // 计划平均值
//        double midPlanQtyReference = new Double(DEFAULT_CLASS_STOCK_REFERENCE);
//        midPlanQtyReference = Math.ceil(midPlanQtyReference);
//        double difNum = BigDecimalUtil.sub(totalMidPlanQty, midPlanQtyReference); // 早班和平均值的差值
//        double oldTotalMidPlanQty = totalMidPlanQty; // 备份原夜班总计划量
        double classStockReference = params.getClassStockReference(); // 交接班库存平衡基准值
        double totalClassStock = scheduleList.stream().collect(Collectors.summingDouble(TqScheduleResultVo::getClassStock)).doubleValue();
        double difStock = BigDecimalUtil.sub(totalClassStock, classStockReference); // 早班和平均值的差值
        if (Math.abs(difStock) <= toolCapacity.doubleValue()) { // 差异少于一车，则不需要处理
            return;
        }
//        double difNum = 0; // 新旧夜班的差值，默认为0，只要该差值尽可能接近difStock才算完成平衡
        boolean isNightClassPass = difStock > 0; // 夜班是否超量
        scheduleList = scheduleList.stream().sorted((r1, r2) -> {
            if (isNightClassPass) {
                BigDecimal initStock1 = BigDecimalUtils.add(r1.getStockQty(), r1.getLastMidPlanQty());
                BigDecimal initStock2 = BigDecimalUtils.add(r2.getStockQty(), r2.getLastMidPlanQty());
                BigDecimal cxClassPlan1 = BigDecimalUtils.add(r1.getCxClass1Plan(), r1.getCxClass2Plan());
                BigDecimal cxClassPlan2 = BigDecimalUtils.add(r2.getCxClass1Plan(), r2.getCxClass2Plan());
                BigDecimal classStock1 = BigDecimalUtils.sub(initStock1, cxClassPlan1);
                BigDecimal classStock2 = BigDecimalUtils.sub(initStock2, cxClassPlan2);
                // 夜班超量，将交接班库存较充足的转移到早班（倒序）
                return classStock2.compareTo(classStock1);
            } else {
                BigDecimal classStock1 = BigDecimalUtils.sub(r1.getClassStock(), r1.getCxClass3Plan());
                BigDecimal classStock2 = BigDecimalUtils.sub(r2.getClassStock(), r2.getCxClass3Plan());
                // 早班超量，将交接班库存较充低的转移到夜班（顺序）
                return classStock1.compareTo(classStock2);
            }
        }).collect(Collectors.toList());

        for (TqScheduleResultVo scheduleVo: scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) {
                continue; // 收尾规格不处理
            }
            BigDecimal stockQty = BigDecimalUtils.valueOf(scheduleVo.getStockQty());
            BigDecimal midPlanQty = BigDecimalUtils.valueOf(scheduleVo.getMidPlanQty());
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal cxPlanQty1 = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());// 第一天成型两个班消耗量
            BigDecimal midAddPlan = BigDecimal.ZERO; // 夜班增加量
            BigDecimal nightAddPlan = BigDecimal.ZERO; // 早班增加量
            BigDecimal dayAddPlan = BigDecimal.ZERO; // 次日夜班增加量
            if (isNightClassPass) { // 夜班超量，则从夜班转移到隔天早班
                if (stockQty.add(midPlanQty).compareTo(cxPlanQty1) < 0) { // 库存+早班计划不够第一天需求的不处理
                    continue;
                }
                // 计算夜班不生产的交接班库存
                BigDecimal initStock = BigDecimalUtils.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty());
                BigDecimal cxClassPlan1 = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
                BigDecimal classStock1 = BigDecimalUtils.sub(initStock, cxClassPlan1);
                if (classStock1.doubleValue() <= scheduleVo.getCxClass3Plan()) { // 交接班库存不够早班需求的不处理
                    continue;
                }
                nightAddPlan = midPlanQty; // 早班加
                midAddPlan = nightAddPlan.negate(); // 夜班减
            } else if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) { // 隔天超量，且早班大于0，则从早班转移到夜班
                midAddPlan = nightPlanQty; // 夜班加
                nightAddPlan = midAddPlan.negate(); // 早班减
            } else {
                continue;
            }
            // 先算一下是否调整后差异反而更大
            double newTotalMidPlanQty = BigDecimalUtils.add(totalMidPlanQty, midAddPlan).doubleValue();
            double newTotalClassStock = BigDecimalUtils.add(totalClassStock, midAddPlan).doubleValue();
            double newDifStock = BigDecimalUtil.sub(newTotalClassStock, classStockReference);
            if (Math.abs(newDifStock) > Math.abs(difStock)) { // 如果更大跳过该规格
                continue;
            }
            // 更新各班计划量
            scheduleVo.setMidPlanQty(midPlanQty.add(midAddPlan).doubleValue());
            scheduleVo.setNightPlanQty(nightPlanQty.add(nightAddPlan).doubleValue());
            scheduleVo.setDayPlanQty(dayPlanQty.add(dayAddPlan).doubleValue());
            scheduleVo.setClassStock(BigDecimalUtils.add(scheduleVo.getClassStock(), midAddPlan).doubleValue()); // 夜班计划有变动，需要重算交接班库存
            totalMidPlanQty = newTotalMidPlanQty;
            totalNightPlanQty = BigDecimalUtils.add(totalNightPlanQty, nightAddPlan).doubleValue();
            totalDayPlanQty = BigDecimalUtils.add(totalDayPlanQty, dayAddPlan).doubleValue();
            totalClassStock = newTotalClassStock;
            difStock = newDifStock;
            if (Math.abs(difStock) <= toolCapacity.doubleValue()) { // 差异少于一车，则不需要处理
                break;
            }
//            difNum = newDifNum;
//            if (isNightClassPass ^ Math.abs(difNum) < Math.abs(difStock)) { // 如果计算前后差值符号相反则直接结束
//                break;
//            }
        }
        totalPlanQtyVo.setTotalMidPlanQty(totalMidPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 计算交接班库存
     * @param scheduleVo
     * @return
     */
    private Double getClassStock(TqScheduleResultVo scheduleVo) {
        BigDecimal planQty = BigDecimalUtils.add(scheduleVo.getStockQty(), scheduleVo.getLastMidPlanQty(), scheduleVo.getMidPlanQty());
        BigDecimal cxPlanQty = BigDecimalUtils.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
        return planQty.subtract(cxPlanQty).doubleValue();
    }

    /**
     * 均衡第二天早夜班计划
     *
     * @param scheduleList   排程列表
     * @param paramss      工序参数
     * @param totalPlanQtyVo 胎圈中班和夜班总计划量Vo
     * @param bisectThreshold 中夜班平分阈值，超过该数值的计划中夜班平分
     */
    private void equilibriumDay2(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo, TqScheduleParams params) {
        this.equalShare(scheduleList, params); // 中夜班计划量均分
        refreshTotalPlanQtyVo(scheduleList, totalPlanQtyVo);
        double totalNightPlanQty = totalPlanQtyVo.getTotalNightPlanQty(); // 早班总计划量
        double totalDayPlanQty = totalPlanQtyVo.getTotalDayPlanQty(); // 次日夜班总计划量
        double toolCapacity = params.getToolCapacity(); // 满工装数
        double difNum = BigDecimalUtil.sub(totalDayPlanQty, totalNightPlanQty); //早班和次日夜班的计划量差额
        if (Math.abs(difNum) <= toolCapacity) { // 差异少于一个工装，无需处理
            return;
        }
        double bigSizeNgintPlanQty = scheduleList.stream().filter(s -> this.isBigSizeSpec(s)).collect(Collectors.summingDouble(TqScheduleResultVo::getNightPlanQty)).doubleValue(); // 早班大尺寸规格数量
        double bigSizeDayPlanQty = scheduleList.stream().filter(s -> this.isBigSizeSpec(s)).collect(Collectors.summingDouble(TqScheduleResultVo::getDayPlanQty)).doubleValue(); // 夜班大尺寸规格数量

        boolean isNightClassPass = difNum < 0;  //true：早班超量，false：次日夜班超量
        if (isNightClassPass) {
            // 早班超量，说明库存不足，需要从供需比例较大的（库存比较足的）开始调整
            scheduleList = scheduleList.stream().sorted(this.bigSizeSpecComparator() // 大尺寸规格先处理
                    .thenComparing(TqScheduleResultVo::getSupplyDemandRatio, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        } else {
            // 次日夜班超量，说明库存充足，都再提前做隔天的，需要从供需比例较小的（库存比较小的）开始调整
            scheduleList = scheduleList.stream().sorted(this.bigSizeSpecComparator() // 大尺寸规格先处理
                    .thenComparing(TqScheduleResultVo::getSupplyDemandRatio))
                    .collect(Collectors.toList());
        }

        for (TqScheduleResultVo scheduleVo: scheduleList) {
            if (ApsConstant.STATUS_ENABLE.equals(scheduleVo.getCloseOutSpecFlag())) { // 收尾规格不调整
                continue;
            }
            boolean isBigSizeSpec = this.isBigSizeSpec(scheduleVo);
            double nightPlanQty = scheduleVo.getNightPlanQty();
            double dayPlanQty = scheduleVo.getDayPlanQty();
            if (nightPlanQty == dayPlanQty) { // 中夜班计划量相等的不调整
                continue;
            }
            // 尝试平衡第二天早夜班的计划量
            boolean isNightPlanQtyLarger = nightPlanQty > dayPlanQty; // 本规格早班计划量较大
            double diffPlanQty = BigDecimalUtil.sub(dayPlanQty, nightPlanQty); // 本计划的差异值，次日夜班 - 早班
            if (isBigSizeSpec) { // 大尺寸，要同时判断大尺寸规格的总计划量
                boolean isBigSizeNightPlanQtyLarger = bigSizeNgintPlanQty > bigSizeDayPlanQty; // 大规格夜班总计划量较大
                if (isNightPlanQtyLarger != isBigSizeNightPlanQtyLarger) { // 本规格计划量较高的班次与大规格的相同才有必要调换
                    continue;
                }
            }
            if (isNightPlanQtyLarger ^ isNightClassPass) { // 本规格计划量较高的班次与总计划的相同才有必要调换
                continue;
            }
            if (Math.abs(diffPlanQty) > Math.abs(difNum)) { // 如果差异值超过了总差异，则不处理
                continue;
            }
            if (isNightPlanQtyLarger && scheduleVo.getClassStock() < scheduleVo.getCxClass3Plan()) { // 早班较大，只有交接班库存超过早班需求才能挪到夜班
                continue;
            }
            scheduleVo.setNightPlanQty(dayPlanQty);
            scheduleVo.setDayPlanQty(nightPlanQty);
            totalNightPlanQty = BigDecimalUtil.add(totalNightPlanQty, diffPlanQty); // 总早班更新为：总早班 + (次日夜班 - 早班)
            totalDayPlanQty = BigDecimalUtil.sub(totalDayPlanQty, diffPlanQty); // 总夜班更新为：总夜班 - (次日夜班 - 早班)
            if (isBigSizeSpec) {
                bigSizeNgintPlanQty = BigDecimalUtil.add(bigSizeNgintPlanQty, diffPlanQty); // 大尺寸总早班更新为：总早班 + (次日夜班 - 早班)
                bigSizeDayPlanQty = BigDecimalUtil.sub(bigSizeDayPlanQty, diffPlanQty); // 大尺寸总夜班更新为：总夜班 - (次日夜班 - 早班)
            }
            difNum = BigDecimalUtil.sub(totalDayPlanQty, totalNightPlanQty); // 重算差异
            if (Math.abs(difNum) <= toolCapacity || isNightClassPass ^ difNum < 0) { // 差异不足一个工装，如果计算前后差值符号相反则直接结束
                break;
            }
        }
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty); // 早班总计划里量
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty); // 次日夜班总计划量
    }

    /**
     * 更新计划量统计对象
     * @param scheduleList
     * @param totalPlanQtyVo
     */
    private void refreshTotalPlanQtyVo(List<TqScheduleResultVo> scheduleList, TqTotalPlanQtyVo totalPlanQtyVo) {
        Double totalMidPlanQty = scheduleList.stream().mapToDouble(TqScheduleResultVo::getMidPlanQty).sum();
        Double totalDayPlanQty = scheduleList.stream().mapToDouble(TqScheduleResultVo::getDayPlanQty).sum();
        Double totalNightPlanQty = scheduleList.stream().mapToDouble(TqScheduleResultVo::getNightPlanQty).sum();
        totalPlanQtyVo.setTotalMidPlanQty(totalMidPlanQty);
        totalPlanQtyVo.setTotalDayPlanQty(totalDayPlanQty);
        totalPlanQtyVo.setTotalNightPlanQty(totalNightPlanQty);
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalDayPlanQty, totalNightPlanQty, totalMidPlanQty));
    }

    /**
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param params  排产参数
     */
    private void equalShare(List<TqScheduleResultVo> scheduleList, TqScheduleParams params) {
        BigDecimal bisectThreshold = params.getEqualShareThreshold(); // 各班计划量均分阈值
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity()); // 满工装长度
        // 次日早夜班总计划量超过阈值的平分中夜班计划量
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty());
            BigDecimal dayPlanQty = BigDecimalUtils.valueOf(scheduleVo.getDayPlanQty());
            BigDecimal nextPlanQty = nightPlanQty.add(dayPlanQty);
            BigDecimal nextPlanQtyNum = nextPlanQty.divide(toolCapacity, 1, RoundingMode.HALF_UP); // 工装数
            if (nextPlanQty.compareTo(bisectThreshold) <= 0) {
                if (nightPlanQty.compareTo(BigDecimal.ZERO) > 0) {
                    scheduleVo.setNightPlanQty(nextPlanQty.doubleValue());
                    scheduleVo.setDayPlanQty(0D);
                    continue;
                }
            } else { // 超过指定计划量，中夜班均分
                BigDecimal newDayPlanQty = nextPlanQtyNum.divide(BigDecimalUtils.TWO, 0, RoundingMode.UP).multiply(toolCapacity); // 夜班平分后的计划量，先换算成工装数，平分后再换算成米数
                newDayPlanQty = BigDecimalUtils.least(newDayPlanQty, nextPlanQty); // 取整后的量不能超过总量
                BigDecimal newNightPlanQty = nextPlanQty.subtract(newDayPlanQty); // 夜班计划 = 总计划 - 早班计划
                scheduleVo.setNightPlanQty(newNightPlanQty.doubleValue());
                scheduleVo.setDayPlanQty(newDayPlanQty.doubleValue());
            }
        }
    }

    /**
     * 大尺寸规格排序比对器，大尺寸在前
     * @return
     */
    private Comparator<TqScheduleResultVo> bigSizeSpecComparator() {
        return new Comparator<TqScheduleResultVo>() {
            @Override
            public int compare(TqScheduleResultVo arg0, TqScheduleResultVo arg1) {
                Integer bigSizeSpecFlag0 = isBigSizeSpec(arg0)? 1: -1;
                Integer bigSizeSpecFlag1 = isBigSizeSpec(arg1)? 1: -1;
                return bigSizeSpecFlag1.compareTo(bigSizeSpecFlag0);
            }
        };
    }

    /**
     * 判断是否大尺寸规格
     * @param scheduleVo
     * @return
     */
    private boolean isBigSizeSpec(TqScheduleResultVo scheduleVo) {
//        BigDecimal bigSizeSpec = (BigDecimal) scheduleVo.getParams().get(EngineConstants.BIG_SIZE_SPEC);
//        return BigDecimalUtils.valueOf(scheduleVo.getSpecSize()).compareTo(bigSizeSpec) >= 0;
        return false; // TODO 暂不识别大尺寸规格
    }

    /**
     * 中班和夜班计排程计划量均衡处理
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎圈中班和夜班总计划量Vo
     */
    private void equilibrium(String batchNo, List<TqScheduleResultVo> scheduleList, Map<String, String> paramsMap, TqTotalPlanQtyVo totalPlanQtyVo, double toolCapacity) {
        BigDecimal hardSpecSize = new BigDecimal("35"); // 难做尺寸，超过的话不要合并
        String oldScheduleList = toJSONString(scheduleList);
        double difRate = getDoubleOrDefault(paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE), 0D) ;  //参数配置：夜班总量和早班总量差额百分比
        double supplyTimePass = getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS), 12D);  //参数配置：库存供应时长小时数
        double difNum = BigDecimalUtil.sub(totalPlanQtyVo.getTotalMidPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()); //夜班和早班计划量差额
        double actualDifRate = Math.abs(difNum) / totalPlanQtyVo.getTotalPlanQty() * 100;  //实际夜班和早班总计划量差额百分比
        if (actualDifRate > difRate) {
            //夜班总量和早班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理
            boolean isMidClassPass = (difNum > 0);  //true：夜班超量，false：早班超量
            if (isMidClassPass) {
                //夜班超量，排程结果按夜班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TqScheduleResultVo::getMidPlanQty)).collect(Collectors.toList());
            } else {
                //早班超量，排程结果按早班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TqScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
            }
            //开始计划量均衡处理
            double lastDifRate = actualDifRate;  //上一次的夜班总量和早班总量的差额百分比
            for (TqScheduleResultVo resultVo : scheduleList) {
                double supplyTime = resultVo.getSupplyTime() == null ? 0D : resultVo.getSupplyTime(); //库存供应时长
                double midPlanQty = resultVo.getMidPlanQty();    //夜班计划量
                double nightPlanQty = resultVo.getNightPlanQty();  //早班计划量

                if (isMidClassPass) {  //夜班超量，早班移到夜班
                    if (midPlanQty == 0 || supplyTime <= supplyTimePass) {
                        //库存供应时长 超过supplyTimePass的， 才允许拆到早班生产
                        continue;
                    }
                    if (new BigDecimal(resultVo.getSpecSize()).compareTo(hardSpecSize) >= 0) {
                        continue;
                    }
                    double increasePlanQty = midPlanQty > toolCapacity? toolCapacity: midPlanQty; // 每次只转移一车
                    double totalMidPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalMidPlanQty(), increasePlanQty);
                    double totalNightPlan = BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), increasePlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalMidPlan, totalNightPlan); //夜班和早班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setMidPlanQty(BigDecimalUtil.sub(midPlanQty, increasePlanQty));
                    resultVo.setNightPlanQty(BigDecimalUtil.add(nightPlanQty, increasePlanQty));
                    //重新计算夜班和早班的总计划量
                    totalPlanQtyVo.setTotalMidPlanQty(totalMidPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                } else {  //早班超量，早班移到夜班
                    if (nightPlanQty == 0) {
                        continue;
                    }
                    if (new BigDecimal(resultVo.getSpecSize()).compareTo(hardSpecSize) >= 0) {
                        continue;
                    }
                    double increasePlanQty = nightPlanQty > toolCapacity? toolCapacity: nightPlanQty; // 每次只转移一车
                    double totalMidPlan =  BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), increasePlanQty);
                    double totalNightPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalNightPlanQty(), increasePlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalMidPlan, totalNightPlan); //夜班和早班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setMidPlanQty(BigDecimalUtil.add(midPlanQty, increasePlanQty));
                    resultVo.setNightPlanQty(BigDecimalUtil.sub(nightPlanQty, increasePlanQty));
                    //重新计算夜班和早班的总计划量
                    totalPlanQtyVo.setTotalMidPlanQty(totalMidPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                }
            }
        }
        this.equilibriumLog(batchNo, oldScheduleList, scheduleList, paramsMap, totalPlanQtyVo);  //添加日志
    }

    /**
     * 均衡日志
     * @param scheduleList
     * @param paramsMap
     * @param totalPlanQtyVo
     */
    private void equilibriumLog(String batchNo, String oldScheduleList, List<TqScheduleResultVo> scheduleList, Map<String, String> paramsMap, TqTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }

    /**
     * 根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<TqScheduleResultVo> scheduleList) {
        int midProduceOrder = 1;  //中班产顺序
        int nightProduceOrder = 1;  //夜班生产顺序
        int dayProduceOrder = 1; //白班生产顺序
        int nextMidProduceOrder = 1; //次日中班生产顺序
        //根据库存供应时长升序排序
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(TqScheduleResultVo::getSupplyTime)).collect(Collectors.toList());
        for(TqScheduleResultVo scheduleVo : scheduleList) {
            Double midPlanQty = scheduleVo.getMidPlanQty();
            Double nightPlanQty = scheduleVo.getNightPlanQty();
            Double dayPlanQty = scheduleVo.getDayPlanQty();
            Double nextMidPlanQty = scheduleVo.getNextMidPlanQty();

            if(midPlanQty > 0) {
                scheduleVo.setMidProduceOrder(midProduceOrder++);
            }
            if(nightPlanQty > 0) {
                scheduleVo.setNightProduceOrder(nightProduceOrder++);
            }
            if(dayPlanQty > 0) {
                scheduleVo.setDayProduceOrder(dayProduceOrder++);
            }
            if(nextMidPlanQty > 0) {
                scheduleVo.setNextMidProduceOrder(nextMidProduceOrder++);
            }

            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                    logSplit("根据库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(TqScheduleResultVo scheduleResultVo, TqMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            log.error("月计划汇总数据为空，物料编号为：", scheduleResultVo.getBeadCode());
            return;
        }
        Double monthFinishQty = monthSurplusVo.getMonthFinishQty();  //月度计划完成量
        Double monthRemainQty = monthSurplusVo.getMonthRemainQty();  //月度计划剩余量
        if(monthRemainQty < closeOutNum) {
            //剩余量小宇等于“临近收尾阈值”，设置收尾提示
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NEED);
        } else {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
        }
        autoScheduleLogService.insertTqScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
                logSplit("剩余量小宇等于“临近收尾阈值”，设置收尾提示","月度计划剩余量：" + monthRemainQty + ",提示收尾阈值：" + closeOutNum, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志

        if(monthFinishQty == 0D) {
            //没有完成量
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
        } else if(monthFinishQty > 0D && monthRemainQty > 0) {
            //完成量大于0，月度计划量也大于0，说明出于生产中
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_ING);
        } else if(monthRemainQty <= 0) {
            //月度计划量小于等于0，说明出于生产完成
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_FINISH);
        }
        autoScheduleLogService.insertTqScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明出于生产中;③月度计划量小于等于0，说明出于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<TqScheduleResultVo> mergeExistSchedule(String batchNo, List<TqScheduleResultVo> autoScheduleList, List<TqScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<TqScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<TqScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(TqScheduleResultVo::getBeadCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(TqScheduleResultVo autoSchedule : autoScheduleList) {
            List<TqScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getBeadCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                TqScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                //重排前的数据如果已经发布过，在重新排程后仍有相应的生产需求，计划量按照重新自动排程的计划量安排；订单号需要和之前发布个mes的订单号一致
                autoSchedule.setOrderNo(existSchedule.getOrderNo());  //订单号
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);  //发布状态修改
                autoSchedule.setMachineId(existSchedule.getMachineId());  //机台沿用重排前的机台
                mergeList.add(autoSchedule);
            } else if(existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                //对应规格重排前已经发布，并且此规格重排前只有多条排程记录（对应了多个机台）。那需要保留重排之前的排产，并且要把此规格重排后的各班的计划量，拼接到备注中
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark2");
                remarkTip = StringUtils.format(remarkTip, stripZeros(autoSchedule.getMidPlanQty()), stripZeros(autoSchedule.getNightPlanQty()), stripZeros(autoSchedule.getDayPlanQty()), stripZeros(autoSchedule.getNextMidPlanQty()));
                for(TqScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getBeadCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<TqScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r->r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }
        return mergeList;
    }

    /**
     * 创建自动排程记录
     *
     * @param scheduleDate 排程日期
     * @param cxBatchNo    对成型批次号
     * @param batchNo      胎圈批次号
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        tqEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表,删除历史外协排程数据
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncTqScheduleToLog(String scheduleDate) {
        tqEngineMapper.syncTqScheduleToLog(scheduleDate);
        tqEngineMapper.deleteTqSchedule(scheduleDate);
        tqEngineMapper.deleteTqAssistSchedule(scheduleDate);

    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyCanMachineMap 定点机台中限制作业map
     * @param specifyNotMachineMap 定点机台中不可作业
     * @param mouthPlateMachineMap 口型板代码map
     */
    private void chooseMachine(List<TqScheduleResultVo> scheduleList, List<TqMachineInfo> allMachineList,
            Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
            Map<String, String> mouthPlateMachineMap) {
        if (CollectionUtil.isEmpty(scheduleList)) {
            return;
        }
        TqScheduleMachineStatistics statistics = new TqScheduleMachineStatistics();
        Map<Long, BigDecimal> midCapacityMap = new HashMap<>(); // 机台夜班已占用产能
        Map<Long, BigDecimal> nightCapacityMap = new HashMap<>(); // 机台白班已占用产能
        Map<String, List<Long>> glueMap = statistics.getGlueMap(); // 胶料分配机台
        Map<String, List<Long>> mouthPlatMap = statistics.getMouthPlatMap(); // 口型版分配机台
        Map<String, Long> plannedMachineMap = tqEngineMachineService
                .getLastDayPlanMachine(CollectionUtil.firstElement(scheduleList).getScheduleDate());// 已排规格，初始为上一个班的规格
//        statistics.setGluePlanMap(scheduleList.stream().filter(r -> StringUtils.isNotEmpty(r.getGlueCode()))
//                .collect(Collectors.groupingBy(TqScheduleResultVo::getGlueCode, Collectors.reducing(BigDecimal.ZERO,
//                        s -> BigDecimalUtils.add(s.getMidPlanQty(), s.getNightPlanQty()), BigDecimal::add))));
//        statistics.setMouthPlatPlanMap(scheduleList.stream().filter(r -> StringUtils.isNotEmpty(r.getMouthPlateCode()))
//                .collect(Collectors.groupingBy(TqScheduleResultVo::getMouthPlateCode, Collectors.reducing(BigDecimal.ZERO,
//                        s -> BigDecimalUtils.add(s.getMidPlanQty(), s.getNightPlanQty()), BigDecimal::add))));

        // 先对排产计划排序
        List<TqScheduleResultVo> chooseMachineScheduleList = scheduleList.stream().sorted(new Comparator<TqScheduleResultVo>() {
            @Override
            public int compare(TqScheduleResultVo o1, TqScheduleResultVo o2) {
                Integer flag1 = specifyCanMachineMap.containsKey(o1.getBeadCode())? 1: 2;
                Integer flag2 = specifyCanMachineMap.containsKey(o2.getBeadCode())? 1: 2;
                int result = flag1.compareTo(flag2);
                if (result != 0) { // 先看哪个有定点机台，有定点机台的优先选机台
                    return result;
                }

                // 如果有上个班有排产的规格优先选机台
                Integer isPlanned1 = plannedMachineMap.containsKey(o1.getBeadCode())? 1: 2;
                Integer isPlanned2 = plannedMachineMap.containsKey(o2.getBeadCode())? 1: 2;
                result = isPlanned1.compareTo(isPlanned2);
                if (result != 0) {
                    return result;
                }

                // 如果定点机台设置一样，则按计划量从大到小的顺序选机台
                BigDecimal planQty1 = BigDecimalUtils.add(o1.getMidPlanQty(), o1.getNightPlanQty());
                BigDecimal planQty2 = BigDecimalUtils.add(o2.getMidPlanQty(), o2.getNightPlanQty());
                return planQty2.compareTo(planQty1);
            }
        }).collect(Collectors.toList());

        // 根据夜班计划分配机台
        for (TqScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            Double midPlanQty = scheduleVo.getMidPlanQty();
            if (midPlanQty == null || midPlanQty <= 0) {
                continue;
            }
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()); // 夜班
            List<TqMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, midCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, statistics, plannedMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            TqMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果早班不作业，则把计划量都转移到夜班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()))) {
                scheduleVo.setMidPlanQty(BigDecimalUtil.add(midPlanQty, scheduleVo.getNightPlanQty()));
                scheduleVo.setNightPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getMidPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            plannedMachineMap.put(scheduleVo.getBeadCode(), machineId);
            this.putMachineId(scheduleVo.getGlueCode(), machineId, glueMap);
            this.putMachineId(scheduleVo.getMouthPlateCode(), machineId, mouthPlatMap);
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap); // 添加日志
        }

        // 剩余没有分配到机台的排程检查早班是否有可分配机台
        for (TqScheduleResultVo scheduleVo : chooseMachineScheduleList) {
            if (StringUtils.isNotEmpty(scheduleVo.getMachineId())) {
                continue;
            }
            String classCode = String.valueOf(OpenMachineClassEnums.CLASS_THREE.getClassIndex()); // 早班
            List<TqMachineInfo> optionalMachineList = this.searchOptionalMachineList(scheduleVo, classCode, nightCapacityMap,
                    allMachineList, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap, statistics, plannedMachineMap); // 检索当班可选机台
            if (CollectionUtil.isEmpty(optionalMachineList)) {
                continue;
            }
            // 如果有匹配机台，则直接取第一个机台赋值
            TqMachineInfo machine = CollectionUtil.firstElement(optionalMachineList);
            Long machineId = machine.getId();
            scheduleVo.setMachineId(String.valueOf(machineId));
            //检查机台，如果夜班不作业，则把计划量都转移到早班
            if (!machine.getOpenMachineClass().contains(String.valueOf(OpenMachineClassEnums.CLASS_TWO.getClassIndex()))) {
                scheduleVo.setNightPlanQty(BigDecimalUtil.add(scheduleVo.getMidPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setMidPlanQty(0D);
            }
            // 占用机台各班产能
            midCapacityMap.put(machineId, midCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getMidPlanQty())));
            nightCapacityMap.put(machineId, nightCapacityMap.getOrDefault(machineId, BigDecimal.ZERO).add(BigDecimalUtils.valueOf(scheduleVo.getNightPlanQty())));
            plannedMachineMap.put(scheduleVo.getBeadCode(), machineId);
            this.putMachineId(scheduleVo.getGlueCode(), machineId, glueMap);
            this.putMachineId(scheduleVo.getMouthPlateCode(), machineId, mouthPlatMap);
            chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap); // 添加日志
        }
    }

    /**
     * 获取指定key分配的机台
     * @param key
     * @param machineMap
     * @return
     */
    private List<Long> getMachineId(String key, Map<String, List<Long>> machineMap) {
        if (StringUtils.isEmpty(key) || machineMap.get(key) == null) {
            return new ArrayList<>(0);
        }
        return machineMap.get(key);
    }

    /**
     * 将指定key分配给特定机台
     * @param key
     * @param machineMap
     * @return
     */
    private void putMachineId(String key, Long machineId, Map<String, List<Long>> machineMap) {
        if (StringUtils.isEmpty(key)) {
            return;
        }
        List<Long> machineList = machineMap.get(key);
        if (machineList == null) {
            machineList = new ArrayList<>();
            machineMap.put(key, machineList);
        }
        if (!machineList.contains(machineId)) {
            machineList.add(machineId);
        }
    }

    /**
     * 检索复合条件的机台
     * 1、根据机台状态、开机班次确定哪个班次可以排<br/>
     * 2、选择机台要看机台已分配产能，优先选择已分配产能较低的机台<br/>
     * 3、可选择机台限制：1、定点机台（包括限制生产和不可生产）。2、施工的寸口必须在机台的“工装信息”范围之内<br/>
     * 4、胎圈规格根据优先级分配到机台。优先级：1、定点限制生产机台；2、可供成型生产时长最低的
     * @param scheduleVo
     * @param classCode
     * @param capacityMap
     * @param allMachineList
     * @param specifyCanMachineMap
     * @param specifyNotMachineMap
     * @param mouthPlateMachineMap
     * @return
     */
    private List<TqMachineInfo> searchOptionalMachineList(TqScheduleResultVo scheduleVo, String classCode,
            Map<Long, BigDecimal> capacityMap, List<TqMachineInfo> allMachineList,
            Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
            Map<String, String> mouthPlateMachineMap, TqScheduleMachineStatistics statistics, Map<String, Long> plannedMachineMap) {
//        List<Long> glueMachineList = this.getMachineId(scheduleVo.getGlueCode(), statistics.getGlueMap()); // 胶料分配机台
//        List<Long> mouthPlatMachineList = this.getMachineId(scheduleVo.getMouthPlateCode(), statistics.getMouthPlatMap()); // 口型版分配机台
//        Map<String, BigDecimal> gluePlanMap = statistics.getGluePlanMap();
//        Map<String, BigDecimal> mouthPlatPlanMap = statistics.getMouthPlatPlanMap();
        BigDecimal dimension = BigDecimalUtils.valueOf(scheduleVo.getDimension()); // 寸口
        String beadCode = scheduleVo.getBeadCode(); // 胎圈代码
        String mouthPlateCode = scheduleVo.getMouthPlateCode(); // 口型板code
        // 定点机台ID列表
        String specifyMachineIds = specifyCanMachineMap.get(beadCode);
        specifyMachineIds = StringUtils.isBlank(specifyMachineIds) ? mouthPlateMachineMap.get(mouthPlateCode)
                : specifyMachineIds; // 从口型板中找机台
        List<String> machineIds;
        // 如果有设置定点机台，需要把非定点全部过滤掉
        if (StringUtils.isNotEmpty(specifyMachineIds)) {
            machineIds = Arrays.asList(specifyMachineIds.split(","));
        } else {
            machineIds = new ArrayList<>(0);
        }
        // 可选机台
        List<TqMachineInfo> optionalMachineList = allMachineList.stream().filter(m -> {// 排除定点不可生产机台
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
        }).filter(m -> { // 如果有设置定点机台，则仅选中定点机台
            if (CollectionUtils.isNotEmpty(machineIds)) {
                return machineIds.contains(String.valueOf(m.getId()));
            }
            return true;
        }).filter(m -> StringUtils.contains(m.getOpenMachineClass(), classCode)) // 对应班次可用
                .filter(m -> {// 寸口需要在工装范围内
                    String toolingInfo = m.getToolingInfo(); // 工装信息
                    if (StringUtils.isNotEmpty(toolingInfo)) {
                        String[] toolingInfoArr = toolingInfo.split("-");
                        BigDecimal minDimension = BigDecimal.ZERO;
                        BigDecimal maxDimension = BigDecimal.ZERO;
                        if (toolingInfoArr.length > 0) {
                            String minDimensionStr = toolingInfoArr[0];
                            minDimension = NumberUtils.isDigits(minDimensionStr) ? new BigDecimal(minDimensionStr)
                                    : BigDecimal.ZERO; // 寸口限制最小
                        }
                        if (toolingInfoArr.length > 1) {
                            String maxDimensionStr = toolingInfoArr[1];
                            maxDimension = NumberUtils.isDigits(maxDimensionStr) ? new BigDecimal(maxDimensionStr)
                                    : BigDecimal.ZERO; // 寸口限制最大
                            if (minDimension.compareTo(maxDimension) > 0) {
                                maxDimension = minDimension;
                            }
                        }
                        if (minDimension.compareTo(dimension) > 0) {
                            return false;
                        }
                        if (maxDimension.compareTo(dimension) < 0) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }).sorted(new Comparator<TqMachineInfo>() {// 按剩余产能升序排序
                    @Override
                    public int compare(TqMachineInfo m1, TqMachineInfo m2) {
                        // 未安排胶料的机台优先
//                        Integer hasProduct1 = capacityMap.containsKey(m1.getId())? 1: 0;
//                        Integer hasProduct2 = capacityMap.containsKey(m2.getId())? 1: 0;
//                        int result = hasProduct1.compareTo(hasProduct2);
//                        if (result != 0) {
//                            return result;
//                        }
//                        Integer hasGlue1 = glueMachineList.contains(m1.getId())? 0: 1;
//                        Integer hasGlue2 = glueMachineList.contains(m2.getId())? 0: 1;
//                        result = hasGlue1.compareTo(hasGlue2);
//                        if (result != 0) {
//                            return result;
//                        }
//                        Integer hasPlant1 = mouthPlatMachineList.contains(m1.getId())? 0: 1;
//                        Integer hasPlant2 = mouthPlatMachineList.contains(m2.getId())? 0: 1;
//                        result = hasPlant1.compareTo(hasPlant2);
//                        if (result != 0) {
//                            return result;
//                        }

                        // 同一个规格优先排在已排过相同规格的机台上
                        Long scheduleMachineId = plannedMachineMap.getOrDefault(beadCode, 0L);
                        Integer hasMachine1 = m1.getId().equals(scheduleMachineId) ? 0 : 1;
                        Integer hasMachine2 = m2.getId().equals(scheduleMachineId) ? 0 : 1;
                        int result = hasMachine1.compareTo(hasMachine2);
                        if (result != 0) {
                            return result;
                        }
                        // 按剩余产能升序排序
                        BigDecimal capacity1 = capacityMap.getOrDefault(m1.getId(), BigDecimal.ZERO);
                        BigDecimal capacity2 = capacityMap.getOrDefault(m2.getId(), BigDecimal.ZERO);
                        result = capacity1.compareTo(capacity2);
                        if (result != 0) {
                            return result;
                        }
                        result = m1.getId().compareTo(m2.getId());
                        return result;
                    }
                }).collect(Collectors.toList());
        return optionalMachineList;
    }

    /**
     * 设置生产线日志
     * @param scheduleVo
     * @param specifyCanMachineMap
     * @param specifyNotMachineMap
     * @param mouthPlateMachineMap
     */
    private void chooseMachineLog(TqScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap, Map<String, String> mouthPlateMachineMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“口型板与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("口型板与机台对应关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(TqScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+12小时；预计库存-1班计划-2班计划大于等于0时，供应时长+24小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=24个小时+（((预计库存-1班计划-2班计划)/3班计划)*12）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getBeadCode() + ",7点预计库存：" + stockQty + "，对应成型一班的计划量：" + 0 + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

        //根据1班计算库存供应时长
        double remnantStock = stockQty;    //剩余库存
//        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass1Plan)) {
//            return;
//        }

        //根据2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, 0);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass2Plan)) {
            return;
        }

        //根据3班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass2Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass3Plan)) {
            return;
        }

        //根据次日1班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass3Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass4Plan)) {
            return;
        }

        //根据次日2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass4Plan);  //重新计算剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass5Plan)) {
            return;
        }
        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getBeadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(TqScheduleResultVo scheduleVo,Double remnantStock, Double classPlan) {
        Double supplyTime = scheduleVo.getSupplyTime();
        supplyTime = (supplyTime == null ? 0D : supplyTime);
        if(BigDecimalUtil.sub(remnantStock, classPlan) >= 0) {
            //如果剩余库存 大于 对应班次库存，则库存供应时长直接+12小时
            scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 12));  //库存供应时长加12小时
            return true;
        } else {
            //如果剩余库存 小于 对应班次库存，则库存供应时长在加上：((剩余库存)/对应班班计划)*12小时
            double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(remnantStock, classPlan), 12);
            supplyTime = supplyTime + BigDecimalUtil.roundDown(classSupplyTime, 1);  //设置库存供应时长向下保留1位小数
            scheduleVo.setSupplyTime(supplyTime);
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getBeadCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算并设置库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaKeys 成型机台code和胎胚代码，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     * @param unitConsume 单耗
     */
    private void computeSupplyTime(TqScheduleResultVo scheduleVo, String quotaKeys, Double stockQty, Double unitConsume) {
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "库存供应时长为空，原因：没找到对应的成型排程记录");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        unitConsume = (unitConsume == null) ? 2 : unitConsume;
        Double quota = cxQuota * unitConsume;   //胎圈定额
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置困存公用时长向下保留2位小数
        }
        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗：" + unitConsume,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(TqScheduleResultVo scheduleVo) {
        int count = 0;
        int addTime = 12;  //每班12小时
        if(scheduleVo.getCxClass1Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass2Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass3Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass4Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass5Plan() == 0) {
            count++;
        }
        return count * addTime;
    }

    /**
     * 计算胎圈中班和夜班计划量
     *
     * @param scheduleVo
     * @param totalPlanQtyVo 计划量总计VO
     * @param lossMap        耗损率map
     * @param paramLossRate  工序参数中配置的耗损率
     * @param mergeThreshold 往前一班合并计划量阈值
     * @param toolCapacity   工装容量
     * @param productStockDay   预生产库存天数
     */
    private void computeTqPlanQty(TqScheduleResultVo scheduleVo, TqTotalPlanQtyVo totalPlanQtyVo,
            Map<String, Double> lossMap, TqScheduleParams params) {
        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 收尾标记默认非收尾
        double paramLossRate = params.getLossRate();
        double productStockDay = params.getProductStockDay();
        double largeDemand = params.getLargeDemand();
        BigDecimal oneProductQty = params.getOneRollNum();
        BigDecimal toolCapacity = BigDecimalUtils.valueOf(params.getToolCapacity());

        String oldScheduleResult = toJSONString(scheduleVo); // 没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); // 库存
        Double lastMidPlanQty = scheduleVo.getLastMidPlanQty(); // 前日白班计划
        double supplyClass = productStockDay; // 预生产库存天数
        Double totalConsumeQty = scheduleVo.getSurplusQty(); // 剩余量
//        Double totalConsumeQty = this.getCxClassPlanCumulative(scheduleVo, OpenMachineClassEnums.CLASS_FOUR); // 总需求量，前四个班
//        totalConsumeQty = BigDecimalUtils.greatest(totalConsumeQty, scheduleVo.getSurplusQty()).doubleValue(); // 取四个半的消耗量与剩余量的最大值

        // 每个早班计算交接班库存 = 上一天交接班库存 + 上一天胎圈计划量总量 - 上一天成型两个班的消耗量
        // 交接班库存要按生产几个小时库存算，例如预生产12小时库存，则交接班库存要 > 当天成型需求量 / 2，最多超过一车（110个）
        // 上一天胎圈计划总量原则上平均分配给两个班，但是早班的计划量要 > 上一天成型两个班的需求量 - 上一天交接班库存
        double cxPlanQty1 = BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());// 第一天成型两个班消耗量
        double cxPlanQty2 = BigDecimalUtil.add(scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan());// 第二天成型两个班消耗量
        double cxPlanQty3 = cxPlanQty2;// 第三天成型两个班消耗量（收尾则取次日夜班成型计划，如果未收尾暂时先预计与第二天一样）
        double classStock1 = stockQty; // 第一天交接班库存，初始为当天库存
        double classStock2 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty2, supplyClass), 0); // 第二天交接班库存，第二天成型两个班的消耗量 * 预生产天数
        // 计算第一天相关数值
        double tqPlanQty1 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock2, classStock1), cxPlanQty1);// 第一天胎圈计划量 = 第二天交接班库存 - 第一天交接班库存 + 第一天成型两个班的消耗量
        tqPlanQty1 = tqPlanQty1 > 0? tqPlanQty1: 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
        double tqClass1PlanQty1 = lastMidPlanQty;// 第一天早班计划 = 前日早班计划
        double tqClass2PlanQty1 = BigDecimalUtil.sub(tqPlanQty1, tqClass1PlanQty1);// 第一天夜班计划 = 等于第一天胎圈计划 - 第一天早班计划
//        tqClass2PlanQty1 = this.limitProductQty(tqClass2PlanQty1, oneProductQty); // 控制计划量不要低于最低生产量
        tqClass2PlanQty1 = this.planQtyRounding(scheduleVo, tqClass2PlanQty1, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_TWO); // 整车取整
        double midPlanQty = tqClass2PlanQty1; // 夜班计划
        scheduleVo.setMidPlanQty(midPlanQty);
        // 根据排好的计划量重算相关数值
        tqPlanQty1 = BigDecimalUtil.add(tqClass1PlanQty1, tqClass2PlanQty1); // 刷新第一天胎圈计划量
        classStock2 = BigDecimalUtil.sub(BigDecimalUtil.add(tqPlanQty1, classStock1), cxPlanQty1);// 刷新第二天交接班库存
        scheduleVo.setClassStock(classStock2); // 保存交接班库存，用于均衡计算
        scheduleVo.setSupplyDemandRatio(BigDecimalUtil.div(classStock2, cxPlanQty2, 4)); // 计算交接班库存供需比率，第二天交接班库存 / 成型第二天需求量，用于均衡计算

        // 计算第二天相关数值
        double classStock3 = BigDecimalUtil.roundDown(BigDecimalUtil.mul(cxPlanQty3, supplyClass), 0); // 第三天交接班库存，第三天成型两个班的消耗量 * 预生产天数
        double tqPlanQty2 = BigDecimalUtil.add(BigDecimalUtil.sub(classStock3, classStock2), cxPlanQty2);// 第二天胎圈计划量 = 第三天交接班库存 - 第二天交接班库存 + 第二天成型两个班的消耗量
        tqPlanQty2 = tqPlanQty2 > 0? tqPlanQty2: 0D; // 上一天交接班库存过多会计算成负数，需要处理成0
//        if (!isCloseOutSpec && classStock2 >= cxPlanQty2 && tqPlanQty2 <= toolCapacity.doubleValue()) {
//            tqPlanQty2 = 0D; // 非收尾、交接班库存满足一天需求，且需求量少于一车，则今天暂不生产
//        } else if (!isCloseOutSpec && classStock2 < cxPlanQty2 && tqPlanQty2 <= toolCapacity.doubleValue()) {
//            tqPlanQty2 = oneProductQty.doubleValue(); // 非收尾、交接班库存不足，且需求量少于一车的，计划量补够最低生产量
//        }
        double tqClass1PlanQty2 = BigDecimalUtil.sub(cxPlanQty2, classStock2); // 早班先补交接班库存缺口
        double class3lackPlanQty = BigDecimalUtil.sub(scheduleVo.getCxClass3Plan(), classStock2); // 早班库存缺口
        if (cxPlanQty3 >= largeDemand) { // 如果计划量是大需求量，则直接早夜班计划量对半分
            tqClass1PlanQty2 = BigDecimalUtil.div(cxPlanQty3, 2, 0);
        } else if (class3lackPlanQty > 0) { // 如果早班有库存缺口，优先把早班缺口补上
            tqClass1PlanQty2 = class3lackPlanQty;
        }
        // 如果中班夜班有任意个班计划量不足一个工装，则合并成一个班内完成
        if (tqClass1PlanQty2 <= toolCapacity.doubleValue() || BigDecimalUtil.sub(tqPlanQty2, tqClass1PlanQty2) <= toolCapacity.doubleValue()) {
            tqClass1PlanQty2 = tqPlanQty2;
        }
//        tqClass1PlanQty2 = this.limitProductQty(tqClass1PlanQty2, oneProductQty); // 控制计划量不要低于最低生产量
        tqClass1PlanQty2 = this.planQtyRounding(scheduleVo, tqClass1PlanQty2, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_THREE); // 整车取整
        double nightPlanQty = tqClass1PlanQty2; // 早班计划
        scheduleVo.setNightPlanQty(nightPlanQty);
        double tqClass2PlanQty2 = BigDecimalUtil.sub(tqPlanQty2, tqClass1PlanQty2);// 第二天夜班计划 = 等于第二天胎圈计划 - 第二天早班计划
//        tqClass2PlanQty2 = this.limitProductQty(tqClass2PlanQty2, oneProductQty); // 控制计划量不要低于最低生产量
        double dayPlanQty = this.planQtyRounding(scheduleVo, tqClass2PlanQty2, toolCapacity, totalConsumeQty,
                OpenMachineClassEnums.CLASS_FOUR); // 次日夜班计划 = 第二天夜班计划整车取整
        scheduleVo.setDayPlanQty(dayPlanQty);

        double nextMidPlanQty = 0;        /*
        // 计算夜班计划量 = 成型前三个班累计消耗量 - 早班胎圈计划 - 胎圈库存
        double midPlanQty = BigDecimalUtil.sub(BigDecimalUtil
                .sub(this.getCxClassPlanCumulative(scheduleVo, OpenMachineClassEnums.CLASS_THREE), lastMidPlanQty),
                stockQty);
        midPlanQty = this.planQtyRounding(scheduleVo, midPlanQty, toolCapacity, totalConsumeQty, isCloseOutSpec,
                OpenMachineClassEnums.CLASS_TWO); // 整车取整
        scheduleVo.setMidPlanQty(midPlanQty); // 先设置进对象，后续计算要使用

        // 计算早班计划量 = 成型前四个班累计消耗量 - 胎圈前2个班的累计已排计划量 - 胎圈库存
        double nightPlanQty = BigDecimalUtil
                .sub(BigDecimalUtil.sub(this.getCxClassPlanCumulative(scheduleVo, OpenMachineClassEnums.CLASS_FOUR),
                        this.getTqClassPlanCumulative(scheduleVo, OpenMachineClassEnums.CLASS_TWO)), stockQty);
        nightPlanQty = this.planQtyRounding(scheduleVo, nightPlanQty, toolCapacity, totalConsumeQty, isCloseOutSpec,
                OpenMachineClassEnums.CLASS_THREE); // 整车取整
        scheduleVo.setNightPlanQty(nightPlanQty);

        // 次日夜班计划量 = 成型五个班累计消耗量 - 胎圈前3个班的累计已排计划量 - 胎圈库存
        double dayPlanQty = BigDecimalUtil.sub(BigDecimalUtil.sub(totalConsumeQty,
                this.getTqClassPlanCumulative(scheduleVo, OpenMachineClassEnums.CLASS_THREE)), stockQty);
        dayPlanQty = this.planQtyRounding(scheduleVo, dayPlanQty, toolCapacity, totalConsumeQty, isCloseOutSpec,
                OpenMachineClassEnums.CLASS_FOUR); // 整车取整
        scheduleVo.setDayPlanQty(dayPlanQty);
        // 次日中班(16点-24点)计划量 = 成型次日2班消耗胎圈的计划量
        */

        /*
        //根据库存重新计算中班计划量：（原中班计划量>库存） ？（ 原中班计划量-库存） ： 0
        midPlanQty = (initMidPlanQty > stockQty) ? BigDecimalUtil.sub(midPlanQty, stockQty) : 0;
        //根据库存重新计算夜班计划量：（原中班计划量>库存） ？原夜班计划量 ： （原中班计划量+原夜班计划量 - 库存）
        nightPlanQty = (initMidPlanQty > stockQty) ? nightPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, nightPlanQty), stockQty);
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;
        //根据库存重新计算次日夜班计划量：（原夜班计划量+早班计划量 > 库存） ？原次日夜班计划量 ： （原夜班计划量+原早班计划量+原白班计划量 - 库存）
        dayPlanQty = (BigDecimalUtil.add(initMidPlanQty, initNightPlanQty) > stockQty) ? dayPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, dayPlanQty), stockQty);
        dayPlanQty = (dayPlanQty < 0) ? 0D : dayPlanQty;
        //根据库存重新计算次日中班计划量：（原中班计划量+原夜班计划量+原白班计划量 > 库存） ？次日中班计划量 ： （原中班计划量+原夜班计划量+原白班计划量+次日中班计划量 - 库存）
        nextMidPlanQty = (BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, initDayPlanQty) > stockQty) ? nextMidPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, dayPlanQty, initDayPlanQty), stockQty);
        nextMidPlanQty = (nextMidPlanQty < 0) ? 0D : nextMidPlanQty;
        */

        /*//为了防止二次投产，把后面一班的计划量往前面一班合并（这块暂时注释掉，待确定）
        //如果夜班计划量>0并且夜班的计划量没有超过参数配置的阈值，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        if(nightPlanQty > 0 && dayPlanQty <= mergeThreshold) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, dayPlanQty);
            dayPlanQty = 0D;
        }
        //如果中班计划量>0并且夜班的计划量没有超过参数配置的阈值，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        if (midPlanQty > 0 && nightPlanQty <= mergeThreshold) {
            midPlanQty = BigDecimalUtil.add(midPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        } */

        String machineId = scheduleVo.getMachineId();  //机台id
        double lossRate = 0;
        //只有单个机台的时候，自动排程才计算耗损率
        if(StringUtils.isNotBlank(machineId) && !machineId.contains(",")) {
            //计划量要加上耗损量
            lossRate = tqEngineLossService.getLossRate(scheduleVo.getBeadCode(), scheduleVo.getMachineId(), lossMap, paramLossRate);
            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            nextMidPlanQty = BigDecimalUtil.add(nextMidPlanQty, BigDecimalUtil.mul(nextMidPlanQty, lossRate));
        }

        scheduleVo.setMidPlanQty(BigDecimalUtil.roundUp(midPlanQty,0));
        scheduleVo.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty,0));
        scheduleVo.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty,0));
        scheduleVo.setNextMidPlanQty(BigDecimalUtil.roundUp(nextMidPlanQty,0));

        //计算中班总计划量 和 夜班总计划量
        //计算各班总计划量
        totalPlanQtyVo.setTotalMidPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), midPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNextMidPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNextMidPlanQty(), nextMidPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), totalPlanQtyVo.getTotalNightPlanQty(), totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNextMidPlanQty()));
        this.computeTqPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate);  //添加日志
    }

    /**
     * 控制生产量不要小于最低生产量
     * @param planQty
     * @param oneProductQty
     * @return
     */
    private double limitProductQty(double planQty, BigDecimal oneProductQty) {
        if (planQty > 0 && planQty < oneProductQty.doubleValue()) { // 如果有生产量，则不要小于最低生产量
            planQty = oneProductQty.doubleValue();
        }
        return planQty;
    }

    /**
     * 计划量取整
     * @param scheduleVo        排产记录
     * @param planQty           原计划量
     * @param toolCapacity      一个胎圈车可以放的胎圈量
     * @param totalConsumeQty   总需期间量，用于判断收尾规格是否超量
     * @param isCloseOutSpec    是否收尾
     * @param classNum          当前班次，从前日早班开始
     * @return
     */
    private double planQtyRounding(TqScheduleResultVo scheduleVo, double planQty, BigDecimal toolCapacity,
            Double totalConsumeQty, OpenMachineClassEnums classNum) {
        if (planQty <= 0D) { // 不排的情况直接返回0即可
            return 0D;
        }
        double roudingPlanQty = BigDecimalUtils.valueOf(planQty).divide(toolCapacity, 0, RoundingMode.CEILING)
                .multiply(toolCapacity).doubleValue(); // 取整车
        if (classNum == null) {
            return roudingPlanQty;
        }
        OpenMachineClassEnums lastClass = classNum;
        if (classNum != OpenMachineClassEnums.CLASS_ONE) { // 取出上一班的班次
            Integer classIndex = classNum.getClassIndex();
            lastClass = OpenMachineClassEnums.getClassEnums(classIndex - 1);
        }
        double lastPlanCumulative = this.getTqClassPlanCumulative(scheduleVo, lastClass); // 到上个班次班次班的累计已排计划量
        double newPlanQty = BigDecimalUtil.add(lastPlanCumulative, roudingPlanQty, scheduleVo.getStockQty()); // 库存+已排计划+本班计划
        double result = roudingPlanQty;
        // 如果库存+计划已经超过总需求量，则本班的计划量要限制住不允许超量
        if (newPlanQty > totalConsumeQty) {
            Double increaseMidPlanQty = BigDecimalUtil.sub(newPlanQty, totalConsumeQty);
            result = BigDecimalUtil.sub(roudingPlanQty, increaseMidPlanQty);
            result = result > 0? result: 0D;
        }
        scheduleVo.setCloseOutSpecFlag(newPlanQty >= totalConsumeQty? ApsConstant.STATUS_ENABLE: ApsConstant.STATUS_DISABLE);
        return result;
    }

    /**
     * 获取各班计划量的累计值（从前日早班开始）
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getTqClassPlanCumulative(TqScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getLastMidPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getMidPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, scheduleVo.getNightPlanQty());
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, scheduleVo.getDayPlanQty());
    }

    /**
     * 获取各班需求量的累计值（从前日早班开始）
     * @param scheduleVo
     * @param classNum
     * @return
     */
    private Double getCxClassPlanCumulative(TqScheduleResultVo scheduleVo, OpenMachineClassEnums classNum) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型前日早班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型夜班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型早班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日夜班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日早班的计划量
        Double planQty = 0D;
        if (classNum == null) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass1Plan);
        if (classNum == OpenMachineClassEnums.CLASS_ONE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass2Plan);
        if (classNum == OpenMachineClassEnums.CLASS_TWO) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass3Plan);
        if (classNum == OpenMachineClassEnums.CLASS_THREE) {
            return planQty;
        }
        planQty = BigDecimalUtil.add(planQty, cxClass4Plan);
        if (classNum == OpenMachineClassEnums.CLASS_FOUR) {
            return planQty;
        }
        return BigDecimalUtil.add(planQty, cxClass5Plan);
    }

    private void computeTqPlanQtyLog(String oldScheduleResult, TqScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("计算中班计划量midPlanQty= （成型一班消耗胎圈计划量cxClass1Plan + 成型二班消耗胎圈计划量CxClass2Plan）").append(division);
        logDetail.append("计算夜班计划量nightPlanQty = 成型三班消耗胎圈计划量cxClass3Plan ").append(division);
        logDetail.append("计算夜班计划量dayPlanQty = 成型次日一班消耗胎圈计划量cxClass4Plan ").append(division);
        logDetail.append("计算次日中班计划量nextMidPlanQty = 成型次日二班消耗胎圈计划量cxClass5Plan ").append(division);
        logDetail.append("根据库存重新计算中班计划量midPlanQty：根据库存重新计算中班计划量midPlanQty：（原中班计划量midPlanQty>库存stockQty） ？（ 原中班计划量-库存）：0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量midPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ：（原中班计划量midPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("根据库存重新计算白班计划量dayPlanQty：（原中班计划量+夜班计划量 > 库存） ？原白班计划量 ：原中班计划量+原夜班计划量+原白班计划量 - 库存）").append(division);
        logDetail.append("根据库存重新计算次日中班计划量nextMidPlanQty：（原中班计划量+原夜班计划量+原白班计划量 > 库存）？原白班计划量 ：（原中班计划量+原夜班计划量+原白班计划量+次日中班计划量 - 库存）").append(division);
        logDetail.append("胎圈耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 胎圈代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
//        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）").append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertTqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.TQ_BATCH_NO_PREFIX + scheduleDate);
    }

    /**
     * 创建工单号
     * @param batchNo 批次号
     * @return
     */
    private String createOrderNo(String batchNo) {
        return incrementService.getSequence4(batchNo);
    }

    /**
     * 获取工序参数map
     * @return
     */
    private TqScheduleParams loadParams() {
        List<TqParamsVo> list = this.tqEngineMapper.listTqParams();
        Map<String, String> paramsMap = list.stream()
                .collect(Collectors.toMap(TqParamsVo::getParamCode, TqParamsVo::getParamValue));
        TqScheduleParams params = new TqScheduleParams();

        params.setProductionStage(paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE));
        params.setLossRate(getDouble(paramsMap.get(EngineConstants.LOSS_RATE)));
        params.setMergeThreshold(getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD)));
        params.setCloseOutNum(getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));
        params.setToolCapacity(getDouble(paramsMap.getOrDefault(EngineConstants.TOOL_CAPACITY, DEFAULT_TOOL_CAPACITY)));
        BigDecimal productStockHour = new BigDecimal(paramsMap.getOrDefault(EngineConstants.PRODUCT_STOCK_HOUR, DEFAULT_PRODUCT_STOCK_HOUR));
        params.setProductStockDay(productStockHour.divide(HOUR24, 2, RoundingMode.HALF_UP).doubleValue());
        params.setLargeDemand(getDouble(paramsMap.getOrDefault(EngineConstants.LARGE_DEMAND, DEFAULT_LARGE_DEMAND)));
        params.setBigSizeSpec(BigDecimalUtils.valueOf(paramsMap.getOrDefault(EngineConstants.BIG_SIZE_SPEC, DEFAULT_BIG_SIZE_SPEC)));
        params.setMinPlanQty(getDouble(paramsMap.getOrDefault(EngineConstants.MIN_PLAN_QTY, DEFAULT_MIN_PLAN_QTY)));
        params.setStockLossRate(getDouble(paramsMap.getOrDefault(EngineConstants.STOCK_LOSS_RATE, "0")));
        params.setEqualShareThreshold(new BigDecimal(paramsMap.getOrDefault(EngineConstants.EQUAL_SHARE_THRESHOLD, DEFAULT_EQUAL_SHARE_THRESHOLD))); // 平分阈值
        params.setClassStockReference(getDouble(paramsMap.getOrDefault(EngineConstants.CLASS_STOCK_REFERENCE, DEFAULT_CLASS_STOCK_REFERENCE)));
        params.setOneRollNum(new BigDecimal(paramsMap.getOrDefault(EngineConstants.ONE_ROLL_NUM, DEFAULT_ONE_ROLL_NUM)));


        return params;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param mouthPlateMachineMap 口型板和机台关系集合
     * @param specifyCanMachineMap 定点机台和机台的限制作业集合
     * @param specifyNotMachineMap 定点集合和机台的不可作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param params 参数设置集合
     */
    private void baseDataLog(String batchNo,Map<String, String> mouthPlateMachineMap, Map<String, String> specifyCanMachineMap,
                             Map<String, String> specifyNotMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, TqMonthSurplusVo> monthSurplus, TqScheduleParams params) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("口型板和机台关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(params)).append(division);
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

    @Override
    public void batchUpdateBatchNoAndOrderNo(String scheduleDate) {
        List<TqScheduleResultVo> scheduleResultVoList = tqEngineMapper.listTqEnginSchedule(scheduleDate);
        //查询当前排程的批次号
        String batchNo = tqEngineMapper.getTqCurrentBatchNo(scheduleDate);
        if (StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“或者做的批量导入（需要删掉已排的数据），那么自己生成一个排程批次号
            //排程批次号
            batchNo = this.createBatchNo(scheduleDate);
            //创建自动排程记录
            this.createScheduleRecord(scheduleDate, "", batchNo);
        }
        for (TqScheduleResultVo scheduleResult : scheduleResultVoList) {
            //批次号
            scheduleResult.setBatchNo(batchNo);
            //工单号
            String orderNo = this.createOrderNo(batchNo);
            scheduleResult.setOrderNo(orderNo);
        }

        if (CollectionUtils.isNotEmpty(scheduleResultVoList)) {
            tqEngineMapper.batchUpdateBatchNoAndOrderNo(scheduleResultVoList);
        }
    }
}
