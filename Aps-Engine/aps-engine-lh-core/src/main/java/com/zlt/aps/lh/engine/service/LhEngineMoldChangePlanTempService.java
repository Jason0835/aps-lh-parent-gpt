package com.zlt.aps.lh.engine.service;


import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;

import java.util.Date;
import java.util.List;

/**
 * 模具变动单Service接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface LhEngineMoldChangePlanTempService {

    /**
     * 查询模具变动单列表
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 模具变动单集合
     */
    public List<LhEngineMoldChangePlan> selectLhEngineMoldChangePlanList(LhEngineMoldChangePlan lhEngineMoldChangePlan);


    /**
     * 批量生成模具变动单
     * @param lhEngineMoldChangePlanList
     * @return
     */
    public int batchCreateMoldChangePlan(List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList);


    /**
     * 根据成型工单号删除模具变动单数据
     * @param sourceCxOrder 成型工单号
     * @param list  原始硫化机台编码
     * @return
     */
    int deleteLhEngineMoldChangePlanByParams(String sourceCxOrder, List<String> list, List<Long> idList, Date scheduleDate);


}
