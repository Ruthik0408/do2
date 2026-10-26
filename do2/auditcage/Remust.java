package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.SoldierGroup;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageBase;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/* 
 * author : K. RAMADEVI   &&    N. RAMACHANDRAN   Dt. 26.10.2009.
 */

@RestController
public class Remust extends AuditCageBase implements
		 Do2AuditCageProcessor, java.io.Serializable {
	
	 
	public boolean validity = true;
	StringBuffer buffer = new StringBuffer();
	public String trade = null;
	
	static public final String [] SPECIAL_CATEGORY = {"AVFAF", "AVFARM", "AVFINP", "AVFELE", 
			 "AVFAEN", "AVFAVN", "AVNTECAE", "AVNTECAF", "AVNTECAVS","AVNTECELE","AVNTECIP", "HMT"};
	/*static public final List<String> NON_REMUST_CATEGORY = Arrays.asList("HMT","PA","JECIV","JEEM", "MTECELE" ,"MTECAVEH",
		"MTECBVEH","MTECCVEH","MTECAVNAE","MTECAVNAF","MTECAVNAR","MTECAVNCS","MTECAVNEL",	"MTECAVNIP"
,		"MTECNW","MTECCOMN","MTECGCE","MTECO","MTECRDR","MTECSA");*/
	// As per orders of ACDA, the PA category is exempted on  21.07.2016.
	//As per amendment no. 24 of MSTHAV -- MSTHAV, REMUST, APPT codes amended and above list renamed as HAV_REMUST_CATEGORY as 
	//stated below.
	static public final List<String> HAV_REMUST_CATEGORY = Arrays.asList("PA","HMT","JECIV","JEEM", "MTECELE" ,"MTECAVEH",
			"MTECBVEH","MTECCVEH","MTECAVNAE","MTECAVNAF","MTECAVNAR","MTECAVNCS","MTECAVNEL",	"MTECAVNIP"
	,		"MTECNW","MTECCOMN","MTECGCE","MTECO","MTECRDR","MTECSA");
	
	public static boolean VALID_SPECIAL_CATEGORY = false;
	
	public static final String NOT_YET_MUSTERED = "AU:PBOR has not been initially mustered. \n";
	public static final String INVALID_GROUP = "AU: Incorrect group on from date. \n ";
	public static final String INVALID_CLASS_CATEGORY = "AU:Incorrect class/category since pbor is still in lower class. \n ";
	public static final String IS_HONY_RANK = "AU:PBOR is  holding honorary Rank. \n ";
	public static final String INVALID_PAY_STAGE = "AU:Pay mismatch. \n ";
	public static final String PBOR_ON_LEAVE = "AU:PBOR is on leave on the date of Remust. \n";  // eme observation 11/2018
	
	public int personalPay = 0;
	public Employee employee;
    public Rank rank;
    public SoldierGroup soldierGroup;
    public String currentSolGroup;
    List<PayScale> previousPayList, newPayList;
    List<AuditCage> atcList;
	
    @Transactional
    @Override
	public boolean isValid(Do2 do2) {
		
		boolean payValidity = true;
		 
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"PAYFIX");
		
		if (Utils.getGroupCategory(do2,"PREV")!=null) {
			currentSolGroup = Utils.getGroupCategory(do2,"PREV").getSoldierGroup();	
		}		
	 
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && !isAlreadyMustered(employee, do2)) {
			validity = false;
			buffer.append(NOT_YET_MUSTERED);
		}
		 
		if (!isValidGroup(do2)) {
			validity = false;
			buffer.append(INVALID_GROUP);
		}
		if (!isValidClassCategory(do2)) {
			validity = false;
			buffer.append(INVALID_CLASS_CATEGORY);
		}
		
		// Exclusion of two trades for this validation as per Hqrs. Office letter No.Army/Pay/3510/7thCPC/Vol-II'A' dated 08.10.2020
		if (do2.getColumnC()!=null && !(do2.getColumnC().trim().equals("OSS") || do2.getColumnC().trim().equals("INSTRSPL"))) {
			if (isPborOnRegularLeave(do2, employee)) {
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(PBOR_ON_LEAVE);				
			}	
		}		
		
		if ((isHonoraryRank(do2))) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(IS_HONY_RANK);
		}
	 
		if (newDo2.getId()==do2.getId())
		if (do2.getFromDate().before(DateManager.getSixthCpcDate()))
			if (!isValidPayStage(do2)) {
				validity = false;
				payValidity = false;
				buffer.append(INVALID_PAY_STAGE);
		}
	 
		 if (validity) {
			do2.setReason(null);
			do2.setStatus(Do2Manager.VALID);
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
							
					/*		Utils.getEntityManager().createQuery(
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
			if (!payValidity) 
				do2.setStatus(Do2Manager.PENDING);
			else 
				do2.setStatus(Do2Manager.INVALID);
			
			buffer.append("\n");
			do2.setReason(buffer.toString());
		}		
	 	do2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(do2);
	
		return validity;
	}
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		
		doPayFix(newAuditCage);
		
		if ((!newAuditCage.getDo2().getFromDate().before(DateManager.getDPDate())) &&
				(newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate()))) {
			newAuditCage.setDp((int)Math.round(newAuditCage.getBasicPay() * 0.5));
		}
		
		if (!VALID_SPECIAL_CATEGORY)
			newAuditCage.setClassPay(0);
			
	 	return newAuditCage;
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);

		updateBdtInfo(do2);
		
		if (personalPay!=0)
		AuditCageHelper.updateAuditCageAllowance(newAuditCage,"PERSRM",personalPay);
					
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_MUSTER+"");
		
		return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_MUSTER);
				
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
	 
		employee.setGroupCategory(employee.getPreviousGroupCategory());
		employee.setPreviousGroupCategory(null);
		employee.setRemusteringDate(null);
		
		List<Do2> classPayDo2List = do2Repo.findByFromDateLessThanAndMultipleDescriptionAndStatus(employee.getId(), currentDo2.getFromDate(),
																							"UPCLAS","MUSTER", Do2Manager.VALID);
				
			/*	Utils.getEntityManager().createQuery(
										"SELECT do2 FROM Do2 do2 WHERE do2.employee=:employee AND do2.fromDate < :fromDate" +
										" AND (do2.description = :desc1 OR do2.description = :desc2)" +
										" AND do2.status = :status ORDER BY do2.fromDate DESC")
										.setParameter("employee", employee)
										.setParameter("fromDate", currentDo2.getFromDate())
										.setParameter("desc1", "UPCLASS")
										.setParameter("desc2", "MUSTER")
										.setParameter("status", Do2Manager.VALID)
										.getResultList();*/
			 	
		if(classPayDo2List.size() > 0) {
			if (classPayDo2List.get(0).getColumnB()!=null) {
				employee.setSoldierClass(classPayDo2List.get(0).getColumnB().charAt(3)+"");
			}
		}
	// 	employee.setSoldierClass(classPayDo2List.get(0).getColumnB().charAt(3));
		empRepo.save(employee);
	 //	Utils.getEntityManager().persist(employee);
 	
	 	return Do2Manager.SUCCESS;
 	}
		
	private boolean isValidGroup(Do2 do2) {
		
		/*if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (do2.getColumnA().charAt(3)=='Y' && do2.getColumnC().equals("CLKGD")) {
				return true;
			} else {
				return false;
			}
		}*/
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (!do2.getFromDate().after(DateManager.getSixthCpcDate())) {
				if (do2.getColumnA().charAt(3)=='Y' && do2.getColumnC().equals("CLKGD")) {
					return true;
				} else 
					return false;
			}
			
			if (!do2.getFromDate().before(DateManager.getSixthCpcDate())) {
				if (do2.getColumnA().charAt(3)=='Y' && do2.getColumnC().equals("CLKSD")) {
					return true;
				} else 
					return false;
			}
		}

		
		if (!(do2.getFromDate().before(DateManager.getSixthCpcDate()))) {
			if (do2.getColumnA().charAt(3)=='Z' && do2.getId()!=0) {
				return true;
			}  
 			if ((do2.getColumnA().charAt(3) == 'X')	|| (do2.getColumnA().charAt(3) == 'Y'))
				return true;
		}
		else {
			if ((do2.getColumnA().charAt(3) == 'X') || (do2.getColumnA().charAt(3) == 'Y') 	|| (do2.getColumnA().charAt(3) == 'Z'))
				return true;
		}
		
		return false;
	}

	private boolean isHonoraryRank(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
			if (((employee.getSubstantiveRank()!=null) && 
					   ((employee.getSubstantiveRank().getRankCode().equals("80")) || 
							(employee.getSubstantiveRank().getRankCode().equals("90")))) ||
					   ((employee.getActingRank()!=null) &&	 
							((employee.getActingRank().getRankCode().equals("80")) ||
							(employee.getActingRank().getRankCode().equals("90"))))) {
				if (employee.getSubstantiveRankDate().before(do2.getFromDate())
						   || ( employee.getActingRankDate().before(do2.getFromDate()))) {
					return true;
				}
			}
			return false;
	}

	private void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		String trade = null;
		if (newDo2!=null && newDo2.getId()==do2.getId()) {
			employee.setPreviousGroupCategory(employee.getGroupCategory());
			employee.setPreMusterDate(employee.getInitialMusteringDate());
		}else {//lines added if the do2 is not processed as new do2
			trade = getTrade(do2);
			GroupCategory grpcat = new GroupCategory();
			grpcat=gcRepo.findBySoldierCategory(trade).get(0);
	//		grpcat =((GroupCategory) Utils.getForeignKey(grpcat, "soldierCategory", trade));
			if (employee.getGroupCategory() != null ) {
				if(grpcat.getId() !=employee.getGroupCategory().getId()){
					employee.setPreviousGroupCategory(employee.getGroupCategory());
					employee.setPreMusterDate(employee.getInitialMusteringDate());
					}
				}
		}
		trade = getTrade(do2);
		GroupCategory newGrpcat = new GroupCategory();
		newGrpcat=gcRepo.findBySoldierCategory(trade).get(0);
	//	newGrpcat =((GroupCategory) Utils.getForeignKey(newGrpcat, "soldierCategory", trade));
		employee.setGroupCategory(newGrpcat);
		employee.setSoldierClass(do2.getColumnB().charAt(3)+"");
		employee.setRemusteringDate(do2.getFromDate());
		empRepo.save(employee);
		//Utils.getEntityManager().persist(employee);
		//Utils.getEntityManager().flush();
 	 }
		

	private boolean isAlreadyMustered(Employee employee, Do2 do2) {
		try {
			if (employee.getInitialMusteringDate()!=null && employee.getInitialMusteringDate().before(DateManager.getDate("1998-09-01")))
					return true;
		} catch (Exception e) {
			Utils.handleException(e);
		}	
		if ((employee.getInitialMusteringDate()!=null && employee.getInitialMusteringDate().before(do2.getFromDate())) 
				|| (employee.getAttestationDate()!=null && employee.getAttestationDate().before(do2.getFromDate()))) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isValidClassCategory(Do2 do2) {
		trade = do2.getColumnC().trim();
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", do2.getColumnC().trim());
				
		/*		em.createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
				.setParameter("sgrp", do2.getColumnA().charAt(3))
				.setParameter("scat", do2.getColumnC().trim())
				.getResultList();*/
		if (grpCatList.size() == 0) {
		 	return false;
		}
		
		System.out.println ( " grpCatList.size " +grpCatList.size());
		System.out.println ( " grpCatList.size " +grpCatList.get(0).getId());
		
		// above code amended as stated below in view of amendment NO.24  to appendix_j for description MSTHAV
		
		if (!do2.getFromDate().before(DateManager.getRevisedMsthavImplDate())) {
			if (HAV_REMUST_CATEGORY.contains(do2.getColumnC())){
				rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"PROMOT");
			//	System.out.println ( " RANK  "+rank.getRankCode());
				if (Utils.getPao().getPaoCode().equals("56")) {  // BEG Roorkee - Hqrs. Office letters No.Army/Pay/ORs/Appendix-J/Vol-1A/E-1804 dated 16.09.2021 and 29.12.2021
					if (rank.getRankCode().equals("40") || rank.getRankCode().equals("30")) {
						return true;
						
					} 
				} else if (rank.getRankCode().equals("40")) {
					return true;
				} else {
					buffer.append(" TO BE PUBLISHED WITH DESCRIPTION MSTHAV ");
					return false;
				}
			}
		}
			
		if (grpCatList.get(0).getRedesignatedAbbr() != null){
			trade = grpCatList.get(0).getRedesignatedAbbr();
		} 
		for (int i=0; i < SPECIAL_CATEGORY.length; i++) {
			if (do2.getColumnC().equals(SPECIAL_CATEGORY[i])) {
				if (employee.getGroupCategory().getSoldierGroup().equals("X")) {
					 if (do2.getColumnB().charAt(3)=='1') {
						 if (employee.getSoldierClass().equals("1")) {
								VALID_SPECIAL_CATEGORY = true;
								return true;
						 } else {  // case where pbor is in class 2 or above and do2 is for 1
							 	return false;
						 }
					 }
				}
			}
		}		
		String do2EntryClass = do2.getColumnB().charAt(3)+""; 
	//	if (do2.getColumnB().charAt(3)+""==grpCatList.get(0).getEntryClass()) {
		
		if (grpCatList.size()==1) {
			if (do2EntryClass.equals(grpCatList.get(0).getEntryClass())) {
				return true;
			} else {
			  	return false;
			}
		} else  {
			if (grpCatList.size() > 0 ) {
				for (int i=0; i < grpCatList.size(); i++) {
					if (do2EntryClass.equals(grpCatList.get(i).getEntryClass()) && !do2.getFromDate().before(grpCatList.get(0).getFromDate())) {
						
						return true;
					}
				}
			}
			
			
		}
		return false;
	}
	
	private void doPayFix(AuditCage newAuditCage) {
		int currentBasicPay = 0;
		int newScaleIncrStage = 0;
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
 		rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(),newAuditCage.getFromDate(),"PAYFIX");
 		String remusteredGroup = newAuditCage.getDo2().getColumnA().charAt(3)+"";
	    
		getOldPayDetails(newAuditCage.getDo2());
	
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV");
		
		if (groupCategory!=null) {
			currentSolGroup = groupCategory.getSoldierGroup();
		}
		
		if (groupCategory==null) {
			if (currentSolGroup==null || (currentSolGroup!=null && currentSolGroup.length()==0)) {
				if (employee.getPreviousGroupCategory()!=null && employee.getRemusteringDate()!=null
						&& employee.getRemusteringDate().before(newAuditCage.getFromDate())) {
					currentSolGroup = employee.getPreviousGroupCategory().getSoldierGroup();
				} else {
					currentSolGroup = employee.getGroupCategory().getSoldierGroup();
				}	
			}			
		}
		
		if (currentSolGroup.equals("Z") && !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			currentSolGroup = "Y";
		}
	
