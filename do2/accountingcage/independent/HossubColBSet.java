package itsdc.dolphin.do2.accountingcage.independent;

import itsdc.dolphin.models.AdjustmentCb;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.do2.Do2Manager;
 
import itsdc.dolphin.utils.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
 
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HossubColBSet {
  
	public void checkColumnBInHossub() {
		 
	 
		/*em.createNativeQuery("DROP TABLE IF EXISTS hossubreviewtemp ").executeUpdate();
		 
		em.createNativeQuery("create temp table hossubreviewtemp as " +
		 			" select id, army_no as ano, column_b as colb from do2 where " +
					" description='HOSSUB' and status='V' and from_date>'2008-08-31' order by id ").executeUpdate();
		*/
		List<Object[]> hossubReviewList =null;// em.createNativeQuery("SELECT * FROM hossubreviewtemp").getResultList();
		int doneCount=0;
		int notDoneCount=0;
		for (Object[] obj : hossubReviewList) {
			int date = 0;
			int month = 0;
			int year = 0;
			long do2Id = Long.valueOf(obj[0].toString());
			String armyNo = obj[1].toString();
			String columnB = "";
			if (obj[2]!=null) {
				columnB = obj[2].toString();
			}
			String reversedDob = "";
			String dateStr="";
			String monthStr=""; 
			 		
			if (columnB.length()==8) {
				 try {
					 date  = Integer.parseInt(columnB.substring(0,2));
					 month = Integer.parseInt(columnB.substring(2,4));
					 year  = Integer.parseInt(columnB.substring(4,8));
				 } catch (NumberFormatException e) {
					 notDoneCount++;
					 continue;
				 }
			} else {
				notDoneCount++;
				continue;
			}
			
			if (year!=0 && month!=0 && date!=0 && year>1980 && year<2011 && month>0 && month<13 && date>0 && date<32) {
				if ((month==2 && date>0 && ((year/4==0) && date<30) || (year/4!=0 && date<29))
						|| (month!=2)) {
					if (date<10) {
						dateStr="0"+date;
					} else {
						dateStr=date+"";
					}
					if (month<10) {
						monthStr="0"+month;
					} else {
						monthStr=month+"";
					}
					reversedDob = year+monthStr+dateStr;
					//em.createNativeQuery(" update do2 set column_b=:column where id=:do2Id and army_no=:ano ")
					//		.setParameter("column",reversedDob).setParameter("do2Id", do2Id).setParameter("ano", armyNo).executeUpdate();
					//em.flush();
					doneCount++;
				} else {
					notDoneCount++;
					continue;
				}
			} else {
				notDoneCount++;
				continue;
			}
		}
	//	FacesMessages.instance().add("Review Completed ......................     ");
	//	FacesMessages.instance().add("Done Count .................................     " + doneCount);
	//	FacesMessages.instance().add("Not Done Count .............................     " + notDoneCount);
	}
 }


/*
 * try { em.merge(ceaAdj);                                              SAMPLE CODE NOT TO BE DELETED
 * .................. em.flush(); } catch
 * (org.hibernate.validator.InvalidStateException e) {
 * InvalidValue[] invalidValues = e.getInvalidValues();
 * for(InvalidValue invalidValue : invalidValues) {
 * System.out.println("$$$$$$$$$$$$$$$$$$$$$$$  Invalid Value: #0" +
 * invalidValue.getPropertyName()); } }
 */
