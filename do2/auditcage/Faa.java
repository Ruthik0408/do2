package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;

import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;

 
/**
 * 
 * @author ITSDC, SEC'BAD.
 *
 */

@RestController
public class Faa extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	
	public List<Allowance> alwList;
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String BEFORE_START_DATE = "AU:Admissible from 01/09/2008. \n";
	static public final String HONY_RANKS = "AU:Not admissible to JCOs / Honorary Ranks. \n";

	
	public StringBuffer buffer = new StringBuffer();
	Rank rank;
	Employee emp;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
				
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
			
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
		
		if (rank!=null && Integer.parseInt(rank.getRankCode()) > 49 ) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(HONY_RANKS);
			do2Repo.save(do2);
			return false;
		}
		
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			do2.setReason(BEFORE_START_DATE);
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
			return false;
		}
		
		if(!validity) {
			if (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING)) {
				do2.setStatus(Do2Manager.INVALID);
			}	
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
	
		int hraAdmissible = 0;
		// following three lines added due to sevencpc
		int gradePay = 0;
		int msPay = 0;
		int basicPay = 0;
		
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
	
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {	
								
				
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getFromDate(), do2.getFromDate());

			if (alwList.size() > 0) {
				final String[] alwUnderProcess = { 	Do2Manager.CILQ,	Do2Manager.HRA, Do2Manager.HRAFAM, 
													Do2Manager.HRALF, Do2Manager.FAMLF };
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
				
				/*int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
				int msPay = newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay();*/
				 
			// above lines commented and  changed as stated below due to seven cpc
				if(do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
					 basicPay = newAuditCage.getBasicPay();
					 gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
					 msPay = newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay();
				} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					 basicPay = newAuditCage.getOldBasicPay();
						gradePay = newAuditCage.getOldGradePay() == null ? 0 : newAuditCage.getOldGradePay();
						msPay = Utils.get6cpcMsPay(newAuditCage);
				}
				
				
				hraAdmissible = (int) Math.round(((basicPay + gradePay + msPay) 
													* alwList.get(0).getRatePercentage()));
		
				AuditCageHelper.updateAuditCageAllowance(newAuditCage,Do2Manager.FAA, hraAdmissible);
			}
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {

			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);

			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;

	}
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
}