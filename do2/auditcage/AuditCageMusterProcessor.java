package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 
 
 
@RestController
public class AuditCageMusterProcessor extends SpecialProcessor {

	/*public static String NOT_A_RECRUIT = "PBOR Is not a Recruit";

	@Override
	public String process(Do2 do2) {

		updateEmployee(do2);
		if (!isEnrMstDo2Exist(do2)) {
			createSysDo2(do2);
		}
		if (!AuditCageHelper.isTdmergDo2Exist(do2)){
			String presentTrade = null;
			String sldrClass = null;
			char group =' ';
			String previousTrade=do2.getColumnC().trim();
			if (do2.getColumnB()!=null){
				sldrClass = do2.getColumnB().trim();
			}
			if (do2.getColumnA()!=null && do2.getColumnA().length()>3){
				group = do2.getColumnA().charAt(3);
			}
			List<GroupCategory> grpCatList = Utils.getEntityManager().createQuery(
				"SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat ")
				.setParameter("sgrp", group ).setParameter("scat", previousTrade).getResultList();
				if ((grpCatList.size() > 0)	&& (previousTrade.equals(grpCatList.get(0).getSoldierCategory()))) {
					 if (grpCatList.get(0).getMergedAbbr() != null){				
						presentTrade = grpCatList.get(0).getMergedAbbr();
						AuditCageHelper.createMergedTradeDo2(do2, sldrClass, previousTrade, presentTrade);
					 }
				}				
		}
		if (do2.getId() == 0) {
			Utils.getEntityManager().persist(do2);
		} else {
			Utils.getEntityManager().merge(do2);
		}

		Utils.getEntityManager().flush();

		return Do2Manager.SUCCESS;
	}

	public boolean isValidRecruit(Do2 do2) {
		boolean recruit = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (!(employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT))) {
			do2.setStatus(Do2Manager.INVALID);
			recruit = false;
		}

		if (employee.getSubstantiveRankDate() == null
				|| employee.getInitialMusteringDate() != null
				|| employee.getCurrentClassPayDate() != null
				|| employee.getSoldierClass() != null) {

			recruit = false;
		}
		return recruit;
	}

	private boolean isEnrMstDo2Exist(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		List<Do2> do2List = Utils.getEntityManager().createQuery(
						"SELECT d FROM Do2 d WHERE d.employee=:employee AND d.description=:description AND d.status in ('V','P')")
				.setParameter("employee", employee)
				.setParameter("description","ENRMST")
				.getResultList();

		return (do2List.size() != 0);
	}

	private boolean isTdmergDo2Exist(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());

		List<Do2> do2List = Utils.getEntityManager().createQuery(
						"SELECT d FROM Do2 d WHERE d.employee=:employee AND d.description=:description AND d.status in ('V','P')")
				.setParameter("employee", employee)
				.setParameter("description","TDMERG")
				.getResultList();

		return (do2List.size() != 0);
	}

	public void createSysIncrDo2(Do2 do2, Employee employee) {
		Date dni = DateManager.getToday();
		Calendar empIncrDate = new GregorianCalendar();
		empIncrDate.setTimeInMillis(employee.getIncrementDate().getTime());
		int year = empIncrDate.get(Calendar.YEAR);
		int month =  empIncrDate.get(Calendar.MONTH) + 1;
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& year == 2006 && month == 1) {
			;
		} else if (do2.getFromDate().before(DateManager.getSixthCpcDate())
				&& year >= 2006) {
			month = 7;
		}

		int date = 01;
		if (empIncrDate.get(Calendar.DATE) != 01 || month == 7) {
			try {
				dni = DateManager.getDate(year, month, date);
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else {
			dni = employee.getIncrementDate();
		}
		List<Do2> duplicateDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
		" d.employee=:emp AND d.description='INCR' AND d.status in ('V', 'P') AND d.fromDate = :fromDate")
		.setParameter("emp", employee)
		.setParameter("fromDate", dni).getResultList();

		if (duplicateDo2List == null || duplicateDo2List.size() == 0){
			Do2 incrDo2 = (Do2) Utils.getClonedEntity(do2);
			incrDo2.setEmployee(employee);
			incrDo2.setArmyNo(do2.getArmyNo());
			incrDo2.setUnitCode(do2.getUnitCode());
			incrDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", do2
					.getUnitCode()));
			incrDo2.setDo2Group("IA");
			incrDo2.setCheckDigit(do2.getCheckDigit());
			incrDo2.setDo2Type('6');
			incrDo2.setDo2No("6666");
			incrDo2.setDo2ItemNo("666");
			incrDo2.setDo2Date(dni);
			incrDo2.setDescription("INCR");
			incrDo2.setFromDate(dni);
			incrDo2.setColumnA(null);
			incrDo2.setColumnB(null);
			incrDo2.setColumnC("MUSTER");
			incrDo2.setStatus(Do2Manager.PENDING);
			incrDo2.setReason(null);
			incrDo2.setQe(Utils.getCurrentQuarter());
			incrDo2.setInputQe(do2.getInputQe());
			incrDo2.setCreatedAt(DateManager.getCurrentDate());
			incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			incrDo2.setAuditCage(true);
			Utils.getEntityManager().persist(incrDo2);
			Utils.getEntityManager().flush();
		}
		if (year == 2006 && month == 1) {
			try {
				employee.setIncrementDate(DateManager.getDate(year, 7,
						date));
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

		Utils.getEntityManager().persist(employee);
		Utils.getEntityManager().flush();
	}

	public Date musterIncrCal(Do2 do2) {
		if (employee == null) {
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		}
		Date incrementDate = DateManager.getToday();
		if (employee.getInitialMusteringDate().before(
				DateManager.getSixthCpcDate())) {
			incrementDate = DateManager.getNextIncrementDate(do2.getFromDate());
		} else {
			Date musterDate = do2.getFromDate();
			incrementDate = new Date(musterDate.getTime());
			Calendar cal = new GregorianCalendar();
			cal.setTime(musterDate);
			try {
				if (cal.get(Calendar.MONTH) == Calendar.JANUARY
						&& cal.get(Calendar.DATE) == 1) {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "07-01");
				} else {
					incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + 1 + "07-01");
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}
		return incrementDate;
	}

	public Date musterAttestIncrCal(Employee employee) {
		Date incrementDate = DateManager.getToday();
		Date empMusterAttDate=null;
		if (employee.getAttestationDate().before(employee.getInitialMusteringDate())) {
			if (DateManager.getNoOfDays(employee.getAttestationDate(), employee.getInitialMusteringDate()) > 364){
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
                                                    incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + "07-01");	
                                                } else if (cal.get(Calendar.MONTH) == Calendar.JULY
							&& cal.get(Calendar.DATE) == 1) {
						    incrementDate = cal.getTime();
						} else {
						    incrementDate = DateManager.getDate(cal.get(Calendar.YEAR) + 1 + "07-01");
						}
					} catch (Exception e) {
						Utils.handleException(e);
		 			}
                                 }
			} else {
				empMusterAttDate= DateManager.getNextIncrementDate(employee.getAttestationDate());
				if (empMusterAttDate.before(DateManager.getSixthCpcDate())) {
					Calendar cal = new GregorianCalendar();
					cal.setTime(empMusterAttDate);
					cal.set(Calendar.DATE,1);
			             	incrementDate = cal.getTime();
			        } else if (!empMusterAttDate.before(employee.getInitialMusteringDate())) {
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
	                                 incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)
							+ 1 + "07-01");
		                     } catch (Exception e) {
				         Utils.handleException(e);
				}
			}
			
			}
		}
		return incrementDate;
		
	}

	
	public void updateEmployee(Do2 do2) {
		String trade = do2.getColumnC().trim();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setInitialMusteringDate(do2.getFromDate());
		employee.setCurrentClassPayDate(do2.getFromDate());
		List<GroupCategory> grpCatList = Utils.getEntityManager().createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
		.setParameter("sgrp", do2.getColumnA().charAt(3))
		.setParameter("scat", do2.getColumnC().trim())
		.getResultList();
		
		if (grpCatList.size() > 0 ) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null){
				trade = grpCatList.get(0).getRedesignatedAbbr();
			} else if (grpCatList.get(0).getMergedAbbr() != null) {
				trade = grpCatList.get(0).getMergedAbbr();
			} else {
				trade = grpCatList.get(0).getSoldierCategory();
			}
		}	
				
		GroupCategory grpcat = new GroupCategory();
		employee.setGroupCategory((GroupCategory) Utils.getForeignKey(grpcat,"SoldierCategory", trade));
		employee.setSoldierClass(do2.getColumnB().charAt(3));
		Rank rank = new Rank();

		if (employee.getSportHavildarFlag() != null	&& employee.getSportHavildarFlag() == true) {
			employee.setSubstantiveRank((Rank) Utils.getForeignKey(rank, "rankCode", "40"));
		} if (employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true) {
			employee.setSubstantiveRank((Rank) Utils.getForeignKey(rank, "rankCode", "40"));
		} else {
			employee.setSubstantiveRank((Rank) Utils.getForeignKey(rank,"rankCode", "20"));
		}
		employee.setSubstantiveRankDate(do2.getFromDate());
		List <Rank> rankList = null;
		if (employee.getGraduateHavildarFlag()!=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
	    } else {
	    	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='20'").getResultList();
	    }	
		employee.setTermsColourService(rankList.get(0).getColorService());
		employee.setTermsMaxAgeRetention(rankList.get(0).getMaximumAge());

		Date chk6cpcDate = DateManager.getToday();
		Calendar sixCpc = new GregorianCalendar(2005, Calendar.DECEMBER, 31);
		chk6cpcDate.setTime(sixCpc.getTimeInMillis());

		if ((employee.getAttestationDate() == null)
				|| employee.getAttestationDate().equals(employee.getInitialMusteringDate())
				|| employee.getAttestationDate().after(employee.getInitialMusteringDate())) {
			employee.setIncrementDate(musterIncrCal(do2));
		} else {
			employee.setIncrementDate(musterAttestIncrCal(employee));
		}

		if (!(Utils.isDateAfterCurrentQuarterEnding(employee.getIncrementDate()))) {
			boolean done = false;
			for (; !done ;) {
				createSysIncrDo2(do2, employee);
				if (Utils.isDateAfterCurrentQuarterEnding(employee.getIncrementDate())) {
					done = true;
				}
			}
		}
	}

	private void createSysDo2(Do2 do2) {
		// Generation Of ENRMST DoII To Start Sepoy Pay From Enrollment Date
		Do2 enrMstDo2 = (Do2) Utils.getClonedEntity(do2);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		enrMstDo2.setDescription("ENRMST");
		//CODE AMENDED FOR ta WHERE THE ENRMST FROM DATE WILL BE 01032012
		if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag()=='T'){
			
			if(AuditCageHelper.isBaseAfterEnrollDate(do2)){
			
				enrMstDo2.setFromDate(DateManager.getNextDate(DateManager.getTaCutOffDate()));
				
			}else{
				enrMstDo2.setFromDate(employee.getEnrolmentDate());
			}
		}else{
			enrMstDo2.setFromDate(employee.getEnrolmentDate());	
		}
		
		enrMstDo2.setEmployee(employee);
		enrMstDo2.setArmyNo(do2.getArmyNo());
		enrMstDo2.setUnitCode(do2.getUnitCode());
		enrMstDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode",
				do2.getUnitCode()));
		enrMstDo2.setDo2Group("IA");
		enrMstDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		enrMstDo2.setStatus(Do2Manager.PENDING);
		enrMstDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		enrMstDo2.setAuditCage(true);
		enrMstDo2.setCreatedAt(new Date());
		enrMstDo2.setReason(null);
		enrMstDo2.setQe(Utils.getCurrentQuarter());
		enrMstDo2.setInputQe(do2.getInputQe());
		Utils.getEntityManager().persist(enrMstDo2);

		AuditCageHelper.insertDaDo2s(employee, do2);
		AuditCageHelper.insertLraDo2s(employee, do2);

		// Below Statement Is For Generating ALWREV For MSPay (01.09.2008)
		if (employee.getInitialMusteringDate().before(
				DateManager.getSixthCpcAllowanceDate())) {
			List<Do2> duplicateDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='ALWREV' AND d.status in ('V', 'P') AND d.fromDate = '2008-09-01'")
			.setParameter("emp", employee).getResultList();

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager
					.getSixthCpcAllowanceDate(), do2);
			}
		}

		// Below Statement Is For Generating ALWREV For Dearness Pay
		// (01.04.2004)
		if (employee.getEnrolmentDate().before(DateManager.getDPDate())) {
			List<Do2> duplicateDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='ALWREV' AND d.status in ('V', 'P') AND d.fromDate = '2004-04-01'")
			.setParameter("emp", employee).getResultList();

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager
					.getDPDate(), do2);
			}
		}

		// Below Statement Is For Generating PAYFIX (01.01.2006)
		if (!(employee.getEnrolmentDate().after(DateManager.getSixthCpcDate()))) {
			List<Do2> duplicateDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='PAYFIX' AND d.status in ('V', 'P') AND d.fromDate = '2006-01-01'")
			.setParameter("emp", employee).getResultList();

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertPayfixDo2(employee, DateManager
						.getSixthCpcDate());
			}
		}
	}	*/
}
