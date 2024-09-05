package com.zlt.aps.cx.mapper;

import java.util.List;
import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;

/**
 * 成型收尾范围系数Mapper接口
 * 
 * @author zlt
 * @date 2021-12-28
 */
public interface CxCloseOutRangeMapper 
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

    public List<CxCloseOutRange> checkCxCloseOutRangeUnique(CxCloseOutRange cxCloseOutRange);


    /**
     * 新增成型收尾范围系数
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 结果
     */
    public int insertCxCloseOutRange(CxCloseOutRange cxCloseOutRange);

    /**
     * 修改成型收尾范围系数
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 结果
     */
    public int updateCxCloseOutRange(CxCloseOutRange cxCloseOutRange);

    /**
     * 删除成型收尾范围系数
     * 
     * @param id 成型收尾范围系数ID
     * @return 结果
     */
    public int deleteCxCloseOutRangeById(Long id);

    /**
     * 批量删除成型收尾范围系数
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxCloseOutRangeByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<CxCloseOutRange> list);
}
