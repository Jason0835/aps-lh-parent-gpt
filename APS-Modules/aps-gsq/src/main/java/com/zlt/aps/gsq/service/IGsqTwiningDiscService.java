package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.vo.GsqMesTwiningDiscSyncVO;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * 钢丝圈缠绕盘Service接口
 * <p>单表管理缠绕盘基础信息；规格关系与机台关系按编码关联、独立页面维护</p>
 *
 * @author zlt
 * @date 2026-07-08
 */
public interface IGsqTwiningDiscService extends IDocService<GsqTwiningDisc> {

    /**
     * 校验缠绕盘编码唯一性
     *
     * @param entity 实体
     * @return 唯一性结果（UserConstants.UNIQUE=唯一，UserConstants.NOT_UNIQUE=不唯一）
     */
    String checkUnique(GsqTwiningDisc entity);

    /**
     * 保存钢丝圈缠绕盘（单表保存，带编码唯一性校验）
     *
     * @param entity 实体
     * @return 操作结果
     */
    AjaxResult saveWithCheck(GsqTwiningDisc entity);

    /**
     * 删除钢丝圈缠绕盘（逻辑删除主表，并按缠绕盘编码级联逻辑删除规格关系及机台关系）
     *
     * @param ids 主表ID集合
     * @return 操作结果
     */
    AjaxResult removeMainAndRelation(List<Long> ids);

    /**
     * 获取主表反显公式（用于列表/导出反显）
     *
     * @return 反显公式数组
     */
    String[] getQueryFormulas();

    /**
     * 查询施工信息表全部钢丝圈选项（编码+名称，去重），供页面下拉选择使用
     *
     * @return 钢丝圈选项列表（key：BEAD_CODE 钢丝圈编号、BEAD_NAME 钢丝圈名称）
     */
    List<Map<String, Object>> listSteelRingOptions();

    /**
     * 导入数据，并保存记录
     * 校验规则：缠绕盘编码必填；按编码校验重复
     *
     * @param list          要导入的数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqTwiningDisc> list, boolean updateSupport, Long importLogId);

    /**
     * MES缠绕盘三表同步落库（事务性操作，供GsqMesSyncController远程调用）
     * <p>单事务处理缠绕盘清单/规格关系/机台关系，保证三表一致性：</p>
     * <p>1. 主表UPSERT：按缠绕盘编码分流，存在则仅更新MES字段（英寸/排列方式/状态/工厂/版本/来源，
     * 保留名称/数量/备注等手工维护字段），不存在则批量插入（名称默认取编码）；</p>
     * <p>2. 主表清理：APS中MES来源但MES最新清单已不存在的缠绕盘逻辑删除，并级联逻辑删除规格关系/机台关系；</p>
     * <p>3. 规格关系UPSERT：按缠绕盘编码+钢丝圈编号组合分流更新/插入（名称反显自施工信息表），
     * MES来源已失效的组合逻辑删除；</p>
     * <p>4. 机台关系UPSERT：按缠绕盘编码+机台编号组合分流更新/插入，MES来源已失效的组合逻辑删除</p>
     *
     * @param syncVO   MES三表聚合数据
     * @param updateBy 更新者（MES同步传"MES"）
     * @return 操作结果
     */
    AjaxResult syncFromMes(GsqMesTwiningDiscSyncVO syncVO, String updateBy);
}
