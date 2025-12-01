package com.zlt.mix.service;

import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.schedule.api.service.ISchedulePermissionService;
import com.zlt.mix.setting.api.domain.entity.LhflMachine;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import com.zlt.mix.setting.api.service.ILhflMachineService;
import com.zlt.mix.setting.api.service.IMesBasMaterialService;
import com.zlt.mix.setting.api.service.IMixMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author Gim
 */
@Service("setting")
public class SettingService {

    @Autowired
    private IMixMachineService machineService;

    @Autowired
    private ILhflMachineService iLhflMachineService;

    @Autowired
    private IMesBasMaterialService iMesBasMaterialService;

    @Autowired
    private ISchedulePermissionService iSchedulePermissionService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Value("${materialName.timeout:86400000}")
    private Long materialNameTimeout;

    @Value("${machine.timeout:86400000}")
    private Long machineTimeout;

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 从缓存中获取数据，有则返回，无则更新缓存并返回
     *
     * @param function 实际调用module方法
     * @param t        调用module方法的参数
     * @param key      缓存中的key
     * @param timeout  缓存中的key对应的过期时间（单位：毫秒）
     * @param <T>      方法调用值类型
     * @param <R>      方法返回值类型
     * @return 调用后台方法返回的数据或者之前缓存的数据
     */
    private <T, R> R getRedisCache(Function<T, R> function, T t, String key, Long timeout) {
        Object o = redisTemplate.opsForValue().get(key);
        if (o != null) {
            return (R) o;
        }
        R r;
        try {
            r = function.apply(t);
        } catch (Exception e) {
            //获取返回值类型为List出现异常
            e.printStackTrace();
            return null;
        }
        //获取返回值类型为TableDataInfo出现异常
        if (r instanceof TableDataInfo) {
            TableDataInfo tableDataInfo = (TableDataInfo) r;
            if (!Integer.valueOf("200").equals(tableDataInfo.getCode())) {
                return r;
            }
        }
        redisTemplate.opsForValue().set(key, r, timeout, TimeUnit.MILLISECONDS);
        return r;
    }

    /**
     * 机台下拉列表
     */
    public List<MixMachine> getMachineInfo() {
        return getMachineInfo("");
    }

