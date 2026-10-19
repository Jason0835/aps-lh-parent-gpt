package com.zlt.aps.lh.component;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.cx.entity.schedule.CxEmbryoEndingShift;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 成型胎胚收尾后的结构机台配额豁免公共规则。
 * 仅按胎胚代码匹配收尾边界，不区分产品状态，结构归属由当前SKU或占用结果独立校验。
 *
 * @author APS
 * @date 2026/10/14
 */
public final class EmbryoEndingStructureMachineExemptionPolicy {

    /**
     * 禁止实例化无状态公共规则。
     */
    private EmbryoEndingStructureMachineExemptionPolicy() {
    }

    /**
     * 构建本工厂、本三天窗口的只读胎胚收尾班次索引。
     * 仅按胎胚代码归集，相同记录去重，不同班次保留；产品状态为空的记录同样参与匹配。
     *
     * @param factoryCode 工厂编码
     * @param startDate 窗口T日，含边界
     * @param endDateExclusive T+3日，不含边界
     * @param records 批量加载的收尾记录
     * @return 胎胚代码到归属日期及当日班次键的不可变索引
     */
    public static Map<String, Set<String>> buildIndex(String factoryCode, Date startDate,
            Date endDateExclusive, List<CxEmbryoEndingShift> records) {
        Map<String, Set<String>> index = new LinkedHashMap<>(16);
        if (StringUtils.isEmpty(factoryCode) || Objects.isNull(startDate)
                || Objects.isNull(endDateExclusive) || CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        String firstDate = DateUtil.formatDate(startDate);
        String endDate = DateUtil.formatDate(endDateExclusive);
        records.stream().filter(Objects::nonNull)
                .filter(record -> StringUtils.equals(factoryCode, record.getFactoryCode()))
                .filter(record -> StringUtils.isNotBlank(record.getEmbryoCode()))
                .filter(record -> Objects.nonNull(record.getShiftDate()))
                .filter(record -> DateUtil.formatDate(record.getShiftDate()).compareTo(firstDate) >= 0
                        && DateUtil.formatDate(record.getShiftDate()).compareTo(endDate) < 0)
                .forEach(record -> {
                    String shiftKey = resolveShiftKey(record.getShiftDate(), record.getShiftIndex());
                    if (Objects.nonNull(shiftKey)) {
                        index.computeIfAbsent(record.getEmbryoCode(), key -> new LinkedHashSet<>()).add(shiftKey);
                    }
                });
        index.replaceAll((embryoCode, shifts) -> Collections.unmodifiableSet(shifts));
        return Collections.unmodifiableMap(index);
    }

    /**
     * 按胎胚代码读取冻结收尾班次，同胎胚的不同产品状态共用收尾信息。
     *
     * @param context 含只读收尾索引的排程上下文
     * @param embryoCode 当前SKU或实际结果的胎胚编码
     * @return 匹配的只读班次集合；胎胚为空白、上下文为空或未匹配时返回空集合
     */
    public static Set<String> resolveEndingShifts(LhScheduleContext context, String embryoCode) {
        if (Objects.isNull(context) || StringUtils.isBlank(embryoCode)
                || CollectionUtils.isEmpty(context.getCxEmbryoEndingShiftIndex())) {
            return Collections.emptySet();
        }
        return context.getCxEmbryoEndingShiftIndex().getOrDefault(
                embryoCode, Collections.emptySet());
    }

    /**
     * 判断同胎胚、同结构占用是否已进入任一匹配成型收尾记录之后的班次。
     * 当前班次必须严格晚于匹配记录；收尾班次本身不豁免，跨天后持续生效。
     *
     * @param context 含只读收尾索引的排程上下文
     * @param statisticsStructure 当前统计结构
     * @param embryoCode SKU胎胚编码
     * @param skuStructure SKU实际所属结构
     * @param shift 当前排产班次
     * @return 是否不占用当前结构机台配额
     */
    public static boolean isExempt(LhScheduleContext context, String statisticsStructure,
            String embryoCode, String skuStructure, LhShiftConfigVO shift) {
        if (Objects.isNull(context) || Objects.isNull(shift)
                || StringUtils.isEmpty(statisticsStructure) || StringUtils.isEmpty(embryoCode)
                || !StringUtils.equals(statisticsStructure, skuStructure)
                || CollectionUtils.isEmpty(context.getCxEmbryoEndingShiftIndex())) {
            return false;
        }
        Set<String> endingShifts = resolveEndingShifts(context, embryoCode);
        String currentShift = resolveShiftKey(shift.getWorkDate(), shift.getShiftType());
        return Objects.nonNull(currentShift) && !CollectionUtils.isEmpty(endingShifts)
                && endingShifts.stream().anyMatch(endingShift -> currentShift.compareTo(endingShift) > 0);
    }

    /**
     * 统一生成跨批次恒定的班次键，兼容数据库整数1/2/3与01/02/03编码。
     * 夜班日期使用班次归属日，不使用窗口class序号或夜班开始日。
     *
     * @param shiftDate 班次归属日期
     * @param shiftType 当日班次类型
     * @return 日期与班次顺序组合键；非法日期或班次返回null
     */
    public static String resolveShiftKey(Date shiftDate, String shiftType) {
        ShiftEnum shiftEnum = ShiftEnum.getByCode(StringUtils.leftPad(shiftType, 2, '0'));
        return Objects.isNull(shiftDate) || Objects.isNull(shiftEnum)
                ? null : DateUtil.formatDate(shiftDate) + "|" + shiftEnum.getCode();
    }
}
