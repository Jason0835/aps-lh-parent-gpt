package com.zlt.aps.nc.engine.service.impl;

import static com.alibaba.fastjson.JSON.toJSONString;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDouble;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.getDoubleOrDefault;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;
import static com.zlt.aps.common.core.utils.ApsCommonUtil.stripZeros;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.mapper.NcEngineMapper;
import com.zlt.aps.nc.engine.service.NcEngineCurlRollService;
import com.zlt.aps.nc.engine.service.NcEngineGlueService;
import com.zlt.aps.nc.engine.service.NcEngineLossService;
import com.zlt.aps.nc.engine.service.NcEngineMachineService;
import com.zlt.aps.nc.engine.service.NcEngineMonthSurplusService;
import com.zlt.aps.nc.engine.service.NcEngineService;
import com.zlt.aps.nc.engine.service.NcEngineStockService;
import com.zlt.aps.nc.engine.vo.NcMonthSurplusVo;
import com.zlt.aps.nc.engine.vo.NcParamsVo;
import com.zlt.aps.nc.engine.vo.NcScheduleBaseInfoVo;
import com.zlt.aps.nc.engine.vo.NcScheduleResultVo;
import com.zlt.aps.nc.engine.vo.NcTotalPlanQtyVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NcEngineServiceImpl implements NcEngineService {

    @Resource
    private NcEngineMapper ncEngineMapper;
    @Resource
    private NcEngineGlueService ncEngineGlueService;
    @Resource
    private NcEngineStockService ncEngineStockService;
    @Resource
    private NcEngineMachineService ncEngineMachineService;
    @Resource
    private IncrementService incrementService;
    @Resource
    private NcEngineLossService ncEngineLossService;
    @Resource
    private NcEngineMonthSurplusService ncEngineMonthSurplusService;
    @Resource
    private CxEngineQuotaCommonService cxEngineQuotaCommonService;
    @Resource
    private CommonMapper commonMapper;
    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    @Resource
    private NcEngineCurlRollService ncEngineCurlRollService;
    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 排程参数预设值，参数设置取不到值时使用这些预设值
     */
    private final static String DEFAULT_STANDARD_CRIMP_LENGTH = "500"; // 卷曲标准长度
    private final static String DEFAULT_CURL_DECIMAL_ROUNDING = "0.3"; // 卷曲数小数取整值
    private final static String DEFAULT_CLOSE_OUT_DAYS = "1"; // 共用规格收尾判断天数
	/**
	 * 生产阶段校验开关状态：打开
	 */
	private final static String PRODUCTION_STAGE_ON = "1";
	
    /**
     * 内衬胶自动排程
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    @Transactional(rollbackFor=Exception.class)
    public void autoNcSchedule(String scheduleDate) {
        String userName = SecurityUtils.getUsername();  //用户名称
        String cxBatchNo = "";  //成型批次号
        String batchNo = this.createBatchNo(scheduleDate);  //内衬排程批次号
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        Map<String, String> mapAssistSpec = this.mapAssistSpec(); //获得外协规格Map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        BigDecimal standardCurlLength = new BigDecimal(paramsMap.getOrDefault(EngineConstants.STANDARD_CRIMP_LENGTH, DEFAULT_STANDARD_CRIMP_LENGTH)); // 卷曲标准长度
        standardCurlLength = standardCurlLength.compareTo(BigDecimal.ZERO) > 0? standardCurlLength: new BigDecimal(DEFAULT_STANDARD_CRIMP_LENGTH); // 卷曲标准长度防错处理，不合法的配置都按默认值处理
        BigDecimal curlDecimalRounding = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CURL_DECIMAL_ROUNDING, DEFAULT_CURL_DECIMAL_ROUNDING)); // 卷曲数小数取整值，小数部分大于等于该值的进位，否则舍弃
        BigDecimal closeOutDays = new BigDecimal(paramsMap.getOrDefault(EngineConstants.CLOSE_OUT_DAYS, DEFAULT_CLOSE_OUT_DAYS)); // 共用规格收尾判断天数
        List<NcScheduleResultVo> scheduleList = ncEngineMapper.statNcScheduleBase(scheduleDate, productionStage);  //根据成型排程记录 统计出 内衬胶排程记录基础数据
        if (scheduleList == null || scheduleList.isEmpty()) {
            log.info("根据成型排程记录 统计出 内衬胶排程记录基础数据 为空");
            autoScheduleLogService.insertNcScheduleLog(batchNo, "", "自动排程失败", "自动排程失败，原因：成型排程数据为空，或没有在施工信息中找到对应的物料"); //添加日志
            throw new RuntimeException(I18nUtil.getMessage("engine.auto.scheule.tip1"));
        }
        //过滤掉成型5个班的计划量都为0的数据
        scheduleList = scheduleList.stream().filter(s -> (s.getCxClass1Plan()+s.getCxClass2Plan()+s.getCxClass3Plan()+s.getCxClass4Plan()+s.getCxClass5Plan())>0).collect(Collectors.toList());
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "根据成'型排程记录'统计出内衬胶排程记录基础数据",  toJSONString(scheduleList));
        this.ValidatedConstruction(scheduleDate, batchNo, productionStage, mapAssistSpec);   //证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
        Map<String, String> glueSeqMap = ncEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, String> specifyMachineMap = ncEngineMachineService.getSpecifyMachineMap(EngineConstants.JOB_TYPE_CAN); //获得内衬代码和定点机台的map
        Map<String, Double> planStockMap = ncEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算内衬16点预计库存
        Map<String, Double> lossRateMap = ncEngineLossService.getLossRateMap();   //损耗率map
        Map<String, NcMonthSurplusVo> monthSurplus = ncEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        List<String> closeOutSpecList = this.getCloseOutSpecList(scheduleDate, closeOutDays, productionStage); // 获取当天的收尾规格列表
        Map<String, BigDecimal> ncCurlLengthMap = ncEngineCurlRollService.getNcCurlLengthMap(); // 胎侧卷曲设置
        this.baseDataLog(batchNo, glueSeqMap, specifyMachineMap, planStockMap, lossRateMap, monthSurplus, paramsMap); //把基础数据假如到日志中
        Map<String, NcTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        for (NcScheduleResultVo scheduleVo : scheduleList) {
            cxBatchNo = scheduleVo.getCxBatchNo();
            scheduleVo.setBatchNo(batchNo);    //批次号
            String orderNo = this.createOrderNo(batchNo);   //创建工单号
            scheduleVo.setOrderNo(orderNo);

            scheduleVo.setGlueSeq(glueSeqMap.get(scheduleVo.getGlueCode()));  //胶料序号
            autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "根据'胶料顺序集合'设置胶料序号",
                    logSplit("胶料顺序集合：" + toJSONString(glueSeqMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.chooseMachine(scheduleVo, specifyMachineMap);  //选择生产线
            scheduleVo.setStockQty(planStockMap.getOrDefault(scheduleVo.getLiningCode(), 0D));  //16点预计库存
            autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "根据'16点预计库存集合'设置库存",
                    logSplit("16点预计库存集合：" + toJSONString(planStockMap), "数据结果：" + toJSONString(scheduleVo))); //添加日志

            this.newComputeSupplyTime(scheduleVo, scheduleVo.getStockQty());  //库存供应时长
            this.computeNcPlanQty(scheduleVo, totalPlanQtyMap, lossRateMap, getDouble(paramsMap.get(EngineConstants.LOSS_RATE)));  //计算内衬中班和夜班计划量
            this.computeNcCurlRoll(scheduleVo, ncCurlLengthMap, standardCurlLength, closeOutSpecList, curlDecimalRounding, totalPlanQtyMap); // 计算卷曲数
            this.setStatusAndCloseTip(scheduleVo, monthSurplus.get(scheduleVo.getLiningCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段

            if(BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan(), scheduleVo.getCxClass3Plan(), scheduleVo.getCxClass4Plan()) == 0D) {
                //判断成型前4个班是否有计划量，若无计划，判断为新投产规格，半部件计划计划量放在“预计划”栏位中，中班和夜班计划都显示为0；若有计划，半部件计划正常排产
                scheduleVo.setPrePlanQty(BigDecimalUtil.add(scheduleVo.getDayPlanQty(), scheduleVo.getNightPlanQty()));
                scheduleVo.setDayPlanQty(0D);
                scheduleVo.setNightPlanQty(0D);
            }
            scheduleVo.setIsRelease(ApsConstant.NO_RELEASE);
            scheduleVo.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            scheduleVo.setCreateTime(new Date());
            scheduleVo.setCreateBy(userName);
        }
        this.equilibrium(scheduleList, paramsMap, totalPlanQtyMap);  //中班和夜班计排程计划量均衡处理
        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
        this.equalShare(batchNo, scheduleList, paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD));  //单规格排产数量达到设定值时，中夜班数量对半分
        this.setProduceOrder(scheduleList);  //设置白班和夜班的生产顺序

        List<NcScheduleResultVo> existScheduleList = this.ncEngineMapper.listNcEnginSchedule(scheduleDate);  //查询当天已经存在的排产记录
        this.syncNcScheduleToLog(scheduleDate);  //把排程数据同步到log表
        this.createScheduleRecord(scheduleDate, cxBatchNo, batchNo);  //创建自动排程记录

        List<NcScheduleResultVo> assistScheduleList = scheduleList.stream().filter(r -> mapAssistSpec.containsKey(r.getLiningCode())).collect(Collectors.toList()); //过滤出外协排程数据
        scheduleList = scheduleList.stream().filter(r -> !mapAssistSpec.containsKey(r.getLiningCode())).collect(Collectors.toList());  //过滤出非外协的排产数据
        if(StringUtils.isNotEmpty(assistScheduleList)) {
            ncEngineMapper.batchCreateAssistScheduleResult(assistScheduleList);   //批量新增外协排程结果数据
        }

        scheduleList = this.mergeExistSchedule(batchNo, scheduleList, existScheduleList);  //如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
        if(StringUtils.isNotEmpty(scheduleList)) {
            ncEngineMapper.batchCreateScheduleResult(scheduleList);   //批量新增非外协排程结果数据
        }
    }

    /**
     * 获取当天的收尾规格列表
     * @param scheduleDate	排程日期
     * @param closeOutDays	判断收尾的天数
     * @return
     */
	private List<String> getCloseOutSpecList(String scheduleDate, BigDecimal closeOutDays, String productionStage) {
		BigDecimal queryCloseOutDays = closeOutDays.compareTo(BigDecimal.ONE) >= 0? closeOutDays.subtract(BigDecimal.ONE): BigDecimal.ZERO; // 判断天数需要减1，0才是查1天的数据进行判断
		boolean isProductionStage = PRODUCTION_STAGE_ON.equals(productionStage); // 判断是否只看投产规格
		return ncEngineMapper.listCloseOutSpec(DateUtils.parseDate(scheduleDate), queryCloseOutDays.intValue(), isProductionStage);
	}

	/**
	 * 计算胎面卷曲长度
	 * 
	 * @param scheduleVo          胎面排程
	 * @param tmCurlLengthMap     各规格卷曲长度配置
	 * @param standardCurlLength  卷曲标准长度
	 * @param closeOutSpecList    收尾规格列表
	 * @param curlDecimalRounding 卷曲数小数取整值
	 * @param totalPlanQtyMap     每个生产线的计划量汇总MAP
	 */
	private void computeNcCurlRoll(NcScheduleResultVo scheduleVo, Map<String, BigDecimal> tmCurlLengthMap,
			BigDecimal standardCurlLength, List<String> closeOutSpecList, BigDecimal curlDecimalRounding,
			Map<String, NcTotalPlanQtyVo> totalPlanQtyMap) {
		String liningCode = scheduleVo.getLiningCode();
		if (closeOutSpecList.contains(liningCode)) { // 收尾规格，则直接返回
	        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_ENABLE); // 打上收尾标记
			return;
		}
		BigDecimal curlLength = tmCurlLengthMap.get(liningCode); // 本规格的卷曲长度
		if (curlLength == null || curlLength.compareTo(BigDecimal.ZERO) <= 0) { // 不合法的配置都按默认值为准
			curlLength = standardCurlLength;
		}
		BigDecimal dayPlanQty = BigDecimalUtil.getValue(scheduleVo.getDayPlanQty());
		BigDecimal nightPlanQty = BigDecimalUtil.getValue(scheduleVo.getNightPlanQty());
		BigDecimal totalPlanQty = dayPlanQty.add(nightPlanQty); // 本规格胎面的总计划量

		BigDecimal planNum = totalPlanQty.divide(curlLength, 1, RoundingMode.UP); // 卷数，保留1位小数
		// 卷数小数部分处理
		if (planNum.subtract(planNum.setScale(0, RoundingMode.DOWN)).compareTo(curlDecimalRounding) >= 0) {
			planNum = planNum.setScale(0, RoundingMode.UP); // 如果小数部分大于等于卷曲数小数取整值，直接进位
		} else if (planNum.compareTo(curlDecimalRounding) < 0) {
			planNum = planNum.setScale(0, RoundingMode.UP); // 如果原计划卷数比最小取整卷数少，也直接进位
		} else {
			planNum = planNum.setScale(0, RoundingMode.DOWN); // 其余情况舍去小数部分
		}
		BigDecimal newPlanQty = planNum.multiply(curlLength).setScale(0, RoundingMode.UP); // 新计划量
		BigDecimal planQtyDifference = newPlanQty.subtract(totalPlanQty);

		if (planQtyDifference.compareTo(BigDecimal.ZERO) == 0) {
			return;
		}
		boolean isDay = dayPlanQty.compareTo(BigDecimal.ZERO) > 0; // 是否安排在中班
		if (isDay) {
			dayPlanQty = newPlanQty;
			scheduleVo.setDayPlanQty(dayPlanQty.doubleValue());
		} else {
			nightPlanQty = newPlanQty;
			scheduleVo.setNightPlanQty(nightPlanQty.doubleValue());
		}

		// 将增加的量补到汇总值中
		String key = scheduleVo.getMachineId(); // 机台id作为Map的key
		key = StringUtils.isBlank(key) ? "" : key;
		NcTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new NcTotalPlanQtyVo()); // 取出对应生产线的计划量汇总对象
		if (isDay) {
			totalPlanQtyVo.setTotalDayPlanQty(
					BigDecimalUtil.getValue(totalPlanQtyVo.getTotalDayPlanQty()).add(planQtyDifference).doubleValue());
		} else {
			totalPlanQtyVo.setTotalNightPlanQty(
					BigDecimalUtil.getValue(totalPlanQtyVo.getTotalNightPlanQty()).add(planQtyDifference).doubleValue());
		}
		totalPlanQtyVo.setTotalPlanQty(
				BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()));
		totalPlanQtyMap.put(key, totalPlanQtyVo);
	}


    /**
     * 验证成型排程记录的胎胚code在施工表中是否都能找到对应记录，如果不能则提示
     * @param scheduleDate 排程日志
     * @param batchNo 批次号
     * @param productionStage 仅投产阶段规格排产标识
     */
    private void ValidatedConstruction(String scheduleDate, String batchNo, String productionStage, Map<String, String> mapAssistSpec) {
        List<EngineConstructionInfo> list = ncEngineMapper.listNcNeedConstruction(scheduleDate, productionStage);
        list = list.stream().filter(r -> !mapAssistSpec.containsKey(r.getInsideCode())).collect(Collectors.toList());  //校验忽略掉 外协规格，只校验 不是外协的规格
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
            if(StringUtils.isBlank(construction.getInsideCode())) {
                //施工表内衬代码为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.insideCode") + "\"");
            }
            if(StringUtils.isBlank(construction.getInsideRubber())) {
                //施工表内衬胶料为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.insideRubber") + "\"");
            }
            if(construction.getSidewallLength() == null || construction.getSidewallLength() == 0) {
                //施工表内衬长(胎侧长)为空
                errorColumns.add("\"" + I18nUtil.getMessage("ui.construction.sidewallLength") + "\"");
            }
            if(!errorColumns.isEmpty()) {
                String tip = StringUtils.format(I18nUtil.getMessage("engine.auto.scheule.construction.validate"), embryoCode, embryoVersion, String.join(",", errorColumns));
                autoScheduleLogService.insertNcScheduleLog(batchNo, "", "自动排程失败", tip); //添加日志
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
        List<String> listAssistSpec = this.ncEngineMapper.listAssistSpec();
        if(listAssistSpec == null || listAssistSpec.size() == 0) {
            return map;
        }
        for(String assistSpec : listAssistSpec) {
            map.put(assistSpec, "1");
        }
        return map;
    }

    /**
     * 内衬插单
     * @param scheduleVo
     */
    public int inertNcOrder(NcScheduleResultVo scheduleVo) {
        String scheduleDate = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleVo.getScheduleDate()); //排程日期
        List<NcScheduleResultVo> scheduleList = new ArrayList<>();
        scheduleList.add(scheduleVo);
        return this.batchSaveNcSchedule(scheduleDate, scheduleList, true);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     */
    public int batchSaveNcSchedule(String scheduleDate, List<NcScheduleResultVo> scheduleList) {
        return this.batchSaveNcSchedule(scheduleDate, scheduleList, false);
    }

    /**
     * 批量更新或新增排程记录信息
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param scheduleList 排程数据
     * @param isUpdate 相同唯一键是否做更新操作。true：是
     */
    @Transactional(rollbackFor=Exception.class)
    public int batchSaveNcSchedule(String scheduleDate, List<NcScheduleResultVo> scheduleList, boolean isUpdate) {
        if(scheduleList == null || scheduleList.isEmpty()) {
            return -1;
        }
        String batchNo = "";
        if(isUpdate) {
            batchNo = ncEngineMapper.getNcCurrentBatchNo(scheduleDate);  //查询当前排程的批次号
        }
        if(StringUtils.isBlank(batchNo)) {
            //当前的批次号为空，说明还没”自动排程“，那么自己生成一个排程批次号
            batchNo = this.createBatchNo(scheduleDate);  //内衬排程批次号
            this.createScheduleRecord(scheduleDate, "", batchNo);  //创建自动排程记录
            this.syncNcScheduleToLog(scheduleDate);  //把排程数据同步到log表
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "插单或批量导入初始数据", toJSONString(scheduleList));  //添加日志

        List<String> liningCodes = scheduleList.stream().map(NcScheduleResultVo::getLiningCode).collect(Collectors.toList());
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String productionStage = paramsMap.get(EngineConstants.PRODUCTION_STAGE_PRODUCE);  //仅投产阶段规格排产标识
        Map<String, NcScheduleBaseInfoVo> scheduleBaseInfoMap = getScheduleBaseInfoMap(scheduleDate, liningCodes, productionStage);  //根据内衬代码查询对应的内衬基础信息
        Map<String, String> glueSeqMap = ncEngineGlueService.getGlueSeqMap();  //获取胶料序号map
        Map<String, Double> planStockMap = ncEngineStockService.getPlanStockMap(batchNo, scheduleDate, getDouble(paramsMap.get(EngineConstants.STOCK_LOSS_RATE)));  //计算内衬16点预计库存
        Map<String, NcMonthSurplusVo> monthSurplus = ncEngineMonthSurplusService.getMonthSurplus(scheduleDate);  //获得月度计划剩余量、完成量
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "插单或批量导入基础数据", logSplit("半部件基础数据信息:" + toJSONString(scheduleBaseInfoMap),
                "胶料序号map：" + glueSeqMap, "16点预计库存：" + planStockMap, "月度计划剩余量、完成量：" + monthSurplus, "工序参数map：" + paramsMap));  //添加日志

        for(NcScheduleResultVo schedule : scheduleList) {
            schedule.setBatchNo(batchNo);  //批次号
            String orderNo = this.createOrderNo(batchNo); //工单号
            schedule.setOrderNo(orderNo);  //工单号
            NcScheduleBaseInfoVo baseInfoVo = scheduleBaseInfoMap.get(schedule.getLiningCode());
            if(baseInfoVo != null) {
                BeanUtils.copyProperties(baseInfoVo, schedule);
            }
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            schedule.setDayPlanQty(dayPlanQty == null ? 0D : dayPlanQty);
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            schedule.setNightPlanQty(nightPlanQty == null ? 0D : nightPlanQty);

            schedule.setGlueSeq(glueSeqMap.get(schedule.getGlueCode()));  //胶料序号
            schedule.setStockQty(planStockMap.getOrDefault(schedule.getLiningCode(), 0D));  //16点预计库存
            this.newComputeSupplyTime(schedule, schedule.getStockQty());  //库存供应时长
            this.setStatusAndCloseTip(schedule, monthSurplus.get(schedule.getLiningCode()), getDouble(paramsMap.get(EngineConstants.CLOSE_OUT_NUM)));  //设置收尾提示标识 和 生产状态字段
            schedule.setIsRelease(ApsConstant.NO_RELEASE);
            schedule.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            schedule.setCreateTime(new Date());
            schedule.setCreateBy(SecurityUtils.getUsername());
            schedule.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "插单或批量导入最终数据", toJSONString(scheduleList));  //添加日志
        return ncEngineMapper.mergeNcScheduleResult(scheduleList);  //批量合并排程结果表（根据唯一字段，做更新或新增）
    }

    /**
     * 根据内衬代码查询对应的内衬基础信息
     * @param scheduleDate
     * @return
     */
    private Map<String, NcScheduleBaseInfoVo> getScheduleBaseInfoMap(String scheduleDate, List<String> liningCodes, String productionStage) {
        Map<String, NcScheduleBaseInfoVo> map = new HashMap<>();
        List<NcScheduleBaseInfoVo> list = ncEngineMapper.listNcScheduleBaseInfo(liningCodes, ""); //查询出胎面在施工表的基础信息
        if(!StringUtils.isEmpty(list)) {
            map = list.stream().collect(Collectors.toMap(NcScheduleBaseInfoVo::getLiningCode, baseInfoVo->baseInfoVo));
        }

        Map<String, NcScheduleBaseInfoVo> hasCxMap = new HashMap<>();
        List<NcScheduleResultVo> hasCxlist = ncEngineMapper.statNcScheduleBase(scheduleDate, productionStage); //查询出在有对应成型排程的胎面基础信息
        for(NcScheduleResultVo info : hasCxlist) {
            NcScheduleBaseInfoVo baseInfoVo = new NcScheduleBaseInfoVo();
            BeanUtils.copyProperties(info, baseInfoVo);
            hasCxMap.put(info.getLiningCode(), baseInfoVo);
        }

        map.putAll(hasCxMap);  //有对应成型排程的胎面基础信息 覆盖掉，没有成型排程的胎面基础信息
        return map;
    }

