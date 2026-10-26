package itsdc.dolphin.do2.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import itsdc.dolphin.common.NamePublicKey;
import itsdc.dolphin.models.AppendixJ;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Do2Control;
import itsdc.dolphin.models.Do2Xml;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EnrollDo2Xml;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Section;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.repositories.AppendixJRepository;
import itsdc.dolphin.repositories.Do2ControlRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.Do2XmlRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EnrollDo2XmlRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.repositories.SectionRepository;
import itsdc.dolphin.repositories.TaskRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

public class UploadDigiBatches {

	String dir;
	Do2XmlRepository do2XmlRepo;
	public Do2XmlRepository getDo2XmlRepo() {
		return do2XmlRepo;
	}
	public void setDo2XmlRepo(Do2XmlRepository do2XmlRepo) {
		this.do2XmlRepo = do2XmlRepo;
	}
	Do2ControlRepository do2ControlRepo;
	UnitRepository unitRepo;
	String me;
	MiscRepository miscRepo;
	EmployeeRepository empRepo;
	Do2Repository do2Repo;
	AppendixJRepository appjRepo;
	EnrollDo2XmlRepository enrollDo2XmlRepo;

	SectionRepository sectionRepo;// Added--10122025

	Environment env;

	String batchFileName;
	String keyPath;
	String zipFileName;
	HashMap<Integer, Integer> taskBatchMap = new HashMap<Integer, Integer>();

	HashMap<Integer, Integer> batchCountMap = new HashMap<Integer, Integer>();
	HashMap<String, String> auditCageMap = null;
	HashMap<String, String> accountingCageMap = null;

	static Integer BATCH_SIZE = 500;

	public AppendixJRepository getAppjRepo() {
		return appjRepo;
	}

	public void setAppjRepo(AppendixJRepository appjRepo) {
		this.appjRepo = appjRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public UnitRepository getUnitRepo() {
		return unitRepo;
	}

	public void setUnitRepo(UnitRepository unitRepo) {
		this.unitRepo = unitRepo;
	}

	public String getZipFileName() {
		return zipFileName;
	}

	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}

	public String getKeyPath() {
		return keyPath;
	}

	public void setKeyPath(String keyPath) {
		this.keyPath = keyPath;
	}

	public MiscRepository getMiscRepo() {
		return miscRepo;
	}

	public void setMiscRepo(MiscRepository miscRepo) {
		this.miscRepo = miscRepo;
	}

	public Do2ControlRepository getDo2ControlRepo() {
		return do2ControlRepo;
	}

	public void setDo2ControlRepo(Do2ControlRepository do2ControlRepo) {
		this.do2ControlRepo = do2ControlRepo;
	} 

	public String getBatchFileName() {
		return batchFileName;
	}

	public void setBatchFileName(String batchFileName) {
		this.batchFileName = batchFileName;
	}

	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getMe() {
		return me;
	}

	public void setMe(String me) {
		this.me = me;
	}

	public SectionRepository getSectionRepo() {
		return sectionRepo;
	}

	public void setSectionRepo(SectionRepository sectionRepo) {
		this.sectionRepo = sectionRepo;
	}
	public EnrollDo2XmlRepository getEnrollDo2XmlRepo() {
		return enrollDo2XmlRepo;
	}

	public void setEnrollDo2XmlRepo(EnrollDo2XmlRepository enrollDo2XmlRepo) {
		this.enrollDo2XmlRepo = enrollDo2XmlRepo;
	}
	public static HashMap<String, String> MONTH_MAP;

	public static String[][] monthMapping = { { "01", "January" }, { "02", "February" }, { "03", "March" },
			{ "04", "April" }, { "05", "May" }, { "06", "June" }, { "07", "July" }, { "08", "August" },
			{ "09", "September" }, { "10", "October" }, { "11", "November" }, { "12", "December" } };

	public static HashMap<String, String> getMonthMap() {
		if (MONTH_MAP != null)
			return MONTH_MAP;
		MONTH_MAP = new HashMap<String, String>();
		for (int i = 0; i < monthMapping.length; i++) {
			String keyVal[] = monthMapping[i];
			MONTH_MAP.put(keyVal[0], keyVal[1]);
		}
		return MONTH_MAP;
	}

