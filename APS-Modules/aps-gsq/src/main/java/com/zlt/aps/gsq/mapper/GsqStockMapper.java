package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈库存管理Mapper接口
 *
 * @author zlt
 * @date 2026-07-08
 */
@Mapper
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
     * 根据库存日期字符串（yyyy-MM-dd）逻辑删除钢丝圈库存数据
     * 只删除指定库存日期的数据，历史数据保留
     * 使用字符串日期比较，规避JVM时区与SQL Server时区不一致导致的Date参数偏移
     *
     * @param stockDateStr 库存日期字符串，格式yyyy-MM-dd
     * @param updateBy     更新者
     * @param updateTime   更新时间
     * @return 更新的记录数
     */
    int logicDeleteByStockDate(@Param("stockDateStr") String stockDateStr,
                               @Param("updateBy") String updateBy,
                               @Param("updateTime") Date updateTime);

    /**
     * MES库存同步专用批量插入
     * 显式指定CREATE_BY/UPDATE_BY等值，绕过MyBatis-Plus MetaObjectHandler自动填充，
     * 确保Feign调用上下文（syncUser）下审计字段仍为"MES"
     *
     * @param list 待插入的钢丝圈库存列表
     */
    void batchInsertMesStock(@Param("list") List<GsqStock> list);
}
