package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.GlueFinish;

import java.util.List;

/**
 * 胶料完成量Service接口
 *
 * @author Gim
 * @date 2022-03-29
 */
public interface GlueFinishService  extends IService<GlueFinish>
{
    /**
     * 查询炼胶时间信息列表
     * 
     * @param glueFinish 炼胶时间信息
     * @return 炼胶时间信息集合
     */
    List<GlueFinish> selectGlueFinishList(GlueFinish glueFinish);

    /**
     * 保存炼胶时间信息信息（id为空则新增，id不为空则修改）
     *
     * @param glueFinish
     */
    void saveGlueFinish(GlueFinish glueFinish);

    /**
     * 校验炼胶时间信息唯一性
     */
    String checkGlueFinishUnique(GlueFinish glueFinish);

    /**
     * 导入炼胶时间信息数据
     */
    AjaxResult importData(List<GlueFinish> list, boolean updateSupport, Long importLogId);
}
