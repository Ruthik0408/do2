package itsdc.dolphin.do2;

 

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.dom4j.Attribute;
import org.dom4j.io.SAXReader;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.utils.DateManager;

@RestController
public class Do2Feedback implements java.io.Serializable {
	public String inFileDir = null;
	public String outFileDir = null;
	public String feedbackType;
	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
	Do2Repository do2Repo;
	
	
	
	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public String getOutFileDir() {
		return outFileDir;
	}

	public void setOutFileDir(String outFileDir) {
		this.outFileDir = outFileDir;
	}

	public String getFeedbackType() {
		return feedbackType;
	}

	public void setFeedbackType(String feedbackType) {
		this.feedbackType = feedbackType;
	}

	public String getInFileDir() {
		return inFileDir;
	}

	public void setInFileDir(String inFileDir) {
		this.inFileDir = inFileDir;
	}

	public Do2Feedback() {
	}

	public void modify() {

		if (inFileDir.endsWith("/")) {
			inFileDir = inFileDir.substring(0, inFileDir.length() - 1);
		}
		if (outFileDir.endsWith("/")) {
			outFileDir = outFileDir.substring(0, outFileDir.length() - 1);
		}
		File dir = new File(inFileDir);
		if (!dir.exists()) {
			//FacesMessages.instance().add("Folder " + inFileDir + " not found -- Check absolute path ");
			return;
		}
		File dir1 = new File(outFileDir);
		if (!dir1.exists()) {
			//FacesMessages.instance().add("Folder " + outFileDir + " not found -- Check absolute path ");
			return;
		}
		File[] files = dir.listFiles();

		if (files != null && files.length > 0) {
			for (int k = 0; k < files.length; k++) {
				if (files[k].isFile()&& (files[k].getName().endsWith(".XML") || files[k].getName().endsWith(".xml"))) {
					if(files[k].exists()) {
						modifyThisXmlFile(files[k]);
					} else {
						//FacesMessages.instance().add(" File not found -- Check absolute path ");
						continue;
					}
				} else {
					continue;
				}

			}
			//FacesMessages.instance().add(" Job Done ");
			System.out.println("Done");
		}
	}
	
