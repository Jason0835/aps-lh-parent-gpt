package com.zlt.aps.lh.service;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMachineInfo;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 密炼机台信息Service接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMachineInfoService
{
    /**
     * 查询密炼机台信息
     * 
     * @param id 密炼机台信息ID
     * @return 密炼机台信息
     */
    public MixMachineInfo selectMixMachineInfoById(Long id);

    /**
     * 查询密炼机台信息列表
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 密炼机台信息集合
     */
    public List<MixMachineInfo> selectMixMachineInfoList(MixMachineInfo mixMachineInfo);

    /**
     * 新增密炼机台信息
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 结果
     */
    @Transactional
    public int insertMixMachineInfo(MixMachineInfo mixMachineInfo);

    /**
     * 修改密炼机台信息
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 结果
     */
    @Transactional
    public int updateMixMachineInfo(MixMachineInfo mixMachineInfo);

    /**
     * 批量删除密炼机台信息
     * 
     * @param ids 需要删除的密炼机台信息ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMachineInfoByIds(Long[] ids);

    /**
     * 删除密炼机台信息信息
     * 
     * @param id 密炼机台信息ID
     * @return 结果
     */
    @Transactional
    public int deleteMixMachineInfoById(Long id);

    /**
     * 校验密炼机台信息唯一性
     */
    public String checkMixMachineInfoUnique(MixMachineInfo mixMachineInfo);

    /**
     * 导入密炼机台信息数据
     */
    @Transactional
    public AjaxResult importData(List<MixMachineInfo> list, boolean updateSupport, Long importLogId);
}
