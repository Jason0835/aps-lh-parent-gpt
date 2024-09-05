package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import com.zlt.aps.lh.entity.LhParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 硫化参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface LhParamsService extends IService<LhParams> {
    /**
     * 查询硫化参数信息
     *
     * @param id 硫化参数信息ID
     * @return 硫化参数信息
     */
    public LhParams selectParamsById(Long id);

    /**
     * 查询硫化参数信息列表
     *
     * @param params 硫化参数信息     * @return 硫化参数信息集合
     */
    public List<LhParamsDto> selectParamsList(LhParams params);

    /**
     * 修改硫化参数信息
     *
     * @param params 硫化参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(LhParams params);

    /**
     * 校验硫化参数代码唯一
     *
     * @param params 硫化参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(LhParams params);
}
