package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixRecipeInfo;

/**
 * 密炼配方信息Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixRecipeInfoMapper 
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
    public int insertMixRecipeInfo(MixRecipeInfo mixRecipeInfo);

    /**
     * 修改密炼配方信息
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 结果
     */
    public int updateMixRecipeInfo(MixRecipeInfo mixRecipeInfo);

    /**
     * 删除密炼配方信息
     * 
     * @param id 密炼配方信息ID
     * @return 结果
     */
    public int deleteMixRecipeInfoById(Long id);

    /**
     * 批量删除密炼配方信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixRecipeInfoByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixRecipeInfo> list);
}
