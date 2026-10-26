package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.RhLocation;
import itsdc.dolphin.repositories.AuditCageAllowanceRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
// HIGH ALTITUDE UN-CONGENIAL CLIMATE AREA - CONCURRENT RISK and HARDSHIP
 
@RestController
public class Hauca extends AuditCageBase implements Do2AuditCageProcessor {
	Employee employee;	
	List<Allowance> alwList = null;
 	static public final String NO_RATE_AVAILABLE="AU:Rate not available in Allowance Rate table.\n";
	static public final String BEFORE_START_DATE="VL:Admissible with effect from 22 Feb 2019.\n";
	static public final String BEFORE_PUBLICATION_START_DATE="VL:Publication Date Earlier Than AppendixJ Acceptance Date.\n";  
	static public final String PREVIOUS_ALLW_NOT_CANCELLED_CEASED="AU:Cancellation/Cessation Not Found For Previously Granted Field Allowance.\n";
	static public final String INVALID_PAOS="VL:Not Applicable For HQ 22 Estt. \n";
	static public final String PBOR_IS_RECRUIT="AU:Not admissible to recruits. \n";
	static public final String COLUMN_ABC_NULL="VL:Column A / B / C Values Cannot Be Blank. \n";
	static public final String INVALID_COLUMNA="VL:Invalid Column-A Value. \n";
	static public final String INVALID_COLUMNB="VL:Invalid Column-B Value. \n";
	static public final String INVALID_LOCATION="VL:Location code in Column-C is not valid location.\n";
	static public final String INVALID_COLUMNC="VL:Invalid Column-C. Column-C should contain location code.\n";
	static public final String INVALID_COLB_COLC_COMBINATION="VL:RH matrix in ColB does not match with location code given in ColC.\n";
	public static final String PBOR_ON_LEAVE = "AU:PBOR is on leave on the date of grant of allowance. \n";
	static public final String LOCATION_BEYOND_ELIGIBLE_PERIOD="VL:RH Location Found But Grant Date/Applicable Rate Beyond Admissible Period.\n";

	@Override
	public boolean isValid(Do2 do2) {
		
		if (do2.getColumnA()!=null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}
/*		
	//	if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) || Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_PAOS);
			do2Repo.save(do2);
			return false;
		}
	*/	
		if (do2.getColumnA()==null ||  do2.getColumnB()==null || do2.getColumnC()==null) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(COLUMN_ABC_NULL);
			do2Repo.save(do2);
			return false;
		}
		
