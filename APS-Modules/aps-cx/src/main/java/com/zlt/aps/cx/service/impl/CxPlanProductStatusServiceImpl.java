package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import com.zlt.aps.cx.mapper.CxPlanProductStatusMapper1;
import com.zlt.aps.cx.service.CxPlanProductStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 成型计划投产状态Service业务层处理
 *
 * @author zlt
 * @date 2021-07-21
 */
@Service
public class CxPlanProductStatusServiceImpl implements CxPlanProductStatusService {
    @Autowired
    private CxPlanProductStatusMapper1 cxPlanProductStatusMapper;

    /**
     * 查询成型计划投产状态
     *
     * @param id 成型计划投产状态ID
     * @return 成型计划投产状态
     */
    @Override
    public CxPlanProductStatus selectCxPlanProductStatusById(Long id) {
        return cxPlanProductStatusMapper.selectCxPlanProductStatusById(id);
    }

    /**
     * 查询成型计划投产状态
     *
     * @param id 成型计划投产状态ID
     * @return 成型计划投产状态
     */
    @Override
    public CxPlanProductStatus selectCxPlanProductStatusByCxBatchNo(CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusMapper.selectCxPlanProductStatusByCxBatchNo(cxPlanProductStatus);
    }


    /**
     * 查询成型计划投产状态列表
     *
     * @param cxPlanProductStatus 成型计划投产状态
     * @return 成型计划投产状态
     */
    @Override
    public List<CxPlanProductStatus> selectCxPlanProductStatusList(CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusMapper.selectCxPlanProductStatusList(cxPlanProductStatus);
    }

    /**
     * 查询成型计划投产状态列表
     */
    public List<CxPlanProductStatus> seleteCxPlanProductStatusByIds(Long[] arr) {
        return cxPlanProductStatusMapper.seleteCxPlanProductStatusByIds(arr);
    }

    /**
     * 新增成型计划投产状态
     *
     * @param cxPlanProductStatus 成型计划投产状态
     * @return 结果
     */
    @Override
    public int insertCxPlanProductStatus(CxPlanProductStatus cxPlanProductStatus) {
        cxPlanProductStatus.setBaseVale(null);
        return cxPlanProductStatusMapper.insertCxPlanProductStatus(cxPlanProductStatus);
    }

    /**
     * 修改成型计划投产状态
     *
     * @param cxPlanProductStatus 成型计划投产状态
     * @return 结果
     */
    @Override
    public int updateCxPlanProductStatus(CxPlanProductStatus cxPlanProductStatus) {
        cxPlanProductStatus.setBaseVale(cxPlanProductStatus.getId());
        return cxPlanProductStatusMapper.updateCxPlanProductStatus(cxPlanProductStatus);
    }

    /**
     * 批量删除成型计划投产状态
     *
     * @param ids 需要删除的成型计划投产状态ID
     * @return 结果
     */
    @Override
    public int deleteCxPlanProductStatusByIds(Long[] ids) {
        return cxPlanProductStatusMapper.deleteCxPlanProductStatusByIds(ids);
    }

    /**
     * 标记不投产
     */
    public int markUnProduct(Long[] ids) {
        return cxPlanProductStatusMapper.markUnProduct(ids);
    }

    /**
     * 删除成型计划投产状态信息
     *
     * @param id 成型计划投产状态ID
     * @return 结果
     */
    @Override
    public int deleteCxPlanProductStatusById(Long id) {
        return cxPlanProductStatusMapper.deleteCxPlanProductStatusById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkCxPlanProductStatusUnique(CxPlanProductStatus cxPlanProductStatus) {
        if (cxPlanProductStatus == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxPlanProductStatus> list = cxPlanProductStatusMapper.selectCxPlanProductStatusList(cxPlanProductStatus);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 发布成功后将投产状态为待发布的变更为已投产
     *
     * @ClassName CxPlanProductStatusMapper1
     * @Author Joran.Zhang
     * @Date 2021/8/4 9:16
     * @Version 1.0
     **/
    @Override
    public int updateProductStatusToProduct(CxPlanProductStatus cxPlanProductStatus) {
        return cxPlanProductStatusMapper.updateProductStatusToProduct(cxPlanProductStatus);
    }

}
