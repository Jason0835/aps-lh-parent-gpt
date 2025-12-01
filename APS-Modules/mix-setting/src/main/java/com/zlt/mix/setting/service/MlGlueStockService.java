package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.dto.MlGlueStockDto;
import com.zlt.mix.setting.api.domain.entity.MlGlueStock;

import java.util.List;

/**
 * 母炼库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-12
 */
public interface MlGlueStockService extends IService<MlGlueStock> {
    /**
     * 查询母炼库存信息列表
     *
     * @param mlGlueStock 母炼库存信息
     * @return 母炼库存信息集合
     */
    List<MlGlueStockDto> selectMlGlueStockList(MlGlueStock mlGlueStock);

    /**
     * 保存母炼库存信息信息（id为空则新增，id不为空则修改）
     *
     * @param mlGlueStock
     */
    void saveMlGlueStock(MlGlueStock mlGlueStock);

    /**
     * 批量删除母炼库存信息
     *
     * @param ids 需要删除的母炼库存信息ID
     * @return 结果
     */
    int deleteMlGlueStockByIds(Long[] ids);

    /**
     * 校验母炼库存信息唯一性
     */
    String checkMlGlueStockUnique(MlGlueStock mlGlueStock);

    /**
     * 导入母炼库存信息数据
     */
    AjaxResult importData(List<MlGlueStock> list, boolean updateSupport, Long importLogId);
}
