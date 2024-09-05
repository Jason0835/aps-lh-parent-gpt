package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;

import java.util.List;

/**
 * 硫化定点机台信息Mapper接口
 *
 * @author zlt
 * @date 2021-07-21
 */
public interface LhSpecifyMachineMapper {
    /**
     * 查询硫化定点机台信息
     *
     * @param id 硫化定点机台信息ID
     * @return 硫化定点机台信息
     */
    public LhSpecifyMachine selectLhSpecifyMachineById(Long id);

    /**
     * 查询硫化定点机台信息列表
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 硫化定点机台信息集合
     */
    public List<LhSpecifyMachine> selectLhSpecifyMachineList(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 唯一性校验
     */
    public List<LhSpecifyMachine> checkLhSpecifyMachineUnique(LhSpecifyMachine lhSpecifyMachine);


    /**
     * 新增硫化定点机台信息
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 结果
     */
    public int insertLhSpecifyMachine(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 修改硫化定点机台信息
     *
     * @param lhSpecifyMachine 硫化定点机台信息
     * @return 结果
     */
    public int updateLhSpecifyMachine(LhSpecifyMachine lhSpecifyMachine);

    /**
     * 删除硫化定点机台信息
     *
     * @param id 硫化定点机台信息ID
     * @return 结果
     */
    public int deleteLhSpecifyMachineById(Long id);

    /**
     * 批量删除硫化定点机台信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhSpecifyMachineByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<LhSpecifyMachine> list);

}
