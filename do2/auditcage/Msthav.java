package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;
 
      
@RestController
public class Msthav extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee = null;;
	Employee previousEmployeeStatus = null;
	List<EmployeeServiceDetail> esdPromotList = null;
	List<EmployeeServiceDetail> esdRankList = null;
	List<EmployeeServiceDetail> esdAptList = null;
	List<EmployeeServiceDetail> esdClassList = null;
	List<AppointmentRate> appointmentRateList = null;
	Rank rank = null;
	Rank payFixRank = null;
	Rank prevRankList = null;
	Rank promotRank = null;
	List<PayScale> previousPayList = null;
	List<PayScale> currentPayList = null;
	Rank esdRank = null;
	public boolean updated = false;
	public boolean clspayToBeSetToZero = false;
	static final String IS_A_RECRUIT = "AU : PBOR Is A Recruit.\n";
	static final String IS_A_JCO = "AU:PBOR Already A JCO.\n";
	static final String NOT_VALID_CORE = "AU:Not valid Corps for GP-X entry.\n";
	static final String NOT_CLASS_ONE = "AU:PBOR has not qualified for Class I before MSTHAV.\n";
	static final String INVALID_PAY_STAGE = "AU:Pay stage wrong for fixation.\n";
	static final String IS_A_HAV = "AU:PBOR Already HAVILDAR.\n";
	public static final String PBOR_ON_LEAVE = "AU:PBOR is on leave on the date of Msthav. \n"; // eme oberseration  11/2018
	static public final String INCR_NOT_DUE_ON_OPTION_DATE  = "AU:Increment Not Due In Pre-Promoted/Pre-Upgraded Grade On Opted Date \n";

	@Override
	public boolean isValid(Do2 do2) {
		StringBuffer buffer = new StringBuffer();
		boolean msthavValidity = true;
		do2.setStatus(Do2Manager.VALID);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		payFixRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		promotRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PROMOT");

		Date amendmentappxjno24date=null;		
		try {
			amendmentappxjno24date=DateManager.getDate("2016-08-16");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		if (newDo2.getId()==do2.getId()) {
			if (!do2.getDo2Date().before(amendmentappxjno24date) && 
				!do2.getFromDate().before(amendmentappxjno24date)) {
				if (rank.getRankCode().equals("40")) {
					buffer.append(IS_A_HAV);
					msthavValidity = false;
				}
			}
		}
		
		
		if (!(isRecruit(do2))) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(IS_A_RECRUIT);
			msthavValidity = false;
		}

		if (!(isHavJco())) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(IS_A_JCO);
			msthavValidity = false;
		}
		
		
		if (isPborOnRegularLeave(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(PBOR_ON_LEAVE);
			msthavValidity = false;
		}

		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null)			
			if (!doesIncrExistsOnOptionDate(do2, employee)) {
				buffer.append(INCR_NOT_DUE_ON_OPTION_DATE);
				msthavValidity=false;
				do2.setStatus(Do2Manager.INVALID);
			}
		
		if (newDo2.getId()==do2.getId() && Utils.getPao().getPaoCode().equals(Do2Manager.PAO_EME)) {
			if (!(isAvCore(do2))) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(NOT_VALID_CORE + NOT_CLASS_ONE);
				msthavValidity = false;
			}
		}
		
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			if (!(isValidPayStage(do2))) {
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(INVALID_PAY_STAGE);
				msthavValidity = false;
			}
		}

		if (!msthavValidity){
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}

		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		if (msthavValidity && newDo2.getId()==do2.getId()) { 		
			if (employee!=null && employee.getAcpnaikDate()!=null
					 && !employee.getAcpnaikDate().before(DateManager.getSixthCpcDate()) && employee.getNaikDate()==null) {
				;
			} else if (employee!=null && (employee.getAcphavDate()!=null || employee.getHavDate()!=null)) {
				;
			} else if (employee!=null && employee.getAcpnaikDate()!=null && employee.getAcphavDate()!=null) {
				;
			} else {
				if (!isIncrDo2Generated(do2))
					generateSysDo2(do2);
			}			
		}
 		return msthavValidity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());		
		Rank previousRankList = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PAYFIX");
		previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(previousRankList.getId(),
				employee.getGroupCategory().getSoldierGroup(), newAuditCage.getDo2().getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale is NOT NULL")
				.setParameter("rnk", previousRankList)
				.setParameter("grp", employee.getGroupCategory().getSoldierGroup())
				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
				.getResultList();*/
		
		if (isClassPayToBeSetToZero(newAuditCage.getDo2())) {
			newAuditCage.setClassPay(0);
		}

		updateBdtInfo(newAuditCage.getDo2());
         
		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(employee.getSubstantiveRank().getId(),
				employee.getGroupCategory().getSoldierGroup(), newAuditCage.getDo2().getFromDate());
				
				
				/*Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale is NOT NULL")
				.setParameter("rnk", Utils.getForeignKey(new Rank(), "rankCode",
						employee.getSubstantiveRank().getRankCode()))
				.setParameter("grp", employee.getGroupCategory().getSoldierGroup())
				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
				.getResultList();	*/	
		
	 
	
	 	Date sixcpcFromDate = sixcpcDo2Exists(newAuditCage.getDo2());
	 	if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) || sixcpcFromDate != null && !newAuditCage.getFromDate().after(sixcpcFromDate)) {
	 			get5cpcPay(newAuditCage, baseAuditCage);
	 	} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
	 			get6cpcPay(newAuditCage, baseAuditCage);
	 			updateOldBasicPay(newAuditCage,baseAuditCage,previousRankList);
	 	} else if (newAuditCage.getGradePay()!=0 && newAuditCage.getPayLevel()!=null && newAuditCage.getPayLevel().equals("0") 
						&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				get6cpcPay(newAuditCage, baseAuditCage);
				get6cpcOldPay(newAuditCage, baseAuditCage);
	 	} else if (newAuditCage.getGradePay()==0 && newAuditCage.getPayLevel()!=null && Integer.parseInt(newAuditCage.getPayLevel())>1 
	 					&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
 				get7cpcPay(newAuditCage,baseAuditCage);
 				get6cpcOldPay(newAuditCage,baseAuditCage);
	 	} 
	 	
		/*if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate())){
			get5cpcPay(newAuditCage, baseAuditCage);
		} else {
			get6cpcPay(newAuditCage, baseAuditCage);
            if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
        	    updateOldBasicPay(newAuditCage,baseAuditCage,previousRankList);
            }
		}   */
	
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		int index = do2.getColumnC().indexOf('-');
		String columnC = do2.getColumnC();
		if (index > 0) {
			columnC = columnC.substring(0, index);	
		}
			
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_MUSTER+"");

		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		if (newAuditCage.getClassPay()==0) {// Code added as higher rate of xpay getting recovered in EME cases due to wrong updations in esd for clspay -- 19.11.2020
			List<EmployeeServiceDetail> esdList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateAscToDateAsc(newAuditCage.getEmployee().getId(),
																							Do2Manager.ESD_CLASS_PAY+"");
					
					
			/*		Utils.getEntityManager().createQuery(
					"SELECT e FROM EmployeeServiceDetail e WHERE e.status=:status AND e.employee=:emp ORDER BY e.fromDate, e.toDate ")
					.setParameter("emp", newAuditCage.getEmployee()).setParameter("status", Do2Manager.ESD_CLASS_PAY).getResultList();*/
			if (esdList.size()>0) {
				if (esdList.size()==1) {
					if (esdList.get(0).getFromDate().before(do2.getFromDate())) {
				 	 	esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));				 	 
					} else if (esdList.get(0).getFromDate().equals(do2.getFromDate())) {  
						esdList.get(0).setToDate(do2.getFromDate());
					}
				} else {
					for (int i=0; i<esdList.size(); i++) {
					 	if (esdList.get(i).getFromDate().after(do2.getFromDate())) {
					 		if (i>0) {
					 			esdList.get(i-1).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
					 			break;
					 		}  
						}										
					}
				}			
			}
		}
		
		
		/*if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_EME) && columnC.equals("HMT")) {
				List<AppointmentRate> appointmentRateList = Utils.getEntityManager().createQuery(
					" SELECT ar FROM AppointmentRate AS ar WHERE ar.appointmentName =:apptName AND " +
					"  ar.pao is null AND ((:fromDate BETWEEN ar.fromDate AND ar.toDate) OR " +
					" (:fromDate > ar.fromDate AND ar.toDate IS null))")
					.setParameter("apptName", "CHM")
					.setParameter("fromDate", do2.getFromDate()).getResultList();
		 		AuditCageHelper.updateAuditCageAllowance(newAuditCage, "APTPAY", appointmentRateList.get(0).getRate());
			} else {
				final String[] alwUnderProcess = { "APTPAY" };
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			}*/
		
		// above code amended as stated below in view of amendment NO.24  to appendix_j for description MSTHAV
		if (Utils.getPao().getPaoCode().equals("88") && do2.getFromDate().before(DateManager.getRevisedMsthavImplDate()) 
				&& columnC.equals("HMT")){	
			List<AppointmentRate> appointmentRateList = apptRateRepo.findByApptNameFromDateBetweenOrFromDateGreater("CHM",
																						do2.getFromDate());
					
					
		/*			Utils.getEntityManager().createQuery(
					" SELECT ar FROM AppointmentRate AS ar WHERE ar.appointmentName =:apptName AND " +
					"  ar.pao is null AND ((:fromDate BETWEEN ar.fromDate AND ar.toDate) OR " +
					" (:fromDate > ar.fromDate AND ar.toDate IS null))")
					.setParameter("apptName", "CHM")
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		 		AuditCageHelper.updateAuditCageAllowance(newAuditCage, "APTPAY", appointmentRateList.get(0).getRate());
		} else {
			final String[] alwUnderProcess = { "APTPAY" };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_PROMOT);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_MUSTER);
		reverseEmployeeStatus(currentDo2, refDo2);
		return Do2Manager.SUCCESS;
	}

	// *************************************************************
	public boolean isRecruit(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (rank.getRankCode() != null && (!rank.getRankCode().equals(Do2Manager.RECRUIT)))
			return true;
		else
			return false;
	}

	public boolean isHavJco() {
		if (!(Do2Manager.JCO_RANKS.contains(rank.getRankCode())))
			return true;
		else
			return false;
	}

	public boolean isAvCore(Do2 do2) {
		// (Auth : IHQ Lr No. B/55320/HMT Gen/Policy/EME Trg-4 dated 29.04.2008. -- commented on 07.02.2014 as per request from PAO EME EDP Centre)
		/*final List<String> AV_CORE = Arrays.asList(new String[] { "AVFAF", "AVFARM", "AVFINP", "AVFELE", 
			 "AVFAEN", "AVFAVN", "AVNTECAE", "AVNTECAF", "AVNTECAVS","AVNTECELE","AVNTECIP" });
		if (do2.getColumnA().charAt(3) == 'X') {
			if (!AV_CORE.contains(employee.getGroupCategory().getSoldierCategory())) {
				if (do2.getColumnB().charAt(3)=='1') {
					do2.setStatus(Do2Manager.INVALID);
					return false;
				} 
			} else if (!((employee.getSoldierClass() == '1') && (employee.getCurrentClassPayDate().before(do2.getFromDate())))) {
					do2.setStatus(Do2Manager.INVALID);
					return false;
				}
		}*/
		return true;
	}

	public boolean isValidPayStage(Do2 do2) {
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
        GroupCategory gp = Utils.getGroupCategory(do2,"PREV");
       	previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(rank.getId(),gp.getSoldierGroup(),
       															do2.getFromDate());
       			
       		/*	Utils.getEntityManager().createQuery(
				"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
						+ " AND ps.soldierGroup.soldierGroup=:grp "
						+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
						+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
						+ " AND ps.startScale is NOT NULL")
		.setParameter("rnk", rank).setParameter("grp",
				gp.getSoldierGroup())
		.setParameter("fromDate", do2.getFromDate()).getResultList();*/

		//CONDITION <= MADE AS <
		List<AuditCage> atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(), do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery(
						"SELECT atc FROM AuditCage atc WHERE atc.employee=:emp AND atc.cancelled=false  " +
						" AND atc.effective=true AND atc.recordStatus!='P' AND atc.fromDate < :frdt ORDER BY atc.fromDate DESC")
				.setParameter("emp", employee)
				.setParameter("frdt", do2.getFromDate()).getResultList();*/
		for (int i = previousPayList.get(0).getStartScale(); i <= previousPayList
				.get(0).getScaleEnd0(); i += previousPayList.get(0).getIncr0()) {
			if (atcList.get(0).getBasicPay() == i)
				return true;
		}
		return false;
	}

	public void updateEmployee(Do2 do2) {
	 	int index = do2.getColumnC().indexOf('-');
		String columnC = do2.getColumnC();
		if (index > 0) {
			columnC = columnC.substring(0,index);	
		}		
		if (newDo2!=null && newDo2.getId()==do2.getId()) {
			employee.setPreviousGroupCategory(employee.getGroupCategory());
			employee.setPreMusterDate(employee.getInitialMusteringDate());
		}else{//lines added if the do2 is not processed as new do2
			GroupCategory grpcat = new GroupCategory();
			grpcat =gcRepo.findBySoldierCategory(columnC).get(0);
		//	grpcat =((GroupCategory) Utils.getForeignKey(grpcat, "soldierCategory", columnC));
			if(grpcat.getId() !=employee.getGroupCategory().getId()){
				System.out.println ( " grpcat id "+grpcat.getId()   + " employeegpcategory " +employee.getGroupCategory().getId());
				if (!grpcat.getSoldierGroup().equals(employee.getGroupCategory().getSoldierGroup())) {
					System.out.println ( " grpcatsoldiergroup " +grpcat.getSoldierGroup()   +" grp " +employee.getGroupCategory().getSoldierGroup());
					employee.setPreviousGroupCategory(employee.getGroupCategory());
					employee.setPreMusterDate(employee.getInitialMusteringDate());
				}
				
			}
		}
		Rank rank = rankRepo.findByRankCode("40");
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT rnk FROM Rank AS rnk WHERE rnk.rankCode=:rank")
				.setParameter("rank", "40").getResultList();*/
		if (employee.getHavDate()!=null && employee.getHavDate().before(do2.getFromDate())) {   // added on 23.07.2024
			;
		} else {
			employee.setSubstantiveRank(rank);
			employee.setSubstantiveRankDate(do2.getFromDate());
			employee.setActingRank(rank);
			employee.setActingRankDate(do2.getFromDate());
			employee.setHavDate(do2.getFromDate());
		}
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_EME) && columnC.equals("HMT")) {
			List<AppointmentRate> appointmentRateList = apptRateRepo.findByApptNameFromDateBetweenOrFromDateGreater("CHM",
					do2.getFromDate());
					
					
				/*	Utils.getEntityManager().createQuery(
							"SELECT ar FROM AppointmentRate AS ar WHERE "
									+ " ar.appointmentName =:apptName AND "
									+ " ar.pao is null AND "
									+ " ((:fromDate BETWEEN ar.fromDate AND ar.toDate) OR"
									+ " (:fromDate > ar.fromDate AND ar.toDate IS null))")
					.setParameter("apptName", "CHM")
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
			employee.setAppointmentRate(appointmentRateList.get(0));
			employee.setAppointmentDate(do2.getFromDate());
			
 		} else {
			employee.setAppointmentRate(null);
		}
		//these values are set to null in nbsub promotion ,in reaudit soldier class isnull
		//condition add for checking null
		if(employee.getSoldierClass()==null){
		//	employee.setSoldierClass(do2.getColumnB().charAt(3));
			employee.setSoldierClass(do2.getColumnB().substring(3));
			employee.setCurrentClassPayDate(do2.getFromDate());
		}else{
		/*	if (!(employee.getSoldierClass()=="1" && employee.getGroupCategory().getSoldierGroup()=="X" 
				&& employee.getCurrentClassPayDate().before(do2.getFromDate()))) {*/
			if (!(employee.getSoldierClass().equals("1") && employee.getGroupCategory().getSoldierGroup().equals("X") 
						&& employee.getCurrentClassPayDate().before(do2.getFromDate()))) {
//				employee.setSoldierClass(do2.getColumnB().charAt(3));	
			employee.setSoldierClass(do2.getColumnB().substring(3));
			employee.setCurrentClassPayDate(do2.getFromDate());
			}
		}
	
		GroupCategory grpcat = new GroupCategory();
		GroupCategory gc = gcRepo.findBySoldierCategory(columnC).get(0);
	//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(grpcat, "soldierCategory", columnC));
		employee.setGroupCategory(gc);
	
		employee.setRemusteringDate(do2.getFromDate());
		employee.setTermsColourService(rank.getColorService());
		employee.setTermsMaxAgeRetention(rank.getServiceRetention());

		employee.setMusterHavildarFlag(true);
		employee.setMusterHavildarDate(do2.getFromDate());
		
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);
	//	Utils.getEntityManager().flush();
	}

	public void generateSysDo2(Do2 do2) {
		employee.setIncrementDate(fixDni(do2));
		empRepo.save(employee);
		if (employee.getIncrementDate().before(Utils.getLastDayOfCurrentQuarter())) {
			boolean done = false;
			for (; !done;) {
				createSysIncrDo2(do2);
				if (employee.getIncrementDate().after(Utils.getLastDayOfCurrentQuarter())) {
					done = true;
				}
			}
		}
		if (do2.getFromDate().after(DateManager.getSixthCpcDate())) {
			if (employee.getAcpnaikDate()!=null && employee.getAcpnaikDate().before(do2.getFromDate())
					&& employee.getNaikDate()==null) {
				Date xDate = do2.getFromDate();
				Calendar yy = new GregorianCalendar();
				yy.setTimeInMillis(xDate.getTime());
				int zz = yy.get(Calendar.YEAR);
				Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
				Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
				Date jan01 = chkDate1.getTime();
				Date jul01 = chkDate2.getTime();
				if ((xDate.after(jan01) && xDate.before(jul01))
						|| (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null)) {
					createProfixDo2(do2);
				}		
			}			
		}
	}

	public Date fixDni(Do2 do2) {
		Date xx = do2.getFromDate();
		Date dni =DateManager.getToday();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xx.getTime());
		int year = yy.get(Calendar.YEAR);
		int month = yy.get(Calendar.MONTH);
		int date = 01;
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			Calendar chkDate0 = new GregorianCalendar(year, month, date);
			dni = chkDate0.getTime();
			return DateManager.getNextIncrementDate(dni);
		} else {
			Calendar chkDate1 = new GregorianCalendar(year, Calendar.JANUARY, 01);
			Calendar chkDate2 = new GregorianCalendar(year, Calendar.JULY, 01);

			if ((do2.getFromDate().equals(chkDate1.getTime()))) {
				dni = chkDate2.getTime();
				return dni;
			} else {
				dni = chkDate2.getTime();
				return DateManager.getNextIncrementDate(dni);
			}
		}
	}

	public String createSysIncrDo2(Do2 do2) {
		if (employee == null)
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getFromDate().after(DateManager.getSixthCpcDate())) {
			if (!(checkNQS(do2))) {
				employee.setIncrementDate(DateManager.getNextIncrementDate(employee.getIncrementDate()));
				empRepo.save(employee);
				return Do2Manager.FAILURE;
			}
		}
		Date dni = DateManager.getToday();
		Calendar empIncrDate = new GregorianCalendar();
		empIncrDate.setTimeInMillis(employee.getIncrementDate().getTime());
		int year = empIncrDate.get(Calendar.YEAR);
		int month = empIncrDate.get(Calendar.MONTH) + 1;
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& year == 2006 && month == 1) {
			;
		} else if (do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& year >= 2006) {
			month = 7;
		}

		int date = 01;
		if (empIncrDate.get(Calendar.DATE) != 01  || month == 7) {
			try {
				dni = DateManager.getDate(year, month, date);
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else {
			dni = employee.getIncrementDate();
		}

		Do2 incrDo2 = new Do2();
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(do2.getArmyNo());
		incrDo2.setUnitCode(do2.getUnitCode());
	//	incrDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", do2.getUnitCode()));
		incrDo2.setUnit(do2.getUnit());
		incrDo2.setDo2Group("IA");
		incrDo2.setCheckDigit(do2.getCheckDigit());
		incrDo2.setDo2Type("6");
		incrDo2.setDo2No("6666");
		incrDo2.setDo2ItemNo("666");
		incrDo2.setDo2Date(dni);
		incrDo2.setDescription("INCR");
		incrDo2.setFromDate(dni);
		incrDo2.setColumnA(null);
		incrDo2.setColumnB(null);
		if (do2.getDescription().equals("MSTHAV"))
			incrDo2.setColumnC("HAV");
		else
			incrDo2.setColumnC(do2.getColumnA());
		incrDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setReason(null);
		incrDo2.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setAuditCage(true);
		if (year == 2006 && month == 1) {
			try {
				employee.setIncrementDate(DateManager.getDate(year, 7,	date));
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else if (year == 2006 ) {
			try {
				employee.setIncrementDate(DateManager.getDate(year + 1, month,	date));
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else {
			employee.setIncrementDate(DateManager.getNextIncrementDate(employee.getIncrementDate()));
		}
		empRepo.save(employee);
		if (!(chkDupIncr(incrDo2)))
			do2Repo.save(incrDo2);
		//	Utils.getEntityManager().persist(incrDo2);
		return Do2Manager.SUCCESS;
	}

	public String createProfixDo2(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
	 	Date xFromDate = do2.getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xFromDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar incrDate = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date havfixDate = incrDate.getTime();		
		
		 
		List<Do2> chkFixDo2 = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","HAVFIX", incrDate.getTime());
				
				
		/*		Utils.getEntityManager().createQuery(
						"SELECT cfd FROM Do2 AS cfd WHERE cfd.employee=:employee AND cfd.description='HAVFIX' AND cfd.fromDate=:frdt AND " +
						" (cfd.status in ('P', 'V')) ").setParameter("employee", employee).setParameter("frdt", incrDate.getTime())
						.getResultList();*/
	
		if (chkFixDo2.size() > 0)
			return Do2Manager.FAILURE;
	
		if (employee.getNaikDate() == null &&
//			   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getMacpsStartDate())) &&
			   (employee.getAcpnaikDate()!= null && !employee.getAcpnaikDate().before(DateManager.getAcpStartDate()) &&
				employee.getAcpnaikDate().before(do2.getFromDate()))){
			return Do2Manager.FAILURE;
		} 
		
		Do2 havfixDo2 = new Do2();
		
		if (do2.getBatchNo()!=null)
			havfixDo2.setBatchNo(do2.getBatchNo());
		
		havfixDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		havfixDo2.setDo2Group(do2.getDo2Group());
		havfixDo2.setDo2Type(do2.getDo2Type());
		havfixDo2.setDo2No(do2.getDo2No());
	 	havfixDo2.setDo2ItemNo(do2.getDo2ItemNo());
		havfixDo2.setDo2Date(do2.getDo2Date());
		havfixDo2.setDescription("HAVFIX");
		if (do2.getUnit()!=null)
			havfixDo2.setUnit(do2.getUnit());
		havfixDo2.setUnitCode(do2.getUnitCode());
		havfixDo2.setEmployee(employee);
		havfixDo2.setArmyNo(do2.getArmyNo());
		havfixDo2.setCheckDigit(do2.getCheckDigit());
		havfixDo2.setFromDate(havfixDate);
		havfixDo2.setColumnA(do2.getDescription());
		havfixDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		havfixDo2.setStatus(Do2Manager.PENDING);
		havfixDo2.setAuditCage(true);
		havfixDo2.setQe(Utils.getCurrentQuarter());
		havfixDo2.setAcceptedQe(Utils.getCurrentQuarter());
		do2Repo.save(havfixDo2);
	 	//Utils.getEntityManager().persist(havfixDo2);
		//Utils.getEntityManager().flush(); 		
		
		return Do2Manager.SUCCESS;
	}	
	
	public boolean chkDupIncr(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> incrList = do2Repo.findByDescColCFromDateStatusNotEqual(employee.getId(), Do2Manager.DO2_CANCEL_STATUS, 
														"INCR",do2.getColumnC(), do2.getFromDate());
				
			/*	Utils.getEntityManager().createQuery(
				"SELECT il FROM Do2 AS il WHERE il.employee=:employee AND il.description=:desn AND il.fromDate=:frdt AND il.columnC=:colc AND il.status !=:stat")
				.setParameter("employee", employee).setParameter("desn", "INCR").setParameter("frdt", do2.getFromDate())
				.setParameter("colc", do2.getColumnC()).setParameter("stat", Do2Manager.DO2_CANCEL_STATUS).getResultList();*/
		if (incrList.size() > 0)
			return true;
		return false;
	}

	public boolean checkNQS(Do2 do2) {
		Date checkStartDate = DateManager.getToday();
		Date checkEndDate = employee.getIncrementDate();
		Calendar cal = new GregorianCalendar();
		cal.setTime(checkEndDate);
		cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
		checkStartDate.setTime(cal.getTimeInMillis());
		int nqsDays = 0;
		Date firstIncrDate = new Date(2006 - 07 - 01);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<EmployeeServiceDetail> nqsInESD = esdRepo.findNqsForMsthav(employee.getId(), Do2Manager.ESD_NQS+"", checkStartDate, checkEndDate);
								
		/*		Utils.getEntityManager().createQuery("SELECT nqs FROM EmployeeServiceDetail AS nqs "+
				" WHERE nqs.employee=:employee AND nqs.status=:status AND ((nqs.fromDate <:csd AND nqs.toDate <=:ced) OR (nqs.fromDate >=:csd AND nqs.toDate >:ced))" +
				" OR (nqs.fromDate >=:csd AND nqs.toDate <=:ced) ").setParameter("employee", employee).setParameter("csd", checkStartDate)
				.setParameter("ced", checkEndDate).setParameter("status", Do2Manager.ESD_NQS).getResultList();*/
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

	public void reverseEmployeeStatus(Do2 cDo2, Do2 rDo2) {
		employee =Utils.getCurrentEmployee(cDo2.getArmyNo());
		
		employee.setInitialMusteringDate(employee.getPreMusterDate());
		employee.setPreMusterDate(null);
		employee.setGroupCategory(employee.getPreviousGroupCategory());
		employee.setPreviousGroupCategory(null);
		employee.setRemusteringDate(null);
				
		esdPromotList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_PROMOT+"");
				
		/*		Utils.getEntityManager().createQuery(
						"SELECT ac FROM EmployeeServiceDetail ac WHERE "
								+ " ac.employee=:employee AND ac.status=:stat order by ac.fromDate desc")
				.setParameter("employee", employee).setParameter("stat", Do2Manager.ESD_PROMOT).getResultList();*/
		
		//esdRank = Utils.getRank(cDo2.getArmyNo(), cDo2.getFromDate());
		esdRank = Utils.getPborRank(cDo2.getArmyNo(), cDo2.getFromDate(),"ALLCE");

		if (esdRank!=null && esdPromotList.size()>0) {
			employee.setSubstantiveRank(esdRank);
			employee.setActingRank(esdRank);
			employee.setSubstantiveRankDate(esdPromotList.get(0).getFromDate());
			employee.setActingRankDate(esdPromotList.get(0).getFromDate());

			if (esdRank.getRankCode().equals("40"))
				employee.setHavDate(esdPromotList.get(0).getDo2().getFromDate());
			else
				employee.setHavDate(null);
			
			employee.setTermsColourService(esdRank.getColorService());
			employee.setTermsMaxAgeRetention(esdRank.getServiceRetention());
		}
	
		esdAptList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), Do2Manager.ESD_APPT+"");
				
			/*	Utils.getEntityManager().createQuery("SELECT ac FROM EmployeeServiceDetail ac WHERE "
								+ " ac.employee=:employee AND ac.status=:stat order by ac.fromDate desc")
								.setParameter("employee", employee).setParameter("stat", Do2Manager.ESD_APPT).getResultList();*/
		
		if (esdPromotList.size()>0 && esdAptList.size()>0) {
			appointmentRateList = apptRateRepo.findByAppointmentNameFromDateBetweenOrFromDateLessThanFromDateDesc(esdAptList.get(0).getDo2().getColumnA(),
					esdAptList.get(0).getDo2().getFromDate());
					
					
				/*	Utils.getEntityManager().createQuery(
							"SELECT appt FROM AppointmentRate appt WHERE appt.appointmentName = :apptName and "
									+ " ((:fromDate between appt.fromDate and appt.toDate) or "
									+ "  (:fromDate > appt.fromDate and appt.toDate is null)) order by appt.fromDate desc")
					.setParameter("apptName", esdAptList.get(0).getDo2().getColumnA())
					.setParameter("fromDate", esdAptList.get(0).getDo2().getFromDate()).getResultList();*/
			
			if (esdAptList.get(0).getFromDate().after(esdPromotList.get(0).getFromDate())) {
				employee.setAppointmentRate(appointmentRateList.get(0));
				employee.setAppointmentDate(esdAptList.get(0).getFromDate());
			} else {
				employee.setAppointmentRate(null);
				employee.setAppointmentDate(null);
			}
		} else {
			employee.setAppointmentRate(null);
			employee.setAppointmentDate(null);
		}
		
		List<Do2> do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(),"V", "UPCLAS",rDo2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND d.fromDate<:fromDate AND " +
										" d.description='UPCLAS' AND d.status='V' ORDER BY d.fromDate DESC ").setParameter("emp", employee)
										.setParameter("fromDate", rDo2.getFromDate()).getResultList();*/
		if (do2List.size()>0) {
		//	employee.setSoldierClass(do2List.get(0).getColumnB().charAt(3));
			employee.setSoldierClass(do2List.get(0).getColumnB().charAt(3)+"");
		} else {
			do2List = do2Repo.findByEmpIdFromDateLessDescAndStatus(employee.getId(),rDo2.getFromDate(),"MUSTER","V");
					
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND d.fromDate<:fromDate AND " +
				" d.description='MUSTER' AND d.status='V' ").setParameter("emp", employee)
				.setParameter("fromDate", rDo2.getFromDate()).getResultList();*/
			
			if (do2List.size()>0) {
			//	employee.setSoldierClass(do2List.get(0).getColumnB().charAt(3));
				employee.setSoldierClass(do2List.get(0).getColumnB().charAt(3)+"");
			} else {
				employee.setSoldierClass(null);
			}
			
		}
		employee.setMusterHavildarFlag(false);
		employee.setMusterHavildarDate(null);
		empRepo.save(employee);
	}

	public void get6cpcPay(AuditCage nAuditCage, AuditCage bAuditCage) {

		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime(); 
		// basic pay
		//to skip giving increment if individual is already HAV on 16052012
		// V CPC code requires some revision 
		if (nAuditCage.getGradePay()==2800) {
			;
		} else {
			if (!((xDate.after(jan01)) && (xDate.before(jul01)))) {
				if ((nAuditCage.getDo2().getDescription().equals("MSTHAV")) && 
						employee.getNaikDate() == null &&
				//	   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getAcpStartDate())) &&
					   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getMacpsStartDate())) &&
						employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){
						;
				} else {
					nAuditCage.setBasicPay((bAuditCage.getBasicPay() + Utils
						.getIncrmentRoundedToTen((bAuditCage.getBasicPay()
								+ bAuditCage.getGradePay() + (bAuditCage
								.getXGroupPay() == null ? 0 : bAuditCage
								.getXGroupPay())), bAuditCage.getFromDate())));
					if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
						nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
					}						
				}
			} 
		}
		
		/*if (!nAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) && nAuditCage.getPayLevel()!=null && nAuditCage.getPayLevel().equals("0")) {
			nAuditCage.setOldBasicPay(nAuditCage.getBasicPay());
		}*/
		// Above two lines commented and the following part added based on EME complaint - 29.05.2018
		if (!nAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) && nAuditCage.getPayLevel()!=null && nAuditCage.getPayLevel().equals("0")) {
			nAuditCage.setBasicPay((bAuditCage.getBasicPay() + Utils
					.getIncrmentRoundedToTen((bAuditCage.getBasicPay()
							+ bAuditCage.getGradePay() + (bAuditCage
							.getXGroupPay() == null ? 0 : bAuditCage
							.getXGroupPay())), bAuditCage.getFromDate())));
				if (currentPayList!=null && nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
					nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
				}
				
				if (!(xDate.after(jan01) && xDate.before(jul01))) {
					nAuditCage.setOldBasicPay(nAuditCage.getBasicPay());
				}
	 	}
		
		// grade pay
		nAuditCage.setGradePay(currentPayList.get(0).getGradePay());
		// ms pay
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
			nAuditCage.setMsPay(currentPayList.get(0).getMsp());
		}
		// X pay
	//	if (employee.getGroupCategory().getSoldierGroup() == "X") {
		if (employee.getGroupCategory().getSoldierGroup().equals("X")) {
			nAuditCage.setXGroupPay(currentPayList.get(0).getXGroupPay());
		}
		
		String soldierClass = nAuditCage.getDo2().getColumnB().charAt(3)+"";
		 
		
		// class Pay
		List<ClassPayRate> classPayRateList = classPayRepo.findByGpCatClassFromDateBetweenOrFromDateGreater(Utils.getGroupCategory(nAuditCage.getDo2(),"CUR").getId(),
				soldierClass, nAuditCage.getFromDate());

		/*		Utils.getEntityManager().createQuery("SELECT cp FROM ClassPayRate AS cp WHERE cp.groupCategory =:groupCategory "
				+ " AND cp.classPayCode =:soldierClass AND  ((:fromDate BETWEEN cp.fromDate AND cp.toDate) OR (:fromDate > cp.fromDate AND cp.toDate IS null))")
				.setParameter("groupCategory", Utils.getGroupCategory(nAuditCage.getDo2(),"CUR").getId())
				.setParameter("soldierClass", soldierClass).setParameter("fromDate", nAuditCage.getFromDate()).getResultList();*/
		if (classPayRateList.size() > 0	&& classPayRateList.get(0).getRate() > 0) {
			nAuditCage.setClassPay((int) Math.round(classPayRateList.get(0).getRate()));
		}
	}

	public void get5cpcPay(AuditCage nAuditCage, AuditCage bAuditCage) {		
		
		Rank oldRank = Utils.getPborRank(employee.getArmyNo(), nAuditCage.getFromDate(),"PAYFIX");
		
		Date fifthCpcDate = null;
		try {
			fifthCpcDate = DateManager.getDate("2005-12-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		  	
		GroupCategory gp = Utils.getGroupCategory(nAuditCage.getDo2(),"CUR");
		
		String catg = null;
		catg = gp.getSoldierCategory();
		List<GroupCategory> grpCatList=null;
		try {
			grpCatList = gcRepo.findByFromDateAndRedesgAbbrNotNull(DateManager.getDate("1997-10-10"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		/*		Utils.getEntityManager().createQuery(
		"SELECT gc FROM GroupCategory gc WHERE gc.redesignatedAbbr is not null and from_date='1997-10-10'").getResultList();*/
		if (grpCatList.size() > 0 ) {
			for (GroupCategory gc1 : grpCatList) {
				if (gc1.getRedesignatedAbbr().equals(catg)){
					catg=gc1.getSoldierCategory();
					gp=gcRepo.findBySoldierCategory(catg).get(0);
				//	gp  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
				}
			}				
		}
		previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(oldRank.getId(),
														gp.getSoldierGroup(),fifthCpcDate);
				
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
						+ " AND ps.soldierGroup.soldierGroup=:grp "
						+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
						+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
						+ " AND ps.startScale is NOT NULL")
		.setParameter("rnk", oldRank)
		.setParameter("grp", gp.getSoldierGroup())
		.setParameter("fromDate", fifthCpcDate)
		.getResultList();*/
		
		
		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(employee.getSubstantiveRank().getId(),
															gp.getSoldierGroup(),fifthCpcDate);
				
				
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
						+ " AND ps.soldierGroup.soldierGroup=:grp "
						+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
						+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))"
						+ " AND ps.startScale is NOT NULL")
		.setParameter("rnk", Utils.getForeignKey(new Rank(), "rankCode",
				employee.getSubstantiveRank().getRankCode()))
		.setParameter("grp",gp.getSoldierGroup())
		.setParameter("fromDate", fifthCpcDate)
		.getResultList();*/
		int oldBasicPay = bAuditCage.getBasicPay();
		int notionalPay = (oldBasicPay + previousPayList.get(0).getIncr0());
		int newBasicPay = 0;
		Rank previousRank = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(),"PAYFIX");
		// for the PBORs getting remustered from Hav to Msthav
		if (previousRank.getRankCode().equals("40")){
			notionalPay = oldBasicPay;
		}
		// basic Pay
		for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList
				.get(0).getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
			if (i > notionalPay) {
				newBasicPay = i;
				break;
			}
		}

		nAuditCage.setBasicPay(newBasicPay);
		if (newBasicPay < currentPayList.get(0).getStartScale())
			nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());

		// d pay
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getDPDate()))) {
			if (nAuditCage.getDo2().getFromDate().before(
					DateManager.getSixthCpcDate()))
				nAuditCage.setDp((int) Math
						.round(nAuditCage.getBasicPay() * 0.50));
		}
		// class pay
		List<ClassPayRate> classPayRateList = classPayRepo.findByGpCatClassFromDateBetweenOrFromDateGreater(employee.getGroupCategory().getId(),
																employee.getSoldierClass(), nAuditCage.getFromDate());
				
				
			/*	Utils
				.getEntityManager()
				.createQuery(
						"SELECT cp FROM ClassPayRate AS cp WHERE cp.groupCategory =:groupCategory "
								+ " AND cp.classPayCode =:soldierClass AND "
								+ " ((:fromDate BETWEEN cp.fromDate AND cp.toDate) OR (:fromDate > cp.fromDate AND cp.toDate IS null))")
				.setParameter("groupCategory", employee.getGroupCategory())
				.setParameter("soldierClass", employee.getSoldierClass())
				.setParameter("fromDate", nAuditCage.getFromDate())
				.getResultList();*/
		if (classPayRateList.size() > 0
				&& classPayRateList.get(0).getRate() > 0) {
			nAuditCage.setClassPay((int) Math.round(classPayRateList.get(0)
					.getRate()));
		}
	}
	public void updateOldBasicPay(AuditCage newAuditCage,AuditCage baseAuditCage,Rank previousRankList){
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		
		try{
		previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(previousRankList.getId(),
				employee.getGroupCategory().getSoldierGroup(), DateManager.getDate("1997-10-10"));
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale is NOT NULL")
				.setParameter("rnk", previousRankList).setParameter("grp",
						employee.getGroupCategory().getSoldierGroup())
				.setParameter("fromDate", DateManager.getDate("1997-10-10"))
				.getResultList();*/

		

		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(employee.getSubstantiveRank().getId(),
											employee.getGroupCategory().getSoldierGroup(),DateManager.getDate("1997-10-10"));
				
				
		/*		Utils
				.getEntityManager()
				.createQuery(
						"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale is NOT NULL")
				.setParameter(
						"rnk",
						Utils.getForeignKey(new Rank(), "rankCode", employee
								.getSubstantiveRank().getRankCode()))
				.setParameter("grp",
						employee.getGroupCategory().getSoldierGroup())
				.setParameter("fromDate", DateManager.getDate("1997-10-10"))
				.getResultList();*/
		int oldBasicPay = baseAuditCage.getOldBasicPay();
		int notionalPay = (oldBasicPay + previousPayList.get(0).getIncr0());
		int newBasicPay = 0;
		// basic Pay
		for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList
				.get(0).getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
			if (i > notionalPay) {
				newBasicPay = i;
				break;
			}
		}

		newAuditCage.setOldBasicPay(newBasicPay);
		if (newBasicPay < currentPayList.get(0).getStartScale())
			newAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public boolean isIncrDo2Generated(Do2 do2) {
		List<Do2> incrDo2List = do2Repo.findByEmployeeIdAndDescriptionAndStatusAndColumnC(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),
																					"INCR","V","MSTHAV"); 
				 
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND" +
				" d.description='INCR' AND d.status='V' AND d.columnC='MSTHAV'").setParameter("emp", Utils.getCurrentEmployee(do2.getArmyNo()))
				.getResultList();*/
		return (incrDo2List.size()>0);
	}
	
