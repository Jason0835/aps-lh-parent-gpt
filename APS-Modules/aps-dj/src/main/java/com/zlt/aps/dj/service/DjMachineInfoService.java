package com.zlt.aps.dj.service;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;

/**
 * 垫胶机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface DjMachineInfoService {
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
     * @param ids 需要删除的垫胶机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(DjMachineInfo machineInfo);

    /**
     * 根据垫胶和口型板获取对应机台信息
     *
     * @param machineInfo 垫胶机台信息
     * @return 垫胶机台信息集合
     */
    public List<DjMachineInfo> selectMachineInfoList2(DjMachineInfo machineInfo);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<DjMachineInfo> list, boolean updateSupport, Long importLogId);
}
