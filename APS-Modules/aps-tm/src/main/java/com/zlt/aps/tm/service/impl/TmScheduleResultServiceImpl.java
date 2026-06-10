package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.mapper.TmDispatcherLogMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.ITmScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 胎面排程结果表 业务层处理
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmScheduleResultServiceImpl extends AbstractDocService<TmScheduleResult> implements ITmScheduleResultService {

    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;

    @Resource
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0815";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0815");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmScheduleResult query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.scheduleResult.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "batchNo", "scheduleDate", "treadCode", "machineCode"));
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TmScheduleResult> list, List<TmScheduleResult> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TmScheduleResult::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 分批查询机台基础数据
        List<List<String>> splitList = ListUtil.split(machineCodeList, 500);
        List<TmMachineInfo> machineInfoList = new ArrayList<>();
        for (List<String> codes : splitList) {
            LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TmMachineInfo::getMachineCode, codes);
            machineInfoList.addAll(tmMachineInfoMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(machineInfoList)) {
            serviceCheckParams.put("tmMachineCodeList",
                    machineInfoList.stream().map(TmMachineInfo::getMachineCode).collect(Collectors.toList()));
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TmScheduleResult importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验机台编码是否存在
        if (serviceCheckParams.containsKey("tmMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tmMachineCodeList");
            String machineCode = importDocEntity.getMachineCode();
            if (!machineCodeList.contains(machineCode)) {
                String message = String.format(I18nUtil.getMessage("ui.data.alert.tm.machineCodeNotExist"), machineCode);
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }

    /**
     * 修改胎面排程结果
     * @param scheduleResult 胎面排程结果
     * @return 结果
     */
    @Override
    public int updateTmScheduleResult(TmScheduleResult scheduleResult) {
        scheduleResult.setBaseVale(scheduleResult.getId());
        // 校验字段是否修改，修改则改状态为未发布
        if (!ApsConstant.RELEASING.equals(scheduleResult.getReleaseStatus())
                && !ApsConstant.TIMEOUT_FAILURE.equals(scheduleResult.getReleaseStatus())) {
            TmScheduleResult old = tmScheduleResultMapper.selectById(scheduleResult.getId());
            if (old != null) {
                boolean flag = compare(old.getMachineCode(), scheduleResult.getMachineCode());
                flag = flag && compare(old.getClass1PlanQty(), scheduleResult.getClass1PlanQty());
                flag = flag && compare(old.getClass2PlanQty(), scheduleResult.getClass2PlanQty());
                flag = flag && compare(old.getClass3PlanQty(), scheduleResult.getClass3PlanQty());
                flag = flag && compare(old.getClass4PlanQty(), scheduleResult.getClass4PlanQty());
                flag = flag && compare(old.getClass5PlanQty(), scheduleResult.getClass5PlanQty());
                flag = flag && compare(old.getClass6PlanQty(), scheduleResult.getClass6PlanQty());
                if (!flag) {
                    scheduleResult.setReleaseStatus(scheduleResult.getReleaseStatus() == null || "".equals(scheduleResult.getReleaseStatus())
                            ? ApsConstant.NO_RELEASE : ApsConstant.WAIT_RELEASING);
                }
            }
        }
        return tmScheduleResultMapper.updateById(scheduleResult);
    }

    /**
     * 比较两个值是否相等
     * @param oldVal 旧值
     * @param newVal 新值
     * @return true表示相等
     */
    private boolean compare(Object oldVal, Object newVal) {
        if (oldVal == null && newVal == null) {
            return true;
        }
        if (oldVal != null) {
            return oldVal.equals(newVal);
        }
        return false;
    }

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录数
     * @param ids id数组
     * @return 符合条件的记录数
     */
    @Override
    public int isReleasingOrTimeoutByIds(Long[] ids) {
        return tmScheduleResultMapper.isReleasingOrTimeoutByIds(ids);
    }

    /**
     * 记录调度员操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule 操作后的排程数据
     */
    @Override
    public void insetDispatcherLog(String operType, TmScheduleResult newSchedule) {
        TmScheduleResult oldSchedule = tmScheduleResultMapper.selectById(newSchedule.getId());
        TmDispatcherLog log = new TmDispatcherLog();
        // 基础信息赋值
        log.setScheduleId(newSchedule.getId());
        log.setOperType(operType);
        log.setScheduleDate(newSchedule.getScheduleDate());
        log.setTreadCode(newSchedule.getTreadCode());
        log.setFactoryCode(newSchedule.getFactoryCode());
        log.setBatchNo(newSchedule.getBatchNo());
        // 操作前的信息赋值
        if (oldSchedule != null) {
            log.setBeforeMachineCode(oldSchedule.getMachineCode());
            log.setBeforeClass1PlanQty(oldSchedule.getClass1PlanQty());
            log.setBeforeClass2PlanQty(oldSchedule.getClass2PlanQty());
            log.setBeforeClass3PlanQty(oldSchedule.getClass3PlanQty());
            log.setBeforeClass4PlanQty(oldSchedule.getClass4PlanQty());
            log.setBeforeClass5PlanQty(oldSchedule.getClass5PlanQty());
            log.setBeforeClass6PlanQty(oldSchedule.getClass6PlanQty());
        }
        // 操作后的信息赋值
        log.setAfterMachineCode(newSchedule.getMachineCode());
        log.setAfterClass1PlanQty(newSchedule.getClass1PlanQty());
        log.setAfterClass2PlanQty(newSchedule.getClass2PlanQty());
        log.setAfterClass3PlanQty(newSchedule.getClass3PlanQty());
        log.setAfterClass4PlanQty(newSchedule.getClass4PlanQty());
        log.setAfterClass5PlanQty(newSchedule.getClass5PlanQty());
        log.setAfterClass6PlanQty(newSchedule.getClass6PlanQty());
        // 调用插入日志方法
        tmDispatcherLogMapper.insert(log);
    }
}
