package com.zlt.aps.cd15.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15DayFinishQty;
import com.zlt.aps.cd15.api.domain.entity.Cd15DispatcherLog;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.service.Cd15EngineProductOrderService;
import com.zlt.aps.cd15.engine.service.Cd15EngineService;
import com.zlt.aps.cd15.entity.Cd15Params;
import com.zlt.aps.cd15.mapper.Cd15CurlLengthEntityMapper;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultMapper;
import com.zlt.aps.cd15.service.Cd15DispatcherLogService;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import com.zlt.aps.cd15.service.Cd15ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.service.impl.BaseFinishQtyImportService;


/**
 * 15度裁断排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-07-05
 */
@Service
public class Cd15ScheduleResultServiceImpl implements Cd15ScheduleResultService {

    @Autowired
    private Cd15ScheduleResultMapper cd15ScheduleResultMapper;

    @Autowired
    private Cd15MachineInfoService machineInfoService;

    @Autowired
    private Cd15EngineService cd15EngineService;

    @Autowired
    private Cd15EngineProductOrderService cd15EngineProductOrderService;

    @Autowired
    private FactoryService factoryService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private Cd15DispatcherLogService cd15DispatcherLogService;

    @Autowired
    private Cd15CurlLengthEntityMapper curlRollMapper;

    /**
     * 默认标准长度
     */
    private static final String DEFAULT_CRIMP_LENGTH = "190";
    @Autowired
    private Cd15ParamsMapper paramsMapper;

    /**
     * 查询15度裁断排程结果
     *
     * @param id 15度裁断排程结果ID
     * @return 15度裁断排程结果
     */
    @Override
    public Cd15ScheduleResult selectCd15ScheduleResultById(Long id) {
        return cd15ScheduleResultMapper.selectCd15ScheduleResultById(id);
    }

    @Override
    public List<Cd15ScheduleResult> selectCd15ScheduleResultByIds(List<Long> ids) {

        return cd15ScheduleResultMapper.selectCd15ScheduleResultByIds(ids);
    }

    /**
     * 赋值成型消耗量(成型昨日早班消耗量+成型夜班消耗量)、卷曲长度、交接班库存、计划量对应卷数
     *
     * @param scheduleResult 排程结果
     * @param cxConsume1Map  成型消耗量map
     * @param curlRollMap    卷曲长度map
     */
    private static void setLastDayAndCalculate(Cd15ScheduleResult scheduleResult,
                                               Map<String, Double> cxConsume1Map,
                                               Map<String, Double> cxConsume2Map,
                                               Map<String, BigDecimal> curlRollMap,
                                               BigDecimal standardLength) {
        String steelStripCode1 = scheduleResult.getSteelStripCode1();
        String steelStripCode2 = scheduleResult.getSteelStripCode2();
        if (cxConsume1Map.containsKey(steelStripCode1)) {
            Double cxConsumeQty = cxConsume1Map.get(steelStripCode1);
            scheduleResult.setCxConsumeQty(cxConsumeQty);
        }
        if (StringUtils.isBlank(steelStripCode2) && cxConsume2Map.containsKey(steelStripCode2)) {
            Double cxConsumeQty = cxConsume2Map.get(steelStripCode2);
            scheduleResult.setCxConsumeQty(cxConsumeQty);
        }
        if (curlRollMap.containsKey(steelStripCode1)) {
            BigDecimal curlRollLength = curlRollMap.get(steelStripCode1);
            scheduleResult.setCurlLength(curlRollLength.doubleValue());
        }
        if (StringUtils.isBlank(steelStripCode1) && curlRollMap.containsKey(steelStripCode2)) {
            BigDecimal curlRollLength = curlRollMap.get(steelStripCode2);
            scheduleResult.setCurlLength(curlRollLength.doubleValue());
        }
        if (scheduleResult.getCurlLength() == null) {
            scheduleResult.setCurlLength(standardLength.doubleValue());
        }
        ReflectUtils.invokeMethodByName(scheduleResult, "calculateTheoreticClassLastDayPlanQty", new Object[]{});
        // 执行计算卷数方法
        ReflectUtils.invokeMethodByName(scheduleResult, "calculatePlanQty", new Object[]{});
    }

