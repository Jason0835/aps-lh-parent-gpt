package com.zlt.aps.dj.service;

import java.util.List;

import com.zlt.aps.dj.api.domain.dto.DjSpecifyMachineDto;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.api.domain.entity.DjSpecifyMachine;
import com.zlt.bill.common.service.IDocService;

/**
 * <p>
 * 垫胶定点机台表 服务类
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
public interface DjSpecifyMachineService extends IDocService<DjSpecifyMachine>  {

    /**
     * 根据条件查询定点机台列表
     *
     * @return
     */
//    List<DjSpecifyMachineDto> listSpecifyMachine(DjSpecifyMachineDto dto);
}
