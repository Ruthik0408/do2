package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.text.Highlighter;

import org.springframework.web.bind.annotation.RestController;

 

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class Incr extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	Employee employee;
	List<PayScale> payList;
	boolean incrValidity = true, higherRankAcpAvailable = false;
	StringBuffer buffer = new StringBuffer();
	Date sixcpcDate=null, sevencpcDate=null, higherRankAcpDate = null, firstJanuaryOfTheIncrYear=null, firstIncrDate=null;
	final static public long MILLIS_IN_DAY = 1000 * 3600 * 24;
	static String IS_A_RECRUIT = "AU:PBOR is a Recruit.\n";
	static String FS_DUE = "AU:Increment Date Beyond FS Due Date.\n";
	static String QS_LESS_FROM_REMUST = "AU:Not completed required minimum Qualifying Service From REMUST.\n";
	static String QS_LESS_FROM_PROMOT = "AU:Not completed required minimum Qualifying Service From PROMOT.\n";
	static String QS_LESS_FROM_STEPUP = "AU:Not completed required minimum Qualifying Service From STEPUP.\n";
	static String DUPLICATE_INCR_DO2 = "DU:More than one Incr Do2/SIXCPC option exercised/From date incorrect. \n";
	static String HONYCOMMN_THIS_YEAR = "AU:Honorary Commission - superannuation before Date of Incr \n";
	static String NQS_MORE_THAN_LIMIT = "AU:NonQualifyingService exceeded 180 days for the year. \n";
	static String NO_AUDIT_CAGE_EXISTS = "AU:Audit Cages not available for the full year to calculate NQS. \n";
	static String INCR_DO2_WITHIN_ONE_YEAR = "DU:Incr/Promot/Macps Do2 found adjusted within previous 12 months \n";
	static String PROMOT_MACPS_DO2 = "AU:As on ADHOC INCR date PROMOT/MACPS Exists. ADHOC INCR not admissible \n";
	static String INCR_NOT_DUE_ON_THIS_DATE = "AU: Increment Not Due On 7CPC Date In Normal Course. \n";
	static String PRE2006_GROUP_CATEGORY_UNAVAILABLE = "VL: Present GroupCategory and Previous GroupCategory are same. \n";
	static String INCR_ON_HOLD_STOPIN_PUNISHMENT = "VL: Valid Punishment DOII - Stop Increment Exists With This FromDate. \n";
	
	@Override
	public boolean isValid(Do2 do2) {
		buffer.setLength(0);
		firstJanuaryOfTheIncrYear=getFirstJanuary(do2.getFromDate());
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee == null){ 
			do2.setReason("Current Employee Record not found");
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
			return false;	
		}
		//rejects the do2 if the pbor is ta and increment is generated already
		if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("T")){
		    do2.setReason("Regular INCR Not Admissible for TA Personnel");
		    do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
		   return false;						
		}
		
		if (!sixcpcDo2Exists(do2)) {
			sixcpcDate = DateManager.getSixthCpcDate();
		}
		
		if (!optcpcDo2For7cpcExists(do2)) {
			sevencpcDate = DateManager.getSeventhCpcDate();
		}
		
		if (Utils.getPao().getPaoCode().equals("94")) {
			;
		} else if (employee.getGroupCategory() !=null && employee.getGroupCategory().getSoldierCategory().equals("RT")) {
			; 
		} else if (employee.getSportHavildarFlag() != null && employee.getSportHavildarFlag() == true && !employee.getEnrolmentDate().equals(employee.getSportHavildarDate())) {
			;
		} else if (employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
				if ((employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true)) {			 
					if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() != null) {
						firstIncrDate = grhavIncrCal(employee, do2);
					} else {
						;
					}			 
				} else if (employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(DateManager.getSeventhCpcDate())) {
					Date enrollDate = employee.getEnrolmentDate();
					Calendar cal = new GregorianCalendar();
					cal.setTime(enrollDate);
					if (cal.get(Calendar.YEAR)<2015) {
						try {
							if (cal.get(Calendar.MONTH) == Calendar.JANUARY	&& cal.get(Calendar.DATE) == 1) {
								firstIncrDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
							} else {
								firstIncrDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
							}
						} catch (Exception e) {
							Utils.handleException(e);
						} 
					} else {
						firstIncrDate = Utils.getIncrementDateForAttestMusterIn7cpc(employee.getEnrolmentDate());
					}
				} else if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() != null) {
					firstIncrDate = musterIncrCal(employee, do2);
				} else if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() != null) {
					if (!employee.getAttestationDate().before(employee.getInitialMusteringDate())) {				
						firstIncrDate = musterIncrCal(employee, do2);
					} else {
						firstIncrDate = musterAttestIncrCal(employee, do2);
					}
				} 
		} else if (!employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())
				&& (employee.getAttestationDate()!=null || employee.getInitialMusteringDate()!=null)) {
				firstIncrDate = Utils.getIncrementDateForAttestMusterIn7cpc(employee.getEnrolmentDate());
		}  
	
	//	System.out.println(".............. first increment date  " +firstIncrDate );
	 		
		if (firstIncrDate != null && (do2.getColumnC() == null || 
				(do2.getColumnC()!=null && !do2.getColumnC().equals("ADHOC"))) 
				&& do2.getFromDate().before(firstIncrDate)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason("Date of Increment before date of first Increment");
			incrValidity=false;
			return false;
		}
		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) && !checkIncrementDateValidityIn7cpc(do2)) { 
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("Incorrect Date of Increment - not entitled");
			incrValidity=false;
			return false;
		}
				
		int nonQualifyingServiceDays = 0;
		
		if (doesIncrDo2ExistOnSameDate(do2)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(DUPLICATE_INCR_DO2);
			incrValidity=false;
			return false;
		}
		
		if (doesDuplicateIncrDo2Exist(do2)) {
			do2.setStatus(Do2Manager.INVALID);  // Changed From Pending Status To Invalid Status on 19.08.2020.
			do2.setReason(DUPLICATE_INCR_DO2);
			incrValidity=false;
			return false;
		}
				
		if (do2.getColumnC()!=null && do2.getColumnC().equals("ADHOC")) {
			;  
			//Below validation commented on 18.11.2025 in view of Hqrs. Office clarification No.Army/BR/Pay/ORs/3510/7thCPC/E-1210 dated 10.11.2025
		/*	if (doesPromotMacpsDo2ExistOnSameDate(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(PROMOT_MACPS_DO2);
				incrValidity=false;
				return false; 				
			}			*/
		} else if (doesIncrDo2ExistWithinOneYear(do2)) {
			do2.setStatus(Do2Manager.INVALID); // Changed From Pending Status To Invalid Status on 19.08.2020.
			do2.setReason(INCR_DO2_WITHIN_ONE_YEAR);
			incrValidity=false;
			return false;
		}
		
		try {
			if (!do2.getFromDate().before(DateManager.getSixthCpcDate())) {
                if (do2.getColumnC() == null || (do2.getColumnC() != null && !do2.getColumnC().equals("POSTPONED"))) {
                    if ((nonQualifyingServiceDays = getNqsForIncr(employee, do2.getFromDate())) != 0) {
                        if (nonQualifyingServiceDays > 180) {
                            // Check QS — if soldier served more than 180 days, grant increment
                            int qualYearTotalDays = getQualYearTotalDays(do2.getFromDate());
                            int qualifyingServiceDays = qualYearTotalDays - nonQualifyingServiceDays;
                            if (qualifyingServiceDays < 180) {
                                do2.setStatus(Do2Manager.INVALID);
                                do2.setReason(NQS_MORE_THAN_LIMIT);
                                incrValidity = false;
                                return false;
                            }
                            // QS > 180 days → soldier served enough → allow increment
                        }
                        if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22) &&
                                do2.getFromDate().equals(DateManager.getDate("2016-07-01"))) {
                            ;
                        } else {
                            if (nonQualifyingServiceDays < 0) {
                                do2.setStatus(Do2Manager.INVALID);
                                do2.setReason(NO_AUDIT_CAGE_EXISTS);
                                incrValidity = false;
                                return false;
                            }
                        }
                    }
                }
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		if (do2.getFromDate().after(DateManager.getSixthCpcDate())){
			if (stepup(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(QS_LESS_FROM_STEPUP);
				incrValidity=false;
				return false;	
			}
		}
		
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			incrValidity = false;
			buffer.append(IS_A_RECRUIT);
		}
		
		if (employee.getHonyLieuDate() == null && employee.getHonyCaptainDate() == null ) {
			;
		} else {
			  if (superannuationBeforeIncrDate(do2, employee)) {
				  incrValidity = false;
				  buffer.append(HONYCOMMN_THIS_YEAR);
			  }
		}
	/*	
		if (employee.getFsDueDate() != null &&
		   (employee.getFsDueDate().equals(do2.getFromDate()) || 
			employee.getFsDueDate().before(do2.getFromDate()))) {
			do2.setStatus(Do2Manager.PENDING);
			incrValidity = false;
			buffer.append(FS_DUE);
		}
	 */ // This condition commented and rewritten as below - CMP FS case 25.10.2021
		
		if (employee.getFsDueDate() != null &&
				do2.getFromDate().after(employee.getFsDueDate())) { 
			do2.setStatus(Do2Manager.PENDING);
			incrValidity = false;
			buffer.append(FS_DUE);
		}
		
		// Added on 08.11.2021.
		if (do2.getFromDate().equals(DateManager.getSeventhCpcDate())) {
			// Increment on sevencpc date possible only for enrolled before 01.01.2016 and attested/mustered after 31.12.2015.		
		//	if (employee.getEnrolmentDate()!=null && !employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {           // condition commented on 10.06.2026 11GRRC IRLA
				do2.setStatus(Do2Manager.INVALID);
				incrValidity = false;
				buffer.append(INCR_NOT_DUE_ON_THIS_DATE);				
		//	}
			// Increment on sevencpc date and cannot allow option for migration to 7cpc due to priority issues
			if (optcpcDo2For7cpcExists(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				incrValidity = false;
				buffer.append(INCR_NOT_DUE_ON_THIS_DATE);
			}		
		}		
		
		if (do2.getColumnC()!=null && do2.getColumnC().trim().toLowerCase().equals("adhoc")) {
			if (employee.getPreviousGroupCategory()!=null && employee.getGroupCategory()!=null
					&& employee.getGroupCategory().equals(employee.getPreviousGroupCategory())) {
				if (!employee.getGroupCategory().getFromDate().before(DateManager.getSixthCpcDate())) {
					do2.setStatus(Do2Manager.INVALID);
					incrValidity = false;
					buffer.append(PRE2006_GROUP_CATEGORY_UNAVAILABLE);	
				}
			}
		}
		
		if (validStopinDo2Exists(do2)) {
			incrValidity = false;
			buffer.append(INCR_ON_HOLD_STOPIN_PUNISHMENT);
		}
		
		if (incrValidity) {
			saveValidIncrDo2(do2);
		} else {
			saveErrorIncrDo2(do2, buffer);
		}
		return incrValidity;
	}
 
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		firstJanuaryOfTheIncrYear=getFirstJanuary(newAuditCage.getDo2().getFromDate());
		Date promotDate=null;
		
		if (!sixcpcDo2Exists(newAuditCage.getDo2())) {
			sixcpcDate = DateManager.getSixthCpcDate();
		}
		
		if (!optcpcDo2For7cpcExists(newAuditCage.getDo2())) {
			sevencpcDate = DateManager.getSeventhCpcDate();
		}
		
		if (newAuditCage.getFromDate().after(sixcpcDate) && newAuditCage.getFromDate().before(sevencpcDate)) {
			List<String> descList=Arrays.asList(new String[] {"PAYFIX","SIXCPC"});
			List<Do2> payfixDo2List = do2Repo.findByEmployeeDescListOrderFromDateDesc(newAuditCage.getEmployee().getId(), descList);
					
			/*		Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and "
					+ " d.description in ('PAYFIX', 'SIXCPC') and d.status='V' order by d.fromDate ")
					.setParameter("emp", newAuditCage.getEmployee()).getResultList();*/
			
			if (payfixDo2List!=null && payfixDo2List.size()>0) {
				for (Do2 d : payfixDo2List) {
					if (d.getFromDate().after(sixcpcDate)) {
						sixcpcDate=d.getFromDate();
					}
				}
			} 
		}
		
		if (newAuditCage.getFromDate().after(sevencpcDate)) {
			
			List<String> descList=Arrays.asList(new String[] {"FIX7PC","OPTCPC"});
			List<Do2> fix7pcDo2List = do2Repo.findByEmployeeDescListOrderFromDateDesc(newAuditCage.getEmployee().getId(), descList);
			
		/*	List<Do2> fix7pcDo2List = Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and "
					+ " d.description in ('FIX7PC', 'OPTCPC') and d.status='V' order by d.fromDate ")
					.setParameter("emp", newAuditCage.getEmployee()).getResultList();*/
			
			if (fix7pcDo2List!=null && fix7pcDo2List.size()>0) {
				for (Do2 d : fix7pcDo2List) {
					if (d.getFromDate().after(sevencpcDate)) {
						sevencpcDate=d.getFromDate();
					}
				}
			}
		}
				
	//	System.out.println("************************   sixcpc date from incr  " +sixcpcDate);
		employee = Utils.getCurrentEmployee(baseAuditCage.getDo2().getArmyNo());
		
		if (!newAuditCage.getFromDate().after(sixcpcDate)) {

			if (newAuditCage.getBasicPay() == 2440 || newAuditCage.getBasicPay() == 2850){
				return newAuditCage;
			}
			newAuditCage.setBasicPay(get5cpcPay(newAuditCage.getDo2(), baseAuditCage));
			// Amended
			if (!newAuditCage.getFromDate().before(DateManager.getDPDate()))
				newAuditCage.setDp(getDp(newAuditCage.getDo2(), baseAuditCage));
			
			return newAuditCage;
		
		} else if (newAuditCage.getFromDate().before(sevencpcDate)) {			 
					if (employee.getSubstantiveRank() != null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){				
						return newAuditCage;
					} else if ((newAuditCage.getBasicPay()== 11400 ||  newAuditCage.getBasicPay()== 8550 || newAuditCage.getBasicPay()== 5700) && 
							   (newAuditCage.getGradePay() == null || (newAuditCage.getGradePay() != null && newAuditCage.getGradePay() == 0))) {
						return newAuditCage;
					}  else if  (newAuditCage.getGradePay() == null || (newAuditCage.getGradePay() != null && newAuditCage.getGradePay() == 0)) {
						//Added on 30082012 to draw incr if sixcpc exists
						//where the gradepay will be zero upto sixcpc
						if (sixcpcDo2Exists(newAuditCage.getDo2())){ 
							if (sixcpcDate!=null && newAuditCage.getFromDate().before(sixcpcDate)){
							   newAuditCage.setBasicPay(get5cpcPay(newAuditCage.getDo2(), baseAuditCage));
							}
						}
						return newAuditCage;
					}
	   
			if (newAuditCage.getBasicPay() == 9300 && 
				newAuditCage.getFromDate().after(DateManager.getSixthCpcDate())) {
				promotDate = Utils.getFromDateForProfixPromot(newAuditCage.getDo2());
				if (promotDate != null && promotDate.after(firstJanuaryOfTheIncrYear)) {
					AuditCage auditCageEarlierToPromotDate = Utils.getEarlierAuditCageToPromot(newAuditCage, promotDate);
					if ((auditCageEarlierToPromotDate.getXGroupPay() != null &&
						 auditCageEarlierToPromotDate.getXGroupPay() != 0 &&
						 auditCageEarlierToPromotDate.getBasicPay() < 8580)) {								
						return newAuditCage;
					}
					if (auditCageEarlierToPromotDate.getBasicPay() < 8600 &&
					   (auditCageEarlierToPromotDate.getXGroupPay() == 0 ||
					    auditCageEarlierToPromotDate.getXGroupPay() == null)) {								
						 return newAuditCage;
					}
				}
			}
			
			if (newAuditCage.getBasicPay() == 15600 && 
				newAuditCage.getFromDate().after(DateManager.getSixthCpcDate())) {
				List<Do2> hcommnList = do2Repo.findByDescriptionStatusOrderByFromDateDesc(employee.getId(), "HCOMMN","V");
						
						
			/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE  " +
					" d.employee=:emp  AND d.description='HCOMMN' AND d.status='V' order by d.fromDate desc")
					.setParameter("emp", employee).getResultList();*/
		//		if (hcommnList.size() == 1){   // Commented on 05.02.2024.
				if (hcommnList.size()>0) {
					if (hcommnList.get(0).getFromDate().after(firstJanuaryOfTheIncrYear)) { 
						if (hcommnList.get(0).getToDate() != null && hcommnList.get(0).getToDate().after(newAuditCage.getDo2().getFromDate())) {
							return newAuditCage;	
						}
					}
				}
			}
			
			promotDate = getFromDateForPromot(newAuditCage.getDo2());
			
			System.out.println (  " Promot date xxx incr  " +promotDate );
			System.out.println ( " first january of the year " +firstJanuaryOfTheIncrYear);
			
			if (promotDate == null) {
				promotDate = newAuditCage.getFromDate();
			} else {
				if (!promotDate.after(firstJanuaryOfTheIncrYear)){
					promotDate= newAuditCage.getFromDate();	
				}
			}
		 	newAuditCage.setBasicPay(get6cpcPay(newAuditCage.getDo2(), baseAuditCage, promotDate));
		//	newAuditCage.setOldBasicPay(get5cpcPay(newAuditCage.getDo2(),baseAuditCage));	Commented on 08.11.2021 as 6cpc pay gets written over 5cpc pay in auditcage.
			
			// Transport Allowance
			if (newAuditCage.getTransport() != null && newAuditCage.getTransport() !=0) {
				newAuditCage.setTransport(AuditCageHelper.getTransportRate(newAuditCage,baseAuditCage));
			}
			
		} else {
			
			if (employee.getSubstantiveRank() != null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {				
				return newAuditCage;
			} 
		
			if ((newAuditCage.getBasicPay()==14600 || newAuditCage.getBasicPay()== 11400 ||  newAuditCage.getBasicPay()== 8550 || newAuditCage.getBasicPay()== 5700) && 
					   (newAuditCage.getGradePay() == null || (newAuditCage.getGradePay() != null && newAuditCage.getGradePay() == 0))) {   
		 		return newAuditCage;
			}			
		 
			if (newAuditCage.getDp()!=null && newAuditCage.getDp()>0) {
				return newAuditCage;
			}				
				
			promotDate = getFromDateForPromotFor7CPC(newAuditCage.getDo2());
			
			if (promotDate == null) {
				promotDate = newAuditCage.getFromDate();
			} else {
				if (!promotDate.after(firstJanuaryOfTheIncrYear)){
					promotDate= newAuditCage.getFromDate();	
				}
			}
	 		if (newAuditCage.getPayLevel()!=null && newAuditCage.getPayLevel().equals("0")) {
				if (optcpcDo2For7cpcExists(newAuditCage.getDo2())) {
					if (sevencpcDate!=null) {					 
						if (!sevencpcDate.before(newAuditCage.getFromDate())) {						 
						    newAuditCage.setBasicPay(get6cpcPay(newAuditCage.getDo2(), baseAuditCage, promotDate));
						    newAuditCage.setOldBasicPay(newAuditCage.getOldBasicPay());	
						    acRepo.save(newAuditCage);
					//	    Utils.getEntityManager().merge(newAuditCage);
					//		Utils.getEntityManager().flush();
				 			return newAuditCage;
						} else {
							double noOfDaysSinceLastHike = 0;
							noOfDaysSinceLastHike = DateManager.getNoOfDays(sevencpcDate, newAuditCage.getFromDate());
							if (noOfDaysSinceLastHike<180) {
								newAuditCage.setOldBasicPay(get6cpcPay(newAuditCage.getDo2(), baseAuditCage, promotDate));								 
							}
						}
					  
					}
				} else if (sevencpcDate!=null) {					 
						double noOfDaysSinceLastHike = 0;
						noOfDaysSinceLastHike = DateManager.getNoOfDays(sevencpcDate, newAuditCage.getFromDate());
						if (noOfDaysSinceLastHike<180) {
							newAuditCage.setOldBasicPay(get6cpcPay(newAuditCage.getDo2(), baseAuditCage, promotDate));						 
						}													
					}
					newAuditCage.setBasicPay(get7cpcPay(newAuditCage.getDo2(), baseAuditCage, promotDate));	
					acRepo.save(newAuditCage);
			//	Utils.getEntityManager().merge(newAuditCage);
			//	Utils.getEntityManager().flush();
	 			return newAuditCage;
			} else {
	 			newAuditCage.setBasicPay(get7cpcPay(newAuditCage.getDo2(), baseAuditCage, promotDate));
				newAuditCage.setOldBasicPay(get6pcOldBasicPay(newAuditCage.getDo2(),baseAuditCage, promotDate));
				acRepo.save(newAuditCage);
			//	Utils.getEntityManager().merge(newAuditCage);
			//	Utils.getEntityManager().flush();
			}
	 		acRepo.save(newAuditCage);
	//	Utils.getEntityManager().merge(newAuditCage);
	//	Utils.getEntityManager().flush();
		}		
 		return newAuditCage;
	}
 
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date dni = Utils.getNextIncrementDate(do2.getFromDate());
 		
		if (employee.getIncrementDate()!=null && !employee.getIncrementDate().after(do2.getFromDate()) 
				&& !employee.getIncrementDate().equals(dni)) {
			employee.setIncrementDate(dni);
			empRepo.save(employee);
		}		
	 
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
 		return Do2Manager.SUCCESS;
	}

	// ******************************************************************************
	public int get5cpcPay(Do2 do2, AuditCage auditCage) {
		int newBasic = auditCage.getBasicPay();
		String soldierGroup=null;
	 
		try {			
			Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			GroupCategory groupCategory = Utils.getGroupCategory(do2,"PREV");
	//		System.out.println ( " rank " +rank.getRankCode()   +"  xx group  " +groupCategory.getSoldierGroup()  +" categroy " +groupCategory.getSoldierCategory()  +" id ");
			if (groupCategory==null) { // introduced on 05.01.2026 - adhoc incr - groupcategory and previousgroupcategory are same - id 294 (eme) 
				return newBasic;
			}
			if (rank.getId()>=9 && groupCategory.getSoldierCategory().equals("CATER")) {
				soldierGroup="Y";
			} else {
				soldierGroup=groupCategory.getSoldierGroup();
			}
			
			String catg = null;
			if (groupCategory!=null) {
				catg = groupCategory.getSoldierCategory();	
			} else {
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)
						&& !do2.getFromDate().after(DateManager.getSixthCpcDate())) {
					List<EmployeeServiceDetail> esdGroupCategoryList = esdRepo.findByStatusAndFromDateLessThanEqualOrderByFromDateDesc(employee.getId(),
																															"R", do2.getFromDate());
							
							
					/*		Utils.getEntityManager()
							.createQuery("SELECT esd FROM EmployeeServiceDetail esd WHERE esd.employee=:emp AND "
											+ " esd.status='R' and esd.fromDate <= :frdt ORDER BY esd.fromDate DESC")
							.setParameter("emp", employee).setParameter("frdt", do2.getFromDate()).getResultList();*/
					if (esdGroupCategoryList!=null && esdGroupCategoryList.size()>0) {
						if (esdGroupCategoryList.get(0).getDo2().getColumnC().endsWith("Y")) {
							catg = "CLKGD";							
						} else {
							catg = "DSCGD";
						}
						groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
					//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
					} else {
						if (newBasic<3250) {
							catg = "DSCGD";
							groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
						//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
						} else {
							for (int i=3250; i<=4300; i+=70) {
								if (newBasic==i) {
									catg = "CLKGD";
									groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
								//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
									break;
								}
							}
							if (catg==null) {
								for (int i=3050; i<=3875; i+=55) {
									if (newBasic==i) {
										catg = "DSCGD";
										groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
									//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
										break;
									}
								}	
							}							
						}
					}
					
				}
			}
			 
		//	System.out.println ( " categroy yy "  +groupCategory.getSoldierCategory()   +" category kkkk " +catg);
			 
			List<GroupCategory> grpCatList = gcRepo.findByFromDateAndRedesgAbbrNotNull(DateManager.getDate("1997-10-10"));
			
			
								
		//	Utils.getEntityManager().createQuery(
		//	"SELECT gc FROM GroupCategory gc WHERE gc.redesignatedAbbr is not null and from_date='1997-10-10'").getResultList();
			if (grpCatList.size() > 0 ) {
				for (GroupCategory gc1 : grpCatList) {
					if (gc1.getRedesignatedAbbr().equals(catg)){
						catg=gc1.getSoldierCategory();
						groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
					//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
					}
				}				
			}
			
			
			List<Do2> acpfixDo2List = do2Repo.findByEmployeeDescriptionAndFromDate(employee.getId(),"ACPFIX", do2.getFromDate());
					
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d " +
					" WHERE d.employee=:emp AND d.description='ACPFIX' and d.fromDate = :fromDate " +
					" and d.status='V'")
					.setParameter("emp", employee)
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (acpfixDo2List.size()>0) {
				List<Do2> acpDo2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(), "V", "ACP", do2.getFromDate());
						
				/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d " +
						" WHERE d.employee=:emp AND d.description='ACP' and d.fromDate < :fromDate " +
						" and d.status='V' ORDER BY d.fromDate DESC ")
						.setParameter("emp", employee)
						.setParameter("fromDate", do2.getFromDate()).getResultList();*/
				if (acpDo2List.size()>0)					
					rank = Utils.getPborRank(employee.getArmyNo(), acpDo2List.get(0).getFromDate(),"PAYFIX");				
				}
			if (rank != null && groupCategory != null) {
		//		System.out.println ( " RANK " +rank.getRankCode()   +" Group " +groupCategory.getSoldierGroup()  + "category "+groupCategory.getSoldierCategory());
				
				
					payList = psRepo.findByRankGroupFromDateToDateStartScaleGreaterThanZero(rank.getId(), soldierGroup,
							DateManager.getDate("1997-10-10"), DateManager.getDate("2005-12-31"));
				
				
						
						
				/*		Utils.getEntityManager().createQuery(
								"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
										+ " AND ps.startScale > 0 "
										+ " AND ps.soldierGroup.soldierGroup=:grp "
										+ " AND ps.fromDate=:fromDate "
										+ " AND ps.toDate=:toDate")
						.setParameter("rank", rank)
						.setParameter("grp",groupCategory.getSoldierGroup())
						.setParameter("fromDate", DateManager.getDate("1997-10-10"))
						.setParameter("toDate", DateManager.getDate("2005-12-31"))
						.getResultList();*/
			}
		} catch (Exception e) {
			Utils.handleException(e);

		}
		
	//	System.out.println ( " PAY SCALE LIST " +payList.size());
		
	//	System.out.println ( " PAY SCALE  xxxx " +payList.get(0).getId());
		
		if (payList!=null && payList.size() > 0) {			//	gradepay condition added on 11.03.2021
			if (auditCage.getGradePay()>0) {
				if ((auditCage.getOldBasicPay() + payList.get(0).getIncr0()) <= payList.get(0).getScaleEnd0())
					newBasic = (auditCage.getOldBasicPay() + payList.get(0).getIncr0());
				else
					newBasic = payList.get(0).getScaleEnd0();
			} else {
				if ((auditCage.getBasicPay() + payList.get(0).getIncr0()) <= payList.get(0).getScaleEnd0())
					newBasic = (auditCage.getBasicPay() + payList.get(0).getIncr0());
				else
					newBasic = payList.get(0).getScaleEnd0();		
			}
		
		}
		return newBasic;
	}
		
	public int getDp(Do2 do2, AuditCage auditCage) {
		int newDp = 0;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());		
		if(employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
		   return newDp; 
		} else if (!(do2.getFromDate().before(DateManager.getDPDate()))
				&& (do2.getFromDate().before(sixcpcDate))) {
			newDp = ((int) Math.round(auditCage.getBasicPay() * 0.50));
		}
		return newDp;
	}
 
	public int get6cpcPay(Do2 do2, AuditCage auditCage, Date promotDate) {
	 	int newBasic = auditCage.getBasicPay();
		int gradepay=auditCage.getGradePay();
		int xpay=auditCage.getXGroupPay();	 	
		System.out.println ( " do2 " +do2.getFromDate()   +" audit_cage " +auditCage.getFromDate()   +" Promot date " +promotDate);
		AuditCage promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(promotDate, do2);		
	 System.out.println ( " promot previous auditCage " +promotPreviousAuditCage);
		if (promotPreviousAuditCage != null) {
			System.out.println ( " IAM HERE  111" +do2.getFromDate()   + promotDate);
			newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay() + (promotPreviousAuditCage.getGradePay() == null ? 0:
					  promotPreviousAuditCage.getGradePay()) +
					  (promotPreviousAuditCage.getXGroupPay() == null ? 0:
					  promotPreviousAuditCage.getXGroupPay()),
					  promotPreviousAuditCage.getFromDate()));
			
			if(promotPreviousAuditCage.getXGroupPay()==0 && auditCage.getXGroupPay()!=0) {
				newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
						+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	promotPreviousAuditCage.getFromDate()));					
				
			} else if (promotPreviousAuditCage.getXGroupPay()!=0 && auditCage.getXGroupPay()!=0) {
				
				Date remustDate = Utils.getFromDateForRemust(do2);
	    		if (remustDate!=null && !remustDate.before(DateManager.getSixthCpcDate())) {
	        		if (DateManager.getNoOfMonths(remustDate, do2.getFromDate()) > 6) {					
	        			AuditCage remustAuditCage = AuditCageHelper.getRemustAuditCage(remustDate, do2);
	        					if (remustAuditCage!=null && remustAuditCage.getXGroupPay()!=0) {	
	        						if(remustAuditCage.getBasicPay()>auditCage.getBasicPay()  ) {
	        							newBasic = (remustAuditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(remustAuditCage.getBasicPay()
		        								+ (remustAuditCage.getGradePay() == null ? 0: remustAuditCage.getGradePay())
		        								+ (remustAuditCage.getXGroupPay() == null ? 0: remustAuditCage.getXGroupPay()),	remustAuditCage.getFromDate()));
	        						} else {
	        							newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
		        								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
		        								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	promotPreviousAuditCage.getFromDate()));
	        						   }
	        						
	        						
	        						
	        						
	        						/*newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
	        								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
	        								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	promotPreviousAuditCage.getFromDate()));*/
	        					} else {
	        						System.out.println ( " XXXXXX AAAA " +do2.getFromDate());
	        						
	        						AuditCage firstAuditCage = AuditCageHelper.getFirstAuditCage(do2.getFromDate(), do2);
	        						Date firstAuditCageDate = firstAuditCage.getFromDate();
	        						System.out.println ( " first auditcage date " +firstAuditCageDate);
	        						if (DateManager.getNoOfMonths(firstAuditCageDate, do2.getFromDate()) >= 6) {		
	        							if (firstAuditCage.getXGroupPay()!=0) {
	        							newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
	        									+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
	        									+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	promotPreviousAuditCage.getFromDate()));

	        						} else { 
	        							newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
		        								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
		        								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	promotPreviousAuditCage.getFromDate()));	
	        							//xpay added on 23-07-2025
	        						}
	        							
	        						
	        									
	        				} else {
	        					newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
        								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
        								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()) ,promotPreviousAuditCage.getFromDate()));	
    						}
	        				}           			
	           		} else {
	           			System.out.println ( " Im ahere   in remust pay ");
	           			newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
								 ,promotPreviousAuditCage.getFromDate()));		
	           		}
	    	  }
			} else {
				System.out.println ( " Iam here in 3333 ");					
				if (auditCage.getBasicPay()==9300 && auditCage.getGradePay()==4200 && promotPreviousAuditCage.getBasicPay()<9300 && promotPreviousAuditCage.getGradePay()<4200) {
					newBasic = (promotPreviousAuditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getBasicPay()
							+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
							 ,promotPreviousAuditCage.getFromDate()));	
					System.out.println(".................-------------------............. special new condition.................");
				} else {
					newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
							+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
							 ,promotPreviousAuditCage.getFromDate()));
				}
			}
		}  else {
				System.out.println ( " Im hee 2222 ");
				AuditCage firstAuditCage = AuditCageHelper.getFirstAuditCage(do2.getFromDate(), do2);
				
				Date firstAuditCageDate = firstAuditCage.getFromDate();
				System.out.println ( " first auditcage date " +firstAuditCageDate);
				if (firstAuditCage.getDo2().getDescription().equals("REMUST")) {
					if (DateManager.getNoOfMonths(firstAuditCageDate, do2.getFromDate()) >= 6) {		
						if (firstAuditCage.getXGroupPay()!=0) {
						newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
								+ (auditCage.getGradePay() == null ? 0: auditCage.getGradePay())
								+ (auditCage.getXGroupPay() == null ? 0: auditCage.getXGroupPay()),	auditCage.getFromDate()));

					} else { 
						newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
								+ (auditCage.getGradePay() == null ? 0: auditCage.getGradePay())
								+ (auditCage.getXGroupPay() == null ? 0: auditCage.getXGroupPay()),	auditCage.getFromDate()));	
						//xpay added on 23-07-2025
					}
				} else {
					if (firstAuditCage.getXGroupPay()!=0  && auditCage.getXGroupPay()!=0) {
						newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()
								+ (auditCage.getGradePay() == null ? 0: auditCage.getGradePay()),
									auditCage.getFromDate()));

					}
				}
			} else {
				 Rank rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"ALLCE");
				   if (rank.getRankCode().equals("30")
						   || rank.getRankCode().equals("29")){
					     gradepay= 2000;
				   } else if(rank.getRankCode().equals("40")
						   || rank.getRankCode().equals("39")){
					   gradepay=2400;
				   } else if (rank.getRankCode().equals("50")
						   || rank.getRankCode().equals("49")){
					   gradepay=2800;
				   } else if (rank.getRankCode().equals("60")
						   || rank.getRankCode().equals("59")){
					   gradepay=4200;
				   } else if (rank.getRankCode().equals("70")
						   || rank.getRankCode().equals("69")){
					   gradepay=4600;
				   }
		 		   GroupCategory groupCategory= Utils.getGroupCategory(do2,promotDate);
				   if (groupCategory !=null) {
					   if (groupCategory.getSoldierGroup().equals("X")){
						   xpay=1400;
					   }
				   }			   
				   newBasic = (auditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getBasicPay()+(gradepay)+(xpay), do2.getFromDate()));
	 		}
			}
				
				
				
				
			// This condition should not arise -- 			  
					  
		
				return newBasic;
		}
						
	 
	public void saveValidIncrDo2(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
		do2.setReason("");
		do2Repo.save(do2);
	}

	public void saveErrorIncrDo2(Do2 do2, StringBuffer buffer) {
		if (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING)) {
			do2.setStatus(Do2Manager.INVALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	} 
	
	public boolean doesIncrDo2ExistOnSameDate(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> duplicateDo2List = do2Repo.findByDescriptionStatusFromDateEqualToAndDo2IdNotEqualTo(emp.getId(),
																						"INCR", "V", do2.getFromDate(), do2.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id != :id AND " +
							" d.employee=:emp AND d.description='INCR' AND d.status='V' AND d.fromDate = :fromDate")
							.setParameter("emp", emp)
							.setParameter("id", do2.getId())
							.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		
		if (duplicateDo2List == null || duplicateDo2List.size() == 0){
			return false;
		}
		return true;
	}
	
	/*public boolean doesPromotMacpsDo2ExistOnSameDate(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> duplicateDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id != :id AND " +
							" d.employee=:emp AND d.description in ('INCR', 'PROMOT', 'MACPS', 'HCOMMN', 'MSTHAV', 'MSTJCO', 'LATENR') " + 
							" AND d.status='V' AND d.fromDate = :fromDate")
							.setParameter("emp", emp)
							.setParameter("id", do2.getId())
							.setParameter("fromDate", do2.getFromDate()).getResultList();
		
		if (duplicateDo2List == null || duplicateDo2List.size() == 0) {
			return false;
		}
		
		return true;
	}*/
	//Updated on 22042021
	public boolean doesPromotMacpsDo2ExistOnSameDate(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		List<String> descList=Arrays.asList(new String[] {"INCR","PROMOT","MACPS","HCOMMN","MSTHAV","MSTJCO","LATENR"});
		List<Do2> duplicateDo2List = do2Repo.findByDescListAndFromDate(emp.getId(), do2.getId(),descList,do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id != :id AND " +
							" d.employee=:emp AND d.description in ('INCR', 'PROMOT', 'MACPS', 'HCOMMN', 'MSTHAV', 'MSTJCO', 'LATENR') " + 
							" AND d.status='V' AND d.fromDate = :fromDate")
							.setParameter("emp", emp)
							.setParameter("id", do2.getId())
							.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		
		if (duplicateDo2List == null || duplicateDo2List.size() == 0) {
			return false;
		} else if (duplicateDo2List!=null && duplicateDo2List.size()>0){
			for (Do2 promotDo2 : duplicateDo2List) {
				Date acpDate = getAcpDateBeforeCurrentDo2FromDate(promotDo2);
				if (acpDate!=null && !acpDate.after(promotDo2.getFromDate())) {
					return false;
				}
			}
		}	

		return true;
	}
	
	
	public boolean doesDuplicateIncrDo2Exist(Do2 do2) {
		Date qualYearBeg = new Date();
		Date qualYearEnd = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar prevIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(do2.getFromDate());
		int date = currentIncrCal.get(Calendar.DATE);
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);
		Employee emp = do2.getEmployee();
		if (year<2006) {
			prevIncrCal = new GregorianCalendar(year - 1, month, 2);
		 	qualYearBeg.setTime(prevIncrCal.getTimeInMillis());
			qualYearEnd.setTime(DateManager.getPreviousDate(currentIncrCal.getTime()).getTime());
			if (emp==null)
				emp=Utils.getCurrentEmployee(do2.getArmyNo());
			List<Do2> duplicateDo2List = do2Repo.findByFromDateBetweenAndSpecificColumnC(emp.getId(), do2.getArmyNo(),
																					qualYearBeg, qualYearEnd);
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
								" (d.employee=:emp OR d.armyNo=:ano) AND d.description='INCR' AND d.status='V' " + 
						        " AND d.fromDate between :prevIncrDate and :currIncrPrevDate AND "+
						        "(d.columnC is null or (d.columnC is not null and d.columnC not in ('POSTPONED','ADHOC'))) ")
						        .setParameter("ano", do2.getArmyNo())
								.setParameter("emp", emp)
								.setParameter("prevIncrDate", qualYearBeg)
								.setParameter("currIncrPrevDate", qualYearEnd).getResultList();*/
	//		System.out.println("wwwwwwwwwwwwwwwwwwwww  qualyrbeg qualyrend duplido2list do2fromdt  "+qualYearBeg+"  "+qualYearEnd+"  "+duplicateDo2List.size()+"  "+do2.getFromDate());

			return (duplicateDo2List!=null && duplicateDo2List.size()>0);
		}		
		
		if (year >= 2006 && year <= 2015) {
			List<Do2> sixcpcList = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(emp.getId(), "SIXCPC", "V");
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='SIXCPC' AND d.status='V'")
			.setParameter("emp", emp).getResultList();*/

			if (sixcpcList == null || sixcpcList.size() == 0){
				if ((month+1 == 7 && date != 1) || (month+1 != 7 )) {
					if (firstJanuaryOfTheIncrYear.equals(do2.getFromDate())){
						;
					} else if ((do2.getColumnC() == null || (do2.getColumnC() != null && !do2.getColumnC().equals("POSTPONED")))) {
					   return true;
					}
				} 
			} else {
				Date sixcpcOptDate = sixcpcList.get(0).getFromDate();
				Calendar sixcpcCal = new GregorianCalendar();
				sixcpcCal.setTime(sixcpcOptDate);
				int sixcpcYear = sixcpcCal.get(Calendar.YEAR);
				if (do2.getFromDate().after(sixcpcOptDate)){
					if (year > 2006 && ((month+1 == 7 && date != 1) || (month+1 != 7 ))  && 
							((do2.getColumnC() == null || (do2.getColumnC() != null && !do2.getColumnC().equals("POSTPONED"))))) {
							return true;
					} 
					if (year == sixcpcYear && !(firstJanuaryOfTheIncrYear.equals(sixcpcOptDate))){
						return true;
					}
				}
				
			}		 
		}
		if (year >= 2006) {
           if (month + 1 >= 7) {
			  prevIncrCal = new GregorianCalendar(year, Calendar.JULY, 1);
			  currentIncrCal = new GregorianCalendar(year + 1, Calendar.JUNE, 30);
           } else {
        	  prevIncrCal = new GregorianCalendar(year - 1, Calendar.JULY, 1);
        	  currentIncrCal = new GregorianCalendar(year, Calendar.JUNE, 30);
           }
		} else {
			prevIncrCal = new GregorianCalendar(year - 1, month, 1);
			currentIncrCal.setTime(do2.getFromDate());
		}
		qualYearBeg.setTime(prevIncrCal.getTimeInMillis());
		qualYearEnd.setTime(currentIncrCal.getTimeInMillis());
		if (emp==null)
			emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> duplicateDo2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanEqualToAndSpecidicColumnCAndDo2IdNotEqual(emp.getId(),
																								do2.getArmyNo(), do2.getId(), qualYearBeg, qualYearEnd);
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id!=:id AND " +
							" (d.employee=:emp OR d.armyNo=:ano) AND d.description='INCR' AND d.status='V' AND" +
							" (d.columnC is null or (d.columnC is not null and d.columnC not in ('POSTPONED','ADHOC'))) " +
					        " AND d.fromDate >= :prevIncrDate and d.fromDate <= :fromDate")
					        .setParameter("ano", do2.getArmyNo())
							.setParameter("emp", emp)
							.setParameter("id", do2.getId())
							.setParameter("prevIncrDate", qualYearBeg)
							.setParameter("fromDate", qualYearEnd).getResultList();*/
		return (duplicateDo2List.size()>1);
	}	
	
	public boolean superannuationBeforeIncrDate(Do2 do2, Employee employee) {
		List<Do2> hcommnList = do2Repo.findByDescriptionStatusOrderByFromDateDesc(employee.getId(), "HCOMMN", "V");
				
	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE  " +
				" d.employee=:emp  AND d.description='HCOMMN' AND d.status='V' order by d.fromDate desc")
				.setParameter("emp", employee).getResultList();*/
		if (hcommnList.size() > 0){			
			if (hcommnList.get(0).getFromDate().before(do2.getFromDate())){ 
			   if (hcommnList.get(0).getToDate() == null){
				   return true;
			   } else if (!hcommnList.get(0).getToDate().after(do2.getFromDate())){
				   return true;
			   }
			}
		}	
		return false;
	}
	public int getNqsForIncr(Employee emp, Date incr_date) {
		// If there is no audit cage, the method will return -1. 
			Calendar cal = new GregorianCalendar();
			cal.setTime(incr_date);
			int year = cal.get(Calendar.YEAR);
			int prev_year = (year - 1);
			int  nqsDays        = 0;
			Date qual_year_frdt = null;
			Date qual_year_todt = null;
			Date first_date     = null;
			Date last_date      = null;
			Date startDate      = null;
			Date endDate        = null;
			//amended
			
			try {
				if (cal.get(Calendar.MONTH)==Calendar.JANUARY) {
					qual_year_frdt = DateManager.getDate(prev_year+"-01-01");
					qual_year_todt = DateManager.getDate(prev_year+"-12-31");
				} else {
					qual_year_frdt = DateManager.getDate(prev_year+"-07-01");
					qual_year_todt = DateManager.getDate(year+"-06-30");
				}
			} catch (Exception e) {
				Utils.handleException(e);
			} 
			
			List<AuditCage> atcgList = acRepo.findAllEffectiveAcOrderByFromDateToDateAsc(emp.getId());
					
			/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac where ac.employee=:employee" +
					" and ac.effective = true and ac.cancelled = false and ac.recordStatus != 'P' order by ac.fromDate, ac.toDate")	
			.setParameter("employee", emp).getResultList();*/
			if (atcgList == null || atcgList.size() == 0)
	              return nqsDays = -1;
			for(AuditCage ac : atcgList){
				if (!ac.getFromDate().after(qual_year_frdt)){
					first_date = ac.getFromDate();
					last_date  = ac.getFromDate();
				}
				if (ac.getFromDate().after(qual_year_frdt) && 
				   !(ac.getFromDate().after(qual_year_todt)))
						last_date = ac.getFromDate();			
			}
		   
			// added on 24.08.2011 to accept the increment do2s of dropped and recreated cases --- to be checked.
			if (first_date == null) {
		    	for (AuditCage ac1 : atcgList) {
		    		if (!ac1.getFromDate().before(qual_year_frdt) && ac1.getFromDate().before(qual_year_todt)) {
		    			first_date = ac1.getFromDate();
		    		}
		    	}
		    }
			
		    if (first_date==null)
	    		return nqsDays = -1;
	        atcgList = acRepo.findByFromDateBetweenOrderByFromDateToDate(emp.getId(),first_date,last_date);
	        		
	        		
	 /*       		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac where ac.employee=:employee and ac.effective = true" +
		    	" and ac.cancelled = false and ac.recordStatus != 'P' and ac.fromDate between :firstDate and :lastDate order by ac.fromDate, ac.toDate")	
			.setParameter("employee", emp)
			.setParameter("firstDate", first_date)
			.setParameter("lastDate", last_date).getResultList();*/	
		   
		    int i = 0;
		    startDate = null;
	        endDate = qual_year_todt;
	        nqsDays=0;
		    for(AuditCage ac : atcgList){
		    	++i;
		    	if (i == atcgList.size()){
		    	 	if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == true && startDate != null){
		    		     endDate = qual_year_todt;
		    		     if (endDate.after(startDate)){
	     						nqsDays += DateManager.getNoOfDays(startDate, endDate);
	     				 }	
		     		} else if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == false && startDate != null){
		     					endDate=DateManager.getDate(ac.getFromDate(), -1);	
		     					if (endDate.after(startDate)){
		     						nqsDays += DateManager.getNoOfDays(startDate, endDate);
		     					}		     					
		                    } 
		    	} else if  (ac.getDeputationFlag() == false && ac.getAbsentFlag() == false && startDate != null){
		    			endDate=DateManager.getDate(ac.getFromDate(), -1);	
     					if (endDate.after(startDate)){
     						nqsDays += DateManager.getNoOfDays(startDate, endDate);
     					}
		    			startDate = null;
		    			endDate = qual_year_todt;
		    	}
		    	
		    	if (ac.getDeputationFlag() == false && ac.getAbsentFlag() == true && startDate == null){
		    	   startDate = ac.getFromDate();
		    	   if (startDate.before(qual_year_frdt))
		    		   startDate=qual_year_frdt;
		     	}
		    }
		     return nqsDays;
		}

    private int getQualYearTotalDays(Date incrDate) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(incrDate);
        int year = cal.get(Calendar.YEAR);
        int prev_year = year - 1;
        Date qualFromDate, qualToDate;
        try {
            if (cal.get(Calendar.MONTH) == Calendar.JANUARY) {
                qualFromDate = DateManager.getDate(prev_year + "-01-01");
                qualToDate   = DateManager.getDate(prev_year + "-12-31");
            } else {
                qualFromDate = DateManager.getDate(prev_year + "-07-01");
                qualToDate   = DateManager.getDate(year + "-06-30");
            }
            return (int) DateManager.getNoOfDays(qualFromDate, qualToDate);
        } catch (Exception e) {
            Utils.handleException(e);
            return 365; // safe default
        }
    }

	public boolean doesIncrDo2ExistWithinOneYear(Do2 do2) {
		Employee emp  = do2.getEmployee();
		Date prevIncrDate = null;
		boolean rejectable = false;
 		if (!do2.getFromDate().before(sevencpcDate)) {
			Calendar cal = new GregorianCalendar();
			cal.setTime(do2.getFromDate());
			Date firstJanuary=null, firstJuly=null, oneYearPriorDate=null;
			try {
				firstJanuary = DateManager.getDate(cal.get(Calendar.YEAR), 1, 1);
				firstJuly = DateManager.getDate(cal.get(Calendar.YEAR), 7, 1);	
				oneYearPriorDate = DateManager.getDate(cal.get(Calendar.YEAR)-1, cal.get(Calendar.MONTH)+1, 1);			
			} catch (Exception e) {
				Utils.handleException(e);
			}
			
			List<String> descList=Arrays.asList(new String[] {"PROMOT","MACPS","HCOMMN","MSTHAV","MSTJCO","LATERN","OPTFIX"});
			List<Do2> promotMacpDo2List=do2Repo.findByDescListFromdateGreaterThanFromDateLessThanAndDoIdNotEqualTo(emp.getId(),descList,
																					oneYearPriorDate,do2.getFromDate(),do2.getId());
					
					
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND "
		//			+ " d.description in ('PROMOT', 'MACPS', 'HCOMMN', 'MSTHAV', 'MSTJCO', 'LATENR', 'OPTFIX') "
					+ " AND d.status='V' and d.fromDate>:lastIncrDate and d.fromDate<:thisIncrDate and d.id!=:did ORDER BY " 
					+ " d.fromDate DESC, d.description DESC ").setParameter("emp", emp).setParameter("lastIncrDate", oneYearPriorDate)
					.setParameter("thisIncrDate", do2.getFromDate()).setParameter("did", do2.getId()).getResultList();*/	
			
			if (promotMacpDo2List==null || (promotMacpDo2List!=null && promotMacpDo2List.size()==0)) {
				List<Do2> incrDo2List =  do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanAndDo2IdNotEqual(emp.getId(),
																								"INCR",oneYearPriorDate,do2.getFromDate(),do2.getId());
						
						
				/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND "
						+ " d.description='INCR' AND d.status='V' and d.fromDate>=:lastIncrDate and d.fromDate<:thisIncrDate and d.id!=:did ORDER BY " 
						+ " d.fromDate DESC ").setParameter("emp", emp).setParameter("lastIncrDate", oneYearPriorDate)
						.setParameter("thisIncrDate", do2.getFromDate()).setParameter("did", do2.getId()).getResultList();*/	
				if (incrDo2List!=null && incrDo2List.size()>0) {
			 		double noOfDaysSinceLastIncr=DateManager.getNoOfDays(incrDo2List.get(0).getFromDate(), do2.getFromDate());	
			 		System.out.println ( " noOfDaysSinceLastIncr " +noOfDaysSinceLastIncr  + " last incr from date " +incrDo2List.get(0).getFromDate()  +" do2 from date " +do2.getFromDate());
	 		 		if ((incrDo2List.get(0).getColumnC()!=null && incrDo2List.get(0).getColumnC().startsWith("POSTPONE"))
			 				&& noOfDaysSinceLastIncr>179) {
			 			return false;
			 		} else if ((incrDo2List.get(0).getColumnC()==null || (incrDo2List!=null && incrDo2List.get(0).getColumnC().length()>0))
			 	 			&& noOfDaysSinceLastIncr>364) {
			 			return false;
			 		} else {
			 			List<String> descList2=Arrays.asList(new String[] {"ACPFIX","PROFIX","HAVFIX"});
			 			List<Do2> payfixDo2List = do2Repo.findByDescListFromdateGreaterThanFromDateLessThanAndDoIdNotEqualTo(emp.getId(),
			 																						descList2,oneYearPriorDate,do2.getFromDate(),do2.getId());
			 					
			 			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND "
								+ " d.description in ('ACPFIX', 'PROFIX', 'HAVFIX') AND d.status='V' and d.fromDate>=:lastIncrDate and d.fromDate<:thisIncrDate and d.id!=:did ORDER BY " 
								+ " d.fromDate DESC ").setParameter("emp", emp).setParameter("lastIncrDate", oneYearPriorDate)
								.setParameter("thisIncrDate", do2.getFromDate()).setParameter("did", do2.getId()).getResultList();*/
			 			if (payfixDo2List == null || (payfixDo2List!=null && payfixDo2List.size()==0)) {
			 				return true;
			 			} else {
			 				return false;	
			 			}			 			
			 		}			 		 
				} else {	
					List<String> descList3=Arrays.asList(new String[] {"ATTSBM","ATTSAM","MUSTER"});
					List<Do2> musterAttestDo2List=do2Repo.findByMultipleDescriptionsFromDateGreaterThanAndDo2IdNotEqualOrderByFromDateDescAndDescriptionDesc(emp.getId(),
																												descList3, emp.getEnrolmentDate(), do2.getId());
							
							
				/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND "
					 		+ " d.description in ('ATTSBM', 'ATTSAM', 'MUSTER') AND d.status='V' and d.fromDate>:enrolDate "
					 		+ " and d.id!=:did ORDER BY d.fromDate DESC, d.description DESC ")
							.setParameter("emp", emp).setParameter("enrolDate", emp.getEnrolmentDate())
							.setParameter("did", do2.getId()).getResultList();*/	
					if (musterAttestDo2List!=null && musterAttestDo2List.size()>0) {
						for (Do2 mado2 : musterAttestDo2List) {
							if (mado2.getDescription().equals("ATTSBM") || mado2.getDescription().equals("ATTSAM") || mado2.getDescription().equals("MUSTER")) {
								double noOfDaysSinceEnrol=DateManager.getNoOfDays(mado2.getEmployee().getEnrolmentDate(), do2.getFromDate());
								if (noOfDaysSinceEnrol>179) {
					 				return false;
								} else {
					 				return true;
								}
							} 
						}						
					} else {
					
						double noOfDaysSinceEnrol=DateManager.getNoOfDays(do2.getEmployee().getEnrolmentDate(), do2.getFromDate());
	 					if (noOfDaysSinceEnrol>179) {
			 				return false;
						} else {
			 				return true;
						}
					}
				
				}				
			}
 
			for (Do2 d : promotMacpDo2List) {
				
				List<Do2> incrDo2List =  do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanAndDo2IdNotEqual(emp.getId(),
						"INCR",oneYearPriorDate,do2.getFromDate(),do2.getId());
				System.out.println("incrDo2List (inside promot loop) size=" + (incrDo2List!=null ? incrDo2List.size() : 0));
			
				if (incrDo2List!=null && incrDo2List.size()>0) {
					double noOfDaysSinceLastIncr=DateManager.getNoOfDays(incrDo2List.get(0).getFromDate(), do2.getFromDate());	
					if (noOfDaysSinceLastIncr > 364) {
						System.out.println("More than 1 year gap since last incr inside loop -> return false");
						return false;
					}
					if ((incrDo2List.get(0).getColumnC()!=null && incrDo2List.get(0).getColumnC().startsWith("POSTPONE"))
			 				&& noOfDaysSinceLastIncr>179) {
			 			return false;
			 		}
				}
				
				
	 	 		double noOfDaysSinceLastIncr=DateManager.getNoOfDays(d.getFromDate(), do2.getFromDate());	
	 	 		System.out.println ( " no of days since last incr " +noOfDaysSinceLastIncr  + "do2 from date " +d.getFromDate()   +" do2 from date " +do2.getFromDate());
 	 			if (d.getDescription().equals("OPTFIX")) {
	 				return false;
	 			}
 	 			if (d.getDescription().equals("PROMOT") && d.getColumnA().equals("TSNK")) {
	 				return false;
	 			}
	 			if (d.getDescription().equals("PROMOT") || d.getDescription().equals("MSTHAV") || d.getDescription().equals("MSTJCO")) {
					Date acpDate = getAcpDateBeforeCurrentDo2FromDate(d);
					if (acpDate!=null && !acpDate.after(d.getFromDate())) {
						if (DateManager.getNoOfDays(acpDate, do2.getFromDate())>179) {
							return false;
						} else {
							rejectable=true;
							continue;
						}
					} else if (d.getToDate()!=null && d.getToDate().equals(do2.getFromDate())) {
						return false;
					} else if (acpDate==null && noOfDaysSinceLastIncr>179) {
						return false;
					} else {
						if (acpDate==null && d.getDescription().equals("MSTHAV")) {
							if (emp.getHavDate()!=null && emp.getHavDate().before(d.getFromDate())) {
								noOfDaysSinceLastIncr=DateManager.getNoOfDays(emp.getHavDate(), do2.getFromDate());
								if (noOfDaysSinceLastIncr>179) {
									return false;
								}
							}
							if ((emp.getAcpnaikDate()!=null && emp.getAcpnaikDate().before(d.getFromDate()))
									|| (emp.getAcphavDate()!=null && emp.getAcphavDate().before(d.getFromDate()))) {
								
								if  (emp.getAcphavDate()!=null)
									noOfDaysSinceLastIncr=DateManager.getNoOfDays(emp.getAcphavDate(), do2.getFromDate());
								else if (emp.getAcpnaikDate()!=null) 
									noOfDaysSinceLastIncr=DateManager.getNoOfDays(emp.getAcpnaikDate(), do2.getFromDate());
								
								if (noOfDaysSinceLastIncr>179) {
									return false;
								} else {
									return true;
								}
							} else  {
								return true;
							}
							
						} else {
							
							noOfDaysSinceLastIncr=DateManager.getNoOfDays(do2.getFromDate(), oneYearPriorDate);
							
							System.out.println ( " xxx no fo days since last year xxx " +noOfDaysSinceLastIncr  + "do2 from date" +do2.getFromDate()  +" one yer priordate" +oneYearPriorDate);
							if (noOfDaysSinceLastIncr>=364) {
								return false;
							} else {
								return true;
							}
							
						}						
					}
				} else if (d.getDescription().equals("MACPS")) {
							if (d.getToDate()!=null && d.getToDate().equals(do2.getFromDate())) {
								return false;
							} else if (DateManager.getNoOfDays(d.getFromDate(), do2.getFromDate())>179 && !d.getFromDate().before(sevencpcDate)) {
								return false;
							} else {
								rejectable=true;
								continue;
							}
				} else if (d.getDescription().equals("HCOMMN") && d.getColumnC()!=null && d.getColumnC().trim().length()==8) {
							Date optionDate = null;
					 		optionDate = DateManager.getDateFromDdMmYyyy(do2.getColumnC());		 				
					 		if (optionDate!=null && optionDate.equals(do2.getFromDate())) {
					 			return false;
					 		} else {
					 			return true;
					 		}
				} else if (noOfDaysSinceLastIncr>179) {
					return false;					
				} else {
		 			rejectable=true;
		 			continue;
				}				
			}
	 		return rejectable;
		}
	 	if (do2.getFromDate().before(sevencpcDate)) {
			if (sixcpcDo2Exists(do2)) {
				if (do2.getFromDate().before(sixcpcDate)){
					 prevIncrDate=getPreviousIncrementSixCpcDate(do2.getFromDate());
				} else {
					 prevIncrDate=getPreviousIncrementDate(do2.getFromDate());					
				}
			} else {
				 prevIncrDate=getPreviousIncrementDate(do2.getFromDate());			
			}			
			
			Date prevDate = DateManager.getDate(do2.getFromDate(), -1);
		 	
			Date incrDt = do2.getFromDate();
			try {
				if (incrDt.after(DateManager.getDate("2006-06-30")) &&  !incrDt.after(DateManager.getDate("2006-12-31"))) {
					incrDt = DateManager.getDate("2006-07-01");
				}
			} catch (Exception e1) {
				Utils.handleException(e1);
			}
			
			if (emp==null)
				emp=Utils.getCurrentEmployee(do2.getArmyNo());
			
			List<Do2> incrDo2List = do2Repo.findByFromDateGreaterThanAndFromDateLessThanEqualToAndSpecificColumnCNotInAndDo2IdNotEqual(emp.getId(),
																									"INCR", do2.getId(), prevIncrDate, prevDate);
					
					
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
						" d.employee=:emp AND d.id !=:id AND d.description='INCR' AND " +
						" (d.columnC is null or (d.columnC is not null and d.columnC not in ('POSTPONED','ADHOC'))) AND d.status='V' " +
						" AND d.fromDate > :prevIncrDate and d.fromDate <= :prevDate")
						.setParameter("id", do2.getId())
						.setParameter("emp", emp)
						.setParameter("prevIncrDate",prevIncrDate)
						.setParameter("prevDate", prevDate).getResultList();*/
 			if (incrDo2List==null || incrDo2List.size()==0)
				return false;
			
			if (incrDo2List.size()>0) {
				Calendar incrCal = new GregorianCalendar();
				incrCal.setTime(incrDo2List.get(0).getFromDate());
				int date = incrCal.get(Calendar.DATE);
				int year = incrCal.get(Calendar.YEAR);
				int month = incrCal.get(Calendar.MONTH) + 1 ;{
				
		 		try {
					if (incrDt.equals(DateManager.getDate("2006-07-01"))){
						if (incrDo2List.get(0).getFromDate().equals(DateManager.getDate("2006-01-01"))
								|| (year==2005 && (month>6 && month<=12)))
							return false;
					} else {
						Date qualYearBeg = new Date();
						Date qualYearEnd = new Date();
						Calendar currentIncrCal = new GregorianCalendar();
						Calendar prevIncrCal = new GregorianCalendar();
						currentIncrCal.setTime(do2.getFromDate());
						date = currentIncrCal.get(Calendar.DATE);
						year = currentIncrCal.get(Calendar.YEAR);
						month = currentIncrCal.get(Calendar.MONTH);
						if (year>=2006) {
				           if ((month+1)>=7) {
							  prevIncrCal = new GregorianCalendar(year, Calendar.JULY, 1);
							  currentIncrCal = new GregorianCalendar((year+1), Calendar.JUNE, 30);
				           } else {
				        	  prevIncrCal = new GregorianCalendar((year-1), Calendar.JULY, 1);
				        	  currentIncrCal = new GregorianCalendar(year, Calendar.JUNE, 30);
				           }
						} else {
							prevIncrCal = new GregorianCalendar((year-1), month, 1);
							currentIncrCal.setTime(do2.getFromDate());
						}
						qualYearBeg.setTime(prevIncrCal.getTimeInMillis());
						qualYearEnd.setTime(currentIncrCal.getTimeInMillis());
						if (emp==null)
							emp=Utils.getCurrentEmployee(do2.getArmyNo());
						
				 		List<Do2> duplicateDo2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanEqualToAndSpecidicColumnCAndDo2IdNotEqual(emp.getId(),
				 																			do2.getArmyNo(), do2.getId(), qualYearBeg, qualYearEnd);
				 				
				 				
				 		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id!=:id AND " +
											" (d.employee=:emp OR d.armyNo=:ano) AND d.description='INCR' AND d.status='V' AND" +
											" (d.columnC is null or (d.columnC is not null and d.columnC not in ('POSTPONED','ADHOC'))) " +
									        " AND d.fromDate >= :prevIncrDate and d.fromDate <= :fromDate")
									        .setParameter("ano", do2.getArmyNo())
											.setParameter("emp", emp)
											.setParameter("id", do2.getId())
											.setParameter("prevIncrDate", qualYearBeg)
											.setParameter("fromDate", qualYearEnd).getResultList();*/
//				 		System.out.println("yyyyyyyyyyyyyyyyyyyy  qaulyerbeg qualyearend "+qualYearBeg+"  "+qualYearEnd+"  "+duplicateDo2List.size());
		 				return (duplicateDo2List.size()>0);					
					}
				} catch (Exception e) {
					Utils.handleException(e);
				}
				return true;
				}
			}	 
			return true;
		}
		return false;
	}

	public Date getPreviousIncrementDate(Date currentIncrementDate) {	
		Date dni = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar prevIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(currentIncrementDate);
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);		
		if ((year-1)>=2006) {
			prevIncrCal = new GregorianCalendar((year-1), Calendar.JULY, 1);
		} else {
			prevIncrCal = new GregorianCalendar((year-1), month, 1);
		}
		dni.setTime(prevIncrCal.getTimeInMillis());
		return dni;
	}
	
	public Date getPreviousIncrementDateIn7cpc(Date currentIncrementDate) {	
		Date dni = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar prevIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(currentIncrementDate);
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);		
		if (year>2015 && month==Calendar.JANUARY) {
			prevIncrCal = new GregorianCalendar((year-1), Calendar.JULY, 1);
		} else {
			prevIncrCal = new GregorianCalendar((year-1), month, 1);
		}
		dni.setTime(prevIncrCal.getTimeInMillis());
		return dni;
	}
	
	public Date getPreviousIncrementSixCpcDate(Date currentIncrementDate) {		
		Date dni = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar prevIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(currentIncrementDate); 
		int year = currentIncrCal.get(Calendar.YEAR);
		int month = currentIncrCal.get(Calendar.MONTH);			
		prevIncrCal = new GregorianCalendar((year-1), month, 1);			
		dni.setTime(prevIncrCal.getTimeInMillis());
		return dni;
	}

	
	static public Date getFirstJanuary(Date currentIncrementDate) {		
		Date firstJanuary = currentIncrementDate;
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(currentIncrementDate);
		
		try {
			firstJanuary = DateManager.getDate(cal.get(Calendar.YEAR), cal.get(Calendar.JANUARY), 1);
		} catch (Exception e) {
			 Utils.handleException(e);
		}
		
		return firstJanuary;
	}
	
	public Date getFromDateForPromot(Do2 do2) {
		firstJanuaryOfTheIncrYear=getFirstJanuary(do2.getFromDate());
		Date promotFromDate = null;
		
	/*	List<Do2> do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp AND "
				+ " d.fromDate<:fromDate AND (d.description in ('ACP', 'MACPS', 'PROMOT', 'MSTHAV', 'MSTJCO', 'LATENR', 'HCOMMN', 'REMUST')) AND d.status='V' "
				+ " ORDER BY d.fromDate desc").setParameter("emp", do2.getEmployee()).setParameter("fromDate", do2.getFromDate()).getResultList();
	*/	
		List<String> descList=Arrays.asList(new String[] {"ACP","MACPS","PROMOT","MSTHAV","MSTJCO","LATENR","HCOMMN", "REMUST"});
		List<Do2> do2List = do2Repo.findByDescListFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(), "V", descList,do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp AND "
				+ " d.fromDate<:fromDate AND (d.description in ('ACP', 'MACPS', 'PROMOT', 'MSTHAV', 'MSTJCO', 'LATENR', 'HCOMMN')) AND d.status='V' "
				+ " ORDER BY d.fromDate desc").setParameter("emp", do2.getEmployee()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
			
		if (do2List.size() > 0) {
			for (Do2 promotDo2 : do2List){
				if (!promotDo2.getFromDate().after(firstJanuaryOfTheIncrYear)){
					promotFromDate=promotDo2.getFromDate();
				} else {
					promotFromDate=promotDo2.getFromDate();
		 			if (Utils.isAcpBeforeCurrentDo2(promotDo2)) {
		 				promotFromDate=getAcpDateBeforeCurrentDo2FromDate(promotDo2);
	 					if (higherRankAcpAvailable) {
							if (higherRankAcpDate!=null && higherRankAcpDate.before(promotDo2.getFromDate())) {
								promotFromDate=higherRankAcpDate;
							}
						}						
						if (promotFromDate.before(firstJanuaryOfTheIncrYear)) {
							promotFromDate=promotDo2.getFromDate();
						}
					}
					
			
					//promotFromDate=promotDo2.getFromDate();
					
					break;
				} 
			/*	if (!promotDo2.getFromDate().after(firstJanuaryOfTheIncrYear)){
					promotFromDate=do2.getFromDate();
					break;
				} else {
					promotFromDate=promotDo2.getFromDate();
					break;
				}*/
				 
			}			
		}
	 	return promotFromDate;
	}
	
	public Date getFromDateForPromotFor7CPC(Do2 do2) {
		firstJanuaryOfTheIncrYear=getFirstJanuary(do2.getFromDate());
		Date promotFromDate = null;
		List<String> descList=Arrays.asList(new String[] {"ACP","MACPS","PROMOT","MSTHAV","MSTJCO","LATENR","HCOMMN","REMUST"});
		List<Do2> do2List = do2Repo.findByFromDateLessThanMultipleDescriptionAndColANotInOrderByFromDateDesc(do2.getEmployee().getId(),
																									do2.getFromDate(), descList);
				
			/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp AND "
				+ " d.fromDate<:fromDate AND (d.description in ('ACP', 'MACPS', 'PROMOT', 'MSTHAV', 'MSTJCO', 'LATENR', 'HCOMMN', 'REMUST')) AND d.status='V' "
				+ " AND d.columnA not in ('TSNK') ORDER BY d.fromDate desc").setParameter("emp", do2.getEmployee()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
		if (do2List.size() > 0) {
			for (Do2 promotDo2 : do2List){
			/*	if (!promotDo2.getFromDate().after(firstJanuaryOfTheIncrYear)){
					promotFromDate=promotDo2.getFromDate();
				} else {
					promotFromDate=promotDo2.getFromDate();
					break;
				}*/
				if (!promotDo2.getFromDate().after(firstJanuaryOfTheIncrYear)){
					promotFromDate=do2.getFromDate();
					break;
				} else {
					promotFromDate=promotDo2.getFromDate();
					break;
				}
				 
			}			
		}            
		return promotFromDate;
	}
	
	public  boolean sixcpcDo2Exists(Do2 do2){
		List<Do2> do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(), "SIXCPC", Do2Manager.VALID);
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status=:status and d.description=:description")
													.setParameter("emp",do2.getEmployee())
													.setParameter("description","SIXCPC")
													.setParameter("status",Do2Manager.VALID)
													.getResultList();*/
		
        if (do2List.size()>0) {
        	sixcpcDate = do2List.get(0).getFromDate();
        	return true;
        }
        return false;
	}
	
	public  boolean optcpcDo2For7cpcExists(Do2 do2){
		List<Do2> do2List =  do2Repo.findByEmployeeIdAndDescriptionAndStatusAndColumnB(do2.getEmployee().getId(),"OPTCPC","SEVENPC","V");
				
		//		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' and d.description='OPTCPC' "
			//		+ " AND d.columnB='SEVENPC' ").setParameter("emp",do2.getEmployee()).getResultList();
		
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		   	return true;
		}
		return false;
	}
	
	public  boolean fix7pcDo2Exists(Do2 do2){
		List<Do2> do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(), "FIX7PC", "V");
				
			//	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' and d.description='FIX7PC' ")
		//		.setParameter("emp",do2.getEmployee()).getResultList();
		
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		   	return true;
		}
		return false;
	}
	public Date musterIncrCal(Employee employee, Do2 do2) {
		Date incrementDate = null; 
		 
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && employee.getInitialMusteringDate() == null){
			return null;
		}
		
		if (employee.getInitialMusteringDate()!=null) {
			if (employee.getInitialMusteringDate().before(DateManager.getSixthCpcDate())) {
				incrementDate = Utils.getNextIncrementDate(employee.getInitialMusteringDate());
			} else if (employee.getInitialMusteringDate().before(DateManager.getSeventhCpcDate())
						&& employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
					Date musterDate = employee.getInitialMusteringDate();
					Calendar cal = new GregorianCalendar();
					cal.setTime(musterDate);
					try {
						if (cal.get(Calendar.MONTH) == Calendar.JANUARY	&& cal.get(Calendar.DATE) == 1) {
							incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
						} else {
							incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
						}
					} catch (Exception e) {
						Utils.handleException(e);
					}					
			} else if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
					&& employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())
					&& ((employee.getAttestationDate()!=null && !employee.getAttestationDate().before(DateManager.getSeventhCpcDate()))
					|| (employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(DateManager.getSeventhCpcDate())))) {
					Date enrollDate = employee.getEnrolmentDate();
					Calendar cal = new GregorianCalendar();
					cal.setTime(enrollDate);
					try {
						if (cal.get(Calendar.MONTH) == Calendar.JANUARY	&& cal.get(Calendar.DATE) == 1) {
							incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
						} else {
							incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
						}
					} catch (Exception e) {
						Utils.handleException(e);
					}			
			} else if (!employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
					incrementDate = Utils.getIncrementDateForAttestMusterIn7cpc(employee.getEnrolmentDate());	
			}  					
		}
		return incrementDate;
	}
	
	public Date grhavIncrCal(Employee employee, Do2 do2) {
		/*Date incrementDate = null;
		if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() == null){
			return null;
		} 
		// Attestation Date and Enrolment date assumed to be same. Unless Muster is published
		// Increment can not be granted.
		if (Utils.getPao().getPaoCode().equals("93")){
			if (employee.getAttestationDate().before(employee.getInitialMusteringDate())) {
				incrementDate= Utils.getNextIncrementDate(employee.getAttestationDate());
			}	
		}
		return incrementDate;*/
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && 
				employee.getAttestationDate() == null && employee.getInitialMusteringDate() == null){
			return null;
		} 
		Date incrementDate = null;
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
			&& employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())
			&& ((employee.getAttestationDate()!=null && !employee.getAttestationDate().before(DateManager.getSeventhCpcDate()))
			|| (employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(DateManager.getSeventhCpcDate())))) {
			Date enrollDate = employee.getEnrolmentDate();
			Calendar cal = new GregorianCalendar();
			cal.setTime(enrollDate);
			try {
				if (cal.get(Calendar.MONTH) == Calendar.JANUARY	&& cal.get(Calendar.DATE) == 1) {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
				} else {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
			return incrementDate;
		}
		
		
		if (Utils.getPao().getPaoCode().equals("93")) {
			if (employee.getInitialMusteringDate()!=null && employee.getAttestationDate()!=null) {
				if (employee.getAttestationDate().before(employee.getInitialMusteringDate())) {
					incrementDate= Utils.getNextIncrementDate(employee.getAttestationDate());
				} else {
					incrementDate= Utils.getNextIncrementDate(employee.getInitialMusteringDate());
				}
			} else if (employee.getInitialMusteringDate()!=null) {
				incrementDate= Utils.getNextIncrementDate(employee.getInitialMusteringDate());
			}
		}
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
				&& !employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate()) 
				&& (employee.getAttestationDate() != null || employee.getInitialMusteringDate() != null)) {
			incrementDate = Utils.getIncrementDateForAttestMusterIn7cpc(employee.getEnrolmentDate());
		}
		
		return incrementDate;
	}
	public Date musterAttestIncrCal(Employee employee, Do2 do2) {
		Date incrementDate = null;
		Date empMusterAttDate=null;
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
			&& employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())
			&& ((employee.getAttestationDate()!=null && !employee.getAttestationDate().before(DateManager.getSeventhCpcDate()))
			|| (employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().before(DateManager.getSeventhCpcDate())))) {
			Date enrollDate = employee.getEnrolmentDate();
			Calendar cal = new GregorianCalendar();
			cal.setTime(enrollDate);
			try {
				if (cal.get(Calendar.MONTH) == Calendar.JANUARY	&& cal.get(Calendar.DATE) == 1) {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");
				} else {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)+1 + "-07-01");
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
			return incrementDate;
		}
		if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() == null){
			return null;
		} else {
			if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
					&& !employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())
					&& (employee.getAttestationDate()!=null || employee.getInitialMusteringDate()!=null)) {
		   		return Utils.getIncrementDateForAttestMusterIn7cpc(employee.getEnrolmentDate());
			}
			if (employee.getAttestationDate().before(employee.getInitialMusteringDate())) {
				if (DateManager.getNoOfDays(employee.getAttestationDate(), employee.getInitialMusteringDate()) > 364) {
					Calendar cal = new GregorianCalendar();
					cal.setTime(employee.getInitialMusteringDate());
					cal.set(Calendar.DATE,1);
					empMusterAttDate= cal.getTime();
					if (empMusterAttDate.before(DateManager.getSixthCpcDate())) {
						incrementDate = empMusterAttDate;	
	                } else {
						Date musterDate = empMusterAttDate;
						incrementDate = new Date(musterDate.getTime());
						cal = new GregorianCalendar();
						cal.setTime(musterDate);
						cal.set(Calendar.DATE,1);
						try {
							if (cal.get(Calendar.MONTH) < Calendar.JULY) {
	                           incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "-07-01");	
	                        } else if (cal.get(Calendar.MONTH) == Calendar.JULY	&& cal.get(Calendar.DATE) == 1) {
							    incrementDate = cal.getTime();
							} else {
							    incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + 1 + "-07-01");
							}
						} catch (Exception e) {
							Utils.handleException(e);
			 			}
	              }
				} else {   
					empMusterAttDate= Utils.getNextIncrementDate(employee.getAttestationDate());
					if (empMusterAttDate.before(DateManager.getSixthCpcDate())) {
					 	Calendar cal = new GregorianCalendar();
						cal.setTime(empMusterAttDate);
						cal.set(Calendar.DATE,1);
				        incrementDate = cal.getTime();
				    } else {
				    	if (empMusterAttDate.before(DateManager.getSeventhCpcDate())) {
				        	  if (!empMusterAttDate.before(employee.getInitialMusteringDate())) {
							    	Calendar cal = new GregorianCalendar();
									cal.setTime(empMusterAttDate);
									cal.set(Calendar.DATE,1);
							        incrementDate = cal.getTime();
								} else {  
									Calendar cal = new GregorianCalendar();
									cal.setTime(empMusterAttDate);
									cal.set(Calendar.DATE,1);
									incrementDate = cal.getTime();
									try {
										incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)+ 1 + "-07-01");
									} catch (Exception e) {
										Utils.handleException(e);
									}
								}
				    	} else {
				    		if (employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
				    			
				    			Calendar musterCal = new GregorianCalendar();
								musterCal.setTime(employee.getInitialMusteringDate());
								Calendar calJuly = new GregorianCalendar();
								calJuly.setTime(employee.getInitialMusteringDate());
								calJuly.set(Calendar.MONTH, Calendar.JULY);
								calJuly.set(Calendar.DATE,1);
								if (musterCal.before(calJuly)) {
									if(DateManager.getNoOfDays(musterCal.getTime(), calJuly.getTime()) > 178) {
										calJuly.set(Calendar.YEAR, calJuly.get(Calendar.YEAR)-1);
										incrementDate = Utils.getNextIncrementDate(calJuly.getTime());
									//	System.out.println ( " incr date " +incrementDate  + " mustercal " +musterCal.getTime()  + " CAL JULY " +calJuly.getTime());
									} else {
										calJuly.set(Calendar.YEAR, calJuly.get(Calendar.YEAR));
										incrementDate = Utils.getNextIncrementDate(calJuly.getTime());
									//	System.out.println ( " incr date00 " +incrementDate  + " mustercal " +musterCal.getTime()  + " CAL JULY " +calJuly.getTime());

									}
								} else {
									incrementDate = Utils.getNextIncrementDate(calJuly.getTime());
								//	System.out.println ( " incr date11 " +incrementDate   + " mustercal " +musterCal.getTime()  + " CAL JULY " +calJuly.getTime());
								}
				    		}				    	 
				    	}
				    }							 
				}
			}
		}
	 	return incrementDate;		
	}
	public boolean stepup(Do2 do2) {
		Date stepupDate = null;
		List<Do2> do2List = null;
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(), Do2Manager.VALID, "STEPUP", do2.getFromDate());
			/*	Utils.getEntityManager().createQuery(
			" SELECT d FROM Do2 d where d.employee=:emp and d.status=:status and " +
			" d.description=:description AND d.fromDate < :incrDate order by d.fromDate desc")
			.setParameter("emp",do2.getEmployee())
			.setParameter("description","STEPUP")
			.setParameter("incrDate",do2.getFromDate())
			.setParameter("status",Do2Manager.VALID)
			.getResultList();*/
			if (do2List.size()>0) {
				stepupDate = do2List.get(0).getFromDate();
				if (stepupDate.after(firstJanuaryOfTheIncrYear) && stepupDate.before(do2.getFromDate()))
				     return true;
			}
		} else {
			Calendar cal = new GregorianCalendar();
			cal.setTime(do2.getFromDate());
			Date oneYearPriorDate=null;
			try {
 				oneYearPriorDate = DateManager.getDate(cal.get(Calendar.YEAR)-1, cal.get(Calendar.MONTH)+1, 1);			
			} catch (Exception e) {
				Utils.handleException(e);
			}
			do2List = do2Repo.findStepupDo2DuringPastOneYearIn7cpc(do2.getEmployee().getId(), oneYearPriorDate, do2.getFromDate());
			if (do2List.size()>0) {
				stepupDate = do2List.get(0).getFromDate();
				if (DateManager.getNoOfDays(stepupDate, do2.getFromDate())<365) {
				 	return true;
				}				 
			} 
		}	
		return false;	
	}	 

	public int get7cpcPay(Do2 do2, AuditCage auditCage, Date promotDate) {
	 
		String rankCode = ""; 
		int newBasic = auditCage.getBasicPay();
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		List<PayMatrix>payMatrixList = null;
		rankCode = rank.getRankCode();
				
	 	List<Do2> do2List = do2Repo.findByFromDateLessThanEqualToForHcommn(do2.getEmployee().getId(), do2.getFromDate());
	 			
	 	/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and " +
 				" d.status='V' and d.fromDate <= :fromDate and ((d.description='MACPS' and d.columnA='HONYGRADE') " +
 				" or d.description='HCOMMN') order by d.fromDate").setParameter("emp",do2.getEmployee())
 				.setParameter("fromDate", do2.getFromDate()).getResultList();*/

		if (do2List!=null && do2List.size() > 0) {
			if (do2List.get(0).getDescription().equals("MACPS")) {
				rankCode="79";
			} else if (do2List.get(0).getColumnA().equals("HONYLT")) {
				rankCode="80";
			} else {
				rankCode="90";
			}			
		} 
 	
		String payLevel = Do2Manager.getPayLevelPromot(rankCode);
	
		if (payLevel!=null && !payLevel.equals(auditCage.getPayLevel())) {
			payLevel=auditCage.getPayLevel();
		}		
		
		// To validate the existing pay whether its available in the pay level or not
	 	payMatrixList = matrixRepo.findByPayLevelAndBasicPayOrderByBasicPayAsc(payLevel, newBasic);
	 			
	 			
	 	/*		Utils.getEntityManager().createQuery("select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
					+ " pm.basicPay=:sevencpcpay order by basicPay asc ")
					.setParameter("paylevel", payLevel)
					.setParameter("sevencpcpay", newBasic)
					.getResultList();*/
			
		if (payMatrixList==null || payMatrixList.size()==0)
			return newBasic;				
			
		if (do2.getColumnC()!=null && do2.getColumnC().startsWith("POSTPONE")) {
			payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(payLevel, newBasic);
					
			/*		Utils.getEntityManager().createQuery(" select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
					 		+ " pm.basicPay>:sevencpcpay order by basicPay asc ").setParameter("paylevel", payLevel)
					 		.setParameter("sevencpcpay", newBasic).getResultList();*/
				
			if (payMatrixList!=null && payMatrixList.size() > 0) {
				newBasic = payMatrixList.get(0).getBasicPay();
			}
			return newBasic;	
		}
		
		int noOfMacpsPromotDo2DuringYear=0;		 
		Calendar cal = new GregorianCalendar();
		cal.setTime(do2.getFromDate());
		Date sixMonthsPriorDate = null, oneYearPriorDate = null;
		if (cal.get(Calendar.MONTH)==Calendar.JANUARY) {	
			try {
		 		sixMonthsPriorDate=DateManager.getDate(cal.get(Calendar.YEAR)-1, 7, 1);
				oneYearPriorDate=DateManager.getDate(cal.get(Calendar.YEAR)-1, 1, 1);
			} catch (Exception e) {
				Utils.handleException(e);
//	  	System.out.println("======+++++++++++++++++ calmonth  " +cal.get(Calendar.MONTH)); 
			}
		}
		if (cal.get(Calendar.MONTH)==Calendar.JULY) {
			try {
		 		sixMonthsPriorDate=DateManager.getDate(cal.get(Calendar.YEAR), 1, 1);
				oneYearPriorDate=DateManager.getDate(cal.get(Calendar.YEAR)-1, 7, 1);
			} catch (Exception e) {
				Utils.handleException(e);  
			}
		}
	 
 		String atcgPayLevel = Do2Manager.getPayLevel(auditCage.getOldGradePay());
		// to take care LORANK - Pay Level arrived with reference to old Grade Pay  

		if (!rankCode.equals("79")) {
			if (payLevel!=atcgPayLevel) {
				payLevel=atcgPayLevel;
			}
		}
