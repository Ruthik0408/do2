package itsdc.dolphin.do2; 

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List; 
import javax.transaction.Transactional;
import org.springframework.web.bind.annotation.RestController; 
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils; 
/*
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class Incr010106 implements java.io.Serializable {

	private String armyNo;	
	EmployeeRepository empRepo;	
	Do2Repository do2Repo;
	AuditCageRepository acRepo;
	LpcRegisterRepository lpcRegisterRepo;
	
	static String qe=Utils.getCurrentQuarterEnding();
		
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
	
	public LpcRegisterRepository getLpcRegisterRepo() {
		return lpcRegisterRepo;
	}

	public void setLpcRegisterRepo(LpcRegisterRepository lpcRegisterRepo) {
		this.lpcRegisterRepo = lpcRegisterRepo;
	}
	
	public Incr010106 createInstance() {
		Incr010106 incrFirstJan = new Incr010106();
		return incrFirstJan;
	} 
 
	@Transactional
	public String isArmyNoEligible(String armyNo){
		Employee emp = Utils.getCurrentEmployee(armyNo);
		String mesg=" ";
		if (emp==null){
	//		FacesMessages.instance().add(" Army Number is incorrect - Check correctness ");
			mesg="Army number is null/blank/incorrect !!!";
			return mesg;
		}
		if (emp!=null && emp.getStatusCode()!=null && emp.getStatusCode().getId()>4) {
			List<LpcRegister> lpcList = lpcRegisterRepo.findByEmployeeIdEcsGenerated(emp.getId());
			if (lpcList!=null && lpcList.size()>0) {
				;
			} else {
				mesg="IRLA Not Finalised On Dolphin !!!";
				return mesg;
			}
		}
		String begindt = "2005-01-31";
		String enddt = "2005-07-01";
		Date endDate = new Date();
		Date beginDate = new Date();
		Date prevIncrementDate = null;
		Date promotDate = null; 
		boolean payfixOpted = false;
		try {
		    endDate = DateManager.getDate(enddt);
		    beginDate=DateManager.getDate(begindt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!emp.getRegularTaFlag().equals("R")){
			mesg = " Teritorial Army Personnel. Not Eligible ";
			return mesg;
		}/*
		if (!emp.getStatusCode().getStatusCode().equals("E")&& !emp.getStatusCode().getStatusCode().equals("N")) {
			mesg =" Army Number is Recruit ";
			return mesg;
		}*/
		if (emp.getStatusCode().getStatusCode().equals("R")) {
			mesg = "Army Number is Recruit";
			return mesg;
		}
		if (emp.getEnrolmentDate().after(endDate)){
			mesg = " Enrolment date is after 01-07-2005";
			return mesg;
		}
			
		if (getDateOfFirstAuditCage(emp).after(endDate)){
			mesg = " Base Audit cage date is afer 01-07-2005";
			return mesg;
		}
			
		payfixOpted = getSixcpcDo2(emp);
		if (payfixOpted == true){
			mesg=" Indiviual opted for PAY Fixaction . Not eligible for ADHOC INCR ON 01012006";
			return mesg;
		}
		
		promotDate = getBasicEffectDate(emp);
		System.out.println(" date promot " + promotDate);
		
		if (promotDate != null) {
            if (promotDate.equals(DateManager.getSixthCpcDate())) {
            	mesg=" Promot date equals 01012006. Not Eligible for ADHOC INCR ON 01012006";
            }
                
			if (promotDate.after(beginDate) && promotDate.before(endDate)){
				generateIncrDo2(emp); 
				mesg="ADHOC INCR 01012006 Generated";
				return mesg;
			} else if (!promotDate.before(endDate)) {
				mesg=" Promot Date is after 01-07-2005. ANot Eligible for ADHOC INCR ON 01012006";
				return mesg;
			}
		}
		promotDate = getAcpDateInFirstHalfOf2005(emp);
		if (promotDate!=null) {
		   if (promotDate.equals(DateManager.getSixthCpcDate())) {
			   mesg=" Promot date equals 01012006. Not Eligible for ADHOC INCR ON 01012006";
			   return mesg;
		   }
             
		   try {
				if (promotDate.after(DateManager.getDate("2005-01-01")) && promotDate.before(endDate)){
					generateIncrDo2(emp); 
					mesg="ADHOC INCR 01012006 Generated";
					return mesg;
				} else if (!promotDate.before(endDate)) {
					mesg=" Promot Date is after 01-07-2005. Not Eligible for ADHOC INCR ON 01012006";
					return mesg;
				}
					
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}				
		/*revertDate = getReversionDate(emp);
		if (revertDate != null) {			
			return;
		}*/		
		prevIncrementDate = getPrevIncrDate(emp);	
		if (prevIncrementDate != null && !prevIncrementDate.before(endDate)){
			mesg=" INCR  Date is after 01-07-2005. Not Eligible for ADHOC INCR ON 01012006";
			return mesg;
		}		
		if (prevIncrementDate != null && 
		   (prevIncrementDate.after(beginDate) && prevIncrementDate.before(endDate))) {
			generateIncrDo2(emp);  
			mesg="ADHOC INCR 01012006 Generated";
			return mesg;
		}
		if (Utils.getPao().getPaoCode().equals("94") &&
		   (emp.getEnrolmentDate().after(beginDate) && emp.getEnrolmentDate().before(endDate))){
		    generateIncrDo2(emp); 
		    mesg="ADHOC INCR 01012006 Generated";
			return mesg;
		}
		
		if (emp.getGroupCategory().getSoldierCategory() != null && !emp.getGroupCategory().getSoldierCategory().equals("RT")) {
			if (emp.getInitialMusteringDate() == null && emp.getAttestationDate() == null) {
				mesg=" Employee Initial Musgering_date && Attestation Date is null . Please check ";
				return mesg;
			}
			if (emp.getInitialMusteringDate() != null && emp.getAttestationDate() != null &&
				!emp.getInitialMusteringDate().before(endDate) && !emp.getAttestationDate().before(endDate)) {
				mesg=" Employee Initial Musgering_date && Attestation Date is after 01-07-2005 ";
				return mesg;
			}
			if (emp.getAttestationDate() == null && 
			   (emp.getInitialMusteringDate() != null &&
				emp.getInitialMusteringDate().after(beginDate) && emp.getInitialMusteringDate().before(endDate))) {
				generateIncrDo2(emp);
				mesg="ADHOC INCR 01012006 Generated";
				return mesg;
			}
			if ((emp.getAttestationDate() != null && emp.getInitialMusteringDate() != null) &&
			    ((emp.getInitialMusteringDate().equals(emp.getAttestationDate()) || 
			     emp.getAttestationDate().after(emp.getInitialMusteringDate())) &&
				 emp.getInitialMusteringDate().after(beginDate) && emp.getInitialMusteringDate().before(endDate))) {
				 generateIncrDo2(emp);
				 mesg="ADHOC INCR 01012006 Generated";
					return mesg;
			}
			if ((emp.getAttestationDate() != null && emp.getInitialMusteringDate() != null) &&
			    (emp.getAttestationDate().before(emp.getInitialMusteringDate()) &&
				 emp.getAttestationDate().after(beginDate) && emp.getAttestationDate().before(endDate) &&
				 (DateManager.getNoOfDays(emp.getAttestationDate(), emp.getInitialMusteringDate()) < 365))) {
				 generateIncrDo2(emp);
				 mesg="ADHOC INCR 01012006 Generated";
					return mesg;
			}
			if ((emp.getAttestationDate() != null && emp.getInitialMusteringDate() != null) &&
			    (emp.getAttestationDate().before(emp.getInitialMusteringDate()) &&
			     emp.getInitialMusteringDate().after(beginDate) && emp.getInitialMusteringDate().before(endDate) &&
				 (DateManager.getNoOfDays(emp.getAttestationDate(), emp.getInitialMusteringDate()) > 364))) {
				 generateIncrDo2(emp);
				 mesg="ADHOC INCR 01012006 Generated";
					return mesg;
			}
		}
		return mesg;
	}
			
	public void generateIncrDo2(Employee employee) {
		Date incrDate=DateManager.getToday();
		
		try {
		    incrDate = DateManager.getDate("2006-01-01");	   
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*if (isAbsentOrDeputation(employee, incrDate)){
			return;
		}*/
	
		if (doesIncrDo2Exist(employee, incrDate)) {
			return;
		}
		
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
		incrDo2.setColumnC("ADHOC");
		incrDo2.setAuditCage(true);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setInputQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		do2Repo.save(incrDo2);
		/*Utils.getEntityManager().persist(incrDo2);
		Utils.getEntityManager().flush();*/
	}
	
	public boolean doesIncrDo2Exist(Employee emp, Date incrDt) {		
	/*
		List<Do2> incrDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
					" d.employee=:emp AND d.description='INCR' AND d.status='V' AND d.columnC='ADHOC' AND d.fromDate = :incrDate")
				.setParameter("emp", emp)
				.setParameter("incrDate",incrDt).getResultList();	*/
		
		List<Do2>incrDo2List = do2Repo.findAdhocIncr(emp.getId(),incrDt);
		if (incrDo2List == null || incrDo2List.size() == 0 ){
			return false;
		}					
		return true;
	}
	
	public Date getPrevIncrDate(Employee emp) {
	 	String begindt = "2005-01-31";
		Date beginDate = new Date();
		Calendar cal = new GregorianCalendar();
		cal.setTime(beginDate);
		Date dni = null;
		/*
		List<Do2>incrDo2List= Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.employee=:emp AND d.description in ('INCR' , 'LATENR')  AND d.status='V' AND extract(year from d.fromDate)=:year")
			.setParameter("emp", emp)
			.setParameter("year",2005).getResultList();	*/
		
		List<Do2>incrDo2List = do2Repo.findIncrForAdhocIncr(emp.getId(),cal.get(Calendar.YEAR));
		if (incrDo2List.size() != 0){
			dni=incrDo2List.get(0).getFromDate();
		}
		return dni;		
	}
	
	public Date getBasicEffectDate(Employee emp) {
		Date promotDate = null;
		String begindt = "2005-01-31";
		String enddt = "2005-07-01";
		Date endDate = new Date();
		Date beginDate = new Date();
		try {
		    endDate = DateManager.getDate(enddt);
		    beginDate=DateManager.getDate(begindt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<Do2>promotDo2List= null;
	/*	promotDo2List= Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.employee=:emp AND (d.description='PROMOT' OR d.description='ACP' OR " +
                " d.description = 'STEPUP' OR d.description = 'MSTHAV' OR d.description = 'MSTJCO')" +
				" AND d.status='V' AND d.fromDate = '2006-01-01'")
			.setParameter("emp", emp).getResultList();
		
		*/
		promotDo2List = do2Repo.findPromotDo2List(emp.getId());
		
		
		
		// if (promotDo2List != null && promotDo2List.size() > 0) {
			if (promotDo2List != null && promotDo2List.size() > 0) {
				if (promotDo2List.get(0).getDescription().equals("PROMOT")
						&& promotDo2List.get(0).getColumnA().substring(2)
								.equals("NK")
						&& emp.getAcpnaikDate() != null
						&& emp.getAcpnaikDate().before(
								promotDo2List.get(0).getFromDate())) {
					return null;
				} else if (promotDo2List.get(0).getDescription()
						.equals("PROMOT")
						&& promotDo2List.get(0).getColumnA().substring(2)
								.equals("HAV")
						&& emp.getAcphavDate() != null
						&& emp.getAcphavDate().before(
								promotDo2List.get(0).getFromDate())) {
					return null;
				}
			}
			// return promotDo2List.get(0).getFromDate();
		// }
		/*promotDo2List= Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.employee=:emp AND (d.description='PROMOT' OR d.description='ACP' OR d.description = 'STEPUP' " +
				" OR d.description = 'REMUST' OR d.description = 'MSTHAV' OR d.description = 'MSTJCO' OR d.description = 'INCR')" +
				" AND d.status='V' AND extract(year from d.fromDate)=:year order by d.fromDate desc")
			.setParameter("emp", emp)
			.setParameter("year",2005).getResultList();	*/
			
			
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(endDate);
		
		promotDo2List = do2Repo.findPromotDo2ListForAdhocIncr(emp.getId(), cal.get(Calendar.YEAR));
		
		if (promotDo2List != null && promotDo2List.size() > 0){
			promotDate=promotDo2List.get(0).getFromDate();	
			Rank prevRank = null;
			Rank presentRank = null;
			int prevRankCode = 0;
			int presentRankCode = 0;
			System.out.println ( " PROMOT DATE " +promotDate);
			if (promotDo2List.get(0).getDescription().equals("ACP")){
				prevRank = Utils.getPborRank(emp.getArmyNo(), DateManager.getNextDate(promotDate), "PAYFIX");
				prevRankCode = Integer.parseInt(prevRank.getRankCode());				
			} else {
				prevRank = Utils.getPborRank(emp.getArmyNo(), promotDate, "PAYFIX");
				prevRankCode = Integer.parseInt(prevRank.getRankCode());		
	//			System.out.println ( " PREVIOUS RANK CODE " +prevRankCode);
			}
			presentRank = Utils.getPborRank(emp.getArmyNo(), promotDate, "ALLCE");
			presentRankCode = Integer.parseInt(presentRank.getRankCode());
			
			//System.out.println("payfix rank " + prevRankCode + " allce rank " + presentRankCode);
			if (promotDo2List.get(0).getDescription().equals("REMUST")) {
				System.out.println (  " IN REMUST " +promotDo2List.get(0).getDescription());
				List<AuditCage> aclistPromotDate = acRepo.findByFromDateLessThan(promotDo2List.get(0).getEmployee().getId(), promotDo2List.get(0).getFromDate());
						int promotBasicPay = aclistPromotDate.get(0).getBasicPay();
						
						List<AuditCage> aclistLessThanPromotDate = acRepo.findByFromDateLessThan(promotDo2List.get(0).getEmployee().getId(), promotDo2List.get(0).getFromDate());
						int promotBeforeBasicPay = aclistLessThanPromotDate.get(0).getBasicPay();
						
						if (promotBasicPay== promotBeforeBasicPay){
							promotDo2List = do2Repo.findPromotDo2ListForAdhocIncrWithoutRemust(emp.getId(), cal.get(Calendar.YEAR));
						}
						 return promotDate=promotDo2List.get(0).getFromDate();	
			} 
		
			
			if (prevRankCode >= presentRankCode) {
				if (prevRankCode == 30) {
			//		promotDate = emp.getAcpnaikDate();
					if (emp.getAcpnaikDate() != null && (emp.getAcpnaikDate().after(beginDate) && emp.getAcpnaikDate().before(endDate))){
						promotDate = emp.getAcpnaikDate();
					} else if (promotDate != null && promotDate.before(beginDate) && 
							(getIncrDateOf2005(promotDate).after(beginDate) && 
							 getIncrDateOf2005(promotDate).before(endDate))){
						promotDate = getIncrDateOf2005(promotDate);						
					}  else if (promotDate != null && promotDate.after(beginDate) && promotDate.before(endDate)){
						;
					} else {
						promotDate=null;
					}
				} else if (prevRankCode == 40) {
				//	promotDate = emp.getAcphavDate();
	//				System.out.println("promot date 1 " + promotDate);
					if (emp.getAcphavDate() != null && (emp.getAcphavDate().after(beginDate) && emp.getAcphavDate().before(endDate))){
						promotDate = emp.getAcphavDate();
	//					System.out.println("promot date 2 " + promotDate);
					} else if (promotDate != null && promotDate.before(beginDate) && 
							(getIncrDateOf2005(promotDate).after(beginDate) && 
							 getIncrDateOf2005(promotDate).before(endDate))){
						promotDate = getIncrDateOf2005(promotDate);				
					} else if (promotDate != null && promotDate.after(beginDate) && promotDate.before(endDate)){
						;
					} else {
						promotDate=null;
					}
				} 
			}							
		}
		System.out.println ( " PRMOT DATE " +promotDate);
		return promotDate;		
	}

	public Date getAcpDateInFirstHalfOf2005(Employee emp) {
		List<Do2>acpDo2List= null;
		/*acpDo2List= Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.employee=:emp AND d.description='ACP' AND d.status='V' AND " +
				" d.fromDate between '2005-01-01' and '2005-06-30' ").setParameter("emp", emp).getResultList();
		*/
		acpDo2List = do2Repo.findAcpListForAdhocIncr(emp.getId());
		if (acpDo2List!=null && acpDo2List.size()>0) {
			return acpDo2List.get(0).getFromDate();
			
		}
		return null;	
	}
	
	 public Date getReversionDate(Employee emp) {		
		Date revertDate = null; 
		Date beginDate = new Date();
		Calendar cal = new GregorianCalendar();
		cal.setTime(beginDate);
	
	/*	List<Do2>revertDo2List= Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.employee=:emp AND (d.description = 'RANKS' OR d.description = 'LORANK' OR d.description = 'REVERT')" +
				" AND d.status='V' AND extract(year from d.fromDate)=:year order by d.fromDate desc")
			.setParameter("emp", emp)
			.setParameter("year",2005).getResultList();	*/
		
		List<Do2>revertDo2List = do2Repo.findRevertListForAdhocIncr(emp.getId(),cal.get(Calendar.YEAR));
		if (revertDo2List.size() != 0){
			revertDate=revertDo2List.get(0).getFromDate();
		}
		return revertDate;
	}
	
	 public Boolean getSixcpcDo2(Employee emp) {		
/*		List<Do2>sixcpcDo2List= Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.employee=:emp AND d.description='SIXCPC' AND d.status='V'")
			.setParameter("emp", emp)
			.getResultList();	
		*/
		List<Do2>sixcpcDo2List=do2Repo.findSixcpcDo2List(emp.getId());			
		return (sixcpcDo2List.size() > 0);		
	}

	 public Date getDateOfFirstAuditCage(Employee emp){
	      Date firstDate = null;
	   	/*  List<AuditCage> auditCageList = Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE ac.employee=:emp "+
	   			  							"  AND  ac.effective=true AND ac.cancelled=false  AND ac.recordStatus!='P' order by ac.fromDate")
	   			  							.setParameter("emp",emp)
	   			  							.getResultList();
	   	  */
	   	  List<AuditCage> auditCageList = acRepo.findFirstAuditCageAdhocIncr(emp.getId());
	   	  if (auditCageList.size() >0)
	   	      firstDate = auditCageList.get(0).getFromDate();
	   	  return firstDate; 
    }

	public Boolean isAbsentOrDeputation(Employee emp, Date incrementDate) {		
	/*	List<AuditCage> deputationAuditCageList = Utils.getEntityManager().createQuery(
				"SELECT ac FROM AuditCage ac WHERE  ac.employee=:emp AND ac.effective=true AND ac.cancelled=false"
				+ " AND  ac.recordStatus!='P' AND ac.absentFlag=true AND ((:fromDate between ac.fromDate AND ac.toDate) "
				+ " OR (:fromDate >= ac.fromDate AND ac.toDate is null))  "
				+ " order by ac.fromDate ")
		.setParameter("emp", emp)
		.setParameter("fromDate", incrementDate).getResultList();*/
		
		List<AuditCage> deputationAuditCageList = acRepo.findDeputationAuditCageAdhocIncr(emp.getId(), incrementDate);
		if (deputationAuditCageList.size() >0){
			return true;
		 }
		 return false;		 
	}

	 static public Date getIncrDateOf2005(Date date1){	
		Date date2 = null;
		Calendar currentCal = new GregorianCalendar();
		currentCal.setTime(date1);
		int month = currentCal.get(Calendar.MONTH);
		date2 = new Date(new GregorianCalendar(2005, month, 1).getTimeInMillis());
		return date2;
	}
}
