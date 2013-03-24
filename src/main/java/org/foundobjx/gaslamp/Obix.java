package org.foundobjx.gaslamp;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.foundobjx.gaslamp.http.RequestBuilder;
import org.foundobjx.gaslamp.http.Client;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * This is a class that demonstrates simple usage of the OBIX REST interface to
 * obtain numeric values. This class uses the following libraries
 * 
 * The Oasis oBix standards can be found here https://www.oasis-open.org/committees/documents.php?wg_abbrev=obix
 * 
 */
public class Obix {
    private static final String DOUBLE_VALUE_TAG = "real";
    private static final String VALUE_ATTR = "val";
    private static final String TIMESTAMP_TAG = "abstime";
    private static final Logger LOGGER = LoggerFactory.getLogger(Obix.class);
    public static final String END_KEY = "end";
    public static final String START_KEY = "start";
    public static final String HISTORY_QUERY = "~historyQuery";
    public static final String LIST_OBJ = "//list/obj";
    public static final String OBJ_REF = "//obj/ref";
    public static final String HREF = "href";
    public static final String HISTORY_URL_SUFFIX = "/obix/histories/";
    private final URL baseURL;
    private final String user;
    private final String password;

    /**
     * Create Obix with specified user, password, and url.
     * @param user just what one would think
     * @param password just what one would think
     * @param host e.g. //192.168.1.23
     */
    public Obix(final String user, final String password, final URL host) {
        this.user = user;
        this.password = password;
        this.baseURL = host;
    }

    /**
     * Convenience method, Get all of the history names 
     * @param location to request history names
     * @return The point names
     */
    public Collection<String> getPointNames(final String location) throws XPathExpressionException, ParseException,
                    IOException {
        return getPointNames(getPointNodes(location));
    }

    /**
     * Convenience method, Get all of the history URL
     * 
     * @param location to request
     * @return the point URLs
     */
    public Collection<URL> getPointURLs(final String location) throws XPathExpressionException, ParseException,
                    IOException {
        return getPointURLs(getPointNodes(location));
    }

