package com.figaf.plugin.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.jaxen.JaxenException;
import org.jaxen.dom4j.Dom4jXPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergey Klochkov
 */
@Slf4j
public abstract class XMLUtils {

    private static final Pattern XML_DECLARATION_PATTERN = Pattern.compile("^\\<\\?xml[ a-zA-Z0-9'\\.\"=\\-_]*\\?\\>");

    public static final String UTF8_BOM = "\uFEFF";

    public static String prettyPrintXML(byte[] xmlFile) {
        try {

            String xmlFileString = new String(xmlFile, StandardCharsets.UTF_8);

            if (xmlFileString.startsWith(UTF8_BOM)) {
                xmlFileString = xmlFileString.substring(1);
            }

            Matcher xmlFileMatcher = XML_DECLARATION_PATTERN.matcher(xmlFileString);
            boolean isContainXmlDeclaration = xmlFileMatcher.find();
            String xmlDeclaration = isContainXmlDeclaration
                ? xmlFileString.substring(xmlFileMatcher.start(), xmlFileMatcher.end())
                : "";

            if (StringUtils.isNoneBlank(xmlDeclaration)) {
                xmlDeclaration = xmlDeclaration + "\n";
            }

            List<String> splitted = new ArrayList<>(Arrays.asList(xmlFileString.split("[\n\r]")));
            CollectionUtils.transform(splitted, String::trim);
            CollectionUtils.filter(splitted, StringUtils::isNotBlank);
            String normilized = StringUtils.join(splitted.toArray());

            Source xmlInput = new StreamSource(new StringReader(normilized));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            String formattedXml = xmlOutput.getWriter().toString();
            Matcher formattedXmlMatcher = XML_DECLARATION_PATTERN.matcher(formattedXml);
            if (formattedXmlMatcher.find()) {
                formattedXml = formattedXmlMatcher.replaceFirst(xmlDeclaration);
            } else if (StringUtils.isNoneBlank(xmlDeclaration)) {
                formattedXml = xmlDeclaration + formattedXml;
            }
            return formattedXml;
        } catch (Exception ex) {
            log.error("Error occurred while formatting xml: " + ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }


}
