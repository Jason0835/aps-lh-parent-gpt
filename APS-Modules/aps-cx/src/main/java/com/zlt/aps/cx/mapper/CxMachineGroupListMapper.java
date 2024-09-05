package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;

/**
 * 组别机台列Mapper接口
 * 
 * @author zlt
 * @date 2021-12-16
 */
public interface CxMachineGroupListMapper 
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

    public List<CxMachineGroupList> checkCxMachineGroupListUnique(CxMachineGroupList cxMachineGroupList);



    /**
     * 新增组别机台列
     * 
     * @param cxMachineGroupList 组别机台列
     * @return 结果
     */
    public int insertCxMachineGroupList(CxMachineGroupList cxMachineGroupList);

    /**
     * 修改组别机台列
     * 
     * @param cxMachineGroupList 组别机台列
     * @return 结果
     */
    public int updateCxMachineGroupList(CxMachineGroupList cxMachineGroupList);

    /**
     * 删除组别机台列
     * 
     * @param id 组别机台列ID
     * @return 结果
     */
    public int deleteCxMachineGroupListById(Long id);

    /**
     * 批量删除组别机台列
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxMachineGroupListByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxMachineGroupList> list);
}
