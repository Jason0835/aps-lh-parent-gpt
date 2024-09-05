package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixRecipeInfo;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 密炼配方信息Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixRecipeInfoService
{
    /**
     * 查询密炼配方信息
     * 
     * @param id 密炼配方信息ID
     * @return 密炼配方信息
     */
    public MixRecipeInfo selectMixRecipeInfoById(Long id);

    /**
     * 查询密炼配方信息列表
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 密炼配方信息集合
     */
    public List<MixRecipeInfo> selectMixRecipeInfoList(MixRecipeInfo mixRecipeInfo);

    /**
     * 新增密炼配方信息
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 结果
     */
    @Transactional
    public int insertMixRecipeInfo(MixRecipeInfo mixRecipeInfo);

    /**
     * 修改密炼配方信息
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 结果
     */
    @Transactional
    public int updateMixRecipeInfo(MixRecipeInfo mixRecipeInfo);

    /**
     * 批量删除密炼配方信息
     * 
     * @param ids 需要删除的密炼配方信息ID
     * @return 结果
     */
    @Transactional
    public int deleteMixRecipeInfoByIds(Long[] ids);

    /**
     * 删除密炼配方信息信息
     * 
     * @param id 密炼配方信息ID
     * @return 结果
     */
    @Transactional
    public int deleteMixRecipeInfoById(Long id);

    /**
     * 校验密炼配方信息唯一性
     */
    public String checkMixRecipeInfoUnique(MixRecipeInfo mixRecipeInfo);

    /**
     * 导入密炼配方信息数据
     */
    @Transactional
    public AjaxResult importData(List<MixRecipeInfo> list, boolean updateSupport, Long importLogId);
}
