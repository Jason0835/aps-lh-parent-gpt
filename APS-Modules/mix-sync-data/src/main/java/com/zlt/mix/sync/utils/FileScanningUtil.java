package com.zlt.mix.sync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.ruoyi.common.utils.StringUtils;

/**
 * 文件扫描工具
 * 
 * @author hakimryan
 *
 */
public class FileScanningUtil {
	/**
	 * 按指定路径加载指定文件名的文件
	 * 
	 * @param isJar    是否从jar包加载
	 * @param path     文件所在的资源路径，以classpath作为根目录的相对路径
	 * @param fileName 文件名
	 * @return
	 * @throws IOException
	 */
	public static InputStream loadFile(boolean isJar, String path, String fileName) throws IOException {
		String filePath = StringUtils.join(path, fileName); // 拼接文件的完整路径
		return isJar ? loadFileInJar(filePath) : loadSystemFile(filePath); // 根据是否从jar包读取掉用不同的方法
	}

	/**
	 * 从文件系统读取文件
	 * 
	 * @param filePath 文件的资源路径，以classPath作为根目录的相对路径
	 * @return
	 * @throws IOException
	 */
	public static InputStream loadSystemFile(String filePath) throws IOException {
		URL url = StringUtils.class.getClassLoader().getResource(filePath); // 从classPath开始扫描文件
		if (url == null) {
			throw new FileNotFoundException(); // 扫描不到文件，则抛出异常
		}
		return new FileInputStream(url.getPath()); // 直接读取资源路径中的文件到流中
	}

	/**
	 * 从jar包读取文件
	 * 
	 * @param filePath 文件的资源路径，以classPath作为根目录的相对路径
	 * @return
	 * @throws IOException
	 */
	public static InputStream loadFileInJar(String filePath) throws IOException {
		URL url = StringUtils.class.getClassLoader().getResource(filePath); // 从classPath开始扫描文件
		if (url == null) {
			throw new FileNotFoundException(); // 扫描不到文件，则抛出异常
		}
		String realPath = url.getPath();
		String jarPath = new StringBuffer(realPath).deleteCharAt(realPath.lastIndexOf("!/")).toString(); // 截取文件路径，将多余的‘!’移除
		URL jarUrl = new URL(StringUtils.join("jar:", jarPath));
		JarURLConnection jarConnection = (JarURLConnection) jarUrl.openConnection();
		return jarConnection.getInputStream(); // 将文件内容读取到流中
	}

	/**
	 * 扫描jar包内指定路径的文件，并转换成流
	 * 
	 * @param jarPath  jar包路径
	 * @param filePath 文件全路径
	 * @return
	 * @throws IOException
	 */
	public static InputStream scanningJar(String jarPath, String filePath) throws IOException {
		try (JarFile jar = new JarFile(new File(jarPath))) {
			Enumeration<JarEntry> entries = jar.entries(); // 加载jar包内元素
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement(); // 遍历每一个元素
				String currentPath = entry.getName();
				if (!entry.isDirectory() && currentPath.equals(filePath)) { // 路径匹配完全匹配，且不是文件夹
					URL url = new URL(StringUtils.join("jar:file:", jarPath, "!/", currentPath));
					JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
					return jarConnection.getInputStream(); // 将文件内容读取到流中
				}
			}
			throw new FileNotFoundException(); // 扫描不到文件，则抛出异常
		}
	}
}
