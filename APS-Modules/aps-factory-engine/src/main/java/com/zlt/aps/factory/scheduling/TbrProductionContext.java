package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import lombok.Data;

import java.util.Map;

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

}
