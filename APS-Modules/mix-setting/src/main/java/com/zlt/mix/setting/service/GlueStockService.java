package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.dto.GlueStockDto;
import com.zlt.mix.setting.api.domain.entity.GlueStock;

import java.util.List;

/**
 * 终炼胶库存信息Service接口
 *
 * @author Gim
 * @date 2022-03-18
 */
public interface GlueStockService extends IService<GlueStock> {
    /**
     * 查询库存信息列表
     *
     * @param glueStock 库存信息
     * @return 库存信息集合
     */
    List<GlueStockDto> selectGlueStockList(GlueStock glueStock);

    /**
     * 保存库存信息信息（id为空则新增，id不为空则修改）
     *
     * @param glueStock
     */
    void saveGlueStock(GlueStock glueStock);

    /**
     * 批量删除库存信息
     *
     * @param ids 需要删除的库存信息ID
     * @return 结果
     */
    int deleteGlueStockByIds(Long[] ids);

    /**
     * 校验库存信息唯一性
     */
    String checkGlueStockUnique(GlueStock glueStock);

    /**
     * 导入库存信息数据
     */
    AjaxResult importData(List<GlueStock> list, boolean updateSupport, Long importLogId);
}
