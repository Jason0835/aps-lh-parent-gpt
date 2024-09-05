package com.zlt.aps.cx.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxStock;

import java.util.List;


/**
 * 成型库存信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface CxStockService {
    /**
     * 查询成型库存信息
     *
     * @param id 成型库存信息ID
     * @return 成型库存信息
     */
    public CxStock selectCxStockById(Long id);

    /**
     * 查询成型库存信息列表
     *
     * @param cxStock 成型库存信息
     * @return 成型库存信息集合
     */
    public List<CxStock> selectCxStockList(CxStock cxStock);

    /**
     * 新增成型库存信息
     *
     * @param cxStock 成型库存信息
     * @return 结果
     */
    public int insertCxStock(CxStock cxStock);

    /**
     * 修改成型库存信息
     *
     * @param cxStock 成型库存信息
     * @return 结果
     */
    public int updateCxStock(CxStock cxStock);

    /**
     * 批量删除成型库存信息
     *
     * @param ids 需要删除的成型库存信息ID
     * @return 结果
     */
    public int deleteCxStockByIds(Long[] ids);

    /**
     * 删除成型库存信息信息
     *
     * @param id 成型库存信息ID
     * @return 结果
     */
    public int deleteCxStockById(Long id);

    /**
     * 校验胎面库存唯一性（根据库存日期+物料编号+id）
     */
    public List<CxStock> checkCxStockListUnic(CxStock cxStock);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxStock> list, boolean updateSupport, Long importLogId);

    /**
     * 释放不可用库存
     * @param ids
     * @return
     */
    int releaseStock(Long[] ids);
}
