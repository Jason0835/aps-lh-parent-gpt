package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxStockLocationMapping;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 库存地点映射Service接口
 * 
 * @author zlt
 * @date 2021-11-15
 */
public interface CxStockLocationMappingService
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

    /**
     * 新增库存地点映射
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 结果
     */
    @Transactional
    public int insertCxStockLocationMapping(CxStockLocationMapping cxStockLocationMapping);

    /**
     * 修改库存地点映射
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 结果
     */
    @Transactional
    public int updateCxStockLocationMapping(CxStockLocationMapping cxStockLocationMapping);

    /**
     * 批量删除库存地点映射
     * 
     * @param ids 需要删除的库存地点映射ID
     * @return 结果
     */
    @Transactional
    public int deleteCxStockLocationMappingByIds(Long[] ids);

    /**
     * 删除库存地点映射信息
     * 
     * @param id 库存地点映射ID
     * @return 结果
     */
    @Transactional
    public int deleteCxStockLocationMappingById(Long id);

    /**
     * 校验库存地点映射唯一性
     */
    public String checkCxStockLocationMappingUnique(CxStockLocationMapping cxStockLocationMapping);

    /**
     * 导入库存地点映射数据
     */
    @Transactional
    public AjaxResult importData(List<CxStockLocationMapping> list, boolean updateSupport, Long importLogId);
}
