package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.Unit;  
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager; 
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils; 
/* 
 * author : IT & SDC (Secunderabad) 
 */
@RestController
public class Muster extends AuditCageBase implements Do2AccountingCageProcessor, 
		Do2AuditCageProcessor, java.io.Serializable {	
 
	public boolean musterValidity = false;
	StringBuffer buffer = new StringBuffer();
	List<ClassPayRate> clsPayList = null;
	
 	public static final String INVALID_GROUP = "AU:Invalid group as on From date.\n";
	public static final String INVALID_CLASS_CATEGORY = "AU:Class/category not found in the group.\n";
	public static final String NO_PAY_SCALE_FOUND = "AU:Pay scale not found.\n";
	public static final String IS_HONY_RANK = "AU:PBOR is holding honorary rank as on From date.\n";
	//public static final String ATTEST_MUSTER_GROUP_CATEGORY_DIFFERS = "AU:Attestation/Mustering Group/Category differs. \n ";
	public static final String ALREADY_MUSTERED = "AU:Already mustered as per employee table. \n";
	public static final String INVALID_FROM_DATE = "VL:Mustering date is before enrolment date. \n";
	public static final String ENROLL_JCO = "AU: Enrolled as Direct JCO. Muster do2 need not to be published. \n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	
	public Employee employee;
	
	String gpcatg = null;
	@Override
	public boolean isValid(Do2 do2) {
	
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		musterValidity = true;
		 
		if(newDo2 != null && newDo2.getId()==do2.getId()){
			if (!AuditCageHelper.isValidZcategory(do2)){
				do2.setStatus("MR");
				do2.setReason("VL: Group Category mismatch with column 7b and column 7C" +NOT_IN_APPENDIX_J_FORMAT);
				do2Repo.save(do2);
				return false;
			}
		}
		if (employee.getInitialMusteringDate()!=null && !employee.getInitialMusteringDate().equals(do2.getFromDate())) {
			musterValidity = false;
			buffer.append(ALREADY_MUSTERED);
		}
		
		if (employee.getEnrolmentDate()!=null && employee.getNbsubDate()!=null && 
				 employee.getEnrolmentDate().equals(employee.getNbsubDate())) {
				 buffer.append(ENROLL_JCO);
				 musterValidity = false;
		}
		
		if (employee.getEnrolmentDate()!=null && do2.getFromDate().before(employee.getEnrolmentDate())) {
			buffer.append(INVALID_FROM_DATE);
			musterValidity = false;
		} 
		
		if (!isValidGroup(do2)) {
			musterValidity = false;
			buffer.append(INVALID_GROUP);
		}
		
		if (!isValidClassCategory(do2)) {
			musterValidity = false;
			buffer.append(INVALID_CLASS_CATEGORY);
		}

		if ((isHonoraryRank(employee))) {
			musterValidity = false;
			buffer.append(IS_HONY_RANK);
		}
 		
		/*if (employee.getAttestationDate()!=null) {
			if (!isAttestMusterSameGroupCategory(do2, employee)) {
				musterValidity = false;				
			}
		}*/
		
		do2.setReason(buffer.toString());
		do2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			
		if (musterValidity) {
			do2.setStatus(Do2Manager.VALID);
		//changes for TA
			if (employee.getRegularTaFlag()!=null && employee.getRegularTaFlag()=="T"){				
				if(AuditCageHelper.isBaseAfterEnrollDate(do2)){
					do2.setReason("A/o P&A paid from 01032012.Feed arrears from enroll date to 29022012 through adjustment voucher");	
				}
			}
			if (!isEnrMstDo2Exist(do2)) {
				createSysDo2(do2);
			}
			
			if (do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
				if (!AuditCageHelper.isTdmergDo2Exist(do2)){
					String presentTrade = null;
					String sldrClass = null;
					String group ="";
					String previousTrade=do2.getColumnC().trim();
					if (do2.getColumnB()!=null){
						sldrClass = do2.getColumnB().trim();
					}
					if (do2.getColumnA()!=null && do2.getColumnA().length()>3){
						group = do2.getColumnA().charAt(3)+"";
					}
					List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(group, previousTrade);
					/*	Utils.getEntityManager().createQuery(
						"SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat ")
						.setParameter("sgrp", group ).setParameter("scat", previousTrade).getResultList();*/
					if ((grpCatList.size() > 0)	&& (previousTrade.equals(grpCatList.get(0).getSoldierCategory()))) {
						 if (grpCatList.get(0).getMergedAbbr() != null){				
							presentTrade = grpCatList.get(0).getMergedAbbr();
							AuditCageHelper.createMergedTradeDo2(do2, sldrClass, previousTrade, presentTrade);
						 }
					}				
				}			
			}			
		} else {
			do2.setStatus(Do2Manager.INVALID);
		}
		
		do2Repo.save(do2);		 

		return musterValidity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"CUR");
  		Rank rank = Utils.getPborRank(employee.getArmyNo(), newAuditCage.getFromDate(),"PAYFIX");
		updateBdtInfo(newAuditCage.getDo2());
		newAuditCage.setLra((int)AuditCageHelper.getLraRate(newAuditCage));
		newAuditCage.setPmha((int)AuditCageHelper.getPmhaRate(newAuditCage.getDo2(), newAuditCage));
		
		if (!newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate())) {
			newAuditCage.setMsPay(5200);
		}
		
		if (groupCategory!=null) {
			clsPayList = classPayRepo.findByGpCatClassFromDateBetweenOrFromDateGreater(groupCategory.getId(), groupCategory.getEntryClass(),
																						employee.getEnrolmentDate());
					
				/*	Utils.getEntityManager().createQuery("SELECT cp FROM ClassPayRate AS cp WHERE "
					+ " cp.groupCategory =:groupCategory AND cp.classPayCode =:soldierClass AND "
					+ " ((:fromDate BETWEEN cp.fromDate AND cp.toDate) OR (:fromDate > cp.fromDate AND cp.toDate IS null))")
					.setParameter("groupCategory", groupCategory)
					.setParameter("soldierClass", groupCategory.getEntryClass())
					.setParameter("fromDate", employee.getEnrolmentDate())
					.getResultList();*/
			
		}
		
		System.out.println ( " RANK "  +rank.getRankCode());
		if (rank!=null && clsPayList!=null) {
			if (Integer.parseInt(rank.getRankCode()) < 50) {
				if (clsPayList.size() > 0 && clsPayList.get(0).getRate() > 0) {
					newAuditCage.setClassPay((int) Math.round(clsPayList.get(0).getRate()));
				}
			}
		}
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());		 

		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_MUSTER+"");
		
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		 
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_PROMOT);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.ESD_MUSTER);
		reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}
 	
	public boolean isValidGroup(Do2 do2) {
		if ((do2.getFromDate().equals(DateManager.getSixthCpcDate()) || do2.getFromDate().after(DateManager.getSixthCpcDate())
				&& (do2.getColumnA().charAt(3) == 'X' || do2.getColumnA().charAt(3) == 'Y')))
			return true;
		else {
			if (do2.getColumnA().charAt(3) == 'X' || do2.getColumnA().charAt(3) == 'Y' || do2.getColumnA().charAt(3) == 'Z')
				return true;
		}
		return false;
	}

	/*public boolean isValidClassCategory(Do2 do2) {
		List<GroupCategory> grpCatList = em.createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
				.setParameter("sgrp", do2.getColumnA().charAt(3))
				.setParameter("scat", do2.getColumnC().trim())
				.getResultList();
		
		
		if (grpCatList.size() > 0 ) {
			if (do2.getColumnB().charAt(3)==grpCatList.get(0).getEntryClass()) {
				return true;
			}
		}
			return false;
					
	}
//	Updated on 21042021
	public boolean isValidClassCategory(Do2 do2) {
		List<GroupCategory> grpCatList = em.createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat "
				+ " AND gc.fromDate >= :fromDate")
				.setParameter("sgrp", do2.getColumnA().charAt(3))
				.setParameter("scat", do2.getColumnC().trim())
				.setParameter("fromDate" ,do2.getFromDate())
				.getResultList();
		
		
		if (grpCatList.size() > 0 ) {
			
			if (do2.getColumnB().charAt(3)==grpCatList.get(0).getEntryClass()) {
				return true;
			}
		}
			return false;
					
	}
*/
	//Updated on 30.04.2021
	public boolean isValidClassCategory(Do2 do2) {
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"",
																		do2.getColumnC().trim());
				
		/*		em.createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
				.setParameter("sgrp", do2.getColumnA().charAt(3))
				.setParameter("scat", do2.getColumnC().trim())
				.getResultList();*/
		
	//	System.out.println (  " from date " +do2.getFromDate()   + " colA " +do2.getColumnA()  + " colc " +do2.getColumnC());
		
		System.out.println ( " grpCatListsize " +grpCatList.size());
		if (grpCatList.size() > 0 ) {
			for (int i=0; i<grpCatList.size(); i++) {
				if ((do2.getColumnB().charAt(3)+"").equals(grpCatList.get(i).getEntryClass())){
					return true;
				}
			}
		}
		return false;				
	}
	
	public boolean isHonoraryRank(Employee employee) {
		return (employee.getSubstantiveRank()!=null && 
				(employee.getSubstantiveRank().getRankCode().equals("80") || (employee.getSubstantiveRank().getRankCode()).equals("90")));
	}
    
	private boolean isEnrMstDo2Exist(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());		
		boolean enrmstdo2exists=false;
		List<Do2> do2List = do2Repo.findByValidAndPendingAndDescription(employee.getId(),"ENRMST");
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT d FROM Do2 d WHERE d.employee=:employee AND d.description=:description AND d.status in ('V', 'P')")
				.setParameter("employee", employee)
				.setParameter("description", "ENRMST")				
				.getResultList();*/
		if(do2List.size() > 0) {
			enrmstdo2exists=true;
		}
		/*if (do2List.size() > 0){
			for (Do2 enrmstdo2 : do2List){
				if (enrmstdo2.getColumnC()!= null && enrmstdo2.getColumnC().equals(do2.getColumnC())){
					enrmstdo2exists=true;
				}
			}
		}*/
		if (enrmstdo2exists == true)
			return true;
		else
			return false;
	}
	
	public void updateEmployee(Do2 do2) {
	//	String trade = do2.getColumnC().trim();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setInitialMusteringDate(do2.getFromDate());
		employee.setCurrentClassPayDate(do2.getFromDate());
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			employee.setAttestationDate(do2.getFromDate());
		}
		List<GroupCategory> grpCatList = null;
		
		grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", do2.getColumnC().trim());
		
		if (grpCatList==null || (grpCatList!=null && grpCatList.size()==0)) {
 			grpCatList = gcRepo.findBySoldierGroupAndMergedAbbr(do2.getColumnA().charAt(3)+"", do2.getColumnC().trim());
			if (grpCatList==null || (grpCatList!=null && grpCatList.size()==0)) {
				grpCatList = gcRepo.findBySoldierGroupAndRedesignatedAbbr(do2.getColumnA().charAt(3)+"", do2.getColumnC().trim());							
			}			 	
		}		
				
		/*		Utils.getEntityManager().createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
		.setParameter("sgrp", do2.getColumnA().charAt(3))
		.setParameter("scat", do2.getColumnC().trim())
		.getResultList();*/
		
