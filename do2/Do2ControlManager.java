package itsdc.dolphin.do2;

 

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

 
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.crypto.dsig.XMLSignatureFactory;

import org.apache.commons.io.filefilter.WildcardFileFilter;
 
import org.dom4j.Attribute;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.RestController;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import itsdc.dolphin.common.NamePublicKey;
import itsdc.dolphin.common.VerifyDigitalSignature;
import itsdc.dolphin.models.Do2Control;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Section;
import itsdc.dolphin.models.SpecimenSignature;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.reports.ReportUtils;
import itsdc.dolphin.repositories.Do2ControlRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.repositories.SectionRepository;
import itsdc.dolphin.repositories.SpecimenSignatureRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Do2ControlManager {

//	@In(required=false)
	Usr usr;
	
 
Environment env;
String dir;

	UnitRepository unitRepo;
	MiscRepository miscRepo;
	Do2Repository do2Repo;
	SpecimenSignatureRepository ssRepo;
	JdbcTemplate jdbcTemplate;
	Do2ControlRepository do2ControlRepo;
	SectionRepository sectionRepo;
	
	String mesg;
	StringBuffer sb=new StringBuffer();

	
	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}

	public UnitRepository getUnitRepo() {
		return unitRepo;
	}

	public void setUnitRepo(UnitRepository unitRepo) {
		this.unitRepo = unitRepo;
	}

	public MiscRepository getMiscRepo() {
		return miscRepo;
	}

	public void setMiscRepo(MiscRepository miscRepo) {
		this.miscRepo = miscRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public SpecimenSignatureRepository getSsRepo() {
		return ssRepo;
	}

	public void setSsRepo(SpecimenSignatureRepository ssRepo) {
		this.ssRepo = ssRepo;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Do2ControlRepository getDo2ControlRepo() {
		return do2ControlRepo;
	}

	public void setDo2ControlRepo(Do2ControlRepository do2ControlRepo) {
		this.do2ControlRepo = do2ControlRepo;
	}

	public SectionRepository getSectionRepo() {
		return sectionRepo;
	}

	public void setSectionRepo(SectionRepository sectionRepo) {
		this.sectionRepo = sectionRepo;
	}

	@Autowired
	public Do2ControlManager(UnitRepository unitRepo1,MiscRepository miscRepo1,Environment env1) {
		 
		 
		 unitRepo=unitRepo1;
		 miscRepo=miscRepo1;
		 env=env1;
		 dir=env.getProperty("spring.application.filedir");
		 
		File f = new File(dir + "/verifiedDos2");
		if (!f.exists())
			f.mkdirs();
		File f1 = new File(dir + "/softCopiesToBeReviewd");
		if (!f1.exists())
			f1.mkdirs();
		File f2 = new File(dir + "/dos2SummaryReports");
		if (!f2.exists())
			f2.mkdirs();
		File f3 = new File(dir + "/dos2Attachments");
		if (!f3.exists())
			f3.mkdirs();
		setSummaryReportDir(f2.getAbsolutePath());
		unitCodeList =unitRepo.findAllUnitCode();
		miscCurrentQuarterList =miscRepo.findByKey1OrderByDate1(Utils.CURRENT_QUARTER);  
		batchCtrlList = getBatchControlList();
	}

	SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_hhmmss");
	String summaryReportDir = null;

	public String getSummaryReportDir() {
		return summaryReportDir;
	}

	public void setSummaryReportDir(String summaryReportDir) {
		this.summaryReportDir = summaryReportDir;
	}

	BufferedReader reader;
	BufferedWriter writer;
	String line, monthEnding, errorMessage, omittedFilesList,   fileType, importSkipReason;
	List<String> unitCodeList;
	List<String> summaryReport = new ArrayList<String>();
	List<BatchControlObject> batchCtrlList = new ArrayList<BatchControlObject>();
	List<String> filesTobeMoved2ReviewFolder = null;
	 
	List<String> pdfFilesTobeMoved2AttachFolder = null;

	public String getMonthEnding() {
		return monthEnding;
	}

	public void setMonthEnding(String me) {
		this.monthEnding = me;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getImportSkipReason() {
		return importSkipReason;
	}

	public void setImportSkipReason(String importSkipReason) {
		this.importSkipReason = importSkipReason;
	}

	public String getErrorMessage() {
		if (this.errorMessage == null)
			this.errorMessage = "";
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getOmittedFilesList() {
		if (this.omittedFilesList == null)
			this.omittedFilesList = "";
		return omittedFilesList;
	}

	public void setOmittedFilesList(String omittedFilesList) {
		this.omittedFilesList = omittedFilesList;
	}

	 
//	javax.transaction.UserTransaction userTransaction = null;

	public List<BatchControlObject> getBatchControlList() {
		if (batchCtrlList != null && batchCtrlList.size() > 0)
			batchCtrlList.clear();
		if (getMonthEnding() == null) {
			for (Misc m : miscCurrentQuarterList) {
				BatchControlObject bcdo = new BatchControlObject();
				bcdo.setId((long) m.getId());
				bcdo.setToDate(m.getDate2());
				bcdo.setMe(bc.getMonthEndingFromDate(m.getDate1()));
				batchCtrlList.add(bcdo);
			}
		} else {
			for (Misc m : miscCurrentQuarterList) {
				if (bc.getMonthEndingFromDate(m.getDate1()).equals(getMonthEnding())) {
					BatchControlObject bcdo = new BatchControlObject();
					bcdo.setId((long) m.getId());
					bcdo.setToDate(m.getDate2());
					bcdo.setMe(bc.getMonthEndingFromDate(m.getDate1()));
					batchCtrlList.add(bcdo);
				}
			}
		}

		if (batchCtrlList != null && batchCtrlList.size() > 0) {
			for (int i = 0; i < batchCtrlList.size(); i++) {
				if (i > 0) {
					if (batchCtrlList.get(i - 1).getToDate() != null) {
						batchCtrlList.get(i).setFromDate(DateManager.getNextDate(batchCtrlList.get(i - 1).getToDate()));
					}
				}
			}
		}
		/*
		 * if(batchCtrlList != null && batchCtrlList.size() > 0) { for(int i = 0 ; i <
		 * batchCtrlList.size() ; i++) { if((i+1) < batchCtrlList.size())
		 * if(batchCtrlList.get(i+1).getToDate() != null) {
		 * batchCtrlList.get(i).setFromDate(DateManager.getNextDate(batchCtrlList.get(i+
		 * 1).getToDate())); } } }
		 */
		return batchCtrlList;
	}

	private Integer taskValue = 0;
	boolean recordsInserted = false;
	int noOfFiles = 0;
	int noOfFilesProcessed = 0;
	int omittedFileKount = 0;
	int missingDo2UpdatedFileKount = 0;
	int xmlFiles = 0;
	int pdfFiles = 0;
	String dtString = "";
	boolean validRecordLength = true;
	boolean validUnitCode = true;
	List<String> unitCodesInSingleFileList = new ArrayList<String>();
	List<String> inputFilesNamesList = new ArrayList<String>();
	List<String> duplicateFilesNamesList = new ArrayList<String>();
	List<String> do2DateList = new ArrayList<String>();
	List<Object[]> objList = null;
	BigInteger b = BigInteger.ZERO;
	BigInteger totalItems = BigInteger.ZERO;
	int itemKount = 0;
	HashMap<String, Integer> xmlFileItemsMap = new HashMap<String, Integer>();
	List<Do2Control> checkList = null;

	public String verifyImportDo2() {
		String summaryReportFile="NA";
		SimpleDateFormat sdf1 = new SimpleDateFormat("dd-MM-yyyy");
		filesTobeMoved2ReviewFolder = new ArrayList<String>();
		pdfFilesTobeMoved2AttachFolder = new ArrayList<String>();
		List<Unit> unitLst = null;
		if (isValidMonthEnding()) {
			long startTime = new Date().getTime();
			long endTime = 0l;
			File ackFile = null;
			/*
			 * userTransaction = com.arjuna.ats.jta.UserTransaction.userTransaction(); try {
			 * userTransaction.setTransactionTimeout(1200000); } catch (Exception e1) {
			 * Utils.handleException(e1); }
			 */ File dir1 = new File(dir + "/checkDuplicateDos2");
//			FileFilter fileFilter = new WildcardFileFilter("*.txt");
//			File[] files = dir.listFiles(fileFilter);
			File[] files = dir1.listFiles();
			noOfFiles = files.length;
			System.out.println("--------------------- files : " + files.length);
			List<String> fileNameList = new ArrayList<String>();	 
			List<Object[]> itemKountList = null;
			Boolean coreValidity = null, isDcUnqKeySatisfied = null;
			String qryStr = null, pksXml = null, pksXmlRefined = null, pksTbl = null, pksTblRefined = null, errMsgStr = "";
			Integer ssid = null;
			if (files != null && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					fileNameList.add(files[i].getAbsolutePath());
					/*
					 * if(files[i].getName().length() > 50) {
					 * longFileNameList.add(files[i].getName()); omittedFileKount++; } else {
					 * fileNameList.add(files[i].getAbsolutePath()); }
					 */
				}
				jdbcTemplate.execute("drop table if exists doiitmp");
				jdbcTemplate.execute("CREATE TEMP TABLE doiitmp (unit_code character varying(6),do2_no character varying(4), total_items integer,"
						+ " imported_items integer,month_ending character varying(7), dt_of_receipt date, batch_no integer, batch_dt date, "
						+ " do2_year integer, imported boolean, created_at timestamp without time zone DEFAULT now(),task integer, input_file_name "
						+ " character varying(50), do2_date date, missing_do2 boolean, do2_type character(1), fk_usr integer, core_validity boolean, "
						+ " fk_specimen_signature integer, fund_batch boolean, input_file_type character(1)) WITH ( OIDS=FALSE) ");
				 
				if (doiiControlList != null && doiiControlList.size() > 0)
					doiiControlList.clear();
				if (inputFilesNamesList != null && inputFilesNamesList.size() > 0)
					inputFilesNamesList.clear();
				List<String> errorMsg = new ArrayList<String>();
				List<Unit> unitList = null;
				int uc_start = 0;
				int uc_end = 0;
				int grpIndex = 0;
				boolean nameContainsSpaces = false;
				boolean validFile = true, pkMatchFound = false;
				String susno = "";
				Boolean isXmlDs = null;
				Do2Control dcFromXml = null;
				List<NamePublicKey> do2DsVerificationDataList = null;
				Object verifyDigitalSignaturesOfXmlObj = null;
				List<SpecimenSignature> ssList = null;
				try {
//					System.out.println("$$$$$$$$$$$$$ files length : " + files.length);
					for (File f : files) {
						pkMatchFound = false;
//						System.out.println("************* f (286) : " + f.getName());
						if (f.getName().endsWith(".txt")) {
							if (f.getName().toLowerCase().contains("enroll")) {
								setFileType("E");
							} else {
								setFileType("N");
							}
						} else {
							setFileType(null);
						}
						if (f.length() > 0) {
							if (unitCodesInSingleFileList != null)
								unitCodesInSingleFileList.clear();
							if (f.getName().length() <= 50) {
								for (int i = 0; i < f.getName().length(); i++) {
									if (String.valueOf(f.getName().trim().charAt(i)).equals(" ")) {
										nameContainsSpaces = true;
										continue;
									}
								}
								if (!nameContainsSpaces) {
									if (f.getName().endsWith(".txt")) {
//									System.out.println("-------------------------- f : " + f.getName());
										if (!fileContainsBlankLines(f)) {
											if (singleDo2Date(f)) {
												try {
													reader = new BufferedReader(new FileReader(f));
													LineNumberReader lreader = new LineNumberReader(new FileReader(f));
													if (getFileType().equals("N")) {
														resetRecordLength(f, lreader);
													}
													if (getFileType().equals("N")) {
														uc_start = 22;
														uc_end = 28;
														grpIndex = 21;
													} else if (getFileType().equals("E")) {
														uc_start = 21;
														uc_end = 27;
														grpIndex = 30;
													}
													String errMsgString = null;
													int lineKount = 0;
													reader = new BufferedReader(new FileReader(f));
//										        	System.out.println("--------------- errorMsg : " + errorMsg.size());
													while ((line = reader.readLine()) != null) {
														if (line.trim().length() > 0) {
															if (unitCodesInSingleFileList == null
																	|| !unitCodesInSingleFileList.contains(
																			line.substring(uc_start, uc_end))) {
																unitCodesInSingleFileList
																		.add(line.substring(uc_start, uc_end));
															}
														}
													}
													if (unitCodesInSingleFileList != null
															&& unitCodesInSingleFileList.size() == 1) {
														validFile = validateSoftCopy(f, grpIndex, getFileType(), null);
														if (validFile) {
															while ((line = reader.readLine()) != null) {
																if (getFileType().equals("N")) {
																	if (line.trim().length() > 0) {
																		errMsgString = validateLine(++lineKount, line);
																	}
																} else {

																}
																if (errMsgString != null
																		&& errMsgString.trim().length() > 0)
																	errorMsg.add(errMsgString);
															}
 											        	System.out.println("=============== errorMsg : " + errorMsg.size());
															if (errorMsg == null || errorMsg.size() <= 0) {
																updateDoiiTmpTableWithFile(f);
																if (inputFilesNamesList == null
																		|| inputFilesNamesList.size() <= 0) {
																	inputFilesNamesList.add(f.getName());
																} else if (!inputFilesNamesList.contains(f.getName())) {
																	inputFilesNamesList.add(f.getName());
																}
 												         		System.out.println("+++++++++++++++ f : " + f.getName() + " is OK +++++++++++++++++++");
															} else {
																errMsgString = f.getName() + " omitted due to ";
																for (String err : errorMsg) {
																	errMsgString += err;
																}
																summaryReport.add(errMsgString);
//												  				System.out.println("~~~~~~~~~~~~~~~~ errMsgString : " + errMsgString);
																errorMsg.clear();
																errMsgString = null;
																omittedFileKount++;
																if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
																		|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
																	filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
																}
															}
//															userTransaction.commit();
														} else {
															omittedFileKount++;
															if (getFileType().equals("N")) {
																summaryReport.add(f.getName()
																		+ " omitted due to DO-II Group 'IC' "
																		+ "mixed with other groups in a single File.");
															} else {
																summaryReport.add(f.getName()
																		+ " omitted due to other than "
																		+ "DO-II Group 'IC' mixed with 'IC' group in a single File.");
															}
															if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
																	|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
																filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
															}
														}
													} else {
														omittedFileKount++;
														summaryReport.add(f.getName() + " omitted due to Multiple ("
																+ unitCodesInSingleFileList.size()
																+ ") Unit Codes in a Single File.");
														if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
																|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
															filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
														}
													}
												} catch (Exception b) {
													Utils.handleException(b);
												}
											} else {
												omittedFileKount++;
												summaryReport.add(f.getName()
														+ " omitted due to multiple DO-II Publication Dates for a single DO-II");
												if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
														|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
													filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
												}
											}
										} else {
											omittedFileKount++;
											summaryReport.add(f.getName() + " omitted due to blank lines in file.");
											if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
													|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
												filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
											}
										}
									} else if (Utils.fileTypeBasedOnContentType(f.getAbsolutePath()).equals("XML")) {
//											f.getName().toLowerCase().endsWith(".xml")) {
//------------------------ xmlfile
										xmlFiles++;
//										List<XmlDataObject> xmlDataList = Do2ControlImportDo2Xml.
//																importXmlData(f.getAbsolutePath(), getMonthEnding());
//						            	validFile = validateSoftCopy(f, grpIndex, getFileType(), xmlDataList.get(0));
										dcFromXml = Do2ControlImportDo2Xml.ascertainData(f, getMonthEnding());
										isXmlDs = VerifyDigitalSignature.isXmlDigitallySigned(f.getAbsolutePath());
//										System.out.print(">>>>>>>> dcFromXml.getUnitCode() : " + dcFromXml.getUnitCode());
										Unit u =unitRepo.findByUnitCode(dcFromXml.getUnitCode()) ;
												 
										if(u!=null) {
											susno = u.getSusNo();
										}
									
										if (isXmlDs != null && isXmlDs == true && (susno == null || (susno != null && susno.trim().length() > 0))) {		
											
		//									System.out.println("++++++++++ " + f.getAbsolutePath() + " is digitally signed +++++");
											// coreValidity =
											// VerifyDigitalSignature.validateDsXmlFile(f.getAbsolutePath());
											coreValidity = VerifyDigitalSignature.validateDsXmlFile(f.getAbsolutePath(),
													Constants.VALIDATE_DS_CORE_VALUE);
											dcFromXml.setCoreValidity(coreValidity);
											dcFromXml.setInputFileType("D");
			//								System.out.println("\t??????????? coreValidity : " + coreValidity);
											if (coreValidity != null && coreValidity == true) {
												errMsgStr = "";
								//				if(f.getName().equals("IIB_24_3928064_004542021_16122021_0.xml")) {
								//					System.out.println(">>>>> " + f.getName() + " is valid digitally signed xml File <<<<<");
								//				}
												// do2DsVerificationDataList = VerifyDigitalSignature.verifyDigitalSignaturesOfXml(f.getAbsolutePath());
												verifyDigitalSignaturesOfXmlObj = VerifyDigitalSignature.verifyDigitalSignaturesOfXml(f.getAbsolutePath(),
																dcFromXml.getDo2Date());
			//									System.out.println ( " Verify DigitalSignatureOfXmlObj " +verifyDigitalSignaturesOfXmlObj.toString());
												if (verifyDigitalSignaturesOfXmlObj instanceof String) {
													dcFromXml.setSsVerified(false);
											 
													
													// modified as sus no checking newly introduced 13-01-2022
												 
													
		//										 
													ssList =ssRepo.findBySusNoApprovedOrderById(susno); 
													System.out.println ( " ss list size " +ssList.size()    +" sus no " +susno);
													if (ssList != null && ssList.size() > 0) {
														for (SpecimenSignature ss : ssList) {
															pksTbl = Utils.removeCrNlFromStr(ss.getPublicKey());
															pksTblRefined = Utils.removeCrNlFromStr(pksTbl.replaceAll("[^a-zA-Z0-9]", "").replaceAll("/", ""));
															pksXml = Utils.removeCrNlFromStr((String) verifyDigitalSignaturesOfXmlObj);
															pksXmlRefined = Utils.removeCrNlFromStr(pksXml.replaceAll("[^a-zA-Z0-9]", "").replaceAll("/", ""));
															if(f.getName().equals("IIB_24_3928064_004542021_16122021_0.xml")) {
																System.out.println("\t\t||||||||||| ss.id : " + ss.getId()
																+ "\t%%%%%%%%% pksXml.equals(pksTbl) : " + pksXml.equals(pksTbl)
																+ "\t%%%%%%%%% pksXmlRefined.equals(pksTblRefined) : " 
																+ pksXmlRefined.equals(pksTblRefined)); 
															}
															 
															
				//											System.out.println ( " pksTbl "  +pksTbl );
				//											System.out.println ( " pksxml "  +pksXml);
				//											System.out.println ( " pksTblRefined  " +pksTblRefined);
				//											System.out.println ( " pksXmlRefined " +pksXmlRefined);
				//											System.out.println ( " ssid "+ss.getId());
															
															
															if (Utils.removeCrNlFromStr(ss.getPublicKey()).equals(Utils.removeCrNlFromStr((String) verifyDigitalSignaturesOfXmlObj)) || 
																	pksTblRefined.equals(pksXmlRefined)) {
																pkMatchFound = true;
																ssid = ss.getId();
																break;
															}
														}
														
													
													//	if(f.getName().equals("IIB_24_3928064_004542021_16122021_0.xml")) {
			//												System.out.println("\t\tIIIII pkMatchFound : " + pkMatchFound + "\tssid : " + ssid);
														//}

														if(pkMatchFound == false) {
															errMsgStr += ((errMsgStr.trim().length() > 0) ? " "
																	: "");
															errMsgStr += "Either (1) No matching Public Key in the database or "
																			+ "	(2) DOs-II is signed with different Digital Signature "
																			+ "sample of which is not provided to the PAO for registration or "
																			+ "(3) Please verify SUS_NO as Specimen_Signauture SUS_NO might have"
																			+ " been recorded with check digit or (4) Sample of Specimen Signature "
																			+ "found in the xml might not have been received by the PAO";
														} else {
															if(ssid != null) {
																//qryStr = "SELECT ss FROM SpecimenSignature ss WHERE ss.id = " + ssid + " ORDER BY ss.id";
																SpecimenSignature ss =ssRepo.findById(ssid).get(); 
																 
																	if (ss!=null) {
																		if (ss.getApproved() == null || (ss.getApproved() != null
																				&& ss.getApproved() == false)) {
																			errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
																			errMsgStr += "Specimen Signature found in the database but not approved.";
																		} else {
																			if (ss.getToDate() != null) {
																				if(!DateManager.checkBetween(dcFromXml.getDo2Date(), ss.getFromDate(), ss.getToDate())) {
																						errMsgStr += ((errMsgStr.trim()
																								.length() > 0) ? " " : "");
																						errMsgStr += "Specimen Signature found in database with signatory name : " + 
																								 ss.getSignatoryName() + " but DO-II Date : "
																										+ sdf1.format(dcFromXml.getDo2Date())
																										+ " is not being covered by Specimen Signature' from and to dates. ";
																				}
																			} else {
																				if(dcFromXml.getDo2Date().before(ss.getFromDate())) {
																					if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) 
																							&& DateManager.getNoOfDays(dcFromXml.getDo2Date(), ss.getFromDate())<=90) {
																						;																						
																					}  																				
																				}

																			}
																		}
																	}
																 														
															} else {
																errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
																errMsgStr += "Sample of Specimen Signature found in the xml not yet received as per records.";
															}
														}
	//													System.out.println("pkmatchfound   " +pkMatchFound +" ssid "+ssid + "  errmsgstr "+errMsgStr);
													} else {
													
														/*qryStr = "SELECT ss FROM SpecimenSignature ss WHERE (ss.approved is null or "
																+ "(ss.approved is not null and ss.approved = false)) and ss.susNo = '" 
																+ susno + "' ORDER BY ss.id";
														
														*/
														
													 
														ssList = ssRepo.findBySusNotApproved(susno);
														if (ssList != null && ssList.size() > 0) {
															if(ssList.size() > 1) {
																errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
																errMsgStr += ssList.size() + " Specimen Signature Records found in database.";
															}
															for (SpecimenSignature ss : ssList) {
																pksTbl = ss.getPublicKey();
																pksTblRefined = pksTbl.replaceAll("[^a-zA-Z0-9]", "").replaceAll("/", "");
																pksXml = ((String) verifyDigitalSignaturesOfXmlObj);
																pksXmlRefined = pksXml.replaceAll("[^a-zA-Z0-9]", "").replaceAll("/", "");
/*																
																System.out.println("\t\t||||||||||| ss.id : " + ss.getId()
																		+ "\t%%%%%%%%% pks.equals(pkString) : "
																		+ pks.equals((String) verifyDigitalSignaturesOfXmlObj));
*/																
																if (pksTblRefined.equals(pksXmlRefined) 
																		|| ss.getPublicKey().equals((String) verifyDigitalSignaturesOfXmlObj)) {
																	pkMatchFound = true;
																	ssid = ss.getId();
																	errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
																	errMsgStr += "This Specimen Signature record is not yet approved.";
																	if(ss.getToDate() == null) {
																		if(DateManager.checkBetween(dcFromXml.getDo2Date(), ss.getFromDate(), ss.getFromDate()) == false) {
																			errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
																			errMsgStr += "DO-II date is preceeding the date from which Specimen Signature is effective.";
																		}
																	} else {
																		if(DateManager.checkBetween(dcFromXml.getDo2Date(), ss.getFromDate(), ss.getToDate()) == false) {
																			errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
																			errMsgStr += "DO-II date is either preceeding the date from which the Specimen Signature is effective " 
																							+ " or succeding the date upto which the Specimen Signature is effective.";
																		}
																	}
																}
															}
														} else {
															errMsgStr += ((errMsgStr.trim().length() > 0) ? " " : "");
															errMsgStr += "Sample of Specimen Signature found in the xml not yet received as per records.";
														}
													}
//													omittedFileKount++;
//													summaryReport.add("Import of data from " + f.getName()
//															+ " is aborted. Reason(s) : " + errMsgStr);
//													if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
//															|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
//														filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
//													}
                                                    if (errMsgStr != null && errMsgStr.trim().length() > 0) {
                                                        omittedFileKount++;
                                                        summaryReport.add("Import of data from " + f.getName()
                                                                + " is aborted. Reason(s) : " + errMsgStr);
                                                        if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0
                                                                || !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
                                                            filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
                                                        }
                                                    } else {
                                                        dcFromXml.setSsVerified(true);
                                                    }
												} else {
													do2DsVerificationDataList = ((List<NamePublicKey>) verifyDigitalSignaturesOfXmlObj);
													dcFromXml.setSsVerified(true);
													if (do2DsVerificationDataList != null && do2DsVerificationDataList.size() > 0) {
			//											System.out.println("\t>>>>>>>>>>>>>>>>> do2DsVerificationDataList.size : "+ do2DsVerificationDataList.size());
														for (NamePublicKey npkKey : do2DsVerificationDataList) {
															if (npkKey.getVerified() != null
																	&& npkKey.getVerified() == true) {
																dcFromXml.setSpecimenSignature(
																		npkKey.getSpecimenSignature());
																break;
															}
														}

													}
												}
											} else {
												omittedFileKount++;
												summaryReport.add("Import of data from " + f.getName()
														+ " appears to have been tampered.");
												if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
														|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
													filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
												}
											}
										} else {
											dcFromXml.setSpecimenSignature(null);
											dcFromXml.setInputFileType("C");
										}
										xmlFileItemsMap.put(f.getName(), dcFromXml.getTotalItems());
//						            	if(validFile) {
										if (validSusNo(dcFromXml, unitCodeList)) {

											/*
											 * System.out.println("//////////////// dcFromXml getCoreValidity() : " +
											 * dcFromXml.getCoreValidity()); if(dcFromXml.getSpecimenSignature() !=
											 * null) { System.out.println("//////////////// dcFromXml ss : " +
											 * dcFromXml.getSpecimenSignature().getId()); } else {
											 * System.out.println("//////////////// dcFromXml ss : " +
											 * dcFromXml.getSpecimenSignature()); }
											 * System.out.println("//////////////// dcFromXml getInputFileType() : " +
											 * dcFromXml.getInputFileType());
											 */
											if ((dcFromXml.getSsVerified() != null && dcFromXml.getSsVerified() == true && dcFromXml.getInputFileType().equals("D"))
													|| (dcFromXml.getInputFileType().equals("C"))) {
												updateDataFromXmlFile(dcFromXml);
											}
											if (inputFilesNamesList == null || inputFilesNamesList.size() <= 0) {
												inputFilesNamesList.add(f.getName());
											} else if (!inputFilesNamesList.contains(f.getName())) {
												inputFilesNamesList.add(f.getName());
											}
										} else {
											omittedFileKount++;
											summaryReport.add(f.getName() + " omitted due to Invalid SUS No.");
											if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
													|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
												filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
											}
										}

										/*
										 * >>>>>>>>>>>>>>>>> do2DsVerificationDataList.size : } else {
										 * omittedFileKount++; if(getFileType().equals("N")) {
										 * summaryReport.add(f.getName() + " omitted due to DO-II Group 'IC' " +
										 * "mixed with other groups in a single File."); } else {
										 * summaryReport.add(f.getName() + " omitted due to other than " +
										 * "DO-II Group 'IC' mixed with 'IC' group in a single File."); }
										 */
//						            	}
									} else if(Utils.fileTypeBasedOnContentType(f.getAbsolutePath()).equals("PDF")) {
										pdfFiles++;
										if(pdfFilesTobeMoved2AttachFolder == null || pdfFilesTobeMoved2AttachFolder.size() <= 0 
												|| !pdfFilesTobeMoved2AttachFolder.contains(f.getAbsolutePath())) {
											pdfFilesTobeMoved2AttachFolder.add(f.getAbsolutePath());
										}
									}
									coreValidity = null;
								} else {
									omittedFileKount++;
									summaryReport.add(f.getName() + " omitted due to Spaces in File Name.");
									nameContainsSpaces = false;
									if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
											|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
										filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
									}
								}

							} else {
								omittedFileKount++;
								summaryReport.add(f.getName()
										+ " omitted due to length of file name (including EXT) is > 50 char.");
								if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
										|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
									filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
								}
							}
						} else {
							omittedFileKount++;
							summaryReport.add(f.getName() + " omitted due to 0 byte length.");
							if(filesTobeMoved2ReviewFolder == null || filesTobeMoved2ReviewFolder.size() <= 0 
									|| !filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
								filesTobeMoved2ReviewFolder.add(f.getAbsolutePath());
							}
						}
					}
				} catch (Exception e1) {
					Utils.handleException(e1);
				}

				try {
//					if (userTransaction.getStatus() != 0)
//						 userTransaction.begin();
					objList =jdbcTemplate.query("SELECT DISTINCT unit_code, do2_no, do2_year, input_file_name, do2_type  FROM doiitmp "
							+ " ORDER BY unit_code, do2_no, do2_year, do2_type, input_file_name",new RowMapper<Object[]>() {
						 
						@Override
						public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
							// TODO Auto-generated method stub
							Object [] obj=new Object[5];
							obj[0]=rs.getString(1);
							obj[1]=rs.getString(2);
							obj[2]=rs.getInt(3);
							obj[3]=rs.getString(4);
							obj[4]=rs.getString(5);
							return obj;
						}
						 
					 }); 
							
							 
					Boolean localCoreValidity = null, localFundBatch = false;
					Integer localSsId = null;
					Character localIft = null;
