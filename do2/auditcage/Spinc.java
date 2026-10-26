package itsdc.dolphin.do2.auditcage;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.SoldierGroup;

import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;

 

/**
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Spinc extends AuditCageBase implements Do2AuditCageProcessor,  java.io.Serializable {
	
	public List<Allowance> alwList;

	static public String INVALID_RATE = "VL:Rate in Rate column not valid.\n";
	static public String INVALID_TJRA = "VL:No. of increments should be between 1 and 5.\n";
	static public String INVALID_SPINCS_BEFORE = "AU:5 increments already granted in entire service.\n";
	static public String INVALID_SPINCS_AFTER = "AU:No. of increments exceeding 5, in entire service, if those in current do2 are granted.\n";
	static public String INVALID_START = "AU:Claim not starting from 1st of the Month.\n";
	
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	Rank rank;
	public int basicPay = 0;
	public int dp_or_gradePay = 0;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit()+"");
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		
		if (!isValidRate(do2)) {
			validity = false;
			buffer.append(INVALID_RATE);
		}
		
		if (!isValidTjra(do2)) {
			validity = false;
			buffer.append(INVALID_TJRA);
		}
		
		if (!isValidSpincEntireService(do2)) {
			validity = false;
			buffer.append(INVALID_SPINCS_BEFORE);
		}

		if (!isFirstOfTheMonth(do2)) {
			validity = false;
			buffer.append(INVALID_START);
		}
		
		if (!isValidSpincs(do2)) {
			validity = false;
			buffer.append(INVALID_SPINCS_AFTER);
		}

		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setQe(Utils.getCurrentQuarter());
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}
	
	public boolean isValidSpincs(Do2 do2) {
		return ((getTotalSpincsInService(do2) + do2.getTjra()) <= 5);
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		if (newDo2.getId()==do2.getId()){
			if (emp.getSportIncrement()==null){
				emp.setSportIncrement(do2.getTjra());
			} else {
				emp.setSportIncrement(emp.getSportIncrement()+do2.getTjra());	
			}
		}
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		int existingAmount=0;
		List<AuditCageAllowance> atallList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
			/*	Utils.getEntityManager().createQuery(
				"SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage")
				.setParameter("auditCage",newAuditCage)
				.getResultList();*/
		
		for (AuditCageAllowance aca : atallList){
			if (aca.getPayCode().getPayCode().equals("SPINC")){
				existingAmount = aca.getAmount();
			}
		}
		int currentIncrement = calculate_Increment(do2,newAuditCage);
	
		System.out.println ( " AAAAAA " +do2.getFromDate()     +" existing rate   " +existingAmount   +"  current increment   " +currentIncrement );
		
		AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.SPINC, (currentIncrement+existingAmount));
		
		return Do2Manager.SUCCESS;
	}
	
	public int calculate_Increment(Do2 do2,AuditCage atCage) {
		
		int total_inc = 0;
		int increment_arrived_at = 0;
		
			if (do2.getFromDate().before(DateManager.getSpincRevisedDateSixCpc())) {
				if (do2.getTjra() == 1) {
					increment_arrived_at = getIncrementVcpc(do2);
				} else {
					increment_arrived_at = getIncrementVcpc(do2) * do2.getTjra();
				}
			
				total_inc = increment_arrived_at;			
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
				List<Allowance> alwList = Utils.getApplicableAllowanceRates("STRINC",
								do2.getFromDate(), do2.getFromDate(), rank);
				if (alwList.size() > 0) {
					if (do2.getTjra() == 1) {					
						increment_arrived_at = (int) alwList.get(0).getRateAmount();
						total_inc = increment_arrived_at;
				
					} else {
						increment_arrived_at = (int) alwList.get(0).getRateAmount();
						total_inc = increment_arrived_at * do2.getTjra();
					}
				}
			
			} else  {
				
				String payLevel = null;
				payLevel = atCage.getPayLevel();
				List<Allowance>alwList  = Utils.getApplicableAllowanceRates(do2.getDescription(),  Do2Manager.getSevenCpcGradepay(payLevel), payLevel,do2.getFromDate(),
	                    do2.getFromDate());
				
				if (alwList.size() > 0) {
					if (do2.getTjra() == 1) {					
						increment_arrived_at = (int) alwList.get(0).getRateAmount();
						total_inc = increment_arrived_at;
				//		System.out.println ( " kkkk  rank " +rank.getRankName()    +"  total rate " +total_inc);
					} else {
						increment_arrived_at = (int) alwList.get(0).getRateAmount();
						total_inc = increment_arrived_at * do2.getTjra();
					}
				}
			}
			
		return total_inc;
	}
	
	public int getIncrementVcpc(Do2 do2) {
		int incr = 0;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		String soldierGroup = employee.getGroupCategory().getSoldierGroup();
		if (employee.getRemusteringDate() != null
				&& do2.getFromDate().before(employee.getRemusteringDate())) {
			soldierGroup = employee.getPreviousGroupCategory().getSoldierGroup();
		}

		Date fifthCpcDate = null;
		try {
			fifthCpcDate = DateManager.getDate("2005-12-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		List<PayScale> payList = psRepo.findByRankStartScaleGroupFromDateBetweenOrFromDateGreaterThanEqaual(rank.getId(),
																							soldierGroup, fifthCpcDate);
				
		/*		Utils.getEntityManager().createQuery(
						"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
								+ " AND ps.startScale > 0 "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
				.setParameter("rank", rank).setParameter("grp", soldierGroup)
				.setParameter("fromDate", fifthCpcDate).getResultList();*/

		if (payList.size() > 0) {
			incr = payList.get(0).getIncr0();
		}
	//	System.out.println ( " v cpc incr spinc "     +"  " +do2.getFromDate()   +" rank " +rank.getRankName()   +" soldier group " +soldierGroup    +" rate   " +incr);
		return incr;
	}

	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		Employee emp =Utils.getCurrentEmployee(currentDo2.getArmyNo());
		emp.setSportIncrement(emp.getSportIncrement()-refDo2.getTjra());
		return Do2Manager.SUCCESS;
	}

	public boolean isValidRate(Do2 do2) {
		return (do2.getRate() != null || do2.getRate() > 0);
	}
	
	public boolean isValidTjra(Do2 do2) {
		return (do2.getTjra() > 0 && do2.getTjra() < 6);
	}
	
	public boolean isValidSpincEntireService(Do2 do2) {
		return (getTotalSpincsInService(do2)< 6);
	}
	
	public boolean isFirstOfTheMonth(Do2 do2) {
		Calendar c1 = new GregorianCalendar();
		c1.setTime(do2.getFromDate());
		return (c1.get(Calendar.DATE) == 1);
	}
	
	public int getTotalSpincsInService(Do2 do2) {
		Employee employee =Utils.getCurrentEmployee(do2.getArmyNo());
		int spincCount = 0;
		List<Do2> spincDo2List = do2Repo.findByDescIdNotEqual(employee.getId(), Do2Manager.VALID, do2.getDescription(),
																					do2.getId());
				
			/*	Utils.getEntityManager().createQuery("SELECT spincdo2 FROM Do2 spincdo2 where " + 
									"spincdo2.employee=:employee and  spincdo2.description=:desc and " + 
									"spincdo2.id!=:do2id and spincdo2.status=:do2status")
									.setParameter("employee", employee)
									.setParameter("desc", do2.getDescription())
									.setParameter("do2id", do2.getId())
									.setParameter("do2status", Do2Manager.VALID).getResultList();*/
		if (spincDo2List.size() > 0) {
			for (Do2 sd : spincDo2List) {
				spincCount += sd.getTjra();
			}
		}
		return spincCount;
	}
}