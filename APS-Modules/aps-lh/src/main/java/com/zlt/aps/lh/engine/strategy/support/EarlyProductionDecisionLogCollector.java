package com.zlt.aps.lh.engine.strategy.support;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 每个业务日提前生产判断日志采集器。
 *
 * <p>采集器只保存轻量日志明细，不重新执行提前生产判断、不读取数据库；落库文本展示时
 * 仅按已有排序字段升序排列，不改变真实调用顺序和排程执行顺序。同一 SKU 在不同业务日
 * 或不同实际尝试中允许追加多条明细，避免后一次结果覆盖前一次失败原因。</p>
 *
 * @author APS
 */
public class EarlyProductionDecisionLogCollector {

    /** 日期展示格式。 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 当前业务日期。 */
    private final LocalDate businessDate;
    /** 当前业务日相对窗口 T 日偏移。 */
    private final int dateOffset;
    /** 按真实调用顺序保存的日志明细；该顺序用于查找和保留多次尝试。 */
    private final List<EarlyProductionDecisionLogEntry> entryList =
            new ArrayList<EarlyProductionDecisionLogEntry>(64);

    /**
     * 创建业务日提前生产日志采集器。
     *
     * @param businessDate 当前业务日期
     * @param dateOffset 相对窗口 T 日偏移
     */
    public EarlyProductionDecisionLogCollector(LocalDate businessDate, int dateOffset) {
        this.businessDate = Objects.requireNonNull(businessDate, "提前生产日志业务日期不能为空");
        this.dateOffset = dateOffset;
    }

    /** 获取当前业务日期。 */
    public LocalDate getBusinessDate() {
        return businessDate;
    }

    /** 获取当前业务日相对窗口 T 日的偏移。 */
    public int getDateOffset() {
        return dateOffset;
    }

    /**
     * 追加一条提前生产判断明细。
     *
     * @param entry 日志明细
     * @return 已追加的明细
     */
    public EarlyProductionDecisionLogEntry record(EarlyProductionDecisionLogEntry entry) {
        if (Objects.isNull(entry)) {
            return null;
        }
        if (Objects.isNull(entry.getSortOrder()) || entry.getSortOrder() <= 0) {
            entry.setSortOrder(entryList.size() + 1);
        }
        if (Objects.isNull(entry.getBusinessDate())) {
            entry.setBusinessDate(businessDate);
        }
        entryList.add(entry);
        return entry;
    }

    /**
     * 查找当前业务日同 SKU、同来源日期的最后一条明细。
     *
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param sourcePlanDate 来源计划日期
     * @return 最后一条明细；不存在返回 null
     */
    public EarlyProductionDecisionLogEntry findLatest(
            String materialCode,
            String productStatus,
            LocalDate sourcePlanDate) {
        for (int index = entryList.size() - 1; index >= 0; index--) {
            EarlyProductionDecisionLogEntry entry = entryList.get(index);
            if (StringUtils.equals(materialCode, entry.getMaterialCode())
                    && StringUtils.equals(StringUtils.defaultString(productStatus),
                    StringUtils.defaultString(entry.getProductStatus()))
                    && Objects.equals(sourcePlanDate, entry.getSourcePlanDate())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 查找当前业务日同 SKU 的最后一条判断明细，不限制来源日期。
     *
     * <p>用于 S4.4 后置失败收口：失败回调本身只携带原有失败原因，来源日期已经由此前
     * 的共享提前生产判断快照记录，不能因为回调缺少来源日期而重复创建空日期日志。</p>
     *
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 最后一条明细；不存在返回 null
     */
    public EarlyProductionDecisionLogEntry findLatest(
            String materialCode,
            String productStatus) {
        for (int index = entryList.size() - 1; index >= 0; index--) {
            EarlyProductionDecisionLogEntry entry = entryList.get(index);
            if (StringUtils.equals(materialCode, entry.getMaterialCode())
                    && StringUtils.equals(StringUtils.defaultString(productStatus),
                    StringUtils.defaultString(entry.getProductStatus()))) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 构建过程日志标题。
     *
     * @return 业务日提前生产判断日志标题
     */
    public String buildTitle() {
        String dayLabel = dateOffset == 0 ? "T日" : "T+" + dateOffset + "日";
        return "结构提前生产 " + dayLabel + "（"
                + businessDate.format(DATE_FORMATTER) + "）判断明细";
    }

    /**
     * 构建业务日全部判断明细。
     *
     * @return 日志明细
     */
    public String buildDetail() {
        if (CollectionUtils.isEmpty(entryList)) {
            return "当日无提前生产判断SKU";
        }
        List<EarlyProductionDecisionLogEntry> sortedEntryList =
                new ArrayList<EarlyProductionDecisionLogEntry>(entryList);
        sortedEntryList.sort(Comparator.comparingInt(this::resolveDisplaySortOrder));
        StringBuilder detailBuilder = new StringBuilder(2048);
        for (int index = 0; index < sortedEntryList.size(); index++) {
            if (index > 0) {
                detailBuilder.append('\n');
            }
            detailBuilder.append(sortedEntryList.get(index).buildDetail());
        }
        return detailBuilder.toString();
    }

    /**
     * 获取日志展示使用的排序字段。
     *
     * <p>真实进入新增选机流程的 SKU 使用已有实际选机排序；尚未进入正式选机流程的
     * 前置失败 SKU 使用采集器为其保留的稳定顺序。这里只复制并排序日志快照，不改变
     * 排程候选列表和业务执行顺序。</p>
     *
     * @param entry 日志明细
     * @return 日志展示排序值
     */
    private int resolveDisplaySortOrder(EarlyProductionDecisionLogEntry entry) {
        if (Objects.nonNull(entry.getActualSelectionOrder())
                && entry.getActualSelectionOrder() > 0) {
            return entry.getActualSelectionOrder();
        }
        return Objects.isNull(entry.getSortOrder())
                ? Integer.MAX_VALUE : entry.getSortOrder();
    }

    /**
     * 获取采集明细数量。
     *
     * @return 明细数量
     */
    public int size() {
        return entryList.size();
    }

    /**
     * 清理轻量明细，避免日志落库后继续持有对象。
     */
    public void clear() {
        entryList.clear();
    }
}