    /**
     * 查询15度裁断排程结果列表
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 15度裁断排程结果
     */
    @Override
    public List<Cd15ScheduleResult> selectCd15ScheduleResultList(Cd15ScheduleResult cd15ScheduleResult) {
        List<Cd15ScheduleResult> list = cd15ScheduleResultMapper.selectCd15ScheduleResultList(cd15ScheduleResult);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<Cd15MachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(new Cd15MachineInfo());
        Map<Long, Cd15MachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getId, Function.identity(), (s1, s2) -> s1));
        if (CollectionUtils.isNotEmpty(list)) {

            List<String> code1List = list.stream().map(Cd15ScheduleResult::getSteelStripCode1).collect(Collectors.toList());
            List<String> code2List = list.stream().map(Cd15ScheduleResult::getSteelStripCode1).collect(Collectors.toList());
            cd15ScheduleResult.getParams().put("code1List", code1List);
            Map<String, Double> cxConsume1Map = new HashMap<>(16);
            List<Cd15ScheduleResult> cxConsume1List = cd15ScheduleResultMapper.getCxConsume4List(cd15ScheduleResult);
            if (CollectionUtils.isNotEmpty(cxConsume1List)) {
                cxConsume1Map = cxConsume1List.stream().collect(Collectors.toMap(Cd15ScheduleResult::getSteelStripCode1, Cd15ScheduleResult::getCxConsumeQty));
            }
            cd15ScheduleResult.getParams().remove("code1List");
            cd15ScheduleResult.getParams().put("code2List", code1List);
            Map<String, Double> cxConsume2Map = new HashMap<>(16);
            List<Cd15ScheduleResult> cxConsume2List = cd15ScheduleResultMapper.getCxConsume4List(cd15ScheduleResult);
            cd15ScheduleResult.getParams().remove("code2List");
            if (CollectionUtils.isNotEmpty(cxConsume2List)) {
                cxConsume2Map = cxConsume2List.stream().collect(Collectors.toMap(Cd15ScheduleResult::getSteelStripCode2, Cd15ScheduleResult::getCxConsumeQty));
            }
            LambdaQueryWrapper<Cd15CurlLength> curlWrapper = new LambdaQueryWrapper<>();
            code1List.addAll(code2List);
            curlWrapper.in(Cd15CurlLength::getSteelStripCode, code1List);
            List<Cd15CurlLength> curlRollList = curlRollMapper.selectList(curlWrapper);
            Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(curlRollList)) {
                curlRollMap = curlRollList.stream().collect(Collectors.toMap(Cd15CurlLength::getSteelStripCode, Cd15CurlLength::getCurlLength));
            }
            LambdaQueryWrapper<Cd15Params> paramWrapper = new LambdaQueryWrapper<>();
            paramWrapper.eq(Cd15Params::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
            Cd15Params standardLength = paramsMapper.selectOne(paramWrapper);

            for (Cd15ScheduleResult scheduleResult : list) {
                String machineIdStr = scheduleResult.getMachineId();
                if (StringUtils.isNotBlank(machineIdStr)) {
                    List<String> machineNameList = new ArrayList<>();
                    String[] machineIdArr = machineIdStr.split(",");
                    for (String machineId : machineIdArr) {
                        Long key = null;
                        try {
                            key = Long.valueOf(machineId);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            continue;
                        }
                        if (machineInfoMap.containsKey(key)) {
                            Cd15MachineInfo machineInfo = machineInfoMap.get(key);
                            machineNameList.add(machineInfo.getMachineName());
                        }
                    }
                    scheduleResult.setMachineName(String.join(",", machineNameList));
                }
                // 赋值卷曲长度、计算计划量对应卷数
                setLastDayAndCalculate(scheduleResult, cxConsume1Map, cxConsume2Map, curlRollMap,
                        standardLength == null ? new BigDecimal(DEFAULT_CRIMP_LENGTH) : new BigDecimal(standardLength.getParamValue()));
            }
        }
        return list;
    }

    /**
     * 新增15度裁断排程结果
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 结果
     */
    @Override
    public int insertCd15ScheduleResult(Cd15ScheduleResult cd15ScheduleResult) {
        cd15ScheduleResult.setBaseVale(null);
        // 调用引擎插单接口
        return cd15EngineService.insertCd15Order(cd15ScheduleResult);
    }

    /**
     * 修改15度裁断排程结果
     *
     * @param scheduleResult 15度裁断排程结果
     * @return 结果
     */
    @Override
    public int updateCd15ScheduleResult(Cd15ScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            Cd15ScheduleResult scheduleResult2 = cd15ScheduleResultMapper.selectCd15ScheduleResultById(scheduleResult.getId());
            boolean flag = compare(scheduleResult2.getMachineId(), scheduleResult.getMachineId());
            flag = flag && compare(scheduleResult2.getDayPlanQty1(), scheduleResult.getDayPlanQty1());
            flag = flag && compare(scheduleResult2.getNightPlanQty1(), scheduleResult.getNightPlanQty1());
            flag = flag && compare(scheduleResult2.getDayProduceOrder1(), scheduleResult.getDayProduceOrder1());
            flag = flag && compare(scheduleResult2.getNightProduceOrder1(), scheduleResult.getNightProduceOrder1());
            flag = flag && compare(scheduleResult2.getDayHandAnalysis1(), scheduleResult.getDayHandAnalysis1());
            flag = flag && compare(scheduleResult2.getNightHandAnalysis1(), scheduleResult.getNightHandAnalysis1());
            flag = flag && compare(scheduleResult2.getRemark(), scheduleResult.getRemark());
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
        }
        return cd15ScheduleResultMapper.updateCd15ScheduleResult(scheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, Cd15ScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        Cd15ScheduleResult oldSchedule = this.cd15ScheduleResultMapper.selectCd15ScheduleResultById(newSchedule.getId());  //操作前的排程数据
        Cd15DispatcherLog log = new Cd15DispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getSteelStripCode1());    //1#钢带代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty1());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty1());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty1());
        log.setAfterNightPlan(newSchedule.getNightPlanQty1());
        /** 调用插入日志方法  **/
        cd15DispatcherLogService.insertCd15DispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<Cd15ScheduleResult> scheduleResults, Cd15ScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<Cd15ScheduleResult> cd15ScheduleResults = this.selectByScheduleDateAndBigRollCode(newSchedule);
        Cd15DispatcherLog log = new Cd15DispatcherLog();
        //基础信息赋值
        log.setScheduleId(cd15ScheduleResults.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getSteelStripCode1());    //1#钢带代码
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<Cd15ScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(Cd15ScheduleResult::getCreateTime));
            if (max.isPresent()) {
                Cd15ScheduleResult cd15ScheduleResult = max.get();
                log.setBeforeMachineId(cd15ScheduleResult.getMachineId());
                log.setBeforeDayPlan(cd15ScheduleResult.getDayPlanQty1());
                log.setBeforeNightPlan(cd15ScheduleResult.getNightPlanQty1());
            }
        }
        // 操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty1());
        log.setAfterNightPlan(newSchedule.getNightPlanQty1());
        // 调用插入日志方法
        cd15DispatcherLogService.insertCd15DispatcherLog(log);
    }

    /**
     * 修改15度裁断排程结果机台
     *
     * @param scheduleResult 15度裁断排程结果
     * @return 结果
     */
    @Override
    public void chooseMachine(Cd15ScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        // 调用引擎确认机台接口
        cd15EngineService.confirmCd15Machine(scheduleResult);
        updateCd15ScheduleResult(scheduleResult);
        // 调用引擎生产顺序重算接口
        cd15EngineProductOrderService.recalculateProduceOrder(scheduleResult);
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }

    public boolean compare(Long l1, Long l2) {
        return (l1 == null ? l2 == null : l1.equals(l2));
    }

    /**
     * 批量删除15度裁断排程结果
     *
     * @param ids 需要删除的15度裁断排程结果ID
     * @return 结果
     */
    @Override
    public int deleteCd15ScheduleResultByIds(Long[] ids) {
        return cd15ScheduleResultMapper.deleteCd15ScheduleResultByIds(ids);
    }

    /**
     * 删除15度裁断排程结果信息
     *
     * @param id 15度裁断排程结果ID
     * @return 结果
     */
    @Override
    public int deleteCd15ScheduleResultById(Long id) {
        return cd15ScheduleResultMapper.deleteCd15ScheduleResultById(id);
    }


    @Override
    public int batchUpdate(long[] ids, Date scheduleDate, String dataVersion) {
        //把排程数据发布到中间库，并通知MES
        this.deployScheduleToMid(ids, scheduleDate, dataVersion);
        // 保存发布记录日志
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD15);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        cd15ScheduleResultMapper.insertPublishRecord(record);
        return cd15ScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), ApsConstant.RELEASING);
    }

    /**
     * 发布排程数据到中间库,并通知 MES
     * @param ids 发布的排程记录id
     * @param scheduleDate 排产日期
     * @param dataVersion 数据同步版本
     */
	private void deployScheduleToMid(long[] ids, Date scheduleDate, String dataVersion) {
		if (ids == null) {
			return;
		}
		// 厂别、分公司编号
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		// 把排程数据同步到接口中间库中
		cd15ScheduleResultMapper.deployCd15ScheduleToMid(dataVersion, ids, factoryCode, companyCode,
				DateUtils.getNowDate());
	}

	/**
	 * 给mes发送排程下发通知
	 *
	 * @param scheduleDate 排产日
	 * @param dataVersion  数据版本
	 */
	public void publishNoticeMes(Date scheduleDate, String dataVersion, int rowCount) {
		// 调整为itf接口
//        // 厂别、分公司编号
//        String factoryCode = factoryService.getFactoryCode();
//        String companyCode = factoryService.getCompanyCode();
//		//数据同步到中间库后，往 mq中发送消息通知 MES去取数据
//        SyncParamsVO syncParamsVO = new SyncParamsVO();
//        syncParamsVO.setSyncKey(ApsConstant.CD15_DEPLOY_SYNC_KEY);
//        syncParamsVO.setDataVersion(dataVersion);
//        // 请求参数
//        JSONObject params = new JSONObject();
//        params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
//		params.put("rowCount", rowCount);
//        syncParamsVO.setParams(params);
//        syncParamsVO.setDockSys(ApsConstant.DOCK_SYS_MES);
//        syncParamsVO.setFactoryCode(factoryCode);
//        syncParamsVO.setCompanyCode(companyCode);
//        //往消息队列发送消息
//        cd15SyncDataHandle.syncNotice(syncParamsVO);
	}

	/**
	 * 更新指定相关数据记录的发布状态
	 *
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
    @Override
    public void updateRelaseStatus(String dataVersion, long[] ids, String status) {
        cd15ScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), status);
        cd15ScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    @Override
    public List<Cd15ScheduleResult> checkScheduleResultUnique(Cd15ScheduleResult cd15ScheduleResult) {
        return cd15ScheduleResultMapper.checkScheduleResultUnique(cd15ScheduleResult);
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD15);
        record.setScheduleDate(scheduleDate);
        return cd15ScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<Cd15ScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd15ScheduleResult> importList = new ArrayList<>();
        Cd15MachineInfo cd15MachineInfo=new Cd15MachineInfo();
        cd15MachineInfo.setStatus("0");
        List<Cd15MachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(cd15MachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        //根据机台名称去重
        TreeSet<Cd15MachineInfo> treeSet = new TreeSet<Cd15MachineInfo>(new Comparator<Cd15MachineInfo>() {
            @Override
            public int compare(Cd15MachineInfo o1, Cd15MachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);

        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd15MachineInfo::getMachineName, Cd15MachineInfo::getId));
        Map<String, String> machineIsOutTwoMap = machineInfoList.stream().filter(a-> Objects.nonNull(a.getIsOutTwo())).collect(Collectors.toMap(Cd15MachineInfo::getMachineName, Cd15MachineInfo::getIsOutTwo));

        //按业务主键分组
        Map<String, Long> groupMap =list.stream().collect(Collectors.groupingBy(a-> (a.getSteelStripCode1()+a.getMachineId()),Collectors.counting()));

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            Cd15ScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd",scheduleDate));

            //重复记录校验
            Long hasValue=groupMap.get(entity.getSteelStripCode1()+entity.getMachineId());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cd15ScheduleResult.steelStripCode1");
                String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 3,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 3, entity);
            // 机台code 转为机台id
            if(entity.getMachineId()!=null && entity.getMachineId().indexOf(",")>0){
                String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                message=String.format(message, i + 3, I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                addImportErrorLog(importLogId, i + 3,message, validated);
            }
            if (machineCodeMap.get(entity.getMachineId())==null) {
                addImportErrorLog(importLogId, i + 3,
                        I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
            }

            // 如果不支持一出二机台，1#钢带和2#钢带不能同时为空
            boolean isOutTwo = ApsConstant.APS_STRING_1.equals(machineIsOutTwoMap.get(entity.getMachineId()));
            if (isOutTwo && StringUtils.isAllBlank(entity.getSteelStripCode1(), entity.getSteelStripCode2())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.cd15ScheduleResult.code1AndCode2CanNotAllNull");
                addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                continue;
            }

            // 如果支持一出二机台，1#钢带和2#钢带不能为空
            if (!isOutTwo && StringUtils.isAnyBlank(entity.getSteelStripCode1(), entity.getSteelStripCode2())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.cd15ScheduleResult.code1AndCode2CanNotNull");
                addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                continue;
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                entity.setMachineId(machineCodeMap.get(entity.getMachineId())+"");
                successNum++;
                entity.setBaseVale(null);
                entity.setDataSource(EngineConstants.SCHEDULE_DATA_SOURCE_IMPORT);
                importList.add(entity);
            }
        }

		// 把验证成功的记录进行导入 importList
		if (!importList.isEmpty()) {
			// 如果引擎导入失败，会将失败日志返回
			List<ImportErrorLog> engineImportErrorLogs = cd15EngineService
					.batchSaveCd15Schedule(DateUtils.dateTime("yyyy-MM-dd", scheduleDate), importList);
			// 如果有记录导入失败，则需要合并失败日志
			if (!engineImportErrorLogs.isEmpty()) {
				engineImportErrorLogs.stream().forEach(v -> v.setImportLogId(importLogId));
				importErrorLogs.addAll(engineImportErrorLogs);
				successNum -= engineImportErrorLogs.size();
				failureNum += engineImportErrorLogs.size();
			}
		}

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return cd15ScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return cd15ScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(Cd15ScheduleResult entity) {
        return cd15ScheduleResultMapper.changeReleaseStatus(entity);
    }

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     * @return 修改行数
     */
    @Override
    public int combinationMiddleAndNight(Long[] ids, String classifiedShift) {
        Map<String, Object> map = new HashMap<>();
        map.put("classifiedShift", classifiedShift);
        map.put("ids", ids);
        return cd15ScheduleResultMapper.combinationMiddleAndNight(map);
    }

    @Override
    public int checkCd15CodeExist(Cd15ScheduleResult cd15ScheduleResult) {
        return cd15ScheduleResultMapper.checkCd15CodeExist(cd15ScheduleResult);
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return cd15ScheduleResultMapper.isPublishByIds(ids);
    }

    /**
     * 根据排程日期和钢带代码查询排程结果
     * @param cd15ScheduleResult 排程日期、钢带代码
     * @return 查询到的数据
     */
    @Override
    public List<Cd15ScheduleResult> selectByScheduleDateAndBigRollCode(Cd15ScheduleResult cd15ScheduleResult) {
        return cd15ScheduleResultMapper.selectByScheduleDateAndBigRollCode(cd15ScheduleResult);
    }

    @Autowired
    private BaseFinishQtyImportService baseFinishQtyImportService;

    /**
     * 导入数据，并保存记录
     *
     * @param list        要导入数据
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importFinishQty(List<Cd15DayFinishQty> list, Long importLogId) {
        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.CD15);
    }

    /**
     * 查询出对应的施工信息字段
     *
     * @param embryoCodeList  施工代码
     * @param productionStage 仅投产阶段规格排产标识
     * @return 结果
     */
    @Override
    public List<EngineConstructionInfo> listConstruction(List<String> embryoCodeList, String productionStage) {
        return cd15ScheduleResultMapper.listConstruction(embryoCodeList, productionStage);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(Cd15ScheduleResult scheduleResult) {
        List<Cd15ScheduleResult> scheduleResultList = this.selectCd15ScheduleResultList(scheduleResult);
        scheduleResult.setParams(new HashMap<>(16));
        List<Cd15ScheduleResult> lastDayPlanQty4List1 = cd15ScheduleResultMapper.getLastDayPlanQty4List1(scheduleResult);
        List<Cd15ScheduleResult> lastDayPlanQty4List2 = cd15ScheduleResultMapper.getLastDayPlanQty4List2(scheduleResult);
        // 添加昨日排程有，今日排程没有的物料对象，用于后续计算理论交接班库存合计
        List<String> resultCode1List = scheduleResultList.stream().map(Cd15ScheduleResult::getSteelStripCode1).collect(Collectors.toList());
        List<String> resultCode2List = scheduleResultList.stream().map(Cd15ScheduleResult::getSteelStripCode2).collect(Collectors.toList());
        List<String> notExistCode1List = lastDayPlanQty4List1.stream().map(Cd15ScheduleResult::getSteelStripCode1)
                .filter(item -> !resultCode1List.contains(item)).collect(Collectors.toList());
        List<String> notExistCode2List = lastDayPlanQty4List1.stream().map(Cd15ScheduleResult::getSteelStripCode2)
                .filter(item -> !resultCode2List.contains(item)).collect(Collectors.toList());

        List<String> notExistCodeList = new ArrayList<>();
        notExistCodeList.addAll(notExistCode1List);
        notExistCodeList.addAll(notExistCode2List);
        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(notExistCodeList)) {
            LambdaQueryWrapper<Cd15CurlLength> lengthParamWrapper = new LambdaQueryWrapper<>();
            lengthParamWrapper.in(Cd15CurlLength::getSteelStripCode, notExistCodeList);
            List<Cd15CurlLength> curlRollList = curlRollMapper.selectList(lengthParamWrapper);
            if (CollectionUtils.isNotEmpty(curlRollList)) {
                curlRollMap = curlRollList.stream().collect(Collectors.toMap(Cd15CurlLength::getSteelStripCode, Cd15CurlLength::getCurlLength));
            }
        }

        LambdaQueryWrapper<Cd15Params> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(Cd15Params::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
        Cd15Params standardLengthParams = paramsMapper.selectOne(paramWrapper);

        for (String code : notExistCode1List) {
            Cd15ScheduleResult result = new Cd15ScheduleResult();
            result.setSteelStripCode1(code);
            BigDecimal standardLength = standardLengthParams == null ? new BigDecimal(DEFAULT_CRIMP_LENGTH) : new BigDecimal(standardLengthParams.getParamValue());
            result.setCurlLength(curlRollMap.getOrDefault(code, standardLength).doubleValue());
            scheduleResultList.add(result);
        }
        for (String code : notExistCode2List) {
            Cd15ScheduleResult result = new Cd15ScheduleResult();
            result.setSteelStripCode2(code);
            BigDecimal standardLength = standardLengthParams == null ? new BigDecimal(DEFAULT_CRIMP_LENGTH) : new BigDecimal(standardLengthParams.getParamValue());
            result.setCurlLength(curlRollMap.getOrDefault(code, standardLength).doubleValue());
            scheduleResultList.add(result);
        }
        Map<String, Cd15ScheduleResult> lastDayPlan1Map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lastDayPlanQty4List1)) {
            lastDayPlan1Map = lastDayPlanQty4List1.stream().collect(Collectors.toMap(Cd15ScheduleResult::getSteelStripCode1, Function.identity()));
        }
        Map<String, Cd15ScheduleResult> lastDayPlan2Map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lastDayPlanQty4List2)) {
            lastDayPlan2Map = lastDayPlanQty4List2.stream().collect(Collectors.toMap(Cd15ScheduleResult::getSteelStripCode2, Function.identity()));
        }
        scheduleResult.getParams().put("code1List", resultCode1List);
        List<Cd15ScheduleResult> cxConsume4List1 = cd15ScheduleResultMapper.getCxConsume4List(scheduleResult);
        Map<String, Cd15ScheduleResult> cxConsume1Map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(cxConsume4List1)) {
            cxConsume1Map = cxConsume4List1.stream().collect(Collectors.toMap(Cd15ScheduleResult::getSteelStripCode1, Function.identity()));
        }
        scheduleResult.getParams().remove("code1List");
        scheduleResult.getParams().put("code2List", resultCode2List);
        List<Cd15ScheduleResult> cxConsume4List2 = cd15ScheduleResultMapper.getCxConsume4List(scheduleResult);
        Map<String, Cd15ScheduleResult> cxConsume2Map = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(cxConsume4List2)) {
            cxConsume2Map = cxConsume4List2.stream().collect(Collectors.toMap(Cd15ScheduleResult::getSteelStripCode2, Function.identity()));
        }
        BigDecimal totalDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalNightPlanQty = BigDecimal.ZERO;
        BigDecimal totalNightPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalStockQty = BigDecimal.ZERO;
        BigDecimal totalStockQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalStockQty2 = BigDecimal.ZERO;
        BigDecimal totalStockQty2RollNum = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQty = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalNextDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalNextDayPlanQtyRollNum = BigDecimal.ZERO;

        for (Cd15ScheduleResult result : scheduleResultList) {
            Double nightPlanQty = ObjectUtils.defaultIfNull(result.getNightPlanQty1(), 0D);
            Double stockQty = ObjectUtils.defaultIfNull(result.getStock1Qty1(), 0D);
            String code1 = result.getSteelStripCode1();
            String code2 = result.getSteelStripCode2();
            if (lastDayPlan1Map.containsKey(code1)) {
                Cd15ScheduleResult lastDayResult = lastDayPlan1Map.get(code1);
                result.setLastMidPlanQty1(lastDayResult.getLastMidPlanQty1());
                result.setLastMidPlanQtyRollNum1(lastDayResult.getLastMidPlanQtyRollNum1());
            }
            if (StringUtils.isBlank(code1) && lastDayPlan2Map.containsKey(code2)) {
                Cd15ScheduleResult lastDayResult = lastDayPlan2Map.get(code2);
                result.setLastMidPlanQty2(lastDayResult.getLastMidPlanQty2());
                result.setLastMidPlanQtyRollNum2(lastDayResult.getLastMidPlanQtyRollNum2());
            }
            if (cxConsume1Map.containsKey(code1)) {
                Cd15ScheduleResult cxConsumeResult = cxConsume1Map.get(code1);
                result.setCxConsumeQty(cxConsumeResult.getCxConsumeQty());
            }
            if (StringUtils.isBlank(code1) && cxConsume2Map.containsKey(code2)) {
                Cd15ScheduleResult cxConsumeResult = cxConsume2Map.get(code2);
                result.setCxConsumeQty(cxConsumeResult.getCxConsumeQty());
            }
            Double lastMidPlanQty = result.getLastMidPlanQty1();
            Double lastMidPlanQty2 = result.getLastMidPlanQty2();
            Double cxConsumeQty = result.getCxConsumeQty();
            // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 成型消耗量
            if (lastMidPlanQty != null && cxConsumeQty != null) {
                result.setTheoreticClassStockQty1(stockQty + lastMidPlanQty + nightPlanQty - cxConsumeQty);
            }
            if (StringUtils.isBlank(code1) && lastMidPlanQty2 != null && cxConsumeQty != null) {
                result.setTheoreticClassStockQty2(stockQty + lastMidPlanQty2 + nightPlanQty - cxConsumeQty);
            }
            result.calculatePlanQty();

            totalDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQty1(), 0D), totalDayPlanQty);
            totalDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQtyRollNum(), 0D), totalDayPlanQtyRollNum);
            totalNightPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQty1(), 0D), totalNightPlanQty);
            totalNightPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQtyRollNum(), 0D), totalNightPlanQtyRollNum);
            totalNextDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQty(), 0D), totalNightPlanQty);
            totalNextDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQtyRollNum(), 0D), totalNightPlanQtyRollNum);

            totalStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStock1Qty1(), 0D), totalStockQty);
            totalStockQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStockQty1RollNum(), 0D), totalStockQtyRollNum);
            totalStockQty2 = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStock1Qty2(), 0D), totalStockQty2);
            totalStockQty2RollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStockQty2RollNum(), 0D), totalStockQtyRollNum);
            // 昨日早班
            totalLastDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQty1(), 0D), totalLastDayPlanQty);
            totalLastDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQtyRollNum1(), 0D), totalLastDayPlanQtyRollNum);
            totalLastDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQty2(), 0D), totalLastDayPlanQty);
            totalLastDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQtyRollNum2(), 0D), totalLastDayPlanQtyRollNum);
            // 交接班库存
            totalTheoreticClassStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getTheoreticClassStockQty1(), 0D), totalTheoreticClassStockQty);
            totalTheoreticClassStockQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getTheoreticClassStockQtyRollNum1(), 0D), totalTheoreticClassStockQtyRollNum);
            totalTheoreticClassStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getTheoreticClassStockQty2(), 0D), totalTheoreticClassStockQty);
            totalTheoreticClassStockQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getTheoreticClassStockQtyRollNum2(), 0D), totalTheoreticClassStockQtyRollNum);

            if (StringUtils.isNotBlank(code1) && StringUtils.isNotBlank(code2)) {
                totalDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQty1(), 0D), totalDayPlanQty);
                totalDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQtyRollNum(), 0D), totalDayPlanQtyRollNum);
                totalNightPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQty1(), 0D), totalNightPlanQty);
                totalNightPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQtyRollNum(), 0D), totalNightPlanQtyRollNum);
                totalNextDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQty(), 0D), totalNightPlanQty);
                totalNextDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQtyRollNum(), 0D), totalNightPlanQtyRollNum);

                totalStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStock1Qty2(), 0D), totalStockQty);
                totalStockQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStockQty2RollNum(), 0D), totalStockQtyRollNum);
            }
        }
        ScheduleSummaryVo scheduleSummaryVo = new ScheduleSummaryVo();
        scheduleSummaryVo.setDayPlanQty(totalDayPlanQty.doubleValue());
        scheduleSummaryVo.setDayPlanQtyRollNum(totalDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setNightPlanQty(totalNightPlanQty.doubleValue());
        scheduleSummaryVo.setNightPlanQtyRollNum(totalNightPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setNextDayPlanQty(totalNextDayPlanQty.doubleValue());
        scheduleSummaryVo.setNextDayPlanQtyRollNum(totalNextDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setStockQty(totalStockQty.doubleValue());
        scheduleSummaryVo.setStockQty2(totalStockQty2.doubleValue());
        scheduleSummaryVo.setStockQtyRollNum(totalStockQtyRollNum.doubleValue());
        scheduleSummaryVo.setStockQty2RollNum(totalStockQty2RollNum.doubleValue());
        scheduleSummaryVo.setLastDayPlanQty(totalLastDayPlanQty.doubleValue());
        scheduleSummaryVo.setLastDayPlanQtyRollNum(totalLastDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQty(totalTheoreticClassStockQty.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQtyRollNum(totalTheoreticClassStockQtyRollNum.doubleValue());
        /*
        ScheduleSummaryVo summaryVo = cd15ScheduleResultMapper.getSummaryVo(scheduleResult);
        if (summaryVo == null) {
            summaryVo = new ScheduleSummaryVo();
            summaryVo.setScheduleDate(scheduleResult.getScheduleDate());
        }
        ScheduleSummaryVo lastDayPlanQtySummaryVo = cd15ScheduleResultMapper.getLastDayPlanQty(scheduleResult);
        Double lastDayPlanQty = null;
        if (lastDayPlanQtySummaryVo != null) {
            lastDayPlanQty = lastDayPlanQtySummaryVo.getNightPlanQty();
            summaryVo.setLastDayPlanQty(lastDayPlanQty);
        }
        ScheduleSummaryVo cxConsumeSummaryVo = null;
        Double cxConsumeQty = null;
        if (StringUtils.isBlank(scheduleResult.getIsRelease()) && StringUtils.isBlank(scheduleResult.getMachineId())) {
            cxConsumeSummaryVo = cd15ScheduleResultMapper.getCxConsume(scheduleResult);
        }
        if (cxConsumeSummaryVo != null) {
            cxConsumeQty = cxConsumeSummaryVo.getCxConsumeQty();
            summaryVo.setCxConsumeQty(cxConsumeQty);
        }
        // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 成型消耗量
        Double stockQty = ObjectUtils.defaultIfNull(summaryVo.getStockQty(), 0D);
        Double nightPlanQty = ObjectUtils.defaultIfNull(summaryVo.getNightPlanQty(), 0D);
        if (lastDayPlanQty != null && cxConsumeQty != null) {
            summaryVo.setTheoreticClassStockQty(stockQty + lastDayPlanQty + nightPlanQty - cxConsumeQty);
        }*/
        return AjaxResult.success(scheduleSummaryVo);
    }

}
