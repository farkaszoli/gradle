/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal.tasks.generator;

import groovy.util.Node;
import groovy.util.XmlParser;
import org.gradle.api.internal.XmlTransformer;
import org.gradle.util.UncheckedException;

import java.io.*;

/**
 * A {@link PersistableConfigurationObject} which is stored in an XML file.
 */
public abstract class XmlPersistableConfigurationObject implements PersistableConfigurationObject {
    private final XmlTransformer xmlTransformer;
    private Node xml;

    protected XmlPersistableConfigurationObject(XmlTransformer xmlTransformer) {
        this.xmlTransformer = xmlTransformer;
    }

    public Node getXml() {
        return xml;
    }

    public void load(File inputFile) {
        try {
            FileReader reader = new FileReader(inputFile);
            try {
                load(reader);
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            throw UncheckedException.asUncheckedException(e);
        }
    }

    public void loadDefaults() {
        try {
            Reader reader = new InputStreamReader(getClass().getResourceAsStream(getDefaultResourceName()));
            try {
                load(reader);
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            throw UncheckedException.asUncheckedException(e);
        }
    }

    public void load(Reader reader) throws Exception {
        xml = new XmlParser().parse(reader);
        initFromXml(xml);
    }

    public void store(File outputFile) {
        try {
            Writer writer = new FileWriter(outputFile);
            try {
                store(writer);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw UncheckedException.asUncheckedException(e);
        }
    }

    public void store(Writer writer) {
        updateXml(xml);
        xmlTransformer.transform(xml, writer);
    }

    /**
     * Called immediately after the XML file has been read.
     */
    protected abstract void initFromXml(Node xml);

    protected abstract String getDefaultResourceName();

    /**
     * Called immediately before the XML file is to be written.
     */
    protected abstract void updateXml(Node xml);
}
