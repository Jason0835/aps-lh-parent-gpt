package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixFinalRubberStock;

/**
 * 终炼胶库存Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixFinalRubberStockMapper 
{
    /**
     * 查询终炼胶库存
     * 
     * @param id 终炼胶库存ID
     * @return 终炼胶库存
     */
    public MixFinalRubberStock selectMixFinalRubberStockById(Long id);

    /**
     * 查询终炼胶库存列表
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 终炼胶库存集合
     */
    public List<MixFinalRubberStock> selectMixFinalRubberStockList(MixFinalRubberStock mixFinalRubberStock);

    /**
     * 新增终炼胶库存
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 结果
     */
    public int insertMixFinalRubberStock(MixFinalRubberStock mixFinalRubberStock);

    /**
     * 修改终炼胶库存
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 结果
     */
    public int updateMixFinalRubberStock(MixFinalRubberStock mixFinalRubberStock);

    /**
     * 删除终炼胶库存
     * 
     * @param id 终炼胶库存ID
     * @return 结果
     */
    public int deleteMixFinalRubberStockById(Long id);

    /**
     * 批量删除终炼胶库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixFinalRubberStockByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixFinalRubberStock> list);
}
