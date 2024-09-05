package com.zlt.aps.nc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;

import java.util.List;

/**
 * 内衬机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface NcMachineInfoService {
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
     * @param ids 需要删除的内衬机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(NcMachineInfo machineInfo);

    /**
     * 根据内衬和口型板获取对应机台信息
     *
     * @param machineInfo 内衬机台信息
     * @return 内衬机台信息集合
     */
    public List<NcMachineInfo> selectMachineInfoList2(NcMachineInfo machineInfo);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcMachineInfo> list, boolean updateSupport, Long importLogId);
}
