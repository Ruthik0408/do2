package itsdc.dolphin.do2.auditcage;
 
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
 
import itsdc.dolphin.do2.auditcage.*;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.ReadyReckoner;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;

import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.sql.Timestamp;
import java.util.Arrays;

/**
 * 
 * @author K.RAMADEVI &  N. RAMACHANDRAN  Dt. 06.10.2009.
 * 
 */

@RestController
public class Stopin extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
				
	 
	EmployeeServiceDetail esd;
	private StringBuffer buffer = new StringBuffer();
	boolean validity = true;
	private List<EmployeeServiceDetail> esdList;
	private List<AuditCage> acList; 
	Employee employee;
	int nqsPeriod;
	Rank rank;
	Date fromDate, toDate;
	Do2 refDo2;
	int basicPay, gradePay, newBasicPay, xPay, noOfIncrements, dearnessPay, incrRate, period;
	//static public final List<String> NQS_TYPE = Arrays.asList("LORANK", "OSL", "REVERT", "RANKS", "AWL", "DESN", "MILRI", "ARREST"); 
	
	static public final String REF_DOII_NOT_CORRECT= "AU:Incorrect reference DOII - not referring to PUNISHMENT DOII.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		if (newDo2.getId() == do2.getId()) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			
		 		for (Do2 incrDo2 : getIncrList(do2)) {
					AuditCageHelper.generateIncrCancelDo2s(incrDo2);
				}
				List<Do2> do2List = do2Repo.findByFromDateGreaterThanAndDescription(employee.getId(), "RSTPIN", do2.getFromDate());
						
						
				/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee " +
						"AND d.description=:description and d.fromDate>:fromDate")
					   .setParameter("employee",employee)
					   .setParameter("description", "RSTPIN")
					   .setParameter("fromDate", do2.getFromDate())
					   .getResultList();*/
				if(do2List.size() ==0){
					generateRstpinDo2(do2);
				}
				do2.setStatus(Do2Manager.VALID);
				do2.setReason(null);
			}
		 		do2Repo.save(do2);
		 
		return validity;
		}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}
	
	@Transactional
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	 public boolean isValidRefDo2(Do2 do2) {
		refDo2= Utils.getReferenceDo2(do2);
		
		esdList = esdRepo.findByEmployeeIdAndStatusAndDo2Id(employee.getId(), Do2Manager.ESD_PUNISHMENT+"", refDo2.getId());
				
	/*			Utils
		.getEntityManager()
		.createQuery(
				"select esd from EmployeeServiceDetail esd where esd.do2.employee = :employee AND esd.status=:status and esd.do2.id = :id")
				.setParameter("employee", employee)
				.setParameter("status",Do2Manager.ESD_PUNISHMENT)
				.setParameter("id", refDo2.getId())
				.getResultList();*/
	
		return (esdList.size() == 1); 
	} 
		
	public List<Do2> getIncrList(Do2 do2){
		Date toDate = do2.getToDate();
	
		if(do2.getToDate()==null)  {
			toDate = DateManager.getMonthSkipDate(do2.getFromDate(), do2.getTjra());
			
		}
		
		List<Do2> incrDo2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanEqualToAndStatusAndDescription(employee.getId(),
																							"INCR", do2.getFromDate(), toDate, Do2Manager.VALID);
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee " +
		 		" AND d.description=:description AND d.fromDate >= :fromDate AND d.fromDate<=:toDate AND d.status=:vld")
		 					.setParameter("employee",employee)
		 					.setParameter("description", "INCR")
		 					.setParameter("fromDate", do2.getFromDate())
		 					.setParameter("toDate", toDate)
		 					.setParameter("vld", Do2Manager.VALID)
		 					.getResultList();*/
		 
		return (incrDo2List.size() > 0 ? incrDo2List : null);
	}
	
	public List<AuditCage> getAuditCageList(Do2 do2){
		acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(Utils.getCurrentEmployee(do2.getArmyNo()).getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE ac.employee=:employee " +
				" AND ac.effective=true AND ac.cancelled=false AND ac.absentFlag=false AND ac.fromDate <= :fromDate " +
				" AND ac.recordStatus!='P' ORDER BY ac.fromDate DESC ")
								.setParameter("fromDate", do2.getFromDate())
								.setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
								.getResultList();*/
	
		return (acList.size() > 0 ? acList : null);
	}

	public void generateRstpinDo2(Do2 do2){
	
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		toDate = DateManager.getMonthSkipDate(do2.getFromDate(), do2.getTjra());
		Do2 rstpinDo2 = new Do2();
			rstpinDo2.setEmployee(employee);
			rstpinDo2.setArmyNo(do2.getArmyNo());
			rstpinDo2.setUnitCode(do2.getUnitCode());
			Unit uc=unitRepo.findByUnitCode(do2.getUnitCode());
			rstpinDo2.setUnit(uc);
	//		rstpinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", do2.getUnitCode()));
			rstpinDo2.setDo2Group(do2.getDo2Group());
			rstpinDo2.setCheckDigit(do2.getCheckDigit());
			rstpinDo2.setDo2Type(do2.getDo2Type());
			rstpinDo2.setDo2No(do2.getDo2No());
			rstpinDo2.setDo2ItemNo(do2.getDo2ItemNo());
			rstpinDo2.setDo2Date(DateManager.getCurrentDate());
			rstpinDo2.setDescription("RSTPIN");
			rstpinDo2.setFromDate(toDate);
			rstpinDo2.setStatus(Do2Manager.PENDING);
			rstpinDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
			rstpinDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			rstpinDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			rstpinDo2.setAuditCage(true);
			rstpinDo2.setQe(Utils.getCurrentQuarterEnding());
			do2Repo.save(rstpinDo2);
		//	Utils.getEntityManager().persist(rstpinDo2);
	
	}
	
}
