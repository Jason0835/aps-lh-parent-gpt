package com.zlt.aps.tc.service.impl;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.enums.BillTypeCodeEnums;
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.common.engine.service.impl.BaseFinishQtyImportService;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcDispatcherLog;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;
import com.zlt.aps.tc.engine.service.TcEngineService;
import com.zlt.aps.tc.engine.vo.TcScheduleResultVo;
import com.zlt.aps.tc.entity.TcParams;
import com.zlt.aps.tc.mapper.TcCurlRollMapper;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.TcDispatcherLogService;
import com.zlt.aps.tc.service.TcMachineInfoService;
import com.zlt.aps.tc.service.TcScheduleResultService;
import com.zlt.aps.tc.service.TcSidewallCodeColorService;
import com.zlt.bill.common.service.AbstractBillService;

/**
 * 胎侧排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-06-21
 */
@Service
public class TcScheduleResultServiceImpl extends AbstractBillService<TcScheduleResult> implements TcScheduleResultService {

    @Resource
    private TcScheduleResultMapper tcScheduleResultMapper;
    @Resource
    private TcEngineService tcEngineService;

    @Autowired
    private TcMachineInfoService tcMachineInfoService;

    @Autowired
    private TcSidewallCodeColorService tcSidewallCodeColorService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private TcDispatcherLogService tcDispatcherLogService;

    /**
     * 默认标准长度
     */
    private static final String DEFAULT_STANDARD_LENGTH = "50";
    @Autowired
    private TcCurlRollMapper curlRollMapper;
    @Autowired
    private TcParamsMapper paramsMapper;


    /**
     * 查询胎侧排程结果
     *
     * @param id 胎侧排程结果ID
     * @return 胎侧排程结果
     */
    @Override
    public TcScheduleResult selectTcScheduleResultById(Long id) {
        return tcScheduleResultMapper.selectTcScheduleResultById(id);
    }

    /**
     * 赋值成型消耗量(成型昨日早班消耗量+成型夜班消耗量)、卷曲长度、交接班库存、计划量对应卷数
     *
     * @param scheduleResult 排程结果
     * @param cxConsumeMap   成型消耗量map
     * @param curlRollMap    卷曲长度map
     */
    private static void setLastDayAndCalculate(TcScheduleResult scheduleResult, Map<String, Double> cxConsumeMap,
                                               Map<String, BigDecimal> curlRollMap, BigDecimal standardLength) {
        String code = scheduleResult.getSidewallCode();
        if (cxConsumeMap.containsKey(code)) {
            Double cxConsumeQty = cxConsumeMap.get(code);
            scheduleResult.setCxConsumeQty(cxConsumeQty);
        }
        BigDecimal curlRollLength = curlRollMap.getOrDefault(code, standardLength);
        scheduleResult.setCurlLength(curlRollLength.doubleValue());
        ReflectUtils.invokeMethodByName(scheduleResult, "calculateTheoreticClassLastDayPlanQty", new Object[]{});
        // 执行计算卷数方法
        ReflectUtils.invokeMethodByName(scheduleResult, "calculatePlanQty", new Object[]{});
    }

