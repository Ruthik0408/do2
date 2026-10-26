package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
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

public class Tla1 extends AuditCageBase implements Do2AuditCageProcessor,java.io.Serializable {

	public List<Allowance> alwList;
	public StringBuffer buffer = new StringBuffer();
	Employee emp;

	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public final String INVALID_FORMAT= "VL:TLA1/TLA2/TLA3  are w.e.f 01-07-2017.\n";
	
	@Override	
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getColumnA()!=null && do2.getColumnA().equals("CEASED")) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		
		if (!isValidFormat(do2)) {
		   	buffer.append(INVALID_FORMAT);
		   	validity = false;
		}
		
		if(!isValidAllowanceRate(do2)) {
			validity = false;
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			do2.setStatus(Do2Manager.PENDING);
		}
		if(!isServiceDetailAvailable(do2)){
			validity = false;
			buffer.append(Do2Manager.MISSING_SERVICE_DETAIL);
			do2.setStatus(Do2Manager.PENDING);
		}
		if(validity) {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}
	
	public boolean isServiceDetailAvailable(Do2 do2){
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return rank != null;
		
	}
		
	public boolean isValidAllowanceRate(Do2 do2) {
		AuditCage auditCage = getBaseAuditCage(do2.getFromDate(), do2);
		int oldGradePay = 0;
		if (auditCage != null){
			oldGradePay = auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay();
		}
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),
						  oldGradePay,do2.getFromDate(),do2.getFromDate());
				
		return (alwList.size() > 0);
	}
	
	public AuditCage getBaseAuditCage(Date fromDate, Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atCageList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(employee.getId(), fromDate);
				
		/*		Utils.getEntityManager().createQuery(
			" SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.cancelled=false AND " +
			" ac.effective=true AND ac.recordStatus!='P' AND ac.fromDate <=:fromDate order by ac.fromDate desc, ac.toDate desc")
				.setParameter("employee", employee)
				.setParameter("fromDate", fromDate).getResultList();*/
        if (atCageList.size() > 0)
        	return atCageList.get(0);
        return null;
	}

	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(), 
						newAuditCage.getOldGradePay(),do2.getFromDate(), do2.getFromDate());				
			if (alwList.size() > 0) {			
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnB(), alwList.get(0).getRateAmount());
			}				
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			List<String> alwToStop = new ArrayList<String>();
			
			if(do2.getDescription().equals("TLA1")){
				final String[] alwUnderProcess = 
				{"ANAL","ARUNA","HIMA1","JK1","LAKDWP","MIZO1","SIKKIM","UK","NAGLND","TRIPU1"};	
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("TLA2")){
				final String[] alwUnderProcess = 
				{"HIMA2","JK2","MIZO2","MANI","TRIPU2"};	
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("TLA3")){
				final String[] alwUnderProcess = 
				{"HIMA3","ASMEGH"};	
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}
			
		
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
	
	
public boolean isValidFormat(Do2 do2) {
		
		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			return false;
		} else {
			return true;
		}
		
	}
}