package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMachineRecipeInfo;

/**
 * 机台和配方对应及下车重量Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMachineRecipeInfoMapper 
{
    /**
     * 查询机台和配方对应及下车重量
     * 
     * @param id 机台和配方对应及下车重量ID
     * @return 机台和配方对应及下车重量
     */
    public MixMachineRecipeInfo selectMixMachineRecipeInfoById(Long id);

    /**
     * 查询机台和配方对应及下车重量列表
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 机台和配方对应及下车重量集合
     */
    public List<MixMachineRecipeInfo> selectMixMachineRecipeInfoList(MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 新增机台和配方对应及下车重量
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 结果
     */
    public int insertMixMachineRecipeInfo(MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 修改机台和配方对应及下车重量
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 结果
     */
    public int updateMixMachineRecipeInfo(MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 删除机台和配方对应及下车重量
     * 
     * @param id 机台和配方对应及下车重量ID
     * @return 结果
     */
    public int deleteMixMachineRecipeInfoById(Long id);

    /**
     * 批量删除机台和配方对应及下车重量
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixMachineRecipeInfoByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixMachineRecipeInfo> list);
}