//						System.out.println("_______________________ objList size : " + objList.size());
					if (objList != null && objList.size() > 0) {
						for (Object[] obj : objList) {
							itemKount = 0;
							itemKountList = jdbcTemplate.query("SELECT * FROM doiitmp WHERE unit_code = ? AND do2_no = ? AND "
									+ "do2_year = ? AND input_file_name = ? AND do2_type = ?",new PreparedStatementSetter() {
						                @Override
						                public void setValues(PreparedStatement ps) throws SQLException {
						                    ps.setString(1, (String) obj[0]);
						                    ps.setString(2, (String) obj[1]);
						                    ps.setInt(3, (Integer) obj[2]);
						                    ps.setString(4, (String) obj[3]);
						                    ps.setString(5, (String) obj[4]);
						                }
						            },
									new RowMapper<Object[]>() {
										 
										@Override
										public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
											// TODO Auto-generated method stub
											Object [] obj=new Object[5];
											obj[0]=rs.getString(1);
											obj[1]=rs.getString(2);
											obj[2]=rs.getInt(3);
											obj[3]=rs.getString(4);
											obj[4]=rs.getString(5);
											return obj;
										}
										 
									 }); 
									
									
							if (((String) obj[3]).toLowerCase().endsWith(".txt")) {
								itemKount = itemKountList.size();
							} else {
								itemKount = xmlFileItemsMap.get((String) obj[3]);
							}
//			            	System.out.println("list size : " + itemKountList.size() + "\t~~~~~~ " + (String)obj[3] + "\t~~~~~ " + itemKount);
							System.out.println("--"+(String) obj[0]+"--"+(String) obj[1]+"--"+(Integer) obj[2]+"--"+(String) obj[3]+"--"+(String) obj[4]);
							checkList =do2ControlRepo.findByFileNameAndCoreValidityTrue((String) obj[0], (String) obj[1], (Integer) obj[2], (String) obj[4], (String) obj[3]); 
									 
							Date doiiDate = null;
							/*List<Date> do2Date =  (List<Date>) jdbcTemplate.queryForObject("SELECT do2_date FROM doiitmp WHERE unit_code = ? AND do2_no = ? AND do2_year = ?"
									+ " AND input_file_name = ? AND do2_type = ? ORDER BY do2_date, input_file_name", Date.class,
										new PreparedStatementSetter() {
						                @Override
						                public void setValues(PreparedStatement ps) throws SQLException {
						                    ps.setString(1, (String) obj[0]);
						                    ps.setString(2, (String) obj[1]);
						                    ps.setInt(3, (Integer) obj[2]);
						                    ps.setString(4, (String) obj[3]);
						                    ps.setString(5, (String) obj[4]);
						                }
						            });*/
							//System.out.println("do2date "+do2Date);
									 
							doiiDate =  jdbcTemplate.queryForObject("SELECT do2_date FROM doiitmp WHERE unit_code = ? AND do2_no = ? AND do2_year = ?"
									+ " AND input_file_name = ? AND do2_type = ? ORDER BY do2_date, input_file_name",
									new Object[] {(String) obj[0],(String) obj[1],(Integer) obj[2],(String) obj[3],(String) obj[4]}, Date.class);
							
							if (checkList != null && checkList.size() > 0) {
								
								for (Do2Control dc : checkList) {
									DoiiControl duplicateDoii = new DoiiControl();
									duplicateDoii.setDo2No((String) obj[1]);
									duplicateDoii.setUnitCode((String) obj[0]);
									duplicateDoii.setDo2Year((Integer) obj[2]);
									duplicateDoii.setDo2Type(((String)obj[4]).charAt(0));
									taskValue=0;
									taskValue = unitRepo.findTaskForUnitCode(duplicateDoii.getUnitCode());
									if (dcFromXml.getInputFileName()!=null && dcFromXml.getInputFileName().startsWith("ID")
											&& taskValue>0 && taskValue!=991) {
										dcFromXml.setFundBatch(true);
										List<Section> sectionList=null;
										Integer sectionId=0;
										sectionList=sectionRepo.findBySecName("fund cell");
										if (sectionList!=null && sectionList.size()==1) {
											sectionId=sectionList.get(0).getId();
										} else {
											System.out.println(" Alert !!!  Fund Cell - Section Not Found In The PAO. ");
										}
										if (sectionId>0 && sectionId<10) {
											if (taskValue>10 && taskValue<100) {
												taskValue=(sectionId*100)+taskValue;
											} else if (taskValue>99 && taskValue<1000) {
												taskValue=(sectionId*1000)+taskValue;
											} else {
												System.out.println(" Alert !!! Invalid TASK value for Ledger Groups in the PAO.");
											}
										} else {
											System.out.println(" Alert !!! Invalid section id value for Fund Cell Section in the PAO.");
										}
									}
									System.out.println("..........task value duplicate do2 ... " +taskValue);
									duplicateDoii.setTask(taskValue);
									duplicateDoii.setTotalItems(itemKount);
									duplicateDoii.setInputFileName((String) obj[3]);
									duplicateDoii.setDo2Date(doiiDate);
									duplicateDoii.setReceivedInPreviousBatch(dc.getBatchFileName());
									if (!duplicatedoiiList.contains(duplicateDoii)) {
										duplicatedoiiList.add(duplicateDoii);
										duplicateFilesNamesList.add(duplicateDoii.inputFileName);
									}
									if (inputFilesNamesList.contains(duplicateDoii.getInputFileName())) {
										inputFilesNamesList.remove(duplicateDoii.getInputFileName());
									}
								}
							} else {
								taskValue=0;
								taskValue = unitRepo.findTaskForUnitCode((String) obj[0]); 
				//				if (dcFromXml.getInputFileName()!=null && dcFromXml.getInputFileName().startsWith("ID")
				//						&& taskValue>0 && taskValue!=991) {
								if (obj[3]!=null && obj[3].toString().startsWith("ID")
										&& taskValue>0 && taskValue!=991) {
									dcFromXml.setFundBatch(true);
									List<Section> sectionList=null;
									Integer sectionId=0;
									sectionList=sectionRepo.findBySecName("fund cell");
									if (sectionList!=null && sectionList.size()==1) {
										sectionId=sectionList.get(0).getId();
									} else {
										System.out.println(" Alert !!!  Fund Cell - Section Not Found In The PAO. ");
									}
									if (sectionId>0 && sectionId<10) {
										if (taskValue>10 && taskValue<100) {
											taskValue=(sectionId*100)+taskValue;
										} else if (taskValue>99 && taskValue<1000) {
											taskValue=(sectionId*1000)+taskValue;
										} else {
											System.out.println(" Alert !!! Invalid TASK value for Ledger Groups in the PAO.");
										}
									} else {
										System.out.println(" Alert !!! Invalid section id value for Fund Cell Section in the PAO.");
									}
								}
														 
								itemKountList = jdbcTemplate.query("SELECT core_validity,fk_specimen_signature,input_file_type, fund_batch FROM doiitmp WHERE unit_code = ? AND do2_no = ? AND "
										+ "do2_year = ? AND input_file_name = ? AND do2_type = ?",new PreparedStatementSetter() {
							                @Override
							                public void setValues(PreparedStatement ps) throws SQLException {
							                    ps.setString(1, (String) obj[0]);
							                    ps.setString(2, (String) obj[1]);
							                    ps.setInt(3, (Integer) obj[2]);
							                    ps.setString(4, (String) obj[3]);
							                    ps.setString(5, (String) obj[4]);
							                }
							            },
										new RowMapper<Object[]>() {
											 
											@Override
											public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
												// TODO Auto-generated method stub
												Object [] obj=new Object[4];
												obj[0]=rs.getBoolean(1);
												obj[1]=rs.getInt(2);
												obj[2]=rs.getString(3);
												obj[3]=rs.getBoolean(4);
												 
												return obj;
											}
											 
										 }); 
										
										 
			            		//System.out.println("----------itemKountList : " + itemKountList.size());
								for (Object[] obj2 : itemKountList) {
									//System.out.println("----"+obj2)
									localCoreValidity = (Boolean) obj2[0];
									localSsId = (Integer) obj2[1];
									localIft =   ((String) obj2[2]).charAt(0);
									localFundBatch = (Boolean) obj2[3];
								}
//			            		System.out.println(">>>>>>>>>>>>>>> localSsId : " + localSsId + "\tlocalCoreValidity : " + localCoreValidity);
//	            				unit_code, do2_year, do2_type, do2_no, input_file_name, dt_of_receipt
								 

								isDcUnqKeySatisfied =  do2ControlRepo.findWhetherFileExists(((String) obj[0]), ((Integer) obj[2]), ((String) obj[4]), ((String) obj[1]), ((String) obj[3]), DateManager.getToday()); 
								
 	            				System.out.println("<<<<<<<<<<< qryStr : " + qryStr);
 	            				System.out.println("<<<<<<<<<<< isDcUnqKeySatisfied : " + isDcUnqKeySatisfied);
								if (isDcUnqKeySatisfied != true) {
									if (localCoreValidity != null) {
										if (localSsId != null && localSsId>0) {
 				            				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 1 <<<<<<<<<<<<<<<<<<<<<<<");
											do2ControlRepo.insertIntoDo2Control((String)obj[0], (String) obj[1], DateManager.getToday(), (Integer) obj[2], taskValue,
											(Integer) itemKount, (Boolean) false, (String) obj[3], getMonthEnding(), doiiDate,(Boolean) false, (String) obj[4], usr.getId(), (Boolean) false,(Boolean) false,DateManager.getToday(),
											localCoreValidity, localSsId, localIft+"", localFundBatch);
											 
										} else {
 				            				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 2 <<<<<<<<<<<<<<<<<<<<<<<");
										do2ControlRepo.insertIntoDo2ControlSsNull((String)obj[0], (String) obj[1], DateManager.getToday(), (Integer) obj[2], taskValue,
													(Integer) itemKount, (Boolean) false, (String) obj[3], getMonthEnding(), doiiDate,(Boolean) false, (String) obj[4], usr.getId(), (Boolean) false,(Boolean) false,DateManager.getToday(),
													localCoreValidity,localIft+"", localFundBatch);
											 
										}
									} else {
 			            				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 3 <<<<<<<<<<<<<<<<<<<<<<<");
										do2ControlRepo.insertIntoDo2ControlSsNull((String)obj[0], (String) obj[1], DateManager.getToday(), (Integer) obj[2], taskValue,
												(Integer) itemKount, (Boolean) false, (String) obj[3], getMonthEnding(), doiiDate,(Boolean) false, (String) obj[4], usr.getId(), (Boolean) false,(Boolean) false,DateManager.getToday(),
												null, localIft+"", localFundBatch);
										 
									}
								}

//					    		System.out.println("??????????? dcFromXml : " + dcFromXml.getInputFileName());
//					    		System.out.println("??????????? dcFromXml : " + dcFromXml.getCoreValidity() + "\tss id : " 
//					    				+ dcFromXml.getSpecimenSignature().getId() + "\tinput_file_type : " + dcFromXml.getInputFileType());
								File sourceFile = new File(
										dir + "/checkDuplicateDos2/" + ((String) obj[3]));
								if (isXmlDs != null) {
									if (isXmlDs == true) {
										if (localCoreValidity != null) {
											if ((localCoreValidity == true && localSsId == null)
													|| localCoreValidity == false) {
												if(filesTobeMoved2ReviewFolder.contains(sourceFile.getAbsolutePath())) {
													sourceFile.renameTo(new File(dir+ "/softCopiesToBeReviewd/" + ((String) obj[3])));
													omittedFileKount++;
												}
											} else {
												sourceFile.renameTo(new File(
														dir + "/verifiedDos2/" + ((String) obj[3])));
												noOfFilesProcessed++;
											}
										} else {
											if(filesTobeMoved2ReviewFolder.contains(sourceFile.getAbsolutePath())) {
												sourceFile.renameTo(new File(dir
														+ "/softCopiesToBeReviewd/" + ((String) obj[3])));
												omittedFileKount++;
											}
										}
									} else {
										sourceFile.renameTo(
												new File(dir + "/verifiedDos2/" + ((String) obj[3])));
										noOfFilesProcessed++;
									}
								} else {
									sourceFile.renameTo(
											new File(dir + "/verifiedDos2/" + ((String) obj[3])));
									noOfFilesProcessed++;
								}
								if (!recordsInserted)
									recordsInserted = true;
//					    		System.out.println(">>>>>>>>>>> sourceFile : " + sourceFile);
							}
						}
					}
					endTime = new Date().getTime();