		if (!do2.getColumnA().equals(Do2Manager.GRANTED)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_COLUMNA);
			do2Repo.save(do2);
			return false;
		}
		
		if (!Do2Manager.getValidColumnBValuesForHauca().contains(do2.getColumnB())) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_COLUMNB);
			do2Repo.save(do2);
			return false;
		}
		
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank!=null && rank.getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
		
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StringBuffer buffer=new StringBuffer();		 	
	 
		if (do2.getFromDate().before(DateManager.getRiskAndHaucaAllowanceStartDate())) { 
			buffer.append(BEFORE_START_DATE);
			validity = false;
		}
		
		if (do2.getDo2Date().before(DateManager.getRiskAndHaucaPublicationStartDate())) { 
			buffer.append(BEFORE_PUBLICATION_START_DATE);
			validity = false;
		}
				
		if (!isRateAvailable(do2)) { 
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}
		
		String validOther=verifyOtherInadmissibleFieldAllowances(do2);
		if(!validOther.startsWith("valid")) {
			validity=false;
			buffer.append(validOther);
		}
		
		/*if (!verifyOtherInadmissibleFieldAllowances(do2)) {
			buffer.append(PREVIOUS_ALLW_NOT_CANCELLED_CEASED);  
			validity = false;
		}*/
 		
		
		if (do2.getColumnC()!=null && !do2.getColumnC().isEmpty()) { 			
			String locationCode=do2.getColumnC().toUpperCase();
			List<RhLocation> rhlList = rhLocationRepo.getFullRhLocationListForLocationCode(locationCode);
			if (rhlList==null || (rhlList!=null && rhlList.size()==0)) {
				validity=false;
				buffer.append(INVALID_LOCATION);
			} else {
				RhLocation rhl=null;
				rhl=rhLocationRepo.getRhLocationList(locationCode, do2.getFromDate());
				if (rhl==null && (locationCode.trim().toUpperCase().endsWith("/S"))) {
					rhl=rhLocationRepo.getRhLocationList(locationCode.substring(0, locationCode.indexOf("/S")), do2.getFromDate());
				}
				if (rhl==null && (locationCode.trim().toUpperCase().endsWith("S"))) {
					rhl=rhLocationRepo.getRhLocationList(locationCode.substring(0, locationCode.lastIndexOf("S")), do2.getFromDate());
				}							 
				if (rhl==null) {
					rhl=rhLocationRepo.getRhLocationList(locationCode+"/S", do2.getFromDate());
				}
				if (rhl==null) {
					rhl=rhLocationRepo.getRhLocationList(locationCode+"S", do2.getFromDate());
				}
				if (rhl!=null) {
					if (!rhl.getHhCode().trim().toUpperCase().equals(do2.getColumnB().trim())) {
						validity=false;
						buffer.append(INVALID_COLB_COLC_COMBINATION);
					}	 		
				} else {
					validity=false;
					buffer.append(LOCATION_BEYOND_ELIGIBLE_PERIOD);
				}
			}
		} else {
			validity=false;
			buffer.append(INVALID_COLUMNC);
		}  
		
		if (isPborOnRegularLeave(do2, employee)) {			 
			buffer.append(PBOR_ON_LEAVE);
			validity = false;
		}
		
		if (!validity) {
	 		do2.setStatus(Do2Manager.INVALID);				
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());	
		do2Repo.save(do2);		
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			alwList=null;
			alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());	
			// Hqrs. Office letter No.AT-I/Army/ALLO-3500/5/2020-PAYC/E-869 dated 09.04.2025 addressed to ITSDC and copy to all PCsDA/CsDA, received through email.
			if (newAuditCage.getPayLevel()!=null && ((newAuditCage.getPayLevel().length()<=2 && Integer.parseInt(newAuditCage.getPayLevel())>=9) 
					|| (newAuditCage.getPayLevel().length()==3 && newAuditCage.getPayLevel().equals("10B")))) {
				int paylevel=9; // to fetch appropriate record from allowance_rate table
				alwList=null;
				alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), paylevel, do2.getFromDate(), do2.getFromDate());
			} 
			if (alwList!=null && alwList.size()>0) {
				AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), alwList.get(0).getRateAmount());
			}		
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
		} else {
			final String[] alwUnderProcess = {do2.getDescription()};
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	} 

	public boolean isRateAvailable(Do2 do2) {
		alwList=null;
		alwList=Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());
		if (alwList!=null && alwList.size()>0) {
			return true;				 
		} else {
			return false;
		}
	}
	
	public String verifyOtherInadmissibleFieldAllowances(Do2 do2) {
		List<Do2> haucaReplacedGtdDo2List = null;	 
		haucaReplacedGtdDo2List=do2Repo.findByDescListColAFromDateLessThanEqual(do2.getEmployee().getId(),"V",Do2Manager.getRiskHaucaReplacedAllowancesList().subList(4,7), "GRANTED",do2.getFromDate());
		if (haucaReplacedGtdDo2List!=null && haucaReplacedGtdDo2List.size()>0) {
	 		List<Do2> haucaReplacedCtdDo2List = null;
			haucaReplacedCtdDo2List =do2Repo.findByDescColAFromDateGreaterThanEqualFromDateLessThanEqual(do2.getEmployee().getId(), "V",haucaReplacedGtdDo2List.get(0).getDescription(), "CEASED", haucaReplacedGtdDo2List.get(0).getFromDate(), do2.getFromDate()); 
			if (haucaReplacedCtdDo2List==null || (haucaReplacedCtdDo2List!=null && haucaReplacedCtdDo2List.size()==0)) {
				List<AuditCage> auditCageList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(do2.getEmployee().getId(), do2.getFromDate());
			  	if (auditCageList!=null && auditCageList.size()>0) {
					List<AuditCageAllowance> acaList = acaRepo.findByAuditCageOrderByPayCode(auditCageList.get(0).getId());
					if (acaList!=null && acaList.size()>0) {
						for (AuditCageAllowance aca : acaList) {
							if (Do2Manager.getRiskHaucaReplacedAllowancesList().subList(4, 7).contains(aca.getPayCode().getPayCode())) {
								return haucaReplacedGtdDo2List.get(0).getDescription()+" not ceased/cancelled. "+do2.getDescription()+" not admissible with "+haucaReplacedGtdDo2List.get(0).getDescription();
							}
						}
					}
					return "valid";							
				}				
				return haucaReplacedGtdDo2List.get(0).getDescription()+" not ceased/cancelled. "+do2.getDescription()+" not admissible with "+haucaReplacedGtdDo2List.get(0).getDescription();
 			} else {
 				return "valid";
			}
		} else {
			return "valid";
		} 
	}
	
	public Boolean isLastCharAlpha(String str) {
		String last = str.substring(str.length() - 1);
		if (last.matches("^[a-zA-Z]"))
			return true;
		else
			return false;
	}
	
	public boolean isPborOnRegularLeave(Do2 do2, Employee employee) {
		 
		List<Do2> do2List = do2Repo.findForDescriptionsWithFromDateBetween(employee.getId(), Do2Manager.VALID,
				Do2Manager.PENDING, Do2Manager.getAllLeaveTypes(), do2.getFromDate());
		for (Do2 leaveDo2 : do2List) {
			if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription())) {
				Date leaveDo2FromDate = leaveDo2.getFromDate();
				Date leaveDo2ToDate = leaveDo2.getToDate();
				if (do2.getFromDate().equals(DateManager.getRiskAndHaucaAllowanceStartDate())) {
					List<Do2> recallDo2List = do2Repo.findRecallDo2(employee, leaveDo2FromDate);
					if (recallDo2List != null && recallDo2List.size() > 0) {
						if (recallDo2List.get(0).getToDate().equals(leaveDo2.getToDate())) {
							return false;
						}
					}
					List<Do2> replacedFieldAllowanceCeasedList = do2Repo.findForDescriptionsColumnAWithFromDateBetween(
							employee.getId(), Do2Manager.VALID, Do2Manager.PENDING,
							Do2Manager.getRiskHaucaReplacedAllowancesList().subList(4, 7), do2.getFromDate());
//					System.out.println("..............noofdays...hauca........"+ DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate)+" leavfrdt "+leaveDo2FromDate+" leavtodt "+leaveDo2ToDate);
//System.out.println(".........hauca replaced descriptions ...."+ Do2Manager.getRiskHaucaReplacedAllowancesList().subList(4,7)+"  listsize  " +replacedFieldAllowanceCeasedList.size());

					if (replacedFieldAllowanceCeasedList == null || (replacedFieldAllowanceCeasedList != null
							&& replacedFieldAllowanceCeasedList.size() == 0)) {
						return true;
					}
					if (!(leaveDo2.getDescription().equals("CL") && leaveDo2.getDescription().equals("EXTNCL"))) {
					    return false;	
					}
					if (DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate) > 14.0
							&& (leaveDo2.getDescription().equals("CL") || leaveDo2.getDescription().equals("EXTNCL"))) {
						return true;
					}					 
				} else {
					if (!(leaveDo2.getDescription().equals("CL") && leaveDo2.getDescription().equals("EXTNCL"))) {
					    return false;	
					}
					if (DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate) > 14.0
							&& (leaveDo2.getDescription().equals("CL") || leaveDo2.getDescription().equals("EXTNCL"))) {
						return true;
					}					 
				}
			}
		}
		return false;
	}
}