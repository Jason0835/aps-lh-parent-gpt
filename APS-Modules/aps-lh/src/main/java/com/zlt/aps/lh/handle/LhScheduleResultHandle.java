package com.zlt.aps.lh.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.context.AutoLhScheduleResultContextDTO;
import com.zlt.aps.enums.MdmMachineTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.common.CommonRedisService;
import com.zlt.aps.common.CommonUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.constants.LhPrefixConstants;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertParamDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftTimeWindowDTO;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import com.zlt.aps.lh.api.domain.vo.LhMoldInfoVo;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultVo;
import com.zlt.aps.lh.api.enums.LhParamCodeEnums;
import com.zlt.aps.lh.api.enums.MachineTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftSystemEnum;
import com.zlt.aps.lh.api.enums.ShiftSystemNameEnum;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.lh.service.ILhParamsService;
import com.zlt.aps.lh.service.ILhSpecifyMachineService;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.aps.maindata.domain.dto.MdmProductConstructionDto;
import com.zlt.aps.maindata.service.*;
import com.zlt.aps.mp.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.mp.api.domain.vo.MdmDeviceMaintenancePlanVo;
import com.zlt.aps.mp.api.domain.vo.MdmProductConstructionVO;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProdFinalRemoteService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化排程处理器
 * @date 2025/3/6
 */
@Slf4j
@Service
public class LhScheduleResultHandle {


    /**
     * 成型硫化相关service
     */
    @Autowired
    private LhScheduleResultService lhScheduleResultService;
    @Autowired
    private ILhParamsService lhParamsService;
    @Autowired
    private ILhSpecifyMachineService lhSpecifyMachineService;
    @Autowired
    private ILhMonthPlanSurplusService lhMonthPlanSurplusService;
    @Autowired
    private ILhMachineInfoService lhMachineInfoService;

    /**
     * 月度计划相关service
     */
    @Autowired
    private IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalRemoteService;
    /**
     * 基础数据相关service
     */
    @Autowired
    private IMdmDeviceMaintenancePlanService mdmDeviceMaintenancePlanService;
    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;
    @Autowired
    private IMdmMouldUseStatusService mdmMouldUseStatusService;
    @Autowired
    private IMdmProductModelRelationService mdmProductModelRelationService;
    @Autowired
    private IMdmMaterialInfoService mdmMaterialInfoService;
    @Autowired
    private CommonRedisService commonCacheService;
    @Autowired
    private ISysDictDataCacheService sysDictDataService;

    private final static String LOCATION_DICT_TYPE = "biz_brand_type"; // 品牌字典类型

