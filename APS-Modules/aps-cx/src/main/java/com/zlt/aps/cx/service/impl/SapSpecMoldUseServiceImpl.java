package com.zlt.aps.cx.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.mapper.SapSpecMoldUseMapper;
import com.zlt.aps.cx.api.domain.entity.SapSpecMoldUse;
import com.zlt.aps.cx.service.SapSpecMoldUseService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 规格使用模数Service业务层处理
 * 
 * @author zlt
 * @date 2022-01-18
 */
@Service
public class SapSpecMoldUseServiceImpl implements SapSpecMoldUseService
{
    @Autowired
    private SapSpecMoldUseMapper sapSpecMoldUseMapper;

    /**
     * 查询规格使用模数
     * 
     * @param id 规格使用模数ID
     * @return 规格使用模数
     */
    @Override
    public SapSpecMoldUse selectSapSpecMoldUseById(Long id)
    {
        return sapSpecMoldUseMapper.selectSapSpecMoldUseById(id);
    }

    /**
     * 查询规格使用模数列表
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 规格使用模数
     */
    @Override
    public List<SapSpecMoldUse> selectSapSpecMoldUseList(SapSpecMoldUse sapSpecMoldUse)
    {
        return sapSpecMoldUseMapper.selectSapSpecMoldUseList(sapSpecMoldUse);
    }

    public List<SapSpecMoldUse> getSpecDesc(SapSpecMoldUse sapSpecMoldUse){
        return sapSpecMoldUseMapper.getSpecDesc(sapSpecMoldUse);
    }

    /**
     * 新增规格使用模数
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 结果
     */
    @Override
    public int insertSapSpecMoldUse(SapSpecMoldUse sapSpecMoldUse)
    {
        sapSpecMoldUse.setBaseVale(null);
        return sapSpecMoldUseMapper.insertSapSpecMoldUse(sapSpecMoldUse);
    }

    /**
     * 修改规格使用模数
     * 
     * @param sapSpecMoldUse 规格使用模数
     * @return 结果
     */
    @Override
    public int updateSapSpecMoldUse(SapSpecMoldUse sapSpecMoldUse)
    {
        sapSpecMoldUse.setBaseVale(sapSpecMoldUse.getId());
        return sapSpecMoldUseMapper.updateSapSpecMoldUse(sapSpecMoldUse);
    }

    /**
     * 批量删除规格使用模数
     * 
     * @param ids 需要删除的规格使用模数ID
     * @return 结果
     */
    @Override
    public int deleteSapSpecMoldUseByIds(Long[] ids)
    {
        return sapSpecMoldUseMapper.deleteSapSpecMoldUseByIds(ids);
    }

    /**
     * 删除规格使用模数信息
     * 
     * @param id 规格使用模数ID
     * @return 结果
     */
    @Override
    public int deleteSapSpecMoldUseById(Long id)
    {
        return sapSpecMoldUseMapper.deleteSapSpecMoldUseById(id);
    }

    /**
     * 校验规格使用模数唯一性
     */
    @Override
    public String checkSapSpecMoldUseUnique(SapSpecMoldUse sapSpecMoldUse) {
        List<SapSpecMoldUse> list = sapSpecMoldUseMapper.checkSapSpecMoldUseUnique(sapSpecMoldUse);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入规格使用模数数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<SapSpecMoldUse> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<SapSpecMoldUse> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getSapCode()+a.getEmbryoCode()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            SapSpecMoldUse sapSpecMoldUse = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(sapSpecMoldUse.getSapCode()+sapSpecMoldUse.getEmbryoCode());
            if (hasValue > 1) {
                failureNum++;
                sapSpecMoldUse.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.sapCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.embryoCode");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, sapSpecMoldUse);
            if (StringUtils.isEmpty(sapSpecMoldUse.getSapCode()) && StringUtils.isEmpty(sapSpecMoldUse.getEmbryoCode())) {
                addImportErrorLog(importLogId, i + 2,I18nUtil.getMessage("ui.data.column.sapSpecMoldUse.isAllNull"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                sapSpecMoldUse.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                sapSpecMoldUse.setBaseVale(null);
                importList.add(sapSpecMoldUse);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    sapSpecMoldUseMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    SapSpecMoldUse sapSpecMoldUse = list.get(i);
                    // 错误记录跳过
                    if (sapSpecMoldUse.getId() != null && sapSpecMoldUse.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkSapSpecMoldUseUnique(sapSpecMoldUse);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertSapSpecMoldUse(sapSpecMoldUse);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
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
