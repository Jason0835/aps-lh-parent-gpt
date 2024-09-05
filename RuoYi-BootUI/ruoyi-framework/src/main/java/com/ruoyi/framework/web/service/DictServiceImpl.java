package com.ruoyi.framework.web.service;

import java.util.List;

import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * RuoYi首创 html调用 thymeleaf 实现字典读取
 * 
 * @author ruoyi
 */
@Service("dict")
public class DictServiceImpl implements IDictService
{

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;
    /**
     * 根据字典类型查询字典数据信息
     * 
     * @param dictType 字典类型
     * @return 参数键值
     */
    @Override
    public List<SysDictData> getType(String dictType)
    {
        List<SysDictData> dicts = iSysDictDataCacheService.getType(dictType);
        if(StringUtils.isNull(dicts)){
            throw new RuntimeException(I18nUtil.getMessage("ui.dicCache.noFound.key") + dictType);
        }
        return dicts;
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     * 
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String getLabel(String dictType, String dictValue)
    {
        String dicts = iSysDictDataCacheService.getLabel(dictType, dictValue);
        if(StringUtils.isNull(dicts)){
            throw new RuntimeException(I18nUtil.getMessage("ui.dicCache.noFound.key") + dictType + ":" + dictValue);
        }
        return dicts;
    }

    @Override
    public SysDictData getOneDict(String dictType, String dictValue){
        List<SysDictData> dicts = iSysDictDataCacheService.getType(dictType);
        if(StringUtils.isNull(dicts)){
            throw new RuntimeException(I18nUtil.getMessage("ui.dicCache.noFound.key")+ dictType);
        }
        SysDictData result = null;

        for (SysDictData i : dicts){
            if(StringUtils.equals(i.getDictValue(),dictValue)){
                result = i;
                break;
            }
        }

        if(StringUtils.isNull(result)){
            throw new RuntimeException(I18nUtil.getMessage("ui.dicCache.noFound") + dictType + ":"+dictValue);
        }

        return  result;
    }

    @Override
    public void reloadCache(){
        iSysDictDataCacheService.reloadCache();
    }
}
