package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixReturnRubberStock;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 返回胶库存Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixReturnRubberStockService
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
    @Transactional
    public int insertMixReturnRubberStock(MixReturnRubberStock mixReturnRubberStock);

    /**
     * 修改返回胶库存
     * 
     * @param mixReturnRubberStock 返回胶库存
     * @return 结果
     */
    @Transactional
    public int updateMixReturnRubberStock(MixReturnRubberStock mixReturnRubberStock);

    /**
     * 批量删除返回胶库存
     * 
     * @param ids 需要删除的返回胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixReturnRubberStockByIds(Long[] ids);

    /**
     * 删除返回胶库存信息
     * 
     * @param id 返回胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixReturnRubberStockById(Long id);

    /**
     * 校验返回胶库存唯一性
     */
    public String checkMixReturnRubberStockUnique(MixReturnRubberStock mixReturnRubberStock);

    /**
     * 导入返回胶库存数据
     */
    @Transactional
    public AjaxResult importData(List<MixReturnRubberStock> list, boolean updateSupport, Long importLogId);
}
