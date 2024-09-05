package com.zlt.aps.gsq.engine.service.impl;

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
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import com.zlt.aps.gsq.engine.mapper.GsqEngineMapper;
import com.zlt.aps.gsq.engine.service.*;
import com.zlt.aps.gsq.engine.vo.*;
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
public class GsqEngineServiceImpl implements GsqEngineService {

    @Resource
    private GsqEngineMapper gsqEngineMapper;
    @Resource
    private GsqEngineStockService gsqEngineStockService;
    @Resource
    private GsqEngineMachineService gsqEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private GsqEngineLossService gsqEngineLossService;
    @Resource
    private GsqEngineMonthSurplusService gsqEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 钢丝圈胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoGsqSchedule(String scheduleDate) {
        String username = SecurityUtils.getUsername(); //用户账号
        String tqBatchNo = "";  //胎圈批次号
        String batchNo = this.createBatchNo(scheduleDate);  //钢丝圈排程批次号
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        List<GsqScheduleResultVo> scheduleList = gsqEngineMapper.statGsqScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 钢丝圈胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据胎圈排程记录 统计出 钢丝圈胶排程记录基础数据 为空");
            autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：胎圈排程数据为空，或没有在施工信息中找到对应的物料！"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.gsq.tip"));
        }
        //过滤掉钢丝圈3个班的计划量都为0（扣减库存之前的计划量）的数据
        scheduleList = scheduleList.stream().filter(s -> (s.getMidPlanQty()+s.getNightPlanQty()+s.getDayPlanQty())>0).collect(Collectors.toList());
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "根据成'型排程记录'统计出钢丝圈胶排程记录基础数据",  toJSONString(scheduleList));
        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> twiningDiscMachineMap = gsqEngineMachineService.getTwiningDiscMachineMap(); //获得钢丝圈代码和缠绕盘map (key = 规格尺寸~排列方式 )
        Map<String, String> twiningDiscMap = gsqEngineMachineService.getTwiningDiscMap(scheduleDate); //获得钢丝圈代码和缠绕盘（value = 规格尺寸~排列方式）map
        Map<String, String> specifyCanMachineMap = gsqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得钢丝圈代码和定点机台的限制作业map
        Map<String, String> specifyNotMachineMap = gsqEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_NOT); //获得钢丝圈代码和定点机台的不可作业map
        Map<String, Double> planStockMap = gsqEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算钢丝圈16点预计库存
        Map<String, Double> lossRateMap = gsqEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> quotaParamMap = this.getQuotaParamMap(scheduleDate, productionStage);   //获取钢丝圈对应的成型胎胚code和机台code
        Map<String, GsqMonthSurplusVo> monthSurplus = gsqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        this.baseDataLog(batchNo, twiningDiscMachineMap, twiningDiscMap, specifyCanMachineMap, specifyNotMachineMap, planStockMap, lossRateMap, monthSurplus, paramsMap); //把基础数据假如到日志中
        GsqTotalPlanQtyVo totalPlanQtyVo = new GsqTotalPlanQtyVo();  //钢丝圈中班和夜班总计划量Vo
        for (GsqScheduleResultVo scheduleVo : scheduleList) {
            tqBatchNo = scheduleVo.getTqBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);

            this.chooseMachine(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMachineMap, twiningDiscMap);  //选择生产线
            scheduleVo.setStockQty(planStockMap.getOrDefault(scheduleVo.getSteelRingCode(), 0D));  //16点预计库存
            autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getStockQty());  //库存供应时长
            this.computeGsqPlanQty(scheduleVo, totalPlanQtyVo, lossRateMap, getDouble(paramsMap.get(EngineConstants.LOSS_RATE)), getDouble(paramsMap.get(EngineConstants.MERGE_PLAN_THRESHOLD)));  //计算钢丝圈各班计划量
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getSteelRingCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(username);
        }
