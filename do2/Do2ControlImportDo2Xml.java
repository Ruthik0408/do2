package itsdc.dolphin.do2;

 

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import itsdc.dolphin.models.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.NodeList;

//import com.google.gwt.dom.client.Node;

import antlr.StringUtils;
import itsdc.dolphin.repositories.Do2ControlRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.Do2XmlRejectedItemsRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EnrollDo2Repository;
import itsdc.dolphin.repositories.EntryCategoryRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.PasswordManager;
import itsdc.dolphin.utils.Utils;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// ... your existing queries stay

// Built-in: save(entity) or saveAll(list) handles inserts perfectly
// No new method needed!

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

@RestController
public class Do2ControlImportDo2Xml {
	
	@Autowired
	UnitRepository unitRepo1;
	
	@Autowired
	Do2Repository do2Repo1;
	
	@Autowired
	Do2ControlRepository do2ControlRepo1;
	
	@Autowired
	Do2XmlRejectedItemsRepository do2XmlRejItemsRepo1;
	
	@Autowired
	EntryCategoryRepository entryCatRepo1;
	
	@Autowired
	EnrollDo2Repository enrollDo2Repo1;
	
	@Autowired
	EmployeeRepository empRepo1;
	
	public static UnitRepository unitRepo;
	public static Do2Repository do2Repo;
	public static Do2ControlRepository do2ControlRepo;
	public static Do2XmlRejectedItemsRepository do2XmlRejItemsRepo;
	public static EntryCategoryRepository entryCatRepo;
	public static EnrollDo2Repository enrollDo2Repo;
	public static EmployeeRepository empRepo;
	
	
	@PostConstruct
	public void init() {
		do2Repo=do2Repo1;
		unitRepo=unitRepo1;
		do2ControlRepo=do2ControlRepo1;
		do2XmlRejItemsRepo=do2XmlRejItemsRepo1;
		entryCatRepo=entryCatRepo1;
		enrollDo2Repo=enrollDo2Repo1;
		empRepo=empRepo1;		
	}

