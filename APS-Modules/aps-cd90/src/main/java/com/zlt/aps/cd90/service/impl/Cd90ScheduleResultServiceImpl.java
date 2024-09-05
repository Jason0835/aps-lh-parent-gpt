package com.zlt.aps.cd90.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.entity.Cd90DispatcherLog;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.common.handle.Cd90SyncDataHandle;
import com.zlt.aps.cd90.engine.service.Cd90EngineProductOrderService;
import com.zlt.aps.cd90.engine.service.Cd90EngineService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.service.Cd90DispatcherLogService;
import com.zlt.aps.cd90.service.Cd90MachineInfoService;
import com.zlt.aps.cd90.service.Cd90ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.sync.povo.SyncParamsVO;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 90度裁断排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-07-06
 */
@Service
public class Cd90ScheduleResultServiceImpl implements Cd90ScheduleResultService {

    @Autowired
    private Cd90ScheduleResultMapper cd90ScheduleResultMapper;

    @Autowired
    private Cd90MachineInfoService machineInfoService;

    @Autowired
    private Cd90SyncDataHandle cd90SyncDataHandle;

    @Autowired
    private Cd90EngineService cd90EngineService;

    @Autowired
    private Cd90EngineProductOrderService cd90EngineProductOrderService;

    @Autowired
    private FactoryService factoryService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private Cd90DispatcherLogService cd90DispatcherLogService;

    /**
     * 查询90度裁断排程结果
     *
     * @param id 90度裁断排程结果ID
     * @return 90度裁断排程结果
     */
    @Override
    public Cd90ScheduleResult selectCd90ScheduleResultById(Long id) {
        return cd90ScheduleResultMapper.selectCd90ScheduleResultById(id);
    }

    /**
     * 查询90度裁断排程结果列表
     *
     * @param cd90ScheduleResult 90度裁断排程结果
     * @return 90度裁断排程结果
     */
    @Override
    public List<Cd90ScheduleResult> selectCd90ScheduleResultList(Cd90ScheduleResult cd90ScheduleResult) {
        return cd90ScheduleResultMapper.selectCd90ScheduleResultList(cd90ScheduleResult);
    }

    /**
     * 新增90度裁断排程结果
     *
     * @param cd90ScheduleResult 90度裁断排程结果
     * @return 结果
     */
    @Override
    public int insertCd90ScheduleResult(Cd90ScheduleResult cd90ScheduleResult) {
        cd90ScheduleResult.setBaseVale(null);
        // 调用引擎插单接口
        return cd90EngineService.insertCd90Order(cd90ScheduleResult);
    }

