package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxProductDimensionLimit;

/**
 * 成型投产班次同寸口硫化班次限定设置Mapper接口
 * 
 * @author zlt
 * @date 2022-01-08
 */
public interface CxProductDimensionLimitMapper 
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
    public List<CxProductDimensionLimit> checkCxProductDimensionLimitUnique(CxProductDimensionLimit cxProductDimensionLimit);


    /**
     * 新增成型投产班次同寸口硫化班次限定设置
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 结果
     */
    public int insertCxProductDimensionLimit(CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 修改成型投产班次同寸口硫化班次限定设置
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 结果
     */
    public int updateCxProductDimensionLimit(CxProductDimensionLimit cxProductDimensionLimit);

    /**
     * 删除成型投产班次同寸口硫化班次限定设置
     * 
     * @param id 成型投产班次同寸口硫化班次限定设置ID
     * @return 结果
     */
    public int deleteCxProductDimensionLimitById(Long id);

    /**
     * 批量删除成型投产班次同寸口硫化班次限定设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxProductDimensionLimitByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxProductDimensionLimit> list);
}
