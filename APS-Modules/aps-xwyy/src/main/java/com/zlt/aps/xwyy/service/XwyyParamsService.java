package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.dto.XwyyParamsDto;
import com.zlt.aps.xwyy.entity.XwyyParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 纤维压延参数信息Service接口
 *
 * @author chenxueyuan
 * @date 2021-06-07
 */
public interface XwyyParamsService extends IService<XwyyParams> {
    /**
     * 查询纤维压延参数信息
     *
     * @param id 纤维压延参数信息ID
     * @return 纤维压延参数信息
     */
    public XwyyParams selectParamsById(Long id);

    /**
     * 查询纤维压延参数信息列表
     *
     * @param params 纤维压延参数信息     * @return 纤维压延参数信息集合
     */
    public List<XwyyParamsDto> selectParamsList(XwyyParams params);

    /**
     * 修改纤维压延参数信息
     *
     * @param params 纤维压延参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(XwyyParams params);

    /**
     * 校验纤维压延参数代码唯一
     *
     * @param params 纤维压延参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(XwyyParams params);
}
