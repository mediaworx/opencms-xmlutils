/**
 * Copyright (c) 2014 mediaworx berlin AG (http://mediaworx.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about mediaworx berlin AG, please see the
 * company website: http://mediaworx.com
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 * If not, see <http://www.gnu.org/licenses/>
 */

package com.mediaworx.xmlutils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class used to create helper objects that simplify parsing and modifying XML files. To retrieve single nodes or
 * multiple nodes from the XML XPath is used, see the 
 * <a href="http://docs.oracle.com/javase/6/docs/api/javax/xml/xpath/XPath.html">Java API documentation on XPath</a>
 * for more information.
 *
 * @author Kai Widmann, mediaworx Berlin AG
 */
public class XmlHelper {

	private static final Logger LOG = LoggerFactory.getLogger(XmlHelper.class);

	/** Default encoding that is used if no encoding is given */
	public static final String DEFAULT_ENCODING = "UTF-8";

	/** DocumentBuilder used to parse xml files */
	private DocumentBuilder builder;

	/** factory used to create new XPath objects */
	XPathFactory xPathfactory;


	/**
	 * Creates and initializes a new XmlHelper instance.
	 * @throws ParserConfigurationException if for some reason the DocumentBuilder used to parse the XML can't be
	 *                                      initialized
	 */
	public XmlHelper() throws ParserConfigurationException {
		builder = getNonValidatingDocumentBuilder();
		xPathfactory = XPathFactory.newInstance();
	}

