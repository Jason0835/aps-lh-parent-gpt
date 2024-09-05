package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;

import java.util.List;

/**
 * 硫化机台当前生产规格Mapper接口
 *
 * @author chen
 * @date 2022-03-23
 */
public interface LhInProductionSpecMapper {
    /**
     * 查询硫化机台当前生产规格
     *
     * @param id 硫化机台当前生产规格ID
     * @return 硫化机台当前生产规格
     */
    public LhInProductionSpec selectLhInProductionSpecById(Long id);

    /**
     * 查询硫化机台当前生产规格列表
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 硫化机台当前生产规格集合
     */
    public List<LhInProductionSpec> selectLhInProductionSpecList(LhInProductionSpec lhInProductionSpec);

    /**
     * 新增硫化机台当前生产规格
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 结果
     */
    public int insertLhInProductionSpec(LhInProductionSpec lhInProductionSpec);

    /**
     * 修改硫化机台当前生产规格
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 结果
     */
    public int updateLhInProductionSpec(LhInProductionSpec lhInProductionSpec);

    /**
     * 删除硫化机台当前生产规格
     *
     * @param id 硫化机台当前生产规格ID
     * @return 结果
     */
    public int deleteLhInProductionSpecById(Long id);

    /**
     * 批量删除硫化机台当前生产规格
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhInProductionSpecByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<LhInProductionSpec> list);
}
