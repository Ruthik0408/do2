package itsdc.dolphin.do2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2Control;
import itsdc.dolphin.models.Do2XmlRejectedItems;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.PasswordManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class ReviewBatchMismatch {
	
	public static void main(String args[]) {
		ReviewBatchMismatch m=new ReviewBatchMismatch();
		File f=new File("/home/itsdcsec/Documents/r/IA_7_4139001_112012022_23122022_4.XML");
		Object[] obj=m.importXmlData(f.getAbsolutePath(), "01/2023", true, null,null,"62011");
		List<Do2XmlDataObject> xmlLocalDataList=(List<Do2XmlDataObject>) obj[0];
		for(Do2XmlDataObject o:xmlLocalDataList) {
			System.out.println(o.getArmy_no()+"--"+o.getDescription());
		}
		//System.out.println(obj[0]);
	}
	
	public Object[] importXmlData(String fileName, String monthEnding, Boolean validDo2Items, Usr usr,
			Do2Control dc, String batchNo) {

		Object[] obj=new Object[2];

		List<Do2XmlDataObject> xmlDataList = new ArrayList<Do2XmlDataObject>();
		List<Do2XmlRejectedItems> dxirDataList = new ArrayList<Do2XmlRejectedItems>();
		Document doc = null;
		List list = null;
		Attribute attribute = null;
		Object p2order_group, p2order_number, sus_number, p2order_date, army_no, ano, cd, fr_date, to_date, occm, obsn,
				pub_date, rate, sra, lra, col_a, col_b, col_c, refdono, p2order_type, p2order_no, rootName, unitcd,
				description, do2ItemNo, pdfAttachFn;
		ano = null;
		cd = null;
		obsn = null;
		description = null;
		do2ItemNo = null;
		int no_of_items_in_do2;
		try {
			SAXReader reader = new SAXReader();
			doc = reader.read(new File(fileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Element rootElement = doc.getRootElement();
		rootName = rootElement.getName() + "//Header//";
		list = doc.selectNodes(rootName + "PTO_Group");
		p2order_group = ((Element) list.get(0)).getText();
		list = doc.selectNodes(rootName + "PTO_No");
		p2order_number = ((Element) list.get(0)).getText();
		p2order_type = ((String) p2order_number).substring(0, 1);
		p2order_no = ((String) p2order_number).substring(2, 6);
		list = doc.selectNodes(rootName + "SUS_No");
		sus_number = ((Element) list.get(0)).getText();
		list = doc.selectNodes(rootName + "PTO_Date");
		p2order_date = ((Element) list.get(0)).getText();
		pub_date = null;
		pub_date = ((String) p2order_date).substring(6) + "-" + ((String) p2order_date).substring(3, 5) + "-"
				+ ((String) p2order_date).substring(0, 2);
		rootName = rootElement.getName() + "//PTO_Details//row//";
		list = doc.selectNodes(rootName + "Item_No");
		no_of_items_in_do2 = list.size();
		// System.out.println("\t\t ^^^^^^^^^^^^^^^^^^^^^^^ no_of_items_in_do2 : "
		// + no_of_items_in_do2);
		unitcd = "";
		/*List<Unit> unitList = unitRepo.findBySusNo((String)sus_number);
				
			 
		if (unitList != null && unitList.size() > 0) {
			unitcd = unitList.get(0).getUnitCode();
		}*/
		List<HashMap<String, String>> hpItemList = readRows(new File(fileName));
		 System.out.println("\t\t############## hpItemList size : " +
		 hpItemList.size());
		Object tagValue = null;
		StringBuffer rejReason = null;
		Do2XmlDataObject xmlRecord = null;
		Do2XmlRejectedItems erroneousXmlRecord = null;
		PasswordManager passwordService = PasswordManager.getInstance();
		for (HashMap<String, String> hm : hpItemList) {
			try {
				rejReason = new StringBuffer();
				xmlRecord = new Do2XmlDataObject();
				erroneousXmlRecord = new Do2XmlRejectedItems();
				xmlRecord.setUnit_code(unitcd);
				xmlRecord.setDo2_group(p2order_group);
				xmlRecord.setDo2_type(p2order_type);
				xmlRecord.setDo2_no(p2order_no);
				xmlRecord.setDo2_date(pub_date);
				xmlRecord.setStatus("P");
				xmlRecord.setReason("Batch to be cleared after verification");
				xmlRecord.setBatch_no(batchNo);
				tagValue = hm.get("Item_No");

				/*
				 * if(validDo2Items) { System.out.println("\t------------ Item No : " +
				 * (String)tagValue); }
				 */ /*
					 * if (tagValue != null && ((String) tagValue).trim().length() > 0) { do2ItemNo
					 * = padNumData((String)tagValue, 3); }
					 */
				//System.out.println("\t^^^^^^^^^^ do2ItemNo : " + do2ItemNo);
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					do2ItemNo = padNumData((String) tagValue, 3);
					xmlRecord.setDo2_item_no(do2ItemNo);
				} else {
					rejReason.append("Tag <Item_No> is missing.");
					xmlRecord.setDo2_item_no(null);
				}
				tagValue = hm.get("Obsn_item");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setStatus("RR");
					xmlRecord.setReason((String) tagValue);
				}
				xmlRecord.setQe(monthEnding);
				xmlRecord.setTranscription_type("D");
				tagValue = hm.get("Pin");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					/*
					 * if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){ if
					 * (((String)tagValue).trim().length() == 5){ String ano1 = (String) tagValue;
					 * ano1="000"+ano1; ano = ano1; cd = String.valueOf(Utils.getCheckDigit(ano1));
					 * xmlRecord.setArmy_no(ano); xmlRecord.setCheck_digit(cd); } else if
					 * (((String)tagValue).trim().length() == 4){ String ano1 = (String) tagValue;
					 * ano1="0000"+ano1; ano = ano1; cd = String.valueOf(Utils.getCheckDigit(ano1));
					 * xmlRecord.setArmy_no(ano); xmlRecord.setCheck_digit(cd); } else {
					 * rejReason.append("Invalid value at Tag <Pin>."); xmlRecord.setArmy_no(ano);
					 * xmlRecord.setCheck_digit(cd); } } else
					 */ {
						if (((String) tagValue).trim().length() == 9) {
							ano = ((String) tagValue).substring(0, 8);
							cd = ((String) tagValue).substring(8);
							xmlRecord.setArmy_no(ano);
							xmlRecord.setCheck_digit(cd);
						} else {
							ano = ((String) tagValue).substring(0, 8);
							rejReason.append("Invalid value at Tag <Pin>.");
							xmlRecord.setArmy_no(ano);
							xmlRecord.setCheck_digit(cd);
						}
					}
				} else {
					rejReason.append("Tag <Pin> is missing.");
					xmlRecord.setArmy_no(null);
					xmlRecord.setCheck_digit(null);
				}
				tagValue = hm.get("Description");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setDescription((String) tagValue);
					description = xmlRecord.getDescription();
				} else {
					rejReason.append("Tag <Description> is missing.");
					xmlRecord.setDescription(null);
				}
				tagValue = hm.get("From_Date");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setFrom_date(((String) tagValue).substring(6, 10) + "-"
							+ ((String) tagValue).substring(3, 5) + "-" + ((String) tagValue).substring(0, 2));
				} else {
					rejReason.append("Tag <From_Date> is missing.");
					xmlRecord.setFrom_date(null);
				}
				if (((String) p2order_group).startsWith("II")) {
					// xmlRecord.setTo_date(null);
					tagValue = hm.get("To_Date");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						xmlRecord.setTo_date(((String) tagValue).substring(6, 10) + "-"
								+ ((String) tagValue).substring(3, 5) + "-" + ((String) tagValue).substring(0, 2));
					} else {
						xmlRecord.setTo_date(null);
					}
					xmlRecord.setRate(0.0);
					xmlRecord.setTjra(0);
					xmlRecord.setLra(0);
				} else if (((String) p2order_group).startsWith("IB")) {
					tagValue = hm.get("To_Date");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						xmlRecord.setTo_date(((String) tagValue).substring(6, 10) + "-"
								+ ((String) tagValue).substring(3, 5) + "-" + ((String) tagValue).substring(0, 2));
					} else {
						xmlRecord.setTo_date(null);
					}
					tagValue = hm.get("Months");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						if (Utils.isValidNumber(((String) tagValue).trim())) {
							xmlRecord.setTjra(Integer.parseInt(((String) tagValue).trim()));
						}
					} else {
						xmlRecord.setTjra(0);
					}
					tagValue = hm.get("Days");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						if (Utils.isValidNumber(((String) tagValue).trim())) {
							xmlRecord.setLra(Integer.parseInt(((String) tagValue).trim()));
						}
					} else {
						xmlRecord.setLra(0);
					}
					tagValue = hm.get("OCCM");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						xmlRecord.setPunishment((String) tagValue);
					} else {
						xmlRecord.setPunishment(null);
					}
				} else if (((String) p2order_group).startsWith("IA")) {
					tagValue = hm.get("To_Date");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						xmlRecord.setTo_date(((String) tagValue).substring(6, 10) + "-"
								+ ((String) tagValue).substring(3, 5) + "-" + ((String) tagValue).substring(0, 2));
					} else {
						xmlRecord.setTo_date(null);
					}
					tagValue = hm.get("Amt");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						if (Utils.isValidNumber((String) tagValue)) {
							if (tagValue instanceof String) {
								if (((String) tagValue).contains(".")) {
									xmlRecord.setRate(Double.parseDouble(((String) tagValue).trim()));
								} else {
									xmlRecord.setRate(Double.parseDouble(((String) tagValue).trim() + ".00"));
								}
							}
						} else {
							rejReason.append("Tag <Amt> contains non-numeric value.");
							xmlRecord.setRate(null);
						}
					}
					tagValue = hm.get("SRA");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						if (Utils.isValidNumber(((String) tagValue).trim())) {
							xmlRecord.setTjra(Integer.parseInt(((String) tagValue).trim()));
						}
					} else {
						xmlRecord.setTjra(0);
					}
					tagValue = hm.get("NRA");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						if (Utils.isValidNumber(((String) tagValue).trim())) {
							xmlRecord.setLra(Integer.parseInt(((String) tagValue).trim()));
						}
					} else {
						xmlRecord.setLra(0);
					}
				}
				tagValue = hm.get("Other_Allow1");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setColumn_a((String) tagValue);
				} else {
					xmlRecord.setColumn_a(null);
				}
				tagValue = hm.get("Other_Allow2");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setColumn_b((String) tagValue);
				} else {
					xmlRecord.setColumn_b(null);
				}
				tagValue = hm.get("Other_Allow3");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setColumn_c((String) tagValue);
				} else {
					xmlRecord.setColumn_c(null);
				}
				tagValue = hm.get("Ref_PTO_No");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setRef_do2_no((String) tagValue);
				} else {
					xmlRecord.setRef_do2_no(null);
				}
				tagValue = hm.get("Attachment");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setAttachment(Utils.getEncrypt((String) tagValue));
				} else {
					xmlRecord.setAttachment(null);
				}
