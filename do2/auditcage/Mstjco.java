package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage; 
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory; 
import itsdc.dolphin.models.Rank; 
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RestController;
 

@RestController
public class Mstjco extends PromotBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee employee;
	Rank rank;
	public Employee prevEmpStatus = null;
	Rank esdRank = null;
	StringBuffer buffer = new StringBuffer();		
	List<EmployeeServiceDetail> esdPromotList = null;
	List<EmployeeServiceDetail> esdRankList = null;
	List<EmployeeServiceDetail> esdAptList = null;
	List<EmployeeServiceDetail> esdClassList = null;
	List<AppointmentRate> appointmentRateList = null;

	public static final String ALREADY_A_JCO = "AU:PBOR is a JCO.\n";
	public static final String NO_INITIAL_MUSTER = "AU:PBOR has not been mustered.\n";
	public static final String PBOR_ON_LEAVE = "AU:Individual is on leave on the date of MSTJCO. \n";
	static public final String INCR_NOT_DUE_ON_OPTION_DATE  = "AU:Increment Not Due In Pre-Promoted/Pre-Upgraded Grade On Opted Date \n";


	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		
		if (newDo2.getId() == do2.getId()) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
			rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");			
		
			if (!(isDo2AfterMuster(do2))) {
				validity = false;
				buffer.append(NO_INITIAL_MUSTER);
			}

			if (!(isValidRank(do2, employee))) {
				validity = false;
				buffer.append(ALREADY_A_JCO);
			}
			
			if (isPborOnRegularLeave(do2, employee)) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(PBOR_ON_LEAVE);
				validity = false;
			}
			
			if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null)			
				if (!doesIncrExistsOnOptionDate(do2, employee)) {
					buffer.append(INCR_NOT_DUE_ON_OPTION_DATE);
					validity=false;
					do2.setStatus(Do2Manager.INVALID);
				}
						
			if(!Utils.getPao().getPaoCode().equals("89") && newDo2.getColumnC().trim().equals("TRL")) {
				validity = false;
			}
			
			if (!validity) {
				if ((do2.getStatus() == null) || (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
					do2.setStatus(Do2Manager.INVALID);
				}
			} else {
				do2.setStatus(Do2Manager.VALID);
			}
			do2.setReason(buffer.toString());
			if (do2.getId() == newDo2.getId()) {
				if (do2.getColumnA().equals("NBSUB")) {
					if (!isMinimumPay(do2))
						createSysDo2(employee, do2);
				} else {
					createSysDo2(employee, do2);
				}
			}
		 
			if(!validity){
				if(do2.getStatus() == null)
					do2.setStatus(Do2Manager.INVALID);
			}else{
				do2.setStatus(Do2Manager.VALID);
			}
	 	do2.setReason(buffer.toString());
		}
		
		if (do2.getId()==0) {
		 	do2Repo.save(do2);
		}
		return validity;
	}

	/*@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		prevEmpStatus = employee;

		updateBdtInfo(newAuditCage.getDo2());
		Date sixcpcFromDate = sixcpcDo2Exists(newAuditCage.getDo2());
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) || 
		   (sixcpcFromDate != null && !newAuditCage.getFromDate().after(sixcpcFromDate))) {
			get5cpcPay(newAuditCage, baseAuditCage, prevEmpStatus);
		} else {
			get6cpcPay(newAuditCage, baseAuditCage);
			if((newAuditCage.getDo2().getColumnB().charAt(3)=='X') && 
						(newAuditCage.getDo2().getColumnC().trim().equals("TRL"))) {
				newAuditCage.setXGroupPay(1400);
			} else if (newAuditCage.getDo2().getColumnB().charAt(3)=='Y' && baseAuditCage.getXGroupPay() > 0) {
				newAuditCage.setXGroupPay(0);
				
			}
		}
		
        if(!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
        	getOldBasicPay(newAuditCage,baseAuditCage,prevEmpStatus);
        }
		return newAuditCage;
	}
*/
	
	// above method commented and amended as below due to sevencpc
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		prevEmpStatus = employee;

		updateBdtInfo(newAuditCage.getDo2());
		Date sixcpcFromDate = sixcpcDo2Exists(newAuditCage.getDo2());
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) || 
		   (sixcpcFromDate != null && !newAuditCage.getFromDate().after(sixcpcFromDate))) {
			get5cpcPay(newAuditCage, baseAuditCage, prevEmpStatus);
		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			get6cpcPay(newAuditCage, baseAuditCage);
			getOldBasicPay(newAuditCage,baseAuditCage,prevEmpStatus);
			if((newAuditCage.getDo2().getColumnB().charAt(3)=='X') && 
						(newAuditCage.getDo2().getColumnC().trim().equals("TRL"))) {
				newAuditCage.setXGroupPay(1400);
			} else if (newAuditCage.getDo2().getColumnB().charAt(3)=='Y' && baseAuditCage.getXGroupPay() > 0) {
				newAuditCage.setXGroupPay(0);
			}
		} else {
			 if (newAuditCage.getGradePay()!=0 && newAuditCage.getPayLevel()!=null && newAuditCage.getPayLevel().equals("0") 
					 			&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					 get6cpcPay(newAuditCage, baseAuditCage);
					 get6cpcOldPay(newAuditCage, baseAuditCage);
					 if ((newAuditCage.getDo2().getColumnB().charAt(3)=='X') && (newAuditCage.getDo2().getColumnC().trim().equals("TRL"))) {
							newAuditCage.setXGroupPay(1400);
					 } else if (newAuditCage.getDo2().getColumnB().charAt(3)=='Y' && baseAuditCage.getXGroupPay() > 0) {
							newAuditCage.setXGroupPay(0);
					 }
			 } else if (newAuditCage.getGradePay()==0 && newAuditCage.getPayLevel()!=null && !newAuditCage.getPayLevel().equals("0") 
					 			&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					get7cpcPay(newAuditCage, baseAuditCage);
					get6cpcOldPay(newAuditCage,baseAuditCage);
					if((newAuditCage.getDo2().getColumnB().charAt(3)=='X') && (newAuditCage.getDo2().getColumnC().trim().equals("TRL"))) {
						newAuditCage.setXGroupPay(3600);
					} else if (newAuditCage.getDo2().getColumnB().charAt(3)=='Y' && baseAuditCage.getXGroupPay() > 0) {
						newAuditCage.setXGroupPay(0);
					}
			} 
		}		
		newAuditCage.setClassPay(0);
		newAuditCage.setOldClassPay(0);
		
     	return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_MUSTER+"");
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		final String[] alwUnderProcess = { "APTPAY", "GSPAY", };
		final List<String> alwToStop = Arrays.asList(alwUnderProcess);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);

		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_PROMOT);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_MUSTER);

		reverseEmployeeStatus(currentDo2, refDo2);
		return Do2Manager.SUCCESS;
	}

	// ******************************************************************
