package itsdc.dolphin.do2;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays; 
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;  

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import itsdc.dolphin.controllers.Do2Controller;
import itsdc.dolphin.do2.auditcage.ArrearsObject;
import itsdc.dolphin.do2.auditcage.AuditCageProcessor;
import itsdc.dolphin.do2.auditcage.DueDrawnElement;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.reports.ReportUtils;
import itsdc.dolphin.repositories.AdjustmentCbRepository;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.AppendixJRepository;
import itsdc.dolphin.repositories.AppointmentRateRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageAllowanceRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.BhutanSlabRepository;
import itsdc.dolphin.repositories.ClassPayRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeEarningRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EmployeeServiceDetailRepository;
import itsdc.dolphin.repositories.FamilyDetailRepository;
import itsdc.dolphin.repositories.Form16Repository;
import itsdc.dolphin.repositories.FundSummaryRepository;
import itsdc.dolphin.repositories.GroupCategoryRepository;
import itsdc.dolphin.repositories.LpcDeputationRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.PayCodeRepository;
import itsdc.dolphin.repositories.PayMatrixRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.RankRepository;
import itsdc.dolphin.repositories.ReadyReckonerRepository;
import itsdc.dolphin.repositories.RhLocationRepository;
import itsdc.dolphin.repositories.StatusCodeRepository;
import itsdc.dolphin.repositories.TransferInOutRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Arrearcorr implements java.io.Serializable {
//	@In(required=false)
	
	@Autowired
	Do2Controller d2c;	
	
	public Arrearcorr() {		
	}
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

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public AuditCageAllowanceRepository getAcaRepo() {
		return acaRepo;
	}

	public void setAcaRepo(AuditCageAllowanceRepository acaRepo) {
		this.acaRepo = acaRepo;
	}

	public EmployeeDeductionRepository getEdRepo() {
		return edRepo;
	}

	public void setEdRepo(EmployeeDeductionRepository edRepo) {
		this.edRepo = edRepo;
	}

	public ArrearsRepository getArrRepo() {
		return arrRepo;
	}

	public void setArrRepo(ArrearsRepository arrRepo) {
		this.arrRepo = arrRepo;
	}

	public PayCodeRepository getPcRepo() {
		return pcRepo;
	}

	public void setPcRepo(PayCodeRepository pcRepo) {
		this.pcRepo = pcRepo;
	}

	public LpcRegisterRepository getLpcRepo() {
		return lpcRepo;
	}

	public void setLpcRepo(LpcRegisterRepository lpcRepo) {
		this.lpcRepo = lpcRepo;
	}
	
	PayScaleRepository psRepo;
	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
	}

	public EmployeeServiceDetailRepository getEsdRepo() {
		return esdRepo;
	}

	public void setEsdRepo(EmployeeServiceDetailRepository esdRepo) {
		this.esdRepo = esdRepo;
	}

	public RankRepository getRankRepo() {
		return rankRepo;
	}

	public void setRankRepo(RankRepository rankRepo) {
		this.rankRepo = rankRepo;
	}

	public GroupCategoryRepository getGcRepo() {
		return gcRepo;
	}

	public void setGcRepo(GroupCategoryRepository gcRepo) {
		this.gcRepo = gcRepo;
	}

	public PayMatrixRepository getMatrixRepo() {
		return matrixRepo;
	}

	public void setMatrixRepo(PayMatrixRepository matrixRepo) {
		this.matrixRepo = matrixRepo;
	}

	public ClassPayRateRepository getClassPayRepo() {
		return classPayRepo;
	}

	public void setClassPayRepo(ClassPayRateRepository classPayRepo) {
		this.classPayRepo = classPayRepo;
	}

	public AppointmentRateRepository getApptRateRepo() {
		return apptRateRepo;
	}

	public void setApptRateRepo(AppointmentRateRepository apptRateRepo) {
		this.apptRateRepo = apptRateRepo;
	}

	public AllowanceRateRepository getAlwRateRepo() {
		return alwRateRepo;
	}

	public void setAlwRateRepo(AllowanceRateRepository alwRateRepo) {
		this.alwRateRepo = alwRateRepo;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public StatusCodeRepository getScRepo() {
		return scRepo;
	}

	public void setScRepo(StatusCodeRepository scRepo) {
		this.scRepo = scRepo;
	}

	public UnitRepository getUnitRepo() {
		return unitRepo;
	}

	public void setUnitRepo(UnitRepository unitRepo) {
		this.unitRepo = unitRepo;
	}

	public ReadyReckonerRepository getRrRepo() {
		return rrRepo;
	}

	public void setRrRepo(ReadyReckonerRepository rrRepo) {
		this.rrRepo = rrRepo;
	}

	public LpcDeputationRepository getLpcDepRepo() {
		return lpcDepRepo;
	}

	public void setLpcDepRepo(LpcDeputationRepository lpcDepRepo) {
		this.lpcDepRepo = lpcDepRepo;
	}

	public BhutanSlabRepository getBhutanSlabRepo() {
		return bhutanSlabRepo;
	}

	public void setBhutanSlabRepo(BhutanSlabRepository bhutanSlabRepo) {
		this.bhutanSlabRepo = bhutanSlabRepo;
	}

	public FundSummaryRepository getFsRepo() {
		return fsRepo;
	}

	public void setFsRepo(FundSummaryRepository fsRepo) {
		this.fsRepo = fsRepo;
	}

	public EmployeeEarningRepository getEeRepo() {
		return eeRepo;
	}

	public void setEeRepo(EmployeeEarningRepository eeRepo) {
		this.eeRepo = eeRepo;
	}

	public Form16Repository getF16Repo() {
		return f16Repo;
	}

	public void setF16Repo(Form16Repository f16Repo) {
		this.f16Repo = f16Repo;
	}

	public AdjustmentCbRepository getAdjCbRepo() {
		return adjCbRepo;
	}

	public void setAdjCbRepo(AdjustmentCbRepository adjCbRepo) {
		this.adjCbRepo = adjCbRepo;
	}

	public FamilyDetailRepository getFdRepo() {
		return fdRepo;
	}

	public void setFdRepo(FamilyDetailRepository fdRepo) {
		this.fdRepo = fdRepo;
	}

	public TransferInOutRepository getTioRepo() {
		return tioRepo;
	}

	public void setTioRepo(TransferInOutRepository tioRepo) {
		this.tioRepo = tioRepo;
	}

	public RhLocationRepository getRhLocationRepo() {
		return rhLocationRepo;
	}

	public void setRhLocationRepo(RhLocationRepository rhLocationRepo) {
		this.rhLocationRepo = rhLocationRepo;
	}
	
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	AppendixJRepository appjRepo;
	Do2Repository do2Repo;
	AuditCageRepository acRepo;
	AuditCageAllowanceRepository acaRepo;
	EmployeeDeductionRepository edRepo;
	ArrearsRepository arrRepo;
	PayCodeRepository pcRepo;
	LpcRegisterRepository lpcRepo; 	
	EmployeeServiceDetailRepository esdRepo;
	RankRepository rankRepo;
	GroupCategoryRepository gcRepo;
	PayMatrixRepository matrixRepo;
	ClassPayRateRepository classPayRepo;
	AppointmentRateRepository apptRateRepo;
	AllowanceRateRepository alwRateRepo;
	EmployeeRepository empRepo;
	StatusCodeRepository scRepo;
	UnitRepository unitRepo;
	ReadyReckonerRepository rrRepo;
	LpcDeputationRepository lpcDepRepo;
	BhutanSlabRepository bhutanSlabRepo;
	FundSummaryRepository fsRepo;
	EmployeeEarningRepository eeRepo;
	Form16Repository f16Repo;
	AdjustmentCbRepository adjCbRepo;
	FamilyDetailRepository fdRepo;
	TransferInOutRepository tioRepo;
	RhLocationRepository rhLocationRepo;
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	private Environment env;

	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}
	
	String rupeeFontDir = null;
	String hindiFontDir = null;
	String reportsDir = null;
	Font rupee = null;
	Font hindi = null;
	Font hindi2 = null;

	private String armyNo, do2FromDate, do2Description, curqe;	
	StringBuffer buffer = new StringBuffer();
	AuditCage[] dueDrawnAuditCage = new AuditCage[2];
    List<AuditCageAllowance> blankList = new ArrayList<AuditCageAllowance>();
    List<ArrearsObject> arrearsObjectList = new ArrayList<ArrearsObject>();
    ArrearsObject arrearsObject = new ArrearsObject(); 
	boolean fkDo2NullAvailable;
	Date qeFromDate = null;
	Date  endingDate;
	Employee employee;
	Do2 currentDo2;
	Font f1 = FontFactory.getFont("HELVETICA", 9f, Font.BOLD, Color.BLACK);
	Font f2 = FontFactory.getFont("HELVETICA", 8f, Font.NORMAL, Color.BLACK);
	static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
	List<ArrearsDetailsObject> adoList = new ArrayList<ArrearsDetailsObject>();
	static public final String NO_ED_RECORD = "No Record available in Employee Deduction table. \n";
	static public final String NO_ATCG_FOR_DO2 = "No valid Audit Cage available for the DO2. \n";
	static public final String INCORRECT_ARMY_NO = "Either ArmyNo is incorrect or PBOR is non-effective. \n";
	static public final String INCORRECT_DESCR_FRDT = "Either description is incorrect or From date is incorrect or not an audit cage do2. \n";
	static java.util.HashMap<String, String> DESC_MAP;
	static public HashMap<String, String> getDescMap() {
		if (DESC_MAP != null)
			return DESC_MAP;

		DESC_MAP = new HashMap<String, String>();
		
		DESC_MAP.put("REJOSL", "OSL");
		DESC_MAP.put("REJAWL", "AWL");
		DESC_MAP.put("REDESN","DESN"); 
		DESC_MAP.put("RMILRI", "MILRI");
		DESC_MAP.put("REVDEP", "TFRDEP");
		
		return DESC_MAP;
	}
