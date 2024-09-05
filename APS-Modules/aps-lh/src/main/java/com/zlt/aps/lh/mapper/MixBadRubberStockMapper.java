package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixBadRubberStock;

/**
 * 不合格胶库存Mapper接口
 * 
 * @author zlt
 * @date 2021-11-08
 */
public interface MixBadRubberStockMapper 
{
    /**
     * 查询不合格胶库存
     * 
     * @param id 不合格胶库存ID
     * @return 不合格胶库存
     */
    public MixBadRubberStock selectMixBadRubberStockById(Long id);

    /**
     * 查询不合格胶库存列表
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 不合格胶库存集合
     */
    public List<MixBadRubberStock> selectMixBadRubberStockList(MixBadRubberStock mixBadRubberStock);

    /**
     * 新增不合格胶库存
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 结果
     */
    public int insertMixBadRubberStock(MixBadRubberStock mixBadRubberStock);

    /**
     * 修改不合格胶库存
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 结果
     */
    public int updateMixBadRubberStock(MixBadRubberStock mixBadRubberStock);

    /**
     * 删除不合格胶库存
     * 
     * @param id 不合格胶库存ID
     * @return 结果
     */
    public int deleteMixBadRubberStockById(Long id);

    /**
     * 批量删除不合格胶库存
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixBadRubberStockByIds(Long[] ids);

    /**
 * 合并操作，如果记录存在则更新，否则新增
 */
    public void mergeSql(List<MixBadRubberStock> list);
}
