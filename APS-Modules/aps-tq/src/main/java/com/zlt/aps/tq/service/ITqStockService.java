package com.zlt.aps.tq.service;

import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

public interface ITqStockService extends IDocService<TqStock> {

    String checkUnique(TqStock entity);

    /**
     * 逻辑删除并批量保存胎圈库存（事务性操作）
     * 步骤1：根据库存日期字符串逻辑删除当天旧数据（IS_DELETE置为1）
     * 步骤2：在目标JVM时区解析日期字符串，回填到每条记录的stockDate字段
     * 步骤3：强制回填createBy/updateBy，使用XML批量插入绕过MetaObjectHandler，确保审计字段为"MES"而非syncUser
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param stockDateStr 库存日期字符串，格式yyyy-MM-dd
     * @param createBy     创建者/更新者（同步场景固定为"MES"）
     * @param list         待插入的胎圈库存列表（stockDate未设置，由本方法统一回填）
     */
    void logicDeleteAndSaveBatch(String stockDateStr, String createBy, List<TqStock> list);
}
