package com.zlt.aps.tq.engine.service.impl;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.service.impl.IncrementService;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.engine.mapper.TqEngineMapper;
import com.zlt.aps.tq.engine.service.*;
import com.zlt.aps.tq.engine.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
    private AutoScheduleLogService autoScheduleLogService;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

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
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        List<TqScheduleResultVo> scheduleList = tqEngineMapper.statTqScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 胎圈胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据成型排程记录 统计出 胎圈胶排程记录基础数据 为空");
            autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
        }
        //过滤掉成型5个班的计划量都为0的数据
        scheduleList = scheduleList.stream().filter(s -> (s.getCxClass1Plan()+s.getCxClass2Plan()+s.getCxClass3Plan()+s.getCxClass4Plan()+s.getCxClass5Plan())>0).collect(Collectors.toList());
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "根据成'型排程记录'统计出胎圈胶排程记录基础数据",  toJSONString(scheduleList));
        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> mouthPlateMachineMap = tqEngineMachineService.getMouthPlateMachineMap(); //获得口型板代码map
        Map<String, String> specifyCanMachineMap = tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得胎圈代码和定点机台的限制作业map
        Map<String, String> specifyNotMachineMap = tqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得胎圈代码和定点机台的不可作业map
        Map<String, Double> planStockMap = tqEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算胎圈16点预计库存
        Map<String, Double> lossRateMap = tqEngineLossService.getLossRateMap();   //损耗率map
        Map<String, TqMonthSurplusVo> monthSurplus = tqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        this.baseDataLog(batchNo, mouthPlateMachineMap, specifyCanMachineMap, specifyNotMachineMap, planStockMap, lossRateMap, monthSurplus, paramsMap); //把基础数据假如到日志中
        TqTotalPlanQtyVo totalPlanQtyVo = new TqTotalPlanQtyVo();  //胎圈中班和夜班总计划量Vo
        for (TqScheduleResultVo scheduleVo : scheduleList) {
            cxBatchNo = scheduleVo.getCxBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);

            this.chooseMachine(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);  //选择生产线
            scheduleVo.setStockQty(planStockMap.getOrDefault(scheduleVo.getBeadCode(), 0D));  //16点预计库存
            autoScheduleLogService.insertTqScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getStockQty());  //库存供应时长
            this.computeTqPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, getDouble(paramsMap.get(EngineConstants.LOSS_RATE)), getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD)));  //计算胎圈中班和夜班计划量
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getBeadCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(username);
        }