	public UploadSummary importDigitBatches() {
		File dir1 = new File(dir + "/checkDuplicateDos2");
		File[] files = dir1.listFiles();
		int xmlFiles = 0, pdfFiles = 0;
		List<File> filesList = new ArrayList<File>();
		StringBuffer sb = new StringBuffer();
		UploadSummary summary = new UploadSummary();
		String mesg = "", ip = "", mac = "";
		BufferedWriter do2XmlWriter = null, do2cWriter = null, do2Writer = null, enrollDo2XmlWriter = null,
				enrollDo2Writer = null;
		List<String> filesTobeMoved2DuplicateDo2Folder = new ArrayList<String>();
		List<String> attachmentList = new ArrayList<String>();
		List<String> summaryReport = new ArrayList<String>();
		List<String> xmlFileList = new ArrayList<String>();
		List<String> invalidList = new ArrayList<String>();
		int validFiles = 0, invalidFiles = 0, duplicateFiles = 0;
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		int usrId = Utils.getLoggedInUsr().getId();
		//mac=Utils.getMacAddress(request.getRemoteAddr());
		ip = request.getRemoteAddr();
		mac = ArpUtil.getMacAddress(ip);

		try {
			do2XmlWriter = new BufferedWriter(new FileWriter(dir + "/do2Xml.csv"));
			do2cWriter = new BufferedWriter(new FileWriter(dir + "/do2c.csv"));
			do2Writer = new BufferedWriter(new FileWriter(dir + "/do2.csv"));

			enrollDo2XmlWriter = new BufferedWriter(new FileWriter(dir + "/enrollDo2Xml.csv"));
			enrollDo2Writer = new BufferedWriter(new FileWriter(dir + "/enrollDo2.csv"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int batchCount = 1;
		int fileCount = 0;
		String batch = (Integer.parseInt(me.substring(3)) * 100 + Integer.parseInt(me.substring(0, 2))) + ""
				+ batchCount;
		int batchNo = Integer.parseInt(batch);
		Pageable limit = PageRequest.of(0, 1);

		if (auditCageMap == null) {
			auditCageMap = new HashMap<String, String>();
			List<String> appjList = appjRepo.findAuditCageDescription();
			if (appjList != null && appjList.size() > 0) {
				for (String appj : appjList) {
					auditCageMap.put(appj, "AT");
				}
			}
		}

		for (File f : files) {
			fileCount++;

			System.out.println("updating file " + f.getName());
			if (Utils.fileTypeBasedOnContentType(f.getAbsolutePath()).equals("XML")) {
				xmlFiles++;
				writeToFile(f, do2XmlWriter, do2cWriter, do2Writer, enrollDo2XmlWriter, enrollDo2Writer, xmlFileList,
						usrId, ip, mac, summary);
			}
			if (Utils.fileTypeBasedOnContentType(f.getAbsolutePath()).equals("PDF")) {
				pdfFiles++;
				if (!attachmentList.contains(f.getName())) {
					attachmentList.add(f.getName());
				}
			}
			summary.setTotalPdfs(pdfFiles);
		}
		try {
			do2XmlWriter.flush();
			do2XmlWriter.close();
			do2cWriter.flush();
			do2cWriter.close();
			do2Writer.flush();
			do2Writer.close();
			enrollDo2XmlWriter.close();
			enrollDo2Writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String dbwriteerror = writeToDb(batchFileName);
		if (!dbwriteerror.startsWith("valid")) {

			summary.setMessage(dbwriteerror);
			return summary;

		} else {

			// no message required
			summary.setMessage("");
		}

		// clean up files
		for (String str : attachmentList) {
			File f = new File(dir + "/checkDuplicateDos2/" + str);
			if (f.exists()) {
				f.renameTo(new File(dir + "/dos2Attachments/" + f.getName()));
			}
		}
		String destinationFolder = dir + "/ImportDos2/" + getMonthMap().get(me.substring(0, 2)) + "-"
				+ me.substring(3, 7) + "/Xml_Files";
		File fd = new File(destinationFolder);
		if (!fd.exists())
			fd.mkdirs();

		for (String str : xmlFileList) {
			File f = new File(dir + "/checkDuplicateDos2/" + str);
			if (f.exists())
				f.renameTo(new File(destinationFolder + "/" + f.getName()));

		}

		mesg = sb.toString();
		System.out.println("mesg==============" + mesg);
		summary.setXmlFileNames(xmlFileList);

		return summary;
	}

	private String writeToDb(String fname) {

		// Connection con = getConnection();
		String dbmesg = "valid";

		// Changed on 15.06.2026
		try (Connection con = getConnection()) {

			CopyManager copyManager = new CopyManager((BaseConnection) con);

			FileReader fileReader = new FileReader(dir + "/do2Xml.csv");

			copyManager.copyIn(
					"copy do2_xml(do2_group,sus_no,unit_code, pto_no, pto_date, last_pto_no, last_pto_date, do2_no, do2_date, total_items, army_no,check_digit,\n"
							+ " rank_name, soldier_category, employee_name, do2_item_no, description,  from_date,to_date,column_a,column_b,\n"
							+ "             column_c,ref_do2_no,amount,sra,nra,pto_remarks,remarks1,remarks2,remarks3,remarks4, remarks5,\n"
							+ "             remarks6, remarks7, remarks8, remarks9, remarks10,remarks11,remarks12,remarks13,\n"
							+ "          remarks14,remarks15,remarks16,remarks17,remarks18,remarks19,remarks20,remarks21,remarks22,\n"
							+ "             remarks23,remarks24,remarks25,  attachment, delayed,cfa_sanction_received,cfa_sanction_letter_no,\n"
							+ "             cfa_sanction_dt,sig_verified,approved_by,  record_status,upload_date,xml_file_name,\n"
							+ "             upload_file_name, uploaded_by, ip, mac, created_at, reason, month_ending, do2_type)\n"
							+ "  from STDIN  with delimiter as '~'  NULL as ''",
					fileReader);

			fileReader.close();

			System.out.println("writing enroll do2xml over");

			fileReader = new FileReader(dir + "/do2c.csv");

			copyManager.copyIn(
					"copy do2_control(unit_code,do2_no,total_items,month_ending,dt_of_receipt,batch_number,batch_dt,created_at,do2_year,\n"
							+ "                task,imported,input_file_name,do2_date,do2_type,input_file_type,batch_file_name,fund_batch) "
							+ "  from STDIN  with delimiter as '~'  NULL as ''",
					fileReader);

			fileReader.close();

			fileReader = new FileReader(dir + "/do2.csv");

			copyManager.copyIn(
					"copy do2(batch_no,unit_code,fk_unit,army_no,check_digit,fk_employee,do2_group,do2_type,do2_no,do2_item_no,do2_date,"
							+ "punishment,description,from_date,to_date,rate,tjra,lra,column_a,column_b,column_c,ref_do2_no,pto_remarks,status,reason,"
							+ "transcription_type,qe,audit_cage,time_bar_sanction,approved,input_qe,delayed,cfa_sanction_received,cfa_sanction_letter_no,cfa_sanction_date,attachment,column9_remarks) "
							+ "  from STDIN  with delimiter as '~'  NULL as ''",
					fileReader);

			fileReader.close();
			
			fileReader = new FileReader(dir + "/enrollDo2Xml.csv");
	 System.out.println("......filereader .... "+fileReader.toString());
			copyManager.copyIn(
					"copy enroll_do2_xml (do2_group, sus_no, unit_code, pto_no, pto_date, last_pto_no, last_pto_date, do2_no, do2_date,"
					+ " total_items, do2_item_no, svc_type, army_no, check_digit, rank_name, name, soldier_category, entry_trade, "
					+ " toe_colour_service, birth_date, religion, civil_edu, food_bill, bank_acno, ifs_code, pan_no, aadhar_no, email_id, "
					+ " mobile_no, gender, gso_code, cca_code, description, enroll_date, advance_amount, pto_remarks, remarks1, remarks2,"
					+ " remarks3, remarks4, remarks5, remarks6, remarks7, remarks8, remarks9, remarks10, remarks11, remarks12, remarks13, "
					+ " remarks14, remarks15, remarks16, remarks17, remarks18, remarks19, remarks20, remarks21, remarks22, "
					+ "	remarks23, remarks24, remarks25, attachment, delayed, cfa_sanction_received, cfa_sanction_letter_no, "
					+ " cfa_sanction_dt, sig_verified, approved_by, record_status, created_at, upload_file_name, xml_file_name, "
					+ " uploaded_by, ip, mac, reason, month_ending, do2_type ) " 
					+ " from STDIN  with delimiter as '~'  NULL as ''",
					fileReader);

			fileReader.close();

			fileReader = new FileReader(dir + "/enrollDo2.csv");
			 System.out.println("......filereader .... "+fileReader.toString());
					copyManager.copyIn(
							"copy enroll_do2 (batch_no, do2_group, do2_type, unit_code, do2_no, do2_item_no, do2_date, army_no, check_digit, "
							+ " employee_name, entry_category, entry_trade, terms_colour_service, terms_max_age_retention, date_of_birth, "
							+ " date_of_arrival, cca_code, advance_amount, gso_code, remarks, status, reason, da_days, da_hours,"
							+ " age_indicator, transcription_type, qe, fk_auditor, created_at, ta_appointment_category, approved,"
							+ " commutation_willingness, gratuity_amount, gratuity_surrendered_dsc, mobile_no, email_id, "
							+ " uid_no, civil_education, bank_account_no, ifsc_code, pan_number, date_of_departure,"
							+ " arrival_time, departure_time, gender, over_age_exemption ) " 
							+ " from STDIN  with delimiter as '~'  NULL as ''",
							fileReader);
					
			fileReader.close();

			System.out.println("writing do2control over");

			List<Integer> batchList = do2XmlRepo.findDistinctPendingBatch();

			if (batchList != null && batchList.size() > 0) {

				for (Integer bat : batchList) {

					List<Misc> miscList = miscRepo.findByKey1AndValue1AndIntNumber1AndCurrent("Do2Upload", me, bat,
							true);

					if (miscList == null || miscList.size() == 0) {

						Misc mis = new Misc();

						mis.setKey1("Do2Upload");
						mis.setValue1(me);
						mis.setValue2("Ready");
						mis.setIntNumber1(bat);
						mis.setCurrent(true);

						miscRepo.save(mis);
					}
				}
			}

			// ONLY RETURN VALID
			dbmesg = "valid";

		} catch (SQLException e1) {

			e1.printStackTrace();
			dbmesg = "Error writing do2. Check terminal log.";

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			dbmesg = "Error writing do2. Check terminal log.";

		} catch (IOException e) {

			e.printStackTrace();
			dbmesg = "Error writing do2. Check terminal log.";
		}

		return dbmesg;
	}

	@Transactional
	public void writeToFile(File f, BufferedWriter do2XmlWriter, BufferedWriter do2cWriter, BufferedWriter do2Writer,
			BufferedWriter enrollDo2XmlWriter, BufferedWriter enrollDo2Writer, List<String> xmlFileList, Integer usrId,
			String ip, String mac, UploadSummary summary) {
		// TODO Auto-generated method stub
		Document doc = null;
		String rootName = null;
		List list = null;
		Attribute attribute = null;
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");

		DecimalFormat df = new DecimalFormat("##.##");
		Object verifyDigitalSignaturesOfXmlObj = null;
		StringBuffer sb = new StringBuffer();
		String ptoGroup = "", susNo = null, ptoNo = null, ptoDate = null, totalItems = null, rejectionReason;
		String unitcd = null, lastPtoNo = null, lastPtoDate = null;
		Date lastDo2Date = null, do2Date = null;
		String do2No;
		String keyVerified = "N";
		String fileName = null, susNoWithoutCheckDigit = "", do2DateStr = "", unitCode = "", batchDateStr = "";
		int fkUsr = Utils.getLoggedInUsr().getId();
		int itemCount = 0, do2Year = 0, task = 0, batchCount = 0;
		int fkUnit = 0;
		long fkEmployee = 0;
		Integer batchNo = 0;
		batchDateStr = dbFormat.format(DateManager.getToday());
		String do2Type = null;// added--21032026

		try {
			SAXReader reader = new SAXReader();
			doc = reader.read(new File(f.getAbsolutePath()));
		} catch (Exception e) {
			e.printStackTrace();
		}

//		// Added--10122025
//		boolean isFundBatch = isFundBatch(doc);
//		System.out.println("-----------------------is Fund Batch---------------:"+ isFundBatch);
//		int taskValue;

		boolean isFundBatch = false;
		boolean isEnrollBatch = false;

		fileName = f.getName();
		try {
			Element rootElement = doc.getRootElement();
			rootName = rootElement.getName() + "//Header//";

			System.out.println("rootname====" + rootName);

			if (!rootName.startsWith("BRAHMA"))
				return;

			list = doc.selectNodes(rootName + "PTO_Group");
			ptoGroup = ((Element) list.get(0)).getText();

			if (ptoGroup.equals("ID")) {// Added--10122025
				isFundBatch = true;
			}

			if (ptoGroup.equals("IC")) {// Added--24072026
				isEnrollBatch = true;
			}

			list = doc.selectNodes(rootName + "SUS_No");
			susNo = ((Element) list.get(0)).getText();
			if (UploadHelper.isLastCharAlpha(susNo)) {
				susNoWithoutCheckDigit = susNo.substring(0, susNo.trim().length());
			} else
				susNoWithoutCheckDigit = susNo;

			System.out.println("  susnowocd " + susNoWithoutCheckDigit + "  unitcode " + unitCode + "  fkunit " + fkUnit
					+ "  task " + task);

			int taskValue;
			List<Unit> unitList = unitRepo.findBySusNo(susNoWithoutCheckDigit);
			if (isFundBatch) {
				unitCode = unitList.get(0).getUnitCode();
				fkUnit = unitList.get(0).getId();
				taskValue = 0;
				taskValue = unitRepo.findTaskForUnitCode(unitList.get(0).getUnitCode());
				if (taskValue > 0 && taskValue != 991) {
					int adjustedTask = computeFundTaskValue(taskValue);
					task = adjustedTask;
					if (task > 0) {

						Integer maxBatch = null;

						if (taskBatchMap.containsKey(task)) {
							maxBatch = taskBatchMap.get(task);
						} else
							maxBatch = do2ControlRepo.findMaxBatchForFundTask(task, me);

						if (maxBatch != null) {
							batchNo = maxBatch + 1;
						} else
							batchNo = task * 1000 + 1;

						if (batchCountMap.containsKey(batchNo)) {
							batchCount = batchCountMap.get(batchNo);
						} else
							batchCountMap.put(batchNo, 0);
					}
				} else {

				}
			} else {
				if (unitList != null && unitList.size() > 0) {

					unitCode = unitList.get(0).getUnitCode();
					fkUnit = unitList.get(0).getId();
					if (unitList.get(0).getTask_1() != null)
						task = unitList.get(0).getTask_1();

					System.out.println("  susnowocd 1111111111111" + susNoWithoutCheckDigit + "  unitcode " + unitCode
							+ "  fkunit " + fkUnit + "  task " + task);
					if (task > 0) {

						Integer maxBatch = null;

						if (taskBatchMap.containsKey(task)) {
							maxBatch = taskBatchMap.get(task);
						} else
							maxBatch = do2ControlRepo.findMaxBatchForTask(task, me);

						if (maxBatch != null) {
							batchNo = maxBatch + 1;
						} else
							batchNo = task * 1000 + 1;

						if (batchCountMap.containsKey(batchNo)) {
							batchCount = batchCountMap.get(batchNo);
						} else
							batchCountMap.put(batchNo, 0);
					}
				}
			}

			list = doc.selectNodes(rootName + "PTO_No");
			ptoNo = ((Element) list.get(0)).getText();

			list = doc.selectNodes(rootName + "PTO_Date");
			ptoDate = ((Element) list.get(0)).getText();

			list = doc.selectNodes(rootName + "L_PTO_No");
			lastPtoNo = ((Element) list.get(0)).getText();

			list = doc.selectNodes(rootName + "L_PTO_Date");
			lastPtoDate = ((Element) list.get(0)).getText();

			String[] ptoStr = ptoNo.split("/");
			do2No = ptoStr[1];
			do2Type = ptoStr[0];

			list = doc.selectNodes(rootName + "Tot_Items");
			totalItems = ((Element) list.get(0)).getText();

			try {
				if (ptoDate != null && ptoDate.trim().length() == 10)
					do2Date = formatter.parse(ptoDate);

				if (do2Date != null) {
					do2DateStr = dbFormat.format(do2Date);
					do2Year = Integer.parseInt(do2DateStr.substring(0, 4));
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
			try {
				if (lastPtoDate != null && lastPtoDate.trim().length() == 10)
					lastDo2Date = formatter.parse(lastPtoDate);
			} catch (Exception e) {

			}

			Boolean isDuplicateDo2 = false, isRejected = false;
			if (isEnrollBatch) {
				List<EnrollDo2Xml> enrollDo2XmlList = enrollDo2XmlRepo.findBySusNoAndPtoNoAndPtoDate(susNo, ptoNo, ptoDate);
				System.out.println("duplicate====" + enrollDo2XmlList + "--" + susNo + "--" + ptoNo + "--" + f.getAbsolutePath()
						+ "--" + keyPath);
				if (enrollDo2XmlList != null && enrollDo2XmlList.size() > 0) {
					isDuplicateDo2 = true;
				}
			} else {
				List<Do2Xml> do2XmlList = do2XmlRepo.findBySusNoAndPtoNoAndPtoDate(susNo, ptoNo, ptoDate);
				System.out.println("duplicate====" + do2XmlList + "--" + susNo + "--" + ptoNo + "--" + f.getAbsolutePath()
						+ "--" + keyPath);
				if (do2XmlList != null && do2XmlList.size() > 0) {
					isDuplicateDo2 = true;
				}
			}
	
			String isXmlDs = VerifyXMLSignature.validateSignature(f.getAbsolutePath(), keyPath);
			if (isXmlDs != null && isXmlDs.startsWith("valid")) {
				keyVerified = "Y";
				summary.setDigitalSignatureStatus("Valid");
			} else {
				summary.setDigitalSignatureStatus("Invalid");
			}

			if (!xmlFileList.contains(f.getName()))
				xmlFileList.add(f.getName());

			List<HashMap<String, String>> hpItemList = readRows(f);
			for (HashMap<String, String> hm : hpItemList) {
				summary.setTotalXmlItemsUploaded(summary.getTotalXmlItemsUploaded() + 1);

				String itemNo = "", description = "", frDate = "", toDate = "", cola = "", colb = "", colc = "",
						attachment = "", remarks = "", certificate = "";
				String remarks1 = "", remarks2 = "", remarks3 = "", remarks4 = "", remarks5 = "", remarks6 = "",
						remarks7 = "", remarks8 = "", remarks9 = "", remarks10 = "";
				String fromDateStr = "", toDateStr = "", recordStatus = "U", reason = "";
				String armyNo = null, empName = "", rankName = "", trade = "", refPtoNo = "";
				Double amt = null;
				Integer sra = 0, nra = 0;
				String remarks11 = "", remarks12 = "", remarks13 = "", remarks14 = "", remarks15 = "", remarks16 = "",
						remarks17 = "", remarks18 = "", remarks19 = "", remarks20 = "";
				String remarks21 = "", remarks22 = "", remarks23 = "", remarks24 = "", remarks25 = "";
				String delayed = "", cfaSanctionReceived = "N", cfaSanctionLetterNo = "", cfaSanctionDate = "",
						obsnItems = "", approvedBy = "";
				String amountStr = "";
				Boolean duplicateItemNo = false, isAuditCage = false;
				isRejected = false;
				String checkDigit = "", punishment = "", fkemp = "", col9Remarks = "", attachEnc = "";
				StringBuffer strb = new StringBuffer();
				Boolean timeBarSanction = false;
				try {

					String tagValue = hm.get("Item_No");

					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						itemNo = (String) tagValue;

					}
					if (itemNo.length() > 3) {
						itemNo = itemNo.substring(1);
					}

					tagValue = hm.get("Pin");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						armyNo = ((String) tagValue);
						if (UploadHelper.isLastCharAlpha(armyNo)) {
							checkDigit = armyNo.substring(armyNo.length() - 1);
							armyNo = armyNo.substring(0, armyNo.length() - 1);
						}
					}
					itemCount++;

					if (armyNo != null) {
						List<Long> empList = empRepo.findEmployeeIdByArmyNo(armyNo);
						if (empList != null && empList.size() > 0) {
							fkEmployee = empList.get(0).longValue();
						}
					}

					if (fkEmployee > 0) {
						fkemp = fkEmployee + "";
					}

					System.out.println("--------------------do2 Type here------------:" + do2Type);
					List<Do2> d2List = do2Repo.findByUnitIdTrimDo2NoTrimItemNoYearAndStatus(fkUnit, do2No, itemNo,
							do2Year, do2Type);
					if (d2List != null && d2List.size() > 0) {
						// duplicate item no;
						duplicateItemNo = true;

					}
					if (isDuplicateDo2) {
						recordStatus = "D";
						reason = "Record with give do2 no, do2 date and sus no already exists.";
					}
					if (duplicateItemNo) {
						summary.setTotalDuplicateItems(summary.getTotalDuplicateItems() + 1);
					}

					if (keyVerified.equals("N")) {
						recordStatus = "R";
						reason = "Specimen Signature is Invalid";
						isRejected = true;
					}

					tagValue = hm.get("Rank");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						rankName = ((String) tagValue);
					}

					tagValue = hm.get("Trade");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						trade = ((String) tagValue);
					}
					tagValue = hm.get("Name");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						empName = ((String) tagValue);
					}

					tagValue = hm.get("Description");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						description = ((String) tagValue);

					}
					tagValue = hm.get("From_Date");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						frDate = (((String) tagValue).substring(6, 10) + "-" + ((String) tagValue).substring(3, 5) + "-"
								+ ((String) tagValue).substring(0, 2));
					}

					// xmlRecord.setTo_date(null);
					tagValue = hm.get("To_Date");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						toDate = (((String) tagValue).substring(6, 10) + "-" + ((String) tagValue).substring(3, 5) + "-"
								+ ((String) tagValue).substring(0, 2));
					}
					tagValue = hm.get("Amt");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						amt = Double.parseDouble(tagValue);
					}

					if (amt == null)
						amt = 0.0;

					if (amt > 0)
						amountStr = df.format(amt);

					if (ptoGroup.startsWith("IB")) {
						tagValue = hm.get("Months");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							sra = Integer.parseInt(tagValue);
						}

						tagValue = hm.get("Days");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							nra = Integer.parseInt(tagValue);
						}