//    /**
//     * 转机台后，修改排程结果表相应字段数据
//     * @param oldMachineIds  转机台前，旧的机台id
//     * @param scheduleResult
//     */
//    public void changeNcMachine(String oldMachineIds, NcScheduleResult scheduleResult) {
//        String batchNo = scheduleResult.getBatchNo();  //批次号
//        String orderNo = scheduleResult.getOrderNo();  //工单号
//        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "转机台初始数据", logSplit("转机台前的机台ID：" + oldMachineIds, "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
//        Map<String, Double> lossRateMap = ncEngineLossService.getLossRateMap();   //损耗率map
//        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
//        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));
//
//        //转机台后，不同机台的损耗率不一样，需要重新计算计划量
//        double oldLossRate = ncEngineLossService.getLossRate(scheduleResult.getLiningCode(), oldMachineIds, lossRateMap, paramLossRate);  //计算出转机台前的耗损率
//        double lossRate = ncEngineLossService.getLossRate(scheduleResult.getLiningCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);
//        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "转机台需要根据不同机台耗损率重新计算计划量",
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
//        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "转机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
//    }

    /**
     * 确认自动排程机台
     * @param scheduleResult  排程信息
     */
    public void confirmNcMachine(NcScheduleResult scheduleResult) {
        String batchNo = scheduleResult.getBatchNo();  //批次号
        String orderNo = scheduleResult.getOrderNo();  //工单号
        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "确认机台初始数据", logSplit( "页面提交的信息：" + toJSONString(scheduleResult)));  //添加日志
        Map<String, Double> lossRateMap = ncEngineLossService.getLossRateMap();   //损耗率map
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        double paramLossRate = getDouble(paramsMap.get(EngineConstants.LOSS_RATE));

        //耗损率
        double lossRate = ncEngineLossService.getLossRate(scheduleResult.getLiningCode(), scheduleResult.getMachineId(), lossRateMap, paramLossRate);  //计算出新机台的耗损率
        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "确认机台耗损率", "耗损率：" + lossRate);  //添加日志

        Double dayPlanQty = scheduleResult.getDayPlanQty();  //中班计划量
        if(dayPlanQty != null) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            scheduleResult.setDayPlanQty(BigDecimalUtil.roundUp(dayPlanQty, 0));
        }
        Double nightPlanQty = scheduleResult.getNightPlanQty();  //夜班计划量
        if(nightPlanQty != null) {
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
            scheduleResult.setNightPlanQty(BigDecimalUtil.roundUp(nightPlanQty, 0));
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, orderNo, "确认机台结束后的排程数据", toJSONString(scheduleResult));  //添加日志
    }

    /**
     * 手动均衡和重新设置生产顺序
     * @param scheduleDate 排程日期,格式：yyyy-mm-dd
     */
    public void handEquilibriumAndProduceOrder(String scheduleDate) {
        List<NcScheduleResultVo> scheduleList = ncEngineMapper.listNcEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }

        String batchNo = "";
        Map<String, NcTotalPlanQtyVo> totalPlanQtyMap = new HashMap<>();  //每个生产线的计划量汇总MAP
        for(NcScheduleResultVo schedule : scheduleList ) {
            batchNo = schedule.getBatchNo();
            Double dayPlanQty = schedule.getDayPlanQty();  //中班计划量
            Double nightPlanQty = schedule.getNightPlanQty();  //夜班计划量
            //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（不二次排程逻辑处理）
            if (dayPlanQty > 0) {
                dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
                nightPlanQty = 0D;
                schedule.setDayPlanQty(dayPlanQty);
                schedule.setNightPlanQty(nightPlanQty);
            }
            if(schedule.getMachineId() == null) {
                schedule.setMachineId("");
            }
            if(schedule.getSupplyTime() == null) {
                schedule.setSupplyTime(0D);
            }
            //计算中班总计划量 和 夜班总计划量
            this.groupTotalPlanQtyMap(schedule, totalPlanQtyMap);
        }

        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        this.equilibrium(scheduleList, paramsMap, totalPlanQtyMap);  //均衡
        this.equalShare(batchNo, scheduleList, paramsMap.get(EngineConstants.EQUAL_SHARE_THRESHOLD));  //单规格排产数量达到设定值时，中夜班数量对半分
        this.setProduceOrder(scheduleList);  //生产顺序重新计算
        ncEngineMapper.batchUpdateProduceOrder(scheduleDate, scheduleList);  //批量更新各班的计划量和生产顺序
    }

    /**
     * 手动 同胶料合并生产
     * @param scheduleDate
     */
    public void handGlueMerge(String scheduleDate) {
        List<NcScheduleResultVo> scheduleList = ncEngineMapper.listNcEnginSchedule(scheduleDate);
        if(StringUtils.isEmpty(scheduleList)) {
            return;
        }
        Map<String, String> paramsMap = this.getParamsMap();  // 获取工序参数map
        String batchNo = scheduleList.get(0).getBatchNo();  //批次号
        this.glueMerge(batchNo, scheduleList, paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD), paramsMap.get(EngineConstants.GLUE_MERGE_THRESHOLD_MAX));  //同胶料合并生产
        ncEngineMapper.batchUpdatePlanQty(scheduleDate, scheduleList);  //批量更新各班的计划量
    }

    /**
     * 中班和夜班计排程计划量均衡处理(根据生产线进行分组均衡)
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void equilibrium(List<NcScheduleResultVo> scheduleList, Map<String, String> paramsMap, Map<String, NcTotalPlanQtyVo> totalPlanQtyMap) {
        Map<String, List<NcScheduleResultVo>> map = scheduleList.stream().collect(Collectors.groupingBy(s->s.getMachineId()));
        scheduleList.clear();
        map.forEach((key, valueList) -> {
            this.equilibriumOne(valueList, paramsMap, totalPlanQtyMap.get(key));
            scheduleList.addAll(valueList);
        });
    }

    /**
     * 中班和夜班计排程计划量均衡处理
     *
     * @param scheduleList   排程列表
     * @param paramsMap      工序参数
     * @param totalPlanQtyVo 内衬中班和夜班总计划量Vo
     */
    private void equilibriumOne(List<NcScheduleResultVo> scheduleList, Map<String, String> paramsMap, NcTotalPlanQtyVo totalPlanQtyVo) {
        String batchNo = "";  //批次号
        String oldScheduleList = toJSONString(scheduleList);
        double difRate = getDoubleOrDefault(paramsMap.get(EngineConstants.PLAN_DIFFERENCE_RATE) ,100D);  //参数配置：中班总量和夜班总量差额百分比
        double supplyTimePass = getDoubleOrDefault(paramsMap.get(EngineConstants.SUPPLY_TIME_PASS),12D);;  //参数配置：库存供应时长小时数
        double difNum = BigDecimalUtil.sub(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()); //中班和夜班计划量差额
        double actualDifRate = Math.abs(difNum) / totalPlanQtyVo.getTotalPlanQty() * 100;  //实际中班和夜班总计划量差额百分比
        if (actualDifRate > difRate) {
            //中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理
            boolean isDayClassPass = (difNum > 0);  //true：中班超量，false：夜班超量
            if (isDayClassPass) {
                //中班超量，排程结果按中班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(NcScheduleResultVo::getDayPlanQty)).collect(Collectors.toList());
            } else {
                //夜班超量，排程结果按夜班计划量，从小到大排序
                scheduleList = scheduleList.stream().sorted(Comparator.comparing(NcScheduleResultVo::getNightPlanQty)).collect(Collectors.toList());
            }
            //开始计划量均衡处理
            double lastDifRate = actualDifRate;  //上一次的中班总量和夜班总量的差额百分比
            for (NcScheduleResultVo resultVo : scheduleList) {
                batchNo = resultVo.getBatchNo();
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
     * 单规格排产数量达到设定值（equalShareThreshold）时，中夜班数量对半分
     * @param scheduleList 排程列表
     * @param equalShareThreshold  各班计划量均分阈值
     */
    private void equalShare(String batchNo, List<NcScheduleResultVo> scheduleList, String equalShareThreshold) {
        if(StringUtils.isBlank(equalShareThreshold)) {
            return;
        }
        Integer threshold = Integer.parseInt(equalShareThreshold);
        scheduleList.forEach(schedule->{
            Double totalPlay = BigDecimalUtil.add(schedule.getDayPlanQty(), schedule.getNightPlanQty());  //一天总计划量
            if(totalPlay >= threshold) {
                //单规格排产数量达到参数配置的阈值,中夜班数量对半分
                Double equalSharePlan = BigDecimalUtil.div(totalPlay, 2);
                schedule.setDayPlanQty(BigDecimalUtil.roundUp(equalSharePlan, 0));   //均分后，中班向上取整
                schedule.setNightPlanQty(BigDecimalUtil.roundDown(equalSharePlan, 0));  //均分后，夜班向下取整
            }
        });
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "单规格排产数量达到设定值时，中夜班数量对半分", logSplit("班计划量均分阈值:" + equalShareThreshold,
                "均分后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 同一个机台，胶料一样的排程记录，供应时长有一个小于等于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到中班;
     * 反之如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
     *
     * @param scheduleList 排程列表
     * @param glueMergethreshold  同胶料合并生产预计库存可供应时长参数
     * @param glueMergethresholdMax  同胶料归并生产可供应时长(MAX)
     */
    private void glueMerge(String batchNo, List<NcScheduleResultVo> scheduleList, String glueMergethreshold, String glueMergethresholdMax) {
        Double threshold = 12D;
        Double thresholdMax = 28D;
        try {
            threshold = Double.parseDouble(glueMergethreshold);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长参数转换错误");
        }
        try {
            thresholdMax = Double.parseDouble(glueMergethresholdMax);
        } catch (NumberFormatException e) {
            log.error("同胶料合并生产预计库存可供应时长(Max)参数转换错误");
        }

        //根据机台+胶料进行分组
        Map<String, List<NcScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for(List<NcScheduleResultVo> list : groupMap.values()) {
            boolean isPassParam = this.compareSupplyTime(list, threshold);  //判断集合中的库存供应时长 是否 有小于参数值的

            for(NcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(isPassParam) {
                    //库存供应时长 有小于 {GLUE_MERGE_THRESHOLD}参数值.计划量全归并到中班
                    scheduleVo.setDayPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    scheduleVo.setNightPlanQty(0D);
                } else {
                    //如果供应时长都大于{GLUE_MERGE_THRESHOLD}参数，则计划量都归并到夜班，在此情况下其中要是有记录的供应时长又大于{GLUE_MERGE_THRESHOLD_MAX}参数，则把计划量归并到【预计划】字段中，中班和夜班计划量变0
                    Double supplyTime = scheduleVo.getSupplyTime();
                    if(supplyTime > thresholdMax ) {
                        //则把计划量归并到【预计划】字段中，中班和夜班计划量变0
                        if(scheduleVo.getDayPlanQty() > 0 || scheduleVo.getNightPlanQty() > 0) {
                            scheduleVo.setPrePlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));  //设置预计库存
                            scheduleVo.setDayPlanQty(0D);
                            scheduleVo.setNightPlanQty(0D);
                        }
                    } else {
                        //计划量都归并到夜班
                        scheduleVo.setDayPlanQty(0D);
                        scheduleVo.setNightPlanQty(BigDecimalUtil.add(dayPlanQty, nightPlanQty));
                    }
                }
            }
            scheduleList.addAll(list);
        }
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "同胶料合并生产", logSplit("同胶料合并生产预计库存可供应时长参数:" + glueMergethreshold,
                "同胶料合并生产后排程数据：" + toJSONString(scheduleList)));  //添加日志
    }

    /**
     * 判断集合中是否有 库存供应时长 小于 参数值
     * @param list
     * @param equalShareThreshold 同胶料合并生产预计库存可供应时长参数
     * @return
     */
    private boolean compareSupplyTime(List<NcScheduleResultVo> list, Double equalShareThreshold) {
        for(NcScheduleResultVo schedule : list) {
            if(schedule.getSupplyTime() <= equalShareThreshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算每个生产线的中班总计划量、夜班总计划量，以及总计划量
     * @param scheduleVo 排程数据
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     */
    private void groupTotalPlanQtyMap(NcScheduleResultVo scheduleVo, Map<String, NcTotalPlanQtyVo> totalPlanQtyMap) {
        String key = scheduleVo.getMachineId();  //机台id作为Map的key
        key = StringUtils.isBlank(key) ? "" : key;
        NcTotalPlanQtyVo totalPlanQtyVo = totalPlanQtyMap.getOrDefault(key, new NcTotalPlanQtyVo());  //取出对应生产线的计划量汇总对象

        Double dayPlanQty = scheduleVo.getDayPlanQty();  //中班计划量
        Double nightPlanQty = scheduleVo.getNightPlanQty();  //夜班计划量
        totalPlanQtyVo.setTotalDayPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), dayPlanQty));
        totalPlanQtyVo.setTotalNightPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalNightPlanQty(), nightPlanQty));
        totalPlanQtyVo.setTotalPlanQty(BigDecimalUtil.add(totalPlanQtyVo.getTotalDayPlanQty(), totalPlanQtyVo.getTotalNightPlanQty()));
        totalPlanQtyMap.put(key, totalPlanQtyVo);  //计算完毕后，把生产线的计划量汇总对象重新存入map中
    }

    /**
     * 均衡日志
     * @param scheduleList
     * @param paramsMap
     * @param totalPlanQtyVo
     */
    private void equilibriumLog(String batchNo, String oldScheduleList, List<NcScheduleResultVo> scheduleList, Map<String, String> paramsMap, NcTotalPlanQtyVo totalPlanQtyVo) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("对排产结果进行均衡操作。中班总量和夜班总量的差额百分比超过了参数配置的百分比，则需要做均衡处理，也就是说要把其中一班的计划量合并到另外一班，" +
                "一直合并到中班和夜班计划量总量的差额不超过参数配置的百分比。其中中班合并到夜班还需要遵循一个规则，就是只有库存供应时长必须要大于参数配置的值的时候，才允许从中班合并到夜班。").append(division);
        logDetail.append("各班总计划量：" + toJSONString(totalPlanQtyVo)).append(division);
        logDetail.append("参数配置集合，这里要用到‘PLAN_DIFFERENCE_RATE（中班总量和夜班总量差额百分比）’和‘SUPPLY_TIME_PASS（库存供应时长小时数）’：" + toJSONString(paramsMap)).append(division);
        logDetail.append("均衡前的排程数据列表：" + oldScheduleList).append(division);
        logDetail.append("均衡后的排产数据列表：" + toJSONString(scheduleList));
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "计划量均衡处理", logDetail.toString());
    }

    /**
     * 根据机台+胶料进行分组，然后在根据库存供应时长，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）
     * @param scheduleList
     */
    private void setProduceOrder(List<NcScheduleResultVo> scheduleList) {
        //根据机台+胶料进行分组
        Map<String, List<NcScheduleResultVo>> groupMap = scheduleList.stream().collect(Collectors.groupingBy(v -> v.getGlueCode() + v.getMachineId()));
        scheduleList.clear();

        for(List<NcScheduleResultVo> list : groupMap.values()) {
            int dayProduceOrder = 1; //白班生产顺序
            int nightProduceOrder = 1;  //夜班生产顺序
            //根据库存供应时长升序排序
            list = list.stream().sorted(Comparator.comparing(NcScheduleResultVo::getSupplyTime)).collect(Collectors.toList());
            for(NcScheduleResultVo scheduleVo : list) {
                Double dayPlanQty = scheduleVo.getDayPlanQty();
                Double nightPlanQty = scheduleVo.getNightPlanQty();
                if(dayPlanQty > 0) {
                    scheduleVo.setDayProduceOrder(dayProduceOrder++);
                }
                if(nightPlanQty > 0) {
                    scheduleVo.setNightProduceOrder(nightProduceOrder++);
                }
                autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "设置生产顺序字段",
                        logSplit("根据机台+胶料进行分组，然后在根据库存供应时长(从小到大)，设置中班和夜班的生产顺序（有计划量的才设置生产顺序）", "设置后的排程数据：" + toJSONString(scheduleVo)));  //添加日志
            }
            scheduleList.addAll(list);
        }
    }

    /**
     * 设置收尾提示标识 和 生产状态字段
     * @param scheduleResultVo
     * @param monthSurplusVo
     * @param closeOutNum  参数配置表设置的 提示收尾阈值
     */
    private void setStatusAndCloseTip(NcScheduleResultVo scheduleResultVo, NcMonthSurplusVo monthSurplusVo, Double closeOutNum) {
        if(monthSurplusVo == null) {
            scheduleResultVo.setMarkCloseOutTip(EngineConstants.CLOSE_TIP_NOT);
            scheduleResultVo.setProductionStatus(EngineConstants.PRODUCTION_STATUS_NOT);
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
        autoScheduleLogService.insertNcScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "设置收尾提示标识markCloseOutTip",
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
        autoScheduleLogService.insertNcScheduleLog(scheduleResultVo.getBatchNo(), scheduleResultVo.getOrderNo(), "修改生产状态productionStatus",
                logSplit("①完成量为0，对应生产状态：未生产;②完成量大于0，月度计划量也大于0，说明出于生产中;③月度计划量小于等于0，说明出于生产完成",
                        "月度计划剩余量：" + monthRemainQty + ",月度计划完成量：" + monthFinishQty, "最终的排程数据：" + toJSONString(scheduleResultVo)));  //添加日志
    }

    /**
     * 如果当天排程已经存在，则把当天的排程合并到 自动排程的列表中
     * @param batchNo   批次号
     * @param autoScheduleList   自动排程列表
     * @param existScheduleList  当天已经存在的排产记录
     */
    private List<NcScheduleResultVo> mergeExistSchedule(String batchNo, List<NcScheduleResultVo> autoScheduleList, List<NcScheduleResultVo> existScheduleList) {
        if(StringUtils.isEmpty(existScheduleList)) {
            return autoScheduleList;
        }
        List<NcScheduleResultVo> mergeList = new ArrayList<>();

        Map<String, List<NcScheduleResultVo>> existScheduleMap = existScheduleList.stream().filter(s->s.getPublishSuccessCount()>0)
                .collect(Collectors.groupingBy(NcScheduleResultVo::getLiningCode)); //拿到重排前，已经有发布给MES的排产数据。key为 半部件规格代码

        for(NcScheduleResultVo autoSchedule : autoScheduleList) {
            List<NcScheduleResultVo> existScheduleGroupList = existScheduleMap.get(autoSchedule.getLiningCode());

            if(existScheduleGroupList != null && existScheduleGroupList.size() == 1) {
                //对应规格重排前已经发布，并且此规格重排前只有一条排程记录（只对应了一个机台）
                NcScheduleResultVo existSchedule = existScheduleGroupList.get(0);
                //重排前的数据如果已经发布过，在重新排程后仍有相应的生产需求，计划量按照重新自动排程的计划量安排；订单号需要和之前发布个mes的订单号一致
                autoSchedule.setOrderNo(existSchedule.getOrderNo());  //订单号
                autoSchedule.setPublishSuccessCount(existSchedule.getPublishSuccessCount());
                autoSchedule.setNewestPublishTime(existSchedule.getNewestPublishTime());
                autoSchedule.setIsRelease(ApsConstant.WAIT_RELEASING);  //发布状态修改
                autoSchedule.setMachineId(existSchedule.getMachineId());  //机台沿用重排前的机台
                mergeList.add(autoSchedule);
            } else if(existScheduleGroupList != null && existScheduleGroupList.size() > 1) {
                //对应规格重排前已经发布，并且此规格重排前只有多条排程记录（对应了多个机台）。那需要保留重排之前的排产，并且要把此规格重排后的各班的计划量，拼接到备注中
                String remarkTip = I18nUtil.getMessage("reschedule.double.spec.remark");
                remarkTip = StringUtils.format(remarkTip, stripZeros(autoSchedule.getDayPlanQty()), stripZeros(autoSchedule.getNightPlanQty()));
                for(NcScheduleResultVo existSchedule : existScheduleGroupList) {
                    existSchedule.setBatchNo(batchNo);
                    existSchedule.setRemark(remarkTip);
                    mergeList.add(existSchedule);
                }
            } else {
                //对应的规格，重排前没有找到相应记录
                mergeList.add(autoSchedule);
            }
            existScheduleMap.remove(autoSchedule.getLiningCode());
        }

        //重排前的已发布的规格如果没有在重排后的列表中，则需要把对应的规格也加入到最新的排程列表中
        for(List<NcScheduleResultVo> list : existScheduleMap.values()) {
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
     * @param batchNo      内衬批次号
     */
    private void createScheduleRecord(String scheduleDate, String cxBatchNo, String batchNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleDate", scheduleDate);
        params.put("cxBatchNo", cxBatchNo);
        params.put("batchNo", batchNo);
        params.put("userName", SecurityUtils.getUsername());  //用户名
        ncEngineMapper.createScheduleRecord(params);
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     */
    private void syncNcScheduleToLog(String scheduleDate) {
        ncEngineMapper.syncNcScheduleToLog(scheduleDate);
        ncEngineMapper.deleteNcSchedule(scheduleDate);
        ncEngineMapper.deleteNcAssistSchedule(scheduleDate);
    }

    /**
     * 生产线挑选(优先选择“定点机台”匹配上的机台，如果没有，在选择“口型板”的机台信息)
     *
     * @param scheduleVo
     * @param specifyMachineMap
     */
    private void chooseMachine(NcScheduleResultVo scheduleVo, Map<String, String> specifyMachineMap) {
        String liningCode = scheduleVo.getLiningCode();  //内衬代码
        String machineIds = specifyMachineMap.get(liningCode);
        scheduleVo.setMachineId(machineIds == null ? "" : machineIds);
    }

    /**
     * （新）计算并设置供成型库存供应时长（小时）。
     * 具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；
     *         预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）
     * @param scheduleVo
     * @param stockQty
     */
    private void newComputeSupplyTime(NcScheduleResultVo scheduleVo,  Double stockQty) {
        Double cxClass1Plan = (scheduleVo.getCxClass1Plan() == null ? 0D : scheduleVo.getCxClass1Plan());  //对应成型一班的计划量
        Double cxClass2Plan = (scheduleVo.getCxClass2Plan() == null ? 0D : scheduleVo.getCxClass2Plan());  //对应成型二班的计划量
        Double cxClass3Plan = (scheduleVo.getCxClass3Plan() == null ? 0D : scheduleVo.getCxClass3Plan());  //对应成型三班的计划量
        Double cxClass4Plan = (scheduleVo.getCxClass4Plan() == null ? 0D : scheduleVo.getCxClass4Plan());  //对应成型次日一班的计划量
        Double cxClass5Plan = (scheduleVo.getCxClass5Plan() == null ? 0D : scheduleVo.getCxClass5Plan());  //对应成型次日一班的计划量
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长前数据",
                logSplit("具体算法：从1班开始判断，预计库存-1班的计划大于等于0时，供应时长+8小时；预计库存-1班计划-2班计划大于等于0时，供应时长+16小时；预计库存-1班计划-2班计划-3班计划小于0，供应时长=16个小时+（((预计库存-1班计划-2班计划)/3班计划)*8）；以此类推到第5班",
                        "物料编号：" + scheduleVo.getLiningCode() + "，16点预计库存：" + stockQty + "，对应成型一班的计划量：" + cxClass1Plan + "，对应成型二班的计划量：" + cxClass2Plan + "，对应成型三班的计划量：" + cxClass3Plan + "，对应成型次日一班的计划量：" + cxClass4Plan + "，对应成型次日二班的计划量：" + cxClass5Plan));

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
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getLiningCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
    }

    /**
     * 根据对应成型每班计划量，计算库存供应时长
     * @param scheduleVo  排程实体
     * @param remnantStock 剩余库存
     * @param classPlan 对应成型的计划量
     * @return false：不需要再根据其他班在计算了。 true：还需要根据其他班计划量，继续计算库存供应时长
     */
    private boolean oneComputeSupplyTime(NcScheduleResultVo scheduleVo,Double remnantStock, Double classPlan) {
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
            autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长结束","物料编号：" + scheduleVo.getLiningCode() + "，库存供应时长=" + scheduleVo.getSupplyTime());
            return false;
        }
    }

    /**
     * 计算库存供应时长（小时）= 库存/(成型定额*单耗)*8小时
     * @param quotaKeys 成型机台code和胎胚代码，格式：成型机台code$胎胚代码
     * @param stockQty 16点预计库存
     * @param unitConsume 单耗
     */
    private void computeSupplyTime(NcScheduleResultVo scheduleVo, String quotaKeys, Double stockQty, Double unitConsume) {
        if(StringUtils.isBlank(quotaKeys)) {
            scheduleVo.setSupplyTime(0D);
            autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长", "库存供应时长为空，原因：没找到对应的成型排程记录");
            return;
        }
        String [] quotaKeyArray = quotaKeys.split(",");
        Integer cxQuota = cxEngineQuotaCommonService.getCxMachineQuota(quotaKeyArray);  //成型定额
        unitConsume = BigDecimalUtil.div(unitConsume, 1000);   //单耗把毫米转成米
        Double quota = BigDecimalUtil.mul(cxQuota, unitConsume);   //定额
        if(quota == 0) {
            scheduleVo.setSupplyTime(0D);;
        } else {
            Double supplyTime = stockQty / quota * 8;  //库存可供成型连续生产的时长
            supplyTime = BigDecimalUtil.add(supplyTime, addComputeSupplyTime(scheduleVo)); //如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
            scheduleVo.setSupplyTime(BigDecimalUtil.roundDown(supplyTime, 1)); //设置困存公用时长向下保留2位小数
        }
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算库存供应时长",
                logSplit("库存供应时长supplyTime（小时）= 库存/(成型定额*单耗)*8小时;其中成型定额取成型定额的平均值，单耗也是取平均单耗", "成型定额：" + cxQuota + "，半制品平均单耗：" + unitConsume,
                        "计算后的结果数据：" + toJSONString(scheduleVo)));
    }

    /**
     * 如果成型一班计划量为0，那么库存供应时长增加8小时；如果成型一班、二班计划量为0，那么供应时长增加16小时；以此类推
     * @param scheduleVo
     * @return
     */
    private int addComputeSupplyTime(NcScheduleResultVo scheduleVo) {
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
     * 计算内衬中班和夜班计划量
     * @param scheduleVo
     * @param totalPlanQtyMap 每个生产线的计划量汇总MAP
     * @param lossMap 耗损率map
     * @param paramLossRate 工序参数中配置的耗损率
     */
    private void computeNcPlanQty(NcScheduleResultVo scheduleVo, Map<String, NcTotalPlanQtyVo> totalPlanQtyMap, Map<String, Double> lossMap, double paramLossRate) {
        String oldScheduleResult = toJSONString(scheduleVo); //没看是计算前的排程数据json字符串（日志使用）
        Double stockQty = scheduleVo.getStockQty(); //库存
//        double unitConsume = BigDecimalUtil.div(scheduleVo.getUnitConsume(), 1000D, 3);
        //计算中班计划量 = （成型一班消耗胎面计划量 + 成型二班消耗胎面计划量 + 成型三班班消耗胎面计划量）
        double dayPlanQty = BigDecimalUtil.add(scheduleVo.getCxClass1Plan(), scheduleVo.getCxClass2Plan(), scheduleVo.getCxClass3Plan());
        double initDayPlanQty = dayPlanQty;
        //计算夜班计划量=（成型次日一班消耗胎面计划量 + 对应成型次二班的胎面胶计划量）
        double nightPlanQty = BigDecimalUtil.add(scheduleVo.getCxClass4Plan(), scheduleVo.getCxClass5Plan());

        //根据库存重新计算中班计划量：（原中班计划量>库存） ？（ 原中班计划量-库存） ： 0
        dayPlanQty = (initDayPlanQty > stockQty) ? BigDecimalUtil.sub(dayPlanQty, stockQty) : 0;
        //根据库存重新计算夜班计划量：（原中班计划量>库存） ？原夜班计划量 ： （原中班计划量+原夜班计划量 - 库存）
        nightPlanQty = (initDayPlanQty > stockQty) ? nightPlanQty : BigDecimalUtil.sub(BigDecimalUtil.add(initDayPlanQty, nightPlanQty), stockQty);
        nightPlanQty = (nightPlanQty < 0) ? 0D : nightPlanQty;

        String machineId = scheduleVo.getMachineId();  //机台id
        double lossRate = 0;
        //只有单个机台的时候，自动排程才计算耗损率
        if(StringUtils.isNotBlank(machineId) && !machineId.contains(",")) {
            //计划量要加上耗损量
            lossRate = ncEngineLossService.getLossRate(scheduleVo.getLiningCode(), scheduleVo.getMachineId(), lossMap, paramLossRate);
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, BigDecimalUtil.mul(dayPlanQty, lossRate));
            nightPlanQty = BigDecimalUtil.add(nightPlanQty, BigDecimalUtil.mul(nightPlanQty, lossRate));
        }

        //如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）
        if (dayPlanQty > 0) {
            dayPlanQty = BigDecimalUtil.add(dayPlanQty, nightPlanQty);
            nightPlanQty = 0D;
        }

        //计划量向上取整
        dayPlanQty = BigDecimalUtil.roundUp(dayPlanQty, 0);
        nightPlanQty = BigDecimalUtil.roundUp(nightPlanQty, 0);
        scheduleVo.setDayPlanQty(dayPlanQty);
        scheduleVo.setNightPlanQty(nightPlanQty);
        scheduleVo.setCloseOutSpecFlag(ApsConstant.STATUS_DISABLE);

        //计算中班总计划量 和 夜班总计划量
        this.groupTotalPlanQtyMap(scheduleVo, totalPlanQtyMap);
        this.computeNcPlanQtyLog(oldScheduleResult, scheduleVo, lossMap, paramLossRate, lossRate);  //添加日志
    }

    private void computeNcPlanQtyLog(String oldScheduleResult, NcScheduleResultVo scheduleVo, Map<String, Double> lossMap, double paramLossRate, double lossRate) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("开始计算中班和夜班计划量").append(division);
        logDetail.append("计算前排程数据：" + oldScheduleResult).append(division);
        logDetail.append("计算中班计划量dayPlanQty = （成型一班消耗内衬计划量cxClass1Plan + 成型二班消耗内衬计划量CxClass2Plan）").append(division);
        logDetail.append("计算夜班计划量nightPlanQty =（成型三班消耗内衬计划量cxClass3Plan + 成型次日一班消耗内衬计划量cxClass4Plan）").append(division);
        logDetail.append("根据库存重新计算中班计划量dayPlanQty：（原中班计划量dayPlanQty > 库存stockQty） ？（ 原中班计划量-库存） ： 0").append(division);
        logDetail.append("根据库存重新计算夜班计划量nightPlanQty：（原中班计划量dayPlanQty>库存stockQty） ？原夜班计划量nightPlanQty ： （原中班计划量dayPlanQty + 原夜班计划量nightPlanQty - 库存stockQty）").append(division);
        logDetail.append("内衬耗损率集合：" + toJSONString(lossMap) + "  参数配置耗损率：" + paramLossRate).append(division);
        logDetail.append("获得损耗率（从损耗率表获取对应的损耗率，获取顺序：机台+物料编号 > 内衬代码 > 机台 >工序参数配置），耗损率：" + lossRate).append(division);
        logDetail.append("重新计算中班计划量和夜班计划量(计划量 = 计划量 + 计划量 * 耗损率)，计划量要加上耗损率的损耗数").append(division);
        logDetail.append("如果中班计划量>0，那么中班计划量=中班计划量+夜班计划量，夜班计划量=0（为了让相同的胶在同一个班生产，而且又不能延误生产）").append(division);
        logDetail.append("计划量计算好后的排程数据：" + toJSONString(scheduleVo));
        autoScheduleLogService.insertNcScheduleLog(scheduleVo.getBatchNo(), scheduleVo.getOrderNo(), "计算各班计划量", logDetail.toString());
    }

    /**
     * 创建批次号
     * @param scheduleDate
     * @return
     */
    private String createBatchNo(String scheduleDate) {
        scheduleDate = scheduleDate.replace("-", "");
        return incrementService.getSequence3(EngineConstants.NC_BATCH_NO_PREFIX + scheduleDate);
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
        List<NcParamsVo> list = this.ncEngineMapper.listNcParams();
        Map<String, String> map = list.stream().collect(Collectors.toMap(NcParamsVo::getParamCode, NcParamsVo::getParamValue));
        return map == null ? new HashMap<>() : map;
    }

    /**
     * 自动排程基础表的数据日志
     * @param batchNo 自动排程批次号
     * @param glueSeqMap 胶料顺序集合
     * @param specifyMachineMap 定点机台和机台的限制作业集合
     * @param planStockMap 16点预计库存集合
     * @param lossRateMap 耗损率集合
     * @param monthSurplus 月度计划剩余量、完成量集合
     * @param paramsMap 参数设置集合
     */
    private void baseDataLog(String batchNo, Map<String, String> glueSeqMap, Map<String, String> specifyMachineMap, Map<String, Double> planStockMap, Map<String, Double> lossRateMap,
                             Map<String, NcMonthSurplusVo> monthSurplus,Map<String, String> paramsMap) {
        StringBuffer logDetail = new StringBuffer("");
        logDetail.append("胶料顺序集合：" + toJSONString(glueSeqMap)).append(division);
        logDetail.append("定点机台和机台的限制作业集合：" + toJSONString(specifyMachineMap)).append(division);
        logDetail.append("16点预计库存集合：" + toJSONString(planStockMap)).append(division);
        logDetail.append("耗损率集合：" + toJSONString(lossRateMap)).append(division);
        logDetail.append("月度计划剩余量、完成量集合：" + toJSONString(monthSurplus)).append(division);
        logDetail.append("参数设置集合：" + toJSONString(paramsMap)).append(division);
        autoScheduleLogService.insertNcScheduleLog(batchNo, "", "自动排程基础表的数据日志", logDetail.toString());
    }
}