/*	public boolean isDo2AfterMuster(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		return (employee.getInitialMusteringDate()!=null && !do2.getFromDate().before(employee.getInitialMusteringDate())); 
	}*/
	
	public boolean isDo2AfterMuster(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee.getEnrolmentDate()!=null && employee.getNbsubDate()!=null  && employee.getEnrolmentDate().equals(employee.getNbsubDate())) {
			return true;
		}
		return (employee.getInitialMusteringDate()!=null && !do2.getFromDate().before(employee.getInitialMusteringDate())); 
	}
/*
	public boolean isValidRank(Do2 do2, Employee employee) {
		
	 	rank=null;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		if (rank!=null && Integer.parseInt(rank.getRankCode())>=Do2Manager.JCO_RANK_CODE ) {
			return false;
		} else {
			return true;
		}
	}
	*/
public boolean isValidRank(Do2 do2, Employee employee) {
		
		if (employee.getEnrolmentDate()!=null && employee.getNbsubDate()!=null  && employee.getEnrolmentDate().equals(employee.getNbsubDate())) {
			return true;
		}
	 	rank=null;
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
	//	rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PROMOT");
		
		if (rank!=null && Integer.parseInt(rank.getRankCode())>=Do2Manager.JCO_RANK_CODE ) {
			return false;
		} else {
			return true;
		}
	}

	public Date sixcpcDo2Exists(Do2 do2){
		List<Do2> do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(),"SIXCPC",Do2Manager.VALID);
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT d FROM Do2 d where d.employee=:emp and d.status=:status and d.description=:description")
						.setParameter("emp",do2.getEmployee())
						.setParameter("description","SIXCPC")
						.setParameter("status",Do2Manager.VALID)
						.getResultList();*/
		
	    if (do2List.size()>0){
	        	return do2List.get(0).getFromDate();
	    }
	    return null;
	} 

	public void updateEmployeeToJco(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (newDo2!=null && newDo2.getId()==do2.getId()) {
			employee.setPreviousGroupCategory(employee.getGroupCategory());
			employee.setPreMusterDate(employee.getInitialMusteringDate());
		}else{//lines added if the do2 is not processed as new do2
			//GroupCategory grpcat = new GroupCategory();
		//	grpcat=gcRepo.findBySoldierCategory(do2.getColumnC()).get(0);
			List<GroupCategory> grpcat = gcRepo.findByCatAndFromDateLessThanEqual(do2.getColumnC(),do2.getFromDate());
			
		//	grpcat =((GroupCategory) Utils.getForeignKey(grpcat, "soldierCategory", do2.getColumnC()));
			if(grpcat.get(0).getId() !=employee.getGroupCategory().getId()){
				employee.setPreviousGroupCategory(employee.getGroupCategory());
				employee.setPreMusterDate(employee.getInitialMusteringDate());
			}
		}
	
		employee.setRemusteringDate(do2.getFromDate());
		employee.setCurrentClassPayDate(null);
		Rank rank = rankRepo.findByRankCode("50");
				
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rank")
				.setParameter("rank", "50").getResultList();*/
		employee.setSubstantiveRank(rank);
		employee.setSubstantiveRankDate(do2.getFromDate());
		employee.setActingRank(null);
		employee.setActingRankDate(null);
		if(employee.getEnrolmentDate()!=null && employee.getNbsubDate()!=null && employee.getEnrolmentDate()!=employee.getNbsubDate()) {
			employee.setNbsubDate(do2.getFromDate());
		}
		
	//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "SoldierCategory", (do2.getColumnC().trim().toUpperCase())));
		GroupCategory grpcat=gcRepo.findBySoldierCategory(do2.getColumnC().trim()).get(0);
		employee.setGroupCategory(grpcat);
		employee.setTermsColourService(rank.getColorService());
		employee.setTermsMaxAgeRetention(rank.getServiceRetention());
		employee.setMusterJcoFlag(true);
		employee.setMusterJcoDate(do2.getFromDate());
		employee.setSoldierClass("0");
		employee.setCurrentGspayCode("0");
		employee.setCurrentGsPayDate(null);
		employee.setAppointmentDate(null);
		
		empRepo.save(employee);
		 
	}

	public void reverseEmployeeStatus(Do2 cDo2, Do2 rDo2) {
		GroupCategory grpcat=new GroupCategory();
		employee = Utils.getCurrentEmployee(cDo2.getArmyNo());
		employee.setInitialMusteringDate(employee.getPreMusterDate());
		employee.setPreMusterDate(null);
		employee.setGroupCategory(employee.getPreviousGroupCategory());
		employee.setPreviousGroupCategory(null);
		employee.setRemusteringDate(null);
		employee.setMusterJcoFlag(false);
		employee.setMusterJcoDate(null);
		employee.setNbsubDate(null);

		// Reversal To Previous Rank - from ESD
	 	//esdRank = Utils.getRank(cDo2.getArmyNo(), cDo2.getFromDate());
	 	esdRank = Utils.getPborRank(cDo2.getArmyNo(), cDo2.getFromDate(),"ALLCE");
	 	esdPromotList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_PROMOT+"");
	 			
	 			
	 	/*		Utils.getEntityManager().createQuery("SELECT al FROM EmployeeServiceDetail al WHERE "
						+ " al.employee=:employee AND al.status=:stat order by al.fromDate desc")
						.setParameter("employee", employee).setParameter("stat", Do2Manager.ESD_PROMOT)
						.getResultList();*/

		if (esdRank != null) {
			employee.setSubstantiveRank(esdRank);
			if (esdRank.getRankCode().equals("20")) {
				employee.setActingRank(null);
				employee.setActingRankDate(null);
			} else {
				if (esdPromotList.size()>0) {
					employee.setActingRank(esdRank);
					employee.setActingRankDate(esdPromotList.get(0).getDo2().getFromDate());
				}
			}
			
			if (esdPromotList.size()>0) 
				employee.setSubstantiveRankDate(esdPromotList.get(0).getDo2().getFromDate());
			
			employee.setNbsubDate(null);
			employee.setTermsColourService(esdRank.getColorService());
			employee.setTermsMaxAgeRetention(esdRank.getServiceRetention());
		}

		// Reversal To Previous Appointment - if held - from ESD
	 	esdAptList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_APPT+"");
	 			
	 			
	 		/*	Utils
				.getEntityManager()
				.createQuery(
						"SELECT al FROM EmployeeServiceDetail al WHERE "
								+ " al.employee=:employee AND al.status=:stat order by al.fromDate desc")
				.setParameter("employee", employee).setParameter("stat", Do2Manager.ESD_APPT).getResultList();*/
		if (esdPromotList.size() > 0 && esdAptList.size() > 0) {
			appointmentRateList = apptRateRepo.findByAppointmentNameFromDateBetweenOrFromDateLessThanFromDateDesc(esdAptList.get(0).getDo2().getColumnA(),
																					esdAptList.get(0).getDo2().getFromDate());
					
			/*		Utils
					.getEntityManager()
					.createQuery(
							"SELECT rl FROM AppointmentRate rl WHERE "
									+ " rl.appointmentName = :apptName and "
									+ " ((:fromDate between rl.fromDate and rl.toDate) or "
									+ "  (:fromDate > rl.fromDate and rl.toDate is null)) order by rl.fromDate desc")
					.setParameter("apptName",
							esdAptList.get(0).getDo2().getColumnA())
					.setParameter("fromDate",
							esdAptList.get(0).getDo2().getFromDate())
					.getResultList();*/
			if (esdAptList.get(0).getFromDate().after(
					esdPromotList.get(0).getFromDate())) {
				employee.setAppointmentRate(appointmentRateList.get(0));
				employee.setAppointmentDate(esdAptList.get(0).getDo2()
						.getFromDate());
			} else {
				employee.setAppointmentRate(null);
				employee.setAppointmentDate(null);
			}
		} else {
			employee.setAppointmentRate(null);
			employee.setAppointmentDate(null);
		}

		// Reversal To Previous Class Pay Rat - if held - from ESD
	 	esdClassList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(),Do2Manager.ESD_CLASS_PAY+"");
	 			
	 	/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT cl FROM EmployeeServiceDetail cl WHERE "
								+ " cl.employee=:employee AND cl.status=:stat order by cl.fromDate desc")
				.setParameter("employee", employee).setParameter("stat",
						Do2Manager.ESD_CLASS_PAY).getResultList();*/
		if (esdClassList.size() > 0) {
			if (esdClassList.get(0).getDo2().getColumnC()!=null)
				grpcat=gcRepo.findBySoldierCategory(rDo2.getColumnC().trim().toUpperCase()).get(0);
				employee.setGroupCategory(grpcat);
			//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "SoldierCategory", (rDo2
			//					.getColumnC().trim().toUpperCase())));

			employee.setSoldierClass(esdClassList.get(0).getDo2().getColumnB().charAt(3)+"");
			employee.setCurrentClassPayDate(esdClassList.get(0).getFromDate());
		}
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);
	}

	
	public boolean checkNQS(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Date checkStartDate = DateManager.getToday();
		Date checkEndDate = employee.getIncrementDate();
		Calendar cal = new GregorianCalendar();
		cal.setTime(checkEndDate);
		cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
		checkStartDate.setTime(cal.getTimeInMillis());
		int nqsDays = 0;
		Date firstIncrDate = new Date(2006 - 07 - 01);
		List<EmployeeServiceDetail> nqsInESD = esdRepo.findNqs2(employee.getId(), Do2Manager.ESD_NQS+"", checkStartDate, checkEndDate);
				
				
			/*	Utils
				.getEntityManager()
				.createQuery(
						"SELECT nqs FROM EmployeeServiceDetail nqs "
								+ " WHERE nqs.employee=:emp AND nqs.status=:status AND ((nqs.fromDate < :csd AND nqs.toDate <= :ced) OR"
								+ " (nqs.fromDate >=:csd AND nqs.toDate > :ced) OR"
								+ " (nqs.fromDate >= :csd AND nqs.toDate <=:ced) OR nqs.toDate IS null)")

				.setParameter("csd", checkStartDate).setParameter("ced",checkEndDate).setParameter("emp", employee)
				.setParameter("status", Do2Manager.ESD_NQS).getResultList();*/
		
	 
		
		if (nqsInESD.size() == 0)
			return true;

		if (nqsInESD.size() > 0) {
			Date sDate = null;
			Date eDate = null;
			for (EmployeeServiceDetail nqs : nqsInESD) {
				sDate = nqs.getFromDate();
				eDate = nqs.getToDate();
				if (employee.getIncrementDate().equals(firstIncrDate))
					sDate = new Date(2006 - 01 - 01);
				eDate = new Date(2006 - 06 - 30);
				nqsDays += (int) DateManager.getNoOfDays(sDate, eDate);
				if (nqsDays > 0)
					return false;
				if (sDate.before(checkStartDate))
					sDate = checkStartDate;
				else if (eDate.after(checkEndDate))
					eDate = checkEndDate;
				nqsDays += (int) DateManager.getNoOfDays(sDate, eDate);
			}
			if (nqsDays > 180)
				return false;
		}
		return true;
	}
 	
	public void createSysDo2(Employee employee, Do2 do2) {
		  
		if (!(do2.getFromDate().before(DateManager.getSixthCpcDate()))) {
			Date xDate = do2.getFromDate();
			Calendar yy = new GregorianCalendar();
			yy.setTimeInMillis(xDate.getTime());
			int zz = yy.get(Calendar.YEAR);

			Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
			Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);

			Date jan01 = chkDate1.getTime();
			Date jul01 = chkDate2.getTime();
			if (((xDate.after(jan01)) && (xDate.before(jul01)) && !Utils.isAcpBeforeCurrentDo2(do2)) 
					|| (!Utils.isAcpBeforeCurrentDo2(do2) && do2.getFromDate().after(DateManager.getSeventhCpcDate()) 
							&& do2.getToDate()!=null)) {
				createProfixDo2(employee,do2);
			}
		}
	}
	
	public boolean isMinimumPay(Do2 do2) {
		
		List<AuditCage> auditCageList = acRepo.findByFromDateLessThanEqualOrderByFromDateDesc(Utils.getCurrentEmployee(do2.getArmyNo()).getId(), do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac WHERE ac.employee=:employee " +
							" AND ac.cancelled=false AND ac.recordStatus!='P' AND ac.absentFlag=false " +
							" AND ac.fromDate<=:fromDate AND ac.effective=true ORDER BY fromDate DESC ").setParameter("employee", Utils.getCurrentEmployee(do2.getArmyNo()))
							.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		
		if (auditCageList.size()>0) {
		 	if ((auditCageList.get(0).getXGroupPay()!=null && auditCageList.get(0).getXGroupPay()!=0 && auditCageList.get(0).getBasicPay()<8520)
		 			|| (auditCageList.get(0).getBasicPay()<8600)){
		 		return true;
		  	}
	 	}
		return false;
 		
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployeeToJco(do2);
	}
	
	public boolean isPborOnRegularLeave(Do2 do2, Employee employee) {
        Date adjustedQeDate = null; 
		Date dolphinStartDate = Utils.getDolphinStartDate();
		if (do2.getAcceptedQe() != null) {
			try {
				adjustedQeDate = DateManager.getDate(do2.getAcceptedQe().substring(3)  + "-" +do2.getAcceptedQe().substring(0,2) + "-01");
			} catch (Exception e) {
				Utils.handleException(e);
			}							 
		}
		if (adjustedQeDate != null && adjustedQeDate.before(dolphinStartDate)){
			return false;
		}
		List<String> descList=Arrays.asList(new String[] {"CL","EXTNCL"});
		List<Do2> do2List = do2Repo.findForNotInMultipleDescriptionsWithFromDateBetween(employee.getId(),Do2Manager.VALID,
												Do2Manager.PENDING,descList,do2.getFromDate());
				
				
			/*	Utils.getEntityManager().createQuery(
						" SELECT d2 FROM Do2 d2 WHERE d2.employee.id=:empId AND " +
						" :fromDate BETWEEN d2.fromDate AND d2.toDate AND " +
						" (d2.status=:status OR d2.status=:status1) AND " +
						" d2.description!=:desc1 AND d2.description!=:desc2 ")
				.setParameter("empId", employee.getId())
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("status",	Do2Manager.VALID)
				.setParameter("status1",Do2Manager.PENDING)
				.setParameter("desc1", "CL")
				.setParameter("desc2", "EXTNCL").getResultList();*/

		for (Do2 leaveDo2 : do2List) {
			if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription()))
				return true;
		}
		return false;
	}
	
	public boolean doesIncrExistsOnOptionDate(Do2 do2, Employee employee) {
		List<Do2> do2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V","INCR",do2.getToDate());
				
			/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
		 		+ " and d.employee=:emp and d.status='V' and d.fromDate=:toDate ")
				.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
				.getResultList();*/
		
		if (do2List!=null && do2List.size()>0) {
			return true;
		} else {
			do2List=null;
			do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(),"V","INCR",do2.getToDate()); 
					
				/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
			 		+ " and d.employee=:emp and d.status='V' and d.fromDate<:toDate order by d.fromDate desc ")
					.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
					.getResultList();*/
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
				System.out.println("xxxxxxxxxxxxxxxxxxxxxxxx next incr date from optfix "+nextIncrDate.getTime()+"  "+do2.getToDate());
				
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

}
