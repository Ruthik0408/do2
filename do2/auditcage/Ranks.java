package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.ReadyReckoner;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
@RestController
public class Ranks extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee employee = null;
	Rank rank = null;
	int probableBasic = 0;
	List<EmployeeServiceDetail>esdPromotList = null;
	
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		boolean ranksValidity = true;
		StringBuffer buffer = new StringBuffer();
		
		if (!isMusterAvailable(do2)) {
			do2.setReason("AU:Muster/ATTSBM/ATTSAM not available in ESD for pay fixation. \n");
			do2.setStatus(Do2Manager.PENDING);
			ranksValidity=false;
		}
		
		if (!do2.getFromDate().after(DateManager.getSixthCpcDate())) {
			if (!isOldBasicPayAvailable(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				ranksValidity = false;
			}
		}
		
		if(ranksValidity) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
		} else {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(buffer.toString());
		}
		
		do2Repo.save(do2);
	
		return ranksValidity;
		
	}
    @Override
    public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
        employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());

        System.out.println("[getNewAuditCage] fromDate=" + newAuditCage.getFromDate()
                + " | 6CPC=" + DateManager.getSixthCpcDate()
                + " | 7CPC=" + DateManager.getSeventhCpcDate());

        if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
            System.out.println("[getNewAuditCage] Branch: PRE-6CPC");
            if (isOldBasicPayAvailable(newAuditCage.getDo2()) == true) {
                System.out.println("[getNewAuditCage] probableBasic from isOldBasicPayAvailable=" + probableBasic);
                if (probableBasic != 0) {
                    newAuditCage.setBasicPay(probableBasic);
                    newAuditCage.setOldBasicPay(probableBasic);
                    System.out.println("[getNewAuditCage] Set basicPay=oldBasicPay=" + probableBasic);
                    if (!newAuditCage.getFromDate().before(DateManager.getDPDate())) {
                        int dp = (int) Math.round(newAuditCage.getBasicPay() * 0.50);
                        newAuditCage.setDp(dp);
                        System.out.println("[getNewAuditCage] Set DP=" + dp);
                    }
                }
            }
        } else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
            System.out.println("[getNewAuditCage] Branch: 6CPC (gradePay forced to 2000), calling updateOldBasicPay");
            newAuditCage.setGradePay(2000);
            updateOldBasicPay(newAuditCage);
            System.out.println("[getNewAuditCage] After updateOldBasicPay: oldBasicPay=" + newAuditCage.getOldBasicPay());
        } else {
            System.out.println("[getNewAuditCage] Branch: 7CPC, calling get7cpcPay");
            get7cpcPay(newAuditCage, newAuditCage.getDo2());
            System.out.println("[getNewAuditCage] After get7cpcPay: basicPay=" + newAuditCage.getBasicPay()
                    + " | payLevel=" + newAuditCage.getPayLevel());
            newAuditCage.setOldGradePay(2000);
        }

        return newAuditCage;
    }
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
	//	System.out.println("at 111117");
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		updateBdtInfo(do2);
		final String[] alwNotAdmissible = {"APTPAY", "GSPAY"};
		List<String> alwToStop = Arrays.asList(alwNotAdmissible);
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
	//	System.out.println("at 111118");
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}
	