    /**
     * 机台下拉列表
     */
    public List<MixMachine> getMachineInfo(String mixArea) {
        MixMachine mixMachine = new MixMachine();
        if (StringUtils.isNotBlank(mixArea)) {
            mixMachine.setMixArea(mixArea);
        }
        //调用先从换缓存中获取的方法
        TableDataInfo info = getRedisCache(machineService::listMixMachine, mixMachine, ZltConstant.CACHE_MIX_MACHINE + mixArea, machineTimeout);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.message.getMachineInfo"));
        }
        return (List<MixMachine>) info.getRows();
    }

    /**
     * 机台下拉列表
     */
    public List<MixMachine> getEnableMachineInfo() {
        return getEnableMachineInfo("");
    }

    /**
     * 获取启用的密炼机台下拉列表
     */
    public List<MixMachine> getEnableMachineInfo(String mixArea) {
        MixMachine mixMachine = new MixMachine();
        mixMachine.setStatus(ZltConstant.STATUS_ENABLE);
        if (StringUtils.isNotBlank(mixArea)) {
            mixMachine.setMixArea(mixArea);
        }
        //调用先从换缓存中获取的方法
        TableDataInfo info = getRedisCache(machineService::listMixMachine, mixMachine, ZltConstant.CACHE_ENABLE_MIX_MACHINE + mixArea, machineTimeout);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.message.getMachineInfo"));
        }
        return (List<MixMachine>) info.getRows();
    }

    /**
     * 机台下拉列表
     */
    public List<LhflMachine> getLhflMachineInfo() {
        return getLhflMachineInfo("");
    }

    /**
     * 机台下拉列表
     */
    public List<LhflMachine> getLhflMachineInfo(String mixArea) {
        LhflMachine lhflMachine = new LhflMachine();
        if (StringUtils.isNotBlank(mixArea)) {
            lhflMachine.setMixArea(mixArea);
        }
        //调用先从换缓存中获取的方法
        TableDataInfo info = getRedisCache(iLhflMachineService::listLhflMachine, lhflMachine, ZltConstant.CACHE_LHFL_MACHINE + mixArea, machineTimeout);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.message.getMachineInfo"));
        }
        return (List<LhflMachine>) info.getRows();
    }

    /**
     * 机台下拉列表
     */
    public List<LhflMachine> getEnableLhflMachineInfo() {
        return getEnableLhflMachineInfo("");
    }

    /**
     * 机台下拉列表
     */
    public List<LhflMachine> getEnableLhflMachineInfo(String mixArea) {
        LhflMachine lhflMachine = new LhflMachine();
        lhflMachine.setStatus(ZltConstant.STATUS_ENABLE);
        if (StringUtils.isNotBlank(mixArea)) {
            lhflMachine.setMixArea(mixArea);
        }
        //调用先从换缓存中获取的方法
        TableDataInfo info = getRedisCache(iLhflMachineService::listLhflMachine, lhflMachine, ZltConstant.CACHE_ENABLE_LHFL_MACHINE + mixArea, machineTimeout);
        if (StringUtils.isNull(info)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.message.getMachineInfo"));
        }
        return (List<LhflMachine>) info.getRows();
    }

    /**
     * 胶料名称列表
     */
    public List<String> getFinalGlueNames() {
        //物料大类为5的终炼胶料名称
        return getRedisCache(iMesBasMaterialService::listMaterialName, Arrays.asList(5), ZltConstant.CACHE_FINALGLUE_NAME, materialNameTimeout);
    }

    /**
     * 胶料名称列表
     */
    public List<String> getGlueNames() {
        //物料大类为3、4、5的为胶料名称（3为塑炼胶）
        return getRedisCache(iMesBasMaterialService::listMaterialName, Arrays.asList(3, 4, 5), ZltConstant.CACHE_COMPOUND_NAME, materialNameTimeout);
    }

    /**
     * 辅料名称列表
     */
    public List<String> getAccessoriesNames() {
        //物料大类为2的为辅料名称
        return getRedisCache(iMesBasMaterialService::listMaterialName, Arrays.asList(2), ZltConstant.CACHE_ACCESSORIES_NAME, materialNameTimeout);
    }

    /**
     * 物料名称列表
     */
    public List<String> getMaterialNames() {
        //不指定物料大类表示所有物料名称
        return getRedisCache(iMesBasMaterialService::listMaterialName, Arrays.asList(), ZltConstant.CACHE_MATERIALS_NAME, materialNameTimeout);
    }

    /**
     * 获取用户可以选定的密炼区的字典
     */
    public List<SysDictData> haveMixAreaPermission(List<String> mixAreaList){
        if(mixAreaList==null||mixAreaList.isEmpty()){
            return null;
        }
        //转换为密炼区对应字典
        Map<String, SysDictData> mixType = CollectionUtil.toMap(iSysDictDataCacheService.getType("MIX_AREA"), SysDictData::getDictValue);
        List<SysDictData> sysDictDataList=new ArrayList<>();
        for (String i : mixAreaList) {
            sysDictDataList.add(mixType.get(i));
        }
        return sysDictDataList;
    }

    /**
     * 获取密炼排程的当前用户的密炼区权限字典
     */
    public List<SysDictData> scheduleMixAreaPermission(){
        return haveMixAreaPermission(iSchedulePermissionService.haveMixAreaPermission());
    }

    /**
     * 查询所有机台信息(包含硫磺辅料机台信息)
     * @return 查询到的机台信息
     */
    public List<MixMachine> getAllMachineInfo() {
        return machineService.getAllMachineInfo();
    }
}