	public static Object[] importXmlData(String fileName, String monthEnding, Boolean validDo2Items, Usr usr,
			Do2Control dc, String batchNo) {

		Object[] obj=new Object[2];

		List<Do2XmlDataObject> xmlDataList = new ArrayList<Do2XmlDataObject>();
		List<Do2XmlRejectedItems> dxirDataList = new ArrayList<Do2XmlRejectedItems>();
		Document doc = null;
		List list = null;
		Attribute attribute = null;
		Object p2order_group, p2order_number, sus_number, p2order_date, army_no, ano, cd, fr_date, to_date, occm, obsn,
				pub_date, rate, sra, lra, col_a, col_b, col_c, refdono, p2order_type, p2order_no, rootName, unitcd,
				description, do2ItemNo, pdfAttachFn, pto_remarks, col9_remarks, pan;
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
		List<Unit> unitList = unitRepo.findBySusNo((String)sus_number);
				
			 
		if (unitList != null && unitList.size() > 0) {
			unitcd = unitList.get(0).getUnitCode();
		}
		List<HashMap<String, String>> hpItemList = readRows(new File(fileName));
		 System.out.println("\t\t############## hpItemList size : " +
		 hpItemList.size());
		Object tagValue = null;
		StringBuffer rejReason = null;
		StringBuffer col9Remarks = null;
		Do2XmlDataObject xmlRecord = null;
		Do2XmlRejectedItems erroneousXmlRecord = null;
		PasswordManager passwordService = PasswordManager.getInstance();
		for (HashMap<String, String> hm : hpItemList) {
			try {
				ano = null;
				cd = null;
				obsn = null;
				description = null;
				do2ItemNo = null;
				rejReason = new StringBuffer();
				col9Remarks = new StringBuffer();
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
				System.out.println("\t^^^^^^^^^^ do2ItemNo : " + do2ItemNo);
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
				if (((String) p2order_group).startsWith("ID")) {
					tagValue = hm.get("PAN");
					if (tagValue!=null) {
						if (((String) tagValue).trim().length()==10) {							
							xmlRecord.setPan((String) tagValue);
							pan = xmlRecord.getPan();
						} else {
							rejReason.append("Incorrect Value For Tag <PAN>.");
							xmlRecord.setArmy_no(null);
							xmlRecord.setCheck_digit(null);
						}
					}
				}				 
				tagValue = hm.get("Description");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setDescription((String) tagValue);
					description = xmlRecord.getDescription();
				} else {
					rejReason.append("Tag <Description> is missing.");
					xmlRecord.setDescription(null);
				}
				if (p2order_group!=null && p2order_group.toString().trim().equals("ID") && description!=null && description.toString().trim().equals("PFWDL")) {
					System.out.println("111111111111111 ano " +xmlRecord.getArmy_no());
					System.out.println("....p2ordergroup " +p2order_group+"  "+description+"11111111");
					xmlRecord.setReason("Item to be cleared by Fund Cell");
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
				} else if (((String) p2order_group).startsWith("IA") || ((String) p2order_group).startsWith("ID")) {
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
				tagValue = hm.get("Delayed");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) { 
					xmlRecord.setDelayed((String) tagValue);
				} else {
					xmlRecord.setDelayed("N");
				}
				tagValue = hm.get("CFA_sanc_received");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setCfa_sanction_received((String) tagValue);
				} else {
					xmlRecord.setCfa_sanction_received("N");
				}
				tagValue = hm.get("CFA_sanc_letter_no");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					if (((String) tagValue).trim().length() > 30) {
						xmlRecord.setCfa_sanction_letter_no(((String) tagValue).trim().substring(0,29));	
					} else {
						xmlRecord.setCfa_sanction_letter_no((String) tagValue);	
					}					
				} else {
					xmlRecord.setCfa_sanction_letter_no(null);
				}
				tagValue = hm.get("CFA_sanc_dt");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setCfa_sanction_date(((String) tagValue).substring(6, 10) + "-"
							+ ((String) tagValue).substring(3, 5) + "-" + ((String) tagValue).substring(0, 2));
				} else {
					xmlRecord.setCfa_sanction_date(null);
				}
				tagValue = hm.get("PTO_Remarks");
				if (tagValue != null && ((String) tagValue).trim().length() > 0) {
					xmlRecord.setPto_remarks((String) tagValue); 
				} else {
					xmlRecord.setPto_remarks(null);
				}
				
 				for (int i=1; i<=25; i++) { 
					tagValue = hm.get("Remarks"+i); 
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						col9Remarks.append("Remarks"+i+":"+tagValue.toString().trim()+"~");						
					} 
				}	
				
				if (col9Remarks!=null && col9Remarks.length()>0) {
					xmlRecord.setColumn9_remarks(col9Remarks.toString().trim());					
				} else {
					xmlRecord.setColumn9_remarks(null);
				}
				System.out.println("rejection reason .................. " +rejReason);
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
 		//			System.out.println("\t^^^^^^^^^^ do2ItemNo : " + do2ItemNo);
		//			  System.out.println("\t^^^^^^^^^^ do2ItemNo11 : " + (String)
		//			  padNumData((String)tagValue, 3));
 		//			System.out.println("\t\t~~~~~~~~~~~~~ rejReason : " + rejReason.toString());
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

	public static Integer insertDataIntoDo2(List<Do2XmlDataObject> xmlDataList, String batchNo) {
		String url;
		String driver;
		String user;
		String password;
	 
		List<Do2Control> dcList = null;
		List<Do2XmlRejectedItems> dxriList = null;
		String[] month = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
		final int[] mth = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
		Connection con = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date d = null;
		java.sql.Date date = null;
		 
		String unitCode,armyNo,checkDigit,do2Group,do2Type,do2No,do2ItemNo,punishment,description,colA,colB,colC,refDo2No,status,reason,transType,qe;
		String inputQe,attachment, delayed, cfaSanctionRecd, cfaSanctionNo, ptoRemarks, column9Remarks, pan;
		Date do2Date,fromDate,toDate,cfaSanctionDate;
		Double rate;
		Integer tjra,lra,fkUnit,bNo;
		Long fkEmp;
		
		Do2Control dc = null;
		int count = 0;
		if (xmlDataList.size() > 0) {
			try {
				

				for (int s = 0; s < xmlDataList.size(); s++) {
					unitCode=null;armyNo=null;checkDigit=null;do2Group=null;do2Type=null;do2No=null;do2ItemNo=null;punishment=null;description=null;
					colA=null;colB=null;colC=null;refDo2No=null;status=null;reason=null;transType=null;qe=null;
					inputQe=null;attachment=null;do2Date=null;fromDate=null;toDate=null;rate=null;tjra=null;lra=null;fkUnit=null;bNo=null;
					fkEmp=null; delayed=null; cfaSanctionRecd=null; cfaSanctionNo=null;cfaSanctionDate=null; ptoRemarks=null; column9Remarks=null; pan=null;
					count++;
					Do2XmlDataObject xmlRecord = xmlDataList.get(s);
					unitCode=(String) xmlRecord.getUnit_code();
					armyNo=(String) xmlRecord.getArmy_no();
					checkDigit= (String) xmlRecord.getCheck_digit();
					do2Group=(String) xmlRecord.getDo2_group();
					do2Type= (String) xmlRecord.getDo2_type();
					do2No=(String) xmlRecord.getDo2_no();				
				 
					if (((String) xmlRecord.getDo2_item_no()).length() <= 3) {
						do2ItemNo=padNumData((String) xmlRecord.getDo2_item_no(), 3);
					} else {
						do2ItemNo=((String) xmlRecord.getDo2_item_no())
								.substring(((String) xmlRecord.getDo2_item_no()).length() - 3);
					}
					if (xmlRecord.getDo2_date() == null) {
						do2Date=null;
					} else {						 
						do2Date = formatter.parse((String) xmlRecord.getDo2_date());						
					}
					punishment=(String) xmlRecord.getPunishment();
					description=(String) xmlRecord.getDescription();
					if (xmlRecord.getFrom_date() == null) {
						fromDate=null;
					} else {
						fromDate = formatter.parse((String) xmlRecord.getFrom_date());						 
					}
					if (xmlRecord.getTo_date() == null) {
						toDate=null;
					} else {
						toDate = formatter.parse((String) xmlRecord.getTo_date());						 
					}
					
					if (xmlRecord.getRate() != null) {
						if (xmlRecord.getRate() instanceof Double) {
							rate=(Double) xmlRecord.getRate();
						} else if (xmlRecord.getRate() instanceof String) {
							if (((String) xmlRecord.getRate()).contains(".00")
									|| ((String) xmlRecord.getRate()).contains(".0")) {
								rate=(Double) xmlRecord.getRate();
							} else {
								rate=((Integer) xmlRecord.getRate()).doubleValue();
							}
						}
					} else {
						rate= 0.0;
					}
					if (xmlRecord.getTjra() != null) {
						tjra=(Integer) xmlRecord.getTjra();
					} else {
						tjra= 0;
					}
					if (xmlRecord.getLra() != null) {
						lra=(Integer) xmlRecord.getLra();
					} else {
						lra= 0;
					}
					if (xmlRecord.getColumn_a() != null) {
						colA=((String) xmlRecord.getColumn_a()).trim().length() > 25
										? ((String) xmlRecord.getColumn_a()).substring(0, 24)
										: ((String) xmlRecord.getColumn_a()).trim();
					} else {
						colA=(String) xmlRecord.getColumn_a();
					}
					if (xmlRecord.getColumn_b() != null) {
						colB=((String) xmlRecord.getColumn_b()).trim().length() > 25
										? ((String) xmlRecord.getColumn_b()).substring(0, 24)
										: ((String) xmlRecord.getColumn_b()).trim();
					} else {
						colB=(String) xmlRecord.getColumn_b();
					}
					if (xmlRecord.getColumn_c() != null) {
						colC=((String) xmlRecord.getColumn_c()).trim().length() > 25
										? ((String) xmlRecord.getColumn_c()).substring(0, 24)
										: ((String) xmlRecord.getColumn_c()).trim();
					} else {
						colC=(String) xmlRecord.getColumn_c();
					}
					 
					refDo2No=(String) xmlRecord.getRef_do2_no();
					status=(String) xmlRecord.getStatus();
					reason=(String) xmlRecord.getReason();
					transType=(String) xmlRecord.getTranscription_type();
					qe=(String) xmlRecord.getQe();
					if (xmlRecord.getArmy_no() == null) {
						//stmt.setNull(24, java.sql.Types.NULL);
					} else {
						Employee employee = Utils.getCurrentEmployee((String) xmlRecord.getArmy_no());
						if (employee != null) {
							fkEmp=employee.getId(); 
						}  
					}				
				 
					Unit unit = unitRepo.findByUnitCode((String) xmlRecord.getUnit_code());
					if (unit!=null)
						fkUnit=unit.getId();
				
					bNo=(Integer) xmlRecord.getBatch_no();
					attachment=(String) xmlRecord.getAttachment();
				
					delayed=(String) xmlRecord.getDelayed();
			 		cfaSanctionRecd=(String) xmlRecord.getCfa_sanction_received();
					cfaSanctionNo=(String) xmlRecord.getCfa_sanction_letter_no();
					if (xmlRecord.getCfa_sanction_date()!=null) {
					 	d = formatter.parse((String) xmlRecord.getCfa_sanction_date());
						date = new java.sql.Date(d.getTime());
						date.setTime(d.getTime());
						cfaSanctionDate=date;
					}
					ptoRemarks=(String) xmlRecord.getPto_remarks();
					column9Remarks=(String) xmlRecord.getColumn9_remarks();
					pan=(String) xmlRecord.getPan();
				
					if (fkEmp==null) {
						if (do2Group.equals("ID") && description.equals("PFWDL")) {
							status="M";
							reason="MM:Master Missing";
							do2Repo.insertDo2FromDo2ControlEmpNull(unitCode, armyNo, checkDigit, do2Group, do2Type, do2No, do2ItemNo, do2Date, punishment, description, fromDate, toDate, rate, tjra, lra, colA, colB, colC, refDo2No, status, reason, transType, qe, qe, fkUnit, bNo, attachment, delayed, cfaSanctionRecd, cfaSanctionNo, cfaSanctionDate, ptoRemarks, column9Remarks, pan);
						} else {
							do2Repo.insertDo2FromDo2ControlEmpNull(unitCode, armyNo, checkDigit, do2Group, do2Type, do2No, do2ItemNo, do2Date, punishment, description, fromDate, toDate, rate, tjra, lra, colA, colB, colC, refDo2No, status, reason, transType, qe, qe, fkUnit, bNo, attachment, delayed, cfaSanctionRecd, cfaSanctionNo, cfaSanctionDate, ptoRemarks, column9Remarks, pan);
						}
					} else {
						do2Repo.insertDo2FromDo2Control(unitCode, armyNo, checkDigit, do2Group, do2Type, do2No, do2ItemNo, do2Date, punishment, description, fromDate, toDate, rate, tjra, lra, colA, colB, colC, refDo2No, status, reason, transType, qe, fkEmp, qe, fkUnit, bNo, attachment, delayed, cfaSanctionRecd, cfaSanctionNo, cfaSanctionDate, ptoRemarks, column9Remarks, pan);
					}
/*// commented on 07.04.2025
					if(fkEmp==null)
						do2Repo.insertDo2FromDo2ControlEmpNull(unitCode, armyNo, checkDigit, do2Group, do2Type, do2No, do2ItemNo, do2Date, punishment, description, fromDate, toDate, rate, tjra, lra, colA, colB, colC, refDo2No, status, reason, transType, qe, qe, fkUnit, bNo, attachment, delayed, cfaSanctionRecd, cfaSanctionNo, cfaSanctionDate, ptoRemarks, column9Remarks, pan);
					else						
						do2Repo.insertDo2FromDo2Control(unitCode, armyNo, checkDigit, do2Group, do2Type, do2No, do2ItemNo, do2Date, punishment, description, fromDate, toDate, rate, tjra, lra, colA, colB, colC, refDo2No, status, reason, transType, qe, fkEmp, qe, fkUnit, bNo, attachment, delayed, cfaSanctionRecd, cfaSanctionNo, cfaSanctionDate, ptoRemarks, column9Remarks, pan);
*/					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
		return count;
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

	public static void insertDataIntoDo2XmlRejectedItems(List<Do2XmlRejectedItems> dxriList) {
		// System.out.println("\t\t^^^^^^^^^^^^^^^^^^^^^^^^^^ received dxriList.size() :
		// "
		// + dxriList.size());
	 
		Do2Control dc = null;
		List<Do2XmlRejectedItems> dList = new ArrayList<Do2XmlRejectedItems>();
		List<Integer> dcIdList = new ArrayList<Integer>();
		for (Do2XmlRejectedItems dxri : dxriList) {
			if (dcIdList == null || dcIdList.size() <= 0 || !dcIdList.contains(dxri.getDo2Control().getId())) {
				dcIdList.add(dxri.getDo2Control().getId());
			}
			do2XmlRejItemsRepo.save(dxri);
			 
		}
		for (Integer dcid : dcIdList) {
			dc = do2ControlRepo.findById(dcid.intValue()).get();
			 
			dList = do2XmlRejItemsRepo.findByDo2ControlId(dcid.intValue());
					 
			if (dList == null || dList.size() <= 0) {
				dc.setImportedItems(dc.getTotalItems());
			} else {
				dc.setImportedItems(dc.getTotalItems() - dList.size());
			}
			
			do2ControlRepo.save(dc);
		}
		 
	}

	public static Do2Control ascertainData(File f, String me) {
		// System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% f : " +
		// f.getAbsolutePath());
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		Do2Control dc = new Do2Control();
		Document doc = null;
		List list = null;
		Attribute attribute = null;
		String p2order_group, p2order_number, sus_number, p2order_date, army_no, ano, cd, fr_date, to_date, occm, obsn,
				pub_date, rate, sra, lra, col_a, col_b, col_c, refdono, p2order_type, p2order_no, rootName, unitcd;
		obsn = null;
		int no_of_items_in_do2;
		try {
			SAXReader reader = new SAXReader();
			doc = reader.read(new File(f.getAbsolutePath()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Element rootElement = doc.getRootElement();
		rootName = rootElement.getName() + "//Header//";
		list = doc.selectNodes(rootName + "PTO_Group");
		p2order_group = ((Element) list.get(0)).getText();
		list = doc.selectNodes(rootName + "PTO_No");
		p2order_number = ((Element) list.get(0)).getText();
		p2order_type = p2order_number.substring(0, 1);
		p2order_no = p2order_number.substring(2, 6);
		list = doc.selectNodes(rootName + "SUS_No");
		sus_number = ((Element) list.get(0)).getText();
		list = doc.selectNodes(rootName + "PTO_Date");
		p2order_date = ((Element) list.get(0)).getText();
		pub_date = null;
		pub_date = p2order_date.substring(6) + "-" + p2order_date.substring(3, 5) + "-" + p2order_date.substring(0, 2);
		rootName = rootElement.getName() + "//PTO_Details//row//";
		list = doc.selectNodes(rootName + "Item_No");
		no_of_items_in_do2 = list.size();
		unitcd = "";
		 
		List<Unit> unitList =unitRepo.findBySusNo(sus_number); 
		if (unitList != null && unitList.size() > 0) {
			unitcd = unitList.get(0).getUnitCode();
		}
		
		dc.setUnitCode(unitcd);
//		System.out.println("\tUUUUUU ascertainData qryStr : " + qryStr + "\tdc.getUnitCode() : " + dc.getUnitCode());
		dc.setDo2No(p2order_no);
		dc.setDo2Type(p2order_type);
		try {
			dc.setDo2Date(formatter.parse(p2order_date));
		} catch (Exception e) {
			Utils.handleException(e);
		}
		dc.setTotalItems(no_of_items_in_do2);
		dc.setInputFileName(f.getName());
		if (p2order_group!=null && p2order_group.equals("ID") && f.getName().startsWith("ID")) {
			dc.setFundBatch(true);
		 	System.out.println(" part 2 order group " +p2order_group+"  f name  " +f.getName());			 
		}
		return dc;
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

	//Modified for New Enroll Do2------01062021
    public static Object[] importEnrollXmlData(
            String fileName,
            String monthEnding,
            Boolean validDo2Items,
            Usr usr,
            Do2Control dc,
            String batchNo
    ) {

        System.out.println("\n========== START IMPORT ENROLL XML ==========");
        System.out.println("File: " + fileName);
        System.out.println("Month Ending: " + monthEnding);
        System.out.println("Batch No: " + batchNo);
        System.out.println("================================================\n");

        Object[] obj = new Object[2];

        List<EnrollDo2XmlDataObject> enrollXmlDataList = new ArrayList<>();
        List<Do2XmlRejectedItems> dxirDataList = new ArrayList<>();

        Document doc = null;
        List list = null;
        Attribute attribute = null;

        Object p2order_group, p2order_number, p2order_type, p2order_no, p2order_date,
                sus_number, army_no, ano, cd, enroll_date, pub_date, rootName, unitcd,
                color_service, max_age, birth_date, arrival_date, advance_amount,
                entry_category, entry_trade, da_days, gso_code, obsn, description,
                do2ItemNo, willing;

        String[] gsoClass = null;
        ano = null;
        cd = null;
        obsn = null;
        description = null;
        willing = null;
        do2ItemNo = null;

        int no_of_items_in_do2;

        try {
            System.out.println("Reading XML file...");
            SAXReader reader = new SAXReader();
            doc = reader.read(new File(fileName));
            System.out.println("XML read successfully.");
        } catch (Exception e) {
            System.out.println("ERROR: Unable to read XML file!");
            e.printStackTrace();
        }

        Element rootElement = doc.getRootElement();
        rootName = rootElement.getName() + "//Header//";
        System.out.println("Root Element: " + rootElement.getName());

        list = doc.selectNodes(rootName + "PTO_Group");
        p2order_group = ((Element) list.get(0)).getText();
        System.out.println("PTO Group: " + p2order_group);

        list = doc.selectNodes(rootName + "PTO_No");
        p2order_number = ((Element) list.get(0)).getText();
        System.out.println("PTO No: " + p2order_number);

        p2order_type = ((String) p2order_number).substring(0, 1);
        p2order_no = ((String) p2order_number).substring(2, 6);
        System.out.println("Order Type: " + p2order_type);
        System.out.println("Order No: " + p2order_no);

        list = doc.selectNodes(rootName + "SUS_No");
        sus_number = ((Element) list.get(0)).getText();
        System.out.println("SUS No: " + sus_number);

        list = doc.selectNodes(rootName + "PTO_Date");
        p2order_date = ((Element) list.get(0)).getText();
        pub_date = ((String) p2order_date).substring(6) + "-" +
                ((String) p2order_date).substring(3, 5) + "-" +
                ((String) p2order_date).substring(0, 2);
        System.out.println("PTO Date Raw: " + p2order_date);
        System.out.println("PTO Date Parsed: " + pub_date);

        rootName = rootElement.getName() + "//PTO_Details//row//";
        list = doc.selectNodes(rootName + "Item_No");
        no_of_items_in_do2 = list.size();
        System.out.println("Total Items in DO2: " + no_of_items_in_do2);

        unitcd = "";
        List<Unit> unitList = unitRepo.findBySusNo((String) sus_number);
        if (unitList != null && !unitList.isEmpty()) {
            unitcd = unitList.get(0).getUnitCode();
        }
        System.out.println("Unit Code from DB: " + unitcd);

        Boolean separateMaxAge = null;

        System.out.println("Reading rows from XML...");
        List<HashMap<String, String>> hpItemList = readRows(new File(fileName));
        System.out.println("Total Rows Parsed: " + hpItemList.size());

        Object tagValue = null;

        int rowCount = 0;

        for (HashMap<String, String> hm : hpItemList) {
            rowCount++;
            System.out.println("\n================ Processing Row " + rowCount + " ================");

            try {
                separateMaxAge = false;
                StringBuffer rejReason = new StringBuffer();

                EnrollDo2XmlDataObject enrollXmlRecord = new EnrollDo2XmlDataObject();
                Do2XmlRejectedItems erroneousXmlRecord = new Do2XmlRejectedItems();

                enrollXmlRecord.setUnitCode(unitcd);
                enrollXmlRecord.setDo2Group(p2order_group);
                enrollXmlRecord.setDo2Type(p2order_type);
                enrollXmlRecord.setDo2No(p2order_no);
                enrollXmlRecord.setDo2Date(pub_date);
                enrollXmlRecord.setStatus("P");
                enrollXmlRecord.setReason("Batch to be cleared after verification");
                enrollXmlRecord.setQe(monthEnding);
                enrollXmlRecord.setBatchNo(Integer.parseInt(batchNo));
                enrollXmlRecord.setTranscriptionType("D");

                // ===================== Description =====================
                System.out.println("Reading <Description>...");
                tagValue = hm.get("Description");
                String xmlDescription = (tagValue != null) ? ((String) tagValue).trim() : "";

                if (xmlDescription.length() > 0) {
                    String finalDescription;

                    if ("94".equals(Utils.getPao().getPaoCode())) {
                        finalDescription = "REENRL";
                        System.out.println("PAO 94 detected → Description forced to: REENRL");
                    } else {
                        finalDescription = xmlDescription.toUpperCase(); // optional: normalize
                        System.out.println("Description from XML: " + finalDescription);
                    }

                    enrollXmlRecord.setDescription(finalDescription);
                } else {
                    rejReason.append("Tag <Description> is missing or empty. ");
                    System.out.println("ERROR: Description tag is missing or empty.");
                    enrollXmlRecord.setDescription(null);
                }

                // ===================== ITEM NO =====================
                System.out.println("Reading <Item_No>...");
                tagValue = hm.get("Item_No");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    do2ItemNo = padNumData((String) tagValue, 3);
                    enrollXmlRecord.setDo2ItemNo(do2ItemNo);
                    System.out.println("Item_No: " + do2ItemNo);
                } else {
                    rejReason.append("Tag <Item_No> is missing.");
                    System.out.println("ERROR: Item_No missing.");
                    enrollXmlRecord.setDo2ItemNo(null);
                }

                // ===================== PIN =====================
                System.out.println("Reading <Pin>...");
                tagValue = hm.get("Pin");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    if (((String) tagValue).trim().length() == 9) {
                        ano = ((String) tagValue).substring(0, 8);
                        cd = ((String) tagValue).substring(8);
                        enrollXmlRecord.setArmyNo(ano);
                        enrollXmlRecord.setCheckDigit(cd);
                        System.out.println("PIN OK: " + ano + "/" + cd);
                    } else {
                        rejReason.append("Invalid value at Tag <Pin>.");
                        System.out.println("ERROR: Invalid PIN.");
                        enrollXmlRecord.setArmyNo(null);
                        enrollXmlRecord.setCheckDigit(null);
                    }
                } else {
                    rejReason.append("Tag <Pin> is missing.");
                    System.out.println("ERROR: PIN missing.");
                    enrollXmlRecord.setArmyNo(null);
                    enrollXmlRecord.setCheckDigit(null);
                }

                // ===================== TRADE =====================
                System.out.println("Reading <Trade>...");
                tagValue = hm.get("Trade");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    if (((String) tagValue).trim().length() <= 9) {
                        enrollXmlRecord.setEntryTrade((String) tagValue);
                        System.out.println("Trade: " + tagValue);
                    } else {
                        rejReason.append("Value at Tag <Trade> more than 9 Characters.");
                        System.out.println("ERROR: Trade length > 9");
                        enrollXmlRecord.setEntryTrade(null);
                    }
                } else {
                    rejReason.append("Tag <Trade> is missing.");
                    System.out.println("ERROR: Trade missing.");
                    enrollXmlRecord.setEntryTrade(null);
                }

                // ===================== NAME =====================
                System.out.println("Reading <Name>...");
                tagValue = hm.get("Name");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    if (((String) tagValue).trim().length() <= 25) {
                        enrollXmlRecord.setEmployeeName((String) tagValue);
                    } else {
                        enrollXmlRecord.setEmployeeName(((String) tagValue).substring(0, 25));
                    }
                    System.out.println("Name: " + enrollXmlRecord.getEmployeeName());
                } else {
                    rejReason.append("Tag <Name> is missing.");
                    System.out.println("ERROR: Name missing.");
                    enrollXmlRecord.setEmployeeName(null);
                }

                // ===================== ENROL_DT =====================
                System.out.println("Reading <Enrol_Dt>...");
                tagValue = hm.get("Enrol_Dt");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    try {
                        String target = ((String) tagValue);
                        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                        Date result = df.parse(target);
                        enrollXmlRecord.setEnrollmentDate(result);
                        System.out.println("Enrol_Dt: " + target);
                    } catch (ParseException pe) {
                        rejReason.append("Invalid value at Tag <Enrol_Dt>.");
                        System.out.println("ERROR: Invalid Enrol_Dt format.");
                        enrollXmlRecord.setEnrollmentDate(null);
                    }
                } else {
                    rejReason.append("Tag <Enrol_Dt> is missing.");
                    System.out.println("ERROR: Enrol_Dt missing.");
                    enrollXmlRecord.setEnrollmentDate(null);
                }

                // ===================== TOE COLOUR SERVICE =====================
                System.out.println("Reading <TOE_Colour_Service>...");
                tagValue = hm.get("TOE_Colour_Service");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    String[] toe_cs_ma = ((String) tagValue).trim().split("/", 2);

                    if (Utils.isValidNumber(toe_cs_ma[0])) {
                        enrollXmlRecord.setColorService(toe_cs_ma[0]);
                        System.out.println("Color Service: " + toe_cs_ma[0]);
                    } else {
                        rejReason.append("Inappropriate Value Tag <TOE_Colour_Service>.");
                        System.out.println("ERROR: Invalid Colour Service");
                        enrollXmlRecord.setColorService(null);
                    }

                    if (Utils.isValidNumber(toe_cs_ma[1])) {
                        enrollXmlRecord.setMaxAge(toe_cs_ma[1]);
                        System.out.println("Max Age: " + toe_cs_ma[1]);
                    } else {
                        rejReason.append("Inappropriate Max Age Value under Tag <TOE_Colour_Service>.");
                        System.out.println("ERROR: Invalid Max Age");
                        enrollXmlRecord.setColorService(null);
                    }

                } else {
                    rejReason.append("Tag <TOE_Colour_Service> is missing.");
                    System.out.println("ERROR: TOE_Colour_Service missing.");
                    enrollXmlRecord.setColorService(null);
                }

                // ===================== DOB =====================
                System.out.println("Reading <DOB>...");
                tagValue = hm.get("DOB");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    try {
                        String target = ((String) tagValue);
                        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                        Date result = df.parse(target);
                        enrollXmlRecord.setDateOfBirth(result);
                        System.out.println("DOB: " + target);
                    } catch (ParseException pe) {
                        rejReason.append("Invalid value at Tag <DOB>.");
                        System.out.println("ERROR: Invalid DOB");
                        enrollXmlRecord.setDateOfBirth(null);
                    }
                } else {
                    rejReason.append("Tag <DOB> is missing.");
                    System.out.println("ERROR: DOB missing.");
                    enrollXmlRecord.setDateOfBirth(null);
                }

                // =============== REMARKS1 – DATE OF DEPARTURE ===============
                System.out.println("Reading <Remarks1> (Departure)...");
                tagValue = hm.get("Remarks1");

                if (tagValue != null && !((String) tagValue).trim().isEmpty()) {

                    String remarks1 = ((String) tagValue).trim();
                    String time1 = StringUtils.stripFrontBack(remarks1, "(", "hrs)").trim();
                    String digits1 = remarks1.replaceAll("\\D+", "");

                    if (digits1.length() < 8) {
                        System.out.println("No valid Departure Date found → saved as NULL");
                        enrollXmlRecord.setDateOfDeparture(null);
                        enrollXmlRecord.setDepartureTime(time1.isEmpty() ? null : time1);

                    } else {

                        String dodStr = digits1.substring(0, 8);

                        try {
                            Date dateDod = DateManager.getDateFromYyyyMmDdWithoutSeparators(dodStr);

                            enrollXmlRecord.setDateOfDeparture(dateDod);
                            enrollXmlRecord.setDepartureTime(time1.isEmpty() ? null : time1);

                            System.out.println("Departure Date: " + dodStr + "  Time: " + time1);

                        } catch (Exception e) {

                            System.out.println("Invalid Departure Date → saved as NULL");
                            enrollXmlRecord.setDateOfDeparture(null);
                            enrollXmlRecord.setDepartureTime(time1.isEmpty() ? null : time1);
                        }
                    }

                } else {

                    System.out.println("Remarks1 missing → saved as NULL");
                    enrollXmlRecord.setDateOfDeparture(null);
                    enrollXmlRecord.setDepartureTime(null);
                }
                // =============== REMARKS2 – DATE OF ARRIVAL ===============
                System.out.println("Reading <Remarks2> (Arrival)...");
                tagValue = hm.get("Remarks2");
                if (tagValue != null && !((String) tagValue).trim().isEmpty()) {

                    String remarks2 = ((String) tagValue).trim();
                    String time2 = StringUtils.stripFrontBack(remarks2, "(", "hrs)").trim();
                    String digits2 = remarks2.replaceAll("\\D+", "");

                    if (digits2.length() >= 8) {
                        String doaStr = digits2.substring(0, 8);
                        try {
                            Date dateDoa = DateManager.getDateFromYyyyMmDdWithoutSeparators(doaStr);
                            enrollXmlRecord.setDateOfArrival(dateDoa);
                            enrollXmlRecord.setArrivalTime(time2.isEmpty() ? null : time2);

                            System.out.println("Arrival Date: " + doaStr + "  Time: " + time2);

                        } catch (Exception e) {
                            rejReason.append("Invalid Arrival Date in Remarks2. ");
                            System.out.println("ERROR: Invalid Arrival Date");
                        }
                    } else {
                        rejReason.append("Invalid/Missing Arrival Date in Remarks2. ");
                        System.out.println("ERROR: Invalid Remarks2 format.");
                    }
                } else {
                    rejReason.append("Remarks2 (Arrival) is missing. ");
                    System.out.println("ERROR: Remarks2 missing.");
                }

                // ===================== Remarks3 – Mobile =====================
                System.out.println("Reading <Remarks3> (Mobile)...");
                tagValue = hm.get("Remarks3");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    if (((String) tagValue).trim().length() == 10) {
                        enrollXmlRecord.setMobileNo((String) tagValue);
                        System.out.println("Mobile: " + tagValue);
                    } else {
                        enrollXmlRecord.setMobileNo(null);
                        System.out.println("WARNING: Invalid mobile, setting null");
                    }
                } else {
                    rejReason.append("Tag <Remarks3> is missing.");
                    System.out.println("ERROR: Remarks3 missing.");
                    enrollXmlRecord.setMobileNo(null);
                }

                // ===================== Remarks4 =====================
                System.out.println("Reading <Remarks4>...");
                tagValue = hm.get("Remarks4");

                if (Utils.getPao().getPaoCode().equals("94")) {

                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        if (((String) tagValue).trim().contains("@")) {
                            enrollXmlRecord.setEmailId(((String) tagValue));
                            System.out.println("Email: " + tagValue);
                        } else {
                            rejReason.append("Value at Tag <Remarks4> is incorrect.");
                            System.out.println("ERROR: Invalid Email");
                            enrollXmlRecord.setEmailId(null);
                        }
                    } else {
                        rejReason.append("Value at Tag<Remarks4> is Missing.");
                        System.out.println("ERROR: Remarks4 missing (email).");
                    }

                } else {

                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        if (((String) tagValue).trim().length() == 1) {
                            enrollXmlRecord.setGender((String) tagValue.toString().trim());
                            System.out.println("Gender: " + tagValue);
                        } else {
                            rejReason.append("Value at Tag <Remarks4> is incorrect.");
                            System.out.println("ERROR: Invalid Gender.");
                            enrollXmlRecord.setGender(null);
                        }
                    } else {
                        rejReason.append("Tag <Remarks4> is missing.");
                        System.out.println("ERROR: Gender missing.");
                        enrollXmlRecord.setGender(null);
                    }
                }

                // ===================== CATEGORY =====================
                System.out.println("Reading <Category>...");
                tagValue = hm.get("Category");
                if (!Utils.getPao().getPaoCode().equals("94")) {
                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        if (((String) tagValue).trim().length() <= 9) {
                            enrollXmlRecord.setEntryCategory(((String) tagValue).trim());
                            System.out.println("Category: " + tagValue);
                        } else {
                            rejReason.append("Value at Tag <Category> is more than 9 Characters.");
                            System.out.println("ERROR: Category length > 9");
                            enrollXmlRecord.setEntryCategory(null);
                        }
                    } else {
                        rejReason.append("Tag <Category> is missing.");
                        System.out.println("ERROR: Category missing.");
                        enrollXmlRecord.setEntryCategory(null);
                    }

                }

                // ===================== Intake Category =====================
                System.out.println("Reading <Intake_Category>...");
                tagValue = hm.get("Intake_Category");
                if (Utils.getPao().getPaoCode().equals("94")) {
                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        if (((String) tagValue).trim().length() <= 9) {
                            enrollXmlRecord.setEntryCategory(((String) tagValue).trim());
                            System.out.println("Intake Category: " + tagValue);
                        } else {
                            rejReason.append("Value at Tag <Category> is more than 9 Characters.");
                            System.out.println("ERROR: Intake_Category too long");
                            enrollXmlRecord.setEntryCategory(null);
                        }
                    } else {
                        rejReason.append("Tag <Category> is missing.");
                        System.out.println("ERROR: Intake_Category missing.");
                        enrollXmlRecord.setEntryCategory(null);
                    }
                }

                // ===================== Remarks6 (Civil Edu - PAO 94 Only) =====================
                System.out.println("Reading <Remarks6>...");
                tagValue = hm.get("Remarks6");
                if (Utils.getPao().getPaoCode().equals("94")) {
                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        enrollXmlRecord.setCivilEdu(((String) tagValue).trim());
                        System.out.println("Civil Edu: " + tagValue);
                    } else {
                        enrollXmlRecord.setCivilEdu(null);
                        System.out.println("Civil Edu missing (ignored)");
                    }
                }

                // ===================== Religion_For_PAO =====================
                System.out.println("Reading <Religion_For_PAO>...");
                tagValue = hm.get("Religion_For_PAO");
                String gsoCode = "";

                if (tagValue != null && ((String) tagValue).trim().length() > 0) {

                    if (((String) tagValue).contains("/")) {
                        int pos = ((String) tagValue).lastIndexOf('/');
                        String religion = ((String) tagValue).substring(pos + 1);

                        if (religion == null || religion.isEmpty())
                            religion = "OTHERS";

                        gsoCode = religion.substring(0, 1);

                    } else {
                        gsoCode = ((String) tagValue).toUpperCase().substring(0, 1);
                    }

                    if (gsoCode.equals("G") || gsoCode.equals("S")) {
                        enrollXmlRecord.setGsoCode(gsoCode);
                    } else {
                        enrollXmlRecord.setGsoCode("N");
                    }

                    System.out.println("GSO Code: " + enrollXmlRecord.getGsoCode());

                } else {
                    rejReason.append("Tag <Class> is missing.");
                    System.out.println("ERROR: Religion_For_PAO missing.");
                    enrollXmlRecord.setGsoCode(null);
                }

                // ===================== Civil_Edu for other PAOs =====================
                System.out.println("Reading <Civil_Edu>...");
                tagValue = hm.get("Civil_Edu");
                if (!Utils.getPao().getPaoCode().equals("94")) {
                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        enrollXmlRecord.setCivilEdu(((String) tagValue).trim());
                        System.out.println("Civil Edu: " + tagValue);
                    } else {
                        rejReason.append("Tag <Civil_Edu> is missing.");
                        System.out.println("ERROR: Civil_Edu missing.");
                        enrollXmlRecord.setCivilEdu(null);
                    }
                }

                // ===================== Food_Bill (DA Days/Hours) =====================
                System.out.println("Reading <Food_Bill>...");
                tagValue = hm.get("Food_Bill");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {

                    if (((String) tagValue).trim().contains("/")) {
                        String[] daysHours = ((String) tagValue).trim().split("/", 2);

                        if (Utils.isValidNumber(daysHours[0])) {
                            enrollXmlRecord.setDaDays(Integer.parseInt(daysHours[0]));
                            System.out.println("DA Days: " + daysHours[0]);
                        } else {
                            rejReason.append("Non-Numeric Value for DA_Days.");
                            System.out.println("ERROR: Invalid DA Days");
                            enrollXmlRecord.setDaDays(null);
                        }

                        if (Utils.isValidNumber(daysHours[1])) {
                            enrollXmlRecord.setDaHours(Integer.parseInt(daysHours[1]));
                            System.out.println("DA Hours: " + daysHours[1]);
                        } else {
                            rejReason.append("Non-Numeric Value for DA_Hours.");
                            System.out.println("ERROR: Invalid DA Hours");
                            enrollXmlRecord.setDaHours(null);
                        }

                    } else {

                        if (Utils.isValidNumber((String) tagValue)) {
                            enrollXmlRecord.setDaDays(Integer.parseInt((String) tagValue));
                            enrollXmlRecord.setDaHours(0);
                            System.out.println("DA Days: " + tagValue);
                        } else {
                            rejReason.append("Invalid DA_Days_Hours.");
                            System.out.println("ERROR: Invalid DA value");
                            enrollXmlRecord.setDaDays(null);
                            enrollXmlRecord.setDaHours(null);
                        }
                    }

                } else {
                    rejReason.append("Tag <DA_Days_Hours> is missing.");
                    System.out.println("ERROR: Food_Bill missing.");
                    enrollXmlRecord.setDaDays(null);
                }

                // ===================== Bank Account =====================
                System.out.println("Reading <Bank_ACNO>...");
                tagValue = hm.get("Bank_ACNO");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    enrollXmlRecord.setBankAcno(((String) tagValue).trim());
                    System.out.println("Bank AC: " + tagValue);
                } else {
                    rejReason.append("Tag <Bank_ACNO> is missing.");
                    System.out.println("ERROR: Bank AC missing.");
                    enrollXmlRecord.setBankAcno(null);
                }

                // ===================== IFSC Code =====================
                System.out.println("Reading <IFSC_Code>...");
                tagValue = hm.get("IFSC_Code");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    enrollXmlRecord.setIfscCode(((String) tagValue).trim());
                    System.out.println("IFSC: " + tagValue);
                } else {
                    rejReason.append("Tag <IFSC_Code> is missing.");
                    System.out.println("ERROR: IFSC missing.");
                    enrollXmlRecord.setIfscCode(null);
                }

                // ===================== PAN =====================
                System.out.println("Reading <PAN_No>...");
                tagValue = hm.get("PAN_No");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    enrollXmlRecord.setPanNo(((String) tagValue).trim());
                    System.out.println("PAN: " + tagValue);
                } else {
                    System.out.println("PAN missing (allowed)");
                    enrollXmlRecord.setPanNo(null);
                }

                // ===================== Aadhaar =====================
                System.out.println("Reading <Aadhaar_No>...");
                tagValue = hm.get("Aadhaar_No");
                if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                    enrollXmlRecord.setAadhaarNo(((String) tagValue).trim());
                    System.out.println("Aadhaar: " + tagValue);
                } else {
                    System.out.println("Aadhaar missing (allowed)");
                    enrollXmlRecord.setAadhaarNo(null);
                }

                // ===================== Willing =====================
                System.out.println("Reading <Willing>...");
                tagValue = hm.get("Willing");
                if (Utils.getPao().getPaoCode().equals("94")) {

                    if (tagValue != null) {
                        if (((String) tagValue).equals("Yes")) {
                            enrollXmlRecord.setWilling(true);
                            System.out.println("Willing: Yes");
                        } else {
                            enrollXmlRecord.setWilling(false);
                            System.out.println("Willing: No");
                        }
                    } else {
                        rejReason.append("Value at Tag <Willing> is missing.");
                        System.out.println("ERROR: Willing missing.");
                        enrollXmlRecord.setWilling(false);
                    }

                }

                // ===================== Provisional Recovery =====================
                System.out.println("Reading <Provisional_Recovery>...");
                tagValue = hm.get("Provisional_Recovery");
                if (Utils.getPao().getPaoCode().equals("94")) {

                    if (tagValue != null) {
                        if (((String) tagValue).trim().length() > 0) {
                            int provRec = Integer.parseInt((String) tagValue);
                            enrollXmlRecord.setProvisionalRecovery(provRec);
                            System.out.println("Provisional Recovery: " + provRec);
                        } else {
                            enrollXmlRecord.setProvisionalRecovery(null);
                            System.out.println("Provisional Recovery empty");
                        }
                    } else {
                        enrollXmlRecord.setProvisionalRecovery(0);
                        System.out.println("Provisional Recovery missing -> default 0");
                    }

                }

                // ===================== Email_Id =====================
                System.out.println("Reading <Email_Id> (general)...");
                tagValue = hm.get("Email_Id");
                if (!Utils.getPao().getPaoCode().equals("94")) {

                    if (tagValue != null && ((String) tagValue).trim().length() > 0) {
                        enrollXmlRecord.setEmailId(((String) tagValue).trim());
                        System.out.println("Email_Id: " + tagValue);
                    } else {
                        rejReason.append("Value at Tag <Email_Id> is missing.");
                        System.out.println("ERROR: Email_Id missing.");
                        enrollXmlRecord.setEmailId(null);
                    }
                }

                // ===================== SAVE / REJECT =====================
                if (rejReason.toString().trim().length() <= 0) {
                    enrollXmlRecord.setXmlFileName(new File(fileName).getName());
                    enrollXmlDataList.add(enrollXmlRecord);
                    System.out.println("ROW VALID → Added to enrollXmlDataList");
                } else {

                    erroneousXmlRecord.setCreatedAt(DateManager.getCurrentTimestamp());
                    erroneousXmlRecord.setDo2Description((String) description);
                    erroneousXmlRecord.setArmyNo((String) ano);
                    erroneousXmlRecord.setCheckDigit((String) cd);
                    erroneousXmlRecord.setDo2ItemNo(
                            ((String) enrollXmlRecord.getDo2ItemNo()).substring(
                                    ((String) enrollXmlRecord.getDo2ItemNo()).length() - 3
                            )
                    );

                    erroneousXmlRecord.setPao(Utils.getPao());
                    erroneousXmlRecord.setUsr(usr);
                    erroneousXmlRecord.setDo2Control(dc);
                    erroneousXmlRecord.setRejectionReason(rejReason.toString());

                    System.out.println("ROW REJECTED → " + rejReason.toString());

                    if (!dxirDataList.contains(erroneousXmlRecord)) {
                        dxirDataList.add(erroneousXmlRecord);
                    }
                }

            } catch (Exception e) {
                System.out.println("ERROR processing row " + rowCount);
                e.printStackTrace();
            }
        }

        obj[0] = enrollXmlDataList;
        obj[1] = dxirDataList.isEmpty() ? null : dxirDataList;

        System.out.println("\n========== IMPORT COMPLETE ==========");
        System.out.println("Valid Records: " + enrollXmlDataList.size());
        System.out.println("Rejected Records: " + (dxirDataList == null ? 0 : dxirDataList.size()));
        System.out.println("=======================================");

        return obj;
    }
    private static String toStr(Object obj) {
        return obj == null ? null : obj.toString().trim();
    }

    private static String toStr(Object obj, String defaultVal) {
        return obj == null ? defaultVal : obj.toString().trim();
    }

    private static java.sql.Date toSqlDate(Object obj) {
        try {

            if (obj == null) return null;

            // Case 1: Already a Date
            if (obj instanceof Date) {
                return new java.sql.Date(((Date) obj).getTime());
            }

            // Case 2: Comes as String like "10/07/2025"
            if (obj instanceof String) {
                String str = ((String) obj).trim();
                if (str.isEmpty() || str.equals("00000000")) return null;

                if (str.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    return new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(str).getTime());
                }

                if (str.matches("\\d{8}")) { // 10072025
                    return new java.sql.Date(new SimpleDateFormat("ddMMyyyy").parse(str).getTime());
                }
            }

            // Case 3: Comes as LinkedHashMap like {value=20/11/2003}
            if (obj instanceof Map) {
                Object v = ((Map<?,?>) obj).get("value");
                if (v != null) {
                    return toSqlDate(v.toString());
                }
            }

            // No valid format
            return null;

        } catch (Exception e) {
            System.out.println("toSqlDate() parse failed: " + e.getMessage());
            return null;
        }
    }


    public static Integer insertDataIntoEnrollDo2(List<EnrollDo2XmlDataObject> enrollXmlDataList, String batchNo) {

        System.out.println("=== insertDataIntoEnrollDo2 START, Records: " + enrollXmlDataList.size() + " ===");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        int count = 0;

        if (enrollXmlDataList.isEmpty()) {
            System.out.println("No records to insert.");
            return 0;
        }

        try {
            for (int s = 0; s < enrollXmlDataList.size(); s++) {

                EnrollDo2XmlDataObject xmlRecord = enrollXmlDataList.get(s);
                System.out.println("\n---- Processing XML Record #" + (s + 1) + " ----");

                // =============================
                // DECLARE ALL VARIABLES (unchanged)
                // =============================
                Integer bNo = null, daDays = 0, daHours = 0, gratuityAmount = 0, fkUnit = null, fkEntryCategory = null;
                String unitCode = null, armyNo = null, checkDigit = null, do2Group = null, do2Type = null;
                String do2No = null, do2ItemNo = null, empName = null, termsColourService = null;
                String termsMaxAgeRetention = null, entryCategory = null, gsoCode = null, mobileNo = null;
                String emailId = null, uidNo = null, civilEducation = null, bankAccountNo = null;
                String ifscCode = null, panNumber = null, status = null, reason = null, transType = "D";
                String qe = null, entryTrade = null, arrivalTime = null, departureTime = null, gender = null;

                Date do2Date = null, enrollmentDate = null, dob = null, dateOfDeparture = null, dateOfArrival = null;

                Boolean gratuityWillingness = false, commutationWillingness = false, approved = false;

                // =============================
                // POPULATE FIELDS SAFELY (unchanged)
                // =============================
                // ... (all your population code stays exactly the same – no changes here)

                bNo = xmlRecord.getBatchNo() instanceof Integer ? (Integer) xmlRecord.getBatchNo() : null;

                unitCode = toStr(xmlRecord.getUnitCode());
                armyNo = toStr(xmlRecord.getArmyNo());
                checkDigit = toStr(xmlRecord.getCheckDigit());

                do2Group = toStr(xmlRecord.getDo2Group());
                do2Type = toStr(xmlRecord.getDo2Type());
                do2No = toStr(xmlRecord.getDo2No());

                // ITEM NO (pad to 3 digits)
                if (xmlRecord.getDo2ItemNo() != null) {
                    String it = xmlRecord.getDo2ItemNo().toString();
                    do2ItemNo = (it.length() <= 3) ? padNumData(it, 3) : it.substring(it.length() - 3);
                }

                // DO2 Date
                if (xmlRecord.getDo2Date() != null) {
                    do2Date = formatter.parse(xmlRecord.getDo2Date().toString());
                }

                // Employee Name last 25 chars
                if (xmlRecord.getEmployeeName() != null) {
                    String nm = xmlRecord.getEmployeeName().toString();
                    empName = nm.length() <= 25 ? nm : nm.substring(nm.length() - 25);
                }

                // Enrollment Date
                enrollmentDate = toSqlDate(xmlRecord.getEnrollmentDate());

                termsColourService = toStr(xmlRecord.getColorService());
                termsMaxAgeRetention = toStr(xmlRecord.getMaxAge());

                // DOB
                dob = toSqlDate(xmlRecord.getDateOfBirth());

                // FIX DATE OF DEPARTURE
                if (xmlRecord.getDateOfDeparture() instanceof Date) {
                    dateOfDeparture = toSqlDate(xmlRecord.getDateOfDeparture());
                } else {
                    System.out.println("Date of Departure NULL or invalid → setting NULL");
                }

                entryCategory = toStr(xmlRecord.getEntryCategory());
                gsoCode = toStr(xmlRecord.getGsoCode());

                daDays = xmlRecord.getDaDays() instanceof Integer ? (Integer) xmlRecord.getDaDays() : 0;
                daHours = xmlRecord.getDaHours() instanceof Integer ? (Integer) xmlRecord.getDaHours() : 0;

                mobileNo = toStr(xmlRecord.getMobileNo());
                emailId = toStr(xmlRecord.getEmailId());
                uidNo = toStr(xmlRecord.getAadhaarNo());
                civilEducation = toStr(xmlRecord.getCivilEdu());
                bankAccountNo = toStr(xmlRecord.getBankAcno());
                ifscCode = toStr(xmlRecord.getIfscCode());
                panNumber = toStr(xmlRecord.getPanNo());

                status = toStr(xmlRecord.getStatus());
                reason = toStr(xmlRecord.getReason());
                transType = toStr(xmlRecord.getTranscriptionType(), "D");
                qe = toStr(xmlRecord.getQe(), Utils.getCurrentQuarter());

                entryTrade = toStr(xmlRecord.getEntryTrade());

                gratuityWillingness = xmlRecord.getWilling() instanceof Boolean ? (Boolean) xmlRecord.getWilling() : false;
                gratuityAmount = xmlRecord.getProvisionalRecovery() instanceof Integer ? (Integer) xmlRecord.getProvisionalRecovery() : 0;

                arrivalTime = toStr(xmlRecord.getArrivalTime());
                departureTime = toStr(xmlRecord.getDepartureTime());
                gender = toStr(xmlRecord.getGender());

                // FIX DATE OF ARRIVAL
                dateOfArrival = toSqlDate(xmlRecord.getDateOfArrival());

                // Lookups
                try {
                    if (unitCode != null) {
                        Unit unit = unitRepo.findByUnitCode(unitCode);
                        fkUnit = unit != null ? unit.getId() : null;
                    }
                } catch (Exception e) {
                    System.out.println("Unit lookup failed.");
                }

                if (entryCategory != null) {
                    EntryCategory ecat = entryCatRepo.findByEntryCategory(entryCategory);
                    fkEntryCategory = (ecat != null) ? ecat.getId() : 5;
                }
              
                System.out.println("\n========== FINAL VALUES BEFORE INSERT ==========");

                System.out.println("bNo                 = "    + bNo                + "  (" + typeOf(bNo) + ")");
                System.out.println("unitCode            = "    + unitCode           + "  (" + typeOf(unitCode) + ")");
                System.out.println("armyNo              = "    + armyNo             + "  (" + typeOf(armyNo) + ")");
                System.out.println("checkDigit          = "    + checkDigit         + "  (" + typeOf(checkDigit) + ")");
                System.out.println("do2Group            = "    + do2Group           + "  (" + typeOf(do2Group) + ")");
                System.out.println("do2Type             = "    + do2Type            + "  (" + typeOf(do2Type) + ")");
                System.out.println("do2No               = "    + do2No              + "  (" + typeOf(do2No) + ")");
                System.out.println("do2ItemNo           = "    + do2ItemNo          + "  (" + typeOf(do2ItemNo) + ")");
                System.out.println("do2Date             = "    + do2Date            + "  (" + typeOf(do2Date) + ")");
                System.out.println("empName             = "    + empName            + "  (" + typeOf(empName) + ")");
                System.out.println("enrollmentDate      = "    + enrollmentDate     + "  (" + typeOf(enrollmentDate) + ")");
                System.out.println("termsColourService  = "    + termsColourService + "  (" + typeOf(termsColourService) + ")");
                System.out.println("termsMaxAge         = "    + termsMaxAgeRetention        + "  (" + typeOf(termsMaxAgeRetention) + ")");
                System.out.println("dob                 = "    + dob                + "  (" + typeOf(dob) + ")");
                System.out.println("dateOfDeparture     = "    + dateOfDeparture    + "  (" + typeOf(dateOfDeparture) + ")");
                System.out.println("entryCategory       = "    + entryCategory      + "  (" + typeOf(entryCategory) + ")");
                System.out.println("gsoCode             = "    + gsoCode            + "  (" + typeOf(gsoCode) + ")");
                System.out.println("daDays              = "    + daDays             + "  (" + typeOf(daDays) + ")");
                System.out.println("daHours             = "    + daHours            + "  (" + typeOf(daHours) + ")");
                System.out.println("mobileNo            = "    + mobileNo           + "  (" + typeOf(mobileNo) + ")");
                System.out.println("emailId             = "    + emailId            + "  (" + typeOf(emailId) + ")");
                System.out.println("uidNo               = "    + uidNo              + "  (" + typeOf(uidNo) + ")");
                System.out.println("civilEducation      = "    + civilEducation     + "  (" + typeOf(civilEducation) + ")");
                System.out.println("bankAccountNo       = "    + bankAccountNo      + "  (" + typeOf(bankAccountNo) + ")");
                System.out.println("ifscCode            = "    + ifscCode           + "  (" + typeOf(ifscCode) + ")");
                System.out.println("panNumber           = "    + panNumber          + "  (" + typeOf(panNumber) + ")");
                System.out.println("status              = "    + status             + "  (" + typeOf(status) + ")");
                System.out.println("reason              = "    + reason             + "  (" + typeOf(reason) + ")");
                System.out.println("transType           = "    + transType          + "  (" + typeOf(transType) + ")");
                System.out.println("qe                  = "    + qe                 + "  (" + typeOf(qe) + ")");
                System.out.println("approved            = "    + approved           + "  (" + typeOf(approved) + ")");
                System.out.println("entryTrade          = "    + entryTrade         + "  (" + typeOf(entryTrade) + ")");
                System.out.println("commutationWilling. = "    + commutationWillingness + "  (" + typeOf(commutationWillingness) + ")");
                System.out.println("gratuityWillingness = "    + gratuityWillingness     + "  (" + typeOf(gratuityWillingness) + ")");
                System.out.println("gratuityAmount      = "    + gratuityAmount     + "  (" + typeOf(gratuityAmount) + ")");
                System.out.println("arrivalTime         = "    + arrivalTime        + "  (" + typeOf(arrivalTime) + ")");
                System.out.println("departureTime       = "    + departureTime      + "  (" + typeOf(departureTime) + ")");
                System.out.println("gender              = "    + gender             + "  (" + typeOf(gender) + ")");
                System.out.println("dateOfArrival       = "    + dateOfArrival      + "  (" + typeOf(dateOfArrival) + ")");
                System.out.println("fkUnit              = "    + fkUnit             + "  (" + typeOf(fkUnit) + ")");
                System.out.println("fkEntryCategory     = "    + fkEntryCategory    + "  (" + typeOf(fkEntryCategory) + ")");
                System.out.println("=====================================================\n");
                EnrollDo2 enrollDo2 = new EnrollDo2();  // Fresh entity

                // Set all fields (your vars map directly – nulls handled perfectly)
                enrollDo2.setBatchNo(bNo);
                enrollDo2.setUnitCode(unitCode);
                enrollDo2.setArmyNo(armyNo);
                enrollDo2.setCheckDigit(checkDigit);
                enrollDo2.setDo2Group(do2Group);
                enrollDo2.setDo2Type(do2Type);
                enrollDo2.setDo2No(do2No);
                enrollDo2.setDo2ItemNo(do2ItemNo);
                enrollDo2.setDo2Date(do2Date);  // Null OK – no bytea!
                enrollDo2.setEmployeeName(empName);
                enrollDo2.setEnrollmentDate(enrollmentDate);  // Null OK
                enrollDo2.setTermsColourService(termsColourService);
                enrollDo2.setTermsMaxAgeRetention(termsMaxAgeRetention);
                enrollDo2.setDateOfBirth(dob);  // Null OK
                enrollDo2.setDateOfArrival(dateOfArrival);  // Null OK
                enrollDo2.setEntryCategory(entryCategory);
                enrollDo2.setGsoCode(gsoCode);
                enrollDo2.setDaDays(daDays);
                enrollDo2.setDaHours(daHours);
                enrollDo2.setMobileNo(mobileNo);
                enrollDo2.setEmailId(emailId);
                enrollDo2.setUidNo(uidNo);
                enrollDo2.setCivilEducation(civilEducation);
                enrollDo2.setBankAccountNo(bankAccountNo);
                enrollDo2.setIfscCode(ifscCode);
                enrollDo2.setPanNumber(panNumber);
                enrollDo2.setStatus(status);
                enrollDo2.setReason(reason);
                enrollDo2.setTranscriptionType(transType);
                enrollDo2.setQe(qe);
                enrollDo2.setApproved(approved);
                enrollDo2.setEntryTrade(entryTrade);
                enrollDo2.setCommutationWillingness(commutationWillingness);
                enrollDo2.setGratuityWillingness(gratuityWillingness);
                enrollDo2.setGratuityAmount(gratuityAmount);
                enrollDo2.setArrivalTime(arrivalTime);
                enrollDo2.setDepartureTime(departureTime);
                enrollDo2.setGender(gender);
                enrollDo2.setDateOfDeparture(dateOfDeparture);  // Null = proper NULL date – bye bytea!

                // Set FKs (relations – JPA handles lazy load)
                if (fkUnit != null) {
                    Unit unit = new Unit();  // Stub or fetch full if needed
                    unit.setId(fkUnit);
                    enrollDo2.setUnit(unit);
                }
                if (fkEntryCategory != null) {
                    EntryCategory ecat = new EntryCategory();  // Stub
                    ecat.setId(fkEntryCategory);
                    enrollDo2.setEntryCategory_1(ecat);
                }

                // SAVE! (JPA magic – generates INSERT, binds correctly)
                EnrollDo2 saved = enrollDo2Repo.save(enrollDo2);  // Returns saved entity with ID
                System.out.println("✅ SAVED Record #" + (s + 1) + " (ID: " + saved.getId() + ") for ArmyNo: " + armyNo);

                count++;

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("=== insertDataIntoEnrollDo2 END, Records Inserted: " + count + " ===");
        return count;
    }


    /** Helper: Print object type */
    private static String typeOf(Object o) {
        return (o == null) ? "NULL" : o.getClass().getName();
    }


    /*public static Integer insertDataIntoEnrollDo2(List<EnrollDo2XmlDataObject> enrollXmlDataList, String batchNo) {
			 
			 
			List<Do2Control> dcList = null;
			List<Do2XmlRejectedItems> dxriList = null;
			String[] month = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
					"Aug", "Sep", "Oct", "Nov", "Dec" };
			final int[] mth = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
			Connection con = null;
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			java.util.Date d = null;
			java.sql.Date date = null;
			 
			Do2Control dc = null;
			int count = 0;
			if (enrollXmlDataList.size() > 0) {
				try {
					 
					 
					 
				 
							 
					
					String unitCode,armyNo,checkDigit,do2Group,do2Type,do2No,do2ItemNo,punishment,description,colA,colB,colC,refDo2No,status,reason,transType,qe;
					String inputQe,attachment,empName,termsColourService,termsMaxAge,entryCategory,gsoCode,mobileNo,emailId,uidNo,civilEducation,bankAccountNo;
					String ifscCode,panNumber,entryTrade,arrivalTime,departureTime,gender = null;
					Date do2Date,fromDate,toDate,enrollmentDate,dob,dateOfDeparture,dateOfArrival;
					Double rate;
					Integer tjra,lra,fkUnit,bNo,daDays,daHours,gratuityAmount,fkEntryCategory;
					Long fkEmp;
					Boolean commutationWillingness,approved,gratuityWillingness;
					
								
					
					for (int s = 0; s < enrollXmlDataList.size(); s++) {
						
						unitCode=null;armyNo=null;checkDigit=null;do2Group=null;do2Type=null;do2No=null;do2ItemNo=null;punishment=null;description=null;
						colA=null;colB=null;colC=null;refDo2No=null;status=null;reason=null;transType=null;qe=null;
						inputQe=null;attachment=null;do2Date=null;fromDate=null;toDate=null;rate=null;tjra=null;lra=null;fkUnit=null;bNo=null;
						fkEmp=null;empName=null;enrollmentDate=null;termsColourService=null;termsMaxAge=null;dob=null;dateOfDeparture=null;
						entryCategory=null;gsoCode=null;mobileNo=null;emailId=null;uidNo=null;civilEducation=null;bankAccountNo=null;
						ifscCode=null;panNumber=null;entryTrade=null;daDays=null;daHours=null;commutationWillingness=false;approved=false;gratuityWillingness=false;
						arrivalTime=null;departureTime=null;dateOfArrival=null;gratuityAmount=null;fkEntryCategory=null;
						
						EnrollDo2XmlDataObject xmlRecord = enrollXmlDataList.get(s);
						count++;
						
	*//*					System.out.println("================== xmlRecord : " + xmlRecord.getDescription() + "\t ; " +
								xmlRecord.getArmyNo() + "\t : " + xmlRecord.getEmployeeName());
	*//*
						if (xmlRecord.getDescription() != null && (xmlRecord.getDescription().equals("ENROL") || xmlRecord.getDescription().equals("REENRL"))) {
							
							
							bNo=(Integer)xmlRecord.getBatchNo();
							unitCode=(String)xmlRecord.getUnitCode();
							armyNo=(String)xmlRecord.getArmyNo();
							checkDigit=(String)xmlRecord.getCheckDigit();
							do2Group=(String)xmlRecord.getDo2Group();
							do2Type=(String)xmlRecord.getDo2Type();
							do2No=(String)xmlRecord.getDo2No();
							*//*if (((String)xmlRecord.getDo2ItemNo()).length() <= 4) {
								stmt.setString(8, padNumData(((String)xmlRecord.getDo2ItemNo()), 4));
							} else {
								stmt.setString(8, ((String)xmlRecord.getDo2ItemNo()).substring(0, 4));
							}
							*//*
							if (((String) xmlRecord.getDo2ItemNo()).length() <= 3) {
								do2ItemNo=padNumData((String) xmlRecord.getDo2ItemNo(), 3);
							} else {
								do2ItemNo=((String) xmlRecord.getDo2ItemNo()).substring(((String) xmlRecord
												.getDo2ItemNo()).length() - 3);
							}
							if (xmlRecord.getDo2Date() == null) {
								do2Date=null;;
							} else {
								d = formatter.parse((String)xmlRecord.getDo2Date());
								do2Date = new java.sql.Date(d.getTime());
								 
							}
							int len = ((String)xmlRecord.getEmployeeName()).length();
							if (len <= 25) {
								empName= (String)xmlRecord.getEmployeeName();
							} else {
								empName=((String)xmlRecord.getEmployeeName()).substring(len - 25);
							}
							if (xmlRecord.getEnrollmentDate() == null) {
								enrollmentDate=null;
							} else {
								d = (Date)xmlRecord.getEnrollmentDate();
								enrollmentDate = new java.sql.Date(d.getTime());
								 
							}
							termsColourService=(String)xmlRecord.getColorService();
							termsMaxAge=(String)xmlRecord.getMaxAge();
							if (xmlRecord.getDateOfBirth() == null) {
								 ;
							} else {
								d = (Date)xmlRecord.getDateOfBirth();
								dob = new java.sql.Date(d.getTime());
								 
							}
							if (xmlRecord.getDateOfDeparture() == null) {
								dateOfDeparture=null;
							} else {
							
								d = (Date)xmlRecord.getDateOfDeparture();
							//	System.out.println("-----------Date of Departure ---------:" + d);
								dateOfDeparture = new java.sql.Date(d.getTime());
								 
							}
						//	stmt.setInt(15, (Integer)xmlRecord.getAdvanceAmount());
	*//*						System.out.println("******************** (String)xmlRecord.getEntryCategory() : " +
									((String)xmlRecord.getEntryCategory()).trim().length());
	*//*
							entryCategory=(String)xmlRecord.getEntryCategory();
							gsoCode=(String)xmlRecord.getGsoCode();
							daDays=(Integer)xmlRecord.getDaDays();
							daHours=(Integer)xmlRecord.getDaHours();
							mobileNo=(String)xmlRecord.getMobileNo();
							
							 
							emailId=(String)xmlRecord.getEmailId();
							uidNo=(String)xmlRecord.getAadhaarNo();
							civilEducation=(String)xmlRecord.getCivilEdu();
							bankAccountNo=(String)xmlRecord.getBankAcno();
							ifscCode= (String)xmlRecord.getIfscCode();
							panNumber=(String)xmlRecord.getPanNo();
							
							status=(String)xmlRecord.getStatus();
							reason=(String)xmlRecord.getReason();
							transType=(String)xmlRecord.getTranscriptionType();
							qe=(String)xmlRecord.getQe();
							 
							entryTrade=(String)xmlRecord.getEntryTrade();
							 
							gratuityWillingness=(Boolean)xmlRecord.getWilling();
							gratuityAmount= (Integer)xmlRecord.getProvisionalRecovery();
							
							arrivalTime=(String)xmlRecord.getArrivalTime();
							departureTime=(String)xmlRecord.getDepartureTime();
							
							gender=(String)xmlRecord.getGender();
							
							if (xmlRecord.getDateOfArrival() == null) {
								dateOfArrival=null;
							} else {
							
								d = (Date)xmlRecord.getDateOfArrival();
							//	System.out.println("-----------Date of Departure ---------:" + d);
								dateOfArrival = new java.sql.Date(d.getTime());
								 
							}
							
							if (xmlRecord.getUnitCode() == null) {
								fkUnit=null;
							} else {
								Unit unit =unitRepo.findByUnitCode((String)xmlRecord.getUnitCode());
								if (unit != null) {
									fkUnit=unit.getId();
								} else {
									fkUnit=null;
								}
							}
							// System.out.println("entry category " +
							// xmlRecord.getEntryCategory());
							if (xmlRecord.getEntryCategory() == null) {
								fkEntryCategory=null;
							} else {
								EntryCategory ecat = entryCatRepo.findByEntryCategory((String)xmlRecord.getEntryCategory());
								if (ecat != null) {
									fkEntryCategory=ecat.getId();
								} else {
								//	fkEntryCategory=null;
									fkEntryCategory=5;  // Entry Category not properly given in importdata.Hence NER being set.
								}
							} 
							if (transType==null || (transType!=null && !transType.equals("D"))) {
								transType="D";
							}
							if (inputQe==null) {
								inputQe=Utils.getCurrentQuarter();
							}
							
							enrollDo2Repo.insertEnrollDo2(bNo, unitCode, armyNo, checkDigit, do2Group, do2Type, do2No, do2ItemNo, do2Date, empName, enrollmentDate, termsColourService, termsMaxAge, dob, dateOfDeparture, entryCategory, gsoCode, daDays, daHours, mobileNo, emailId, uidNo, civilEducation, bankAccountNo, ifscCode, panNumber, status, reason, transType, inputQe, approved, entryTrade, commutationWillingness, gratuityWillingness, gratuityAmount, arrivalTime, departureTime, gender, dateOfArrival, fkUnit, fkEntryCategory);
							 
						} *//* else if (xmlRecord.getDescription() != null && xmlRecord.getDescription().equals("CANCEL")) {
							bNo=Integer.parseInt((String)xmlRecord.getBatchNo());
							unitCode=(String)xmlRecord.getUnitCode();
							armyNo=(String)xmlRecord.getArmyNo();
							checkDigit=(String)xmlRecord.getCheckDigit();
							do2Group=(String)xmlRecord.getDo2Group();
							do2Type=(String)xmlRecord.getDo2Type();
							do2No=(String)xmlRecord.getDo2No();
							if (((String)xmlRecord.getDo2ItemNo()).length() <= 3) {
								do2ItemNo=padNumData((String)xmlRecord.getDo2ItemNo(), 3);
							} else {
								do2ItemNo=((String)xmlRecord.getDo2ItemNo()).substring(1);
							}
							if (xmlRecord.getDo2Date() == null) {
								do2Date=null;
							} else {
								d = (Date)xmlRecord.getDo2Date();
								do2Date = new java.sql.Date(d.getTime());
								 
							}
							 
							description=(String)xmlRecord.getDescription();
							if (xmlRecord.getFromDate() == null) {
								fromDate=null;
							} else {
								d = (Date)xmlRecord.getFromDate();
								fromDate = new java.sql.Date(d.getTime());
								 
							}
							 
							rate=0.0; 
							tjra=0;
							lra=0;
							colA=(String)xmlRecord.getColumnA();
							 
							refDo2No=(String)xmlRecord.getRefDo2No();
							status=(String)xmlRecord.getStatus();
							reason=(String)xmlRecord.getReason();
							transType=(String)xmlRecord.getTranscriptionType();
							qe=(String)xmlRecord.getQe();
							if (xmlRecord.getArmyNo() == null) {
								fkEmp=null;
							} else {
								Employee employee = Utils.getCurrentEmployee((String)xmlRecord.getArmyNo());
								if (employee != null) {
									fkEmp=employee.getId();
								}  
							}
							qe=(String)xmlRecord.getQe();
							if (xmlRecord.getUnitCode() == null) {
								fkUnit=null;
							} else {
								Unit unit = unitRepo.findByUnitCode( (String)xmlRecord.getUnitCode()); 
								if (unit != null) {
									fkUnit=unit.getId();
								}  
							}
							do2Repo.insertDo2FromDo2Control(unitCode, armyNo, gender, do2Group, do2Type, do2No, do2ItemNo, do2Date, punishment, description, fromDate, toDate, rate, tjra, lra, colA, colB, colC, refDo2No, status, reason, transType, qe, fkEmp, inputQe, fkUnit, bNo, attachment);
							 
						}*//*  // Commented on 23.03.2023 - During Timebar related updation
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			return count;
		}*/
		
	}
		
		
		
	
	
	/*
	 * 
	 * public static Object importEnrollXmlData(String fileName, String monthEnding,
	 * Boolean validDo2Items, Usr usr, Do2Control dc, String batchNo) { if
	 * (validDo2Items) { System.out.
	 * println("\t\t************importEnrollXmlData******************** file name : "
	 * + fileName + "\t validDo2Items : " + validDo2Items);
	 * System.out.println("\t\t******************************** file name : " +
	 * fileName); }
	 * 
	 * // Boolean addToList = true; List<EnrollDo2XmlDataObject> enrollXmlDataList =
	 * new ArrayList<EnrollDo2XmlDataObject>(); List<Do2XmlRejectedItems>
	 * dxirDataList = new ArrayList<Do2XmlRejectedItems>(); Document doc = null;
	 * List list = null; Attribute attribute = null; Object p2order_group,
	 * p2order_number, p2order_type, p2order_no, p2order_date, sus_number, army_no,
	 * ano, cd, enroll_date, pub_date, rootName, unitcd, color_service, max_age,
	 * birth_date, arrival_date, advance_amount, entry_category, entry_trade,
	 * da_days, gso_code, obsn, description, do2ItemNo; String[] gsoClass = null;
	 * ano = null; cd = null; StringBuffer rejReason; obsn = null; description =
	 * null; do2ItemNo = null; int no_of_items_in_do2; try { SAXReader reader = new
	 * SAXReader(); doc = reader.read(new File(fileName)); } catch (Exception e) {
	 * e.printStackTrace(); } Element rootElement = doc.getRootElement(); rootName =
	 * rootElement.getName() + "//Header//"; list = doc.selectNodes(rootName +
	 * "PTO_Group"); p2order_group = ((Element) list.get(0)).getText(); list =
	 * doc.selectNodes(rootName + "PTO_No"); p2order_number = ((Element)
	 * list.get(0)).getText(); p2order_type = ((String) p2order_number).substring(0,
	 * 1); p2order_no = ((String) p2order_number).substring(2, 6); list =
	 * doc.selectNodes(rootName + "SUS_No"); sus_number = ((Element)
	 * list.get(0)).getText(); list = doc.selectNodes(rootName + "PTO_Date");
	 * p2order_date = ((Element) list.get(0)).getText(); pub_date = null; pub_date =
	 * ((String) p2order_date).substring(6) + "-" + ((String)
	 * p2order_date).substring(3, 5) + "-" + ((String) p2order_date).substring(0,
	 * 2); rootName = rootElement.getName() + "//PTO_Details//row//"; list =
	 * doc.selectNodes(rootName + "Item_No"); no_of_items_in_do2 = list.size();
	 * 
	 * unitcd = ""; List<Unit> unitList = Utils.getEntityManager().
	 * createQuery("SELECT u FROM Unit u WHERE u.susNo = :susno")
	 * .setParameter("susno", sus_number).getResultList(); if (unitList != null &&
	 * unitList.size() > 0) { unitcd = unitList.get(0).getUnitCode(); } Boolean
	 * separateMaxAge = null; List<HashMap<String, String>> hpItemList =
	 * readRows(new File(fileName)); //
	 * System.out.println("\t\t############## hpItemList size : " +
	 * hpItemList.size()); Object tagValue = null; for (HashMap<String, String> hm :
	 * hpItemList) { try { separateMaxAge = false; rejReason = new StringBuffer();
	 * // addToList = true; EnrollDo2XmlDataObject enrollXmlRecord = new
	 * EnrollDo2XmlDataObject(); Do2XmlRejectedItems erroneousXmlRecord = new
	 * Do2XmlRejectedItems(); enrollXmlRecord.setUnitCode(unitcd);
	 * enrollXmlRecord.setDo2Group(p2order_group);
	 * enrollXmlRecord.setDo2Type(p2order_type);
	 * enrollXmlRecord.setDo2No(p2order_no); enrollXmlRecord.setDo2Date(pub_date);
	 * enrollXmlRecord.setStatus("P");
	 * enrollXmlRecord.setReason("Batch to be cleared after verification");
	 * enrollXmlRecord.setQe(monthEnding);
	 * enrollXmlRecord.setBatchNo(Integer.parseInt(batchNo));
	 * enrollXmlRecord.setTranscriptionType("D");
	 * 
	 * tagValue = hm.get("Description"); if (tagValue != null && ((String)
	 * tagValue).trim().length() > 0) { description = (String) tagValue; if
	 * (Utils.getPao().getPaoCode().equals("94") && description.equals("REENRL"))
	 * description = "ENROL"; } else { description = null; }
	 * enrollXmlRecord.setDescription(description); tagValue = hm.get("Item_No"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) { do2ItemNo =
	 * padNumData((String)tagValue, 3); enrollXmlRecord.setDo2ItemNo(do2ItemNo); }
	 * else { // addToList = false; rejReason.append("Tag <Item_No> is missing.");
	 * enrollXmlRecord.setDo2ItemNo(null); } tagValue = hm.get("Pin"); if (tagValue
	 * != null && ((String) tagValue).trim().length() > 0) {
	 * if(((String)tagValue).trim().length() == 9) { ano = ((String)
	 * tagValue).substring(0, 8); cd = ((String) tagValue).substring(8);
	 * enrollXmlRecord.setArmyNo(ano); enrollXmlRecord.setCheckDigit(cd); } else {
	 * // addToList = false; rejReason.append("Invalid value at Tag <Pin>.");
	 * enrollXmlRecord.setArmyNo(null); enrollXmlRecord.setCheckDigit(null); } }
	 * else { // addToList = false; rejReason.append("Tag <Pin> is missing.");
	 * enrollXmlRecord.setArmyNo(null); enrollXmlRecord.setCheckDigit(null); }
	 * tagValue = hm.get("Trade"); if (tagValue != null && ((String)
	 * tagValue).trim().length() > 0) { if(((String)tagValue).trim().length() <= 9)
	 * { enrollXmlRecord.setEntryTrade((String)tagValue); } else { // addToList =
	 * false; rejReason.append("Value at Tag <Trade> more than 9 Characters.");
	 * enrollXmlRecord.setEntryTrade(null); } } else { // addToList = false;
	 * rejReason.append("Tag <Trade> is missing.");
	 * enrollXmlRecord.setEntryTrade(null); } tagValue = hm.get("Name"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) {
	 * if(((String)tagValue).trim().length() <= 25) {
	 * enrollXmlRecord.setEmployeeName((String)tagValue); } else {
	 * enrollXmlRecord.setEmployeeName(((String)tagValue).substring(0, 25)); } }
	 * else { // addToList = false; rejReason.append("Tag <Name> is missing.");
	 * enrollXmlRecord.setEmployeeName(null); } tagValue = hm.get("Enrol_Dt"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) { try { String
	 * target = ((String)tagValue); DateFormat df = new
	 * SimpleDateFormat("dd/MM/yyyy"); Date result = df.parse(target);
	 * enrollXmlRecord.setEnrollmentDate(result); } catch (ParseException pe) { //
	 * addToList = false; rejReason.append("Invalid value at Tag <Enrol_Dt>.");
	 * enrollXmlRecord.setEnrollmentDate(null); } } else {
	 * 
	 * // addToList = false; rejReason.append("Tag <Enrol_Dt> is missing.");
	 * enrollXmlRecord.setEnrollmentDate(null); } tagValue =
	 * hm.get("TOE_Colour_Service"); if (tagValue != null && ((String)
	 * tagValue).trim().length() > 0) { if(((String)tagValue).trim().contains("/"))
	 * { String[] toe_cs_ma = ((String)tagValue).trim().split("/", 2);
	 * if(Utils.isValidNumber(toe_cs_ma[0])) {
	 * enrollXmlRecord.setColorService(toe_cs_ma[0]); } else { // addToList = false;
	 * rejReason.append("Inappropriate Value Tag <TOE_Colour_Service>.");
	 * enrollXmlRecord.setColorService(null); }
	 * if(Utils.isValidNumber(toe_cs_ma[1])) {
	 * enrollXmlRecord.setMaxAge(toe_cs_ma[1]); } else { // addToList = false;
	 * rejReason.
	 * append("Inappropriate Max Age Value under Tag <TOE_Colour_Service>.");
	 * enrollXmlRecord.setColorService(null); } } else { separateMaxAge = true;
	 * if(Utils.isValidNumber((String)tagValue)) {
	 * enrollXmlRecord.setColorService((String)tagValue); } else { // addToList =
	 * false; rejReason.append("Inappropriate Value Tag <TOE_Colour_Service>.");
	 * enrollXmlRecord.setColorService(null); } } } else { // addToList = false;
	 * rejReason.append("Tag <TOE_Colour_Service> is missing.");
	 * enrollXmlRecord.setColorService(null); } if(separateMaxAge) { tagValue =
	 * hm.get("TOE_Max_Age"); if (tagValue != null && ((String)
	 * tagValue).trim().length() > 0) { if(Utils.isValidNumber((String)tagValue)) {
	 * enrollXmlRecord.setMaxAge((String)tagValue); } else { // addToList = false;
	 * rejReason.append("Inappropriate Value Tag <TOE_Max_Age>.");
	 * enrollXmlRecord.setMaxAge(null); } } else { // addToList = false;
	 * rejReason.append("Tag <TOE_Max_Age> is missing.");
	 * enrollXmlRecord.setMaxAge(null); } } tagValue = hm.get("DOB"); if (tagValue
	 * != null && ((String) tagValue).trim().length() > 0) { try { String target =
	 * ((String)tagValue); DateFormat df = new SimpleDateFormat("dd/MM/yyyy"); Date
	 * result = df.parse(target); enrollXmlRecord.setDateOfBirth(result); } catch
	 * (ParseException pe) { // addToList = false;
	 * rejReason.append("Invalid value at Tag <DOB>.");
	 * enrollXmlRecord.setDateOfBirth(null); } } else { // addToList = false;
	 * rejReason.append("Tag <DOB> is missing.");
	 * enrollXmlRecord.setDateOfBirth(null); } tagValue = hm.get("Arrival_Dt"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) { try { String
	 * target = ((String)tagValue); DateFormat df = new
	 * SimpleDateFormat("dd/MM/yyyy"); Date result = df.parse(target);
	 * enrollXmlRecord.setDateOfArrival(result); } catch (ParseException pe) { //
	 * addToList = false; rejReason.append("Invalid value at Tag <Arrival_Dt>.");
	 * enrollXmlRecord.setDateOfArrival(null); } } else { // addToList = false;
	 * rejReason.append("Tag <Arrival_Dt> is missing.");
	 * enrollXmlRecord.setDateOfBirth(null); } tagValue = hm.get("Cat_Entry"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) {
	 * if(((String)tagValue).trim().length() <= 9) {
	 * enrollXmlRecord.setEntryCategory(((String)tagValue).trim()); } else { //
	 * addToList = false;
	 * rejReason.append("Value at Tag <Cat_Entry> is more than 9 Characters.");
	 * enrollXmlRecord.setEntryCategory(null); } } else { // addToList = false;
	 * rejReason.append("Tag <Cat_Entry> is missing.");
	 * enrollXmlRecord.setEntryCategory(null); } tagValue = hm.get("Adv_Paid"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) {
	 * if(Utils.isValidNumber((String)tagValue)) {
	 * enrollXmlRecord.setAdvanceAmount(Integer.parseInt((String)tagValue)); } else
	 * { // addToList = false;
	 * rejReason.append("Inappropriate value at Tag <Adv_Paid>.");
	 * enrollXmlRecord.setAdvanceAmount(null); } } else { // addToList = false;
	 * rejReason.append("Tag <Adv_Paid> is missing.");
	 * enrollXmlRecord.setAdvanceAmount(null); } tagValue = hm.get("Class"); String
	 * gsoCode = ""; if (tagValue != null && ((String) tagValue).trim().length() >
	 * 0) { if(((String)tagValue).contains("/")) { int pos =
	 * ((String)tagValue).lastIndexOf('/'); String religion = "";
	 * 
	 * if (((String)tagValue).substring(pos+1) == null ||
	 * ((String)tagValue).substring(pos+1).isEmpty()){ religion="OTHERS"; } else {
	 * religion=((String)tagValue).substring(pos+1); } gsoCode =
	 * religion.substring(0, 1); } else { gsoCode =
	 * ((String)tagValue).toUpperCase().substring(0, 1); } if(gsoCode.equals("G") ||
	 * gsoCode.equals("S")) { enrollXmlRecord.setGsoCode(gsoCode); } else {
	 * enrollXmlRecord.setGsoCode("N"); } } else { // addToList = false;
	 * rejReason.append("Tag <Class> is missing.");
	 * enrollXmlRecord.setGsoCode(null); } tagValue = hm.get("DA_Days_Hours"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) {
	 * if(((String)tagValue).trim().contains("/")) { String[] daysHours =
	 * ((String)tagValue).trim().split("/", 2);
	 * if(Utils.isValidNumber(daysHours[0])) {
	 * enrollXmlRecord.setDaDays(Integer.parseInt(daysHours[0])); } else { //
	 * addToList = false;
	 * rejReason.append("Non-Numeric Value for DA_Days in Tag <DA_Days_Hours>.");
	 * enrollXmlRecord.setDaDays(null); } if(Utils.isValidNumber(daysHours[1])) {
	 * enrollXmlRecord.setDaHours(Integer.parseInt(daysHours[1])); } else { //
	 * addToList = false;
	 * rejReason.append("Non-Numeric Value for DA_Hours in Tag <DA_Days_Hours>.");
	 * enrollXmlRecord.setDaHours(null); } } else {
	 * if(Utils.isValidNumber((String)tagValue)) {
	 * enrollXmlRecord.setDaDays(Integer.parseInt((String)tagValue));
	 * enrollXmlRecord.setDaHours(0); } else { // addToList = false;
	 * rejReason.append("Non-Numeric Value in Tag <DA_Days_Hours>..");
	 * enrollXmlRecord.setDaDays(null); enrollXmlRecord.setDaHours(null); } }
	 * 
	 * } else { // addToList = false;
	 * rejReason.append("Tag <DA_Days_Hours> is missing.");
	 * enrollXmlRecord.setDaDays(null); } tagValue = hm.get("Remarks1"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) {
	 * enrollXmlRecord.setColumnA((String)tagValue); } else {
	 * enrollXmlRecord.setColumnA(null); } tagValue = hm.get("Remarks2"); if
	 * (tagValue != null && ((String) tagValue).trim().length() > 0) {
	 * enrollXmlRecord.setRefDo2No((String)tagValue); } else {
	 * enrollXmlRecord.setRefDo2No(null); } if (rejReason.toString().trim().length()
	 * <= 0) { enrollXmlRecord.setXmlFileName(new File(fileName).getName());
	 * enrollXmlDataList.add(enrollXmlRecord); } else {
	 * erroneousXmlRecord.setCreatedAt(new Date());
	 * erroneousXmlRecord.setDo2Description((String) description);
	 * System.out.println("=========== erroneousXmlRecord.getDescription : " +
	 * erroneousXmlRecord.getDo2Description().trim().length());
	 * erroneousXmlRecord.setArmyNo((String) ano);
	 * System.out.println("=========== erroneousXmlRecord.getArmyNo : " +
	 * erroneousXmlRecord.getArmyNo().trim().length()); if(((String) cd)!= null) {
	 * erroneousXmlRecord.setCheckDigit(((String) cd).charAt(0)); } else {
	 * erroneousXmlRecord.setCheckDigit(null); }
	 * erroneousXmlRecord.setDo2ItemNo((String) do2ItemNo); /*
	 * System.out.println("=========== erroneousXmlRecord.getdo2itemno : " +
	 * erroneousXmlRecord.getDo2ItemNo().trim().length());
	 * 
	 * 
	 * erroneousXmlRecord.setDo2ItemNo(((String)
	 * enrollXmlRecord.getDo2ItemNo()).substring( ((String)
	 * enrollXmlRecord.getDo2ItemNo()).length() - 3));
	 * erroneousXmlRecord.setPao(Utils.getPao()); erroneousXmlRecord.setUsr(usr);
	 * erroneousXmlRecord.setDo2Control(dc);
	 * erroneousXmlRecord.setRejectionReason(rejReason.toString());
	 * System.out.println("\t^^^^^^^^^^ do2ItemNo : " + do2ItemNo);
	 * System.out.println("\t\t\t~~~~~~~~~~~~~ rejReason : " +
	 * rejReason.toString()); if (dxirDataList == null || dxirDataList.size() <= 0
	 * || !dxirDataList.contains(erroneousXmlRecord)) {
	 * dxirDataList.add(erroneousXmlRecord); } } } catch (Exception e) {
	 * Utils.handleException(e); // Throwable cause = new Throwable(); //
	 * cause.getStackTrace(); } } if (validDo2Items) { return enrollXmlDataList; }
	 * else { //
	 * System.out.println("\t\t************* returning dxirDataList size : " // +
	 * dxirDataList.size()); return dxirDataList; } }
	 * 
	 * public static void insertDataIntoEnrollDo2(List<EnrollDo2XmlDataObject>
	 * enrollXmlDataList, String batchNo) { String url; String driver; String user;
	 * String password; Do2ControlHome dch = (Do2ControlHome)
	 * Component.getInstance("do2ControlHome"); List<Do2Control> dcList = null;
	 * List<Do2XmlRejectedItems> dxriList = null; String[] month = { "Jan", "Feb",
	 * "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" }; final
	 * int[] mth = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }; Connection con = null;
	 * SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	 * java.util.Date d = null; java.sql.Date date = null; JdbcInf jdbcinf = new
	 * JdbcInf(); jdbcinf.JdbcDetails(); url = jdbcinf.getUrl(); password =
	 * jdbcinf.getPassword(); driver = jdbcinf.getDriver(); user =
	 * jdbcinf.getUser(); Do2Control dc = null; int count = 0; if
	 * (enrollXmlDataList.size() > 0) { try { Class.forName(driver).newInstance();
	 * con = DriverManager.getConnection(url, user, password); PreparedStatement
	 * stmt = con
	 * .prepareStatement(" INSERT INTO enroll_do2(batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, "
	 * +
	 * " do2_date, employee_name, enrollment_date, terms_colour_service, terms_max_age_retention, date_of_birth, "
	 * +
	 * " date_of_arrival, advance_amount, entry_category, gso_code, da_days, da_hours, status, reason, "
	 * +
	 * " transcription_type, qe, approved, entry_trade, commutation_willingness, gratuity_willingness,fk_unit,fk_entry_category) "
	 * +
	 * " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
	 * ); PreparedStatement stmt1 = con
	 * .prepareStatement("INSERT INTO do2(batch_no, unit_code, army_no, check_digit, do2_group, "
	 * +
	 * "do2_type, do2_no, do2_item_no, do2_date, punishment, description, from_date, to_date, rate, tjra, lra, column_a, "
	 * +
	 * "column_b, column_c, ref_do2_no, status, reason, transcription_type, qe, fk_employee, input_qe, fk_unit)"
	 * + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
	 * 
	 * for (int s = 0; s < enrollXmlDataList.size(); s++) { EnrollDo2XmlDataObject
	 * xmlRecord = enrollXmlDataList.get(s);
	 * 
	 * System.out.println("================== xmlRecord : " +
	 * xmlRecord.getDescription() + "\t ; " + xmlRecord.getArmyNo() + "\t : " +
	 * xmlRecord.getEmployeeName());
	 * 
	 * if (xmlRecord.getDescription() != null &&
	 * xmlRecord.getDescription().equals("ENROL")) { stmt.setInt(1,
	 * (Integer)xmlRecord.getBatchNo()); stmt.setString(2,
	 * (String)xmlRecord.getUnitCode()); stmt.setString(3,
	 * (String)xmlRecord.getArmyNo()); stmt.setString(4,
	 * (String)xmlRecord.getCheckDigit()); stmt.setString(5,
	 * (String)xmlRecord.getDo2Group()); stmt.setString(6,
	 * (String)xmlRecord.getDo2Type()); stmt.setString(7,
	 * (String)xmlRecord.getDo2No()); if
	 * (((String)xmlRecord.getDo2ItemNo()).length() <= 4) { stmt.setString(8,
	 * padNumData(((String)xmlRecord.getDo2ItemNo()), 4)); } else {
	 * stmt.setString(8, ((String)xmlRecord.getDo2ItemNo()).substring(0, 4)); }
	 * 
	 * if (((String) xmlRecord.getDo2ItemNo()).length() <= 3) { stmt.setString(8,
	 * padNumData((String) xmlRecord.getDo2ItemNo(), 3)); } else { stmt.setString(
	 * 8, ((String) xmlRecord.getDo2ItemNo()).substring(((String) xmlRecord
	 * .getDo2ItemNo()).length() - 3)); } if (xmlRecord.getDo2Date() == null) {
	 * stmt.setNull(9, java.sql.Types.NULL); } else { d =
	 * formatter.parse((String)xmlRecord.getDo2Date()); date = new
	 * java.sql.Date(d.getTime()); date.setTime(d.getTime()); stmt.setDate(9, date);
	 * } int len = ((String)xmlRecord.getEmployeeName()).length(); if (len <= 25) {
	 * stmt.setString(10, (String)xmlRecord.getEmployeeName()); } else {
	 * stmt.setString(10, ((String)xmlRecord.getEmployeeName()).substring(len -
	 * 25)); } if (xmlRecord.getEnrollmentDate() == null) { stmt.setNull(11,
	 * java.sql.Types.NULL); } else { d = (Date)xmlRecord.getEnrollmentDate(); date
	 * = new java.sql.Date(d.getTime()); date.setTime(d.getTime()); stmt.setDate(11,
	 * date); } stmt.setString(12, (String)xmlRecord.getColorService());
	 * stmt.setString(13, (String)xmlRecord.getMaxAge()); if
	 * (xmlRecord.getDateOfBirth() == null) { stmt.setNull(14, java.sql.Types.NULL);
	 * } else { d = (Date)xmlRecord.getDateOfBirth(); date = new
	 * java.sql.Date(d.getTime()); date.setTime(d.getTime()); stmt.setDate(14,
	 * date); } if (xmlRecord.getDateOfArrival() == null) { stmt.setNull(15,
	 * java.sql.Types.NULL); } else { d = (Date)xmlRecord.getDateOfArrival(); date =
	 * new java.sql.Date(d.getTime()); date.setTime(d.getTime()); stmt.setDate(15,
	 * date); } stmt.setInt(16, (Integer)xmlRecord.getAdvanceAmount()); System.out.
	 * println("******************** (String)xmlRecord.getEntryCategory() : " +
	 * ((String)xmlRecord.getEntryCategory()).trim().length());
	 * 
	 * stmt.setString(17, (String)xmlRecord.getEntryCategory()); stmt.setString(18,
	 * (String)xmlRecord.getGsoCode()); stmt.setInt(19,
	 * (Integer)xmlRecord.getDaDays()); stmt.setInt(20,
	 * (Integer)xmlRecord.getDaHours()); stmt.setString(21,
	 * (String)xmlRecord.getStatus()); stmt.setString(22,
	 * (String)xmlRecord.getReason()); stmt.setString(23,
	 * (String)xmlRecord.getTranscriptionType()); stmt.setString(24,
	 * (String)xmlRecord.getQe()); stmt.setBoolean(25, false); stmt.setString(26,
	 * (String)xmlRecord.getEntryTrade()); stmt.setBoolean(27, false);
	 * stmt.setBoolean(28, false); if (xmlRecord.getUnitCode() == null) {
	 * stmt.setNull(29, java.sql.Types.NULL); } else { Unit unit = (Unit)
	 * Utils.getForeignKey(new Unit(), "unitCode", (String)xmlRecord.getUnitCode());
	 * if (unit != null) { stmt.setInt(29, unit.getId()); } else { stmt.setNull(29,
	 * java.sql.Types.NULL); } } // System.out.println("entry category " + //
	 * xmlRecord.getEntryCategory()); if (xmlRecord.getEntryCategory() == null) {
	 * stmt.setNull(30, java.sql.Types.NULL); } else { EntryCategory ecat =
	 * (EntryCategory) Utils.getForeignKey(new EntryCategory(), "entryCategory",
	 * (String)xmlRecord.getEntryCategory()); if (ecat != null) { stmt.setInt(30,
	 * ecat.getId()); } else { stmt.setNull(30, java.sql.Types.NULL); } }
	 * stmt.executeUpdate(); ++count; } else if (xmlRecord.getDescription() != null
	 * && xmlRecord.getDescription().equals("CANCEL")) { stmt1.setInt(1,
	 * Integer.parseInt((String)xmlRecord.getBatchNo())); stmt1.setString(2,
	 * (String)xmlRecord.getUnitCode()); stmt1.setString(3,
	 * (String)xmlRecord.getArmyNo()); stmt1.setString(4,
	 * (String)xmlRecord.getCheckDigit()); stmt1.setString(5,
	 * (String)xmlRecord.getDo2Group()); stmt1.setString(6,
	 * (String)xmlRecord.getDo2Type()); stmt1.setString(7,
	 * (String)xmlRecord.getDo2No()); if
	 * (((String)xmlRecord.getDo2ItemNo()).length() <= 3) {
	 * stmt1.setString(8,padNumData((String)xmlRecord.getDo2ItemNo(), 3)); } else {
	 * stmt1.setString(8, ((String)xmlRecord.getDo2ItemNo()).substring(1)); } if
	 * (xmlRecord.getDo2Date() == null) { stmt1.setNull(9, java.sql.Types.NULL); }
	 * else { d = (Date)xmlRecord.getDo2Date(); date = new
	 * java.sql.Date(d.getTime()); date.setTime(d.getTime()); stmt1.setDate(9,
	 * date); } stmt1.setNull(10, java.sql.Types.NULL); stmt1.setString(11,
	 * (String)xmlRecord.getDescription()); if (xmlRecord.getFromDate() == null) {
	 * stmt1.setNull(12, java.sql.Types.NULL); } else { d =
	 * (Date)xmlRecord.getFromDate(); date = new java.sql.Date(d.getTime());
	 * date.setTime(d.getTime()); stmt1.setDate(12, date); } stmt1.setNull(13,
	 * java.sql.Types.NULL); stmt1.setDouble(14, 0); stmt1.setInt(15, 0);
	 * stmt1.setInt(16, 0); stmt1.setString(17, (String)xmlRecord.getColumnA());
	 * stmt1.setNull(18, java.sql.Types.NULL); stmt1.setNull(19,
	 * java.sql.Types.NULL); stmt1.setString(20, (String)xmlRecord.getRefDo2No());
	 * stmt1.setString(21, (String)xmlRecord.getStatus()); stmt1.setString(22,
	 * (String)xmlRecord.getReason()); stmt1.setString(23,
	 * (String)xmlRecord.getTranscriptionType()); stmt1.setString(24,
	 * (String)xmlRecord.getQe()); if (xmlRecord.getArmyNo() == null) {
	 * stmt1.setNull(25, java.sql.Types.NULL); } else { Employee employee =
	 * Utils.getCurrentEmployee((String)xmlRecord.getArmyNo()); if (employee !=
	 * null) { stmt1.setLong(25, employee.getId()); } else { stmt1.setNull(25,
	 * java.sql.Types.NULL); } } stmt1.setString(26, (String)xmlRecord.getQe()); if
	 * (xmlRecord.getUnitCode() == null) { stmt1.setNull(27, java.sql.Types.NULL); }
	 * else { Unit unit = (Unit) Utils.getForeignKey(new Unit(), "unitCode",
	 * (String)xmlRecord.getUnitCode()); if (unit != null) { stmt1.setInt(27,
	 * unit.getId()); } else { stmt1.setNull(27, java.sql.Types.NULL); } }
	 * stmt1.executeUpdate(); ++count; } } } catch (Exception e) {
	 * e.printStackTrace(); } } }
	 */

