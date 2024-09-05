package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixReturnRubberStock;

/**
 * 返回胶库存Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixReturnRubberStockMapper 
{
    /**
     * 查询返回胶库存
     * 
     * @param id 返回胶库存ID
     * @return 返回胶库存
     */
    public MixReturnRubberStock selectMixReturnRubberStockById(Long id);

    /**
     * 查询返回胶库存列表
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 返回胶库存集合
     */
    public List<MixReturnRubberStock> selectMixReturnRubberStockList(MixReturnRubberStock mixReturnRubberStock);

    /**
     * 新增返回胶库存
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 结果
     */
    public int insertMixReturnRubberStock(MixReturnRubberStock mixReturnRubberStock);

    /**
     * 修改返回胶库存
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 结果
     */
    public int updateMixReturnRubberStock(MixReturnRubberStock mixReturnRubberStock);

    /**
     * 删除返回胶库存
     * 
     * @param id 返回胶库存ID
     * @return 结果
     */
    public int deleteMixReturnRubberStockById(Long id);

    /**
     * 批量删除返回胶库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixReturnRubberStockByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixReturnRubberStock> list);
}