    private NodeList getPointNodes(final String location) throws MalformedURLException, IOException,
                    XPathExpressionException {
        final Document document = httpGet(new URL(baseURL + HISTORY_URL_SUFFIX + location));
        final XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList)xPath.evaluate(OBJ_REF, document, XPathConstants.NODESET);
    }

    private Collection<String> getPointNames(final NodeList nodes) throws DOMException {
        final Collection<String> result = new ArrayList<String>();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            final NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                final Node hrefAttribute = attributes.getNamedItem(HREF);
                if (hrefAttribute != null) {
                    result.add(StringUtils.strip(hrefAttribute.getTextContent(), "/"));
                }
            }
        }
        return result;
    }

    private Collection<URL> getPointURLs(final NodeList nodes) throws DOMException {
        final Collection<URL> result = new ArrayList<URL>();
        for (String pointName : getPointNames(nodes)) {
            try {
                result.add(new URL(baseURL + HISTORY_URL_SUFFIX + pointName + "/"));
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to create URL for " + baseURL + HISTORY_URL_SUFFIX + pointName + "/");
            }
        }
        return result;
    }

    /**
     * Get a map of history values for a specific history
     * URL of form: 
     *  http://localhost/obix/histories/obix_location/SolarPanelTemperature/~historyQuery?start=2013-01-01T00:00:00.000-00:00&end=2013-1-2T00:00:00&limit=100
     * 
     * @param location to request history from
     * @param historyName history to request
     * @param start history start time
     * @param end history end time
     * @param limit maximum number of values to request
     * @return history values
     */
    public Map<DateTime, Double> getHistoryValues(final String location, final String historyName,
                    final DateTime start, final DateTime end, final int limit) throws XPathExpressionException,
                    ParseException, IOException {
        String historyQuery = HISTORY_QUERY + "?" + START_KEY + "=" + start + "&" + END_KEY + "=" + end + "&limit=" + limit;
        return getHistoryValues(location, historyName, historyQuery);
    }

    private Map<DateTime, Double> getHistoryValues(final String location, final String pointName, final String query)
                    throws XPathExpressionException, ParseException, IOException {
        final Map<DateTime, Double> result = new LinkedHashMap<DateTime, Double>();

        final Document document = httpGet(new URL(baseURL + HISTORY_URL_SUFFIX + location + pointName + "/" + query));
        final XPath historyXPath = XPathFactory.newInstance().newXPath();
        final NodeList historyNodeList = (NodeList) historyXPath.evaluate(LIST_OBJ, document, XPathConstants.NODESET);
        for (int i = 0; i < historyNodeList.getLength(); i++) {
            final Element history = (Element) historyNodeList.item(i);
            try {
                result.put(getDateValue(history), getDoubleValue(history));
            } catch (NullPointerException e) {
                LOGGER.info("could not parse " + history +" to a double");
            }
        }
        return result;
    }

    /**
     * @return Collection of URLs for the host. If an exception is
     *         thrown creating a URL then the URL is not added and the invalid
     *         string is logged
     * @throws XPathExpressionException
     * @throws IOException
     * @throws URISyntaxException
     */
    public Collection<URL> getLocations() throws XPathExpressionException, IOException {
        final URL baseHistoryURL = new URL(baseURL + HISTORY_URL_SUFFIX);
        final Document document = httpGet(baseHistoryURL);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final NodeList nodeList = (NodeList) xPath.evaluate(OBJ_REF, document, XPathConstants.NODESET);
        final Collection<URL> result = new ArrayList<URL>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            Node hrefAttribute = node.getAttributes().getNamedItem(HREF);
            if (hrefAttribute != null) {
                final String URLString = baseURL + HISTORY_URL_SUFFIX + hrefAttribute.getTextContent();
                try {
                    result.add(new URL(URLString));
                } catch (MalformedURLException e) {
                    LOGGER.error("Failed to create URL for " + URLString);
                }
            }
        }
        return result;
    }

    private Document httpGet(final URL url) throws IOException {
        final Client client = new Client();
        Document result = null;
        InputStream inputStream = null;
        try {
            inputStream = client.get(RequestBuilder.request()
                                                   .withUrl(url)
                                                   .withAuth(getAuth())
                                                   .withTimeout(20000)
                                                   .build());
            result = getDocument(inputStream);
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return result;
    }

    private String getAuth() {
        return Base64.encodeBase64String((user + ":" + password).getBytes());
   }

    /**
     * Get Document from the input stream
     * 
     * @param inputStream
     *            InputStream to read
     * @return a Document read from the input stream
     */
    public static Document getDocument(InputStream inputStream) throws SAXException, IOException,
                    ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringComments(false);
        factory.setIgnoringElementContentWhitespace(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new EmptyResolve());
        return builder.parse(inputStream);
    }

    /**
     * Convert a node (or document) to a string suitable for printing or logging
     * 
     * @param node
     *            Node to convert to a string
     * @return String representation of the node
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    public static String nodeToString(final Node node) {
        final StringWriter stringWriter = new StringWriter();
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        } catch (TransformerConfigurationException e) {
            LOGGER.error("", e);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error("", e);
        } catch (TransformerException e) {
            LOGGER.error("", e);
        }
        return stringWriter.toString();
    }

    /**
     * Extract the value from a "real" element. Will throw a null pointer
     * exception if the element is invalide
     * 
     * @param element
     * @return the Double value
     */
    protected static Double getDoubleValue(Element element) {
        final Node node = element.getElementsByTagName(DOUBLE_VALUE_TAG).item(0);
        return Double.valueOf(((Element) node).getAttribute(VALUE_ATTR));
    }

    /**
     * Extract the timestamp from an element
     * 
     * @param element
     * @return the timestamp
     */
    protected static DateTime getDateValue(Element element) throws ParseException {
        final DateTime result;
        final NodeList nodes = element.getElementsByTagName(TIMESTAMP_TAG);
        if (nodes.getLength() > 0) {
            final String value = ((Element) nodes.item(0)).getAttribute(VALUE_ATTR);
            DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
            result = formatter.parseDateTime(value);
        } else {
            result = new DateTime();
        }
        return result;
    }

    private static class EmptyResolve implements EntityResolver {
        @Override
        public InputSource resolveEntity(String arg0, String arg1) throws SAXException, IOException {
            return new InputSource(new StringReader(""));
        }
    }
}
