package itsdc.dolphin.do2.auditcage;

import java.util.List;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EnrollDo2;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

public class Enrmst extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	
	Employee employee;
	List<PayScale> payList = null;
	List<Allowance> alwList = null;
	List<PayMatrix> paymatrixList = null;
//	List<ClassPayRate> clsPayList = null;

	@Override
	public boolean isValid(Do2 do2) {
		do2.setStatus(Do2Manager.VALID);
	
		do2Repo.save(do2);
 		
		return true;
	}

/*@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {

	
 	    updateEmployee(newAuditCage.getDo2());
 	    
 		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
  		char soldierGroup = 'Y';
  		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"CUR");
  		Rank rank = Utils.getPborRank(employee.getArmyNo(), newAuditCage.getFromDate(),"PAYFIX");
  		
  		
  		
  		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
  			if (employee.getGroupCategory()!=null && employee.getGroupCategory().getSoldierGroup()=='Z') {
  				soldierGroup='Y';
  			} else if (employee.getGroupCategory()!=null) {
  				soldierGroup=employee.getGroupCategory().getSoldierGroup();
  			}
  		} else if (employee.getGroupCategory()!=null) {
				soldierGroup=employee.getGroupCategory().getSoldierGroup();
  		}
 		
		payList = Utils.getEntityManager().createQuery("SELECT ps FROM PayScale ps WHERE "
					+ " ps.rank=:rank AND ps.soldierGroup.soldierGroup=:soldierGroup AND " +
			" ps.startScale > 0 AND ((:fromDate between ps.fromDate and ps.toDate)  or (:fromDate >= ps.fromDate and ps.toDate is null))")
				.setParameter("rank", employee.getSubstantiveRank())
				.setParameter("fromDate", employee.getEnrolmentDate())
				.setParameter("soldierGroup", soldierGroup)
				.getResultList();
 		
		int pmhaRate = AuditCageHelper.getPmhaRate(newAuditCage.getDo2(), newAuditCage);
		
		// The following lines commented since the same was added in MUSTER 
		clsPayList = Utils.getEntityManager().createQuery("SELECT cp FROM ClassPayRate AS cp WHERE "
								+ " cp.groupCategory =:groupCategory AND cp.classPayCode =:soldierClass AND "
								+ " ((:fromDate BETWEEN cp.fromDate AND cp.toDate) OR (:fromDate > cp.fromDate AND cp.toDate IS null))")
				.setParameter("groupCategory", groupCategory)
				.setParameter("soldierClass", groupCategory.getEntryClass())
				.setParameter("fromDate", employee.getEnrolmentDate())
				.getResultList();

		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			
			newAuditCage.setBasicPay(payList.get(0).getStartScale());
			
			if (!(newAuditCage.getFromDate().before(DateManager.getDPDate()))) {
				newAuditCage.setDp((int) Math.round(newAuditCage.getBasicPay()*0.50));
			}
			
		} else { 			
			newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
			newAuditCage.setGradePay(payList.get(0).getGradePay());
		
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				newAuditCage.setMsPay(payList.get(0).getMsp());
			}
			
			if (employee.getGroupCategory().getSoldierGroup()=='X') {
				newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
			}

			newAuditCage.setOldBasicPay(getOldBasicPay(newAuditCage.getDo2()));
	 	}
		
	// The following lines commented since the same was added in MUSTER 
	//	if (Do2Manager.NCO_RANKS.contains(employee.getSubstantiveRank().getRankCode())) {
		if (rank!=null) {
			if (Integer.parseInt(rank.getRankCode()) < 50) {
				if (clsPayList.size() > 0 && clsPayList.get(0).getRate() > 0) {
					newAuditCage.setClassPay((int) Math.round(clsPayList.get(0).getRate()));
				}
			}
		}
		
		
		newAuditCage.setPmha(pmhaRate);
		if (employee.getSportHavildarFlag() != null && employee.getSportHavildarFlag()== true && 
			employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			newAuditCage.setPmha(pmhaRate);
		} else if (employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true && 
			employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			newAuditCage.setPmha(pmhaRate);
		} else {
			newAuditCage.setPmha((pmhaRate*3)/4);
		}
	
		return newAuditCage;
	}*/

	// above method commented and amended as below due to sevencpc
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
 	    updateEmployee(newAuditCage.getDo2());
 		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
  		String soldierGroup = "Y";
  		
  		//List <Rank> rankList = null;
  		Rank rank;
  		if (employee.getMusterJcoFlag() !=null && employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("50");
	//		rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getGraduateHavildarFlag() !=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
	//		rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		//	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
	    } else {
	    	rank=rankRepo.findByRankCode("20");
	//    	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='20'").getResultList();
	    }
	//	Rank rank = rankList.get(0);
  		String enrmstDo2Rank = newAuditCage.getDo2().getColumnB();
  		if (enrmstDo2Rank.equals("RT")) {
  			rank=rankRepo.findByRankCode("50");
  		}
  		String payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
  		
  		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
  			if (employee.getGroupCategory()!=null && employee.getGroupCategory().getSoldierGroup().equals("Z")) {
  				soldierGroup="Y";
  			} else if (employee.getGroupCategory()!=null) {
  				soldierGroup=employee.getGroupCategory().getSoldierGroup();
  			}
  		} else if (employee.getGroupCategory()!=null) {
				soldierGroup=employee.getGroupCategory().getSoldierGroup();
  		}
  		
  		soldierGroup = newAuditCage.getDo2().getColumnA().substring(3);
 		
		payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleGreaterThanZero(rank.getId(),
																						soldierGroup, employee.getEnrolmentDate());
				
	/*			Utils.getEntityManager().createQuery("SELECT ps FROM PayScale ps WHERE "
					+ " ps.rank=:rank AND ps.soldierGroup.soldierGroup=:soldierGroup AND " +
			" ps.startScale > 0 AND ((:fromDate between ps.fromDate and ps.toDate)  or (:fromDate >= ps.fromDate and ps.toDate is null))")
				.setParameter("rank", rank)
				.setParameter("fromDate", employee.getEnrolmentDate())
				.setParameter("soldierGroup", soldierGroup)
				.getResultList();*/
 		
		paymatrixList = matrixRepo.findByPayLevelOrderByBasicPayAsc(payLevel);
				
		/*		Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel order by basicPay asc ")
				.setParameter("paylevel", payLevel)
				.getResultList();*/
		
		
		int pmhaRate = AuditCageHelper.getPmhaRate(newAuditCage.getDo2(), newAuditCage);
		
		// The following lines commented since the same was added in MUSTER 
		/*clsPayList = Utils.getEntityManager().createQuery("SELECT cp FROM ClassPayRate AS cp WHERE "
								+ " cp.groupCategory =:groupCategory AND cp.classPayCode =:soldierClass AND "
								+ " ((:fromDate BETWEEN cp.fromDate AND cp.toDate) OR (:fromDate > cp.fromDate AND cp.toDate IS null))")
				.setParameter("groupCategory", groupCategory)
				.setParameter("soldierClass", groupCategory.getEntryClass())
				.setParameter("fromDate", employee.getEnrolmentDate())
				.getResultList();*/

		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			
			newAuditCage.setBasicPay(payList.get(0).getStartScale());
			
			if (!(newAuditCage.getFromDate().before(DateManager.getDPDate()))) {
				newAuditCage.setDp((int) Math.round(newAuditCage.getBasicPay()*0.50));
			}
			
			if (newAuditCage.getEmployee().getRegularTaFlag().equals("T")) {
		 		newAuditCage.setAbsentFlag(baseAuditCage.getAbsentFlag());
		 	}
			
		} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){ 			
			newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
			newAuditCage.setGradePay(payList.get(0).getGradePay());
		
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				newAuditCage.setMsPay(payList.get(0).getMsp());
				newAuditCage.setMsPay(getMsPay(newAuditCage.getDo2()));
			}
			
			if (employee.getGroupCategory().getSoldierGroup().equals("X")) {
				newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
			}

			newAuditCage.setOldBasicPay(getOldBasicPay(newAuditCage.getDo2()));
			newAuditCage.setOldGradePay(0);
			newAuditCage.setPayLevel("0");
			if (newAuditCage.getEmployee().getRegularTaFlag().equals("T")) {
		 		newAuditCage.setAbsentFlag(baseAuditCage.getAbsentFlag());
		 	}
	 	} else {
	 				
			if (paymatrixList.size() > 0) {
				newAuditCage.setBasicPay(paymatrixList.get(0).getBasicPay());
			//	newAuditCage.setMsPay(getMsPay(newAuditCage.getDo2()));
				newAuditCage.setPayLevel(paymatrixList.get(0).getPayLevel());
				newAuditCage.setGradePay(0);
				if (employee.getGroupCategory().getSoldierGroup().equals("X")) {
					newAuditCage.setXGroupPay(3600);  // lower x group pay adjusted . On AICTGP do2 publication higher rate will be admitted.
				}
			}
			if (newAuditCage.getEmployee().getRegularTaFlag().equals("T")) {
		 		newAuditCage.setAbsentFlag(baseAuditCage.getAbsentFlag());
		 	}
			get6cpcOldPay(newAuditCage, baseAuditCage);
		}
		
	// The following lines commented since the same was added in MUSTER 
	//	if (Do2Manager.NCO_RANKS.contains(employee.getSubstantiveRank().getRankCode())) {
		/*if (rank!=null) {
			if (Integer.parseInt(rank.getRankCode()) < 50) {
				if (clsPayList.size() > 0 && clsPayList.get(0).getRate() > 0) {
					newAuditCage.setClassPay((int) Math.round(clsPayList.get(0).getRate()));
				}
			}
		}*/
		
		
		newAuditCage.setPmha(pmhaRate);
		/*if (employee.getSportHavildarFlag() != null && employee.getSportHavildarFlag()== true && 
			employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			newAuditCage.setPmha(pmhaRate);
		} else if (employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true && 
			employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			newAuditCage.setPmha(pmhaRate);
		} else {
			newAuditCage.setPmha((pmhaRate*3)/4);
		}*/
	
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage currentAuditCage) {
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
	// Following line commented on 25062013 since the ENRMST is generated by SYSTEM for admiting PAY.
	// There is no need to reverse on cancellation. The cancellation will taken place in MUSTER, ATTSBM/ATTSAM .
	//	reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}
 	
	private int getOldBasicPay(Do2 do2){
		int oldBasicPay=0;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//List <Rank> rankList = null;
		Rank rank;
		
		if (employee.getMusterJcoFlag() !=null && employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("50");
	//		rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		}else if (employee.getGraduateHavildarFlag() !=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		//	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		//	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
	    } else {
	    	rank=rankRepo.findByRankCode("20");
	   // 	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='20'").getResultList();
	    }
	//	Rank rank = rankList.get(0);
		try {
			payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleGreaterThanZero(rank.getId(),
																	employee.getGroupCategory().getSoldierGroup(), DateManager.getDate("1997-10-10"));
					
		/*			Utils.getEntityManager().createQuery("SELECT ps FROM PayScale ps WHERE "
								+ " ps.rank=:rank AND ps.soldierGroup.soldierGroup=:soldierGroup AND (ps.startScale > 0) AND "
								+ " (:fromDate between ps.fromDate and ps.toDate  or :fromDate >= ps.fromDate and ps.toDate is null)")
				.setParameter("rank", rank)
				.setParameter("fromDate", DateManager.getDate("1997-10-10"))
				.setParameter("soldierGroup", employee.getGroupCategory().getSoldierGroup())
				.getResultList();*/
		} catch (Exception e) { 
			Utils.handleException(e);
		}
		
		if (payList.size()>0)
			oldBasicPay =  payList.get(0).getStartScale();
		
		return oldBasicPay;
	}
	 
	private void reverseUpdateEmployee(Do2 refDo2) {
		
		 
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		
		employee.setInitialMusteringDate(null);
		employee.setCurrentClassPayDate(null);
		employee.setSoldierClass(null);
		employee.setIncrementDate(null);
		employee.setGroupCategory(null);
		Rank rank=rankRepo.findByRankCode("10");
		employee.setSubstantiveRank(rank);
	//	employee.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", "10"));
		employee.setSubstantiveRankDate(employee.getEnrolmentDate());
		StatusCode sc=scRepo.findByStatusCode("R");
		employee.setStatusCode(sc);
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", "R"));
		employee.setStatusEffectDate(employee.getEnrolmentDate());
		
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);
	//	Utils.getEntityManager().flush();
 	}
	
	private void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//List <Rank> rankList = null;
		Rank rank;
		
		if (employee.getMusterJcoFlag() !=null && employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("50");
	//		rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else	if (employee.getGraduateHavildarFlag() !=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		//	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		//	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
	    } else {
	    	rank=rankRepo.findByRankCode("20");
	    //	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='20'").getResultList();
	    }		
		if (do2.getColumnB()!=null && do2.getColumnB().length()>1 && !do2.getColumnB().startsWith("CL-")) {
			List<GroupCategory> gc=gcRepo.findBySoldierCategory(do2.getColumnB().trim().toUpperCase());
			if (gc.size()>0)
			employee.setGroupCategory(gc.get(0));
		//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "SoldierCategory", (do2.getColumnB()
		//			.trim().toUpperCase())));
		} else if (do2.getColumnC()!=null && do2.getColumnC().length()>1) {
			List<GroupCategory> gc=gcRepo.findBySoldierCategory(do2.getColumnC().trim().toUpperCase());
			if (gc.size()>0 )			
			employee.setGroupCategory(gc.get(0));
		//	employee.setGroupCategory((GroupCategory) Utils.getForeignKey(new GroupCategory(), "SoldierCategory", (do2.getColumnC()
			//			.trim().toUpperCase())));
			employee.setCurrentClassPayDate(do2.getFromDate());
			if (do2.getColumnB()!=null)
			employee.setSoldierClass(do2.getColumnB().charAt(3)+"");
		}  
		
		employee.setSubstantiveRank(rank);
		employee.setTermsColourService(rank.getColorService());
		employee.setTermsMaxAgeRetention(rank.getMaximumAge());
		
		if (employee.getSubstantiveRankDate()!=null && !employee.getSubstantiveRankDate().before(do2.getFromDate()))
			employee.setSubstantiveRankDate(do2.getFromDate());
		StatusCode sc=scRepo.findByStatusCode("E");
		employee.setStatusCode(sc);
	//	employee.setStatusCode((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", "E"));
		
		empRepo.save(employee);
	//	Utils.getEntityManager().persist(employee);
