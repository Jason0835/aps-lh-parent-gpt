package com.zlt.aps.lh.engine.observer.listeners;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.autoLogin.feign.FeignTokenHelper;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.lh.api.enums.EventTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.observer.IScheduleEventListener;
import com.zlt.aps.lh.engine.observer.ScheduleEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 硫化排程完成量回报同步监听器。
 *
 * <p>业务背景：</p>
 * <p>硫化自动排程在 S4.6 步骤会先删除目标日原排程结果再重新生成（{@code replaceScheduleAtomically}），
 * 这会导致原先从 MES 同步并回填到排程结果表各班次的完成量被一并清除。</p>
 *
 * <p>处理策略：</p>
 * <p>监听 {@link EventTypeEnum#SCHEDULE_COMPLETED} 事件（在 {@code replaceScheduleAtomically} 之后触发），
 * 按排程上下文的 {@code factoryCode} 立即调用 MES 接口重新同步硫化排程完成量回报并回填新生成的排程结果，
 * 保证自动排程后用户看到的结果数据仍带有最新的 MES 完成量。</p>
 *
 * <p>同步范围：</p>
 * <ul>
 *   <li>当天数据：覆盖排程目标日 T+1 的早中班次回填；</li>
 *   <li>上一天数据：扩展覆盖凌晨 0 点边界场景，避免自动排程在凌晨刚过触发时 MES 当天数据尚未生成导致回填空跑。</li>
 * </ul>
 *
 * <p>容错说明：</p>
 * <ul>
 *   <li>监听器内部异常仅记录日志，不影响主流程和其他监听器；</li>
 *   <li>外层 {@code ScheduleEventPublisher} 还有兜底 try-catch；</li>
 *   <li>同步失败时由 {@code MesTask} 定时任务兜底补偿。</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class LhFinishQtySyncListener implements IScheduleEventListener {

    @Resource
    private IMesItfService iMesItfService;

    /**
     * 处理排程完成事件：按工厂同步当天及上一天 MES 硫化排程完成量回报并回填排程结果。
     *
     * @param event 排程事件
     */
    @Override
    public void onEvent(ScheduleEvent event) {
        if (Objects.isNull(event) || Objects.isNull(event.getContext())) {
            log.warn("硫化排程完成量同步监听跳过，事件或上下文为空");
            return;
        }

        LhScheduleContext context = event.getContext();
        String factoryCode = context.getFactoryCode();
        String batchNo = context.getBatchNo();
        if (StringUtils.isBlank(factoryCode)) {
            log.warn("硫化排程完成量同步监听跳过，工厂编码为空，批次号: {}", batchNo);
            return;
        }

        log.info("硫化排程完成量同步监听开始，批次号: {}，工厂: {}", batchNo, factoryCode);

        // Step1：同步当天 MES 完成量回报并回填排程结果
        this.syncCurrentDayFinishQty(factoryCode, batchNo);

        // Step2：同步上一天 MES 完成量回报，覆盖凌晨 0 点边界场景
        this.syncYesterdayFinishQty(factoryCode, batchNo);
    }

    /**
     * 是否关注该事件类型：仅关注排程完成事件。
     *
     * @param eventType 事件类型
     * @return true-关注，false-不关注
     */
    @Override
    public boolean supports(EventTypeEnum eventType) {
        return eventType == EventTypeEnum.SCHEDULE_COMPLETED;
    }

    /**
     * 同步当天 MES 硫化排程完成量回报并回填排程结果。
     * <p>查询 MES 中间表 SCHEDULE_DATE = GETDATE() 的最新版本数据，
     * 回填规则按回报日 D 更新排程日期 D-1/D/D+1 的 8 个班次完成量。</p>
     *
     * @param factoryCode 工厂编码
     * @param batchNo     批次号（用于日志）
     */
    private void syncCurrentDayFinishQty(String factoryCode, String batchNo) {
        try {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            syncDataLogs.setFactoryCode(factoryCode);
            AjaxResult result = FeignTokenHelper.callWithToken(() -> iMesItfService.syncLhClassShiftFinishQty(syncDataLogs));
            log.info("硫化排程完成量同步监听[当天]完成，批次号: {}，工厂: {}，结果: {}", batchNo, factoryCode, result);
        } catch (Exception e) {
            log.error("硫化排程完成量同步监听[当天]异常，批次号: {}，工厂: {}", batchNo, factoryCode, e);
        }
    }

    /**
     * 同步上一天 MES 硫化排程完成量回报并回填排程结果。
     * <p>扩展覆盖凌晨 0 点边界场景：自动排程在凌晨刚过触发时，MES 当天数据可能尚未生成，
     * 此时通过同步上一天数据保证回填不空跑。</p>
     *
     * @param factoryCode 工厂编码
     * @param batchNo     批次号（用于日志）
     */
    private void syncYesterdayFinishQty(String factoryCode, String batchNo) {
        try {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            syncDataLogs.setFactoryCode(factoryCode);
            AjaxResult result = FeignTokenHelper.callWithToken(() -> iMesItfService.syncLhClassShiftFinishQtyByYesterday(syncDataLogs));
            log.info("硫化排程完成量同步监听[上一天]完成，批次号: {}，工厂: {}，结果: {}", batchNo, factoryCode, result);
        } catch (Exception e) {
            log.error("硫化排程完成量同步监听[上一天]异常，批次号: {}，工厂: {}", batchNo, factoryCode, e);
        }
    }
}
