package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Acr;
import itsdc.dolphin.models.AdjustmentCb;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.BhutanClaim;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.EmployeeEarning;
import itsdc.dolphin.models.Famo;
import itsdc.dolphin.models.Form16;
import itsdc.dolphin.models.FundSummary;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.MedicalClaim;
import itsdc.dolphin.models.Pli;
import itsdc.dolphin.models.PliAdjustment;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author ITSDC, Sec'bad.
 * 
 */

@RestController
public class Fsdsch extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee emp;
	private StringBuffer buffer = new StringBuffer();
	final static public int TRANSPORT_HANDICAPPED_MAX_REBATE = 1600;//(1600*12)
	final static public int TRANSPORT_NORMAL_MAX_REBATE = 800;// (800*12)
	final static public int HOSSUB_MAX_REBATE_PER_ANNUM = 3600;// (300*12)
	final static public int CEA_MAX_REBATE_PER_ANNUM = 1200;//(100*12)
	static final public String FS_STATUS_INCORRECT = "FS status not yet updated in Employee.  Please Check the Correctness \n";
	static final public String FS_DUE_DATE_INCORRECT = "FS due date in employee differs from do2 fromdate \n";
	static final public String DISCH_DESCRIPTION_NOT_FOUND = "Correspondign DISCH description not found. Please feed the same. \n";
	double exemptionSec10 = 0;
	String ydate;
	Date curMonthDate, lastDateFinYear, begDateFinYear;
	int startMth, endMth, fundStartMth, fundEndMth;
	int month = Integer.parseInt(Utils.getCurrentQuarter().substring(0, 2));
	int year = Integer.parseInt(Utils.getCurrentQuarter().substring(3));
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (Utils.isEmpThisMonthFs(emp)) {
			if (Do2Manager.getFsDischDescriptions().contains(do2.getDescription())) {
				do2.setReason("To be fed in following month");
				return false;
			}
		}
		
		if (do2.getId() == newDo2.getId()) {
			if (!isDischargeDo2Exists(do2)) {
				validity = false;
				buffer.append(DISCH_DESCRIPTION_NOT_FOUND);				
			}
		}
		
		if (validity==false) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
	 	newAuditCage.setAbsentFlag(true);
		return newAuditCage;
	}
 
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		List<Character> fsStatusList = Arrays.asList(new Character[] {'1', '2', '3','4','5','6','7', '8', '9', 'F' }); 
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		char employeeStatusCode = getEmployeeStatusCode(do2);
		String qe = Utils.getCurrentQuarter();
		StatusCode sc=scRepo.findByStatusCode(employeeStatusCode+"");
		emp.setStatusCode(sc);
	//	emp.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(),"statusCode", employeeStatusCode));
		emp.setFsDueDate(do2.getFromDate());
		
		empRepo.save(emp);
		
	//	Utils.getEntityManager().persist(emp);
//		Utils.getEntityManager().flush();
		
		List<EmployeeDeduction> edList = edRepo.findByEmployeeIdAndQe(emp.getId(), qe);
				
		//		Utils.getEntityManager().createQuery("SELECT ed FROM EmployeeDeduction ed where " +
		//												" ed.employee=:emp AND ed.qe=:qe").setParameter("emp", emp).setParameter("qe",qe).getResultList();
		
		if (edList!=null && edList.size()>0 && !edList.get(0).getEmployeeType().equals("P")) {
			edList.get(0).setEmployeeType("N");
			edRepo.save(edList.get(0));
		//	Utils.getEntityManager().persist(edList.get(0));
	//		Utils.getEntityManager().flush();
		
		}                      
		Date startDate = DateManager.getNextDate(Utils.getLastDayOfPreviousQuarter());
		List<FundSummary> fsList=null;
		if (startDate.equals(Utils.getDolphinStartDate())){
			fsList = fsRepo.findByEmployeeIdAndQe(emp.getId(), Utils.getFundQuarter());
					
		//			Utils.getEntityManager().createQuery("SELECT fs FROM FundSummary fs where " +
	//		" fs.employee=:emp AND fs.qe=:qe").setParameter("emp", emp).setParameter("qe", Utils.getFundQuarter()).getResultList();
	
		} else {
			fsList = fsRepo.findByEmployeeIdAndQe(emp.getId(), qe);
					
		//			Utils.getEntityManager().createQuery("SELECT fs FROM FundSummary fs where " +
		//	" fs.employee=:emp AND fs.qe=:qe").setParameter("emp", emp).setParameter("qe", qe).getResultList();
		}
		
		if (fsList!=null && fsList.size()>0 && !fsList.get(0).getEmployeeType().equals("P")) {
			fsList.get(0).setEmployeeType("N");
			fsRepo.save(fsList.get(0));
		//	Utils.getEntityManager().persist(fsList.get(0));
		//	Utils.getEntityManager().flush();
		}
	 	