/*	public static List<String> nonRecurringAllowancesList = Arrays.asList("071", "136", "155", "156", "200", "201", "202",
			"204", "205", "211", "212", "213", "214", "217", "218", "219", "221",
			"228", "231", "239", "241", "242", "243", "265", "266", "267", "271",
			"272", "276", "277", "278", "279", "313", "322", "328", "331", "334",
			"336", "339","320","310");
	

	// PayCode 217 removed with ear file sent on 25.10.2018 (Dolphin Sys Cir 298)
	public static List<String> nonRecurringAllowancesList = Arrays.asList("071", "136", "155", "156", "200", "201", "202",
			"204", "205", "211", "212", "213", "214", "218", "219", "221",
			"228", "231", "239", "241", "242", "243", "265", "266", "267", "271",
			"272", "276", "277", "278", "279", "313", "322", "328", "331", "334",
			"336", "339", "320", "310");
	
*/	
	
	// PayCodes with CodeHead 352, 353, 354 and 291 added on 13.11.2019 For Dolphin Sys Cir 335
	public static List<String> nonRecurringAllowancesList = Arrays.asList("071", "136", "155", "156", "200", "201", "202",
			"204", "205", "211", "212", "213", "214", "218", "219", "221",
			"228", "231", "239", "241", "242", "243", "265", "266", "267", "271",
			"272", "276", "277", "278", "279", "313", "322", "328", "331", "334",
			"336", "339", "320", "310", "352", "353", "354", "291");
	
	public static List<String> abolishedDo2DescriptionList = Arrays.asList( "CILQ", "SNLQ", "FAA", "STRINC", "HCA", "FLCHCA", "ADALLC", "ACTGA",
			"COMNDO", "INSTAL", "OUTFIT", "HSINC", "HCWA", "CONDMT", "BOUNTY", "RLCA1", "RLCA2", "RLCA3", "RLCA4",
			"TYOJ", "TYRJ", "DAILY"); 
	
	public static List<String> auditCageDescList;	

	public String getArmyNo() {
		return armyNo;
	}

	public void setArmyNo(String armyNo) {
		this.armyNo = armyNo;
	}
	
	public String getDo2FromDate() {
		return do2FromDate;
	}

	public void setDo2FromDate(String do2FromDate) {
		this.do2FromDate = do2FromDate;
	}
	public String getDo2Description() {
		return do2Description;
	}

	public void setDo2Description(String do2Description) {
		this.do2Description = do2Description;
	}
	
 
	public void correctArrears() {
		Date endDate = Utils.getLastDayOfPreviousQuarter();
		GregorianCalendar endCal = new GregorianCalendar();
		endCal.setTime(endDate);		
		endingDate = endCal.getTime();
	//	Do2Manager.getPriorityTable();	
		/*
		auditCageDescList=em.createNativeQuery("SELECT distinct description FROM appendix_j where audit_account_cage = 'AT'").getResultList();
		*/
		auditCageDescList=appjRepo.findAuditCageDescription();
		auditCageDescList.add("LRA");
		auditCageDescList.add("INCR");
		auditCageDescList.add("DA");
		auditCageDescList.add("PROFIX");
		auditCageDescList.add("ACPFIX");
		auditCageDescList.add("ALWREV");
		auditCageDescList.add("FIX7PC");
		auditCageDescList.add("ENROLL");
	//	auditCageDescList.remove("CANCEL");
		
	}

	HashMap<String, Double> roundOffMap = new HashMap<String, Double>();

	public String insertDiffOfArrearsInArrears(String armyNo, String description, String fromdate) {
	//	System.out.println("......armyno "+armyNo+" description "+description+" fromdate "+fromdate);
		Date fromDate = null;	
		Date do2frdt = null;
		Date acFromDate = null;
		Date acToDate = null;
        fkDo2NullAvailable = false;
        curqe=Utils.getCurrentQuarterEnding();
		List<AuditCageAllowance> drawnList = new ArrayList<AuditCageAllowance>();
		List<AuditCageAllowance> dueList = new ArrayList<AuditCageAllowance>();
		this.armyNo=armyNo;
		this.do2Description=description;
		this.do2FromDate=fromdate;		
		employee=Utils.getCurrentEmployee(this.armyNo);		
		
		String frdt = this.do2FromDate.substring(4)+"-"+this.do2FromDate.substring(2,4)+"-"+this.do2FromDate.substring(0,2);
		try {
			do2frdt=DateManager.getDate(frdt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!isValid()){
			return "Not Valid"; 
		}	 
		fromDate=currentDo2.getFromDate();
		
		/*
		List<Do2> do2List = em.createQuery(
		"SELECT d FROM Do2 d where d.employee=:employee AND d.description = 'REVIEW' AND d.status ='V' order by d.fromDate")
		.setParameter("employee", employee).getResultList();*/
		List<Do2> do2List=do2Repo.getInitialValidReviewDo2(employee.getId());
		if (do2List.size() > 0) {
			if (do2List.get(0).getFromDate().before(fromDate) ){
				fromDate = do2List.get(0).getFromDate();
				if (fromDate.before(qeFromDate)){
					fromDate = qeFromDate;
				}
			}
		}
//		System.out.println("....... fromdate "+fromdate+"  fromDate "+fromDate+" qefrdt " +qeFromDate+" do2frdt " +do2frdt);
		if (fromDate.before(do2frdt)) {
			fromDate=do2frdt;
		}
		correctArrears();
		insertAtcorrDo2(employee, fromDate);
		/*
		List<Do2> do2AtcorrList = em.createQuery(
		"SELECT d FROM Do2 d where d.employee=:employee AND d.description = 'ATCORR' AND d.status ='V' AND d.fromDate =:fromDate AND d.qe=:qe")
		.setParameter("employee", employee)
		.setParameter("fromDate",fromDate)
		.setParameter("qe",curqe).getResultList();
		*/
		List<Do2> do2AtcorrList=do2Repo.getValidAtcorrDo2(employee.getId(), fromDate, curqe);
		if (do2AtcorrList.size() == 0) {
				return "Not Valid";
		}		
		
//		System.out.println("oooooooooooooooooooooooooo fromdate for getting due aclist  " +fromDate);
	/*	List<AuditCage> auditCageList = em.createQuery(
						"SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.effective=true " +
						"AND ac.fromDate>=:fromDate AND ac.recordStatus !='P' AND ac.cancelled=false  order by ac.fromDate, ac.toDate")
				.setParameter("employee", employee).setParameter("fromDate", fromDate).getResultList();
 */
		List<AuditCage> auditCageList=acRepo.findEffectiveFromDateGreaterThanEqualOrderByFromDateToDate(employee.getId(), fromDate);
		 
		for(AuditCage ac : auditCageList){
		
			   if(ac.getDo2() ==null){
				   fkDo2NullAvailable = true;
				   break;
			   }
		  }
	
	      if(!fkDo2NullAvailable){
			   sortAuditCages(auditCageList);
		  }
			  
		
		List<ArrearsObject> arrearsObjectList = new ArrayList<ArrearsObject>();
		int listSize = auditCageList.size();
      
	    for (int i = 0; i < listSize - 1; i++) {	
        	if ((auditCageList.get(i).getFromDate().equals(auditCageList.get(listSize - 1).getFromDate())) && 
        	   (auditCageList.get(listSize - 1).getToDate() != null && auditCageList.get(i).getToDate() == null)) {
        		AuditCage ac1 = auditCageList.get(i);
        		auditCageList.remove(i);
        		auditCageList.add(ac1);
        		break;
        	}
         }
	   
	    listSize = auditCageList.size();
		   
		for (int i = 0; i < listSize; i++) {
			AuditCage ac = auditCageList.get(i); 			

			if (i < listSize - 1) {

				if (ac.getFromDate().equals(auditCageList.get(i + 1).getFromDate()))
					continue;
				
			}
			acFromDate = ac.getFromDate();
			if (i < listSize -1) {
				acToDate = ac.getToDate();
				if (ac.getToDate() != null && 
					!ac.getToDate().equals(DateManager.getPreviousDate(auditCageList.get(i + 1).getFromDate()))){
					acToDate = DateManager.getPreviousDate(auditCageList.get(i + 1).getFromDate());
				}
			}				  
			arrearsObject = new ArrearsObject();
			dueDrawnAuditCage[0] = (AuditCage) Utils.getClonedEntity(ac);
			dueDrawnAuditCage[0].setArrestPayment(false);
			dueDrawnAuditCage[1] = (AuditCage) Utils.getClonedEntity(ac);
			setZeroValues(dueDrawnAuditCage[1]);
			drawnList = blankList;
					
			if (acToDate == null || acToDate.after(endingDate)) {
				acToDate = endingDate;
			}
			
			if (acFromDate.after(endingDate)) {
				continue;
			}			
			if (listSize - 1 == i){				
				if(acToDate !=null){
					acToDate =endingDate;
				}
			}		
			
			if (dueDrawnAuditCage[0].getAbsentFlag() == true) {
				setZeroValues(dueDrawnAuditCage[0]);
				dueList = blankList;
			} else {
	/*			dueList = em.createQuery("SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage")
						.setParameter("auditCage", ac).getResultList();*/
				dueList = acaRepo.findAllByAuditCageId(ac.getId());
			}
	
			arrearsObject.setDueAuditCage(dueDrawnAuditCage[0]);
			arrearsObject.setRegularPay(false);
		
			addDueDrawnElements(arrearsObject, dueDrawnAuditCage, dueList, drawnList);
			arrearsObjectList.add(arrearsObject);
		}   
		
		if (arrearsObjectList.size() > 0) {
			adoList.clear();
			calculateDue(arrearsObjectList);
			calculateDrawn(employee, fromDate);
			updateArrears(arrearsObjectList, currentDo2);
			if (fromDate.after(do2frdt)){
				buffer.append("Arrear calculation reviewed from " + fromDate.toString() + 
						      ". Please review arrears/recovery of earlier period manually");
	 		}
			return "Success";
	 	} else {
	 		return "Failure";
	 	}
		
		
	}
	
	
	public void calculateDue(List<ArrearsObject> arrearsObjectList) {		
		AuditCage dueAuditCage = null;
		PayCode payCode = null;
		double monthCount = 0.0;
		double dueAmount = 0.0;
		double drawnAmount = 0.0;
		Date fromDate = null;
		Date toDate = null; 
		ArrearsObject arrearsObject = null;
		
		for (int i = 0; i < arrearsObjectList.size(); i++) {
			arrearsObject = arrearsObjectList.get(i);
			dueAuditCage = arrearsObject.getDueAuditCage();
			
			if(i < arrearsObjectList.size() -1){
				
				if(dueAuditCage.getFromDate().equals(arrearsObjectList.get(i+1).getDueAuditCage().getFromDate()))
					continue;
			}
			List<DueDrawnElement> dueDrawnElementList = arrearsObject.getDueDrawnElementList();

			fromDate = dueAuditCage.getFromDate(); 			
			toDate = dueAuditCage.getToDate();		
			monthCount = DateManager.getNoOfMonths(fromDate, toDate);
            
			for (DueDrawnElement element : dueDrawnElementList) {

				payCode = element.getPayCode();
				dueAmount = element.getDue() * monthCount;
				drawnAmount = element.getDrawn() * monthCount;
				double fullValue = dueAmount - drawnAmount;
				if (fullValue == 0.0)
					continue;		
				Double amount = 0.0;
				
				if (!roundOffMap.containsKey(String.valueOf(payCode.getId()))){
					amount = new Double(0.0);
					roundOffMap.put(new String(String.valueOf(payCode.getId())), amount);
				} 
				if (roundOffMap.containsKey(String.valueOf(payCode.getId()))){
					roundOffMap.put(String.valueOf(payCode.getId()), roundOffMap.get(String.valueOf(payCode.getId()))+new BigDecimal(fullValue).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				/*
				 * if (payCode.getId() == 55){ System.out.println("due - fromdate " + fromDate +
				 * " to date " + toDate + " due amount " + fullValue); }
				 */				
				ArrearsDetailsObject ado = new ArrearsDetailsObject();
				ado.setPayCode(String.valueOf(payCode.getId()));
				ado.setDueFromDate(df.format(fromDate));
				ado.setDueToDate(df.format(toDate));
				ado.setDueAmount(new BigDecimal(fullValue).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); 
				ado.setDueQe(curqe);
				ado.setDrawnFromDate("");
				ado.setDrawnToDate("");
				ado.setDrawnAmount(0.00);
				ado.setDrawnQe("");
				adoList.add(ado);
			}
		}
		
	}
	public void calculateDrawn(Employee employee, Date fromDate) {	
		Date arrFromDate = null;
		Date arrToDate = null;
		String frdt = null;
		String qe = null;
		int qtrend = 0;
		PayCode payCode = null;
		String descr = null;
		double monthCount = 0.0;
		double fullMonthCount = 0.0;
		double drawnAmount = 0.0;
	/*	List<EmployeeDeduction> edList=em.createQuery(
				"SELECT ed FROM EmployeeDeduction ed where ed.employee=:employee order by " +
				"((CAST(substr(ed.qe,4) AS int) * 100) + CAST(substr(ed.qe,0,3) AS int))")
				.setParameter("employee", employee)
				.getResultList();*/
	 	
		List<EmployeeDeduction> edList=edRepo.findByEmployeeIdOrderByQeInt(employee.getId());
		qe=edList.get(0).getQe();
		qtrend=(Integer.parseInt(qe.substring(3)) * 100)+Integer.parseInt(qe.substring(0,2));
		/*
		List<Arrears> arrearList = em.createQuery(
				" SELECT ar FROM Arrears ar where ar.employee=:employee  " +
				" AND (ar.fromDate>=:fromDate OR (:fromDate > ar.fromDate AND :fromDate <= ar.toDate)) " +
				" AND ((CAST(substr(ar.qe,4) AS int) * 100) + CAST(substr(ar.qe,0,3) AS int)) >=:qtrend order by ar.payCode, ar.fromDate")
		.setParameter("employee", employee)
		.setParameter("fromDate", fromDate)
		.setParameter("qtrend", qtrend) 
		.getResultList();*/
 
		List<Arrears> arrearList=arrRepo.findByEmployeeIdFromDateGreaterThanAndQeGreaterThanOrderByPayCodeFromDate(employee.getId(), fromDate, qtrend);
//		System.out.println(".............. arrears size  " +arrearList.size());
		if (arrearList.size() > 0){
			for (int i = 0; i < arrearList.size(); i++) {
				Arrears ar = arrearList.get(i);
				arrFromDate = ar.getFromDate(); 			 
				arrToDate = ar.getToDate();
				payCode = ar.getPayCode();
				descr = null;
// System.out.println("........... arrears id " +ar.getId()+"  paycode " +payCode);
				if (ar.getDo2() != null) {
					descr = ar.getDo2().getDescription();
				}
				
				if (descr == null) {
					;
				} else if (descr.equals("REVIEW")) {
					; 
				} else if (descr.equals("AWARD") && (ar.getDo2().getColumnA().equals("LSGCM") || ar.getDo2().getColumnA().equals("MSM"))){
					continue;				 
			    } else if (descr.equals("CANCEL") && 
						(ar.getDo2().getColumnA() != null && auditCageDescList.contains(ar.getDo2().getColumnA()))){
					;
				} else if (auditCageDescList.contains(descr)) {
					;
				} else  {
					continue; 
				}
				
			//	System.out.println  ( " arreas from date " +ar.getFromDate()   +" ar desc " +descr);
				/*if (!arrFromDate.before(DateManager.getSeventhCpcAllowanceDate())) {
				//	System.out.println ( " abolished descriptions " +Do2Manager.ABOLISHED_DESCRIPTIONS_SEVENCPC);
					if (abolishedDo2DescriptionList.contains(descr)){
						continue;
					}
				}*/
				if (payCode!=null && nonRecurringAllowancesList.contains(payCode.getCodeHead())) {
					continue;
				}
				if (ar.getRegularPay() == true && ar.getQe().equals(curqe)) {
					continue;
				}
                drawnAmount = 0.0;		
				if (arrFromDate.before(fromDate) && !arrToDate.before(fromDate)){
					fullMonthCount = DateManager.getNoOfMonths(arrFromDate, arrToDate);
					monthCount = DateManager.getNoOfMonths(fromDate, arrToDate);
					drawnAmount = ar.getAmount() * monthCount / fullMonthCount;					
				} else if (!arrFromDate.before(fromDate)) {
					drawnAmount=ar.getAmount();
				}
				Double amount=0.0; 
				if (!roundOffMap.containsKey(String.valueOf(payCode.getId()))){
		//			amount = new Double(0.0);
					roundOffMap.put(new String(String.valueOf(payCode.getId())), amount);
				} 
				if (roundOffMap.containsKey(String.valueOf(payCode.getId()))){
					roundOffMap.put(String.valueOf(payCode.getId()), roundOffMap.get(String.valueOf(payCode.getId()))-new BigDecimal(drawnAmount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				/*
				 * if (payCode.getId() == 55){ System.out.println("drawn - fromdate " +
				 * arrFromDate + " to date " + arrToDate + " drawn amount " + drawnAmount); }
				 */  	
				if (adoList != null && adoList.size() > 0){
					boolean updated = false;
					for (ArrearsDetailsObject ado : adoList){
						if (ado.getPayCode().equals(String.valueOf(payCode.getId()))){
							if (ado.getDrawnFromDate().equals("")){
								ado.setDrawnFromDate(df.format(arrFromDate));
								ado.setDrawnToDate(df.format(arrToDate));
								ado.setDrawnAmount(new BigDecimal(drawnAmount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
								ado.setDrawnQe(ar.getQe());
								updated=true;
								break;
							} 
						}
					}
					if (updated == false){
						ArrearsDetailsObject adso = new ArrearsDetailsObject();				
						adso.setPayCode(String.valueOf(payCode.getId()));
						adso.setDueFromDate("");
						adso.setDueToDate("");
						adso.setDueAmount(0.0);
						adso.setDueQe("");
						adso.setDrawnFromDate(df.format(arrFromDate));
						adso.setDrawnToDate(df.format(arrToDate));
						adso.setDrawnAmount(drawnAmount);
						adso.setDrawnQe(ar.getQe());
						adoList.add(adso);
					}
				} else {
					ArrearsDetailsObject ado = new ArrearsDetailsObject();				
					ado.setPayCode(String.valueOf(payCode.getId()));
					ado.setDueFromDate("");
					ado.setDueToDate("");
					ado.setDueAmount(0.0);
					ado.setDueQe("");
					ado.setDrawnFromDate(df.format(arrFromDate));
					ado.setDrawnToDate(df.format(arrToDate));
					ado.setDrawnAmount(drawnAmount);
					ado.setDrawnQe(ar.getQe());
					adoList.add(ado);
				}
			}
		}
	}
	public void updateArrears(List<ArrearsObject> arrearsObjectList, Do2 currentdo2) {
		List<String> payCodeList=new ArrayList<String>();
		Date fromDate = null;
		Date toDate = null; 
		boolean arrearsUpdated = false;
		fromDate = arrearsObjectList.get(0).getDueAuditCage().getFromDate();
		toDate = arrearsObjectList.get(arrearsObjectList.size() - 1)
				.getDueAuditCage().getToDate();
		if (toDate == null)
			toDate = Utils.getLastDayOfPreviousQuarter();
		Set<String> payCodes = roundOffMap.keySet();
		for (String roundOffPayCode : payCodes) {
			double amount = Math.round(roundOffMap.get(roundOffPayCode));
			amount = (double) Math.round(amount);
			payCodeList.add(roundOffPayCode);
//			System.out.println(".............round off paycode " +roundOffPayCode+ " amount "+amount);
			if (amount < -5 || amount > 5){
				if (arrearsUpdated  == false){
					Do2 reviewDo2 = new Do2();	               
					reviewDo2.setEmployee(arrearsObjectList.get(0).getDueAuditCage().getEmployee());
					reviewDo2.setArmyNo(arrearsObjectList.get(0).getDueAuditCage().getEmployee().getArmyNo());
					reviewDo2.setCheckDigit(arrearsObjectList.get(0).getDueAuditCage().getEmployee().getCheckDigit());
					reviewDo2.setUnitCode(arrearsObjectList.get(0).getDueAuditCage().getEmployee().getUnit().getUnitCode());
					reviewDo2.setUnit(arrearsObjectList.get(0).getDueAuditCage().getEmployee().getUnit());
					reviewDo2.setDo2Group("IA");
					reviewDo2.setDo2Type("0");
					reviewDo2.setDo2No("9999");
					reviewDo2.setDo2ItemNo("999");
					reviewDo2.setDo2Date(DateManager.getToday());
					reviewDo2.setDescription("REVIEW");
					reviewDo2.setRemarks("Reviewed due-drawn payments of auditcage Dos2");
					reviewDo2.setFromDate(fromDate);
					reviewDo2.setBatchNo(999);
					reviewDo2.setStatus(Do2Manager.VALID);
					reviewDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
					reviewDo2.setAuditCage(false);
					reviewDo2.setQe(curqe);
					reviewDo2.setInputQe(curqe);
					reviewDo2.setAcceptedQe(curqe);
					reviewDo2.setCreatedAt(DateManager.getCurrentTimestamp());
					do2Repo.save(reviewDo2);
					arrearsUpdated=true;
				}
			} else {
			  continue;		
			}/*
			List<Do2> do2List = em.createQuery(
					"SELECT d FROM Do2 d where d.employee=:employee AND d.fromDate=:fromDate AND d.status ='V' AND " +
					"d.description=:descr AND d.qe=:qtrend order by d.id desc")
					.setParameter("employee", arrearsObjectList.get(0).getDueAuditCage().getEmployee())
					.setParameter("fromDate",fromDate)
					.setParameter("descr", "REVIEW")
					.setParameter("qtrend", curqe)
					.getResultList();*/
			List<Do2> do2List=do2Repo.findByEmployeeIdAndDescriptionAndFromDateAndQeOrderByIdDesc(arrearsObjectList.get(0).getDueAuditCage().getEmployee().getId(), 
					"REVIEW", fromDate, curqe);
 	 		Arrears arrears = new Arrears();
			arrears.setPayCode((PayCode) pcRepo.findByPayCodeId(Integer.parseInt(roundOffPayCode)));
			arrears.setFromDate(fromDate);
			arrears.setToDate(toDate);
			arrears.setAmount(amount);
			arrears.setArrestPayment(false);
			if (do2List.size() > 0) {
				arrears.setDo2(do2List.get(0));
			}
			arrears.setRegularPay(false);
			arrears.setEmployee(arrearsObjectList.get(0).getDueAuditCage().getEmployee());
			arrears.setPaid(false);
			arrears.setCreatedAt(DateManager.getCurrentTimestamp());
			arrears.setRoundOffAmount(false);
			arrears.setQe(curqe);
			arrRepo.save(arrears);	
		}
		if (adoList != null && adoList.size() > 0) {
			generateReport(employee, payCodeList);
		} 
		if (arrearsUpdated == true) {
			currentdo2.setReviewQe(curqe);
			do2Repo.save(currentdo2);			
		} else {
	//		FacesMessages.instance().add("No Arrears/Recovery due to the PBOR.");
		}
		
	}
	HashMap<String, AuditCageAllowance> drawnAllowanceMap = new HashMap<String, AuditCageAllowance>();
	HashMap<String, AuditCageAllowance> dueAllowanceMap = new HashMap<String, AuditCageAllowance>();

	private List<DueDrawnElement> getDueDrawnAllowanceList(AuditCage[] dueDrawnAuditCage, 
			List<AuditCageAllowance> dueAllowanceList,
			List<AuditCageAllowance> drawnAllowanceList) {
		List<DueDrawnElement> dueDrawnElementList = new ArrayList<DueDrawnElement>();
		if (dueDrawnAuditCage[0].getFromDate().after(Utils.getLastDayOfPreviousQuarter()))
			return dueDrawnElementList;
		drawnAllowanceMap.clear();
		dueAllowanceMap.clear();

		for (AuditCageAllowance drawnAllowance : drawnAllowanceList) {
			drawnAllowanceMap.put(drawnAllowance.getPayCode().getPayCode(),
					drawnAllowance);
		}

		for (AuditCageAllowance dueAllowance : dueAllowanceList) {
			AuditCageAllowance drawnAllowance = drawnAllowanceMap
					.get(dueAllowance.getPayCode().getPayCode());
			if (drawnAllowance == null)	{
				DueDrawnElement element = new DueDrawnElement();
				element.setPayCode(dueAllowance.getPayCode());
				element.setDue(dueAllowance.getAmount());
				element.setDrawn(drawnAllowance == null ? 0 : drawnAllowance.getAmount());
				dueDrawnElementList.add(element);
			}
		}

		for (AuditCageAllowance dueAllowance : dueAllowanceList) {
			dueAllowanceMap.put(dueAllowance.getPayCode().getPayCode(),	dueAllowance);
		}

		for (AuditCageAllowance drawnAllowance : drawnAllowanceList) {
			AuditCageAllowance dueAllowance = dueAllowanceMap.get(drawnAllowance.getPayCode().getPayCode());
			if (dueAllowance == null) {
				DueDrawnElement element = new DueDrawnElement();
				element.setPayCode(drawnAllowance.getPayCode());
				element.setDrawn(drawnAllowance.getAmount());
				element.setDue(dueAllowance == null ? 0 : dueAllowance
						.getAmount());
				dueDrawnElementList.add(element);
			}
		}
		return dueDrawnElementList;
	}

	public void addDueDrawnElements(ArrearsObject arrearsObject,
			AuditCage[] dueDrawnAuditCage, List<AuditCageAllowance> dueList,
			List<AuditCageAllowance> drawnList) {
		arrearsObject.setDueAuditCage(dueDrawnAuditCage[0]);			
			
		List<DueDrawnElement> dueDrawnElementList = getAuditCageOnlyDueDrawnList(dueDrawnAuditCage);
		dueDrawnElementList.addAll(getDueDrawnAllowanceList(dueDrawnAuditCage, dueList, drawnList));
		arrearsObject.setDueDrawnElementList(dueDrawnElementList);

		arrearsObjectList.add(arrearsObject);
		
	}
	public List<DueDrawnElement> getAuditCageOnlyDueDrawnList(AuditCage[] dueDrawnAuditCage) {

		List<DueDrawnElement> dueDrawnElementList = new ArrayList<DueDrawnElement>();

		AuditCage dueAuditCage = dueDrawnAuditCage[0];
		AuditCage drawnAuditCage = dueDrawnAuditCage[1];
 
		if (dueAuditCage.getFromDate().after(Utils.getLastDayOfPreviousQuarter()))
			return dueDrawnElementList;
		if (dueAuditCage.getToDate() == null
				|| dueAuditCage.getToDate().after(Utils.getLastDayOfPreviousQuarter())) {
					dueAuditCage.setToDate(Utils.getLastDayOfPreviousQuarter());
		}
		
	 	dueDrawnElementList.add(getDueDrawnElement(Do2Manager.BASIC_PAY,
					dueAuditCage.getBasicPay(), drawnAuditCage.getBasicPay()));
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.DP,
					dueAuditCage.getDp(), drawnAuditCage.getDp()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.GRADE_PAY,
					dueAuditCage.getGradePay(), drawnAuditCage.getGradePay()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.MS_PAY,
					dueAuditCage.getMsPay(), drawnAuditCage.getMsPay()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.X_GROUP_PAY,
					dueAuditCage.getXGroupPay(), drawnAuditCage.getXGroupPay()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.CLASS_PAY,
					dueAuditCage.getClassPay(), drawnAuditCage.getClassPay()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.PMHAL,
					dueAuditCage.getPmha(), drawnAuditCage.getPmha()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.LRA,
					dueAuditCage.getLra(), drawnAuditCage.getLra()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.TPTL, 
					dueAuditCage.getTransport(), drawnAuditCage.getTransport()));		
		dueDrawnElementList.add(getDueDrawnElement(Do2Manager.DA,
					dueAuditCage.getDa(), drawnAuditCage.getDa()));

		return dueDrawnElementList;
	}
 
	private DueDrawnElement getDueDrawnElement(String payCode, double due,
			double drawn) {
		DueDrawnElement element = new DueDrawnElement();
		element.setPayCode(Utils.getPayCode(payCode));
		element.setDue(due);
		element.setDrawn(drawn);
		return element;
	}

	@SuppressWarnings("unchecked")
	public void sortAuditCages(List<AuditCage> auditCageList) {
		
		   
		   for (AuditCage auditCage : auditCageList) {
			   String columnA = null;
	       if (auditCage.getDo2().getColumnA() != null) {
				  columnA = auditCage.getDo2().getColumnA().trim();
			  }	  
			  if (Do2Manager.priorityDescList.contains(auditCage.getDo2().getDescription().trim()) ){
			      auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
			  } else if (columnA != null){
				 auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()+ " " + columnA));
			  } else {
				  auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
			  }
		   }
		   Collections.sort(auditCageList);
	}
	public void setZeroValues(AuditCage auditCage) {
		
		auditCage.setBasicPay(0);
		auditCage.setDp(0);
		auditCage.setGradePay(0);
		auditCage.setMsPay(0);
		auditCage.setXGroupPay(0);
		auditCage.setClassPay(0);
		auditCage.setDa(0);
		auditCage.setTransport(0);
		auditCage.setPmha(0);
		auditCage.setLra(0);
	}	
	public boolean isValid() {
		Date fromDate = null;
		String frdt = null;
		String qefrdt = null;
		String qe = null;
		int qtrend = 0;
		String orgDesc=null;
		String startDesc=null;
		List<Do2> do2List=null;
		
		 
		employee=Utils.getCurrentEmployee(armyNo);
		if (employee == null){
			buffer.append(INCORRECT_ARMY_NO);
			return false;
		} else if (employee.getStatusCode().getId() > 4) {
		/*	List<LpcRegister> neList = Utils.getEntityManager().createQuery("SELECT lpc from LpcRegister lpc WHERE " +
					"  lpc.employee=:emp AND lpc.recordStatus=:stat AND lpc.fsApproved=true AND lpc.ecsGenerated=true ")
					.setParameter("stat", 'F')
					.setParameter("emp", employee)
					.getResultList();*/
 
			List<LpcRegister> neList=lpcRepo.findFsAprovedAndEcsGeneratedForArrearcorr(employee.getId());
			if (neList.size()==0) {
				buffer.append(INCORRECT_ARMY_NO);	
				return false;
			}
		}
		
		if (this.do2FromDate!=null) {
			Date fromdate = null;
			frdt = this.do2FromDate.substring(4)+"-"+this.do2FromDate.substring(2,4)+"-"+this.do2FromDate.substring(0,2);

			try {
				fromdate = DateManager.getDate(frdt);
			} catch (Exception e) {
				Utils.handleException(e);
			}
			if (fromdate!=null && fromdate.before(Utils.getDolphinStartDate())) {
				buffer.append("INCORRECT FROMDATE.  CORRECTION CAN BE DONE AFTER DOLPHIN START DATE ONLY!!!");
				return false;
			}			
		}
		
		orgDesc = getDescMap().get(this.do2Description);		
		if (orgDesc != null) {
			buffer.append("Please feed particulars of corresponding " + orgDesc + " and from date of " + orgDesc + 
					      "  to find out diff of arrears of  " + this.do2Description);
			return false;
		}
		frdt = this.do2FromDate.substring(4)+"-"+this.do2FromDate.substring(2,4)+"-"+this.do2FromDate.substring(0,2);

		try {
			fromDate=DateManager.getDate(frdt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	/* 	
		List<EmployeeDeduction> edList=em.createQuery(
				"SELECT ed FROM EmployeeDeduction ed where ed.employee=:employee and closed=true order by " +
				"((CAST(substr(ed.qe,4) AS int) * 100) + CAST(substr(ed.qe,0,3) AS int))")
				.setParameter("employee", employee)
				.getResultList();  */
		List<EmployeeDeduction> edList=edRepo.findByEmployeeIdClosedOrderbyIntQe(employee.getId());
		
		if (edList.size() == 0){
			buffer.append(NO_ED_RECORD);
			return false;
		}
		qe=edList.get(0).getQe();
		qtrend=(Integer.parseInt(qe.substring(3))*100)+Integer.parseInt(qe.substring(0,2));
		qefrdt = qe.substring(3)+"-"+qe.substring(0,2)+"-01";
		try {
			qeFromDate=DateManager.getDate(qefrdt);
		} catch (Exception e) {
			Utils.handleException(e);
		}	
		
	/*	List<Do2> enrollDo2List = Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.description='ENROLL' and "
				+ " d.status='V' and d.acceptedQe=:aqe ").setParameter("emp", employee).setParameter("aqe", qe).getResultList();*/
		
		List<Do2> enrollDo2List=do2Repo.findByEmployeeIdAndDescriptionAndStatusAndAcceptedQe(employee.getId(), "ENROLL", "V", qe);
		
	//	System.out.println("xxxxxxxxxxxxxxxxx enroldo2list  "+enrollDo2List.size() +"  fromdate "+fromDate+"  qefromdate "+qeFromDate +"  dolstartdt "+Utils.getDolphinStartDate());
		
		if (enrollDo2List.size()>0) {
			if (enrollDo2List.get(0).getFromDate().after(Utils.getDolphinStartDate())) {
				fromDate = enrollDo2List.get(0).getFromDate();
			} else {
				fromDate = qeFromDate;
			}
		}
		
		if (fromDate.before(qeFromDate)) {			
	/*        do2List = em.createQuery(
			" SELECT d FROM Do2 d where d.employee=:employee AND d.fromDate > :fromDate AND d.status ='V' AND " +
			" d.auditCage=true AND ((CAST(substr(d.acceptedQe,4) AS int) * 100) + CAST(substr(d.acceptedQe,0,3) AS int)) >=:qtrend " +
			" order by d.fromDate")
			.setParameter("employee", employee)
		//	.setParameter("fromDate", qeFromDate)
			.setParameter("fromDate", fromDate)
			.setParameter("qtrend", qtrend)
			.getResultList();*/
			
			do2List=do2Repo.getAuditCageAffectedDosIIAfterIrlaCreation(employee.getId(), fromDate, qtrend);
			
			if (do2List != null && do2List.size() == 0){
				buffer.append("No Audit Cage effective DOsII after creation of IRLA");	
				return false;
			} else {
				fromDate = do2List.get(0).getFromDate();
	//			buffer.append(DO2_ADJUSTED_BEFORE_IMPLEMENTATION_OF_DOLPHIN);					
			}
		} else {
	/*		do2List = em.createQuery(
					"SELECT d FROM Do2 d where d.employee=:employee AND d.fromDate=:fromDate AND d.status ='V' AND " +
					"d.description=:descr AND d.auditCage=true AND ((CAST(substr(d.acceptedQe,4) AS int) * 100) + CAST(substr(d.acceptedQe,0,3) AS int)) >=:qtrend")
					.setParameter("employee", employee)
					.setParameter("fromDate", fromDate)
					.setParameter("descr", this.getDo2Description())
					.setParameter("qtrend", qtrend)
					.getResultList();  */
	       
			do2List=do2Repo.getReviewStartDoII(employee.getId(), fromDate, this.getDo2Description(), qtrend);
			
			if (do2List != null && do2List.size() == 0){
				buffer.append(INCORRECT_DESCR_FRDT);	
				return false;
			} 
		}
		
		if (do2List.size() > 0){
			Do2 d = do2List.get(0);
		/*	List<AuditCage> auditCageList = em.createQuery(
					"SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.effective=true " +
					"AND ac.fromDate=:fromDate AND ac.recordStatus !='P' AND ac.cancelled=false AND ac.do2=:id " +
					" AND ((CAST(substr(ac.qe,4) AS int) * 100) + CAST(substr(ac.qe,0,3) AS int)) >=:qtrend")
			.setParameter("employee", employee)
			.setParameter("fromDate", fromDate)
			.setParameter("id", d)
			.setParameter("qtrend", qtrend)
			.getResultList();*/
			List<AuditCage> auditCageList = acRepo.getAuditCageForReviewStartDo2(employee.getId(), fromDate, d.getId(), qtrend);
	 			
			if (auditCageList.size() == 0){
				buffer.append(NO_ATCG_FOR_DO2);	
				return false;
			} 
			currentDo2=d;
		}  
		
	 	return true;
	}
	
	public void insertAtcorrDo2(Employee employee, Date fromDate) {
		
		Do2 atcorrDo2 = new Do2();		
		atcorrDo2.setEmployee(employee);
		atcorrDo2.setArmyNo(employee.getArmyNo());
		atcorrDo2.setCheckDigit(employee.getCheckDigit());
		atcorrDo2.setUnitCode(employee.getUnit().getUnitCode());
		atcorrDo2.setUnit(employee.getUnit());
		atcorrDo2.setDo2Group("IA");
		atcorrDo2.setDo2Type("0");
		atcorrDo2.setDo2No("3333");
		atcorrDo2.setDo2ItemNo("333");
		atcorrDo2.setDo2Date(fromDate);
		atcorrDo2.setDescription("ATCORR");
		atcorrDo2.setFromDate(fromDate);
		atcorrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		atcorrDo2.setAuditCage(true);
		atcorrDo2.setInputQe(curqe);
		atcorrDo2.setStatus(Do2Manager.PENDING);
		atcorrDo2.setQe(curqe);
		atcorrDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2Repo.save(atcorrDo2);		 
		processAtcorrDo2(atcorrDo2); 
	}	
	
	public void processAtcorrDo2(Do2 atcorrDo2) {
		AuditCageProcessor atProcessor = new AuditCageProcessor();
		atProcessor.setAcRepo(acRepo);
		atProcessor.setDo2Repo(do2Repo);
		atProcessor.setAcaRepo(acaRepo);
		atProcessor.setArrRepo(arrRepo);
		atProcessor.setAppjRepo(appjRepo);
		atProcessor.setPsRepo(psRepo);
		atProcessor.setEsdRepo(esdRepo);
		atProcessor.setRankRepo(rankRepo);
		atProcessor.setGcRepo(gcRepo);
		atProcessor.setMatrixRepo(matrixRepo);
		atProcessor.setClassPayRepo(classPayRepo);
		atProcessor.setApptRateRepo(apptRateRepo);
		atProcessor.setAlwRateRepo(alwRateRepo);
		atProcessor.setEmpRepo(empRepo);
		atProcessor.setScRepo(scRepo);
		atProcessor.setUnitRepo(unitRepo);
		atProcessor.setRrRepo(rrRepo);
		atProcessor.setLpcRepo(lpcRepo);
		atProcessor.setLpcDepRepo(lpcDepRepo);
		atProcessor.setBhutanSlabRepo(bhutanSlabRepo);
		atProcessor.setEdRepo(edRepo);
		atProcessor.setFsRepo(fsRepo);
		atProcessor.setEeRepo(eeRepo);
		atProcessor.setF16Repo(f16Repo);
		atProcessor.setAdjCbRepo(adjCbRepo);
		atProcessor.setFdRepo(fdRepo);
		atProcessor.setTioRepo(tioRepo);
		atProcessor.setRhLocationRepo(rhLocationRepo);

		atProcessor.setJdbcTemplate(jdbcTemplate);

		atProcessor.process(atcorrDo2); 
	}
	
	public void generateReport(Employee emp, List<String>payCodeList) {			  
			File f = new File(reportsDir+"/ArrearsVerification");
			if (!f.exists())
				f.mkdirs();
			
			String currentMonth = Utils.getCurrentMonth().substring(0,2)+Utils.getCurrentMonth().substring(3);
			String fileName = reportsDir+"/ArrearsVerification/ArrearsReview"+emp.getArmyNo()+"_"+currentMonth+".pdf";
		 
			EndPageEvent1 endPageEvent = new EndPageEvent1();
			float[] width = { 100f };
			PdfPTable finalTable = new PdfPTable(width);
			finalTable.setWidthPercentage(95f);
			Document pdf = new Document();
			PdfWriter writer = null;
//			System.out.println("filename ............  "+fileName);
			try {
				writer = PdfWriter.getInstance(pdf, new FileOutputStream(fileName));
			} catch (Exception e1) {
				Utils.handleException(e1);
			}
			pdf.setPageSize(PageSize.A4);
			writer.setViewerPreferences(PdfWriter.PageModeUseOutlines | PdfWriter.FitWindow);
			pdf.setMargins(25f, 25f, 50f, 20f);
			writer.setPageEvent(endPageEvent);
			endPageEvent.setPaoName(Utils.getPao().getPaoName());
			endPageEvent.setEmpName(emp.getArmyNo() + emp.getCheckDigit() + " " + emp.getName());
			endPageEvent.setDescription("REVIEW");
			endPageEvent.setFromDate(this.do2FromDate);
			endPageEvent.setUnitName(emp.getUnit().getUnitName());
			endPageEvent.setTask(String.valueOf(emp.getUnit().getTask_1()));
			pdf.open();
			addArrearsTable(pdf, payCodeList);
			pdf.close();
			writer.close();
	}
	
	public void addArrearsTable(Document doc, List<String>payCodeList) {
		PdfPTable arrearsTable  = new PdfPTable(new float[]{100f});
		PdfPTable arrearsHeader = new PdfPTable(new float[] {4f, 3.5f, 3.5f, 5f, 3.5f, 3.5f, 3.5f, 5f});
		PdfPTable arrearsData   = new PdfPTable(new float[] {4f, 3.5f, 3.5f, 5f, 3.5f, 3.5f, 3.5f, 5f});
		PdfPTable arrearsTotal  = new PdfPTable(new float[] {4f, 3.5f, 3.5f, 5f, 3.5f, 3.5f, 3.5f, 5f});
		arrearsHeader.setWidthPercentage(100f);
		arrearsData.setWidthPercentage(100f);
		arrearsTotal.setWidthPercentage(100f);
		arrearsTable.setWidthPercentage(100f);
		double dueTotal = 0;
		double breakUpDueTotal = 0;
		double drawnTotal = 0;
		double breakUpDrawnTotal = 0;
		double diff = 0;
		String payCode = null;
		int kounter = 0;		
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "Sl. No.", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "From Date", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "To Date", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "Due Amount", f1);	
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "From Date", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "To Date", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "Paid Me", f1);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsHeader, "Drawn Amount", f1);	
		for (int j = 0; j < payCodeList.size(); j++){
		//	System.out.println("................... paycodelist size  " +payCodeList.size());
		//	System.out.println("................... paycode string    " +payCodeList.get(j));
			PayCode pc = (PayCode) pcRepo.findByPayCodeId(Integer.parseInt(payCodeList.get(j)));
			payCode=pc.getPayCode();
			ReportUtils.addCell2Table(Element.ALIGN_LEFT, Element.ALIGN_MIDDLE, arrearsData, "Pay Code : " + payCode,
					f1, null, "0", null, null, null, "8");
		    for (int i = 0 ; i < adoList.size() ; i++) {
    			if (payCodeList.get(j).equals(adoList.get(i).getPayCode())) {
    					ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, 
    							"(" + String.valueOf(++kounter) + ")", f2, null, null, null, "0", null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, 
    							adoList.get(i).getDueFromDate(), f2, null, null, null, null, null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, 
    							adoList.get(i).getDueToDate(), f2, null, null, null, null, null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsData, 
    		//					String.valueOf(adoList.get(i).getDueAmount().intValue()), f2, null, null, null, null, null, null);
    							String.valueOf(Math.round(adoList.get(i).getDueAmount())), f2, null, null, null, null, null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, 
    							adoList.get(i).getDrawnFromDate(), f2, null, null, null, null, null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, 
    							adoList.get(i).getDrawnToDate(), f2, null, null, null, null, null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, 
    							adoList.get(i).getDrawnQe(), f2, null, null, null, null, null, null);
    					ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsData, 
    							String.valueOf(adoList.get(i).getDrawnAmount().intValue()), f2, null, null, null, null, null, null);
    					dueTotal += adoList.get(i).getDueAmount();
    					breakUpDueTotal += adoList.get(i).getDueAmount();
    					drawnTotal += adoList.get(i).getDrawnAmount();
    					breakUpDrawnTotal += adoList.get(i).getDrawnAmount();
 //   					System.out.println(".......... paycode " +adoList.get(i).getPayCode()+"  due " +adoList.get(i).getDueAmount()+" drawn " +adoList.get(i).getDrawnAmount());
        		} 
		    }	
//		    System.out.println("........b4 finally diff " +diff );
			diff=(int)Math.round(breakUpDueTotal-breakUpDrawnTotal);
			if (breakUpDueTotal!=breakUpDrawnTotal && diff==0) {
				diff=((int)Math.round(breakUpDueTotal)-(int)Math.round(breakUpDrawnTotal));				
			}
//			 System.out.println("........finally diff " +diff );
			ReportUtils.addCell2Table(Element.ALIGN_LEFT, Element.ALIGN_MIDDLE, arrearsData, "Arr/Rec of " + payCode + ":",
					f1, null, "0", null, null, null, "3");			
			ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsData, ReportUtils.indianRupeeFormat((int)diff), 
					f1, "0", null, null, null, null, null);
			ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, "Due :", 
					f1, "0", "0", null, null, null, null);
			ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsData, ReportUtils.indianRupeeFormat((int)Math.round(breakUpDueTotal)), 
					f1, "0", null, null, null, null, null);
			ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsData, "Drawn :", 
					f1, "0", "0", null, null, null, null);
			ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsData, ReportUtils.indianRupeeFormat((int)Math.round(breakUpDrawnTotal)), 
					f1, "0", null, null, null, null, null);
			breakUpDueTotal = 0;
			breakUpDrawnTotal = 0;
			diff=0;
			kounter = 0;
    	}
		diff=(int)Math.round(dueTotal-drawnTotal);
		if (dueTotal!=drawnTotal && diff==0) {
			diff=((int)Math.round(dueTotal)-(int)Math.round(drawnTotal));
		}
		ReportUtils.addCell2Table(Element.ALIGN_LEFT, Element.ALIGN_MIDDLE, arrearsTotal, "Grand Arrears/Rec :",
				f1, null, "0", null, null, null, "3");
		ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsTotal, ReportUtils.indianRupeeFormat((int)diff), 
				f1, "0", null, null, null, null, null);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsTotal, "Due :", 
				f1, "0", "0", null, null, null, null);
		ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsTotal, ReportUtils.indianRupeeFormat((int)Math.round(dueTotal)), 
				f1, "0", null, null, null, null, null);
		ReportUtils.addCell2Table(Element.ALIGN_CENTER, Element.ALIGN_MIDDLE, arrearsTotal, "Drawn :", 
				f1, "0", "0", null, null, null, null);
		ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, arrearsTotal, ReportUtils.indianRupeeFormat((int)Math.round(drawnTotal)), 
				f1, "0", null, null, null, null, null);
		arrearsTable.setSplitLate(false);
		arrearsTable.setHeaderRows(1);
		arrearsTable.setKeepTogether(true);
		ReportUtils.addTable2Cell(arrearsTable, arrearsHeader);
		ReportUtils.addTable2Cell(arrearsTable, arrearsData);
		ReportUtils.addTable2Cell(arrearsTable, arrearsTotal);
		try {
			doc.add(arrearsTable);
		} catch (DocumentException e) {
			Utils.handleException(e);
		}
	  }
	public void initializeRupeeFont() {
		BaseFont bfrupee = null;
		try {
			bfrupee = BaseFont.createFont(rupeeFontDir, BaseFont.IDENTITY_H, true);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		rupee = new Font(bfrupee, 10f, Font.BOLD, Color.BLACK);
	}

	public void initializeHindiFont() {
		BaseFont bfhindi = null;
		try {
			bfhindi = BaseFont.createFont(hindiFontDir, BaseFont.IDENTITY_H, true);
		} catch (Exception e) {
			Utils.handleException(e);
		}
		hindi = new Font(bfhindi, 10f, Font.BOLD, Color.BLACK);
		hindi2 = new Font(bfhindi, 16f, Font.BOLD, Color.BLACK);
	}

	public Arrearcorr(Environment env) {

		String dir = env.getProperty("spring.application.filedir");
		String binDir = env.getProperty("spring.application.bin");
		Path root = Paths.get(dir);
		File f = root.toFile();
		reportsDir = f.getAbsolutePath();
//		System.out.println("=========In Arrears Verification Report ===reportsDir " + reportsDir);
//		System.out.println("reportsDir " + reportsDir);

		Path hindi = Paths.get(binDir + "/shusha.ttf");
		File hindiF = hindi.toFile();
		hindiFontDir = hindiF.getAbsolutePath();

		Path rupee = Paths.get(binDir + "/Rupee.ttf");
		File rupeeF = rupee.toFile();
		rupeeFontDir = rupeeF.getAbsolutePath();

		initializeRupeeFont();
		initializeHindiFont();

	}
	
   }
	class ArrearsDetailsObject {
		String dueQe, dueFromDate, dueToDate,drawnQe, drawnFromDate, drawnToDate, payCode;

		Double dueAmount, drawnAmount;		
		
		public String getDueQe() {
			return dueQe;
		}
		public void setDueQe(String dueQe) {
			this.dueQe = dueQe;
		}
		public String getDueFromDate() {
			return dueFromDate;
		}
		public void setDueFromDate(String dueFromDate) {
			this.dueFromDate = dueFromDate;
		}
		public String getDueToDate() {
			return dueToDate;
		}
		public void setDueToDate(String dueToDate) {
			this.dueToDate = dueToDate;
		}
		public String getDrawnQe() {
			return drawnQe;
		}
		public void setDrawnQe(String drawnQe) {
			this.drawnQe = drawnQe;
		}
		public String getDrawnFromDate() {
			return drawnFromDate;
		}
		public void setDrawnFromDate(String drawnFromDate) {
			this.drawnFromDate = drawnFromDate;
		}
		public String getDrawnToDate() {
			return drawnToDate;
		}
		public void setDrawnToDate(String drawnToDate) {
			this.drawnToDate = drawnToDate;
		}
		public String getPayCode() {
			return payCode;
		}
		public void setPayCode(String payCode) {
			this.payCode = payCode;
		}
		public Double getDueAmount() {
			return dueAmount;
		}
		public void setDueAmount(Double dueAmount) {
			this.dueAmount = dueAmount;
		}
		public Double getDrawnAmount() {
			return drawnAmount;
		}
		public void setDrawnAmount(Double drawnAmount) {
			this.drawnAmount = drawnAmount;
		}
	}
	class EndPageEvent1 extends PdfPageEventHelper {
		protected PdfTemplate template;
		protected BaseFont helv;
		int pageCount = 0;
		int totalPages;
		private String paoName = null;
		private String empName = null;
		private String unitName = null;
		private String task = null;
		private String description = null;
		private String fromDate = null;

		Font f6 = FontFactory.getFont("HELVETICA", 12f, Font.BOLD, Color.BLACK);
		Font f7 = FontFactory.getFont("HELVETICA", 10f, Font.BOLD, Color.BLACK);

		
		public void setPaoName(String paoName) {
			this.paoName = paoName;
		}

		public void setEmpName(String empName) {
			this.empName = empName;
		}

		public void setUnitName(String unitName) {
			this.unitName = unitName;
		}

		public void setTask(String task) {
			this.task = task;
		}

		public void setDescription(String description) {
			this.description = description;
		}
		
		public void setFromDate(String fromDate) {
			this.fromDate = fromDate;
		}
		
		public void onOpenDocument(PdfWriter writer, Document document) {
			PdfContentByte cb = writer.getDirectContent();
			PdfOutline root = cb.getRootOutline();
			root.setOpen(false);
			PdfDestination d1 = new PdfDestination(PdfDestination.FIT);
			PdfAction a1 = null;
			PdfAction a2 = null;
			PdfName a1Name = new PdfName("Arr", true);
			a1 = PdfAction.gotoLocalPage(writer.getPageNumber(), d1, writer);
			template = writer.getDirectContent().createTemplate(100, 100);
			template.setBoundingBox(new Rectangle(-20, -20, 100, 100));
			try {
				helv = FontFactory.getFont("HELVETICA", 8f, Font.BOLD, Color.BLUE).getBaseFont();
			} catch (Exception e) {
				throw new ExceptionConverter(e);
			}
		}

		public void onEndPage(PdfWriter writer, Document document) {
			Font f1 = FontFactory.getFont("HELVETICA", 9f, Font.BOLD, Color.BLACK);
			Font f2 = FontFactory.getFont("HELVETICA", 8f, Font.NORMAL, Color.BLACK);
			pageCount++;
			writer.setViewerPreferences(PdfWriter.PageModeUseOutlines | PdfWriter.FitWindow);
			PdfContentByte cb = writer.getDirectContent();
			PdfOutline root = cb.getRootOutline();
			root.setOpen(false);
			Rectangle page = document.getPageSize();
			cb.saveState();
			String text = "Page " + new Chunk(String.valueOf(writer.getPageNumber()))
							.setLocalDestination(String.valueOf(writer.getPageNumber())) + " of ";
			float textBase = document.top() + 15;
			float textSize = helv.getWidthPoint(text, 8);
			cb.beginText();
			cb.setFontAndSize(helv, 8);
			float adjust = helv.getWidthPoint("0", 8);
			cb.setTextMatrix(document.right() - textSize - adjust, textBase);
			cb.showText(text);
			cb.endText();
			cb.addTemplate(template, document.right() - adjust, textBase);
			cb.restoreState();
			if (writer.getPageNumber() >= 1) {
				PdfPTable head = new PdfPTable(new float[] { 90f });
				head.setWidthPercentage(95f);
				String paoHeader = "PAO : " + this.paoName; 
				String reportHeader = "Details of Arrears Report";
				String empDetails = this.empName;
				PdfPTable pdfTbl = new PdfPTable(new float[]{10f, 20f, 20f, 50f});
				ReportUtils.addCell2Table(Element.ALIGN_LEFT, Element.ALIGN_MIDDLE, pdfTbl, "Task : " + this.task,
						f1, "0", "0", "0", "0", null, null);
				ReportUtils.addCell2Table(Element.ALIGN_LEFT, Element.ALIGN_MIDDLE, pdfTbl, "Description : " + this.description,
						f1, "0", "0", "0", "0", null, null);
				ReportUtils.addCell2Table(Element.ALIGN_LEFT, Element.ALIGN_MIDDLE, pdfTbl, "From Date : " + this.fromDate,
						f1, "0", "0", "0", "0", null, null);
				ReportUtils.addCell2Table(Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE, pdfTbl, "Unit : " + this.unitName,
						f1, "0", "0", "0", "0", null, null);
				PdfPCell pdfCell = null;
				
				pdfCell = new PdfPCell(new Phrase(paoHeader, f6));
				pdfCell.setHorizontalAlignment(Element.ALIGN_CENTER);
				pdfCell.setVerticalAlignment(Element.ALIGN_TOP);
				pdfCell.setBorderWidth(0);
				head.addCell(pdfCell);
				
				pdfCell = new PdfPCell(new Phrase(reportHeader + " in respect of " + empDetails, f7));
				pdfCell.setHorizontalAlignment(Element.ALIGN_CENTER);
				pdfCell.setVerticalAlignment(Element.ALIGN_TOP);
				pdfCell.setBorderWidth(0);
				head.addCell(pdfCell);
				
				pdfCell = new PdfPCell(pdfTbl);
				pdfCell.setHorizontalAlignment(Element.ALIGN_CENTER);
				pdfCell.setVerticalAlignment(Element.ALIGN_TOP);
				pdfCell.setBorderWidth(0);
				head.addCell(pdfCell);
				
				head.setTotalWidth(page.getWidth() - document.leftMargin() - document.rightMargin());

				head.writeSelectedRows(-1, -1, document.leftMargin(), page.getHeight()
						- document.topMargin() + head.getTotalHeight(), writer.getDirectContent());
				writer.setOutlines(null);
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
