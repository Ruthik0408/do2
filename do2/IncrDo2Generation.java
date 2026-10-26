
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

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Misc;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeRepository;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/**
 * Author : IT & SDC, Secunderabad
 */
@RestController
public class IncrDo2Generation implements java.io.Serializable {

    
	 
	JdbcTemplate jdbcTemplate;
	
	 
	Environment env;
	
	 
	EmployeeRepository empRepo;
	 
	 
	MiscRepository miscRepo;
	
	 
	Do2Repository do2Repo;
	 	
	
	
	
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

	SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
	
	final static public long MILLIS_IN_DAY = 1000 * 3600 * 24;
	final static public int min_qual_service_days = 180;
	 
	 
	int incrGeneratedCases    = 0;
	int incrNotGeneratedCases = 0;
	int incrAfterCurrentYearCases = 0;
	int incrNullCases         = 0;
	int incrWithinSixMonths   = 0;
	int notMusteredCases      = 0;
	int recruitCases          = 0;
	int enrolCases            = 0;
	int fsdueCases            = 0;
	int nqsEffCases           = 0;
	int deserterCases         = 0;
	int totalCasesRead        = 0;
	int recs                  = 0;
	int batch                 = 5001;	 
	 
	public List<Object []> getIncrGenData(String qe,Date incrDate) {		
	 	jdbcTemplate.execute("DROP TABLE IF EXISTS empdet");	 	
	 	if (qe!=null && qe.substring(0,2).equals("01")) {
	 		jdbcTemplate.execute("CREATE temp TABLE empdet AS " +
			" SELECT employee.id as empid, army_no, name, unit_code, task, increment_date, enrolment_date, fs_due_date, fk_status_code, " +
			" fk_substantive_rank, fk_present_unit, check_digit, attestation_date, initial_mustering_date, hony_lieu_date, hony_captain_date, " +
			" remustering_date from employee join unit ON employee.fk_present_unit = unit.id where employee.current_record=true and employee.qe='"+qe+"' and employee.regular_ta_flag = 'R' " +
			" and employee.fk_status_code < 5 and employee.increment_date='"+incrDate+"'"); 
	 		
	 		
	 	} else if (qe!=null && qe.substring(0,2).equals("07")) {
			jdbcTemplate.execute("CREATE temp TABLE empdet AS " +
			" SELECT employee.id as empid, army_no, name, unit_code, task, increment_date, enrolment_date, fs_due_date, fk_status_code, " +
			" fk_substantive_rank, fk_present_unit, check_digit, attestation_date, initial_mustering_date, hony_lieu_date, hony_captain_date, " +
			" remustering_date from employee join unit ON employee.fk_present_unit = unit.id where employee.qe ='"+qe+"' and employee.regular_ta_flag = 'R' " +
			" and fk_status_code < 5 and employee.increment_date='"+incrDate+"'");
		} else {
			//move to controller
			//FacesMessages.instance().add("Increments Can Be Generated Only In January or July After 7cpc Implementation !!! ");
			return null;
		}				
		List<Object[]> empList = jdbcTemplate.query("SELECT * from empdet order by task, unit_code, army_no",new RowMapper<Object[]>() {
			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				// TODO Auto-generated method stub
				Object [] obj=new Object[17];
				obj[0]=rs.getLong(1);
				obj[1]=rs.getString(2);
				obj[2]=rs.getString(3);
				obj[3]=rs.getString(4);
				obj[4]=rs.getInt(5);
				obj[5]=rs.getDate(6);
				obj[6]=rs.getDate(7);
				obj[7]=rs.getDate(8);
				obj[8]=rs.getInt(9);
				obj[9]=rs.getInt(10);
				obj[10]=rs.getInt(11);
				obj[11]=rs.getString(12);
				obj[12]=rs.getDate(13);
				obj[13]=rs.getDate(14);
				obj[14]=rs.getDate(15);
				obj[15]=rs.getDate(16);
				obj[16]=rs.getDate(17);
				return obj;
			}
			 
		 });
	    return empList;
	}		
	
	public String generateIncrDo2(String qe,Date incrDate,Date firstDate) {
	 /* to be activated in Jan/July each year*/
		String mesg="";
		StringBuffer sb = new StringBuffer("                                                                                                                                    ");
		BufferedWriter writer=null;
		String dir=env.getProperty("spring.application.filedir");
		File file = new File(dir+"/insertincr.txt");
		BufferedWriter writer1 = null;
		File file1 = new File(dir+"/incrGenRejections"+(qe.substring(0,2))+(qe.substring(3)));
		String valid=isValid(qe);
		if (!valid.startsWith("valid"))
	            return valid;
		List<Object[]> empList=getIncrGenData(qe,incrDate);
	// NQS verification shifted to incr.java
	// getNqsCases();	
		long empId = 0;
		boolean incrReqd = true;		
		String armyNo=null;
		String unitcd=null;
		Date incrdt = null;
		Date enroldt=null;
		Date fsduedt=null;
		Date attestdt=null;
		Date musterdt=null;
		Date remusterdt=null;
		Date lieudt = null;
		Date captdt = null;
		int stcd = 0;
		int subrk = 0;
		int fkUnit = 0;
        String cd = "";
		String incDate = null;
		String pborName = null;
		String remk = null;
		int slno = 0;
		int lncnt = 0;
		int prevTask = 0;
		int task = 0;
		
		try {	    	
	    	writer = new BufferedWriter(new FileWriter(file));
	    	writer1 = new BufferedWriter(new FileWriter(file1));
	    	for(Object[] obj : empList) {
	    		if (obj[0] != null){
	    			/*BigInteger bigInt = (BigInteger) obj[0];
	    			empId = 0;
	    			if (bigInt != null)
	    				empId = bigInt.longValue();*/
	    			empId=(Long) obj[0];
	    		}
	    		armyNo=null;
	    		if (obj[1] != null)
	    			armyNo=(String) obj[1];
	    		pborName=null;
	    		if (obj[2] != null)
	    			pborName=(String) obj[2];	    		
	    		unitcd=null;
	    		if (obj[3] != null)
	    			unitcd=(String) obj[3];
	    		task=0;
	    		if (obj[4] != null)
	    			task=(Integer) obj[4];
	    		incrdt=null;
	    		incDate=null;
	    		if (obj[5] != null){
	    			incrdt=(Date) obj[5];
	    		    incDate = df1.format(incrdt);
	    		}
	    		enroldt=null;
	    		if (obj[6] != null){
	    			enroldt=(Date) obj[6];	    		   
	    		}
	    		fsduedt=null;
	    		if (obj[7] != null){
	    			fsduedt=(Date) obj[7];	    		   
	    		}
	    		stcd=0;
	    		if (obj[8] != null){
	    			stcd=(Integer) obj[8];	    		   
	    		}
	    		subrk=0;
	    		if (obj[9] != null){
	    			subrk=(Integer) obj[9];	    		   
	    		}
	    		fkUnit=0;
	    		if (obj[10] != null){
	    			fkUnit=(Integer) obj[10];	    		   
	    		}
	    		cd="";
	    		if (obj[11] != null){
	    			cd=(String) obj[11];	    		   
	    		}
	    		attestdt=null;
	    		if (obj[12] != null){
	    			attestdt=(Date) obj[12];	    		   
	    		}
	    		musterdt=null;
	    		if (obj[13] != null){
	    			musterdt=(Date) obj[13];	    		   
	    		}
	    		lieudt=null;
	    		if (obj[14] != null){
	    			lieudt=(Date) obj[14];	    		   
	    		}
	    		captdt=null;
	    		if (obj[15] != null){
	    			captdt=(Date) obj[15];	    		   
	    		}
	    		remusterdt=null;
	    		if (obj[16] != null){
	    			remusterdt=(Date) obj[16];	    		   
	    		}
	    		++totalCasesRead;
	    	 	if (lncnt > 58){
	    	 		writer1.newLine();
			  		writer1.write("\f");
			  		sb = new StringBuffer("                                                                                                                                        ");
			   		sb.replace(5,69,"               LIST OF PBORs FOR WHOM INCREMENT NOT GENERATED                                                             ");
			  		sb.replace(72, 81, qe);
			  		sb.replace(83,99,"TASK :");
			  		sb.replace(101, 104, task+"");
			  		writer1.write(sb.toString());
					writer1.write("\r\n");
			  		sb = new StringBuffer("------------------------------------------------------------------------------------------------------------------------------------");
			  		writer1.write(sb.toString());
					writer1.write("\r\n");
			  		sb = new StringBuffer("    Sl No    Army Number        Name                    Unit      Dt. Incmt       Remarks                                                 ");
			  		writer1.write(sb.toString());
					writer1.write("\r\n");
			  		sb = new StringBuffer("------------------------------------------------------------------------------------------------------------------------------------");
			  		lncnt = 5;
			  		writer1.write(sb.toString());
					writer1.write("\r\n");
			  	}
	    	 	
	    	 	incrReqd=true;
	    	 	remk=null;
	    	 	if (incrdt == null) {
    	    		if (subrk > 1) {
    	    			if (Utils.getPao().getPaoCode().equals("94")) {
    	    				++incrNullCases;
    	    	    		remk="Dt of Increment is blank";
    	    	    		incrReqd=false;	 
    	    			} else {
    	    				if (incrDate.before(DateManager.getSeventhCpcDate())) {
    	    					if (musterdt != null) {
    			    	    		++incrNullCases;
    			    	    		remk="Dt of Increment is blank";
    			    	    		incrReqd=false;	 
    	    	    			} else if (remusterdt != null) {
    			    	    		++incrNullCases;
    			    	    		remk="Dt of Increment is blank";
    			    	    		incrReqd=false;
    	    	    			} else {
    	    	    				++notMusteredCases;
    	    	    				incrReqd=false;
    	    	    			}
    	    				} else {
    	    					if (enroldt!=null && !enroldt.before(DateManager.getSeventhCpcDate()) 
    	    							&& musterdt==null && attestdt==null) {
    	    						++incrNullCases;
    			    	    		remk="Dt of Increment is blank";
    			    	    		incrReqd=false;
    	    					}
    	    					if (enroldt!=null && enroldt.before(DateManager.getSeventhCpcDate()) 
    	    							&& musterdt==null) {
    	    						++notMusteredCases;
    	    						incrReqd=false;
    	    					}
    	    					if (enroldt!=null && enroldt.before(DateManager.getSeventhCpcDate()) 
    	    							&& musterdt != null) {
    			    	    		++incrNullCases;
    			    	    		remk="Dt of Increment is blank";
    			    	    		incrReqd=false;	 
    	    	    			}
    	    				}
    	    			}
    	    	    } else if (subrk == 1) { 
    	    		   ++recruitCases;	   			    			
    	    		   incrReqd=false;
    	    	    }
    	    		incrReqd=false;
	    	 	}
	    	 	
	    	 	if (incrReqd == true){	
	    	    	if (incrdt.before(DateManager.getSixthCpcDate()) && subrk > 1){
	    	    		remk="Date of Next Increment < 20060101";
	    				++incrNotGeneratedCases;
	    	    		incrReqd=false;
	    	    	}	
	    	    }
	    	 	
	       	   if (incrReqd == true){
	    	    	if (incrdt.equals(incrDate) && (lieudt != null  || captdt != null)) {
	    	    		if (superannuationBeforeIncrDate(incrDate, armyNo)){
	    	    			incrReqd=false;		 
	    	    		}
	    	    	}
	    	    }	
	       	    
	       		if (incrReqd == true){
		    		if (subrk > 1 && stcd == 4) {
		    			++deserterCases;
		    			remk="Deserter";	    			
		    			incrReqd=false;
		    	    }  	
	       		}
	    	
	    		if (incrReqd == true){
	    			if ((incrdt.after(incrDate)) && subrk > 1) {	
	    				++incrAfterCurrentYearCases;
	    				incrReqd=false;	
	    			}
	    		}
	   	    
	    		
	    		if (incrReqd == true) {
	    			if ((incrdt.equals(incrDate)) && subrk > 1) {
	    			   if (!incrDate.before(DateManager.getSixthCpcDate()) 
	    					   && incrDate.before(DateManager.getSeventhCpcDate())) {
		    			   if (((attestdt != null && attestdt.after(firstDate)) &&
		    	    	       ((musterdt != null && musterdt.after(firstDate)))) ||
		    	    	       (attestdt == null  &&
		    	    	    	(musterdt != null && musterdt.after(firstDate)))){
		    				    remk="Incorrect date of Increment";
		    				    ++incrNotGeneratedCases;
		    	    	    	incrReqd=false;	
		    	    	    //	System.out.println("xxxxxxxxxxxxxxxxxxxxxxxx  " +armyNo);
		    	    	   }
	    			   } 
	    			   // added condition
	    			   if (incrdt.after(DateManager.getSeventhCpcDate())) {
	    				   if (enroldt!=null && !enroldt.before(DateManager.getSeventhCpcDate())
	    						   && enroldt.after(firstDate)) {
	    					   remk="Incorrect date of Increment";
		    				   ++incrNotGeneratedCases;
		    	    	       incrReqd=false;	
		    	    	   //    System.out.println("yyyyyyyyyyyyyyyyyyyyyyyy  " +armyNo);
	    				   }
	    			   }
	    		    }
	   	        }
	    		// not reqd in the context of 7 CPC
	    		/*if (incrReqd == true) {
	    			if (incrementAdjustedInJanuary(empId, incrdt)) {
	    				remk="Incr within 6 months --- DNI updated now";
	    				++incrWithinSixMonths;
	    				incrReqd=false;
	    			}
	    		}*/
	  		    		
	       	    if (incrReqd == true) {
	       	    	if (subrk == 1){
	       	    		++recruitCases;	
		    			incrReqd = false;	
		    		}    
	    	    }	    	   
	    	    	   
	    	    if (incrReqd == true) {	
	    	    	if (fsduedt != null && !fsduedt.after(incrDate)) {
	    	    		++fsdueCases;
	    	    		remk="FS Due Case";
	    	    		incrReqd=false;
	    	    	}	
	    	    }	
	    	    	    	    	   
	    		if ((incrReqd == false) && (remk != null)) {	
	    		  	if (prevTask != task){	
	    		  		writer1.newLine();
	    		  		writer1.write("\f");
				  		sb = new StringBuffer("                                                                                                                                        ");
				  		sb.replace(5,69,"               LIST OF PBORs FOR WHOM INCREMENT NOT GENERATED                                                             ");
				  		sb.replace(72, 81, qe);
				  		sb.replace(83,99,"TASK :");
				  		sb.replace(101, 104, task+"");
				  		writer1.write(sb.toString());
						writer1.write("\r\n");
				  		sb = new StringBuffer("----------------------------------------------------------------------------------------------------------");
				  		writer1.write(sb.toString());
						writer1.write("\r\n");
				  		sb = new StringBuffer("    Sl No    Army Number        Name                Unit      Dt. Incmt       Remarks                                                 ");
				  		writer1.write(sb.toString());
						writer1.write("\r\n");
				  		sb = new StringBuffer("----------------------------------------------------------------------------------------------------------");
				  		lncnt = 5;
				  		writer1.write(sb.toString());
						writer1.write("\r\n");
				  		prevTask = task;			  		
				  	}
	    			++slno;
	    			sb = new StringBuffer("                                                                                                                                        ");	
	    			sb.replace(5,10, slno+"");
	    			sb.replace(13,21, armyNo);
	    			sb.replace(24,48, pborName);
	    			sb.replace(51,56, unitcd);
	    			if (incDate != null){
	    				sb.replace(61,70, incDate);
	    			}
	    			sb.replace(75,100,remk);
	    			writer1.write(sb.toString());
	    			writer1.write("\r\n");
	    			++lncnt;	
	    		}	  	
	    		if (incrReqd == true){		
	    			generateSysIncrDo2(empId, armyNo, unitcd, fkUnit, cd, incrdt,incrDate,qe,writer);
	    			++incrGeneratedCases;
	    			empRepo.updateIncrDate(Utils.getNextIncrementDate(incrDate), empId);    		
	    		}
	    	}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
			
	
		 if (writer !=null){
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (writer1 !=null){
			try {
				writer1.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		writeInDo2();
		writeControls();
		updateMiscTable(qe);
		writeBatchInMisc(qe,incrDate);
		mesg="INCR Do2 Generation Done.";
		return mesg;
		//FacesMessages.instance().add("INCR SQL Generation Completed !!!!!");
	}   
	private void writeBatchInMisc(String qe,Date incrDate) {
		// TODO Auto-generated method stub
		List<Misc> miscList=miscRepo.findIncrBatch(qe);
		if((miscList!=null && miscList.size()==0) || miscList==null) {
			List<Integer> batchList=do2Repo.findIncrGeneratedBatches(incrDate, qe);
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

	private void writeInDo2() {
		// TODO Auto-generated method stub
		System.out.println("writing do2");
		Connection con=getConnection();
		String dir=env.getProperty("spring.application.filedir");
		try {
			CopyManager copyManager =new CopyManager((BaseConnection) con);
			FileReader fileReader = new FileReader(dir+"/insertincr.txt");
			copyManager.copyIn("copy do2(batch_no,unit_code,fk_unit,army_no,fk_employee,check_digit,do2_group,do2_type,do2_no,do2_item_no,"
					+ "		do2_date,description,from_date,column_c,status,transcription_type,qe,input_qe,audit_cage,fk_usr,remarks) from STDIN  with delimiter as ','  NULL as ''",fileReader);
			fileReader.close();
			System.out.println("writing do2 over");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}

	public void generateSysIncrDo2(long empid, String ano, String unitcd, int fkUnit, String cd, Date incrdt,Date incrDate,String qe,BufferedWriter bw ) {
		Usr usr=Utils.getLoggedInUsr();
		while (incrdt != null && !(incrdt.after(incrDate))) {	
			incrdt = getValidDNI(incrdt);
			if (incrdt == null)
				return;
			if (incrdt.before(incrDate) &&  doesDuplicateIncrDo2Exist(empid, incrdt)){
				 incrdt = DateManager.getNextIncrementDate(incrdt);	 
	    	     continue;
			}
			String dtIncr = incrdt.toString();
			if (recs > 999){
				batch = batch + 1;
				recs = 0;
			}
			try {    
				bw.write(batch+","+unitcd+","+fkUnit+","+ano+","+empid+","+cd+","+"IA"+","+"0"+","+"6666"+","+"666"+","+
			dtIncr+","+"INCR"+","+dtIncr+","+"REG"+","+Do2Manager.PENDING+","+ Do2Manager.SYSTEM_GENERATED+","+qe+","+qe+","+"true"+","+usr.getId()+","+"System Generated"); 
			 bw.newLine();	
			++recs;
    	 } catch (IOException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    	 }
    	 
    	 incrdt = DateManager.getNextIncrementDate(incrdt);	 
    	 continue;
		}
	 }
	 public String isValid(String qe) {
			String valid="valid";
			if ((qe.substring(0,2)).equals("01") || (qe.substring(0,2)).equals("07")) {
				if (isIncrGenerationOver()) {
					valid="Increment DOs2 Already Generated For " + qe;
					
				}
			} else {
				valid="Invalid Month For Increment DO2 Generation !!!!! ";
				
			}
			return valid;
	 }
	 
	 public boolean isIncrGenerationOver() {
		List<Misc> miscList =miscRepo.findByKey1AndValue1("INCR_GENERATION", Utils.getCurrentQuarter());

		return miscList.size() > 0;
	 }
	 public void updateMiscTable(String qe){
		 Usr usr=Utils.getLoggedInUsr();
		 Misc misc = new Misc();
	    	misc.setKey1("INCR GENERATION COMPLETED");
	    	misc.setValue1(qe);	    	
	    	misc.setDate1(DateManager.getToday());
	    	misc.setUsr(usr);
	    	miscRepo.save(misc);
	 }
	 public void writeControls() {
		    String fileName="incrgenControls";
		    String dir=env.getProperty("spring.application.filedir");
			File file = new File(dir+"/"+fileName);
		    String line = "";
			BufferedWriter writer = null;
			incrNotGeneratedCases+=(deserterCases+incrNullCases+recruitCases+enrolCases+fsdueCases+incrAfterCurrentYearCases
					                  +notMusteredCases+incrWithinSixMonths);
			try {
			    writer = new BufferedWriter(new FileWriter(file));
			    line="             CONTROLS FOR GENERATION OF INCREMENTS IN " + Utils.getCurrentQuarter();
			    writer.write(line); writer.newLine();
			    line="             -------- --- ---------- -- ---------- -- -------";
			    writer.write(line);
			    writer.newLine(); writer.newLine(); writer.newLine(); writer.newLine();
			    line="  Total Cases Read                                        :" + padStringData(totalCasesRead, 8);
			    writer.write(line);  
			    writer.newLine(); writer.newLine(); writer.newLine(); 
			    line="  Increment Generated                                     :" + padStringData(incrGeneratedCases, 8) ;
			    writer.write(line);
			    writer.newLine(); writer.newLine(); writer.newLine();
			    line="  Increment NOT Generated                                 :" + padStringData(incrNotGeneratedCases, 8) ;
			    writer.write(line); 	
			    writer.newLine(); writer.newLine();	writer.newLine();
			    line="  REASONS FOR NON-GENERATION OF INCREMENT DOs2";
			    writer.write(line); 
			    writer.newLine(); writer.newLine();
			    line="  Recruit Cases                                           :" + padStringData(recruitCases, 8);
			    writer.write(line);
			    writer.newLine(); writer.newLine();
			    line="  Attested but not yet mustered for recruits b4 7cpc date :" + padStringData(notMusteredCases, 8);
			    writer.write(line);
			    writer.newLine(); writer.newLine();
			    line="  Increment date after current increment date             :" + padStringData(incrAfterCurrentYearCases, 8);
			    writer.write(line);
			    writer.newLine(); writer.newLine();
			    line="  Increment Date Within 6 Months In 7cpc (DNI updated now):" + padStringData(incrWithinSixMonths, 8) ;
			    writer.write(line);			 
			    writer.newLine(); writer.newLine();  
			    line="  FS Due Cases                                            :" + padStringData(fsdueCases, 8);
			    writer.write(line);
			    writer.newLine(); writer.newLine();
			    line="  Deserter Cases                                          :" + padStringData(deserterCases, 8);
			    writer.write(line);
			    writer.newLine(); writer.newLine();
			    line="  Increment date is blank                                 :" + padStringData(incrNullCases, 8);
			    writer.write(line);
			    writer.newLine(); writer.newLine();
			  
			 /*   line="  Enrolled after 1st January                              :" + padStringData(enrolCases, 8);
			    writer.write(line);
			    writer.newLine();
			    writer.newLine();
			 */ 
			    writer.close();
               
        
			}  catch (Exception e) {
				Utils.handleException(e);
			}
			if (writer !=null){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}
	
	 public Date getDNI(String qe) {	// year to be changed in every January
		if ((Integer.parseInt(qe.substring(0,2))==1 && Integer.parseInt(qe.substring(3))==2026)) {
			return new Date(new GregorianCalendar((Integer.parseInt(qe.substring(3))), Calendar.JANUARY, 1).getTimeInMillis());
		} else {
			return new Date(new GregorianCalendar(Integer.parseInt(qe.substring(3)), Calendar.JULY, 1).getTimeInMillis());
		}
	}
	
	 public Date getFirstDayOfTheCurrentYear(String qe) {
		return new Date((new GregorianCalendar(Integer.parseInt(qe.substring(3)), Calendar.JANUARY, 1)
				.getTimeInMillis()));
	}
	
	public Date getFirstJulyOfTheCurrentYear(String qe) {
		return new Date((new GregorianCalendar(Integer.parseInt(qe.substring(3)), Calendar.JULY, 1)
				.getTimeInMillis()));
	}
	
	 public Date getValidDNI(Date incrdt){		
		Date incrementDate = incrdt;
		Calendar cal = new GregorianCalendar();
		cal.setTime(incrementDate);
		cal.set(Calendar.DATE, 1);		
		if (incrementDate.before(DateManager.getSixthCpcDate())) {
			incrementDate = null;	
		} else if (incrementDate.before(DateManager.getSeventhCpcDate())) {
			cal.set(Calendar.MONTH, Calendar.JULY);			
		} else {
			if (cal.get(Calendar.MONTH)==Calendar.JANUARY) {
		 		;		
			} else if (cal.get(Calendar.MONTH)==Calendar.JULY) {
				;					
			} else {
				incrementDate = null;
			}
		}
		if (incrementDate == null){
			return null;
		}
		incrementDate.setTime(cal.getTimeInMillis());
		return incrementDate;
	}
	
	public boolean doesDuplicateIncrDo2Exist(long empId, Date incrdate) {
		 
		List<Do2> do2List = do2Repo.findByDescFromDateEqualStatusNotEqual(empId, "C", "INCR", incrdate) ;
		if (do2List!=null && do2List.size()>0)
		  return true;
		
		return false;
	}	
	
	public boolean superannuationBeforeIncrDate(Date incrDate, String ano) {
		Employee employee = Utils.getCurrentEmployee(ano);
		List<Do2> hcommnList =do2Repo.findAllByEmployeeIdAndDescriptionAndStatus(employee.getId(),"HCOMMN", "V");
		if (hcommnList.size() > 0){
			for (Do2 d : hcommnList){
				if (d.getFromDate().before(incrDate)){ 
				   if (d.getToDate() == null){
					   return true;
				   } else if (!d.getToDate().after(incrDate)){
					   return true;
				   }
				}
			}
		}	
		return false;
	}
	/*public boolean incrementAdjustedInJanuary(Long empId, Date incrDt) {
		
		boolean adjustedInJanuary = false;
		
		List<Do2> incrDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE "
				+ " d.employee.id=:emp AND d.description='INCR' AND (d.status='V' or d.status='P') AND "
				+ " d.fromDate=:firstDate")
				.setParameter("emp", empId)
				.setParameter("firstDate", firstDate).getResultList();
		
		if (incrDo2List!=null && incrDo2List.size()>0) {
			
			adjustedInJanuary = true;
	
			List<Do2> hikeDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE "
					+ " d.employee.id=:emp AND (d.description='PROMOT' OR d.description='MACPS' OR "
					+ " d.description='MSTHAV' OR d.description='MSTJCO' OR d.description='LATENR' OR "
					+ " d.description='HCOMMN') AND d.status='V' AND d.fromDate=:firstDate")
					.setParameter("emp", empId).setParameter("firstDate", firstDate)
					.getResultList();
						
			if (hikeDo2List!=null && hikeDo2List.size()>0) {
				adjustedInJanuary = false;
			} 
			
			if (adjustedInJanuary==true) {
				Calendar cal = new GregorianCalendar();
				cal.setTime(incrDt);
				try {
					cal.setTime(DateManager.getDate(cal.get(Calendar.YEAR)+1, cal.get(Calendar.JANUARY), 1));
				} catch (Exception e) {
					Utils.handleException(e);
				}
				Utils.getEntityManager().createNativeQuery("update employee set increment_date=:INCR "
							+ " where id=:empid and current_record=true ").setParameter("INCR",cal.getTime())
							.setParameter("empid", empId).executeUpdate();
				Utils.getEntityManager().flush();					
			}
		}
		return adjustedInJanuary;
	}*/
	
	public String padStringData(int value, int fieldLength) {
		String field = String.valueOf(value).trim();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < fieldLength-field.length(); i++) {
			buffer.append(" ");
		}
		return buffer.toString()+field;
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
}




