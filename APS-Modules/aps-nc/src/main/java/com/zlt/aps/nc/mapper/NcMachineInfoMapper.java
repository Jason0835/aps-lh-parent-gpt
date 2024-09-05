package com.zlt.aps.nc.mapper;

import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;

import java.util.List;

/**
 * 内衬机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface NcMachineInfoMapper {
    /**
     * 查询内衬机台信息
     *
     * @param id 内衬机台信息ID
     * @return 内衬机台信息
     */
    public NcMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询内衬机台信息列表
     *
     * @param machineInfo 内衬机台信息
     * @return 内衬机台信息集合
     */
    public List<NcMachineInfo> selectMachineInfoList(NcMachineInfo machineInfo);

    /**
     * 新增内衬机台信息
     *
     * @param machineInfo 内衬机台信息
     * @return 结果
     */
    public int insertMachineInfo(NcMachineInfo machineInfo);

    /**
     * 修改内衬机台信息
     *
     * @param machineInfo 内衬机台信息
     * @return 结果
     */
    public int updateMachineInfo(NcMachineInfo machineInfo);

    /**
     * 批量删除内衬机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验内衬机台唯一性
     */
    public List<NcMachineInfo> checkMachineCodeUnique(NcMachineInfo machineInfo);
    public List<NcMachineInfo> checkMachineNameUnique(NcMachineInfo machineInfo);


    /**
     * 根据内衬和口型板获取对应机台信息
     *
     * @param machineInfo 内衬机台信息
     * @return 内衬机台信息集合
     */
    public List<NcMachineInfo> selectMachineInfoList2(NcMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcMachineInfo> list);
}
