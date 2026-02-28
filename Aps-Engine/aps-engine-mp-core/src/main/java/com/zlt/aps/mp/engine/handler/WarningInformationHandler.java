package com.zlt.aps.mp.engine.handler;

import com.ruoyi.common.core.context.SecurityContextHolder;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Set;

/**
 * 预警信息处理器
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260228
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarningInformationHandler {

    private final MessageServiceUtils messageServiceAdapter;

    /**
     * 异步发送预警信息业务
     *
     * @param context
     */
    @Async("taskExecutor")
    public void sendWarningInformation(Context context, RequestAttributes requestAttributes, String userName, String factoryName) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //特殊原材料库存不足
        sendStockLimitMessage(productionContext, requestAttributes, userName, factoryName);
        //没有达到起排量
        sendNoReachMinSpecialMaterialStandardMessage(productionContext, requestAttributes, userName, factoryName);
    }

    /**
     * 发送特殊原材料库存不足预警
     *
     * @param productionContext 排产上下文
     * @param requestAttributes 线程信息
     * @param userName          用户
     * @param factoryName       工厂
     */
    private void sendStockLimitMessage(TbrProductionContext productionContext, RequestAttributes requestAttributes, String userName, String factoryName) {
        //1. 获取是否有特殊材料库存不足
        Set<String> limitSkuSet = productionContext.getSpecialMaterialStockLimitSkuInfo();
        if (CollectionUtils.isEmpty(limitSkuSet)) {
            log.info("特殊材料库存可满足净需求");
            return;
        }
        log.info("特殊原材料库存不能满足净需求");
        RequestContextHolder.setRequestAttributes(requestAttributes);
        SecurityContextHolder.setUserName(userName);
        String templateCode = MsgTemplateEnums.MP_SPECIAL_MATERIAL_STOCK_LIMIT.getCode();
        //2.发送消息
        messageServiceAdapter.sendNoticeByAsync(templateCode, null, SecurityUtils.getUsername(), factoryName,
                productionContext.getYear(),
                productionContext.getMonth(),
                productionContext.getProductionVersion());
    }

    /**
     * 发送特殊结构没有达到最小起排量
     *
     * @param productionContext 排产上下文
     * @param requestAttributes 线程信息
     * @param userName          用户
     * @param factoryName       工厂
     */
    private void sendNoReachMinSpecialMaterialStandardMessage(TbrProductionContext productionContext, RequestAttributes requestAttributes, String userName, String factoryName) {
        boolean isNoWarning = null == productionContext.getReachMinSpecialMaterialStandard() ? true : productionContext.getReachMinSpecialMaterialStandard();
        if (isNoWarning) {
            log.info("Sku净需求已经达到特殊材料最小起排量");
            return;
        }
        log.info("Sku净需求未达到特殊材料最小起排量");
        RequestContextHolder.setRequestAttributes(requestAttributes);
        SecurityContextHolder.setUserName(userName);
        String templateCode = MsgTemplateEnums.MP_NO_REACH_MIN_SPECIAL_MATERIAL.getCode();
        //2.发送消息
        messageServiceAdapter.sendNoticeByAsync(templateCode, null, SecurityUtils.getUsername(), factoryName,
                productionContext.getYear(),
                productionContext.getMonth(),
                productionContext.getProductionVersion());
    }

}
