package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixFinalRubberStock;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 终炼胶库存Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixFinalRubberStockService
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
    @Transactional
    public int insertMixFinalRubberStock(MixFinalRubberStock mixFinalRubberStock);

    /**
     * 修改终炼胶库存
     * 
     * @param mixFinalRubberStock 终炼胶库存
     * @return 结果
     */
    @Transactional
    public int updateMixFinalRubberStock(MixFinalRubberStock mixFinalRubberStock);

    /**
     * 批量删除终炼胶库存
     * 
     * @param ids 需要删除的终炼胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixFinalRubberStockByIds(Long[] ids);

    /**
     * 删除终炼胶库存信息
     * 
     * @param id 终炼胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixFinalRubberStockById(Long id);

    /**
     * 校验终炼胶库存唯一性
     */
    public String checkMixFinalRubberStockUnique(MixFinalRubberStock mixFinalRubberStock);

    /**
     * 导入终炼胶库存数据
     */
    @Transactional
    public AjaxResult importData(List<MixFinalRubberStock> list, boolean updateSupport, Long importLogId);
}