//		            System.out.println("-------------- recordsInserted : " + recordsInserted);
					if (recordsInserted) {
						Do2Feedback dfb = new Do2Feedback();
						for (Object[] obj : objList) {
							checkList =do2ControlRepo.findByUnitCodeDo2NoYearDo2Type((String) obj[0], (String) obj[1], (Integer) obj[2], (String) obj[4]);
							 
//				            System.out.println("-------------- checkList size : " + checkList.size());
							for (Do2Control do2c : checkList) {
								if (do2c.getInputFileName().toLowerCase().endsWith("_0.xml")
										|| do2c.getInputFileName().toLowerCase().endsWith("_4.xml")) {
									dfb.setFeedbackType("ACK");
									dfb.setOutFileDir(dir + "/reports/DOs2_Ack_FeedBack/Acknowledgement");
									File f = new File(dfb.getOutFileDir());
									if (!f.exists()) {
										f.mkdirs();
									}
									ackFile = new File(dir + "/verifiedDos2/" + do2c.getInputFileName());
									if (ackFile.exists()) {
										dfb.modifyThisXmlFile(ackFile);
									} else {
//											System.out.println("++++++++++ " + ackFile.getAbsolutePath() +
//													" does not exist +++++++++");
									}
									/*
									 * dcfb.createFile(do2c.getInputFileName().trim(), "A", do2c.getTotalItems(),
									 * do2c.getAckDate(), do2c.getFeedBackDate(), do2c.getUnitCode().trim(),
									 * do2c.getDo2No().trim(), do2c.getDo2Type(),do2c.getDo2Date());
									 */ } else {
									mesg="" + do2c.getInputFileName()
											+ " is not valid (not ending with 0.xml or 4.xml)";
								}
							}
						}
						objList = jdbcTemplate.query("SELECT DISTINCT unit_code, do2_no, do2_year, input_file_name  FROM doiitmp",new RowMapper<Object[]>() {
									 
									@Override
									public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
										// TODO Auto-generated method stub
										Object [] obj=new Object[4];
										obj[0]=rs.getString(1);
										obj[1]=rs.getString(2);
										obj[2]=rs.getInt(3);
										obj[3]=rs.getString(4);
										 
										return obj;
									}
									 
								 }); 
										
							 
						b =  jdbcTemplate.queryForObject("SELECT count(*) from doiitmp", BigInteger.class);
						 
						/*
						 * for(String f : inputFilesNamesList) {
						 * System.out.println("------------ inputFilesNamesList : " + f); }
						 */ for (DoiiControl d2c : duplicatedoiiList) {
							duplicateFilesNamesList.add(d2c.inputFileName);
						}
						totalItems =   do2ControlRepo.findSumOfTotalItemsForFileName(inputFilesNamesList); 
								
						 
						/*
						 * summaryReport.add("Input Files Read : " + noOfFiles + " ----- Valid Files : "
						 * + (noOfFiles - omittedFileKount - duplicatedoiiList.size()) +
						 * " ----- Omitted Files : " + omittedFileKount +
						 * " ----- Missing DOs-II Now received : " + missingDo2UpdatedFileKount +
						 * " ----- Duplicate (Deleted) : " + duplicatedoiiList.size());
						 */ summaryReport.add("DOs-II Control table has been updated with data from "
								+ noOfFilesProcessed + " DOs-II containing " + totalItems.intValue() + " items.");
						sb.append("DOs-II Control table has been updated with data from "
								+ noOfFilesProcessed + " DOs-II containing " + totalItems.intValue() + " items.");