public boolean isClassPayToBeSetToZero(Do2 do2) {
		
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		char do2colA=do2.getColumnA().charAt(3);
		char do2colB=do2.getColumnB().charAt(3);
	//	System.out.println ( " do2 cola " +do2colA  + " do2 colb " +do2colB);
	//	System.out.println ( " group category " +employee.getPreviousGroupCategory().getSoldierGroup());
	//	System.out.println ( " soldier class " +employee.getSoldierClass());
	//	System.out.println ( " employee class pay date  " +employee.getCurrentClassPayDate()    +" do2fromdate " +do2.getFromDate());
		
		if (employee.getPreviousGroupCategory()!=null && employee.getPreviousGroupCategory().getSoldierGroup().equals("X") && 
				do2colA=='X' && do2colB=='1') {
		//	System.out.println ( " IN CLSS PAY ZERO ");
			if (employee.getSoldierClass().equals("1") && employee.getCurrentClassPayDate().before(do2.getFromDate())) {
		//		System.out.println ( " IN FALSE " +employee.getSoldierClass()    );
					return false;
				}
			}
	//	System.out.println ( " iN TRUE ");
		/* 
		if (employee.getPreviousGroupCategory() !=null && employee.getPreviousGroupCategory().getSoldierGroup()=="X" && 
				do2.getColumnA().charAt(3)+""=="X"&& do2.getColumnB().charAt(3)+""=="1") {
			if (employee.getSoldierClass()=="1" && employee.getCurrentClassPayDate().before(do2.getFromDate())) {
					return false;
				}
			}*/
		return true;
	}
	
	
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}	
	
	public Date sixcpcDo2Exists(Do2 do2){
		List<Do2> do2List = do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(do2.getEmployee().getId(),"SIXCPC",Do2Manager.VALID);
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status=:status and d.description=:description")
													.setParameter("emp",do2.getEmployee())
													.setParameter("description","SIXCPC")
													.setParameter("status",Do2Manager.VALID)
													.getResultList();*/
		
        if(do2List.size()>0){
        	return do2List.get(0).getFromDate();
        }
        return null;
	}	

	public void get7cpcPay(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		
		if (nAuditCage.getDo2().getDescription().equals("MSTHAV"))
			qRank = rankRepo.findByRankCode("40");
		//	qRank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "40");
				
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(),"CUR");
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}
			if (nAuditCage.getDo2().getColumnA()!=null) {
			soldierGroup = nAuditCage.getDo2().getColumnA().charAt(3)+"";
	
		}
		
		if (soldierGroup == "Z" && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}
		
			

		
		Integer newBasic = nAuditCage.getBasicPay();
		 		
		Rank currentRank = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(),"PAYFIX");   	
		
		String currentPayLevel = Do2Manager.getPayLevelPromot(currentRank.getRankCode());
		String promotPayLevel = Do2Manager.getPayLevelPromot(qRank.getRankCode());
		
	 	System.out.println ( " ARMY NO " +nAuditCage.getDo2().getArmyNo());
		System.out.println( "current rank " +currentRank.getRankCode() );
		System.out.println( " Q rank " +qRank.getRankCode());
		System.out.println (" current pay level "  +currentPayLevel);
		System.out.println( " promot pay level" +promotPayLevel);
		System.out.println (" soldier group " +soldierGroup);
	 	
		if (currentPayLevel!=null && promotPayLevel!=null && currentPayLevel.equals(promotPayLevel)) {
			if (soldierGroup=="X") {
				if (bAuditCage.getXGroupPay()==0) {
					nAuditCage.setXGroupPay(3600); 
				} else {
					nAuditCage.setXGroupPay(bAuditCage.getXGroupPay()); 
				}
			} 
			return;
		}
		boolean noPayFixationOnMsthav = false;
		if (currentPayLevel!=null && promotPayLevel!=null 
				&& (Integer.parseInt(currentPayLevel)<Integer.parseInt(promotPayLevel))) {
			if (employee!=null && employee.getAcpnaikDate()!=null 
					&& (employee.getNaikDate()==null || (employee.getNaikDate()!=null && employee.getNaikDate().after(nAuditCage.getFromDate())))) {
			noPayFixationOnMsthav = true;	
			}
			
		}
		
		 
		 
		List<Do2> optfixDo2List = do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(nAuditCage.getEmployee().getId(),
				nAuditCage.getFromDate(), "V", "OPTFIX");
				
		/*		
				Utils.getEntityManager().createQuery("select d from Do2 d where d.description='OPTFIX' and d.status='V' "
				+ " and d.fromDate=:macpsDate and d.employee=:emp ").setParameter("emp", nAuditCage.getEmployee())
				.setParameter("macpsDate", nAuditCage.getFromDate()).getResultList();*/
 		if ((optfixDo2List!=null && optfixDo2List.size()>0)	
 				|| (nAuditCage.getDo2().getToDate()!=null)
 				|| (noPayFixationOnMsthav==true)) {

			List<PayMatrix>paymatrixList1 = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel,newBasic);
					
				/*	Utils.getEntityManager().createQuery( 
					 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
					+ " pm.basicPay>=:sevencpcpay order by basicPay asc ")
					.setParameter("paylevel", promotPayLevel)
					.setParameter("sevencpcpay", newBasic)
					.getResultList();*/
			
			if (paymatrixList1.size() > 0) {
				newBasic = paymatrixList1.get(0).getBasicPay();
			}
		
			nAuditCage.setBasicPay(newBasic);
			nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
			nAuditCage.setPayLevel(promotPayLevel);
	//		if (soldierGroup=="X") {
			if (soldierGroup.equals("X")) {
				if (bAuditCage.getXGroupPay()==0) {
					nAuditCage.setXGroupPay(3600); 
				} else {
					nAuditCage.setXGroupPay(bAuditCage.getXGroupPay()); 
				}
			} 
			 
 			return;
		} 
 		if (currentPayLevel!=null && promotPayLevel!=null 
				&& (Integer.parseInt(currentPayLevel)<Integer.parseInt(promotPayLevel))) {
				List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentPayLevel,newBasic);
						
			/*			Utils.getEntityManager().createQuery("select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
						+ "  pm.basicPay>:sevencpcpay order by basicPay asc ").setParameter("paylevel", currentPayLevel)
						.setParameter("sevencpcpay", newBasic).getResultList();*/
				
				if (paymatrixList.size() > 0) {
					newBasic = paymatrixList.get(0).getBasicPay();
				}
				
				List<PayMatrix>paymatrixList1 = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel,newBasic);
						
				/*		Utils.getEntityManager().createQuery("select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
						+ " pm.basicPay>=:sevencpcpay order by basicPay asc ").setParameter("paylevel", promotPayLevel)
						.setParameter("sevencpcpay", newBasic).getResultList();*/
				
				if (paymatrixList.size() > 0) {
					newBasic = paymatrixList1.get(0).getBasicPay();
				}
				nAuditCage.setPayLevel(promotPayLevel);
				if (nAuditCage.getDo2().getToDate()==null) {
					if (employee!=null && employee.getAcpnaikDate()!=null && employee.getNaikDate()==null) {
						;
					} else if (employee!=null && (employee.getAcphavDate()!=null || employee.getHavDate()!=null)) {
						;
					} else if (employee!=null && employee.getAcpnaikDate()!=null && employee.getAcphavDate()!=null) {
						;
					} else {
						employee.setIncrementDate(Utils.getSevenPcNextIncrementDate(nAuditCage.getDo2().getFromDate()));	
						empRepo.save(employee);
					}					
				}		
 		}
		
	//	System.out.println (" basic pay " +newBasic  +" rank " +currentRank.getRankCode() +"EMPLOYEE " +employee.getArmyNo());
		nAuditCage.setBasicPay(newBasic);
		nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
		
	//	if (soldierGroup=="X") {
		if (soldierGroup.equals("X")) {
			nAuditCage.setXGroupPay(3600); 
		}
		
	 
		String soldierClass = nAuditCage.getDo2().getColumnB().charAt(3)+"";
		
		// class Pay
		List<ClassPayRate> classPayRateList = classPayRepo.findByGpCatClassFromDateBetweenOrFromDateGreater(Utils.getGroupCategory(nAuditCage.getDo2(),"CUR").getId(),
				soldierClass,nAuditCage.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT cp FROM ClassPayRate AS cp WHERE cp.groupCategory =:groupCategory "
								+ " AND cp.classPayCode =:soldierClass AND ((:fromDate BETWEEN cp.fromDate AND cp.toDate) OR (:fromDate > cp.fromDate AND cp.toDate IS null))")
				.setParameter("groupCategory", Utils.getGroupCategory(nAuditCage.getDo2(),"CUR"))
				.setParameter("soldierClass", soldierClass).setParameter("fromDate", nAuditCage.getFromDate()).getResultList();*/
		
		if (classPayRateList.size() > 0 && classPayRateList.get(0).getRate() > 0) {
			nAuditCage.setClassPay((int) Math.round(classPayRateList.get(0).getRate()));
		}
		
	 	// basic pay
		// The following lines commented hoping that the situtation does not arise in 7cpc since pay fixation based on pay level
		// previously only 3% increase on promotion.
		
		
		/* Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		 * 
		 * 
		 * if ((xDate.after(jan01) && xDate.before(jul01)) && 
			(employee.getFsDueDate() == null || (employee.getFsDueDate() != null && !employee.getFsDueDate().before(jul01)))) {
			if ((nAuditCage.getDo2().getDescription().equals("PROMOT") ||
				(nAuditCage.getDo2().getDescription().equals("MSTJCO"))) && nAuditCage.getDo2().getColumnA().equals("NBSUB")) {

				if ((nAuditCage.getBasicPay() < newBasic && nAuditCage.getXGroupPay()==0)
						|| (nAuditCage.getBasicPay() < newBasic && nAuditCage.getXGroupPay()==1400)) {
					nAuditCage.setBasicPay(newBasic);
				}
			}
		} else {
			// Added to take care of MACPS condition - if the PBOR is ACPNK and getting direct SPHAV/GRHAV from sepoy, then there is no 3% pay fixation
			// only increase in grade pay (ie 2400 to 2800)
			if	((nAuditCage.getDo2().getDescription().equals("MSTHAV") || 
				  (nAuditCage.getDo2().getDescription().equals("PROMOT") && nAuditCage.getDo2().getColumnA().equals("SPHAV")) ||
			      (nAuditCage.getDo2().getDescription().equals("PROMOT") && nAuditCage.getDo2().getColumnA().equals("GRHAV"))) && 
			    employee.getNaikDate() == null &&
		        (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
			    employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){
				;
			} else if ((nAuditCage.getDo2().getDescription().equals("MSTJCO"))  && 
					employee.getNaikDate() == null &&
				   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
					employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))){
						if (nAuditCage.getBasicPay() < newBasic) {
							nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
						}
				;
			} else {
				if (nAuditCage.getDo2().getDescription().equals("MSTHAV") && nAuditCage.getGradePay() == 2800){
					;					
				}  else if (Utils.isPromotionAfterReversionAfter010106(nAuditCage.getDo2())){
                    ;
                } else {
					nAuditCage.setBasicPay(bAuditCage.getBasicPay()
						+ (Utils.getIncrmentRoundedToTen(bAuditCage.getBasicPay()
								+ bAuditCage.getGradePay()
								+ (bAuditCage.getXGroupPay() == null ? 0: bAuditCage.getXGroupPay()), 
								bAuditCage.getFromDate())));
					if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
						nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
					}
				}
			}
		} 
			
		// grade pay
		nAuditCage.setGradePay(currentPayList.get(0).getGradePay());
		// ms pay
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager
				.getSixthCpcAllowanceDate()))) {
			nAuditCage.setMsPay(currentPayList.get(0).getMsp());
		}
		// X pay
		if (soldierGroup=='X') {
			nAuditCage.setXGroupPay(currentPayList.get(0).getXGroupPay()); 
		} */
		
		// minimum xgroup pay set 
		/*if (soldierGroup=='X') {
			nAuditCage.setXGroupPay(3600); 
		} */
		
		// class Pay
		// If PROMOT to JCO Rank removing Class Pay
		if (Do2Manager.getJcoRankDescription().contains(nAuditCage.getDo2().getColumnA())){
			nAuditCage.setClassPay(0);
		}
			
		// Transport Allowance
		if (nAuditCage.getTransport() != null && nAuditCage.getTransport() !=0) {
			nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage,bAuditCage));
		}
	}	
	
	public void get6cpcOldPay(AuditCage nAuditCage, AuditCage bAuditCage) {

		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();		 
		// basic pay
		//to skip giving increment if individual is already HAV on 16052012
		// V CPC code requires some revision 
		if (nAuditCage.getGradePay()>=2800) {
			;
		} else {
			if (!(xDate.after(jan01) && xDate.before(jul01))) {
				if ((nAuditCage.getDo2().getDescription().equals("MSTHAV")) && employee.getNaikDate() == null && 
					   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getMacpsStartDate())) &&
						employee.getAcpnaikDate().before(nAuditCage.getDo2().getFromDate()))) {
						;
				} else {
					nAuditCage.setOldBasicPay((bAuditCage.getOldBasicPay() + Utils.getIncrmentRoundedToTen((bAuditCage.getOldBasicPay()
								+ bAuditCage.getOldGradePay() + (bAuditCage.getXGroupPay() == null ? 0 : bAuditCage.getXGroupPay())), bAuditCage.getFromDate())));
					if (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale())
						nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
				}
			}
		}
		if (nAuditCage.getOldGradePay()<2400)
		nAuditCage.setOldGradePay(currentPayList.get(0).getGradePay());			
	}
	
	
	public boolean isPborOnRegularLeave(Do2 do2, Employee employee) {
        Date adjustedQeDate = null; 
		Date dolphinStartDate = Utils.getDolphinStartDate();
		if (do2.getAcceptedQe() != null) {
			try {
				adjustedQeDate = DateManager.getDate(do2.getAcceptedQe().substring(3)  + "-" +do2.getAcceptedQe().substring(0,2) + "-01");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}							 
		}
		if (adjustedQeDate != null && adjustedQeDate.before(dolphinStartDate)){
			return false;
		}
		if (do2.getColumnA().equals("TSNK")) {
			return false;
		}
		List<String> descList=Arrays.asList(new String[] {"CL","EXTNCL"});
		List<Do2> do2List = do2Repo.findForMultipleDescriptionsWithFromDateBetween(employee.getId(),Do2Manager.VALID,
												Do2Manager.PENDING,descList,do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
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
		List<Do2> do2List = do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(employee.getId(),do2.getToDate(),"V","INCR");
				
				
			/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
		 		+ " and d.employee=:emp and d.status='V' and d.fromDate=:toDate ")
				.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
				.getResultList();*/
		
		if (do2List!=null && do2List.size()>0) {
			return true;
		} else {
			do2List=null;
			do2List = do2Repo.findByDescStatusFromDateLessThanFromDateDesc(employee.getId(),"V","INCR",do2.getToDate());
			
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
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
		 		if (do2.getToDate().equals(nextIncrDate.getTime())) {
					return true;
				} else {
					Do2 previousIncrDo2 = do2List.get(0);
					do2List=null;
					do2List = do2Repo.findByDescriptionsAndFromDate(employee.getId(), previousIncrDo2.getFromDate());
					System.out.println("............profixacpfixhavfix list size " +do2List.size());
					if (do2List!=null && do2List.size()>0) {
						nextIncrDate.setTime(previousIncrDo2.getFromDate());
			 			int month =nextIncrDate.get(Calendar.MONTH);
			 			System.out.println("........year .... " +year+"  month........." +month);
			 			if (month==Calendar.JANUARY) {
			 				try {
								nextIncrDate.setTime(DateManager.getDate(year--, nextIncrDate.get(Calendar.JULY), nextIncrDate.get(Calendar.DATE)));
							} catch (Exception e) {
								Utils.handleException(e);
							}
			 			} else {
			 				try {
								nextIncrDate.setTime(DateManager.getDate(year, nextIncrDate.get(Calendar.JANUARY), nextIncrDate.get(Calendar.DATE)));
							} catch (Exception e) {
								Utils.handleException(e);
							}
			 			}				
			 			System.out.println("...........nextincrdate " +nextIncrDate.getTime()+"  do2todate "+do2.getToDate()); 
						return (do2.getToDate()!=null && do2.getToDate().equals(nextIncrDate.getTime()));
					} else {
						return false;
					}
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