//		System.out.println("----------- cal month  " +cal.getTime()+"  "+cal.get(Calendar.MONTH)+"   "+cal.get(Calendar.YEAR) + sixMonthsPriorDate);
		List<String> descList=Arrays.asList(new String[] {"PROMOT","MACPS","MSTJCO","MSTHAV","HCOMMN","LATENR"});
		do2List = do2Repo.findByFromDateGreaterThanAndFromDateLessThanAndDescriptionListAndColANotIn(do2.getEmployee().getId(),
				descList, oneYearPriorDate, do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(" SELECT d FROM Do2 d where d.employee=:emp and "
				+ " d.status='V' and d.description in ('PROMOT', 'MACPS', 'MSTJCO', 'MSTHAV','HCOMMN', 'LATENR') AND "
				+ " d.columnA not in ('TSNK') AND d.fromDate > :previousDate AND d.fromDate < :presentDate order by d.fromDate")
				.setParameter("emp",do2.getEmployee()).setParameter("previousDate", oneYearPriorDate)
				.setParameter("presentDate", do2.getFromDate()).getResultList();*/
 
		boolean payFixationDoneOnMapcsPromot = false;
		boolean optionForFixationExercised = false;	 		 
 		Date macpsPromotDate=null;
		Do2 macpsPromotDo2 = null;
		
		noOfMacpsPromotDo2DuringYear=do2List.size();
//	   	System.out.println("incr............................. do2list size caltime  "+do2List.size()+"  "+cal.getTime()+"  "+do2.getArmyNo()+"  "+do2.getFromDate() + "  totalpromot "+noOfMacpsPromotDo2DuringYear);
		if (noOfMacpsPromotDo2DuringYear > 0) {
	 		for(int i=0; i<noOfMacpsPromotDo2DuringYear; i++) {
				macpsPromotDo2=do2List.get(i);
	  			macpsPromotDate=macpsPromotDo2.getFromDate();
	  			if (macpsPromotDo2.getDescription().equals("MACPS"))  {	  			
	  				if (macpsPromotDo2.getToDate()!=null && macpsPromotDo2.getToDate().equals(do2.getFromDate())) {
	  					optionForFixationExercised = true;	  				 	
	  	 			} else {
	  	 				payFixationDoneOnMapcsPromot = true;
	  	 			}
	  				break;	  			 
	  			} else {
	  				if (!Utils.isAcpBeforeCurrentDo2(macpsPromotDo2))  { 
//	 					System.out.println("jjjjjjjjjjjjjjjj  inside no macps before promot condition ...................... ");
	  					if ((macpsPromotDo2.getToDate()!=null && macpsPromotDo2.getToDate().equals(do2.getFromDate()))
	  							|| (macpsPromotDo2.getDescription().equals("HCOMMN") && macpsPromotDo2.getColumnC()!=null 
	  								&& macpsPromotDo2.getColumnC().length()==8)) {
	  						optionForFixationExercised = true;	 
	  						if (macpsPromotDo2.getDescription().equals("HCOMMN") && macpsPromotDo2.getColumnC()!=null && macpsPromotDo2.getColumnC().length()==8
	  							&& !DateManager.getDateFromDdMmYyyy(macpsPromotDo2.getColumnC()).equals(do2.getFromDate())) {
	  							optionForFixationExercised = false;	 
	  						}
	  	 				} else if (macpsPromotDo2.getDescription().equals("MSTJCO")) {    // newly added on 01-05-2025 in view of enroll as direct NBSUB and after that MSTJCO is published and adjusted.
	  	 					String promotPayLevel="";
	  	 					int promotNewBasic=0;
	  	 					String enrollPayLevel="";
	  	 					int enrollNewBasic=0;
	  	 				 List<AuditCage> promotAcList= acRepo.findByFromDateOrderByFromDateToDatePayLevelDesc(do2.getEmployee().getId(), macpsPromotDate);
	  	 				if (promotAcList!=null && promotAcList.size()>0) {
	  						promotPayLevel=promotAcList.get(0).getPayLevel();
	  						promotNewBasic=promotAcList.get(0).getBasicPay();
	  					}	
	  	 				List<AuditCage> enrollAcList= acRepo.findByFromDateOrderByFromDateToDatePayLevelDesc(do2.getEmployee().getId(), do2.getEmployee().getEnrolmentDate());
	  	 				if (enrollAcList!=null && enrollAcList.size()>0) {
	  						enrollPayLevel=enrollAcList.get(0).getPayLevel();
	  						enrollNewBasic=enrollAcList.get(0).getBasicPay();
	  					}	
	  	 				if (promotPayLevel.equals(enrollPayLevel)  && (promotNewBasic==enrollNewBasic)) {
	  	 					payFixationDoneOnMapcsPromot=false;
	  	 				}
	  	 				
	  	 				} else if (macpsPromotDo2.getDescription().equals("MSTHAV")) { // newly added on 26-03-2026 in view of  pay fixation not done but only scale of pay changed  (MSTHAV - EME observation )
	  	 					if (do2.getEmployee()!=null && do2.getEmployee().getAcpnaikDate()!=null && employee.getNaikDate()==null) {
	  	 						payFixationDoneOnMapcsPromot=false;
	  	 					}
	  	 					
	  	 					if (do2.getEmployee()!=null && (do2.getEmployee().getAcphavDate()!=null 
	  	 				 			|| (do2.getEmployee().getHavDate()!=null && do2.getEmployee().getHavDate().before(macpsPromotDo2.getFromDate())))) {
	  	 						payFixationDoneOnMapcsPromot=false;
	  	 					}
	  	 					
	  	 					if (do2.getEmployee()!=null && do2.getEmployee().getAcpnaikDate()!=null && do2.getEmployee().getAcphavDate()!=null) {
	  	 						payFixationDoneOnMapcsPromot=false;
	  	 					}
	  	 				} else {
	  	 					payFixationDoneOnMapcsPromot = true; 
	  	 				}
	  					break; 
	  				}	  					
	  			}
	  			macpsPromotDo2=null;
	  			macpsPromotDate=null;
	  		}
		} else {
			payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(payLevel, newBasic);
					
			/*		Utils.getEntityManager().createQuery(" select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
			 		+ " pm.basicPay>:sevencpcpay order by basicPay asc ").setParameter("paylevel", payLevel)
			 		.setParameter("sevencpcpay", newBasic).getResultList();*/
			
			if (payMatrixList!=null && payMatrixList.size() > 0) {
				newBasic = payMatrixList.get(0).getBasicPay();
			}
			return newBasic;			
		} 
		
		if (macpsPromotDate!=null && macpsPromotDate.equals(DateManager.getSeventhCpcDate())) {
			List<Do2> fix7pcDo2List=null;
			try {
				fix7pcDo2List = do2Repo.findByEmployeeDescriptionAndFromDate(do2.getEmployee().getId(), "FIX7PC", DateManager.getDate("2016-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//		Utils.getEntityManager().createQuery("Select d from Do2 d where d.description='FIX7PC' and d.employee=:emp and d.fromDate='2016-01-01' "
				//	+ " and d.status='V' ").setParameter("emp", do2.getEmployee()).getResultList();
					
			if (fix7pcDo2List!=null && fix7pcDo2List.size()>0) {
				 List<AuditCage> acList= acRepo.findByFromDateOrderByFromDateToDatePayLevelDesc(do2.getEmployee().getId(), macpsPromotDate);
						 
				/*		 Utils.getEntityManager().createQuery("SELECT a from AuditCage a WHERE a.effective=true and a.cancelled=false and a.recordStatus='V' and " +
						 " a.fromDate=:macpPromotDate and a.employee=:emp order by a.fromDate desc, a.toDate desc, a.payLevel desc ").setParameter("emp", do2.getEmployee())
						.setParameter("macpPromotDate", macpsPromotDate).getResultList();*/
				if (acList!=null && acList.size()>0) {
					payLevel=acList.get(0).getPayLevel();
					newBasic=acList.get(0).getBasicPay();
				}	
			}	
		}
		
		if (payFixationDoneOnMapcsPromot==true && optionForFixationExercised==false && macpsPromotDate!=null && macpsPromotDate.after(sixMonthsPriorDate)) {
			return newBasic;
		}
		
		if (optionForFixationExercised==true && macpsPromotDate!=null && payFixationDoneOnMapcsPromot==false) {
	 		List<AuditCage> acList=null;
			if (macpsPromotDate.equals(DateManager.getSeventhCpcDate())) {
				acList= acRepo.findByFromDateGradePayZeroOrderByPayLevelAsc(do2.getEmployee().getId(), macpsPromotDate);
						
				/*		Utils.getEntityManager().createQuery("SELECT a from AuditCage a WHERE a.effective=true and a.cancelled=false and a.recordStatus='V' and " +
						 " a.fromDate=:macpPromotDate and a.employee=:emp and a.gradePay=0 order by a.payLevel asc ")
						 .setParameter("emp", do2.getEmployee())
						.setParameter("macpPromotDate", macpsPromotDate).getResultList();*/
				if (acList!=null && acList.size()>0) {
					payLevel=acList.get(0).getPayLevel();
					newBasic=acList.get(0).getBasicPay();
				}	
			} else {
				acList= acRepo.findPreviousDateEffectiveAc(do2.getEmployee().getId(), macpsPromotDate);
						
			/*			Utils.getEntityManager().createQuery("SELECT a from AuditCage a WHERE a.effective=true and a.cancelled=false and a.recordStatus='V' and " +
						 " a.fromDate<:macpPromotDate and a.employee=:emp order by a.fromDate desc, a.toDate desc ").setParameter("emp", do2.getEmployee())
						.setParameter("macpPromotDate", macpsPromotDate).getResultList();*/
				if (acList!=null && acList.size()>0) {
					List<AuditCage> sameDateAuditCageList=new ArrayList<>();
					if (acList.get(0).getToDate().after(acList.get(0).getFromDate())) {
						payLevel=acList.get(0).getPayLevel();
						newBasic=acList.get(0).getBasicPay();
					} else {						
						Date acFromDtEqualsToDt = acList.get(0).getToDate();
						for (AuditCage ac : acList) {
							if (ac.getFromDate().equals(ac.getToDate()) 
									&& ac.getFromDate().equals(acFromDtEqualsToDt)) {
								sameDateAuditCageList.add(ac);
							} else {
								break;
							}
						}
						if (sameDateAuditCageList!=null && sameDateAuditCageList.size()>0) {
							sortAuditCages(sameDateAuditCageList);
							payLevel=sameDateAuditCageList.get(sameDateAuditCageList.size()-1).getPayLevel();
							newBasic=sameDateAuditCageList.get(sameDateAuditCageList.size()-1).getBasicPay();
						}				
					}				
				}
			}			  
			acList=null;
			System.out.println("wwwwwwwwwww  paylevel "+payLevel+" newbasic " +newBasic +"  macppromotdate " +macpsPromotDate);			 
		}	
		// Following check added to handle cases where before grant of promoted rank increment, another promotion comes with option
		// for pay fixation on increment date of earlier promoted rank - Nasik case - 05.04.2022
		if (payFixationDoneOnMapcsPromot==true && optionForFixationExercised==false && noOfMacpsPromotDo2DuringYear>1) {
			optionForFixationExercised=false;
			payFixationDoneOnMapcsPromot=false;
			do2List=null;
			 
			List<String> descList2=Arrays.asList(new String[] {"PROMOT","MACPS","MSTJCO","MSTHAV","HCOMMN","LATENR"});
			do2List = do2Repo.findByFromDateGreaterThanAndFromDateLessThanAndDescriptionListAndColANotIn(do2.getEmployee().getId(),
																						descList2, sixMonthsPriorDate, do2.getFromDate());
					
				/*	Utils.getEntityManager().createQuery(" SELECT d FROM Do2 d where d.employee=:emp and "
					+ " d.status='V' and d.description in ('PROMOT', 'MACPS', 'MSTJCO', 'MSTHAV','HCOMMN', 'LATENR') AND "
					+ " d.columnA not in ('TSNK') AND d.fromDate > :previousDate AND d.fromDate < :presentDate order by d.fromDate")
					.setParameter("emp",do2.getEmployee()).setParameter("previousDate", sixMonthsPriorDate)
					.setParameter("presentDate", do2.getFromDate()).getResultList();*/
			if (do2List!=null && do2List.size()>0) {
				noOfMacpsPromotDo2DuringYear=do2List.size();
				for(int i=0; i<noOfMacpsPromotDo2DuringYear; i++) {
					macpsPromotDo2=do2List.get(i);
		  			macpsPromotDate=macpsPromotDo2.getFromDate();
		  			if (macpsPromotDo2.getDescription().equals("MACPS"))  {	  			
		  				if (macpsPromotDo2.getToDate()!=null && macpsPromotDo2.getToDate().equals(do2.getFromDate())) {
		  					optionForFixationExercised = true;	  				 	
		  	 			} else {
		  	 				payFixationDoneOnMapcsPromot = true;
		  	 			}
		  				break;	  			 
		  			} else {
		  				if (!Utils.isAcpBeforeCurrentDo2(macpsPromotDo2))  { 
//		 					System.out.println("jjjjjjjjjjjjjjjj  kkkkinside no macps before promot condition ...................... ");
		  					if ((macpsPromotDo2.getToDate()!=null && macpsPromotDo2.getToDate().equals(do2.getFromDate()))
		  							|| (macpsPromotDo2.getDescription().equals("HCOMMN") && macpsPromotDo2.getColumnC()!=null 
		  								&& macpsPromotDo2.getColumnC().length()==8)) {
		  						optionForFixationExercised = true;	 
		  	 				} else {
		  	 					payFixationDoneOnMapcsPromot = true; 
		  	 				}
		  					break; 
		  				}	  					
		  			}
		  			macpsPromotDo2=null;
		  			macpsPromotDate=null;
		  		}
			}
						
			if (payFixationDoneOnMapcsPromot==false && optionForFixationExercised==true && macpsPromotDate!=null) {
				List<AuditCage> acList= acRepo.findPreviousDateEffectiveAc(do2.getEmployee().getId(), do2List.get(0).getFromDate());
						
				/*		Utils.getEntityManager().createQuery("SELECT a from AuditCage a WHERE a.effective=true and a.cancelled=false and a.recordStatus='V' and " +
							 " a.fromDate<:macpPromotDate and a.employee=:emp order by a.fromDate desc, a.toDate desc ").setParameter("emp", do2.getEmployee())
							.setParameter("macpPromotDate", do2List.get(0).getFromDate()).getResultList();*/
				if (acList!=null && acList.size()>0) {
					
					payLevel=acList.get(0).getPayLevel();
					newBasic=acList.get(0).getBasicPay();
				}	
				acList=null;
				System.out.println("wwwwwwwwwww  xxxxxxxxxx paylevel "+payLevel+" newbasic " +newBasic +"  macppromotdate " +macpsPromotDate);			 
			}		
		}
		
		payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(payLevel, newBasic);
				
		/*		Utils.getEntityManager().createQuery(" select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				 		+ " pm.basicPay>:sevencpcpay order by basicPay asc ").setParameter("paylevel", payLevel)
				 		.setParameter("sevencpcpay", newBasic).getResultList();*/
		
		if (payMatrixList!=null && payMatrixList.size() > 0) {
			newBasic = payMatrixList.get(0).getBasicPay();
		}
	 	
		System.out.println("incr 7cpc method ............................... newbasic   "+do2.getFromDate()+"   "+newBasic);
		return newBasic;
	}

	public int get6pcOldBasicPay(Do2 do2, AuditCage auditCage, Date promotDate) { 
	 
		int oldBasic = auditCage.getOldBasicPay();
		int oldGradepay = auditCage.getOldGradePay();
		int xpay=Utils.get6cpcXGroupPay(auditCage);
		Date firstJanuary = null, firstJuly = null;
		Calendar cal = new GregorianCalendar();
		cal.setTime(do2.getFromDate());
		
		try {
			firstJanuary = DateManager.getDate(cal.get(Calendar.YEAR)+"-01-01");
			firstJuly    = DateManager.getDate(cal.get(Calendar.YEAR)+"-07-01");			
		} catch (Exception e) {
			e.printStackTrace( );
		}
	 
		if (!do2.getFromDate().equals(firstJuly)) {
			return auditCage.getOldBasicPay();
		}
		
	/*	oldBasic = (auditCage.getOldBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getOldBasicPay()
				+ (auditCage.getOldGradePay() == null ? 0: auditCage.getOldGradePay()) + xpay,	auditCage.getFromDate()));	
	*/	
				
	//	AuditCage promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(promotDate, do2);
//		System.out.println (  " do2 " +do2.getFromDate() +"  PROMOT Date " +promotDate  +"DESC" +do2.getDescription());
//		System.out.println ( " PROMOT PREVIOUS AUDIT CAGE " +promotPreviousAuditCage.getFromDate()  +" army number " +do2.getArmyNo());
	 	AuditCage promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCageFor6PcOldPay(promotDate, do2);
	 	if (promotPreviousAuditCage != null) {
	 		if (promotPreviousAuditCage.getPayLevel()==null || (promotPreviousAuditCage.getPayLevel()!=null && promotPreviousAuditCage.getPayLevel().equals("0"))) {
				oldBasic = promotPreviousAuditCage.getBasicPay();
				oldGradepay = promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay();				
			} else {
				oldBasic = promotPreviousAuditCage.getOldBasicPay();
				oldGradepay = promotPreviousAuditCage.getOldGradePay() == null ? 0: promotPreviousAuditCage.getOldGradePay();					
			}
	 		
	 	}
	 	return (oldBasic+Utils.getIncrmentRoundedToTen((oldBasic+oldGradepay+ xpay), auditCage.getFromDate()));
		 	 
	 		/*	oldBasic = (auditCage.getOldBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getOldBasicPay()
					+ oldGradepay
					+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),
									promotPreviousAuditCage.getFromDate()));	 
			System.out.println("xxxxxxxxxxxttttttttttttttttt  " + promotPreviousAuditCage.getOldBasicPay()+"  "+promotPreviousAuditCage.getOldGradePay()+"  "
									+xpay+"  "+(Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getOldBasicPay()
											+ (promotPreviousAuditCage.getOldGradePay() == null ? 0: promotPreviousAuditCage.getOldGradePay())
											+ xpay, promotPreviousAuditCage.getFromDate())));
	*/	/*	oldBasic = (promotPreviousAuditCage.getOldBasicPay()+Utils.getIncrmentRoundedToTen(promotPreviousAuditCage.getOldBasicPay()
					+ (promotPreviousAuditCage.getOldGradePay() == null ? 0: promotPreviousAuditCage.getOldGradePay())
					+ xpay,	promotPreviousAuditCage.getFromDate()));	
			
					
		} else {
			
	// This condition should not arise -- 			  
			   Rank rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"ALLCE");
			   if (rank.getRankCode().equals("30")
					   || rank.getRankCode().equals("29")){
				     oldGradepay= 2000;
			   } else if(rank.getRankCode().equals("40")
					   || rank.getRankCode().equals("39")){
				   oldGradepay=2400;
			   } else if (rank.getRankCode().equals("50")
					   || rank.getRankCode().equals("49")){
				   oldGradepay=2800;
			   } else if (rank.getRankCode().equals("60")
					   || rank.getRankCode().equals("59")){
				   oldGradepay=4200;
			   } else if (rank.getRankCode().equals("70")
					   || rank.getRankCode().equals("69")){
				   oldGradepay=4600;
			   }
			 
			   GroupCategory groupCategory= Utils.getGroupCategory(do2,promotDate);
			   if (groupCategory !=null) {
				   if (groupCategory.getSoldierGroup()=='X'){
					   xpay=1400;
				   }
			   }
			   
			   oldBasic = (auditCage.getOldBasicPay()+Utils.getIncrmentRoundedToTen(auditCage.getOldBasicPay()
						+ (oldGradepay)+ (xpay),do2.getFromDate()));
			   
		}*/
	//	return oldBasic;

	}
	
	public boolean checkIncrementDateValidityIn7cpc(Do2 do2) {
	  	boolean validity = true;
		if (do2.getColumnC()!=null && do2.getColumnC().startsWith("POSTPONED")) {
			return validity;
		}		
		
		int rankCode = 0;
		Date maxPromotDate = null;
		Date minPromotDate = null;
		Calendar incrCal = new GregorianCalendar();
		incrCal.setTime(do2.getFromDate());
		int date = incrCal.get(Calendar.DATE);
		int year = incrCal.get(Calendar.YEAR);
		int month = incrCal.get(Calendar.MONTH) + 1 ;
		//.date=1;
		try {
			minPromotDate = DateManager.getDate(year-1, month, date);
		} catch (Exception e1) {
			Utils.handleException(e1);
		}
		
		if (month==1) {
			month=7;
			year--;
		} else if (month==7) {
			month=1;
		} else {
			return false;
		}
		
		try {
			maxPromotDate = DateManager.getDate(year, month, date);
		} catch (Exception e) {
			 
			e.printStackTrace();
		}
		// Checking for promot/upgradation six months prior to increment date in 7th cpc
	//	System.out.println("``````````````````min promot date  "+minPromotDate+" maxPromotdate "+maxPromotDate+" do2fromdt "+do2.getFromDate());
		List<String> descList=Arrays.asList(new String[] {"PROMOT","MACPS","MSTJCO","MSTHAV","HCOMMN"});
		List<Do2> do2List = do2Repo.findByFromDateGreaterThanAndFromDateLessThanEqualToAndDescListOrderByFromDateDesc(do2.getEmployee().getId(),
																									descList,minPromotDate,maxPromotDate);
				
		/*		Utils.getEntityManager().createQuery("Select d from Do2 d where d.employee=:emp and d.status='V' and d.fromDate > :minDate and "
				+ " d.fromDate <= :maxDate and d.description in ('PROMOT', 'MACPS', 'MSTJCO', 'MSTHAV', 'HCOMMN') order by d.fromDate DESC")
				.setParameter("emp", do2.getEmployee()).setParameter("minDate", minPromotDate).setParameter("maxDate", maxPromotDate).getResultList();*/
	  	
		if (do2List!=null && do2List.size()>0) {
	 		for (Do2 d : do2List) {
				rankCode = 0;
				if (d.getDescription().equals("PROMOT") || d.getDescription().equals("HCOMMN") || d.getDescription().equals("MACPS")
						|| d.getDescription().equals("MSTJCO") || d.getDescription().equals("MSTHAV")) {
					String colA = null;
				 
					if (d.getDescription().equals("PROMOT") && d.getColumnA()!=null && (d.getColumnA().contains("SB") || d.getColumnA().contains("PA"))) {
						colA = d.getColumnA().substring(2);
						if (colA.equals("NK")) {
							rankCode = 30;
						} else if (colA.equals("HAV")) {
							rankCode = 40;
						}  else if (colA.equals("SUB")) {
							rankCode = 60;
						} else if (colA.equals("SUBMAJ")) {
							rankCode = 70;
						}					
					} else if (d.getDescription().equals("PROMOT") && d.getColumnA().equals("NBSUB"))  {
						rankCode = 50;
					} else if (d.getDescription().equals("HCOMMN") && d.getColumnA().equals("HONYLT"))  {
						rankCode = 80;
		 			} else if (d.getDescription().equals("HCOMMN") && d.getColumnA().equals("HONYCAPT"))  {
						rankCode = 90;
		 			} else if (d.getDescription().equals("MSTJCO")) {
		 				rankCode = 50;
		 			} else if (d.getDescription().equals("MSTHAV")) {
		 				rankCode = 40;
		 			} else if (d.getDescription().equals("MACPS") && d.getColumnA()!=null) {
		 				colA=d.getColumnA();
		 				if (colA.startsWith("NK")) {
		 					rankCode = 30;
		 				} else if (colA.startsWith("HAV")) {
		 					rankCode = 40;
		 				} else if (colA.startsWith("NSUB")) {
		 					rankCode = 50;
		 				} else if (colA.startsWith("SUB")) {
		 					rankCode = 60;
		 				} else if (colA.startsWith("SMAJ")) {
		 					rankCode = 70;
		 				} else if (colA.startsWith("HONY")) {
		 					rankCode = 80;
		 				}
		 			}
							 
					Rank previousRank = Utils.getPborRank(do2.getArmyNo(), d.getFromDate(), "PAYFIX");
					int previousRankCode = 0;
					
					if (previousRank!=null) {
						previousRankCode = Integer.parseInt(previousRank.getRankCode());
					}
 					
					Date tmpDate = null;
					Date tmpDate1 = null;
					Date incrDateAsperMacpRank = new Date();
									
					if (rankCode==30) {
						if (previousRankCode == 20) {
							tmpDate = d.getFromDate();
						} else if (previousRankCode == 30 && do2.getEmployee().getAcpnaikDate() != null && d.getFromDate().after(do2.getEmployee().getAcpnaikDate())) {
							tmpDate = do2.getEmployee().getAcpnaikDate();
						} else if (previousRankCode == 40 && do2.getEmployee().getAcphavDate() != null && d.getFromDate().after(do2.getEmployee().getAcphavDate())) {
							tmpDate = do2.getEmployee().getAcphavDate();
						} 
					} else if (rankCode==40) {	
						if (previousRankCode == 30) {
							tmpDate = d.getFromDate();
						} else if (previousRankCode == 40 && do2.getEmployee().getAcphavDate() != null && d.getFromDate().after(do2.getEmployee().getAcphavDate())){
							tmpDate = do2.getEmployee().getAcphavDate();
						} else if (previousRankCode == 50 && do2.getEmployee().getAcpnbsubDate() != null && d.getFromDate().after(do2.getEmployee().getAcpnbsubDate())){
							tmpDate = do2.getEmployee().getAcpnbsubDate();
						}				
					} else if (rankCode==50) {
						if (previousRankCode == 40) {
							tmpDate = d.getFromDate();
						} else if (previousRankCode == 50 && do2.getEmployee().getAcpnbsubDate() != null && d.getFromDate().after(do2.getEmployee().getAcpnbsubDate())){
							tmpDate = do2.getEmployee().getAcpnbsubDate();
						} else if (previousRankCode == 60 && do2.getEmployee().getAcpsubDate() != null && d.getFromDate().after(do2.getEmployee().getAcpsubDate())){
							tmpDate = do2.getEmployee().getAcpsubDate();
						}				
					} else if (rankCode==60) {
						if (previousRankCode == 50) {
							tmpDate = d.getFromDate();
						} else if (previousRankCode == 60 && do2.getEmployee().getAcpsubDate() != null && d.getFromDate().after(do2.getEmployee().getAcpsubDate())){
							tmpDate = do2.getEmployee().getAcpsubDate();
						} else if (previousRankCode == 70 && do2.getEmployee().getAcpsubmajDate() != null && d.getFromDate().after(do2.getEmployee().getAcpsubmajDate())){
							tmpDate = do2.getEmployee().getAcpsubmajDate();
						}				
					} else if (rankCode==70) {
						if (previousRankCode == 60) {
							tmpDate = d.getFromDate();
						} else if (previousRankCode == 70 && do2.getEmployee().getAcpsubmajDate() != null && d.getFromDate().after(do2.getEmployee().getAcpsubmajDate())){
							tmpDate = do2.getEmployee().getAcpsubDate();
						} 			
					} else if (rankCode > 70) {
						tmpDate = d.getFromDate();
					}
					
					if (tmpDate != null) {
                        if (!tmpDate.before(DateManager.getSeventhCpcDate())){
                        	tmpDate1 = Utils.getSevenPcNextIncrementDate(tmpDate);
                        } else {
                        	tmpDate1 = Utils.getNextIncrementDate(tmpDate);
                        }
                        Calendar previousIncrCal = new GregorianCalendar();
                		Calendar presentIncrCal = new GregorianCalendar();
                		Calendar currentIncrCal = new GregorianCalendar();

                		previousIncrCal.setTime(tmpDate1);
                		int dd = previousIncrCal.get(Calendar.DATE);
                		int yy = previousIncrCal.get(Calendar.YEAR);
                		int mm = previousIncrCal.get(Calendar.MONTH);
                		
                		presentIncrCal.setTime(do2.getFromDate());                		 
                		int yy1 = presentIncrCal.get(Calendar.YEAR);
                	 
                        if (yy < yy1) {
                			boolean done = false;
                			for (; !done ;) {
                				yy++;
                				if (yy == yy1) {
                					done = true;
                				}
                			}
                		}
                       currentIncrCal = new GregorianCalendar(yy, mm, 1);
                       incrDateAsperMacpRank.setTime(currentIncrCal.getTimeInMillis());
                       if (rankCode>0 && previousRankCode>0) {
                    	   if (rankCode>previousRankCode && incrDateAsperMacpRank.equals(do2.getFromDate())) {
                    		   validity=true;
                    		   break;
                    	   } else if (rankCode<=previousRankCode && incrDateAsperMacpRank.equals(do2.getFromDate())) {
                    		   validity=true;
                    	   } else {
                    		   validity=false;                    	   
                    	   }
                    	   validity=true;
                    	   break;
                       } else {
                    	   validity=false;
                       }
					}				
				}
			}	
		} else {
			List<String> descList2=Arrays.asList(new String[] {"PROFIX","ACPFIX","HAVFIX","LATENR"});
			List<Do2> fixDo2List = do2Repo.findByDescListFromDateEqual(do2.getEmployee().getId(), "V", descList2, maxPromotDate);
					
				/*	Utils.getEntityManager().createQuery("Select d from Do2 d where d.employee=:emp and d.status='V' and "
					+ " d.description in ('PROFIX', 'ACPFIX', 'HAVFIX', 'LATENR') and d.fromDate=:prevIncrDate")
					.setParameter("emp", do2.getEmployee()).setParameter("prevIncrDate", maxPromotDate).getResultList();*/
 
			if (fixDo2List!=null && fixDo2List.size()>0) {
				validity = true;
			} else {
				List<Do2> incrDo2List = do2Repo.findByFromDateBetweenAndFromDateNotEqualToAndColCNotEqualTo(do2.getEmployee().getId(),
						"INCR", minPromotDate,do2.getFromDate(),do2.getFromDate());
						
					/*	Utils.getEntityManager().createQuery("Select d from Do2 d where d.employee=:emp and d.status='V' and d.fromDate between :minDate and "
						+ " :maxDate and d.description='INCR' and d.fromDate!=:do2FromDate and d.columnC!='POSTPONED' order by d.fromDate desc")
						.setParameter("emp", do2.getEmployee()).setParameter("minDate", minPromotDate).setParameter("maxDate", do2.getFromDate()).setParameter("do2FromDate", do2.getFromDate()).getResultList();*/
	 			if (incrDo2List!=null && incrDo2List.size()>0) {
					if (!minPromotDate.equals(DateManager.getSeventhCpcDate()) && !incrDo2List.get(0).getFromDate().equals(minPromotDate)) {
						validity = false;
					}
				}	
			}			
		}
	 	return validity;
	}
	
	
	public Date getAcpDateBeforeCurrentDo2FromDate(Do2 do2) {

		if (do2.getDescription().equals("STEPUP"))
			return null;

		Date revertDate = null;
		Date acpDate = null;
		higherRankAcpAvailable = false;
		higherRankAcpDate = null;
		List<Do2> macpList = null;

		String rankName = do2.getColumnA();

		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());

		/*if (do2.getDescription().equals("HCOMMN")
				&& employee != null
				&& (Integer.parseInt(employee.getActingRank().getRankCode())) == 79) {
			return (employee.getActingRankDate().before(do2.getFromDate()));
		}*/
		
		if (do2.getDescription().equals("HCOMMN") && employee != null) {
			macpList=do2Repo.findByEmployeeIdAndDescriptionAndColumnA(employee.getId(), "MACPS", "HONYGRADE");
					
			/*		Utils.getEntityManager().createQuery("select d from Do2 d where "
					+ " d.employee=:emp and d.description='MACPS' and d.columnA='HONYGRADE' and d.status='V'")
					.setParameter("emp", employee).getResultList();	*/		
			if (macpList!=null && macpList.size() > 0){
				if (macpList.get(0).getFromDate() != null && macpList.get(0).getFromDate().before(do2.getFromDate())
					&& do2.getColumnA().equals("HONYLT")) {
					acpDate = macpList.get(0).getFromDate();
				}
			}	
		} 

		if (do2.getDescription().equals("MSTHAV")) {
			rankName = do2.getDescription().substring(3);
		}

		if (do2.getDescription().equals("MSTJCO")) {
			rankName = "NBSUB";
		}

		if (do2.getColumnA() != null) {
			if (!do2.getColumnA().equals("NBSUB")
					&& !do2.getColumnA().equals("NBRIS")) {
				rankName = do2.getColumnA().substring(2);
			}
			if (rankName != null) {
				if ((rankName.equals("NK") || rankName.equals("LD")  || rankName.equals("SL"))
						&& employee.getAcpnaikDate() != null) {
					acpDate = employee.getAcpnaikDate();
					if (employee.getAcphavDate()!=null) {
						higherRankAcpAvailable = true;
						higherRankAcpDate = employee.getAcphavDate();
					}
				} else if ((rankName.equals("HAV") || rankName.equals("DFR") || rankName.equals("AL"))
						&& employee.getAcphavDate() != null) {
					acpDate = employee.getAcphavDate();
					if (employee.getAcpnbsubDate()!=null) {
						higherRankAcpAvailable = true;
						higherRankAcpDate = employee.getAcpnbsubDate();
					}
				} else if ((rankName.equals("NBSUB") || rankName
						.equals("NBRIS") || rankName.equals("DL")) && employee.getAcpnbsubDate() != null) {
					acpDate = employee.getAcpnbsubDate();
					if (employee.getAcpsubDate()!=null) {
						higherRankAcpAvailable = true;
						higherRankAcpDate = employee.getAcpsubDate();
					}
				} else if ((rankName.equals("SUB") || rankName.equals("RIS") || rankName.equals("CL"))
						&& employee.getAcpsubDate() != null) {
					acpDate = employee.getAcpsubDate();
					if (employee.getAcpsubmajDate()!=null) {
						higherRankAcpAvailable = true;
						higherRankAcpDate = employee.getAcpsubmajDate();
					}
				} else if ((rankName.equals("SUBMAJ") || rankName
						.equals("RISMAJ") || rankName.equals("POL"))
						&& employee.getAcpsubmajDate() != null) {
					acpDate = employee.getAcpsubmajDate();
			 		if (macpList!=null && macpList.size()>0) {
						higherRankAcpAvailable = true;
						higherRankAcpDate = macpList.get(0).getFromDate();
					}
				}
			}
		}

		if (higherRankAcpAvailable) {
			if (acpDate!=null && acpDate.before(do2.getFromDate())
					&& higherRankAcpDate!=null && higherRankAcpDate.before(do2.getFromDate())) {
				acpDate=higherRankAcpDate;
			}
		}		
		
		revertDate = Utils.getRevertDateForPromot(do2);
 		if (acpDate != null && revertDate != null) {
			if (acpDate.after(revertDate)
					&& (!do2.getFromDate().before(acpDate))) {
				return acpDate;
			}
		}

		if (revertDate == null && acpDate != null
				&& !acpDate.after(do2.getFromDate())) {
			return acpDate;
		}

		if (do2.getDescription().startsWith("MST") && acpDate == null
				&& revertDate == null) {
			return null;
		}
		return null;
	}	
	@SuppressWarnings("unchecked")
	public void sortAuditCages(List<AuditCage> auditCageList) {
		
		   
		   for (AuditCage auditCage : auditCageList) {
			   String columnA = null;
	       if (auditCage.getDo2().getColumnA() != null) {
				  columnA = auditCage.getDo2().getColumnA().trim();
			  }	  
			  if (Do2Manager.priorityDescList.contains(auditCage.getDo2().getDescription().trim()) ){
			      auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
			  } else if (columnA != null){
				 auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()+ " " + columnA));
			  } else {
				  auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
			  }
		   }
		   Collections.sort(auditCageList);
	}
	
	public boolean validStopinDo2Exists(Do2 do2) {
		List<Do2> stopinDo2List = do2Repo.getValidStopinDo2ForIncrementDate(do2.getEmployee().getId(), do2.getFromDate());
		if (stopinDo2List!=null && stopinDo2List.size()>0) {
			return true;
		}
		return false;
	}	
}
