package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMachineRecipeInfo;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 机台和配方对应及下车重量Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMachineRecipeInfoService
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
    @Transactional
    public int insertMixMachineRecipeInfo(MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 修改机台和配方对应及下车重量
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 结果
     */
    @Transactional
    public int updateMixMachineRecipeInfo(MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 批量删除机台和配方对应及下车重量
     * 
     * @param ids 需要删除的机台和配方对应及下车重量ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMachineRecipeInfoByIds(Long[] ids);

    /**
     * 删除机台和配方对应及下车重量信息
     * 
     * @param id 机台和配方对应及下车重量ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMachineRecipeInfoById(Long id);

    /**
     * 校验机台和配方对应及下车重量唯一性
     */
    public String checkMixMachineRecipeInfoUnique(MixMachineRecipeInfo mixMachineRecipeInfo);

    /**
     * 导入机台和配方对应及下车重量数据
     */
    @Transactional
    public AjaxResult importData(List<MixMachineRecipeInfo> list, boolean updateSupport, Long importLogId);
}