//		                FacesMessages.instance().add((noOfFiles - noOfFilesProcessed) + " Input Files have been omitted.");
						sb.append("Total Time Taken : " + (endTime - startTime) + " milliseconds.");
						if (duplicatedoiiList != null && duplicatedoiiList.size() > 0) {
							sb.append(duplicatedoiiList.size() + " duplicate DOs-II were found.");
							for (DoiiControl dc : duplicatedoiiList) {
								/*
								 * summaryReport.add("Input File : " + dc.getInputFileName() +
								 * " ---------- UnitCode : " + dc.getUnitCode() + " ---------- DO-II # " +
								 * dc.getDo2No() + " ---------- Year : " + dc.getDo2Year() +
								 * " ---------- Task : " + dc.getTask() +
								 * " is DUPLICATE and therefore will be DELETED.");
								 */
								summaryReport.add("Input File : " + dc.getInputFileName()
										+ (dc.getReceivedInPreviousBatch() != null
												? " already processed vide batch with file name "
														+ dc.getReceivedInPreviousBatch()
												: " included in current lot of DOs-II Files."));

								sb.append("Input File : " + dc.getInputFileName() + " ---------- UnitCode : "
												+ dc.getUnitCode() + " ---------- DO-II # " + dc.getDo2No()
												+ " ---------- Year : " + dc.getDo2Year() + " ---------- Task : "
												+ dc.getTask() + " is DUPLICATE and therefore will be DELETED.");

								File removeDuplicateFile = new File(dir + "/checkDuplicateDos2/" + dc.getInputFileName());
								if (removeDuplicateFile.exists()) {
//			            			summaryReport.add(removeDuplicateFile.getAbsolutePath() + " is being deleted.");
//			            			FacesMessages.instance().add(removeDuplicateFile.getAbsolutePath() + " is being deleted.");
									removeDuplicateFile.delete();
								}
							}
						}
					}
					File ddir = new File(dir+ "/checkDuplicateDos2");
