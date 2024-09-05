package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;

import java.util.List;

/**
 * 硫化机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface LhMachineInfoMapper {
    /**
     * 查询硫化机台信息
     *
     * @param id 硫化机台信息ID
     * @return 硫化机台信息
     */
    public LhMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询硫化机台信息列表
     *
     * @param machineInfo 硫化机台信息
     * @return 硫化机台信息集合
     */
    public List<LhMachineInfo> selectMachineInfoList(LhMachineInfo machineInfo);

    /**
     * 新增硫化机台信息
     *
     * @param machineInfo 硫化机台信息
     * @return 结果
     */
    public int insertMachineInfo(LhMachineInfo machineInfo);

    /**
     * 修改硫化机台信息
     *
     * @param machineInfo 硫化机台信息
     * @return 结果
     */
    public int updateMachineInfo(LhMachineInfo machineInfo);

    /**
     * 批量删除硫化机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验硫化机台唯一性
     */
    public List<LhMachineInfo> checkMachineCodeUnique(LhMachineInfo machineInfo);
    public List<LhMachineInfo> checkMachineNameUnique(LhMachineInfo machineInfo);


    /**
     * 根据条件查询机台信息
     *
     * @param machineInfo 查询条件
     * @return 结果
     */
    List<LhMachineInfo> listMachineInfo(LhMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<LhMachineInfo> list);

}
