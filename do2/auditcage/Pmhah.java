package itsdc.dolphin.do2.auditcage;

import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;

/*
 * 
 * author : IT & SDC (Secunderabad)
 * 
 */
@RestController
public class Pmhah extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {
	 
	double pmhaRate = 0.0;
	String status = "";
	StringBuffer buffer = new StringBuffer();
	Employee employee;
	List<Allowance> alwList;
	Rank rank;
  	public static String HONY_RANK = "AU:Not admissible to Hony Ranks.\n";
	static public final String PBOR_IS_RECRUIT = "AU:Not admissible to recruits. \n";
	static public final String HONY_RANKS = "AU:Not admissible to Honorary Ranks. \n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean pmhahValidity = true;
		if(newDo2!=null && newDo2.getId()== do2.getId()){
			if(do2.getDescription().equals("PMHAL")){
				do2.setReason("Publication Not Required for PMHAL. System Generates the Amount");
				do2.setStatus(Do2Manager.INVALID);
				do2Repo.save(do2);
				return false;
			}
		}
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank=Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"ALLCE");
		//rejects if pmhah is published for ta personnel appt category D and O
		
		if(employee.getRegularTaFlag() !=null && employee.getRegularTaFlag().equals("T")
				&& employee.getTaAppointmentCategory() !=null && 
				(employee.getTaAppointmentCategory().equals("D") ||
				employee.getTaAppointmentCategory().equals("O"))){
			do2.setReason("PMHA Not Admissible for TA Personnel: Departmental and Others");
	    	do2.setStatus(Do2Manager.INVALID);
	    	do2Repo.save(do2);
		
		}
		
		if(do2.getColumnA()!= null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		} 
		
		if (rank!=null 	&& 
			(rank.getRankCode().equals(Do2Manager.HONYLT) || rank.getRankCode().equals(Do2Manager.HONYCP))) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(HONY_RANKS);
			do2Repo.save(do2);
			return false;
		}
		/*if (!isServiceDetailAvailable()) {
			pmhahValidity = false;
			status = Do2Manager.PENDING;
			buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
		}*/
	 
		if (pmhahValidity)
			updatePmhahValidRecord(do2);
		else
			updatePmhahErrorRecord(do2, buffer, status);

		return pmhahValidity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,AuditCage baseAuditCage) {
		int gsoCode = 1;
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		if(employee.getGsoCode() != null){
			gsoCode = employee.getGsoCode();
			if(gsoCode == 3){
				gsoCode = 1;
			}
		}
		if(newAuditCage.getPmha()!= null)
			pmhaRate = newAuditCage.getPmha();
		
		Do2 do2 = newAuditCage.getDo2();
		Pao pao;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pao = Utils.getPao();
		} else {
			pao = null; 
		}

		if (do2.getDescription().equals("PMHAH") && do2.getColumnA().equals("GRANTED")) {
			
			alwList = Utils.getApplicableAllowanceRates("PMHAH", do2.getFromDate()
					,do2.getFromDate(), gsoCode, pao);
			if(alwList.size() > 0)
			pmhaRate = alwList.get(0).getRateAmount();			
		} else {
			alwList = Utils.getApplicableAllowanceRates("PMHAL", do2.getFromDate(),do2.getFromDate(), gsoCode, pao);
			if (alwList.size() > 0) {
				pmhaRate = alwList.get(0).getRateAmount();
				if (employee.getInitialMusteringDate()!=null && 
						newAuditCage.getFromDate().before(employee.getInitialMusteringDate()))
					pmhaRate = (pmhaRate * 3 / 4);
			}
		}
		newAuditCage.setPmha((int) pmhaRate);
		return newAuditCage;
	}

	// ***************************************************************
	 

	public boolean isServiceDetailAvailable() {
		
		return (rank != null && !employee.getSubstantiveRank()
				.getRankCode().equals(Do2Manager.RECRUIT));

		/*return (rank == null && !employee.getSubstantiveRank()
				.getRankCode().equals(Do2Manager.RECRUIT));*/

	}

	public void updatePmhahValidRecord(Do2 do2) {
		do2.setReason(null);
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
	}

	public void updatePmhahErrorRecord(Do2 do2, StringBuffer buffer,
			String status) {
		do2.setStatus(status);
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		// blank
		return Do2Manager.SUCCESS;
	}
}
