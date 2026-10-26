
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
public class NeEncDo2Generation implements java.io.Serializable {

    
	 
	JdbcTemplate jdbcTemplate;
	
	 
	Environment env;
	
	 
	EmployeeRepository empRepo;
	 
	 
	MiscRepository miscRepo;
	
	 
	Do2Repository do2Repo;
	 	
	AllowanceRateRepository allowanceRateRepo;
	LpcRegisterRepository lpcRepo;
	

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
	
	EmployeeDeductionRepository empDedRepo;

	public EmployeeDeductionRepository getEmpDedRepo() {
		return empDedRepo;
	}

	public void setEmpDedRepo(EmployeeDeductionRepository empDedRepo) {
		this.empDedRepo = empDedRepo;
	}

	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");

	public String generateNeEncfinDo2(){
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
		int batch  = 7500; //starting batch number of neenc Dos2
		int num = 7500; //starting batch number of neenc Dos2
		BufferedWriter writer=null;
		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertneencdos2.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";
		 int encDays=0;
		
		 List<Do2> neEncList = null;
		 List<Do2> encFinDo2List = null;
		
		try {	
			
			daEffectiveDate = DateManager.getDate(Constants.DA_EFFECTIVE_DATE);			
			previousDaDate = DateManager.getDate(Constants.DA_PREVIOUS_EFFECTIVE_DATE);
			do2Date = DateManager.getToday();
			writer = new BufferedWriter(new FileWriter(file));
			if (isNeDaDo2AlreadyGenerated(effectiveDate)){
				mesg="NE ENC DO2 ALREADY GENERATED WEF 01-07-2024";
				return mesg; 
			}
						
			 List<LpcRegister>lpcList= lpcRepo.findNeDaEmpList(daEffectiveDate, Utils.getLastDayOfPreviousQuarter());
			 
			  for (LpcRegister lpcemp : lpcList){
				  encDays=0;
	        		Employee emp = lpcemp.getEmployee();
	        		dischargeDate = lpcemp.getDischargeDate();
	        		Long empId = emp.getId();
	        		System.out.println ( " employee "  +empId   +" army number "+lpcemp.getArmyNo()   +" lpcemp " +lpcemp.getEmployee());
	        		
	       
	        				 encFinDo2List = do2Repo.findEncfinDo2List(empId, "ENCASH", "ENCFIN", "DISCH", daEffectiveDate, Utils.getLastDayOfPreviousQuarter());
	        				 
	        			if (encFinDo2List.size()!=0) {
	        					 
	        					 Do2 d = encFinDo2List.get(0);
	        						
	        					 if (encFinDo2List.get(0).getDo2Date().before(DateManager.getRevisedAppendixJImplementationDate())){
	        							if (encFinDo2List.get(0).getDescription().equals("ENCASH")) {
	        								encDays=encFinDo2List.get(0).getTjra();	
	        							} 
	        						} else { 
	        							if (encFinDo2List.get(0).getDescription().equals("ENCFIN")) {
	        								encDays= Integer.parseInt(encFinDo2List.get(0).getColumnC());
	        							}
	        						}
	        					 
	        					 
	        					 
	        					 neEncList = do2Repo.findNeEncList(empId,dischargeDate,"NEENC", "DADIFF");
		        					
	     	        			
		        				 if (neEncList.size()==0) {
		        					 str = "";
		        						++recordsRead;
		        						effectiveDate = daEffectiveDate;
		        						
		        						if (recs > 999){
		        							batch = batch + 1;
		        							recs = 0;
		        						}
		        						str = batch+","+emp.getUnit().getUnitCode()+","+emp.getArmyNo()+","+emp.getCheckDigit()+",IA,0,5555,555,"+
		        						      getStringDate(do2Date)+",NEENC,"+getStringDate(dischargeDate)+","+encDays+",DADIFF,P,S,"+qe+","+qe+",true,true,false,"+
		        							  usr.getId()+",System Generated Do2";	
		        						writer.write(str);	
		        					    writer.newLine();			
		        					    ++recs;
		        					    ++recordsAdded;
		        				 }
	        					
	        		      	}
	        					
	        				 
	        		 }		 
			  	 
	        		
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
	   	    FileReader fileReader = new FileReader(dir+"/insertneencdos2.csv");
	   	    copyManager.copyIn(
	   	    		" COPY do2(batch_no, unit_code, army_no, check_digit, do2_group, do2_type, do2_no, do2_item_no, do2_date, " +
	   	            " description, from_date, tjra, column_a,status, transcription_type, qe, input_qe, audit_cage, approved, time_bar_sanction, fk_usr, reason) " +
	   	            " FROM STDIN  with delimiter as ','  NULL as ''", fileReader);
	        fileReader.close();
	        file.delete();
	        
	        updateMiscTable(qe, daEffectiveDate);
			writeBatchInMisc(qe);
	        
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		  mesg="NE ENC Do2 Generation Done.";
		  return mesg;
	}
	
	
	
	public boolean isNeDaDo2AlreadyGenerated(Date effectiveDate){
	
		List<Misc> miscList =miscRepo.findByKey1AndDateAndCurrent("NE_ENC_GENERATION", effectiveDate);
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
	
	
	
	 public void updateMiscTable(String qe, Date date){
		 Usr usr=Utils.getLoggedInUsr();
		 Misc misc = new Misc();
	    	misc.setKey1("NE_ENC_GENERATION");
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
	 
	 
		private void writeBatchInMisc(String qe) {
			// TODO Auto-generated method stub
			List<Misc> miscList=miscRepo.findIncrBatch(qe);
			if((miscList!=null && miscList.size()==0) || miscList==null) {
				List<Integer> batchList=do2Repo.findNeEncGeneratedBatches(qe);
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