//		currentSolGroup = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV").getSoldierGroup();
	
		
			 
		
	 	newPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(), remusteredGroup, newAuditCage.getFromDate());
	 			
	 		/*	Utils.getEntityManager().createQuery(
				"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
				+ " AND ps.soldierGroup.soldierGroup=:grp "
				+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
				+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
				+ " AND ps.startScale is NOT NULL")
					.setParameter("rnk", rank)
					.setParameter("grp", remusteredGroup)
					.setParameter("fromDate", newAuditCage.getFromDate())
					.getResultList();*/
	 	
	 	
	 	if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) 
	 			&& newAuditCage.getGradePay()==0){
	 				System.out.println ( " AAAAAA ");
	 			try {
					newPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),remusteredGroup,DateManager.getDate("1997-10-10"));
					
							
						/*	Utils.getEntityManager().createQuery(
								"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale is NOT NULL")
									.setParameter("rnk", rank)
									.setParameter("grp", remusteredGroup)
									.setParameter("fromDate", DateManager.getDate("1997-10-10"))
									.getResultList();*/
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	 				
	 	} else {
	 		
	 		System.out.println ( " BBBBB ");
 		
	 		newPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),remusteredGroup,newAuditCage.getFromDate());
	 				
	 		/*		Utils.getEntityManager().createQuery(
					"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
					+ " AND ps.soldierGroup.soldierGroup=:grp "
					+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
					+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
					+ " AND ps.startScale is NOT NULL")
						.setParameter("rnk", rank)
						.setParameter("grp", remusteredGroup)
						.setParameter("fromDate", newAuditCage.getFromDate())
						.getResultList();*/
		 	
	 	}
	 /*	if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
	 		if (remusteredGroup=='X') {
	 			newAuditCage.setXGroupPay(newPayList.get(0).getXGroupPay());
	 		}
	 	}
	 	*/
	 	
		if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) 
				&& !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
				&& newAuditCage.getGradePay()!=0) {
	 		if (remusteredGroup.equals("X")) {
	 			newAuditCage.setXGroupPay(newPayList.get(0).getXGroupPay());
	 		}
	 	} else if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
	 		if (remusteredGroup.equals("X")) {
	 			newAuditCage.setXGroupPay(3600);
	 		}
	 	}
	 	
  	 	if (remusteredGroup!=currentSolGroup) {
 			if ((newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
 					|| (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && 
 							newAuditCage.getGradePay()==0))) {
 
 				currentBasicPay = newAuditCage.getBasicPay();
				
 				for (int i = newPayList.get(0).getStartScale(); i <= newPayList
						.get(0).getScaleEnd0(); i += newPayList.get(0)
						.getIncr0()) {
 					if (i >= currentBasicPay) {
						newScaleIncrStage = i;
						break;
					}
				}
			}
		}
  	 	
 	 	if (newScaleIncrStage != 0) {
	 		System.out.println ( " CURRENT BASIC PAY " +currentBasicPay  +" newpay " +newPayList.get(0).getId());
			if (currentBasicPay < newPayList.get(0).getStartScale()) {
 				newAuditCage.setBasicPay(newPayList.get(0).getStartScale());
			} else {
				if (currentBasicPay == newScaleIncrStage) {
 					newAuditCage.setBasicPay(newScaleIncrStage);
				} 
				else {
					if (newAuditCage.getDo2().getDescription().equals("OWNMST")) {
 						if (currentBasicPay < newScaleIncrStage) {
 							newAuditCage.setBasicPay(newScaleIncrStage-newPayList.get(0).getIncr0());
							personalPay = (currentBasicPay-(newScaleIncrStage-newPayList.get(0).getIncr0()));
						}
					} else { // stage where description is REMUST and pay fixation done (increase in pay - 5th CPC)
						newAuditCage.setBasicPay(newScaleIncrStage);
 						employee.setIncrementDate(DateManager.getNextIncrementDate(newAuditCage.getDo2().getFromDate()));
					}
				}
			}
		}
	}
		
	private boolean isValidPayStage(Do2 do2) {
					
		getOldPayDetails(do2);
	
		if (previousPayList.size() > 0)
		for (int i = previousPayList.get(0).getStartScale(); i <= previousPayList
				.get(0).getScaleEnd0(); i += previousPayList.get(0).getIncr0()) {
	 		if (atcList.get(0).getBasicPay() == i) {
				
				return true;
			}
		}
		return false;
	}
	
	private void getOldPayDetails(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"PAYFIX");
		rank = Utils.getPborRank(do2.getArmyNo(),DateManager.getPreviousDate(do2.getFromDate()),"PAYFIX");
