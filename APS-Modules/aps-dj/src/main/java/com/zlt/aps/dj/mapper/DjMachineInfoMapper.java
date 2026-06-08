package com.zlt.aps.dj.mapper;

import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;

/**
 * 垫胶机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface DjMachineInfoMapper {
    /**
     * 查询垫胶机台信息
     *
     * @param id 垫胶机台信息ID
     * @return 垫胶机台信息
     */
    public DjMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询垫胶机台信息列表
     *
     * @param machineInfo 垫胶机台信息
     * @return 垫胶机台信息集合
     */
    public List<DjMachineInfo> selectMachineInfoList(DjMachineInfo machineInfo);

    /**
     * 新增垫胶机台信息
     *
     * @param machineInfo 垫胶机台信息
     * @return 结果
     */
    public int insertMachineInfo(DjMachineInfo machineInfo);

    /**
     * 修改垫胶机台信息
     *
     * @param machineInfo 垫胶机台信息
     * @return 结果
     */
    public int updateMachineInfo(DjMachineInfo machineInfo);

    /**
     * 批量删除垫胶机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验垫胶机台唯一性
     */
    public List<DjMachineInfo> checkMachineCodeUnique(DjMachineInfo machineInfo);
    public List<DjMachineInfo> checkMachineNameUnique(DjMachineInfo machineInfo);


    /**
     * 根据垫胶和口型板获取对应机台信息
     *
     * @param machineInfo 垫胶机台信息
     * @return 垫胶机台信息集合
     */
    public List<DjMachineInfo> selectMachineInfoList2(DjMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<DjMachineInfo> list);
}
