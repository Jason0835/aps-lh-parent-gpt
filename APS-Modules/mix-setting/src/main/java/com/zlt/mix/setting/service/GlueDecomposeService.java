package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;

import java.util.List;

/**
 * 终炼母炼分解Service接口
 *
 * @author Liam
 * @date 2022-03-28
 */
public interface GlueDecomposeService extends IService<GlueDecompose> {
    /**
     * 查询终炼母炼分解列表
     *
     * @param glueDecompose 终炼母炼分解
     * @return 终炼母炼分解集合
     */
    List<GlueDecompose> selectGlueDecomposeList(GlueDecompose glueDecompose);

    /**
     * 保存终炼母炼分解信息（id为空则新增，id不为空则修改）
     *
     * @param glueDecompose
     */
    void saveGlueDecompose(GlueDecompose glueDecompose);

    /**
     * 批量删除终炼母炼分解
     *
     * @param ids 需要删除的终炼母炼分解ID
     * @return 结果
     */
    int deleteGlueDecomposeByIds(Long[] ids);

    /**
     * 校验终炼母炼分解唯一性
     */
    String checkGlueDecomposeUnique(GlueDecompose glueDecompose);

    /**
     * 导入终炼母炼分解数据
     */
    AjaxResult importData(List<GlueDecompose> list, boolean updateSupport, Long importLogId);
}
