package com.zlt.aps.nc.service.impl;

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
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.service.NcEngineService;
import com.zlt.aps.nc.engine.vo.NcScheduleResultVo;
import com.zlt.aps.nc.entity.NcParams;
import com.zlt.aps.nc.mapper.NcCurlRollMapper;
import com.zlt.aps.nc.mapper.NcParamsMapper;
import com.zlt.aps.nc.mapper.NcScheduleResultMapper;
import com.zlt.aps.nc.service.NcDispatcherLogService;
import com.zlt.aps.nc.service.NcMachineInfoService;
import com.zlt.aps.nc.service.NcScheduleResultService;
import com.zlt.bill.common.service.AbstractBillService;

/**
 * 内衬胶排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-06-24
 */
@Service
public class NcScheduleResultServiceImpl extends AbstractBillService<NcScheduleResult> implements NcScheduleResultService {
    @Resource
    private NcScheduleResultMapper ncScheduleResultMapper;

    @Resource
    private NcEngineService ncEngineService;

    @Autowired
    private NcMachineInfoService machineInfoService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private NcDispatcherLogService ncDispatcherLogService;

    /**
     * 默认标准长度
     */
    private static final String DEFAULT_STANDARD_LENGTH = "80";
    @Autowired
    private NcCurlRollMapper curlRollMapper;
    @Autowired
    private NcParamsMapper paramsMapper;

    /**
     * 查询内衬排程结果
     *
     * @param id 内衬排程结果ID
     * @return 内衬排程结果
     */
    @Override
    public NcScheduleResult selectNcScheduleResultById(Long id) {
        return ncScheduleResultMapper.selectNcScheduleResultById(id);
    }