//        this.equilibrium(batchNo, scheduleList, paramsMap, totalPlanQtyVo);  //中班和夜班计排程计划量均衡处理
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
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        Map<String, TqScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, beadCodes, productionStage);  //根据胎圈代码查询对应的胎圈基础信息
        Map<String, Double> planStockMap = tqEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算胎圈16点预计库存
        Map<String, TqMonthSurplusVo> monthSurplus = tqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map：" + paramsMap));  //添加日志

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
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getBeadCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            schedule.setUnitConsume(2D);  //单耗：1条胎对应2个钢丝圈
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
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

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
     * 中班和夜班计排程计划量均衡处理
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 胎圈中班和夜班总计划量Vo
     */
    private void equilibrium(String batchNo, List<TqScheduleResultVo> scheduleList, Map<String, String> paramsMap, TqTotalPlanQtyVo totalPlanQtyVo) {
        String oldScheduleList = toJSONString(scheduleList);
        double difRate = getDoubleOrDefault(paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE), 100D) ;  //参数配置：中班总量和夜班总量差额百分比
        double supplyTimePass = getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS), 12D);  //参数配置：库存供应时长小时数
        double difNum = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()); //中班和夜班计划量差额
        double actualDifRate = Math.abs(difNum) / totalPlanQtyVo.getTotalPlanQty() * 100;  //实际中班和夜班总计划量差额百分比
        if (actualDifRate > difRate) {
            //中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理
            boolean isDayClassPass = (difNum > 0);  //true：中班超量，false：夜班超量
            if (isDayClassPass) {
                //中班超量，排程结果按中班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TqScheduleResultVo::getDayPlanQty)).collect(Collectors.toList());
            } else {
                //夜班超量，排程结果按夜班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(TqScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
            }
            //开始计划量均衡处理
            double lastDifRate = actualDifRate;  //上一次的中班总量和夜班总量的差额百分比
            for (TqScheduleResultVo resultVo : scheduleList) {
                double supplyTime = resultVo.getSupplyTime() == null ? 0D : resultVo.getSupplyTime(); //库存供应时长
                double dayPlanQty = resultVo.getDayPlanQty();    //中班计划量
                double nightPlanQty = resultVo.getNightPlanQty();  //夜班计划量

                if (isDayClassPass) {  //中班超量，中班移到夜班
                    if (dayPlanQty == 0 || supplyTime <= supplyTimePass) {
                        //库存供应时长 超过supplyTimePass的， 才允许拆到夜班生产
                        continue;
                    }
                    double totalDayPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty);
                    double totalNightPlan = BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), dayPlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalDayPlan, totalNightPlan); //中班和夜班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setDayPlanQty(0D);
                    resultVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    //重新计算中班和夜班的总计划量
                    totalPlanQtyVo.setTotalDayPlanQty(totalDayPlan);
                    totalPlanQtyVo.setTotalNightPlanQty(totalNightPlan);
                    lastDifRate = newDifRate;
                } else {  //夜班超量，夜班移到中班
                    if (nightPlanQty == 0) {
                        continue;
                    }
                    double totalDayPlan =  BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), nightPlanQty);
                    double totalNightPlan = BigDecimalUtil.sub(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty);
                    double newDifNum = BigDecimalUtil.sub(totalDayPlan, totalNightPlan); //中班和夜班计划量差额
                    double newDifRate = Math.abs(newDifNum) / totalPlanQtyVo.getTotalPlanQty() * 100;   //计算新的差额率
                    if(newDifRate >= lastDifRate) {
                        //如果调整后的差额率，比上次调整的高，那上次调整的数据是最均衡的。均衡处理全部结束
                        break;
                    }

                    resultVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    resultVo.setNightPlanQty(0D);
                    //重新计算中班和夜班的总计划量
                    totalPlanQtyVo.setTotalDayPlanQty(totalDayPlan);
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
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中不可作业
     * @param mouthPlateMachineMap
     */
    private void chooseMachine(TqScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap, Map<String, String> mouthPlateMachineMap) {
        String beadCode = scheduleVo.getBeadCode();  //胎圈代码
        String mouthPlateCode = scheduleVo.getMouthPlateCode();  //口型板code

        String machineIds = specifyCanMachineMap.get(beadCode);
        machineIds = StringUtils.isBlank(machineIds) ? mouthPlateMachineMap.get(mouthPlateCode) : machineIds;  //从口型板中找机台
        //过滤掉 定点机台中 设置的不可作业的机台
        String notMachineIds = specifyNotMachineMap.get(beadCode);  //定点机台中不可作业的机台
        if(StringUtils.isNotBlank(machineIds) && StringUtils.isNotBlank(notMachineIds)) {
            List<String> machineList = new ArrayList<>();
            String[] machineArray = machineIds.split(",");
            List<String> notMachineIdList = Arrays.asList(notMachineIds.split(","));
            for(int i = 0;i < machineArray.length; i++) {
                if(!notMachineIdList.contains(machineArray[i])) {
                    machineList.add(machineArray[i]);  //此机台不在 定点机台的 不可作业集合中
                }
            }
            machineIds = String.join(",", machineList);
        }
        scheduleVo.setMachineId(machineIds == null ? "" : machineIds);
        chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, mouthPlateMachineMap);  //添加日志
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
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getBeadCode() + "，16点预计库存：" + stockQty + "，对应成型一班的计划量：" + cxClass1Plan + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

        //根据1班计算库存供应时长
        double remnantStock = stockQty;    //剩余库存
        if(!oneComputeSupplyTime(scheduleVo, remnantStock, cxClass1Plan)) {
            return;
        }

        //根据2班计算库存供应时长
        remnantStock = BigDecimalUtil.sub(remnantStock, cxClass1Plan);  //重新计算剩余库存
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
            //如果剩余库存 大于 对应班次库存，则库存供应时长直接+8小时
            scheduleVo.setSupplyTime(BigDecimalUtil.add(supplyTime, 8));  //库存供应时长加8小时
            return true;
        } else {
            //如果剩余库存 小宇 对应班次库存，则库存供应时长在加上：((剩余库存)/对应班班计划)*8小时
            double classSupplyTime = BigDecimalUtil.mul(BigDecimalUtil.div(remnantStock, classPlan), 8);
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
        int addTime = 8;  //每班8小时
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
     * @param scheduleVo
     * @param totalPlanQtyVo 计划量总计VO
     * @param lossMap 耗损率map
     * @param paramLossRate 工序参数中配置的耗损率
     * @param mergeThreshold 往前一班合并计划量阈值
     */
    private void computeTqPlanQty(TqScheduleResultVo scheduleVo, TqTotalPlanQtyVo totalPlanQtyVo, Map<String, Double> lossMap, double paramLossRate, double mergeThreshold) {
        String oldScheduleResult = toJSONString(scheduleVo); //没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); //库存

        //计算中班计划量 = （成型一班消耗胎圈计划量 + 成型二班消耗胎圈计划量）
        double midPlanQty = BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan());
        double initMidPlanQty = midPlanQty;
        //夜班班计划量 = 成型3班消耗胎圈的计划量
        double nightPlanQty = scheduleVo.getCxClass3Plan();
        double initNightPlanQty = nightPlanQty;
        //计算白班计划量 = 成型次日1班消耗胎圈的计划量
        double dayPlanQty = scheduleVo.getCxClass4Plan();
        double initDayPlanQty = dayPlanQty;
        //次日中班(16点-24点)计划量 = 成型次日2班消耗胎圈的计划量
        double nextMidPlanQty = scheduleVo.getCxClass5Plan();

        //根据库存重新计算中班计划量：（原中班计划量>库存） ？（ 原中班计划量-库存） ： 0
        midPlanQty = (initMidPlanQty > stockQty) ? BigDecimalUtil.sub(midPlanQty, stockQty) : 0;
        //根据库存重新计算夜班计划量：（原中班计划量>库存） ？原夜班计划量 ： （原中班计划量+原夜班计划量 - 库存）
        nightPlanQty = (initMidPlanQty > stockQty) ? nightPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, nightPlanQty), stockQty);
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;
        //根据库存重新计算白班计划量：（原中班计划量+夜班计划量 > 库存） ？原白班计划量 ： （原中班计划量+原夜班计划量+原白班计划量 - 库存）
        dayPlanQty = (BigDecimalUtil.add(initMidPlanQty, initNightPlanQty) > stockQty) ? dayPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, dayPlanQty), stockQty);
        dayPlanQty = (dayPlanQty < 0) ? 0D : dayPlanQty;
        //根据库存重新计算次日中班计划量：（原中班计划量+原夜班计划量+原白班计划量 > 库存） ？次日中班计划量 ： （原中班计划量+原夜班计划量+原白班计划量+次日中班计划量 - 库存）
        nextMidPlanQty = (BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, initDayPlanQty) > stockQty) ? nextMidPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, dayPlanQty, initDayPlanQty), stockQty);
        nextMidPlanQty = (nextMidPlanQty < 0) ? 0D : nextMidPlanQty;

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
    private Map<String, String> getParamsMap() {
        List<TqParamsVo> list = this.tqEngineMapper.listTqParams();
        Map<String, String> map = list.stream().collect(Collectors.toMap(TqParamsVo::getParamCode, TqParamsVo::getParamValue));
        return map == null ? new HashMap<>() : map;
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
     * @param paramsMap 参数设置集合
     */
    private void baseDataLog(String batchNo,Map<String, String> mouthPlateMachineMap, Map<String, String> specifyCanMachineMap,
                             Map<String, String> specifyNotMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, TqMonthSurplusVo> monthSurplus,Map<String, String> paramsMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("口型板和机台关系集合：" + toJSONString(mouthPlateMachineMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(paramsMap)).append(division);
        autoScheduleLogService.insertTqScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

}