//					fileFilter = new WildcardFileFilter("*.txt");
//					files = dir.listFiles(fileFilter);
					files = ddir.listFiles();
					int kounter = 0;
					for (File f : files) {
						if(pdfFilesTobeMoved2AttachFolder.contains(f.getAbsolutePath())) {
							f.renameTo(new File(dir	+ "/dos2Attachments/" + f.getName()));
						}
						if(filesTobeMoved2ReviewFolder.contains(f.getAbsolutePath())) {
							f.renameTo(new File(dir	+ "/softCopiesToBeReviewd/" + f.getName()));
						}
						for (int i=0; i<duplicateFilesNamesList.size(); i++) {
							File removeDuplicateFile = new File(dir + "/checkDuplicateDos2/" + f.getName());
							if (removeDuplicateFile.exists()) {
								removeDuplicateFile.delete();
							}							
						}						
					}
					if (pdfFilesTobeMoved2AttachFolder != null && pdfFilesTobeMoved2AttachFolder.size() > 0) {
						summaryReport.add(pdfFilesTobeMoved2AttachFolder.size() + " files have been moved to the folder " +dir
								+ "/dos2Attachments");
					}
					if (filesTobeMoved2ReviewFolder != null && filesTobeMoved2ReviewFolder.size() > 0) {
						summaryReport.add(filesTobeMoved2ReviewFolder.size() + " files have been moved to the folder " + dir
								+ "/softCopiesToBeReviewd");
					}
					summaryReport.add("Input Files Read : " + noOfFiles + " Out of which PDF ~ " + pdfFiles 
							+ " XML ~ " + (xmlFiles - omittedFileKount - duplicatedoiiList.size()) + " ----- Omitted XML Files : "
							+ omittedFileKount + " ----- Missing DOs-II Now received : " + missingDo2UpdatedFileKount
							+ " ----- Duplicate (Deleted) : " + duplicatedoiiList.size());
					summaryReportFile=  generateSummaryReport();
					//aggregateAcknowledgementsToCsv(); commented on 10102025 rectify this method
