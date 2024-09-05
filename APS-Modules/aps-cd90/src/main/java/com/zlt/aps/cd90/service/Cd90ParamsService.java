package com.zlt.aps.cd90.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.dto.Cd90ParamsDto;
import com.zlt.aps.cd90.entity.Cd90Params;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 90度裁断参数信息Service接口
 *
 * @author chenxueyuan
 * @date 2021-06-07
 */
public interface Cd90ParamsService extends IService<Cd90Params> {
    /**
     * 查询90度裁断参数信息
     *
     * @param id 90度裁断参数信息ID
     * @return 90度裁断参数信息
     */
    public Cd90Params selectParamsById(Long id);

    /**
     * 查询90度裁断参数信息列表
     *
     * @param params 90度裁断参数信息     * @return 90度裁断参数信息集合
     */
    public List<Cd90ParamsDto> selectParamsList(Cd90Params params);

    /**
     * 修改90度裁断参数信息
     *
     * @param params 90度裁断参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(Cd90Params params);

    /**
     * 校验90度裁断参数代码唯一
     *
     * @param params 90度裁断参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(Cd90Params params);
}
