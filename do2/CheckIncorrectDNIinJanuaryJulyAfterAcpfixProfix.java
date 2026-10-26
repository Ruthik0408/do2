package itsdc.dolphin.do2;
  
import java.util.Date; 
import java.util.List; 
import javax.transaction.Transactional;
import org.springframework.web.bind.annotation.RestController; 
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee; 
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/*
 * Author : IT & SDC, Secunderabad  20.07.2025 --  Program to check any missing increment in January / July after adjustment of ACPFIX/PROFIX six months prior date.
 */

@RestController
public class CheckIncorrectDNIinJanuaryJulyAfterAcpfixProfix implements java.io.Serializable {

	private String armyNo;	
	EmployeeRepository empRepo;	
	Do2Repository do2Repo;
	static AuditCageRepository acRepo;

	public String getArmyNo() {
		return armyNo;
	}
	public void setArmyNo(String armyNo) {
		this.armyNo = armyNo;
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
	public AuditCageRepository getAcRepo() {
		return acRepo;
	}
	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}
 	
	final static public long MILLIS_IN_DAY = 1000 * 3600 * 24;
		
	public CheckIncorrectDNIinJanuaryJulyAfterAcpfixProfix createInstance() {
		CheckIncorrectDNIinJanuaryJulyAfterAcpfixProfix incrFirstJan = new CheckIncorrectDNIinJanuaryJulyAfterAcpfixProfix();
		return incrFirstJan;
	} 	
	 
	@Transactional
	public String isArmyNoEligible(Employee emp, Date sixMonthsPriorDate, Date incrDate){
 		String mesg=" ";
		if (emp == null){
 			mesg="Army number is null ";
			return mesg;
		}
		String month = Utils.getCurrentQuarter();
		if (!DateManager.getToday().before(DateManager.getSeventhCpcDate())) {
			if (!(month.substring(0,2).equals("01") || month.substring(0,2).equals("07"))) {
				mesg="Activity Available Only In January or July !!!";
				return mesg;
			}
		}
 		if (!emp.getRegularTaFlag().equals("R")) {
			mesg = " Teritorial Army Personnel. Not Eligible ";
			return mesg;
		}		
		if (emp.getStatusCode().getStatusCode().equals("R")) {
			mesg = "Army Number is Recruit";
			return mesg;
		}
		
 	
		if (emp.getEnrolmentDate().after(sixMonthsPriorDate)){
			mesg = " Enrolment date is after "+sixMonthsPriorDate;
			return mesg;
		}			
		if (getDateOfFirstAuditCage(emp).after(sixMonthsPriorDate)) {
			mesg = " Base Audit cage date is afer " + sixMonthsPriorDate;
			return mesg;
		}		 
		if (emp.getGroupCategory().getSoldierCategory() != null && !emp.getGroupCategory().getSoldierCategory().equals("RT")) {
			if (emp.getInitialMusteringDate() == null && emp.getAttestationDate() == null) {
				mesg=" Employee Initial Musgering_date && Attestation Date is null . Please check ";
				return mesg;
			}
			if (emp.getInitialMusteringDate() != null && emp.getAttestationDate() != null &&
				!emp.getInitialMusteringDate().before(incrDate) && !emp.getAttestationDate().before(incrDate) &&
				emp.getEnrolmentDate()!=null && !emp.getEnrolmentDate().before(sixMonthsPriorDate)) {
				mesg=" Employee Initial Musgering_date && Attestation Date is after "+sixMonthsPriorDate;
				return mesg;
			}					 	 
		}
		if (!getPromotionsDuringPastSixMonths(emp, sixMonthsPriorDate, incrDate)) {
			mesg=" Beneficial MACPS/Promotion Exists During Past Six Months !!! ";
			return mesg;
		}	 
		if (doesIncrDo2Exist(emp, incrDate)) {
			mesg=" Valid Increment DOII for "+incrDate+" Already Exists!!! ";
			return mesg;
		}		
		if (emp.getIncrementDate()!=null && emp.getIncrementDate().after(incrDate)) {
			emp.setIncrementDate(Utils.getNextIncrementDate(incrDate));
			empRepo.save(emp);
		}
		generateIncrDo2(emp, incrDate);
		mesg="Missing INCR DOII For "+incrDate+" Generated!!!";	
		return mesg;
	}
			