    /**
     * 查询胎侧排程结果列表
     *
     * @param tcScheduleResult 胎侧排程结果
     * @return 胎侧排程结果
     */
    @Override
    public List<TcScheduleResult> selectTcScheduleResultList(TcScheduleResult tcScheduleResult) {
        List<TcScheduleResult> list=tcScheduleResultMapper.selectTcScheduleResultList(tcScheduleResult);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<String> codeList = list.stream().map(TcScheduleResult::getSidewallCode).collect(Collectors.toList());
        tcScheduleResult.getParams().put("codeList", codeList);
        Map<String, Double> cxConsumeMap = new HashMap<>(16);
        List<TcScheduleResult> cxConsumeList = tcScheduleResultMapper.getCxConsume4List(tcScheduleResult);
        if (CollectionUtils.isNotEmpty(cxConsumeList)) {
            cxConsumeMap = cxConsumeList.stream().collect(Collectors.toMap(TcScheduleResult::getSidewallCode, TcScheduleResult::getCxConsumeQty));
        }
        TcCurlRoll curlRoll = new TcCurlRoll();
        curlRoll.getParams().put("codeList", codeList);
        List<TcCurlRoll> curlRollList = curlRollMapper.listCurlRoll(curlRoll);
        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength));
        }
        LambdaQueryWrapper<TcParams> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(TcParams::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
        TcParams standardLength = paramsMapper.selectOne(paramWrapper);

        List<TcMachineInfo> machineInfoList = tcMachineInfoService.selectMachineInfoList(new TcMachineInfo());
        Map<Long, TcMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(TcMachineInfo::getId, Function.identity(), (s1, s2) -> s1));
        //设置胎侧代码颜色值
        if (CollectionUtils.isNotEmpty(list)){
            TcSidewallCodeColor tcSidewallCodeColor=new TcSidewallCodeColor();
            tcSidewallCodeColor.setStatus("0");
            List<TcSidewallCodeColor> colorList=tcSidewallCodeColorService.selectTcSidewallCodeColorList(tcSidewallCodeColor);

            for (TcScheduleResult tcs : list) {
                String machineIdStr = tcs.getMachineId();
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
                            TcMachineInfo machineInfo = machineInfoMap.get(key);
                            machineNameList.add(machineInfo.getMachineName());
                        }
                    }
                    tcs.setMachineName(String.join(",", machineNameList));
                }
                if (CollectionUtils.isNotEmpty(colorList)) {
                    for (TcSidewallCodeColor color:colorList){
                        if(Pattern.matches(color.getRegularExpression(),tcs.getSidewallCode())){
                            tcs.setColorCode(color.getColorCode());
                            tcs.setColorType(color.getColorType());
                            break;
                        }
                    }
                }
                // 赋值卷曲长度、计算计划量对应卷数
                setLastDayAndCalculate(tcs, cxConsumeMap, curlRollMap,
                        standardLength == null ? new BigDecimal(DEFAULT_STANDARD_LENGTH) : new BigDecimal(standardLength.getParamValue()));
            }
        }

        return list;
    }


    /**
     * 新增胎侧排程结果
     *
     * @param tcScheduleResult 胎侧排程结果
     * @return 结果
     */
    @Override
    public int insertTcScheduleResult(TcScheduleResult tcScheduleResult) {
        tcScheduleResult.setBaseVale(null);
        TcScheduleResultVo scheduleVo = new TcScheduleResultVo();
        BeanUtils.copyProperties(tcScheduleResult, scheduleVo);
        return tcEngineService.inertTcOrder(scheduleVo);
    }

    /**
     * 修改胎侧排程结果
     *
     * @param scheduleResult 胎侧排程结果
     * @return 结果
     */
    @Override
    public int updateTcScheduleResult(TcScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            TcScheduleResult scheduleResult2 = tcScheduleResultMapper.selectTcScheduleResultById(scheduleResult.getId());
            boolean flag = compare(scheduleResult2.getMachineId(), scheduleResult.getMachineId());
            flag = flag && compare(scheduleResult2.getDayPlanQty(), scheduleResult.getDayPlanQty());
            flag = flag && compare(scheduleResult2.getNightPlanQty(), scheduleResult.getNightPlanQty());
            flag = flag && compare(scheduleResult2.getDayProduceOrder(), scheduleResult.getDayProduceOrder());
            flag = flag && compare(scheduleResult2.getNightProduceOrder(), scheduleResult.getNightProduceOrder());
            flag = flag && compare(scheduleResult2.getDayHandAnalysis(), scheduleResult.getDayHandAnalysis());
            flag = flag && compare(scheduleResult2.getNightHandAnalysis(), scheduleResult.getNightHandAnalysis());
            flag = flag && compare(scheduleResult2.getRemark(), scheduleResult.getRemark());
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
        }

        return tcScheduleResultMapper.updateTcScheduleResult(scheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    @Override
    public void insetDispatcherLog(String operType, TcScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        TcScheduleResult oldSchedule = this.tcScheduleResultMapper.selectTcScheduleResultById(newSchedule.getId());  //操作前的排程数据
        TcDispatcherLog log = new TcDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getSidewallCode());    //胎侧代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /** 调用插入日志方法 **/
        tcDispatcherLogService.insertTcDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<TcScheduleResult> scheduleResults, TcScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<TcScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        TcDispatcherLog log = new TcDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getSidewallCode());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<TcScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(TcScheduleResult::getCreateTime));
            if (max.isPresent()) {
                TcScheduleResult scheduleResult = max.get();
                log.setBeforeMachineId(scheduleResult.getMachineId());
                log.setBeforeDayPlan(scheduleResult.getDayPlanQty());
                log.setBeforeNightPlan(scheduleResult.getNightPlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /* 调用插入日志方法 **/
        tcDispatcherLogService.insertTcDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<TcScheduleResult> selectByScheduleDateAndCode(TcScheduleResult scheduleResult) {
        return tcScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
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
     * 批量删除胎侧排程结果
     *
     * @param ids 需要删除的胎侧排程结果ID
     * @return 结果
     */
    @Override
    public int deleteTcScheduleResultByIds(Long[] ids) {
        return tcScheduleResultMapper.deleteTcScheduleResultByIds(ids);
    }

    /**
     * 删除胎侧排程结果信息
     *
     * @param id 胎侧排程结果ID
     * @return 结果
     */
    @Override
    public int deleteTcScheduleResultById(Long id) {
        return tcScheduleResultMapper.deleteTcScheduleResultById(id);
    }

    /**
     * 批量更新发布状态
     *
     * @param ids
     * @param status	发布状态
     */
    @Override
    public int batchUpdate(long[] ids, String status) {
        return tcScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), status);
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TC);
        record.setScheduleDate(scheduleDate);
        return tcScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 唯一性校验
     */
    @Override
    public List<TcScheduleResult> checkUnique(TcScheduleResult entity) {
        return tcScheduleResultMapper.checkUnique(entity);
    }

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<TcScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TcScheduleResult> importList = new ArrayList<>();
        TcMachineInfo tcMachineInfo= new TcMachineInfo();
        tcMachineInfo.setStatus("0");
        List<TcMachineInfo> machineInfoList = tcMachineInfoService.selectMachineInfoList(tcMachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        //根据机台名称去重
        TreeSet<TcMachineInfo> treeSet = new TreeSet<TcMachineInfo>(new Comparator<TcMachineInfo>() {
            @Override
            public int compare(TcMachineInfo o1, TcMachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);


        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TcMachineInfo::getMachineName, TcMachineInfo::getId));
        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSidewallCode() + a.getMachineId()), Collectors.counting()));

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            TcScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));

            //重复记录校验
            Long hasValue = groupMap.get(entity.getSidewallCode()+entity.getMachineId());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.sidewallCode");
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
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                entity.setMachineId(machineCodeMap.get(entity.getMachineId())+"");
                successNum++;
                entity.setBaseVale(null);
                importList.add(entity);
            }
        }
        this.batchSaveTmSchedule(scheduleDate, importList);  //把验证成功的记录进行导入

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 批量更新或新增排程记录信息
     *
     * @param scheduleDate 排程日志，格式：yyyy-MM-dd
     * @param importList   导入数据
     */
    private void batchSaveTmSchedule(String scheduleDate, List<TcScheduleResult> importList) {
        List<TcScheduleResultVo> scheduleList = new ArrayList<>();
        for (TcScheduleResult result : importList) {
            TcScheduleResultVo vo = new TcScheduleResultVo();
            BeanUtils.copyProperties(result, vo);
            if(result.getDayProduceOrder() != null) {
                vo.setDayProduceOrder(result.getDayProduceOrder().intValue());
            }
            if(result.getNightProduceOrder() != null) {
                vo.setNightProduceOrder(result.getNightProduceOrder().intValue());
            }
            scheduleList.add(vo);
        }
        if (!scheduleList.isEmpty()) {
            this.tcEngineService.batchSaveTcSchedule(scheduleDate, scheduleList);
        }
    }

    /**
     * 排程发布
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult publish(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode) {
        //数据同步,发起通知
        tcScheduleResultMapper.deployScheduleToMes(dataVersion, ids, factoryCode, companyCode);

        //保存发布记录，更新发布状态
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TC);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        tcScheduleResultMapper.insertPublishRecord(record);
        tcScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), ApsConstant.RELEASING);
        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
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
        this.batchUpdate(ids, status);
        tcScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 选机台
     */
    @Override
    public AjaxResult chooseMachine(TcScheduleResult scheduleResult) {
        if (CollectionUtils.isNotEmpty(tcScheduleResultMapper.checkUnique(scheduleResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        this.tcEngineService.confirmTcMachine(scheduleResult);  //确认自动排程机台
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        tcScheduleResultMapper.updateTcScheduleResult(scheduleResult);
        return AjaxResult.success();
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return tcScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return tcScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param entity 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(TcScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TC);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        tcScheduleResultMapper.updatePublishRecord(record);
        return tcScheduleResultMapper.changeReleaseStatus(entity);
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
        return tcScheduleResultMapper.combinationMiddleAndNight(map);
    }

    @Override
    public int checkTcCodeExist(TcScheduleResult tcScheduleResult) {
        return tcScheduleResultMapper.checkTcCodeExist(tcScheduleResult);
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return tcScheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<TcScheduleResult> selectByIds(List<Long> ids2) {
        return tcScheduleResultMapper.selectByIds(ids2);
    }

    @Override
    protected String getBillTypeCode() {
        return BillTypeCodeEnums.TC_SCHEDULE_RESULT.getBillTypeCode();
    }

    @Override
    public int importData(List<TcScheduleResult> list, boolean b, long l) {
        return 0;
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
    public AjaxResult importFinishQty(List<TcDayFinishQty> list, Long importLogId) {
        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.TC);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(TcScheduleResult scheduleResult) {
        List<TcScheduleResult> tmScheduleResultList = selectTcScheduleResultList(scheduleResult);
        List<TcScheduleResult> lastDayPlanQty4List = tcScheduleResultMapper.getLastDayPlanQty4List(scheduleResult);
        // 添加昨日排程有，今日排程没有的物料对象，用于后续计算理论交接班库存合计
        List<String> resultCodeList = tmScheduleResultList.stream().map(TcScheduleResult::getSidewallCode).collect(Collectors.toList());
        List<String> notExistCodeList = lastDayPlanQty4List.stream().map(TcScheduleResult::getSidewallCode)
                .filter(item -> !resultCodeList.contains(item)).collect(Collectors.toList());
        TcCurlRoll curlRoll = new TcCurlRoll();
        curlRoll.getParams().put("codeList", notExistCodeList);
        List<TcCurlRoll> curlRollList = curlRollMapper.listCurlRoll(curlRoll);
        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength));
        }

        LambdaQueryWrapper<TcParams> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(TcParams::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
        TcParams standardLengthParams = paramsMapper.selectOne(paramWrapper);

        for (String code : notExistCodeList) {
            TcScheduleResult result = new TcScheduleResult();
            result.setSidewallCode(code);
            BigDecimal standardLength = standardLengthParams == null ? new BigDecimal(DEFAULT_STANDARD_LENGTH) : new BigDecimal(standardLengthParams.getParamValue());
            scheduleResult.setCurlLength(curlRollMap.getOrDefault(code, standardLength).doubleValue());
            tmScheduleResultList.add(result);
        }
        Map<String, TcScheduleResult> lastDayPlanMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lastDayPlanQty4List)) {
            lastDayPlanMap = lastDayPlanQty4List.stream().collect(Collectors.toMap(TcScheduleResult::getSidewallCode, Function.identity()));
        }
        List<TcScheduleResult> cxConsume4List = tcScheduleResultMapper.getCxConsume4List(scheduleResult);
        Map<String, TcScheduleResult> cxConsumeMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(cxConsume4List)) {
            cxConsumeMap = cxConsume4List.stream().collect(Collectors.toMap(TcScheduleResult::getSidewallCode, Function.identity()));
        }
        BigDecimal totalDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalNightPlanQty = BigDecimal.ZERO;
        BigDecimal totalNightPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalStockQty = BigDecimal.ZERO;
        BigDecimal totalStockQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQty = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQtyRollNum = BigDecimal.ZERO;

        for (TcScheduleResult result : tmScheduleResultList) {
            Double nightPlanQty = ObjectUtils.defaultIfNull(result.getNightPlanQty(), 0D);
            Double stockQty = ObjectUtils.defaultIfNull(result.getStockQty(), 0D);
            String code = result.getSidewallCode();
            if (lastDayPlanMap.containsKey(code)) {
                TcScheduleResult lastDayResult = lastDayPlanMap.get(code);
                result.setLastMidPlanQty(lastDayResult.getLastMidPlanQty());
                result.setLastMidPlanQtyRollNum(lastDayResult.getLastMidPlanQtyRollNum());
            }
            if (cxConsumeMap.containsKey(code)) {
                TcScheduleResult cxConsumeResult = cxConsumeMap.get(code);
                result.setCxConsumeQty(cxConsumeResult.getCxConsumeQty());
            }
            Double lastMidPlanQty = result.getLastMidPlanQty();
            Double cxConsumeQty = result.getCxConsumeQty();
            // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 成型消耗量
            if (lastMidPlanQty != null && cxConsumeQty != null) {
                result.setTheoreticClassStockQty(stockQty + lastMidPlanQty + nightPlanQty - cxConsumeQty);
            }
            result.calculatePlanQty();

            totalDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQty(), 0D), totalDayPlanQty);
            totalDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQtyRollNum(), 0D), totalDayPlanQtyRollNum);
            totalNightPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQty(), 0D), totalNightPlanQty);
            totalNightPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQtyRollNum(), 0D), totalNightPlanQtyRollNum);
            totalStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStockQty(), 0D), totalStockQty);
            totalStockQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStockQtyRollNum(), 0D), totalStockQtyRollNum);
            totalLastDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQty(), 0D), totalLastDayPlanQty);
            totalLastDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQtyRollNum(), 0D), totalLastDayPlanQtyRollNum);
            totalTheoreticClassStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getTheoreticClassStockQty(), 0D), totalTheoreticClassStockQty);
            totalTheoreticClassStockQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getTheoreticClassStockQtyRollNum(), 0D), totalTheoreticClassStockQtyRollNum);
        }
        ScheduleSummaryVo scheduleSummaryVo = new ScheduleSummaryVo();
        scheduleSummaryVo.setDayPlanQty(totalDayPlanQty.doubleValue());
        scheduleSummaryVo.setDayPlanQtyRollNum(totalDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setNightPlanQty(totalNightPlanQty.doubleValue());
        scheduleSummaryVo.setNightPlanQtyRollNum(totalNightPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setStockQty(totalStockQty.doubleValue());
        scheduleSummaryVo.setStockQtyRollNum(totalStockQtyRollNum.doubleValue());
        scheduleSummaryVo.setLastDayPlanQty(totalLastDayPlanQty.doubleValue());
        scheduleSummaryVo.setLastDayPlanQtyRollNum(totalLastDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQty(totalTheoreticClassStockQty.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQtyRollNum(totalTheoreticClassStockQtyRollNum.doubleValue());
        /*ScheduleSummaryVo summaryVo = tcScheduleResultMapper.getSummaryVo(scheduleResult);
        if (summaryVo == null) {
            summaryVo = new ScheduleSummaryVo();
            summaryVo.setScheduleDate(scheduleResult.getScheduleDate());
        }
        ScheduleSummaryVo lastDayPlanQtySummaryVo = tcScheduleResultMapper.getLastDayPlanQty(scheduleResult);
        Double lastDayPlanQty = null;
        if (lastDayPlanQtySummaryVo != null) {
            lastDayPlanQty = lastDayPlanQtySummaryVo.getNightPlanQty();
            summaryVo.setLastDayPlanQty(lastDayPlanQty);
        }
        ScheduleSummaryVo cxConsumeSummaryVo = null;
        Double cxConsumeQty = null;
        if (StringUtils.isBlank(scheduleResult.getIsRelease()) && StringUtils.isBlank(scheduleResult.getMachineId())) {
            cxConsumeSummaryVo = tcScheduleResultMapper.getCxConsume(scheduleResult);
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
