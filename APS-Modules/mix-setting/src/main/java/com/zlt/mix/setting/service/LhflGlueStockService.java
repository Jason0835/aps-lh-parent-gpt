package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.LhflGlueStock;

import java.util.List;

/**
 * 硫磺辅料终炼库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
public interface LhflGlueStockService extends IService<LhflGlueStock> {
    /**
     * 查询硫磺辅料终炼库存信息列表
     *
     * @param lhflGlueStock 硫磺辅料终炼库存信息
     * @return 硫磺辅料终炼库存信息集合
     */
    List<LhflGlueStock> selectLhflGlueStockList(LhflGlueStock lhflGlueStock);

    /**
     * 保存硫磺辅料终炼库存信息信息（id为空则新增，id不为空则修改）
     *
     * @param lhflGlueStock
     */
    void saveLhflGlueStock(LhflGlueStock lhflGlueStock);

    /**
     * 批量删除硫磺辅料终炼库存信息
     *
     * @param ids 需要删除的硫磺辅料终炼库存信息ID
     * @return 结果
     */
    int deleteLhflGlueStockByIds(Long[] ids);

    /**
     * 校验硫磺辅料终炼库存信息唯一性
     */
    String checkLhflGlueStockUnique(LhflGlueStock lhflGlueStock);

    /**
     * 导入硫磺辅料终炼库存信息数据
     */
    AjaxResult importData(List<LhflGlueStock> list, boolean updateSupport, Long importLogId);
}
