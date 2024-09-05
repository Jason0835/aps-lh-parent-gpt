package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxStockLocationSortDto;
import com.zlt.aps.cx.entity.CxStockLocationSort;

import java.util.List;

/**
 * 库存地点生产顺序Service接口
 *
 * @author chen
 * @date 2021-07-22
 */
public interface CxStockLocationSortService extends IService<CxStockLocationSort> {
    /**
     * 查询库存地点生产顺序
     *
     * @param id 库存地点生产顺序ID
     * @return 库存地点生产顺序
     */
    public CxStockLocationSortDto selectCxStockLocationSortById(Long id);

    /**
     * 查询库存地点生产顺序列表
     *
     * @param cxStockLocationSort 库存地点生产顺序
     * @return 库存地点生产顺序集合
     */
    public List<CxStockLocationSortDto> selectCxStockLocationSortList(CxStockLocationSort cxStockLocationSort);

    /**
     * 新增库存地点生产顺序
     *
     * @param cxStockLocationSort 库存地点生产顺序
     * @return 结果
     */
    public int insertCxStockLocationSort(CxStockLocationSort cxStockLocationSort);

    /**
     * 修改库存地点生产顺序
     *
     * @param cxStockLocationSort 库存地点生产顺序
     * @return 结果
     */
    public int updateCxStockLocationSort(CxStockLocationSort cxStockLocationSort);

    /**
     * 批量删除库存地点生产顺序
     *
     * @param ids 需要删除的库存地点生产顺序ID
     * @return 结果
     */
    public int deleteCxStockLocationSortByIds(Long[] ids);

    /**
     * 删除库存地点生产顺序信息
     *
     * @param id 库存地点生产顺序ID
     * @return 结果
     */
    public int deleteCxStockLocationSortById(Long id);

    /**
     * 校验库存地点生产顺序唯一性
     */
    public String checkCxStockLocationSortUnique(CxStockLocationSort cxStockLocationSort);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxStockLocationSortDto> list, boolean updateSupport, Long importLogId);
}
