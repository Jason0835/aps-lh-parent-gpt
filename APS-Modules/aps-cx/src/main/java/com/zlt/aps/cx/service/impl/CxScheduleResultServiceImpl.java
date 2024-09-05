package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.service.CxEngineChangeLhMachineService;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.*;
import com.zlt.aps.cx.engine.common.CommonCacheService;
import com.zlt.aps.cx.engine.constants.CxEngineConstants;
import com.zlt.aps.cx.engine.constants.CxParamCodeConstants;
import com.zlt.aps.cx.engine.enums.AdjustTypeEnums;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.service.CxScheduleEngineService;
import com.zlt.aps.cx.mapper.CxFactoryProductionProductMapper;
import com.zlt.aps.cx.mapper.CxMachineInfoMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.*;
import com.zlt.aps.lh.engine.enums.TaskTypeEnum;
import com.zlt.aps.lh.engine.service.LhEngineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 成型排程结果Service业务层处理
 *
 * @author zlt
 * @date 2021-07-12
 */
@Service
@Slf4j
public class CxScheduleResultServiceImpl implements CxScheduleResultService {
    @Autowired
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Autowired
    private CxPlanProductStatusService cxPlanProductStatusService;

    @Autowired
    private CxMachineInfoService machineInfoService;

    @Autowired
    private CxMachineInfoMapper cxMachineInfoMapper;

    @Autowired
    private LhEngineService lhEngineService;

    @Autowired
    private CxFactoryProductionProductMapper factoryProductionProductMapper;

    @Resource(name = "cxScheduleEngineService")
    private CxScheduleEngineService cxScheduleEngineService;

    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private CxDispatcherLogService cxDispatcherLogService;

    @Autowired
    private CxCheckConstructionService cxCheckConstructionService;

    @Autowired
    private CommonCacheService commonCacheService;

    @Autowired
    private CxEngineChangeLhMachineService cxEngineChangeLhMachineService;

    @Autowired
    private ExecutorService executorService;

    /**
     * 查询成型排程结果
     *
     * @param id 成型排程结果ID
     * @return 成型排程结果
     */
    @Override
    public CxScheduleResult selectCxScheduleResultById(Long id) {
        return cxScheduleResultMapper.selectCxScheduleResultById(id);
    }

    public CxScheduleResult selectCxScheduleResultByIdForQty(Long id) {
        return cxScheduleResultMapper.selectCxScheduleResultByIdForQty(id);
    }

    /**
     * 查询成型排程结果列表
     *
     * @param cxScheduleResult 成型排程结果
     * @return 成型排程结果
     */
    @Override
    public List<CxScheduleResult> selectCxScheduleResultList(CxScheduleResult cxScheduleResult) {
        return cxScheduleResultMapper.selectCxScheduleResultList(cxScheduleResult);
    }

    public List<CxScheduleResult> finishedList(CxScheduleResult cxScheduleResult){
        return cxScheduleResultMapper.finishedList(cxScheduleResult);
    }
    /**
     * 硫化自动排程校验
     */
    public List<CxScheduleResult> getLhList(CxScheduleResult cxScheduleResult) {
        return cxScheduleResultMapper.getLhList(cxScheduleResult);
    }

    /**
     * 查询非本id的且包含该硫化机的记录
     */
    public List<CxScheduleResult> getListByLhMachineCode(CxScheduleResult cxScheduleResult) {
        return cxScheduleResultMapper.getListByLhMachineCode(cxScheduleResult);
    }

    /**
     * 新增成型排程结果
     *
     * @param cxScheduleResult 成型排程结果
     * @return 结果
     */
    @Override
    public int insertCxScheduleResult(CxScheduleResult cxScheduleResult) {
        cxScheduleResult.setBaseVale(null);
        return cxScheduleResultMapper.insertCxScheduleResult(cxScheduleResult);
    }

    @Override
    public int updateCxScheduleResultForMolds(CxScheduleResult scheduleResult,CxScheduleResult osEntity) {

        //硫化状态为投产中且硫化机台数变少了后更改硫化状态为已收尾欠产，
        List<String> str1 =null;
        List<String> str2 =null;
        int newMachines=0;
        int osMachines=0;
        if(StringUtils.isNotBlank(scheduleResult.getLhMachineCode())){
            str1 = Arrays.asList(scheduleResult.getLhMachineCode().split(","));
            newMachines=str1.size();
        }
        if(StringUtils.isNotBlank(osEntity.getLhMachineCode())){
            str2 = Arrays.asList(osEntity.getLhMachineCode().split(","));
            osMachines=str2.size();
        }

        if(CxEngineConstants.TASK_TYPE_DOING.equals(scheduleResult.getTaskType()) && newMachines<osMachines){
            scheduleResult.setTaskType(CxEngineConstants.TASK_CLOSE_OUT_DELIN);
        }

        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {

            List<String> desc1 =null;
            List<String> desc2 =null;
            if(StringUtils.isNotBlank(scheduleResult.getLhMachineChangeMoldDesc())){
                desc1 = Arrays.asList(scheduleResult.getLhMachineChangeMoldDesc().split(";"));
            }
            if(StringUtils.isNotBlank(osEntity.getLhMachineChangeMoldDesc())){
                desc2 = Arrays.asList(osEntity.getLhMachineChangeMoldDesc().split(";"));
            }

            boolean flag = compare(str1, str2) && compare(desc1, desc2);
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
        }
        //Joran 2021-09-16 进行单班硫化量重算
        cxScheduleEngineService.calcAvaliableClassShift(scheduleResult, AdjustTypeEnums.CHANGE_LH_MACHINE);
        int result =cxScheduleResultMapper.updateCxScheduleResult(scheduleResult);

        //Joran 2021-12-21 更新完毕后进行同机台同胎胚不同外胎计划量重新合并更新start
        if(result>0){
            cxScheduleEngineService.reSetSingleLhShiftQty(scheduleResult);
        }
        //Joran 2021-12-21 更新完毕后进行同机台同胎胚不同外胎计划量重新合并更新end
        return result;
    }

    /**
     * 修改成型排程结果
     *
     * @param scheduleResult 成型排程结果
     * @return 结果
     */
    @Override
    public int updateCxScheduleResult(CxScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
       // if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            CxScheduleResult scheduleResult2 = cxScheduleResultMapper.selectCxScheduleResultById(scheduleResult.getId());
            boolean flag = compare(scheduleResult2.getCxMachineCode(), scheduleResult.getCxMachineCode());
            flag = flag && compare(scheduleResult2.getLhMachineCode(), scheduleResult.getLhMachineCode());
            flag = flag && compare(scheduleResult2.getClass1PlanQty(), scheduleResult.getClass1PlanQty());
            flag = flag && compare(scheduleResult2.getClass2PlanQty(), scheduleResult.getClass2PlanQty());
            flag = flag && compare(scheduleResult2.getClass3PlanQty(), scheduleResult.getClass3PlanQty());
            flag = flag && compare(scheduleResult2.getClass4PlanQty(), scheduleResult.getClass4PlanQty());
            flag = flag && compare(scheduleResult2.getClass5PlanQty(), scheduleResult.getClass5PlanQty());
            flag = flag && compare(scheduleResult2.getClass1AnalysisInput(), scheduleResult.getClass1AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass2AnalysisInput(), scheduleResult.getClass2AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass3AnalysisInput(), scheduleResult.getClass3AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass4AnalysisInput(), scheduleResult.getClass4AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass5AnalysisInput(), scheduleResult.getClass5AnalysisInput());
            flag = flag && compare(scheduleResult2.getRemark(), scheduleResult.getRemark());
            flag = flag && compare(scheduleResult2.getMaximumClassQty(), scheduleResult.getMaximumClassQty());
            flag = flag && compare(scheduleResult2.getExpectedOverProduction(), scheduleResult.getExpectedOverProduction());
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
        //}
        Integer ac=scheduleResult.getActualOverProduction()==null?0:scheduleResult.getActualOverProduction();
        Integer ex=scheduleResult.getExpectedOverProduction()==null?0:scheduleResult.getExpectedOverProduction();
        Integer difrence=ac-ex;
        scheduleResult.setDifferenceOverProduction(difrence);
        return cxScheduleResultMapper.updateCxScheduleResult(scheduleResult);
    }

