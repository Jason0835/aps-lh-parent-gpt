package com.zlt.aps.cx.engine.mapper;

import com.zlt.aps.cx.engine.domain.CxEngineMonthStock;
import com.zlt.aps.cx.engine.domain.CxEngineStock;

import java.util.List;

/**
 * 成型库存信息Mapper接口
 * 
 * @author Joran.zhang
 * @date 2021-07-14
 */
public interface CxEngineStockMapper 
{
    /**
     * 查询成型库存信息
     * 
     * @param id 成型库存信息ID
     * @return 成型库存信息
     */
    public CxEngineStock selectCxEngineStockById(Long id);

    /**
     * 查询成型库存信息列表
     * 
     * @param cxEngineStock 成型库存信息
     * @return 成型库存信息集合
     */
    public List<CxEngineStock> selectCxEngineStockList(CxEngineStock cxEngineStock);

    /**
     * 新增成型库存信息
     * 
     * @param cxEngineStock 成型库存信息
     * @return 结果
     */
    public int insertCxEngineStock(CxEngineStock cxEngineStock);

    /**
     * 修改成型库存信息
     * 
     * @param cxEngineStock 成型库存信息
     * @return 结果
     */
    public int updateCxEngineStock(CxEngineStock cxEngineStock);

    /**
     * 删除成型库存信息
     * 
     * @param id 成型库存信息ID
     * @return 结果
     */
    public int deleteCxEngineStockById(Long id);

    /**
     * 批量删除成型库存信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxEngineStockByIds(Long[] ids);

    /**
     * 查询上月月结库存信息
     * @param cxEngineMonthStock
     * @return
     */
    public List<CxEngineMonthStock> selectCxEngineMonthStockList(CxEngineMonthStock cxEngineMonthStock);
}
