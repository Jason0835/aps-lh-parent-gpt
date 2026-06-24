package com.zlt.aps.tq.engine.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;

public interface TqEngineMachineService {
    /**
     * 查询胎圈机台
     * @param factoryCode 分厂编码（按工厂过滤机台）
     * @return
     */
    List<TqMachineInfo> listTqMachine(String factoryCode);
    /**
     * 获得胎圈代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    Map<String, String> getMouthPlateMachineMap();

    /**
     * 获得机台寸口映射，key=机台编号，value=该机台可做的寸口值列表
     * @return
     */
    Map<String, List<BigDecimal>> getMachineChuckMap();

    /**
     * 获取上一天规格已排产机台列表
     * @param scheduleDate
     * @return
     */
    Map<String, String> getLastDayPlanMachine(Date scheduleDate);
}
