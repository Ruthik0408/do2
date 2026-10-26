package itsdc.dolphin.do2.auditcage;
 
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author K.RAMADEVI &  N. RAMACHANDRAN  Dt. 06.10.2009.
 * System generated description used to arrive at pay on restoration of stopped increments penalty of the PBOR.
 */

@RestController
public class Rstpin extends Stopin {
				
	 
	EmployeeServiceDetail esd;
	boolean validity = true;
	private List <Do2> incrDo2List;
	private List<PayScale>payList;
	private List<AuditCage> acList; 
	Employee employee;
	Rank rank;
	Date fromDate, toDate;
	Do2 refDo2;
	int basicPay, gradePay, newBasicPay, xPay, noOfIncrements, dearnessPay, incrRate, period;
	//static public final List<String> NQS_TYPE = Arrays.asList("LORANK", "OSL", "REVERT", "RANKS", "AWL", "DESN", "MILRI", "ARREST"); 
	
	static public final String REF_DOII_NOT_CORRECT= "AU:Incorrect Reference DOII- not referring to Punishment DOII.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		do2Repo.save(do2);
	
		return true;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		if(newAuditCage.getDo2().getColumnA().equals("NONCUM")) {
			newAuditCage.setBasicPay(getBasicPay(newAuditCage.getDo2()));
		} else {
            refDo2 = Utils.getReferenceDo2(newAuditCage.getDo2());		
			acList = getAuditCageList(refDo2);
			newAuditCage.setBasicPay(acList.get(0).getBasicPay());
		}
		return newAuditCage;
	}
	
	@Transactional
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		do2.setStatus(Do2Manager.VALID);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	public int getBasicPay(Do2 do2) {
		String soldierGroup;
		
			incrDo2List = getIncrList(do2);
			if (incrDo2List!=null && incrDo2List.size() > 0)
				noOfIncrements = incrDo2List.size();
			acList = getAuditCageList(do2);
			if (acList.size() > 0) {
				 if(!do2.getFromDate().before(DateManager.getSixthCpcDate())) {
					 basicPay = acList.get(0).getBasicPay();
					 gradePay = acList.get(0).getGradePay();
					 xPay = acList.get(0).getXGroupPay()==null ? 0 : acList.get(0).getXGroupPay();
					 for (int i=1; i<=noOfIncrements; i++) {
						 int incrAmount = 0;
						 incrAmount =  Utils.getIncrmentRoundedToTen(((int) basicPay+gradePay+xPay), do2.getFromDate());
						 basicPay = basicPay + incrAmount;
						
					 }
					 newBasicPay = basicPay;
				 } else {
					 basicPay = acList.get(0).getBasicPay();
					 //rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
					 rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
					 if (employee.getRemusteringDate() != null && do2.getFromDate().before(employee.getRemusteringDate()) ) {
						soldierGroup = employee.getPreviousGroupCategory().getSoldierGroup();
					} else {
						soldierGroup = employee.getGroupCategory().getSoldierGroup();
					}
					
					payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),
																								soldierGroup, do2.getFromDate());
							
							
					/*		Utils.getEntityManager().createQuery("SELECT pl FROM PayScale AS pl WHERE "
								+ " pl.rank=:rank AND pl.soldierGroup.soldierGroup=:soldierGroup AND "
								+ " ((:fromDate BETWEEN pl.fromDate AND pl.toDate ) OR "
								+ "  (:fromDate >= pl.fromDate AND pl.toDate IS null )) AND "
								+ " pl.startScale IS NOT null")
								.setParameter("rank",rank)
								.setParameter("soldierGroup",soldierGroup)
								.setParameter("fromDate", do2.getFromDate())
								.getResultList();*/
						
					incrRate = payList.get(0).getIncr0();
					newBasicPay = basicPay+(incrRate*noOfIncrements);
				}
			}
		return newBasicPay;
	}
		
	public List<Do2> getIncrList(Do2 do2){
		refDo2 = Utils.getReferenceDo2(do2);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> incrDo2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanEqualToAndDesc(employee.getId(), "INCR",
																						refDo2.getFromDate(), do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:employee " +
		 		" AND d.description=:description AND d.fromDate >= :fromDate AND d.fromDate<=:toDate ")
		 					.setParameter("employee",employee)
		 					.setParameter("description", "INCR")
		 					.setParameter("fromDate", refDo2.getFromDate())
		 					.setParameter("toDate", do2.getFromDate())
		 					.getResultList();*/
		
		
		 
		return (incrDo2List.size() > 0 ? incrDo2List : null);
	}
	
	public List<AuditCage> getAuditCageList(Do2 do2){
		acList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),
																								do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE ac.employee=:employee AND " +
				" ac.effective=true AND ac.cancelled=false AND ac.fromDate<=:fromDate AND ac.recordStatus!='P' AND " +
				" ac.absentFlag=false ORDER BY ac.fromDate DESC ").setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
								.setParameter("fromDate", do2.getFromDate()).getResultList();*/
	
		return (acList.size() > 0 ? acList : null);
	}
	
}
