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
public class Rlca1 extends AuditCageBase implements Do2AuditCageProcessor,java.io.Serializable {

	public List<Allowance> alwList;
	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	Date sevencpcDate = DateManager.getSeventhCpcDate();

	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	static public final String INVALID_FORMAT= "VL:To be published as TLA1/TLA2/TLA3 w.e.f 01-07-2017.\n";
	@Override	
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getColumnA()!=null && do2.getColumnA().equals("CEASED")) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
		
		if(!isValidAllowanceRate(do2)) {
			validity = false;
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			do2.setStatus(Do2Manager.PENDING);
		}
		
		if (!isValidFormat(do2)) {
		   	buffer.append(INVALID_FORMAT);
		   	validity = false;
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
		//Rank rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		return rank != null;
		
	}
	/*public boolean isValidAllowanceRate(Do2 do2) {
		AuditCage auditCage = getBaseAuditCage(do2.getFromDate(), do2);
		int basicPay = 0;
		int gradePay = 0;
		if (auditCage != null){
			basicPay = auditCage.getBasicPay();
			gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
		}
		if(do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),
						do2.getFromDate(), do2.getFromDate(), basicPay);
			} else {
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),
						  gradePay,do2.getFromDate(),do2.getFromDate());
			}
		
		return (alwList.size() > 0);
	}*/
	
	// above method commented and changed as stated below due to sevencpc
	
	public boolean isValidAllowanceRate(Do2 do2) {
		AuditCage auditCage = getBaseAuditCage(do2.getFromDate(), do2);
		int basicPay = 0;
		int gradePay = 0;
		int oldBasicPay = 0;
		int oldGradePay = 0;
		
		if (auditCage != null){
			basicPay = auditCage.getBasicPay();
			gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
			oldGradePay = auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay();
			oldBasicPay = auditCage.getOldBasicPay() == null ? 0 : auditCage.getOldBasicPay();
		}
		
		
				
		if (optcpcDo2For7cpcExists(do2) && oldGradePay==0) {
			oldGradePay = gradePay;
		}
		
		if(do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),
						do2.getFromDate(), do2.getFromDate(), basicPay);
			} else  if (do2.getFromDate().before(sevencpcDate)){
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),
						  gradePay,do2.getFromDate(),do2.getFromDate());
			} else {
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(),
						  oldGradePay,do2.getFromDate(),do2.getFromDate());
			}
		
		return (alwList.size() > 0);
	}
	
	public AuditCage getBaseAuditCage(Date fromDate, Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atCageList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(employee.getId(),fromDate);
				
				
		/*		Utils.getEntityManager().createQuery(
			" SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.cancelled=false AND " +
			" ac.effective=true AND ac.recordStatus!='P' AND ac.fromDate <=:fromDate order by ac.fromDate desc, ac.toDate desc")
				.setParameter("employee", employee)
				.setParameter("fromDate", fromDate).getResultList();*/
        if (atCageList.size() > 0)
        	return atCageList.get(0);
        return null;
	}

/*	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			if(do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(), 
							do2.getFromDate(), do2.getFromDate(), newAuditCage.getBasicPay());		
			} else {
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(), 
							newAuditCage.getGradePay(),do2.getFromDate(), do2.getFromDate() );
			}
			
			if(alwList.size() > 0){
			
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnB(), alwList.get(0).getRateAmount());
			}
				
				
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			List<String> alwToStop = new ArrayList<String>();
			
			if(do2.getDescription().equals("RLCA1")){
				final String[] alwUnderProcess = 
				{"ANAL1","ARUNA1","HIMA1","JK1","LAKDWP","MIZO1","SIKKIM","UP"};	
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("RLCA2")){
				final String[] alwUnderProcess = 
				{"ANAL2","ARUNA2","HIMA2","JK2","MIZO2","NAGLND","TRIPU1"};	
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("RLCA3")){
				final String[] alwUnderProcess = 
				{"HIMA3","JK3","MIZO3","MANI","TRIPU2"};	
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("RLCA4")){
				final String[] alwUnderProcess ={"HIMA4","ASMEGH"};
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}
			
		
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}

		return Do2Manager.SUCCESS;
	}*/

	// above method commented and modified as stated below due to sevencpc
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		int basicPay = 0;
		int gradePay = 0;
		int oldBasicPay = 0;
		int oldGradePay = 0;
		
		
	
		
		basicPay = newAuditCage.getBasicPay();
		gradePay = newAuditCage.getGradePay();
		oldBasicPay = newAuditCage.getOldBasicPay();
		oldGradePay = newAuditCage.getOldGradePay();
		
		if (optcpcDo2For7cpcExists(do2) && oldGradePay==0) {
			oldGradePay = gradePay;
		}
		
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
			if(do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(), 
							do2.getFromDate(), do2.getFromDate(), basicPay);		
			} else  if (do2.getFromDate().before(sevencpcDate)){
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(), 
							gradePay,do2.getFromDate(), do2.getFromDate() );
			} else {
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(),do2.getColumnB(), 
						oldGradePay,do2.getFromDate(), do2.getFromDate() );
				
			}
			
			if(alwList.size() > 0){
			
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnB(), alwList.get(0).getRateAmount());
			}
				
				
		} else if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			List<String> alwToStop = new ArrayList<String>();
			
			if(do2.getDescription().equals("RLCA1")){
				final String[] alwUnderProcess = 
				{"ANAL1","ARUNA1","HIMA1","JK1","LAKDWP","MIZO1","SIKKIM","UP"};	
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("RLCA2")){
				final String[] alwUnderProcess = 
				{"ANAL2","ARUNA2","HIMA2","JK2","MIZO2","NAGLND","TRIPU1"};	
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("RLCA3")){
				final String[] alwUnderProcess = 
				{"HIMA3","JK3","MIZO3","MANI","TRIPU2"};	
				Arrays.asList(alwUnderProcess);
				alwToStop = Arrays.asList(alwUnderProcess);
			}else if(do2.getDescription().equals("RLCA4")){
				final String[] alwUnderProcess ={"HIMA4","ASMEGH"};
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
		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			return false;
		} else {
			return true;
		}
		
	}
	
	public boolean optcpcDo2For7cpcExists(Do2 do2){
		List<Do2> do2List = do2Repo.findByEmployeeIdAndDescriptionAndStatusAndColumnB(do2.getEmployee().getId(),"OPTCPC", "SEVENPC","V");
				
		//		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' and d.description='OPTCPC' "
			//		+ " AND d.columnB='SEVENPC' ").setParameter("emp",do2.getEmployee()).getResultList();
		
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		   	return true;
		}
		return false;
	}
	
}