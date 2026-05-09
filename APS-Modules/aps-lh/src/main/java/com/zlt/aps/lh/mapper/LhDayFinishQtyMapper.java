package com.zlt.aps.lh.mapper;

import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 硫化排程日完成量Mapper
 *
 * @author APS Team
 * @since 2026/04/13
 */
@Mapper
public interface LhDayFinishQtyMapper extends CommBaseMapper<LhDayFinishQty> {

    /**
     * 根据唯一键查询已存在的数据
     *
     * @param list 唯一键列表
     * @return 已存在的数据
     */
    List<LhDayFinishQty> selectByUniqueKeyList(@Param("list") List<LhDayFinishQty> list);

    /**
     * 根据分厂编号逻辑删除硫化排程日完成量数据
     *
     * @param factoryCode 分厂编号
     * @param updateBy    更新者
     * @param updateTime  更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_LH_DAY_FINISH_QTY SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE FACTORY_CODE = #{factoryCode} AND IS_DELETE = 0")
    int logicDeleteByFactoryCode(@Param("factoryCode") String factoryCode, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

}
