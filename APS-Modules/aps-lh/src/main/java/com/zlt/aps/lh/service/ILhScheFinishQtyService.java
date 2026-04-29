package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 硫化排程完成量回报Service接口
 *
 * @author APS Team
 * @since 2026/04/09
 */
public interface ILhScheFinishQtyService extends IDocService<LhScheFinishQty> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<LhScheFinishQty> list);

    /**
     * 硫化排程完成量回写硫化排程结果表各班次完成量
     * 根据完成量回报数据，按机台+物料+排程日期汇总后，
     * 查询排程结果表（排程日期为D、D+1、D+2）并按班次映射关系回写完成量
     *
     * @param finishQtyList 完成量回报数据列表
     * @return 回写结果
     */
    AjaxResult writeBackScheduleResultFinishQty(List<LhScheFinishQty> finishQtyList);
}
