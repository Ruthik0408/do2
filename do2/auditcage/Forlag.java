package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
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
 * 
 * @author IT & SDC , Secunderabad
 * 
 */
@RestController
public class Forlag extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	
	static public String RATE_NOT_AVAILABLE = "AU:Rate not available in the Allowance Rate table.\n ";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";
	
	private StringBuffer buffer = new StringBuffer();
	private Rank rankCode;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isRankAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(SERVICE_DETAIL_UNAVAILABLE);
	 	}
		
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			if (!isRateAvailable(do2)) {
				validity = false;
				do2.setStatus(Do2Manager.PENDING);
				buffer.append(RATE_NOT_AVAILABLE);
				
		 	}
		}
		
		
		if (validity){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		
		
		
		return validity;
	}
	
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
	
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
	
		if (do2.getColumnA().trim().equals(Do2Manager.CEASED)) {
			List<String> alwToStop = Arrays.asList(new String[]{ Do2Manager.FORLAG });
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		} else {
			if (do2.getColumnA().trim().equals(Do2Manager.GRANTED)) {
				if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					List<Allowance> allowances=null;	 
					allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());
					if (allowances!=null && allowances.size()==1) {
						AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.FORLAG, allowances.get(0).getRateAmount());
					}
				} else {
					AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.FORLAG, do2.getRate());
				}
			
			}
		}
		return Do2Manager.SUCCESS;
	}
	
	
	
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		return newAuditCage;
	}
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	public boolean isRankAvailable(Do2 do2) {
		if (employee == null) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo()); 
			rankCode = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		}
		if (rankCode != null) {
			return true;
		}
		return false;
	}
	

	
	
	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances=null;
		if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {			 
			allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());
			if (allowances!=null && allowances.size()==1) {
				return true;
			}
		} else {
			if (do2.getRate()==null || (do2.getRate()!=null && do2.getRate()==0)) {
				return false;
			}
			allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate());
			if (allowances!=null && allowances.size()>0) {
				for (Allowance ar : allowances) {
					if (ar.getRateAmount()==do2.getRate()) {
						return true;
					}
				}			 
			}			
		}	
		return false;
	}
	
	
	
}