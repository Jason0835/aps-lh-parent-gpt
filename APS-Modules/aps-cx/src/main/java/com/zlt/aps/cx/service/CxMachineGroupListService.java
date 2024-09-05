package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 组别机台列Service接口
 * 
 * @author zlt
 * @date 2021-12-16
 */
public interface CxMachineGroupListService
{
    /**
     * 查询组别机台列
     * 
     * @param id 组别机台列ID
     * @return 组别机台列
     */
    public CxMachineGroupList selectCxMachineGroupListById(Long id);

    /**
     * 查询组别机台列列表
     * 
     * @param cxMachineGroupList 组别机台列
     * @return 组别机台列集合
     */
    public List<CxMachineGroupList> selectCxMachineGroupListList(CxMachineGroupList cxMachineGroupList);

    public List<CxMachineGroupList> selectCxMachineGroupListList4MachineName(CxMachineGroupList cxMachineGroupList);
    /**
     * 新增组别机台列
     * 
     * @param cxMachineGroupList 组别机台列
     * @return 结果
     */
    @Transactional
    public int insertCxMachineGroupList(CxMachineGroupList cxMachineGroupList);

    /**
     * 修改组别机台列
     * 
     * @param cxMachineGroupList 组别机台列
     * @return 结果
     */
    @Transactional
    public int updateCxMachineGroupList(CxMachineGroupList cxMachineGroupList);

    /**
     * 批量删除组别机台列
     * 
     * @param ids 需要删除的组别机台列ID
     * @return 结果
     */
    @Transactional
    public int deleteCxMachineGroupListByIds(Long[] ids);

    /**
     * 删除组别机台列信息
     * 
     * @param id 组别机台列ID
     * @return 结果
     */
    @Transactional
    public int deleteCxMachineGroupListById(Long id);

    /**
     * 校验组别机台列唯一性
     */
    public List<CxMachineGroupList> checkCxMachineGroupListUnique(CxMachineGroupList cxMachineGroupList);

    /**
     * 导入组别机台列数据
     */
    @Transactional
    public AjaxResult importData(List<CxMachineGroupList> list, boolean updateSupport, Long importLogId);
}
