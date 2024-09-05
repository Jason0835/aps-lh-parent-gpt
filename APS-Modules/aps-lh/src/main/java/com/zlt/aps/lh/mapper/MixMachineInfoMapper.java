package com.zlt.aps.lh.mapper;

import java.util.List;
import com.zlt.aps.lh.api.domain.entity.MixMachineInfo;

/**
 * 密炼机台信息Mapper接口
 * 
 * @author zlt
 * @date 2021-11-09
 */
public interface MixMachineInfoMapper 
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
    public int insertMixMachineInfo(MixMachineInfo mixMachineInfo);

    /**
     * 修改密炼机台信息
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 结果
     */
    public int updateMixMachineInfo(MixMachineInfo mixMachineInfo);

    /**
     * 删除密炼机台信息
     * 
     * @param id 密炼机台信息ID
     * @return 结果
     */
    public int deleteMixMachineInfoById(Long id);

    /**
     * 批量删除密炼机台信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMixMachineInfoByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<MixMachineInfo> list);
}