//				 	userTransaction.commit();
				} catch (Exception ef) {
					Utils.handleException(ef);
				}
			} else {
				sb.append("No files were found available.");
			}
		} else {
			sb.append(getErrorMessage());
		}
		
		return summaryReportFile;
	}

	public static Boolean validSusNo(Do2Control dcFromXml, List<String> ucList) {
		boolean validUnitCode = true;
		if (!ucList.contains(dcFromXml.getUnitCode())) {
			validUnitCode = false;
		}
		return validUnitCode;
	}

	public Boolean fileContainsBlankLines(File f) {
		boolean containsBlankLines = false;
		try {
			reader = new BufferedReader(new FileReader(f));
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() <= 0) {
					containsBlankLines = true;
					break;
				}
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		return containsBlankLines;
	}

	public Boolean singleDo2Date(File f) {
		List<String> do2DateList = new ArrayList<String>();
		do2DateList.clear();
		Object obj = null;
		String dtString = null;
		try {
			reader = new BufferedReader(new FileReader(f));
			while ((line = reader.readLine()) != null) {
				dtString = line.substring(39, 46);
				if (do2DateList == null || !do2DateList.contains(dtString)) {
					do2DateList.add(dtString);
				}
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (do2DateList != null && do2DateList.size() == 1) {
			return true;
		} else {
			return false;
		}
	}

	private String generateSummaryReport() {
		////FacesContext faces = FacesContext.getCurrentInstance();
		//HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();
		String fName="";
		FileOutputStream fos = null;
		LocalEndPageEvent endPageEvent = new LocalEndPageEvent();
		PdfPTable finalTable = new PdfPTable(new float[] { 100f });
		//ByteArrayOutputStream baos = new ByteArrayOutputStream();
		finalTable.setWidthPercentage(95f);
		Document pdf = new Document();
		pdf.setPageSize(PageSize.A4);
		pdf.setHeader(null);
		pdf.setMargins(25f, 25f, 50f, 20f);
		//PdfWriter writer = null;
		PdfWriter writer1 = null;
		File f=new File(dir+"/summaryReport");
		if(!f.exists())
			f.mkdir();
		try {
			fName="Summary_Report_" + sdf.format(new Date()) + ".pdf";
			fos = new FileOutputStream(dir + "/summaryReport/"+fName);
			//writer = PdfWriter.getInstance(pdf, baos);
			writer1 = PdfWriter.getInstance(pdf, fos);
		} catch (Exception e1) {
			Utils.handleException(e1);
		}
		writer1.setViewerPreferences(PdfWriter.PageModeUseOutlines | PdfWriter.FitWindow);
		//writer.setViewerPreferences(PdfWriter.PageModeUseOutlines | PdfWriter.FitWindow);
		pdf.setMargins(30f, 25f, 40f, 20f);
		endPageEvent.setPaoName(Utils.getPao().getPaoName());
		endPageEvent.setMe(getMonthEnding());
		//writer.setPageEvent(endPageEvent);
		writer1.setPageEvent(endPageEvent);
		pdf.open();
		addTable(pdf);
		pdf.close();
		try {
			fos.flush();
			fos.close();
			//writer.flush();
			writer1.flush();
			//writer.close();
			writer1.close();
			//baos.flush();
		} catch (IOException e3) {
			Utils.handleException(e3);
		}
		//byte[] data = baos.toByteArray();
		/*response.setContentType("application/pdf");
		response.setContentLength(data.length);
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		response.setHeader("Content-disposition",
				"attachment; filename = Summary_Report_" + sdf.format(new Date()) + ".pdf");
		try {
			response.getOutputStream().write(data);
		} catch (IOException e4) {
			Utils.handleException(e4);
		}
		faces.responseComplete();*/
		return fName;
	}

	public void updateDataFromXmlFile(Do2Control dcFromXml) {
		validUnitCode = true;
		if (dcFromXml!=null) {
			System.out.println(".........dcfromxml id "+dcFromXml.getId()+"  task "+dcFromXml.getTask() +"  fund batch " +dcFromXml.getFundBatch());
		}
 		 
		usr=Utils.getLoggedInUsr();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if (doiiControlList != null && doiiControlList.size() > 0)
			doiiControlList.clear();
		try {
			DoiiControl d2c = new DoiiControl();
			taskValue=0;
			if (!dcFromXml.getInputFileName().trim().toUpperCase().contains("RECORD_OFFICE")) {
				d2c.setUnitCode(dcFromXml.getUnitCode());
				taskValue = unitRepo.findTaskForUnitCode(d2c.getUnitCode()); 
			} else {
				d2c.setUnitCode("RECOFF");
				taskValue = 991;
			}
			 
			if (dcFromXml.getInputFileName()!=null && dcFromXml.getInputFileName().startsWith("ID")
					&& taskValue>0 && taskValue!=991) {
				dcFromXml.setFundBatch(true);
				List<Section> sectionList=null;
				Integer sectionId=0;
				sectionList=sectionRepo.findBySecName("fund cell");
				if (sectionList!=null && sectionList.size()==1) {
					sectionId=sectionList.get(0).getId();
				} else {
					System.out.println(" Alert !!!  Fund Cell - Section Not Found In The PAO. ");
				}
				if (sectionId>0 && sectionId<10) {
					if (taskValue>10 && taskValue<100) {
						taskValue=(sectionId*100)+taskValue;
					} else if (taskValue>99 && taskValue<1000) {
						taskValue=(sectionId*1000)+taskValue;
					} else {
						System.out.println(" Alert !!! Invalid TASK value for Ledger Groups in the PAO.");
					}
				} else {
					System.out.println(" Alert !!! Invalid section id value for Fund Cell Section in the PAO.");
				}
			} else {
				dcFromXml.setFundBatch(false);
			}
			System.out.println("task value --"+taskValue+"--"+d2c.getUnitCode()+"  input file name : "+dcFromXml.getInputFileName()+" dcfromxml fundbatch " + dcFromXml.getFundBatch());
			d2c.setTask(taskValue);
			d2c.setDo2No(dcFromXml.getDo2No());
			d2c.setDo2Year(Integer.parseInt(sdf.format(dcFromXml.getDo2Date()).substring(0, 4)));
			d2c.setDo2Date(dcFromXml.getDo2Date());
			d2c.setDo2Type(dcFromXml.getDo2Type().charAt(0));
			d2c.setInputFileName(dcFromXml.getInputFileName());
			d2c.setTotalItems(dcFromXml.getTotalItems());
			d2c.setCoreValidity(dcFromXml.getCoreValidity());
			d2c.setSpecimenSignatureId(dcFromXml.getSpecimenSignature() != null ? dcFromXml.getSpecimenSignature().getId() : null);
			d2c.setInputFileType(dcFromXml.getInputFileType().charAt(0));
			d2c.setFundBatch(dcFromXml.getFundBatch());
//			System.out.println("\t\t\tYYYYYYYYYYYY d2c.getinputfilename : " + d2c.getInputFileName() + "\t" + d2c.getSpecimenSignatureId() 
//				+ "\t" + d2c.getInputFileType());
			if (doiiControlList == null || doiiControlList.size() <= 0 || !doiiControlList.contains(d2c)) {
				doiiControlList.add(d2c);
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}

		
		  if(doiiControlList != null && doiiControlList.size() > 0) {
		  System.out.println("\t\t-------------- doiiControlList size : " +
		  doiiControlList.size()); } else { System.out.
		  println("\t\t-------------- doiiControlList is null -------------------------"
		  ); }
		 
		for (DoiiControl diic : doiiControlList) {
			if (diic.getSpecimenSignatureId() != null) {
				if (diic.getCoreValidity() != null) {
					jdbcTemplate.update("INSERT INTO doiitmp (unit_code,  do2_no, do2_year, task, input_file_name, "
									+ "do2_date, missing_do2, do2_type, fk_usr, core_validity, fk_specimen_signature, input_file_type, fund_batch) "
									+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?, ?)",new PreparedStatementSetter() {
										public void setValues(PreparedStatement ps) throws SQLException{
											ps.setString(1, diic.getUnitCode());
											ps.setString(2, diic.getDo2No());
											ps.setInt(3,diic.getDo2Year());
											ps.setInt(4,diic.getTask());
											ps.setString(5,diic.getInputFileName());
											ps.setDate(6,new java.sql.Date(diic.getDo2Date().getTime()));
											ps.setBoolean(7, false);
											ps.setString(8, diic.getDo2Type()+"");
											ps.setInt(9, usr.getId());
											ps.setBoolean(10, diic.getCoreValidity());
											ps.setInt(11,diic.getSpecimenSignatureId());
											ps.setString(12, diic.getInputFileType()+"");
											ps.setBoolean(13, diic.getFundBatch());
										}
									});
							
				} else {
					jdbcTemplate.update("INSERT INTO doiitmp (unit_code,  do2_no, do2_year, task, input_file_name, "
							+ "do2_date, missing_do2, do2_type, fk_usr, fk_specimen_signature, input_file_type, fund_batch) "
							+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",new PreparedStatementSetter() {
								public void setValues(PreparedStatement ps) throws SQLException{
									ps.setString(1, diic.getUnitCode());
									ps.setString(2, diic.getDo2No());
									ps.setInt(3,diic.getDo2Year());
									ps.setInt(4,diic.getTask());
									ps.setString(5,diic.getInputFileName());
									ps.setDate(6,new java.sql.Date(diic.getDo2Date().getTime()));
									ps.setBoolean(7, false);
									ps.setString(8, diic.getDo2Type()+"");
									ps.setInt(9, usr.getId());
									ps.setInt(10,diic.getSpecimenSignatureId());
									ps.setString(11, diic.getInputFileType()+"");
									ps.setBoolean(12, diic.getFundBatch());
								}
							});
					
					 
				}
			} else {
				if (diic.getCoreValidity() != null) {
					jdbcTemplate.update("INSERT INTO doiitmp (unit_code,  do2_no, do2_year, task, input_file_name, "
							+ "do2_date, missing_do2, do2_type, fk_usr, core_validity, input_file_type, fund_batch) "
							+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",new PreparedStatementSetter() {
								public void setValues(PreparedStatement ps) throws SQLException{
									ps.setString(1, diic.getUnitCode());
									ps.setString(2, diic.getDo2No());
									ps.setInt(3,diic.getDo2Year());
									ps.setInt(4,diic.getTask());
									ps.setString(5,diic.getInputFileName());
									ps.setDate(6,new java.sql.Date(diic.getDo2Date().getTime()));
									ps.setBoolean(7, false);
									ps.setString(8, diic.getDo2Type()+"");
									ps.setInt(9, usr.getId());
									ps.setBoolean(10, diic.getCoreValidity());
									ps.setString(11, diic.getInputFileType()+"");
									ps.setBoolean(12, diic.getFundBatch());
								}
							});
					
					 
				} else {
					jdbcTemplate.update("INSERT INTO doiitmp (unit_code,  do2_no, do2_year, task, input_file_name, "
							+ "do2_date, missing_do2, do2_type, fk_usr,input_file_type, fund_batch) "
							+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",new PreparedStatementSetter() {
								public void setValues(PreparedStatement ps) throws SQLException{
									ps.setString(1, diic.getUnitCode());
									ps.setString(2, diic.getDo2No());
									ps.setInt(3,diic.getDo2Year());
									ps.setInt(4,diic.getTask());
									ps.setString(5,diic.getInputFileName());
									ps.setDate(6,new java.sql.Date(diic.getDo2Date().getTime()));
									ps.setBoolean(7, false);
									ps.setString(8, diic.getDo2Type()+"");
									ps.setInt(9, usr.getId());
									ps.setString(10, diic.getInputFileType()+"");
									ps.setBoolean(11, diic.getFundBatch());
								}
							});
					
					 
				}
			}
 			System.out.println("\t~~~~~ " + diic.getInputFileName() + "\t ~~~ " + diic.getTotalItems() + "\t ~~~ ss id " 
 					+ diic.getSpecimenSignatureId() + "\t ~~~ " + diic.getInputFileType() +"\t ~~~ "+diic.getFundBatch());
		}
	}

	public void updateDoiiTmpTableWithFile(File f) {
		validRecordLength = true;
		validUnitCode = true;
		if (doiiControlList != null && doiiControlList.size() > 0)
			doiiControlList.clear();
		try {
			reader = new BufferedReader(new FileReader(f));
			LineNumberReader lreader = new LineNumberReader(new FileReader(f));
			lreader.skip(Long.MAX_VALUE);
//					 System.out.println("----------- f : " + f.getName() + "\t length : " + f.length() +
//							 "\tlreader.getLineNumber() : " + lreader.getLineNumber());
			if (getFileType().equals("N")) {
				if (f.length() != (lreader.getLineNumber() * 134)) {
					validRecordLength = false;
					omittedFileKount++;
					summaryReport.add(f.getName() + " omitted due to Invalid Record Length.");
//						 FacesMessages.instance().add(f.getName() + " omitted due to Invalid Record Length.");
				} else {
					validRecordLength = true;
				}
			}
		} catch (Exception e1) {
			Utils.handleException(e1);
		}
		if (validRecordLength) {
			try {
				reader = new BufferedReader(new FileReader(f));
				while ((line = reader.readLine()) != null) {
					if (getFileType().equals("N")) {
						if (!unitCodeList.contains(line.substring(22, 28))) {
							summaryReport.add(f.getName() + " omitted due to Invalid Unit Code.");
//			  					FacesMessages.instance().add(f.getName() + " omitted due to Invalid Unit Code.");
							validUnitCode = false;
							omittedFileKount++;
							break;
						}
					} else {
						if (!unitCodeList.contains(line.substring(21, 27))) {
							summaryReport.add(f.getName() + " omitted due to Invalid Unit Code.");
//				  				FacesMessages.instance().add(f.getName() + " omitted due to Invalid Unit Code.");
							validUnitCode = false;
							omittedFileKount++;
							break;
						}
					}
				}
			} catch (Exception e1) {
				Utils.handleException(e1);
			}
			if (validUnitCode) {
				try {
					reader = new BufferedReader(new FileReader(f));
					while ((line = reader.readLine()) != null) {
						DoiiControl d2c = new DoiiControl();
						taskValue=0;
						if (getFileType().equals("N")) {
							if (!f.getName().trim().toUpperCase().contains("RECORD_OFFICE")) {
								d2c.setUnitCode(line.substring(22, 28));
								taskValue = unitRepo.findTaskForUnitCode(line.substring(22, 28)) ;
										 
							} else {
								d2c.setUnitCode("RECOFF");
								taskValue = 991;
							}
						} else {
							d2c.setUnitCode(line.substring(21, 27));
							taskValue = unitRepo.findTaskForUnitCode( line.substring(21, 27)) ;
								 
						}
					System.out.println(" going to set task value with " +taskValue);
						d2c.setTask(taskValue);
//		 						 d2c.setTask(Integer.parseInt(line.substring(28, 31)));
						d2c.setDo2No(line.substring(32, 36));
						d2c.setDo2Year(Integer.parseInt(line.substring(43, 47)));
						if (Integer.parseInt(line.substring(41, 43)) > 12) {
							dtString = line.substring(43, 47) + "-" + line.substring(39, 41) + "-"
									+ line.substring(41, 43);
						} else {
							dtString = line.substring(43, 47) + "-" + line.substring(41, 43) + "-"
									+ line.substring(39, 41);
						}
						d2c.setDo2Date(DateManager.getDate(dtString));
						d2c.setDo2Type(line.substring(31, 32).charAt(0));
						d2c.setInputFileName(f.getName());
						d2c.setInputFileType('T');
						d2c.setCoreValidity(null);
						d2c.setSpecimenSignatureId(null);
						if (!doiiControlList.contains(d2c))
							doiiControlList.add(d2c);
					}

					for (DoiiControl diic : doiiControlList) {
						
						jdbcTemplate.update("INSERT INTO doiitmp (unit_code,  do2_no, do2_year, task, input_file_name, "
								+ "do2_date, missing_do2, do2_type, fk_usr,input_file_type) "
								+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",new PreparedStatementSetter() {
									public void setValues(PreparedStatement ps) throws SQLException{
										ps.setString(1, diic.getUnitCode());
										ps.setString(2, diic.getDo2No());
										ps.setInt(3,diic.getDo2Year());
										ps.setInt(4,diic.getTask());
										ps.setString(5,diic.getInputFileName());
										ps.setDate(6,new java.sql.Date(diic.getDo2Date().getTime()));
										ps.setBoolean(7, false);
										ps.setString(8, diic.getDo2Type()+"");
										ps.setInt(9, usr.getId());
										ps.setString(10, diic.getInputFileType()+"");
									}
								});
						
					 
					}
					/*
					 * for(DoiiControl diic : doiiControlList) {
					 * Utils.getEntityManager().createNativeQuery("INSERT INTO doiitmp " +
					 * "(unit_code,  do2_no, do2_year, task, input_file_name, do2_date, " +
					 * "missing_do2, do2_type, fk_usr, fk_specimen_signature, core_validity, input_file_type) "
					 * +
					 * "VALUES(:val1, :val2, :val3, :val4, :val5, :val6, false, :val7, :val8, :val9, :val10, :val11)"
					 * ) .setParameter("val1", diic.getUnitCode()) .setParameter("val2",
					 * diic.getDo2No()) .setParameter("val3", diic.getDo2Year())
					 * .setParameter("val4", diic.getTask()) .setParameter("val5",
					 * diic.getInputFileName()) .setParameter("val6", diic.getDo2Date())
					 * .setParameter("val7", diic.getDo2Type()) .setParameter("val8", usr.getId())
					 * .setParameter("val9", diic.getSpecimenSignatureId()) .setParameter("val10",
					 * diic.getCoreValidity()) .setParameter("val11", diic.getInputFileType())
					 * .executeUpdate(); }
					 */
				} catch (Exception e) {
					Utils.handleException(e);
				}
			}
		}
	}

	public String validateLine(int lineKount, String line) {
//		System.out.println("********** lineKount : " + lineKount + " - " + line);
		String errMsg = null;
		Object obj = null;
		boolean isValidInteger = true;
		obj = line.substring(39, 46);
		String dtString = (String) obj;
		for (int i = 0; i < ((String) obj).length(); i++) {
			if (!isValidInteger) {
				continue;
			}
			if (i + 1 < ((String) obj).length()) {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i, i + 1));
			} else {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i));
			}
		}
		if (isValidInteger) {
			if (Integer.parseInt(dtString.substring(0, 2)) > 31 || Integer.parseInt(dtString.substring(2, 4)) > 12) {
				if (errMsg == null) {
					errMsg = "Invalid do2_date at Line # " + lineKount + " from 39-46; ";
				} else {
					errMsg += "Invalid do2_date at Line # " + lineKount + " from 39-46; ";
				}
			}
		}
		isValidInteger = true;
		obj = line.substring(64, 71);
		dtString = (String) obj;
		for (int i = 0; i < ((String) obj).length(); i++) {
			if (!isValidInteger) {
				continue;
			}
			if (i + 1 < ((String) obj).length()) {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i, i + 1));
			} else {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i));
			}
		}
		if (!isValidInteger) {
			if (Integer.parseInt(dtString.substring(0, 2)) > 31 || Integer.parseInt(dtString.substring(2, 4)) > 12) {
				if (errMsg == null) {
					errMsg = "Invalid from_date at Line # " + lineKount + " from 64-71; ";
				} else {
					errMsg += "Invalid from_date at Line # " + lineKount + " from 64-71; ";
				}
			}
		}
		isValidInteger = true;
		obj = line.substring(72, 79);
		dtString = (String) obj;
		for (int i = 0; i < ((String) obj).length(); i++) {
			if (!isValidInteger) {
				continue;
			}
			if (i + 1 < ((String) obj).length()) {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i, i + 1));
			} else {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i));
			}
		}
		if (!isValidInteger) {
			if (Integer.parseInt(dtString.substring(0, 2)) > 31 || Integer.parseInt(dtString.substring(2, 4)) > 12) {
				if (errMsg == null) {
					errMsg = "Invalid to_date at Line # " + lineKount + " from 72-79; ";
				} else {
					errMsg += "Invalid to_date at Line # " + lineKount + " from 72-79; ";
				}
			}
		}
		isValidInteger = true;
		obj = line.substring(80, 85);
		for (int i = 0; i < ((String) obj).length(); i++) {
			if (!isValidInteger) {
				continue;
			}
			if (i + 1 < ((String) obj).length()) {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i, i + 1));
			} else {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i));
			}
		}
		if (!isValidInteger) {
			if (errMsg == null) {
				errMsg = "Invalid rate at Line # " + lineKount + " from 80-85; ";
			} else {
				errMsg += "Invalid rate at Line # " + lineKount + " from 80-85; ";
			}
		}
		isValidInteger = true;
		obj = line.substring(86, 89);
		for (int i = 0; i < ((String) obj).length(); i++) {
			if (!isValidInteger) {
				continue;
			}
			if (i + 1 < ((String) obj).length()) {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i, i + 1));
			} else {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i));
			}
		}
		if (!isValidInteger) {
			if (errMsg == null) {
				errMsg = "Invalid tjra at Line # " + lineKount + " from 86-89; ";
			} else {
				errMsg += "Invalid tjra at Line # " + lineKount + " from 86-89; ";
			}
		}
		isValidInteger = true;
		obj = line.substring(90, 93);
		for (int i = 0; i < ((String) obj).length(); i++) {
			if (!isValidInteger) {
				continue;
			}
			if (i + 1 < ((String) obj).length()) {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i, i + 1));
			} else {
				isValidInteger = Utils.isValidNumber(((String) obj).substring(i));
			}
		}
		if (!isValidInteger) {
			if (errMsg == null) {
				errMsg = "Invalid lra at Line # " + lineKount + " from 90-93; ";
			} else {
				errMsg += "Invalid lra at Line # " + lineKount + " from 90-93; ";
			}
		}
		isValidInteger = true;
		return errMsg;
	}
	
	/*
	 * public void aggregateAcknowledgementsToCsv() { String ackDir = dir +
	 * "/reports/DOs2_Ack_FeedBack/Acknowledgement"; String csvOutputPath = dir +
	 * "/reports/DOs2_Ack_FeedBack/acknowledgments_" + sdf.format(new Date()) +
	 * ".csv"; File dirFile = new File(ackDir);
	 * 
	 * System.out.println("Starting CSV generation at " + new java.util.Date() +
	 * " for directory: " + ackDir);
	 * 
	 * if (!dirFile.exists() || !dirFile.isDirectory()) {
	 * summaryReport.add("Error: Acknowledgement directory not found: " + ackDir);
	 * System.out.println("Error: Acknowledgement directory not found: " + ackDir);
	 * return; } System.out.println("Acknowledgement directory found: " + ackDir);
	 * 
	 * File[] xmlFiles = dirFile.listFiles((file, name) ->
	 * name.toLowerCase().endsWith("_9.xml")); if (xmlFiles == null ||
	 * xmlFiles.length == 0) { summaryReport.add("No _9.xml files found in " +
	 * ackDir); System.out.println("No _9.xml files found in " + ackDir); return; }
	 * System.out.println("Found " + xmlFiles.length + " _9.xml files in " +
	 * ackDir);
	 * 
	 * int totalItems = 0; List<String[]> dataRows = new ArrayList<>(); String[]
	 * header = {"Filename", "PTO_Group", "SUS_No", "PTO_No", "PTO_Date",
	 * "L_PTO_No", "L_PTO_Date", "Tot_Items", "Obsn_header"}; dataRows.add(header);
	 * System.out.println("CSV header prepared: " + String.join(",", header));
	 * 
	 * for (File xmlFile : xmlFiles) { System.out.println("Processing XML file: " +
	 * xmlFile.getName()); try { javax.xml.parsers.DocumentBuilderFactory docFactory
	 * = javax.xml.parsers.DocumentBuilderFactory.newInstance();
	 * javax.xml.parsers.DocumentBuilder docBuilder =
	 * docFactory.newDocumentBuilder(); org.w3c.dom.Document doc =
	 * docBuilder.parse(xmlFile); doc.getDocumentElement().normalize();
	 * 
	 * org.w3c.dom.Node agency = doc.getFirstChild(); org.w3c.dom.NodeList
	 * agencyList = agency.getChildNodes(); String[] row = new
	 * String[header.length]; row[0] = xmlFile.getName();
	 * System.out.println("Parsing XML file: " + xmlFile.getName() + ", root node: "
	 * + agency.getNodeName());
	 * 
	 * for (int i = 0; i < agencyList.getLength(); i++) { org.w3c.dom.Node
	 * headerNode = agencyList.item(i); if
	 * (headerNode.getNodeName().equals("Header")) { org.w3c.dom.NodeList headerList
	 * = headerNode.getChildNodes(); System.out.println("Found Header node in " +
	 * xmlFile.getName() + ", processing " + headerList.getLength() +
	 * " child nodes"); for (int j = 0; j < headerList.getLength(); j++) {
	 * org.w3c.dom.Node node = headerList.item(j); String nodeValue =
	 * node.getTextContent() != null ? node.getTextContent().trim() : ""; switch
	 * (node.getNodeName()) { case "PTO_Group": row[1] = nodeValue;
	 * System.out.println("PTO_Group: " + nodeValue); break; case "SUS_No": row[2] =
	 * nodeValue; System.out.println("SUS_No: " + nodeValue); break; case "PTO_No":
	 * row[3] = nodeValue; System.out.println("PTO_No: " + nodeValue); break; case
	 * "PTO_Date": row[4] = nodeValue; System.out.println("PTO_Date: " + nodeValue);
	 * break; case "L_PTO_No": row[5] = nodeValue; System.out.println("L_PTO_No: " +
	 * nodeValue); break; case "L_PTO_Date": row[6] = nodeValue;
	 * System.out.println("L_PTO_Date: " + nodeValue); break; case "Tot_Items":
	 * row[7] = nodeValue; try { int items = Integer.parseInt(nodeValue); totalItems
	 * += items; System.out.println("Added Tot_Items: " + items +
	 * ", Running total: " + totalItems); } catch (NumberFormatException e) {
	 * System.out.println("Warning: Invalid Tot_Items value in " + xmlFile.getName()
	 * + ": " + nodeValue + ", skipping for total"); } break; case "Obsn_header":
	 * row[8] = nodeValue; System.out.println("Obsn_header: " + nodeValue); break; }
	 * } } } dataRows.add(row); System.out.println("Added row for " +
	 * xmlFile.getName() + " to CSV data"); } catch (Exception e) {
	 * summaryReport.add("Error processing file " + xmlFile.getName() + ": " +
	 * e.getMessage()); System.out.println("Error processing file " +
	 * xmlFile.getName() + ": " + e.getMessage()); } }
	 * 
	 * File csvFile = new File(csvOutputPath); File parentDir =
	 * csvFile.getParentFile(); if (!parentDir.exists()) {
	 * System.out.println("Creating output directory: " +
	 * parentDir.getAbsolutePath()); parentDir.mkdirs(); }
	 * System.out.println("Output CSV path: " + csvOutputPath);
	 * 
	 * try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
	 * for (String[] row : dataRows) { for (int i = 0; i < row.length; i++) { if
	 * (row[i] == null) { row[i] = ""; } else if (row[i].contains(",") ||
	 * row[i].contains("\"")) { row[i] = "\"" + row[i].replace("\"", "\"\"") + "\"";
	 * } } String rowData = String.join(",", row);
	 * System.out.println("Writing CSV row: " + rowData); writer.write(rowData);
	 * writer.newLine(); }
	 * 
	 * // Add a blank row with empty strings String[] blankRow = new
	 * String[header.length]; for (int i = 0; i < blankRow.length; i++) {
	 * blankRow[i] = ""; // Explicitly set to empty string }
	 * writer.write(String.join(",", blankRow)); writer.newLine();
	 * 
	 * // Add total items row String[] totalRow = new String[header.length];
	 * totalRow[6] = "Total items"; // 7th column (L_PTO_Date) totalRow[7] =
	 * String.valueOf(totalItems); // 8th column (Tot_Items) for (int i = 0; i <
	 * totalRow.length; i++) { if (totalRow[i] == null) { totalRow[i] = ""; //
	 * Explicitly replace null with blank } else if (totalRow[i].contains(",") ||
	 * totalRow[i].contains("\"")) { totalRow[i] = "\"" + totalRow[i].replace("\"",
	 * "\"\"") + "\""; } } String totalRowData = String.join(",", totalRow);
	 * System.out.println("Writing total row: " + totalRowData);
	 * writer.write(totalRowData); writer.newLine();
	 * 
	 * summaryReport.add("CSV file generated at: " + csvOutputPath);
	 * summaryReport.add("Total items across all files: " + totalItems);
	 * System.out.println("CSV file generated at: " + csvOutputPath);
	 * System.out.println("Total items across all files: " + totalItems); } catch
	 * (IOException e) { summaryReport.add("Error writing CSV file " + csvOutputPath
	 * + ": " + e.getMessage()); System.out.println("Error writing CSV file " +
	 * csvOutputPath + ": " + e.getMessage()); } }
	 */

	List<DoiiControl> doiiControlList = new ArrayList<DoiiControl>();
	List<DoiiControl> duplicatedoiiList = new ArrayList<DoiiControl>();
	List<Misc> miscCurrentQuarterList;
	BatchControl bc = new BatchControl();

	public boolean isValidMonthEnding() {
		boolean isValid = true;
		boolean monthEndFound = false;
		List<Misc> closingMonthEndingList = miscRepo.findAllByKey1AndValue1AndCurrent( Constants.MONTH_CLOSING, getMonthEnding(), true) ;
				
				 
		if (closingMonthEndingList != null && closingMonthEndingList.size() > 0) {
			isValid = false;
			setErrorMessage("Closed done for Month : " + getMonthEnding() + ".  Cannot accept a Batch.");
		} else {
			if (batchCtrlList != null && batchCtrlList.size() > 0) {
				for (BatchControlObject bco : batchCtrlList) {
					if (bco.getMe().equals(getMonthEnding())) {
						monthEndFound = true;
						break;
					}
				}
				if (monthEndFound) {
					for (BatchControlObject bco : batchCtrlList) {
						if (bco.getMe().equals(getMonthEnding())) {
							if (bco.getToDate() != null && DateManager.getToday().after(bco.getToDate())) {
								isValid = false;
								setErrorMessage("Cannot accept a Batch for Month Ending : " + getMonthEnding()
										+ " after " + bco.getToDate());
							} else if (bco.getFromDate() != null && DateManager.getToday().before(bco.getFromDate())) {
								isValid = false;
								setErrorMessage("Cannot accept a Batch for Month Ending : " + getMonthEnding()
										+ " before " + bco.getFromDate());
							}
						}
					}
				} else {
					isValid = false;
					setErrorMessage("Cannot accept a Batch for Month Ending : " + getMonthEnding()
							+ ".  Date from which Batches are accepted not defined.  Contact System Administrator.");
				}
			} else {
				isValid = false;
				setErrorMessage("Cannot accept a Batch for Month Ending : " + getMonthEnding()
						+ ".  Date from which Batches are accepted not defined.  Contact System Administrator.");
			}
		}
		System.out.println("is valid "+isValid+"--"+errorMessage+"--");
		if(batchCtrlList!=null)
			batchCtrlList.forEach(bc->System.out.println(bc.getId()+"--"+bc.getFromDate()));
		return isValid;
	}

	public Boolean validateSoftCopy(File f, int grpIndex, String fileType, XmlDataObject xdo) {
		boolean validFile = true;
		try {
			if (f.getName().toLowerCase().endsWith(".txt")) {
				BufferedReader reader = new BufferedReader(new FileReader(f));
				if (fileType.equals("N")) {
					while ((line = reader.readLine()) != null) {
						if (line.substring(grpIndex, grpIndex + 1).equals("C")) {
							validFile = false;
							continue;
						}
					}
				} else {
					while ((line = reader.readLine()) != null) {
						if (!line.substring(grpIndex, grpIndex + 1).equals("C")) {
							validFile = false;
							continue;
						}
					}
				}

				/*
				 * } else { if(fileType.equals("N")) { if(xdo.getDo2_group().endsWith("C")) {
				 * validFile = false; } } else { if(!xdo.getDo2_group().endsWith("C")) {
				 * validFile = false; } }
				 */
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		return validFile;
	}

	public void resetRecordLength(File sourceFile, LineNumberReader lreader) {
		try {
			lreader.skip(Long.MAX_VALUE);
			if (sourceFile.length() > (lreader.getLineNumber() * 134)) {
				Runtime r = Runtime.getRuntime();
				File outFile = null;
//				outFile = new File(Utils.getJbossHome() + "/checkDuplicateDos2/a.txt");
				outFile = new File(dir + "/checkDuplicateDos2/new_" + sourceFile.getName());
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, true)));
				String runCmd = "cut -c 1-133 " + sourceFile.getAbsolutePath();
				/*
				 * System.out.println("----------- sourceFile : " + sourceFile.getName() +
				 * "\t length : " + sourceFile.length() + "\tlreader.getLineNumber() : " +
				 * lreader.getLineNumber());
				 */
				try {
					Process p = r.exec(runCmd);
					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((line = br.readLine()) != null) {
						bw.write(line + "\n");
					}
					br.close();
					bw.flush();
					bw.close();
					p.destroy();
					File dirr = new File(dir + "/checkDuplicateDos2");
					FileFilter fileFilter = new WildcardFileFilter("new_*.txt");
					File[] files = dirr.listFiles(fileFilter);
//					System.out.println("\t\t***************** files length : " + files.length);
					for (File f : files) {
						/*
						 * System.out.println("\t^^^^^^^^^^ outputFile : " + sourceFile.getName() +
						 * "\t length : " + sourceFile.length() + "\tlreader.getLineNumber() : " +
						 * lreader.getLineNumber() + "\t*134 : " + lreader.getLineNumber()*134 +
						 * "\ta.txt : " + f.length());
						 */
//						System.out.println("--------- f.name : " + f.getName() + "\tsource file : " + sourceFile.getName());
						if (f.getName().substring(4).equals(sourceFile.getName())) {
							f.renameTo(sourceFile);
						}
					}
				} catch (Exception e) {
					Utils.handleException(e);
				}
			}
		} catch (Exception e1) {
			Utils.handleException(e1);
		}
	}

	public void addTable(Document doc) {
		int slNo = 0;
		int grtotal = 0;
		PdfPTable tblHeader, tblDetail, tblTotal, tblfinal;

		Font f1 = FontFactory.getFont("HELVETICA", 10f, Font.BOLD, Color.BLACK);
		Font f2 = FontFactory.getFont("HELVETICA", 8f, Font.BOLD, Color.BLACK);
		Font f3 = FontFactory.getFont("HELVETICA", 8f, Font.BOLD, Color.BLACK);
		Font f6 = FontFactory.getFont("HELVETICA", 13f, Font.BOLD, Color.BLUE);
		SimpleDateFormat printDt = new SimpleDateFormat("dd-MM-yyyy");
		tblHeader = new PdfPTable(new float[] { 8f, 92f });
		tblDetail = new PdfPTable(new float[] { 8f, 92f });
		tblfinal = new PdfPTable(new float[] { 40f });
		tblHeader.setWidthPercentage(40);
		tblDetail.setWidthPercentage(40);
		tblfinal.setWidthPercentage(95);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, tblHeader, "Sl. No.", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, tblHeader, "Remarks", f1);
		for (String s : summaryReport) {
			slNo++;
			ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, tblDetail, String.valueOf(slNo) + ")",
					f2);
			ReportUtils.addCell2Table(Element.ALIGN_JUSTIFIED, Element.ALIGN_MIDDLE, tblDetail, String.valueOf(s), f2);
		}
		tblfinal.addCell(new PdfPCell(tblHeader));
		tblfinal.addCell(new PdfPCell(tblDetail));

		tblfinal.setHeaderRows(1);
		tblfinal.setSplitLate(false);
		tblfinal.setKeepTogether(true);
		try {
			doc.add(tblfinal);
		} catch (DocumentException e) {
			Utils.handleException(e);
		}
	}
}

