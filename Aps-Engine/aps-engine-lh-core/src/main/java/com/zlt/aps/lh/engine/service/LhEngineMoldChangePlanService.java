package com.zlt.aps.lh.engine.service;


import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;

import java.util.List;

/**
 * 模具变动单Service接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface LhEngineMoldChangePlanService {
    /**
     * 查询模具变动单
     *
     * @param id 模具变动单ID
     * @return 模具变动单
     */
    public LhEngineMoldChangePlan selectLhEngineMoldChangePlanById(Long id);

    /**
     * 查询模具变动单列表
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 模具变动单集合
     */
    public List<LhEngineMoldChangePlan> selectLhEngineMoldChangePlanList(LhEngineMoldChangePlan lhEngineMoldChangePlan);

    /**
     * 新增模具变动单
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 结果
     */
    public int insertLhEngineMoldChangePlan(LhEngineMoldChangePlan lhEngineMoldChangePlan);

    /**
     * 修改模具变动单
     *
     * @param lhEngineMoldChangePlan 模具变动单
     * @return 结果
     */
    public int updateLhEngineMoldChangePlan(LhEngineMoldChangePlan lhEngineMoldChangePlan);

    /**
     * 批量删除模具变动单
     *
     * @param ids 需要删除的模具变动单ID
     * @return 结果
     */
    public int deleteLhEngineMoldChangePlanByIds(Long[] ids);

    /**
     * 删除模具变动单信息
     *
     * @param id 模具变动单ID
     * @return 结果
     */
    public int deleteLhEngineMoldChangePlanById(Long id);

    /**
     * 批量生成模具变动单
     * @param lhEngineMoldChangePlanList
     * @return
     */
    public int batchCreateMoldChangePlan(List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList);

    /**
     * 根据成型批次号进行模具变动单计划删除
     * @param cxBatchNo
     * @return
     */
    int deleteLhEngineMoldChangePlanByCxBatchNo(String cxBatchNo);

    /**
     * 根据成型工单号删除模具变动单数据
     * @param sourceCxOrder 成型工单号
     * @param list  原始硫化机台编码
     * @return
     */
    int deleteLhEngineMoldChangePlanByParams(String sourceCxOrder, List<String> list, List<Long> idList, String cxBatchNo, String moldBatchNo);

    /**
     *  根据模具变动单生成日期进行当前日期对应的模具变动单数据删除
     * @param scheduleDate
     * @return
     */
    int deleteLhEngineMoldChangePlanByScheduleDate(String scheduleDate);


}
