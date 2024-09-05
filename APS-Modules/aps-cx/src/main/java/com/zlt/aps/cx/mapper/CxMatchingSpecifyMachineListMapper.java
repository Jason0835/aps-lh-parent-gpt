package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;

import java.util.List;

/**
 * 定点机台配置列Mapper接口
 *
 * @author zlt
 * @date 2021-06-15
 */
public interface CxMatchingSpecifyMachineListMapper {
    /**
     * 查询定点机台配置列
     *
     * @param id 定点机台配置列ID
     * @return 定点机台配置列
     */
    public CxMatchingSpecifyMachineList selectCxSpecifyMachineListById(Long id);

    /**
     * 查询定点机台配置列列表
     *
     * @param CxMatchingSpecifyMachineList 定点机台配置列
     * @return 定点机台配置列集合
     */
    public List<CxMatchingSpecifyMachineList> selectCxSpecifyMachineListList(CxMatchingSpecifyMachineList CxMatchingSpecifyMachineList);

    /**
     * 新增定点机台配置列
     *
     * @param CxMatchingSpecifyMachineList 定点机台配置列
     * @return 结果
     */
    public int insertCxSpecifyMachineList(CxMatchingSpecifyMachineList CxMatchingSpecifyMachineList);

    /**
     * 修改定点机台配置列
     *
     * @param CxMatchingSpecifyMachineList 定点机台配置列
     * @return 结果
     */
    public int updateCxSpecifyMachineList(CxMatchingSpecifyMachineList CxMatchingSpecifyMachineList);

    /**
     * 删除定点机台配置列
     *
     * @param id 定点机台配置列ID
     * @return 结果
     */
    public int deleteCxSpecifyMachineListById(Long id);

    /**
     * 批量删除定点机台配置列
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxSpecifyMachineListByIds(Long[] ids);

    public List<CxMatchingSpecifyMachineList> viewList(CxMatchingSpecifyMachineList CxMatchingSpecifyMachineList);

    /**
     * 校验定点机台详情唯一性
     */
    public List<CxMatchingSpecifyMachineList> checkCxSpecifyMachineDetailUnic(CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxMatchingSpecifyMachineList> list);


}
