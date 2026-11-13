package com.zlt.aps.lh.component;

import com.zlt.aps.mp.api.domain.entity.LhScheduleResultIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 硫化排程结果下发内容指纹存储组件。
 *
 * <p>业务背景：</p>
 * <ul>
 *   <li>硫化排程发布到 MES 采用版本化追加模式，每次下发都会生成一个新的 DATA_VERSION；</li>
 *   <li>此前没有内容校验，操作员重复点击“发布”或重复触发下发时会连续产生多个内容完全相同的版本。</li>
 * </ul>
 *
 * <p>处理方式：</p>
 * <ul>
 *   <li>按“工厂 + 排程日期”缓存最近一次成功下发的内容指纹；</li>
 *   <li>本次下发前计算内容指纹，与缓存一致则表示数据未变化，跳过下发，不消耗版本号、不写中间表；</li>
 *   <li>仅在下发成功后记录指纹，下发失败时保留上一次指纹，保证重试仍可正常下发。</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class LhIssueFingerprintStore {

    /** 指纹缓存key前缀 */
    private static final String FINGERPRINT_KEY_PREFIX = "APS:LH:ISSUE_TO_MES:FINGERPRINT:";

    /** 指纹缓存有效期（天），排程日期维度缓存，保留一周足够覆盖追溯与重发场景 */
    private static final long FINGERPRINT_TTL_DAYS = 7L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 计算下发数据内容指纹。
     *
     * <p>说明：</p>
     * <ul>
     *   <li>只使用真正下发到 MES 的业务字段，不包含主键ID、数据版本等每次下发都会变化的字段；</li>
     *   <li>不包含排程结果的UPDATE_TIME：发布成功本身会刷新排程结果的更新时间，
     *       若纳入更新时间会导致重复发布的指纹永远不一致，从而失去去重作用；</li>
     *   <li>先按业务键排序再拼接，保证同一批数据无论查询顺序如何，指纹一致。</li>
     * </ul>
     *
     * @param issueList 本次待下发的硫化排程结果
     * @return 内容指纹（MD5），数据为空时返回空字符串
     */
    public String computeFingerprint(List<LhScheduleResultIssue> issueList) {
        if (CollectionUtils.isEmpty(issueList)) {
            return "";
        }
        String content = issueList.stream()
                .sorted(Comparator
                        .comparing((LhScheduleResultIssue item) -> toText(item.getScheduleDate()))
                        .thenComparing(item -> toText(item.getLhMachineCode()))
                        .thenComparing(item -> toText(item.getMaterialCode()))
                        .thenComparing(item -> toText(item.getOrderNo()))
                        .thenComparing(item -> toText(item.getLeftRightMold())))
                .map(this::toCanonicalLine)
                .collect(Collectors.joining(";"));
        return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取上一次成功下发的内容指纹。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 上次成功下发的指纹，无记录或Redis异常时返回null
     */
    public String getLastFingerprint(String factoryCode, LocalDate scheduleDate) {
        String key = buildKey(factoryCode, scheduleDate);
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // 指纹读取失败不阻断下发，按无历史指纹处理
            log.warn("获取硫化下发内容指纹失败, key: {}, 原因: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 记录本次成功下发的内容指纹。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param fingerprint  内容指纹
     */
    public void saveFingerprint(String factoryCode, LocalDate scheduleDate, String fingerprint) {
        if (fingerprint == null) {
            return;
        }
        String key = buildKey(factoryCode, scheduleDate);
        try {
            stringRedisTemplate.opsForValue().set(key, fingerprint, FINGERPRINT_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            // 指纹写入失败不影响已成功的下发结果
            log.warn("保存硫化下发内容指纹失败, key: {}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 构建指纹缓存key：工厂 + 排程日期
     */
    private String buildKey(String factoryCode, LocalDate scheduleDate) {
        return FINGERPRINT_KEY_PREFIX + factoryCode + ":" + scheduleDate;
    }

    /**
     * 将单条下发数据转换为固定顺序的规范字符串。
     * 字段顺序固定且包含全部下发业务字段，任何字段变化都会改变指纹。
     */
    private String toCanonicalLine(LhScheduleResultIssue item) {
        return String.join("|",
                toText(item.getScheduleDate()),
                toText(item.getLhMachineCode()),
                toText(item.getLhMachineName()),
                toText(item.getLeftRightMold()),
                toText(item.getMaterialCode()),
                toText(item.getMesMaterialCode()),
                toText(item.getSpecCode()),
                toText(item.getSpecDesc()),
                toText(item.getDailyPlanQty()),
                toText(item.getClass1PlanQtySeq()),
                toText(item.getClass1PlanQty()),
                toText(item.getClass1AnalysisInput()),
                toText(item.getClass1Analysis()),
                toText(item.getClass1ExampleType()),
                toText(item.getClass1ExampleNo()),
                toText(item.getClass1PlanType()),
                toText(item.getClass2PlanQtySeq()),
                toText(item.getClass2PlanQty()),
                toText(item.getClass2AnalysisInput()),
                toText(item.getClass2Analysis()),
                toText(item.getClass2ExampleType()),
                toText(item.getClass2ExampleNo()),
                toText(item.getClass2PlanType()),
                toText(item.getClass3PlanQtySeq()),
                toText(item.getClass3PlanQty()),
                toText(item.getClass3AnalysisInput()),
                toText(item.getClass3Analysis()),
                toText(item.getClass3ExampleType()),
                toText(item.getClass3ExampleNo()),
                toText(item.getClass3PlanType()),
                toText(item.getLhTime()),
                toText(item.getLhBatchNo()),
                toText(item.getOrderNo()),
                toText(item.getCompanyCode()),
                toText(item.getFactoryCode()));
    }

    /**
     * 空值统一转换为空字符串，避免 null 与 "" 造成指纹抖动
     */
    private String toText(Object value) {
        return Objects.toString(value, "");
    }
}
