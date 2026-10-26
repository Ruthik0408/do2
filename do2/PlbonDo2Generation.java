
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
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.Constants;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class PlbonDo2Generation implements java.io.Serializable {

    
	 
	JdbcTemplate jdbcTemplate;
	
	 
	Environment env;
	
	 
	EmployeeRepository empRepo;
	 
	 
	MiscRepository miscRepo;
	
	 
	Do2Repository do2Repo;
	 	
	AllowanceRateRepository allowanceRateRepo;
	
	

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

	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");

	public String generatePlbonDo2(){
		 Usr usr=Utils.getLoggedInUsr();
		System.out.println ( "  AAAAAAA  BBBBBB  CCCCCC ");
		String mesg="";
		Date effectiveDate = null;
		Date plbFromDate = null;
		Date plbToDate = null;
		Date previousDaDate = null;
		Date do2Date = null;		
	   	int recs   = 0;
	   	int recordsRead  = 0;
	   	int recordsAdded  = 0;
	   	int taDeptCases  = 0;
	   	int neCases  = 0;
	   	int recruits  = 0;
	   	int thisMonthFsCases  = 0;
		int batch  = 8000; //starting batch number of plbon Dos2
		int num = 8000; //starting batch number of plbon Dos2
		BufferedWriter writer=null;
		
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertplbondos2.csv");		
		String qe = Utils.getCurrentQuarter();
		String str = "";
		
		try {	
			
			 plbFromDate = DateManager.getDate(Constants.PLBON_FROM_DATE);
			 plbToDate = DateManager.getDate(Constants.PLBON_TO_DATE);
			do2Date = DateManager.getToday();
			writer = new BufferedWriter(new FileWriter(file));
			if (isPlbonDo2AlreadyGenerated(plbFromDate,plbToDate)){
				mesg="PLBON  DO2 ALREADY GENERATED";
				return mesg; 
			}
			insertPlbonRate(plbFromDate,plbToDate);
		
			List<Employee> empList = empRepo.findDaGenEmployee(Utils.getCurrentQuarter());
					
			
			if (empList.size() == 0) {
				mesg = "NO EMPLOYEE FOUND";
				return mesg;
			}
			
			for (Employee emp : empList){
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
				      getStringDate(do2Date)+",PLBON,"+getStringDate(plbFromDate)+","+getStringDate(plbToDate)+",P,S,"+qe+","+qe+",false,true,false,"+
					  usr.getId()+",System Generated Do2";	
				writer.write(str);	
			    writer.newLine();			
			    ++recs;
			    ++recordsAdded;
			}
			writer.close();
			Connection con=getConnection();
		//	String dir=env.getProperty("spring.application.filedir");
			CopyManager copyManager = new CopyManager((BaseConnection) con);
			
	   	    FileReader fileReader = new FileReader(dir+"/insertplbondos2.csv");
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
		  mesg="PLBON Do2 Generation Done.";
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
		     allowanceRate.setCreatedAt(DateManager.getCurrentTimestamp());
			 allowanceRateRepo.save(allowanceRate);
		}
		
		List<Misc> miscList =miscRepo.findAllByKey1AndCurrent("Bulk Processor", true);
			if (miscList.size() > 0) {
				miscList.get(0).setValue1("Busy");
			}
	}
	
	public boolean isPlbonDo2AlreadyGenerated(Date plbonFromDate, Date plbonToDate) {
		List<Misc> miscList =miscRepo.findByKey1AndValue1AndDate1AndDate2AndCurrent("PLBON_GENERATION", Constants.PLBON_FINANCIAL_YEAR, 
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
	    	misc.setKey1("PLBON_GENERATION");
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
			List<Misc> miscList=miscRepo.findPlbonBatch(qe);
			if((miscList!=null && miscList.size()==0) || miscList==null) {
				List<Integer> batchList=do2Repo.findPlbonGeneratedBatches(frDate, toDate,qe);
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
