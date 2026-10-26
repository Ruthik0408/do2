
package itsdc.dolphin.do2;
 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeDeductionRepository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.LpcRegisterRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class NeBonDo2Generation implements java.io.Serializable {

    
	 
	JdbcTemplate jdbcTemplate;
	
	 
	Environment env;
	
	 
	EmployeeRepository empRepo;
	 
	 
	MiscRepository miscRepo;
	
	 
	Do2Repository do2Repo;
	 	
	AllowanceRateRepository allowanceRateRepo;
	LpcRegisterRepository lpcRepo;
	
	EmployeeDeductionRepository edRepo;

	Usr usr;
	
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public EmployeeDeductionRepository getEdRepo() {
		return edRepo;
	}

	public void setEdRepo(EmployeeDeductionRepository edRepo) {
		this.edRepo = edRepo;
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
	
	
	public LpcRegisterRepository getLpcRepo() {
		return lpcRepo;
	}

	public void setLpcRepo(LpcRegisterRepository lpcRepo) {
		this.lpcRepo = lpcRepo;
	}
	
	EmployeeDeductionRepository empDedRepo;

	public EmployeeDeductionRepository getEmpDedRepo() {
		return empDedRepo;
	}

	public void setEmpDedRepo(EmployeeDeductionRepository empDedRepo) {
		this.empDedRepo = empDedRepo;
	}
	

	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");

	public String generatePlbonDo2(){
		 Usr usr=Utils.getLoggedInUsr();
		System.out.println ( "  AAAAAAA  BBBBBB  CCCCCC ");
		String mesg="";
		Date effectiveDate = null;
		Date plbFromDate = null;
		Date plbToDate = null;
		Date plbEffectiveDate = null;
		Date previousDaDate = null;
		Date do2Date = null;		
	   	int recs   = 0;
	   	int recordsRead  = 0;
	   	int recordsAdded  = 0;
	   	int taDeptCases  = 0;
	   	int neCases  = 0;
	   	int recruits  = 0;
	   	int thisMonthFsCases  = 0;
		int batch  = 9000; //starting batch number of plbon Dos2
		int num = 8000; //starting batch number of plbon Dos2
		BufferedWriter writer=null;
		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertneplbondos2.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";
		Date dischargeDate=null;
		 List<EmployeeDeduction> edList=null;
		 List<Do2> plbonDo2List = null;
		try {	
			
			 plbFromDate = DateManager.getDate(Constants.PLBON_FROM_DATE);
			 plbToDate = DateManager.getDate(Constants.PLBON_TO_DATE);
			 plbEffectiveDate = DateManager.getDate(Constants.NE_PLBON_EFFECTIVE_DATE);
			 
			 
			 
			do2Date = DateManager.getToday();
			writer = new BufferedWriter(new FileWriter(file));
			if (isNePlbonDo2AlreadyGenerated(plbFromDate,plbToDate)){
				mesg="NE PLBON  DO2 ALREADY GENERATED";
				return mesg; 
			}
		//	insertPlbonRate(plbFromDate,plbToDate);
			
			 List<LpcRegister>lpcList= lpcRepo.findNeDaEmpList(plbEffectiveDate, Utils.getLastDayOfPreviousQuarter());
		
			 for (LpcRegister lpcemp : lpcList){
	        		Employee emp = lpcemp.getEmployee();
	        		dischargeDate = lpcemp.getDischargeDate();
	        		Long empId = emp.getId();
	        		System.out.println ( " employee "  +empId   +" army number "+lpcemp.getArmyNo()   +" lpcemp " +lpcemp.getEmployee());
	        		
	        		 edList = empDedRepo.findNeEdIdList(empId, qe);
			
	        		 plbonDo2List = do2Repo.findDaOrNeDaDo2(empId, plbFromDate, "PLBON", "NEBON");
	        		 
	        		 if (plbonDo2List.size()==0) {
	        				str = "";
	        				++recordsRead;
	        				effectiveDate = plbFromDate;
	        				if (emp.getRegularTaFlag().equals('T') && (emp.getTaAppointmentCategory().equals('D') || emp.getTaAppointmentCategory().equals('O'))){
	        					++taDeptCases;
	        					continue;
	        				} else if (emp.getSubstantiveRank() != null && emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
	        					++recruits;
	        					continue;
	        				} else if (emp.getStatusCode() != null && emp.getStatusCode().getId() > 5 && emp.getFsDueDate() != null && emp.getFsDueDate().before(effectiveDate)){
	        					++neCases;
	        					continue;
	        				} 
	        				
	        				if (Utils.getPao().getPaoCode().equals("94")){
	        			    	if (emp.getEnrolmentDate() !=null && emp.getEnrolmentDate().after(effectiveDate)){
	        			    		effectiveDate = emp.getEnrolmentDate();
	        			    	}
	        			    } else {
	        			    	if (emp.getEnrolmentDate() !=null && emp.getEnrolmentDate().after(effectiveDate)){
	        			    		if (emp.getGroupCategory()!=null && emp.getGroupCategory().getSoldierCategory().equals("RT")){
	        			    		    effectiveDate = emp.getEnrolmentDate();
	        			    		}
	        			    	}
	        			    }
	        				if (recs > 999){
	        					batch = batch + 1;
	        					recs = 0;
	        				}
	        				str = batch+","+emp.getUnit().getUnitCode()+","+emp.getArmyNo()+","+emp.getCheckDigit()+",IA,0,5555,555,"+
	        				      getStringDate(do2Date)+",NEBON,"+getStringDate(plbFromDate)+","+getStringDate(plbToDate)+",P,S,"+qe+","+qe+",false,true,false,"+
	        					  usr.getId()+",System Generated Do2";	
	        				writer.write(str);	
	        			    writer.newLine();			
	        			    ++recs;
	        			    ++recordsAdded;
	        		 }
	        		 
		
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
	   	    FileReader fileReader = new FileReader(dir+"/insertneplbondos2.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2(batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, from_date, to_date,status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();
	        
	        updateMiscTable(Constants.PLBON_FINANCIAL_YEAR,qe,plbFromDate,plbToDate);
			writeBatchInMisc(qe,plbFromDate,plbToDate);
	        
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		  mesg="NEBON Do2 Generation Done.";
		  return mesg;
	}
	
	public void insertPlbonRate(Date plbonFromDate, Date plbonToDate){
		
		List<AllowanceRate> allowance = allowanceRateRepo.findByAlwNameFromDate("PLBON",plbonFromDate, plbonToDate);
		
		if (allowance.size()==0) {
			
			AllowanceRate allowanceRate = new AllowanceRate();
			 allowanceRate.setAllowanceName("PLBON");
			 allowanceRate.setFromDate(plbonFromDate);
			 allowanceRate.setToDate(plbonToDate);
			 allowanceRate.setRatePercentage(Constants.PLBON_NO_OF_DAYS);
			 allowanceRate.setRateAmount(Constants.PLBON_MAX_EMOULMENTS);
		//     allowanceRate.setCreatedAt(DateManager.getToday());
			 allowanceRateRepo.save(allowanceRate);
		}
		
		List<Misc> miscList =miscRepo.findAllByKey1AndCurrent("Bulk Processor", true);
			if (miscList.size() > 0) {
				miscList.get(0).setValue1("Busy");
			}
	}
	
	public boolean isNePlbonDo2AlreadyGenerated(Date plbonFromDate, Date plbonToDate) {
		List<Misc> miscList =miscRepo.findByKey1AndValue1AndDate1AndDate2AndCurrent("NE_PLBON_GENERATION", Constants.PLBON_FINANCIAL_YEAR, 
 			   plbonFromDate, plbonToDate);
	
		
		return miscList.size() >0;
	
	}
	
	
	

	
	public String getStringDate(Date date){
		String strdate=null;
		GregorianCalendar gc=new GregorianCalendar();
		gc.setTime(date);
		String mm = null,dd=null;
		
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return con;
	}
	
	
	
	 public void updateMiscTable(String plbonYear, String qe, Date frDate, Date toDate){
		 Usr usr=Utils.getLoggedInUsr();
		 Misc misc = new Misc();
	    	misc.setKey1("NE_PLBON_GENERATION");
	    	misc.setValue1(plbonYear);	  
	    	misc.setValue2(qe);
	    	misc.setDate1(frDate);
	    	misc.setDate2(toDate);
	    	misc.setUsr(usr);
	    	misc.setCurrent(true);
	    	miscRepo.save(misc);
	 }
	 
	 
		private void writeBatchInMisc(String qe,Date frDate, Date toDate) {
			// TODO Auto-generated method stub
			List<Misc> miscList=miscRepo.findNePlbonBatch(qe);
			if((miscList!=null && miscList.size()==0) || miscList==null) {
				List<Integer> batchList=do2Repo.findNePlbonGeneratedBatches(frDate, toDate,qe);
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
		}
		
		// the following method to generate missing NEBON do2 for the year 2022-2023, 2023-2024, 2024-2025 to the individuals
		// who retired on 30-09-2022, 30-09-2023, 20-09-2024 to adjust PLBON in prorata basis which is missed earlier.
public String generatePlbonDo2Sep2022Sep2023Sep2024(){
	 Usr usr=Utils.getLoggedInUsr();
	System.out.println ( "  AAAAAAA  BBBBBB  CCCCCC ");
	String mesg="";
	Date effectiveDate = null;
	Date plbFromDate = null;
	Date plbToDate = null;
	Date plbEffectiveDate = null;
	Date previousDaDate = null;
	Date do2Date = null;		
   	int recs   = 0;
   	int recordsRead  = 0;
   	int recordsAdded  = 0;
   	int taDeptCases  = 0;
   	int neCases  = 0;
   	int recruits  = 0;
   	int thisMonthFsCases  = 0;
	int batch  = 9000; //starting batch number of plbon Dos2
	int num = 8000; //starting batch number of plbon Dos2
	BufferedWriter writer=null;
	
	String dir=env.getProperty("spring.application.filedir");
	File file = new File(dir+"/insertneplbondos2.csv");		
	String qe = Utils.getCurrentQuarter();
	String str = "";
	Date dischargeDate=null;
	 List<EmployeeDeduction> edList=null;
	 List<Do2> plbonDo2List = null;
	try {	
		
	
		
		 
			
		do2Date = DateManager.getToday();
		writer = new BufferedWriter(new FileWriter(file));
	//	if (isNePlbonDo2AlreadyGenerated(plbFromDate,plbToDate)){
	//		mesg="NE PLBON  DO2 ALREADY GENERATED";
	//		return mesg; 
	//	}
	//	insertPlbonRate(plbFromDate,plbToDate);
		
		 List<LpcRegister>lpcList= lpcRepo.findNeBonSepEmpList();
	
		 for (LpcRegister lpcemp : lpcList){
        		Employee emp = lpcemp.getEmployee();
        		openNeIrla(emp);
        		dischargeDate = lpcemp.getDischargeDate();
        		System.out.println ( " discharege date " +dischargeDate);
        		String dischdate = DateManager.getStringDateWithSeparators(dischargeDate);
        		System.out.println ( " disch  date " +dischdate);
        		
        		if (dischdate.equals("30/09/2022")) {
        			System.out.println ( " discharege date 11 " +dischargeDate);
        			 plbFromDate = DateManager.getDate("2022-04-01");
    				 plbToDate = DateManager.getDate("2022-09-30");
    				 System.out.println ( " PLBON ROM DATE 11 " +plbFromDate);
    				 System.out.println ( " PLBON TO DATE 11 " +plbToDate);
    			}
        		
        		if (dischdate.equals("30/09/2023")) {
        			System.out.println ( " discharege date 22 " +dischargeDate);
        			 plbFromDate = DateManager.getDate("2023-04-01");
    				 plbToDate = DateManager.getDate("2023-09-30");
    				 System.out.println ( " PLBON ROM DATE 22 " +plbFromDate);
    				 System.out.println ( " PLBON TO DATE 22 " +plbToDate);
    			}
        		
        		if (dischdate.equals("30/09/2024")) {
        			System.out.println ( " discharege date 33" +dischargeDate);
        			 plbFromDate = DateManager.getDate("2024-04-01");
    				 plbToDate = DateManager.getDate("2024-09-30");
    				 System.out.println ( " PLBON ROM DATE 33 " +plbFromDate);
    				 System.out.println ( " PLBON TO DATE 33 " +plbToDate);
    			}
        		
        		Long empId = emp.getId();
        		System.out.println ( " employee "  +empId   +" army number "+lpcemp.getArmyNo()   +" lpcemp " +lpcemp.getEmployee());
        		
        		 edList = empDedRepo.findNeEdIdList(empId, qe);
		
        		 plbonDo2List = do2Repo.findDaOrNeDaDo2(empId, plbFromDate, "PLBON", "NEBON");
        		 
        		 if (plbonDo2List.size()==0) {
        				str = "";
        				++recordsRead;
        				effectiveDate = plbFromDate;
        				if (emp.getRegularTaFlag().equals('T') && (emp.getTaAppointmentCategory().equals('D') || emp.getTaAppointmentCategory().equals('O'))){
        					++taDeptCases;
        					continue;
        				} else if (emp.getSubstantiveRank() != null && emp.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)){
        					++recruits;
        					continue;
        				} else if (emp.getStatusCode() != null && emp.getStatusCode().getId() > 5 && emp.getFsDueDate() != null && emp.getFsDueDate().before(effectiveDate)){
        					++neCases;
        					continue;
        				} 
        				
        				if (Utils.getPao().getPaoCode().equals("94")){
        			    	if (emp.getEnrolmentDate() !=null && emp.getEnrolmentDate().after(effectiveDate)){
        			    		effectiveDate = emp.getEnrolmentDate();
        			    	}
        			    } else {
        			    	if (emp.getEnrolmentDate() !=null && emp.getEnrolmentDate().after(effectiveDate)){
        			    		if (emp.getGroupCategory()!=null && emp.getGroupCategory().getSoldierCategory().equals("RT")){
        			    		    effectiveDate = emp.getEnrolmentDate();
        			    		}
        			    	}
        			    }
        				if (recs > 999){
        					batch = batch + 1;
        					recs = 0;
        				}
        				str = batch+","+emp.getUnit().getUnitCode()+","+emp.getArmyNo()+","+emp.getCheckDigit()+",IA,0,5555,555,"+
        				      getStringDate(do2Date)+",NEBON,"+getStringDate(plbFromDate)+","+getStringDate(plbToDate)+",P,S,"+qe+","+qe+",false,true,false,"+
        					  usr.getId()+",System Generated Do2";	
        				writer.write(str);	
        			    writer.newLine();			
        			    ++recs;
        			    ++recordsAdded;
        		 }
        		 
	
		}
		writer.close();
		Connection con=getConnection();
	//	String dir=env.getProperty("spring.application.filedir");
		CopyManager copyManager = new CopyManager((BaseConnection) con);
		
   	    FileReader fileReader = new FileReader(dir+"/insertneplbondos2.csv");
   	    copyManager.copyIn(
   	    		" COPY do2(batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
   	            " description, from_date, to_date,status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
        fileReader.close();
        file.delete();
        
//        updateMiscTable(Constants.PLBON_FINANCIAL_YEAR,qe,plbFromDate,plbToDate);
		writeBatchInMisc(qe,plbFromDate,plbToDate);
        

	} catch (Exception e) {
		e.printStackTrace();
	
	}	 
	  mesg="NEBON Do2 Generation Done.";
	  return mesg;
}
		


public String openNeIrla(Employee emp) {
		String mesg="";
	List<EmployeeDeduction> edList = empDedRepo.findByEmpIdAndQe(emp.getId(), Utils.getCurrentQuarter());
	if (edList.size()>0) {
		return mesg;
	}

	
			EmployeeDeduction ed = new EmployeeDeduction();
			ed.setAcrTotalAmount(0.0);
			ed.setAdjustmentTotalAmountCr(0.0);
			ed.setAdjustmentTotalAmountDb(0.0);
			ed.setAgisAmount(0.0);
			ed.setBhutanTotalAmount(0.0);
			ed.setBonusOnCreditBalance(0.0);
			ed.setCbTotalAmountCr(0.0);
			ed.setCbTotalAmountDb(0.0);
			ed.setClosed(false);
			ed.setClosingBalance(0.0);
			ed.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			ed.setEmployee(emp);
			ed.setEmployeeType("P");
			ed.setFamoTotalAmount(0.0);
			ed.setFundSubscriptionRefund(0.0);
			ed.setFundSubscriptionRefund(0.0);
			ed.setIncomeTaxEc(0.0);
			ed.setLoanRecoveryAmount(0.0);
			ed.setMedicalClaimTotalAmount(0.0);
			ed.setNePaidAmount(0.0);
			ed.setOpeningBalance(0.0);
			ed.setPliAmount(0.0);
			ed.setQe(Utils.getCurrentQuarter());
			ed.setQuarterPay(0.0);
			ed.setTotalCredits(0.0);
			ed.setTotalDebits(0.0);
			ed.setTransferInAmount(0.0);
			ed.setTransferOutAmount(0.0);
			ed.setUdfamoTotalAmount(0.0);
			ed.setUsr(usr);
			empDedRepo.save(ed);
			
						
		return mesg;

	}

		
}
