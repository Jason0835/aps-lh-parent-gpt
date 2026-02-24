package com.zlt.aps.mp.common;


import com.ruoyi.common.core.web.domain.BaseEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 档案通用Maper(系统生成）
 *
 * @param <T>
 */
public interface ICommonMapper<T extends BaseEntity> {
    /**
     * delete by primary key
     *
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(Long id);

    /**
     * insert record to table
     *
     * @param record the record
     * @return insert count
     */
    int insert(T record);

    /**
     * insert record to table selective
     *
     * @param record the record
     * @return insert count
     */
    int insertSelective(T record);

    /**
     * 批量更新
     *
     * @param list
     * @return
     */
    int insertBatch(List<T> list);

    /**
     * select by primary key
     *
     * @param id primary key
     * @return object by primary key
     */
    T selectByPrimaryKey(Long id);

    /**
     * update record selective
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(T record);

    /**
     * update record
     *
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKey(T record);

    /**
     * 批量更新
     *
     * @param list
     * @return
     */
    int updateBatch(List<T> list);

    /**
     * 批量更新
     *
     * @param list
     * @return
     */
    int updateBatchSelective(List<T> list);

    /**
     * 按指定IDS批量查询数据
     *
     * @param ids
     * @return
     */
    List<T> selectByIds(@Param("ids") List<Long> ids);

}
