package com.zlt.aps.dj.service.impl;

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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.security.aspect.PreAuthorizeAspect;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.core.enums.HalfComponentFinishTableEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.common.engine.service.impl.BaseFinishQtyImportService;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjDispatcherLog;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;
import com.zlt.aps.dj.engine.service.DjEngineNewService;
import com.zlt.aps.dj.engine.service.DjEngineService;
import com.zlt.aps.dj.engine.vo.DjScheduleResultVo;
import com.zlt.aps.dj.mapper.DjCurlRollMapper;
import com.zlt.aps.dj.mapper.DjParamsMapper;
import com.zlt.aps.dj.mapper.DjScheduleResultMapper;
import com.zlt.aps.dj.service.DjDispatcherLogService;
import com.zlt.aps.dj.service.DjMachineInfoService;
import com.zlt.aps.dj.service.DjScheduleResultService;
import com.zlt.aps.utils.BillUtils;
import com.zlt.bill.common.service.AbstractBillService;

/**
 * 垫胶胶排程结果Service业务层处理
 *
 * @author zlt
 * @date 2026-06-24
 */
@Service
public class DjScheduleResultServiceImpl extends AbstractBillService<DjScheduleResult>
        implements DjScheduleResultService {
    @Resource
    private DjScheduleResultMapper djScheduleResultMapper;

    @Resource
    private DjEngineNewService djEngineService;

    @Autowired
    private DjMachineInfoService machineInfoService;

    @Resource
    private PreAuthorizeAspect preAuthorizeAspect;

    @Resource
    private DjDispatcherLogService djDispatcherLogService;

    /**
     * 默认标准长度
     */
    private static final String DEFAULT_STANDARD_LENGTH = "80";
    @Autowired
    private DjCurlRollMapper curlRollMapper;
    @Autowired
    private DjParamsMapper paramsMapper;

    /**
     * 查询垫胶排程结果
     *
     * @param id 垫胶排程结果ID
     * @return 垫胶排程结果
     */
    @Override
    public DjScheduleResult selectDjScheduleResultById(Long id) {
        return djScheduleResultMapper.selectById(id);
    }

    /**
     * 查询垫胶排程结果列表
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 垫胶排程结果
     */
    @Override
    public List<DjScheduleResult> selectDjScheduleResultList(DjScheduleResult djScheduleResult) {
        QueryWrapper<DjScheduleResult> queryWrapper = BillUtils.builderCondition(djScheduleResult);
        List<DjScheduleResult> list = djScheduleResultMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<DjMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(new DjMachineInfo());
        Map<String, DjMachineInfo> machineInfoMap = machineInfoList.stream()
                .collect(Collectors.toMap(DjMachineInfo::getMachineCode, Function.identity(), (s1, s2) -> s1));
        if (CollectionUtils.isNotEmpty(list)) {
            for (DjScheduleResult scheduleResult : list) {
                String machineCodeStr = scheduleResult.getMachineCode();
                if (StringUtils.isNotBlank(machineCodeStr)) {
                    List<String> machineNameList = new ArrayList<>();
                    String[] machineIdArr = machineCodeStr.split(",");
                    for (String machineCode : machineIdArr) {
                        String key = machineCode;
                        if (machineInfoMap.containsKey(key)) {
                            DjMachineInfo machineInfo = machineInfoMap.get(key);
                            machineNameList.add(machineInfo.getMachineName());
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * 新增垫胶排程结果
     *
     * @param djScheduleResult 垫胶排程结果
     * @return 结果
     */
    @Override
    public int insertDjScheduleResult(DjScheduleResult djScheduleResult) {
        djScheduleResult.setBaseVale(null);
        DjScheduleResultVo scheduleVo = new DjScheduleResultVo();
        BeanUtils.copyProperties(djScheduleResult, scheduleVo);
//        return djEngineService.insertDjOrder(scheduleVo);
        return 0;
    }

    /**
     * 修改垫胶排程结果
     *
     * @param scheduleResult 垫胶排程结果
     * @return 结果
     */
    @Override
    public int updateDjScheduleResult(DjScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getReleaseStatus())
                || !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getReleaseStatus())
                || StringUtils.isEmpty(scheduleResult.getReleaseStatus())) {
            DjScheduleResult scheduleResult2 = djScheduleResultMapper.selectById(scheduleResult.getId());
            boolean flag = compare(scheduleResult2.getMachineCode(), scheduleResult.getMachineCode());
            flag = flag && Objects.compare(scheduleResult2.getClass1PlanQty(), scheduleResult.getClass1PlanQty(),
                    BigDecimal::compareTo) != 0;
            flag = flag && Objects.compare(scheduleResult2.getClass2PlanQty(), scheduleResult.getClass2PlanQty(),
                    BigDecimal::compareTo) != 0;
            flag = flag && Objects.compare(scheduleResult2.getClass3PlanQty(), scheduleResult.getClass3PlanQty(),
                    BigDecimal::compareTo) != 0;
            flag = flag && compare(scheduleResult2.getClass1Analysis(), scheduleResult.getClass1Analysis());
            flag = flag && compare(scheduleResult2.getClass2Analysis(), scheduleResult.getClass2Analysis());
            flag = flag && compare(scheduleResult2.getClass3Analysis(), scheduleResult.getClass1Analysis());
            flag = flag && compare(scheduleResult2.getRemark(), scheduleResult.getRemark());
            if (!flag) {
                scheduleResult.setReleaseStatus(scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE
                        : ApsConstant.WAIT_RELEASING);
            }
        }
        LambdaUpdateWrapper<DjScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(DjScheduleResult::getUpdateBy, scheduleResult.getUpdateBy());
        updateWrapper.set(DjScheduleResult::getUpdateTime, scheduleResult.getUpdateTime());
        updateWrapper.set(DjScheduleResult::getReleaseStatus, scheduleResult.getReleaseStatus());
        updateWrapper.eq(DjScheduleResult::getId, scheduleResult.getId());
        return djScheduleResultMapper.update(scheduleResult, updateWrapper);
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * 
     * @param operType    操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    @Override
    public void insertDispatcherLog(String operType, DjScheduleResult newSchedule) {
        // 20231018 需求确认单各个工序中，调度员操作日志，改成排程操作日志，统计全部人员的操作记录，调度员字段改为“操作人员”字段
        // if(!preAuthorizeAspect.hasRole(ApsConstant.DISPATCHER_ROLE)) {
        // return;
        // }
        DjScheduleResult oldSchedule = this.djScheduleResultMapper.selectById(newSchedule.getId()); // 操作前的排程数据
        // 构建日志并保存
        djDispatcherLogService.saveBill(this.buildDispatcherLog(operType, newSchedule, oldSchedule));
    }

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     *
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    @Override
    public void insertDispatcherLogInsertOrder(String operType, List<DjScheduleResult> scheduleResults,
            DjScheduleResult newSchedule) {
        List<DjScheduleResult> scheduleResultList = this.selectByScheduleDateAndCode(newSchedule);
        // 基础信息赋值
        newSchedule.setId(scheduleResultList.get(0).getId());
        // 操作前的信息赋值，取创建时间最大的记录为操作前信息
        DjScheduleResult oldSchedule = null;
        if (CollectionUtils.isNotEmpty(scheduleResults)) {
            oldSchedule = scheduleResults.stream().max(Comparator.comparing(DjScheduleResult::getCreateTime))
                    .orElse(null);
        }
        // 构建日志并保存
        djDispatcherLogService.saveBill(this.buildDispatcherLog(operType, newSchedule, oldSchedule));
    }

    /*
     * 构建排产操作操作日志
     */
    private DjDispatcherLog buildDispatcherLog(String operType, DjScheduleResult newSchedule,
            DjScheduleResult oldSchedule) {
        DjDispatcherLog log = new DjDispatcherLog();
        // 基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate()); // 排程日期
        log.setMaterialCode(newSchedule.getPaddingCode()); // 垫胶代码
        // 操作前的信息赋值
        if (oldSchedule != null) {
            log.setBeforeMachineCode(oldSchedule.getMachineCode());
            log.setBeforeClass1PlanQty(oldSchedule.getClass1PlanQty());
            log.setBeforeClass2PlanQty(oldSchedule.getClass2PlanQty());
            log.setBeforeClass3PlanQty(oldSchedule.getClass3PlanQty());
        }
        // 操作后的信息赋值
        log.setBeforeMachineCode(newSchedule.getMachineCode());
        log.setBeforeClass1PlanQty(newSchedule.getClass1PlanQty());
        log.setBeforeClass2PlanQty(newSchedule.getClass2PlanQty());
        log.setBeforeClass3PlanQty(newSchedule.getClass3PlanQty());
        return log;
    }

    /**
     * 根据排程日期和代码查询排程结果
     * 
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    @Override
    public List<DjScheduleResult> selectByScheduleDateAndCode(DjScheduleResult scheduleResult) {
        return djScheduleResultMapper.selectList(BillUtils.builderCondition(scheduleResult));
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
     * 批量删除垫胶排程结果
     *
     * @param ids 需要删除的垫胶排程结果ID
     * @return 结果
     */
    @Override
    public int deleteDjScheduleResultByIds(Long[] ids) {
        return djScheduleResultMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 删除垫胶排程结果信息
     *
     * @param id 垫胶排程结果ID
     * @return 结果
     */
    @Override
    public int deleteDjScheduleResultById(Long id) {
        return djScheduleResultMapper.deleteById(id);
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_DJ);
        record.setScheduleDate(scheduleDate);
        record.setPublishStatus(ApsConstant.RELEASING);
        record.setDataVersion(dataVersion);
        this.deployDjScheduleToMid(ids, dataVersion, factoryCode, companyCode); // 把排程数据发布到中间库，并通知MES
//        djScheduleResultMapper.insert(record);
//        return djScheduleResultMapper.batchUpdate(Arrays.stream(ids).boxed().collect(Collectors.toList()),
//                ApsConstant.RELEASING);
        return 0;
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
        LambdaUpdateWrapper<DjScheduleResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(DjScheduleResult::getReleaseStatus, status);
        updateWrapper.in(DjScheduleResult::getId, ids);
        djScheduleResultMapper.update(null, updateWrapper);
//        djScheduleResultMapper.updatePublishRecordVersion(dataVersion, status);
    }

    /**
     * 把排程数据发布到中间库
     *
     * @param ids 排程id
     */
    private void deployDjScheduleToMid(long[] ids, String dataVersion, String factoryCode, String companyCode) {
        if (ids == null) {
            return;
        }
        // TODO 调用itf接口
//        djScheduleResultMapper.deployDjScheduleToMid(dataVersion, ids, factoryCode, companyCode); // 把排程数据同步到接口中间库中
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
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_DJ);
        record.setScheduleDate(scheduleDate);
//        return djScheduleResultMapper.isPublish(record) > 0;
        return true;
    }

    /**
     * 唯一性校验
     */
    @Override
    public List<DjScheduleResult> checkUnique(DjScheduleResult entity) {
        return djScheduleResultMapper.selectList(BillUtils.builderCondition(entity));
    }

    /**
     * 导入数据，并保存记录
     */
    @Override
    @Transactional
    public AjaxResult importData(List<DjScheduleResult> list, Long importLogId, String scheduleDate) {

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<DjScheduleResult> importList = new ArrayList<>();
        DjMachineInfo djMachineInfo = new DjMachineInfo();
        djMachineInfo.setStatus("0");
        List<DjMachineInfo> machineInfoList = machineInfoService.selectMachineInfoList(djMachineInfo);
        if (CollectionUtils.isEmpty(machineInfoList)) {
            String message = I18nUtil.getMessage("ui.error.message.column.machineIsNull");
            addImportErrorLog(importLogId, null, message, importErrorLogs);
            return AjaxResult.error(message, importErrorLogs);
        }

        // 根据机台名称去重
        TreeSet<DjMachineInfo> treeSet = new TreeSet<DjMachineInfo>(new Comparator<DjMachineInfo>() {
            @Override
            public int compare(DjMachineInfo o1, DjMachineInfo o2) {
                return o1.getMachineName().compareTo(o2.getMachineName());
            }
        });
        treeSet.addAll(machineInfoList);
        machineInfoList = new ArrayList<>(treeSet);

        Map<String, Long> machineCodeMap = machineInfoList.stream()
                .collect(Collectors.toMap(DjMachineInfo::getMachineName, DjMachineInfo::getId));
        // 按业务主键分组
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(a -> (a.getPaddingCode() + a.getMachineCode()), Collectors.counting()));

        // 遍历校验
        for (int i = 0; i < list.size(); i++) {
            DjScheduleResult entity = list.get(i);
            entity.setDataSource("2");
            entity.setScheduleDate(DateUtils.dateTime("yyyy-MM-dd", scheduleDate));

            // 重复记录校验
            Long hasValue = groupMap.get(entity.getPaddingCode() + entity.getMachineCode());
            if (hasValue > 1) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.quota.liningCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 3, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 3, entity);
            // 机台code 转为机台id
            if (entity.getMachineCode() != null && entity.getMachineCode().indexOf(",") > 0) {
                String message = I18nUtil.getMessage("ui.data.column.machine.produceLineValidate");
                message = String.format(message, i + 3,
                        I18nUtil.getMessage("ui.data.column.scheduleResult.produceLine"));
                addImportErrorLog(importLogId, i + 3, message, validated);
            }
            if (machineCodeMap.get(entity.getMachineCode()) == null) {
                addImportErrorLog(importLogId, i + 3,
                        I18nUtil.getMessage("ui.error.message.column.produceLineNotExist"), validated);
            }

            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                entity.setMachineCode(machineCodeMap.get(entity.getMachineCode()) + "");
                successNum++;
                entity.setBaseVale(null);
                importList.add(entity);
            }
        }
        this.batchSaveDjSchedule(scheduleDate, importList); // 把验证成功的记录进行导入

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
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
    private void batchSaveDjSchedule(String scheduleDate, List<DjScheduleResult> importList) {
        List<DjScheduleResultVo> scheduleList = new ArrayList<>();
        for (DjScheduleResult result : importList) {
            DjScheduleResultVo vo = new DjScheduleResultVo();
            BeanUtils.copyProperties(result, vo);
            scheduleList.add(vo);
        }
//        if (!scheduleList.isEmpty()) {
//            this.djEngineService.batchSaveDjSchedule(scheduleDate, scheduleList);
//        }
    }

    /**
     * 选机台
     */
    @Override
    public AjaxResult chooseMachine(DjScheduleResult scheduleResult) {
        if (CollectionUtils.isNotEmpty(this.checkUnique(scheduleResult))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.already.exists"));
        }
//        this.djEngineService.confirmDjMachine(scheduleResult); // 确认自动排程机台
        scheduleResult.setReleaseStatus(
                scheduleResult.getPublishSuccessCount() == 0 ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
//        djScheduleResultMapper.update(scheduleResult, updateWrapper);
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
//        return djScheduleResultMapper.isReleasingOrTimeoutByDate(scheduleDate);
        return 0;
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
//        return djScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
        return 0;
    }

    /**
     * 更改发布状态
     *
     * @param entity 排程日期
     * @return 结果
     */
    @Override
    public int changeReleaseStatus(DjScheduleResult entity) {
        SchedulePublishRecord record = new SchedulePublishRecord();
        record.setBaseVale(1L);
        record.setProcedureCode(ApsConstant.PROCEDURE_CODE_DJ);
        record.setScheduleDate(entity.getScheduleDate());
        record.setPublishStatus(entity.getReleaseStatus());
//        djScheduleResultMapper.updatePublishRecord(record);
//        return djScheduleResultMapper.changeReleaseStatus(entity);
        return 0;
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
//        return djScheduleResultMapper.combinationMiddleAndNight(map);
        return 0;
    }

    @Override
    public int checkDjCodeExist(DjScheduleResult djScheduleResult) {
//        return djScheduleResultMapper.checkDjCodeExist(djScheduleResult);
        return 0;
    }

    @Override
    public int isPublishByIds(Long[] ids) {
//        return djScheduleResultMapper.isPublishByIds(ids);
        return 0;
    }

    @Override
    public List<DjScheduleResult> selectByIds(List<Long> ids2) {
//        return djScheduleResultMapper.selectByIds(ids2);
        return null;
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
    public AjaxResult importFinishQty(List<DjDayFinishQty> list, Long importLogId) {
        return baseFinishQtyImportService.importFinishQty(list, importLogId, HalfComponentFinishTableEnum.DJ);
    }

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @Override
    public AjaxResult getSummaryVo(DjScheduleResult scheduleResult) {
        
        List<DjScheduleResult> djScheduleResultList = selectDjScheduleResultList(scheduleResult);
//        List<DjScheduleResult> lastDayPlanQty4List = djScheduleResultMapper.getLastDayPlanQty4List(scheduleResult);
        List<DjScheduleResult> lastDayPlanQty4List = new ArrayList<>();
        // 添加昨日排程有，今日排程没有的物料对象，用于后续计算理论交接班库存合计
        List<String> resultCodeList = djScheduleResultList.stream().map(DjScheduleResult::getPaddingCode)
                .collect(Collectors.toList());
        List<String> notExistCodeList = lastDayPlanQty4List.stream().map(DjScheduleResult::getPaddingCode)
                .filter(item -> !resultCodeList.contains(item)).collect(Collectors.toList());
        
        if (CollectionUtils.isNotEmpty(notExistCodeList)) {
            LambdaQueryWrapper<DjCurlRoll> curlRollQueryWrapper = new LambdaQueryWrapper<>();
            curlRollQueryWrapper.in(DjCurlRoll::getPaddingCode, notExistCodeList);
            List<DjCurlRoll> curlRollList = curlRollMapper.selectList(curlRollQueryWrapper);
            Map<String, BigDecimal> curlRollMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(curlRollList)) {
                curlRollMap = curlRollList.stream()
                        .collect(Collectors.toMap(DjCurlRoll::getPaddingCode, DjCurlRoll::getCurlLength));
            }
        }

        LambdaQueryWrapper<DjParams> paramWrapper = new LambdaQueryWrapper<>();
        paramWrapper.eq(DjParams::getParamCode, EngineConstants.STANDARD_CRIMP_LENGTH);
        DjParams standardLengthParams = paramsMapper.selectOne(paramWrapper);
        for (String code : notExistCodeList) {
            DjScheduleResult result = new DjScheduleResult();
            result.setPaddingCode(code);
            BigDecimal standardLength = standardLengthParams == null ? new BigDecimal(DEFAULT_STANDARD_LENGTH)
                    : new BigDecimal(standardLengthParams.getParamValue());
            djScheduleResultList.add(result);
        }
        Map<String, DjScheduleResult> lastDayPlanMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(lastDayPlanQty4List)) {
            lastDayPlanMap = lastDayPlanQty4List.stream()
                    .collect(Collectors.toMap(DjScheduleResult::getPaddingCode, Function.identity()));
        }
//        List<DjScheduleResult> cxConsume4List = djScheduleResultMapper.getCxConsume4List(scheduleResult);
        Map<String, DjScheduleResult> cxConsumeMap = new HashMap<>(16);
//        if (CollectionUtils.isNotEmpty(cxConsume4List)) {
//            cxConsumeMap = cxConsume4List.stream()
//                    .collect(Collectors.toMap(DjScheduleResult::getPaddingCode, Function.identity()));
//        }
        BigDecimal totalDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalNightPlanQty = BigDecimal.ZERO;
        BigDecimal totalNextDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalStockQty = BigDecimal.ZERO;
        BigDecimal totalLastDayPlanQty = BigDecimal.ZERO;
        BigDecimal totalTheoreticClassStockQty = BigDecimal.ZERO;

        for (DjScheduleResult result : djScheduleResultList) {
//            Double stockQty = ObjectUtils.defaultIfNull(result.getStockQty(), 0D);
//            BigDecimal nightPlanQty = BigDecimalUtils.valueOf(result.getClass1PlanQty());
//            String code = result.getPaddingCode();
//            if (lastDayPlanMap.containsKey(code)) {
//                DjScheduleResult lastDayResult = lastDayPlanMap.get(code);
//                result.setLastMidPlanQty(lastDayResult.getLastMidPlanQty());
//            }
//            if (cxConsumeMap.containsKey(code)) {
//                DjScheduleResult cxConsumeResult = cxConsumeMap.get(code);
//                result.setCxConsumeQty(cxConsumeResult.getCxConsumeQty());
//            }
//            Double lastMidPlanQty = result.getLastMidPlanQty();
//            Double cxConsumeQty = result.getCxConsumeQty();
//            // 理论交班库存计算,理论交班库存 = 库存 + 昨日早班 + 夜班 - 成型消耗量
//            if (lastMidPlanQty != null && cxConsumeQty != null) {
//                result.setTheoreticClassStockQty(stockQty + lastMidPlanQty + nightPlanQty - cxConsumeQty);
//            }
//
//            totalDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getDayPlanQty(), 0D),
//                    totalDayPlanQty);
//            totalNightPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNightPlanQty(), 0D),
//                    totalNightPlanQty);
//            totalNextDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getNextDayPlanQty(), 0D),
//                    totalNightPlanQty);
//            totalStockQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getStockQty(), 0D), totalStockQty);
//            totalLastDayPlanQty = BigDecimalUtils.add(ObjectUtils.defaultIfNull(result.getLastMidPlanQty(), 0D),
//                    totalLastDayPlanQty);
//            totalTheoreticClassStockQty = BigDecimalUtils.add(
//                    ObjectUtils.defaultIfNull(result.getTheoreticClassStockQty(), 0D), totalTheoreticClassStockQty);
        }
        ScheduleSummaryVo scheduleSummaryVo = new ScheduleSummaryVo();
        scheduleSummaryVo.setDayPlanQty(totalDayPlanQty.doubleValue());
        scheduleSummaryVo.setNightPlanQty(totalNightPlanQty.doubleValue());
        scheduleSummaryVo.setNextDayPlanQty(totalNextDayPlanQty.doubleValue());
        scheduleSummaryVo.setStockQty(totalStockQty.doubleValue());
        scheduleSummaryVo.setLastDayPlanQty(totalLastDayPlanQty.doubleValue());
        scheduleSummaryVo.setTheoreticClassStockQty(totalTheoreticClassStockQty.doubleValue());
        return AjaxResult.success(scheduleSummaryVo);
    }

    @Override
    public int importData(List<DjScheduleResult> list, boolean updateSupport, long importLogId) {
        return 0;
    }

    @Override
    protected String getBillTypeCode() {
        return "";
    }
}