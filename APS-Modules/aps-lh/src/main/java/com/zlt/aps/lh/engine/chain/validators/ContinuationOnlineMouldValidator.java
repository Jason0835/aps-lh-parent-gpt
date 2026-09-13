package com.zlt.aps.lh.engine.chain.validators;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.exception.ScheduleErrorCode;
import com.zlt.aps.lh.exception.ScheduleException;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 续作在机模具与 SKU 模具关系校验器。
 *
 * <p>本校验器不注册到 S4.2 基础数据校验链，而是在 S4.4 续作处理开始前主动执行。
 * 因为续作 SKU 由 S4.3 根据 MES 在机物料归集后才形成，避免在基础数据阶段重复实现续作归集逻辑。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class ContinuationOnlineMouldValidator {

    private static final String MESSAGE_LINE_BREAK = "\n";
    /** 单条不匹配明细的多语言 key，携带机台、物料、在机模具和异常原因。 */
    private static final String MISMATCH_MESSAGE_KEY =
            "ui.data.column.lhScheduleResult.validator.continuationOnlineMouldMismatch";
    private static final String VALIDATOR_NAME_KEY =
            "ui.data.column.lhScheduleResult.validator.continuationOnlineMouldValidatorName";

    /**
     * 续作在机模具校验开关，默认开启；仅允许在隔离排程验证时通过外部配置临时关闭。
     */
    @Value("${aps.lh.validation.continuation-online-mould-enabled:true}")
    private boolean enabled = true;

    /**
     * 校验全部续作在机机台的实际模具是否属于当前 SKU 合法模具关系。
     *
     * <p>先完整扫描所有续作机台，汇总全部不匹配明细后一次性抛出业务异常，确保调用方能够
     * 一次看到所有问题；校验仍发生在任何续作排产动作之前。</p>
     *
     * @param context 排程上下文
     * @return true 表示校验通过
     * @throws ScheduleException 存在实际在机模具不属于当前 SKU 模具关系时抛出
     */
    public boolean validate(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getContinuousSkuList())) {
            return true;
        }
        if (!enabled) {
            log.warn("{}已通过配置停用，本次跳过续作在机模具校验, 工厂: {}, 批次: {}, 续作机台数: {}",
                    this.getValidatorName(), context.getFactoryCode(), context.getBatchNo(),
                    context.getContinuousSkuList().size());
            return true;
        }
        List<String> mismatchDetailList = new ArrayList<String>(context.getContinuousSkuList().size());
        for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
            if (Objects.isNull(sku)) {
                continue;
            }
            String mismatchDetail = this.validateSkuMould(context, sku);
            if (StringUtils.isNotEmpty(mismatchDetail)) {
                mismatchDetailList.add(mismatchDetail);
            }
        }
        if (!CollectionUtils.isEmpty(mismatchDetailList)) {
            /*
             * 明细先逐条写入排程上下文，由 AbsLhScheduleTemplate 构建中断响应时统一回传接口
             * validationErrors；调用方无需翻日志堆栈即可定位到具体机台、物料和模具号。
             */
            for (String mismatchDetail : mismatchDetailList) {
                context.addValidationError(mismatchDetail);
            }
            throw new ScheduleException(ScheduleStepEnum.S4_4_CONTINUOUS_PRODUCTION,
                    ScheduleErrorCode.CONTINUOUS_ONLINE_MOULD_MISMATCH,
                    context.getFactoryCode(), context.getBatchNo(),
                    String.join(MESSAGE_LINE_BREAK, mismatchDetailList));
        }
        log.info("{}通过, 工厂: {}, 批次: {}, 续作机台数: {}", this.getValidatorName(),
                context.getFactoryCode(), context.getBatchNo(), context.getContinuousSkuList().size());
        return true;
    }

    /**
     * 获取校验器名称。
     *
     * @return 国际化后的校验器名称
     */
    public String getValidatorName() {
        return I18nUtil.getMessage(VALIDATOR_NAME_KEY);
    }

    /**
     * 校验单个续作 SKU 在机机台。
     *
     * @param context 排程上下文
     * @param sku 当前续作 SKU
     * @return 当前 SKU 的完整不匹配明细；匹配时返回空值
     */
    private String validateSkuMould(LhScheduleContext context, SkuScheduleDTO sku) {
        String machineCode = StringUtils.trim(sku.getContinuousMachineCode());
        if (StringUtils.isEmpty(machineCode)) {
            return null;
        }
        LinkedHashSet<String> rawActualMouldCodeSet = LhMouldCodeUtil.resolveInMachineMouldCodeSet(
                context, machineCode);
        List<String> actualMouldCodeList = LhMouldCodeUtil.normalizeAndSortMouldCodes(
                rawActualMouldCodeSet);
        if (CollectionUtils.isEmpty(actualMouldCodeList)) {
            return null;
        }

        List<MdmSkuMouldRel> mouldRelList = this.resolveSkuMouldRelList(
                context, sku.getMaterialCode());
        List<String> allowedMouldCodeList = this.resolveAllowedMouldCodes(mouldRelList);
        Set<String> allowedMouldCodeSet = new LinkedHashSet<String>(allowedMouldCodeList);
        List<String> invalidMouldCodeList = new ArrayList<String>(actualMouldCodeList.size());
        for (String actualMouldCode : actualMouldCodeList) {
            if (!allowedMouldCodeSet.contains(actualMouldCode)) {
                invalidMouldCodeList.add(actualMouldCode);
            }
        }
        if (CollectionUtils.isEmpty(invalidMouldCodeList)) {
            return null;
        }

        String materialCode = StringUtils.trim(sku.getMaterialCode());
        String detailMessage = this.buildMismatchDetailMessage(machineCode, materialCode,
                actualMouldCodeList);
        log.error("{}失败, 工厂: {}, 批次: {}, 机台编码: {}, "
                        + "当前SKU/物料编码: {}, 当前在机模具号: {}, SKU允许的模具号: {}, "
                        + "不匹配模具号: {}",
                this.getValidatorName(), context.getFactoryCode(), context.getBatchNo(), machineCode, materialCode,
                actualMouldCodeList, allowedMouldCodeList, invalidMouldCodeList);
        return detailMessage;
    }

    /**
     * 构建单条续作在机模具不匹配明细。
     *
     * <p>明细携带机台编码、物料编码和在机模具号，由 {@link LhScheduleContext#addValidationError(String)}
     * 写入上下文后随中断响应返回接口；允许模具号与不匹配模具号只保留在过程日志中。</p>
     *
     * @param machineCode 机台编码
     * @param materialCode SKU物料编码
     * @param actualMouldCodeList 实际在机模具号
     * @return 可直接返回接口的明细文案
     */
    private String buildMismatchDetailMessage(String machineCode, String materialCode,
                                             List<String> actualMouldCodeList) {
        // 模具号直接传入集合，保持与日志、既有测试一致的 [M001, M002] 展示口径。
        return MessageFormat.format(I18nUtil.getMessage(MISMATCH_MESSAGE_KEY),
                machineCode, materialCode, actualMouldCodeList);
    }

    /**
     * 获取当前 SKU 的模具关系列表。
     *
     * @param context 排程上下文
     * @param materialCode SKU 物料编码
     * @return SKU 模具关系列表
     */
    private List<MdmSkuMouldRel> resolveSkuMouldRelList(LhScheduleContext context,
                                                         String materialCode) {
        Map<String, List<MdmSkuMouldRel>> skuMouldRelMap = context.getSkuMouldRelMap();
        if (CollectionUtils.isEmpty(skuMouldRelMap) || StringUtils.isEmpty(materialCode)) {
            return Collections.emptyList();
        }
        List<MdmSkuMouldRel> mouldRelList = skuMouldRelMap.get(materialCode);
        if (Objects.nonNull(mouldRelList)) {
            return mouldRelList;
        }
        String normalizedMaterialCode = StringUtils.trim(materialCode);
        return skuMouldRelMap.getOrDefault(normalizedMaterialCode, Collections.<MdmSkuMouldRel>emptyList());
    }

    /**
     * 按现有模具号标准化逻辑整理 SKU 允许使用的模具号。
     *
     * @param mouldRelList SKU 模具关系列表
     * @return 去空、去重、排序后的允许模具号
     */
    private List<String> resolveAllowedMouldCodes(List<MdmSkuMouldRel> mouldRelList) {
        if (CollectionUtils.isEmpty(mouldRelList)) {
            return Collections.emptyList();
        }
        List<String> mouldCodeTextList = new ArrayList<String>(mouldRelList.size());
        for (MdmSkuMouldRel mouldRel : mouldRelList) {
            if (Objects.nonNull(mouldRel)) {
                mouldCodeTextList.add(mouldRel.getMouldCode());
            }
        }
        return LhMouldCodeUtil.normalizeAndSortMouldCodes(mouldCodeTextList);
    }
}
