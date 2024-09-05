package com.ruoyi.framework.web.service;

import com.ruoyi.api.gateway.system.domain.SysDictData;

import java.util.List;

public interface IDictService {

    /**
     * 根据字典类型查询字典数据信息
     *
     * @param dictType 字典类型
     * @return 参数键值
     */
    public List<SysDictData> getType(String dictType);

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    public String getLabel(String dictType, String dictValue);

    public void reloadCache();

    /***
     * 找出一个对象出来，用来获取界面参数值
     * @param dictType
     * @param dictValue
     * @return
     */
    public SysDictData getOneDict(String dictType, String dictValue);
}
