package com.zlt.aps.xwyy.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyMachineInfo;

import java.util.List;

/**
 * 纤维压延机台信息Service接口
 *
 * @author zlt
 * @date 2021-05-28
 */
public interface XwyyMachineInfoService {
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
     * @param ids 需要删除的纤维压延机台信息ID
     * @return 结果
     */
    public int deleteMachineInfoByIds(Long[] ids);

    /**
     * 校验机台编号唯一性
     */
    public String checkMachineCodeUnique(XwyyMachineInfo machineInfo);

    /**
     * 查询帘布大卷和机台映射信息
     */
    public List<XwyyMachineInfo> listMachineInfo(XwyyMachineInfo machineInfo);

    /**
     * 导入数据
     */
    AjaxResult importData(List<XwyyMachineInfo> list, boolean updateSupport, Long importLogId);
}
