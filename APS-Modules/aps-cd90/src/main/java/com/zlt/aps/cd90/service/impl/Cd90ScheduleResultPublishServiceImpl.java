package com.zlt.aps.cd90.service.impl;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.component.Cd90ScheduleResultIssueAssembler;
import com.zlt.aps.cd90.component.ScheduleExecutionGuard;
import com.zlt.aps.cd90.service.Cd90ScheduleResultPublishService;
import com.zlt.aps.cd90.service.ICd90ScheduleResultService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 直裁排程结果发布业务编排实现。
 *
 * <p>架构优化点（与 LH 模式差异）：
 * <ul>
 *   <li>Controller 薄，业务编排集中在本服务</li>
 *   <li>锁前预校验，避免空记录/无效记录浪费锁周期</li>
 *   <li>锁键 cd90:issue:{factoryCode}:{scheduleDate} 工厂维度隔离</li>
 *   <li>置 RELEASING 中间态，避免用户重复点击</li>
 *   <li>MES 调用无事务，状态回写用 REQUIRES_NEW 独立短事务</li>
 *   <li>publishTraceId 贯穿日志与 MES 调用</li>
 *   <li>指数退避（1s/2s/4s），4xx 业务异常不重试</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
public class Cd90ScheduleResultPublishServiceImpl implements Cd90ScheduleResultPublishService {

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAY_MS = {1000L, 2000L, 4000L};

    @Resource
    private ICd90ScheduleResultService cd90ScheduleResultService;
    @Resource
    private Cd90ScheduleResultIssueAssembler issueAssembler;
    @Resource
    private ScheduleExecutionGuard scheduleExecutionGuard;
    @Resource
    private IMesItfService mesItfService;

