package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixFinalMaterialStockMapper;
import com.zlt.aps.lh.api.domain.entity.MixFinalMaterialStock;
import com.zlt.aps.lh.service.MixFinalMaterialStockService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 终炼小料库存Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixFinalMaterialStockServiceImpl implements MixFinalMaterialStockService
{
    @Autowired
    private MixFinalMaterialStockMapper mixFinalMaterialStockMapper;

    /**
     * 查询终炼小料库存
     * 
     * @param id 终炼小料库存ID
     * @return 终炼小料库存
     */
    @Override
    public MixFinalMaterialStock selectMixFinalMaterialStockById(Long id)
    {
        return mixFinalMaterialStockMapper.selectMixFinalMaterialStockById(id);
    }

    /**
     * 查询终炼小料库存列表
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 终炼小料库存
     */
    @Override
    public List<MixFinalMaterialStock> selectMixFinalMaterialStockList(MixFinalMaterialStock mixFinalMaterialStock)
    {
        return mixFinalMaterialStockMapper.selectMixFinalMaterialStockList(mixFinalMaterialStock);
    }

    /**
     * 新增终炼小料库存
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 结果
     */
    @Override
    public int insertMixFinalMaterialStock(MixFinalMaterialStock mixFinalMaterialStock)
    {
        mixFinalMaterialStock.setBaseVale(null);
        return mixFinalMaterialStockMapper.insertMixFinalMaterialStock(mixFinalMaterialStock);
    }

    /**
     * 修改终炼小料库存
     * 
     * @param mixFinalMaterialStock 终炼小料库存
     * @return 结果
     */
    @Override
    public int updateMixFinalMaterialStock(MixFinalMaterialStock mixFinalMaterialStock)
    {
        mixFinalMaterialStock.setBaseVale(mixFinalMaterialStock.getId());
        return mixFinalMaterialStockMapper.updateMixFinalMaterialStock(mixFinalMaterialStock);
    }

    /**
     * 批量删除终炼小料库存
     * 
     * @param ids 需要删除的终炼小料库存ID
     * @return 结果
     */
    @Override
    public int deleteMixFinalMaterialStockByIds(Long[] ids)
    {
        return mixFinalMaterialStockMapper.deleteMixFinalMaterialStockByIds(ids);
    }

    /**
     * 删除终炼小料库存信息
     * 
     * @param id 终炼小料库存ID
     * @return 结果
     */
    @Override
    public int deleteMixFinalMaterialStockById(Long id)
    {
        return mixFinalMaterialStockMapper.deleteMixFinalMaterialStockById(id);
    }

    /**
     * 校验终炼小料库存唯一性
     */
    @Override
    public String checkMixFinalMaterialStockUnique(MixFinalMaterialStock mixFinalMaterialStock) {
        if (mixFinalMaterialStock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixFinalMaterialStock> list = mixFinalMaterialStockMapper.selectMixFinalMaterialStockList(mixFinalMaterialStock);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入终炼小料库存数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixFinalMaterialStock> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixFinalMaterialStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixFinalMaterialStock mixFinalMaterialStock = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixFinalMaterialStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixFinalMaterialStock.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixFinalMaterialStock.setBaseVale(null);
                importList.add(mixFinalMaterialStock);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixFinalMaterialStockMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixFinalMaterialStock mixFinalMaterialStock = list.get(i);
                    // 错误记录跳过
                    if (mixFinalMaterialStock.getId() != null && mixFinalMaterialStock.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixFinalMaterialStockUnique(mixFinalMaterialStock);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixFinalMaterialStock(mixFinalMaterialStock);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
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
