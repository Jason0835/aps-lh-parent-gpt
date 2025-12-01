package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.FhGlueStock;

import java.util.List;

/**
 * 返回胶库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-12
 */
public interface FhGlueStockService extends IService<FhGlueStock> {
    /**
     * 查询返回胶库存信息列表
     *
     * @param fhGlueStock 返回胶库存信息
     * @return 返回胶库存信息集合
     */
    List<FhGlueStock> selectFhGlueStockList(FhGlueStock fhGlueStock);

    /**
     * 保存返回胶库存信息信息（id为空则新增，id不为空则修改）
     *
     * @param fhGlueStock
     */
    void saveFhGlueStock(FhGlueStock fhGlueStock);

    /**
     * 批量删除返回胶库存信息
     *
     * @param ids 需要删除的返回胶库存信息ID
     * @return 结果
     */
    int deleteFhGlueStockByIds(Long[] ids);

    /**
     * 校验返回胶库存信息唯一性
     */
    String checkFhGlueStockUnique(FhGlueStock fhGlueStock);

    /**
     * 导入返回胶库存信息数据
     */
    AjaxResult importData(List<FhGlueStock> list, boolean updateSupport, Long importLogId);
}
