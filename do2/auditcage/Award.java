package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 
/**
 * 
 * @author IT&SDC, Secunderabad
 * 
 */

@RestController
public class Award extends AuditCageBase implements 
		Do2AuditCageProcessor, java.io.Serializable {

	 
	String message;
	Employee employee;
	
	static public String INVALID_RANK_SERVICE_GRTY = "VL:Invalid Rank/Service/GRTY in Col.7B for LGSCM.\n ";
	static public String INVALID_RANK_MSM = "VL:Invalid Rank for MSM.\n ";
	static public String INVALID_URSCM_RSCM = "AU:Invalid PAO/Rank/Service/Due for retirement/More than 5 red ink entries.\n ";
	static public String LOWER_RATE = "AU:Lower Rate than existing Award Rate.\n ";
	static public String MUTUALLY_EXCLUSIVE = "AU:Mutually exclusive with - URSCM/RSCM/CONVCT.\n ";
	static public String NO_GALLANTRY = "VL:No GALLANTRY in Col.7C.\n ";
	static public String ZERO_RATE = "AU:Rate should not be zero.\n ";
	static public String NOT_SERVED_SFF = "AU:Individual Is Not Serving / Served SFF.\n";
	static public String BEFORE_ASSPP_START_DATE = "VL:Publication Before Award Start Date - 27.08.2019.\n";
	static public String RECORD_OFFICE_PUBLICATION_REQUIRED = "VL:Record Office Publication Is Required. \n";
	
	private StringBuffer buffer = new StringBuffer();
	private double age = 0.0, serviceYears = 0.0, retentionAge = 0.0,
			awardRate = 0.0, existingAwardrate = 0.0;
	private Rank rank;
	public String paoNo;
	public Date enrollDate; 

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (!isHigherRate(do2)){
			validity = false;
			buffer.append(LOWER_RATE);
		}
		if (!isValidUrscm(do2)) {
			validity = false;
			buffer.append(INVALID_URSCM_RSCM);
		}
		if (!isLsgcmAdmissible(do2)) {
			validity = false;
			buffer.append(INVALID_RANK_SERVICE_GRTY);
		}
		if (!isMsmAdmissible(do2)) {
			validity = false;
			buffer.append(INVALID_RANK_MSM);
		}		
		if (isMutex(do2)){
			validity = false;
			buffer.append(MUTUALLY_EXCLUSIVE);
		}
		
		if (do2.getColumnA()!=null && do2.getColumnA().equals("ASSPP")) {
			if (!isIndividualServedSFF(do2, employee)) {
				validity = false;	
				buffer.append(NOT_SERVED_SFF);
			}
		 
			if (do2.getFromDate().before(DateManager.getAssppAwardEffectiveDate())) {
				validity = false;	
				buffer.append(BEFORE_ASSPP_START_DATE);
			}
			
			if (!do2.getDo2Type().equals("1")) {
				validity = false;	
				buffer.append(RECORD_OFFICE_PUBLICATION_REQUIRED);
			}		
		}
		
		if (validity == false) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getColumnA().equals("AC") ||  do2.getColumnA().equals("KC") || do2.getColumnA().equals("MVC") ||
			    do2.getColumnA().equals("PVC") || do2.getColumnA().equals("SC") || 	do2.getColumnA().equals("VRC") || 
				do2.getColumnA().equals("SM")) {
				checkAlwrevDo2Exist(do2);
			}
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		getAward(do2, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}

	
	public boolean isHigherRate(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<String> colAList=Arrays.asList(new String[] {"AC", "KC", "MVC", "PVC", "SC", "VRC", "ASSPP", "SM" });
		List<Do2> do2List = do2Repo.findByColAListFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(),Do2Manager.VALID,do2.getFromDate(), colAList);
				
			 
		if (do2List.size() > 0) {
			List<Allowance> allowanceRateList = new ArrayList<Allowance>();
			allowanceRateList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2List.get(0).getColumnA(), 
			do2.getFromDate(), do2.getFromDate());
			existingAwardrate = allowanceRateList.get(0).getRateAmount();
	
			List<Allowance> allowance = new ArrayList<Allowance>();
			allowance = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(),
				do2.getFromDate(), do2.getFromDate());
			awardRate = allowanceRateList.get(0).getRateAmount();
			if ((existingAwardrate <= awardRate) || (do2.getColumnA().equals("LSGCM") || do2.getColumnA().equals("MSM")
					|| do2.getColumnA().equals("URSCM") || do2.getColumnA().equals("RSCM") || do2.getColumnA().equals("ASSPP"))){
				return true;
			}
			return false;
		}
		return true;
	}
	
	public void getAward(Do2 do2, AuditCage baseAuditCage) {
		List<Allowance> allowanceRateList = new ArrayList<Allowance>();
		allowanceRateList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(),
				do2.getFromDate(), do2.getFromDate());
		awardRate = allowanceRateList.get(0).getRateAmount();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
			 
		 for(AuditCageAllowance aca : acaList){
			  if (Do2Manager.AWARD_LIST.contains(aca.getPayCode().getPayCode())){
				  existingAwardrate = aca.getAmount();
			  }
		 }
		 
		List<String> colAList=Arrays.asList(new String[] {"AC", "KC", "MVC", "PVC", "SC", "VRC", "SM", "ASSPP" });
		List<Do2> do2List =do2Repo.findByColAListFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(),Do2Manager.VALID,do2.getFromDate(), colAList);
	
	   	if (do2List.size() > 0) {
					
			    Do2 d = do2List.get(0);
			    System.out.println("............d desc " +d.getDescription() +"  cola " +d.getColumnA() +"  existingaward rate  " +existingAwardrate+"   award rate "+awardRate);
				if (d.getColumnA().equals(do2.getColumnA()) && d.getColumnB() == null && do2.getColumnB() == null){
					for (Allowance allow1 : allowanceRateList){
						AuditCageHelper.removeAllowances(Arrays.asList(new String[] { d.getColumnA() }), newAuditCage);
						AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow1.getRateAmount());
					}
				}
				if (d.getColumnA().equals(do2.getColumnA())){
					if (d.getColumnB() == null && (do2.getColumnB() != null && do2.getColumnB().equals("BAR"))){
						   for (Allowance allow1 : allowanceRateList){
							    AuditCageHelper.removeAllowances(Arrays.asList(new String[] { d.getColumnA() }), newAuditCage);
							    AuditCageHelper.updateAuditCageAllowance(newAuditCage, (do2.getColumnB().trim()+do2.getColumnA().trim()), existingAwardrate + allow1.getRateAmount());
						   }
					   }
				   if ((d.getColumnB() != null && d.getColumnB().equals("BAR")) &&	(do2.getColumnB() != null && do2.getColumnB().equals("BAR"))){
					   for (Allowance allow1 : allowanceRateList){
						    AuditCageHelper.removeAllowances(Arrays.asList(new String[] { do2.getColumnB().trim()+do2.getColumnA().trim() }), newAuditCage);
						    AuditCageHelper.updateAuditCageAllowance(newAuditCage, (do2.getColumnB().trim()+do2.getColumnA().trim()), existingAwardrate + allow1.getRateAmount());
					   }
				   }
				} else {
					/*if (d.getColumnB() == null && do2.getColumnB() == null) {
						if (existingAwardrate < awardRate){
							for (Allowance allow1 : allowanceRateList){
								AuditCageHelper.removeAllowances(Arrays.asList(new String[] { d.getColumnA() }), newAuditCage);
								AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow1.getRateAmount());
							}
						}
					}*/
					if (!do2.getColumnA().equals("ASSPP") && (d.getColumnB() == null && do2.getColumnB() == null)) {
						
						if (existingAwardrate < awardRate) {
							for (Allowance allow1 : allowanceRateList) {
								System.out.println(".................aaaaaaaaaaaaaaaa1111111111111111111.........");
								if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
								//	Old Award Removal commented based on MoD letter No.7(62)/2014-D(AG) dated 04.12.2017 - Samanvyay SOS2025121800022
								//	AuditCageHelper.removeAllowances(Arrays.asList(new String[] { d.getColumnA() }), newAuditCage);
									AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow1.getRateAmount());					
								} else {
									 	AuditCageHelper.removeAllowances(Arrays.asList(new String[] { d.getColumnA() }), newAuditCage);
										AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow1.getRateAmount());	
								}
							}
						}
					} else if (do2.getColumnA().equals("ASSPP") && (d.getColumnB() == null && do2.getColumnB() == null)) { 
						for (Allowance allow1 : allowanceRateList){
						 	AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow1.getRateAmount());
						}
					}
					
					 if   (do2.getColumnA().equals("SM")) { 
						  	for (Allowance allow : allowanceRateList){
						  		System.out.println ( " allwoamount " +allow.getRateAmount());
						  		AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow.getRateAmount());
				     		//  Utils.addArrears(do2, allow.getRateAmount(), "SM", do2.getFromDate(), do2.getFromDate());
					     	}
					  }
				}
		} 
			
	 	
		if (do2List.size() == 0) {
			if (newDo2.getId()==do2.getId()){
				  if   (do2.getColumnA().equals("LSGCM")) {
				        for (Allowance allow : allowanceRateList){
				     	  Utils.addArrears(do2, allow.getRateAmount(), "LSGCM", do2.getFromDate(), do2.getFromDate());
					    }
				  }
				  if   (do2.getColumnA().equals("MSM")) {
				        for (Allowance allow : allowanceRateList){
				     	  Utils.addArrears(do2, allow.getRateAmount(), "MSM", do2.getFromDate(), do2.getFromDate());
					    }
				  }
				  if   (do2.getColumnA().equals("URSCM")) {
					   	for (Allowance allow : allowanceRateList){
			     		  Utils.addArrears(do2, allow.getRateAmount(), "URSCM", do2.getFromDate(), do2.getFromDate());
				     	}
			      }
				  if   (do2.getColumnA().equals("RSCM")) { 
					  	for (Allowance allow : allowanceRateList){
			     		  Utils.addArrears(do2, allow.getRateAmount(), "RSCM", do2.getFromDate(), do2.getFromDate());
				     	}
				  }
				  
				
				  /*if (do2.getColumnA().equals("ASSPP")){
					  for (Allowance allow : allowanceRateList){
				  			Utils.addArrears(do2, allow.getRateAmount(), "ASSPP",do2.getFromDate(), do2.getFromDate()); 
				  		}
				  	}*/
				 
		    }
		    if  (do2.getColumnA().equals("LSGCM") || do2.getColumnA().equals("MSM") || do2.getColumnA().equals("URSCM") || do2.getColumnA().equals("RSCM") ) {
		        // do2.getColumnA().equals("RSCM") ||  do2.getColumnA().equals("ASSPP") ){ 
                   ;
		    } else if (do2.getColumnB() == null){
			   for (Allowance allow : allowanceRateList){
				   AuditCageHelper.removeAllowances(Arrays.asList(new String[] { do2.getColumnA() }), newAuditCage);
				   AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getColumnA(), allow.getRateAmount());
			   }
		    } else if (do2.getColumnB() != null && do2.getColumnB().equals("BAR")){
		       for (Allowance allow1 : allowanceRateList){
		    	    AuditCageHelper.removeAllowances(Arrays.asList(new String[] { do2.getColumnA() }), newAuditCage);
				    AuditCageHelper.updateAuditCageAllowance(newAuditCage, (do2.getColumnB().trim()+do2.getColumnA().trim()), existingAwardrate + allow1.getRateAmount());
			   }
		    }
		}
	}
	public void getValidRankAge(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		serviceYears = DateManager.getNoOfMonths(employee.getEnrolmentDate(),
					do2.getFromDate()) /12;
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		paoNo = Utils.getPao().getPaoCode();
		age = DateManager.getNoOfMonths(employee.getBirthDate(), do2
					.getFromDate()) / 12;
		retentionAge = Integer.valueOf(employee.getTermsMaxAgeRetention()) - 1;
		enrollDate = employee.getEnrolmentDate();
	}

	public boolean isValidUrscm(Do2 do2) {
		boolean recordValidity = false;
	//	getValidRankAge(do2);		
		/* service years >15 and age < retention Age conditions removed since the previous service details of dsc
		personnels are not captured in dolphin */	
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		paoNo = Utils.getPao().getPaoCode();
		if (!(do2.getColumnA().equals("URSCM") || do2.getColumnA().equals("RSCM"))){ 
			recordValidity=true;
			return recordValidity;
		} 
		if ((rank.getRankName().equals("SEP") || rank.getRankName().equals("NK") ||
			rank.getRankName().equals("HAV")) && (paoNo.equals(Do2Manager.PAO_DSC))) {
			recordValidity=true;
		}
		
		return recordValidity;
	}
	
	public boolean isLsgcmAdmissible(Do2 do2){
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (do2.getColumnA().equals("LSGCM") && do2.getColumnB() != null && do2.getColumnB().equals("GRTY")) {
			if (rank.getRankName().equals("SEP") || rank.getRankName().equals("NK"))
				return true;			
			else 
			   return false;
		}
		return true;
	}
	public boolean isMsmAdmissible(Do2 do2){;
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		if (do2.getColumnA().equals("MSM")) {
			if (rank.getRankName().equals("SEP") || rank.getRankName().equals("NK") ||
				rank.getRankName().equals("HAV"))
				return true;
			else
				return false;
		}
		return true;
	}
	
	public boolean isMutex(Do2 do2){
		getValidRankAge(do2);
		List<String> mutexNames = Arrays.asList(new String[] { "URSCM" ,"RSCM", "CONVCT" });
		if (do2.getColumnA().equals("URSCM") || do2.getColumnA().equals("RSCM")){
			List<Do2> do2List2 = Utils.getValidDo2s(do2.getArmyNo(), enrollDate, DateManager.getPreviousDate(do2.getFromDate()),do2);
			for (Do2 dolist : do2List2) {
				if (mutexNames.contains(dolist.getDescription()) || mutexNames.contains(dolist.getColumnA()))
					return true;
			}
		}
		return false;
	}
	public  void checkAlwrevDo2Exist(Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AllowanceRate> alwList = alwRateRepo.findByAllowanceNameAndSubAllowanceNameIgnoreCase("AWARD",do2.getColumnA());
			 
		if (alwList.size() > 0){
			for (int i = 0; i < alwList.size(); i++){
				if (i < (alwList.size() - 1)){
					if (alwList.get(i).getFromDate().equals(alwList.get(i + 1).getFromDate())){
						continue;
					}
				}			
				if (alwList.get(i).getFromDate().after(do2.getFromDate())){
					List<Do2> do2List = do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(), Do2Manager.VALID,Do2Manager.PENDING, "ALWREV",alwList.get(i).getFromDate());
					 
					if (do2List.size() == 0){
						createAlwrevDo2(do2, alwList.get(i).getFromDate());
					}
				}
			}
		}		
	}
	public  void createAlwrevDo2(Do2 do2, Date fromDate) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Do2 alwrevDo2 = new Do2();
		alwrevDo2.setEmployee(employee);
		alwrevDo2.setArmyNo(do2.getArmyNo());
		alwrevDo2.setUnitCode(do2.getUnitCode());
		alwrevDo2.setUnit(do2.getUnit());
		alwrevDo2.setDo2Group("IA");
		alwrevDo2.setCheckDigit(do2.getCheckDigit());
		alwrevDo2.setDo2Type(do2.getDo2Type());
		alwrevDo2.setDo2No(do2.getDo2No());
		alwrevDo2.setDo2ItemNo(do2.getDo2ItemNo());
		alwrevDo2.setDo2Date(DateManager.getCurrentDate());
		alwrevDo2.setDescription("ALWREV");
		alwrevDo2.setFromDate(fromDate);
		alwrevDo2.setBatchNo(do2.getBatchNo());
		alwrevDo2.setStatus(Do2Manager.PENDING);
		alwrevDo2.setReason(null);
		alwrevDo2.setQe(Utils.getCurrentQuarter());
		alwrevDo2.setCreatedAt(DateManager.getCurrentTimestamp());
		alwrevDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		alwrevDo2.setAuditCage(true);
		do2Repo.save(alwrevDo2);
	}
		
	public  boolean isIndividualServedSFF(Do2 do2, Employee employee) {
		
		Date validSFFServedDate = do2.getFromDate();
		if (do2.getFromDate().equals(DateManager.getAssppAwardEffectiveDate()) 
				&& do2.getToDate()!=null && do2.getToDate().before(DateManager.getAssppAwardEffectiveDate())) {
			validSFFServedDate = do2.getToDate();
		}		
		
		List<Do2> do2List;
		if (Utils.getPao().getPaoCode().equals("40"))  {
			do2List = do2Repo.findByDescStatusFromDateLessThan(do2.getEmployee().getId(),Do2Manager.VALID,"SSA", validSFFServedDate);
		} else {
			do2List = do2Repo.findByDescStatusFromDateLessThan(do2.getEmployee().getId(),Do2Manager.VALID,"SFASFF", validSFFServedDate);
		}
				
		if (do2List!=null && do2List.size()>0) {
			return true;
		}		
		
		return false;
	}
}