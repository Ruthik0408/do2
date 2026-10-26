package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * Author : A J NORONHA & T VANAJA, IT & SDC, Secunderabad.
 */
@RestController
public class Hca extends AuditCageBase implements 
		Do2AuditCageProcessor {
	Employee employee;
	Rank rank;
	List<Allowance> alwList = null;
	StringBuffer buffer = new StringBuffer();
	Date sevencpcDate = DateManager.getSeventhCpcDate();

	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String NOT_A_VALID_RANK = "AU:Invalid rank on date of effect.\n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n";
 
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		//method replaced on 15/04/2012
		if(employee.getSubstantiveRank()!=null){
			int rankCode = Integer.parseInt(employee.getSubstantiveRank().getRankCode());
			if(rankCode ==10){
				do2.setStatus(Do2Manager.PENDING);
				do2.setReason(PBOR_IS_RECRUIT);
				do2Repo.save(do2);
				return false;
			}
		}
		
		if (!(isServiceDetailAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.SERVICE_DETAIL_MISSING);
			validity = false;
		}
		if (!(isRateAvailable(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}

		if (!validity) {
			if ((do2.getStatus() == null) || (do2.getStatus() != null
					&& !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(buffer.toString());
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//line 104 to 115 added,parameter in 121 changed to basicpay
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
		int dearnessPay = newAuditCage.getDp() == null ? 0 : newAuditCage.getDp();
		int oldBasicPay = newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay();
		// following two lines added due to sevencpc
		int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
		int oldGradePay = newAuditCage.getOldGradePay() == null ? 0 : newAuditCage.getOldGradePay();
		
		if(newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
			if(newAuditCage.getFromDate().before(DateManager.getDPDate())){
				;
			}else{
				basicPay= basicPay+dearnessPay;
			}
		}
	//	if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
		// HCA abolished with effect from 01-07-2017.
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {	
			/*if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(), do2.getFromDate(),
						do2.getFromDate(), basicPay);
				

			} else if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(),newAuditCage.getGradePay(), do2.getFromDate(),
						do2.getFromDate() );
				

			} else if (!do2.getFromDate().before(DateManager.getSixthCpcDate())
					&& do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(), do2.getFromDate(),
						do2.getFromDate(), newAuditCage.getOldBasicPay());
				
				
			}*/
			
			// above lines commented and amended as below due to sevencpc
			
			if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {  // up to 31-12-2005
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(), do2.getFromDate(),
						do2.getFromDate(), basicPay);
				

			} else if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {  // w.e.f 01-01-2016
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(),oldGradePay, do2.getFromDate(),
						do2.getFromDate() );
				

			} else if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) &&
					 do2.getFromDate().before(DateManager.getSeventhCpcDate())) {  // w.e.f 01-09-2008
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(),gradePay, do2.getFromDate(),
						do2.getFromDate() );
				

			} else if (!do2.getFromDate().before(DateManager.getSixthCpcDate()) // w.e.f 01-01-2006 to 31-08-2008
					&& do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				alwList = Utils.getApplicableAllowanceRates(do2
						.getDescription(), do2.getFromDate(),
						do2.getFromDate(), oldBasicPay);
			}
					
			for(Allowance alw : alwList){
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2
						.getDescription(), alw.getRateAmount());
			}
			

			AuditCageHelper.allowInclusiveAllowances(do2,
					newAuditCage, baseAuditCage);
		} else {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		return Do2Manager.SUCCESS;
	}

	// *************************************************************************
	public boolean isDo2AfterMuster(Do2 do2) {
		
		if(employee.getInitialMusteringDate() !=null && do2.getFromDate().before(employee.getInitialMusteringDate()))
			return false;
		if(employee.getAttestationDate() !=null && do2.getFromDate().before(employee.getAttestationDate()))
			return false;
		return true;
	}

	public boolean isServiceDetailAvailable(Do2 do2) {
		if (rank == null)
			return false;
		else
			return true;
	}

	public boolean isRateAvailable(Do2 do2) {
		AuditCage auditCage = getBaseAuditCage(do2.getFromDate(), do2);
		int basicPay = 0;
		int gradePay = 0;
		int oldGradePay = 0;
		if(auditCage != null){
			basicPay = auditCage.getBasicPay();
			gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
			// folllowing line added due to sevencpc
			oldGradePay = auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay();
		}
/*		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(),
					do2.getFromDate(), auditCage.getBasicPay());
			

		} else if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(),auditCage.getGradePay(), do2.getFromDate(),
					do2.getFromDate() );
			
						
		} else if (!do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(),
					do2.getFromDate(), auditCage.getOldBasicPay());
			
			
		}*/
		
		// above lines commented and amended as stated below due to sevencpc
		
		if (optcpcDo2For7cpcExists(do2) && oldGradePay==0) {
			oldGradePay = gradePay;
		}
		
	
		
		
		
		/*
		 * if (do2.getFromDate().before(DateManager.getSixthCpcDate())) { alwList =
		 * Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
		 * do2.getFromDate(), auditCage.getBasicPay());
		 * 
		 * } else if (!do2.getFromDate().before(sevencpcDate)) { // w.e.f 01-01-2016
		 * alwList = Utils.getApplicableAllowanceRates(do2
		 * .getDescription(),oldGradePay, do2.getFromDate(), do2.getFromDate() );
		 * 
		 * 
		 * } else if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())
		 * && do2.getFromDate().before(DateManager.getSeventhCpcDate())) { // w.e.f
		 * 01-09-2008 alwList = Utils.getApplicableAllowanceRates(do2
		 * .getDescription(),gradePay, do2.getFromDate(), do2.getFromDate() );
		 * 
		 * } else if (!do2.getFromDate().before(DateManager.getSixthCpcDate()) &&
		 * do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) { alwList =
		 * Utils.getApplicableAllowanceRates(do2 .getDescription(), do2.getFromDate(),
		 * do2.getFromDate(), auditCage.getOldBasicPay());
		 * 
		 * 
		 * }
		 */
		
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
					do2.getFromDate(), auditCage.getBasicPay());
		
		} else if (!do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(), do2.getFromDate(),
					do2.getFromDate(), auditCage.getOldBasicPay());
			
			
		} else if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) &&
				 do2.getFromDate().before(sevencpcDate)) {  // w.e.f 01-09-2008
			alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(),gradePay, do2.getFromDate(),
					do2.getFromDate() );
		
		} else if (!do2.getFromDate().before(sevencpcDate)) {  // w.e.f 01-01-2016
			alwList = Utils.getApplicableAllowanceRates(do2
					.getDescription(),oldGradePay, do2.getFromDate(),
					do2.getFromDate() );
		} 
		return (alwList.size() > 0);
		
	}
	public AuditCage getBaseAuditCage(Date fromDate, Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atCageList = acRepo.findByFromDateLessThanEqualOrderByFromDateDescToDateDescBpDesc(employee.getId(),
																											fromDate);
				
	/*			Utils.getEntityManager().createQuery(
			" SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.cancelled=false AND " +
			" ac.effective=true AND ac.recordStatus!='P' AND ac.fromDate <=:fromDate order by ac.fromDate desc, ac.toDate desc")
				.setParameter("employee", employee)
				.setParameter("fromDate", fromDate).getResultList();*/
        if (atCageList.size() > 0)
        	return atCageList.get(0);
        return null;
	}
	public boolean isDrawingHca(Do2 do2){
		List<String> allowanceList = null;
		allowanceList = Arrays.asList(new String[] {Do2Manager.HCA});
		return Utils.isMutexAllowance(do2.getArmyNo(), allowanceList, do2.getFromDate(), do2.getToDate(),do2);
	}
	
	public boolean optcpcDo2For7cpcExists(Do2 do2){
		List<Do2> do2List =do2Repo.findByDescColB(do2.getEmployee().getId(),"V","OPTCPC","SEVENPC");
				
			//	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' and d.description='OPTCPC' "
		//			+ " AND d.columnB='SEVENPC' ").setParameter("emp",do2.getEmployee()).getResultList();
		
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		   	return true;
		}
		return false;
	}
}