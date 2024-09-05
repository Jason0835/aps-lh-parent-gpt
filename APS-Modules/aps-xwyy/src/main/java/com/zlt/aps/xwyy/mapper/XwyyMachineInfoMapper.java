package com.zlt.aps.xwyy.mapper;


import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;

import java.util.List;

/**
 * 纤维压延机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface XwyyMachineInfoMapper {
    /**
     * 查询纤维压延机台信息
     *
     * @param id 纤维压延机台信息ID
     * @return 纤维压延机台信息
     */
    public XwyyMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询纤维压延机台信息列表
     *
     * @param machineInfo 纤维压延机台信息
     * @return 纤维压延机台信息集合
     */
    public List<XwyyMachineInfo> selectMachineInfoList(XwyyMachineInfo machineInfo);

    /**
     * 新增纤维压延机台信息
     *
     * @param machineInfo 纤维压延机台信息
     * @return 结果
     */
    public int insertMachineInfo(XwyyMachineInfo machineInfo);

    /**
     * 修改纤维压延机台信息
     *
     * @param machineInfo 纤维压延机台信息
     * @return 结果
     */
    public int updateMachineInfo(XwyyMachineInfo machineInfo);

    /**
     * 批量删除纤维压延机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验纤维压延机台唯一性
     */
    public List<XwyyMachineInfo> checkMachineCodeUnique(XwyyMachineInfo machineInfo);
    public List<XwyyMachineInfo> checkMachineNameUnique(XwyyMachineInfo machineInfo);

    /**
     * 查询帘布大卷和机台映射信息
     *
     * @param machineInfo 帘布大卷信息
     */
    public List<XwyyMachineInfo> listMachineInfo(XwyyMachineInfo machineInfo);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyMachineInfo> list);

}
