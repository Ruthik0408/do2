package itsdc.dolphin.do2;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.auditcage.AuditCageBase;
import itsdc.dolphin.do2.auditcage.Do2AuditCageProcessor;
import itsdc.dolphin.models.AppendixJ;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Do2Control;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AdjustmentCbRepository;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.AppendixJRepository;
import itsdc.dolphin.repositories.AppointmentRateRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageAllowanceRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.BhutanSlabRepository;
import itsdc.dolphin.repositories.ClassPayRateRepository;
import itsdc.dolphin.repositories.Do2ControlRepository;
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
import itsdc.dolphin.repositories.PayCodeRepository;
import itsdc.dolphin.repositories.PayMatrixRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.RankRepository;
import itsdc.dolphin.repositories.ReadyReckonerRepository;
import itsdc.dolphin.repositories.RhLocationRepository;
import itsdc.dolphin.repositories.StatusCodeRepository;
import itsdc.dolphin.repositories.TaskRepository;
import itsdc.dolphin.repositories.TransferInOutRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Do2InsertionValidation {

	 
	Do2ControlRepository do2cRepo;
	 
	TaskRepository taskRepo;
	PayCodeRepository pcRepo;
	
	Do2Repository do2Repo;
	AuditCageRepository acRepo;
	AuditCageAllowanceRepository acaRepo;
	ArrearsRepository arrRepo;
	AppendixJRepository appjRepo;
	PayScaleRepository psRepo;
	EmployeeServiceDetailRepository esdRepo;
	   
	    RankRepository rankRepo;
	    GroupCategoryRepository gcRepo;
	    PayMatrixRepository matrixRepo;
	   
	    ClassPayRateRepository classPayRepo;
	    AppointmentRateRepository apptRateRepo;
	    AllowanceRateRepository alwRateRepo;
	    EmployeeRepository empRepo;
	    StatusCodeRepository scRepo;
	    
	     
	    UnitRepository unitRepo;
	    ReadyReckonerRepository rrRepo;
	    LpcRegisterRepository lpcRepo;
	    
	    LpcDeputationRepository lpcDepRepo;
	    BhutanSlabRepository bhutanSlabRepo;
	    
	    EmployeeDeductionRepository edRepo;
	    FundSummaryRepository fsRepo;
	    EmployeeEarningRepository eeRepo;
	    Form16Repository f16Repo;
	    AdjustmentCbRepository adjCbRepo;
	    FamilyDetailRepository fdRepo;
	    TransferInOutRepository tioRepo;
	    JdbcTemplate jdbcTemplate;
	    RhLocationRepository rhLocationRepo;
	    
	    
	
	public PayCodeRepository getPcRepo() {
			return pcRepo;
		}

		public void setPcRepo(PayCodeRepository pcRepo) {
			this.pcRepo = pcRepo;
		}

		public RhLocationRepository getRhLocationRepo() {
			return rhLocationRepo;
		}

		public void setRhLocationRepo(RhLocationRepository rhLocationRepo) {
			this.rhLocationRepo = rhLocationRepo;
		}

	public Do2ControlRepository getDo2cRepo() {
			return do2cRepo;
		}

		public void setDo2cRepo(Do2ControlRepository do2cRepo) {
			this.do2cRepo = do2cRepo;
		}

		public TaskRepository getTaskRepo() {
			return taskRepo;
		}

		public void setTaskRepo(TaskRepository taskRepo) {
			this.taskRepo = taskRepo;
		}

		public Do2Repository getDo2Repo() {
			return do2Repo;
		}

		public void setDo2Repo(Do2Repository do2Repo) {
			this.do2Repo = do2Repo;
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

		public ArrearsRepository getArrRepo() {
			return arrRepo;
		}

		public void setArrRepo(ArrearsRepository arrRepo) {
			this.arrRepo = arrRepo;
		}

		public AppendixJRepository getAppjRepo() {
			return appjRepo;
		}

		public void setAppjRepo(AppendixJRepository appjRepo) {
			this.appjRepo = appjRepo;
		}

		public PayScaleRepository getPsRepo() {
			return psRepo;
		}

		public void setPsRepo(PayScaleRepository psRepo) {
			this.psRepo = psRepo;
		}

		public EmployeeServiceDetailRepository getEsdRepo() {
			return esdRepo;
		}

		public void setEsdRepo(EmployeeServiceDetailRepository esdRepo) {
			this.esdRepo = esdRepo;
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

		public PayMatrixRepository getMatrixRepo() {
			return matrixRepo;
		}

		public void setMatrixRepo(PayMatrixRepository matrixRepo) {
			this.matrixRepo = matrixRepo;
		}

		public ClassPayRateRepository getClassPayRepo() {
			return classPayRepo;
		}

		public void setClassPayRepo(ClassPayRateRepository classPayRepo) {
			this.classPayRepo = classPayRepo;
		}

		public AppointmentRateRepository getApptRateRepo() {
			return apptRateRepo;
		}

		public void setApptRateRepo(AppointmentRateRepository apptRateRepo) {
			this.apptRateRepo = apptRateRepo;
		}

		public AllowanceRateRepository getAlwRateRepo() {
			return alwRateRepo;
		}

		public void setAlwRateRepo(AllowanceRateRepository alwRateRepo) {
			this.alwRateRepo = alwRateRepo;
		}

		public EmployeeRepository getEmpRepo() {
			return empRepo;
		}

		public void setEmpRepo(EmployeeRepository empRepo) {
			this.empRepo = empRepo;
		}

		public StatusCodeRepository getScRepo() {
			return scRepo;
		}

		public void setScRepo(StatusCodeRepository scRepo) {
			this.scRepo = scRepo;
		}

		public UnitRepository getUnitRepo() {
			return unitRepo;
		}

		public void setUnitRepo(UnitRepository unitRepo) {
			this.unitRepo = unitRepo;
		}

		public ReadyReckonerRepository getRrRepo() {
			return rrRepo;
		}

		public void setRrRepo(ReadyReckonerRepository rrRepo) {
			this.rrRepo = rrRepo;
		}

		public LpcRegisterRepository getLpcRepo() {
			return lpcRepo;
		}

		public void setLpcRepo(LpcRegisterRepository lpcRepo) {
			this.lpcRepo = lpcRepo;
		}

		public LpcDeputationRepository getLpcDepRepo() {
			return lpcDepRepo;
		}

		public void setLpcDepRepo(LpcDeputationRepository lpcDepRepo) {
			this.lpcDepRepo = lpcDepRepo;
		}

		public BhutanSlabRepository getBhutanSlabRepo() {
			return bhutanSlabRepo;
		}

		public void setBhutanSlabRepo(BhutanSlabRepository bhutanSlabRepo) {
			this.bhutanSlabRepo = bhutanSlabRepo;
		}

		public EmployeeDeductionRepository getEdRepo() {
			return edRepo;
		}

		public void setEdRepo(EmployeeDeductionRepository edRepo) {
			this.edRepo = edRepo;
		}

		public FundSummaryRepository getFsRepo() {
			return fsRepo;
		}

		public void setFsRepo(FundSummaryRepository fsRepo) {
			this.fsRepo = fsRepo;
		}

		public EmployeeEarningRepository getEeRepo() {
			return eeRepo;
		}

		public void setEeRepo(EmployeeEarningRepository eeRepo) {
			this.eeRepo = eeRepo;
		}

		public Form16Repository getF16Repo() {
			return f16Repo;
		}

		public void setF16Repo(Form16Repository f16Repo) {
			this.f16Repo = f16Repo;
		}

		public AdjustmentCbRepository getAdjCbRepo() {
			return adjCbRepo;
		}

		public void setAdjCbRepo(AdjustmentCbRepository adjCbRepo) {
			this.adjCbRepo = adjCbRepo;
		}

		public FamilyDetailRepository getFdRepo() {
			return fdRepo;
		}

		public void setFdRepo(FamilyDetailRepository fdRepo) {
			this.fdRepo = fdRepo;
		}

		public TransferInOutRepository getTioRepo() {
			return tioRepo;
		}

		public void setTioRepo(TransferInOutRepository tioRepo) {
			this.tioRepo = tioRepo;
		}

		public JdbcTemplate getJdbcTemplate() {
			return jdbcTemplate;
		}

		public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

	public static List<String> exemptedDescList = Arrays.asList("ATCORR", "CANCEL", "REFIXP", "BUNCH");
	
	public String isValid(Do2 do2,Usr usr) {
		String valid="valid";
		boolean validity=true;
		StringBuffer sb=new StringBuffer();
		List<Integer> taskList=taskRepo.findTaskForAUser(usr.getId());
		int level=usr.getDesignation().getLevel();
		Do2AuditCageProcessor do2Processor;
		
		if(do2.getEmployee()==null) {
			validity=false;
			sb.append("Employee not selected.");
		}else {
			if(do2.getEmployee().getUnit()==null) {
				validity=false;
				sb.append("Unit not defined for Employee.");
			}else {
				if(taskList.contains(do2.getEmployee().getUnit().getTask_1())) {
					//valid task
				}else if(level<30) {
					if(do2.getEmployee()!=null && do2.getEmployee().getStatusCode()!=null && do2.getEmployee().getStatusCode().getId()>4) {
						;
					}else {
					validity=false;
					sb.append("Not authorised task holder.");
					}
				}
			}
		}
		
		if(validity && do2.getDescription()!=null && (do2.getDescription().toUpperCase().equals("CANCEL") || do2.getDescription().toUpperCase().equals("ATCORR"))) {
			
			return valid;
		}
//		Do2GenericProcessor genericProcessor = new Do2GenericProcessor(do2);
//
//		genericProcessor.setDo2Repo(do2Repo);
//		genericProcessor.setEmpRepo(empRepo);
//		genericProcessor.setUnitRepo(unitRepo);
//		genericProcessor.setAppjRepo(appjRepo);
//		genericProcessor.setLpcRegisterRepo(lpcRepo);
//		genericProcessor.setGcRepo(gcRepo);
//		genericProcessor.setAlwRateRepo(alwRateRepo);
//
//	System.out.println("................. going to generic ................");
//		if (!genericProcessor.isValidDo2()) {				
//			validity=false;
//			sb.append(do2.getReason());
//		}
		
		if(validity) {
			try {
			System.out.println("------------audit cage-----------------------:" + isAuditCage(do2, do2.getDescription()));
				if(isAuditCage(do2,do2.getDescription())){
				do2Processor = (Do2AuditCageProcessor) Class.forName(
						"itsdc.dolphin.do2.auditcage."
								+ Utils.getCamelCase(do2.getDescription())).getDeclaredConstructor()
						.newInstance();
				
				((AuditCageBase) do2Processor).setDo2Repo(do2Repo);
				//((PromotBase) do2Processor).setDo2Repo(do2Repo);
				((AuditCageBase) do2Processor).setPsRepo(psRepo);
				((AuditCageBase) do2Processor).setAcRepo(acRepo);
				((AuditCageBase) do2Processor).setEmpRepo(empRepo);
				((AuditCageBase) do2Processor).setAcaRepo(acaRepo);
				((AuditCageBase) do2Processor).setNewDo2(do2);
				
				((AuditCageBase) do2Processor).setEsdRepo(esdRepo);
				((AuditCageBase) do2Processor).setRankRepo(rankRepo);
				((AuditCageBase) do2Processor).setGcRepo(gcRepo);
				((AuditCageBase) do2Processor).setMatrixRepo(matrixRepo);
				((AuditCageBase) do2Processor).setClassPayRepo(classPayRepo);
				((AuditCageBase) do2Processor).setApptRateRepo(apptRateRepo);
				((AuditCageBase) do2Processor).setAlwRateRepo(alwRateRepo);
				((AuditCageBase) do2Processor).setScRepo(scRepo);
				((AuditCageBase) do2Processor).setUnitRepo(unitRepo);
				((AuditCageBase) do2Processor).setRrRepo(rrRepo);
				((AuditCageBase) do2Processor).setLpcRepo(lpcRepo);
				((AuditCageBase) do2Processor).setArrRepo(arrRepo);
				((AuditCageBase) do2Processor).setLpcDepRepo(lpcDepRepo);
				((AuditCageBase) do2Processor).setBhutanSlabRepo(bhutanSlabRepo);
				((AuditCageBase) do2Processor).setEdRepo(edRepo);
				((AuditCageBase) do2Processor).setFsRepo(fsRepo);
				((AuditCageBase) do2Processor).setEeRepo(eeRepo);
				((AuditCageBase) do2Processor).setF16Repo(f16Repo);
				((AuditCageBase) do2Processor).setAdjCbRepo(adjCbRepo);
				((AuditCageBase) do2Processor).setFdRepo(fdRepo);
				((AuditCageBase) do2Processor).setTioRepo(tioRepo);
				((AuditCageBase) do2Processor).setJdbcTemplate(jdbcTemplate);
				((AuditCageBase) do2Processor).setRhLocationRepo(rhLocationRepo);
				
				if (!do2Processor.isValid(do2)) {
					System.out.println("----------------------"+do2.getReason());
					validity=false;
					sb.append(do2.getReason());
				}
			}else {
				Do2AccountingCageProcessor acProcessor = AccountingCageHelper
						.getDo2SpecificAccountingCageProcessor(do2.getDescription());
				
				((AccountingCageBase) acProcessor).setAcRepo(acRepo);
				((AccountingCageBase) acProcessor).setDo2Repo(do2Repo);
				((AccountingCageBase) acProcessor).setGcRepo(gcRepo);
				((AccountingCageBase) acProcessor).setEmpRepo(empRepo);
				((AccountingCageBase) acProcessor).setFdRepo(fdRepo);
				((AccountingCageBase) acProcessor).setAdjCbRepo(adjCbRepo);
				((AccountingCageBase) acProcessor).setEsdRepo(esdRepo);
				((AccountingCageBase) acProcessor).setF16Repo(f16Repo);
				((AccountingCageBase) acProcessor).setUnitRepo(unitRepo);
				((AccountingCageBase) acProcessor).setPsRepo(psRepo);
				((AccountingCageBase) acProcessor).setArrRepo(arrRepo);
				((AccountingCageBase) acProcessor).setPcRepo(pcRepo);
				((AccountingCageBase) acProcessor).setLpcRepo(lpcRepo);
				System.out.println("--------------------in accountttttttttttttttttttt------------------");
				if(!acProcessor.isValid(do2)) {
					validity=false;
					sb.append(do2.getReason());
				}
			}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		if(usr!=null && usr.getDesignation().getLevel()>30) {
		String fradulentMesg=notFradulentDo2(do2);
		if(!fradulentMesg.startsWith("NA")) {
			validity=false;
			sb.append(fradulentMesg);
		}
		}
		
		if(!validity) {
			valid=sb.toString();
		}
			
		System.out.println("valid from class "+valid);
		
		return valid;
	}
	
	public String notFradulentDo2(Do2 do2) {
		String fraudMessage = "NA";
		if(!exemptedDescList.contains(do2.getDescription())) {
			try {
				if(do2.getDo2Date().after(DateManager.getDate("2013-11-01"))) {
					List<Do2Control> dcList =do2cRepo.findByDo2TypeDo2NoYearForManualInsertion(do2.getDo2Type(), do2.getDo2No(), do2.getUnitCode(),getYearFromDate(do2.getDo2Date())) ;
							
							 
					if (dcList != null && dcList.size() > 0) {
						if (Integer.parseInt(do2.getDo2ItemNo()) > dcList.get(0).getTotalItems()) {
							fraudMessage = "Inserted a DO-II with Item No. beyond the "
											+ "Total No. of Items published";
						} else if (Integer.parseInt(do2.getDo2ItemNo()) < dcList.get(0).getTotalItems()) {
							
							List<Do2> do2List =do2Repo.checkDuplicateForManualInsertion(do2.getDo2Type(), do2.getDo2No(), do2.getUnitCode(),getYearFromDate(do2.getDo2Date()),do2.getDo2ItemNo(), do2.getId());
							
								 
							if (do2List != null && do2List.size() > 0) {
								fraudMessage = "Inserted a DO-II " + Utils.getReferenceDo2No(do2)
												+ " against existing DO-II item in the table";
							} else {
								fraudMessage = "Inserted a DO-II " + Utils.getReferenceDo2No(do2)
											+ " against non-available DO-II Item No. in the soft copy.";
							}
						}
					} else {
						fraudMessage = "Inserted a DO-II " + Utils.getReferenceDo2No(do2)
										+ " which is not imported into Do2Control.";
					}
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}
		}
		return fraudMessage;
	}
	
	static public int getYearFromDate(Date date) {
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		return cal.get(Calendar.YEAR);
	}
	
	 private boolean isAuditCage(Do2 do2,String do2Description) {
			if (do2Description!=null && do2Description.equals("BUNCH")) {
				return true;
			}

			if (do2Description!=null && do2Description.equals("REJEOL")) {
				return true;
			}

			
			if (Do2Manager.getSystemGeneratedDescription().contains(do2Description) || 
					Do2Manager.getSystemGeneratedNeDescription().contains(do2Description)) {
				if (do2Description.equals("PLBON") || do2Description.equals("NEBON") || do2Description.equals("NEENC")) {
					//System.out.println("In Do2Home.isAudtiCage BON test");
					do2.setAuditCage(false);
					return false;
				} else {
					do2.setAuditCage(true);
					//System.out.println("In Do2Home.isAudtiCage else");
					//System.out.println("Do2Description in isAuditCage is :" +do2Description);
					return true;
				}
			}
			
			/*if (Do2Manager.getSystemGeneratedDescription().contains(do2Description)) {
				if (do2Description.equals("PLBON")) {
					getInstance().setAuditCage(false);
					return false;
				} else {
					getInstance().setAuditCage(true);
					return true;
				}
			}*/
			List<AppendixJ> appjList =appjRepo.findByDescription(do2Description); 
					 
			if (appjList.size() > 0) {
				if (appjList.get(0).getAuditAccountCage().equals("AT")) {
					do2.setAuditCage(true);
				} else {
					do2.setAuditCage(false);
				}
			}
			return appjList.get(0).getAuditAccountCage().equals("AT");
		}

}
