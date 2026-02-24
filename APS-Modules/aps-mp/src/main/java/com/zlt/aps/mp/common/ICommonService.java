package com.zlt.aps.mp.common;


import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.constant.Constant;
import com.zlt.aps.mp.common.utils.PubUtil;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 档案业务接口
 *
 * @param <T>
 * @author Chad
 * 2021年7月16日13:53:16
 */
public interface ICommonService<T extends BaseEntity> {

    /***
     * sql会话工厂
     * @return
     */
    SqlSessionFactory getSqlSessionFactory();

    /**
     * 指定查询Maper接口
     *
     * @return
     */
    ICommonMapper<T> getMapper();

    /***
     * 映射实际mapper名称
     * @return
     */
    Class<? extends ICommonMapper<T>> getMapperClazz();

    /**
     * 按主鍵刪除數據
     *
     * @param id
     * @return
     */
    default int deleteByPrimaryKey(Long id) {
        T entity = this.getMapper().selectByPrimaryKey(id);
        if (PubUtil.isEmpty(entity)) {
            return 0;
        }

        entity.setIsDelete(Constant.TRUE);
        entity.setUpdateTime(new Date());
        entity.setUpdateBy(SecurityUtils.getUsername());
        return getMapper().updateByPrimaryKey(entity);
    }

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    default int insert(T entity) {
        if (PubUtil.isEmpty(entity)) {
            return 0;
        }

        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(new Date());
        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(new Date());
        return getMapper().insertSelective(entity);
    }

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    default int insertSelective(T entity) {
        if (PubUtil.isEmpty(entity)) {
            return 0;
        }

        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(new Date());
        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(new Date());

        return getMapper().insertSelective(entity);
    }

    /**
     * 按主鍵查詢
     */
    default T selectByPrimaryKey(Long id) {
        return getMapper().selectByPrimaryKey(id);
    }

    /**
     * 按主鍵更新
     *
     * @param entity
     * @return
     */
    default int updateByPrimaryKeySelective(T entity) {
        if (PubUtil.isEmpty(entity)) {
            return 0;
        }

        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(new Date());
        return getMapper().updateByPrimaryKeySelective(entity);
    }

    /**
     * 按主鍵更新
     *
     * @param entity
     * @return
     */
    default int updateByPrimaryKey(T entity) {
        if (PubUtil.isEmpty(entity)) {
            return 0;
        }

        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(new Date());
        return getMapper().updateByPrimaryKey(entity);
    }

    /**
     * 批量更新
     *
     * @param list
     * @return
     */
    default int updateBatch(List<T> list) {
        if (PubUtil.isEmpty(list)) {
            return 0;
        }

        list.forEach(entity -> {
            entity.setUpdateBy(SecurityUtils.getUsername());
            entity.setUpdateTime(new Date());
        });
        return getMapper().updateBatch(list);
    }

    /**
     * 批量更新
     *
     * @param list
     * @return
     */
    default int updateBatchSelective(List<T> list) {
        if (PubUtil.isEmpty(list)) {
            return 0;
        }

        list.forEach(entity -> {
            entity.setUpdateBy(SecurityUtils.getUsername());
            entity.setUpdateTime(new Date());
        });
        return getMapper().updateBatchSelective(list);
    }

    /**
     * 按分公司編號+分厂编号+年度+月份查询数据
     *
     * @param companyCode
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    default List<T> queryData(String companyCode, String factoryCode, Integer year, Integer month) {
        return null;
    }


    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    default Integer deleteBatch(List<Long> ids) {
        if (PubUtil.isEmpty(ids)) {
            return 0;
        }

        List<T> entitys = getMapper().selectByIds(ids);
        entitys.forEach(entity -> {
            entity.setUpdateBy(SecurityUtils.getUsername());
            entity.setIsDelete(Constant.TRUE);
            entity.setUpdateTime(new Date());
        });

        return getMapper().updateBatch(entitys);
    }

//    /**
//     * 批量删除
//     *
//     * @param ids
//     * @return
//     */
//    default Integer deleteByIds(Long[] ids) {
//        return getMapper().deleteByIds(ids);
//    }

    /**
     * 批量保存数据
     *
     * @param entities
     * @return
     */
    default Integer saveBatch(Collection<? extends T> entities) {
        if (PubUtil.isEmpty(entities)) {
            return 0;
        }

        List<T> insertList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();
        entities.forEach(entity -> {
            if (PubUtil.isEmpty(entity.getId())) {
                insertList.add(entity);
            } else {
                updateList.add(entity);
            }
        });

        if (PubUtil.isNotEmpty(updateList)) {
            this.updateBatchSelective(updateList);
        }

        if (PubUtil.isNotEmpty(insertList)) {
            this.insertBatchData(insertList);
        }

        return entities.size();
    }


    default void insertBatchData(Collection<? extends T> dataList) {

        //取最后一个记录，做判断使用
        int size = dataList.size();
        if (size == 0) {
            return;
        }

        try (SqlSession sqlSession = getSqlSessionFactory().openSession(ExecutorType.BATCH)) {

            ICommonMapper<T> userMapper = sqlSession.getMapper(getMapperClazz());
            int index = 0, endIndex = 1000;
            int inputCount = 0, recordCount = 0;
            for (T oneRecord : dataList) {
                recordCount += 1;
                index = index + 1;

                oneRecord.setCreateBy(SecurityUtils.getUsername());
                oneRecord.setCreateTime(new Date());
                oneRecord.setUpdateBy(SecurityUtils.getUsername());
                oneRecord.setUpdateTime(new Date());

                userMapper.insertSelective(oneRecord);
                //达到一批数量的时候，提交记录,继续循环
                if (index == endIndex) {
                    sqlSession.flushStatements();
                    inputCount += index;
                    index = 0;
                    continue;
                }

                //没有剩余记录的时候，提交记录
                if (recordCount == size) {
                    sqlSession.flushStatements();
                    inputCount += index;
                    break;
                }
            }

            sqlSession.commit();
//            log.debug("InsertBatch data:rows:{}", inputCount);
        } catch (Throwable ex) {
//            log.error("InsertBatch data:fail", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

}
