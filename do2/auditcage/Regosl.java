package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;


@RestController
public class Regosl extends AuditCageBase implements Do2AuditCageProcessor,
				 java.io.Serializable {

	 
	Do2 stopAllDo2,restoreDo2;
	String description;
	static public String INVALID_REFDO2 = "AU:Incorrect Reference Do2.\n ";
	static public String INVALID_LRA = "AU:LRA days not matching with regularisation period.\n";
	static public String INVALID_COLUMNA = "AU:Col.7A should contain leave to which regularised\n";
	static public String INVALID_COLUMNB = "AU:Col.7B should contain year/no debit/nqs\n";
	static public String REGAL_ONE_DAY = "AU:Regularisation for one day is not valid.\n";
	static public String REJOIN_ALREADY_CANCELLED = "AU:Rejoining DoII already cancelled. \n";
	static public String TODATE_REQUIRED = "VL:ToDate Cannot Be Blank. \n";
	static public String INVALID_TODATE = "VL:Invalid ToDate. \n";
//	static public String INVALID_PERIOD = "VL:Beyond Covid19 Lockdown Period - 21032020 To 31082020. \n";
	static public String INVALID_PERIOD = "VL:Beyond Covid19 Lockdown Period - 21032020 To 31082020 OR 26042021 To 31052021. \n";
	static public String INVALID_FROMDATE = "VL:Incorrect FromDate, not immediately after referred Do2 Todate. \n";
	
	private StringBuffer buffer = new StringBuffer();
	List<String> leaveList = Arrays.asList("CL", "AL", "PAL", "BAL", "RL", "SL", "PRPLVE", "PATLVE", "JRNYPD", "ADVAL", "EXTLVE", "EXTNAL", "EXTNCL", "REGOSL");
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		Date revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append(INVALID_REFDO2);
		}	
		
		if (do2.getColumnA()!=null && do2.getColumnA().trim().startsWith("SPL") 
				&& do2.getColumnB()!=null && do2.getColumnB().trim().startsWith("NO")) {
			try {
			//	if (do2.getFromDate().before(DateManager.getDate("2020-03-21"))
			//			|| do2.getToDate().after(DateManager.getDate("2020-08-31"))) {
				if (do2.getFromDate().before(DateManager.getDate("2020-03-21"))
						|| do2.getFromDate().after(DateManager.getDate("2021-05-31"))) {
					validity = false;
					buffer.append(INVALID_PERIOD);
				} else if (do2.getToDate().before(DateManager.getDate("2020-03-21"))
						|| do2.getToDate().after(DateManager.getDate("2021-05-31"))) {					
					validity = false;
					buffer.append(INVALID_PERIOD);
				} else if (do2.getFromDate().after(DateManager.getDate("2020-08-31"))
						&& do2.getToDate().before(DateManager.getDate("2021-04-26"))) {					
					validity = false;
					buffer.append(INVALID_PERIOD);								
				} else if (do2.getFromDate().after(DateManager.getDate("2020-08-31"))
						&& do2.getFromDate().before(DateManager.getDate("2021-04-26"))) {					
					validity = false;
					buffer.append(INVALID_PERIOD);
				} else if (do2.getToDate().after(DateManager.getDate("2020-08-31"))
						&& do2.getToDate().before(DateManager.getDate("2021-04-26"))) {					
					validity = false;
					buffer.append(INVALID_PERIOD);
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
			
		//	System.out.println("........ do2fromdt " +do2.getFromDate()+" stopalldo2 todate  " +stopAllDo2.getToDate()+"  prevtotodate "+DateManager.getPreviousDate(do2.getFromDate()));
			
			if (validity && stopAllDo2!=null && do2.getId()==newDo2.getId()) { // Added on 11.02.2021 based on PAO/DSC observation
				 if (stopAllDo2.getToDate()!=null && stopAllDo2.getToDate().equals(DateManager.getPreviousDate(do2.getFromDate()))) {
					 validity = true;
				 } else {
					 validity = false;
					 buffer.append(INVALID_FROMDATE);
				 }
			}			
		}
		
		if (do2.getDo2Date().before(revised_appxj_impl_date)) {
			;
		} else if (!isValidColumnA(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNA);
		}
		if (do2.getDo2Date().before(revised_appxj_impl_date)) {
			;
		} else if (!isValidColumnB(do2)){
			validity = false;
			buffer.append(INVALID_COLUMNB);
		}
		
		if (do2.getToDate()==null) {
			validity = false;
			buffer.append(TODATE_REQUIRED);
		} 
		
		if (!isValidToDate(do2)) {
			validity = false;
			buffer.append(INVALID_TODATE);
		}
		
		if (do2.getLra() == 1) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(REGAL_ONE_DAY);
			return false;
		}		
		if (!isValidLra(do2)) {
			validity = false;
			buffer.append(INVALID_LRA);
		}
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);			
		} else {
			do2.setStatus(Do2Manager.VALID);
			
			if (do2.getColumnA().trim().toUpperCase().startsWith("SPL") && do2.getColumnB().trim().toUpperCase().startsWith("NO")) {
				generateCeaseDo2(do2);
			}
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	public boolean isValidLra(Do2 do2) {	
		if (do2.getFromDate()!=null && do2.getToDate()!=null && do2.getLra()!=null && do2.getLra()>1) {
			double do2Days = DateManager.getNoOfDays(do2.getFromDate(), do2.getToDate());
			return (do2.getLra() == do2Days);	
		}
		return false;			
	}

	public boolean isValidColumnA(Do2 do2) {
		List<String> col7AList = Arrays.asList("AL","SPLLVE","SL","EOL");
		if (do2.getColumnA()!=null && col7AList.contains(do2.getColumnA())) {
			return true;
		}
		return false;
	}
	
	public boolean isValidColumnB(Do2 do2) {
		if (do2.getColumnB() == null){
			return false;
		} else if ((do2.getColumnB().equals("NO DEBIT") || do2.getColumnB().equals("NODEBIT")) && 
			(do2.getColumnA().equals("SL")) || do2.getColumnA().equals("SPLLVE")){
			return true;
		} else if (do2.getColumnB().contains("-") || ((do2.getColumnB().length()==4 && Utils.isNumeric(do2.getColumnB())) && do2.getColumnA().equals("AL"))){
			return true;
		} else if (do2.getColumnB().contains("NQS") && do2.getColumnA().equals("EOL")){
			return true;
		}
		return false;
	}
	
	public boolean isValidRefDo2(Do2 do2) {
		List<Do2> stopAllDo2List = Utils.getReferenceDo2List(do2);
		if(stopAllDo2List==null || (stopAllDo2List!=null && stopAllDo2List.size()==0)) {
			return false;
		} else {
			for (Do2 d : stopAllDo2List) {
				if (d.getColumnA()!=null &&  d.getTranscriptionType().equals("S") && (d.getColumnA().equals("CEASED") || d.getColumnA().equals("GRANTED"))) {
					continue;
				} else {
					stopAllDo2 = d;
					break;
				}
			}
		}
					
		if (stopAllDo2 == null)
			return false;	
		String refDo2No = do2.getRefDo2No();
		
		if (refDo2No==null || (refDo2No!=null && refDo2No.trim().length()!=15)) {
			return false;
		}		
		
		if (refDo2No != null && refDo2No.trim().length()==15) {
			String[] refDo2Parts = Utils.getRefDo2Parts(refDo2No);
			Calendar cal = new GregorianCalendar();
			cal.setTime(do2.getDo2Date());			
			if (refDo2Parts[3]!=null) {
				if (Integer.parseInt(refDo2Parts[3])==cal.get(Calendar.YEAR)) {
					if (Integer.parseInt(refDo2Parts[1])<Integer.parseInt(do2.getDo2No())) {
						return true;
					} else if (Integer.parseInt(refDo2Parts[2])<Integer.parseInt(do2.getDo2ItemNo())) {
						return true;
					} else {
						return false;
					}
				} else if (Integer.parseInt(refDo2Parts[3])<cal.get(Calendar.YEAR)) {
					return true;
				} else {
					return false;
				}				
			} else {
				return false;
			}
		} 
	
		if ((stopAllDo2.getDescription().equals(Do2Manager.OSL)  ||
			 stopAllDo2.getDescription().equals(Do2Manager.HOSPITAL)  ||
			 stopAllDo2.getDescription().equals(Do2Manager.AWL)) &&
			 stopAllDo2.getFromDate().equals(do2.getFromDate())) {
                return true;
		} else if ((do2.getColumnB().equals("NO DEBIT") || do2.getColumnB().equals("NODEBIT")) && do2.getColumnA().equals("SPLLVE")) {				
					if (stopAllDo2.getDescription().equals("CL") || stopAllDo2.getDescription().endsWith("AL")
							|| stopAllDo2.getDescription().equals("RL") || stopAllDo2.getDescription().equals("SL")
							|| stopAllDo2.getDescription().equals("PRPLVE") 
							|| stopAllDo2.getDescription().equals("PATLVE")
							|| stopAllDo2.getDescription().startsWith("EXTN")
							|| stopAllDo2.getDescription().equals("JRNYPD")) {
						return true;
					} else {
						List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
			 			if (refDo2List!=null && refDo2List.size()>0) {
							for (Do2 refDo2 : refDo2List) {
		 						if (refDo2.getDescription().equals("CL") || refDo2.getDescription().endsWith("AL")
										|| refDo2.getDescription().equals("RL") || refDo2.getDescription().equals("SL") 
										|| refDo2.getDescription().equals("PRPLVE")
										|| refDo2.getDescription().equals("PATLVE")
										|| refDo2.getDescription().startsWith("EXTN")
										|| refDo2.getDescription().equals("JRNYPD")) {
		 							return true;
								}	
							}
						}
		 				return false;
					}				 
		} else {
        	  return false;
		}
	}
	 
	
	
	public boolean isValidToDate(Do2 do2) {		
		if (do2.getToDate()!=null && do2.getToDate().after(do2.getFromDate())) {
			return true;
		}
		return false;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		if (newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("EOL")) {
			newAuditCage.setAbsentFlag(true);
		} 
		if (newAuditCage.getDo2().getColumnA() != null && (((newAuditCage.getDo2().getColumnA().equals("SPLLVE") || newAuditCage.getDo2().getColumnA().equals("SL"))
				&& (newAuditCage.getDo2().getColumnB().equals("NO DEBIT") || newAuditCage.getDo2().getColumnB().equals("NODEBIT"))))) {
			if (newAuditCage.getAbsentFlag()==false && newAuditCage.getDeputationFlag()==false) {
				newAuditCage.setAbsentFlag(false);	
			}		
		} 		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
//		if (do2.getColumnA().trim().toUpperCase().startsWith("SPL") && do2.getColumnB().trim().toUpperCase().startsWith("NO")) {
	//		System.out.println("-----------------------");
	//		generateCeaseDo2(do2);	 //Method called from isvalid -- updated on 22.02.2021.
//		}		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	private void generateCeaseDo2(Do2 do2) {
		Do2 referredDo2 = stopAllDo2;
		
		if (referredDo2!=null && (referredDo2.getDescription().trim().startsWith("ADV") || referredDo2.getDescription().trim().startsWith("EXT") ||
				leaveList.contains(referredDo2.getDescription()))) {
			List<Do2> stopAllDo2List = Utils.getReferenceDo2List(do2);
			if(stopAllDo2List==null || (stopAllDo2List!=null && stopAllDo2List.size()==0)) {
				List<Do2> do2List = do2Repo.findByToDateEqualAndDescListOrderByFromDateDescToDateDesc(do2.getEmployee().getId(),"V",
																		DateManager.getPreviousDate(referredDo2.getFromDate()),leaveList);
						
						
		/*				Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status='V' and "
						+ " d.toDate=:td and description in (:leaveList) order by d.fromDate desc, d.toDate desc ").setParameter("emp", do2.getEmployee())
						.setParameter("td", DateManager.getPreviousDate(referredDo2.getFromDate())).setParameter("leaveList", leaveList).getResultList();*/
			
				if (stopAllDo2List!=null && stopAllDo2List.size()>0) {
					referredDo2 = stopAllDo2List.get(0);
					stopAllDo2 = referredDo2;
				}
			} else {
				for (Do2 d : stopAllDo2List) {
					if (d.getColumnA()!=null &&  d.getTranscriptionType().equals("S") && (d.getColumnA().equals("CEASED") || d.getColumnA().equals("GRANTED"))) {
						continue;
					} else {
						stopAllDo2 = d;
						referredDo2 = d;
						break;
					}
				}
			}
		}
		
//		System.out.println("[[[[[[[[[[[[[[ referreddo2 id " +referredDo2.getId()+"   stopalldo2 id " +stopAllDo2.getId());
 		
 		AuditCage ac = Utils.getAuditCage(referredDo2);
		Calendar cal = new GregorianCalendar(), tocal = new GregorianCalendar();	 	
 		if (ac!=null) {
 			cal.setTime(ac.getFromDate());
 			tocal.setTime(do2.getToDate());
 			int date = cal.get(Calendar.DATE);
 			int month = cal.get(Calendar.MONTH);
 			int year = cal.get(Calendar.YEAR);
 	 		Date ceaseDate = ac.getFromDate();
 			if (date>1) {
 				month++;
 				cal.set(cal.get(Calendar.YEAR), month, 1);
 				ceaseDate = cal.getTime(); 				
 	//		 	System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxx ano ceasedate "+do2.getArmyNo()+"  "+ceaseDate);
	//		 	System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx ano cal "+do2.getArmyNo()+"  "+cal.getTime());				
 			}
 			date = tocal.get(Calendar.DATE);
 			month = tocal.get(Calendar.MONTH);
 			year = tocal.get(Calendar.YEAR);
 	//	 	 System.out.println("zzzzzzzzzzzzzzzzzzzzzzzzzzz do2todate "+do2.getToDate()+ "   tocal time "+tocal.getTime());
	//		  System.out.println("vvvvvvvvvvvvv "+ cal.get(Calendar.MONTH)+"  "+ cal.get(Calendar.YEAR) +"  "+cal.get(Calendar.DATE)+"  month "+month);
			if (DateManager.getNoOfDaysInMonth(month+1, year)==date) {
				month++;
	//			  System.out.println("UUUUUUUUUUUUUUUUUUUUUUUU  cal month " +cal.get(Calendar.MONTH)+"  "+month);				
				tocal.set(Calendar.MONTH, month);
	//			  System.out.println("WWWWWWWWWWWWWWWWWWWW  cal month " +cal.get(Calendar.MONTH)+"  "+month);
	 		} 
			tocal.set(year, month, 1);	
			if (!cal.getTime().equals(tocal.getTime())) {
				if (ac.getTransport()>0) {
					insertCeasedDo2IntoTable(do2, "TPTL", tocal, ceaseDate);			 
					insertGrantedDo2IntoTable(do2, "TPTL", tocal, referredDo2.getFromDate());		
				} else {
	//				System.out.println("************ emp "+do2.getEmployee().getArmyNo()+" ceasedate "+ceaseDate+" grantdate "+tocal.getTime()+" description "+description);
					List<Do2> do2List = do2Repo.findByFromDateGreaterThanAndFromDateLessThanEqualAndDescLikeAndColumnAAndMultipleStatus(do2.getEmployee().getId(),
																											ceaseDate, tocal.getTime(),"TPT"+'%',"GRANTED");
							
						//	Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status in ('P', 'V') and "
					//			+ " d.fromDate>:ceasedate and d.fromDate<=:grantdate and d.description like  :desc and d.columnA='GRANTED' order by d.fromDate ").setParameter("emp", do2.getEmployee())
						//		.setParameter("ceasedate", ceaseDate).setParameter("grantdate", tocal.getTime()).setParameter("desc", "TPT"+'%' ).getResultList();
	//				System.out.println(".............. do2list size  " +do2List.size());
					if (do2List!=null && do2List.size()>0) {
						for (Do2 d : do2List) {
	//						System.out.println(":::::::::::::::::::::::::: .do2 id " +d.getId()+"   "+d.getStatus());
							d.setStatus("C");
						//	Utils.getEntityManager().merge(d);
					//		Utils.getEntityManager().flush();
	//						System.out.println(":::::::::::::::::::::::::: .do2 id ;;;;;;;;;;;  " +d.getId()+"   "+d.getStatus());
						}
					}			
				}						 			
			}
 	//			  System.out.println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy do2.armyno "+do2.getArmyNo()+"  cal "+cal.getTime()+"   tocal "+tocal.getTime());
 		}
 		double absentDays = DateManager.getNoOfDays(referredDo2.getFromDate(), do2.getToDate());
 		
 		if (absentDays>14 && absentDays<22) {
 			if(ac!=null) {
 				List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(ac.getId());
 				//	Utils.getEntityManager().createQuery("select aca from AuditCageAllowance aca where aca.auditCage=:ac")
				//		.setParameter("ac", ac).getResultList();
 			for (AuditCageAllowance aca : acaList) {
 				if (Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyFourteenDays().contains(aca.getDo2().getDescription())) {
 					insertCeasedDo2IntoTable(do2, aca.getDo2().getDescription(), tocal, ac.getFromDate());
 				}
 			}	
 			}
 			
 		} else if (absentDays>21) {
 			if(ac!=null) {
 				List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(ac.getId());
					
 	 			//		Utils.getEntityManager().createQuery("select aca from AuditCageAllowance aca where aca.auditCage=:ac")
 				//			.setParameter("ac", ac).getResultList();
 	 			for (AuditCageAllowance aca : acaList) {
 	 				if (aca.getDo2()!=null && Do2Manager.getStopFieldAllowanceDescriptionsOnTemporaryDutyTwentyoneDays().contains(aca.getDo2().getDescription())) {
 	 					insertCeasedDo2IntoTable(do2, aca.getDo2().getDescription(), tocal, ac.getFromDate());
 	 				}
 	 			}	
 			}
 		
 		} 		 
	}
	
	public void insertCeasedDo2IntoTable(Do2 do2, String description, Calendar grantcal, Date ceaseDate) {	
		List<Do2> do2List = do2Repo.findByFromDateEqualAndDescriptionAndColumnAAndStatus(do2.getEmployee().getId(),
																						ceaseDate, description, "CEASED");
				
			//	Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status in ('P', 'V') and "
		//	+ " d.fromDate=:fromdate and d.description=:desc and d.columnA='CEASED' ").setParameter("emp", do2.getEmployee())
			//	.setParameter("fromdate", ceaseDate).setParameter("desc", description).getResultList();
	//	System.out.println("1111111111111111111 do2list size ..... " +do2List.size());
		if (do2List!=null && do2List.size()>0) {
			do2List = null;
			do2List = do2Repo.findByFromDateGreaterThanEqualAndFromDateLessThanAndDescAndColAAndStatus(do2.getEmployee().getId(), ceaseDate,
																							grantcal.getTime(), description, "GRANTED");
					
				//	Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status in ('P', 'V') and "
				//		+ " d.fromDate>=:ceasedate and d.fromDate<:grantdate and d.description=:desc and d.columnA='GRANTED' ").setParameter("emp", do2.getEmployee())
				//		.setParameter("ceasedate", ceaseDate).setParameter("grantdate", grantcal.getTime()).setParameter("desc", description).getResultList();
	//		System.out.println(".............. do2list size  " +do2List.size());
			if (do2List!=null && do2List.size()>0) {
				for (Do2 d : do2List) {
					d.setStatus("C");
				//	Utils.getEntityManager().merge(d);
				//	Utils.getEntityManager().flush();
				}
			}			
			return;
		}
		Do2 ceasedDo2 = new Do2();			 
		ceasedDo2.setBatchNo(49000);
		ceasedDo2.setEmployee(do2.getEmployee());		
		ceasedDo2.setArmyNo(do2.getArmyNo());
		ceasedDo2.setCheckDigit(do2.getCheckDigit());
		ceasedDo2.setUnitCode(do2.getUnitCode());
		ceasedDo2.setUnit(do2.getUnit());
		ceasedDo2.setDo2Group("IA");
		ceasedDo2.setDo2Type(do2.getDo2Type());
		ceasedDo2.setDo2No(do2.getDo2No());
		ceasedDo2.setDo2ItemNo(do2.getDo2ItemNo());
		ceasedDo2.setDo2Date(do2.getDo2Date());
		ceasedDo2.setDescription(description);
		ceasedDo2.setFromDate(ceaseDate);
		ceasedDo2.setColumnA(Do2Manager.CEASED);
 		ceasedDo2.setStatus(Do2Manager.PENDING);
		ceasedDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		ceasedDo2.setRemarks("COVID19 Special Leave - REGOSL");
		ceasedDo2.setAuditCage(true);
		ceasedDo2.setQe(Utils.getCurrentQuarter());
		ceasedDo2.setInputQe(Utils.getCurrentQuarter());			
		ceasedDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
	//	Utils.getEntityManager().persist(ceasedDo2);
	//	Utils.getEntityManager().flush();
	}
	public void insertGrantedDo2IntoTable(Do2 do2, String description, Calendar grantDate, Date referredDo2FromDate) {	
		List<Do2> do2List = do2Repo.findByFromDateEqualAndDescriptionAndColumnAAndStatus(do2.getEmployee().getId(),
																					grantDate.getTime(), description, "GRANTED");
				
				
		/*		Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status in ('P', 'V') and "
				+ " d.fromDate=:fromdate and d.description=:desc and d.columnA='GRANTED' ").setParameter("emp", do2.getEmployee())
				.setParameter("fromdate", grantDate.getTime()).setParameter("desc", description).getResultList();*/
		if (do2List!=null && do2List.size()>0) {
			return;
		}
		String ceasedMe = referredDo2FromDate.toString().substring(5,7) +"/"+ referredDo2FromDate.toString().substring(0,4);
 		String month = String.valueOf(grantDate.get(Calendar.MONTH));
		if (Integer.valueOf(month)<10) {
			month="0"+month;
		}
	 	String grantedMe = month +"/"+ grantDate.get(Calendar.YEAR);
		
//	 	System.out.println("QE .............. ceasedqe "+ceasedMe+" .......grantedqe "+grantedMe +"  "+grantDate);
		List<Employee> empList = empRepo.findByArmyNoAndQe(do2.getArmyNo(),ceasedMe,grantedMe);
				
			/*	Utils.getEntityManager().createQuery("select e from Employee e where (e.armyNo=:ano or e.oldArmyNo=:ano) "
				+ " and e.qe in (:cme, :gme) order by id ").setParameter("ano", do2.getArmyNo()).setParameter("cme", ceasedMe)
				.setParameter("gme", grantedMe).getResultList();*/
		if (empList!=null && empList.size()==2) {
			if (!empList.get(0).getUnit().equals(empList.get(1).getUnit())) {
				return;
			}
		}
		do2List=null;
		do2List = do2Repo.findByFromDateLessThanAndUnitAndDescAndColA(do2.getEmployee().getId(), do2.getUnit(), 
																			grantDate.getTime(), description, "GRANTED");
				
				/*Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:emp and d.status='V' and d.unit=:unit and "
				+ " d.fromDate<:fromdate and d.description=:desc and d.columnA='GRANTED' order by d.fromDate desc").setParameter("emp", do2.getEmployee())
				.setParameter("fromdate", grantDate.getTime()).setParameter("desc", description).setParameter("unit", do2.getUnit()).getResultList();*/
		if (do2List==null || (do2List!=null && do2List.size()==0)) {
			return;
		}
		Do2 grantedDo2 = new Do2();			 
		grantedDo2.setBatchNo(49000);
		grantedDo2.setEmployee(do2.getEmployee());		
		grantedDo2.setArmyNo(do2.getArmyNo());
		grantedDo2.setCheckDigit(do2.getCheckDigit());
		grantedDo2.setUnitCode(do2.getUnitCode());
		grantedDo2.setUnit(do2.getUnit());
		grantedDo2.setDo2Group("IA");
		grantedDo2.setDo2Type(do2.getDo2Type());
		grantedDo2.setDo2No(do2.getDo2No());
		grantedDo2.setDo2ItemNo(do2.getDo2ItemNo());
		grantedDo2.setDo2Date(do2.getDo2Date());
		grantedDo2.setDescription(description);
		grantedDo2.setFromDate(grantDate.getTime());
		grantedDo2.setColumnA(Do2Manager.GRANTED);
		grantedDo2.setColumnC(do2List.get(0).getColumnC());
 		grantedDo2.setStatus(Do2Manager.PENDING);
		grantedDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		grantedDo2.setRemarks("COVID19 Special Leave - REGOSL");
		grantedDo2.setAuditCage(true);
		grantedDo2.setQe(Utils.getCurrentQuarter());
		grantedDo2.setInputQe(Utils.getCurrentQuarter());			
		grantedDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		
		if (do2List.get(0).getRate()!=null) {
			grantedDo2.setRate(do2List.get(0).getRate());
		} else {
			grantedDo2.setRate(0.0);			
		}
		
	//	Utils.getEntityManager().persist(grantedDo2);
	//	Utils.getEntityManager().flush();
	}
}
