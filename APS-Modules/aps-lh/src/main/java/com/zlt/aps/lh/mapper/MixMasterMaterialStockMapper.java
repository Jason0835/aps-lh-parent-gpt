package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMasterMaterialStock;

/**
 * 母炼胶小料库存Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMasterMaterialStockMapper 
{
    /**
     * 查询母炼胶小料库存
     * 
     * @param id 母炼胶小料库存ID
     * @return 母炼胶小料库存
     */
    public MixMasterMaterialStock selectMixMasterMaterialStockById(Long id);

    /**
     * 查询母炼胶小料库存列表
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 母炼胶小料库存集合
     */
    public List<MixMasterMaterialStock> selectMixMasterMaterialStockList(MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 新增母炼胶小料库存
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 结果
     */
    public int insertMixMasterMaterialStock(MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 修改母炼胶小料库存
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 结果
     */
    public int updateMixMasterMaterialStock(MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 删除母炼胶小料库存
     * 
     * @param id 母炼胶小料库存ID
     * @return 结果
     */
    public int deleteMixMasterMaterialStockById(Long id);

    /**
     * 批量删除母炼胶小料库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixMasterMaterialStockByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixMasterMaterialStock> list);
}
