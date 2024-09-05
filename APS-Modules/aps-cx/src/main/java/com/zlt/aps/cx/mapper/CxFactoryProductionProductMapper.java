package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxFactoryProductionProduct;
import org.apache.ibatis.annotations.Param;

/**
 * 排程给主计划的成型在产品种Mapper接口
 * 
 * @author zlt
 * @date 2021-09-19
 */
public interface CxFactoryProductionProductMapper 
{
    /**
     * 查询排程给主计划的成型在产品种
     * 
     * @param 主键 排程给主计划的成型在产品种ID
     * @return 排程给主计划的成型在产品种
     */
    public CxFactoryProductionProduct selectCxFactoryProductionProductById(Long id);

    /**
     * 查询排程给主计划的成型在产品种列表
     * 
     * @param cxFactoryProductionProduct 排程给主计划的成型在产品种
     * @return 排程给主计划的成型在产品种集合
     */
    public List<CxFactoryProductionProduct> selectCxFactoryProductionProductList(CxFactoryProductionProduct cxFactoryProductionProduct);

    /**
     * 新增排程给主计划的成型在产品种
     * 
     * @param cxFactoryProductionProduct 排程给主计划的成型在产品种
     * @return 结果
     */
    public int insertCxFactoryProductionProduct(CxFactoryProductionProduct cxFactoryProductionProduct);

    /**
     * 修改排程给主计划的成型在产品种
     * 
     * @param cxFactoryProductionProduct 排程给主计划的成型在产品种
     * @return 结果
     */
    public int updateCxFactoryProductionProduct(CxFactoryProductionProduct cxFactoryProductionProduct);

    /**
     * 删除排程给主计划的成型在产品种
     * 
     * @param 主键 排程给主计划的成型在产品种ID
     * @return 结果
     */
    public int deleteCxFactoryProductionProductById(Long id);

    /**
     * 批量删除排程给主计划的成型在产品种
     * 
     * @param 主键s 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxFactoryProductionProductByIds(Long[] ids);

    /**
     * 批量删除给主计划的成型在产品种
     * @param year
     * @param month
     */
    void deleteByYearAndMonth(@Param("year") String year,@Param("month") String month);
}