    /**
     * 构建上下文对象
     *
     * @param autoLhScheduleResultDTO
     * @return
     */
    public AutoLhScheduleResultContextDTO buildLhScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO) {

        Map<String, String> paramsMap = lhParamsService.listLhParams(autoLhScheduleResultDTO.getFactoryCode());
        //构建上下文对象
        AutoLhScheduleResultContextDTO contextDTO = new AutoLhScheduleResultContextDTO();
        contextDTO.setScheduleTime(autoLhScheduleResultDTO.getScheduleTime());
        contextDTO.setLhParamsMap(paramsMap);
        contextDTO.setTDayFlag(ApsConstant.TRUE);
        contextDTO.setHadSchedulePlanNum(0);
        //取系统配置参数_班制 如果不为空用参数覆盖
        String classSystem = paramsMap.get(LhParamCodeEnums.CLASS_SYSTEM.getCode());
        // 1) 从配置获取班制，此处假设返回 "2" 或 "3" 字符串
        int shiftsPerDay = StringUtils.isNotEmpty(classSystem) ? Integer.valueOf(classSystem) : ShiftSystemEnum.SHIFT_SYSTEM_2.getCode();
        contextDTO.setWorkShifts(shiftsPerDay);
        // 2) 日排程总计划量限制（条）
        String limitTotalPlanNum = paramsMap.get(LhParamCodeEnums.TOTAL_PLAN_NUM_LIMIT.getCode());
        int iLimitTotalPlanNum = StringUtils.isNotEmpty(limitTotalPlanNum) ? Integer.valueOf(limitTotalPlanNum) : 9999999;
        contextDTO.setLimitTotalPlanNum(iLimitTotalPlanNum);

        // 2) 首排规格判断时间（天数）
        String firstSkuCheckTime = paramsMap.get(LhParamCodeEnums.FIRST_SKU_CHECK_TIME.getCode());
        int iFirstSkuCheckTime = StringUtils.isNotEmpty(firstSkuCheckTime) ? Integer.valueOf(firstSkuCheckTime) : 0;
        contextDTO.setFirstSkuCheckTime(iFirstSkuCheckTime);
        // 3) 首排规格排产计划量（条）
        String firstSkuScheduleNum = paramsMap.get(LhParamCodeEnums.FIRST_SKU_SCHEDULE_NUM.getCode());
        int iFirstSkuScheduleNum = StringUtils.isNotEmpty(firstSkuScheduleNum) ? Integer.valueOf(firstSkuScheduleNum) : 0;
        contextDTO.setFirstSkuScheduleNum(iFirstSkuScheduleNum);
        // 4) 辅助时间，包括检查轮胎、喷脱模剂、胶囊定型等
        String brushBagTime = paramsMap.get(LhParamCodeEnums.BRUSH_BAG_TIME.getCode());
        int iBrushBagTime = StringUtils.isNotEmpty(brushBagTime) ? Integer.valueOf(brushBagTime) : 0;
        contextDTO.setBrushBagTime(iBrushBagTime);
        // 5) 机械机台操作时长，用于计算单班硫化量
        String mechanicalMachineOperTime = paramsMap.get(LhParamCodeEnums.MECHANICAL_MACHINE_OPER_TIME.getCode());
        int iMechanicalMachineOperTime = StringUtils.isNotEmpty(mechanicalMachineOperTime) ? Integer.valueOf(mechanicalMachineOperTime) : 0;
        contextDTO.setMechanicalMachineOperTime(iMechanicalMachineOperTime);
        // 6) 液压机台操作时长，用于计算单班硫化量
        String hydraulicMachineOperTime = paramsMap.get(LhParamCodeEnums.HYDRAULIC_MACHINE_OPER_TIME.getCode());
        int iHydraulicMachineOperTime = StringUtils.isNotEmpty(hydraulicMachineOperTime) ? Integer.valueOf(hydraulicMachineOperTime) : 0;
        contextDTO.setHydraulicMachineOperTime(iHydraulicMachineOperTime);
        // 判断是否夏季
        contextDTO.setBSummerSeason(CommonUtils.isSummerSeason(paramsMap.get(LhParamCodeEnums.START_CURING_SUMMER_DAY.getCode()),paramsMap.get(LhParamCodeEnums.START_CURING_WINTER_DAY.getCode())));
        //初始化动态班次
        contextDTO.setShiftTimeWindowDTOList(this.buildShiftTimeWindows(paramsMap, contextDTO.getScheduleTime()));

        //硫化自动排程批次号
        String scheduleDateStr = DateUtils.parseDateToStr("yyyyMMdd", autoLhScheduleResultDTO.getScheduleTime());
        String lhBatchNo = commonCacheService.getSequence(LhPrefixConstants.SCHEDULE_BATCH_NO_PREFIX + scheduleDateStr, LhPrefixConstants.LH_BATCH_NO_PREFIX + scheduleDateStr);
        contextDTO.setBatchNo(lhBatchNo);

        //初始化日志
        contextDTO.setLogDetail(new StringBuilder());

        //品牌优先生产排序
        String result = paramsMap.get(LhParamCodeEnums.BRAND_ORDER.getCode());
        String brandOrder = StringUtils.isNotEmpty(result) ? result : null;
        contextDTO.setBrandOrder(brandOrder);

        // 换模次数限制
        String changeMouldLimit = paramsMap.get(LhParamCodeEnums.CHANGE_MOULD_LIMIT.getCode());
        contextDTO.setChangeMouldLimit(StringUtils.defaultIfEmpty(changeMouldLimit,null));
        return contextDTO;
    }



    /**
     * 初始化硫化排程数据
     *
     * @param autoLhScheduleResultDTO
     */
    public AutoLhScheduleResultContextDTO initLhScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO, AutoLhScheduleResultContextDTO contextDTO) throws BusinessException {
        // 1. 获取排程时间
        Date scheduleTime = autoLhScheduleResultDTO.getScheduleTime();
        // 2. 获取T-1日硫化计划（昨天的计划）
        this.buildLastDayLhScheduleResult(autoLhScheduleResultDTO, contextDTO);
        // 3.把T月度计划T日和T+1日区分出来
        this.buildTAndT1Spec(contextDTO, autoLhScheduleResultDTO.getFactoryCode());
        // 4.把规格代号对应的生胎代码查询出来 并查找每个规格的胎胚库存情况
        this.buildEmbryoCode(contextDTO, autoLhScheduleResultDTO.getFactoryCode(), scheduleTime);
        // 5.查询每个规格的模数情况
        this.buildModelRelation(contextDTO, autoLhScheduleResultDTO.getFactoryCode());
        // 6.计算硫化时长
        //this.singleModeSingleShiftProductionCapacity(autoLhScheduleResultDTO, contextDTO);
        // 7. 查询所有可用机台（状态为 NO，即表示可用机台）
        this.initAllMachines(autoLhScheduleResultDTO, contextDTO);
        // 8. 一次性查询当前月份的维修保养计划（本月所有计划）
        this.initMaintenancePlan(contextDTO, scheduleTime);
        // 9、清空满排机台清单
        contextDTO.setFullMachineCodeList(new ArrayList<>());
        contextDTO.setMaintainMachineMap(new HashMap<>());
        return contextDTO;
    }

    /**
     * 初始化维修保养计划
     * @param contextDTO
     * @param scheduleTime
     */
    private void initMaintenancePlan(AutoLhScheduleResultContextDTO contextDTO, Date scheduleTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduleTime);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        MdmDeviceMaintenancePlanVo docDeviceMaintenancePlan = new MdmDeviceMaintenancePlanVo();
        docDeviceMaintenancePlan.setMachineType(MdmMachineTypeEnum.COMBINE_LH.getValue()); // 硫化机+洗模
        docDeviceMaintenancePlan.setYear(year);
        docDeviceMaintenancePlan.setMonth(month);
        List<MdmDeviceMaintenancePlan> maintenancePlanList = mdmDeviceMaintenancePlanService.selectDocDeviceMaintenancePlanList(docDeviceMaintenancePlan);
        contextDTO.setMaintenancePlanList(maintenancePlanList);
    }

    /**
     * 初始化所有的机台信息
     * @param autoLhScheduleResultDTO
     * @param contextDTO
     */
    private void initAllMachines(AutoLhScheduleResultDTO autoLhScheduleResultDTO, AutoLhScheduleResultContextDTO contextDTO) {
        LhMachineInfo queryLhMachineInfo = new LhMachineInfo();
        queryLhMachineInfo.setFactoryCode(autoLhScheduleResultDTO.getFactoryCode());
        queryLhMachineInfo.setStatus(YesOrNoEnum.YES.getCode());
        List<LhMachineInfo> allMachineList = lhMachineInfoService.selectList(queryLhMachineInfo);
        contextDTO.setAllMachineList(allMachineList);
    }

    /**
     * 获取排程机台信息
     * @param insertParamDTO
     * @return
     */
    public List<LhMachineInfoVo> getScheduleMachineInfo(LhOrderInsertParamDTO insertParamDTO) {
        // 1. 初始化上下文
        AutoLhScheduleResultDTO autoLhScheduleResultDTO = new AutoLhScheduleResultDTO();
        autoLhScheduleResultDTO.setFactoryCode(insertParamDTO.getFactoryCode());
        autoLhScheduleResultDTO.setScheduleTime(insertParamDTO.getScheduleTime());
        AutoLhScheduleResultContextDTO contextDTO = this.buildLhScheduleResult(autoLhScheduleResultDTO);
        // 2. 获取T-1日硫化计划（昨天的计划）
        this.buildLastDayLhScheduleResult(autoLhScheduleResultDTO, contextDTO);
        // 3. 查询所有可用机台（状态为 NO，即表示可用机台）
        this.initAllMachines(autoLhScheduleResultDTO, contextDTO);
        // 4. 一次性查询当前月份的维修保养计划（本月所有计划）
        contextDTO.setMaintainMachineMap(new HashMap<>());
        this.initMaintenancePlan(contextDTO, insertParamDTO.getScheduleTime());
        List<LhMachineInfoVo> allMachineList = availableMachinesList(contextDTO);
        contextDTO.setAvailableMachines(allMachineList);
        // 5. 获取T日硫化计划
        List<LhScheduleResultVo> specList = lhScheduleResultService.getTDayLhScheduleResult(insertParamDTO.getScheduleTime(), insertParamDTO.getFactoryCode());
        contextDTO.setTDayScheduleList(specList);
        // 6.查询每个规格的模数情况
        this.buildModelRelation(contextDTO, insertParamDTO.getFactoryCode());
        // 8. 计算机台剩余产能
        Date windowStart;
        LhScheduleResultVo lastScheduleResultVo;
        LhScheduleResultVo scheduleResultVo = createInsertScheduleResult(insertParamDTO, contextDTO);
        //初始化班制时间
        initShiftTime(contextDTO, scheduleResultVo);

        //按机台分组，根据机台的维度查询该机台已排规格
        Map<String, List<LhScheduleResultVo>> machineScheduledMap =
                specList.stream().collect(Collectors.groupingBy(LhScheduleResultVo::getLhMachineCode));

        //遍历所有机台，计算剩余可用产能（按班次分配）
        for (LhMachineInfoVo machineVo : allMachineList) {
            windowStart = scheduleResultVo.getClass1StartTime();
            scheduleResultVo.setRemainMoldQty(machineVo.getMaxMoldNum() == 1 ? 1:2);
            specList = machineScheduledMap.get(machineVo.getMachineCode());
            if (PubUtil.isNotEmpty(specList)){
                lastScheduleResultVo = specList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo != null && lastScheduleResultVo.getSpecEndTime() != null){
                    windowStart = lastScheduleResultVo.getSpecEndTime();
                }
            }
            //计算机台剩余时间
            reCalcOneMachineRemainTime(contextDTO, scheduleResultVo, specList, machineVo,windowStart);
            //计算机台剩余产能
            calcOneMachineRemainCapacity(contextDTO,scheduleResultVo,specList,machineVo,false);
        }
        allMachineList.sort(Comparator.comparingLong(LhMachineInfoVo::getRemainCapacity).reversed()
                .thenComparing(LhMachineInfoVo::getMachineOrder));
        return allMachineList;
    }

    /**
     * 创建插单排程结果
     * @param insertParamDTO
     * @param contextDTO
     * @return
     */
    private LhScheduleResultVo createInsertScheduleResult(LhOrderInsertParamDTO insertParamDTO, AutoLhScheduleResultContextDTO contextDTO) {
        LhScheduleResultVo scheduleResultVo = new LhScheduleResultVo();
        scheduleResultVo.setSpecCode(insertParamDTO.getSpecCode());
        MdmProductConstructionDto cons = mdmProductConstructionService.getCuringTime(insertParamDTO.getFactoryCode(), insertParamDTO.getProductCode(), insertParamDTO.getSpecCode());
        if (null == cons) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.constructionRelationshipNotFound"));
        }
        scheduleResultVo.setEmbryoCode(cons.getEmbryoCode());
        // 根据胎胚号获取库存情况
        scheduleResultVo.setBomVersion(cons.getBomVersion());
        scheduleResultVo.setMouldMethod(cons.getMouldMethod());
        scheduleResultVo.setMoldCavity(cons.getMoldCavity());
        scheduleResultVo.setMouldClampingPressure(cons.getMouldClampingPressure());
        String trialPrefix = contextDTO.getLhParamsMap().get(LhParamCodeEnums.TRIAL_PRODUCTION_PRE_FIX.getCode());
        if (StringUtils.isNotEmpty(trialPrefix) && StringUtils.isNotEmpty(cons.getConstructionCode())){
            scheduleResultVo.setIsTrial(trialPrefix.indexOf(cons.getConstructionCode().substring(0,1)) >= 0 ? ApsConstant.TRUE:ApsConstant.FALSE);
        }
        if (contextDTO.getBSummerSeason()){
            //夏季
            scheduleResultVo.setMachineryCuringTime(cons.getCuringTime()+contextDTO.getMechanicalMachineOperTime());
            scheduleResultVo.setHydraulicPressureCuringTime(cons.getHydraulicPressureCuringTime()+contextDTO.getHydraulicMachineOperTime());
        }else{
            //冬季
            scheduleResultVo.setMachineryCuringTime(cons.getCuringTime2()+contextDTO.getMechanicalMachineOperTime());
            scheduleResultVo.setHydraulicPressureCuringTime(cons.getHydraulicPressureCuringTime2()+contextDTO.getHydraulicMachineOperTime());
        }
        return scheduleResultVo;
    }

    /**
     * 把T日需要排的规格分组并且排序
     * 分组规则：
     * 1. 续作规格：T-1日有生产且T日也需要生产的规格。
     * 2. 限制规格：存在定点数据中的规格（不包括续作中的）。
     * 3. 普通规格：T日中未归入续作或限制的规格。
     *
     * 同时，如果某规格满足收尾条件（即：月剩余量减去T日计划量等于0），则打上“收尾”标记。
     *
     * 在每个组内，按照dailyPlanQty降序，再按照remainMoldQty降序（null当作0），最后按specCode升序排序。
     *
     * @param autoLhScheduleResultDTO 排程请求数据
     * @param contextDTO              排程上下文（包含T日和T-1日硫化计划、月剩余数据、定点机台限制等）
     * @throws BusinessException
     */
    public void groupLhScheduleResultByWorkShifts(AutoLhScheduleResultDTO autoLhScheduleResultDTO,
                                                  AutoLhScheduleResultContextDTO contextDTO) throws BusinessException {
        // 1. 获取T日所有规格代号
        Set<String> tDaySpecCodes = contextDTO.getTDaySpecList();

        // 2. 收集 T-1 日的规格信息到 Map<规格,机台列表>
        Map<String, List<String>> lastDaySpecMap = new HashMap<>();
        if (contextDTO.getLastDayScheduleList() != null) {
            Map<String, List<LhScheduleResultVo>> machineScheduledMap = contextDTO.getLastMachineScheduledMap();
            for (Map.Entry<String, List<LhScheduleResultVo>> entry : machineScheduledMap.entrySet()) {
                LhScheduleResultVo lastScheduleResultVo = entry.getValue().stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo == null){
                    continue;
                }
                if (StringUtils.isEmpty(lastScheduleResultVo.getLeftRightMold())){
                    //最后规格是双模排产
                    //使用规格代码作为Key存入续作Map
                    addContinuedMachine(lastDaySpecMap,lastScheduleResultVo);
                }else{
                    //最后规格是L/R模排产
                    if (ApsConstant.L_MOLD.equals(lastScheduleResultVo.getLeftRightMold())) {
                        //若L模，继续查找R模
                        addContinuedMachine(lastDaySpecMap,lastScheduleResultVo);
                        LhScheduleResultVo rlastScheduleResultVo = entry.getValue().stream()
                                .filter(x -> ApsConstant.R_MOLD.equals(x.getLeftRightMold()))
                                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                        if (rlastScheduleResultVo != null) {
                            addContinuedMachine(lastDaySpecMap,rlastScheduleResultVo);
                        }
                    }else if (ApsConstant.R_MOLD.equals(lastScheduleResultVo.getLeftRightMold())) {
                        //若R模，继续查找L模
                        addContinuedMachine(lastDaySpecMap,lastScheduleResultVo);
                        LhScheduleResultVo llastScheduleResultVo = entry.getValue().stream()
                                .filter(x -> ApsConstant.L_MOLD.equals(x.getLeftRightMold()))
                                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                        if (llastScheduleResultVo != null) {
                            addContinuedMachine(lastDaySpecMap,llastScheduleResultVo);
                        }
                    }
                }
            }
        }

        // 3. 遍历 T 日规格，进行续作赋值和识别
        Set<String> continuedSpecs = new HashSet<>();
        Set<String> trialSpecs = new HashSet<>();
        if (contextDTO.getTDayScheduleList() != null) {
            for (LhScheduleResultVo toDayVo : contextDTO.getTDayScheduleList()) {
                String specCode = toDayVo.getSpecCode();
                // 如果T-1日续作Map中存在该规格，则标记为续作规格
                if (lastDaySpecMap.containsKey(specCode)) {
                    continuedSpecs.add(specCode);
                }
                if (ApsConstant.TRUE.equals(toDayVo.getIsTrial())){
                    //试产试制规格
                    trialSpecs.add(specCode);
                }
            }
        }
        // 4. 获取限制规格：存在定点数据中的规格
        List<LhSpecifyMachine> specifyMachineList = lhSpecifyMachineService.queryByFactoryCodeAndSpecCodes(
                autoLhScheduleResultDTO.getFactoryCode(), tDaySpecCodes);
        Set<String> restrictedSpecs = new HashSet<>();
        if (specifyMachineList != null && !specifyMachineList.isEmpty()) {
            for (LhSpecifyMachine sm : specifyMachineList) {
                restrictedSpecs.add(sm.getSpecCode());
            }
        }
        // 注意：此处不再执行 restrictedSpecs.removeAll(continuedSpecs)，允许续作和限制有交集


        // 6. 判断收尾规格
        Map<String, Integer> todayPlanQtyMap = new HashMap<>();
        if (contextDTO.getTDayScheduleList() != null) {
            for (LhScheduleResultVo vo : contextDTO.getTDayScheduleList()) {
                String spec = vo.getSpecCode();
                int qty = vo.getDailyPlanQty() == null ? 0 : vo.getDailyPlanQty();
                todayPlanQtyMap.put(spec, todayPlanQtyMap.getOrDefault(spec, 0) + qty);
            }
        }

        Map<String, Integer> monthRemainQtyMap = contextDTO.getRemainMpQtyMap();
        Set<String> finishedSpecs = new HashSet<>();
        for (String spec : tDaySpecCodes) {
            int remainQty = monthRemainQtyMap.getOrDefault(spec, 0);
            int todayPlanQty = todayPlanQtyMap.getOrDefault(spec, 0);
            if (remainQty - todayPlanQty <= 0) {
                finishedSpecs.add(spec);
            }
        }

        // 7. 对T日规格进行标记（续作、限制、收尾）
        if (contextDTO.getTDayScheduleList() != null) {
            List<String> existSpecCodeList = getExistSpecCodeList(autoLhScheduleResultDTO.getFactoryCode(),contextDTO);
            for (LhScheduleResultVo vo : contextDTO.getTDayScheduleList()) {
                String spec = vo.getSpecCode();
                // 标记续作规格
                if (continuedSpecs.contains(spec)) {
                    vo.setIsContinue(ApsConstant.TRUE);
                    vo.setContinuedMachineList(lastDaySpecMap.get(spec));
                }else{
                    vo.setIsContinue(ApsConstant.FALSE);
                }

                // 标记限制规格
                vo.setIsLimit(restrictedSpecs.contains(spec) ? ApsConstant.TRUE : ApsConstant.FALSE);
                // 标记收尾规格
                vo.setIsEnd(finishedSpecs.contains(spec) ? ApsConstant.TRUE : ApsConstant.FALSE);
                // 标记首排规格
                vo.setIsFirst(checkIsFirstSku(vo.getSpecCode(),existSpecCodeList));
                if (ApsConstant.TRUE.equals(vo.getIsFirst())){
                    //若是首排规格，将日计划量置为20
                    int iDailyPlanQty = vo.getDailyPlanQty()>contextDTO.getFirstSkuScheduleNum() ? contextDTO.getFirstSkuScheduleNum():vo.getDailyPlanQty();
                    vo.setDailyPlanQty(iDailyPlanQty);
                }
            }
        }
        //把续作规格和限制规格做交集，去除掉续作规格
        restrictedSpecs.removeAll(continuedSpecs);
        trialSpecs.removeAll(continuedSpecs);
        // 5. 获取普通规格：T日中未归入续作或限制的规格（仅用于分组）
        Set<String> normalSpecs = new HashSet<>(tDaySpecCodes);
        normalSpecs.removeAll(continuedSpecs);
        normalSpecs.removeAll(restrictedSpecs);
        normalSpecs.removeAll(trialSpecs);

        // 7. 分组：将T日硫化计划列表按规格归类到三个组，且如果某规格满足finished条件，打上收尾标记
        List<LhScheduleResultVo> continuedList = new ArrayList<>();
        List<LhScheduleResultVo> restrictedList = new ArrayList<>();
        List<LhScheduleResultVo> trialList = new ArrayList<>();
        List<LhScheduleResultVo> normalList = new ArrayList<>();
        if (contextDTO.getTDayScheduleList() != null) {
            for (LhScheduleResultVo vo : contextDTO.getTDayScheduleList()) {
                String spec = vo.getSpecCode();
                if (continuedSpecs.contains(spec)) {
                    continuedList.add(vo); // 添加到续作规格组
                } else if (restrictedSpecs.contains(spec)) {
                    restrictedList.add(vo); // 添加到限制规格组
                } else if (trialSpecs.contains(spec)) {
                    trialList.add(vo); // 添加到试制规格组
                } else if (normalSpecs.contains(spec)) {
                    normalList.add(vo); // 添加到普通规格组
                }
            }
        }

        // 8. 定义排序规则：组内按 dailyPlanQty 降序，再按 remainMoldQty 降序（null当作0），再按 specCode 升序
       /* Comparator<LhScheduleResultVo> groupComparator = Comparator
                .comparing((LhScheduleResultVo vo) -> vo.getDailyPlanQty() == null ? 0 : vo.getDailyPlanQty(), Comparator.reverseOrder())
                .thenComparing((LhScheduleResultVo vo) -> vo.getRemainMoldQty() == null ? 0 : vo.getRemainMoldQty(), Comparator.reverseOrder())
                .thenComparing(LhScheduleResultVo::getSpecCode);

        Collections.sort(continuedList, groupComparator);
        Collections.sort(restrictedList, groupComparator);
        Collections.sort(trialList, groupComparator);
        Collections.sort(normalList, groupComparator);*/
        //放在 orderScheduleList 统一排序;
        // 更新上下文中的 分组
        contextDTO.setContinuedScheduleList(continuedList);
        contextDTO.setRestrictedScheduleList(restrictedList);
        contextDTO.setTrialScheduleList(trialList);
        contextDTO.setRemainingScheduleList(normalList);
    }

    /**
     * 判断当前规格是否是首排规格
     * @param specCode
     * @param existSpecCodeList
     * @return
     */
    private String checkIsFirstSku(String specCode, List<String> existSpecCodeList){
        if (PubUtil.isEmpty(existSpecCodeList)){
            return ApsConstant.FALSE;
        }
        if (existSpecCodeList.indexOf(specCode)<0){
            //若当前规格不存在于 排程列表中，表示首排
            return ApsConstant.TRUE;
        }
        return ApsConstant.FALSE;
    }

    /**
     * 获取T日规格是否在一定时间段内存在于 硫化排程
     * @param factoryCode 工厂
     * @param contextDTO 上下文
     * @return 存在于硫化排程的规格列表
     */
    private List<String> getExistSpecCodeList(String factoryCode,AutoLhScheduleResultContextDTO contextDTO){
        if (!ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
            //非T日直接返回
            return new ArrayList<>();
        }
        Date endDate = contextDTO.getScheduleTime();
        Date beginDate = DateUtils.addDays(endDate,-contextDTO.getFirstSkuCheckTime());
        return lhScheduleResultService.selectLhSpecCodeList(factoryCode,beginDate,endDate,contextDTO.getTDayAllSpecCodeList());
    }

    /**
     * 初始化月度剩余
     * @param factoryCode
     * @param contextDTO
     * @param allSpecCodes
     */
    private void initRemainMpQtyMap(String factoryCode, AutoLhScheduleResultContextDTO contextDTO, Set<String> allSpecCodes) {
       /* Calendar cal = Calendar.getInstance();
        cal.setTime(contextDTO.getScheduleTime());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;*/
        List<LhMonthPlanSurplus> monthPlanSurplusList = lhMonthPlanSurplusService
                .queryByFactoryAndSpecCodes(factoryCode, allSpecCodes, contextDTO.getMpYear(), contextDTO.getMpMonth());
        Map<String, Integer> monthRemainQtyMap = new HashMap<>();
        if (PubUtil.isEmpty(monthPlanSurplusList)){
            //设置月度剩余量
            contextDTO.setRemainMpQtyMap(monthRemainQtyMap);
            return;
        }
        for (LhMonthPlanSurplus surplus : monthPlanSurplusList) {
            monthRemainQtyMap.put(surplus.getSpecCode(), surplus.getMonthRemainQty() == null ? 0 : surplus.getMonthRemainQty());
        }
        Date todayDate = formatDateByCleanHms(new Date());
        Date scheduleDate = formatDateByCleanHms(contextDTO.getScheduleTime());
        if (scheduleDate.before(todayDate) ||
                scheduleDate.equals(todayDate)){
            //排程时间在今天或今天之前，前日的完成量已回，不用再处理
            contextDTO.setRemainMpQtyMap(monthRemainQtyMap);
            return;
        }

        if (!ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
            //若不是T日,-1
            scheduleDate = DateUtils.addDays(scheduleDate,-1);
        }
        int traceDays = DateUtils.getDayInterval(scheduleDate,todayDate);
        List<LhScheduleResultVo> lastDayScheduleList = lhScheduleResultService.getLastDayLhScheduleResult(scheduleDate,
                factoryCode,traceDays);
        Date deductDate;
        List<LhScheduleResultVo> newLastDayScheduleList;
        for (int i=0; i<traceDays;i++){
            deductDate = DateUtils.addDays(todayDate,i);
            newLastDayScheduleList = getFilterLastScheduleResult(lastDayScheduleList,deductDate);
            if (PubUtil.isNotEmpty(newLastDayScheduleList)){
                deductPreScheduleResult(monthRemainQtyMap, newLastDayScheduleList);
            }
        }

        if (!ApsConstant.TRUE.equals(contextDTO.getTDayFlag())) {
            //非T日，在算T+1日时，月度剩余量应先扣 T日的计划量，因为完成量未回传。
            //注：将非T日的分开，是因为它的前日是T日，排程结果还未保存
            lastDayScheduleList = contextDTO.getLastDayScheduleList();
            if (PubUtil.isNotEmpty(lastDayScheduleList)){
                deductPreScheduleResult(monthRemainQtyMap, lastDayScheduleList);
            }
        }
        //设置月度剩余量
        contextDTO.setRemainMpQtyMap(monthRemainQtyMap);
    }

    /**
     * 格式化日期，清空时分秒
     * @param formatDate
     * @return
     */
    private Date formatDateByCleanHms(Date formatDate){
        // 获取当前日期
        // 创建Calendar对象
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(formatDate);
        // 将时分秒清零
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 获取处理后的Date对象
        return calendar.getTime();
    }
    /**
     * 过滤前日排程
     * @param lastDayScheduleList
     * @param filterDate
     * @return
     */
    private List<LhScheduleResultVo> getFilterLastScheduleResult(List<LhScheduleResultVo> lastDayScheduleList, Date filterDate){
        if (PubUtil.isEmpty(lastDayScheduleList)){
            return lastDayScheduleList;
        }
        return lastDayScheduleList.stream().filter(x->x.getScheduleDate().equals(filterDate)).collect(Collectors.toList());
    }

    /**
     * 扣减前日排程
     * @param monthRemainQtyMap
     * @param lastDayScheduleList
     */
    private void deductPreScheduleResult(Map<String, Integer> monthRemainQtyMap, List<LhScheduleResultVo> lastDayScheduleList) {
        Map<String, List<LhScheduleResultVo>> lastScheduledMap =
                lastDayScheduleList.stream().collect(Collectors.groupingBy(LhScheduleResultVo::getSpecCode));
        List<LhScheduleResultVo> dayScheduleList2;
        Integer remainMpQty;
        for (Map.Entry<String, List<LhScheduleResultVo>> entry : lastScheduledMap.entrySet()) {
            dayScheduleList2 = entry.getValue();
            remainMpQty = monthRemainQtyMap.get(entry.getKey());
            if (remainMpQty == null || remainMpQty == 0){
                continue;
            }
            for (LhScheduleResultVo lastDayScheduleVo:dayScheduleList2){
                remainMpQty -= (lastDayScheduleVo.getClass1PlanQty() != null ? lastDayScheduleVo.getClass1PlanQty():0);
                remainMpQty -= (lastDayScheduleVo.getClass2PlanQty() != null ? lastDayScheduleVo.getClass2PlanQty():0);
                remainMpQty -= (lastDayScheduleVo.getClass3PlanQty() != null ? lastDayScheduleVo.getClass3PlanQty():0);
            }
            remainMpQty = remainMpQty >= 0 ? remainMpQty : 0;
            monthRemainQtyMap.put(entry.getKey(),remainMpQty);
        }
    }

    /**
     * 增加续作机台
     * @param lastDaySpecMap
     * @param lastScheduleResultVo
     */
    private void addContinuedMachine(Map<String, List<String>> lastDaySpecMap,LhScheduleResultVo lastScheduleResultVo ){
        if (PubUtil.isEmpty(lastDaySpecMap.get(lastScheduleResultVo.getSpecCode()))){
            List<String> machineList = new ArrayList<>();
            machineList.add(lastScheduleResultVo.getLhMachineCode());
            lastDaySpecMap.put(lastScheduleResultVo.getSpecCode(),machineList);
        }else{
            if (lastDaySpecMap.get(lastScheduleResultVo.getSpecCode()).indexOf(lastScheduleResultVo.getLhMachineCode())<0){
                lastDaySpecMap.get(lastScheduleResultVo.getSpecCode()).add(lastScheduleResultVo.getLhMachineCode());
            }
        }
    }

    /**
     * 按照顺序遍历各分组的规格进行机台挑选
     *
     * @param autoLhScheduleResultDTO 排程请求数据，包含工厂等信息
     * @param contextDTO              排程上下文，包含各分组规格的硫化计划、班次时间及其他预先查询数据
     */
    public void traverseSpecificationsBySort(AutoLhScheduleResultDTO autoLhScheduleResultDTO, AutoLhScheduleResultContextDTO contextDTO,
                                             List<LhScheduleResultVo> lhScheduledResultFinalVoList) {
        Date startTraverseMachineTime = new Date();
        contextDTO.setAvailableMachines(availableMachinesList(contextDTO));
        // 4. 查询当前月份内所有定点机台限制记录，按规格过滤时使用
        // 从 T 日硫化计划中收集所有规格代号
        Set<String> tDaySpecCodes = contextDTO.getTDaySpecList();

        List<LhSpecifyMachine> specifyMachineList = lhSpecifyMachineService.queryByFactoryCodeAndSpecCodes(autoLhScheduleResultDTO.getFactoryCode(), tDaySpecCodes);
        contextDTO.setLhSpecifyMachineList(specifyMachineList);

        // 获取品牌的字典内容
        List<SysDictData> dictDataList = sysDictDataService.getType(LOCATION_DICT_TYPE);

        Map<String, String> dictMap = new HashMap<>(16);//key:value   如双钱:01
        if (PubUtil.isNotEmpty(dictDataList)) {
            dictMap = dictDataList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue, (key1, key2) -> key2, LinkedHashMap::new));
        }
        // 根据字典convert 如双钱,回力,飞跃,佳路临,昆仑  转成 字典对应值01,02,03,04
        String brandOrder = contextDTO.getBrandOrder();
        Map<String, String> finalDictMap = dictMap;
        List<String> brandOrderList = Arrays.stream(brandOrder.split(StringPool.COMMA)).map(String::trim).map(finalDictMap::get).filter(Objects::nonNull).collect(Collectors.toList());
        LinkedHashMap<String, Integer> brandOrderMap = new LinkedHashMap<>();
        // 转成对应的顺序MAP
        for (int i = 1; i <=brandOrderList.size() ; i++) {
            brandOrderMap.put(brandOrderList.get(i-1),i);
        }
        // 5. 按照分组遍历各规格进行机台挑选
        // 先遍历续作规格
        int i = 1;
        Date startTime;
        contextDTO.setContinuedScheduleList(orderScheduleList(contextDTO.getContinuedScheduleList(),brandOrderMap));
        List<LhScheduleResultVo> continuedScheduleList  = contextDTO.getContinuedScheduleList();
        for (LhScheduleResultVo vo : continuedScheduleList) {
            startTime = new Date();
            //规格挑选可用机台
            contextDTO.getLogDetail().append(String.format("续作规格:%s,筛选机台列表开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            specifyMachine(contextDTO, vo,lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("续作规格:%s,筛选机台列表结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            //筛选机台
            contextDTO.getLogDetail().append(String.format("续作规格:%s,选中机台并按班排产开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            screenMachine(vo, contextDTO, lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("续作规格:%s,选中机台并按班排产结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            log.debug("第{}个规格{},耗时{}毫秒,总规格数{}!", i++,vo.getSpecCode(),DateUtils.getDiffMillTime(startTime,new Date()),continuedScheduleList.size());
        }
        createAutoScheduleLog(contextDTO,"续作任务");

        // 然后限制规格
        i = 1;
        contextDTO.setRestrictedScheduleList(orderScheduleList(contextDTO.getRestrictedScheduleList(),brandOrderMap));
        List<LhScheduleResultVo> restrictedScheduleList = contextDTO.getRestrictedScheduleList();
        for (LhScheduleResultVo vo : restrictedScheduleList) {
            startTime = new Date();
            contextDTO.getLogDetail().append(String.format("限制规格:%s,筛选机台列表开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            specifyMachine(contextDTO, vo,lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("限制规格:%s,筛选机台列表结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            //筛选机台
            contextDTO.getLogDetail().append(String.format("限制规格:%s,选中机台并按班排产开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            screenMachine(vo, contextDTO, lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("限制规格:%s,选中机台并按班排产结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            log.debug("第{}个规格{},耗时{}毫秒,总规格数{}!", i++,vo.getSpecCode(),DateUtils.getDiffMillTime(startTime,new Date()),restrictedScheduleList.size());
        }
        createAutoScheduleLog(contextDTO,"限制任务");

        // 然后试制规格
        i = 1;
        contextDTO.setTrialScheduleList(orderScheduleList(contextDTO.getTrialScheduleList(),brandOrderMap));
        List<LhScheduleResultVo> trialScheduleList = contextDTO.getTrialScheduleList();
        for (LhScheduleResultVo vo : trialScheduleList) {
            startTime = new Date();
            contextDTO.getLogDetail().append(String.format("试制规格:%s,筛选机台列表开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            specifyMachine(contextDTO, vo,lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("试制规格:%s,筛选机台列表结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            //筛选机台
            contextDTO.getLogDetail().append(String.format("试制规格:%s,选中机台并按班排产开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            screenMachine(vo, contextDTO, lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("试制规格:%s,选中机台并按班排产结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            log.debug("第{}个规格{},耗时{}毫秒,总规格数{}!", i++,vo.getSpecCode(),DateUtils.getDiffMillTime(startTime,new Date()),trialScheduleList.size());
        }
        createAutoScheduleLog(contextDTO,"试制任务");

        // 最后普通规格
        i = 1;
        contextDTO.setRemainingScheduleList(orderScheduleList(contextDTO.getRemainingScheduleList(),brandOrderMap));
        List<LhScheduleResultVo> remainingScheduleList = contextDTO.getRemainingScheduleList();
        for (LhScheduleResultVo vo : remainingScheduleList) {
            startTime = new Date();
            contextDTO.getLogDetail().append(String.format("普通规格:%s,筛选机台列表开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            specifyMachine(contextDTO, vo,lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("普通规格:%s,筛选机台列表结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            //筛选机台
            contextDTO.getLogDetail().append(String.format("普通规格:%s,选中机台并按班排产开始",vo.getSpecCode())).append(ApsConstant.DIVISION);
            screenMachine(vo, contextDTO, lhScheduledResultFinalVoList);
            contextDTO.getLogDetail().append(String.format("普通规格:%s,选中机台并按班排产结束",vo.getSpecCode())).append(ApsConstant.DIVISION);
            log.debug("第{}个规格{},耗时{}毫秒,总规格数{}!", i++,vo.getSpecCode(),DateUtils.getDiffMillTime(startTime,new Date()),remainingScheduleList.size());
        }
        createAutoScheduleLog(contextDTO,"普通任务");

        log.debug("完成挑选机台并按班排产,总耗时{}秒!",DateUtils.getDiffMillTime(startTraverseMachineTime,new Date())/1000);
    }

    /**
     * 排序，并将相同生胎放一起
     * @param scheduleResultVoList
     * @return
     */
    private List<LhScheduleResultVo> orderScheduleList(List<LhScheduleResultVo> scheduleResultVoList,LinkedHashMap<String, Integer> brandOrderMap){
        if (PubUtil.isEmpty(scheduleResultVoList)){
            return scheduleResultVoList;
        }
        // 定义排序规则：组内按 dailyPlanQty 降序，再按 remainMoldQty 降序（null当作0），再按 specCode 升序
//        Comparator<LhScheduleResultVo> groupComparator = Comparator
//                .comparing((LhScheduleResultVo vo) -> vo.getDailyPlanQty() == null ? 0 : vo.getDailyPlanQty(), Comparator.reverseOrder())
//                .thenComparing((LhScheduleResultVo vo) -> vo.getRemainMoldQty() == null ? 0 : vo.getRemainMoldQty(), Comparator.reverseOrder())
//                .thenComparing(LhScheduleResultVo::getSpecCode);


        // 添加对象中的品牌参数
        this.execBrandOrderFormulas(scheduleResultVoList);

        // 新增按照参数品牌排序新增排序条件 zhuoqh-20250717
        // 定义排序规则：组内按 dailyPlanQty 月度计划日需求量 降序，再按 remainMoldQty剩余模数 降序（null当作0），再按 specCode 升序
        Comparator<LhScheduleResultVo> groupComparator = Comparator
                .comparingInt((LhScheduleResultVo vo) -> brandOrderMap.getOrDefault(vo.getBrandOrder(), Integer.MAX_VALUE))
                .thenComparing((LhScheduleResultVo vo) -> vo.getDailyPlanQty() == null ? 0 : vo.getDailyPlanQty(), Comparator.reverseOrder())
                .thenComparing((LhScheduleResultVo vo) -> vo.getRemainMoldQty() == null ? 0 : vo.getRemainMoldQty(), Comparator.reverseOrder())
                .thenComparing(LhScheduleResultVo::getSpecCode);//使用 specCode 的自然顺序排序（默认升序）

        Collections.sort(scheduleResultVoList, groupComparator);
        // 在保证主顺序的情况下，将相同生胎的拉在一起
        Map<String, List<LhScheduleResultVo>> sameEmbryoScheduledMap = scheduleResultVoList.stream().collect(Collectors.groupingBy(item->item.getEmbryoCode()));
        List<String> hasAddEmbryoList = new ArrayList<>();
        List<LhScheduleResultVo> newScheduleResultVoList = new LinkedList<>();
        for (LhScheduleResultVo scheduleResultVo:scheduleResultVoList){
            if (hasAddEmbryoList.indexOf(scheduleResultVo.getEmbryoCode())>=0){
                continue;
            }
            newScheduleResultVoList.addAll(sameEmbryoScheduledMap.get(scheduleResultVo.getEmbryoCode()));
            hasAddEmbryoList.add(scheduleResultVo.getEmbryoCode());
        }
        return newScheduleResultVoList;
    }

    /**
     * 执行品牌顺序公式
     * @param scheduleResultListVo
     */
    private void execBrandOrderFormulas(List<LhScheduleResultVo> scheduleResultListVo) {
        //执行公式
        try {
            QueryFormulaUtil.execFormula(scheduleResultListVo, new String[]{
                    "brandOrder -> getcolvaluewithcondition(T_MDM_MATERIAL_INFO, BRAND, PRODUCT_CODE, productCode, IS_DELETE = 0)",
            });
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
    }
    /**
     * 创建自动排程日志
     *
     * @param contextDTO 上下文
     */
    private void createAutoScheduleLog(AutoLhScheduleResultContextDTO contextDTO,String taskName) {
    }
    /**
     * 规格挑选可用机台
     * 逻辑：
     * 1. 使用 contextDTO.allMachineList 获取所有可用机台。
     * 2. 使用 contextDTO.shiftStart 和 contextDTO.shiftEnd 获取整体班次起始和结束时间（后续作为机台可用时间）。
     * 3. 使用 contextDTO.maintenancePlanList 获取当前月份的维修保养计划。
     * 4. 根据当前班制（contextDTO.getWorkShifts()）和班次时间窗口（contextDTO.shiftTimeWindowDTOList），
     *    为每台机台初始化当日各班的定额，并按机台维修保养计划在各班次内的占用时长扣减定额：
     *    - 若班制为2班，则只初始化一班和二班，三班定额设为0；
     *    - 对于每个班次，找到该机台所有与班次时间窗口有交集的维修计划，
     *         * 如果某维修计划的结束时间 ≥ 班次结束时间，则认为该班无法生产，将该班定额置0；
     *         * 否则，对于每个维修计划：
     *             - 如果维修计划起始时间在班次开始之前，则以班次开始时间为有效开始时间；
     *             - 有效维修时长 = 有效结束时间 - 有效开始时间（单位：小时，向上取整）；
     *         * 将所有维修计划的有效维修时长累加，记为 maintenanceHours；
     *         * 计算每小时扣减额度 = (班次默认定额) / (班次总时长，单位：小时)；
     *         * 该班扣减的定额 = 每小时扣减额度 × maintenanceHours，剩余定额 = max(默认定额 - 扣减定额, 0)。
     *    - 汇总所有班次剩余定额，得到当日总剩余定额 dailyRemainingQuota；
     *    - 同时设定机台的整体可用时间为所有班次的最早开始和最晚结束。
     * 5. 使用 contextDTO.lhSpecifyMachineList 对当前规格进行定点机台限制过滤（保留限制作业机台，剔除不可作业机台）。
     * 6. 根据生产尺寸要求进一步过滤机台（vo.getProSize()）。
     * 7. 对筛选后的机台按照 dailyRemainingQuota 升序排序（若相同则按机台编号排序），存入结果。
     *
     * @param contextDTO              排程上下文数据，包含可用机台、班次信息、维修保养计划、定点机台限制记录、班制等
     * @param vo                      当前需要挑选机台的规格对象
     */
    private void specifyMachine(AutoLhScheduleResultContextDTO contextDTO,
                                LhScheduleResultVo vo,List<LhScheduleResultVo> lhScheduledResultFinalVoList) {
        //计算维修保养占用后的可用时间
        List<LhMachineInfoVo> availableMachines = contextDTO.getAvailableMachines();

        // 5. 定点机台限制（原逻辑不变）
        List<LhSpecifyMachine> allSpecifyList = contextDTO.getLhSpecifyMachineList();
        List<LhSpecifyMachine> currentSpecSpecifyList = allSpecifyList.stream()
                .filter(sm -> vo.getSpecCode().equals(sm.getSpecCode()))
                .collect(Collectors.toList());
        if (currentSpecSpecifyList != null && !currentSpecSpecifyList.isEmpty()) {
            Set<String> allowedMachineCodes = new HashSet<>();
            Set<String> disallowedMachineCodes = new HashSet<>();
            for (LhSpecifyMachine sm : currentSpecSpecifyList) {
                if ("0".equals(sm.getJobType())) {
                    allowedMachineCodes.add(sm.getMachineCode());
                } else if ("1".equals(sm.getJobType())) {
                    disallowedMachineCodes.add(sm.getMachineCode());
                }
            }
            if (!allowedMachineCodes.isEmpty()) {
                availableMachines = availableMachines.stream()
                        .filter(m -> allowedMachineCodes.contains(m.getMachineCode()))
                        .collect(Collectors.toList());
            }
            if (!disallowedMachineCodes.isEmpty()) {
                availableMachines = availableMachines.stream()
                        .filter(m -> !disallowedMachineCodes.contains(m.getMachineCode()))
                        .collect(Collectors.toList());
            }
        }

        // 6. 规格要求过滤：若 vo.getProSize() 不为空，仅保留生产寸口范围符合的机台
        if (vo.getProSize() != null) {
            availableMachines = availableMachines.stream()
                    .filter(m -> {
                        if (m.getDimensionMinimum() != null && m.getDimensionMaximum() != null) {
                            return vo.getProSize().compareTo(m.getDimensionMinimum()) >= 0 &&
                                    vo.getProSize().compareTo(m.getDimensionMaximum()) <= 0;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        if (PubUtil.isNotEmpty(contextDTO.getFullMachineCodeList())){
            //排除满排的机台
            availableMachines = availableMachines.stream()
                    .filter(m -> contextDTO.getFullMachineCodeList().indexOf(m.getMachineCode())<0)
                    .collect(Collectors.toList());
        }
        if (PubUtil.isNotEmpty(availableMachines)){
            for (LhMachineInfoVo machineInfoVo:availableMachines){
                //续作 收尾换模时间 清空
                machineInfoVo.setContinueChangeMoldDate(null);
            }
        }
        //按照定额排序处理机台
        if (ApsConstant.TRUE.equals(vo.getIsContinue())){
           vo.setCopyAvailableLhMachineList(availableMachines);
        }else{
            //非续作机台才处理机台 剩余时间
            availableMachines = dealAvailableMachinesList(contextDTO,availableMachines,vo,lhScheduledResultFinalVoList);
        }
        // 设置可用硫化机台列表
        vo.setAvailableLhMachineList(availableMachines);
        contextDTO.getLogDetail().append(String.format("规格:%s,可用机台列表:%s",vo.getSpecCode(),JSON.toJSONString(availableMachines))).append(ApsConstant.DIVISION);
    }


    /**
     * 根据各班次剩余定额及班次生产能力重新计算机台当日剩余产能，并按剩余产能升序排序。
     * 逻辑说明：
     * 1. 遍历每个班次窗口（仅处理有效班次：SHIFT_SYSTEM_CLASS_1, 2, 3）。
     * 2. 对于每个班次：
     *    - 计算班次总时长（秒）：shiftDurationSec = (shiftEnd - shiftStart) / 1000.
     *    - 根据机台类型选择硫化时间（单位秒）：如果机台类型为机械则使用 vo.getCuringTime()，液压则使用 vo.getHydraulicPressureCuringTime()。
     *    - 计算单班单模硫化量 = shiftDurationSec / curingTime.
     *    - 有效模具数量 = min(vo.getRemainMoldQty(), 2)。
     *    - 理论生产容量 = 单班单模硫化量 * 有效模具数量.
     *    - 该班实际可生产量 = min(理论生产容量, 当前班次剩余定额)（向下取整）。
     * 3. 累加各班次实际生产量，得到机台当日总剩余产能 remainCapacity。
     * 4. 最后对所有机台按 remainCapacity 升序排序（若相同则按机台编号升序）。
     *
     * @param contextDTO       排程上下文数据（包含班次窗口、班制）
     * @param availableMachines 已扣减维修保养后可用机台列表
     * @param vo               当前规格对象，用于获取硫化时间及剩余模数
     * @return 计算并排序后的机台列表
     */
    private List<LhMachineInfoVo> dealAvailableMachinesList(AutoLhScheduleResultContextDTO contextDTO,
                                                            List<LhMachineInfoVo> availableMachines,
                                                            LhScheduleResultVo vo,
                                                            List<LhScheduleResultVo> lhScheduledResultFinalVoList) {

        // 按机台分组，根据机台的维度查询该机台已排规格
        Map<String, List<LhScheduleResultVo>> machineScheduledMap =
                lhScheduledResultFinalVoList.stream().collect(Collectors.groupingBy(LhScheduleResultVo::getLhMachineCode));

        List<LhScheduleResultVo> specList;
        // 用一个新的集合存放通过筛选的机台
        List<LhMachineInfoVo> filteredMachines = new ArrayList<>();
        Date windowStart;
        for (LhMachineInfoVo machineVo : availableMachines) {
            windowStart = getDayStartTime(contextDTO, vo);
            // 先取出该机台已排规格
            specList = machineScheduledMap.get(machineVo.getMachineCode());
            if (machineVo.getMaxMoldNum() != null && machineVo.getMaxMoldNum() >= 2 && CollectionUtils.isNotEmpty(specList)) {
                // 如果非续作且拼模判断不通过，则剔除此机台（不加入最终集合）
                if (!ApsConstant.TRUE.equals(vo.getIsContinue()) &&
                        !isAssemblingMolds(machineVo, specList, vo,contextDTO)) {
                    continue;
                }
            }
            //重算单机台的剩余产能
            windowStart = getMachineLastEndTime(specList, windowStart);
            if (reCalcOneMachineRemainTime(contextDTO, vo, specList, machineVo,windowStart)){
                continue;
            }

            // 将机台加入最终结果列表
            filteredMachines.add(machineVo);
        }

        // 按当日总剩余定额升序排序；若相同则按机台编号升序
       /* filteredMachines.sort(Comparator.comparingInt(LhMachineInfoVo::getRemainCapacity)
                .thenComparing(LhMachineInfoVo::getMachineCode));*/
        return filteredMachines;
    }

    /**
     * 获取机台上规格结束时间
     * @param specList
     * @param windowStart
     * @return
     */
    private Date getMachineLastEndTime(List<LhScheduleResultVo> specList, Date windowStart) {
        if (PubUtil.isEmpty(specList)){
            return windowStart;
        }
        LhScheduleResultVo lastScheduleResultVo = specList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        if (lastScheduleResultVo == null){
            return windowStart;
        }

        if (StringUtils.isEmpty(lastScheduleResultVo.getLeftRightMold())){
            //双模
            if (lastScheduleResultVo.getSpecEndTime() != null){
                windowStart = lastScheduleResultVo.getSpecEndTime();
            }
        }else{
            //存在单模
            if (ApsConstant.L_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                //若最后1笔是L模，则找R模最后时间，目的是获取最大的剩余时间
                lastScheduleResultVo = specList.stream().filter(x->ApsConstant.R_MOLD.equals(x.getLeftRightMold())).sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo != null && lastScheduleResultVo.getSpecEndTime() != null){
                    windowStart = lastScheduleResultVo.getSpecEndTime();
                }
            }else if (ApsConstant.R_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                //若最后1笔是R模，则找L模最后时间，目的是获取最大的剩余时间
                lastScheduleResultVo = specList.stream().filter(x->ApsConstant.L_MOLD.equals(x.getLeftRightMold())).sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo != null && lastScheduleResultVo.getSpecEndTime() != null){
                    windowStart = lastScheduleResultVo.getSpecEndTime();
                }
            }
        }
        return windowStart;
    }

    /**
     * 重算单机台的剩余时间
     * @param contextDTO
     * @param vo
     * @param machineVo
     * @return
     */
    private boolean reCalcOneMachineRemainTime(AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo vo, List<LhScheduleResultVo> specList,
                                               LhMachineInfoVo machineVo, Date windowStart) {

        // 1. 计算机台剩余时间
        Date windowEnd = getClassEndTime(contextDTO, vo);
        // 如果机台存在规格，则起始时间取该机台已排规格中最晚的结束时间
        windowStart = getValidStartTimeWithChangeMould(contextDTO, vo, machineVo, specList, windowStart);
        long shiftDurationSec = windowStart.before(windowEnd)
                ? DateUtils.getDiffMillTime(windowStart, windowEnd) / 1000
                : 0;

        //扣除设备维修(洗模)时长
        int machineDayMaintainTime = getMachineDayMaintainTime(machineVo,contextDTO.getWorkShifts());
        shiftDurationSec = shiftDurationSec > machineDayMaintainTime ? shiftDurationSec - machineDayMaintainTime : 0;
        //扣除辅助时间 = 每班的辅助时间*班制数
        int assistTimeSec = contextDTO.getBrushBagTime() * contextDTO.getWorkShifts();
        shiftDurationSec = shiftDurationSec > assistTimeSec ? shiftDurationSec - assistTimeSec : 0;

        machineVo.setRemainTime(shiftDurationSec);
        //calcOneMachineRemainCapacity(contextDTO, vo, lhScheduledResultFinalVoList, machineVo, shiftDurationSec);
        return shiftDurationSec <=0;
    }

    /**
     * 计算单台剩余产能
     * @param contextDTO 上下文
     * @param vo 当前规格
     * @param lhScheduledResultFinalVoList 机台上在产规格
     * @param machineVo 机台
     */
    private void calcOneMachineRemainCapacity(AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo vo, List<LhScheduleResultVo> lhScheduledResultFinalVoList, LhMachineInfoVo machineVo, boolean isReplenishment) {
        if (machineVo.getRemainTime() <=0){
            //若机台剩余时间为0，则剩余产能也为0
            machineVo.setRemainCapacity(0);
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,剩余时间为0,最终机台剩余产能也为0！",
                    vo.getSpecCode(), machineVo.getMachineCode()))
                    .append(ApsConstant.DIVISION);
            return;
        }
        Integer workShifts = contextDTO.getWorkShifts();
        // 根据机台类型选择硫化时间（单位秒）
        int curingTime = 0;
        if (MachineTypeEnum.MACHINERY.getCode().equals(machineVo.getMachineType())) {
            curingTime = vo.getMachineryCuringTime() != null ? vo.getMachineryCuringTime() : 0;
        } else if (MachineTypeEnum.HYDRAULIC_PRESSURE.getCode().equals(machineVo.getMachineType())) {
            curingTime = vo.getHydraulicPressureCuringTime() != null ? vo.getHydraulicPressureCuringTime() : 0;
        }
        //int realLhTime = curingTime + contextDTO.getBrushBagTime();
        // 有效模具数量：若剩余模数大于2，则按2计算
        Integer moldNum = vo.getMoldQty();
        if (!isReplenishment){
            //非补量时，注：补量的，模数不能变化
            moldNum = getMoldNum(contextDTO, vo, lhScheduledResultFinalVoList, machineVo);
            vo.setMoldQty(moldNum);
        }
        // 计算理论单日产能
        int theoreticalCapacity = CommonUtils.calcPeriodCapacity(BigDecimal.valueOf(machineVo.getRemainTime()), BigDecimal.valueOf(curingTime), moldNum);

        // 2. 计算机台剩余定额
        String[] classQtyFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassQtyFieldNames();
        String[] classQuotaFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassQuotaFieldNames();
        // 2.1 计算字段取值开始位置、结束位置
        int startIndex = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? 0 : classQtyFieldNameArr.length / 2;
        int endIndex = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? classQtyFieldNameArr.length / 2 : classQtyFieldNameArr.length;
        // 2.2 遍历班制数，累计剩余定额
        int remainTotalQuota = 0;
        for (int i = startIndex; i < endIndex; i++) {
            remainTotalQuota += getRemainClassQuota(lhScheduledResultFinalVoList, machineVo, classQtyFieldNameArr[i], classQuotaFieldNameArr[i]);
        }

        // 实际该班次可生产量取理论生产容量与班次剩余定额的较小值，向下取整
        int totalCapacity = (int) Math.floor(Math.min(theoreticalCapacity, remainTotalQuota));
        machineVo.setRemainCapacity(totalCapacity);
        contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,剩余产能:%s,剩余定额:%s,最终机台剩余产能:%s！",
                        vo.getSpecCode(), machineVo.getMachineCode(), theoreticalCapacity, remainTotalQuota, totalCapacity))
                .append(ApsConstant.DIVISION);
    }

    /**
     * 获取模数
     * @param vo
     * @param lhScheduledResultFinalVoList
     * @param machineVo
     * @return
     */
    private Integer getMoldNum(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo vo, List<LhScheduleResultVo> lhScheduledResultFinalVoList, LhMachineInfoVo machineVo) {
        if (machineVo.getMaxMoldNum() ==1){
            //若模台数为1，则模数置1
            return 1;
        }
        Integer moldNum = vo.getRemainMoldQty() != null ? Math.min(vo.getRemainMoldQty(), 2) : 0;
        // lhScheduledResultFinalVoList会出现为空的情况 zhuoqh-20250728
        if (null != lhScheduledResultFinalVoList) {
            if (moldNum == 2 && !ApsConstant.TRUE.equals(machineVo.getIsChangeMoldFlag())){
                //判断当前已排产列表
                LhScheduleResultVo lastScheduleResultVo = lhScheduledResultFinalVoList.stream()
                        .filter(x->x.getLhMachineCode().equals(machineVo.getMachineCode()))
                        .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo != null && StringUtils.isNotEmpty(lastScheduleResultVo.getLeftRightMold())){
                    //若非换模的情况下，有L/R模存在，则当前规格的模数最多为1
                    moldNum = 1;
                    return moldNum;
                }
                //判断前日已排产列表
                List<LhScheduleResultVo> lastScheduleResultList = contextDTO.getLastMachineScheduledMap().get(machineVo.getMachineCode());
                if (PubUtil.isEmpty(lastScheduleResultList)){
                    return moldNum;
                }
                lastScheduleResultVo = lastScheduleResultList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo != null && StringUtils.isNotEmpty(lastScheduleResultVo.getLeftRightMold())){
                    //若非换模的情况下，有L/R模存在，则当前规格的模数最多为1
                    moldNum = 1;
                    return moldNum;
                }
            }
        }


        return moldNum;
    }

    /**
     * 判断机台是否满足拼模条件
     * 说明：
     * 1. 如果机台上没有单模排产的记录（leftRightMold为空的视为双模排产），则直接返回true。
     * 2. 如果存在单模记录，则统计左右模数量：
     *    - 如果数量相等，则表示没有缺失，直接返回true，继续计算剩余产能；
     *    - 如果数量不等，则确定缺失侧，并仅取对应对侧的最新单模记录来判断拼模条件，
     *      如果该最新记录与当前待排规格在硫化时间（允许±30秒）、合模压力（允许±100误差）、
     *      以及模具型腔一致性方面均符合要求，则返回true，否则返回false。
     *
     * @param machineVo 机台对象，包含机台类型、模台数等信息
     * @param specList 该机台已排规格列表（包含左右模信息及相关参数）
     * @param vo 当前待排规格对象（用于对比硫化时间、合模压力、模具型腔等）
     * @return true 表示满足拼模条件；false 表示不满足拼模条件
     */
    private Boolean isAssemblingMolds(LhMachineInfoVo machineVo, List<LhScheduleResultVo> specList, LhScheduleResultVo vo,AutoLhScheduleResultContextDTO contextDTO) {
        // 1. 先过滤出单模排产记录（leftRightMold不为空的）
        List<LhScheduleResultVo> singleMoldRecords = specList.stream()
                .filter(s -> StringUtils.isNotBlank(s.getLeftRightMold()))
                .collect(Collectors.toList());
        // 如果没有单模记录，则说明仅有双模排产，不参与拼模判断，返回true
        if (singleMoldRecords.isEmpty()) {
            return true;
        }

        // 2. 统计单模记录中左右模的数量
        int countL = 0;
        int countR = 0;
        for (LhScheduleResultVo scheduled : singleMoldRecords) {
            String lr = scheduled.getLeftRightMold();
            if (ApsConstant.L_MOLD.equalsIgnoreCase(lr)) {
                countL++;
            } else if (ApsConstant.R_MOLD.equalsIgnoreCase(lr)) {
                countR++;
            }
        }
        // 如果左右模数量相等，则没有缺失侧，直接返回true
        if (countL == countR) {
            return true;
        }

        // 3. 确定缺失的一侧，比如如果左侧多，则缺失R侧
        String missingSide = countL > countR ? ApsConstant.R_MOLD : ApsConstant.L_MOLD;
        // 对侧即为已排记录中存在的那一侧
        String presentSide = missingSide.equalsIgnoreCase(ApsConstant.L_MOLD) ? ApsConstant.R_MOLD : ApsConstant.L_MOLD;

        // 4. 从单模记录中过滤出对侧的记录
        List<LhScheduleResultVo> presentRecords = singleMoldRecords.stream()
                .filter(s -> presentSide.equalsIgnoreCase(s.getLeftRightMold()))
                .collect(Collectors.toList());
        // 如果没有对侧记录，则返回true（可能数据异常，但不强制剔除）
        if (presentRecords.isEmpty()) {
            return true;
        }
        // 5. 按规格结束时间取最新的一条记录
        LhScheduleResultVo lastRecord = presentRecords.stream()
                .max(Comparator.comparing(LhScheduleResultVo::getSpecEndTime))
                .orElse(null);
        if (lastRecord == null) {
            return true;
        }

        // 6. 根据机台类型获取当前规格与最新记录的硫化时间（int类型）
        int currentCuringTime = 0, scheduledCuringTime = 0;
        if (MachineTypeEnum.MACHINERY.getCode().equals(machineVo.getMachineType())) {
            currentCuringTime = vo.getMachineryCuringTime() != null ? vo.getMachineryCuringTime() : 0;
            scheduledCuringTime = lastRecord.getMachineryCuringTime() != null ? lastRecord.getMachineryCuringTime() : 0;
        } else if (MachineTypeEnum.HYDRAULIC_PRESSURE.getCode().equals(machineVo.getMachineType())) {
            currentCuringTime = vo.getHydraulicPressureCuringTime() != null ? vo.getHydraulicPressureCuringTime() : 0;
            scheduledCuringTime = lastRecord.getHydraulicPressureCuringTime() != null ? lastRecord.getHydraulicPressureCuringTime() : 0;
        }
        // 判断硫化时间是否在±30秒范围内
        if (Math.abs(currentCuringTime - scheduledCuringTime) > 30) {
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,当前规格硫化时间:%s,原拼模规格硫化时间:%s,硫化时间相差超过30秒！",vo.getSpecCode(),machineVo.getMachineCode(),currentCuringTime,scheduledCuringTime)).append(ApsConstant.DIVISION);
            return false;
        }

        // 7. 对比合模压力（BigDecimal类型），允许±100误差
        BigDecimal currentClamping = vo.getMouldClampingPressure();
        BigDecimal scheduledClamping = lastRecord.getMouldClampingPressure();
        if (currentClamping == null || scheduledClamping == null ||
                currentClamping.subtract(scheduledClamping).abs().compareTo(BigDecimal.valueOf(100)) > 0) {
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,当前规格合模压力:%s,原拼模规格合模压力:%s,误差超过±100！",vo.getSpecCode(),machineVo.getMachineCode(),currentClamping,scheduledClamping)).append(ApsConstant.DIVISION);
            return false;
        }

        // 8. 对比模具型腔是否一致
        String currentMoldCavity = vo.getMoldCavity();
        String scheduledMoldCavity = lastRecord.getMoldCavity();
        if (currentMoldCavity == null || scheduledMoldCavity == null ||
                !currentMoldCavity.equals(scheduledMoldCavity)) {
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,当前规格模具型腔:%s,原拼模规格模具型腔:%s,不一致！",vo.getSpecCode(),machineVo.getMachineCode(),currentMoldCavity,scheduledMoldCavity)).append(ApsConstant.DIVISION);
            return false;
        }

        // 9. 对比是否试产试制 是否一致，即 试产试制的规格与普通规格不能混拼
        String currentTrial = vo.getIsTrial();
        String scheduledTrial = lastRecord.getIsTrial();
        if (currentTrial == null || scheduledTrial == null ||
                !currentTrial.equals(scheduledTrial)) {
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,当前规格是否试产试制:%s,原拼模规格试产试制:%s,不一致！",vo.getSpecCode(),machineVo.getMachineCode(),currentTrial,scheduledTrial)).append(ApsConstant.DIVISION);
            return false;
        }
        // 如果以上条件均满足，则返回true
        return true;
    }

    /**
     * 考虑换模，获取有效开始时间
     * @param contextDTO 上下文
     * @param scheduleResultVo 当前排程硫化规格
     * @param machineVo 当前硫化机台
     * @param specList 在机硫化规格列表
     * @param defaultStartTime 默认开始时间
     * @return
     */
    private Date getValidStartTimeWithChangeMould(AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo scheduleResultVo,
                                                     LhMachineInfoVo machineVo, List<LhScheduleResultVo> specList,Date defaultStartTime) {
        machineVo.setIsChangeMoldFlag(ApsConstant.FALSE);
        int changeMouldTime = 0;
        //1.获取日班次开始时间
        Date windowStart = defaultStartTime;
        Date changeMouldDate;
        if(PubUtil.isNotEmpty(specList)){
            //2. 若存在在机规格，获取最后规格，即最大的规格结束时间
            //windowStart = getSpecEndTime(contextDTO, specList);
            changeMouldDate = machineVo.getContinueChangeMoldDate() != null ? machineVo.getContinueChangeMoldDate() : getChangeMoldTime(specList,scheduleResultVo,machineVo);
            if (changeMouldDate != null){
                //加上换模时间
                changeMouldTime = contextDTO.getLhParamsMap().get(LhParamCodeEnums.CHANGE_MOULD_TIME.getCode()) != null ? Integer.valueOf(contextDTO.getLhParamsMap().get(LhParamCodeEnums.CHANGE_MOULD_TIME.getCode())):0;
                changeMouldDate = DateUtils.addSeconds(changeMouldDate,changeMouldTime);
                if (changeMouldDate.compareTo(windowStart)>=0){
                    windowStart = changeMouldDate;
                }
                machineVo.setIsChangeMoldFlag(ApsConstant.TRUE);
            }
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,获取有效开始时间,从在机规格列表中获取前规格,是否续作%s,换模时间%s,考虑换模后的实际开始时间:%s！",scheduleResultVo.getSpecCode(),machineVo.getMachineCode(),scheduleResultVo.getIsContinue(),changeMouldTime,DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,windowStart))).append(ApsConstant.DIVISION);
        }else{
            //3.若不存在在机规格，则从前日获取最后规格，即最大的规格结束时间
            List<LhScheduleResultVo> lastDayScheduleList = contextDTO.getLastDayScheduleList();
            //获取前日最后的规格结束时间
            changeMouldDate = machineVo.getContinueChangeMoldDate() != null ? machineVo.getContinueChangeMoldDate() : getChangeMoldTime(lastDayScheduleList,scheduleResultVo,machineVo);
            if (changeMouldDate != null){
                //若前后规格不一致，前规格的结束时间，加上换模时间
                changeMouldTime = contextDTO.getLhParamsMap().get(LhParamCodeEnums.CHANGE_MOULD_TIME.getCode()) != null ? Integer.valueOf(contextDTO.getLhParamsMap().get(LhParamCodeEnums.CHANGE_MOULD_TIME.getCode())):0;
                changeMouldDate = DateUtils.addSeconds(changeMouldDate,changeMouldTime);
                if (changeMouldDate.compareTo(windowStart)>=0){
                    windowStart = changeMouldDate;
                }
                machineVo.setIsChangeMoldFlag(ApsConstant.TRUE);
            }

            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,获取有效开始时间,从前日排程列表中获取前规格,换模时间%s,考虑换模后的实际开始时间:%s！",scheduleResultVo.getSpecCode(),machineVo.getMachineCode(),changeMouldTime,DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,windowStart))).append(ApsConstant.DIVISION);
        }
        return windowStart;
    }

    /**
     * 获取换模时间
     * @param scheduleList
     * @param lhScheduleResultVo
     * @param machineVo
     * @return
     */
    private Date getChangeMoldTime(List<LhScheduleResultVo> scheduleList,LhScheduleResultVo lhScheduleResultVo,LhMachineInfoVo machineVo){
        if (PubUtil.isEmpty(scheduleList)){
            return null;
        }
        LhScheduleResultVo lastScheduleResultVo = scheduleList.stream()
                .filter(x->x.getLhMachineCode().equals(machineVo.getMachineCode()))
                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        if (lastScheduleResultVo == null){
            return null;
        }
        if (StringUtils.isEmpty(lastScheduleResultVo.getLeftRightMold())){
            //1.机台上存在双模排产
            if (!lastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                return doDefaultSpecEndTime(lastScheduleResultVo.getSpecEndTime(),lhScheduleResultVo);
            }
        }else{
            //2.机台非双模排产
            //注：双+L或只有L，若R是空，也不需要换模
            //注：双+R或只有R，若L是空，也不需要换模
            boolean isLChange = false;
            boolean isRChange = false;
            //2.1 机台是否L模排产
            if (ApsConstant.L_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                if (!lastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                    isLChange = true;
                }
                LhScheduleResultVo rlastScheduleResultVo = scheduleList.stream()
                        .filter(x->x.getLhMachineCode().equals(machineVo.getMachineCode()) && (ApsConstant.R_MOLD.equals(x.getLeftRightMold()) || StringUtils.isEmpty(x.getLeftRightMold())))
                        .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (rlastScheduleResultVo != null) {
                    if (!rlastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                        isRChange = true;
                    }
                }
                if (isLChange && isRChange){
                    return doDefaultSpecEndTime(lastScheduleResultVo.getSpecEndTime() != null && rlastScheduleResultVo.getSpecEndTime() != null && lastScheduleResultVo.getSpecEndTime().compareTo(rlastScheduleResultVo.getSpecEndTime())>=0 ? lastScheduleResultVo.getSpecEndTime():rlastScheduleResultVo.getSpecEndTime(),lhScheduleResultVo);
                }
            }else if (ApsConstant.R_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                //2.2 机台是否R模排产
                if (!lastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                    isRChange = true;
                }
                LhScheduleResultVo llastScheduleResultVo = scheduleList.stream()
                        .filter(x->x.getLhMachineCode().equals(machineVo.getMachineCode()) && (ApsConstant.L_MOLD.equals(x.getLeftRightMold()) || StringUtils.isEmpty(x.getLeftRightMold())))
                        .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (llastScheduleResultVo != null) {
                    if (!llastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                        isLChange = true;
                    }
                }

                if (isLChange && isRChange){
                    return doDefaultSpecEndTime(llastScheduleResultVo.getSpecEndTime() != null && lastScheduleResultVo.getSpecEndTime() != null && llastScheduleResultVo.getSpecEndTime().compareTo(lastScheduleResultVo.getSpecEndTime())>=0 ? llastScheduleResultVo.getSpecEndTime():lastScheduleResultVo.getSpecEndTime(),lhScheduleResultVo);
                }
            }
        }
        return null;
    }

    private Date doDefaultSpecEndTime(Date specEndTime,LhScheduleResultVo lastScheduleResultVo){
        if (specEndTime == null){
            //若规格结束时间为空,则取第1班的开始时间
            return lastScheduleResultVo.getClass1StartTime();
        }
        return specEndTime;
    }

    /**
     * 返回可用机台List
     * 扣减维修保养占用的机台定额，并计算剩余产能（remainCapacity）
     *
     * @param contextDTO 排程上下文（包含所有预查询数据：机台、班次时间、维修计划、班次窗口、班制等）
     * @return 处理后的可用机台列表
     */
    private List<LhMachineInfoVo> availableMachinesList(AutoLhScheduleResultContextDTO contextDTO) {
        // 1. 获取所有机台、整体班次时间、维修计划
        List<LhMachineInfo> allMachineList = contextDTO.getAllMachineList();
        List<MdmDeviceMaintenancePlan> maintenancePlanList = contextDTO.getMaintenancePlanList();

        // 取得班次窗口列表（包含 shiftName、startTime、endTime、默认定额 defaultQuota）
        List<ShiftTimeWindowDTO> shiftWindows = contextDTO.getShiftTimeWindowDTOList();

        List<LhMachineInfoVo> availableMachines = new ArrayList<>();
        // 2. 针对每台机台，根据各班次窗口扣减维修保养占用的定额
        for (LhMachineInfo machine : allMachineList) {
            LhMachineInfoVo machineVo = new LhMachineInfoVo();
            BeanUtils.copyProperties(machine, machineVo);
            List<MdmDeviceMaintenancePlan> plans = maintenancePlanList.stream()
                    .filter(plan -> machineVo.getMachineCode().equals(plan.getMachineCode()))
                    .collect(Collectors.toList());
            // 初始化各班次定额：根据班制，若班制不足则对应班次定额置0
            int iQuota = machineVo.getQuota() == null ? 0:machineVo.getQuota();
            for (ShiftTimeWindowDTO shift : shiftWindows) {
                String shiftName = shift.getShiftName();
                if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
                    //T日
                    if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_1.getName().equals(shiftName)) {
                        machineVo.setClass1Quota(iQuota);
                    } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_2.getName().equals(shiftName)) {
                        machineVo.setClass2Quota(iQuota);
                    } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_3.getName().equals(shiftName)) {
                        machineVo.setClass3Quota(iQuota);
                    } else{
                        continue;
                    }
                }else{
                    //T+1日
                    if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_4.getName().equals(shiftName)) {
                        machineVo.setClass1Quota(iQuota);
                    } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_5.getName().equals(shiftName)) {
                        machineVo.setClass2Quota(iQuota);
                    } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_6.getName().equals(shiftName)) {
                        machineVo.setClass3Quota(iQuota);
                    } else{
                        continue;
                    }
                }
                // 扣减当前班次定额，根据维修计划
                if (PubUtil.isNotEmpty(plans)){
                    setQuota(plans, machineVo, shift,contextDTO);
                }
            }
            availableMachines.add(machineVo);
            contextDTO.getMaintainMachineMap().put(machineVo.getMachineCode(),machineVo);
        }
        return availableMachines;
    }

    /**
     * 比对维修保养计划与班次窗口，扣减机台默认定额
     * 逻辑说明（以“一班”为例）：
     * 1. 班次窗口的总时长 = shiftEndTime - shiftStartTime（单位：小时）。
     * 2. 对于每个维修计划，计算其在该班次窗口内的“有效维修时长”，规则如下：
     *    - 有效开始时间 = max(plan.getBeginDate(), shiftStartTime)
     *    - 有效结束时间 = min(plan.getEndDay(), shiftEndTime)
     *    - 有效维修时长 = 向上取整((有效结束时间 - 有效开始时间)的小时数)
     * 3. 如果任一维修计划的有效维修时长 ≥ 班次总时长，或累计有效维修时长 ≥ 班次总时长，则认为该班次完全被维修占用，
     *    剩余定额设为 0。
     * 4. 否则，计算每小时扣减额度 = 默认定额 / 班次总时长，
     *    扣减定额 = 向上取整(每小时扣减额度 × 累计有效维修时长)；
     *    最终剩余定额 = 默认定额 - 扣减定额。
     * 5. 根据班次名称，将剩余定额写入对应班次字段。
     *
     * @param maintenancePlanList 当前月份所有维修保养计划
     * @param machineVo           机台信息Vo（含默认定额 quota）
     * @param shift               当前班次窗口，包含 shiftName、startTime、endTime
     */
    private void setQuota(List<MdmDeviceMaintenancePlan> maintenancePlanList, LhMachineInfoVo machineVo, ShiftTimeWindowDTO shift,AutoLhScheduleResultContextDTO contextDTO) {
        if (PubUtil.isEmpty(maintenancePlanList)){
            return;
        }

        // 获取班次起始和结束时间
        Date shiftStartTime = shift.getStartTime();
        Date shiftEndTime = shift.getEndTime();
        long shiftDurationMillis = shiftEndTime.getTime() - shiftStartTime.getTime();
        int shiftDurationSecond = (int)shiftDurationMillis / 1000; // 班次总时长（秒）

        // 默认定额，从机台信息中获取
        int defaultQuota = machineVo.getQuota();

        int totalMaintenanceSecond = 0;
        boolean shiftInvalid = false; // 标识是否有维修计划导致该班次完全不可用

        for (MdmDeviceMaintenancePlan plan : maintenancePlanList) {
            // 有效开始时间：取计划开始和班次开始较晚的那个
            Date effectiveStart = plan.getBeginDate().before(shiftStartTime) ? shiftStartTime : plan.getBeginDate();
            // 有效结束时间：取计划结束和班次结束较早的那个
            Date effectiveEnd = plan.getEndDay().after(shiftEndTime) ? shiftEndTime : plan.getEndDay();

            long effectiveMillis = effectiveEnd.getTime() - effectiveStart.getTime();
            if (effectiveMillis <= 0) {
                continue; // 如果维修计划与班次无交集，则跳过
            }
            //得到维护 多少秒
            int effectiveSeconds = (int) Math.ceil(effectiveMillis / 1000);

            // 如果单个维修计划的有效时长覆盖整个班次，则该班次不可用
            if (effectiveSeconds >= shiftDurationSecond) {
                shiftInvalid = true;
                //break;
            }
            totalMaintenanceSecond += effectiveSeconds;
            contextDTO.getLogDetail().append(String.format("机台:%s,维护开始时间:%s,维护结束时间:%s,维护时长:%s,维护总时长:%s！",machineVo.getMachineCode(),DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,effectiveStart),DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,effectiveEnd),effectiveSeconds,totalMaintenanceSecond)).append(ApsConstant.DIVISION);
        }

        int remainingQuotaForShift;
        int deductedQuota = 0;
        if (shiftInvalid || totalMaintenanceSecond >= shiftDurationSecond) {
            remainingQuotaForShift = 0;
        } else {
            // 每秒扣减额度 = 默认定额 / 班次总时长
            double quotaPerSecond = shiftDurationSecond > 0 ? (double) defaultQuota / shiftDurationSecond : 0;
            deductedQuota = (int) Math.ceil(quotaPerSecond * totalMaintenanceSecond);
            remainingQuotaForShift = Math.max(defaultQuota - deductedQuota, 0);
        }

        // 根据班次名称写入对应字段
        String shiftName = shift.getShiftName();
        if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_1.getName().equals(shiftName) ||
                ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_4.getName().equals(shiftName)) {
            machineVo.setClass1Quota(remainingQuotaForShift);
            machineVo.setClass1MaintainTime(totalMaintenanceSecond);
            contextDTO.getLogDetail().append(String.format("机台:%s,日第1班,默认定额:%s,维护扣减定额:%s,剩余定额:%s！",machineVo.getMachineCode(),defaultQuota,deductedQuota,remainingQuotaForShift)).append(ApsConstant.DIVISION);
        } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_2.getName().equals(shiftName)||
                ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_5.getName().equals(shiftName)) {
            machineVo.setClass2Quota(remainingQuotaForShift);
            machineVo.setClass2MaintainTime(totalMaintenanceSecond);
            contextDTO.getLogDetail().append(String.format("机台:%s,日第2班,默认定额:%s,维护扣减定额:%s,剩余定额:%s！",machineVo.getMachineCode(),defaultQuota,deductedQuota,remainingQuotaForShift)).append(ApsConstant.DIVISION);
        } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_3.getName().equals(shiftName)||
                ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_6.getName().equals(shiftName)) {
            machineVo.setClass3Quota(remainingQuotaForShift);
            machineVo.setClass3MaintainTime(totalMaintenanceSecond);
            contextDTO.getLogDetail().append(String.format("机台:%s,日第3班,默认定额:%s,维护扣减定额:%s,剩余定额:%s！",machineVo.getMachineCode(),defaultQuota,deductedQuota,remainingQuotaForShift)).append(ApsConstant.DIVISION);
        }
    }

    /**
     * 构建T-1日硫化计划（昨天的计划）
     *
     * @return
     */
    private void buildLastDayLhScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO, AutoLhScheduleResultContextDTO contextDTO) {
        List<LhScheduleResultVo> newLastDayScheduleList;
        //如果当前是T日 则查询T-1日硫化计划 如果当前是T+1日则把T日的计划作为T-1日硫化计划
        if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())) {
            List<LhScheduleResultVo> machineScheduleList;
            Date firstLastScheduleDate;
            newLastDayScheduleList = new ArrayList<>();
            Date lastScheduleTime = DateUtils.addDays(autoLhScheduleResultDTO.getScheduleTime(),-1);
            //往前追溯天数,默认7天
            String traceDays = contextDTO.getLhParamsMap().get(LhParamCodeEnums.LAST_SCHEDULE_TRACE_DAYS.getCode());
            List<LhScheduleResultVo> lastDayScheduleList = lhScheduleResultService.getLastDayLhScheduleResult(lastScheduleTime,
                    autoLhScheduleResultDTO.getFactoryCode(),StringUtils.isNotEmpty(traceDays) ? Integer.valueOf(traceDays):0);

            //设置前日所有规格代号
            List<String> lastDaySpecCodeList = lastDayScheduleList.stream().map(x->x.getSpecCode()).distinct().collect(Collectors.toList());
            contextDTO.setLastDaySpecCodeList(lastDaySpecCodeList);

            Map<String, List<LhScheduleResultVo>> lastMachineScheduledMap = lastDayScheduleList.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()));
            for (Map.Entry<String, List<LhScheduleResultVo>> entry : lastMachineScheduledMap.entrySet()) {
                //按排程日期大->小，T日最后规格排产时间大->小排序
                machineScheduleList = entry.getValue().stream().sorted(Comparator.comparing(LhScheduleResultVo::getScheduleDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(LhScheduleResultVo::getTDaySpecEndTime, (d1,d2)->{
                            if(d1 == null && d2 == null) {
                                return 0;
                            }
                            if(d1 == null) {
                                return -1;
                            }
                            if(d2 == null){
                                return 1;
                            }
                            return d2.compareTo(d1);  // 降序
                        })).collect(Collectors.toList());

                firstLastScheduleDate = machineScheduleList.get(0).getScheduleDate();
                for (LhScheduleResultVo lastScheduleVo:machineScheduleList){
                    if (lastScheduleVo.getScheduleDate().equals(firstLastScheduleDate)){
                        //若与第1笔日期相同，则纳入
                        newLastDayScheduleList.add(lastScheduleVo);
                    }else{
                        break;
                    }
                }
            }
        } else {
            //如果当前是T+1日则把T日的计划作为T-1日硫化计划
            newLastDayScheduleList = contextDTO.getTDayScheduleList();
        }
        //处理T-1日的排程，纳入故障机台待供的规格
        newLastDayScheduleList = doLastDayScheduleListWithFaultMachine(newLastDayScheduleList);
        contextDTO.setLastDayScheduleList(newLastDayScheduleList);
        contextDTO.setLastMachineScheduledMap(newLastDayScheduleList.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode())));
    }

    /**
     * 按机台循环，处理T-1日的排程，纳入故障机台待供的规格
     * @param lastDayScheduleList
     * @return
     */
    private List<LhScheduleResultVo> doLastDayScheduleListWithFaultMachine(List<LhScheduleResultVo> lastDayScheduleList){
        if (PubUtil.isEmpty(lastDayScheduleList)){
            return lastDayScheduleList;
        }
        List<LhScheduleResultVo> machineScheduleList;
        List<LhScheduleResultVo> newLastDayScheduleList = new ArrayList<>();
        Map<String, List<LhScheduleResultVo>> lastMachineScheduledMap = lastDayScheduleList.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()));
        for (Map.Entry<String, List<LhScheduleResultVo>> entry : lastMachineScheduledMap.entrySet()) {
            machineScheduleList = entry.getValue().stream().filter(x->x.getClass1PlanQty() != null || x.getClass2PlanQty() != null || x.getClass3PlanQty() != null).collect(Collectors.toList());
            if (PubUtil.isNotEmpty(machineScheduleList)){
                newLastDayScheduleList.addAll(machineScheduleList);
            }else{
                //若是空的情况下，有可能是机台故障，按机台维度，应纳入最后1笔；
                LhScheduleResultVo lastScheduleResultVo = entry.getValue().stream().sorted(Comparator.comparing(LhScheduleResultVo::getTDaySpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo == null){
                    continue;
                }
                if (StringUtils.isEmpty(lastScheduleResultVo.getLeftRightMold())){
                    //双模排产
                    newLastDayScheduleList.add(lastScheduleResultVo);
                    continue;
                }else{
                    //单/拼模排产
                    if (ApsConstant.L_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                        newLastDayScheduleList.add(lastScheduleResultVo);
                        LhScheduleResultVo rlastScheduleResultVo = entry.getValue().stream()
                                .filter(x->ApsConstant.R_MOLD.equals(x.getLeftRightMold()))
                                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                        if (rlastScheduleResultVo != null) {
                            newLastDayScheduleList.add(rlastScheduleResultVo);
                        }

                    }else if (ApsConstant.R_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                        newLastDayScheduleList.add(lastScheduleResultVo);
                        //机台是否R模排产
                        LhScheduleResultVo llastScheduleResultVo = entry.getValue().stream()
                                .filter(x->ApsConstant.L_MOLD.equals(x.getLeftRightMold()))
                                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                        if (llastScheduleResultVo != null) {
                            newLastDayScheduleList.add(llastScheduleResultVo);
                        }
                    }
                }
            }
        }
        return newLastDayScheduleList;
    }

    /**
     * 构建剩余模具数量（模数情况）
     * <p>
     * 模具关系说明：每个规格在 T_MDM_PRODUCT_MODEL_RELATION 表中有若干副关联模具，
     * 而实际可用的剩余模数，取决于当前（年、月）在 T_MDM_MOULD_USE_STATUS 表中状态为“可用”的模具数量。
     *
     * @param contextDTO  上下文对象，包含 T 日与 T+1 日硫化计划数据
     * @param factoryCode 分厂编号
     * @throws Exception 若查询数据出错，则抛出异常
     */
    private void buildModelRelation(AutoLhScheduleResultContextDTO contextDTO, String factoryCode) throws BusinessException {
        // 1. 从 T 日和 T+1 日硫化计划中，获取所有的规格代号
        Set<String> specCodes = new HashSet<>();
        if (contextDTO.getTDayScheduleList() != null) {
            for (LhScheduleResultVo vo : contextDTO.getTDayScheduleList()) {
                specCodes.add(vo.getSpecCode());
            }
        }
        if (contextDTO.getT1DayScheduleList() != null) {
            for (LhScheduleResultVo vo : contextDTO.getT1DayScheduleList()) {
                specCodes.add(vo.getSpecCode());
            }
        }

        // 2. 根据分厂和规格代号集合查询模具关系数据
        List<MdmSkuMouldRel> modelRelationList = mdmProductModelRelationService.queryBySpecCodes(specCodes, factoryCode);
        // 构造“规格代号 -> 模具关系集合”的映射
        Map<String, List<MdmSkuMouldRel>> specToModelRelations = new HashMap<>();
        Map<String, List<MdmSkuMouldRel>> mouldToModelRelations = new HashMap<>();
        if (modelRelationList != null && !modelRelationList.isEmpty()) {
            for (MdmSkuMouldRel relation : modelRelationList) {
                String specCode = relation.getSpecCode();
                specToModelRelations.computeIfAbsent(specCode, k -> new ArrayList<>()).add(relation);
                mouldToModelRelations.computeIfAbsent(relation.getMouldCode(), k -> new ArrayList<>()).add(relation);
            }
        }

        // 3. 从模具关系数据中提取所有涉及的模具编号，减少后续查询范围
        /*Set<String> mouldCodes = new HashSet<>();
        for (MdmSkuMouldRel relation : modelRelationList) {
            mouldCodes.add(relation.getMouldCode());
        }*/

        // 4. 根据排程时间获取当前年月
       /* Date scheduleTime = contextDTO.getScheduleTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduleTime);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;*/

        // 5. 查询当前年月的模具可用状态数据，仅查询模具编号在上一步集合中的数据
       /* List<MdmMouldUseStatus> mouldStatusList = mdmMouldUseStatusService
                .queryByFactoryCodeYearMonth(factoryCode, year, month, mouldCodes);*/

        // 6. 根据可用状态筛选出当前可用的模具（假设 mouldStatus==1 表示可用）
       /* Set<String> availableMouldCodes = new HashSet<>();
        if (mouldStatusList != null && !mouldStatusList.isEmpty()) {
            for (MdmMouldUseStatus status : mouldStatusList) {
                if (status.getMouldStatus() != null && status.getMouldStatus() == 1) {
                    availableMouldCodes.add(status.getMouldCode());
                }
            }
        }*/

        // 7. 对于每个规格代号，计算可用的剩余模具数量
        Map<String, List<LhMoldInfoVo>> specRemainMoldQtyMap = new HashMap<>();
        List<String> specCodeList = new ArrayList<>();
        specCodeList.addAll(specCodes);
        for (String specCode : specCodes) {
            List<MdmSkuMouldRel> relations = specToModelRelations.get(specCode);
            List<LhMoldInfoVo> availableList = new ArrayList<>();
            List<String> availableCodeList = new ArrayList<>();
            if (relations != null) {
                for (MdmSkuMouldRel relation : relations) {
                    //if (availableMouldCodes.contains(relation.getMouldCode())) {
                    if (availableCodeList.indexOf(relation.getMouldCode())<0){
                        LhMoldInfoVo moldInfoVo = new LhMoldInfoVo();
                        moldInfoVo.setMoldNo(relation.getMouldCode());
                        moldInfoVo.setShareNum(calcMouldShareNum(relation,mouldToModelRelations,specCodeList));
                        availableList.add(moldInfoVo);
                        availableCodeList.add(relation.getMouldCode());
                    }
                }
            }
            specRemainMoldQtyMap.put(specCode, availableList);
        }

        // 8. 更新 T 日与 T+1 日硫化计划 VO 中的剩余模数字段
        if (contextDTO.getTDayScheduleList() != null) {
            for (LhScheduleResultVo vo : contextDTO.getTDayScheduleList()) {
                List<LhMoldInfoVo> list = specRemainMoldQtyMap.get(vo.getSpecCode());
                if (ApsConstant.TRUE.equals(vo.getIsTrial())){
                    //若是试制规格,以月度计划提供的为准 pancd+20250511
                    vo.setRemainMoldQty(vo.getMpMoldQty());
                    vo.setIsSingleMold( vo.getMpMoldQty() != null && vo.getMpMoldQty() == 1 ? ApsConstant.TRUE : ApsConstant.FALSE);
                }else {
                    vo.setRemainMoldQty(list == null ? 0 : list.size());
                    //判断是否单模
                    if(list != null && list.size() == 1){
                        vo.setIsSingleMold(ApsConstant.TRUE);
                    }else {
                        vo.setIsSingleMold(ApsConstant.FALSE);
                    }
                }
                //设置模具可用List
                vo.setAvailLhMoldInfoVoList(list);
            }
        }
        if (contextDTO.getT1DayScheduleList() != null) {
            for (LhScheduleResultVo vo : contextDTO.getT1DayScheduleList()) {
                List<LhMoldInfoVo> list = specRemainMoldQtyMap.get(vo.getSpecCode());
                if (ApsConstant.TRUE.equals(vo.getIsTrial())){
                    //若是试制规格,以月度计划提供的为准 pancd+20250511
                    vo.setRemainMoldQty(vo.getMpMoldQty());
                    vo.setIsSingleMold(vo.getMpMoldQty() == 1 ? ApsConstant.TRUE : ApsConstant.FALSE);
                }else {
                    vo.setRemainMoldQty(list == null ? 0 : list.size());
                    //判断是否单模
                    if(list != null && list.size() == 1){
                        vo.setIsSingleMold(ApsConstant.TRUE);
                    }else {
                        vo.setIsSingleMold(ApsConstant.FALSE);
                    }
                }

                //设置模具可用List
                vo.setAvailLhMoldInfoVoList(list);
            }
        }
        // 9. 将规格可用模具映射保存到上下文中
        contextDTO.setSpecRemainMoldQtyMap(specRemainMoldQtyMap);
    }

    /**
     * 计算模具共用规格数量（规格需是排产规格）
     * @param relation
     * @param mouldToModelRelations
     * @param specCodeList
     * @return
     */
    private int calcMouldShareNum(MdmSkuMouldRel relation,
                                  Map<String, List<MdmSkuMouldRel>> mouldToModelRelations,
                                  List<String> specCodeList){
        if (PubUtil.isEmpty(specCodeList)){
            return 0;
        }
        List<MdmSkuMouldRel> modelRelations = mouldToModelRelations.get(relation.getMouldCode());
        if (PubUtil.isEmpty(modelRelations)){
            return 0;
        }
        int iCount = 0;
        for (MdmSkuMouldRel rel : modelRelations) {
            if (specCodeList.indexOf(rel.getSpecCode())>=0){
                iCount++;
            }
        }
        return iCount;
    }


    /**
     * 把规格代号对应的胎胚号查询出来 且 查找每个规格的胎胚库存情况
     */
    private void buildEmbryoCode(AutoLhScheduleResultContextDTO contextDTO, String factoryCode, Date scheduleTime) throws BusinessException {
    }

    /**
     * 检查是否当月最大天数
     * @param contextDTO
     * @return
     */
    public String checkMaxDayOfMonth(AutoLhScheduleResultContextDTO contextDTO){
        // 获取当前排程时间的日历实例
        Calendar cal = Calendar.getInstance();
        cal.setTime(contextDTO.getScheduleTime());
        // 排程当前日
        int scheduleDay = cal.get(Calendar.DAY_OF_MONTH);

        FactoryMonthPlanProdFinalVo firstMpProdFinalVo = contextDTO.getCurrentMonthPlanList().get(0);
        //获取当前月最大天数
        int maxDayOfMonth = getMaxDays(firstMpProdFinalVo.getProductionStartDate());
        if (scheduleDay == maxDayOfMonth) {
            return ApsConstant.TRUE;
        }
        return ApsConstant.FALSE;
    }

    /**
     * 根据传入的上下文对象，构建T日和T+1日排产结果
     *
     * @param contextDTO 上下文
     */
    private void buildTAndT1Spec(AutoLhScheduleResultContextDTO contextDTO, String factoryCode) throws BusinessException {
        // 获取当前排程时间的日历实例
        Calendar cal = Calendar.getInstance();
        cal.setTime(contextDTO.getScheduleTime());
        // 排程当前日
        int scheduleDay = cal.get(Calendar.DAY_OF_MONTH);
        // 当前月最大天数
        //int maxDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        //List<FactoryMonthPlanProdFinalVo> currentMonthPlanList = getFactoryMonthPlanProdFinal(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, factoryCode);
        //List<FactoryMonthPlanProdFinalVo> currentMonthPlanList = getFactoryMonthPlanProdFinalByDate(contextDTO.getScheduleTime(), factoryCode);
        List<FactoryMonthPlanProdFinalVo> currentMonthPlanList = contextDTO.getCurrentMonthPlanList();
        if (PubUtil.isEmpty(currentMonthPlanList)) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.monthlyPlanNotFound"));
        }

        FactoryMonthPlanProdFinalVo firstMpProdFinalVo = currentMonthPlanList.get(0);
        Date mpStartDate = firstMpProdFinalVo.getProductionStartDate();
        contextDTO.setMpYear(firstMpProdFinalVo.getYear());
        contextDTO.setMpMonth(firstMpProdFinalVo.getMonth());
        //获取当前月最大天数
        int maxDayOfMonth = getMaxDays(mpStartDate);
        List<FactoryMonthPlanProdFinalVo> nextMonthPlanList = new ArrayList<>();

        // 如果当前日为当月最后一天，则需要查询下个月的计划数据
        if (scheduleDay == maxDayOfMonth) {
            // 计算下个月的年份和月份
            /*cal.add(Calendar.MONTH, 1);
            int nextYear = cal.get(Calendar.YEAR);
            int nextMonth = cal.get(Calendar.MONTH) + 1;
            nextMonthPlanList = getFactoryMonthPlanProdFinal(nextYear, nextMonth, factoryCode);*/
            Date nextDate = DateUtils.addDays(contextDTO.getScheduleTime(),1);
            nextMonthPlanList = getFactoryMonthPlanProdFinalByDate(nextDate, factoryCode);
            if (PubUtil.isEmpty(nextMonthPlanList)) {
                throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.nextMonthlyPlanNotFound"));
            }
        }

        //获取天数偏差
        int diffDays = DateUtils.getDayInterval(contextDTO.getScheduleTime(),mpStartDate);
        //存储对应的实际天数
        int currentDay = diffDays+1;

        // 初始化两个结果 List
        List<LhScheduleResultVo> currentDayList = new ArrayList<>();
        List<LhScheduleResultVo> nextDayList = new ArrayList<>();

        Set<String> allSpecCodes = new HashSet<>();
        // 处理T日数据（来自当前月计划）
        // 如 "day31"
        Long dayValue,dailyPlanQty;
        String currentDayField = "day" + currentDay;
        for (FactoryMonthPlanProdFinalVo monthPlan : currentMonthPlanList) {
            dayValue = monthPlan.getFieldValueByFieldName(currentDayField) != null ? (Long) monthPlan.getFieldValueByFieldName(currentDayField) : 0;
            dailyPlanQty = dayValue;
            if (dayValue <=0){
                //若T日该规格无效，加尝试加载 未排产过的规格 pancd+20250516
                dailyPlanQty = addNoExistsSpec(contextDTO,monthPlan,currentDay);
                if (dailyPlanQty <=0){
                    continue;
                }
                contextDTO.getLogDetail().append(String.format("规格:%s,纳入增补未排产过的规格!",monthPlan.getSpecCode())).append(ApsConstant.DIVISION);
            }
            LhScheduleResultVo result = this.buildFactoryMonthPlanProdFinalVo(monthPlan, contextDTO);
            result.setDailyPlanQty(dailyPlanQty.intValue());
            currentDayList.add(result);
            allSpecCodes.add(result.getSpecCode());
        }

        // 初始化月度剩余
        //追加前日的规格
        List<LhScheduleResultVo> lastDayScheduleList = contextDTO.getLastDayScheduleList();
        for (LhScheduleResultVo scheduleResultVo:lastDayScheduleList){
            allSpecCodes.add(scheduleResultVo.getSpecCode());
        }
        initRemainMpQtyMap(factoryCode, contextDTO, allSpecCodes);

        // 处理T+1日数据
        // 如果当前日为月末，则T+1日为下个月的1号，否则T+1日为当前月的 (currentDay+1)
        int nextDay;
        if (scheduleDay == maxDayOfMonth) {
            nextDay = 1;
        } else {
            nextDay = currentDay + 1;
        }
        // 如 "day1" 或 "day32"（注意：day32不存在，因此这里只可能是day1或其他有效值）
        String nextDayField = "day" + nextDay;

        // 根据是否为跨月选择对应的计划数据
        List<FactoryMonthPlanProdFinalVo> targetPlanList = (scheduleDay == maxDayOfMonth) ? nextMonthPlanList : currentMonthPlanList;
        for (FactoryMonthPlanProdFinalVo monthPlan : targetPlanList) {
            dayValue = monthPlan.getFieldValueByFieldName(nextDayField) != null ? (Long) monthPlan.getFieldValueByFieldName(nextDayField) : 0;
            if (dayValue <=0 ){
                continue;
            }
            LhScheduleResultVo result = this.buildFactoryMonthPlanProdFinalVo(monthPlan, contextDTO);
            result.setDailyPlanQty(dayValue.intValue());
            // 同样复制其他需要的属性
            nextDayList.add(result);
        }

        //追加昨天存在但今天不存在的规格(生产异常导致的计划延误)
        addTSpec(contextDTO,currentDayList,currentMonthPlanList,currentDay);
        // 将结果设置到上下文中
        contextDTO.setTDayScheduleList(currentDayList);

        Set<String> tDaySpecCodes = new HashSet<>();
        for (LhScheduleResultVo scheduleResultVo:currentDayList){
            tDaySpecCodes.add(scheduleResultVo.getSpecCode());
        }
        contextDTO.setTDaySpecList(tDaySpecCodes);

        contextDTO.setT1DayScheduleList(nextDayList);
        if (PubUtil.isNotEmpty(currentDayList)){
            contextDTO.setTDayAllSpecCodeList(currentDayList.stream().map(x->x.getSpecCode()).distinct().collect(Collectors.toList()));
        }
    }

    /**
     * 获取月起始日的最大天数
     * @param mpStartDate
     * @return
     */
    private int getMaxDays(Date mpStartDate){
        Calendar cal = Calendar.getInstance();
        cal.setTime(mpStartDate);
        int iDay = cal.get(Calendar.DAY_OF_MONTH);
        if (iDay == 1){
            //若起始日是1号,则取当月的最大天数
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }else{
            //获取前1天的值，作为最大天数
            cal.setTime(DateUtils.addDays(mpStartDate,-1));
            return cal.get(Calendar.DAY_OF_MONTH);
        }
    }

    /**
     * 加载未排产过的规格（含前日往前追溯）
     * @param contextDTO
     * @param monthPlan
     * @param currentDay
     * @return
     */
    private long addNoExistsSpec(AutoLhScheduleResultContextDTO contextDTO,FactoryMonthPlanProdFinalVo monthPlan,int currentDay){
        if (monthPlan == null){
            return 0;
        }
        List<String> lastDaySpecCodeList = contextDTO.getLastDaySpecCodeList();
        if (PubUtil.isEmpty(lastDaySpecCodeList)){
            return 0;
        }
        if (lastDaySpecCodeList.indexOf(monthPlan.getSpecCode())>=0){
            //若规格在前日(含往前推N天)存在，则略过
            return 0;
        }

        // 月度计划也往前追溯
        String currentDayField;
        //往前追溯天数,默认7天
        int traceDays = contextDTO.getLhParamsMap().get(LhParamCodeEnums.LAST_SCHEDULE_TRACE_DAYS.getCode()) != null ? Integer.valueOf(contextDTO.getLhParamsMap().get(LhParamCodeEnums.LAST_SCHEDULE_TRACE_DAYS.getCode())) : 0;
        int iMin = currentDay - 1 - traceDays;
        iMin = iMin < 1 ? 1:iMin;
        long dayValue;
        long resultValue = 0;
        for (int i = currentDay - 1; i>=iMin; i--){
            currentDayField = "day" + i;
            dayValue = monthPlan.getFieldValueByFieldName(currentDayField) != null ? (long)monthPlan.getFieldValueByFieldName(currentDayField) : 0;
            if (dayValue <=0 ){
                continue;
            }
            resultValue = dayValue;
        }
        //获取最后1笔有计划值的
        return resultValue;
    }
    /**
     *  追加昨天存在但今天不存在的规格
     *  检查 【截止今天的月度剩余量】（月度剩余量-今天至月底的计划量），若【截止今天的月度剩余量】>0，
     *  则继续将该规格纳入续作排产规格。
     * @param contextDTO
     * @param currentDayList
     * @param monthPlanList
     */
    private void addTSpec(AutoLhScheduleResultContextDTO contextDTO,List<LhScheduleResultVo> currentDayList,List<FactoryMonthPlanProdFinalVo> monthPlanList,int currentDay){
        Map<String,String> lhParamsMap = contextDTO.getLhParamsMap();
        if (ApsConstant.FALSE.equals(lhParamsMap.get(LhParamCodeEnums.IS_START_PLAN_DELAY_AUTO_SUPPLE.getCode()))){
            return;
        }
        List<LhScheduleResultVo> lastDayScheduleList = contextDTO.getLastDayScheduleList();
        List<String> specList = currentDayList.stream().map(x->x.getSpecCode()).collect(Collectors.toList());
        Map<String, List<FactoryMonthPlanProdFinalVo>> monthPlanMap = monthPlanList.stream().collect(Collectors.groupingBy(item->item.getSpecCode()));
        int toEndPlanQty,remainMpQty,diffQty,tDailyQty;
        LhScheduleResultVo newScheduleResult;
        for (LhScheduleResultVo lastDayScheduleVo:lastDayScheduleList){
            if (specList.indexOf(lastDayScheduleVo.getSpecCode())>=0){
                //若前日规格存在于今日规格，继续
                continue;
            }
            //获取T日到月度的计划量
            List<FactoryMonthPlanProdFinalVo> monthPlanSpecList = monthPlanMap.get(lastDayScheduleVo.getSpecCode());
            if (PubUtil.isEmpty(monthPlanSpecList)){
                continue;
            }
            if (!ApsConstant.TRUE.equals(lhParamsMap.get(LhParamCodeEnums.IS_ALLOW_ADVANCE_SCHEDULE_SPEC.getCode()))){
                toEndPlanQty = getTDayToEndPlanQty(monthPlanSpecList,currentDay);
            }else{
                toEndPlanQty = 0;
            }

            remainMpQty = contextDTO.getRemainMpQtyMap().getOrDefault(lastDayScheduleVo.getSpecCode(), 0);
            diffQty = remainMpQty - toEndPlanQty;
            diffQty = diffQty > 0 ? diffQty:0;
            if (diffQty >0){
                newScheduleResult = this.buildFactoryMonthPlanProdFinalVo(monthPlanSpecList.get(0), contextDTO);
                BigDecimal durationSec = contextDTO.getWorkShifts() == 2 ? BigDecimal.valueOf(43200):BigDecimal.valueOf(28800);
                //刷囊时间
                //String brushBagTime = lhParamsMap.get(LhParamCodeEnums.BRUSH_BAG_TIME.getCode());
                //int iBrushBagTime = StringUtils.isNotEmpty(brushBagTime) ? Integer.valueOf(brushBagTime) : 0;
                durationSec = BigDecimalUtils.sub(durationSec,contextDTO.getBrushBagTime());
                //BigDecimal lhTime = BigDecimalUtils.add(lastDayScheduleVo.getLhTime(),contextDTO.getBrushBagTime());
                //单班硫化量
                int shiftCapacity = CommonUtils.calcPeriodCapacity(durationSec, lastDayScheduleVo.getLhTime(),lastDayScheduleVo.getMoldQty());
                //日计划量 = 单班硫化量 * 班制
                tDailyQty = contextDTO.getWorkShifts()*shiftCapacity;
                //日计划量与剩余计划差值比，取小，作为T日计划量
                tDailyQty = diffQty > tDailyQty ? tDailyQty:diffQty;
                newScheduleResult.setDailyPlanQty(tDailyQty);
                if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
                    newScheduleResult.setClass1Analysis(ApsConstant.PLAN_DELAY_AUTO_SUPPLE);
                    newScheduleResult.setClass2Analysis(ApsConstant.PLAN_DELAY_AUTO_SUPPLE);
                }else{
                    newScheduleResult.setClass4Analysis(ApsConstant.PLAN_DELAY_AUTO_SUPPLE);
                    newScheduleResult.setClass5Analysis(ApsConstant.PLAN_DELAY_AUTO_SUPPLE);
                }
                currentDayList.add(newScheduleResult);
            }
        }
    }

    /**
     * 获取T日到月度的计划量
     * @param monthPlanList
     * @param currentDay
     * @return
     */
    private int getTDayToEndPlanQty(List<FactoryMonthPlanProdFinalVo> monthPlanList,int currentDay){
        long dayValue;
        int totalPlanQty = 0;
        String currentDayField;
        for (FactoryMonthPlanProdFinalVo monthPlan:monthPlanList){
            for (int iDay = currentDay + 1;iDay<= 31;iDay++){
                currentDayField = "day" + iDay;
                dayValue = monthPlan.getFieldValueByFieldName(currentDayField) != null ? (Long) monthPlan.getFieldValueByFieldName(currentDayField) : 0;
                totalPlanQty += dayValue;
            }
        }
        return totalPlanQty;
    }
    /**
     * 月计划构建硫化VO对象
     *
     * @param monthPlan
     * @return
     */
    private LhScheduleResultVo buildFactoryMonthPlanProdFinalVo(FactoryMonthPlanProdFinalVo monthPlan, AutoLhScheduleResultContextDTO contextDTO) {
        LhScheduleResultVo vo = new LhScheduleResultVo();
        vo.setFactoryCode(monthPlan.getFactoryCode());
        vo.setProductCode(monthPlan.getProductCode());
        vo.setSpecCode(monthPlan.getSpecCode());
        vo.setSpecDesc(monthPlan.getProductDesc());
        vo.setMpMoldQty(monthPlan.getMouldQty());
        vo.setScheduleDate(contextDTO.getScheduleTime());
        vo.setRealScheduleDate(contextDTO.getScheduleTime());
        //vo.setProSize(monthPlan.getProSize());
        vo.setDataSource("0");
        vo.setIsDelivery(monthPlan.getIsDeliveryDate() == null ? ApsConstant.FALSE : String.valueOf(monthPlan.getIsDeliveryDate()));
        vo.setMonthPlanNo(monthPlan.getProductionNo());
        vo.setMonthPlanVersion(monthPlan.getProductionVersion());
        //初始化班制时间
        initShiftTime(contextDTO, vo);

        return vo;
    }

    /**
     * 实始化班制时间
     * @param contextDTO
     * @param vo
     */
    private void initShiftTime(AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo vo) {
        // 初始化班制时间
        // 从上下文中获取班次窗口列表（ShiftTimeWindowDTO 包含：shiftName、startTime、endTime、defaultQuota）
        List<ShiftTimeWindowDTO> shiftList = contextDTO.getShiftTimeWindowDTOList();
        // 当前班制，取值2或3
        Integer workShifts = contextDTO.getWorkShifts();

        // 遍历班次窗口，根据班次名称（用枚举替代）设置相应的班次开始/结束时间
        // 注意：对于2班制，忽略“三班”和“次日三班”；对于3班制，则全部初始化
        for (ShiftTimeWindowDTO shift : shiftList) {
            String shiftName = shift.getShiftName();
            Date startTime = shift.getStartTime();
            Date endTime = shift.getEndTime();

            if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_1.getName().equals(shiftName)) {
                vo.setClass1StartTime(startTime);
                vo.setClass1EndTime(endTime);
            } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_2.getName().equals(shiftName)) {
                vo.setClass2StartTime(startTime);
                vo.setClass2EndTime(endTime);
            } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_3.getName().equals(shiftName)) {
                // 仅在3班制下才初始化“三班”
                if (workShifts != null && workShifts >= 3) {
                    vo.setClass3StartTime(startTime);
                    vo.setClass3EndTime(endTime);
                }
            } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_4.getName().equals(shiftName)) {
                // 次日一班，无论2班还是3班都需要初始化
                vo.setClass4StartTime(startTime);
                vo.setClass4EndTime(endTime);
            } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_5.getName().equals(shiftName)) {
                // 次日二班，无论2班还是3班都需要初始化
                vo.setClass5StartTime(startTime);
                vo.setClass5EndTime(endTime);
            } else if (ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_6.getName().equals(shiftName)) {
                // 次日三班仅在3班制下有效
                if (workShifts != null && workShifts >= 3) {
                    vo.setClass6StartTime(startTime);
                    vo.setClass6EndTime(endTime);
                }
            }
        }
    }

    /**
     * 分厂月生产计划排产结果-最终版本
     *
     * @return
     */
    private List<FactoryMonthPlanProdFinalVo> getFactoryMonthPlanProdFinal(Integer year, Integer month, String factoryCode) {
        //构造查询条件
        FactoryMonthPlanProdFinalQueryDto queryDto = new FactoryMonthPlanProdFinalQueryDto();
        queryDto.setFactoryCode(factoryCode);
        queryDto.setYear(year);
        queryDto.setMonth(month);
        //查询月度计划
        List<FactoryMonthPlanProdFinalVo> voList = factoryMonthPlanProdFinalRemoteService.getProdResult(queryDto);
        //需要按照唯一key的维度分组
        //monthPlan.getProductCode()+"_"+monthPlan.getSpecCode()+"_"+monthPlan.getIsDeliveryDate()
        // 合并操作
        voList = this.mergeFactoryMonthPlanProdFinal(voList);
        return voList;
    }

    /**
     * 分厂月生产计划排产结果-最终版本
     *
     * @return
     */
    public List<FactoryMonthPlanProdFinalVo> getFactoryMonthPlanProdFinalByDate(Date scheduleDate, String factoryCode) {
        //构造查询条件
        FactoryMonthPlanProdFinalQueryDto queryDto = new FactoryMonthPlanProdFinalQueryDto();
        queryDto.setFactoryCode(factoryCode);
        queryDto.setProductionDate(scheduleDate);
        //查询月度计划
        List<FactoryMonthPlanProdFinalVo> voList = factoryMonthPlanProdFinalRemoteService.getMonthPlanProdResult(queryDto);
        //需要按照唯一key的维度分组
        //monthPlan.getProductCode()+"_"+monthPlan.getSpecCode()+"_"+monthPlan.getIsDeliveryDate()
        // 合并操作
        voList = this.mergeFactoryMonthPlanProdFinal(voList);
        return voList;
    }

    /**
     * 获取销售计划
     * @param monthPlanVersion
     * @return
     */
    public List<MonthPlanRequireStock> getSaleMonthPlanRequireStock(String monthPlanVersion){
        return  factoryMonthPlanProdFinalRemoteService.getSaleMonthPlanRequireStock(monthPlanVersion);
    }


    /**
     * 合并月度计划的数据
     * @param voList
     * @return
     */
    private List<FactoryMonthPlanProdFinalVo> mergeFactoryMonthPlanProdFinal(List<FactoryMonthPlanProdFinalVo> voList) {
        // 用于存放合并后的结果，key 为 productCode_specCode_isDeliveryDate
        Map<String, FactoryMonthPlanProdFinalVo> resultMap = new HashMap<>();

        for (FactoryMonthPlanProdFinalVo vo : voList) {
            // 构建分组的唯一key
            String key = vo.getProductCode() + "_" + vo.getSpecCode() + "_" + vo.getIsDeliveryDate();

            if (resultMap.containsKey(key)) {
                // 如果已经存在，则累加各天的排产量
                FactoryMonthPlanProdFinalVo merged = resultMap.get(key);
                //每天的量 合并
//                merged.setDay1( safeAdd(merged.getDay1(), vo.getDay1()) );
//                merged.setDay2( safeAdd(merged.getDay2(), vo.getDay2()) );
//                merged.setDay3( safeAdd(merged.getDay3(), vo.getDay3()) );
//                merged.setDay4( safeAdd(merged.getDay4(), vo.getDay4()) );
//                merged.setDay5( safeAdd(merged.getDay5(), vo.getDay5()) );
//                merged.setDay6( safeAdd(merged.getDay6(), vo.getDay6()) );
//                merged.setDay7( safeAdd(merged.getDay7(), vo.getDay7()) );
//                merged.setDay8( safeAdd(merged.getDay8(), vo.getDay8()) );
//                merged.setDay9( safeAdd(merged.getDay9(), vo.getDay9()) );
//                merged.setDay10( safeAdd(merged.getDay10(), vo.getDay10()) );
//                merged.setDay11( safeAdd(merged.getDay11(), vo.getDay11()) );
//                merged.setDay12( safeAdd(merged.getDay12(), vo.getDay12()) );
//                merged.setDay13( safeAdd(merged.getDay13(), vo.getDay13()) );
//                merged.setDay14( safeAdd(merged.getDay14(), vo.getDay14()) );
//                merged.setDay15( safeAdd(merged.getDay15(), vo.getDay15()) );
//                merged.setDay16( safeAdd(merged.getDay16(), vo.getDay16()) );
//                merged.setDay17( safeAdd(merged.getDay17(), vo.getDay17()) );
//                merged.setDay18( safeAdd(merged.getDay18(), vo.getDay18()) );
//                merged.setDay19( safeAdd(merged.getDay19(), vo.getDay19()) );
//                merged.setDay20( safeAdd(merged.getDay20(), vo.getDay20()) );
//                merged.setDay21( safeAdd(merged.getDay21(), vo.getDay21()) );
//                merged.setDay22( safeAdd(merged.getDay22(), vo.getDay22()) );
//                merged.setDay23( safeAdd(merged.getDay23(), vo.getDay23()) );
//                merged.setDay24(Math.toIntExact(safeAdd(merged.getDay24(), vo.getDay24())));
//                merged.setDay25( safeAdd(merged.getDay25(), vo.getDay25()) );
//                merged.setDay26( safeAdd(merged.getDay26(), vo.getDay26()) );
//                merged.setDay27( safeAdd(merged.getDay27(), vo.getDay27()) );
//                merged.setDay28( safeAdd(merged.getDay28(), vo.getDay28()) );
//                merged.setDay29( safeAdd(merged.getDay29(), vo.getDay29()) );
//                merged.setDay30( safeAdd(merged.getDay30(), vo.getDay30()) );
//                merged.setDay31( safeAdd(merged.getDay31(), vo.getDay31()) );
            } else {
                // 第一次出现的key，直接放入Map
                resultMap.put(key, vo);
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    /**
     * 安全的加法：处理null值，将null视为0
     */
    private Long safeAdd(Long a, Long b) {
        return (a == null ? 0L : a) + (b == null ? 0L : b);
    }

    /**
     * 根据传入的排程时间，动态构建班次时间窗口：
     * - 2 班制：排 4 个班（如：一班、二班、次日一班、次日二班）
     * - 3 班制：排 6 个班（如：一班、二班、三班、次日一班、次日二班、次日三班）
     * <p>
     * 班次起始时间及每班时长均动态计算：
     * 每班时长 = 24 / (每天班次数) 小时
     * 起始时间从配置读取（例如 19:00），后续自动累加计算结束时间。
     * <p>
     * 班次名称直接从 ShiftSystemNameEnum 中获取，不进行字符串拼接。
     */
    private List<ShiftTimeWindowDTO> buildShiftTimeWindows(Map<String,String> lhParamsMap, Date scheduleTime) {
        //取系统配置参数_班制 如果不为空用参数覆盖
        String classSystem = lhParamsMap.get(LhParamCodeEnums.CLASS_SYSTEM.getCode());
        // 1) 从配置获取班制，此处假设返回 "2" 或 "3" 字符串 没有就默认两班制
        int shiftsPerDay = StringUtils.isNotEmpty(classSystem) ? Integer.valueOf(classSystem) : ShiftSystemEnum.SHIFT_SYSTEM_2.getCode();
        // 2) 从配置获取班次起始时间（小时和分钟），例如配置动态读取，此处示例为 19:00
        int startHours = 19;
        int startMinutes = 0;
        int startDays = 0;
        //取系统配置参数_班次起始时间 如果不为空用参数覆盖
        String classSystemStartDaysParam = lhParamsMap.get(LhParamCodeEnums.CLASS_SYSTEM_START_DAYS.getCode());
        if (StringUtils.isNotEmpty(classSystemStartDaysParam)) {
            startDays = Integer.valueOf(classSystemStartDaysParam);
        }
        //取系统配置参数_班次起始时间 如果不为空用参数覆盖
        String classSystemStartHoursParam = lhParamsMap.get(LhParamCodeEnums.CLASS_SYSTEM_START_HOURS.getCode());
        if (StringUtils.isNotEmpty(classSystemStartHoursParam)) {
            startHours = Integer.valueOf(classSystemStartHoursParam);
        }
        //取系统配置参数_分钟起始时间 如果不为空用参数覆盖
        String classSystemStartMinutesParam = lhParamsMap.get(LhParamCodeEnums.CLASS_SYSTEM_START_MINUTES.getCode());
        if (StringUtils.isNotEmpty(classSystemStartMinutesParam)) {
            startMinutes = Integer.valueOf(classSystemStartMinutesParam);
        }

        // 3) 以传入的 scheduleTime 的日期为基准，归零时间（仅保留日期部分）
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUtils.addDays(scheduleTime,startDays));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date baseDate = cal.getTime();

        // 4) 设定排班天数，例如排2天
        int totalShifts = shiftsPerDay * ApsConstant.TWO_DAY;

        // 5) 动态计算每班时长（小时）= 24 / 每天班次数
        int shiftDurationHours = 24 / shiftsPerDay;

        // 6) 根据班制选择对应的班次名称枚举数组
        ShiftSystemNameEnum[] shiftEnums;
        if (ShiftSystemEnum.SHIFT_SYSTEM_2.getCode().equals(shiftsPerDay)) {
            shiftEnums = new ShiftSystemNameEnum[]{
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_1, // 一班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_2, // 二班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_4, // 次日一班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_5  // 次日二班
            };
        } else if (ShiftSystemEnum.SHIFT_SYSTEM_3.getCode().equals(shiftsPerDay)) {
            shiftEnums = new ShiftSystemNameEnum[]{
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_1, // 一班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_2, // 二班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_3, // 三班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_4, // 次日一班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_5, // 次日二班
                    ShiftSystemNameEnum.SHIFT_SYSTEM_CLASS_6  // 次日三班
            };
        } else {
            throw new BusinessException(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.unsupportedShiftSystem" + shiftsPerDay));
        }

        // 7) 初始化第一个班次的起始时间（基于 baseDate 加上配置起始时刻）
        Calendar currentShiftCal = Calendar.getInstance();
        currentShiftCal.setTime(baseDate);
        currentShiftCal.set(Calendar.HOUR_OF_DAY, startHours);
        currentShiftCal.set(Calendar.MINUTE, startMinutes);
        currentShiftCal.set(Calendar.SECOND, 0);
        currentShiftCal.set(Calendar.MILLISECOND, 0);

        // 8) 依次生成各个班次的时间窗口
        List<ShiftTimeWindowDTO> shifts = new ArrayList<>();
        for (int i = 0; i < totalShifts; i++) {
            Date shiftStart = currentShiftCal.getTime();
            // 根据每班时长动态计算结束时间
            Calendar shiftEndCal = (Calendar) currentShiftCal.clone();
            shiftEndCal.add(Calendar.HOUR_OF_DAY, shiftDurationHours);
            Date shiftEnd = shiftEndCal.getTime();

            // 从预定义的枚举数组中直接获取班次名称
            ShiftSystemNameEnum shiftEnum = shiftEnums[i];
            String shiftName = shiftEnum.getName();
            shifts.add(new ShiftTimeWindowDTO(shiftName, shiftStart, DateUtils.addSeconds(shiftEnd,-1)));

            // 更新当前班次起始时间为本班次结束时间
            currentShiftCal = shiftEndCal;
        }

        return shifts;
    }

    /**
     * 获取今日最后一个班次的结束时间
     * @return
     */
    public ShiftTimeWindowDTO getTodayLastShiftTime(String factoryCode, Date scheduleTime){
        //返回班次对象
        ShiftTimeWindowDTO returnTimeWindow = new ShiftTimeWindowDTO();
        Map<String, String> paramsMap = lhParamsService.listLhParams(factoryCode);
        //取系统配置参数_班制 如果不为空用参数覆盖
        String classSystem = paramsMap.get(LhParamCodeEnums.CLASS_SYSTEM.getCode());
        // 1) 从配置获取班制，此处假设返回 "2" 或 "3" 字符串 没有就默认两班制
        int shiftsPerDay = StringUtils.isNotEmpty(classSystem) ? Integer.valueOf(classSystem) : ShiftSystemEnum.SHIFT_SYSTEM_2.getCode();
        //拿到班次List
        List<ShiftTimeWindowDTO>  shiftTimeList = this.buildShiftTimeWindows(paramsMap,scheduleTime);
        //拿到第一班的起始时间
        ShiftTimeWindowDTO startShiftTimeWindowDTO = shiftTimeList.get(0);
        returnTimeWindow.setStartTime(startShiftTimeWindowDTO.getStartTime());
        //拿到最后一班次
        ShiftTimeWindowDTO endShiftTimeWindowDTO = shiftTimeList.get(shiftsPerDay - 1);
        returnTimeWindow.setEndTime(endShiftTimeWindowDTO.getEndTime());
        returnTimeWindow.setShiftName("当日");
        return returnTimeWindow;
    }

    /**
     * 获取当日班次
     * @param factoryCode
     * @param scheduleTime
     * @return
     */
    public List<ShiftTimeWindowDTO> getTodayShiftTime(String factoryCode, Date scheduleTime){
        Map<String, String> paramsMap = lhParamsService.listLhParams(factoryCode);
        List<ShiftTimeWindowDTO>  shiftTimeList = this.buildShiftTimeWindows(paramsMap,scheduleTime);
        List<ShiftTimeWindowDTO>  resultList = new ArrayList<>();
        //取系统配置参数_班制 如果不为空用参数覆盖
        String classSystem = paramsMap.get(LhParamCodeEnums.CLASS_SYSTEM.getCode());
        // 1) 从配置获取班制，此处假设返回 "2" 或 "3" 字符串 没有就默认两班制
        int shiftsPerDay = StringUtils.isNotEmpty(classSystem) ? Integer.valueOf(classSystem) : ShiftSystemEnum.SHIFT_SYSTEM_2.getCode();
        //根据班制来返回对应当日的班制
        if (ShiftSystemEnum.SHIFT_SYSTEM_2.getCode().equals(shiftsPerDay)) {
            resultList.add(shiftTimeList.get(0));
            resultList.add(shiftTimeList.get(1));
        } else if (ShiftSystemEnum.SHIFT_SYSTEM_3.getCode().equals(shiftsPerDay)) {
            resultList.add(shiftTimeList.get(0));
            resultList.add(shiftTimeList.get(1));
            resultList.add(shiftTimeList.get(2));
        }
        return resultList;
    }


    /**
     * 筛选机台
     *
     * @param lhScheduleResultVo          当前要筛选机台的规格
     * @param contextDTO                  上下文
     * @param lhScheduleResultFinalVoList 最终的流程排程结果
     */
    private void screenMachine(LhScheduleResultVo lhScheduleResultVo, AutoLhScheduleResultContextDTO contextDTO,
                               List<LhScheduleResultVo> lhScheduleResultFinalVoList) {
        if (PubUtil.isEmpty(lhScheduleResultVo.getAvailableLhMachineList())) {
            createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.screenMachine.empty2"), lhScheduleResultVo.getDailyPlanQty()),
                    contextDTO,null);
            return;
        }
        if (contextDTO.getHadSchedulePlanNum()>contextDTO.getLimitTotalPlanNum()) {
            //已排计划量>日排程总计划量限制 pancd+ 20250716
            createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.screenMachine.empty3"), lhScheduleResultVo.getDailyPlanQty(),contextDTO.getHadSchedulePlanNum()),
                    contextDTO,null);
            return;
        }
        //1. 获取可用机台列表最大的剩余产能
        Map<String,LhMachineInfoVo> returnMap = getMaxRemainCapacityMachine(lhScheduleResultVo, contextDTO,lhScheduleResultFinalVoList);
        if (returnMap.get(ApsConstant.FALSE) != null){
            //续作规格多出的任务(没有续作机台了)，纳入普通任务，直接返回
            return;
        }
        LhMachineInfoVo selectedLhMachineInfo = returnMap.get(ApsConstant.TRUE);
        contextDTO.getLogDetail().append(String.format("规格:%s,获取可用最大剩余产能的机台%s",lhScheduleResultVo.getSpecCode(),JSON.toJSONString(selectedLhMachineInfo))).append(ApsConstant.DIVISION);
        if (selectedLhMachineInfo == null) {
            createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.screenMachine.empty2"), lhScheduleResultVo.getDailyPlanQty()),
                    contextDTO,null);
            return;
        }

        //2. 检查是否有剩余模数，若有，减模数,拆任务
        if (!checkHasRemainMoldQty(lhScheduleResultVo, contextDTO)) {
            return;
        }

        //3. 根据选中的机台类型，设置硫化时间
        setLhTime(lhScheduleResultVo, selectedLhMachineInfo);
        lhScheduleResultVo.setLhMachineCode(selectedLhMachineInfo.getMachineCode());
        lhScheduleResultVo.setLhMachineName(selectedLhMachineInfo.getMachineName());
        lhScheduleResultVo.setMachineOrder(selectedLhMachineInfo.getMachineOrder());
        //4. 设置关联模具，标记共用模
        setLhMoldInfo(contextDTO,lhScheduleResultVo, selectedLhMachineInfo,lhScheduleResultFinalVoList,contextDTO.getLogDetail());
        if (StringUtils.isEmpty(lhScheduleResultVo.getMoldInfo())){
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,没有匹配到模具信息！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
            createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.moldInfo.empty"), selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty()),
                    contextDTO,null);
            deleteMachineMoldInfo(selectedLhMachineInfo,lhScheduleResultVo.getSpecCode());
            return;
        }
        //5. 设置单班产能
        setSingleMoldQty(lhScheduleResultVo, contextDTO);
        contextDTO.getLogDetail().append(String.format("规格:%s,计算的硫化时间:%s秒,单班产能:%s",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getLhTime(),lhScheduleResultVo.getSingleMoldShiftLhQty())).append(ApsConstant.DIVISION);

        //6. 机台剩余产能判断
        if (lhScheduleResultVo.getDailyPlanQty() <= selectedLhMachineInfo.getRemainCapacity()) {
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,日计划量:%s <= 机台剩余产能:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty(),selectedLhMachineInfo.getRemainCapacity())).append(ApsConstant.DIVISION);
            //6.1 若 T日排程量<=机台最大的剩余产能
            lhScheduleResultVo.setRemainMoldQty(0);
            //选中机台并按班次连载
            selectedMachineAndScheduleClass(lhScheduleResultVo, selectedLhMachineInfo, contextDTO, lhScheduleResultFinalVoList);
        } else {
            //6.2 若 T日排程量 > 机台最大的剩余产能
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,日计划量:%s > 机台剩余产能:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty(),selectedLhMachineInfo.getRemainCapacity())).append(ApsConstant.DIVISION);
            if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsLimit()) &&
                    ApsConstant.TRUE.equals(lhScheduleResultVo.getIsDelivery())){
                //6.2.1 若是限制任务且有交期,需要强制挤占
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,限制且有交期,开始强制挤占！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
                //计算 单日产能 = 单班产能*班数
                int singleDayQty = lhScheduleResultVo.getSingleMoldShiftLhQty() * contextDTO.getWorkShifts();
                int machineDayQuota = getMachineDayQuota(selectedLhMachineInfo,contextDTO.getWorkShifts());
                int realQuota = singleDayQty > machineDayQuota ? machineDayQuota:singleDayQty;
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,限制且有交期,计算出单日产能:%s,机台定额:%s,满排量:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),singleDayQty,machineDayQuota,realQuota)).append(ApsConstant.DIVISION);
                if (lhScheduleResultVo.getDailyPlanQty() <= realQuota){
                    //有交期的限制任务:若T日排程量<=满排量，则选中机台并按班次连载
                    contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,限制且有交期,日排程量:%s <= 满排量:%s,选中机台并班次连载！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty(),realQuota)).append(ApsConstant.DIVISION);
                    lhScheduleResultVo.setRemainMoldQty(0);
                    //根据多挤占的计划量，生成挤占任务。挤占量 = T日计划量-当前机台的剩余量
                    squeezeScheduleTask(lhScheduleResultVo,selectedLhMachineInfo,lhScheduleResultFinalVoList,contextDTO);
                    selectedMachineAndScheduleClass(lhScheduleResultVo, selectedLhMachineInfo, contextDTO, lhScheduleResultFinalVoList);
                }else {
                    //有交期的限制任务：若T日排程量 > 满排量,按满排量拆分任务，累减剩余模数
                    contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,限制且有交期,日排程量:%s > 满排量:%s,按满排量拆分任务,累减剩余模数！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty(),realQuota)).append(ApsConstant.DIVISION);
                    splitScheduleResultTask(lhScheduleResultVo, contextDTO, lhScheduleResultFinalVoList, selectedLhMachineInfo,
                            realQuota);
                }
            }else {
                //6.2.2 非 限制任务非交期的情况下，按机台剩余产能拆分，累减剩余模数
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,非限制或非交期,日排程量:%s > 机台剩余产能:%s,按机台剩余产能拆分任务,累减剩余模数！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty(),selectedLhMachineInfo.getRemainCapacity())).append(ApsConstant.DIVISION);
                splitScheduleResultTask(lhScheduleResultVo, contextDTO, lhScheduleResultFinalVoList, selectedLhMachineInfo,
                        selectedLhMachineInfo.getRemainCapacity());
            }
        }
    }

    /**
     * 获取最大的剩余产能机台
     * @param lhScheduleResultVo
     * @param contextDTO
     * @return
     */
    private Map<String,LhMachineInfoVo> getMaxRemainCapacityMachine(LhScheduleResultVo lhScheduleResultVo, AutoLhScheduleResultContextDTO contextDTO,List<LhScheduleResultVo> lhScheduleResultFinalVoList) {

        Map<String,LhMachineInfoVo> returnMap = new HashMap<>();
        LhMachineInfoVo selectedLhMachineInfo = null;
        List<LhMachineInfoVo> availableLhMachineList;
        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsContinue())){
            //有续作，优先从续作机台获取机台信息
            availableLhMachineList = lhScheduleResultVo.getAvailableLhMachineList().stream().filter(x->lhScheduleResultVo.getContinuedMachineList().indexOf(x.getMachineCode())>=0).collect(Collectors.toList());
            //有续作时，检查前日规格是否收尾,换模
            checkContinuedChangeMold(lhScheduleResultVo,availableLhMachineList,contextDTO,lhScheduleResultFinalVoList);
            availableLhMachineList = dealAvailableMachinesList(contextDTO,availableLhMachineList,lhScheduleResultVo,lhScheduleResultFinalVoList);
            selectedLhMachineInfo = selectOptimalMachine(contextDTO,lhScheduleResultVo,availableLhMachineList,lhScheduleResultFinalVoList);
            if (selectedLhMachineInfo != null){
                lhScheduleResultVo.setAvailableLhMachineList(availableLhMachineList);
                if (StringUtils.isEmpty(selectedLhMachineInfo.getOnLineMoldInfo())){
                    //若找到续作机台，但续作机台上的在线模具为空，则找到该机台上前规格信息
                    setMachineOnLineMoldInfoWithLhSchedule(lhScheduleResultVo, contextDTO, selectedLhMachineInfo);
                }
            }else{
                //若续作机台找不到，有可能今天机台没有了，则将续作标志置否，纳入普通任务
                contextDTO.getLogDetail().append(String.format("规格:%s,续作机台:%s不可用,将续作标志置否，重新计算可用机台的剩余产能-开始！",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getLhMachineCode())).append(ApsConstant.DIVISION);
                lhScheduleResultVo.setIsContinue(ApsConstant.FALSE);
                contextDTO.getRemainingScheduleList().add(lhScheduleResultVo);
                returnMap.put(ApsConstant.FALSE,new LhMachineInfoVo());
                return returnMap;
                //从备选机台中处理可用机台
                /*availableLhMachineList = dealAvailableMachinesList(contextDTO,lhScheduleResultVo.getCopyAvailableLhMachineList().stream().filter(x->lhScheduleResultVo.getContinuedMachineList().indexOf(x.getMachineCode())<0).collect(Collectors.toList()), lhScheduleResultVo,lhScheduleResultFinalVoList);
                lhScheduleResultVo.setAvailableLhMachineList(availableLhMachineList);
                contextDTO.getLogDetail().append(String.format("规格:%s,续作机台:%s不可用,将续作标志置否，重新计算可用机台的剩余产能-结束！",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getLhMachineCode())).append(ApsConstant.DIVISION);*/
            }
        }

        if (selectedLhMachineInfo == null){
            //若存在相同生胎，优选相应机台
            LhScheduleResultVo scheduleResultVo = lhScheduleResultFinalVoList.stream()
                    .filter(x->x.getEmbryoCode().equals(lhScheduleResultVo.getEmbryoCode())).findFirst().orElse(null);
            if (scheduleResultVo != null){
                availableLhMachineList = lhScheduleResultVo.getAvailableLhMachineList().stream().filter(x->x.getMachineCode().equals(scheduleResultVo.getLhMachineCode())).collect(Collectors.toList());
                selectedLhMachineInfo = selectOptimalMachine(contextDTO,lhScheduleResultVo,availableLhMachineList,lhScheduleResultFinalVoList);
            }
        }

        if (selectedLhMachineInfo == null){
            selectedLhMachineInfo = selectOptimalMachine(contextDTO,lhScheduleResultVo,lhScheduleResultVo.getAvailableLhMachineList(),lhScheduleResultFinalVoList);
        }
        returnMap.put(ApsConstant.TRUE,selectedLhMachineInfo);
        return returnMap;
    }

    /**
     * 检查续作规格，前日存在收尾换模
     * @param lhScheduleResultVo
     * @param availableLhMachineList
     * @param contextDTO
     */
    private void checkContinuedChangeMold(LhScheduleResultVo lhScheduleResultVo,List<LhMachineInfoVo> availableLhMachineList,AutoLhScheduleResultContextDTO contextDTO,List<LhScheduleResultVo> lhScheduleResultFinalVoList){
        //T日所有规格列表
        List<String> tDayAllSpecCodeList = contextDTO.getTDayAllSpecCodeList();
        Map<String, List<LhScheduleResultVo>> lastMachineScheduledMap = contextDTO.getLastMachineScheduledMap();
        List<LhScheduleResultVo> lastScheduleResultVoList;
        Date specEndTime;
        for (LhMachineInfoVo machineInfoVo:availableLhMachineList){
            lastScheduleResultVoList =  lastMachineScheduledMap.get(machineInfoVo.getMachineCode());
            if (PubUtil.isEmpty(lastScheduleResultVoList)){
                continue;
            }
            LhScheduleResultVo lastScheduleResultVo = lastScheduleResultVoList.stream().filter(x->x.getSpecCode().equals(lhScheduleResultVo.getSpecCode())).sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
            //优先获取 自身规格
            if (lastScheduleResultVo == null){
                lastScheduleResultVo = lastScheduleResultVoList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (lastScheduleResultVo == null){
                    continue;
                }
            }
            if (StringUtils.isEmpty(lastScheduleResultVo.getLeftRightMold())){
                //双模排产,继续
                continue;
            }

            //机台非双模排产
            boolean isLChange = false;
            boolean isRChange = false;
            boolean isEnd = false;
            boolean isMachineEnd = false;
            int hasSchedulePlanQty = 0;
            int dailyPlanQty = 0;
            //2.1 机台是否L模排产
            if (ApsConstant.L_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                if (!lastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                    isLChange = true;
                    isEnd = tDayAllSpecCodeList.indexOf(lastScheduleResultVo.getSpecCode())<0;
                }
                LhScheduleResultVo rlastScheduleResultVo = lastScheduleResultVoList.stream()
                        .filter(x->ApsConstant.R_MOLD.equals(x.getLeftRightMold()))
                        .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (rlastScheduleResultVo != null) {
                    //汇总不同于当前机台的规格已排计划量 pancd+ 20250609
                    //解决某规格已排的机台计划量已完全覆盖 今日需求，那么当前机台的规格，可认为收尾
                    hasSchedulePlanQty = getHasSchedulePlanQty(contextDTO, lhScheduleResultFinalVoList, machineInfoVo.getMachineCode(),rlastScheduleResultVo.getSpecCode());
                    dailyPlanQty = contextDTO.getTDayScheduleList().stream().filter(x->x.getSpecCode().equals(rlastScheduleResultVo.getSpecCode())).mapToInt(e ->
                            e.getDailyPlanQty() != null ? e.getDailyPlanQty() : 0)
                            .sum();
                    isMachineEnd = dailyPlanQty<hasSchedulePlanQty;
                    if (!rlastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                        isRChange = true;
                        isEnd = tDayAllSpecCodeList.indexOf(rlastScheduleResultVo.getSpecCode())<0;
                    }
                }
                if ((isLChange || isRChange) && (isEnd||isMachineEnd)){
                    //续作收尾换模，只要有1个换即可，故用或关系
                    specEndTime = doDefaultSpecEndTime(lastScheduleResultVo.getSpecEndTime() != null && rlastScheduleResultVo.getSpecEndTime() != null && lastScheduleResultVo.getSpecEndTime().compareTo(rlastScheduleResultVo.getSpecEndTime())>=0 ? lastScheduleResultVo.getSpecEndTime():rlastScheduleResultVo.getSpecEndTime(),lhScheduleResultVo);
                    machineInfoVo.setContinueChangeMoldDate(specEndTime);
                }
            }else if (ApsConstant.R_MOLD.equals(lastScheduleResultVo.getLeftRightMold())){
                //2.2 机台是否R模排产
                if (!lastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                    isRChange = true;
                    isEnd = tDayAllSpecCodeList.indexOf(lastScheduleResultVo.getSpecCode())<0;
                }
                LhScheduleResultVo llastScheduleResultVo = lastScheduleResultVoList.stream()
                        .filter(x->ApsConstant.L_MOLD.equals(x.getLeftRightMold()))
                        .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                if (llastScheduleResultVo != null) {
                    //汇总不同于当前机台的规格已排计划量 pancd+ 20250609
                    //解决某规格已排的机台计划量已完全覆盖 今日需求，那么当前机台的规格，可认为收尾
                    hasSchedulePlanQty = getHasSchedulePlanQty(contextDTO, lhScheduleResultFinalVoList, machineInfoVo.getMachineCode(),llastScheduleResultVo.getSpecCode());
                    dailyPlanQty = contextDTO.getTDayScheduleList().stream().filter(x->x.getSpecCode().equals(llastScheduleResultVo.getSpecCode())).mapToInt(e ->
                            e.getDailyPlanQty() != null ? e.getDailyPlanQty() : 0)
                            .sum();
                    isMachineEnd = dailyPlanQty<hasSchedulePlanQty;
                    if (!llastScheduleResultVo.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                        isLChange = true;
                        isEnd = tDayAllSpecCodeList.indexOf(llastScheduleResultVo.getSpecCode())<0;
                    }
                }

                if ((isLChange || isRChange) && (isEnd||isMachineEnd)){
                    specEndTime = doDefaultSpecEndTime(llastScheduleResultVo.getSpecEndTime() != null && lastScheduleResultVo.getSpecEndTime() != null && llastScheduleResultVo.getSpecEndTime().compareTo(lastScheduleResultVo.getSpecEndTime())>=0 ? llastScheduleResultVo.getSpecEndTime():lastScheduleResultVo.getSpecEndTime(),lhScheduleResultVo);
                    machineInfoVo.setContinueChangeMoldDate(specEndTime);
                }
            }
        }
    }

    /**
     * 获取规格已排计划量
     * @param contextDTO
     * @param lhScheduleResultFinalVoList
     * @param curMachineCode
     * @param curSpecCode
     * @return
     */
    private Integer getHasSchedulePlanQty(AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> lhScheduleResultFinalVoList,
                                          String curMachineCode,String curSpecCode) {
        int hasSchedulePlanQty;
        if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
            hasSchedulePlanQty = lhScheduleResultFinalVoList.stream().filter(x->(x.getSpecCode().equals(curSpecCode)) && (!x.getLhMachineCode().equals(curMachineCode))).mapToInt(e ->
                    (e.getClass1PlanQty() != null ? e.getClass1PlanQty() : 0)+
                            (e.getClass2PlanQty() != null ? e.getClass2PlanQty() : 0)+
                            (e.getClass3PlanQty() != null ? e.getClass3PlanQty() : 0))
                    .sum();
        }else{
            hasSchedulePlanQty = lhScheduleResultFinalVoList.stream().filter(x->(x.getSpecCode().equals(curSpecCode)) && (!x.getLhMachineCode().equals(curMachineCode))).mapToInt(e ->
                    (e.getClass4PlanQty() != null ? e.getClass4PlanQty() : 0)+
                            (e.getClass5PlanQty() != null ? e.getClass5PlanQty() : 0)+
                            (e.getClass6PlanQty() != null ? e.getClass6PlanQty() : 0))
                    .sum();
        }
        return hasSchedulePlanQty;
    }

    /**
     * 获取最优的机台，剩余计划量刚好满足的
     * @param lhScheduleResultVo
     * @param machineInfoVoList
     * @return
     */
    private LhMachineInfoVo selectOptimalMachine(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo lhScheduleResultVo,List<LhMachineInfoVo> machineInfoVoList,List<LhScheduleResultVo> lhScheduleResultFinalVoList){
        if (PubUtil.isEmpty(machineInfoVoList)){
            return null;
        }
        machineInfoVoList.sort(Comparator.comparingLong(LhMachineInfoVo::getRemainTime)
                .thenComparing(LhMachineInfoVo::getMachineOrder));
        for (LhMachineInfoVo machineInfoVo:machineInfoVoList){
            //计算机台剩余产能
            calcOneMachineRemainCapacity(contextDTO,lhScheduleResultVo,lhScheduleResultFinalVoList,machineInfoVo,false);

            if (lhScheduleResultVo.getDailyPlanQty() <= machineInfoVo.getRemainCapacity()){
                return machineInfoVo;
            }
        }
        return machineInfoVoList.get(machineInfoVoList.size()-1);
    }
    /**
     * 从在机规格中，获取机台在线模具信息
     * @param lhScheduleResultVo
     * @param contextDTO
     * @param selectedLhMachineInfo
     */
    private void setMachineOnLineMoldInfoWithLhSchedule(LhScheduleResultVo lhScheduleResultVo, AutoLhScheduleResultContextDTO contextDTO, LhMachineInfoVo selectedLhMachineInfo) {
        String selMachineCode = selectedLhMachineInfo.getMachineCode();
        //1. 判断最后规格是否是双模（按规格最后时间大->小排序）
        LhScheduleResultVo lastScheduleResultVo = contextDTO.getLastDayScheduleList().stream()
                .filter(x->x.getSpecCode().equals(lhScheduleResultVo.getSpecCode()) && x.getLhMachineCode().equals(selMachineCode) && StringUtils.isEmpty(x.getLeftRightMold()))
                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        if (lastScheduleResultVo != null){
            selectedLhMachineInfo.setOnLineMoldInfo(lastScheduleResultVo.getMoldInfo());
            return;
        }
        //2. 判断最后规格是否L模
        List<LhMoldInfoVo> onLineMoldInfoList = new ArrayList<>();
        LhScheduleResultVo lMoldLastScheduleResultVo = contextDTO.getLastDayScheduleList().stream()
                .filter(x->x.getSpecCode().equals(lhScheduleResultVo.getSpecCode()) && x.getLhMachineCode().equals(selMachineCode) && StringUtils.isNotEmpty(x.getLeftRightMold()) && x.getLeftRightMold().contains(ApsConstant.L_MOLD) )
                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        if (lMoldLastScheduleResultVo != null && StringUtils.isNotEmpty(lMoldLastScheduleResultVo.getMoldInfo())){
            onLineMoldInfoList.addAll(JSON.parseObject(lMoldLastScheduleResultVo.getMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {}));
        }

        //3. 判断最后规格是否R模
        LhScheduleResultVo rMoldLastScheduleResultVo = contextDTO.getLastDayScheduleList().stream()
                .filter(x->x.getSpecCode().equals(lhScheduleResultVo.getSpecCode()) && x.getLhMachineCode().equals(selMachineCode) && StringUtils.isNotEmpty(x.getLeftRightMold()) && x.getLeftRightMold().contains(ApsConstant.R_MOLD) )
                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        if (rMoldLastScheduleResultVo != null && StringUtils.isNotEmpty(rMoldLastScheduleResultVo.getMoldInfo())){
            onLineMoldInfoList.addAll(JSON.parseObject(rMoldLastScheduleResultVo.getMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {}));
        }
        //4. 将L/R模的模具信息合并
        if (PubUtil.isNotEmpty(onLineMoldInfoList)){
            selectedLhMachineInfo.setOnLineMoldInfo(JSON.toJSONString(onLineMoldInfoList));
        }
    }

    /**
     * 移除不是有效模具的机台
     * @param lhScheduleResultVo
     */
    /*private void removeUnValidMachine(LhScheduleResultVo lhScheduleResultVo) {
        List<String> availMoldNoList = lhScheduleResultVo.getAvailLhMoldInfoVoList().stream().map(x->x.getMoldNo()).distinct().collect(Collectors.toList());
        if (PubUtil.isEmpty(availMoldNoList)){
            return;
        }

        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsLimit()) &&
                ApsConstant.TRUE.equals(lhScheduleResultVo.getIsDelivery())){
            //若是限制且有交期，直接退出，因为需要强占
            return;
        }

        List<LhMachineInfoVo> removeMachineList = new ArrayList<>();
        for (LhMachineInfoVo machineInfoVo: lhScheduleResultVo.getAvailableLhMachineList()){
            if (StringUtils.isEmpty(machineInfoVo.getOnLineMoldInfo())){
                continue;
            }
            if (!checkValidMold(availMoldNoList, machineInfoVo)){
                removeMachineList.add(machineInfoVo);
            }
        }

        if (PubUtil.isNotEmpty(removeMachineList)){
            for (LhMachineInfoVo machineInfoVo1: removeMachineList){
                lhScheduleResultVo.getAvailableLhMachineList().remove(machineInfoVo1);
            }
        }

    }*/

    /**
     * 检查有效模具
     * @param availMoldNoList
     * @param machineInfoVo
     * @return
     */
    private boolean checkValidMold(List<String> availMoldNoList, LhMachineInfoVo machineInfoVo) {
        boolean isExist = true;
        List<LhMoldInfoVo> onLineMoldInfoList = JSON.parseObject(machineInfoVo.getOnLineMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {});
        for (LhMoldInfoVo moldInfoVo:onLineMoldInfoList){
            if (availMoldNoList.indexOf(moldInfoVo.getMoldNo())<0){
                isExist = false;
                break;
            }
        }
        return isExist;
    }

    /**
     * 设置单班单模产能
     * @param lhScheduleResultVo 硫化排程
     * @param contextDTO 上下文
     */
    private void setSingleMoldQty(LhScheduleResultVo lhScheduleResultVo, AutoLhScheduleResultContextDTO contextDTO) {
        Date classStartTime = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? lhScheduleResultVo.getClass1StartTime(): lhScheduleResultVo.getClass4StartTime();
        Date classEndTime =  ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? lhScheduleResultVo.getClass1EndTime(): lhScheduleResultVo.getClass4EndTime();
        long diffMillTimes = DateUtils.getDiffMillTime(classStartTime, classEndTime);
        //硫化时长=单胎硫化时长+刷囊时间
        //Double realLhTime = lhScheduleResultVo.getLhTime().doubleValue() + contextDTO.getBrushBagTime();
        //BigDecimal singleMoldShiftLhQty = BigDecimal.valueOf((diffMillTimes / 1000) / realLhTime).setScale(2, BigDecimal.ROUND_DOWN);
        // 扣减辅助时间,1个班
        diffMillTimes = diffMillTimes/1000 - contextDTO.getBrushBagTime();
        int singleMoldShiftLhQty = CommonUtils.calcPeriodCapacity(BigDecimal.valueOf(diffMillTimes),lhScheduleResultVo.getLhTime(),lhScheduleResultVo.getMoldQty());
        lhScheduleResultVo.setSingleMoldShiftLhQty(singleMoldShiftLhQty);
    }

    /**
     * 获取机台日定额
     * @param selectedLhMachineInfo
     * @param workShifts
     * @return
     */
    private Integer getMachineDayQuota(LhMachineInfoVo selectedLhMachineInfo,Integer workShifts){
        if (ShiftSystemEnum.SHIFT_SYSTEM_2.getCode().equals(workShifts)){
            //2班制
            return selectedLhMachineInfo.getClass1Quota() + selectedLhMachineInfo.getClass2Quota();
        }else {
            //3班制
            return selectedLhMachineInfo.getClass1Quota() + selectedLhMachineInfo.getClass2Quota() + + selectedLhMachineInfo.getClass3Quota();
        }
    }

    /**
     * 获取机台日维修时长
     * @param selectedLhMachineInfo
     * @param workShifts
     * @return
     */
    private Integer getMachineDayMaintainTime(LhMachineInfoVo selectedLhMachineInfo,Integer workShifts){
        if (ShiftSystemEnum.SHIFT_SYSTEM_2.getCode().equals(workShifts)){
            //2班制
            return (selectedLhMachineInfo.getClass1MaintainTime() != null ? selectedLhMachineInfo.getClass1MaintainTime() : 0) + (selectedLhMachineInfo.getClass2MaintainTime() != null ? selectedLhMachineInfo.getClass2MaintainTime() : 0);
        }else {
            //3班制
            return (selectedLhMachineInfo.getClass1MaintainTime() != null ? selectedLhMachineInfo.getClass1MaintainTime() : 0)+ (selectedLhMachineInfo.getClass2MaintainTime() != null ? selectedLhMachineInfo.getClass2MaintainTime() : 0) + (selectedLhMachineInfo.getClass3MaintainTime() != null ? selectedLhMachineInfo.getClass3MaintainTime():0);
        }
    }

    /**
     * 挤占排程任务
     * @param lhScheduleResultVo 当前硫化排程
     * @param selectedLhMachineInfo 当前选中机台
     * @param lhScheduleResultFinalVoList 已排硫化排程
     * @param contextDTO 上下文
     */
    private void squeezeScheduleTask(LhScheduleResultVo lhScheduleResultVo,
                                     LhMachineInfoVo selectedLhMachineInfo,
                                     List<LhScheduleResultVo> lhScheduleResultFinalVoList,
                                     AutoLhScheduleResultContextDTO contextDTO){
        if (PubUtil.isEmpty(lhScheduleResultFinalVoList)){
            return;
        }
        //注：仅支持往前推1个规格
        //1. 根据当前规格需要多挤占的时间
        //需要多挤占的计划量，T日计划量-当前机台的剩余量
        int selMachineRemainCapacity = selectedLhMachineInfo.getRemainCapacity() <0 ? 0:selectedLhMachineInfo.getRemainCapacity();
        int currSqueezeQty = lhScheduleResultVo.getDailyPlanQty() - selMachineRemainCapacity;
        contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,限制且有交期,计算出需要多挤占的计划量(T日计划量-当前机台的剩余量):%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),currSqueezeQty)).append(ApsConstant.DIVISION);
        if (currSqueezeQty <=0){
            return;
        }
        //当前规格硫化时长=单胎硫化时长+刷囊时间
        //Double realLhTime = lhScheduleResultVo.getLhTime().doubleValue() + contextDTO.getBrushBagTime();
        //当前规格多挤占的所需时间 = 填充的排程量/机台模数 *硫化时长
        Double curMoreSqueezeTime = (currSqueezeQty/lhScheduleResultVo.getMoldQty()) * lhScheduleResultVo.getLhTime().doubleValue();
        contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,限制且有交期,计算出需要多挤占计划量所需要的时间:%s秒！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),curMoreSqueezeTime)).append(ApsConstant.DIVISION);
        //2. 根据机台，过滤已排硫化排程结果
        List<LhScheduleResultVo> machineScheduledResutlList = lhScheduleResultFinalVoList.stream().filter(x -> selectedLhMachineInfo.getMachineCode().equals(x.getLhMachineCode())).collect(Collectors.toList());
        if (PubUtil.isEmpty(machineScheduledResutlList)) {
            return;
        }

        //3. 获取前规格，即最大的规格结束时间
        LhScheduleResultVo preScheduledResultVo = machineScheduledResutlList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        //先将前规格从已排列表移除
        lhScheduleResultFinalVoList.remove(preScheduledResultVo);
        //4. 计算前规格需要减少的计划量
        //Double preLhTime = preScheduledResultVo.getLhTime().doubleValue() + contextDTO.getBrushBagTime();
        Double preLhTime = preScheduledResultVo.getLhTime().doubleValue();
        BigDecimal moldNum = BigDecimal.valueOf(preScheduledResultVo.getMoldQty());
        BigDecimal reduceLhQty = BigDecimal.valueOf(curMoreSqueezeTime / preLhTime).setScale(2, BigDecimal.ROUND_DOWN).multiply(moldNum);
        contextDTO.getLogDetail().append(String.format("前规格:%s,机台:%s,计算出需要减少的计划量:%s,开始生成普通任务！",preScheduledResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),reduceLhQty)).append(ApsConstant.DIVISION);
        //5. 生成挤占任务，并纳入普通任务
        createCrowdOutTask(selectedLhMachineInfo, contextDTO, preScheduledResultVo, reduceLhQty);

        //6. 还原月度剩余
        int remainMpQty = contextDTO.getRemainMpQtyMap().get(preScheduledResultVo.getSpecCode()) != null ? contextDTO.getRemainMpQtyMap().get(preScheduledResultVo.getSpecCode()) : 0;
        contextDTO.getRemainMpQtyMap().put(preScheduledResultVo.getSpecCode(),remainMpQty + preScheduledResultVo.getDailyPlanQty());
        contextDTO.getLogDetail().append(String.format("前规格:%s,按前规格的日计划量还原月度剩余量,原月度剩余量:%s,还原后的月度剩余量:%s！",preScheduledResultVo.getSpecCode(),remainMpQty,contextDTO.getRemainMpQtyMap().get(preScheduledResultVo.getSpecCode()))).append(ApsConstant.DIVISION);
        //将前规格的结束时间作为本规格的前规格结束时间
        lhScheduleResultVo.setSpecEndTime(preScheduledResultVo.getSpecEndTime());
        //7. 重排前规格(减去 被挤占的计划量)，按班次连载
        int preScheduledDailyPlanQty = preScheduledResultVo.getDailyPlanQty() - reduceLhQty.intValue();
        if (preScheduledDailyPlanQty >0){
            preScheduledResultVo.setDailyPlanQty(preScheduledDailyPlanQty);
            preScheduledResultVo.setClass1PlanQty(null);
            preScheduledResultVo.setClass2PlanQty(null);
            preScheduledResultVo.setClass3PlanQty(null);
            preScheduledResultVo.setClass4PlanQty(null);
            preScheduledResultVo.setClass5PlanQty(null);
            preScheduledResultVo.setClass6PlanQty(null);
            contextDTO.getLogDetail().append(String.format("前规格:%s,机台:%s,重排前规格,开始按班次连载！",preScheduledResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
            boolean bSuccess = realScheduleClass(preScheduledResultVo.getDailyPlanQty(), preScheduledResultVo, selectedLhMachineInfo,contextDTO, lhScheduleResultFinalVoList);
            //将前规格再加回到已排列表
            if (bSuccess){
                lhScheduleResultFinalVoList.add(preScheduledResultVo);
                //增加已排计划量
                addHadSchedulePlanNum(contextDTO,preScheduledResultVo,true);
            }
        }
    }

    /**
     * 增加已排计划量
     * @param contextDTO
     * @param lhScheduleResultVo
     */
    private void addHadSchedulePlanNum(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo lhScheduleResultVo,boolean isAdd){
        int iTotalPlanQty = 0;
        if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
            //T日
            iTotalPlanQty = lhScheduleResultVo.getClass1PlanQty() == null ? 0:lhScheduleResultVo.getClass1PlanQty();
            iTotalPlanQty += lhScheduleResultVo.getClass2PlanQty() == null ? 0:lhScheduleResultVo.getClass2PlanQty();
            iTotalPlanQty += lhScheduleResultVo.getClass3PlanQty() == null ? 0:lhScheduleResultVo.getClass3PlanQty();
        }else{
            //T+1日
            iTotalPlanQty = lhScheduleResultVo.getClass4PlanQty() == null ? 0:lhScheduleResultVo.getClass4PlanQty();
            iTotalPlanQty += lhScheduleResultVo.getClass5PlanQty() == null ? 0:lhScheduleResultVo.getClass5PlanQty();
            iTotalPlanQty += lhScheduleResultVo.getClass6PlanQty() == null ? 0:lhScheduleResultVo.getClass6PlanQty();
        }
        if (isAdd){
            contextDTO.setHadSchedulePlanNum(contextDTO.getHadSchedulePlanNum() + iTotalPlanQty);
        }else{
            contextDTO.setHadSchedulePlanNum(contextDTO.getHadSchedulePlanNum() - iTotalPlanQty);
        }

    }

    /**
     * 创建挤占任务
     * @param selectedLhMachineInfo
     * @param contextDTO
     * @param preScheduledResultVo
     * @param reduceLhQty
     */
    private void createCrowdOutTask(LhMachineInfoVo selectedLhMachineInfo, AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo preScheduledResultVo, BigDecimal reduceLhQty) {
        LhScheduleResultVo newLhScheduleResultVo = new LhScheduleResultVo();
        BeanUtils.copyProperties(preScheduledResultVo, newLhScheduleResultVo);
        newLhScheduleResultVo.setDailyPlanQty(reduceLhQty.intValue());
        newLhScheduleResultVo.setLhMachineCode(null);
        newLhScheduleResultVo.setLhMachineName(null);
        newLhScheduleResultVo.setMachineOrder(null);
        List<LhMachineInfoVo> availLhMachineInfoVoList = preScheduledResultVo.getAvailableLhMachineList().stream().filter(x->!x.getMachineCode().equals(selectedLhMachineInfo.getMachineCode())).collect(Collectors.toList());
        newLhScheduleResultVo.setAvailableLhMachineList(availLhMachineInfoVoList);
        newLhScheduleResultVo.setRemainMoldQty(newLhScheduleResultVo.getMpMoldQty() - preScheduledResultVo.getMoldQty());
        newLhScheduleResultVo.setMoldInfo(null);
        newLhScheduleResultVo.setClass1PlanQty(null);
        newLhScheduleResultVo.setClass2PlanQty(null);
        newLhScheduleResultVo.setClass3PlanQty(null);
        newLhScheduleResultVo.setClass4PlanQty(null);
        newLhScheduleResultVo.setClass5PlanQty(null);
        newLhScheduleResultVo.setClass6PlanQty(null);
        contextDTO.getRemainingScheduleList().add(newLhScheduleResultVo);
        contextDTO.getLogDetail().append(String.format("前规格:%s,拆出的硫化排程任务信息:%s！",preScheduledResultVo.getSpecCode(),JSON.toJSONString(newLhScheduleResultVo))).append(ApsConstant.DIVISION);
    }

    /**
     * 拆分排程结果任务
     * @param lhScheduleResultVo 当前硫化排程
     * @param contextDTO 上下文
     * @param lhScheduleResultFinalVoList 已排硫化排程
     * @param selectedLhMachineInfo 选中的机台
     * @param splitQty 拆分的量
     */
    private void splitScheduleResultTask(LhScheduleResultVo lhScheduleResultVo,
                                         AutoLhScheduleResultContextDTO contextDTO,
                                         List<LhScheduleResultVo> lhScheduleResultFinalVoList,
                                         LhMachineInfoVo selectedLhMachineInfo, int splitQty) {
        //将规格任务进行拆分，累减剩余模数
        LhScheduleResultVo newLhScheduleResultVo = new LhScheduleResultVo();
        BeanUtils.copyProperties(lhScheduleResultVo, newLhScheduleResultVo);
        newLhScheduleResultVo.setRemainMoldQty(0);
        newLhScheduleResultVo.setDailyPlanQty(splitQty);
        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsLimit()) &&
                ApsConstant.TRUE.equals(lhScheduleResultVo.getIsDelivery())){
            //根据多挤占的计划量，生成挤占任务。挤占量 = T日计划量-当前机台的剩余量
            squeezeScheduleTask(newLhScheduleResultVo,selectedLhMachineInfo,lhScheduleResultFinalVoList,contextDTO);
        }

        //选中机台并按班次连载
        selectedMachineAndScheduleClass(newLhScheduleResultVo, selectedLhMachineInfo, contextDTO, lhScheduleResultFinalVoList);

        //2.3 原来的规格任务 排程量及模数 累减
        lhScheduleResultVo.setDailyPlanQty(lhScheduleResultVo.getDailyPlanQty() - splitQty);
        lhScheduleResultVo.setRemainMoldQty(lhScheduleResultVo.getRemainMoldQty() - newLhScheduleResultVo.getMoldQty());
        List<LhMachineInfoVo> availLhMachineInfoVoList = lhScheduleResultVo.getAvailableLhMachineList().stream().filter(x->!x.getMachineCode().equals(selectedLhMachineInfo.getMachineCode())).collect(Collectors.toList());
        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsContinue())){
             if (PubUtil.isEmpty(availLhMachineInfoVoList)){
                 //若是续作规格，且有效续作机台为空，应续作作备选机台中获取有效机台
                 lhScheduleResultVo.setIsContinue(ApsConstant.FALSE);
                 //availLhMachineInfoVoList = lhScheduleResultVo.getCopyAvailableLhMachineList().stream().filter(x->lhScheduleResultVo.getContinuedMachineList().indexOf(x.getMachineCode())<0).collect(Collectors.toList());
                 //availLhMachineInfoVoList = dealAvailableMachinesList(contextDTO,availLhMachineInfoVoList,lhScheduleResultVo,lhScheduleResultFinalVoList);
                 lhScheduleResultVo.setAvailableLhMachineList(availLhMachineInfoVoList);
                 lhScheduleResultVo.setLhMachineCode(null);
                 lhScheduleResultVo.setLhMachineName(null);
                 lhScheduleResultVo.setMachineOrder(null);
                 lhScheduleResultVo.setMoldInfo(null);
                 lhScheduleResultVo.setMoldQty(null);
                 lhScheduleResultVo.setLeftRightMold(null);
                 //若是没有续作机台，将其纳入普通机台
                 contextDTO.getRemainingScheduleList().add(lhScheduleResultVo);
                 return;
             }
        }else{
            //重新计算可用机台的剩余时间
            //续作规格的计算统一放在getMaxRemainCapcityMachine()方法中
            availLhMachineInfoVoList = dealAvailableMachinesList(contextDTO,availLhMachineInfoVoList,lhScheduleResultVo,lhScheduleResultFinalVoList);
        }
        lhScheduleResultVo.setAvailableLhMachineList(availLhMachineInfoVoList);
        lhScheduleResultVo.setLhMachineCode(null);
        lhScheduleResultVo.setLhMachineName(null);
        lhScheduleResultVo.setMachineOrder(null);
        lhScheduleResultVo.setMoldInfo(null);
        lhScheduleResultVo.setMoldQty(null);
        lhScheduleResultVo.setLeftRightMold(null);
        screenMachine(lhScheduleResultVo, contextDTO, lhScheduleResultFinalVoList);
    }

    /**
     * 机台补量
     *
     * @param contextDTO                  上下文
     * @param lhScheduleResultFinalVoList 已排
     */
    public void machineReplenishment(AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> lhScheduleResultFinalVoList) {
        if (PubUtil.isEmpty(lhScheduleResultFinalVoList)) {
            return;
        }
        contextDTO.getLogDetail().append("==================机台补量开始！==================").append(ApsConstant.DIVISION);
        //按机台分组，获取最后规格，并补量
        Map<String, List<LhScheduleResultVo>> machineScheduledMap = lhScheduleResultFinalVoList.stream().collect(Collectors.groupingBy(item->item.getLhMachineCode()));
        for (Map.Entry<String, List<LhScheduleResultVo>> entry : machineScheduledMap.entrySet()) {
            //1. 获取最后规格，即最大的规格结束时间
            LhScheduleResultVo scheduleResultVo = entry.getValue().stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
            if (ApsConstant.TRUE.equals(scheduleResultVo.getIsTrial())){
                //若是试产试制，则不进行补量 pancd+20250510
                continue;
            }
            if (ApsConstant.TRUE.equals(scheduleResultVo.getIsFirst())){
                //若是首排，则不进行补量 pancd+20250625
                continue;
            }
            if (contextDTO.getHadSchedulePlanNum()>contextDTO.getLimitTotalPlanNum()) {
                //若已排计划量>日排程总计划量 pancd+20250716
                continue;
            }

            //先减少 计划量，后面再加 pancd+20250716
            entry.getValue().forEach(x->{
                addHadSchedulePlanNum(contextDTO,x,false);
            });

            if (StringUtils.isEmpty(scheduleResultVo.getLeftRightMold())){
                //双模排产
                machineOneSideReplenishment(contextDTO, lhScheduleResultFinalVoList, entry.getKey(), scheduleResultVo);
            }else{
                if (ApsConstant.L_MOLD.equals(scheduleResultVo.getLeftRightMold())){
                    //L模补量
                    machineOneSideReplenishment(contextDTO, lhScheduleResultFinalVoList, entry.getKey(), scheduleResultVo);
                    LhScheduleResultVo rlastScheduleResultVo = entry.getValue().stream()
                            .filter(x -> ApsConstant.R_MOLD.equals(x.getLeftRightMold()))
                            .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                    if (rlastScheduleResultVo != null) {
                        //若存在R模，也补量
                        machineOneSideReplenishment(contextDTO, lhScheduleResultFinalVoList, entry.getKey(), rlastScheduleResultVo);
                    }

                }else if (ApsConstant.R_MOLD.equals(scheduleResultVo.getLeftRightMold())){
                    //R模补量
                    machineOneSideReplenishment(contextDTO, lhScheduleResultFinalVoList, entry.getKey(), scheduleResultVo);
                    LhScheduleResultVo llastScheduleResultVo = entry.getValue().stream()
                            .filter(x -> ApsConstant.L_MOLD.equals(x.getLeftRightMold()))
                            .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
                    if (llastScheduleResultVo != null) {
                        //若存在L模，也补量
                        machineOneSideReplenishment(contextDTO, lhScheduleResultFinalVoList, entry.getKey(), llastScheduleResultVo);
                    }
                }
            }
            // 左右模平衡
            doLeftRightMoldPlanBalance(contextDTO,entry.getValue());

            //增加 计划量
            entry.getValue().forEach(x->{
                addHadSchedulePlanNum(contextDTO,x,true);
            });

        }
        contextDTO.getLogDetail().append("==================机台补量结束！==================").append(ApsConstant.DIVISION);
    }

    /**
     *  进行左右模平衡
     * @param contextDTO
     * @param scheduleResultVoList
     */
    private void doLeftRightMoldPlanBalance(AutoLhScheduleResultContextDTO contextDTO,List<LhScheduleResultVo> scheduleResultVoList){
        if (PubUtil.isEmpty(scheduleResultVoList)){
            return;
        }
        //1. 分别过滤出左右模列表,按规格结束时间排序
        List<LhScheduleResultVo> llastScheduleResultVoList = scheduleResultVoList.stream()
                .filter(x -> ApsConstant.L_MOLD.equals(x.getLeftRightMold()))
                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());

        List<LhScheduleResultVo> rlastScheduleResultVoList = scheduleResultVoList.stream()
                .filter(x -> ApsConstant.R_MOLD.equals(x.getLeftRightMold()))
                .sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());

        if (PubUtil.isEmpty(llastScheduleResultVoList) || PubUtil.isEmpty(rlastScheduleResultVoList)){
            return;
        }
        //2. 进行左右模平衡
        LhScheduleResultVo llastScheduleResultVo,rlastScheduleResultVo;
        int iSize = llastScheduleResultVoList.size() > rlastScheduleResultVoList.size() ? rlastScheduleResultVoList.size():llastScheduleResultVoList.size();
        for (int i = 0; i<iSize; i++){
            llastScheduleResultVo = llastScheduleResultVoList.get(i);
            rlastScheduleResultVo = rlastScheduleResultVoList.get(i);
            // 左右模平衡
            leftRightMoldPlanBalance(contextDTO,llastScheduleResultVo,rlastScheduleResultVo);
        }
    }

    private void leftRightMoldPlanBalance(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo scheduleResultVo,LhScheduleResultVo otherScheduleResultVo){
        if (scheduleResultVo == null || otherScheduleResultVo == null){
            return;
        }
        int diffTotalPlan = 0;
        int diffOtherTotalPlan = 0;
        int plan1 = 0;
        int plan2 = 0;
        //比对左右模每个班次的计划量，按最小的计划量执行
        if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())){
            //T日
            //1班计划量
            plan1 = scheduleResultVo.getClass1PlanQty() != null ? scheduleResultVo.getClass1PlanQty():0;
            plan2 = otherScheduleResultVo.getClass1PlanQty() != null ? otherScheduleResultVo.getClass1PlanQty():0;
            if (plan1 > plan2){
                diffTotalPlan += plan1 - plan2;
                scheduleResultVo.setClass1PlanQty(plan2 == 0 ? null:plan2);
                scheduleResultVo.setSpecEndTime(otherScheduleResultVo.getSpecEndTime());
            }else if (plan1 < plan2){
                diffOtherTotalPlan += plan2 - plan1;
                otherScheduleResultVo.setClass1PlanQty(plan1 == 0 ? null:plan1);
                otherScheduleResultVo.setSpecEndTime(scheduleResultVo.getSpecEndTime());
            }
            //2班计划量
            plan1 = scheduleResultVo.getClass2PlanQty() != null ? scheduleResultVo.getClass2PlanQty():0;
            plan2 = otherScheduleResultVo.getClass2PlanQty() != null ? otherScheduleResultVo.getClass2PlanQty():0;
            if (plan1 > plan2){
                diffTotalPlan += plan1 - plan2;
                scheduleResultVo.setClass2PlanQty(plan2 == 0 ? null:plan2);
                scheduleResultVo.setSpecEndTime(otherScheduleResultVo.getSpecEndTime());
            }else if (plan1 < plan2){
                diffOtherTotalPlan += plan2 - plan1;
                otherScheduleResultVo.setClass2PlanQty(plan1 == 0 ? null:plan1);
                otherScheduleResultVo.setSpecEndTime(scheduleResultVo.getSpecEndTime());
            }
            //3班计划量
            plan1 = scheduleResultVo.getClass3PlanQty() != null ? scheduleResultVo.getClass3PlanQty():0;
            plan2 = otherScheduleResultVo.getClass3PlanQty() != null ? otherScheduleResultVo.getClass3PlanQty():0;
            if (plan1 > plan2){
                diffTotalPlan += plan1 - plan2;
                scheduleResultVo.setClass3PlanQty(plan2 == 0 ? null:plan2);
                scheduleResultVo.setSpecEndTime(otherScheduleResultVo.getSpecEndTime());
            }else if (plan1 < plan2){
                diffOtherTotalPlan += plan2 - plan1;
                otherScheduleResultVo.setClass3PlanQty(plan1 ==0 ? null:plan1);
                otherScheduleResultVo.setSpecEndTime(scheduleResultVo.getSpecEndTime());
            }

        }else {
            // T+1日
            //4班计划量
            plan1 = scheduleResultVo.getClass4PlanQty() != null ? scheduleResultVo.getClass4PlanQty():0;
            plan2 = otherScheduleResultVo.getClass4PlanQty() != null ? otherScheduleResultVo.getClass4PlanQty():0;
            if (plan1 > plan2){
                diffTotalPlan += plan1 - plan2;
                scheduleResultVo.setClass4PlanQty(plan2 == 0 ? null:plan2);
                scheduleResultVo.setSpecEndTime(otherScheduleResultVo.getSpecEndTime());
            }else if (plan1 < plan2){
                diffOtherTotalPlan += plan2 - plan1;
                otherScheduleResultVo.setClass4PlanQty(plan1 ==0 ? null:plan1);
                otherScheduleResultVo.setSpecEndTime(scheduleResultVo.getSpecEndTime());
            }
            //5班计划量
            plan1 = scheduleResultVo.getClass5PlanQty() != null ? scheduleResultVo.getClass5PlanQty():0;
            plan2 = otherScheduleResultVo.getClass5PlanQty() != null ? otherScheduleResultVo.getClass5PlanQty():0;
            if (plan1 > plan2){
                diffTotalPlan += plan1 - plan2;
                scheduleResultVo.setClass5PlanQty(plan2 == 0 ? null:plan2);
                scheduleResultVo.setSpecEndTime(otherScheduleResultVo.getSpecEndTime());
            }else if (plan1 < plan2){
                diffOtherTotalPlan += plan2 - plan1;
                otherScheduleResultVo.setClass5PlanQty(plan1 ==0 ? null:plan1);
                otherScheduleResultVo.setSpecEndTime(scheduleResultVo.getSpecEndTime());
            }
            //6班计划量
            plan1 = scheduleResultVo.getClass6PlanQty() != null ? scheduleResultVo.getClass6PlanQty():0;
            plan2 = otherScheduleResultVo.getClass6PlanQty() != null ? otherScheduleResultVo.getClass6PlanQty():0;
            if (plan1 > plan2){
                diffTotalPlan += plan1 - plan2;
                scheduleResultVo.setClass6PlanQty(plan2 == 0 ? null:plan2);
                scheduleResultVo.setSpecEndTime(otherScheduleResultVo.getSpecEndTime());
            }else if (plan1 < plan2){
                diffOtherTotalPlan += plan2 - plan1;
                otherScheduleResultVo.setClass6PlanQty(plan1 ==0 ? null:plan1);
                otherScheduleResultVo.setSpecEndTime(scheduleResultVo.getSpecEndTime());
            }
        }

        if (diffTotalPlan > 0){
            contextDTO.getLogDetail().append(String.format("规格:%s,左右模平衡,减少计划量为:%s,进入未排！",scheduleResultVo.getSpecCode(),diffTotalPlan)).append(ApsConstant.DIVISION);
            createNoSchedule(scheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.num.leftRightBalance"),diffTotalPlan),
                    contextDTO,diffTotalPlan);
        }
        if (diffOtherTotalPlan > 0){
            contextDTO.getLogDetail().append(String.format("规格:%s,左右模平衡,减少计划量为:%s,进入未排！",otherScheduleResultVo.getSpecCode(),diffOtherTotalPlan)).append(ApsConstant.DIVISION);
            createNoSchedule(otherScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.num.leftRightBalance"),diffOtherTotalPlan),
                    contextDTO,diffOtherTotalPlan);
        }
    }
    /**
     * 单边补量
     * @param contextDTO
     * @param lhScheduleResultFinalVoList
     * @param machineCode
     * @param scheduleResultVo
     */
    private void machineOneSideReplenishment(AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> lhScheduleResultFinalVoList, String machineCode, LhScheduleResultVo scheduleResultVo) {
        Date classEndTime;
        Date classStartTime;
        //标识续作
        scheduleResultVo.setIsContinue(ApsConstant.TRUE);
        classEndTime = getClassEndTime(contextDTO, scheduleResultVo);
        classStartTime = scheduleResultVo.getSpecEndTime();
        if (scheduleResultVo.getSpecEndTime() == null){
            classStartTime = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? scheduleResultVo.getClass1StartTime(): scheduleResultVo.getClass4StartTime();
        }
        long diffMillTimes = DateUtils.getDiffMillTime(classStartTime, classEndTime);
        // 扣减辅助时间
        diffMillTimes = diffMillTimes/1000 - contextDTO.getBrushBagTime() * contextDTO.getWorkShifts();
        if (diffMillTimes <= 0) {
            //满排，没有偏差，则略过
            return;
        }
      /*  if (!checkNeedReplenishment(contextDTO,scheduleResultVo)){
            //最后规格的模具是否存在于未排的可用模具列表中，若存在，表示不需要补量
            return;
        }*/

        //2、按最后的规格进行补量
        //硫化时长=单胎硫化时长+刷囊时间
        //Double realLhTime = scheduleResultVo.getLhTime().doubleValue() + contextDTO.getBrushBagTime();
        Double realLhTime = scheduleResultVo.getLhTime().doubleValue();
        int replenishmentLhQty = CommonUtils.calcPeriodCapacity(BigDecimal.valueOf(diffMillTimes),BigDecimal.valueOf(realLhTime), scheduleResultVo.getMoldQty());
        //3、检查规格月度剩余,获取实际的补量
        Integer remainMpQty = contextDTO.getRemainMpQtyMap().get(scheduleResultVo.getSpecCode());
        remainMpQty = (remainMpQty ==null || remainMpQty<=0) ? 0:remainMpQty;
        int realReplenishmentQty = replenishmentLhQty > remainMpQty ? remainMpQty : replenishmentLhQty;
        //4、按班连续排载
        //String[] machineCodes = entry.getKey().split(ApsConstant.SPLIT_CHAR);
        LhMachineInfoVo selectedLhMachineInfo = scheduleResultVo.getAvailableLhMachineList().stream().filter(x->x.getMachineCode().equals(machineCode)).findFirst().orElse(null);
        Map<String, List<LhScheduleResultVo>> tmpMachineScheduledMap =
                lhScheduleResultFinalVoList.stream().collect(Collectors.groupingBy(LhScheduleResultVo::getLhMachineCode));
        //重算机台剩余时间及剩余产能
        reCalcOneMachineRemainTime(contextDTO, scheduleResultVo, tmpMachineScheduledMap.get(selectedLhMachineInfo.getMachineCode()),selectedLhMachineInfo,classStartTime);
        calcOneMachineRemainCapacity(contextDTO,scheduleResultVo,lhScheduleResultFinalVoList,selectedLhMachineInfo,true);

        contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,计算需要补的量:%s,月度剩余量:%s,实际补的计划量:%s ！", scheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),replenishmentLhQty,remainMpQty,realReplenishmentQty)).append(ApsConstant.DIVISION);
        if (realReplenishmentQty <=0){
            //没有实际要补的，继续
            return;
        }
        realScheduleClass(realReplenishmentQty, scheduleResultVo, selectedLhMachineInfo, contextDTO, lhScheduleResultFinalVoList);
    }

    /**
     * 检查是否需要补量，若最后规格的模具存在于未排的可用模具列表中时，表示不需要补量，其应该是准备换模
     * @param contextDTO
     * @param scheduleResultVo
     * @return
     */
    private boolean checkNeedReplenishment(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo scheduleResultVo){
        List<LhMoldInfoVo> moldInfoList = JSON.parseObject(scheduleResultVo.getMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {});
        if (PubUtil.isEmpty(moldInfoList)){
            return true;
        }
        List<LhUnscheduledResult> lhUnscheduledResultList = contextDTO.getLhUnscheduledResultList();
        if (PubUtil.isEmpty(lhUnscheduledResultList)){
            return true;
        }
        List<LhMoldInfoVo> availLhMoldInfoVoList;
        for (LhMoldInfoVo moldInfoVo:moldInfoList){
            for (LhUnscheduledResult unscheduledResult:lhUnscheduledResultList){
                availLhMoldInfoVoList = unscheduledResult.getAvailLhMoldInfoVoList();
                if (PubUtil.isEmpty(availLhMoldInfoVoList)){
                    continue;
                }
                //若当前模具，存在于未排中可用的模具列表，表示不需要补量
                for (LhMoldInfoVo availMoldInfo:availLhMoldInfoVoList){
                    if (moldInfoVo.getMoldNo().equals(availMoldInfo.getMoldNo())){
                        return false;
                    }
                }
            }
        }
        return true;
    }
    /**
     * 获取日开始时间
     * @param contextDTO 上下文
     * @param scheduleResultVo 硫化排程
     */
    private Date getDayStartTime(AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo scheduleResultVo) {
        Date classStartTime;
        if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())) {
            classStartTime = scheduleResultVo.getClass1StartTime();
        } else {
            classStartTime = scheduleResultVo.getClass4StartTime();
        }
        return classStartTime;
    }

    /**
     * 获取班次结束时间
     * @param contextDTO 上下文
     * @param scheduleResultVo 硫化排程
     */
    private Date getClassEndTime(AutoLhScheduleResultContextDTO contextDTO, LhScheduleResultVo scheduleResultVo) {
        Date classEndTime;
        if (ApsConstant.TRUE.equals(contextDTO.getTDayFlag())) {
            classEndTime = contextDTO.getWorkShifts() == 2 ? scheduleResultVo.getClass2EndTime() : scheduleResultVo.getClass3EndTime();
        } else {
            classEndTime = contextDTO.getWorkShifts() == 2 ? scheduleResultVo.getClass5EndTime() : scheduleResultVo.getClass6EndTime();
        }
        return classEndTime;
    }

    /**
     * 检查是否还有剩余模数
     *
     * @param lhScheduleResultVo 硫化排程
     * @return
     */
    private boolean checkHasRemainMoldQty(LhScheduleResultVo lhScheduleResultVo,
                                          AutoLhScheduleResultContextDTO contextDTO) {
        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsSingleMold())) {
            //单模
            if (lhScheduleResultVo.getRemainMoldQty() < ApsConstant.SINGLE_MOLD) {
                contextDTO.getLogDetail().append(String.format("规格:%s,单模排产,剩余可用模数为:%s,进入未排！",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getRemainMoldQty())).append(ApsConstant.DIVISION);
                createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.mold.notEnough"),lhScheduleResultVo.getDailyPlanQty()),
                        contextDTO,null);
                return false;
            }
        } else {
            //多模
            //if (lhScheduleResultVo.getRemainMoldQty() < ApsConstant.SINGLE_MOLD) {
            if (lhScheduleResultVo.getRemainMoldQty() < ApsConstant.DOUBLE_MOLD) {
                contextDTO.getLogDetail().append(String.format("规格:%s,多模排产,剩余可用模数为:%s,进入未排！",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getRemainMoldQty())).append(ApsConstant.DIVISION);
                createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.mold.notEnough"),lhScheduleResultVo.getDailyPlanQty()),
                        contextDTO,null);
                return false;
            }
        }
        return true;
    }

    /**
     * 选中机台并按班次连载
     *
     * @param lhScheduleResultVo          当前硫化排程
     * @param selectedLhMachineInfo       当前选中的机台
     * @param contextDTO                  上下文
     * @param lhScheduleResultFinalVoList 已排机台的硫化排程
     */
    private void selectedMachineAndScheduleClass(LhScheduleResultVo lhScheduleResultVo, LhMachineInfoVo selectedLhMachineInfo,
                                                 AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> lhScheduleResultFinalVoList) {
        boolean bSuccess = false;
        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsContinue()) ||
                ApsConstant.TRUE.equals(lhScheduleResultVo.getIsDelivery()) ||
                ApsConstant.TRUE.equals(lhScheduleResultVo.getIsEnd())) {
            //1、若任务是续作或有交期或收尾，则强制按班连续排载
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,有续作或有交期或有收尾,按班次连载,相关信息:【是否续作:%s,是否交期:%s,是否收尾:%s】！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getIsContinue(),lhScheduleResultVo.getIsDelivery(),lhScheduleResultVo.getIsEnd())).append(ApsConstant.DIVISION);
            bSuccess = realScheduleClass(lhScheduleResultVo.getDailyPlanQty(), lhScheduleResultVo, selectedLhMachineInfo,contextDTO, lhScheduleResultFinalVoList);
        } else {
            //2、非续作、非交期、非收尾，新机台的情况
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,非续作非交期非收尾,按班次连载开始！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
            //2.2 T+1日有计划且已排机台产能不满足T+1日需求
            if (checkHadMachineMeetT1DayCapacity(lhScheduleResultVo,contextDTO, lhScheduleResultFinalVoList)) {
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,非续作非交期非收尾,T+1日有计划且已排机台产能不满足T+1日需求,继续按班次连载！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
                // 按班连续排载
                bSuccess = realScheduleClass(lhScheduleResultVo.getDailyPlanQty(), lhScheduleResultVo, selectedLhMachineInfo,contextDTO, lhScheduleResultFinalVoList);
            } else {
                // 直接舍弃不排
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,非续作非交期非收尾,T+1日无计划或已排机台产能满足T+1日需求,直接舍弃不排！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
                lhScheduleResultVo.setLhMachineCode(null);
                lhScheduleResultVo.setLhMachineName(null);
                lhScheduleResultVo.setMachineOrder(null);
                lhScheduleResultVo.setMoldInfo(null);
                lhScheduleResultVo.setLeftRightMold(null);
                createNoSchedule(lhScheduleResultVo, StringUtils.format(I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.tailQty.giveUpSchedule"),selectedLhMachineInfo.getMachineCode(),lhScheduleResultVo.getDailyPlanQty()),
                        contextDTO,null);
                deleteMachineMoldInfo(selectedLhMachineInfo,lhScheduleResultVo.getSpecCode());
                return;
            }
        }

        if (bSuccess){
            lhScheduleResultFinalVoList.add(lhScheduleResultVo);
            //增加已排计划量
            addHadSchedulePlanNum(contextDTO,lhScheduleResultVo,true);
        }else{
            //撤回机台的模具信息 pancd+ 20250411
            if (StringUtils.isNotEmpty(selectedLhMachineInfo.getOnLineMoldInfo())){
                List<LhMoldInfoVo> moldInfoList = JSON.parseObject(selectedLhMachineInfo.getOnLineMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {});
                moldInfoList = moldInfoList.stream().filter(x->!lhScheduleResultVo.getSpecCode().equals(x.getUsedSpecCode())).collect(Collectors.toList());
                selectedLhMachineInfo.setOnLineMoldInfo(PubUtil.isEmpty(moldInfoList)? null:JSON.toJSONString(moldInfoList));
            }
        }
    }

    /**
     * 检查规格在已排机台是否满足T+1日的需求
     * @param lhScheduleResultVo 当前硫化排程
     * @param contextDTO 上下文
     * @param lhScheduleResultFinalVoList 已排硫化排程列表
     * @return
     */
    private boolean checkHadMachineMeetT1DayCapacity(LhScheduleResultVo lhScheduleResultVo, AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> lhScheduleResultFinalVoList) {
        //1.预估已排机台在T+1日的产能
        int scheduledTotalCapacity = 0;
        int scheduledCapacity = 0;
        List<LhScheduleResultVo> lhScheduledList = lhScheduleResultFinalVoList.stream().filter(item -> lhScheduleResultVo.getSpecCode().equals(item.getSpecCode())).collect(Collectors.toList());
        if (PubUtil.isNotEmpty(lhScheduledList)) {
            for (LhScheduleResultVo resultVo : lhScheduledList) {
                //单日总产能 = 单班产能*单日班数
                scheduledCapacity = resultVo.getSingleMoldShiftLhQty() * contextDTO.getWorkShifts();
                scheduledTotalCapacity = scheduledTotalCapacity + scheduledCapacity;
                contextDTO.getLogDetail().append(String.format("规格:%s,该规格在T+1日的机台:%s,单班产能:%s * 单日班数:%s,算出该机台单日产能:%s,累计该规格单日产能:%s！",lhScheduleResultVo.getSpecCode(),resultVo.getLhMachineCode(),resultVo.getSingleMoldShiftLhQty(),contextDTO.getWorkShifts(),scheduledCapacity,scheduledTotalCapacity)).append(ApsConstant.DIVISION);
            }
        }
        //2.规格T+1日的计划量
        int scheduledTotalPlan = 0;
        List<LhScheduleResultVo> t1LhScheduledList = contextDTO.getT1DayScheduleList().stream().filter(item -> lhScheduleResultVo.getSpecCode().equals(item.getSpecCode())).collect(Collectors.toList());
        if (PubUtil.isNotEmpty(t1LhScheduledList)) {
            for (LhScheduleResultVo resultVo : t1LhScheduledList) {
                scheduledTotalPlan = scheduledTotalPlan + resultVo.getDailyPlanQty();
            }
        }
        contextDTO.getLogDetail().append(String.format("规格:%s,该规格在T+1的计划量:%s,预估该规格在已排机台的T+1日产能:%s！",lhScheduleResultVo.getSpecCode(),scheduledTotalPlan,scheduledTotalCapacity)).append(ApsConstant.DIVISION);
        return scheduledTotalPlan > 0 && scheduledTotalCapacity < scheduledTotalPlan;
    }

    /**
     * 根据机台类型，设置硫化时间
     * @param lhScheduleResultVo
     * @param selectedLhMachineInfo
     */
    private void setLhTime(LhScheduleResultVo lhScheduleResultVo, LhMachineInfoVo selectedLhMachineInfo) {
        int curingTime = 0;
        if (MachineTypeEnum.MACHINERY.getCode().equals(selectedLhMachineInfo.getMachineType())) {
            curingTime = lhScheduleResultVo.getMachineryCuringTime() != null ? lhScheduleResultVo.getMachineryCuringTime() : 0;
        } else if (MachineTypeEnum.HYDRAULIC_PRESSURE.getCode().equals(selectedLhMachineInfo.getMachineType())) {
            curingTime = lhScheduleResultVo.getHydraulicPressureCuringTime() != null ? lhScheduleResultVo.getHydraulicPressureCuringTime() : 0;
        }
        if (curingTime == 0){
            throw new BusinessException(StringUtils.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.curingTime.empty"), lhScheduleResultVo.getProductCode(),lhScheduleResultVo.getSpecCode()));
        }
        lhScheduleResultVo.setLhTime(BigDecimal.valueOf(curingTime));
    }

    /**
     * 设置关联模具，标记共用模
     *
     * @param lhScheduleResultVo          当前硫化排程结果
     * @param lhScheduleResultFinalVoList 已排硫化排程结果列表
     */
    private void setLhMoldInfo(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo lhScheduleResultVo,LhMachineInfoVo lhMachineInfoVo,
                               List<LhScheduleResultVo> lhScheduleResultFinalVoList,StringBuilder logDetail) {
      /*  List<LhMachineInfoVo> availableLhMachineList = lhScheduleResultVo.getAvailableLhMachineList();
        if (PubUtil.isEmpty(availableLhMachineList)) {
            return;
        }
        //1、在可用机台列表中筛选出机台信息
        LhMachineInfoVo lhMachineInfoVo = availableLhMachineList.stream().filter(x -> x.getMachineCode().equals(lhScheduleResultVo.getLhMachineCode())).findFirst().orElse(null);
        if (lhMachineInfoVo == null) {
            return;
        }*/
        if (ApsConstant.TRUE.equals(lhMachineInfoVo.getIsChangeMoldFlag())){
            //有换模标志的，强清，重新匹配模具
            lhMachineInfoVo.setOnLineMoldInfo(null);
        }

        //2、设置模具信息
        //机台上的模具信息
        List<LhMoldInfoVo> machineMatchMoldInfoList = new ArrayList<>();
        //当前规格需要增补的模具
        List<LhMoldInfoVo> newMatchMoldInfoList = new ArrayList<>();
        if (StringUtils.isNotEmpty(lhMachineInfoVo.getOnLineMoldInfo())) {
            //2.1 存在在机模具信息,直接使用
            List<LhMoldInfoVo> onLineMoldInfoList = JSON.parseObject(lhMachineInfoVo.getOnLineMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {});
            if (ApsConstant.SINGLE_MOLD.equals(onLineMoldInfoList.size())){
                //若只有1模，且该规格还有模具时，则需要拼模
                machineMatchMoldInfoList.add(onLineMoldInfoList.get(0));
                //增补模具
                newMatchMoldInfoList = addNotOnlineMouldInfo(lhScheduleResultVo, lhScheduleResultFinalVoList,lhMachineInfoVo,machineMatchMoldInfoList);
            }else{
                newMatchMoldInfoList.addAll(onLineMoldInfoList);
                machineMatchMoldInfoList.addAll(onLineMoldInfoList);
            }
            logDetail.append(String.format("规格:%s,机台:%s,在机模具数:%s,含增补后规格模具数:%s",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getLhMachineCode(),onLineMoldInfoList.size(),newMatchMoldInfoList.size())).append(ApsConstant.DIVISION);
        } else {
            //2.2 非在机模具，则从规格模具列表中筛选
            newMatchMoldInfoList = addNotOnlineMouldInfo(lhScheduleResultVo, lhScheduleResultFinalVoList,lhMachineInfoVo,machineMatchMoldInfoList);
            logDetail.append(String.format("规格:%s,机台:%s,非在机模具,机台模具数:%s,规格模具数:%s",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getLhMachineCode(),machineMatchMoldInfoList.size(),newMatchMoldInfoList.size())).append(ApsConstant.DIVISION);
        }

        if (PubUtil.isNotEmpty(newMatchMoldInfoList)) {
            lhScheduleResultVo.setMoldInfo(JSON.toJSONString(newMatchMoldInfoList));
            lhScheduleResultVo.setMoldQty(newMatchMoldInfoList.size());
            if (ApsConstant.SINGLE_MOLD.equals(newMatchMoldInfoList.size())){
                //设置左右模
                setLeftRightMold(contextDTO,lhScheduleResultVo, lhScheduleResultFinalVoList, lhMachineInfoVo);
            }
            logDetail.append(String.format("规格:%s,机台:%s,规格模具数:%s,左右模:%s,规格上的模具信息:%s", lhScheduleResultVo.getSpecCode(), lhScheduleResultVo.getLhMachineCode(), lhScheduleResultVo.getMoldQty(), lhScheduleResultVo.getLeftRightMold(),lhScheduleResultVo.getMoldInfo())).append(ApsConstant.DIVISION);
        }
        if (PubUtil.isNotEmpty(machineMatchMoldInfoList)) {
            lhMachineInfoVo.setOnLineMoldInfo(JSON.toJSONString(machineMatchMoldInfoList));
            logDetail.append(String.format("规格:%s,机台:%s,机台模具数:%s,机台上的模具信息:%s", lhScheduleResultVo.getSpecCode(), lhMachineInfoVo.getMachineCode(), machineMatchMoldInfoList.size(), lhMachineInfoVo.getOnLineMoldInfo())).append(ApsConstant.DIVISION);
        }
    }

    /**
     * 增加非在机的模具信息
     * @param lhScheduleResultVo 当前排产规格
     * @param lhScheduleResultFinalVoList 已排硫化排程
     * @param lhMachineInfoVo 机台信息
     * @param machineMatchMoldInfoList 机台上的模具信息（总的）
     * @return
     */
    private List<LhMoldInfoVo> addNotOnlineMouldInfo(LhScheduleResultVo lhScheduleResultVo, List<LhScheduleResultVo> lhScheduleResultFinalVoList,
                                       LhMachineInfoVo lhMachineInfoVo,List<LhMoldInfoVo> machineMatchMoldInfoList) {
        List<LhMoldInfoVo> newMatchMoldInfoList  = new ArrayList<>();
        List<LhMachineInfoVo> availableLhMachineList = lhScheduleResultVo.getAvailableLhMachineList();
        List<LhMoldInfoVo> availLhMoldInfoVoList = lhScheduleResultVo.getAvailLhMoldInfoVoList();
        if (PubUtil.isEmpty(availLhMoldInfoVoList)) {
            return newMatchMoldInfoList;
        }
        //1. 获取机台上的模具
        List<String> onMoldCodeList = getOnLineMoldCodeList(availableLhMachineList,lhMachineInfoVo);
        //2. 在规格模具列表中排除机台在产模具并排序:共用标识升序、模具号升序
        List<LhMoldInfoVo> availMoldInfoVoList = availLhMoldInfoVoList.stream().filter(x -> onMoldCodeList.indexOf(x.getMoldNo()) < 0).distinct().sorted(Comparator.nullsLast(Comparator.comparing(LhMoldInfoVo::getShareNum))
                .thenComparing(LhMoldInfoVo::getMoldNo, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        if (PubUtil.isEmpty(availMoldInfoVoList)) {
            return newMatchMoldInfoList;
        }
        //已在机台的模具里，且在当前规格的可用模具清单里的，要补回到当前规格的模具清单中
        if (PubUtil.isNotEmpty(machineMatchMoldInfoList)){
            for (LhMoldInfoVo moldInfoVo:machineMatchMoldInfoList){
                if (availMoldInfoVoList.stream().filter(x->x.getMoldNo().equals(moldInfoVo.getMoldNo())).count()>0){
                    moldInfoVo.setUsedSpecCode(lhScheduleResultVo.getSpecCode());
                    newMatchMoldInfoList.add(moldInfoVo);
                }
            }
        }

        //3. 获取已排的规格模具信息
        List<String> scheduledMoldCodeList = getScheduledMoldCodeList(lhScheduleResultFinalVoList);
        //4. 从规格可用模具列表中筛选出模具
        int maxMoldCount = lhMachineInfoVo.getMaxMoldNum() == null ? Integer.valueOf(ApsConstant.APS_STRING_2): lhMachineInfoVo.getMaxMoldNum();
        //需要新追加的模具数 = 模台数 - 已匹配的模具数
        maxMoldCount -= machineMatchMoldInfoList.size();
        if (lhScheduleResultVo.getMoldQty() == 1){
            //若仅有一副模具时，且机台上不存在该规格的模具时
            if (PubUtil.isNotEmpty(newMatchMoldInfoList)){
                maxMoldCount = 0;
            }else{
                maxMoldCount = 1;
            }
        }
        for (int i = 0; i < maxMoldCount; i++) {
            for (LhMoldInfoVo moldInfoVo : availMoldInfoVoList) {
                if (scheduledMoldCodeList.indexOf(moldInfoVo.getMoldNo()) >= 0) {
                    //若模具号在已排的规格模具列表中，则跳过，防止重复绑定
                    continue;
                }
                if (machineMatchMoldInfoList.stream().filter(x->x.getMoldNo().equals(moldInfoVo.getMoldNo())).count()<=0){
                    moldInfoVo.setUsedSpecCode(lhScheduleResultVo.getSpecCode());
                    machineMatchMoldInfoList.add(moldInfoVo);
                    newMatchMoldInfoList.add(moldInfoVo);
                    break;
                }
            }
        }
        return newMatchMoldInfoList;
    }

    /**
     * 设置左右模
     */
    private void setLeftRightMold(AutoLhScheduleResultContextDTO contextDTO,LhScheduleResultVo lhScheduleResultVo, List<LhScheduleResultVo> lhScheduleResultFinalVoList, LhMachineInfoVo lhMachineInfoVo) {
        if (ApsConstant.SINGLE_MOLD.equals(lhMachineInfoVo.getMaxMoldNum())){
            //若模台数 = 1,直接返回，不需要设左右模信息
            return;
        }

        LhScheduleResultVo otherLhScheduleResultVo = lhScheduleResultFinalVoList.stream().filter(x->x.getLhMachineCode().equals(lhMachineInfoVo.getMachineCode()) &&
                StringUtils.isNotEmpty(x.getLeftRightMold())).findFirst().orElse(null);
        if (ApsConstant.TRUE.equals(lhScheduleResultVo.getIsContinue())){
            //若是续作
            if (otherLhScheduleResultVo != null){
                lhScheduleResultVo.setLeftRightMold(otherLhScheduleResultVo.getLeftRightMold());
                //return;
            }
            //若是续作规格，看前日规格的左右模方向
            if (otherLhScheduleResultVo == null){
                List<LhScheduleResultVo> lhLastScheduleResultVoList = contextDTO.getLastMachineScheduledMap().get(lhScheduleResultVo.getLhMachineCode());
                if (PubUtil.isNotEmpty(lhLastScheduleResultVoList)){
                    lhLastScheduleResultVoList = lhLastScheduleResultVoList.stream().filter(x->x.getSpecCode().equals(lhScheduleResultVo.getSpecCode())).collect(Collectors.toList());
                    if (PubUtil.isNotEmpty(lhLastScheduleResultVoList)){
                        lhScheduleResultVo.setLeftRightMold(lhLastScheduleResultVoList.get(0).getLeftRightMold());
                        return;
                    }
                }
            }

        }
        if (otherLhScheduleResultVo == null ||
                ApsConstant.R_MOLD.equals(otherLhScheduleResultVo.getLeftRightMold())){
            lhScheduleResultVo.setLeftRightMold(ApsConstant.L_MOLD);
        }else{
            lhScheduleResultVo.setLeftRightMold(ApsConstant.R_MOLD);
        }
    }

    /**
     * 获取机台上的模具
     *
     * @param availableLhMachineList 可用机台列表
     * @return 台上的模具列表
     */
    private List<String> getOnLineMoldCodeList(List<LhMachineInfoVo> availableLhMachineList,LhMachineInfoVo lhMachineInfoVo) {
        List<String> onMoldCodeList = new ArrayList<>();
        for (LhMachineInfoVo availMachineInfoVo : availableLhMachineList) {
            if (StringUtils.isEmpty(availMachineInfoVo.getOnLineMoldInfo())) {
                continue;
            }
            if (availMachineInfoVo.getMachineCode().equals(lhMachineInfoVo.getMachineCode())){
                //将当前机台上的模具排除
                continue;
            }
            List<LhMoldInfoVo> moldInfoList = JSON.parseObject(availMachineInfoVo.getOnLineMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {
            });
            if (PubUtil.isEmpty(moldInfoList)) {
                continue;
            }
            for (LhMoldInfoVo moldInfoVo : moldInfoList) {
                if (onMoldCodeList.indexOf(moldInfoVo.getMoldNo()) < 0) {
                    onMoldCodeList.add(moldInfoVo.getMoldNo());
                }
            }
        }
        return onMoldCodeList;
    }

    /**
     * 获取已排的规格模具信息
     *
     * @param lhScheduleResultFinalVoList 已排硫化排程记录
     * @return 已排模具列表
     */
    private List<String> getScheduledMoldCodeList(List<LhScheduleResultVo> lhScheduleResultFinalVoList) {
        List<String> onMoldCodeList = new ArrayList<>();
        for (LhScheduleResultVo scheduleResultVo : lhScheduleResultFinalVoList) {
            if (StringUtils.isEmpty(scheduleResultVo.getMoldInfo())) {
                continue;
            }
            List<LhMoldInfoVo> moldInfoList = JSON.parseObject(scheduleResultVo.getMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {
            });
            if (PubUtil.isEmpty(moldInfoList)) {
                continue;
            }
            for (LhMoldInfoVo moldInfoVo : moldInfoList) {
                if (onMoldCodeList.indexOf(moldInfoVo.getMoldNo()) < 0) {
                    onMoldCodeList.add(moldInfoVo.getMoldNo());
                }
            }
        }
        return onMoldCodeList;
    }

    /**
     * 按班连续排载
     *
     * @param lhScheduleResultVo
     */
    private boolean realScheduleClass(int remainPlanQty, LhScheduleResultVo lhScheduleResultVo, LhMachineInfoVo selectedLhMachineInfo,AutoLhScheduleResultContextDTO contextDTO,
                                   List<LhScheduleResultVo> lhScheduleResultFinalVoList) {
        int workShifts = contextDTO.getWorkShifts();
        int remainMpQty = contextDTO.getRemainMpQtyMap().get(lhScheduleResultVo.getSpecCode()) != null ? contextDTO.getRemainMpQtyMap().get(lhScheduleResultVo.getSpecCode()) : 0;
        if (remainMpQty <=0){
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,月度剩余量<=0！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode())).append(ApsConstant.DIVISION);
            createNoSchedule(lhScheduleResultVo, I18nUtil.getMessage("ui.data.column.lhUnScheduleResult.remainMpQty.empty"),
                    contextDTO,null);
            deleteMachineMoldInfo(selectedLhMachineInfo,lhScheduleResultVo.getSpecCode());
            return false;
        }
        //根据月度剩余量修正剩余日计划 pancd+ 20250528
        remainPlanQty = remainMpQty > remainPlanQty ? remainPlanQty:remainMpQty;

        //获取同边和双模；双模认为左右模都一样；
        List<LhScheduleResultVo> machineScheduledResutlList = lhScheduleResultFinalVoList.stream().filter(x -> lhScheduleResultVo.getLhMachineCode().equals(x.getLhMachineCode()) && (StringUtils.isNotEmpty(lhScheduleResultVo.getLeftRightMold()) ? (lhScheduleResultVo.getLeftRightMold().equals(x.getLeftRightMold()) || StringUtils.isEmpty(x.getLeftRightMold())):true)).collect(Collectors.toList());
        LhScheduleResultVo lastScheduleResultVo = machineScheduledResutlList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        //取有效开始时间，考虑换模场景，重置前规格结束时间
        Date classStartTime = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? lhScheduleResultVo.getClass1StartTime() : lhScheduleResultVo.getClass4StartTime();
        if (lastScheduleResultVo != null){
            classStartTime = lastScheduleResultVo.getSpecEndTime();
        }
        //重新获取同机台规格（若有L/R单边模，一起取）
        machineScheduledResutlList = lhScheduleResultFinalVoList.stream().filter(x -> lhScheduleResultVo.getLhMachineCode().equals(x.getLhMachineCode())).collect(Collectors.toList());
        classStartTime = getValidStartTimeWithChangeMould(contextDTO,lhScheduleResultVo,selectedLhMachineInfo,machineScheduledResutlList,classStartTime);
        lhScheduleResultVo.setSpecEndTime(classStartTime);

        String[] classQtyFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassQtyFieldNames();
        String[] classQuotaFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassQuotaFieldNames();
        String[] classMaintainFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassMaintainFieldNames();
        String[] classAnalysisFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassAnalysisFieldNames();
        //String[] classStartTimeFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassStartTimeFieldNames();
        String[] classEndTimeFieldNameArr = ShiftSystemEnum.getByCode(workShifts).getClassEndTimeFieldNames();
        //2、计算字段取值开始位置、结束位置
        int startIndex = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? 0 : classQtyFieldNameArr.length / 2;
        int endIndex = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? classQtyFieldNameArr.length / 2 : classQtyFieldNameArr.length;
        //3、遍历班制数
        int totalRemainCapacity = 0;
        int lastClassIndex = 0;
        Date addMaintainStartTime;
        for (int i = startIndex; i < endIndex; i++) {
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,开始班次:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1)).append(ApsConstant.DIVISION);
            //3.1 硫化班计划量及班次结束时间
            int classQty = lhScheduleResultVo.getFieldValueByFieldName(classQtyFieldNameArr[i]) != null ? (Integer) lhScheduleResultVo.getFieldValueByFieldName(classQtyFieldNameArr[i]) : 0;
            Date classEndTime = lhScheduleResultVo.getFieldValueByFieldName(classEndTimeFieldNameArr[i]) != null ? (Date) lhScheduleResultVo.getFieldValueByFieldName(classEndTimeFieldNameArr[i]) : null;
            //3.2 剩余班制产能及剩余班定额
            int remainClassQty = 0;
            int remainClassQuota = 0;

            int maintainTime = selectedLhMachineInfo.getFieldValueByFieldName(classMaintainFieldNameArr[i]) != null ? (Integer) selectedLhMachineInfo.getFieldValueByFieldName(classMaintainFieldNameArr[i]) : 0;
            if (maintainTime >0){
                BigDecimal secondsDecimal = new BigDecimal(maintainTime);
                BigDecimal hours = secondsDecimal.divide(new BigDecimal(3600), 2, RoundingMode.HALF_UP); // 保留两位小数
                lhScheduleResultVo.setFieldValueByFieldName(classAnalysisFieldNameArr[i],String.format("维护:%s小时", hours.toString()));
            }

            lastClassIndex = getLastClassIndex(contextDTO);
            if (i == lastClassIndex){
                //每日最后班次，用机台剩余产能-前班次的产能合计，解决尾差问题
                remainClassQty = selectedLhMachineInfo.getRemainCapacity() - totalRemainCapacity;
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,最后班次:%s,采用机台剩余产能:%s 减去 前班次填充的产能合计:%s,最后剩余班制产能:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1,selectedLhMachineInfo.getRemainCapacity(),totalRemainCapacity,remainClassQty)).append(ApsConstant.DIVISION);
            }else{
                //在前规格结束时间，加上 维修时间，作为本班次可开始时间，等同 扣减了维修时间
                addMaintainStartTime = addMaintainTime(contextDTO,lhScheduleResultVo.getSpecEndTime(),lhScheduleResultVo,classMaintainFieldNameArr[i],i);
                remainClassQty = getRemainClassQty(lhScheduleResultVo,classEndTimeFieldNameArr[i], contextDTO,addMaintainStartTime);
                remainClassQuota = getRemainClassQuota(lhScheduleResultFinalVoList,selectedLhMachineInfo,classQtyFieldNameArr[i],classQuotaFieldNameArr[i]);
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,剩余班制产能:%s,剩余班定额:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1,remainClassQty,remainClassQuota)).append(ApsConstant.DIVISION);
                if (remainClassQty <=0 || remainClassQuota <=0){
                    //没有剩余，继续下一个班次
                    contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,没有剩余,继续下一个班次！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1)).append(ApsConstant.DIVISION);
                    continue;
                }
                //若班制产能大于班产定额，按班产定额计算
                remainClassQty = remainClassQty > remainClassQuota ? remainClassQuota:remainClassQty;
                totalRemainCapacity+=remainClassQty;
            }

            //T日剩余排程量>剩余班制产能?
            if (remainPlanQty >= remainClassQty) {
                //S13、满排：分配班制产能的量给该班，结束时间=班制结束时间
                lhScheduleResultVo.setFieldValueByFieldName(classQtyFieldNameArr[i], classQty + remainClassQty);
                lhScheduleResultVo.setSpecEndTime(classEndTime);
                contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,日剩余排程量:%s >= 剩余班制产能:%s,则满排计划量:%s(其中该班次已放计划量:%s),规格结束时间:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1,remainPlanQty,remainClassQty,lhScheduleResultVo.getFieldValueByFieldName(classQtyFieldNameArr[i]),classQty,DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,classEndTime))).append(ApsConstant.DIVISION);
                remainPlanQty = remainPlanQty - remainClassQty;
                remainMpQty = remainMpQty - remainClassQty;
                if (i == lastClassIndex){
                    //最后班次满排
                    if (lhScheduleResultVo.getMoldQty() == 2 && contextDTO.getFullMachineCodeList().indexOf(selectedLhMachineInfo.getMachineCode())<0){
                        contextDTO.getFullMachineCodeList().add(selectedLhMachineInfo.getMachineCode());
                    }
                }
            } else {
                //S14、分配T日剩余排程量给该班，结束时间=前段结束时间+实际所需时间
                //注：若有月度剩余量，加量拉满============================================
                //非首排的情况下，先将剩余计划量拉到min(月度剩余量,班次剩余量)
                //首排，计划量只有20
                if (!ApsConstant.TRUE.equals(lhScheduleResultVo.getIsFirst())){
                    remainPlanQty = remainMpQty > remainClassQty ? remainClassQty:remainMpQty;
                }
                if (lhScheduleResultVo.getMoldQty() == 2 && remainPlanQty % 2 != 0){
                    //若双模排产，且剩余计划量是单数
                    remainPlanQty += 1;
                }
                if ((classQty + remainPlanQty)>=lhScheduleResultVo.getSingleMoldShiftLhQty()){
                    //若计划量大于单班硫化量，则减 差值
                    int diffQty = classQty + remainPlanQty - lhScheduleResultVo.getSingleMoldShiftLhQty();
                    remainPlanQty -= diffQty;
                    //补量到单班硫化量，则相当于满排
                    lhScheduleResultVo.setFieldValueByFieldName(classQtyFieldNameArr[i], classQty + remainPlanQty);
                    lhScheduleResultVo.setSpecEndTime(classEndTime);
                    contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,补量到单班硫化量,则满排计划量:%s(其中该班次已放计划量:%s),规格结束时间:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1,lhScheduleResultVo.getFieldValueByFieldName(classQtyFieldNameArr[i]),classQty,DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,classEndTime))).append(ApsConstant.DIVISION);
                    remainMpQty = remainMpQty - remainPlanQty;
                    if (i == lastClassIndex){
                        //最后班次满排
                        if (lhScheduleResultVo.getMoldQty() == 2 && contextDTO.getFullMachineCodeList().indexOf(selectedLhMachineInfo.getMachineCode())<0){
                            contextDTO.getFullMachineCodeList().add(selectedLhMachineInfo.getMachineCode());
                        }
                    }
                }else{
                    lhScheduleResultVo.setFieldValueByFieldName(classQtyFieldNameArr[i], classQty + remainPlanQty);
                    //硫化时长=单胎硫化时长+刷囊时间
                    //Double realLhTime = lhScheduleResultVo.getLhTime().doubleValue() + contextDTO.getBrushBagTime();
                    Double realLhTime = lhScheduleResultVo.getLhTime().doubleValue();
                    //实际所需时间 = (填充的排程量/机台模数)*硫化时长
                    Double realLhLongTime = ((double)remainPlanQty/lhScheduleResultVo.getMoldQty()) * realLhTime;
                    contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,分配T日剩余排程量:%s,硫化时长:%s秒,实际所需时间:%s秒！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1,lhScheduleResultVo.getFieldValueByFieldName(classQtyFieldNameArr[i]),realLhTime,realLhLongTime)).append(ApsConstant.DIVISION);
                    //重置结束时间
                    lhScheduleResultVo.setSpecEndTime(DateUtils.addSeconds(lhScheduleResultVo.getSpecEndTime(), realLhLongTime.intValue()));
                    remainMpQty = remainMpQty - remainPlanQty;
                    contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,日剩余排程量:%s < 剩余班制产能:%s,则分配日剩余计划量:%s,规格结束时间:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1,remainPlanQty,remainClassQty,lhScheduleResultVo.getFieldValueByFieldName(classQtyFieldNameArr[i]),DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,lhScheduleResultVo.getSpecEndTime()))).append(ApsConstant.DIVISION);
                    break;
                }
            }

            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,结束班次:%s！",lhScheduleResultVo.getSpecCode(),selectedLhMachineInfo.getMachineCode(),i+1)).append(ApsConstant.DIVISION);
        }
        //重置月度剩余量
        contextDTO.getRemainMpQtyMap().put(lhScheduleResultVo.getSpecCode(), remainMpQty);

        return true;
    }

    /**
     * 增加维修时间
     * @param contextDTO
     * @param specEndTime
     * @param lhScheduleResultVo
     * @param classMaintainTimeFieldName
     * @return
     */
    private Date addMaintainTime(AutoLhScheduleResultContextDTO contextDTO,Date specEndTime,LhScheduleResultVo lhScheduleResultVo,
                                 String classMaintainTimeFieldName,int classIndex){
        LhMachineInfoVo machineInfoVo = contextDTO.getMaintainMachineMap().get(lhScheduleResultVo.getLhMachineCode());
        if (machineInfoVo == null){
            return specEndTime;
        }
        Date startTime = specEndTime;
        int maintainTime = machineInfoVo.getFieldValueByFieldName(classMaintainTimeFieldName) != null ? (Integer) machineInfoVo.getFieldValueByFieldName(classMaintainTimeFieldName) : 0;
        if (maintainTime >0){
            startTime = DateUtils.addSeconds(specEndTime,maintainTime);
            //用掉，则将维修时间清0，防止其他规格重复扣减
            machineInfoVo.setFieldValueByFieldName(classMaintainTimeFieldName,0);
            contextDTO.getMaintainMachineMap().put(lhScheduleResultVo.getLhMachineCode(),machineInfoVo);
            contextDTO.getLogDetail().append(String.format("规格:%s,机台:%s,按班次连载,班次:%s,维修时间:%s秒,增加维修后的可开始时间:%s！",lhScheduleResultVo.getSpecCode(),lhScheduleResultVo.getLhMachineCode(),classIndex+1,maintainTime,DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS,startTime))).append(ApsConstant.DIVISION);
        }

        return startTime;
    }
    /**
     * 获取最后班次索引
     * @param contextDTO
     * @return
     */
    private int getLastClassIndex(AutoLhScheduleResultContextDTO contextDTO) {
        //获取每日末班，若是2班制，T日取2班，T+1日取5班，索引-1
        int lastClassIndex = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? 1:3;
        if (contextDTO.getWorkShifts().equals(ShiftSystemEnum.SHIFT_SYSTEM_3.getCode())){
            //若是3班制，T日取3班，T+1日取6班，索引-1
            lastClassIndex = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? 2:5;
        }
        return lastClassIndex;
    }

    /**
     * 获取班制剩余产能
     *
     * @param lhScheduleResultVo          当前排程规格
     * @param classEndTimeFieldName       班计划结束时间字段
     * @param classStartTime 班次可开始时间
     * @return 班制剩余产能
     */
    private Integer getRemainClassQty(LhScheduleResultVo lhScheduleResultVo,
                                      String classEndTimeFieldName,
                                      AutoLhScheduleResultContextDTO contextDTO,
                                      Date classStartTime) {
        //Date classStartTime = lhScheduleResultVo.getFieldValueByFieldName(classStartTimeFieldName) != null ? (Date) lhScheduleResultVo.getFieldValueByFieldName(classStartTimeFieldName) : null;
        Date classEndTime = lhScheduleResultVo.getFieldValueByFieldName(classEndTimeFieldName) != null ? (Date) lhScheduleResultVo.getFieldValueByFieldName(classEndTimeFieldName) : null;
        //1.获取有效开始时间，考虑换模场景
       /* List<LhScheduleResultVo> machineScheduledResutlList = lhScheduleResultFinalVoList.stream().filter(x -> lhScheduleResultVo.getLhMachineCode().equals(x.getLhMachineCode())).collect(Collectors.toList());
        classStartTime = getValidStartTimeWithChangeMould(contextDTO,lhScheduleResultVo,selectedLhMachineInfo,machineScheduledResutlList,classStartTime);*/
        //2.计算班制剩余时间
        long diffMillTimes = classStartTime.before(classEndTime) ? DateUtils.getDiffMillTime(classStartTime, classEndTime):0;
        // 扣减辅助时间
        diffMillTimes = diffMillTimes/1000 - contextDTO.getBrushBagTime();
        if (diffMillTimes <=0){
            return 0;
        }
        //Double realLhTime = lhScheduleResultVo.getLhTime().doubleValue() + contextDTO.getBrushBagTime();
        Double realLhTime = lhScheduleResultVo.getLhTime().doubleValue();
        return CommonUtils.calcPeriodCapacity(BigDecimal.valueOf(diffMillTimes),BigDecimal.valueOf(realLhTime),lhScheduleResultVo.getMoldQty());
    }

    /**
     * 获取前规格结束时间
     * @param contextDTO
     * @param machineScheduledResutlList
     * @return
     */
    private Date getSpecEndTime(AutoLhScheduleResultContextDTO contextDTO, List<LhScheduleResultVo> machineScheduledResutlList) {
        Date classStartTime;
        LhScheduleResultVo maxEndTimeResultVo = machineScheduledResutlList.stream().sorted(Comparator.comparing(LhScheduleResultVo::getSpecEndTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()).findFirst().orElse(null);
        classStartTime = maxEndTimeResultVo.getSpecEndTime();
        if (classStartTime == null) {
            classStartTime = ApsConstant.TRUE.equals(contextDTO.getTDayFlag()) ? maxEndTimeResultVo.getClass1StartTime() : maxEndTimeResultVo.getClass4StartTime();
        }
        return classStartTime;
    }

    /**
     * 获取班制剩余定额
     *
     * @param lhScheduleResultFinalVoList 已排硫化排程结果
     * @param selectedLhMachineInfo     选中的机台
     * @param classQtyFieldName       班计划量字段
     * @param classQuotaFieldName     班定额字段
     * @return 班制剩余产能
     */
    private Integer getRemainClassQuota(List<LhScheduleResultVo> lhScheduleResultFinalVoList,
                                           LhMachineInfoVo selectedLhMachineInfo,
                                      String classQtyFieldName,String classQuotaFieldName) {
        int classQuota = selectedLhMachineInfo.getFieldValueByFieldName(classQuotaFieldName) != null ? (Integer) selectedLhMachineInfo.getFieldValueByFieldName(classQuotaFieldName) : 0;
        int usedClassQuota = 0;
        //1. 根据机台，过滤已排硫化排程结果，计算已用定额
        if (PubUtil.isNotEmpty(lhScheduleResultFinalVoList)) {
            List<LhScheduleResultVo> machineScheduledResutlList = lhScheduleResultFinalVoList.stream().filter(x -> selectedLhMachineInfo.getMachineCode().equals(x.getLhMachineCode())).collect(Collectors.toList());
            if (PubUtil.isNotEmpty(machineScheduledResutlList)) {
                usedClassQuota = machineScheduledResutlList.stream().mapToInt(x->{
                    return x.getFieldValueByFieldName(classQtyFieldName) != null ? (Integer) x.getFieldValueByFieldName(classQtyFieldName) : 0;
                }).sum();
            }
        }
        //2.计算班制剩余定额
        return classQuota - usedClassQuota;
    }

    /**
     * 将当前机台上规格所使用的模具信息排除
     * @param selectedLhMachineInfo
     * @param specCode
     */
    private void deleteMachineMoldInfo(LhMachineInfoVo selectedLhMachineInfo,String specCode){
        if (PubUtil.isEmpty(selectedLhMachineInfo.getOnLineMoldInfo())){
            return;
        }
        List<LhMoldInfoVo> moldInfoList = JSON.parseObject(selectedLhMachineInfo.getOnLineMoldInfo(), new TypeReference<List<LhMoldInfoVo>>() {});
        if (PubUtil.isEmpty(moldInfoList)){
            return;
        }
        //将当前规格使用模具 排除掉
        List<LhMoldInfoVo> newMoldInfoList = new ArrayList<>();
        for (LhMoldInfoVo moldInfoVo:moldInfoList){
            if (!moldInfoVo.getUsedSpecCode().equals(specCode)){
                newMoldInfoList.add(moldInfoVo);
            }
        }
        selectedLhMachineInfo.setOnLineMoldInfo(JSON.toJSONString(newMoldInfoList));
    }
    /**
     * 生成未排记录
     *
     * @param lhScheduleResultVo 排程记录
     * @param unScheduleReason   未排原因
     * @param contextDTO         上下文
     */
    private void createNoSchedule(LhScheduleResultVo lhScheduleResultVo, String unScheduleReason,
                                  AutoLhScheduleResultContextDTO contextDTO,Integer qty) {
        //如果为空，创建数组
        if (contextDTO.getLhUnscheduledResultList() == null) {
            contextDTO.setLhUnscheduledResultList(new ArrayList<>());
        }
        if (qty == null){
            qty = lhScheduleResultVo.getDailyPlanQty();
        }
        boolean bSame = false;
        //同规格代码存在多条未排时，进行合并（小量多拆的场景）
        if(PubUtil.isNotEmpty(contextDTO.getLhUnscheduledResultList())){
            for (LhUnscheduledResult unscheduledResult : contextDTO.getLhUnscheduledResultList()) {
                if (unscheduledResult.getSpecCode().equals(lhScheduleResultVo.getSpecCode())) {
                    //未排量 = 日计划排程量
                    int dailyPlanQty = unscheduledResult.getUnscheduledQty() + qty;
                    unscheduledResult.setUnscheduledQty(dailyPlanQty);
                    if (unscheduledResult.getUnscheduledReason() != null && !unscheduledResult.getUnscheduledReason().equals(unScheduleReason)){
                        unscheduledResult.setUnscheduledReason(unscheduledResult.getUnscheduledReason()+unScheduleReason);
                    }
                    bSame = true;
                    break;
                }
            }
        }
        if (!bSame) {
            //未排量 = 日计划排程量
            LhUnscheduledResult lhUnscheduledResult = new LhUnscheduledResult();
            BeanUtils.copyProperties(lhScheduleResultVo, lhUnscheduledResult);
            lhUnscheduledResult.setUnscheduledQty(qty);
            lhUnscheduledResult.setUnscheduledReason(unScheduleReason);
            lhUnscheduledResult.setBatchNo(contextDTO.getBatchNo());
            //加载可用的模具列表，用于判断是否要补量
            lhUnscheduledResult.setAvailLhMoldInfoVoList(lhScheduleResultVo.getAvailLhMoldInfoVoList());
            contextDTO.getLhUnscheduledResultList().add(lhUnscheduledResult);
        }

    }
}
