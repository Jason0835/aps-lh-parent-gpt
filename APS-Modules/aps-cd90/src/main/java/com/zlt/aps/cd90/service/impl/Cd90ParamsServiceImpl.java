package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd90.api.domain.dto.Cd90ParamsDto;
import com.zlt.aps.cd90.entity.Cd90Params;
import com.zlt.aps.cd90.mapper.Cd90ParamsMapper;
import com.zlt.aps.cd90.service.Cd90ParamsService;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 90度裁断参数信息Service业务层处理
 *
 * @author zlt
 * @date 2021-06-07
 */
@Service
public class Cd90ParamsServiceImpl extends ServiceImpl<Cd90ParamsMapper, Cd90Params> implements Cd90ParamsService {
    @Autowired
    private Cd90ParamsMapper cd90ParamsMapper;

    /**
     * 查询90度裁断参数信息
     *
     * @param id 90度裁断参数信息ID
     * @return 90度裁断参数信息
     */
    @Override
    public Cd90Params selectParamsById(Long id) {
        LambdaQueryWrapper<Cd90Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90Params::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(Cd90Params::getId, id);
        return cd90ParamsMapper.selectOne(wrapper);
    }

    /**
     * 查询90度裁断参数信息列表
     *
     * @param params 90度裁断参数信息
     * @return 90度裁断参数信息
     */
    @Override
    public List<Cd90ParamsDto> selectParamsList(Cd90Params params) {
        return cd90ParamsMapper.listParams(params);
    }

    /**
     * 修改90度裁断参数信息
     *
     * @param params 90度裁断参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(Cd90Params params) {
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
            cd90ParamsMapper.updateById(params);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(params.getErrorTips());
    }

    /**
     * 校验参数代码唯一性
     *
     * @param params 90度裁断参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(Cd90Params params) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();
        Cd90Params info = cd90ParamsMapper.checkParamsCodeUnique(params.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
