package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Date;
import java.util.List; 
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.RhLocation; 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils; 
//  RISK ALLOWANCE wef 22022019
@RestController
public class Risk extends AuditCageBase implements Do2AuditCageProcessor {
	Employee employee;	
	List<Allowance> alwList = null; 
	static public final String NO_RATE_AVAILABLE="AU:Rate not available in Allowance Rate table.\n";
	static public final String BEFORE_START_DATE="VL:Admissible with effect from 22 Feb 2019.\n";
	static public final String BEFORE_PUBLICATION_START_DATE="VL:Publication Date Earlier Than AppendixJ Acceptance Date.\n";
	static public final String INVALID_RATE_COLUMN="VL:Invalid Value In Rate Column.\n";
	static public final String PREVIOUS_ALLW_NOT_CANCELLED_CEASED="AU:Cessation/Cancellation Not Found For Previously Granted Field Allowance.\n";
//	static public final String INVALID_PAOS="VL:Not Applicable For APS-Nagpur / HQ 22 Estt. \n";
	static public final String INVALID_PAOS="VL:Not Applicable For HQ 22 Estt. \n";
	static public final String PBOR_IS_RECRUIT="AU:Not admissible to recruits. \n";
	static public final String COLUMN_ABC_NULL="VL:Column A / B / C Values Cannot Be Blank. \n";
	static public final String INVALID_COLUMNA="VL:Invalid Column-A Value. \n";
	static public final String INVALID_COLUMNB="VL:Invalid Column-B Value. \n";
	public static final String PBOR_ON_LEAVE = "AU:PBOR is on leave on the date of grant of allowance. \n";
//	static public final String INVALID_LOCATION="VL:Location code in Column-C is not valid location.\n";
	static public final String INVALID_LOCATION="VL:Incorrect location code found in PTO.  Kindly check up the location code with IHQ.\n"; // added on 01.05.2025
	static public final String INVALID_COLUMNC="VL:Invalid Column-C. Column-C should contain location code.\n";
	static public final String INVALID_COLB_COLC_COMBINATION="VL:RH matrix in ColB does not match with location code given in ColC.\n";
	static public final String INVALID_VALUE_IN_COLUMNC="VL:Invalid Value In Column-C. CFAA-CMFAA Not Allowed In This Combination.\n";
	static public final String AFTER_GOVT_SANCTION_DATE = "AU:CHAFAA Admissible upto 31/12/2026. \n";  
	static public final String INVALID_RATE_COLC_COMBINATION="VL:Admissible Percentage For RH Location Not Match Rate Column.\n";
	static public final String INVALID_DESC_COLC_COMBINATION="VL:RH Location Not Match For The DOII Description.\n";
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
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(COLUMN_ABC_NULL);
			do2Repo.save(do2);
			return false;
		}
		
		if (do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().length()==0)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(COLUMN_ABC_NULL);
			do2Repo.save(do2);
			return false;
		}
		
		if (do2.getColumnB()==null || (do2.getColumnB()!=null && do2.getColumnB().length()==0)) {
			if (do2.getColumnC()!=null && !(do2.getColumnC().equals("CFAA") || do2.getColumnC().equals("CMFAA") || do2.getColumnC().equals("CHAFAA"))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(COLUMN_ABC_NULL);
				do2Repo.save(do2);
				return false;
			}
		}
	 
		/*
		if (do2.getColumnA()==null ||  do2.getColumnB()==null || do2.getColumnC()==null) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(COLUMN_ABC_NULL);
			do2Repo.save(do2);
			return false;
		}*/
		
		if (!do2.getColumnA().equals(Do2Manager.GRANTED)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_COLUMNA);
			do2Repo.save(do2);
			return false;
		}
		
		if (do2.getColumnC()!=null && !(do2.getColumnC().equals("CFAA") || do2.getColumnC().equals("CMFAA") || do2.getColumnC().equals("CHAFAA"))) {
			if (!Do2Manager.getValidColumnBValuesForRisk().contains(do2.getColumnB())) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(INVALID_COLUMNB);
				do2Repo.save(do2);
				return false;
			}
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
				
		if (do2.getRate()!=null 
				&& (do2.getRate()==100 || do2.getRate()==77 || do2.getRate()==60)) {
			;
		} else { 
			buffer.append(INVALID_RATE_COLUMN);
			validity = false;
		}
		
		if (!isRateAvailable(do2)) { 
			buffer.append(NO_RATE_AVAILABLE);
			validity = false;
		}
	
		if (isPborOnRegularLeave(do2, employee)) {			 
			buffer.append(PBOR_ON_LEAVE);
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
			if (!do2.getColumnC().equals("CFAA") && !do2.getColumnC().equals("CMFAA") && !do2.getColumnC().equals("CHAFAA")) {
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
						if (!rhl.getRhCode().trim().toUpperCase().equals(do2.getColumnB().trim())) {
							validity=false;
							buffer.append(INVALID_COLB_COLC_COMBINATION);
						}
		//				if (do2.getRate()!=null && rhl.getAdmissiblePercentage()!=do2.getRate()) {  Modified on 07.01.2026
						if (do2.getRate()!=null && do2.getRate()>rhl.getAdmissiblePercentage()) {  
							validity=false;
							buffer.append(INVALID_RATE_COLC_COMBINATION);
						}
							// the following validation check removed as the R2H2 non rationalised rates of CFAA, CMFAA published under RISK. As we are
							// taken care of regarding leave period (AL,CL) more than authorised limit, the validation check removed on 081012024. 
							// As appendix_j for form RISK with column_c as CFAA, CMFAA, CHAFAA is not required in this scenario. (since matrix and
							//(with percentage namely 100, 77,60 )
						/*	if (!rhl.getDescription().trim().contains(do2.getDescription().trim())) {
								validity=false;
								buffer.append(INVALID_DESC_COLC_COMBINATION);
							}	*/			
					} else {
						validity=false;
						buffer.append(LOCATION_BEYOND_ELIGIBLE_PERIOD);
					}
				}
			} 
		} else {
			validity=false;
			buffer.append(INVALID_COLUMNC);
		} 
		
		if (do2.getColumnC()==null || do2.getColumnC().isEmpty() || do2.getColumnC().isBlank()) {
			validity=false;
			buffer.append(INVALID_COLUMNC);
		} /*else if (do2.getColumnB()!=null && do2.getColumnB().trim().length()>0 &&
					(do2.getColumnC()!=null && (do2.getColumnC().trim().equals("CFAA") || do2.getColumnC().trim().equals("CMFAA")))) {
			validity=false;
			buffer.append(INVALID_VALUE_IN_COLUMNC);
		}*/	 
		
		if (do2.getColumnC()!=null && do2.getColumnC().trim().toUpperCase().equals("CHAFAA")
				&& !do2.getFromDate().before(DateManager.getChafaaStopDate())) {		 
			buffer.append(AFTER_GOVT_SANCTION_DATE);
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
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			final String[] alwUnderProcess = {do2.getDescription(), "CFAA", "CMFAA", "CHAFAA"};
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			return Do2Manager.SUCCESS;
		}
		
		if (do2.getColumnA().equals(Do2Manager.GRANTED)) {
			alwList=null;
		//	System.out.println("111111111111");
			if (do2.getColumnC()!=null 
					&& (do2.getColumnC().trim().toUpperCase().equals("CFAA") || do2.getColumnC().trim().toUpperCase().equals("CMFAA")
						||do2.getColumnC().trim().toUpperCase().equals("CHAFAA"))) {
				Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
				Pao pao;
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
					pao = Utils.getPao();
				} else {
					pao = null;
				}
		//		System.out.println("22222222");
				alwList = Utils.getApplicableAllowanceRates(do2.getColumnC().trim().toUpperCase(), do2.getFromDate(), do2.getFromDate(), rank, pao);				
			} else {
				// Hqrs. Office letter No.AT-I/Army/ALLO-3500/5/2020-PAYC/E-869 dated 09.04.2025 addressed to ITSDC and copy to all PCsDA/CsDA, received through email.
				if (newAuditCage.getPayLevel()!=null && ((newAuditCage.getPayLevel().length()<=2 && Integer.parseInt(newAuditCage.getPayLevel())>=9) 
						|| (newAuditCage.getPayLevel().length()==3 && newAuditCage.getPayLevel().equals("10B")))) {
					int paylevel=9; // to fetch appropriate record from allowance_rate table
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), paylevel, do2.getFromDate(), do2.getFromDate());
				} else {
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(), do2.getFromDate());	
				}
						
		//		System.out.println("33333333333");
			}
		//	System.out.println(" 4444444444444444  alwlist size  "+ alwList.size());
			if (alwList!=null && alwList.size()>0) {
				for (Allowance allowance : alwList) {
					 if (do2.getColumnC()!=null 
							&& (do2.getColumnC().trim().toUpperCase().equals("CFAA") || do2.getColumnC().trim().toUpperCase().equals("CMFAA")
									||do2.getColumnC().trim().toUpperCase().equals("CHAFAA"))) {
			//			 System.out.println(" 555555555555555555  "+" colc "+do2.getColumnC());
						AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnC().trim().toUpperCase(), allowance.getRateAmount()); 
						break;
					} else	if (allowance.getRatePercentage()==do2.getRate()) {
			//			System.out.println(" 666666666666666666666  ");
						AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), allowance.getRateAmount());
						break;
					} 									
				}				
			}		
			AuditCageHelper.allowInclusiveAllowances(do2, newAuditCage, baseAuditCage);
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
			for (Allowance allowance : alwList) {
				if (allowance.getRatePercentage()==do2.getRate() && allowance.getRateAmount()>0 && allowance.getRateAmount()<=21625) {
					return true;
				}						
			}
		}
		if (do2.getColumnC()!=null && (do2.getColumnC().trim().toUpperCase().equals("CFAA") 
				|| do2.getColumnC().trim().toUpperCase().equals("CMFAA") || do2.getColumnC().trim().toUpperCase().equals("CHAFAA"))) {
			Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "ALLCE");
			Pao pao;
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				pao = Utils.getPao();
			} else {
				pao = null;
			}
			alwList=null;
			alwList = Utils.getApplicableAllowanceRates(do2.getColumnC().trim().toUpperCase(), do2.getFromDate(), do2.getFromDate(), rank, pao);				
			if (alwList!=null && alwList.size()>0) {
				 return true;
			}
		}
		return false;		 		
	}
	
	public String verifyOtherInadmissibleFieldAllowances(Do2 do2) {	 
		List<Do2> riskReplacedGtdDo2List = null;
		riskReplacedGtdDo2List = do2Repo.findByDescListColAFromDateLessThanEqual(do2.getEmployee().getId(),"V",Do2Manager.getRiskHaucaReplacedAllowancesList().subList(0,4), "GRANTED", do2.getFromDate());
		if (riskReplacedGtdDo2List!=null && riskReplacedGtdDo2List.size()>0) {
			List<Do2> riskReplacedCtdDo2List = null;
			riskReplacedCtdDo2List =do2Repo.findByDescColAFromDateGreaterThanEqualFromDateLessThanEqual(do2.getEmployee().getId(),"V",riskReplacedGtdDo2List.get(0).getDescription(),"CEASED",riskReplacedGtdDo2List.get(0).getFromDate(),do2.getFromDate());
			if (riskReplacedCtdDo2List==null || (riskReplacedCtdDo2List!=null && riskReplacedCtdDo2List.size()==0)) {
				List<AuditCage> auditCageList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(do2.getEmployee().getId(), do2.getFromDate());
			  	if (auditCageList!=null && auditCageList.size()>0) {
					List<AuditCageAllowance> acaList = acaRepo.findByAuditCageOrderByPayCode(auditCageList.get(0).getId());
					if (acaList!=null && acaList.size()>0) {
						for (AuditCageAllowance aca : acaList) {
							if (Do2Manager.getRiskHaucaReplacedAllowancesList().subList(0, 4).contains(aca.getPayCode().getPayCode())) {
								return riskReplacedGtdDo2List.get(0).getDescription()+" not ceased/cancelled. "+do2.getDescription()+" not admissible with "+riskReplacedGtdDo2List.get(0).getDescription();
							}
						}
					}
					return "valid";							
				}				
				return riskReplacedGtdDo2List.get(0).getDescription()+" not ceased/cancelled. "+do2.getDescription()+" not admissible with "+riskReplacedGtdDo2List.get(0).getDescription();
			} else {
 				return "valid";
			}
		} else {
			return "valid";
		}  
	}	

	public boolean isPborOnRegularLeave(Do2 do2, Employee employee) {
	 
		List<Do2> do2List = do2Repo.findForDescriptionsWithFromDateBetween(employee.getId(), Do2Manager.VALID,
				Do2Manager.PENDING, Do2Manager.getAllLeaveTypes(), do2.getFromDate());
		for (Do2 leaveDo2 : do2List) {
//			System.out.println(".........risk replaced descriptions111111111111111111111 ...."+ Do2Manager.getRiskHaucaReplacedAllowancesList().subList(0, 4));

			if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription())) {
				Date leaveDo2FromDate = leaveDo2.getFromDate();
				Date leaveDo2ToDate = leaveDo2.getToDate();
				List<Do2> recallDo2List = do2Repo.findRecallDo2(employee, leaveDo2FromDate);
				if (do2.getFromDate().equals(DateManager.getRiskAndHaucaAllowanceStartDate())) {
					if (recallDo2List != null && recallDo2List.size() > 0) {
						if (recallDo2List.get(0).getToDate().equals(leaveDo2.getToDate())) {
							return false;
						}
					}
					List<Do2> replacedFieldAllowanceCeasedList = do2Repo.findForDescriptionsColumnAWithFromDateBetween(
							employee.getId(), Do2Manager.VALID, Do2Manager.PENDING,
							Do2Manager.getRiskHaucaReplacedAllowancesList().subList(0, 4), do2.getFromDate());
					System.out.println("..............noofdays..........."+ DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate)+" leavfrdt "+leaveDo2FromDate+" leavtodt "+leaveDo2ToDate);
//System.out.println(".........risk replaced descriptions ...."+ Do2Manager.getRiskHaucaReplacedAllowancesList().subList(0, 4)+"  listsize  " +replacedFieldAllowanceCeasedList.size());
					if (replacedFieldAllowanceCeasedList == null || (replacedFieldAllowanceCeasedList != null
							&& replacedFieldAllowanceCeasedList.size() == 0)) {
						return true;
					}
//					System.out.println("1111111111111111111");
					if (DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate) > 30.0
							&& (leaveDo2.getDescription().equals("CL") || leaveDo2.getDescription().equals("EXTNCL"))) {
						return true;
					}
	//				System.out.println("22222222222222222222222");
					if (DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate) > 28.0
							&& !(leaveDo2.getDescription().equals("CL") || leaveDo2.getDescription().equals("EXTNCL"))) {
						return true;
					}
				} else {
					if (DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate) > 30.0
							&& (leaveDo2.getDescription().equals("CL") || leaveDo2.getDescription().equals("EXTNCL"))) {
						if (recallDo2List!=null && recallDo2List.size()>0) {
							for (Do2 d : recallDo2List) {
								if (d.getToDate().equals(do2.getToDate())) {
									if (DateManager.getNoOfDays(do2.getFromDate(), DateManager.getPreviousDate(d.getFromDate()))>30) {
										return true;
									} 
								}
							}	
						} else {
							return true;
						}
					}
					if (DateManager.getNoOfDays(leaveDo2FromDate, leaveDo2ToDate) > 28.0
							&& !(leaveDo2.getDescription().equals("CL") || leaveDo2.getDescription().equals("EXTNCL"))) {
						if (recallDo2List!=null && recallDo2List.size()>0) {
							for (Do2 d : recallDo2List) {
								if (d.getToDate().equals(do2.getToDate())) {
									if (DateManager.getNoOfDays(do2.getFromDate(), DateManager.getPreviousDate(d.getFromDate()))>28) {
										return true;
									} 
								}
							}	
						} else {
							return true;
						}
					}
				}				 
			}
		}
		return false;
	}
	
	public Boolean isLastCharAlpha(String str) {
		String last = str.substring(str.length() - 1);
		if (last.matches("^[a-zA-Z]"))
			return true;
		else
			return false;
	}
}


