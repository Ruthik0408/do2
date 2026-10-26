package itsdc.dolphin.do2;
 
import java.io.BufferedWriter;
import java.io.File; 
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException; 
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.transaction.Transactional;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.AppendixJRepository;
import itsdc.dolphin.repositories.AppointmentRateRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageAllowanceRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.BhutanSlabRepository;
import itsdc.dolphin.repositories.ClassPayRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeEarningRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.EmployeeServiceDetailRepository;
import itsdc.dolphin.repositories.FamilyDetailRepository;
import itsdc.dolphin.repositories.Form16Repository;
import itsdc.dolphin.repositories.FundSummaryRepository;
import itsdc.dolphin.repositories.GroupCategoryRepository;
import itsdc.dolphin.repositories.LpcDeputationRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.repositories.PayCodeRepository;
import itsdc.dolphin.repositories.PayMatrixRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.RankRepository;
import itsdc.dolphin.repositories.ReadyReckonerRepository;
import itsdc.dolphin.repositories.RhLocationRepository;
import itsdc.dolphin.repositories.StatusCodeRepository;
import itsdc.dolphin.repositories.TransferInOutRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils; 
import itsdc.dolphin.do2.Arrearcorr;

/**
 * Author : IT & SDC, Secunderabad
 */

@RestController
public class AtcorrGenerationForAllowanceRevision {	 
	  
	@Autowired
	Arrearcorr arrearcorr;
	
	JdbcTemplate jdbcTemplate;
	Environment env;
	EmployeeRepository empRepo;
	MiscRepository miscRepo;
	Do2Repository do2Repo;
	AllowanceRateRepository allowanceRateRepo;
	AuditCageRepository acRepo;
	AuditCageAllowanceRepository acaRepo;
	ArrearsRepository arrearsRepo;
	LpcRegisterRepository lpcRegisterRepo;
	
	public EmployeeEarningRepository getEmpearningRepo() {
		return empearningRepo;
	}

	public void setEmpearningRepo(EmployeeEarningRepository empearningRepo) {
		this.empearningRepo = empearningRepo;
	}

	public ClassPayRateRepository getClassPayRepo() {
		return classPayRepo;
	}

	public void setClassPayRepo(ClassPayRateRepository classPayRepo) {
		this.classPayRepo = classPayRepo;
	}

	public EmployeeServiceDetailRepository getEsdRepo() {
		return esdRepo;
	}

	public void setEsdRepo(EmployeeServiceDetailRepository esdRepo) {
		this.esdRepo = esdRepo;
	}

	public AppendixJRepository getAppxJRepo() {
		return appxJRepo;
	}

	public void setAppxJRepo(AppendixJRepository appxJRepo) {
		this.appxJRepo = appxJRepo;
	}

	public RhLocationRepository getRhlocationRepo() {
		return rhlocationRepo;
	}

	public void setRhlocationRepo(RhLocationRepository rhlocationRepo) {
		this.rhlocationRepo = rhlocationRepo;
	}

	public PayScaleRepository getPsRepo() {
		return psRepo;
	}

	public void setPsRepo(PayScaleRepository psRepo) {
		this.psRepo = psRepo;
	}

	public PayMatrixRepository getPmRepo() {
		return pmRepo;
	}

	public void setPmRepo(PayMatrixRepository pmRepo) {
		this.pmRepo = pmRepo;
	}

	public ReadyReckonerRepository getRrRepo() {
		return rrRepo;
	}

	public void setRrRepo(ReadyReckonerRepository rrRepo) {
		this.rrRepo = rrRepo;
	}

	public RankRepository getRankRepo() {
		return rankRepo;
	}

	public void setRankRepo(RankRepository rankRepo) {
		this.rankRepo = rankRepo;
	}

	public GroupCategoryRepository getGcRepo() {
		return gcRepo;
	}

	public void setGcRepo(GroupCategoryRepository gcRepo) {
		this.gcRepo = gcRepo;
	}

	public PayCodeRepository getPcRepo() {
		return pcRepo;
	}

	public void setPcRepo(PayCodeRepository pcRepo) {
		this.pcRepo = pcRepo;
	}

	public StatusCodeRepository getScRepo() {
		return scRepo;
	}

	public void setScRepo(StatusCodeRepository scRepo) {
		this.scRepo = scRepo;
	}

	public Form16Repository getF16Repo() {
		return f16Repo;
	}

	public void setF16Repo(Form16Repository f16Repo) {
		this.f16Repo = f16Repo;
	}

	public FamilyDetailRepository getFdRepo() {
		return fdRepo;
	}

	public void setFdRepo(FamilyDetailRepository fdRepo) {
		this.fdRepo = fdRepo;
	}

	public UnitRepository getUnitRepo() {
		return unitRepo;
	}

	public void setUnitRepo(UnitRepository unitRepo) {
		this.unitRepo = unitRepo;
	}

	public AppointmentRateRepository getArRepo() {
		return arRepo;
	}

	public void setArRepo(AppointmentRateRepository arRepo) {
		this.arRepo = arRepo;
	}

	public FundSummaryRepository getFsRepo() {
		return fsRepo;
	}

	public void setFsRepo(FundSummaryRepository fsRepo) {
		this.fsRepo = fsRepo;
	}

	public BhutanSlabRepository getBsRepo() {
		return bsRepo;
	}

	public void setBsRepo(BhutanSlabRepository bsRepo) {
		this.bsRepo = bsRepo;
	}

	public LpcDeputationRepository getLpcDepnRepo() {
		return lpcDepnRepo;
	}

	public void setLpcDepnRepo(LpcDeputationRepository lpcDepnRepo) {
		this.lpcDepnRepo = lpcDepnRepo;
	}

	public TransferInOutRepository getTransferInOutRepo() {
		return transferInOutRepo;
	}

	public void setTransferInOutRepo(TransferInOutRepository transferInOutRepo) {
		this.transferInOutRepo = transferInOutRepo;
	}

	EmployeeDeductionRepository empdedRepo; 
	EmployeeEarningRepository empearningRepo;
	ClassPayRateRepository classPayRepo;
	EmployeeServiceDetailRepository esdRepo;
	AppendixJRepository appxJRepo;
	RhLocationRepository rhlocationRepo;
	PayScaleRepository psRepo;
	PayMatrixRepository pmRepo;
	ReadyReckonerRepository rrRepo;
	RankRepository rankRepo;
	GroupCategoryRepository gcRepo;
	PayCodeRepository pcRepo;
	StatusCodeRepository scRepo;
	Form16Repository f16Repo;
	FamilyDetailRepository fdRepo;
	UnitRepository unitRepo;
	AppointmentRateRepository arRepo;
	FundSummaryRepository fsRepo;
	BhutanSlabRepository bsRepo;
	LpcDeputationRepository lpcDepnRepo;
	TransferInOutRepository transferInOutRepo;	
	
	public EmployeeDeductionRepository getEmpdedRepo() {
		return empdedRepo;
	}

	public void setEmpdedRepo(EmployeeDeductionRepository empdedRepo) {
		this.empdedRepo = empdedRepo;
	}
	
	public AuditCageRepository getAcRepo() {
		return acRepo;
	}

	public void setAcRepo(AuditCageRepository acRepo) {
		this.acRepo = acRepo;
	}

	public AuditCageAllowanceRepository getAcaRepo() {
		return acaRepo;
	}

	public void setAcaRepo(AuditCageAllowanceRepository acaRepo) {
		this.acaRepo = acaRepo;
	}

	public ArrearsRepository getArrearsRepo() {
		return arrearsRepo;
	}

	public void setArrearsRepo(ArrearsRepository arrearsRepo) {
		this.arrearsRepo = arrearsRepo;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public MiscRepository getMiscRepo() {
		return miscRepo;
	}

	public void setMiscRepo(MiscRepository miscRepo) {
		this.miscRepo = miscRepo;
	}

	public Do2Repository getDo2Repo() {
		return do2Repo;
	}

	public void setDo2Repo(Do2Repository do2Repo) {
		this.do2Repo = do2Repo;
	}
		
	public AllowanceRateRepository getAllowanceRateRepo() {
		return allowanceRateRepo;
	}

	public void setAllowanceRateRepo(AllowanceRateRepository allowanceRateRepo) {
		this.allowanceRateRepo = allowanceRateRepo;
	}
	
	public LpcRegisterRepository getLpcRegisterRepo() {
		return lpcRegisterRepo;
	}

	public void setLpcRegisterRepo(LpcRegisterRepository lpcRegisterRepo) {
		this.lpcRegisterRepo = lpcRegisterRepo;
	}
	
	private Connection getConnection() {
		String url = "jdbc:postgresql://localhost:5432/dolphin";
		String driver = "org.postgresql.Driver";
		String user = env.getProperty("spring.datasource.username");
		String password = env.getProperty("spring.datasource.password");
		Connection con = null;		 
		try {
			Class.forName(driver).newInstance();
			con = DriverManager.getConnection(url, user, password);
		} catch (InstantiationException e) { 
			e.printStackTrace();
		} catch (IllegalAccessException e) { 
			e.printStackTrace();
		} catch (ClassNotFoundException e) { 
			e.printStackTrace();
		} catch (SQLException e) { 
			e.printStackTrace();
		}
		return con;
	}
	
	public boolean isAtcorrDo2AlreadyGenerated(Date effectiveDate) {	
		List<Misc>miscList=miscRepo.findByKey1AndValue2AndDate1AndCurrent(miscKey1, miscValue2, effectiveDate);
		return miscList.size()>0;	
	}
	
	public boolean isAgisRecoveryDone() {	
		List<Misc>miscList=miscRepo.findByKey1AndValue2("AGIS REVISION",Utils.getCurrentQuarter());	 
		return miscList.size()>0;	
	}
	
	public String getStringDate(Date date) {
		String strdate=null;
		GregorianCalendar gc=new GregorianCalendar();
		gc.setTime(date);
		String mm = null, dd=null;		
		String mth=String.valueOf(gc.get(Calendar.MONTH)+1);
		if (mth.length()<2)
			mm="0".concat(mth);
		else
			mm=mth;		
		String day=String.valueOf(gc.get(Calendar.DATE));		
		if (day.length()<2)
			dd="0".concat(day);
		else
			dd=day;
		String yyyy=String.valueOf(gc.get(Calendar.YEAR));		
		strdate=yyyy+"-"+mm+"-"+dd;		
		return strdate;
	}
	
	public void updateMiscTable(String qe, Date date, Usr usr){
		Misc misc = new Misc();		
	    misc.setKey1(miscKey1);
	    misc.setValue1(qe);	
	    misc.setValue2(miscValue2);
	    misc.setDate1(date);
	    misc.setDate2(DateManager.getToday());
	    misc.setUsr(usr);
	    misc.setCurrent(true);
	    miscRepo.save(misc);
	}
	 	 
	private void writeBatchInMisc(String qe, Date date, Usr usr, String allowanceName) { 
		List<Integer> batchList=do2Repo.findAtcorrGeneratedForRevisionBatches(qe, allowanceName.toUpperCase());
	 /*	List<Integer> batchList=null;
		try {
			batchList = do2Repo.findIncrGeneratedForPromotion(qe, DateManager.getDate("2026-01-01"), "INCR", "REG");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		System.out.println(",,,,,  batchlist size  " +batchList.size());
		if (batchList!=null && batchList.size()>0) {
	//		List<Misc> miscList=miscRepo.findAtcorrRevisionBatch(qe, batchList);
	//		if ((miscList!=null && miscList.size()==0) || miscList==null) {				 
				for(Integer batch:batchList) {
					Misc m=new Misc();
					m.setKey1("Batch");
					m.setValue1(qe);
					m.setValue2("Ready");
					m.setDate1(DateManager.getToday());
					m.setCurrent(true);
					m.setIntNumber1(batch);
					miscRepo.save(m);
			//	}
			}
		} 
	}
	
	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
	Date actualAllowanceRevisionFromDate=null, allowanceRevisionFromDate=null; 	
	List<Object[]> affectedEmpList=null;
	final String miscKey1="ATCORR_DO2_GENERATION";	 
	String columnB=null;
	String miscValue2=null;
	int batch=0; 
	final String allowedMonth="07/2026"; 
	Usr usr=Utils.getLoggedInUsr();
	// Bhutan Rate Revision w.e.f 01-04-2019  done in the month of 12/2023 
	
	public String generateAtcorrDo2(String allowanceName) {
		
		  if (!Utils.getCurrentQuarter().equals(allowedMonth)) { 
			  return "No Allowance Revision In This Month !!!"; 
		  } 
		  
		  if (Utils.getClosingStarted()) {
			  return "Accounts Closing For The Month Already In Progress.  No Other Jobs Please !!!"; 
		  }
	/*	  
		  if (allowanceName.trim().toUpperCase().equals("AdpostCtd".toUpperCase())) { 
			  adpostMultipleCeasedReview(); return "ADPOST-Ceased Review Completed !!!";
		  }
	*/	 
		columnB=allowanceName.trim().toUpperCase();
		miscValue2=allowanceName.trim().toUpperCase()+"_Revision";
		usr=Utils.getLoggedInUsr();
		String mesg=""; 
	   	int recs=0; 
	 	int recordsAdded=0;		
		BufferedWriter writer=null;		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/InsertAtcorrDo2ForChafaaRevision.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";		
		
		if (allowanceName.trim().equals("FirstIncr7cpc")) {
			List<Misc>miscList=miscRepo.findByKey1AndValue2("FirstIncr7cpc",Utils.getCurrentQuarter());	 
				if (miscList.size()>0) {
				mesg = " ALREADY INCR REVIEW DONE FOR FIRST INCR MISSING IRLAS ";
				return mesg;
			}
			mesg = firstIncrementGenerationForMissingAnosSevenCpc(allowanceName);
			 miscList=miscRepo.findIncrRevBatch(qe);
				if((miscList!=null && miscList.size()==0) || miscList==null) {
							List<Integer> batchList=do2Repo.findIncrRevBatches(qe);
							if(batchList!=null && batchList.size()>0) {
								for(Integer batch:batchList) {
									Misc m=new Misc();
									m.setKey1("Batch");
									m.setValue1(qe);
									m.setValue2("Ready");
									m.setDate1(DateManager.getToday());
									m.setCurrent(true);
									m.setIntNumber1(batch);
									miscRepo.save(m);
								}
							}
				} 
			return mesg;
		}
		
		if (allowanceName.trim().equals("IncrReview7cpc")) {
			List<Misc>miscList=miscRepo.findByKey1AndValue2("IncrReview7cpc",Utils.getCurrentQuarter());	 
			if (miscList.size()>0) {
			mesg = " ALREADY NCR REVIEW DONE FOR  IRLAS ";
			return mesg;
			}
			mesg = incrReviewSevenCpc(allowanceName);
			
			 miscList=miscRepo.findIncrRevBatch(qe);
			if((miscList!=null && miscList.size()==0) || miscList==null) {
						List<Integer> batchList=do2Repo.findIncrRevBatches(qe);
						if(batchList!=null && batchList.size()>0) {
							for(Integer batch:batchList) {
								Misc m=new Misc();
								m.setKey1("Batch");
								m.setValue1(qe);
								m.setValue2("Ready");
								m.setDate1(DateManager.getToday());
								m.setCurrent(true);
								m.setIntNumber1(batch);
								miscRepo.save(m);
							}
						}
			} 
		
			return mesg;
		}
		
		
		if (Utils.getPao().getPaoCode().equals("98")) {
			if (allowanceName.trim().equals("Bhutan")) {
				setValuesForBhutanRevision();
				mesg = generateAtcorrDo2ForBcaRevision(allowanceName);
				return mesg;
			}
		}
	
		if (allowanceName.trim().equals("Chafaa")) {
	 		setValuesForChafaaRevision();
		}
		if (allowanceName.trim().equals("RiskCfaaRecovery")) {
	//	 	setValuesForRiskCfaaRecovery();
		}
		if (allowanceName.trim().equals("OptfixReview")) {
			setValuesForOptfixReview();
		}
		if (allowanceName.trim().equals("Enroll7cpcIncrReview")) {
			if (1==1) {
				return "Under Testing !!!";
			}
			setValuesForEnroll7cpcIncrReview();
			return "Success";
		}
		
		if (allowanceName.trim().equals("SfaSfacom")) {
			setValuesForSfaSfacomRevision();
			mesg=generateSfaAtcorrDo2(allowanceName);	
	//		mesg=recordsAdded+" ATCORR DO2 Generation For "+allowanceName.toUpperCase()+" Revision Done.";
			return mesg;
			}
		
		if (allowanceName.trim().equals("drdsracancel")) {
			reviewDrdSraCancel();
			mesg=" CANCEL-DRDSRA do2s Review Done" ;
			return mesg;
		}
			
		if (allowanceName.trim().equals("IncrPre2016Reversal")) {
			if (1==1) {
				return "Under Testing / Awaiting Orders";
			}
			EnrollPre2016ReviewForReversionAll erev = new EnrollPre2016ReviewForReversionAll();
			erev.setDo2Repo(do2Repo);
			erev.setAcRepo(acRepo);
			erev.setEnv(env);
			List<Employee> empList = empRepo.findAllEffectiveRegularIdForThisMonth();
			for (Employee e : empList) {
				erev.verifyIncrementDo2AndDateForOneAmongAll(e);
			}
			writeBatchInMiscForPre2016IncrReview(qe, DateManager.getToday(), usr, "INCRREV");
 			mesg=" Increment Review For Reversal of 2016 Erroneous Increment Done" ;
			return mesg;
		}
		
		if (!isAgisRecoveryDone()){
			if (allowanceName.trim().equals("agisrecovery")) {
				jdbcTemplate.execute("delete from adjustment_cb where (pay_code_str='AGIS' or pay_code_str like '%AGIS') and from_date='2025-03-01' and approval_amount in (2500, 12000) "
						+ " and remarks like 'Diff of Revised%' and qe='04/2025' and paid=false and approved=true and record_status='V' and fk_pay_code=18 ");
				recoverAGISforMARCH2025();
				recoverTAGIFforMARCH2025();
				updateMiscAgis(Utils.getCurrentQuarter());
				mesg="AGIS Recovery for the month of 03/2025 inserted in Adjustment CB table." ;
				return mesg;
			}
		}
		
		if (allowanceName.trim().equals("Spic")) {
			setValuesForSpicRevision();
			mesg=generateSfaAtcorrDo2(allowanceName);	
			return mesg;
		}
		
		if (allowanceName.trim().equals("TrgallNccUnitsReview")) {
			setValuesForTrgallGrantedForNccUnitsReview();
			mesg=generateSfaAtcorrDo2(allowanceName);	
			return mesg;
		}
		
		if (allowanceName.trim().equals("TA_EnrollTagifReview")) { 
			mesg=generateAdjustmentCbForTagifDiffAmount();	
			return mesg;
		}
		
		if (allowanceName.trim().equals("AdhocIncrReview")) { 
 			mesg=generateAdhocIncrDo201012006ForRejectedIrlas();	
			return mesg;
		}
		
		if (allowanceName.trim().equals("MacpsDischargeDateReview")) { 
 			mesg=generateAtcorrForMacpsOnDischargeDate();	
			return mesg;
		}
		
		if (allowanceName.trim().equals("IncrDatePromotion")) { 
 			mesg=generateMissingIncrementForASC_South();	
			return mesg;
		}
		
		if (allowanceName.trim().equals("AdhocIncrReviewForNEIrla")) { 
 			mesg=generateAdhocIncrDo201012006ForNEIrlas();	
			return mesg;
		}
		
		if(allowanceName.trim().equals("ExtwkCeasedDaZero")) {
			mesg=generateAtcorrForExtWkCeasedDaZeroCases();
			return mesg;
		}
		
		if(allowanceName.trim().equals("ReviewTPTLonTD")) {
			mesg=generateCeasedGrantedForTPTLonTD();
			return mesg;
		}
		
		if (allowanceName.trim().equals("agisrecoverydsc") && !Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
	 		return "Activity Is Not For Your PAO !!!" ;
	 	}
		
	 	if (!isAgisRecoveryDone()) { 
			if (allowanceName.trim().equals("agisrecoverydsc")) {
				if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && !Utils.getCurrentQuarter().equals("04/2026")) {
					return "Activity Is Not For Your PAO !!!";
 				} 
	 			recoverAGISdiffForApr2026InDSC();
	 			updateMiscAgis(Utils.getCurrentQuarter());
				mesg="Difference of revised AGIS Recovery for the month of 04/2026 inserted in Adjustment CB table." ;
				return mesg;
			}
		}
	 	
	 	if (allowanceName.trim().equals("agisrecoverydsc")) {
	 		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
	 	 		return "Activity Is Already Run !!!" ;
	 		}
	 	}
	 	
