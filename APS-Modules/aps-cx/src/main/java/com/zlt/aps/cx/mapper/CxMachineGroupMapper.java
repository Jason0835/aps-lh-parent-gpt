package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxMachineGroup;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupForExcel;
import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;

import java.util.List;

/**
 * 成型机组Mapper接口
 *
 * @author zlt
 * @date 2021-12-16
 */
public interface CxMachineGroupMapper {
    /**
     * 查询成型机组
     *
     * @param id 成型机组ID
     * @return 成型机组
     */
    public CxMachineGroup selectCxMachineGroupById(Long id);

    /**
     * 查询成型机组列表
     *
     * @param cxMachineGroup 成型机组
     * @return 成型机组集合
     */
    public List<CxMachineGroup> selectCxMachineGroupList(CxMachineGroup cxMachineGroup);

    public List<CxMachineGroupForExcel> selectCxMachineGroup4Excel(CxMachineGroup cxMachineGroup);


    public List<CxMachineGroup> checkCxMachineGroupUnique(CxMachineGroup cxMachineGroup);


    /**
     * 新增成型机组
     *
     * @param cxMachineGroup 成型机组
     * @return 结果
     */
    public int insertCxMachineGroup(CxMachineGroup cxMachineGroup);

    /**
     * 修改成型机组
     *
     * @param cxMachineGroup 成型机组
     * @return 结果
     */
    public int updateCxMachineGroup(CxMachineGroup cxMachineGroup);

    /**
     * 删除成型机组
     *
     * @param id 成型机组ID
     * @return 结果
     */
    public int deleteCxMachineGroupById(Long id);

    /**
     * 批量删除成型机组
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxMachineGroupByIds(Long[] ids);

    /**
     * 批量删除组别机台列
     *
     * @param customerIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxMachineGroupListByGroupIds(Long[] ids);

    /**
     * 批量新增组别机台列
     *
     * @param cxMachineGroupListList 组别机台列列表
     * @return 结果
     */
    public int batchCxMachineGroupList(List<CxMachineGroupList> cxMachineGroupListList);

    /**
     * 通过成型机组ID删除组别机台列信息
     *
     * @param roleId 角色ID
     * @return 结果
     */
    public int deleteCxMachineGroupListByGroupId(Long id);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxMachineGroup> list);
}
