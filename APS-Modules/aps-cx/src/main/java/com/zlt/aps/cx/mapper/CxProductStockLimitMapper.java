package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxProductStockLimit;

/**
 * 成型投产班次库存限定设置Mapper接口
 * 
 * @author zlt
 * @date 2022-01-07
 */
public interface CxProductStockLimitMapper 
{
    /**
     * 查询成型投产班次库存限定设置
     * 
     * @param id 成型投产班次库存限定设置ID
     * @return 成型投产班次库存限定设置
     */
    public CxProductStockLimit selectCxProductStockLimitById(Long id);

    /**
     * 查询成型投产班次库存限定设置列表
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 成型投产班次库存限定设置集合
     */
    public List<CxProductStockLimit> selectCxProductStockLimitList(CxProductStockLimit cxProductStockLimit);
    public List<CxProductStockLimit> checkCxProductStockLimitUnique(CxProductStockLimit cxProductStockLimit);


    /**
     * 新增成型投产班次库存限定设置
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 结果
     */
    public int insertCxProductStockLimit(CxProductStockLimit cxProductStockLimit);

    /**
     * 修改成型投产班次库存限定设置
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 结果
     */
    public int updateCxProductStockLimit(CxProductStockLimit cxProductStockLimit);

    /**
     * 删除成型投产班次库存限定设置
     * 
     * @param id 成型投产班次库存限定设置ID
     * @return 结果
     */
    public int deleteCxProductStockLimitById(Long id);

    /**
     * 批量删除成型投产班次库存限定设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxProductStockLimitByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxProductStockLimit> list);
}