    /**
     * 赋值成型消耗量(成型昨日早班消耗量+成型夜班消耗量)、卷曲长度、交接班库存、计划量对应卷数
     *
     * @param scheduleResult 排程结果
     * @param cxConsumeMap   成型消耗量map
     * @param curlRollMap    卷曲长度map
     */
    private static void setLastDayAndCalculate(NcScheduleResult scheduleResult, Map<String, Double> cxConsumeMap,
                                               Map<String, BigDecimal> curlRollMap, BigDecimal standardLength) {
        String code = scheduleResult.getLiningCode();
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
     * 查询内衬排程结果列表
     *
     * @param ncScheduleResult 内衬排程结果
     * @return 内衬排程结果
     */
    @Override
    public List<NcScheduleResult> selectNcScheduleResultList(NcScheduleResult ncScheduleResult) {
        List<NcScheduleResult> list = ncScheduleResultMapper.selectNcScheduleResultList(ncScheduleResult);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<NcMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(new NcMachineInfo());
        Map<Long, NcMachineInfo> machineInfoMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getId, Function.identity(), (s1, s2) -> s1));
        if (CollectionUtils.isNotEmpty(list)) {

            List<String> codeList = list.stream().map(NcScheduleResult::getLiningCode).collect(Collectors.toList());
            ncScheduleResult.getParams().put("codeList", codeList);
            Map<String, Double> cxConsumeMap = new HashMap<>(16);
            List<NcScheduleResult> cxConsumeList = ncScheduleResultMapper.getCxConsume4List(ncScheduleResult);
            if (CollectionUtils.isNotEmpty(cxConsumeList)) {
                cxConsumeMap = cxConsumeList.stream().collect(Collectors.toMap(NcScheduleResult::getLiningCode, NcScheduleResult::getCxConsumeQty));
            }
            NcCurlRoll curlRoll = new NcCurlRoll();
            curlRoll.getParams().put("codeList", codeList);
            List<NcCurlRoll> curlRollList = curlRollMapper.listCurlRoll(curlRoll);
            Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(curlRollList)) {
                curlRollMap = curlRollList.stream().collect(Collectors.toMap(NcCurlRoll::getLiningCode, NcCurlRoll::getCurlLength));
            }
            LambdaQueryWrapper<NcParams> paramWrapper = new LambdaQueryWrapper<>();
            paramWrapper.eq(NcParams::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
            NcParams standardLength = paramsMapper.selectOne(paramWrapper);

            for (NcScheduleResult scheduleResult : list) {
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
                            NcMachineInfo machineInfo = machineInfoMap.get(key);
                            machineNameList.add(machineInfo.getMachineName());
                        }
                    }
                    scheduleResult.setMachineName(String.join(",", machineNameList));
                    // 赋值卷曲长度、计算计划量对应卷数
                    setLastDayAndCalculate(scheduleResult, cxConsumeMap, curlRollMap,
                            standardLength == null ? new BigDecimal(DEFAULT_STANDARD_LENGTH) : new BigDecimal(standardLength.getParamValue()));
                }
            }
        }
        return list;
    }

    /**
     * 新增内衬排程结果
     *
     * @param ncScheduleResult 内衬排程结果
     * @return 结果
     */
    @Override
    public int insertNcScheduleResult(NcScheduleResult ncScheduleResult) {
        ncScheduleResult.setBaseVale(null);
        NcScheduleResultVo scheduleVo = new NcScheduleResultVo();
        BeanUtils.copyProperties(ncScheduleResult, scheduleVo);
        return ncEngineService.inertNcOrder(scheduleVo);
    }

    /**
     * 修改内衬排程结果
     *
     * @param scheduleResult 内衬排程结果
     * @return 结果
     */
    @Override
    public int updateNcScheduleResult(NcScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            NcScheduleResult scheduleResult2 = ncScheduleResultMapper.selectNcScheduleResultById(scheduleResult.getId());
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
        return ncScheduleResultMapper.updateNcScheduleResult(scheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    @Override
    public void insetDispatcherLog(String operType, NcScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        NcScheduleResult oldSchedule = this.ncScheduleResultMapper.selectNcScheduleResultById(newSchedule.getId());  //操作前的排程数据
        NcDispatcherLog log = new NcDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getLiningCode());    //内衬代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /** 调用插入日志方法 **/
        ncDispatcherLogService.insertNcDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<NcScheduleResult> scheduleResults, NcScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<NcScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        NcDispatcherLog log = new NcDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getLiningCode());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<NcScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(NcScheduleResult::getCreateTime));
            if (max.isPresent()) {
                NcScheduleResult scheduleResult = max.get();
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
        ncDispatcherLogService.insertNcDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<NcScheduleResult> selectByScheduleDateAndCode(NcScheduleResult scheduleResult) {
        return ncScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
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
     * 批量删除内衬排程结果
     *
     * @param ids 需要删除的内衬排程结果ID
     * @return 结果
     */
    @Override
    public int deleteNcScheduleResultByIds(Long[] ids) {
        return ncScheduleResultMapper.deleteNcScheduleResultByIds(ids);
    }

    /**
     * 删除内衬排程结果信息
     *
     * @param id 内衬排程结果ID
     * @return 结果
     */
    @Override
    public int deleteNcScheduleResultById(Long id) {
        return ncScheduleResultMapper.deleteNcScheduleResultById(id);
    }

    /**
     * 批量更新发布状态
     *
     * @param ids
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdate(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_NC);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        this.deployNcScheduleToMid(ids, dataVersion, factoryCode, companyCode);   //把排程数据发布到中间库，并通知MES
        ncScheduleResultMapper.insertPublishRecord(record);
        return ncScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), ApsConstant.RELEASING);
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
    	ncScheduleResultMapper.batchUpdate(Arrays.stream(ids)
                .boxed().collect(Collectors.toList()), status);
        ncScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 把排程数据发布到中间库
     *
     * @param ids 排程id
     */
    private void deployNcScheduleToMid(long[] ids, String dataVersion, String factoryCode, String companyCode) {
        if (ids == null) {
            return;
        }
        ncScheduleResultMapper.deployNcScheduleToMid(dataVersion, ids, factoryCode, companyCode);   //把排程数据同步到接口中间库中
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_NC);
        record.setScheduleDate(scheduleDate);
        return ncScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 唯一性校验
     */
    @Override
    public List<NcScheduleResult> checkUnique(NcScheduleResult entity) {
        return ncScheduleResultMapper.checkUnique(entity);
    }


    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<NcScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<NcScheduleResult> importList = new ArrayList<>();
        NcMachineInfo ncMachineInfo=new NcMachineInfo();
        ncMachineInfo.setStatus("0");
        List<NcMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(ncMachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        //根据机台名称去重
        TreeSet<NcMachineInfo> treeSet = new TreeSet<NcMachineInfo>(new Comparator<NcMachineInfo>() {
            @Override
            public int compare(NcMachineInfo o1, NcMachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);

        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(NcMachineInfo::getMachineName, NcMachineInfo::getId));
        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getLiningCode() + a.getMachineId()), Collectors.counting()));

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            NcScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));

            //重复记录校验
            Long hasValue = groupMap.get(entity.getLiningCode() + entity.getMachineId());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.liningCode");
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
        this.batchSaveNcSchedule(scheduleDate, importList);  //把验证成功的记录进行导入


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
    private void batchSaveNcSchedule(String scheduleDate, List<NcScheduleResult> importList) {
        List<NcScheduleResultVo> scheduleList = new ArrayList<>();
        for (NcScheduleResult result : importList) {
            NcScheduleResultVo vo = new NcScheduleResultVo();
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
            this.ncEngineService.batchSaveNcSchedule(scheduleDate, scheduleList);
        }
    }

    /**
     * 选机台
     */
    @Override
    public AjaxResult chooseMachine(NcScheduleResult scheduleResult) {
        if (CollectionUtils.isNotEmpty(ncScheduleResultMapper.checkUnique(scheduleResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        this.ncEngineService.confirmNcMachine(scheduleResult);  //确认自动排程机台
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        ncScheduleResultMapper.updateNcScheduleResult(scheduleResult);
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
        return ncScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return ncScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param entity 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(NcScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_NC);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        ncScheduleResultMapper.updatePublishRecord(record);
        return ncScheduleResultMapper.changeReleaseStatus(entity);
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
        return ncScheduleResultMapper.combinationMiddleAndNight(map);
    }

    @Override
    public int checkNcCodeExist(NcScheduleResult ncScheduleResult) {
        return ncScheduleResultMapper.checkNcCodeExist(ncScheduleResult);
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return ncScheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<NcScheduleResult> selectByIds(List<Long> ids2) {
        return ncScheduleResultMapper.selectByIds(ids2);
    }

    @Override
    protected String getBillTypeCode() {
        return BillTypeCodeEnums.NC_SCHEDULE_RESULT.getBillTypeCode();
    }

    @Override
    public int importData(List<NcScheduleResult> list, boolean b, long l) {
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
    public AjaxResult importFinishQty(List<NcDayFinishQty> list, Long importLogId) {
        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.NC);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(NcScheduleResult scheduleResult) {
        List<NcScheduleResult> tmScheduleResultList = selectNcScheduleResultList(scheduleResult);
        List<NcScheduleResult> lastDayPlanQty4List = ncScheduleResultMapper.getLastDayPlanQty4List(scheduleResult);
        // 添加昨日排程有，今日排程没有的物料对象，用于后续计算理论交接班库存合计
        List<String> resultCodeList = tmScheduleResultList.stream().map(NcScheduleResult::getLiningCode).collect(Collectors.toList());
        List<String> notExistCodeList = lastDayPlanQty4List.stream().map(NcScheduleResult::getLiningCode)
                .filter(item -> !resultCodeList.contains(item)).collect(Collectors.toList());
        NcCurlRoll curlRoll = new NcCurlRoll();
        curlRoll.getParams().put("codeList", notExistCodeList);
        List<NcCurlRoll> curlRollList = curlRollMapper.listCurlRoll(curlRoll);
        Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(curlRollList)) {
            curlRollMap = curlRollList.stream().collect(Collectors.toMap(NcCurlRoll::getLiningCode, NcCurlRoll::getCurlLength));
        }

        LambdaQueryWrapper<NcParams> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(NcParams::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
        NcParams standardLengthParams = paramsMapper.selectOne(paramWrapper);
        for (String code : notExistCodeList) {
            NcScheduleResult result = new NcScheduleResult();
            result.setLiningCode(code);
            BigDecimal standardLength = standardLengthParams == null ? new BigDecimal(DEFAULT_STANDARD_LENGTH) : new BigDecimal(standardLengthParams.getParamValue());
            scheduleResult.setCurlLength(curlRollMap.getOrDefault(code, standardLength).doubleValue());
            tmScheduleResultList.add(result);
        }
        Map<String, NcScheduleResult> lastDayPlanMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lastDayPlanQty4List)) {
            lastDayPlanMap = lastDayPlanQty4List.stream().collect(Collectors.toMap(NcScheduleResult::getLiningCode, Function.identity()));
        }
        List<NcScheduleResult> cxConsume4List = ncScheduleResultMapper.getCxConsume4List(scheduleResult);
        Map<String, NcScheduleResult> cxConsumeMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(cxConsume4List)) {
            cxConsumeMap = cxConsume4List.stream().collect(Collectors.toMap(NcScheduleResult::getLiningCode, Function.identity()));
        }
        BigDecimal totalDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalNightPlanQty = BigDecimal.ZERO;
        BigDecimal totalNightPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalNextDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalNextDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalStockQty = BigDecimal.ZERO;
        BigDecimal totalStockQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQtyRollNum = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQty = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQtyRollNum = BigDecimal.ZERO;

        for (NcScheduleResult result : tmScheduleResultList) {
            Double nightPlanQty = ObjectUtils.defaultIfNull(result.getNightPlanQty(), 0D);
            Double stockQty = ObjectUtils.defaultIfNull(result.getStockQty(), 0D);
            String code = result.getLiningCode();
            if (lastDayPlanMap.containsKey(code)) {
                NcScheduleResult lastDayResult = lastDayPlanMap.get(code);
                result.setLastMidPlanQty(lastDayResult.getLastMidPlanQty());
                result.setLastMidPlanQtyRollNum(lastDayResult.getLastMidPlanQtyRollNum());
            }
            if (cxConsumeMap.containsKey(code)) {
                NcScheduleResult cxConsumeResult = cxConsumeMap.get(code);
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
            totalNextDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQty(), 0D), totalNightPlanQty);
            totalNextDayPlanQtyRollNum = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQtyRollNum(), 0D), totalNightPlanQtyRollNum);
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
        scheduleSummaryVo.setNextDayPlanQty(totalNextDayPlanQty.doubleValue());
        scheduleSummaryVo.setNextDayPlanQtyRollNum(totalNextDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setStockQty(totalStockQty.doubleValue());
        scheduleSummaryVo.setStockQtyRollNum(totalStockQtyRollNum.doubleValue());
        scheduleSummaryVo.setLastDayPlanQty(totalLastDayPlanQty.doubleValue());
        scheduleSummaryVo.setLastDayPlanQtyRollNum(totalLastDayPlanQtyRollNum.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQty(totalTheoreticClassStockQty.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQtyRollNum(totalTheoreticClassStockQtyRollNum.doubleValue());
        /*ScheduleSummaryVo summaryVo = ncScheduleResultMapper.getSummaryVo(scheduleResult);
        if (summaryVo == null) {
            summaryVo = new ScheduleSummaryVo();
            summaryVo.setScheduleDate(scheduleResult.getScheduleDate());
        }
        ScheduleSummaryVo lastDayPlanQtySummaryVo = ncScheduleResultMapper.getLastDayPlanQty(scheduleResult);
        Double lastDayPlanQty = null;
        if (lastDayPlanQtySummaryVo != null) {
            lastDayPlanQty = lastDayPlanQtySummaryVo.getNightPlanQty();
            summaryVo.setLastDayPlanQty(lastDayPlanQty);
        }
        ScheduleSummaryVo cxConsumeSummaryVo = null;
        Double cxConsumeQty = null;
        if (StringUtils.isBlank(scheduleResult.getIsRelease()) && StringUtils.isBlank(scheduleResult.getMachineId())) {
            cxConsumeSummaryVo = ncScheduleResultMapper.getCxConsume(scheduleResult);
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
