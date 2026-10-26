
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
public class NeDaDo2Generation implements java.io.Serializable {

    
	 
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
	
	

	public EmployeeDeductionRepository getEdRepo() {
		return edRepo;
	}

	public void setEdRepo(EmployeeDeductionRepository edRepo) {
		this.edRepo = edRepo;
	}



	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");

	public String generateNeDaDo2(){
		 Usr usr=Utils.getLoggedInUsr();
		System.out.println ( "  AAAAAAA  BBBBBB  CCCCCC ");
		String mesg="";
		Date effectiveDate = null;
		Date daEffectiveDate = null;
		Date dischargeDate = null;
		Date previousDaDate = null;
		Date do2Date = null;		
	   	int recs   = 0;
	   	int recordsRead  = 0;
	   	int recordsAdded  = 0;
	   	int taDeptCases  = 0;
	   	int neCases  = 0;
	   	int recruits  = 0;
	   	int thisMonthFsCases  = 0;
		int batch  = 6500; //starting batch number of Ne Da Dos2
		int num = 6500; //starting batch number of NeDa Dos2
		BufferedWriter writer=null;
		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertnedados2.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";
		
		 List<EmployeeDeduction> edList=null;
		 List<Do2> daDo2List = null;
		try {	
			
			daEffectiveDate = DateManager.getDate(Constants.DA_EFFECTIVE_DATE);			
			previousDaDate = DateManager.getDate(Constants.DA_PREVIOUS_EFFECTIVE_DATE);
			do2Date = DateManager.getToday();
			writer = new BufferedWriter(new FileWriter(file));
			if (isNeDaDo2AlreadyGenerated(effectiveDate)){
				mesg="NE DA DO2 ALREADY GENERATED WEF 01-07-2024";
				return mesg; 
			}
			
			
					 
					
			
		//	insertDaRate(previousDaDate,daEffectiveDate);
			
			 List<LpcRegister>lpcList= lpcRepo.findNeDaEmpList(daEffectiveDate, Utils.getLastDayOfPreviousQuarter());
			 
			  for (LpcRegister lpcemp : lpcList){
	        		Employee emp = lpcemp.getEmployee();
	        		dischargeDate = lpcemp.getDischargeDate();
	        		Long empId = emp.getId();
	        		System.out.println ( " employee "  +empId   +" army number "+lpcemp.getArmyNo()   +" lpcemp " +lpcemp.getEmployee());
	        		
	        		 edList = edRepo.findNeEdIdList(empId, qe);
		//	List<Employee> empList = empRepo.findDaGenEmployee(Utils.getCurrentQuarter());
	        	//	 if (edList.size()==1) {
	        			 
	        				 daDo2List = do2Repo.findDaOrNeDaDo2(empId, daEffectiveDate, "DA", "NEDA");
	        				 
	        				 if (daDo2List.size()==0) {
	        					 str = "";
	        						++recordsRead;
	        						effectiveDate = daEffectiveDate;
	        						
	        						if (recs > 999){
	        							batch = batch + 1;
	        							recs = 0;
	        						}
	        						str = batch+","+emp.getUnit().getUnitCode()+","+emp.getArmyNo()+","+emp.getCheckDigit()+",IA,0,5555,555,"+
	        						      getStringDate(do2Date)+",NEDA,"+getStringDate(effectiveDate)+","+getStringDate(dischargeDate)+",P,S,"+qe+","+qe+",true,true,false,"+
	        							  usr.getId()+",System Generated Do2";	
	        						writer.write(str);	
	        					    writer.newLine();			
	        					    ++recs;
	        					    ++recordsAdded;
	        				 }
	        				 
	        		 //}		 
			  }	 
	        		
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
	   	    FileReader fileReader = new FileReader(dir+"/insertnedados2.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2(batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, from_date, to_date, status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();
	        
	        updateMiscTable(qe, daEffectiveDate);
			writeBatchInMisc(qe,daEffectiveDate);
	        
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		  mesg="NE DA Do2 Generation Done.";
		  return mesg;
	}
	
	
	
	public boolean isNeDaDo2AlreadyGenerated(Date effectiveDate){
	
		List<Misc> miscList =miscRepo.findByKey1AndDateAndCurrent("NE_DA_GENERATION", effectiveDate);
		return miscList.size() >0;
	
	}
	
	public String openNeIrla() {

		String message = "Success";
		String mesg = null;
		Usr usr = Utils.getLoggedInUsr();
		
		List<Employee> empList = empRepo.findNeLraEmployee(Utils.getCurrentQuarter());
		Date daEffectiveDate = null;
		
		try {
			daEffectiveDate = DateManager.getDate(Constants.DA_EFFECTIVE_DATE)
					;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		 List<LpcRegister>lpcList= lpcRepo.findNeDaEmpList(daEffectiveDate, Utils.getLastDayOfPreviousQuarter());
		 
		
		
		//List<LpcRegister> lpcRegisterList = lpcRepo.findNeLraEmpList();
		System.out.println ( " EMPLOYEE LIST SIZE " +empList.size());
		  for (LpcRegister lpcemp : lpcList){
      		Employee emp = lpcemp.getEmployee();
      		
		
		if (emp != null) {
			
		/*	if (recordExistsInLpcRegister(emp) == false) {
				System.out.println ( " NO recod in lpc register " +emp.getArmyNo());
				mesg = "No record in Lpc Register -- Issue Tender Memo manually";
				return mesg;

			}
			if (duplicateEdRecord(emp) == true) {
				System.out.println ( " duplicate record  " +emp.getArmyNo());
				mesg = "Ed Record already created";
				return mesg;

			}

			if (recordExistsInEmployeeForCurrentMonth(emp) == true) {
				System.out.println ( " record exists for this month  " +emp.getArmyNo());
				mesg = "Employee Record in employee Table - IRLA recreated";
				return mesg;

			}*/
		
			if(!duplicateEdRecord(emp) && !recordExistsInEmployeeForCurrentMonth(emp)) {
				System.out.println  ( " ed created now " +emp.getArmyNo());
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
				edRepo.save(ed);
				System.out.println("employee from ed " + ed.getEmployee().getId());

				mesg = "NE Irla Opened ";
			}
		
		} else {
			mesg = "Cannot Open NE Irla, reason being  Employee with current record missing ";
		}
		
		  }	
			
			return mesg;

		}
 
	
	
	private boolean duplicateEdRecord(Employee emp) {
		// TODO Auto-generated method stub
		if (emp != null) {
			List<EmployeeDeduction> edList = edRepo.findByEmpIdAndQe(emp.getId(), Utils.getCurrentQuarter());

			if (edList.size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean recordExistsInEmployeeForCurrentMonth(Employee emp) {
		// TODO Auto-generated method stub
		if (emp != null) {
			List<LpcRegister> lpcList = lpcRepo
					.findByEmployeeIdRecordStatusApprovedEcsGeneratedDischargeDateGreaterThanAndQe(emp.getId(), "F",
							DateManager.getNeImplementationDate(), Utils.getCurrentQuarter());

			if (lpcList == null || lpcList.size() >= 0) {
				String armyNo = lpcList.get(0).getArmyNo();
				if (Utils.getCurrentEmployee(armyNo).getQe().equals(Utils.getCurrentQuarter())) {
					return true;
				}
			}
		}

		return false;
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
	
	
	
	 public void updateMiscTable(String qe, Date date){
		 Usr usr=Utils.getLoggedInUsr();
		 Misc misc = new Misc();
	    	misc.setKey1("NE_DA_GENERATION");
	    	misc.setValue1(qe);	    	
	    	misc.setDate1(date);
	    	misc.setUsr(usr);
	    	misc.setCurrent(true);
	    	miscRepo.save(misc);
	    	
	    	List<Misc> miscList =miscRepo.findAllByKey1AndCurrent("Bulk Processor", true);
			if (miscList.size() > 0) {
				miscList.get(0).setValue1("Busy");
			}
	 }
	 
	 
		private void writeBatchInMisc(String qe,Date daEffectiveDate) {
			// TODO Auto-generated method stub
			List<Misc> miscList=miscRepo.findIncrBatch(qe);
			if((miscList!=null && miscList.size()==0) || miscList==null) {
				List<Integer> batchList=do2Repo.findNeDaGeneratedBatches(daEffectiveDate, qe);
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
}
