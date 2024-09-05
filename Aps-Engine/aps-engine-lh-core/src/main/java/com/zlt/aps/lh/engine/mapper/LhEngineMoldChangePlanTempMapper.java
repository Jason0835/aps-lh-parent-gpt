package com.zlt.aps.lh.engine.mapper;


import com.zlt.aps.lh.engine.domain.LhEngineMoldChangePlan;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 模具变动单临时表Mapper接口
 *
 * @author zlt
 * @date 2021-09-03
 */
public interface LhEngineMoldChangePlanTempMapper {
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
     * @param LhEngineMoldChangePlan 模具变动单
     * @return 模具变动单集合
     */
    public List<LhEngineMoldChangePlan> selectLhEngineMoldChangePlanList(LhEngineMoldChangePlan LhEngineMoldChangePlan);

    /**
     * 批量生成模具变动单
     * @param lhEngineMoldChangePlanList
     * @return
     */
    public int batchCreateMoldChangePlan(@Param("list") List<LhEngineMoldChangePlan> lhEngineMoldChangePlanList);

    /**
     * 根据成型批次号进行模具变动单计划删除
     * @param cxBatchNo
     * @return
     */
    int deleteLhEngineMoldChangePlanByCxBatchNo(@Param("cxBatchNo") String cxBatchNo);

    /**
     * 根据条件将复合条件的模具变动单数据转存到日志表
     * @param sourceCxOrder
     * @param list
     * @return
     */
    int syncMoldChagePlanToLog(@Param("sourceCxOrder") String sourceCxOrder, @Param("list") List<String> list, @Param("idList") List<Long> idList);

    /**
     * 根据原始成型工单号 和机台编号列表进行模具变动单数据删除
     * @param sourceCxOrder
     * @param list
     * @return
     */
    int deleteLhEngineMoldChangePlanByParams(@Param("sourceCxOrder") String sourceCxOrder, @Param("list") List<String> list, @Param("idList") List<Long> idList, @Param("scheduleDate") Date scheduleDate);
}
