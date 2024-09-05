package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.dto.CxShowDeDto;
import com.zlt.aps.cx.api.domain.dto.LhShowDeDto;
import com.zlt.aps.cx.entity.CxParams;
import com.zlt.aps.cx.mapper.CxParamsMapper;
import com.zlt.aps.cx.service.CxParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 成型工序参数维护功能逻辑层
 */
@Service
public class CxParamsServiceImpl extends ServiceImpl<CxParamsMapper, CxParams> implements CxParamsService {

    @Autowired
    private CxParamsMapper paramsMapper;

    /**
     * 查询成型参数信息
     *
     * @param id 成型参数信息ID
     * @return 成型参数信息
     */
    @Override
    public CxParams selectParamsById(Long id) {
        LambdaQueryWrapper<CxParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(CxParams::getId, id);
        return paramsMapper.selectOne(wrapper);
    }

    /**
     * 查询成型参数信息列表
     *
     * @param params 成型参数信息
     * @return 成型参数信息
     */
    @Override
    public List<CxParamsDto> selectParamsList(CxParams params) {
        return paramsMapper.listParams(params);
    }


    /**
     * 查询成型定额信息列表
     *
     * @param params 成型参数信息
     * @return 成型参数信息
     */
    @Override
    public List<CxShowDeDto> selectCxShowDeDtoList(CxShowDeDto params) {
        return paramsMapper.selectCxShowDeDtoList(params);
    }

    /**
     * 修改成型参数信息
     *
     * @param params 成型参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(CxParams params) {
        //基本参数校验
        if (!ObjectUtils.allNotNull(params.getId(), params.getParamCode(), params.getParamName(), params.getParamValue())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.invalidParameter"));
        }
        String unique = checkParamsCodeUnique(params);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        //校验正则表达式
        boolean regularResult = true;
        String regularExpression = params.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)) {
            Pattern pattern = Pattern.compile(regularExpression);
            regularResult = pattern.matcher(params.getParamValue()).matches();
        }
        if (regularResult) {
            params.setBaseVale(params.getId());
            paramsMapper.updateById(params);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(params.getErrorTips());
    }

    /**
     * 校验参数代码唯一性
     *
     * @param params 成型参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(CxParams params) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();
        CxParams info = paramsMapper.checkParamsCodeUnique(params.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }


    @Override
    public List<LhShowDeDto> selectLhShowDeDtoList(LhShowDeDto showDeDto) {
        return paramsMapper.selecLhShowDeDtoList(showDeDto);
    }
}
