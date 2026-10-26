package itsdc.dolphin.do2.auditcage;

 

import itsdc.dolphin.models.AuditCage; 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank; 
import itsdc.dolphin.do2.Do2Manager; 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List; 

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;
 
@RestController
public class Optfix extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	private String message;
	boolean validity = true; 
	Employee employee;
	String refDo2NoForAcProFix = "";
	Do2 actualMacpsPromotDo2 = null;
	static public final String INVALID_OPTION_DATE = "VL:OptionDate/ToDate Is Blank OR Is After SOS/Discharge Date \n";  // added on 03.12.2025
 //	static public final String INVALID_OPTION_DATE = "VL:Option Can Be Exercised Using This Description Between 01-07-2016 And 01.07.2019 \n";
//	static public final String INVALID_PROMOT_DATE = "VL:PROMOT/MACPS Date (FromDate) Should Be Between 01-01-2016 And 31.07.2019 \n";
	static public final String INVALID_PROMOT_DATE = "VL:PROMOT/MACPS Date (FromDate) Should Be Between 01-01-2016 And 31.12.2020 \n";
	static public final String PROMOT_NOT_EXISTS = "AU:Valid PROMOT/MACPS DO2 Not Found On From-Date \n";
	static public final String PAY_FIXATION_NOT_INVOLVED = "AU:Pay Fixation Not Involved For The Promotion.  Hence Option Not Possible \n";
	static public final String OPTCPC_DO2_EXISTS = "AU:OPTCPC DO2 Exists On Promot/Upgradation Date.  OPTFIX Not Possible \n";
	static public final String PAY_NOT_IN_7CPC  = "AU:Pay Not Drawn In 7cpc As On Promot/Upgradation Date.  OPTFIX Not Possible \n";
	static public final String RO_PUBLICATION_REQUIRED  = "VL:OPTFIX To Be Published By Only Record Offices \n";
	static public final String INCR_NOT_DUE_ON_OPTION_DATE  = "AU:Increment Not Due In Pre-Promoted/Pre-Upgraded Grade On Opted Date \n";
	static public final String INVALID_COLUMNB  = "AU:Invalid Value In Column7B In Relation To Column7A Value \n";
	static public final String INCR_PROMOT_MACPS_SAME_DATE="AU:MACPS/Promotion On Regular Increment Date.  Not Admissible. \n";

	@Override
	public boolean isValid(Do2 do2) {		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		StringBuffer buffer = new StringBuffer();

		/*
		 * if (do2.getDo2Type()==null || (do2.getDo2Type()!=null &&
		 * do2.getDo2Type()!='1')) { buffer.append(RO_PUBLICATION_REQUIRED);
		 * validity=false; }
		 */
	
		// These two methods are modified as per the HQrs letter No. Army/Pay/ORs/Appendix J / Vol-1A/E-1804 ( DFA/3524- vol 1.2) dated 07.01.2021
		
		  if (!isValidFromDate(do2)) {
			  buffer.append(INVALID_PROMOT_DATE);
			  validity=false; 
		  }
		  
		  if (!isValidToDate(do2, employee)) { 
			  buffer.append(INVALID_OPTION_DATE);
		  	  validity=false; 
		  }
		 
	
		if (!isValidColumnB(do2)) {
			buffer.append(INVALID_COLUMNB); 
			validity=false;
		}
		
		if (isDuplicate(do2, employee)) {	
			buffer.append("DU:Duplicate Do2 \n"); 
			validity=false;
		}
		
		if (!isValidPromotExists(do2, employee)) {
			buffer.append(PROMOT_NOT_EXISTS);	 
			validity=false;
		}	
		
		if (!isPromotionBeneficial(do2, employee)) {
			buffer.append(PAY_FIXATION_NOT_INVOLVED);
			validity=false;
		}
					 
		if (doesOptcpcExists(do2, employee)) {
			buffer.append(OPTCPC_DO2_EXISTS);
			validity=false;
			do2.setStatus(Do2Manager.INVALID);
		}
		
		if (!doesIncrExistsOnOptionDate(do2, employee)) {
			buffer.append(INCR_NOT_DUE_ON_OPTION_DATE);
			validity=false;
			do2.setStatus(Do2Manager.INVALID);
		}
		
		if (!isPayDrawnIn7CPC(do2, employee)) {
			buffer.append(PAY_NOT_IN_7CPC);
			validity=false;	
		}

        if (do2.getFromDate()!=null && do2.getToDate()!=null) {
            if (do2.getFromDate().equals(do2.getToDate())) {
                buffer.append(INCR_PROMOT_MACPS_SAME_DATE);
                validity=false;
            } else {
                if (do2.getFromDate().equals(DateManager.getFirstDateOfMonth(do2.getFromDate()))
                        && do2.getToDate().equals(DateManager.getNextYear(do2.getFromDate()))) {
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(do2.getFromDate());
                    if (cal.get(Calendar.MONTH)==Calendar.JANUARY || cal.get(Calendar.MONTH)==Calendar.JULY) {
                        List<Do2> incrDo2List = do2Repo.findValidIncr(do2.getEmployee().getId(), do2.getFromDate());
                        if (incrDo2List!=null && incrDo2List.size()>0) {
                            buffer.append(INCR_PROMOT_MACPS_SAME_DATE);
                            validity=false;
                        }
                    }
                }
            }
        }
		
		if (validity) {
			Do2 promotDo2 = getPromotDo2(do2);
			if (promotDo2==null) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason("VL:Valid Reference DO2 Not Found \n");
			} else {
				do2.setStatus(Do2Manager.VALID);		 
				do2.setReason(null);	 
			}			
		} else {
			do2.setStatus(Do2Manager.INVALID);				
		}
		
		if (validity==false && !do2.getStatus().equals(Do2Manager.VALID)) {
			message = buffer.toString();
			do2.setReason(message);   
			
			if (actualMacpsPromotDo2!=null && actualMacpsPromotDo2.getToDate()!=null) {
				actualMacpsPromotDo2.setToDate(null);
				 
			}
			/*
			List<Do2> do2List = Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.fromDate=:fromdate "
					+ " and d.status='V' and d.description=:colA ").setParameter("emp", do2.getEmployee())
					.setParameter("fromdate", do2.getFromDate()).setParameter("colA", do2.getColumnA()).getResultList();
			
			if (do2List!=null && do2List.size()>0) {
				do2List.get(0).setToDate(null);
				Utils.getEntityManager().flush();				
			}*/
			List<Do2> do2List=null;
			List<String> descList=Arrays.asList(new String[] {"ACPFIX","PROFIX","HAVFIX"});
		    do2List = do2Repo.findByDescListFromDateEqual(do2.getEmployee().getId(), "V", descList, do2.getToDate());
		    		
		    	/*	Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.fromDate=:todate "
					+ " and d.status='V' and d.description in ('ACPFIX', 'PROFIX', 'HAVFIX') ").setParameter("emp", do2.getEmployee())
					.setParameter("todate", do2.getToDate()).getResultList();*/
		    if (do2List!=null && do2List.size()>0) {
				do2List.get(0).setStatus(Do2Manager.INVALID);
				 				
			}		    
		}
		
			do2Repo.save(do2);
		
		if (validity && do2.getId() == newDo2.getId()) {
			if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null && !do2.getToDate().before(do2.getFromDate())) {
		//		if (employee.getRegularTaFlag() != null && employee.getRegularTaFlag().equals("R")) {
					createProfixDo2(do2);
		//		}
			}
		}
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {		 
		return newAuditCage; 
	}
	
	@Transactional
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) { 
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);			
		Do2 promotDo2 = getPromotDo2(do2);
		if (promotDo2!=null) {
			if (!promotDo2.getDescription().trim().equals("HCOMMN")) {
				promotDo2.setToDate(do2.getToDate());				
			} else {
				promotDo2.setColumnC(DateManager.getStringDateWithoutSeparators(do2.getToDate()));
			}
			do2Repo.save(promotDo2);			
		}
		Employee employee = do2.getEmployee();
		if (do2.getToDate().after(Utils.getLastDayOfCurrentQuarter())) {
			employee.setIncrementDate(do2.getToDate());
			empRepo.save(employee);
		}  
		 
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {		
		List<Do2> refDo2List = Utils.getReferenceDo2List(refDo2);
		for (Do2 d : refDo2List) {
			if (Do2Manager.getIncrCancelDescription().contains(d.getDescription()) 
					&& d.getFromDate().equals(refDo2.getFromDate()) 
					&& refDo2.getColumnA().equals(d.getDescription())) {
				if (!d.getDescription().equals("HCOMMN")) {
					d.setToDate(null);
				} else {
					d.setColumnC(null);
				}					
				 
				break;
			}
		}
		return Do2Manager.SUCCESS;
	}
	
	public boolean isDuplicate(Do2 do2, Employee employee) {
		List<Do2> dupDo2List = do2Repo.findByDescIdNotEqualAndToDateEqual(employee.getId(),
																	"OPTFIX", do2.getToDate(),do2.getId());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='OPTFIX' "
		 		+ " and d.employee=:emp and d.status in ('V', 'P') and d.id!=:do2Id and d.toDate=:todate ")
				.setParameter("emp", employee).setParameter("do2Id", do2.getId()).setParameter("todate", do2.getToDate()).getResultList();*/
		return (dupDo2List!=null && dupDo2List.size()>0);
	}
	
	public boolean isValidFromDate(Do2 do2) {		 
	/*	if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
				 && !do2.getFromDate().after(DateManager.getOptfixPublicationEndDate())) {
			 return true;
		 } */
		
		if (do2.getFromDate()==null) 
			return false;
		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			 return true;
		 } 
		
		return false;
	}
	
	public boolean isValidToDate(Do2 do2, Employee emp) {		
		if (do2.getToDate()==null) 
			return false;
		/*
		if (do2.getToDate().after(DateManager.getSeventhCpcDate()) 
				 && !do2.getToDate().after(DateManager.getOptfixPublicationEndDate())) {
			 return true;
		}*/
		
		if (emp!=null && emp.getFsDueDate()!=null 
			&& do2.getToDate().after(emp.getFsDueDate())) {
			return false;
		}
		
		List<Do2> dischargeTypesDescriptions = do2Repo.findByDescListAndFromDateEarlier(emp.getId(), Do2Manager.VALID, Do2Manager.DISCHARGE_TYPES_FOR_OPTFIX, do2.getToDate());
		if (dischargeTypesDescriptions!=null && dischargeTypesDescriptions.size()>0) {
			 return false;
		}
		
 		if (do2.getToDate().after(DateManager.getSeventhCpcDate())) {
			 return true;
		}
		
		return false;
	}
	
	public boolean isValidColumnB(Do2 do2) {		 
		if (do2.getColumnB()==null || (do2.getColumnB()!=null && do2.getColumnB().length()==0)) {
			 return false;
		}
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("PROMOT")) {
			List<String> promotRanks = Arrays.asList(new String[] { 
					"TSNK",	"TSLD", "PANK", "PALD", "SBNK", "SBLD", "SBSL", "PAHAV", "PADFR",
					"SBHAV", "SBDFR", "SBAL", "GRHAV", "GRDFR", "GRAL", "SPHAV", "SPDFR", "SPAL", "NBSUB",
					"NBRIS", "DL", "PASUB", "PARIS", "SBSUB", "SBRIS", "SBCL", "PASUBMAJ", "PARISMAJ",
					"SBSUBMAJ", "SBRISMAJ", "SBPOL"});	
			if (promotRanks.contains(do2.getColumnB().trim())) {
				return true;
			}			
		}
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("MACPS")) {		 
			List<String> macpsRanks = Arrays.asList(new String[] {"NKGRADE", "HAVGRADE", "NSUBGRADE", "SUBGRADE", "SMAJGRADE", "HONYGRADE"});	
			if (macpsRanks.contains(do2.getColumnB().trim())) {
				return true;
			}			
		}
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("MSTJCO") && do2.getColumnB().trim().equals("NBSUB")) {		 
		 	 	return true;		 		
		}
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("HCOMMN") 
				&& (do2.getColumnB().trim().equals("HONYLT") || do2.getColumnB().trim().equals("HONYCAPT"))) {		 
	 	 	return true;		 		
		}
		if (do2.getColumnA()!=null && do2.getColumnA().trim().equals("MSTHAV") 
				&& (do2.getColumnB().trim().equals("PAHAV") || do2.getColumnB().trim().equals("SBHAV") 
						|| do2.getColumnB().trim().equals("HAVGRADE") || do2.getColumnB().trim().equals("GP-X") || do2.getColumnB().trim().equals("GP-Y"))) {		 
	 	 	return true;		 		
		}		
		return false;
	}
	
	 
	
	public boolean isValidPromotExists(Do2 do2, Employee employee) {
		List<Do2> do2List = null;
		if (do2.getColumnA()!=null && do2.getColumnA().trim().length()>0) {
			 do2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V", do2.getColumnA().trim(), do2.getFromDate());
					 			
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}
			refDo2NoForAcProFix = Utils.getReferenceDo2No(do2List.get(0));
			actualMacpsPromotDo2 = do2List.get(0);
		//	do2.setColumnC(do2List.get(0).getColumnA());	
			if (do2.getColumnB().trim().equals(actualMacpsPromotDo2.getColumnA().trim())) 
				return true;
			else { // following condition added on 22.09.2022 for Nashik email
				if (actualMacpsPromotDo2.getDescription().equals("MSTHAV")
						|| actualMacpsPromotDo2.getDescription().equals("MSTJCO")
						|| actualMacpsPromotDo2.getDescription().equals("HCOMMN")) {
					return true;
				}				
				if (do2.getId()==newDo2.getId()) {
					return false;
				} else {
					do2.setColumnB(actualMacpsPromotDo2.getColumnA());
					 do2Repo.save(do2);
					return true;
				}				
			}				
		} else {
			return false;
		}		
	//	return true;
	}
	
	public boolean isPromotionBeneficial(Do2 do2, Employee employee) {		
				
		if (actualMacpsPromotDo2!=null) {
 			if (Utils.isAcpBeforeCurrentDo2(actualMacpsPromotDo2)) {
 				return false;
 			}
 		}	 		
		
		if (actualMacpsPromotDo2!=null && actualMacpsPromotDo2.getDescription().equals("MSTHAV")) {
		 	if (employee!=null && employee.getAcpnaikDate()!=null && employee.getNaikDate()==null) {
		 		return false;
		 	}
			
			
		 	if (employee!=null && (employee.getAcphavDate()!=null 
		 			|| (employee.getHavDate()!=null && employee.getHavDate().before(actualMacpsPromotDo2.getFromDate())))) {  //Already ACPHAV/SBHAV before MSTHAV
		 		return false;
		 	}
				  
			 if (employee!=null && employee.getAcpnaikDate()!=null && employee.getAcphavDate()!=null) {
					return false;
				}
			}
		 
		Rank previousDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getPreviousDate(do2.getFromDate()), "PAYFIX");
		Rank optionDateRank = Utils.getPborRank(do2.getArmyNo(), do2.getToDate(), "PAYFIX");
		
	 	if (previousDateRank!=null && optionDateRank!=null 
				&& (Integer.parseInt(previousDateRank.getRankCode())<Integer.parseInt(optionDateRank.getRankCode()))) {
	 		if (actualMacpsPromotDo2!=null) {
	 			if (Utils.isAcpBeforeCurrentDo2(actualMacpsPromotDo2)) {
	 				return false;
	 			}
	 		}	 		
		 	return true;
		} 
	 	
		if (do2.getToDate()!=null && do2.getFromDate().equals(do2.getToDate())) {
			return true;
		}
		
		return false;
	}	
	
	public boolean doesOptcpcExists(Do2 do2, Employee employee) {
		List<Do2> do2List = do2Repo.findByFromDateOrToDate(employee.getId(), do2.getFromDate(), do2.getToDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='OPTCPC' "
		 		+ " and d.employee=:emp and d.status='V' and (d.fromDate=:fromDate OR d.fromDate=:toDate) ")
				.setParameter("emp", employee).setParameter("fromDate", do2.getFromDate()).setParameter("toDate", do2.getToDate())
				.getResultList();*/
		return (do2List!=null && do2List.size()>0);
	}
	/*
	public boolean doesIncrExistsOnOptionDate(Do2 do2, Employee employee) {
		List<Do2> do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
		 		+ " and d.employee=:emp and d.status='V' and d.fromDate=:toDate ")
				.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
				.getResultList();
		
		if (do2List!=null && do2List.size()>0) {
			return true;
		} else {
			do2List=null;
			do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
			 		+ " and d.employee=:emp and d.status='V' and d.fromDate<:toDate order by d.fromDate desc ")
					.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
					.getResultList();
			if (do2List!=null && do2List.size()>0) {
				Calendar nextIncrDate = new GregorianCalendar();
				nextIncrDate.setTime(do2List.get(0).getFromDate());
				int year = nextIncrDate.get(Calendar.YEAR);
				year++;
				try {
					nextIncrDate.setTime(DateManager.getDate(year, nextIncrDate.get(Calendar.MONTH)+1, nextIncrDate.get(Calendar.DATE)));
				} catch (Exception e) {
					Utils.handleException(e);
				}
	 			
				if (do2.getToDate().equals(nextIncrDate.getTime())) {
					return true;
				} else {
					return false;
				}				
			} else {
				if (employee!=null && employee.getIncrementDate()!=null) {
					return (employee.getIncrementDate().equals(do2.getToDate()));
				} else {
					return false;
				}
			}		 
		}	 
	}
	*/
