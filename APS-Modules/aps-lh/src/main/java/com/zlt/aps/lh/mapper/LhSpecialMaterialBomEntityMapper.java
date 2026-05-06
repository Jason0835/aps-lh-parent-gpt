package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 特殊物料清单配置Mapper接口
 *
 * @author zlt
 * @date 2026-05-06
 */
@Mapper
public interface LhSpecialMaterialBomEntityMapper extends BaseMapper<LhSpecialMaterialBom> {

    /**
     * 批量插入
     *
     * @param list 数据列表
     * @return 插入行数
     */
    int insertBatch(@Param("list") List<LhSpecialMaterialBom> list);

    /**
     * 批量更新
     *
     * @param list 数据列表
     * @return 更新行数
     */
    int updateBatch(@Param("list") List<LhSpecialMaterialBom> list);
}
