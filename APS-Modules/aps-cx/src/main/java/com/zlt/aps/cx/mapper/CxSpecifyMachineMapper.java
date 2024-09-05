package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.CxSpecifyMachine;

import java.util.List;

/**
 * 定点机台Mapper接口
 *
 * @author zlt
 * @date 2021-07-21
 */
public interface CxSpecifyMachineMapper {
    /**
     * 查询定点机台
     *
     * @param id 定点机台ID
     * @return 定点机台
     */
    public CxSpecifyMachine selectCxSpecifyMachine1ById(Long id);

    /**
     * 查询定点机台列表
     *
     * @param cxSpecifyMachine 定点机台
     * @return 定点机台集合
     */
    public List<CxSpecifyMachine> selectCxSpecifyMachine1List(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 唯一性校验
     */
    public List<CxSpecifyMachine> checkCxSpecifyMachine1Unique(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 新增定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    public int insertCxSpecifyMachine1(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 修改定点机台
     *
     * @param cxSpecifyMachine 定点机台
     * @return 结果
     */
    public int updateCxSpecifyMachine1(CxSpecifyMachine cxSpecifyMachine);

    /**
     * 删除定点机台
     *
     * @param id 定点机台ID
     * @return 结果
     */
    public int deleteCxSpecifyMachine1ById(Long id);

    /**
     * 批量删除定点机台
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxSpecifyMachine1ByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxSpecifyMachine> list);

}
