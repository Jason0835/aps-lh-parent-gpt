package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 在机结构，续作Sku续作模具数处理控制
 * 如果在机结构成型机台数 > 估算需要使用的成型机台数，则优先释放配比高的机台，其他成型机编号大的
 *
 * @author ZLT
 * @date 20251223
 */
@Slf4j
public class CxContinueSkuAllocationMouldHandler {
    /**
     * 根据结构在机机台及实际使用机台数，
     * 对续作Sku进行续作模具数调整
     * 并分配到成型机台
     *
     * @param groupContinueInfo
     * @param realWholeMachineNumber
     */
    public static void allocationContinueSkuMouldNumber(CxContinueInfoHelper groupContinueInfo, Integer realWholeMachineNumber) {
        if (null == groupContinueInfo || CollectionUtils.isEmpty(groupContinueInfo.getCxMachineCodeSet())) {
            return;
        }
        Set<String> cxMachineCodeSet = groupContinueInfo.getCxMachineCodeSet();
        if (isBuilderFullLhMachine(cxMachineCodeSet, realWholeMachineNumber)) {
            //todo 构建续作？
            return;
        }
        //构建配比大，机台编号大的需要释放的硫化机台数，按sku续作模具数多的优先减，其次排产需求量少的优先减模具数，直到减到满足硫化配比机台数为止

    }

    /**
     * 是否需要构建满机台的续作信息
     *
     * @param cxMachineCodeSet       续作机台信息
     * @param realWholeMachineNumber 需要使用的机台数
     * @return
     */
    private static boolean isBuilderFullLhMachine(Set<String> cxMachineCodeSet, Integer realWholeMachineNumber) {
        if (null == realWholeMachineNumber || realWholeMachineNumber <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        return cxMachineCodeSet.size() <= realWholeMachineNumber;
    }
}
