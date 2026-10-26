package itsdc.dolphin.do2;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

 
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

 
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.AppendixJ;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.AppendixJRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.GroupCategoryRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * 
 * @author Y Raja Reddy 2009-06-18
 */

@RestController
public class Do2GenericProcessor implements java.io.Serializable {
	 
	String message;
	String subMessage;
	boolean validity = true;	 

	Do2 do2;

	Do2Repository do2Repo;
	EmployeeRepository empRepo;
	AppendixJRepository appjRepo;
	AllowanceRateRepository alwRateRepo;
	LpcRegisterRepository lpcRegisterRepo;
	UnitRepository unitRepo;
	GroupCategoryRepository gcRepo;
	
	static public String MASTER_MISSING="MM:Master Missing ";
	static public String DO2_GROUP_NOT_VALID = "VL:Do2 Group is not valid.\n ";
	static public String DO2_TYPE_NOT_VALID = "VL:Do2 Type is not valid.\n ";
	static public String DO2_NO_REQUIRED = "VL:Do2 Number Required.\n ";
	static public String DO2_ITEM_NO_REQUIRED = "VL:Do2 Item Number Required.\n ";
	static public String DO2_DATE_REQUIRED = "VL:DO2 Publication Date is required.\n ";
	static public String DO2_DATE_IN_FUTURE = "VL:Advance date of publication. \n ";
	static public String DO2_DUPLICATE = "DU:Duplicate Do2 with \n";
	static public String DO2_DUPLICATE_PUBLICATION = "DU:DupliCate Publication with \n ";
	static public String DO2_DATE_BEFORE_ENROLMENT_DATE = "VL:DO2 Publication Date is before Enrolment Date.\n ";
	static public String FROM_DATE_REQUIRED = "VL:From Date is required.\n";
	static public String FROM_DATE_AFTER_CURRENT_QUARTER = "VL:From Date is after current month.\n ";
	static public String FROM_DATE_LATER_THAN_DO2_DATE = "VL:From Date is later than Do2 Publication Date.\n ";
	static public String EMPLOYEE_NOT_FOUND = "VL:Army No. and Check Digit combination not available in Employee table.\n ";
	static public String NOT_EFFECTIVE = "VL:Status in Employee table is Not Effective.\n ";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	static public String INVALID_ABC_COMBINATION = "VL:Invalid Description,Group,Column A, B, C combination.\n ";
	static public String FROM_DATE_SHOULD_BE_BLANK = "VL:From Date should be blank.\n ";
	static public String TO_DATE_REQUIRED = "VL:To Date is required. \n ";
	static public String TO_DATE_SHOULD_BE_BLANK = "VL:To Date should be blank.\n ";
	static public String LRA_REQUIRED = "VL:LRA is required.\n ";
	static public String LRA_SHOULD_BE_BLANK = "VL:LRA column should be blank.\n ";
	static public String PUNISHMMENT_REQUIRED = "VL:OC/CM is required.\n ";
	static public String PUNISHMENT_SHOULD_BE_BLANK = "VL:OC/CM should be blank.\n ";
	static public String REF_DO2_REQUIRED = "VL:Ref. Do2 No. is required.\n ";
	static public String REF_DO2_SHOULD_BE_BLANK = "VL:Ref. Do2 No. should be blank.\n ";
	static public String RATE_REQUIRED = "VL:Rate is required.\n ";
	static public String RATE_SHOULD_BE_BLANK = "VL:Rate should be blank.\n ";
	static public String FROM_DATE_BEFORE_ALLOWANCE_BIRTH = "VL:From Date is before the date on which the allowance came into effect.\n ";
	static public String FROM_DATE_AFTER_ALLOWANCE_DEATH = "VL:From Date is after the date on which the allowance was discontinued.\n ";
	static public String TO_DATE_AFTER_ALLOWANCE_DEATH = "VL:To Date is after the date on which the allowance was discontinued.\n ";
	static public String FROM_DATE_AFTER_TO_DATE = "VL:From Date is after To Date. \n ";
	static public String TJRA_REQUIRED = "VL:TJRA is required.\n ";
	static public String TJRA_SHOULD_BE_BLANK = "VL:TJRA Column should be blank.\n ";
	static public String UNIT_NOT_VALID = "VL:Unit code Not Found in Unit Table. \n ";
	static public String REF_DO2_LATER_THAN_DO2 = "VL:Reference Do2 is later than current Do2. \n";
	static public String INCOMPATIBLE_DO2_TYPES = "VL:Do2 Type and Reference Do2 Type are not matching. \n";
	static public String ABSENT = "AU:Absent during the Period. \n";
	static public String DEPUTATION = "AU:On Deputation during the Period. \n";
	static public String REF_DO2_NOT_FOUND = "VL:Reference Do2 Not Found. \n";
	static public String INVALID_TIME = "VL:Time in Column 7A / Column 7B invalid. \n";
	static public String TIME_BAR_PUBLICATION = "VL:Time Bar Publication. \n";
	static public String INCORRECT_REF_DO2 = "VL:Incorrect Reference Do2 . \n";
	static public String REF_DO2_NO_REQUIRED = "VL:Reference Do2 No. Required. \n";
	static public String OSL_ALREADY_REGULARISED = "AU:OSL/ARREST Period Already Regularised. \n";
	static public String DESCRIPTION_NOT_FOUND = "VL:Do2 Description is not found in the AppendixJ. \n";
	static public String REF_DO2_ALREADY_CANCELLED = "AU:Reference Do2 Already Cancelled. \n";
	static public String REF_DO2_PENDING = "AU:Reference Do2 Is Pending / Invalid / MR. Reference Do2 Marked As Cancelled. \n";
	static public String REF_DO2_MARKED_CANCELLED = "AU:Reference Do2 Marked As Cancelled with Reference To Cancel Do2. \n";
	static public String BACK_DATED_DOII = "AU:Back Dated DoII. \n";
	static public String INVALID_RO_PUBLICAITON = "VL:Invalid Record Office publication.\n";
	static public String INVALID_REFDO2_LENGTH = "VL:Reference Do2 length should be 15 eg: 0/1111/222/2009.\n";
	static public String INVALID_REF_DO2_FORMAT = "VL:Reference Do2 format is invalid.  valid format: 0/1111/222/2009.\n";
	static public String INVALID_FROM_DATE = "VL:Invalid From date.\n ";
	static public String INVALID_TO_DATE = "VL:Invalid To Date.\n";
	static public String ARREARS_CLAIM = "VL:To be claimed in arrears.\n";
	static public String BEYOND_CURRENT_QUARTER = "VL:To Date is beyond the current month.\n";
	static public String NO_CORRESPONING_AUDITCAGE="AU:No Corresponding AuditCage for the Cancelled Do2.\n";
	static public String PAY_LEVEL_NOT_FOUND="VL:Paylevel in auditcage is null or 0. Please feed ATCORR as on 01-01-2016 for updation of Paylevel.\n";
	static public String CEA_RATE_IS_ZERO ="VL:Claimed Amount Is Required To Be Entered Against Rate Column.\n";
	static public String CEA_RATE_MORE_THAN_MAXIMUM = "VL:Amount Claimed Is More Than Maximum Admissible Amount.\n";
	static public String DO2_DATE_AFTER_OPTFIX_PUBLICATION_END_DATE = "VL:DO2 Publication Date Is After OPTFIX Publication End Date(31/07/2019).\n ";
	static public String DO2_DATE_BEFORE_OPTFIX_APPROVAL_DATE = "VL:DO2 Publication Date Is Earlier To AppendixJ Optfix Inclusion Date.\n ";
	static public String INCORRECT_FORMAT_USED_FOR_OPTFIX_AFTER_ENDDATE = "VL:DO2 FromDate/ToDate After OPTFIX Publication End Date(31/07/2019).\n ";
	static public String FORMAT_VALID_ONLY_IN_7CPC = "VL:DO2 Description To Be Used Only For Promotion/Upgradation on or After 01/01/2016.\n ";
	static public String FROM_DATE_BEFORE_ENROLMENT_DATE = "VL:DO2 From Date Is Before Enrollment Date For GP-IA Description OR Enrollment Date Is Null.\n ";
	
	static public Date revised_appxj_impl_date ;
	
	
	
	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public AppendixJRepository getAppjRepo() {
		return appjRepo;
	}

	public void setAppjRepo(AppendixJRepository appjRepo) {
		this.appjRepo = appjRepo;
	}

	public AllowanceRateRepository getAlwRateRepo() {
		return alwRateRepo;
	}

	public void setAlwRateRepo(AllowanceRateRepository alwRateRepo) {
		this.alwRateRepo = alwRateRepo;
	}

	public GroupCategoryRepository getGcRepo() {
		return gcRepo;
	}

	public void setGcRepo(GroupCategoryRepository gcRepo) {
		this.gcRepo = gcRepo;
	}

	public UnitRepository getUnitRepo() {
		return unitRepo;
	}

	public void setUnitRepo(UnitRepository unitRepo) {
		this.unitRepo = unitRepo;
	}

	public LpcRegisterRepository getLpcRegisterRepo() {
		return lpcRegisterRepo;
	}

	public void setLpcRegisterRepo(LpcRegisterRepository lpcRegisterRepo) {
		this.lpcRegisterRepo = lpcRegisterRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}

	public Do2GenericProcessor(Do2 do2) {
		 this.setDo2(do2);
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();		
	}

	public Do2GenericProcessor() {
	}

	public Do2 getDo2() {
		return do2;
	}

	public void setDo2(Do2 do2) {
		this.do2 = do2;
	}

	public boolean isValidArmyNo() {
		if (do2.getArmyNo() == null || do2.getCheckDigit() == null)
			return false;

		return Utils.getValidArmyNo(do2.getArmyNo(), do2.getCheckDigit());
	}

	public String getMessage() {
		return message;
	}
	
	String[] VALID_GROUPS = { "IA", "IB", "IIA", "IIB", "ID" };
	char[] VALID_TYPES = { '0', '1', '2', '3' };
	
	public boolean isValidGroup(String do2Group) {
		boolean groupValidity = false;
		if (do2Group == null) {
			groupValidity = false;
		}		
		for (String group : VALID_GROUPS) {
			if (group.equalsIgnoreCase(do2Group)) {
				return true;
			}
		}
		return groupValidity;
	}

	public boolean isValidType(Do2 do2) {
		Character do2Type = do2.getDo2Type().charAt(0);
		if (do2Type == null)
			return false;

		for (char type : VALID_TYPES) {
			if (type == do2Type)
				return true;
		}
		return false;
	}

