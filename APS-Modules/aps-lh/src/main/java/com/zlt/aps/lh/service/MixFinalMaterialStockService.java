package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixFinalMaterialStock;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 终炼小料库存Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixFinalMaterialStockService
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
    @Transactional
    public int insertMixFinalMaterialStock(MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 修改终炼小料库存
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 结果
     */
    @Transactional
    public int updateMixFinalMaterialStock(MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 批量删除终炼小料库存
     * 
     * @param ids 需要删除的终炼小料库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixFinalMaterialStockByIds(Long[] ids);

    /**
     * 删除终炼小料库存信息
     * 
     * @param id 终炼小料库存ID
     * @return 结果
     */
    @Transactional
    public int deleteMixFinalMaterialStockById(Long id);

    /**
     * 校验终炼小料库存唯一性
     */
    public String checkMixFinalMaterialStockUnique(MixFinalMaterialStock mixFinalMaterialStock);

    /**
     * 导入终炼小料库存数据
     */
    @Transactional
    public AjaxResult importData(List<MixFinalMaterialStock> list, boolean updateSupport, Long importLogId);
}
