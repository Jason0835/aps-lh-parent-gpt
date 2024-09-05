package com.zlt.aps.cx.service;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 成型收尾范围系数Service接口
 * 
 * @author zlt
 * @date 2021-12-28
 */
public interface CxCloseOutRangeService
{
    /**
     * 查询成型收尾范围系数
     * 
     * @param id 成型收尾范围系数ID
     * @return 成型收尾范围系数
     */
    public CxCloseOutRange selectCxCloseOutRangeById(Long id);

    /**
     * 查询成型收尾范围系数列表
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 成型收尾范围系数集合
     */
    public List<CxCloseOutRange> selectCxCloseOutRangeList(CxCloseOutRange cxCloseOutRange);

    /**
     * 新增成型收尾范围系数
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 结果
     */
    @Transactional
    public int insertCxCloseOutRange(CxCloseOutRange cxCloseOutRange);

    /**
     * 修改成型收尾范围系数
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 结果
     */
    @Transactional
    public int updateCxCloseOutRange(CxCloseOutRange cxCloseOutRange);

    /**
     * 批量删除成型收尾范围系数
     * 
     * @param ids 需要删除的成型收尾范围系数ID
     * @return 结果
     */
    @Transactional
    public int deleteCxCloseOutRangeByIds(Long[] ids);

    /**
     * 删除成型收尾范围系数信息
     * 
     * @param id 成型收尾范围系数ID
     * @return 结果
     */
    @Transactional
    public int deleteCxCloseOutRangeById(Long id);

    /**
     * 校验成型收尾范围系数唯一性
     */
    public String checkCxCloseOutRangeUnique(CxCloseOutRange cxCloseOutRange);

    /**
     * 导入成型收尾范围系数数据
     */
    @Transactional
    public AjaxResult importData(List<CxCloseOutRange> list, boolean updateSupport, Long importLogId);
}