	 	if (allowanceName.trim().equals("ReviewDoubleAllowance")) {
	 		setRightDoubleAllowanceAffectedIrlas();
			return mesg;
		}
	 	
		/*if (allowanceName.trim().equals("neBonReview")) {
			reviewNeBon2022to2024();
			mesg=" NEBONUS REVIEW FOR THE YEARS 2022-2023 and 2023-2024 completed in effected cases " ;
			return mesg;
			}*/
		 
		if (isAtcorrDo2AlreadyGenerated(actualAllowanceRevisionFromDate)) {
			mesg="ATCORR DO2 Generation Already Done For "+allowanceName.toUpperCase()+" Revision - "+getStringDate(allowanceRevisionFromDate);
			return mesg; 
		} 
		/*
	 	try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;
			writer = new BufferedWriter(new FileWriter(file));
			for (Object[] obj : affectedEmpList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				effectiveRevisionDate=null;
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
	    			cd=(String) obj[1];
				}
				if (obj[2]!=null) {
	    			unitcd=(String) obj[2];
				}
				if (obj[3]!=null) {
	    			effectiveRevisionDate=(Date) obj[3];
				}				
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
	//			str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+columnB+",ALWREV,"+effectiveRevisionDateString+",P,T,"
	//					+qe+","+qe+",true,true,false,"+usr.getId()+",System Generated Do2";	
				str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+columnB+",,"+effectiveRevisionDateString+",P,T,"
						+qe+","+qe+",true,true,false,"+usr.getId()+",System Generated Do2";	
		
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
	//		    jdbcTemplate.execute("update do2 set status='P', reason=null where description in ('PROFIX', 'ACPFIX', 'HAVFIX', 'INCR') and status in ('I', 'P')  "
	//                     + " and (accepted_qe='09/2024' or review_qe='09/2024') and from_date>'"+effectiveRevisionDate+"' and army_no='"+armyNo+"'");	                 
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertAtcorrDo2ForChafaaRevision.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, column_b, column_c, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();	        
	        updateMiscTable(qe, actualAllowanceRevisionFromDate, usr);
			writeBatchInMisc(qe, actualAllowanceRevisionFromDate, usr, allowanceName.toUpperCase());		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		*/
		mesg=recordsAdded+" ATCORR DO2 Generation For "+allowanceName.toUpperCase()+" Revision Done.";
		return mesg;
	}    
		
	private void setValuesForBhutanRevision() {
		batch=8501;
	/*	try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2019-04-01");  // Date to be changed here after ensuring allowancerate table insertion //Bhutan Rate Revision w.e.f 01-04-2019  done in the month of 12/2023 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2023-04-01");  // Date to be changed here after ensuring allowancerate table insertion //hutan Rate Revision w.e.f 01-04-2019  done in the month of 07/2024 
		} catch (Exception e) {
			e.printStackTrace();
		} */
		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2024-04-01");  // Date to be changed here after ensuring allowancerate table insertion //hutan Rate Revision w.e.f 01-04-2024  done in the month of 06/2026 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		affectedEmpList=getBcaRevisionAffectedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	private void setValuesForChafaaRevision() {
		batch=9001;
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2026-01-01");  // Date to be changed here after ensuring allowancerate table insertion
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		System.out.println(".dates ... "+allowanceRevisionFromDate+"  actual "+actualAllowanceRevisionFromDate+"  string dt "+getStringDate(allowanceRevisionFromDate));
		affectedEmpList=getChafaaRevisionAffectedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	private void setValuesForRiskCfaaRecovery() {
		batch=7101;
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2019-02-22");  // Date to be changed here after ensuring allowancerate table insertion
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		System.out.println("Risk Cfaa Recovery dates ... "+allowanceRevisionFromDate+"  actual "+actualAllowanceRevisionFromDate+"  string dt "+getStringDate(allowanceRevisionFromDate));
		affectedEmpList=getRiskCfaaRecoveryRelatedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	private void setValuesForOptfixReview() {
		batch=3001;		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2016-01-01");   
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		affectedEmpList=getOptfixReviewAffectedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	public List<Object[]> getBcaRevisionAffectedIrlas(String bcaRevisionMinimumDate) {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(
			" create temp table t(emp integer, from_date date); insert into t(emp,from_date)select distinct(fk_employee), min(from_date) "
		  + " from audit_cage where effective and cancelled = false and record_status = 'V' and from_date >= '"+bcaRevisionMinimumDate+"' and id in "
		  + " (select fk_audit_cage from audit_cage_allowance where fk_pay_code = 66 and amount > 0) group by fk_employee; create temp "
		  + " table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; CREATE temp TABLE empdet AS select employee.army_no, "
		  + " employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join employee inner join t1 on t1.emp=employee.id on"
		  + " unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	
	public List<Object[]> getChafaaRevisionAffectedIrlas(String chafaaRevisionMinimumDate) {	
		System.out.println("............chafaa revision fromdate  " +chafaaRevisionMinimumDate);
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute("create temp table t(emp integer, from_date date); "
	 			+ " insert into t(emp,from_date)select distinct(fk_employee), min(from_date) from audit_cage where effective and cancelled = false "
	 			+ " and record_status='V' and from_date>='"+chafaaRevisionMinimumDate+"' and id in (select fk_audit_cage from audit_cage_allowance where "
	 			+ " fk_pay_code=122 and amount=0) group by fk_employee; insert into t(emp, from_date) select distinct(fk_employee), min(from_date) "
	 			+ " from do2 where (description='CHAFAA' or (description='RISK' and column_c='CHAFAA')) "
	 			+ " and from_date>='"+chafaaRevisionMinimumDate+"' and (status='I' or status='P') group by "
	 			+ " fk_employee;create temp table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; "
	 			+ " CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join "
	 			+ " employee inner join t1 on t1.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no");
	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	
	public List<Object[]> getRiskCfaaRecoveryRelatedIrlas(String RiskCfaaRecoveryMinimumDate) {	
		System.out.println("............chafaa revision fromdate  " +RiskCfaaRecoveryMinimumDate);
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	/*
	 	jdbcTemplate.execute("create temp table t(emp integer, from_date date); "
	 			+ " insert into t(emp,from_date)select distinct(fk_employee), min(from_date) from audit_cage where effective = true and cancelled = false "
	 			+ " and record_status='V' and from_date>='"+RiskCfaaRecoveryMinimumDate+"' and id in (select fk_audit_cage from audit_cage_allowance where "
	 			+ " fk_pay_code=298 and amount=0) group by fk_employee; insert into t(emp, from_date) select distinct(fk_employee), min(from_date) "
	 			+ " from do2 where column_a='GRANTED' and description='RISK' and column_b is not null and column_c is not null and column_c in ('CFAA', 'CMFAA') "
	 			+ " and from_date>='"+ RiskCfaaRecoveryMinimumDate+"' and status='V' group by fk_employee;create temp table t1 as select distinct(emp), "
	 			+ " min(from_date) as frdt from t group by emp order by emp; "
	 			+ " CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join "
	 			+ " employee inner join t1 on t1.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no");
	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
		*/
 		jdbcTemplate.execute("create temp table t(emp integer, from_date date); insert into t(emp,from_date)select distinct(fk_employee), min(from_date)  "
				+ "	 	from do2 where description='RISK' and column_c in ('CFAA','CMFAA','CHAFAA')  "
				+ "	 	and column_a='GRANTED' and status='V' and from_date>='2019-02-22'  group by fk_employee;  "
				+ "	 	CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t.from_date, unit.task from unit inner join  "
				+ "	 	employee inner join t on t.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no;");
	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet  ", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[5];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				obj[4]=rs.getInt(5);
				return obj;
			}			 
		});
	    return empList;
	}
	
	public void adpostMultipleCeasedReview() {
		System.out.println("............ADPOST-CEASED Multiple Records Review.................  ");
	 	jdbcTemplate.execute("drop table if exists moreceased;");
		jdbcTemplate.execute("create temp table moreceased as select fk_employee, from_date, min(id), count(*) from do2 where description='ADPOST' "
				+ "and column_a='CEASED' AND status='V' and transcription_type='S' group by fk_employee, from_date order by from_date, count desc;");
	 	List<Object[]> affectedIrlaList = jdbcTemplate.query("select * from moreceased where count>1 order by fk_employee, from_date;", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getLong(1);
				obj[1]=rs.getDate(2); 
				obj[2]=rs.getLong(3); 
				obj[3]=rs.getInt(4);
				return obj;
			}			 
		});
	 	Long empid=0l, do2MinId=0l;
	 	Date fromDate; Integer count=0;
	 	List<AuditCage> acList=null;
	 	List<AuditCageAllowance> acaList=null;
	 	List<Arrears> arrearsList=null;
	 	List<Do2> do2List=null; 	 	
		System.out.println("............ADPOST-CEASED Multiple Records Review................. Valid Affected IRLAs list size  "+affectedIrlaList.size());
	 	for (Object[] obj : affectedIrlaList) {
	 		empid=0l; do2MinId=0l; fromDate=null; count=0; 
		 	acList=null; acaList=null; arrearsList=null; do2List=null;
	 		empid=(Long) obj[0];
	 		fromDate=(Date) obj[1];
	 		do2MinId=(Long) obj[2];
	 		count=(Integer) obj[3];	 		
	 		if (count > 1) {	 			
	 			acaList=acaRepo.getAdpostDo2AffectedAuditCageAllowanceList(empid, do2MinId, fromDate);
	 			if (acaList!=null && acaList.size()>0) {
	 				for (AuditCageAllowance aca : acaList) {
	 					System.out.println("Valid................aca id "+aca.getId()+" emp id "+empid+" do2minid "+do2MinId+" do2id "+aca.getDo2().getId()+ "  fromdate "+fromDate);
	 		 			jdbcTemplate.execute("delete from audit_cage_allowance where audit_cage_allowance.id="+aca.getId()+" and "
	 			 			+ " fk_audit_cage in (select id from audit_cage where effective=true and cancelled=false and record_status='V' and "
	 				 		+ "	from_date=to_date and fk_employee="+empid+" and fk_do2>"+do2MinId+" and fk_do2 in (select id from do2 where description='ADPOST' and column_a='CEASED' "
	 				 		+ " AND status='V' and transcription_type='S' and from_date='"+ fromDate+"' and id>"+do2MinId+" and fk_employee="+empid+"))");
	 				}	
	 			}
	 			acList=acRepo.getAffectedAuditCagesForAdpostCeased(empid, do2MinId, fromDate);
	 			if (acList!=null && acList.size()>0) {
	 				for (AuditCage ac : acList) {
	 					jdbcTemplate.execute("delete from audit_cage ac where ac.id="+ac.getId()+" and ac.effective=true and cancelled=false and record_status='V' and fk_employee="+empid+" "
	 						+ " and ac.fk_do2>"+do2MinId+" and (to_date is null or ac.from_date=ac.to_date) and ac.fk_do2 in (select id from do2 where description='ADPOST'"
	 						+ " and column_a='CEASED' AND status='V' and transcription_type='S' and from_date='"+fromDate+"' and fk_employee="+empid+" and id>"+do2MinId+")"); 
	 							
	 				}
	 			}
	 	 		arrearsList=arrearsRepo.getAffectedArrearsForAdpostCeased(empid, do2MinId, fromDate);
	 	 		if (arrearsList!=null && arrearsList.size()>0) {
	 	 			for (Arrears ar : arrearsList) {
	 	 				jdbcTemplate.execute(" delete from arrears where id="+ar.getId()+" and fk_employee="+empid+" and fk_do2>"+do2MinId+" and fk_do2 in (select id from do2 where description='ADPOST' "
	 	 		 				+ " and column_a='CEASED' AND status='V' and transcription_type='S' and from_date='"+fromDate+"' and fk_employee="+empid+" and id>"+do2MinId+"); ");
	 	 			}
	 	 		}
	 		    do2List=do2Repo.getAffectedDo2ForAdpostCeased(empid, do2MinId, fromDate);
	 		    if (do2List!=null && do2List.size()>0) {
	 		    	for (Do2 d : do2List) {
	 		    		jdbcTemplate.execute(" delete from do2 where id="+d.getId()+" and description='ADPOST' and column_a='CEASED' AND status='V' and transcription_type='S' "
	 		    				+ " and from_date='"+fromDate+"' and fk_employee="+empid+" and id>"+do2MinId+" and id not in (select fk_do2 from audit_cage where effective=true and "
	 		    				+ " cancelled=false and record_status='V' and fk_employee="+empid+" and from_date='"+fromDate+"' and fk_do2>"+do2MinId+" and fk_do2 in (select id from "
	 		    				+ " do2 where id="+d.getId()+" and description='ADPOST' and column_a='CEASED' AND status='V' and transcription_type='S' and from_date='"+fromDate+"' "
	 		    				+ " and fk_employee="+empid+" and id>"+do2MinId+"))");	 			 		
	 		    	}
	 		    }	 			
	 		}
	 	}	
	 	// Pending status adpost ceased
	 	jdbcTemplate.execute("drop table if exists moreceased;");
		jdbcTemplate.execute("create temp table moreceased as select fk_employee, from_date, min(id), count(*) from do2 where description='ADPOST' "
				+ "and column_a='CEASED' AND status='P' and transcription_type='S' group by fk_employee, from_date order by from_date, count desc;");
		affectedIrlaList=null;
	 	affectedIrlaList=jdbcTemplate.query("select * from moreceased where count>1 order by fk_employee, from_date;", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getLong(1);
				obj[1]=rs.getDate(2); 
				obj[2]=rs.getLong(3); 
				obj[3]=rs.getInt(4);
				return obj;
			}			 
		});
	 	empid=0l; do2MinId=0l; fromDate=null; count=0; 
	 	acList=null; acaList=null; arrearsList=null; do2List=null;	
		System.out.println("............ADPOST-CEASED Multiple Records Review................. Pending Affected IRLAs list size  "+affectedIrlaList.size());
	 	for (Object[] obj : affectedIrlaList) {
	 		empid=0l; do2MinId=0l; fromDate=null; count=0; 
		 	acList=null; acaList=null; arrearsList=null; do2List=null;
	 		empid=(Long) obj[0];
	 		fromDate=(Date) obj[1];
	 		do2MinId=(Long) obj[2];
	 		count=(Integer) obj[3];	 		
	 		if (count > 1) {	 			
	 			acaList=acaRepo.getAdpostDo2AffectedAuditCageAllowanceList(empid, do2MinId, fromDate);
	 			if (acaList!=null && acaList.size()>0) {
	 				for (AuditCageAllowance aca : acaList) {
	 					System.out.println("Pending................aca id "+aca.getId()+" emp id "+empid+" do2minid "+do2MinId+ " do2id "+aca.getDo2().getId()+ " fromdate "+fromDate);		 		 		
	 		 			jdbcTemplate.execute("delete from audit_cage_allowance where audit_cage_allowance.id="+aca.getId()+" and "
	 			 			+ " fk_audit_cage in (select id from audit_cage where effective=true and cancelled=false and record_status='P' and (to_date is null "
	 				 		+ "	or from_date=to_date) and fk_employee="+empid+" and fk_do2>"+do2MinId+" and fk_do2 in (select id from do2 where description='ADPOST' and column_a='CEASED' "
	 				 		+ " AND status='P' and transcription_type='S' and from_date='"+fromDate+"' and id>"+do2MinId+" and fk_employee="+empid+"))");
	 				}	 
	 			}
	 			acList=acRepo.getAffectedAuditCagesForAdpostCeased(empid, do2MinId, fromDate);
	 			if (acList!=null && acList.size()>0) {
	 				for (AuditCage ac : acList) {
	 					jdbcTemplate.execute("delete from audit_cage ac where ac.id="+ac.getId()+" and ac.effective=true and cancelled=false and record_status='P' and fk_employee="+empid+" "
	 						+ " and ac.fk_do2>"+do2MinId+" and (to_date is null or ac.from_date=ac.to_date) and ac.fk_do2 in (select id from do2 where description='ADPOST' and "
	 						+ "column_a='CEASED' AND status='P' and transcription_type='S' and from_date='"+fromDate+"' and fk_employee="+empid+" and id>"+do2MinId+")"); 	 							
	 				}
	 			}
	 	 		arrearsList=arrearsRepo.getAffectedArrearsForAdpostCeased(empid, do2MinId, fromDate);
	 	 		if (arrearsList!=null && arrearsList.size()>0) {
	 	 			for (Arrears ar : arrearsList) {
	 	 				jdbcTemplate.execute(" delete from arrears where id="+ar.getId()+" and fk_employee="+empid+" and fk_do2>"+do2MinId+" and fk_do2 in (select id from do2 where description='ADPOST' "
	 	 		 				+ " and column_a='CEASED' AND status='P' and transcription_type='S' and from_date='"+fromDate+"' and fk_employee="+empid+" and id>"+do2MinId+"); ");
	 	 			}
	 	 		}
	 		    do2List=do2Repo.getAffectedDo2ForAdpostCeased(empid, do2MinId, fromDate);
	 		    if (do2List!=null && do2List.size()>0) {
	 		    	for (Do2 d : do2List) {
	 		    		jdbcTemplate.execute(" delete from do2 where id="+d.getId()+" and description='ADPOST' and column_a='CEASED' AND status='P' and transcription_type='S' and from_date='"+fromDate 
	 			 			+ "' and fk_employee="+empid+" and id>"+do2MinId+" and id not in (select fk_do2 from audit_cage where effective=true and "
	 			 			+ "  cancelled=false and record_status='V' and fk_employee="+empid+" and from_date='"+fromDate+"' and fk_do2>"+do2MinId+" and fk_do2 in (select id from "
	 			 			+ "	 do2 where id="+d.getId()+" and description='ADPOST' and column_a='CEASED' AND status='V' and transcription_type='S' and from_date='"+fromDate 
	 			 			+ "' and fk_employee="+empid+" and id>"+do2MinId+"))");	 			 			 			 		
	 		    	}
	 		    }
	 		    
	 		   do2List=do2Repo.getAffectedDo2ForAdpostCeasedPending(empid, do2MinId, fromDate);
	 		   System.out.println("..............do2 list size pending ceased " +do2List.size()  +"  Please wait..................");
	 		    if (do2List!=null && do2List.size()>0) {
	 		    	for (Do2 d : do2List) {
	 		    		jdbcTemplate.execute(" delete from do2 where id="+d.getId()+" and description='ADPOST' and column_a='CEASED' AND status='P' and transcription_type='S' and from_date='"+fromDate 
	 			 			+ "' and fk_employee="+empid+" and id>"+do2MinId);	 			 			 			 		
	 		    	}
	 		    }	
	 		}
	 	}	 	
	}
		
	// to get the IRLAs are effected personnel for whom class pay has to be revised w.e.f 05-01-2009 due to merger of trades
	public List<Object[]> getClpayRevisionIrlas(String chafaaRevisionMinimumDate,String curQe) {	
		System.out.println("............clspay revision fromdate  " +chafaaRevisionMinimumDate);
				
		String[] revisedCategory = { "DVRSPLVEH", "ENGART", "OPR", "SSY", "CHEFSPL", "CHEFSPEL"} ;
		final List<String> rev_CATEGORY = Arrays.asList(revisedCategory);
	
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 			 	
	 	jdbcTemplate.execute("create temp table t(emp integer); "
	 			+ "insert into t(emp) select employee.id from employee join "
	 			+ "employee_deduction on employee_deduction.fk_employee=employee.id and "
	 			+ "employee_deduction.qe='"+curQe+"' and employee.current_record where employee.fk_group_category in "
	 			+ "(select id from group_category where soldier_category in ('DVRSPLVEH', 'ENGART', 'OPR', 'SSY',  'CHEFSPEL') "
	 			+ "and from_date='2009-01-05') and employee.qe='"+curQe+"' and fk_status_code <5;"
	 			+ "create temp table empdet as select employee.army_no,employee.check_digit,"
	 			+ "unit.unit_code, unit.task from unit inner join employee inner join t on t.emp=employee.id on unit.id=employee.fk_present_unit "
	 			+ "order by unit.task, unit.unit_code,employee.army_no;");
	 			List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet  ", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
			
				return obj;
			}			 
		});
	    return empList;
	}
		
	private void setValuesForClpayRevision(String curQe) {
		batch=8501;
	/*	try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2019-04-01");  // Date to be changed here after ensuring allowancerate table insertion //Bhutan Rate Revision w.e.f 01-04-2019  done in the month of 12/2023 
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2009-01-05");  // Date to be changed here after ensuring allowancerate table insertion //hutan Rate Revision w.e.f 01-04-2019  done in the month of 07/2024 
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		affectedEmpList=getClpayRevisionIrlas(getStringDate(allowanceRevisionFromDate),curQe);	
	}
	
	public List<Object[]> getOptfixReviewAffectedIrlas(String optfixRevisionMinimumDate) {	
		System.out.println("............optfix revision fromdate  " +optfixRevisionMinimumDate);
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1, t2;");	 	
/*	 	jdbcTemplate.execute("create temp table t(emp integer, from_date date); "
	 			+ "	insert into t(emp, from_date) select distinct(fk_employee), min(from_date) from do2 where description='OPTFIX' and status!='V' "
	 			+ "	and from_date>='2016-01-01' and (accepted_qe='09/2024' or review_qe='09/2024') group by fk_employee; " 
	 			+ " create temp table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; "
	 			+ "	CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join "
	 			+ "	employee inner join t1 on t1.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no");   */
	/* 	
		 	jdbcTemplate.execute("create temp table t(emp integer, from_date date); "		 	
		 			+ "	insert into t (emp, from_date) select distinct(fk_employee), min(from_date) from do2 where description='OPTFIX' and status!='V' "
		 			+ "	and from_date>='2016-01-01' and (accepted_qe='09/2024' or review_qe='09/2024') group by fk_employee; "
		 			+ "	create temp table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; " 
		 			+ "	insert into t1 (emp, frdt) select distinct(fk_employee), cast('2023-12-30' as date) as from_date from arrears where qe='09/2024' "
		 			+ "	and fk_pay_code=55 and drawn>due and from_date in ('2024-01-01', '2024-07-01'); "
		 			+ " insert into t1 (emp, frdt) select fk_employee, cast('2024-01-01' as date) as  from_date from do2 where description='HRA' and column_a='GRANTED' "
		 			+ " and status='V' and from_date<='2024-01-01' and fk_employee not in (select fk_employee from do2 where description='HRA' and column_a='CEASED' "
		 			+ " and from_date<='2024-01-01' and status='V') and fk_employee in (select id from employee where current_record and fk_status_code=1) group by fk_employee;"
		 			+ "	create temp table t2 as select distinct(emp), min(frdt) as frdt from t1 group by emp order by emp; "
		 			+ "	CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t2.frdt, unit.task from unit inner join " 
		 			+ "	employee inner join t2 on t2.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no; ");
*/
	 	
	 	jdbcTemplate.execute("create temp table t(emp integer, from_date date); "		 	
	 			+ "	insert into t (emp, from_date) select distinct(fk_employee), min(from_date) from do2 where description='OPTFIX' and status!='V' "
	 			+ "	and from_date>='2016-01-01' and (accepted_qe='09/2024' or review_qe='09/2024') and fk_employee in  (select id from employee  "
	 			+ " where current_Record and fk_status_code<4)	group by fk_employee ; "
	 			+ "	create temp table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; " 
	 			+ "	insert into t1 (emp, frdt) select distinct(fk_employee), cast('2023-12-30' as date) as from_date from arrears where qe='09/2024' "
	 			+ "	and fk_pay_code=55 and drawn>due and from_date in ('2024-01-01', '2024-07-01'); "
	 			+ " insert into t1 (emp, frdt)  select distinct(fk_employee), min(from_date) from audit_cage where effective and cancelled = false "
	 			+ "	and record_status='V' and from_date>='2024-01-01' and id in (select fk_audit_cage from audit_cage_allowance where "
	 			+ " fk_pay_code=146 and amount!=0 and fk_do2 is not null and fk_do2 in (select id from do2 where description='HRA' "
	 			+ "	and column_a='GRANTED' and from_date <='2024-01-01')) and fk_employee in  (select id from employee where current_Record and fk_status_code<4)"
	 			+ "	group by fk_employee;"
	 			+ "	create temp table t2 as select distinct(emp), min(frdt) as frdt from t1 group by emp order by emp; "
	 			+ "	CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t2.frdt, unit.task from unit inner join " 
	 			+ "	employee inner join t2 on t2.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no; ");

	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	
	private void setValuesForEnroll7cpcIncrReview() {
		batch=3001;	
		actualAllowanceRevisionFromDate=null;
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2016-01-01");   
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		getEnroll7cpcIncrAffectedIrla(getStringDate(allowanceRevisionFromDate));	
	}
	
	public void getEnroll7cpcIncrAffectedIrla(String enroll7cpcIncrMinimumDate) {	
		System.out.println("............enroll incr revision fromdate  " +enroll7cpcIncrMinimumDate);
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS a, b;");	 	
 	 	 
 	 	jdbcTemplate.execute("create temp table a as "
	 			+ " select employee.id as fk_employee from enroll_do2 inner join employee on employee.army_no=enroll_do2.army_no "
	 			+ "	where enrollment_date>'2016-01-01' and employee.regular_ta_flag='R' and employee.current_record=true; "
	 			+ "	CREATE TEMP TABLE B AS WITH INCREMENT_RANKS AS(SELECT FK_EMPLOYEE,FROM_DATE,row_NUMBER() OVER (PARTITION BY FK_EMPLOYEE ORDER BY FROM_DATE) "
	 			+ "	AS RANK FROM DO2 WHERE DESCRIPTION='INCR' AND STATUS='V' AND FK_EMPLOYEE IN (SELECT * FROM A)) "
	 			+ "	SELECT R1.FK_EMPLOYEE,R1.FROM_DATE AS FIRST_INCREMENT_DATE,R2.FROM_DATE AS SECOND_INCREMENT_DATE,R2.FROM_DATE-R1.FROM_DATE AS DAY_DIFFERENCE "
	 			+ "	FROM INCREMENT_RANKS R1 JOIN INCREMENT_RANKS R2 ON R1.FK_EMPLOYEE=R2.FK_EMPLOYEE AND R1.RANK = 1 AND R2.RANK=2 "
	 			+ "	WHERE (R2.FROM_DATE - R1.FROM_DATE)>366; ");

	 	List<Object[]> empList = jdbcTemplate.query("SELECT employee.id, EMPLOYEE.ARMY_NO, employee.check_digit, employee.fk_present_unit, "
	 			+ " B.FIRST_INCREMENT_DATE, B.SECOND_INCREMENT_DATE, DAY_DIFFERENCE "
	 			+ "	FROM B INNER JOIN EMPLOYEE ON EMPLOYEE.ID=B.FK_EMPLOYEE WHERE EMPLOYEE.CURRENT_RECORD=true "
	 			+ " order by employee.id ", new RowMapper<Object[]>() {			 
				@Override
				public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
					Object[] obj=new Object[7];
					obj[0]=rs.getLong(1);
					obj[1]=rs.getString(2); 
					obj[2]=rs.getString(3);
					obj[3]=rs.getLong(4);
					obj[4]=rs.getDate(5);
					obj[5]=rs.getDate(6);
					obj[6]=rs.getLong(7);
 
					return obj;
				}			 
			});
		 	 
		 	if (empList!=null && empList.size()>0) {
		 		IncrReview incrRev = new IncrReview();
		 		incrRev.setDo2Repo(do2Repo);
		 		incrRev.setAcRepo(acRepo);
		 		incrRev.setEmpRepo(empRepo);
		 		incrRev.setRevtype("SINGLE");		 		
	 		for (Object[] obj : empList) {
	 			System.out.println("... Reviewing for ......" +obj[1].toString());
	 			incrRev.setArmyNumber(obj[1].toString().toUpperCase());
	  			incrRev.startReview(obj[1].toString().toUpperCase());
	 			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Utils.handleException(e);
				}
	 		}
	 		String qe = Utils.getCurrentQuarter();
	 		List<Integer> batchList=do2Repo.findIncrGeneratedForRevisionBatches(qe);
//	 		System.out.println("..........do2 batch list size " +batchList.size());
			if (batchList!=null && batchList.size()>0) {
				List<Misc> miscList=miscRepo.findAtcorrRevisionBatch(qe, batchList);
		//		System.out.println("........... misc batch list size "+miscList.size());
				if ((miscList!=null && miscList.size()==0) || miscList==null) {				 
					for (Integer batch:batchList) {
						Misc m=new Misc();
						m.setKey1("Batch");
						m.setValue1(qe);
						m.setValue2("Ready");
						m.setDate1(DateManager.getToday());
						m.setCurrent(true);
						m.setIntNumber1(batch);
						miscRepo.save(m);
					}
				}
			} 
	 	}
	}
	
	public String generateAlwrev(String allowanceName, String curQe) {
		
 //		setValuesForClpayRevision(curQe);
		
		if (!Utils.getCurrentQuarter().equals(allowedMonth)) {
	 		return "No Allowance Revision In This Month !!!";
		}		
		if (Utils.getClosingStarted()) {
			return "Accounts Closing For The Month Already In Progress.  No Other Jobs Please !!!";			 
		}
	
		columnB=allowanceName.trim().toUpperCase();
		miscValue2=allowanceName.trim().toUpperCase()+"_Revision";
		Usr usr=Utils.getLoggedInUsr();
		String mesg=""; 
	   	int recs=0; 
	 	int recordsAdded=0;		
		BufferedWriter writer=null;		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/InsertAlwrevDo2ForClspayRevision.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";		
				
	 	try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;
			writer = new BufferedWriter(new FileWriter(file));
			System.out.println ( " affectedEmpList " +affectedEmpList.size());
			for (Object[] obj : affectedEmpList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				effectiveRevisionDate=null;
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
	    			cd=(String) obj[1];
				}
				if (obj[2]!=null) {
	    			unitcd=(String) obj[2];
				}
				
				effectiveRevisionDate=DateManager.getDate("2009-01-05");
							
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
			
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
				str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ALWREV,CLPAYREV,"+effectiveRevisionDateString+",P,T,"
						+qe+","+qe+",true,true,false,"+usr.getId()+",System Generated Do2";	
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertAlwrevDo2ForClspayRevision.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, column_a, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	     
	        file.delete();	        
	        updateMiscTableAlwrev(qe, actualAllowanceRevisionFromDate, usr);
			writeBatchInMiscAlwrev(qe, actualAllowanceRevisionFromDate, usr, allowanceName.toUpperCase());		
		} catch (Exception e) {
			e.printStackTrace();
		}
		mesg=recordsAdded+" ALWREV DO2 Generation For "+allowanceName.toUpperCase()+" Revision Done.";
		return mesg;
	}    
	
	public void updateMiscTableAlwrev(String qe, Date date, Usr usr){
		Misc misc = new Misc();
	    misc.setKey1("ALWREV_DO2_GENERATION");
	    misc.setValue1(qe);	
	    misc.setValue2(miscValue2);
	    misc.setDate1(date);
	    misc.setDate2(DateManager.getToday());
	    misc.setUsr(usr);
	    misc.setCurrent(true);
	    miscRepo.save(misc);
	}
	 	 
	private void writeBatchInMiscAlwrev(String qe, Date date, Usr usr, String allowanceName) { 
		List<Integer> batchList=do2Repo.findAlwrevGeneratedForRevisionBatches(qe);
		if (batchList!=null && batchList.size()>0) {
			List<Misc> miscList=miscRepo.findAtcorrRevisionBatch(qe, batchList);
			if ((miscList!=null && miscList.size()==0) || miscList==null) {				 
				for (Integer batch:batchList) {
					Misc m=new Misc();
					m.setKey1("Batch");
					m.setValue1(qe);
					m.setValue2("Ready");
					m.setDate1(DateManager.getToday());
					m.setCurrent(true);
					m.setIntNumber1(batch);
					miscRepo.save(m);
				}
			}
		} 
	}		

	private void setValuesForSfaSfacomRevision() {
		batch=8501;
	/*	try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2019-04-01");  // Date to be changed here after ensuring allowancerate table insertion //Bhutan Rate Revision w.e.f 01-04-2019  done in the month of 12/2023 
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2024-01-01");  // Date to be changed here after ensuring allowancerate table insertion //hutan Rate Revision w.e.f 01-04-2019  done in the month of 07/2024 
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		affectedEmpList=getSfaSfacomAffectedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	public List<Object[]> getSfaSfacomAffectedIrlas(String sfaRevisionDate) {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(
			" create temp table t(emp integer, from_date date); insert into t(emp,from_date)select distinct(fk_employee), min(from_date) "
		  + " from audit_cage where effective and cancelled = false and record_status = 'V' and from_date >= '"+sfaRevisionDate+"' and id in "
		  + " (select fk_audit_cage from audit_cage_allowance where fk_pay_code in (258,288) and amount > 0) group by fk_employee; create temp "
		  + " table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; CREATE temp TABLE empdet AS select employee.army_no, "
		  + " employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join employee inner join t1 on t1.emp=employee.id on"
		  + " unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	
	
	public String generateSfaAtcorrDo2(String allowanceName) {
		if (!Utils.getCurrentQuarter().equals(allowedMonth)) {
	 		return "No Allowance Revision In This Month !!!";
		}		
		if (Utils.getClosingStarted()) {
			return "Accounts Closing For The Month Already In Progress.  No Other Jobs Please !!!";			 
		}
	
		columnB=allowanceName.trim().toUpperCase();
		miscValue2=allowanceName.trim().toUpperCase()+"_Revision";
		Usr usr=Utils.getLoggedInUsr();
		String mesg=""; 
	   	int recs=0; 
	 	int recordsAdded=0;		
		BufferedWriter writer=null;		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/InsertAtcorrDo2ForSfaSfacomRevision.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";			
		
		if (isAtcorrDo2AlreadyGenerated(actualAllowanceRevisionFromDate)) {
			mesg="ATCORR DO2 Generation Already Done For "+allowanceName.toUpperCase()+" Revision - "+getStringDate(allowanceRevisionFromDate);
			return mesg; 
		}	
	 	try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;
			writer = new BufferedWriter(new FileWriter(file));
			for (Object[] obj : affectedEmpList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				effectiveRevisionDate=null;
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
	    			cd=(String) obj[1];
				}
				if (obj[2]!=null) {
	    			unitcd=(String) obj[2];
				}
				if (obj[3]!=null) {
	    			effectiveRevisionDate=(Date) obj[3];
				}				
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
					str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+columnB+",,"+effectiveRevisionDateString+",P,T,"
						+qe+","+qe+",true,true,false,"+usr.getId()+",System Generated Do2";	
		
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
			    	                 
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertAtcorrDo2ForSfaSfacomRevision.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, column_b, column_c, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();	        
	        updateMiscTable(qe, actualAllowanceRevisionFromDate, usr);
			writeBatchInMisc(qe, actualAllowanceRevisionFromDate, usr, allowanceName.toUpperCase());		
		} catch (Exception e) {
			e.printStackTrace();
		}
		mesg=recordsAdded+" ATCORR DO2 Generation For "+allowanceName.toUpperCase()+" Revision Done.";
		return mesg;
	}    
			
	public String reviewDrdSraCancel() {
		String mesg=""; 
		if (!Utils.getCurrentQuarter().equals(allowedMonth)) {
	 		return mesg="No Allowance Revision In This Month !!!";
		}		
		if (Utils.getClosingStarted()) {
			return mesg="Accounts Closing For The Month Already In Progress.  No Other Jobs Please !!!";			 
		}
		
		List<Do2> drdSraList = do2Repo.findDrdSraCancelDo2List(); 
		for(Do2 drdSraCancelDo2 : drdSraList){
			List<Do2> refDo2List = Utils.getReferenceDo2List(drdSraCancelDo2);
			for(Do2 doRef : refDo2List){
				
				if (doRef.getDescription().equals(drdSraCancelDo2.getColumnA())){
					insertArrears(drdSraCancelDo2, doRef);
				}				
			}				
		}		
		return mesg;
	}
	
	public void insertArrears(Do2 do2,Do2 refDo2){
		
			List<Arrears> arrearsList = arrearsRepo.findByDo2Id(refDo2.getId()); 
			Long empid = do2.getEmployee().getId();
			int neencarrs=0;
		
			String description = "";
			if (arrearsList.size() > 0) {
				for (Arrears arr : arrearsList) {			
					description = refDo2.getDescription();
						 if (description.equals("DRDSRA") || description.equals("GSRA")) {
							description = Do2Manager.SRA;
						 }	
					 Utils.addArrearsForCancelDo2(do2, refDo2, (arr.getAmount() * -1), description, arr.getFromDate(), arr.getToDate());
				}
			}
	}	
		
	/*
	public String reviewNeBon2022to2024() {
		
		String mesg="";
	
		
		Usr usr = Utils.getLoggedInUsr();
		int level = usr.getDesignation().getLevel();
		
		List<Do2> neBondo2List  = do2Repo.NeBondo2List();
		for(Do2 NeBondo2List : neBondo2List){
		
		List<LpcRegister> lpcList = lpcRegisterRepo.findLpcRegisterRecord(neBondo2List.get(0).getEmployee().getId());
			if(lpcList.size()>0) {
				Date dischargeDate= lpcList.get(0).getDischargeDate();
				Date do2ToDate = neBondo2List.get(0).getToDate();
				if(!do2ToDate.equals(dischargeDate) && dischargeDate.before(do2ToDate)) {
					
					double AVERAGE_DAYS_OF_MONTH_FOR_PLBON = 30.4;
					double adjustedNeBonAmount = 0.0;
					double arrearsAmount = 0.0;
					double dueNeBonAmount = 0.0;
					int noOfMonths = 0;
					double diffNeBonAmount = 0.0;
					
					neBondo2List.get(0).setRemarks("TO date chaged from" +do2ToDate +" to " +dischargeDate  +" Reviewed Ne Bonus in the month of 04/2025" );
					neBondo2List.get(0).setToDate(dischargeDate);
					do2Repo.save(neBondo2List.get(0));
					
					
					 if (neBondo2List.get(0).getDescription().equals("NEBON")) {
						 List<Allowance> plbonFromAllowanceRate = Utils.getApplicableAllowanceRates("PLBON", neBondo2List.get(0).getFromDate(), neBondo2List.get(0).getToDate());
						 	 arrearsAmount=((plbonFromAllowanceRate.get(0).getRateAmount()*plbonFromAllowanceRate.get(0).getRatePercentage())/AVERAGE_DAYS_OF_MONTH_FOR_PLBON);
						 	 noOfMonths = (int) DateManager.getNoOfDays(neBondo2List.get(0).getFromDate(), neBondo2List.get(0).getToDate());
							 dueNeBonAmount=Math.round(arrearsAmount * noOfMonths / 12);
							 adjustedNeBonAmount = 0.0;
							
							List<Arrears> arrlist =arrearsRepo.findByEmpIdAndDo2(neBondo2List.get(0).getEmployee().getId(), neBondo2List.get(0).getId());
				  			   
							  
							   for (Arrears arr : arrlist){
								 //  System.out.println ( " arr amount " +arr.getAmount() +" ID  " +d.getEmployee().getId()  +" ano" +d.getEmployee().getArmyNo()  +" do2id " +d.getId()  +" from date " +d.getFromDate());
							 	    adjustedNeBonAmount += arr.getAmount();
							   }
							   diffNeBonAmount = adjustedNeBonAmount - dueNeBonAmount;
				    	
							   if (diffNeBonAmount != 0.0 ){
							 	   insertIntoArrears(neBondo2List.get(0), "PLBON",diffNeBonAmount);
							   }		
					
					 }
			}
			
		}
				
		
		
	}
		return mesg="NEBONUS REVIEW FOR THE YEARS 2022-2023 and 2023-2024 completed in effected cases ";
	}	
		public  void insertIntoArrears(Do2 do2,String description,double amount){
  		    do2.setReviewQe(Utils.getCurrentQuarter());  	
  		    do2Repo.save(do2);
  		    
  			Arrears arrears = new Arrears();
  			arrears.setDo2(do2);
  			arrears.setFromDate(do2.getFromDate());
  			if (do2.getToDate() == null) {
  				arrears.setToDate(do2.getFromDate());
  			} else {
  				arrears.setToDate(do2.getToDate());
  			}  			
  			arrears.setAmount((double) Math.round(amount));
  			arrears.setPayCode(Utils.getPayCode(description));
  			arrears.setQe(Utils.getCurrentQuarter());
  			arrears.setRoundOffAmount(false);
  			arrears.setRegularPay(false);
  			arrears.setPaid(false);
  			arrears.setEmployee(do2.getEmployee());
  			arrears.setArrestPayment(false);
  			arrears.setCreatedAt(new Timestamp(System.currentTimeMillis()));
  			arrearsRepo.save(arrears);
  			 
  	}*/
	public String recoverAGISforMARCH2025() {
		String mesg="";
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertAdjAgisRecords.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";
//		String previousQe = Utils.getPreviousQuarterEnding();
		String previousQe = "03/2025";
		BufferedWriter writer=null;
		int batch=999;
		try {	
			writer = new BufferedWriter(new FileWriter(file));
		List<EmployeeDeduction> empdedList = empdedRepo.getAgisRecoveryIRLAsList(previousQe);
		for (EmployeeDeduction empded : empdedList){
			
			Employee emp = Utils.getCurrentEmployee(empded.getEmployee().getArmyNo());
			if (emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("D")){
				continue;
			}
			 if (emp.getSubstantiveRank() != null && emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
				 continue;
			 }
			 
			 if (emp.getStatusCode() != null && emp.getStatusCode().getId() > 5) {
				 continue;
			 }
			
			 Date agisStartDate=null;
			
				agisStartDate = DateManager.getDate("2025-03-01");
		
			 Date agisEndDate=null;
		
				agisEndDate = DateManager.getDate("2025-03-31");
			
			 
			 str = batch+","+emp.getId()+","+emp.getArmyNo()+","+emp.getCheckDigit()+",S,+"+emp.getUnit().getId()+",18,"+
				      getStringDate(agisStartDate)+","+getStringDate(agisEndDate)+",2500,AD,"+qe+",V,AGIS,false,true,AGIS,Diff of Revised AGIS Recovered";
			 
			 
			 	writer.write(str);	
			    writer.newLine();			
			    
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
	   	    FileReader fileReader = new FileReader(dir+"/insertAdjAgisRecords.csv");
	   	    copyManager.copyIn(
	   	    		" COPY adjustment_Cb(batch_no, fk_employee,army_no, check_digit, transcription_type,fk_unit, fk_pay_code, from_date, to_Date, approval_amount, adj_cb,"
	   	    		+ " qe, record_status, pay_code, it_adjustment, approved, pay_code_str, remarks) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		  mesg="AGIF Recovery for the month of 03/2025 inserted in Adjustment CB table.";
		  return mesg;	
	}				
	
	public String recoverTAGIFforMARCH2025() {
		String mesg="";
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertAdjTagifRecords.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";
		BufferedWriter writer=null;
		int batch=999;
		try {	
			writer = new BufferedWriter(new FileWriter(file));
		List<EmployeeDeduction> empdedList = empdedRepo.getTagifRecoveryIRLAsList();
		for (EmployeeDeduction empded : empdedList){
			
			Employee emp = Utils.getCurrentEmployee(empded.getEmployee().getArmyNo());
			if (emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("D")){
				continue;
			}
			 if (emp.getSubstantiveRank() != null && emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
				 continue;
			 }
			 
			 if (emp.getStatusCode() != null && emp.getStatusCode().getId() > 5) {
				 continue;
			 }
			
			 Date agisStartDate=null;
			
				agisStartDate = DateManager.getDate("2025-03-01");
		
			 Date agisEndDate=null;
		
				agisEndDate = DateManager.getDate("2026-02-28");
			
			 
			 str = batch+","+emp.getId()+","+emp.getArmyNo()+","+emp.getCheckDigit()+",S,+"+emp.getUnit().getId()+",18,"+
				      getStringDate(agisStartDate)+","+getStringDate(agisEndDate)+",12000,AD,"+qe+",V,AGIS,false,true,AGIS,Diff of Revised TAGIF Recovered";
			 
			 
			 	writer.write(str);	
			    writer.newLine();			
			    
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
	   	    FileReader fileReader = new FileReader(dir+"/insertAdjTagifRecords.csv");
	   	    copyManager.copyIn(
	   	    		" COPY adjustment_Cb(batch_no,fk_employee, army_no, check_digit, transcription_type,fk_unit, fk_pay_code, from_date, to_Date, approval_amount, adj_cb,"
	   	    		+ " qe, record_status, pay_code, it_adjustment, approved, pay_code_str, remarks) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	     //   file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		  mesg="AGIF/TAGIF Recovery for the month of 03/2025 inserted in Adjustment CB table.";
		  return mesg;

	}
				
	public void updateMiscAgis(String qe) {
		Misc misc = new Misc();
	    misc.setKey1("AGIS REVISION");
	    misc.setValue1(qe);	
	    misc.setValue2(Utils.getCurrentQuarter());
	    try {
			misc.setDate1(DateManager.getDate("2026-04-01"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    misc.setDate2(DateManager.getToday());
	    misc.setCurrent(true);
	    miscRepo.save(misc);
	}
	
	private void setValuesForSpicRevision() {
		batch=8501;
	/*	try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2019-04-01");  // Date to be changed here after ensuring allowancerate table insertion //Bhutan Rate Revision w.e.f 01-04-2019  done in the month of 12/2023 
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2024-01-01");  // Date to be changed here after ensuring allowancerate table insertion //hutan Rate Revision w.e.f 01-04-2019  done in the month of 07/2024 
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		affectedEmpList=getSpicAffectedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	public List<Object[]> getSpicAffectedIrlas(String spicRevisionDate) {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(
			" create temp table t(emp integer, from_date date); insert into t(emp,from_date)select distinct(fk_employee), min(from_date) "
		  + " from audit_cage where effective and cancelled = false and record_status = 'V' and from_date >= '"+spicRevisionDate+"' and id in "
		  + " (select fk_audit_cage from audit_cage_allowance where fk_pay_code in (292) and amount > 0) group by fk_employee; create temp "
		  + " table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; CREATE temp TABLE empdet AS select employee.army_no, "
		  + " employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join employee inner join t1 on t1.emp=employee.id on"
		  + " unit.id = employee.fk_present_unit and fk_status_code < 5  order by unit.task,unit.unit_code,employee.army_no"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	 			
	private void setValuesForPmhaRevision() {
		batch=8501;
	/*	try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2019-04-01");  // Date to be changed here after ensuring allowancerate table insertion //Bhutan Rate Revision w.e.f 01-04-2019  done in the month of 12/2023 
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		try {
			actualAllowanceRevisionFromDate=DateManager.getDate("2024-01-01");  // Date to be changed here after ensuring allowancerate table insertion //hutan Rate Revision w.e.f 01-04-2019  done in the month of 07/2024 
		} catch (Exception e) {
			e.printStackTrace();
		}
		allowanceRevisionFromDate=actualAllowanceRevisionFromDate;
		affectedEmpList=getPmhaAffectedIrlas(getStringDate(allowanceRevisionFromDate));	
	}
	
	public List<Object[]> getPmhaAffectedIrlas(String pmhaRevisionDate) {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(
			" create temp table t(emp integer, from_date date); insert into t(emp,from_date)select distinct(fk_employee), min(from_date) "
		  + " from audit_cage where effective and cancelled = false and record_status = 'V' and from_date >= '"+pmhaRevisionDate+"' and pmha=90 group by fk_employee;  create temp "
		  + " table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; CREATE temp TABLE empdet AS select employee.army_no, "
		  + " employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join employee inner join t1 on t1.emp=employee.id on"
		  + " unit.id = employee.fk_present_unit and fk_status_code < 5 order by unit.task,unit.unit_code,employee.army_no"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	
	private void setValuesForTrgallGrantedForNccUnitsReview() {
		batch=8501; 
		affectedEmpList=getTrgallGrantedForNCCunitsPersonnel();	
	}
	
	public List<Object[]> getTrgallGrantedForNCCunitsPersonnel() {	
		System.out.println("............Training allowance TRGALL granted to NCC units Personnel review................  ");
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(" create temp table t(emp integer, from_date date); "
	 			+ " insert into t(emp,from_date)select distinct(fk_employee), min(from_date) from audit_cage where effective and cancelled = false "
	 			+ "	and record_status='V' and from_date>='2017-07-01' and id in (select fk_audit_cage from audit_cage_allowance where "
	 			+ "	fk_pay_code=293 and amount>0) group by fk_employee; insert into t(emp, from_date) select distinct(fk_employee), min(from_date) "
	 			+ "	from do2 where description='TRGALL' and column_a='GRANTED' and from_date>='2017-07-01' and  status='V' group by fk_employee; "
	 			+ "	create temp table t1 as select distinct(emp), min(from_date) as frdt from t group by emp order by emp; "
	 			+ "	CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t1.frdt, unit.task from unit inner join "
	 			+ "	employee inner join t1 on t1.emp=employee.id on unit.id = employee.fk_present_unit where unit.unit_code like 'N%' "
	 			+ "	order by unit.task,unit.unit_code,employee.army_no; "); 			 	
	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	    return empList;
	}
	
	public String generateAdjustmentCbForTagifDiffAmount() {
		System.out.println("............TAGIF Enroll Short Recovery Review................  ");
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet;");	 	
	 	jdbcTemplate.execute("create temp table empdet as select id, army_no, check_digit, enrolment_date, fk_present_unit from employee where current_record=true "
	 			+ " and regular_ta_flag='T' and ta_appointment_category='R' and enrolment_date>'2012-02-29' and fk_status_code<5  order by id"); 			 	
	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by id", new RowMapper<Object[]>() {			 
	 		@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[5];
				obj[0]=rs.getLong(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3); 
				obj[3]=rs.getDate(4);
				obj[4]=rs.getInt(5);
				return obj;
			}			 
		}); 
	 	System.out.println(".....................Affected employee numbers ............. "+empList.size());
	 	if (empList!=null && empList.size()>0) {
	 		String dir=env.getProperty("spring.application.filedir");
			File file = new File(dir+"/insertAdjTagifRecords.csv");		
			String qe = Utils.getCurrentQuarter();
			String str = "";
 			BufferedWriter writer=null;
			try {
				writer = new BufferedWriter(new FileWriter(file));
			} catch (IOException e1) {
				Utils.handleException(e1);
			}
			int batch=999, month=0, year=0, beginmonth=0, endmonth=0;
			Date enrollDate=null, recoveryToDate=null;  Calendar cal=new GregorianCalendar();
			Double agisAmount=0.0; double alreadyRecoveredAgisAmount=0, numberOfRecoveryMonths=0, firstFullMonthRecoveryAmount=0; 
	 		Calendar enrollMonthCal=new GregorianCalendar();
			List<Allowance> tagifRateList = null; Employee emp=null;
			for (Object[] empobj : empList) {
				emp=null;
 	 			System.out.println("..  emp id  .. " +empobj[0]);
	 			month=0; year=0; beginmonth=0; endmonth=0; enrollMonthCal=new GregorianCalendar(); firstFullMonthRecoveryAmount=0; tagifRateList=null; recoveryToDate=null;
			 	jdbcTemplate.execute("DROP TABLE IF EXISTS empded;");	 	
	 			jdbcTemplate.execute("create temp table empded as select id, agis_amount, qe from employee_deduction where fk_employee="+Long.parseLong(empobj[0].toString())+" order by id"); 			 	
	 			List<Object[]> edList = jdbcTemplate.query("SELECT * from empded order by id", new RowMapper<Object[]>() {			 
					@Override
					public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
						Object[] obj=new Object[3];
						obj[0]=rs.getLong(1);
						obj[1]=rs.getDouble(2);  
						obj[2]=rs.getString(3);
						return obj;
					}
	 			});
	 			if (edList!=null && edList.size()>0) { 
	 				try {
						enrollMonthCal.setTime(DateManager.getDate(empobj[3].toString()));
					} catch (Exception e) {
						Utils.handleException(e);
					}
	 				enrollDate=enrollMonthCal.getTime();
	 				beginmonth=(enrollMonthCal.get(Calendar.YEAR)*100)+(enrollMonthCal.get(Calendar.MONTH)+1);
	 				if (edList.get(0)[2].toString().trim().startsWith("02")) {
	 					if (Double.parseDouble(edList.get(0)[1].toString())>0) {
		 		 			List<Allowance> allowance = Utils.getApplicableAllowanceRates("TAGIF", enrollMonthCal.getTime(), enrollMonthCal.getTime());
		 		 			if (allowance!=null && allowance.size()>0) {
		 		 				if (allowance.get(0).getRateAmount()==Double.parseDouble(edList.get(0)[1].toString())) {
		 		 					continue;
		 		 				}
		 		 			}
	 					}
	 				}
	 				cal=enrollMonthCal;	
	 				System.out.println("...........enroll date  " +cal.getTime());
	 				System.out.println("........ recovery to date " + recoveryToDate+"  ..beginmth "+beginmonth+" .. endmonth "+endmonth);
	 				for (Object[] edobj : edList) {
	 					if (edobj[2].toString().trim().startsWith("02")
	 							&& Double.parseDouble(edobj[1].toString().trim())!=0) {
	 						cal.set(Calendar.YEAR, Integer.parseInt(edobj[2].toString().trim().substring(3)));
		 					cal.set(Calendar.MONTH, (Integer.parseInt(edobj[2].toString().trim().substring(0,2))));
		 					try {				 			
		 						recoveryToDate=DateManager.getDate(cal.get(Calendar.YEAR), 3, 31);
		 						System.out.println(".................recoverytodate setting...........");
								cal.set(Calendar.DATE, 31);	
		 					} catch (Exception e) {
								Utils.handleException(e);
							}				 					
		 					break;
		 				}
	 				}
	 				if (recoveryToDate==null) {
	 		//			if (cal.get(Calendar.YEAR)==2025) {
	 						System.out.println("..yyyyyyyyyyyyyyyyyyyyyyyyyyyyy...");
	 						try {
								recoveryToDate=DateManager.getDate(2026, 3, 31);
								cal.setTime(recoveryToDate);
			 				} catch (Exception e) {
								Utils.handleException(e);
							}
	// 					}
	 				}
 				 	month=0; year=0;
	 				month=cal.get(Calendar.MONTH);
	 				year=cal.get(Calendar.YEAR);	 		 				 
	 				endmonth=(year*100)+(month);	 		 				
	 				System.out.println("..beginmonth " +beginmonth+"  endmonth "+endmonth +"  ");
	 				System.out.println("...........cal " +cal.getTime() +"   recoverytodate "+recoveryToDate);
	 			    numberOfRecoveryMonths=DateManager.getNoOfMonths(enrollMonthCal.getTime(),recoveryToDate);
 				 	jdbcTemplate.execute("DROP TABLE IF EXISTS agistotal;");	
		 			jdbcTemplate.execute("create temp table agistotal as select sum(ed.agis_amount) FROM employee_deduction ed "
		 					+ " where ed.fk_employee="+Long.parseLong(empobj[0].toString()) + " and " 			 	
                            + " ((CAST(substr(ed.qe,4) AS int) * 100) + CAST(substr(ed.qe,0,3) AS int))>="+beginmonth +" and "
		 					+ " ((CAST(substr(ed.qe,4) AS int) * 100) + CAST(substr(ed.qe,0,3) AS int))<"+endmonth);
	 				List<Object[]>amtObjList=jdbcTemplate.query("select * from agistotal", new RowMapper<Object[]>() { 
						@Override 
						public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException { 
							Object[] obj=new Object[1]; 
							obj[0]=rs.getLong(1);  
							return obj; 
						}
	 				});
	 				if (amtObjList!=null && amtObjList.size()>0) {
	 					alreadyRecoveredAgisAmount=Double.parseDouble(amtObjList.get(0)[0].toString());
	 				}
	 				
	 				jdbcTemplate.execute("DROP TABLE IF EXISTS agisadjtotal;");	
	 				System.out.println(". sql enrolldt "+DateManager.convertUtilDtToSqlDt(enrollDate)+"  rectodt " +DateManager.convertUtilDtToSqlDt(recoveryToDate));
		 			jdbcTemplate.execute("create temp table agisadjtotal as select sum(a.approval_amount) FROM adjustment_cb a "
		 					+ " where a.fk_employee="+Long.parseLong(empobj[0].toString()) + " and a.record_status='V' and a.approved=true and a.pay_code_str='AGIS' and " 			 	
                            + " a.fk_pay_code=18 and ((a.from_date>='"+DateManager.convertUtilDtToSqlDt(enrollDate) + "' and a.from_date<='"+DateManager.convertUtilDtToSqlDt(recoveryToDate)+ "')"   
		 			 		+ " or (a.to_date >='"+DateManager.convertUtilDtToSqlDt(enrollDate) + "' and a.to_date <= '" + DateManager.convertUtilDtToSqlDt(recoveryToDate)+ "'))");
	 				List<Object[]>amtAdjList=jdbcTemplate.query("select * from agisadjtotal", new RowMapper<Object[]>() { 
						@Override 
						public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException { 
							Object[] obj=new Object[1]; 
							obj[0]=rs.getLong(1);  
							return obj; 
						}
	 				});
	 				if (amtAdjList!=null && amtAdjList.size()>0) {
	 					if (alreadyRecoveredAgisAmount!=0 && Double.parseDouble(amtAdjList.get(0)[0].toString())!=0
	 							&& alreadyRecoveredAgisAmount==Double.parseDouble(amtAdjList.get(0)[0].toString())) {
	 						; 
	 					} else {
 		 					alreadyRecoveredAgisAmount+=Double.parseDouble(amtAdjList.get(0)[0].toString());
	 					}	 			 		 						
	 				}
	 			 	jdbcTemplate.execute("DROP TABLE IF EXISTS agisFirstFullRecovery;");	
		 			jdbcTemplate.execute("create temp table agisFirstFullRecovery as select sum(ed.agis_amount) FROM employee_deduction ed "
		 					+ " where ed.fk_employee="+Long.parseLong(empobj[0].toString()) + " and "
		 					+ " ((CAST(substr(ed.qe,4) AS int) * 100) + CAST(substr(ed.qe,0,3) AS int))="+endmonth);
	 				List<Object[]>fullRecoveryMonthAmount=jdbcTemplate.query("select * from agisFirstFullRecovery", new RowMapper<Object[]>() { 
						@Override 
						public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException { 
							Object[] obj=new Object[1]; 
							obj[0]=rs.getLong(1);  
							return obj; 
						}
	 				});
	 				Calendar fullRecoveryMonthCal = cal; 
	 				if (fullRecoveryMonthAmount!=null && fullRecoveryMonthAmount.size()>0) {
	 					firstFullMonthRecoveryAmount=Double.parseDouble(fullRecoveryMonthAmount.get(0)[0].toString());	 		 					
	 					try {
 		 					if (fullRecoveryMonthCal.get(Calendar.YEAR)%4==0) {	 		 						
					 			fullRecoveryMonthCal.set(Calendar.DATE, 29);
 		 					} else {
 		 	 					fullRecoveryMonthCal.set(Calendar.DATE, 28);
 		 					}	
	 					} catch (Exception e) {
							Utils.handleException(e);
						}
	 				}			  
 		 			System.out.println(".alredy rec amt  ... " +alreadyRecoveredAgisAmount+ "  no of recovery month s "+numberOfRecoveryMonths+"  enroll date "+enrollDate);
 		 			tagifRateList = Utils.getApplicableAllowanceRates("TAGIF", fullRecoveryMonthCal.getTime(), fullRecoveryMonthCal.getTime());
 		 			System.out.println(".................tagifratelist size   "+tagifRateList.size()+"  firstfullmonthrecamt "+firstFullMonthRecoveryAmount);
 		 			if (tagifRateList!=null && tagifRateList.size()>0) {
 		 				if (firstFullMonthRecoveryAmount==tagifRateList.get(0).getRateAmount()) {
 		 					System.out.println("zzzzzzzzzzzz");
 		 					;
 		 				} else if (firstFullMonthRecoveryAmount<tagifRateList.get(0).getRateAmount()) {
 		 					alreadyRecoveredAgisAmount-=(tagifRateList.get(0).getRateAmount()-firstFullMonthRecoveryAmount);
 		 					System.out.println("......yyyyyyyyy...alreadyrecagis "+alreadyRecoveredAgisAmount+"  tagifrateamt  "+tagifRateList.get(0).getRateAmount());
 		 				} else {
 		 					alreadyRecoveredAgisAmount+=(firstFullMonthRecoveryAmount-tagifRateList.get(0).getRateAmount());
 		 					System.out.println("......xxxxxxxxxxx...alreadyrecagis "+alreadyRecoveredAgisAmount+"  tagifrateamt  "+tagifRateList.get(0).getRateAmount());
 		 				}
 		 			}
 		 		 	System.out.println("..............allowance tagifrate list size  "+tagifRateList.size());
 		 			Date allowanceNextStartDate=null;
 		 			numberOfRecoveryMonths=0; agisAmount=0.0;
 		 			tagifRateList=null;
 					tagifRateList = Utils.getApplicableAllowanceRates("TAGIF", enrollDate, recoveryToDate);	
 		 		 	System.out.println("..............allowance tagifrate list size 11111111111111  "+tagifRateList.size()+" enrol dt "+enrollDate+"  rectodt "+recoveryToDate);

 		 			for (Allowance allw : tagifRateList) {
 		 				System.out.println("...........allw amount ,,,,,, " +allw.getRateAmount()+ "  todate "+allw.getToDate());
 		 				if (allw.getToDate().equals(Utils.getLastDayOfCurrentQuarter())) {
 		 					allw.setToDate(recoveryToDate);
 		 				}
 						if (!allw.getFromDate().after(enrollDate) && allw.getToDate()!=null && !allw.getToDate().after(recoveryToDate)) {
 							numberOfRecoveryMonths=0;  allowanceNextStartDate=enrollDate;
 							numberOfRecoveryMonths=Math.ceil(DateManager.getNoOfMonths(allowanceNextStartDate, allw.getToDate()));
 							System.out.println("..........00000000000 xxxxxxxxxxxxxxxxxxxxx allowannextstartdt " +allowanceNextStartDate+"  rectodt "+recoveryToDate);
	 						agisAmount+=(allw.getRateAmount()/12)*numberOfRecoveryMonths;
	 	 					System.out.println(".........000000000000000000.number of recovery months "+numberOfRecoveryMonths+" agisamt "+agisAmount);

	 					} 
 						if (!allw.getFromDate().after(enrollDate) && (allw.getToDate()==null || (allw.getToDate()!=null && allw.getToDate().after(recoveryToDate)))) {
 							numberOfRecoveryMonths=0;  allowanceNextStartDate=enrollDate;
 							numberOfRecoveryMonths=Math.ceil(DateManager.getNoOfMonths(allw.getFromDate(), recoveryToDate));
 							System.out.println("..........00000000000 yyyyyyyyyyyyyyyallowannextstartdt " +allowanceNextStartDate+"  rectodt "+recoveryToDate);
	 						agisAmount+=(allw.getRateAmount()/12)*numberOfRecoveryMonths;
	  						System.out.println(".........000000000000000000.number of recovery months "+numberOfRecoveryMonths+" agisamt "+agisAmount);

	 					}	
 						if (allw.getFromDate().after(enrollDate) && allw.getToDate()!=null && !allw.getToDate().after(recoveryToDate)) {
 							numberOfRecoveryMonths=0;  allowanceNextStartDate=enrollDate;
 							numberOfRecoveryMonths=Math.ceil(DateManager.getNoOfMonths(allw.getFromDate(), allw.getToDate()));
 							System.out.println("..........00000000000 1111111111111allowannextstartdt " +allowanceNextStartDate+"  rectodt "+recoveryToDate);
	 						agisAmount+=(allw.getRateAmount()/12)*numberOfRecoveryMonths;
	  						System.out.println(".........000000000000000000.number of recovery months "+numberOfRecoveryMonths+" agisamt "+agisAmount);

	 					} 
 						if (allw.getFromDate().after(enrollDate) && (allw.getToDate()==null || (allw.getToDate()!=null && allw.getToDate().after(recoveryToDate)))) {
 							numberOfRecoveryMonths=0;  allowanceNextStartDate=enrollDate;
 							numberOfRecoveryMonths=Math.ceil(DateManager.getNoOfMonths(allw.getFromDate(), recoveryToDate));
 							System.out.println("..........00000000000 22222222222222allowannextstartdt " +allowanceNextStartDate+"  rectodt "+recoveryToDate);
	 						agisAmount+=(allw.getRateAmount()/12)*numberOfRecoveryMonths;
	  						System.out.println(".........000000000000000000.number of recovery months "+numberOfRecoveryMonths+" agisamt "+agisAmount);

	 					}		
 						
 					}
 					System.out.println(".......agist amount " +agisAmount);
 					agisAmount-=alreadyRecoveredAgisAmount;
					str = batch+","+empobj[0]+","+empobj[1]+","+empobj[2]+",S,+"+empobj[4]+",18,"+
					      getStringDate(enrollDate)+","+recoveryToDate+","+(agisAmount.intValue())+",AD,"+qe+",V,AGIS,false,true,AGIS,Short Recovery of TAGIF";
				 	try {
				 		if (agisAmount>0) {						 			
				 			writer.write(str);
						    writer.newLine();
						    emp=empRepo.getById(Long.parseLong(empobj[0].toString()));
						    if (emp!=null && emp.getTagifRecoveryInd()!=null && emp.getTagifRecoveryInd()<4) {
						    	emp.setTagifRecoveryInd(4);
						    	empRepo.save(emp);
						    }
				 		}								
					} catch (IOException e) {
						Utils.handleException(e);							
					}
	 				}
 	 		 	}
	 		try {
				writer.close();
				Connection con=getConnection();
				CopyManager copyManager = new CopyManager((BaseConnection) con);
				FileReader fileReader = new FileReader(dir+"/insertAdjTagifRecords.csv");
			   	copyManager.copyIn(
			   	    		" COPY adjustment_Cb(batch_no,fk_employee, army_no, check_digit, transcription_type,fk_unit, fk_pay_code, from_date, to_Date, approval_amount, adj_cb,"
			   	    		+ " qe, record_status, pay_code, it_adjustment, approved, pay_code_str, remarks) " +
			   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
			    fileReader.close();			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	 		}
	 	return "Success";
	 }

	private void writeBatchInMiscForPre2016IncrReview(String qe, Date date, Usr usr, String allowanceName) { 
		List<Integer> batchList=do2Repo.findINCRGeneratedForRevisionBatches(qe, allowanceName.toUpperCase());
		if (batchList!=null && batchList.size()>0) {
			for(Integer batch:batchList) {
				Misc m=new Misc();
				m.setKey1("Batch");
				m.setValue1(qe);
				m.setValue2("Ready");
				m.setDate1(DateManager.getToday());
				m.setCurrent(true);
				m.setIntNumber1(batch);
				miscRepo.save(m);					 
			}
		} 
	}
	
	private String generateAdhocIncrDo201012006ForRejectedIrlas() {
		if (isAtcorrDo2AlreadyGenerated(DateManager.getSixthCpcDate())) {
			return miscValue2+" Already Done !!!";
		}
	 
		jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(" create temp table t (emp integer, from_date date); insert into t(emp, from_date) select fk_employee, '2006-01-01' from do2 "
	 			+ " where description='INCR' and column_c='ADHOC' and from_date='2006-01-01' and ((status in ('I', 'P') "
	 			+ " and reason like '%AU:As on ADHOC INCR date PROMOT/MACPS Exists. ADHOC INCR not admissible%') or status='C') and fk_employee in "
	 			+ " (select id from employee where current_Record and fk_Status_code<5); create temp table t1 as select distinct(emp), min(from_date) as frdt "
	 			+ " from t group by emp order by emp; CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t1.frdt, "
	 			+ " unit.task from unit inner join employee inner join t1 on t1.emp=employee.id on unit.id = employee.fk_present_unit and fk_status_code<5  "
	 			+ " order by unit.task, unit.unit_code, employee.army_no "); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[5];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				obj[4]=rs.getInt(5);
				return obj;
			}			 
		});
	 System.out.println("........adhoc on macps/promot ....total affected effetive irlas for the pao "+empList.size());
		Incr010106 adhocincr = new Incr010106();
		String me=Utils.getCurrentQuarter();
		
		for (Object[] empobj : empList) {
			adhocincr=new Incr010106(); 
 			System.out.println("..  emp id for adhoc-macps/promot review... .. " +empobj[0]);
 			adhocincr.setArmyNo(empobj[0].toString());
 			adhocincr.setAcRepo(acRepo);
 			adhocincr.setDo2Repo(do2Repo);
 			adhocincr.setEmpRepo(empRepo);
 			adhocincr.setLpcRegisterRepo(lpcRegisterRepo);
 			adhocincr.isArmyNoEligible(empobj[0].toString().trim());
		}	
		updateMiscTable(me, DateManager.getSixthCpcDate(), usr);
		List<Do2> batchList=do2Repo.getAdhocIncrBatch(Utils.getCurrentQuarter());
		if (batchList!=null && batchList.size()>0) { 
		    int count=0, batchno=2006;
		    if (batchList.size()<101) {
		    	Misc m=new Misc();
				m.setKey1("Batch");
				m.setValue1(me);
				m.setValue2("Ready");
				m.setDate1(DateManager.getToday());
				m.setCurrent(true);
				m.setIntNumber1(batchno);
				miscRepo.save(m);	
			 	jdbcTemplate.execute(" update do2 set batch_no = "+batchno+" where description='INCR' and column_c='ADHOC' and transcription_type='S' "
			 			+ "  and status='P' and input_qe='"+me+"' and reason='System Generated Do2' and from_date='2006-01-01' and batch_no=7777 "); 	 		
		    } else {
		    	for (int i=0; i<batchList.size(); i++) {
					count++;
					if (count>100) {
						Misc m=new Misc();
						m.setKey1("Batch");
						m.setValue1(me);
						m.setValue2("Ready");
						m.setDate1(DateManager.getToday());
						m.setCurrent(true);
						m.setIntNumber1(batchno);
						miscRepo.save(m);	
						batchno++;
						count=0;
					}		
					batchList.get(i).setBatchNo(batchno);
					do2Repo.save(batchList.get(i));
				}
		    	if (count>0 && count<100) {
		    		Misc m=new Misc();
					m.setKey1("Batch");
					m.setValue1(me);
					m.setValue2("Ready");
					m.setDate1(DateManager.getToday());
					m.setCurrent(true);
					m.setIntNumber1(batchno);
					miscRepo.save(m);
		    	}
		    }							
		} 	
		return "Review Completed And Adhoc INCR DOII Generated !!!";
	}
	
	private String generateAtcorrForMacpsOnDischargeDate() {
		if (isAtcorrDo2AlreadyGenerated(DateManager.getSixthCpcDate())) {
			return miscValue2+" Already Done !!!";
		}
		batch=6901;
 		jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute("  create temp table t (emp integer, from_date date); insert into t(emp, from_date) select e.id, d.from_date from employee e join do2 d on e.id=d.fk_employee \n"
	 			+ "				join lpc_register l on d.fk_employee=l.fk_employee where d.description='MACPS' and d.status='V' and d.from_date=l.discharge_date\n"
	 			+ "				and l.ecs_generated and extract(day from e.enrolment_date)=1 and e.current_record=true order by from_date desc, e.id;\n"
	 			+ "				CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t.from_date, \n"
	 			+ "	 			 unit.task from unit inner join employee inner join t on t.emp=employee.id on unit.id = employee.fk_present_unit   \n"
	 			+ "	 			 order by t.from_date desc, employee.army_no "); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[5];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				obj[4]=rs.getInt(5);
				return obj;
			}			 
		});
	    System.out.println("........Incorrect MACPS Published/Adjusted On Discharge Date...number... "+empList.size());
	    String me = Utils.getCurrentQuarter();
		String mesg=""; 
	   	int recs=0; 
		int recordsAdded=0;		
			
	    try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;	
			BufferedWriter writer=null;		
			String dir=env.getProperty("spring.application.filedir");
			File file = new File(dir+"/InsertAtcorrDo2ForMacpsDischargeDate.csv");		
			
			String str = "";		
			try {	
				writer = new BufferedWriter(new FileWriter(file));
			} catch (Exception e) {
				Utils.handleException(e);
			}
			for (Object[] obj : empList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				effectiveRevisionDate=null;
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
	    			cd=(String) obj[1];
				}
				if (obj[2]!=null) {
	    			unitcd=(String) obj[2];
				}
				if (obj[3]!=null) {
	    			effectiveRevisionDate=(Date) obj[3];
	    			effectiveRevisionDate= DateManager.getDateEarlierByGivenDays(effectiveRevisionDate, 5);
				}				
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
					str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+columnB+",,"+effectiveRevisionDateString+",P,T,"
						+me+","+me+",true,true,false,"+usr.getId()+",System Generated Do2";	
		
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
			    	                 
			}
			writer.close();
			Connection con=getConnection();
		
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertAtcorrDo2ForMacpsDischargeDate.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, column_b, column_c, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	//      file.delete();	        
	//      updateMiscTable(me, actualAllowanceRevisionFromDate, usr);
			writeBatchInMisc(me, actualAllowanceRevisionFromDate, usr, "MacpsDischargeDateReview".toUpperCase());		
		} catch (Exception e) {
			e.printStackTrace();
		}  
		updateMiscTable(me, DateManager.getToday(), usr);  
		mesg=recordsAdded+" ATCORR DO2 Generation For "+"generateAtcorrForMacpsOnDischargeDate".toUpperCase()+" Revision Done.";
		return mesg;
	}
	
	private String generateMissingIncrementForASC_South() {
	
		if (!Utils.getPao().getPaoCode().equals("82")) {
			return "Activity specific for a particular PAO In 02/2026 !!!";
		}
		
		if (Utils.getCurrentQuarter().equals("02/2026")) {
			return "Activity only in 02/2026!!!";
		}
		
  		jdbcTemplate.execute(" DROP TABLE IF EXISTS t; ");	 	
	 	jdbcTemplate.execute(" create temp table t as select army_no from do2 where description in ('PROMOT', 'MACPS') and status='V' AND FROM_DATE='2026-01-01' AND to_date is null and \n"
	 			+ "FK_EMPLOYEE NOT IN (select fk_employee from do2 where description='INCR' AND STATUS='V' AND FROM_DATE='2026-01-01')\n"
	 			+ "AND FK_EMPLOYEE IN (select fk_employee from do2 where description='INCR' AND STATUS='V' AND FROM_DATE='2025-01-01') order by fk_employee \n"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from t", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[1];
				obj[0]=rs.getString(1); 
				return obj;
			}			 
		});
	    System.out.println("........Increment not generated IRLAs...number... "+empList.size());
	    String me = Utils.getCurrentQuarter();
		String mesg=""; 
	   	int recs=0; 
		int recordsAdded=0;		
			
	    try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;	
			Employee emp = null;
			BufferedWriter writer=null;		
			String dir=env.getProperty("spring.application.filedir");
			File file = new File(dir+"/InsertIncrDo2ForPromotion01012026.csv");		
			
			String str = "";		
			try {	
				writer = new BufferedWriter(new FileWriter(file));
			} catch (Exception e) {
				Utils.handleException(e);
			}
			effectiveRevisionDate=DateManager.getDate("2026-01-01");
			batch=7001;
			for (Object[] obj : empList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				emp=null;
				
				if (obj[0]!=null) {					
	    			armyNo=(String) obj[0];
	    			emp=Utils.getCurrentEmployee(armyNo);
	    			armyNo=emp.getArmyNo();
	    			cd=emp.getCheckDigit();
	    			unitcd=emp.getUnit().getUnitCode();
				} 
				
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
				
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
					str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",INCR,,REG,"+effectiveRevisionDateString+",P,S,"
						+me+","+me+",true,true,false,"+usr.getId()+",System Generated Do2";	
		
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;			    	                 
			}
			writer.close();
			Connection con=getConnection();
		
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertIncrDo2ForPromotion01012026.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, column_b, column_c, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	//      file.delete();	        
	//      updateMiscTable(me, actualAllowanceRevisionFromDate, usr);
 			writeBatchInMisc(me, DateManager.getDate("2026-01-01"), usr, "IncrDatePromotionIRLAs".toUpperCase());		
		} catch (Exception e) {
			e.printStackTrace();
		}  
		updateMiscTable(me, DateManager.getToday(), usr);  
		mesg=recordsAdded+" INCR DO2 Generation For Missing ASC(S) IRLAs On Promotion01012026";
		return mesg;
	}
	
	private String generateAdhocIncrDo201012006ForNEIrlas() {
		if (isAtcorrDo2AlreadyGenerated(DateManager.getSixthCpcDate())) {
			return miscValue2+" Already Done !!!";
		}
	 
		jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute(" create temp table t (emp integer, from_date date); insert into t(emp, from_date) select fk_employee, '2006-01-01' from do2 "
	 			+ " where description='INCR' and column_c='ADHOC' and from_date='2006-01-01' and ((status in ('I', 'P') "
	 			+ " and reason like '%AU:As on ADHOC INCR date PROMOT/MACPS Exists. ADHOC INCR not admissible%') or status='C') and fk_employee in "
	 			+ " (select id from employee where current_Record and fk_Status_code>4); create temp table t1 as select distinct(emp), min(from_date) as frdt "
	 			+ " from t group by emp order by emp; CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t1.frdt, "
	 			+ " unit.task from unit inner join employee inner join t1 on t1.emp=employee.id on unit.id = employee.fk_present_unit and fk_status_code>4  "
	 			+ " order by unit.task, unit.unit_code, employee.army_no "); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[5];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				obj[4]=rs.getInt(5);
				return obj;
			}			 
		});
	 System.out.println("........adhoc on macps/promot ....total affected NE irlas for the pao "+empList.size());
		Incr010106 adhocincr = new Incr010106();
		String me=Utils.getCurrentQuarter();
		
		for (Object[] empobj : empList) {
			adhocincr=new Incr010106(); 
 			System.out.println("..  emp id for adhoc-macps/promot NE IRLA review... .. " +empobj[0]);
 			adhocincr.setArmyNo(empobj[0].toString());
 			adhocincr.setAcRepo(acRepo);
 			adhocincr.setDo2Repo(do2Repo);
 			adhocincr.setEmpRepo(empRepo);
 			adhocincr.setLpcRegisterRepo(lpcRegisterRepo);
 			adhocincr.isArmyNoEligible(empobj[0].toString().trim());
		}	
		updateMiscTable(me, DateManager.getSixthCpcDate(), usr);
		List<Do2> batchList=do2Repo.getAdhocIncrBatch(Utils.getCurrentQuarter());
		if (batchList!=null && batchList.size()>0) { 
		    int count=0, batchno=200601;
		    if (batchList.size()<101) {
		    	Misc m=new Misc();
				m.setKey1("Batch");
				m.setValue1(me);
				m.setValue2("Ready");
				m.setDate1(DateManager.getToday());
				m.setCurrent(true);
				m.setIntNumber1(batchno);
				miscRepo.save(m);	
			 	jdbcTemplate.execute(" update do2 set batch_no = "+batchno+" where description='INCR' and column_c='ADHOC' and transcription_type='S' "
			 			+ "  and status='P' and input_qe='"+me+"' and reason='System Generated Do2' and from_date='2006-01-01' and batch_no=7777 "); 	 		
		    } else {
		    	for (int i=0; i<batchList.size(); i++) {
					count++;
					if (count>100) {
						Misc m=new Misc();
						m.setKey1("Batch");
						m.setValue1(me);
						m.setValue2("Ready");
						m.setDate1(DateManager.getToday());
						m.setCurrent(true);
						m.setIntNumber1(batchno);
						miscRepo.save(m);	
						batchno++;
						count=0;
					}		
					batchList.get(i).setBatchNo(batchno);
					do2Repo.save(batchList.get(i));
				}
		    	if (count>0 && count<100) {
		    		Misc m=new Misc();
					m.setKey1("Batch");
					m.setValue1(me);
					m.setValue2("Ready");
					m.setDate1(DateManager.getToday());
					m.setCurrent(true);
					m.setIntNumber1(batchno);
					miscRepo.save(m);
		    	}
		    }							
		} 	
		return "Review Completed And Adhoc INCR DOII Generated For NE IRLAs !!!";
	}
	
	
	private String generateAtcorrForExtWkCeasedDaZeroCases() {
		String mesg="";
		List<Misc>miscList=miscRepo.findByKey1AndValue2AndCurrent("ATCORR_DO2_GENERATION", "EXTWKCEASEDDAZERO_Revision");
		if (miscList.size()>0) {
			mesg="ATCORR DO2 Generation Already Done For ExtWKCeasedDAZeroRevision  ";
			return mesg; 
		}	
		jdbcTemplate.execute("DROP TABLE IF EXISTS empdet, t, t1;");	 	
	 	jdbcTemplate.execute("create temp table t(emp integer, from_date date); "
	 			+ " insert into t(emp,from_date)select distinct(fk_employee), min(from_date) from audit_cage where effective and cancelled = false "
	 			+ " and record_status='V' and da=0 and fk_do2 in (select id from do2 where description='EXTWK' and column_a='CEASED' and status='V') group by fk_employee;"
	 			+ " CREATE temp TABLE empdet AS select employee.army_no, employee.check_digit, unit.unit_code, t.from_date, unit.task from unit inner join "
	 			+ " employee inner join t on t.emp=employee.id on unit.id = employee.fk_present_unit order by unit.task,unit.unit_code,employee.army_no");
	 	List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[4];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2); 
				obj[2]=rs.getString(3);
				obj[3]=rs.getDate(4);
				return obj;
			}			 
		});
	   
	    String me = Utils.getCurrentQuarter();
		
	   	int recs=0; 
		int recordsAdded=0;		
		batch = 7000;	
	    try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;	
			BufferedWriter writer=null;		
			String dir=env.getProperty("spring.application.filedir");
			File file = new File(dir+"/InsertAtcorrDo2ForExtWkCeasedDaZeroCases.csv");		
			
			String str = "";		
			try {	
				writer = new BufferedWriter(new FileWriter(file));
			} catch (Exception e) {
				Utils.handleException(e);
			}
			for (Object[] obj : empList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				effectiveRevisionDate=null;
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
	    			cd=(String) obj[1];
				}
				if (obj[2]!=null) {
	    			unitcd=(String) obj[2];
				}
				if (obj[3]!=null) {
	    			effectiveRevisionDate=(Date) obj[3];
	    			effectiveRevisionDate= DateManager.getDateEarlierByGivenDays(effectiveRevisionDate, 5);
				}				
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
					str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+effectiveRevisionDateString+",P,T,"
						+me+","+me+",true,true,false,"+usr.getId()+",System Generated Do2, GeneratedForRectificationOfDaBecomingZeroOnExtwkCeasedDo2s";	
		
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
			    	                 
			}
			writer.close();
			Connection con=getConnection();
		
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertAtcorrDo2ForExtWkCeasedDaZeroCases.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason,remarks) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        
	        
	        
			List<Integer> batchList=null;
			try {
				batchList = do2Repo.findAtcorrDo2sGeneratedForExtWkCeasedDaZeroCases(Utils.getCurrentQuarter());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(",,,,,  batchlist size  " +batchList.size());
			if (batchList!=null && batchList.size()>0) {
		//		List<Misc> miscList=miscRepo.findAtcorrRevisionBatch(qe, batchList);
		//		if ((miscList!=null && miscList.size()==0) || miscList==null) {				 
					for(Integer batch:batchList) {
						Misc m=new Misc();
						m.setKey1("Batch");
						m.setValue1(Utils.getCurrentQuarter());
						m.setValue2("Ready");
						m.setDate1(DateManager.getToday());
						m.setCurrent(true);
						m.setIntNumber1(batch);
						miscRepo.save(m);
				//	}
				}
			} 
	        
	        
	
		} catch (Exception e) {
			e.printStackTrace();
		}  
		updateMiscTable(me, DateManager.getToday(), usr);  
		mesg=recordsAdded+" ATCORR DO2 Generation For "+"generateAtcorrForExtWkCeasedDaZeroCases".toUpperCase()+" Revision Done.";
		return mesg;
	}
	
	private String generateCeasedGrantedForTPTLonTD() {
		if (isAtcorrDo2AlreadyGenerated(DateManager.getSeventhCpcAllowanceDate())) {
			return miscValue2+" Already Done !!!";
		}	 
		
		if (1==1) 
			return "Kept On Hold!!!";
 		
		List<Object[]> empList = jdbcTemplate.query("WITH numbered AS (SELECT *, SUM (CASE WHEN description = 'RTYOJ' THEN 1 ELSE 0 END) "
			+ "    OVER (PARTITION BY fk_employee ORDER BY from_date, description) AS trip_seq FROM do2 "
			+ "    WHERE from_date >= DATE '2017-07-01' and status='V' and description in ('RTYOJ', 'RDAILY', 'RTYRJ')), "
			+ "	   trip_summary AS (SELECT fk_employee, trip_seq, "
			+ "    MIN(CASE WHEN description = 'RTYOJ' THEN from_date END) AS raw_onward, "
			+ "    MAX(CASE WHEN description = 'RTYRJ' THEN to_date END)   AS raw_return, "
			+ "	   MIN(CASE WHEN description = 'RTYOJ' THEN id END) AS onward_id, "
			+ "    MAX(CASE WHEN description = 'RTYRJ' THEN id END) AS return_id "
			+ "    FROM numbered GROUP BY fk_employee, trip_seq) "
			+ "    SELECT fk_employee, ROW_NUMBER() OVER (PARTITION BY fk_employee ORDER BY raw_onward) AS grp, "
			+ "    (DATE_TRUNC('month', raw_onward) + INTERVAL '1 month')::date AS onward_from, DATE_TRUNC('month', raw_return)::date AS return_to, "
			+ "	   onward_id, return_id FROM trip_summary WHERE (DATE_TRUNC('month', raw_return) - (DATE_TRUNC('month', raw_onward) + INTERVAL '1 month')) >= INTERVAL '1 month' "
			+ "    ORDER BY fk_employee, grp;", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[6];
				obj[0]=rs.getLong(1);
				obj[1]=rs.getLong(2); 
				obj[2]=rs.getDate(3);
				obj[3]=rs.getDate(4);
				obj[4]=rs.getLong(5);
				obj[5]=rs.getLong(6);
				return obj;
			}			 
		});
 		System.out.println("........TPTL on TD Affected List size ..... "+empList.size());
 		if (empList==null || (empList!=null && empList.size()==0)) {
 			return "No Affected IRLAs In The PAO!!!";
 		}
 		String me=Utils.getCurrentQuarter();
		BufferedWriter writer=null;		
		String armyNo=null;
		String cd=null;
		String unitcd=null;
		Date tptlCeasedDate=null, tptlRegrantDate=null;
		String ceaseQueryString=null, regrantQueryString;
		Employee emp = null;
		Do2 rtyojDo2=null, rtyrjDo2=null;
		Long tripcount=0l, rtyojId=0l, rtyrjId=0l;
		int recs=0, recordsAdded=0;
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/InsertTptlGtdCtdOnTD.csv");	
		batch=99991;	
		List<Do2> prevTptlDo2List=null;
		Do2 prevTptlGrantedDo2=null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
		} catch (IOException e1) {
			Utils.handleException(e1);
		}		
		try {
			for (Object[] obj : empList) {
				emp=null;
				armyNo=null;
				cd=null;
				unitcd=null;
				tptlCeasedDate=null;
				tptlRegrantDate=null;
 				tripcount=0l;
 				rtyojId=0l;
 				rtyrjId=0l; 
 				rtyojDo2=null;
 				rtyrjDo2=null;
 				ceaseQueryString=null;
 				regrantQueryString=null;
 				prevTptlDo2List=null;
 				prevTptlGrantedDo2=null;
				if (obj[0]!=null) {
					emp = empRepo.getById(Long.parseLong(obj[0].toString()));
				} else {
					continue;
				}
	 			if (emp!=null) {
		    		armyNo=emp.getArmyNo();
					cd=emp.getCheckDigit();
					unitcd=emp.getUnit().getUnitCode();
	 			}
	 			if (obj[1]!=null) {
	 				tripcount=Long.parseLong(obj[1].toString());
	 			}
	 			if (obj[2]!=null) {
	 				tptlCeasedDate = (Date) obj[2];
	 			}
	 			if (obj[3]!=null) {
	 				tptlRegrantDate = (Date) obj[3];
	 			}
	 			if (obj[4]!=null) {
	 				rtyojId=Long.parseLong(obj[4].toString());
	 				rtyojDo2=do2Repo.findByDo2Id(rtyojId);
	 			}
	 			if (obj[5]!=null) {
	 				rtyrjId=Long.parseLong(obj[5].toString());
	 				rtyrjDo2=do2Repo.findByDo2Id(rtyrjId);
	 			}
	 			if (recs>299) {
	 				batch=batch+1;
	 				recs=0;
				}
	 			prevTptlDo2List=do2Repo.getPreviousTptlGrantedDo2List(emp, rtyojDo2.getFromDate());
	 			if (prevTptlDo2List!=null && prevTptlDo2List.size()>0) {
	 				prevTptlGrantedDo2=prevTptlDo2List.get(0);
	 			} else {
	 				continue;
	 			}
	 			unitcd=rtyojDo2.getUnitCode();
		 		ceaseQueryString = batch+","+unitcd+","+armyNo+","+cd+",IA,"+rtyojDo2.getDo2Type()+","+rtyojDo2.getDo2No()+","+rtyojDo2.getDo2ItemNo()+","+
 									getStringDate(rtyojDo2.getDo2Date())+",0,TPTL,"+"CEASED"+",TDREVIEW,,"+getStringDate(tptlCeasedDate)+",P,S,"
 									+me+","+me+",true,true,false,"+usr.getId()+",System Generated Do2";	
		 		unitcd=null;
				writer.write(ceaseQueryString);	
				writer.newLine();	
				unitcd=rtyrjDo2.getUnitCode();
		 		regrantQueryString = batch+","+unitcd+","+armyNo+","+cd+",IA,"+rtyrjDo2.getDo2Type()+","+rtyrjDo2.getDo2No()+","+rtyrjDo2.getDo2ItemNo()+","+
							getStringDate(rtyrjDo2.getDo2Date())+","+prevTptlGrantedDo2.getRate()+","+"TPTL,GRANTED,TDREVIEW,"+prevTptlGrantedDo2.getColumnC()+","+getStringDate(tptlRegrantDate)+",P,S,"
							+me+","+me+",true,true,false,"+usr.getId()+",System Generated Do2";	
		 		writer.write(regrantQueryString);	
				writer.newLine();	
				++recs;
				recordsAdded=recordsAdded+2;
			}			
			writer.close();
			Connection con=getConnection();
 			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertTptlGtdCtdOnTD.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, rate, " +
	   	            " description, column_a, column_b, column_c, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	    //  file.delete();	    
		} catch (Exception e) {
			e.printStackTrace();
		}		 	
		updateMiscTable(me, DateManager.getSeventhCpcAllowanceDate(), usr);
		List<Do2> batchList=do2Repo.getTptlDo2OnTDReview(me);
		if (batchList!=null && batchList.size()>0) { 
		    int count=0, batchno=201799001;
		    if (batchList.size()<301) {
		    	Misc m=new Misc();
				m.setKey1("Batch");
				m.setValue1(me);
				m.setValue2("Ready");
				m.setDate1(DateManager.getToday());
				m.setCurrent(true);
				m.setIntNumber1(batchno);
				miscRepo.save(m);	
			 	jdbcTemplate.execute(" update do2 set batch_no = "+batchno+" where description='TPTL' and column_b='TDREVIEW' and transcription_type='S' "
			 			+ "  and status='P' and input_qe='"+me+"' and reason='System Generated Do2' and from_date>='2017-07-01' and batch_no>=99991 "); 	 		
		    } else {System.out.println(".......... batchlist size " +batchList.size());
		    	for (int i=0; i<batchList.size(); i++) {
					count++;
					if (count>300) {
						Misc m=new Misc();
						m.setKey1("Batch");
						m.setValue1(me);
						m.setValue2("Ready");
						m.setDate1(DateManager.getToday());
						m.setCurrent(true);
						m.setIntNumber1(batchno);
						miscRepo.save(m);	
						batchno++;
						count=0;
	 				}		
					batchList.get(i).setBatchNo(batchno);
					do2Repo.save(batchList.get(i));
				}
		    	if (count>0 && count<300) {
		    		Misc m=new Misc();
					m.setKey1("Batch");
					m.setValue1(me);
					m.setValue2("Ready");
					m.setDate1(DateManager.getToday());
					m.setCurrent(true);
					m.setIntNumber1(batchno);
					miscRepo.save(m);
 		    	}
		    }							
		} 	
		return "TPTL Review On TD Period Beyond Calendar Month For Effective IRLAs Completed !!!";
	}
	
	public String recoverAGISdiffForApr2026InDSC() { 

		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			return "Activity Is Not For Your PAO !!!";
		}
		String mesg = "";
		String dir = env.getProperty("spring.application.filedir");
		File file = new File(dir + "/insertAdjAgisRecordsDSC.csv");
		String qe = Utils.getCurrentQuarter();
		String str = ""; 
		String previousQe = "03/2026";
		BufferedWriter writer = null;
		int batch = 999;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			List<EmployeeDeduction> empdedList = empdedRepo.getAgisRecoveryDscIRLAsList(previousQe);
			Date agisStartDate = null; 			
			agisStartDate = DateManager.getDate("2026-04-01");
			Date agisEndDate = null;
			agisEndDate = DateManager.getDate("2026-04-30");
			for (EmployeeDeduction empded : empdedList) {
				Employee emp = Utils.getCurrentEmployee(empded.getEmployee().getArmyNo());
				if (emp.getRegularTaFlag().equals("T") && emp.getTaAppointmentCategory().equals("D")) {
					continue;
				}
				if (emp.getSubstantiveRank() != null
						&& emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
					continue;
				}

				if (emp.getStatusCode() != null && emp.getStatusCode().getId() > 5) {
					continue;
				}
			
				str = batch + "," + emp.getId() + "," + emp.getArmyNo() + "," + emp.getCheckDigit() + ",S,+"
						+ emp.getUnit().getId() + ",18," + getStringDate(agisStartDate) + ","
						+ getStringDate(agisEndDate) + ",410,AD," + qe
						+ ",V,AGIS,false,true,AGIS,Diff of Revised AGIS For DSC Recovered";

				writer.write(str);
				writer.newLine();
			}
			writer.close();
			Connection con = getConnection();
			// String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);

			FileReader fileReader = new FileReader(dir + "/insertAdjAgisRecordsDSC.csv");
			copyManager.copyIn(
					" COPY adjustment_Cb(batch_no, fk_employee,army_no, check_digit, transcription_type,fk_unit, fk_pay_code, from_date, to_Date, approval_amount, adj_cb,"
							+ " qe, record_status, pay_code, it_adjustment, approved, pay_code_str, remarks) "
							+ " FROM STDIN  with delimiter as ','  NULL as ''",
					fileReader);
			fileReader.close();
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mesg = "Difference Of Revised AGIS Recovery for the month of 04/2026 inserted in Adjustment CB table.";
		return mesg;
	}
	
	
	public String generateAtcorrDo2ForBcaRevision(String allowanceName) {
		String mesg=""; 
		if (isAtcorrDo2AlreadyGenerated(actualAllowanceRevisionFromDate)) {
			mesg="ATCORR DO2 Generation Already Done For "+allowanceName.toUpperCase()+" Revision - "+getStringDate(allowanceRevisionFromDate);
			return mesg; 
		} 
		
		columnB=allowanceName.trim().toUpperCase();
		miscValue2=allowanceName.trim().toUpperCase()+"_Revision";
		usr=Utils.getLoggedInUsr();
	
	   	int recs=0; 
	 	int recordsAdded=0;		
		BufferedWriter writer=null;		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/InsertAtcorrDo2ForBcaRevision.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";		
		
		
	 	try {
			String armyNo=null;
			String cd=null;
			String unitcd=null;
			Date effectiveRevisionDate=null;
			String effectiveRevisionDateString=null;
			writer = new BufferedWriter(new FileWriter(file));
			for (Object[] obj : affectedEmpList) { 
				armyNo=null;
				cd=null;
				unitcd=null;
				effectiveRevisionDate=null;
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
	    			cd=(String) obj[1];
				}
				if (obj[2]!=null) {
	    			unitcd=(String) obj[2];
				}
				if (obj[3]!=null) {
	    			effectiveRevisionDate=(Date) obj[3];
				}				
				if (recs>299) {
					batch=batch+1;
					recs=0;
				}
				effectiveRevisionDateString=getStringDate(effectiveRevisionDate);
	//			str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+columnB+",ALWREV,"+effectiveRevisionDateString+",P,T,"
	//					+qe+","+qe+",true,true,false,"+usr.getId()+",System Generated Do2";	
				str = batch+","+unitcd+","+armyNo+","+cd+",IA,0,8888,888,"+effectiveRevisionDateString+",ATCORR,"+columnB+",,"+effectiveRevisionDateString+",P,T,"
						+qe+","+qe+",true,true,false,"+usr.getId()+",System Generated Do2";	
		
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
	//		    jdbcTemplate.execute("update do2 set status='P', reason=null where description in ('PROFIX', 'ACPFIX', 'HAVFIX', 'INCR') and status in ('I', 'P')  "
	//                     + " and (accepted_qe='09/2024' or review_qe='09/2024') and from_date>'"+effectiveRevisionDate+"' and army_no='"+armyNo+"'");	                 
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);			
	   	    FileReader fileReader = new FileReader(dir+"/InsertAtcorrDo2ForBcaRevision.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2 (batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, column_b, column_c, from_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();	        
	        updateMiscTable(qe, actualAllowanceRevisionFromDate, usr);
			writeBatchInMisc(qe, actualAllowanceRevisionFromDate, usr, allowanceName.toUpperCase());		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		mesg=recordsAdded+" ATCORR DO2 Generation For "+allowanceName.toUpperCase()+" Revision Done.";
		return mesg;
	
	} 
	 
	public void setRightDoubleAllowanceAffectedIrlas() {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS t");	 	
	 	jdbcTemplate.execute("create temp table t as SELECT * FROM (SELECT id as acaid, fk_audit_cage as acid, fk_do2 as doid, fk_pay_code as pcid,  "
	 			+ " ROW_NUMBER() OVER (PARTITION BY fk_audit_cage, fk_do2, fk_pay_code  ORDER BY id) AS rn FROM audit_cage_allowance) t  "
	 			+ " WHERE rn>1 ORDER BY acaid, acid, doid, pcid; ");	 	
	 	List<Object[]> affectedEmpList = jdbcTemplate.query("SELECT * from t order by acaid", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[5];
				obj[0]=rs.getLong(1);
				obj[1]=rs.getLong(2); 
				obj[2]=rs.getLong(3);
				obj[3]=rs.getInt(4);
				obj[4]=rs.getInt(5);
				return obj;
			}			 
		}); 
	 	Long acaid=0l, acid=0l, doid=0l;
	 	int pcid=0, rn=0;	 
	 	Map<Long, Long> empacmap = new LinkedHashMap<>();  // empid, acid
 	 	System.out.println(".........affected irla size " +affectedEmpList.size());
	 	AuditCage ac=null;  Employee emp=null;
		for (Object[] obj : affectedEmpList) { 
			acaid=0L; acid=0L; doid=0L; pcid=0; rn=0;
			acaid=Long.parseLong(obj[0].toString());
			acid=Long.parseLong(obj[1].toString());
			doid=Long.parseLong(obj[2].toString());
			pcid=Integer.parseInt(obj[3].toString());
			rn=Integer.parseInt(obj[4].toString());			
			ac=null;
			ac=acRepo.findByAcId(acid);	
			System.out.println(".....acid......." +acid);
			if (ac!=null) {			
				emp=null;
				emp=empRepo.getById(ac.getEmployee().getId());
	 			if (empacmap.get(emp.getId())==null) {
					empacmap.put(emp.getId(), acid);
 				}
			}
			acaRepo.deleteById(acaid);		
		}			
	 	String armyno=null; String desc=null, fromdate=null;
		for (Map.Entry<Long, Long> entry : empacmap.entrySet()) {
	 		ac=null; emp=null; armyno=null; desc=null; fromdate=null;
	 		emp=empRepo.getById(entry.getKey());
			if (emp!=null) {					
				arrearcorr = new Arrearcorr();
				arrearcorr.setAcaRepo(acaRepo);
				arrearcorr.setAcRepo(acRepo);
				arrearcorr.setAlwRateRepo(allowanceRateRepo);
				arrearcorr.setArrRepo(arrearsRepo);
				arrearcorr.setDo2Repo(do2Repo);
				arrearcorr.setEmpRepo(empRepo);
				arrearcorr.setLpcRepo(lpcRegisterRepo);
				arrearcorr.setEdRepo(empdedRepo);
				arrearcorr.setEsdRepo(esdRepo);
				arrearcorr.setClassPayRepo(classPayRepo);
				arrearcorr.setAppjRepo(appxJRepo);
				arrearcorr.setPsRepo(psRepo);
				arrearcorr.setMatrixRepo(pmRepo);
				arrearcorr.setRrRepo(rrRepo);
				arrearcorr.setRankRepo(rankRepo);
				arrearcorr.setGcRepo(gcRepo); 
				arrearcorr.setPcRepo(pcRepo);
				arrearcorr.setScRepo(scRepo);  
				arrearcorr.setF16Repo(f16Repo);  
				arrearcorr.setFdRepo(fdRepo);  
				arrearcorr.setUnitRepo(unitRepo);  
				arrearcorr.setApptRateRepo(arRepo);
				arrearcorr.setAppjRepo(appxJRepo);
				arrearcorr.setFsRepo(fsRepo);
				arrearcorr.setRhLocationRepo(rhlocationRepo);
				arrearcorr.setBhutanSlabRepo(bsRepo);
				arrearcorr.setTioRepo(transferInOutRepo);
				arrearcorr.setLpcDepRepo(lpcDepnRepo);
				arrearcorr.setEnv(env);
				arrearcorr.setJdbcTemplate(jdbcTemplate);
				armyno=emp.getArmyNo();
				acid=0L;
				acid=entry.getValue();
				ac=null;
				ac=acRepo.getById(acid);
				if (ac!=null) {
					desc=ac.getDo2().getDescription();
					fromdate=DateManager.getStringDateWithoutSeparators(ac.getFromDate()); 
				}
				System.out.println(".........atcorrgen.... armyno "+armyno+"  desc "+desc+" fromdt "+fromdate);
				arrearcorr.setArmyNo(armyno);
				arrearcorr.setDo2Description(desc);
				arrearcorr.setDo2FromDate(fromdate);
				arrearcorr.insertDiffOfArrearsInArrears(armyno, desc, fromdate);
 			}				
 		}		 
	}
	
	
	public String firstIncrementGenerationForMissingAnosSevenCpc(String allowanceName) {
		
		String mesg=null;
		affectedEmpList=getFirstIncrementMissingIrlasSevenCpc();	
		String armyNo=null;
		Date firstIncrDate=null;
		List<Do2> firstIncrDo2List=null;
		System.out.println ( " affectedEmpList " +affectedEmpList.size());
		
	
			for (Object[] obj : affectedEmpList) { 
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
				if (obj[1]!=null) {
					firstIncrDate=(Date) obj[1];
				}
				
				Employee emp = Utils.getCurrentEmployee(armyNo);
				
				 firstIncrDo2List  = do2Repo.getValidFirstIncrDo2List(emp.getId(), firstIncrDate);
				
				System.out.println ( " first incr do2 list " +firstIncrDo2List.size());
				
				if (firstIncrDo2List!=null && firstIncrDo2List.size()==1 ) {
					;
				} else if (firstIncrDo2List==null  || firstIncrDo2List.size()==0) {
					
					IncrReview incrRev = new IncrReview();
					incrRev.setDo2Repo(do2Repo);
					incrRev.setAcRepo(acRepo);
					incrRev.setEmpRepo(empRepo);
					incrRev.setMiscRepo(miscRepo);
					incrRev.setArmyNumber(armyNo);
					incrRev.setJdbc(jdbcTemplate);

					mesg = incrRev.startReview(armyNo);
					
				}
				
				
			}	
		
		
		mesg=" INCR REVIEW DONE FOR FIRST INCR MISSING IRLAS "+allowanceName.toUpperCase();
		return mesg;
	}   
	
	
	public List<Object[]> getFirstIncrementMissingIrlasSevenCpc() {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS t;");	 	
	 		 	
	 	jdbcTemplate.execute(
			" create temp table t(army_no character varying, first_incr_due_date date); insert into t(army_no, first_incr_due_date) select  army_no,  CASE "
			+ "  WHEN EXTRACT(MONTH FROM (enrolment_date + INTERVAL '6 months')) < 7 "
			+ "  OR (EXTRACT(MONTH FROM (enrolment_date + INTERVAL '6 months')) = 7 "
			+ "  AND EXTRACT(DAY FROM (enrolment_date + INTERVAL '6 months')) = 1)"
			+ "  THEN MAKE_DATE(EXTRACT(YEAR FROM (enrolment_date + INTERVAL '6 months'))::INT, 7, 1) "
			+ "  ELSE MAKE_DATE(  CASE   WHEN EXTRACT(MONTH FROM (enrolment_date + INTERVAL '6 months')) >= 7"
			+ "  THEN EXTRACT(YEAR FROM (enrolment_date + INTERVAL '6 months'))::INT + 1 "
			+ "  ELSE EXTRACT(YEAR FROM (enrolment_date + INTERVAL '6 months'))::INT "
			+ "  END,  1, 1)   END  AS first_increment_due_date FROM employee WHERE enrolment_date >= DATE '2016-01-01' and current_record"
			+ "  and initial_mustering_date is not null and fk_status_code < 4 and regular_ta_flag='R'"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from t ", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[2];
				obj[0]=rs.getString(1);
				obj[1]=rs.getDate(2);
				return obj;
			}			 
		});
	    return empList;
	}
	
public String incrReviewSevenCpc(String allowanceName) {
		
		String mesg=null;
		affectedEmpList=getIncrReviewIrlasSevenCpc();	
		String armyNo=null;
	
		System.out.println ( " affectedEmpList " +affectedEmpList.size());
		
	
			for (Object[] obj : affectedEmpList) { 
				if (obj[0]!=null) {
	    			armyNo=(String) obj[0];
				}
					
					IncrReview incrRev = new IncrReview();
					incrRev.setDo2Repo(do2Repo);
					incrRev.setAcRepo(acRepo);
					incrRev.setEmpRepo(empRepo);
					incrRev.setArmyNumber(armyNo);
					incrRev.setMiscRepo(miscRepo);
					incrRev.setJdbc(jdbcTemplate);

					mesg = incrRev.startReview(armyNo);
					
				}
				
				
		
		
		
		mesg=" INCR REVIEW DONE FOR  IRLAS "+allowanceName.toUpperCase();
		return mesg;
	}   
	
	
	public List<Object[]>getIncrReviewIrlasSevenCpc() {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS t;");	 	
	 		 	
	 	jdbcTemplate.execute(
			" create temp table t(army_no character varying); insert into t(army_no) select  army_no"
			+ "  FROM employee WHERE enrolment_date >'2016-01-01' and fk_substantive_rank=2 and current_record"
			+ " and initial_mustering_date is not null and fk_status_code < 4 and regular_ta_flag='R'"); 	 		
		List<Object[]> empList = jdbcTemplate.query("SELECT * from t ", new RowMapper<Object[]>() {			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				Object[] obj=new Object[1];
				obj[0]=rs.getString(1);
				return obj;
			}			 
		});
	    return empList;
	}
}