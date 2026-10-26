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
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Hrdall extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	 
	String message;
	Employee employee;
	Rank rank;
	static public String RATE_NOT_AVAILABLE = "AU:Incorrect Rate Percentage in rate column  \n ";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";

	// 'orders awaited' validation in respect of HRDALL removed from do2home, vide HQRs letter No. AT/1/16/Appx-J/Revised dated 20.01.2014 

	private StringBuffer buffer = new StringBuffer();
	private double hrdallRate = 0.0;
		
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getColumnA() != null 
				&& do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
	//	if (do2.getRate() != null && (do2.getRate() == 12.5 || do2.getRate() == 15.0)) {
		/*if (do2.getRate() != null) {
			do2.setStatus(Do2Manager.VALID);
	 	} else {			
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(RATE_NOT_AVAILABLE);
	  	}*/
		
		if (!isRateAvailable(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(RATE_NOT_AVAILABLE);
		}
		
		if(validity) {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
			return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
	
			
		AuditCageHelper.copyCurrentAllowances(newAuditCage,	baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
				AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ do2.getDescription() }), newAuditCage);
		} else if ((do2.getColumnA().equals(Do2Manager.GRANTED))) {
			//hrdallRate = AuditCageHelper.getHrdallRate(do2,newAuditCage);
			hrdallRate = AuditCageHelper.getHrdallRate(do2,newAuditCage.getDo2(),newAuditCage);
			AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), hrdallRate);
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			
			
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
	
	public boolean isRateAvailable(Do2 do2) {
		List<Allowance> allowances = new ArrayList<Allowance>();
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(),
				do2.getFromDate());
		
		return allowances.size() >0;
	}
	
}