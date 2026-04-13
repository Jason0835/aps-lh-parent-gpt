package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 硫化排程日完成量Service接口
 *
 * @author APS Team
 * @since 2026/04/13
 */
public interface ILhDayFinishQtyService extends IDocService<LhDayFinishQty> {

    /**
     * 批量保存或更新数据
     *
     * @param list 数据列表
     * @return 结果
     */
    int saveOrUpdateBatch(List<LhDayFinishQty> list);

    /**
     * 导入数据
     *
     * @param list 数据列表
     * @param updateSupport 是否更新支持
     * @param importLogId 导入日志ID
     * @return 结果
     */
    AjaxResult importData(List<LhDayFinishQty> list, boolean updateSupport, Long importLogId);

    /**
     * 查询列表
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    List<LhDayFinishQty> selectList(LhDayFinishQty queryVO);
}