public boolean doesIncrExistsOnOptionDate(Do2 do2, Employee employee) {
		
	Calendar nextIncrDate = new GregorianCalendar();

    List<String> descriptions;
    if (employee.getRegularTaFlag().equals("T")) {
        descriptions = Arrays.asList("TAINCR", "STEPUP");
    } else {
        descriptions = Arrays.asList("INCR", "STEPUP");
    }

    List<Do2> do2List = do2Repo.findByDescFromDateLessThanEqualDesc(employee.getId(), "V", descriptions, do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
		 		+ " and d.employee=:emp and d.status='V' and d.fromDate<=:fromDate order by d.fromDate desc ")
				.setParameter("emp", employee).setParameter("fromDate", do2.getFromDate())
				.getResultList();*/
		
    if (do2List==null || (do2List!=null && do2List.size()==0)) {
		if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			return true;
		}
	}
		System.out.println("...........do2 list size from optfix " +do2List.size());
		if (do2List!=null && do2List.size()>0) {
			if(do2List.get(0).getColumnC()!=null && do2List.get(0).getColumnC().equals("POSTPONED")) {
				nextIncrDate.setTime(do2List.get(0).getFromDate());
				if(nextIncrDate.get(Calendar.DATE)!=1) {
					nextIncrDate.set(Calendar.DATE,1);
				}
				
				if(nextIncrDate.get(Calendar.MONTH)!= Calendar.JULY) {
					nextIncrDate.set(Calendar.MONTH, Calendar.JULY);
				}
				
			} else {
				nextIncrDate.setTime(do2List.get(0).getFromDate());
			}
			
			List<Do2> acpfixprofixList = do2Repo.getIncrDateAcpfixProfixDo2List(employee.getId(), do2List.get(0).getFromDate());
			if (acpfixprofixList!=null && acpfixprofixList.size()>0) {
				int year = nextIncrDate.get(Calendar.YEAR);
				int month = nextIncrDate.get(Calendar.MONTH);
				int date = nextIncrDate.get(Calendar.DATE);
				if (month==Calendar.JULY) {
					year++;
					month=Calendar.JANUARY;
				} else {
					month=Calendar.JULY;
				}
				Date dni = null;
				try {
					 dni = DateManager.getDate(year, month+1, date);
				} catch (Exception e) {
					Utils.handleException(e);
				}
				
				//the following 9 lines added for DSC NE IRLA ( individual is havingprofix on 01072025 and macps 01012026 then optfix form macpsgettingrejected with reason that incr not due on 01072026 
				// which is handled by adding below lines  on 15-12-2025
				if(do2List.get(0).getFromDate().before(DateManager.getSeventhCpcDate()) 
					&& do2.getFromDate().equals(DateManager.getSeventhCpcDate())
					&& dni.equals(DateManager.getSeventhCpcDate())) {
					try {
						dni = DateManager.getDate(year, Calendar.JULY+1,date);
					} catch (Exception e) {
						Utils.handleException(e);
					}
				}
				
				
				return (dni!=null && do2.getToDate()!=null && dni.equals(do2.getToDate()));
			}
		} else 	if (employee!=null && employee.getIncrementDate()!=null) {
			return (employee.getIncrementDate().equals(do2.getToDate()));
		} 
		/*else {
			return false;
		}*/

		int year = nextIncrDate.get(Calendar.YEAR);
		year++;
			
		try {
			nextIncrDate.setTime(DateManager.getDate(year, nextIncrDate.get(Calendar.MONTH)+1, nextIncrDate.get(Calendar.DATE)));
		} catch (Exception e) {
			Utils.handleException(e);
		}
			
		System.out.println(".........optfix nextincrdt " +do2.getToDate()+"   nextIncrDate "+nextIncrDate.getTime());
	
		if (employee.getRegularTaFlag().equals("T")) {
			if (do2.getToDate().equals(nextIncrDate.getTime()))  {
				return true;
			} else {
				if (do2.getToDate().after(nextIncrDate.getTime()))  {
					return true;
				}
			}
		} else {
			if (do2.getToDate().equals(nextIncrDate.getTime())) {
				return true;
			} else {
				Long currentPromotMacpsDo2Id=0l;
				List<Do2> currentPromotMacpsDo2List = do2Repo.findByEmployeeIdAndDescriptionAndFromDateAndStatus(do2.getEmployee().getId(), do2.getColumnA(), do2.getFromDate());
				if (currentPromotMacpsDo2List!=null && currentPromotMacpsDo2List.size()>0) {
					currentPromotMacpsDo2Id=currentPromotMacpsDo2List.get(0).getId();
				} 
				List<String> descList=Arrays.asList(new String[] {"PROMOT","MACPS","MSTHAV","HCOMMN","MSTJCO"});
				List<Do2> macpsPromotDo2List = do2Repo.findByDescListAndFromDateBetweenAndNotDo2id(do2.getEmployee().getId(),descList,
						do2List.get(0).getFromDate(), nextIncrDate.getTime(), currentPromotMacpsDo2Id);
						
				/*		Utils.getEntityManager().createQuery("select d from Do2 d where d.status='V' and d.employee=:emp "
						+ " and d.description in ('PROMOT', 'MACPS', 'MSTHAV', 'HCOMMN', 'MSTJCO') and d.fromDate between :adjustedincrdt "
						+ " and :subsequentincrdt order by d.fromDate ").setParameter("emp", do2.getEmployee()).setParameter("adjustedincrdt", do2List.get(0).getFromDate())
						.setParameter("subsequentincrdt", nextIncrDate.getTime()).getResultList();*/
				
				if (macpsPromotDo2List!=null && macpsPromotDo2List.size()>0) {
					for (Do2 d : macpsPromotDo2List) {
						if (d.getDescription().equals("MACPS")) {
							return (Utils.getSevenPcNextIncrementDate(d.getFromDate()).equals(do2.getToDate()));						
						} else {
							if (Utils.isAcpBeforeCurrentDo2(d)) {
								continue;
							} else {
								return (Utils.getSevenPcNextIncrementDate(d.getFromDate()).equals(do2.getToDate()));
							}
						}
					}
				}		
				return false;
			}
		}
		
	 	 return false;
	}


	public boolean isPayDrawnIn7CPC(Do2 do2, Employee employee) {
		Date cutOffDate=null;
		
		try {
			cutOffDate=DateManager.getDate("2015-12-31");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<AuditCage> acList = acRepo.findByFromDateGreaterThanAndFromDateLessThanEqualToOrderByFromDateToDate(employee.getId(),
																							do2.getFromDate(),cutOffDate);
				
			/*	Utils.getEntityManager().createQuery("SELECT a FROM AuditCage a WHERE a.effective=true AND a.cancelled=false "
				+ " AND a.recordStatus='V' AND a.employee=:emp AND a.fromDate>'2015-12-31' AND a.fromDate<=:fromDate "
				+ " ORDER BY a.fromDate, a.toDate, id ").setParameter("emp", employee).setParameter("fromDate", do2.getFromDate()).getResultList();*/
		
		if (acList!=null && acList.size()>0) {
			AuditCage ac = acList.get(acList.size()-1);
			return (ac.getGradePay()!=null && ac.getGradePay()==0 
	//				&& ac.getPayLevel()!=null && Integer.parseInt(ac.getPayLevel())>0);
					&& ac.getPayLevel()!=null && !ac.getPayLevel().equals("0"));
					
		}
		
		return false;
	}
	
	public Do2 getPromotDo2(Do2 do2) {
		List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
		Do2 promotDo2 = null;
		if (refDo2List!=null && refDo2List.size()>0) {
			for (Do2 d : refDo2List) {
				if (d.getDescription().equals(do2.getColumnA()) && d.getFromDate().equals(do2.getFromDate())
						&& d.getEmployee().equals(do2.getEmployee()) && d.getStatus().equals(Do2Manager.VALID)) {
					if (d.getFromDate().equals(do2.getFromDate())) {
						promotDo2 = d;				 
						break;
					}
				}
			}
		}	
		return promotDo2;
	}
	
	public String createProfixDo2(Do2 do2) {
		 
 		String descriptionInserted = "PROFIX";
		
		if ((do2.getColumnA().equals("MACPS"))) {
			descriptionInserted = "ACPFIX";
		}
		if ((do2.getColumnA().equals("MSTHAV"))) {
			descriptionInserted = "HAVFIX";
		}
			
		List<Do2> chkFixDo2 = do2Repo.findByDescriptionFromdateAndStatusNotEqualToCancel(do2.getEmployee().getId(),
																descriptionInserted, do2.getToDate());
				
				
		/*		em.createQuery("SELECT cfd FROM Do2 cfd WHERE "
								+ " cfd.employee=:emp AND cfd.description=:desc AND cfd.fromDate=:frdt AND cfd.status!='C' ")
								.setParameter("emp", do2.getEmployee())
								.setParameter("desc", descriptionInserted)
								.setParameter("frdt", do2.getToDate())
								.getResultList();*/
		
    	if (chkFixDo2!=null && chkFixDo2.size() > 0) {
    		if (chkFixDo2.get(0).getStatus().equals(Do2Manager.INVALID)) {
    			chkFixDo2.get(0).setStatus(Do2Manager.PENDING);
    		}
    		return Do2Manager.FAILURE;
    	}
		
		Do2 profixDo2 = new Do2();
		
		if (do2.getBatchNo()!=null)
			profixDo2.setBatchNo(do2.getBatchNo());
		
		profixDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		profixDo2.setDo2Group(do2.getDo2Group());
		profixDo2.setDo2Type(do2.getDo2Type());
		profixDo2.setDo2No(do2.getDo2No());
	 	profixDo2.setDo2ItemNo(do2.getDo2ItemNo());
		profixDo2.setDo2Date(do2.getDo2Date());
		profixDo2.setDescription(descriptionInserted);
		if (do2.getUnit()!=null)
			profixDo2.setUnit(do2.getUnit());
		profixDo2.setUnitCode(do2.getUnitCode());
		profixDo2.setEmployee(do2.getEmployee());
		profixDo2.setArmyNo(do2.getArmyNo());
		profixDo2.setCheckDigit(do2.getCheckDigit());
		profixDo2.setFromDate(do2.getToDate());		 
		profixDo2.setColumnB(do2.getDescription());
		
		if (do2.getColumnA().equals("MSTHAV")) {
			profixDo2.setColumnA(do2.getColumnA());						
		} else {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {		// in view of Estt No.22 changes are made on 28.08.2017		
				if (do2.getColumnB().trim().equals("NKGRADE")) {
					profixDo2.setColumnA("SLGRADE");
				} else if (do2.getColumnB().trim().equals("HAVGRADE")) {
					profixDo2.setColumnA("ALGRADE");
				} else if (do2.getColumnB().trim().equals("NSUBGRADE")) {
					profixDo2.setColumnA("DLGRADE");
				} else if (do2.getColumnB().trim().equals("SUBGRADE")) {
					profixDo2.setColumnA("CLGRADE");
				} else if (do2.getColumnB().trim().equals("SMAJGRADE")) {
					profixDo2.setColumnA("POLGRADE");
				} 
			} else {
				profixDo2.setColumnA(do2.getColumnB());
			}	
		}
		
		profixDo2.setRefDo2No(refDo2NoForAcProFix);
		profixDo2.setStatus(Do2Manager.PENDING);
		profixDo2.setAuditCage(true);
		profixDo2.setQe(Utils.getCurrentQuarter());
	//	profixDo2.setAcceptedQe(Utils.getCurrentQuarter());
		profixDo2.setInputQe(Utils.getCurrentQuarter());
		do2Repo.save(profixDo2);
	 	 
 		return Do2Manager.SUCCESS;
	}
}
