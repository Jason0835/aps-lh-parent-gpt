package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import com.zlt.aps.lh.entity.LhParams;
import com.zlt.aps.lh.mapper.LhParamsMapper;
import com.zlt.aps.lh.service.LhParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 硫化工序参数维护功能逻辑层
 */
@Service
public class LhParamsServiceImpl extends ServiceImpl<LhParamsMapper, LhParams> implements LhParamsService {

    @Autowired
    private LhParamsMapper paramsMapper;

    /**
     * 查询硫化参数信息
     *
     * @param id 硫化参数信息ID
     * @return 硫化参数信息
     */
    @Override
    public LhParams selectParamsById(Long id) {
        LambdaQueryWrapper<LhParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(LhParams::getId, id);
        return paramsMapper.selectOne(wrapper);
    }

    /**
     * 查询硫化参数信息列表
     *
     * @param params 硫化参数信息
     * @return 硫化参数信息
     */
    @Override
    public List<LhParamsDto> selectParamsList(LhParams params) {
        return paramsMapper.listParams(params);
    }

    /**
     * 修改硫化参数信息
     *
     * @param params 硫化参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(LhParams params) {
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
     * @param params 硫化参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(LhParams params) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();
        LhParams info = paramsMapper.checkParamsCodeUnique(params.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
