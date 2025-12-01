package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.BhgGlueStock;

import java.util.List;

/**
 * 不合格胶库存信息Service接口
 *
 * @author Liam
 * @date 2022-04-12
 */
public interface BhgGlueStockService extends IService<BhgGlueStock> {
    /**
     * 查询不合格胶库存信息列表
     *
     * @param bhgGlueStock 不合格胶库存信息
     * @return 不合格胶库存信息集合
     */
    List<BhgGlueStock> selectBhgGlueStockList(BhgGlueStock bhgGlueStock);

    /**
     * 保存不合格胶库存信息信息（id为空则新增，id不为空则修改）
     *
     * @param bhgGlueStock
     */
    void saveBhgGlueStock(BhgGlueStock bhgGlueStock);

    /**
     * 批量删除不合格胶库存信息
     *
     * @param ids 需要删除的不合格胶库存信息ID
     * @return 结果
     */
    int deleteBhgGlueStockByIds(Long[] ids);

    /**
     * 校验不合格胶库存信息唯一性
     */
    String checkBhgGlueStockUnique(BhgGlueStock bhgGlueStock);

    /**
     * 导入不合格胶库存信息数据
     */
    AjaxResult importData(List<BhgGlueStock> list, boolean updateSupport, Long importLogId);
}
