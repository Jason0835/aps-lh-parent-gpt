package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;

import java.util.List;


/**
 * 成型计划投产状态Service接口
 *
 * @author zlt
 * @date 2021-07-21
 */
public interface CxPlanProductStatusService {
    /**
     * 查询成型计划投产状态
     *
     * @param id 成型计划投产状态ID
     * @return 成型计划投产状态
     */
    public CxPlanProductStatus selectCxPlanProductStatusById(Long id);

    /**
     * 查询成型计划投产状态
     *
     * @param id 成型计划投产状态ID
     * @return 成型计划投产状态
     */
    public CxPlanProductStatus selectCxPlanProductStatusByCxBatchNo(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 查询成型计划投产状态列表
     *
     * @param cxPlanProductStatus 成型计划投产状态
     * @return 成型计划投产状态集合
     */
    public List<CxPlanProductStatus> selectCxPlanProductStatusList(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 查询成型计划投产状态列表
     */
    public List<CxPlanProductStatus> seleteCxPlanProductStatusByIds(Long[] arr);

    /**
     * 新增成型计划投产状态
     *
     * @param cxPlanProductStatus 成型计划投产状态
     * @return 结果
     */
    public int insertCxPlanProductStatus(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 修改成型计划投产状态
     *
     * @param cxPlanProductStatus 成型计划投产状态
     * @return 结果
     */
    public int updateCxPlanProductStatus(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 批量删除成型计划投产状态
     *
     * @param ids 需要删除的成型计划投产状态ID
     * @return 结果
     */
    public int deleteCxPlanProductStatusByIds(Long[] ids);


    /**
     * 标记不投产
     */
    public int markUnProduct(Long[] ids);

    /**
     * 删除成型计划投产状态信息
     *
     * @param id 成型计划投产状态ID
     * @return 结果
     */
    public int deleteCxPlanProductStatusById(Long id);

    /**
     * 校验成型计划投产状态唯一性
     */
    public String checkCxPlanProductStatusUnique(CxPlanProductStatus cxPlanProductStatus);

    /**
     * 发布成功后将投产状态为待发布的变更为已投产
     *
     * @ClassName CxPlanProductStatusMapper1
     * @Description TODO
     * @Author Joran.Zhang
     * @Date 2021/8/4 9:16
     * @Version 1.0
     **/
    public int updateProductStatusToProduct(CxPlanProductStatus cxPlanProductStatus);
}