	public void modifyThisXmlFile(File f) {
//		System.out.println("------------------ f absolutepath : " + f.getAbsolutePath());
		String p2order_group = "";
		String p2order_number = "";
		String p2order_type = "";
		String p2order_date = "";
		String p2order_no = "";
		String p2order_itemNo = "";
		String itemNo = "";
		String sus_number = "";
		Date pub_date = null;
		String reason = null;
		String filename = null;
		String fileextn = null;
		String outfile = null;
		List<Do2> do2List = null; 
		int valid = 0;
		int rej = 0;
		valid = 0;
		rej = 0;
		try {
			int nameLen = (int) f.getName().length();
			filename = f.getName().substring(0, nameLen - 5);
			fileextn = f.getName().substring(nameLen - 4);
			String toDay = df1.format(DateManager.getToday());
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(f.getAbsolutePath());
			// Get the root element
			Node agency = doc.getFirstChild();
			NodeList agencyList = agency.getChildNodes();
			for (int i = 0; i < agencyList.getLength(); i++) {
				Node header = agencyList.item(i);
				if (header.getNodeName().equals("Header")) {
					NodeList headerList = header.getChildNodes();
					for (int j = 0; j < headerList.getLength(); j++) {

						Node node = headerList.item(j);

						// get the element, and update the value
						if (node.getNodeName().equals("PTO_Group")) {
							p2order_group = node.getTextContent();
						}
						if (node.getNodeName().equals("PTO_No")) {
							p2order_number = node.getTextContent();
							p2order_type = p2order_number.substring(0,
									1);
							p2order_no = p2order_number.substring(2, 6);
						}
						if (node.getNodeName().equals("SUS_No")) {
							sus_number = node.getTextContent();
						}
						if (node.getNodeName().equals("PTO_Date")) {
							p2order_date = node.getTextContent();
							try {
								pub_date = DateManager.getDate(p2order_date.substring(6) + "-"
											+ p2order_date.substring(3, 5) + "-" + p2order_date.substring(0, 2));
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if (node.getNodeName().equals("Obsn_header")) {
							node.setTextContent(toDay);
						}
					}
				}
			}
			if (this.feedbackType != null && this.feedbackType.equals("ACK")) {
				Node ptodetails = agency.getLastChild();
				agency.removeChild(ptodetails);
				outfile = outFileDir + "/" + filename + "9" + fileextn;
				TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(outfile));
				transformer.transform(source, result);
				//FacesMessages.instance().add(" Job Done ");
				System.out.println("Done");
			}
			if (this.feedbackType != null && this.feedbackType.equals("CORR")) {
				Node ptodetails = agency.getLastChild();
				agency.removeChild(ptodetails);
				outfile = outFileDir + "/" + filename + "7" + fileextn;
				TransformerFactory transformerFactory = TransformerFactory
						.newDefaultInstance();
				Transformer transformer = transformerFactory
						.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(
						new File(outfile));
				transformer.transform(source, result);
			//	FacesMessages.instance().add(" Job Done ");
				System.out.println("Done");
			}
			if(!getFeedbackType().equals("ACK")) {
				do2List =do2Repo.findBySusNo(sus_number,p2order_group,p2order_type, p2order_no, pub_date);
				 
				if (do2List == null || do2List.size() == 0) {
					//FacesMessages.instance().add(" Data of the given input file not found in DO2 table ");
					return;
				}
				for (Do2 d : do2List) {
					if (d.getStatus().equals("P") && d.getReason().contains("Batch to be")) {
						//FacesMessages.instance().add(" Batch has not been processed completely ");
						return;
					} else if (d.getStatus().equals("V")) {
						++valid;
					} else {
						++rej;
					}
				}
				if (do2List.size() == valid) {
					Node ptodetails = agency.getLastChild();
					agency.removeChild(ptodetails);
					outfile = outFileDir + "/" + filename + "5" + fileextn;
					TransformerFactory transformerFactory = TransformerFactory
							.newDefaultInstance();
					Transformer transformer = transformerFactory
							.newTransformer();
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(
							new File(outfile));
					transformer.transform(source, result);
					//FacesMessages.instance().add(" Job Done ");
					System.out.println("Done");
					// return;
				}
				for (Do2 d : do2List) {
					if (d.getStatus().equals("V")
							|| d.getStatus().equals("C")) {// valid items
						// can be
						// removed
						p2order_itemNo = d.getDo2ItemNo();
						reason = d.getReason();
						Node ptodetails = agency.getLastChild();
						NodeList rowList = ptodetails.getChildNodes();
						for (int i = 0; i < rowList.getLength(); i++) {
							Node row = rowList.item(i);
							NodeList list = row.getChildNodes();
							boolean flag = false;
							for (int j = 0; j < list.getLength(); j++) {

								Node node = list.item(j);
								if (node.getNodeName().equals("Item_No")) {
									itemNo = node.getTextContent();
								}

								// get the element, and delete
								if (Integer.parseInt(itemNo) == Integer
										.parseInt(p2order_itemNo)) {
									flag = true;
								}

							}
							if (flag == true)
								ptodetails.removeChild(row);
						}
					} else {
						p2order_itemNo = d.getDo2ItemNo();
						reason = "";
						for (int w = 0; w < d.getReason().length(); w++) {
							if (d.getReason().charAt(w) == '\n') {
								reason += ":Y^";
							} else {
								reason += d.getReason().charAt(w);
							}
						}
						Node ptodetails = agency.getLastChild();
						NodeList rowList = ptodetails.getChildNodes();
						for (int i = 0; i < rowList.getLength(); i++) {
							Node row = rowList.item(i);
							NodeList list = row.getChildNodes();
							for (int j = 0; j < list.getLength(); j++) {

								Node node = list.item(j);
								if (node.getNodeName().equals("Item_No")) {
									itemNo = node.getTextContent();
								}
								// get the element, and update the value
								if (node.getNodeName().equalsIgnoreCase("Obsn_item")) {
									if (Integer.parseInt(itemNo) == Integer.parseInt(p2order_itemNo)) {
										node.setTextContent(reason);
									}
								}
							}
						}
					}
				}
			}
			// write the content into xml file
			if (this.feedbackType != null && this.feedbackType.equals("RESULT")) {
				outfile = outFileDir + "/" + filename + "6" + fileextn;
				TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(outfile));
				transformer.transform(source, result);
			}
		} catch (Exception pce) {
			pce.printStackTrace();
		}
	}

}
