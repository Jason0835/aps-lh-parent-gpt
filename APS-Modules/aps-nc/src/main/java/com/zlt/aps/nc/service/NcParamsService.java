package com.zlt.aps.nc.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcParams;

/**
 * 内衬参数信息Service接口
 *
 * @author zlt
 * @date 2026-06-25
 */
public interface NcParamsService extends IService<NcParams> {
    /**
     * 查询排产参数信息
     *
     * @param id 排产参数信息ID
     * @return 排产参数信息
     */
    public NcParams selectParamsById(Long id);

    /**
     * 查询排产参数信息列表
     *
     * @param params 排产参数信息
     * @return 排产参数信息集合
     */
    public List<NcParams> selectParamsList(NcParams params);

    /**
     * 修改排产参数信息
     *
     * @param params 排产参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(NcParams params);

    /**
     * 校验排产参数代码唯一
     *
     * @param params 排产参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(NcParams params);

    /**
     * 根据条件查询排产参数
     *
     * @param factoryCode      工厂编码
     * @param productTypeCode  产品品类
     * @param paramCode        参数编码
     * @return 排产参数信息
     */
    public NcParams getParamsByCondition(String factoryCode, String productTypeCode, String paramCode);
}
