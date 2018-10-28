/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 将网络地址转换为本地地址，并返回对应 scheme Resources
 * http://www.springframework.org/schema/beans/spring-beans.xsd
 *     ==> org/springframework/beans/factory/xml/spring-beans.xsd
 *
 * EntityResolver implementation that tries to resolve entity references
 * through a {@link org.springframework.core.io.ResourceLoader} (usually,
 * relative to the resource base of an ApplicationContext), if applicable.
 * Extends {@link DelegatingEntityResolver} to also provide DTD and XSD lookup.
 *
 * 允许使用标准 XML 实体将 XML 片段包含到应用程序上下文定义中，例如将大型XML文件拆分为各种模块。
 * include 路径可以像往常一样相对于应用程序上下文的资源库，
 * 而不是相对于 JVM 工作目录（ XML 解析器的默认值）。
 * <p>Allows to use standard XML entities to include XML snippets into an
 * application context definition, for example to split a large XML file
 * into various modules. The include paths can be relative to the
 * application context's resource base as usual, instead of relative
 * to the JVM working directory (the XML parser's default).
 *
 * <p>Note: In addition to relative paths, every URL that specifies a
 * file in the current system root, i.e. the JVM working directory,
 * will be interpreted relative to the application context too.
 *
 * @author Juergen Hoeller
 * @since 31.07.2003
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 */
public class ResourceEntityResolver extends DelegatingEntityResolver {

	private static final Log logger = LogFactory.getLog(ResourceEntityResolver.class);

	private final ResourceLoader resourceLoader;


	/**
	 * Create a ResourceEntityResolver for the specified ResourceLoader
	 * (usually, an ApplicationContext).
	 * @param resourceLoader the ResourceLoader (or ApplicationContext)
	 * to load XML entity includes with
	 */
	public ResourceEntityResolver(ResourceLoader resourceLoader) {
		super(resourceLoader.getClassLoader());
		this.resourceLoader = resourceLoader;
	}


	@Override
	@Nullable
	public InputSource resolveEntity(String publicId, @Nullable String systemId) throws SAXException, IOException {
		// 将网络地址转为本地地址获取本地 scheme Resource
		InputSource source = super.resolveEntity(publicId, systemId);
		if (source == null && systemId != null) {
			String resourcePath = null;
			try {
				String decodedSystemId = URLDecoder.decode(systemId, "UTF-8");
				String givenUrl = new URL(decodedSystemId).toString();
				String systemRootUrl = new File("").toURI().toURL().toString();
				// Try relative to resource base if currently in system root.
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			}
			catch (Exception ex) {
				// Typically a MalformedURLException or AccessControlException.
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve XML entity [" + systemId + "] against system root URL", ex);
				}
				// No URL (or no resolvable URL) -> try relative to resource base.
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Trying to locate XML entity [" + systemId + "] as resource [" + resourcePath + "]");
				}
				Resource resource = this.resourceLoader.getResource(resourcePath);
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
				if (logger.isDebugEnabled()) {
					logger.debug("Found XML entity [" + systemId + "]: " + resource);
				}
			}
		}
		return source;
	}

}
