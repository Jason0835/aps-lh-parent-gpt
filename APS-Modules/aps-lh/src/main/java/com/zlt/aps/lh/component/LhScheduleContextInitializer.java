package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.enums.FactoryCodeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 硫化排程上下文时间轴初始化器。
 *
 * <p>把「工厂 + 排程目标日 → 排程配置快照 + 窗口时间轴」的构建口径收敛到一处，
 * 供正式排程入口与只读报表入口共用，避免两边各自推算 T 日、窗口结束日和提前生产截止日导致口径漂移。</p>
 *
 * <p>本类只写上下文的工厂与日期字段，不访问数据库（参数查询由
 * {@link LhScheduleConfigResolver} 完成），也不触发任何排程计算。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class LhScheduleContextInitializer {

    /**
     * 硫化排程配置解析器
     */
    @Resource
    private LhScheduleConfigResolver scheduleConfigResolver;

    /**
     * 初始化工厂与排程窗口时间轴。
     *
     * @param context           排程上下文（新建对象）
     * @param factoryCode       分厂编码
     * @param scheduleTargetDate 排程目标日（业务口径 T+1，为空时取当天）
     */
    public void initialize(LhScheduleContext context, String factoryCode, Date scheduleTargetDate) {
        // 1. 工厂信息：配置解析与后续日志都依赖工厂编码
        context.setFactoryCode(factoryCode);
        // 工厂名称来源于工厂枚举：116=越南，117=泰国
        context.setFactoryName(FactoryCodeEnum.getFactoryNameByCode(factoryCode));

        // 2. 排程目标日为业务保存日期（T+1），统一清零到自然日
        Date target = LhScheduleTimeUtil.clearTime(
                scheduleTargetDate != null ? scheduleTargetDate : new Date());
        context.setScheduleTargetDate(target);

        // 3. 解析硫化参数形成配置快照（排程天数、停产保机观察天数等）
        scheduleConfigResolver.resolveAndAttach(context);

        // 4. 反推窗口起点 T 日：T = 目标日 - (排程天数 - 2)，保证窗口仍为 T～T+2
        int scheduleDays = context.getScheduleConfig().getScheduleDays();
        int offsetDays = Math.max(0, scheduleDays - 2);
        context.setScheduleDate(LhScheduleTimeUtil.addDays(target, -offsetDays));

        // 5. 排程窗口结束日 = T + (排程天数 - 1)，即业务口径的 T+2
        context.setWindowEndDate(LhScheduleTimeUtil.addDays(context.getScheduleDate(), scheduleDays - 1));

        // 6. 提前生产可拉取的最晚原始计划日期 = 窗口结束日 + SYS0304028
        int earlyProductionDays = context.getScheduleConfig().getEarlyProductionDaysThreshold();
        context.setEarlyProductionMaxDate(
                LhScheduleTimeUtil.addDays(context.getWindowEndDate(), earlyProductionDays));

        log.info("排程上下文时间轴初始化完成, 工厂: {}, 工厂名称: {}, 目标日(业务): {}, T日: {}, "
                        + "窗口结束日: {}, 提前生产额外天数: {}, 提前生产最晚计划日: {}, 排程天数: {}",
                context.getFactoryCode(), context.getFactoryDisplayName(),
                LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                LhScheduleTimeUtil.formatDate(context.getWindowEndDate()),
                earlyProductionDays,
                LhScheduleTimeUtil.formatDate(context.getEarlyProductionMaxDate()),
                scheduleDays);
    }
}
