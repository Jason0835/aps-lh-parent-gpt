package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxStockLocationMapping;

/**
 * 库存地点映射Mapper接口
 * 
 * @author zlt
 * @date 2021-11-15
 */
public interface CxStockLocationMappingMapper 
{
    /**
     * 查询库存地点映射
     * 
     * @param id 库存地点映射ID
     * @return 库存地点映射
     */
    public CxStockLocationMapping selectCxStockLocationMappingById(Long id);

    /**
     * 查询库存地点映射列表
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 库存地点映射集合
     */
    public List<CxStockLocationMapping> selectCxStockLocationMappingList(CxStockLocationMapping cxStockLocationMapping);

    public List<CxStockLocationMapping> ckeckCxStockLocationMappingUnique(CxStockLocationMapping cxStockLocationMapping);
    /**
     * 新增库存地点映射
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 结果
     */
    public int insertCxStockLocationMapping(CxStockLocationMapping cxStockLocationMapping);

    /**
     * 修改库存地点映射
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 结果
     */
    public int updateCxStockLocationMapping(CxStockLocationMapping cxStockLocationMapping);

    /**
     * 删除库存地点映射
     * 
     * @param id 库存地点映射ID
     * @return 结果
     */
    public int deleteCxStockLocationMappingById(Long id);

    /**
     * 批量删除库存地点映射
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxStockLocationMappingByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxStockLocationMapping> list);
}
