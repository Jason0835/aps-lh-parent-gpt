package com.zlt.mix.sync.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleMissingPluginsException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.sync.utils.FileScanningUtil;
import com.zlt.mix.sync.vo.MesDBConfig;
import com.zlt.mix.sync.vo.MixDBConfig;
import com.zlt.mix.sync.vo.SyncDataDbConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * kettle服务
 * 
 * @author hakimryan
 *
 */
@Slf4j
@Component
public class KettleService {
	@Resource
	private SyncDataLockService syncLockService;
	/**
	 * mes数据库配置
	 */
	@Resource
	private MesDBConfig mesDBConfig;
	/**
	 * 密炼数据库配置
	 */
	@Resource
	private MixDBConfig mixDBConfig;
	/**
	 * ktr文件是否再jar包中（用于区分开发环境与部署环境）
	 */
	@Value("${syncdata.kettle.jar}")
	private boolean isktrInJar;
	/**
	 * ktr文件扫描路径
	 */
	private String KTR_PATH = "kettle/";
	
	/**
	 * 已经加载过的ktr文件缓存起来，不用每次调用都重复加载
	 */
	private Map<String, TransMeta> transMetaCache = new HashMap<>();

	static {
		// 执行kettle任务前必须先初始化kettle运行环境，且只需要初始化一次，因此在类加载就需要直接初始化好
		try {
			KettleEnvironment.init();
		} catch (KettleException e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 
	 * 执行指定ktr文件
	 * 
	 * @param ktrName ktr文件名
	 * @throws IOException
	 * @throws KettleException
	 */
	public void excuteKettle(String ktrName) {
		try {
			syncLockService.addLock(ktrName); // 加锁
			TransMeta meta = this.getTransMeta(ktrName); // 获取kettle转换配置
			Trans trans = new Trans(meta);
			log.info(StringUtils.format("同步接口{}开始同步...", ktrName));
			trans.execute(null);
			trans.waitUntilFinished();
			log.info(StringUtils.format("同步接口{}同步结束", ktrName));
		} catch (KettleException e) {
			log.error(StringUtils.format("同步接口{}执行失败！！！", ktrName));
			throw new RuntimeException(e);
		} finally {
			syncLockService.releaseLock(ktrName); // 释放锁
		}
	}

	/**
	 * 获取kettle转换配置
	 * 
	 * @param ktrName ktr文件名
	 * @return
	 * @throws KettleException
	 */
	private TransMeta getTransMeta(String ktrName) throws KettleException {
		log.info(StringUtils.format("同步接口{}转换配置加载开始...", ktrName));
		TransMeta transMeta;
		if (transMetaCache.containsKey(ktrName)) {
			transMeta = transMetaCache.get(ktrName); // 优先从缓存加载
		} else {
			transMeta = this.loadTransMeta(ktrName); // 如果缓存没有，则从文件系统读取
			transMetaCache.put(ktrName, transMeta);
		}
		log.info(StringUtils.format("同步接口{}转换配置加载完成...", ktrName));
		return transMeta;
	}

	/**
	 * 加载ktr文件，并构建成为转换配置对象
	 * 
	 * @param ktrName ktr文件名
	 * @return
	 * @throws KettleXMLException
	 * @throws KettleMissingPluginsException
	 */
	private TransMeta loadTransMeta(String ktrName) throws KettleException {
		// 读取ktr文件，通过读取文件流 -> document -> transMeta进行转换
		try (InputStream input = FileScanningUtil.loadFile(isktrInJar, KTR_PATH, ktrName)) { // 根据路径 + 文件名扫描
			if (input == null) {
				return null;
			}
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input); // 转换为document
			TransMeta transMeta = new TransMeta();
			transMeta.loadXML(doc.getDocumentElement(), ktrName, null, null, true, new Variables(), null);
			this.loadDBConfig(transMeta); // 加载数据库配置
			return transMeta;
		} catch (IOException | SAXException | ParserConfigurationException e) {
			log.error(StringUtils.format("{}文件解析失败...", ktrName));
			throw new RuntimeException(e);
		}
	}

	/**
	 * 加载数据库配置
	 * 
	 * @param transMeta kettle转换配置
	 */
	private void loadDBConfig(TransMeta transMeta) {
		List<DatabaseMeta> dbMetas = transMeta.getDatabases();
		// 读取数据库配置
		for (DatabaseMeta dbMeta : dbMetas) {
			String metaName = dbMeta.getName();
			// 将数据库配置替换成配置文件的配置
			SyncDataDbConfig dbConfig = this.getDbConfig(metaName);
			// 有读取到配置就
			if (dbConfig != null) {
				dbMeta.setDBName(dbConfig.getDbName());
				dbMeta.setHostname(dbConfig.getHostName());
				dbMeta.setUsername(dbConfig.getUserName());
				dbMeta.setPassword(dbConfig.getPassword());
			}
		}
	}

	/**
	 * 通过名称获取数据库配置
	 */
	private SyncDataDbConfig getDbConfig(String dbName) {
		if (mixDBConfig.getLinkName().equals(dbName)) {
			return mixDBConfig;
		} else if (mesDBConfig.getLinkName().equals(dbName)) {
			return mesDBConfig;
		}
		return null;
	}
}