//		Utils.getEntityManager().flush();
	}
	
	
	public void get6cpcOldPay(AuditCage newAuditCage, AuditCage baseAuditCage) {
 		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
  		String soldierGroup = "Y";
  		//List <Rank> rankList = null;
  		Rank rank;
		if (employee.getGraduateHavildarFlag() !=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
	//		rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		//	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='40'").getResultList();
	    } else {
	    	rank=rankRepo.findByRankCode("20");
	  // 	rankList = Utils.getEntityManager().createQuery("SELECT rk FROM Rank rk WHERE rk.rankCode='20'").getResultList();
	    }
	//	Rank rank = rankList.get(0);
   		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
  			if (employee.getGroupCategory()!=null && employee.getGroupCategory().getSoldierGroup().equals("Z")) {
  				soldierGroup="Y";
  			} else if (employee.getGroupCategory()!=null) {
  				soldierGroup=employee.getGroupCategory().getSoldierGroup();
  			}
  		} else if (employee.getGroupCategory()!=null) {
				soldierGroup=employee.getGroupCategory().getSoldierGroup();
  		}
 		payList =psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleGreaterThanZero(rank.getId(),soldierGroup,employee.getEnrolmentDate());
 				
 		/*		Utils.getEntityManager().createQuery("SELECT ps FROM PayScale ps WHERE "
					+ " ps.rank=:rank AND ps.soldierGroup.soldierGroup=:soldierGroup AND " +
			" ps.startScale > 0 AND ((:fromDate between ps.fromDate and ps.toDate)  or (:fromDate >= ps.fromDate and ps.toDate is null))")
				.setParameter("rank",rank)
				.setParameter("fromDate", employee.getEnrolmentDate())
				.setParameter("soldierGroup", soldierGroup)
				.getResultList();*/
 		
		if (payList.size() > 0) {
			newAuditCage.setOldBasicPay(payList.get(0).getNewRecruitPay());
			newAuditCage.setOldGradePay(payList.get(0).getGradePay());
		}
	}
	
	public int getMsPay(Do2 do2){
		int mspay=0;
 		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
 			mspay=2000;
 		} else {
 			mspay=5200;
 		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		
		
		return mspay;
	}
	
 }