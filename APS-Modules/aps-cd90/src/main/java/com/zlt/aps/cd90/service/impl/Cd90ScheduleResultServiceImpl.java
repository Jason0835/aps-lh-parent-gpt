package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90BatchDataCheckResult;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleBatchDataValidator;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleTaskService;
import com.zlt.aps.cd90.mapper.Cd90ScheduleResultMapper;
import com.zlt.aps.cd90.model.Cd90ScheduleOverwriteDecision;
import com.zlt.aps.cd90.service.Cd90AutoScheduleAsyncExecutor;
import com.zlt.aps.cd90.service.Cd90ScheduleOverwriteValidator;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ScheduleResultServiceImpl extends AbstractDocService<Cd90ScheduleResult> implements ICd90ScheduleResultService {

    @Resource
    private Cd90ScheduleResultMapper cd90ScheduleResultMapper;
    @Resource
    private Cd90ScheduleOverwriteValidator overwriteValidator;
    @Resource
    private Cd90ScheduleTaskService taskService;
    @Resource
    private Cd90AutoScheduleAsyncExecutor asyncExecutor;
    @Resource
    private Cd90AutoScheduleBatchDataValidator batchDataValidator;

    /**
     * 接收自动排程请求。
     * 排程算法统一由Aps-Engine中的直裁引擎实现，本服务只负责业务接口转发。
     *
     * @param scheduleResult 自动排程条件，当前使用工厂编码和排程日期
     * @return 接口调用成功
     */
    @Override
    public AjaxResult autoSchedule(Cd90ScheduleResult scheduleResult) {
        if (scheduleResult == null) {
            return AjaxResult.error("自动排程请求不能为空");
        }
        if (scheduleResult.getFactoryCode() == null || scheduleResult.getFactoryCode().trim().isEmpty()
                || scheduleResult.getScheduleDate() == null) {
            return AjaxResult.error("自动排程工厂编码和排程日期不能为空");
        }
        // 正式进入自动排程前，同步做1.2节批次级数据先行检查；
        // 失败时不创建PENDING任务、不占用执行锁、不进入异步执行器，直接返回结构化错误。
        LocalDate localScheduleDate = scheduleResult.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Cd90BatchDataCheckResult batchCheck = batchDataValidator.check(
                scheduleResult.getFactoryCode(), localScheduleDate);
        if (batchCheck.isFailed()) {
            // 走success+batchCheckFailed标记，避免HTTP 500被前端拦截器拦截且丢失data；
            // 与needConfirm模式一致，由前端按data.batchCheckFailed分流渲染结构化错误。
            Map<String, Object> data = new HashMap<>();
            data.put("needConfirm", false);
            data.put("batchCheckFailed", true);
            data.put("errors", toErrorList(batchCheck.getErrors()));
            data.put("warnings", toErrorList(batchCheck.getWarnings()));
            return AjaxResult.success(batchCheck.getPrimaryMessage(), data);
        }
        List<Cd90ScheduleResult> existing = cd90ScheduleResultMapper.selectList(
                new LambdaQueryWrapper<Cd90ScheduleResult>()
                        .eq(Cd90ScheduleResult::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(Cd90ScheduleResult::getScheduleDate, scheduleResult.getScheduleDate()));
        Cd90ScheduleOverwriteDecision decision = overwriteValidator.validate(existing,
                Boolean.TRUE.equals(scheduleResult.getForceRegenerate()));
        if (decision.isRejected()) {
            return AjaxResult.error(decision.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        if (decision.isNeedConfirm()) {
            data.put("needConfirm", true);
            return AjaxResult.success(decision.getMessage(), data);
        }
        Cd90ScheduleTask activeTask = taskService.findActive(
                scheduleResult.getFactoryCode(), scheduleResult.getScheduleDate());
        if (activeTask != null) {
            data.put("needConfirm", false);
            data.put("taskId", activeTask.getTaskId());
            return AjaxResult.success("当前日期已有自动排程任务正在执行", data);
        }
        String snapshot = "factoryCode=" + scheduleResult.getFactoryCode()
                + ",scheduleDate=" + scheduleResult.getScheduleDate()
                + ",forceRegenerate=" + Boolean.TRUE.equals(scheduleResult.getForceRegenerate());
        Cd90ScheduleTask task = taskService.createPending(scheduleResult.getFactoryCode(),
                scheduleResult.getScheduleDate(), "MANUAL", snapshot, null);
        asyncExecutor.execute(task.getTaskId(), task.getFactoryCode(), task.getScheduleDate());
        data.put("needConfirm", false);
        data.put("taskId", task.getTaskId());
        return AjaxResult.success("自动排程任务已提交", data);
    }

    @Override
    protected String getDocTypeCode() { return "CD90_SCHEDULE_RESULT"; }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_SCHEDULE_RESULT");
        return sysDocType;
    }

    /** 将批次级检查错误列表转为前端可渲染的List<Map>结构。 */
    private List<Map<String, Object>> toErrorList(List<Cd90BatchDataCheckResult.CheckError> errors) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (errors == null) {
            return result;
        }
        for (Cd90BatchDataCheckResult.CheckError error : errors) {
            Map<String, Object> item = new HashMap<>();
            item.put("field", error.getField());
            item.put("reasonCode", error.getReasonCode());
            item.put("message", error.getMessage());
            item.put("suggestion", error.getSuggestion());
            result.add(item);
        }
        return result;
    }
}