//		currentSolGroup = Utils.getGroupCategory(do2,"PREV").getSoldierGroup();
		GroupCategory groupCategory = Utils.getGroupCategory(do2,"PREV");
		
		if (groupCategory!=null) {
			currentSolGroup = groupCategory.getSoldierGroup();
		} else {
			if (currentSolGroup==null || (currentSolGroup!=null && currentSolGroup.length()==0)) {
				if (employee.getPreviousGroupCategory()!=null && employee.getRemusteringDate()!=null
						&& employee.getRemusteringDate().before(newAuditCage.getFromDate())) {
					currentSolGroup = employee.getPreviousGroupCategory().getSoldierGroup();
				} else {
					currentSolGroup = employee.getGroupCategory().getSoldierGroup();
				}	
			}			
		}	
		 previousPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),currentSolGroup,do2.getFromDate());
				 
		/*		 Utils
				.getEntityManager()
				.createQuery(
						"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale is NOT null")
				.setParameter("rnk", rank)
				.setParameter("grp", currentSolGroup)
				.setParameter("fromDate", do2.getFromDate())
				.getResultList();*/
		
	 	 		 
 		 atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(), do2.getFromDate());
 				 
 			/*	 Utils.getEntityManager().createQuery(
				"SELECT atc FROM AuditCage atc WHERE atc.employee=:emp AND atc.cancelled=false AND atc.effective=true " +
				" AND atc.absentFlag=false AND atc.recordStatus!='P' AND atc.fromDate<:frdt "
						+ "ORDER BY atc.fromDate DESC, atc.toDate Desc").setParameter("emp",
				employee).setParameter("frdt", do2.getFromDate())
				.getResultList();*/
  	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
	
	public String getTrade(Do2 do2) {
		String trade = null;
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", do2.getColumnC().trim());
				
		/*		Utils.getEntityManager().createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
				.setParameter("sgrp", do2.getColumnA().charAt(3))
				.setParameter("scat", do2.getColumnC().trim())
				.getResultList();*/
				
				if (grpCatList.size() > 0 ) {
					if (grpCatList.get(0).getRedesignatedAbbr() != null){
						trade = grpCatList.get(0).getRedesignatedAbbr();
					} else if (grpCatList.get(0).getMergedAbbr() != null) {
						trade = grpCatList.get(0).getMergedAbbr();
					} else {
						trade = grpCatList.get(0).getSoldierCategory();
					}
				}
			return trade;
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
		List<Do2> do2List = do2Repo.findForMultipleDescriptionsWithFromDateBetween(employee.getId(), Do2Manager.VALID,
																			Do2Manager.PENDING, descList, do2.getFromDate());
				
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
/*
		for (Do2 leaveDo2 : do2List) {
			if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription()))
				return true;
		} */
		
		
		for (Do2 leaveDo2 : do2List) {
			if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription())) {
				Date leaveDo2FromDate = leaveDo2.getFromDate();
				List<Do2> recallDo2List = do2Repo.findRecallDo2(employee,leaveDo2FromDate);
				if (recallDo2List.size()!=0) {
					if (recallDo2List.get(0).getToDate().equals(leaveDo2.getToDate()));
					return false;
				} 
			return true;
		}
			
	  }
		
		
		return false;
	}
	
}
