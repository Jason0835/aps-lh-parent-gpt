package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxProductStockLimit;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 成型投产班次库存限定设置Service接口
 * 
 * @author zlt
 * @date 2022-01-07
 */
public interface CxProductStockLimitService
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

    /**
     * 新增成型投产班次库存限定设置
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 结果
     */
    @Transactional
    public int insertCxProductStockLimit(CxProductStockLimit cxProductStockLimit);

    /**
     * 修改成型投产班次库存限定设置
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 结果
     */
    @Transactional
    public int updateCxProductStockLimit(CxProductStockLimit cxProductStockLimit);

    /**
     * 批量删除成型投产班次库存限定设置
     * 
     * @param ids 需要删除的成型投产班次库存限定设置ID
     * @return 结果
     */
    @Transactional
    public int deleteCxProductStockLimitByIds(Long[] ids);

    /**
     * 删除成型投产班次库存限定设置信息
     * 
     * @param id 成型投产班次库存限定设置ID
     * @return 结果
     */
    @Transactional
    public int deleteCxProductStockLimitById(Long id);

    /**
     * 校验成型投产班次库存限定设置唯一性
     */
    public List<CxProductStockLimit> checkCxProductStockLimitUnique(CxProductStockLimit cxProductStockLimit);

    /**
     * 导入成型投产班次库存限定设置数据
     */
    @Transactional
    public AjaxResult importData(List<CxProductStockLimit> list, boolean updateSupport, Long importLogId);
}
