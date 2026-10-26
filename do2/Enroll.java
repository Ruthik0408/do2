package itsdc.dolphin.do2;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.models.Acr;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ChangeInRateSub;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeBankDetails;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.EnrollDo2;
import itsdc.dolphin.models.EntryCategory;
import itsdc.dolphin.models.FundSubRefund;
import itsdc.dolphin.models.FundSummary;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.LoanSanction;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.repositories.AcrRepository;
import itsdc.dolphin.repositories.AcrTransCodeRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.ChangeInRateSubRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeBankDetailsRepository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EmployeeServiceDetailRepository;
import itsdc.dolphin.repositories.EnrollDo2Repository;
import itsdc.dolphin.repositories.EntryCategoryRepository;
import itsdc.dolphin.repositories.FundSubRefundRepository;
import itsdc.dolphin.repositories.FundSummaryRepository;
import itsdc.dolphin.repositories.GroupCategoryRepository;
import itsdc.dolphin.repositories.LoanCodeRepository;
import itsdc.dolphin.repositories.LoanSanctionRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.repositories.PayCodeRepository;
import itsdc.dolphin.repositories.PayMatrixRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.RankRepository;
import itsdc.dolphin.repositories.StatusCodeRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Enroll implements java.io.Serializable {
    
    
    private String isValidEnrollDo2 = "failure";
    public StringBuffer buffer = new StringBuffer();
	
	static public final List<String> SPECIAL_ENTRY_CATEGORY = Arrays.asList("SHGD","SHT","DVRMT","DVRSPLVEH","TXRE");
	static public final List<String> COMMON_ENTRY_TRADE = Arrays.asList("HAVEDN", "HAVCLK", "SOLCLK", "SOLGD", "SOLNAS", "SOLTDN","SOLTECH","DPHARMA");
	static public final List<String> AGE_RELAX_CATEGORY = Arrays.asList("RT", "SURVCARTO", "CATER");
	
	static public final List<String> VALID_DSC_JC_CODE = Arrays.asList("JC","TJ","RV");
	static public final List<String> VALID_JC_ENTRY_CATEGORY_SUBSTRING = Arrays.asList("NSUB", "SUBE", "SMAJ", "DEJCO", "DERTJCO","SPNBSUB" , "TAJCOECO", "TAJCOREG");//SPNBSUB Added for New Enroll Do2
	static public final List<String> VALID_GSO_CODE = Arrays.asList("N", "S", "G");
	
	static public final String INVALID_ARMYNO = "Army No should consist 8 digits \n ";
	static public final String TOE_NOT_VALID = "Invalild Terms of engagement\n ";
	static public final String AGE_NOT_VALID = "Invalid Age. \n ";
	static public final String DO2_DATE_REQUIRED = "Do2 Date is required.\n ";
	static public final String DOE_NOT_VALID = "Date of Enrollment Beyond Current Quarter.\n";
	static public final String ENROLL_CATEGORY_NOT_VALID = "Invalid Enrollment Category.\n";
	static public final String ARMYNO_NOT_CONTINUOUS = "Previous Army No. in the series does not exist.\n";
	static public final String DUPLICATE_ARMYNO = "Army No. already exists.\n";
	static public final String INVALID_CHECK_DIGIT = "Invalid check digit entered. \n";
	static public final String DO2_TYPE_NOT_VALID = "Invalid Do2 Type. \n";
	static public final String VALID_GSO_CODE_REQUIRED = "Valid GSO Code Required.\n";
	static public final String INVALID_UNITCODE = "Invalid unitcode. \n";
	static public final String INVALID_DATE_OF_ARRIVAL = "Date of arrival cannot be before date of enrollment. \n";
	static public final String INVALID_ARMYNO_FOR_JC = "Invalid Army number given for JCO entry. \n";
	static public final String DUPLICATE_DO2 = "Duplicate do2. \n";
	static public final String INVALID_ENTRY_TRADE = "Invalid entry trade. \n";
	static public final String GRATUITY_WILLINGNESS_AMOUNT_MISMATCH = "Gratuity willingness opted but invalid amount given or vice versa. \n";
	static public final String COMMUTATION_WILLINGNESS_AMOUNT_MISMATCH = "Commutation willingness opted but invalid amount given or vice versa. \n";
	static public final String INVALID_ENTRY_CATEGORY = "For entry trade - SOLTECH, entry category required is MER and for SOLGD, NER \n";
	static public final String INVALID_ENTRY_CATEGORY_FOR_DSC_NCO = "Valid DSC entry category not given  \n";
	static public final String INVALID_ENTRY_CATEGORY_FOR_AEC = "Entry category should be HAVEDN for INSTR, INSTRSPL  \n";
	static public final String INVALID_ENTRY_CATEGORY_NON_DSC = "DSC entry category given for a non DSC soldier \n";
	static public final String INVALID_ENTRY_CATEGORY_DEJC = " For entry Category DERTJCO, NSUB-Y entry trade should be RT '\n";
	//Added for NEW ENROLL Do2
	static public final String MISMATCH_IN_RANK_AND_CATEGORY = " Mis-match in Rank and Entry Category'\n";
	static public final List<String> RECT_ENTRY_CATEGORY = Arrays.asList("SOLCLK", "SOLGD", "SOLNASS", "SOLTDN","SOLTECH","SKT");
	static public final List<String> HAV_ENTRY_CATEGORY = Arrays.asList("HAVEDN", "HAVCLK", "HAVSAC","SPHAV");
	static public final List<String> NBSUB_ENTRY_CATEGORY = Arrays.asList("DERTJCO", "DEJCO", "SPNBSUB", "TAJCOECO","TAJCOREG");

	static public final String VERY_OLD_ENROLLMENT_DATE_FOR_TA = "Enrollment Date Prior To TA Implementation Date. \n";
	static public final String FIVE_YEAR_OLD_ENROLLMENT_DATE = "Enrollment Date Is Nearly Five Year Old. \n";
	static public final String SEVEN_YEAR_OLD_ENROLLMENT_DATE_TA = "Enrolment Date is Nearly Seven Year Old For TA. \n";
	
	Employee emp = null;
	List<Allowance> allowanceRate;
	List<Rank> rankList;
	Rank rank;
	int basicPay, gradePay;
  
	AuditCage enrollAuditCage = new AuditCage();
	boolean armyNoExists = false;

	static public Date revised_appxj_impl_date ;
	
	RankRepository rankRepo;
	Do2Repository do2Repo;
	EnrollDo2Repository enrollDo2Repo;
	EntryCategoryRepository entryCatRepo;
	GroupCategoryRepository gcRepo;
	UnitRepository unitRepo;
	EmployeeRepository empRepo;
	StatusCodeRepository scRepo;
	EmployeeServiceDetailRepository esdRepo;
	EmployeeBankDetailsRepository ebdRepo;
	PayCodeRepository pcRepo;
	ArrearsRepository arrRepo;
	EmployeeDeductionRepository edRepo;
	ChangeInRateSubRepository rateSubRepo;
	FundSubRefundRepository fsrRepo;
	FundSummaryRepository  fsRepo;
	LoanSanctionRepository lsRepo;
	LoanCodeRepository lcRepo;
	AuditCageRepository acRepo;
	AcrRepository acrRepo;
	AcrTransCodeRepository acrTransCodeRepo;
	PayScaleRepository psRepo;
	PayMatrixRepository payMatrixRepo;
	MiscRepository miscRepo;
	
	public Employee getEmp() {
		return emp;
	}

	public void setEmp(Employee emp) {
		this.emp = emp;
	}

	public List<Allowance> getAllowanceRate() {
		return allowanceRate;
	}

	public void setAllowanceRate(List<Allowance> allowanceRate) {
		this.allowanceRate = allowanceRate;
	}

	public RankRepository getRankRepo() {
		return rankRepo;
	}

	public void setRankRepo(RankRepository rankRepo) {
		this.rankRepo = rankRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public EnrollDo2Repository getEnrollDo2Repo() {
		return enrollDo2Repo;
	}

	public void setEnrollDo2Repo(EnrollDo2Repository enrollDo2Repo) {
		this.enrollDo2Repo = enrollDo2Repo;
	}

	public EntryCategoryRepository getEntryCatRepo() {
		return entryCatRepo;
	}

	public void setEntryCatRepo(EntryCategoryRepository entryCatRepo) {
		this.entryCatRepo = entryCatRepo;
	}

	public GroupCategoryRepository getGcRepo() {
		return gcRepo;
	}

	public void setGcRepo(GroupCategoryRepository gcRepo) {
		this.gcRepo = gcRepo;
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

	public EmployeeServiceDetailRepository getEsdRepo() {
		return esdRepo;
	}

	public void setEsdRepo(EmployeeServiceDetailRepository esdRepo) {
		this.esdRepo = esdRepo;
	}

	public EmployeeBankDetailsRepository getEbdRepo() {
		return ebdRepo;
	}

	public void setEbdRepo(EmployeeBankDetailsRepository ebdRepo) {
		this.ebdRepo = ebdRepo;
	}

	public PayCodeRepository getPcRepo() {
		return pcRepo;
	}

	public void setPcRepo(PayCodeRepository pcRepo) {
		this.pcRepo = pcRepo;
	}

	public ArrearsRepository getArrRepo() {
		return arrRepo;
	}

	public void setArrRepo(ArrearsRepository arrRepo) {
		this.arrRepo = arrRepo;
	}

	public EmployeeDeductionRepository getEdRepo() {
		return edRepo;
	}

	public void setEdRepo(EmployeeDeductionRepository edRepo) {
		this.edRepo = edRepo;
	}

	public ChangeInRateSubRepository getRateSubRepo() {
		return rateSubRepo;
	}

	public void setRateSubRepo(ChangeInRateSubRepository rateSubRepo) {
		this.rateSubRepo = rateSubRepo;
	}

	public FundSubRefundRepository getFsrRepo() {
		return fsrRepo;
	}

	public void setFsrRepo(FundSubRefundRepository fsrRepo) {
		this.fsrRepo = fsrRepo;
	}

	public FundSummaryRepository getFsRepo() {
		return fsRepo;
	}

	public void setFsRepo(FundSummaryRepository fsRepo) {
		this.fsRepo = fsRepo;
	}

	public LoanSanctionRepository getLsRepo() {
		return lsRepo;
	}

	public void setLsRepo(LoanSanctionRepository lsRepo) {
		this.lsRepo = lsRepo;
	}

	public LoanCodeRepository getLcRepo() {
		return lcRepo;
	}

	public void setLcRepo(LoanCodeRepository lcRepo) {
		this.lcRepo = lcRepo;
	}

	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public AcrRepository getAcrRepo() {
		return acrRepo;
	}

	public void setAcrRepo(AcrRepository acrRepo) {
		this.acrRepo = acrRepo;
	}

	public AcrTransCodeRepository getAcrTransCodeRepo() {
		return acrTransCodeRepo;
	}

	public void setAcrTransCodeRepo(AcrTransCodeRepository acrTransCodeRepo) {
		this.acrTransCodeRepo = acrTransCodeRepo;
	}

	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
	}

	public PayMatrixRepository getPayMatrixRepo() {
		return payMatrixRepo;
	}

	public void setPayMatrixRepo(PayMatrixRepository payMatrixRepo) {
		this.payMatrixRepo = payMatrixRepo;
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
	
	public boolean isValidEdo2(EnrollDo2 edo2) {
		
		//unit code is required to check whether TA or Not.Validation introduced
		if (edo2.getUnitCode() ==null || !unitCodeExists(edo2)){			
			edo2.setReason(INVALID_UNITCODE);
			edo2.setStatus(Do2Manager.INVALID);
			return false;
		}
		
	/*	//to ensure the unit code startwith TA for TA IRLAs
		if (edo2.getTaAppointmentCategory() != null ){
			if (!edo2.getUnitCode().startsWith("TA")){
				edo2.setReason("UnitCode should start with TA for TA enrolment");
				edo2.setStatus(Do2Manager.INVALID);
				return false;
			}
		} else {
			if (edo2.getUnitCode().startsWith("TA")){
				edo2.setReason("UnitCode should not start with TA for Regular Army");
				edo2.setStatus(Do2Manager.INVALID);
				return false;
			}
		}*/
		
		
		//Modified for NEW ENROLL Do2----29052021
		//to ensure the unit code startwith TA for TA IRLAs
		if (edo2.getUnitCode().startsWith("TA")){
			if (!isTaEntryCategory(edo2)){
				edo2.setReason("UnitCode should start with TA for TA enrolment");
				edo2.setStatus(Do2Manager.INVALID);
				return false;
			}
		} else {
			if (edo2.getUnitCode().startsWith("TA")){
				edo2.setReason("UnitCode should not start with TA for Regular Army");
				edo2.setStatus(Do2Manager.INVALID);
				return false;
			}
		}
			
		rank = rankRepo.findByRankCode(Do2Manager.RECRUIT); 
		 
		boolean validity = true;
		//enroll do2 published by unit for TA not by record office.To exclude TA code included
		if(!edo2.getUnitCode().startsWith("TA") && !edo2.getDo2Type().equals("3")){
			validity = false;
			buffer.append(DO2_TYPE_NOT_VALID);
		}else {
			validity=true;
		}
		
		if (edo2.getArmyNo().trim() != null) {
			if (edo2.getArmyNo().trim().length() != 8 ) {
				validity = false;
				buffer.append(INVALID_ARMYNO);
			}
		}
		// CGDA letter No. AT/I/3510/MPS/Jabalpur dated 07.08.2014 for re-enrolled cases allotted same 
		// army no.
		if (armyNoExists(edo2.getArmyNo())) {
			validity = false;			
		}
		
		
		if (!(Utils.getCheckDigit(edo2.getArmyNo())+"").equals(edo2.getCheckDigit())) {
			validity = false;
			buffer.append(INVALID_CHECK_DIGIT);
		}
	 
		if (isDuplicate(edo2)) {
			validity = false;
			buffer.append(DUPLICATE_DO2);
		}
		//Modified for NEW ENROLL Do2----29052021
	/*	if(isMismatchRankAndCategory(edo2)) {
		validity=false;
		buffer.append(MISMATCH_IN_RANK_AND_CATEGORY);
		
		}*/
		
		if (!isValidCategory(edo2)) {
			validity = false;
			buffer.append(ENROLL_CATEGORY_NOT_VALID);
		}
		
		if (!isValidDateOfEnroll(edo2)) {
			validity = false;
			buffer.append(DOE_NOT_VALID);
		}
		
		if (!isValidDoB(edo2)) {
			validity = false;
			edo2.setAgeIndicator(Do2Manager.ENROLL_AGE_INDICATOR+"");
			buffer.append(AGE_NOT_VALID);
		}
		
		/*if (!isValidTermsOfEngagement(edo2, rank)) {
			validity = false;
			buffer.append(TOE_NOT_VALID);
		}*/
		
		if (edo2.getDateOfArrival().before(edo2.getEnrollmentDate())) {
			validity = false;
			buffer.append(INVALID_DATE_OF_ARRIVAL);
		}
		
		if (!isValidArmyNoForJC(edo2)) {
			validity = false;
			buffer.append(INVALID_ARMYNO_FOR_JC);
		}
		
		if (edo2.getGsoCode()==null || 
				(edo2.getGsoCode()!=null && !VALID_GSO_CODE.contains(edo2.getGsoCode()+""))) {
			validity = false;
			buffer.append(VALID_GSO_CODE_REQUIRED);
		}

		
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (!edo2.getDo2Date().after(revised_appxj_impl_date)){
			if ((edo2.getDaDays() != null && edo2.getDaDays()>0) || (edo2.getDaHours() != null && edo2.getDaHours() > 0)) {
				if (edo2.getCcaCode() == null){
					validity = false;
					buffer.append("Class of city not shown");
				}
				if (!isRateAvailable(edo2)) {
					validity = false;
					buffer.append(Do2Manager.RATE_NOT_AVAILABLE);
				}
			}
		}
	//Commented from Line No:243 to 247 --- for New Enroll Do2			
	/*	if (edo2.getEntryTrade()==null || 
				(edo2.getEntryTrade()!=null && !isValidEntryTrade(edo2))) {
			validity = false;
			buffer.append(INVALID_ENTRY_TRADE);
		}*/
		
		/*if (!(edo2.getArmyNo().startsWith("TJ") || edo2.getArmyNo().startsWith("JC"))) {
			if (edo2.getEntryTrade()==null || ((edo2.getEntryTrade().equals("SOLGD") && 
					!(edo2.getEntryCategory().equals("NER") || edo2.getEntryCategory().equals("NORMAL")))
					|| (edo2.getEntryTrade().equals("SOLTECH") && 
							!(edo2.getEntryCategory().equals("MER") || edo2.getEntryCategory().equals("MATRIC"))))) {
				validity = false;
				buffer.append(INVALID_ENTRY_CATEGORY);
			}
		}*/
		
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (edo2.getArmyNo().startsWith("JC")) {
				if (!(edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") || edo2.getEntryCategory().equals("SPNBSUB"))) {
						validity=false;
						buffer.append(INVALID_ENTRY_CATEGORY_DEJC);
					}
				}
			
		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_AEC)) { //AEC PACHCHMARHI
			if (edo2.getEntryCategory() != null &&	!(edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))){
				validity = false;
			    buffer.append(INVALID_ENTRY_CATEGORY_FOR_AEC);
			}
		}
		/*if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (edo2.getEntryCategory()==null || (edo2.getEntryCategory()!=null 
					&& (edo2.getEntryCategory().equals("SPHV-Y") || edo2.getEntryCategory().equals("SPHV-Z"))
					&& (!Do2Manager.getDscRecruitCategoryMap().containsKey(edo2.getEntryCategory()) 
							|| !Do2Manager.getRecruitCategoryMap().containsKey(edo2.getEntryCategory())))) {
				validity = false;
				buffer.append(INVALID_ENTRY_CATEGORY_FOR_DSC_NCO);
			}
			
			if ((edo2.getCommutationWillingness() && (edo2.getCommutationAmount()==null || edo2.getCommutationAmount()<=0))
					|| (!edo2.getCommutationWillingness() && edo2.getCommutationAmount()!=null && edo2.getCommutationAmount()>0)) {
				validity = false;
				buffer.append(COMMUTATION_WILLINGNESS_AMOUNT_MISMATCH);
			}
			if ((edo2.getGratuitySurrenderedDsc() != null && edo2.getGratuitySurrenderedDsc() == 'Y' && 
					(edo2.getGratuityAmount()==null || edo2.getGratuityAmount()<=0))
					|| (edo2.getGratuitySurrenderedDsc() != null && edo2.getGratuitySurrenderedDsc() == 'N' 
					 	&& edo2.getGratuityAmount()!=null && edo2.getGratuityAmount()>0)){
				validity = false;
				buffer.append(GRATUITY_WILLINGNESS_AMOUNT_MISMATCH);
			}
			
		} else if (edo2.getEntryCategory()==null || (edo2.getEntryCategory()!=null 
					&& Do2Manager.getDscRecruitCategoryMap().containsKey(edo2.getEntryCategory()))) { 
			validity = false;
			buffer.append(INVALID_ENTRY_CATEGORY_NON_DSC);
		}*/
		
		//Modified for New Enroll
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (edo2.getEntryCategory()==null || (edo2.getEntryCategory()!=null 			
					&& (!Do2Manager.getDscRecruitCategoryMap().containsKey(edo2.getEntryCategory())))) //Changed here from above
							 {
				validity = false;
				buffer.append(INVALID_ENTRY_CATEGORY_FOR_DSC_NCO);
			}
			
			if ((edo2.getCommutationWillingness() && (edo2.getCommutationAmount()==null || edo2.getCommutationAmount()<=0))
					|| (!edo2.getCommutationWillingness() && edo2.getCommutationAmount()!=null && edo2.getCommutationAmount()>0)) {
				validity = false;
				buffer.append(COMMUTATION_WILLINGNESS_AMOUNT_MISMATCH);
			}
			if ((edo2.getGratuitySurrenderedDsc() != null && edo2.getGratuitySurrenderedDsc().equals("Y") && 
					(edo2.getGratuityAmount()==null || edo2.getGratuityAmount()<=0))
					|| (edo2.getGratuitySurrenderedDsc() != null && edo2.getGratuitySurrenderedDsc().equals("N") 
					 	&& edo2.getGratuityAmount()!=null && edo2.getGratuityAmount()>0)){
				validity = false;
				buffer.append(GRATUITY_WILLINGNESS_AMOUNT_MISMATCH);
			}
			
		} else if (edo2.getEntryCategory()==null || (edo2.getEntryCategory()!=null 
				&& Do2Manager.getRecruitCategoryMap().containsKey(edo2.getEntryCategory()))) { //Changed to RecruitCategoryMap
				validity = false;
				buffer.append(INVALID_ENTRY_CATEGORY_NON_DSC);
			}
		
		// added on 24.06.2024AN
		if (edo2.getUnitCode()!=null && edo2.getUnitCode().trim().toUpperCase().startsWith("TA") 
				&& edo2.getEnrollmentDate()!=null && !edo2.getEnrollmentDate().after(DateManager.getTaCutOffDate())) {
			validity = false;
			buffer.append(VERY_OLD_ENROLLMENT_DATE_FOR_TA);
		}
		
		// added on 24.06.2024AN
	//	if (edo2.getEnrollmentDate()!=null
	//			&& DateManager.getNoOfDays(edo2.getEnrollmentDate(), DateManager.getToday())>1800) {
	//		validity = false;
	//		buffer.append(FIVE_YEAR_OLD_ENROLLMENT_DATE);
	//	}
		
		if (edo2.getTaAppointmentCategory()!=null || edo2.getUnitCode().trim().toUpperCase().startsWith("TA")  ) {
			if (DateManager.getNoOfDays(edo2.getEnrollmentDate(), DateManager.getToday())>2555) {
				validity = false;
				buffer.append(SEVEN_YEAR_OLD_ENROLLMENT_DATE_TA);
			}
		} else {
			 if (DateManager.getNoOfDays(edo2.getEnrollmentDate(), DateManager.getToday())>1900)  {
				 validity = false;
					buffer.append(FIVE_YEAR_OLD_ENROLLMENT_DATE);
			 }
		}
		
		
		edo2.setReason(buffer.toString());
		
		if (validity) {
			edo2.setStatus(Do2Manager.VALID);
			edo2.setReason(null);
		} else {
			edo2.setStatus(Do2Manager.INVALID);
		}
		
		enrollDo2Repo.save(edo2);  
		
		return validity;
	}

	public String process(EnrollDo2 edo2) {
		System.out.println("......... landed in enroll.......");
		if (isValidEdo2(edo2))  {
			if (edo2.getUsr()!=null && edo2.getUsr().getDesignation().getLevel() <= 30){
				edo2.setStatus(Do2Manager.PENDING);
				edo2.setReason("TO BE APPROVED");
			} else if (edo2.getUsr()!=null && edo2.getUsr().getDesignation().getLevel() > 30){
				edo2.setStatus(Do2Manager.VALID);
				edo2.setApproved(true);
				edo2.setReason(null);
			}
		}
	 	if (edo2.getStatus().equals(Do2Manager.VALID))
			isValidEnrollDo2 = commitEnrollDo2(edo2);
		
		return isValidEnrollDo2;
	} 

	public String processEnrollDo2FromBatch(EnrollDo2 edo2) {
		System.out.println("............... edo2 army no "+edo2.getArmyNo());
		if (isValidEdo2(edo2))  {
			if (edo2.getUsr()!=null && edo2.getUsr().getDesignation().getLevel() > 30){
				edo2.setStatus(Do2Manager.VALID);
				edo2.setApproved(true);
				edo2.setReason(null);
			}
		}
		if (edo2.getStatus().equals(Do2Manager.VALID)) {
			isValidEnrollDo2 = commitEnrollDo2(edo2);
		}
		System.out.println("... isvalidenrold2  " +isValidEnrollDo2 +" for "+edo2.getArmyNo());
		return isValidEnrollDo2;
	} 

	
	public boolean armyNoExists(String armyNo) {
		Employee employee = Utils.getCurrentEmployee(armyNo);
  		
  		if (employee == null){
  			return false;
  		} else if (employee.getStatusCode().getId() > 4){
  			buffer.append(" Please check this Army number was already discharged ");
  		//	FacesMessages.instance().add(" Please check this Army number was already discharged ");
  			armyNoExists=true;
  			return false;
  		}
  		buffer.append(DUPLICATE_ARMYNO);
  		return true;
	}
 	
	public boolean isDuplicate(EnrollDo2 edo2) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(edo2.getDo2Date());
		int year = cal.get(Calendar.YEAR);
		List<EnrollDo2> enrollDo2List =enrollDo2Repo.findByUnitCodeDo2TypeDo2NoItemNoIntYear(edo2.getUnitCode(), edo2.getDo2Type(), edo2.getId(), edo2.getDo2No(), edo2.getDo2ItemNo(), year); 
				
		 
		
		return (enrollDo2List.size()>0);
	}
	
	/*public boolean previousArmyNoExists(String armyno) {
		String ano = String.valueOf((Integer.parseInt(armyno) - 1));
		List<Employee> emp = em.createQuery("select emp from Employee emp where emp.armyNo=:armyno")
				.setParameter("armyno", ano).getResultList();
		
		if (emp.size() > 0) 
			if (emp.get(0).getArmyNo().equals(ano))  
				return true;
		 
		return false;
	}*/
	
	
	//Newly Introduced for New Enroll Do2
	/*	public boolean isMismatchRankAndCategory(EnrollDo2 edo2) {
			
			if(edo2.getEntryRank() != null) {
				if(edo2.getEntryRank().equals("RECT") && RECT_ENTRY_CATEGORY.contains(edo2.getEntryCategory())) {
					return true;
				}
				
				if(edo2.getEntryRank().equals("HAV") && HAV_ENTRY_CATEGORY.contains(edo2.getEntryCategory())) {
					return true;
				}
				
				if(edo2.getEntryRank().equals("NBSUB") && NBSUB_ENTRY_CATEGORY.contains(edo2.getEntryCategory())) {
					return true;
				}
				
			}
			
			return false;
		}*/

	/*public boolean isValidCategory(EnrollDo2 edo2) {
		
		if (edo2.getEntryCategory()!=null) {
			String entryCategoryString = edo2.getEntryCategory();
			entryCategoryString.replaceAll("(?:\\r\\n|\\n\\r|\\n|\\r)", "");
			edo2.setEntryCategory(entryCategoryString.trim());
		}
		
		if (edo2.getEnrollmentDate()!=null && !edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
			if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().substring(edo2.getEntryCategory().length()-1).equals("Z")) {
				return false;
			}
		}
		
		if (edo2.getArmyNo().startsWith("JC")) {
			if(!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){
				if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().equals("NSUB-Y")
						&& edo2.getEntryTrade()!=null && !edo2.getEntryTrade().equals("RT")) {
					return false;
				}
			}
		}
		
		if (edo2.getArmyNo().startsWith("JC")) {
			if(!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){
				if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().equals("DERTJCO") 
						&& edo2.getEntryTrade()!=null && !edo2.getEntryTrade().equals("RT")) {
					return false;
				}
			}
		}
		
		List<EntryCategory> ec = em.createQuery("select ec from EntryCategory ec where ec.entryCategory=:entcat")
									.setParameter("entcat", edo2.getEntryCategory().toUpperCase().trim()).getResultList();

		if (ec.size() > 0) 
			return true;
		
		return false;
	}*/
	
	//Modified for New Enroll Do2	
	public boolean isValidCategory(EnrollDo2 edo2) {
		
		if (edo2.getEntryCategory()!=null) {
			String entryCategoryString = edo2.getEntryCategory();
			entryCategoryString.replaceAll("(?:\\r\\n|\\n\\r|\\n|\\r)", "");
			edo2.setEntryCategory(entryCategoryString.trim());
		}
		
		if (edo2.getEnrollmentDate()!=null && !edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
			if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().substring(edo2.getEntryCategory().length()-1).equals("Z")) {
				return false;
			}
		}
		
		if (edo2.getArmyNo().startsWith("JC")) {
			if(!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){
				if (!(edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") || edo2.getEntryCategory().equals("SPNBSUB"))) {
					return false;
				}
			}
		}
		//Commented for New ENROLL Do2----29052021
	/*	if (edo2.getArmyNo().startsWith("JC")) {
			if(!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){
				if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().equals("DERTJCO") 
						&& edo2.getEntryTrade()!=null && !edo2.getEntryTrade().equals("RT")) {
					return false;
				}
			}
		}*/
		
		EntryCategory ec =entryCatRepo.findByEntryCategory(edo2.getEntryCategory().toUpperCase().trim());
		
		 
		if (ec!=null) 
			return true;
		
		return false;
	}

	//Added for New ENROLL Do2----29052021
	public boolean isTaEntryCategory(EnrollDo2 edo2) {
			
			if (edo2.getEntryCategory()!=null) {
				String entryCategoryString = edo2.getEntryCategory();
				entryCategoryString.replaceAll("(?:\\r\\n|\\n\\r|\\n|\\r)", "");
				edo2.setEntryCategory(entryCategoryString.trim());
			}
			
			// The following lines commented on 19-09-2024
			/*
			if (edo2.getEnrollmentDate()!=null && !edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
				if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals("TAREG") || edo2.getEntryCategory().equals("TADEPT") || edo2.getEntryCategory().equals("TAECO"))) {
					return true;
				}
			}*/
			
			
			
			EntryCategory ec =entryCatRepo.findByEntryCategory(edo2.getEntryCategory().toUpperCase().trim());
				 

			if (ec!=null) 
				return true;
			
			return false;
	}
	
	public boolean isValidDateOfEnroll(EnrollDo2 edo2) {
		if (edo2.getEnrollmentDate()!=null) {
			if (!edo2.getEnrollmentDate().after(DateManager.getCurrentDate()) || !edo2.getEnrollmentDate().after(edo2.getDo2Date())) {
				return true;
			}
		}
		return false;
	}

	public boolean isValidDoB(EnrollDo2 edo2) {
		if (edo2.getDateOfBirth()==null) { 
			return false;
		}
		
		if (edo2.getEnrollmentDate()!=null && edo2.getEnrollmentDate().before(edo2.getDateOfBirth())) {    // added on 11.01.2024
			return false;
		}
		
		if (edo2.getOverAgeExemption()==true && edo2.getRemarks()!=null && edo2.getRemarks().length()>5) { // added on 11.01.2024
	       	return true;
	    }
	        
		double empAge = DateManager.getNoOfMonths(edo2.getDateOfBirth(), edo2.getEnrollmentDate());
		
		if (edo2.getUnitCode()!=null && edo2.getUnitCode().trim().startsWith("TA")){
			return (empAge>=192 && empAge<=540);
		}
		
		if (edo2.getEntryCategory().trim().equals("DERTJCO") || edo2.getEntryTrade().trim().equals("RT")) {
			 // AG Branch letter No.B/08347/Policy/MP-3 dated 21.08.2019 on Procedure of recruitment/selection and
			// trg of Religious Teachers   Modified on 25.02.2025.  Minimum Age reduced to 25 years from 27 years.
			if (edo2.getEnrollmentDate()!=null) {
				try {
					if (edo2.getEnrollmentDate().before(DateManager.getDate("2019-08-21"))) {
						return (empAge>=324 && empAge<=408);
					} else {
						return (empAge>=300 && empAge<=408); 	
					}
				} catch (Exception e) {
					Utils.handleException(e);
				}				
			}			
		}
		
		if (edo2.getEntryCategory().trim().equals("HAVEDN") && AGE_RELAX_CATEGORY.contains(edo2.getEntryTrade())){
			return (empAge>=192 && empAge<=312);
		}
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			//return (empAge>=408 && empAge<=660);                       // validation introduced on 11.01.2024 -- age > 34y and < 55y
			return (empAge>240 && empAge<=660);   						 // validation modified on 21.02.2024 as PAO informed that TA personnel are reported after 2/3 years of service also -- age > 20y and < 55y
		}
		
		return (empAge>=192 && empAge<=300); 
		
		/*
		 * 
		 if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) 
				|| VALID_DSC_JC_CODE.contains(edo2.getArmyNo().substring(0,2))) {
			return true;
		}
		List<GroupCategory> gcList =gcRepo.findBySoldierCategory(edo2.getEntryTrade().trim());
		GroupCategory gc=null;
		if (gcList!=null && gcList.size()>0) {
			gc=gcList.get(0);
		}
		
		if (gc!=null) {  
			if (gc.getSoldierGroup().trim().equals("Z")) {  
				return (empAge>=192 && empAge<=360);
			}
		}	// validation commented on 11.01.2024 as Z group does not exist now
		
		try { // validation removed on 07.08.2023 as requested vide Grenadiers email dated 17.05.2023.
			if (edo2.getArmyNo().equals("02720029") 
					&& edo2.getDateOfBirth().equals(DateManager.getDate("1996-10-31"))) {
				return true;
			}
			// Validation removed on 28.08.2023 as requested by RVC Meerut as enrollment during covid19 period and exemption exists for the same.
			if (edo2.getArmyNo().equals("07246296") || edo2.getArmyNo().equals("07246153")
					|| edo2.getArmyNo().equals("07246130") || edo2.getArmyNo().equals("07246177")) {
				return true;
			}
			// Validation removed on 24.11.2023 as requested by AMC Lucknow vide email dated 23.11.2023
			if (Utils.getPao().getPaoCode().equals("85")) {
				if (edo2.getArmyNo().equals("22057047") || edo2.getArmyNo().equals("22057048")
						|| edo2.getArmyNo().equals("22057049") || edo2.getArmyNo().equals("22057155")
						|| edo2.getArmyNo().equals("22057156") || edo2.getArmyNo().equals("22057460")
						|| edo2.getArmyNo().equals("22057512") || edo2.getArmyNo().equals("22057513")
						|| edo2.getArmyNo().equals("22057514") || edo2.getArmyNo().equals("22057515")
						|| edo2.getArmyNo().equals("22057516") || edo2.getArmyNo().equals("22057517")
						|| edo2.getArmyNo().equals("22057518") || edo2.getArmyNo().equals("22057519")
						|| edo2.getArmyNo().equals("22057520") || edo2.getArmyNo().equals("22057521")
						|| edo2.getArmyNo().equals("22058062") || edo2.getArmyNo().equals("22058063")
						|| edo2.getArmyNo().equals("22058385") || edo2.getArmyNo().equals("22058386")) {
					return true;
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
        // TA appointment category O or D exclude from the validataion
        if (edo2.getUnitCode().startsWith("TA")){
        	 if (edo2.getTaAppointmentCategory()!=null && (edo2.getTaAppointmentCategory()=='D' || 
        		 edo2.getTaAppointmentCategory()=='O'))
        		 return true;
        }
		// TA appointment category O or D exclude from the validataion
        if (edo2.getUnitCode()!=null && edo2.getUnitCode().trim().startsWith("TA")) {
        	if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals("TAREG") || edo2.getEntryCategory().equals("TADEPT") || edo2.getEntryCategory().equals("TAECO")))
       		 return true;
       } */  // This validation commented on 11.01.2024        
      
		/*
		// Dolphin.ear file sent on 29.08.2019 for 39GTC.
		if (Utils.getPao().getPaoCode().equals("78")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}		
		// Dolphin.ear file sent on 20-09-2019 for RRRC 
		if (Utils.getPao().getPaoCode().equals("62")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}		
		
		// Dolphin.ear file sent on 25-11-2019 for KRC Ranikhet 
		if (Utils.getPao().getPaoCode().equals("69")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}	
		
		// Dolphin.ear file sent on 09.12.2019 for GRC Lansdowne 
		if (Utils.getPao().getPaoCode().equals("68")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}
		
		// class file sent on 26.07.2022 for JAKLI Srinagar 
		if (Utils.getPao().getPaoCode().equals("99")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}
				
		// class file sent on 18.08.2022 for DRC Faizabad 
		if (Utils.getPao().getPaoCode().equals("67")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}
				
		// class file sent on 25.10.2022 for ARC Shillong 
		if (Utils.getPao().getPaoCode().equals("70")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}
				
		// class file sent on 27.10.2022 for RRC Fatehgarh
		if (Utils.getPao().getPaoCode().equals("63")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}
		
		//TA personnel Regular can be enrolled upto 35 years of age and re-employed personnel upto 45 years
		//(ecological) - Addl. Dte Gen TA GS Branch No.68640/Eco/R/TA-2 dated 17.03.1994                       
		if (edo2.getUnitCode().startsWith("TA")){
			return empAge >=192 && empAge <=540;
		}
		
		// commented above lines on request of PAO SAGAR and modified as stated below in the month of 10/2018
		// due to sanction of age relaxation of army number 30052089Y ( he is overaged by 4 months and 2 days)
		// and sent patch to only PAO SAGAR on 24-10-2018
	
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_AEC)) {
		  	if (edo2.getUnitCode().startsWith("TA")){
					return empAge >=192 && empAge <=552;
				}
		}		
		*/  // PAOwise validation commented on 11.01.2024.		
		
		//TA personnel Regular can be enrolled upto 35 years of age and re-employed personnel upto 45 years
				//(ecological) - Addl. Dte Gen TA GS Branch No.68640/Eco/R/TA-2 dated 17.03.1994	
				
		// due to sanction of age relaxation of army number 14881198Y SABIBAR RAHMAN  ( he is overaged by 2 months and 7 days)
		// PAO ASC SOUNT sent mail on 09-05-2019 for relaxation of age on GAUHATI Court Order hence following 
		// amendment made and sent to PAO ASC SOUTH only on 10-05-2019 and sent patch to only PAO SAGAR on 24-10-2018
	
		/*if (Utils.getPao().getPaoCode().equals("82")  && edo2.getArmyNo().equals("14881198")) {
			return (empAge >=192 && empAge <=324); 
		}*/
		
		// as per GOI letter received from PAO (ARC)Shillong through email dated 13-08-2019 the following
		//changed made in the program and sent to PAO(ARC) shillong only on 14-08-2019
		
		/*if (Utils.getPao().getPaoCode().equals("70")  && edo2.getArmyNo().equals("30006587")) {
			return (empAge >=192 && empAge <=564); 
		}
		if (Utils.getPao().getPaoCode().equals("70")  && edo2.getArmyNo().equals("30006602")) {
			return (empAge >=192 && empAge <=552); 
		}*/
		
		/*if (Utils.getPao().getPaoCode().equals("78")  && edo2.getUnit().getUnitCode() != null && edo2.getUnit().getUnitName().contains("Ecological")) {
			return (empAge > 192 && empAge <= 600);
		}*/		
	}

	public boolean isValidTermsOfEngagement(EnrollDo2 edo2, Rank rank) {
	 
		if (SPECIAL_ENTRY_CATEGORY.contains(edo2.getEntryCategory())) {
			if (edo2.getTermsColourService().equals("17") && edo2.getTermsMaxAgeRetention().equals("42")) {
				return true;
			}
		} 
	 
		if (edo2.getEntryCategory().length()>4 && VALID_JC_ENTRY_CATEGORY_SUBSTRING.contains(edo2.getEntryCategory().substring(0,5))
				&& edo2.getArmyNo().substring(0,2).equals("JC")){
			if (Integer.parseInt(edo2.getTermsMaxAgeRetention())>=40 && Integer.parseInt(edo2.getTermsMaxAgeRetention())<=52){
				return true;
			}
		}
		//if enroll as TJ (RT ) of TA personnel- this validation will take care off it
		if (edo2.getEntryCategory().length()>4 && VALID_JC_ENTRY_CATEGORY_SUBSTRING.contains(edo2.getEntryCategory().substring(0,5))
				&& edo2.getArmyNo().substring(0,2).equals("TJ")){
			if (Integer.parseInt(edo2.getTermsMaxAgeRetention())>=40 && Integer.parseInt(edo2.getTermsMaxAgeRetention())<=52){
				return true;
			}
		}
			    
		if (rank!=null) {
			if ((Integer.parseInt(edo2.getTermsColourService())>=17) && 
					(Integer.parseInt(edo2.getTermsColourService())<=Integer.parseInt(rank.getColorService()))
					&& (Integer.parseInt(edo2.getTermsMaxAgeRetention())>=40 && Integer.parseInt(edo2.getTermsMaxAgeRetention())<=52)) {
				return true;
			}
		}
		
		if (Integer.parseInt(edo2.getTermsColourService())==20 && Integer.parseInt(edo2.getTermsMaxAgeRetention())==40)
			return true;
		
		return false;
	}

	/*public boolean isValidArmyNoForJC(EnrollDo2 edo2) {
		//condition for including TJ of TA personnel added
		if ((edo2.getEntryCategory()!=null && edo2.getEntryCategory().length()>4 
				&& VALID_JC_ENTRY_CATEGORY_SUBSTRING.contains(edo2.getEntryCategory().substring(0,5))) 
				&& !(edo2.getArmyNo().substring(0,2).equals("JC")
						|| edo2.getArmyNo().substring(0,2).equals("TJ"))) 
			return false;
		
		if (edo2.getEntryTrade()!=null && edo2.getEntryTrade().equals("RT") && 
				!(edo2.getArmyNo().substring(0,2).equals("JC")
						|| edo2.getArmyNo().substring(0,2).equals("TJ"))) 
			return false;
		
	 	return true;
	}*/
	//Modified for New Enroll Do2
	public boolean isValidArmyNoForJC(EnrollDo2 edo2) {
		//condition for including TJ of TA personnel added
		if ((edo2.getEntryCategory()!=null && edo2.getEntryCategory().length()>4 
				&& VALID_JC_ENTRY_CATEGORY_SUBSTRING.contains(edo2.getEntryCategory().substring(0,5))) 
				&& !(edo2.getArmyNo().substring(0,2).equals("JC")
						|| edo2.getArmyNo().substring(0,2).equals("TJ"))) 
			return false;
		
				
	 	return true;
	}
	
	public boolean isRateAvailable(EnrollDo2 edo2) {
        int basic = 0;
		if (edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
			basic=2850;
		} else if (edo2.getEnrollmentDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
			basic=5700;
		} else if (edo2.getEnrollmentDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
			basic=8550;
		} else {
			basic=11400;
		}
		String city = null;
		if (edo2.getCcaCode() != null) {
			city = edo2.getCcaCode();
			if ((!edo2.getCcaCode().equals("CCA") && !edo2.getCcaCode().equals("CCA1") && 
				 !edo2.getCcaCode().equals("CCB1"))) {
				if (edo2.getCcaCode().startsWith("CC"))
					city = "CCOTH";
			}
		}
		allowanceRate = Utils.getApplicableAllowanceRates(Do2Manager.DAILY,	edo2.getEnrollmentDate(), 
							edo2.getEnrollmentDate(), city, basic);
				
		return (allowanceRate.size()>0);
	}
	//Commented for New Enroll Do2
	/*public boolean isValidEntryTrade(EnrollDo2 edo2) {
		
		if (edo2.getEntryTrade()!=null) {
			String entryTradeString = edo2.getEntryTrade();
			entryTradeString.replaceAll("(?:\\r\\n|\\n\\r|\\n|\\r)", "");
			edo2.setEntryTrade(entryTradeString.trim());
		}
	
		if (edo2.getEntryTrade()!=null && (Do2Manager.MSTHAV_TRADE_LIST.contains(edo2.getEntryTrade()))) {
			return false;
		}
		List<GroupCategory> groupCategoryList = em.createQuery("SELECT gc FROM GroupCategory gc WHERE " +
								" gc.soldierCategory=:category").setParameter("category", edo2.getEntryTrade()).getResultList();
		
		if (groupCategoryList.size()>0){
			return true;
		} else if (COMMON_ENTRY_TRADE.contains(edo2.getEntryTrade())){
			return true;
		} else {
			return false;
		}
	}*/
	
	public String commitEnrollDo2(EnrollDo2 edo2) {
		if (edo2.getCcaCode()!=null) {
			if (edo2.getCcaCode().length()<3)
				edo2.setCcaCode(null);
		}
	
		insertEmployee(edo2);
		Do2 do2 = insertDo2(edo2);
		insertEmployeeServiceDetail(do2);
		insertNewAuditCage(edo2, do2);
		if (edo2.getBankAccountNo()!=null)
		insertEmployeeBankDetails(edo2);
		
		if (!enrollAuditCage.getFromDate().after(Utils.getLastDayOfPreviousQuarter())) {
			Employee emp = Utils.getCurrentEmployee(edo2.getArmyNo());
			if (emp.getRegularTaFlag()!=null && !emp.getRegularTaFlag().equals("T")){
				if (enrollAuditCage.getBasicPay()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getBasicPay(), Do2Manager.BASIC_PAY);
				
				if (enrollAuditCage.getGradePay()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getGradePay(), Do2Manager.GRADE_PAY);
				
				if (enrollAuditCage.getDp()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getDp(), Do2Manager.DP);
				
				if (enrollAuditCage.getDa()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getDa(), Do2Manager.DA);
							
				if (enrollAuditCage.getMsPay()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getMsPay(), Do2Manager.MS_PAY);
				
				if (enrollAuditCage.getXGroupPay()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getXGroupPay(), Do2Manager.X_GROUP_PAY);
				
				if (enrollAuditCage.getPmha()!=0)
					if (edo2.getGsoCode().equals("S"))
						addArrears(enrollAuditCage, enrollAuditCage.getPmha(), Do2Manager.PMHAH);
					else
						addArrears(enrollAuditCage, enrollAuditCage.getPmha(), Do2Manager.PMHAL);
						
				if (enrollAuditCage.getLra()!=0)
					addArrears(enrollAuditCage, enrollAuditCage.getLra(), "LRA");
			}			
		}
		
		Employee employee = Utils.getCurrentEmployee(edo2.getArmyNo());
		updateAgisAmount(employee);
		
		if (edo2.getAdvanceAmount()!= null && edo2.getAdvanceAmount() > 0)
			insertAcr(employee, edo2);
		
		if (employee.getRegularTaFlag()==null || (employee.getRegularTaFlag()!=null && !employee.getRegularTaFlag().equals("T")))
			insertChangeInRate(employee, edo2);
	 	
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
		
			if (edo2.getGratuitySurrenderedDsc() != null && edo2.getGratuitySurrenderedDsc().equals("Y")) {
				createLoanRecordForGratuity(employee, edo2);
			}
		}
	
		if (edo2.getCcaCode()!=null && (Do2Manager.getValidCityCodes().contains(edo2.getCcaCode()) && !edo2
				.getEnrollmentDate().after(DateManager.getSixthCpcAllowanceDate()))
				|| (edo2.getCcaCode()!=null && edo2.getCcaCode().equals(Do2Manager.HCA))) {
			generateDo2(edo2, edo2.getCcaCode(), edo2.getEnrollmentDate());
		}		
		
		if ((edo2.getArmyNo().substring(0,2).equals("JC")) || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
			(edo2.getEntryCategory() !=null && edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))
			|| (edo2.getEntryCategory() !=null && edo2.getEntryCategory().startsWith("SPHV"))){ 
            //if there is enrolement as JC in TA also -appointment category excluded 
			//from LRA and DA generation
		
			if(employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("D")){		
				;
			} else {
				if (employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("O")){
					insertDaDo2(emp, edo2);
				} else {
					insertLraDo2(emp, edo2);
					insertDaDo2(emp, edo2);
				}
			}
			

			if (edo2.getEnrollmentDate().before(DateManager.getDPDate())) {
				insertAllowanceRevisionDo2(emp, DateManager.getDPDate());
			}
			
			if (edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
				generateDo2(edo2, "PAYFIX", DateManager.getSixthCpcDate());
			}

			if (edo2.getEnrollmentDate().before(DateManager.getSixthCpcAllowanceDate())) {
				insertAllowanceRevisionDo2(emp, DateManager.getSixthCpcAllowanceDate());
			}
			if ((edo2.getArmyNo().substring(0,2).equals("JC")) || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)){
			
				if (employee.getRegularTaFlag().equals("T")){
					;
				} else{
	 			   if (!(Utils.isDateAfterCurrentQuarterEnding(emp.getIncrementDate()))) {
	 				   boolean done = false;
					   for (; !done ; ) {
						   createSysIncrDo2(do2,emp);
						   if (Utils.isDateAfterCurrentQuarterEnding(emp.getIncrementDate())) {
							  done = true;
						   }
					   }
					}
				}
		   }
		}

		if (edo2.getEnrollmentDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
			insertAllowanceRevisionDo2(emp, DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate());
		}
		
		List<Do2> do2List =do2Repo.findByArmyNoFromDateGreaterThanEqualStatusOrderByFromDateAndId(edo2.getArmyNo(),edo2.getEnrollmentDate(),Do2Manager.PENDING);
						
		if (do2List.size()>0) {
			Misc m=new Misc();
			m.setKey1("Batch");
			m.setValue1(Utils.getCurrentQuarter());
			m.setValue2("Ready");
			m.setDate1(DateManager.getToday());
			m.setCurrent(true);
			m.setIntNumber1(do2List.get(0).getBatchNo());
			miscRepo.save(m);
 		}
	

		return Do2Manager.SUCCESS;
	}
