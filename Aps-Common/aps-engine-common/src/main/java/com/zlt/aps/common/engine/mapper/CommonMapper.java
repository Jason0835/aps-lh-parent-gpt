package com.zlt.aps.common.engine.mapper;


import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
  * 通用的数据获取
  * @ClassName CommonMapper
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/7/1 11:02
  * @Version 1.0
**/
public interface CommonMapper {

    /**
     * 成型机台列表加载
     * @return
     */
    List<CxMachineInfo> selectCxMachineInfoList(CxMachineInfo cxMachineInfo);

    /**
     * 查询在施工信息中 没有对应记录的胎胚code
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return
     */
    List<String> listLossConstructionForCx(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询在施工信息中 没有对应记录的胎圈代码
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @return
     */
    List<String> listLossConstructionForTq(@Param("scheduleDate") String scheduleDate);

}
