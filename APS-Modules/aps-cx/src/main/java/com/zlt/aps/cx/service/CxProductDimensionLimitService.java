package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxProductDimensionLimit;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 成型投产班次同寸口硫化班次限定设置Service接口
 * 
 * @author zlt
 * @date 2022-01-08
 */
public interface CxProductDimensionLimitService
{
    /**
     * 查询成型投产班次同寸口硫化班次限定设置
     * 
     * @param id 成型投产班次同寸口硫化班次限定设置ID
     * @return 成型投产班次同寸口硫化班次限定设置
     */
    public CxProductDimensionLimit selectCxProductDimensionLimitById(Long id);

    /**
     * 查询成型投产班次同寸口硫化班次限定设置列表
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 成型投产班次同寸口硫化班次限定设置集合
     */
    public List<CxProductDimensionLimit> selectCxProductDimensionLimitList(CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 新增成型投产班次同寸口硫化班次限定设置
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 结果
     */
    @Transactional
    public int insertCxProductDimensionLimit(CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 修改成型投产班次同寸口硫化班次限定设置
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 结果
     */
    @Transactional
    public int updateCxProductDimensionLimit(CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 批量删除成型投产班次同寸口硫化班次限定设置
     * 
     * @param ids 需要删除的成型投产班次同寸口硫化班次限定设置ID
     * @return 结果
     */
    @Transactional
    public int deleteCxProductDimensionLimitByIds(Long[] ids);

    /**
     * 删除成型投产班次同寸口硫化班次限定设置信息
     * 
     * @param id 成型投产班次同寸口硫化班次限定设置ID
     * @return 结果
     */
    @Transactional
    public int deleteCxProductDimensionLimitById(Long id);

    /**
     * 校验成型投产班次同寸口硫化班次限定设置唯一性
     */
    public String checkCxProductDimensionLimitUnique(CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 导入成型投产班次同寸口硫化班次限定设置数据
     */
    @Transactional
    public AjaxResult importData(List<CxProductDimensionLimit> list, boolean updateSupport, Long importLogId);
}