//				System.out.println("????tagValue : " + tagValue);
//				System.out.println("????xmlRecord.getAttachment : " + xmlRecord.getAttachment());
				if (rejReason.toString().trim().length() <= 0) {
					xmlRecord.setXmlFileName(new File(fileName).getName());
					xmlDataList.add(xmlRecord);
				} else {
					erroneousXmlRecord.setCreatedAt(DateManager.getCurrentTimestamp());
					erroneousXmlRecord.setDo2Description((String) description);
					erroneousXmlRecord.setArmyNo((String) ano);
					if (((String) cd) != null) {
						erroneousXmlRecord.setCheckDigit(((String) cd));
					} else {
						erroneousXmlRecord.setCheckDigit(null);
					}
					// erroneousXmlRecord.setDo2ItemNo((String) do2ItemNo);

					erroneousXmlRecord.setDo2ItemNo(((String) xmlRecord.getDo2_item_no())
							.substring(((String) xmlRecord.getDo2_item_no()).length() - 3));
					// erroneousXmlRecord.setDo2ItemNo((String) padNumData((String)tagValue, 3));
					erroneousXmlRecord.setPao(Utils.getPao());
					erroneousXmlRecord.setUsr(usr);
					erroneousXmlRecord.setDo2Control(dc);
					erroneousXmlRecord.setRejectionReason(rejReason.toString());
					erroneousXmlRecord.setReviewedImported(false);
//					System.out.println("\t^^^^^^^^^^ do2ItemNo : " + do2ItemNo);
					// System.out.println("\t^^^^^^^^^^ do2ItemNo11 : " + (String)
					// padNumData((String)tagValue, 3));
//					System.out.println("\t\t~~~~~~~~~~~~~ rejReason : " + rejReason.toString());
					if (dxirDataList == null || dxirDataList.size() <= 0
							|| !dxirDataList.contains(erroneousXmlRecord)) {
						dxirDataList.add(erroneousXmlRecord);
					}
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}
		if (xmlDataList.size()>0) {
			obj[0]=xmlDataList;
		}
		if(dxirDataList.size()>0)
			obj[1]=dxirDataList;
 
			return obj;
		
	}

	public static List<HashMap<String, String>> readRows(File xmlFile) {
		List<HashMap<String, String>> hpList = null;
		try {
			hpList = new ArrayList<HashMap<String, String>>();
			javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			List<String> tagsList = new ArrayList<String>();
			org.w3c.dom.Element rootElement = doc.getDocumentElement();
			String rootName = rootElement.getNodeName() + "//Header//";
			org.w3c.dom.NodeList nodes = doc.getElementsByTagName("row");
			HashMap<String, String> hpItem = null;
			for (int i = 0; i < nodes.getLength(); i++) {
				if (tagsList != null) {
					tagsList.clear();
				}
				org.w3c.dom.Node node = nodes.item(i);
				hpItem = new HashMap<String, String>();
				if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					org.w3c.dom.Element element = (org.w3c.dom.Element) node;
					org.w3c.dom.NodeList nlist = element.getChildNodes();
					// System.out.println("+++++++++++++++++++ element : " +
					// element.getNodeName());
					for (int ii = 0; ii < nlist.getLength(); ii++) {
						if (!nlist.item(ii).getNodeName().equals("#text")) {
							tagsList.add(nlist.item(ii).getNodeName().toString());
							// System.out.println("\t**************** ii : " +
							// nlist.item(ii).getNodeName());
						}
					}
					// System.out.println("_________________________________ tagsList size : "
					// + tagsList.size());
					for (String s : tagsList) {
						hpItem.put(s, getValue(s, element));
						// System.out.println("\t\t^^^^^^^^^^^^^^ Tag : " + s +
						// " \tText : " + getValue(s, element));
					}
					hpList.add(hpItem);
					/*
					 * System.out.println("Stock Symbol: " + getValue("symbol", element));
					 * System.out.println("Stock Price: " + getValue("price", element));
					 * System.out.println("Stock Quantity: " + getValue("quantity", element));
					 */
				}
			}
		} catch (Exception e) {
			Throwable cause = e.getCause();
			Utils.handleException(e);
		}
		return hpList;
	}
	
	public static String getValue(String tag, org.w3c.dom.Element element) {
		org.w3c.dom.NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
		org.w3c.dom.Node node = (org.w3c.dom.Node) nodes.item(0);
		// System.out.println("--------------------- node : " +
		// node.getLocalName());
		return node != null ? node.getNodeValue() : null;
	}

	public static String padNumData(String field, int fieldLength) {
		String field1 = null;
		StringBuffer buffer = null;
		buffer = new StringBuffer();
		if (field.length() > fieldLength) {
			int start = field.length() - fieldLength;
			field1 = field.substring(start);
		} else {
			for (int i = 0; i < fieldLength - field.length(); i++) {
				buffer.append("0");
			}
			field1 = buffer.toString() + field;
		}

		return field1;
	}


}
