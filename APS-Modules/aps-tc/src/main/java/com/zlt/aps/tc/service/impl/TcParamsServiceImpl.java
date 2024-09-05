package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tc.api.domain.dto.TcParamsDto;
import com.zlt.aps.tc.entity.TcParams;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.service.TcParamsService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 胎侧参数信息Service业务层处理
 *
 * @author zlt
 * @date 2021-05-25
 */
@Service
public class TcParamsServiceImpl extends ServiceImpl<TcParamsMapper, TcParams> implements TcParamsService {
    @Autowired
    private TcParamsMapper tcParamsMapper;

    /**
     * 查询胎侧参数信息
     *
     * @param id 胎侧参数信息ID
     * @return 胎侧参数信息
     */
    @Override
    public TcParams selectParamsById(Long id) {
        LambdaQueryWrapper<TcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcParams::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(TcParams::getId, id);
        return tcParamsMapper.selectOne(wrapper);
    }

    /**
     * 查询胎侧参数信息列表
     *
     * @param params 胎侧参数信息
     * @return 胎侧参数信息
     */
    @Override
    public List<TcParamsDto> selectParamsList(TcParams params) {
        return tcParamsMapper.listParams(params);
    }

    /**
     * 修改胎侧参数信息
     *
     * @param params 胎侧参数信息
     * @return 结果
     */
    @Override
    public AjaxResult updateParams(TcParams params) {
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
            tcParamsMapper.updateById(params);
        }
        return regularResult ? AjaxResult.success() : AjaxResult.error(params.getErrorTips());
    }

    /**
     * 校验参数代码唯一性
     *
     * @param params 胎侧参数信息
     * @return 是否唯一
     */
    @Override
    public String checkParamsCodeUnique(TcParams params) {
        //校验参数代码字段唯一性
        Long paramsId = StringUtils.isNull(params.getId()) ? -1L : params.getId();
        TcParams info = tcParamsMapper.checkParamsCodeUnique(params.getParamCode(), paramsId);
        if (StringUtils.isNotNull(info) && info.getId().longValue() != paramsId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
