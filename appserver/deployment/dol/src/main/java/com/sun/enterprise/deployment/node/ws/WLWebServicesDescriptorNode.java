/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.enterprise.deployment.node.ws;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.node.*;
import com.sun.enterprise.deployment.xml.TagNames;
import com.sun.enterprise.deployment.xml.WebServicesTagNames;
import java.util.Map;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.jvnet.hk2.annotations.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node representing weblogic-webservices root element in weblogic-webservices.xml
 *
 * @author Rama Pulavarthi
 */
@Service
public class WLWebServicesDescriptorNode extends AbstractBundleNode {

    public WLWebServicesDescriptorNode(WebServicesDescriptor descriptor) {
        this();
        parentDescriptor = descriptor;
    }
    
    public WLWebServicesDescriptorNode() {
        registerElementHandler(new XMLElement(WLWebServicesTagNames.WEB_SERVICE),
                WLWebServiceNode.class);
        registerElementHandler(new XMLElement(WLWebServicesTagNames.WEBSERVICE_SECURITY),
                WLUnSupportedNode.class);
        SaxParserHandler.registerBundleNode(this, WLWebServicesTagNames.WEB_SERVICES);
    }

    private final static XMLElement ROOT_ELEMENT = new XMLElement(WLWebServicesTagNames.WEB_SERVICES);

    private final static String SCHEMA_ID = WLDescriptorConstants.WL_WEBSERVICES_XML_SCHEMA;
    private final static String SPEC_VERSION = "1.0";
    private final static List<String> systemIDs = initSystemIDs();

    private static List<String> initSystemIDs() {
        List<String> systemIDs = new ArrayList<String>();
        systemIDs.add(SCHEMA_ID);
        return Collections.unmodifiableList(systemIDs);

    }

    private WebServicesDescriptor parentDescriptor;

    /**
     * @return the DOCTYPE of the XML file
     */
    public String getDocType() {
        return null;
    }

    /**
     * @return the SystemID of the XML file
     */
    public String getSystemID() {
        return SCHEMA_ID;
    }

    /**
     * @return the list of SystemID of the XML schema supported
     */
    public List<String> getSystemIDs() {
        return systemIDs;
    }

    @Override
    public String registerBundle(Map<String, String> publicIDToSystemIDMapping) {
        return ROOT_ELEMENT.getQName();
    }

    @Override
    public Map<String, Class> registerRuntimeBundle(Map<String, String> publicIDToSystemIDMapping, final Map<String, List<Class>> versionUpgrades) {
        return Collections.EMPTY_MAP;
    }

    
    /**
     * @return the complete URL for J2EE schemas
     */
    protected String getSchemaURL() {
        return WLDescriptorConstants.WL_WEBSERVICES_SCHEMA_LOCATION;
    }

    /**
     * @return the XML tag associated with this XMLNode
     */
    protected XMLElement getXMLRootTag() {
        return ROOT_ELEMENT;
    }

    /*
    see setAttributeValue below
    public void setElementValue(XMLElement element, String value) {
        if (TagNames.VERSION.equals(element.getQName())) {
            bundleDescriptor.getWebServices().setSpecVersion(value);
        } else super.setElementValue(element, value);
    }
    */

    @Override
    protected boolean setAttributeValue(XMLElement elementName, XMLElement attributeName, String value) {
        // we do not support id attribute for the moment
        if (attributeName.getQName().equals(TagNames.ID)) {
            return true;
        }

        if (TagNames.VERSION.equals(attributeName.getQName()) && SPEC_VERSION.equals(value)) {
            return true;
        }

        return false;
    }

    @Override
    public XMLNode getHandlerFor(XMLElement element) {
        if (WLWebServicesTagNames.WEBSERVICE_SECURITY.equals(element.getQName())) {
            throw new UnsupportedConfigurationException(element + " configuration in weblogic-webservices.xml is not supported.");
        } else {
            return super.getHandlerFor(element);
        }

    }

    @Override
    public void addDescriptor(Object descriptor) {
        //None of the sub nodes should call addDescriptor() on this node.
        // as this configuration only supplements webservices.xml configuration and
        // does not create new web services.
        //DOLUtils.getDefaultLogger().info("Warning: WLWebServiceDescriptorNode.addDescriptor() should not have been called by" + descriptor.toString());

    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    public WebServicesDescriptor getDescriptor() {
        return parentDescriptor;
    }

    public Node writeDescriptor(Node parent, RootDeploymentDescriptor descriptor) {
        Node bundleNode;
        if (getDocType() == null) {
            // we are using schemas for this DDs
            bundleNode = appendChildNS(parent, getXMLRootTag().getQName(), WLDescriptorConstants.WL_WEBSERVICES_XML_NS);

            addBundleNodeAttributes((Element) bundleNode, descriptor);
        } else {
            bundleNode = appendChild(parent, getXMLRootTag().getQName());
        }

        //TODO is this needed?
        // appendTextChild(bundleNode, TagNames.MODULE_NAME, descriptor.getModuleDescriptor().getModuleName());

        // description, display-name, icons...
        writeDisplayableComponentInfo(bundleNode, descriptor);

        if (descriptor instanceof WebServicesDescriptor) {
            WLWebServiceNode wsNode = new WLWebServiceNode();
            for(WebService next : ((WebServicesDescriptor)descriptor).getWebServices()) {
                wsNode.writeDescriptor(bundleNode, WebServicesTagNames.WEB_SERVICE,next);
            }
        }
        return bundleNode;
    }

    @Override
    protected void addBundleNodeAttributes(Element bundleNode, RootDeploymentDescriptor descriptor) {
        String schemaLocation;
        // the latest connector schema still use j2ee namespace
        bundleNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", WLDescriptorConstants.WL_WEBSERVICES_XML_NS);
        bundleNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:j2ee", TagNames.J2EE_NAMESPACE);

        schemaLocation = WLDescriptorConstants.WL_WEBSERVICES_XML_NS + " " + getSchemaURL();
        schemaLocation = schemaLocation+ " "+ TagNames.J2EE_NAMESPACE + " " + "http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd";
        bundleNode.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", W3C_XML_SCHEMA_INSTANCE);

        // add all custom global namespaces
        addNamespaceDeclaration(bundleNode, descriptor);
        String clientSchemaLocation = descriptor.getSchemaLocation();
        if (clientSchemaLocation != null) {
            schemaLocation = schemaLocation + " " + clientSchemaLocation;
        }
        bundleNode.setAttributeNS(W3C_XML_SCHEMA_INSTANCE, SCHEMA_LOCATION_TAG, schemaLocation);
        bundleNode.setAttribute(TagNames.VERSION, getSpecVersion());

    }

    /**
     * @return the default spec version level this node complies to
     */
    public String getSpecVersion() {
        return SPEC_VERSION;
    }

}