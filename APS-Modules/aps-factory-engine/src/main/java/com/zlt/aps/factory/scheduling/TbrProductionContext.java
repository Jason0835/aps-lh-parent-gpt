package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 全钢排产上下文
 *
 * @author ZLT
 * @date 20251210
 */
@Data
public class TbrProductionContext extends Context {
    /**
     * 分组排产计划
     * key 结构名 value 排产计划集合
     */
    Map<String, ProductionPlanGroupInfo> groupProductionInfo;
    /**
     * 成型产能信息集合
     * key cxMachineCode value 成型机信息
     */
    Map<String, CxMachineBaseInfoVo> cxMachineBaseInfo;
    /**
     * 反向匹配成型机台
     */
    Set<String> reverseFindSet;

    /**
     * 加入收尾，方向匹配结构集合
     *
     * @param cxMachineCode
     */
    public void addReverseMachine(String cxMachineCode) {
        if (StringUtils.isBlank(cxMachineCode)) {
            return;
        }
        if (null == reverseFindSet) {
            reverseFindSet = new HashSet<>();
        }
        reverseFindSet.add(cxMachineCode);
    }
}
