package com.zlt.aps.mp.engine.logrecorder;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanLogDto;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.TbrMouldProductionLogType;
import com.zlt.aps.mp.engine.handler.GroupPrioritySchedulerResultHelper;
import com.zlt.aps.mp.engine.utils.TbrProductionLogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TBR 模拟排产日志记录器
 *
 * @author ZLT
 * @date 20260127
 */
@Slf4j
public class TbrSimulateProductionLogRecorder {
    /**
     * 增加开始分组计划模拟排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始分组计划模拟模具排产====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartMouldProductionLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始分组计划模拟模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模拟模具排产数据重置完成日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组计划模拟模具排产数据重置完成====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addResetDataFinishLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，分组计划模拟模具排产数据重置完成====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 排产模式 效率优先或是交付优先 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，采用%s模式进行排产====
     *
     * @param context        排程上下文
     * @param productionMode 排产模式
     * @return
     */
    public static String addProductionModeLog(Context context, Integer productionMode) {
        String productionModeText = YesOrNoEnum.YES.getValue().equals(productionMode) ? "订单高优先级交付优先" : "生产切换效率优先";
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，采用%s模式进行排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                productionModeText);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 开始交付优先排产模式日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始进行交付优先排产模式进行模拟排产====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addStartDeliveryPriorityLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，开始进行交付优先排产模式进行模拟排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 分组匹配优先级最高机台 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，订单高优先级交付优先：分组名 %s 最适配机台 %s====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addHeightPriorityMatchLog(Context context, String groupName, CxMachineBaseInfoVo cxMachineInfo) {
        Set<Integer> preDaySet = Optional.ofNullable(cxMachineInfo.getSelectedProductionDaySet()).orElse(Collections.emptySet());
        String productionDays = preDaySet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，订单高优先级交付优先：分组名 %s 最适配机台 %s 预期排产日：%s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineInfo.getCxMachineCode(), productionDays);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 交付优先排产需要剔除的分组信息 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产模式需要排产的分组信息：%s====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addHeightPriorityExcludeGroupInfo(Context context, Set<String> excludeGroupPlan) {
        if (CollectionUtils.isEmpty(excludeGroupPlan)) {
            return "";
        }
        String groupInfo = excludeGroupPlan.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产模式需要排产的分组信息：%s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }


    /**
     * 增加 交付优先排产获取到的Top列表 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产当前Top选择的分组信息：%s====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addSelectedHeightPriorityGroupInfo(Context context, Set<String> selectedTopInfo) {
        if (CollectionUtils.isEmpty(selectedTopInfo)) {
            return "";
        }
        String groupInfo = selectedTopInfo.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产当前Top选择的分组信息：%s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupInfo);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }
    /**
     * 增加 分组匹配优先级最高机台 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，订单高优先级交付优先：Top3最终选定 分组名 %s 选定机台 %s====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addFinalSelectedGroupLog(Context context, GroupPrioritySchedulerResultHelper finalSelected) {
        if (null == finalSelected) {
            return "";
        }
        String groupName = finalSelected.getPreSelectedGroupName();
        String cxMachineCode = finalSelected.getSelectedCxMachineCode();
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，订单高优先级交付优先：Top3最终选定 分组名 %s 选定机台 %s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, cxMachineCode);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 交付优先排产 结束 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产模式模拟排产结束====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addEndDeliveryPriorityLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产模式模拟排产结束====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 交付优先排产模式因结构固定重排续作日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产因结构固定重排续作====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addDeliveryPriorityResetContinueLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产因结构固定重排续作====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 交付优先排产模式因结构固定优先分组挑选固定机台日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产：分组固定优先挑选固定机台====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addDeliveryPriorityFixedCxMachineGroupLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产：分组固定优先挑选固定机台====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 交付优先排产模式因结构固定优先分组挑选固定机台日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产：[%s]%s====
     * typeText: 指定分组分配同机台时，时间在前优先排产
     *
     * @param context   排程上下文
     * @param groupInfo 分组信息
     * @param typeText  分段信息
     * @return
     */
    public static String addDeliveryPriorityTypeLog(Context context, String groupInfo, String typeText) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产：[%s]%s====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupInfo, typeText);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 交付优先排产模式剩余分组排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产：剩余分组需求排产====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addDeliveryPriorityLeftOverGroupLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，交付优先排产：剩余分组需求排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 不同分组续作模具按分配比例调整后 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，不同分组续作模具按分配比例调整完成====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addFinishedByGroupMoldRatioLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，不同分组续作模具按分配比例调整完成====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加 不同分组续作模具按分配比例调整后重排续作 日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，不同分组续作模具按分配比例调整完成重排续作====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addResetProductionContinueLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，不同分组续作模具按分配比例调整完成重排续作====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产没有需要排产的数据日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产没有排产的数据====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addDataEmptyLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产没有排产的数据====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产排产在机结构====
     *
     * @param context 排程上下文
     * @return
     */
    public static String addProductionContinueGroupLog(Context context) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始分组计划模具排产,排产在机结构====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模拟模具排产-排产在机结构续作Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s 续作Sku 模拟模具排产====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupSingleGroupLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-排产在机结构新增Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始在机结构 %s 新增Sku 模具排产====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @return
     */
    public static String addProductionContinueGroupSingleGroupAddSkuLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始在机结构 %s 新增Sku 模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模拟排产-排产在机结构没有排产计划日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产没有排产计划====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupNoGroupPlanLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产没有排产计划====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模拟排产-排产在机结构没有分配到机台日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产没有分配到机台产能====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupNoAllocationCxMachineLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContentFormat = "=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产没有分配到机台产能====";
        String logContent = String.format(logContentFormat,
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模拟排产-排产在机结构没有续作Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产没有续作Sku信息====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupNoContinueSkuLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产没有续作Sku信息====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.SIMULATE_MOULD_PRODUCTION, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模拟排产-排产在机结构续作Sku日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 模拟排产开始排产Sku====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @param type      续作类型
     * @return
     */
    public static String addProductionContinueGroupSkuLog(Context context, String groupName, ContinueTypeEnum type) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，在机结构 %s %s 开始模拟排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName, type.getDesc());
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_CONTINUE_GROUP_SINGLE_GROUP_NO_CONTINUE_SKU, logContent);
        return logContent;
    }

    /**
     * 增加分组计划模具正式排产-新增结构排产日志信息记录
     * =====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始新增结构 %s 模具排产====
     *
     * @param context   排程上下文
     * @param groupName 分组名 TBR 结构名
     * @return
     */
    public static String addProductionAddGroupSingleGroupLog(Context context, String groupName) {
        String logContent = String.format("=====工厂%s, 计划年月：%d-%d, 需求计划版本：%s, 排产版本：%s，正式开始新增结构 %s 模具排产====",
                context.getFactoryCode(), context.getYear(), context.getMonth(), context.getMonthPlanVersion(), context.getProductionVersion(),
                groupName);
        ProductionPlanLogDto productionPlanInfo = ProductionPlanLogDto.getEmpty();
        TbrProductionLogUtils.addProductionLog(context, productionPlanInfo, TbrMouldProductionLogType.FORMAL_MOULD_ADD_GROUP_SINGLE_GROUP, logContent);
        return logContent;
    }

    private TbrSimulateProductionLogRecorder() {

    }
}
