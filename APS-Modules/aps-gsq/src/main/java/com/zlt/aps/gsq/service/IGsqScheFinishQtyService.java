package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqScheFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程完成量回报Service接口
 *
 * @author APS Team
 * @since 2026/08/14
 */
public interface IGsqScheFinishQtyService extends IDocService<GsqScheFinishQty> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<GsqScheFinishQty> list);

    /**
     * 钢丝圈排程完成量回写钢丝圈排程结果表各班次完成量
     * 根据完成量回报数据，按工厂+钢丝圈代码+工单号+排程日期汇总后，
     * 查询排程结果表（排程日期为D-1、D、D+1）并按6班制3天窗口班次映射关系回写完成量
     *
     * @param finishQtyList 完成量回报数据列表
     * @return 回写结果
     */
    AjaxResult writeBackScheduleResultFinishQty(List<GsqScheFinishQty> finishQtyList);

    /**
     * 逻辑删除分厂指定排程日期的旧数据并批量插入新数据（事务性操作）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期
     * @param updateBy     更新者
     * @param insertList   待插入的数据列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy, List<GsqScheFinishQty> insertList);
}
