package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.InProductionSpec;

import java.util.List;

/**
 * 成型机台当前生产规格Mapper接口
 *
 * @author chen
 * @date 2022-02-25
 */
public interface InProductionSpecMapper {
    /**
     * 查询成型机台当前生产规格
     *
     * @param id 成型机台当前生产规格ID
     * @return 成型机台当前生产规格
     */
    public InProductionSpec selectInProductionSpecById(Long id);

    /**
     * 查询成型机台当前生产规格列表
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 成型机台当前生产规格集合
     */
    public List<InProductionSpec> selectInProductionSpecList(InProductionSpec inProductionSpec);

    /**
     * 新增成型机台当前生产规格
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 结果
     */
    public int insertInProductionSpec(InProductionSpec inProductionSpec);

    /**
     * 修改成型机台当前生产规格
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 结果
     */
    public int updateInProductionSpec(InProductionSpec inProductionSpec);

    /**
     * 删除成型机台当前生产规格
     *
     * @param id 成型机台当前生产规格ID
     * @return 结果
     */
    public int deleteInProductionSpecById(Long id);

    /**
     * 批量删除成型机台当前生产规格
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteInProductionSpecByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<InProductionSpec> list);
}
