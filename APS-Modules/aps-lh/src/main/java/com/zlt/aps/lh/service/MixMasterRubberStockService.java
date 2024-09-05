package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMasterRubberStock;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 母炼胶库存Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMasterRubberStockService
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
    @Transactional
    public int insertMixMasterRubberStock(MixMasterRubberStock mixMasterRubberStock);

    /**
     * 修改母炼胶库存
     * 
     * @param mixMasterRubberStock 母炼胶库存
     * @return 结果
     */
    @Transactional
    public int updateMixMasterRubberStock(MixMasterRubberStock mixMasterRubberStock);

    /**
     * 批量删除母炼胶库存
     * 
     * @param ids 需要删除的母炼胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMasterRubberStockByIds(Long[] ids);

    /**
     * 删除母炼胶库存信息
     * 
     * @param id 母炼胶库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMasterRubberStockById(Long id);

    /**
     * 校验母炼胶库存唯一性
     */
    public String checkMixMasterRubberStockUnique(MixMasterRubberStock mixMasterRubberStock);

    /**
     * 导入母炼胶库存数据
     */
    @Transactional
    public AjaxResult importData(List<MixMasterRubberStock> list, boolean updateSupport, Long importLogId);
}
