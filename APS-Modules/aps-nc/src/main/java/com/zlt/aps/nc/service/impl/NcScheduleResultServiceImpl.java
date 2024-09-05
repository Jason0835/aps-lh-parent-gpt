package com.zlt.aps.nc.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.nc.api.domain.entity.NcDispatcherLog;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import com.zlt.aps.nc.engine.service.NcEngineService;
import com.zlt.aps.nc.engine.vo.NcScheduleResultVo;
import com.zlt.aps.nc.mapper.NcScheduleResultMapper;
import com.zlt.aps.nc.service.NcDispatcherLogService;
import com.zlt.aps.nc.service.NcMachineInfoService;
import com.zlt.aps.nc.service.NcScheduleResultService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 内衬胶排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-06-24
 */
@Service
public class NcScheduleResultServiceImpl implements NcScheduleResultService {
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
     * 查询内衬排程结果列表
     *
     * @param ncScheduleResult 内衬排程结果
     * @return 内衬排程结果
     */
    @Override
    public List<NcScheduleResult> selectNcScheduleResultList(NcScheduleResult ncScheduleResult) {
        return ncScheduleResultMapper.selectNcScheduleResultList(ncScheduleResult);
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
        return ncScheduleResultMapper.batchUpdate(ids, ApsConstant.RELEASING);
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
    	ncScheduleResultMapper.batchUpdate(ids, status);
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
     * @param scheduleDate 排程日期
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
}
