package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMasterMaterialStock;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 母炼胶小料库存Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMasterMaterialStockService
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
    @Transactional
    public int insertMixMasterMaterialStock(MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 修改母炼胶小料库存
     * 
     * @param mixMasterMaterialStock 母炼胶小料库存
     * @return 结果
     */
    @Transactional
    public int updateMixMasterMaterialStock(MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 批量删除母炼胶小料库存
     * 
     * @param ids 需要删除的母炼胶小料库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMasterMaterialStockByIds(Long[] ids);

    /**
     * 删除母炼胶小料库存信息
     * 
     * @param id 母炼胶小料库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMasterMaterialStockById(Long id);

    /**
     * 校验母炼胶小料库存唯一性
     */
    public String checkMixMasterMaterialStockUnique(MixMasterMaterialStock mixMasterMaterialStock);

    /**
     * 导入母炼胶小料库存数据
     */
    @Transactional
    public AjaxResult importData(List<MixMasterMaterialStock> list, boolean updateSupport, Long importLogId);
}
