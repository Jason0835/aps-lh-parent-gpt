package com.zlt.aps.lh.check.checker;

import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.check.LhScheduleCheckContext;
import com.zlt.aps.lh.check.LhScheduleChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查项5：收尾胎胚库存检查（强控）。
 *
 * <p>校验口径：同胎胚（embryoCode）下所有排程行均为收尾行（isEnd=1），
 * 且计划总量合计小于胎胚库存时记异常，说明收尾后仍有库存未排完。</p>
 *
 * <p>跳过条件：存在任一非收尾行（未收尾不检查）；库存字段缺失（无法比较）。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@Slf4j
@Component
public class SharedEmbryoEndingStockChecker implements LhScheduleChecker {

    /** 检查项名称多语言 key */
    private static final String NAME_KEY = "ui.data.column.lhScheduleCheck.endingStock";

    /** 异常消息多语言 key */
    private static final String MESSAGE_KEY = "ui.data.alert.lhScheduleCheck.endingStock";

    /** 收尾标记值 */
    private static final String END_FLAG_YES = "1";

    @Override
    public String getCode() {
        return CODE_ENDING_STOCK;
    }

    @Override
    public String getNameKey() {
        return NAME_KEY;
    }

    @Override
    public boolean isHardControl() {
        return true;
    }

    @Override
    public List<LhScheduleCheckResult> check(LhScheduleCheckContext context) {
        List<LhScheduleCheckResult> abnormalList = new ArrayList<>();
        if (!context.hasResults()) {
            return abnormalList;
        }
        // 1. 按胎胚聚合：计划量合计、库存、是否全部收尾
        Map<String, EmbryoAggregate> aggregateMap = new LinkedHashMap<>(16);
        for (LhScheduleResult result : context.getResults()) {
            String embryoCode = StringUtils.trimToNull(result.getEmbryoCode());
            if (embryoCode == null) {
                continue;
            }
            EmbryoAggregate aggregate = aggregateMap.computeIfAbsent(embryoCode, key -> new EmbryoAggregate());
            aggregate.totalPlanQty += result.getDailyPlanQty() == null ? 0 : result.getDailyPlanQty();
            if (result.getEmbryoStock() != null) {
                aggregate.embryoStock = result.getEmbryoStock();
                aggregate.hasStockValue = true;
            }
            if (!END_FLAG_YES.equals(StringUtils.trimToEmpty(result.getIsEnd()))) {
                aggregate.allEnding = false;
            }
        }
        // 2. 逐胎胚判定
        for (Map.Entry<String, EmbryoAggregate> entry : aggregateMap.entrySet()) {
            EmbryoAggregate aggregate = entry.getValue();
            // 未全部收尾或库存值缺失时跳过，避免误报
            if (!aggregate.allEnding || !aggregate.hasStockValue) {
                continue;
            }
            if (aggregate.totalPlanQty >= aggregate.embryoStock) {
                continue;
            }
            LhScheduleCheckResult abnormal = new LhScheduleCheckResult();
            abnormal.setCheckCode(this.getCode());
            abnormal.setDimensionKey(entry.getKey());
            abnormal.setMessage(MessageFormat.format(
                    I18nUtil.getMessage(MESSAGE_KEY),
                    entry.getKey(), aggregate.totalPlanQty, aggregate.embryoStock));
            abnormal.setDetailJson(JSONUtil.toJsonStr(this.buildDetail(aggregate)));
            abnormalList.add(abnormal);
        }
        return abnormalList;
    }

    /**
     * 构建异常明细。
     *
     * @param aggregate 胎胚聚合信息
     * @return 明细键值对
     */
    private Map<String, Object> buildDetail(EmbryoAggregate aggregate) {
        Map<String, Object> detail = new LinkedHashMap<>(4);
        detail.put("totalPlanQty", aggregate.totalPlanQty);
        detail.put("embryoStock", aggregate.embryoStock);
        return detail;
    }

    /**
     * 胎胚维度的聚合信息。
     */
    private static class EmbryoAggregate {

        /** 计划量合计 */
        int totalPlanQty;

        /** 胎胚库存（取首个非空值） */
        int embryoStock;

        /** 是否存在非空库存值 */
        boolean hasStockValue;

        /** 是否所有行都收尾 */
        boolean allEnding = true;
    }
}
