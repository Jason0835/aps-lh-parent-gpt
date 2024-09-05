package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;

import java.util.List;

/**
 * 成型排程中夜班完成量Mapper接口
 *
 * @author chen
 * @date 2022-02-25
 */
public interface MidNightShiftFinishMapper {
    /**
     * 查询成型排程中夜班完成量
     *
     * @param id 成型排程中夜班完成量ID
     * @return 成型排程中夜班完成量
     */
    public MidNightShiftFinish selectMidNightShiftFinishById(Long id);

    /**
     * 查询成型排程中夜班完成量列表
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 成型排程中夜班完成量集合
     */
    public List<MidNightShiftFinish> selectMidNightShiftFinishList(MidNightShiftFinish midNightShiftFinish);

    /**
     * 新增成型排程中夜班完成量
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 结果
     */
    public int insertMidNightShiftFinish(MidNightShiftFinish midNightShiftFinish);

    /**
     * 修改成型排程中夜班完成量
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 结果
     */
    public int updateMidNightShiftFinish(MidNightShiftFinish midNightShiftFinish);

    /**
     * 删除成型排程中夜班完成量
     *
     * @param id 成型排程中夜班完成量ID
     * @return 结果
     */
    public int deleteMidNightShiftFinishById(Long id);

    /**
     * 批量删除成型排程中夜班完成量
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMidNightShiftFinishByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<MidNightShiftFinish> list);
}
