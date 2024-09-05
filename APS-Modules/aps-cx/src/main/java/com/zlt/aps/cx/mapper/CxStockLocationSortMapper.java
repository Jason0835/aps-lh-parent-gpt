package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxStockLocationSortDto;
import com.zlt.aps.cx.entity.CxStockLocationSort;

import java.util.List;

/**
 * 库存地点生产顺序Mapper接口
 *
 * @author chen
 * @date 2021-07-22
 */
public interface CxStockLocationSortMapper extends BaseMapper<CxStockLocationSort> {
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
     * 删除库存地点生产顺序
     *
     * @param id 库存地点生产顺序ID
     * @return 结果
     */
    public int deleteCxStockLocationSortById(Long id);

    /**
     * 批量删除库存地点生产顺序
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxStockLocationSortByIds(Long[] ids);

    /**
     * 校验成型损耗率设定记录唯一性
     *
     * @param cxStockLocationSort 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkUnique(CxStockLocationSort cxStockLocationSort);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxStockLocationSort> list);

}
