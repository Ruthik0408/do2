package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.ReadyReckoner;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;
 

@RestController
public class Payfix extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee = null;
	List<GroupCategory> gcList = null;

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
			
		do2.setStatus(Do2Manager.VALID);
	
		if (do2.getColumnA()!=null) {
 			if (!isValidColumnA(do2)) {
 				do2.setStatus(Do2Manager.INVALID);
 				do2.setReason("Invalid value given in column_a / Amount should be rounded off to nearest ten \n");
 				validity=false;
 			}
 		}
		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
	 
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
	//	Rank rank = Utils.getRankForPayFixation(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate());
		Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"PAYFIX");
		if (employee.getSubstantiveRank()!=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)
				|| (baseAuditCage.getBasicPay()==2440 || baseAuditCage.getBasicPay()==2850)){
			newAuditCage.setBasicPay(5700);
			newAuditCage.setDp(0);
			newAuditCage.setDa(0);
			newAuditCage.setMsPay(0);
			newAuditCage.setXGroupPay(0);
			newAuditCage.setTransport(0);
			newAuditCage.setPmha(0);
			newAuditCage.setLra(0);
			newAuditCage.setClassPay(0);
			return newAuditCage;
		}	 
		
		if (rank.getRankCode().equals(Do2Manager.RECRUIT) && employee.getEnrolmentDate().before(DateManager.getSixthCpcDate()))  {
			if (employee.getInitialMusteringDate()!=null && 
					!employee.getInitialMusteringDate().before(DateManager.getSixthCpcDate())) {
				rank=rankRepo.findByRankCode("20");
			//	 rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"20");
			}
			if (employee.getAttestationDate()!=null && 
					!employee.getAttestationDate().before(DateManager.getSixthCpcDate())) {
				rank=rankRepo.findByRankCode("20");
			//	 rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"20");
			}
		}
		 		
		String soldierGroup = "";

		int basicPay = newAuditCage.getBasicPay();
		
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"PREV");
		if(groupCategory!=null)
			System.out.println("utils gp "+groupCategory.getSoldierGroup()+"--"+groupCategory.getCategoryName());
		else
			System.out.println("gp null");
		if (groupCategory != null) {
			soldierGroup = groupCategory.getSoldierGroup();
		} else {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)
					&& !newAuditCage.getFromDate().after(DateManager.getSixthCpcDate())) {
				String catg=null;
				List<EmployeeServiceDetail> esdGroupCategoryList = esdRepo.findByFromDateLessThanEqualAndStatus(employee.getId(),
																										newAuditCage.getFromDate(), "R");
						
			/*			Utils.getEntityManager()
						.createQuery("SELECT esd FROM EmployeeServiceDetail esd WHERE esd.employee=:emp AND "
										+ " esd.status='R' and esd.fromDate <= :frdt ORDER BY esd.fromDate DESC")
						.setParameter("emp", employee).setParameter("frdt", newAuditCage.getFromDate()).getResultList();*/
				if (esdGroupCategoryList!=null && esdGroupCategoryList.size()>0) {
					if (esdGroupCategoryList.get(0).getDo2().getColumnC().endsWith("Y")) {
						catg = "CLKGD";							
					} else {
						catg = "DSCGD";
					}
					groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
					//groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
				} else {
					if (basicPay<3250) {
						catg = "DSCGD";
						groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
					//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
					} else {
						for (int i=3250; i<=4300; i+=70) {
							if (basicPay==i) {
								catg = "CLKGD";
								groupCategory=gcRepo.findBySoldierCategory(catg).get(0);
							//	groupCategory  = (GroupCategory)(Utils.getForeignKey(new GroupCategory(), "soldierCategory", catg));
								break;
							}
						}
						if (catg==null) {
							for (int i=3050; i<=3875; i+=55) {
								if (basicPay==i) {
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
			if (groupCategory != null) {
				soldierGroup = groupCategory.getSoldierGroup();
			}
		}
		
		if (soldierGroup.equals("Y")) { 
			if (basicPay<3250) {
				basicPay=3250;
			}
		}
		
		System.out.println("............... rank " +rank.getId()+"  grp "+soldierGroup+"  basic " + newAuditCage.getBasicPay()+"  fromdate "+newAuditCage.getDo2().getFromDate());		
		List<ReadyReckoner> reckonerList = rrRepo.findByRankGroupBasicAndFromDateGreaterThanEqual(rank, soldierGroup,
																					newAuditCage.getDo2().getFromDate(), basicPay);
				
			/*	Utils.getEntityManager().createQuery(
						"SELECT rl FROM ReadyReckoner AS rl WHERE "
								+ " rl.rank=:rnk AND "
								+ " rl.soldierGroup.soldierGroup=:grp AND "
							 	+ " :frdt >= rl.effectiveDate AND "
								+ " rl.oldBasic =:basic")
				.setParameter("rnk",rank)
				.setParameter("grp",soldierGroup)
				.setParameter("frdt", newAuditCage.getDo2().getFromDate())
				//.setParameter("basic", newAuditCage.getBasicPay())
				.setParameter("basic", basicPay)
				.getResultList();*/
	 	
		if (soldierGroup.equals("Z") && !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			//System.out.println("lands here");
			soldierGroup="Y";
		}
		//System.out.println("6cpc "+DateManager.getSixthCpcDate());
		//System.out.println("==========="+rank.getId()+"--"+soldierGroup+"--"+newAuditCage.getDo2().getFromDate()+"--"+newAuditCage.getFromDate()+"--"+newAuditCage.getDo2().getDescription());
		
       	List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(), soldierGroup,
       			newAuditCage.getDo2().getFromDate());
       			
       			
       /*			Utils.getEntityManager().createQuery("SELECT pl FROM PayScale AS pl WHERE "
								+ " pl.rank=:rnk AND "
								+ " pl.soldierGroup.soldierGroup=:soldierGroup AND "
								+ " ((:fromDate BETWEEN pl.fromDate AND pl.toDate ) OR "
								+ "  (:fromDate >= pl.fromDate AND pl.toDate IS null )) AND "
								+ " pl.startScale IS NOT null")
				.setParameter("rnk",rank)
				.setParameter("soldierGroup", soldierGroup)
				.setParameter("fromDate", newAuditCage.getDo2().getFromDate())
				.getResultList();*/
 
       	if (newAuditCage.getDo2().getColumnA()!=null && newAuditCage.getDo2().getColumnA().length()!=0){
			newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));
			newAuditCage.setGradePay(payList.get(0).getGradePay());
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setOldClassPay(baseAuditCage.getOldClassPay());
			if (soldierGroup.equals("X")){
				newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
			}			
		} else {
			if (reckonerList.size() > 0) {
				newAuditCage.setBasicPay(reckonerList.get(0).getNewBasic());
				newAuditCage.setGradePay(payList.get(0).getGradePay());
				newAuditCage.setDp(0);
				newAuditCage.setOldBasicPay(baseAuditCage.getBasicPay());
				newAuditCage.setOldClassPay(baseAuditCage.getOldClassPay());
				if (soldierGroup.equals("X")) {
					newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
				}
			}	
			//System.out.println("1111111111111111 ");
		}
       //	System.out.println("............... reckonerlist size  "+reckonerList.size()+"  payscalelist size " +payList.size() +" soldiergroup "+soldierGroup);
       
       	if (employee.getRegularTaFlag().equals("T")) {
       		return newAuditCage;
       	}
       	
       	// Added the below code on 26.06.2021 to handle direct entry pay for affected IRLAs.
       	if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && payList!=null && payList.size()>0
       			&& payList.get(0).getNewRecruitPay()!=null && newAuditCage.getBasicPay()<payList.get(0).getNewRecruitPay()
       			&& employee.getEnrolmentDate()!=null && employee.getEnrolmentDate().before(DateManager.getSixthCpcDate())
       			&& (newAuditCage.getGradePay()==2000 || newAuditCage.getGradePay()==2800 || newAuditCage.getGradePay()==4200)) {
       		
       		if (newAuditCage.getGradePay()==2000) {
       			newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());	
       		} 
       		
       		
       		if (newAuditCage.getGradePay()==2800) {
       			List<String> entitledHavildarCategory = Arrays.asList("INSTR", "INSTRSPL", "SURVCARTO");
       	//		System.out.println ( " group category " +groupCategory.getSoldierCategory());
       			if (groupCategory!=null && entitledHavildarCategory.contains(groupCategory.getSoldierCategory())) {
       		//		System.out.println ( " iam here 11 "  +groupCategory.getSoldierCategory());
       				if (employee!=null && employee.getEnrolmentDate()!=null && employee.getHavDate()!=null 
    	 					&& employee.getEnrolmentDate().equals(employee.getHavDate())) {
       					newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
       				} else if (employee!=null && employee.getEnrolmentDate()!=null && employee.getNaikDate()==null 
       						&& employee.getAcpnaikDate()==null && employee.getAcphavDate()==null
       						&& employee.getHavDate()!=null && employee.getGraduateHavildarDate()!=null  
       						&& employee.getGraduateHavildarFlag()!=null
       						&& employee.getHavDate().after(employee.getEnrolmentDate())
    	 					&& employee.getHavDate().equals(employee.getGraduateHavildarDate())) {
       			//		System.out.println ( " iam here "  +employee.getGraduateHavildarDate());
       					newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
       				}
       			} else {
       				/*if (employee!=null && employee.getEnrolmentDate()!=null && employee.getHavDate()!=null && employee.getSportHavildarFlag()==true
       						&& employee.getNaikDate()==null && employee.getAcpnaikDate()==null && employee.getAcphavDate()==null
       						&&(employee.getHavDate().equals(employee.getEnrolmentDate()) ||  employee.getHavDate().after(employee.getEnrolmentDate()))
    	 					&& employee.getHavDate().equals(employee.getSportHavildarDate())) {                   // condition added on 01.03.2023.
    	 					newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
    	 				*/	
    	 					
    	 			if (employee!=null && employee.getEnrolmentDate()!=null && employee.getHavDate()!=null && employee.getSportHavildarFlag()==true
    	       			//	&& employee.getNaikDate()==null && employee.getAcpnaikDate()==null && employee.getAcphavDate()==null
    	       				&&(employee.getHavDate().equals(employee.getEnrolmentDate()) ||  employee.getHavDate().after(employee.getEnrolmentDate()))
    	    	 			&& employee.getHavDate().equals(employee.getSportHavildarDate())) {                   // condition added on 01.03.2023.
    	    	 			newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
    	 			}
       			}
       		}
       		
       		/*
       		if (newAuditCage.getGradePay()==2800) {
       			if (employee!=null && employee.getEnrolmentDate()!=null && employee.getHavDate()!=null 
	 					&& employee.getEnrolmentDate().equals(employee.getHavDate())) {
       				
       				List<String> entitledHavildarCategory = Arrays.asList("INSTR", "INSTRSPL", "SURVCARTO");
       				
       				if (groupCategory!=null && entitledHavildarCategory.contains(groupCategory.getSoldierCategory())) {
	 					newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
	 				} else if (employee.getSportHavildarFlag()!=null && employee.getSportHavildarFlag()==true 
	 							&& employee.getSportHavildarDate()!=null && employee.getEnrolmentDate().equals(employee.getSportHavildarDate())) {
	 					newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
	 				}	 				
	 			} else if (employee!=null && employee.getEnrolmentDate()!=null && employee.getHavDate()!=null
	 					&& employee.getHavDate().after(employee.getEnrolmentDate())
	 					&& employee.getHavDate().equals(employee.getSportHavildarDate()) && employee.getSportHavildarFlag()==true
	 					&& employee.getNaikDate()==null && employee.getAcpnaikDate()==null &&  employee.getAcphavDate()==null) {                   // condition added on 01.03.2023.
	 					
	 					newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
	 			}
       		}*/
       		
       		if (newAuditCage.getGradePay()==4200) {
	 			if (employee!=null && employee.getEnrolmentDate()!=null && employee.getNbsubDate()!=null 
	 					&& employee.getEnrolmentDate().equals(employee.getNbsubDate())) {
	 				newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
	 			} else if (employee!=null && employee.getMusterJcoFlag()!=null && employee.getMusterJcoFlag()==true
	 						&& employee.getNbsubDate().before(DateManager.getSixthCpcDate())) {
	 				newAuditCage.setBasicPay(payList.get(0).getNewRecruitPay());
	 			} 
	 		}       		
       	}
 		//	System.out.println("..............  newauditcage basic "+newAuditCage.getBasicPay()+"   gradepay "+newAuditCage.getGradePay()); 
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isValidColumnA(Do2 do2) {
		
		try {
			if ((Integer.parseInt(do2.getColumnA()))%10==0)
				return true;
		} catch (Exception e) {
			Utils.handleException(e);
			return false;
		}
		return false;
	}
	
}