/*		if (grpCatList.size() > 0 ) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null){
			//	trade = grpCatList.get(0).getRedesignatedAbbr();
				
			} else if (grpCatList.get(0).getMergedAbbr() != null) {
				trade = grpCatList.get(0).getMergedAbbr();
			} else {
				trade = grpCatList.get(0).getSoldierCategory();
			}
		}	
				
		GroupCategory grpcat = new GroupCategory();
		grpcat=gcRepo.findBySoldierCategory(trade).get(0);*/
	//	employee.setGroupCategory(grpcat);
		employee.setGroupCategory(grpCatList.get(0));
	//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(grpcat,"SoldierCategory", trade));
		employee.setSoldierClass(do2.getColumnB().charAt(3)+"");
		Rank rank = new Rank();

		if (employee.getSportHavildarFlag() != null	&& employee.getSportHavildarFlag() == true) {
			rank=rankRepo.findByRankCode("40");
			employee.setSubstantiveRank(rank);
		//	employee.setSubstantiveRank((Rank) Utils.getForeignKey(rank, "rankCode", "40"));
		} if (employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true) {
			rank=rankRepo.findByRankCode("40");
			employee.setSubstantiveRank(rank);
		//	employee.setSubstantiveRank((Rank) Utils.getForeignKey(rank, "rankCode", "40"));
		} else {
			rank=rankRepo.findByRankCode("20");
			employee.setSubstantiveRank(rank);
	//		employee.setSubstantiveRank((Rank) Utils.getForeignKey(rank,"rankCode", "20"));
		}
		
		if (employee.getSubstantiveRankDate()==null || employee.getEnrolmentDate().equals(employee.getSubstantiveRankDate())) {
			employee.setSubstantiveRankDate(do2.getFromDate());
		} else if  (employee.getSubstantiveRankDate()!=null && do2.getFromDate().before(employee.getSubstantiveRankDate())){
			employee.setSubstantiveRankDate(do2.getFromDate());
		}
		
		//List <Rank> rankList = null;
		//Check whether below commented code req or not , since rank is obtained above----24052022
		/*if (employee.getGraduateHavildarFlag()!=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
	    } else {
	    	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='20'").getResultList();
	    }	*/
		employee.setTermsColourService(rank.getColorService());
		employee.setTermsMaxAgeRetention(rank.getMaximumAge());
		 
		StatusCode sc=scRepo.findByStatusCode("E");
		employee.setStatusCode(sc);
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", "E"));
		Date chk6cpcDate = DateManager.getToday();
		Calendar sixCpc = new GregorianCalendar(2005, Calendar.DECEMBER, 31);
		chk6cpcDate.setTime(sixCpc.getTimeInMillis());

		if (newDo2.getId()==do2.getId()) {
			if (do2.getEmployee().getEnrolmentDate()!=null && do2.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
				if ((employee.getAttestationDate() == null)
						|| employee.getAttestationDate().equals(employee.getInitialMusteringDate())
						|| employee.getAttestationDate().after(employee.getInitialMusteringDate())) {
					employee.setIncrementDate(musterIncrCal(do2));
				} else {
					employee.setIncrementDate(musterAttestIncrCal(employee));
				}
			} else {
				employee.setIncrementDate(Utils.getNextIncrementDate(employee.getEnrolmentDate()));
			}
			empRepo.save(employee);
			 
			if (!(Utils.isDateAfterPreviousQuarterEnding(employee.getIncrementDate()))) {
				boolean done = false;
				for (; !done ;) {
					createSysIncrDo2(do2, employee);
					if (Utils.isDateAfterPreviousQuarterEnding(employee.getIncrementDate())) {
						done = true;
					}
				}
			}	
		}
	}

	public void reverseUpdateEmployee(Do2 refDo2) {
		 
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		
		employee.setInitialMusteringDate(null);
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			employee.setAttestationDate(null);
		}
		employee.setCurrentClassPayDate(null);
		employee.setSoldierClass(null);
		employee.setIncrementDate(null);
		
		if (employee.getAttestationDate()==null) {
			employee.setGroupCategory(null);
			
			Rank rank=rankRepo.findByRankCode("10");
			employee.setSubstantiveRank(rank);
			//employee.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", "10"));
			employee.setSubstantiveRankDate(employee.getEnrolmentDate());
			
			StatusCode sc=scRepo.findByStatusCode("R");
			employee.setStatusCode(sc);
			//employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", "R"));
			employee.setStatusEffectDate(employee.getEnrolmentDate());
		}
		empRepo.save(employee);
	}
	
	public boolean isAttestMusterSameGroupCategory(Do2 do2, Employee employee) {
	 	String empcatg = null;
		
		if (employee.getRemusteringDate() != null) {
			if (!do2.getFromDate().before(employee.getRemusteringDate())){
				if (employee.getGroupCategory()!=null) {
					if (employee.getGroupCategory().getMergedAbbr()!=null )  {
						empcatg = employee.getGroupCategory().getMergedAbbr();
					} else if (employee.getGroupCategory().getRedesignatedAbbr()!=null )  {
						empcatg = employee.getGroupCategory().getRedesignatedAbbr();
					} else {
						empcatg = employee.getGroupCategory().getSoldierCategory();
					}
				}
			} else {
				if (employee.getPreviousGroupCategory()!=null) {
					if (employee.getPreviousGroupCategory().getMergedAbbr()!=null )  {
						empcatg = employee.getPreviousGroupCategory().getMergedAbbr();
					} else if (employee.getPreviousGroupCategory().getRedesignatedAbbr()!=null )  {
						empcatg = employee.getPreviousGroupCategory().getRedesignatedAbbr();
					} else {
						empcatg = employee.getPreviousGroupCategory().getSoldierCategory();
					}
				}
			}
		} else {
			if (employee.getGroupCategory()!=null) {
				if (employee.getGroupCategory().getMergedAbbr()!=null )  {
					empcatg = employee.getGroupCategory().getMergedAbbr();
				} else if (employee.getGroupCategory().getRedesignatedAbbr()!=null )  {
					empcatg = employee.getGroupCategory().getRedesignatedAbbr();
				} else {
					empcatg = employee.getGroupCategory().getSoldierCategory();
				}
			}
		}


		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", 
																		do2.getColumnC().trim());
				
	/*			em.createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
		.setParameter("sgrp", do2.getColumnA().charAt(3))
		.setParameter("scat", do2.getColumnC().trim())
		.getResultList();*/

		if (grpCatList.size() > 0 ) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null ) {
				gpcatg = grpCatList.get(0).getRedesignatedAbbr(); 
			} else if (grpCatList.get(0).getMergedAbbr() != null ) {
				gpcatg = grpCatList.get(0).getMergedAbbr();
			} else {
				gpcatg = grpCatList.get(0).getSoldierCategory();
			}
		}
		
		if (empcatg == null ) {
			buffer.append("FK_GROUP_CATEGORY in employee table not available");
			return false;
		} else {
			if (empcatg.equals(gpcatg)) {
				return true;
			} else {
	//			buffer.append(ATTEST_MUSTER_GROUP_CATEGORY_DIFFERS);
				return false;
			}
		}		
	}

	private void createSysDo2(Do2 do2) {
		// Generation Of ENRMST DoII To Start Sepoy Pay From Enrollment Date
		Do2 enrMstDo2 = new Do2();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		enrMstDo2.setDescription("ENRMST");
		//CODE AMENDED FOR ta WHERE THE ENRMST FROM DATE WILL BE 01032012
		/*if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag()=="T"){
			
			if(AuditCageHelper.isBaseAfterEnrollDate(do2)){
			
				enrMstDo2.setFromDate(DateManager.getNextDate(DateManager.getTaCutOffDate()));
				
			}else{
				enrMstDo2.setFromDate(employee.getEnrolmentDate());
			}
		}else{
			enrMstDo2.setFromDate(employee.getEnrolmentDate());	
		}*/
		if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("T")){
			
			if(AuditCageHelper.isBaseAfterEnrollDate(do2)){
			
				enrMstDo2.setFromDate(DateManager.getNextDate(DateManager.getTaCutOffDate()));
				
			}else{
				enrMstDo2.setFromDate(employee.getEnrolmentDate());
			}
		}else{
			List<Do2> do2List =do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(employee.getId(), employee.getEnrolmentDate(),"V","ENROLL");
			if (do2List.size()==1) {
				if(AuditCageHelper.isBaseAfterEnrollDate(do2)){
					return;
				}
				enrMstDo2.setFromDate(employee.getEnrolmentDate());	
			} else return;
				
		}
		
		enrMstDo2.setBatchNo(do2.getBatchNo());
		enrMstDo2.setColumnA(do2.getColumnA());
		enrMstDo2.setColumnB(do2.getColumnB());
		enrMstDo2.setColumnC(do2.getColumnC());
		enrMstDo2.setTjra(do2.getTjra());
		enrMstDo2.setLra(do2.getLra());
		enrMstDo2.setEmployee(employee);
		enrMstDo2.setArmyNo(do2.getArmyNo());
		enrMstDo2.setCheckDigit(do2.getCheckDigit());
		enrMstDo2.setUnitCode(do2.getUnitCode());
		Unit uc=unitRepo.findByUnitCode(do2.getUnitCode());
		enrMstDo2.setUnit(uc);
	//	enrMstDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode",
	//			do2.getUnitCode()));
		enrMstDo2.setDo2Group("IA");
		enrMstDo2.setDo2Type(do2.getDo2Type());
		enrMstDo2.setDo2No(do2.getDo2No());
		enrMstDo2.setDo2ItemNo(do2.getDo2ItemNo());
		enrMstDo2.setDo2Date(do2.getDo2Date());
		enrMstDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		enrMstDo2.setStatus(Do2Manager.PENDING);
		enrMstDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		enrMstDo2.setAuditCage(true);
		enrMstDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		enrMstDo2.setReason(null);
		enrMstDo2.setQe(Utils.getCurrentQuarter());
		enrMstDo2.setInputQe(do2.getInputQe());
		do2Repo.save(enrMstDo2);
		 
		AuditCageHelper.insertDaDo2s(employee, do2);
		AuditCageHelper.insertLraDo2s(employee, do2);

		System.out.println(" MUSTER ...................... .. " );
		if (!employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) { System.out.println("7th cpc increment generation ....................... ");
			AuditCageHelper.insertIncrementDos2ForAttestMusterIn7cpc(do2, employee.getEnrolmentDate(), do2.getDescription());
			System.out.println("7th cpc increment generation ....................... ");
		}
		// Below Statement Is For Generating ALWREV For MSPay (01.09.2008)
		if (do2.getFromDate().before(
				DateManager.getSixthCpcAllowanceDate())) {
			List<Do2> duplicateDo2List=null;
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","ALWREV"
						,DateManager.getDate("2008-09-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='ALWREV' AND d.status in ('V', 'P') AND d.fromDate = '2008-09-01'")
			.setParameter("emp", employee).getResultList();*/

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager
					.getSixthCpcAllowanceDate(), do2);
			}
		}

		// Below Statement Is For Generating ALWREV For Dearness Pay
		// (01.04.2004)
		if (employee.getEnrolmentDate().before(DateManager.getDPDate())) {
			List<Do2> duplicateDo2List=null;
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","ALWREV"
						,DateManager.getDate("2004-04-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='ALWREV' AND d.status in ('V', 'P') AND d.fromDate = '2004-04-01'")
			.setParameter("emp", employee).getResultList();*/

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager
					.getDPDate(), do2);
			}
		}

		// Below Statement Is For Generating PAYFIX (01.01.2006)
		if (!(employee.getEnrolmentDate().after(DateManager.getSixthCpcDate()))) {
			List<Do2> duplicateDo2List=null;
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","PAYFIX"
						,DateManager.getDate("2006-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='PAYFIX' AND d.status in ('V', 'P') AND d.fromDate = '2006-01-01'")
			.setParameter("emp", employee).getResultList();*/

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertPayfixDo2(employee, DateManager
						.getSixthCpcDate());
			}
		}
		
		// Below Statement Is For Generating FIX7PC (01.01.2016)
		if (!(employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate()))) {
			List<Do2> duplicateDo2List=null;
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","FIX7PC"
						,DateManager.getDate("2016-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
	/*				Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='FIX7PC' AND d.status in ('V', 'P') AND d.fromDate = '2016-01-01'")
			.setParameter("emp", employee).getResultList();*/

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertFix7pcDo2(employee, DateManager.getSeventhCpcDate());
			}
		}
				
		// Below Statement Is For Generating ALWREV For Allowances (01.07.2017)
		if (!(employee.getEnrolmentDate().before(DateManager.getSeventhCpcAllowanceDate()))) {
			List<Do2> duplicateDo2List=null;
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","ALWREV"
						,DateManager.getDate("2017-07-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
	/*				Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
			" d.employee=:emp AND d.description='ALWREV' AND d.status in ('V', 'P') AND d.fromDate = '2017-07-01'")
			.setParameter("emp", employee).getResultList();*/

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager.getSeventhCpcAllowanceDate(),do2);
			}
		}		
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
                         incrementDate = DateManager.getDate(cal.get(Calendar.YEAR)	+ 1 + "07-01");
	                 } catch (Exception e) {
	                	 Utils.handleException(e);
	                 }
				}			
			}
		}
		return incrementDate;
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
				&& year >= 2006 && year < 2016) {
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
		List<Do2> duplicateDo2List = do2Repo.findByDescFromDateEqualValidPending(employee.getId(),"V","P","INCR",dni);
				
	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
		" d.employee=:emp AND d.description='INCR' AND d.status in ('V', 'P') AND d.fromDate = :fromDate")
		.setParameter("emp", employee)
		.setParameter("fromDate", dni).getResultList();*/
    //    System.out.println("from date " + dni + " employee " + employee.getArmyNo() + " size " + duplicateDo2List.size());
		if (duplicateDo2List == null || duplicateDo2List.size() == 0){
			Do2 incrDo2 = new Do2();
			incrDo2.setEmployee(employee);
			incrDo2.setArmyNo(do2.getArmyNo());
			incrDo2.setBatchNo(do2.getBatchNo());
			incrDo2.setCheckDigit(do2.getCheckDigit());
			incrDo2.setUnitCode(do2.getUnitCode());
			Unit uc=unitRepo.findByUnitCode(do2.getUnitCode());
			incrDo2.setUnit(uc);
		//	incrDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", do2.getUnitCode()));
			incrDo2.setDo2Group("IA");
			incrDo2.setDo2Type("0");
			incrDo2.setDo2No("6666");
			incrDo2.setDo2ItemNo("666");
			incrDo2.setDo2Date(dni);
			incrDo2.setDescription("INCR");
			incrDo2.setFromDate(dni);
			incrDo2.setColumnA(null);
			incrDo2.setColumnB(null);
			incrDo2.setColumnC("MUSTER");
			incrDo2.setTjra(do2.getTjra());
			incrDo2.setLra(do2.getLra());
			incrDo2.setStatus(Do2Manager.PENDING);
			incrDo2.setReason(null);
			incrDo2.setQe(Utils.getCurrentQuarter());
			incrDo2.setInputQe(Utils.getCurrentQuarter());
			incrDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			incrDo2.setAuditCage(true);
			do2Repo.save(incrDo2);		
		}
		if (year == 2006 && month == 1) {
			try {
				employee.setIncrementDate(DateManager.getDate(year, 7, 1));
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
			employee.setIncrementDate(Utils.getNextIncrementDate(employee.getIncrementDate()));
		}

		empRepo.save(employee);
	}

	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}
