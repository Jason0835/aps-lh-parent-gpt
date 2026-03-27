package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.cxlh.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxShowDeDto;
import com.zlt.aps.cxlh.cx.api.domain.dto.LhShowDeDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 成型参数信息Service接口
 *
 * @author zlt
 * @date 2021-05-25
 */
public interface CxParamsService extends IService<CxParams> {
    /**
     * 查询成型参数信息
     *
     * @param id 成型参数信息ID
     * @return 成型参数信息
     */
    public CxParams selectParamsById(Long id);

    /**
     * 查询成型参数信息列表
     *
     * @param params 成型参数信息
     * @return 成型参数信息集合
     */
    public List<CxParamsDto> selectParamsList(CxParams params);

    /**
     * 查询成型定额信息列表
     *
     * @param params 成型参数信息
     * @return 成型参数信息集合
     */
    public List<CxShowDeDto> selectCxShowDeDtoList(CxShowDeDto params);

    /**
     * 修改成型参数信息
     *
     * @param params 成型参数信息
     * @return 结果
     */
    @Transactional
    public AjaxResult updateParams(CxParams params);

    /**
     * 校验成型参数代码唯一
     *
     * @param params 成型参数信息
     * @return 是否唯一
     */
    public String checkParamsCodeUnique(CxParams params);


    List<LhShowDeDto> selectLhShowDeDtoList(LhShowDeDto showDeDto);
}
