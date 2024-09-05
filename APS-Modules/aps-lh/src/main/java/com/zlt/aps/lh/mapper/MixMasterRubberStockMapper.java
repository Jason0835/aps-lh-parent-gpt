package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMasterRubberStock;

/**
 * 母炼胶库存Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMasterRubberStockMapper 
{
    /**
     * 查询母炼胶库存
     * 
     * @param id 母炼胶库存ID
     * @return 母炼胶库存
     */
    public MixMasterRubberStock selectMixMasterRubberStockById(Long id);

    /**
     * 查询母炼胶库存列表
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 母炼胶库存集合
     */
    public List<MixMasterRubberStock> selectMixMasterRubberStockList(MixMasterRubberStock mixMasterRubberStock);

    /**
     * 新增母炼胶库存
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 结果
     */
    public int insertMixMasterRubberStock(MixMasterRubberStock mixMasterRubberStock);

    /**
     * 修改母炼胶库存
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 结果
     */
    public int updateMixMasterRubberStock(MixMasterRubberStock mixMasterRubberStock);

    /**
     * 删除母炼胶库存
     * 
     * @param id 母炼胶库存ID
     * @return 结果
     */
    public int deleteMixMasterRubberStockById(Long id);

    /**
     * 批量删除母炼胶库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixMasterRubberStockByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixMasterRubberStock> list);
}
