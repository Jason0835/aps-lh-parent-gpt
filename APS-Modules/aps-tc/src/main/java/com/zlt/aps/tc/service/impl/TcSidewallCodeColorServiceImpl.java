package com.zlt.aps.tc.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.tc.mapper.TcSidewallCodeColorMapper;
import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;
import com.zlt.aps.tc.service.TcSidewallCodeColorService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎侧代码前缀颜色设定Service业务层处理
 * 
 * @author zlt
 * @date 2022-01-14
 */
@Service
public class TcSidewallCodeColorServiceImpl implements TcSidewallCodeColorService
{
    @Autowired
    private TcSidewallCodeColorMapper tcSidewallCodeColorMapper;

    /**
     * 查询胎侧代码前缀颜色设定
     * 
     * @param id 胎侧代码前缀颜色设定ID
     * @return 胎侧代码前缀颜色设定
     */
    @Override
    public TcSidewallCodeColor selectTcSidewallCodeColorById(Long id)
    {
        return tcSidewallCodeColorMapper.selectTcSidewallCodeColorById(id);
    }

    /**
     * 查询胎侧代码前缀颜色设定列表
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 胎侧代码前缀颜色设定
     */
    @Override
    public List<TcSidewallCodeColor> selectTcSidewallCodeColorList(TcSidewallCodeColor tcSidewallCodeColor)
    {
        return tcSidewallCodeColorMapper.selectTcSidewallCodeColorList(tcSidewallCodeColor);
    }

    /**
     * 新增胎侧代码前缀颜色设定
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 结果
     */
    @Override
    public int insertTcSidewallCodeColor(TcSidewallCodeColor tcSidewallCodeColor)
    {
        tcSidewallCodeColor.setBaseVale(null);
        return tcSidewallCodeColorMapper.insertTcSidewallCodeColor(tcSidewallCodeColor);
    }

    /**
     * 修改胎侧代码前缀颜色设定
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 结果
     */
    @Override
    public int updateTcSidewallCodeColor(TcSidewallCodeColor tcSidewallCodeColor)
    {
        tcSidewallCodeColor.setBaseVale(tcSidewallCodeColor.getId());
        return tcSidewallCodeColorMapper.updateTcSidewallCodeColor(tcSidewallCodeColor);
    }

    /**
     * 批量删除胎侧代码前缀颜色设定
     * 
     * @param ids 需要删除的胎侧代码前缀颜色设定ID
     * @return 结果
     */
    @Override
    public int deleteTcSidewallCodeColorByIds(Long[] ids)
    {
        return tcSidewallCodeColorMapper.deleteTcSidewallCodeColorByIds(ids);
    }

    /**
     * 删除胎侧代码前缀颜色设定信息
     * 
     * @param id 胎侧代码前缀颜色设定ID
     * @return 结果
     */
    @Override
    public int deleteTcSidewallCodeColorById(Long id)
    {
        return tcSidewallCodeColorMapper.deleteTcSidewallCodeColorById(id);
    }

    /**
     * 校验胎侧代码前缀颜色设定唯一性
     */
    @Override
    public String checkTcSidewallCodeColorUnique(TcSidewallCodeColor tcSidewallCodeColor) {
        List<TcSidewallCodeColor> list = tcSidewallCodeColorMapper.checkTcSidewallCodeColorUnique(tcSidewallCodeColor);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入胎侧代码前缀颜色设定数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<TcSidewallCodeColor> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<TcSidewallCodeColor> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSidewallCode()==null?"":a.getSidewallCode()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            TcSidewallCodeColor tcSidewallCodeColor = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(tcSidewallCodeColor.getSidewallCode());
            if (hasValue > 1) {
                failureNum++;
                tcSidewallCodeColor.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.sidewallCodeColor.sidewallCode");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, tcSidewallCodeColor);
            if (CollectionUtils.isNotEmpty(validated)) {
                tcSidewallCodeColor.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                //设置正则表达式(匹配类型:0-全匹配，1-前缀，2-后缀，3-中间，4-自定义)
                if("0".equals(tcSidewallCodeColor.getMatchType())){
                    tcSidewallCodeColor.setRegularExpression("^"+tcSidewallCodeColor.getSidewallCode()+"$");
                }else if("1".equals(tcSidewallCodeColor.getMatchType())){
                    tcSidewallCodeColor.setRegularExpression("^"+tcSidewallCodeColor.getSidewallCode()+".*$");
                }else if("2".equals(tcSidewallCodeColor.getMatchType())){
                    tcSidewallCodeColor.setRegularExpression("^.*"+tcSidewallCodeColor.getSidewallCode()+"$");
                }else if("3".equals(tcSidewallCodeColor.getMatchType())){
                    tcSidewallCodeColor.setRegularExpression("^(?!"+tcSidewallCodeColor.getSidewallCode()+").*"+tcSidewallCodeColor.getSidewallCode()+".*(?<!"+tcSidewallCodeColor.getSidewallCode()+")$");
                }else if("4".equals(tcSidewallCodeColor.getMatchType())){
                    tcSidewallCodeColor.setRegularExpression(tcSidewallCodeColor.getSidewallCode());
                }

                if (StringUtils.isBlank(tcSidewallCodeColor.getColorCode())) {
                    tcSidewallCodeColor.setColorCode("#000000");
                }
                tcSidewallCodeColor.setBaseVale(null);
                importList.add(tcSidewallCodeColor);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tcSidewallCodeColorMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    TcSidewallCodeColor tcSidewallCodeColor = list.get(i);
                    // 错误记录跳过
                    if (tcSidewallCodeColor.getId() != null && tcSidewallCodeColor.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkTcSidewallCodeColorUnique(tcSidewallCodeColor);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertTcSidewallCodeColor(tcSidewallCodeColor);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.sidewallCodeColor.checkUnique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
