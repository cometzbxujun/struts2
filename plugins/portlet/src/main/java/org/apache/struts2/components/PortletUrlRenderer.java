/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts2.components;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.apache.struts2.dispatcher.mapper.ActionMapper; 
import org.apache.struts2.StrutsException;
import org.apache.struts2.portlet.context.ContextUtil;
import org.apache.struts2.portlet.context.PortletActionContext;
import org.apache.struts2.portlet.util.PortletUrlHelper;
import org.apache.struts2.portlet.util.UrlHelper;
import org.apache.commons.lang.xwork.StringUtils;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;

/**
 * Implementation of the {@link UrlRenderer} interface that renders URLs for portlet environments.
 * 
 * @see UrlRenderer
 *
 */
public class PortletUrlRenderer implements UrlRenderer {
	
	/**
	 * The servlet renderer used when not executing in a portlet context.
	 */
	private UrlRenderer servletRenderer = null;
	
	UrlHelper urlHelper = new PortletUrlHelper();
	
	ContextUtil context = new PortletActionContext();
	
	public PortletUrlRenderer() {
		this.servletRenderer = new ServletUrlRenderer();
	}

	@Inject
	public void setActionMapper(ActionMapper actionMapper) {
		servletRenderer.setActionMapper(actionMapper);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void renderUrl(Writer writer, UrlProvider urlComponent) {
		if(context.getPortletContext() == null || "none".equalsIgnoreCase(urlComponent.getPortletUrlType())) {
			servletRenderer.renderUrl(writer, urlComponent);
		}
		else {
			String action = null;
			if(urlComponent.getAction() != null) {
				action = urlComponent.findString(urlComponent.getAction());
			}
			/*String scheme = urlComponent.getHttpServletRequest().getScheme();

			if (urlComponent.getScheme() != null) {
				scheme = urlComponent.getScheme();
			} */

			String result;
			urlComponent.setNamespace(urlComponent.determineNamespace(urlComponent.getNamespace(), urlComponent.getStack(), urlComponent.getHttpServletRequest()));
			@SuppressWarnings("unchecked")
			Map<String,Object> parameters = urlComponent.getParameters();
			if (onlyActionSpecified(urlComponent)) {
				result = urlHelper.buildUrl(action, urlComponent.getNamespace(), urlComponent.getMethod(), parameters, urlComponent.getPortletUrlType(),
                        urlComponent.getPortletMode(), urlComponent.getWindowState());
			} else if(onlyValueSpecified(urlComponent)){
			    ComponentUrlProvider provider = (ComponentUrlProvider)urlComponent;
				result = urlHelper.buildResourceUrl(urlComponent.getValue(), parameters,provider.isEscapeAmp());
			}
			else {
				result = createDefaultUrl(urlComponent);
			}
            String anchor = urlComponent.getAnchor();
			if (StringUtils.isNotEmpty(anchor)) {
				result += '#' + urlComponent.findString(anchor);
			}

			String var = urlComponent.getVar();

			if (var != null) {
				urlComponent.putInContext(result);

				// add to the request and page scopes as well
				urlComponent.getHttpServletRequest().setAttribute(var, result);
			} else {
				try {
					writer.write(result);
				} catch (IOException e) {
					throw new StrutsException("IOError: " + e.getMessage(), e);
				}
			}
		}
	}

	private String createDefaultUrl(UrlProvider urlComponent) {
		String result;
		@SuppressWarnings("unchecked")
		Map<String,Object> parameters = urlComponent.getParameters();
		ActionInvocation ai = (ActionInvocation)urlComponent.getStack().getContext().get(
				ActionContext.ACTION_INVOCATION);
		String action = ai.getProxy().getActionName();
		result = urlHelper.buildUrl(action, urlComponent.getNamespace(), urlComponent.getMethod(), parameters,
                urlComponent.getPortletUrlType(), urlComponent.getPortletMode(), urlComponent.getWindowState());
		return result;
	}

	private boolean onlyValueSpecified(UrlProvider urlComponent) {
		return urlComponent.getValue() != null && urlComponent.getAction() == null;
	}

	private boolean onlyActionSpecified(UrlProvider urlComponent) {
		return urlComponent.getValue() == null && urlComponent.getAction() != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderFormUrl(Form formComponent) {
		if(context.getPortletContext() == null) {
			servletRenderer.renderFormUrl(formComponent);
		}
		else {
			String namespace = formComponent.determineNamespace(formComponent.namespace, formComponent.getStack(),
					formComponent.request);
			String action = null;
			if (formComponent.action != null) {
				action = formComponent.findString(formComponent.action);
			}
			else {
				ActionInvocation ai = (ActionInvocation) formComponent.getStack().getContext().get(ActionContext.ACTION_INVOCATION);
				action = ai.getProxy().getActionName();
			}
			String type = "action";
			if (StringUtils.isNotEmpty(formComponent.method)) {
				if ("GET".equalsIgnoreCase(formComponent.method.trim())) {
					type = "render";
				}
			}
			if (action != null) {
				@SuppressWarnings("unchecked")
				Map<String,Object> parameters = formComponent.getParameters();
				String result = urlHelper.buildUrl(action, namespace, null,
						parameters, type, formComponent.portletMode, formComponent.windowState);
				formComponent.addParameter("action", result);


				// name/id: cut out anything between / and . should be the id and
				// name
				String id = formComponent.getId();
				if (id == null) {
					int slash = action.lastIndexOf('/');
					int dot = action.indexOf('.', slash);
					if (dot != -1) {
						id = action.substring(slash + 1, dot);
					} else {
						id = action.substring(slash + 1);
					}
					formComponent.addParameter("id", formComponent.escape(id));
				}
			}
		}
		
	}

	public void beforeRenderUrl(UrlProvider urlComponent) {
		if(context.getPortletContext() == null) {
			servletRenderer.beforeRenderUrl(urlComponent);
		}
	}

	public void setServletRenderer(UrlRenderer nonPortletRenderer) {
		this.servletRenderer = nonPortletRenderer;
		
	}

}
