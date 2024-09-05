package com.zlt.aps.tm.service.impl;

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
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.engine.service.TmEngineService;
import com.zlt.aps.tm.engine.vo.TmScheduleResultVo;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.TmDispatcherLogService;
import com.zlt.aps.tm.service.TmMachineInfoService;
import com.zlt.aps.tm.service.TmScheduleResultService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎面排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-06-17
 */
@Service
public class TmScheduleResultServiceImpl implements TmScheduleResultService {
    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;
    @Resource
    private TmEngineService tmEngineService;
    @Autowired
    private TmMachineInfoService tmMachineInfoService;
    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;
    @Resource
    private TmDispatcherLogService tmDispatcherLogService;

    /**
     * 查询胎面排程结果
     *
     * @param id 胎面排程结果ID
     * @return 胎面排程结果
     */
    @Override
    public TmScheduleResult selectTmScheduleResultById(Long id) {
        TmScheduleResult tmScheduleResult = tmScheduleResultMapper.selectTmScheduleResultById(id);
        return tmScheduleResult;
    }

    /**
     * 查询胎面排程结果列表
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 胎面排程结果
     */
    @Override
    public List<TmScheduleResult> selectTmScheduleResultList(TmScheduleResult tmScheduleResult) {
        return tmScheduleResultMapper.selectTmScheduleResultList(tmScheduleResult);
    }

    /**
     * 新增胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    @Override
    public int insertTmScheduleResult(TmScheduleResult tmScheduleResult) {
        tmScheduleResult.setBaseVale(null);
        TmScheduleResultVo scheduleVo = new TmScheduleResultVo();
        BeanUtils.copyProperties(tmScheduleResult, scheduleVo);
        return tmEngineService.inertTmOrder(scheduleVo);
    }

    @Override
    public int checkTmCodeExist(TmScheduleResult tmScheduleResult) {
        return tmScheduleResultMapper.checkTmCodeExist(tmScheduleResult);
    }

    /**
     * 修改胎面排程结果
     *
     * @param scheduleResult 胎面排程结果
     * @return 结果
     */
    @Override
    public int updateTmScheduleResult(TmScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            TmScheduleResult scheduleResult2 = tmScheduleResultMapper.selectTmScheduleResultById(scheduleResult.getId());
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
        return tmScheduleResultMapper.updateTmScheduleResult(scheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, TmScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        TmScheduleResult oldSchedule = this.tmScheduleResultMapper.selectTmScheduleResultById(newSchedule.getId());  //操作前的排程数据
        TmDispatcherLog log = new TmDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getTreadCode());    //胎面代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /** 调用插入日志方法 **/
        tmDispatcherLogService.insertTmDispatcherLog(log);
    }


    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<TmScheduleResult> scheduleResults, TmScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<TmScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        TmDispatcherLog log = new TmDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getTreadCode());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<TmScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(TmScheduleResult::getCreateTime));
            if (max.isPresent()) {
                TmScheduleResult scheduleResult = max.get();
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
        tmDispatcherLogService.insertTmDispatcherLog(log);
    }

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<TmScheduleResult> selectByScheduleDateAndCode(TmScheduleResult scheduleResult) {
        return tmScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
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
     * 批量删除胎面排程结果
     *
     * @param ids 需要删除的胎面排程结果ID
     * @return 结果
     */
    @Override
    public int deleteTmScheduleResultByIds(Long[] ids) {
        return tmScheduleResultMapper.deleteTmScheduleResultByIds(ids);
    }

    /**
     * 删除胎面排程结果信息
     *
     * @param id 胎面排程结果ID
     * @return 结果
     */
    @Override
    public int deleteTmScheduleResultById(Long id) {
        return tmScheduleResultMapper.deleteTmScheduleResultById(id);
    }

    /**
     * 批量更新发布状态
     *
     * @param ids
     * @param status	发布状态
     */
    @Override
    public int batchUpdate(long[] ids, String status) {
        return tmScheduleResultMapper.batchUpdate(ids, status);
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TM);
        record.setScheduleDate(scheduleDate);
        return tmScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 唯一性校验
     */
    public List<TmScheduleResult> checkUnique(TmScheduleResult tmScheduleResult) {
        return tmScheduleResultMapper.checkUnique(tmScheduleResult);
    }

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<TmScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmScheduleResult> importList = new ArrayList<>();
        TmMachineInfo tmMachineInfo = new TmMachineInfo();
        tmMachineInfo.setStatus("0");
        List<TmMachineInfo> machineInfoList = tmMachineInfoService.selectMachineInfoList(tmMachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        //根据机台名称去重
        TreeSet<TmMachineInfo> treeSet = new TreeSet<TmMachineInfo>(new Comparator<TmMachineInfo>() {
            @Override
            public int compare(TmMachineInfo o1, TmMachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);

        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(TmMachineInfo::getMachineName, TmMachineInfo::getId));
        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getTreadCode() + a.getMachineId()), Collectors.counting()));

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            TmScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));

            //重复记录校验
            Long hasValue = groupMap.get(entity.getTreadCode() + entity.getMachineId());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.treadCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 3, entity);
            // 机台code 转为机台id
            if (entity.getMachineId()!=null && entity.getMachineId().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                message = String.format(message, i + 3, I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                addImportErrorLog(importLogId, i + 3, message, validated);
            }
            if (machineCodeMap.get(entity.getMachineId()) == null) {
                addImportErrorLog(importLogId, i + 3,
                        I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                entity.setMachineId(machineCodeMap.get(entity.getMachineId()) + "");
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
    private void batchSaveTmSchedule(String scheduleDate, List<TmScheduleResult> importList) {
        List<TmScheduleResultVo> scheduleList = new ArrayList<>();
        for (TmScheduleResult result : importList) {
            TmScheduleResultVo vo = new TmScheduleResultVo();
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
            this.tmEngineService.batchSaveTmSchedule(scheduleDate, scheduleList);
        }
    }

    /**
     * 排程发布
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult publish(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode) {
        //数据同步,发起通知
        tmScheduleResultMapper.deployScheduleToMes(dataVersion, ids, factoryCode, companyCode);

        //保存发布记录，更新发布状态
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TM);
        record.setScheduleDate(scheduleDate);
        record.setDataVersion(dataVersion);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        tmScheduleResultMapper.insertPublishRecord(record);
        tmScheduleResultMapper.batchUpdate(ids, ApsConstant.RELEASING);
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
        tmScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 选机台
     */
    public AjaxResult chooseMachine(TmScheduleResult scheduleResult) {
        if (CollectionUtils.isNotEmpty(tmScheduleResultMapper.checkUnique(scheduleResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
        this.tmEngineService.confirmTmMachine(scheduleResult);  //确认自动排程机台
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        tmScheduleResultMapper.updateTmScheduleResult(scheduleResult);
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
        return tmScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return tmScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(TmScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_TM);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        tmScheduleResultMapper.updatePublishRecord(record);
        return tmScheduleResultMapper.changeReleaseStatus(entity);
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
        return tmScheduleResultMapper.combinationMiddleAndNight(map);
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return tmScheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<TmScheduleResult> selectByIds(List<Long> ids2) {
        return tmScheduleResultMapper.selectByIds(ids2);
    }
}
