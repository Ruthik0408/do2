package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date; 
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
@RestController
public class Comndo extends AuditCageBase implements Do2AuditCageProcessor,
		 java.io.Serializable {

	public List<Allowance> alwList;
	public StringBuffer buffer = new StringBuffer();

	Rank rank;
	
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public String SERVICE_DETAIL_UNAVAILABLE = "AU:Employee Service Detail is not available.\n";
	static public String INVALID_UNIT_PUBLICATION = "AU:Not published by authorized unit.\n";
	static public final String NOT_ON_DEPUTATION = "AU:PBOR is not on deputation to UNMISSION";

	@Override
	public boolean isValid(Do2 do2) {
		if (do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		boolean validity = true;
	/* Auth : PAO Para letter No.PARA/DDP/DOL/CORR dated 11.06.2014 to remove cut off date
	   Auth : PAO Para letter No.PARA/DDP/DOL/CORR dated 03-04-2013 and 19-08-2013 to incorporate cut off date
	          as 01-10-2011
		Date paraComndoDate = null;
		try {
			paraComndoDate = DateManager.getDate("2011-10-01");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Utils.getPao().getPaoCode().equals("96") && do2.getFromDate().after(paraComndoDate)) {*/
		if (Utils.getPao().getPaoCode().equals("96")) {
			if (!isComndoUnit(do2)) {			
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(INVALID_UNIT_PUBLICATION);
			}
		}
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
     
		if (!isValidServiceDetail(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(SERVICE_DETAIL_UNAVAILABLE);
		}
		/*if (!isPborOnUNMissionDeputation(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(NOT_ON_DEPUTATION);
			validity = false;
		}*/
		if (!isValidAllowanceRate(do2)) {
			validity = false;
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
		}

		if (validity){
			do2.setStatus(Do2Manager.VALID);
		}
			
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	public boolean isValidServiceDetail(Do2 do2) {
		return rank != null;
	}

	public boolean isValidAllowanceRate(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(),
				rank);
		return alwList.size() > 0;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {	
	 
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
					do2.getFromDate(), rank);
			if (alwList.size() > 0) {

				AuditCageHelper.updateAuditCageAllowance(newAuditCage,
						Do2Manager.COMNDO, alwList.get(0).getRateAmount());
				
			}
			final String[] alwUnderProcess = { "PARA", "PARAR" };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		} else {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}
	 public boolean isPborOnUNMissionDeputation(Do2 do2) {
			Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
			List<EmployeeServiceDetail> esdList = esdRepo.findByFromDateLessThanEqualAndStatus(do2.getEmployee().getId(), do2.getFromDate(), Do2Manager.DEPU_UNMISSION+"");
					
			 
			
			if (esdList.size() > 0 &&  ((esdList.get(0).getFromDate().equals(do2.getFromDate()) ||
			   (esdList.get(0).getFromDate().before(do2.getFromDate()))))) {
			
				if (esdList.get(0).getToDate()==null){		
					return true;
				} else {
					if (do2.getFromDate().after(esdList.get(0).getToDate())){					
				    	return false; 
				     } else {			    	
				    	 return true;
				     }
				} 
			}  
				return false;
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
	public boolean isComndoUnit(Do2 do2) {
		if (do2.getUnit() != null && do2.getUnit().getParaUnit()){
			return true;
		} else {
			List<Do2> do2List =  do2Repo.findByDescFromDateGreaterThanEqual(do2.getEmployee().getId(), Do2Manager.VALID, "POSOUT",do2.getFromDate());
			 
			if (do2List.size() == 0) {
				return false;
			} else {
				if (do2List.get(0).getUnit().getParaUnit())
					return true;
			}
		}
		return false;
	}
	
	
}