	public boolean isValidRecordOfficePublication(Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee !=null &&  employee.getRegularTaFlag().equals("T")) {
			return true;
		}
		Character do2Type = do2.getDo2Type().charAt(0);
		if (do2Type == null)
			return false;
		if(Do2Manager.getRoExemptedPaoList().contains(Utils.getPao().getPaoCode()))
			return true;
		return do2Type == '1';

	}

	public boolean isOverLapping() {
		Date toDate = do2.getToDate();
		if (toDate == null)
			toDate = do2.getFromDate();
		
		if (Do2Manager.getMutexNames().contains(do2.getDescription())) {
			return Utils.isMutexAllowance(do2.getArmyNo(), Do2Manager.getMutexNames(), do2.getFromDate(), toDate, do2);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean isDuplicatePublication() {
		if (do2.getDescription().equals("ATCORR") || do2.getDescription().equals("LRA") ||
			do2.getDescription().equals("DA") || do2.getDescription().equals("CEATWN") || do2.getDescription().equals("NOK")
			|| (do2.getDescription()!=null && do2.getAuditCage()==true && do2.getColumnA()!=null && do2.getColumnA().equals(Do2Manager.CEASED))
			|| (do2.getDescription().equals("TPTL") && do2.getColumnA().equals("GRANTED") && do2.getColumnB()!=null && do2.getColumnB().equals("TDREVIEW")
			   && do2.getColumnC()!=null && do2.getTranscriptionType().equals("S"))) {
			return false;
		}
		List<Do2> do2List = new ArrayList<Do2>();
		
		if (do2.getDescription().equals("BUNCH")) {
			do2List = do2Repo.findByDescriptionStatusNotEqualAndNotThisIdForBunch(do2.getEmployee().getId(), do2.getId()); 
		}
				
		if (Do2Manager.getRationRelatedDescriptions().contains(do2.getDescription())) {			 
			do2List =do2Repo.findDuplicatedByEmpIdFromDateToDateDesc(do2.getId(), do2.getEmployee().getId(),do2.getFromDate(),do2.getToDate(), do2.getDescription());
        } else if (do2.getDescription().equals("CEA") /*|| do2.getDescription().equals("CEATWN")*/
                || do2.getDescription().equals("CEAHC") || do2.getDescription().equals("HOSSUB")
                || do2.getDescription().equals("HSBTWN") || do2.getDescription().equals("CHNOK")
                || do2.getDescription().equals("CHSRNM") || do2.getDescription().equals("CHFMNM")) {

            do2List =do2Repo.findDuplicatedByEmpIdFromDateDescColABC(do2.getId(),do2.getEmployee().getId(),do2.getFromDate(),do2.getDescription(),do2.getColumnA(),do2.getColumnB(), do2.getColumnC());
					 
		} else if (do2.getDescription().equals(Do2Manager.CANCEL)) {
			 
			do2List =do2Repo.findDuplicatedByEmpIdFromDateDescColARefDo2No(do2.getId(),do2.getEmployee().getId(),do2.getFromDate(), do2.getDescription(),do2.getColumnA(), do2.getRefDo2No()); 
					 
	//	}else if (do2.getDescription().equals("SCCIA") && do2.getColumnA()!=null
		} else if ((do2.getDescription().equals("SCCIA") || do2.getDescription().equals("MEDAL")) 
				&& do2.getColumnA()!=null && do2.getColumnA()!=null
				&& do2.getColumnA().equals("GRANTED")) {
			 
			do2List =do2Repo.findDuplicatedByEmpIdFromDateDescColAB(do2.getId(),do2.getEmployee().getId(),do2.getFromDate(),do2.getDescription(),do2.getColumnA(),do2.getColumnB());
					 
		} else if (( do2.getDescription().equals("MEDAL") && do2.getColumnA()!=null
				&& do2.getColumnA().equals("AWARDED")) || (do2.getDescription().equals("UID")) || (do2.getDescription().equals("BIOMET"))) {
			do2List =do2Repo.findDuplicatedByEmpIdFromDateDescColAB(do2.getId(),do2.getEmployee().getId(),do2.getFromDate(),do2.getDescription(),do2.getColumnA(),do2.getColumnB());
		} else {
 			do2List =do2Repo.findDuplicatedByEmpIdFromDateDescColA(do2.getId(),do2.getEmployee().getId(),do2.getFromDate(),do2.getDescription(),do2.getColumnA());
		}
			
		if (do2List.size() == 0 && Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			String colA = do2.getColumnA();
			if (do2.getDescription().equals("PROMOT") || do2.getDescription().equals("LORANK") ||
				do2.getDescription().equals("REVERT")){				
				if (do2.getColumnA().equals("SL")){
					colA = "NK";
				} else if (do2.getColumnA().substring(2).equals("SL")){
					colA = do2.getColumnA().substring(0,2)+"NK";
				} else if (do2.getColumnA().equals("AL")){
					colA = "HAV";
				} else if (do2.getColumnA().substring(2).equals("AL")){
					colA = do2.getColumnA().substring(0,2)+"HAV";
				} else if (do2.getColumnA().equals("DL")){
					colA = "NBSUB";
				} else if (do2.getColumnA().equals("CL")){
					colA = "SUB";
				} else if (do2.getColumnA().substring(2).equals("CL")){
					colA = do2.getColumnA().substring(0,2)+"SUB";
				} else if (do2.getColumnA().equals("POL")){
					colA = "SUBMAJ";
				} else if (do2.getColumnA().substring(2).equals("POL")){
					colA = do2.getColumnA().substring(0,2)+"SUBMAJ";
				} 
			}
			if (do2.getDescription().equals("MACPS")){	
			    if (do2.getColumnA().substring(0,2).equals("SL")){
					colA = "NK"+do2.getColumnA().substring(2);
				} else if (do2.getColumnA().substring(0,2).equals("AL")){
					colA = "HAV"+do2.getColumnA().substring(2);
				} else if (do2.getColumnA().substring(0,2).equals("DL")){
					colA = "NSUB"+do2.getColumnA().substring(2);
				} else if (do2.getColumnA().substring(0,2).equals("CL")){
					colA = "SUB"+ do2.getColumnA().substring(2);
				} else if (do2.getColumnA().substring(0,3).equals("POL")){
					colA = "SMAJ"+ do2.getColumnA().substring(3);
				} 
			}
			if (!colA.equals(do2.getColumnA())) {
				do2List =do2Repo.findByDescColAStatusValidPendingFromDateEqualDo2IdNotEqual(do2.getEmployee().getId(),do2.getDescription(), do2.getFromDate(), colA,do2.getId());
			}
		}
		// added code
		if (do2List.size() > 0) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			Do2 duplicateDo2 = do2List.get(0);
			Date toDate = duplicateDo2.getToDate();
			String do2No = duplicateDo2.getDo2Type() + "/"
					+ duplicateDo2.getDo2No() + "/"
					+ duplicateDo2.getDo2ItemNo();
			if (duplicateDo2.getToDate() == null) {
				toDate = duplicateDo2.getFromDate();
			}
			String rejectionType = "DU:  ";
			if (duplicateDo2.getTranscriptionType() != null && duplicateDo2.getTranscriptionType().equals("S")) {
				rejectionType = "SY:  ";
			}
			subMessage = (rejectionType + DO2_DUPLICATE_PUBLICATION
					+ "ARMYNO  " + duplicateDo2.getArmyNo()
					+ duplicateDo2.getDescription() + " "
					+ dateFormat.format(duplicateDo2.getFromDate()) + "  To "
					+ dateFormat.format(toDate) + "   Do2 No. " + do2No
					+ "   Do2 Date. "
					+ dateFormat.format(duplicateDo2.getDo2Date()) + "QE" + duplicateDo2
					.getQe());
		}
 		return do2List.size() != 0;
	}

	public boolean isDo2DateBeforeEnrolment() {
		if (do2.getDo2Date() == null)
			return false;
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (emp == null)// ????
			return false;
		if (emp.getEnrolmentDate() == null)
			return false;
		return (do2.getDo2Date()).before(emp.getEnrolmentDate());
	}

	public boolean isFromDateBeforeEnrolment() {
		if (do2.getFromDate() == null)
			return false;	
		if (do2.getDo2Group() == null)
			return false;
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (emp == null)
			return false;
		if (emp!=null && emp.getEnrolmentDate() == null)
			return true;
		
		if (do2.getDescription().equals("ATCORR") && do2.getFromDate().equals(emp.getEnrolmentDate())) {
			return false;
		}
		if (do2.getDo2Group()!=null && do2.getDo2Group().trim().equals("IA")) {
			if (do2.getFromDate().before(emp.getEnrolmentDate())) {
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)
						&& do2.getDescription().equals("FORSER")) {
					return false;   // added on 05.02.2024
				}
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)  || (employee.getRegularTaFlag()!=null && employee.getTaAppointmentCategory()!=null && 
						(employee.getRegularTaFlag().equals("T") && employee.getTaAppointmentCategory().equals("R")) || employee.getRegularTaFlag().equals("R"))) {
					if (do2.getDescription().startsWith("CEA") || do2.getDescription().equals("PLBON")) {
						return false;
					}
				}
				return true;
			}
		} 		
		return false;
	}
	
	public boolean isFromDateBeforeDo2Date(Date do2Date, Date fromDate) {
		if (do2Date == null || fromDate == null)
			return false;
		
		return !fromDate.after(do2Date);
	}

	public boolean isFromDateAfterCurrentQuarterEnding(Date date) {
		if (date == null)
			return false;
		return Utils.isDateAfterCurrentQuarterEnding(date);
	}

	List<Character> statusList = Arrays.asList(new Character[] { 'E', 'N', 'R', 'D', 'M', 'W', 'T' });
	
	
	public boolean isEffective(String armyNo) {
		if (armyNo == null)
			return false;
		List<Employee> emps =empRepo.findByArmyNoOldArmyNo(armyNo);
		boolean effective = false;
		for (Employee emp : emps) {
			if (emp.getStatusCode() == null)
				return false;
			char status = emp.getStatusCode().getStatusCode().charAt(0);
			if (statusList.contains(status))
				effective = true;
		}
		return effective;
	}
	public List<String> getDo2Descriptions() {
		List<String> do2Descriptions = null;

		 
		if (do2Descriptions == null) {
			do2Descriptions = appjRepo.findDistinctDescription(); 
			 
		}
		return do2Descriptions;
	}

	public List<String> getAllowanceNames() {
		List<String> allowanceNames = null;

		 
		if (allowanceNames == null) {
			allowanceNames =alwRateRepo.findDistinctAllowanceName();
 		}
		return allowanceNames;
	}

	public List<AllowanceRate> getAllowanceRateList() {
		List<AllowanceRate> allowanceRateList = null;
		
		if (allowanceRateList == null) {
			allowanceRateList =alwRateRepo.findByAllowanceNameIgnoreCase(do2.getDescription());
 		}
		return allowanceRateList;
	}

	public boolean isFromDateBeforeAllowanceBirth() {
		List<String> allowanceNames = getAllowanceNames();

		if (allowanceNames.contains(do2.getDescription())) {
			List<AllowanceRate> allowanceRateList = getAllowanceRateList();
			for (AllowanceRate ar : allowanceRateList) {
				if (ar.getFromDate() != null
						&& (ar.getFromDate().before(do2.getFromDate()) || (ar
								.getFromDate().equals(do2.getFromDate())))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isFromDateAfterAllowanceDeath() {
		List<String> allowanceNames = getAllowanceNames();
		if (allowanceNames.contains(do2.getDescription())) {
			List<AllowanceRate> allowanceRateList = getAllowanceRateList();
			for (AllowanceRate ar : allowanceRateList) {
				if (ar.getToDate() == null) {
					return false;
				}
				if (ar.getToDate() != null
						&& !ar.getToDate().before(do2.getFromDate())) {

					return false;
				}
			}
			return true;
		}
		return false;
	}

	List<AppendixJ> appjList = new ArrayList<AppendixJ>();
	List<AppendixJ> cancelAppjList = new ArrayList<AppendixJ>();

	public boolean isFormatValid(Do2 do2) {
		
		if (do2.getDescription()!=null && do2.getDescription().equals("ATCORR")				
		//		&& do2.getColumnC()!=null && do2.getColumnC().equals("ALWREV")
				&& do2.getReason()!=null && do2.getReason().equals("System Generated Do2")
				&& (do2.getColumnB()!=null && (do2.getColumnB().equals("BHUTAN") || do2.getColumnB().equals("CHAFAA") || do2.getColumnB().equals("RISKCFAARECOVERY")
						|| do2.getColumnB().equals("OPTFIXREVIEW") || do2.getColumnB().equals("SFASFACOM")  || do2.getColumnB().equals("SPIC")  
						|| do2.getColumnB().equals("PMHA") || do2.getColumnB().equals("TRGALLNCCUNITSREVIEW")))) {
			return true;
		}
		/*
		if (do2.getDescription()!=null && do2.getDescription().equals("BUNCH")) {
			return true;
		}*/
		
		if (do2.getDescription()!=null && do2.getDescription().equals("INCR") && do2.getColumnC().equals("INCRREV") 
				&& Utils.getCurrentQuarter().equals("12/2024")) {
 			return true;
 		}

		List<String> promotRanks = Arrays.asList(new String[] { 
				"TSNK",	"TSLD", "PANK", "PALD", "SBNK", "SBLD", "SBSL", "PAHAV", "PADFR",
				"SBHAV", "SBDFR", "SBAL", "GRHAV", "GRDFR", "GRAL", "SPHAV", "SPDFR", "SPAL", "NBSUB",
				"NBRIS", "DL", "PASUB", "PARIS", "SBSUB", "SBRIS", "SBCL", "PASUBMAJ", "PARISMAJ",
				"SBSUBMAJ", "SBRISMAJ", "SBPOL",  "SPNBSUB", "SPNBRIS", "SPRIS", "SPSUB"});	
		List<String> sphavRanks = Arrays.asList(new String[] { 
				"SEP",	"SEPOY", "CFN", "GNR", "GDR", "GDSM", "RFN", "SIGMN", "SPR"});	
		
		if (Do2Manager.getSystemGeneratedDescription().contains(do2.getDescription()))
			return true;
		
		// This id condition is added for NE 
		if (Do2Manager.getSystemGeneratedNeDescription().contains(do2.getDescription()))
			return true;
								
		if (do2.getColumnA() !=null && do2.getColumnA().equals(Do2Manager.CEASED)){
	     	   do2.setColumnB(null);
	     	   do2.setColumnC(null);
	     	   do2.setRate(null);
	     }
		 if ((do2.getDescription().equals(Do2Manager.CILQ) || do2.getDescription().equals(Do2Manager.SNLQ) )
				 && do2.getColumnA() != null 
				 && do2.getColumnA().equals(Do2Manager.GRANTED)
				 && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			 if (do2.getColumnC() != null && (do2.getColumnC().equals("CCC") 
					 || do2.getColumnC().equals("CCOTH"))){
				 do2.setColumnC("CCZ");
			 }
			 if (do2.getColumnC() != null &&  do2.getColumnC().equals("CCA1")){
				 do2.setColumnC("CCX");
			 }
			 if (do2.getColumnC() != null && 
					 (do2.getColumnC().equals("CCA") || do2.getColumnC().equals("CCB1") 
					 || do2.getColumnC().equals("CCB2"))){
				 do2.setColumnC("CCY");
			 }
		 }
		 
		 if ((do2.getDescription().equals(Do2Manager.TPTL) || do2.getDescription().equals(Do2Manager.TPTH))
				 && do2.getColumnA() != null 
				 && do2.getColumnA().equals(Do2Manager.GRANTED)
				 && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			 if (do2.getColumnC() != null && (do2.getColumnC().equals("CCX"))){
				 do2.setColumnC("CCA");
			 }
			 if (do2.getColumnC() != null &&  (do2.getColumnC().equals("CCY") ||do2.getColumnC().equals("CCZ"))){
				 do2.setColumnC("CCOTH");
			 }
			 
		}
		boolean formatValidity = true;
		StringBuffer buffer = new StringBuffer();
		subMessage = "";
		
		String trade = null;
		char group = ' ';
		char gpclass = ' ';
	
		AppendixJ appj;
		
			
			String colA = null;
			String colB = null;
			String colC = null;
			if (do2.getColumnA() != null
					&& do2.getColumnA().trim().length() == 0)
				colA = null;
			else
				colA = do2.getColumnA();
			if (do2.getColumnB() != null
					&& do2.getColumnB().trim().length() == 0)
				colB = null;
			else
				colB = do2.getColumnB();
			if (do2.getColumnC() != null
					&& do2.getColumnC().trim().length() == 0)
				colC = null;
			else
				colC = do2.getColumnC();

		

			appjList = appjRepo.findByDescriptionAndDo2GroupAndColumnAAndColumnBAndColumnC(do2.getDescription(), do2.getDo2Group(), colA, colB, colC);
			if (appjList.size() > 0) {
				appj = appjList.get(0);
			} else {
				List<AppendixJ> specialList = appjRepo.findByDescriptionAndSpecial(do2.getDescription(), true); 
				if (specialList.size() == 0) {
					if (do2.getDescription().equals("TPTL") && do2.getColumnB()!=null && do2.getColumnB().equals("TDREVIEW")
							&& do2.getColumnA()!=null && do2.getColumnA().equals("GRANTED") && do2.getColumnC()!=null) {
						return true;
					}
					if (do2.getDescription().equals("PFWDL") && do2.getDo2Group().equals("ID")) {
						appjList = null;
						appjList = appjRepo.findByDescriptionAndDo2GroupAndColumnAAndColumnBAndColumnC(do2.getDescription(), do2.getDo2Group(), colA, colB, null);
						if (appjList!=null && appjList.size()>0) {
							if (do2.getColumnC().trim().toLowerCase().equals("special")) {
								return true;
							}
						}
					}
					subMessage = INVALID_ABC_COMBINATION;
					return false;
				}
				appjList=specialList;
				appj = specialList.get(0);
			}
		
		
		AppendixJ cancelAppj = null;
		if (cancelAppj == null)
			cancelAppj = new AppendixJ();
		if(do2.getDescription().equals("CANCEL") && (do2.getColumnA()!=null)){			
			cancelAppj.setDescription(do2.getColumnA());
			cancelAppj.setDo2Group(do2.getDo2Group());
		}
		List<String> fieldList1 = new ArrayList<String>();
		fieldList1.add("description");
		fieldList1.add("do2Group");
		
		cancelAppjList = appjRepo.findByDescriptionAndDo2Group(do2.getColumnA(), do2.getDo2Group());
		
		 
		
		if (appj.getFromDateReq() == true && do2.getFromDate() == null) {
			formatValidity = false;
			buffer.append(FROM_DATE_REQUIRED);
		} else if (appj.getFromDateReq() == false && do2.getFromDate() != null) {
			formatValidity = false;
			buffer.append(FROM_DATE_SHOULD_BE_BLANK);
		}
		if (appj.getToDateReq() == true && do2.getToDate() == null) {
			formatValidity = false;
			buffer.append(TO_DATE_REQUIRED);
		} else if (appj.getToDateReq() == false && do2.getToDate() != null) {
			if (Do2Manager.getCeasedMap().get(appj.getDescription()) != null) {
				;
			} else if (do2.getDescription().equals("ENCASH") && do2.getColumnA().equals("LTC")){
				;
			}else if (do2.getDescription().equals("MEDCAT")){
				;
			}else if (Do2Manager.getIncrCancelDescription().contains(do2.getDescription()) && !do2.getFromDate().before(DateManager.getOptionOnPromotionApprovedDate())) {
				;  // To Allow Option Date In To-Date Column
			} else if (do2.getDescription().equals("AWARD") && do2.getColumnA().equals("ASSPP") && do2.getFromDate().before(DateManager.getAssppAwardEffectiveDate())) {
				;  // To allow todate set in do2home with original fromdate as published in do2 for Award-ASSPP before allowance effective date				
			} else {
				formatValidity = false;
				buffer.append(TO_DATE_SHOULD_BE_BLANK);
			}
		}
		/*
        if (appj.getTjraReq()==false ){
        	do2.setTjra(null);
        }
        if (appj.getLraReq()==false ){
        	do2.setLra(null);
        }
        if (appj.getRateReq()==false ){
        	do2.setRate(null);
        }*/
        
        
        if (appj.getTjraReq()==false ){
        	do2.setTjra(0);
        }
        if (appj.getLraReq()==false ){
        	do2.setLra(0);
        }
        if (appj.getRateReq()==false ){
        	do2.setRate(0.0);
        }
        if (appj.getOcCmReq() == false ){
        	do2.setPunishment(null);
        }
        if (appj.getRefDo2NoReq()==false ){
        	do2.setRefDo2No(null);
        }   
        if (do2.getDescription().equals("CANCEL")){        	
        	if (checkforCancelColumnA(do2) == false){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	}
        }
        if (do2.getDescription().equals("FORGSP") && do2.getDo2Group().equals("IB") &&
        	!do2.getDo2Date().before(revised_appxj_impl_date)) { 
        	formatValidity=false;
			subMessage = INVALID_ABC_COMBINATION;
        }
        if (do2.getDescription().equals("ALOTRD")) { 
        	if (do2.getColumnA() == null){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	} else if (do2.getColumnA().startsWith("GP-") || do2.getColumnA().startsWith("GP ")){
        	    ;
        	} else {
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	} 
        	if (do2.getColumnC() == null){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	}             	
        	if (formatValidity == true){
	           	group = do2.getColumnA().charAt(3);
	        	trade = do2.getColumnC().trim();	
	        	if (checkforTradeGroupClass(trade, group, gpclass) == false){
	        		formatValidity=false;
	        		subMessage = INVALID_ABC_COMBINATION;
	        	}
        	}        	
        }
        if (do2.getDescription().equals("MUSTER") || do2.getDescription().equals("OWNMST") || do2.getDescription().equals("REMUST")) {
           	if (do2.getColumnA() == null){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	} else if (do2.getColumnA().startsWith("GP-") || do2.getColumnA().startsWith("GP ")){
        	    ;
        	} else {
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	}
        	if (do2.getColumnB() == null) {
    			formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
    		} else if (do2.getColumnB().startsWith("CL-") || do2.getColumnB().startsWith("CL ")) {
        		;
        	} else {
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	}
        	if (do2.getColumnC() == null){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	} 
        	if (formatValidity == true){	        	
	        	group = do2.getColumnA().charAt(3);
	        	gpclass = do2.getColumnB().charAt(3);
	        	trade = do2.getColumnC().trim();	
	        	if (checkforTradeGroupClass(trade, group, gpclass) == false){
	        		formatValidity=false;
	        		subMessage = INVALID_ABC_COMBINATION;
	        	}
        	}   
    	}
        if (do2.getDescription().equals("ATTSAM") || do2.getDescription().equals("ATTSBM")) {
           	if (do2.getColumnA() == null){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	} else 	if (do2.getColumnA().startsWith("GP-") || do2.getColumnA().startsWith("GP ")){
        	    ;
        	} else {
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	}
        	if (do2.getColumnB() == null) {
    			formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
    		}        	
        	if (formatValidity == true){	        	
	        	group = do2.getColumnA().charAt(3);	        	
	        	trade = do2.getColumnB().trim();	
	        	if (checkforTradeGroupClass(trade, group, gpclass) == false){
	        		formatValidity=false;
	        		subMessage = INVALID_ABC_COMBINATION;
	        	}
        	}   
    	}
        if (do2.getDescription().equals("PROMOT")) {
        	if (do2.getColumnA() == null){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	} else if (!promotRanks.contains(do2.getColumnA())){
        		formatValidity=false;
				subMessage = INVALID_ABC_COMBINATION;
        	}
        	if (!do2.getDo2Date().before(revised_appxj_impl_date) && (do2.getColumnA() != null && 
                do2.getColumnA().equals("PANK")     || do2.getColumnA().equals("PALD") || 
                do2.getColumnA().equals("PASUBMAJ") || do2.getColumnA().equals("PARISMAJ"))) {
        		formatValidity=false;
        		subMessage="Paid acting promotions are admissible only for Hav/Dfr/Sub/Ris";
        	}
        	if (do2.getColumnA() != null && 
        	   (do2.getColumnA().equals("GRHAV") || do2.getColumnA().equals("SPHAV"))) {
        		if (do2.getColumnB() == null){
            		formatValidity=false;
    				subMessage = INVALID_ABC_COMBINATION;
            	} else if (!sphavRanks.contains(do2.getColumnB().trim())) {
            		formatValidity=false;
    				subMessage = INVALID_ABC_COMBINATION;
            	}
        	}
        	if (do2.getColumnA() != null && 
               (do2.getColumnA().equals("GRAL") || do2.getColumnA().equals("SPAL"))) {
        		if (do2.getColumnB() == null){
             		formatValidity=false;
     				subMessage = INVALID_ABC_COMBINATION;
             	} else if (!(do2.getColumnB().equals("TRN"))) {
             		formatValidity=false;
     				subMessage = INVALID_ABC_COMBINATION;
             	}
            }
        	if (do2.getColumnA() != null && 
               (do2.getColumnA().equals("GRDFR") || do2.getColumnA().equals("SPDFR"))) {
         		if (do2.getColumnB() == null){
             		formatValidity=false;
     				subMessage = INVALID_ABC_COMBINATION;
             	} else if (!(do2.getColumnB().equals("SOWAR")) || (do2.getColumnB().equals("SWR"))) {
             		formatValidity=false;
     				subMessage = INVALID_ABC_COMBINATION;
             	}
            }        	
            if (do2.getColumnA() != null && (do2.getColumnA().startsWith("NB") || do2.getColumnA().equals("DL"))) {
           		if (do2.getColumnB() == null) {
           			formatValidity=false;
           			subMessage = INVALID_ABC_COMBINATION;
           		} else if (do2.getColumnB().startsWith("GP-") || do2.getColumnB().startsWith("GP ")){
           			;
           		} else {
           			formatValidity=false;
           			subMessage = INVALID_ABC_COMBINATION;
           		}
           		if (do2.getColumnC() == null){
           			formatValidity=false;
           			subMessage = INVALID_ABC_COMBINATION;
           		} 
           		if (formatValidity == true){	        	
           			group = do2.getColumnB().charAt(3);	    
           			if (do2.getColumnC()!=null && !do2.getColumnC().equals("OUTOFTURN")) {
           				trade = do2.getColumnC().trim();	
               			if (checkforTradeGroupClass(trade, group, gpclass) == false){
               				formatValidity=false;
               				subMessage = INVALID_ABC_COMBINATION;
               			}
           			}
           			
           		}   
            } 
        }
        if (do2.getDescription().equals("MSTHAV")) {
        	if (Utils.getPao().getPaoCode().equals("88")) {
        		if (do2.getColumnC() != null) {
        			if (do2.getColumnC().equals("HMT")) {
        				if (!do2.getFromDate().before(DateManager.getRevisedMsthavImplDate())){
        					formatValidity=false;
        				//	subMessage = INVALID_ABC_COMBINATION;
        					buffer.append(INVALID_ABC_COMBINATION);
        				} else {
        					;
        				}
        			} else if (!Do2Manager.MSTHAV_TRADE_LIST.contains(do2.getColumnC())){
        				formatValidity=false;
    					// subMessage = INVALID_ABC_COMBINATION;
    					buffer.append(INVALID_ABC_COMBINATION);
        			}
        		}
        	} else if (Utils.getPao().getPaoCode().equals("54") || Utils.getPao().getPaoCode().equals("55") || Utils.getPao().getPaoCode().equals("56")) {        		
        			if (!do2.getFromDate().before(DateManager.getRevisedMsthavImplDate())) {
        				if (do2.getColumnC() != null && 
        				   !(do2.getColumnC().equals("JECIV") || do2.getColumnC().equals("JEEM") || do2.getColumnC().equals("DED")
        						   || do2.getColumnC().equals("AIG") || do2.getColumnC().equals("TIFC") || do2.getColumnC().equals("SURVCARTO")
        						   || do2.getColumnC().equals("TIR") || do2.getColumnC().equals("TICC") || do2.getColumnC().equals("TIGM"))) {
        					formatValidity=false;
        					// subMessage = INVALID_ABC_COMBINATION;
        					buffer.append(INVALID_ABC_COMBINATION);
        			}
        		}
        	} /*else { // added on 29.08.2023
        		formatValidity=false;
				// subMessage = INVALID_ABC_COMBINATION;
				buffer.append(INVALID_ABC_COMBINATION);
        	}*/ // commented on 27.09.2023 -ASC FS
        }
		if (appj.getTjraReq() == true && do2.getTjra() == null) {
			if ((do2.getDescription().equals("CEA") || do2.getDescription().equals("CEATWN") || do2.getDescription().equals("CEAHC"))
					&& (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) || !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()))) {
	 			;
			} else {
				formatValidity = false;
				buffer.append(TJRA_REQUIRED);
			}

		} else if (appj.getTjraReq() == false && do2.getTjra() != null
				&& do2.getTjra() != 0) {
			formatValidity = false;
			buffer.append(TJRA_SHOULD_BE_BLANK);
		}
		
		if (!do2.getDescription().equals(Do2Manager.DAILY)) {
			if (do2.getDescription().equals(Do2Manager.TYOJ)) {
				if ((do2.getColumnA()==null || do2.getColumnA().length()==0) && do2.getColumnC()!=null)
					;
			} else if (appj.getLraReq() == true && do2.getLra() == null) {
				if ((do2.getDescription().equals("CEA")    || 
					 do2.getDescription().equals("CEATWN") || 
					 do2.getDescription().equals("CEAHC")) &&  
					(do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) || !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()))) {
					;
				} else if (do2.getDescription().equals("JRNYPD") && !do2.getDo2Date().before(revised_appxj_impl_date)){
					;
				} else if (do2.getDescription().equals("ENCASH") && !do2.getColumnA().equals("LTC")){
					;
				} else {
					formatValidity = false;
					buffer.append(LRA_REQUIRED);
				}
			} else if (appj.getLraReq() == false && do2.getLra() != null
					&& do2.getLra() != 0) {
				formatValidity = false;
				buffer.append(LRA_SHOULD_BE_BLANK);
			}
		}

		if (appj.getOcCmReq() == true && 
			!(do2.getPunishment() == null || do2.getPunishment().equals("OC") || do2.getPunishment().equals("CM") || do2.getPunishment().equals("CV"))) {
			formatValidity = false;
			buffer.append(PUNISHMMENT_REQUIRED);
		} else if (appj.getOcCmReq() == false && do2.getPunishment() != null
				&& do2.getPunishment().length() != 0) {
			formatValidity = false;
			buffer.append(PUNISHMENT_SHOULD_BE_BLANK);
		}
		if (appj.getRefDo2NoReq() == true && do2.getRefDo2No() == null) {			
			if (do2.getDescription().equals("REP")    || do2.getDescription().equals("SEVREP") ||
				do2.getDescription().equals("FINE")   || do2.getDescription().equals("ABF")    ||
		        do2.getDescription().equals("MILRI")  || do2.getDescription().equals("REVERT") ||
		        do2.getDescription().equals("REAPPT") || do2.getDescription().equals("FORPAY") ||
		        do2.getDescription().equals("FORGSP") || do2.getDescription().equals("FRFGSP") ||
		        do2.getDescription().equals("RANKS") || do2.getDescription().equals("EXGDRL") ||
		        do2.getDescription().equals("LORANK") || do2.getDescription().equals("LOCLAS") ||
		        (do2.getDescription().equals("ADINC") && do2.getColumnA().equals("FIRST")) ||
		        ((do2.getDescription().equals("TFRGTH") || do2.getDescription().equals("TFRGTL")) && 
		          do2.getColumnB().equals("INDMOVE") && Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode())))  {
				;
			} else if ((do2.getDescription().equals("CEA")|| 
					    do2.getDescription().equals("CEATWN") ||
				        do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CEAHCT")) &&
					    ((do2.getColumnC() != null && (do2.getColumnC().equals("INITIAL") || do2.getColumnC().startsWith("Q")))
							    || ((do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().trim().length()==0)) 
							    	&& !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())))) {
									;
			} else {
				
				try {
					if (do2.getDescription().equals("HRA")
							&& !do2.getDo2Date().before(DateManager.getDate("2022-06-01"))
							&& !do2.getFromDate().before(DateManager.getDate("2022-04-08"))) {
						;				
					}else if(do2.getDescription().equals("MEDCAT")) {
						;
					}else {
						
						formatValidity = false;
						buffer.append(REF_DO2_REQUIRED);		
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

		} else if (appj.getRefDo2NoReq() == false && do2.getRefDo2No() != null
				&& do2.getRefDo2No().length() != 0) {
			formatValidity = false;
			buffer.append(REF_DO2_SHOULD_BE_BLANK);
		}
		
		if (appj.getRateReq() == true && do2.getRate() == null) {
	 		if (do2.getDescription().equals(Do2Manager.TYOJ) && 
			   (do2.getColumnA()==null || do2.getColumnA().length()==0) &&
				do2.getColumnC()!=null) { 		 		
				;
			} else if (do2.getColumnA().equals("CEASED")){
				;
			} else {
				try {	// added on 16.09.2022
					if (do2.getDescription().equals("HRA")
							&& !do2.getDo2Date().before(DateManager.getDate("2022-06-01"))
							&& !do2.getFromDate().before(DateManager.getDate("2022-04-08"))) {
						;				
					} else {						
						formatValidity = false;	 			
						buffer.append(RATE_REQUIRED);	
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 				
			}
		} /*else if (appj.getRateReq() == true && do2.getRate()!=null && do2.getRate()>=0 
				&& do2.getDescription()!=null && do2.getDescription().trim().startsWith("CEA") 
				&& do2.getFromDate()!=null && !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			// Maximum admissible amount wef 01.07.2017 is Rs.27,000.00 per annum 
			if (do2.getRate()==0) {
				formatValidity=false;
				buffer.append(CEA_RATE_IS_ZERO);
			} else if ((do2.getDescription().trim().equals("CEA") || do2.getDescription().trim().equals("CEATWN")) && do2.getRate()>27000) {
			 	formatValidity = false; 
				buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);					 
			} else if ((do2.getDescription().trim().equals("CEAHC") || do2.getDescription().trim().equals("CEAHCT")) && do2.getRate()>54000) {
				formatValidity = false;
				buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);
			}			
		} */ // commented and added the following on 19.04.2024.
		
		else if (appj.getRateReq() == true && do2.getRate()!=null && do2.getRate()>=0 
				&& do2.getDescription()!=null && do2.getDescription().trim().startsWith("CEA") 
				&& do2.getFromDate()!=null && !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			// Maximum admissible amount wef 01.07.2017 is Rs.27,000.00 per annum 
			if (do2.getRate()==0) {
				formatValidity=false;
				buffer.append(CEA_RATE_IS_ZERO);
			} else if ((do2.getDescription().trim().equals("CEA") || do2.getDescription().trim().equals("CEATWN")) && do2.getRate()>27000) {
				//do2.getFromDate().after(DateManager.getSeventhCpcDaFiftyPercentDate()) -1
				if (do2.getFromDate().compareTo(DateManager.getSeventhCpcDaFiftyPercentDate())>=0
						&& do2.getToDate().after(DateManager.getSeventhCpcDaFiftyPercentDate())) {
					if (do2.getRate()>33750) {
						formatValidity = false; 
						buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);	
					}
					//do2.getFromDate().after(DateManager.getSeventhCpcDaFiftyPercentDate()) -2
				} else if (do2.getFromDate().compareTo(DateManager.getSeventhCpcDaFiftyPercentDate())>=0
						&& do2.getToDate().after(DateManager.getSeventhCpcDaFiftyPercentDate())) {
					if (do2.getRate()>28688) {
						formatValidity = false; 
						buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);	
					}
				} else {
					formatValidity = false; 
					buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);			
				}			 						 
			} else if ((do2.getDescription().trim().equals("CEAHC") || do2.getDescription().trim().equals("CEAHCT")) && do2.getRate()>54000) {
				//do2.getFromDate().after(DateManager.getSeventhCpcDaFiftyPercentDate()) - 3
				if (do2.getFromDate().compareTo(DateManager.getSeventhCpcDaFiftyPercentDate())>=0
						&& do2.getToDate().after(DateManager.getSeventhCpcDaFiftyPercentDate())) {
					if (do2.getRate()>67500) {
						formatValidity = false; 
						buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);	
					}	
					//do2.getFromDate().after(DateManager.getSeventhCpcDaFiftyPercentDate()) - 4
				} else if (do2.getFromDate().compareTo(DateManager.getSeventhCpcDaFiftyPercentDate())>=0
						&& do2.getToDate().after(DateManager.getSeventhCpcDaFiftyPercentDate())) {
					if (do2.getRate()>57375) {
						formatValidity = false; 
						buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);	
					}
				} else {
					formatValidity = false; 
					buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);			
				}	
			//	formatValidity = false;
			//	buffer.append(CEA_RATE_MORE_THAN_MAXIMUM);
			}			
		} else if (appj.getRateReq() == false && do2.getRate() != null	&& do2.getRate() != 0) {
			if (do2.getDescription().endsWith("INC")) {
				if (do2.getFromDate() != null &&
				   !do2.getFromDate().before(DateManager.getSixthCpcDate())) {
					formatValidity = false;
					buffer.append(RATE_SHOULD_BE_BLANK);
				}
			} else {
				formatValidity = false;
				buffer.append(RATE_SHOULD_BE_BLANK);
			}
		}
		if (appj.getRefDo2NoReq()==true && do2.getRefDo2No() != null) {

			if (do2.getRefDo2No().length() != 15) {
				formatValidity = false;
				buffer.append(INVALID_REFDO2_LENGTH);
			} else if (do2.getRefDo2No().length() == 15
					&& !isRefDo2LengthsOk(do2)) {
				buffer.append(INVALID_REF_DO2_FORMAT);
				formatValidity = false;
			} else if (!isRefDo2EarlierThanCurrentDo2()) {
				if (do2.getDescription().equals("CEA") || do2.getDescription().equals("REDESN") || do2.getDescription().equals("CEATWN")
					      || do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CILQ") 
					      || do2.getDescription().equals("REJAL") || do2.getDescription().equals("REJCL")
					      || do2.getDescription().equals("REJSL") || (do2.getDescription().equals("REJLVE"))
					      || do2.getDescription().equals("REP")
					      || do2.getDescription().equals("SEVREP")
					      || do2.getDescription().equals("FINE")
					      || do2.getDescription().equals("FAA")
					      || do2.getDescription().equals("RMA")
					      || do2.getDescription().equals("OUTFIT")
					      || do2.getDescription().equals("REGSL")
					      || do2.getDescription().equals("SL")
					      || do2.getDescription().equals("MILRI")
					      || do2.getDescription().equals("JCNO")
					      || do2.getDescription().equals("OPTFIX")
					      || (do2.getDescription().equals("RISK") && do2.getColumnA().equals("CEASED"))
					      || (do2.getDescription().equals("HAUCA") && do2.getColumnA().equals("CEASED"))
					      || (do2.getDescription().equals("ENCFIN") && 
					  		  do2.getRefDo2No() != null	&& do2.getRefDo2No().trim().length() != 0 && (do2.getRefDo2No().charAt(0) != do2.getDo2Type().charAt(0)))) {
					;
				} else {
					formatValidity = false;
					buffer.append(REF_DO2_LATER_THAN_DO2);
				}
			}
		}

		if (!do2.getDescription().equals("OUTFIT")  && !do2.getDescription().equals("REDESN") && 
			!do2.getDescription().equals("JCNO")    && !do2.getDescription().equals("CEA") && 
			!do2.getDescription().equals("CEATWN") && !do2.getDescription().equals("CEAHC") &&
			!do2.getDescription().equals("HSBTWN") && !do2.getDescription().equals("HOSSUB") &&
			!do2.getDescription().equals("PBNCTR") && !do2.getDescription().equals("OPTFIX") &&
			!do2.getDescription().equals("ENCFIN") && 
			do2.getRefDo2No() != null	&& do2.getRefDo2No().trim().length() != 0) {
			if (do2.getRefDo2No().charAt(0) != do2.getDo2Type().charAt(0)) {
				formatValidity = false;
				buffer.append(INCOMPATIBLE_DO2_TYPES);
			}
		}
		
		if (do2.getDescription().equals("OPTFIX")) {	 
			if (do2.getDo2Date().before(new Date((new GregorianCalendar(2019, Calendar.JUNE, 1).getTimeInMillis())))) {
				formatValidity=false;
				buffer.append(DO2_DATE_BEFORE_OPTFIX_APPROVAL_DATE);		 
			}			
			
		/*	if (do2.getDo2Date().after(DateManager.getOptfixPublicationEndDate())) {
				formatValidity=false;
				buffer.append(DO2_DATE_AFTER_OPTFIX_PUBLICATION_END_DATE);			 
			}*/
			
	/*		if (do2.getFromDate().after(DateManager.getOptfixPublicationEndDate()) 
					|| (do2.getToDate()!=null && do2.getToDate().after(DateManager.getOptfixPublicationEndDate()))) {
				formatValidity=false;
				buffer.append(INCORRECT_FORMAT_USED_FOR_OPTFIX_AFTER_ENDDATE);
			}
		Commented on 17.05.2021 - EME obsn. */
			
			if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
					|| (do2.getToDate()!=null && do2.getToDate().before(DateManager.getSeventhCpcDate()))) {
				formatValidity=false;
				buffer.append(FORMAT_VALID_ONLY_IN_7CPC); 
			}
			
	      	if (do2.getColumnA() == null 
	      			|| (do2.getColumnA()!=null && !Do2Manager.getOptfixAllowedDescriptions().contains(do2.getColumnA().trim()))) {
	       		formatValidity=false;
				buffer.append(INVALID_ABC_COMBINATION);
	       	}
			   
		   	if (do2.getColumnB() == null
		   			|| (do2.getColumnB()!=null && !Do2Manager.getOptfixValidColumnB().contains(do2.getColumnB().trim()))) {
	       		formatValidity=false;
				buffer.append(INVALID_ABC_COMBINATION); 
	       	}  	    
		}		
		subMessage = buffer.toString();
		return formatValidity;
	}

	public boolean isRefDo2LengthsOk(Do2 do2) {
		int startYear = 1985;
		boolean validity = true;
		String[] do2Parts = Utils.getRefDo2Parts(do2.getRefDo2No());
		
		if (do2Parts[0].length() != 1 || do2Parts[0].contains("/")) {
			validity = false;

		} else if (do2Parts[1].length() != 4 || do2Parts[1].equals("0000")
				|| do2Parts[1].contains("/")) {
			validity = false;
			
		} else if (do2Parts[2].length() != 3 || do2Parts[2].equals("0000")
				|| do2Parts[2].contains("/")) {
			validity = false;
			
		} else if (do2Parts[3].length() != 4 || do2Parts[3].equals("0000")
				|| do2Parts[3].contains("/")) {
			validity = false;
			
		} else if (Integer.parseInt(do2Parts[3]) < startYear) {
			validity = false;
			
		}
		return validity;
	}


	public boolean isValidCancelDo2() {
			
			boolean validity = false;	       
			subMessage = "";
			if(do2.getColumnA()==null || do2.getColumnA().trim().length()==0){
				subMessage="ColumnA is Blank. ColumnA Is Required";
				return false;
			}
			List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
	       

			if (refDo2List.size() == 0) {
				subMessage = REF_DO2_NOT_FOUND;
				return false;
			} else {
				for (Do2 refDo2 : refDo2List) {

					if (refDo2.getTranscriptionType() != null &&
						refDo2.getTranscriptionType() == Do2Manager.SYSTEM_GENERATED) {
						continue;
					}
					if  ((refDo2.getArmyNo().equals(do2.getArmyNo()) || refDo2.getEmployee() == do2.getEmployee())
						&& refDo2.getFromDate().equals(do2.getFromDate())
						&& refDo2.getDescription().equals(do2.getColumnA())) {
						if (refDo2.getStatus().equals(Do2Manager.DO2_CANCEL_STATUS)) {
							do2.setStatus(Do2Manager.INVALID);
							validity=false;
							subMessage = REF_DO2_ALREADY_CANCELLED;
						} else if (!refDo2.getStatus().equals(Do2Manager.VALID)) {
								if(refDo2.getAuditCage() != null && refDo2.getAuditCage() == true){
									AuditCage refAuditCage = Utils.getAuditCage(refDo2);
									if(refAuditCage != null ){									
										refAuditCage.setCancelled(true);	
									}
								}
								String refDo2No = Utils.getReferenceDo2No(do2);
								refDo2.setReason("Original Status was "+refDo2.getStatus() + ".\n"
										+ REF_DO2_MARKED_CANCELLED + "   " + refDo2No);
								do2.setStatus(Do2Manager.VALID);
								refDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
								do2Repo.save(refDo2);
								do2Repo.save(do2);
				// validity is made false to avoid going to AuditcageProcessor.java
								validity=false;
								subMessage = null;
						} else if (refDo2.getStatus().equals(Do2Manager.VALID)) {
						/*	// Following code added on 07.12.2021 to handle award-asspp published with fromdate prior to 27.08.2019 and getting cancelled.
									if (do2.getDescription().equals("CANCEL") && do2.getColumnA().equals("AWARD") 
											&& do2.getFromDate().before(DateManager.getAssppAwardEffectiveDate())
											&& refDo2.getDescription().equals("AWARD") && refDo2.getColumnA().equals("ASSPP")
											&& refDo2.getFromDate().equals(DateManager.getAssppAwardEffectiveDate())
											&& refDo2.getToDate()!=null && refDo2.getToDate().equals(do2.getFromDate())) {
										
										do2.setToDate(do2.getFromDate());
										do2.setFromDate(DateManager.getAssppAwardEffectiveDate());
									}
							*/
								if (refDo2.getAuditCage() != null && refDo2.getAuditCage() == true){
									AuditCage refAuditCage = Utils.getAuditCage(refDo2);
									if(refAuditCage == null) {
										do2.setStatus(Do2Manager.INVALID);
										subMessage = NO_CORRESPONING_AUDITCAGE;
										do2Repo.save(do2);
										validity=false;
									} else {
										validity =true;
										break;
									}
										
								} else {
									validity =true;
									break;
								}							
						} else {
							validity=false;
						}
					} else {
						validity=false;
						subMessage = INCORRECT_REF_DO2;
					}
				}
			}
	        if (!validity){
				do2Repo.save(do2);
	        }
			return validity;
	}

	public boolean isRefDo2EarlierThanCurrentDo2() {
		String refDo2No = do2.getRefDo2No();// Eg:0/0012/132/2008

		Date do2Date = do2.getDo2Date();
		Calendar cal = new GregorianCalendar();
		cal.setTime(do2Date);

		String[] refDo2Parts = Utils.getRefDo2Parts(refDo2No);
		if (refDo2Parts[3].compareTo("" + cal.get(Calendar.YEAR)) < 0)
			return true;
		if (refDo2Parts[1].compareTo(do2.getDo2No()) < 0)
			return true;
		if (refDo2Parts[2].compareTo(do2.getDo2ItemNo()) < 0)
			return true;
		return false;
	}

	public boolean isAccountingCage() {
		List<AppendixJ> appjList =appjRepo.findAccountingCageDescription(); 
		if (appjList.size() > 0)
			return true;
		return false;
	}

	public boolean isValidDo2() {
		/*
		 * if (do2.getDescription()!=null && do2.getDescription().equals("BUNCH")) {
		 * return true; }
		 */
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date cbillbegDate=null;
		Date cbillendDate=null;
		StringBuffer buffer = new StringBuffer();
		validity = true;
		try {
		   cbillbegDate=DateManager.getDate("2013-04-01");
		   cbillendDate=DateManager.getDate("2014-07-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		if (Utils.isFsAccountClosed(do2.getArmyNo())) {
			//FacesMessages.instance().add(" THIS ACCOUNT ALREADY CLOSED ");
			do2.setReason(" THIS ACCOUNT ALREADY CLOSED ");
			return false;
		}	
		
		
		/*if (!Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && !Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
			if (!do2.getEmployee().getSubstantiveRank().getRankName().equals(Do2Manager.RECRUIT)) {
				if(!do2.getEmployee().getEnrolmentDate().equals(do2.getEmployee().getNbsubDate())) {
					if (!(do2.getDescription().equals("ATCORR") && do2.getReason()!=null && do2.getReason().startsWith("System"))) {
						if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
							AuditCage baseAuditCage = AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
							if (baseAuditCage!=null) {
								if (baseAuditCage.getPayLevel()==null || (baseAuditCage.getPayLevel()!=null && baseAuditCage.getPayLevel().equals("0"))) {
									 Date optCpcDate = null;
									 List<Do2> optcpcList = Utils.getEntityManager().createQuery("SELECT d from Do2 d where d.employee=:emp AND " +
									 		" d.description='OPTCPC' AND d.status='V' ").setParameter("emp", baseAuditCage.getEmployee()).getResultList();
									 if (optcpcList.size() > 0) {
										  optCpcDate = optcpcList.get(0).getFromDate();
									 }
									 if (optCpcDate==null || (optCpcDate!=null && do2.getFromDate().after(optCpcDate))) {
										do2.setReason("AU: Paylevel in auditcage is null or 0. Please feed ATCORR as on 01-01-2016 for updation of Paylevel.\n");
										validity = false;
										do2.setStatus(Do2Manager.INVALID);
										buffer.append(PAY_LEVEL_NOT_FOUND);
										if (do2.getId() == 0){
											Utils.getEntityManager().persist(do2);
										} else {
											Utils.getEntityManager().merge(do2);
										}
										return false;
									 }					
								}
							}
						}
					}
				}
				
			}
		}*/
		
		if (!isEffective(do2.getArmyNo())) {
			if(do2.getDescription().equals("DEATH")){ 
				return  true;
			}
			
			if(!do2.getFromDate().after(do2.getEmployee().getFsDueDate()) && do2.getDescription().equals("ATCORR")) {
				return true;
				
			}
			List<LpcRegister>lpcList = lpcRegisterRepo.findByEmployeeIdRecordStatusApprovedEcsGeneratedDischargeDateGreaterThan(do2.getEmployee().getId(), "F",DateManager.getNeImplementationDate());
			 
			if (lpcList == null || lpcList.size() == 0){	
				do2.setStatus("N");
				do2.setReason(NOT_EFFECTIVE);
				do2Repo.save(do2);
				return  false;					
			}
			if(employee.getFsDueDate()==null && lpcList!=null && lpcList.size()>0) {
				 employee.setFsDueDate(lpcList.get(0).getDischargeDate());
				 empRepo.save(employee);
			}
			if (do2.getFromDate().after(employee.getFsDueDate())) {
				if (do2.getDescription().startsWith("CEA")) {
					System.out.println(".........cea ano " +do2.getArmyNo()+"  frdt "+do2.getFromDate());
					if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						int validFromDateMonth = do2.getTjra()/100;
						int validFromDateYear  = do2.getTjra()%100;
						int validToDateMonth   = do2.getLra()/100;
						int validToDateYear    = do2.getLra()%100;
						int days = DateManager.getNoOfDaysInMonth(validToDateMonth, validToDateYear+2000);
						Date academicYearBeg = null; 
						Date academicYearEnd = null; 
						try {
							academicYearBeg = DateManager.getDate(validFromDateYear+2000, validFromDateMonth, 1);
							academicYearEnd = DateManager.getDate(validToDateYear+2000, validToDateMonth, days);
						} catch (Exception e) {
							Utils.handleException(e);
						}
						if (employee.getFsDueDate().after(academicYearBeg) && employee.getFsDueDate().before(academicYearEnd)) {
							;
						} else {
							do2.setStatus("N");
							do2.setReason("From date is after fs due date");
							do2Repo.save(do2);
							return  false;
						}
					} else {
						lpcList = null;
						if (do2.getFromDate()!=null && do2.getToDate()!=null) {
							lpcList =lpcRegisterRepo.findByEmployeeIdStatusApprovedEcsGeneratedAndDischargeDateBetween(do2.getEmployee().getId(),"F", do2.getFromDate(), do2.getToDate());
							System.out.println(".......lpc list size " +lpcList.size());
							if (lpcList!=null && lpcList.size()>0) {
								;
							} else {
								System.out.println("lllllllllllllllllll   employe statuscode " +employee.getStatusCode().getId()+"  ");
								if (employee.getStatusCode().getId()==15 && (do2.getStatus().equals("N")|| do2.getStatus().equals("P"))) {
									lpcList=lpcRegisterRepo.findFsAprovedAndEcsGeneratedForDeathIrla(employee.getId());
									System.out.println("............1111111111111111. lpclist size "+lpcList.size());
									if (lpcList!=null && lpcList.size()==1) {
										if (do2.getFromDate().after(employee.getFsDueDate())) {
											Double noOfYearsService = DateManager.getNoOfMonths(employee.getEnrolmentDate(), do2.getFromDate());
											System.out.println(".......enrol " +employee.getEnrolmentDate()+"  d2frdt " +do2.getFromDate());
											if (noOfYearsService>408) {
												do2.setStatus("I");
												do2.setReason("Claim Date Exceeds 34 Years Of Deceased's Service.");
												do2Repo.save(do2);
												return  false;
											}
											Double noOfYearsAge = DateManager.getNoOfMonths(employee.getBirthDate(), do2.getFromDate());
											System.out.println("..........age " +employee.getBirthDate()+"  drfrdt " +do2.getFromDate());
											if (noOfYearsAge>648) {
												do2.setStatus("I");
												do2.setReason("Claim Date Exceeds 54 Years Of Deceased's Age.");
												do2Repo.save(do2);
												return  false;
											}											 
										}
									} else {
										System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxx do2frdt " +do2.getFromDate()+"  emp fsduedate " +employee.getFsDueDate());
										if (do2.getFromDate().after(employee.getFsDueDate())) {
											do2.setStatus("N");
											do2.setReason("From date is after fs due date");
											do2Repo.save(do2);
											return  false;
										}		
									}
								} else if (do2.getFromDate().after(employee.getFsDueDate())) {
									do2.setStatus("N");
									do2.setReason("From date is after fs due date");
									do2Repo.save(do2);
									return  false;
								}		
							}
						} else if (do2.getFromDate().after(employee.getFsDueDate())) {
							do2.setStatus("N");
							do2.setReason("From date is after fs due date");
							do2Repo.save(do2);
							return  false;
						}		
					}				
				} else {
				  do2.setStatus("N");
				  do2.setReason("From date is after fs due date");
				  do2Repo.save(do2);
				  return  false;
			   } 
		    }
			
			if (do2.getDescription().equals("OPTFIX") && do2.getToDate()!=null 
					&& employee.getFsDueDate()!=null && do2.getToDate().after(employee.getFsDueDate())){
				  do2.setStatus("N");
				  do2.setReason("From date is after fs due date");
				  do2Repo.save(do2);
				  return  false;
			}
		}

		String description = do2.getDescription();
        
		if (Utils.isEmpThisMonthFs(do2.getEmployee())) {
			if (Do2Manager.getFsDischDescriptions().contains(description)) {
				do2.setReason("To be fed in following month");
				return false;
			}
		}
		
		/*if (Do2Manager.getFsDischDescriptions().contains(description)) {
			return true;
		}	*/	

		//Amended for NE
		
		if (do2.getTranscriptionType()!=null &&  do2.getTranscriptionType()==Do2Manager.SYSTEM_GENERATED && 
				(!Do2Manager.getValidationRequiredSystemGeneratedDescription().contains(do2.getDescription())
						|| !Do2Manager.getValidationRequiredSystemGeneratedNeDescription().contains(do2.getDescription()))) {
			return true;
		}
				
		if (description == null)
			description = "";
	
		if (do2.getDo2No() == null || do2.getDo2No().trim().length() == 0) {
			validity = false;
			buffer.append(DO2_NO_REQUIRED);
		}
		if (do2.getDo2ItemNo() == null || do2.getDo2ItemNo().trim().length() == 0) {
			validity = false;
			buffer.append(DO2_ITEM_NO_REQUIRED);
		}

		if (do2.getToDate() == null && do2.getTjra() != null
				&& do2.getTjra() == 1 && isAccountingCage()) {
			List<AppendixJ> appjList = appjRepo.findAccountingCageDescription();
			if (appjList.size()>0){
				if (appjList.get(0).getToDateReq()==true){
					do2.setToDate(do2.getFromDate());		
				}
			}			
		}

		if (do2.getToDate() == null	&& DateManager.isValidTime(do2.getColumnB())) {
			do2.setToDate(do2.getFromDate());
		}
		
		if (do2.getFromDate() != null && (!do2.getFromDate().before(cbillbegDate) && !do2.getFromDate().after(cbillendDate)) &&
		   (do2.getDescription().equals("TFRGTH") || do2.getDescription().equals("TFRGTL") || 
		   (do2.getDescription().equals("ENCASH") && do2.getColumnA().equals("LTC")))) {
			validity = false;
			buffer.append("To be claimed as advance through IAFA 194 between 1.4.2013 and 31.7.2014");	
		}
		  
		if (do2.getFromDate() != null) {
			if (!(description.equals(Do2Manager.STRINC) ||  description.equals(Do2Manager.SPINC)) && isFromDateBeforeAllowanceBirth()) {
				validity = false;
				buffer.append(FROM_DATE_BEFORE_ALLOWANCE_BIRTH);
			} else if (isFromDateAfterAllowanceDeath()) {
				validity = false;
				buffer.append(FROM_DATE_AFTER_ALLOWANCE_DEATH);
			}  
		} else {
			validity = false;
			buffer.append(FROM_DATE_REQUIRED);
		}		

		if (do2.getToDate() != null) {
			if (DateManager.isValidToDate(do2.getToDate())) {
				if (isFromDateAfterAllowanceDeath()) {
					validity = false;
					buffer.append(TO_DATE_AFTER_ALLOWANCE_DEATH);
				} else {
					if (do2.getToDate().after(do2.getDo2Date())
							&& Do2Manager.getToDateBeforeDo2Date().contains(do2.getDescription())) {
						if (Utils.isEmpThisMonthFs(do2.getEmployee())) {
							;
						} else {
							validity = false;
							buffer.append(ARREARS_CLAIM);
						}						
					} else if (do2.getToDate().after(Utils.getLastDayOfCurrentQuarter())
							&& !(Do2Manager.getToDateBeyondCurrentQuarter().contains(do2.getDescription())
									|| Do2Manager.getIncrCancelDescription().contains(do2.getDescription()))) {
						validity = false;
						buffer.append(BEYOND_CURRENT_QUARTER);
					}
				}
			} else {
				if (do2.getDescription().equals("CONSER") && Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
					;
		//		} else if (do2.getDescription().equals("PFWDL") && Utils.getPao().getPaoCode().equals(Do2Manager.PAO_AOC)){
				} else if (do2.getDescription().equals("PFWDL")) {
					;
				} else {
					validity = false;
					buffer.append(INVALID_TO_DATE);
				}
			}

		}

		if (do2.getFromDate() != null && do2.getToDate() != null && !do2.getDescription().equals("ACQUIT")) {
			if (DateManager.isValidFromDate(do2.getFromDate()) && DateManager.isValidToDate(do2.getFromDate())) {
				if (DateManager.isFromDateAfterToDate(do2.getFromDate(), do2.getToDate())) {
					validity = false;
					buffer.append(FROM_DATE_AFTER_TO_DATE);
				}
			} else {
				validity = false;
				buffer.append(INVALID_FROM_DATE);
			}
		}

		if (!isValidGroup(do2.getDo2Group())) {
			validity = false;
			buffer.append(DO2_GROUP_NOT_VALID);
		}

		if (!isValidType(do2)) {
			validity = false;
			buffer.append(DO2_TYPE_NOT_VALID);
		}
		
		if (do2.getDescription().equals("SRGRTY") || do2.getDescription().equals("DCRG")) {
		   if (Utils.getPao().getPaoCode().equals("94")){
			   if (!do2.getDo2Type().equals("1")){
				   buffer.append(INVALID_RO_PUBLICAITON); 
				   validity = false;
			   }
		   } else {
			   buffer.append("This description is valid only in PAO DSC");
			   validity = false;
		   }
		}		
		
		if (Do2Manager.getRecordOfficeDescriptions().contains(do2.getDescription())) {
			if (isValidRecordOfficePublication(do2)) {
				;			
			} else {
				if (do2.getDescription().equals("REFIXP") && (!Utils.getPao().getPaoCode().equals("94"))){
					;
				} else {
					validity = false;
					buffer.append(INVALID_RO_PUBLICAITON);
				}
			}
		}
		
		if (do2.getDo2Date() == null) {
			validity = false;
			buffer.append(DO2_DATE_REQUIRED);
		} else if (DateManager.isFutureDate(do2.getDo2Date())) {				
			validity = false;
			buffer.append(DO2_DATE_IN_FUTURE);
		}
		
		if (do2.getDo2Date() != null) {
			if (isDo2DateBeforeEnrolment()) {
				validity = false;
				buffer.append(DO2_DATE_BEFORE_ENROLMENT_DATE);
			}
		}

		if (do2.getFromDate() != null) {
		   if (isFromDateAfterCurrentQuarterEnding(do2.getFromDate())) {
			   
	//		   if (employee.getRegularTaFlag().equals("T") && description.startsWith("FS") && do2.getInputQe().equals("05/2024")) {
			   if (employee.getRegularTaFlag().equals("T") && description.startsWith("FS")) { // removed on 29.07.2024
					return true;
				}
			   
			  if (description.equals("DISCH") || description.equals("RETN") || description.equals("NORETN") || description.equals("PROVFS") ||
					  (description.equals("ENCFIN") && do2.getColumnA().equals("DISCH")) ||
					  (description.equals("CONSER") && Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) ||
				 (description.equals("CANCEL") && (do2.getColumnA().equals("DISCH")
						 || do2.getColumnA().equals("RETN") || do2.getColumnA().equals("NORETN") || do2.getColumnA().equals("PROVFS")))) {
				  ;
			  } else {
				   validity = false;
				   buffer.append(FROM_DATE_AFTER_CURRENT_QUARTER);
			  }
		   }
		   if (isFromDateBeforeEnrolment()) { // added on 09/12/2020
				validity = false;
				buffer.append(FROM_DATE_BEFORE_ENROLMENT_DATE);
		   }
		}
		
		if (!isFromDateBeforeDo2Date(do2.getDo2Date(), do2.getFromDate())) {
			//***Lines added condition to exclude  encash,tfrgth for fs case
			if (do2.getDo2Date() != null && do2.getFromDate() != null) {
				if (description.equals("PROVFS") || (description.equals("CANCEL") && do2.getColumnA()!=null && do2.getColumnA().equals("PROVFS"))) {
					return true;
				}
				
				if (do2.getDo2Group().equals("IA") && !do2.getDescription().equals("CANCEL") && do2.getColumnA()!=null && do2.getColumnA().equals("CEASED")) {
					return true;
				}
				
				//added on 29.01.2024
				if (employee.getRegularTaFlag().equals("T") && description.startsWith("FS") && do2.getInputQe().equals("05/2024")) {
					return true;
				}
				
				if (description.equals("DISCH")  || description.equals("RETN") || description.equals("NORETN") ||
					(description.equals("CONSER") && Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) ||
					((do2.getDescription().equals("FSDSCH") || do2.getDescription().equals("FSDSMS")  || do2.getDescription().equals("FSINVL") 
							|| do2.getDescription().equals("FSRMVL") || do2.getDescription().equals("FSDETH") || do2.getDescription().equals("FDAILY") || 
					(description.equals("ENCFIN") && do2.getColumnA().equals("DISCH")) || description.equals("MACPS")) && 
					(do2.getFromDate() != null && !do2.getFromDate().after(getLastDayOfPublicationMonth(do2.getDo2Date())))) ||  
					(description.equals("ENCASH") && do2.getColumnA()!=null && !do2.getColumnA().equals("LTC")) ||	
					((description.equals("TFRGTH") || description.equals("TFRGTL")) && do2.getColumnB()!=null && do2.getColumnB().equals("FS")) ||
					(description.equals(Do2Manager.CANCEL) && (do2.getColumnA().equals("DISCH")|| do2.getColumnA().equals("ENCASH") ||
					 do2.getColumnA().equals("TFRGTH") || do2.getColumnA().equals("TFRGTL") || do2.getColumnA().equals("NORETN") ||
					 do2.getColumnA().equals("RETN") || do2.getColumnA().equals("MACPS") || do2.getColumnA().equals("ENCFIN")))){
					validity = true;
				} else {	
					validity = false;
					buffer.append(FROM_DATE_LATER_THAN_DO2_DATE);
				}
			}
		}
		
		if (!isValidArmyNo()) {
			validity = false;
			buffer.append(EMPLOYEE_NOT_FOUND);
		}
		
		if ((description.equals(Do2Manager.CANCEL)
				&& do2.getColumnA() != null	&& Do2Manager.getValidCancelExceptions().contains(do2.getColumnA())
				|| description.equals(Do2Manager.REINST))) {
			validity = true;			
		} 

		if (!isFormatValid(do2)) {
			validity = false;
			buffer.append(NOT_IN_APPENDIX_J_FORMAT);
			buffer.append(subMessage);
			do2.setStatus("MR");
		}
		
		if (do2.getUnitCode() == null || !Utils.isValidUnit(do2.getUnitCode())) {
			validity = false;
			buffer.append(UNIT_NOT_VALID);
		} else {
			do2.setUnit(unitRepo.findByUnitCode(do2.getUnitCode()));
		}
        if (do2.getTranscriptionType() !=null && !do2.getTranscriptionType().equals("S")){
			if (timeBarPublication(do2) && do2.getTimeBarSanction() != null	&& !do2.getTimeBarSanction()) {
				validity = false;
				buffer.append(TIME_BAR_PUBLICATION);	
			}
        }

		if (validity) {// added code
			if (appjList != null && appjList.size() > 0) {
				if (appjList.get(0).getAuditAccountCage().equals("AT")) {
					do2.setAuditCage(true);
				} else {
					do2.setAuditCage(false);
					
				}
			}
			if (appjList != null && appjList.size() > 0 && (appjList.get(0).getAuditAccountCage().equals("AT"))) {
				if (do2.getDescription().equals("CANCEL") && cancelAppjList != null && cancelAppjList.size() > 0) {
					if (cancelAppjList.get(0).getAuditAccountCage().equals("AT")){
						Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
						if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_JKLI) || 
							Utils.getPao().getPaoCode().equals(Do2Manager.PAO_LADAK_SCOUTS)){
							if (!do2.getFromDate().after(DateManager.getOtherCutOffDate())) {
								do2.setStatus(Do2Manager.BACK_DATED);     
								validity = false;
								return false;
							}
						}
						if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
							if (!do2.getFromDate().after(DateManager.getEstt22CutOffDate())) {
								do2.setStatus(Do2Manager.BACK_DATED);     
								validity = false;
								return false;
							}
						}
						
						if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)){
							if (!do2.getFromDate().after(DateManager.getApsCutOffDate())) {
								do2.setStatus(Do2Manager.BACK_DATED);     
								validity = false;
								return false;
							}
						}
						
						
						if (emp !=null && emp.getRegularTaFlag()!=null && emp.getRegularTaFlag().equals("T")){
							Date taCutOffDate = DateManager.getTaCutOffDate();
							if (Utils.getPao().getPaoCode().equals("62")){
								taCutOffDate = DateManager.getRRRCTaCutOffDate();
							}
							if (!do2.getFromDate().after(taCutOffDate)) {
								do2.setStatus(Do2Manager.BACK_DATED);
								do2.setReason(BACK_DATED_DOII);
								validity = false;
								return false;
							}
						}						
						if (!do2.getFromDate().after(DateManager.getCutOffDate())) {
							do2.setStatus(Do2Manager.BACK_DATED);     
							validity = false;
							return false;
						}
					}
				} else if (!do2.getDescription().equals("CANCEL")) {
					Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
					if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_JKLI) || 
						Utils.getPao().getPaoCode().equals(Do2Manager.PAO_LADAK_SCOUTS)){
						if (!do2.getFromDate().after(DateManager.getOtherCutOffDate())) {
							do2.setStatus(Do2Manager.BACK_DATED);     
							validity = false;
							return false;
						}
					}
					if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
						if (!do2.getFromDate().after(DateManager.getEstt22CutOffDate())) {
							do2.setStatus(Do2Manager.BACK_DATED);     
							validity = false;
							return false;
						}
					}
					
					if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)){
						if (!do2.getFromDate().after(DateManager.getApsCutOffDate())) {
							do2.setStatus(Do2Manager.BACK_DATED);     
							validity = false;
							return false;
						}
					}
					
					if (emp !=null && emp.getRegularTaFlag()!=null && emp.getRegularTaFlag().equals("T")) {
						Date taCutOffDate = DateManager.getTaCutOffDate();
						if (Utils.getPao().getPaoCode().equals("62")){
							taCutOffDate = DateManager.getRRRCTaCutOffDate();
						}
						if	(!do2.getFromDate().after(DateManager.getTaCutOffDate())){
							do2.setStatus(Do2Manager.BACK_DATED);
							do2.setReason(BACK_DATED_DOII);
							validity = false;
							return false;
						}
					}				
					if (!do2.getFromDate().after(DateManager.getCutOffDate())) {
						do2.setStatus(Do2Manager.BACK_DATED);     
						validity = false;
						return false;
					}
				}
			}

			if (isDuplicatePublication()) {
				buffer.append(subMessage);
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
				// buffer.append(DO2_DUPLICATE_PUBLICATION);
			}
		
			if (validity && isOverLapping()) {
				validity = false;
				buffer.append(do2.getReason());
				do2.setStatus(Do2Manager.INVALID);
			}

			if (validity && do2.getRefDo2No() != null
					&& do2.getRefDo2No().trim().length() != 0) {
				if (do2.getDescription().equals("CEA") 
						|| do2.getDescription().equals("REJAL") || do2.getDescription().equals("REJCL") || (do2.getDescription().equals("REJLVE"))
						|| do2.getDescription().equals("REJSL") || do2.getDescription().equals("CEATWN")
						|| do2.getDescription().equals("CEAHC") || do2.getDescription().equals("CILQ")
						|| do2.getDescription().equals("REP")   || do2.getDescription().equals("ABF")
					    || do2.getDescription().equals("SEVREP")  || do2.getDescription().equals("JCNO")
					    || do2.getDescription().equals("FINE")
					    || do2.getDescription().equals("FAA")
					    || do2.getDescription().equals("RMA")
					    || do2.getDescription().equals("OUTFIT")
					    || do2.getDescription().equals("REGSL")
					    || do2.getDescription().equals("SL")
						|| Do2Manager.STOP_ALL.contains(do2.getDescription())) {
					;
				} else {
					// amended to stop verifying the CEA Ref Do2
					if(do2.getDescription().equals("REDESN")){
						if(Utils.getReferenceDo2ListForRedesn(do2).size()==0){
							do2.setStatus(Do2Manager.INVALD);//changed on 22/09/2025 from pending to Invalid
							buffer.append(REF_DO2_NOT_FOUND);
							validity = false;
						}
						
					}else{
						if (!do2.getDescription().equals("CANCEL") && do2.getColumnA()!=null && do2.getColumnA().equals("CEASED")) {
							validity = true;
						}else if (Utils.getReferenceDo2List(do2).size() == 0) {
							do2.setStatus(Do2Manager.INVALID);//changed on 22/09/2025 from pending to Invalid
							buffer.append(REF_DO2_NOT_FOUND);
							validity = false;
						}		
					}
				}
			}

			if (description.equals(Do2Manager.CANCEL)) {
			
				if (!Do2Manager.STOP_ALL.contains(do2.getColumnA())) {
					if (!isValidCancelDo2()) {
						if (do2.getStatus() == null) {
							do2.setStatus(Do2Manager.PENDING);
						}
						
						do2.setReason(subMessage);
						
						return false;
					} else {
			
						do2.setReason(null);
						do2.setStatus(Do2Manager.VALID);
			
						return true;
					}
				} else {
					
					if (!isValidCancelDo2()) {
						if (do2.getStatus() == null) {
							do2.setStatus(Do2Manager.PENDING);							
						}
						
						do2.setReason(subMessage);
						
						return false;
					}
					
					Do2 refDo2 = Utils.getReferenceDo2(do2, do2.getColumnA());

					if (do2.getColumnA().equals(Do2Manager.TFRDEP)) {
						if (refDo2 != null
								&& !Do2Manager.getValidStopPayDeputation()
										.contains(refDo2.getColumnA())) {
							return true;
						}
					}

					/*if (refDo2 != null) {
						AuditCage stopAllAuditCage = Utils.getAuditCage(refDo2);
						
						if (stopAllAuditCage.getAbsentFlag() != null && stopAllAuditCage.getAbsentFlag() == false) {
							do2.setStatus(Do2Manager.INVALID);
							buffer.append(OSL_ALREADY_REGULARISED);
							do2.setReason(buffer.toString());
							entityManager.persist(do2);
							return false;
						}

					}*/
				}
				
			}

			if (isRestorePay(do2) && validity) {				
				return true;
			}

			if (!do2.getDescription().equals("CANCEL")) {
				 
				if (Utils.isAbsent(do2)) {
					validity = false;
					if (do2.getDescription().equals("PFWDL")) {
						do2.setStatus(Do2Manager.INVALID);
					} else {
						do2.setStatus(Do2Manager.PENDING);
					}
					buffer.append(ABSENT);
				}
				if (Utils.isOnDeputation(do2)) {
					validity = false;
					do2.setStatus(Do2Manager.PENDING);
					buffer.append(DEPUTATION);
				}

			}

		} else {
			do2.setStatus(Do2Manager.INVALID);
		} 
		
		message = buffer.toString();
	
		do2.setReason(message);
		do2Repo.save(do2);
		
		return validity;
	}

	private boolean isRestorePay(Do2 do2) {
		if (Do2Manager.getRestorePayDescriptions().contains(do2.getDescription()))
			return true;
		return false;
	}

	Employee employee;
	Date maxDate;

	static public boolean timeBarPublication(Do2 do2) {
		if (do2.getDescription()!=null && do2.getDescription().equals("BUNCH")) {
			return false;
		}

        //commented on 02032026 as per the order no.C/7099/policy/sapc/2023 dated 18/08/2023 samanvyay ticket-SOS2026022600011
//		if (do2.getDescription()!=null && do2.getDescription().equals("OPTFIX")) {
//			return false;
//		}
				
		if (do2.getDescription()!=null && do2.getDescription().equals(Do2Manager.MACPS)) {
			return false;
		} 
			
		if (do2.getDescription()!=null && do2.getDescription().equals("RISK")){
			return false;
		}
		
		if (do2.getDescription()!=null && do2.getDescription().equals("HAUCA")){
			return false;
		}
	 	
		if (do2.getColumnA()!=null && do2.getColumnA().equals(Do2Manager.CEASED)) {
			return false;
		} // added on 12.04.2023
		
		if (do2.getFromDate() == null)
			return false;
		
		if (do2.getDescription()!=null && do2.getDescription().equals(Do2Manager.CANCEL)){
			return false;
		}
		  
		if (do2.getDescription()!=null && do2.getDescription().equals("STEPUP")){
			return false;
		}
		if (do2.getDescription()!=null && do2.getDescription().equals("RETN")){
			return false;
		}
		if (do2.getDescription()!=null && (do2.getDo2Group().equals("IIA") || do2.getDo2Group().equals("IIB"))){
			return false;
		}
		
		if (do2.getDescription()!=null && do2.getDescription().startsWith("FS")){
			return false;
		}
		
		/*
		if (do2.getDescription()!=null && do2.getDescription().equals(Do2Manager.AICTGP)){
			return false;
		}
		
		if (do2.getDescription()!=null && do2.getDescription().equals("OPTCPC")) {
			return false;
		}
	 	
		if (do2.getDescription()!=null && do2.getDescription().equals("TRGALL")) {
			return false;
		}
		
		if (do2.getDescription()!=null && do2.getDescription().equals("RUMCIG") 
				&& !do2.getDo2Date().after(DateManager.getCutOfDateForRumcigPublication())) {
			return false;
		}
		*/ // Timebar check removed on 09.07.2024.  Initially timebar check avoided due to introduction of description wef retrospective date. 
		
		double monthCount = (DateManager.getNoOfMonths(do2.getFromDate(), do2.getDo2Date()));
		
		if (do2.getDescription()!=null && (do2.getDescription().startsWith("CEA") || do2.getDescription().equals("HOSSUB")
				|| do2.getDescription().startsWith("HSBTWN")) 
				&& do2.getFromDate().after(DateManager.getSeventhCpcAllowanceDate())) {  //Code added on 19.11.2020.  HSBTWN, HOSSUB added on 24-06-2022
			monthCount=0;
			if (do2.getToDate()!=null) {
				monthCount = (DateManager.getNoOfMonths(DateManager.getNextDate(do2.getToDate()), do2.getDo2Date()));
				if (monthCount<=24) {
					return false;
				}
			}
		}
		if (monthCount <= 24 || (monthCount > 36 && monthCount <= 48
						&& do2.getPunishment() != null && do2.getPunishment().equals("CM"))
				|| (monthCount > 24 && monthCount <= 36
						&& do2.getPunishment() != null && do2.getPunishment().equals("OC")))
			return false;
		
		if (do2.getDescription()!=null && do2.getDescription().startsWith("CEA")) {  //Code added on 19.11.2020.
			monthCount=0;
			if (do2.getToDate()!=null) {
				monthCount = (DateManager.getNoOfMonths(DateManager.getNextDate(do2.getToDate()), do2.getDo2Date()));
				if (monthCount<=24) {
					return false;
				}
			}
		}
		
		return true;
	}                
	
	public boolean isMasterExists(Do2 do2){
		List<Employee> emps = empRepo.findByArmyNoOldArmyNo(do2.getArmyNo());
		return emps.size() !=0;
	}
	
	public boolean checkforCancelColumnA(Do2 do2){
		boolean columnsValidity=true;
		List<String> do2Descriptions = getDo2Descriptions();
		if (do2.getColumnA() == null){
			columnsValidity=false;
			return columnsValidity;
    	}
    	if (do2.getColumnA() != null && do2.getColumnA().equals("CANCEL")){
    		columnsValidity=false;
    		return columnsValidity;
    	}
    	if (do2.getColumnA() != null && do2.getColumnA().startsWith("FS")){
    		columnsValidity=false;
    		return columnsValidity;
    	}
		if (!do2Descriptions.contains(do2.getColumnA())) {
			columnsValidity=false;
		}
		return columnsValidity;
	}
	public boolean checkforTradeGroupClass(String trade, char group, char gpclass){
		boolean columnsValidity=false;
		List<GroupCategory>catgList=gcRepo.findBySoldierCategory(trade);
		if (catgList.size() == 0){
			return columnsValidity;
		}	
		if (group == ' ') {
			return columnsValidity;
		}
		for (GroupCategory gc : catgList){					    
			if (gc.getToDate() != null && (!do2.getFromDate().before(gc.getFromDate()) && !do2.getFromDate().after(gc.getToDate()))) {
    			if (group == gc.getSoldierGroup().charAt(0)){
    				columnsValidity=true;
    				return columnsValidity;
    			}
    			if (gpclass == ' ') {
    				columnsValidity=true;
    				return columnsValidity;
    			} else {
    				if ((Integer.parseInt(gpclass+"") > (Integer.parseInt(gc.getMaximumClass()+"") - 1)) && 
    					(Integer.parseInt(gpclass+"") < (Integer.parseInt(gc.getEntryClass()+"") + 1))) {
    					columnsValidity=true;
    					return columnsValidity;
    				}
    			}
    		} else if (gc.getToDate() == null && !do2.getFromDate().before(gc.getFromDate())) {
    			if (group == gc.getSoldierGroup().charAt(0)){
    				columnsValidity=true;
    				return columnsValidity;
    			}
    			if (gpclass == ' ') {
    				columnsValidity=true;
    				return columnsValidity;
    			} else {
    				if ((Integer.parseInt(gpclass+"") > (Integer.parseInt(gc.getMaximumClass()+"") - 1)) && 
    					(Integer.parseInt(gpclass+"") < (Integer.parseInt(gc.getEntryClass()+"") + 1))) {
    					columnsValidity=true;
    					return columnsValidity;
    				}
    			}
    		} 
		}		
		return columnsValidity;
	}
	public Date getLastDayOfPublicationMonth(Date date1){
		Date date2 = null;
		Calendar currentCal = new GregorianCalendar();
		currentCal.setTime(date1);
		int date = currentCal.get(Calendar.DATE);
		int year = currentCal.get(Calendar.YEAR);
		int month = currentCal.get(Calendar.MONTH);
		int lastdayofmth = DateManager.getNoOfDaysInMonth(month+1, year);
		date2 = new Date(new GregorianCalendar(year, month, lastdayofmth ).getTimeInMillis());
		return date2;
	}
}
