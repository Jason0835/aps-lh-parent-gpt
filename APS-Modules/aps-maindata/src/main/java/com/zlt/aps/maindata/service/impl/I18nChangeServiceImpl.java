package com.zlt.aps.maindata.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.TimeExpiredPoolCache;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.utils.DeflateCompressor;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.messagesource.RedisMessageSource;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.client.RedissonLockClient;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.tlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.constant.I18nConstant;
import com.zlt.aps.common.core.utils.InflateCompressor;
import com.zlt.aps.maindata.mapper.I18nChangeMapper;
import com.zlt.aps.maindata.mapper.I18nRelationMapper;
import com.zlt.aps.maindata.service.I18nChangeService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.I18nChange;
import com.zlt.aps.monthplan.api.domain.entity.I18nRelation;
import com.zlt.aps.monthplan.api.domain.vo.I18nJsonVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 国际化变更记录ServiceImpl
 */
@Slf4j
@Service
public class I18nChangeServiceImpl implements I18nChangeService {

    @Autowired
    RedisService redisService;
    @Value("#{'${i18n_msg.commonBaseName}'.split(',')}")
    private List<String> commonBaseName;
    @Value("#{'${i18n_msg.systemBaseName}'.split(',')}")
    private List<String> systemBaseName;
    @Value("#{'${i18n_msg.uiLoadBaseName}'.split(',')}")
    private List<String> uiLoadBaseName;
    @Value("#{'${i18n_msg.uiUseBaseName}'.split(',')}")
    private List<String> uiUseBaseName;
    @Value("${token.expires.after:720}")
    private long expireTime;
    /**
     * 记录页面多语言的JSON的第一级子元素
     */
    private Set<String> firstElementSet = new HashSet<>();
    @Autowired
    private I18nRelationMapper i18nRelationMapper;
    @Autowired
    private I18nChangeMapper i18nChangeMapper;
    @Autowired
    private RedissonLockClient redissonLockClient;

    @Override
    public void loadPageBundle() {
        // 查询需要页面需要的国际化文件名称
        I18nRelation relation = new I18nRelation();
        relation.setIsPage(ApsConstant.APS_YES_NO_1);
        List<I18nRelation> i18nRelationList = i18nRelationMapper.selectList(relation);
        Set<String> fileNameSet = i18nRelationList.stream().map(I18nRelation::getFileName).collect(Collectors.toSet());

        Map<String, Long> relationMap = i18nRelationList.stream().collect(Collectors.toMap(I18nRelation::getFileName, I18nRelation::getId, (v1, v2) -> v1));
        for (String basename : fileNameSet) {
            // 加载JSON格式的数据到Redis中，并将properties更新到数据库中
            loadCommonBundleDBAndCache(basename, relationMap, ApsConstant.APS_YES_NO_1, true, true, -1L);
        }
    }

    @Override
    public List<I18nChange> selectList(I18nChange query) {
        List<I18nChange> i18nChangeList = i18nChangeMapper.selectRelList(query);
        JsonI18nConvertUtils.conventJsonI18n(i18nChangeList, I18nChange.class);
        return i18nChangeList;
    }


    @Override
    public I18nChange getInfo(Long id) {
        I18nChange query = new I18nChange();
        query.setId(id);
        List<I18nChange> i18nChangeList = i18nChangeMapper.selectRelList(query);
        I18nChange i18nChange = i18nChangeList.get(0);
        JsonI18nConvertUtils.conventJsonI18n(Collections.singletonList(i18nChange), I18nChange.class);
        return i18nChange;
    }

    @Override
    public AjaxResult pageJson(I18nJsonVo jsonVo) {
        String redisKey = buildPageRedisKey(jsonVo.getLocale(),
                StringUtils.isNotBlank(jsonVo.getBasename()) ? jsonVo.getBasename() : I18nConstant.PAGE_FILE_NAME);

        // 本地缓存
        Object cache = TimeExpiredPoolCache.getInstance().get(redisKey);
        if (cache != null) {
            return AjaxResult.success(cache);
        }

        Object pageBundle = getAndLoadPageBundle(redisKey);
        if (pageBundle == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.i18nChange.load"));
        }

        TimeExpiredPoolCache.getInstance().put(redisKey, pageBundle, 20000L);

        return AjaxResult.success(pageBundle);
    }

