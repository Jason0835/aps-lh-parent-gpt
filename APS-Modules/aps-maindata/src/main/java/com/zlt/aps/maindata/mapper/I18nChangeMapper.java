package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.I18nChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface I18nChangeMapper {

    /**
     * 查询列表
     */
    List<I18nChange> selectRelList(I18nChange i18nChange);

    /**
     * 更新
     */
    void update(I18nChange change);

    /**
     * 根据relId+changeKey查询已存在数据库的数据
     *
     * @param list 唯一键列表
     * @return 结果
     */
    List<I18nChange> selectByUniqueKey(@Param("list") List<I18nChange> list);

    /**
     * 根据relId+changeKey批量更新
     *
     * @param list 列表
     * @return 影响行数
     */
    int batchUpdateByRelIdAndKey(@Param("list") List<I18nChange> list);

    /**
     * 创建临时表
     *
     * @return 影响行数
     */
    int createTempTable();

    /**
     * 删除临时表
     *
     * @return 影响行数
     */
    int dropTempTable();

    /**
     * 批量插入临时表
     *
     * @param i18nChangeList 列表
     * @return 影响行数
     */
    int insertTempTable(@Param("list") List<I18nChange> i18nChangeList);

    /**
     * 批量插入
     *
     * @return 影响行数
     */
    int batchInsertByRelIdAndKey();
}
