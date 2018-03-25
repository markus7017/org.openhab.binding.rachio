/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Scanner;

import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openhab.binding.rachio.internal.RachioHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Main OSGi service and HTTP servlet for Rachio Event Webhook.
 *
 * @author Markus Michels (markus7017) - Initial distribution (derived from Netamo binding, thanks)
 */
@Component(service = HttpServlet.class, configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true)
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = -4654253998990066051L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);
    private final Gson gson = new Gson();

    private HttpService httpService;
    private RachioHandlerFactory rachioHandlerFactory;

    /**
     * OSGi activation callback.
     *
     * @param config Service config.
     */
    @Activate
    protected void activate(Map<String, Object> config) {
        try {
            httpService.registerServlet(WEBHOOK_PATH, this, null, httpService.createDefaultHttpContext());
            logger.info("Started Rachio Webhook servlet at {}", WEBHOOK_PATH);
        } catch (ServletException | NamespaceException e) {
            logger.error("Could not start Rachio Webhook servlet: {}", e.getMessage(), e);
        }
    }

    /**
     * OSGi deactivation callback.
     */
    @Deactivate
    protected void deactivate() {
        httpService.unregister(WEBHOOK_PATH);
        logger.info("RachioWebHook: Servlet stopped");
    }

    @SuppressWarnings("null")
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        String data = inputStreamToString(request);
        try {
            // Fix malformed API v3 Event JSON
            data = data.replace("\"{", "{");
            data = data.replace("}\"", "}");
            data = data.replace("\\", "");

            String ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            String path = request.getRequestURI();
            logger.trace("RachioWebHook: Reqeust from {}:{}{} ({}, {})", ipAddress, request.getRemotePort(), path,
                    request.getRemoteHost(), request.getProtocol());
            if (!path.equalsIgnoreCase(WEBHOOK_PATH)) {
                logger.error("RachioWebHook: Invalid request received - path = {}", path);
                return;
            }
            // String ipFilter = rachioHandlerFactory.getIpFilter();
            // if (!RachioNetwork.ipInSubnet(ipAddress, ipFilter)) {
            // logger.info("RachioWebHook: IP address '{}' does not match the configured filter '{}', data='{}'",
            // ipAddress, ipFilter, data);
            // // return;
            // }

            X509Certificate cert = extractCertificate(request);
            if (cert != null) {
                X500Principal principal = cert.getIssuerX500Principal();
                logger.debug("RachioEvent: Certificate from '{}'", principal.getName());
            }

            if (data != null) {
                logger.trace("RachioWebHook: Data='{}'", data);
                RachioEvent event = gson.fromJson(data, RachioEvent.class);
                if ((event != null) && (rachioHandlerFactory != null)) {
                    logger.trace("RachioEvent {}.{} for device '{}': {}", event.category, event.type, event.deviceId,
                            event.summary);

                    event.setRateLimit(request.getHeader(RACHIO_JSON_RATE_LIMIT),
                            request.getHeader(RACHIO_JSON_RATE_REMAINING), request.getHeader(RACHIO_JSON_RATE_RESET));

                    if (!rachioHandlerFactory.webHookEvent(event)) {
                        logger.debug("RachioWebHook: Event-JSON='{}'", data);
                    }

                    return;
                }
                logger.debug("RachioWebHook: Unable to process inbound request, data='{}'", data);
            }
        } catch (Exception e) {
            if (data != null) {
                logger.error("RachioWebHook: Exception processing callback: {}, data='{}'", e.getMessage(), data);
            } else {
                logger.error("RachioWebHook: Exception processing callback: {}", e.getMessage());

            }
        } finally {
            setHeaders(resp);
            resp.getWriter().write("");
        }
    } // service()

    @SuppressWarnings("resource")
    private String inputStreamToString(HttpServletRequest request) throws IOException {
        Scanner scanner = new Scanner(request.getInputStream()).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private void setHeaders(HttpServletResponse response) {
        response.setCharacterEncoding(WEBHOOK_CHARSET);
        response.setContentType(WEBHOOK_APPLICATION_JSON);
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    }

    private X509Certificate extractCertificate(HttpServletRequest req) {
        X509Certificate[] certs = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
        if (null != certs && certs.length > 0) {
            return certs[0];
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setRachioHandlerFactory(RachioHandlerFactory rachioHandlerFactory) {
        if (rachioHandlerFactory != null) {
            this.rachioHandlerFactory = rachioHandlerFactory;
            logger.debug("RachioWebHook: HandlerFactory bound");
        }
    }

    public void unsetRachioHandlerFactory(RachioHandlerFactory rachioHandlerFactory) {
        this.rachioHandlerFactory = null;
    }

    @Reference
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    public static String replace(String string, String[] toFind, String[] toReplace) {
        if (toFind.length != toReplace.length) {
            throw new IllegalArgumentException("Arrays must be of the same length.");
        }
        for (int i = 0; i < toFind.length; i++) {
            string = string.replace(toFind[i], toReplace[i]);
        }
        return string;
    }
} // RachioWebHookServlet
