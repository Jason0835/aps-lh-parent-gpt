package com.zlt.aps.mps.mapper;


import com.zlt.aps.cx.api.domain.entity.CxStock;

import java.util.List;

/**
 * 成型库存信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface CxStockMapper {
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
     * 删除成型库存信息
     *
     * @param id 成型库存信息ID
     * @return 结果
     */
    public int deleteCxStockById(Long id);

    /**
     * 批量删除成型库存信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxStockByIds(Long[] ids);

    /**
     * 校验库存唯一性
     */
    public List<CxStock> checkCxStockListUnic(CxStock cxStock);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxStock> list);


}