class DoiiControl {
	String unitCode, do2No, inputFileName, receivedInPreviousBatch;
	Integer totalItems, do2Year, task, specimenSignatureId;
	Date do2Date;
	Character do2Type, inputFileType;
	Boolean coreValidity;
	Boolean fundBatch;

	public Boolean getFundBatch() {
		return fundBatch;
	}

	public void setFundBatch(Boolean fundBatch) {
		this.fundBatch = fundBatch;
	}

	public String getUnitCode() {
		return unitCode;
	}

	public void setUnitCode(String unitCode) {
		this.unitCode = unitCode;
	}

	public String getDo2No() {
		return do2No;
	}

	public void setDo2No(String do2No) {
		this.do2No = do2No;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

	public String getReceivedInPreviousBatch() {
		return receivedInPreviousBatch;
	}

	public void setReceivedInPreviousBatch(String receivedInPreviousBatch) {
		this.receivedInPreviousBatch = receivedInPreviousBatch;
	}

	public Integer getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(Integer totalItems) {
		this.totalItems = totalItems;
	}

	public Integer getDo2Year() {
		return do2Year;
	}

	public void setDo2Year(Integer do2Year) {
		this.do2Year = do2Year;
	}

	public Integer getTask() {
		return task;
	}

	public void setTask(Integer task) {
		this.task = task;
	}

	public Integer getSpecimenSignatureId() {
		return specimenSignatureId;
	}

	public void setSpecimenSignatureId(Integer specimenSignatureId) {
		this.specimenSignatureId = specimenSignatureId;
	}

	public Date getDo2Date() {
		return do2Date;
	}

	public void setDo2Date(Date do2Date) {
		this.do2Date = do2Date;
	}

	public Character getDo2Type() {
		return do2Type;
	}

	public void setDo2Type(Character do2Type) {
		this.do2Type = do2Type;
	}

	public Character getInputFileType() {
		return inputFileType;
	}

	public void setInputFileType(Character inputFileType) {
		this.inputFileType = inputFileType;
	}

	public Boolean getCoreValidity() {
		return coreValidity;
	}

	public void setCoreValidity(Boolean coreValidity) {
		this.coreValidity = coreValidity;
	}
}

class LocalEndPageEvent extends PdfPageEventHelper {
	protected PdfTemplate template;
	protected BaseFont helv;
	String paoName;
	String me;
	String batchNo, batchFileName;
	int task;
	int pageCount;
	int totalPages;

