package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈库存管理Mapper接口
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface GsqStockMapper extends BaseMapper<GsqStock> {

    /**
     * 校验"库存日期+钢丝圈代码"组合是否已存在
     *
     * @param entity 实体
     * @return 已存在数量（0表示唯一，>0表示不唯一）
     */
    int checkUnique(GsqStock entity);

    /**
     * 批量合并保存（存在则更新，否则新增），用于导入场景
     *
     * @param list 待保存数据集合
     */
    void mergeSql(List<GsqStock> list);

    /**
     * 根据库存日期逻辑删除钢丝圈库存数据
     *
     * @param stockDate  库存日期
     * @param updateBy   更新者
     * @param updateTime 更新时间
     * @return 更新的记录数
     */
    @Update("UPDATE T_GSQ_STOCK SET IS_DELETE = 1, UPDATE_BY = #{updateBy}, UPDATE_TIME = #{updateTime} WHERE DATE(STOCK_DATE) = #{stockDate} AND IS_DELETE = 0")
    int logicDeleteByStockDate(@Param("stockDate") Date stockDate, @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);
}
