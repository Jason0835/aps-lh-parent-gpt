package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 钢丝圈库存管理Service接口（本地）
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface IGsqStockService extends IDocService<GsqStock> {

    /**
     * 校验"库存日期+钢丝圈代码"组合唯一性
     *
     * @param entity 实体
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqStock entity);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqStock> list, boolean updateSupport, Long importLogId);

    /**
     * 逻辑删除指定库存日期的旧数据并批量插入新数据（事务性操作）
     * 日期字符串在目标JVM时区解析，使用XML批量插入绕过MetaObjectHandler确保CREATE_BY/UPDATE_BY正确
     *
     * @param stockDateStr 库存日期字符串，格式yyyy-MM-dd
     * @param createBy     创建者/更新者（MES同步场景固定为"MES"）
     * @param list         待插入的钢丝圈库存列表（stockDate/createBy等字段由本方法统一回填）
     */
    void logicDeleteAndSaveBatch(String stockDateStr, String createBy, List<GsqStock> list);
}