	public String getBatchNo() {
		return batchNo;
	}

	public void setBatchNo(String batchNo) {
		this.batchNo = batchNo;
	}

	public String getBatchFileName() {
		return batchFileName;
	}

	public void setBatchFileName(String batchFileName) {
		this.batchFileName = batchFileName;
	}

	public String getMe() {
		return me;
	}

	public void setMe(String me) {
		this.me = me;
	}

	public int getTask() {
		return task;
	}

	public void setTask(int task) {
		this.task = task;
	}

	public String getPaoName() {
		return paoName;
	}

	public void setPaoName(String paoName) {
		this.paoName = paoName;
	}

	Font f6 = FontFactory.getFont("HELVETICA", 13f, Font.BOLD, Color.BLUE);
	Font f7 = FontFactory.getFont("HELVETICA", 10f, Font.NORMAL, Color.BLUE);
	Font f8 = FontFactory.getFont("HELVETICA", 10f, Font.BOLDITALIC, Color.BLACK);

	public void onOpenDocument(PdfWriter writer, Document document) {
		template = writer.getDirectContent().createTemplate(100, 100);
		template.setBoundingBox(new Rectangle(-20, -20, 100, 100));
		try {
			helv = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
		} catch (DocumentException e1) {
			Utils.handleException(e1);
		} catch (IOException e1) {
			Utils.handleException(e1);
		}
	}

	public void onEndPage(PdfWriter writer, Document document) {
		pageCount++;
		SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
		PdfContentByte cb = writer.getDirectContent();
		Rectangle page = document.getPageSize();
		cb.saveState();
		String text = "Page " + writer.getPageNumber() + " of ";
		float textBase = document.top() + 5;
		float textSize = helv.getWidthPoint(text, 8f);
		String inLieuText = "Date : " + df1.format(DateManager.getCurrentDate());
		cb.beginText();
		cb.setFontAndSize(helv, 8);
		float adjust = helv.getWidthPoint("0", 8);
		cb.setTextMatrix(document.right() - textSize - adjust, textBase);
		cb.showText(text);
		cb.endText();
		cb.beginText();
		cb.setTextMatrix(document.left() - adjust, textBase);
		cb.showText(inLieuText);
		cb.endText();
		cb.addTemplate(template, document.right() + adjust, textBase);
		cb.restoreState();
		if (writer.getPageNumber() >= 1) {
			PdfPTable head = new PdfPTable(new float[] { 80f });
			head.setWidthPercentage(80f);

			String reportHeader = null;

			if (this.paoName != null) {
				reportHeader = "PAO : " + this.paoName + "   - Summary Report ";
			}

			PdfPCell pdfCell = null;

			pdfCell = new PdfPCell(new Phrase(reportHeader, f6));
			pdfCell.setHorizontalAlignment(Element.ALIGN_CENTER);
			pdfCell.setVerticalAlignment(Element.ALIGN_TOP);
			pdfCell.setBorderWidth(0);

			head.addCell(pdfCell);

			head.setSpacingAfter(50f);

			head.setTotalWidth(page.getWidth() - (document.leftMargin() + document.rightMargin()));

			head.writeSelectedRows(-1, -1, document.leftMargin(),
					page.getHeight() - document.topMargin() + head.getTotalHeight(), writer.getDirectContent());
		}
	}

	public void onCloseDocument(PdfWriter writer, Document document) {
		this.totalPages = writer.getPageNumber() - 1;
		template.beginText();
		template.setFontAndSize(helv, 8);
		template.setTextMatrix(0, 0);
		template.showText(String.valueOf(this.totalPages));
		template.endText();
	}

}