						tagValue = hm.get("OCCM");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							punishment = ((String) tagValue.trim());
						}
					} else {
						tagValue = hm.get("SRA");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							if (tagValue.endsWith(".0")) {
								sra = Integer.parseInt(tagValue.substring(0, tagValue.indexOf(".0")));
							} else {
								sra = Integer.parseInt(tagValue);
							}
						}
						tagValue = hm.get("NRA");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							if (tagValue.endsWith(".0")) {
								nra = Integer.parseInt(tagValue.substring(0, tagValue.indexOf(".0")));
							} else {
								nra = Integer.parseInt(tagValue);
							}
						}
					}

					tagValue = hm.get("Other_Allow1");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						cola = ((String) tagValue);

						if (description.startsWith("CANCEL")) {
							if (auditCageMap.containsKey(cola))
								isAuditCage = true;
						} else if (auditCageMap.containsKey(description))
							isAuditCage = true;
					}
					tagValue = hm.get("Other_Allow2");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						colb = ((String) tagValue);
					}
					tagValue = hm.get("Other_Allow3");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						colc = ((String) tagValue);
					}

					cola = cola.replaceAll("\n", " ");
					colb = colb.replaceAll("\n", " ");
					colc = colc.replaceAll("\n", " ");

					if (cola.length() > 50)
						cola = cola.substring(0, 49);
					if (colb.length() > 50)
						colb = colb.substring(0, 49);
					if (colc.length() > 100)
						colc = colc.substring(0, 99);

					tagValue = hm.get("Ref_PTO_No");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						refPtoNo = ((String) tagValue);
						if (refPtoNo != null && refPtoNo.length() > 15) {
							recordStatus = "R";
							reason = "Length of Ref PTO Number exceeds 15 " + refPtoNo;
							isRejected = true;
							refPtoNo = refPtoNo.substring(0, 14);
						}
					}

					tagValue = hm.get("PTO_Remarks");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks = ((String) tagValue);
						remarks = remarks.replaceAll("\n", " ");
					}

					tagValue = hm.get("Remarks1");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks1 = ((String) tagValue);
						remarks1 = remarks1.replaceAll("\n", " ");
						if (remarks1.length() > 0) {
							strb.append("Remarks1: " + remarks1);
							strb.append("~");
						}
					}
					tagValue = hm.get("Remarks2");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks2 = ((String) tagValue);
						remarks2 = remarks2.replaceAll("\n", " ");
						if (remarks2.length() > 0) {
							strb.append("Remarks2: " + remarks2);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks3");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks3 = ((String) tagValue);
						remarks3 = remarks3.replaceAll("\n", " ");
						if (remarks3.length() > 0) {
							strb.append("Remarks3: " + remarks3);
							strb.append("~");
						}						
					}
					tagValue = hm.get("Remarks4");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks4 = ((String) tagValue);
						remarks4 = remarks4.replaceAll("\n", " ");
						if (remarks4.length() > 0) {
							strb.append("Remarks4: " + remarks4);
							strb.append("~");
						}
					}
					tagValue = hm.get("Remarks5");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks5 = ((String) tagValue);
						remarks5 = remarks5.replaceAll("\n", " ");
						if (remarks5.length() > 0) {
							strb.append("Remarks5: " + remarks5);
							strb.append("~");
						}
					}
					tagValue = hm.get("Remarks6");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks6 = ((String) tagValue);
						remarks6 = remarks6.replaceAll("\n", " ");
						if (remarks6.length() > 0) {
							strb.append("Remarks6: " + remarks6);
							strb.append("~");
						}
					}
					tagValue = hm.get("Remarks7");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks7 = ((String) tagValue);
						remarks7 = remarks7.replaceAll("\n", " ");
						if (remarks7.length() > 0) {
							strb.append("Remarks7: " + remarks7);
							strb.append("~");
						}
					}
					tagValue = hm.get("Remarks8");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks8 = ((String) tagValue);
						remarks8 = remarks8.replaceAll("\n", " ");
						if (remarks8.length() > 0) {
							strb.append("Remarks8: " + remarks8);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks9");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks9 = ((String) tagValue);
						remarks9 = remarks9.replaceAll("\n", " ");
						if (remarks9.length() > 0) {
							strb.append("Remarks9: " + remarks9);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks10");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks10 = ((String) tagValue);
						remarks10 = remarks10.replaceAll("\n", " ");
						if (remarks10.length() > 0) {
							strb.append("Remarks10: " + remarks10);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks11");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks11 = ((String) tagValue);
						remarks11 = remarks11.replaceAll("\n", " ");
						if (remarks11.length() > 0) {
							strb.append("Remarks11: " + remarks11);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks12");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks12 = ((String) tagValue);
						remarks12 = remarks12.replaceAll("\n", " ");
						if (remarks12.length() > 0) {
							strb.append("Remarks12: " + remarks12);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks13");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks13 = ((String) tagValue);
						remarks13 = remarks13.replaceAll("\n", " ");
						if (remarks13.length() > 0) {
							strb.append("Remarks13: " + remarks13);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks14");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks14 = ((String) tagValue);
						remarks14 = remarks14.replaceAll("\n", " ");
						if (remarks14.length() > 0) {
							strb.append("Remarks14: " + remarks14);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks15");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks15 = ((String) tagValue);
						remarks15 = remarks15.replaceAll("\n", " ");
						if (remarks15.length() > 0) {
							strb.append("Remarks15: " + remarks15);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks16");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks16 = ((String) tagValue);
						remarks16 = remarks16.replaceAll("\n", " ");
						if (remarks16.length() > 0) {
							strb.append("Remarks16: " + remarks16);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks17");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks17 = ((String) tagValue);
						remarks17 = remarks17.replaceAll("\n", " ");
						if (remarks17.length() > 0) {
							strb.append("Remarks17: " + remarks17);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks18");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks18 = ((String) tagValue);
						remarks18 = remarks18.replaceAll("\n", " ");
						if (remarks18.length() > 0) {
							strb.append("Remarks18: " + remarks18);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks19");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks19 = ((String) tagValue);
						remarks19 = remarks19.replaceAll("\n", " ");
						if (remarks19.length() > 0) {
							strb.append("Remarks19: " + remarks19);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks20");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks20 = ((String) tagValue);
						remarks20 = remarks20.replaceAll("\n", " ");
						if (remarks20.length() > 0) {
							strb.append("Remarks20: " + remarks20);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks21");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks21 = ((String) tagValue);
						remarks21 = remarks21.replaceAll("\n", " ");
						if (remarks21.length() > 0) {
							strb.append("Remarks21: " + remarks21);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks22");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks22 = ((String) tagValue);
						remarks22 = remarks22.replaceAll("\n", " ");
						if (remarks22.length() > 0) {
							strb.append("Remarks22: " + remarks22);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks23");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks23 = ((String) tagValue);
						remarks23 = remarks23.replaceAll("\n", " ");
						if (remarks23.length() > 0) {
							strb.append("Remarks23: " + remarks23);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks24");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks24 = ((String) tagValue);
						remarks24 = remarks24.replaceAll("\n", " ");
						if (remarks24.length() > 0) {
							strb.append("Remarks24: " + remarks24);
							strb.append("~");
						}
					}

					tagValue = hm.get("Remarks25");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						remarks25 = ((String) tagValue);
						remarks25 = remarks25.replaceAll("\n", " ");
						if (remarks25.length() > 0) {
							strb.append("Remarks25: " + remarks25);
							strb.append("~");
						}
					}
					col9Remarks = strb.toString();
					if (col9Remarks.contains("~")) {
						col9Remarks = strb.toString().replaceAll("~", " ");
					}
					
					
					tagValue = hm.get("Delayed");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						delayed = ((String) tagValue);
					}

					tagValue = hm.get("CFA_sanc_received");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						cfaSanctionReceived = ((String) tagValue);
					}

					tagValue = hm.get("CFA_sanc_letter_no");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						cfaSanctionLetterNo = ((String) tagValue);
					}
					tagValue = hm.get("CFA_sanc_dt");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						cfaSanctionDate = ((String) tagValue);
					}
					if ("Y".equals(delayed) 
					        && "Y".equals(cfaSanctionReceived) 
					        && cfaSanctionLetterNo != null && cfaSanctionLetterNo.trim().length() > 5 
					        && cfaSanctionDate != null && cfaSanctionDate.trim().length() > 6) {
					    timeBarSanction = true;
					}									
					tagValue = hm.get("Obsn_item");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						obsnItems = ((String) tagValue);
					}

 					tagValue = hm.get("Attachment");
					if (tagValue != null) {
						attachment = ((String) tagValue).trim();
						if (!attachment.isEmpty()) {
							// Remove the entire /PART2/ portion
							attachment = attachment.replace("/PART2/", "");
							attachEnc = Utils.getEncrypt(attachment);
						}
					}			 

					tagValue = hm.get("Approved_by");
					if (tagValue != null && ((String) tagValue).trim().length() > 0) {
						approvedBy = ((String) tagValue);
					} else {
						recordStatus = "R";// Rejected since Approved tag is null
						reason = "Record Rejected Since Approved tag is null.";
						isRejected = true;
					}

					// Enroll Do2 begins

					if (isEnrollBatch) {
						String  soldierCategory = "", entryTrade = "", toeColourService = "",
								religion = "", civilEdu = "", foodBill = "", bankAcno = "", ifsCode = "", panNo = "",
								aadharNo = "", emailId = "", arrivalTime = "", departureTime = "", gender = "",
								gsoCode = "", ccaCode = "", taAppointmentCategory = "", sigVerified = "",
								  svcType = "", mobileNo = "", gratuity_surrendered_dsc = "N", termsService="0", termsAge="0";
						Double commutationAmount = 0.0;
						Integer advanceAmount = 0;
						Boolean commutationWillingness = false, gratuity_willingness = false;
						Date last_do2_date = null, enrollDate = null, birthDate = null, dateOfArrival = null,
								dateOfDeparture = null, uploadDate = null;
						
						tagValue = hm.get("Category");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							soldierCategory = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Category tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("Trade");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							entryTrade = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Trade tag is null.";
							isRejected = true;
						}
						tagValue = hm.get("Enrol_Dt");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							String enrollDateStr = null;
							enrollDateStr = (((String) tagValue).substring(6, 10) + "-" + ((String) tagValue).substring(3, 5) + "-"
									+ ((String) tagValue).substring(0, 2));
							enrollDate = DateManager.getDate(enrollDateStr);							 
						} else {
							recordStatus = "R"; // Rejected since Enrollment Date tag is null
							reason = "Record Rejected Since Enrol_Dt tag is null.";
							isRejected = true;
						}
						tagValue = hm.get("TOE_Colour_Service");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							toeColourService= ((String) tagValue);	
							String[] parts = toeColourService.trim().split("/");
							termsService = parts[0];  
							termsAge = parts[1];  
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since TOE_Colour_Service tag is null.";
							isRejected = true;
						}
						tagValue = hm.get("DOB");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							String birthDateStr = null;
							birthDateStr = (((String) tagValue).substring(6, 10) + "-" + ((String) tagValue).substring(3, 5) + "-"
									+ ((String) tagValue).substring(0, 2));
							birthDate = DateManager.getDate(birthDateStr);							 
						} else {
							recordStatus = "R"; // Rejected since Enrollment Date tag is null
							reason = "Record Rejected Since Enrol_Dt tag is null.";
							isRejected = true;
						}
						tagValue = hm.get("Religion_For_PAO");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							religion= ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Religion_For_PAO tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("Civil_Edu");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							civilEdu = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Civil_Edu tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("Food_Bill");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							foodBill = ((String) tagValue);							 
						} 
						
						tagValue = hm.get("Bank_ACNO");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							bankAcno = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Bank_ACNO tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("IFSC_Code");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							ifsCode = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since IFSC_Code tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("PAN_No");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							panNo = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since PAN_No tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("Aadhaar_No");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							aadharNo = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Aadhaar_No tag is null.";
							isRejected = true;
						}
						
						tagValue = hm.get("Email_Id");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							emailId = ((String) tagValue);							 
						} else {
							recordStatus = "R"; // Rejected since soldier category tag is null
							reason = "Record Rejected Since Email_Id tag is null.";
							isRejected = true;
						}
						tagValue = hm.get("SVC_TYPE");
						if (tagValue != null && ((String) tagValue).trim().length() > 0) {
							svcType = ((String) tagValue);							 
						} 
						tagValue = hm.get("Remarks3");
						if (remarks3!=null && remarks3.trim().length()==10 && Utils.isNumeric(remarks3.trim())) {
							mobileNo = (String)remarks3.trim();
						}
						tagValue = hm.get("Remarks4");
						if (remarks4==null && remarks4.trim().length()==1) {
							gender = ((String) remarks4.trim());
						}
						if (gender==null || (gender!=null && gender.trim().length()==0)) {
							gender="M";
						}
						if (gsoCode==null || (gsoCode!=null && gsoCode.trim().length()==0)) {
							if (religion!=null && religion.trim().length()>0) {
								if (religion.trim().toLowerCase().contains("sikh")) {
									gsoCode="S";
								} else if (religion.trim().toLowerCase().contains("gurkha")
									|| religion.trim().toLowerCase().contains("gorkha")) {
									gsoCode="G";
								} else {
									gsoCode="O";
								}
							}
						}
						if (ccaCode==null || (ccaCode!=null && ccaCode.trim().length()==0)) {
							ccaCode="CCOTH";
						}
						if (remarks1!=null && remarks1.length()==18) {
							dateOfArrival = DateManager.getDate(((String) remarks1).substring(4, 8) + "-" + ((String) remarks1).substring(2, 4) + "-"
									+ ((String) remarks1).substring(0, 2));
							arrivalTime = remarks1.substring(10,14);
						}
						if (remarks2!=null && remarks2.length()==18) {
							dateOfDeparture = DateManager.getDate(((String) remarks2).substring(4, 8) + "-" + ((String) remarks2).substring(2, 4) + "-"
									+ ((String) remarks2).substring(0, 2));
							departureTime = remarks2.substring(10, 14);
						}						
			 			enrollDo2XmlWriter.write(ptoGroup + "~" + susNo + "~" + unitCode + "~" + ptoNo + "~" + ptoDate
								+ "~" + lastPtoNo + "~" + lastPtoDate + "~" + do2No + "~" + do2DateStr + "~"
								+ totalItems + "~" + itemNo + "~" + svcType + "~" + armyNo + "~" + checkDigit + "~" 
								+ rankName + "~" + empName + "~"  
								+ soldierCategory + "~" + entryTrade + "~" + toeColourService + "~" + birthDate + "~" + religion + "~"  
								+ civilEdu + "~" + foodBill + "~" + bankAcno + "~" + ifsCode + "~" + panNo + "~" + aadharNo + "~"
								+ emailId + "~" + mobileNo + "~" + gender + "~" + gsoCode + "~" + ccaCode  + "~" + description 
								+ "~" + enrollDate + "~" + amountStr  + "~" + remarks + "~" + remarks1 + "~" + remarks2 + "~" + remarks3 + "~" + remarks4
								+ "~" + remarks5 + "~" + remarks6 + "~" + remarks7 + "~" + remarks8 + "~" + remarks9
								+ "~" + remarks10 + "~" + remarks11 + "~" + remarks12 + "~" + remarks13 + "~"
								+ remarks14 + "~" + remarks15 + "~" + remarks16 + "~" + remarks17 + "~" + remarks18
								+ "~" + remarks19 + "~" + remarks20 + "~" + remarks21 + "~" + remarks22 + "~"
								+ remarks23 + "~" + remarks24 + "~" + remarks25 + "~" + attachment + "~" + delayed + "~"
								+ cfaSanctionReceived + "~" + cfaSanctionLetterNo + "~" + cfaSanctionDate + "~"
								+ keyVerified + "~" + approvedBy + "~" + recordStatus + "~" + DateManager.getCurrentTimestamp()
								+ "~"  + zipFileName + "~" + f.getName() + "~" + Utils.getLoggedInUsr().getId()+ "~"+ ip + "~" + mac + "~" + reason + "~" + me + "~" + do2Type );
						enrollDo2XmlWriter.newLine();
			/*			enrollDo2Writer.write(batchNo + "~" + ptoGroup + "~" + do2Type + "~" + unitCode + "~" + do2No + "~" + itemNo + "~" + do2Date 
								+ "~" + armyNo + "~" + checkDigit + "~" + empName + "~" + soldierCategory + "~" + entryTrade + "~" +
								toeColourService.substring(0,2) + "~" + toeColourService.substring(2) + "~" + birthDate + "~" +
								civilEdu  + "~" + 0  + "~" + bankAcno + "~" + ifsCode  + "~" + panNo + "~" + dateOfArrival + "~" +  arrivalTime
								+ "~" + dateOfDeparture + "~" + departureTime + "~" + ccaCode + "~" + advanceAmount + "~" + gsoCode
								+ "~" + remarks + "~" + "P" + "~" + reason + "~" + 0 + "~" + 0 + "~" + 0 + "~" +
								"D" + "~" + Utils.getCurrentQuarter() + "~" + Utils.getLoggedInUsr().getId() + "~" + DateManager.getCurrentTimestamp()
								+ "~" + taAppointmentCategory + "~" + false
								+ "~" + false + "~" + 0 + "~" + false + "~" + mobileNo + "~" + emailId
								+ "~" + aadharNo + "~" + gender+ "~" + false);
	*/
						enrollDo2Writer.write(batchNo + "~" + ptoGroup + "~" + do2Type + "~" + unitCode + "~" + do2No + "~" + itemNo + "~" + do2Date 
								+ "~" + armyNo + "~" + checkDigit + "~" + empName + "~" + soldierCategory + "~" + entryTrade + "~" +
								termsService + "~" + termsAge + "~" + birthDate + "~" + dateOfArrival
								+ "~" + ccaCode  + "~" + advanceAmount  + "~" + gsoCode + "~" + remarks + "~" + "P" + "~" 
								+ "To Be Cleared Manually" + "~" + 0 + "~" + 0  + "~" + 0 + "~" + "D" + "~" + Utils.getCurrentQuarter() + "~" + 
								Utils.getLoggedInUsr().getId() + "~" + DateManager.getCurrentTimestamp() + "~" + "O" + "~" + false + "~" + false 
								+ "~" + "0" + "~" + gratuity_surrendered_dsc + "~" + mobileNo + "~" + emailId + "~" + aadharNo + "~" + civilEdu + "~" + bankAcno
								+ "~" + ifsCode  + "~" + panNo + "~" +  dateOfDeparture + "~" +  arrivalTime + "~" + departureTime  + "~" +  
								gender + "~" + false);
						
						enrollDo2Writer.newLine();
					}
					// Enroll Do2 ends

					if (fkUnit == 0) {
						isRejected = true;
						recordStatus = "R";
						reason = "Unit with the given susNo " + susNo + " not found in unit master.";
					}
					// System.out.println("authority "+authority+"--"+ssInFile);
					// write to do2xml
					if (do2Type == null || (do2Type != null && do2Type.trim().length() == 0)) {
						do2Type = "0";
					}

					if (!duplicateItemNo) {
						do2XmlWriter.write(ptoGroup + "~" + susNo + "~" + unitCode + "~" + ptoNo + "~" + ptoDate + "~"
								+ lastPtoNo + "~" + lastPtoDate + "~" + do2No + "~" + do2DateStr + "~" + totalItems
								+ "~" + armyNo + "~" + checkDigit + "~" + rankName + "~" + trade + "~" + empName + "~"
								+ itemNo + "~" + description + "~" + frDate + "~" + toDate + "~" + cola + "~" + colb
								+ "~" + colc + "~" + refPtoNo + "~" + amountStr + "~" + sra + "~" + nra + "~" + remarks
								+ "~" + remarks1 + "~" + remarks2 + "~" + remarks3 + "~" + remarks4 + "~" + remarks5
								+ "~" + remarks6 + "~" + remarks7 + "~" + remarks8 + "~" + remarks9 + "~" + remarks10
								+ "~" + remarks11 + "~" + remarks12 + "~" + remarks13 + "~" + remarks14 + "~"
								+ remarks15 + "~" + remarks16 + "~" + remarks17 + "~" + remarks18 + "~" + remarks19
								+ "~" + remarks20 + "~" + remarks21 + "~" + remarks22 + "~" + remarks23 + "~"
								+ remarks24 + "~" + remarks25 + "~" + attachment + "~" + delayed + "~"
								+ cfaSanctionReceived + "~" + cfaSanctionLetterNo + "~" + cfaSanctionDate + "~"
								+ keyVerified + "~" + approvedBy + "~" + recordStatus + "~" + DateManager.getToday()
								+ "~" + f.getName() + "~" + zipFileName + "~" + fkUsr + "~" + ip + "~" + mac + "~"
								+ DateManager.getCurrentTimestamp() + "~" + reason + "~" + me + "~" + do2Type);
						do2XmlWriter.newLine();
					} else {
						do2XmlWriter.write(ptoGroup + "~" + susNo + "~" + unitCode + "~" + ptoNo + "~" + ptoDate + "~"
								+ lastPtoNo + "~" + lastPtoDate + "~" + do2No + "~" + do2DateStr + "~" + totalItems
								+ "~" + armyNo + "~" + checkDigit + "~" + rankName + "~" + trade + "~" + empName + "~"
								+ itemNo + "~" + description + "~" + frDate + "~" + toDate + "~" + cola + "~" + colb
								+ "~" + colc + "~" + refPtoNo + "~" + amountStr + "~" + sra + "~" + nra + "~" + remarks
								+ "~" + remarks1 + "~" + remarks2 + "~" + remarks3 + "~" + remarks4 + "~" + remarks5
								+ "~" + remarks6 + "~" + remarks7 + "~" + remarks8 + "~" + remarks9 + "~" + remarks10
								+ "~" + remarks11 + "~" + remarks12 + "~" + remarks13 + "~" + remarks14 + "~"
								+ remarks15 + "~" + remarks16 + "~" + remarks17 + "~" + remarks18 + "~" + remarks19
								+ "~" + remarks20 + "~" + remarks21 + "~" + remarks22 + "~" + remarks23 + "~"
								+ remarks24 + "~" + remarks25 + "~" + attachment + "~" + delayed + "~"
								+ cfaSanctionReceived + "~" + cfaSanctionLetterNo + "~" + cfaSanctionDate + "~"
								+ keyVerified + "~" + approvedBy + "~" + "D" + "~" + DateManager.getToday() + "~"
								+ f.getName() + "~" + zipFileName + "~" + fkUsr + "~" + ip + "~" + mac + "~"
								+ DateManager.getCurrentTimestamp() + "~" + "Record with given do2No " + do2No + "/"
								+ itemNo + " for the year " + do2Year + " for this unit already uploaded." + "~" + me
								+ "~" + do2Type);
						do2XmlWriter.newLine();
					}
					System.out.println("duplicate====" + duplicateItemNo + "--" + recordStatus + "--" + reason + "---"
							+ fkUnit + "---" + isFundBatch + "---" + batchNo);
					if (!duplicateItemNo & !isRejected) {
						summary.setTotalValidItems(summary.getTotalValidItems() + 1);
						if (fkUnit > 0 && !isFundBatch) {

							do2Writer.write(batchNo + "~" + unitCode + "~" + fkUnit + "~" + armyNo + "~" + checkDigit
									+ "~" + fkemp + "~" + ptoGroup + "~" + do2Type + "~" + do2No + "~" + itemNo + "~"
									+ do2DateStr + "~" + punishment + "~" + description + "~" + frDate + "~" + toDate
									+ "~" + amt + "~" + sra + "~" + nra + "~" + cola + "~" + colb + "~" + colc + "~"
									+ refPtoNo + "~" + remarks + "~" + "P" + "~"
									+ "Batch to be cleared after verification" + "~" + "D" + "~" + me + "~"
									+ isAuditCage + "~" + timeBarSanction + "~" + "false" + "~" + me + "~" + delayed + "~"
									+ cfaSanctionReceived + "~" + cfaSanctionLetterNo + "~" + cfaSanctionDate + "~"
									+ attachEnc + "~" + col9Remarks);
							do2Writer.newLine();
						} else if (fkUnit > 0 && isFundBatch) {
							do2Writer.write(batchNo + "~" + unitCode + "~" + fkUnit + "~" + armyNo + "~" + checkDigit
									+ "~" + fkemp + "~" + ptoGroup + "~" + do2Type + "~" + do2No + "~" + itemNo + "~"
									+ do2DateStr + "~" + punishment + "~" + description + "~" + frDate + "~" + toDate
									+ "~" + amt + "~" + sra + "~" + nra + "~" + cola + "~" + colb + "~" + colc + "~"
									+ refPtoNo + "~" + remarks + "~" + "P" + "~" + "Item to be cleared by Fund Cell"
									+ "~" + "D" + "~" + me + "~" + isAuditCage + "~" + timeBarSanction + "~" + "false" + "~"
									+ me + "~" + delayed + "~" + cfaSanctionReceived + "~" + cfaSanctionLetterNo + "~"
									+ cfaSanctionDate + "~" + attachEnc + "~" + col9Remarks);
							do2Writer.newLine();
						}
					}
				} catch (Exception e) {
					Utils.handleException(e);
				}
			}

			if (batchCount + itemCount > BATCH_SIZE) {
				taskBatchMap.remove(task);
				batchNo++;
				taskBatchMap.put(task, batchNo);
				batchCountMap.put(batchNo, itemCount);
			}

			if (f.getName() != null && f.getName().startsWith("ID")) {
				do2Type = "F"; // Fund
			} else if (f.getName() != null && f.getName().startsWith("IC")) {
				do2Type = "E";
			} else {
				do2Type = "N";
			}

			if (!isFundBatch) {
				do2cWriter.write(unitCode + "~" + do2No + "~" + totalItems + "~" + me + "~" + DateManager.getToday()
						+ "~" + batchNo + "~" + batchDateStr + "~" + DateManager.getCurrentTimestamp() + "~" + do2Year
						+ "~" + task + "~" + true + "~" + f.getName() + "~" + do2DateStr + "~" + do2Type + "~" + "S"
						+ "~" + "Do2_" + batchNo + "_" + me.substring(3) + "" + me.substring(0, 2) + "~" + false);
				do2cWriter.newLine();
			} else if (isFundBatch) {
				do2cWriter.write(unitCode + "~" + do2No + "~" + totalItems + "~" + me + "~" + DateManager.getToday()
						+ "~" + batchNo + "~" + batchDateStr + "~" + DateManager.getCurrentTimestamp() + "~" + do2Year
						+ "~" + task + "~" + true + "~" + f.getName() + "~" + do2DateStr + "~" + do2Type + "~" + "S"
						+ "~" + "Fund_" + batchNo + "_" + me.substring(3) + "" + me.substring(0, 2) + "~" + true);
				do2cWriter.newLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Added--10122025
	private boolean isFundBatch(Document doc) {
		try {
			Node descNode = doc.selectSingleNode("//Header/Description");
			if (descNode == null)
				return false;
			String desc = descNode.getText().trim();
			return "PFWDL".equalsIgnoreCase(desc);
		} catch (Exception e) {
			return false;
		}
	}

	// Added--10122025
	private int computeFundTaskValue(int taskValue) {

		if (taskValue <= 0 || taskValue == 991)
			return taskValue;

		List<Section> sectionList = sectionRepo.findBySecName("fund cell");

		if (sectionList == null || sectionList.size() != 1) {
			System.out.println("Alert! Fund Cell Section Not Found");
			return taskValue;
		}

		int sectionId = sectionList.get(0).getId();

		if (sectionId > 0 && sectionId < 10) {
			if (taskValue > 10 && taskValue < 100) {
				taskValue = (sectionId * 100) + taskValue;
			} else if (taskValue > 99 && taskValue < 1000) {
				taskValue = (sectionId * 1000) + taskValue;
			} else {
				System.out.println(" Alert !!! Invalid TASK value for Ledger Groups in the PAO.");
			}
		} else {
			System.out.println(" Alert !!! Invalid section id value for Fund Cell Section in the PAO.");
		}

		System.out.println("Invalid Task value for Ledger groups");
		return taskValue;
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
			String rootName = rootElement.getNodeName() + "//HRMS2.5//PTO_Details//";
			// org.w3c.dom.NodeList nodes = doc.getElementsByTagName("Row");
			org.w3c.dom.NodeList nodesUpper = doc.getElementsByTagName("Row");
			org.w3c.dom.NodeList nodesLower = doc.getElementsByTagName("row");
			List<org.w3c.dom.NodeList> allNodeLists = Arrays.asList(nodesUpper, nodesLower);

			HashMap<String, String> hpItem = null;
			for (org.w3c.dom.NodeList nodes : allNodeLists) {
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

	private Connection getConnection() {

		String url = "jdbc:postgresql://localhost:5432/dolphin";
		String driver = "org.postgresql.Driver";
		String user = env.getProperty("spring.datasource.username");
		String password = env.getProperty("spring.datasource.password");
		Connection con = null;

		try {
			Class.forName(driver).newInstance();
			con = DriverManager.getConnection(url, user, password);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return con;
	}


 
 
public class ArpUtil {

    public static String getMacAddress(String ipAddress) {

        try {
            Process process = Runtime.getRuntime().exec("arp -a");

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

            String line;

            Pattern pattern = Pattern.compile(
                    "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+([0-9a-fA-F\\-]{17})");

            while ((line = reader.readLine()) != null) {

                Matcher matcher = pattern.matcher(line);
System.out.println(".............matcher find " +matcher.find());
                if (matcher.find()) {

                    String ip = matcher.group(1);
                    String mac = matcher.group(2);
System.out.println("...........ip "+ip+"   mac  "+mac);
                    if (ip.equals(ipAddress)) {
                        return mac.toUpperCase();
                    }
                }
            }
            System.out.println("..............returning null from mac ..............process..... " +process.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("..............returning null from mac .............. " );

        return null;
    }
	}
}