	public void generateIncrDo2(Employee employee, Date incrDate) {	 
		
		Do2 incrDo2 = new Do2();		
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(employee.getArmyNo());
		incrDo2.setCheckDigit(employee.getCheckDigit());
		incrDo2.setUnitCode(employee.getUnit().getUnitCode());
		incrDo2.setUnit(employee.getUnit());
		incrDo2.setBatchNo(7777);
		incrDo2.setDo2Group("IA");
		incrDo2.setDo2Type("0");
		incrDo2.setDo2No("6666");
		incrDo2.setDo2ItemNo("666");
		incrDo2.setDo2Date(incrDate);
		incrDo2.setDescription("INCR");		
		incrDo2.setFromDate(incrDate);
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setReason("System Generated Do2");
		incrDo2.setColumnC("REG");
		incrDo2.setAuditCage(true);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setInputQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2Repo.save(incrDo2);	 
	}
	
	public boolean doesIncrDo2Exist(Employee emp, Date incrDt) {	 
		List<Do2>incrDo2List = do2Repo.findIncr(emp.getId(),incrDt);
		if (incrDo2List == null || incrDo2List.size() == 0 ){
			return false;
		}		
		return true;
	}
		
	public boolean getPromotionsDuringPastSixMonths(Employee emp, Date sixMonthsPriorDate, Date incrDate) {
	 	List<Do2> macpsPromotDo2List=null;
	 	boolean validity=true;
 		macpsPromotDo2List = do2Repo.findMacpsPromotDo2List(emp.getId(), sixMonthsPriorDate, incrDate);
 		if (macpsPromotDo2List!=null && macpsPromotDo2List.size()==1) {
 			String columnA=null;
 			columnA=macpsPromotDo2List.get(0).getColumnA().substring(2);
 			if (macpsPromotDo2List.get(0).getColumnA().equals("NBSUB") || macpsPromotDo2List.get(0).getColumnA().equals("NBRIS")) {
 				columnA=macpsPromotDo2List.get(0).getColumnA();
 			}
 			if (columnA!=null && macpsPromotDo2List.get(0).getDescription().equals("PROMOT")) { 					
 				if (columnA.trim().contains("NK") || columnA.trim().contains("LD")) {
					if (emp.getAcpnaikDate()!=null && emp.getAcpnaikDate().before(sixMonthsPriorDate)) {
						;
					} else {
						validity=false;
					}
				} else if (columnA.trim().contains("HAV") || columnA.trim().contains("DFR")) {
					if (emp.getAcphavDate()!=null && emp.getAcphavDate().before(sixMonthsPriorDate)) {
							;							
					} else {
						validity=false;
					}
				} else if (columnA.equals("NBSUB") || columnA.equals("NBRIS")) {
					if (emp.getAcpnbsubDate()!=null && emp.getAcpnbsubDate().before(sixMonthsPriorDate)) {
						;
					} else {
						validity=false;
					}
				} else if (columnA.trim().endsWith("SUB") || columnA.trim().endsWith("RIS")) {
					if (emp.getAcpsubDate()!=null && emp.getAcpsubDate().before(sixMonthsPriorDate)) {
						;
					} else {
						validity=false;
					}
				} else if (columnA.trim().contains("SUBMAJ") || columnA.trim().contains("RISMAJ")) {
					if (emp.getAcpsubmajDate()!=null && emp.getAcpsubmajDate().before(sixMonthsPriorDate)) {
						;
					} else {
						validity=false;
					}
				} 
 			} else {
 				if (macpsPromotDo2List.get(0).getFromDate().after(sixMonthsPriorDate) 
 					&& macpsPromotDo2List.get(0).getFromDate().before(incrDate)) {
 					validity=false;
 				}
 			}
 		}
 		return validity;
	}	
 
 	public Date getDateOfFirstAuditCage(Employee emp){
	      Date firstDate = null;
	   	  List<AuditCage> auditCageList = acRepo.findFirstAuditCageAdhocIncr(emp.getId());
	   	  if (auditCageList.size() >0)
	   	      firstDate = auditCageList.get(0).getFromDate();
	   	  return firstDate; 
    }
 	
	static public Boolean isAbsentOrDeputation(Employee emp, Date incrementDate) {
 		List<AuditCage> deputationAuditCageList = acRepo.findDeputationAuditCageAdhocIncr(emp.getId(), incrementDate);
		if (deputationAuditCageList.size() >0) {
			return true;
		 }
		 return false;		 
	} 
}