package itsdc.dolphin.do2.auditcage;

 

import itsdc.dolphin.models.AuditCage; 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.Rank; 
import itsdc.dolphin.do2.Do2Manager; 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.Date;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController; 
 
@RestController
public class Optcpc extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	
	private static final double SEVEN_CPC_PAY_INDEX=2.57;
	EmployeeServiceDetail esd; 
	private StringBuffer buffer = new StringBuffer();
	private String message;
	boolean validity = true, incorrectOptionDate = false;
	private List <Do2> do2List = null; 
	Employee employee;
	int nqsPeriod;
	Rank rank;
	boolean msthavMstjcoDescription=false;	
	static public final String ENROLLED_AFTER_7CPC_DATE= "AU:Option Not Available For Those Enrolled On Or After 01.01.2016 \n";
	static public final String INVALID_OPTION_DATE = "VL:Option Can Be Exercised During The Period Between 02-01-2016 And 03.05.2017 Only \n";
	static public final String DESC_FROM_DATE_NOT_MATCH = " Does Not Exist On Option Date.\n";
	static public final String INCORRECT_OPTION_DATE = "AU:Incorrectly Exercised Option As Option Date Is Increment Date And Individual Promoted/Upgraded Before Increment Date.\n";
	static public final String SEVENCPC_OPTION_NOT_BENEFICIAL = "Rank On Option Date Is Same Or Lower Than 01.01.2016. Hence, Not Beneficial.\n";
	static public final String PUBLICATION_BEYOND_180_DAYS = "VL:OPTCPC DO2 Can Be Published Within 180 Days Only From Date Of Notification \n";
	static public final String INCORRECT_OPTION = "AU:Incorrectly Exercised Option As Option Date Cannot Be On Subsequent Promotion/Upgradation After 01.01.2016.\n";
	
	@Override
	public boolean isValid(Do2 do2) {		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		if (isDuplicate(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append("Duplicate Do2");
			validity=false;
		}
		
		if (!isEnrolledBefore7CPC(employee)) {
			buffer.append(ENROLLED_AFTER_7CPC_DATE);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		} 
			
		/*if (isDo2PublishedAfter180days(do2)) {
			buffer.append(PUBLICATION_BEYOND_180_DAYS);
			do2.setStatus(Do2Manager.INVALID);
			validity = false;
		}*/
		
		if (!isValidFromDate(do2)) {
			buffer.append(INVALID_OPTION_DATE);
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
			do2Repo.save(do2);	 
			return false;
		}
		
		if (!isFromDateMatchingWithDescInColumn7A(do2, employee)) {
			do2.setStatus(Do2Manager.PENDING);
			validity=false;
			buffer.append("VL:Reason For Option - "+do2.getColumnA()+"-"+DESC_FROM_DATE_NOT_MATCH);				
		}
		
		if (do2.getColumnA().equals("INCR") && do2.getColumnB().equals("SEVENPC")) {
			Rank sevenCpcEffectiveDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getPreviousDate(DateManager.getSeventhCpcDate()), "PAYFIX");
			Rank sevenCpcOptionDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()), "PAYFIX");
			if (sevenCpcEffectiveDateRank!=null && sevenCpcOptionDateRank!=null 
					&& (Integer.parseInt(sevenCpcEffectiveDateRank.getRankCode())!=Integer.parseInt(sevenCpcOptionDateRank.getRankCode()))) {
				buffer.append(INCORRECT_OPTION_DATE);
				validity=false;
			}			
		}
		// Check introduced in Dolphin System Version 2.2019.7.4 dated 25.07.2019.
		if (do2.getColumnA().equals("PROMOT") && do2.getColumnB().equals("SEVENPC")) {
			Rank sevenCpcEffectiveDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getPreviousDate(DateManager.getSeventhCpcDate()), "PAYFIX");
			Rank sevenCpcOptionDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()), "PAYFIX");			
			if (sevenCpcEffectiveDateRank!=null && sevenCpcOptionDateRank!=null 
					&& ((Integer.parseInt(sevenCpcOptionDateRank.getRankCode())-Integer.parseInt(sevenCpcEffectiveDateRank.getRankCode())))!=10) {
		 		System.out.println("Rank on 7th cpc date ...........  "+sevenCpcEffectiveDateRank.getId()+"  "+sevenCpcEffectiveDateRank.getRankCode()+"  "+sevenCpcEffectiveDateRank.getRankName());
		 		System.out.println("Rank on 7th cpc Option date ....  "+sevenCpcOptionDateRank.getId()+"  "+sevenCpcOptionDateRank.getRankCode()+"  "+sevenCpcOptionDateRank.getRankName());
				if (do2.getColumnA().equals("PROMOT") && msthavMstjcoDescription==true) {
					if ((Integer.parseInt(sevenCpcOptionDateRank.getRankCode())-Integer.parseInt(sevenCpcEffectiveDateRank.getRankCode()))>30) {				 
						buffer.append(INCORRECT_OPTION);
						validity=false;
						do2.setStatus(Do2Manager.INVALID);
					}
				} else {
					buffer.append(INCORRECT_OPTION);
					validity=false;
					do2.setStatus(Do2Manager.INVALID);
				}				
			}			
		}
		
		if (!isValidPromotOption(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			validity=false;
		}
		
		if (do2.getColumnC()!=null && do2.getColumnC().equals("SPECIAL")) {
			do2.setRemarks("DO2 REQUIRES TO BE MANUALLY AUDITED FOR ITS VALIDITY W R T RULES 6(I)(i)(ii) OF ARMY PAY RULES 2017");
		}
		
			if (validity) {
			do2.setStatus(Do2Manager.VALID);
			Rank sevenCpcEffectiveDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getPreviousDate(DateManager.getSeventhCpcDate()), "PAYFIX");
			Rank sevenCpcOptionDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()), "PAYFIX");
			if (sevenCpcEffectiveDateRank!=null && sevenCpcOptionDateRank!=null 
					&& (Integer.parseInt(sevenCpcEffectiveDateRank.getRankCode())>=Integer.parseInt(sevenCpcOptionDateRank.getRankCode()))) {
				if (!do2.getColumnA().equals("INCR"))
					do2.setReason(SEVENCPC_OPTION_NOT_BENEFICIAL);
			} else {
				do2.setReason(null);
			}						
		} else {
			message = buffer.toString();
			do2.setReason(message);
		}
			System.out.println("xxxxxxxxxxxxxxxxx validity from optcpc " +validity +"  buffer "+buffer.toString() +"  message " +message) ;
			
		do2Repo.save(do2);
		buffer=new StringBuffer();
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		if (newAuditCage.getDo2().getReason()!=null && newAuditCage.getDo2().getReason().contains(SEVENCPC_OPTION_NOT_BENEFICIAL)) {
			return newAuditCage;
		}
		
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo()); 
		List<PayMatrix> paymatrixList=null; 
		int atcgGradePay=0;
		int sevenCpcBasicPay=0;
		int payMatrixBasicPay=0;
		int atcgBasicPay=0;		 
		String payLevel = null;
	 	Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PAYFIX");
		Rank clspayRank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"ALLCE");
		 
		atcgGradePay = baseAuditCage.getGradePay();
		atcgBasicPay = baseAuditCage.getBasicPay();	

		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"CUR");
		if (groupCategory != null)
			soldierGroup = groupCategory.getSoldierGroup(); 
	 		
		if (atcgGradePay!=0) {
			payLevel = Do2Manager.getPayLevel(atcgGradePay);
		} else {
			payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
			atcgGradePay = Do2Manager.getSevenCpcGradepay(payLevel);
		}
						
		sevenCpcBasicPay = (int)Math.round((atcgBasicPay+atcgGradePay)*SEVEN_CPC_PAY_INDEX);
		
		paymatrixList = matrixRepo.findByPayLevelAndGradePayAndBasicPayGreaterThanEqualOrderByBasicPayAsc(payLevel,
																	atcgGradePay, sevenCpcBasicPay);
				
		/*		Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ " pm.gradePay=:gradepay and pm.basicPay>=:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", payLevel)
				.setParameter("gradepay", atcgGradePay)
				.setParameter("sevencpcpay", sevenCpcBasicPay)
				.getResultList();*/
		
		if (paymatrixList.size() > 0) {
			payMatrixBasicPay = paymatrixList.get(0).getBasicPay();
		}			
	
		if (payMatrixBasicPay>0) {
			newAuditCage.setBasicPay(payMatrixBasicPay);		
			newAuditCage.setGradePay(0);
			newAuditCage.setDp(0);
			newAuditCage.setPayLevel(payLevel);
	 		newAuditCage.setOldGradePay(atcgGradePay);
			newAuditCage.setOldClassPay(baseAuditCage.getClassPay());
			if(Integer.parseInt(clspayRank.getRankCode()) > 49 ) {
				newAuditCage.setClassPay(0);
			}		
		}
	 		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)  
				&& baseAuditCage.getEmployee().getEnrolmentDate()!=null 
				&& !baseAuditCage.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())
				&& baseAuditCage.getEmployee().getEnrolmentDate().equals(newAuditCage.getDo2().getFromDate())) {
		 			
			if (rank!=null) {
				payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
				atcgGradePay = Do2Manager.getSevenCpcGradepay(payLevel);
				
				sevenCpcBasicPay = (int)Math.round((atcgBasicPay+atcgGradePay)*SEVEN_CPC_PAY_INDEX);
				if (payLevel!=null && atcgGradePay>0) {
					paymatrixList = matrixRepo.findByPayLevelAndGradePayOrderByMatrixLevelAsc(payLevel, atcgGradePay);
							
							
				/*			Utils.getEntityManager().createQuery( 
						 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
						+ " pm.gradePay=:gradepay order by pm.matrixLevel asc ")
						.setParameter("paylevel", payLevel)
						.setParameter("gradepay", atcgGradePay) 
						.getResultList();*/
				}
				
				if (paymatrixList.size() > 0) {
					payMatrixBasicPay = paymatrixList.get(0).getBasicPay();
				}
				
				if (payMatrixBasicPay>0) {
					newAuditCage.setBasicPay(payMatrixBasicPay); 		
					newAuditCage.setGradePay(0);
					newAuditCage.setDp(0);
					newAuditCage.setPayLevel(payLevel);
					newAuditCage.setOldGradePay(atcgGradePay);
					newAuditCage.setDp(0);
					newAuditCage.setMsPay(5200);
					newAuditCage.setXGroupPay(0);
				}				
			}  			
		}		
		
		if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==2000) {
			newAuditCage.setMsPay(5200);
		} else {
			if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==6000) {
				newAuditCage.setMsPay(15500);
				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && baseAuditCage.getOldGradePay()<5400
						&& baseAuditCage.getEmployee().getEnrolmentDate()!=null 
						&& !baseAuditCage.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
					newAuditCage.setMsPay(5200);
				}
			}			
		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			newAuditCage.setMsPay(0);
		}
		
	 	if (soldierGroup=="X" && baseAuditCage.getXGroupPay()!=null && baseAuditCage.getXGroupPay()==1400) {
			newAuditCage.setXGroupPay(3600);
		} else if (baseAuditCage.getXGroupPay()!=null && baseAuditCage.getXGroupPay()==1400) {
			newAuditCage.setXGroupPay(3600);
		}

	 	Calendar cal = new GregorianCalendar();
	 	cal.setTime(newAuditCage.getFromDate());
	 	Calendar firstJan = new GregorianCalendar();
	 	firstJan.setTime(DateManager.getSeventhCpcDate());
	 	Calendar firstJul = new GregorianCalendar();
	 	try {
			firstJul.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+"-"+(Calendar.JULY+1)+"-01"));
			firstJan.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+"-"+(Calendar.JANUARY+1)+"-01"));
		} catch (Exception e) {
			Utils.handleException(e);
		}
	 	if (!(newAuditCage.getFromDate().after(firstJan.getTime()) && newAuditCage.getFromDate().before(firstJul.getTime()))) {
	 		newAuditCage.setOldBasicPay(baseAuditCage.getBasicPay());
	 	} else {
	 		newAuditCage.setOldBasicPay(AuditCageHelper.getPreviousDateAuditCageFor6PcOldPay(baseAuditCage.getFromDate(), baseAuditCage.getDo2()).getBasicPay());	 		  		
	 	}
		
	 	newAuditCage.setOldGradePay(baseAuditCage.getGradePay());		
 		
		 
		return newAuditCage; 
	}
	
	@Transactional
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {		
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		return Do2Manager.SUCCESS;
	}

	public boolean isEnrolledBefore7CPC(Employee employee) {
		return (employee.getEnrolmentDate()!=null && employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())); 
	}
	
	/*public boolean isDo2PublishedAfter180days(Do2 do2) {
		java.util.Date lastDate = null;
		try {
			lastDate = DateManager.getDate("2017-10-29");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		return (lastDate!=null && do2.getDo2Date().after(lastDate)); 
	}*/
		
		
	public boolean isValidFromDate(Do2 do2) {		 
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) 
				 && !do2.getFromDate().after(DateManager.getSeventhCpcNotificationDate())) {
			 return true;
		 }
		 return false;
	}

	 public boolean isFromDateMatchingWithDescInColumn7A(Do2 do2, Employee employee) {	
		do2List = null;
		
		// Checking whether column7A of Optcpc Do2 with Description of original Do2
			
		if (do2.getColumnA().trim().equals("MACPS")) {
			do2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V","MACPS",do2.getFromDate());
					
					
			/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
					"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
					"do2.description='MACPS' AND do2.status='V' ").setParameter("employee", employee) 
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}			
		} else if (do2.getColumnA().trim().equals("INCR")) {
			do2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V","INCR",do2.getFromDate());
					
			/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
					"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
					"do2.description='INCR' AND do2.status='V' ").setParameter("employee", employee) 
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}
		} else if (do2.getColumnA().trim().equals("PROMOT")) {	
			List<String> descList=Arrays.asList(new String[] {"MSTJCO","MSTHAV","PROMOT","HCOMMN","LATERN"});
			do2List = do2Repo.findByDescListFromDateEqual(employee.getId(),"V",descList,do2.getFromDate());
					
					
			/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
					"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
					"do2.description in ('MSTJCO', 'MSTHAV', 'PROMOT', 'HCOMMN', 'LATENR') AND do2.status='V' ")
					.setParameter("employee", employee).setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}
			if (do2List.get(0).getDescription().startsWith("MST")) {
				msthavMstjcoDescription=true;
			}
		}
		
		return true;		 
	 }
	 
	public boolean isDuplicate(Do2 do2, Employee employee) {
		List<Do2> dupDo2List = do2Repo.findByDescIdNotEqualAndStatusValidAndPending(employee.getId(),"OPTCPC",
																							do2.getId());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='OPTCPC' "
		 		+ " and d.employee=:emp and status in ('V', 'P') and d.id!=:do2Id")
				.setParameter("emp", employee).setParameter("do2Id", do2.getId()).getResultList();*/
		return (dupDo2List!=null && dupDo2List.size()>0);
	}
	
	public boolean isValidPromotOption(Do2 do2, Employee employee) {
		//Since MACPS handled above, PROMOT w r t other description between 7cpc date and option date being verified below
			if (do2.getColumnA().equals("PROMOT")) {
				List<String> descList=Arrays.asList(new String[] {"MSTJCO","MSTHAV","PROMOT","HCOMMN","MACPS","LATERN"});
				
					do2List = do2Repo.findByDescListAndFromDateGreaterThanEqualAndFromDateLessThan(employee.getId(),
							descList, DateManager.getSeventhCpcDate(), do2.getFromDate());
							
					/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
							"do2.employee= :employee AND do2.fromDate>=:firstJan and do2.fromDate<:fromDate AND " +
							"do2.description in ('MSTJCO', 'MSTHAV', 'PROMOT', 'HCOMMN', 'MACPS', 'LATENR') AND do2.status='V' ")
							.setParameter("employee", employee) 
							.setParameter("firstJan", DateManager.getSeventhCpcDate()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
					if (do2List==null || (do2List!=null && do2List.size()==0)) {
						return true;
					}	
					do2List = do2Repo.findByFromDateGreaterThanEqualAndFromDateLessThan(employee.getId(),"MACPS",
																		DateManager.getSeventhCpcDate(), do2.getFromDate());
							
				/*			Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
							"do2.employee= :employee AND do2.fromDate>=:firstJan and do2.fromDate<:fromDate AND " +
							"do2.description='MACPS' AND do2.status='V' ")
							.setParameter("employee", employee)
							.setParameter("firstJan", DateManager.getSeventhCpcDate()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
					if (do2List.size()>0) {
						buffer.append("MACPS Exists Before Option Date. Option To Be Exercised On MACPS Date.\n");
						return false;
					}
					do2List = do2Repo.findByFromDateGreaterThanEqualAndFromDateLessThan(employee.getId(),
							"MST%",DateManager.getSeventhCpcDate(), do2.getFromDate());
							
					/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
							"do2.employee= :employee AND do2.fromDate>=:firstJan and do2.fromDate<:fromDate AND " +
							"do2.description like 'MST%' AND do2.status='V' ")
							.setParameter("employee", employee)
							.setParameter("firstJan", DateManager.getSeventhCpcDate()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
				
					if (do2List.size()>0) {
						List<String> descList1=Arrays.asList(new String[] {"MSTJCO","MSTHAV","PROMOT"});
						List<Do2> originalPromotDo2List = do2Repo.findByDescListFromDateEqual(employee.getId(),
																					"V",descList1,do2.getFromDate());
								
						/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
								"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
								"do2.description in ('MSTJCO', 'MSTHAV', 'PROMOT')  AND do2.status='V' ")
								.setParameter("employee", employee).setParameter("fromDate", do2.getFromDate()).getResultList();*/
						if (originalPromotDo2List.size()>0) {
							if (originalPromotDo2List.get(0).getDescription().startsWith("MST")) {
								buffer.append("MSTJCO / MSTHAV Exists Before Option Date. Option To Be Exercised On That Date.\n");
								return false;	
							}
							Rank optionDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()),"PAYFIX");
							Rank rankPriorToOption = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
							if (optionDateRank!=null && rankPriorToOption!=null 
									&& (Integer.parseInt(optionDateRank.getRankCode())<=Integer.parseInt(rankPriorToOption.getRankCode()))) {
								buffer.append(SEVENCPC_OPTION_NOT_BENEFICIAL+"\n");
								return false;
							}
						}				  
					}
					
					do2List = do2Repo.findByFromDateGreaterAndFromDateLessThan2(employee.getId(),"LATENR",
																			DateManager.getSeventhCpcDate(), do2.getFromDate());
							
						/*	Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
							"do2.employee= :employee AND do2.fromDate>:firstJan and do2.fromDate<:fromDate AND " +
							"do2.description in ('LATENR') AND do2.status='V' ")
							.setParameter("employee", employee) 
							.setParameter("firstJan", DateManager.getSeventhCpcDate()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
					
					if (do2List.size()>0) {
						buffer.append("LATENR Exists Before Option Date. Option To Be Exercised On That Date.\n");
						return false;
					}
				}
				 
		return true;
	}	 
}
