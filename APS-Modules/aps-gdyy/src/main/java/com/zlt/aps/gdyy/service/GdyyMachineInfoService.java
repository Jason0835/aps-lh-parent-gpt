package com.zlt.aps.gdyy.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;

import java.util.List;

/**
 * 纤维压延机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface GdyyMachineInfoService {
    /**
     * 查询纤维压延机台信息
     *
     * @param id 纤维压延机台信息ID
     * @return 纤维压延机台信息
     */
    public GdyyMachineInfo selectMachineInfoById(Long id);

    /**
     * 查询纤维压延机台信息列表
     *
     * @param machineInfo 纤维压延机台信息
     * @return 纤维压延机台信息集合
     */
    public List<GdyyMachineInfo> selectMachineInfoList(GdyyMachineInfo machineInfo);

    /**
     * 新增纤维压延机台信息
     *
     * @param machineInfo 纤维压延机台信息
     * @return 结果
     */
    public int insertMachineInfo(GdyyMachineInfo machineInfo);

    /**
     * 修改纤维压延机台信息
     *
     * @param machineInfo 纤维压延机台信息
     * @return 结果
     */
    public int updateMachineInfo(GdyyMachineInfo machineInfo);

    /**
     * 批量删除纤维压延机台信息
     *
     * @param ids 需要删除的纤维压延机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(GdyyMachineInfo machineInfo);

    /**
     * 查询帘布大卷和机台映射信息
     */
    public List<GdyyMachineInfo> listMachineInfo(GdyyMachineInfo machineInfo);

    /**
     * 导入数据
     */
    AjaxResult importData(List<GdyyMachineInfo> list, boolean updateSupport, Long importLogId);
}
