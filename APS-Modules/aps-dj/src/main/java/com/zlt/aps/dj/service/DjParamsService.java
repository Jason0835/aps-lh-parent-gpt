package com.zlt.aps.dj.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.dto.DjParamsDto;
import com.zlt.aps.dj.api.domain.entity.DjParams;

/**
 * 垫胶参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface DjParamsService extends IService<DjParams> {
    /**
     * 查询垫胶参数信息
     *
     * @param id 垫胶参数信息ID
     * @return 垫胶参数信息
     */
    public DjParams selectParamsById(Long id);

    /**
     * 查询垫胶参数信息列表
     *
     * @param params 垫胶参数信息     * @return 垫胶参数信息集合
     */
    public List<DjParamsDto> selectParamsList(DjParams params);

    /**
     * 修改垫胶参数信息
     *
     * @param params 垫胶参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(DjParams params);

    /**
     * 校验垫胶参数代码唯一
     *
     * @param params 垫胶参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(DjParams params);
}
