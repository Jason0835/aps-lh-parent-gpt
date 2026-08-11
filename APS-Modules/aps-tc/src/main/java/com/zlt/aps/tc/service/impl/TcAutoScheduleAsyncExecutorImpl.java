package com.zlt.aps.tc.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleRequestVo;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.service.ITcScheduleResultService;
import com.zlt.aps.tc.service.TcAutoScheduleAsyncExecutor;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 胎侧自动排程异步执行实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcAutoScheduleAsyncExecutorImpl implements TcAutoScheduleAsyncExecutor {

    private static final String LANGUAGE_ZH_CN = "zh_CN";

    private static final String LANGUAGE_EN_US = "en_US";

    private static final String LANGUAGE_VI_VN = "vi_VN";

    @Lazy
    private final ITcScheduleResultService tcScheduleResultService;

    private final TcAutoScheduleTaskService taskService;

    /**
     * 执行胎侧自动排程任务。
     *
     * @param taskId 对外任务 ID
     */
    @Async
    @Override
    public void execute(String taskId) {
        LocaleContext originalLocaleContext = LocaleContextHolder.getLocaleContext();
        LocaleContextHolder.setLocale(this.resolveTaskLocale(taskId));
        try {
            if (!taskService.start(taskId)) {
                log.warn("[TC_AUTO_PLAN] 自动排程任务启动失败或状态已变化, taskId={}", taskId);
                return;
            }
            try {
                tcScheduleResultService.executeTcAutoPlanTask(taskId);
            } catch (Exception exception) {
                log.error("[TC_AUTO_PLAN] 自动排程异步任务执行失败, taskId={}", taskId, exception);
                TcAutoScheduleTask task = taskService.findByTaskId(taskId);
                List<TcAutoScheduleIssueVo> issues = task == null || taskService.toResponse(task) == null
                        ? Collections.emptyList() : taskService.toResponse(task).getIssues();
                taskService.markFailed(taskId, exception.getMessage(), issues);
            }
        } finally {
            if (originalLocaleContext == null) {
                LocaleContextHolder.resetLocaleContext();
            } else {
                LocaleContextHolder.setLocaleContext(originalLocaleContext);
            }
        }
    }

    /**
     * 从任务请求快照解析提交时的界面语言，旧任务或非法语言统一回退中文。
     *
     * @param taskId 对外任务 ID
     * @return 异步任务执行期间使用的语言
     */
    private Locale resolveTaskLocale(String taskId) {
        TcAutoScheduleTask task = taskService.findByTaskId(taskId);
        if (task == null || StrUtil.isBlank(task.getRequestSnapshot())) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        try {
            TcAutoScheduleRequestVo request = JSON.parseObject(task.getRequestSnapshot(), TcAutoScheduleRequestVo.class);
            return this.resolveSupportedLocale(request == null ? null : request.getLanguage());
        } catch (RuntimeException exception) {
            log.warn("[TC_AUTO_PLAN] 自动排程任务语言解析失败，使用中文, taskId={}", taskId);
            return Locale.SIMPLIFIED_CHINESE;
        }
    }

    /**
     * 将受支持的语言编码转换为 Locale。
     *
     * @param language 界面语言编码
     * @return 对应 Locale，缺失或非法时返回中文
     */
    private Locale resolveSupportedLocale(String language) {
        if (LANGUAGE_EN_US.equalsIgnoreCase(language)) {
            return Locale.US;
        }
        if (LANGUAGE_VI_VN.equalsIgnoreCase(language)) {
            return new Locale("vi", "VN");
        }
        if (LANGUAGE_ZH_CN.equalsIgnoreCase(language)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.SIMPLIFIED_CHINESE;
    }
}
