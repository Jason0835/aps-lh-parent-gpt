package com.zlt.aps.lh.engine.observer.listeners;

import cn.hutool.core.date.DateUtil;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
 * <p>以排程上下文的排程窗口日期（T 日、T+1 日排程目标日、T+2 日窗口结束日）为核心检索条件，
 * 逐日调用 MES 接口同步硫化排程完成量回报。仅同步系统当天及当天之前已生成 MES 回报的日期，
 * 未来日期因 MES 数据尚未生成而跳过，避免回填空跑。</p>
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
     * 处理排程完成事件：按排程窗口日期同步 MES 硫化排程完成量回报并回填排程结果。
     *
     * <p>同步策略：</p>
     * <ol>
     *   <li>取排程窗口三天日期：T 日（{@code scheduleDate}）、T+1 日排程目标日（{@code scheduleTargetDate}）、
     *       T+2 日窗口结束日（{@code windowEndDate}）；</li>
     *   <li>仅保留系统当天及当天之前的日期（未来日期 MES 完成量回报尚未生成，跳过避免空跑）；</li>
     *   <li>按保留的每个排程日期逐日调用 MES 接口，以该排程日期为检索条件抓取最新版本完成量回报并回填。</li>
     * </ol>
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

        // 排程窗口三天日期：T日、T+1日（排程目标日）、T+2日（窗口结束日）
        List<Date> scheduleWindowDates = this.buildScheduleWindowDates(context);

        // 系统当天零点，用于过滤未来日期（未来日期MES完成量回报尚未生成）
        Date today = DateUtil.beginOfDay(DateUtil.date());
        List<Date> datesToSync = scheduleWindowDates.stream()
                .filter(Objects::nonNull)
                .map(DateUtil::beginOfDay)
                .filter(date -> !date.after(today))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (datesToSync.isEmpty()) {
            log.info("硫化排程完成量同步监听跳过，排程窗口均为未来日期无MES完成量可同步，批次号: {}，工厂: {}",
                    batchNo, factoryCode);
            return;
        }

        log.info("硫化排程完成量同步监听开始，批次号: {}，工厂: {}，待同步排程日期: {}",
                batchNo, factoryCode, this.formatDates(datesToSync));

        // 按排程日期逐日同步MES硫化排程完成量回报并回填排程结果
        for (Date scheduleDate : datesToSync) {
            this.syncFinishQtyByScheduleDate(factoryCode, batchNo, scheduleDate);
        }
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
     * 构建排程窗口日期列表：T 日（排程窗口起点）、T+1 日（排程目标日）、T+2 日（窗口结束日）。
     *
     * @param context 排程上下文
     * @return 排程窗口日期列表
     */
    private List<Date> buildScheduleWindowDates(LhScheduleContext context) {
        List<Date> windowDates = new ArrayList<>(3);
        windowDates.add(context.getScheduleDate());       // T日：排程窗口起点
        windowDates.add(context.getScheduleTargetDate()); // T+1日：排程目标日
        windowDates.add(context.getWindowEndDate());      // T+2日：窗口结束日
        return windowDates;
    }

    /**
     * 按指定排程日期同步 MES 硫化排程完成量回报并回填排程结果。
     * <p>以排程日期作为核心检索条件，查询 MES 中间表该日期最新版本的完成量回报数据，
     * 逻辑删除 APS 旧数据后插入新数据，并回填硫化排程结果表各班次完成量。
     * 回填规则按回报日 D 更新排程日期 D-1/D/D+1 的 8 个班次完成量。</p>
     *
     * @param factoryCode  工厂编码
     * @param batchNo      批次号（用于日志）
     * @param scheduleDate 排程日期（MES 检索条件）
     */
    private void syncFinishQtyByScheduleDate(String factoryCode, String batchNo, Date scheduleDate) {
        try {
            AuxReqSyncDataLogs syncDataLogs = new AuxReqSyncDataLogs();
            syncDataLogs.setFactoryCode(factoryCode);
            // 以排程日期作为MES检索条件，覆盖原按系统当天/上一天检索的逻辑
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("scheduleDate", DateUtil.formatDate(scheduleDate));
            syncDataLogs.setQueryParams(queryParams);
            AjaxResult result = FeignTokenHelper.callWithToken(() -> iMesItfService.syncLhClassShiftFinishQty(syncDataLogs));
            log.info("硫化排程完成量同步监听[排程日期={}]完成，批次号: {}，工厂: {}，结果: {}",
                    DateUtil.formatDate(scheduleDate), batchNo, factoryCode, result);
        } catch (Exception e) {
            log.error("硫化排程完成量同步监听[排程日期={}]异常，批次号: {}，工厂: {}",
                    DateUtil.formatDate(scheduleDate), batchNo, factoryCode, e);
        }
    }

    /**
     * 格式化日期列表为逗号分隔的字符串，用于日志输出。
     *
     * @param dates 日期列表
     * @return 格式化后的字符串（yyyy-MM-dd,yyyy-MM-dd）
     */
    private String formatDates(List<Date> dates) {
        return dates.stream()
                .map(DateUtil::formatDate)
                .collect(Collectors.joining(","));
    }
}
