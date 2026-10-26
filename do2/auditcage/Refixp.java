package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

@RestController
public class Refixp extends AuditCageBase implements  Do2AuditCageProcessor, java.io.Serializable {

	public StringBuffer buffer = new StringBuffer();
	Employee emp;
	Rank rank;
	
	static public String INVALID_7A   = "AU:Incorrect Pay shown in Column 7A.\n";
	static public String NULL_7A      = "VL:REFIX Pay to be shown Column 7A.\n";
	static public String NULL_7B      = "VL:SixCPC Pay to be shown Column 7B.\n";
	static public String INVALID_7B   = "AU:Incorrect Six CPC Pay shown in Column 7B.\n";
	static public String INVALID_RATE = "VL:Pre-revised (5CPC) pay to be shown in Rate Column/Pl.check pay given in rate column.\n";
	static public String INVALID_7C   = "VL:Col-7C should be either MACPS/INCR/PROMOT.\n";
	static public String INVALID_FROM_DATE = "VL:From date should be before 7cpc notification date.\n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if (!do2.getFromDate().before(DateManager.getSixthCpcDate()) && do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			if (do2.getColumnA() == null) {
				validity = false;
				buffer.append(NULL_7A);
			} else if (!isValid7A(do2)) {				
				validity = false;
				buffer.append(INVALID_7A);
			}
		}		

		if (!do2.getFromDate().after(DateManager.getSixthCpcDate()) && !isValidRate(do2)) {
			validity = false;
			buffer.append(INVALID_RATE);
		}
		
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())){
			if (!get7pcPay(do2)){
				validity = false;
				buffer.append(INVALID_7A);
			}
		}
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())){
			if (do2.getColumnB() == null) {
				validity = false;
				buffer.append(NULL_7B);
			} else {
				if (Integer.parseInt(do2.getColumnB()) % 10 != 0) {
					validity = false;
					buffer.append(INVALID_7B);
				}
			}
		}
		// To facilitate APS_NAGPUR to feed optcpc after 1.10.2016 (cut_off_date) through Refixp  
		// col-7a -- revised pay after fixation in 7cpc scales.
		// col-7b -- sixcpc pay  
		// col-7c -- INCR/MACPS/PROMOT (option description)
		if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)){
			if (do2.getColumnC() != null && 
				!(do2.getColumnC().equals("MACPS") || do2.getColumnC().equals("INCR") || do2.getColumnC().equals("PROMOT"))){
				validity = false;
				buffer.append(INVALID_7C);
			}			
			if (do2.getColumnC() != null && 
			   (do2.getColumnC().equals("MACPS") || do2.getColumnC().equals("INCR") || do2.getColumnC().equals("PROMOT"))){
				// for other PAOs
				/*if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && 
					!do2.getFromDate().after(DateManager.getSeventhCpcNotificationDate())) {
					 ;
				} else {
					validity = false;
					buffer.append(INVALID_FROM_DATE);
				}*/		
				// For aps nagpur
				Date apsCutOffDate = null;
				try {
					apsCutOffDate = DateManager.getDate("2016-10-01");// cut off date for aps nagpur
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					
				if (!do2.getFromDate().before(apsCutOffDate) && 
					!do2.getFromDate().after(DateManager.getSeventhCpcNotificationDate())) {
					 ;
				} else {
					validity = false;
					buffer.append(INVALID_FROM_DATE);
				}
			}
			if (validity && (do2.getColumnC() != null && 
			   (do2.getColumnC().equals("MACPS") || do2.getColumnC().equals("INCR") || do2.getColumnC().equals("PROMOT"))) && !isFromDateMatchingWithDescInColumn7C(do2)) {
				do2.setStatus(Do2Manager.INVALID);
				validity=false;
				buffer.append("VL:Reason For Option - "+do2.getColumnC()+"-Does Not Exist On Option Date.\n");				
			}
		}		
		
		if(!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setQe(Utils.getCurrentQuarter());
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);

		return validity;
	}

	/*@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		emp=Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
	
		char soldierGroup = ' ';
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"CUR");
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}
		
		if (soldierGroup == 'Z' && !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = 'Y';
		}
	
//		rank=Utils.getRankForPayFixation(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate());
//		rank=Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),"PAYFIX");
		rank=Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),"PAYFIX");
		
		List<PayScale> payList = getPayScaleList(rank, soldierGroup, emp, newAuditCage.getDo2());
		
		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && soldierGroup=='X') {
			newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
		}
		
		if(!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			if (payList!=null && payList.size()>0) {
				newAuditCage.setGradePay(payList.get(0).getGradePay());	
			}
			newAuditCage.setDp(0);
		} else {
			newAuditCage.setBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setGradePay(0);
			if (!newAuditCage.getFromDate().before(DateManager.getDPDate()) 
					&& !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				newAuditCage.setDp((int)Math.round(newAuditCage.getBasicPay()*0.5));
			}
		}
		
		if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
				&& newAuditCage.getGradePay()!=null && newAuditCage.getGradePay()!=0) {
			if (payList!=null && payList.size()>0) {
				newAuditCage.setMsPay(payList.get(0).getMsp());
			}
			
		}
		

		Pao pao=null;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pao = Utils.getPao();
		}  
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("PMHAL", 
													newAuditCage.getFromDate(), newAuditCage.getFromDate(), emp.getGsoCode(), pao);
		if (allowanceList.size()>0) {
			newAuditCage.setPmha((int) allowanceList.get(0).getRateAmount());
 		}
		
		newAuditCage.setLra((int) Math.round(AuditCageHelper.getLraRate(newAuditCage)));

		return newAuditCage;
	}*/

	// above method commented and modified as stated due to sevencpc
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		emp=Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
	
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(newAuditCage.getDo2(),"CUR");
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}
		
		if (soldierGroup.equals("Z") && !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}

		rank=Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),"PAYFIX");
		
		List<PayScale> payList = getPayScaleList(rank, soldierGroup, emp, newAuditCage.getDo2());
		
		System.out.println("............ sol grp " +soldierGroup + " ps list size  " +payList.size());
		
		if (soldierGroup.equals("X")) {
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {				
				newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
		//		System.out.println("............. pay list x grop pay " +payList.get(0).getxGroupPay());
			} else if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				List<Do2> aictgpDo2List = do2Repo.findByDescColAFromDateLessThanEqual(newAuditCage.getEmployee().getId(), "V", "AICTGP", "GP-X", newAuditCage.getFromDate());
			//	System.out.println("...... aictgp list size  "+aictgpDo2List.size());
 				if (aictgpDo2List!=null && aictgpDo2List.size()>0) {
 				//	System.out.println("111111");
					newAuditCage.setXGroupPay(6200);
				} else {
				//	System.out.println("22222");
					newAuditCage.setXGroupPay(3600);
				}
			} 
		}
		
		
		
	/*	if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && soldierGroup.equals("X")) {
			
			newAuditCage.setXGroupPay(payList.get(0).getXGroupPay());
			System.out.println("............. pay list x grop pay " +payList.get(0).getxGroupPay());
		}
		*/
	/*	if(!newAuditCage.getFromDate().after(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setGradePay(0);
			if (!newAuditCage.getFromDate().before(DateManager.getDPDate()) 
					&& !newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				newAuditCage.setDp((int)Math.round(newAuditCage.getBasicPay()*0.5));
			}
		} else if(!newAuditCage.getFromDate().after(DateManager.getSeventhCpcDate())) {
			newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			if (payList!=null && payList.size()>0) {
				newAuditCage.setGradePay(payList.get(0).getGradePay());	
			}
			newAuditCage.setDp(0);
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
					&& newAuditCage.getGradePay()!=null && newAuditCage.getGradePay()!=0) {
				if (payList!=null && payList.size()>0) {
					newAuditCage.setMsPay(payList.get(0).getMsp());
				}
			}
		} else {
			newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setGradePay(0);
		}		
*/
		if(newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			newAuditCage.setBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			newAuditCage.setGradePay(0);
			if (!newAuditCage.getFromDate().before(DateManager.getDPDate()) 
					&& newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				newAuditCage.setDp((int)Math.round(newAuditCage.getBasicPay()*0.5));
			}
		} else if(newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));
			newAuditCage.setOldBasicPay(newAuditCage.getDo2().getRate().intValue());
			if (payList!=null && payList.size()>0) {
				newAuditCage.setGradePay(payList.get(0).getGradePay());	
			}
			newAuditCage.setDp(0);
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
					&& newAuditCage.getGradePay()!=null && newAuditCage.getGradePay()!=0) {
				if (payList!=null && payList.size()>0) {
					newAuditCage.setMsPay(payList.get(0).getMsp());
				}
			}
		} else {
			newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));
			newAuditCage.setOldBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnB()));
	
			Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), DateManager.getNextDate(newAuditCage.getDo2().getFromDate()),"PAYFIX");
			
			String payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
		
			newAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(payLevel));
			newAuditCage.setGradePay(0);
			newAuditCage.setPayLevel(payLevel);
			if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==2000) {
				newAuditCage.setMsPay(5200);
			}
		}	
		// For APS Nagpur only
		if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && (newAuditCage.getDo2().getColumnC() != null && 
			(newAuditCage.getDo2().getColumnC().equals("MACPS") || newAuditCage.getDo2().getColumnC().equals("INCR") || newAuditCage.getDo2().getColumnC().equals("PROMOT")))) {
			if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==2000) {
				newAuditCage.setMsPay(5200);
			} else {
				if (baseAuditCage.getMsPay()!=null && baseAuditCage.getMsPay()==6000) {
					newAuditCage.setMsPay(15500);
					if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && baseAuditCage.getOldGradePay()<5400
							&& baseAuditCage.getEmployee().getEnrolmentDate()!=null 
							&& !baseAuditCage.getEmployee().getEnrolmentDate().before(DateManager.getSeventhCpcDate())) {
						newAuditCage.setMsPay(5200);
					}
				}			
			}
		}
		
		Pao pao=null;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pao = Utils.getPao();
		}  
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("PMHAL", 
										newAuditCage.getFromDate(), newAuditCage.getFromDate(), emp.getGsoCode(), pao);
		if (allowanceList.size()>0) {
			newAuditCage.setPmha((int) allowanceList.get(0).getRateAmount());
 		}
		
		newAuditCage.setLra((int) Math.round(AuditCageHelper.getLraRate(newAuditCage)));

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
	
	protected boolean isValid7A(Do2 do2) {
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			if(do2.getColumnA() != null) {
				if(Integer.parseInt(do2.getColumnA()) % 10 == 0) {
					return true;
				}
			}
		}		
		return false;
	}
	
	protected boolean isValidRate(Do2 do2) {
		if(do2.getRate() != null && do2.getRate().intValue() > 0) {
			return validatePay(do2, do2.getRate().intValue());
		}
		return false;
	}

	protected boolean validatePay(Do2 do2, int amount) {
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		if (Utils.getPao().getPaoCode().equals("94")) {
			if (do2.getFromDate().equals(emp.getEnrolmentDate())){
				if (emp.getEnrolmentDate()!=null && (emp.getEnrolmentDate().equals(emp.getNbsubDate()) ||  emp.getEnrolmentDate().equals(emp.getSubDate())
						 || emp.getEnrolmentDate().equals(emp.getSubmajDate()))) {
					 rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getDo2().getFromDate(),"ALLCE");
				}
				if  (rank!=null && rank.getRankCode().equals("10")) {
					rank = rankRepo.findByRankCode("20");
				//	rank = (Rank) Utils.getForeignKey(new Rank(), "rankCode",	"20");
				}
			}
		}		
		GroupCategory gc = Utils.getGroupCategory(do2, "CUR");
		String solGroup = gc.getSoldierGroup();
		List<PayScale> psList = getOldPayScaleList(rank, solGroup, emp, do2);
 		if (psList.size() > 0) {
			for (int i=0 ; i < psList.size(); i++) {
				if (psList.get(i).getStartScale() != null  && psList.get(i).getScaleEnd0() != null) {
					if (amount >= psList.get(i).getStartScale() && amount <= psList.get(i).getScaleEnd0()) {
						return true;
					}
				}
			}
		}
		return false;
		/*System.out.println("rank " + rank.getRankCode() + " group " + solGroup);
		System.out.println(" start " + psList.get(0).getStartScale() + " End " + psList.get(0).getScaleEnd0());
		return (amount >= psList.get(0).getStartScale() && amount <= psList.get(0).getScaleEnd0());*/
	}
	
	protected List<PayScale> getOldPayScaleList(Rank rank, String sgp, Employee emp, Do2 do2) {
	
			
		List<PayScale> psList=null;
		try {
			psList = psRepo.findByRankAndSoldierGroupAndFromDate(rank.getId(), sgp, DateManager.getDate("1997-10-10"));
					
					
			/*		Utils.getEntityManager().createQuery(
										"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk AND "
										+ " ps.soldierGroup.soldierGroup=:grp AND ps.fromDate = :fromDate ")
										.setParameter("rnk", rank)
										.setParameter("grp",sgp)
										.setParameter("fromDate", DateManager.getDate("1997-10-10"))
										.getResultList();*/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return psList;
	}
	protected List<PayScale> getPayScaleList(Rank rank, String sgp, Employee emp, Do2 do2) {
		
		if (!do2.getFromDate().before(DateManager.getSixthCpcDate()) && sgp.equals("Z"))
			sgp="Y";
		
		List<PayScale> psList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),sgp,
																									do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery(
									"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk AND "
									+ " ps.soldierGroup.soldierGroup=:grp AND "
									+ " ((:fromDate BETWEEN ps.fromDate AND ps.toDate ) OR "
									+ "  (:fromDate >= ps.fromDate AND ps.toDate IS null )) AND "
									+ " ps.startScale IS NOT null")
									.setParameter("rnk", rank)
									.setParameter("grp",sgp)
									.setParameter("fromDate", do2.getFromDate()).getResultList();*/
	
		return psList;
	}
	
	
	public boolean get7pcPay(Do2 do2) {
		Rank rank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()),"PAYFIX");
		int newBasic = Integer.parseInt(do2.getColumnA().trim());
		
		String payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());		
	
		List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayOrderByBasicPayAsc(payLevel, newBasic);
				
		/*		Utils.getEntityManager().createQuery( 
			" select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND pm.basicPay=:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", payLevel)
				.setParameter("sevencpcpay", newBasic).getResultList();*/
		
		if (paymatrixList.size() > 0) {
			return true;
		}
		
		return false;
		
	}
	// Added Code for APS Nagpur  -- 13/07/2018
	
	 public boolean isFromDateMatchingWithDescInColumn7C(Do2 do2) {	
		List<Do2>do2List = null;
		
		// Checking whether column7C of Refixp Do2 with Description of original Do2		
	
		if (do2.getColumnC().trim().equals("MACPS")) {
			do2List = do2Repo.findByDescFromDateEqual(do2.getEmployee().getId(), "V", "MACPS", do2.getFromDate());
					
					
			/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
					"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
					"do2.description='MACPS' AND do2.status='V' ")
					.setParameter("employee", do2.getEmployee()) 
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}			
		} else if (do2.getColumnC().trim().equals("INCR")) {
			do2List = do2Repo.findByDescFromDateEqual(do2.getEmployee().getId(), "V", "INCR", do2.getFromDate());
					
		/*			
					Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
					"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
					"do2.description='INCR' AND do2.status='V' ")
					.setParameter("employee", do2.getEmployee()) 
					.setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}
		} else if (do2.getColumnC().trim().equals("PROMOT")) {	
			List<String> descList=Arrays.asList(new String[] {"MSTJCO","MSTHAV","PROMOT","HCOMMN","LATENR"});
			do2List = do2Repo.findByDescListFromDateEqual(do2.getEmployee().getId(),"V", descList,do2.getFromDate());
					
			/*		Utils.getEntityManager().createQuery("SELECT do2 from Do2 do2 where " +
					"do2.employee= :employee AND do2.fromDate = :fromDate AND " +
					"do2.description in ('MSTJCO', 'MSTHAV', 'PROMOT', 'HCOMMN', 'LATENR') AND do2.status='V' ")
					.setParameter("employee", do2.getEmployee()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				return false;
			}
		}
		
		return true;		 
	 }
}