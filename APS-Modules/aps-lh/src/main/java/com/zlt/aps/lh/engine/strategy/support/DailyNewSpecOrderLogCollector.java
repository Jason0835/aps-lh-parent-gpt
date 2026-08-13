package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 新增按天排产的每日 SKU 实际执行顺序日志采集器。
 *
 * <p>采集器只接收新增主循环已经完成当天准入过滤、且即将进入真实选机的 SKU。
 * 它不读取候选快照、不复制排产队列、不排序、不去重，也不执行任何加机台规则；
 * 目标机台数由真实排产计算过程回填，因此该类只承担顺序保存和最终文本格式化。</p>
 *
 * <p>每个业务日创建一个采集器，当日结束后一次性使用 {@link StringBuilder} 构建明细并清理，
 * 保存的明细不引用排程大对象，避免过程日志引入额外的高内存占用。</p>
 *
 * @author APS
 */
public class DailyNewSpecOrderLogCollector {

    /** 每日无真实可排 SKU 时的固定明细 */
    public static final String EMPTY_DAY_DETAIL = "当日无可参与排产的SKU";

    /** 日志标题中的日期格式 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 当前实际业务日期 */
    private final LocalDate scheduleDate;
    /** 当前业务日相对排程 T 日的偏移 */
    private final int dateOffset;
    /** 按真实遍历先后追加的日志明细，不按 SKU 去重 */
    private final List<DailyNewSpecOrderLogEntry> entryList =
            new ArrayList<DailyNewSpecOrderLogEntry>(64);

    /**
     * 创建当前业务日顺序日志采集器。
     *
     * @param scheduleDate 当前实际业务日期
     * @param dateOffset 当前业务日相对排程 T 日偏移
     */
    public DailyNewSpecOrderLogCollector(LocalDate scheduleDate, int dateOffset) {
        this.scheduleDate = Objects.requireNonNull(scheduleDate, "新增排产日志业务日期不能为空");
        // 日期偏移直接使用现有日驱动班次口径，日志层不重新校正或推导 T 日。
        this.dateOffset = dateOffset;
    }

    /**
     * 记录一次真实进入当前新增排产主循环的 SKU。
     *
     * <p>在机延续阶段只延用原机台，不属于本次“新增排产顺序”，因此明确排除；
     * FINALIZE 只是日状态收口，同样不产生排产顺序。其他现有或未来新增排产阶段
     * 均按调用先后原样追加，保证日志顺序与真实执行顺序一致。</p>
     *
     * @param materialCode 物料编码/SKU
     * @param phase 当前日内实际执行阶段
     * @param sourceType 当前新增入口主来源编码
     * @param originalDayPlanQty 当前实际业务日原始月计划 dayN 数量
     * @param structureEarlyProduction 是否结构提前
     * @param initialRequiredMachineCount 当前路径已经明确的初始目标机台数
     * @return 新增的可回填明细；排除阶段返回 null
     */
    public DailyNewSpecOrderLogEntry record(String materialCode,
                                            DailySchedulePhase phase,
                                            String sourceType,
                                            int originalDayPlanQty,
                                            boolean structureEarlyProduction,
                                            int initialRequiredMachineCount) {
        if (Objects.isNull(phase)
                || DailySchedulePhase.CARRY_OVER == phase
                || DailySchedulePhase.FINALIZE == phase) {
            return null;
        }
        DailyNewSpecOrderLogEntry entry = new DailyNewSpecOrderLogEntry(
                materialCode,
                this.resolveProductionPhase(phase),
                this.resolveMaterialSourceType(sourceType),
                originalDayPlanQty,
                structureEarlyProduction,
                initialRequiredMachineCount);
        entryList.add(entry);
        return entry;
    }

    /**
     * 移除尚未真正形成新增机台需求的明细。
     *
     * <p>现有 dayN 模拟可能在候选试算后确认“已有同物料机台已满足”，该 SKU 不应出现在
     * 每日可排顺序中。调用方仅能在本轮尚未成功落地机台时移除，避免删除已真实执行的顺序。</p>
     *
     * @param entry 待移除明细
     */
    public void remove(DailyNewSpecOrderLogEntry entry) {
        if (Objects.nonNull(entry)) {
            entryList.remove(entry);
        }
    }

    /**
     * 构建每日过程日志标题。
     *
     * @return 形如“新增排产 T+1日（2026-08-11）排产明细”的标题
     */
    public String buildTitle() {
        String dayLabel = dateOffset == 0 ? "T日" : "T+" + dateOffset + "日";
        return "新增排产 " + dayLabel + "（" + scheduleDate.format(DATE_FORMATTER) + "）排产明细";
    }

    /**
     * 按真实追加顺序构建每日过程日志明细。
     *
     * @return 完整日志明细；当天无明细时返回固定空日说明
     */
    public String buildDetail() {
        if (entryList.isEmpty()) {
            return EMPTY_DAY_DETAIL;
        }
        StringBuilder detailBuilder = new StringBuilder(1024);
        for (int index = 0; index < entryList.size(); index++) {
            if (index > 0) {
                detailBuilder.append('\n');
            }
            DailyNewSpecOrderLogEntry entry = entryList.get(index);
            detailBuilder.append(index + 1)
                    .append('｜').append(entry.getMaterialCode())
                    .append('｜').append(entry.getProductionPhase())
                    .append('｜').append(entry.getMaterialSourceType())
                    .append("｜需排机台数：").append(entry.getRequiredMachineCount()).append('台')
                    .append("｜月计划dayN：").append(entry.getOriginalDayPlanQty())
                    .append("｜是否结构提前：")
                    .append(entry.isStructureEarlyProduction() ? "是" : "否");
        }
        return detailBuilder.toString();
    }

    /**
     * 当前日志写入过程日志列表后释放明细引用。
     */
    public void clear() {
        entryList.clear();
    }

    /**
     * 返回当前已记录明细数，供专项测试和日终审计使用。
     *
     * @return 明细数量
     */
    public int size() {
        return entryList.size();
    }

    /**
     * 将排产阶段枚举转换为业务展示名称。
     *
     * @param phase 实际阶段
     * @return 阶段业务名称
     */
    private String resolveProductionPhase(DailySchedulePhase phase) {
        if (DailySchedulePhase.NORMAL_RESOURCE_COMPETITION == phase) {
            return "正常排产阶段";
        }
        if (DailySchedulePhase.LEGACY_SHORTAGE_OR_ENDING == phase) {
            return "历史欠产/收尾遗留阶段";
        }
        if (DailySchedulePhase.EARLY_PRODUCTION == phase) {
            return "提前生产阶段";
        }
        return phase.name();
    }

    /**
     * 将运行态来源编码转换为每日顺序日志使用的主来源名称。
     *
     * @param sourceType 运行态来源编码
     * @return 主来源名称
     */
    private String resolveMaterialSourceType(String sourceType) {
        if (StringUtils.isEmpty(sourceType)
                || StringUtils.equals(SkuScheduleSourceTypeEnum.NORMAL_NEW_SPEC.getCode(), sourceType)) {
            return "完全新增";
        }
        if (StringUtils.equals(
                SkuScheduleSourceTypeEnum.CONTINUATION_ADD_MACHINE.getCode(), sourceType)) {
            return "续作新增";
        }
        if (StringUtils.equals(
                SkuScheduleSourceTypeEnum.TYPE_BLOCK_TO_NEW_SPEC.getCode(), sourceType)) {
            return "换活字块新增";
        }
        // 后续若新增其他合法来源，直接记录其实际来源编码，禁止错误归类为现有三种来源。
        return sourceType;
    }

}
