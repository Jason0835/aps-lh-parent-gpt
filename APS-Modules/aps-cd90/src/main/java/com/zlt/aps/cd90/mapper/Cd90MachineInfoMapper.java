package com.zlt.aps.cd90.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;

import java.util.List;

/**
 * 90°裁断机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface Cd90MachineInfoMapper {
    /**
     * 查询90°裁断机台信息
     *
     * @param id 90°裁断机台信息ID
     * @return 90°裁断机台信息
     */
    public Cd90MachineInfo selectMachineInfoById(Long id);

    /**
     * 查询90°裁断机台信息列表
     *
     * @param machineInfo 90°裁断机台信息
     * @return 90°裁断机台信息集合
     */
    public List<Cd90MachineInfo> selectMachineInfoList(Cd90MachineInfo machineInfo);

    /**
     * 新增90°裁断机台信息
     *
     * @param machineInfo 90°裁断机台信息
     * @return 结果
     */
    public int insertMachineInfo(Cd90MachineInfo machineInfo);

    /**
     * 修改90°裁断机台信息
     *
     * @param machineInfo 90°裁断机台信息
     * @return 结果
     */
    public int updateMachineInfo(Cd90MachineInfo machineInfo);

    /**
     * 批量删除90°裁断机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验90°裁断机台唯一性
     */
    public List<Cd90MachineInfo> checkMachineCodeUnique(Cd90MachineInfo machineInfo);
    public List<Cd90MachineInfo> checkMachineNameUnique(Cd90MachineInfo machineInfo);

    public List<Cd90MachineInfo> selectMachineInfoList2(Cd90MachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90MachineInfo> list);
}
