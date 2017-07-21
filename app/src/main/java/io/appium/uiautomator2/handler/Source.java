package io.appium.uiautomator2.handler;

import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.appium.uiautomator2.common.exceptions.UiAutomator2Exception;
import io.appium.uiautomator2.handler.request.SafeRequestHandler;
import io.appium.uiautomator2.http.AppiumResponse;
import io.appium.uiautomator2.http.IHttpRequest;
import io.appium.uiautomator2.server.WDStatus;
import io.appium.uiautomator2.utils.Logger;
import io.appium.uiautomator2.utils.ReflectionUtils;
import io.appium.uiautomator2.utils.XMLHierarchy;

import static io.appium.uiautomator2.utils.Device.getUiDevice;

/**
 * Get page source. Return as string of XML doc
 */
public class Source extends SafeRequestHandler {

    public Source(String mappedUri) {
        super(mappedUri);
    }

    @Override
    public AppiumResponse safeHandle(IHttpRequest request) {

        String xmlString = null;

        // 先尝试使用 getUiDevice().dumpWindowHierarchy(baos);
        try {
            ReflectionUtils.clearAccessibilityCache();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            getUiDevice().dumpWindowHierarchy(baos);
            xmlString = baos.toString("UTF-8");
            baos.close();
        } catch (Exception e) {
            Logger.error("Exception while performing UiDevice.dumpWindowHierarchy to UTF-8 string ", e);
        }

        if ( null != xmlString && xmlString.length() > 0 ) {
            return new AppiumResponse(getSessionId(request), WDStatus.SUCCESS, xmlString);
        } else {
            try {
                final Document doc = (Document) XMLHierarchy.getFormattedXMLDoc();
                final TransformerFactory tf = TransformerFactory.newInstance();
                final StringWriter writer = new StringWriter();
                Transformer transformer = tf.newTransformer();
                transformer.transform(new DOMSource(doc), new StreamResult(writer));
                xmlString = writer.getBuffer().toString();
                return new AppiumResponse(getSessionId(request), WDStatus.SUCCESS, xmlString);

            } catch (final TransformerConfigurationException e) {
                Logger.error("Unable to handle the request:" + e);
                return new AppiumResponse(getSessionId(request), WDStatus.UNKNOWN_ERROR, "Something went terribly wrong while converting xml document to string:" + e);
            } catch (final TransformerException e) {
                Logger.error("Unable to handle the request:" + e);
                return new AppiumResponse(getSessionId(request), WDStatus.UNKNOWN_ERROR, "Could not parse xml hierarchy to string: " + e);
            } catch (UiAutomator2Exception e) {
                Logger.error("Exception while performing LongPressKeyCode action: ", e);
                return new AppiumResponse(getSessionId(request), WDStatus.UNKNOWN_ERROR, e);
            }
        }

    }
}