    @Override
    public byte[] download() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);

        Locale zh = new Locale("zh", "CN");
        Locale en = new Locale("en", "US");
        Locale vi = new Locale("vi", "VN");

        I18nChange query = new I18nChange();
        query.getParams().put("orderBy", "CHANGE_KEY");
        List<I18nChange> i18nChangeList = i18nChangeMapper.selectRelList(query);
        // 国际化构建
        for (I18nChange i18nChange : i18nChangeList) {
            if (StringUtils.isBlank(i18nChange.getChangeValue())) {
                continue;
            }
            i18nChange.setChangeValueI18n_zh_CN(StringUtils.getLocaleName(i18nChange.getChangeValue(), zh, null));
            i18nChange.setChangeValueI18n_en_US(StringUtils.getLocaleName(i18nChange.getChangeValue(), en, null));
            i18nChange.setChangeValueI18n_vi_VN(StringUtils.getLocaleName(i18nChange.getChangeValue(), vi, null));
        }

        Map<Long, List<I18nChange>> i18nChangeMap = i18nChangeList.stream().filter(v -> v.getRelId() != null).collect(Collectors.groupingBy(I18nChange::getRelId));
        for (Map.Entry<Long, List<I18nChange>> entry : i18nChangeMap.entrySet()) {
            try {
                List<I18nChange> list = entry.getValue();
                if (CollectionUtils.isEmpty(list)) {
                    continue;
                }
                I18nChange first = list.get(0);

                String fileName = first.getFileName();
                if (StringUtils.isBlank(fileName)) {
                    fileName = "temp" + System.currentTimeMillis();
                }

                writeZip(fileName + "_" + I18nConstant.ZH_CN + ".properties", zip, list, I18nChange::getChangeValueI18n_zh_CN);
                writeZip(fileName + "_" + I18nConstant.EN_US + ".properties", zip, list, I18nChange::getChangeValueI18n_en_US);
                writeZip(fileName + "_" + I18nConstant.VI_VN + ".properties", zip, list, I18nChange::getChangeValueI18n_vi_VN);

            } catch (Exception e) {
                log.error("转国际化出现异常,所属文件映射：" + entry.getKey(), e);
            }
        }

        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    @Override
    public void loadCommonBundle() {
        if (CollectionUtils.isEmpty(commonBaseName)) {
            return;
        }

        // 查询需要页面需要的国际化文件名称
        I18nRelation relation = new I18nRelation();
        relation.setIsPage(ApsConstant.APS_YES_NO_0);
        List<I18nRelation> i18nRelationList = i18nRelationMapper.selectList(relation);
        Map<String, Long> relationMap = i18nRelationList.stream().collect(Collectors.toMap(I18nRelation::getFileName, I18nRelation::getId, (v1, v2) -> v1));

        for (String basename : commonBaseName) {
            loadCommonBundleDBAndCache(basename, relationMap, ApsConstant.APS_YES_NO_0, false, false, -1L);
        }
    }

    @Override
    public void loadSystemBundle() {
        if (CollectionUtils.isEmpty(systemBaseName)) {
            return;
        }

        // 查询需要页面需要的国际化文件名称
        I18nRelation relation = new I18nRelation();
        relation.setIsPage(ApsConstant.APS_YES_NO_0);
        List<I18nRelation> i18nRelationList = i18nRelationMapper.selectList(relation);
        Map<String, Long> relationMap = i18nRelationList.stream().collect(Collectors.toMap(I18nRelation::getFileName, I18nRelation::getId, (v1, v2) -> v1));

        for (String basename : systemBaseName) {
            loadCommonBundleDBAndCache(basename, relationMap, ApsConstant.APS_YES_NO_0, true, false, -1L);
        }
    }

    /**
     * 加载ui加载的框架国际化到数据库
     */
    @Override
    public void loadUiBaseBundle() {
        if (CollectionUtils.isEmpty(uiLoadBaseName)) {
            return;
        }

        // 查询需要页面需要的国际化文件名称
        I18nRelation relation = new I18nRelation();
        relation.setIsPage(ApsConstant.APS_YES_NO_0);
        List<I18nRelation> i18nRelationList = i18nRelationMapper.selectList(relation);
        Map<String, Long> relationMap = i18nRelationList.stream().collect(Collectors.toMap(I18nRelation::getFileName, I18nRelation::getId, (v1, v2) -> v1));

        for (String basename : uiLoadBaseName) {
            loadCommonBundleDBAndCache(basename, relationMap, ApsConstant.APS_YES_NO_0, false, false, -1L);
        }
    }

    /**
     * 初始化页面多语言的第一级结构
     */
    @Override
    public void initPageJsonFirstElement() {
        // 查询需要页面需要的国际化文件名称
        I18nRelation relation = new I18nRelation();
        relation.setIsPage(ApsConstant.APS_YES_NO_1);
        List<I18nRelation> i18nRelationList = i18nRelationMapper.selectList(relation);
        Set<String> fileNameSet = i18nRelationList.stream().map(I18nRelation::getFileName).collect(Collectors.toSet());

        Set<String> set = new HashSet<>();

        // 取出第一级子元素
        for (String basename : fileNameSet) {
            Locale zh = new Locale("zh", "CN");
            List<Map.Entry<String, String>> zhList = loadBundle(basename, zh);

            if (CollectionUtils.isNotEmpty(zhList)) {
                for (Map.Entry<String, String> entry : zhList) {
                    String key = entry.getKey();
                    key = key.substring(0, key.indexOf("."));
                    set.add(key);
                }
            }
        }

        log.info("解析的页面多语言的第一级结构：" + set);
        firstElementSet = set;
    }

    /**
     * 将国际化记录写入到压缩文件流
     *
     * @param fileName  文件名称
     * @param zip       压缩文件流
     * @param list      国际化记录列表
     * @param fieldFunc 取出的字段字段值
     * @throws IOException IOException
     */
    private void writeZip(String fileName, ZipOutputStream zip, List<I18nChange> list, Function<I18nChange, String> fieldFunc) throws IOException {
        StringWriter sw = new StringWriter();
        for (I18nChange i18nChange : list) {
            sw.append(i18nChange.getChangeKey()).append("=").append(fieldFunc.apply(i18nChange)).append("\n");
        }

        zip.putNextEntry(new ZipEntry(fileName));
        IOUtils.write(sw.toString(), zip, Constants.UTF8);
        IOUtils.closeQuietly(sw);
        zip.flush();
        zip.closeEntry();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult save(I18nChange change) {
        change.setIsChange(ApsConstant.APS_YES_NO_1);
        // 国际化回显具体字段
        change.buildChangeValue();

        // 取出实际数据
        I18nChange query = new I18nChange();
        query.setId(change.getId());
        List<I18nChange> i18nChangeList = i18nChangeMapper.selectRelList(query);
        if (i18nChangeList.size() != 1) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.save.error.msg"));
        }
        I18nChange i18nChange = i18nChangeList.get(0);
        JsonI18nConvertUtils.conventJsonI18n(Collections.singletonList(i18nChange), I18nChange.class);

        // 如果没有差异，不执行更新操作
        if (StringUtils.equals(change.getChangeValue(), i18nChange.getChangeValue())) {
            return AjaxResult.success();
        }

        if (ApsConstant.APS_YES_NO_1.equals(i18nChange.getIsPage())) {
            // 如果是页面显示内容，需要更新Redis存储的JSON文件
            checkAndUpdateRedisJson(I18nConstant.ZH_CN, i18nChange.getFileName(), i18nChange.getChangeKey(), i18nChange.getChangeValueI18n_zh_CN(), change.getChangeValueI18n_zh_CN());
            checkAndUpdateRedisJson(I18nConstant.EN_US, i18nChange.getFileName(), i18nChange.getChangeKey(), i18nChange.getChangeValueI18n_en_US(), change.getChangeValueI18n_en_US());
            checkAndUpdateRedisJson(I18nConstant.VI_VN, i18nChange.getFileName(), i18nChange.getChangeKey(), i18nChange.getChangeValueI18n_vi_VN(), change.getChangeValueI18n_vi_VN());

        } else {
            // 如果是后端内容，需要刷新Redis存储的boot模块和后台模块不同的lang
            checkAndUpdate(I18nConstant.ZH_CN, i18nChange.getFileName(), i18nChange.getChangeKey(), i18nChange.getChangeValueI18n_zh_CN(), change.getChangeValueI18n_zh_CN());
            checkAndUpdate(I18nConstant.EN_US, i18nChange.getFileName(), i18nChange.getChangeKey(), i18nChange.getChangeValueI18n_en_US(), change.getChangeValueI18n_en_US());
            checkAndUpdate(I18nConstant.VI_VN, i18nChange.getFileName(), i18nChange.getChangeKey(), i18nChange.getChangeValueI18n_vi_VN(), change.getChangeValueI18n_vi_VN());
        }

        change.setBaseVale(change.getId());
        i18nChangeMapper.update(change);

        return AjaxResult.success();
    }

    /**
     * 校验是否存在修改内容，需要刷新Redis存储的boot模块和后台模块不同的lang
     *
     * @param localName   国际化名称
     * @param basename    资源名称
     * @param changeKey   修改的键
     * @param beforeValue 修改前的值
     * @param afterValue  修改后的值
     */
    private void checkAndUpdate(String localName, String basename, String changeKey, String beforeValue, String afterValue) {
        if (StringUtils.equals(beforeValue, afterValue)) {
            return;
        }

        // 如果是ui需要使用的框架包，更新前端的国际化
        if (CollectionUtils.isNotEmpty(uiUseBaseName) && uiUseBaseName.contains(basename)) {
            redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_UI_KEY_PREFIX, localName, changeKey), afterValue, expireTime, TimeUnit.SECONDS);
        }

        // 如果是系统模块，不过期更新
        if (CollectionUtils.isNotEmpty(systemBaseName) && systemBaseName.contains(basename)) {
            redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, localName, changeKey), afterValue);

            return;
        }

        // 如果是前后端通用包，更新前端和后端的lang
        if (CollectionUtils.isNotEmpty(commonBaseName) && commonBaseName.contains(basename)) {
            redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, localName, changeKey), afterValue, expireTime, TimeUnit.SECONDS);
            redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_UI_KEY_PREFIX, localName, changeKey), afterValue, expireTime, TimeUnit.SECONDS);
        }
    }

    /**
     * 校验是否存在修改内容，存在修改内容需要更新Redis的Json串
     *
     * @param localName   国际化名称
     * @param basename    资源名称
     * @param changeKey   修改的键
     * @param beforeValue 修改前的值
     * @param afterValue  修改后的值
     */
    private void checkAndUpdateRedisJson(String localName, String basename, String changeKey, String beforeValue, String afterValue) {
        if (StringUtils.equals(beforeValue, afterValue)) {
            return;
        }

        String[] split = changeKey.split("\\.");

        // 取出Redis存储的Json格式内容
        String redisKey = buildPageRedisKey(localName, basename) + ":" + split[0];

        // 解压缩
        Object object = redisService.getCacheObject(redisKey);

        try {
            object = JSONObject.parseObject(InflateCompressor.uncompress2Base64((String) object));
            if (object == null) {
                log.error("解析缓存的多语言有误");
                throw new RuntimeException(I18nUtil.getMessage("ui.data.column.i18nChange.notFound"));
            }
        } catch (Exception e) {
            log.error("修改页面多语言异常", e);
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.i18nChange.notFound"));
        }

        // 根据配置的键更新对应的Json
        JSONObject parent = (JSONObject) object;
        for (int i = 1; i < (split.length - 1); i++) {
            Object child = parent.get(split[i]);
            if (!(child instanceof JSONObject)) {
                throw new RuntimeException(I18nUtil.getMessage("ui.data.column.i18nChange.notFound"));
            }

            parent = (JSONObject) child;
        }

        parent.put(split[split.length - 1], afterValue);

        // 压缩缓存
        redisService.setCacheObject(redisKey, DeflateCompressor.compressObjec2Base64(object));
    }

    /**
     * 查询Redis的语言包，如果不存在尝试加载一次在获取
     *
     * @param keyPrefix 小JSON结构的前缀
     * @return 语言包对象
     */
    private Object getAndLoadPageBundle(String keyPrefix) {


        JSONObject json = buildPageJson(keyPrefix);

        // 如果Redis缓存中没有，尝试加载到Redis缓存中
        if (json == null) {
            log.info("存在重新加载国际化");

            RLock lock = redissonLockClient.getLock(I18nConstant.REDIS_I18N_INIT_LOCK);

            try {
                boolean isLock = lock.tryLock(5, 100, TimeUnit.SECONDS);
                if (!isLock) {
                    log.info("国际化初始化获取锁失败" + keyPrefix);
                    return null;
                }

                // 重新尝试获取本地缓存
                Object cache = TimeExpiredPoolCache.getInstance().get(keyPrefix);
                if (cache != null) {
                    return cache;
                }

                log.info("重新加载页面国际化");
                // 加载页面需要的国际化
                loadPageBundle();

                json = buildPageJson(keyPrefix);
                if (json != null) {
                    return json;
                }

                log.info("重新加载页面国际化完毕");
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;

            } finally {
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        }

        return json;
    }

    /**
     * 根据页面第一级的json缓存构建出完整的json结构
     *
     * @param keyPrefix 前缀
     * @return 完整的json结构，如果存在key丢失，返回为空
     */
    private JSONObject buildPageJson(String keyPrefix) {
        initPageJsonFirstElement();
        JSONObject json = new JSONObject();

        for (String jsonKey : this.firstElementSet) {
            String miniKey = keyPrefix + ":" + jsonKey;

            // 前端解压缩
            Object cacheObject = redisService.getCacheObject(miniKey);
            // cacheObject = JSONObject.parseObject(InflateCompressor.uncompress2Base64((String) cacheObject));

            // 可能存在部分key丢失的情况
            if (cacheObject == null) {
                return null;
            }

            json.put(jsonKey, cacheObject);
        }

        return json;
    }


    /**
     * 加载JSON格式的数据到Redis中，并将properties存储到数据库中
     *
     * @param basename     资源文件
     * @param relationMap  国际化关系映射
     * @param isPage       是否查询页面请求
     * @param isCacheRedis 是否缓存到Redis中
     * @param isCacheJson  是否缓存为JSON格式，为true标识页面解析，否则为解析系统资源包
     * @param expireTime   Redis超时时间，小于0表示不超时
     */
    private void loadCommonBundleDBAndCache(String basename, Map<String, Long> relationMap, Integer isPage, boolean isCacheRedis, boolean isCacheJson, Long expireTime) {
        Locale zh = new Locale("zh", "CN");
        Locale en = new Locale("en", "US");
        Locale vi = new Locale("vi", "VN");

        // 加载对应资源的国际化文件
        List<Map.Entry<String, String>> zhList = loadBundle(basename, zh);
        List<Map.Entry<String, String>> enList = loadBundle(basename, en);
        List<Map.Entry<String, String>> viList = loadBundle(basename, vi);

        // 读取被用户修改的过的文件
        I18nChange i18nChange = new I18nChange();
        i18nChange.setIsPage(isPage);
        i18nChange.setIsChange(ApsConstant.APS_YES_NO_1);
        i18nChange.getParams().put("fileNameList", Collections.singletonList(basename));
        List<I18nChange> i18nChangeList = i18nChangeMapper.selectRelList(i18nChange);
        for (I18nChange change : i18nChangeList) {
            if (StringUtils.isBlank(change.getChangeValue())) {
                continue;
            }
            String json = change.getChangeValue();

            // 添加到国际化资源列表的最后，优先使用用户修改过的国际化内容
            zhList.add(new AbstractMap.SimpleEntry<>(change.getChangeKey(), StringUtils.getLocaleName(json, zh, null)));
            enList.add(new AbstractMap.SimpleEntry<>(change.getChangeKey(), StringUtils.getLocaleName(json, en, null)));
            viList.add(new AbstractMap.SimpleEntry<>(change.getChangeKey(), StringUtils.getLocaleName(json, vi, null)));
        }

        // 记录用户变更过的relId+key
        Function<I18nChange, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getRelId(), v.getChangeKey());
        Set<String> changeSet = i18nChangeList.stream().map(keyFunc).collect(Collectors.toSet());

        // 构建国际化修改记录
        List<I18nChange> changeList = new ArrayList<>();
        Map<String, I18nChange> changeMap = new HashMap<>();
        buildI18nChangeList(zhList, changeList, changeMap, I18nChange::setChangeValueI18n_zh_CN);
        buildI18nChangeList(enList, changeList, changeMap, I18nChange::setChangeValueI18n_en_US);
        buildI18nChangeList(viList, changeList, changeMap, I18nChange::setChangeValueI18n_vi_VN);

        // 映射到实际DB的字段
        for (I18nChange change : changeList) {
            // 国际化字段格式
            change.buildChangeValue();
            change.setBaseVale(null);
            change.setRelId(relationMap.getOrDefault(basename, I18nConstant.I18N_DEFAULT_MODE));
        }

        // 过滤已经被用户修改过的记录
        changeList = changeList.stream().filter(v -> !changeSet.contains(keyFunc.apply(v))).collect(Collectors.toList());

        // 批量merge到DB
        if (!changeList.isEmpty()) {
            String tableSuffix = UUID.randomUUID().toString().replace("-", "");
            i18nChangeMapper.createTempTable(tableSuffix);
            for (List<I18nChange> list : ScmListUtils.getSplitList(changeList, 500)) {
                // 如果 changeKey+relId已经存在，则调用更新SQL，否则调用新增SQL
                i18nChangeMapper.insertTempTable(list);
                i18nChangeMapper.batchUpdateByRelIdAndKey(list);
            }
            i18nChangeMapper.batchInsertByRelIdAndKey();
//            i18nChangeMapper.dropTempTable();
        }

        // 如果需要进行国际化
        if (!isCacheRedis) {
            return;
        }

        if (isCacheJson) {
            // 解析成JSON加载到Redis中
            loadPageBundleJsonRedis(zhList, I18nConstant.ZH_CN, basename);
            loadPageBundleJsonRedis(enList, I18nConstant.EN_US, basename);
            loadPageBundleJsonRedis(viList, I18nConstant.VI_VN, basename);

            return;
        }

        // 拼接后台lang格式即可
        for (I18nChange change : changeList) {
            if (expireTime > 0) {
                redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, zh, change.getChangeKey()), change.getChangeValueI18n_zh_CN(), expireTime, TimeUnit.SECONDS);
                redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, en, change.getChangeKey()), change.getChangeValueI18n_en_US(), expireTime, TimeUnit.SECONDS);
                redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, vi, change.getChangeKey()), change.getChangeValueI18n_vi_VN(), expireTime, TimeUnit.SECONDS);
            } else {
                redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, zh, change.getChangeKey()), change.getChangeValueI18n_zh_CN());
                redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, en, change.getChangeKey()), change.getChangeValueI18n_en_US());
                redisService.setCacheObject(StringUtils.format(RedisMessageSource.LANG_KEY_PREFIX, vi, change.getChangeKey()), change.getChangeValueI18n_vi_VN());
            }
        }
    }

    /**
     * 加载国际化资源中的所有内容
     *
     * @param basename 资源名称
     * @param locale   Locale对象
     * @return 内容映射列表
     */
    private List<Map.Entry<String, String>> loadBundle(String basename, Locale locale) {
        List<Map.Entry<String, String>> list = new ArrayList<>();

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(basename, locale);
            Enumeration<String> keys = bundle.getKeys();

            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = bundle.getString(key);
                list.add(new AbstractMap.SimpleEntry<>(key, value));
            }
        } catch (Exception e) {
            log.error("加载页面国际化失败" + basename + locale, e);
        }

        return list;
    }

    /**
     * 构建国际化修改记录列表，过滤重复key。将相同key的不同国际化文件加载到一个对象
     *
     * @param list       构建国际化修改记录的配置文件列表
     * @param changeList 国际化修改记录列表
     * @param changeMap  国际化修改记录Map
     * @param biConsumer 指定字段的Set方法
     */
    private void buildI18nChangeList(List<Map.Entry<String, String>> list, List<I18nChange> changeList, Map<String, I18nChange> changeMap, BiConsumer<I18nChange, String> biConsumer) {
        for (Map.Entry<String, String> entry : list) {
            String key = entry.getKey();
            String value = entry.getValue();
            I18nChange change = changeMap.get(key);
            if (change == null) {
                change = new I18nChange();
                change.setChangeKey(key);
                changeList.add(change);
                changeMap.put(key, change);
            }
            biConsumer.accept(change, value);
        }
    }

    /**
     * 解析配置文件成Json格式到Redis中
     *
     * @param list       资源文件映射列表
     * @param localeName 国际化名称
     * @param basename   资源名称
     */
    private void loadPageBundleJsonRedis(List<Map.Entry<String, String>> list, String localeName, String basename) {
        // 解析为JSON格式
        JSONObject bigJson = propertiesToJson(list);

        String keyPrefix = buildPageRedisKey(localeName, basename);

        Set<String> keySet = bigJson.keySet();

        // 拆分这个完整的JSON结构成小JSON，压缩存储到Redis
        for (String jsonKey : keySet) {
            String miniKey = keyPrefix + ":" + jsonKey;

            String miniJson = DeflateCompressor.compressObjec2Base64(bigJson.get(jsonKey));
            redisService.setCacheObject(miniKey, miniJson);
        }
    }

    /**
     * 构建页面国际化的Redis key
     *
     * @param localeName 国际化名称
     * @param basename   资源名称
     * @return
     */
    private String buildPageRedisKey(String localeName, String basename) {
        if (StringUtils.isBlank(localeName)) {
            localeName = I18nConstant.ZH_CN;
        }
        return I18nConstant.REDIS_PAGE_JSON + localeName + ":" + basename;
    }

    /**
     * 将properties对象解析为JSON对象
     *
     * @param list properties列表
     * @return JSON对象
     */
    private JSONObject propertiesToJson(List<Map.Entry<String, String>> list) {
        if (CollectionUtils.isEmpty(list)) {
            return new JSONObject();
        }

        // 子节点前缀映射Map
        Map<String, JSONObject> map = new HashMap<>();
        // 根节点
        JSONObject root = new JSONObject();

        // 迭代去除根节点和存在父节点的节点
        for (Map.Entry<String, String> entry : list) {
            String key = entry.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            String value = StringUtils.isNotBlank(entry.getValue()) ? entry.getValue() : "";

            String[] split = key.split("\\.");
            if (split.length == 1) {
                // 如果层级为1表示为根节点
                root.put(key, value);
                continue;
            }

            // 根据前缀添加到映射中
            String suffix = split[split.length - 1];
            String prefix = key.substring(0, key.length() - suffix.length() - 1);
            JSONObject node = map.getOrDefault(prefix, new JSONObject());
            node.put(suffix, value);
            map.put(prefix, node);
        }

        for (Map.Entry<String, JSONObject> entry : map.entrySet()) {
            String key = entry.getKey();
            JSONObject value = entry.getValue();

            // 构建父节点
            String[] split = key.split("\\.");
            JSONObject parent = root;
            for (int i = 0; i < split.length - 1; i++) {
                String prefix = split[i];
                if (parent.containsKey(prefix)
                        && parent.get(prefix) instanceof JSONObject) {
                    parent = (JSONObject) parent.get(split[i]);
                } else {
                    JSONObject node = new JSONObject();
                    parent.put(prefix, node);
                    parent = node;
                }
            }

            // 已存在后缀，重新组装JSON结构
            String lastKey = split[split.length - 1];
            if (parent.containsKey(lastKey) && parent.get(lastKey) instanceof JSONObject) {
                JSONObject lastNode = (JSONObject) parent.get(lastKey);
                for (String item : value.keySet()) {
                    lastNode.put(item, value.get(item));
                }
            } else {
                parent.put(lastKey, value);
            }
        }

        return root;
    }

}
