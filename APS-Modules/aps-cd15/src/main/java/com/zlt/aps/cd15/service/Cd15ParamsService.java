package com.zlt.aps.cd15.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.dto.Cd15ParamsDto;
import com.zlt.aps.cd15.entity.Cd15Params;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 15度裁断参数信息Service接口
 *
 * @author chenxueyuan
 * @date 2021-06-07
 */
public interface Cd15ParamsService extends IService<Cd15Params> {
    /**
     * 查询15度裁断参数信息
     *
     * @param id 15度裁断参数信息ID
     * @return 15度裁断参数信息
     */
    public Cd15Params selectParamsById(Long id);

    /**
     * 查询15度裁断参数信息列表
     *
     * @param params 15度裁断参数信息     * @return 15度裁断参数信息集合
     */
    public List<Cd15ParamsDto> selectParamsList(Cd15Params params);

    /**
     * 修改15度裁断参数信息
     *
     * @param params 15度裁断参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(Cd15Params params);

    /**
     * 校验15度裁断参数代码唯一
     *
     * @param params 15度裁断参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(Cd15Params params);
}
