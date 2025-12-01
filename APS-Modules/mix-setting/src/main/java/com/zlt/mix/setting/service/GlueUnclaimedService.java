package com.zlt.mix.setting.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimed;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 胶料白班待支领Service接口
 * 
 * @author zlt
 * @date 2022-09-05
 */
public interface GlueUnclaimedService  extends IService<GlueUnclaimed>
{
    /**
     * 查询胶料白班待支领列表
     * 
     * @param glueUnclaimed 胶料白班待支领
     * @return 胶料白班待支领集合
     */
    List<GlueUnclaimed> selectGlueUnclaimedList(GlueUnclaimed glueUnclaimed);

    /**
     * 保存胶料白班待支领信息（id为空则新增，id不为空则修改）
     *
     * @param glueUnclaimed
     */
    void saveGlueUnclaimed(GlueUnclaimed glueUnclaimed);

    /**
     * 批量删除胶料白班待支领
     * 
     * @param ids 需要删除的胶料白班待支领ID
     * @return 结果
     */
    int deleteGlueUnclaimedByIds(Long[] ids);

    /**
     * 校验胶料白班待支领唯一性
     */
    String checkGlueUnclaimedUnique(GlueUnclaimed glueUnclaimed);

    /**
     * 导入胶料白班待支领数据
     */
    AjaxResult importData(List<GlueUnclaimed> list, boolean updateSupport, Long importLogId);
}
