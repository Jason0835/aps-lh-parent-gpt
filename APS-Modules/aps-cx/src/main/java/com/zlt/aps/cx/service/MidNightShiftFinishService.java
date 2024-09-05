package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型排程中夜班完成量Service接口
 *
 * @author chen
 * @date 2022-02-25
 */
public interface MidNightShiftFinishService {
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
    @Transactional
    public int insertMidNightShiftFinish(MidNightShiftFinish midNightShiftFinish);

    /**
     * 修改成型排程中夜班完成量
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 结果
     */
    @Transactional
    public int updateMidNightShiftFinish(MidNightShiftFinish midNightShiftFinish);

    /**
     * 批量删除成型排程中夜班完成量
     *
     * @param ids 需要删除的成型排程中夜班完成量ID
     * @return 结果
     */
    @Transactional
    public int deleteMidNightShiftFinishByIds(Long[] ids);

    /**
     * 删除成型排程中夜班完成量信息
     *
     * @param id 成型排程中夜班完成量ID
     * @return 结果
     */
    @Transactional
    public int deleteMidNightShiftFinishById(Long id);

    /**
     * 校验成型排程中夜班完成量唯一性
     */
    public String checkMidNightShiftFinishUnique(MidNightShiftFinish midNightShiftFinish);

    /**
     * 导入成型排程中夜班完成量数据
     */
    @Transactional
    public AjaxResult importData(List<MidNightShiftFinish> list, boolean updateSupport, Long importLogId);
}
