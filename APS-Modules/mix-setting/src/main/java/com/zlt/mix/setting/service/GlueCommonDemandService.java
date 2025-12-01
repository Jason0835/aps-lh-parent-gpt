package com.zlt.mix.setting.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;

/**
 * 密炼机常用大规格设置Service接口
 * 
 * @author zlt
 * @date 2023-02-05
 */
public interface GlueCommonDemandService  extends IService<GlueCommonDemand>
{
    /**
     * 查询密炼机常用大规格设置列表
     * 
     * @param glueCommonDemand 密炼机常用大规格设置
     * @return 密炼机常用大规格设置集合
     */
    List<GlueCommonDemand> selectGlueCommonDemandList(GlueCommonDemand glueCommonDemand);

    /**
     * 保存密炼机常用大规格设置信息（id为空则新增，id不为空则修改）
     *
     * @param glueCommonDemand
     */
    void saveGlueCommonDemand(GlueCommonDemand glueCommonDemand);

    /**
     * 批量删除密炼机常用大规格设置
     * 
     * @param ids 需要删除的密炼机常用大规格设置ID
     * @return 结果
     */
    int deleteGlueCommonDemandByIds(Long[] ids);

    /**
     * 校验密炼机常用大规格设置唯一性
     */
    String checkGlueCommonDemandUnique(GlueCommonDemand glueCommonDemand);

    /**
     * 导入密炼机常用大规格设置数据
     */
    AjaxResult importData(List<GlueCommonDemand> list, boolean updateSupport, Long importLogId);
}
