package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachine;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;

import java.util.List;

/**
 * 定点机台Mapper接口
 *
 * @author zlt
 * @date 2021-06-11
 */
public interface CxMatchingSpecifyMachineMapper {
    /**
     * 查询定点机台
     *
     * @param id 定点机台ID
     * @return 定点机台
     */
    public CxMatchingSpecifyMachine selectTSpecifyMachineById(Long id);

    /**
     * 查询定点机台列表
     *
     * @param CxSpecifyMachine 定点机台
     * @return 定点机台集合
     */
    public List<CxMatchingSpecifyMachine> selectTSpecifyMachineList(CxMatchingSpecifyMachine CxSpecifyMachine);

    /**
     * 新增定点机台
     *
     * @param CxSpecifyMachine 定点机台
     * @return 结果
     */
    public int insertTSpecifyMachine(CxMatchingSpecifyMachine CxSpecifyMachine);

    /**
     * 修改定点机台
     *
     * @param CxSpecifyMachine 定点机台
     * @return 结果
     */
    public int updateTSpecifyMachine(CxMatchingSpecifyMachine CxSpecifyMachine);

    /**
     * 删除定点机台
     *
     * @param id 定点机台ID
     * @return 结果
     */
    public int deleteTSpecifyMachineById(Long id);

    /**
     * 批量删除定点机台
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSpecifyMachineByIds(Long[] ids);

    /**
     * 批量删除定点机台配置列
     *
     * @param customerIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteTSpecifyMachineListBySpecifyMachineIds(Long[] ids);

    /**
     * 批量新增定点机台配置列
     *
     * @param tSpecifyMachineListList 定点机台配置列列表
     * @return 结果
     */
    public int batchTSpecifyMachineList(List<CxMatchingSpecifyMachineList> tSpecifyMachineListList);


    /**
     * 通过定点机台ID删除定点机台配置列信息
     *
     * @param roleId 角色ID
     * @return 结果
     */
    public int deleteTSpecifyMachineListBySpecifyMachineId(Long id);

    /**
     * 校验唯一性
     *
     * @param cxSpecifyMachine
     * @return
     */
    List<CxMatchingSpecifyMachine> checkCxSpecifyMachineUnic(CxMatchingSpecifyMachine cxSpecifyMachine);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxMatchingSpecifyMachine> list);

}
