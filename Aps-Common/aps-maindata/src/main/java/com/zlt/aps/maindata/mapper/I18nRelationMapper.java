package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.I18nRelation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface I18nRelationMapper {

    /**
     * 查询列表
     */
    List<I18nRelation> selectList(I18nRelation i18nRelation);
}