/*
	private void insertNewAuditCage(EnrollDo2 edo2, Do2 do2) {
		emp = Utils.getCurrentEmployee(edo2.getArmyNo());
		
		// TA personnel, not on permanent Staff are entitled for minimum basic pay of their rank and 
		// service group in which they are employed in Ecological Units
		// Authority : GOI, MOD letter No.68640/130/R/2 Addl Coy/TA-2/248/US/D(GS-III)(ii) dated 31st March 2008.
		String entryCategory = "";
		if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag() == 'T' && 
				emp.getTaAppointmentCategory() !=null && emp.getTaAppointmentCategory() == 'O' && 
				     emp.getUnit().getUnitName() !=null && emp.getUnit().getUnitName().contains("Ecological")) {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}

		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO")) {
			entryCategory = "NSUB-Y";
		}
		
		if (edo2.getEntryCategory().equals("SPHAV") || edo2.getEntryTrade().equals("SOLGD")) {
			entryCategory = "SPHV-Y";
		}
		
		List<PayScale> payScaleList = Utils
				.getEntityManager()
				.createQuery(
						"SELECT ps FROM PayScale ps where ps.entryCategory =:entryCategory " +
						"and ((:fromDate between ps.fromDate and ps.toDate ) or (:fromDate >= ps.fromDate and ps.toDate is null))")
				.setParameter("entryCategory", entryCategory)
				.setParameter("fromDate", edo2.getEnrollmentDate())
				.getResultList();
		// PAYSCALE TABLE TO BE AMENDED ACCOMMODATING ENTRY CATEGORY FOR 6CPC SCALES ALSO.  OTHERWISE NULL POINTER EXCEPTION EXPECTED.
		if (payScaleList.size() > 0) {
			basicPay = payScaleList.get(0).getNewRecruitPay();		
			enrollAuditCage.setFromDate(edo2.getEnrollmentDate());	
			enrollAuditCage.setDo2(do2);
			enrollAuditCage.setBasicPay(basicPay);
		 	setDefaultValues(enrollAuditCage);
		 	enrollAuditCage.setEmployee(emp);
		 	if (emp.getRegularTaFlag()=='T') {
		 		enrollAuditCage.setAbsentFlag(true);
		 	}
	  
		 	if ((edo2.getArmyNo().substring(0,2).equals("TJ")
		 			||edo2.getArmyNo().substring(0,2).equals("JC")) || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
		 		(edo2.getEntryCategory() !=null && edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))
		 		|| (edo2.getEntryCategory() !=null && edo2.getEntryCategory().startsWith("SPHV"))
		 		|| (edo2.getUnitCode().startsWith("TA")) && (edo2.getUnit().getUnitName().contains("Ecological"))
		 		&& (edo2.getTaAppointmentCategory() == 'O')) { 
				
				if (edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate()) 
								&& !edo2.getEnrollmentDate().before(DateManager.getDPDate())) {
					enrollAuditCage.setDp(Math.round(basicPay/2));
				}
			 
				if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
					if (payScaleList.get(0).getGradePay() != null) {
						gradePay = payScaleList.get(0).getGradePay();	
					}
					enrollAuditCage.setGradePay(gradePay);
					enrollAuditCage.setOldBasicPay(getOldBasicPay(edo2));
				}
				 
				if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcAllowanceDate())) {
					enrollAuditCage.setMsPay(getMsPay());
				}  
				
				if (payScaleList.get(0).getXGroupPay()!=null && payScaleList.get(0).getXGroupPay()!=0) {
					enrollAuditCage.setXGroupPay(payScaleList.get(0).getXGroupPay());
				}
				//In case there is enrolement as JC in TA DA,PMHA,LRA will not be
				//given for appointment Category D
				//pmha,lra will not be given for appointment category O
				if(emp.getRegularTaFlag()=='T' && emp.getTaAppointmentCategory()=='D'){
				
					;
				} else {
					if (emp.getRegularTaFlag()=='T' && emp.getTaAppointmentCategory()=='O'){
						enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));	
					} else {
						enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));
						enrollAuditCage.setPmha(AuditCageHelper.getPmhaRate(do2, enrollAuditCage));
						enrollAuditCage.setLra((int)AuditCageHelper.getLraRate(enrollAuditCage));	
					}
				}
				
			}
		 	em.persist(enrollAuditCage);
			em.flush();
		
		} else {
			FacesMessages.instance().add("PAY SCALE NOT FOUND. PLEASE VERIFY");
		}
	
	}*/
	
	// above method commented and amended below due to sevencpc
    
	/*private void insertNewAuditCage(EnrollDo2 edo2, Do2 do2) {
		emp = Utils.getCurrentEmployee(edo2.getArmyNo());
		
		// TA personnel, not on permanent Staff are entitled for minimum basic pay of their rank and 
		// service group in which they are employed in Ecological Units
		// Authority : GOI, MOD letter No.68640/130/R/2 Addl Coy/TA-2/248/US/D(GS-III)(ii) dated 31st March 2008.
		String entryCategory = "";
		if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag() == 'T' && 
				emp.getTaAppointmentCategory() !=null && emp.getTaAppointmentCategory() == 'O' && 
				     emp.getUnit().getUnitName() !=null && emp.getUnit().getUnitName().contains("Ecological")) {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}

		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO")) {
			entryCategory = "NSUB-Y";
		}
		if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
			
			List<PayScale> payScaleList = Utils
					.getEntityManager()
					.createQuery(
							"SELECT ps FROM PayScale ps where ps.entryCategory =:entryCategory " +
							"and ((:fromDate between ps.fromDate and ps.toDate ) or (:fromDate >= ps.fromDate and ps.toDate is null))")
					.setParameter("entryCategory", entryCategory)
					.setParameter("fromDate", edo2.getEnrollmentDate())
					.getResultList();
			// PAYSCALE TABLE TO BE AMENDED ACCOMMODATING ENTRY CATEGORY FOR 6CPC SCALES ALSO.  OTHERWISE NULL POINTER EXCEPTION EXPECTED.
			if (payScaleList.size() > 0) {
				basicPay = payScaleList.get(0).getNewRecruitPay();		
				enrollAuditCage.setFromDate(edo2.getEnrollmentDate());	
				enrollAuditCage.setDo2(do2);
				enrollAuditCage.setBasicPay(basicPay);
			 	setDefaultValues(enrollAuditCage);
			 	enrollAuditCage.setEmployee(emp);
			 	if (emp.getRegularTaFlag()=='T') {
			 		enrollAuditCage.setAbsentFlag(true);
			 	}
		  
			 	if ((edo2.getArmyNo().substring(0,2).equals("TJ")
			 			||edo2.getArmyNo().substring(0,2).equals("JC")) || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
			 		(edo2.getEntryCategory() !=null && edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))
			 		|| (edo2.getEntryCategory() !=null && edo2.getEntryCategory().startsWith("SPHV"))
			 		|| (edo2.getUnitCode().startsWith("TA")) && (edo2.getUnit().getUnitName().contains("Ecological"))
			 		&& (edo2.getTaAppointmentCategory() == 'O')) { 
					
					if (edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate()) 
									&& !edo2.getEnrollmentDate().before(DateManager.getDPDate())) {
						enrollAuditCage.setDp(Math.round(basicPay/2));
					}
				 
					if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
						if (payScaleList.get(0).getGradePay() != null) {
							gradePay = payScaleList.get(0).getGradePay();	
						}
						enrollAuditCage.setGradePay(gradePay);
						enrollAuditCage.setOldBasicPay(getOldBasicPay(edo2));
					}
					 
					if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcAllowanceDate())) {
						enrollAuditCage.setMsPay(getMsPay(edo2));
					}  
					
					if (payScaleList.get(0).getXGroupPay()!=null && payScaleList.get(0).getXGroupPay()!=0) {
						enrollAuditCage.setXGroupPay(payScaleList.get(0).getXGroupPay());
					}
					//In case there is enrolement as JC in TA DA,PMHA,LRA will not be
					//given for appointment Category D
					//pmha,lra will not be given for appointment category O
					if(emp.getRegularTaFlag()=='T' && emp.getTaAppointmentCategory()=='D'){
					
						;
					} else {
						if (emp.getRegularTaFlag()=='T' && emp.getTaAppointmentCategory()=='O'){
							enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));	
						} else {
							enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));
							enrollAuditCage.setPmha(AuditCageHelper.getPmhaRate(do2, enrollAuditCage));
							enrollAuditCage.setLra((int)AuditCageHelper.getLraRate(enrollAuditCage));	
						}
					}
					
				}
			 	em.persist(enrollAuditCage);
				em.flush();
			
			} else {
				FacesMessages.instance().add("PAY SCALE NOT FOUND. PLEASE VERIFY");
			}
		
		} else 	if (!edo2.getEnrollmentDate().before(DateManager.getSeventhCpcDate())) {
			
			int basic = 0;
			
			String entryPayLevel = Do2Manager.getEntryPayLevel(entryCategory);
			
			if (entryPayLevel!=null && entryPayLevel.equals('3')) {
				basic = 14500;
			} else {
				List<PayMatrix>paymatrixList = Utils.getEntityManager().createQuery( 
						 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel order by basicPay asc ")
						.setParameter("paylevel", entryPayLevel)
						.getResultList();
				
				if (paymatrixList.size() > 0) {
					basic = paymatrixList.get(0).getBasicPay();
				}
			}
		
			
			enrollAuditCage.setBasicPay(basicPay);
			getOld6cpcPay(edo2);
		//	enrollAuditCage.setMsPay(getMsPay(edo2));
		//	enrollAuditCage.setXGroupPay(1400);
		
			//In case there is enrolement as JC in TA DA,PMHA,LRA will not be
			//given for appointment Category D
			//pmha,lra will not be given for appointment category O
			if(emp.getRegularTaFlag()=='T' && emp.getTaAppointmentCategory()=='D'){
				;
			} else {
				if (emp.getRegularTaFlag()=='T' && emp.getTaAppointmentCategory()=='O'){
					enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));	
				} else {
					enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));
					enrollAuditCage.setPmha(AuditCageHelper.getPmhaRate(do2, enrollAuditCage));
					enrollAuditCage.setLra((int)AuditCageHelper.getLraRate(enrollAuditCage));	
				}
			}
			
		}
		
		
	}*/
	
	private void insertNewAuditCage(EnrollDo2 edo2, Do2 do2) {
	//	System.out.println ( " inser new audit cage " +edo2.getArmyNo()  +"  "+do2.getArmyNo());
		if (edo2.getEnrollmentDate().before(DateManager.getSeventhCpcDate())) {
			get6pcPay(edo2,do2);
		} else {
		//	System.out.println (  " 7cpc   "+edo2.getArmyNo());
			get7pcPay(edo2,do2);
		}
		
	}
	
	private Do2 insertDo2(EnrollDo2 edo2) {
		double jdaHours = 0;
		double jdaDays = 0;
		double totalJdaAdmissible = 0;
	 	Do2 do2 = new Do2();
		int basicPay = 2850;
	    
		if (edo2.getCcaCode()!=null && !edo2.getCcaCode().equals("CCA1") 
				&& !edo2.getCcaCode().equals("CCA") && !edo2.getCcaCode().equals("CCB1") && edo2.getCcaCode().startsWith("CC")) {
			edo2.setCcaCode("CCOTH");
		}
		
		allowanceRate = Utils.getApplicableAllowanceRates(Do2Manager.DAILY,	edo2.getEnrollmentDate(), edo2.getEnrollmentDate(),	edo2.getCcaCode(), basicPay);
		if (allowanceRate.size() > 0) {
			if (edo2.getDaDays() != null && edo2.getDaDays() > 0) {
				jdaDays = edo2.getDaDays() * allowanceRate.get(0).getRateAmount();
			}
			
			if (edo2.getDaHours() != null && edo2.getDaHours() > 6) {
				// 0.7 refers to 70% for journey more than 6 hours
				jdaHours = allowanceRate.get(0).getRateAmount() * 0.7;
			}
		}
		String entryCategory = " ";
		
		/*if (edo2.getUnitCode().startsWith("TA") && (edo2.getUnit().getUnitName().contains("Ecological")) 
				&& edo2.getTaAppointmentCategory() == 'O') {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}*/
		//Rewritten for New Enroll Do2
		if (edo2.getUnitCode().startsWith("TA") && (edo2.getUnit().getUnitName().contains("Ecological"))) {
			 entryCategory = "TAECO";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}
		//Added SPNBSUB --- For New Enroll Do2
		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") || edo2.getEntryCategory().equals("SPNBSUB") || edo2.getEntryCategory().equals("TAJCOECO") || edo2.getEntryCategory().equals("TAJCOREG")) {
			entryCategory = "NSUB-Y";
		}
		
		totalJdaAdmissible = (double) (Math.round(jdaDays + jdaHours));
		do2.setEmployee(emp);
		do2.setUnit(unitRepo.findByUnitCode(edo2.getUnitCode()));
		do2.setBatchNo(edo2.getBatchNo());
	 	do2.setArmyNo(edo2.getArmyNo());
		do2.setDo2Date(edo2.getDo2Date());
		do2.setDo2Group(edo2.getDo2Group());
		do2.setDo2ItemNo(edo2.getDo2ItemNo());
		do2.setDo2No(edo2.getDo2No());
		do2.setDo2Type("3");
		do2.setCheckDigit(edo2.getCheckDigit());
		do2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2.setDescription(Do2Manager.ENROLL);
		do2.setColumnC(entryCategory);
		do2.setFromDate(edo2.getEnrollmentDate());
		do2.setQe(Utils.getCurrentQuarter());
		do2.setInputQe(Utils.getCurrentQuarter());		
		do2.setAcceptedQe(Utils.getCurrentQuarter());	
		do2.setStatus(edo2.getStatus());
		do2.setUnitCode(edo2.getUnitCode());
		if (edo2.getUsr()!=null)
			do2.setUsr(edo2.getUsr());
	    do2.setTimeBarSanction(false);
		do2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		do2.setAuditCage(true);
		do2Repo.save(do2);
		Utils.addArrears(do2, totalJdaAdmissible);
		return do2;
	}
	//Commented for New Enroll Do2
	/*private void insertEmployee(EnrollDo2 edo2) {
		if (armyNoExists) {
			Employee emp1 = Utils.getCurrentEmployee(edo2.getArmyNo());
			emp1.setCurrentRecord(false);
			em.persist(emp1);
		 	em.flush();
		}
		emp = new Employee();
		emp.setArmyNo(edo2.getArmyNo());
		emp.setCheckDigit(edo2.getCheckDigit());
		emp.setName(edo2.getEmployeeName());
		emp.setBirthDate(edo2.getDateOfBirth());
		emp.setEnrolmentDate(edo2.getEnrollmentDate());
		emp.setSubstantiveRankDate(edo2.getEnrollmentDate());
		emp.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "soldierCategory", edo2.getEntryTrade()));
		
		String entryCategory = " ";
		if (edo2.getUnitCode().startsWith("TA") && (edo2.getUnit().getUnitName().contains("Ecological")) 
				&& edo2.getTaAppointmentCategory() == 'O') {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}
		
		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO")) {
			entryCategory = "NSUB-Y";
		}
		
		String rankCode = null;
		
		
		if (edo2.getUnitCode().startsWith("TA") && (edo2.getUnit().getUnitName().contains("Ecological"))
				&& (edo2.getTaAppointmentCategory() == 'O')) {
			rankCode =  Do2Manager.getDscRecruitCategoryMap().get(entryCategory);
		} else {
			rankCode = Do2Manager.getRecruitCategoryMap().get(entryCategory);
		}
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ")) {
				rankCode = Do2Manager.getRecruitCategoryMap().get(entryCategory);
			} else {
				rankCode = Do2Manager.getDscRecruitCategoryMap().get(entryCategory);
			} 	
		}
	 	if (rankCode != null) {
 	 		emp.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", rankCode));
			emp.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'E'));
		} else if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
				(edo2.getUnitCode().startsWith("TA") && (edo2.getUnit().getUnitName().contains("Ecological"))
						&& (edo2.getTaAppointmentCategory() == 'O'))) {
				emp.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", rankCode));
				emp.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'E'));
		} else {
			 	emp.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", Do2Manager.RECRUIT));
				emp.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", Do2Manager.RECRUIT_STATUS_CODE));
		} 
  			
		Unit unit = (Unit) Utils.getForeignKey(new Unit(), "unitCode", edo2.getUnitCode());
		emp.setUnit(unit);
		emp.setPresentUnitDate(edo2.getEnrollmentDate());
		emp.setStatusEffectDate(edo2.getEnrollmentDate());
		int gsoCode = 0;
		
		if (edo2.getGsoCode() == 'N')
			gsoCode = Do2Manager.ENROLL_NORMAL;
		else if (edo2.getGsoCode() == 'S')
			gsoCode = Do2Manager.ENROLL_SIKH;
		else if (edo2.getGsoCode() == 'G')
			gsoCode = Do2Manager.ENROLL_GURKHA;
		
		emp.setGsoCode(gsoCode);
		emp.setMarried(false); 
		emp.setTermsColourService(edo2.getTermsColourService());
		emp.setTermsMaxAgeRetention(edo2.getTermsMaxAgeRetention());
		emp.setSportHavildarFlag(false);
		emp.setGraduateHavildarFlag(false);
		emp.setMusterHavildarFlag(false);
		emp.setMusterJcoFlag(false);
		if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_Y)
				|| edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_Z))) {
			emp.setSportHavildarFlag(true);
			emp.setSportHavildarDate(edo2.getEnrollmentDate());
			emp.setHavDate(edo2.getEnrollmentDate());
		}
		if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC)  || edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_BEGRKE))) {
			emp.setGraduateHavildarFlag(true);
			emp.setGraduateHavildarDate(edo2.getEnrollmentDate());
			emp.setHavDate(edo2.getEnrollmentDate());
		}
		if (edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ")) {
		
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) { 
			   if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().equals("NSUB-Y")) {
				    emp.setNbsubDate(edo2.getEnrollmentDate());
			   } else if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().startsWith("NSUB")) {
				    emp.setNbsubDate(edo2.getEnrollmentDate());
			   } else if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().startsWith("SUBE")) {
				    emp.setSubDate(edo2.getEnrollmentDate());
			   } else if (edo2.getEntryCategory()!=null && edo2.getEntryCategory().startsWith("SMAJ")) {
				    emp.setSubmajDate(edo2.getEnrollmentDate());
			   }
		   } else
			        emp.setNbsubDate(edo2.getEnrollmentDate());
		}
		//earlier updated with reference to unit regular_ta
		//now with reference to unitcode
		if(emp.getUnit().getUnitCode().startsWith("TA")){
			emp.setTaAppointmentCategory(edo2.getTaAppointmentCategory());
			emp.setRegularTaFlag('T');
		}else{
			emp.setTaAppointmentCategory('R');
			emp.setRegularTaFlag('R');
		}	
	 	emp.setParaAllowanceFlag(false);
		emp.setPao(Utils.getPao());
	 	emp.setDeputationFlag(false);
		emp.setTbFlag(false);
		emp.setFormerServiceCode(false);
		emp.setGazetteFlag(edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ"));
		emp.setCurrentRecord(true);
		if (edo2.getUsr()!=null)
			emp.setUsr(edo2.getUsr());
		emp.setTranscriptionType('D');
		emp.setRecordStatus('V');
	 	emp.setQe(Utils.getCurrentQuarter());
		emp.setCreatedAt(edo2.getCreatedAt());
		//to skip the increment generation for TA personnel
		if(edo2.getUnitCode().startsWith("TA")){
			;
		}else{
			if((edo2.getEntryCategory()!=null && edo2.getEntryCategory().equals("NSUB-Y"))
				   || edo2.getArmyNo().startsWith("JC")
				   || edo2.getArmyNo().startsWith("TJ")
	    		   || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
	    
			emp.setIncrementDate(getIncrementDate(edo2));
			}
		}
		
		em.persist(emp);
	 	em.flush();
	}*/
	//Rewritten for New Enroll Do2
	private void insertEmployee(EnrollDo2 edo2) {
		if (armyNoExists) {
			Employee emp1 = Utils.getCurrentEmployee(edo2.getArmyNo());
			emp1.setCurrentRecord(false);
			empRepo.save(emp1);
		 	
		}
//		System.out.println("---------In Employee========:");
		
		emp = new Employee();
		emp.setArmyNo(edo2.getArmyNo());
		emp.setCheckDigit(edo2.getCheckDigit());
		emp.setName(edo2.getEmployeeName());
		emp.setBirthDate(edo2.getDateOfBirth());
		emp.setEnrolmentDate(edo2.getEnrollmentDate());
		emp.setSubstantiveRankDate(edo2.getEnrollmentDate());
	//	emp.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "soldierCategory", edo2.getEntryTrade()));
		
	//	String entryCategory = " ";
		String entryCategory = edo2.getEntryCategory();
				
		String rankCode = null;
		
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			
			if (edo2.getEntryCategory().equals("HAVCLK") || edo2.getEntryCategory().equals("HAVSAC") || edo2.getEntryCategory().equals("HAVEDN")
					|| edo2.getEntryCategory().equals("SPHAV")) {
				entryCategory = "HAV";
				rankCode = Do2Manager.getRecruitCategoryMap().get(entryCategory);
				emp.setStatusCode(scRepo.findByStatusCode("E"));
			} else if (edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ")) {
				if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") || edo2.getEntryCategory().equals("SPNBSUB") || edo2.getEntryCategory().equals("TAJCOECO") || edo2.getEntryCategory().equals("TAJCOREG")) {
					entryCategory = "NBSUB";
					rankCode = Do2Manager.getRecruitCategoryMap().get(entryCategory);
				//	System.out.println("=============Entry Category===========:" + entryCategory);
				//	System.out.println("=============Rank Code===========:" + rankCode);
					emp.setNbsubDate(edo2.getEnrollmentDate());
					emp.setStatusCode(scRepo.findByStatusCode("E"));
				}
			
			} else {
				entryCategory = "RECT";
				rankCode = Do2Manager.getRecruitCategoryMap().get(entryCategory);
				emp.setStatusCode(scRepo.findByStatusCode(Do2Manager.RECRUIT_STATUS_CODE));
			}
			
			
			
			
	/*	if (edo2.getEntryCategory().equals("SOLGD") || edo2.getEntryCategory().equals("SOLTECH") || edo2.getEntryCategory().equals("SOLNASS")
				|| edo2.getEntryCategory().equals("SOLCLK") || edo2.getEntryCategory().equals("SKT") || edo2.getEntryCategory().equals("SOLTDN")
				|| edo2.getEntryCategory().equals("TAREG") || edo2.getEntryCategory().equals("TADEPT") || edo2.getEntryCategory().equals("TAECO")) {
			entryCategory = "RECT";
			rankCode = Do2Manager.getRecruitCategoryMap().get(entryCategory);
		} */
		
		
		
	}	if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
		
		if(edo2.getEntryCategory().equals("DSC-Y")) {
			entryCategory = "DSC-Y";
			rankCode = Do2Manager.getDscRecruitCategoryMap().get(entryCategory);
			emp.setStatusCode(scRepo.findByStatusCode("E"));
		}
		
		if (edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ")) {
			if (edo2.getEntryCategory().equals("NSUB-Y")) { 
					entryCategory = "NSUB-Y";
					rankCode = Do2Manager.getDscRecruitCategoryMap().get(entryCategory);
					emp.setNbsubDate(edo2.getEnrollmentDate());
			}else if
				(edo2.getEntryCategory().equals("SUB-Y")){
					entryCategory = "SUB-Y";
					rankCode = Do2Manager.getDscRecruitCategoryMap().get(entryCategory);
					emp.setSubDate(edo2.getEnrollmentDate());
			}else if
				(edo2.getEntryCategory().equals("SMAJ-Y")){
					entryCategory = "SMAJ-Y";
					rankCode = Do2Manager.getDscRecruitCategoryMap().get(entryCategory);
					emp.setSubmajDate(edo2.getEnrollmentDate());
			}
		}
		emp.setStatusCode(scRepo.findByStatusCode("E"));
	}					
			
	 	if (rankCode != null) {
 	 		emp.setSubstantiveRank(rankRepo.findByRankCode(rankCode));
			
		} 
  			
		Unit unit =unitRepo.findByUnitCode(edo2.getUnitCode());
		emp.setUnit(unit);
		emp.setPresentUnitDate(edo2.getEnrollmentDate());
		emp.setStatusEffectDate(edo2.getEnrollmentDate());
		int gsoCode = 0;
		
		if (edo2.getGsoCode().equals("N"))
			gsoCode = Do2Manager.ENROLL_NORMAL;
		else if (edo2.getGsoCode().equals("S"))
			gsoCode = Do2Manager.ENROLL_SIKH;
		else if (edo2.getGsoCode().equals("G"))
			gsoCode = Do2Manager.ENROLL_GURKHA;
		
		emp.setGsoCode(gsoCode);
		emp.setMarried(false); 
		emp.setTermsColourService(edo2.getTermsColourService());
		emp.setTermsMaxAgeRetention(edo2.getTermsMaxAgeRetention());
		emp.setSportHavildarFlag(false);
		emp.setGraduateHavildarFlag(false);
		emp.setMusterHavildarFlag(false);
		emp.setMusterJcoFlag(false);
		if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals("SPHAV"))) {
			emp.setSportHavildarFlag(true);
			emp.setSportHavildarDate(edo2.getEnrollmentDate());
			emp.setHavDate(edo2.getEnrollmentDate());
		}
		if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC)  || edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_BEGRKE))) {
			emp.setGraduateHavildarFlag(true);
			emp.setGraduateHavildarDate(edo2.getEnrollmentDate());
			emp.setHavDate(edo2.getEnrollmentDate());
		}
		
		//earlier updated with reference to unit regular_ta
		//now with reference to unitcode
		
		/*
		if(emp.getUnit().getUnitCode().startsWith("TA")) {
				if(edo2.getEntryCategory().equals("TAREG") || edo2.getEntryCategory().equals("TAECO")){
					emp.setTaAppointmentCategory("R");
					emp.setRegularTaFlag("T");
		}else{
					emp.setTaAppointmentCategory("O");
					emp.setRegularTaFlag("T");
			}
		}else {
					emp.setTaAppointmentCategory("R");
					emp.setRegularTaFlag("R");
		} */
		
		
		if(emp.getUnit().getUnitCode().startsWith("TA")) {
		//	if(edo2.getEntryCategory().equals("TAREG") || edo2.getEntryCategory().equals("TAECO") || edo2.getEntryCategory().equals("TADEPT")){
				emp.setRegularTaFlag("T");
				emp.setTaAppointmentCategory(edo2.getTaAppointmentCategory());
		//	}
		} else {
			emp.setTaAppointmentCategory("R");
			emp.setRegularTaFlag("R");
		}
	 	emp.setParaAllowanceFlag(false);
		emp.setPao(Utils.getPao());
	 	emp.setDeputationFlag(false);
		emp.setTbFlag(false);
		emp.setFormerServiceCode(false);
	//	emp.setGazetteFlag(edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ"));
		if(edo2.getArmyNo().startsWith("JC") || edo2.getArmyNo().startsWith("TJ")){
			emp.setGazetteFlag(true);
		}
		emp.setCurrentRecord(true);
		if (edo2.getUsr()!=null)
			emp.setUsr(edo2.getUsr());
		emp.setTranscriptionType("D");
		emp.setRecordStatus("V");
	 	emp.setQe(Utils.getCurrentQuarter());
		emp.setCreatedAt(edo2.getCreatedAt());
		
		// FOUR Fields updated as per new Enroll format
		emp.setPanNumber(edo2.getPanNumber());
		emp.setUidNo(edo2.getUidNo());
		emp.setEmailId(edo2.getEmailId());
		emp.setMobileNo(edo2.getMobileNo());
		
	//	System.out.println("=============PAN No===========:" + emp.getPanNumber() + " " + edo2.getPanNumber().length());
	//	System.out.println("=============Mobile No===========:" + emp.getMobileNo() + " " + edo2.getMobileNo().length());
	//	System.out.println("============Uid No===========:" + emp.getUidNo() + " " + edo2.getUid().length());
	//	System.out.println("=============Email No===========:" + emp.getEmailId() + " " + edo2.getEmailId().length());
		//to skip the increment generation for TA personnel
		if(edo2.getUnitCode().startsWith("TA")){
			;
		}else{
			if(edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") || edo2.getEntryCategory().equals("SPNBSUB"))
				   || edo2.getArmyNo().startsWith("JC")
				//   || edo2.getArmyNo().startsWith("TJ")
	    		   || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
	    
			emp.setIncrementDate(getIncrementDate(edo2));
			}
		}
	//	System.out.println("===:" + emp.getId());
		empRepo.save(emp);
		
	}

	private void insertEmployeeServiceDetail(Do2 do2) {
		EmployeeServiceDetail esd = new EmployeeServiceDetail();
		esd.setDo2(do2);
		esd.setFromDate(do2.getFromDate());
		esd.setStatus(Do2Manager.ESD_PROMOT+"");     // ESD Status setting modified on 08.08.2011  
		esd.setCreatedAt(DateManager.getCurrentTimestamp());
		esd.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
		if (do2.getUsr()!=null)
			esd.setUsr(do2.getUsr());
		esdRepo.save(esd);
	}
	
	//Modified for NEW ENROLL Do2-----30052021
		private void insertEmployeeBankDetails(EnrollDo2 edo2) {
			EmployeeBankDetails ebd = new EmployeeBankDetails();
			
			ebd.setEmployee(Utils.getCurrentEmployee(edo2.getArmyNo()));
			ebd.setBatchNo(edo2.getBatchNo());
			ebd.setArmyNo(edo2.getArmyNo());
			ebd.setCheckDigit(edo2.getCheckDigit());
			ebd.setPayIfscCode(edo2.getIfscCode());
		//	ebd.setFamoBankAccountNo(edo2.getIfscCode());
			ebd.setPayBankAccountNo(edo2.getBankAccountNo());
		//	ebd.setFamoBankAccountNo(edo2.getBankAccountNo());
			ebd.setFromDate(edo2.getEnrollmentDate());
			ebd.setQe(edo2.getQe());
			ebd.setRecordStatus("P");
			ebd.setApproved(false);
			ebd.setRegularOther("R");
			ebd.setBankAccountName(edo2.getEmployeeName());
			ebd.setCreatedAt(DateManager.getCurrentTimestamp());
			
			ebdRepo.save(ebd);
		}
	
	protected void setDefaultValues(AuditCage auditCage) {
		auditCage.setAbsentFlag(false);
		auditCage.setDeputationFlag(false);
		auditCage.setCancelled(false);
		auditCage.setQe(Utils.getCurrentQuarter());
		auditCage.setRecordStatus("V");
		auditCage.setCreatedAt(DateManager.getCurrentTimestamp());
		auditCage.setDp(0);
		auditCage.setMsPay(0);
		auditCage.setXGroupPay(0);
		auditCage.setOldBasicPay(0);
		auditCage.setOldClassPay(0);
		auditCage.setDa(0);
		auditCage.setLra(0);
		auditCage.setPmha(0);
		auditCage.setTransport(0);
		auditCage.setGradePay(0);
		auditCage.setClassPay(0);
		auditCage.setEffective(true);
		auditCage.setOldGradePay(0);
		auditCage.setCurrentQuarter(true);
		auditCage.setPayLevel("0");
	}

	private void addArrears(AuditCage auditCage, int rate, String payCode) {
 		Arrears arrears = new Arrears(); 		
 		Date fromDate = auditCage.getFromDate(); 	
		Date toDate = Utils.getLastDayOfPreviousQuarter();
		arrears.setAmount((double) (Math.round(rate*DateManager.getNoOfMonths(fromDate, toDate))));
		arrears.setDo2(auditCage.getDo2());
		arrears.setFromDate(fromDate);
		arrears.setToDate(toDate);
		arrears.setQe(Utils.getCurrentQuarter());
		arrears.setDo2ReverseArrears(auditCage.getDo2());
		arrears.setPayCode(pcRepo.findByPayCode(payCode));
		arrears.setRoundOffAmount(false);
		arrears.setArrestPayment(false);
		arrears.setCreatedAt(DateManager.getCurrentTimestamp());
		arrears.setPaid(false);
		arrears.setRegularPay(false);
		arrears.setEmployee(Utils.getCurrentEmployee(auditCage.getDo2().getArmyNo()));
		arrRepo.save(arrears);
	}
	
	private void generateDo2(EnrollDo2 edo2, String description, Date fromDate){
		Do2 do2 = new Do2();
		do2.setArmyNo(edo2.getArmyNo());
		do2.setDo2Date(DateManager.getNextDate(fromDate));
		do2.setDo2Group("IA");
		do2.setDo2ItemNo(edo2.getDo2ItemNo());
		do2.setDo2No(edo2.getDo2No());
		do2.setDo2Type("0");
		do2.setCheckDigit(edo2.getCheckDigit());
		do2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2.setAuditCage(true);
		do2.setDescription(description);
		do2.setFromDate(fromDate);
		if (!description.equals("PAYFIX"))
			do2.setColumnA(Do2Manager.GRANTED);
	 	do2.setQe(Utils.getCurrentQuarter());
		do2.setStatus(Do2Manager.PENDING);
		do2.setUnitCode(edo2.getUnitCode());
		if (edo2.getUsr()!=null)
			do2.setUsr(edo2.getUsr());
		do2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		do2.setEmployee(emp);
		do2.setAuditCage(true);
		do2.setBatchNo(edo2.getBatchNo());
		do2.setInputQe(Utils.getCurrentQuarter());
		do2.setUnit(unitRepo.findByUnitCode(edo2.getUnitCode()));
		do2Repo.save(do2);
	}
	
	private void updateAgisAmount(Employee employee){
		    List<Allowance> allowance;
		    double agisAmount=0.0;
			EmployeeDeduction empDeduction = new EmployeeDeduction();
			empDeduction.setEmployee(employee);
			empDeduction.setQe(Utils.getCurrentQuarter());
			empDeduction.setOpeningBalance(0.0);
			empDeduction.setAcrTotalAmount(0.0);
			empDeduction.setAdjustmentTotalAmountCr(0.0);
			empDeduction.setAdjustmentTotalAmountDb(0.0);
			empDeduction.setAgisAmount(0.0);
			empDeduction.setBhutanTotalAmount(0.0);
			empDeduction.setCbTotalAmountCr(0.0);
			empDeduction.setCbTotalAmountDb(0.0);
			empDeduction.setFamoTotalAmount(0.0);
			empDeduction.setFundSubscriptionRefund(0.0);
			empDeduction.setIncomeTaxEc(0.0);
			empDeduction.setLoanRecoveryAmount(0.0);
			empDeduction.setPliAmount(0.0);
			empDeduction.setTransferInAmount(0.0);
			empDeduction.setBonusOnCreditBalance(0.0);
			empDeduction.setMedicalClaimTotalAmount(0.0);
			empDeduction.setTransferInAmount(0.0);
			empDeduction.setTransferOutAmount(0.0);
			empDeduction.setUdfamoTotalAmount(0.0);
			empDeduction.setNePaidAmount(0.0);
			empDeduction.setClosingBalance(0.0);
			empDeduction.setQuarterPay(0.0);
			empDeduction.setTotalCredits(0.0);
			empDeduction.setTotalDebits(0.0);
			empDeduction.setEmployeeType("E");
			empDeduction.setClosed(false);
			empDeduction.setCumulativeAgisAmount(0.0);
			empDeduction.setCreatedAt(DateManager.getCurrentTimestamp());
			if (employee.getUsr()!=null) {
				empDeduction.setUsr(employee.getUsr());
			}
			Calendar fromCal = new GregorianCalendar();
			fromCal.setTime(employee.getEnrolmentDate());
			fromCal.set(Calendar.DATE,1);
	 		fromCal.set(Calendar.MONTH, fromCal.get(Calendar.MONTH)-1);
	 		Date fromDate = new Date();
			fromDate.setTime(fromCal.getTimeInMillis());
			if(employee.getRegularTaFlag().equals("T")){
				if(employee.getTaAppointmentCategory().equals("D")
					|| employee.getTaAppointmentCategory().equals("O")){
					edRepo.save(empDeduction);
					return;
				}else{
				 Date startDate=employee.getEnrolmentDate();
				 Date cutOffDate =null;
				 try {
					cutOffDate = DateManager.getDate("2011-03-31");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(employee.getEnrolmentDate().after(cutOffDate)){
					Date nextYearDate=null;
					try {
						nextYearDate= DateManager.getDate("2012-04-01");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					startDate = nextYearDate;
				}
				 allowance = Utils.getApplicableAllowanceRates("TAGIF", startDate,startDate);
			 	 if(allowance.size() >0){
					 String monthOfQuarter = Utils.getCurrentQuarter().substring(0,2);
					 if(monthOfQuarter.equals("02")){
						    empDeduction.setAgisAmount(allowance.get(0).getRateAmount());
						    employee.setTagifRecoveryInd(4);
					 }else{
						   empDeduction.setAgisAmount(allowance.get(0).getRateAmount()/4);
						   employee.setTagifRecoveryInd(1);
					 } 
				 }
				 edRepo.save(empDeduction);
				}
				return;  
			}else if(Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) || Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22) ){
				  allowance = Utils.getApplicableAllowanceRates("AGIS",fromDate,Utils.getLastDayOfCurrentQuarter(),Utils.getPao());
			}else{
				  allowance = Utils.getApplicableAllowanceRates("AGIS",fromDate,Utils.getLastDayOfCurrentQuarter());
			}
			
				for(Allowance alw : allowance){
					agisAmount += alw.getRateAmount() *(DateManager.getNoOfMonths(alw.getFromDate(),alw.getToDate()));
				 }
		empDeduction.setAgisAmount(agisAmount);
		edRepo.save(empDeduction);
	}
	
	private void insertChangeInRate(Employee employee,EnrollDo2 edo2){
		   
		   ChangeInRateSub subRate = new ChangeInRateSub();
		   subRate.setEmployee(employee);
		   subRate.setQe(Utils.getCurrentQuarter());
		   subRate.setRecordStatus(Do2Manager.VALID);
		   subRate.setRevisedAmount(Utils.getSubRateRoundedToFive(basicPay+gradePay));
		   subRate.setChangeReason("SYSTEM GENERATED");
	                  
		   if((edo2.getEntryCategory()!=null && edo2.getEntryCategory().equals("NSUB-Y"))
				   || edo2.getArmyNo().startsWith("JC")
				   || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
	    	   GregorianCalendar enrollCal = new GregorianCalendar();
	    	   enrollCal.setTime(edo2.getEnrollmentDate());
	    	   enrollCal.set(Calendar.DATE,1);
	      	   subRate.setFromDate(enrollCal.getTime()); 
	       } else {
	    	   GregorianCalendar enrollCal = new GregorianCalendar();
	    	   enrollCal.setTime(edo2.getEnrollmentDate());
	    	   enrollCal.set(Calendar.DATE,1);
	    	   Date nextSubDate = enrollCal.getTime();
	    	   subRate.setFromDate(DateManager.getNextYear(nextSubDate)); 
 	       }
		  
		   subRate.setApproved(true);
		   subRate.setCreatedAt(DateManager.getCurrentTimestamp()); 
		   subRate.setArmyNo(edo2.getArmyNo()); 
		   subRate.setCheckDigit(edo2.getCheckDigit());
		   
		   if (edo2.getUsr()!=null)
			   subRate.setUsr(edo2.getUsr());
		   rateSubRepo.save(subRate);
		   
		   
		   insertFundSummary(employee); 
		   
		   if (subRate.getFromDate().before(Utils.getLastDayOfPreviousQuarter())) {			  
			   String fundQe=Utils.getFundQuarter();
			   String startMonth=fundQe.substring(0,2);
			   String startYear=fundQe.substring(3);
			   int amount = (int) DateManager.getNoOfMonths(subRate.getFromDate(), Utils.getLastDayOfPreviousQuarter())*subRate.getRevisedAmount();
			   insertFundSubRefund(employee, startMonth, startYear, amount);
		   }
	}
	
	private void insertFundSubRefund(Employee employee, String fromMonth, String fromYear, int amount) {
		FundSubRefund fundSubRefund = new FundSubRefund();
		fundSubRefund.setEmployee(employee);
		fundSubRefund.setQe(Utils.getCurrentQuarter());
		fundSubRefund.setFundQe(Utils.getFundQuarter());
		fundSubRefund.setMonth(fromMonth);
		fundSubRefund.setYear(fromYear);
		fundSubRefund.setRefAmount(0);
		fundSubRefund.setSubAmount(amount);
		fundSubRefund.setCreatedAt(DateManager.getCurrentTimestamp()); 
		if (employee.getUsr()!=null)
			fundSubRefund.setUsr(employee.getUsr());
		fsrRepo.save(fundSubRefund); 
	}
	
	private void insertFundSummary(Employee employee) {
		FundSummary fundSummary =new FundSummary();
        fundSummary = new FundSummary();
        fundSummary.setEmployee(employee);
        fundSummary.setOpeningBalance(0);
        fundSummary.setSubTotal(0);
        fundSummary.setRefundTotal(0);
        fundSummary.setDebitTotal(0);
        fundSummary.setYearlyInterest(0);
        fundSummary.setClosingBalance(0);
        fundSummary.setQe(Utils.getCurrentQuarter());
        fundSummary.setCreatedAt(DateManager.getCurrentTimestamp());
        fundSummary.setEmployeeType("E");
        fundSummary.setNePaidAmount(0);
        fundSummary.setTransferInAmount(0);
        fundSummary.setTransferOutAmount(0);
        fundSummary.setOtherAdjustment(0);
        fundSummary.setInterestAdjusted(0);
        
		if (employee.getUsr()!=null)
			fundSummary.setUsr(employee.getUsr());
       fsRepo.save(fundSummary);
    }
	
	private void createLoanRecordForCommutation(Employee emp, EnrollDo2 edo2) {
		LoanSanction loanRecord = new LoanSanction();
		loanRecord.setArmyNo(edo2.getArmyNo());
		loanRecord.setEmployee(emp);
		loanRecord.setBalancePrincipal(edo2.getCommutationAmount());
		loanRecord.setRecoveryRate(Math.round(edo2.getCommutationAmount()/36));
		loanRecord.setRecoveryInstalmentCount(0);
		loanRecord.setPaymentInstalmentCount(1);
		loanRecord.setSanctionAmount(edo2.getCommutationAmount());
		loanRecord.setSanctionNo("ENR DO2");
	    loanRecord.setQe(Utils.getCurrentQuarter());
		loanRecord.setRecoveryDate(edo2.getEnrollmentDate());
		loanRecord.setArrearsRecovery(true);
		loanRecord.setLoanCode(lcRepo.findByLoanCode("P").get(0));
		loanRecord.setRecordStatus("V");
		loanRecord.setRejectionReason("Pension commuation surrendered on enrolment");
		if (edo2.getUsr()!=null)
			loanRecord.setUsr(edo2.getUsr());
		loanRecord.setCreatedAt(DateManager.getCurrentTimestamp());
		lsRepo.save(loanRecord);
	}
	
	private void createLoanRecordForGratuity(Employee emp, EnrollDo2 edo2) {
		int gratuityAmount = 18000;
		int recoveryRate = 500;
		int totalInstalmentCount = 36;
		Date recoveryDate = edo2.getEnrollmentDate();
		Calendar cal = new GregorianCalendar();
		cal.setTime(edo2.getEnrollmentDate());
		cal.set(Calendar.DATE,1);
		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		if (month==12) {
			month=1;
			year++;
		} else {
			month++;
		}
		String date = year+"-"+month+"-01";
		try {
			recoveryDate = DateManager.getDate(date);
		} catch (Exception e) {
		Utils.handleException(e);
		}
		cal.set(Calendar.MONTH,cal.get(Calendar.MONTH)+1);	
		
		LoanSanction loanRecord = new LoanSanction();
		loanRecord.setArmyNo(edo2.getArmyNo());
		loanRecord.setEmployee(emp);
		loanRecord.setBalancePrincipal(gratuityAmount);
		loanRecord.setRecoveryRate(recoveryRate);
		loanRecord.setRecoveryInstalmentCount(0);
		loanRecord.setPaymentInstalmentCount(1);
		loanRecord.setTotalInstalmentCount(totalInstalmentCount);
		loanRecord.setSanctionAmount(gratuityAmount);
		loanRecord.setSanctionDate(edo2.getEnrollmentDate());
		loanRecord.setSanctionUnit(edo2.getUnit().getUnitCode());
		loanRecord.setSanctionNo("ENR DO2");
	    loanRecord.setQe(Utils.getCurrentQuarter());
		loanRecord.setRecoveryDate(recoveryDate);
		loanRecord.setArrearsRecovery(true);
		loanRecord.setLoanCode(lcRepo.findByLoanCode("G").get(0));
		loanRecord.setRecordStatus("V");
		loanRecord.setRejectionReason("Service Gratuity surrendered on enrolment");
		if (edo2.getUsr()!=null)
			loanRecord.setUsr(edo2.getUsr());
		loanRecord.setCreatedAt(DateManager.getCurrentTimestamp());
		lsRepo.save(loanRecord);
	}
	
	private void insertAcr(Employee employee, EnrollDo2 edo2) {
		Acr acr = new Acr();
		acr.setArmyNo(edo2.getArmyNo());
		acr.setEmployee(employee);
		acr.setCheckDigit(edo2.getCheckDigit());
		acr.setTransType("ACS");
		acr.setCrDr("D");
		acr.setMonth(edo2.getQe().substring(0,2));
		acr.setYear(edo2.getQe().substring(3));
		acr.setAcrTransCode(acrTransCodeRepo.findByTransCode("501"));
		acr.setTransCode("501");
		acr.setOwnPao(Utils.getPao());
		acr.setOtherPao(Utils.getPao());
		acr.setAcceptedQe(edo2.getQe());
		acr.setPaymentDate(edo2.getEnrollmentDate());
		acr.setAmount(edo2.getAdvanceAmount());
		acr.setCreatedAt(DateManager.getCurrentTimestamp());
		acr.setRecordStatus("V");
		acr.setRejectionReason("RO Advance");
		if (edo2.getUsr()!=null)
			acr.setUsr(edo2.getUsr());
		acrRepo.save(acr);
	}
	
	public int getFullMonthDa(AuditCage enrollAuditCage) {
		 
		emp = enrollAuditCage.getEmployee();
	 	
		if ((emp.getRegularTaFlag()!=null && emp.getRegularTaFlag().equals(Do2Manager.TERRITORIAL_ARMY))
				&& (emp.getTaAppointmentCategory()!=null && emp.getTaAppointmentCategory().equals(Do2Manager.STOP_DA_TA_CATEGORY))) {
			return 0;
		}
		
		if (emp.getSubstantiveRank()!=null && emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return 0;
		}
		
		if (emp.getDeputationFlag()!=null && emp.getDeputationFlag()==true
				&& emp.getRecordStatus().equals(Do2Manager.DEPU_IMTRAT_BHUTAN)) {
			return 0;
		}

		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",	enrollAuditCage.getFromDate(), enrollAuditCage.getFromDate());

		double da = 0.0;
		int variableComponents = 0;		
		double daRate = 0.0;
		
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			
			if (!enrollAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && enrollAuditCage.getDp()!=0) {
				daRate = allowance.getRateAmount();
			}			
		 	if (enrollAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = enrollAuditCage.getClassPay();
			} else if (enrollAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (!enrollAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && enrollAuditCage.getDp()!=0)) {
				variableComponents = enrollAuditCage.getDp();
			} else if (enrollAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = enrollAuditCage.getGradePay() + enrollAuditCage.getXGroupPay();
			} else {
				variableComponents = enrollAuditCage.getGradePay()+enrollAuditCage.getXGroupPay()+ enrollAuditCage.getMsPay();
			}
			da += (enrollAuditCage.getBasicPay()+variableComponents)*daRate;
		}
		return ((int) Math.round(da));
	}
	
	private Date getIncrementDate(EnrollDo2 edo2) {
		Date incrementDate = DateManager.getToday();
		if (emp.getEnrolmentDate().before(DateManager.getSixthCpcDate())) {
			incrementDate = DateManager.getNextIncrementDate(edo2.getEnrollmentDate());
		} else if (emp.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
			Date enrollDate = edo2.getEnrollmentDate();
			incrementDate = new Date(enrollDate.getTime());
			Calendar cal = new GregorianCalendar();
			cal.setTime(incrementDate);
			try {
				if (cal.get(Calendar.MONTH)==Calendar.JANUARY && cal.get(Calendar.DATE) == 1) {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "07-01");
				} else {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + 1 + "07-01");
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else {
			Calendar cal = new GregorianCalendar();
			cal.setTime(edo2.getEnrollmentDate());
			Calendar firstJan = new GregorianCalendar(); 
		 	Calendar firstJul = new GregorianCalendar();		 
		 	try {
				firstJul.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+"-"+(Calendar.JULY+1)+"-01"));
				firstJan.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+"-"+(Calendar.JANUARY+1)+"-01"));
			} catch (Exception e) {
				Utils.handleException(e);
			}
			
			if (cal.getTime().equals(firstJan.getTime())) {
				incrementDate = firstJul.getTime();
			} else if (cal.getTime().after(firstJan.getTime()) && !cal.getTime().after(firstJul.getTime())) {
				try {
			 		cal.setTime(DateManager.getDate((cal.get(Calendar.YEAR)+1)+"-"+(Calendar.JANUARY+1)+"-01"));
				} catch (Exception e) {
					Utils.handleException(e);
				}
				incrementDate = cal.getTime(); 
			} else if (cal.getTime().after(firstJul.getTime())) {
				try {
			 		cal.setTime(DateManager.getDate((cal.get(Calendar.YEAR)+1)+"-"+(Calendar.JULY+1)+"-01"));
				} catch (Exception e) {
					Utils.handleException(e);
				}
				incrementDate = cal.getTime();
			}		 
		}
		return incrementDate;
	}
	
	public void createSysIncrDo2(Do2 do2, Employee employee) {
		//skip the generation of increment in case enrolled as JC(TA Personnel)
		if (employee.getRegularTaFlag().equals("T")) {
			return;
		}
		Calendar cal = new GregorianCalendar();
		if (employee!=null && employee.getIncrementDate()!=null) {
			cal.setTime(employee.getIncrementDate());
		} 
	 	Calendar firstJan = new GregorianCalendar(); 
	 	Calendar firstJul = new GregorianCalendar();
	 
	 	try {
			firstJul.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+"-"+(Calendar.JULY+1)+"-01"));
			firstJan.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+"-"+(Calendar.JANUARY+1)+"-01"));
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 	
		Date dni = cal.getTime();	 
				
		int year = cal.get(Calendar.YEAR);        
		int month = cal.get(Calendar.MONTH)+1;	
		int date = 1;	
  		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {			 
			try {
				employee.setIncrementDate(DateManager.getDate(year+1, month, date));
			} catch (Exception e) {
				Utils.handleException(e);
			}
	 	}
		 
		try {
			dni = DateManager.getDate(year, month, date);
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 		
		Do2 incrDo2 = new Do2();
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(do2.getArmyNo());
		incrDo2.setUnitCode(do2.getUnitCode());
		incrDo2.setUnit(unitRepo.findByUnitCode(do2.getUnitCode()));
		incrDo2.setDo2Group("IA");
		incrDo2.setCheckDigit(do2.getCheckDigit());
		incrDo2.setDo2Type("0");
		incrDo2.setDo2No("6666");
		incrDo2.setDo2ItemNo("666");
		incrDo2.setDo2Date(dni);
		incrDo2.setDescription("INCR");
		incrDo2.setFromDate(dni);
		incrDo2.setColumnA(null);
		incrDo2.setColumnB(null);
		incrDo2.setColumnC(null);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setReason(null);
		incrDo2.setBatchNo(do2.getBatchNo());
		incrDo2.setInputQe(Utils.getCurrentQuarter());
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setAuditCage(true);
		do2Repo.save(incrDo2);
		
		if (year < 2006) {
			try {
				employee.setIncrementDate(DateManager.getDate(year+1, month, date));
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else if (year>2005 && do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			employee.setIncrementDate(DateManager.getNextIncrementDate(employee.getIncrementDate()));	
		}  		 
	 	empRepo.save(employee);	
	}
	
	public  void insertDaDo2(Employee employee, EnrollDo2 edo2) {
		Date fromDate= employee.getEnrolmentDate();
		//to generate dado2 from the cut off date if enrolement before ta cut off date for TA personnel
		if(employee.getRegularTaFlag().equals("T")){
			if(!employee.getEnrolmentDate().after(DateManager.getTaCutOffDate())){
				fromDate= DateManager.getNextDate(DateManager.getTaCutOffDate());
			}
		}
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates(Do2Manager.DA, fromDate, 
												Utils.getLastDayOfCurrentQuarter());
		  
		for (Allowance allowance : allowanceList) {
			Do2 daDo2 = new Do2();
			daDo2.setEmployee(employee);
			daDo2.setArmyNo(employee.getArmyNo());
			daDo2.setCheckDigit(employee.getCheckDigit());
			daDo2.setUnitCode(employee.getUnit().getUnitCode());
			daDo2.setUnit(employee.getUnit());
			daDo2.setDo2Group("IA");
			daDo2.setDo2Type("0");
			daDo2.setDo2No("5555");
			daDo2.setDo2ItemNo("555");
			daDo2.setDo2Date(DateManager.getNextDate(allowance.getFromDate()));
			daDo2.setDescription("DA");
			daDo2.setFromDate(allowance.getFromDate());
			daDo2.setStatus(Do2Manager.PENDING);
			daDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			daDo2.setAuditCage(true);
			daDo2.setQe(Utils.getCurrentQuarter());
			daDo2.setInputQe(Utils.getCurrentQuarter());
			daDo2.setBatchNo(edo2.getBatchNo());
			if (employee.getUsr()!=null)
				daDo2.setUsr(employee.getUsr());
			daDo2.setCreatedAt(DateManager.getCurrentTimestamp());
			do2Repo.save(daDo2);
		}
	}

	public  void insertLraDo2(Employee employee, EnrollDo2 edo2) {
		Date fromDate= employee.getEnrolmentDate();
		//to generate lrado2 from the cut off date if enrolement before ta cut off date for TA personnel
		if(employee.getRegularTaFlag().equals("T")){
			if(!employee.getEnrolmentDate().after(DateManager.getTaCutOffDate())){
				fromDate= DateManager.getNextDate(DateManager.getTaCutOffDate());
			}
		}
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates(Do2Manager.NRA, fromDate, Utils.getLastDayOfCurrentQuarter());
	 	for (Allowance allowance : allowanceList) {
			Do2 lraDo2 = new Do2();
			lraDo2.setEmployee(employee);
			lraDo2.setArmyNo(employee.getArmyNo());
			lraDo2.setCheckDigit(employee.getCheckDigit());
			lraDo2.setUnitCode(employee.getUnit().getUnitCode());
			lraDo2.setUnit(employee.getUnit());
			lraDo2.setDo2Group("IA");
			lraDo2.setDo2Type("0");
			lraDo2.setDo2No("4444");
			lraDo2.setDo2ItemNo("444");
			lraDo2.setDo2Date(DateManager.getNextDate(allowance.getFromDate()));
			lraDo2.setDescription("LRA");
			lraDo2.setFromDate(allowance.getFromDate());
		 	lraDo2.setColumnA(Do2Manager.GRANTED);
			lraDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			lraDo2.setAuditCage(true);
			lraDo2.setStatus(Do2Manager.PENDING);
			lraDo2.setQe(Utils.getCurrentQuarter());
			lraDo2.setInputQe(Utils.getCurrentQuarter());
			lraDo2.setBatchNo(edo2.getBatchNo());
			
			lraDo2.setCreatedAt(DateManager.getCurrentTimestamp());
			do2Repo.save(lraDo2);
	 	}
	 }

	public  void insertAllowanceRevisionDo2(Employee employee, Date fromDate) {
		Do2 alwrevDo2 = new Do2();
 		alwrevDo2.setEmployee(employee);
		alwrevDo2.setArmyNo(employee.getArmyNo());
		alwrevDo2.setCheckDigit(employee.getCheckDigit());
		alwrevDo2.setUnitCode(employee.getUnit().getUnitCode());
		alwrevDo2.setUnit(employee.getUnit());
		alwrevDo2.setDo2Group("IA");
		alwrevDo2.setDo2Type("0");
		alwrevDo2.setDo2No("7777");
		alwrevDo2.setDo2ItemNo("777");
		alwrevDo2.setDo2Date(DateManager.getNextDate(fromDate));
		alwrevDo2.setDescription("ALWREV");
		alwrevDo2.setFromDate(fromDate);
		if (fromDate.equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
			alwrevDo2.setColumnA("25PC");
		}
		alwrevDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		alwrevDo2.setStatus(Do2Manager.PENDING);
		alwrevDo2.setAuditCage(true);
		alwrevDo2.setQe(Utils.getCurrentQuarter());
		alwrevDo2.setInputQe(Utils.getCurrentQuarter());
		alwrevDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2Repo.save(alwrevDo2);
	}
	public boolean unitCodeExists(EnrollDo2 edo2){
		Unit unit =unitRepo.findByUnitCode(edo2.getUnitCode());
		if(unit!=null)
			return true;
		
		return false;
		
	}
	private int getOldBasicPay(EnrollDo2 edo2){
		List<PayScale> payList = null;
		int oldBasicPay=0;
		emp = Utils.getCurrentEmployee(edo2.getArmyNo());
		try {
			if (emp.getGroupCategory()!=null) {
				payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleGreaterThanZero(emp.getSubstantiveRank().getId(),emp.getGroupCategory().getSoldierGroup(), DateManager.getDate("1997-10-10"));
						
			 
			} else {   // Added for BEG Roorkee on 26.09.2017
				payList =psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleGreaterThanZero(emp.getSubstantiveRank().getId(),"Y", DateManager.getDate("1997-10-10"));
			 
			}
		} catch (Exception e) { 
			Utils.handleException(e);
		}
		
		if (payList.size()>0)
			oldBasicPay =  payList.get(0).getStartScale();
		
		return oldBasicPay;
	}
	/*public int getMsPay(){
		int mspay = 2000;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		return mspay;
	}*/

	
	public int getMsPay(EnrollDo2 edo2){
 		int mspay=0;
 		if (edo2.getEnrollmentDate().before(DateManager.getSeventhCpcDate())) {
 			mspay=2000;
 		} else {
 			mspay=5200;
 		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		
		
		return mspay;
	}
	
	private void get6pcPay(EnrollDo2 edo2, Do2 do2) {
		
		emp = Utils.getCurrentEmployee(edo2.getArmyNo());
		
		// TA personnel, not on permanent Staff are entitled for minimum basic pay of their rank and 
		// service group in which they are employed in Ecological Units
		// Authority : GOI, MOD letter No.68640/130/R/2 Addl Coy/TA-2/248/US/D(GS-III)(ii) dated 31st March 2008.
		String entryCategory = "";
		if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag().equals("T") && 
				emp.getTaAppointmentCategory() !=null && emp.getTaAppointmentCategory().equals("O") && 
				     emp.getUnit().getUnitName() !=null && emp.getUnit().getUnitName().contains("Ecological")) {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}

		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") ||  edo2.getEntryCategory().equals("TAJCOECO") || edo2.getEntryCategory().equals("TAJCOREG")) {
			entryCategory = "NSUB-Y";
		}
		
		if (edo2.getEntryCategory().equals("SPHAV") || edo2.getEntryCategory().equals("GRHAV")) {
			entryCategory = "SPHV-Y";
		}
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (edo2.getEntryCategory().equals("SUB-Y")) {			
				entryCategory = "SUBE-Y";			
			}
				
			if (edo2.getEntryCategory().equals("SUB-Z")) {			
				entryCategory = "SUBE-Z";			
			}
		}
		
		List<PayScale> payScaleList =psRepo.findByEntryCategoryFromDateBetweenOrFromDateGreaterThan(entryCategory, edo2.getEnrollmentDate()); 
				
		// PAYSCALE TABLE TO BE AMENDED ACCOMMODATING ENTRY CATEGORY FOR 6CPC SCALES ALSO.  OTHERWISE NULL POINTER EXCEPTION EXPECTED.
		if (payScaleList.size() > 0) {
			basicPay = payScaleList.get(0).getNewRecruitPay();		
			enrollAuditCage.setFromDate(edo2.getEnrollmentDate());	
			enrollAuditCage.setDo2(do2);
			enrollAuditCage.setBasicPay(basicPay);
		 	setDefaultValues(enrollAuditCage);
		 	enrollAuditCage.setEmployee(emp);
		 	if (emp.getRegularTaFlag().equals("T")) {
		 		enrollAuditCage.setAbsentFlag(true);
		 	}
	  
		 	if ((edo2.getArmyNo().substring(0,2).equals("TJ")
		 			||edo2.getArmyNo().substring(0,2).equals("JC")) || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
		 		(edo2.getEntryCategory() !=null && edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))
		 		|| (edo2.getEntryCategory() !=null && edo2.getEntryCategory().startsWith("SPHV"))
		 		|| (edo2.getUnitCode().startsWith("TA") && (edo2.getUnit().getUnitName().contains("Ecological"))
		 		&& (edo2.getTaAppointmentCategory().equals("O")))) { 
				
				if (edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate()) 
								&& !edo2.getEnrollmentDate().before(DateManager.getDPDate())) {
					enrollAuditCage.setDp(Math.round(basicPay/2));
				}
			 
				if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcDate())) {
					if (payScaleList.get(0).getGradePay() != null) {
						gradePay = payScaleList.get(0).getGradePay();	
					}
					enrollAuditCage.setGradePay(gradePay);
					enrollAuditCage.setOldBasicPay(getOldBasicPay(edo2));
					enrollAuditCage.setOldGradePay(0);
				}
				 
				if (!edo2.getEnrollmentDate().before(DateManager.getSixthCpcAllowanceDate())) {
					enrollAuditCage.setMsPay(getMsPay(edo2));
				}  
				
				if (payScaleList.get(0).getXGroupPay()!=null && payScaleList.get(0).getXGroupPay()!=0) {
					enrollAuditCage.setXGroupPay(payScaleList.get(0).getXGroupPay());
				}
				//In case there is enrolement as JC in TA DA,PMHA,LRA will not be
				//given for appointment Category D
				//pmha,lra will not be given for appointment category O
				if(emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("D")){
				
					;
				} else {
					if (emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("O")){
						enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));	
					} else {
						enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));
						enrollAuditCage.setPmha(AuditCageHelper.getPmhaRate(do2, enrollAuditCage));
						enrollAuditCage.setLra((int)AuditCageHelper.getLraRate(enrollAuditCage));	
					}
				}
				
			}
		 	acRepo.save(enrollAuditCage); 
		
		} else {
			//FacesMessages.instance().add("PAY SCALE NOT FOUND. PLEASE VERIFY");
		}
	}
	
	private void get7pcPay(EnrollDo2 edo2, Do2 do2) {
		
		emp = Utils.getCurrentEmployee(edo2.getArmyNo());
		
		int basic = 0;
		String entryPayLevel = null;
		
		//enrollAuditCage.setBasicPay(basicPay);
	 	setDefaultValues(enrollAuditCage);
	 	enrollAuditCage.setFromDate(edo2.getEnrollmentDate());	
		enrollAuditCage.setDo2(do2);
		enrollAuditCage.setEmployee(emp);
		if (emp.getRegularTaFlag().equals("T")) {
	 		enrollAuditCage.setAbsentFlag(true);
	 	}
		// TA personnel, not on permanent Staff are entitled for minimum basic pay of their rank and 
		// service group in which they are employed in Ecological Units
		// Authority : GOI, MOD letter No.68640/130/R/2 Addl Coy/TA-2/248/US/D(GS-III)(ii) dated 31st March 2008.
	
		String entryCategory = edo2.getEntryCategory();
		if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag().equals("T") && 
				emp.getTaAppointmentCategory() !=null && emp.getTaAppointmentCategory().equals("O") && 
				     emp.getUnit().getUnitName() !=null && emp.getUnit().getUnitName().contains("Ecological")) {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
				if (edo2.getEntryCategory()!=null && (edo2.getEntryCategory().equals("TAREG") || edo2.getEntryCategory().equals("TADEPT") || edo2.getEntryCategory().equals("TAECO")  || edo2.getEntryCategory().equals("DPHARMA"))) {
					entryCategory="SEP-Y";
				}
		}

		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO") || edo2.getEntryCategory().equals("TAJCOECO") || edo2.getEntryCategory().equals("TAJCOREG") 
				|| edo2.getEntryCategory().equals("SPNBSUB")) {
			entryCategory = "NSUB-Y";
		}
		
		if (edo2.getEntryCategory().equals("SPHAV") || edo2.getEntryCategory().equals("GRHAV")) {
			entryCategory = "SPHV-Y";
		}
		
		
		if ((edo2.getArmyNo().substring(0,2).equals("TJ") || edo2.getArmyNo().substring(0,2).equals("JC")) 
				|| Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
	 		(edo2.getEntryCategory() !=null && edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))
	 		|| (edo2.getEntryCategory() !=null && edo2.getEntryCategory().startsWith("SPHV"))
	 		|| (edo2.getUnitCode().startsWith("TA")) && (edo2.getUnit().getUnitName().contains("Ecological"))
	 		&& (edo2.getTaAppointmentCategory().equals("O"))) { 
			
			entryPayLevel = Do2Manager.getEntryPayLevel(entryCategory);
			List<PayMatrix>paymatrixList = payMatrixRepo.findByPayLevelOrderByBasicPayAsc(entryPayLevel);
			 
			if (paymatrixList.size() > 0) {
				basic = paymatrixList.get(0).getBasicPay();
				enrollAuditCage.setBasicPay(basic);
				enrollAuditCage.setMsPay(getMsPay(edo2));
				enrollAuditCage.setPayLevel(paymatrixList.get(0).getPayLevel());
			} else {
				//FacesMessages.instance().add("PAY MATRIX NOT FOUND. PLEASE VERIFY");
			}
			
		} else {
			entryPayLevel = Do2Manager.getEntryPayLevel(entryCategory);
			if (entryPayLevel!=null && entryPayLevel.equals("3")) {
				basic = 14600;
				enrollAuditCage.setBasicPay(basic);
				enrollAuditCage.setPayLevel("1");
			} else if (entryPayLevel!=null && !entryPayLevel.equals("3")){
				List<PayMatrix>paymatrixList =payMatrixRepo.findByPayLevelOrderByBasicPayAsc(entryPayLevel);
				 				
				if (paymatrixList.size() > 0) {
					basic = paymatrixList.get(0).getBasicPay();
					enrollAuditCage.setBasicPay(basic);
					enrollAuditCage.setPayLevel(paymatrixList.get(0).getPayLevel());
				} else {
					//FacesMessages.instance().add("PAY MATRIX NOT FOUND. PLEASE VERIFY");
				}
				
		}		
			
		}			
		enrollAuditCage.setDo2(do2);
		// commented 
		/*if (!entryPayLevel.equals("3")) {
			enrollAuditCage.setMsPay(5200);
			enrollAuditCage.setXGroupPay(3600);0.
		}*/
		
		getOld6cpcPay(edo2, do2);
	//	enrollAuditCage.setMsPay(getMsPay(edo2));
	//	enrollAuditCage.setXGroupPay(1400);
	
		//In case there is enrolement as JC in TA DA,PMHA,LRA will not be
		//given for appointment Category D
		//pmha,lra will not be given for appointment category O
		if(emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("D")){
			;
		} else {
			if (emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("O")){
				enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));	
			} else {
				enrollAuditCage.setDa(getFullMonthDa(enrollAuditCage));
				enrollAuditCage.setPmha(AuditCageHelper.getPmhaRate(do2, enrollAuditCage));
				enrollAuditCage.setLra((int)AuditCageHelper.getLraRate(enrollAuditCage));	
			}
		}
		
		basicPay = basic;	//	 Added on 09.02.2021 for Lansdowne email observation regarding change in rate sub
		gradePay = 0;
		
		acRepo.save(enrollAuditCage);		
	}
	
	
	private  void getOld6cpcPay(EnrollDo2 edo2, Do2 do2){

		emp = Utils.getCurrentEmployee(edo2.getArmyNo());
				
		// TA personnel, not on permanent Staff are entitled for minimum basic pay of their rank and 
		// service group in which they are employed in Ecological Units
		// Authority : GOI, MOD letter No.68640/130/R/2 Addl Coy/TA-2/248/US/D(GS-III)(ii) dated 31st March 2008.
		String entryCategory = "";
		if (emp.getRegularTaFlag() != null && emp.getRegularTaFlag().equals("T") && 
				emp.getTaAppointmentCategory() !=null && emp.getTaAppointmentCategory().equals("O") && 
				     emp.getUnit().getUnitName() !=null && emp.getUnit().getUnitName().contains("Ecological")) {
			 entryCategory = "SEP-Y";
		} else {
			 entryCategory = edo2.getEntryCategory();
		}
	
		if (edo2.getEntryCategory().equals("DERTJCO") || edo2.getEntryCategory().equals("DEJCO")  || edo2.getEntryCategory().equals("TAJCOECO") || edo2.getEntryCategory().equals("TAJCOREG")) {
			entryCategory = "NSUB-Y";
		}
		
		if (edo2.getEntryCategory().equals("SPHAV") || edo2.getEntryCategory().equals("GRHAV")) {
			entryCategory = "SPHV-Y";
		}
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (edo2.getEntryCategory().equals("SUB-Y")) {			
				entryCategory = "SUBE-Y";			
			}
				
			if (edo2.getEntryCategory().equals("SUB-Z")) {			
				entryCategory = "SUBE-Z";			
			}
		}
		
		List<PayScale> payScaleList = psRepo.findByEntryCategoryFromDateBetweenOrFromDateGreaterThan(entryCategory, edo2.getEnrollmentDate());
				 
			enrollAuditCage.setOldGradePay(0);
		// PAYSCALE TABLE TO BE AMENDED ACCOMMODATING ENTRY CATEGORY FOR 6CPC SCALES ALSO.  OTHERWISE NULL POINTER EXCEPTION EXPECTED.
			if (payScaleList.size() > 0) {
				basicPay = payScaleList.get(0).getNewRecruitPay();	
			
				enrollAuditCage.setOldBasicPay(basicPay);
			 	
		  
			 	if ((edo2.getArmyNo().substring(0,2).equals("TJ")
			 			||edo2.getArmyNo().substring(0,2).equals("JC")) || Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) ||
			 		(edo2.getEntryCategory() !=null && edo2.getEntryCategory().equals(Do2Manager.ENROLL_HAV_AEC))
			 		|| (edo2.getEntryCategory() !=null && edo2.getEntryCategory().startsWith("SPHV"))
			 		|| (edo2.getUnitCode().startsWith("TA")) && (edo2.getUnit().getUnitName().contains("Ecological"))
			 		&& (edo2.getTaAppointmentCategory().equals("O"))) { 
					
				 	gradePay = payScaleList.get(0).getGradePay();	
				 	enrollAuditCage.setOldGradePay(gradePay);						
			 	} 
			} else {
				//	FacesMessages.instance().add("PAY SCALE NOT FOUND. PLEASE VERIFY");
			}
		}	
}