//        this.equilibrium(batchNo, scheduleList, paramsMap, totalPlanQtyVo);  //中班和夜班计排程计划量均衡处理
        this.setProduceOrder(scheduleList);  //设置各班生产顺序

        List<GsqScheduleResultVo> existScheduleList = this.gsqEngineMapper.listGsqEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncGsqScheduleToLog(scheduleDate);  //把排程数据同步到log表，删除历史外协排程数据
        this.createScheduleRecord(scheduleDate, tqBatchNo, batchNo);  //创建自动排程记录
        List<GsqScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getSteelRingCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getSteelRingCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            gsqEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            gsqEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    /**
     * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     * @param scheduleDate 排程日志
     * @param batchNo 批次号
     * @param productionStage 仅投产阶段规格排产标识
     */
    private void ValidatedConstruction(String scheduleDate, String batchNo, String productionStage, Map<String, String> mapAssistSpec) {
        List<String> list = commonMapper.listLossConstructionForTq(scheduleDate);
        if(list != null && !list.isEmpty()) {
            String tip = I18nUtil.getMessage("gsq.engine.auto.scheule.validated");
            String embryoCodes = String.join(",", list);
            tip = String.format(tip, embryoCodes);
            autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(tip);
        }

        List<EngineConstructionInfo> list1 = gsqEngineMapper.listGsqNeedConstruction(scheduleDate, productionStage);
        list1 = list1.stream().filter(r -> !mapAssistSpec.containsKey(r.getBeadCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
        for(EngineConstructionInfo construction : list1) {
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
            if(StringUtils.isBlank(construction.getBeadCode())) {
                //施工表钢丝圈代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getBeadType())) {
                //施工表钢丝类型代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadType") + "\"");
            }
            if(StringUtils.isBlank(construction.getBeadArrange())) {
                //施工表钢丝圈排列为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.beadArrange") + "\"");
            }
            if(construction.getDimension() == null || construction.getDimension() == 0) {
                //施工表钢丝圈长为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.dimension") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
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
        List<String> listAssistSpec = this.gsqEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 钢丝圈插单
     * @param scheduleVo
     */
    public int inertGsqOrder(GsqScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<GsqScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveGsqSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveGsqSchedule(String scheduleDate, List<GsqScheduleResultVo> scheduleList) {
        return this.batchSaveGsqSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveGsqSchedule(String scheduleDate, List<GsqScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = gsqEngineMapper.getGsqCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“，那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //钢丝圈排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncGsqScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        List<String> steelRingCodes = scheduleList.stream().map(GsqScheduleResultVo::getSteelRingCode).collect(Collectors.toList());
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        Map<String, GsqScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, steelRingCodes, productionStage);  //根据钢丝圈代码查询对应的钢丝圈基础信息
        Map<String, Double> planStockMap = gsqEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算钢丝圈16点预计库存
        Map<String, GsqMonthSurplusVo> monthSurplus = gsqEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map（这里值用到：" + paramsMap));  //添加日志

        for(GsqScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            GsqScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getSteelRingCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }

            Double midPlanQty = schedule.getMidPlanQty();  //中班计划量
            schedule.setMidPlanQty(midPlanQty == null ? 0D : midPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);
            Double dayPlanQty = schedule.getDayPlanQty();  //白班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);

            schedule.setStockQty(planStockMap.getOrDefault(schedule.getSteelRingCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getSteelRingCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            schedule.setUnitConsume(2D);  //单耗：1条胎需要2个钢丝圈
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
        }
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return gsqEngineMapper.mergeGsqScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据钢丝圈代码查询对应的钢丝圈基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, GsqScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> steelRingCodes, String productionStage) {
        Map<String, GsqScheduleBaseInfoVo> map = new HashMap<>();
        List<GsqScheduleBaseInfoVo> list = gsqEngineMapper.listGsqScheduleBaseInfo(steelRingCodes, ""); //查询出钢丝圈在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(GsqScheduleBaseInfoVo::getSteelRingCode, baseInfoVo->baseInfoVo));
        }

        Map<String, GsqScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<GsqScheduleResultVo> hasCxlist = gsqEngineMapper.statGsqScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(GsqScheduleResultVo info : hasCxlist) {
            GsqScheduleBaseInfoVo baseInfoVo = new GsqScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getSteelRingCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResultVo
//     */
//    public void changeGsqMachine(String oldMachineIds, GsqScheduleResultVo scheduleResultVo) {
//        String batchNo = scheduleResultVo.getBatchNo();  //批次号
//        String orderNo = scheduleResultVo.getOrderNo();  //工单号
//        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResultVo)));  //添加日志
//        Map<String, Double> lossRateMap = gsqEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = gsqEngineLossService.getLossRate(scheduleResultVo.getSteelRingCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = gsqEngineLossService.getLossRate(scheduleResultVo.getSteelRingCode(), scheduleResultVo.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
//        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
//                logSplit("重新计算计划量规则：先要根据之前机台的耗损率推算出之前在没有加上耗损率之前的计划量A，然后再用计划量A * 当前机台对应的耗损率，计算出最终的计划量", "转机台前的耗损率：" + oldLossRate + "转机台后的耗损率：" + lossRate));  //添加日志
//
//
//        Double midPlanQty = scheduleResultVo.getMidPlanQty();  //中班计划量
//        if(midPlanQty != null) {
//            midPlanQty = BigDecimalUtil.div(midPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
//            scheduleResultVo.setNightPlanQty(midPlanQty);
//        }
//        Double nightPlanQty = scheduleResultVo.getNightPlanQty();  //夜班计划量
//        if(nightPlanQty != null) {
//            nightPlanQty = BigDecimalUtil.div(nightPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
//            scheduleResultVo.setNightPlanQty(nightPlanQty);
//        }
//        Double dayPlanQty = scheduleResultVo.getDayPlanQty();  //白班计划量
//        if(dayPlanQty != null) {
//            dayPlanQty = BigDecimalUtil.div(dayPlanQty, 1 + oldLossRate, 4); //计算出之前没有加上损耗量的 计划量
//            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
//            scheduleResultVo.setDayPlanQty(dayPlanQty);
//        }
//        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResultVo));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmGsqMachine(GsqScheduleResultDto scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = gsqEngineLossService.getLossRateMap();   //损耗率map
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

        //耗损率
        double lossRate = gsqEngineLossService.getLossRate(scheduleResult.getSteelRingCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

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
        autoScheduleLogService.insertGsqScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 均衡日志
     * @param scheduleList
     * @param paramsMap
     * @param totalPlanQtyVo
     */
    private void equilibriumLog(String batchNo, String oldScheduleList, List<GsqScheduleResultVo> scheduleList, Map<String, String> paramsMap, GsqTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }

    /**
     * 根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<GsqScheduleResultVo> scheduleList) {
        int midProduceOrder = 1;  //中班生产顺序
        int nightProduceOrder = 1;  //夜班生产顺序
        int dayProduceOrder = 1; //白班生产顺序

        //根据库存供应时长升序排序
        scheduleList = scheduleList.stream().sorted(Comparator.comparing(GsqScheduleResultVo::getSupplyTime)).collect(Collectors.toList());
        for(GsqScheduleResultVo scheduleVo : scheduleList) {
            Double midPlanQty = scheduleVo.getMidPlanQty();
            Double nightPlanQty = scheduleVo.getNightPlanQty();
            Double dayPlanQty = scheduleVo.getDayPlanQty();
            Integer notOrderTag = scheduleVo.getNotOrderTag();  //不需要参与生产顺序排程tag。值不为空，表示不需要参与排序

            if(midPlanQty > 0 && notOrderTag == null) {
                scheduleVo.setMidProduceOrder(midProduceOrder++);
            }
            if(nightPlanQty > 0 && notOrderTag == null) {
                scheduleVo.setNightProduceOrder(nightProduceOrder++);
            }
            if(dayPlanQty > 0 && notOrderTag == null) {
                scheduleVo.setDayProduceOrder(dayProduceOrder++);
            }
            autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                    logSplit("根据库存供应时长(从小到大)，设置各班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(GsqScheduleResultVo scheduleResultVo, GsqMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
            log.error("月计划汇总数据为空，物料编号为：", scheduleResultVo.getSteelRingCode());
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
        autoScheduleLogService.insertGsqScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
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
        autoScheduleLogService.insertGsqScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明出于生产中;③月度计划量小于等于0，说明出于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<GsqScheduleResultVo> mergeExistSchedule(String batchNo, List<GsqScheduleResultVo> autoScheduleList, List<GsqScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<GsqScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<GsqScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(GsqScheduleResultVo::getSteelRingCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(GsqScheduleResultVo autoSchedule : autoScheduleList) {
            List<GsqScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getSteelRingCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                GsqScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                //重排前的数据如果已经发布过，在重新排程后仍有相应的生产需求，计划量按照重新自动排程的计划量安排；订单号需要和之前发布个mes的订单号一致
                autoSchedule.setOrderNo(existSchedule.getOrderNo());  //订单号
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);  //发布状态修改
                autoSchedule.setMachineId(existSchedule.getMachineId());  //机台沿用重排前的机台
                mergeList.add(autoSchedule);
            } else if(existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                //对应规格重排前已经发布，并且此规格重排前只有多条排程记录（对应了多个机台）。那需要保留重排之前的排产，并且要把此规格重排后的各班的计划量，拼接到备注中
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark1");
                remarkTip = StringUtils.format(remarkTip, stripZeros(autoSchedule.getMidPlanQty()), stripZeros(autoSchedule.getNightPlanQty()) ,stripZeros(autoSchedule.getDayPlanQty()));
                for(GsqScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getSteelRingCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<GsqScheduleResultVo> list : existScheduleMap.values()) {
            list.forEach(r->r.setBatchNo(batchNo));
            mergeList.addAll(list);
        }
        return mergeList;
    }

    /**
     * 创建自动排程记录
     *
     * @param scheduleDate 排程日期
     * @param tqBatchNo    对成型批次号
     * @param batchNo      钢丝圈批次号
     */
    private void createScheduleRecord(String scheduleDate, String tqBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("tqBatchNo", tqBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        gsqEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncGsqScheduleToLog(String scheduleDate) {
        gsqEngineMapper.syncGsqScheduleToLog(scheduleDate);
        gsqEngineMapper.deleteGsqSchedule(scheduleDate);
        gsqEngineMapper.deleteGsqAssistSchedule(scheduleDate);
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyCanMachineMap  定点机台中限制作业map
     * @param specifyNotMachineMap  定点机台中不可作业
     * @param twiningDiscMachineMap 缠绕盘和机台关系集合（key = 规格尺寸~排列方式）
     * @param twiningDiscMap 获得钢丝圈代码和缠绕盘集合（value = 规格尺寸~排列方式）map
     */
    private void chooseMachine(GsqScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                               Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap) {
        String steelRingCode = scheduleVo.getSteelRingCode();  //钢丝圈代码
        String machineIds = specifyCanMachineMap.get(steelRingCode);  //根据钢丝圈代码从定点机台MAP中获取机台
        if(StringUtils.isBlank(machineIds)) {
            //定点机台匹配的机台为空，则从钢丝圈缠绕盘中去匹配机台
            machineIds = twiningDiscMachineMap.get(twiningDiscMap.get(steelRingCode));
        }
        //过滤掉 定点机台中 设置的不可作业的机台
        String notMachineIds = specifyNotMachineMap.get(steelRingCode);  //定点机台中不可作业的机台
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
        chooseMachineLog(scheduleVo, specifyCanMachineMap, specifyNotMachineMap, twiningDiscMachineMap, twiningDiscMap);  //添加日志
    }

    /**
     * 设置生产线日志
     * @param scheduleVo
     * @param specifyCanMachineMap
     * @param specifyNotMachineMap
     * @param twiningDiscMachineMap
     * @param twiningDiscMap
     */
    private void chooseMachineLog(GsqScheduleResultVo scheduleVo, Map<String, String> specifyCanMachineMap, Map<String, String> specifyNotMachineMap,
                                  Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("①优先选择“定点机台中限制作业集合”匹配上的机台;②如果没有，在选择“缠绕盘与机台对应关系集合”的机台信息，不过需要过滤掉'定点机台中不可作业'中的机台").append(division);
        logDetail.append("定点机台中限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点机台中不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("缠绕盘（尺寸~排列方式）与机台对应关系集合：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("钢丝圈和缠绕盘（尺寸~排列方式）对应计划：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("结果数据：" + toJSONString(scheduleVo)).append(division);
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产线（机台）", logDetail.toString());
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(GsqScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getSteelRingCode() + "，16点预计库存：" + stockQty + "，对应成型一班的计划量：" + cxClass1Plan + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

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
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getSteelRingCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(GsqScheduleResultVo scheduleVo,Double remnantStock, Double classPlan) {
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
            autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getSteelRingCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算并设置库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaParamMap 成型机台code和胎胚代码集合，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     */
    private void computeSupplyTime(GsqScheduleResultVo scheduleVo, Map<String, String> quotaParamMap, Double stockQty) {
        String quotaKeys = quotaParamMap.get(scheduleVo.getSteelRingCode());  //获得钢丝圈对应的成型的机台code和胎胚code
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            scheduleVo.setMidSysAnalysis("当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            scheduleVo.setNightSysAnalysis("当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            scheduleVo.setDaySysAnalysis("当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            scheduleVo.setNotOrderTag(0);   //生产顺序字段 不参与排序
            autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "当天成型排程不需要此规格的钢丝圈所以’库存供应时长‘为0");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        Double quota = cxQuota * 2D;   //钢丝圈定额(1条胎=2个钢丝圈)
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置困存公用时长向下保留2位小数
        }
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗(1条胎需要消耗2个钢丝圈)：2" ,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(GsqScheduleResultVo scheduleVo) {
        int count = 0;
        int addTime = 8;  //每班8小时
        if(scheduleVo.getCxClass1Plan() == null || scheduleVo.getCxClass1Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass2Plan() == null || scheduleVo.getCxClass2Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass3Plan() == null || scheduleVo.getCxClass3Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass4Plan() == null || scheduleVo.getCxClass4Plan() == 0) {
            count++;
        } else {
            return count * addTime;
        }

        if(scheduleVo.getCxClass5Plan() == null || scheduleVo.getCxClass5Plan() == 0) {
            count++;
        }
        return count * addTime;
    }

    /**
     * 计算钢丝圈中班和夜班计划量
     * @param scheduleVo
     * @param totalPlanQtyVo 计划量总计VO
     * @param lossMap 耗损率map
     * @param paramLossRate 工序参数中配置的耗损率
     * @param mergeThreshold 往前一班合并计划量阈值
     */
    private void computeGsqPlanQty(GsqScheduleResultVo scheduleVo, GsqTotalPlanQtyVo totalPlanQtyVo, Map<String, Double> lossMap, double paramLossRate, double mergeThreshold) {
        String oldScheduleResult = toJSONString(scheduleVo); //没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); //库存
        //中班计划量
        double midPlanQty = scheduleVo.getMidPlanQty();
        double initMidPlanQty = midPlanQty;
        //夜班计划量
        double nightPlanQty = scheduleVo.getNightPlanQty();
        double initNightPlanQty = nightPlanQty;
        //白班计划量
        double dayPlanQty = scheduleVo.getDayPlanQty();

        //根据库存重新计算中班计划量：（原中班计划量>库存） ？（ 原中班计划量-库存） ： 0
        midPlanQty = (initMidPlanQty > stockQty) ? BigDecimalUtil.sub(midPlanQty, stockQty) : 0;
        //根据库存重新计算夜班计划量：（原中班计划量>库存） ？原夜班计划量 ： （原中班计划量+原夜班计划量 - 库存）
        nightPlanQty = (initMidPlanQty > stockQty) ? nightPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, nightPlanQty), stockQty);
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;
        //根据库存重新计算白班计划量：（原中班计划量+夜班计划量 > 库存） ？原白班计划量 ： （原中班计划量+原夜班计划量+原白班计划量 - 库存）
        dayPlanQty = (BigDecimalUtil.add(initMidPlanQty, initNightPlanQty) > stockQty) ? dayPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initMidPlanQty, initNightPlanQty, dayPlanQty), stockQty);
        dayPlanQty = (dayPlanQty < 0) ? 0D : dayPlanQty;

        /*  为了防止二次投产，把后面一班的计划量往前面一班合并（这块暂时注释掉，待确定）
        //如果夜班计划量>0并且夜班的计划量没有超过参数配置的阈值，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        if(nightPlanQty > 0 && dayPlanQty <= mergeThreshold) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, dayPlanQty);
            dayPlanQty = 0D;
        }
        //如果中班计划量>0并且夜班的计划量没有超过参数配置的阈值，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        if (midPlanQty > 0 && nightPlanQty <= mergeThreshold) {
            midPlanQty = BigDecimalUtil.add(midPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }
        */

        String machineId = scheduleVo.getMachineId();  //机台id
        double lossRate = 0;
        //只有单个机台的时候，自动排程才计算耗损率
        if(StringUtils.isNotBlank(machineId) && !machineId.contains(",")) {
            //计划量要加上耗损量
            lossRate = gsqEngineLossService.getLossRate(scheduleVo.getSteelRingCode(), scheduleVo.getMachineId(), lossMap, paramLossRate);
            midPlanQty = BigDecimalUtil.add(midPlanQty, BigDecimalUtil.mul(midPlanQty, lossRate));
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
        }

        scheduleVo.setMidPlanQty(BigDecimalUtil.roundUp(midPlanQty,0));
        scheduleVo.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty,0));
        scheduleVo.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty,0));

        //计算各班总计划量
        totalPlanQtyVo.setTotalMidPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), midPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalMidPlanQty(), totalPlanQtyVo.getTotalNightPlanQty(), totalPlanQtyVo.getTotalDayPlanQty()));
        this.computeGsqPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate, mergeThreshold);  //添加日志
    }

    private void computeGsqPlanQtyLog(String oldScheduleResult, GsqScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate, double mergeThreshold) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("在sql中计算各班的计划量。钢丝圈中班计划量=胎圈中班+夜班；钢丝圈夜班计划量=胎圈白天计划量；钢丝圈白天计划量=胎圈次日中班计划量").append(division);
        logDetail.append("根据库存重新计算中班计划量midPlanQty：根据库存重新计算中班计划量midPlanQty：（原中班计划量midPlanQty>库存stockQty） ？（ 原中班计划量-库存）：0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量midPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ： （原中班计划量midPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("根据库存重新计算白班计划量dayPlanQty：（原中班计划量+夜班计划量 > 库存） ？原白班计划量 ： （原中班计划量+原夜班计划量+原白班计划量 - 库存）").append(division);
        logDetail.append("钢丝圈耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 钢丝圈代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
//        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）,往前一班合并计划量阈值参数值：" + mergeThreshold).append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertGsqScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.GSQ_BATCH_NO_PREFIX + scheduleDate);
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
        List<GsqParamsVo> list = this.gsqEngineMapper.listGsqParams();
        Map<String, String> map = list.stream().collect(Collectors.toMap(GsqParamsVo::getParamCode, GsqParamsVo::getParamValue));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 获取钢丝圈对应的成型胎胚code和机台code
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return
     */
    private Map<String, String> getQuotaParamMap(String scheduleDate, String productionStage) {
        List<GsqQuotaParam> list = gsqEngineMapper.listQuotaParam(scheduleDate, productionStage);
        Map<String, String> map = list.stream().collect(Collectors.toMap(GsqQuotaParam::getSteelRingCode, GsqQuotaParam::getQuotaKeys));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param twiningDiscMachineMap 缠绕盘和机台关系集合（key = 规格尺寸~排列方式）
     * @param twiningDiscMap 获得钢丝圈代码和缠绕盘集合（value = 规格尺寸~排列方式）map
     * @param specifyCanMachineMap 定点机台和机台的限制作业集合
     * @param specifyNotMachineMap 定点集合和机台的不可作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param paramsMap 参数设置集合
     */
    private void baseDataLog(String batchNo, Map<String, String> twiningDiscMachineMap, Map<String, String> twiningDiscMap, Map<String, String> specifyCanMachineMap,
                             Map<String, String> specifyNotMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, GsqMonthSurplusVo> monthSurplus,Map<String, String> paramsMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("缠绕盘和机台关系集合（key=规格尺寸~排列方式）：" + toJSONString(twiningDiscMachineMap)).append(division);
        logDetail.append("钢丝圈代码和缠绕盘计划（value=规格尺寸~排列方式）：" + toJSONString(twiningDiscMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyCanMachineMap)).append(division);
        logDetail.append("定点集合和机台的不可作业集合：" + toJSONString(specifyNotMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(paramsMap)).append(division);
        autoScheduleLogService.insertGsqScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }

}