    @Override
    public AjaxResult publish(Cd90ScheduleResult dto, String ids) {
        if (dto == null || dto.getScheduleDate() == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        String factoryCode = dto.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }
        Date scheduleDate = dto.getScheduleDate();
        String publishTraceId = UUID.randomUUID().toString();
        log.info("直裁排程发布开始, traceId={}, factoryCode={}, scheduleDate={}, ids={}",
                publishTraceId, factoryCode, scheduleDate, ids);

        // 1. 查库
        List<Cd90ScheduleResult> all = cd90ScheduleResultService.selectByDateAndFactory(scheduleDate, factoryCode);
        if (all.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        // 2. 按 ids 过滤
        List<Cd90ScheduleResult> selected = filterByIds(all, ids);
        if (selected.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        // 3. 预过滤可发布状态
        List<Cd90ScheduleResult> publishable = selected.stream()
                .filter(item -> ApsConstant.NO_RELEASE.equals(item.getIsRelease())
                        || ApsConstant.FAILURE_RELEASE.equals(item.getIsRelease())
                        || ApsConstant.TIMEOUT_FAILURE.equals(item.getIsRelease())
                        || ApsConstant.WAIT_RELEASING.equals(item.getIsRelease()))
                .collect(Collectors.toList());
        if (publishable.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.errorPublish"));
        }

        // 4. 校验机台编码
        List<Cd90ScheduleResult> invalid = publishable.stream()
                .filter(item -> StringUtils.isBlank(item.getMachineCode()) || item.getMachineCode().contains(","))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            log.warn("直裁排程发布校验失败, traceId={}, 异常记录: {}", publishTraceId,
                    invalid.stream().map(item -> "ID=" + item.getId() + ",machineCode=" + item.getMachineCode())
                            .collect(Collectors.joining("; ")));
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.hasMultipleIds"));
        }

        // 5. 置 RELEASING 中间态（独立短事务），避免用户刷新页面后重复点击
        cd90ScheduleResultService.batchUpdateReleaseStatus(publishable, ApsConstant.RELEASING);

        // 6. 抢锁
        String lockToken = scheduleExecutionGuard.acquireIssueLock(factoryCode, scheduleDate);
        if (lockToken == null) {
            log.warn("直裁排程发布锁已被占用, traceId={}, factoryCode={}, scheduleDate={}",
                    publishTraceId, factoryCode, scheduleDate);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.issueInProgress"));
        }

        try {
            // 7. 重试 MES 下发
            int retryCount = 0;
            while (retryCount < MAX_RETRIES) {
                try {
                    List<Cd90ScheduleResultIssue> issueList = issueAssembler.assemble(
                            publishable, scheduleDate, factoryCode, publishTraceId);
                    if (issueList.isEmpty()) {
                        log.warn("直裁排程发布: 装配下发列表为空, traceId={}", publishTraceId);
                        cd90ScheduleResultService.batchUpdateReleaseStatus(publishable, ApsConstant.FAILURE_RELEASE);
                        return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.noIssueData"));
                    }

                    AjaxResult issueResult = FeignTokenHelper.callWithToken(
                            () -> mesItfService.issueCd90ScheduleResult(issueList));
                    if (issueResult != null && Objects.equals(HttpStatus.SUCCESS, issueResult.get(AjaxResult.CODE_TAG))) {
                        cd90ScheduleResultService.batchUpdateReleaseStatus(publishable, ApsConstant.IS_RELEASE);
                        log.info("直裁排程发布成功, traceId={}, retryCount={}, 记录数={}",
                                publishTraceId, retryCount + 1, publishable.size());
                        return AjaxResult.success(I18nUtil.getMessage("ui.data.column.scheduleResult.successPublish"));
                    }

                    if (isBusinessError(issueResult)) {
                        // 4xx 业务异常：MES 明确拒绝，重试无意义
                        log.warn("直裁排程发布 MES 返回业务错误, traceId={}, retryCount={}, result={}",
                                publishTraceId, retryCount + 1, issueResult);
                        cd90ScheduleResultService.batchUpdateReleaseStatus(publishable, ApsConstant.FAILURE_RELEASE);
                        String msg = issueResult != null && issueResult.get(AjaxResult.MSG_TAG) != null
                                ? String.valueOf(issueResult.get(AjaxResult.MSG_TAG))
                                : I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish");
                        return AjaxResult.error(msg);
                    }

                    // 5xx/超时/网络：指数退避重试
                    log.warn("直裁排程发布第{}次下发失败, traceId={}, result={}",
                            retryCount + 1, publishTraceId, issueResult);
                    retryCount++;
                    if (retryCount < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS[retryCount - 1]);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("直裁排程发布被中断, traceId={}", publishTraceId, ie);
                    break;
                } catch (Exception e) {
                    log.error("直裁排程发布第{}次下发异常, traceId={}", retryCount + 1, publishTraceId, e);
                    retryCount++;
                    if (retryCount < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS[retryCount - 1]);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            // 8. 最终失败
            log.error("直裁排程发布最终失败, traceId={}, 已重试{}次", publishTraceId, MAX_RETRIES);
            cd90ScheduleResultService.batchUpdateReleaseStatus(publishable, ApsConstant.FAILURE_RELEASE);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.failedPublish"));
        } finally {
            scheduleExecutionGuard.releaseIssueLock(factoryCode, scheduleDate, lockToken);
        }
    }

    private List<Cd90ScheduleResult> filterByIds(List<Cd90ScheduleResult> all, String ids) {
        if (StringUtils.isBlank(ids)) {
            return all;
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return all.stream()
                .filter(item -> idList.contains(item.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 判定 MES 返回是否业务异常（不应重试）。
     * AjaxResult.CODE_TAG 非 HttpStatus.SUCCESS（200）且 MSG_TAG 非空时视为业务错误。
     * 网络异常/超时类会以 exception 形式抛出，进入 catch 分支处理重试。
     */
    private boolean isBusinessError(AjaxResult result) {
        if (result == null) {
            return false;
        }
        Object code = result.get(AjaxResult.CODE_TAG);
        Object msg = result.get(AjaxResult.MSG_TAG);
        return !Objects.equals(HttpStatus.SUCCESS, code) && msg != null;
    }
}
