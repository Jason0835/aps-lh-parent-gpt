package com.zlt.aps.job.conf;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import com.ruoyi.common.core.utils.ip.IpUtils;

/**
 * aps-job服务发现配置<br/>
 * 额外将合并后的服务注册到nacos中
 *
 */
@Configuration
public class ApsJobDiscoveryConfig implements SmartInitializingSingleton, ApplicationListener<ContextClosedEvent> {
	@Value("${server.port:9203}")
	Integer prot;

	@Autowired
	ApplicationContext context;

	@Autowired
	NacosServiceRegistry registry;

	NacosRegistration authRegistration;
	NacosRegistration billRegistration;

	@Override
	public void afterSingletonsInstantiated() {
		// 注册kettle服务
		authRegistration = this.registService("kettle");
		registry.register(authRegistration);
	}

	/**
	 * 将当前服务注册为指定服务名
	 * 
	 * @param serviceName
	 * @return
	 */
	private NacosRegistration registService(String serviceName) {
		NacosDiscoveryProperties properties = new NacosDiscoveryProperties();
		properties.setNamespace(serviceName);
		properties.setPort(prot);
		properties.setIp(IpUtils.getHostIp());
		properties.setServerAddr(serviceName);
		properties.setService(serviceName);
		Map<String, String> metadata = new HashMap<>();
		metadata.put("management.endpoints.web.base-path", "/metrics");
		metadata.put("preserved.register.source", "SPRING_CLOUD");
		properties.setMetadata(metadata);
		NacosRegistration registration = new NacosRegistration(null, properties, context);
		registration.setPort(prot);
		return registration;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		registry.deregister(authRegistration);
		registry.deregister(billRegistration);
	}
}
