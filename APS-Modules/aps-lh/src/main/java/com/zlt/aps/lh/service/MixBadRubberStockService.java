package com.zlt.aps.lh.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.MixBadRubberStock;
import org.springframework.transaction.annotation.Transactional;

/**
 * 不合格胶库存Service接口
 * 
 * @author zlt
 * @date 2021-11-08
 */
public interface MixBadRubberStockService
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
    @Transactional
    public int insertMixBadRubberStock(MixBadRubberStock mixBadRubberStock);

    /**
     * 修改不合格胶库存
     * 
     * @param mixBadRubberStock 不合格胶库存
     * @return 结果
     */
    @Transactional
    public int updateMixBadRubberStock(MixBadRubberStock mixBadRubberStock);

    /**
     * 批量删除不合格胶库存
     * 
     * @param ids 需要删除的不合格胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixBadRubberStockByIds(Long[] ids);

    /**
     * 删除不合格胶库存信息
     * 
     * @param id 不合格胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixBadRubberStockById(Long id);

    /**
     * 校验不合格胶库存唯一性
     */
    public String checkMixBadRubberStockUnique(MixBadRubberStock mixBadRubberStock);

    /**
     * 导入不合格胶库存数据
     */
    @Transactional
    public AjaxResult importData(List<MixBadRubberStock> list, boolean updateSupport, Long importLogId);
}