//************************************************************************
 
	public boolean isMusterAvailable(Do2 do2) {
	
		if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() ==  null)
				return false;
		
        return true;
	}
	
	public boolean isOldBasicPayAvailable(Do2 do2) {
		Date sepdate = null;
		boolean validity = false;
		if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() ==  null){
				sepdate = employee.getAttestationDate();
		} else if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() !=  null){
			sepdate = employee.getInitialMusteringDate();
		}  else if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() !=  null){
			if (employee.getAttestationDate().before(employee.getInitialMusteringDate())){
				sepdate = employee.getAttestationDate();
			} else if (!employee.getAttestationDate().before(employee.getInitialMusteringDate())){
				sepdate = employee.getInitialMusteringDate();
			}
		}
		try {
			if (sepdate != null) {
				if (sepdate.before(DateManager.getDate("1997-10-10"))){
					List<AuditCage>atcList = acRepo.findByFromDateEqual(employee.getId(), DateManager.getDate("1998-08-31"));

			/*				Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
						" atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND  " +
						" atc.recordStatus!='P' AND atc.fromDate=:frdt ORDER BY atc.fromDate DESC")
						.setParameter("employee",employee)
						.setParameter("frdt",DateManager.getDate("1998-08-31"))
						.getResultList();*/
				
				  if (atcList == null || atcList.size() == 0){
			         	validity = false;
				  } else {
					  Date nextIncrementDate = sepdate ;
					  while (nextIncrementDate.before(DateManager.getDate("1997-10-10"))){
						  nextIncrementDate = Utils.getNextIncrementDate(nextIncrementDate);
					  }
					  probableBasic = arriveProbablePay(do2, nextIncrementDate, atcList.get(0).getBasicPay());
					  validity = true;
				  }
				} else {
					probableBasic = arriveProbablePay(do2, sepdate, 0);
					validity = true;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return validity;
	}
	
	/*public int getPreviousRankGradePay(Do2 do2) {
		List<PayScale> payScaleList = Utils.getEntityManager().createQuery(
						"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
								+ " AND ps.startScale IS NOT null")
								.setParameter("rnk", (Rank) Utils.getForeignKey(new Rank(), "rankCode","20"))
								.setParameter("grp", Utils.getGroupCategory(do2).getSoldierGroup())
								.setParameter("fromDate", do2.getFromDate())
								.getResultList();
		
		return payScaleList.get(0).getGradePay();
	}*/

	/*public int getPreviousRankBasic(Do2 do2) {
		esdPromotList = Utils.getEntityManager().createQuery("SELECT esdpl FROM EmployeeServiceDetail esdpl WHERE " +
				" esdpl.employee=:employee AND (esdpl.do2.description='MUSTER' OR esdpl.do2.description='ATTSBM' " +
				" OR esdpl.do2.description='ATTSAM') AND esdpl.do2.status!='C' ORDER BY esdpl.fromDate DESC ")
				.setParameter("employee", employee).getResultList();
				
		List<AuditCage>atcList = Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
				" atc.employee=:employee AND atc.effective=true AND atc.cancelled=false AND  " +
				" atc.recordStatus!='P' AND atc.fromDate<=:frdt ORDER BY atc.fromDate DESC")
				.setParameter("employee",employee)
				.setParameter("frdt",esdPromotList.get(0).getFromDate())
				.getResultList();
		if (atcList.size() > 0 ){
			return arriveProbablePay(do2, esdPromotList.get(0).getFromDate(), atcList.get(0).getBasicPay());	
		}
		return 0; 
	}*/

    public int arriveProbablePay(Do2 do2, Date previousPromotDate, int previousBasic) {
        int probableBasic = 0;
        Date previousIncrDate = Utils.getNextIncrementDate(previousPromotDate);

        System.out.println("[arriveProbablePay] START | previousPromotDate=" + previousPromotDate
                + " | previousBasic=" + previousBasic
                + " | do2.fromDate=" + do2.getFromDate());

        Rank rank = rankRepo.findByRankCode("20");
        List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(
                rank.getId(), Utils.getGroupCategory(do2, "CUR").getSoldierGroup(), previousPromotDate);

        if (previousBasic == 0) {
            probableBasic = payScaleList.get(0).getStartScale();
            System.out.println("[arriveProbablePay] previousBasic was 0, using startScale=" + probableBasic);
        } else {
            probableBasic = previousBasic;
            System.out.println("[arriveProbablePay] Using previousBasic=" + probableBasic);
        }

        while (previousIncrDate.before(do2.getFromDate())) {
            int before = probableBasic;
            probableBasic += payScaleList.get(0).getIncr0();
            System.out.println("[arriveProbablePay] Increment on " + previousIncrDate
                    + " | " + before + " + " + payScaleList.get(0).getIncr0() + " = " + probableBasic);
            previousIncrDate = Utils.getNextIncrementDate(previousIncrDate);
        }

        employee.setIncrementDate(previousIncrDate);
        System.out.println("[arriveProbablePay] FINAL probableBasic=" + probableBasic
                + " | nextIncrementDate=" + previousIncrDate);
        return probableBasic;
    }

	public void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Rank sepoyRank=rankRepo.findByRankCode("20");
	//	Rank sepoyRank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "20");
		employee.setNaikDate(null);
		employee.setHavDate(null);
		employee.setSubstantiveRank(sepoyRank);
		employee.setSubstantiveRankDate(do2.getFromDate());
		employee.setActingRank(sepoyRank);
		employee.setActingRankDate(do2.getFromDate());
		employee.setAppointmentDate(null);
		employee.setAppointmentRate(null);
		employee.setCurrentGspayCode("0");
		employee.setCurrentGsPayDate(null);

		List<EmployeeServiceDetail> esdGspayList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(), "G");
				
		//		Utils.getEntityManager().createQuery("SELECT esd FROM EmployeeServiceDetail esd WHERE " +
		//									" esd.employee=:emp AND esd.status='G' ORDER BY esd.fromDate DESC ").setParameter("emp", employee)
		//									.getResultList();
		
		if (esdGspayList.size()>0) {
			esdGspayList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
		}
		
	}
	
	private void updateOldBasicPay(AuditCage auditCage){
		Do2 do2 = auditCage.getDo2();
		Date previousPromotDate = do2.getFromDate();
		rank=rankRepo.findByRankCode("20");
		
		if (employee==null) {
			employee=auditCage.getEmployee();
		}
		
		esdPromotList = esdRepo.findOldbasicPay(employee.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT esdpl FROM EmployeeServiceDetail esdpl WHERE " +
				" esdpl.employee=:employee AND (esdpl.do2.description='MUSTER' OR esdpl.do2.description='ATTSBM' " +
				" OR esdpl.do2.description='ATTSAM') AND esdpl.do2.status!='C' ORDER BY esdpl.fromDate DESC" )
				.setParameter("employee", employee).getResultList();*/
		
		if (esdPromotList!=null && esdPromotList.size()>0) {
			previousPromotDate = esdPromotList.get(0).getFromDate();
		}
		
		if (previousPromotDate.before(DateManager.getCutOffDate())) {
			previousPromotDate = DateManager.getCutOffDate();
		}
		
		List<AuditCage> atcList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(Utils.getCurrentEmployee(do2.getArmyNo()).getId(),
																								previousPromotDate);
				
	/*			Utils.getEntityManager().createQuery("SELECT atc FROM AuditCage atc WHERE " +
		" atc.employee=:emp AND atc.effective=true AND atc.cancelled=false AND atc.recordStatus!='P' AND atc.absentFlag=false " +
		" AND atc.fromDate<=:frdt ORDER BY atc.fromDate DESC")
		.setParameter("emp",Utils.getCurrentEmployee(do2.getArmyNo()))
		.setParameter("frdt", previousPromotDate)
		.getResultList();*/
		
		int probableBasic=0;
		
		if ( atcList != null && atcList.size() > 0 ) {
			if (atcList.get(0).getFromDate().before(DateManager.getSixthCpcDate())){
				probableBasic = atcList.get(0).getBasicPay();	
			} else {
				probableBasic = atcList.get(0).getOldBasicPay();
			}
		}
		Date previousIncrDate=Utils.getNextIncrementDate(previousPromotDate);
		
		List<PayScale> payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),
																					Utils.getGroupCategory(do2,"CUR").getSoldierGroup(), previousPromotDate);
				
				
	/*			Utils.getEntityManager().createQuery(
				"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rnk "
						+ " AND ps.soldierGroup.soldierGroup=:grp "
						+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
						+ " or (:fromDate >= ps.fromDate and ps.toDate is null )) "
						+ " AND ps.startScale IS NOT null")
		.setParameter("rnk", Utils.getForeignKey(new Rank(), "rankCode","20"))
		.setParameter("grp", Utils.getGroupCategory(do2,"CUR").getSoldierGroup())
		.setParameter("fromDate", previousPromotDate).getResultList();*/
	
		while (previousIncrDate.before(do2.getFromDate())) {
			probableBasic+=payScaleList.get(0).getIncr0();
			previousIncrDate=Utils.getNextIncrementDate(previousIncrDate);
		}
		auditCage.setOldBasicPay(probableBasic);
	}
	
	public void reverseUpdateEmployee(Do2 do2) {
		List<Do2> promotAcpDo2List = do2Repo.findByFromDateLessThanAndMultipleDescription(do2.getArmyNo(), do2.getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where " +
				"(d.description='PROMOT' OR d.description='MSTHAV' OR d.description='ACP' OR d.description='MACPS' ) " +
				" AND d.status='V' AND d.armyNo=:ano AND d.fromDate<:fromDate ORDER BY d.fromDate DESC ")
				.setParameter("ano", do2.getArmyNo()).setParameter("fromDate", do2.getFromDate()).getResultList();*/	
		Rank rank = new Rank();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (promotAcpDo2List.size()>0) {
			String rankString = "";
			if (promotAcpDo2List.get(0).getDescription().equals("MSTHAV")) {
				rank=rankRepo.findByRankCode("40");
		//		rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "40");
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(promotAcpDo2List.get(0).getFromDate());
				employee.setActingRank(rank);
				employee.setActingRankDate(promotAcpDo2List.get(0).getFromDate());
				employee.setHavDate(promotAcpDo2List.get(0).getFromDate());
				employee.setAppointmentDate(promotAcpDo2List.get(0).getFromDate());
				AppointmentRate ar=apptRateRepo.findByAppointmentCode("40");
				employee.setAppointmentRate(ar);
			//	employee.setAppointmentRate((AppointmentRate) Utils.getForeignKey(new AppointmentRate(), "appointmentCode", "40"));
			} else if (promotAcpDo2List.get(0).getDescription().equals("PROMOT")) {
				rankString = promotAcpDo2List.get(0).getColumnA().substring(2);
				if (rankString.equals("HAV")){
					rank=rankRepo.findByRankCode("40");
				//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "40");
				} else {
					rank=rankRepo.findByRankCode("30");
			//		rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "30");
				}				
				employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(promotAcpDo2List.get(0).getFromDate());
				employee.setActingRank(rank);
				employee.setActingRankDate(promotAcpDo2List.get(0).getFromDate());
				if (rankString.equals("HAV")) {
					employee.setHavDate(promotAcpDo2List.get(0).getFromDate());			
				} else {
					employee.setNaikDate(promotAcpDo2List.get(0).getFromDate());
				}
			} else if (((promotAcpDo2List.get(0).getDescription().equals("ACP")) &&  
			          (promotAcpDo2List.get(0).getColumnA().equals("NKSCALE")) ||
			          (promotAcpDo2List.get(0).getColumnA().equals("HAVSCALE"))) ||
			          ((promotAcpDo2List.get(0).getDescription().equals("MACPS")) &&  
					  (promotAcpDo2List.get(0).getColumnA().equals("NKGRADE")) ||
					  (promotAcpDo2List.get(0).getColumnA().equals("HAVGRADE")))) {
				      if (promotAcpDo2List.get(0).getColumnA().equals("NKSCALE") ||
				    	  promotAcpDo2List.get(0).getColumnA().equals("NKGRADE")){				    		  
				    		  rankString = "NK";
				    		  rank=rankRepo.findByRankCode("29");
				    		//  rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "29");
				      } else {
				    	      rankString = "HAV";
				    	      rank=rankRepo.findByRankCode("39");
				    	  //    rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "39");
				      }					
					employee.setActingRank(rank);
					employee.setActingRankDate(promotAcpDo2List.get(0).getFromDate());
					
					if (promotAcpDo2List.size()>1) {
						if (promotAcpDo2List.get(1).getDescription().equals("PROMOT")) {
							rankString = promotAcpDo2List.get(1).getColumnA().substring(2);
							if (rankString.equals("HAV")){
								rank=rankRepo.findByRankCode("40");
							//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "40");
							} else {
								rank=rankRepo.findByRankCode("30");
							//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode", "30");
							}
							employee.setSubstantiveRank(rank);
							employee.setSubstantiveRankDate(promotAcpDo2List.get(1).getFromDate());
							if (rankString.equals("HAV")) {
								employee.setHavDate(promotAcpDo2List.get(1).getFromDate());			
							} else {
								employee.setNaikDate(promotAcpDo2List.get(1).getFromDate());
							}
						} else {
							if (promotAcpDo2List.get(1).getDescription().equals("MUSTER") 
									|| promotAcpDo2List.get(1).getDescription().equals("ATTSBM") || (promotAcpDo2List.get(1).getDescription().equals("ATTSAM"))) {
								rank=rankRepo.findByRankCode("20");
								employee.setSubstantiveRank(rank);
							//	employee.setSubstantiveRank((Rank) Utils.getForeignKey(new Rank(), "rankCode", "20"));
								employee.setSubstantiveRankDate(promotAcpDo2List.get(1).getFromDate());
							}
						}
					}
				}	
		}
		
		List<EmployeeServiceDetail> esdGspayList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(),"G");
				
		//		Utils.getEntityManager().createQuery("SELECT esd FROM EmployeeServiceDetail esd WHERE " +
				//							" esd.employee=:emp AND esd.status='G' ORDER BY esd.fromDate DESC ").setParameter("emp", employee)
				//							.getResultList();
		
		if (esdGspayList.size()>0) {
			esdGspayList.get(0).setToDate(null);
			employee.setCurrentGsPayDate(esdGspayList.get(0).getFromDate());
			employee.setCurrentGspayCode(String.valueOf(esdGspayList.size()).charAt(0)+"");
		}
	}
	
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}


    public void get7cpcPay(AuditCage nAuditCage,Do2 do2) {
        Date sepdate = null;
        String previousRankCode = "";
        boolean adhocIncrement01012006Exists = false;
        int probable6pcBasic = 0;
        int probable6pcXpay  = 0;
        int probable5pcBasic = 0;
        int probable7pcBasic = 0;
        int probablegradepay = 0;

        if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() ==  null){
            sepdate = employee.getAttestationDate();
        } else if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() !=  null){
            sepdate = employee.getInitialMusteringDate();
        }  else if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() !=  null){
            if (employee.getAttestationDate().before(employee.getInitialMusteringDate())){
                sepdate = employee.getAttestationDate();
            } else if (!employee.getAttestationDate().before(employee.getInitialMusteringDate())){
                sepdate = employee.getInitialMusteringDate();
            }
        }

        Rank previousRank = rankRepo.findByRankCode("20");
        previousRankCode="20";

        String previousRankPayLevel = Do2Manager.getPayLevelPromot(previousRank.getRankCode());
        Date previousIncrDate=Utils.getNextIncrementDate(sepdate);

        try {
            if (previousIncrDate.before(DateManager.getDate("1997-10-10"))){
                previousIncrDate = (DateManager.getDate("1997-10-10"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ===== ENTRY PRINT =====
        System.out.println("[get7cpcPay] ENTRY | sepdate=" + sepdate
                + " | do2.fromDate=" + do2.getFromDate()
                + " | payLevel=" + previousRankPayLevel
                + " | firstIncrDate=" + previousIncrDate);

        List<PayScale> payScaleList;

        if (previousIncrDate.before(DateManager.getSixthCpcDate())) {
            List<Do2> do2List=null;
            try {
                do2List = do2Repo.findByDescColCFromDateEqual(employee.getId(), "V", "INCR", "ADHOC", DateManager.getDate("2006-01-01"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (do2List!=null && do2List.size()>0) {
                adhocIncrement01012006Exists = true;
            }
        }

        // ===== ADHOC PRINT =====
        System.out.println("[get7cpcPay] adhocIncrement01012006Exists=" + adhocIncrement01012006Exists
                + " | startingIncrDate=" + previousIncrDate);

        List<Do2> incrDo2List=null;
        while (previousIncrDate.before(do2.getFromDate())) {

            List<AuditCage>atcList = acRepo.findByFromDateLessThanOrderByFromDateDesc(employee.getId(), previousIncrDate);

            if (atcList.size() > 0) {
                if (previousIncrDate.before(DateManager.getSixthCpcDate())) {
                    probable5pcBasic = atcList.get(0).getBasicPay();
                } else if (previousIncrDate.before(DateManager.getSeventhCpcDate())) {
                    probable6pcBasic  = atcList.get(0).getBasicPay();
                    probable6pcXpay   = atcList.get(0).getXGroupPay();
                    probablegradepay  = atcList.get(0).getGradePay();
                }
            } else {
                atcList = acRepo.findAllEffectiveAcOrderByFromDateToDateAsc(employee.getId());
                if (previousIncrDate.before(DateManager.getSixthCpcDate())) {
                    probable5pcBasic = atcList.get(0).getBasicPay();
                } else if (previousIncrDate.before(DateManager.getSeventhCpcDate())) {
                    probable6pcBasic  = atcList.get(0).getBasicPay();
                    probable6pcXpay   = atcList.get(0).getXGroupPay();
                    probablegradepay  = atcList.get(0).getGradePay();
                }
            }

            if(previousIncrDate.before(DateManager.getSixthCpcDate()) ||
                    previousIncrDate.equals(DateManager.getSixthCpcDate())) {

                // ===== 5CPC BEFORE INCREMENT PRINT =====
                System.out.println("[get7cpcPay] 5CPC | BEFORE incr | probable5pcBasic=" + probable5pcBasic
                        + " | incrDate=" + previousIncrDate);

                Rank rank=rankRepo.findByRankCode(previousRankCode);

                payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),
                        Utils.getGroupCategory(do2,"CUR").getSoldierGroup(), previousIncrDate);

                probable5pcBasic += payScaleList.get(0).getIncr0();
                previousIncrDate=Utils.getNextIncrementDate(previousIncrDate);

                // ===== 5CPC AFTER INCREMENT PRINT =====
                System.out.println("[get7cpcPay] 5CPC | AFTER incr (+=" + payScaleList.get(0).getIncr0()
                        + ") | probable5pcBasic=" + probable5pcBasic
                        + " | nextIncrDate=" + previousIncrDate);

                if (previousIncrDate.after(DateManager.getSixthCpcDate())) {
                    if (adhocIncrement01012006Exists) {
                        probable5pcBasic += payScaleList.get(0).getIncr0();
                        // ===== ADHOC INCREMENT APPLIED PRINT =====
                        System.out.println("[get7cpcPay] 5CPC | ADHOC incr applied (+=" + payScaleList.get(0).getIncr0()
                                + ") | probable5pcBasic=" + probable5pcBasic);
                    }
                }

            } else if (previousIncrDate.before(DateManager.getSeventhCpcDate())) {
                String soldierGroup = "";
                GroupCategory groupCategory = Utils.getGroupCategory(do2,"PREV");
                if (groupCategory != null)
                    soldierGroup = groupCategory.getSoldierGroup();
                Rank rank=rankRepo.findByRankCode(previousRankCode);
                List<ReadyReckoner> reckonerList = rrRepo.findByRankGroupAndBasic(rank.getId(),soldierGroup,probable5pcBasic);

                if (reckonerList.size() > 0) {
                    probable6pcBasic = reckonerList.get(0).getNewBasic();
                }

                // ===== 5CPC→6CPC CONVERSION PRINT =====
                System.out.println("[get7cpcPay] 5CPC→6CPC ReadyReckoner | probable5pcBasic(input)=" + probable5pcBasic
                        + " | probable6pcBasic(output)=" + probable6pcBasic
                        + " | soldierGroup=" + soldierGroup);

                if (!previousIncrDate.before(DateManager.getSixthCpcDate()) && employee.getEnrolmentDate()!=null
                        && employee.getEnrolmentDate().before(DateManager.getSixthCpcDate())
                        && probablegradepay==2000) {
                    payScaleList=null;
                    payScaleList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),
                            Utils.getGroupCategory(do2,"CUR").getSoldierGroup(), previousIncrDate);
                    if (payScaleList!=null && payScaleList.size()>0) {
                        if (payScaleList.get(0).getGradePay()==2000 && probable6pcBasic<payScaleList.get(0).getNewRecruitPay()) {
                            probable6pcBasic=payScaleList.get(0).getNewRecruitPay();
                        }
                    }
                }

                // ===== 6CPC BASE AFTER NEWRECRUITPAY CHECK PRINT =====
                System.out.println("[get7cpcPay] 6CPC base after newRecruitPay check | probable6pcBasic=" + probable6pcBasic
                        + " | gradePay=" + probablegradepay
                        + " | Xpay=" + probable6pcXpay);

                while (previousIncrDate.before(DateManager.getSeventhCpcDate())) {

                    String incrYear = getYear(previousIncrDate);
                    incrDo2List=null;

                    incrDo2List = do2Repo.findByDescFromDateYear(employee.getId(), "V", "INCR", Integer.parseInt(incrYear));

                    // ===== 6CPC YEAR LOOP PRINT =====
                    System.out.println("[get7cpcPay] 6CPC year loop | incrDate=" + previousIncrDate
                            + " | incrYear=" + incrYear
                            + " | incrDo2Found=" + (incrDo2List != null && incrDo2List.size() > 0)
                            + " | probable6pcBasic(before)=" + probable6pcBasic);

                    if (incrDo2List!=null && incrDo2List.size()>0)  {
                        int before6pc = probable6pcBasic;
                        probable6pcBasic = (probable6pcBasic)+Utils.getIncrmentRoundedToTen(probable6pcBasic
                                + (probablegradepay)
                                + (probable6pcXpay),previousIncrDate);

                        // ===== 6CPC AFTER INCREMENT PRINT =====
                        System.out.println("[get7cpcPay] 6CPC | AFTER incr | " + before6pc
                                + " + roundedIncr(basic=" + before6pc
                                + "+gp=" + probablegradepay
                                + "+xpay=" + probable6pcXpay + ")"
                                + " = probable6pcBasic=" + probable6pcBasic);
                    }

                    previousIncrDate=Utils.getNextIncrementDate(previousIncrDate);
                }

            } else {

                // ===== FIX: sepdate is post-6CPC so probable6pcBasic was never set via 5CPC/6CPC blocks.
                // Fetch actual 6CPC values from the latest audit cage record before 7CPC date =====
                if (probable6pcBasic == 0) {
                    List<AuditCage> prevAtcList = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(
                            employee.getId(), DateManager.getSeventhCpcDate());
                    if (prevAtcList != null && prevAtcList.size() > 0) {
                        probable6pcBasic = prevAtcList.get(0).getOldBasicPay();
                        probablegradepay = prevAtcList.get(0).getOldGradePay();
                        probable6pcXpay  = prevAtcList.get(0).getXGroupPay();
                        System.out.println("[get7cpcPay] FIX: sepdate is post-6CPC, fetched 6CPC values from DB"
                                + " | probable6pcBasic=" + probable6pcBasic
                                + " | gradePay=" + probablegradepay
                                + " | Xpay=" + probable6pcXpay);
                    } else {
                        System.out.println("[get7cpcPay] WARNING: probable6pcBasic still 0, no audit cage found before 7CPC date");
                    }
                }

                probable7pcBasic = (int)Math.round((probable6pcBasic+2000)*2.57);

                // ===== 6CPC→7CPC CONVERSION PRINT =====
                System.out.println("[get7cpcPay] 6CPC→7CPC | formula: (" + probable6pcBasic
                        + "+2000)*2.57 = " + (int)Math.round((probable6pcBasic+2000)*2.57)
                        + " | payLevel=" + previousRankPayLevel);

                List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(previousRankPayLevel,
                        probable7pcBasic);
                if (paymatrixList.size() > 0) {
                    probable7pcBasic = paymatrixList.get(0).getBasicPay();
                }

                // ===== 7CPC MATRIX ROUNDED PRINT =====
                System.out.println("[get7cpcPay] 7CPC | after matrix rounding | probable7pcBasic=" + probable7pcBasic);

                List<Object[]> promotMacpsDo2List=null; boolean finalIteration=false;
               // while (previousIncrDate.before(do2.getFromDate())) {
                while (!previousIncrDate.after(do2.getFromDate())) {
                    String incrYear = getYear(previousIncrDate);
                    String do2FromDateYear="";
                    incrDo2List=null; promotMacpsDo2List=null;
                    incrDo2List = do2Repo.findByDescFromDateYear(employee.getId(), "V", "INCR", Integer.parseInt(incrYear));
                    if (incrDo2List!=null && incrDo2List.size() > 0  )  {
                        do2FromDateYear = getYear(incrDo2List.get(0).getFromDate());
                    } else {
                        Date firstJanuaryOfIncrYear=null, firstJulyOfIncrYear=null;
                        try {
                            firstJanuaryOfIncrYear = DateManager.getDate(Integer.parseInt(incrYear), 1, 1);
                            firstJulyOfIncrYear = DateManager.getDate(Integer.parseInt(incrYear), 7, 1);
                        }  catch (Exception e) {
                            Utils.handleException(e);
                        }
                        System.out.println("1111111111111       "+firstJanuaryOfIncrYear+"  "+firstJulyOfIncrYear);
                        if (Integer.parseInt(getMonth(firstJanuaryOfIncrYear))==0) {
                            promotMacpsDo2List=do2Repo.getMacpsPromotDos2BetweenTwoIncrDates(do2.getEmployee().getId(), firstJanuaryOfIncrYear, DateManager.getPreviousDate(firstJulyOfIncrYear));
                            System.out.println("2222222");
                        }
                        try {
                            firstJulyOfIncrYear = DateManager.getDate(Integer.parseInt(incrYear)-1, 7, 1);
                        }  catch (Exception e) {
                            Utils.handleException(e);
                        }

                        System.out.println("1111111111111333333333333333       "+firstJanuaryOfIncrYear+"  "+firstJulyOfIncrYear);

                        if (Integer.parseInt(getMonth(firstJulyOfIncrYear))==7) {
                            promotMacpsDo2List=do2Repo.getMacpsPromotDos2BetweenTwoIncrDates(do2.getEmployee().getId(), firstJulyOfIncrYear, DateManager.getPreviousDate(firstJanuaryOfIncrYear));
                            System.out.println("444444444444444       "+firstJanuaryOfIncrYear+"  "+firstJulyOfIncrYear);
                        }
                        if (promotMacpsDo2List!=null && promotMacpsDo2List.size()>0) {
                            do2FromDateYear=incrYear;
                        }
                        System.out.println("555555555555 do2fromdtyr "+do2FromDateYear+" incr yr "+incrYear+" promotmacp size "+promotMacpsDo2List);
                    }
                    paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(previousRankPayLevel, probable7pcBasic);
                    int before7pc = probable7pcBasic;
                    if (paymatrixList.size() > 0) {
                        if (((incrDo2List!=null && incrDo2List.size()>0) || (promotMacpsDo2List!=null && promotMacpsDo2List.size()>0))
                                && incrYear.equals(do2FromDateYear)) {
                            probable7pcBasic = paymatrixList.get(0).getBasicPay();
                        }
                    }

                    // ===== 7CPC YEAR LOOP PRINT =====
                    System.out.println("[get7cpcPay] 7CPC year loop | incrDate=" + previousIncrDate
                            + " | incrYear=" + incrYear
                            + " | do2FromDateYear=" + do2FromDateYear
                            + " | incrGranted=" + (((incrDo2List!=null&&incrDo2List.size()>0)||(promotMacpsDo2List!=null&&promotMacpsDo2List.size()>0)) && incrYear.equals(do2FromDateYear))
                            + " | before=" + before7pc
                            + " | after=" + probable7pcBasic);

                    previousIncrDate=DateManager.getNextIncrementDate(previousIncrDate);
                    if (previousIncrDate.after(do2.getFromDate()) && !finalIteration) {
                    	previousIncrDate=do2.getFromDate();
                    	finalIteration=true;
                    }
                }
                previousIncrDate=DateManager.getNextIncrementDate(previousIncrDate);
                nAuditCage.setBasicPay(probable7pcBasic);
                nAuditCage.setPayLevel(previousRankPayLevel);
                employee.setIncrementDate(previousIncrDate);

                // ===== 7CPC FINAL (EARLY RETURN) PRINT =====
                System.out.println("[get7cpcPay] FINAL(early-return) | probable7pcBasic=" + probable7pcBasic
                        + " | nextIncrDate=" + previousIncrDate
                        + " | payLevel=" + previousRankPayLevel);
                return;
            }

        }

        probable7pcBasic = (int)Math.round((probable6pcBasic+2000)*2.57);

        // ===== 7CPC FALLTHROUGH PATH PRINT =====
        System.out.println("[get7cpcPay] 7CPC fallthrough path | (probable6pcBasic=" + probable6pcBasic
                + "+2000)*2.57 = " + probable7pcBasic
                + " | payLevel=" + previousRankPayLevel);

        List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(previousRankPayLevel, probable7pcBasic);

        List<Do2> incrDo2List2 = do2Repo.findByDescFromDateEqual(employee.getId(),"V", "INCR", previousIncrDate);

        if (incrDo2List2.size()>0)  {
            if (paymatrixList.size() > 0) {
                probable7pcBasic = paymatrixList.get(0).getBasicPay();
            }
        }

        previousIncrDate=DateManager.getNextIncrementDate(previousIncrDate);
        nAuditCage.setBasicPay(probable7pcBasic);
        nAuditCage.setPayLevel(previousRankPayLevel);
        employee.setIncrementDate(previousIncrDate);

        // ===== 7CPC FINAL (FALLTHROUGH) PRINT =====
        System.out.println("[get7cpcPay] FINAL(fallthrough) | incrDo2List2Found=" + (incrDo2List2.size()>0)
                + " | probable7pcBasic=" + probable7pcBasic
                + " | nextIncrDate=" + previousIncrDate
                + " | payLevel=" + previousRankPayLevel);
    }
	


static public String getYear(Date date){
	/*String strdate=null;
	GregorianCalendar gc=new GregorianCalendar();
	gc.setTime(date);
	String mm = null,dd=null;
	
	String mth=String.valueOf(gc.get(Calendar.MONTH)+1);
	if(mth.length()<2)
		mm="0".concat(mth);
	else
		mm=mth;
	
	String day=String.valueOf(gc.get(Calendar.DATE));
	
	if(day.length()<2)
		dd="0".concat(day);
	else
		dd=day;
	String yyyy=String.valueOf(gc.get(Calendar.YEAR));
	
	strdate=dd+mm+yyyy;
	
	
	return yyyy;*/
	
	GregorianCalendar gc=new GregorianCalendar();
	gc.setTime(date);
	return String.valueOf(gc.get(Calendar.YEAR));
}
static public String getMonth(Date date){	 
	GregorianCalendar gc=new GregorianCalendar();
	gc.setTime(date);
	System.out.println("..............gc month "+date+"  "+gc.get(Calendar.MONTH));
	return String.valueOf(gc.get(Calendar.MONTH));
}
}

