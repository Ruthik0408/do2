package itsdc.dolphin.do2.auditcage;
 
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.SoldierGroup;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Latenr extends PromotBase implements Do2AuditCageProcessor, java.io.Serializable {

	static public final String NOT_ADMISSIBLE = "AU:PBOR should belong to DSC CORPS.\n";
	static public final String INVALID_GROUP_CATEGORY = "AU:Group and Category Does Not Match.\n";
	static public final String INCORRECT_GROUP = "AU:Group is incorrect in 6CPC.\n";
	static public final String INVALID_RANK = "AU:PBOR Existing Rank is not below DoII ColumnA.\n";
	static public final String RANK_NOT_AVAILABLE = "AU:Service Details Not Available To Arrive the Rank of the PBOR.\n";
	
	Employee employee;
	Rank rank;
	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		StringBuffer buffer = new StringBuffer();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
        rank = Utils.getPborRank(do2.getArmyNo(),do2.getFromDate(),"PROMOT");
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (newDo2.getId() == do2.getId()) {
				if (!isGroupValid(do2)) {
					if (do2.getFromDate().before(DateManager.getSixthCpcDate())){
						buffer.append(INVALID_GROUP_CATEGORY);	
					} else {
						buffer.append(INCORRECT_GROUP);
					}
					
					validity = false;
				}				
			}
		} else {
			buffer.append(NOT_ADMISSIBLE);
			validity = false;
		}
		
        if (!isValidRank(do2)){
        	validity = false;
        	buffer.append("Not a valid rank");
        }
        
		do2.setReason(buffer.toString());

		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		}

		do2Repo.save(do2);

		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay(getBasicPay(newAuditCage));
		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			newAuditCage.setGradePay(getGradePay(newAuditCage));
	//		newAuditCage.setBasicPay(getBasicPay(newAuditCage));
		} else {
			if (newAuditCage.getGradePay()==0) {
				if (!Utils.isAcpBeforeCurrentDo2(newAuditCage.getDo2())) {             // condition added on 14.02.2024.
					get7cpcPay(newAuditCage);
					newAuditCage.setOldBasicPay(getOldBasicPay(newAuditCage, baseAuditCage));	
				}				
			}			
		}
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		final String[] alwUnderProcess = { "APTPAY" };
		final List<String> alwToStop = Arrays.asList(alwUnderProcess);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		updateBdtInfo(do2);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {
		
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	 		
	 	if (refDo2.getColumnA().equals("PAHAV") || refDo2.getColumnA().equals("SBHAV")) {	 		
	 		
	 		List<Do2> do2List = do2Repo.findByEmployeeIdAndDescriptionAndColumnA(employee.getId(), "LATENR", "PANK");
	 				
	 	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND " +
	 									" d.description='LATENR' AND d.status='V' AND d.columnA='PANK' ")
	 									.setParameter("emp", employee).getResultList();*/
	 		
	 		if (do2List.size()>0) {
	 			rank=rankRepo.findByRankCode("30");
	 		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", do2List.get(0).getColumnA().substring(2));
	 			employee.setActingRankDate(do2List.get(0).getFromDate());
	 			employee.setSubstantiveRankDate(do2List.get(0).getFromDate());
	 			employee.setSubstantiveRank(rank);
	 			employee.setActingRank(rank);
	 		} else {
	 			do2List=null;
	 			do2List = do2Repo.findByEmployeeIdAndDescriptionAndColumnA(employee.getId(), "LATENR", "SBNK");
	 			if (do2List.size()>0) {
		 			rank=rankRepo.findByRankCode("30");
		 		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", do2List.get(0).getColumnA().substring(2));
		 			employee.setActingRankDate(do2List.get(0).getFromDate());
		 			employee.setSubstantiveRankDate(do2List.get(0).getFromDate());
		 			employee.setSubstantiveRank(rank);
		 			employee.setActingRank(rank);
		 		} else {
		 			rank=rankRepo.findByRankCode("20");
			 		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", "SEP");
			 		employee.setActingRankDate(employee.getEnrolmentDate());
			 		employee.setSubstantiveRankDate(employee.getEnrolmentDate());
			 		employee.setSubstantiveRank(rank);
			 		employee.setActingRank(rank); 			
		 		}
	 			
	 		}
	 		employee.setHavDate(null);
		}
	 	
	 	if (refDo2.getColumnA().equals("PANK") || refDo2.getColumnA().equals("SBNK")) {
	 		List<Do2> do2List = do2Repo.findByEmployeeIdAndDescriptionAndColumnA(employee.getId(), "LATENR", "PAHAV");
	 				
	 			/*	Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:emp AND " +
	 									" d.description='LATENR' AND d.status='V' AND d.columnA='PAHAV' ")
	 									.setParameter("emp", employee).getResultList();*/
	 		
	 		if (do2List.size()>0) {
	 			rank=rankRepo.findByRankCode("40");
	 	//		rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", do2List.get(0).getColumnA().substring(2));
	 			employee.setActingRankDate(do2List.get(0).getFromDate());
	 			employee.setSubstantiveRankDate(do2List.get(0).getFromDate());
	 			employee.setSubstantiveRank(rank);
	 			employee.setActingRank(rank);
	 		} else {
	 			do2List=null;	
	 			do2List = do2Repo.findByEmployeeIdAndDescriptionAndColumnA(employee.getId(), "LATENR", "SBHAV");
	 			if (do2List.size()>0) {
		 			rank=rankRepo.findByRankCode("40");
		 	//		rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", do2List.get(0).getColumnA().substring(2));
		 			employee.setActingRankDate(do2List.get(0).getFromDate());
		 			employee.setSubstantiveRankDate(do2List.get(0).getFromDate());
		 			employee.setSubstantiveRank(rank);
		 			employee.setActingRank(rank);
		 		} else {		 				 		
		 			rank=rankRepo.findByRankCode("20");
		 		//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", "SEP");
		 			employee.setActingRankDate(employee.getEnrolmentDate());
		 			employee.setSubstantiveRankDate(employee.getEnrolmentDate());
		 			employee.setSubstantiveRank(rank);
		 			employee.setActingRank(rank);
		 		}
	 		}
	 		employee.setNaikDate(null);
		}
	 	empRepo.save(employee);		 
	//	Utils.getEntityManager().persist(employee);
	//	Utils.getEntityManager().flush();
		
		return Do2Manager.SUCCESS;
	}

	private void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());		
		String rankString = do2.getColumnA().substring(2);
		if (rankString.trim().equals("HAV")) {
			rankString="40";
		}
		if (rankString.trim().equals("NK")) {
			rankString="30";
		}
		Rank promotedRank=rankRepo.findByRankCode(rankString);
	//	Rank promotedRank = (Rank) Utils.getForeignKey(new Rank(), "rankName", rankString);
		employee.setSubstantiveRank(promotedRank);
		employee.setActingRank(promotedRank);
		employee.setSubstantiveRankDate(do2.getFromDate());
		employee.setActingRankDate(do2.getFromDate());
        employee.setAppointmentRate(null);
        employee.setAppointmentDate(null);
		if (rankString.equals("30"))
			employee.setNaikDate(do2.getFromDate());

		if (rankString.equals("40"))
			employee.setHavDate(do2.getFromDate());
         
		empRepo.save(employee);	
	//	Utils.getEntityManager().persist(employee);
	//	Utils.getEntityManager().flush();
	}

	private boolean isGroupValid(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		GroupCategory groupCategory = Utils.getGroupCategory(do2,"PREV");
		String group = groupCategory.getSoldierGroup().toString();

		if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
			if (group.equals("Y") && do2.getColumnC().equals("CLKGD")) {
				return true;
			}
			if (group.equals("Z") && do2.getColumnC().equals("DSCGD")) {
				return true;
			}
		} else {
			if (do2.getColumnC().equals("CLKSD") || do2.getColumnC().equals("CLKGD") || do2.getColumnC().equals("DSCGD")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isValidRank(Do2 do2){
        int rankCode = Integer.parseInt(rank.getRankCode());
        int do2RankCode=0;
        String rankName=do2.getColumnA().substring(2);
        if (rankName.contains("NK")) {
        	do2RankCode=20;
        } else if (rankName.contains("HAV")) {
        	do2RankCode=30;
        }
		        
	  	return (rankCode == do2RankCode);
	}
	
	/*//this method replaced on 16042012
	public int getBasicPay(AuditCage newAuditCage) {
		Do2 currentDo2 = newAuditCage.getDo2();
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		String rankString = currentDo2.getColumnA().substring(2);
		char soldierGroup=' ';
		GroupCategory gp = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV");
		soldierGroup =gp.getSoldierGroup();
		rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", rankString);
		int basicPay=newAuditCage.getBasicPay();
	
		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			basicPay=newAuditCage.getOldBasicPay();
		} 
		List<PayScale> payList = Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
						+ " AND ps.soldierGroup.soldierGroup=:grp AND ps.fromDate='1997-10-10' and ps.toDate='2005-12-31' ")
						.setParameter("rank", rank).setParameter("grp", soldierGroup).getResultList();

		if (payList.size() > 0) {
			for (int i=payList.get(0).getStartScale(); i<=payList.get(0).getScaleEnd0(); i+=payList.get(0).getIncr0()) {
				if (i>=basicPay) {
					System.out.println ( " AAAAA  " +i);
					return i;
				}
			}
		}
		
		return newAuditCage.getBasicPay();
	}
	*/
	
	//this method replaced on 16042012
	public int getBasicPay(AuditCage newAuditCage) {
		Do2 currentDo2 = newAuditCage.getDo2();
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		String rankString = currentDo2.getColumnA().substring(2);
		String soldierGroup="";
		GroupCategory gp = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV");
		soldierGroup =gp.getSoldierGroup();
		rank=rankRepo.findByRankCode(rankString);
	//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", rankString);
		int basicPay=newAuditCage.getBasicPay();
	
		System.out.println ( " BASIC PAY " +basicPay);
	
		/*if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) &&
				!newAuditCage.getFromDate().after(DateManager.getSeventhCpcDate())) {
			System.out.println ( " BASIC PAY111111 " +basicPay);
			basicPay=newAuditCage.getOldBasicPay();
		} */
		
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				List<PayScale> payList=null;
				try {
					payList = psRepo.findByFromdateToDateRankAndSoldierGroup(rank.getId(), soldierGroup, 
																DateManager.getDate("1997-10-10"), DateManager.getDate("2005-12-31"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
						
			/*			Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
						+ " AND ps.soldierGroup.soldierGroup=:grp AND ps.fromDate='1997-10-10' and ps.toDate='2005-12-31' ")
						.setParameter("rank", rank).setParameter("grp", soldierGroup).getResultList();*/
	
				if (payList.size() > 0) {
					for (int i=payList.get(0).getStartScale(); i<=payList.get(0).getScaleEnd0(); i+=payList.get(0).getIncr0()) {
						if (i>=basicPay) {
							System.out.println ( " AAAAA  " +i);
							return i;
						}
					}
				}	
		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			basicPay=newAuditCage.getOldBasicPay();
		}
		
		
		return newAuditCage.getBasicPay();
	}
	
	public int getGradePay(AuditCage newAuditCage) {
		Do2 currentDo2 = newAuditCage.getDo2();
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		String rankString = currentDo2.getColumnA().substring(2); 
	//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", rankString);
		if (rankString!=null && rankString.equals("NK")) {
			rankString="30";
		}
		if (rankString!=null && rankString.equals("HAV")) {
			rankString="40";
		}
		rank = rankRepo.findByRankCode(rankString);
		List<PayScale> payList=null;
		try {
			payList = psRepo.findByRankAndSoldierGroupAndFromDate(rank.getId(), "Y", DateManager.getDate("2006-01-01"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		//		Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
		//				+ " AND ps.soldierGroup.soldierGroup='Y' AND ps.fromDate='2006-01-01' ").setParameter("rank", rank)
		//				.getResultList();
 		if (payList.size() > 0) {
			return payList.get(0).getGradePay();
		}
	
		return newAuditCage.getGradePay();
	}
	 
	public void get7cpcPay(AuditCage newAuditCage) {
		Do2 currentDo2 = newAuditCage.getDo2();
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		String rankString = currentDo2.getColumnA().substring(2);
		String soldierGroup="";
		GroupCategory gp = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV");
		soldierGroup =gp.getSoldierGroup();
		System.out.println("..........rank string from latenr " +rankString);
		if (rankString.trim().equals("HAV")) {
			rankString="40";			
		}  
		if (rankString.trim().equals("NK")) {
			rankString="30";			
		} 
		rank = rankRepo.findByRankCode(rankString);
	//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", rankString);
		Integer newBasic = 0;
		Integer basicpay = newAuditCage.getBasicPay();
		
		Rank currentRank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PROMOT");   	
		
		String currentPayLevel = Do2Manager.getPayLevelPromot(currentRank.getRankCode());
		String promotPayLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
		
		
		
		List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(promotPayLevel, basicpay);
				
		/*		Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ "  pm.basicPay>:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", promotPayLevel)
				.setParameter("sevencpcpay", basicpay)
				.getResultList();*/
		
		if (paymatrixList.size() > 0) {
			newBasic = paymatrixList.get(0).getBasicPay();
		}
		
		/*List<PayMatrix>paymatrixList1 = Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ " pm.basicPay>=:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", promotPayLevel)
				.setParameter("sevencpcpay", newBasic)
				.getResultList();
		
		if (paymatrixList.size() > 0) {
			newBasic = paymatrixList1.get(0).getBasicPay();
		}*/
		
		System.out.println (" basic pay " +newBasic  +" rank " +currentRank.getRankCode() +"EMPLOYEE " +employee.getArmyNo());
		newAuditCage.setBasicPay(newBasic);
		newAuditCage.setOldBasicPay(getBasicPay(newAuditCage));
		newAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
		newAuditCage.setPayLevel(promotPayLevel);
		//employee.setIncrementDate(Utils.getSevenPcNextIncrementDate(nAuditCage.getDo2().getFromDate()));
	//	Utils.getEntityManager().flush();
		
	}
	
	
	public int getOldBasicPay(AuditCage newAuditCage, AuditCage baseAuditCage) {
		Do2 currentDo2 = newAuditCage.getDo2();
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		String rankString = currentDo2.getColumnA().substring(2);
		String soldierGroup="";
		GroupCategory gp = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV");
		soldierGroup =gp.getSoldierGroup();
	//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankName", rankString);
		if (rankString.trim().equals("HAV")) {
			rankString="40";			
		}  
		if (rankString.trim().equals("NK")) {
			rankString="30";			
		} 
		rank = rankRepo.findByRankCode(rankString);
		int oldBasicPay=baseAuditCage.getOldBasicPay();
		System.out.println ( " BASE AUDIT CAGE " +baseAuditCage.getOldBasicPay());
		System.out.println ( " BASE AUDIT CAGE 11 " +baseAuditCage.getBasicPay());
		List<PayScale> payList=null;
		try {
			payList = psRepo.findByFromdateToDateRankAndSoldierGroup(rank.getId(), soldierGroup, 
																DateManager.getDate("1997-10-10"), DateManager.getDate("2005-12-31"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
				
			//	Utils.getEntityManager().createQuery("SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
		//				+ " AND ps.soldierGroup.soldierGroup=:grp AND ps.fromDate='1997-10-10' and ps.toDate='2005-12-31' ")
			//			.setParameter("rank", rank).setParameter("grp", soldierGroup).getResultList();

		if (payList.size() > 0) {
			for (int i=payList.get(0).getStartScale(); i<=payList.get(0).getScaleEnd0(); i+=payList.get(0).getIncr0()) {
				if (i>=oldBasicPay) {
					System.out.println ( " AAAAA  " +i);
					return i;
				}
			}
		}
		
		return baseAuditCage.getOldBasicPay();
	}

	@Override
	public void updateBdtInfo(Do2 do2) {
		updateEmployee(do2);
	}

}