/*		List<Acr> acrList = Utils.getEntityManager().createQuery("SELECT acr FROM Acr acr WHERE " +
								" (acr.employee=:emp OR acr.armyNo=:ano) AND acr.qe=:qe ").setParameter("emp", emp)
								.setParameter("ano", do2.getArmyNo()).setParameter("qe", qe).getResultList();
		
		for (Acr acr : acrList) {
			acr.setRecordStatus('N');
		}
		

		List<AdjustmentCb> adjCbList = Utils.getEntityManager().createQuery("SELECT adjcb FROM AdjustmentCb adjcb WHERE " +
								" (adjcb.employee=:emp OR adjcb.armyNo=:ano) AND adjcb.qe=:qe ").setParameter("emp", emp)
								.setParameter("ano", do2.getArmyNo()).setParameter("qe", qe).getResultList();
		
		for (AdjustmentCb adjcb : adjCbList) {
			adjcb.setRecordStatus('N');
		}
		
		List<Famo> famoList = Utils.getEntityManager().createQuery("SELECT famo FROM Famo famo WHERE " +
										" (famo.employee=:emp OR famo.armyNo=:ano) AND famo.qe=:qe ").setParameter("emp", emp)
										.setParameter("ano", do2.getArmyNo()).setParameter("qe", qe).getResultList();
		for (Famo famo : famoList) {
			famo.setRecordStatus('N');
		}
		
		List<Do2> do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
									" d.id!=:do2Id AND (d.employee=:emp OR d.armyNo=:ano) AND d.qe=:qe ").setParameter("emp", emp)
									.setParameter("ano", do2.getArmyNo()).setParameter("qe", qe)
									.setParameter("do2Id", do2.getId()).getResultList();
		
		for (Do2 adjustedDo2 : do2List) {
			adjustedDo2.setStatus("N");
		}
		
		//Amended for NE
		for (Do2 adjustedDo2 : do2List) {
			char status = emp.getStatusCode().getStatusCode();
			if (fsStatusList.contains(status) && !(Do2Manager.getNeAdmissibleDescriptions().contains(adjustedDo2.getDescription()))) {
			adjustedDo2.setStatus("N");
			}
		}
		
		List<BhutanClaim> bcList =  Utils.getEntityManager().createQuery("SELECT bc FROM BhutanClaim bc WHERE " +
											" (bc.employee=:emp OR bc.armyNo=:ano) AND bc.qe=:qe ").setParameter("emp", emp)
											.setParameter("ano", do2.getArmyNo()).setParameter("qe", qe).getResultList();

		for (BhutanClaim bc : bcList) {
			bc.setRecordStatus('N');
		}

		List<MedicalClaim> mcList =  Utils.getEntityManager().createQuery("SELECT mc FROM MedicalClaim mc WHERE " +
											" (mc.employee=:emp OR mc.armyNo=:ano) AND mc.qe=:qe ").setParameter("emp", emp)
											.setParameter("ano", do2.getArmyNo()).setParameter("qe", qe).getResultList();
		

		for (MedicalClaim mc : mcList) {
			mc.setRecordStatus('N');
		}
		
		List<Pli> pliList = Utils.getEntityManager().createQuery("SELECT p From Pli p where p.employee=:emp")
													.setParameter("emp",emp)
													.getResultList();
		
		for(Pli p : pliList){
			List<PliAdjustment> pliadjList = Utils.getEntityManager().createQuery("SELECT padj From PliAdjustment padj where padj.pli=:pli")
												  .setParameter("pli",p)
												  .getResultList();
			
			for(PliAdjustment padj : pliadjList){
				 padj.setRecordStatus('N');
			}
					
		}*/
		if (newDo2.getId()==do2.getId()) {
			Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
			List<LpcRegister> lpcList = lpcRepo.findByRecordStatusEntryApprovedAndEcsGenerated(emp.getId());
					
					
		/*			Utils.getEntityManager().createQuery("SELECT l FROM LpcRegister l WHERE l.employee=:emp " +
			" and l.fsApproved = true and l.ecsGenerated = true")
			.setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo())).getResultList();*/
			if (lpcList == null || lpcList.size() == 0){	
				updForm16(emp);	
			}
		}
		return Do2Manager.SUCCESS;
	}

	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
	 	return Do2Manager.SUCCESS;
	}
 
	public char getEmployeeStatusCode(Do2 do2) {
		
		if (do2.getDescription().equals("FSDETH"))
			return '1';
	
		if (do2.getDescription().equals("FSDSCH"))
			return '2';
			
		if (do2.getDescription().equals("FSDSMS"))
			return '3';
		
		if (do2.getDescription().equals("FSBTTL"))
			return '4';
		
		if (do2.getDescription().equals("FSKILL"))
			return '5';
		
		if (do2.getDescription().equals("FSDESN"))
			return '6';
	
		if (do2.getDescription().equals("FSINVL"))
			return '7';
		
		if (do2.getDescription().equals("FSTFOT"))
			return '8';
				
		if (do2.getDescription().equals("FSCOMN"))
			return '9';
		
		if (do2.getDescription().equals("FSRMVL"))
			return 'F';
		
		return ' ';		
	}
	
	
	public boolean isDischargeDo2Exists(Do2 do2) {
		
		String dischargeDescription = null;
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getDescription().equals("FSDETH"))  {
			dischargeDescription = "DEATH";
		}		
		if (do2.getDescription().equals("FSDSCH"))  {
			dischargeDescription = "DISCH";
		}
		if (do2.getDescription().equals("FSDSMS"))  {
			dischargeDescription = "DISMIS";
		}
		if (do2.getDescription().equals("FSBTTL"))  {
			dischargeDescription = "BATTLE";
		}
		if (do2.getDescription().equals("FSKILL"))  {
			dischargeDescription = "KILLED";
		}
		if (do2.getDescription().equals("FSDESN"))  {
			dischargeDescription = "DESN";
		}
		if (do2.getDescription().equals("FSINVL"))  {
			dischargeDescription = "INVALD";
		}
		if (do2.getDescription().equals("FSTFOT"))  {
			dischargeDescription = "TFROUT";
		}
		if (do2.getDescription().equals("FSCOMN"))  {
			dischargeDescription = "COMMN";
		}
		if (do2.getDescription().equals("FSRMVL"))  {
			dischargeDescription = "REMVAL";
		}
		List<Do2> do2List = do2Repo.findByDescFromDateLessThanEqual(emp.getId(), "V", dischargeDescription, do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery( " SELECT d from do2  d  WHERE  d.employee=:emp " +
				" AND d.description=:desc  AND d.status='V' AND d.fromDate <= fromDate")
				.setParameter("emp", emp)
				.setParameter("desc", dischargeDescription)
				.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		
		if (do2List!=null && do2List.size() > 0 )  {
			return true;
		} else 
			return false;
		
	}
	
	static String [][] codeAmountMapping = { 		
		{"113", "2600"}, {"114", "1000"},
		{"115", "3900"}, {"116", "1060"},
		{"117", "1600"}, {"122", "4200"}, 
		{"124", "1600"}, {"197", "7000"}}; 
	static public HashMap<String, String> CODE_AMOUNT_MAP;
	static public HashMap<String, String> getCodeAmountMap() {
		if (CODE_AMOUNT_MAP != null)
			return CODE_AMOUNT_MAP;
		CODE_AMOUNT_MAP = new HashMap<String, String>();
		for (int i = 0; i < codeAmountMapping.length; i++) {
			String keyVal[] = codeAmountMapping[i];
			CODE_AMOUNT_MAP.put(keyVal[0], keyVal[1]);
		}
		return CODE_AMOUNT_MAP;
	}
	public void updForm16(Employee employee){		
		if (month > 2 && month < 13) {
			startMth = ((year * 100) + 03);
			endMth = (((year + 1) * 100) + 02);
		} else {
			startMth = (((year - 1) * 100) + 03);
			endMth = ((year * 100) + 02);
		}
		if (month > 0 && month < 10) 
			ydate = year+"-0"+month+"-01";
		else if (month > 9 && month < 13) 
			ydate = year+"-"+month+"-01";
		try {
			curMonthDate = DateManager.getDate(ydate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		if (month > 2 && month < 13)
			ydate = (year + 1)+"-03-01";			
		else 
			ydate = year+"-03-01";
	
		try {
			lastDateFinYear = DateManager.getDate(ydate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		if (month > 2 && month < 13)
			ydate = year+"-03-01";			
		else 
			ydate = (year - 1)+"-03-01";
	
		try {
			begDateFinYear = DateManager.getDate(ydate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		Form16 frm16 = getF16Details(emp, getAssessmentYear());
		double transportExemption = 0;
		double taxableEmol = eeRepo.findByEmployeePayCodeAndQeGreaterThanEqualAndQeLessThanEqual(emp.getId(), startMth, endMth);
				
	/*			(Double) Utils.getEntityManager().createQuery("SELECT COALESCE(sum(ee.amount),0) FROM EmployeeEarning ee, PayCode pc where ee.payCode = pc " +
		" and ee.employee = :emp and (((CAST(substr(ee.qe,4) AS int) * 100) + CAST(substr(ee.qe,0,3) AS int)) >= :startMth AND " +
		" ((CAST(substr(ee.qe,4) AS int) * 100) + CAST(substr(ee.qe,0,3) AS int)) <= :endMth) AND pc.taxable = true")
		.setParameter("emp", emp)
		.setParameter("startMth", startMth)
		.setParameter("endMth", endMth)
		.getSingleResult();*/
		// coalesce function returns 0 amount if sum(e.amount) becomes null - there are no records of 129,130
		double transportAllowance = eeRepo.findByEmployeeAndTransportAllowancePayCodeAndFromDateGreaterThanEqualAndQeBetween(emp.getId(),
				begDateFinYear, lastDateFinYear, startMth, endMth);
				
				
			/*	(Double) Utils.getEntityManager().createQuery(
				 " SELECT COALESCE(sum(e.amount),0) FROM EmployeeEarning e WHERE e.employee=:employee AND " +
				 " (e.fromDate >= :firstday  and (e.toDate is null or (e.toDate is not null and e.toDate < :lastday)))" +
				 "  and e.payCode in (129, 130) and " +
				 " ((CAST(substr(e.qe,4) AS int) * 100) + CAST(substr(e.qe,0,3) AS int)) between :startMth and :endMth")
				 .setParameter("employee", emp) 
				 .setParameter("firstday", begDateFinYear) 
				 .setParameter("lastday", lastDateFinYear)
				 .setParameter("startMth", startMth) 
				 .setParameter("endMth", endMth) .getSingleResult();*/
		if (frm16.getHandicapped()){
			int transport_exempt_limit = (int) (TRANSPORT_HANDICAPPED_MAX_REBATE *
					         DateManager.getNoOfMonths(begDateFinYear, Utils.getLastDayOfCurrentQuarter()));
			if (transportAllowance > transport_exempt_limit){
			    transportExemption = transport_exempt_limit;
		    } else if (transportAllowance > 0){
		    	transportExemption = transportAllowance;
			}
		} else {
			int transport_exempt_limit = (int) (TRANSPORT_NORMAL_MAX_REBATE *
			         DateManager.getNoOfMonths(begDateFinYear, Utils.getLastDayOfCurrentQuarter()));
			if (transportAllowance > transport_exempt_limit){
			    transportExemption = transport_exempt_limit;
		    } else if (transportAllowance > 0){
		    	transportExemption = transportAllowance;
			}						
		}					
		taxableEmol -= transportExemption;
		// field allowances amount more than the permissible limits is taxable
		// rebate for CEA, HOSSUB -- only in feb or fs cases
		int fieldAllceTaxableAmt = 0;
		int totlimit = 0;
		int limit = 0;
		double noOfmonths = 0;
		double totmths = 0;
		double totamt = 0;	
		Date mindate = null;
		Date maxdate = null;
		List<EmployeeEarning> emperngList = getEmpErng(employee); 
		if (emperngList.size() > 0){
			for (EmployeeEarning e : emperngList) {							
			    totmths =  DateManager.getNoOfMonths(e.getFromDate(),e.getToDate());	
			    mindate=e.getFromDate();
			    if (e.getFromDate().before(begDateFinYear))
				    mindate = begDateFinYear;
			    if (e.getToDate() != null){
			        maxdate=e.getToDate();
			    } else {
			    	maxdate = Utils.getLastDayOfPreviousQuarter();
			    }
                if (e.getToDate().after(DateManager.getDate(lastDateFinYear, -1)))
				    maxdate = DateManager.getDate(lastDateFinYear, -1);
                noOfmonths = DateManager.getNoOfMonths(mindate, maxdate);			                
                limit = (int)(Integer.parseInt(getCodeAmountMap().get(e.getPayCode().getId()+""))*noOfmonths);			                
			    totamt = (int) (e.getAmount() * noOfmonths / totmths);						    
				if (totamt > 0 && totamt > limit){
					fieldAllceTaxableAmt += (totamt - limit);	
					totlimit += limit;
				}
				if (totamt < 0 && totamt < (limit * -1)){
					fieldAllceTaxableAmt += (totamt - (limit * -1));	
					totlimit += (limit * -1);
				}
			}						 
		}
		taxableEmol = taxableEmol + fieldAllceTaxableAmt + totlimit;					
		// CEA, HOSSUB - rebate upto permissible limits 1200 and 3600 per child
		int rebateamt = getCea(employee);
		//	taxableEmol = taxableEmol - rebateamt;
		List<AdjustmentCb>adjCbList = getAdjCb(employee);// it adjustments proposed by PAO for the full year
		for (AdjustmentCb ad : adjCbList) { // it adjustments proposed by PAO
			if (ad.getPayCode().getTaxable()) {
				taxableEmol = taxableEmol + ad.getApprovalAmount();
			}
		}
		frm16.setGrossSalary((int)taxableEmol);
		exemptionSec10 = (totlimit + rebateamt);
		frm16.setExemptionSection10((int) exemptionSec10);
		double salary = taxableEmol - exemptionSec10;
		frm16.setSalary((int) salary);
		f16Repo.save(frm16);
	//	Utils.getEntityManager().persist(frm16);

	}
	public List<AdjustmentCb> getAdjCb(Employee employee) {		
		
		List<AdjustmentCb> adjCbList = adjCbRepo.findByEmployeeApprovedAndItAdjustmentAndQeBetween(employee.getId(), startMth, endMth);
				
	/*			Utils.getEntityManager().createQuery(
		 " SELECT a FROM AdjustmentCb a WHERE a.employee=:employee AND a.approved = true AND " +
		 " a.recordStatus ='V' AND  a.itAdjustment = true AND " +
		 " ((CAST(substr(a.qe,4) AS int) * 100) + CAST(substr(a.qe,0,3) AS int)) between :startMth and :endMth ") 
		 .setParameter("employee", employee) 
		 .setParameter("startMth", startMth) 
		 .setParameter("endMth", endMth) .getResultList();*/
		 
		return adjCbList;
	}
	private String getAssessmentYear(){
		String assessYear = "";
		Date lastDayOfQuarter = Utils.getLastDayOfCurrentQuarter();
		Calendar assessmentYear = new GregorianCalendar();
		assessmentYear.setTimeInMillis(lastDayOfQuarter.getTime());
		int currentQuarterYear = assessmentYear.get(Calendar.YEAR);
		int currentQuarterMonth = assessmentYear.get(Calendar.MONTH)+1;
		if (currentQuarterMonth > 2 && currentQuarterMonth < 13) {
			assessYear = (currentQuarterYear + 1) + "-" + (currentQuarterYear + 2);
		} else {
			assessYear = currentQuarterYear + "-" + (currentQuarterYear + 1);
		}
		return assessYear;
	}
	public List<EmployeeEarning> getEmpErng(Employee employee) {
		/* fk_pay_code        pay_code
		 *    113               CFAA
		 *    114               CMFAA
		 *    115               SCCIA
		 *    116               HAUCL
		 *    117               HAUCH
		 *    124               HAUCS
		 *    122               CHAFAA
		 *    197               SIACHN		     
		 */
		
		List<EmployeeEarning> emperngList = eeRepo.findByEmployeeFromDateGreaterThanEqualAndToDateLessThanEqualAndFieldAllcePayCodeAndQeBetweenOrderByPayCodeFromDateToDate(employee.getId(),
																								begDateFinYear, lastDateFinYear, startMth, endMth);
				
	/*			Utils.getEntityManager().createQuery(
		 " SELECT e FROM EmployeeEarning e WHERE e.employee=:employee AND " +
		 " ((e.fromDate >= :firstday  and e.toDate <= :lastday) or (e.fromDate < :firstday  and e.toDate > :firstday)) and " +
		 " e.payCode in (113, 114, 115, 116, 117, 124, 122, 197) and " +
		 " ((CAST(substr(e.qe,4) AS int) * 100) + CAST(substr(e.qe,0,3) AS int)) between :startMth and :endMth " +
		 " order by e.payCode, e.fromDate, e.toDate") 
		 .setParameter("employee", employee) 
		 .setParameter("firstday", begDateFinYear) 
		 .setParameter("lastday", lastDateFinYear)
		 .setParameter("startMth", startMth) 
		 .setParameter("endMth", endMth) .getResultList();*/
		 
		return emperngList;
	}
	public Form16 getF16Details(Employee employee,String assessmentYear) {
		 Form16 frm16=null;
		 frm16 = f16Repo.findByEmployeeIdAndAssessmentYear(employee.getId(),assessmentYear);
				 
	/*			 (Form16) Utils.getEntityManager().createQuery(
		  "SELECT f FROM Form16 f WHERE f.employee = :employee AND f.assessmentYear = :assYear") 
		  .setParameter("employee", employee) 
		  .setParameter("assYear", assessmentYear).getSingleResult();*/
		 
		return frm16;
	}
	public int getCea(Employee employee) {
		/* fk_pay_code        pay_code
		 *    140               CEA
		 *    218               HOSSUB	     
		 */
		int rebateamt = 0;
		int hossubChildren = 0;
		int ceaChildren = 0;
		int cealimit = 0;
		List<EmployeeEarning> emperngList = eeRepo.findByEmployeeFromDateGreaterThanEqualAndToDateLessThanEqualAndCeaPayCodeAndQeBetween(employee.getId(),
																										begDateFinYear, lastDateFinYear, startMth, endMth);
				
	/*			Utils.getEntityManager().createQuery(
		 " SELECT e FROM EmployeeEarning e WHERE e.employee=:employee AND " +
		 " ((e.fromDate >= :firstday and e.toDate <= :lastday) or (e.fromDate < :firstday and e.toDate > :firstday)) " +
		 " AND e.payCode in (140, 218) and " +
		 " ((CAST(substr(e.qe,4) AS int) * 100) + CAST(substr(e.qe,0,3) AS int)) between :startMth and :endMth ")
		 .setParameter("employee", employee) 
		 .setParameter("firstday", begDateFinYear) 
		 .setParameter("lastday", lastDateFinYear)
		 .setParameter("startMth", startMth) 
		 .setParameter("endMth", endMth) .getResultList();*/
		if (emperngList.size() == 0)
			return 0;
		int ceaamt = 0;
		for (EmployeeEarning e : emperngList){
			ceaamt += e.getAmount();
		}
		List<String> descList=Arrays.asList(new String[] {"HOSSUB","HSBTWN"});
		List<Do2> do2List = do2Repo.findByFromDateGreaterThanEqualAndToDateLessThanAndFromDateLessThanEqualAndToDateGreaterAndQeBetweenOrderByColB(employee.getId(),
																											descList,begDateFinYear, lastDateFinYear, startMth, endMth);
				
				
		/*		Utils.getEntityManager().createQuery(
				 " SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status = 'V' AND " +
				 " ((d.fromDate >= :firstday and d.toDate < :lastday) or (d.fromDate <= :firstday and d.toDate > :firstday) or " +
				 " (d.fromDate < :lastday and d.toDate >= :lastday)) and d.description in ('HOSSUB', 'HSBTWN') and " +
				 " ((CAST(substr(d.acceptedQe,4) AS int) * 100) + CAST(substr(d.acceptedQe,0,3) AS int)) between :startMth and :endMth " +
				 " order by d.columnB")
				 .setParameter("employee", employee) 
				 .setParameter("firstday", begDateFinYear) 
				 .setParameter("lastday", lastDateFinYear)
				 .setParameter("startMth", startMth) 
				 .setParameter("endMth", endMth) .getResultList(); */
		if (do2List.size() == 0){
			hossubChildren = 0;
		} else {
			String date = do2List.get(0).getColumnB();
			hossubChildren = 1;
			for (Do2 d : do2List){
				if (!date.equals(d.getColumnB())){
					hossubChildren = 2;
				}
			}
		}
		cealimit = hossubChildren * HOSSUB_MAX_REBATE_PER_ANNUM;
		if (hossubChildren == 2){
			if (ceaamt > cealimit)
				rebateamt = cealimit;
			else
				rebateamt = ceaamt;
			return rebateamt;
		}
		List<String> descList2=Arrays.asList(new String[] {"CEA","CEATWN","CEAHC"});
		List<Do2> do2List1 = do2Repo.findByFromDateGreaterThanEqualAndToDateLessThanAndFromDateLessThanEqualAndToDateGreaterAndQeBetweenOrderByColB(employee.getId(),
																											descList2,begDateFinYear, lastDateFinYear, startMth, endMth);
				
				
	/*			Utils.getEntityManager().createQuery(
				 " SELECT d FROM Do2 d WHERE d.employee=:employee AND d.status = 'V' AND " +
				 " ((d.fromDate >= :firstday and d.toDate < :lastday) or (d.fromDate <= :firstday and d.toDate > :firstday) or " +
				 " (d.fromDate < :lastday and d.toDate >= :lastday)) and d.description in ('CEA', 'CEATWN', 'CEAHC') and " +
				 " ((CAST(substr(d.acceptedQe,4) AS int) * 100) + CAST(substr(d.acceptedQe,0,3) AS int)) between :startMth and :endMth " +
				 " order by d.columnB")
				 .setParameter("employee", employee) 
				 .setParameter("firstday", begDateFinYear) 
				 .setParameter("lastday", lastDateFinYear)
				 .setParameter("startMth", startMth) 
				 .setParameter("endMth", endMth) .getResultList(); */
		if (do2List1.size() == 0){
			ceaChildren = 0;
		} else {
			String date = do2List1.get(0).getColumnB();
			ceaChildren = 1;
			for (Do2 d : do2List1){
				if (!date.equals(d.getColumnB())){
					ceaChildren = 2;
				}
			}
		}
		if (hossubChildren == 1){// hossub one child
			if (ceaChildren > 0){ 
			    cealimit += CEA_MAX_REBATE_PER_ANNUM;
			} 
		} else {
			cealimit = ceaChildren * CEA_MAX_REBATE_PER_ANNUM;
		}
		if (ceaamt > cealimit)
			rebateamt = cealimit;
		else
			rebateamt = ceaamt;
		return rebateamt;
	}	
	}