    /**
     * 修改90度裁断排程结果
     *
     * @param cd90ScheduleResult 90度裁断排程结果
     * @return 结果
     */
    @Override
    public int updateCd90ScheduleResult(Cd90ScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            Cd90ScheduleResult scheduleResult2 = cd90ScheduleResultMapper.selectCd90ScheduleResultById(scheduleResult.getId());
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
        return cd90ScheduleResultMapper.updateCd90ScheduleResult(scheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, Cd90ScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        Cd90ScheduleResult oldSchedule = this.cd90ScheduleResultMapper.selectCd90ScheduleResultById(newSchedule.getId());  //操作前的排程数据
        Cd90DispatcherLog log = new Cd90DispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getClothCode());    //胎圈代码
        //操作前的信息赋值
        log.setBeforeMachineId(oldSchedule.getMachineId());
        log.setBeforeDayPlan(oldSchedule.getDayPlanQty());
        log.setBeforeNightPlan(oldSchedule.getNightPlanQty());
        //操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        /** 调用插入日志方法 **/
        cd90DispatcherLogService.insertCd90DispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<Cd90ScheduleResult> scheduleResults, Cd90ScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<Cd90ScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        Cd90DispatcherLog log = new Cd90DispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setMaterialCode(newSchedule.getClothCode());
        //操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<Cd90ScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(Cd90ScheduleResult::getCreateTime));
            if (max.isPresent()) {
                Cd90ScheduleResult scheduleResult = max.get();
                log.setBeforeMachineId(scheduleResult.getMachineId());
                log.setBeforeDayPlan(scheduleResult.getDayPlanQty());
                log.setBeforeNightPlan(scheduleResult.getNightPlanQty());
            }
        }
        // 操作后的信息赋值
        log.setAfterMachineId(newSchedule.getMachineId());
        log.setAfterDayPlan(newSchedule.getDayPlanQty());
        log.setAfterNightPlan(newSchedule.getNightPlanQty());
        // 调用插入日志方法
        cd90DispatcherLogService.insertCd90DispatcherLog(log);
    }

    /**
     * 修改90度裁断排程结果机台
     *
     * @param scheduleResult 90度裁断排程结果
     * @return 结果
     */
    @Override
    public void chooseMachine(Cd90ScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
        // 调用引擎确认机台接口
    	cd90EngineService.confirmCd90Machine(scheduleResult);
        updateCd90ScheduleResult(scheduleResult);
        // 调用引擎生产顺序重算接口
        cd90EngineProductOrderService.recalculateProduceOrder(scheduleResult);
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
     * 批量删除90度裁断排程结果
     *
     * @param ids 需要删除的90度裁断排程结果ID
     * @return 结果
     */
    @Override
    public int deleteCd90ScheduleResultByIds(Long[] ids) {
        return cd90ScheduleResultMapper.deleteCd90ScheduleResultByIds(ids);
    }

    /**
     * 删除90度裁断排程结果信息
     *
     * @param id 90度裁断排程结果ID
     * @return 结果
     */
    @Override
    public int deleteCd90ScheduleResultById(Long id) {
        return cd90ScheduleResultMapper.deleteCd90ScheduleResultById(id);
    }

    /**
     * 批量修改
     */
    @Override
    public int batchUpdate(long[] ids, Date scheduleDate, String dataVersion) {
        //把排程数据发布到中间库，并通知MES
        this.deployScheduleToMid(ids, scheduleDate, dataVersion);
        // 保存发布记录日志
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD90);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        cd90ScheduleResultMapper.insertPublishRecord(record);
        return cd90ScheduleResultMapper.batchUpdate(ids, ApsConstant.RELEASING);
    }

    /**
     * 发布排程数据到中间库,并通知 MES
     * @param ids 发布的排程记录id
     * @param scheduleDate 排产日期
     * @param dataVersion 数据同步版本
     */
    private void deployScheduleToMid(long[] ids, Date scheduleDate, String dataVersion) {
        if(ids == null) {
            return;
        }
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
        //把排程数据同步到接口中间库中
        cd90ScheduleResultMapper.deployCd90ScheduleToMid(dataVersion, ids, factoryCode, companyCode,
				DateUtils.getNowDate());
    }

	/**
	 * 给mes发送排程下发通知
	 * 
	 * @param scheduleDate 排产日
	 * @param dataVersion  数据版本
	 */
    @Override
	public void publishNoticeMes(Date scheduleDate, String dataVersion, int rowCount) {
        // 厂别、分公司编号
        String factoryCode = factoryService.getFactoryCode();
        String companyCode = factoryService.getCompanyCode();
		//数据同步到中间库后，往 mq中发送消息通知 MES去取数据
        SyncParamsVO syncParamsVO = new SyncParamsVO();
        syncParamsVO.setSyncKey(ApsConstant.CD90_DEPLOY_SYNC_KEY);
        syncParamsVO.setDataVersion(dataVersion);
        syncParamsVO.setFactoryCode(factoryCode);
        syncParamsVO.setCompanyCode(companyCode);
        // 请求参数
        JSONObject params = new JSONObject();
        params.put("scheduleDate", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, scheduleDate));
        params.put("rowCount", rowCount);
        syncParamsVO.setParams(params);
        //往消息队列发送消息
        cd90SyncDataHandle.syncNotice(syncParamsVO);
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
        cd90ScheduleResultMapper.batchUpdate(ids, status);
        cd90ScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }
    
    /**
     * 校验查询
     */
    @Override
    public List<Cd90ScheduleResult> checkScheduleResultUnique(Cd90ScheduleResult cd90ScheduleResult) {
        return cd90ScheduleResultMapper.checkScheduleResultUnique(cd90ScheduleResult);
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CD90);
        record.setScheduleDate(scheduleDate);
        return cd90ScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<Cd90ScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<Cd90ScheduleResult> importList = new ArrayList<>();
        Cd90MachineInfo cd90MachineInfo= new Cd90MachineInfo();
        cd90MachineInfo.setStatus("0");
        List<Cd90MachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(cd90MachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        //根据机台名称去重
        TreeSet<Cd90MachineInfo> treeSet = new TreeSet<Cd90MachineInfo>(new Comparator<Cd90MachineInfo>() {
            @Override
            public int compare(Cd90MachineInfo o1, Cd90MachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);

        Map<String, Long> machineCodeMap = machineInfoList.stream().collect(Collectors.toMap(Cd90MachineInfo::getMachineName, Cd90MachineInfo::getId));

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getClothCode() + a.getMachineId()), Collectors.counting()));

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            Cd90ScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 3, entity);

            //重复记录校验
            Long hasValue = groupMap.get(entity.getClothCode() + entity.getMachineId());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cd90ScheduleResult.clothCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                continue;
            }


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
        //把验证成功的记录进行导入  importList
        if (!importList.isEmpty()) {
			// 如果引擎导入失败，会将失败日志返回
        	List<ImportErrorLog> engineImportErrorLogs = cd90EngineService.batchSaveCd90Schedule(DateUtils.dateTime("yyyy-MM-dd", scheduleDate), importList);
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
        return cd90ScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return cd90ScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(Cd90ScheduleResult entity) {
        return cd90ScheduleResultMapper.changeReleaseStatus(entity);
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
        return cd90ScheduleResultMapper.combinationMiddleAndNight(map);
    }

    @Override
    public int checkCd90CodeExist(Cd90ScheduleResult cd90ScheduleResult) {
        return cd90ScheduleResultMapper.checkCd90CodeExist(cd90ScheduleResult);
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return cd90ScheduleResultMapper.isPublishByIds(ids);
    }

    @Override
    public List<Cd90ScheduleResult> selectByIds(List<Long> ids2) {
        return cd90ScheduleResultMapper.selectByIds(ids2);
    }

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<Cd90ScheduleResult> selectByScheduleDateAndCode(Cd90ScheduleResult scheduleResult) {
        return cd90ScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }
}