	/**
	 * Creates and returns a document builder that is configured with the following options:
	 * <ul>
	 *     <li>don't validate</li>
	 *     <li>ignore comments</li>
	 *     <li>ignore content whitespace</li>
	 *     <li>convert CDATA nodes to text nodes</li>
	 *     <li>don't perform namespace processing</li>
	 *     <li>ignore DTDs</li>
	 * </ul>
	 * @return the DocumentBuilder
	 * @throws ParserConfigurationException if for some reason the DocumentBuilder used to parse the XML can't be
	 *                                      initialized
	 */
	private DocumentBuilder getNonValidatingDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setValidating(false);
		documentBuilderFactory.setIgnoringComments(true);
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilderFactory.setCoalescing(true);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return documentBuilderFactory.newDocumentBuilder();
	}

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text noes containing whitespace only are removed.
	 *
	 * @param path the XML file's path
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(String path) throws IOException, SAXException {
		return parseFile(path, null, DEFAULT_ENCODING);
	}

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text noes containing whitespace only are removed. If a replacement map is provided, each key in the map is
	 * replaced by the corresponding value in the file's content.
	 *
	 * @param path the XML file's path
	 * @param replacements Map containing replacement strings, key: string to be replaced, value: replacement string (if the map is null, no replacements are made)
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(String path, Map<String, String> replacements) throws IOException, SAXException {
		return parseFile(path, replacements, DEFAULT_ENCODING);
	}

	/**
	 * Parses the XML content of the file at the given path using the given encoding. Empty text nodes or
	 * text noes containing whitespace only are removed. If a replacement map is provided, each key in the map is
	 * replaced by the corresponding value in the file's content.
	 *
	 *
	 * @param path the XML file's path
	 * @param replacements Map containing replacement strings, key: string to be replaced, value: replacement string (if the map is null, no replacements are made)
	 * @param encoding  the encoding to be used to parse the file (must be a valid encoding like "UTF-8")
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(String path, Map<String, String> replacements, String encoding) throws IOException, SAXException {
		return parseFile(new File(path), replacements, encoding);
	}

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text noes containing whitespace only are removed. If a replacement map is provided, each key in the map is
	 * replaced by the corresponding value in the file's content.
	 *
	 * @param file the file containing the XML
	 * @param replacements Map containing replacement strings, key: string to be replaced, value: replacement string (if the map is null, no replacements are made)
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(File file, Map<String, String> replacements) throws IOException, SAXException {
		return parseFile(file, replacements, DEFAULT_ENCODING);
	}

	/**
	 * Parses the XML content of the file at the given path using the default encoding (UTF-8). Empty text nodes or
	 * text nodes containing whitespace only are removed. If a replacement map is provided, each key in the map is
	 * replaced by the corresponding value in the file's content.
	 *
	 * @param file the file containing the XML
	 * @param replacements Map containing replacement strings, key: string to be replaced, value: replacement string (if the map is null, no replacements are made)
	 * @param encoding  the encoding to be used to parse the file (must be a valid encoding like "UTF-8")
	 * @return  the parsed XML document
	 * @throws IOException  if there's a problem accessing the file
	 * @throws SAXException if the file content can't be parsed
	 */
	public Document parseFile(File file, Map<String, String> replacements, String encoding) throws IOException, SAXException {
		String fileContent = readFile(file, encoding);

		if (replacements != null) {
			for (String search : replacements.keySet()) {
				String replace = replacements.get(search);
				fileContent = fileContent.replaceAll(Pattern.quote(search), replace);
			}
		}
		StringReader reader = new StringReader(fileContent);
		Document document = builder.parse(new InputSource(reader));
		cleanEmptyTextNodes(document);
		return document;
	}

	/**
	 * Helper method to read the file's content into a String
	 * @param file the file to be read
	 * @param encoding  the encoding to be used to read the file (must be a valid encoding like "UTF-8")
	 * @return String containing the file's content
	 * @throws IOException if there's a problem accessing the file
	 */
	private static String readFile(File file, String encoding) throws IOException {
		InputStreamReader in = new InputStreamReader(new FileInputStream(file), encoding);
		BufferedReader reader = new BufferedReader(in);
		StringBuilder fileContent = new StringBuilder();
		String line = reader.readLine();
        while(line != null){
	        fileContent.append(line).append('\n');
            line = reader.readLine();
        }
		return fileContent.toString();
	}

	/**
	 * Retrieves the NodeList for the given XPath from the given ancestor Node.
	 * @param ancestorNode the node from which the NodeList is to be read
	 * @param xPath        the XPath (relative to the ancestor node)
	 * @return the NodeList for the given XPath
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist)
	 */
	public NodeList getNodeListForXPath(Node ancestorNode, String xPath) throws XPathExpressionException {
		XPath xpath = xPathfactory.newXPath();
		return (NodeList)xpath.evaluate(xPath, ancestorNode, XPathConstants.NODESET);
	}

	/**
	 * Retrieves a single node at a given XPath from the given ancestor node.
	 * @param ancestorNode the node from which the Node is to be read
	 * @param xPath        the XPath (relative to the ancestor node)
	 * @return the Node for the given XPath
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist or
	 *                                  because it does not point to a single node)
	 */
	public Node getSingleNodeForXPath(Node ancestorNode, String xPath) throws XPathExpressionException {
		XPath xpath = xPathfactory.newXPath();
		return (Node)xpath.evaluate(xPath, ancestorNode, XPathConstants.NODE);
	}

	/**
	 * Retrieves the String content of a node at the given XPath.
	 * @param ancestorNode  the parent node from which the Node content is to be read
	 * @param xPath         the XPath (relative to the ancestor node)
	 * @return the String content of the node at the given XPath
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist or
	 *                                  because it does not point to a single node)
	 */
	public String getStringValueForXpath(Node ancestorNode, String xPath) throws XPathExpressionException {
		return getSingleNodeForXPath(ancestorNode, xPath).getFirstChild().getNodeValue();
	}

	/**
	 * Retrieves the content of the node at the given XPath as int.
	 * @param ancestorNode  the parent node from which the Node content is to be read
	 * @param xPath         the XPath (relative to the ancestor node)
	 * @return the content of the node at the given XPath as int
	 * @throws XPathExpressionException if the given XPath can't be evaluated (e.g. because it does not exist or
	 *                                  because it does not point to a single node)
	 * @throws NumberFormatException    if the content of the node at the XPath can't be converted to int
	 */
	public int getIntValueForXpath(Node ancestorNode, String xPath) throws XPathExpressionException, NumberFormatException {
		return Integer.parseInt(getStringValueForXpath(ancestorNode, xPath));
	}

	/**
	 * Appends a new child node to a parent node.
	 * @param parent    the parent node
	 * @param newChild  the child node to be appended
	 */
	public void appendNode(Node parent, Node newChild) {
		Node toBeImported = newChild instanceof Document ? ((Document) newChild).getDocumentElement() : newChild;
		Node importedNode = parent.getOwnerDocument().importNode(toBeImported, true);
		parent.appendChild(importedNode);
	}

	/**
	 * parses and appends the content of a file as a child node to the given parent node
	 * @param parent            the parent node
	 * @param newChildFilePath  the path to the file whose content is to be added as a child node
	 * @param replacements      Map containing replacement strings, key: string to be replaced, value: replacement string
	 * @throws IOException      if there's a problem accessing the file
	 * @throws SAXException     if the file can't be parsed
	 */
	public void appendFileAsNode(Node parent, String newChildFilePath, Map<String, String> replacements) throws IOException, SAXException {
		Document newChild = parseFile(newChildFilePath, replacements);
		appendNode(parent, newChild);
	}

	/**
	 * Converts the document to a formatted XML String (indentation level is 4) using default encoding (UTF-8).
	 * @param document      The document to be converted to String
	 * @param cdataElements String array containing the names of all elements that are to be added within CDATA sections
	 * @return  the String representation of the given Document
	 */
	public String getXmlStringFromDocument(Document document, String[] cdataElements) {
		return getXmlStringFromDocument(document, cdataElements, DEFAULT_ENCODING);
	}

	/**
	 * Converts the document to a formatted XML String (indentation level is 4) using the given encoding.
	 * @param document      The document to be converted to String
	 * @param cdataElements String array containing the names of all elements that are to be added within CDATA sections
	 * @param encoding      encoding to be used (added in the XML declaration)
	 * @return  the String representation of the given Document
	 */
	public String getXmlStringFromDocument(Document document, String[] cdataElements, String encoding) {
		cleanEmptyTextNodes(document);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
		}
		catch (TransformerConfigurationException e) {
			LOG.error("Exception configuring the XML transformer", e);
			return "";
		}
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		if (cdataElements != null && cdataElements.length > 0) {
			String cdataElementsJoined = StringUtils.join(cdataElements, ' ');
			transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, cdataElementsJoined);
		}
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		OutputStream out = new ByteArrayOutputStream();
		try {
			transformer.transform(new DOMSource(document), new StreamResult(out));
		}
		catch (TransformerException e) {
			LOG.error("Exception transforming the XML document to String", e);
		}
		finally {
			try {
				out.close();
			}
			catch (IOException e) {
				// it seems the output stream was closed already
				LOG.warn("Exception closing the output stream", e);
			}
		}
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(encoding).append("\"?>\n");
		xml.append(out.toString());
		return xml.toString();
	}

	/**
	 * Removes text nodes that are empty or contain whitespace only if the parent node has at least one child of any
	 * of the following types: ELEMENT, CDATA, COMMENT. This is used to improve the XML format when using a transformer
	 * to do the formatting (whitespace nodes are interfering with indentation and line breaks).
	 * This method was modeled after a method by "user2401669" found on
	 * <a href="http://stackoverflow.com/questions/16641835/strange-xml-indentation">StackOverflow</a>.
	 */
	public static void cleanEmptyTextNodes(Node parentNode) {
		boolean removeEmptyTextNodes = false;

		Node childNode = parentNode.getFirstChild();
		while (childNode != null) {
			short nodeType = childNode.getNodeType();

			if (nodeType == Node.ELEMENT_NODE || nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.COMMENT_NODE) {
				removeEmptyTextNodes = true;
				if (nodeType == Node.ELEMENT_NODE) {
					cleanEmptyTextNodes(childNode); // recurse into subtree
				}
			}
			childNode = childNode.getNextSibling();
		}

		if (removeEmptyTextNodes) {
			removeEmptyTextNodes(parentNode);
		}
	}

	/**
	 * Removes all empty or whitespace only text nodes from the given parent node.
	 * @param parentNode    the parent node to be cleared of empty or whitespace only text nodes
	 */
	private static void removeEmptyTextNodes(Node parentNode) {
		Node childNode = parentNode.getFirstChild();
		while (childNode != null) {
			// grab the "nextSibling" before the child node is removed
			Node nextChild = childNode.getNextSibling();

			short nodeType = childNode.getNodeType();
			if (nodeType == Node.TEXT_NODE) {
				boolean containsOnlyWhitespace = childNode.getNodeValue().trim().isEmpty();
				if (containsOnlyWhitespace) {
					parentNode.removeChild(childNode);
				}
			}
			childNode = nextChild;
		}
	}

}
