package cpk;

import javax.servlet.http.*;
import javax.servlet.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.jcabi.xml.XMLDocument;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Serv extends HttpServlet {

	private static final long serialVersionUID = -1825743577532768126L;
	private static int PRETTY_PRINT_INDENT_FACTOR = 4;
	private final String errConnection = "Service is temporarily unavailable.";
	private final int SC_OK = 200;
	private final int SC_UNPROCESSABLE = 422;
	private int TIMEOUT_VALUE = 3000;

	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		res.setContentType("application/json; charset=UTF-8");
		PrintWriter pw = res.getWriter();
		String sigla = req.getParameter("sigla");

		Response response = sendRequest("http://aleph.nkp.cz/X?op=find&find_code=wrd&base=ADR&request=sig=" +
		        sigla.toLowerCase());
		if (response.statusCode != SC_OK) { printErrorMessage(pw, errConnection); res.setStatus(response.statusCode); return; }
		String set_no = response.content.substring(response.content.indexOf("<set_number>") + 12,
		        response.content.indexOf("</set_number>"));

		Response info = sendRequest("http://aleph.nkp.cz/X?op=present&set_entry=000000001&format=marc&set_no=" + set_no);
		if (info.statusCode != SC_OK) { printErrorMessage(pw, errConnection); res.setStatus(response.statusCode); return; }

		Pattern p = Pattern.compile(".*/(\\w+)");
		Matcher m = p.matcher(req.getRequestURL());
		if (m.find()) {
			if (m.group(1).equals("getname")) {
				info.content = convertLabels(info.content, true);
			}
			else if (m.group(1).equals("getinfo")) {
				info.content = convertLabels(info.content, false);
			}
			String json = convertXmlToJson(info.content);
			pw.println(json);
		}
	}

	private void printErrorMessage(PrintWriter pw, String message) {
		String json = convertXmlToJson("<error>" + message + "</error>");
		pw.println(json);
	}

	private String convertLabels(String info, boolean shorten) {
		Document sourceDoc = buildDocument(info);
		String xml = "";
		Element name = null;
		final String specialAppend = "TEL|FAX|POI|EMK|ZKR|SGL|DRL|AKT|POU";
		NodeList oai_marc = sourceDoc.getElementsByTagName("oai_marc").item(0).getChildNodes();
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("library");
			doc.appendChild(rootElement);

			for (final Node varfield : new IterableNodeList(oai_marc)) {
				Element varfieldElem = (Element) varfield;
				if (varfieldElem.getNodeName().equals("varfield")) {
					if (varfieldElem.getAttribute("id").matches("MES")) continue;
					if (shorten) if ( ! varfieldElem.getAttribute("id").matches("NAZ|ZKR|VAR")) continue;
					if (varfieldElem.getAttribute("id").matches("NAZ|VAR")) {
						if (name == null) {
							name = doc.createElement("name");
							rootElement.appendChild(name);
						}
						Element label = null;
						if (varfieldElem.getAttribute("id").matches("NAZ")) {
							label = doc.createElement("cs");
						} else if (varfieldElem.getAttribute("id").matches("VAR")) {
							label = doc.createElement("en");
						}
						for (final Node subfield : new IterableNodeList(
								varfieldElem.getChildNodes())) {
							Element subfieldElem = (Element) subfield;
							Element sublabel = doc.createElement(renameId(varfieldElem.getAttribute("id"),
									subfieldElem.getAttribute("label")));
							sublabel.appendChild(doc.createTextNode(subfieldElem.getTextContent()));
							label.appendChild(sublabel);
						}
						name.appendChild(label);
						continue;
					}
					Element id = null;
					if ( ! varfieldElem.getAttribute("id").matches(specialAppend)) {
						id = doc.createElement(renameId(varfieldElem.getAttribute("id"), null));
						rootElement.appendChild(id);
					}
					for (final Node subfield : new IterableNodeList(varfieldElem.getChildNodes())) {
						Element subfieldElem = (Element) subfield;
						if (varfieldElem.getAttribute("id").matches(specialAppend)) {
							Element label = doc.createElement(renameId(varfieldElem.getAttribute("id"), null));
							label.appendChild(doc.createTextNode(subfieldElem.getTextContent()));
							rootElement.appendChild(label);
						}
						else {
							Element label = doc.createElement(renameId(varfieldElem.getAttribute("id"), 
									subfieldElem.getAttribute("label")));
							label.appendChild(doc.createTextNode(subfieldElem.getTextContent()));
							id.appendChild(label);
						}
					}
				}
	        }
			xml = new XMLDocument(doc).toString();

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}
		return xml;
	}
	
	private String renameId(String attribute, String sub) {
		switch (attribute) {
		case "SGL":
			if (sub == null) return "sigla";
			return sub.toLowerCase();
		case "ZKR":
			if (sub == null) return "code";
			return sub.toLowerCase();
		case "ICO":
			if (sub == null) return "ico";
			if (sub.equals("a")) return "ic";
			if (sub.equals("b")) return "universal";
			return sub.toLowerCase();
		case "TYP":
			if (sub == null) return "type";
			if (sub.equals("a")) return "short";
			if (sub.equals("b")) return "full";
			return sub.toLowerCase();
		case "ADR":
			if (sub == null) return "address";
			if (sub.equals("u")) return "street";
			if (sub.equals("c")) return "zip";
			if (sub.equals("m")) return "city";
			if (sub.equals("g")) return "coordinates";
			return sub.toLowerCase();
		case "KRJ":
			if (sub == null) return "region";
			if (sub.equals("a")) return "district";
			if (sub.equals("b")) return "town";
			return sub.toLowerCase();
		case "JMN":
			if (sub == null) return "person";
			if (sub.equals("t")) return "prefix";
			if (sub.equals("k")) return "givenname";
			if (sub.equals("p")) return "surname";
			if (sub.equals("c")) return "sufix";
			if (sub.equals("r")) return "function";
			if (sub.equals("f")) return "phone";
			if (sub.equals("e")) return "email";
			if (sub.equals("o")) return "accosting";
			return sub.toLowerCase();
		case "TEL":
			if (sub == null) return "phone";
			return sub.toLowerCase();
		case "FAX":
			if (sub == null) return "fax";
			return sub.toLowerCase();
		case "EML":
			if (sub == null) return "email";
			if (sub.equals("u")) return "email";
			if (sub.equals("z")) return "division";
			return sub.toLowerCase();
		case "URL":
			if (sub == null) return "url";
			if (sub.equals("u")) return "link";
			if (sub.equals("z")) return "description";
			return sub.toLowerCase();
		case "POI":
			if (sub == null) return "description";
			return sub.toLowerCase();
		default:
			if (sub == null) return attribute.toLowerCase();
			return sub.toLowerCase();
		}
	}

	protected Document buildDocument(String response) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document doc = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new InputSource(new StringReader(response)));
            //doc.getDocumentElement().normalize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

	private String convertXmlToJson(String info) {
		String jsonPrettyPrintString = "";
		try {
			org.json.JSONObject xmlJSONObj = XML.toJSONObject(info);
			jsonPrettyPrintString = xmlJSONObj
					.toString(PRETTY_PRINT_INDENT_FACTOR);
		} catch (JSONException je) {
			jsonPrettyPrintString = je.toString();
		}
		return jsonPrettyPrintString;
	}

	private Response sendRequest(String link) {
		HttpURLConnection con;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			URL url = new URL(link);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(TIMEOUT_VALUE);
			con.setReadTimeout(TIMEOUT_VALUE);
			rd = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			return new Response(null, SC_UNPROCESSABLE);
		}
		return new Response(result, SC_OK);
	}
}
