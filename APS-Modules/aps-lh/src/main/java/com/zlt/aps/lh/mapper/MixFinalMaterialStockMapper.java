package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixFinalMaterialStock;

/**
 * 终炼小料库存Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixFinalMaterialStockMapper 
{
    /**
     * 查询终炼小料库存
     * 
     * @param id 终炼小料库存ID
     * @return 终炼小料库存
     */
    public MixFinalMaterialStock selectMixFinalMaterialStockById(Long id);

    /**
     * 查询终炼小料库存列表
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 终炼小料库存集合
     */
    public List<MixFinalMaterialStock> selectMixFinalMaterialStockList(MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 新增终炼小料库存
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 结果
     */
    public int insertMixFinalMaterialStock(MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 修改终炼小料库存
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 结果
     */
    public int updateMixFinalMaterialStock(MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 删除终炼小料库存
     * 
     * @param id 终炼小料库存ID
     * @return 结果
     */
    public int deleteMixFinalMaterialStockById(Long id);

    /**
     * 批量删除终炼小料库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixFinalMaterialStockByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixFinalMaterialStock> list);
}
