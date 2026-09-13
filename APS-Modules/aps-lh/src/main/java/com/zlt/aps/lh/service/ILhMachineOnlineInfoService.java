package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 硫化在机信息Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ILhMachineOnlineInfoService extends IDocService<LhMachineOnlineInfo> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<LhMachineOnlineInfo> list);

    /**
     * 逻辑删除分厂指定在线日期的旧数据并批量插入新数据（事务性操作）
     * 如果插入失败，删除操作也会回滚，保证数据一致性
     *
     * @param factoryCode 分厂编号
     * @param onlineDate  在线日期
     * @param updateBy    更新者
     * @param insertList  待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date onlineDate, String updateBy, List<LhMachineOnlineInfo> insertList);

    /**
     * 按维度键UPSERT批量保存（MES同步新模式）
     * 维度键 = 工厂 + 机台 + 日期
     * 键存在则更新（命中已逻辑删除的行会回填ID并复活），不存在则插入；
     * 不做对账删除，MES未上报的既有键保持原状
     *
     * @param list 待保存的数据列表
     * @return 实际处理的数据条数
     */
    int upsertBatch(List<LhMachineOnlineInfo> list);
}