    /**
     * 修改成型、硫化状态
     */
    @Override
    public int modifyStatus(CxScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        if (!ApsConstant.RELEASING.equals(scheduleResult.getIsRelease()) || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getIsRelease()) || StringUtils.isEmpty(scheduleResult.getIsRelease())) {
            CxScheduleResult scheduleResult2 = cxScheduleResultMapper.selectCxScheduleResultById(scheduleResult.getId());
            boolean flag = compare(scheduleResult2.getTaskType(), scheduleResult.getTaskType());
            flag = flag && compare(scheduleResult2.getProductionStatus(), scheduleResult.getProductionStatus());
            flag = flag && compare(scheduleResult2.getRemark(), scheduleResult.getRemark());
            if (!flag) {
                scheduleResult.setIsRelease(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
            scheduleResult.setMaximumClassQty(scheduleResult2.getMaximumClassQty());
            scheduleResult.setExpectedOverProduction(scheduleResult2.getExpectedOverProduction());
        }
        return cxScheduleResultMapper.updateCxScheduleResult(scheduleResult);
    }

    /**
     * 修改施工版本
     * @param cxScheduleResult
     * @return
     */
    public int changeBomDataVersion(CxScheduleResult cxScheduleResult){
        CxScheduleResult scheduleResult2 = cxScheduleResultMapper.selectCxScheduleResultById(cxScheduleResult.getId());
        if (ApsConstant.IS_RELEASE.equals(cxScheduleResult.getIsRelease()) || StringUtils.isEmpty(cxScheduleResult.getIsRelease())) {
            Boolean flag= compare(scheduleResult2.getBomDataVersion(), cxScheduleResult.getBomDataVersion());
            if (!flag) {
                cxScheduleResult.setIsRelease(cxScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                cxScheduleResult.setBaseVale(cxScheduleResult.getId());
            }
        }
        int result=cxScheduleResultMapper.changeBomDataVersion(cxScheduleResult);

        //Joran 2022-06-20 检查当前排程日期的所有排程是否还有空的施工版本，如果没有的话进行班次时间计算start
         Date scheduleDate=scheduleResult2.getScheduleDate();
         //调用重新计算任务时间
         calcTaskListTime(scheduleDate);
        //Joran 2022-06-20 检查当前排程日期的所有排程是否还有空的施工版本，如果没有的话进行班次时间计算end
        return result;
    }

    /**
     *  修改施工版本后触发计算任务时间
     * @param scheduleDate
     */
    private void calcTaskListTime(Date scheduleDate) {
        int bomDataVersionEmptyCount=cxScheduleResultMapper.checkBomDataVersionEmpty(scheduleDate);
        //不存在施工版本未确认的数据进行任务时间计算start
        if(bomDataVersionEmptyCount== BigDecimal.ZERO.intValue()){
            //为了防止影响用户体验效果启用线程来执行start
          /*  Runnable calcTimeRunnable= new Runnable() {
                @Override
                public void run() {
                    cxScheduleEngineService.calcCxScheduleTaskListTime(scheduleDate);
                }
            };
           //通过线程池来启动任务
          // ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
           //cachedThreadPool.execute(calcTimeRunnable);
            executorService.execute(calcTimeRunnable);*/
            cxScheduleEngineService.calcCxScheduleTaskListTime(scheduleDate);
        }
        //不存在施工版本未确认的数据进行任务时间计算end
    }

    /**
     * 调量更新
     */
    public int updateCxScheduleResultForQty(CxScheduleResult cxScheduleResult) {

        if (CollectionUtils.isNotEmpty(cxScheduleResult.getTmScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getTmScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateTmScheduleResult(cxScheduleSub);
            }
        }

        if (CollectionUtils.isNotEmpty(cxScheduleResult.getTcScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getTcScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateTcScheduleResult(cxScheduleSub);
            }
        }

        if (CollectionUtils.isNotEmpty(cxScheduleResult.getTqScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getTqScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateTqScheduleResult(cxScheduleSub);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getNcScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getNcScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateNcScheduleResult(cxScheduleSub);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getCd15ScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getCd15ScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateCd15ScheduleResult(cxScheduleSub);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getCd90ScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getCd90ScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateCd90cheduleResult(cxScheduleSub);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getGdyyScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getGdyyScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateGdyyScheduleResult(cxScheduleSub);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getXwyyScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getXwyyScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateXwyyScheduleResult(cxScheduleSub);
            }
        }
        if (CollectionUtils.isNotEmpty(cxScheduleResult.getGsqScheduleList())) {
            List<CxScheduleSub> list = cxScheduleResult.getGsqScheduleList();
            for (CxScheduleSub cxScheduleSub : list) {
                cxScheduleResultMapper.updateGsqScheduleResult(cxScheduleSub);
            }
        }
        // 校验字段是否修改，修改则改状态为未发布
        if (ApsConstant.IS_RELEASE.equals(cxScheduleResult.getIsRelease()) || StringUtils.isEmpty(cxScheduleResult.getIsRelease())) {
            CxScheduleResult scheduleResult2 = cxScheduleResultMapper.selectCxScheduleResultById(cxScheduleResult.getId());
            boolean flag = compare(scheduleResult2.getClass1PlanQty(), cxScheduleResult.getClass1PlanQty());
            flag = flag && compare(scheduleResult2.getClass2PlanQty(), cxScheduleResult.getClass2PlanQty());
            flag = flag && compare(scheduleResult2.getClass3PlanQty(), cxScheduleResult.getClass3PlanQty());
            flag = flag && compare(scheduleResult2.getClass4PlanQty(), cxScheduleResult.getClass4PlanQty());
            flag = flag && compare(scheduleResult2.getClass5PlanQty(), cxScheduleResult.getClass5PlanQty());
            flag = flag && compare(scheduleResult2.getClass1AnalysisInput(), cxScheduleResult.getClass1AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass2AnalysisInput(), cxScheduleResult.getClass2AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass3AnalysisInput(), cxScheduleResult.getClass3AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass4AnalysisInput(), cxScheduleResult.getClass4AnalysisInput());
            flag = flag && compare(scheduleResult2.getClass5AnalysisInput(), cxScheduleResult.getClass5AnalysisInput());
            flag = flag && compare(scheduleResult2.getRemark(), cxScheduleResult.getRemark());
            if (!flag) {
                cxScheduleResult.setIsRelease(cxScheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
            }
        }
        return cxScheduleResultMapper.updateCxScheduleResult(cxScheduleResult);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    public void insetDispatcherLog(String operType, CxScheduleResult oldSchedule, CxScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        if(oldSchedule == null) {
            oldSchedule = this.cxScheduleResultMapper.selectCxScheduleResultById(newSchedule.getId());  //操作前的排程数据
        }
        CxDispatcherLog log = new CxDispatcherLog();
        //基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setSapCode(newSchedule.getSapCode());  //sap品号
        log.setEmbryoCode(newSchedule.getEmbryoCode());  //胎胚代码
        log.setEmbryoVersion(newSchedule.getBomDataVersion());  //胎胚版本
        //操作前的信息赋值
        log.setBeforeLhMachineCode(oldSchedule.getLhMachineCode());
        log.setBeforeCxMachineCode(oldSchedule.getCxMachineCode());
        log.setBeforeClass1Plan(oldSchedule.getClass1PlanQty());
        log.setBeforeClass2Plan(oldSchedule.getClass2PlanQty());
        log.setBeforeClass3Plan(oldSchedule.getClass3PlanQty());
        log.setBeforeClass4Plan(oldSchedule.getClass4PlanQty());
        log.setBeforeClass5Plan(oldSchedule.getClass5PlanQty());
        //操作后的信息赋值
        log.setAfterLhMachineCode(newSchedule.getLhMachineCode());
        log.setAfterCxMachineCode(newSchedule.getCxMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        log.setAfterClass4Plan(newSchedule.getClass4PlanQty());
        log.setAfterClass5Plan(newSchedule.getClass5PlanQty());
        /** 调用插入日志方法 **/
        cxDispatcherLogService.insertCxDispatcherLog(log);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType        操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insetDispatcherLogInsertOrder(String operType, List<CxScheduleResult> scheduleResults, CxScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        //        if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        //            return;
        //        }
        List<CxScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        CxDispatcherLog log = new CxDispatcherLog();
        //基础信息赋值
        log.setScheduleId(scheduleResultList.get(0).getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());  //排程日期
        log.setSapCode(newSchedule.getSapCode());  //sap品号
        log.setEmbryoCode(newSchedule.getEmbryoCode());  //胎胚代码
        log.setEmbryoVersion(newSchedule.getBomDataVersion());  //胎胚版本
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            Optional<CxScheduleResult> max = scheduleResults.stream().max(Comparator.comparing(CxScheduleResult::getCreateTime));
            if (max.isPresent()) {
                CxScheduleResult scheduleResult = max.get();
                log.setBeforeCxMachineCode(scheduleResult.getCxMachineCode());
                log.setBeforeLhMachineCode(scheduleResult.getLhMachineCode());
                log.setBeforeClass1Plan(scheduleResult.getClass1PlanQty());
                log.setBeforeClass2Plan(scheduleResult.getClass2PlanQty());
                log.setBeforeClass3Plan(scheduleResult.getClass3PlanQty());
                log.setBeforeClass4Plan(scheduleResult.getClass4PlanQty());
                log.setBeforeClass5Plan(scheduleResult.getClass5PlanQty());
            }
        }
        //操作后的信息赋值
        log.setAfterLhMachineCode(newSchedule.getLhMachineCode());
        log.setAfterCxMachineCode(newSchedule.getCxMachineCode());
        log.setAfterClass1Plan(newSchedule.getClass1PlanQty());
        log.setAfterClass2Plan(newSchedule.getClass2PlanQty());
        log.setAfterClass3Plan(newSchedule.getClass3PlanQty());
        log.setAfterClass4Plan(newSchedule.getClass4PlanQty());
        log.setAfterClass5Plan(newSchedule.getClass5PlanQty());
        /* 调用插入日志方法 **/
        cxDispatcherLogService.insertCxDispatcherLog(log);
    }

    /**
     * 根据排程日期、胎胚代码、SAP、施工版本查询记录
     * @return 查询到的记录
     */
    @Override
    public List<CxScheduleResult> selectByScheduleDateAndCode(CxScheduleResult scheduleResult) {
        return cxScheduleResultMapper.selectByScheduleDateAndCode(scheduleResult);
    }

    public boolean compare(String str1, String str2) {
        return (StringUtils.isEmpty(str1) ? StringUtils.isEmpty(str2) : str1.equals(str2));
    }

    public boolean compare(Integer d1, Integer d2) {
        return (d1 == null ? d2 == null : d1.equals(d2));
    }

    public boolean compare(Double d1, Double d2) {
        d1 = ObjectUtils.isEmpty(d1) ? 0D : d1;
        d2 = ObjectUtils.isEmpty(d2) ? 0D : d2;
        return d1.equals(d2);
    }

    public boolean compare(List<String> a, List<String> b) {
        if(CollectionUtils.isEmpty(a) && CollectionUtils.isEmpty(b)){
            return true;
        }
        if(CollectionUtils.isNotEmpty(a) && CollectionUtils.isEmpty(b)){
            return false;
        }
        if(CollectionUtils.isEmpty(a) && CollectionUtils.isNotEmpty(b)){
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        Collections.sort(a);
        Collections.sort(b);
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 批量删除成型排程结果
     *
     * @param ids 需要删除的成型排程结果ID
     * @return 结果
     */
    @Override
    public int deleteCxScheduleResultByIds(Long[] ids) {
        return cxScheduleResultMapper.deleteCxScheduleResultByIds(ids);
    }

    /**
     * 手工收尾
     *
     * @param ids
     * @return
     */
    public int manualClose(Long[] ids) {
        return cxScheduleResultMapper.manualClose(ids);
    }

    /**
     * 删除成型排程结果信息
     *
     * @param id 成型排程结果ID
     * @return 结果
     */
    @Override
    public int deleteCxScheduleResultById(Long id) {
        return cxScheduleResultMapper.deleteCxScheduleResultById(id);
    }

    /**
     * 批量修改
     */
    @Override
    public int batchUpdate(long[] ids) {
        return cxScheduleResultMapper.batchUpdate(ids, ApsConstant.IS_RELEASE);
    }

    /**
     * 校验查询
     */
    @Override
    public List<CxScheduleResult> checkScheduleResultUnique(CxScheduleResult cxScheduleResult) {
        return cxScheduleResultMapper.checkScheduleResultUnique(cxScheduleResult);
    }

    /**
     * 查询成型排程结果列表
     *
     * @param cxScheduleResult 成型排程结果
     * @return 成型排程结果
     */
    @Override
    public List<CxScheduleResult> selectCxScheduleResultListForExport(CxScheduleResult cxScheduleResult) {
        return cxScheduleResultMapper.selectCxScheduleResultListForExport(cxScheduleResult);
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isCxPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CX);
        record.setScheduleDate(scheduleDate);
        return cxScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @Override
    public Boolean isLhPublish(Date scheduleDate) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_LH);
        record.setScheduleDate(scheduleDate);
        return cxScheduleResultMapper.isPublish(record) > 0;
    }

    /**
     * 排程发布后更新状态及投产状态表状态更新
     *
     * @param ids
     * @param status	发布状态
     * @return
     */
    @Override
    @Transactional
    public int schedulePublish(long[] ids, String status) {
        //Joran 2021-08-04 更新投产状态中待发布的状态变更为已投产
        this.updateProductStatus(status);
        return cxScheduleResultMapper.batchUpdate(ids, status);
    }

    /**
     * 更新投产状态中待发布的状态变更为已投产
     * @param status
     */
	private void updateProductStatus(String status) {
        // 只有发布成功才更新投产发布状态
		if (ApsConstant.IS_RELEASE.equals(status)) {
            CxPlanProductStatus cxPlanProductStatus = new CxPlanProductStatus();
            cxPlanProductStatus.setProductStatus(CxEngineConstants.MDM_PLAN_PRODUCT_STATUS_YES);
            cxPlanProductStatus.setUpdateTime(DateUtil.now());
            cxPlanProductStatusService.updateProductStatusToProduct(cxPlanProductStatus);
        }
	}

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<CxScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<CxScheduleResult> importList = new ArrayList<>();
        CxMachineInfo machineInfo = new CxMachineInfo();
        machineInfo.setStatus("0");
        List<CxMachineInfo> cxmachineInfoList = machineInfoService.selectCxMachineInfoList(machineInfo);
        machineInfo.setId(6L);
        List<CxMachineInfo> lhmachineInfoList = machineInfoService.selectCxMachineInfoList2(machineInfo);

        if (CollectionUtils.isEmpty(cxmachineInfoList) || CollectionUtils.isEmpty(lhmachineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }
        //根据机台名称去重
        TreeSet<CxMachineInfo> treeSet = new TreeSet<CxMachineInfo>(new Comparator<CxMachineInfo>() {
            @Override
            public int compare(CxMachineInfo o1, CxMachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(cxmachineInfoList);
        cxmachineInfoList =new ArrayList<>(treeSet);
        treeSet.clear();
        treeSet.addAll(lhmachineInfoList);
        lhmachineInfoList =new ArrayList<>(treeSet);

        Map<String, String> cxmachineNameMap = cxmachineInfoList.stream().collect(Collectors.toMap(CxMachineInfo::getMachineName, CxMachineInfo::getMachineCode));
        Map<String, String> cxmachineTypeMap = cxmachineInfoList.stream().collect(Collectors.toMap(CxMachineInfo::getMachineName, CxMachineInfo::getMachineType));
        Map<String, String> lhmachineNameMap = lhmachineInfoList.stream().collect(Collectors.toMap(CxMachineInfo::getMachineName, CxMachineInfo::getMachineCode));
        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getCxMachineName() + a.getEmbryoCode() + a.getSapCode()), Collectors.counting()));

        //遍历校验
        for (int i = 0; i < list.size(); i++) {
            CxScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            //重复记录校验
            Long hasValue = groupMap.get(entity.getCxMachineName() + entity.getEmbryoCode() + entity.getSapCode());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cxScheduleResult.cxMachineCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.cxScheduleResult.embryoCode");
                String columnName3 = I18nUtil.getMessage("ui.data.column.cxScheduleResult.sapCode");
                message=String.format(message,columnName+"+"+columnName2+"+"+columnName3);
                addImportErrorLog(importLogId, i + 4,message, importErrorLogs);
                continue;
            }

            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 4, entity);

            if(entity.getCxMachineName()!=null && entity.getCxMachineName().indexOf(",")>0){
                String message = I18nUtil.getMessage("ui.data.column.machine.cxMachineCodeValidate");
                message=String.format(message, i + 4, I18nUtil.getMessage("ui.data.column.cxScheduleResult.cxMachineCode"));
                addImportErrorLog(importLogId, i + 4,message, validated);
            }

            //成型机台编号校验
            if (StringUtils.isEmpty(cxmachineNameMap.get(entity.getCxMachineName()))) {
                addImportErrorLog(importLogId, i + 4, I18nUtil.getMessage("ui.error.message.column.machineNotExist"), validated);
            }
            entity.setCxMachineCode(cxmachineNameMap.get(entity.getCxMachineName()));


            //20230908 导入时成型机类型需要和胎胚对应 Nick+
//            if (StringUtils.isNotEmpty(cxmachineTypeMap.get(entity.getCxMachineName()))) {
//                String machineType = cxmachineTypeMap.get(entity.getCxMachineName()).equals("1") ? "Y" : "E";
//                String embryoCode = entity.getEmbryoCode();
//
//                if (embryoCode.length() < 1 || !embryoCode.startsWith(machineType)){
//                    addImportErrorLog(importLogId, i + 4, I18nUtil.getMessage("ui.error.message.column.machineTypeNotMatch1"), validated);
//                }
//            }

            if (StringUtils.isNotEmpty(entity.getLhMachineName())) {
                String[] machineCodeArr = entity.getLhMachineName().split(",");
                boolean hasError = false;
                String machineCodes="";
                for (String machineName : machineCodeArr) {
                    if (StringUtils.isEmpty(lhmachineNameMap.get(machineName))) {
                        hasError = true;
                        break;
                    }else{
                        if(StringUtils.isNotBlank(machineCodes)){
                            machineCodes=machineCodes+","+lhmachineNameMap.get(machineName);
                        }else{
                            machineCodes=lhmachineNameMap.get(machineName);
                        }
                    }
                }
                if (hasError) {
                    addImportErrorLog(importLogId, i + 4, I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.notExist"), validated);
                }else{
                    entity.setLhMachineCode(machineCodes);
                }
            }


            //重设使用模数和更换类型描述
            if (StringUtils.isNotEmpty(entity.getLhMachineCode()) && entity.getLhMachineQty()!=null) {
                Double lhMachineQty = entity.getLhMachineQty();
                String lhMachineCode = entity.getLhMachineCode();
                int arrLength = lhMachineCode.split(",").length;
                if (lhMachineQty % 2 == 0 && lhMachineQty / arrLength == 2) {
                    String[] machineCodeArr = lhMachineCode.split(",");
                    String lhMachineChangeMoldDesc = "";
                    for (String machineCode : machineCodeArr) {
                        lhMachineChangeMoldDesc += machineCode + ":0:2;";
                    }
                    entity.setLhMachineChangeMoldDesc(lhMachineChangeMoldDesc);
                }else if(arrLength == 1 && (lhMachineQty == 2 || lhMachineQty==1)){
                    entity.setLhMachineQty(lhMachineQty);
                    if(lhMachineQty == 2){
                        entity.setLhMachineChangeMoldDesc(lhMachineCode + ":0:2;");
                    }else{
                        entity.setLhMachineChangeMoldDesc(lhMachineCode + ":0:1;");
                    }
                }else{
                    entity.setLhMachineQty(null);
                }
            }else{
                entity.setLhMachineQty(null);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                successNum++;
                entity.setBaseVale(null);
                importList.add(entity);
            }

        }
        //此处调用engine接口，importList
        List<ImportErrorLog> importErrorLogList=null;
        if(StringUtils.isNotEmpty(importList)){
            importErrorLogList = cxScheduleEngineService.batchImportSchedule(importList, DateUtils.dateTime("yyyy-MM-dd", scheduleDate), importLogId);
        }

        if (StringUtils.isNotEmpty(importErrorLogList)) {
            importErrorLogs.addAll(importErrorLogList);
            failureNum += importErrorLogList.size();
            successNum -= importErrorLogList.size();
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 排程发布
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult publish(long[] ids, Date scheduleDate,String dataVersion,String factoryCode,String companyCode) {
        String language="zh_CN";
        //TODO Joran 2021-12-04 添加国际化语言获取，PCR6厂传语言怕MES无法解析，默认中文，预留功能
       // Locale locale=I18nUtil.getLocaleFromRedis();
       // language=locale.getLanguage();

        //数据同步,发起通知
        cxScheduleResultMapper.deployScheduleToMes(dataVersion, ids, factoryCode, companyCode,language);
        //保存发布记录，更新发布状态
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(null);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CX);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        //Joran 2022-03-09记录发布对应的数据版本号
        record.setDataVersion(dataVersion);
        cxScheduleResultMapper.insertPublishRecord(record);
        //Joran 2021-10-12 排程发布更新投产表状态，再进行排程更新
        schedulePublish(ids, ApsConstant.RELEASING);
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
        this.schedulePublish(ids, status);
        cxScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 获取-使用模数
     */
    public CxScheduleResult getMolds(CxScheduleResult cxScheduleResult) {

        //初始化数据，获取最大模数
        Long maxMolds = 0L;
        Long usedMolds = 0L;
        Long usableMolds = 0L;
        String lhMachineCode = cxScheduleResult.getLhMachineCode();
        CxMachineInfo machineInfo = new CxMachineInfo();
        machineInfo.setMachineCode(lhMachineCode);
        List<CxMachineInfo> lhMachine = cxMachineInfoMapper.getMaxMoldsByLhMachineCode(machineInfo);
        if (CollectionUtils.isNotEmpty(lhMachine)) {
            maxMolds = lhMachine.get(0).getQuata() == null ? 0L : lhMachine.get(0).getQuata();
        }

        //统计已使用模数
        List<CxScheduleResult> list = cxScheduleResultMapper.getListByLhMachineCode(cxScheduleResult);
        if (CollectionUtils.isNotEmpty(list)) {
            for (CxScheduleResult item : list) {
                //更换类型描述：硫化机code1:拆模换code:模数molds;硫化机code2:点数换code:模数molds;
                String desc = item.getLhMachineChangeMoldDesc();
                if (StringUtils.isNotEmpty(desc)) {
                    String[] descs = desc.split(";");
                    for (String str : descs) {
                        if (str.indexOf(lhMachineCode + ":") != -1) {
                            String[] detail = str.split(":");
                            usedMolds = usedMolds + Long.valueOf(detail[2]);
                        }
                    }
                }
            }
        }
        usableMolds = maxMolds - usedMolds;
        usableMolds = usableMolds < 0L ? 0L : usableMolds;
        cxScheduleResult.setAvailableMoldQty(usableMolds.intValue());
        return cxScheduleResult;
    }

    /**
     * 校验-使用模数
     */
    public AjaxResult modifyMoldsValidate(CxScheduleResult entity) {

        String msg = "";
        String msg1 = "";
        String notWork = ""; //不可作业机台名称集合
        String errorLimitWork = ""; //错误的限制作业机台名称集合
        String limitMachineName=""; //已配置限制作业的机台名称集合
        List<LhMachineChangeTpye> lhMachineChangeTpyeList = entity.getLhMachineChangeTpyeList();
        if (CollectionUtils.isNotEmpty(lhMachineChangeTpyeList)) {
            CxScheduleResult cxScheduleResult = cxScheduleResultMapper.selectCxScheduleResultById(entity.getId());

            //初始值-机台
            CxMachineInfo machineInfo = new CxMachineInfo();
            machineInfo.setId(6L);
            List<CxMachineInfo> list3 = cxMachineInfoMapper.selectCxMachineInfoList2(machineInfo);
            Map<String, String> machineNameMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(list3)) {
                machineNameMap = list3.stream().collect(Collectors.toMap(a -> a.getMachineCode(), a -> a.getMachineName()));
            }

            //原硫化机台数组
            String osLhMachineCode = cxScheduleResult.getLhMachineCode();
            List<String> osLhMachineCodeArr = null;
            if (StringUtils.isNotEmpty(osLhMachineCode)) {
                osLhMachineCodeArr = Arrays.asList(osLhMachineCode.split(","));
            }

            //重复记录校验
            TreeSet<LhMachineChangeTpye> treeSet = new TreeSet<LhMachineChangeTpye>(new Comparator<LhMachineChangeTpye>() {
                @Override
                public int compare(LhMachineChangeTpye o1, LhMachineChangeTpye o2) {
                    return o1.getMachineCode().compareTo(o2.getMachineCode());
                }
            });
            treeSet.addAll(lhMachineChangeTpyeList);
            if (treeSet.size() != lhMachineChangeTpyeList.size()) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.alreadyExists"));
            }


            //校验-相同sap硫化机不能重复，警告提示：使用模数不能大于剩余模数
            for (LhMachineChangeTpye item : lhMachineChangeTpyeList) {

                //相同sap硫化机不能重复
                CxScheduleResult query = new CxScheduleResult();
                query.setScheduleDate(cxScheduleResult.getScheduleDate());
                query.setSapCode(cxScheduleResult.getSapCode());
                query.setId(cxScheduleResult.getId());
                query.setLhMachineCode(item.getMachineCode());
                List<CxScheduleResult> list = cxScheduleResultMapper.getListByLhMachineCode(query);
                if (CollectionUtils.isNotEmpty(list)) {
                    String machineName = machineNameMap.get(item.getMachineCode());
                    return AjaxResult.error(StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.sapCodeAlreadyExists"), machineName));
                }

                //警告提示：使用模数不能大于剩余模数
                Long maxMolds = 0L;
                Long usedMolds = 0L;
                Long usableMolds = 0L;
                CxMachineInfo query1 = new CxMachineInfo();
                query1.setMachineCode(item.getMachineCode());
                List<CxMachineInfo> lhMachine = cxMachineInfoMapper.getMaxMoldsByLhMachineCode(query1);
                if (CollectionUtils.isNotEmpty(lhMachine)) {
                    maxMolds = lhMachine.get(0).getQuata() == null ? 0L : lhMachine.get(0).getQuata();
                }

                //统计已使用模数
                CxScheduleResult query2 = new CxScheduleResult();
                query2.setScheduleDate(cxScheduleResult.getScheduleDate());
                query2.setId(cxScheduleResult.getId());
                query2.setLhMachineCode(item.getMachineCode());
                List<CxScheduleResult> list2 = cxScheduleResultMapper.getListByLhMachineCode(query2);
                for (CxScheduleResult item1 : list2) {
                    String desc = item1.getLhMachineChangeMoldDesc();
                    if (StringUtils.isNotEmpty(desc)) {
                        String[] descs = desc.split(";");
                        for (String str : descs) {
                            if (str.indexOf(item.getMachineCode() + ":") != -1) {
                                String[] detail = str.split(":");
                                usedMolds = usedMolds + Long.valueOf(detail[2]);
                            }
                        }
                    }
                }

                //计算可用模数
                usableMolds = maxMolds - usedMolds;
                String machineName = machineNameMap.get(item.getMachineCode());
                if (usableMolds <= 0L) {
                    if (StringUtils.isEmpty(msg)) {
                        msg = machineName;
                    } else {
                        msg = msg + "，" + machineName;
                    }
                } else {
                    long de = usableMolds - item.getMolds();
                    if (de < 0L) {
                        if (StringUtils.isEmpty(msg1)) {
                            msg1 = StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.usableMoldsValidate"), machineName, usableMolds, item.getMolds());
                        } else {
                            msg1 = msg1 + "<br/>" + StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.usableMoldsValidate"), machineName, usableMolds, item.getMolds());
                        }
                    }
                }
            }

            //若配置了定点机台那么做不可/限制作业校验，
            CxMachineInfo cxMachineInfo=new CxMachineInfo();
            cxMachineInfo.setMachineName(cxScheduleResult.getSapCode());
            List<CxMachineInfo> specimalMachineList=cxMachineInfoMapper.getLhSpecimalMachine(cxMachineInfo);
            if(CollectionUtils.isNotEmpty(specimalMachineList)){
                List<CxMachineInfo> notWorkMachineList=specimalMachineList.stream().filter(a->"1".equals(a.getType())).collect(Collectors.toList());
                Map<String, String> notWorkMachineMap = new HashMap<>();
                if(CollectionUtils.isNotEmpty(notWorkMachineList)){
                    notWorkMachineMap=notWorkMachineList.stream().collect(Collectors.toMap(a->a.getMachineCode(),a->a.getMachineName()));
                }

                List<CxMachineInfo> limitWorkMachineList=specimalMachineList.stream().filter(a->"0".equals(a.getType())).collect(Collectors.toList());
                Map<String, String> limitWorkMachineMap = new HashMap<>();
                if(CollectionUtils.isNotEmpty(limitWorkMachineList)){
                    limitWorkMachineMap=limitWorkMachineList.stream().collect(Collectors.toMap(a->a.getMachineCode(),a->a.getMachineName()));
                    List<String> machineNameList=limitWorkMachineList.stream().map(a->a.getMachineName()).collect(Collectors.toList());
                    limitMachineName=StringUtils.join(machineNameList,"、");
                }

                for (LhMachineChangeTpye item : lhMachineChangeTpyeList) {
                    if(StringUtils.isNotBlank(notWorkMachineMap.get(item.getMachineCode()))){
                        if(StringUtils.isNotBlank(notWork)){
                            notWork= notWork+"、"+notWorkMachineMap.get(item.getMachineCode());
                        }else{
                            notWork= notWorkMachineMap.get(item.getMachineCode());
                        }
                        continue;
                    }
                    if(CollectionUtils.isNotEmpty(limitWorkMachineList) && StringUtils.isBlank(limitWorkMachineMap.get(item.getMachineCode()))){
                        if(StringUtils.isNotBlank(errorLimitWork)){
                            errorLimitWork= errorLimitWork+"、"+machineNameMap.get(item.getMachineCode());
                        }else{
                            errorLimitWork= machineNameMap.get(item.getMachineCode());
                        }
                        continue;
                    }
                }
            }
        }

        //提示信息
        String resultMsg = "";
        if (StringUtils.isNotEmpty(msg)) {
            resultMsg = StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.moreThanThreeUsed"), msg);
        }
        if (StringUtils.isNotEmpty(msg1)) {
            if (StringUtils.isNotEmpty(resultMsg)) {
                resultMsg = resultMsg + "<br/>" + msg1;
            } else {
                resultMsg = msg1;
            }
        }

        if (StringUtils.isNotEmpty(resultMsg)) {
            resultMsg = resultMsg + " " + I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.makesureAdd");
        }

        if (StringUtils.isNotBlank(notWork)) {
            String notWorkMsg=StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.notWorkMsg"), notWork);
            if (StringUtils.isNotBlank(resultMsg)) {
                resultMsg = resultMsg + "<br/>"+notWorkMsg;
            }else{
                resultMsg = notWorkMsg ;
            }
        }

        if (StringUtils.isNotBlank(errorLimitWork)) {
            String errorLimitWorkMsg=StringUtils.format(I18nUtil.getMessage("ui.data.column.scheduleResult.luMachineCode.errorLimitWork"), errorLimitWork,limitMachineName);
            if (StringUtils.isNotBlank(resultMsg)) {
                resultMsg = resultMsg + "<br/>"+errorLimitWorkMsg;
            }else{
                resultMsg = errorLimitWorkMsg ;
            }
        }
        return AjaxResult.success(resultMsg);
    }

    /**
     * 修改-使用模数
     * 更换类型：硫化机code1:拆模换code:模数molds;硫化机code2:点数换code:模数molds;
     */
    public AjaxResult modifyMolds(CxScheduleResult entity) {
        //初始值定义
        List<LhMachineChangeTpye> lhMachineChangeTpyeList = entity.getLhMachineChangeTpyeList();
        CxScheduleResult cxScheduleResult0 = cxScheduleResultMapper.selectCxScheduleResultById(entity.getId());
        CxScheduleResult oldScheduleResult = (CxScheduleResult)cxScheduleResult0.clone();
        CxScheduleResult osEntity=new CxScheduleResult();
        BeanUtils.copyProperties(cxScheduleResult0,osEntity);
        //原硫化机台数组
        String osLhMachineCode = cxScheduleResult0.getLhMachineCode();
        List<String> osLhMachineCodeArr = null;
        if (StringUtils.isNotEmpty(osLhMachineCode)) {
            osLhMachineCodeArr = Arrays.asList(osLhMachineCode.split(","));
        }

        //硫化机台名称map
        CxMachineInfo machineInfo = new CxMachineInfo();
        machineInfo.setId(6L);
        List<CxMachineInfo> list3 = cxMachineInfoMapper.selectCxMachineInfoList2(machineInfo);
        Map<String, String> machineNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(list3)) {
            machineNameMap = list3.stream().collect(Collectors.toMap(a -> a.getMachineCode(), a -> a.getMachineName()));
        }

        //更新硫化机台、使用模数、硫化机变更类型描述（覆盖更新）
        Double lhMachineQty = 0d;
        String newLhMachineCode = "";
        String lhMachineChangeMoldDesc = "";
        for (LhMachineChangeTpye item : lhMachineChangeTpyeList) {
            lhMachineQty = lhMachineQty + item.getMolds();
            if (StringUtils.isEmpty(newLhMachineCode)) {
                newLhMachineCode = item.getMachineCode();
            } else {
                newLhMachineCode = newLhMachineCode + "," + item.getMachineCode();
            }
            if (StringUtils.isEmpty(lhMachineChangeMoldDesc)) {
                lhMachineChangeMoldDesc = item.getMachineCode() + ":" + item.getChangeType() + ":" + item.getMolds() + ";";
            } else {
                lhMachineChangeMoldDesc = lhMachineChangeMoldDesc + item.getMachineCode() + ":" + item.getChangeType() + ":" + item.getMolds() + ";";
            }
        }

        //更新
        boolean flag = false;
        if (StringUtils.isNotEmpty(cxScheduleResult0.getLhMachineChangeMoldDesc())) {
            List<String> desc1 = Arrays.asList(cxScheduleResult0.getLhMachineChangeMoldDesc().split(";"));
            List<String> desc2 = Arrays.asList(lhMachineChangeMoldDesc.split(";"));
            flag = compare(desc1, desc2);
        }
        if (!flag && StringUtils.isNotEmpty(lhMachineChangeMoldDesc)) {
            cxScheduleResult0.setTaskType(TaskTypeEnum.MOLD.getTaskType());
        }
        cxScheduleResult0.setLhMachineQty(lhMachineQty);
        cxScheduleResult0.setLhMachineCode(newLhMachineCode);
        cxScheduleResult0.setLhMachineChangeMoldDesc(lhMachineChangeMoldDesc);
        this.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, oldScheduleResult, cxScheduleResult0);  //如果是调度员操作，则需要增加操作日志
        updateCxScheduleResultForMolds(cxScheduleResult0,osEntity);
        //Joran 2021-09-02 调用单排程模具变动单记录生成
        lhEngineService.singleMoldChangePlanTask(cxScheduleResult0, osLhMachineCodeArr);
        // 批量保存成型排程硫化机台调整记录
        batchSaveCxChangeLhMachine(lhMachineChangeTpyeList, entity.getOrderNo(), entity.getScheduleDate());
        return AjaxResult.success();
    }

    /**
     * 批量保存成型排程硫化机台调整记录
     * @param lhMachineChangeTpyeList 记录
     */
    private void batchSaveCxChangeLhMachine(List<LhMachineChangeTpye> lhMachineChangeTpyeList, String orderNo, Date scheduleDate) {
        cxEngineChangeLhMachineService.deleteChangeLhMachineByScheduleDate(null, null, orderNo);
        List<CxChangeLhMachine> list = new ArrayList<>();
        for (LhMachineChangeTpye lhMachineChangeTpye : lhMachineChangeTpyeList) {
            CxChangeLhMachine machine = new CxChangeLhMachine();
            machine.setLhMachineCode(lhMachineChangeTpye.getMachineCode());
            machine.setChangeType(lhMachineChangeTpye.getChangeType());
            machine.setEmbryoStock(lhMachineChangeTpye.getEmbryoStock());
            machine.setChangeMoldTime(lhMachineChangeTpye.getChangeMoldTime());
            machine.setUseMoldNum(lhMachineChangeTpye.getMolds());
            machine.setCxOrderNo(orderNo);
            machine.setScheduleDate(scheduleDate);
            machine.setDataSource("0");
            machine.setBaseVale(null);
            list.add(machine);
        }
        cxEngineChangeLhMachineService.batchInsertCxChangeLhMachine(list);
    }

    /**
     * 在产下发MPS
     */
    @Transactional
    public AjaxResult producingIssue(CxScheduleResult entity) {
        if (entity.getScheduleDate() == null) {
            entity.setScheduleDate(new Date());
        }
        List<CxScheduleResult> list = cxScheduleResultMapper.producingIssue(entity);
        String date = DateUtils.parseDateToStr("yyyy-MM", entity.getScheduleDate());
        String year = date.substring(0, 4);
        String month = date.substring(5);
        factoryProductionProductMapper.deleteByYearAndMonth(year, month);
        for (CxScheduleResult item : list) {
            CxFactoryProductionProduct cxFactoryProductionProduct = new CxFactoryProductionProduct();
            cxFactoryProductionProduct.setBaseVale(null);
            cxFactoryProductionProduct.setYear(Long.valueOf(year));
            cxFactoryProductionProduct.setMonth(Long.valueOf(month));
            cxFactoryProductionProduct.setMachineCode(item.getCxMachineName());
            cxFactoryProductionProduct.setProductCode(item.getSapCode());
            cxFactoryProductionProduct.setConstructionCode(item.getEmbryoCode());
            factoryProductionProductMapper.insertCxFactoryProductionProduct(cxFactoryProductionProduct);
        }
        return AjaxResult.success();
    }

    /**
     * 单机自动排程校验
     */
    public List<CxScheduleResult> singleMachinAutoPlanValidate(CxScheduleResult cxScheduleResult){
        return cxScheduleResultMapper.singleMachinAutoPlanValidate(cxScheduleResult);
    }

    /**
     * 生成模具变动单校验
     */
    public AjaxResult modelChangeValidate(CxScheduleResult entity){
        int a=cxScheduleResultMapper.modelChangeValidate(entity);
        if(a>0){
            return AjaxResult.success("0");
        }else{
            return AjaxResult.success();
        }
    }

    /**
     * 验证成型排程结果表中是否存在施工版本为空的数据
     * @param cxScheduleResultList
     * @return
     */
    @Override
    public String checkBomDataVersion(List<CxScheduleResult> cxScheduleResultList) {
        StringBuilder errorMsg=new StringBuilder();
        if(StringUtils.isNotEmpty(cxScheduleResultList)){
            for(CxScheduleResult cxScheduleResult:cxScheduleResultList){
                if(StringUtils.isEmpty(cxScheduleResult.getBomDataVersion())){
                    errorMsg.append(StringUtils.format(I18nUtil.getMessage("cx.publish.bomDataVersion.empty.error"),cxScheduleResult.getSapCode(),cxScheduleResult.getEmbryoCode(),cxScheduleResult.getCxMachineName())).append("<br/>");
                    break;
                }
            }
        }
        return errorMsg.toString();
    }

    /**
     * 删除排程校验结果
     * @param ids
     * @return
     */
    @Override
    public String removeResultCheck(Long[] ids,List<CxScheduleResult> finalList) {
       if(StringUtils.isEmpty(ids)){
           throw new IllegalArgumentException(I18nUtil.getMessage("cx.schedule.result.remove.error.params"));
       }
       List<CxScheduleResult> removeList=cxScheduleResultMapper.selectRemoveList(ids);
       if(StringUtils.isEmpty(removeList)){
           throw new IllegalArgumentException(I18nUtil.getMessage("cx.schedule.result.remove.error.params"));
       }
       StringBuilder errorLog=new StringBuilder();

       //遍历进行校验提醒
       for(CxScheduleResult cxScheduleResult:removeList){
           //硫化状态
           String taskType=cxScheduleResult.getTaskType();
           //成型状态
           String productStatus=cxScheduleResult.getProductionStatus();
           //发布状态
           String isRelease=cxScheduleResult.getIsRelease();
           if(CxEngineConstants.IS_PUBLISH_YES.equals(isRelease)&&StringUtils.isNotEmpty(isRelease)){
               errorLog.append(StringUtils.format(I18nUtil.getMessage("cx.schedule.result.remove.isPublish.yes"),cxScheduleResult.getOrderNo()));
               continue;
           }
//           if(!CxEngineConstants.PRODUCTION_STATUS_UNDO.equals(productStatus)&&StringUtils.isNotEmpty(productStatus)){
//               errorLog.append(StringUtils.format(I18nUtil.getMessage("cx.schedule.result.productStatus.isNotUnDo.yes"),cxScheduleResult.getOrderNo()));
//               continue;
//           }
//           if(!"0".equals(taskType)&&!CxEngineConstants.TASK_TYPE_TODO.equals(taskType)&&StringUtils.isNotEmpty(taskType)){
//               errorLog.append(StringUtils.format(I18nUtil.getMessage("cx.schedule.result.taskType.isNotUnDo.yes"),cxScheduleResult.getOrderNo()));
//               continue;
//           }
           finalList.add(cxScheduleResult);
       }
        return errorLog.toString();
    }

    /**
     * 进行数据删除
     * @param ids
     * @param removeList
     * @return
     */
    @Override
    public int removeCxSecheduleResultByList(Long[] ids,List<CxScheduleResult> removeList) {
        //删除成型排程结果表
        int result=deleteCxScheduleResultByIds(ids);
        if(result>0&&StringUtils.isNotEmpty(removeList)){
            for(CxScheduleResult cxScheduleResult:removeList){
                String removeDate=DateUtils.parseDateToStr("yyyyMMdd",cxScheduleResult.getScheduleDate());
                String sourceCxOrder=cxScheduleResult.getOrderNo();
                //Joran 2021-12-13 删除模具变动单临时表数据
                cxLhEngineCommonMapper.syncMoldChagePlanToLog(removeDate,sourceCxOrder);
                cxLhEngineCommonMapper.deleteLhEngineMoldChangePlanByScheduleDate(removeDate,sourceCxOrder);
            }
        }

        return result;
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询成型排程结果）
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByDate(Date scheduleDate) {
        return cxScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询硫化排程结果）
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @Override
    public int lhIsReleasingOrTimeoutByDate(Date scheduleDate) {
        return cxScheduleResultMapper.lhIsReleasingOrTimeoutByDate(scheduleDate);
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return cxScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 更改发布状态
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(CxScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_CX);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getIsRelease());
        cxScheduleResultMapper.updatePublishRecord(record);
        // 更新投产状态中待发布的状态变更为已投产
        this.updateProductStatus(record.getPublishStatus());
        return cxScheduleResultMapper.changeReleaseStatus(entity);
    }

    /**
     * 验证施工
     * @param ids
     * @return
     */
    @Override
    public String validateConstructionByIds(Long[] ids) {
        //Joran 2022-03-16 从成型参数中获取开关如果是Y则进行验证，否则直接返回成功start
        Map<String,String> cxParams=commonCacheService.loadCxParamsMap();
        String switchConfig= CxEngineConstants.NO;
        if(cxParams.containsKey(CxParamCodeConstants.VALIDATE_CONSTRUCTION_SWITCH)){
            switchConfig=cxParams.get(CxParamCodeConstants.VALIDATE_CONSTRUCTION_SWITCH);
        }
        //Joran 2022-03-16 从成型参数中获取开关如果是Y则进行验证，否则直接返回成功end
        StringBuilder msg=new StringBuilder();
        //用于存储已经校验过的数据防止重复校验
        Set<String> existsKey=new TreeSet<>();
        if(CxEngineConstants.YES.equals(switchConfig)){
            List<CxScheduleResult> validateResultList=cxScheduleResultMapper.selectRemoveList(ids);
            if(StringUtils.isNotEmpty(validateResultList)){
                for(CxScheduleResult cxScheduleResult:validateResultList){
                    String embryoCode=cxScheduleResult.getEmbryoCode();
                    String bomDataVersion=cxScheduleResult.getBomDataVersion();
                    String key= GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                    if(existsKey.contains(key)){
                        log.debug("当前键值："+key+",已经验证过。不重复校验");
                        continue;
                    }
                    String errorMsg=cxCheckConstructionService.checkConstruction(embryoCode,bomDataVersion);
                    if(StringUtils.isNotEmpty(errorMsg)){
                        if(StringUtils.isEmpty(msg)){
                            msg.append(errorMsg);
                        }else{
                            msg.append("<br/>").append(errorMsg);
                        }
                    }
                    existsKey.add(key);
                }
            }
        }

        return msg.toString();
    }

    /**
     * 查询成型排程最新排程日期
     *
     * @return 最新排程日期
     */
    @Override
    public Date selectMaxScheduleDate() {
        return cxScheduleResultMapper.selectMaxScheduleDate();
    }

    @Override
    public int isPublishByIds(Long[] ids) {
        return cxScheduleResultMapper.isPublishByIds(ids);
    }

    /**
     * 查询成型排程机台甘特图数据
     */
    public List<Gante> getCxGanteData(Gante gante) {
        //机台甘特图
        List<Gante> newGanteList=new ArrayList<>();
        if (gante.getFlag() == 1) {
            List<Gante> ganteList = cxScheduleResultMapper.getCxGanteData(gante);
            //构造开始日、结束日、开始时刻、结束时刻，遇到跨月时截掉
            if (CollectionUtils.isNotEmpty(ganteList)) {
                for (Gante item : ganteList) {

                    //构造开始日、结束日、开始时刻、结束时刻、起点位置、时差;
                    String scheduleDay = DateUtils.getDay(item.getScheduleDate())+"";
                    String startDay = DateUtils.getDay(item.getStartDate())+"";
                    String endDay = DateUtils.getDay(item.getEndDate())+"";
                    int startHours = DateUtils.getHour(item.getStartDate());
                    int endHours = DateUtils.getHour(item.getEndDate());
                    int dayInterval = DateUtils.getDayInterval(item.getEndDate(), item.getStartDate());
                    int dayInterval2 = DateUtils.getDayInterval(item.getScheduleDate(), item.getStartDate());

                    //计算以下三个值，用户画甘特图
                    //算起点位置：后端给72小时制的起始时刻
                    //算长条宽度：小时差*25：(endHour-startHour+1)*25，后端给时差;
                    //算margin-left宽度：固定值*天数，不用后端给


                    if (dayInterval2>0){ //起始日期在排程日期前
                        item.setHourStart(startHours);
                    }else if (dayInterval2==0){ //起始日期就是排程日期
                        item.setHourStart(startHours+24);
                    }else{ //起始日期在排程日期后
                        item.setHourStart(startHours+48);
                    }

                    //跨天存在前一天数据
                    if (!startDay.equals(endDay) && scheduleDay.equals(endDay)){
                        item.setHourInterval(24-startHours+endHours);
                        //跨多天
                        if (dayInterval>1){
                            item.setHourInterval(24-startHours+24*(dayInterval-1)+endHours);
                        }
                    }else if (!startDay.equals(endDay) && !scheduleDay.equals(endDay)) {
                        item.setHourInterval(24-startHours+endHours);
                        //跨多天
                        if (dayInterval>1){
                            item.setHourInterval(24-startHours+24*(dayInterval-1)+endHours);
                        }
                    }else{
                        item.setHourInterval(endHours-startHours);
                    }

                    item.setStartDay(startDay + "");
                    item.setEndDay(endDay + "");
                    item.setStartHour(startHours + "");
                    item.setEndHour(endHours + "");
                    newGanteList.add(item);
                }
            }
            return newGanteList;
        } else if (gante.getFlag() == 2) {
            //规格甘特图
            List<Gante> ganteList = cxScheduleResultMapper.getCxSpecGanteData(gante);
            //构造开始日、结束日
            if (CollectionUtils.isNotEmpty(ganteList)) {
                for (Gante item : ganteList) {
                    //判断是否跨月
                    int scheduleMonth = DateUtils.getMonth(item.getScheduleDate());
                    int startMonth = DateUtils.getMonth(item.getStartDate());
                    int endMonth =  DateUtils.getMonth(item.getEndDate());
                    if (startMonth != scheduleMonth && endMonth != scheduleMonth) {
                        continue;
                    }
                    //构造开始日、结束日
                    String startDay = DateUtils.getDay(item.getStartDate())+"";
                    String endDay = DateUtils.getDay(item.getEndDate())+"";
                    item.setStartDay(startDay);
                    item.setEndDay(endDay);

                    //判断是否跨月
                    if (startMonth != endMonth && endMonth == scheduleMonth) { //月初跨月
                        item.setStartDay("1");
                    }
                    if (startMonth != endMonth && startMonth == scheduleMonth) {  //月末跨月
                        item.setEndDay(DateUtils.getLastDay(item.getStartDate()).substring(8));
                    }
                    newGanteList.add(item);
                }
            }
            return newGanteList;
        }
        return new ArrayList<>();
    }


}
